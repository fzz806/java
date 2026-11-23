package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.falsegod.entity.GuildApply;
import com.falsegod.entity.GuildContribution;
import com.falsegod.entity.GuildInfo;
import com.falsegod.entity.GuildNotice;
import com.falsegod.entity.Member;
import com.falsegod.entity.Power;
import com.falsegod.entity.WarehouseItem;
import com.falsegod.mapper.GuildApplyMapper;
import com.falsegod.mapper.GuildContributionMapper;
import com.falsegod.mapper.GuildInfoMapper;
import com.falsegod.mapper.GuildNoticeMapper;
import com.falsegod.mapper.MemberMapper;
import com.falsegod.mapper.PowerMapper;
import com.falsegod.mapper.WarehouseItemMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 恶鬼之牙公会业务：名册 / 入会申请 / 审批 / 职位 / 公告 / 仓库 / 供奉贡献。
 *
 * <p>会长由配置 {@code falsegod.guild.master} 指定（默认 chenhaoyu），
 * 仅会长可执行审批、任免、移除、发公告、改简报等管理动作。
 * 剧情 NPC（user_id 为空）与真实在册玩家同表存放，前端按 self 标记本人。
 */
@Service
public class GuildService {

    /** 单次供奉消耗灵能 */
    private static final int CONTRIBUTE_COST = 20;
    /** 单次供奉获得贡献 */
    private static final int CONTRIBUTE_GAIN = 30;

    private final MemberMapper memberMapper;
    private final GuildInfoMapper guildInfoMapper;
    private final GuildApplyMapper guildApplyMapper;
    private final GuildNoticeMapper guildNoticeMapper;
    private final GuildContributionMapper contributionMapper;
    private final WarehouseItemMapper warehouseItemMapper;
    private final PowerMapper powerMapper;
    private final PowerService powerService;
    private final String masterName;

    public GuildService(MemberMapper memberMapper,
                        GuildInfoMapper guildInfoMapper,
                        GuildApplyMapper guildApplyMapper,
                        GuildNoticeMapper guildNoticeMapper,
                        GuildContributionMapper contributionMapper,
                        WarehouseItemMapper warehouseItemMapper,
                        PowerMapper powerMapper,
                        PowerService powerService,
                        @Value("${falsegod.guild.master:chenhaoyu}") String masterName) {
        this.memberMapper = memberMapper;
        this.guildInfoMapper = guildInfoMapper;
        this.guildApplyMapper = guildApplyMapper;
        this.guildNoticeMapper = guildNoticeMapper;
        this.contributionMapper = contributionMapper;
        this.warehouseItemMapper = warehouseItemMapper;
        this.powerMapper = powerMapper;
        this.powerService = powerService;
        this.masterName = masterName;
    }

    /* ==================== 总览 ==================== */

    /** 公会总览：简报 + 名册 + 仓库 + 公告 + 贡献榜 + 我的状态 */
    public Map<String, Object> overview(Long userId, String username) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 简报（键值）
        Map<String, String> info = new LinkedHashMap<>();
        for (GuildInfo g : guildInfoMapper.selectList(null)) {
            info.put(g.getK(), g.getV());
        }
        data.put("info", info);

        // 名册（战力倒序，标记本人）
        List<Member> members = listMembersWithSelf(userId, username);
        data.put("members", members);
        data.put("memberCount", members.size());
        // 简报里的「在册 N 人」对齐真实人数，避免自相矛盾
        String headcount = info.get("members");
        if (headcount != null) {
            info.put("members", headcount.replaceFirst("在册\\s*\\d+\\s*人", "在册 " + members.size() + " 人"));
        }

        // 仓库
        List<Map<String, Object>> warehouse = new ArrayList<>();
        for (WarehouseItem w : warehouseItemMapper.selectList(
                new LambdaQueryWrapper<WarehouseItem>().orderByAsc(WarehouseItem::getSortNo))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", w.getName());
            m.put("count", w.getCount());
            m.put("rarity", w.getRarity());
            warehouse.add(m);
        }
        data.put("warehouse", warehouse);

        // 公告
        List<Map<String, Object>> notices = new ArrayList<>();
        for (GuildNotice n : guildNoticeMapper.selectList(
                new LambdaQueryWrapper<GuildNotice>()
                        .orderByAsc(GuildNotice::getSortNo)
                        .orderByDesc(GuildNotice::getId))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("tag", n.getTag());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("author", n.getAuthor());
            notices.add(m);
        }
        data.put("notices", notices);

