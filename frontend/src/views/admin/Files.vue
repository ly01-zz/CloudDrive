<template>
  <div class="admin-files">
    <h2>文件治理</h2>
    <p class="tip">查看/搜索所有用户的文件（含回收站），可恢复或彻底删除</p>

    <!-- 搜索栏 -->
    <el-form inline class="filter-bar" @submit.prevent>
      <el-form-item label="文件名">
        <el-input v-model="query.keyword" placeholder="模糊搜索" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="精确匹配" clearable style="width: 150px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.isFolder" clearable placeholder="全部" style="width: 110px">
          <el-option label="文件" :value="false" />
          <el-option label="文件夹" :value="true" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.deleted" clearable placeholder="全部" style="width: 110px">
          <el-option label="正常" :value="0" />
          <el-option label="回收站" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 文件列表 -->
    <el-table :data="fileList" v-loading="loading" stripe style="margin-top: 8px">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="所属用户" width="130">
        <template #default="{ row }">
          <div class="owner-cell">
            <span class="nickname">{{ row.nickname || '-' }}</span>
            <span class="phone">{{ row.phone || '' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-icon :size="16" class="file-icon"><Folder v-if="row.isFolder" /><Document v-else /></el-icon>
          <span :class="{ 'deleted-name': row.deletedAt }">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="100" align="right">
        <template #default="{ row }">{{ row.fileSizeDesc }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.deletedAt" type="info" size="small">回收站</el-tag>
          <el-tag v-else-if="row.uploadStatus === 0" type="warning" size="small">上传中</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button v-if="row.deletedAt" type="primary" link size="small" @click="handleRestore(row)">恢复</el-button>
          <el-button type="danger" link size="small" @click="handlePurge(row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && fileList.length === 0" description="暂无符合条件的文件" />

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      class="pagination"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Document } from '@element-plus/icons-vue'
import { listAdminFiles, purgeAdminFile, restoreAdminFile } from '@/api/admin'

const loading = ref(false)
const fileList = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  phone: '',
  isFolder: null,
  deleted: null,
  page: 1,
  size: 10,
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAdminFiles({
      keyword: query.keyword || undefined,
      phone: query.phone || undefined,
      isFolder: query.isFolder,
      deleted: query.deleted,
      page: query.page,
      size: query.size,
    })
    fileList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  fetchList()
}

const handleReset = () => {
  query.keyword = ''
  query.phone = ''
  query.isFolder = null
  query.deleted = null
  query.page = 1
  fetchList()
}

const handlePageChange = (page) => {
  query.page = page
  fetchList()
}

const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复「${row.name}」吗？`,
      '恢复确认',
      { type: 'info', confirmButtonText: '恢复', cancelButtonText: '取消' }
    )
    await restoreAdminFile(row.id)
    ElMessage.success('已恢复')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

const handlePurge = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除「${row.name}」吗？此操作不可恢复，文件夹会级联删除其所有子孙`,
      '彻底删除确认',
      { type: 'warning', confirmButtonText: '彻底删除', cancelButtonText: '取消' }
    )
    await purgeAdminFile(row.id)
    ElMessage.success('已彻底删除')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

const formatTime = (iso) => {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.admin-files {
  h2 {
    font-size: 18px;
    font-weight: 500;
    color: $text-primary;
  }

  .tip {
    margin-top: 4px;
    font-size: 12px;
    color: $text-secondary;
  }

  .filter-bar {
    margin-top: 12px;
    padding: 12px 16px 0;
    background: #fff;
    border: 1px solid $border-light;
    border-radius: 8px;
  }

  .owner-cell {
    display: flex;
    flex-direction: column;

    .nickname {
      font-size: 14px;
      color: $text-primary;
    }

    .phone {
      font-size: 12px;
      color: $text-secondary;
    }
  }

  .file-icon {
    color: $primary;
    margin-right: 6px;
    vertical-align: -3px;
  }

  .deleted-name {
    color: $text-secondary;
    text-decoration: line-through;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
