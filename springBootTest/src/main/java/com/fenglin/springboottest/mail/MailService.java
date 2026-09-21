package com.fenglin.springboottest.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务（QQ 邮箱 SMTP）
 * - 通过 JavaMailSender 向用户邮箱投递注册验证码
 * - 若未配置 QQ 邮箱 / 授权码导致发送失败，会回退把验证码打印到控制台，保证演示流程仍可走通
 */
@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.username:}")
	private String from;

	@Value("${app.mail.from-name:熊出没社区}")
	private String fromName;

	/** 发送注册验证码邮件 */
	public void sendCode(String to, String code) {
		String subject = "【熊出没社区】你的注册验证码";
		String html = buildHtml(code);
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			if (from != null && !from.isBlank()) {
				try {
					helper.setFrom(from, fromName);
				} catch (Exception e) {
					log.warn("[邮件] 设置发件人失败，使用默认发件人：{}", e.getMessage());
				}
			}
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(message);
			log.info("[邮件] 验证码已成功发送至 {} : {}", to, code);
		} catch (MailException e) {
			// 未配置授权码 / 鉴权失败等：回退到控制台，方便本地演示
			log.warn("[邮件] 发送失败（请检查 spring.mail.username / spring.mail.password 授权码配置）：{}", e.getMessage());
			log.info("[邮件-回退] 验证码 {} 请手动告知用户 {}（演示环境未真实发送）", code, to);
		} catch (Exception e) {
			log.warn("[邮件] 构造邮件异常：{}，验证码 {} 已回退打印到控制台", e.getMessage(), code);
		}
	}

	/** 构建验证码邮件 HTML 内容（熊出没主题） */
	private String buildHtml(String code) {
		return """
			<div style="max-width:480px;margin:0 auto;padding:24px;font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;background:#0f1b2d;border-radius:16px;color:#e8eef7;">
			  <div style="text-align:center;font-size:20px;font-weight:700;color:#7fd1ff;">🌲 熊出没社区</div>
			  <p style="margin-top:20px;font-size:15px;line-height:1.7;">你好！你正在注册 <b>熊出没社区</b> 账号，本次操作的验证码如下：</p>
			  <div style="margin:18px 0;padding:18px 0;text-align:center;background:linear-gradient(135deg,#16324f,#1f4e79);border-radius:12px;">
			    <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#aee1ff;">%s</span>
			  </div>
			  <p style="font-size:13px;color:#9fb0c3;line-height:1.7;">验证码 5 分钟内有效，请勿泄露给他人。若非本人操作，请忽略本邮件。</p>
			  <p style="font-size:12px;color:#6b7d92;margin-top:24px;text-align:center;">© 2026 熊出没社区 · 演示作品</p>
			</div>
			""".formatted(code);
	}
}
