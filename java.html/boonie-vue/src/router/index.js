import { createRouter, createWebHashHistory } from 'vue-router'

import LandingView from '@/views/LandingView.vue'

/**
 * 路由表
 *   /            → 首页（水墨英雄区）
 *   /login       → 登录 / 注册（游客 + 注册 + 登录）
 *   /home        → 个人中心（需登录）
 *   /item/:id    → 条目详情（无需登录）
 *   /game        → 伞下听雨（休闲小游戏，无需登录）
 *   /admin/login → 后台管理登录（仅登录，无注册）
 *   /admin       → 后台管理（侧边栏 + 增删改查，需登录）
 */
const routes = [
  {
    path: '/',
    name: 'landing',
    component: LandingView,
    meta: { title: '离恨烟 · 烟雨阁', nav: 'landing' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 · 离恨烟', nav: 'login' },
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '个人中心 · 离恨烟', nav: 'home', requiresAuth: true },
  },
  {
    path: '/game',
    name: 'game',
    component: () => import('@/views/GameView.vue'),
    meta: { title: '伞下听雨 · 离恨烟', nav: 'game' },
  },
  {
    path: '/item/:id',
    name: 'item',
    component: () => import('@/views/DetailView.vue'),
    meta: { title: '条目详情 · 离恨烟', nav: 'home' },
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/AdminLoginView.vue'),
    meta: { title: '后台登录 · 离恨烟', nav: 'admin-login' },
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { title: '后台管理 · 离恨烟', nav: 'admin', requiresAuth: true },
  },
  // 兼容旧路由：全部落回首页
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

/** 读取本地保存的登录令牌（与 user store 的持久化键保持一致） */
function localToken() {
  try {
    return localStorage.getItem('bb_token') || ''
  } catch {
    return ''
  }
}

router.beforeEach((to, from, next) => {
  const needAuth = to.meta && to.meta.requiresAuth
  const hasToken = !!localToken()

  // 需登录但无令牌 → 跳到对应登录页（后台去 /admin/login，前台去 /login）
  if (needAuth && !hasToken) {
    if (to.name === 'admin') return next({ name: 'admin-login' })
    return next({ name: 'login' })
  }

  // 已登录又访问登录页 → 直接进入对应主页（避免重复登录）
  if (hasToken && (to.name === 'admin-login')) {
    return next({ name: 'admin' })
  }
  if (hasToken && to.name === 'login') {
    return next({ name: 'home' })
  }

  next()
})

router.afterEach((to) => {
  document.title = to.meta.title || '离恨烟 · 烟雨阁'
})

export default router
