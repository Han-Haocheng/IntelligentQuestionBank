import { defineStore } from 'pinia'
import request from '../api/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('qbank_token') || '',
    userInfo: JSON.parse(localStorage.getItem('qbank_user') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => !!state.userInfo && state.userInfo.role === 0
  },
  actions: {
    saveLogin (token, user) {
      this.token = token
      this.userInfo = user
      localStorage.setItem('qbank_token', token)
      localStorage.setItem('qbank_user', JSON.stringify(user))
    },
    async fetchInfo () {
      const user = await request.get('/user/info')
      this.userInfo = user
      localStorage.setItem('qbank_user', JSON.stringify(user))
      return user
    },
    logout () {
      // 先同步取出 token 手动注入请求头: axios 请求拦截器在微任务中才读取 localStorage,
      // 若先清空本地状态, logout 请求将不带 Authorization 而收到 401,
      // 误弹「未登录或登录已过期」(与「已退出登录」同时出现)
      const token = this.token || localStorage.getItem('qbank_token') || ''
      request.post('/user/logout', null, {
        headers: { Authorization: 'Bearer ' + token },
        silent401: true // 主动退出: token 已失效时不弹「未登录或登录已过期」
      }).catch(() => {})
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('qbank_token')
      localStorage.removeItem('qbank_user')
    }
  }
})