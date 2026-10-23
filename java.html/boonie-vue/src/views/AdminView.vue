<script setup>
/**
 * 后台管理主界面 · 离恨烟烟雨阁
 * 左侧侧边栏（概览 / 藏品管理 / 分类管理）+ 右侧主区。
 * 藏品管理：对首页「江湖拾遗 / 藏品」app_item 做 增 / 删 / 改 / 查（列表 + 搜索 + 分页 + 弹窗表单）。
 * 分类管理：对 app_category 做增删改查。
 * 所有接口经 src/api/admin.js 自动携带 Bearer token；令牌失效自动退回登录页。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'
import {
  fetchItems,
  createItem,
  updateItem,
  removeItem,
  fetchCategories,
  createCategory,
  updateCategory,
  removeCategory,
  fetchProfile,
} from '@/api/admin'

const router = useRouter()
const user = useUserStore()

/* ---------------- 侧边栏 / 菜单 ---------------- */
const activeMenu = ref('overview')
const titleMap = { overview: '概览', items: '藏品管理', categories: '分类管理' }
const accountDisplay = computed(() => user.account || '阁主')
const roleDisplay = ref('')

function backHome() {
  router.push({ path: '/' })
}
async function onLogout() {
  await user.logout()
  router.push({ name: 'admin-login' })
}

/** 无后台权限：提示后退回登录页 */
function handleForbidden() {
  notify('该账号无后台管理权限，请重新登录', 'err')
  user.logout()
  setTimeout(() => router.push({ name: 'admin-login' }), 600)
}

/* ---------------- 轻量提示 ---------------- */
const toast = reactive({ show: false, msg: '', type: '' })
let toastTimer = null
function notify(msg, type = 'ok') {
  toast.msg = msg
  toast.type = type
  toast.show = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.show = false), 2600)
}

/** 令牌失效统一处理 */
function handleAuthFail() {
  notify('登录已失效，请重新登录', 'err')
  user.logout()
  setTimeout(() => router.push({ name: 'admin-login' }), 600)
}

/* ---------------- 分类（供下拉 / 映射使用） ---------------- */
const categories = ref([])
const categoryMap = computed(() => {
  const m = {}
  categories.value.forEach((c) => (m[c.id] = c))
  return m
})
const categoryName = (id) => (categoryMap.value[id] ? categoryMap.value[id].name : '—')

async function loadCategories() {
  const res = await fetchCategories()
  if (!res.success) {
    if (res.code === 401) return handleAuthFail()
    if (res.code === 403) return handleForbidden()
    notify(res.message || '分类加载失败', 'err')
    return
  }
  categories.value = res.data || []
}

/* ---------------- 概览数据 ---------------- */
const overview = reactive({ itemTotal: 0, categoryTotal: 0, featured: 0, hot: 0, loading: false })
async function loadOverview() {
  overview.loading = true
  const [itemsRes, catsRes] = await Promise.all([fetchItems({ page: 1, pageSize: 999 }), fetchCategories()])
  if (!itemsRes.success || !catsRes.success) {
    overview.loading = false
    if (itemsRes.code === 401 || catsRes.code === 401) return handleAuthFail()
    if (itemsRes.code === 403 || catsRes.code === 403) return handleForbidden()
    notify('概览数据加载失败', 'err')
    return
  }
  const items = itemsRes.data.items || []
  overview.itemTotal = itemsRes.data.total || 0
  overview.categoryTotal = (catsRes.data || []).length
  overview.featured = items.filter((i) => i.featured === 1).length
  overview.hot = items.filter((i) => i.hot === 1).length
  overview.loading = false
}

/* ---------------- 藏品（app_item）管理 ---------------- */
const items = ref([])
const itemTotal = ref(0)
const itemPage = ref(1)
const itemPageSize = ref(8)
const itemKeyword = ref('')
const itemLoading = ref(false)

async function loadItems() {
  itemLoading.value = true
  const res = await fetchItems({
    page: itemPage.value,
    pageSize: itemPageSize.value,
    keyword: itemKeyword.value,
  })
  itemLoading.value = false
  if (!res.success) {
    if (res.code === 401) return handleAuthFail()
    if (res.code === 403) return handleForbidden()
    notify(res.message || '藏品加载失败', 'err')
    return
  }
  items.value = res.data.items || []
  itemTotal.value = res.data.total || 0
}

