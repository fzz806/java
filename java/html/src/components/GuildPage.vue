<script setup>
import { ref, computed } from 'vue'
import {
  fetchGuildOverview, applyGuild, handleApply, quitGuild,
  setMemberRole, removeMember, publishNotice, deleteNotice,
  updateGuildInfo, contributeGuild
} from '../api/guild.js'
import { getSession } from '../api/session.js'

const info = ref({})
const members = ref([])
const warehouse = ref([])
const notices = ref([])
const rank = ref([])
const applies = ref([])
const me = ref({})
const memberCount = ref(0)
const loading = ref(true)
const errorText = ref('')
const meName = getSession()?.username || ''

// 交互状态
const applyReason = ref('')
const roleDraft = ref({})          // { memberId: roleText }
const noticeForm = ref({ tag: '通告', title: '', content: '' })
const editingInfo = ref(false)
const infoDraft = ref({})
const busy = ref(false)
const toast = ref('')
const tab = ref('members')         // members | notices | warehouse | rank | manage

load()

async function load() {
  const r = await fetchGuildOverview()
  if (r.code === 0 && r.data) {
    const d = r.data
    info.value = d.info || {}
    members.value = d.members || []
    memberCount.value = d.memberCount || (members.value.length)
    warehouse.value = d.warehouse || []
    notices.value = d.notices || []
    rank.value = d.rank || []
    applies.value = d.applies || []
    me.value = d.me || {}
  } else {
    errorText.value = r.message || '公会大门暂时关闭'
  }
  loading.value = false
}

const isMaster = computed(() => me.value.master === true)
const joined = computed(() => me.value.joined === true)
const applyStatus = computed(() => me.value.applyStatus || '')

function initial(name) {
  return name ? name.slice(0, 1) : '？'
}
function isSelf(m) {
  return m.self === true || (meName && m.name === meName)
}

function flash(msg) {
  toast.value = msg
  clearTimeout(flash._t)
  flash._t = setTimeout(() => { toast.value = '' }, 2600)
}

async function run(fn, successMsg, after) {
  if (busy.value) return
  busy.value = true
  const r = await fn()
  busy.value = false
  if (r.code === 0) {
    flash(successMsg || r.message || '神堂已记下')
    if (after) after()
    await load()
  } else {
    flash('✗ ' + (r.message || '操作未成'))
  }
}

function doApply() {
  run(() => applyGuild(applyReason.value), '名帖已递上', () => { applyReason.value = '' })
}
function doQuit() {
  if (!confirm('确定要脱离恶鬼之牙吗？贡献将清零。')) return
  run(() => quitGuild(), '已除名')
}
function doContribute() {
  run(() => contributeGuild(), '')
}
function doHandle(a, pass) {
  const note = pass ? '准入' : '不收'
  run(() => handleApply(a.id, pass, note), pass ? '已准入伙' : '已驳回')
}
function doSetRole(id) {
  const role = roleDraft.value[id]
  if (!role || !role.trim()) return flash('请填写职位')
  run(() => setMemberRole(id, role), '职位已改', () => { delete roleDraft.value[id] })
}
function doRemove(m) {
  if (!confirm(`确定逐出「${m.name}」？`)) return
  run(() => removeMember(m.id), '已逐出')
}
function doPublish() {
  const f = noticeForm.value
  if (!f.title.trim()) return flash('请填写标题')
  run(() => publishNotice(f.tag, f.title, f.content), '公告已贴出', () => {
    noticeForm.value = { tag: '通告', title: '', content: '' }
  })
}
function doDeleteNotice(id) {
  if (!confirm('确定撤下这条公告？')) return
  run(() => deleteNotice(id), '公告已撤下')
}
function openInfoEdit() {
  infoDraft.value = { ...info.value }
  editingInfo.value = true
}
function doSaveInfo() {
  const keys = ['name', 'en', 'motto', 'members', 'stance', 'warehouse']
  const tasks = keys
    .filter(k => infoDraft.value[k] != null && infoDraft.value[k] !== info.value[k])
    .map(k => updateGuildInfo(k, infoDraft.value[k]))
  if (!tasks.length) { editingInfo.value = false; return }
  run(async () => {
    for (const t of tasks) {
      const r = await t
      if (r.code !== 0) return r
    }
    return { code: 0 }
  }, '简报已修订', () => { editingInfo.value = false })
}

