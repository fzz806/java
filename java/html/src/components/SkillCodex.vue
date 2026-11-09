<script setup>
import { ref } from 'vue'
import { fetchSkills } from '../api/content.js'

const skills = ref([])
const loading = ref(true)
const errorText = ref('')

load()
async function load() {
  const r = await fetchSkills()
  if (r.code === 0) {
    skills.value = r.data || []
  } else {
    errorText.value = r.message
  }
  loading.value = false
}

function masteryText(m) {
  return `熟练 ${m}%`
}
</script>

<template>
  <div class="page">
    <header class="page-head">
      <h2>神 技 图 鉴</h2>
      <span class="page-en">SKILL CODEX · 禁忌在前，神堂未许</span>
      <span class="page-count" v-if="!loading">共 {{ skills.length }} 式</span>
    </header>

    <div v-if="loading" class="hint">图鉴开启中…</div>
    <div v-else-if="errorText" class="hint err">{{ errorText }}</div>

    <div v-else class="skill-list">
      <div
        v-for="s in skills"
        :key="s.id"
        class="skill-card"
        :class="{ forbidden: s.forbidden === 1 }"
      >
        <div class="s-left">
          <span class="s-level" :class="'lv-' + (s.level || '').replace('+', 'p')">{{ s.level }}</span>
        </div>
        <div class="s-main">
          <div class="s-top">
            <span class="s-name">
              {{ s.name }}
              <em v-if="s.forbidden === 1" class="fb-tag">禁忌</em>
            </span>
            <span class="s-element">{{ s.element }}<template v-if="s.transform"> · {{ s.transform }}</template></span>
          </div>
          <p class="s-desc">{{ s.desc }}</p>
          <div class="s-track">
            <div class="s-fill" :style="{ width: s.mastery + '%' }"></div>
          </div>
        </div>
        <div class="s-right">{{ masteryText(s.mastery) }}</div>
      </div>
    </div>
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
.page-count { margin-left: auto; font-size: 11px; letter-spacing: .14em; color: #9a8a55; }

.hint {
  text-align: center;
  font-size: 13px;
  line-height: 2;
  letter-spacing: .2em;
  color: #6f6779;
  padding: 80px 0;
}
.hint.err { color: #c05a50; }

.skill-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.skill-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .78), rgba(7, 4, 5, .84));
  transition: border-color .3s, box-shadow .3s, transform .3s, background .8s;
}
:global(body.god-mode) .skill-card {
  background: linear-gradient(160deg, rgba(14, 17, 30, .78), rgba(4, 5, 10, .84));
}
.skill-card:hover {
  border-color: var(--c1-dim);
  box-shadow: 0 12px 40px rgba(0, 0, 0, .45), inset 0 0 30px var(--c1-soft);
  transform: translateX(3px);
}
.skill-card.forbidden {
  border-color: rgba(255, 77, 77, .3);
  border-left: 2px solid #ff4d4d;
}

.s-left { flex: none; }
.s-level {
  display: grid;
  place-items: center;
  min-width: 56px;
  height: 40px;
  padding: 0 8px;
  font-family: Georgia, serif;
  font-size: 17px;
  font-weight: 900;
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: var(--c1);
  text-shadow: 0 0 12px var(--c1-glow);
  transition: color .8s, border-color .8s;
}
.s-level.lv-SSS {
  color: #ffb14d;
  border-color: rgba(255, 177, 77, .5);
  text-shadow: 0 0 14px rgba(255, 177, 77, .6);
}
.s-level.lv-B { color: #9d93a4; border-color: rgba(255, 255, 255, .16); text-shadow: none; }

.s-main { flex: 1; min-width: 0; }
.s-top { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.s-name { font-size: 15px; letter-spacing: .1em; color: #e8e0d0; display: flex; align-items: center; gap: 8px; }
.fb-tag {
  font-style: normal;
  font-size: 9.5px;
  letter-spacing: .14em;
  color: #ff8f8f;
  border: 1px solid rgba(255, 77, 77, .4);
  background: rgba(255, 77, 77, .08);
  border-radius: 2px;
  padding: 1px 7px;
}
.s-element { font-size: 10.5px; letter-spacing: .12em; color: #9a8a55; }
.s-desc {
  margin: 8px 0 10px;
  font-size: 12px;
  line-height: 1.7;
  letter-spacing: .04em;
  color: #938a96;
}
.s-track {
  height: 3px;
  background: rgba(255, 255, 255, .07);
  border-radius: 2px;
  overflow: hidden;
}
.s-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--c1-dim), var(--c1));
  box-shadow: 0 0 12px var(--c1-glow);
  transition: width .8s ease, background .8s;
}
.s-right {
  flex: none;
  font-size: 10.5px;
  letter-spacing: .1em;
  color: #5f5869;
}

@media (max-width: 640px) {
  .page { padding: 24px 16px 50px; }
  .s-right { display: none; }
}
</style>
