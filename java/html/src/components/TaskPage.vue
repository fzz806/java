<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { fetchTask, claimTask } from '../api/task.js'

const emit = defineEmits(['sync', 'goto'])

/** 有对应修行页面的动作，允许「去完成」直接跳转 */
const GO_ACTIONS = ['rest', 'seal', 'essence', 'skill', 'realm', 'upgrade']

function canGo(t) {
  return t.status === 'doing' && GO_ACTIONS.includes(t.action)
}

function onTaskClick(t) {
  if (t.status === 'claimable') {
    claim(t)
    return
  }
  if (canGo(t)) emit('goto', 'power')
}

function btnText(t) {
  if (busyId.value === t.id) return '结 算 中'
  if (t.status === 'claimed') return '已 领 取'
  if (t.status === 'claimable') {
    return data.value && data.value.limitReached ? '额 度 已 满' : '领 取 经 验'
  }
  return canGo(t) ? '去 完 成' : '进 行 中'
}

const data = ref(null)
const loading = ref(true)
const errorText = ref('')
const busyId = ref(0)

const toast = ref('')
let toastTimer = 0
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

/* 经验数字滚动 */
const displayExp = ref(0)
let rafId = 0
function animateExp(from, to) {
  cancelAnimationFrame(rafId)
  const start = performance.now()
  const dur = 640
  const step = (now) => {
    const p = Math.min(1, (now - start) / dur)
    const e = 1 - Math.pow(1 - p, 3)
    displayExp.value = Math.round(from + (to - from) * e)
    if (p < 1) rafId = requestAnimationFrame(step)
  }
  rafId = requestAnimationFrame(step)
}

/* 领取反馈：经验气泡 */
const expTip = ref(0)
let tipTimer = 0
function showExpTip(v) {
  expTip.value = v
  clearTimeout(tipTimer)
  tipTimer = setTimeout(() => (expTip.value = 0), 2200)
}

/* 阶位晋升动画 */
const ascended = ref(null) // { level, title }
let ascendTimer = 0
function playAscend(level, title) {
  ascended.value = { level, title }
  clearTimeout(ascendTimer)
  ascendTimer = setTimeout(() => (ascended.value = null), 2200)
}

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  clearTimeout(toastTimer)
  clearTimeout(tipTimer)
  clearTimeout(ascendTimer)
})

load()
async function load() {
  const r = await fetchTask()
  if (r.code === 0 && r.data) {
    data.value = r.data
    displayExp.value = r.data.exp
    // 通知导航同步阶位与可领取数量
    emit('sync', r.data)
  } else {
    errorText.value = r.message || '任务碑无法读取'
  }
  loading.value = false
}

async function claim(t) {
  if (busyId.value || t.status !== 'claimable') return
  // 额度是独立一层限制：任务达标但今日额度用尽，前端先挡一次，后端也会拒
  if (data.value && data.value.limitReached) {
    showToast(`今日已领 ${data.value.claimedToday} 个任务，明日再来`)
    return
  }
  busyId.value = t.id
  const expBefore = data.value ? data.value.exp : 0
  const r = await claimTask(t.id)
  busyId.value = 0

  if (r.code === 0 && r.data) {
    const panel = r.data.panel
    data.value = panel
    animateExp(expBefore, panel.exp)
    showExpTip(r.data.gain)
    emit('sync', panel)
    if (r.data.levelUp) playAscend(r.data.newLevel, r.data.newTitle)
  } else if (r.code === 401) {
    errorText.value = r.message
  } else {
    showToast(r.message || '领取失败')
  }
}

/* ---------------- 派生数据 ---------------- */
const dailyTasks = computed(() =>
  data.value ? data.value.tasks.filter((t) => t.category !== 'main') : []
)
const mainTasks = computed(() =>
  data.value ? data.value.tasks.filter((t) => t.category === 'main') : []
)

const dailyExp = computed(() => dailyTasks.value.reduce((s, t) => s + t.exp, 0))
const mainExp = computed(() => mainTasks.value.reduce((s, t) => s + t.exp, 0))

