package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

/**
 * 认证业务接口（验证码发送 / 注册）
 */
public interface AuthService {

	/**
	 * 发送验证码
	 * @param account 账号（邮箱或手机号）
	 */
	Result sendCode(String account);

	/**
	 * 注册账号
	 * @param account  账号
	 * @param password 明文密码（5-15 位）
	 * @param code     验证码
	 */
	Result register(String account, String password, String code);

	/**
	 * 登录：校验账号 + 密码（BCrypt），成功返回 token
	 * @param account  账号
	 * @param password 明文密码
	 */
	Result login(String account, String password);

	/**
	 * 登出：使 token 失效
	 * @param token 登录时发放的 token
	 */
	Result logout(String token);
}
