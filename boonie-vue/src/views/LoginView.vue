<script setup>
/**
 * 登录页
 * 支持 4 种风格的外观（由 App.vue 写在 html 上的 data-style 决定）
 * 逻辑来自原项目的登录脚本，改为 ref + computed 的响应式写法
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useTilt } from '@/composables/useTilt'
import { useLoginForm } from '@/composables/useLoginForm'
import { celebrate } from '@/composables/useCelebrate'

const router = useRouter()
const theme = useThemeStore()
const user = useUserStore()

/* ---------------- 表单状态 ---------------- */
const account = ref('')
const password = ref('')
const remember = ref(false)
const showPassword = ref(false)
const loading = ref(false)
const message = ref('')
const messageType = ref('')
const shaking = ref(false)

const { accountState, accountIcon, passwordState } = useLoginForm(account, password)

/* ---------------- 卡片 3D 倾斜（仅森林风） ---------------- */
const { el: cardEl, style: cardStyle } = useTilt(8)

/* ---------------- 各风格的品牌与按钮文案 ---------------- */
const BRAND = {
  forest: { title: '熊出没', sub: 'BOONIE BEARS', action: '登 录', divider: '丛林通道' },
  cartoon: { title: '登录乐园', sub: '', action: '登 录 🐾', divider: '— 或用其他方式 —' },
  neon: { title: 'NEON 登录', sub: '', action: '登 录', divider: '— 丛林通道 —' },
  minimal: { title: '登录', sub: '', action: '登 录', divider: '— 或使用以下方式 —' },
}

const brand = computed(() => BRAND[theme.style] ?? BRAND.forest)
const isForest = computed(() => theme.style === 'forest')

/* ---------------- 回填「记住我」 ---------------- */
onMounted(() => {
  const saved = user.restore()
  if (saved) {
    account.value = saved
    remember.value = true
  }
})

/* ---------------- 交互 ---------------- */
function togglePassword() {
  showPassword.value = !showPassword.value
}

function setMsg(text, type = '') {
  message.value = text
  messageType.value = type
}

function onSubmit() {
  const acc = account.value.trim()
  const pwd = password.value

  if (!acc || pwd.length < 6) {
    shaking.value = true
    setTimeout(() => (shaking.value = false), 420)
    setMsg(
      theme.style === 'cartoon' ? '检查一下账号或密码哦' : '请检查账号或密码',
      'err',
    )
    return
  }

  loading.value = true
  setMsg('')

  // 模拟请求（演示用途，无后端）
  setTimeout(() => {
    loading.value = false
    user.login(acc, remember.value)
    setMsg('登录成功，欢迎来到狗熊岭！')
    celebrate(theme.style)
    router.push({ name: 'home' })
  }, 900)
}

function onForgot() {
  setMsg(theme.style === 'cartoon' ? '找管理员帮帮忙' : '请联系管理员重置密码')
}

function onRegister() {
  setMsg('注册功能待接入后端')
}

function onSocial(name) {
  setMsg(`${name} 登录待接入`)
}
</script>

<template>
  <section class="view login active">
    <div class="login-wrap">
      <div
        ref="cardEl"
        class="card"
        :class="{ shake: shaking }"
        :style="isForest ? cardStyle : undefined"
      >
        <div class="brand">
          <div class="emblem">🐻</div>
          <div>
            <h1>{{ brand.title }}</h1>
            <p v-if="brand.sub">{{ brand.sub }}</p>
          </div>
        </div>

        <form novalidate @submit.prevent="onSubmit">
          <div class="field">
            <label for="acc">账号</label>
            <input
              id="acc"
              v-model="account"
              class="input"
              :class="accountState"
              placeholder="手机号 / 邮箱 / 用户名"
              autocomplete="username"
            />
            <span class="icon">{{ accountIcon }}</span>
          </div>

          <div class="field">
            <label for="pwd">密码</label>
            <input
              id="pwd"
              v-model="password"
              class="input"
              :class="passwordState"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
            <span class="icon" @click="togglePassword">{{ showPassword ? '🙈' : '👁️' }}</span>
          </div>

          <div class="row">
            <label class="remember">
              <input v-model="remember" type="checkbox" /> 记住我
            </label>
            <a href="#" class="link" @click.prevent="onForgot">忘记密码？</a>
          </div>

          <button class="btn primary block" type="submit" :class="{ loading }" :disabled="loading">
            <span v-if="isForest" class="spinner"></span>
            <span>{{ loading ? '登录中…' : brand.action }}</span>
          </button>

          <div class="msg" :class="messageType">{{ message }}</div>
        </form>

        <div class="divider">{{ brand.divider }}</div>

        <div class="social">
          <button type="button" @click="onSocial('微信')">🐻 微信</button>
          <button type="button" @click="onSocial('Apple')">🍎 Apple</button>
          <button type="button" @click="onSocial('QQ')">💬 QQ</button>
        </div>

        <p class="foot">
          还没有账号？<a href="#" class="link" @click.prevent="onRegister"><b>立即注册</b></a>
        </p>
      </div>
    </div>
  </section>
</template>
