package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 公会成员实体，对应 MySQL 表 t_member（兼作战力榜数据源）
 */
@TableName("t_member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String role;

    /** 综合战力 */
    private Long power;

    private String element;

    /** 修为等级 */
    private String realm;

    /** 一句话介绍 */
    private String bio;

    /**
     * 该行是否为「当前登录玩家」本人（不落库，仅战力榜合并时临时置 true）。
     * 前端据此高亮自己那一行，比按昵称匹配更可靠。
     */
    @TableField(exist = false)
    private Boolean self;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getPower() {
        return power;
    }

    public void setPower(Long power) {
        this.power = power;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Boolean getSelf() {
        return self;
    }

    public void setSelf(Boolean self) {
        this.self = self;
    }
}
