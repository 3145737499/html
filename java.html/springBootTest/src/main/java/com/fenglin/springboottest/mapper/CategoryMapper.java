package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容分类 Mapper（MyBatis-Plus）
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
