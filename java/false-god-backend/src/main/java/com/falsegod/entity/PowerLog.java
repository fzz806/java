package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 战力提升记录，对应 MySQL 表 t_power_log。
 *
 * <p>前端「提升记录」列表取最近若干条按时间倒序展示。
 */
@TableName("t_power_log")
public class PowerLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 t_user.id */
    private Long userId;

    /** 记录文案；text 与 MySQL 类型名同名，用反引号包裹避免歧义 */
    @TableField("`text`")
    private String text;

    /** 本次战力增量，静修为 0 */
    private Integer gain;

    /** 发生时间 */
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

    public Integer getGain() {
        return gain;
    }

    public void setGain(Integer gain) {
        this.gain = gain;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
