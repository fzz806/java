package com.falsegod.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码哈希工具。
 *
 * <p>当前算法：BCrypt（自带随机盐、可调强度、抗彩虹表），输出以 {@code $2a$} 开头。
 *
 * <p>兼容旧数据：项目早期使用「MD5(原文 + 固定盐)」，老用户库里仍是 32 位十六进制串。
 * 这类密码在 {@link #matches} 中仍可校验通过，但登录成功后会被
 * {@link #needsUpgrade} 判定为需要升级，由 UserService 自动改写为 BCrypt。
 */
public final class PasswordUtil {

    /** BCrypt 强度因子，10 为业界常用默认值（约 50~100ms/次） */
    private static final int BCRYPT_STRENGTH = 10;

    /** 旧版固定盐，仅用于兼容历史 MD5 数据，新数据不再使用 */
    private static final String LEGACY_SALT = "falsegod";

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    private PasswordUtil() {
    }

    /** 生成新密码哈希（BCrypt） */
    public static String hash(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验密码。同时支持 BCrypt 与历史 MD5 格式。
     *
     * @param rawPassword 用户输入的明文
     * @param storedHash  数据库中的哈希
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            return ENCODER.matches(rawPassword, storedHash);
        }
        // 历史 MD5 数据
        return legacyMd5(rawPassword).equalsIgnoreCase(storedHash);
    }

    /** 该哈希是否需要用 BCrypt 重新计算（历史 MD5 或强度过低） */
    public static boolean needsUpgrade(String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return true;
        }
        if (!isBcrypt(storedHash)) {
            return true;
        }
        // 强度低于当前配置时也顺手升级
        try {
            return extractStrength(storedHash) < BCRYPT_STRENGTH;
        } catch (Exception e) {
            return false;
        }
    }

    /** 是否为 BCrypt 格式（$2a$ / $2b$ / $2y$ 前缀） */
    public static boolean isBcrypt(String hash) {
        return hash != null
                && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private static int extractStrength(String bcryptHash) {
        // 格式：$2a$10$...
        String[] parts = bcryptHash.split("\\$");
        return Integer.parseInt(parts[2]);
    }

    private static String legacyMd5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((text + LEGACY_SALT).getBytes(StandardCharsets.UTF_8));
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
