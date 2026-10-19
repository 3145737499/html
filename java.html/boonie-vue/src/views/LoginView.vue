<script setup>
/**
 * 登录 / 注册页 · 离恨烟水墨风（立绘分栏版）
 * 布局参考 lihengyan-login/index.html：左侧立绘 + 竖排诗题，右侧登录卡
 * 已接入 Spring Boot 后端：
 *   登录  POST /api/auth/login
 *   注册  GET /api/auth/captcha + POST /api/auth/register（图形验证码）
 * 流程：注册成功 → 不直接登录，跳回登录页并预填注册账号 → 输入密码登录 → 进入主页
 * 游客免注册：点「游客免注册·直接入阁」→ 用演示账号 demo@boonie.com 一键登录 → 直接进主页（不经过注册/登录页）
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import { useUserStore } from '@/stores/user'
import { useLoginForm } from '@/composables/useLoginForm'
import { celebrate } from '@/composables/useCelebrate'
import { getCaptcha, register as apiRegister, login as apiLogin } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const user = useUserStore()

/* ---------------- 表单状态 ---------------- */
const mode = ref('login') // 'login' | 'register'
const account = ref('')
const password = ref('')
const code = ref('')
const remember = ref(false)
const showPassword = ref(false)
const loading = ref(false)
const message = ref('')
const messageType = ref('')
const shaking = ref(false)
const pwdInput = ref(null)
const confirmPassword = ref('')

/* ---------------- 图形验证码 ---------------- */
const captchaImg = ref('')
const captchaKey = ref('')
const captchaLoading = ref(false)

const { accountState, passwordState } = useLoginForm(account, password)

/* ---------------- 注册专属：确认密码 + 密码强度 ---------------- */
/** 确认密码状态：'' | 'ok' | 'err'（仅注册模式有意义） */
const confirmState = computed(() => {
  if (!confirmPassword.value) return ''
  return confirmPassword.value === password.value ? 'ok' : 'err'
})

/** 密码强度：score 0-4，label 弱/中/强/极强，cls 用于配色 */
const pwdStrength = computed(() => {
  const v = password.value
  if (!v) return { score: 0, label: '', cls: '' }
  let score = 0
  if (v.length >= 6) score++ // 达标
  if (v.length >= 10) score++ // 更长
  if (/[a-z]/.test(v) && /[A-Z]/.test(v)) score++ // 大小写混合
  if (/\d/.test(v)) score++ // 含数字
  if (/[^A-Za-z0-9]/.test(v)) score++ // 含符号
  score = Math.min(score, 4)
  const map = [
    { label: '', cls: '' },
    { label: '弱', cls: 's-weak' },
    { label: '中', cls: 's-fair' },
    { label: '强', cls: 's-good' },
    { label: '极强', cls: 's-strong' },
  ]
  return { score, ...map[score] }
})

/* ---------------- 品牌文案 ---------------- */
const brand = {
  title: '离 恨 烟',
  sub: '烟 雨 江 湖 · 与 君 同 行',
  action: '登 录',
  divider: '其 他 方 式 登 录',
}

const submitLabel = computed(() => {
  if (loading.value) return mode.value === 'login' ? '登录中…' : '注册中…'
  return mode.value === 'login' ? brand.action : '注 册'
})

/* ---------------- 回填「记住我」 + 注册回跳预填 ---------------- */
onMounted(() => {
  const saved = user.restore()
  if (saved) {
    account.value = saved
    remember.value = true
  }
  // 注册成功后回跳登录页：URL 带 ?account=xxx，自动预填账号并切到登录模式
  const qAccount = route.query.account
  if (typeof qAccount === 'string' && qAccount.trim()) {
    account.value = qAccount.trim()
    mode.value = 'login'
    if (!saved) remember.value = false
    setMsg('注册成功，请使用账号登录', 'ok')
    nextTick(() => pwdInput.value?.focus())
  }
})

/* ---------------- 图形验证码交互 ---------------- */
async function loadCaptcha() {
  captchaLoading.value = true
  const res = await getCaptcha()
  captchaLoading.value = false
  if (!res.success || !res.data) {
    captchaImg.value = ''
    setMsg(res.message || '验证码加载失败，请点击重试', 'err')
    return
  }
  captchaImg.value = res.data.image
  captchaKey.value = res.data.key
  code.value = ''
  setMsg('')
}

/* ---------------- 交互 ---------------- */
function togglePassword() {
  showPassword.value = !showPassword.value
}

