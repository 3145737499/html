<script setup>
/**
 * 首页 · 离恨烟 · 烟雨阁（登录后落地页）
 * 功能：搜索栏 + 导航栏（分类切换）+ 内容卡片（点击进详情）
 * 风格：沿用登录页水墨设计系统（ink.css 变量与卡片质感）
 *
 * 特效层（2026-10-14 优化）：
 *   - AI 生成水墨横幅（/ink-hero.png）做英雄区背景，随鼠标轻微视差
 *   - 雨丝层（烟雨阁点题），纯 DOM + CSS 动画，卸载时清理
 *   - 卡片：聚光跟随鼠标 + 水墨印纹水印 + 进场瀑布延迟
 *   - 主标题逐字浮现（毛笔落纸感）
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'
import { useReveal } from '@/composables/useReveal'
import { search, fetchHome, fetchStats } from '@/api/search'
import InkMotif from '@/components/InkMotif.vue'

const router = useRouter()
const user = useUserStore()
const root = ref(null)
useReveal(root)

/* ---------------- 搜索与导航状态 ---------------- */
const keyword = ref('')
const activeCat = ref('all')
const results = ref([])
const loading = ref(false)
const searched = ref(false)

/* 导航分类：先静态兜底，/api/home 就绪后由数据库覆盖（2026-10-14 起后端驱动） */
const navItems = ref([
  { key: 'all', label: '全部' },
  { key: 'jianghu', label: '江湖' },
  { key: 'umbrella', label: '油纸伞' },
  { key: 'badge', label: '江湖徽章' },
  { key: 'log', label: '行侠日志' },
  { key: 'friend', label: '侠客同游' },
  { key: 'setting', label: '账号设置' },
])

/* 热门词：同样静态兜底 + 接口覆盖 */
const hotWords = ref(['油纸伞', '江湖徽章', '行侠日志', '侠客同游', '账号设置'])

/* 精选条目：来自 /api/home 的 featured（最多 6 条），接口失败时回退到静态兜底 */
const featured = ref([])
const fallbackFeatured = [
  { id: 1, category: 'umbrella', ic: '🌂', title: '青竹油纸伞', desc: '伞骨以三年青竹剖篾，桐油七遍，雨打不漏。' },
  { id: 2, category: 'badge', ic: '🏅', title: '江湖同心徽', desc: '侠客结义所持，朱砂为印，金线绕边。' },
  { id: 3, category: 'jianghu', ic: '⚔️', title: '听雨剑谱', desc: '剑意如雨，绵绵不绝，后劲藏于收敛。' },
  { id: 4, category: 'log', ic: '📜', title: '行侠手札', desc: '记一路风雨与相逢，墨淡而情长。' },
]

/* 诗笺主题藏品：以三句江湖诗为引，始终并入「阁中珍藏」，与别的卡片功能相同
 * - id 23 桥上看景：你站在桥上看风景，看风景的人在楼上看你（桥/楼意象）
 * - id 24 岁晚风暖：风清日暖，正是人间岁晚时（暖日/岁晚意象）
 * - id 25 镜花辞树：最是人间留不住，朱颜辞镜花辞树（镜/花辞树意象）
 * 后端就绪后由数据库提供（同 id），此处作为兜底与常驻展示；category 直接驱动 InkMotif 母题 */
const extraFeatured = ref([
  {
    id: 23,
    category: 'bridge',
    ic: '🌉',
    title: '桥上看景',
    desc: '你站在桥上看风景，看风景的人在楼上看你。',
  },
  {
    id: 24,
    category: 'sun',
    ic: '☀️',
    title: '岁晚风暖',
    desc: '风清日暖，正是人间岁晚时。',
  },
  {
    id: 25,
    category: 'mirror',
    ic: '🌸',
    title: '镜花辞树',
    desc: '最是人间留不住，朱颜辞镜花辞树。',
  },
])

/* 全量分类（含子类，来自 /api/home），用于子分类筛选与名称解析 */
const categories = ref([])

/* 数字速览（来自 /api/stats），后端未就绪时回退到静态兜底 */
const stats = ref({ itemCount: 25, categoryCount: 6, listenRain: 0 })

/**
 * 当前选中分类的顶级编码：选中子分类时高亮其父级导航页签
 */
const activeTopCode = computed(() => {
  const cur = categories.value.find((c) => c.code === activeCat.value)
  if (cur && cur.parentId !== 0) {
    const parent = categories.value.find((c) => c.id === cur.parentId)
    return parent ? parent.code : activeCat.value
  }
  return activeCat.value
})

/**
 * 子分类快捷筛选：选中顶级分类时展示其子类（分级分类搜索入口）
 */
const subCats = computed(() => {
  if (!categories.value.length || activeCat.value === 'all') return []
  const all = categories.value
  const cur = all.find((c) => c.code === activeCat.value)
  if (!cur) return []
  const parent = cur.parentId === 0 ? cur : all.find((c) => c.id === cur.parentId)
  if (!parent) return []
  return all.filter((c) => c.parentId === parent.id)
})

