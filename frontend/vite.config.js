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
      }
    }
  }
})
