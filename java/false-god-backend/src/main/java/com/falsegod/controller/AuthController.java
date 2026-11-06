package com.falsegod.controller;

import com.falsegod.common.JwtUtil;
import com.falsegod.common.Result;
import com.falsegod.config.AuthInterceptor;
import com.falsegod.entity.User;
import com.falsegod.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口，路径与前端 src/api/auth.js 保持一致：
 *   POST /api/login      { username, password }  -> { username, token, expiresIn }
 *   POST /api/register   { username, password }  -> { username }
 *   GET  /api/me         需 Bearer token         -> { id, username, createdAt }
 *   POST /api/logout     需 Bearer token         -> 无状态 JWT，仅前端清除即可
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /** 请求体 DTO */
    record AuthRequest(String username, String password) {
    }

    /** 健康检查，前端可用来判断后端是否已就绪 */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        return Result.ok(Map.of("status", "up", "ts", System.currentTimeMillis()));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AuthRequest req) {
        if (req == null || isBlank(req.username()) || isBlank(req.password())) {
            return Result.error(1, "真名与神印不可为空");
        }
        UserService.LoginResult r = userService.login(req.username().trim(), req.password());
        if (r.success()) {
            String token = jwtUtil.generate(r.userId(), r.username());
            Map<String, Object> data = new HashMap<>();
            data.put("username", r.username());
            data.put("token", token);
            data.put("expiresIn", jwtUtil.getExpireMillis());
            return Result.ok(data);
        }
        return Result.error(r.code(), r.message());
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody AuthRequest req) {
        if (req == null || isBlank(req.username()) || isBlank(req.password())) {
            return Result.error(1, "真名与神印不可为空");
        }
        if (req.password().length() < 6) {
            return Result.error(3, "神印至少需要 6 位，方能承受神性");
        }
        UserService.RegisterResult r = userService.register(req.username().trim(), req.password());
        if (r.success()) {
            return Result.ok(Map.of("username", r.username()));
        }
        return Result.error(r.code(), r.message());
    }

    /**
     * 当前登录用户。由 AuthInterceptor 保证进入时 token 已校验通过，
     * 这里再从数据库取一次最新信息（用户名可能被改动、账号可能被删除）。
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpServletRequest request) {
        Long uid = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (uid == null) {
            return Result.error(401, "神印凭证无效");
        }
        User user = userService.findById(uid);
        if (user == null) {
            return Result.error(401, "该真名已不在神堂名册");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("createdAt", user.getCreatedAt());
        return Result.ok(data);
    }

    /** 无状态 JWT，服务端无需处理，前端清除本地 token 即可 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
