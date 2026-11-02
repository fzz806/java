<script setup>
import { ref } from 'vue'
import { login } from '../api/auth.js'

const emit = defineEmits(['submit', 'error', 'go-register'])

const props = defineProps({
  // 从注册页回来时带回的真名
  initialUsername: { type: String, default: '' }
})

const username = ref(props.initialUsername)
const password = ref('')
const pending = ref(false)

async function onSubmit() {
  if (pending.value) return
  pending.value = true
  try {
    // TODO: 对接真实后端时，在 src/api/auth.js 中将 USE_MOCK 改为 false 即可
    const result = await login({ username: username.value, password: password.value })
    if (result.code === 0) {
      emit('submit', { username: username.value, data: result.data })
    } else {
      emit('error', result.message)
    }
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="altar">
    <!-- 红色神印 -->
    <div class="seal">
      <svg viewBox="0 0 100 100" fill="none">
        <path
          d="M50 8 L58 34 L86 34 L63 50 L72 78 L50 61 L28 78 L37 50 L14 34 L42 34 Z"
          stroke="var(--c1)"
          stroke-width="2.5"
          fill="none"
        />
        <circle cx="50" cy="50" r="6" fill="var(--c1)" />
      </svg>
    </div>

    <h2>神印认证</h2>
    <div class="sub">THE <b>RED</b> SEAL · 镇兽大比入场凭证</div>

    <form autocomplete="off" @submit.prevent="onSubmit">
      <div class="field">
        <label>真　名</label>
        <input v-model.trim="username" type="text" placeholder="被神堂封存的真名" required />
      </div>
      <div class="field">
        <label>神　印</label>
        <input v-model.trim="password" type="password" placeholder="烙入心脏的密语" required />
      </div>

      <div class="options">
        <label class="remember">
          <input type="checkbox" /> 铭记真名
        </label>
        <a href="javascript:void(0)" @click="alert('神印遗失？恶鬼之牙的仓库永远为你敞开。')">神印遗失？</a>
      </div>

      <button type="submit" class="ascend-btn" :disabled="pending">
        {{ pending ? '神 印 认 证 中 …' : '烙 印 登 场' }}
      </button>
    </form>

    <div class="switch-line">
      尚未觉醒？<a href="javascript:void(0)" @click="emit('go-register')">觉醒伪神天赋</a>
    </div>

    <div class="verse">
      「 神堂说：他这辈子只是个强壮点的凡人。<br />
      　镇兽大比那日，规则第一次裂开了缝。 」
    </div>
  </div>
</template>

<style scoped>
.altar {
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
}
:global(body.god-mode) .altar {
  background: linear-gradient(160deg, rgba(14, 17, 30, .93), rgba(4, 5, 10, .97));
}

.seal {
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
:global(body.god-mode) .seal {
  background: radial-gradient(circle, #0e1426, #04060d);
}
.seal svg {
  width: 70%;
  height: 70%;
  animation: sealSpin 14s linear infinite;
}
@keyframes sealSpin {
  to { transform: rotate(360deg); }
}

h2 {
  text-align: center;
  font-size: 23px;
  letter-spacing: .35em;
  text-indent: .35em;
  color: #e8e0d0;
  margin-top: 8px;
}
.sub {
  text-align: center;
  font-size: 11px;
  color: #6d6478;
  letter-spacing: .3em;
  margin: 8px 0 34px;
}
.sub b { color: var(--c1); font-weight: normal; transition: color .8s; }

.field { margin-bottom: 22px; }
.field label {
  display: block;
  font-size: 12px;
  letter-spacing: .4em;
  color: #9a8a55;
  margin-bottom: 9px;
}
.field input {
  width: 100%;
  background: rgba(0, 0, 0, .5);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 2px;
  padding: 13px 14px;
  color: #e8e0d0;
  font-size: 15px;
  font-family: inherit;
  letter-spacing: .1em;
  transition: border-color .3s, box-shadow .3s;
  outline: none;
}
.field input:focus {
  border-color: var(--c1);
  box-shadow: 0 0 16px var(--c1-soft), inset 0 0 10px var(--c1-soft);
}
.field input::placeholder { color: #565058; letter-spacing: .2em; }

.options {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #786f83;
  letter-spacing: .15em;
  margin-bottom: 28px;
}
.options a {
  color: #9d8fc0;
  text-decoration: none;
  border-bottom: 1px dotted rgba(157, 143, 192, .5);
}
.options a:hover { color: #cfc2ee; }
.remember { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.remember input { accent-color: var(--c1); cursor: pointer; }

.ascend-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #2e1113, #140608);
  border: 1px solid var(--c1);
  border-radius: 2px;
  color: var(--c1);
  font-family: inherit;
  font-size: 17px;
  letter-spacing: .55em;
  text-indent: .55em;
  cursor: pointer;
  transition: all .4s;
  text-shadow: 0 0 12px var(--c1-glow);
}
:global(body.god-mode) .ascend-btn {
  background: linear-gradient(135deg, #131c33, #060a14);
}
.ascend-btn:hover {
  background: var(--c1);
  color: #0a0508;
  text-shadow: none;
  box-shadow: 0 0 36px var(--c1-glow), 0 0 90px var(--c1-soft);
}
.ascend-btn:active { transform: scale(.985); }

.switch-line {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  color: #6d6478;
  letter-spacing: .2em;
}
.switch-line a { color: #9a8a55; text-decoration: none; }
.switch-line a:hover { color: var(--c1); }

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
