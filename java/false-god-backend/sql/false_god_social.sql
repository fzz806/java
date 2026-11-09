-- ============================================================
-- 觉醒伪神 · 社交扩展（好友 + 传音/聊天）
-- 在 false_god 库执行；t_user 已存在，不动原表
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 好友表（双向冗余：每对好友存两行，按 user_id 直接查自己的列表）
-- ----------------------------
DROP TABLE IF EXISTS `t_friend`;
CREATE TABLE `t_friend` (
  `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint NOT NULL COMMENT '归属用户',
  `friend_id`   bigint NOT NULL COMMENT '好友用户',
  `remark`      varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注名（空则显示真名）',
  `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结契时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_friend`(`user_id`, `friend_id`) USING BTREE,
  INDEX `idx_friend`(`friend_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 好友表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 传音消息表（点对点聊天）
-- ----------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
  `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sender_id`   bigint NOT NULL COMMENT '传音者',
  `receiver_id` bigint NOT NULL COMMENT '接收者',
  `content`     varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '传音内容',
  `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '传音时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pair`(`sender_id`, `receiver_id`, `id`) USING BTREE,
  INDEX `idx_pair_rev`(`receiver_id`, `sender_id`, `id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 传音消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 演示数据：结契 + 少量传音
-- （演示账号 shenyan / bailu 需先通过 /api/register 注册，见启动说明）
-- ----------------------------
INSERT INTO `t_friend` (`user_id`, `friend_id`, `remark`) VALUES
  (1, 3, '雨织'),
  (3, 1, NULL);

INSERT INTO `t_message` (`sender_id`, `receiver_id`, `content`) VALUES
  (3, 1, '陈昊宇，神堂的缉杀令贴到塔下了，你什么时候回来？'),
  (1, 3, '等镇兽大比打完。神堂要贴，就让他们贴。'),
  (3, 1, '……好。仓库给你留了静默砂，别死在外面。');
