package com.falsegod.common;

/**
 * 境界阶梯 —— 全项目唯一的阶位口径。
 *
 * <p>修行（{@code PowerService}）与神格任务（{@code TaskService}）共用这一条阶梯，
 * 避免出现「修行页是四印·圆满、任务页却是弃子」这种同一个玩家两个身份的矛盾：
 * <ul>
 *   <li>大境界共 10 阶：一印 → 八印【天锁八印】 → 半神 → 真神（自封之神）</li>
 *   <li>神格阶位 Lv.N 与第 N 个大境界一一对应（Lv.4 = 四印）</li>
 *   <li>每阶内含 4 档小境界：初印 → 中印 → 圆满 → 巅峰（半神为初阶/中阶/圆满/巅峰）</li>
 * </ul>
 *
 * <p>两边都引用这里的常量，改阶梯只需要改这一处。「弃子」属于神堂对玩家的判定
 * （身份），不是阶位名，只出现在宣告文案里。
 */
public final class RealmLadder {

    /** 大境界名册，下标 0 = 一印 */
    public static final String[] REALMS = {
            "一印", "二印", "三印", "四印", "五印",
            "六印", "七印", "八印", "半神", "真神"
    };

    /** 大境界别称：八印锁天、真神自封，其余留空 */
    public static final String[] REALM_MARKS = {
            "", "", "", "", "", "", "", "天锁八印", "", "自封之神"
    };

    /** 印级（一印~八印）的小境界 */
    public static final String[] STAGES_SEAL = {"初印", "中印", "圆满", "巅峰"};

    /** 半神的小境界 */
    public static final String[] STAGES_HALF = {"初阶", "中阶", "圆满", "巅峰"};

    /** 真神只有一个位格 */
    public static final String[] STAGES_TRUE_GOD = {"自封之神"};

    /** 每个大境界内的小境界档数（真神除外） */
    public static final int STAGE_MAX = 4;

    /** 新档案默认境界：四印 · 圆满（战力 3940，与剧情设定一致） */
    public static final int DEFAULT_REALM_INDEX = 3;
    public static final int DEFAULT_STAGE = 2;

    private RealmLadder() {
    }

    /** 阶位上限（= 大境界数） */
    public static int levels() {
        return REALMS.length;
    }

    /** 大境界下标收敛到合法区间 */
    public static int clamp(int index) {
        return Math.max(0, Math.min(index, REALMS.length - 1));
    }

    /** 是否已至终局「真神」 */
    public static boolean isTrueGod(int realmIndex) {
        return clamp(realmIndex) >= REALMS.length - 1;
    }

    /** 阶位等级（1 起）→ 大境界下标 */
    public static int indexOfLevel(int level) {
        return clamp(Math.max(1, level) - 1);
    }

    /** 阶位等级（1 起）→ 大境界名，如 4 → 四印 */
    public static String title(int level) {
        return REALMS[indexOfLevel(level)];
    }

    /** 该大境界的小境界名册 */
    public static String[] stagesOf(int realmIndex) {
        int ri = clamp(realmIndex);
        if (isTrueGod(ri)) {
            return STAGES_TRUE_GOD;
        }
        if (ri == REALMS.length - 2) {   // 半神
            return STAGES_HALF;
        }
        return STAGES_SEAL;
    }

    /** 小境界名，如「圆满」 */
    public static String stageName(int realmIndex, int stage) {
        String[] stages = stagesOf(realmIndex);
        int i = Math.max(0, Math.min(stage, stages.length - 1));
        return stages[i];
    }

    /** 完整境界名：「四印 · 圆满」「八印【天锁八印】 · 巅峰」「真神（自封之神）」 */
    public static String fullRealm(int realmIndex, int stage) {
        int ri = clamp(realmIndex);
        String base = REALMS[ri];
        String mark = REALM_MARKS[ri];
        if (mark != null && !mark.isEmpty()) {
            base = isTrueGod(ri) ? base + "（" + mark + "）" : base + "【" + mark + "】";
        }
        if (isTrueGod(ri)) {
            return base;
        }
        return base + " · " + stageName(ri, stage);
    }
}
