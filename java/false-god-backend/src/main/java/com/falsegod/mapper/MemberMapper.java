package com.falsegod.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.falsegod.entity.Member;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
