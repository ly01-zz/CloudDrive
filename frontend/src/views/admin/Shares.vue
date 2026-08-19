<template>
  <div class="admin-shares">
    <h2>分享管理</h2>
    <p class="tip">查看所有用户的分享，可强制取消违规分享</p>

    <el-table :data="shareList" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column label="创建者" min-width="150">
        <template #default="{ row }">
          <div class="creator-cell">
            <span class="nickname">{{ row.nickname || '-' }}</span>
            <span class="phone">{{ row.phone || '' }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="文件名" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.fileName }}
        </template>
      </el-table-column>

      <el-table-column label="分享码" width="110">
        <template #default="{ row }">
          <span :title="`链接：${origin}/s/${row.shareCode}`">{{ row.shareCode }}</span>
        </template>
      </el-table-column>

      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.shareType === 1" type="warning" size="small">私密</el-tag>
          <el-tag v-else type="success" size="small">公开</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="访问 / 下载" width="130" align="center">
        <template #default="{ row }">
          <span class="count">{{ row.totalVisits || 0 }}<span v-if="row.maxVisits">/{{ row.maxVisits }}</span></span>
          <span class="count-divider">/</span>
          <span class="count">{{ row.totalDownloads || 0 }}<span v-if="row.maxDownloads">/{{ row.maxDownloads }}</span></span>
        </template>
      </el-table-column>

      <el-table-column label="过期时间" width="170">
        <template #default="{ row }">
          {{ row.expireTime ? formatTime(row.expireTime) : '永久' }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            type="danger"
            size="small"
            @click="handleForceCancel(row)"
          >强制取消</el-button>
          <span v-else class="no-op">-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && shareList.length === 0" description="暂无分享记录" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAllShares, forceCancelShare } from '@/api/admin'

const loading = ref(false)
const shareList = ref([])
const origin = window.location.origin

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAllShares()
    shareList.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const statusType = (status) => {
  if (status === 0) return 'success'
  if (status === 1) return 'info'
  return 'danger'
}

const handleForceCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要强制取消「${row.fileName}」的分享吗？取消后链接将无法访问`,
      '强制取消确认',
      { type: 'warning', confirmButtonText: '强制取消', cancelButtonText: '返回' }
    )
    await forceCancelShare(row.id)
    ElMessage.success('已强制取消')
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
.admin-shares {
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

  .creator-cell {
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

  .count {
    font-size: 13px;
    color: $text-regular;
  }

  .count-divider {
    margin: 0 6px;
    color: $text-placeholder;
  }

  .no-op {
    color: $text-placeholder;
  }
}
</style>
