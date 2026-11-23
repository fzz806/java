package com.falsegod.service;

import com.falsegod.entity.Power;
import com.falsegod.entity.PowerLog;
import com.falsegod.entity.Skill;
import com.falsegod.mapper.PowerLogMapper;
import com.falsegod.mapper.PowerMapper;
import com.falsegod.mapper.SkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 战力业务单元测试：数值模型 + 五条提升途径的通过/拒绝分支。
 *
 * <p>全部用 Mock 隔离数据库，不依赖 MySQL，跑得快。
 * 重点覆盖三类容易写错的地方：
 * <ol>
 *   <li>战力公式与增量是否与权重一致（改权重会立刻红）</li>
 *   <li>前置条件（小境界是否至巅峰、境界是否到顶、灵能是否够）</li>
 *   <li>失败时不得写库、不得扣灵能</li>
 * </ol>
 */
@DisplayName("PowerService 战力模型与提升逻辑")
class PowerServiceTest {

    private static final Long UID = 1L;

    private PowerMapper powerMapper;
    private PowerLogMapper powerLogMapper;
    private SkillMapper skillMapper;
    private TaskService taskService;
    private PowerService service;

    @BeforeEach
    void setUp() {
        powerMapper = mock(PowerMapper.class);
        powerLogMapper = mock(PowerLogMapper.class);
        skillMapper = mock(SkillMapper.class);
        taskService = mock(TaskService.class);
        when(powerLogMapper.selectList(any())).thenReturn(List.of());
        service = new PowerService(powerMapper, powerLogMapper, skillMapper, taskService);
    }

    // ==================== 面板与公式 ====================

