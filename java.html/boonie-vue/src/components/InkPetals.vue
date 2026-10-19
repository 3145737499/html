<script setup>
/**
 * 飘落的花瓣 / 墨点（2026-10-14 升级）
 * 三种形态（花瓣/竹叶/墨点）× 三色（朱红/鎏金/青绿），
 * 双落径（直落 / 摇曳 S 形），近景清晰远景微糊，共 16 粒
 */
import { onBeforeUnmount, onMounted } from 'vue'

const COLORS = ['#b3362b', '#c9a35c', '#4e8d86', '#d98e85', '#8fbdb5']
let petals = []

onMounted(() => {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduced) return // base.css 也会兜底停掉动画，这里直接不生成

  for (let i = 0; i < 16; i++) {
    const p = document.createElement('div')
    p.className = 'petal'
    const size = 5 + Math.random() * 8
    const c = COLORS[Math.floor(Math.random() * COLORS.length)]
    const shape = i % 4 // 0/1 花瓣，2 竹叶，3 墨点
    const far = Math.random() < 0.35 // 远景：更小更糊更慢
    p.style.cssText = [
      `left:${Math.random() * 100}vw`,
      `width:${size}px`,
      `height:${shape === 2 ? size * 0.45 : size * (shape === 3 ? 1 : 0.7)}px`,
      `background:${c}`,
      `border-radius:${shape === 2 ? '0 60% 0 60%' : shape === 3 ? '50%' : '60% 40% 60% 40%'}`,
      `opacity:${(far ? 0.18 : 0.35) + Math.random() * 0.35}`,
      `filter:${far ? 'blur(1.2px)' : 'none'}`,
      `animation-name:${far ? 'petal-fall' : Math.random() < 0.5 ? 'petal-fall' : 'petal-fall-sway'}`,
      `animation-duration:${(far ? 12 : 8) + Math.random() * 10}s`,
      `animation-delay:${-Math.random() * 14}s`,
    ].join(';')
    document.body.appendChild(p)
    petals.push(p)
  }
})

onBeforeUnmount(() => {
  petals.forEach((p) => p.remove())
  petals = []
})
</script>

<template></template>

<!-- 全局样式：花瓣追加在 body 上，scoped 样式匹配不到，必须用全局块 -->
<style>
@keyframes petal-fall-sway {
  0% {
    transform: translate(0, -30px) rotate(0deg);
  }
  25% {
    transform: translate(46px, 27vh) rotate(150deg);
  }
  50% {
    transform: translate(-34px, 54vh) rotate(300deg);
  }
  75% {
    transform: translate(52px, 82vh) rotate(440deg);
  }
  100% {
    transform: translate(-70px, 110vh) rotate(580deg);
  }
}
</style>
