import { defineStore } from 'pinia'
import { categoryApi } from '../api'

/**
 * 分类全局缓存: 题目/练习/分类页共用一份分类树, 减少重复请求
 * 变更分类后调用 refresh() 刷新缓存
 */
export const useCategoryStore = defineStore('categories', {
  state: () => ({
    tree: [],
    loaded: false
  }),
  getters: {
    /** 平铺列表(含 pathName 与 parentId), 供筛选下拉使用 */
    flat (state) {
      const result = []
      const walk = (list, prefix) => {
        for (const item of list) {
          const pathName = prefix ? prefix + ' / ' + item.name : item.name
          result.push({ id: item.id, name: item.name, pathName, parentId: item.parentId || 0 })
          if (item.children && item.children.length) walk(item.children, pathName)
        }
      }
      walk(state.tree, '')
      return result
    },
    primary () {
      return this.flat.filter(c => c.parentId === 0)
    }
  },
  actions: {
    /** 取分类树(已缓存则直接返回) */
    async fetchTree () {
      if (!this.loaded) {
        this.tree = await categoryApi.tree()
        this.loaded = true
      }
      return this.tree
    },
    /** 强制刷新缓存 */
    async refresh () {
      this.tree = await categoryApi.tree()
      this.loaded = true
      return this.tree
    }
  }
})
