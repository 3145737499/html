<script setup>
/**
 * 条目详情页 · 离恨烟 · 烟雨阁
 * 路由 /item/:id（name: item，无需登录）
 * 数据：GET /api/item/{id}（正文 + 面包屑 + 同分类相关条目）
 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useReveal } from '@/composables/useReveal'
import { fetchItem } from '@/api/search'
import InkMotif from '@/components/InkMotif.vue'

const route = useRoute()
const router = useRouter()

const root = ref(null)
useReveal(root)

/* ---------------- 特效：细雨 / 卡片聚光（比首页更轻，不打扰阅读） ---------------- */
const rainEl = ref(null)
let drops = []
function buildRain() {
  if (!rainEl.value) return
  for (let i = 0; i < 14; i++) {
    const d = document.createElement('i')
    d.className = 'drop'
    d.style.cssText = [
      `left:${Math.random() * 100}%`,
      `height:${34 + Math.random() * 46}px`,
      `opacity:${0.1 + Math.random() * 0.2}`,
      `animation-duration:${1.3 + Math.random() * 1.6}s`,
      `animation-delay:${-Math.random() * 3}s`,
    ].join(';')
    rainEl.value.appendChild(d)
    drops.push(d)
  }
}
onMounted(buildRain)
onBeforeUnmount(() => {
  drops.forEach((d) => d.remove())
  drops = []
})

/* 卡片聚光：鼠标位置写入 CSS 变量 */
function cardSpot(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  el.style.setProperty('--mx', `${e.clientX - r.left}px`)
  el.style.setProperty('--my', `${e.clientY - r.top}px`)
}

const loading = ref(true)
const failed = ref(false)
const item = ref(null)

const crumbs = computed(() => {
  const d = item.value
  if (!d) return []
  const arr = []
  if (d.parentCategoryName) arr.push({ label: d.parentCategoryName, code: d.parentCategory })
  if (d.categoryName) arr.push({ label: d.categoryName, code: d.category })
  return arr
})

/** 详情正文按 \n 拆段（后端存真实换行；兼容字面 \n） */
const paragraphs = computed(() => {
  const t = item.value?.detail || item.value?.desc || ''
  return String(t)
    .replace(/\\n/g, '\n')
    .split(/\n+/)
    .map((s) => s.trim())
    .filter(Boolean)
})

async function load(id) {
  loading.value = true
  failed.value = false
  item.value = null
  const res = await fetchItem(id)
  loading.value = false
  if (res.success && res.data) {
    item.value = res.data
  } else {
    failed.value = true
  }
}

function goBack() {
  // 有历史回上一页，否则回首页
  if (window.history.length > 1) router.back()
  else router.push({ name: 'home' })
}

function goCategory(code) {
  if (code) router.push({ name: 'home' })
}

function goItem(id) {
  router.push({ name: 'item', params: { id } })
}

watch(
  () => route.params.id,
  (id) => {
    if (route.name === 'item' && id) load(id)
  },
  { immediate: true }
)
</script>

