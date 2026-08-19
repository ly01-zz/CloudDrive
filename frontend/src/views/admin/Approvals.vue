<template>
  <div class="admin-approvals">
    <h2>扩容审批</h2>
    <el-table :data="list" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column prop="id" label="申请ID" width="80" />
      <el-table-column prop="userId" label="用户ID" />
      <el-table-column prop="applySize" label="申请大小">
        <template #default="{ row }">{{ formatSize(row.applySize) }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="理由" show-overflow-tooltip />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            type="primary"
            size="small"
            @click="openApprove(row)"
          >审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 审批弹窗 -->
    <el-dialog v-model="dialogVisible" title="审批" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input v-model="form.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleApprove">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllApplications, approveApplication } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const currentRow = ref(null)

const form = reactive({
  status: 1,
  remark: '',
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAllApplications({ page: 1, size: 10 })
    list.value = res.data?.records || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const openApprove = (row) => {
  currentRow.value = row
  form.status = 1
  form.remark = ''
  dialogVisible.value = true
}

const handleApprove = async () => {
  try {
    submitting.value = true
    await approveApplication(currentRow.value.id, {
      status: form.status,
      approveRemark: form.remark,
    })
    ElMessage.success('审批完成')
    dialogVisible.value = false
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

onMounted(fetchList)
</script>

<style scoped lang="scss">
.admin-approvals {
  h2 {
    font-size: 18px;
    font-weight: 500;
    color: $text-primary;
  }
}
</style>
