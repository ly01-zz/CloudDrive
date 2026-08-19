<template>
  <div class="applications-page">
    <div class="page-header">
      <h2>我的扩容申请</h2>
      <el-button type="primary" :icon="Plus" @click="showApply = true">申请扩容</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="applySize" label="申请大小">
        <template #default="{ row }">{{ formatSize(row.applySize) }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="申请理由" show-overflow-tooltip />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applyTime" label="申请时间">
        <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
      </el-table-column>
    </el-table>

    <!-- 申请弹窗 -->
    <el-dialog v-model="showApply" title="申请扩容" width="400px">
      <el-form :model="form" ref="formRef" label-width="80px">
        <el-form-item label="申请大小" prop="applySize">
          <el-input-number v-model="form.applySize" :min="1" :max="102400" />
          <span class="unit">MB</span>
        </el-form-item>
        <el-form-item label="申请理由" prop="reason">
          <el-input v-model="form.reason" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApply = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleApply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyApplications, applySpace } from '@/api/space'

const loading = ref(false)
const list = ref([])
const showApply = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  applySize: 1024,
  reason: '',
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getMyApplications({ page: 1, size: 10 })
    list.value = res.data?.records || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handleApply = async () => {
  try {
    submitting.value = true
    await applySpace({
      applySize: form.applySize * 1024 * 1024, // MB → bytes
      reason: form.reason,
    })
    ElMessage.success('申请已提交')
    showApply.value = false
    fetchList()
  } catch (err) {
    console.error(err)
  } finally {
    submitting.value = false
  }
}

const statusType = (s) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[s] || 'info'
}

const statusText = (s) => {
  const map = { 0: '待审批', 1: '已通过', 2: '已拒绝' }
  return map[s] || '未知'
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
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
.applications-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 {
      font-size: 18px;
      font-weight: 500;
      color: $text-primary;
    }
  }

  .unit {
    margin-left: 8px;
    color: $text-secondary;
  }
}
</style>