<template>
  <section ref="root" class="view detail-page active">
    <div class="home-mist m1"></div>
    <div class="home-mist m2"></div>

    <!-- 细雨层（比首页更疏，陪读不打扰） -->
    <div ref="rainEl" class="rain" aria-hidden="true"></div>

    <!-- 水墨横幅余韵（页首淡淡一层，向下羽化） -->
    <div class="detail-banner" aria-hidden="true"></div>

    <!-- ============ 顶部：返回 + 面包屑 ============ -->
    <div class="detail-top reveal">
      <button type="button" class="btn ghost back-btn" @click="goBack">‹ 返 回</button>
      <nav class="crumbs" v-if="crumbs.length">
        <button v-for="(c, i) in crumbs" :key="i" type="button" class="crumb" @click="goCategory(c.code)">
          {{ c.label }}
        </button>
        <span class="crumb-sep">/</span>
      </nav>
    </div>

    <!-- ============ 加载 / 失败 ============ -->
    <div v-if="loading" class="result-tip detail-tip">翻阅卷宗中…</div>

    <div v-else-if="failed" class="empty reveal">
      <div class="empty-ic">🌫️</div>
      <p>这一页卷宗被雨打湿，字迹难辨了。</p>
      <button type="button" class="btn ghost" @click="load(route.params.id)">重 新 翻 阅</button>
    </div>

    <!-- ============ 正文 ============ -->
    <article v-else-if="item" class="detail-sheet">
      <span class="hcard-corner tl"></span>
      <span class="hcard-corner tr"></span>
      <span class="hcard-corner bl"></span>
      <span class="hcard-corner br"></span>

      <header class="detail-head">
        <div class="detail-ic">{{ item.ic }}</div>
        <div class="detail-head-text">
          <h1 class="detail-title">{{ item.title }}</h1>
          <p class="detail-desc">{{ item.desc }}</p>
          <div class="detail-tags">
            <span v-if="item.categoryName" class="dtag">{{ item.parentCategoryName ? item.parentCategoryName + ' · ' : '' }}{{ item.categoryName }}</span>
            <span v-if="item.hot" class="dtag hot">热门</span>
            <span v-if="item.featured" class="dtag feat">精选</span>
          </div>
        </div>
      </header>

      <div class="detail-divider"><span>❖</span></div>

      <div class="detail-body">
        <p v-for="(p, i) in paragraphs" :key="i" class="detail-para">{{ p }}</p>
      </div>

      <footer class="detail-foot">
        <span class="seal-mark-s">离恨烟</span>
        <span class="foot-verse">烟水茫茫 · 离恨绵绵</span>
      </footer>
    </article>

    <!-- ============ 相关条目 ============ -->
    <div v-if="item && item.related && item.related.length" class="related-wrap">
      <div class="result-head reveal">
        <h2 class="result-title">同类细览</h2>
        <span class="result-count">共 {{ item.related.length }} 项</span>
      </div>
      <div class="hgrid">
        <article
          v-for="(r, i) in item.related"
          :key="r.id"
          class="hcard card-pop related-card"
          :style="{ animationDelay: `${Math.min(i, 6) * 70}ms` }"
          @click="goItem(r.id)"
          @mousemove="cardSpot"
        >
          <span class="hcard-corner tl"></span>
          <span class="hcard-corner tr"></span>
          <span class="hcard-corner bl"></span>
          <span class="hcard-corner br"></span>
          <InkMotif :cat="r.category" class="hcard-motif" />
          <div class="hcard-ic">{{ r.ic }}</div>
          <h3 class="hcard-title">{{ r.title }}</h3>
          <p class="hcard-desc">{{ r.desc }}</p>
          <button type="button" class="hcard-link">进入 ›</button>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.detail-page {
  position: relative;
  max-width: 880px;
  margin: 0 auto;
  padding: 40px 42px 70px;
  min-height: calc(100vh - 130px);
}

/* 飘浮烟雾（与首页一致） */
.home-mist {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.26;
  z-index: 0;
  pointer-events: none;
  animation: mist-drift 18s ease-in-out infinite alternate;
}
.home-mist.m1 {
  width: 420px;
  height: 420px;
  background: #a9cdc5;
  top: -160px;
  right: -140px;
}
.home-mist.m2 {
  width: 340px;
  height: 340px;
  background: #e6d9bd;
  bottom: -120px;
  left: -110px;
  animation-delay: -8s;
  opacity: 0.2;
}
@keyframes mist-drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(50px, 30px) scale(1.1); }
}

/* ============ 细雨层 ============ */
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
  background: linear-gradient(to bottom, transparent, rgba(78, 141, 134, 0.8));
  animation: drop-fall linear infinite;
}
@keyframes drop-fall {
  from { transform: rotate(9deg) translateY(-60px); }
  to { transform: rotate(9deg) translateY(110vh); }
}

