package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.falsegod.entity.ExpLog;
import com.falsegod.entity.Task;
import com.falsegod.entity.UserExp;
import com.falsegod.entity.UserTask;
import com.falsegod.mapper.ExpLogMapper;
import com.falsegod.mapper.TaskMapper;
import com.falsegod.mapper.UserExpMapper;
import com.falsegod.mapper.UserTaskMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 神格任务与阶位经验。
 *
 * <p><b>经验与等级</b>：只落 {@code t_user_exp.total_exp} 一个数字，
 * 阶位由 {@link #levelOf} 实时推导。升级门槛为
 * {@code 100 + (level-1) * 80}，一到九阶累计 3780 点，十阶封顶。
 *
 * <p><b>进度</b>由真实操作驱动：战力模块每次提升成功后调用
 * {@link #onUpgrade}，按 action 推进对应任务；日课「每日登临」在打开
 * 任务面板时结算。没有任何接口能直接把任务改成已完成。
 *
 * <p><b>重置</b>靠 {@code task_date}：日课用当天日期，跨天自动多出一行，
 * 等于免费重置；主线固定 1970-01-01，一人一条永不重置。
 *
 * <p><b>防重复领取</b>：领取采用「条件更新 + 受影响行数」，
 * 并发双击只有一次能改成 claimed_at，另一次返回已领取。
 *
 * <p><b>每日额度</b>：日课池共 10 条，每天最多领取 {@link #DAILY_CLAIM_LIMIT} 个，
 * 做满 10 条即额度用尽，次日零点自然归零。主线是一次性功业（终身各领一次），
 * <b>不占额度、也不受额度限制</b>。
 *
 * <p>额度口径为「{@code claimed_at} 落在今天且 {@code task_date} = 今天」：
 * 日课的 task_date 恒为当天，主线的 task_date 固定为 {@link #MAIN_DATE}，
 * 因此不需要任何额外字段就能把两类分开，也不必联表查 category。
 */
@Service
public class TaskService {

    /** 阶位名，下标 0 = 一阶 */
    private static final String[] TITLES = {
            "弃子", "拾印者", "淬印者", "窥神者", "伪神候补",
            "半神", "伪神", "真神之影", "觉醒者", "神明"
    };

    /** 阶位上限，到顶后经验不再累计 */
    private static final int LEVEL_MAX = TITLES.length;

    /** 升级门槛：第 n 阶升 n+1 阶需 EXP_BASE + (n-1) * EXP_STEP */
    private static final int EXP_BASE = 100;
    private static final int EXP_STEP = 80;

    /** 主线任务的占位日期（只为满足唯一索引，不参与重置） */
    private static final LocalDate MAIN_DATE = LocalDate.of(1970, 1, 1);

    /** 「任意提升」类任务的动作标识 */
    private static final String ACTION_OVERALL = "upgrade";

    /** 经验流水保留条数 */
    private static final int LOG_LIMIT = 6;

    /**
     * 每日可领取的日课数上限（与日课池条数一致），次日零点自动归零。
     *
     * <p>只统计日课的领取次数：日课与主线用 task_date 区分（日课=当天，主线=固定占位日期），
     * 所以「今天能领几个」与主线进度无关。将来往日课池里加任务，
     * 记得同步这个数字，否则要么提前卡额度、要么额度永远用不完。
     */
    static final int DAILY_CLAIM_LIMIT = 10;

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final TaskMapper taskMapper;
    private final UserTaskMapper userTaskMapper;
    private final UserExpMapper userExpMapper;
    private final ExpLogMapper expLogMapper;

    public TaskService(TaskMapper taskMapper, UserTaskMapper userTaskMapper,
                       UserExpMapper userExpMapper, ExpLogMapper expLogMapper) {
        this.taskMapper = taskMapper;
        this.userTaskMapper = userTaskMapper;
        this.userExpMapper = userExpMapper;
        this.expLogMapper = expLogMapper;
    }

    // ==================== 对外能力 ====================

    /** 任务面板：阶位 + 经验 + 任务列表 + 最近所得 */
    public Map<String, Object> panel(Long userId) {
        // 打开任务面板即视为今日登临，日课第一条自动结算
        onAction(userId, "login");
        return buildPanel(userId);
    }

    private Map<String, Object> buildPanel(Long userId) {
        UserExp ue = loadOrCreateExp(userId);
        LevelInfo level = levelOf(ue.getTotalExp());

        List<Task> tasks = listEnabled();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> list = new ArrayList<>(tasks.size());
        int claimable = 0;
        int dailyTotal = 0;
        int dailyClaimed = 0;
        int mainTotal = 0;
        int mainClaimed = 0;

        for (Task t : tasks) {
            UserTask row = findRow(userId, t.getId(), dateOf(t, today));
            Map<String, Object> m = taskView(t, row);
            String status = (String) m.get("status");
            boolean daily = isDaily(t);
            if (daily) {
                dailyTotal++;
                if ("claimed".equals(status)) {
                    dailyClaimed++;
                }
            } else {
                mainTotal++;
                if ("claimed".equals(status)) {
                    mainClaimed++;
                }
            }
            if ("claimable".equals(status)) {
                claimable++;
            }
            list.add(m);
        }

        int claimedToday = countClaimedToday(userId);
        int remaining = Math.max(0, DAILY_CLAIM_LIMIT - claimedToday);

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(level.toMap());
        data.put("claimable", claimable);
        data.put("dailyTotal", dailyTotal);
        data.put("dailyClaimed", dailyClaimed);
        data.put("mainTotal", mainTotal);
        data.put("mainClaimed", mainClaimed);
        // 每日额度：只针对日课，按「今天领过几次日课」统计；主线不占额度
        data.put("claimLimit", DAILY_CLAIM_LIMIT);
        data.put("claimedToday", claimedToday);
        data.put("remainingClaim", remaining);
        data.put("limitReached", remaining <= 0);
        data.put("tasks", list);
        data.put("logs", recentLogs(userId));
        return data;
    }

    /**
     * 领取任务经验。
     *
     * <p>整个领取在一个事务里完成（任务置为已领 + 经验入账 + 写流水），
     * 任一步失败都不会留下「任务已领但经验没到」的半成品。
     *
     * <p>经验行用 {@code for update} 加锁，使同一用户的领取串行化。隔离级别特意定为
     * {@code READ_COMMITTED}：MySQL 默认的 REPEATABLE READ 下，等锁结束后普通读仍用
     * 事务开始时的旧快照，会读到别人领取之前的额度，导致每日上限被突破。
     *
     * @param taskId t_task.id
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ClaimOutcome claim(Long userId, Long taskId) {
        if (taskId == null) {
            return ClaimOutcome.fail("未指定要领取的任务");
        }

        // 先锁经验行，把「查额度 → 判定 → 加经验」放进同一个临界区。
        // 否则同一账号在两台设备上同时领不同任务时，两边都会读到「还剩 1 次」而双双通过。
        UserExp ue = loadOrCreateExp(userId);
        UserExp locked = userExpMapper.selectOne(new LambdaQueryWrapper<UserExp>()
                .eq(UserExp::getUserId, userId)
                .last("limit 1 for update"));
        if (locked != null) {
            ue = locked;
        }

        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getEnabled() == null || task.getEnabled() != 1) {
            return ClaimOutcome.fail("任务不存在或已下线");
        }

        // 每日额度只约束日课（日课池 10 条）；主线是一次性功业，不占额度也不受额度限制。
        // 先判任务再判额度，是为了别让已下线/不存在的任务被误报成「额度已满」。
        if (isDaily(task) && countClaimedToday(userId) >= DAILY_CLAIM_LIMIT) {
            return ClaimOutcome.fail("今日日课已领满（" + DAILY_CLAIM_LIMIT + " 个），明日再来");
        }

        LocalDate date = dateOf(task, LocalDate.now());
        UserTask row = findRow(userId, task.getId(), date);
        if (row == null) {
            return ClaimOutcome.fail("该任务尚未开启");
        }
        if (row.getClaimedAt() != null) {
            return ClaimOutcome.fail("该任务已领取过经验");
        }
        int target = safeInt(task.getTarget(), 1);
        int progress = safeInt(row.getProgress(), 0);
        if (progress < target) {
            return ClaimOutcome.fail("任务尚未完成（" + progress + " / " + target + "）");
        }

        // 条件更新：只有 claimed_at 仍为空才写得进去，双击第二下会拿到 0 行。
        // 这里用字符串列名而非 Lambda：set() 会在构造时就解析列名，
        // 单测里没有 MyBatis 元数据，用 Lambda 会抛 lambda cache 异常。
        LocalDateTime now = LocalDateTime.now();
        int rows = userTaskMapper.update(null, new UpdateWrapper<UserTask>()
                .eq("id", row.getId())
                .isNull("claimed_at")
                .set("claimed_at", now)
                .set("updated_at", now));
        if (rows == 0) {
            return ClaimOutcome.fail("该任务已领取过经验");
        }

        int gain = safeInt(task.getExp(), 0);
        // 经验行已在方法开头加锁，这里直接用锁住的那份，不再重复读取
        int beforeTotal = safeInt(ue.getTotalExp(), 0);
        LevelInfo before = levelOf(beforeTotal);

        int afterTotal = beforeTotal + gain;
        ue.setTotalExp(afterTotal);
        ue.setUpdatedAt(now);
        userExpMapper.updateById(ue);

        LevelInfo after = levelOf(afterTotal);
        writeLog(userId, "完成「" + task.getTitle() + "」", gain);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gain", gain);
        data.put("levelUp", after.level > before.level);
        data.put("oldLevel", before.level);
        data.put("newLevel", after.level);
        data.put("newTitle", after.title);
        data.put("panel", buildPanel(userId));
        return ClaimOutcome.ok("获得 " + gain + " 点神格经验", data);
    }

    /**
     * 战力提升成功后推进任务：精确匹配 action，外加「任意提升」类任务。
     *
     * <p>只由 PowerService 在真正落库成功后调用；提升被拒绝时不推进。
     */
    public void onUpgrade(Long userId, String action) {
        if (userId == null || action == null || action.isBlank()) {
            return;
        }
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getEnabled, 1)
                .and(w -> w.eq(Task::getAction, action).or().eq(Task::getAction, ACTION_OVERALL)));
        advance(userId, tasks, 1);
    }

    /** 精确推进某一动作对应的任务（如 login） */
    public void onAction(Long userId, String action) {
        if (userId == null || action == null || action.isBlank()) {
            return;
        }
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getEnabled, 1)
                .eq(Task::getAction, action));
        advance(userId, tasks, 1);
    }

    // ==================== 内部实现 ====================

    private void advance(Long userId, List<Task> tasks, int amount) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        for (Task t : tasks) {
            int target = safeInt(t.getTarget(), 1);
            LocalDate date = dateOf(t, today);
            UserTask row = findRow(userId, t.getId(), date);

            if (row == null) {
                row = new UserTask();
                row.setUserId(userId);
                row.setTaskId(t.getId());
                row.setTaskDate(date);
                row.setProgress(Math.min(target, amount));
                row.setCreatedAt(now);
                row.setUpdatedAt(now);
                try {
                    userTaskMapper.insert(row);
                    continue;
                } catch (DuplicateKeyException e) {
                    // 并发下另一路已插入：重读后走更新分支
                    row = findRow(userId, t.getId(), date);
                    if (row == null) {
                        continue;
                    }
                }
            }

            // 已领取或已达标的不再累加
            if (row.getClaimedAt() != null) {
                continue;
            }
            int progress = safeInt(row.getProgress(), 0);
            if (progress >= target) {
                continue;
            }
            row.setProgress(Math.min(target, progress + amount));
            row.setUpdatedAt(now);
            userTaskMapper.updateById(row);
        }
    }

    /**
     * 今天已经领取过经验的<b>日课</b>数（主线不算）。
     *
     * <p>按 {@code claimed_at} 是否落在今天统计，而不是按 {@code task_date} 跨天比较：
     * 还要额外限定 {@code task_date = 今天}，因为主线的 task_date 是固定占位值，
     * 若不加这条，主线领取也会被算进当天额度。日课每天新建一行且日期恒为当天，
     * 所以「日期=今天 + 今天已领取」正好筛出今天领过的日课。
     */
    int countClaimedToday(Long userId) {
        LocalDate today = LocalDate.now();
        Long c = userTaskMapper.selectCount(new LambdaQueryWrapper<UserTask>()
                .eq(UserTask::getUserId, userId)
                .eq(UserTask::getTaskDate, today)
                .isNotNull(UserTask::getClaimedAt)
                .ge(UserTask::getClaimedAt, today.atStartOfDay()));
        return c == null ? 0 : c.intValue();
    }

    /** 是否日课：t_task.category 只有 daily / main 两种，非 main 即日课 */
    private boolean isDaily(Task t) {
        return !"main".equals(t.getCategory());
    }

    private List<Task> listEnabled() {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getEnabled, 1)
                .orderByAsc(Task::getSort)
                .orderByAsc(Task::getId));
    }

    private UserTask findRow(Long userId, Long taskId, LocalDate date) {
        return userTaskMapper.selectOne(new LambdaQueryWrapper<UserTask>()
                .eq(UserTask::getUserId, userId)
                .eq(UserTask::getTaskId, taskId)
                .eq(UserTask::getTaskDate, date)
                .last("limit 1"));
    }

    /** 日课用当天，主线用固定占位日期 */
    private LocalDate dateOf(Task t, LocalDate today) {
        return "main".equals(t.getCategory()) ? MAIN_DATE : today;
    }

    /** 组装与前端 TaskPage.vue 约定一致的单条任务结构 */
    private Map<String, Object> taskView(Task t, UserTask row) {
        int target = safeInt(t.getTarget(), 1);
        int progress = row == null ? 0 : Math.min(target, safeInt(row.getProgress(), 0));
        boolean claimed = row != null && row.getClaimedAt() != null;

        String status;
        if (claimed) {
            status = "claimed";
        } else if (progress >= target) {
            status = "claimable";
        } else {
            status = "doing";
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("code", t.getCode());
        m.put("title", t.getTitle());
        m.put("desc", t.getSummary());
        m.put("category", t.getCategory());
        m.put("categoryLabel", "main".equals(t.getCategory()) ? "主线" : "日课");
        m.put("action", t.getAction());
        m.put("progress", progress);
        m.put("target", target);
        m.put("exp", safeInt(t.getExp(), 0));
        m.put("status", status);
        m.put("percent", target <= 0 ? 100 : Math.min(100, progress * 100 / target));
        return m;
    }

    private UserExp loadOrCreateExp(Long userId) {
        UserExp ue = userExpMapper.selectOne(new LambdaQueryWrapper<UserExp>()
                .eq(UserExp::getUserId, userId)
                .last("limit 1"));
        if (ue != null) {
            return ue;
        }
        ue = new UserExp();
        ue.setUserId(userId);
        ue.setTotalExp(0);
        ue.setUpdatedAt(LocalDateTime.now());
        userExpMapper.insert(ue);
        return ue;
    }

    private List<Map<String, Object>> recentLogs(Long userId) {
        List<ExpLog> rows = expLogMapper.selectList(new LambdaQueryWrapper<ExpLog>()
                .eq(ExpLog::getUserId, userId)
                .orderByDesc(ExpLog::getId)
                .last("limit " + LOG_LIMIT));
        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (ExpLog r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time", r.getCreatedAt() == null ? "" : r.getCreatedAt().format(LOG_TIME));
            m.put("text", r.getText());
            m.put("exp", safeInt(r.getExp(), 0));
            list.add(m);
        }
        return list;
    }

    private void writeLog(Long userId, String text, int exp) {
        ExpLog log = new ExpLog();
        log.setUserId(userId);
        log.setText(text);
        log.setExp(exp);
        log.setCreatedAt(LocalDateTime.now());
        expLogMapper.insert(log);
    }

    private static int safeInt(Integer v, int fallback) {
        return v == null ? fallback : v;
    }

    /**
     * 由累计经验推导阶位。
     *
     * <p>逐阶扣减门槛，剩下的是当前阶内的经验；到十阶后不再有下一阶，
     * expMax 返回 0，前端据此显示「神格圆满」。
     */
    static LevelInfo levelOf(int totalExp) {
        int level = 1;
        int remain = Math.max(0, totalExp);
        while (level < LEVEL_MAX && remain >= need(level)) {
            remain -= need(level);
            level++;
        }
        boolean full = level >= LEVEL_MAX;
        int expMax = full ? 0 : need(level);
        int percent = full ? 100 : (int) Math.round(remain * 100.0 / expMax);
        return new LevelInfo(level, TITLES[level - 1], remain, expMax, Math.max(0, totalExp),
                percent, full);
    }

    /** 第 level 阶升下一阶所需经验 */
    static int need(int level) {
        return EXP_BASE + (level - 1) * EXP_STEP;
    }

    /** 阶位快照，附带前端要的百分比 */
    static class LevelInfo {

        final int level;
        final String title;
        final int exp;
        final int expMax;
        final int totalExp;
        final int percent;
        final boolean full;

        LevelInfo(int level, String title, int exp, int expMax, int totalExp, int percent, boolean full) {
            this.level = level;
            this.title = title;
            this.exp = exp;
            this.expMax = expMax;
            this.totalExp = totalExp;
            this.percent = percent;
            this.full = full;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", level);
            m.put("title", title);
            m.put("exp", exp);
            m.put("expMax", expMax);
            m.put("totalExp", totalExp);
            m.put("percent", percent);
            m.put("levelMax", LEVEL_MAX);
            m.put("full", full);
            m.put("nextTitle", full ? title : TITLES[level]);
            return m;
        }
    }

    /** 领取结果：成功带面板，失败带人话提示 */
    public static class ClaimOutcome {

        private final boolean ok;
        private final String message;
        private final Map<String, Object> data;

        private ClaimOutcome(boolean ok, String message, Map<String, Object> data) {
            this.ok = ok;
            this.message = message;
            this.data = data;
        }

        static ClaimOutcome ok(String message, Map<String, Object> data) {
            return new ClaimOutcome(true, message, data);
        }

        static ClaimOutcome fail(String message) {
            return new ClaimOutcome(false, message, null);
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
