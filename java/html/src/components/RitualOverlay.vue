<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  godMode: { type: Boolean, default: false },
  username: { type: String, default: '' }
})
const emit = defineEmits(['done'])

const demonSteps = [
  '正在烙入红色神印……',
  '恶鬼低吼，骨翼展开……',
  '神堂的规则，出现了裂缝……'
]
const godSteps = [
  '银纹浮现于额间……',
  '瞳孔化作星环……',
  '他人神技，尽归我用……'
]

const text = ref('')
const timer = ref(null)
const markSymbol = ref('卍')

function stop() {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
}

watch(
  () => props.visible,
  (visible) => {
    stop()
    if (!visible) return
    const steps = props.godMode ? godSteps : demonSteps
    markSymbol.value = props.godMode ? '✷' : '卍'
    let i = 0
    text.value = steps[0]
    timer.value = setInterval(() => {
      i++
      if (i < steps.length) {
        text.value = steps[i]
      } else {
        stop()
        text.value = `${props.username} · 神印烙成 · ${props.godMode ? '伪神形态' : '恶鬼形态'}登场`
        setTimeout(() => emit('done'), 1500)
      }
    }, 950)
  }
)

onBeforeUnmount(stop)
</script>

<template>
  <div class="ritual" :class="{ show: visible }">
    <div class="mark">{{ markSymbol }}</div>
    <p>{{ text }}</p>
  </div>
</template>

<style scoped>
.ritual {
  position: fixed;
  inset: 0;
  background: rgba(4, 2, 3, .96);
  z-index: 10;
  display: none;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 30px;
}
.ritual.show {
  display: flex;
  animation: fadeIn .6s;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.mark {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  border: 2px solid var(--c1);
  box-shadow: 0 0 40px var(--c1-glow), inset 0 0 30px var(--c1-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  color: var(--c1);
  animation: markPulse 1.4s ease-in-out infinite;
  transition: color .8s, border-color .8s;
}
@keyframes markPulse {
  0%, 100% { transform: scale(1);    box-shadow: 0 0 30px var(--c1-glow); }
  50%      { transform: scale(1.06); box-shadow: 0 0 60px var(--c1-glow), 0 0 100px var(--c1-soft); }
}

p {
  color: var(--c1);
  letter-spacing: .45em;
  text-indent: .45em;
  font-size: 15px;
  text-shadow: 0 0 14px var(--c1-glow);
  transition: color .8s;
}
</style>
