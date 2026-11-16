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
 */
@TableName("t_power")
public class Power {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 t_user.id */
    private Long userId;

    /** 境界序号，0 起，对应 PowerService.REALMS */
    private Integer realmIndex;

    /** 当前境界内的红印重数 0-9，满 9 可突破 */
    private Integer seal;

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

    public Integer getSeal() {
        return seal;
    }

    public void setSeal(Integer seal) {
        this.seal = seal;
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
