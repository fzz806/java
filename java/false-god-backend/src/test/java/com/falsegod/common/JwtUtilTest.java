package com.falsegod.common;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JwtUtil token 签发与校验")
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 24);
    }

    @Test
    @DisplayName("签发的 token 可被解析出真名与 uid")
    void generateAndParse() {
        String token = jwtUtil.generate(1L, "chenhaoyu");
        assertNotNull(token);
        assertTrue(token.contains("."), "JWT 应为三段式");

        Claims claims = jwtUtil.parse(token);
        assertNotNull(claims);
        assertEquals("chenhaoyu", claims.getSubject());
        assertEquals(1, ((Number) claims.get("uid")).intValue());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("无效/伪造 token 解析为 null 而不抛异常")
    void invalidToken() {
        assertNull(jwtUtil.parse("not-a-token"));
        assertNull(jwtUtil.parse("a.b.c"));
        assertNull(jwtUtil.parse(""));
        assertNull(jwtUtil.parse(null));
        assertFalse(jwtUtil.isValid("garbage"));
    }

    @Test
    @DisplayName("被篡改的 token 校验失败")
    void tamperedToken() {
        String token = jwtUtil.generate(1L, "chenhaoyu");
        String wrongSecret = new JwtUtil("another-secret-key-with-enough-length-32b", 24)
                .generate(1L, "chenhaoyu");
        assertNull(jwtUtil.parse(wrongSecret), "不同密钥签发的 token 不应通过");
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    @DisplayName("过期 token 校验失败")
    void expiredToken() {
        // expireHours = 0 -> 立即过期
        JwtUtil shortLived = new JwtUtil(SECRET, 0);
        String token = shortLived.generate(2L, "expiredUser");
        assertNull(shortLived.parse(token), "过期 token 应校验失败");
    }

    @Test
    @DisplayName("密钥不足 32 字节直接拒绝启动")
    void shortSecretRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new JwtUtil("too-short", 24));
        assertTrue(e.getMessage().contains("32"));
    }

    @Test
    @DisplayName("getUsername 对无效 token 返回 null")
    void getUsernameSafe() {
        String token = jwtUtil.generate(3L, "abc");
        assertEquals("abc", jwtUtil.getUsername(token));
        assertNull(jwtUtil.getUsername("bad"));
    }

    @Test
    @DisplayName("过期时长按配置计算")
    void expireMillis() {
        assertEquals(24 * 60 * 60 * 1000L, jwtUtil.getExpireMillis());
    }
}
