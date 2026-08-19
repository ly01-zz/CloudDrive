<template>
  <el-dialog
    v-model="visible"
    title="分享文件"
    width="480px"
    :close-on-click-modal="false"
    @close="resetForm"
  >
    <!-- 分享表单 -->
    <template v-if="!result">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" @submit.prevent>
        <!-- 文件信息 -->
        <div class="file-info">
          <el-icon :size="20" class="file-icon"><Document /></el-icon>
          <span class="file-name">{{ file?.name }}</span>
          <span class="file-size">{{ formatSize(file?.fileSize) }}</span>
        </div>

        <el-form-item label="分享类型" prop="shareType">
          <el-radio-group v-model="form.shareType">
            <el-radio :value="0">公开分享</el-radio>
            <el-radio :value="1">私密分享</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.shareType === 1" label="提取码" prop="extractCode">
          <el-input
            v-model="form.extractCode"
            placeholder="留空将自动生成4位数字提取码"
            maxlength="6"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="有效期" prop="expireDays">
          <el-select v-model="form.expireDays" style="width: 100%">
            <el-option label="1 天" :value="1" />
            <el-option label="7 天" :value="7" />
            <el-option label="30 天" :value="30" />
            <el-option label="365 天" :value="365" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">高级选项（留空表示不限）</el-divider>

        <el-form-item label="访问上限" prop="maxVisits">
          <el-input-number v-model="form.maxVisits" :min="1" :max="999999" :controls="false" placeholder="最大访问次数" />
        </el-form-item>

        <el-form-item label="下载上限" prop="maxDownloads">
          <el-input-number v-model="form.maxDownloads" :min="1" :max="999999" :controls="false" placeholder="最大下载次数" />
        </el-form-item>

        <el-form-item label="流量上限" prop="maxDownloadSizeMb">
          <el-input-number v-model="form.maxDownloadSizeMb" :min="1" :max="999999" :controls="false" placeholder="最大下载流量（MB）" />
        </el-form-item>
      </el-form>
    </template>

    <!-- 分享结果 -->
    <template v-else>
      <div class="share-result">
        <el-result icon="success" title="分享创建成功" sub-title="复制链接发给好友即可下载">
          <template #extra>
            <div class="result-links">
              <div class="link-item">
                <span class="link-label">分享链接</span>
                <el-input :model-value="result.shareUrl" readonly />
                <el-button type="primary" link @click="copyText(result.shareUrl, '链接已复制')">
                  复制链接
                </el-button>
              </div>
              <div v-if="result.extractCode" class="link-item">
                <span class="link-label">提取码</span>
                <el-input :model-value="result.extractCode" readonly />
                <el-button type="primary" link @click="copyText(result.extractCode, '提取码已复制')">
                  复制提取码
                </el-button>
              </div>
            </div>
          </template>
        </el-result>
      </div>
    </template>

    <template #footer>
      <template v-if="!result">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">创建分享</el-button>
      </template>
      <el-button v-else type="primary" @click="visible = false">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createShare } from '@/api/share'

const props = defineProps(['modelValue', 'file'])
const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const loading = ref(false)
const result = ref(null)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const form = reactive({
  shareType: 0,          // 0-公开，1-私密
  extractCode: '',
  expireDays: 7,
  maxVisits: null,
  maxDownloads: null,
  maxDownloadSizeMb: null,
})

const rules = {
  extractCode: [
    {
      validator: (rule, value, callback) => {
        if (!value) return callback()
        if (value.length < 4 || value.length > 6) {
          callback(new Error('提取码长度需为4~6位'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const resetForm = () => {
  form.shareType = 0
  form.extractCode = ''
  form.expireDays = 7
  form.maxVisits = null
  form.maxDownloads = null
  form.maxDownloadSizeMb = null
  result.value = null
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    const res = await createShare({
      fileId: props.file?.id,
      shareType: form.shareType,
      expireDays: form.expireDays,
      extractCode: form.shareType === 1 ? form.extractCode || undefined : undefined,
      maxVisits: form.maxVisits || undefined,
      maxDownloads: form.maxDownloads || undefined,
      maxDownloadSize: form.maxDownloadSizeMb ? form.maxDownloadSizeMb * 1024 * 1024 : undefined,
    })
    const link = res.data
    // 组装分享链接（当前站点地址 + 分享码路径）
    result.value = {
      shareUrl: `${window.location.origin}/s/${link.shareCode}`,
      extractCode: link.extractCode,
    }
    emit('success')
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
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

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped lang="scss">
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  margin-bottom: 16px;
  background: $bg-gray;
  border-radius: 8px;

  .file-icon {
    color: $primary;
  }

  .file-name {
    font-size: 14px;
    color: $text-primary;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .file-size {
    font-size: 12px;
    color: $text-secondary;
  }
}

.share-result {
  .result-links {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;

    .link-item {
      display: flex;
      align-items: center;
      gap: 8px;

      .link-label {
        font-size: 13px;
        color: $text-secondary;
        white-space: nowrap;
      }
    }
  }
}
</style>
