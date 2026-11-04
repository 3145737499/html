package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.Poem;
import com.fenglin.springboottest.mapper.PoemMapper;
import com.fenglin.springboottest.service.PoemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 众声诗笺·访客留句实现
 */
@Service
public class PoemServiceImpl implements PoemService {

	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Autowired
	private PoemMapper poemMapper;

	@Override
	public Map<String, Object> listRecent(int page, int pageSize) {
		if (page < 1) page = 1;
		if (pageSize < 1) pageSize = 12;
		if (pageSize > 50) pageSize = 50;

		long total = poemMapper.selectCount(null);
		long off = (long) (page - 1) * pageSize;

		List<Poem> rows = poemMapper.selectList(
				new LambdaQueryWrapper<Poem>()
						.orderByDesc(Poem::getCreatedAt)
						.last("LIMIT " + off + ", " + pageSize));

		List<Map<String, Object>> items = new ArrayList<>();
		for (Poem p : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", p.getId());
			m.put("content", p.getContent());
			m.put("source", p.getSource());
			m.put("createdAt", p.getCreatedAt() == null ? "" : p.getCreatedAt().format(FORMATTER));
			items.add(m);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("items", items);
		data.put("total", total);
		return data;
	}

	@Override
	public List<Map<String, Object>> corpus() {
		List<Poem> rows = poemMapper.selectList(
				new LambdaQueryWrapper<Poem>()
						.select(Poem::getContent, Poem::getSource)
						.orderByDesc(Poem::getCreatedAt)
						.last("LIMIT 500"));

		List<Map<String, Object>> items = new ArrayList<>();
		for (Poem p : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("content", p.getContent());
			m.put("source", p.getSource());
			items.add(m);
		}
		return items;
	}

	@Override
	public Poem submit(String content, String source, String ip) {
		if (content == null) {
			throw new IllegalArgumentException("诗句不可为空");
		}
		content = content.trim();
		if (content.length() < 2) {
			throw new IllegalArgumentException("诗句至少两句字（2 字以上）");
		}
		if (content.length() > 200) {
			throw new IllegalArgumentException("诗句过长，请控制在 200 字以内");
		}
		if (source != null) {
			source = source.trim();
			if (source.length() > 40) source = source.substring(0, 40);
		}

		Long dup = poemMapper.selectCount(new LambdaQueryWrapper<Poem>().eq(Poem::getContent, content));
		if (dup != null && dup > 0) {
			throw new IllegalArgumentException("这句已被收录过了");
		}

		Poem p = new Poem();
		p.setContent(content);
		p.setSource(source);
		p.setIp(ip);
		p.setCreatedAt(LocalDateTime.now());
		poemMapper.insert(p);
		return p;
	}
}
