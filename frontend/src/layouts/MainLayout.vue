<template>
  <div class="main-layout">
    <AppSidebar />
    <div class="main-content">
      <AppHeader />
      <div class="page-container">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import { getLatestAnnouncement } from '@/api/admin'

// 公告为广播通知，不记录已读：
// - localStorage 记录"已弹过的公告 ID"，弹过一次后刷新页面不再弹（登录期间只弹一次）
// - 登录成功时 Login.vue 会清除该标记，重新登录后再次弹出
const SEEN_KEY = 'announcement_seen_id'

onMounted(async () => {
  try {
    const res = await getLatestAnnouncement()
    const announcement = res.data
    if (!announcement) return
    // 本次登录已弹过该公告 → 不再弹
    if (localStorage.getItem(SEEN_KEY) === String(announcement.id)) return
    // 弹窗后记录已弹的公告 ID
    localStorage.setItem(SEEN_KEY, String(announcement.id))
    ElMessageBox.alert(announcement.content, `📢 ${announcement.title}`, {
      confirmButtonText: '知道了',
    })
  } catch (err) {
    console.error('获取公告失败：', err)
  }
})
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;

  .main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .page-container {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
    background: $bg-gray;
  }
}
</style>
