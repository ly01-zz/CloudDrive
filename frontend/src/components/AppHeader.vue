<template>
  <header class="app-header">
    <div class="header-left">
      <BreadcrumbNav v-if="$route.path === '/app/files'" />
      <span v-else class="page-title">{{ pageTitle }}</span>
    </div>

    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :icon="UserFilled" />
          <span class="nickname">{{ userStore.userInfo?.nickname || '用户' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 个人信息弹窗 -->
    <UserProfileDialog v-model="profileVisible" />

    <!-- 修改密码弹窗 -->
    <UpdatePasswordDialog v-model="passwordVisible" />
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import BreadcrumbNav from './BreadcrumbNav.vue'
import UpdatePasswordDialog from './UpdatePasswordDialog.vue'
import UserProfileDialog from './UserProfileDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const passwordVisible = ref(false)
const profileVisible = ref(false)

const pageTitle = computed(() => {
  const map = {
    '/app/applications': '我的申请',
    '/app/recycle': '回收站',
    '/app/admin/users': '用户管理',
    '/app/admin/approvals': '扩容审批',
  }
  return map[route.path] || ''
})

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    } catch {
      // cancel
    }
  } else if (cmd === 'password') {
    passwordVisible.value = true
  } else if (cmd === 'profile') {
    profileVisible.value = true
  }
}
</script>

<style scoped lang="scss">
.app-header {
  height: $header-height;
  background: #fff;
  border-bottom: 1px solid $border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;

  .header-left {
    .page-title {
      font-size: 18px;
      font-weight: 500;
      color: $text-primary;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 8px;
      transition: background 0.2s;

      &:hover {
        background: $bg-gray;
      }

      .nickname {
        font-size: 14px;
        color: $text-primary;
      }
    }
  }
}
</style>
