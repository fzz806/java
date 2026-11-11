<script setup>
import { ref } from 'vue'
import {
  loreTabs,
  realms,
  normalElements,
  specialElements,
  seals,
  protagonist,
  guildFactions,
  contests,
  powers
} from '../data/loreData.js'

const props = defineProps({
  // 是否已觉醒（登临 / 铸印后）。未觉醒时，标注 awake 的卷（神印谱）不可翻阅
  awakened: { type: Boolean, default: true }
})

const active = ref('realms')

// 当前卷是否需要觉醒，且尚未觉醒
function isLocked(tab) {
  return !!tab.awake && !props.awakened
}

function pickTab(tab) {
  if (isLocked(tab)) return
  active.value = tab.key
}
</script>

<template>
  <div class="lore-hall">
    <!-- 大厅题头 -->
    <header class="hall-head">
      <div class="head-left">
        <h2>万 神 大 厅</h2>
        <span class="hall-en">WORLD HALL · 世界观图鉴 · 七卷</span>
      </div>
      <span class="hall-note">登录即入厅 · 神堂未登记的知识都在这里</span>
    </header>

    <!-- 分类标签 -->
    <nav class="tabs">
      <button
        v-for="t in loreTabs"
        :key="t.key"
        class="tab"
        :class="{ active: active === t.key, locked: isLocked(t) }"
        @click="pickTab(t)"
      >
        <span class="t-cn">{{ t.label }}</span>
        <span class="t-en">{{ t.en }}</span>
        <span v-if="t.awake" class="t-lock">{{ props.awakened ? '觉醒解锁' : '未觉醒' }}</span>
      </button>
    </nav>

    <!-- ================= 卷一 · 修为等级 ================= -->
    <div v-if="active === 'realms'" class="panel">
      <div class="ladder">
        <template v-for="(r, i) in realms" :key="r.name">
          <div class="step" :class="{ apex: r.mark }">
            <span class="step-tier">{{ r.tier }}</span>
            <span class="step-name">{{ r.name }}</span>
            <span v-if="r.mark" class="step-mark">{{ r.mark }}</span>
            <span v-else class="step-desc">{{ r.desc }}</span>
          </div>
          <span v-if="i < realms.length - 1" class="arrow">→</span>
        </template>
      </div>
      <p class="panel-foot">凡人拾级而上，八印锁天，半神临门——真神之位，神堂不给，自己拿。</p>
    </div>

    <!-- ================= 卷二 · 普通元素（六系） ================= -->
    <div v-else-if="active === 'normal'" class="panel">
      <div class="sub-title">普通元素 · 六系</div>
      <div class="elem-grid">
        <div
          v-for="e in normalElements"
          :key="e.name"
          class="elem-chip"
          :style="{ '--e': e.color }"
        >
          <span class="e-orb"></span>
          <span class="e-name">{{ e.name }}</span>
          <span class="e-en">{{ e.en }}</span>
          <span class="e-desc">{{ e.desc }}</span>
        </div>
      </div>
      <p class="panel-foot">火冰雷风土水，六系立世，寻常修士一生的全部可能。</p>
    </div>

    <!-- ================= 卷三 · 特殊系（六路） ================= -->
    <div v-else-if="active === 'special'" class="panel">
      <div class="sub-title">特殊系 · 六路</div>
      <div class="sp-grid">
        <div
          v-for="s in specialElements"
          :key="s.name"
          class="sp-card"
          :style="{ '--e': s.color }"
        >
          <div class="sp-top">
            <span class="sp-name">{{ s.name }}</span>
            <span class="sp-tag" :class="{ rare: s.rare }">{{ s.tag }}</span>
          </div>
          <p class="sp-desc">{{ s.desc }}</p>
        </div>
      </div>
      <p class="panel-foot">男主【伪神】不属任何一系——他靠神魔权柄作战。</p>
    </div>

    <!-- ================= 卷四 · 神印谱（觉醒后解锁） ================= -->
    <div v-else-if="active === 'seals'" class="panel">
      <template v-if="props.awakened">
        <div class="seal-grid">
          <div v-for="s in seals" :key="s.name" class="seal-card" :style="{ '--s': s.color, '--s2': s.sub }">
            <div class="seal-orb" aria-hidden="true"></div>
            <div class="seal-info">
              <div class="seal-top">
                <span class="seal-name">{{ s.name }}</span>
                <span class="seal-tag" :class="{ rare: s.rare }">{{ s.tag }}</span>
              </div>
              <p class="seal-desc">{{ s.desc }}</p>
            </div>
          </div>
        </div>
        <p class="panel-foot">神堂只认金白，其余皆是野种——可红印偏偏长在了男主心上。</p>
      </template>

      <!-- 未觉醒：封印态 -->
      <div v-else class="seal-locked">
        <span class="lock-mark">✕</span>
        <p class="lock-title">此 卷 尚 未 解 封</p>
        <p class="lock-desc">神印颜色与属性，需觉醒（登临神位 / 铸印封神）后方可翻阅。</p>
      </div>
    </div>

    <!-- ================= 卷五 · 伪神核心 ================= -->
    <div v-else-if="active === 'protagonist'" class="panel">
      <div class="talent-card">
        <div class="talent-row">
          <span class="talent-grade">SSS</span>
          <div class="talent-main">
            <span class="talent-name">{{ protagonist.talent }}</span>
            <span class="talent-desc">{{ protagonist.talentDesc }}</span>
          </div>
          <span class="talent-motto">{{ protagonist.motto }}</span>
        </div>
      </div>

      <div class="sub-title">神魔四变</div>
      <div class="tr-grid">
        <div v-for="t in protagonist.transforms" :key="t.name" class="tr-card" :style="{ '--t': t.color }">
          <div class="tr-head">
            <span class="tr-name">{{ t.name }}</span>
            <span class="tr-en">{{ t.en }}</span>
          </div>
          <p class="tr-desc">{{ t.desc }}</p>
        </div>
      </div>

      <div class="sub-title">核心能力</div>
      <div class="core-chips">
        <span v-for="c in protagonist.coreAbilities" :key="c" class="core-chip">{{ c }}</span>
      </div>
    </div>

    <!-- ================= 卷六 · 势力地点（公会 / 赛事） ================= -->
    <div v-else-if="active === 'factions'" class="panel">
      <div class="sub-title">公会 &amp; 阵营</div>
      <div class="fac-grid">
        <div v-for="f in guildFactions" :key="f.name" class="fac-card" :style="{ '--e': f.color }">
          <div class="fac-top">
            <span class="fac-name">{{ f.name }}</span>
            <span class="fac-tag">{{ f.stance }} · {{ f.tag }}</span>
          </div>
          <p class="fac-desc">{{ f.desc }}</p>
        </div>
      </div>

      <div class="sub-title">试炼 &amp; 赛事</div>
      <div class="fac-grid">
        <div v-for="f in contests" :key="f.name" class="fac-card" :style="{ '--e': f.color }">
          <div class="fac-top">
            <span class="fac-name">{{ f.name }}</span>
            <span class="fac-tag">{{ f.stance }} · {{ f.tag }}</span>
          </div>
          <p class="fac-desc">{{ f.desc }}</p>
        </div>
      </div>
    </div>

    <!-- ================= 卷七 · 特殊力量 ================= -->
    <div v-else class="panel">
      <div class="fac-grid">
        <div v-for="p in powers" :key="p.name" class="fac-card" :style="{ '--e': p.color }">
          <div class="fac-top">
            <span class="fac-name">{{ p.name }}</span>
            <span class="fac-tag">{{ p.tag }}</span>
          </div>
          <p class="fac-desc">{{ p.desc }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lore-hall {
  margin-top: 26px;
  padding: 22px 24px 26px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .78), rgba(7, 4, 5, .84));
  transition: border-color .4s, background .8s;
}
:global(body.god-mode) .lore-hall {
  background: linear-gradient(160deg, rgba(14, 17, 30, .78), rgba(4, 5, 10, .84));
}

