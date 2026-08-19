<template>
  <div class="admin-dashboard" v-loading="loading">
    <h2>数据看板</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">用户总数</div>
          <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
          <div class="stat-sub">冻结 {{ stats.frozenUsers || 0 }} 人</div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">存储空间</div>
          <div class="stat-value">{{ formatSize(stats.usedSpace) }}</div>
          <div class="stat-sub">总空间 {{ formatSize(stats.totalSpace) }}</div>
          <el-progress
            :percentage="spacePercentage"
            :stroke-width="6"
            :show-text="false"
            color="#409EFF"
            class="stat-progress"
          />
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">本月下载流量</div>
          <div class="stat-value">{{ formatSize(stats.monthDownloadTraffic) }}</div>
          <div class="stat-sub">近 7 天下载 {{ totalTrendCount }} 次</div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">有效分享</div>
          <div class="stat-value">{{ stats.activeShares || 0 }}</div>
          <div class="stat-sub">当前可访问的分享链接数</div>
        </div>
      </el-col>
    </el-row>

    <!-- 近 7 天下载趋势 -->
    <div class="trend-panel">
      <div class="trend-header">
        <span class="trend-title">近 7 天下载趋势</span>
        <span class="trend-tip">按天统计下载次数</span>
      </div>
      <div class="trend-chart">
        <div v-for="item in stats.downloadTrend" :key="item.date" class="trend-item">
          <div class="bar-wrap">
            <div class="bar" :style="{ height: barHeight(item.downloadCount) }"></div>
          </div>
          <div class="bar-count">{{ item.downloadCount }}</div>
          <div class="bar-date">{{ item.date.slice(5) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboardStats } from '@/api/admin'

const loading = ref(false)
const stats = ref({})

const spacePercentage = computed(() => {
  const total = stats.value.totalSpace || 0
  if (!total) return 0
  return Math.min(100, Math.round(((stats.value.usedSpace || 0) / total) * 100))
})

const totalTrendCount = computed(() =>
  (stats.value.downloadTrend || []).reduce((sum, d) => sum + d.downloadCount, 0)
)

const fetchStats = async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    stats.value = res.data || {}
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 柱状图高度：按最大值等比缩放，最小 8% 保证可见
const barHeight = (count) => {
  const max = Math.max(...(stats.value.downloadTrend || []).map((d) => d.downloadCount), 1)
  const ratio = max > 0 ? count / max : 0
  return `${Math.max(8, Math.round(ratio * 100))}%`
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(fetchStats)
</script>

<style scoped lang="scss">
.admin-dashboard {
  h2 {
    font-size: 18px;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 16px;
  }

  .stat-cards {
    .stat-card {
      background: #fff;
      border: 1px solid $border-light;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 16px;

      .stat-label {
        font-size: 13px;
        color: $text-secondary;
      }

      .stat-value {
        margin-top: 8px;
        font-size: 24px;
        font-weight: 600;
        color: $text-primary;
      }

      .stat-sub {
        margin-top: 4px;
        font-size: 12px;
        color: $text-secondary;
      }

      .stat-progress {
        margin-top: 10px;
      }
    }
  }

  .trend-panel {
    background: #fff;
    border: 1px solid $border-light;
    border-radius: 8px;
    padding: 20px;

    .trend-header {
      display: flex;
      align-items: baseline;
      gap: 12px;
      margin-bottom: 20px;

      .trend-title {
        font-size: 15px;
        font-weight: 500;
        color: $text-primary;
      }

      .trend-tip {
        font-size: 12px;
        color: $text-secondary;
      }
    }

    .trend-chart {
      display: flex;
      align-items: flex-end;
      justify-content: space-around;
      height: 220px;
      padding: 0 8px;

      .trend-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        flex: 1;
        max-width: 72px;

        .bar-wrap {
          display: flex;
          align-items: flex-end;
          height: 160px;
          width: 100%;

          .bar {
            width: 100%;
            background: $primary;
            border-radius: 4px 4px 0 0;
            transition: height 0.3s;

            &:hover {
              background: $primary-dark;
            }
          }
        }

        .bar-count {
          margin-top: 6px;
          font-size: 12px;
          color: $text-regular;
        }

        .bar-date {
          margin-top: 2px;
          font-size: 12px;
          color: $text-secondary;
        }
      }
    }
  }
}
</style>
