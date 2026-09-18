import { defineStore } from 'pinia'

const STORAGE_KEY = 'bb_acc'

/**
 * 用户 / 登录状态
 * 由原来的原生 JS 全局变量 + DOM 操作，改为可组合的响应式 store
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    /** 已登录账号，未登录为 '' */
    account: '',
    /** 记住我 */
    remember: false,
  }),

  getters: {
    isLoggedIn: (state) => state.account !== '',
    /** 个人中心问候语 */
    greeting: (state) =>
      state.account ? `欢迎回来，${state.account}！` : '欢迎回来，森林伙伴！',
  },

  actions: {
    /** 从 localStorage 回填「记住我」的账号 */
    restore() {
      try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved) {
          this.remember = true
          return saved
        }
      } catch {
        /* localStorage 不可用时静默降级 */
      }
      return ''
    },

    /** 登录成功：写入状态 + 持久化 */
    login(account, remember) {
      this.account = account
      this.remember = remember
      try {
        if (remember) localStorage.setItem(STORAGE_KEY, account)
        else localStorage.removeItem(STORAGE_KEY)
      } catch {
        /* ignore */
      }
    },

    logout() {
      this.account = ''
      try {
        localStorage.removeItem(STORAGE_KEY)
      } catch {
        /* ignore */
      }
    },
  },
})
