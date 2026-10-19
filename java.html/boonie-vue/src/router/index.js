import { createRouter, createWebHashHistory } from 'vue-router'

import LandingView from '@/views/LandingView.vue'

/**
 * 路由表
 *   /            → 首页（水墨英雄区）
 *   /login       → 登录 / 注册
 *   /home        → 个人中心（需登录）
 *   /item/:id    → 条目详情（无需登录）
 *   /game        → 伞下听雨（休闲小游戏，无需登录）
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

router.afterEach((to) => {
  document.title = to.meta.title || '离恨烟 · 烟雨阁'
})

export default router
