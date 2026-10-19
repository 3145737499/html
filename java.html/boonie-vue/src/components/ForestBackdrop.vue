<script setup>
/**
 * 森林风的环境层：动态渐变背景 + 三团极光光斑
 * 原来写死在 body 里，现在抽成组件，只由森林风渲染
 */
</script>

<template>
  <div class="forest-bg" aria-hidden="true">
    <div class="bg-anim"></div>
    <div class="glow g1"></div>
    <div class="glow g2"></div>
    <div class="glow g3"></div>
  </div>
  <slot />
</template>

<style scoped>
.forest-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
