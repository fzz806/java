package com.falsegod.controller;

import com.falsegod.common.Result;
import com.falsegod.config.AuthInterceptor;
import com.falsegod.entity.ArenaMatch;
import com.falsegod.entity.Member;
import com.falsegod.entity.Skill;
import com.falsegod.service.ContentService;
import com.falsegod.service.PowerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 内容接口（神技图鉴 / 镇兽大比 / 恶鬼之牙 / 战力榜），
 * 路径与前端 src/api/content.js 保持一致：
 *   GET /api/skills        -> [{ id, name, level, element, transform, mastery, forbidden, desc }]
 *   GET /api/arena/matches -> [{ id, roundNo, stage, matchDate, playerA, playerB, venue, state, result }]
 *   GET /api/guild         -> { info: {k:v...}, members: [...], memberCount }
 *                             （members 含当前玩家那条 self=true，简报「在册 N 人」会同步成真实人数）
 *   GET /api/rank          -> [{ id, name, role, power, element, realm, bio, self }]
 *
 * 位于 /api/** 下，受 AuthInterceptor 保护，需携带 Bearer token。
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    private final ContentService contentService;
    private final PowerService powerService;

    public ContentController(ContentService contentService, PowerService powerService) {
        this.contentService = contentService;
        this.powerService = powerService;
    }

    @GetMapping("/skills")
    public Result<List<Skill>> skills() {
        return Result.ok(contentService.listSkills());
    }

    @GetMapping("/arena/matches")
    public Result<List<ArenaMatch>> matches() {
        return Result.ok(contentService.listMatches());
    }

    /**
     * 恶鬼之牙：简报 + 名册（战力倒序）。
     *
     * <p>与战力榜同一套做法：把当前玩家并入名册，玩家那条带 {@code self=true}，
     * 战力/境界取自战力档案。同时把简报里的「在册 N 人」改写成真实人数，
     * 免得出现「名册 8 条、简报写 7 人」这种自相矛盾。
     *
     * <p>另外单独给出 {@code memberCount}，前端不必再靠解析文案取人数。
     */
    @GetMapping("/guild")
    public Result<Map<String, Object>> guild(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId,
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username) {
        Map<String, Object> data = contentService.guildOverview();

        List<Member> members = new ArrayList<>(contentService.listMembers());
        if (userId != null) {
            members.add(selfEntry(userId, username));
            members.sort(Comparator
                    .comparingLong((Member m) -> m.getPower() == null ? 0L : m.getPower())
                    .reversed());
        }
        data.put("members", members);
        data.put("memberCount", members.size());
        syncHeadcount(data, members.size());
        return Result.ok(data);
    }

    /** 把简报里的「在册 N 人」对齐到真实人数；文案其余部分原样保留 */
    @SuppressWarnings("unchecked")
    private void syncHeadcount(Map<String, Object> data, int count) {
        Object raw = data.get("info");
        if (!(raw instanceof Map)) {
            return;
        }
        Map<String, String> info = (Map<String, String>) raw;
        String text = info.get("members");
        if (text == null) {
            return;
        }
        info.put("members", text.replaceFirst("在册\\s*\\d+\\s*人", "在册 " + count + " 人"));
    }

    /** 战力榜：静态成员与「当前玩家」合并后按战力倒序。
     *
     * <p>玩家的战力与境界取自 {@link PowerService}，所以修行提升后名次会立刻变化，
     * 不会再出现「练到超越榜首却仍不在榜上」的割裂感。
     * 玩家那条带 {@code self=true}，前端据此高亮。
     */
    @GetMapping("/rank")
    public Result<List<Member>> rank(
            @RequestAttribute(name = AuthInterceptor.ATTR_USER_ID, required = false) Long userId,
            @RequestAttribute(name = AuthInterceptor.ATTR_USERNAME, required = false) String username) {
        List<Member> members = new ArrayList<>(contentService.listMembers());
        if (userId != null) {
            members.add(selfEntry(userId, username));
            members.sort(Comparator
                    .comparingLong((Member m) -> m.getPower() == null ? 0L : m.getPower())
                    .reversed());
        }
        return Result.ok(members);
    }

    /** 把当前玩家拼成一条榜单记录，战力/境界取自战力档案 */
    private Member selfEntry(Long userId, String username) {
        Map<String, Object> panel = powerService.panel(userId);

        Member me = new Member();
        me.setId(-1L);  // 不落库，负数占位避免与 t_member 主键冲突
        me.setName(username == null || username.isBlank() ? "无名者" : username);
        me.setRole("神弃者");
        me.setPower(panel.get("power") instanceof Number n ? n.longValue() : 0L);
        me.setElement("伪神");
        me.setRealm(String.valueOf(panel.getOrDefault("realm", "一印 · 初印")));
        me.setBio("你自己 · 战力随修行实时变动");
        me.setSelf(true);
        return me;
    }
}
