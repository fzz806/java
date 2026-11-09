<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { fetchMessages, sendMessage } from '../api/social.js'

const props = defineProps({
  /** 打开的面板信息：{ friendId, username, remark } */
  friend: { type: Object, default: null }
})

const emit = defineEmits(['close'])

/* ---------------- 传音记录 ---------------- */
const messages = ref([]) // [{ id, mine, content, time }]
const loading = ref(true)
const errorText = ref('')

// 增量轮询：记录本地最大消息 id，每次只拉新消息
let lastId = 0
let pollTimer = 0
const POLL_MS = 3000

const displayName = () => (props.friend ? props.friend.remark || props.friend.username : '')

async function pull(first = false) {
  if (!props.friend) return
  const r = await fetchMessages(props.friend.friendId, first ? 0 : lastId)
  if (r.code === 0) {
    const list = r.data || []
    errorText.value = ''
    if (list.length) {
      for (const m of list) {
        messages.value.push(m)
        if (m.id > lastId) lastId = m.id
      }
      scrollBottom()
    }
    loading.value = false
  } else if (first) {
    errorText.value = r.message
    loading.value = false
  }
}

function startPolling() {
  stopPolling()
  lastId = 0
  messages.value = []
  loading.value = true
  pull(true)
  pollTimer = setInterval(() => pull(false), POLL_MS)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = 0
  }
}

// 由父组件的 v-if 挂载触发
function onMounted() {
  startPolling()
}

onBeforeUnmount(stopPolling)

/* ---------------- 发送 ---------------- */
const input = ref('')
const sending = ref(false)
const listEl = ref(null)

async function onSend() {
  const text = input.value.trim()
  if (!text || sending.value || !props.friend) return
  sending.value = true
  const r = await sendMessage(props.friend.friendId, text)
  sending.value = false
  if (r.code === 0) {
    input.value = ''
    // 立即本地回显 + 调 lastId，不等下一轮轮询
    messages.value.push({
      id: r.data.id,
      mine: true,
      content: r.data.content,
      time: new Date().toISOString()
    })
    lastId = Math.max(lastId, r.data.id)
    scrollBottom()
  } else {
    errorText.value = r.message
    setTimeout(() => (errorText.value = ''), 3000)
  }
}

