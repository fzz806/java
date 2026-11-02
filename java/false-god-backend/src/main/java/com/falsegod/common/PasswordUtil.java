package com.falsegod.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 简单密码哈希工具：MD5(原文 + 固定盐)
 * 说明：这是课程/演示级别的处理方式；生产环境建议使用 BCrypt。
 */
public final class PasswordUtil {

    private static final String SALT = "falsegod";

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        return md5Hex(rawPassword + SALT);
    }

    public static boolean matches(String rawPassword, String storedHash) {
        return hash(rawPassword).equalsIgnoreCase(storedHash);
    }

    private static String md5Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
