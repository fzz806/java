<script setup>
import { ref } from 'vue'
import { fetchGuild } from '../api/content.js'
import { getSession } from '../api/session.js'

const info = ref({})
const members = ref([])
const loading = ref(true)
const errorText = ref('')
const me = getSession()?.username || ''

load()
async function load() {
  const r = await fetchGuild()
  if (r.code === 0) {
    info.value = r.data?.info || {}
    members.value = r.data?.members || []
  } else {
    errorText.value = r.message
  }
  loading.value = false
}

function initial(name) {
  return name ? name.slice(0, 1) : '？'
}
</script>

<template>
  <div class="page">
    <header class="page-head">
      <h2>恶 鬼 之 牙</h2>
      <span class="page-en">GUILD · 神堂黑名单头号公会</span>
    </header>

    <div v-if="loading" class="hint">公会大门开启中…</div>
    <div v-else-if="errorText" class="hint err">{{ errorText }}</div>

    <template v-else>
      <!-- 简报 -->
      <section class="brief card-panel">
        <div class="brand-row">
          <span class="g-name">{{ info.name || '恶鬼之牙' }}</span>
          <span class="g-en">{{ info.en }}</span>
        </div>
        <p class="g-motto">「 {{ info.motto }} 」</p>
        <div class="g-meta">
          <span>{{ info.members }}</span>
          <span>{{ info.warehouse }}</span>
          <span class="danger">{{ info.stance }}</span>
        </div>
      </section>

      <!-- 成员 -->
      <section class="card-panel">
        <h3>在 册 恶 鬼</h3>
        <div class="member-list">
          <div v-for="m in members" :key="m.id" class="member" :class="{ mine: m.name === me }">
            <span class="avatar">{{ initial(m.name) }}</span>
            <div class="mem-main">
              <span class="mem-name">{{ m.name }}<i class="mem-role">{{ m.role }}</i><i v-if="m.name === me" class="mem-me">我</i></span>
              <span class="mem-desc">{{ m.bio }}</span>
              <span class="mem-sub">{{ m.element }} · {{ m.realm }}</span>
            </div>
            <span class="mem-power">{{ m.power }}</span>
          </div>
        </div>
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

.card-panel {
  padding: 20px 24px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .78), rgba(7, 4, 5, .84));
  margin-bottom: 18px;
  transition: background .8s;
}
:global(body.god-mode) .card-panel {
  background: linear-gradient(160deg, rgba(14, 17, 30, .78), rgba(4, 5, 10, .84));
}

.brief .brand-row { display: flex; align-items: baseline; gap: 14px; flex-wrap: wrap; }
.g-name {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: .18em;
  background: linear-gradient(180deg, #fff4e6, var(--c1) 70%, var(--c1-dim));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 12px var(--c1-glow));
  transition: filter .8s;
}
.g-en { font-size: 10px; letter-spacing: .24em; color: #5d5666; }
.g-motto {
  margin-top: 10px;
  font-size: 13px;
  letter-spacing: .26em;
  color: #a89c8a;
}
.g-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.g-meta span {
  font-size: 11px;
  letter-spacing: .12em;
  color: #a99eb0;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 2px;
  padding: 5px 11px;
  background: rgba(0, 0, 0, .3);
}
.g-meta span.danger {
  color: #ff8f8f;
  border-color: rgba(255, 77, 77, .4);
  background: rgba(255, 77, 77, .08);
}

.card-panel h3 {
  font-size: 12px;
  font-weight: 400;
  letter-spacing: .3em;
  color: #9a8a55;
  margin-bottom: 16px;
}

.member-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.member {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, .07);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: border-color .3s;
}
.member:hover { border-color: var(--c1-dim); }
.member.mine {
  border-color: rgba(255, 77, 77, .45);
  background: rgba(255, 77, 77, .06);
  box-shadow: inset 0 0 18px rgba(255, 77, 77, .1);
}
.mem-me {
  font-style: normal;
  font-size: 9.5px;
  letter-spacing: .1em;
  color: #ff9a9a;
  border: 1px solid rgba(255, 77, 77, .45);
  border-radius: 2px;
  padding: 1px 5px;
  background: rgba(255, 77, 77, .12);
  text-shadow: 0 0 8px rgba(255, 77, 77, .3);
}
.avatar {
  flex: none;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--c1-dim);
  background: rgba(0, 0, 0, .5);
  color: var(--c1);
  font-size: 15px;
  text-shadow: 0 0 10px var(--c1-glow);
  transition: color .8s, border-color .8s;
}
.mem-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.mem-name { font-size: 13.5px; letter-spacing: .1em; color: #e8e0d0; display: flex; align-items: center; gap: 8px; }
.mem-role {
  font-style: normal;
  font-size: 9.5px;
  letter-spacing: .1em;
  color: var(--c1);
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  padding: 1px 6px;
  transition: color .8s, border-color .8s;
}
.mem-desc {
  font-size: 11px;
  line-height: 1.6;
  letter-spacing: .03em;
  color: #938a96;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mem-sub { font-size: 9.5px; letter-spacing: .1em; color: #5f5869; }
.mem-power {
  flex: none;
  font-family: Georgia, serif;
  font-size: 16px;
  color: var(--c1);
  text-shadow: 0 0 10px var(--c1-soft);
  transition: color .8s;
}

@media (max-width: 860px) {
  .member-list { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .page { padding: 24px 16px 50px; }
}
</style>
