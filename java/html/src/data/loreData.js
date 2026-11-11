/**
 * 万神大厅 · 世界观图鉴数据
 *
 * 「觉醒伪神」核心设定，共六卷：
 *   修为等级 / 元素异能 / 神印谱 / 伪神核心 / 势力地点 / 特殊力量
 * 纯前端数据，后续需要时再接后端。
 */

/** 大厅分类（标签页顺序）
 *  awake: true 表示该卷需觉醒（登临 / 铸印）后方可翻阅
 */
export const loreTabs = [
  { key: 'realms', label: '修为等级', en: 'REALMS' },
  { key: 'normal', label: '普通元素', en: 'ELEMENTS' },
  { key: 'special', label: '特殊系', en: 'SPECIAL' },
  { key: 'seals', label: '神印谱', en: 'SEALS', awake: true },
  { key: 'protagonist', label: '伪神核心', en: 'PROTAGONIST' },
  { key: 'factions', label: '势力地点', en: 'FACTIONS' },
  { key: 'powers', label: '特殊力量', en: 'POWERS' }
]

/* ================= 一、修为等级 =================
 * 凡人 → 一印 ~ 八印【天锁八印】 → 半神 → 真神（自封之神）
 */
export const realms = [
  { name: '凡人', tier: '凡阶', desc: '尚未觉醒，未铸神印', mark: '' },
  { name: '一印', tier: '初启', desc: '烙下第一枚神印，踏入修行', mark: '' },
  { name: '二印', tier: '渐深', desc: '双印共鸣，可施展基础神技', mark: '' },
  { name: '三印', tier: '立足', desc: '三印之躯，小镇之地已可称强者', mark: '' },
  { name: '四印', tier: '成名', desc: '四印圆满，权贵门阀的入场门槛', mark: '' },
  { name: '五印', tier: '一方', desc: '五印威压，足以坐镇一城', mark: '' },
  { name: '六印', tier: '擎天', desc: '六印强者，神堂名录在册', mark: '' },
  { name: '七印', tier: '传说', desc: '七印之境，寻常岁月难以企及', mark: '' },
  { name: '八印', tier: '禁忌', desc: '天锁八印，肉身即神狱，凡躯锁天门', mark: '天锁八印' },
  { name: '半神', tier: '临神', desc: '半步神域，只差最后一场造化', mark: '' },
  { name: '真神', tier: '终极', desc: '不受神堂册封，自己给自己的神格', mark: '自封之神' }
]

/* ================= 二、元素 & 异能体系 ================= */
/** 普通元素（六系，各自带主题色） */
export const normalElements = [
  { name: '火', en: 'PYRO', color: '#ff6b4a', desc: '焚尽，最狂暴的进攻元素' },
  { name: '冰', en: 'CRYO', color: '#7fc4ff', desc: '凝滞，冻结灵能与血肉' },
  { name: '雷', en: 'VOLTA', color: '#b48cff', desc: '霹雳，速度与毁灭并存' },
  { name: '风', en: 'AERO', color: '#8fe8c0', desc: '疾行，身法与切割之风' },
  { name: '土', en: 'TERRA', color: '#d8a56a', desc: '厚重，防御与大地之力' },
  { name: '水', en: 'AQUA', color: '#5aa8e8', desc: '润泽，绵长不绝的韧性' }
]

/** 特殊系（六大异路） */
export const specialElements = [
  {
    name: '光明系', color: '#ffe9a8', tag: '神堂正统',
    desc: '神堂、神子专属，力量直接由诸神赐下'
  },
  {
    name: '黑暗 / 恶鬼系', color: '#ff5a5a', tag: '恶鬼之牙',
    desc: '噬魂、鬼爪，恶鬼之牙公会的看家路数'
  },
  {
    name: '兽系', color: '#e8a04a', tag: '肉身流',
    desc: '兽变之路，肉身狂暴，以蛮力碾压神技'
  },
  {
    name: '精神系', color: '#c48cff', tag: '无形之刃',
    desc: '精神冲击、意念操控，杀人于无形'
  },
  {
    name: '空间系', color: '#7fe8e0', tag: '稀有', rare: true,
    desc: '位移、空间斩，万里之外取人首级'
  },
  {
    name: '植物系', color: '#8fe07a', tag: '生机',
    desc: '控草木、束缚、治疗，战场上最被低估的一系'
  }
]

