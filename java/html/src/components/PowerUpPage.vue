<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { fetchPower, upgradePower } from '../api/power.js'

const data = ref(null)
const loading = ref(true)
const errorText = ref('')
const busy = ref(false)

/* 战力数字滚动展示 */
const displayPower = ref(0)
let rafId = 0
function animatePower(from, to) {
  cancelAnimationFrame(rafId)
  const start = performance.now()
  const dur = 720
  const step = (now) => {
    const p = Math.min(1, (now - start) / dur)
    // easeOutCubic
    const e = 1 - Math.pow(1 - p, 3)
    displayPower.value = Math.round(from + (to - from) * e)
    if (p < 1) rafId = requestAnimationFrame(step)
  }
  rafId = requestAnimationFrame(step)
}
onBeforeUnmount(() => cancelAnimationFrame(rafId))

/* 提升反馈气泡 */
const gainTip = ref(0)
let tipTimer = 0
function showGain(v) {
  gainTip.value = v
  clearTimeout(tipTimer)
  tipTimer = setTimeout(() => (gainTip.value = 0), 2200)
}

/* 突破动画 */
const ascension = ref('')
let ascendTimer = 0
function playAscend(realmName) {
  ascension.value = realmName
  clearTimeout(ascendTimer)
  ascendTimer = setTimeout(() => (ascension.value = ''), 1800)
}

/* 掠夺神技揭示 */
const loot = ref(null)
let lootTimer = 0
function playLoot(skill) {
  loot.value = skill
  clearTimeout(lootTimer)
  lootTimer = setTimeout(() => (loot.value = null), 2400)
}

const toast = ref('')
let toastTimer = 0
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}
onBeforeUnmount(() => {
  clearTimeout(tipTimer)
  clearTimeout(toastTimer)
  clearTimeout(ascendTimer)
  clearTimeout(lootTimer)
})

load()
async function load() {
  const r = await fetchPower()
  if (r.code === 0 && r.data) {
    data.value = r.data
    displayPower.value = r.data.power
  } else {
    errorText.value = r.message || '战力碑无法读取'
  }
  loading.value = false
}

async function run(action) {
  if (busy.value || !action.available) return
  busy.value = true
  const before = data.value ? data.value.power : 0
  const r = await upgradePower(action.key)
  busy.value = false

  if (r.code === 0 && r.data) {
    data.value = r.data.panel
    animatePower(before, r.data.panel.power)
    if (r.data.gain > 0) showGain(r.data.gain)
    // 境界突破：放一段全屏印纹扩散
    if (action.key === 'realm') playAscend(r.data.panel.realm)
    // 掠夺神技：揭示抽到的那一式
    if (r.data.looted) playLoot(r.data.looted)
  } else if (r.code === 401) {
    errorText.value = r.message
  } else {
    showToast(r.message || '提升失败')
  }
}

const powerPercent = computed(() => {
  if (!data.value) return 0
  const { realmLevel, realmMax } = data.value
  return Math.round((realmLevel / realmMax) * 100)
})

const spiritPercent = computed(() => {
  if (!data.value) return 0
  return Math.round((data.value.spirit / data.value.spiritMax) * 100)
})

const powerText = computed(() =>
  displayPower.value.toLocaleString('en-US').replace(/,/g, ' ')
)
</script>

