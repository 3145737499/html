<script setup>
/**
 * 落地页（首页）
 * 同一份模板 + 每种风格各自的文案，替代原来 4 个 HTML 里重复的 hero 区块
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { useThemeStore } from '@/stores/theme'
import { useReveal } from '@/composables/useReveal'

const router = useRouter()
const theme = useThemeStore()

const root = ref(null)
useReveal(root)

/** 四种风格各自的首页文案 */
const COPY = {
  forest: {
    badge: '🐻 BOONIE BEARS · 官方社区',
    titlePrefix: '欢迎来到 ',
    titleShine: '狗熊岭',
    titleSuffix: '森林里的奇妙世界',
    desc: '和熊大、熊二一起守护这片绿色家园。登录即可进入你的专属林区，记录每一次冒险，收集森林徽章。',
    primary: '🚪 进入林区',
    ghost: '✨ 先逛逛',
    stats: [
      { value: '1280', label: '森林伙伴' },
      { value: '365', label: '冒险天数' },
      { value: '99%', label: '快乐指数' },
    ],
    features: [
      { ic: '🌲', title: '认领树木', desc: '在狗熊岭认领属于你的小树，看着它和你一起成长。' },
      { ic: '🏅', title: '收集徽章', desc: '完成每日守护任务，解锁限定森林徽章与荣誉。' },
      { ic: '📖', title: '冒险日志', desc: '用文字与图片记录每一次与熊大熊二的奇遇。' },
      { ic: '👫', title: '伙伴社区', desc: '结识来自各地的森林伙伴，分享你的守护故事。' },
    ],
    sectionTitle: '在这里你能做什么',
    sectionSub: '专为森林伙伴打造的奇妙体验',
  },
  cartoon: {
    badge: '🐻 最可爱的森林社区',
    titlePrefix: '来 ',
    titleShine: '狗熊岭',
    titleSuffix: ' 一起玩吧！',
    desc: '和熊大、熊二蹦蹦跳跳，种小树、捡松果、收集亮晶晶的徽章，每天都超开心～',
    primary: '🚪 进去玩',
    ghost: '✨ 先看看',
    stats: [
      { value: '1280', label: '小伙伴' },
      { value: '365', label: '开心天数' },
      { value: '99%', label: '快乐值' },
    ],
    features: [
      { ic: '🌲', title: '我的小树', desc: '看看你种的小树长高了多少。' },
      { ic: '🏅', title: '亮晶晶徽章', desc: '已收集 12 枚，超厉害！' },
      { ic: '📖', title: '冒险日记', desc: '画下今天的森林奇遇。' },
      { ic: '⚙️', title: '小设置', desc: '改改名字和密码。' },
    ],
    sectionTitle: '乐园里有什么好玩的',
    sectionSub: '每天都想来的森林乐园',
  },
  neon: {
    badge: '⚡ BOONIE BEARS · NIGHT MODE',
    titlePrefix: '进入 ',
    titleShine: '狗熊岭',
    titleSuffix: ' 霓虹之夜',
    desc: '当森林点亮霓虹，和熊大熊二在光影里开启一场未来感冒险。',
    primary: '🚪 进入林区',
    ghost: '✨ 先逛逛',
    stats: [
      { value: '1280', label: '森林伙伴' },
      { value: '365', label: '冒险天数' },
      { value: '99%', label: '快乐指数' },
    ],
    features: [
      { ic: '🌲', title: '我的林区', desc: '查看认领树木与守护进度。' },
      { ic: '🏅', title: '森林徽章', desc: '已收集 12 枚，解锁更多。' },
      { ic: '📖', title: '冒险日志', desc: '记录与熊大熊二的奇遇。' },
      { ic: '⚙️', title: '账号设置', desc: '管理资料、密码与登录。' },
    ],
    sectionTitle: '霓虹森林的能力',
    sectionSub: '光影里的未来感社区',
  },
  minimal: {
    badge: 'BOONIE BEARS · 纯粹体验',
    titlePrefix: '欢迎来到 ',
    titleShine: '狗熊岭',
    titleSuffix: '',
    desc: '少即是多。一个安静、干净的森林社区，记录你与熊大熊二的每一段时光。',
    primary: '进入林区',
    ghost: '先逛逛',
    stats: [
      { value: '1280', label: '森林伙伴' },
      { value: '365', label: '冒险天数' },
      { value: '99%', label: '快乐指数' },
    ],
    features: [
      { ic: '🌲', title: '我的林区', desc: '查看认领树木与守护进度。' },
      { ic: '🏅', title: '森林徽章', desc: '已收集 12 枚，继续收集。' },
      { ic: '📖', title: '冒险日志', desc: '记录与熊大熊二的奇遇。' },
      { ic: '⚙️', title: '账号设置', desc: '管理资料、密码与登录。' },
    ],
    sectionTitle: '简洁的核心体验',
    sectionSub: '少即是多',
  },
}

const copy = computed(() => COPY[theme.style] ?? COPY.forest)

/** 英雄区鼠标聚光（原 index.html 的 .spot） */
const spot = ref({ left: '50%', top: '30%' })

function onHeroMove(e) {
  if (theme.style !== 'forest') return
  const rect = e.currentTarget.getBoundingClientRect()
  spot.value = { left: `${e.clientX - rect.left}px`, top: `${e.clientY - rect.top}px` }
}

onMounted(() => {
  // 保证从其它页返回时揭示动画正常重放
  root.value?.querySelectorAll('.reveal').forEach((el) => el.classList.remove('in'))
})
</script>

<template>
  <section ref="root" class="view landing active">
    <div class="hero" @mousemove="onHeroMove">
      <div v-if="theme.style === 'forest'" class="spot" :style="spot"></div>

      <div class="badge reveal">{{ copy.badge }}</div>

      <h1 class="reveal">
        {{ copy.titlePrefix }}<span class="shine">{{ copy.titleShine }}</span
        ><template v-if="copy.titleSuffix"><br v-if="theme.style === 'forest'" />{{ copy.titleSuffix }}</template>
      </h1>

      <p class="reveal">{{ copy.desc }}</p>

      <div class="cta reveal">
        <button class="btn primary" @click="router.push({ name: 'login' })">
          {{ copy.primary }}
        </button>
        <button class="btn ghost" @click="router.push({ name: 'home' })">
          {{ copy.ghost }}
        </button>
      </div>

      <div class="stats reveal">
        <div v-for="s in copy.stats" :key="s.label" class="stat">
          <b>{{ s.value }}</b><span>{{ s.label }}</span>
        </div>
      </div>
    </div>

    <div class="features">
      <h2 class="section-title reveal">{{ copy.sectionTitle }}</h2>
      <p class="section-sub reveal">{{ copy.sectionSub }}</p>
      <div class="fgrid">
        <div v-for="f in copy.features" :key="f.title" class="fcard reveal">
          <div class="ic">{{ f.ic }}</div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
