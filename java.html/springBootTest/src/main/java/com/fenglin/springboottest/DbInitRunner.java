package com.fenglin.springboottest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

	private static final Logger log = LoggerFactory.getLogger(DbInitRunner.class);

	/** 默认管理员账号 */
	private static final String ADMIN_ACCOUNT = "admin@boonie.com";
	/** 旧版本写死在 SQL 与代码里的弱口令哈希（明文 123456），命中则强制轮换 */
	private static final String KNOWN_WEAK_HASH =
			"$2a$10$44/5E6LE8QMVfjA5plfxEOP34.sM80Vwga2SMvCYt0ucN2aUZY/Yy";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Environment env;

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

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
			// 管理员账号：口令一律来自配置 boonie.admin.password，不再写死在代码/SQL 里
			seedAdmin(stmt);

			// 诗笺主题藏品（桥上看景 / 岁晚风暖）：作为真实藏品写入，首页「阁中珍藏」与详情页均可用，幂等
			stmt.execute("INSERT IGNORE INTO app_category (`id`, `parent_id`, `code`, `name`, `sort_order`, `created_at`, `updated_at`) VALUES " +
					"(19, 4, 'poem_bridge', '桥上诗', 3, NOW(), NOW()), " +
					"(20, 4, 'poem_sun', '岁晚诗', 4, NOW(), NOW()), " +
					"(21, 4, 'poem_mirror', '镜花诗', 5, NOW(), NOW())");
			stmt.execute("INSERT IGNORE INTO app_item (`id`, `category_id`, `icon`, `title`, `summary`, `detail`, `hot`, `featured`, `sort_order`, `created_at`, `updated_at`) VALUES " +
					"(23, 19, '🌉', '桥上看景', '你站在桥上看风景，看风景的人在楼上看你。', " +
					"'桥上看景，楼上看人。\\n此笺取自卞之琳《断章》——你以为在看风景，却不知自己也成了别人的风景。\\n烟雨阁藏此一联，提醒过客：江湖之大，人人皆是观者，亦皆是被观之人。', " +
					"0, 1, 1, NOW(), NOW()), " +
					"(24, 20, '☀️', '岁晚风暖', '风清日暖，正是人间岁晚时。', " +
					"'风清日暖，岁晚情长。\\n一年将尽，檐角风铃轻响，案头热茶正温。此笺写给每一个在江湖奔波后，仍愿慢下来的人。\\n岁晚不是终点，是又一程烟雨的起点。', " +
					"0, 1, 2, NOW(), NOW()), " +
					"(25, 21, '🌸', '镜花辞树', '最是人间留不住，朱颜辞镜花辞树。', " +
					"'最是人间留不住。朱颜辞镜，是少年容颜终要从镜中老去；花辞树，是枝头繁花终要离枝零落。王国维以一句道尽无常——人间最留不住的，从来是春光与少年。烟雨阁藏此一笺，愿你惜取眼前人与当下景。', " +
					"0, 1, 3, NOW(), NOW())");
		}
	}

	/**
	 * 初始化后台管理员账号。
	 *
	 * 口令来源优先级：配置项 boonie.admin.password（可用环境变量 BOONIE_ADMIN_PASSWORD 覆盖）
	 * > 随机生成。旧版本写死在 boonie.sql 与本类里的弱口令（123456）一旦被检测到，
	 * 一律强制轮换，并把新口令打印到启动日志。
	 *
	 * 注意：只在「账号不存在」或「命中已知弱口令」时写入，不会覆盖用户自己改过的密码。
	 */
	private void seedAdmin(Statement stmt) throws Exception {
		String configured = env.getProperty("boonie.admin.password");
		String current = storedHash(stmt); // null 表示账号还不存在

		// 1) 配置优先：存在则对齐（便于用环境变量改口令），不存在则创建
		if (StringUtils.hasText(configured)) {
			String raw = configured.trim();
			upsertAdmin(stmt, encoder.encode(raw));
			if (isWeak(raw)) {
				log.warn("后台管理正在使用弱口令（{}）。部署到任何非本机环境前，"
						+ "请用环境变量 BOONIE_ADMIN_PASSWORD 设置一个强口令。", raw);
			}
			return;
		}

		// 2) 未配置且账号不存在 → 随机生成，并打印到启动日志
		if (current == null) {
			String raw = randomPassword();
			upsertAdmin(stmt, encoder.encode(raw));
			printPassword("已生成后台管理员初始口令", raw);
			return;
		}

		// 3) 未配置但命中旧版写死的弱口令 → 强制轮换
		if (KNOWN_WEAK_HASH.equals(current)) {
			String raw = randomPassword();
			upsertAdmin(stmt, encoder.encode(raw));
			printPassword("检测到旧版写死的弱口令，已强制轮换", raw);
			return;
		}

		// 4) 已有自定义口令，保持不动
	}

	/** 简单弱口令识别：过短、纯数字或常见口令 */
	private boolean isWeak(String raw) {
		return raw.length() < 8 || raw.matches("\\d+")
				|| "123456".equals(raw) || "admin".equals(raw) || "password".equals(raw);
	}

	private void printPassword(String title, String raw) {
		log.warn("========== {} ==========", title);
		log.warn("  账号：{}", ADMIN_ACCOUNT);
		log.warn("  口令：{}", raw);
		log.warn("  提示：设置环境变量 BOONIE_ADMIN_PASSWORD 可指定固定口令；此口令只打印这一次。");
		log.warn("============================================================");
	}

	private void upsertAdmin(Statement stmt, String hash) throws Exception {
		try (PreparedStatement ps = conn(stmt).prepareStatement(
				"INSERT INTO admin_user (`account`, `password`, `nickname`) VALUES (?, ?, '阁主') " +
				"ON DUPLICATE KEY UPDATE `password` = VALUES(`password`)")) {
			ps.setString(1, ADMIN_ACCOUNT);
			ps.setString(2, hash);
			ps.executeUpdate();
		}
	}

	private String storedHash(Statement stmt) throws Exception {
		try (ResultSet rs = stmt.executeQuery(
				"SELECT `password` FROM admin_user WHERE `account` = '" + ADMIN_ACCOUNT + "' LIMIT 1")) {
			return rs.next() ? rs.getString(1) : null;
		}
	}

	private Connection conn(Statement stmt) throws Exception {
		return stmt.getConnection();
	}

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final String PWD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$%";

	/** 生成 12 位随机口令 */
	private String randomPassword() {
		StringBuilder sb = new StringBuilder(12);
		for (int i = 0; i < 12; i++) {
			sb.append(PWD_CHARS.charAt(SECURE_RANDOM.nextInt(PWD_CHARS.length())));
		}
		return sb.toString();
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
