package com.falsegod.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.falsegod.entity.UserTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTaskMapper extends BaseMapper<UserTask> {
}
