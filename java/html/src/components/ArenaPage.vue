<script setup>
import { ref } from 'vue'
import { fetchMatches } from '../api/content.js'

const matches = ref([])
const loading = ref(true)
const errorText = ref('')

load()
async function load() {
  const r = await fetchMatches()
  if (r.code === 0) {
    matches.value = r.data || []
  } else {
    errorText.value = r.message
  }
  loading.value = false
}

const rules = [
  '十六强至决赛均在神堂中央斗场进行，斗场神纹每轮重铸',
  '八强起禁用治愈类神技，违规即判负',
  '镇兽塔妖兽本源可作为底牌，但需赛前申报',
  '决赛胜者可入塔免试一层，并获神堂名录除名权一次'
]
</script>

<template>
  <div class="page">
    <header class="page-head">
      <h2>镇 兽 大 比</h2>
      <span class="page-en">BEAST GRAND ARENA · 年轻一辈的巅峰斗场</span>
    </header>

    <div v-if="loading" class="hint">赛程呈递中…</div>
    <div v-else-if="errorText" class="hint err">{{ errorText }}</div>

    <template v-else>
      <div class="match-list">
        <div v-for="m in matches" :key="m.id" class="match-card" :class="{ done: m.state === '已结束' }">
          <div class="m-date">
            <span class="d">{{ m.matchDate }}</span>
            <span class="stage">{{ m.stage }}</span>
          </div>
          <div class="m-main">
            <span class="vs">
              <b class="pa">{{ m.playerA }}</b>
              <i>×</i>
              <b class="pb">{{ m.playerB }}</b>
            </span>
            <span v-if="m.result" class="result">{{ m.result }}</span>
            <span class="venue">{{ m.venue }}</span>
          </div>
          <span class="m-state" :class="{ done: m.state === '已结束' }">{{ m.state }}</span>
        </div>
      </div>

      <section class="rules card-panel">
        <h3>赛 制 备 忘</h3>
        <ul>
          <li v-for="(r, i) in rules" :key="i">{{ r }}</li>
        </ul>
      </section>
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

.match-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 26px;
}
.match-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 15px 20px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .78), rgba(7, 4, 5, .84));
  transition: border-color .3s, background .8s;
}
:global(body.god-mode) .match-card {
  background: linear-gradient(160deg, rgba(14, 17, 30, .78), rgba(4, 5, 10, .84));
}
.match-card:hover { border-color: var(--c1-dim); }

.m-date {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 76px;
}
.m-date .d {
  font-family: Georgia, serif;
  font-size: 17px;
  color: var(--c1);
  text-shadow: 0 0 12px var(--c1-soft);
  transition: color .8s;
}
.m-date .stage { font-size: 10px; letter-spacing: .16em; color: #7d7466; }

.m-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.vs { font-size: 14px; letter-spacing: .08em; color: #e8e0d0; }
.vs i { font-style: normal; color: var(--c1); margin: 0 10px; transition: color .8s; }
.vs b { font-weight: 700; }
.result { font-size: 11px; letter-spacing: .08em; color: #e0a86a; }
.venue { font-size: 10px; letter-spacing: .14em; color: #5f5869; }

.m-state {
  flex: none;
  font-size: 11px;
  letter-spacing: .12em;
  color: #5f5869;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 2px;
  padding: 4px 10px;
}
.m-state.done { color: #e0a86a; border-color: rgba(224, 168, 106, .4); }

.card-panel {
  padding: 20px 24px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .78), rgba(7, 4, 5, .84));
  transition: background .8s;
}
:global(body.god-mode) .card-panel {
  background: linear-gradient(160deg, rgba(14, 17, 30, .78), rgba(4, 5, 10, .84));
}
.card-panel h3 {
  font-size: 12px;
  font-weight: 400;
  letter-spacing: .3em;
  color: #9a8a55;
  margin-bottom: 14px;
}
.card-panel ul { list-style: none; display: flex; flex-direction: column; gap: 9px; }
.card-panel li {
  font-size: 12.5px;
  line-height: 1.8;
  letter-spacing: .06em;
  color: #938a96;
  padding-left: 16px;
  position: relative;
}
.card-panel li::before {
  content: "·";
  position: absolute;
  left: 0;
  color: var(--c1);
  transition: color .8s;
}

@media (max-width: 640px) {
  .page { padding: 24px 16px 50px; }
  .m-date { min-width: 60px; }
}
</style>
