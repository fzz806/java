/**
 * 恶鬼之牙 · 公会完整系统 API
 *
 * 后端接口（false-god-backend，:8080，均需 Bearer token）：
 *   GET    /api/guild/overview        -> { info, members, memberCount, warehouse, notices, rank, me, applies }
 *   POST   /api/guild/apply           { reason }              递交入会名帖
 *   POST   /api/guild/{id}/handle     ?pass=&note=            批阅名帖（仅会长）
 *   POST   /api/guild/quit                                    退出公会
 *   PUT    /api/guild/members/{id}/role  { role }             任免职位（仅会长）
 *   DELETE /api/guild/members/{id}                            移除成员（仅会长）
 *   POST   /api/guild/notice          { tag, title, content } 发布公告（仅会长）
 *   DELETE /api/guild/notice/{id}                             撤下公告（仅会长）
 *   PUT    /api/guild/info            { key, value }          修订简报（仅会长）
 *   POST   /api/guild/contribute                              每日供奉换贡献
 */

import { getSession } from './session.js'

function authHeaders() {
  const headers = { 'Content-Type': 'application/json' }
  const session = getSession()
  if (session && session.token) headers.Authorization = `Bearer ${session.token}`
  return headers
}

async function request(path, options = {}) {
  let res
  try {
    res = await fetch(path, { ...options, headers: { ...authHeaders(), ...(options.headers || {}) } })
  } catch (e) {
    return { code: -1, message: '无法连接神堂（后端服务未启动？）' }
  }
  if (res.status === 401) return { code: 401, message: '神印凭证已失效，请重新登临' }
  if (!res.ok) return { code: res.status, message: `神堂应答异常（HTTP ${res.status}）` }
  try {
    return await res.json()
  } catch (e) {
    return { code: -2, message: '神堂应答格式异常' }
  }
}

/** 总览 */
export function fetchGuildOverview() {
  return request('/api/guild/overview')
}

/** 递交名帖 */
export function applyGuild(reason) {
  return request('/api/guild/apply', { method: 'POST', body: JSON.stringify({ reason }) })
}

/** 批阅名帖（仅会长） */
export function handleApply(id, pass, note = '') {
  return request(`/api/guild/${id}/handle?pass=${pass ? 'true' : 'false'}&note=${encodeURIComponent(note)}`, {
    method: 'POST'
  })
}

/** 退出公会 */
export function quitGuild() {
  return request('/api/guild/quit', { method: 'POST' })
}

/** 任免职位（仅会长） */
export function setMemberRole(id, role) {
  return request(`/api/guild/members/${id}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role })
  })
}

/** 移除成员（仅会长） */
export function removeMember(id) {
  return request(`/api/guild/members/${id}`, { method: 'DELETE' })
}

/** 发布公告（仅会长） */
export function publishNotice(tag, title, content) {
  return request('/api/guild/notice', {
    method: 'POST',
    body: JSON.stringify({ tag, title, content })
  })
}

/** 撤下公告（仅会长） */
export function deleteNotice(id) {
  return request(`/api/guild/notice/${id}`, { method: 'DELETE' })
}

/** 修订简报（仅会长） */
export function updateGuildInfo(key, value) {
  return request('/api/guild/info', {
    method: 'PUT',
    body: JSON.stringify({ key, value })
  })
}

/** 每日供奉 */
export function contributeGuild() {
  return request('/api/guild/contribute', { method: 'POST' })
}
