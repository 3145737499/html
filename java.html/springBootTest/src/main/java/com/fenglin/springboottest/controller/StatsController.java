package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点统计接口
 *   GET  /api/stats        -> { success, message, data: { itemCount, categoryCount, listenRain } }
 *   POST /api/stats/rain   -> 记录一次「伞下听雨」人次（游戏结算时调用）
 */
@RestController
@RequestMapping("/api")
public class StatsController {

	@Autowired
	private StatsService statsService;

	@GetMapping("/stats")
	public Result stats() {
		Result r = Result.ok("ok");
		r.setData(statsService.overview());
		return r;
	}

	@PostMapping("/stats/rain")
	public Result rain() {
		statsService.recordRain();
		return Result.ok("听雨已记入烟雨簿");
	}
}
