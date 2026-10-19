<script setup>
/**
 * InkMotif · 分类水墨笔意小插画
 * 按分类编码渲染对应的水墨线描母题（伞/剑/徽章/卷宗/同游/机括/山月），
 * 纯 stroke + currentColor，颜色与浓度由使用方 CSS 控制。
 */
import { computed } from 'vue'

const props = defineProps({
  cat: { type: String, default: '' },
})

const kind = computed(() => {
  const c = String(props.cat || '')
  if (c.includes('umbrella')) return 'umbrella'
  if (c.includes('badge')) return 'badge'
  if (c.includes('log')) return 'log'
  if (c.includes('friend')) return 'friend'
  if (c.includes('setting')) return 'gear'
  if (c.includes('game')) return 'rain'
  if (c.includes('jianghu') || c.includes('dynamic') || c.includes('sect')) return 'sword'
  return 'mountain'
})
</script>

<template>
  <svg
    class="ink-motif"
    viewBox="0 0 64 64"
    fill="none"
    stroke="currentColor"
    stroke-width="2.2"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <!-- 油纸伞 -->
    <g v-if="kind === 'umbrella'">
      <path d="M6 32 C8 18 19 8 32 8 C45 8 56 18 58 32" />
      <path d="M6 32 Q12 37 19 32 Q26 37 32 32 Q38 37 45 32 Q52 37 58 32" />
      <path d="M32 8 L15 29 M32 8 L49 29" opacity=".55" />
      <path d="M32 32 V52 Q32 58 26 58" />
      <path d="M32 4 V8" />
    </g>

    <!-- 长剑 -->
    <g v-else-if="kind === 'sword'">
      <path d="M32 6 L37 38 L32 44 L27 38 Z" />
      <path d="M22 44 H42" />
      <path d="M32 44 V54" />
      <circle cx="32" cy="57" r="2.2" />
    </g>

    <!-- 江湖徽章 -->
    <g v-else-if="kind === 'badge'">
      <circle cx="32" cy="25" r="13" />
      <path d="M32 18.5 L34.2 23.4 L39.6 24 L35.6 27.6 L36.7 32.9 L32 30.2 L27.3 32.9 L28.4 27.6 L24.4 24 L29.8 23.4 Z" opacity=".8" />
      <path d="M25 36 L20 54 L32 47 L44 54 L39 36" />
    </g>

    <!-- 行侠日志（卷宗） -->
    <g v-else-if="kind === 'log'">
      <rect x="16" y="13" width="32" height="38" rx="2" />
      <path d="M24 13 V51" opacity=".6" />
      <path d="M31 24 H42 M31 32 H42 M31 40 H38" opacity=".8" />
    </g>

    <!-- 侠客同游 -->
    <g v-else-if="kind === 'friend'">
      <circle cx="24" cy="20" r="6" />
      <path d="M13 44 Q24 30 35 44" />
      <circle cx="42" cy="24" r="5" opacity=".7" />
      <path d="M34 44 Q42 33 50 44" opacity=".7" />
    </g>

    <!-- 账号设置（机括） -->
    <g v-else-if="kind === 'gear'">
      <circle cx="32" cy="32" r="9" />
      <path d="M32 16 V22 M32 42 V48 M16 32 H22 M42 32 H48 M21 21 L25.2 25.2 M42.8 21 L38.6 25.2 M21 43 L25.2 38.8 M42.8 43 L38.6 38.8" />
    </g>

    <!-- 听雨（山月雨意） -->
    <g v-else-if="kind === 'rain'">
      <path d="M6 48 L22 26 L32 40 L40 30 L58 48" />
      <circle cx="48" cy="15" r="5" opacity=".7" />
      <path d="M14 54 L14 57 M26 54 L26 58 M38 54 L38 57 M50 54 L50 58" opacity=".6" />
    </g>

    <!-- 默认：远山淡月 -->
    <g v-else>
      <path d="M6 50 L22 26 L32 40 L40 30 L58 50" />
      <circle cx="16" cy="15" r="5" opacity=".7" />
    </g>
  </svg>
</template>
