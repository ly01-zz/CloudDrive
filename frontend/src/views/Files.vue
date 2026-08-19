<template>
  <div class="files-page">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="Upload" @click="handleUpload">上传文件</el-button>
        <el-button :icon="FolderAdd" @click="showCreateFolder = true">新建文件夹</el-button>
      </div>
      <div class="toolbar-right">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
          <el-radio-button label="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="file-list-wrapper" v-loading="loading">
      <!-- 列表视图 -->
      <template v-if="viewMode === 'list'">
        <el-table :data="fileList" style="width: 100%" @row-dblclick="handleOpen">
          <el-table-column label="文件名" min-width="280">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon :size="20" class="file-icon">
                  <Folder v-if="row.isFolder" />
                  <Document v-else />
                </el-icon>
                <span class="name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="大小" width="120">
            <template #default="{ row }">
              {{ row.isFolder ? '-' : formatSize(row.fileSize) }}
            </template>
          </el-table-column>

          <el-table-column label="修改时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.updatedAt) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
                <el-button type="primary" link>
                  <el-icon><More /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="!row.isFolder" command="download">下载</el-dropdown-item>
                    <el-dropdown-item v-if="!row.isFolder" command="share">分享</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 网格视图 -->
      <template v-else>
        <div class="grid-view">
          <div
            v-for="item in fileList"
            :key="item.id"
            class="grid-item"
            @dblclick="handleOpen(item)"
          >
            <div class="grid-icon">
              <el-icon :size="48" v-if="item.isFolder"><Folder /></el-icon>
              <el-icon :size="48" v-else><Document /></el-icon>
            </div>
            <div class="grid-name" :title="item.name">{{ item.name }}</div>
            <div class="grid-info">{{ item.isFolder ? '-' : formatSize(item.fileSize) }}</div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <el-empty v-if="!loading && fileList.length === 0" description="暂无文件" />
    </div>

    <!-- 新建文件夹弹窗 -->
    <CreateFolderDialog
      v-model="showCreateFolder"
      :parent-id="parentId"
      @success="fetchFiles"
    />

    <!-- 分享文件弹窗 -->
    <CreateShareDialog v-model="showShareDialog" :file="shareFile" />

    <!-- 上传进度面板 -->
    <UploadProgress :tasks="uploadTasks" @clear="uploadTasks = []" />

    <!-- 隐藏的文件上传 input -->
    <input
      ref="fileInput"
      type="file"
      style="display: none"
      multiple
      @change="onFileSelected"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Upload, FolderAdd, List, Grid, Folder, Document, More,
} from '@element-plus/icons-vue'
import { listFiles, getDownloadUrl, deleteToRecycle, cancelUpload } from '@/api/file'
import { uploadFileToCos } from '@/utils/cos'
import { useFileStore } from '@/stores/file'
import { useUserStore } from '@/stores/user'
import CreateFolderDialog from '@/components/CreateFolderDialog.vue'
import CreateShareDialog from '@/components/CreateShareDialog.vue'
import UploadProgress from '@/components/UploadProgress.vue'

const route = useRoute()
const router = useRouter()
const fileStore = useFileStore()
const userStore = useUserStore()

const loading = ref(false)
const fileList = ref([])
const viewMode = ref('list')
const showCreateFolder = ref(false)
const showShareDialog = ref(false)
const shareFile = ref(null)
const parentId = ref(0)
const fileInput = ref(null)

// 上传任务列表
const uploadTasks = ref([])
let taskIdCounter = 0

const fetchFiles = async () => {
  loading.value = true
  try {
    const res = await listFiles(parentId.value)
    fileList.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handleOpen = (item) => {
  if (item.isFolder) {
    parentId.value = item.id
    fileStore.setParentId(item.id)
    fileStore.pushBreadcrumb({ id: item.id, name: item.name })
    router.push({ path: '/app/files', query: { parentId: item.id } })
    fetchFiles()
  } else {
    handleDownload(item)
  }
}

const handleCommand = (cmd, row) => {
  if (cmd === 'download') {
    handleDownload(row)
  } else if (cmd === 'share') {
    shareFile.value = row
    showShareDialog.value = true
  } else if (cmd === 'delete') {
    handleDelete(row)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name}」吗？删除后将移至回收站`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteToRecycle(row.id)
    ElMessage.success('已移至回收站')
    fetchFiles()
    // 删除后回收空间，同步侧边栏已用空间
    userStore.refreshInfo()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

const handleDownload = async (row) => {
  try {
    const res = await getDownloadUrl(row.id)
    const { downloadUrl, fileName } = res.data
    // 创建一个临时 a 标签触发下载
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = fileName
    a.target = '_blank'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    ElMessage.success('开始下载')
    // 下载会扣减月度流量，同步侧边栏数据
    userStore.refreshInfo()
  } catch (err) {
    console.error(err)
  }
}

const handleUpload = () => {
  fileInput.value?.click()
}

const onFileSelected = async (e) => {
  const files = Array.from(e.target.files || [])
  if (!files.length) return

  for (const file of files) {
    // 添加上传任务
    const task = {
      id: ++taskIdCounter,
      name: file.name,
      percent: 0,
      status: 'uploading',
    }
    uploadTasks.value.push(task)

    let uploadResult = null
    try {
      uploadResult = await uploadFileToCos(file, parentId.value, (percent) => {
        task.percent = percent
      })
      task.percent = 100
      task.status = 'success'
      ElMessage.success(uploadResult.quick ? `「${file.name}」秒传成功` : `「${file.name}」上传成功`)
    } catch (err) {
      task.status = 'error'
      ElMessage.error(`「${file.name}」上传失败：${err.message}`)
      // 上传失败：清理后端遗留的"上传中"记录，回滚预扣空间
      const pendingFileId = uploadResult?.uploadId || err.uploadId
      if (pendingFileId) {
        try {
          await cancelUpload(pendingFileId)
        } catch (cleanupErr) {
          console.error('清理失败的上传记录失败：', cleanupErr)
        }
      }
    }
  }

  // 清空 input，允许重复选择同一文件
  e.target.value = ''
  // 刷新文件列表 + 同步侧边栏已用空间
  fetchFiles()
  userStore.refreshInfo()
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatTime = (iso) => {
  if (!iso) return '-'
  const d = new Date(iso)
  return d.toLocaleString('zh-CN')
}

watch(
  () => route.query.parentId,
  (val) => {
    parentId.value = val ? Number(val) : 0
    fetchFiles()
  },
  { immediate: true }
)

onMounted(() => {
  fileStore.init?.()
})
</script>

<style scoped lang="scss">
.files-page {
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #fff;
    border-radius: 8px;
    border: 1px solid $border-light;
  }

  .file-list-wrapper {
    background: #fff;
    border-radius: 8px;
    border: 1px solid $border-light;
    padding: 8px;
    min-height: 400px;

    .file-name-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .file-icon {
        color: $primary;
      }

      .name {
        font-size: 14px;
        color: $text-primary;
      }
    }
  }

  .grid-view {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 16px;
    padding: 16px;

    .grid-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px 8px;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.2s;

      &:hover {
        background: $primary-light;
      }

      .grid-icon {
        color: $primary;
        margin-bottom: 8px;
      }

      .grid-name {
        font-size: 13px;
        color: $text-primary;
        text-align: center;
        width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .grid-info {
        font-size: 12px;
        color: $text-secondary;
        margin-top: 4px;
      }
    }
  }
}
</style>
