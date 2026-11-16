/**
 * 神格任务 API 层
 *
 * 后端接口（false-god-backend，:8080，均需 Bearer token）：
 *   GET  /api/task        任务面板
 *                             -> { level, title, nextTitle, exp, expMax, totalExp, percent,
 *                                  levelMax, full, claimable,
 *                                  dailyTotal, dailyClaimed, mainTotal, mainClaimed,
 *                                  tasks: [{ id, code, title, desc, category, categoryLabel,
 *                                            action, progress, target, exp, status, percent }],
 *                                  logs: [{ time, text, exp }] }
 *   POST /api/task/claim  { id }
 *                             -> { gain, levelUp, oldLevel, newLevel, newTitle, panel: {...同上} }
 *
 * 规则：经验只从任务领取获得；阶位由后端按累计经验推导，前端不自行计算等级。
 * 任务进度由真实操作推进（淬炼/突破/静修/吞噬/掠夺/登临），前端没有任何
 * 「直接完成任务」的入口。
 * 失败时 code=400 + 中文提示，页面直接弹 toast。
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

/** 拉取任务面板 */
export function fetchTask() {
  return request('/api/task')
}

/** 领取任务经验 */
export function claimTask(id) {
  return request('/api/task/claim', { method: 'POST', body: { id } })
}