const resultTitle = computed(() => {
  const cat = categories.value.find((c) => c.code === activeCat.value)
  if (cat) return cat.name
  const item = navItems.value.find((n) => n.key === activeCat.value)
  return item ? item.label : '全部'
})

/* ---------------- 特效状态：视差 / 雨丝 ---------------- */
const heroPx = ref({ x: 0, y: 0 })

const bannerStyle = computed(() => ({
  transform: `scale(1.08) translate(${heroPx.value.x * -12}px, ${heroPx.value.y * -8}px)`,
}))

function onHeroMove(e) {
  const r = e.currentTarget.getBoundingClientRect()
  heroPx.value = {
    x: (e.clientX - r.left) / r.width - 0.5,
    y: (e.clientY - r.top) / r.height - 0.5,
  }
}
function onHeroLeave() {
  heroPx.value = { x: 0, y: 0 }
}

/* 卡片聚光：把鼠标位置写入 CSS 变量，样式层渲染光晕 */
function cardSpot(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  el.style.setProperty('--mx', `${e.clientX - r.left}px`)
  el.style.setProperty('--my', `${e.clientY - r.top}px`)
}

/* 雨丝层：生成随机雨丝，组件卸载时清理 */
const rainEl = ref(null)
let drops = []
function buildRain() {
  if (!rainEl.value) return
  // 移动端降载：雨丝减半，避免小屏性能压力
  const n = window.innerWidth < 768 ? 14 : 26
  for (let i = 0; i < n; i++) {
    const d = document.createElement('i')
    d.className = 'drop'
    d.style.cssText = [
      `left:${Math.random() * 100}%`,
      `height:${38 + Math.random() * 52}px`,
      `opacity:${0.12 + Math.random() * 0.26}`,
      `animation-duration:${1.1 + Math.random() * 1.5}s`,
      `animation-delay:${-Math.random() * 3}s`,
    ].join(';')
    rainEl.value.appendChild(d)
    drops.push(d)
  }
}

/* 分类检索区锚点（卷首语「入阁拾遗」平滑滚动到此） */
const contentRef = ref(null)

/* 每日诗笺：按日期轮换一句江湖烟雨诗，让首页有「活」的气息 */
const verses = [
  '一蓑烟雨任平生',
  '小楼一夜听春雨，深巷明朝卖杏花',
  '竹杖芒鞋轻胜马，谁怕',
  '落花人独立，微雨燕双飞',
  '青箬笠，绿蓑衣，斜风细雨不须归',
  '自在飞花轻似梦，无边丝雨细如愁',
  '回首向来萧瑟处，归去，也无风雨也无晴',
  '江雨霏霏江草齐，六朝如梦鸟空啼',
]
const dailyVerse = computed(() => {
  const day = Math.floor(Date.now() / 86400000)
  return verses[day % verses.length]
})

/* 江湖诗笺：独立板块，可点「换一句」切换；默认落在今日轮换句 */
const poemBook = [
  { t: '小楼一夜听春雨，深巷明朝卖杏花', s: '陆游《临安春雨初霁》' },
  { t: '一蓑烟雨任平生', s: '苏轼《定风波》' },
  { t: '竹杖芒鞋轻胜马，谁怕？一蓑烟雨任平生', s: '苏轼《定风波》' },
  { t: '落花人独立，微雨燕双飞', s: '晏几道《临江仙》' },
  { t: '青箬笠，绿蓑衣，斜风细雨不须归', s: '张志和《渔歌子》' },
  { t: '自在飞花轻似梦，无边丝雨细如愁', s: '秦观《浣溪沙》' },
  { t: '回首向来萧瑟处，归去，也无风雨也无晴', s: '苏轼《定风波》' },
  { t: '江雨霏霏江草齐，六朝如梦鸟空啼', s: '韦庄《台城》' },
  { t: '春水碧于天，画船听雨眠', s: '韦庄《菩萨蛮》' },
  { t: '细雨湿衣看不见，闲花落地听无声', s: '刘长卿《别严士元》' },
  { t: '水光潋滟晴方好，山色空蒙雨亦奇', s: '苏轼《饮湖上初晴后雨》' },
  { t: '墨痕未干山欲雨，纸窗竹影共清寒', s: '烟雨阁·佚名' },
]
const poemIndex = ref(Math.floor(Date.now() / 86400000) % poemBook.length)
function changePoem() {
  let n = poemIndex.value
  while (n === poemIndex.value && poemBook.length > 1) {
    n = Math.floor(Math.random() * poemBook.length)
  }
  poemIndex.value = n
}

/* 听雨佳绩（读小游戏本地存档，serene=悠然听雨 最贴合慢节奏） */
const gameBest = ref(0)
function loadGameBest() {
  try {
    const raw = localStorage.getItem('boonie-rain-best')
    if (raw) {
      const o = JSON.parse(raw)
      gameBest.value = Math.max(o.serene || 0, o.storm || 0)
    }
  } catch {
    /* 损坏数据忽略 */
  }
}

