<script setup>
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import HomeNav from './HomeNav.vue'
import HomeSearch from './HomeSearch.vue'
import LoreHall from './LoreHall.vue'
import FriendDrawer from './FriendDrawer.vue'
import ChatPanel from './ChatPanel.vue'
import SkillCodex from './SkillCodex.vue'
import ArenaPage from './ArenaPage.vue'
import GuildPage from './GuildPage.vue'
import RankPage from './RankPage.vue'
import {
  stats,
  notices,
  schedule,
  skillBars,
  warehouse
} from '../data/homeData.js'
import { loginDurationText } from '../api/session.js'

const props = defineProps({
  username: { type: String, default: '' },
  godMode: { type: Boolean, default: false },
  loginAt: { type: Number, default: 0 }
})
const emit = defineEmits(['logout', 'toggle-form'])

const searchRef = ref(null)

/* ---------------- 轻提示（替代 alert，保持沉浸感） ---------------- */
const toast = ref('')
let toastTimer = 0
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}
onBeforeUnmount(() => clearTimeout(toastTimer))

/* ---------------- 页面切换 ---------------- */
// home = 首页，skill / beast / guild / rank = 四个子页面
const page = ref('home')

// 导航 key -> 页面组件映射
const pageMap = {
  skill: SkillCodex,
  beast: ArenaPage,
  guild: GuildPage,
  rank: RankPage
}
const activePage = computed(() => pageMap[page.value] || null)

