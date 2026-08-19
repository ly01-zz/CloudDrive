<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item
      v-for="(item, index) in fileStore.breadcrumb"
      :key="item.id"
      :class="{ 'is-last': index === fileStore.breadcrumb.length - 1 }"
      @click="jumpTo(index)"
    >
      {{ item.name }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useFileStore } from '@/stores/file'

const router = useRouter()
const fileStore = useFileStore()

const jumpTo = (index) => {
  const item = fileStore.breadcrumb[index]
  fileStore.breadcrumb = fileStore.breadcrumb.slice(0, index + 1)
  fileStore.setParentId(item.id)
  router.push({ path: '/app/files', query: { parentId: item.id } })
}
</script>

<style scoped lang="scss">
:deep(.el-breadcrumb__item) {
  cursor: pointer;

  &.is-last {
    cursor: default;
    font-weight: 500;
    color: $text-primary;
  }

  &:not(.is-last):hover {
    color: $primary;
  }
}
</style>
