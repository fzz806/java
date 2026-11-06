<script setup>
import { ref, computed } from 'vue'
import { navItems } from '../data/homeData.js'

const props = defineProps({
  username: { type: String, default: '' },
  godMode: { type: Boolean, default: false },
  activeKey: { type: String, default: 'home' }
})
const emit = defineEmits(['search', 'navigate', 'toggle-form', 'logout'])

// 移动端折叠菜单
const open = ref(false)

const initial = computed(() => (props.username ? props.username.slice(0, 1) : '？'))
const formText = computed(() => (props.godMode ? '伪神形态' : '恶鬼形态'))

function onNav(item) {
  open.value = false
  emit('navigate', item)
}
</script>

<template>
  <header class="nav">
    <div class="nav-inner">
      <!-- 徽记 + 品牌 -->
      <a class="brand" href="javascript:void(0)" @click="emit('navigate', navItems[0])">
        <span class="mark">
          <svg viewBox="0 0 100 100" fill="none" aria-hidden="true">
            <circle cx="50" cy="50" r="44" stroke="var(--c1)" stroke-width="2" opacity=".45" />
            <path
              d="M50 12 L58 36 L84 36 L63 51 L71 77 L50 62 L29 77 L37 51 L16 36 L42 36 Z"
              stroke="var(--c1)"
              stroke-width="3.4"
              fill="none"
            />
            <circle cx="50" cy="50" r="5" fill="var(--c1)" />
          </svg>
        </span>
        <span class="brand-text">
          <b>觉 醒 伪 神</b>
          <i>神明弃我，我自为神</i>
        </span>
      </a>

      <!-- 主导航 -->
      <nav class="links" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="link"
          :class="{ active: activeKey === item.key }"
          @click="onNav(item)"
        >
          <span class="cn">{{ item.label }}</span>
          <span class="en">{{ item.en }}</span>
        </button>
      </nav>

      <!-- 右侧操作区 -->
      <div class="right">
        <button class="icon-btn" aria-label="搜索" title="搜索" @click="emit('search')">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="10.5" cy="10.5" r="6.5" stroke="currentColor" stroke-width="1.6" />
            <path d="M15.6 15.6 L21 21" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
        </button>

        <button class="form-btn" title="切换形态" @click="emit('toggle-form')">
          <i class="dot" :class="{ god: godMode }"></i>
          {{ formText }}
        </button>

        <div class="user" :title="username">
          <span class="avatar">{{ initial }}</span>
          <span class="uname">{{ username }}</span>
        </div>

        <button class="logout" @click="emit('logout')">退出</button>

        <button class="menu-btn" aria-label="展开菜单" @click="open = !open">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 移动端下拉导航 -->
    <transition name="drop">
      <div v-if="open" class="mobile-panel">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="mobile-link"
          :class="{ active: activeKey === item.key }"
          @click="onNav(item)"
        >
          {{ item.label }}
          <span class="en">{{ item.en }}</span>
        </button>
      </div>
    </transition>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 30;
  background: linear-gradient(180deg, rgba(10, 5, 6, .94), rgba(7, 4, 5, .72));
  border-bottom: 1px solid rgba(255, 255, 255, .07);
  backdrop-filter: blur(14px);
  transition: background .8s, border-color .8s;
}
:global(body.god-mode) .nav {
  background: linear-gradient(180deg, rgba(9, 12, 24, .94), rgba(4, 5, 10, .72));
}

.nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 28px;
  height: 68px;
  display: flex;
  align-items: center;
  gap: 28px;
}

