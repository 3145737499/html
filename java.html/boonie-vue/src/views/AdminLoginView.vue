<script setup>
/**
 * 后台管理 · 登录页（仅登录，无注册）
 * 复用 /api/auth/login：账号 + 密码 -> token，登录态写入 user store 后进入 /admin
 * 与首页 LoginView 的区分：无「注册 / 图形验证码 / 游客免注册」入口，仅作后台入场
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'
import { login as apiLogin } from '@/api/auth'
import { fetchProfile } from '@/api/admin'

const router = useRouter()
const user = useUserStore()

const account = ref('')
const password = ref('')
const remember = ref(false)
const showPassword = ref(false)
const loading = ref(false)
const message = ref('')
const messageType = ref('')
const shaking = ref(false)
const pwdInput = ref(null)

const brand = {
  title: '烟 雨 阁 · 后 台',
  sub: 'ADMIN CONSOLE · 江湖拾遗管理',
}

const submitLabel = computed(() => (loading.value ? '登录中…' : '登 录 入 阁'))

onMounted(() => {
  const saved = user.restore()
  if (saved) {
    account.value = saved
    remember.value = true
  }
  nextTick(() => pwdInput.value?.focus())
})

function setMsg(text, type = '') {
  message.value = text
  messageType.value = type
}

function shake() {
  shaking.value = true
  setTimeout(() => (shaking.value = false), 420)
}

function togglePassword() {
  showPassword.value = !showPassword.value
}

async function onSubmit() {
  const acc = account.value.trim()
  const pwd = password.value
  if (!acc || pwd.length < 6) {
    shake()
    setMsg('请检查账号或密码（密码 6-15 位）', 'err')
    return
  }
  if (pwd.length > 15) {
    shake()
    setMsg('密码长度不能超过 15 位', 'err')
    return
  }

  loading.value = true
  setMsg('')
  try {
    const res = await apiLogin(acc, pwd)
    if (!res.success) {
      loading.value = false
      shake()
      setMsg(res.message || '登录失败，请稍后再试', 'err')
      return
    }
    user.login(acc, remember.value, res.token || '')
    setMsg('登录成功，校验权限中…', 'ok')
    loading.value = false

    // 校验是否拥有后台管理员权限，非管理员拒绝进入
    try {
      const prof = await fetchProfile()
      if (!prof.success || !prof.data || !prof.data.isAdmin) {
        user.logout()
        shake()
        setMsg('该账号无后台管理权限，请使用管理员账号登录', 'err')
        return
      }
      user.setRole(prof.data.role || 'admin')
      // 角色刚由后端核实过，标记为已校验，避免路由守卫再请求一次 /me
      user.markProfileVerified()
    } catch {
      user.logout()
      shake()
      setMsg('网络异常，无法校验权限', 'err')
      return
    }

    router.push({ name: 'admin' })
  } catch {
    loading.value = false
    shake()
    setMsg('网络异常，无法连接服务器', 'err')
  }
}

function backHome() {
  router.push({ path: '/' })
}
</script>

<template>
  <section class="view admin-login-stage">
    <div class="login-mist m1"></div>
    <div class="login-mist m2"></div>

    <div class="panel">
      <div class="card" :class="{ shake: shaking }">
        <span class="corner tl"></span><span class="corner tr"></span>
        <span class="corner bl"></span><span class="corner br"></span>

        <div class="seal seal-mark">阁</div>

        <div class="brand">
          <h1>{{ brand.title }}</h1>
          <p class="sub">{{ brand.sub }}</p>
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
              placeholder="请输入后台账号 / 侠客名号"
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
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码（6-15 位）"
              autocomplete="current-password"
              maxlength="15"
            />
            <span class="line"></span>
            <span class="icon" role="button" :title="showPassword ? '隐藏密码' : '显示密码'" @click="togglePassword">
              <svg v-if="!showPassword" class="umb" viewBox="0 0 24 24" fill="none" stroke="#3d504b" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2.5 C10 5 10 8 12 10 C14 8 14 5 12 2.5 Z" fill="rgba(179,54,43,0.14)" />
                <path d="M12 10 V19.5 Q12 21.5 10 21.5" />
              </svg>
              <svg v-else class="umb" viewBox="0 0 24 24" fill="none" stroke="#3d504b" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 10 C3 4.5 8 3 12 3 C16 3 21 4.5 21 10" fill="rgba(179,54,43,0.14)" />
                <path d="M3 10 Q12 13 21 10" />
                <path d="M12 3 V10" /><path d="M8 4.8 V10" /><path d="M16 4.8 V10" /><path d="M12 3 V1.6" />
                <path d="M12 10 V19.5 Q12 21.5 10 21.5" />
              </svg>
            </span>
          </div>

          <div class="row">
            <label class="remember">
              <input v-model="remember" type="checkbox" />
              <span class="checkbox">
                <svg viewBox="0 0 24 24" fill="none" stroke="#f8f2e4" stroke-width="3.4" stroke-linecap="round">
                  <path d="M4,12 L10,18 L20,6" />
                </svg>
              </span>
              记住我
            </label>
            <a href="#" class="link" @click.prevent="backHome">返回首页</a>
          </div>

          <button class="btn primary block" type="submit" :class="{ loading }" :disabled="loading">
            <span class="spinner"></span>
            <span>{{ submitLabel }}</span>
          </button>

          <p class="tip">此入口仅用于后台登录，无注册功能</p>

          <div class="msg" :class="messageType">{{ message }}</div>
        </form>
      </div>
    </div>

    <div class="login-footer">离恨烟 © 烟雨阁 · 后台管理</div>
  </section>
</template>

<style scoped>
.admin-login-stage {
  position: relative;
  height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

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
  width: 520px; height: 520px;
  background: #a9cdc5; top: -120px; left: -120px;
}
.login-mist.m2 {
  width: 420px; height: 420px;
  background: #bcd8d1; bottom: -140px; right: 22%;
  animation-delay: -6s;
}
@keyframes mist-drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(60px, 30px) scale(1.12); }
}

.panel {
  position: relative;
  z-index: 2;
  width: 420px;
  max-width: 92vw;
  padding: 20px;
}

.card {
  animation: card-in 1.2s 0.2s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(36px); }
  to { opacity: 1; transform: translateY(0); }
}
.card.shake { animation: card-shake 0.4s; }
@keyframes card-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-9px); }
  40% { transform: translateX(9px); }
  60% { transform: translateX(-6px); }
  80% { transform: translateX(6px); }
}

/* 表单字段右侧图标 */
.field .ico {
  position: absolute; right: 4px; bottom: 9px;
  width: 20px; height: 20px; opacity: 0.55; pointer-events: none;
}
.field .icon .umb { width: 20px; height: 20px; display: block; transition: transform 0.2s ease; }
.field .icon:hover .umb { transform: scale(1.12) rotate(-5deg); }

.tip {
  margin: 14px 0 0;
  text-align: center;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  opacity: 0.6;
}

.login-footer {
  position: absolute;
  bottom: 18px; right: 26px;
  z-index: 3;
  font-size: 12px; letter-spacing: 3px;
  color: rgba(61, 80, 75, 0.5);
}

@media (max-width: 560px) {
  .admin-login-stage { height: auto; min-height: 100vh; min-height: 100dvh; overflow-y: auto; }
  .panel { padding: 26px 16px; align-self: flex-start; margin-top: 40px; }
  .brand h1 { font-size: 26px; letter-spacing: 6px; }
  .brand .sub { font-size: 12px; letter-spacing: 2px; }
  .login-footer { position: static; text-align: center; margin-top: 20px; right: auto; bottom: auto; }
}
</style>
