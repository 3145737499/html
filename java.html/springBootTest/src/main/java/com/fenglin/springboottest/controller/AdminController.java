package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.QueryUtil;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.AppItem;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.mapper.AppItemMapper;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理接口（对首页「江湖拾遗 / 藏品」app_item 与分类 app_category 做增删改查）
 * 鉴权：所有接口均需在请求头携带 {@code Authorization: Bearer <token>}，
 * token 由 /api/auth/login 签发；无效或缺失返回 { success:false, message:"未登录或登录已失效…" }
 *
 * 约定（与前端 src/api/admin.js 一致）：
 *   GET    /api/admin/items?page=&pageSize=&keyword= -> { success, data:{ items, total, page, pageSize } }
 *   GET    /api/admin/items/{id}                     -> { success, data: AppItem }
 *   POST   /api/admin/items                          -> { success, data: AppItem(含新 id) }
 *   PUT    /api/admin/items/{id}                     -> { success, data: AppItem }
 *   DELETE /api/admin/items/{id}                     -> { success, message }
 *   GET    /api/admin/categories                     -> { success, data: [Category] }
 *   POST   /api/admin/categories                     -> { success, data: Category }
 *   PUT    /api/admin/categories/{id}                -> { success, data: Category }
 *   DELETE /api/admin/categories/{id}                -> { success, message }
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	@Autowired
	private AppItemMapper itemMapper;

	@Autowired
	private CategoryMapper categoryMapper;

	@Autowired
	private AuthService authService;

	/* ---------------- 鉴权辅助 ---------------- */

	private static final int AUTH_OK = 0;
	private static final int AUTH_NOLOGIN = 1;
	private static final int AUTH_FORBIDDEN = 2;

	/**
	 * 鉴权状态：从 Authorization: Bearer <token> 解析并校验
	 *   - token 无效 / 缺失           -> AUTH_NOLOGIN
	 *   - token 有效但非后台管理员角色  -> AUTH_FORBIDDEN
	 *   - 通过                          -> AUTH_OK
	 */
	private int authState(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return AUTH_NOLOGIN;
		}
		String token = authorization.substring(7).trim();
		if (!authService.isTokenValid(token)) {
			return AUTH_NOLOGIN;
		}
		if (!authService.isAdmin(token)) {
			return AUTH_FORBIDDEN;
		}
		return AUTH_OK;
	}

	private Result denyLogin() {
		return Result.fail("未登录或登录已失效，请重新登录");
	}

	private Result denyForbidden() {
		return Result.fail("该账号无后台管理权限");
	}

	/** 当前登录管理员信息（供前端展示 / 登录后权限探测） */
	@GetMapping("/me")
	public Result me(@RequestHeader(value = "Authorization", required = false) String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return denyLogin();
		}
		String token = authorization.substring(7).trim();
		if (!authService.isTokenValid(token)) {
			return denyLogin();
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("account", authService.getAccount(token));
		data.put("role", authService.getRole(token));
		data.put("isAdmin", authService.isAdmin(token));
		return okData(data);
	}

	private Result okData(Object data) {
		Result r = Result.ok("ok");
		r.setData(data);
		return r;
	}

	/* ================= 内容条目（app_item） ================= */

	@GetMapping("/items")
	public Result listItems(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "10") Integer pageSize,
			@RequestParam(defaultValue = "") String keyword) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();

		int pageNo = (page == null || page < 1) ? 1 : page;
		int size = QueryUtil.pageSize(pageSize, 10, 50);
		String kw = QueryUtil.keyword(keyword);

		LambdaQueryWrapper<AppItem> wrapper = new LambdaQueryWrapper<>();
		if (!kw.isEmpty()) {
			// 转义 % 和 _：否则一串通配符会让索引失效，等同全表扫描
			String safe = QueryUtil.escapeLike(kw);
			wrapper.and(w -> w.like(AppItem::getTitle, safe).or().like(AppItem::getSummary, safe));
		}
		wrapper.orderByAsc(AppItem::getSortOrder).orderByAsc(AppItem::getId);

		Long total = itemMapper.selectCount(wrapper);
		int offset = QueryUtil.offset(pageNo, size);
		wrapper.last("LIMIT " + offset + ", " + size);
		List<AppItem> records = itemMapper.selectList(wrapper);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("items", records);
		data.put("total", total == null ? 0L : total);
		data.put("page", pageNo);
		data.put("pageSize", size);
		return okData(data);
	}

	@GetMapping("/items/{id}")
	public Result getItem(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		AppItem item = itemMapper.selectById(id);
		if (item == null) return Result.fail("条目不存在或已被雨打风吹去");
		return okData(item);
	}

	@PostMapping("/items")
	public Result createItem(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestBody AppItem body) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		if (body == null || body.getTitle() == null || body.getTitle().isBlank()) {
			return Result.fail("标题不能为空");
		}
		body.setId(null);
		body.setCreatedAt(null);
		body.setUpdatedAt(null);
		itemMapper.insert(body);
		return okData(body);
	}

	@PutMapping("/items/{id}")
	public Result updateItem(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id,
			@RequestBody AppItem body) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		AppItem exist = itemMapper.selectById(id);
		if (exist == null) return Result.fail("条目不存在或已被雨打风吹去");
		if (body == null || body.getTitle() == null || body.getTitle().isBlank()) {
			return Result.fail("标题不能为空");
		}
		body.setId(id);
		body.setCreatedAt(null); // 不覆盖创建时间
		itemMapper.updateById(body);
		return okData(itemMapper.selectById(id));
	}

	@DeleteMapping("/items/{id}")
	public Result deleteItem(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		itemMapper.deleteById(id);
		return Result.ok("已删除该条目");
	}

	/* ================= 分类（app_category） ================= */

	@GetMapping("/categories")
	public Result listCategories(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		List<Category> list = categoryMapper.selectList(
				new LambdaQueryWrapper<Category>()
						.orderByAsc(Category::getSortOrder)
						.orderByAsc(Category::getId));
		return okData(list);
	}

	@PostMapping("/categories")
	public Result createCategory(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestBody Category body) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return Result.fail("分类名称不能为空");
		}
		if (body.getCode() == null || body.getCode().isBlank()) {
			return Result.fail("分类编码不能为空");
		}
		body.setId(null);
		body.setCreatedAt(null);
		body.setUpdatedAt(null);
		categoryMapper.insert(body);
		return okData(body);
	}

	@PutMapping("/categories/{id}")
	public Result updateCategory(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id,
			@RequestBody Category body) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		Category exist = categoryMapper.selectById(id);
		if (exist == null) return Result.fail("分类不存在");
		if (body == null || body.getName() == null || body.getName().isBlank()) {
			return Result.fail("分类名称不能为空");
		}
		body.setId(id);
		body.setCreatedAt(null);
		categoryMapper.updateById(body);
		return okData(categoryMapper.selectById(id));
	}

	@DeleteMapping("/categories/{id}")
	public Result deleteCategory(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		int auth = authState(authorization);
		if (auth == AUTH_NOLOGIN) return denyLogin();
		if (auth == AUTH_FORBIDDEN) return denyForbidden();
		categoryMapper.deleteById(id);
		return Result.ok("已删除该分类");
	}
}
