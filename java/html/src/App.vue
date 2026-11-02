<script setup>
import { ref, watchEffect } from 'vue'
import RuneCanvas from './components/RuneCanvas.vue'
import DeclarationPanel from './components/DeclarationPanel.vue'
import LoginCard from './components/LoginCard.vue'
import RegisterCard from './components/RegisterCard.vue'
import RitualOverlay from './components/RitualOverlay.vue'

// 当前页面：login = 登临神位，register = 铸印封神
const view = ref('login')

// 从注册页带回登录页的真名，免去重复输入
const rememberedName = ref('')

// 当前形态：false = 恶鬼形态（黑红），true = 伪神形态（银星紫）
const isGodMode = ref(false)

// 主题类挂到 body 上，让全局 CSS 变量整体切换
watchEffect(() => {
  document.body.classList.toggle('god-mode', isGodMode.value)
})

// 觉醒仪式状态（登录 / 注册共用）
const ritualVisible = ref(false)
const ritualName = ref('')
const ritualFlow = ref('login') // 'login' | 'register'

function setForm(god) {
  isGodMode.value = god
}

function handleLogin({ username }) {
  ritualName.value = username
  ritualFlow.value = 'login'
  ritualVisible.value = true
}

// 觉醒仪式结束：按流程分流
// TODO: 在此接入后端登录接口（如 Spring Boot /api/login）
function handleRitualDone() {
  if (ritualFlow.value === 'register') {
    alert(`神印已铸，${ritualName.value}。伪神之名，神堂从此有档。`)
    rememberedName.value = ritualName.value
    view.value = 'login'
  } else {
    alert(`神印已烙，${ritualName.value}。\n（此处可接入你的登录接口）`)
  }
}

// 登录失败（神印认证未通过）
function handleLoginError(message) {
  alert(`神印认证失败：${message}`)
}

// 注册成功：走一遍觉醒仪式，然后回登录页
function handleRegister({ username }) {
  ritualName.value = username
  ritualFlow.value = 'register'
  ritualVisible.value = true
}

function handleRegisterError(message) {
  alert(`封神失败：${message}`)
}
</script>

<template>
  <!-- 顶部状态条 -->
  <div class="statusbar">
    <span class="talent">天赋评定 · SSS 级 ｜ 异能序列 · 伪神</span>
    <span class="guild">公会 · <b>恶鬼之牙</b> 全员恶人</span>
  </div>

  <!-- 背景粒子（恶鬼红烬 / 伪神星屑） -->
  <RuneCanvas :god-mode="isGodMode" />

  <div class="stage">
    <!-- 左侧：判定书 + 双形态 -->
    <DeclarationPanel :god-mode="isGodMode" @set-form="setForm" />

    <!-- 右侧：登录 / 注册卡片切换 -->
    <LoginCard
      v-if="view === 'login'"
      :initial-username="rememberedName"
      @submit="handleLogin"
      @error="handleLoginError"
      @go-register="view = 'register'"
    />
    <RegisterCard
      v-else
      @success="handleRegister"
      @error="handleRegisterError"
      @go-login="view = 'login'"
    />
  </div>

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
