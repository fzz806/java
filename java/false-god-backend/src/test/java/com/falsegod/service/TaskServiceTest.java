package com.falsegod.service;

import com.falsegod.entity.ExpLog;
import com.falsegod.entity.Power;
import com.falsegod.entity.Task;
import com.falsegod.entity.UserExp;
import com.falsegod.entity.UserTask;
import com.falsegod.mapper.ExpLogMapper;
import com.falsegod.mapper.PowerMapper;
import com.falsegod.mapper.TaskMapper;
import com.falsegod.mapper.UserExpMapper;
import com.falsegod.mapper.UserTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务与阶位经验单元测试：等级推导、领取判定、进度累加。
 *
 * <p>全部 Mock 隔离数据库。重点覆盖三类容易出错的地方：
 * <ol>
 *   <li>等级边界：恰好跨级、一次跨多级、十阶封顶</li>
 *   <li>领取判定：未达标 / 已领取 / 并发重复领取都不许加经验</li>
 *   <li>进度累加：封顶到 target；已领取的任务不再累加</li>
 * </ol>
 */
@DisplayName("TaskService 任务与阶位经验")
class TaskServiceTest {

    private static final Long UID = 1L;

    private TaskMapper taskMapper;
    private UserTaskMapper userTaskMapper;
    private UserExpMapper userExpMapper;
    private ExpLogMapper expLogMapper;
    private PowerMapper powerMapper;
    private TaskService service;

    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        userTaskMapper = mock(UserTaskMapper.class);
        userExpMapper = mock(UserExpMapper.class);
        expLogMapper = mock(ExpLogMapper.class);
        powerMapper = mock(PowerMapper.class);
        when(expLogMapper.selectList(any())).thenReturn(List.of());
        // 默认今天还没领过任何任务，个别用例自行覆写
        when(userTaskMapper.selectCount(any())).thenReturn(0L);
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2));   // 四印 · 圆满
        service = new TaskService(taskMapper, userTaskMapper, userExpMapper, expLogMapper, powerMapper);
    }

    private Power power(int realmIndex, int stage) {
        Power p = new Power();
        p.setUserId(UID);
        p.setRealmIndex(realmIndex);
        p.setStage(stage);
        p.setSpirit(900);
        p.setSpiritMax(2000);
        p.setSkills(7);
        p.setEssence(8);
        return p;
    }

    // ==================== 阶位推导 ====================

    @Test
    @DisplayName("0 经验是一阶一印，门槛 100（阶位名与大境界同名，弃子只是身份判定）")
    void levelOfZero() {
        TaskService.LevelInfo li = TaskService.levelOf(0);

        assertEquals(1, li.level);
        assertEquals("一印", li.title);
        assertEquals(0, li.exp);
        assertEquals(100, li.expMax);
        assertEquals(0, li.percent);
        assertFalse(li.full);
    }

    @Test
    @DisplayName("经验恰好等于门槛时跨级，阶内经验归零")
    void levelUpExactlyAtThreshold() {
        TaskService.LevelInfo li = TaskService.levelOf(100);

        assertEquals(2, li.level);
        assertEquals("二印", li.title);
        assertEquals(0, li.exp);
        assertEquals(180, li.expMax);
    }

    @Test
    @DisplayName("四印的门槛是 540 点：折算后阶位与修行页的「四印」一致")
    void fourSealMatchesRealm() {
        assertEquals(540, TaskService.threshold(4));

        TaskService.LevelInfo li = TaskService.levelOf(TaskService.threshold(4));
        assertEquals(4, li.level);
        assertEquals("四印", li.title);
        assertEquals(0, li.exp, "刚好跨到四印起点，阶内经验归零");
        assertEquals(340, li.expMax);
    }

    @Test
    @DisplayName("门槛按 100 + (n-1) * 80 递增")
    void needGrowsByStep() {
        assertEquals(100, TaskService.need(1));
        assertEquals(180, TaskService.need(2));
        assertEquals(260, TaskService.need(3));
        assertEquals(740, TaskService.need(9));
    }

    @Test
    @DisplayName("累计 3780 点正好十阶真神且封顶，再多的经验也不再加阶")
    void levelCapsAtTen() {
        TaskService.LevelInfo top = TaskService.levelOf(3780);
        assertEquals(10, top.level);
        assertEquals("真神", top.title);
        assertTrue(top.full);
        assertEquals(0, top.expMax);
        assertEquals(100, top.percent);

        TaskService.LevelInfo overflow = TaskService.levelOf(99999);
        assertEquals(10, overflow.level, "十阶后不应继续升级");
        assertEquals(99999, overflow.totalExp, "累计经验仍如实记录");
    }

    // ==================== 领取 ====================

    @Test
    @DisplayName("任务达标即可领取：经验入账、写流水、面板随之更新")
    void claimAddsExp() {
        Task task = task(1L, "daily_login", "daily", "login", 1, 40);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        when(userTaskMapper.update(any(), any())).thenReturn(1);
        when(userExpMapper.selectOne(any())).thenReturn(exp(0));
        when(powerMapper.selectOne(any())).thenReturn(power(0, 0));   // 一印，避免境界折算干扰

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(40, r.getData().get("gain"));
        assertEquals(false, r.getData().get("levelUp"));
        assertNotNull(r.getData().get("panel"));
        verify(userExpMapper).updateById(any(UserExp.class));
        verify(expLogMapper).insert(any(ExpLog.class));
    }

    @Test
    @DisplayName("一次领取跨过门槛会升阶，并报出旧阶与新阶")
    void claimTriggersLevelUp() {
        Task task = task(1L, "main_upgrade20", "main", "upgrade", 20, 320);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 20, null));
        when(userTaskMapper.update(any(), any())).thenReturn(1);
        // 已有 90 点，领 320 → 410：1→2 阶花 100，2→3 阶花 180，剩 130 停在 3 阶
        when(userExpMapper.selectOne(any())).thenReturn(exp(90));
        when(powerMapper.selectOne(any())).thenReturn(power(0, 0));   // 一印，避免境界折算干扰

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(true, r.getData().get("levelUp"));
        assertEquals(1, r.getData().get("oldLevel"));
        assertEquals(3, r.getData().get("newLevel"));
        assertEquals("三印", r.getData().get("newTitle"));
        assertEquals(true, r.getData().get("realmUp"), "经验跨阶要把大境界一起推上去");
        verify(powerMapper).updateById(any(Power.class));
    }

    @Test
    @DisplayName("任务未达标时拒绝领取，且不写经验")
    void claimRejectedWhenNotComplete() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "daily_seal", "daily", "seal", 2, 80));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("尚未完成"));
        verify(userExpMapper, never()).updateById(any(UserExp.class));
        verify(expLogMapper, never()).insert(any(ExpLog.class));
    }

    @Test
    @DisplayName("已领取过的任务不能再领，防止刷经验")
    void claimRejectedWhenAlreadyClaimed() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "daily_login", "daily", "login", 1, 40));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, LocalDateTime.now()));

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("已领取"));
        verify(userExpMapper, never()).updateById(any(UserExp.class));
    }

    @Test
    @DisplayName("并发双击时条件更新命中 0 行，第二次领取不发经验")
    void claimRejectedWhenConditionalUpdateMisses() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "daily_login", "daily", "login", 1, 40));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        // 模拟已被另一路抢先改成 claimed_at
        when(userTaskMapper.update(any(), any())).thenReturn(0);

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("已领取"));
        verify(userExpMapper, never()).updateById(any(UserExp.class));
    }

    @Test
    @DisplayName("任务不存在或已下线时拒绝领取")
    void claimRejectedForMissingOrDisabledTask() {
        when(taskMapper.selectById(9L)).thenReturn(null);
        assertFalse(service.claim(UID, 9L).isOk());
        assertFalse(service.claim(UID, null).isOk());

        Task off = task(2L, "daily_off", "daily", "rest", 1, 50);
        off.setEnabled(0);
        when(taskMapper.selectById(2L)).thenReturn(off);
        assertFalse(service.claim(UID, 2L).isOk());
    }

    // ==================== 每日领取额度 ====================

    @Test
    @DisplayName("今日日课已领满 10 条时拒绝领取，且不改任务状态、不写经验")
    void claimRejectedWhenDailyLimitReached() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "daily_login", "daily", "login", 1, 40));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        when(userTaskMapper.selectCount(any())).thenReturn(10L);

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("已领满"), r.getMessage());
        verify(userTaskMapper, never()).update(any(), any());
        verify(userExpMapper, never()).updateById(any(UserExp.class));
        verify(expLogMapper, never()).insert(any(ExpLog.class));
    }

    @Test
    @DisplayName("日课额度用尽不影响主线：主线是一次性功业，不占额度")
    void mainClaimIgnoresDailyLimit() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "main_realm", "main", "realm", 1, 200));
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        // 今天日课额度已经领满 10 条
        when(userTaskMapper.selectCount(any())).thenReturn(10L);
        when(userTaskMapper.update(any(), any())).thenReturn(1);
        when(userExpMapper.selectOne(any())).thenReturn(exp(0));

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(200, r.getData().get("gain"));
    }

    @Test
    @DisplayName("今日已领 9 个时仍可领走最后一个额度")
    void claimAllowedWhenOneQuotaLeft() {
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "daily_login", "daily", "login", 1, 40));
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        when(userTaskMapper.selectCount(any())).thenReturn(9L);
        when(userTaskMapper.update(any(), any())).thenReturn(1);
        when(userExpMapper.selectOne(any())).thenReturn(exp(0));

        TaskService.ClaimOutcome r = service.claim(UID, 1L);

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(40, r.getData().get("gain"));
    }

    // ==================== 面板 ====================

    @Test
    @DisplayName("面板按进度给出三种状态：可领取 / 进行中 / 已领取")
    @SuppressWarnings("unchecked")
    void panelStatuses() {
        Task login = task(1L, "daily_login", "daily", "login", 1, 40);
        Task seal = task(2L, "daily_seal", "daily", "seal", 2, 80);
        Task main = task(3L, "main_realm", "main", "realm", 1, 200);
        // panel 内部先查一次「登临」任务推进，再查全部任务
        when(taskMapper.selectList(any())).thenReturn(List.of(login), List.of(login, seal, main));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        when(userExpMapper.selectOne(any())).thenReturn(exp(100));

        Map<String, Object> panel = service.panel(UID);

        // 玩家档案是四印（门槛 540）：功业只有 100 点时按门槛折算，阶位必须显示四印，
        // 不能出现「修行页四印、任务页一阶」。
        assertEquals(4, panel.get("level"));
        assertEquals("四印", panel.get("title"));
        assertEquals("四印 · 圆满", panel.get("realm"));
        assertEquals(540, panel.get("totalExp"));
        assertEquals(2, panel.get("dailyTotal"));
        assertEquals(1, panel.get("mainTotal"));

        List<Map<String, Object>> tasks = (List<Map<String, Object>>) panel.get("tasks");
        assertEquals(3, tasks.size());
        // 唯一一行进度为 1：target 1 的已达标，target 2 的仍在进行中
        assertEquals("claimable", tasks.get(0).get("status"));
        assertEquals("doing", tasks.get(1).get("status"));
        assertEquals(50, tasks.get(1).get("percent"));
        assertEquals("claimable", tasks.get(2).get("status"));
        assertEquals(2, panel.get("claimable"));
    }

    @Test
    @DisplayName("面板报出每日额度：已领 7 个时还剩 3 次")
    void panelReportsQuota() {
        Task login = task(1L, "daily_login", "daily", "login", 1, 40);
        when(taskMapper.selectList(any())).thenReturn(List.of(login), List.of(login));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        when(userTaskMapper.selectCount(any())).thenReturn(7L);
        when(userExpMapper.selectOne(any())).thenReturn(exp(0));

        Map<String, Object> panel = service.panel(UID);

        assertEquals(TaskService.DAILY_CLAIM_LIMIT, panel.get("claimLimit"));
        assertEquals(7, panel.get("claimedToday"));
        assertEquals(3, panel.get("remainingClaim"));
        assertEquals(false, panel.get("limitReached"));
    }

    @Test
    @DisplayName("额度用尽时面板标记 limitReached，任务本身仍如实显示为可领取")
    @SuppressWarnings("unchecked")
    void panelMarksLimitReached() {
        Task login = task(1L, "daily_login", "daily", "login", 1, 40);
        when(taskMapper.selectList(any())).thenReturn(List.of(login), List.of(login));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));
        when(userTaskMapper.selectCount(any())).thenReturn(10L);
        when(userExpMapper.selectOne(any())).thenReturn(exp(0));

        Map<String, Object> panel = service.panel(UID);

        assertEquals(10, panel.get("claimedToday"));
        assertEquals(0, panel.get("remainingClaim"));
        assertEquals(true, panel.get("limitReached"));
        // 额度是独立一层限制，不篡改任务自身状态
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) panel.get("tasks");
        assertEquals("claimable", tasks.get(0).get("status"));
    }

    // ==================== 进度累加 ====================

    @Test
    @DisplayName("首次推进任务时建档，进度从 1 开始")
    void progressCreatesRow() {
        when(taskMapper.selectList(any())).thenReturn(List.of(task(1L, "daily_seal", "daily", "seal", 2, 80)));
        when(userTaskMapper.selectOne(any())).thenReturn(null);

        service.onUpgrade(UID, "seal");

        verify(userTaskMapper).insert(any(UserTask.class));
    }

    @Test
    @DisplayName("进度封顶到目标值，不会出现 5/2 这种越界数字")
    void progressIsCappedAtTarget() {
        when(taskMapper.selectList(any())).thenReturn(List.of(task(1L, "daily_seal", "daily", "seal", 2, 80)));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, null));

        service.onUpgrade(UID, "seal");

        verify(userTaskMapper).updateById(any(UserTask.class));
    }

    @Test
    @DisplayName("已达标或已领取的任务不再累加进度")
    void progressSkipsFinishedTasks() {
        when(taskMapper.selectList(any())).thenReturn(List.of(task(1L, "daily_seal", "daily", "seal", 2, 80)));
        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 2, null));
        service.onUpgrade(UID, "seal");
        verify(userTaskMapper, never()).updateById(any(UserTask.class));

        when(userTaskMapper.selectOne(any())).thenReturn(row(1L, 1, LocalDateTime.now()));
        service.onUpgrade(UID, "seal");
        verify(userTaskMapper, never()).updateById(any(UserTask.class));
    }

    @Test
    @DisplayName("非法用户或空动作直接忽略，不产生任何写入")
    void progressIgnoresInvalidInput() {
        service.onUpgrade(null, "seal");
        service.onUpgrade(UID, null);
        service.onAction(UID, " ");

        verify(taskMapper, never()).selectList(any());
        verify(userTaskMapper, never()).insert(any(UserTask.class));
    }

    // ==================== 辅助 ====================

    private Task task(Long id, String code, String category, String action, int target, int exp) {
        Task t = new Task();
        t.setId(id);
        t.setCode(code);
        t.setTitle("测试任务 " + code);
        t.setSummary("测试用描述");
        t.setCategory(category);
        t.setAction(action);
        t.setTarget(target);
        t.setExp(exp);
        t.setSort(10);
        t.setEnabled(1);
        return t;
    }

    private UserTask row(Long taskId, int progress, LocalDateTime claimedAt) {
        UserTask r = new UserTask();
        r.setId(100L + taskId);
        r.setUserId(UID);
        r.setTaskId(taskId);
        r.setTaskDate(LocalDate.now());
        r.setProgress(progress);
        r.setClaimedAt(claimedAt);
        return r;
    }

    private UserExp exp(int total) {
        UserExp e = new UserExp();
        e.setId(1L);
        e.setUserId(UID);
        e.setTotalExp(total);
        return e;
    }
}
