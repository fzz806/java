<script setup>
import { ref, computed } from 'vue'
import { fetchFriends, addFriend, removeFriend } from '../api/social.js'

const props = defineProps({
  open: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'chat'])

/* ---------------- 好友名录 ---------------- */
const friends = ref([])
const loading = ref(false)
const loaded = ref(false)
const errorText = ref('')

async function loadFriends() {
  loading.value = true
  errorText.value = ''
  const r = await fetchFriends()
  loading.value = false
  if (r.code === 0) {
    friends.value = r.data || []
    loaded.value = true
  } else {
    errorText.value = r.message
  }
}

// 抽屉每次打开都刷新一次
function onEnter() {
  loadFriends()
}

/* ---------------- 结契（加好友） ---------------- */
const newName = ref('')
const adding = ref(false)
const addMsg = ref('') // '' | 成功提示 | 错误提示
const addOk = ref(false)

async function onAdd() {
  const name = newName.value.trim()
  if (!name || adding.value) return
  adding.value = true
  addMsg.value = ''
  const r = await addFriend(name)
  adding.value = false
  if (r.code === 0) {
    addOk.value = true
    addMsg.value = `已与「${r.data.username}」结契`
    newName.value = ''
    loadFriends()
  } else {
    addOk.value = false
    addMsg.value = r.message
  }
  setTimeout(() => (addMsg.value = ''), 3200)
}

/* ---------------- 解契（删好友） ---------------- */
const confirmingId = ref(0) // 二次确认中的好友 id

async function onRemove(f) {
  if (confirmingId.value !== f.friendId) {
    confirmingId.value = f.friendId
    setTimeout(() => {
      if (confirmingId.value === f.friendId) confirmingId.value = 0
    }, 3000)
    return
  }
  confirmingId.value = 0
  const r = await removeFriend(f.friendId)
  if (r.code === 0) {
    friends.value = friends.value.filter((x) => x.friendId !== f.friendId)
  } else {
    errorText.value = r.message
  }
}

/* ---------------- 展示名 ---------------- */
function displayName(f) {
  return f.remark || f.username
}

const countText = computed(() =>
  loaded.value ? `已结契 ${friends.value.length} 人` : ''
)
</script>

<template>
  <teleport to="body">
    <transition name="drawer">
      <div v-if="open" class="drawer-mask" @click.self="emit('close')">
        <aside class="drawer" @vue:mounted="onEnter">
          <header class="drawer-head">
            <div class="head-titles">
              <h3>好 友 名 录</h3>
              <span class="head-en">COVENANT · 结契名录</span>
            </div>
            <span class="head-count">{{ countText }}</span>
            <button class="close" aria-label="关闭" @click="emit('close')">✕</button>
          </header>

          <!-- 结契输入 -->
          <div class="add-row">
            <input
              v-model.trim="newName"
              type="text"
              maxlength="16"
              placeholder="输入对方真名，结为好友"
              @keyup.enter="onAdd"
            />
            <button class="add-btn" :disabled="!newName.trim() || adding" @click="onAdd">
              {{ adding ? '结契中…' : '结契' }}
            </button>
          </div>
          <transition name="fade">
            <div v-if="addMsg" class="add-msg" :class="{ ok: addOk }">{{ addMsg }}</div>
          </transition>

          <!-- 名录 -->
          <div class="list">
            <div v-if="loading && !loaded" class="hint">名录开启中…</div>
            <div v-else-if="errorText" class="hint err">{{ errorText }}</div>
            <div v-else-if="!friends.length" class="hint">
              名录空空如也<br />输入真名，与同道结契
            </div>

            <div v-for="f in friends" :key="f.friendId" class="friend">
              <span class="avatar">{{ displayName(f).slice(0, 1) }}</span>
              <div class="f-main">
                <span class="f-name">{{ displayName(f) }}</span>
                <span class="f-sub">
                  {{ f.remark ? `真名 · ${f.username}` : '同道中人' }}
                </span>
              </div>
              <button class="chat-btn" @click="emit('chat', f)">传音</button>
              <button
                class="del-btn"
                :class="{ confirming: confirmingId === f.friendId }"
                :title="confirmingId === f.friendId ? '再点一次确认解契' : '解契'"
                @click="onRemove(f)"
              >
                {{ confirmingId === f.friendId ? '确认解契？' : '解契' }}
              </button>
            </div>
          </div>

          <p class="drawer-foot">结契成双，解契成仇 · 神堂不管的名录</p>
        </aside>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 70;
  background: rgba(0, 0, 0, .5);
  backdrop-filter: blur(3px);
}
.drawer {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 380px;
  max-width: 92vw;
  display: flex;
  flex-direction: column;
  background: linear-gradient(200deg, rgba(22, 14, 18, .98), rgba(8, 4, 6, .99));
  border-left: 1px solid var(--c1-dim);
  box-shadow: -30px 0 80px rgba(0, 0, 0, .7), inset 0 0 60px var(--c1-soft);
  padding: 22px 22px 16px;
  transition: background .8s, border-color .8s;
}
:global(body.god-mode) .drawer {
  background: linear-gradient(200deg, rgba(13, 16, 30, .98), rgba(4, 5, 10, .99));
}

