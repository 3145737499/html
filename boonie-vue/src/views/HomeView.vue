<script setup>
/**
 * 个人中心
 * 由 user store 驱动问候语，替代原来的 $('hi').textContent 直接改 DOM
 */
import { ref, computed } from 'vue'

import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useReveal } from '@/composables/useReveal'

const theme = useThemeStore()
const user = useUserStore()

const root = ref(null)
useReveal(root)

/** 各风格的问候副标题与卡片文案 */
const COPY = {
  forest: {
    sub: '今天也是守护狗熊岭的好日子 🌲',
    tiles: [
      { ic: '🌲', title: '我的林区', desc: '查看你认领的树木与守护进度。' },
      { ic: '🏅', title: '森林徽章', desc: '已收集 12 枚，继续冒险解锁更多。' },
      { ic: '📖', title: '冒险日志', desc: '记录每一次与熊大熊二的奇遇。' },
      { ic: '⚙️', title: '账号设置', desc: '管理资料、密码与登录方式。' },
    ],
  },
  cartoon: {
    sub: '今天也要开开心心哦 🌟',
    tiles: [
      { ic: '🌲', title: '我的小树', desc: '看看你种的小树长高了多少。' },
      { ic: '🏅', title: '亮晶晶徽章', desc: '已收集 12 枚，超厉害！' },
      { ic: '📖', title: '冒险日记', desc: '画下今天的森林奇遇。' },
      { ic: '⚙️', title: '小设置', desc: '改改名字和密码。' },
    ],
  },
  neon: {
    sub: '霓虹之夜，继续守护狗熊岭 🌃',
    tiles: [
      { ic: '🌲', title: '我的林区', desc: '查看认领树木与守护进度。' },
      { ic: '🏅', title: '森林徽章', desc: '已收集 12 枚，解锁更多。' },
      { ic: '📖', title: '冒险日志', desc: '记录与熊大熊二的奇遇。' },
      { ic: '⚙️', title: '账号设置', desc: '管理资料、密码与登录。' },
    ],
  },
  minimal: {
    sub: '今天也是美好的一天 🌿',
    tiles: [
      { ic: '🌲', title: '我的林区', desc: '查看认领树木与守护进度。' },
      { ic: '🏅', title: '森林徽章', desc: '已收集 12 枚，继续收集。' },
      { ic: '📖', title: '冒险日志', desc: '记录与熊大熊二的奇遇。' },
      { ic: '⚙️', title: '账号设置', desc: '管理资料、密码与登录。' },
    ],
  },
}

const copy = computed(() => COPY[theme.style] ?? COPY.forest)
const isForest = computed(() => theme.style === 'forest')

/** 每张卡片独立的倾斜效果：用 ref 数组收集 DOM */
const tileStyles = ref([])

function onTileMove(e, i) {
  if (!isForest.value) return
  const node = e.currentTarget
  const r = node.getBoundingClientRect()
  const x = (e.clientX - r.left) / r.width - 0.5
  const y = (e.clientY - r.top) / r.height - 0.5
  tileStyles.value[i] = { transform: `rotateY(${x * 6}deg) rotateX(${-y * 6}deg)` }
}

function onTileLeave(i) {
  tileStyles.value[i] = {}
}
</script>

<template>
  <section ref="root" class="view home active">
    <div class="home">
      <div class="welcome reveal">
        <div class="ava">🐻</div>
        <div>
          <h2>{{ user.greeting }}</h2>
          <p>{{ copy.sub }}</p>
        </div>
      </div>

      <div class="grid">
        <div
          v-for="(tile, i) in copy.tiles"
          :key="tile.title"
          class="tile reveal"
          :style="tileStyles[i]"
          @mousemove="onTileMove($event, i)"
          @mouseleave="onTileLeave(i)"
        >
          <div class="ic">{{ tile.ic }}</div>
          <h3>{{ tile.title }}</h3>
          <p>{{ tile.desc }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
