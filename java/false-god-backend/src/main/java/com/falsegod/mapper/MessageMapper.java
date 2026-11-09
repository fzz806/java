package com.falsegod.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.falsegod.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 传音消息表 Mapper，继承 BaseMapper 即拥有基础 CRUD（MyBatis-Plus）
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
