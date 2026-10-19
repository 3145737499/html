package com.fenglin.springboottest.vo;

import lombok.Data;

/**
 * 分类视图对象（平铺返回，parentId 供前端自行组树）
 */
@Data
public class CategoryVO {

	private Long id;

	private Long parentId;

	/** 分类编码 */
	private String code;

	private String name;
}