<template>
  <div class="page">
    <header class="page-head">
      <h2>战 力 提 升</h2>
      <span class="page-en">POWER ASCENSION · 神弃者的登阶之路</span>
      <span v-if="data" class="page-count">当前境界 · {{ data.realm }}</span>
    </header>

    <div v-if="loading" class="hint">战力碑校验中…</div>
    <div v-else-if="errorText" class="hint err">{{ errorText }}</div>

    <template v-else-if="data">
      <div class="layout">
        <!-- ============ 左：战力面板 ============ -->
        <section class="board">
          <span class="verdict">神堂判定：神弃之人 · 终生无法镶嵌神印</span>

          <div class="power-main">
            <span class="p-label">综合战力</span>
            <div class="p-row">
              <span class="p-value">{{ powerText }}</span>
              <span class="p-unit">点</span>
              <transition name="pop">
                <span v-if="gainTip" class="p-gain">+{{ gainTip }}</span>
              </transition>
            </div>
            <span class="p-seal">凭 · 野生红印 {{ data.seal }} 重</span>
          </div>

          <!-- 境界阶梯 -->
          <div class="realm">
            <div class="realm-head">
              <span class="realm-now">{{ data.realm }}</span>
              <span class="realm-arrow">→</span>
              <span class="realm-next">{{ data.nextRealm }}</span>
              <span class="realm-num">{{ data.realmLevel }} / {{ data.realmMax }}</span>
            </div>
            <div class="track"><div class="fill" :style="{ width: powerPercent + '%' }"></div></div>
          </div>

          <!-- 资源 -->
          <div class="res">
            <div class="res-item spirit">
              <div class="res-top">
                <span class="r-name">灵能</span>
                <span class="r-val">{{ data.spirit }} / {{ data.spiritMax }}</span>
              </div>
              <div class="r-track"><div class="r-fill" :style="{ width: spiritPercent + '%' }"></div></div>
            </div>
            <div class="res-item"><span class="r-name">红印</span><b>{{ data.seal }}</b><i>/ {{ data.sealMax }} 重</i></div>
            <div class="res-item"><span class="r-name">神技</span><b>{{ data.skills }}</b><i>式</i></div>
            <div class="res-item"><span class="r-name">本源</span><b>{{ data.essence }}</b><i>枚</i></div>
          </div>
        </section>

        <!-- ============ 右：提升途径 ============ -->
        <section class="actions">
          <div
            v-for="a in data.actions"
            :key="a.key"
            class="act"
            :class="{ off: !a.available || busy, rest: a.key === 'rest' }"
          >
            <div class="act-head">
              <span class="act-label">{{ a.label }}</span>
              <span v-if="a.gain" class="act-gain">+{{ a.gain }}</span>
            </div>
            <p class="act-desc">{{ a.desc }}</p>
            <div class="act-foot">
              <span class="act-cost">{{ a.cost ? `灵能 ${a.cost}` : '无消耗' }}</span>
              <span class="act-effect">{{ a.effect }}</span>
            </div>
            <button class="act-btn" :disabled="!a.available || busy" @click="run(a)">
              {{ a.key === 'rest' ? '静 修' : '提 升' }}
            </button>
          </div>
        </section>
      </div>

      <!-- ============ 提升记录 ============ -->
      <section class="logs">
        <h3>提 升 记 录</h3>
        <ul>
          <li v-for="(l, i) in data.logs" :key="i">
            <span class="l-time">{{ l.time }}</span>
            <span class="l-text">{{ l.text }}</span>
            <span v-if="l.gain" class="l-gain">+{{ l.gain }}</span>
          </li>
        </ul>
        <p v-if="!data.logs.length" class="logs-empty">尚无记录 · 从第一次突破开始</p>
      </section>
    </template>

    <!-- ============ 突破动画 ============ -->
    <transition name="ascend">
      <div v-if="ascension" class="ascend">
        <span class="a-flash" aria-hidden="true"></span>
        <span class="a-ring" aria-hidden="true"></span>
        <span class="a-ring delay" aria-hidden="true"></span>
        <svg class="a-seal" viewBox="0 0 100 100" fill="none" aria-hidden="true">
          <path
            d="M50 8 L58 34 L86 34 L63 50 L72 78 L50 61 L28 78 L37 50 L14 34 L42 34 Z"
            stroke="currentColor"
            stroke-width="1.5"
          />
          <circle cx="50" cy="50" r="30" stroke="currentColor" stroke-width=".6" stroke-dasharray="3 6" />
        </svg>
        <p class="a-text">境 界 突 破</p>
        <p class="a-realm">{{ ascension }}</p>
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

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
  gap: 20px;
  align-items: start;
}

