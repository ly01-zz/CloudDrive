<template>
  <div class="not-found">
    <el-result status="404" title="404" sub-title="抱歉，您访问的页面不存在">
      <template #extra>
        <el-button type="primary" @click="$router.push('/app/files')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>