/* ============ 水墨横幅余韵 ============ */
.detail-banner {
  position: absolute;
  top: -34px;
  left: -30px;
  right: -30px;
  height: 320px;
  z-index: 0;
  background: url('/ink-hero.webp') center 30% / cover no-repeat;
  opacity: 0.3;
  -webkit-mask-image: linear-gradient(180deg, #000 0%, transparent 100%);
  mask-image: linear-gradient(180deg, #000 0%, transparent 100%);
  pointer-events: none;
}

/* 顶部 */
.detail-top {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 28px;
}
.back-btn {
  padding: 9px 22px;
  font-size: 14px;
  letter-spacing: 3px;
  border-radius: 999px;
}
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
}
.crumb {
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  cursor: pointer;
  padding: 0;
  transition: color 0.25s;
}
.crumb:hover { color: var(--cinnabar); }
.crumb-sep { color: var(--ink-soft); opacity: 0.5; font-size: 12px; }

.detail-tip { padding: 90px 0; }

/* 正文卷宗 */
.detail-sheet {
  position: relative;
  z-index: 2;
  background: linear-gradient(160deg, rgba(250, 248, 240, 0.94), rgba(240, 238, 228, 0.88));
  border: 1px solid rgba(201, 163, 92, 0.45);
  border-radius: 6px;
  padding: 44px 48px 34px;
  box-shadow: 0 22px 50px rgba(30, 43, 40, 0.14);
}
.detail-sheet::before {
  content: '';
  position: absolute;
  inset: 6px;
  border: 1px solid rgba(201, 163, 92, 0.28);
  border-radius: 3px;
  pointer-events: none;
}
/* 卷宗展开：自上而下摊开，像展开一轴手卷 */
.detail-sheet {
  animation: sheet-unfurl 0.9s cubic-bezier(0.22, 0.8, 0.3, 1) both;
  transform-origin: top center;
}
@keyframes sheet-unfurl {
  from { opacity: 0; transform: scaleY(0.1) translateY(-26px); filter: blur(3px); }
  55% { opacity: 1; }
  to { opacity: 1; transform: none; filter: blur(0); }
}
/* 伞影水印（右上角淡青，呼应站内油纸伞意象） */
.detail-sheet::after {
  content: '';
  position: absolute;
  top: 14px;
  right: 16px;
  width: 96px;
  height: 96px;
  background: url('/umbrella-ink.webp') center / 132% no-repeat;
  opacity: 0.13;
  transform: rotate(14deg);
  pointer-events: none;
}

/* 头部 */
.detail-head {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.detail-ic {
  width: 68px;
  height: 68px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  border-radius: 50%;
  border: 1px solid rgba(201, 163, 92, 0.55);
  background: rgba(250, 248, 240, 0.85);
  box-shadow: 0 8px 20px rgba(30, 43, 40, 0.08);
}
.detail-head-text { flex: 1; min-width: 0; }
.detail-title {
  font-size: 30px;
  font-weight: normal;
  letter-spacing: 8px;
  text-indent: 8px;
  margin-bottom: 10px;
  background: linear-gradient(180deg, #2c423d 20%, #4e8d86 90%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.detail-desc {
  font-size: 14.5px;
  line-height: 1.9;
  letter-spacing: 1.5px;
  color: var(--ink-soft);
  margin-bottom: 12px;
}
.detail-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.dtag {
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--gold-deep);
  border: 1px solid rgba(163, 127, 61, 0.45);
  background: rgba(201, 163, 92, 0.1);
  padding: 3px 12px;
  border-radius: 999px;
}
.dtag.hot { color: var(--cinnabar); border-color: rgba(179, 54, 43, 0.4); background: rgba(179, 54, 43, 0.06); }
.dtag.feat { color: var(--jade); border-color: rgba(78, 141, 134, 0.45); background: rgba(78, 141, 134, 0.08); }

/* 分隔 */
.detail-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 26px 0 8px;
  color: var(--gold-deep);
  font-size: 13px;
  opacity: 0.8;
}
.detail-divider::before,
.detail-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(201, 163, 92, 0.5), transparent);
}

