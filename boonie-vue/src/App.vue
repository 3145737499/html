<script setup>
import { computed, watchEffect } from 'vue'
import { useRoute } from 'vue-router'

import AppNav from '@/components/AppNav.vue'
import StyleSwitcher from '@/components/StyleSwitcher.vue'
import ForestBackdrop from '@/components/ForestBackdrop.vue'
import CartoonBackdrop from '@/components/CartoonBackdrop.vue'
import NeonBackdrop from '@/components/NeonBackdrop.vue'
import FallingLeaves from '@/components/FallingLeaves.vue'

import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const theme = useThemeStore()
const user = useUserStore()

/** 当前风格 id（由路由 meta 推导） */
const style = computed(() => theme.style)

/**
 * 把风格与昼夜状态写到 <html> 上，
 * 各风格的 css 通过 html[data-style='xxx'] 精确作用域生效
 */
watchEffect(() => {
  const el = document.documentElement
  el.dataset.style = style.value
  el.dataset.light = String(theme.light)
})

// 应用启动时回填「记住我」的账号
user.restore()

/** 页脚文案随风格变化 */
const FOOTER = {
  forest: '© 2026 熊出没 · 狗熊岭社区 · 本页面为演示作品',
  cartoon: '© 2026 熊出没 · 卡通乐园 · 演示作品',
  neon: '© 2026 熊出没 · NEON 演示',
  minimal: '© 2026 熊出没 · 极简演示',
}
const footerText = computed(() => FOOTER[style.value] ?? FOOTER.forest)
</script>

<template>
  <!-- 环境层：按风格挂载，用 v-if 避免多余节点 -->
  <ForestBackdrop v-if="style === 'forest'">
    <FallingLeaves />
  </ForestBackdrop>
  <CartoonBackdrop v-else-if="style === 'cartoon'" />
  <NeonBackdrop v-else-if="style === 'neon'" />

  <AppNav />

  <main>
    <!-- 路由过渡；mode="out-in" 时 vue 会自动串接动画 -->
    <RouterView v-slot="{ Component }">
      <Transition name="fade" mode="out-in">
        <component :is="Component" :key="route.path" />
      </Transition>
    </RouterView>
  </main>

  <StyleSwitcher />

  <footer>{{ footerText }}</footer>
</template>

<style scoped>
main {
  position: relative;
  z-index: 2;
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.28s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
