<script setup>
import { ref, watch, watchEffect, onMounted, onBeforeUnmount } from 'vue'
import RuneCanvas from './components/RuneCanvas.vue'
import DeclarationPanel from './components/DeclarationPanel.vue'
import LoginCard from './components/LoginCard.vue'
import RegisterCard from './components/RegisterCard.vue'
import HomePage from './components/HomePage.vue'
import RitualOverlay from './components/RitualOverlay.vue'
import { getSession, saveSession, clearSession } from './api/session.js'
import { fetchMe, logout } from './api/auth.js'

// 登录态（刷新页面后从 localStorage 恢复）
const session = ref(getSession())

// 当前页面：login = 登临神位，register = 铸印封神，home = 首页
// 已有登录态时直接落在首页
const view = ref(session.value ? 'home' : 'login')

// 从注册页带回登录页的真名与提示，免去重复输入
const rememberedName = ref('')
const registerNotice = ref('')

// 当前形态：false = 恶鬼形态（黑红），true = 伪神形态（银星紫）
const isGodMode = ref(session.value && session.value.form === 'god')

const loginAt = ref(session.value ? session.value.loginAt : 0)

// 主题类挂到 body 上，让全局 CSS 变量整体切换
watchEffect(() => {
  document.body.classList.toggle('god-mode', isGodMode.value)
  // 首页需要纵向滚动，登录页需要整屏居中，用 body 类区分
  document.body.classList.toggle('home-view', view.value === 'home')
  // 后台是 iframe 全屏视图，需取消登录页的 flex 居中
  document.body.classList.toggle('admin-view', view.value === 'admin')
})

// 标题随页面切换
watch(
  view,
  (v) => {
    const titles = {
      home: '觉醒伪神 · 首页',
      login: '觉醒伪神 · 登临神位',
      register: '觉醒伪神 · 铸印封神',
      admin: '觉醒伪神 · 管理后台'
    }
    document.title = titles[v] || '觉醒伪神'
    if (v === 'home') window.scrollTo(0, 0)
  },
  { immediate: true }
)

// 觉醒仪式状态（登录 / 注册共用）
const ritualVisible = ref(false)
const ritualName = ref('')
const ritualFlow = ref('login') // 'login' | 'register'

// 登录成功后的 token，等仪式结束再写入会话
let pendingToken = ''

function setForm(god) {
  isGodMode.value = god
  // 已登录时，切换形态同步写回本地
  if (session.value) {
    session.value = { ...session.value, form: god ? 'god' : 'demon' }
    saveSession(session.value)
  }
}

/* ---------------- 登录 ---------------- */
function handleLogin({ username, data }) {
  ritualName.value = username
  ritualFlow.value = 'login'
  pendingToken = (data && data.token) || ''
  ritualVisible.value = true
}

/* ---------------- 注册 ---------------- */
function handleRegister({ username }) {
  ritualName.value = username
  ritualFlow.value = 'register'
  ritualVisible.value = true
}

/* ---------------- 仪式结束：登录进首页 / 注册回登录页 ---------------- */
function handleRitualDone() {
  // 先收起仪式遮罩，否则会一直盖在页面上
  ritualVisible.value = false

  if (ritualFlow.value === 'register') {
    // 注册成功：不自动登临，带真名回到登录页，由本人亲手输入神印
    rememberedName.value = ritualName.value
    registerNotice.value = `神印「${ritualName.value}」已铸成，请登临神位`
    pendingToken = ''
    view.value = 'login'
    return
  }

  // 登录成功：写入会话并进入首页
  saveSession({
    username: ritualName.value,
    token: pendingToken,
    form: isGodMode.value ? 'god' : 'demon'
  })
  pendingToken = ''
  registerNotice.value = ''
  session.value = getSession()
  loginAt.value = session.value ? session.value.loginAt : Date.now()
  view.value = 'home'
}

function handleLoginError(message) {
  alert(`神印认证失败：${message}`)
}

function handleRegisterError(message) {
  alert(`封神失败：${message}`)
}

/* ---------------- 管理后台 ---------------- */
function openAdmin() {
  view.value = 'admin'
}

// 后台 iframe 内点「返回主站」后，通过 postMessage 通知这里切回首页
function onAdminMessage(e) {
  if (e.origin !== window.location.origin) return
  if (e.data && e.data.type === 'afg-admin-back') view.value = 'home'
}

/* ---------------- 首页：切换形态 / 退出 ---------------- */
function handleToggleForm() {
  setForm(!isGodMode.value)
}

