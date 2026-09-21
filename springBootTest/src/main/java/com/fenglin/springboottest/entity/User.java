package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 app_user
 */
@Data
@TableName("app_user")
public class User {

	@TableId(type = IdType.AUTO)
	private Long id;

	/** 登录账号（邮箱或手机号） */
	private String account;

	/** 密码（BCrypt 加密存储） */
	private String password;

	private String email;

	private String phone;

	private String nickname;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