/* ---------- 题头 ---------- */
.hall-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, .06);
}
.hall-head h2 {
  font-size: 14px;
  font-weight: 400;
  letter-spacing: .3em;
  color: #ded5e4;
  position: relative;
  padding-left: 12px;
}
.hall-head h2::before {
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
.hall-en {
  margin-left: 14px;
  font-size: 9.5px;
  letter-spacing: .22em;
  color: #5d5666;
}
.hall-note {
  font-size: 10.5px;
  letter-spacing: .14em;
  color: #6f6779;
}

/* ---------- 标签页 ---------- */
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 20px;
}
.tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  padding: 8px 16px 9px;
  background: rgba(0, 0, 0, .3);
  border: 1px solid rgba(255, 255, 255, .09);
  border-radius: 2px;
  color: #9d93a4;
  font-family: inherit;
  cursor: pointer;
  transition: all .3s;
}
.tab .t-cn { font-size: 12.5px; letter-spacing: .18em; }
.tab .t-en { font-size: 8px; letter-spacing: .2em; color: #57505e; }
.tab:hover { color: #d8cfbf; border-color: rgba(255, 255, 255, .2); }
.tab.active {
  color: var(--c1);
  border-color: var(--c1-dim);
  box-shadow: 0 0 14px var(--c1-soft), inset 0 0 12px var(--c1-soft);
}
.tab.active .t-en { color: var(--c1-dim); }

/* 觉醒解锁标识 */
.tab .t-lock {
  margin-top: 3px;
  padding: 1px 6px;
  font-size: 8px;
  letter-spacing: .18em;
  color: var(--c1);
  border: 1px solid var(--c1-dim);
  border-radius: 1px;
  transition: color .4s, border-color .4s;
}
.tab.locked {
  cursor: not-allowed;
  opacity: .55;
  filter: grayscale(.5);
}
.tab.locked:hover { color: #9d93a4; border-color: rgba(255, 255, 255, .09); }
.tab.locked .t-lock { color: #6d6478; border-color: #3a3342; }

/* 未觉醒封印态 */
.seal-locked {
  padding: 46px 20px 50px;
  text-align: center;
  border: 1px dashed rgba(255, 255, 255, .12);
  border-radius: 3px;
  background: repeating-linear-gradient(
    45deg,
    rgba(255, 255, 255, .015),
    rgba(255, 255, 255, .015) 8px,
    transparent 8px,
    transparent 16px
  );
}
.seal-locked .lock-mark {
  display: inline-block;
  font-size: 26px;
  color: var(--c1);
  text-shadow: 0 0 18px var(--c1-glow);
  margin-bottom: 12px;
}
.seal-locked .lock-title {
  font-size: 14px;
  letter-spacing: .4em;
  text-indent: .4em;
  color: #b9b1a3;
  margin-bottom: 10px;
}
.seal-locked .lock-desc {
  font-size: 11.5px;
  letter-spacing: .16em;
  line-height: 1.9;
  color: #6d6478;
}

/* ---------- 面板通用 ---------- */
.panel { animation: loreIn .45s ease both; }
@keyframes loreIn {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: none; }
}
.sub-title {
  margin: 4px 0 12px;
  font-size: 11px;
  letter-spacing: .3em;
  color: #9a8a55;
}
.sub-title:not(:first-child) { margin-top: 24px; }
.panel-foot {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed rgba(255, 255, 255, .06);
  font-size: 11px;
  letter-spacing: .16em;
  line-height: 1.9;
  color: #5d5666;
}

/* ---------- 卷一 · 修为阶梯 ---------- */
.ladder {
  display: flex;
  align-items: stretch;
  flex-wrap: wrap;
  gap: 6px;
}
.step {
  flex: 1 1 92px;
  min-width: 92px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 12px 13px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: border-color .3s, box-shadow .3s, transform .3s;
}
.step:hover { border-color: var(--c1-dim); transform: translateY(-2px); }
.step.apex {
  border-color: var(--c1);
  box-shadow: inset 0 0 20px var(--c1-soft), 0 0 14px var(--c1-soft);
}
.step-tier {
  font-size: 9.5px;
  letter-spacing: .2em;
  color: #5d5666;
}
.step-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: .1em;
  color: #e8e0d0;
}
.step.apex .step-name { color: var(--c1); text-shadow: 0 0 12px var(--c1-glow); }
.step-mark {
  font-size: 9.5px;
  letter-spacing: .1em;
  color: var(--c1);
  text-shadow: 0 0 8px var(--c1-soft);
}
.step-desc {
  font-size: 9.5px;
  letter-spacing: .04em;
  line-height: 1.6;
  color: #6f6779;
}
.arrow {
  align-self: center;
  flex: none;
  color: #453f4e;
  font-size: 12px;
}

/* ---------- 卷二 · 元素 ---------- */
.elem-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}
.elem-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 14px 8px 12px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: border-color .3s, box-shadow .3s, transform .3s;
}
.elem-chip:hover {
  border-color: var(--e);
  box-shadow: 0 0 16px color-mix(in srgb, var(--e) 35%, transparent);
  transform: translateY(-2px);
}
.e-orb {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #fff8, var(--e) 55%, #000);
  box-shadow: 0 0 14px var(--e);
}
.e-name { font-size: 15px; font-weight: 700; letter-spacing: .1em; color: #e8e0d0; }
.e-en { font-size: 8px; letter-spacing: .18em; color: #57505e; }
.e-desc {
  font-size: 9.5px;
  line-height: 1.6;
  color: #6f6779;
  text-align: center;
}
.sp-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.sp-card {
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-left: 2px solid var(--e);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: box-shadow .3s, transform .3s;
}
.sp-card:hover {
  box-shadow: 0 0 16px color-mix(in srgb, var(--e) 30%, transparent);
  transform: translateY(-2px);
}
.sp-top { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; }
.sp-name { font-size: 13px; letter-spacing: .1em; color: var(--e); }
.sp-tag { font-size: 9.5px; letter-spacing: .1em; color: #5f5869; }
.sp-tag.rare { color: #e0a86a; }
.sp-desc {
  margin-top: 7px;
  font-size: 10.5px;
  line-height: 1.7;
  letter-spacing: .04em;
  color: #938a96;
}

/* ---------- 卷三 · 神印谱 ---------- */
.seal-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.seal-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: box-shadow .3s, transform .3s;
}
.seal-card:hover {
  box-shadow: 0 0 20px color-mix(in srgb, var(--s) 40%, transparent);
  transform: translateX(3px);
}
.seal-orb {
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #fff6, var(--s) 52%, var(--s2));
  border: 1px solid var(--s);
  box-shadow: 0 0 18px var(--s);
  animation: orbPulse 3.2s ease-in-out infinite;
}
@keyframes orbPulse {
  0%, 100% { box-shadow: 0 0 10px var(--s); }
  50%      { box-shadow: 0 0 22px var(--s); }
}
.seal-info { min-width: 0; }
.seal-top { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; }
.seal-name { font-size: 14px; font-weight: 700; letter-spacing: .12em; color: var(--s); }
.seal-tag {
  font-size: 9.5px;
  letter-spacing: .12em;
  color: #9d93a4;
  border: 1px solid rgba(255, 255, 255, .14);
  border-radius: 2px;
  padding: 1px 7px;
}
.seal-tag.rare {
  color: #ff8f8f;
  border-color: rgba(255, 77, 77, .4);
  background: rgba(255, 77, 77, .08);
}
.seal-desc {
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.7;
  letter-spacing: .04em;
  color: #938a96;
}

/* ---------- 卷四 · 伪神核心 ---------- */
.talent-card {
  padding: 18px 20px;
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  background: rgba(0, 0, 0, .34);
  box-shadow: inset 0 0 30px var(--c1-soft);
  transition: border-color .8s, box-shadow .8s;
}
.talent-row {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}
.talent-grade {
  flex: none;
  font-size: 30px;
  font-weight: 900;
  font-family: Georgia, serif;
  color: var(--c1);
  text-shadow: 0 0 20px var(--c1-glow);
  letter-spacing: .04em;
}
.talent-main { flex: 1; min-width: 200px; display: flex; flex-direction: column; gap: 5px; }
.talent-name { font-size: 14px; letter-spacing: .16em; color: #e8e0d0; font-weight: 700; }
.talent-desc {
  font-size: 11px;
  line-height: 1.8;
  letter-spacing: .05em;
  color: #938a96;
}
.talent-motto {
  flex: none;
  font-size: 11px;
  letter-spacing: .2em;
  color: var(--c1);
  text-shadow: 0 0 10px var(--c1-glow);
  border: 1px dashed var(--c1-dim);
  padding: 7px 12px;
  transition: color .8s, border-color .8s;
}
.tr-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.tr-card {
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-top: 2px solid var(--t);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: box-shadow .3s, transform .3s;
}
.tr-card:hover {
  box-shadow: 0 0 16px color-mix(in srgb, var(--t) 35%, transparent);
  transform: translateY(-2px);
}
.tr-head { display: flex; justify-content: space-between; align-items: baseline; }
.tr-name { font-size: 14px; font-weight: 700; letter-spacing: .14em; color: var(--t); }
.tr-en { font-size: 8px; letter-spacing: .18em; color: #57505e; }
.tr-desc {
  margin-top: 7px;
  font-size: 10.5px;
  line-height: 1.7;
  letter-spacing: .04em;
  color: #938a96;
}
.core-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.core-chip {
  padding: 7px 14px;
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  background: rgba(0, 0, 0, .3);
  font-size: 11.5px;
  letter-spacing: .14em;
  color: var(--c1);
  text-shadow: 0 0 8px var(--c1-soft);
  transition: color .8s, border-color .8s, box-shadow .3s;
}
.core-chip:hover { box-shadow: 0 0 14px var(--c1-soft); }

/* ---------- 卷五 / 卷六 · 势力与力量 ---------- */
.fac-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.fac-card {
  padding: 15px 18px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-left: 2px solid var(--e);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: box-shadow .3s, transform .3s;
}
.fac-card:hover {
  box-shadow: 0 0 16px color-mix(in srgb, var(--e) 30%, transparent);
  transform: translateY(-2px);
}
.fac-top { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.fac-name { font-size: 13.5px; font-weight: 700; letter-spacing: .12em; color: var(--e); }
.fac-tag { font-size: 9.5px; letter-spacing: .12em; color: #6f6779; }
.fac-desc {
  margin-top: 8px;
  font-size: 11px;
  line-height: 1.8;
  letter-spacing: .04em;
  color: #938a96;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1080px) {
  .elem-grid { grid-template-columns: repeat(3, 1fr); }
  .sp-grid { grid-template-columns: repeat(2, 1fr); }
  .tr-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .lore-hall { padding: 18px 16px 20px; }
  .elem-grid { grid-template-columns: repeat(2, 1fr); }
  .sp-grid, .tr-grid, .fac-grid { grid-template-columns: 1fr; }
  .step { flex: 1 1 78px; min-width: 78px; }
  .arrow { display: none; }
  .hall-note { display: none; }
}
</style>
