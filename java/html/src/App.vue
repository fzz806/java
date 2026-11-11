<script setup>
import { ref, watch, watchEffect, onMounted } from 'vue'
import RuneCanvas from './components/RuneCanvas.vue'
import DeclarationPanel from './components/DeclarationPanel.vue'
import LoginCard from './components/LoginCard.vue'
import RegisterCard from './components/RegisterCard.vue'
import HomePage from './components/HomePage.vue'
import RitualOverlay from './components/RitualOverlay.vue'
import { getSession, saveSession, clearSession } from './api/session.js'
import { fetchMe, logout, login } from './api/auth.js'

// 登录态（刷新页面后从 localStorage 恢复）
const session = ref(getSession())

// 当前页面：login = 登临神位，register = 铸印封神，home = 首页
// 已有登录态时直接落在首页
const view = ref(session.value ? 'home' : 'login')

// 从注册页带回登录页的真名，免去重复输入
const rememberedName = ref('')

// 当前形态：false = 恶鬼形态（黑红），true = 伪神形态（银星紫）
const isGodMode = ref(session.value && session.value.form === 'god')

const loginAt = ref(session.value ? session.value.loginAt : 0)

// 主题类挂到 body 上，让全局 CSS 变量整体切换
watchEffect(() => {
  document.body.classList.toggle('god-mode', isGodMode.value)
  // 首页需要纵向滚动，登录页需要整屏居中，用 body 类区分
  document.body.classList.toggle('home-view', view.value === 'home')
})

// 标题随页面切换
watch(
  view,
  (v) => {
    const titles = {
      home: '觉醒伪神 · 首页',
      login: '觉醒伪神 · 登临神位',
      register: '觉醒伪神 · 铸印封神'
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

// 登录成功后的 token / 注册时的神印，等仪式结束再使用
let pendingToken = ''
let pendingPassword = ''

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
function handleRegister({ username, password }) {
  ritualName.value = username
  pendingPassword = password || ''
  ritualFlow.value = 'register'
  ritualVisible.value = true
}

/* ---------------- 仪式结束：统一进入首页 ---------------- */
async function handleRitualDone() {
  // 先收起仪式遮罩，否则会一直盖在页面上
  ritualVisible.value = false

  const form = isGodMode.value ? 'god' : 'demon'

  if (ritualFlow.value === 'register') {
    // 注册成功：用刚烙下的神印自动登临一次，拿到真 token；失败则仅以本地会话进入首页
    let token = ''
    if (pendingPassword) {
      const r = await login({ username: ritualName.value, password: pendingPassword })
      if (r.code === 0 && r.data) token = r.data.token || ''
    }
    saveSession({ username: ritualName.value, token, form })
  } else {
    saveSession({ username: ritualName.value, token: pendingToken, form })
  }

  pendingToken = ''
  pendingPassword = ''
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
</script>

<template>
  <!-- 背景粒子（恶鬼红烬 / 伪神星屑） -->
  <RuneCanvas :god-mode="isGodMode" />

  <!-- 首页 -->
  <HomePage
    v-if="view === 'home' && session"
    :username="session.username"
    :god-mode="isGodMode"
    :login-at="loginAt"
    @toggle-form="handleToggleForm"
    @logout="handleLogout"
  />

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
    :visible="ritualVisible"
    :god-mode="isGodMode"
    :username="ritualName"
    @done="handleRitualDone"
  />
</template>

<style scoped>
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
</style>
