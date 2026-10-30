package com.fenglin.springboottest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置：允许前端（Vite 开发服 / 预览服）调用本后端接口。
 *
 * 旧配置是 allowedOriginPatterns("*") + allowCredentials(true)，
 * 意味着任意站点都能带着用户凭据跨域调用本后端的写接口（后台增删改也在内）。
 * 这里改为显式白名单，默认放行本机 Vite 的两个端口，可用
 * 环境变量 BOONIE_CORS_ORIGINS（逗号分隔）覆盖，例如部署到域名时：
 *   BOONIE_CORS_ORIGINS=https://your-site.com
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	/** 允许跨域的来源，逗号分隔；默认本机 Vite 开发服与预览服 */
	@Value("${boonie.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
	private String[] allowedOrigins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}