        // 贡献榜（按累计贡献倒序，取前十）
        data.put("rank", contributionRank());

        // 我的状态
        data.put("me", myState(userId, username));

        // 待审申请（仅会长可见）
        data.put("applies", isMaster(username) ? pendingApplies() : List.of());

        return data;
    }

    /** 名册：真实在册成员 + 当前玩家（未入会时临时占位）按战力倒序 */
    private List<Member> listMembersWithSelf(Long userId, String username) {
        List<Member> members = new ArrayList<>(memberMapper.selectList(
                new LambdaQueryWrapper<Member>().orderByDesc(Member::getPower)));

        // 在列表里的那个实例上标记 self（不能只标记 findMemberByUser 另查出的对象，那是不同实例）
        boolean inRoster = false;
        if (userId != null) {
            for (Member m : members) {
                if (userId.equals(m.getUserId())) {
                    m.setSelf(true);
                    inRoster = true;
                    break;
                }
            }
        }
        if (!inRoster && userId != null) {
            // 未入会：临时占位，让玩家看到自己的战力位置，但不落库
            Member ghost = new Member();
            ghost.setId(-1L);
            ghost.setName(username == null || username.isBlank() ? "无名者" : username);
            ghost.setRole("未入会");
            Map<String, Object> panel = powerService.panel(userId);
            ghost.setPower(panel.get("power") instanceof Number n ? n.longValue() : 0L);
            ghost.setElement("伪神");
            ghost.setRealm(String.valueOf(panel.getOrDefault("realm", "一印 · 初印")));
            ghost.setBio("递交名帖即可在册");
            ghost.setSelf(true);
            members.add(ghost);
        }
        members.sort(Comparator.comparingLong((Member m) -> m.getPower() == null ? 0L : m.getPower()).reversed());
        return members;
    }

    /** 贡献榜：按 user 聚合累计贡献，倒序取前十 */
    private List<Map<String, Object>> contributionRank() {
        List<GuildContribution> rows = contributionMapper.selectList(null);
        Map<Long, Integer> totals = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (GuildContribution c : rows) {
            totals.merge(c.getUserId(), nz(c.getAmount()), Integer::sum);
            names.putIfAbsent(c.getUserId(), c.getUsername());
        }
        List<Map.Entry<Long, Integer>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<Map<String, Object>> rank = new ArrayList<>();
        int i = 1;
        for (Map.Entry<Long, Integer> e : sorted) {
            if (i > 10) break;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("no", i++);
            m.put("userId", e.getKey());
            m.put("name", names.get(e.getKey()));
            m.put("amount", e.getValue());
            rank.add(m);
        }
        return rank;
    }

    /** 我的公会状态 */
    private Map<String, Object> myState(Long userId, String username) {
        Map<String, Object> me = new LinkedHashMap<>();
        boolean master = isMaster(username);
        Member member = findMemberByUser(userId);
        boolean joined = member != null;

        me.put("username", username == null ? "" : username);
        me.put("joined", joined);
        me.put("role", joined ? member.getRole() : "");
        me.put("contribution", joined ? nz(member.getContribution()) : 0);
        me.put("master", master);

        // 我的申请状态：优先待审，其次最近一条
        String applyStatus = "";
        if (userId != null) {
            List<GuildApply> myApplies = guildApplyMapper.selectList(
                    new LambdaQueryWrapper<GuildApply>()
                            .eq(GuildApply::getUserId, userId)
                            .orderByDesc(GuildApply::getId));
            if (!myApplies.isEmpty()) {
                applyStatus = myApplies.get(0).getStatus();
            }
        }
        me.put("applyStatus", applyStatus);

        // 今日是否已供奉
        boolean contributedToday = false;
        if (userId != null) {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            contributedToday = contributionMapper.selectCount(
                    new LambdaQueryWrapper<GuildContribution>()
                            .eq(GuildContribution::getUserId, userId)
                            .ge(GuildContribution::getCreatedAt, start)) > 0;
        }
        me.put("contributedToday", contributedToday);

        return me;
    }

    private List<Map<String, Object>> pendingApplies() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GuildApply a : guildApplyMapper.selectList(
                new LambdaQueryWrapper<GuildApply>()
                        .eq(GuildApply::getStatus, "pending")
                        .orderByAsc(GuildApply::getId))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("userId", a.getUserId());
            m.put("username", a.getUsername());
            m.put("reason", a.getReason());
            m.put("createdAt", a.getCreatedAt() == null ? "" : a.getCreatedAt().toString());
            list.add(m);
        }
        return list;
    }

    /* ==================== 入会 / 审批 / 退出 ==================== */

    /** 递交名帖：申请入会 */
    public ActionResult apply(Long userId, String username, String reason) {
        if (userId == null) return ActionResult.fail("请先登临再递交名帖");
        if (findMemberByUser(userId) != null) return ActionResult.fail("你已在恶鬼之牙名册之中");
        Long pending = guildApplyMapper.selectCount(
                new LambdaQueryWrapper<GuildApply>()
                        .eq(GuildApply::getUserId, userId)
                        .eq(GuildApply::getStatus, "pending"));
        if (pending > 0) return ActionResult.fail("名帖已递上，静待会长朱批");

        GuildApply a = new GuildApply();
        a.setUserId(userId);
        a.setUsername(username == null ? "无名者" : username);
        a.setReason(reason == null ? "" : reason.trim());
        a.setStatus("pending");
        a.setHandleNote("");
        a.setCreatedAt(LocalDateTime.now());
        guildApplyMapper.insert(a);
        return ActionResult.ok("名帖已递交，静待会长朱批");
    }

    /** 审批名帖（仅会长） */
    @Transactional
    public ActionResult handleApply(String operator, Long applyId, boolean pass, String note) {
        if (!isMaster(operator)) return ActionResult.fail("仅会长可批阅名帖");
        GuildApply a = guildApplyMapper.selectById(applyId);
        if (a == null) return ActionResult.fail("名帖不存在");
        if (!"pending".equals(a.getStatus())) return ActionResult.fail("该名帖已批阅");

        a.setStatus(pass ? "approved" : "rejected");
        a.setHandleNote(note == null ? "" : note.trim());
        a.setHandledAt(LocalDateTime.now());
        guildApplyMapper.updateById(a);

        if (pass) {
            if (findMemberByUser(a.getUserId()) != null) {
                return ActionResult.fail("此人已在册，无需重复批准");
            }
            Member m = new Member();
            m.setUserId(a.getUserId());
            m.setName(a.getUsername());
            m.setRole("新晋 · 神弃者");
            Map<String, Object> panel = powerService.panel(a.getUserId());
            m.setPower(panel.get("power") instanceof Number n ? n.longValue() : 0L);
            m.setElement("伪神");
            m.setRealm(String.valueOf(panel.getOrDefault("realm", "一印 · 初印")));
            m.setBio("新入伙的恶鬼，暂未立功");
            m.setContribution(0);
            m.setJoinedAt(LocalDateTime.now());
            memberMapper.insert(m);
            return ActionResult.ok("已朱批：许其入伙");
        }
        return ActionResult.ok("已驳回名帖");
    }

    /** 退出公会 */
    public ActionResult quit(Long userId) {
        if (userId == null) return ActionResult.fail("请先登临");
        Member me = findMemberByUser(userId);
        if (me == null) return ActionResult.fail("你本就不在名册之中");
        memberMapper.deleteById(me.getId());
        return ActionResult.ok("已除名，江湖再见");
    }

    /** 任免职位（仅会长） */
    public ActionResult setRole(String operator, Long memberId, String role) {
        if (!isMaster(operator)) return ActionResult.fail("仅会长可任免职位");
        Member m = memberMapper.selectById(memberId);
        if (m == null) return ActionResult.fail("成员不存在");
        if (isMaster(m.getName())) return ActionResult.fail("会长的位子只能由神堂来动");
        if (role == null || role.isBlank()) return ActionResult.fail("职位不可为空");
        m.setRole(role.trim());
        memberMapper.updateById(m);
        return ActionResult.ok("职位已改：" + role.trim());
    }

    /** 移除成员（仅会长） */
    public ActionResult removeMember(String operator, Long memberId) {
        if (!isMaster(operator)) return ActionResult.fail("仅会长可逐客");
        Member m = memberMapper.selectById(memberId);
        if (m == null) return ActionResult.fail("成员不存在");
        if (isMaster(m.getName())) return ActionResult.fail("会长不可自逐");
        memberMapper.deleteById(memberId);
        return ActionResult.ok("已逐出恶鬼之牙");
    }

    /* ==================== 公告 / 简报 ==================== */

    /** 发布公告（仅会长） */
    public ActionResult publishNotice(String operator, String tag, String title, String content) {
        if (!isMaster(operator)) return ActionResult.fail("仅会长可发布公告");
        if (title == null || title.isBlank()) return ActionResult.fail("标题不可为空");
        GuildNotice n = new GuildNotice();
        n.setTag(tag == null || tag.isBlank() ? "通告" : tag.trim());
        n.setTitle(title.trim());
        n.setContent(content == null ? "" : content.trim());
        n.setAuthor(operator);
        n.setSortNo(99);
        n.setCreatedAt(LocalDateTime.now());
        guildNoticeMapper.insert(n);
        return ActionResult.ok("公告已贴出");
    }

    /** 删除公告（仅会长） */
    public ActionResult deleteNotice(String operator, Long noticeId) {
        if (!isMaster(operator)) return ActionResult.fail("仅会长可撤下公告");
        guildNoticeMapper.deleteById(noticeId);
        return ActionResult.ok("公告已撤下");
    }

    /** 编辑公会简报（仅会长） */
    public ActionResult updateInfo(String operator, String key, String value) {
        if (!isMaster(operator)) return ActionResult.fail("仅会长可修订简报");
        if (key == null || key.isBlank()) return ActionResult.fail("条目不可为空");
        GuildInfo g = guildInfoMapper.selectById(key.trim());
        if (g == null) {
            g = new GuildInfo();
            g.setK(key.trim());
            g.setV(value == null ? "" : value);
            guildInfoMapper.insert(g);
        } else {
            g.setV(value == null ? "" : value);
            guildInfoMapper.updateById(g);
        }
        return ActionResult.ok("简报已修订");
    }

    /* ==================== 供奉贡献 ==================== */

    /** 每日供奉：消耗灵能换贡献，仅成员可参与，每日一次 */
    @Transactional
    public ActionResult contribute(Long userId, String username) {
        if (userId == null) return ActionResult.fail("请先登临");
        Member me = findMemberByUser(userId);
        if (me == null) return ActionResult.fail("只有名册中的恶鬼才能供奉");

        LocalDateTime start = LocalDate.now().atStartOfDay();
        Long today = contributionMapper.selectCount(
                new LambdaQueryWrapper<GuildContribution>()
                        .eq(GuildContribution::getUserId, userId)
                        .ge(GuildContribution::getCreatedAt, start));
        if (today > 0) return ActionResult.fail("今日已供奉，明日再来");

        Power power = powerMapper.selectOne(
                new LambdaQueryWrapper<Power>().eq(Power::getUserId, userId));
        if (power == null || nz(power.getSpirit()) < CONTRIBUTE_COST) {
            return ActionResult.fail("灵能不足 " + CONTRIBUTE_COST + " 点，先去修行");
        }

        // 条件扣减，防止并发超扣
        LambdaUpdateWrapper<Power> uw = new LambdaUpdateWrapper<>();
        uw.eq(Power::getId, power.getId())
          .ge(Power::getSpirit, CONTRIBUTE_COST)
          .setSql("spirit = spirit - " + CONTRIBUTE_COST)
          .set(Power::getUpdatedAt, LocalDateTime.now());
        if (powerMapper.update(null, uw) == 0) {
            return ActionResult.fail("灵能不足，先去修行");
        }

        GuildContribution c = new GuildContribution();
        c.setUserId(userId);
        c.setUsername(username == null ? "无名者" : username);
        c.setAmount(CONTRIBUTE_GAIN);
        c.setCost(CONTRIBUTE_COST);
        c.setCreatedAt(LocalDateTime.now());
        contributionMapper.insert(c);

        me.setContribution(nz(me.getContribution()) + CONTRIBUTE_GAIN);
        memberMapper.updateById(me);

        return ActionResult.ok("供奉完成：耗灵能 " + CONTRIBUTE_COST + "，得贡献 " + CONTRIBUTE_GAIN);
    }

    /* ==================== 工具 ==================== */

    public boolean isMaster(String username) {
        return username != null && username.trim().equalsIgnoreCase(masterName.trim());
    }

    private Member findMemberByUser(Long userId) {
        if (userId == null) return null;
        return memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getUserId, userId));
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 业务动作结果 */
    public static class ActionResult {
        private final boolean ok;
        private final String message;

        private ActionResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static ActionResult ok(String message) {
            return new ActionResult(true, message);
        }

        public static ActionResult fail(String message) {
            return new ActionResult(false, message);
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }
}
