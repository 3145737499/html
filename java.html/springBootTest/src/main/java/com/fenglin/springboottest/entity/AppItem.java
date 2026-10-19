package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容条目实体，对应表 app_item
 */
@Data
@TableName("app_item")
public class AppItem {

	@TableId(type = IdType.AUTO)
	private Long id;

	/** 所属分类ID（app_category.id，可挂任意层级） */
	private Long categoryId;

	/** 图标（emoji） */
	private String icon;

	/** 标题 */
	private String title;

	/** 简介（前端展示用 desc） */
	private String summary;

	/** 详情（预留） */
	private String detail;

	/** 热门搜索词 0/1 */
	private Integer hot;

	/** 首页精选 0/1 */
	private Integer featured;

	/** 排序（小在前） */
	private Integer sortOrder;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
