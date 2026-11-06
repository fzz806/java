<script setup>
import { ref, computed } from 'vue'
import { register } from '../api/auth.js'

const emit = defineEmits(['success', 'error', 'go-login'])

const props = defineProps({
  // 从登录页带过来的真名，方便用户
  initialUsername: { type: String, default: '' }
})

const username = ref(props.initialUsername)
const password = ref('')
const confirm = ref('')
const pending = ref(false)

// 神印强度示意：以烙印深浅命名
const sealStrength = computed(() => {
  const len = password.value.length
  if (!len) return { text: '', cls: '' }
  if (len < 6) return { text: '神印过浅 · 至少六字', cls: 'weak' }
  if (len < 10) return { text: '神印中等 · 可堪封神', cls: 'mid' }
  return { text: '神印深固 · 禁忌之列', cls: 'strong' }
})

const mismatch = computed(() => {
  return confirm.value.length > 0 && confirm.value !== password.value
})

async function onSubmit() {
  if (pending.value) return
  if (password.value !== confirm.value) {
    emit('error', '两次镌刻的神印不一致')
    return
  }
  pending.value = true
  try {
    // TODO: 对接真实后端时，在 src/api/auth.js 中将 USE_MOCK 改为 false 即可
    const result = await register({
      username: username.value,
      password: password.value
    })
    if (result.code === 0) {
      // 一并抛出神印，供上层自动登临（拿真 token）后进入首页
      emit('success', {
        username: result.data?.username || username.value,
        password: password.value
      })
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

    <h2>铸神台</h2>
    <div class="sub">FORGE <b>·</b> THE <b>·</b> SEAL · 自铸神印，自证神格</div>

    <form autocomplete="off" @submit.prevent="onSubmit">
      <div class="field">
        <label>真　名</label>
        <input
          v-model.trim="username"
          type="text"
          placeholder="取一个神堂没登记过的名字"
          minlength="2"
          maxlength="16"
          required
        />
      </div>
      <div class="field">
        <label>神　印</label>
        <input
          v-model.trim="password"
          type="password"
          placeholder="六字以上，烙入心脏"
          minlength="6"
          required
        />
        <div v-if="sealStrength.text" class="strength" :class="sealStrength.cls">
          {{ sealStrength.text }}
        </div>
      </div>
      <div class="field">
        <label>重　铸</label>
        <input
          v-model.trim="confirm"
          type="password"
          placeholder="再镌刻一遍，以免神印有误"
          required
        />
        <div v-if="mismatch" class="strength weak">两次镌刻不一致，神印将无法成形</div>
      </div>

      <button type="submit" class="ascend-btn" :disabled="pending || mismatch">
        {{ pending ? '神 印 铸 造 中 …' : '铸 印 封 神' }}
      </button>
    </form>

    <div class="switch-line">
      神印已成？<a href="javascript:void(0)" @click="emit('go-login')">回去登临神位</a>
    </div>

    <div class="verse">
      「 神堂登记不了的名字，<br />
      　才配得上不受诸神管辖的命运。 」
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
  padding: 46px 42px 36px;
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
  letter-spacing: .25em;
  margin: 8px 0 30px;
}
.sub b { color: var(--c1); font-weight: normal; transition: color .8s; }

.field { margin-bottom: 20px; }
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
  padding: 12px 14px;
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
.field input::placeholder { color: #565058; letter-spacing: .15em; }

.strength {
  margin-top: 8px;
  font-size: 11px;
  letter-spacing: .2em;
  transition: color .4s;
}
.strength.weak { color: #c05a50; }
.strength.mid { color: #9a8a55; }
.strength.strong { color: var(--c1); text-shadow: 0 0 8px var(--c1-glow); }

.ascend-btn {
  width: 100%;
  padding: 15px;
  margin-top: 4px;
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
.ascend-btn:hover:enabled {
  background: var(--c1);
  color: #0a0508;
  text-shadow: none;
  box-shadow: 0 0 36px var(--c1-glow), 0 0 90px var(--c1-soft);
}
.ascend-btn:active:enabled { transform: scale(.985); }
.ascend-btn:disabled {
  opacity: .45;
  cursor: not-allowed;
}

.switch-line {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: #6d6478;
  letter-spacing: .2em;
}
.switch-line a { color: #9a8a55; text-decoration: none; }
.switch-line a:hover { color: var(--c1); }

.verse {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, .08);
  text-align: center;
  font-size: 11px;
  color: #5c5560;
  letter-spacing: .22em;
  line-height: 1.9;
}
</style>
