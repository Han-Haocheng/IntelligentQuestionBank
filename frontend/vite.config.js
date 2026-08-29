import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// base: './' 使打包后的 dist 可以被 Electron 以 file:// 方式加载
export default defineConfig({
  plugins: [vue()],
  base: './',
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 开发模式下前端本地 AI 走此代理规避浏览器 CORS
      '/ai-proxy': {
        target: 'https://api.deepseek.com',
        changeOrigin: true,
        rewrite: (p) => p.substring('/ai-proxy'.length)
      }
    }
  },
  build: {
    // echarts/element-plus 本身体积较大, 拆分后单 chunk 仍 >500kB, 提高阈值避免噪音告警
    chunkSizeWarningLimit: 1500,
    // 拆分大依赖, 业务代码改动不会使三方库缓存失效
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia', 'axios'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          echarts: ['echarts']
        }
      }
    }
  }
})