function setMsg(text, type = '') {
  message.value = text
  messageType.value = type
}

function shake() {
  shaking.value = true
  setTimeout(() => (shaking.value = false), 420)
}

/** 切换 登录 / 注册 模式（进入注册时加载图形验证码，清空确认密码） */
function switchMode(target) {
  if (mode.value === target) return
  mode.value = target
  confirmPassword.value = ''
  setMsg('')
  if (target === 'register' && !captchaImg.value) {
    loadCaptcha()
  }
}

/** 提交登录 / 注册 */
async function onSubmit() {
  const acc = account.value.trim()
  const pwd = password.value

  if (!acc || pwd.length < 6) {
    shake()
    setMsg('请检查账号或密码', 'err')
    return
  }
  if (pwd.length > 15) {
    shake()
    setMsg('密码长度不能超过 15 位', 'err')
    return
  }
  if (mode.value === 'register') {
    if (confirmPassword.value !== pwd) {
      shake()
      setMsg('两次输入的密码不一致', 'err')
      return
    }
    if (!code.value.trim()) {
      shake()
      setMsg('请输入图片中的 4 位验证码', 'err')
      return
    }
  }

  loading.value = true
  setMsg('')

  try {
    const res =
      mode.value === 'login'
        ? await apiLogin(acc, pwd)
        : await apiRegister(acc, pwd, code.value.trim(), captchaKey.value)

    if (!res.success) {
      loading.value = false
      shake()
      setMsg(res.message || '操作失败，请稍后再试', 'err')
      // 验证码一次性使用，失败后自动换一张
      if (mode.value === 'register') loadCaptcha()
      return
    }

    if (mode.value === 'login') {
      // 登录成功：保存账号与 token，进入主页
      user.login(acc, remember.value, res.token || '')
      setMsg('登录成功，伞下江湖见！', 'ok')
      celebrate()
      loading.value = false
      router.push({ name: 'home' })
    } else {
      // 注册成功：不直接登录，跳回登录页并预填注册账号，由用户再次登录
      loading.value = false
      account.value = acc
      password.value = ''
      confirmPassword.value = ''
      code.value = ''
      mode.value = 'login'
      setMsg('注册成功，请使用账号登录', 'ok')
      router.replace({ name: 'login', query: { account: acc, registered: '1' } })
      nextTick(() => pwdInput.value?.focus())
    }
  } catch {
    loading.value = false
    shake()
    setMsg('网络异常，无法连接服务器', 'err')
  }
}

function onForgot() {
  setMsg('请联系阁主重置密码')
}

/**
 * 游客免注册直接体验：用演示账号一键登录并直入主页，
 * 不经过注册、也不跳回登录页（演示级别，账号为 demo@boonie.com）
 */
async function guestLogin() {
  if (loading.value) return
  loading.value = true
  setMsg('')
  try {
    const res = await apiLogin('demo@boonie.com', '123456')
    if (!res.success) {
      loading.value = false
      shake()
      setMsg(res.message || '体验入口暂不可用，请稍后再试', 'err')
      return
    }
    // 记住我=false，游客态不持久化；展示名用「游客体验」
    user.login('游客体验', false, res.token || '')
    setMsg('游客入阁，伞下江湖见！', 'ok')
    celebrate()
    loading.value = false
    router.push({ name: 'home' })
  } catch {
    loading.value = false
    shake()
    setMsg('网络异常，无法连接服务器', 'err')
  }
}
</script>

