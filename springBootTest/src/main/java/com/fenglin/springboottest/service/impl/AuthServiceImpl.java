package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证业务实现
 * - 验证码：内存缓存（账号 -> 验证码 + 过期时间），5 分钟有效，演示环境仅打印到控制台，未真实发送邮件/短信
 * - 注册：校验验证码 + 密码长度(5-15) + 账号唯一，密码使用 BCrypt 加密后入库
 */
@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private MailService mailService;

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	/** 验证码缓存：account -> (code, 过期时间戳) */
	private final Map<String, CodeEntry> codeCache = new ConcurrentHashMap<>();
	private static final long CODE_TTL = 5 * 60 * 1000L;
	private final Random random = new Random();

	/** 登录 token 缓存：token -> account（演示用，重启即失效） */
	private final Map<String, String> tokenMap = new ConcurrentHashMap<>();

	@Override
	public Result sendCode(String account) {
		if (account == null || account.isBlank()) {
			return Result.fail("请输入账号");
		}
		String code = String.format("%06d", random.nextInt(1_000_000));
		codeCache.put(account, new CodeEntry(code, System.currentTimeMillis() + CODE_TTL));
		// 真实发送：QQ 邮箱 SMTP 投递（未配置授权码时自动回退到控制台）
		mailService.sendCode(account, code);
		boolean isEmail = account.contains("@");
		return Result.ok(isEmail ? "验证码已发送至邮箱 " + account : "验证码已发送");
	}

	@Override
	public Result register(String account, String password, String code) {
		if (account == null || account.isBlank()) {
			return Result.fail("请输入账号");
		}
		if (password == null || password.length() < 5 || password.length() > 15) {
			return Result.fail("密码长度需为 5-15 位");
		}
		CodeEntry entry = codeCache.get(account);
		if (entry == null || entry.expireAt < System.currentTimeMillis() || !entry.code.equals(code)) {
			return Result.fail("验证码错误或已失效");
		}
		Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getAccount, account));
		if (count != null && count > 0) {
			return Result.fail("该账号已注册");
		}

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

		codeCache.remove(account);
		String token = "token-" + UUID.randomUUID();
		tokenMap.put(token, account);
		return Result.ok(token, "注册成功");
	}

	@Override
	public Result login(String account, String password) {
		if (account == null || account.isBlank() || password == null || password.isBlank()) {
			return Result.fail("请输入账号和密码");
		}
		User user = userMapper.selectOne(
				new LambdaQueryWrapper<User>().eq(User::getAccount, account));
		if (user == null) {
			return Result.fail("账号不存在");
		}
		if (!encoder.matches(password, user.getPassword())) {
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
	public Result logout(String token) {
		if (token != null) {
			tokenMap.remove(token);
		}
		return Result.ok("已退出登录");
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
