package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户累计经验，对应 MySQL 表 t_user_exp。
 *
 * <p>只存 {@code total_exp} 一个数字，阶位（等级）由
 * {@code TaskService#levelOf} 实时推导，与战力同一思路：
 * 源头只有一个，展示层怎么算都不会互相矛盾。
 */
@TableName("t_user_exp")
public class UserExp {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** t_user.id */
    private Long userId;

    /** 累计经验，只增不减 */
    private Integer totalExp;

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

    public Integer getTotalExp() {
        return totalExp;
    }

    public void setTotalExp(Integer totalExp) {
        this.totalExp = totalExp;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
