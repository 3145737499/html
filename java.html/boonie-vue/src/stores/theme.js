import { defineStore } from 'pinia'

/**
 * 主题 store
 * 离恨烟项目仅保留单一「水墨丹青」风格，
 * 保留 store 结构以便未来扩展更多风格
 */
export const useThemeStore = defineStore('theme', {
  state: () => ({
    light: false,
  }),

  getters: {
    style: () => 'ink',
    styleName: () => '水墨丹青',
    canToggleLight: () => false,
  },

  actions: {
    toggleLight() {
      this.light = !this.light
    },
  },
})
