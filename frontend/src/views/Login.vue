<template>
  <div class="login-page">
    <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleLogin">
      <el-form-item prop="phone">
        <el-input
          v-model="form.phone"
          placeholder="请输入手机号"
          size="large"
          :prefix-icon="Phone"
          clearable
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          size="large"
          :prefix-icon="Lock"
          show-password
          clearable
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          @click="handleLogin"
          style="width: 100%"
        >
          登 录
        </el-button>
      </el-form-item>
    </el-form>

    <div class="auth-footer">
      <span>还没有账号？</span>
      <router-link to="/register">立即注册</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Phone, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    const res = await login(form)
    userStore.setToken(res.data?.token || '')
    userStore.setUserInfo(res.data)
    // 清除公告已弹标记：每次登录后重新弹一次公告
    localStorage.removeItem('announcement_seen_id')
    ElMessage.success('登录成功')
    router.push('/app/files')
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  .auth-footer {
    margin-top: 16px;
    text-align: center;
    font-size: 14px;
    color: $text-regular;

    a {
      color: $primary;
      margin-left: 4px;

      &:hover {
        color: $primary-dark;
      }
    }
  }
}
</style>