async function handleLogout() {
  const token = session.value && session.value.token
  if (token) {
    // 服务端无状态，仅作形式上的登出请求；失败也照常清理本地
    logout(token).catch(() => {})
  }
  clearSession()
  session.value = null
  loginAt.value = 0
  ritualName.value = ''
  pendingToken = ''
  // 登录页只有恶鬼形态，退出时回到默认形态
  isGodMode.value = false
  view.value = 'login'
}

// 启动时校验本地 token 是否仍然有效（可能已过期或被服务端撤销）
onMounted(async () => {
  window.addEventListener('message', onAdminMessage)
  const current = session.value
  if (!current || !current.token) return
  const r = await fetchMe(current.token)
  if (r.code === 401) {
    // token 失效：静默清除，回到登录页
    clearSession()
    session.value = null
    loginAt.value = 0
    view.value = 'login'
  } else if (r.code === 0 && r.data && r.data.username) {
    // token 有效：用服务端最新真名覆盖本地（可能已被改名）
    session.value = { ...current, username: r.data.username }
    saveSession(session.value)
  }
  // code === -1（后端没启动）时保持本地登录态，不打断用户
})

onBeforeUnmount(() => window.removeEventListener('message', onAdminMessage))
</script>

<template>
  <!-- 背景粒子（恶鬼红烬 / 伪神星屑） -->
  <RuneCanvas v-if="view !== 'admin'" :god-mode="isGodMode" />

  <!-- 首页 -->
  <HomePage
    v-if="view === 'home' && session"
    :username="session.username"
    :god-mode="isGodMode"
    :login-at="loginAt"
    @toggle-form="handleToggleForm"
    @logout="handleLogout"
    @admin="openAdmin"
  />

  <!-- 管理后台：内嵌独立页面，样式与脚本完全隔离，互不污染 -->
  <div v-else-if="view === 'admin'" class="admin-shell">
    <iframe
      class="admin-frame"
      src="/admin-dashboard.html"
      title="觉醒伪神 · 管理后台"
    ></iframe>
  </div>

  <!-- 登录 / 注册 -->
  <template v-else>
    <!-- 顶部状态条 -->
    <div class="statusbar">
      <span class="talent">天赋评定 · SSS 级 ｜ 异能序列 · 伪神</span>
      <span class="guild">公会 · <b>恶鬼之牙</b> 全员恶人</span>
    </div>

    <div class="stage">
      <!-- 左侧：判定书 + 单一形态（恶鬼形态） -->
      <DeclarationPanel />

      <!-- 右侧：登录 / 注册卡片切换 -->
      <LoginCard
        v-if="view === 'login'"
        :initial-username="rememberedName"
        :notice="registerNotice"
        @submit="handleLogin"
        @error="handleLoginError"
        @go-register="view = 'register'"
      />
      <RegisterCard
        v-else-if="view === 'register'"
        @success="handleRegister"
        @error="handleRegisterError"
        @go-login="view = 'login'"
      />
    </div>
  </template>

  <!-- 觉醒仪式遮罩 -->
  <RitualOverlay
    v-if="view !== 'admin'"
    :visible="ritualVisible"
    :god-mode="isGodMode"
    :username="ritualName"
    :flow="ritualFlow"
    @done="handleRitualDone"
  />
</template>

<style scoped>
/* ---------- 管理后台：全屏 iframe ---------- */
.admin-shell {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: #f4f6fb;
}
.admin-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
}
/* 后台视图下取消登录页的整屏居中（body 是全局元素，需 :global） */
:global(body.admin-view) {
  display: block;
  overflow: hidden;
  align-items: initial;
  justify-content: initial;
}

.statusbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 28px;
  font-size: 12px;
  letter-spacing: .3em;
  color: #8a8291;
}
.statusbar .talent {
  color: var(--c1);
  border: 1px solid var(--c1);
  border-radius: 2px;
  padding: 4px 10px 4px 13px;
  text-shadow: 0 0 10px var(--c1-glow);
  animation: badgeGlow 2.6s ease-in-out infinite;
  transition: color .8s, border-color .8s;
}
@keyframes badgeGlow {
  0%, 100% { box-shadow: 0 0 6px var(--c1-soft); }
  50%      { box-shadow: 0 0 18px var(--c1-glow); }
}
.statusbar .guild { color: #b3a892; }
.statusbar .guild b { color: var(--c1); font-weight: normal; transition: color .8s; }

/* 窄屏：两块信息挤在同一行会互相贴住，只留更强的左侧评定徽记 */
@media (max-width: 560px) {
  .statusbar { padding: 12px 16px; font-size: 11px; letter-spacing: .16em; }
  .statusbar .guild { display: none; }
  .statusbar .talent { padding: 4px 8px 4px 10px; }
}
</style>
