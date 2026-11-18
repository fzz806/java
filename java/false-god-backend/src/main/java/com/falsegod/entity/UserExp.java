package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户累计经验，对应 MySQL 表 t_user_exp。
 *
 * <p>只存 {@code total_exp} 一个数字，阶位（等级）由
 * {@code TaskService#levelOf} 实时推导。
 *
 * <p>阶位与大境界是同一条阶梯（{@code RealmLadder}），两边靠双向同步保持一致：
 * 经验跨阶会把 {@code t_power.realm_index} 一起推上去，修行突破则把
 * {@code total_exp} 补到该阶起点。所以这里的数字是「功业经验 + 境界折算」，
 * 不再是与战力图无关的独立进度。
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
