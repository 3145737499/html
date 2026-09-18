<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore, STYLES } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useScrolled } from '@/composables/useScrolled'

const router = useRouter()
const theme = useThemeStore()
const user = useUserStore()

const scrolled = useScrolled()

/** 品牌文案随风格变化（对应原 4 个文件的不同 nav 品牌） */
const BRAND = {
  forest: '熊出没 · 狗熊岭',
  cartoon: '熊出没 · 卡通乐园',
  neon: '熊出没 · NEON',
  minimal: '熊出没 · 极简',
}

const brandName = computed(() => BRAND[theme.style] ?? BRAND.forest)

/** 当前是否处于「登录」页 */
const isLogin = computed(() => router.currentRoute.value.name === 'login')

/** 点击导航项：切换到对应风格下的目标视图 */
function go(nav) {
  if (nav === 'landing') router.push(STYLES.find((s) => s.id === theme.style).path)
  else router.push({ name: nav === 'login' ? 'login' : 'home' })
}
</script>

<template>
  <nav :class="{ scrolled }">
    <div class="nav-brand">
      <div class="em">🐻</div>
      <span>{{ brandName }}</span>
    </div>

    <div class="nav-links">
      <a :class="{ active: !isLogin && $route.name !== 'home' }" @click="go('landing')">首页</a>
      <a :class="{ active: isLogin }" @click="go('login')">登录</a>
      <a v-if="user.isLoggedIn" :class="{ active: $route.name === 'home' }" @click="go('home')">
        个人中心
      </a>
      <button
        v-if="theme.canToggleLight"
        class="theme"
        :title="theme.light ? '切换到夜间' : '切换到白天'"
        @click="theme.toggleLight()"
      >
        {{ theme.light ? '☀️' : '🌙' }}
      </button>
    </div>
  </nav>
</template>