/* ---------- 品牌 ---------- */
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  flex: none;
}
.mark {
  width: 34px;
  height: 34px;
  flex: none;
  display: grid;
  place-items: center;
  border: 1px solid var(--c1-dim);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(43, 13, 12, .9), rgba(8, 3, 4, .9));
  box-shadow: 0 0 16px var(--c1-soft);
  transition: border-color .8s, box-shadow .8s, background .8s;
}
:global(body.god-mode) .mark {
  background: radial-gradient(circle, rgba(14, 20, 38, .9), rgba(4, 6, 13, .9));
}
.mark svg { width: 74%; height: 74%; }
.brand-text { display: flex; flex-direction: column; line-height: 1.35; }
.brand-text b {
  font-size: 15px;
  letter-spacing: .22em;
  color: #e8e0d0;
  font-weight: 900;
  background: linear-gradient(180deg, #fff4e6, var(--c1) 70%, var(--c1-dim));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 12px var(--c1-glow));
  transition: filter .8s;
}
.brand-text i {
  font-style: normal;
  font-size: 10px;
  letter-spacing: .26em;
  color: #6f6675;
  margin-top: 2px;
}

/* ---------- 主导航 ---------- */
.links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}
.link {
  position: relative;
  background: none;
  border: none;
  padding: 10px 14px 12px;
  cursor: pointer;
  color: #9d93a4;
  font-family: inherit;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  transition: color .3s;
}
.link .cn { font-size: 13px; letter-spacing: .18em; }
.link .en {
  font-size: 8px;
  letter-spacing: .2em;
  color: #57505e;
  transition: color .3s;
}
.link::after {
  content: "";
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 4px;
  height: 1px;
  background: var(--c1);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform .35s, box-shadow .35s;
  box-shadow: 0 0 10px var(--c1-glow);
}
.link:hover { color: #d8cfbf; }
.link:hover .en { color: #8b8394; }
.link:hover::after { transform: scaleX(.45); }
.link.active { color: var(--c1); }
.link.active .en { color: var(--c1-dim); }
.link.active::after { transform: scaleX(1); }

/* ---------- 右侧 ---------- */
.right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}
.icon-btn {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 2px;
  color: #a99eb0;
  cursor: pointer;
  transition: all .3s;
}
.icon-btn svg { width: 16px; height: 16px; }
.icon-btn:hover {
  color: var(--c1);
  border-color: var(--c1-dim);
  box-shadow: 0 0 14px var(--c1-soft);
}

.form-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 12px;
  background: transparent;
  border: 1px solid var(--c1-dim);
  border-radius: 2px;
  color: #b9b1a3;
  font-family: inherit;
  font-size: 11.5px;
  letter-spacing: .16em;
  cursor: pointer;
  transition: all .3s;
  white-space: nowrap;
}
.form-btn:hover { border-color: var(--c1); color: var(--c1); box-shadow: 0 0 16px var(--c1-soft); }
.form-btn .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff4d4d;
  box-shadow: 0 0 10px rgba(255, 77, 77, .8);
  transition: background .8s, box-shadow .8s;
}
.form-btn .dot.god {
  background: var(--c1);
  box-shadow: 0 0 10px var(--c1-glow);
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 4px;
  min-width: 0;
}
.avatar {
  width: 30px;
  height: 30px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--c1-dim);
  background: rgba(0, 0, 0, .5);
  color: var(--c1);
  font-size: 13px;
  text-shadow: 0 0 10px var(--c1-glow);
  transition: color .8s, border-color .8s;
}
.uname {
  font-size: 12.5px;
  letter-spacing: .12em;
  color: #cfc6d6;
  max-width: 108px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout {
  background: none;
  border: none;
  color: #7f7689;
  font-family: inherit;
  font-size: 12px;
  letter-spacing: .2em;
  cursor: pointer;
  padding: 6px 2px;
  transition: color .3s;
}
.logout:hover { color: #ff6b6b; }

.menu-btn {
  display: none;
  width: 34px;
  height: 34px;
  place-items: center;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 2px;
  color: #a99eb0;
  cursor: pointer;
}
.menu-btn svg { width: 18px; height: 18px; }

/* ---------- 移动端 ---------- */
.mobile-panel {
  display: none;
  border-top: 1px solid rgba(255, 255, 255, .06);
  padding: 8px 20px 14px;
}
.mobile-link {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 4px;
  background: none;
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, .05);
  color: #a99eb0;
  font-family: inherit;
  font-size: 13.5px;
  letter-spacing: .18em;
  cursor: pointer;
}
.mobile-link .en { font-size: 9px; letter-spacing: .2em; color: #57505e; }
.mobile-link.active { color: var(--c1); }

.drop-enter-active, .drop-leave-active { transition: opacity .22s, transform .22s; }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateY(-6px); }

@media (max-width: 1080px) {
  .link .en { display: none; }
}
@media (max-width: 900px) {
  .nav-inner { gap: 14px; padding: 0 16px; height: 62px; }
  .links { display: none; }
  .mobile-panel { display: block; }
  .menu-btn { display: grid; }
  .uname { display: none; }
  .form-btn { font-size: 10.5px; padding: 6px 9px; }
}
@media (max-width: 560px) {
  .brand-text i { display: none; }
  .logout { display: none; }
}
</style>
