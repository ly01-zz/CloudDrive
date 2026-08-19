<template>
  <el-dialog
    v-model="visible"
    title="新建文件夹"
    width="400px"
    :close-on-click-modal="false"
    @close="resetForm"
  >
    <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent
>
      <el-form-item prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入文件夹名称"
          maxlength="255"
          show-word-limit
          autofocus
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createFolder } from '@/api/file'

const props = defineProps(['modelValue', 'parentId'])
const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const form = reactive({
  name: '',
})

const rules = {
  name: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { max: 255, message: '名称长度不能超过255个字符', trigger: 'blur' },
  ],
}

const resetForm = () => {
  form.name = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    await createFolder({
      name: form.name,
      parentId: props.parentId || 0,
    })
    ElMessage.success('创建成功')
    visible.value = false
    emit('success')
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>