/* 正文段落 */
.detail-para {
  font-size: 15.5px;
  line-height: 2.2;
  letter-spacing: 2px;
  text-align: justify;
  margin: 18px 0;
  color: var(--ink);
}
.detail-para:first-child::first-letter {
  font-size: 26px;
  color: var(--cinnabar);
}

/* 落款 */
.detail-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 34px;
  padding-top: 16px;
  border-top: 1px dashed rgba(201, 163, 92, 0.35);
}
.seal-mark-s {
  writing-mode: vertical-rl;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--cinnabar);
  border: 1px solid rgba(179, 54, 43, 0.5);
  padding: 6px 3px;
  border-radius: 2px;
  opacity: 0.85;
  /* 盖章动效：卷宗展开后朱砂印落下 */
  animation: seal-stamp 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) 0.85s both;
}
@keyframes seal-stamp {
  from { opacity: 0; transform: scale(1.9) rotate(9deg); filter: blur(2px); }
  to { opacity: 0.85; transform: scale(1) rotate(0); filter: blur(0); }
}
.foot-verse {
  font-size: 12px;
  letter-spacing: 4px;
  color: var(--ink-soft);
  opacity: 0.6;
}

/* 相关条目 */
.related-wrap {
  position: relative;
  z-index: 2;
  margin-top: 46px;
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
  font-size: 22px;
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
.hgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 20px;
}
.related-card { cursor: pointer; }
.card-pop {
  animation: card-pop 0.45s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}
@keyframes card-pop {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: none; }
}

/* 空状态（复用首页形态） */
.empty { text-align: center; padding: 80px 20px; position: relative; z-index: 2; }
.empty-ic { font-size: 46px; margin-bottom: 16px; opacity: 0.7; }
.empty p {
  font-size: 15px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  margin-bottom: 22px;
}

/* 卡片四角描边（复用首页） */
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
.hcard {
  --mx: 50%;
  --my: 50%;
  position: relative;
  background: linear-gradient(160deg, rgba(250, 248, 240, 0.92), rgba(240, 238, 228, 0.85));
  border: 1px solid rgba(201, 163, 92, 0.45);
  border-radius: 6px;
  padding: 26px 22px 22px;
  transition: transform 0.35s, box-shadow 0.35s;
  overflow: hidden;
}
/* 聚光跟随（与首页一致） */
.hcard::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(210px circle at var(--mx) var(--my), rgba(201, 163, 92, 0.15), transparent 68%);
  opacity: 0;
  transition: opacity 0.35s;
  pointer-events: none;
}
.hcard:hover {
  transform: translateY(-6px);
  box-shadow: 0 22px 50px rgba(30, 43, 40, 0.18);
}
.hcard:hover::after {
  opacity: 1;
}
/* 水墨母题插画（与首页一致） */
.hcard-motif {
  position: absolute;
  right: -10px;
  bottom: -12px;
  width: 88px;
  height: 88px;
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
.hcard-ic {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 23px;
  border-radius: 50%;
  border: 1px solid rgba(201, 163, 92, 0.55);
  background: rgba(250, 248, 240, 0.8);
  margin-bottom: 14px;
}
.hcard-title { font-size: 18px; font-weight: normal; letter-spacing: 4px; margin-bottom: 8px; }
.hcard-desc {
  font-size: 13.5px;
  line-height: 1.9;
  letter-spacing: 1px;
  color: var(--ink-soft);
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
}

/* 响应式 */
@media (max-width: 640px) {
  .detail-page { padding: 24px 16px 60px; }
  .detail-sheet { padding: 28px 22px 24px; }
  .detail-head { flex-direction: column; gap: 16px; }
  .detail-title { font-size: 24px; letter-spacing: 5px; text-indent: 5px; }
  .detail-para { font-size: 14.5px; letter-spacing: 1px; }
}
</style>
