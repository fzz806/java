<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  godMode: { type: Boolean, default: false }
})

const canvasRef = ref(null)
let ctx = null
let rafId = 0
const dots = []

function resize(canvas) {
  canvas.width = innerWidth
  canvas.height = innerHeight
}

function initDots() {
  dots.length = 0
  const COUNT = Math.min(80, Math.floor(innerWidth / 16))
  for (let i = 0; i < COUNT; i++) {
    dots.push({
      x: Math.random() * innerWidth,
      y: Math.random() * innerHeight,
      r: Math.random() * 1.8 + 0.4,
      vy: -(Math.random() * 0.55 + 0.1),
      vx: (Math.random() - 0.5) * 0.3,
      alpha: Math.random() * 0.5 + 0.15
    })
  }
}

function tick() {
  if (!ctx) return
  // 恶鬼形态：红烬；伪神形态：星屑
  const rgb = props.godMode ? '158,203,255' : '255,80,64'
  const canvas = canvasRef.value
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  for (const p of dots) {
    p.y += p.vy
    p.x += p.vx
    if (p.y < -10) { p.y = canvas.height + 10; p.x = Math.random() * canvas.width }
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${rgb}, ${p.alpha})`
    ctx.shadowColor = `rgba(${rgb},.9)`
    ctx.shadowBlur = 8
    ctx.fill()
    ctx.shadowBlur = 0
  }
  rafId = requestAnimationFrame(tick)
}

const onResize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  resize(canvas)
  initDots()
}

onMounted(() => {
  const canvas = canvasRef.value
  ctx = canvas.getContext('2d')
  resize(canvas)
  initDots()
  rafId = requestAnimationFrame(tick)
  addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  removeEventListener('resize', onResize)
})

// 形态切换由 tick 内读取 props，无需额外 watch
watch(() => props.godMode, () => {})
</script>

<template>
  <canvas ref="canvasRef" class="rune-canvas"></canvas>
</template>

<style scoped>
.rune-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
