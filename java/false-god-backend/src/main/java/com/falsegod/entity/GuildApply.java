package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 入会申请，对应 MySQL 表 t_guild_apply。
 */
@TableName("t_guild_apply")
public class GuildApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请玩家 t_user.id */
    private Long userId;

    /** 申请时真名 */
    private String username;

    /** 入会理由（名帖自述） */
    private String reason;

    /** pending 待审 / approved 通过 / rejected 拒绝 */
    private String status;

    /** 会长批语 */
    private String handleNote;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }
}
