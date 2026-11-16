package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.config.AuthInterceptor;
import com.falsegod.service.PowerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 战力接口，路径与前端 src/api/power.js 保持一致：
 *   GET  /api/power           -> { power, realm, nextRealm, realmLevel, realmMax,
 *                                 spirit, spiritMax, seal, sealMax, skills, essence,
 *                                 actions: [...], logs: [...] }
 *   POST /api/power/upgrade   -> { panel: {...}, gain: number, looted?: {...} }
 *
 * <p>位于 /api/** 下，受 AuthInterceptor 保护，需携带 Bearer token；
 * 当前用户由拦截器写入的 request attribute 提供。
 */
@RestController
@RequestMapping("/api")
public class PowerController {

    private final PowerService powerService;

    public PowerController(PowerService powerService) {
        this.powerService = powerService;
    }

    @GetMapping("/power")
    public Result<Map<String, Object>> panel(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "神印凭证无效，请重新登临");
        }
        return Result.ok(powerService.panel(userId));
    }

    @PostMapping("/power/upgrade")
    public Result<Map<String, Object>> upgrade(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId,
            @RequestBody(required = false) Map<String, String> body) {
        if (userId == null) {
            return Result.error(401, "神印凭证无效，请重新登临");
        }
        String type = body == null ? null : body.get("type");
        PowerService.UpgradeOutcome outcome = powerService.upgrade(userId, type);
        if (!outcome.isOk()) {
            return Result.error(400, outcome.getMessage());
        }
        return Result.ok(outcome.getData());
    }
}
