package com.fenglin.springboottest.service;

import java.util.Map;

/**
 * 站点统计服务：首页「数字速览」与「听雨人次」计数
 */
public interface StatsService {

	/**
	 * 概览统计：藏品数 / 分类数 / 听雨人次
	 */
	Map<String, Object> overview();

	/**
	 * 记录一次「伞下听雨」人次（游戏结算时调用）
	 */
	void recordRain();
}
