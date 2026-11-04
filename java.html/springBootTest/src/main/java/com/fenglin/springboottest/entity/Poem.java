package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 众声诗笺·访客留句实体，对应表 app_poem
 */
@Data
@TableName("app_poem")
public class Poem {

	@TableId(type = IdType.AUTO)
	private Long id;

	/** 诗句内容 */
	private String content;

	/** 出处 / 署名（可空，默认「佚名」） */
	private String source;

	/** 提交者 IP（限流与审计，不对外暴露） */
	private String ip;

	private LocalDateTime createdAt;
}