/* 入阁拾遗：平滑滚动到分类检索区 */
function scrollToContent() {
  contentRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function goGame() {
  router.push({ name: 'game' })
}

/* ---------------- 调用后端搜索接口 ---------------- */
async function runSearch() {
  loading.value = true
  searched.value = true
  const res = await search({ q: keyword.value, category: activeCat.value })
  loading.value = false
  results.value = res.success ? res.data.items : []
}

function pickCat(cat) {
  if (activeCat.value === cat) return
  activeCat.value = cat
  runSearch()
}

function onHot(word) {
  keyword.value = word
  runSearch()
}

function onSubmit() {
  runSearch()
}

function onClear() {
  keyword.value = ''
  runSearch()
}

/* 进入条目详情页 */
function goDetail(id) {
  router.push({ name: 'item', params: { id } })
}

onMounted(async () => {
  buildRain()
  runSearch()

  // 首页展示数据（分级分类 + 热门词）由后端提供；失败时保留静态兜底
  const res = await fetchHome()
  if (res.success && res.data) {
    const d = res.data
    if (Array.isArray(d.categories) && d.categories.length) {
      categories.value = d.categories
      const tops = d.categories.filter((c) => c.parentId === 0)
      if (tops.length) {
        navItems.value = [
          { key: 'all', label: '全部' },
          ...tops.map((c) => ({ key: c.code, label: c.name })),
        ]
      }
    }
    if (Array.isArray(d.hotWords) && d.hotWords.length) hotWords.value = d.hotWords
    // 精选：先取后端 featured；再常驻并入两张诗笺卡片（按 id 去重，避免后端已返回时重复）
    if (Array.isArray(d.featured) && d.featured.length) {
      const seen = new Set(d.featured.map((x) => x.id))
      featured.value = [...d.featured, ...extraFeatured.value.filter((x) => !seen.has(x.id))]
    } else {
      featured.value = [...fallbackFeatured, ...extraFeatured.value]
    }
  }

  // 数字速览（藏品数 / 分类数 / 听雨人次），后端失败保留静态兜底
  const statsRes = await fetchStats()
  if (statsRes.success && statsRes.data) stats.value = statsRes.data

  loadGameBest()
})

onBeforeUnmount(() => {
  drops.forEach((d) => d.remove())
  drops = []
})
</script>

<template>
  <section ref="root" class="view home-page active">
    <!-- 飘浮烟雾（呼应登录页氛围） -->
    <div class="home-mist m1"></div>
    <div class="home-mist m2"></div>

    <!-- 雨丝层（烟雨阁点题） -->
    <div ref="rainEl" class="rain" aria-hidden="true"></div>

    <!-- ============ 搜索区（搜索栏） ============ -->
    <div class="search-hero reveal" @mousemove="onHeroMove" @mouseleave="onHeroLeave">
      <!-- 水墨横幅背景（AI 生成，边缘羽化融入纸面） -->
      <div class="hero-banner" :style="bannerStyle" aria-hidden="true"></div>

      <span class="seal seal-mark hero-seal">离恨烟</span>

      <h1 class="hero-title" aria-label="伞下江湖">
        <span
          v-for="(ch, i) in '伞下江湖'"
          :key="i"
          class="title-char"
          :style="{ animationDelay: `${200 + i * 140}ms` }"
          aria-hidden="true"
        >{{ ch }}</span>
      </h1>
      <p class="hero-verse">烟水茫茫 · 离恨绵绵 <b>伞落惊鸿</b></p>
      <p class="hero-greet">{{ user.greeting }} 今日烟雨正好，搜一搜你的江湖。</p>

      <form class="search-bar" novalidate @submit.prevent="onSubmit">
        <span class="search-ico">
          <svg viewBox="0 0 24 24" fill="none" stroke="#3d504b" stroke-width="1.8">
            <circle cx="11" cy="11" r="7" /><path d="M21,21 L16.5,16.5" stroke-linecap="round" />
          </svg>
        </span>
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="搜索油纸伞、江湖徽章、行侠日志…"
        />
        <button v-if="keyword" type="button" class="search-clear" title="清空" @click="onClear">✕</button>
        <button type="submit" class="btn primary search-btn" :class="{ loading }">
          <span class="spinner"></span>
          <span>搜 索</span>
        </button>
      </form>

      <div class="hot-words">
        <span class="hot-label">热门：</span>
        <button v-for="w in hotWords" :key="w" type="button" class="hot-chip" @click="onHot(w)">
          {{ w }}
        </button>
      </div>
    </div>

    <!-- ============ 卷首语（主旨呈现） ============ -->
    <section class="prologue reveal">
      <!-- 藏阁卷首：AI 水墨横幅做背景，羽化融入纸面 -->
      <div class="prologue-bg" aria-hidden="true"></div>
      <span class="seal seal-mark prologue-seal">慢</span>
      <p class="prologue-verse">「{{ dailyVerse }}」</p>
      <h2 class="prologue-title">卷首语 · 一座可慢下来的江湖藏阁</h2>
      <p class="prologue-text">
        离恨烟·烟雨阁，是一座以水墨为骨、以闲情为魂的江湖藏阁。在这里，检索是「拾遗」，阅读是「展卷」，小憩是「听雨」——
        慢一点，再慢一点，方见烟雨本色。
      </p>
      <div class="prologue-cta">
        <button type="button" class="btn primary" @click="scrollToContent">入阁拾遗</button>
        <button type="button" class="btn ghost" @click="goGame">伞下听雨</button>
      </div>
      <!-- 数字速览（真实统计：藏品 / 分类 / 听雨人次） -->
      <div class="prologue-stats">
        <div class="pstat">
          <b>{{ stats.itemCount }}</b>
          <span>阁中藏品</span>
        </div>
        <div class="pstat">
          <b>{{ stats.categoryCount }}</b>
          <span>江湖门类</span>
        </div>
        <div class="pstat">
          <b>{{ stats.listenRain }}</b>
          <span>听雨人次</span>
        </div>
      </div>
    </section>

    <!-- ============ 阁中珍藏（精选条目） ============ -->
    <section v-if="featured.length" class="treasure reveal">
      <div class="treasure-head">
        <h2 class="treasure-title">阁中珍藏</h2>
        <span class="treasure-sub">馆主私藏 · 雨过天青时最宜展读</span>
      </div>
      <div class="treasure-strip">
        <article
          v-for="(item, i) in featured"
          :key="'feat|' + item.id"
          class="hcard card-pop"
          :style="{ animationDelay: `${Math.min(i, 8) * 70}ms` }"
          @click="goDetail(item.id)"
          @mousemove="cardSpot"
        >
          <span class="hcard-corner tl"></span>
          <span class="hcard-corner tr"></span>
          <span class="hcard-corner bl"></span>
          <span class="hcard-corner br"></span>
          <InkMotif :cat="item.category" class="hcard-motif" />
          <div class="hcard-ic">{{ item.ic }}</div>
          <h3 class="hcard-title">{{ item.title }}</h3>
          <p class="hcard-desc">{{ item.desc }}</p>
          <button type="button" class="hcard-link">展卷 ›</button>
        </article>
      </div>
    </section>

    <!-- ============ 江湖诗笺（独立板块 · 可换一句） ============ -->
    <section class="poem reveal">
      <div class="poem-card">
        <span class="seal seal-mark poem-seal">笺</span>
        <p class="poem-label">江 湖 诗 笺</p>
        <p class="poem-text">「{{ poemBook[poemIndex].t }}」</p>
        <p class="poem-src">{{ poemBook[poemIndex].s }}</p>
        <button type="button" class="btn ghost poem-btn" @click="changePoem">换 一 句 ↻</button>
      </div>
    </section>

    <!-- ============ 导航栏（分类切换） ============ -->
    <nav ref="contentRef" class="home-nav reveal">
      <button
        v-for="item in navItems"
        :key="item.key"
        type="button"
        class="nav-tab"
        :class="{ active: activeTopCode === item.key }"
        @click="pickCat(item.key)"
      >
        {{ item.label }}
      </button>
    </nav>

    <!-- ============ 子分类快捷筛选（分级分类） ============ -->
    <div v-if="subCats.length" class="sub-cats">
      <span class="hot-label">细览：</span>
      <button
        v-for="c in subCats"
        :key="c.code"
        type="button"
        class="hot-chip"
        :class="{ active: activeCat === c.code }"
        @click="pickCat(c.code)"
      >
        {{ c.name }}
      </button>
    </div>

    <!-- ============ 内容区 ============ -->
    <div class="home-body">
      <div class="result-head reveal">
        <h2 class="result-title">{{ resultTitle }}</h2>
        <span class="result-count">共 {{ results.length }} 项</span>
      </div>

      <div v-if="loading" class="result-tip">检索江湖中…</div>

      <div v-else-if="results.length" class="hgrid">
        <article
          v-for="(item, i) in results"
          :key="activeCat + '|' + keyword + '|' + item.id"
          class="hcard card-pop"
          :style="{ animationDelay: `${Math.min(i, 8) * 70}ms` }"
          @click="goDetail(item.id)"
          @mousemove="cardSpot"
        >
          <span class="hcard-corner tl"></span>
          <span class="hcard-corner tr"></span>
          <span class="hcard-corner bl"></span>
          <span class="hcard-corner br"></span>
          <InkMotif :cat="item.category" class="hcard-motif" />
          <div class="hcard-ic">{{ item.ic }}</div>
          <h3 class="hcard-title">{{ item.title }}</h3>
          <p class="hcard-desc">{{ item.desc }}</p>
          <button type="button" class="hcard-link">进入 ›</button>
        </article>
      </div>

      <div v-else class="empty reveal">
        <div class="empty-ic">🌫️</div>
        <p>烟雨朦胧，未寻得「{{ keyword || resultTitle }}」相关的内容。</p>
        <button type="button" class="btn ghost" @click="onClear">清 空 重 搜</button>
      </div>
    </div>

    <!-- ============ 伞下听雨（小游戏入口） ============ -->
    <section class="rain-entry reveal">
      <div class="rain-entry-inner">
        <div class="rain-entry-text">
          <span class="seal seal-mark rain-entry-seal">听</span>
          <h2 class="rain-entry-title">伞下听雨</h2>
          <p class="rain-entry-desc">
            撑一柄油纸伞，接住落下的雨珠、飞花与朱砂小印。无失败惩罚，雨势可调——
            一场治愈系的烟雨小憩，正合慢节奏。
          </p>
          <p class="rain-entry-best" v-if="gameBest > 0">最佳佳绩 · {{ gameBest }} 分</p>
          <p class="rain-entry-best" v-else>尚未拾雨，今番不妨一试</p>
        </div>
        <div class="rain-entry-art">
          <InkMotif cat="game" class="rain-entry-motif" />
        </div>
      </div>
      <button type="button" class="btn primary rain-entry-btn" @click="goGame">入 阁 听 雨</button>
    </section>
  </section>
</template>

<style scoped>
.home-page {
  position: relative;
  max-width: 1160px;
  margin: 0 auto;
  padding: 56px 42px 70px;
  min-height: calc(100vh - 130px);
}

/* 飘浮烟雾（呼应登录页） */
.home-mist {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.3;
  z-index: 0;
  pointer-events: none;
  animation: mist-drift 18s ease-in-out infinite alternate;
}
.home-mist.m1 {
  width: 460px;
  height: 460px;
  background: #a9cdc5;
  top: -140px;
  right: -120px;
}
.home-mist.m2 {
  width: 360px;
  height: 360px;
  background: #e6d9bd;
  bottom: -120px;
  left: -100px;
  animation-delay: -8s;
  opacity: 0.22;
}
@keyframes mist-drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(50px, 30px) scale(1.1); }
}

