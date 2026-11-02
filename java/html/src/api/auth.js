/**
 * 登录认证 API 层
 *
 * 对接真实后端（Spring Boot）时只需两步：
 *   1. 将 USE_MOCK 改为 false
 *   2. 确保 vite.config.js 中 /api 代理指向后端地址（默认 http://localhost:8080）
 *
 * 约定的接口格式（可在后端调整后同步修改 parse 函数）：
 *   POST /api/login
 *   请求体: { "username": "xxx", "password": "xxx" }
 *   响应体: { "code": 0, "message": "ok", "data": { "username": "xxx", "token": "..." } }
 *   code === 0 视为成功，其余视为失败（message 为失败原因）
 */

// 已对接本地 Spring Boot 后端（false-god-backend，端口 8080）
// 如需临时回到 Mock 模式，改回 true 即可
const USE_MOCK = false

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

  let res
  try {
    res = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
  } catch (e) {
    return { code: -1, message: '无法连接神堂（后端服务未启动？）', data: null }
  }

  if (!res.ok) {
    return { code: res.status, message: `神堂应答异常（HTTP ${res.status}）`, data: null }
  }
  return res.json()
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * 注册
 * 约定接口：POST /api/register
 *   请求体: { "username": "xxx", "password": "xxx" }
 *   响应体: { "code": 0, "message": "ok", "data": { "username": "xxx" } }
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

  let res
  try {
    res = await fetch('/api/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
  } catch (e) {
    return { code: -1, message: '无法连接神堂（后端服务未启动？）', data: null }
  }

  if (!res.ok) {
    return { code: res.status, message: `神堂应答异常（HTTP ${res.status}）`, data: null }
  }
  return res.json()
}
