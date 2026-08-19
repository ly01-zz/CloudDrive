<template>
  <div class="share-access">
    <div class="share-box" v-loading="loading">
      <!-- 头部 -->
      <div class="share-header">
        <el-icon><Cloudy /></el-icon>
        <span>云盘分享</span>
      </div>

      <!-- 分享无效/已失效 -->
      <div v-if="errorMsg" class="share-error">
        <el-result icon="error" title="无法访问该分享" :sub-title="errorMsg">
          <template #extra>
            <el-button type="primary" @click="goHome">返回云盘</el-button>
          </template>
        </el-result>
      </div>

      <!-- 分享信息 -->
      <div v-else-if="info" class="share-info">
        <div class="file-preview">
          <el-icon :size="56" class="file-icon"><Document /></el-icon>
          <div class="file-name" :title="info.fileName">{{ info.fileName }}</div>
          <div class="file-size">{{ info.fileSizeDesc }}</div>
        </div>

        <el-divider />

        <div class="info-row">
          <span class="label">分享类型</span>
          <span class="value">
            <el-tag v-if="info.needExtract" type="warning" size="small">私密分享</el-tag>
            <el-tag v-else type="success" size="small">公开分享</el-tag>
          </span>
        </div>
        <div class="info-row">
          <span class="label">有效期限</span>
          <span class="value">{{ info.expireTime ? formatTime(info.expireTime) : '永久有效' }}</span>
        </div>
        <div class="info-row">
          <span class="label">已被访问</span>
          <span class="value">{{ info.totalVisits || 0 }} 次</span>
        </div>

        <!-- 私密分享：提取码输入 -->
        <div v-if="info.needExtract" class="extract-input">
          <el-input
            v-model="extractCode"
            placeholder="请输入提取码"
            maxlength="6"
            size="large"
            @keyup.enter="handleDownload"
          />
        </div>

        <el-button
          type="primary"
          size="large"
          class="download-btn"
          :loading="downloading"
          @click="handleDownload"
        >
          <el-icon><Download /></el-icon>&nbsp;下载文件
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Cloudy, Document, Download } from '@element-plus/icons-vue'
import { getShareInfo, downloadShare } from '@/api/share'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const downloading = ref(false)
const info = ref(null)
const errorMsg = ref('')
const extractCode = ref('')

const fetchShareInfo = async () => {
  loading.value = true
  errorMsg.value = ''
  info.value = null
  extractCode.value = ''
  try {
    const res = await getShareInfo(route.params.shareCode)
    info.value = res.data
  } catch (err) {
    errorMsg.value = err.message || '分享不存在或已失效'
  } finally {
    loading.value = false
  }
}

const handleDownload = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('下载文件需要登录，请先登录')
    router.push('/login')
    return
  }
  if (info.value?.needExtract && !extractCode.value) {
    ElMessage.warning('请输入提取码')
    return
  }
  downloading.value = true
  try {
    const res = await downloadShare(route.params.shareCode, extractCode.value || undefined)
    const { downloadUrl, fileName } = res.data
    // 创建一个临时 a 标签触发下载
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = fileName
    a.target = '_blank'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    ElMessage.success('开始下载')
    // 下载会扣减月度流量，同步侧边栏数据
    userStore.refreshInfo()
  } catch (err) {
    console.error(err)
  } finally {
    downloading.value = false
  }
}

const goHome = () => {
  if (userStore.isLoggedIn) {
    router.push('/app/files')
  } else {
    router.push('/login')
  }
}

const formatTime = (iso) => {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN')
}

watch(() => route.params.shareCode, fetchShareInfo)
onMounted(fetchShareInfo)
</script>

<style scoped lang="scss">
.share-access {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #e0ecff 0%, #f5f7fa 100%);

  .share-box {
    width: 440px;
    margin: auto;
    padding: 40px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(64, 158, 255, 0.12);
  }

  .share-header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin-bottom: 24px;
    font-size: 22px;
    font-weight: 600;
    color: $primary;

    .el-icon {
      font-size: 28px;
    }
  }

  .share-error {
    padding: 20px 0;
  }

  .share-info {
    .file-preview {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 8px 0;

      .file-icon {
        color: $primary;
        margin-bottom: 12px;
      }

      .file-name {
        font-size: 16px;
        font-weight: 500;
        color: $text-primary;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .file-size {
        margin-top: 6px;
        font-size: 13px;
        color: $text-secondary;
      }
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 6px 0;
      font-size: 14px;

      .label {
        color: $text-secondary;
      }

      .value {
        color: $text-primary;
      }
    }

    .extract-input {
      margin: 16px 0 4px;
    }

    .download-btn {
      width: 100%;
      margin-top: 20px;
    }
  }
}
</style>
