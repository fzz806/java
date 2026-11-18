package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户战力档案，对应 MySQL 表 t_power。
 *
 * <p>每个用户一行，首次访问 /api/power 时惰性建档。
 * 综合战力不落库，由 {@code PowerService#calcPower} 按各维度实时推导，
 * 避免多个地方各自写数字导致对不上。
 *
 * <p>境界分两轴：{@code realmIndex} 是大境界序号（一印~八印、半神、真神），
 * {@code stage} 是该大境界内的小境界序号（初印/中印/圆满/巅峰）。
 */
@TableName("t_power")
public class Power {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 t_user.id */
    private Long userId;

    /** 大境界序号，0 起，对应 PowerService.REALMS（一印..八印、半神、真神） */
    private Integer realmIndex;

    /** 大境界内的小境界序号 0-3：初印/中印/圆满/巅峰（半神为初阶/中阶/圆满/巅峰） */
    private Integer stage;

    /** 当前灵能 */
    private Integer spirit;

    /** 灵能上限 */
    private Integer spiritMax;

    /** 已习神技数 */
    private Integer skills;

    /** 本源枚数 */
    private Integer essence;

    /** 最后变动时间 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRealmIndex() {
        return realmIndex;
    }

    public void setRealmIndex(Integer realmIndex) {
        this.realmIndex = realmIndex;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getSpirit() {
        return spirit;
    }

    public void setSpirit(Integer spirit) {
        this.spirit = spirit;
    }

    public Integer getSpiritMax() {
        return spiritMax;
    }

    public void setSpiritMax(Integer spiritMax) {
        this.spiritMax = spiritMax;
    }

    public Integer getSkills() {
        return skills;
    }

    public void setSkills(Integer skills) {
        this.skills = skills;
    }

    public Integer getEssence() {
        return essence;
    }

    public void setEssence(Integer essence) {
        this.essence = essence;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
