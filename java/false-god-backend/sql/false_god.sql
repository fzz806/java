-- ============================================================
-- 觉醒伪神 · 登录注册后端 数据库初始化脚本
-- 环境：MySQL 5.7+ / 8.0，本地 root/123456
-- 使用方式（任选其一）：
--   1. 命令行：mysql -uroot -p123456 < false_god.sql
--   2. Navicat / DataGrip / IDEA Database 面板中直接运行本文件
-- ============================================================

CREATE DATABASE IF NOT EXISTS false_god DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE false_god;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(16)  NOT NULL COMMENT '真名（登录名，唯一）',
    password    VARCHAR(64)  NOT NULL COMMENT '神印（密码，MD5(原文+盐) 存储）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '伪神殿 · 用户表';

-- ------------------------------------------------------------
-- 测试账号（可选）：真名 chenhaoyu，神印 123456
-- 哈希为 MD5('123456' + 'falsegod')，与后端 PasswordUtil 的盐保持一致
-- ------------------------------------------------------------
INSERT INTO t_user (username, password)
VALUES ('chenhaoyu', '25d1efb94746338795761c7f0d077eb7')
ON DUPLICATE KEY UPDATE username = username;
