<script setup>
/**
 * 风格切换器：在 4 种视觉风格之间跳转
 * 原项目是靠打开不同 HTML 文件实现的，这里收敛成一个组件
 */
import { useRouter } from 'vue-router'
import { useThemeStore, STYLES } from '@/stores/theme'

const router = useRouter()
const theme = useThemeStore()

function pick(style) {
  router.push(style.path)
}
</script>

<template>
  <div class="style-switcher">
    <span class="label">风格</span>
    <button
      v-for="s in STYLES"
      :key="s.id"
      class="style-btn"
      :class="{ active: theme.style === s.id }"
      :title="s.name"
      @click="pick(s)"
    >
      <span class="emoji">{{ s.emoji }}</span>
      <span class="name">{{ s.name }}</span>
    </button>
  </div>
</template>

<style scoped>
.style-switcher {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  padding: 10px 16px;
  position: relative;
  z-index: 25;
}

.label {
  font-size: 12px;
  letter-spacing: 2px;
  opacity: 0.6;
  margin-right: 2px;
}

.style-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 12px;
  cursor: pointer;
  background: transparent;
  color: inherit;
  border: 1px solid currentColor;
  opacity: 0.55;
  transition: 0.18s;
}

.style-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.style-btn.active {
  opacity: 1;
  font-weight: 700;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
}

@media (max-width: 640px) {
  .style-btn .name {
    display: none;
  }
}
</style>