<template>
  <section class="view login-stage">
    <!-- 飘浮烟雾 -->
    <div class="login-mist m1"></div>
    <div class="login-mist m2"></div>
    <div class="login-mist m3"></div>

    <div class="stage">
      <!-- 左：立绘 + 竖排诗题 -->
      <div class="portrait">
        <img src="/lihengyan.jpg" alt="离恨烟" />
        <div class="verse">烟水茫茫·离恨绵绵 <b>伞落惊鸿</b></div>
      </div>

      <!-- 右：登录 / 注册卡 -->
      <div class="panel">
        <div class="card" :class="{ shake: shaking }">
          <span class="corner tl"></span><span class="corner tr"></span>
          <span class="corner bl"></span><span class="corner br"></span>

          <div class="seal seal-mark">离恨烟</div>

          <div class="brand">
            <h1>{{ mode === 'register' ? '入 阁 通 牒' : brand.title }}</h1>
            <p class="sub">{{ mode === 'register' ? 'BECOME A SWORDSMAN' : brand.sub }}</p>
          </div>

          <svg class="brush-divider" width="240" height="14" viewBox="0 0 240 14">
            <path d="M4,8 Q60,2 120,7 T236,6" stroke="#a37f3d" stroke-width="2.4" fill="none" stroke-linecap="round" opacity=".8" />
            <circle cx="120" cy="7" r="2.6" fill="#b3362b" />
          </svg>

          <form novalidate @submit.prevent="onSubmit">
            <div class="field">
              <label for="acc">账 号</label>
              <input
                id="acc"
                v-model="account"
                class="input"
                :class="accountState"
                placeholder="请输入账号 / 侠客名号"
                autocomplete="username"
              />
              <span class="line"></span>
              <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="#3d504b" stroke-width="1.6">
                <circle cx="12" cy="8" r="4" /><path d="M4,20 Q4,14 12,14 Q20,14 20,20" />
              </svg>
            </div>

            <div class="field">
              <label for="pwd">密 码</label>
              <input
                id="pwd"
                ref="pwdInput"
                v-model="password"
                class="input"
                :class="passwordState"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码（6-15 位）"
                autocomplete="current-password"
                maxlength="15"
              />
              <span class="line"></span>
              <span
                class="icon"
                role="button"
                :title="showPassword ? '隐藏密码' : '显示密码'"
                @click="togglePassword"
              >
                <!-- 油纸伞：闭合=隐藏密码 / 撑开=显示密码 -->
                <svg
                  v-if="!showPassword"
                  class="umb"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#3d504b"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M12 2.5 C10 5 10 8 12 10 C14 8 14 5 12 2.5 Z" fill="rgba(179,54,43,0.14)" />
                  <path d="M12 10 V19.5 Q12 21.5 10 21.5" />
                </svg>
                <svg
                  v-else
                  class="umb"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#3d504b"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M3 10 C3 4.5 8 3 12 3 C16 3 21 4.5 21 10" fill="rgba(179,54,43,0.14)" />
                  <path d="M3 10 Q12 13 21 10" />
                  <path d="M12 3 V10" />
                  <path d="M8 4.8 V10" />
                  <path d="M16 4.8 V10" />
                  <path d="M12 3 V1.6" />
                  <path d="M12 10 V19.5 Q12 21.5 10 21.5" />
                </svg>
              </span>
            </div>

            <!-- 注册模式：确认密码 -->
            <div v-if="mode === 'register'" class="field">
              <label for="cpwd">确 认 密 码</label>
              <input
                id="cpwd"
                v-model="confirmPassword"
                class="input"
                :class="confirmState"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请再次输入密码"
                autocomplete="new-password"
                maxlength="15"
              />
              <span class="line"></span>
              <span v-if="confirmState === 'ok'" class="ico-check">✓</span>
            </div>

            <!-- 注册模式：密码强度条 -->
            <div v-if="mode === 'register'" class="pwd-strength" :class="pwdStrength.cls">
              <div class="bars">
                <span v-for="n in 4" :key="n" class="bar" :class="{ on: n <= pwdStrength.score }"></span>
              </div>
              <span class="strength-label">{{ pwdStrength.label ? '强度：' + pwdStrength.label : '' }}</span>
            </div>

            <!-- 注册模式：实时校验提示 -->
            <p v-if="mode === 'register' && accountState === 'err'" class="hint">账号至少 3 位，或填写邮箱 / 手机号</p>
            <p v-if="mode === 'register' && passwordState === 'err'" class="hint">密码需 6-15 位</p>
            <p v-if="mode === 'register' && confirmState === 'err'" class="hint">两次输入的密码不一致</p>

            <!-- 注册模式：图形验证码 -->
            <div v-if="mode === 'register'" class="field">
              <label for="code">验 证 码</label>
              <div class="code-row">
                <input
                  id="code"
                  v-model="code"
                  class="input"
                  placeholder="请输入图中 4 位字符"
                  maxlength="4"
                  autocomplete="off"
                />
                <button
                  v-if="!captchaImg"
                  type="button"
                  class="code-btn"
                  :disabled="captchaLoading"
                  @click="loadCaptcha"
                >
                  {{ captchaLoading ? '加载中…' : '获取验证码' }}
                </button>
                <img
                  v-else
                  class="captcha-img"
                  :src="captchaImg"
                  title="看不清？点击刷新"
                  alt="图形验证码"
                  @click="loadCaptcha"
                />
              </div>
              <span class="line"></span>
            </div>

            <div v-if="mode === 'login'" class="row">
              <label class="remember">
                <input v-model="remember" type="checkbox" />
                <span class="checkbox">
                  <svg viewBox="0 0 24 24" fill="none" stroke="#f8f2e4" stroke-width="3.4" stroke-linecap="round">
                    <path d="M4,12 L10,18 L20,6" />
                  </svg>
                </span>
                记住我
              </label>
              <a href="#" class="link" @click.prevent="onForgot">忘记密码？</a>
            </div>

            <button class="btn primary block" type="submit" :class="{ loading }" :disabled="loading">
              <span class="spinner"></span>
              <span>{{ submitLabel }}</span>
            </button>

            <!-- 游客免注册：一键直入主页，不经过注册 / 登录页 -->
            <button class="btn ghost block" type="button" :disabled="loading" @click="guestLogin">
              游 客 免 注 册 · 直 入 阁
            </button>

            <div class="msg" :class="messageType">{{ message }}</div>
          </form>

          <div class="divider">{{ brand.divider }}</div>

          <div class="social">
            <button type="button" title="敬请期待" disabled>
              <svg viewBox="0 0 24 24" fill="#4e8d86"><path d="M9.2,4 C5.4,4 2.4,6.6 2.4,9.8 c0,1.8 1,3.5 2.6,4.5 l-0.7,2.1 2.5-1.3 c0.8,0.2 1.6,0.3 2.4,0.3 h0.3 C9.1,14.8 9,14.2 9,13.5 9,10.4 12,8 15.4,8 c0.3,0 0.7,0 1,0.1 C15.7,5.7 12.8,4 9.2,4 Z M7.2,8.2 c0.5,0 0.8,0.4 0.8,0.8 s-0.4,0.8 -0.8,0.8 -0.8,-0.4 -0.8,-0.8 0.4,-0.8 0.8,-0.8 Z M11.4,8.2 c0.5,0 0.8,0.4 0.8,0.8 s-0.4,0.8 -0.8,0.8 -0.8,-0.4 -0.8,-0.8 0.4,-0.8 0.8,-0.8 Z M15.5,9 c-3.2,0 -5.7,2.1 -5.7,4.6 0,2.6 2.6,4.6 5.7,4.6 0.7,0 1.4,-0.1 2,-0.3 l2.1,1.1 -0.6,-1.8 c1.5,-0.9 2.5,-2.3 2.5,-3.9 C21.5,11 18.7,9 15.5,9 Z M13.6,12 c0.4,0 0.7,0.3 0.7,0.7 s-0.3,0.7 -0.7,0.7 -0.7,-0.3 -0.7,-0.7 0.3,-0.7 0.7,-0.7 Z M17.4,12 c0.4,0 0.7,0.3 0.7,0.7 s-0.3,0.7 -0.7,0.7 -0.7,-0.3 -0.7,-0.7 0.3,-0.7 0.7,-0.7 Z"/></svg>
            </button>
            <button type="button" title="敬请期待" disabled>
              <svg viewBox="0 0 24 24" fill="#a37f3d"><path d="M12,2 C8.2,2 5.2,5.1 5.2,9 c0,1.2 0.2,2.3 0.6,3.3 C5,13.6 4.2,15.4 4.2,16.4 c0,0.6 0.5,0.8 1,0.4 0.5,-0.3 1.1,-0.9 1.6,-1.4 0.2,0.3 0.4,0.5 0.7,0.8 C6.6,17 6.2,18 6.2,18.6 c0,0.5 0.4,0.6 0.9,0.3 0.6,-0.4 1.3,-1.1 1.8,-1.7 0.9,0.2 1.9,0.3 3.1,0.3 s2.2,-0.1 3.1,-0.3 c0.5,0.6 1.2,1.3 1.8,1.7 0.5,0.3 0.9,0.2 0.9,-0.3 0,-0.6 -0.4,-1.6 -1.3,-2.4 0.3,-0.3 0.5,-0.5 0.7,-0.8 0.5,0.5 1.1,1.1 1.6,1.4 0.5,0.4 1,0.2 1,-0.4 0,-1 -0.8,-2.8 -1.6,-4.1 0.4,-1 0.6,-2.1 0.6,-3.3 C18.8,5.1 15.8,2 12,2 Z"/></svg>
            </button>
            <button type="button" title="敬请期待" disabled>
              <svg viewBox="0 0 24 24" fill="none" stroke="#b3362b" stroke-width="1.8">
                <rect x="3" y="6" width="18" height="13" rx="2" /><path d="M3,8 L12,14 L21,8" />
              </svg>
            </button>
          </div>

          <p class="social-tip">第三方登录 · 敬请期待</p>

          <p class="foot">
            <template v-if="mode === 'login'">
              尚无账号？<a href="#" class="link" @click.prevent="switchMode('register')"><b>立即注册</b></a>
            </template>
            <template v-else>
              已有账号？<a href="#" class="link" @click.prevent="switchMode('login')"><b>去登录</b></a>
            </template>
          </p>
        </div>
      </div>
    </div>

    <!-- 底部落款 -->
    <div class="login-footer">离恨烟 © 烟雨阁</div>
  </section>
