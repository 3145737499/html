package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Poem;
import com.fenglin.springboottest.service.PoemService;
import com.fenglin.springboottest.vo.PoemSubmitDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 众声诗笺·访客留句接口
 *   GET  /api/poems           -> { success, data: { items:[{id,content,source,createdAt}], total } }
 *   GET  /api/poems/corpus    -> { success, data: { items:[{content,source}] } }（集句生成语料）
 *   POST /api/poems           -> 提交一句诗 { content, source }
 *
 * POST 是匿名可写接口，沿用 StatsController 的按 IP 固定窗口限流，挡住随手刷。
 */
@RestController
@RequestMapping("/api")
public class PoemController {

	private static final Logger log = LoggerFactory.getLogger(PoemController.class);
	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private static final long WINDOW_MS = 60_000L;
	private static final int MAX_PER_WINDOW = 20;
	private static final Map<String, long[]> HITS = new ConcurrentHashMap<>();
	private static long windowStart = System.currentTimeMillis();

	@Autowired
	private PoemService poemService;

	@GetMapping("/poems")
	public Result list(@RequestParam(defaultValue = "1") int page,
					   @RequestParam(defaultValue = "12") int pageSize) {
		Result r = Result.ok("ok");
		r.setData(poemService.listRecent(page, pageSize));
		return r;
	}

	@GetMapping("/poems/corpus")
	public Result corpus() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("items", poemService.corpus());
		Result r = Result.ok("ok");
		r.setData(data);
		return r;
	}

	@PostMapping("/poems")
	public Result submit(@RequestBody PoemSubmitDTO body, HttpServletRequest request) {
		if (!allow(clientIp(request))) {
			return Result.fail("留句太频繁了，稍后再来");
		}
		try {
			Poem p = poemService.submit(
					body == null ? null : body.getContent(),
					body == null ? null : body.getSource(),
					clientIp(request));
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("id", p.getId());
			data.put("content", p.getContent());
			data.put("source", p.getSource());
			data.put("createdAt", p.getCreatedAt() == null ? "" : p.getCreatedAt().format(FORMATTER));
			Result r = Result.ok("已留于烟雨簿");
			r.setData(data);
			return r;
		} catch (IllegalArgumentException e) {
			return Result.fail(e.getMessage());
		} catch (Exception e) {
			log.error("留句失败", e);
			return Result.fail("留句未达，请稍后再试");
		}
	}

	/** 固定窗口限流：窗口过期整体清空，避免 Map 长期膨胀 */
	private synchronized boolean allow(String ip) {
		long now = System.currentTimeMillis();
		if (now - windowStart > WINDOW_MS) {
			HITS.clear();
			windowStart = now;
		}
		long[] slot = HITS.computeIfAbsent(ip, k -> new long[1]);
		if (slot[0] >= MAX_PER_WINDOW) {
			log.warn("留句被限流：ip={}，窗口内已达 {} 次", ip, slot[0]);
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