/* ============ 雨丝层 ============ */
.rain {
  position: absolute;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}
.rain .drop {
  position: absolute;
  top: -120px;
  width: 1.5px;
  background: linear-gradient(to bottom, transparent, rgba(78, 141, 134, 0.85));
  animation: drop-fall linear infinite;
}
@keyframes drop-fall {
  from { transform: rotate(9deg) translateY(-60px); }
  to { transform: rotate(9deg) translateY(110vh); }
}

/* ============ 搜索区 ============ */
.search-hero {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 18px 20px 30px;
}

/* 水墨横幅：置于文字之后（负 z 随本区堆叠上下文），边缘羽化融入纸面 */
.hero-banner {
  position: absolute;
  inset: -58px -44px 30px;
  z-index: -1;
  background: url('/ink-hero.png') center 36% / cover no-repeat;
  opacity: 0.52;
  border-radius: 10px;
  -webkit-mask-image: radial-gradient(115% 95% at 50% 34%, #000 38%, transparent 79%);
  mask-image: radial-gradient(115% 95% at 50% 34%, #000 38%, transparent 79%);
  will-change: transform;
  transition: transform 0.18s ease-out;
  pointer-events: none;
}

/* 印章落款：标题逐字落定后朱砂印盖下 */
.search-hero .hero-seal {
  top: 2px;
  left: auto;
  right: 6%;
  transform: rotate(8deg);
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  writing-mode: vertical-rl;
  font-size: 14px;
  letter-spacing: 2px;
  opacity: 0.92;
  animation: seal-stamp 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) 1.15s both;
}
@keyframes seal-stamp {
  from { opacity: 0; transform: scale(1.9) rotate(16deg); filter: blur(2px); }
  to { opacity: 0.92; transform: rotate(8deg) scale(1); filter: blur(0); }
}

.hero-title {
  font-size: 46px;
  font-weight: normal;
  letter-spacing: 14px;
  text-indent: 14px;
  margin-top: 22px;
}
.title-char {
  display: inline-block;
  background: linear-gradient(180deg, #2c423d 20%, #4e8d86 90%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: char-in 0.9s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}
@keyframes char-in {
  from { opacity: 0; transform: translateY(16px); filter: blur(5px); }
  to { opacity: 1; transform: none; filter: blur(0); }
}

.hero-verse {
  font-size: 15px;
  letter-spacing: 6px;
  color: var(--ink-soft);
  opacity: 0.75;
  margin: 12px 0 6px;
}
.hero-verse b {
  color: var(--cinnabar);
  font-weight: normal;
}

.hero-greet {
  font-size: 14px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  margin-bottom: 26px;
}

/* 搜索栏 */
.search-bar {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 620px;
  margin: 0 auto;
  padding: 8px 8px 8px 16px;
  background: linear-gradient(160deg, rgba(250, 248, 240, 0.96), rgba(240, 238, 228, 0.92));
  border: 1px solid rgba(201, 163, 92, 0.55);
  border-radius: 999px;
  box-shadow: 0 18px 44px rgba(30, 43, 40, 0.18), inset 0 0 30px rgba(201, 163, 92, 0.06);
}

.search-ico {
  display: flex;
  align-items: center;
  opacity: 0.6;
}
.search-ico svg {
  width: 20px;
  height: 20px;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 16px;
  letter-spacing: 2px;
  color: var(--ink);
  padding: 10px 4px;
}
.search-input::placeholder {
  color: rgba(61, 80, 75, 0.45);
  letter-spacing: 2px;
  font-size: 14px;
}

.search-clear {
  border: none;
  background: rgba(61, 80, 75, 0.12);
  color: var(--ink-soft);
  width: 26px;
  height: 26px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  flex-shrink: 0;
  transition: all 0.25s;
}
.search-clear:hover {
  background: rgba(179, 54, 43, 0.15);
  color: var(--cinnabar);
}

.search-btn {
  flex-shrink: 0;
  padding: 11px 28px;
  font-size: 15px;
  letter-spacing: 6px;
  text-indent: 6px;
  border-radius: 999px;
}

/* 热门词 */
.hot-words {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 10px;
}
.hot-label {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.7;
}
.hot-chip {
  border: 1px solid rgba(163, 127, 61, 0.5);
  background: rgba(250, 248, 240, 0.7);
  color: var(--gold-deep);
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 2px;
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.25s;
}
.hot-chip:hover {
  background: rgba(201, 163, 92, 0.16);
  border-color: var(--gold-deep);
  transform: translateY(-1px);
}
.hot-chip.active {
  color: #f8f2e4;
  background: linear-gradient(120deg, #28524b, #3d6f66);
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(40, 82, 75, 0.28);
}

/* 子分类快捷筛选（分级分类入口） */
.sub-cats {
  position: relative;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin: -16px auto 26px;
  animation: card-pop 0.45s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}

/* ============ 导航栏 ============ */
.home-nav {
  position: relative;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 6px;
  margin: 38px auto 30px;
  padding: 14px 18px;
  background: rgba(244, 241, 232, 0.62);
  border: 1px solid rgba(201, 163, 92, 0.3);
  border-radius: 6px;
  backdrop-filter: blur(8px);
}
.nav-tab {
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  letter-spacing: 4px;
  color: var(--ink-soft);
  padding: 8px 18px;
  border-radius: 999px;
  cursor: pointer;
  position: relative;
  transition: color 0.25s, background 0.25s;
}
.nav-tab::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--jade), var(--gold));
  transform: translateX(-50%);
  transition: width 0.3s;
}
.nav-tab:hover {
  color: var(--ink);
}
.nav-tab.active {
  color: #f8f2e4;
  background: linear-gradient(120deg, #28524b, #3d6f66);
  box-shadow: 0 8px 20px rgba(40, 82, 75, 0.3);
}
.nav-tab.active::after {
  width: 0;
}

/* ============ 内容区 ============ */
.home-body {
  position: relative;
  z-index: 2;
}
.result-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(201, 163, 92, 0.3);
}
.result-title {
  font-size: 26px;
  font-weight: normal;
  letter-spacing: 8px;
  text-indent: 8px;
}
.result-count {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.7;
}
.result-tip {
  text-align: center;
  padding: 60px 0;
  font-size: 15px;
  letter-spacing: 4px;
  color: var(--ink-soft);
  opacity: 0.7;
}

.hgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 22px;
}

