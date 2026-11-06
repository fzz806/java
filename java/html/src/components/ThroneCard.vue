<script setup>
import { computed } from 'vue'
import { loginDurationText } from '../api/session.js'

const props = defineProps({
  username: { type: String, default: '' },
  godMode: { type: Boolean, default: false },
  loginAt: { type: Number, default: 0 }
})
const emit = defineEmits(['logout', 'toggle-form'])

const greeting = computed(() => (props.godMode ? '伪神临世' : '恶鬼归位'))
const formText = computed(() => (props.godMode ? '第二变 · 伪神形态' : '第一变 · 恶鬼形态'))
const duration = computed(() => loginDurationText(props.loginAt))
</script>

<template>
  <div class="throne">
    <!-- 神印徽记 -->
    <div class="sigil">
      <svg viewBox="0 0 100 100" fill="none">
        <circle cx="50" cy="50" r="44" stroke="var(--c1)" stroke-width="1" opacity=".45" />
        <circle cx="50" cy="50" r="32" stroke="var(--c1)" stroke-width=".8" opacity=".3" stroke-dasharray="3 5" />
        <path
          d="M50 10 L58 36 L86 36 L63 52 L72 80 L50 63 L28 80 L37 52 L14 36 L42 36 Z"
          stroke="var(--c1)"
          stroke-width="2.2"
          fill="none"
        />
        <circle cx="50" cy="50" r="5" fill="var(--c1)" />
      </svg>
    </div>

    <div class="eyebrow">觉 醒 伪 神</div>
    <h2>{{ greeting }}</h2>
    <div class="motto">神明弃我<span class="sep">，</span>我自为神</div>

    <div class="panel">
      <div class="row">
        <span class="k">真　名</span>
        <span class="v">{{ username }}</span>
      </div>
      <div class="row">
        <span class="k">形　态</span>
        <span class="v">{{ formText }}</span>
      </div>
      <div class="row">
        <span class="k">天　赋</span>
        <span class="v">SSS 级 · 异能序列 伪神</span>
      </div>
      <div class="row" v-if="duration">
        <span class="k">状　态</span>
        <span class="v">{{ duration }}</span>
      </div>
    </div>

    <div class="actions">
      <button class="ghost-btn" @click="emit('toggle-form')">
        切换形态
      </button>
      <button class="ghost-btn danger" @click="emit('logout')">
        退出神位
      </button>
    </div>

    <div class="verse">
      「 神堂说：他这辈子只是个强壮点的凡人。<br />
      　镇兽大比那日，规则第一次裂开了缝。 」
    </div>
  </div>
</template>

<style scoped>
.throne {
  flex: 1;
  max-width: 430px;
  background: linear-gradient(160deg, rgba(20, 14, 18, .93), rgba(7, 4, 5, .97));
  border: 1px solid var(--c1-dim);
  border-radius: 4px;
  padding: 46px 42px 40px;
  position: relative;
  box-shadow: 0 30px 90px rgba(0, 0, 0, .75), inset 0 0 60px var(--c1-soft);
  backdrop-filter: blur(6px);
  transition: border-color .8s, box-shadow .8s, background .8s;
  text-align: center;
}
:global(body.god-mode) .throne {
  background: linear-gradient(160deg, rgba(14, 17, 30, .93), rgba(4, 5, 10, .97));
}

.sigil {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle, #2b0d0c, #120406);
  border: 1px solid var(--c1);
  box-shadow: 0 0 26px var(--c1-glow);
  transition: all .8s;
}
:global(body.god-mode) .sigil {
  background: radial-gradient(circle, #0e1426, #04060d);
}
.sigil svg {
  width: 76%;
  height: 76%;
  animation: sigilSpin 20s linear infinite;
}
@keyframes sigilSpin {
  to { transform: rotate(360deg); }
}

.eyebrow {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  letter-spacing: .5em;
  text-indent: .5em;
  color: var(--c1);
  border: 1px solid var(--c1-dim);
  padding: 5px 10px 5px 15px;
  text-shadow: 0 0 12px var(--c1-glow);
  transition: color .8s, border-color .8s;
}

h2 {
  margin-top: 18px;
  font-size: 28px;
  letter-spacing: .35em;
  text-indent: .35em;
  color: #e8e0d0;
  text-shadow: 0 0 22px var(--c1-glow);
}

.motto {
  margin-top: 12px;
  font-size: 14px;
  letter-spacing: .3em;
  text-indent: .3em;
  color: #a89c8a;
}
.motto .sep {
  color: var(--c1);
  text-shadow: 0 0 10px var(--c1-glow);
  transition: color .8s;
}

.panel {
  margin-top: 30px;
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 3px;
  padding: 6px 18px;
  background: rgba(0, 0, 0, .3);
  text-align: left;
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, .05);
  font-size: 13px;
  letter-spacing: .12em;
}
.row:last-child { border-bottom: none; }
.row .k { color: #7d7466; flex: none; }
.row .v {
  color: #d8cfbf;
  text-align: right;
  word-break: break-all;
}

.actions {
  margin-top: 26px;
  display: flex;
  gap: 12px;
}
.ghost-btn {
  flex: 1;
  padding: 12px;
  background: transparent;
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: #b9b1a3;
  font-family: inherit;
  font-size: 13px;
  letter-spacing: .3em;
  text-indent: .3em;
  cursor: pointer;
  transition: all .35s;
}
.ghost-btn:hover {
  border-color: var(--c1);
  color: var(--c1);
  box-shadow: 0 0 22px var(--c1-soft);
}
.ghost-btn.danger:hover {
  border-color: #ff4d4d;
  color: #ff6b6b;
  box-shadow: 0 0 22px rgba(255, 77, 77, .22);
}

.verse {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, .08);
  text-align: center;
  font-size: 11px;
  color: #5c5560;
  letter-spacing: .22em;
  line-height: 1.9;
}
</style>
