import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/AuthLayout.vue'),
    meta: { public: true },
    children: [
      {
        path: '',
        redirect: '/login',
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
      },
    ],
  },
  {
    path: '/s/:shareCode',
    name: 'ShareAccess',
    component: () => import('@/views/ShareAccess.vue'),
    meta: { public: true },
  },
  {
    path: '/app',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/app/files',
    children: [
      {
        path: 'files',
        name: 'Files',
        component: () => import('@/views/Files.vue'),
      },
      {
        path: 'applications',
        name: 'Applications',
        component: () => import('@/views/Applications.vue'),
      },
      {
        path: 'shares',
        name: 'Shares',
        component: () => import('@/views/Shares.vue'),
      },
      {
        path: 'recycle',
        name: 'Recycle',
        component: () => import('@/views/Recycle.vue'),
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { admin: true },
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { admin: true },
      },
      {
        path: 'admin/approvals',
        name: 'AdminApprovals',
        component: () => import('@/views/admin/Approvals.vue'),
        meta: { admin: true },
      },
      {
        path: 'admin/shares',
        name: 'AdminShares',
        component: () => import('@/views/admin/Shares.vue'),
        meta: { admin: true },
      },
      {
        path: 'admin/files',
        name: 'AdminFiles',
        component: () => import('@/views/admin/Files.vue'),
        meta: { admin: true },
      },
      {
        path: 'admin/logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/Logs.vue'),
        meta: { admin: true },
      },
      {
        path: 'admin/announcement',
        name: 'AdminAnnouncement',
        component: () => import('@/views/admin/Announcement.vue'),
        meta: { admin: true },
      },
      {
        path: 'admin/config',
        name: 'AdminConfig',
        component: () => import('@/views/admin/Config.vue'),
        meta: { admin: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/errors/404.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  userStore.init()

  if (!to.meta.public && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.admin && !userStore.isAdmin) {
    next('/app/files')
    return
  }

  // 已登录：每次进入页面都向后端同步最新数据（空间/流量，异步不阻塞跳转）
  // 解决页面刷新后 localStorage 快照过旧、流量/空间显示不准的问题
  if (userStore.isLoggedIn && !to.meta.public) {
    userStore.refreshInfo()
  }

  next()
})

export default router
