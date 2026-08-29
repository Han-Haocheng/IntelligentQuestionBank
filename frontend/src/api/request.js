import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 开发环境走 vite 代理(/api), 打包后(Electron)直连后端, 可用 localStorage 覆盖
function resolveBase () {
  const custom = localStorage.getItem('qbank_api_base')
  if (custom) return custom.replace(/\/+$/, '')
  if (import.meta.env.DEV) return '/api'
  return 'http://localhost:8080/api'
}

const request = axios.create({
  baseURL: resolveBase(),
  timeout: 60000
})

request.interceptors.request.use((config) => {
  // 每次请求动态解析后端地址: 支持登录页随时切换远程后端, 无需刷新
  config.baseURL = resolveBase()
  const token = localStorage.getItem('qbank_token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res.data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('qbank_token')
      localStorage.removeItem('qbank_user')
      // 同步清空 Pinia 内存态, 避免残留过期角色(动态引入避免循环依赖)
      import('../stores/user').then(({ useUserStore }) => {
        const store = useUserStore()
        store.token = ''
        store.userInfo = null
      }).catch(() => {})
      ElMessage.error('未登录或登录已过期')
      router.push('/login')
    } else if (error.response && error.response.status === 403) {
      ElMessage.error('无权限执行该操作')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