.hcard {
  --mx: 50%;
  --my: 50%;
  position: relative;
  background: linear-gradient(160deg, rgba(250, 248, 240, 0.92), rgba(240, 238, 228, 0.85));
  border: 1px solid rgba(201, 163, 92, 0.45);
  border-radius: 6px;
  padding: 28px 24px 24px;
  transition: transform 0.35s, box-shadow 0.35s;
  overflow: hidden;
  cursor: pointer;
}
.hcard::before {
  content: '';
  position: absolute;
  inset: 6px;
  border: 1px solid rgba(201, 163, 92, 0.28);
  border-radius: 3px;
  pointer-events: none;
  transition: border-color 0.35s;
}
/* 聚光跟随：一层淡金光晕追着鼠标走 */
.hcard::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(230px circle at var(--mx) var(--my), rgba(201, 163, 92, 0.16), transparent 68%);
  opacity: 0;
  transition: opacity 0.35s;
  pointer-events: none;
}
.hcard:hover {
  transform: translateY(-6px);
  box-shadow: 0 22px 50px rgba(30, 43, 40, 0.18);
}
.hcard:hover::before {
  border-color: rgba(163, 127, 61, 0.55);
}
.hcard:hover::after {
  opacity: 1;
}

/* 水墨母题插画（按分类：伞/剑/徽章/卷宗/同游/机括/山月） */
.hcard-motif {
  position: absolute;
  right: -10px;
  bottom: -12px;
  width: 92px;
  height: 92px;
  color: var(--jade);
  opacity: 0.12;
  transform: rotate(-8deg);
  pointer-events: none;
  transition: opacity 0.35s, transform 0.35s;
}
.hcard:hover .hcard-motif {
  opacity: 0.22;
  transform: rotate(-4deg) scale(1.05);
}

