-- ============================================================
-- 觉醒伪神 · 恶鬼之牙公会完整系统
-- 在 false_god 库执行；为 t_member 增加玩家字段，并新建
-- 入会申请 / 公会公告 / 贡献记录三张表。
-- 脚本幂等：可重复执行，不清理已有数据。
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 扩展 t_member：关联真实玩家 + 贡献值 + 入会时间
--    user_id 为空表示剧情 NPC；有值表示真实登录玩家在册
--    MySQL 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 判断后动态执行
-- ----------------------------
DROP PROCEDURE IF EXISTS `sp_guild_alter_member`;
DELIMITER $$
CREATE PROCEDURE `sp_guild_alter_member`()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='false_god' AND TABLE_NAME='t_member' AND COLUMN_NAME='user_id') THEN
    ALTER TABLE `t_member` ADD COLUMN `user_id` bigint DEFAULT NULL COMMENT '关联 t_user.id，NULL 为剧情成员' AFTER `id`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='false_god' AND TABLE_NAME='t_member' AND COLUMN_NAME='contribution') THEN
    ALTER TABLE `t_member` ADD COLUMN `contribution` int NOT NULL DEFAULT 0 COMMENT '公会贡献值' AFTER `bio`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='false_god' AND TABLE_NAME='t_member' AND COLUMN_NAME='joined_at') THEN
    ALTER TABLE `t_member` ADD COLUMN `joined_at` datetime DEFAULT NULL COMMENT '入会时间' AFTER `contribution`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA='false_god' AND TABLE_NAME='t_member' AND INDEX_NAME='uk_member_user') THEN
    ALTER TABLE `t_member` ADD UNIQUE INDEX `uk_member_user`(`user_id`);
  END IF;
END$$
DELIMITER ;
CALL `sp_guild_alter_member`();
DROP PROCEDURE IF EXISTS `sp_guild_alter_member`;

-- ----------------------------
-- 2. 入会申请表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_guild_apply` (
  `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint NOT NULL COMMENT '申请玩家',
  `username`    varchar(16) NOT NULL COMMENT '申请时真名',
  `reason`      varchar(120) NOT NULL DEFAULT '' COMMENT '入会理由',
  `status`      varchar(10) NOT NULL DEFAULT 'pending' COMMENT 'pending待审/approved通过/rejected拒绝',
  `handle_note` varchar(120) NOT NULL DEFAULT '' COMMENT '审批批语',
  `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handled_at`  datetime DEFAULT NULL COMMENT '审批时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pending_user`(`user_id`, `status`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 入会申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 3. 公会公告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_guild_notice` (
  `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag`         varchar(10) NOT NULL DEFAULT '通告' COMMENT '标签：紧急/规则/嘉奖/通告',
  `title`       varchar(120) NOT NULL COMMENT '标题',
  `content`     varchar(500) NOT NULL DEFAULT '' COMMENT '正文',
  `author`      varchar(16) NOT NULL DEFAULT '恶鬼之牙' COMMENT '发布者',
  `sort_no`     int NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
  `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sort`(`sort_no`, `id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 公会公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 4. 贡献记录表（每次供奉一条，便于审计与排行）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_guild_contribution` (
  `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint NOT NULL COMMENT '供奉玩家',
  `username`    varchar(16) NOT NULL COMMENT '真名',
  `amount`      int NOT NULL COMMENT '本次贡献值',
  `cost`        int NOT NULL DEFAULT 0 COMMENT '消耗灵能',
  `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '供奉时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_created`(`created_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 公会贡献记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 5. 初始公会公告（仅表为空时写入）
-- ----------------------------
INSERT INTO `t_guild_notice` (`tag`, `title`, `content`, `author`, `sort_no`)
SELECT '规则', '入会者须守三条铁律', '一、护短；二、不许出卖同门；三、不许在神堂面前低头。', '沈砚', 1
WHERE NOT EXISTS (SELECT 1 FROM `t_guild_notice`);
INSERT INTO `t_guild_notice` (`tag`, `title`, `content`, `author`, `sort_no`)
SELECT '嘉奖', '供奉榜每月结算', '月末贡献前十者，可优先取用仓库二层的禁忌材料。', '老葛', 2
WHERE NOT EXISTS (SELECT 1 FROM `t_guild_notice` WHERE `sort_no` = 2);
INSERT INTO `t_guild_notice` (`tag`, `title`, `content`, `author`, `sort_no`)
SELECT '通告', '恶鬼之牙欢迎新血', '神弃者皆可为恶鬼。递交名帖，待会长朱批即可在册。', '阿盐', 3
WHERE NOT EXISTS (SELECT 1 FROM `t_guild_notice` WHERE `sort_no` = 3);

-- ----------------------------
-- 6. 会长在册（登录账号 chenhaoyu 为 t_user.id=1，名册展示名「陈昊宇」）：会长本人也应在名册中，可供奉
--    注意：WHERE 里的 'chenhaoyu' 是登录账号，用于匹配 t_user，不可改成中文名
-- ----------------------------
INSERT INTO `t_member` (`user_id`, `name`, `role`, `power`, `element`, `realm`, `bio`, `contribution`, `joined_at`)
SELECT 1, '陈昊宇', '会长', 4200, '伪神（神魔同流）', '八印 · 天锁', '恶鬼之牙之主 · 神堂缉杀令之首 · 执掌神魔四变，以伪神之身篡改万神法则', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_member` WHERE `user_id` = 1)
  AND EXISTS (SELECT 1 FROM `t_user` WHERE `id` = 1 AND `username` = 'chenhaoyu');
