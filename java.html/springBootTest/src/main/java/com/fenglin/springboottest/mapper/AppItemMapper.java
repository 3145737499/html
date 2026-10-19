package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AppItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容条目 Mapper（MyBatis-Plus）
 */
@Mapper
public interface AppItemMapper extends BaseMapper<AppItem> {
}
