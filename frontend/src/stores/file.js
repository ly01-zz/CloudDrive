import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useFileStore = defineStore('file', () => {
  const currentParentId = ref(0)
  const breadcrumb = ref([{ id: 0, name: '全部文件' }])

  const setParentId = (id) => {
    currentParentId.value = id
  }

  const pushBreadcrumb = (item) => {
    const index = breadcrumb.value.findIndex((b) => b.id === item.id)
    if (index === -1) {
      breadcrumb.value.push(item)
    } else {
      breadcrumb.value = breadcrumb.value.slice(0, index + 1)
    }
  }

  return {
    currentParentId,
    breadcrumb,
    setParentId,
    pushBreadcrumb,
  }
})