</template>

<style scoped>
/* ===========================================================
   登录页 · 立绘分栏布局（参考 lihengyan-login/index.html）
   卡片、表单、验证码等样式沿用全局 ink.css
   =========================================================== */

.login-stage {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

/* 飘浮烟雾 */
.login-mist {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.35;
  z-index: 1;
  pointer-events: none;
  animation: mist-drift 18s ease-in-out infinite alternate;
}

.login-mist.m1 {
  width: 520px;
  height: 520px;
  background: #a9cdc5;
  top: -120px;
  left: -120px;
}

.login-mist.m2 {
  width: 420px;
  height: 420px;
  background: #bcd8d1;
  bottom: -140px;
  right: 26%;
  animation-delay: -6s;
}

.login-mist.m3 {
  width: 300px;
  height: 300px;
  background: #e6d9bd;
  top: 30%;
  right: -100px;
  animation-delay: -12s;
  opacity: 0.25;
}

@keyframes mist-drift {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(60px, 30px) scale(1.12);
  }
}

/* 主布局：左立绘 / 右登录卡 */
.stage {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  align-items: stretch;
}

/* 左侧立绘 */
.portrait {
  position: relative;
  flex: 1.15;
  min-width: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.portrait::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse 70% 90% at 50% 45%,
    rgba(244, 241, 232, 0) 40%,
    rgba(207, 227, 222, 0.55) 100%
  );
  z-index: 2;
  pointer-events: none;
}

