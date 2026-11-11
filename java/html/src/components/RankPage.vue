<script setup>
import { computed, ref } from 'vue'
import { fetchRank } from '../api/content.js'
import { getSession } from '../api/session.js'

const members = ref([])
const loading = ref(true)
const errorText = ref('')
const me = getSession()?.username || ''

load()
async function load() {
  const r = await fetchRank()
  if (r.code === 0) {
    members.value = r.data || []
  } else {
    errorText.value = r.message
  }
  loading.value = false
}

// 前端计算名次（后端已按战力倒序返回）
const ranked = computed(() => members.value.map((m, i) => ({ ...m, rank: i + 1 })))

const top = computed(() => ranked.value.slice(0, 3))
const rest = computed(() => ranked.value.slice(3))
</script>

<template>
  <div class="page">
    <header class="page-head">
      <h2>战 力 榜</h2>
      <span class="page-en">POWER RANKING · 恶鬼之牙在册战力</span>
    </header>

    <div v-if="loading" class="hint">神纹碑点亮中…</div>
    <div v-else-if="errorText" class="hint err">{{ errorText }}</div>

    <template v-else>
      <!-- 前三甲 -->
      <div class="podium">
        <div
          v-for="m in top"
          :key="m.id"
          class="podium-card"
          :class="['p' + m.rank, { mine: m.name === me }]"
        >
          <span class="rank-badge">{{ m.rank }}</span>
          <span class="p-name">{{ m.name }}<i v-if="m.name === me" class="me-tag">我</i></span>
          <span class="p-role">{{ m.role }} · {{ m.realm }}</span>
          <span class="p-power">{{ m.power }}<i>点</i></span>
        </div>
      </div>

      <!-- 其余名次 -->
      <div class="rank-list">
        <div v-for="m in rest" :key="m.id" class="rank-row" :class="{ mine: m.name === me }">
          <span class="r-no">{{ m.rank }}</span>
          <div class="r-main">
            <span class="r-name">{{ m.name }}<i class="r-role">{{ m.role }}</i><i v-if="m.name === me" class="me-tag">我</i></span>
            <span class="r-sub">{{ m.element }} · {{ m.realm }} · {{ m.bio }}</span>
          </div>
          <span class="r-power">{{ m.power }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 34px 28px 60px;
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
  width: 3px;
  height: 15px;
  margin-top: -8px;
  background: var(--c1);
  box-shadow: 0 0 10px var(--c1-glow);
  transition: background .8s;
}
.page-en { font-size: 9.5px; letter-spacing: .22em; color: #5d5666; }

.hint {
  text-align: center;
  font-size: 13px;
  line-height: 2;
  letter-spacing: .2em;
  color: #6f6779;
  padding: 80px 0;
}
.hint.err { color: #c05a50; }

/* 前三甲 */
.podium {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 22px;
}
.podium-card {
  position: relative;
  padding: 24px 22px 20px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .78), rgba(7, 4, 5, .84));
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: background .8s, border-color .3s, box-shadow .3s, transform .3s;
}
:global(body.god-mode) .podium-card {
  background: linear-gradient(160deg, rgba(14, 17, 30, .78), rgba(4, 5, 10, .84));
}
.podium-card:hover {
  border-color: var(--c1-dim);
  box-shadow: inset 0 0 40px var(--c1-soft);
  transform: translateY(-3px);
}
.podium-card.p1 {
  border-color: var(--c1);
  box-shadow: inset 0 0 50px var(--c1-soft), 0 0 24px var(--c1-soft);
}
.podium-card.p2, .podium-card.p3 {
  margin-top: 14px;
}
.rank-badge {
  position: absolute;
  top: -14px;
  left: 18px;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-family: Georgia, serif;
  font-size: 15px;
  font-weight: 900;
  color: #0a0508;
  background: var(--c1);
  box-shadow: 0 0 20px var(--c1-glow);
  transition: background .8s;
}
.p2 .rank-badge, .p3 .rank-badge {
  color: #e8e0d0;
  background: rgba(0, 0, 0, .6);
  border: 1px solid var(--c1-dim);
  box-shadow: none;
}
.p-name { margin-top: 6px; font-size: 19px; font-weight: 900; letter-spacing: .14em; color: #e8e0d0; }
.p-role { font-size: 10.5px; letter-spacing: .14em; color: #7d7466; }
.p-power {
  font-family: Georgia, serif;
  font-size: 26px;
  font-weight: 900;
  color: var(--c1);
  text-shadow: 0 0 18px var(--c1-glow);
  transition: color .8s;
}
.p-power i { font-style: normal; font-size: 11px; font-weight: 400; color: #7d7466; margin-left: 5px; }

/* 自己那一行 */
.me-tag {
  display: inline-block;
  font-style: normal;
  font-size: 9.5px;
  letter-spacing: .1em;
  color: #ff9a9a;
  border: 1px solid rgba(255, 77, 77, .45);
  border-radius: 2px;
  padding: 1px 5px;
  margin-left: 7px;
  background: rgba(255, 77, 77, .12);
  text-shadow: 0 0 8px rgba(255, 77, 77, .3);
  vertical-align: middle;
}
.podium-card.mine {
  border-color: rgba(255, 77, 77, .55);
  box-shadow: inset 0 0 40px rgba(255, 77, 77, .14), 0 0 18px rgba(255, 77, 77, .18);
}
.rank-row.mine {
  border-color: rgba(255, 77, 77, .4);
  background: rgba(255, 77, 77, .07);
}

/* 其余名次 */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 18px;
  border: 1px solid rgba(255, 255, 255, .07);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: border-color .3s;
}
.rank-row:hover { border-color: var(--c1-dim); }
.r-no {
  flex: none;
  width: 30px;
  font-family: Georgia, serif;
  font-size: 15px;
  color: #9d93a4;
}
.r-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.r-name { font-size: 13.5px; letter-spacing: .1em; color: #e8e0d0; display: flex; align-items: center; gap: 8px; }
.r-role {
  font-style: normal;
  font-size: 9.5px;
  letter-spacing: .1em;
  color: #9a8a55;
  border: 1px solid rgba(154, 138, 85, .4);
  border-radius: 2px;
  padding: 1px 6px;
}
.r-sub {
  font-size: 10.5px;
  letter-spacing: .05em;
  color: #6f6779;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.r-power {
  flex: none;
  font-family: Georgia, serif;
  font-size: 15px;
  color: var(--c1);
  text-shadow: 0 0 10px var(--c1-soft);
  transition: color .8s;
}

@media (max-width: 860px) {
  .podium { grid-template-columns: 1fr; }
  .podium-card.p2, .podium-card.p3 { margin-top: 0; }
}
@media (max-width: 640px) {
  .page { padding: 24px 16px 50px; }
}
</style>
