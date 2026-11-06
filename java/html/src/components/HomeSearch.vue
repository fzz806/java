<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { searchIndex, hotKeywords } from '../data/homeData.js'

const emit = defineEmits(['pick'])

const keyword = ref('')
const focused = ref(false)
const activeIndex = ref(-1)
const inputRef = ref(null)
const rootRef = ref(null)

const trimmed = computed(() => keyword.value.trim())

/** 关键词命中过滤：名称、描述、类型都参与匹配 */
const results = computed(() => {
  const kw = trimmed.value.toLowerCase()
  if (!kw) {
    // 未输入时展示推荐（神技 + 赛程各取几条）
    return searchIndex.filter((it) => it.type === '神技' || it.type === '赛程').slice(0, 6)
  }
  return searchIndex
    .filter((it) => {
      return (
        it.name.toLowerCase().includes(kw) ||
        it.desc.toLowerCase().includes(kw) ||
        it.type.toLowerCase().includes(kw)
      )
    })
    .slice(0, 8)
})

const panelOpen = computed(() => focused.value)

/** 把命中的词切出来做高亮，避免 v-html 注入 */
function parts(text) {
  const kw = trimmed.value
  if (!kw) return [{ t: text, hit: false }]
  const idx = text.toLowerCase().indexOf(kw.toLowerCase())
  if (idx < 0) return [{ t: text, hit: false }]
  return [
    { t: text.slice(0, idx), hit: false },
    { t: text.slice(idx, idx + kw.length), hit: true },
    { t: text.slice(idx + kw.length), hit: false }
  ].filter((p) => p.t)
}

function useHot(word) {
  keyword.value = word
  activeIndex.value = -1
  inputRef.value && inputRef.value.focus()
}

function pick(item) {
  if (!item) return
  emit('pick', item)
  focused.value = false
  activeIndex.value = -1
}

function onSubmit() {
  if (!results.value.length) {
    emit('pick', { type: '空', name: trimmed.value, desc: '神堂档案里没有这一条', level: '—' })
    return
  }
  pick(results.value[Math.max(activeIndex.value, 0)])
}

function onKeydown(e) {
  if (e.key === 'Escape') {
    focused.value = false
    inputRef.value && inputRef.value.blur()
    return
  }
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault()
    if (!results.value.length) return
    const step = e.key === 'ArrowDown' ? 1 : -1
    const next = activeIndex.value + step
    activeIndex.value = (next + results.value.length) % results.value.length
    return
  }
  if (e.key === 'Enter') {
    e.preventDefault()
    onSubmit()
  }
}

/** 点击面板外部时收起 */
function onDocClick(e) {
  if (rootRef.value && !rootRef.value.contains(e.target)) {
    focused.value = false
    activeIndex.value = -1
  }
}

onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))

defineExpose({
  focus() {
    inputRef.value && inputRef.value.focus()
  }
})
</script>

<template>
  <div ref="rootRef" class="search" :class="{ open: panelOpen }">
    <form class="bar" @submit.prevent="onSubmit">
      <span class="icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <circle cx="10.5" cy="10.5" r="6.5" stroke="currentColor" stroke-width="1.6" />
          <path d="M15.6 15.6 L21 21" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        </svg>
      </span>

      <input
        ref="inputRef"
        v-model="keyword"
        type="search"
        class="input"
        placeholder="搜索神技、镇兽、公会成员或赛程…"
        aria-label="站内搜索"
        autocomplete="off"
        @focus="focused = true"
        @keydown="onKeydown"
      />

      <button v-if="keyword" type="button" class="clear" aria-label="清空" @click="keyword = ''">
        ✕
      </button>

      <button type="submit" class="submit">搜 索</button>
    </form>

    <!-- 热词 -->
    <div class="hots">
      <span class="hots-label">热词</span>
      <button v-for="w in hotKeywords" :key="w" class="hot" @click="useHot(w)">{{ w }}</button>
    </div>

    <!-- 结果面板 -->
    <transition name="panel">
      <div v-if="panelOpen" class="panel">
        <div class="panel-head">
          <span v-if="trimmed">检索「{{ trimmed }}」· 命中 {{ results.length }} 条</span>
          <span v-else>神堂推荐 · 你可能在找</span>
          <span class="hint">↑↓ 选择　Enter 确认　Esc 关闭</span>
        </div>

        <ul v-if="results.length" class="list">
          <li
            v-for="(item, i) in results"
            :key="item.type + item.name"
            class="item"
            :class="{ active: activeIndex === i }"
            @mouseenter="activeIndex = i"
            @click="pick(item)"
          >
            <span class="type" :data-type="item.type">{{ item.type }}</span>
            <span class="main">
              <span class="name">
                <template v-for="(p, k) in parts(item.name)" :key="k">
                  <mark v-if="p.hit">{{ p.t }}</mark>
                  <template v-else>{{ p.t }}</template>
                </template>
              </span>
              <span class="desc">{{ item.desc }}</span>
            </span>
            <span class="level">{{ item.level }}</span>
          </li>
        </ul>

        <div v-else class="empty">
          神堂档案中未收录「{{ trimmed }}」<br />
          <span>试试「骨翼」「星环瞳」「镇兽大比」</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.search {
  position: relative;
  width: 100%;
  max-width: 720px;
}

