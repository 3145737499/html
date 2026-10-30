package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.AdminUser;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.AdminUserMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证业务实现
 * - 图形验证码：AWT 绘制水墨风图片（宣纸底 + 噪点 + 干扰弧线 + 旋转字符），
 *   内存缓存（key -> 验证码 + 过期时间），5 分钟有效，一次性使用
 * - 注册：校验图形验证码 + 密码长度(5-15) + 账号唯一，密码使用 BCrypt 加密后入库
 */
@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private AdminUserMapper adminUserMapper;

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	/** 图形验证码缓存：key -> (code, 过期时间戳) */
	private final Map<String, CodeEntry> captchaCache = new ConcurrentHashMap<>();
	private static final long CAPTCHA_TTL = 5 * 60 * 1000L;
	private final Random random = new Random();

	/** 登录 token 缓存：token -> account（演示用，重启即失效） */
	private final Map<String, String> tokenMap = new ConcurrentHashMap<>();

	/** 验证码字符集：去掉易混淆的 I / O / 0 / 1 / l */
	private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int CAPTCHA_LENGTH = 4;

	/** 水墨配色：墨 / 青绿 / 朱砂 / 鎏金 */
	private static final Color[] INK_COLORS = {
			new Color(30, 43, 40),
			new Color(78, 141, 134),
			new Color(179, 54, 43),
			new Color(163, 127, 61),
	};

	@Override
	public Result getCaptcha() {
		// 顺带清理过期条目，避免内存泄漏
		long now = System.currentTimeMillis();
		captchaCache.entrySet().removeIf(e -> e.getValue().expireAt < now);

		String key = UUID.randomUUID().toString();
		StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
		for (int i = 0; i < CAPTCHA_LENGTH; i++) {
			sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
		}
		String code = sb.toString();
		captchaCache.put(key, new CodeEntry(code, now + CAPTCHA_TTL));

		String image;
		try {
			image = drawCaptcha(code);
		} catch (Exception e) {
			log.error("图形验证码绘制失败", e);
			return Result.fail("验证码生成失败，请重试");
		}

		Map<String, String> data = new HashMap<>();
		data.put("key", key);
		data.put("image", image);
		Result r = Result.ok("验证码已生成");
		r.setData(data);
		return r;
	}

	@Override
	public Result register(String account, String password, String code, String captchaKey) {
		// 1. 校验图形验证码（一次性，验证后即作废）
		if (captchaKey == null || code == null || code.isBlank()) {
			return Result.fail("请输入图形验证码");
		}
		CodeEntry entry = captchaCache.get(captchaKey);
		boolean pass = entry != null
				&& entry.expireAt >= System.currentTimeMillis()
				&& entry.code.equalsIgnoreCase(code.trim());
		captchaCache.remove(captchaKey);
		if (!pass) {
			return Result.fail("验证码错误或已失效，请刷新后重试");
		}

		// 2. 账号 / 密码校验
		if (account == null || account.isBlank()) {
			return Result.fail("请输入账号");
		}
		if (password == null || password.length() < 5 || password.length() > 15) {
			return Result.fail("密码长度需为 5-15 位");
		}
		Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getAccount, account));
		if (count != null && count > 0) {
			return Result.fail("该账号已注册");
		}

		// 3. 入库
		User user = new User();
		user.setAccount(account);
		user.setPassword(encoder.encode(password));
		if (account.contains("@")) {
			user.setEmail(account);
		} else {
			user.setPhone(account);
		}
		user.setNickname(account);
		userMapper.insert(user);

		String token = "token-" + UUID.randomUUID();
		tokenMap.put(token, account);
		return Result.ok(token, "注册成功");
	}

	@Override
	public Result login(String account, String password) {
		if (account == null || account.isBlank() || password == null || password.isBlank()) {
			return Result.fail("请输入账号和密码");
		}
		// 前台用户：app_user
		User user = userMapper.selectOne(
				new LambdaQueryWrapper<User>().eq(User::getAccount, account));
		if (user != null) {
			if (!encoder.matches(password, user.getPassword())) {
				// 不区分「密码错误 / 账号不存在」，避免被用来枚举已注册账号
				return Result.fail("账号或密码错误");
			}
			return issueToken(account);
		}
		// 后台管理员：admin_user（与前台账号完全隔离）
		AdminUser admin = adminUserMapper.selectOne(
				new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getAccount, account));
		if (admin != null) {
			if (!encoder.matches(password, admin.getPassword())) {
				// 同上：管理员账号的存在性也不应通过文案泄露
				return Result.fail("账号或密码错误");
			}
			return issueToken(account);
		}
		return Result.fail("账号或密码错误");
	}

	/** 签发登录 token 并回带账号，供前端持久化登录态 */
	private Result issueToken(String account) {
		String token = "token-" + UUID.randomUUID();
		tokenMap.put(token, account);
		Result r = Result.ok(token, "登录成功");
		r.setData(account);
		return r;
	}

	@Override
	public Result logout(String token) {
		if (token != null) {
			tokenMap.remove(token);
		}
		return Result.ok("已退出登录");
	}

	@Override
	public Result adminLogin(String account, String password) {
		if (account == null || account.isBlank() || password == null || password.isBlank()) {
			return Result.fail("请输入账号和密码");
		}
		AdminUser admin = adminUserMapper.selectOne(
				new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getAccount, account));
		if (admin == null) {
			return Result.fail("账号不存在或无后台权限");
		}
		if (!encoder.matches(password, admin.getPassword())) {
			return Result.fail("密码错误");
		}
		String token = "token-" + UUID.randomUUID();
		tokenMap.put(token, account);
		// data 带回账号，方便前端持久化登录态
		Result r = Result.ok(token, "登录成功");
		r.setData(account);
		return r;
	}

	@Override
	public boolean isTokenValid(String token) {
		return token != null && tokenMap.containsKey(token);
	}

	@Override
	public String getAccount(String token) {
		return token == null ? null : tokenMap.get(token);
	}

	@Override
	public String getRole(String token) {
		String account = getAccount(token);
		if (account == null) return null;
		// 后台 token：账号存在于 admin_user 表
		if (adminUserMapper.selectCount(
				new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getAccount, account)) > 0) {
			return "admin";
		}
		// 前台 token：取 app_user 的角色
		User u = userMapper.selectOne(
				new LambdaQueryWrapper<User>().eq(User::getAccount, account));
		return u == null ? null : u.getRole();
	}

	@Override
	public boolean isAdmin(String token) {
		// 与管理员判定保持一致：账号存在于 admin_user 表即视为后台管理员
		return "admin".equals(getRole(token));
	}

	/**
	 * 绘制水墨风图形验证码
	 * 宣纸底色 + 噪点 + 干扰弧线 + 楷体旋转字符，输出 data:image/png;base64
	 */
	private String drawCaptcha(String code) throws Exception {
		int width = 132;
		int height = 46;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 宣纸底
		g.setColor(new Color(244, 241, 232));
		g.fillRect(0, 0, width, height);

		// 噪点（淡墨）
		for (int i = 0; i < 90; i++) {
			g.setColor(new Color(61 + random.nextInt(50), 80 + random.nextInt(50), 75 + random.nextInt(50)));
			g.fillRect(random.nextInt(width), random.nextInt(height), 2, 2);
		}

		// 干扰弧线（水墨笔意）
		for (int i = 0; i < 3; i++) {
			g.setColor(INK_COLORS[random.nextInt(INK_COLORS.length)]);
			g.setStroke(new BasicStroke(1.4f));
			int x = random.nextInt(30);
			int y = random.nextInt(16);
			int w = 60 + random.nextInt(60);
			int h = 20 + random.nextInt(20);
			g.drawArc(x, y, w, h, random.nextInt(60), 120 + random.nextInt(100));
		}

		// 逐字符绘制（楷体，随机旋转 + 水墨配色）
		Font font = new Font("KaiTi", Font.BOLD, 28);
		if (!font.getFamily().equals("KaiTi")) {
			font = new Font("Serif", Font.BOLD, 28);
		}
		g.setFont(font);
		int x = 12;
		for (int i = 0; i < code.length(); i++) {
			g.setColor(INK_COLORS[random.nextInt(INK_COLORS.length)]);
			double theta = (random.nextInt(50) - 25) * Math.PI / 180;
			int charX = x + i * 27;
			int charY = 32 + random.nextInt(5);
			g.rotate(theta, charX + 8, charY - 12);
			g.drawString(String.valueOf(code.charAt(i)), charX, charY);
			g.rotate(-theta, charX + 8, charY - 12);
		}

		g.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(img, "png", baos);
		return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	/** 验证码缓存条目 */
	private static class CodeEntry {
		final String code;
		final long expireAt;

		CodeEntry(String code, long expireAt) {
			this.code = code;
			this.expireAt = expireAt;
		}
	}
}
