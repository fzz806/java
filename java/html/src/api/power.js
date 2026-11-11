/**
 * 战力提升 API 层
 *
 * 后端接口（springBootTest，:8081，均需 Bearer token）：
 *   GET  /api/power          战力面板
 *   POST /api/power/upgrade  { type: realm | seal | skill | essence | rest }
 *
 * 统一响应：{ code: 0, message: 'ok', data: {...} }
 */

import { getSession } from './session.js'

async function request(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const session = getSession()
  if (session && session.token) headers.Authorization = `Bearer ${session.token}`

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

  if (res.status === 401) {
    return { code: 401, message: '神印凭证已失效，请重新登临', data: null }
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

/** 拉取战力面板 */
export function fetchPower() {
  return request('/api/power')
}

/** 执行一次提升 */
export function upgradePower(type) {
  return request('/api/power/upgrade', { method: 'POST', body: { type } })
}
