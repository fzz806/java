-- =====================================================================
--  觉醒伪神 · 神格任务与阶位经验
--
--  设计要点：
--   1. 任务进度由真实操作驱动（淬炼/突破/静修/吞噬/掠夺），
--      不是前端点一下就给奖励的空壳按钮。
--   2. 日课按天落一行（task_date = 今天），跨天自然重置；
--      主线固定用 1970-01-01 作为 task_date，只记一次。
--   3. 阶位（等级）不落库，由 t_user_exp.total_exp 实时推导，
--      与战力同一思路：只存源头数据，避免多处各写一个数字。
--   4. 日课池固定 10 条，对应 TaskService.DAILY_CLAIM_LIMIT = 10：
--      每天最多领 10 条日课的经验，做满即额度用尽。主线不占额度。
--      改日课条数时必须同步改那个常量，否则额度会提前卡住或用不完。
--
--  本脚本幂等：可重复执行，不会清空已有经验与已领取记录。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 任务定义
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_task (
    id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code     VARCHAR(48)  NOT NULL COMMENT '任务编码，业务唯一',
    title    VARCHAR(48)  NOT NULL COMMENT '任务名',
    summary  VARCHAR(255) NOT NULL COMMENT '任务描述',
    category VARCHAR(16)  NOT NULL DEFAULT 'daily' COMMENT 'daily 日课 / main 主线',
    action   VARCHAR(24)  NOT NULL COMMENT '触发动作：login/rest/seal/realm/skill/essence/upgrade',
    target   INT          NOT NULL DEFAULT 1 COMMENT '完成所需次数',
    `exp`    INT          NOT NULL DEFAULT 0 COMMENT '完成后可得经验',
    sort     INT          NOT NULL DEFAULT 0 COMMENT '排序，小的在前',
    enabled  TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 下线',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '任务定义';

-- ---------------------------------------------------------------------
-- 用户任务进度
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user_task (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL COMMENT 't_user.id',
    task_id    BIGINT   NOT NULL COMMENT 't_task.id',
    task_date  DATE     NOT NULL COMMENT '日课=当天；主线=1970-01-01',
    progress   INT      NOT NULL DEFAULT 0 COMMENT '已完成次数，封顶到 target',
    claimed_at DATETIME NULL COMMENT '领取时间，NULL 表示未领取',
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_task_date (user_id, task_id, task_date),
    KEY idx_user_date (user_id, task_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户任务进度';

-- ---------------------------------------------------------------------
-- 用户累计经验（阶位由它推导）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user_exp (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL COMMENT 't_user.id',
    total_exp  INT      NOT NULL DEFAULT 0 COMMENT '累计经验，只增不减',
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户累计经验';

-- ---------------------------------------------------------------------
-- 经验流水（面板上「最近所得」）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_exp_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    text       VARCHAR(120) NOT NULL COMMENT '如：完成「每日登临」',
    `exp`      INT         NOT NULL DEFAULT 0 COMMENT '本次经验',
    created_at DATETIME    NULL,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '经验流水';

-- ---------------------------------------------------------------------
-- 种子任务：日课 10 条（每日 660 经验），主线 4 条（一次性 1150 经验）
-- 用 ON DUPLICATE KEY UPDATE 保证重复执行只更新文案、不改进度
--
-- 日课为什么这样排：灵能上限 2000、静修每次回 240，所以日课总消耗压在
-- 4000 灵能左右（约 9 次静修即可跑完），避开「大境界突破」这类 1600 灵能
-- 且真神后永远做不了的动作为日课，保证任何阶段都能做满 10 条。
-- ---------------------------------------------------------------------
INSERT INTO t_task (code, title, summary, category, action, target, `exp`, sort) VALUES
  ('daily_login',     '每日登临', '踏入神堂报到一次。日课之首，来则记功。',                   'daily', 'login',   1,  40,  10),
  ('daily_rest',      '静心吐纳', '端坐废殿静修一次，回聚灵能。',                             'daily', 'rest',    1,  50,  20),
  ('daily_rest3',     '三度吐纳', '一日三坐，气息三周，灵能才肯归位。',                       'daily', 'rest',    3,  60,  30),
  ('daily_rest6',     '六转周天', '六度静修，气行周天，废殿的灰都认得你了。',                 'daily', 'rest',    6,  70,  40),
  ('daily_seal',      '磨印双锤', '小境界精进两档，印纹愈深，骨血愈硬。',                     'daily', 'seal',    2,  80,  50),
  ('daily_essence',   '吞噬本源', '吞下一枚本源结晶，将他人道果炼入己身。',                   'daily', 'essence', 1,  60,  60),
  ('daily_essence3',  '本源三噬', '连吞三枚本源，道果层叠压身。',                             'daily', 'essence', 3,  75,  70),
  ('daily_skill',     '夺技于野', '从败者身上剥离一式神技，据为己有。',                       'daily', 'skill',   1,  70,  80),
  ('daily_upgrade',   '日日精进', '任意提升途径累计五次，不进则退。',                         'daily', 'upgrade', 5,  70,  90),
  ('daily_upgrade10', '十炼成印', '任意提升途径累计十次，日拱一卒终成伪神。',                 'daily', 'upgrade', 10, 85, 100),
  ('main_realm',      '叩关破境', '完成一次境界突破，小境界臻至巅峰方可叩关。',               'main',  'realm',   1, 200, 110),
  ('main_skill',      '夺技于敌', '掠夺神技一次，从败者身上剥离一式神技。',                   'main',  'skill',   1, 180, 120),
  ('main_realm3',     '三度破境', '累计完成三次境界突破，印级壁垒连破三层。',                 'main',  'realm',   3, 450, 130),
  ('main_upgrade20',  '百炼成钢', '任意提升途径累计二十次，日拱一卒终成伪神。',               'main',  'upgrade', 20, 320, 140)
ON DUPLICATE KEY UPDATE
  title   = VALUES(title),
  summary = VALUES(summary),
  category = VALUES(category),
  action  = VALUES(action),
  target  = VALUES(target),
  `exp`   = VALUES(`exp`),
  sort    = VALUES(sort);
