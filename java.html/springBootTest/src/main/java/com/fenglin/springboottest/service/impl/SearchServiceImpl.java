package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.AppItem;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.mapper.AppItemMapper;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.service.SearchService;
import com.fenglin.springboottest.vo.CategoryVO;
import com.fenglin.springboottest.vo.ItemDetailVO;
import com.fenglin.springboottest.vo.ItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页搜索 / 展示实现
 * 分级分类搜索：分类表 parent_id 构成树，传入父级编码时把其全部子孙分类
 * 一并纳入查询（如 jianghu 会命中「江湖动态」「门派名录」及其下所有条目）。
 */
@Service
public class SearchServiceImpl implements SearchService {

	@Autowired
	private CategoryMapper categoryMapper;

	@Autowired
	private AppItemMapper itemMapper;

	@Override
	public Result search(String q, String category, Integer page, Integer pageSize) {
		String kw = q == null ? "" : q.trim();
		String cat = category == null ? "" : category.trim();
		int pageNo = page == null || page < 1 ? 1 : page;
		int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);

		List<Category> allCategories = categoryMapper.selectList(null);
		Map<Long, String> codeById = new HashMap<>();
		for (Category c : allCategories) {
			codeById.put(c.getId(), c.getCode());
		}

		// 解析分类：all/空 = 不限；否则该分类及其全部子孙
		Set<Long> categoryIds = resolveCategoryIds(allCategories, cat);
		if (categoryIds != null && categoryIds.isEmpty()) {
			// 编码不存在：返回空结果而不是全部（避免误报）
			Result r = Result.ok("ok");
			r.setData(mapOf("items", Collections.emptyList(), "total", 0L));
			return r;
		}

		LambdaQueryWrapper<AppItem> wrapper = new LambdaQueryWrapper<>();
		if (categoryIds != null) {
			wrapper.in(AppItem::getCategoryId, categoryIds);
		}
		if (!kw.isEmpty()) {
			wrapper.and(w -> w.like(AppItem::getTitle, kw).or().like(AppItem::getSummary, kw));
		}
		wrapper.orderByAsc(AppItem::getSortOrder).orderByAsc(AppItem::getId);

		// 手动分页（count + LIMIT）：MP 3.5.9 分页插件已拆到独立依赖，此处不引入
		Long total = itemMapper.selectCount(wrapper);
		int offset = (pageNo - 1) * size;
		wrapper.last("LIMIT " + offset + ", " + size);
		List<AppItem> records = itemMapper.selectList(wrapper);

		List<ItemVO> items = records.stream()
				.map(i -> toItemVO(i, codeById))
				.collect(Collectors.toList());

