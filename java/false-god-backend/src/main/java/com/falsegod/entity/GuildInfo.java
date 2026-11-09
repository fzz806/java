package com.falsegod.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 公会简报键值实体，对应 MySQL 表 t_guild_info（无自增主键，k 为主键）
 */
@TableName("t_guild_info")
public class GuildInfo {

    @TableId
    private String k;

    private String v;

    public String getK() {
        return k;
    }

    public void setK(String k) {
        this.k = k;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }
}
