<template>
  <div class="admin-announcement">
    <h2>公告管理</h2>
    <p class="tip">发布公告后，所有用户下次刷新页面时会弹窗看到（广播通知，不记录已读）</p>

    <!-- 发布表单 -->
    <div class="publish-panel">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="70px" @submit.prevent>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入公告内容" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="publishing" @click="handlePublish">发布公告</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 历史公告 -->
    <el-table :data="announcementList" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">
            {{ row.status === 0 ? '发布中' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="warning" link @click="handleOffline(row)">下架</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && announcementList.length === 0" description="暂无公告" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  publishAnnouncement, listAnnouncements, offlineAnnouncement, deleteAnnouncement,
} from '@/api/admin'

const loading = ref(false)
const publishing = ref(false)
const announcementList = ref([])
const formRef = ref()

const form = reactive({ title: '', content: '' })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAnnouncements()
    announcementList.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handlePublish = async () => {
  try {
    await formRef.value.validate()
    publishing.value = true
    await publishAnnouncement({ title: form.title, content: form.content })
    ElMessage.success('公告已发布，用户刷新页面后可见')
    form.title = ''
    form.content = ''
    fetchList()
  } catch (err) {
    console.error(err)
  } finally {
    publishing.value = false
  }
}

const handleOffline = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要下架「${row.title}」吗？下架后用户将不再看到`,
      '下架确认',
      { type: 'warning', confirmButtonText: '下架', cancelButtonText: '取消' }
    )
    await offlineAnnouncement(row.id)
    ElMessage.success('已下架')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告「${row.title}」吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteAnnouncement(row.id)
    ElMessage.success('已删除')
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
.admin-announcement {
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

  .publish-panel {
    margin-top: 12px;
    padding: 16px 16px 0;
    background: #fff;
    border: 1px solid $border-light;
    border-radius: 8px;
  }
}
</style>
