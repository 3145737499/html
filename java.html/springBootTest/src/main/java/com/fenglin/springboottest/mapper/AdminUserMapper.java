package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台管理员 Mapper（MyBatis-Plus）
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
