<template>
  <el-dialog
    v-model="visible"
    title="个人信息"
    width="460px"
    :close-on-click-modal="false"
  >
    <div class="profile-header">
      <el-avatar :size="56" :icon="UserFilled" />
      <div class="profile-basic">
        <div class="nickname">{{ form.nickname || '未设置' }}</div>
        <div class="phone">{{ userStore.userInfo?.phone || '-' }}</div>
      </div>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" class="profile-form">
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="50" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱（可选）" />
      </el-form-item>
      <el-form-item label="角色">
        <el-tag :type="userStore.isAdmin ? 'danger' : ''">
          {{ userStore.isAdmin ? '管理员' : '普通用户' }}
        </el-tag>
      </el-form-item>
      <el-form-item label="存储空间">
        <div class="storage-info">
          <el-progress
            :percentage="storagePercent"
            :stroke-width="8"
            color="#409EFF"
          />
          <span class="storage-text">{{ usedText }} / {{ totalText }}</span>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// 每次打开弹窗时回填最新用户信息
watch(visible, (val) => {
  if (val) initForm()
})

const form = reactive({
  nickname: '',
  email: '',
})

const rules = {
  nickname: [{ max: 50, message: '昵称长度不能超过50个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

// 打开弹窗时回填当前信息
const initForm = () => {
  form.nickname = userStore.userInfo?.nickname || ''
  form.email = userStore.userInfo?.email || ''
}

const used = computed(() => userStore.userInfo?.usedSpace || 0)
const total = computed(() => userStore.userInfo?.totalSpace || 0)

const storagePercent = computed(() => {
  if (!total.value) return 0
  return Math.min(100, Math.round((used.value / total.value) * 100))
})

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const usedText = computed(() => formatSize(used.value))
const totalText = computed(() => formatSize(total.value))

const handleSave = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    const res = await updateProfile({
      nickname: form.nickname || null,
      email: form.email || null,
    })
    // 更新 store 中的用户信息（保留 role/token 等字段）
    // 后端正常情况下会返回更新后的 UserProfileVo；若为空则用表单值兜底，确保页面即时刷新
    const updatedData = res.data || {
      nickname: form.nickname,
      email: form.email,
    }
    userStore.setUserInfo({
      ...userStore.userInfo,
      ...updatedData,
    })
    ElMessage.success('个人信息已更新')
    visible.value = false
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0 20px;
  border-bottom: 1px solid $border-light;
  margin-bottom: 20px;

  .profile-basic {
    .nickname {
      font-size: 18px;
      font-weight: 600;
      color: $text-primary;
    }

    .phone {
      margin-top: 4px;
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

.profile-form {
  .storage-info {
    width: 100%;

    .storage-text {
      display: block;
      margin-top: 6px;
      font-size: 12px;
      color: $text-secondary;
    }
  }
}
</style>