function scrollBottom() {
  nextTick(() => {
    const el = listEl.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

/* ---------------- 时间显示 ---------------- */
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<template>
  <teleport to="body">
    <transition name="chat">
      <div v-if="friend" class="chat-mask" @vue:mounted="onMounted" @click.self="emit('close')">
        <section class="chat-panel">
          <header class="chat-head">
            <span class="avatar">{{ displayName().slice(0, 1) }}</span>
            <div class="head-main">
              <span class="head-name">{{ displayName() }}</span>
              <span class="head-sub">传音密道 · 每三息刷新一次</span>
            </div>
            <button class="close" aria-label="关闭" @click="emit('close')">✕</button>
          </header>

          <!-- 消息区 -->
          <div ref="listEl" class="msg-list">
            <div v-if="loading" class="hint">密道开启中…</div>
            <div v-else-if="errorText" class="hint err">{{ errorText }}</div>
            <div v-else-if="!messages.length" class="hint">
              密道已通，尚未传音<br />第一句话，往往最难开口
            </div>

            <div
              v-for="m in messages"
              :key="m.id"
              class="msg-row"
              :class="{ mine: m.mine }"
            >
              <div class="bubble">
                <div class="content">{{ m.content }}</div>
                <div class="time">{{ fmtTime(m.time) }}</div>
              </div>
            </div>
          </div>

          <!-- 输入区 -->
          <footer class="chat-foot">
            <textarea
              v-model="input"
              rows="2"
              maxlength="500"
              placeholder="写下传音内容…（Enter 发送，Shift+Enter 换行）"
              @keydown.enter.exact.prevent="onSend"
            ></textarea>
            <button class="send-btn" :disabled="!input.trim() || sending" @click="onSend">
              {{ sending ? '传音中…' : '传 音' }}
            </button>
          </footer>
        </section>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.chat-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(0, 0, 0, .55);
  backdrop-filter: blur(4px);
}
.chat-panel {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 640px;
  max-width: 94vw;
  height: 640px;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(200deg, rgba(22, 14, 18, .98), rgba(8, 4, 6, .99));
  border: 1px solid var(--c1-dim);
  border-radius: 3px;
  box-shadow: 0 40px 120px rgba(0, 0, 0, .8), inset 0 0 60px var(--c1-soft);
  transition: background .8s, border-color .8s;
}
:global(body.god-mode) .chat-panel {
  background: linear-gradient(200deg, rgba(13, 16, 30, .98), rgba(4, 5, 10, .99));
}

.chat-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
}
.avatar {
  flex: none;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--c1-dim);
  background: rgba(0, 0, 0, .5);
  color: var(--c1);
  font-size: 16px;
  text-shadow: 0 0 10px var(--c1-glow);
  transition: color .8s, border-color .8s;
}
.head-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.head-name { font-size: 15px; letter-spacing: .14em; color: #e8e0d0; }
.head-sub { font-size: 9.5px; letter-spacing: .14em; color: #5f5869; }
.close {
  flex: none;
  width: 30px;
  height: 30px;
  background: none;
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 2px;
  color: #a99eb0;
  font-size: 13px;
  cursor: pointer;
  transition: all .3s;
}
.close:hover { color: var(--c1); border-color: var(--c1-dim); }

/* 消息区 */
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hint {
  text-align: center;
  font-size: 12px;
  line-height: 2;
  letter-spacing: .18em;
  color: #6f6779;
  margin: auto 0;
}
.hint.err { color: #c05a50; }

.msg-row { display: flex; }
.msg-row.mine { justify-content: flex-end; }
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 2px;
  border: 1px solid rgba(255, 255, 255, .1);
  background: rgba(0, 0, 0, .34);
}
.msg-row.mine .bubble {
  border-color: var(--c1-dim);
  background: rgba(43, 13, 12, .5);
}
:global(body.god-mode) .msg-row.mine .bubble {
  background: rgba(14, 20, 38, .5);
}
.content {
  font-size: 13px;
  line-height: 1.7;
  letter-spacing: .04em;
  color: #ded5e4;
  word-break: break-word;
  white-space: pre-wrap;
}
.msg-row.mine .content { color: #f2e8dc; }
.time {
  margin-top: 5px;
  font-size: 9px;
  letter-spacing: .1em;
  color: #57505e;
  text-align: right;
}

/* 输入区 */
.chat-foot {
  display: flex;
  gap: 10px;
  padding: 14px 20px 16px;
  border-top: 1px solid rgba(255, 255, 255, .08);
}
.chat-foot textarea {
  flex: 1;
  resize: none;
  background: rgba(0, 0, 0, .5);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 2px;
  padding: 10px 12px;
  color: #e8e0d0;
  font-size: 13px;
  font-family: inherit;
  line-height: 1.6;
  letter-spacing: .04em;
  outline: none;
  transition: border-color .3s, box-shadow .3s;
}
.chat-foot textarea:focus {
  border-color: var(--c1);
  box-shadow: 0 0 14px var(--c1-soft);
}
.chat-foot textarea::placeholder { color: #565058; }
.send-btn {
  flex: none;
  align-self: stretch;
  padding: 0 22px;
  background: linear-gradient(135deg, #2e1113, #140608);
  border: 1px solid var(--c1);
  border-radius: 2px;
  color: var(--c1);
  font-family: inherit;
  font-size: 13px;
  letter-spacing: .2em;
  cursor: pointer;
  transition: all .3s;
}
:global(body.god-mode) .send-btn {
  background: linear-gradient(135deg, #131c33, #060a14);
}
.send-btn:hover:enabled { background: var(--c1); color: #0a0508; }
.send-btn:disabled { opacity: .45; cursor: not-allowed; }

/* 动画 */
.chat-enter-active, .chat-leave-active { transition: opacity .25s; }
.chat-enter-active .chat-panel, .chat-leave-active .chat-panel {
  transition: transform .25s, opacity .25s;
}
.chat-enter-from, .chat-leave-to { opacity: 0; }
.chat-enter-from .chat-panel, .chat-leave-to .chat-panel {
  opacity: 0;
  transform: translate(-50%, -46%) scale(.98);
}
</style>
