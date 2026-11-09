package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 镇兽大比赛程实体，对应 MySQL 表 t_arena_match
 */
@TableName("t_arena_match")
public class ArenaMatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 轮次序号 */
    private Integer roundNo;

    /** 阶段：十六强赛 / 八强赛 / 半决赛 / 决赛 */
    private String stage;

    /** 日期 MM.dd */
    private String matchDate;

    private String playerA;

    private String playerB;

    private String venue;

    /** 状态：未开始 / 待定 / 进行中 / 已结束 */
    private String state;

    /** 赛果（可为空） */
    private String result;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(String matchDate) {
        this.matchDate = matchDate;
    }

    public String getPlayerA() {
        return playerA;
    }

    public void setPlayerA(String playerA) {
        this.playerA = playerA;
    }

    public String getPlayerB() {
        return playerB;
    }

    public void setPlayerB(String playerB) {
        this.playerB = playerB;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
