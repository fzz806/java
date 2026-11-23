package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.config.AuthInterceptor;
import com.falsegod.service.GuildService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 恶鬼之牙公会完整接口。与旧 {@code /api/guild}（只读简报）并存，
 * 本控制器提供可交互的公会系统，路径统一前缀 {@code /api/guild/}。
 *
 * <p>全部位于 /api/** 下，受 AuthInterceptor 保护，需携带 Bearer token；
 * 列表里的 id / username 由拦截器从 JWT 注入。
 */
@RestController
@RequestMapping("/api/guild")
public class GuildController {

    private final GuildService guildService;

    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    /** GET /api/guild/overview —— 公会总览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId,
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username) {
        return Result.ok(guildService.overview(userId, username));
    }

    /** POST /api/guild/apply { reason } —— 递交入会名帖 */
    @PostMapping("/apply")
    public Result<Void> apply(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId,
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username,
            @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null ? "" : String.valueOf(body.getOrDefault("reason", ""));
        return toResult(guildService.apply(userId, username, reason));
    }

    /** POST /api/guild/{id}/handle?pass=true&note=... —— 批阅名帖（仅会长） */
    @PostMapping("/{id}/handle")
    public Result<Void> handleApply(
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username,
            @PathVariable("id") Long id,
            @RequestParam(name = "pass", defaultValue = "true") boolean pass,
            @RequestParam(name = "note", required = false) String note) {
        return toResult(guildService.handleApply(username, id, pass, note));
    }

    /** POST /api/guild/quit —— 退出公会 */
    @PostMapping("/quit")
    public Result<Void> quit(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId) {
        return toResult(guildService.quit(userId));
    }

    /** PUT /api/guild/members/{id}/role { role } —— 任免职位（仅会长） */
    @PutMapping("/members/{id}/role")
    public Result<Void> setRole(
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username,
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        String role = body == null ? "" : String.valueOf(body.getOrDefault("role", ""));
        return toResult(guildService.setRole(username, id, role));
    }

    /** DELETE /api/guild/members/{id} —— 移除成员（仅会长） */
    @DeleteMapping("/members/{id}")
    public Result<Void> removeMember(
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username,
            @PathVariable("id") Long id) {
        return toResult(guildService.removeMember(username, id));
    }

    /** POST /api/guild/notice { tag, title, content } —— 发布公告（仅会长） */
    @PostMapping("/notice")
    public Result<Void> publishNotice(
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = Map.of();
        String tag = String.valueOf(body.getOrDefault("tag", "通告"));
        String title = String.valueOf(body.getOrDefault("title", ""));
        String content = String.valueOf(body.getOrDefault("content", ""));
        return toResult(guildService.publishNotice(username, tag, title, content));
    }

    /** DELETE /api/guild/notice/{id} —— 撤下公告（仅会长） */
    @DeleteMapping("/notice/{id}")
    public Result<Void> deleteNotice(
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username,
            @PathVariable("id") Long id) {
        return toResult(guildService.deleteNotice(username, id));
    }

    /** PUT /api/guild/info { key, value } —— 修订简报（仅会长） */
    @PutMapping("/info")
    public Result<Void> updateInfo(
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = Map.of();
        String key = String.valueOf(body.getOrDefault("key", ""));
        String value = String.valueOf(body.getOrDefault("value", ""));
        return toResult(guildService.updateInfo(username, key, value));
    }

    /** POST /api/guild/contribute —— 每日供奉换贡献 */
    @PostMapping("/contribute")
    public Result<Void> contribute(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId,
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username) {
        return toResult(guildService.contribute(userId, username));
    }

    private static Result<Void> toResult(GuildService.ActionResult r) {
        return r.isOk() ? Result.ok() : Result.error(1, r.getMessage());
    }
}
