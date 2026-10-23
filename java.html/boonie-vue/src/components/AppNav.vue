<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useScrolled } from '@/composables/useScrolled'

const router = useRouter()
const user = useUserStore()

const scrolled = useScrolled()

const brandName = '离恨烟 · 烟雨阁'

/** 当前是否处于「登录」页 */
const isLogin = computed(() => router.currentRoute.value.name === 'login')

/** 是否展示「后台管理」入口：未登录（供管理员登录）或已登录且为管理员时展示 */
const showAdmin = computed(() => !user.isLoggedIn || user.role === 'admin')

/** 点击导航项 */
function go(nav) {
  if (nav === 'landing') router.push({ path: '/' })
  else if (nav === 'admin') router.push({ name: 'admin' })
  else if (nav === 'admin-login') router.push({ name: 'admin-login' })
  else router.push({ name: nav }) // login / home / game
}

/** 退出登录：调用后端作废 token 并清理本地登录态 */
async function onLogout() {
  await user.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <nav :class="{ scrolled }">
    <div class="nav-brand" @click="go('landing')">
      <div class="em seal-mark">离</div>
      <span>{{ brandName }}</span>
    </div>

    <div class="nav-links">
      <a :class="{ active: !isLogin && $route.name !== 'home' && $route.name !== 'game' && $route.name !== 'admin' && $route.name !== 'admin-login' }" @click="go('landing')">首页</a>
      <a :class="{ active: $route.name === 'game' }" @click="go('game')">听雨</a>
      <a v-if="showAdmin" class="admin-entry" :class="{ active: $route.name === 'admin' || $route.name === 'admin-login' }" @click="go('admin-login')">后台管理</a>
      <a :class="{ active: isLogin }" @click="go('login')">登录</a>
      <a v-if="user.isLoggedIn" :class="{ active: $route.name === 'home' }" @click="go('home')">
        个人中心
      </a>
      <a v-if="user.isLoggedIn" class="logout" title="退出登录" @click="onLogout">退出</a>
    </div>
  </nav>
</template>

<style scoped>
.nav-links .admin-entry {
  padding: 6px 16px;
  border: 1px solid rgba(163, 127, 61, 0.55);
  border-radius: 999px;
  color: var(--gold-deep);
  letter-spacing: 3px;
  transition: all 0.25s;
}
.nav-links .admin-entry::after {
  display: none;
}
.nav-links .admin-entry:hover,
.nav-links .admin-entry.active {
  background: rgba(201, 163, 92, 0.16);
  border-color: var(--gold-deep);
  color: var(--ink);
  transform: translateY(-1px);
}
</style>
