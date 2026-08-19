<template>
  <div class="admin-config">
    <div class="config-header">
      <div>
        <h2>系统配置</h2>
        <p class="tip">修改后实时生效，无需重启服务；新增配置需在代码中接入后才真正生效</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAdd">新增配置</el-button>
    </div>

    <el-table :data="configList" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column prop="configKey" label="配置键" width="220">
        <template #default="{ row }">
          <code class="config-key">{{ row.configKey }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="configValue" label="配置值" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && configList.length === 0" description="暂无配置项" />

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑配置' : '新增配置'"
      width="460px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" @submit.prevent>
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如 max_user_limit" :disabled="!!editing" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="配置项用途说明（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listAllConfigs, updateConfig, addConfig, deleteConfig } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const configList = ref([])
const dialogVisible = ref(false)
const editing = ref(null) // null=新增，对象=编辑
const formRef = ref()

const form = reactive({
  configKey: '',
  configValue: '',
  description: '',
})

const rules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAllConfigs()
    configList.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  editing.value = null
  dialogVisible.value = true
}

const openEdit = (row) => {
  editing.value = row
  form.configKey = row.configKey
  form.configValue = row.configValue
  form.description = row.description || ''
  dialogVisible.value = true
}

const resetForm = () => {
  editing.value = null
  form.configKey = ''
  form.configValue = ''
  form.description = ''
  formRef.value?.resetFields()
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    if (editing.value) {
      await updateConfig(editing.value.configKey, {
        configValue: form.configValue,
        description: form.description,
      })
      ElMessage.success('修改成功，已实时生效')
    } else {
      await addConfig({
        configKey: form.configKey,
        configValue: form.configValue,
        description: form.description,
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (err) {
    console.error(err)
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置项「${row.configKey}」吗？删除后相关功能将使用默认值`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteConfig(row.configKey)
    ElMessage.success('已删除')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.admin-config {
  .config-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;

    h2 {
      font-size: 18px;
      font-weight: 500;
      color: $text-primary;
    }

    .tip {
      margin-top: 4px;
      font-size: 12px;
      color: $text-secondary;
    }
  }

  .config-key {
    padding: 2px 6px;
    background: $primary-light;
    color: $primary-dark;
    border-radius: 4px;
    font-size: 13px;
  }
}
</style>