/* ---------------- 战力面板 ---------------- */
.board {
  padding: 26px 26px 24px;
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
  margin-bottom: 22px;
  transform: rotate(-1.2deg);
}
.power-main { position: relative; }
.p-label { font-size: 11px; letter-spacing: .35em; color: #7d7466; }
.p-row { display: flex; align-items: baseline; gap: 8px; margin-top: 6px; }
.p-value {
  font-size: clamp(40px, 6vw, 62px);
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: .04em;
  color: #fff4e6;
  text-shadow: 0 0 26px var(--c1-glow);
  font-variant-numeric: tabular-nums;
}
.p-unit { font-size: 12px; letter-spacing: .3em; color: #9a8a55; }
.p-gain {
  margin-left: 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--c1);
  text-shadow: 0 0 16px var(--c1-glow);
}
.p-seal { display: block; margin-top: 8px; font-size: 11px; letter-spacing: .24em; color: #8a8291; }

/* 境界 */
.realm { margin-top: 26px; }
.realm-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 12px;
  letter-spacing: .2em;
  color: #b9b1a3;
  margin-bottom: 9px;
}
.realm-now { color: var(--c1); text-shadow: 0 0 12px var(--c1-glow); }
.realm-arrow { color: #5c5560; }
.realm-next { color: #8a8291; }
.realm-num { margin-left: auto; font-size: 10px; color: #5c5560; }
.track {
  height: 6px;
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

/* 资源 */
.res {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, .07);
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.res-item {
  flex: 1 1 100px;
  min-width: 100px;
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 11px;
  letter-spacing: .12em;
  color: #6d6660;
}
.res-item.spirit { flex: 1 1 100%; display: block; }
.res-item b { font-size: 16px; color: #e8e0d0; font-weight: 400; }
.res-item i { font-style: normal; color: #57505e; font-size: 10px; }
.res-top { display: flex; justify-content: space-between; margin-bottom: 7px; }
.r-name { color: #7d7466; letter-spacing: .3em; }
.r-val { color: #9a8a55; }
.r-track { height: 4px; background: rgba(255, 255, 255, .06); overflow: hidden; }
.r-fill {
  height: 100%;
  background: linear-gradient(90deg, #2e6ea8, #7fc4ff);
  box-shadow: 0 0 12px rgba(127, 196, 255, .4);
  transition: width .6s ease;
}

/* ---------------- 提升途径 ---------------- */
.actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.act {
  display: flex;
  flex-direction: column;
  padding: 16px 17px 15px;
  border: 1px solid rgba(255, 255, 255, .09);
  border-radius: 3px;
  background: rgba(0, 0, 0, .34);
  transition: border-color .3s, box-shadow .3s, transform .3s;
}
.act:hover { border-color: var(--c1-dim); box-shadow: 0 0 20px var(--c1-soft); transform: translateY(-2px); }
.act.rest { grid-column: span 2; }
.act.off { opacity: .45; }
.act.off:hover { transform: none; box-shadow: none; }
.act-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.act-label { font-size: 14px; letter-spacing: .2em; color: #e8e0d0; }
.act-gain {
  font-size: 12px;
  color: var(--c1);
  letter-spacing: .08em;
  text-shadow: 0 0 10px var(--c1-glow);
}
.act-desc {
  margin: 9px 0 12px;
  font-size: 11.5px;
  line-height: 1.8;
  letter-spacing: .06em;
  color: #6d6660;
  min-height: 42px;
}
.act-foot {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 10.5px;
  letter-spacing: .1em;
  color: #57505e;
  margin-bottom: 12px;
}
.act-cost { color: #9a8a55; }
.act-effect { color: #6d6478; }
.act-btn {
  margin-top: auto;
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, #2e1113, #140608);
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: var(--c1);
  font-family: inherit;
  font-size: 13px;
  letter-spacing: .4em;
  text-indent: .4em;
  cursor: pointer;
  transition: all .3s;
}
:global(body.god-mode) .act-btn { background: linear-gradient(135deg, #131c33, #060a14); }
.act-btn:hover:not(:disabled) {
  background: var(--c1);
  border-color: var(--c1);
  color: #0a0508;
  box-shadow: 0 0 26px var(--c1-glow);
}
.act-btn:active:not(:disabled) { transform: scale(.985); }
.act-btn:disabled { cursor: not-allowed; color: #5c5560; border-color: #332b34; }

/* ---------------- 记录 ---------------- */
.logs {
  margin-top: 20px;
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
.l-gain { color: var(--c1); text-shadow: 0 0 10px var(--c1-glow); }
.logs-empty { font-size: 11px; letter-spacing: .2em; color: #57505e; }

/* 动效 */
.pop-enter-active { transition: opacity .25s, transform .35s cubic-bezier(.2, .9, .3, 1.4); }
.pop-leave-active { transition: opacity .4s, transform .4s; }
.pop-enter-from { opacity: 0; transform: translateY(6px) scale(.8); }
.pop-leave-to { opacity: 0; transform: translateY(-10px); }

.fade-enter-active, .fade-leave-active { transition: opacity .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ---------------- 突破动画 ---------------- */
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
  animation: ascendFlash 1.8s ease-out forwards;
}
.a-ring {
  position: absolute;
  width: 120px;
  height: 120px;
  border: 1px solid var(--c1);
  border-radius: 50%;
  box-shadow: inset 0 0 60px var(--c1-glow), 0 0 40px var(--c1-glow);
  animation: ascendRing 1.7s cubic-bezier(.2, .7, .3, 1) forwards;
}
.a-ring.delay { animation-delay: .28s; opacity: 0; }
.a-seal {
  position: relative;
  width: 96px;
  height: 96px;
  color: var(--c1);
  filter: drop-shadow(0 0 22px var(--c1-glow));
  animation: ascendSeal 1.8s cubic-bezier(.2, .9, .3, 1.15) forwards;
}
.a-text {
  position: relative;
  margin-top: 22px;
  font-size: 12px;
  letter-spacing: .8em;
  text-indent: .8em;
  color: #b9b1a3;
  animation: ascendText 1.8s ease forwards;
}
.a-realm {
  position: relative;
  margin-top: 10px;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: .3em;
  text-indent: .3em;
  color: #fff4e6;
  text-shadow: 0 0 30px var(--c1-glow);
  animation: ascendText 1.8s ease forwards;
}
@keyframes ascendFlash {
  0%   { opacity: 0; }
  16%  { opacity: 1; }
  100% { opacity: 0; }
}
@keyframes ascendRing {
  0%   { transform: scale(.3); opacity: .95; }
  100% { transform: scale(9); opacity: 0; }
}
@keyframes ascendSeal {
  0%   { transform: scale(.4) rotate(-30deg); opacity: 0; }
  28%  { transform: scale(1.08) rotate(0deg); opacity: 1; }
  68%  { transform: scale(1.16) rotate(6deg); opacity: 1; }
  100% { transform: scale(1.42) rotate(16deg); opacity: 0; }
}
@keyframes ascendText {
  0%   { opacity: 0; transform: translateY(12px); }
  24%  { opacity: 1; transform: none; }
  74%  { opacity: 1; transform: none; }
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

@media (max-width: 980px) {
  .layout { grid-template-columns: 1fr; }
}
@media (max-width: 620px) {
  .actions { grid-template-columns: 1fr; }
  .act.rest { grid-column: span 1; }
}
</style>
