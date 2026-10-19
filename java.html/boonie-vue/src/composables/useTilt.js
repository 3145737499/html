import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 鼠标跟随的 3D 倾斜效果
 * 原项目用 el.style.transform 直接操作 DOM，这里改为响应式 style 对象
 *
 * @param {number} max 最大倾斜角度
 */
export function useTilt(max = 8) {
  const el = ref(null)
  const style = ref({})

  const onMove = (e) => {
    const node = el.value
    if (!node) return
    const r = node.getBoundingClientRect()
    const x = (e.clientX - r.left) / r.width - 0.5
    const y = (e.clientY - r.top) / r.height - 0.5
    style.value = {
      transform: `rotateY(${x * max}deg) rotateX(${-y * max}deg)`,
    }
  }

  const onLeave = () => {
    style.value = {}
  }

  onMounted(() => {
    el.value?.addEventListener('mousemove', onMove)
    el.value?.addEventListener('mouseleave', onLeave)
  })
  onUnmounted(() => {
    el.value?.removeEventListener('mousemove', onMove)
    el.value?.removeEventListener('mouseleave', onLeave)
  })

  return { el, style }
}
