package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口
 * 约定（与前端 src/api/auth.js 一致）：
 *   GET  /api/auth/captcha   -> { success, message, data: { key, image } }
 *   POST /api/auth/register  body: { account, password, code, captchaKey } -> { success, token, message }
 *   POST /api/auth/login     body: { account, password }            -> { success, token, message, data:account }
 *   POST /api/auth/logout    body: { token }                        -> { success, message }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthService authService;

	@GetMapping("/captcha")
	public Result captcha() {
		return authService.getCaptcha();
	}

	@PostMapping("/register")
	public Result register(@RequestBody Map<String, String> body) {
		return authService.register(
				body.get("account"),
				body.get("password"),
				body.get("code"),
				body.get("captchaKey"));
	}

	@PostMapping("/login")
	public Result login(@RequestBody Map<String, String> body) {
		return authService.login(body.get("account"), body.get("password"));
	}

	@PostMapping("/logout")
	public Result logout(@RequestBody Map<String, String> body) {
		return authService.logout(body.get("token"));
	}

	/**
	 * 当前登录用户自身信息（供前端判断是否展示「后台管理」入口）
	 * 需携带 Authorization: Bearer <token>，无效返回 401
	 */
	@GetMapping("/me")
	public Result me(@RequestHeader(value = "Authorization", required = false) String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return Result.fail("未登录或登录已失效，请重新登录");
		}
		String token = authorization.substring(7).trim();
		if (!authService.isTokenValid(token)) {
			return Result.fail("未登录或登录已失效，请重新登录");
		}
		Map<String, String> data = new HashMap<>();
		data.put("account", authService.getAccount(token));
		data.put("role", authService.getRole(token));
		Result r = Result.ok("ok");
		r.setData(data);
		return r;
	}
}