.hcard-corner {
  position: absolute;
  width: 16px;
  height: 16px;
  border-color: var(--gold-deep);
  border-style: solid;
  z-index: 2;
  opacity: 0.8;
}
.hcard-corner.tl { top: -1px; left: -1px; border-width: 2px 0 0 2px; }
.hcard-corner.tr { top: -1px; right: -1px; border-width: 2px 2px 0 0; }
.hcard-corner.bl { bottom: -1px; left: -1px; border-width: 0 0 2px 2px; }
.hcard-corner.br { bottom: -1px; right: -1px; border-width: 0 2px 2px 0; }

.hcard-ic {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  border-radius: 50%;
  border: 1px solid rgba(201, 163, 92, 0.55);
  background: rgba(250, 248, 240, 0.8);
  margin-bottom: 16px;
}
.hcard-title {
  font-size: 19px;
  font-weight: normal;
  letter-spacing: 4px;
  margin-bottom: 9px;
}
.hcard-desc {
  font-size: 14px;
  line-height: 1.9;
  letter-spacing: 1px;
  color: var(--ink-soft);
  margin-bottom: 16px;
}
.hcard-link {
  border: none;
  background: transparent;
  color: var(--jade);
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 2px;
  cursor: pointer;
  padding: 0;
  transition: color 0.25s, letter-spacing 0.25s;
}
.hcard-link:hover {
  color: var(--cinnabar);
  letter-spacing: 4px;
}

