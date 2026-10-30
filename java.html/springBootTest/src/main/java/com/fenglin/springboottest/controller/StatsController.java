package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 站点统计接口
 *   GET  /api/stats        -> { success, message, data: { itemCount, categoryCount, listenRain } }
 *   POST /api/stats/rain   -> 记录一次「伞下听雨」人次（游戏结算时调用）
 *
 * /api/stats/rain 是匿名可写的写接口，之前没有任何限制，脚本一循环就能把计数刷到任意值。
 * 这里加一层按 IP 的简单滑动窗口限流（演示级，够挡住随手刷；集群部署请换成 Redis）。
 */
@RestController
@RequestMapping("/api")
public class StatsController {

	private static final Logger log = LoggerFactory.getLogger(StatsController.class);

	/** 时间窗口长度 */
	private static final long WINDOW_MS = 60_000L;
	/** 单窗口内每个 IP 允许的次数 */
	private static final int MAX_PER_WINDOW = 10;

	/** IP -> 窗口内计数（windowStart, count） */
	private static final Map<String, long[]> HITS = new ConcurrentHashMap<>();
	private static long windowStart = System.currentTimeMillis();

	@Autowired
	private StatsService statsService;

	@GetMapping("/stats")
	public Result stats() {
		Result r = Result.ok("ok");
		r.setData(statsService.overview());
		return r;
	}

	@PostMapping("/stats/rain")
	public Result rain(HttpServletRequest request) {
		if (!allow(clientIp(request))) {
			return Result.fail("记录太频繁了，稍后再来");
		}
		statsService.recordRain();
		return Result.ok("听雨已记入烟雨簿");
	}

	/**
	 * 固定窗口限流：窗口过期就整体清空，避免长期运行后 Map 无限膨胀。
	 */
	private synchronized boolean allow(String ip) {
		long now = System.currentTimeMillis();
		if (now - windowStart > WINDOW_MS) {
			HITS.clear();
			windowStart = now;
		}
		long[] slot = HITS.computeIfAbsent(ip, k -> new long[1]);
		if (slot[0] >= MAX_PER_WINDOW) {
			log.warn("听雨计数被限流：ip={}，窗口内已达 {} 次", ip, slot[0]);
			return false;
		}
		slot[0]++;
		return true;
	}

	/** 取客户端 IP（本机直连时为 127.0.0.1） */
	private String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			return xff.split(",")[0].trim();
		}
		String ip = request.getRemoteAddr();
		return ip == null ? "unknown" : ip;
	}
}
