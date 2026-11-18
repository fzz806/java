package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.common.RealmLadder;
import com.falsegod.entity.Power;
import com.falsegod.entity.PowerLog;
import com.falsegod.entity.Skill;
import com.falsegod.mapper.PowerLogMapper;
import com.falsegod.mapper.PowerMapper;
import com.falsegod.mapper.SkillMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 战力业务：面板读取与五种提升途径。
 *
 * <p>数值模型（两轴推进）：
 * <ul>
 *   <li><b>大境界</b> realmIndex — 一印 → 八印【天锁八印】 → 半神 → 真神（自封之神），共 10 阶</li>
 *   <li><b>小境界</b> stage — 每阶内 4 档，需逐档递进：
 *     <ul>
 *       <li>一印~八印：初印 → 中印 → 圆满 → 巅峰</li>
 *       <li>半神：初阶 → 中阶 → 圆满 → 巅峰</li>
 *       <li>真神（自封之神）：终点位格，不再细分</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>小境界臻至巅峰（最后一档）方可冲击大境界壁垒；真神为终局，无可再破。
 *
 * <p>综合战力不落库，由 {@link #calcPower} 实时推导，
 * 保证「面板数字」「提升增量」「战力榜名次」三处永远一致。
 *
 * <p>初始档案为四印·圆满（战力 3940），刚好压过 t_member 榜首沈砚（3860），
 * 与首页「战力榜第 1」的剧情设定对得上。
 */
@Service
public class PowerService {

    private static final Logger log = LoggerFactory.getLogger(PowerService.class);

    /**
     * 大境界阶梯：一印 → 八印【天锁八印】 → 半神 → 真神（自封之神）。
     * 常量统一放在 {@link RealmLadder}，神格任务与战力档案共用同一份，杜绝两套阶位口径。
     */
    private static final String[] REALMS = RealmLadder.REALMS;

    /** 大境界别称：八印锁天、真神自封，其余留空 */
    private static final String[] REALM_MARKS = RealmLadder.REALM_MARKS;

    /** 印级（一印~八印）的小境界 */
    private static final String[] STAGES_SEAL = RealmLadder.STAGES_SEAL;

    /** 半神的小境界 */
    private static final String[] STAGES_HALF = RealmLadder.STAGES_HALF;

    /** 真神只有一个位格 */
    private static final String[] STAGES_TRUE_GOD = RealmLadder.STAGES_TRUE_GOD;

    /** 每个大境界内的小境界档数（真神除外） */
    private static final int STAGE_MAX = RealmLadder.STAGE_MAX;

    /** 灵能上限与静修回复量 */
    private static final int SPIRIT_MAX = 2000;
    private static final int REST_SPIRIT = 240;

    /** 静修每日上限：一个自然日（00:00 清零）最多回聚 5 次 */
    private static final int REST_DAILY_LIMIT = 5;

    /** 静修日志前缀，同时作为「今日已静修几次」的计数依据 */
    private static final String REST_LOG_PREFIX = "静修";

    /** 各途径灵能消耗 */
    private static final int COST_SEAL = 600;
    private static final int COST_REALM = 1600;
    private static final int COST_SKILL = 1200;
    private static final int COST_ESSENCE = 400;

    /** 战力权重：大境界 +800、小境界 +120、神技 +140、本源 +40 */
    private static final int W_REALM = 800;
    private static final int W_STAGE = 120;
    private static final int W_SKILL = 140;
    private static final int W_ESSENCE = 40;

    /** 面板「提升记录」保留条数 */
    private static final int LOG_LIMIT = 8;

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /** 静修并发锁的分段数：按 userId 取模命中同一把锁，省一个 Map 又不至于全站串行 */
    private static final int REST_LOCK_STRIPES = 64;

    /** 静修额度检查 + 落库的临界区锁 */
    private final Object[] restLocks = newRestLocks();

    private static Object[] newRestLocks() {
        Object[] locks = new Object[REST_LOCK_STRIPES];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    private final PowerMapper powerMapper;
    private final PowerLogMapper powerLogMapper;
    private final SkillMapper skillMapper;
    private final TaskService taskService;

    public PowerService(PowerMapper powerMapper, PowerLogMapper powerLogMapper,
                        SkillMapper skillMapper, TaskService taskService) {
        this.powerMapper = powerMapper;
        this.powerLogMapper = powerLogMapper;
        this.skillMapper = skillMapper;
        this.taskService = taskService;
    }

    // ==================== 对外能力 ====================

    /** 读取战力面板；无档案则惰性建档 */
    public Map<String, Object> panel(Long userId) {
        Power p = loadOrCreate(userId);
        return buildPanel(p);
    }

    /**
     * 执行一次提升。
     *
     * <p>静修走按用户加锁的路径：它的每日额度是「先数日志再写库」，
     * 并发连点会让两个请求同时看到「今日还差 1 次」而一起通过，把上限顶穿。
     * 其余动作互不争抢同一份额度，不必加锁。
     *
     * @param userId 当前登录用户
     * @param type   realm | seal | skill | essence | rest
     *               （seal 为历史动作键，语义已变为「小境界精进」，保留以兼容任务配置）
     */
    public UpgradeOutcome upgrade(Long userId, String type) {
        if (!"rest".equals(type)) {
            return doUpgrade(userId, type);
        }
        Object lock = restLocks[(int) Math.floorMod(userId == null ? 0L : userId, REST_LOCK_STRIPES)];
        synchronized (lock) {
            return doUpgrade(userId, type);
        }
    }

    /** 真正的提升逻辑；静修经 {@link #upgrade} 加锁后进入 */
    private UpgradeOutcome doUpgrade(Long userId, String type) {
        if (type == null || type.isBlank()) {
            return UpgradeOutcome.fail("未指定提升途径");
        }
        Power p = loadOrCreate(userId);
        int before = calcPower(p);

        int realmIndex = nz(p.getRealmIndex());
        int stage = nz(p.getStage());

        String logText;
        int spiritCost;
        Map<String, Object> looted = null;

        switch (type) {
            case "seal" -> {
                spiritCost = COST_SEAL;
                if (isTrueGod(realmIndex)) {
                    return UpgradeOutcome.fail("已至真神（自封之神），再无境界可精进");
                }
                if (stage >= STAGE_MAX - 1) {
                    return UpgradeOutcome.fail("小境界已至"
                            + stageName(realmIndex, stage) + "，请先突破大境界");
                }
                if (nz(p.getSpirit()) < spiritCost) {
                    return UpgradeOutcome.fail("灵能不足，需要 " + spiritCost + " 点");
                }
                p.setStage(stage + 1);
                logText = "小境界精进 · 踏入「" + stageName(realmIndex, stage + 1) + "」";
            }
            case "realm" -> {
                spiritCost = COST_REALM;
                if (isTrueGod(realmIndex)) {
                    return UpgradeOutcome.fail("已至真神，境界无可再破");
                }
                if (stage < STAGE_MAX - 1) {
                    return UpgradeOutcome.fail("小境界未至巅峰，无法冲击境界壁垒");
                }
                if (nz(p.getSpirit()) < spiritCost) {
                    return UpgradeOutcome.fail("灵能不足，需要 " + spiritCost + " 点");
                }
                p.setRealmIndex(realmIndex + 1);
                p.setStage(0);
                logText = "境界突破 · 踏入 " + fullRealm(realmIndex + 1, 0);
            }
            case "skill" -> {
                spiritCost = COST_SKILL;
                if (nz(p.getSpirit()) < spiritCost) {
                    return UpgradeOutcome.fail("灵能不足，需要 " + spiritCost + " 点");
                }
                Skill loot = randomSkill();
                if (loot != null) {
                    looted = new LinkedHashMap<>();
                    looted.put("name", loot.getName());
                    looted.put("level", loot.getLevel());
                    looted.put("element", loot.getElement());
                    looted.put("desc", loot.getDesc());
                    logText = "掠夺神技 · " + loot.getName();
                } else {
                    logText = "掠夺神技 · 得残缺一式";
                }
                p.setSkills(nz(p.getSkills()) + 1);
            }
            case "essence" -> {
                spiritCost = COST_ESSENCE;
                if (nz(p.getSpirit()) < spiritCost) {
                    return UpgradeOutcome.fail("灵能不足，需要 " + spiritCost + " 点");
                }
                p.setEssence(nz(p.getEssence()) + 1);
                logText = "本源吞噬 · 第 " + p.getEssence() + " 枚归位";
            }
            case "rest" -> {
                spiritCost = 0;
                // 每日额度先判：超额直接拒绝，既不回灵能、也不推进「静修」类日课
                int usedToday = restUsedToday(userId);
                if (usedToday >= REST_DAILY_LIMIT) {
                    return UpgradeOutcome.fail(
                            "今日静修已达上限（" + REST_DAILY_LIMIT + " 次），明日再来");
                }
                if (nz(p.getSpirit()) >= nz(p.getSpiritMax())) {
                    return UpgradeOutcome.fail("灵能已满，无需静修");
                }
                int after = Math.min(nz(p.getSpiritMax()), nz(p.getSpirit()) + REST_SPIRIT);
                int real = after - nz(p.getSpirit());
                p.setSpirit(after);
                logText = REST_LOG_PREFIX + " · 灵能回聚 +" + real;
            }
            default -> {
                return UpgradeOutcome.fail("未知的提升途径：" + type);
            }
        }

        // 扣灵能并落库
        p.setSpirit(Math.max(0, nz(p.getSpirit()) - spiritCost));
        p.setUpdatedAt(LocalDateTime.now());
        powerMapper.updateById(p);

        int after = calcPower(p);
        int gain = after - before;

        writeLog(userId, logText, gain);

        // 推进任务进度。战力已经落库，任务记账失败不该回滚、也不该让玩家重复消耗灵能，
        // 所以这里兜住异常只记日志——下一动作会继续累加，进度不会丢。
        try {
            taskService.onUpgrade(userId, type);
            // 大境界突破后把神格经验补到该阶起点：修行页与任务页必须永远是同一个阶位，
            // 否则会出现「修行四印、任务页还是二阶」这类自相矛盾。
            if ("realm".equals(type)) {
                taskService.syncRealmExp(userId, nz(p.getRealmIndex()));
            }
        } catch (RuntimeException e) {
            log.warn("推进任务进度/同步阶位失败 userId={} type={}", userId, type, e);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("panel", buildPanel(p));
        data.put("gain", gain);
        if (looted != null) {
            data.put("looted", looted);
        }
        return UpgradeOutcome.ok(data);
    }

    // ==================== 内部实现 ====================

    /** 综合战力：大境界为主，小境界/神技/本源为辅 */
    private int calcPower(Power p) {
        return nz(p.getRealmIndex()) * W_REALM
                + nz(p.getStage()) * W_STAGE
                + nz(p.getSkills()) * W_SKILL
                + nz(p.getEssence()) * W_ESSENCE;
    }

    /** 大境界下标收敛到合法区间 */
    private int clampRealm(int index) {
        return Math.max(0, Math.min(index, REALMS.length - 1));
    }

    /** 是否已至终局「真神」 */
    private boolean isTrueGod(int realmIndex) {
        return clampRealm(realmIndex) >= REALMS.length - 1;
    }

    /** 该大境界的小境界名册 */
    private String[] stagesOf(int realmIndex) {
        int ri = clampRealm(realmIndex);
        if (isTrueGod(ri)) {
            return STAGES_TRUE_GOD;
        }
        if (ri == REALMS.length - 2) {   // 半神
            return STAGES_HALF;
        }
        return STAGES_SEAL;
    }

    /** 大境界名，如「四印」 */
    private String realmName(int realmIndex) {
        return REALMS[clampRealm(realmIndex)];
    }

    /** 小境界名，如「圆满」 */
    private String stageName(int realmIndex, int stage) {
        String[] stages = stagesOf(realmIndex);
        int i = Math.max(0, Math.min(stage, stages.length - 1));
        return stages[i];
    }

    /** 完整境界名：「四印 · 圆满」「八印【天锁八印】 · 巅峰」「真神（自封之神）」 */
    private String fullRealm(int realmIndex, int stage) {
        return RealmLadder.fullRealm(realmIndex, stage);
    }

    /** 下一档境界名；已至真神则返回终点提示 */
    private String nextRealm(int realmIndex, int stage) {
        int ri = clampRealm(realmIndex);
        if (isTrueGod(ri)) {
            return "已至真神";
        }
        if (stage < STAGE_MAX - 1) {
            return fullRealm(ri, stage + 1);
        }
        return fullRealm(ri + 1, 0);
    }

    /** 组装与前端 PowerUpPage.vue 约定一致的面板结构 */
    private Map<String, Object> buildPanel(Power p) {
        int realmIndex = clampRealm(nz(p.getRealmIndex()));
        int stage = isTrueGod(realmIndex) ? 0 : Math.min(nz(p.getStage()), STAGE_MAX - 1);
        int stageMax = stagesOf(realmIndex).length;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("power", calcPower(p));
        m.put("realm", fullRealm(realmIndex, stage));
        m.put("realmName", realmName(realmIndex));
        m.put("realmMark", REALM_MARKS[realmIndex]);
        m.put("stage", stage);
        m.put("stageName", stageName(realmIndex, stage));
        m.put("stageMax", stageMax);
        m.put("nextRealm", nextRealm(realmIndex, stage));
        // realmLevel / realmMax 供进度条使用：当前处于本大境界第几档 / 共几档
        m.put("realmLevel", stage + 1);
        m.put("realmMax", stageMax);
        m.put("spirit", nz(p.getSpirit()));
        m.put("spiritMax", nz(p.getSpiritMax()));
        // seal / sealMax 为旧字段别名，保持前端向后兼容
        m.put("seal", stage);
        m.put("sealMax", stageMax);
        m.put("skills", nz(p.getSkills()));
        m.put("essence", nz(p.getEssence()));
        // 静修每日额度：面板与动作都带上，前端不必自己数。
        // 历史数据可能已超上限（改规则前刷的），对外一律夹到上限，免得显示成「今日 22/5」。
        int restUsed = Math.min(restUsedToday(p.getUserId()), REST_DAILY_LIMIT);
        m.put("restUsed", restUsed);
        m.put("restLimit", REST_DAILY_LIMIT);
        m.put("restLeft", Math.max(0, REST_DAILY_LIMIT - restUsed));
        m.put("actions", buildActions(p, restUsed));
        m.put("logs", recentLogs(p.getUserId()));
        return m;
    }

    /** 五条提升途径；available 由当前资源实时判定，前端据此置灰按钮 */
    private List<Map<String, Object>> buildActions(Power p, int restUsed) {
        int realmIndex = clampRealm(nz(p.getRealmIndex()));
        int stage = nz(p.getStage());
        boolean trueGod = isTrueGod(realmIndex);
        String ladder = String.join(" → ", stagesOf(realmIndex));

        List<Map<String, Object>> list = new ArrayList<>();
        list.add(action("realm", "境界突破",
                "以灵能冲击大境界壁垒，小境界臻至巅峰方可叩关，破则踏入下一境。",
                W_REALM - (STAGE_MAX - 1) * W_STAGE, COST_REALM,
                "大境界 +1",
                !trueGod && stage >= STAGE_MAX - 1 && nz(p.getSpirit()) >= COST_REALM));
        list.add(action("seal", "小境界精进",
                "反复打磨己身，小境界层层递进：" + ladder + "。",
                W_STAGE, COST_SEAL,
                "小境界 +1 档",
                !trueGod && stage < STAGE_MAX - 1 && nz(p.getSpirit()) >= COST_SEAL));
        list.add(action("skill", "掠夺神技",
                "从败者身上剥离一式神技，据为己有。",
                W_SKILL, COST_SKILL,
                "神技 +1 式", nz(p.getSpirit()) >= COST_SKILL));
        list.add(action("essence", "本源吞噬",
                "吞下本源结晶，将他人道果炼入己身。",
                W_ESSENCE, COST_ESSENCE,
                "本源 +1 枚", nz(p.getSpirit()) >= COST_ESSENCE));
        int restLeft = Math.max(0, REST_DAILY_LIMIT - restUsed);
        Map<String, Object> rest = action("rest", "静修",
                "端坐废殿，缓慢回聚灵能。不增战力，却是唯一无耗之举；每日限 "
                        + REST_DAILY_LIMIT + " 次，子时重置。",
                0, 0,
                restLeft > 0
                        ? "灵能 +" + REST_SPIRIT + " · 今日 " + restUsed + "/" + REST_DAILY_LIMIT
                        : "今日已满 " + REST_DAILY_LIMIT + "/" + REST_DAILY_LIMIT,
                restLeft > 0 && nz(p.getSpirit()) < nz(p.getSpiritMax()));
        rest.put("used", restUsed);
        rest.put("limit", REST_DAILY_LIMIT);
        rest.put("left", restLeft);
        list.add(rest);
        return list;
    }

    /**
     * 今日已静修几次：直接数当天 {@code t_power_log} 里以「静修」开头的记录。
     *
     * <p>不新增计数字段/表——静修成功必写一条日志、失败不写，所以日志条数天然等于次数；
     * 自然日按应用时钟取 00:00 为界，跨天自动归零。
     */
    private int restUsedToday(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long n = powerLogMapper.selectCount(new LambdaQueryWrapper<PowerLog>()
                .eq(PowerLog::getUserId, userId)
                .likeRight(PowerLog::getText, REST_LOG_PREFIX)
                .ge(PowerLog::getCreatedAt, LocalDate.now().atStartOfDay()));
        return n == null ? 0 : n.intValue();
    }

    private Map<String, Object> action(String key, String label, String desc,
                                       int gain, int cost, String effect, boolean available) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("key", key);
        a.put("label", label);
        a.put("desc", desc);
        a.put("gain", gain);
        a.put("cost", cost);
        a.put("effect", effect);
        a.put("available", available);
        return a;
    }

    private List<Map<String, Object>> recentLogs(Long userId) {
        List<PowerLog> rows = powerLogMapper.selectList(
                new LambdaQueryWrapper<PowerLog>()
                        .eq(PowerLog::getUserId, userId)
                        .orderByDesc(PowerLog::getCreatedAt)
                        .orderByDesc(PowerLog::getId)
                        .last("limit " + LOG_LIMIT)
        );
        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (PowerLog r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time", r.getCreatedAt() == null ? "" : r.getCreatedAt().format(LOG_TIME));
            m.put("text", r.getText());
            m.put("gain", r.getGain() == null ? 0 : r.getGain());
            list.add(m);
        }
        return list;
    }

    private void writeLog(Long userId, String text, int gain) {
        PowerLog log = new PowerLog();
        log.setUserId(userId);
        log.setText(text);
        log.setGain(gain);
        log.setCreatedAt(LocalDateTime.now());
        powerLogMapper.insert(log);
    }

    private Skill randomSkill() {
        List<Skill> skills = skillMapper.selectList(null);
        if (skills.isEmpty()) {
            return null;
        }
        return skills.get(ThreadLocalRandom.current().nextInt(skills.size()));
    }

    /**
     * 读取档案，没有就按初始值建一份。
     *
     * <p>初始值：四印·圆满、灵能 900/2000、神技 7 式、本源 8 枚 → 战力 3940，
     * 高过战力榜首沈砚（3860），即开局第 1。
     */
    private Power loadOrCreate(Long userId) {
        Power p = powerMapper.selectOne(
                new LambdaQueryWrapper<Power>().eq(Power::getUserId, userId)
        );
        if (p != null) {
            normalize(p);
            return p;
        }
        p = new Power();
        p.setUserId(userId);
        p.setRealmIndex(RealmLadder.DEFAULT_REALM_INDEX);   // 四印
        p.setStage(RealmLadder.DEFAULT_STAGE);              // 圆满
        p.setSpirit(900);
        p.setSpiritMax(SPIRIT_MAX);
        p.setSkills(7);
        p.setEssence(8);
        p.setUpdatedAt(LocalDateTime.now());
        powerMapper.insert(p);
        return p;
    }

    /**
     * 兼容旧档案：境界下标越界、小境界越界的，收敛回合法区间。
     * 旧模型（红印 0-9 重）遗留数据在这里被安全折到 0-3 档，不会抛异常。
     */
    private void normalize(Power p) {
        if (p.getRealmIndex() == null || p.getRealmIndex() < 0 || p.getRealmIndex() >= REALMS.length) {
            p.setRealmIndex(clampRealm(nz(p.getRealmIndex())));
        }
        int maxStage = isTrueGod(nz(p.getRealmIndex())) ? 0 : STAGE_MAX - 1;
        if (p.getStage() == null || p.getStage() < 0 || p.getStage() > maxStage) {
            p.setStage(Math.max(0, Math.min(nz(p.getStage()), maxStage)));
        }
        if (p.getSpiritMax() == null || p.getSpiritMax() <= 0) {
            p.setSpiritMax(SPIRIT_MAX);
        }
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 提升结果：成功带数据，失败带人话提示（前端直接弹 toast） */
    public static class UpgradeOutcome {

        private final boolean ok;
        private final String message;
        private final Map<String, Object> data;

        private UpgradeOutcome(boolean ok, String message, Map<String, Object> data) {
            this.ok = ok;
            this.message = message;
            this.data = data;
        }

        public static UpgradeOutcome ok(Map<String, Object> data) {
            return new UpgradeOutcome(true, "ok", data);
        }

        public static UpgradeOutcome fail(String message) {
            return new UpgradeOutcome(false, message, null);
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }
}