    @Test
    @DisplayName("面板战力按权重实时推导：2400+240+980+320=3940")
    void panelComputesPower() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));

        Map<String, Object> panel = service.panel(UID);

        assertEquals(3940, panel.get("power"));
        assertEquals("四印 · 圆满", panel.get("realm"));
        assertEquals("四印", panel.get("realmName"));
        assertEquals("圆满", panel.get("stageName"));
        assertEquals("四印 · 巅峰", panel.get("nextRealm"));
        assertEquals(3, panel.get("realmLevel"));
        assertEquals(4, panel.get("realmMax"));
        assertEquals(4, panel.get("sealMax"));
        assertEquals(900, panel.get("spirit"));
        assertEquals(2000, panel.get("spiritMax"));
    }

    @Test
    @DisplayName("无档案时惰性建档：注册即从一印·初印开始，战力 140")
    void panelCreatesProfileWhenAbsent() {
        when(powerMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> panel = service.panel(UID);

        verify(powerMapper).insert(any(Power.class));
        assertEquals(140, panel.get("power"));
        assertEquals("一印 · 初印", panel.get("realm"));
        assertEquals("一印", panel.get("realmName"));
        assertEquals("一印 · 中印", panel.get("nextRealm"));
        assertEquals(0, panel.get("seal"));
        assertEquals(4, panel.get("sealMax"));
        assertEquals(1, panel.get("realmLevel"), "刚入一印，进度条应是 1/4");
        assertEquals(2000, panel.get("spiritMax"));
    }

    @Test
    @DisplayName("初始面板仅小境界/本源/静修可点，突破与掠夺因条件不足置灰")
    @SuppressWarnings("unchecked")
    void panelActionAvailability() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));

        List<Map<String, Object>> actions =
                (List<Map<String, Object>>) service.panel(UID).get("actions");

        assertEquals(5, actions.size());
        assertTrue(available(actions, "seal"), "小境界 圆满（2/4）且灵能 900≥600，应可点");
        assertFalse(available(actions, "realm"), "小境界未至巅峰，不可突破");
        assertFalse(available(actions, "skill"), "灵能 900<1200，不可掠夺");
        assertTrue(available(actions, "essence"), "灵能 900≥400，可吞噬本源");
        assertTrue(available(actions, "rest"), "灵能未满，可静修");
    }

    // ==================== 小境界精进 ====================

    @Test
    @DisplayName("小境界精进：档位 +1、灵能 -600、战力 +120")
    void sealUpgradeSucceeds() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "seal");

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(120, r.getData().get("gain"));

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = (Map<String, Object>) r.getData().get("panel");
        assertEquals(4060, panel.get("power"));
        assertEquals(3, panel.get("seal"));
        assertEquals("巅峰", panel.get("stageName"));
        assertEquals(300, panel.get("spirit"));

        verify(powerMapper).updateById(any(Power.class));
        verify(powerLogMapper).insert(any(PowerLog.class));
    }

    @Test
    @DisplayName("小境界已至巅峰时拒绝精进，且不写库")
    void sealUpgradeRejectedWhenFull() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 3, 900, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "seal");

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("小境界已至"));
        verify(powerMapper, never()).updateById(any(Power.class));
    }

    @Test
    @DisplayName("灵能不足 600 时拒绝精进，且不写库")
    void sealUpgradeRejectedWhenSpiritLow() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 100, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "seal");

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("灵能不足"));
        verify(powerMapper, never()).updateById(any(Power.class));
    }

    // ==================== 大境界突破 ====================

    @Test
    @DisplayName("境界突破：大境界 +1、小境界归初印、灵能 -1600、战力 +440")
    void realmUpgradeSucceeds() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 3, 2000, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "realm");

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(440, r.getData().get("gain"));

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = (Map<String, Object>) r.getData().get("panel");
        assertEquals(4500, panel.get("power"));
        assertEquals("五印 · 初印", panel.get("realm"));
        assertEquals(0, panel.get("seal"));
        assertEquals(400, panel.get("spirit"));
    }

    @Test
    @DisplayName("小境界未至巅峰时拒绝突破")
    void realmUpgradeRejectedWhenSealNotFull() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 1, 2000, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "realm");

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("未至巅峰"));
        verify(powerMapper, never()).updateById(any(Power.class));
    }

    @Test
    @DisplayName("已至真神时拒绝突破")
    void realmUpgradeRejectedAtTopRealm() {
        when(powerMapper.selectOne(any())).thenReturn(power(9, 0, 2000, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "realm");

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("真神"));
    }

    @Test
    @DisplayName("灵能不足以支付 1600 时拒绝突破")
    void realmUpgradeRejectedWhenSpiritLow() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 3, 1500, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "realm");

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("灵能不足"));
    }

    // ==================== 掠夺神技 / 本源 / 静修 ====================

    @Test
    @DisplayName("掠夺神技：抽到真实神技并回传 looted，神技数 +1、战力 +140")
    void skillUpgradeLootsRealSkill() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 1400, 2000, 7, 8));
        when(skillMapper.selectList(any())).thenReturn(List.of(skill("刚虎 · 吞天", "S", "兽系")));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "skill");

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(140, r.getData().get("gain"));

        @SuppressWarnings("unchecked")
        Map<String, Object> looted = (Map<String, Object>) r.getData().get("looted");
        assertNotNull(looted);
        assertEquals("刚虎 · 吞天", looted.get("name"));
        assertEquals("S", looted.get("level"));

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = (Map<String, Object>) r.getData().get("panel");
        assertEquals(8, panel.get("skills"));
    }

    @Test
    @DisplayName("神技表为空时仍可掠夺，只是没有 looted")
    void skillUpgradeWithoutCatalog() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 1400, 2000, 7, 8));
        when(skillMapper.selectList(any())).thenReturn(List.of());

        PowerService.UpgradeOutcome r = service.upgrade(UID, "skill");

        assertTrue(r.isOk(), r.getMessage());
        assertNull(r.getData().get("looted"));
    }

    @Test
    @DisplayName("本源吞噬：本源 +1、战力 +40")
    void essenceUpgradeSucceeds() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "essence");

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(40, r.getData().get("gain"));

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = (Map<String, Object>) r.getData().get("panel");
        assertEquals(9, panel.get("essence"));
    }

    @Test
    @DisplayName("静修：灵能回复到上限即止，战力不变、gain 为 0")
    void restCapsSpirit() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 1900, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "rest");

        assertTrue(r.isOk(), r.getMessage());
        assertEquals(0, r.getData().get("gain"));

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = (Map<String, Object>) r.getData().get("panel");
        assertEquals(2000, panel.get("spirit"), "只补到上限，不溢出");
        assertEquals(3940, panel.get("power"));
    }

    @Test
    @DisplayName("灵能已满时拒绝静修")
    void restRejectedWhenFull() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 2000, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "rest");

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("灵能已满"));
    }

    @Test
    @DisplayName("静修每日上限 5 次：第 6 次被拒，不写库也不推进任务")
    void restRejectedWhenDailyLimitReached() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));
        // 今天已经静修满 5 次
        when(powerLogMapper.selectCount(any())).thenReturn(5L);

        PowerService.UpgradeOutcome r = service.upgrade(UID, "rest");

        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("上限"), r.getMessage());
        verify(powerMapper, never()).updateById(any(Power.class));
        verify(powerLogMapper, never()).insert(any(PowerLog.class));
        verify(taskService, never()).onUpgrade(any(), any());
    }

    @Test
    @DisplayName("面板带出静修额度：今日 4/5，剩 1 次仍可点")
    void panelCarriesRestQuota() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));
        when(powerLogMapper.selectCount(any())).thenReturn(4L);

        Map<String, Object> panel = service.panel(UID);

        assertEquals(4, panel.get("restUsed"));
        assertEquals(5, panel.get("restLimit"));
        assertEquals(1, panel.get("restLeft"));

        Map<String, Object> rest = restAction(panel);
        assertEquals(4, rest.get("used"));
        assertEquals(5, rest.get("limit"));
        assertEquals(1, rest.get("left"));
        assertEquals(Boolean.TRUE, rest.get("available"), "还有 1 次、灵能未满，按钮应可点");
        assertTrue(String.valueOf(rest.get("effect")).contains("今日 4/5"));
    }

    @Test
    @DisplayName("静修额度用尽后按钮置灰，文案改为今日已满")
    void panelMarksRestUnavailableWhenQuotaSpent() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));
        when(powerLogMapper.selectCount(any())).thenReturn(5L);

        Map<String, Object> panel = service.panel(UID);

        assertEquals(0, panel.get("restLeft"));

        Map<String, Object> rest = restAction(panel);
        assertEquals(0, rest.get("left"));
        assertEquals(Boolean.FALSE, rest.get("available"));
        assertTrue(String.valueOf(rest.get("effect")).contains("今日已满"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> restAction(Map<String, Object> panel) {
        List<Map<String, Object>> actions = (List<Map<String, Object>>) panel.get("actions");
        assertNotNull(actions, "面板应带 actions");
        return actions.stream()
                .filter(a -> "rest".equals(a.get("key")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("actions 里应有 rest"));
    }

    // ==================== 入参防御 ====================

    @Test
    @DisplayName("未知途径与空途径都拒绝，且不写库")
    void unknownTypeRejected() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));

        assertFalse(service.upgrade(UID, "hack").isOk());
        assertFalse(service.upgrade(UID, null).isOk());
        assertFalse(service.upgrade(UID, "  ").isOk());

        verify(powerMapper, never()).updateById(any(Power.class));
        verify(powerLogMapper, never()).insert(any(PowerLog.class));
    }

    // ==================== 与任务的联动 ====================

    @Test
    @DisplayName("提升成功后才推进任务进度，且按实际途径记账")
    void upgradeAdvancesTaskProgress() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));

        service.upgrade(UID, "seal");

        verify(taskService).onUpgrade(UID, "seal");
    }

    @Test
    @DisplayName("提升被拒绝时不得推进任务进度，避免白送经验")
    void rejectedUpgradeDoesNotAdvanceTask() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 3, 900, 2000, 7, 8));

        service.upgrade(UID, "seal");

        verify(taskService, never()).onUpgrade(any(), any());
    }

    @Test
    @DisplayName("任务记账异常不影响提升结果：战力已落库就不该回滚给玩家")
    void taskFailureDoesNotBreakUpgrade() {
        when(powerMapper.selectOne(any())).thenReturn(power(3, 2, 900, 2000, 7, 8));
        org.mockito.Mockito.doThrow(new IllegalStateException("任务表炸了"))
                .when(taskService).onUpgrade(any(), any());

        PowerService.UpgradeOutcome r = service.upgrade(UID, "seal");

        assertTrue(r.isOk(), "任务记账失败不应让提升整体失败");
        assertEquals(120, r.getData().get("gain"));
        verify(powerMapper).updateById(any(Power.class));
    }

    // ==================== 边界档位：八印 / 半神 / 真神 ====================

    @Test
    @DisplayName("八印带【天锁八印】印记，下一档为半神·初阶")
    void apexSealRealmFormatsWithMark() {
        when(powerMapper.selectOne(any())).thenReturn(power(7, 3, 2000, 2000, 7, 8));

        Map<String, Object> panel = service.panel(UID);

        assertEquals("八印【天锁八印】 · 巅峰", panel.get("realm"));
        assertEquals("半神 · 初阶", panel.get("nextRealm"));
        assertEquals(4, panel.get("realmLevel"));
        assertEquals(4, panel.get("realmMax"));
    }

    @Test
    @DisplayName("半神的小境界名为初阶/中阶/巅峰（3 档，无圆满）")
    void halfGodUsesHalfStages() {
        when(powerMapper.selectOne(any())).thenReturn(power(8, 1, 2000, 2000, 7, 8));

        Map<String, Object> panel = service.panel(UID);

        assertEquals("半神 · 中阶", panel.get("realm"));
        assertEquals("中阶", panel.get("stageName"));
        assertEquals(3, panel.get("stageMax"), "半神只有 3 档");
        assertEquals("半神 · 巅峰", panel.get("nextRealm"));
    }

    @Test
    @DisplayName("半神·巅峰突破即封真神（自封之神）")
    void halfGodBreaksThroughToTrueGod() {
        when(powerMapper.selectOne(any())).thenReturn(power(8, 2, 2000, 2000, 7, 8));

        PowerService.UpgradeOutcome r = service.upgrade(UID, "realm");

        assertTrue(r.isOk(), r.getMessage());

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = (Map<String, Object>) r.getData().get("panel");
        assertEquals("真神（自封之神）", panel.get("realm"));
        assertEquals("已至真神", panel.get("nextRealm"));
        assertEquals(1, panel.get("realmMax"), "真神不再细分，进度条满格");
    }

    @Test
    @DisplayName("真神为终局：小境界精进与大境界突破都被拒绝")
    void trueGodIsTerminal() {
        when(powerMapper.selectOne(any())).thenReturn(power(9, 0, 2000, 2000, 7, 8));

        PowerService.UpgradeOutcome stage = service.upgrade(UID, "seal");
        PowerService.UpgradeOutcome realm = service.upgrade(UID, "realm");

        assertFalse(stage.isOk());
        assertTrue(stage.getMessage().contains("真神"));
        assertFalse(realm.isOk());
        assertTrue(realm.getMessage().contains("真神"));
        verify(powerMapper, never()).updateById(any(Power.class));
    }

    // ==================== 辅助 ====================

    private Power power(int realmIndex, int seal, int spirit, int spiritMax, int skills, int essence) {
        Power p = new Power();
        p.setId(1L);
        p.setUserId(UID);
        p.setRealmIndex(realmIndex);
        p.setStage(seal);
        p.setSpirit(spirit);
        p.setSpiritMax(spiritMax);
        p.setSkills(skills);
        p.setEssence(essence);
        return p;
    }

    private Skill skill(String name, String level, String element) {
        Skill s = new Skill();
        s.setId(1L);
        s.setName(name);
        s.setLevel(level);
        s.setElement(element);
        s.setDesc("测试用神技");
        return s;
    }

    private boolean available(List<Map<String, Object>> actions, String key) {
        return actions.stream()
                .filter(a -> key.equals(a.get("key")))
                .map(a -> (Boolean) a.get("available"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到途径：" + key));
    }
}
