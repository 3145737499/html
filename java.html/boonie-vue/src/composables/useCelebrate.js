/**
 * 庆祝特效：水墨花瓣彩带
 * 朱砂 / 鎏金 / 青绿三色
 */

const INK_COLORS = ['#b3362b', '#c9a35c', '#4e8d86', '#e6d9bd', '#8fbdb5']

/** 彩带下落 */
export function confetti(count = 40) {
  for (let i = 0; i < count; i++) {
    const el = document.createElement('div')
    el.className = 'confetti-piece'
    el.style.left = `${Math.random() * 100}vw`
    el.style.background = INK_COLORS[i % INK_COLORS.length]
    el.style.animation = `fallc ${1.2 + Math.random() * 1.2}s ease-in forwards`
    el.style.animationDelay = `${Math.random() * 0.3}s`
    document.body.appendChild(el)
    setTimeout(() => el.remove(), 2800)
  }
}

/** 按风格自动挑选庆祝方式（离恨烟仅有水墨一种） */
export function celebrate() {
  confetti()
}
