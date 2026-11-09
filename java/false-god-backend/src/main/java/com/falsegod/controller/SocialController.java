package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.config.AuthInterceptor;
import com.falsegod.service.ChatService;
import com.falsegod.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 社交接口（好友 + 传音），路径与前端 src/api/social.js 保持一致：
 *   GET    /api/friends                              -> [{ friendId, username, remark, addedAt }]
 *   POST   /api/friends  { username }                -> { friendId, username }
 *   DELETE /api/friends/{friendId}                   -> 无 data
 *   GET    /api/chat/messages?with={uid}&after={id}  -> [{ id, mine, content, time }]
 *   POST   /api/chat/send { to, content }            -> { id, content }
 *
 * 全部位于 /api/** 下，默认受 AuthInterceptor 保护，需携带 Bearer token。
 */
@RestController
@RequestMapping("/api")
public class SocialController {

    private final FriendService friendService;
    private final ChatService chatService;

    public SocialController(FriendService friendService, ChatService chatService) {
        this.friendService = friendService;
        this.chatService = chatService;
    }

    /** 请求体 DTO */
    record AddFriendRequest(String username) {
    }

    record SendMessageRequest(Long to, String content) {
    }

    @GetMapping("/friends")
    public Result<List<Map<String, Object>>> friends(HttpServletRequest request) {
        Long uid = requireUid(request);
        if (uid == null) {
            return Result.error(401, "神印凭证无效");
        }
        return Result.ok(friendService.listFriends(uid));
    }

    @PostMapping("/friends")
    public Result<Map<String, Object>> addFriend(@RequestBody AddFriendRequest req,
                                                 HttpServletRequest request) {
        Long uid = requireUid(request);
        if (uid == null) {
            return Result.error(401, "神印凭证无效");
        }
        if (req == null || req.username() == null || req.username().isBlank()) {
            return Result.error(1, "请填写要结契的真名");
        }
        FriendService.AddResult r = friendService.addFriend(uid, req.username().trim());
        if (r.success()) {
            Map<String, Object> data = new HashMap<>();
            data.put("friendId", r.friendId());
            data.put("username", r.username());
            return Result.ok(data);
        }
        return Result.error(r.code(), r.message());
    }

    @DeleteMapping("/friends/{friendId}")
    public Result<Void> removeFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long uid = requireUid(request);
        if (uid == null) {
            return Result.error(401, "神印凭证无效");
        }
        if (!friendService.removeFriend(uid, friendId)) {
            return Result.error(1, "此名不在你的好友名录之中");
        }
        return Result.ok();
    }

    @GetMapping("/chat/messages")
    public Result<List<Map<String, Object>>> messages(
            @RequestParam("with") Long other,
            @RequestParam(value = "after", defaultValue = "0") long afterId,
            HttpServletRequest request) {
        Long uid = requireUid(request);
        if (uid == null) {
            return Result.error(401, "神印凭证无效");
        }
        if (!friendService.isFriend(uid, other)) {
            return Result.error(1, "非结契好友，无法查阅传音");
        }
        return Result.ok(chatService.pullMessages(uid, other, afterId));
    }

    @PostMapping("/chat/send")
    public Result<Map<String, Object>> send(@RequestBody SendMessageRequest req,
                                            HttpServletRequest request) {
        Long uid = requireUid(request);
        if (uid == null) {
            return Result.error(401, "神印凭证无效");
        }
        if (req == null || req.to() == null) {
            return Result.error(1, "缺少传音对象");
        }
        ChatService.SendResult r = chatService.send(uid, req.to(), req.content());
        if (r.success()) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", r.id());
            data.put("content", r.content());
            return Result.ok(data);
        }
        return Result.error(r.code(), r.message());
    }

    private static Long requireUid(HttpServletRequest request) {
        return (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    }
}
