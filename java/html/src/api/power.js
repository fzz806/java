/**
 * 战力提升 API 层
 *
 * 后端接口（false-god-backend，:8080，均需 Bearer token）：
 * GET  /api/power          战力面板
 *                                 -> { power, realm, realmName, realmMark,
 *                                      stage, stageName, stageMax, nextRealm,
 *                                      realmLevel, realmMax,
 *                                      spirit, spiritMax, seal, sealMax, skills, essence,
 *                                      actions: [{key,label,desc,gain,cost,effect,available}],
 *                                      logs: [{time,text,gain}] }
 *   POST /api/power/upgrade  { type: realm | seal | skill | essence | rest }
 *                                 -> { panel: {...同上传结构}, gain: number, looted?: {name,level,element,desc} }
 *
 * 境界模型：大境界（一印~八印【天锁八印】、半神、真神）× 小境界分档递进
 * （印级为初印/中印/圆满/巅峰 4 档；半神为初阶/中阶/巅峰 3 档；真神不细分）。
 * 小境界臻至巅峰才能冲击大境界壁垒；seal 为「小境界精进」的历史动作键。
 * 综合战力由后端按各维度实时推导，不落库，保证面板/增量/榜单三处一致。
 * 失败时返回 code=400 与中文提示，页面直接弹 toast。
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
