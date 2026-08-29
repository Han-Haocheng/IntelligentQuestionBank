import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// base: './' 使打包后的 dist 可以被 Electron 以 file:// 方式加载
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入: 组件与样式随用随载, 显著减小打包体积
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] })
  ],
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
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          // 仅固定基础运行时; element-plus/echarts 已按需引入, 不再强制整包进单 chunk(避免破坏摇树)
          vue: ['vue', 'vue-router', 'pinia', 'axios']
        }
      }
    }
  }
})
