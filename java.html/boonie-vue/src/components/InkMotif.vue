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
  if (c.includes('bridge')) return 'bridge'
  if (c.includes('sun')) return 'sun'
  if (c.includes('mirror')) return 'mirror'
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

    <!-- 桥上看景（桥拱 + 楼台 + 桥上/楼上两个看景人） -->
    <g v-else-if="kind === 'bridge'">
      <path d="M6 40 Q32 20 58 40" />
      <path d="M6 40 H58" />
      <path d="M14 40 V33 M22 40 V34 M30 40 V32 M38 40 V34 M46 40 V33 M54 40 V34" opacity=".7" />
      <!-- 楼（右侧亭台，看你的人所在） -->
      <path d="M46 22 L53 17 L60 22 Z" />
      <path d="M47 22 V38 H59 V22" />
      <!-- 桥上小人（看风景的你） -->
      <circle cx="26" cy="29" r="2.2" />
      <path d="M26 31 V37 M26 32 L23 34 M26 32 L29 34" />
      <!-- 楼上小人（看风景的人） -->
      <circle cx="53" cy="29" r="2" opacity=".85" />
      <path d="M53 31 V37" opacity=".85" />
    </g>

    <!-- 岁晚风暖（暖日 + 岁晚细枝微叶） -->
    <g v-else-if="kind === 'sun'">
      <circle cx="32" cy="25" r="9" />
      <path d="M32 9 V13 M32 37 V41 M16 25 H20 M44 25 H48 M20 13 L23 16 M44 13 L41 16 M20 37 L23 34 M44 37 L41 34" opacity=".8" />
      <!-- 岁晚细枝 -->
      <path d="M22 52 Q32 47 46 49" opacity=".7" />
      <path d="M30 50 c-4 -1 -6 -4 -6 -7 c4 1 6 4 6 7 Z" fill="currentColor" opacity=".45" />
      <path d="M40 50 c4 -1 6 -4 6 -7 c-4 1 -6 4 -6 7 Z" fill="currentColor" opacity=".45" />
    </g>

    <!-- 镜花辞树（镜 + 枝头花 + 飘落花瓣，皆留不住） -->
    <g v-else-if="kind === 'mirror'">
      <!-- 镜（朱颜辞镜）-->
      <ellipse cx="22" cy="28" rx="11" ry="14" />
      <path d="M22 42 V47" />
      <path d="M15 53 L22 47 L29 53" opacity=".8" />
      <!-- 枝（花辞树）-->
      <path d="M58 12 Q50 20 47 33" opacity=".7" />
      <path d="M52 18 Q56 20 58 24" opacity=".6" />
      <!-- 枝头花 -->
      <circle cx="47" cy="13" r="2" fill="currentColor" />
      <ellipse cx="47" cy="8" rx="1.6" ry="2.4" fill="currentColor" opacity=".6" />
      <ellipse cx="52" cy="13" rx="2.4" ry="1.6" fill="currentColor" opacity=".6" />
      <ellipse cx="47" cy="18" rx="1.6" ry="2.4" fill="currentColor" opacity=".6" />
      <ellipse cx="42" cy="13" rx="2.4" ry="1.6" fill="currentColor" opacity=".6" />
      <!-- 飘落花瓣（留不住）-->
      <path d="M33 34 c-3 -1 -4 -4 -1 -6 c3 2 4 5 1 6 Z" fill="currentColor" opacity=".5" />
      <path d="M38 45 c-3 -1 -4 -4 -1 -6 c3 2 4 5 1 6 Z" fill="currentColor" opacity=".4" />
    </g>

    <!-- 默认：远山淡月 -->
    <g v-else>
      <path d="M6 50 L22 26 L32 40 L40 30 L58 50" />
      <circle cx="16" cy="15" r="5" opacity=".7" />
    </g>
  </svg>
</template>
