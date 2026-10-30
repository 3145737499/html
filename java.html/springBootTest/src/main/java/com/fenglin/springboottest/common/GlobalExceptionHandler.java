package com.fenglin.springboottest.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;

/**
 * 全局异常处理。
 *
 * 之前项目里没有任何 @ControllerAdvice，数据库异常等会直接以 Spring 默认错误体
 * （whitelabel / JSON error attributes）返回，前端拿不到约定的 {success,message}，
 * 某些情况下还会把堆栈信息回显出去。这里统一收敛成 Result 结构。
 *
 * 注意：异常详情只进日志，不回传客户端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 参数缺失 / 类型不对：多半是前端传参有误，属于可预期的 400 */
	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class,
			IllegalArgumentException.class
	})
	public Result handleBadRequest(Exception e) {
		log.warn("参数异常：{}", e.getMessage());
		return Result.fail("请求参数有误，请检查后重试");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Result handleValidation(MethodArgumentNotValidException e) {
		String msg = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(err -> err.getField() + " " + err.getDefaultMessage())
				.orElse("参数校验未通过");
		log.warn("参数校验未通过：{}", msg);
		return Result.fail(msg);
	}

	/** 数据库异常：日志里保留完整堆栈，对外只给通用提示 */
	@ExceptionHandler(SQLException.class)
	public Result handleSql(SQLException e) {
		log.error("数据库异常", e);
		return Result.fail("数据服务暂时不可用，请稍后再试");
	}

	/** 兜底：任何未预期的异常都不再回显堆栈 */
	@ExceptionHandler(Exception.class)
	public Result handleException(Exception e) {
		log.error("未处理的服务异常", e);
		return Result.fail("服务异常，请稍后再试");
	}
}
