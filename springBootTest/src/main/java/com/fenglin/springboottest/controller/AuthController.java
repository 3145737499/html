package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口
 * 约定（与前端 src/api/auth.js 一致）：
 *   POST /api/auth/send-code   body: { account }                       -> { success, message }
 *   POST /api/auth/register    body: { account, password, code }       -> { success, token, message }
 *   POST /api/auth/login       body: { account, password }            -> { success, token, message, data:account }
 *   POST /api/auth/logout      body: { token }                        -> { success, message }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthService authService;

	@PostMapping("/send-code")
	public Result sendCode(@RequestBody Map<String, String> body) {
		return authService.sendCode(body.get("account"));
	}

	@PostMapping("/register")
	public Result register(@RequestBody Map<String, String> body) {
		return authService.register(body.get("account"), body.get("password"), body.get("code"));
	}

	@PostMapping("/login")
	public Result login(@RequestBody Map<String, String> body) {
		return authService.login(body.get("account"), body.get("password"));
	}

	@PostMapping("/logout")
	public Result logout(@RequestBody Map<String, String> body) {
		return authService.logout(body.get("token"));
	}
}
