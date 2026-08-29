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
      request.post('/user/logout').catch(() => {})
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('qbank_token')
      localStorage.removeItem('qbank_user')
    }
  }
})
