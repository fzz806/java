package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 经验流水，对应 MySQL 表 t_exp_log。
 *
 * <p>任务面板底部「最近所得」用它，只保留最近若干条（读取时 limit）。
 * 与 t_power_log 一样，写流水不参与业务判断，删了也不影响数据正确性。
 */
@TableName("t_exp_log")
public class ExpLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 如：完成「每日登临」 */
    private String text;

    /** 本次获得经验 */
    private Integer exp;

    private LocalDateTime createdAt;

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
