/**
 * 认证 API 层
 * 对接 Spring Boot 后端（/api/auth/*，开发环境经 Vite 代理转发到 http://192.168.68.30:9090）
 * 统一返回结构：{ success, message, token, data }
 */

const BASE = '/api/auth'

/** 请求超时（毫秒）：后端不可达时不让界面永远卡在等待态 */
const TIMEOUT_MS = 8000

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }

  const ctrl = typeof AbortController === 'function' ? new AbortController() : null
  const timer = ctrl ? setTimeout(() => ctrl.abort(), TIMEOUT_MS) : null

  try {
    const res = await fetch(`${BASE}${path}`, {
      ...options,
      headers,
      ...(ctrl ? { signal: ctrl.signal } : {}),
    })
    // 先读原文再按需解析：后端返回的是约定 JSON，还是代理/路由兜底返回的 HTML 首页
    const text = await res.text()
    let body = null
    try {
      body = text ? JSON.parse(text) : null
    } catch {
      body = null
    }

    if (!res.ok) {
      // 非 2xx：优先采用后端返回的业务消息（如「该账号已注册」），避免笼统成「服务异常」
      const msg = body && body.message ? body.message : `服务异常（HTTP ${res.status}），请稍后再试`
      return { success: false, message: msg }
    }
    // 约定结构必须带 success 字段；若拿到的是 HTML（如代理兜底），视为后端未就绪
    if (!body || typeof body.success === 'undefined') {
      return { success: false, message: '后端未就绪或网络不通，请确认服务已启动' }
    }
    return body
  } catch {
    return { success: false, message: '网络异常或请求超时，请稍后再试' }
  } finally {
    if (timer) clearTimeout(timer)
  }
}

function post(path, body = {}) {
  return request(path, { method: 'POST', body: JSON.stringify(body) })
}

/** 获取图形验证码 -> { success, data: { key, image(base64 data URL) } } */
export function getCaptcha() {
  return request('/captcha')
}

/** 注册：{ account, password, code, captchaKey } -> { success, token, message } */
export function register(account, password, code, captchaKey) {
  return post('/register', { account, password, code, captchaKey })
}

/** 登录：{ account, password } -> { success, token, message, data: account } */
export function login(account, password) {
  return post('/login', { account, password })
}

/** 登出：{ token } -> { success, message } */
export function logout(token) {
  return post('/logout', { token })
}

/** 当前登录用户自身信息 -> { success, data: { account, role } } */
export function fetchMe(token) {
  return request('/me', {
    headers: token ? { Authorization: 'Bearer ' + token } : {},
  })
}
