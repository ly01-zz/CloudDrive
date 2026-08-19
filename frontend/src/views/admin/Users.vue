<template>
  <div class="admin-users">
    <h2>用户管理</h2>
    <el-table :data="userList" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="昵称" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="role" label="角色" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'danger' : ''">{{ row.role === 1 ? '管理员' : '用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="存储空间" min-width="130">
        <template #default="{ row }">
          {{ formatSize(row.usedSpace) }} / {{ formatSize(row.totalSpace) }}
        </template>
      </el-table-column>
      <el-table-column label="本月流量" min-width="130">
        <template #default="{ row }">
          {{ formatSize(row.usedDownloadTraffic) }} / {{ formatSize(row.monthlyDownloadLimit) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '已冻结' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            type="danger"
            size="small"
            @click="handleDisable(row.id)"
          >禁用</el-button>
          <el-button
            v-else
            type="success"
            size="small"
            @click="handleEnable(row.id)"
          >启用</el-button>
          <el-button type="warning" size="small" @click="handleResetTraffic(row)">重置流量</el-button>
          <el-button type="primary" size="small" @click="openQuotaDialog(row)">调整配额</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 调整配额弹窗 -->
    <el-dialog
      v-model="quotaDialogVisible"
      title="调整配额"
      width="460px"
      :close-on-click-modal="false"
      @close="resetQuotaForm"
    >
      <div class="quota-user">
        用户：<b>{{ quotaForm.nickname }}</b>（{{ quotaForm.phone }}）
        <span class="quota-current">当前空间 {{ formatSize(quotaForm.currentTotalSpace) }}，流量 {{ formatSize(quotaForm.currentTraffic) }}</span>
      </div>
      <el-form :model="quotaForm" label-width="110px" @submit.prevent>
        <el-form-item label="总空间（MB）">
          <el-input-number v-model="quotaForm.totalSpaceMb" :min="1" :max="99999999" :controls="false" style="width: 100%" placeholder="留空不修改" />
        </el-form-item>
        <el-form-item label="月度流量（MB）">
          <el-input-number v-model="quotaForm.trafficMb" :min="1" :max="99999999" :controls="false" style="width: 100%" placeholder="留空不修改" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="quotaForm.reason" type="textarea" :rows="2" placeholder="选填，将记录到操作日志" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quotaDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveQuota">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllUsers, disableUser, enableUser, resetUserTraffic, updateUserQuota } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const userList = ref([])
const quotaDialogVisible = ref(false)

const quotaForm = ref({
  userId: null,
  nickname: '',
  phone: '',
  currentTotalSpace: 0,
  currentTraffic: 0,
  totalSpaceMb: null,
  trafficMb: null,
  reason: '',
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getAllUsers()
    userList.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handleDisable = async (id) => {
  try {
    await disableUser(id)
    ElMessage.success('已禁用')
    fetchUsers()
  } catch (err) {
    console.error(err)
  }
}

const handleEnable = async (id) => {
  try {
    await enableUser(id)
    ElMessage.success('已启用')
    fetchUsers()
  } catch (err) {
    console.error(err)
  }
}

const handleResetTraffic = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `确定要将「${row.nickname}」的本月下载流量重置为 0 吗？`,
      '重置流量',
      { inputPlaceholder: '请输入重置原因（选填）', confirmButtonText: '重置', cancelButtonText: '取消' }
    )
    await resetUserTraffic(row.id, { reason: value || undefined })
    ElMessage.success('流量已重置')
    fetchUsers()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

const openQuotaDialog = (row) => {
  quotaForm.value = {
    userId: row.id,
    nickname: row.nickname,
    phone: row.phone,
    currentTotalSpace: row.totalSpace,
    currentTraffic: row.monthlyDownloadLimit,
    totalSpaceMb: null,
    trafficMb: null,
    reason: '',
  }
  quotaDialogVisible.value = true
}

const resetQuotaForm = () => {
  quotaForm.value = {
    userId: null, nickname: '', phone: '',
    currentTotalSpace: 0, currentTraffic: 0,
    totalSpaceMb: null, trafficMb: null, reason: '',
  }
}

const handleSaveQuota = async () => {
  if (!quotaForm.value.totalSpaceMb && !quotaForm.value.trafficMb) {
    ElMessage.warning('请至少填写一项要调整的配额')
    return
  }
  saving.value = true
  try {
    await updateUserQuota(quotaForm.value.userId, {
      totalSpace: quotaForm.value.totalSpaceMb ? quotaForm.value.totalSpaceMb * 1024 * 1024 : undefined,
      monthlyDownloadLimit: quotaForm.value.trafficMb ? quotaForm.value.trafficMb * 1024 * 1024 : undefined,
      reason: quotaForm.value.reason || undefined,
    })
    ElMessage.success('配额已更新')
    quotaDialogVisible.value = false
    fetchUsers()
  } catch (err) {
    console.error(err)
  } finally {
    saving.value = false
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(fetchUsers)
</script>

<style scoped lang="scss">
.admin-users {
  h2 {
    font-size: 18px;
    font-weight: 500;
    color: $text-primary;
  }

  .quota-user {
    margin-bottom: 16px;
    font-size: 14px;
    color: $text-regular;

    .quota-current {
      display: block;
      margin-top: 4px;
      font-size: 12px;
      color: $text-secondary;
    }
  }
}
</style>
