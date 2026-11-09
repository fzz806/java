package com.falsegod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.falsegod.entity.ArenaMatch;
import com.falsegod.entity.GuildInfo;
import com.falsegod.entity.Member;
import com.falsegod.entity.Skill;
import com.falsegod.mapper.ArenaMatchMapper;
import com.falsegod.mapper.GuildInfoMapper;
import com.falsegod.mapper.MemberMapper;
import com.falsegod.mapper.SkillMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容业务：神技图鉴 / 镇兽大比赛程 / 恶鬼之牙公会 / 战力榜
 */
@Service
public class ContentService {

    private final SkillMapper skillMapper;
    private final ArenaMatchMapper arenaMatchMapper;
    private final MemberMapper memberMapper;
    private final GuildInfoMapper guildInfoMapper;

    public ContentService(SkillMapper skillMapper,
                          ArenaMatchMapper arenaMatchMapper,
                          MemberMapper memberMapper,
                          GuildInfoMapper guildInfoMapper) {
        this.skillMapper = skillMapper;
        this.arenaMatchMapper = arenaMatchMapper;
        this.memberMapper = memberMapper;
        this.guildInfoMapper = guildInfoMapper;
    }

    /** 神技图鉴（禁忌技置顶，同级按熟练度倒序） */
    public List<Skill> listSkills() {
        return skillMapper.selectList(
                new LambdaQueryWrapper<Skill>()
                        .orderByDesc(Skill::getForbidden)
                        .orderByDesc(Skill::getMastery)
        );
    }

    /** 镇兽大比赛程（按轮次正序） */
    public List<ArenaMatch> listMatches() {
        return arenaMatchMapper.selectList(
                new LambdaQueryWrapper<ArenaMatch>().orderByAsc(ArenaMatch::getRoundNo)
        );
    }

    /** 恶鬼之牙：简报 + 成员（战力倒序） */
    public Map<String, Object> guildOverview() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, String> info = new LinkedHashMap<>();
        for (GuildInfo g : guildInfoMapper.selectList(null)) {
            info.put(g.getK(), g.getV());
        }
        data.put("info", info);
        data.put("members", listMembers());
        return data;
    }

    /** 战力榜（战力倒序） */
    public List<Member> listMembers() {
        return memberMapper.selectList(
                new LambdaQueryWrapper<Member>().orderByDesc(Member::getPower)
        );
    }
}
