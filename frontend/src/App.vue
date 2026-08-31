<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { onMounted } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn.mjs'
import { useThemeStore } from './stores/theme'

// 应用启动即应用主题: 本地缓存立即生效, 再拉取管理员全局默认(登录页同样生效)
onMounted(() => {
  useThemeStore().boot()
})
</script>