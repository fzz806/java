package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.entity.Power;
import com.falsegod.entity.PowerLog;
import com.falsegod.entity.Skill;
import com.falsegod.mapper.PowerLogMapper;
import com.falsegod.mapper.PowerMapper;
import com.falsegod.mapper.SkillMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
 *   <li><b>境界</b> realmIndex — 一印到九印，共 9 阶</li>
 *   <li><b>红印</b> seal — 当前境界内的重数 0-9，满 9 才能突破</li>
 * </ul>
 *
 * <p>综合战力不落库，由 {@link #calcPower} 实时推导，
 * 保证「面板数字」「提升增量」「战力榜名次」三处永远一致。
 *
 * <p>初始档案的战力刻意贴着 t_member 榜首（陈昊宇 4120），
 * 让首页「战力榜第 2」与剧情设定对得上，练几次即可反超。
 */
@Service
public class PowerService {

    private static final Logger log = LoggerFactory.getLogger(PowerService.class);

    /** 境界阶梯：满 9 重红印后突破到下一印 */
    private static final String[] REALMS = {"一印", "二印", "三印", "四印", "五印", "六印", "七印", "八印", "九印"};

    /** 每个境界内的红印重数上限 */
    private static final int REALM_MAX = 9;

    /** 灵能上限与静修回复量 */
    private static final int SPIRIT_MAX = 2000;
    private static final int REST_SPIRIT = 240;

    /** 各途径灵能消耗 */
    private static final int COST_SEAL = 600;
    private static final int COST_REALM = 1600;
    private static final int COST_SKILL = 1200;
    private static final int COST_ESSENCE = 400;

    /** 战力权重：突破 +800、红印 +40、神技 +140、本源 +40 */
    private static final int W_REALM = 800;
    private static final int W_SEAL = 40;
    private static final int W_SKILL = 140;
    private static final int W_ESSENCE = 40;

    /** 面板「提升记录」保留条数 */
    private static final int LOG_LIMIT = 8;

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

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
     * @param userId 当前登录用户
     * @param type   realm | seal | skill | essence | rest
     */
    public UpgradeOutcome upgrade(Long userId, String type) {
        if (type == null || type.isBlank()) {
            return UpgradeOutcome.fail("未指定提升途径");
        }
        Power p = loadOrCreate(userId);
        int before = calcPower(p);

        String logText;
        int spiritCost;
        Map<String, Object> looted = null;

        switch (type) {
            case "seal" -> {
                spiritCost = COST_SEAL;
                if (p.getSeal() >= REALM_MAX) {
                    return UpgradeOutcome.fail("红印已满 " + REALM_MAX + " 重，请先突破境界");
                }
                if (p.getSpirit() < spiritCost) {
                    return UpgradeOutcome.fail("灵能不足，需要 " + spiritCost + " 点");
                }
                p.setSeal(p.getSeal() + 1);
                logText = "红印淬炼 · 第 " + p.getSeal() + " 重已成";
            }
            case "realm" -> {
                spiritCost = COST_REALM;
                if (p.getRealmIndex() >= REALMS.length - 1) {
                    return UpgradeOutcome.fail("已至九印，境界无可再破");
                }
                if (p.getSeal() < REALM_MAX) {
                    return UpgradeOutcome.fail("红印未满 " + REALM_MAX + " 重，无法冲击境界壁垒");
                }
                if (p.getSpirit() < spiritCost) {
                    return UpgradeOutcome.fail("灵能不足，需要 " + spiritCost + " 点");
                }
                p.setRealmIndex(p.getRealmIndex() + 1);
                p.setSeal(0);
                logText = "境界突破 · 踏入 " + realmName(p.getRealmIndex());
            }
            case "skill" -> {
                spiritCost = COST_SKILL;
                if (p.getSpirit() < spiritCost) {
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
                p.setSkills(p.getSkills() + 1);
            }
            case "essence" -> {
                spiritCost = COST_ESSENCE;
                if (p.getSpirit() < spiritCost) {
                    return UpgradeOutcome.fail("灵能不足，需要 " + spiritCost + " 点");
                }
                p.setEssence(p.getEssence() + 1);
                logText = "本源吞噬 · 第 " + p.getEssence() + " 枚归位";
            }
            case "rest" -> {
                spiritCost = 0;
                if (p.getSpirit() >= p.getSpiritMax()) {
                    return UpgradeOutcome.fail("灵能已满，无需静修");
                }
                int after = Math.min(p.getSpiritMax(), p.getSpirit() + REST_SPIRIT);
                int real = after - p.getSpirit();
                p.setSpirit(after);
                logText = "静修 · 灵能回聚 +" + real;
            }
            default -> {
                return UpgradeOutcome.fail("未知的提升途径：" + type);
            }
        }

        // 扣灵能并落库
        p.setSpirit(Math.max(0, p.getSpirit() - spiritCost));
        p.setUpdatedAt(LocalDateTime.now());
        powerMapper.updateById(p);

        int after = calcPower(p);
        int gain = after - before;

        writeLog(userId, logText, gain);

        // 推进任务进度。战力已经落库，任务记账失败不该回滚、也不该让玩家重复消耗灵能，
        // 所以这里兜住异常只记日志——下一动作会继续累加，进度不会丢。
        try {
            taskService.onUpgrade(userId, type);
        } catch (RuntimeException e) {
            log.warn("推进任务进度失败 userId={} type={}", userId, type, e);
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

    /** 综合战力：境界为主，红印/神技/本源为辅 */
    private int calcPower(Power p) {
        return p.getRealmIndex() * W_REALM
                + p.getSeal() * W_SEAL
                + p.getSkills() * W_SKILL
                + p.getEssence() * W_ESSENCE;
    }

    private String realmName(int index) {
        int i = Math.max(0, Math.min(index, REALMS.length - 1));
        return REALMS[i];
    }

    /** 组装与前端 PowerUpPage.vue 约定一致的面板结构 */
    private Map<String, Object> buildPanel(Power p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("power", calcPower(p));
        m.put("realm", realmName(p.getRealmIndex()));
        m.put("nextRealm", realmName(p.getRealmIndex() + 1));
        m.put("realmLevel", p.getSeal());
        m.put("realmMax", REALM_MAX);
        m.put("spirit", p.getSpirit());
        m.put("spiritMax", p.getSpiritMax());
        m.put("seal", p.getSeal());
        m.put("sealMax", REALM_MAX);
        m.put("skills", p.getSkills());
        m.put("essence", p.getEssence());
        m.put("actions", buildActions(p));
        m.put("logs", recentLogs(p.getUserId()));
        return m;
    }

    /** 五条提升途径；available 由当前资源实时判定，前端据此置灰按钮 */
    private List<Map<String, Object>> buildActions(Power p) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(action("realm", "境界突破",
                "以灵能冲击印级壁垒，红印满 " + REALM_MAX + " 重方可叩关，破则踏入下一印。",
                W_REALM - REALM_MAX * W_SEAL, COST_REALM,
                "境界 +1", p.getRealmIndex() < REALMS.length - 1
                        && p.getSeal() >= REALM_MAX && p.getSpirit() >= COST_REALM));
        list.add(action("seal", "红印淬炼",
                "反复锤打野生红印，每成一重，战力随印纹一同攀升。",
                W_SEAL, COST_SEAL,
                "红印 +1 重", p.getSeal() < REALM_MAX && p.getSpirit() >= COST_SEAL));
        list.add(action("skill", "掠夺神技",
                "从败者身上剥离一式神技，据为己有。",
                W_SKILL, COST_SKILL,
                "神技 +1 式", p.getSpirit() >= COST_SKILL));
        list.add(action("essence", "本源吞噬",
                "吞下本源结晶，将他人道果炼入己身。",
                W_ESSENCE, COST_ESSENCE,
                "本源 +1 枚", p.getSpirit() >= COST_ESSENCE));
        list.add(action("rest", "静修",
                "端坐废殿，缓慢回聚灵能。不增战力，却是唯一无耗之举。",
                0, 0,
                "灵能 +" + REST_SPIRIT, p.getSpirit() < p.getSpiritMax()));
        return list;
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
     * <p>初始值：三印八重、灵能 900/2000、神技 7 式、本源 8 枚 → 战力 4020，
     * 略低于战力榜首（4120），练几次即可反超。
     */
    private Power loadOrCreate(Long userId) {
        Power p = powerMapper.selectOne(
                new LambdaQueryWrapper<Power>().eq(Power::getUserId, userId)
        );
        if (p != null) {
            return p;
        }
        p = new Power();
        p.setUserId(userId);
        p.setRealmIndex(3);
        p.setSeal(8);
        p.setSpirit(900);
        p.setSpiritMax(SPIRIT_MAX);
        p.setSkills(7);
        p.setEssence(8);
        p.setUpdatedAt(LocalDateTime.now());
        powerMapper.insert(p);
        return p;
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
