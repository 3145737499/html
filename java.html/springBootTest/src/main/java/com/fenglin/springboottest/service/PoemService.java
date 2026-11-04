package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.Poem;

import java.util.List;
import java.util.Map;

/**
 * 众声诗笺·访客留句服务
 */
public interface PoemService {

	/**
	 * 近期留句（按时间倒序分页）
	 * @return { items:[{id,content,source,createdAt}], total }
	 */
	Map<String, Object> listRecent(int page, int pageSize);

	/**
	 * 全量留句语料（最多 500 条，供前端集句生成使用）
	 * @return [{content,source}]
	 */
	List<Map<String, Object>> corpus();

	/**
	 * 提交一句诗（含校验与去重）
	 * @throws IllegalArgumentException 校验不通过时抛出，由 Controller 收敛为失败提示
	 */
	Poem submit(String content, String source, String ip);
}
