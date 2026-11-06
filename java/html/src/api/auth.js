/**
 * 登录认证 API 层
 *
 * 后端接口（false-god-backend，:8080）：
 *   POST /api/login       { username, password } -> { username, token, expiresIn }
 *   POST /api/register    { username, password } -> { username }
 *   GET  /api/me          需 Authorization: Bearer <token> -> { id, username, createdAt }
 *   GET  /api/ping        健康检查
 *
 * 统一响应：{ "code": 0, "message": "ok", "data": {...} }，code === 0 为成功。
 */

// 已对接本地 Spring Boot 后端；如需临时回到 Mock 模式改回 true
const USE_MOCK = false

/**
 * 统一请求封装：自动附加 Authorization、统一处理网络异常
 * @param {string} path
 * @param {{ method?: string, body?: object, token?: string }} options
 */
async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined
    })
  } catch (e) {
    return { code: -1, message: '无法连接神堂（后端服务未启动？）', data: null }
  }

  // 401 由后端拦截器返回，直接透出为未授权
  if (res.status === 401) {
    let msg = '神印凭证已失效，请重新登临'
    try {
      const j = await res.json()
      if (j && j.message) msg = j.message
    } catch (e) {
      /* 保持默认文案 */
    }
    return { code: 401, message: msg, data: null }
  }

  if (!res.ok) {
    return { code: res.status, message: `神堂应答异常（HTTP ${res.status}）`, data: null }
  }

  try {
    return await res.json()
  } catch (e) {
    return { code: -2, message: '神堂应答格式异常', data: null }
  }
}


/**
 * 登录
 * @param {{ username: string, password: string }} payload
 * @returns {Promise<{ code: number, message: string, data: object }>}
 */
export async function login({ username, password }) {
  if (USE_MOCK) {
    // Mock：模拟网络延迟，直接放行（密码非空即可）
    await sleep(400)
    if (!username || !password) {
      return { code: 1, message: '真名与神印不可为空', data: null }
    }
    return {
      code: 0,
      message: 'ok',
      data: { username, token: `mock-token-${Date.now()}` }
    }
  }
  return request('/api/login', { method: 'POST', body: { username, password } })
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * 注册
 * @param {{ username: string, password: string }} payload
 * @returns {Promise<{ code: number, message: string, data: object }>}
 */
export async function register({ username, password }) {
  if (USE_MOCK) {
    await sleep(500)
    if (!username || !password) {
      return { code: 1, message: '真名与神印不可为空', data: null }
    }
    // Mock：模拟"真名已被神堂登记"的情况，方便测试失败分支
    if (username === '陈昊宇') {
      return { code: 2, message: '此真名已被神堂登记，请另取伪名', data: null }
    }
    return {
      code: 0,
      message: 'ok',
      data: { username }
    }
  }
  return request('/api/register', { method: 'POST', body: { username, password } })
}

/**
 * 获取当前登录用户（用于校验 token 是否仍有效）
 * @param {string} token
 * @returns {Promise<{ code: number, message: string, data: object }>}
 *   code === 401 表示 token 失效，调用方应清除本地登录态
 */
export async function fetchMe(token) {
  if (USE_MOCK) {
    await sleep(200)
    return token ? { code: 0, message: 'ok', data: { username: 'mock' } } : { code: 401, message: '未登录', data: null }
  }
  return request('/api/me', { method: 'GET', token })
}

/**
 * 登出（无状态 JWT，服务端仅作形式应答）
 * @param {string} token
 */
export async function logout(token) {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  return request('/api/logout', { method: 'POST', token })
}

/**
 * 后端健康检查
 * @returns {Promise<boolean>}
 */
export async function ping() {
  if (USE_MOCK) return true
  const r = await request('/api/ping', { method: 'GET' })
  return r.code === 0
}
