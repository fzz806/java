package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 恶鬼之牙仓库物品，对应 MySQL 表 t_warehouse。
 *
 * <p>前端字段为 { name, count, rarity }。
 */
@TableName("t_warehouse")
public class WarehouseItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer count;

    /** 稀有度：普通 / 稀有 / 史诗 */
    private String rarity;

    /** 排序，小在前 */
    private Integer sortNo;

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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