function searchItems() {
  itemPage.value = 1
  loadItems()
}
function changeItemPage(p) {
  itemPage.value = p
  loadItems()
}

/* 藏品弹窗表单 */
const itemModal = reactive({ show: false, editing: false, saving: false, form: blankItem() })
function blankItem() {
  return {
    id: null,
    icon: '🪶',
    title: '',
    categoryId: '',
    summary: '',
    detail: '',
    hot: false,
    featured: false,
    sortOrder: 0,
  }
}
function openItemCreate() {
  Object.assign(itemModal, { show: true, editing: false, saving: false, form: blankItem() })
  if (categories.value.length) itemModal.form.categoryId = categories.value[0].id
}
function openItemEdit(row) {
  Object.assign(itemModal, {
    show: true,
    editing: true,
    saving: false,
    form: {
      id: row.id,
      icon: row.icon || '🪶',
      title: row.title || '',
      categoryId: row.categoryId ?? '',
      summary: row.summary || '',
      detail: row.detail || '',
      hot: row.hot === 1,
      featured: row.featured === 1,
      sortOrder: row.sortOrder ?? 0,
    },
  })
}
async function saveItem() {
  if (!itemModal.form.title.trim()) {
    notify('标题不能为空', 'err')
    return
  }
  itemModal.saving = true
  const f = itemModal.form
  const payload = {
    icon: f.icon || '🪶',
    title: f.title.trim(),
    categoryId: f.categoryId === '' || f.categoryId == null ? null : Number(f.categoryId),
    summary: f.summary,
    detail: f.detail,
    hot: f.hot ? 1 : 0,
    featured: f.featured ? 1 : 0,
    sortOrder: Number(f.sortOrder) || 0,
  }
  const res = itemModal.editing
    ? await updateItem(f.id, payload)
    : await createItem(payload)
  itemModal.saving = false
  if (!res.success) {
    if (res.code === 401) return handleAuthFail()
    notify(res.message || '保存失败', 'err')
    return
  }
  notify(itemModal.editing ? '已更新藏品' : '已新增藏品', 'ok')
  itemModal.show = false
  loadItems()
  loadOverview()
}

/* 删除确认 */
const pendingItem = ref(null)
function askDeleteItem(row) {
  pendingItem.value = row
}
async function confirmDeleteItem() {
  if (!pendingItem.value) return
  const res = await removeItem(pendingItem.value.id)
  pendingItem.value = null
  if (!res.success) {
    if (res.code === 401) return handleAuthFail()
    notify(res.message || '删除失败', 'err')
    return
  }
  notify('已删除该藏品', 'ok')
  loadItems()
  loadOverview()
}

/* ---------------- 分类（app_category）管理 ---------------- */
const catRows = ref([])
const catLoading = ref(false)
async function loadCategoriesTable() {
  catLoading.value = true
  const res = await fetchCategories()
  catLoading.value = false
  if (!res.success) {
    if (res.code === 401) return handleAuthFail()
    if (res.code === 403) return handleForbidden()
    notify(res.message || '分类加载失败', 'err')
    return
  }
  catRows.value = res.data || []
}

const catModal = reactive({ show: false, editing: false, saving: false, form: blankCat() })
function blankCat() {
  return { id: null, code: '', name: '', parentId: 0, sortOrder: 0 }
}
function openCatCreate() {
  Object.assign(catModal, { show: true, editing: false, saving: false, form: blankCat() })
}
function openCatEdit(row) {
  Object.assign(catModal, {
    show: true,
    editing: true,
    saving: false,
    form: {
      id: row.id,
      code: row.code || '',
      name: row.name || '',
      parentId: row.parentId ?? 0,
      sortOrder: row.sortOrder ?? 0,
    },
  })
}
async function saveCat() {
  if (!catModal.form.name.trim()) {
    notify('分类名称不能为空', 'err')
    return
  }
  if (!catModal.form.code.trim()) {
    notify('分类编码不能为空', 'err')
    return
  }
  catModal.saving = true
  const f = catModal.form
  const payload = {
    code: f.code.trim(),
    name: f.name.trim(),
    parentId: Number(f.parentId) || 0,
    sortOrder: Number(f.sortOrder) || 0,
  }
  const res = catModal.editing ? await updateCategory(f.id, payload) : await createCategory(payload)
  catModal.saving = false
  if (!res.success) {
    if (res.code === 401) return handleAuthFail()
    notify(res.message || '保存失败', 'err')
    return
  }
  notify(catModal.editing ? '已更新分类' : '已新增分类', 'ok')
  catModal.show = false
  loadCategoriesTable()
  loadCategories()
  loadOverview()
}

