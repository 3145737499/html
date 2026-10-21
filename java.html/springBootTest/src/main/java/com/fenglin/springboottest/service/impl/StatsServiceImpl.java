package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.AppItem;
import com.fenglin.springboottest.mapper.AppItemMapper;
import com.fenglin.springboottest.mapper.AppStatMapper;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 站点统计实现
 *   藏品数   = app_item 总条数
 *   分类数   = app_category 中 parent_id=0 的顶级分类数（与首页导航一致）
 *   听雨人次 = app_stat 中 listen_rain 计数（由游戏结算时累加）
 */
@Service
public class StatsServiceImpl implements StatsService {

	@Autowired
	private AppItemMapper itemMapper;

	@Autowired
	private CategoryMapper categoryMapper;

	@Autowired
	private AppStatMapper statMapper;

	@Override
	public Map<String, Object> overview() {
		long itemCount = itemMapper.selectCount(null);
		long categoryCount = categoryMapper.selectCount(
				new LambdaQueryWrapper<Category>().eq(Category::getParentId, 0L));
		Long rain = statMapper.selectCnt("listen_rain");
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("itemCount", itemCount);
		m.put("categoryCount", categoryCount);
		m.put("listenRain", rain == null ? 0L : rain);
		return m;
	}

	@Override
	public void recordRain() {
		statMapper.incrementOrCreate("listen_rain");
	}
}
