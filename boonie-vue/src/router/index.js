import { createRouter, createWebHashHistory } from 'vue-router'

import LandingView from '@/views/LandingView.vue'

/**
 * 路由表
 * 风格由「当前路由 + 主题 store」共同决定：
 *   /            → 森林高级风（默认首页）
 *   /cartoon     → 卡通明亮风
 *   /neon        → 暗夜霓虹风
 *   /minimal     → 极简风
 *   各风格下均有 /login 与 /home 子级路径
 */
const routes = [
  {
    path: '/',
    name: 'landing',
    component: LandingView,
    meta: { title: '熊出没 · 狗熊岭', nav: 'landing', style: 'forest' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 · 熊出没', nav: 'login', style: 'forest' },
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '个人中心 · 熊出没', nav: 'home', style: 'forest', requiresAuth: true },
  },
  {
    path: '/cartoon',
    name: 'cartoon',
    component: LandingView,
    meta: { title: '熊出没 · 卡通乐园', nav: 'landing', style: 'cartoon' },
  },
  {
    path: '/neon',
    name: 'neon',
    component: LandingView,
    meta: { title: '熊出没 · 霓虹暗夜', nav: 'landing', style: 'neon' },
  },
  {
    path: '/minimal',
    name: 'minimal',
    component: LandingView,
    meta: { title: '熊出没 · 极简', nav: 'landing', style: 'minimal' },
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  // 使用 hash 模式，构建后双击 dist/index.html 也能直接打开
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0, behavior: 'smooth' }),
})

router.afterEach((to) => {
  document.title = to.meta.title || '熊出没 · 狗熊岭'
})

export default router