/* ---------------- 导航 ---------------- */
function handleNavigate(item) {
  if (!item) return
  const key = item.key

  // 子页面 / 首页：整页切换并回到顶部
  if (key === 'home' || pageMap[key]) {
    page.value = key
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  // 万神大厅：回首页并滚动到图鉴区域
  if (key === 'lore') {
    page.value = 'home'
    nextTick(() => {
      const el = document.getElementById('lore-hall')
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
    return
  }
  showToast(`「${item.label}」尚未开启　先把首页看个够`)
}

/* ---------------- 搜索 ---------------- */
function focusSearch() {
  const el = document.querySelector('.hero-search')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  // 等滚动稳定后再聚焦，避免移动端键盘顶飞
  setTimeout(() => searchRef.value && searchRef.value.focus(), 220)
}

function handlePick(item) {
  if (!item) return
  if (item.type === '空') {
    showToast(`神堂档案里没有收录「${item.name}」`)
    return
  }
  showToast(`${item.type} · ${item.name}　详情页尚未开启`)
}

/* ---------------- 形态 / 身份 ---------------- */
const formText = computed(() => (props.godMode ? '第二变 · 伪神形态' : '第一变 · 恶鬼形态'))
const duration = computed(() => loginDurationText(props.loginAt))
const greeting = computed(() => (props.godMode ? '伪神临世' : '恶鬼归位'))

const today = new Date()
const dateText = `${today.getFullYear()}.${String(today.getMonth() + 1).padStart(2, '0')}.${String(
  today.getDate()
).padStart(2, '0')}`

function notReady(name) {
  showToast(`「${name}」尚未开启`)
}

/* ---------------- 好友 & 传音 ---------------- */
const friendDrawerOpen = ref(false)
const chatFriend = ref(null) // { friendId, username, remark }

function openChat(f) {
  friendDrawerOpen.value = false
  chatFriend.value = f
}
</script>

<template>
  <div class="home">
    <HomeNav
      :username="username"
      :god-mode="godMode"
      :active-key="page === 'lore' ? 'home' : page"
      @search="focusSearch"
      @navigate="handleNavigate"
      @toggle-form="emit('toggle-form')"
      @logout="emit('logout')"
    />

    <!-- 子页面：神技图鉴 / 镇兽大比 / 恶鬼之牙 / 战力榜 -->
    <component :is="activePage" v-if="activePage" />

    <main v-else>
      <!-- ============ HERO ============ -->
      <section class="hero">
        <div class="hero-glow" aria-hidden="true"></div>

        <div class="hero-inner">
          <div class="hero-top">
            <span class="verdict">神堂判定：神弃之人 · 终生无法镶嵌神印</span>
            <span class="date">{{ dateText }} · 镇兽大比赛季</span>
          </div>

          <div class="hero-eyebrow">
            <span class="line"></span>
            觉 醒 伪 神
            <span class="line"></span>
          </div>

          <h1>
            <span class="greet">{{ greeting }}</span>
            <span class="name">{{ username || '无名者' }}</span>
          </h1>

          <p class="motto">神明弃我<span class="sep">，</span>我自为神</p>

          <div class="identity">
            <span class="chip">{{ formText }}</span>
            <span class="chip">天赋 · SSS 级</span>
            <span class="chip">公会 · 恶鬼之牙</span>
            <span v-if="duration" class="chip soft">{{ duration }}</span>
          </div>

          <!-- 搜索栏 -->
          <div class="hero-search">
            <HomeSearch ref="searchRef" @pick="handlePick" />
          </div>
        </div>
      </section>

      <!-- ============ 战力概览 ============ -->
      <section class="stats">
        <div class="stat" v-for="(s, i) in stats" :key="s.label" :style="{ '--i': i }">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">
            {{ s.value }}<span class="unit">{{ s.unit }}</span>
          </div>
          <div class="stat-hint">{{ s.hint }}</div>
        </div>
        <div class="stats-note">示例数据 · 待接入后端</div>
      </section>

      <!-- ============ 内容区 ============ -->
      <section class="grid">
        <!-- 神堂公告 -->
        <article class="card wide">
          <header class="card-head">
            <h2>神堂公告</h2>
            <button class="more" @click="notReady('公告列表')">全部 →</button>
          </header>
          <ul class="notice-list">
            <li v-for="n in notices" :key="n.title" class="notice">
              <span class="n-tag" :class="n.tag === '紧急' ? 'urgent' : ''">{{ n.tag }}</span>
              <span class="n-main">
                <span class="n-title">{{ n.title }}</span>
                <span class="n-time">{{ n.time }}</span>
              </span>
            </li>
          </ul>
        </article>

        <!-- 赛程 -->
        <article class="card">
          <header class="card-head">
            <h2>镇兽大比赛程</h2>
            <button class="more" @click="notReady('赛程详情')">赛制 →</button>
          </header>
          <ul class="schedule">
            <li v-for="s in schedule" :key="s.date + s.stage" class="s-row">
              <span class="s-date">{{ s.date }}</span>
              <span class="s-main">
                <span class="s-stage">{{ s.stage }}</span>
                <span class="s-versus">{{ s.versus }}</span>
              </span>
              <span class="s-state" :class="{ done: s.state === '未开始' }">{{ s.state }}</span>
            </li>
          </ul>
        </article>

        <!-- 神技熟练度 -->
        <article class="card">
          <header class="card-head">
            <h2>神技熟练度</h2>
            <button class="more" @click="notReady('神技图鉴')">图鉴 →</button>
          </header>
          <ul class="skills">
            <li v-for="sk in skillBars" :key="sk.name" class="skill">
              <div class="skill-top">
                <span class="skill-name">{{ sk.name }}</span>
                <span class="skill-level">{{ sk.level }}</span>
              </div>
              <div class="track">
                <div class="fill" :style="{ width: sk.pct + '%' }"></div>
              </div>
            </li>
          </ul>
        </article>

        <!-- 仓库 -->
        <article class="card">
          <header class="card-head">
            <h2>恶鬼之牙 · 仓库</h2>
            <button class="more" @click="notReady('仓库管理')">取用 →</button>
          </header>
          <div class="wares">
            <div v-for="w in warehouse" :key="w.name" class="ware">
              <span class="w-name">{{ w.name }}</span>
              <span class="w-count">×{{ w.count }}</span>
              <span class="w-rarity" :class="'r-' + w.rarity">{{ w.rarity }}</span>
            </div>
          </div>
          <p class="card-foot">仓库二层已开放，需副会长以上神印。</p>
        </article>
      </section>

      <!-- ============ 万神大厅 · 世界观图鉴 ============ -->
      <LoreHall id="lore-hall" />

      <!-- ============ 宣言 ============ -->
      <section class="creed">
        <p>
          神说他是弃子，<em>恶鬼之牙</em>便倾尽仓库为他铺路。<br />
          神不给我的，<em>我自取之</em>；神嫌弃的，我将其升华为<em>禁忌</em>。
        </p>
      </section>
    </main>

    <footer class="foot">
      <div>觉 醒 伪 神 · 神明弃我 我自为神</div>
      <div class="foot-dim">前端首页 · 其余页面尚未开启 · v1.0.0</div>
    </footer>

    <!-- 好友悬浮入口（固定右下角，不占首页布局） -->
    <button
      class="friend-fab"
      title="好友名录 · 传音"
      @click="friendDrawerOpen = true"
    >
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="9" cy="8" r="3.2" stroke="currentColor" stroke-width="1.6" />
        <path d="M3.5 19c.6-3.2 2.9-5 5.5-5s4.9 1.8 5.5 5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        <path d="M15.5 8.5a2.8 2.8 0 1 1 2 4.8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        <path d="M17.5 15.5c1.8.4 2.9 1.7 3.3 3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
      </svg>
      <span>好友</span>
    </button>

    <!-- 好友名录抽屉 -->
    <FriendDrawer
      :open="friendDrawerOpen"
      @close="friendDrawerOpen = false"
      @chat="openChat"
    />

    <!-- 传音（聊天）面板 -->
    <ChatPanel
      :friend="chatFriend"
      @close="chatFriend = null"
    />

    <!-- 轻提示 -->
    <transition name="toast">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.home {
  position: relative;
  z-index: 2;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

main {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 28px 60px;
}

/* ================= HERO ================= */
.hero {
  position: relative;
  padding: 62px 0 54px;
  overflow: hidden;
}
.hero-glow {
  position: absolute;
  top: -180px;
  left: 50%;
  width: 780px;
  height: 480px;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at center, var(--c1-glow), transparent 68%);
  opacity: .38;
  filter: blur(30px);
  pointer-events: none;
  transition: background .9s;
}
.hero-inner {
  position: relative;
  max-width: 860px;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 30px;
}
.verdict {
  display: inline-block;
  font-size: 11.5px;
  letter-spacing: .3em;
  color: #9a917f;
  border: 1px dashed #55503f;
  padding: 6px 12px 6px 16px;
  transform: rotate(-1.2deg);
  position: relative;
}
.verdict::before {
  content: "✕";
  position: absolute;
  right: -13px;
  top: -11px;
  font-size: 17px;
  color: var(--c1);
  text-shadow: 0 0 10px var(--c1-glow);
  transition: color .8s;
}
.date {
  font-size: 11px;
  letter-spacing: .26em;
  color: #5d5666;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 11px;
  letter-spacing: .5em;
  color: var(--c1);
  text-shadow: 0 0 14px var(--c1-glow);
  margin-bottom: 18px;
  transition: color .8s;
}
.hero-eyebrow .line {
  width: 44px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--c1));
}
.hero-eyebrow .line:last-child {
  background: linear-gradient(90deg, var(--c1), transparent);
}

h1 {
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.25;
  font-weight: 900;
  letter-spacing: .06em;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0 18px;
}
h1 .greet { color: #8f8776; font-size: .62em; letter-spacing: .2em; }
h1 .name {
  color: var(--c1);
  text-shadow: 0 0 30px var(--c1-glow);
  transition: color .8s, text-shadow .8s;
}

.motto {
  margin-top: 14px;
  font-size: 15px;
  letter-spacing: .34em;
  color: #a89c8a;
}
.motto .sep { color: var(--c1); margin: 0 4px; text-shadow: 0 0 12px var(--c1-glow); }

.identity {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
}
.chip {
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 2px;
  background: rgba(0, 0, 0, .32);
  font-size: 11.5px;
  letter-spacing: .16em;
  color: #a99eb0;
}
.chip.soft { color: #6f6779; border-style: dashed; }

.hero-search { margin-top: 34px; }

/* ================= 战力概览 ================= */
.stats {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(22, 15, 18, .8), rgba(8, 5, 6, .86));
  box-shadow: inset 0 0 60px var(--c1-soft);
  transition: background .8s, box-shadow .8s;
}
:global(body.god-mode) .stats {
  background: linear-gradient(160deg, rgba(14, 17, 30, .8), rgba(4, 5, 10, .86));
}
.stat {
  padding: 22px 24px 24px;
  border-right: 1px solid rgba(255, 255, 255, .06);
  animation: riseIn .6s ease both;
  animation-delay: calc(var(--i) * 70ms);
}
.stat:last-of-type { border-right: none; }
.stat-label {
  font-size: 11px;
  letter-spacing: .3em;
  color: #7d7466;
}
.stat-value {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 900;
  letter-spacing: .04em;
  color: #e8e0d0;
  text-shadow: 0 0 20px var(--c1-soft);
}
.stat-value .unit {
  font-size: 12px;
  font-weight: 400;
  color: #7d7466;
  margin-left: 4px;
  letter-spacing: .1em;
}
.stat-hint {
  margin-top: 8px;
  font-size: 10.5px;
  letter-spacing: .1em;
  color: #5d5666;
}
.stats-note {
  position: absolute;
  right: 12px;
  bottom: 8px;
  font-size: 9.5px;
  letter-spacing: .12em;
  color: #453f4e;
}

@keyframes riseIn {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: none; }
}

/* ================= 卡片网格 ================= */
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-top: 26px;
}
.card {
  position: relative;
  padding: 22px 24px 24px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .78), rgba(7, 4, 5, .84));
  transition: border-color .4s, box-shadow .4s, transform .4s, background .8s;
}
:global(body.god-mode) .card {
  background: linear-gradient(160deg, rgba(14, 17, 30, .78), rgba(4, 5, 10, .84));
}
.card:hover {
  border-color: var(--c1-dim);
  box-shadow: 0 14px 44px rgba(0, 0, 0, .5), inset 0 0 40px var(--c1-soft);
  transform: translateY(-2px);
}
.card.wide { grid-column: span 2; }

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, .06);
}
.card-head h2 {
  font-size: 14px;
  font-weight: 400;
  letter-spacing: .3em;
  color: #ded5e4;
  position: relative;
  padding-left: 12px;
}
.card-head h2::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 13px;
  margin-top: -7px;
  background: var(--c1);
  box-shadow: 0 0 10px var(--c1-glow);
  transition: background .8s;
}
.more {
  background: none;
  border: none;
  color: #6f6779;
  font-family: inherit;
  font-size: 11px;
  letter-spacing: .16em;
  cursor: pointer;
  transition: color .3s;
  flex: none;
}
.more:hover { color: var(--c1); }

