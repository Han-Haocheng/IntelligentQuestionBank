import { defineStore } from 'pinia'
import { themeApi } from '../api'

// 用户个人选择的主题标识(localStorage)
const THEME_KEY = 'qbank_theme_key'
// 最近一次生效主题的完整配置缓存: 启动时无需等网络请求即可渲染一致外观
const THEME_CACHE = 'qbank_theme_cache'

// 内置兜底主题(后端无任何主题时使用, 与老版本外观一致)
export const DEFAULT_THEME = {
  name: '默认蓝',
  themeKey: 'default',
  config: {
    primary: '#409eff',
    pageBg: '#f5f7fa',
    cardBg: '#ffffff',
    headerBg: '#ffffff',
    headerText: '#303133',
    asideBg: '#001529',
    asideText: '#a6adb4',
    asideActive: '#ffffff',
    loginFrom: '#1f6feb',
    loginTo: '#6e40c9',
    radius: '4'
  }
}

/** 解析主题配置字符串为对象(与默认值合并, 缺省字段自动补齐) */
export function parseThemeConfig (config, fallback = DEFAULT_THEME.config) {
  let c = {}
  if (config) {
    try { c = typeof config === 'string' ? JSON.parse(config) : config } catch (e) { c = {} }
  }
  return { ...fallback, ...c }
}

/** #rgb/#rrggbb 与白色/黑色按比例混合, 生成 Element Plus 主色衍生色 */
function mix (hex, target, ratio) {
  const m = hex.replace('#', '')
  const full = m.length === 3 ? m.split('').map(ch => ch + ch).join('') : m
  const num = parseInt(full, 16)
  const r = (num >> 16) & 255
  const g = (num >> 8) & 255
  const b = num & 255
  const tr = (parseInt(target.replace('#', ''), 16) >> 16) & 255
  const tg = (parseInt(target.replace('#', ''), 16) >> 8) & 255
  const tb = parseInt(target.replace('#', ''), 16) & 255
  const mix2 = (c, t) => Math.round(c + (t - c) * ratio)
  const hex2 = (v) => v.toString(16).padStart(2, '0')
  return '#' + hex2(mix2(r, tr)) + hex2(mix2(g, tg)) + hex2(mix2(b, tb))
}

/** 把主题配置写入 <html> 的 CSS 变量(含 Element Plus 主色体系), 并缓存到本机 */
function applyConfig (config, themeKey) {
  const cfg = parseThemeConfig(config)
  const el = document.documentElement
  const set = (name, value) => el.style.setProperty(name, value)
  set('--q-primary', cfg.primary)
  set('--q-page-bg', cfg.pageBg)
  set('--q-card-bg', cfg.cardBg)
  set('--q-header-bg', cfg.headerBg)
  set('--q-header-text', cfg.headerText)
  set('--q-aside-bg', cfg.asideBg)
  set('--q-aside-text', cfg.asideText)
  set('--q-aside-active', cfg.asideActive)
  set('--q-login-from', cfg.loginFrom)
  set('--q-login-to', cfg.loginTo)
  set('--q-radius', cfg.radius + 'px')
  // Element Plus 主色及衍生色(light-3/5/7/8/9 与 dark-2), 按钮/链接/选中态随之变化
  set('--el-color-primary', cfg.primary)
  set('--el-color-primary-light-3', mix(cfg.primary, '#ffffff', 0.3))
  set('--el-color-primary-light-5', mix(cfg.primary, '#ffffff', 0.5))
  set('--el-color-primary-light-7', mix(cfg.primary, '#ffffff', 0.7))
  set('--el-color-primary-light-8', mix(cfg.primary, '#ffffff', 0.8))
  set('--el-color-primary-light-9', mix(cfg.primary, '#ffffff', 0.9))
  set('--el-color-primary-dark-2', mix(cfg.primary, '#000000', 0.2))
  set('--el-border-radius-base', cfg.radius + 'px')
  try {
    localStorage.setItem(THEME_CACHE, JSON.stringify({ themeKey, config: cfg }))
  } catch (e) {}
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    active: null, // 管理员设置的全局默认主题(公开接口拉取)
    enabled: [],  // 启用的主题列表(登录后拉取, 供切换)
    userKey: localStorage.getItem(THEME_KEY) || '' // 个人选择, 空=跟随管理员默认
  }),
  getters: {
    /** 当前生效主题对象(个人选择 > 全局默认 > 内置兜底) */
    current (state) {
      const mine = state.enabled.find(t => t.themeKey === state.userKey)
      if (mine) return mine
      if (state.active && state.active.themeKey) return state.active
      return DEFAULT_THEME
    },
    /** 当前生效配置(CSS 变量值, 供模板直接取色) */
    css (state) {
      return parseThemeConfig(this.current.config)
    }
  },
  actions: {
    /** 应用主题配置并持久化个人选择(themeKey 为空串=跟随管理员默认) */
    apply (theme) {
      const t = theme || DEFAULT_THEME
      const key = t.themeKey || ''
      this.userKey = key
      if (key) {
        localStorage.setItem(THEME_KEY, key)
      } else {
        localStorage.removeItem(THEME_KEY)
      }
      applyConfig(t.config, key)
    },
    /** 应用启动时调用: 本地缓存立即生效, 再拉取全局默认(登录页也能正确着色) */
    async boot () {
      try {
        const cache = JSON.parse(localStorage.getItem(THEME_CACHE) || 'null')
        if (cache && cache.config) applyConfig(cache.config, cache.themeKey || '')
      } catch (e) {}
      try {
        this.active = await themeApi.active()
      } catch (e) {
        this.active = null
      }
      const def = this.active && this.active.themeKey ? this.active : DEFAULT_THEME
      // 无个人选择时跟随管理员默认(管理员切默认主题后自动生效)
      if (!this.userKey) {
        applyConfig(def.config, def.themeKey)
      }
    },
    /** 登录后拉取启用主题; 校验个人选择仍可用, 被停用/删除则回落默认 */
    async loadEnabled () {
      try {
        this.enabled = await themeApi.enabled()
      } catch (e) {
        this.enabled = []
        return
      }
      const mine = this.enabled.find(t => t.themeKey === this.userKey)
      if (mine) {
        applyConfig(mine.config, mine.themeKey)
      } else if (this.userKey) {
        this.userKey = ''
        localStorage.removeItem(THEME_KEY)
        const def = this.active && this.active.themeKey ? this.active : DEFAULT_THEME
        applyConfig(def.config, def.themeKey)
      }
    },
    /** 切换主题(key 为空串=跟随管理员默认); 返回所选主题对象 */
    switchTo (key) {
      if (!key) {
        const def = this.active && this.active.themeKey ? this.active : DEFAULT_THEME
        this.apply(def)
        return def
      }
      const t = this.enabled.find(x => x.themeKey === key) || this.active
      if (!t) throw new Error('主题不存在')
      this.apply(t)
      return t
    }
  }
})