const pendingCat = ref(null)
function askDeleteCat(row) {
  pendingCat.value = row
}
async function confirmDeleteCat() {
  if (!pendingCat.value) return
  const res = await removeCategory(pendingCat.value.id)
  pendingCat.value = null
  if (!res.success) {
    if (res.code === 401) return handleAuthFail()
    notify(res.message || '删除失败', 'err')
    return
  }
  notify('已删除该分类', 'ok')
  loadCategoriesTable()
  loadCategories()
  loadOverview()
}

/* 切换菜单时按需加载 */
function switchMenu(menu) {
  activeMenu.value = menu
  if (menu === 'items') loadItems()
  else if (menu === 'categories') loadCategoriesTable()
  else if (menu === 'overview') loadOverview()
}

onMounted(() => {
  user.restore()
  fetchProfile()
    .then((p) => {
      if (p.success && p.data) roleDisplay.value = p.data.role || ''
    })
    .catch(() => {})
  loadCategories().then(() => {
    // 默认进入概览
    loadOverview()
  })
})
</script>

<template>
  <div class="admin-shell">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="side-brand">
        <div class="seal-mark side-seal">阁</div>
        <div>
          <div class="side-title">烟雨阁 · 后台</div>
          <div class="side-sub">江湖拾遗管理</div>
        </div>
      </div>

      <nav class="side-menu">
        <a :class="{ active: activeMenu === 'overview' }" @click="switchMenu('overview')">
          <span class="mi">❖</span> 概览
        </a>
        <a :class="{ active: activeMenu === 'items' }" @click="switchMenu('items')">
          <span class="mi">❀</span> 藏品管理
        </a>
        <a :class="{ active: activeMenu === 'categories' }" @click="switchMenu('categories')">
          <span class="mi">▤</span> 分类管理
        </a>
      </nav>

      <div class="side-foot">
        <div class="side-user">
          <span class="avatar">阁</span>
          <div>
            <div class="uname">{{ accountDisplay }}</div>
            <div class="urole">{{ roleDisplay === 'admin' ? '后台管理员' : (roleDisplay || '管理员') }}</div>
          </div>
        </div>
        <div class="side-actions">
          <a class="link" @click="backHome">返回前台</a>
          <a class="link logout" @click="onLogout">退出登录</a>
        </div>
      </div>
    </aside>

    <!-- 主区 -->
    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <h2>{{ titleMap[activeMenu] }}</h2>
          <p class="crumb">后台管理 / {{ titleMap[activeMenu] }}</p>
        </div>
      </header>

      <div class="admin-content">
        <!-- 概览 -->
        <section v-if="activeMenu === 'overview'" class="panel">
          <div class="stat-grid">
            <div class="stat-card">
              <div class="num">{{ overview.loading ? '…' : overview.itemTotal }}</div>
              <div class="lab">藏品总数</div>
            </div>
            <div class="stat-card">
              <div class="num">{{ overview.loading ? '…' : overview.categoryTotal }}</div>
              <div class="lab">分类总数</div>
            </div>
            <div class="stat-card">
              <div class="num">{{ overview.loading ? '…' : overview.featured }}</div>
              <div class="lab">首页精选</div>
            </div>
            <div class="stat-card">
              <div class="num">{{ overview.loading ? '…' : overview.hot }}</div>
              <div class="lab">热门词条</div>
            </div>
          </div>
          <p class="panel-tip">提示：首页「江湖拾遗 / 阁中珍藏」展示的内容，均来自「藏品管理」中的条目；标记「首页精选」的藏品会出现在首页珍藏卷轴。</p>
        </section>

        <!-- 藏品管理 -->
        <section v-else-if="activeMenu === 'items'" class="panel">
          <div class="toolbar">
            <div class="search">
              <input v-model="itemKeyword" class="input" placeholder="搜索标题 / 简介" @keyup.enter="searchItems" />
              <button class="btn ghost sm" @click="searchItems">搜索</button>
            </div>
            <button class="btn primary sm" @click="openItemCreate">+ 新增藏品</button>
          </div>

          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th style="width: 56px">图标</th>
                  <th>标题</th>
                  <th style="width: 110px">分类</th>
                  <th>简介</th>
                  <th style="width: 64px">精选</th>
                  <th style="width: 64px">热门</th>
                  <th style="width: 72px">排序</th>
                  <th style="width: 130px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="itemLoading"><td colspan="8" class="empty">加载中…</td></tr>
                <tr v-else-if="!items.length"><td colspan="8" class="empty">暂无藏品，点击「新增藏品」创建第一条</td></tr>
                <tr v-for="row in items" :key="row.id">
                  <td class="ic-cell">{{ row.icon || '🪶' }}</td>
                  <td class="title-cell">{{ row.title }}</td>
                  <td>{{ categoryName(row.categoryId) }}</td>
                  <td class="summary-cell">{{ row.summary || '—' }}</td>
                  <td><span :class="['tag', row.featured === 1 ? 'on' : '']">{{ row.featured === 1 ? '是' : '否' }}</span></td>
                  <td><span :class="['tag', row.hot === 1 ? 'on' : '']">{{ row.hot === 1 ? '是' : '否' }}</span></td>
                  <td>{{ row.sortOrder ?? 0 }}</td>
                  <td class="ops">
                    <a class="link" @click="openItemEdit(row)">编辑</a>
                    <a class="link danger" @click="askDeleteItem(row)">删除</a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pager" v-if="itemTotal > itemPageSize">
            <button class="pg" :disabled="itemPage <= 1" @click="changeItemPage(itemPage - 1)">上一页</button>
            <span class="pg-info">第 {{ itemPage }} 页 / 共 {{ Math.ceil(itemTotal / itemPageSize) }} 页 · {{ itemTotal }} 条</span>
            <button class="pg" :disabled="itemPage >= Math.ceil(itemTotal / itemPageSize)" @click="changeItemPage(itemPage + 1)">下一页</button>
          </div>
        </section>

        <!-- 分类管理 -->
        <section v-else-if="activeMenu === 'categories'" class="panel">
          <div class="toolbar">
            <div class="search"><span class="muted">共 {{ catRows.length }} 个分类</span></div>
            <button class="btn primary sm" @click="openCatCreate">+ 新增分类</button>
          </div>

          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th style="width: 120px">编码</th>
                  <th>名称</th>
                  <th style="width: 140px">父级</th>
                  <th style="width: 72px">排序</th>
                  <th style="width: 130px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="catLoading"><td colspan="5" class="empty">加载中…</td></tr>
                <tr v-else-if="!catRows.length"><td colspan="5" class="empty">暂无分类</td></tr>
                <tr v-for="row in catRows" :key="row.id">
                  <td class="code-cell">{{ row.code }}</td>
                  <td class="title-cell">{{ row.name }}</td>
                  <td>{{ row.parentId === 0 ? '顶级' : (categoryMap[row.parentId] ? categoryMap[row.parentId].name : '—') }}</td>
                  <td>{{ row.sortOrder ?? 0 }}</td>
                  <td class="ops">
                    <a class="link" @click="openCatEdit(row)">编辑</a>
                    <a class="link danger" @click="askDeleteCat(row)">删除</a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </main>

    <!-- 藏品弹窗 -->
    <div v-if="itemModal.show" class="modal-mask" @click.self="itemModal.show = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ itemModal.editing ? '编辑藏品' : '新增藏品' }}</h3>
          <span class="close" @click="itemModal.show = false">×</span>
        </div>
        <div class="modal-body">
          <div class="form-row two">
            <div class="field">
              <label>图标（emoji）</label>
              <input v-model="itemModal.form.icon" class="input" maxlength="4" placeholder="🪶" />
            </div>
            <div class="field">
              <label>排序</label>
              <input v-model.number="itemModal.form.sortOrder" type="number" class="input" placeholder="0" />
            </div>
          </div>
          <div class="field">
            <label>标题 *</label>
            <input v-model="itemModal.form.title" class="input" placeholder="请输入标题" />
          </div>
          <div class="field">
            <label>所属分类</label>
            <select v-model="itemModal.form.categoryId" class="input">
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="field">
            <label>简介</label>
            <textarea v-model="itemModal.form.summary" class="input" rows="2" placeholder="首页卡片展示的简介"></textarea>
          </div>
          <div class="field">
            <label>详情（多段用换行分隔）</label>
            <textarea v-model="itemModal.form.detail" class="input" rows="3" placeholder="详情页正文"></textarea>
          </div>
          <div class="check-row">
            <label class="check"><input type="checkbox" v-model="itemModal.form.featured" /> 首页精选</label>
            <label class="check"><input type="checkbox" v-model="itemModal.form.hot" /> 热门词条</label>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn ghost sm" @click="itemModal.show = false">取消</button>
          <button class="btn primary sm" :disabled="itemModal.saving" @click="saveItem">
            {{ itemModal.saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 分类弹窗 -->
    <div v-if="catModal.show" class="modal-mask" @click.self="catModal.show = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ catModal.editing ? '编辑分类' : '新增分类' }}</h3>
          <span class="close" @click="catModal.show = false">×</span>
        </div>
        <div class="modal-body">
          <div class="form-row two">
            <div class="field">
              <label>编码 *</label>
              <input v-model="catModal.form.code" class="input" placeholder="如 jianghu" />
            </div>
            <div class="field">
              <label>排序</label>
              <input v-model.number="catModal.form.sortOrder" type="number" class="input" placeholder="0" />
            </div>
          </div>
          <div class="field">
            <label>名称 *</label>
            <input v-model="catModal.form.name" class="input" placeholder="如 江湖" />
          </div>
          <div class="field">
            <label>父级分类</label>
            <select v-model="catModal.form.parentId" class="input">
              <option :value="0">顶级分类</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn ghost sm" @click="catModal.show = false">取消</button>
          <button class="btn primary sm" :disabled="catModal.saving" @click="saveCat">
            {{ catModal.saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <div v-if="pendingItem || pendingCat" class="modal-mask" @click.self="pendingItem = null; pendingCat = null">
      <div class="modal confirm">
        <div class="modal-head"><h3>确认删除</h3></div>
        <div class="modal-body">
          <p>确定要删除
            <b v-if="pendingItem">「{{ pendingItem.title }}」</b>
            <b v-else-if="pendingCat">「{{ pendingCat.name }}」</b>
            吗？此操作不可撤销。</p>
        </div>
        <div class="modal-foot">
          <button class="btn ghost sm" @click="pendingItem = null; pendingCat = null">取消</button>
          <button class="btn danger sm" v-if="pendingItem" @click="confirmDeleteItem">删除</button>
          <button class="btn danger sm" v-else-if="pendingCat" @click="confirmDeleteCat">删除</button>
        </div>
      </div>
    </div>

    <!-- 提示 -->
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="toast.type">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  height: 100vh;
  min-height: 100dvh;
  background: linear-gradient(135deg, #cfe3de 0%, #e8f0ec 45%, #d4e6e0 100%);
  overflow: hidden;
}

/* ---------------- 侧边栏 ---------------- */
.admin-sidebar {
  width: 240px;
  flex: 0 0 240px;
  background: linear-gradient(180deg, #1e2b28 0%, #243832 100%);
  color: #e9efe9;
  display: flex;
  flex-direction: column;
  box-shadow: 6px 0 30px rgba(30, 43, 40, 0.25);
  position: relative;
  z-index: 3;
}
.side-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.side-seal {
  width: 42px; height: 42px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; transform: rotate(6deg);
}
.side-title { font-size: 18px; letter-spacing: 4px; color: #f4f1e8; }
.side-sub { font-size: 12px; letter-spacing: 2px; color: rgba(233, 239, 233, 0.55); margin-top: 2px; }

.side-menu { flex: 1; padding: 16px 12px; display: flex; flex-direction: column; gap: 4px; }
.side-menu a {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
  border-radius: 6px;
  font-size: 15px; letter-spacing: 3px;
  color: rgba(233, 239, 233, 0.75);
  cursor: pointer;
  transition: all 0.25s;
}
.side-menu a .mi { width: 18px; text-align: center; opacity: 0.7; }
.side-menu a:hover { background: rgba(255, 255, 255, 0.06); color: #fff; }
.side-menu a.active {
  background: linear-gradient(90deg, rgba(78, 141, 134, 0.35), rgba(78, 141, 134, 0.08));
  color: #fff;
  box-shadow: inset 3px 0 0 var(--gold);
}

.side-foot { padding: 16px 18px; border-top: 1px solid rgba(255, 255, 255, 0.08); }
.side-user { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.side-user .avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--jade); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 15px;
}
.side-user .uname { font-size: 14px; letter-spacing: 2px; color: #f4f1e8; }
.side-user .urole { font-size: 11px; letter-spacing: 2px; color: rgba(233, 239, 233, 0.5); }
.side-actions { display: flex; gap: 16px; }
.side-actions .link { color: rgba(233, 239, 233, 0.6); font-size: 13px; letter-spacing: 2px; cursor: pointer; }
.side-actions .link:hover { color: #fff; }
.side-actions .link.logout { color: #e08b80; }

/* ---------------- 主区 ---------------- */
.admin-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.admin-topbar {
  padding: 22px 32px 14px;
  border-bottom: 1px solid rgba(201, 163, 92, 0.25);
  background: rgba(244, 241, 232, 0.6);
  backdrop-filter: blur(8px);
}
.admin-topbar h2 { font-size: 24px; font-weight: normal; letter-spacing: 6px; color: var(--ink); margin: 0; }
.admin-topbar .crumb { margin: 4px 0 0; font-size: 12px; letter-spacing: 2px; color: var(--ink-soft); opacity: 0.6; }

.admin-content { flex: 1; overflow-y: auto; padding: 24px 32px 40px; }
.panel { max-width: 1080px; }

/* 概览 */
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 18px; }
.stat-card {
  background: rgba(250, 248, 240, 0.9);
  border: 1px solid rgba(201, 163, 92, 0.4);
  border-radius: 8px;
  padding: 26px 24px;
  box-shadow: 0 12px 28px rgba(30, 43, 40, 0.1);
  position: relative;
  overflow: hidden;
}
.stat-card::after {
  content: ''; position: absolute; right: -20px; top: -20px;
  width: 80px; height: 80px; border-radius: 50%;
  background: radial-gradient(circle, rgba(78, 141, 134, 0.18), transparent 70%);
}
.stat-card .num { font-size: 38px; font-weight: normal; letter-spacing: 2px; color: var(--ink); }
.stat-card .num::after { content: ''; display: block; width: 30px; height: 2px; margin-top: 8px; background: linear-gradient(90deg, var(--gold), transparent); }
.stat-card .lab { margin-top: 8px; font-size: 13px; letter-spacing: 4px; color: var(--ink-soft); opacity: 0.8; }
.panel-tip { margin-top: 22px; font-size: 13px; line-height: 1.9; letter-spacing: 1px; color: var(--ink-soft); opacity: 0.8; }

/* 工具栏 */
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 12px; flex-wrap: wrap; }
.toolbar .search { display: flex; gap: 8px; align-items: center; }
.toolbar .muted { font-size: 13px; letter-spacing: 2px; color: var(--ink-soft); opacity: 0.8; }

/* 表格 */
.table-wrap {
  background: rgba(250, 248, 240, 0.92);
  border: 1px solid rgba(201, 163, 92, 0.4);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(30, 43, 40, 0.1);
}
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table thead th {
  text-align: left; padding: 13px 14px;
  font-weight: normal; letter-spacing: 2px;
  background: rgba(36, 56, 50, 0.06);
  color: var(--ink-soft);
  border-bottom: 1px solid rgba(201, 163, 92, 0.3);
}
.data-table tbody td { padding: 12px 14px; border-bottom: 1px solid rgba(201, 163, 92, 0.16); color: var(--ink); vertical-align: middle; }
.data-table tbody tr:last-child td { border-bottom: none; }
.data-table tbody tr:hover { background: rgba(78, 141, 134, 0.06); }
.ic-cell { font-size: 20px; text-align: center; }
.title-cell { font-weight: 500; letter-spacing: 1px; }
.summary-cell { color: var(--ink-soft); max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.code-cell { font-family: ui-monospace, monospace; letter-spacing: 1px; color: var(--ink-soft); }
.empty { text-align: center; padding: 38px; color: var(--ink-soft); opacity: 0.7; letter-spacing: 2px; }
.ops { white-space: nowrap; }
.ops .link { margin-right: 14px; font-size: 13px; letter-spacing: 1px; }

.tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; letter-spacing: 1px; background: rgba(61, 80, 75, 0.12); color: var(--ink-soft); }
.tag.on { background: rgba(78, 141, 134, 0.18); color: #2f6f4f; }

/* 分页 */
.pager { display: flex; align-items: center; gap: 14px; margin-top: 16px; justify-content: flex-end; }
.pg { padding: 8px 16px; border: 1px solid rgba(163, 127, 61, 0.5); border-radius: 4px; background: rgba(250, 248, 240, 0.8); color: var(--ink); font-size: 13px; letter-spacing: 2px; cursor: pointer; transition: all 0.2s; }
.pg:hover:not(:disabled) { border-color: var(--gold-deep); background: rgba(201, 163, 92, 0.14); }
.pg:disabled { opacity: 0.4; cursor: not-allowed; }
.pg-info { font-size: 13px; letter-spacing: 1px; color: var(--ink-soft); }

/* 按钮尺寸 */
.btn.sm { padding: 9px 20px; font-size: 14px; letter-spacing: 4px; text-indent: 4px; }
.btn.danger { background: linear-gradient(120deg, #a83a2f, #c0523f); color: #f8f2e4; box-shadow: 0 8px 20px rgba(168, 58, 47, 0.3); }
.btn.danger:hover { transform: translateY(-2px); }

/* 表单控件 */
.input {
  width: 100%; padding: 10px 12px;
  border: 1px solid rgba(61, 80, 75, 0.25);
  border-radius: 4px;
  background: #fff;
  font-family: inherit; font-size: 14px; color: var(--ink);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input:focus { outline: none; border-color: var(--jade); box-shadow: 0 0 0 3px rgba(78, 141, 134, 0.15); }
textarea.input { resize: vertical; line-height: 1.6; }

/* ---------------- 弹窗 ---------------- */
.modal-mask {
  position: fixed; inset: 0; z-index: 50;
  background: rgba(30, 43, 40, 0.45);
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(2px);
  animation: fade-in 0.2s ease;
}
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
.modal {
  width: 520px; max-width: 92vw; max-height: 88vh; overflow-y: auto;
  background: #fbf9f3;
  border: 1px solid rgba(201, 163, 92, 0.5);
  border-radius: 10px;
  box-shadow: 0 30px 80px rgba(30, 43, 40, 0.4);
  animation: pop-in 0.22s cubic-bezier(0.22, 0.8, 0.3, 1);
}
@keyframes pop-in { from { opacity: 0; transform: translateY(14px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
.modal.confirm { width: 420px; }
.modal-head { display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-bottom: 1px solid rgba(201, 163, 92, 0.3); }
.modal-head h3 { margin: 0; font-size: 18px; font-weight: normal; letter-spacing: 4px; color: var(--ink); }
.modal-head .close { font-size: 24px; line-height: 1; color: var(--ink-soft); cursor: pointer; opacity: 0.6; }
.modal-head .close:hover { opacity: 1; color: var(--cinnabar); }
.modal-body { padding: 20px 22px; }
.modal-body .field { margin-bottom: 16px; }
.modal-body label { display: block; font-size: 13px; letter-spacing: 2px; color: var(--ink-soft); margin-bottom: 6px; }
.form-row.two { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.check-row { display: flex; gap: 24px; margin-top: 4px; }
.check { display: flex; align-items: center; gap: 8px; font-size: 14px; letter-spacing: 2px; color: var(--ink); cursor: pointer; }
.check input { width: 16px; height: 16px; accent-color: var(--jade); }
.modal-foot { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 22px; border-top: 1px solid rgba(201, 163, 92, 0.3); }

/* 提示 toast */
.toast {
  position: fixed; top: 24px; left: 50%; transform: translateX(-50%);
  z-index: 80; padding: 12px 26px; border-radius: 6px;
  font-size: 14px; letter-spacing: 2px; color: #f8f2e4;
  background: rgba(36, 56, 50, 0.95);
  box-shadow: 0 14px 36px rgba(30, 43, 40, 0.35);
}
.toast.ok { background: linear-gradient(120deg, #2f6f4f, #3d6f66); }
.toast.err { background: linear-gradient(120deg, #a83a2f, #c0523f); }
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -10px); }

@media (max-width: 760px) {
  .admin-sidebar { width: 64px; flex-basis: 64px; }
  .side-brand .side-title, .side-brand .side-sub, .side-menu a span:not(.mi), .side-foot { display: none; }
  .side-menu a { justify-content: center; }
  .admin-content { padding: 18px 16px 32px; }
  .form-row.two { grid-template-columns: 1fr; }
}
</style>
