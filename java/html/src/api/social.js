/**
 * 社交 API 层（好友 + 传音）
 *
 * 后端接口（false-god-backend，:8080，均需 Bearer token）：
 *   GET    /api/friends                              -> [{ friendId, username, remark, addedAt }]
 *   POST   /api/friends  { username }                -> { friendId, username }
 *   DELETE /api/friends/{friendId}                   -> 无 data
 *   GET    /api/chat/messages?with={uid}&after={id}  -> [{ id, mine, content, time }]
 *   POST   /api/chat/send { to, content }            -> { id, content }
 *
 * 统一响应：{ code: 0, message: "ok", data }，code === 0 为成功。
 */

import { getSession } from './session.js'

/**
 * 统一请求封装：自动携带本地会话 token
 */
async function request(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const token = getSession() && getSession().token
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

/** 好友列表 */
export function fetchFriends() {
  return request('/api/friends')
}

/**
 * 结契（加好友）
 * @param {string} username 对方真名
 */
export function addFriend(username) {
  return request('/api/friends', { method: 'POST', body: { username } })
}

/**
 * 解契（删好友）
 * @param {number} friendId 好友 id
 */
export function removeFriend(friendId) {
  return request(`/api/friends/${friendId}`, { method: 'DELETE' })
}

/**
 * 拉取与某人的传音记录
 * @param {number} friendId 好友 id
 * @param {number} afterId  增量拉取：只取 id 大于该值的消息，首次传 0
 */
export function fetchMessages(friendId, afterId = 0) {
  return request(`/api/chat/messages?with=${friendId}&after=${afterId}`)
}

/**
 * 发送传音
 * @param {number} friendId 好友 id
 * @param {string} content  内容（上限 500 字）
 */
export function sendMessage(friendId, content) {
  return request('/api/chat/send', { method: 'POST', body: { to: friendId, content } })
}
