/**
 * 内容 API 层（神技图鉴 / 镇兽大比 / 恶鬼之牙 / 战力榜）
 *
 * 后端接口（false-god-backend，:8080，均需 Bearer token）：
 *   GET /api/skills        -> 神技图鉴
 *   GET /api/arena/matches -> 镇兽大比赛程
 *   GET /api/guild         -> { info, members } 公会简报 + 成员
 *   GET /api/rank          -> 战力榜（战力倒序）
 */

import { getSession } from './session.js'

async function request(path) {
  const headers = { 'Content-Type': 'application/json' }
  const session = getSession()
  if (session && session.token) headers.Authorization = `Bearer ${session.token}`

  let res
  try {
    res = await fetch(path, { headers })
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

export function fetchSkills() {
  return request('/api/skills')
}

export function fetchMatches() {
  return request('/api/arena/matches')
}

export function fetchGuild() {
  return request('/api/guild')
}

export function fetchRank() {
  return request('/api/rank')
}

/** 首页概览：神堂公告 + 仓库库存（库存跟着战力状态变） */
export function fetchHome() {
  return request('/api/home')
}