		Result r = Result.ok("ok");
		r.setData(mapOf("items", items, "total", total == null ? 0L : total));
		return r;
	}

	@Override
	public Result home() {
		List<Category> allCategories = categoryMapper.selectList(
				new LambdaQueryWrapper<Category>()
						.orderByAsc(Category::getSortOrder)
						.orderByAsc(Category::getId));

		List<CategoryVO> categories = allCategories.stream()
				.map(c -> {
					CategoryVO vo = new CategoryVO();
					vo.setId(c.getId());
					vo.setParentId(c.getParentId());
					vo.setCode(c.getCode());
					vo.setName(c.getName());
					return vo;
				})
				.collect(Collectors.toList());

		Map<Long, String> codeById = new HashMap<>();
		for (Category c : allCategories) {
			codeById.put(c.getId(), c.getCode());
		}

		// 热门搜索词（hot=1，最多 8 个）
		List<AppItem> hotItems = itemMapper.selectList(
				new LambdaQueryWrapper<AppItem>()
						.eq(AppItem::getHot, 1)
						.orderByAsc(AppItem::getSortOrder)
						.last("LIMIT 8"));
		List<String> hotWords = hotItems.stream()
				.map(AppItem::getTitle)
				.collect(Collectors.toList());

		// 精选条目（featured=1，最多 10 个；含三张诗笺藏品）
		List<AppItem> featuredItems = itemMapper.selectList(
				new LambdaQueryWrapper<AppItem>()
						.eq(AppItem::getFeatured, 1)
						.orderByAsc(AppItem::getSortOrder)
						.last("LIMIT 10"));
		List<ItemVO> featured = featuredItems.stream()
				.map(i -> toItemVO(i, codeById))
				.collect(Collectors.toList());

		Result r = Result.ok("ok");
		r.setData(mapOf("categories", categories, "hotWords", hotWords, "featured", featured));
		return r;
	}

	/**
	 * 条目详情：正文 + 面包屑 + 同分类相关条目（最多 4 条）
	 */
	@Override
	public Result item(Long id) {
		if (id == null || id < 1) {
			Result r = Result.fail("条目不存在");
			return r;
		}
		AppItem item = itemMapper.selectById(id);
		if (item == null) {
			Result r = Result.fail("条目不存在或已被雨打风吹去");
			return r;
		}

		List<Category> allCategories = categoryMapper.selectList(null);
		Map<Long, String> codeById = new HashMap<>();
		Map<Long, String> nameById = new HashMap<>();
		Map<Long, Long> parentById = new HashMap<>();
		for (Category c : allCategories) {
			codeById.put(c.getId(), c.getCode());
			nameById.put(c.getId(), c.getName());
			parentById.put(c.getId(), c.getParentId());
		}

		ItemDetailVO vo = new ItemDetailVO();
		vo.setId(item.getId());
		vo.setCategory(codeById.get(item.getCategoryId()));
		vo.setCategoryName(nameById.get(item.getCategoryId()));
		Long parentId = parentById.get(item.getCategoryId());
		if (parentId != null && parentId != 0L) {
			vo.setParentCategory(codeById.get(parentId));
			vo.setParentCategoryName(nameById.get(parentId));
		}
		vo.setIc(item.getIcon());
		vo.setTitle(item.getTitle());
		vo.setDesc(item.getSummary());
		vo.setDetail(item.getDetail());
		vo.setHot(item.getHot());
		vo.setFeatured(item.getFeatured());

		// 同分类相关条目（排除自身，按排序取最多 4 条）
		List<AppItem> relatedItems = itemMapper.selectList(
				new LambdaQueryWrapper<AppItem>()
						.eq(AppItem::getCategoryId, item.getCategoryId())
						.ne(AppItem::getId, item.getId())
						.orderByAsc(AppItem::getSortOrder)
						.orderByAsc(AppItem::getId)
						.last("LIMIT 4"));
		vo.setRelated(relatedItems.stream()
				.map(i -> toItemVO(i, codeById))
				.collect(Collectors.toList()));

		Result r = Result.ok("ok");
		r.setData(vo);
		return r;
	}

	/**
	 * 解析分类编码 -> 该分类及其全部子孙的 ID 集合
	 *
	 * @return null 表示不过滤（all / 空编码）；空集合表示编码不存在
	 */
	private Set<Long> resolveCategoryIds(List<Category> all, String code) {
		if (code.isEmpty() || "all".equalsIgnoreCase(code)) {
			return null;
		}

		Category target = null;
		for (Category c : all) {
			if (code.equalsIgnoreCase(c.getCode())) {
				target = c;
				break;
			}
		}
		if (target == null) {
			return Collections.emptySet();
		}

		// 子孙索引：parentId -> children
		Map<Long, List<Category>> childrenByParent = new HashMap<>();
		for (Category c : all) {
			childrenByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
		}

		// 自顶向下收集
		Set<Long> ids = new HashSet<>();
		Deque<Category> stack = new ArrayDeque<>();
		stack.push(target);
		while (!stack.isEmpty()) {
			Category cur = stack.pop();
			if (!ids.add(cur.getId())) {
				continue;
			}
			List<Category> children = childrenByParent.get(cur.getId());
			if (children != null) {
				children.forEach(stack::push);
			}
		}
		return ids;
	}

	private ItemVO toItemVO(AppItem item, Map<Long, String> codeById) {
		ItemVO vo = new ItemVO();
		vo.setId(item.getId());
		vo.setCategory(codeById.get(item.getCategoryId()));
		vo.setIc(item.getIcon());
		vo.setTitle(item.getTitle());
		vo.setDesc(item.getSummary());
		return vo;
	}

	private static Map<String, Object> mapOf(Object... kv) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2) {
			m.put(String.valueOf(kv[i]), kv[i + 1]);
		}
		return m;
	}
}
