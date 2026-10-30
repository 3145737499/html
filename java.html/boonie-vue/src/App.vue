<script setup>
import { useRoute } from 'vue-router'

import AppNav from '@/components/AppNav.vue'
import InkBackdrop from '@/components/InkBackdrop.vue'
import InkPetals from '@/components/InkPetals.vue'

import { useUserStore } from '@/stores/user'

const route = useRoute()
const user = useUserStore()

// 应用启动时回填本地会话（令牌 / 记住的账号 / 角色）
user.restore()
// 有令牌时用 /api/auth/me 回补账号与真实角色；令牌已失效则本地自动登出
user.ensureProfile()
</script>

<template>
  <!-- 环境层：水墨山水 + 飘落花瓣 -->
  <InkBackdrop>
    <InkPetals />
  </InkBackdrop>

  <!-- 登录页 / 后台登录页 / 后台管理页 为独立布局，隐藏全局导航与页脚 -->
  <AppNav v-if="route.name !== 'login' && route.name !== 'admin-login' && route.name !== 'admin'" />

  <main>
    <!-- 路由过渡；mode="out-in" 时 vue 会自动串接动画 -->
    <RouterView v-slot="{ Component }">
      <Transition name="fade" mode="out-in">
        <component :is="Component" :key="route.path" />
      </Transition>
    </RouterView>
  </main>

  <footer v-if="route.name !== 'login' && route.name !== 'admin-login' && route.name !== 'admin'">© 2026 离恨烟 · 烟雨阁 · 本页面为演示作品</footer>
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
