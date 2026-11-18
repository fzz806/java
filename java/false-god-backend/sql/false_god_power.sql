/*
 觉醒伪神 · 战力与首页数据表
 用途：补齐前端已调用但后端未实现的 /api/power、/api/power/upgrade、/api/home

 本脚本幂等，可重复执行：
   - 四张表均为 CREATE TABLE IF NOT EXISTS，不会动已有数据
   - t_notice / t_warehouse 为静态展示数据，每次执行重写
   - t_power / t_power_log 为用户产生数据，不会被清理

 导入：mysql -uroot -p123456 --default-character-set=utf8mb4 false_god < false_god_power.sql
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户战力档案（每个用户一行，首次访问 /api/power 时惰性建档）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_power` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '所属用户 t_user.id',
  `realm_index` int NOT NULL DEFAULT 0 COMMENT '大境界序号 0 起：0-7 一印~八印，8 半神，9 真神',
  `stage` int NOT NULL DEFAULT 0 COMMENT '大境界内小境界 0-3：初印/中印/圆满/巅峰（半神为初阶/中阶/圆满/巅峰）',
  `spirit` int NOT NULL DEFAULT 0 COMMENT '当前灵能',
  `spirit_max` int NOT NULL DEFAULT 2000 COMMENT '灵能上限',
  `skills` int NOT NULL DEFAULT 0 COMMENT '已习神技数',
  `essence` int NOT NULL DEFAULT 0 COMMENT '本源枚数',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后变动时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 用户战力档案' ROW_FORMAT = DYNAMIC;

-- 兼容旧库：把历史的 seal（红印 0-9 重）改成 stage（小境界 0-3 档）
-- 已有 stage 列则跳过；改完把越界的重数折算进 0-3 档，不会丢档案
SET @has_stage := (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_power' AND COLUMN_NAME = 'stage');
SET @has_seal := (SELECT COUNT(*) FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_power' AND COLUMN_NAME = 'seal');
SET @mig := IF(@has_stage = 0 AND @has_seal = 1,
    'ALTER TABLE `t_power` CHANGE COLUMN `seal` `stage` int NOT NULL DEFAULT 0 COMMENT ''大境界内小境界 0-3：初印/中印/圆满/巅峰''',
    'DO 0');
PREPARE mig FROM @mig;
EXECUTE mig;
DEALLOCATE PREPARE mig;
UPDATE `t_power` SET `stage` = LEAST(3, FLOOR(`stage` / 3)) WHERE `stage` > 3;

-- ----------------------------
-- 战力提升记录（前端「提升记录」列表，按时间倒序取最近若干条）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_power_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '所属用户 t_user.id',
  `text` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '记录文案',
  `gain` int NOT NULL DEFAULT 0 COMMENT '本次战力增量',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 战力提升记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 首页公告
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签：紧急/规则/通告',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `time_text` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '时间描述（自由文本）',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序，小在前',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 首页公告' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 首页仓库
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物品名',
  `count` int NOT NULL DEFAULT 0 COMMENT '数量',
  `rarity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '稀有度：普通/稀有/史诗',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序，小在前',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 恶鬼之牙仓库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 静态数据：公告（与前端 data/homeData.js 默认值一致，可由后端接管）
-- ----------------------------
DELETE FROM `t_notice`;
INSERT INTO `t_notice` (`tag`, `title`, `time_text`, `sort_no`) VALUES
  ('紧急', '神印熔铸炉将在三日后停机检修', '11 月 9 日 06:00 — 12:00', 1),
  ('规则', '镇兽大比新增「禁用治愈类神技」条款', '11 月 8 日 起生效', 2),
  ('通告', '恶鬼之牙仓库开放二层，需副会长以上神印', '长期有效', 3);

-- ----------------------------
-- 静态数据：仓库
-- ----------------------------
DELETE FROM `t_warehouse`;
INSERT INTO `t_warehouse` (`name`, `count`, `rarity`, `sort_no`) VALUES
  ('高纯度灵髓', 12, '稀有', 1),
  ('镇兽脊骨', 4, '史诗', 2),
  ('残缺神印', 27, '普通', 3),
  ('静默砂', 9, '稀有', 4);

SET FOREIGN_KEY_CHECKS = 1;
