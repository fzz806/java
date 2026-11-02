package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口，路径与前端 src/api/auth.js 保持一致：
 *   POST /api/login     { username, password }
 *   POST /api/register  { username, password }
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 请求体 DTO */
    record AuthRequest(String username, String password) {
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AuthRequest req) {
        if (req == null || isBlank(req.username()) || isBlank(req.password())) {
            return Result.error(1, "真名与神印不可为空");
        }
        UserService.LoginResult r = userService.login(req.username().trim(), req.password());
        if (r.success()) {
            return Result.ok(Map.of(
                    "username", r.username(),
                    "token", "fake-jwt-" + System.currentTimeMillis()
            ));
        }
        return Result.error(r.code(), r.message());
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody AuthRequest req) {
        if (req == null || isBlank(req.username()) || isBlank(req.password())) {
            return Result.error(1, "真名与神印不可为空");
        }
        UserService.RegisterResult r = userService.register(req.username().trim(), req.password());
        if (r.success()) {
            return Result.ok(Map.of("username", r.username()));
        }
        return Result.error(r.code(), r.message());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
