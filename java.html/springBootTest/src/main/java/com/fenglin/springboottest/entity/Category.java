package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容分类实体，对应表 app_category（分级分类，parent_id 构成树）
 */
@Data
@TableName("app_category")
public class Category {

	@TableId(type = IdType.AUTO)
	private Long id;

	/** 父分类ID（0=顶级），与前端导航 key 一致 */
	private Long parentId;

	/** 分类编码（jianghu / umbrella / sect / ...） */
	private String code;

	/** 分类名称 */
	private String name;

	/** 排序（小在前） */
	private Integer sortOrder;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
