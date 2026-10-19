/**
 * 搜索 / 首页展示 API（真实后端接口）
 *
 * 后端接口（SpringBootTest · SearchController）：
 *   GET /api/search?q={关键词}&category={分类编码}&page={页码}&pageSize={每页}
 *     返回结构（统一）：{ success, message, data: { items: [{id, category, ic, title, desc}], total } }
 *     分级分类：category 传父级编码（如 jianghu）时自动连带全部子分类条目
 *   GET /api/home → { success, message, data: { categories, hotWords, featured } }
 *
 * 历史：2026-10-10 ~ 10-13 为本地数据集占位实现，2026-10-14 起后端接口就绪，
 * 调用方（HomeView）签名与返回结构保持不变、无需改动。
 */

/**
 * 搜索
 * @param {{ q?: string, category?: string, page?: number, pageSize?: number }} params
 * @returns {Promise<{ success: boolean, message: string, data: { items: any[], total: number } }>}
 */
export async function search({ q = '', category = 'all', page = 1, pageSize = 20 } = {}) {
  try {
    const query = new URLSearchParams({
      q: String(q || ''),
      category: String(category || 'all'),
      page: String(page),
      pageSize: String(pageSize),
    })
    const res = await fetch(`/api/search?${query.toString()}`)
    return await res.json()
  } catch (e) {
    // 后端未就绪 / 网络异常：返回与成功结构一致的空结果，前端显示空状态
    return { success: false, message: '江湖信号不通，稍后再试', data: { items: [], total: 0 } }
  }
}

/**
 * 首页展示数据（分类树、热门搜索词、精选条目）
 * @returns {Promise<{ success: boolean, message: string, data: { categories: any[], hotWords: string[], featured: any[] } }>}
 */
export async function fetchHome() {
  try {
    const res = await fetch('/api/home')
    return await res.json()
  } catch (e) {
    return { success: false, message: '江湖信号不通，稍后再试', data: { categories: [], hotWords: [], featured: [] } }
  }
}

/**
 * 条目详情（详情页）
 * GET /api/item/{id} -> { success, message, data: { id, category, categoryName,
 *   parentCategory, parentCategoryName, ic, title, desc, detail(多段\n分隔), hot, featured, related } }
 * @param {number|string} id
 */
export async function fetchItem(id) {
  try {
    const res = await fetch(`/api/item/${id}`)
    return await res.json()
  } catch (e) {
    return { success: false, message: '江湖信号不通，稍后再试', data: null }
  }
}
