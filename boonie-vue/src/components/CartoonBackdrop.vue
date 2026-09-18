<script setup>
/**
 * 卡通风的环境层：太阳 + 三朵飘动云
 */
const clouds = [
  { id: 1, style: { top: '80px', left: '6%' } },
  { id: 2, style: { top: '160px', right: '10%', animationDelay: '3s' } },
  { id: 3, style: { top: '300px', left: '18%', animationDelay: '6s' } },
]

const cloudEmojis = ['☁️', '☁️', '🌥️']
</script>

<template>
  <div class="cartoon-bg" aria-hidden="true">
    <div class="sun">☀️</div>
    <div
      v-for="(cloud, i) in clouds"
      :key="cloud.id"
      class="cloud"
      :style="cloud.style"
    >
      {{ cloudEmojis[i] }}
    </div>
  </div>
  <slot />
</template>

<style scoped>
.cartoon-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
