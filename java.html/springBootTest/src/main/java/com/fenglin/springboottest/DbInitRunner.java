package com.fenglin.springboottest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 启动期数据库兜底初始化。
 *
 * 原因：start-backend.bat 仅在「boonie 库不存在」时才导入 sql/boonie.sql，
 * 对于开发过程中已经存在的旧库，新表/新列不会自动创建。
 * 这里用 CREATE TABLE IF NOT EXISTS + INSERT IGNORE + 幂等 UPDATE（+ 列存在性判断的 ALTER）兜底，
 * 保证任意库状态下统计功能、「伞面丹青」精选、用户角色列与后台管理员表都能正常生效。
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

			// 用户角色列：旧库 app_user 可能缺 role 列，导致认证查询 Unknown column 'role'
			ensureUserRoleColumn(stmt);

			// 后台管理员表：AuthServiceImpl.getRole/isAdmin 依赖此表，缺失会 Table 'boonie.admin_user' doesn't exist
			stmt.execute("CREATE TABLE IF NOT EXISTS admin_user (" +
					"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
					"`account` VARCHAR(64) NOT NULL, " +
					"`password` VARCHAR(100) NOT NULL, " +
					"`nickname` VARCHAR(64) DEFAULT NULL, " +
					"`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
					"`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
					"PRIMARY KEY (`id`), " +
					"UNIQUE KEY `uk_admin_account` (`account`)" +
					") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='后台管理员表'");
			stmt.execute("INSERT IGNORE INTO admin_user (`account`, `password`, `nickname`) VALUES " +
					"('admin@boonie.com', '$2a$10$44/5E6LE8QMVfjA5plfxEOP34.sM80Vwga2SMvCYt0ucN2aUZY/Yy', '阁主')");
		}
	}

	/**
	 * 幂等地为 app_user 增加 role 列：MySQL 不支持 ADD COLUMN IF NOT EXISTS，
	 * 故先查 INFORMATION_SCHEMA 判断列是否已存在，不存在才执行 ALTER。
	 */
	private void ensureUserRoleColumn(Statement stmt) throws Exception {
		try (ResultSet rs = stmt.executeQuery(
				"SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
				"WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'role'")) {
			if (!rs.next()) {
				stmt.execute("ALTER TABLE app_user ADD COLUMN `role` " +
						"VARCHAR(16) NOT NULL DEFAULT 'user' " +
						"COMMENT '角色：user=普通用户, admin=后台管理员' AFTER `nickname`");
			}
		}
	}
}