const tabs = computed(() => {
  const base = [
    { key: 'members', label: '在册名册' },
    { key: 'notices', label: '公会公告' },
    { key: 'warehouse', label: '公用仓库' },
    { key: 'rank', label: '供奉榜' }
  ]
  if (isMaster.value) base.push({ key: 'manage', label: '会长理事' })
  return base
})

const roleOptions = ['副会长', '猎手', '医师', '刺客', '后勤', '哨探', '护法', '执事', '新晋 · 神弃者']
</script>

<template>
  <div class="page">
    <header class="page-head">
      <h2>恶 鬼 之 牙</h2>
      <span class="page-en">GUILD · 神堂黑名单头号公会</span>
    </header>

    <div v-if="loading" class="hint">公会大门开启中…</div>
    <div v-else-if="errorText" class="hint err">{{ errorText }}</div>

    <template v-else>
      <!-- 简报 -->
      <section class="brief card-panel">
        <div v-if="!editingInfo" class="brief-view">
          <div class="brand-row">
            <span class="g-name">{{ info.name || '恶鬼之牙' }}</span>
            <span class="g-en">{{ info.en }}</span>
            <span v-if="isMaster" class="master-tag">会长</span>
          </div>
          <p class="g-motto">「 {{ info.motto }} 」</p>
          <div class="g-meta">
            <span>{{ info.members }}</span>
            <span>{{ info.warehouse }}</span>
            <span class="danger">{{ info.stance }}</span>
          </div>
          <button v-if="isMaster" class="link-btn" @click="openInfoEdit">✎ 修订简报</button>
        </div>
        <div v-else class="brief-edit">
          <div v-for="k in ['name','en','motto','members','stance','warehouse']" :key="k" class="edit-row">
            <label>{{ k }}</label>
            <input v-model="infoDraft[k]" type="text" />
          </div>
          <div class="edit-actions">
            <button class="btn primary" :disabled="busy" @click="doSaveInfo">保存</button>
            <button class="btn" @click="editingInfo = false">取消</button>
          </div>
        </div>
      </section>

      <!-- 我的身份条 -->
      <section class="identity card-panel">
        <div class="id-main">
          <span class="id-label">我的身份</span>
          <strong class="id-value">{{ joined ? me.role : '尚未在册' }}</strong>
          <span v-if="isMaster" class="id-crown">◆ 执掌生杀</span>
        </div>
        <div class="id-stats">
          <span>贡献 <b>{{ me.contribution || 0 }}</b></span>
          <span>供奉 <b>{{ me.contributedToday ? '今日已奉' : '今日可奉' }}</b></span>
        </div>
        <div class="id-actions">
          <template v-if="!joined">
            <span v-if="applyStatus === 'pending'" class="pending-note">名帖待批 · 静候会长朱批</span>
            <span v-else-if="applyStatus === 'rejected'" class="pending-note warn">名帖被驳回 · 可再次递上</span>
            <template v-else>
              <input v-model="applyReason" class="reason-input" type="text" maxlength="60" placeholder="写一句入伙理由…" />
              <button class="btn primary" :disabled="busy" @click="doApply">递交名帖</button>
            </template>
          </template>
          <template v-else>
            <button class="btn" :disabled="busy || me.contributedToday" @click="doContribute">
              {{ me.contributedToday ? '今日已供奉' : '供奉 · 耗20灵能得30贡献' }}
            </button>
            <button class="btn ghost" :disabled="busy" @click="doQuit">脱离公会</button>
          </template>
        </div>
      </section>

      <!-- 页签 -->
      <nav class="tabs">
        <button v-for="t in tabs" :key="t.key" :class="{ active: tab === t.key }" @click="tab = t.key">
          {{ t.label }}
          <i v-if="t.key === 'members'">{{ memberCount }}</i>
          <i v-if="t.key === 'notices'">{{ notices.length }}</i>
          <i v-if="t.key === 'manage' && applies.length">{{ applies.length }}</i>
        </button>
      </nav>

      <!-- 名册 -->
      <section v-if="tab === 'members'" class="card-panel">
        <h3>在 册 恶 鬼</h3>
        <div class="member-list">
          <div v-for="(m, idx) in members" :key="m.id" class="member" :class="{ mine: isSelf(m) }">
            <span class="rank-no">{{ String(idx + 1).padStart(2, '0') }}</span>
            <span class="avatar">{{ initial(m.name) }}</span>
            <div class="mem-main">
              <span class="mem-name">{{ m.name }}<i class="mem-role">{{ m.role }}</i><i v-if="isSelf(m)" class="mem-me">我</i></span>
              <span class="mem-desc">{{ m.bio }}</span>
              <span class="mem-sub">{{ m.element }} · {{ m.realm }}<template v-if="m.userId"> · 贡献 {{ m.contribution || 0 }}</template></span>
            </div>
            <span class="mem-power">{{ m.power }}</span>
          </div>
        </div>
      </section>

      <!-- 公告 -->
      <section v-else-if="tab === 'notices'" class="card-panel">
        <h3>公 会 公 告</h3>
        <div class="notice-list">
          <div v-for="n in notices" :key="n.id" class="notice">
            <div class="notice-top">
              <span class="notice-tag" :class="{ urgent: n.tag === '紧急' }">{{ n.tag }}</span>
              <strong>{{ n.title }}</strong>
              <button v-if="isMaster" class="x-btn" title="撤下" :disabled="busy" @click="doDeleteNotice(n.id)">×</button>
            </div>
            <p>{{ n.content }}</p>
            <span class="notice-by">— {{ n.author }}</span>
          </div>
          <div v-if="!notices.length" class="empty">暂无公告</div>
        </div>
      </section>

      <!-- 仓库 -->
      <section v-else-if="tab === 'warehouse'" class="card-panel">
        <h3>公 用 仓 库</h3>
        <div class="wh-grid">
          <div v-for="(w, i) in warehouse" :key="i" class="wh-item">
            <span class="wh-name">{{ w.name }}</span>
            <span class="wh-count">× {{ w.count }}</span>
            <span class="wh-rarity" :class="w.rarity">{{ w.rarity }}</span>
          </div>
        </div>
        <p class="wh-note">{{ info.warehouse }}</p>
      </section>

      <!-- 供奉榜 -->
      <section v-else-if="tab === 'rank'" class="card-panel">
        <h3>供 奉 榜</h3>
        <div v-if="rank.length" class="rank-list">
          <div v-for="r in rank" :key="r.userId" class="rank-row" :class="{ mine: r.name === meName }">
            <span class="rank-no">{{ String(r.no).padStart(2, '0') }}</span>
            <span class="rank-name">{{ r.name }}</span>
            <span class="rank-bar"><i :style="{ width: (r.amount / (rank[0]?.amount || 1) * 100) + '%' }"></i></span>
            <span class="rank-amount">{{ r.amount }}</span>
          </div>
        </div>
        <div v-else class="empty">尚无人供奉 · 成为第一人</div>
      </section>

      <!-- 会长理事 -->
      <section v-else-if="tab === 'manage'" class="card-panel">
        <h3>会 长 理 事</h3>

        <div class="manage-block">
          <h4>待批名帖 <i>{{ applies.length }}</i></h4>
          <div v-for="a in applies" :key="a.id" class="apply-row">
            <div class="apply-info">
              <strong>{{ a.username }}</strong>
              <span class="apply-reason">「{{ a.reason || '未写理由' }}」</span>
              <span class="apply-time">{{ a.createdAt }}</span>
            </div>
            <div class="apply-ops">
              <button class="btn primary" :disabled="busy" @click="doHandle(a, true)">准入</button>
              <button class="btn ghost" :disabled="busy" @click="doHandle(a, false)">驳回</button>
            </div>
          </div>
          <div v-if="!applies.length" class="empty">暂无待批名帖</div>
        </div>

        <div class="manage-block">
          <h4>任免职位</h4>
          <div v-for="m in members.filter(x => x.userId && x.role !== '会长')" :key="m.id" class="role-row">
            <span class="role-name">{{ m.name }}</span>
            <select v-model="roleDraft[m.id]" class="role-select">
              <option value="" disabled>现职：{{ m.role }}</option>
              <option v-for="opt in roleOptions" :key="opt" :value="opt">{{ opt }}</option>
            </select>
            <button class="btn tiny" :disabled="busy || !roleDraft[m.id]" @click="doSetRole(m.id)">改</button>
            <button class="btn tiny ghost" :disabled="busy" @click="doRemove(m)">逐</button>
          </div>
          <div v-if="!members.some(x => x.userId && x.role !== '会长')" class="empty">名册中暂无真实玩家</div>
        </div>

        <div class="manage-block">
          <h4>张贴公告</h4>
          <div class="notice-form">
            <select v-model="noticeForm.tag" class="role-select">
              <option>通告</option><option>规则</option><option>嘉奖</option><option>紧急</option>
            </select>
            <input v-model="noticeForm.title" type="text" maxlength="60" placeholder="公告标题" />
            <input v-model="noticeForm.content" type="text" maxlength="200" placeholder="公告正文" />
            <button class="btn primary" :disabled="busy" @click="doPublish">贴出</button>
          </div>
        </div>
      </section>
    </template>

    <!-- 轻提示 -->
    <transition name="fade">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.page { width: 100%; max-width: 1280px; margin: 0 auto; padding: 34px 28px 60px; }
