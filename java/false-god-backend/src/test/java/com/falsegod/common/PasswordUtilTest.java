package com.falsegod.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PasswordUtil 密码哈希")
class PasswordUtilTest {

    @Test
    @DisplayName("BCrypt 哈希能被校验通过")
    void hashThenMatches() {
        String hash = PasswordUtil.hash("123456");
        assertTrue(PasswordUtil.isBcrypt(hash), "应为 BCrypt 格式");
        assertTrue(PasswordUtil.matches("123456", hash));
    }

    @Test
    @DisplayName("错误密码校验失败")
    void wrongPasswordFails() {
        String hash = PasswordUtil.hash("123456");
        assertFalse(PasswordUtil.matches("1234567", hash));
        assertFalse(PasswordUtil.matches("", hash));
    }

    @Test
    @DisplayName("相同密码两次哈希结果不同（随机盐）")
    void saltIsRandom() {
        String h1 = PasswordUtil.hash("samePassword");
        String h2 = PasswordUtil.hash("samePassword");
        assertNotEquals(h1, h2, "BCrypt 每次应生成不同盐值");
        assertTrue(PasswordUtil.matches("samePassword", h1));
        assertTrue(PasswordUtil.matches("samePassword", h2));
    }

    @Test
    @DisplayName("兼容历史 MD5(原文+falsegod) 格式")
    void legacyMd5StillWorks() throws Exception {
        // md5("123456falsegod")
        String legacy = md5Hex("123456" + "falsegod");
        assertEquals(32, legacy.length());
        assertFalse(PasswordUtil.isBcrypt(legacy));
        assertTrue(PasswordUtil.matches("123456", legacy), "旧 MD5 密码应仍可登录");
        assertFalse(PasswordUtil.matches("wrong", legacy));
    }

    @Test
    @DisplayName("历史 MD5 会被标记为需要升级")
    void legacyNeedsUpgrade() throws Exception {
        assertTrue(PasswordUtil.needsUpgrade(md5Hex("123456falsegod")));
        assertTrue(PasswordUtil.needsUpgrade(null));
        assertTrue(PasswordUtil.needsUpgrade(""));
        assertFalse(PasswordUtil.needsUpgrade(PasswordUtil.hash("123456")));
    }

    @Test
    @DisplayName("null / 空输入不抛异常")
    void nullSafety() {
        assertFalse(PasswordUtil.matches(null, "x"));
        assertFalse(PasswordUtil.matches("x", null));
        assertFalse(PasswordUtil.matches("x", ""));
        assertFalse(PasswordUtil.isBcrypt(null));
    }

    private static String md5Hex(String text) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("MD5")
                .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
