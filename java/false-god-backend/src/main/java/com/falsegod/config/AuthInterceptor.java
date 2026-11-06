package com.falsegod.config;

import com.falsegod.common.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权拦截器：校验请求头 {@code Authorization: Bearer <token>}。
 *
 * <p>校验通过后把用户信息写入 request attribute，供 Controller 读取：
 * <ul>
 *   <li>{@link #ATTR_USER_ID} — Long</li>
 *   <li>{@link #ATTR_USERNAME} — String</li>
 * </ul>
 *
 * <p>未通过时直接返回 401 与统一 JSON，不进入 Controller。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "currentUserId";
    public static final String ATTR_USERNAME = "currentUsername";

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        Claims claims = jwtUtil.parse(token);

        if (claims == null) {
            writeUnauthorized(response, token == null ? "缺少神印凭证，请先登临" : "神印凭证已失效，请重新登临");
            return false;
        }

        Object uid = claims.get("uid");
        request.setAttribute(ATTR_USER_ID,
                uid instanceof Number n ? n.longValue() : null);
        request.setAttribute(ATTR_USERNAME, claims.getSubject());
        return true;
    }

    /** 从 Authorization 头或 token 参数中取出 token */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String t = header.substring(7).trim();
            return t.isEmpty() ? null : t;
        }
        // 兼容 query 参数（便于浏览器直接调试）
        String param = request.getParameter("token");
        return (param == null || param.isBlank()) ? null : param.trim();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        response.getWriter().write(body);
    }
}
