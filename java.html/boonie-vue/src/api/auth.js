/**
 * 认证 API 层
 * 对接 Spring Boot 后端（/api/auth/*，开发环境经 Vite 代理转发到 http://localhost:9090）
 * 统一返回结构：{ success, message, token, data }
 */

const BASE = '/api/auth'

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    // 网络层 / 服务端 5xx 等异常
    return { success: false, message: `服务异常（HTTP ${res.status}），请稍后再试` }
  }
  return res.json()
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
