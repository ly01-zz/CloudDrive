<template>
  <div class="admin-logs">
    <h2>操作日志</h2>
    <p class="tip">记录管理员的关键操作（禁用/启用、审批、配置修改、取消分享、文件治理、公告发布等）</p>

    <el-table :data="logList" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作类型" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="actionType(row.action)" size="small">{{ actionName(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作对象" width="120" align="center">
        <template #default="{ row }">
          {{ row.targetType || '-' }}<span v-if="row.targetId"> #{{ row.targetId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="原因 / 备注" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reason || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作管理员" width="110" align="center">
        <template #default="{ row }">#{{ row.adminId }}</template>
      </el-table-column>
      <el-table-column label="IP" width="140">
        <template #default="{ row }">{{ row.ipAddress || '-' }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && logList.length === 0" description="暂无操作日志" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listAdminLogs } from '@/api/admin'

const loading = ref(false)
const logList = ref([])

// 操作类型 → 中文名与标签颜色
const ACTION_MAP = {
  DISABLE_USER: { name: '禁用用户', type: 'danger' },
  ENABLE_USER: { name: '启用用户', type: 'success' },
  RESET_TRAFFIC: { name: '重置流量', type: 'warning' },
  UPDATE_QUOTA: { name: '调整配额', type: 'warning' },
  APPROVE_APPLICATION: { name: '扩容审批', type: 'primary' },
  UPDATE_CONFIG: { name: '修改配置', type: 'info' },
  ADD_CONFIG: { name: '新增配置', type: 'info' },
  DELETE_CONFIG: { name: '删除配置', type: 'info' },
  CANCEL_SHARE: { name: '取消分享', type: 'danger' },
  DELETE_FILE: { name: '删除文件', type: 'danger' },
  RESTORE_FILE: { name: '恢复文件', type: 'success' },
  PUBLISH_ANNOUNCEMENT: { name: '发布公告', type: 'primary' },
}

const actionName = (action) => ACTION_MAP[action]?.name || action || '-'
const actionType = (action) => ACTION_MAP[action]?.type || 'info'

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAdminLogs(200)
    logList.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const formatTime = (iso) => {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.admin-logs {
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
}
</style>