.page-head { display: flex; align-items: baseline; gap: 14px; flex-wrap: wrap; padding-bottom: 16px; margin-bottom: 22px; border-bottom: 1px solid rgba(255,255,255,.07); }
.page-head h2 { font-size: 17px; font-weight: 400; letter-spacing: .3em; color: #ded5e4; position: relative; padding-left: 13px; }
.page-head h2::before { content: ""; position: absolute; left: 0; top: 50%; width: 3px; height: 15px; margin-top: -8px; background: var(--c1); box-shadow: 0 0 10px var(--c1-glow); transition: background .8s; }
.page-en { font-size: 9.5px; letter-spacing: .22em; color: #5d5666; }
.hint { text-align: center; font-size: 13px; line-height: 2; letter-spacing: .2em; color: #6f6779; padding: 80px 0; }
.hint.err { color: #c05a50; }

.card-panel { padding: 20px 24px; border: 1px solid rgba(255,255,255,.08); border-radius: 3px; background: linear-gradient(160deg, rgba(20,14,18,.78), rgba(7,4,5,.84)); margin-bottom: 18px; transition: background .8s; }
:global(body.god-mode) .card-panel { background: linear-gradient(160deg, rgba(14,17,30,.78), rgba(4,5,10,.84)); }
.card-panel h3 { font-size: 12px; font-weight: 400; letter-spacing: .3em; color: #9a8a55; margin-bottom: 16px; }

/* 简报 */
.brand-row { display: flex; align-items: baseline; gap: 14px; flex-wrap: wrap; }
.g-name { font-size: 26px; font-weight: 900; letter-spacing: .18em; background: linear-gradient(180deg,#fff4e6,var(--c1) 70%,var(--c1-dim)); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; filter: drop-shadow(0 0 12px var(--c1-glow)); }
.g-en { font-size: 10px; letter-spacing: .24em; color: #5d5666; }
.master-tag { font-size: 9.5px; letter-spacing: .12em; color: #ffd98a; border: 1px solid rgba(255,200,100,.45); border-radius: 2px; padding: 2px 8px; background: rgba(255,190,80,.1); }
.g-motto { margin-top: 10px; font-size: 13px; letter-spacing: .26em; color: #a89c8a; }
.g-meta { margin-top: 14px; display: flex; flex-wrap: wrap; gap: 8px; }
.g-meta span { font-size: 11px; letter-spacing: .12em; color: #a99eb0; border: 1px solid rgba(255,255,255,.1); border-radius: 2px; padding: 5px 11px; background: rgba(0,0,0,.3); }
.g-meta span.danger { color: #ff8f8f; border-color: rgba(255,77,77,.4); background: rgba(255,77,77,.08); }
.link-btn { margin-top: 14px; background: none; border: none; color: var(--c1); font-size: 11px; letter-spacing: .1em; cursor: pointer; opacity: .8; }
.link-btn:hover { opacity: 1; }
.brief-edit .edit-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.brief-edit label { width: 78px; font-size: 11px; color: #8c8394; letter-spacing: .1em; }
.brief-edit input, .reason-input, .role-select, .notice-form input { background: rgba(0,0,0,.4); border: 1px solid rgba(255,255,255,.14); border-radius: 2px; color: #e6dfd2; padding: 7px 10px; font-size: 12px; outline: none; }
.brief-edit input:focus, .reason-input:focus, .notice-form input:focus { border-color: var(--c1-dim); }
.edit-actions { display: flex; gap: 10px; margin-top: 12px; }

/* 身份条 */
.identity { display: flex; align-items: center; gap: 20px; flex-wrap: wrap; }
.id-main { display: flex; align-items: baseline; gap: 10px; }
.id-label { font-size: 10px; letter-spacing: .18em; color: #6f6779; }
.id-value { font-size: 14px; color: #e8e0d0; font-weight: 400; letter-spacing: .1em; }
.id-crown { font-size: 10px; color: #ffd98a; letter-spacing: .1em; }
.id-stats { display: flex; gap: 16px; font-size: 11px; color: #8c8394; }
.id-stats b { color: var(--c1); font-size: 13px; }
.id-actions { display: flex; align-items: center; gap: 8px; margin-left: auto; flex-wrap: wrap; }
.pending-note { font-size: 11px; color: #c9a86a; letter-spacing: .1em; }
.pending-note.warn { color: #c9776a; }

/* 页签 */
.tabs { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 16px; border-bottom: 1px solid rgba(255,255,255,.07); }
.tabs button { background: none; border: none; border-bottom: 2px solid transparent; color: #756d7e; font-size: 12px; letter-spacing: .14em; padding: 10px 14px; cursor: pointer; transition: color .25s, border-color .25s; }
.tabs button:hover { color: #b9b0c2; }
.tabs button.active { color: var(--c1); border-bottom-color: var(--c1); }
.tabs button i { font-style: normal; font-size: 9px; margin-left: 5px; padding: 1px 5px; border-radius: 8px; background: rgba(255,255,255,.08); color: #9d94a5; }
.tabs button.active i { background: rgba(255,77,77,.16); color: #ff9a9a; }

/* 按钮 */
.btn { background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.16); color: #cfc6d4; font-size: 11px; letter-spacing: .1em; padding: 7px 14px; border-radius: 2px; cursor: pointer; transition: all .25s; }
.btn:hover:not(:disabled) { border-color: var(--c1-dim); color: #fff; }
.btn:disabled { opacity: .38; cursor: not-allowed; }
.btn.primary { border-color: var(--c1-dim); color: var(--c1); background: rgba(255,77,77,.08); }
.btn.ghost { border-color: rgba(255,255,255,.1); color: #8b8290; }
.btn.tiny { padding: 4px 10px; font-size: 10px; }

/* 名册 */
.member-list { display: grid; grid-template-columns: repeat(2,1fr); gap: 10px; }
.member { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border: 1px solid rgba(255,255,255,.07); border-radius: 2px; background: rgba(0,0,0,.28); transition: border-color .3s; }
.member:hover { border-color: var(--c1-dim); }
.member.mine { border-color: rgba(255,77,77,.45); background: rgba(255,77,77,.06); box-shadow: inset 0 0 18px rgba(255,77,77,.1); }
.rank-no { font-family: Georgia,serif; font-size: 12px; color: #5f5869; width: 18px; }
.avatar { flex: none; width: 36px; height: 36px; display: grid; place-items: center; border-radius: 50%; border: 1px solid var(--c1-dim); background: rgba(0,0,0,.5); color: var(--c1); font-size: 15px; text-shadow: 0 0 10px var(--c1-glow); }
.mem-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.mem-name { font-size: 13.5px; letter-spacing: .1em; color: #e8e0d0; display: flex; align-items: center; gap: 8px; }
.mem-role { font-style: normal; font-size: 9.5px; letter-spacing: .1em; color: var(--c1); border: 1px solid var(--c1-dim); border-radius: 2px; padding: 1px 6px; }
.mem-me { font-style: normal; font-size: 9.5px; color: #ff9a9a; border: 1px solid rgba(255,77,77,.45); border-radius: 2px; padding: 1px 5px; background: rgba(255,77,77,.12); }
.mem-desc { font-size: 11px; line-height: 1.6; color: #938a96; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mem-sub { font-size: 9.5px; letter-spacing: .1em; color: #5f5869; }
.mem-power { flex: none; font-family: Georgia,serif; font-size: 16px; color: var(--c1); text-shadow: 0 0 10px var(--c1-soft); }

/* 公告 */
.notice-list { display: flex; flex-direction: column; gap: 12px; }
.notice { padding: 14px 16px; border: 1px solid rgba(255,255,255,.07); border-left: 2px solid var(--c1-dim); background: rgba(0,0,0,.26); border-radius: 2px; }
.notice-top { display: flex; align-items: center; gap: 10px; }
.notice-top strong { font-size: 13px; color: #ded5e4; letter-spacing: .08em; font-weight: 400; }
.notice-tag { font-size: 9px; letter-spacing: .1em; padding: 2px 7px; border: 1px solid var(--c1-dim); color: var(--c1); border-radius: 2px; }
.notice-tag.urgent { color: #ff8f8f; border-color: rgba(255,77,77,.5); }
.notice p { margin: 8px 0 4px; font-size: 12px; line-height: 1.7; color: #a99eb0; }
.notice-by { font-size: 10px; color: #6f6779; }
.x-btn { margin-left: auto; background: none; border: none; color: #7d7486; font-size: 18px; line-height: 1; cursor: pointer; }
.x-btn:hover { color: #ff8f8f; }

/* 仓库 */
.wh-grid { display: grid; grid-template-columns: repeat(2,1fr); gap: 10px; }
.wh-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; background: rgba(0,0,0,.28); border: 1px solid rgba(255,255,255,.07); border-radius: 2px; }
.wh-name { flex: 1; font-size: 13px; color: #ded5e4; }
.wh-count { font-family: Georgia,serif; font-size: 14px; color: var(--c1); }
.wh-rarity { font-size: 9.5px; padding: 2px 7px; border: 1px solid rgba(255,255,255,.16); border-radius: 2px; color: #9d94a5; }
.wh-rarity.史诗 { color: #c9a0ff; border-color: rgba(180,120,255,.4); }
.wh-rarity.稀有 { color: #8fb8ff; border-color: rgba(100,160,255,.4); }
.wh-note { margin-top: 14px; font-size: 11px; color: #7d7486; letter-spacing: .08em; }

/* 供奉榜 */
.rank-list { display: flex; flex-direction: column; gap: 8px; }
.rank-row { display: flex; align-items: center; gap: 12px; padding: 9px 12px; background: rgba(0,0,0,.26); border-radius: 2px; }
.rank-row.mine { border: 1px solid rgba(255,77,77,.4); background: rgba(255,77,77,.06); }
.rank-name { width: 120px; font-size: 12px; color: #ded5e4; }
.rank-bar { flex: 1; height: 5px; background: rgba(255,255,255,.06); border-radius: 3px; overflow: hidden; }
.rank-bar i { display: block; height: 100%; background: linear-gradient(90deg, var(--c1-dim), var(--c1)); }
.rank-amount { width: 48px; text-align: right; font-family: Georgia,serif; color: var(--c1); font-size: 14px; }

/* 会长理事 */
.manage-block { margin-bottom: 24px; }
.manage-block h4 { font-size: 11px; font-weight: 400; letter-spacing: .2em; color: #9a8a55; margin-bottom: 12px; }
.manage-block h4 i { font-style: normal; color: var(--c1); }
.apply-row, .role-row { display: flex; align-items: center; gap: 12px; padding: 10px 12px; margin-bottom: 8px; background: rgba(0,0,0,.26); border: 1px solid rgba(255,255,255,.06); border-radius: 2px; flex-wrap: wrap; }
.apply-info { flex: 1; min-width: 200px; display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.apply-info strong { font-size: 13px; color: #e8e0d0; font-weight: 400; }
.apply-reason { font-size: 11px; color: #938a96; }
.apply-time { font-size: 9.5px; color: #5f5869; }
.apply-ops, .edit-actions { display: flex; gap: 8px; }
.role-name { width: 100px; font-size: 12px; color: #ded5e4; }
.role-select { flex: 1; min-width: 120px; }
.notice-form { display: flex; gap: 8px; flex-wrap: wrap; }
.notice-form input { flex: 1; min-width: 160px; }
.empty { font-size: 11px; color: #6f6779; padding: 18px 0; text-align: center; letter-spacing: .1em; }

/* 提示 */
.toast { position: fixed; bottom: 32px; left: 50%; transform: translateX(-50%); z-index: 50; padding: 11px 22px; background: rgba(20,12,14,.96); border: 1px solid var(--c1-dim); border-radius: 3px; color: #e8e0d0; font-size: 12px; letter-spacing: .1em; box-shadow: 0 0 24px rgba(0,0,0,.6); }
.fade-enter-active, .fade-leave-active { transition: opacity .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 860px) {
  .member-list, .wh-grid { grid-template-columns: 1fr; }
  .identity { gap: 12px; }
  .id-actions { margin-left: 0; width: 100%; }
}
@media (max-width: 640px) {
  .page { padding: 24px 16px 50px; }
  .card-panel { padding: 16px; }
}
</style>
