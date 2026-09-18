/**
 * 庆祝特效：彩带 / emoji 迸发
 * 原来每个 HTML 各写一份，这里统一成命令式工具函数
 */

const FOREST_COLORS = ['#d4af37', '#3fb27f', '#f0d98a', '#ff9a9a', '#7fd1a6']
const NEON_COLORS = ['#00f0ff', '#ff2bd6', '#7a5cff', '#7af9ff', '#ff8cf0']
const MINIMAL_COLORS = ['#2f6b3f', '#3f8a52', '#9ccc65', '#cddc39', '#aed581']
const CARTOON_ICONS = ['🐾', '🌟', '🍃', '🐻', '⭐']

/** 彩带下落 */
export function confetti(style = 'forest', count = 40) {
  const palette =
    style === 'neon' ? NEON_COLORS : style === 'minimal' ? MINIMAL_COLORS : FOREST_COLORS

  for (let i = 0; i < count; i++) {
    const el = document.createElement('div')
    el.className = 'confetti-piece'
    el.style.left = `${Math.random() * 100}vw`
    el.style.background = palette[i % palette.length]
    el.style.animation = `fallc ${1.2 + Math.random() * 1.2}s ease-in forwards`
    el.style.animationDelay = `${Math.random() * 0.3}s`
    document.body.appendChild(el)
    setTimeout(() => el.remove(), 2800)
  }
}

/** 卡通风的 emoji 向四周迸发 */
export function burst(count = 24) {
  for (let i = 0; i < count; i++) {
    const el = document.createElement('div')
    el.className = 'burst'
    el.textContent = CARTOON_ICONS[i % CARTOON_ICONS.length]
    const angle = Math.random() * Math.PI * 2
    const dist = 80 + Math.random() * 160
    el.style.setProperty('--x', `${Math.cos(angle) * dist}px`)
    el.style.setProperty('--y', `${Math.sin(angle) * dist}px`)
    document.body.appendChild(el)
    setTimeout(() => el.remove(), 900)
  }
}

/** 按风格自动挑选庆祝方式 */
export function celebrate(style = 'forest') {
  if (style === 'cartoon') burst()
  else confetti(style)
}
