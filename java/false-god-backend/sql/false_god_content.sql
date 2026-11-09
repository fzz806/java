-- ============================================================
-- 觉醒伪神 · 内容扩展（神技图鉴 / 镇兽大比 / 恶鬼之牙 / 战力榜）
-- 在 false_god 库执行；不动 t_user / t_friend / t_message
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 神技表（图鉴）
-- ----------------------------
DROP TABLE IF EXISTS `t_skill`;
CREATE TABLE `t_skill` (
  `id`         bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       varchar(32) NOT NULL COMMENT '神技名',
  `level`      varchar(8)  NOT NULL COMMENT '品级 A / S / SSS',
  `element`    varchar(16) NOT NULL COMMENT '元素系别',
  `transform`  varchar(16) DEFAULT NULL COMMENT '所属形态（男主专属）',
  `mastery`    int NOT NULL DEFAULT 0 COMMENT '熟练度 0-100',
  `forbidden`  tinyint NOT NULL DEFAULT 0 COMMENT '是否禁忌 1/0',
  `desc`       varchar(200) NOT NULL COMMENT '神技描述',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 神技表';

INSERT INTO `t_skill` (`name`, `level`, `element`, `transform`, `mastery`, `forbidden`, `desc`) VALUES
  ('骨翼 · 撕裂', 'A', '兽系 / 骨', '魔变', 92, 0, '第一变专属，骨翼横斩，附带灵能反馈，近身三步内无人可挡'),
  ('星环瞳 · 复写', 'S', '精神 / 空间', '神变', 78, 0, '第二变核心，复制视野内他人神技并篡改参数，被神堂列为重点观察对象'),
  ('吞噬 · 愈伤', 'B', '恶鬼系', '鬼变', 64, 0, '吞食灵能即修复伤势，代价是永无止境的饥饿'),
  ('骨刺 · 贯星', 'A', '兽系 / 骨', '魔变', 71, 0, '将骨翼拆解为十二枚骨刺，可穿透三层护盾，射程百步'),
  ('噬魂 · 鬼爪', 'A+', '黑暗系', '鬼变', 58, 1, '灵魂攻击，神印护壁挡不住，被神堂列为二级禁忌'),
  ('刚虎 · 吞天', 'S', '兽系', '兽变', 45, 0, '兽变奥义，肉身狂暴一息间撕碎神印护壁，刚虎吞天之势'),
  ('神罚 · 权柄', 'SS', '光明系', '神变', 22, 1, '借诸神权柄而行的神罚类攻击，以此审判神，一级禁忌'),
  ('禁忌 · 伪神降临', 'SSS', '神魔同流', '神变', 31, 1, '三分钟内篡改全场规则，冷却七日，神堂绝密档案编号 001');

-- ----------------------------
-- 镇兽大比赛程表
-- ----------------------------
DROP TABLE IF EXISTS `t_arena_match`;
CREATE TABLE `t_arena_match` (
  `id`         bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `round_no`   int NOT NULL COMMENT '轮次序号',
  `stage`      varchar(16) NOT NULL COMMENT '阶段：八强赛 / 半决赛 / 决赛',
  `match_date` varchar(16) NOT NULL COMMENT '日期 MM.dd',
  `player_a`   varchar(32) NOT NULL COMMENT '选手 / 战方',
  `player_b`   varchar(32) NOT NULL COMMENT '对手 / 战方',
  `venue`      varchar(32) NOT NULL COMMENT '场地',
  `state`      varchar(8)  NOT NULL DEFAULT '未开始' COMMENT '状态：未开始 / 待定 / 进行中 / 已结束',
  `result`     varchar(32) DEFAULT NULL COMMENT '赛果',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 镇兽大比赛程';

INSERT INTO `t_arena_match` (`round_no`, `stage`, `match_date`, `player_a`, `player_b`, `venue`, `state`, `result`) VALUES
  (1, '十六强赛', '11.02', '陈昊宇', '骨枭', '神堂中央斗场', '已结束', '陈昊宇 胜 · 骨翼撕裂终结'),
  (2, '八强赛',   '11.12', '陈昊宇', '铁脊犀', '神堂中央斗场', '未开始', NULL),
  (3, '八强赛',   '11.13', '白鹭', '鸣砂雀', '神堂中央斗场', '未开始', NULL),
  (4, '半决赛',   '11.16', '待定', '待定', '神堂中央斗场', '待定', NULL),
  (5, '决赛',     '11.20', '待定', '待定', '神堂中央斗场', '待定', NULL);

-- ----------------------------
-- 公会成员表（恶鬼之牙 · 兼作战力榜数据源）
-- ----------------------------
DROP TABLE IF EXISTS `t_member`;
CREATE TABLE `t_member` (
  `id`         bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       varchar(16) NOT NULL COMMENT '真名',
  `role`       varchar(16) NOT NULL COMMENT '职分',
  `power`      bigint NOT NULL COMMENT '综合战力',
  `element`    varchar(16) NOT NULL COMMENT '元素系别',
  `realm`      varchar(16) NOT NULL COMMENT '修为等级',
  `bio`        varchar(120) NOT NULL COMMENT '一句话介绍',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 公会成员表';

INSERT INTO `t_member` (`name`, `role`, `power`, `element`, `realm`, `bio`) VALUES
  ('陈昊宇', '王牌', 4120, '伪神（神魔同流）', '五印 · 摘印在即', '神堂判定神弃之人，现任恶鬼之牙最强战力，红印在心'),
  ('沈砚', '副会长', 3860, '魂系', '六印', '仓库看守，掌管全部禁忌材料，话少，手黑'),
  ('老枭', '猎手', 3410, '兽系', '六印', '镇兽塔常客，十二年猎龄，眼比鹰毒'),
  ('白鹭', '医师', 2980, '植物系', '四印', '唯一能压住陈昊宇反噬的人，恶鬼之牙不能失去她'),
  ('骨七', '刺客', 2640, '黑暗系', '四印', '噬魂短刃，专走夜路，神堂缉杀令常客'),
  ('老葛', '后勤', 1870, '土系', '三印', '买什么什么涨价，囤什么什么缺货，公会命脉'),
  ('阿盐', '哨探', 1520, '精神系', '三印', '半径十里无死角，坐镇酒馆却什么都知道');

-- 公会简报（键值）
DROP TABLE IF EXISTS `t_guild_info`;
CREATE TABLE `t_guild_info` (
  `k` varchar(16) NOT NULL,
  `v` varchar(120) NOT NULL,
  PRIMARY KEY (`k`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '伪神殿 · 公会简报';

INSERT INTO `t_guild_info` (`k`, `v`) VALUES
  ('name', '恶鬼之牙'),
  ('en', 'TEETH OF THE EVIL GHOST'),
  ('motto', '神明弃我，我自为神'),
  ('members', '在册 7 人 · 神弃者占六'),
  ('warehouse', '仓库二层已开放，需副会长以上神印'),
  ('stance', '神堂黑名单 · 缉杀令挂悬');
