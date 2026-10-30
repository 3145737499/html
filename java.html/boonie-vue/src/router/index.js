import { createRouter, createWebHashHistory } from 'vue-router'

import LandingView from '@/views/LandingView.vue'
import { useUserStore } from '@/stores/user'

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

router.beforeEach(async (to, from, next) => {
  const user = useUserStore()

  // 首次进入时 App.vue 的 restore() 可能还没跑，这里兜一次（幂等）
  if (!user.token) user.restore()
  // 有令牌就向后端核实账号与真实角色（每会话只请求一次，失败不清会话）
  if (user.token) await user.ensureProfile()

  const hasToken = !!user.token

  // 需登录但无令牌 → 跳到对应登录页（后台去 /admin/login，前台去 /login）
  if (to.meta && to.meta.requiresAuth && !hasToken) {
    return next(to.name === 'admin' ? { name: 'admin-login' } : { name: 'login' })
  }

  // 后台管理：必须是「后端已验证的」管理员。
  // roleVerified 为 false 说明 /me 没拿到结果（后端不可达等），此时放行让 AdminView 的
  // 401 处理接管，避免网络抖动就把管理员踢出后台。
  if (to.name === 'admin' && user.roleVerified && !user.isAdmin) {
    return next({ name: 'admin-login' })
  }

  // 已登录又访问登录页 → 直接进入对应主页（避免重复登录）
  if (hasToken && to.name === 'admin-login' && user.isAdmin) {
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