/* ---------- 输入条 ---------- */
.bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 6px 6px 16px;
  background: linear-gradient(160deg, rgba(26, 17, 20, .92), rgba(9, 5, 6, .94));
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 3px;
  box-shadow: 0 16px 50px rgba(0, 0, 0, .55), inset 0 0 34px rgba(255, 255, 255, .015);
  transition: border-color .35s, box-shadow .35s, background .8s;
}
:global(body.god-mode) .bar {
  background: linear-gradient(160deg, rgba(16, 20, 34, .92), rgba(5, 6, 12, .94));
}
.search.open .bar,
.bar:focus-within {
  border-color: var(--c1);
  box-shadow: 0 16px 50px rgba(0, 0, 0, .6), 0 0 26px var(--c1-soft), inset 0 0 22px var(--c1-soft);
}

.icon {
  flex: none;
  width: 18px;
  height: 18px;
  color: #7c7387;
  transition: color .3s;
}
.icon svg { width: 100%; height: 100%; display: block; }
.bar:focus-within .icon { color: var(--c1); }

.input {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: none;
  outline: none;
  color: #e8e0d0;
  font-family: inherit;
  font-size: 14.5px;
  letter-spacing: .1em;
  padding: 12px 0;
}
.input::placeholder { color: #5d5666; letter-spacing: .12em; }
.input::-webkit-search-cancel-button { display: none; }

.clear {
  flex: none;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, .06);
  color: #8a8291;
  font-size: 11px;
  cursor: pointer;
  transition: all .25s;
}
.clear:hover { background: rgba(255, 77, 77, .16); color: #ff8f8f; }

.submit {
  flex: none;
  padding: 11px 22px;
  background: linear-gradient(135deg, #2e1113, #140608);
  border: 1px solid var(--c1);
  border-radius: 2px;
  color: var(--c1);
  font-family: inherit;
  font-size: 13px;
  letter-spacing: .3em;
  text-indent: .3em;
  cursor: pointer;
  transition: all .35s;
  text-shadow: 0 0 12px var(--c1-glow);
}
:global(body.god-mode) .submit {
  background: linear-gradient(135deg, #131c33, #060a14);
}
.submit:hover {
  background: var(--c1);
  color: #0a0508;
  text-shadow: none;
  box-shadow: 0 0 28px var(--c1-glow);
}

/* ---------- 热词 ---------- */
.hots {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-left: 2px;
}
.hots-label {
  font-size: 11px;
  letter-spacing: .3em;
  color: #5f5869;
}
.hot {
  padding: 5px 11px;
  background: rgba(255, 255, 255, .03);
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 999px;
  color: #9d93a4;
  font-family: inherit;
  font-size: 11.5px;
  letter-spacing: .1em;
  cursor: pointer;
  transition: all .28s;
}
.hot:hover {
  border-color: var(--c1-dim);
  color: var(--c1);
  background: var(--c1-soft);
}

/* ---------- 结果面板 ---------- */
.panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 10px);
  z-index: 20;
  background: linear-gradient(170deg, rgba(20, 14, 18, .98), rgba(7, 4, 5, .99));
  border: 1px solid var(--c1-dim);
  border-radius: 3px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, .8), inset 0 0 50px var(--c1-soft);
  overflow: hidden;
  transition: background .8s, border-color .8s;
}
:global(body.god-mode) .panel {
  background: linear-gradient(170deg, rgba(14, 17, 30, .98), rgba(4, 5, 10, .99));
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 11px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, .06);
  font-size: 11.5px;
  letter-spacing: .16em;
  color: #8a8195;
}
.panel-head .hint { color: #4f485a; font-size: 10.5px; letter-spacing: .1em; }

.list { list-style: none; max-height: 328px; overflow-y: auto; }
.list::-webkit-scrollbar { width: 6px; }
.list::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, .12); border-radius: 3px; }

.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, .04);
  cursor: pointer;
  transition: background .2s;
}
.item:last-child { border-bottom: none; }
.item.active { background: var(--c1-soft); }

.type {
  flex: none;
  width: 42px;
  text-align: center;
  padding: 3px 0;
  font-size: 10px;
  letter-spacing: .12em;
  color: #9d93a4;
  border: 1px solid rgba(255, 255, 255, .12);
  border-radius: 2px;
}
.type[data-type="神技"] { color: #e0a86a; border-color: rgba(224, 168, 106, .3); }
.type[data-type="镇兽"] { color: #7fc7a0; border-color: rgba(127, 199, 160, .3); }
.type[data-type="成员"] { color: #b79ce0; border-color: rgba(183, 156, 224, .3); }
.type[data-type="赛程"] { color: #7fb6d8; border-color: rgba(127, 182, 216, .3); }

.main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.name {
  font-size: 13.5px;
  letter-spacing: .1em;
  color: #ded5e4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.name mark {
  background: transparent;
  color: var(--c1);
  text-shadow: 0 0 12px var(--c1-glow);
}
.desc {
  font-size: 11px;
  color: #6f6779;
  letter-spacing: .06em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.level {
  flex: none;
  font-size: 10.5px;
  letter-spacing: .1em;
  color: var(--c1-dim);
}

.empty {
  padding: 26px 16px 30px;
  text-align: center;
  font-size: 12.5px;
  line-height: 2;
  color: #7a7284;
  letter-spacing: .12em;
}
.empty span { color: #4f485a; font-size: 11px; }

.panel-enter-active, .panel-leave-active { transition: opacity .2s, transform .2s; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 640px) {
  .bar { padding-left: 12px; }
  .submit { padding: 10px 14px; font-size: 12px; letter-spacing: .2em; }
  .panel-head .hint { display: none; }
  .hots-label { display: none; }
}
</style>
