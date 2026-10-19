package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

/**
 * 首页搜索 / 展示接口
 * 约定（与前端 src/api/search.js 一致）：
 *   GET /api/search?q=&category=&page=&pageSize= -> { success, message, data: { items, total } }
 *   GET /api/home                              -> { success, message, data: { categories, hotWords, featured } }
 */
public interface SearchService {

	/**
	 * 分级分类搜索
	 *
	 * @param q        关键词（匹配标题或简介，空=不限）
	 * @param category 分类编码（all 或空=全部；传父级编码时自动连带所有子分类）
	 * @param page     页码（从 1 起）
	 * @param pageSize 每页条数（1~50）
	 */
	Result search(String q, String category, Integer page, Integer pageSize);

	/**
	 * 首页展示数据：分类树（平铺）、热门搜索词、精选条目
	 */
	Result home();

	/**
	 * 条目详情（详情页）：正文 + 所属分类面包屑 + 同分类相关条目
	 *
	 * @param id 条目ID
	 */
	Result item(Long id);
}