/* ================= 三、神印颜色 & 属性 ================= */
export const seals = [
  {
    name: '金 / 白印', color: '#f2d98c', sub: '#f8f4e6', tag: '正统神赐',
    desc: '光明属性，神堂神子的专属，诸神亲赐的神格凭证'
  },
  {
    name: '绿印', color: '#8fe07a', sub: '#3a7a3a', tag: '生命',
    desc: '生命、治愈、植物，医护与束缚之道'
  },
  {
    name: '蓝印', color: '#6ab0f2', sub: '#2a4a8a', tag: '水冰精神',
    desc: '水、冰、精神三系同源，静水流深'
  },
  {
    name: '红印', color: '#ff4d4d', sub: '#7a1414', tag: '男主专属', rare: true,
    desc: '野生禁忌，魔与恶鬼之印，神堂明令禁绝——却是神弃者唯一的路'
  },
  {
    name: '黑印', color: '#3a3448', sub: '#12101a', tag: '深渊禁忌',
    desc: '深渊与古神禁忌之力，印成之日，神堂缉杀令随之而来'
  }
]

/* ================= 四、男主 · 伪神核心 ================= */
export const protagonist = {
  talent: 'SSS 级【伪神】',
  talentDesc: '神弃者，无法镶嵌正统神印，只能以野生红神印为凭——诸神拒收之人，便自己给自己封神。',
  motto: '不靠神赐，自我封神',
  transforms: [
    {
      name: '神变', en: 'DIVINE', color: '#f2d98c',
      desc: '借诸神权柄而行，神罚类攻击，以神之名审判神'
    },
    {
      name: '魔变', en: 'DEMONIC', color: '#ff5a5a',
      desc: '禁忌魔力灌体，破坏力极强，禁忌之路的代价与锋芒'
    },
    {
      name: '兽变', en: 'BEAST', color: '#e8a04a',
      desc: '肉身狂暴，刚虎吞天，一息之间撕碎神印护壁'
    },
    {
      name: '鬼变', en: 'WRAITH', color: '#9a7fe8',
      desc: '噬魂鬼爪，直取灵魂，神印护不住的地方它够得到'
    }
  ],
  coreAbilities: ['解析神印', '掠夺权柄', '改写力量', '神魔同流', '打破神明规则', '自我封神']
}

/* ================= 五、势力 & 关键地点 =================
 * 分两组：公会势力（人的阵营） / 试炼赛事（地的舞台）
 */
/** 公会与阵营 */
export const guildFactions = [
  {
    name: '神堂', tag: '正统神权', stance: '对立', color: '#f2d98c',
    desc: '掌控神印铸嵌的至高权柄，明面上维系秩序，暗地里打压一切神弃者与伪神'
  },
  {
    name: '恶鬼之牙公会', tag: '公会 · 男主归属', stance: '阵营', color: '#ff5a5a',
    desc: '接纳神弃者的庇护所，护短成性、全员恶人，仓库里囤着半个大陆的禁忌'
  }
]

/** 试炼与赛事 */
export const contests = [
  {
    name: '镇兽塔', tag: '试炼之地', stance: '闯关', color: '#e8a04a',
    desc: '层层闯关的镇兽试炼，胜者获取妖兽权柄与镇兽本源'
  },
  {
    name: '镇兽大比', tag: '年轻一辈盛事', stance: '比武', color: '#7fe8e0',
    desc: '年轻一辈的巅峰比武场，寒门打脸权贵的唯一明面舞台'
  },
  {
    name: '古神战场 / 秘境', tag: '高阶禁地', stance: '秘境', color: '#9a7fe8',
    desc: '旧神陨落之地，埋藏古神遗物，只有高阶强者才敢踏入的厮杀场'
  }
]

/** 兼容旧引用：公会 + 赛事合并 */
export const factions = [...guildFactions, ...contests]

/* ================= 六、其他特殊力量 ================= */
export const powers = [
  {
    name: '神子权柄', tag: '神堂庇护', color: '#ffe9a8',
    desc: '神明直接赐下的权柄，持柄者受神堂全力庇护，生来高人一等'
  },
  {
    name: '古神力量', tag: '神堂禁忌', color: '#3a3448',
    desc: '旧时代残留的恐怖伟力，被神堂列为头号禁忌，沾之即被缉杀'
  },
  {
    name: '魂系力量', tag: '恶鬼常用', color: '#9a7fe8',
    desc: '操控亡魂、噬魂夺魄，恶鬼之牙公会最阴冷的一路力量'
  },
  {
    name: '镇兽本源', tag: '闯关获取', color: '#e8a04a',
    desc: '镇兽塔妖兽的本源之力，逐层闯关方可摘取，兽系修士梦寐以求'
  }
]
