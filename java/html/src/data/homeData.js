/**
 * 首页静态数据（纯前端）
 *
 * 说明：当前仅实现前端展示，所有内容均为本地示例数据，
 * 后端接口保持原样（/api/login、/api/register、/api/me、/api/ping），
 * 后续接入真实数据时，把这里替换成 api/ 下的请求即可。
 */

/** 顶部导航项（除首页外均为占位，点击只提示“尚未开启”） */
export const navItems = [
  { key: 'home', label: '首页', en: 'HOME' },
  { key: 'skill', label: '神技图鉴', en: 'SKILLS' },
  { key: 'beast', label: '镇兽大比', en: 'ARENA' },
  { key: 'guild', label: '恶鬼之牙', en: 'GUILD' },
  { key: 'rank', label: '战力榜', en: 'RANKING' }
]

/** 搜索框下方的热词 */
export const hotKeywords = ['骨翼', '星环瞳', '禁忌神技', '镇兽大比', '恶鬼之牙']

/** 搜索索引：神技 / 镇兽 / 成员 / 赛程 */
export const searchIndex = [
  { type: '神技', name: '骨翼 · 撕裂', level: 'A', desc: '第一变专属，骨翼横斩，附带灵能反馈' },
  { type: '神技', name: '星环瞳 · 复写', level: 'S', desc: '第二变核心，复制视野内他人神技并篡改参数' },
  { type: '神技', name: '吞噬 · 愈伤', level: 'B', desc: '吞食灵能即修复伤势，代价是永无止境的饥饿' },
  { type: '神技', name: '禁忌 · 伪神降临', level: 'SSS', desc: '三分钟内篡改全场规则，冷却七日' },
  { type: '神技', name: '骨刺 · 贯星', level: 'A', desc: '将骨翼拆解为十二枚骨刺，可穿三层护盾' },
  { type: '镇兽', name: '铁脊犀', level: 'A', desc: '镇兽大比三十二强守门兽，背甲可抵 A 级正面斩击' },
  { type: '镇兽', name: '鸣砂雀', level: 'B', desc: '群居，声波可短暂干扰神印镶嵌' },
  { type: '镇兽', name: '灰潮鲛', level: 'S', desc: '地下暗河霸主，鳞片能吸收灵能斩击' },
  { type: '成员', name: '沈砚', level: '副会长', desc: '恶鬼之牙 · 仓库看守，掌管全部禁忌材料' },
  { type: '成员', name: '白鹭', level: '医师', desc: '恶鬼之牙 · 医疗，唯一能压住反噬的人' },
  { type: '成员', name: '陈昊宇', level: '伪神', desc: '神堂判定神弃之人，现任恶鬼之牙王牌' },
  { type: '赛程', name: '镇兽大比 · 八强赛', level: '8 强', desc: '11 月 12 日 · 神堂中央斗场' },
  { type: '赛程', name: '镇兽大比 · 半决赛', level: '4 强', desc: '11 月 16 日 · 神堂中央斗场' },
  { type: '赛程', name: '镇兽大比 · 决赛', level: '决赛', desc: '11 月 20 日 · 神堂中央斗场' }
]

/** 战力概览（示例数据） */
export const stats = [
  { label: '综合战力', value: '4 120', unit: '点', hint: '较上周 +180' },
  { label: '已习神技', value: '12', unit: '式', hint: '含 1 式禁忌' },
  { label: '战力榜', value: '第 7', unit: '位', hint: '恶鬼之牙最高' },
  { label: '公会上贡', value: '3.2', unit: '万', hint: '本周已结算' }
]

/** 神堂公告 */
export const notices = [
  {
    tag: '紧急',
    title: '神印熔铸炉将在三日后停机检修',
    time: '11 月 9 日 06:00 — 12:00'
  },
  {
    tag: '规则',
    title: '镇兽大比新增「禁用治愈类神技」条款',
    time: '11 月 8 日 起生效'
  },
  {
    tag: '通告',
    title: '恶鬼之牙仓库开放二层，需副会长以上神印',
    time: '长期有效'
  }
]

/** 镇兽大比赛程 */
export const schedule = [
  { date: '11.12', stage: '八强赛', versus: '陈昊宇 × 铁脊犀', state: '未开始' },
  { date: '11.16', stage: '半决赛', versus: '陈昊宇 × 待定', state: '待定' },
  { date: '11.20', stage: '决赛', versus: '神堂中央斗场', state: '待定' }
]

/** 神技熟练度 */
export const skillBars = [
  { name: '骨翼 · 撕裂', pct: 92, level: 'A' },
  { name: '星环瞳 · 复写', pct: 78, level: 'S' },
  { name: '吞噬 · 愈伤', pct: 64, level: 'B' },
  { name: '禁忌 · 伪神降临', pct: 31, level: 'SSS' }
]

/** 仓库条目 */
export const warehouse = [
  { name: '高纯度灵髓', count: 12, rarity: '稀有' },
  { name: '镇兽脊骨', count: 4, rarity: '史诗' },
  { name: '残缺神印', count: 27, rarity: '普通' },
  { name: '静默砂', count: 9, rarity: '稀有' }
]
