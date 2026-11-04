package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Poem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 众声诗笺·访客留句 Mapper（MyBatis-Plus）
 */
@Mapper
public interface PoemMapper extends BaseMapper<Poem> {
}
