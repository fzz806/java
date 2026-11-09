package com.falsegod.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.falsegod.entity.Friend;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友表 Mapper，继承 BaseMapper 即拥有基础 CRUD（MyBatis-Plus）
 */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
}
