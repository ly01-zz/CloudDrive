<template>
  <aside class="sidebar">
    <div class="sidebar-logo">
      <el-icon><Cloudy /></el-icon>
      <span>云盘</span>
    </div>

    <el-menu
      :default-active="$route.path"
      router
      class="sidebar-menu"
      background-color="transparent"
      text-color="#606266"
      active-text-color="#409EFF"
    >
      <el-menu-item index="/app/files">
        <el-icon><Folder /></el-icon>
        <span>全部文件</span>
      </el-menu-item>

      <el-menu-item index="/app/shares">
        <el-icon><Share /></el-icon>
        <span>我的分享</span>
      </el-menu-item>

      <el-menu-item index="/app/applications">
        <el-icon><Expand /></el-icon>
        <span>我的申请</span>
      </el-menu-item>

      <el-menu-item index="/app/recycle">
        <el-icon><Delete /></el-icon>
        <span>回收站</span>
      </el-menu-item>

      <el-sub-menu index="/app/admin" v-if="userStore.isAdmin">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </template>
        <el-menu-item index="/app/admin/dashboard">数据看板</el-menu-item>
        <el-menu-item index="/app/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/app/admin/approvals">扩容审批</el-menu-item>
        <el-menu-item index="/app/admin/shares">分享管理</el-menu-item>
        <el-menu-item index="/app/admin/files">文件治理</el-menu-item>
        <el-menu-item index="/app/admin/logs">操作日志</el-menu-item>
        <el-menu-item index="/app/admin/announcement">公告管理</el-menu-item>
        <el-menu-item index="/app/admin/config">系统配置</el-menu-item>
      </el-sub-menu>
    </el-menu>

    <!-- 月度下载流量 -->
    <div class="sidebar-traffic">
      <div class="traffic-label">
        <span>月度流量</span>
        <span class="traffic-used">{{ trafficText }}</span>
      </div>
      <el-progress
        :percentage="trafficPercentage"
        :stroke-width="6"
        :show-text="false"
        color="#67C23A"
      />
    </div>

    <div class="sidebar-storage">
      <div class="storage-label">
        <span>存储空间</span>
        <span class="storage-used">{{ usedText }}</span>
      </div>
      <el-progress
        :percentage="percentage"
        :stroke-width="6"
        :show-text="false"
        color="#409EFF"
      />
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Cloudy, Folder, Share, Expand, Delete, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

// 字节大小格式化（B/KB/MB/GB）
const formatSize = (b) => {
  if (b < 1024) return b + 'B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + 'KB'
  if (b < 1024 * 1024 * 1024) return (b / 1024 / 1024).toFixed(1) + 'MB'
  return (b / 1024 / 1024 / 1024).toFixed(1) + 'GB'
}

// 存储空间（占位：从 userInfo 读取，暂无则默认）
const used = computed(() => userStore.userInfo?.usedSpace || 0)
const total = computed(() => userStore.userInfo?.totalSpace || 10737418240) // 默认 10GB

const percentage = computed(() => {
  if (!total.value) return 0
  return Math.min(100, Math.round((used.value / total.value) * 100))
})

const usedText = computed(() => `${formatSize(used.value)} / ${formatSize(total.value)}`)

// 月度下载流量（跨月自动重置，后端在下载时扣减）
const trafficUsed = computed(() => userStore.userInfo?.usedDownloadTraffic || 0)
const trafficLimit = computed(() => userStore.userInfo?.monthlyDownloadLimit || 0)

const trafficPercentage = computed(() => {
  if (!trafficLimit.value) return 0
  return Math.min(100, Math.round((trafficUsed.value / trafficLimit.value) * 100))
})

const trafficText = computed(() => {
  if (!trafficLimit.value) return '无限制'
  return `${formatSize(trafficUsed.value)} / ${formatSize(trafficLimit.value)}`
})
</script>

<style scoped lang="scss">
.sidebar {
  width: $sidebar-width;
  background: #fff;
  border-right: 1px solid $border-light;
  display: flex;
  flex-direction: column;

  .sidebar-logo {
    height: $header-height;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: $primary;
    border-bottom: 1px solid $border-light;

    .el-icon {
      font-size: 28px;
    }
  }

  .sidebar-menu {
    flex: 1;
    border-right: none;
    padding-top: 8px;

    .el-menu-item {
      height: 48px;
      line-height: 48px;
      margin: 4px 12px;
      border-radius: 8px;

      &.is-active {
        background: $primary-light !important;
      }

      &:hover {
        background: #f5f7fa;
      }
    }
  }

  .sidebar-traffic {
    padding: 16px 20px;
    border-top: 1px solid $border-light;

    .traffic-label {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: $text-secondary;
      margin-bottom: 8px;

      .traffic-used {
        color: $text-regular;
      }
    }
  }

  .sidebar-storage {
    padding: 16px 20px;
    border-top: 1px solid $border-light;

    .storage-label {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: $text-secondary;
      margin-bottom: 8px;

      .storage-used {
        color: $text-regular;
      }
    }
  }
}
</style>
