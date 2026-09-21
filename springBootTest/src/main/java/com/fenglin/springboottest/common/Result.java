package com.fenglin.springboottest.common;

import lombok.Data;

/**
 * 统一接口返回结构
 * 前端 auth.js 依赖 success / message / token 三个字段
 */
@Data
public class Result {

	private boolean success;
	private String message;
	private String token;
	private Object data;

	public static Result ok(String message) {
		Result r = new Result();
		r.success = true;
		r.message = message;
		return r;
	}

	public static Result ok(String token, String message) {
		Result r = new Result();
		r.success = true;
		r.token = token;
		r.message = message;
		return r;
	}

	public static Result fail(String message) {
		Result r = new Result();
		r.success = false;
		r.message = message;
		return r;
	}
}
