/**
 * 登录态会话管理
 *
 * 后端现已返回真 JWT（HS384 签名，24h 有效），由 AuthInterceptor 校验，
 * 因此这里只做「本地会话」：把登录结果存进 localStorage，
 * 刷新页面后仍保持已登录，并提供登出入口。
 *
 * token 过期或失效时，后端返回 401，各 api/*.js 会归一成 { code: 401 }，
 * 由页面提示重新登临。
 */

const KEY = 'false-god-session'

/**
 * 保存登录态
 * @param {{ username: string, token?: string, form?: 'demon'|'god' }} session
 */
export function saveSession(session) {
  try {
    localStorage.setItem(
      KEY,
      JSON.stringify({
        username: session.username || '',
        token: session.token || '',
        form: session.form || 'demon',
        loginAt: Date.now()
      })
    )
  } catch (e) {
    // localStorage 被禁用（隐私模式等）时静默降级，不影响登录流程
  }
}

/**
 * 读取登录态，未登录返回 null
 * @returns {{username: string, token: string, form: string, loginAt: number} | null}
 */
export function getSession() {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return null
    const s = JSON.parse(raw)
    return s && s.username ? s : null
  } catch (e) {
    return null
  }
}

/** 是否已登录 */
export function isLoggedIn() {
  return !!getSession()
}

/** 退出登录 */
export function clearSession() {
  try {
    localStorage.removeItem(KEY)
  } catch (e) {
    /* ignore */
  }
}

/** 登录时长的人类可读描述 */
export function loginDurationText(loginAt) {
  if (!loginAt) return ''
  const min = Math.floor((Date.now() - loginAt) / 60000)
  if (min < 1) return '刚刚登临'
  if (min < 60) return `已登临 ${min} 分钟`
  const h = Math.floor(min / 60)
  if (h < 24) return `已登临 ${h} 小时`
  return `已登临 ${Math.floor(h / 24)} 天`
}