.portrait img {
  height: 100%;
  width: 100%;
  object-fit: cover;
  object-position: center 20%;
  -webkit-mask-image: linear-gradient(
    to right,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.25) 6%,
    #000 18%,
    #000 82%,
    rgba(0, 0, 0, 0.55) 94%,
    rgba(0, 0, 0, 0) 100%
  );
  mask-image: linear-gradient(
    to right,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.25) 6%,
    #000 18%,
    #000 82%,
    rgba(0, 0, 0, 0.55) 94%,
    rgba(0, 0, 0, 0) 100%
  );
  filter: saturate(1.05);
  animation: portrait-in 1.6s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}

@keyframes portrait-in {
  from {
    opacity: 0;
    transform: scale(1.06) translateY(20px);
    filter: saturate(0.4) blur(6px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
    filter: saturate(1.05) blur(0);
  }
}

/* 竖排诗题 */
.verse {
  position: absolute;
  left: 42px;
  top: 50%;
  transform: translateY(-50%);
  writing-mode: vertical-rl;
  z-index: 3;
  font-size: 22px;
  letter-spacing: 14px;
  color: var(--ink-soft);
  opacity: 0.75;
  user-select: none;
  animation: verse-fade 1.8s 0.4s both;
}

.verse b {
  color: var(--cinnabar);
  font-weight: normal;
}

@keyframes verse-fade {
  from {
    opacity: 0;
    transform: translateY(calc(-50% + 16px));
  }
  to {
    opacity: 0.75;
    transform: translateY(-50%);
  }
}

/* 右侧登录区 */
.panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

/* 卡片进场动画（全局 .card 样式之外的补充） */
.card {
  animation: card-in 1.2s 0.3s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(36px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 抖动动画优先于进场动画 */
.card.shake {
  animation: card-shake 0.4s;
}

/* 表单字段右侧 SVG 图标 */
.field .ico {
  position: absolute;
  right: 4px;
  bottom: 9px;
  width: 20px;
  height: 20px;
  opacity: 0.55;
  pointer-events: none;
}

/* 密码框「油纸伞」图标：闭合=隐藏 / 撑开=显示 */
.field .icon .umb {
  width: 20px;
  height: 20px;
  display: block;
  transition: transform 0.2s ease;
}
.field .icon:hover .umb {
  transform: scale(1.12) rotate(-5deg);
}

/* 游客免注册按钮：整行幽灵按钮，与主按钮拉开间距 */
.btn.ghost.block {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
  letter-spacing: 4px;
  text-indent: 4px;
}

.btn.ghost.block:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* 确认密码一致时的对勾 */
.ico-check {
  position: absolute;
  right: 6px;
  bottom: 9px;
  font-size: 15px;
  color: var(--jade);
  pointer-events: none;
}

/* 密码强度条（注册模式） */
.pwd-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: -10px 0 18px;
}

.pwd-strength .bars {
  display: flex;
  gap: 6px;
  flex: 1;
}

.pwd-strength .bar {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: rgba(61, 80, 75, 0.18);
  transition: background 0.35s, box-shadow 0.35s;
}

.pwd-strength .bar.on {
  background: var(--jade);
}

.pwd-strength.s-weak .bar.on {
  background: #b3362b;
}
.pwd-strength.s-fair .bar.on {
  background: #c8923a;
}
.pwd-strength.s-good .bar.on {
  background: #4e8d86;
}
.pwd-strength.s-strong .bar.on {
  background: #2f6f4f;
  box-shadow: 0 0 8px rgba(47, 111, 79, 0.45);
}

.pwd-strength .strength-label {
  font-size: 12.5px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  white-space: nowrap;
  min-width: 56px;
  text-align: right;
}

/* 注册实时校验提示 */
.hint {
  margin: -12px 0 16px;
  font-size: 12.5px;
  letter-spacing: 1px;
  color: var(--cinnabar);
  opacity: 0.85;
  animation: msg-in 0.3s ease both;
}

/* 统一：聚焦 / 校验 下划线动画（与聚焦态同机制，颜色区分状态） */
.field .input.ok ~ .line {
  width: 100%;
  background: linear-gradient(90deg, var(--jade), var(--jade));
}
.field .input.err ~ .line {
  width: 100%;
  transform-origin: left;
  background: linear-gradient(90deg, var(--cinnabar), #d9594d);
  animation: line-err 0.4s ease;
}

@keyframes line-err {
  0% { transform: scaleX(0.55); opacity: 0.4; }
  55% { transform: scaleX(1.03); }
  100% { transform: scaleX(1); opacity: 1; }
}

/* 底部消息与提示统一进场动画 */
.msg {
  animation: msg-in 0.3s ease both;
}

@keyframes msg-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 第三方登录 SVG 图标尺寸 */
.social button svg {
  width: 20px;
  height: 20px;
}

/* 社交按钮：敬请期待 禁用态 */
.social button.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  filter: grayscale(0.85);
}

/* 第三方登录提示 */
.social-tip {
  margin: 6px 0 0;
  font-size: 12px;
  letter-spacing: 3px;
  text-align: center;
  color: var(--ink-soft);
  opacity: 0.6;
}

/* 底部落款 */
.login-footer {
  position: absolute;
  bottom: 18px;
  right: 26px;
  z-index: 3;
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(61, 80, 75, 0.5);
}

/* 窄屏：隐藏立绘与诗题，仅保留登录卡 */
@media (max-width: 900px) {
  .portrait {
    display: none;
  }
  .panel {
    flex: 1;
    padding: 32px 24px;
  }
}

/* 移动端：页面可滚动、字号收敛、落款改为静态居中 */
@media (max-width: 560px) {
  .login-stage {
    height: auto;
    min-height: 100vh;
    min-height: 100dvh;
    overflow-x: hidden;
    overflow-y: auto;
  }
  .stage {
    height: auto;
    min-height: 100vh;
    min-height: 100dvh;
  }
  .panel {
    padding: 26px 16px;
    align-items: flex-start;
  }
  .brand h1 {
    font-size: 26px;
    letter-spacing: 6px;
  }
  .brand .sub {
    font-size: 12px;
    letter-spacing: 2px;
  }
  .btn.primary {
    font-size: 15px;
    letter-spacing: 4px;
  }
  .btn.ghost.block {
    font-size: 14px;
    letter-spacing: 2px;
  }
  .login-footer {
    position: static;
    text-align: center;
    margin-top: 20px;
    right: auto;
    bottom: auto;
  }
  .social {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
