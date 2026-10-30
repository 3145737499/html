import { defineStore } from 'pinia'

import { logout as apiLogout, fetchMe } from '@/api/auth'

const STORAGE_KEY = 'bb_acc'
const TOKEN_KEY = 'bb_token'
const ROLE_KEY = 'bb_role'

/**
 * 用户 / 登录状态
 * - token：登录令牌，唯一权威的「是否已登录」判据（始终是持久化的）
 * - account：登录账号（供「记住我」回填与问候语展示），未勾记住我时由 /api/auth/me 回补
 * - role：角色（user=普通用户, admin=后台管理员），由 /api/auth/me 回补，决定后台入口与路由权限
 *
 * 注意：早期版本用 account !== '' 判登录态，但 account 仅在「记住我」时持久化，
 * 会出现「路由放行进 /home、导航栏却显示未登录」的矛盾。现统一以 token 为准。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    /** 已登录账号，未登录为 '' */
    account: '',
    /** 登录令牌 —— 登录态的唯一判据 */
    token: '',
    /** 角色 */
    role: '',
    /** 记住我 */
    remember: false,
  }),

  getters: {
    /** 是否已登录：以令牌为准（令牌始终持久化，刷新后不会和路由守卫打架） */
    isLoggedIn: (state) => state.token !== '',
    /** 是否为后台管理员 */
    isAdmin: (state) => state.role === 'admin',
    /**
     * 角色是否已经过后端校验（/api/auth/me 成功返回）。
     * 授权判断必须配合这个用：localStorage 里的 role 可被手改，不能单独信任。
     */
    roleVerified: () => profileLoaded,
    /** 个人中心问候语 */
    greeting: (state) =>
      state.account ? `欢迎回来，${state.account}！` : '欢迎回来，烟雨阁少侠！',
  },

  actions: {
    /** 从 localStorage 回填令牌与角色；账号仅在「记住我」时回填 */
    restore() {
      try {
        this.token = localStorage.getItem(TOKEN_KEY) || ''
        this.role = localStorage.getItem(ROLE_KEY) || ''
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

    /**
     * 用 /api/auth/me 回补账号，并向后端核实真实角色。
     * - 每个页面会话只真正请求一次（成功后置 profileLoaded），并发调用共用同一个请求
     * - 令牌失效（后端重启清空了内存 token）→ 本地会话一并作废
     * - 网络失败不清会话，交由各视图自身的错误处理兜底
     */
    async ensureProfile() {
      if (!this.token) return null
      if (profileLoaded) return { account: this.account, role: this.role }
      if (profileReq) return profileReq

      profileReq = (async () => {
        try {
          const res = await fetchMe(this.token)
          if (res && res.success && res.data) {
            this.account = res.data.account || this.account
            // 角色一律以后端为准，覆盖 localStorage 里的缓存值
            this.setRole(res.data.role || '')
            profileLoaded = true
            return res.data
          }
          // 令牌已失效：清掉本地残留，让界面回到未登录态
          this.clearSession()
          return null
        } catch {
          return null
        } finally {
          profileReq = null
        }
      })()

      return profileReq
    },

    /**
     * 标记「角色已经过后端校验」。
     * 用于登录流程自己已经调过 /me（例如后台登录的 isAdmin 校验），
     * 不必再让 ensureProfile 重复请求一次。
     */
    markProfileVerified() {
      profileLoaded = true
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

    /** 写入角色（登录后由 /api/auth/me 取回） */
    setRole(role = '') {
      this.role = role || ''
      try {
        if (this.role) localStorage.setItem(ROLE_KEY, this.role)
        else localStorage.removeItem(ROLE_KEY)
      } catch {
        /* ignore */
      }
    },

    /** 仅清理本地会话（不通知后端），用于令牌失效等场景 */
    clearSession() {
      this.account = ''
      this.token = ''
      this.role = ''
      try {
        localStorage.removeItem(STORAGE_KEY)
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(ROLE_KEY)
      } catch {
        /* ignore */
      }
    },

    /** 退出登录：清理状态并通知后端作废 token（静默失败，不影响本地登出） */
    async logout() {
      const token = this.token
      this.clearSession()
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

/** ensureProfile 的并发去重句柄（模块级，不进 state 避免被持久化/序列化） */
let profileReq = null
/** 本次页面会话内，角色是否已由 /api/auth/me 校验过 */
let profileLoaded = false
