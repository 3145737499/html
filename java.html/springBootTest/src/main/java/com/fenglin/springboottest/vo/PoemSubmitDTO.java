package com.fenglin.springboottest.vo;

import lombok.Data;

/**
 * 留句提交体：{ content, source }
 */
@Data
public class PoemSubmitDTO {

	/** 诗句内容 */
	private String content;

	/** 出处 / 署名（可空） */
	private String source;
}
