<template>
  <div class="shares-page">
    <div class="page-header">
      <h2>我的分享</h2>
      <span class="tip">分享的文件可在「全部文件」中通过更多操作创建</span>
    </div>

    <div class="shares-table" v-loading="loading">
      <el-table :data="shareList" style="width: 100%">
        <el-table-column label="文件名" min-width="220">
          <template #default="{ row }">
            <div class="file-name-cell">
              <el-icon :size="20" class="file-icon"><Document /></el-icon>
              <span class="name" :title="row.fileName">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.shareType === 1" type="warning" size="small">私密</el-tag>
            <el-tag v-else type="success" size="small">公开</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="提取码" width="90" align="center">
          <template #default="{ row }">
            {{ row.extractCode || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
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

        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="copyLink(row)">复制链接</el-button>
            <el-button v-if="row.extractCode" type="primary" link @click="copyText(row.extractCode, '提取码已复制')">
              复制提取码
            </el-button>
            <el-button v-if="row.status === 0" type="danger" link @click="handleCancel(row)">取消分享</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && shareList.length === 0" description="暂无分享，去文件列表中创建吧" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { listMyShares, cancelShare } from '@/api/share'

const loading = ref(false)
const shareList = ref([])

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listMyShares()
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

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消「${row.fileName}」的分享吗？取消后链接将无法访问`,
      '取消分享确认',
      { type: 'warning', confirmButtonText: '取消分享', cancelButtonText: '返回' }
    )
    await cancelShare(row.id)
    ElMessage.success('已取消分享')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

const copyLink = (row) => {
  copyText(`${window.location.origin}/s/${row.shareCode}`, '分享链接已复制')
}

const copyText = async (text, tip) => {
  try {
    await navigator.clipboard.writeText(text)
  } catch (err) {
    // 降级方案：兼容非 https 环境
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    console.error(err)
  }
  ElMessage.success(tip)
}

const formatTime = (iso) => {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.shares-page {
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

  .shares-table {
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
        color: $primary;
      }

      .name {
        font-size: 14px;
        color: $text-primary;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
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
  }
}
</style>
