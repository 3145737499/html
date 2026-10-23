package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台管理员实体，对应表 admin_user
 * 与前台用户表 app_user 完全隔离：后台账号只存在于此表，前台用户无法登录后台
 */
@Data
@TableName("admin_user")
public class AdminUser {

	@TableId(type = IdType.AUTO)
	private Long id;

	/** 登录账号 */
	private String account;

	/** 密码（BCrypt 加密存储） */
	private String password;

	/** 昵称 */
	private String nickname;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
