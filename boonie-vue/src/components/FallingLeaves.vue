<script setup>
/**
 * 森林风专属的环境装饰：飘落树叶
 * 原来在脚本里用 document.body.appendChild 生成 14 片叶子，
 * 现在改为 v-for 渲染 + 随机参数
 */
const EMOJIS = ['🍃', '🌿', '🍂']

const leaves = Array.from({ length: 14 }, (_, i) => {
  const duration = 8 + Math.random() * 9
  return {
    id: i,
    emoji: EMOJIS[i % 3],
    style: {
      left: `${Math.random() * 100}vw`,
      animationDuration: `${duration}s`,
      animationDelay: `${-Math.random() * duration}s`,
      fontSize: `${14 + Math.random() * 12}px`,
      opacity: 0.4 + Math.random() * 0.4,
    },
  }
})
</script>

<template>
  <div class="leaf-layer" aria-hidden="true">
    <div v-for="leaf in leaves" :key="leaf.id" class="leaf" :style="leaf.style">
      {{ leaf.emoji }}
    </div>
  </div>
</template>

<style scoped>
/* 叶子层本身不拦截事件，叶子样式由各风格 css 提供 */
.leaf-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}
</style>