/* 卡片进场（不依赖 .reveal，瀑布式错峰；带卷轴舒展感） */
.card-pop {
  animation: card-pop 0.5s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}
@keyframes card-pop {
  from { opacity: 0; transform: translateY(20px) scale(0.96); filter: blur(3px); }
  to { opacity: 1; transform: none; filter: blur(0); }
}

/* 空状态 */
.empty {
  text-align: center;
  padding: 70px 20px;
}
.empty-ic {
  font-size: 46px;
  margin-bottom: 16px;
  opacity: 0.7;
}
.empty p {
  font-size: 15px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  margin-bottom: 22px;
}

/* ============ 卷首语（主旨呈现） ============ */
.prologue {
  position: relative;
  z-index: 2;
  max-width: 760px;
  margin: 8px auto 18px;
  padding: 36px 40px 38px;
  text-align: center;
  background: linear-gradient(160deg, rgba(250, 248, 240, 0.82), rgba(240, 238, 228, 0.7));
  border: 1px solid rgba(201, 163, 92, 0.4);
  border-radius: 8px;
  backdrop-filter: blur(6px);
  overflow: hidden;
}
/* 内容层抬到背景图之上，保证文字清晰 */
.prologue > :not(.prologue-bg) {
  position: relative;
  z-index: 1;
}
/* 藏阁卷首水墨横幅：置于文字之后、纸面之上，边缘羽化 */
.prologue-bg {
  position: absolute;
  inset: -10% -6% 0;
  z-index: 0;
  background: url('/ink-prologue.png') center 30% / cover no-repeat;
  opacity: 0.26;
  -webkit-mask-image: radial-gradient(125% 120% at 50% 22%, #000 52%, transparent 86%);
  mask-image: radial-gradient(125% 120% at 50% 22%, #000 52%, transparent 86%);
  pointer-events: none;
}
.prologue .prologue-seal {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%) rotate(-4deg);
  width: 46px;
  height: 46px;
  font-size: 22px;
  letter-spacing: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: seal-stamp 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) 0.3s both;
}
.prologue-verse {
  font-size: 17px;
  letter-spacing: 5px;
  color: var(--cinnabar);
  opacity: 0.9;
  margin-bottom: 16px;
}
.prologue-title {
  font-size: 23px;
  font-weight: normal;
  letter-spacing: 7px;
  text-indent: 7px;
  margin-bottom: 14px;
}
.prologue-text {
  font-size: 15px;
  line-height: 2.1;
  letter-spacing: 2px;
  color: var(--ink-soft);
  margin-bottom: 26px;
}
.prologue-cta {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* 数字速览（真实统计：藏品 / 分类 / 听雨人次） */
.prologue-stats {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px dashed rgba(201, 163, 92, 0.35);
}
.prologue-stats .pstat {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 92px;
  padding: 6px 14px;
  border-radius: 6px;
  background: rgba(250, 248, 240, 0.5);
  border: 1px solid rgba(201, 163, 92, 0.22);
}
.prologue-stats .pstat b {
  font-size: 26px;
  font-weight: normal;
  letter-spacing: 1px;
  color: var(--jade);
  line-height: 1.2;
}
.prologue-stats .pstat span {
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.75;
  margin-top: 4px;
}

/* ============ 阁中珍藏（精选卷轴） ============ */
.treasure {
  position: relative;
  z-index: 2;
  margin: 14px 0 6px;
}
.treasure-head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(201, 163, 92, 0.3);
}
.treasure-title {
  font-size: 24px;
  font-weight: normal;
  letter-spacing: 7px;
  text-indent: 7px;
}
.treasure-sub {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.7;
}
.treasure-strip {
  display: flex;
  gap: 22px;
  overflow-x: auto;
  padding: 6px 2px 18px;
  scroll-snap-type: x mandatory;
}
.treasure-strip .hcard {
  flex: 0 0 260px;
  scroll-snap-align: start;
}
.treasure-strip::-webkit-scrollbar {
  height: 8px;
}
.treasure-strip::-webkit-scrollbar-thumb {
  background: rgba(163, 127, 61, 0.4);
  border-radius: 999px;
}
.treasure-strip::-webkit-scrollbar-track {
  background: transparent;
}

