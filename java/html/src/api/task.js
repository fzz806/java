/**
 * 神格任务 API 层
 *
 * 后端接口（false-god-backend，:8080，均需 Bearer token）：
 *   GET  /api/task        任务面板
 *                             -> { level, title, nextTitle, exp, expMax, totalExp, percent,
 *                                  levelMax, full, realm, realmIndex, realmExp,
 *                                  claimable,
 *                                  dailyTotal, dailyClaimed, mainTotal, mainClaimed,
 *                                  claimLimit, claimedToday, remainingClaim, limitReached,
 *                                  tasks: [{ id, code, title, desc, category, categoryLabel,
 *                                            action, progress, target, exp, status, percent }],
 *                                  logs: [{ time, text, exp }] }
 *   POST /api/task/claim  { id }
 *                             -> { gain, levelUp, realmUp, oldLevel, newLevel, newTitle,
 *                                  panel: {...同上} }
 *
 * 规则：经验只从任务领取获得；阶位由后端按累计经验推导，前端不自行计算等级。
 *
 * 阶位名与大境界同名（Lv.4 = 四印），阶梯由后端 RealmLadder 统一维护：
 * 经验跨阶会把大境界一起推上去，修行页突破也会把经验补到该阶起点，
 * 所以 task 面板的 level/title 与 /api/power 的 realm 永远指向同一个境界。
 * realm 是完整境界（如「四印 · 圆满」），用于在阶位旁标注当前修行进度。
 * 任务进度由真实操作推进（淬炼/突破/静修/吞噬/掠夺/登临），前端没有任何
 * 「直接完成任务」的入口。
 *
 * 每日额度：日课池固定 10 条，claimLimit = 10，每天最多领 10 条日课的经验，
 * 做满即 limitReached=true，次日零点归零。主线是一次性功业，不占额度、
 * 也不受 limitReached 限制——所以前端置灰时要按 category 区分，不能一刀切。
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
