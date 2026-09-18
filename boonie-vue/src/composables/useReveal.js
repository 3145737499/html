import { onMounted, onUnmounted } from 'vue'

/**
 * 滚动进入视口的揭示动画（对应原项目的 .reveal / .in）
 * 用 IntersectionObserver 给带 [data-reveal] 的元素批量挂上 in 类
 */
export function useReveal(rootRef, { threshold = 0.15, stagger = 0.08 } = {}) {
  let io = null

  const observe = () => {
    const root = rootRef.value
    if (!root || typeof IntersectionObserver === 'undefined') return

    const targets = root.querySelectorAll('.reveal')
    io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          entry.target.classList.add('in')
          io.unobserve(entry.target)
        })
      },
      { threshold },
    )

    targets.forEach((el, i) => {
      el.style.transitionDelay = `${(i % 4) * stagger}s`
      io.observe(el)
    })
  }

  onMounted(() => {
    // 等 DOM 更新完成后再取节点
    requestAnimationFrame(observe)
  })

  onUnmounted(() => io?.disconnect())

  return { observe }
}
