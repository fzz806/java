package com.falsegod.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与校验。
 *
 * <p>密钥来自配置 {@code falsegod.jwt.secret}，长度需 ≥ 32 字节（HS256 要求）。
 * 过期时间来自 {@code falsegod.jwt.expire-hours}，默认 24 小时。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(
            @Value("${falsegod.jwt.secret:falsegod-default-dev-secret-key-change-me-32bytes}") String secret,
            @Value("${falsegod.jwt.expire-hours:24}") long expireHours) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "falsegod.jwt.secret 长度不足 32 字节，HS256 无法安全使用");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireMillis = expireHours * 60 * 60 * 1000L;
    }

    /**
     * 签发 token
     *
     * @param userId   用户主键
     * @param username 真名
     */
    public String generate(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token（含过期校验）
     *
     * @return 有效则返回 Claims；无效/过期/签名错误返回 null，不抛异常
     */
    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** token 是否有效 */
    public boolean isValid(String token) {
        return parse(token) != null;
    }

    /** 从 token 取真名，无效返回 null */
    public String getUsername(String token) {
        Claims c = parse(token);
        return c == null ? null : c.getSubject();
    }

    /** 过期时间（毫秒），供前端展示或调试 */
    public long getExpireMillis() {
        return expireMillis;
    }
}