const dailyPercent = computed(() => {
  if (!data.value || !data.value.dailyTotal) return 0
  return Math.round((data.value.dailyClaimed / data.value.dailyTotal) * 100)
})

const expText = computed(() => displayExp.value.toLocaleString('en-US'))

const nextHint = computed(() => {
  if (!data.value) return ''
  if (data.value.full) return '神格圆满 · 再无阶位可登'
  const rest = data.value.expMax - data.value.exp
  return `距「${data.value.nextTitle}」还需 ${rest} 点经验`
})
</script>

<template>
  <div class="page">
    <header class="page-head">
      <h2>神 格 任 务</h2>
      <span class="page-en">QUEST OF ASCENSION · 日拱一卒，终成伪神</span>
      <span v-if="data" class="page-count">今日日课 {{ data.dailyClaimed }} / {{ data.dailyTotal }}</span>
    </header>

    <div v-if="loading" class="hint">任务碑校验中…</div>
    <div v-else-if="errorText" class="hint err">{{ errorText }}</div>

    <template v-else-if="data">
      <!-- ============ 阶位面板 ============ -->
      <section class="board">
        <span class="verdict">神堂判定：神弃之人 · 以功业换取神格</span>

        <div class="lv-main">
          <span class="lv-label">神格阶位</span>
          <div class="lv-row">
            <span class="lv-num">Lv.{{ data.level }}</span>
            <span class="lv-title">{{ data.title }}</span>
          </div>
          <span class="lv-next">{{ nextHint }}</span>
        </div>

        <div class="exp">
          <div class="exp-head">
            <span class="e-name">神格经验</span>
            <span class="e-val">
              <template v-if="data.full">{{ data.totalExp }} 点 · 神格圆满</template>
              <template v-else>
                {{ expText }} / {{ data.expMax }}
                <transition name="pop">
                  <b v-if="expTip" class="e-gain">+{{ expTip }}</b>
                </transition>
              </template>
            </span>
          </div>
          <div class="track"><div class="fill" :style="{ width: data.percent + '%' }"></div></div>
          <span class="exp-total">累计所得 {{ data.totalExp }} 点 · 阶位上限 Lv.{{ data.levelMax }}</span>
          <span class="quota" :class="{ done: data.limitReached }">
            今日领取额度
            <b>{{ data.claimedToday }} / {{ data.claimLimit }}</b>
            <em v-if="data.limitReached">已用尽 · 明日零点恢复</em>
            <em v-else>还可领 {{ data.remainingClaim }} 个任务</em>
          </span>
        </div>

        <div class="stats">
          <div class="stat">
            <b :class="{ hot: data.claimable > 0 && !data.limitReached }">{{ data.claimable }}</b>
            <i>可领取</i>
          </div>
          <div class="stat">
            <b>{{ data.dailyClaimed }} / {{ data.dailyTotal }}</b>
            <i>今日日课</i>
          </div>
          <div class="stat">
            <b>{{ data.mainClaimed }} / {{ data.mainTotal }}</b>
            <i>主线功业</i>
          </div>
          <div class="stat wide">
            <div class="s-track"><div class="s-fill" :style="{ width: dailyPercent + '%' }"></div></div>
            <i>日课完成度 {{ dailyPercent }}%</i>
          </div>
        </div>
      </section>

      <!-- ============ 日课 ============ -->
      <section class="group">
        <h3>
          日 课
          <span class="g-sub">每日重置 · 全清可得 {{ dailyExp }} 点经验</span>
        </h3>
        <div class="grid">
          <article v-for="t in dailyTasks" :key="t.id" class="task" :class="t.status">
            <div class="t-head">
              <span class="t-title">{{ t.title }}</span>
              <span class="t-exp">+{{ t.exp }}</span>
            </div>
            <p class="t-desc">{{ t.desc }}</p>
            <div class="t-prog">
              <div class="t-track"><div class="t-fill" :style="{ width: t.percent + '%' }"></div></div>
              <span class="t-num">{{ t.progress }} / {{ t.target }}</span>
            </div>
            <button
              class="t-btn"
              :class="{ go: canGo(t) }"
              :disabled="busyId === t.id || (t.status !== 'claimable' && !canGo(t)) || (t.status === 'claimable' && data.limitReached)"
              @click="onTaskClick(t)"
            >
              {{ btnText(t) }}
            </button>
          </article>
        </div>
        <p v-if="!dailyTasks.length" class="empty">今日无日课</p>
      </section>

      <!-- ============ 主线 ============ -->
      <section class="group">
        <h3>
          主 线
          <span class="g-sub">一次成就 · 不随日重置 · 共 {{ mainExp }} 点经验</span>
        </h3>
        <div class="grid">
          <article v-for="t in mainTasks" :key="t.id" class="task main-task" :class="t.status">
            <div class="t-head">
              <span class="t-title">{{ t.title }}</span>
              <span class="t-exp">+{{ t.exp }}</span>
            </div>
            <p class="t-desc">{{ t.desc }}</p>
            <div class="t-prog">
              <div class="t-track"><div class="t-fill" :style="{ width: t.percent + '%' }"></div></div>
              <span class="t-num">{{ t.progress }} / {{ t.target }}</span>
            </div>
            <button
              class="t-btn"
              :class="{ go: canGo(t) }"
              :disabled="busyId === t.id || (t.status !== 'claimable' && !canGo(t)) || (t.status === 'claimable' && data.limitReached)"
              @click="onTaskClick(t)"
            >
              {{ btnText(t) }}
            </button>
          </article>
        </div>
        <p v-if="!mainTasks.length" class="empty">主线尚未开启</p>
      </section>

      <!-- ============ 最近所得 ============ -->
      <section class="logs">
        <h3>最 近 所 得</h3>
        <ul>
          <li v-for="(l, i) in data.logs" :key="i">
            <span class="l-time">{{ l.time }}</span>
            <span class="l-text">{{ l.text }}</span>
            <span class="l-exp">+{{ l.exp }} 经验</span>
          </li>
        </ul>
        <p v-if="!data.logs.length" class="logs-empty">尚无经验 · 从第一份日课开始</p>
      </section>

      <p class="tip-line">任务进度由实际修行推进：淬炼红印、突破境界、静修、吞噬本源、掠夺神技，皆记功业。</p>
    </template>

    <!-- ============ 阶位晋升动画 ============ -->
    <transition name="ascend">
      <div v-if="ascended" class="ascend">
        <span class="a-flash" aria-hidden="true"></span>
        <span class="a-ring" aria-hidden="true"></span>
        <span class="a-ring delay" aria-hidden="true"></span>
        <svg class="a-crown" viewBox="0 0 100 100" fill="none" aria-hidden="true">
          <path d="M18 68 L24 32 L40 48 L50 22 L60 48 L76 32 L82 68 Z" stroke="currentColor" stroke-width="1.6" />
          <path d="M18 68 H82" stroke="currentColor" stroke-width="1.2" />
          <circle cx="50" cy="22" r="3.4" stroke="currentColor" stroke-width="1.2" />
          <circle cx="24" cy="32" r="2.4" stroke="currentColor" stroke-width="1.2" />
          <circle cx="76" cy="32" r="2.4" stroke="currentColor" stroke-width="1.2" />
        </svg>
        <p class="a-text">阶 位 晋 升</p>
        <p class="a-level">Lv.{{ ascended.level }} · {{ ascended.title }}</p>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 34px 28px 60px;
  position: relative;
}
.page-head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
  padding-bottom: 16px;
  margin-bottom: 22px;
  border-bottom: 1px solid rgba(255, 255, 255, .07);
}
.page-head h2 {
  font-size: 17px;
  font-weight: 400;
  letter-spacing: .3em;
  color: #ded5e4;
  position: relative;
  padding-left: 13px;
}
.page-head h2::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 2px;
  height: 15px;
  margin-top: -7px;
  background: var(--c1);
  box-shadow: 0 0 10px var(--c1-glow);
}
.page-en { font-size: 10px; letter-spacing: .24em; color: #57505e; }
.page-count { margin-left: auto; font-size: 11px; letter-spacing: .2em; color: #9a8a55; }

.hint { padding: 60px 0; text-align: center; font-size: 12px; letter-spacing: .2em; color: #6d6478; }
.hint.err { color: #ff6b6b; }

/* ---------------- 阶位面板 ---------------- */
.board {
  padding: 26px 26px 22px;
  border: 1px solid var(--c1-dim);
  border-radius: 4px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .9), rgba(7, 4, 5, .95));
  box-shadow: 0 24px 70px rgba(0, 0, 0, .6), inset 0 0 50px var(--c1-soft);
  position: relative;
  overflow: hidden;
}
:global(body.god-mode) .board {
  background: linear-gradient(160deg, rgba(14, 17, 30, .9), rgba(4, 5, 10, .95));
}
.board::after {
  content: "";
  position: absolute;
  inset: -40% -20% auto auto;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, var(--c1-soft), transparent 70%);
  pointer-events: none;
}
.verdict {
  display: inline-block;
  font-size: 11px;
  letter-spacing: .3em;
  color: #9a917f;
  border: 1px dashed #55503f;
  padding: 5px 10px 5px 13px;
  margin-bottom: 20px;
  transform: rotate(-1.2deg);
}

.lv-label { font-size: 11px; letter-spacing: .35em; color: #7d7466; }
.lv-row { display: flex; align-items: baseline; gap: 12px; margin-top: 6px; flex-wrap: wrap; }
.lv-num {
  font-size: clamp(36px, 5vw, 54px);
  font-weight: 900;
  line-height: 1.05;
  color: #fff4e6;
  text-shadow: 0 0 26px var(--c1-glow);
  font-variant-numeric: tabular-nums;
}
.lv-title {
  font-size: 20px;
  letter-spacing: .3em;
  color: var(--c1);
  text-shadow: 0 0 18px var(--c1-glow);
}
.lv-next { display: block; margin-top: 8px; font-size: 11px; letter-spacing: .22em; color: #8a8291; }

.exp { margin-top: 24px; }
.exp-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 11.5px;
  letter-spacing: .16em;
  margin-bottom: 9px;
}
.e-name { color: #7d7466; letter-spacing: .3em; }
.e-val { color: #9a8a55; font-variant-numeric: tabular-nums; position: relative; }
.e-gain {
  margin-left: 8px;
  font-weight: 700;
  color: var(--c1);
  text-shadow: 0 0 16px var(--c1-glow);
}
.track {
  height: 7px;
  border-radius: 1px;
  background: rgba(255, 255, 255, .06);
  overflow: hidden;
}
.fill {
  height: 100%;
  background: linear-gradient(90deg, var(--c1-dim), var(--c1));
  box-shadow: 0 0 14px var(--c1-glow);
  transition: width .7s cubic-bezier(.22, .8, .3, 1);
}
.exp-total { display: block; margin-top: 9px; font-size: 10.5px; letter-spacing: .18em; color: #57505e; }

/* 每日领取额度 */
.quota {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 11px;
  padding: 7px 11px;
  font-size: 10.5px;
  letter-spacing: .16em;
  color: #6c6473;
  border: 1px solid rgba(255, 255, 255, .07);
  border-radius: 6px;
  background: rgba(255, 255, 255, .02);
}
.quota b { color: var(--c1); font-weight: 600; letter-spacing: .06em; }
.quota em { font-style: normal; margin-left: auto; color: #57505e; }
.quota.done { border-color: var(--c1-dim); background: var(--c1-soft); }
.quota.done em { color: var(--c1); }

.stats {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, .07);
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.stat { flex: 1 1 90px; min-width: 84px; display: flex; flex-direction: column; gap: 4px; }
.stat b { font-size: 19px; font-weight: 400; color: #e8e0d0; font-variant-numeric: tabular-nums; }
.stat b.hot { color: var(--c1); text-shadow: 0 0 14px var(--c1-glow); }
.stat i { font-style: normal; font-size: 10.5px; letter-spacing: .18em; color: #6d6660; }
.stat.wide { flex: 1 1 100%; }
.s-track { height: 4px; background: rgba(255, 255, 255, .06); overflow: hidden; margin-bottom: 7px; }
.s-fill {
  height: 100%;
  background: linear-gradient(90deg, #2e6ea8, #7fc4ff);
  box-shadow: 0 0 12px rgba(127, 196, 255, .4);
  transition: width .6s ease;
}

/* ---------------- 任务分组 ---------------- */
.group { margin-top: 24px; }
.group h3 {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-size: 11px;
  font-weight: 400;
  letter-spacing: .35em;
  color: #9a8a55;
  margin-bottom: 14px;
}
.g-sub { font-size: 10px; letter-spacing: .16em; color: #57505e; }

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(268px, 1fr));
  gap: 14px;
}
.task {
  display: flex;
  flex-direction: column;
  padding: 16px 17px 15px;
  border: 1px solid rgba(255, 255, 255, .09);
  border-radius: 3px;
  background: rgba(0, 0, 0, .34);
  transition: border-color .3s, box-shadow .3s, transform .3s;
}
.task:hover { border-color: var(--c1-dim); box-shadow: 0 0 20px var(--c1-soft); transform: translateY(-2px); }
.task.claimable { border-color: var(--c1-dim); box-shadow: 0 0 18px var(--c1-soft); }
.task.claimed { opacity: .55; }
.task.claimed:hover { transform: none; box-shadow: none; }
.main-task { background: rgba(20, 14, 8, .42); }

.t-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.t-title { font-size: 14px; letter-spacing: .2em; color: #e8e0d0; }
.t-exp {
  font-size: 12px;
  color: var(--c1);
  letter-spacing: .08em;
  text-shadow: 0 0 10px var(--c1-glow);
}
.t-desc {
  margin: 9px 0 12px;
  font-size: 11.5px;
  line-height: 1.8;
  letter-spacing: .06em;
  color: #6d6660;
  min-height: 42px;
}
.t-prog { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.t-track { flex: 1; height: 4px; background: rgba(255, 255, 255, .06); overflow: hidden; }
.t-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--c1-dim), var(--c1));
  transition: width .5s ease;
}
.t-num { font-size: 10.5px; letter-spacing: .1em; color: #8a8291; font-variant-numeric: tabular-nums; }

.t-btn {
  margin-top: auto;
  width: 100%;
  padding: 9px;
  background: linear-gradient(135deg, #2e1113, #140608);
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: var(--c1);
  font-family: inherit;
  font-size: 12.5px;
  letter-spacing: .34em;
  text-indent: .34em;
  cursor: pointer;
  transition: all .3s;
}
:global(body.god-mode) .t-btn { background: linear-gradient(135deg, #131c33, #060a14); }
.t-btn:hover:not(:disabled) {
  background: var(--c1);
  border-color: var(--c1);
  color: #0a0508;
  box-shadow: 0 0 26px var(--c1-glow);
}
.t-btn:active:not(:disabled) { transform: scale(.985); }
.t-btn:disabled { cursor: not-allowed; color: #5c5560; border-color: #332b34; }
/* 「去完成」：可点但不是主行动，弱化于领取 */
.t-btn.go {
  background: rgba(0, 0, 0, .3);
  border-color: #454049;
  color: #b9b1a3;
}
.t-btn.go:hover:not(:disabled) {
  background: rgba(255, 255, 255, .06);
  border-color: var(--c1-dim);
  color: #e8e0d0;
  box-shadow: 0 0 18px var(--c1-soft);
}

.empty { font-size: 11px; letter-spacing: .2em; color: #57505e; }
.tip-line { margin-top: 22px; font-size: 10.5px; letter-spacing: .14em; color: #4d4753; text-align: center; }

/* ---------------- 经验流水 ---------------- */
.logs {
  margin-top: 22px;
  padding: 20px 22px 22px;
  border: 1px solid rgba(255, 255, 255, .07);
  border-radius: 3px;
  background: rgba(0, 0, 0, .26);
}
.logs h3 {
  font-size: 11px;
  font-weight: 400;
  letter-spacing: .35em;
  color: #9a8a55;
  margin-bottom: 14px;
}
.logs ul { display: flex; flex-direction: column; gap: 2px; }
.logs li {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 7px 0;
  font-size: 11.5px;
  letter-spacing: .1em;
  color: #8a8291;
  border-bottom: 1px dashed rgba(255, 255, 255, .05);
}
.logs li:last-child { border-bottom: none; }
.l-time { flex: 0 0 62px; color: #57505e; font-size: 10.5px; font-variant-numeric: tabular-nums; }
.l-text { flex: 1; }
.l-exp { color: var(--c1); text-shadow: 0 0 10px var(--c1-glow); }
.logs-empty { font-size: 11px; letter-spacing: .2em; color: #57505e; }

/* 动效 */
.pop-enter-active { transition: opacity .25s, transform .35s cubic-bezier(.2, .9, .3, 1.4); }
.pop-leave-active { transition: opacity .4s, transform .4s; }
.pop-enter-from { opacity: 0; transform: translateY(6px) scale(.8); }
.pop-leave-to { opacity: 0; transform: translateY(-10px); }

.fade-enter-active, .fade-leave-active { transition: opacity .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ---------------- 阶位晋升 ---------------- */
.ascend {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  overflow: hidden;
}
.a-flash {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 50%, var(--c1-soft) 0%, transparent 62%);
  animation: ascendFlash 2.2s ease-out forwards;
}
.a-ring {
  position: absolute;
  width: 120px;
  height: 120px;
  border: 1px solid var(--c1);
  border-radius: 50%;
  box-shadow: inset 0 0 60px var(--c1-glow), 0 0 40px var(--c1-glow);
  animation: ascendRing 2s cubic-bezier(.2, .7, .3, 1) forwards;
}
.a-ring.delay { animation-delay: .3s; opacity: 0; }
.a-crown {
  position: relative;
  width: 100px;
  height: 100px;
  color: var(--c1);
  filter: drop-shadow(0 0 22px var(--c1-glow));
  animation: ascendCrown 2.2s cubic-bezier(.2, .9, .3, 1.15) forwards;
}
.a-text {
  position: relative;
  margin-top: 22px;
  font-size: 12px;
  letter-spacing: .8em;
  text-indent: .8em;
  color: #b9b1a3;
  animation: ascendText 2.2s ease forwards;
}
.a-level {
  position: relative;
  margin-top: 10px;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: .22em;
  text-indent: .22em;
  color: #fff4e6;
  text-shadow: 0 0 30px var(--c1-glow);
  animation: ascendText 2.2s ease forwards;
}
@keyframes ascendFlash {
  0%   { opacity: 0; }
  14%  { opacity: 1; }
  100% { opacity: 0; }
}
@keyframes ascendRing {
  0%   { transform: scale(.3); opacity: .95; }
  100% { transform: scale(9); opacity: 0; }
}
@keyframes ascendCrown {
  0%   { transform: scale(.4) rotate(-18deg); opacity: 0; }
  26%  { transform: scale(1.08) rotate(0deg); opacity: 1; }
  66%  { transform: scale(1.14) rotate(4deg); opacity: 1; }
  100% { transform: scale(1.4) rotate(12deg); opacity: 0; }
}
@keyframes ascendText {
  0%   { opacity: 0; transform: translateY(12px); }
  22%  { opacity: 1; transform: none; }
  72%  { opacity: 1; transform: none; }
  100% { opacity: 0; transform: translateY(-8px); }
}
.ascend-leave-active { transition: opacity .3s; }
.ascend-leave-to { opacity: 0; }

.toast {
  position: fixed;
  left: 50%;
  bottom: 44px;
  transform: translateX(-50%);
  padding: 10px 20px;
  font-size: 12px;
  letter-spacing: .18em;
  color: #e8e0d0;
  background: rgba(14, 8, 10, .94);
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, .6), 0 0 20px var(--c1-soft);
  z-index: 20;
}

@media (max-width: 620px) {
  .page { padding: 24px 16px 48px; }
  .board { padding: 20px 18px 18px; }
  .grid { grid-template-columns: 1fr; }
}
</style>
