<template>
  <div class="recycle-page">
    <div class="page-header">
      <h2>回收站</h2>
      <span class="tip">回收站中的文件将在 15 天后自动清理</span>
    </div>

    <div class="recycle-table" v-loading="loading">
      <el-table :data="recycleList" style="width: 100%">
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

        <el-table-column label="删除时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.deletedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleRestore(row)">
              <el-icon><RefreshLeft /></el-icon>&nbsp;恢复
            </el-button>
            <el-button type="danger" size="small" @click="handlePurge(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && recycleList.length === 0" description="回收站是空的" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document, RefreshLeft } from '@element-plus/icons-vue'
import { listRecycle, restoreFile, deletePermanently } from '@/api/file'

const loading = ref(false)
const recycleList = ref([])

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listRecycle()
    recycleList.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复「${row.name}」吗？`,
      '恢复确认',
      { type: 'info', confirmButtonText: '恢复', cancelButtonText: '取消' }
    )
    await restoreFile(row.id)
    ElMessage.success('恢复成功')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

const handlePurge = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要永久删除「${row.name}」吗？此操作不可恢复！`,
      '永久删除确认',
      { type: 'warning', confirmButtonText: '永久删除', cancelButtonText: '取消' }
    )
    await deletePermanently(row.id)
    ElMessage.success('已永久删除')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
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
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.recycle-page {
  .page-header {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 16px;

    h2 {
      font-size: 18px;
      font-weight: 500;
      color: $text-primary;
    }

    .tip {
      font-size: 12px;
      color: $text-secondary;
    }
  }

  .recycle-table {
    background: #fff;
    border-radius: 8px;
    border: 1px solid $border-light;
    padding: 8px;
    min-height: 300px;

    .file-name-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .file-icon {
        color: $text-secondary;
      }

      .name {
        font-size: 14px;
        color: $text-primary;
      }
    }
  }
}
</style>
