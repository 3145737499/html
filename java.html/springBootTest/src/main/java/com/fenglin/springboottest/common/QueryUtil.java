package com.fenglin.springboottest.common;

/**
 * 查询参数安全化工具。
 *
 * 两件事：
 * 1. LIKE 通配符转义 —— 用户输入的 % 和 _ 原样拼进 LIKE 会被当成通配符，
 *    一串 "%%%%..." 就能让索引失效、拖垮整表扫描。
 * 2. 分页偏移防溢出 —— (page - 1) * size 用 int 计算，page 取到 Integer.MAX_VALUE
 *    会溢出成负数，拼出 "LIMIT -100, 20" 直接触发 MySQL 语法错误。
 */
public final class QueryUtil {

	/** 关键词最大长度，超长直接截断，避免构造超复杂的 LIKE */
	public static final int MAX_KEYWORD = 100;

	private QueryUtil() {
	}

	/**
	 * 转义 LIKE 通配符（MySQL 默认以反斜杠为转义符）。
	 * 转义顺序不能变：先处理反斜杠本身，否则会把后面加的转义符又反转义掉。
	 */
	public static String escapeLike(String raw) {
		if (raw == null) return "";
		return raw.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}

	/** 归一化关键词：去空白 + 截断长度 */
	public static String keyword(String raw) {
		if (raw == null) return "";
		String s = raw.trim();
		return s.length() > MAX_KEYWORD ? s.substring(0, MAX_KEYWORD) : s;
	}

	/**
	 * 计算安全的分页偏移量。用 long 参与运算再夹到 int 范围，
	 * 保证拼进 LIMIT 的一定是非负整数。
	 */
	public static int offset(Integer page, int size) {
		int p = page == null || page < 1 ? 1 : page;
		int s = size < 1 ? 1 : size;
		long off = (long) (p - 1) * s;
		if (off > Integer.MAX_VALUE) return Integer.MAX_VALUE;
		return (int) Math.max(off, 0L);
	}

	/** 归一化每页条数 */
	public static int pageSize(Integer pageSize, int def, int max) {
		if (pageSize == null || pageSize < 1) return def;
		return Math.min(pageSize, max);
	}
}
