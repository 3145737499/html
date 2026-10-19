package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

/**
 * 认证业务接口（图形验证码 / 注册）
 */
public interface AuthService {

	/**
	 * 生成图形验证码
	 * @return data: { key: 验证码标识, image: base64 图片 }
	 */
	Result getCaptcha();

	/**
	 * 注册账号
	 * @param account     账号
	 * @param password    明文密码（5-15 位）
	 * @param code        用户输入的图形验证码
	 * @param captchaKey  验证码标识（获取图片时下发）
	 */
	Result register(String account, String password, String code, String captchaKey);

	/**
	 * 登录：校验账号 + 密码（BCrypt），成功返回 token
	 * @param account  账号
	 * @param password 密码
	 */
	Result login(String account, String password);

	/**
	 * 登出：使 token 失效
	 * @param token 登录时发放的 token
	 */
	Result logout(String token);
}
