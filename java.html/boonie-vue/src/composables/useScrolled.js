import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 导航栏滚动收缩（原 index.html 的 scrolled 效果）
 */
export function useScrolled(threshold = 20) {
  const scrolled = ref(false)
  const onScroll = () => {
    scrolled.value = window.scrollY > threshold
  }

  onMounted(() => {
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
  })
  onUnmounted(() => window.removeEventListener('scroll', onScroll))

  return scrolled
}
