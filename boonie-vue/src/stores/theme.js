import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'

/** 四种视觉风格（对应原项目的 4 个 HTML 文件） */
export const STYLES = [
  { id: 'forest', name: '森林高级风', emoji: '🌲', path: '/' },
  { id: 'cartoon', name: '卡通明亮风', emoji: '☁️', path: '/cartoon' },
  { id: 'neon', name: '暗夜霓虹风', emoji: '⚡', path: '/neon' },
  { id: 'minimal', name: '极简风', emoji: '🕊️', path: '/minimal' },
]

/**
 * 主题 store
 * - style：由当前路由推导（/、/cartoon、/neon、/minimal）
 * - light：森林风专属的昼夜切换（原 body.light）
 */
export const useThemeStore = defineStore('theme', {
  state: () => ({
    light: false,
  }),

  getters: {
    /** 当前风格 id，直接读路由 meta，天然与 URL 同步 */
    style() {
      const route = useRoute()
      return route.meta.style || 'forest'
    },
    /** 当前风格的展示名 */
    styleName() {
      return STYLES.find((s) => s.id === this.style)?.name ?? '森林高级风'
    },
    /** 是否显示昼夜切换按钮（仅森林风有该交互） */
    canToggleLight() {
      return this.style === 'forest'
    },
  },

  actions: {
    toggleLight() {
      this.light = !this.light
    },
  },
})
