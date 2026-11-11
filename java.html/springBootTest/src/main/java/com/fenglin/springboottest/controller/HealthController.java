package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口：供前端在登录/注册页启动时探测后端是否可达，
 * 不可达时给出明确提示（而不是笼统的「服务异常」）。
 */
@RestController
public class HealthController {

	@GetMapping("/api/health")
	public Result health() {
		return Result.ok("ok");
	}
}
