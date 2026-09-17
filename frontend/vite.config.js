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
  // 开发期依赖预打包: 本项目的 EP 组件由 unplugin-vue-components 在 **transform 阶段**注入按需 import,
  // 而依赖扫描器(esbuild)看不到插件注入的 import ⇒ 运行期才发现新依赖 ⇒
  // Vite 重新预打包并整页重载("[vite] optimized dependencies changed. reloading")。
  // 处置: holdUntilCrawlEnd 让首轮优化等扫描收敛; server.warmup 在启动阶段就转换全部视图/组件,
  // 把依赖发现集中在启动那一轮, 避免浏览过程中反复重载。
  optimizeDeps: {
    holdUntilCrawlEnd: true,
    include: ['@element-plus/icons-vue', 'echarts']
  },
  server: {
    port: 5173,
    warmup: {
      clientFiles: [
        './src/main.js',
        './src/App.vue',
        './src/router/index.js',
        './src/views/*.vue',
        './src/components/*.vue'
      ]
    },
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
