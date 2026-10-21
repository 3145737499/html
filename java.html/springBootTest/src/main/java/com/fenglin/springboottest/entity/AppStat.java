package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点统计计数实体，对应表 app_stat（键值计数，如听雨人次）
 */
@Data
@TableName("app_stat")
public class AppStat {

	@TableId(type = IdType.AUTO)
	private Long id;

	/** 统计项编码（如 listen_rain） */
	private String code;

	/** 计数 */
	private Long cnt;

	private LocalDateTime updatedAt;
}
