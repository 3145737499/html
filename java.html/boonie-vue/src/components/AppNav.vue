<script setup>
/**
 * 顶部导航
 * 2026-10-30 升级：
 *  - 导航项改用 RouterLink（真实 href、可键盘聚焦、支持 Ctrl+点击新标签打开），
 *    原先是无 href 的裸 <a>，屏幕阅读器与键盘用户都不可用
 *  - 新增 <768px 汉堡菜单，解决 6 个导航项在手机上挤爆的问题
 */
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import { useUserStore } from '@/stores/user'
import { useScrolled } from '@/composables/useScrolled'

const router = useRouter()
const route = useRoute()
const user = useUserStore()

const scrolled = useScrolled()

const brandName = '离恨烟 · 烟雨阁'

/** 移动端菜单是否展开 */
const menuOpen = ref(false)

/** 「首页」只在真正落在首页时高亮（hash 模式下 '/' 是其他路由的前缀） */
const landingActive = computed(() => route.name === 'landing')
const isLogin = computed(() => route.name === 'login')

/** 是否展示「后台管理」入口：未登录（供管理员登录）或已登录且为管理员时展示 */
const showAdmin = computed(() => !user.isLoggedIn || user.role === 'admin')

// 路由变化后收起移动端菜单
watch(() => route.fullPath, () => {
  menuOpen.value = false
})

function goBrand() {
  menuOpen.value = false
  router.push({ path: '/' })
}

/** 退出登录：调用后端作废 token 并清理本地登录态 */
async function onLogout() {
  menuOpen.value = false
  await user.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <nav :class="{ scrolled, 'menu-open': menuOpen }">
    <div class="nav-brand" role="link" tabindex="0" @click="goBrand" @keydown.enter.prevent="goBrand" @keydown.space.prevent="goBrand">
      <div class="em seal-mark">离</div>
      <span>{{ brandName }}</span>
    </div>

    <button
      class="nav-toggle"
      :aria-expanded="menuOpen"
      aria-controls="nav-menu"
      :aria-label="menuOpen ? '收起导航菜单' : '展开导航菜单'"
      @click="menuOpen = !menuOpen"
    >
      <span></span>
      <span></span>
      <span></span>
    </button>

    <div id="nav-menu" class="nav-links" :class="{ open: menuOpen }">
      <RouterLink :to="{ name: 'landing' }" :class="{ active: landingActive }">首页</RouterLink>
      <RouterLink :to="{ name: 'game' }" :class="{ active: route.name === 'game' }">听雨</RouterLink>
      <RouterLink
        v-if="showAdmin"
        :to="{ name: 'admin-login' }"
        class="admin-entry"
        :class="{ active: route.name === 'admin' || route.name === 'admin-login' }"
      >
        后台管理
      </RouterLink>
      <RouterLink :to="{ name: 'login' }" :class="{ active: isLogin }">登录</RouterLink>
      <RouterLink v-if="user.isLoggedIn" :to="{ name: 'home' }" :class="{ active: route.name === 'home' }">
        个人中心
      </RouterLink>
      <button v-if="user.isLoggedIn" class="logout" title="退出登录" @click="onLogout">退出</button>
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
/* 「后台管理」是胶囊徽章，不走链接的下划线展开效果 */
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

/* 「退出」是动作而非跳转，用 button 保持语义，样式对齐链接 */
.nav-links button.logout {
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  letter-spacing: 4px;
  color: var(--cinnabar);
  cursor: pointer;
  padding: 4px 2px;
  position: relative;
}
.nav-links button.logout:hover {
  color: #8f2a21;
}

/* ---------- 汉堡按钮（仅窄屏显示） ---------- */
.nav-toggle {
  display: none;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid rgba(163, 127, 61, 0.45);
  border-radius: 4px;
  background: rgba(250, 248, 240, 0.6);
  cursor: pointer;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  transition: background 0.25s, border-color 0.25s;
}
.nav-toggle:hover {
  background: rgba(201, 163, 92, 0.16);
  border-color: var(--gold-deep);
}
.nav-toggle span {
  display: block;
  width: 18px;
  height: 2px;
  border-radius: 2px;
  background: var(--ink-soft);
  transition: transform 0.28s ease, opacity 0.2s ease;
}
/* 展开时变成叉号 */
.nav-toggle[aria-expanded='true'] span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}
.nav-toggle[aria-expanded='true'] span:nth-child(2) {
  opacity: 0;
}
.nav-toggle[aria-expanded='true'] span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

@media (max-width: 768px) {
  .nav-toggle {
    display: flex;
  }

  .nav-links {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    display: none;
    flex-direction: column;
    align-items: stretch;
    gap: 2px;
    padding: 8px 20px 14px;
    background: rgba(246, 244, 236, 0.97);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(201, 163, 92, 0.3);
    box-shadow: 0 14px 30px rgba(30, 43, 40, 0.16);
  }

  .nav-links.open {
    display: flex;
    animation: menu-in 0.24s ease both;
  }

  .nav-links > * {
    padding: 12px 4px;
    font-size: 15px;
    border-bottom: 1px dashed rgba(201, 163, 92, 0.22);
  }
  .nav-links > *:last-child {
    border-bottom: none;
  }

  /* 下拉态下徽章式入口改为整行 */
  .nav-links .admin-entry {
    border-radius: 4px;
    border: none;
    text-align: left;
  }
  .nav-links .admin-entry::after {
    display: none;
  }

  .nav-links button.logout {
    text-align: left;
  }
}

@keyframes menu-in {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
