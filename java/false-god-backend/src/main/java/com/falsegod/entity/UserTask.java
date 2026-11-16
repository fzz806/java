package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户任务进度，对应 MySQL 表 t_user_task。
 *
 * <p>{@code task_date} 决定重置节奏：
 * <ul>
 *   <li>日课 — 当天日期，跨天自动出现新行，等于免费重置</li>
 *   <li>主线 — 固定 {@code 1970-01-01}，一人一条，永不重置</li>
 * </ul>
 *
 * <p>{@code (user_id, task_id, task_date)} 上有唯一索引，
 * 并发重复插行会被数据库挡掉，不会出现两条进度各记一半。
 */
@TableName("t_user_task")
public class UserTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** t_user.id */
    private Long userId;

    /** t_task.id */
    private Long taskId;

    /** 日课=当天；主线=1970-01-01 */
    private LocalDate taskDate;

    /** 已完成次数，封顶到 target */
    private Integer progress;

    /** 领取时间，null 表示未领取 */
    private LocalDateTime claimedAt;

    private LocalDateTime createdAt;

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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
