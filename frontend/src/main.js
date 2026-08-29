import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles.css'

// Element Plus 组件/样式由 unplugin-vue-components 按需引入;
// 以下为编程式组件(ElMessage/ElMessageBox/v-loading)的样式
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'

const app = createApp(App)

// 图标体积较小, 保留全局注册(各视图直接用 <el-icon><DataLine/></el-icon>)
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.use(createPinia())
app.use(router)
app.mount('#app')
