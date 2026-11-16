package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 任务定义，对应 MySQL 表 t_task。
 *
 * <p>{@code action} 是任务与真实操作的连接点：
 * 战力模块每次提升成功后调用 {@code TaskService#progress}，
 * 这里按 action 匹配推进对应任务，不做「点按钮就算完成」的假交互。
 *
 * <p>{@code enabled = 0} 的任务不下发到前端，但历史进度与领取记录保留。
 */
@TableName("t_task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编码，业务唯一（daily_login / main_realm …） */
    private String code;

    /** 任务名 */
    private String title;

    /** 任务描述 */
    private String summary;

    /** daily 日课 / main 主线 */
    private String category;

    /** 触发动作：login / rest / seal / realm / skill / essence / upgrade */
    private String action;

    /** 完成所需次数 */
    private Integer target;

    /** 完成后可得经验 */
    private Integer exp;

    /** 排序，小的在前 */
    private Integer sort;

    /** 1 启用 / 0 下线 */
    private Integer enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
