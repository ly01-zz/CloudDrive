<template>
  <div class="upload-progress" v-if="tasks.length > 0">
    <div class="progress-header">
      <span>上传任务</span>
      <span class="summary">{{ doneCount }}/{{ tasks.length }} 已完成</span>
      <el-button type="primary" link @click="$emit('clear')" v-if="allDone">清空记录</el-button>
    </div>

    <div v-for="task in tasks" :key="task.id" class="progress-item">
      <div class="item-top">
        <span class="file-name" :title="task.name">
          <el-icon><Document /></el-icon>
          {{ task.name }}
        </span>
        <span class="item-status" :class="task.status">
          {{ statusText(task) }}
        </span>
      </div>
      <el-progress
        :percentage="task.percent"
        :status="progressStatus(task)"
        :stroke-width="4"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Document } from '@element-plus/icons-vue'

const props = defineProps({
  tasks: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['clear'])

const doneCount = computed(() => props.tasks.filter(t => t.status === 'success').length)
const allDone = computed(() => doneCount.value === props.tasks.length)

// 状态文案：上传中显示百分比，成功/失败显示文字
const statusText = (task) => {
  if (task.status === 'success') return '上传成功'
  if (task.status === 'error') return '上传失败'
  return `上传中 ${task.percent}%`
}

// 进度条状态：成功绿色对勾，失败红色感叹号，上传中蓝色
const progressStatus = (task) => {
  if (task.status === 'success') return 'success'
  if (task.status === 'error') return 'exception'
  return ''
}
</script>

<style scoped lang="scss">
.upload-progress {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 360px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 16px;
  z-index: 100;

  .progress-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 12px;

    .summary {
      flex: 1;
      font-size: 12px;
      color: $text-secondary;
      font-weight: normal;
    }
  }

  .progress-item {
    margin-bottom: 12px;

    .item-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 4px;

      .file-name {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: $text-regular;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 200px;
      }

      .item-status {
        font-size: 12px;
        flex-shrink: 0;

        &.success {
          color: $success;
        }

        &.error {
          color: $danger;
        }

        &:not(.success):not(.error) {
          color: $primary;
        }
      }
    }

    // 进度条过渡动画，避免进度跳变生硬
    :deep(.el-progress-bar__inner) {
      transition: width 0.3s ease;
    }
  }
}
</style>