.drawer-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, .07);
}
.head-titles { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.head-titles h3 {
  font-size: 15px;
  font-weight: 400;
  letter-spacing: .3em;
  color: #ded5e4;
}
.head-en { font-size: 9px; letter-spacing: .22em; color: #5d5666; }
.head-count { font-size: 10px; letter-spacing: .12em; color: #9a8a55; }
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

/* 结契输入 */
.add-row {
  display: flex;
  gap: 8px;
  margin: 16px 0 4px;
}
.add-row input {
  flex: 1;
  min-width: 0;
  background: rgba(0, 0, 0, .5);
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 2px;
  padding: 10px 12px;
  color: #e8e0d0;
  font-size: 13px;
  font-family: inherit;
  letter-spacing: .08em;
  outline: none;
  transition: border-color .3s, box-shadow .3s;
}
.add-row input:focus {
  border-color: var(--c1);
  box-shadow: 0 0 14px var(--c1-soft);
}
.add-row input::placeholder { color: #565058; }
.add-btn {
  flex: none;
  padding: 10px 16px;
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
:global(body.god-mode) .add-btn {
  background: linear-gradient(135deg, #131c33, #060a14);
}
.add-btn:hover:enabled { background: var(--c1); color: #0a0508; }
.add-btn:disabled { opacity: .45; cursor: not-allowed; }

.add-msg {
  font-size: 11px;
  letter-spacing: .1em;
  color: #c05a50;
  padding: 4px 2px 0;
}
.add-msg.ok { color: #8fe07a; }

/* 名录 */
.list {
  flex: 1;
  overflow-y: auto;
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.hint {
  text-align: center;
  font-size: 12px;
  line-height: 2;
  letter-spacing: .18em;
  color: #6f6779;
  padding: 40px 0;
}
.hint.err { color: #c05a50; }

.friend {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, .07);
  border-radius: 2px;
  background: rgba(0, 0, 0, .28);
  transition: border-color .3s;
}
.friend:hover { border-color: var(--c1-dim); }
.avatar {
  flex: none;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--c1-dim);
  background: rgba(0, 0, 0, .5);
  color: var(--c1);
  font-size: 14px;
  text-shadow: 0 0 10px var(--c1-glow);
  transition: color .8s, border-color .8s;
}
.f-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.f-name {
  font-size: 13px;
  letter-spacing: .1em;
  color: #e8e0d0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.f-sub {
  font-size: 9.5px;
  letter-spacing: .1em;
  color: #5f5869;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-btn {
  flex: none;
  padding: 6px 12px;
  background: none;
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: var(--c1);
  font-family: inherit;
  font-size: 11px;
  letter-spacing: .14em;
  cursor: pointer;
  transition: all .3s;
}
.chat-btn:hover { background: var(--c1); color: #0a0508; }
.del-btn {
  flex: none;
  padding: 6px 8px;
  background: none;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 2px;
  color: #7f7689;
  font-family: inherit;
  font-size: 10.5px;
  letter-spacing: .06em;
  cursor: pointer;
  transition: all .3s;
}
.del-btn:hover, .del-btn.confirming { color: #ff6b6b; border-color: rgba(255, 77, 77, .4); }

.drawer-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(255, 255, 255, .06);
  text-align: center;
  font-size: 10px;
  letter-spacing: .18em;
  color: #5d5666;
}

/* 动画 */
.drawer-enter-active, .drawer-leave-active { transition: opacity .28s; }
.drawer-enter-active .drawer, .drawer-leave-active .drawer {
  transition: transform .28s cubic-bezier(.2, .8, .3, 1);
}
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from .drawer, .drawer-leave-to .drawer { transform: translateX(60px); }
.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
