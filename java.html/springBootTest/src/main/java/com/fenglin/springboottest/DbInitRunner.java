package com.fenglin.springboottest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动期数据库兜底初始化。
 *
 * 原因：start-backend.bat 仅在「boonie 库不存在」时才导入 sql/boonie.sql，
 * 对于开发过程中已经存在的旧库，新表（app_stat）不会自动创建。
 * 这里用 CREATE TABLE IF NOT EXISTS + INSERT IGNORE + 幂等 UPDATE 兜底，
 * 保证任意库状态下统计功能与「伞面丹青」精选都能正常生效。
 */
@Component
public class DbInitRunner implements CommandLineRunner {

	@Autowired
	private DataSource dataSource;

	@Override
	public void run(String... args) throws Exception {
		try (Connection conn = dataSource.getConnection();
			 Statement stmt = conn.createStatement()) {

			// 统计计数表（听雨人次等）
			stmt.execute("CREATE TABLE IF NOT EXISTS app_stat (" +
					"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
					"`code` VARCHAR(32) NOT NULL COMMENT '统计项编码', " +
					"`cnt` BIGINT NOT NULL DEFAULT 0 COMMENT '计数', " +
					"`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
					"PRIMARY KEY (`id`), " +
					"UNIQUE KEY `uk_code` (`code`)" +
					") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站点统计计数'");
			stmt.execute("INSERT IGNORE INTO app_stat (`code`, `cnt`) VALUES ('listen_rain', 0)");

			// 让「伞面丹青」也进入首页精选，阁中珍藏卷轴更丰满（幂等）
			stmt.execute("UPDATE app_item SET featured = 1 WHERE title = '伞面丹青' AND featured = 0");
		}
	}
}