/* ============ 江湖诗笺（独立板块 · 可换一句） ============ */
.poem {
  position: relative;
  z-index: 2;
  margin: 6px 0 4px;
  display: flex;
  justify-content: center;
}
.poem-card {
  position: relative;
  width: 100%;
  max-width: 620px;
  padding: 30px 34px 32px;
  text-align: center;
  background: linear-gradient(160deg, rgba(250, 248, 240, 0.8), rgba(240, 238, 228, 0.68));
  border: 1px solid rgba(201, 163, 92, 0.4);
  border-radius: 8px;
  backdrop-filter: blur(6px);
}
.poem-card .poem-seal {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%) rotate(-5deg);
  width: 42px;
  height: 42px;
  font-size: 20px;
  letter-spacing: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: seal-stamp 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) 0.3s both;
}
.poem-label {
  font-size: 13px;
  letter-spacing: 8px;
  text-indent: 8px;
  color: var(--gold-deep);
  opacity: 0.8;
  margin-bottom: 14px;
}
.poem-text {
  font-size: 20px;
  letter-spacing: 4px;
  line-height: 1.9;
  color: var(--ink);
  margin-bottom: 10px;
  min-height: 38px;
  transition: opacity 0.35s, transform 0.35s;
}
.poem-src {
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  opacity: 0.7;
  margin-bottom: 22px;
}
.poem-btn {
  letter-spacing: 5px;
  text-indent: 5px;
  padding: 9px 26px;
  font-size: 14px;
  transition: all 0.25s;
}
.poem-btn:hover {
  letter-spacing: 7px;
  text-indent: 7px;
}

/* ============ 伞下听雨入口 ============ */
.rain-entry {
  position: relative;
  z-index: 2;
  margin: 40px 0 10px;
  padding: 34px 36px 36px;
  border: 1px solid rgba(201, 163, 92, 0.45);
  border-radius: 8px;
  background:
    radial-gradient(120% 140% at 88% 0%, rgba(78, 141, 134, 0.12), transparent 60%),
    linear-gradient(160deg, rgba(250, 248, 240, 0.9), rgba(238, 240, 230, 0.82));
  overflow: hidden;
}
.rain-entry-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
}
.rain-entry-text {
  flex: 1;
  min-width: 0;
}
.rain-entry-seal {
  position: absolute;
  top: 18px;
  right: 22px;
  width: 44px;
  height: 44px;
  font-size: 20px;
  letter-spacing: 0;
  transform: rotate(6deg);
  opacity: 0.9;
}
.rain-entry-title {
  font-size: 26px;
  font-weight: normal;
  letter-spacing: 8px;
  text-indent: 8px;
  margin-bottom: 12px;
}
.rain-entry-desc {
  font-size: 15px;
  line-height: 2;
  letter-spacing: 2px;
  color: var(--ink-soft);
  max-width: 520px;
  margin-bottom: 14px;
}
.rain-entry-best {
  font-size: 14px;
  letter-spacing: 3px;
  color: var(--gold-deep);
  margin-bottom: 20px;
}
.rain-entry-art {
  flex-shrink: 0;
  width: 120px;
  height: 120px;
  color: var(--jade);
  opacity: 0.5;
  transform: rotate(-6deg);
}
.rain-entry-btn {
  padding: 12px 36px;
  letter-spacing: 8px;
  text-indent: 8px;
}

/* ============ 响应式 ============ */
@media (max-width: 960px) {
  .home-page { padding: 40px 20px 60px; }
  .hero-title { font-size: 36px; letter-spacing: 10px; }
  .search-bar { flex-wrap: wrap; border-radius: 18px; padding: 12px; }
  .search-input { flex-basis: 100%; order: 2; }
  .search-ico { order: 1; }
  .search-clear { order: 3; }
  .search-btn { order: 4; width: 100%; }
  .hero-seal { right: 2%; }
  .hero-banner { opacity: 0.4; }
  .prologue { padding: 30px 22px 32px; }
  .prologue-stats { flex-wrap: wrap; gap: 10px; }
  .prologue-stats .pstat { min-width: 78px; padding: 6px 10px; }
  .prologue-stats .pstat b { font-size: 22px; }
  .poem-card { padding: 26px 20px 28px; }
  .poem-text { font-size: 18px; letter-spacing: 2px; }
  .rain-entry-inner { flex-direction: column; text-align: center; }
  .rain-entry-art { display: none; }
  .treasure-strip .hcard { flex-basis: 220px; }
}
</style>
