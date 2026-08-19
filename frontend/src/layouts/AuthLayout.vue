<template>
  <div class="auth-layout">
    <div class="auth-box">
      <div class="auth-header">
        <div class="logo">
          <el-icon><Cloudy /></el-icon>
          <span>云盘</span>
        </div>
        <p class="subtitle">安全、快速的个人云存储</p>
      </div>
      <router-view />
    </div>
    <div class="auth-bg"></div>
  </div>
</template>

<script setup>
import { Cloudy } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.auth-layout {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #e0ecff 0%, #f5f7fa 100%);

  .auth-box {
    width: 420px;
    margin: auto;
    padding: 40px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(64, 158, 255, 0.12);
    z-index: 1;
  }

  .auth-header {
    text-align: center;
    margin-bottom: 32px;

    .logo {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      font-size: 28px;
      font-weight: 600;
      color: $primary;

      .el-icon {
        font-size: 36px;
      }
    }

    .subtitle {
      margin-top: 8px;
      color: $text-secondary;
      font-size: 14px;
    }
  }

  .auth-bg {
    position: fixed;
    inset: 0;
    background:
      radial-gradient(circle at 20% 30%, rgba(64,158,255,0.08) 0%, transparent 50%),
      radial-gradient(circle at 80% 70%, rgba(64,158,255,0.06) 0%, transparent 50%);
    pointer-events: none;
  }
}
</style>
