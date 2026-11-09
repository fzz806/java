package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 神技表实体，对应 MySQL 表 t_skill
 */
@TableName("t_skill")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String level;

    private String element;

    /** 所属形态（男主专属，可为空） */
    private String transform;

    /** 熟练度 0-100 */
    private Integer mastery;

    /** 是否禁忌 1/0 */
    private Integer forbidden;

    /** 神技描述（desc 与 MySQL 关键字一致，这里无需转义，MyBatis-Plus 直接生成 `desc` 反引号列名） */
    @TableField("`desc`")
    private String desc;

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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public Integer getMastery() {
        return mastery;
    }

    public void setMastery(Integer mastery) {
        this.mastery = mastery;
    }

    public Integer getForbidden() {
        return forbidden;
    }

    public void setForbidden(Integer forbidden) {
        this.forbidden = forbidden;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
