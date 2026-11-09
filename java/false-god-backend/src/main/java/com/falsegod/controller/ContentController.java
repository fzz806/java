package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.entity.ArenaMatch;
import com.falsegod.entity.Member;
import com.falsegod.entity.Skill;
import com.falsegod.service.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内容接口（神技图鉴 / 镇兽大比 / 恶鬼之牙 / 战力榜），
 * 路径与前端 src/api/content.js 保持一致：
 *   GET /api/skills        -> [{ id, name, level, element, transform, mastery, forbidden, desc }]
 *   GET /api/arena/matches -> [{ id, roundNo, stage, matchDate, playerA, playerB, venue, state, result }]
 *   GET /api/guild         -> { info: {k:v...}, members: [...] }
 *   GET /api/rank          -> [{ id, name, role, power, element, realm, desc }]
 *
 * 位于 /api/** 下，受 AuthInterceptor 保护，需携带 Bearer token。
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/skills")
    public Result<List<Skill>> skills() {
        return Result.ok(contentService.listSkills());
    }

    @GetMapping("/arena/matches")
    public Result<List<ArenaMatch>> matches() {
        return Result.ok(contentService.listMatches());
    }

    @GetMapping("/guild")
    public Result<Map<String, Object>> guild() {
        return Result.ok(contentService.guildOverview());
    }

    @GetMapping("/rank")
    public Result<List<Member>> rank() {
        return Result.ok(contentService.listMembers());
    }
}
