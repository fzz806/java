mysqldump: [Warning] Using a password on the command line interface can be insecure.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `t_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(16) NOT NULL COMMENT '真名',
  `role` varchar(16) NOT NULL COMMENT '职分',
  `power` bigint NOT NULL COMMENT '综合战力',
  `element` varchar(16) NOT NULL COMMENT '元素系别',
  `realm` varchar(16) NOT NULL COMMENT '修为等级',
  `bio` varchar(120) NOT NULL COMMENT '一句话介绍',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伪神殿 · 公会成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `t_member` DISABLE KEYS */;
INSERT INTO `t_member` (`id`, `name`, `role`, `power`, `element`, `realm`, `bio`) VALUES (1,'陈昊宇','王牌',4120,'伪神（神魔同流）','五印 · 圆满','神堂判定神弃之人，现任恶鬼之牙最强战力，红印在心'),(2,'沈砚','副会长',3860,'魂系','六印 · 中印','仓库看守，掌管全部禁忌材料，话少，手黑'),(3,'老枭','猎手',3410,'兽系','六印 · 初印','镇兽塔常客，十二年猎龄，眼比鹰毒'),(4,'白鹭','医师',2980,'植物系','四印 · 巅峰','唯一能压住陈昊宇反噬的人，恶鬼之牙不能失去她'),(5,'骨七','刺客',2640,'黑暗系','四印 · 圆满','噬魂短刃，专走夜路，神堂缉杀令常客'),(6,'老葛','后勤',1870,'土系','三印 · 圆满','买什么什么涨价，囤什么什么缺货，公会命脉'),(7,'阿盐','哨探',1520,'精神系','三印 · 中印','半径十里无死角，坐镇酒馆却什么都知道');
/*!40000 ALTER TABLE `t_member` ENABLE KEYS */;
DROP TABLE IF EXISTS `t_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sender_id` bigint NOT NULL COMMENT '传音者',
  `receiver_id` bigint NOT NULL COMMENT '接收者',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '传音内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '传音时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pair` (`sender_id`,`receiver_id`,`id`) USING BTREE,
  KEY `idx_pair_rev` (`receiver_id`,`sender_id`,`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='伪神殿 · 传音消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `t_message` DISABLE KEYS */;
INSERT INTO `t_message` (`id`, `sender_id`, `receiver_id`, `content`, `created_at`) VALUES (1,3,1,'陈昊宇，神堂的缉杀令贴到塔下了，你什么时候回来？','2026-11-20 15:37:36'),(2,1,3,'等镇兽大比打完。神堂要贴，就让他们贴。','2026-11-20 15:37:36'),(3,3,1,'……好。仓库给你留了静默砂，别死在外面。','2026-11-20 15:37:36');
/*!40000 ALTER TABLE `t_message` ENABLE KEYS */;
DROP TABLE IF EXISTS `t_arena_match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_arena_match` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `round_no` int NOT NULL COMMENT '轮次序号',
  `stage` varchar(16) NOT NULL COMMENT '阶段：八强赛 / 半决赛 / 决赛',
  `match_date` varchar(16) NOT NULL COMMENT '日期 MM.dd',
  `player_a` varchar(32) NOT NULL COMMENT '选手 / 战方',
  `player_b` varchar(32) NOT NULL COMMENT '对手 / 战方',
  `venue` varchar(32) NOT NULL COMMENT '场地',
  `state` varchar(8) NOT NULL DEFAULT '未开始' COMMENT '状态：未开始 / 待定 / 进行中 / 已结束',
  `result` varchar(32) DEFAULT NULL COMMENT '赛果',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伪神殿 · 镇兽大比赛程';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `t_arena_match` DISABLE KEYS */;
INSERT INTO `t_arena_match` (`id`, `round_no`, `stage`, `match_date`, `player_a`, `player_b`, `venue`, `state`, `result`) VALUES (1,1,'十六强赛','11.02','陈昊宇','骨枭','神堂中央斗场','已结束','陈昊宇 胜 · 骨翼撕裂终结'),(2,2,'八强赛','11.12','陈昊宇','铁脊犀','神堂中央斗场','未开始',NULL),(3,3,'八强赛','11.13','白鹭','鸣砂雀','神堂中央斗场','未开始',NULL),(4,4,'半决赛','11.16','待定','待定','神堂中央斗场','待定',NULL),(5,5,'决赛','11.20','待定','待定','神堂中央斗场','待定',NULL);
/*!40000 ALTER TABLE `t_arena_match` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

