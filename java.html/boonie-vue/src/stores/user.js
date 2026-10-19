import { defineStore } from 'pinia'

import { logout as apiLogout } from '@/api/auth'

const STORAGE_KEY = 'bb_acc'
const TOKEN_KEY = 'bb_token'

/**
 * 用户 / 登录状态
 * - account：登录账号（供「记住我」回填与问候语展示）
 * - token：后端签发的登录令牌（持久化，页面刷新不掉线）
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    /** 已登录账号，未登录为 '' */
    account: '',
    /** 登录令牌 */
    token: '',
    /** 记住我 */
    remember: false,
  }),

  getters: {
    isLoggedIn: (state) => state.account !== '',
    /** 个人中心问候语 */
    greeting: (state) =>
      state.account ? `欢迎回来，${state.account}！` : '欢迎回来，烟雨阁少侠！',
  },

  actions: {
    /** 从 localStorage 回填「记住我」的账号与登录令牌 */
    restore() {
      try {
        this.token = localStorage.getItem(TOKEN_KEY) || ''
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved) {
          this.remember = true
          this.account = saved
          return saved
        }
      } catch {
        /* localStorage 不可用时静默降级 */
      }
      return ''
    },

    /** 登录成功：写入状态 + 持久化（token 始终保存，账号按「记住我」保存） */
    login(account, remember, token = '') {
      this.account = account
      this.remember = remember
      this.token = token
      try {
        if (token) localStorage.setItem(TOKEN_KEY, token)
        if (remember) localStorage.setItem(STORAGE_KEY, account)
        else localStorage.removeItem(STORAGE_KEY)
      } catch {
        /* ignore */
      }
    },

    /** 退出登录：清理状态并通知后端作废 token（静默失败，不影响本地登出） */
    async logout() {
      const token = this.token
      this.account = ''
      this.token = ''
      try {
        localStorage.removeItem(STORAGE_KEY)
        localStorage.removeItem(TOKEN_KEY)
      } catch {
        /* ignore */
      }
      if (token) {
        try {
          await apiLogout(token)
        } catch {
          /* 后端不可达时仅本地登出 */
        }
      }
    },
  },
})
