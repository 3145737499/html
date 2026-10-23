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
	 * 后台管理员登录：仅校验 admin_user 表，成功返回 token
	 * @param account  管理员账号
	 * @param password 密码
	 */
	Result adminLogin(String account, String password);

	/**
	 * 登出：使 token 失效
	 * @param token 登录时发放的 token
	 */
	Result logout(String token);

	/**
	 * 校验 token 是否有效（后台管理接口鉴权用）
	 * @param token 登录时发放的 token
	 */
	boolean isTokenValid(String token);

	/**
	 * 根据 token 取登录账号（无效返回 null）
	 * @param token 登录时发放的 token
	 */
	String getAccount(String token);

	/**
	 * 根据 token 取登录账号的角色（无效或缺失返回 null）
	 * 后台 token 返回 'admin'；前台 token 返回 app_user.role
	 * @param token 登录时发放的 token
	 */
	String getRole(String token);

	/**
	 * 是否为后台管理员（token 有效且账号角色为 admin）
	 * @param token 登录时发放的 token
	 */
	boolean isAdmin(String token);
}
