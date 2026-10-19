package com.fenglin.springboottest.vo;

import lombok.Data;

/**
 * 内容条目视图对象（与前端 HomeView 卡片字段一致：ic / title / desc）
 */
@Data
public class ItemVO {

	private Long id;

	/** 所属分类编码（前端按此过滤） */
	private String category;

	/** 图标 emoji */
	private String ic;

	private String title;

	/** 简介 */
	private String desc;
}
