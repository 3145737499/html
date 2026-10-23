/**
 * 后台管理 API 层
 * 对接 Spring Boot 后端 /api/admin/*（开发环境经 Vite 代理转发到 http://localhost:9090）
 * 所有接口均需在请求头携带 Authorization: Bearer <token>（token 来自登录后 localStorage 的 bb_token）
 * 统一返回结构：{ success, message, data }；未登录返回 { success:false, code:401, message:"未登录或登录已失效…" }
 */

const BASE = '/api/admin'

/** 从 localStorage 读取登录令牌（与 user store 的持久化键保持一致） */
function token() {
  try {
    return localStorage.getItem('bb_token') || ''
  } catch {
    return ''
  }
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const t = token()
  if (t) headers['Authorization'] = 'Bearer ' + t

  let res
  try {
    res = await fetch(`${BASE}${path}`, { headers, ...options })
  } catch {
    return { success: false, message: '网络异常，无法连接服务器' }
  }

  if (res.status === 401) {
    return { success: false, code: 401, message: '登录已失效，请重新登录' }
  }
  if (res.status === 403) {
    return { success: false, code: 403, message: '该账号无后台管理权限' }
  }
  if (!res.ok) {
    return { success: false, message: `服务异常（HTTP ${res.status}），请稍后再试` }
  }
  return res.json()
}

function get(path) {
  return request(path, { method: 'GET' })
}
function post(path, body = {}) {
  return request(path, { method: 'POST', body: JSON.stringify(body) })
}
function put(path, body = {}) {
  return request(path, { method: 'PUT', body: JSON.stringify(body) })
}
function del(path) {
  return request(path, { method: 'DELETE' })
}

/* ---------------- 内容条目（app_item） ---------------- */

/** 分页列表：{ page, pageSize, keyword } -> { success, data:{ items, total, page, pageSize } } */
export function fetchItems({ page = 1, pageSize = 10, keyword = '' } = {}) {
  const kw = encodeURIComponent(keyword || '')
  return get(`/items?page=${page}&pageSize=${pageSize}&keyword=${kw}`)
}

/** 详情 */
export function fetchItem(id) {
  return get(`/items/${id}`)
}

/** 新增 */
export function createItem(body) {
  return post('/items', body)
}

/** 修改 */
export function updateItem(id, body) {
  return put(`/items/${id}`, body)
}

/** 删除 */
export function removeItem(id) {
  return del(`/items/${id}`)
}

/* ---------------- 分类（app_category） ---------------- */

/** 列表 */
export function fetchCategories() {
  return get('/categories')
}

/** 新增 */
export function createCategory(body) {
  return post('/categories', body)
}

/** 修改 */
export function updateCategory(id, body) {
  return put(`/categories/${id}`, body)
}

/** 删除 */
export function removeCategory(id) {
  return del(`/categories/${id}`)
}

/* ---------------- 当前管理员信息 ---------------- */

/** 取当前登录管理员：{ success, data:{ account, role, isAdmin } } */
export function fetchProfile() {
  return get('/me')
}
