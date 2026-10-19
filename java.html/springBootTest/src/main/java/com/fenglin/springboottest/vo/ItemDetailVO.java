package com.fenglin.springboottest.vo;

import lombok.Data;

import java.util.List;

/**
 * 条目详情视图对象（详情页使用）
 * detail 为多段详情正文，段落之间以换行符 \n 分隔
 * related 为同分类下的其他条目（卡片形态，不含自身）
 */
@Data
public class ItemDetailVO {

	private Long id;

	/** 所属分类编码 */
	private String category;

	/** 所属分类名称（面包屑展示） */
	private String categoryName;

	/** 父级分类编码（若有） */
	private String parentCategory;

	/** 父级分类名称（若有） */
	private String parentCategoryName;

	/** 图标 emoji */
	private String ic;

	private String title;

	/** 简介 */
	private String desc;

	/** 详情正文（多段，\n 分隔） */
	private String detail;

	/** 热门 0/1 */
	private Integer hot;

	/** 精选 0/1 */
	private Integer featured;

	/** 同分类其他条目（最多 4 条，卡片形态） */
	private List<ItemVO> related;
}
