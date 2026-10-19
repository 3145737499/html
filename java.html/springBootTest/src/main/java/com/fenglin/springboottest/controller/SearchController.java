package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页搜索 / 展示接口
 * 约定（与前端 src/api/search.js 一致）：
 *   GET /api/search?q={关键词}&category={分类编码}&page={页码}&pageSize={每页}
 *     -> { success, message, data: { items: [{id, category, ic, title, desc}], total } }
 *     分级分类：category 传父级编码（如 jianghu）时自动连带全部子分类条目
 *   GET /api/home
 *     -> { success, message, data: { categories, hotWords, featured } }
 */
@RestController
@RequestMapping("/api")
public class SearchController {

	@Autowired
	private SearchService searchService;

	@GetMapping("/search")
	public Result search(
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "all") String category,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "20") Integer pageSize) {
		return searchService.search(q, category, page, pageSize);
	}

	@GetMapping("/home")
	public Result home() {
		return searchService.home();
	}

	/**
	 * 条目详情（详情页）：正文 + 面包屑 + 同分类相关条目
	 */
	@GetMapping("/item/{id}")
	public Result item(@PathVariable Long id) {
		return searchService.item(id);
	}
}