/* 公告 */
.notice-list { list-style: none; display: flex; flex-direction: column; gap: 2px; }
.notice {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, .05);
}
.notice:last-child { border-bottom: none; }
.n-tag {
  flex: none;
  margin-top: 2px;
  padding: 2px 7px;
  font-size: 10px;
  letter-spacing: .1em;
  color: #9d93a4;
  border: 1px solid rgba(255, 255, 255, .14);
  border-radius: 2px;
}
.n-tag.urgent {
  color: #ff8f8f;
  border-color: rgba(255, 77, 77, .4);
  background: rgba(255, 77, 77, .08);
}
.n-main { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.n-title {
  font-size: 13px;
  letter-spacing: .08em;
  color: #d8cfbf;
  line-height: 1.55;
}
.n-time { font-size: 10.5px; letter-spacing: .1em; color: #5d5666; }

/* 赛程 */
.schedule { list-style: none; }
.s-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, .05);
}
.s-row:last-child { border-bottom: none; }
.s-date {
  flex: none;
  font-size: 13px;
  letter-spacing: .06em;
  color: var(--c1);
  text-shadow: 0 0 12px var(--c1-soft);
  font-family: Georgia, serif;
  transition: color .8s;
}
.s-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.s-stage { font-size: 12.5px; letter-spacing: .14em; color: #cfc6d6; }
.s-versus {
  font-size: 11px;
  color: #6f6779;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.s-state {
  flex: none;
  font-size: 10.5px;
  letter-spacing: .1em;
  color: #5f5869;
}
.s-state.done { color: #e0a86a; }

/* 神技 */
.skills { list-style: none; display: flex; flex-direction: column; gap: 15px; }
.skill-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 7px;
}
.skill-name { font-size: 12.5px; letter-spacing: .1em; color: #cfc6d6; }
.skill-level { font-size: 10.5px; color: var(--c1-dim); letter-spacing: .1em; }
.track {
  height: 3px;
  background: rgba(255, 255, 255, .07);
  border-radius: 2px;
  overflow: hidden;
}
.fill {
  height: 100%;
  background: linear-gradient(90deg, var(--c1-dim), var(--c1));
  box-shadow: 0 0 12px var(--c1-glow);
  transition: width .8s ease, background .8s;
}

/* 仓库 */
.wares {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.ware {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, .07);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: border-color .3s;
}
.ware:hover { border-color: var(--c1-dim); }
.w-name {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  letter-spacing: .08em;
  color: #cfc6d6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.w-count { font-size: 11px; color: var(--c1); letter-spacing: .04em; transition: color .8s; }
.w-rarity { font-size: 9.5px; letter-spacing: .06em; color: #5f5869; }
.w-rarity.r-史诗 { color: #b79ce0; }
.w-rarity.r-稀有 { color: #7fb6d8; }

.card-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(255, 255, 255, .05);
  font-size: 10.5px;
  letter-spacing: .1em;
  color: #5d5666;
}

/* ================= 宣言 ================= */
.creed {
  margin-top: 44px;
  padding: 34px 0 8px;
  text-align: center;
  border-top: 1px solid rgba(255, 255, 255, .06);
}
.creed p {
  font-size: 14px;
  line-height: 2.2;
  letter-spacing: .22em;
  color: #938a96;
}
.creed em {
  font-style: normal;
  color: var(--c1);
  transition: color .8s;
}

/* ================= 页脚 ================= */
.foot {
  border-top: 1px solid rgba(255, 255, 255, .06);
  padding: 26px 28px 30px;
  text-align: center;
  font-size: 11.5px;
  letter-spacing: .3em;
  color: #7d7466;
}
.foot-dim { margin-top: 8px; font-size: 10px; letter-spacing: .2em; color: #453f4e; }

/* ================= 好友悬浮入口 ================= */
.friend-fab {
  position: fixed;
  right: 26px;
  bottom: 26px;
  z-index: 55;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px 12px 14px;
  background: linear-gradient(160deg, rgba(26, 17, 20, .95), rgba(8, 5, 6, .97));
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: var(--c1);
  font-family: inherit;
  font-size: 12.5px;
  letter-spacing: .2em;
  cursor: pointer;
  box-shadow: 0 12px 40px rgba(0, 0, 0, .55), 0 0 18px var(--c1-soft);
  transition: all .35s, background .8s, border-color .8s;
}
:global(body.god-mode) .friend-fab {
  background: linear-gradient(160deg, rgba(14, 17, 30, .95), rgba(4, 5, 10, .97));
}
.friend-fab svg { width: 19px; height: 19px; }
.friend-fab:hover {
  transform: translateY(-2px);
  border-color: var(--c1);
  box-shadow: 0 16px 48px rgba(0, 0, 0, .6), 0 0 30px var(--c1-soft);
}

/* ================= 提示 ================= */
.toast {
  position: fixed;
  left: 50%;
  bottom: 42px;
  transform: translateX(-50%);
  z-index: 60;
  padding: 12px 22px;
  background: linear-gradient(160deg, rgba(26, 17, 20, .97), rgba(8, 5, 6, .98));
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: #ded5e4;
  font-size: 12.5px;
  letter-spacing: .14em;
  white-space: nowrap;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .7), 0 0 26px var(--c1-soft);
  transition: background .8s, border-color .8s;
}
:global(body.god-mode) .toast {
  background: linear-gradient(160deg, rgba(14, 17, 30, .97), rgba(4, 5, 10, .98));
}
.toast-enter-active, .toast-leave-active { transition: opacity .25s, transform .25s; }
.toast-enter-from, .toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}

/* ================= 响应式 ================= */
@media (max-width: 1080px) {
  .grid { grid-template-columns: repeat(2, 1fr); }
  .card.wide { grid-column: span 2; }
}
@media (max-width: 860px) {
  main { padding: 0 18px 50px; }
  .hero { padding: 46px 0 40px; }
  .stats { grid-template-columns: repeat(2, 1fr); }
  .stat { border-right: none; border-bottom: 1px solid rgba(255, 255, 255, .06); }
  .stat:nth-child(2n) { border-right: none; }
  .stats-note { display: none; }
}
@media (max-width: 640px) {
  .grid { grid-template-columns: 1fr; }
  .card.wide { grid-column: span 1; }
  h1 { gap: 0 12px; }
  .motto { font-size: 13px; letter-spacing: .24em; }
  .identity .chip { font-size: 10.5px; padding: 5px 9px; }
  .toast { left: 16px; right: 16px; transform: none; white-space: normal; text-align: center; }
  .toast-enter-from, .toast-leave-to { transform: translateY(10px); }
}
</style>
