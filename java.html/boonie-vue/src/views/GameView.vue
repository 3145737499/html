<script setup>
/**
 * 伞下听雨 · 离恨烟（休闲小游戏 · 精修版）
 * 玩法：撑一把油纸伞，接住烟雨中落下的雨珠、飞花、竹叶与朱砂小印。
 *   - 悠然听雨：慢节奏、无失败惩罚，纯放松
 *   - 疾雨挑战：雨势更急，考验连韵（连击）
 * 雨势 1~5 档可调（细雨 → 骤雨），任何时候都可以随时切换。
 * 收伞后可览「听雨卷宗」：本局物候、最长连韵、听雨时长与诗意评级。
 * 风格：沿用 ink.css 水墨设计系统（青绿山水 × 墨色 × 朱砂 × 金线）
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { recordRain } from '@/api/search'

const router = useRouter()

/* ---------------- 状态 ---------------- */
const cvs = ref(null)
const wrap = ref(null)

const started = ref(false)
const paused = ref(false)
const ended = ref(false) // 收伞结算态
const mode = ref('serene') // serene 悠然 | storm 疾雨
const rainLevel = ref(2) // 1 细雨 ~ 5 骤雨

const hud = reactive({
  score: 0,
  combo: 0,
  maxCombo: 0,
  caught: 0,
})

// 本局物候统计
const stats = reactive({ rain: 0, petal: 0, leaf: 0, seal: 0, missed: 0 })

// 得分滚动显示
const displayScore = ref(0)
// 听雨时长（秒）
const elapsed = ref(0)
// 开局前最佳（用于判定是否破纪录）
const prevBest = ref(0)

const BEST_KEY = 'boonie-rain-best'
const best = reactive({ serene: 0, storm: 0 })

const modeName = computed(() => (mode.value === 'storm' ? '疾雨挑战' : '悠然听雨'))

// 诗意评级
function rankOf(score, m) {
  const storm = m === 'storm'
  if (score >= (storm ? 700 : 600)) return { title: '听雨入化', verse: '一伞收尽江湖雨，万籁俱寂是清欢。' }
  if (score >= (storm ? 460 : 400)) return { title: '风雅听雨', verse: '飞花满袖珠成串，心有烟霞不须归。' }
  if (score >= (storm ? 280 : 250)) return { title: '清雅自在', verse: '疏雨檐头三两声，闲听落花意自平。' }
  if (score >= (storm ? 140 : 120)) return { title: '初得闲趣', verse: '偶接飞花成一笑，始知听雨亦关情。' }
  if (score >= 50) return { title: '初听檐声', verse: '才撑油纸试听雨，已有清音落耳边。' }
  return { title: '方入烟雨', verse: '烟雨初逢伞半开，且听且习莫心裁。' }
}
const rank = computed(() => rankOf(hud.score, mode.value))
const newRecord = computed(() => hud.score > 0 && hud.score > prevBest.value)

// 雅集徽章
const badges = computed(() => {
  const b = []
  if (stats.petal >= 15) b.push('飞花满袖')
  if (hud.maxCombo >= 15) b.push('连韵不绝')
  if (stats.seal >= 3) b.push('朱砂有缘')
  if (stats.missed === 0 && hud.caught >= 20) b.push('一尘不染')
  if (newRecord.value) b.push('刷新佳绩')
  return b
})

const timeText = computed(() => {
  const s = Math.floor(elapsed.value)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
})

// 物候点数图例
const legend = [
  { key: 'rain', label: '雨珠', pts: 1, color: '#4e8d86' },
  { key: 'petal', label: '飞花', pts: 3, color: '#c9a35c' },
  { key: 'leaf', label: '竹叶', pts: 2, color: '#3d6f66' },
  { key: 'seal', label: '朱砂印', pts: 8, color: '#b3362b' },
]

/* ---------------- 画布与实体 ---------------- */
let ctx = null
let W = 0
let H = 0
let raf = 0
let lastT = 0
let spawnAcc = 0
let idleAcc = 0
let reducedMotion = false

/** 雨滴 / 落物 */
const drops = []
/** 接住时的水波涟漪 */
const ripples = []
/** 落水的小涟漪 */
const wisps = []
/** 墨点飞溅 */
const splashes = []
/** 飘浮的 +分 文字 */
const floats = []
/** 连韵 / 百珠 诗句浮笺 */
const toasts = []
/** 伞 */
const umb = { x: 0, targetX: 0, hw: 80, ch: 34, tilt: 0 }
/** 烟雾团 */
const mists = []
/** 流萤 */
const flies = []

const WATER_Y_RATIO = 0.9

/* ---------------- 工具 ---------------- */
const rand = (a, b) => a + Math.random() * (b - a)
const clamp = (v, a, b) => Math.min(b, Math.max(a, v))
const POINTS = { rain: 1, petal: 3, leaf: 2, seal: 8 }
const HUE = { rain: '#4e8d86', petal: '#c9a35c', leaf: '#3d6f66', seal: '#b3362b' }

let lastComboMilestone = 0
let lastScoreMilestone = 0

/* ---------------- 尺寸 ---------------- */
let ro = null

function resize() {
  const el = wrap.value
  const c = cvs.value
  if (!el || !c) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  W = el.clientWidth
  H = el.clientHeight
  c.width = Math.round(W * dpr)
  c.height = Math.round(H * dpr)
  c.style.width = W + 'px'
  c.style.height = H + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  // 伞随画布宽度伸缩，保证各屏都好接
  umb.hw = clamp(W * 0.09, 64, 110)
  umb.ch = umb.hw * 0.42
  umb.x = clamp(umb.x || W / 2, umb.hw, W - umb.hw)
  umb.targetX = clamp(umb.targetX || W / 2, umb.hw, W - umb.hw)
}

/* ---------------- 生成雨滴 ---------------- */
function pickType() {
  const r = Math.random()
  if (r < 0.04) return 'seal'
  if (r < 0.26) return 'petal'
  if (r < 0.38) return 'leaf'
  return 'rain'
}

function spawnDrop(forceType) {
  const lv = rainLevel.value
  const stormy = mode.value === 'storm'
  const type = forceType || pickType()
  const baseV = 105 + (lv - 1) * 42 + rand(-14, 22)
  drops.push({
    type,
    x: rand(14, W - 14),
    y: -18,
    vy: baseV * (stormy ? 1.55 : 1),
    vx: stormy ? rand(-46, 46) : rand(-14, 14),
    sway: rand(0, Math.PI * 2),
    swaySpd: rand(1.4, 2.6),
    size: type === 'seal' ? 13 : type === 'petal' ? 9 : type === 'leaf' ? 8 : rand(3.4, 4.6),
    rot: rand(0, Math.PI * 2),
    rotSpd: rand(-2.2, 2.2),
    caught: false,
  })
}

/* ---------------- 接住 / 落水 ---------------- */
function onCatch(d) {
  hud.combo += 1
  hud.maxCombo = Math.max(hud.maxCombo, hud.combo)
  hud.caught += 1
  stats[d.type] += 1
  const base = POINTS[d.type]
  const pts = base + Math.floor(hud.combo / 5)
  hud.score += pts
  ripples.push({ x: d.x, y: umbY(), r: 6, alpha: 0.85, hue: HUE[d.type] })
  floats.push({ x: d.x, y: umbY() - 16, text: '+' + pts, alpha: 1, vy: -34 })
  // 墨点飞溅
  const n = 4 + Math.floor(Math.random() * 3)
  for (let i = 0; i < n; i++) {
    const a = rand(-Math.PI, 0)
    const sp = rand(40, 120)
    splashes.push({
      x: d.x,
      y: umbY() - 4,
      vx: Math.cos(a) * sp,
      vy: Math.sin(a) * sp,
      life: rand(0.35, 0.6),
      size: rand(1.2, 2.6),
      color: HUE[d.type],
    })
  }
  chime(d.type)

  // 连韵 / 百珠 浮笺
  if (hud.combo > 0 && Math.floor(hud.combo / 10) > lastComboMilestone) {
    lastComboMilestone = Math.floor(hud.combo / 10)
    pushToast('连韵 ' + hud.combo + ' 重')
  }
  const ns = Math.floor(hud.score / 100)
  if (ns > lastScoreMilestone) {
    lastScoreMilestone = ns
    pushToast('听雨 ' + hud.score + ' 珠')
  }

  if (hud.score > best[mode.value]) {
    best[mode.value] = hud.score
    persistBest()
  }
}

function onMiss(d) {
  stats.missed += 1
  wisps.push({ x: d.x, y: waterY(), r: 2, alpha: 0.5 })
  if (hud.combo >= 5) {
    floats.push({ x: d.x, y: waterY() - 12, text: '韵断', alpha: 0.9, vy: -26, grey: true })
  }
  hud.combo = 0
  lastComboMilestone = 0
}

function pushToast(text) {
  toasts.push({ text, life: 1.7 })
  if (toasts.length > 3) toasts.shift()
}

const umbY = () => H * WATER_Y_RATIO - 74
const waterY = () => H * WATER_Y_RATIO

/* ---------------- 更新 ---------------- */
function update(dt) {
  const stormy = mode.value === 'storm'
  const lv = rainLevel.value
  elapsed.value += dt

  // 伞移动（跟随目标点，缓动）
  const k = 1 - Math.exp(-dt * 9)
  const prevX = umb.x
  umb.x += (umb.targetX - umb.x) * k
  umb.tilt = clamp((umb.x - prevX) / (dt || 0.016) * 0.0009, -0.32, 0.32)
  if (keys.has('left')) umb.targetX -= 460 * dt
  if (keys.has('right')) umb.targetX += 460 * dt
  umb.targetX = clamp(umb.targetX, umb.hw, W - umb.hw)

  // 生成
  const interval = (1150 - (lv - 1) * 185) / (stormy ? 2.1 : 1)
  spawnAcc += dt * 1000
  if (spawnAcc >= interval) {
    spawnAcc = 0
    spawnDrop()
  }

  const uy = umbY()

  // 雨滴
  for (let i = drops.length - 1; i >= 0; i--) {
    const d = drops[i]
    d.sway += d.swaySpd * dt
    d.rot += d.rotSpd * dt
    d.y += d.vy * dt
    d.x += (d.vx + Math.sin(d.sway) * (d.type === 'rain' ? 8 : 26)) * dt

    // 越界左右轻反弹
    if (d.x < 10 || d.x > W - 10) d.vx *= -1

    // 接住判定：越过伞面高度且在伞面横向范围内
    if (d.y >= uy && d.y <= uy + 26 && Math.abs(d.x - umb.x) <= umb.hw * 0.97) {
      onCatch(d)
      drops.splice(i, 1)
      continue
    }
    // 落水
    if (d.y >= waterY()) {
      onMiss(d)
      drops.splice(i, 1)
    }
  }

  // 涟漪与文字
  for (let i = ripples.length - 1; i >= 0; i--) {
    const p = ripples[i]
    p.r += 90 * dt
    p.alpha -= 1.4 * dt
    if (p.alpha <= 0) ripples.splice(i, 1)
  }
  for (let i = wisps.length - 1; i >= 0; i--) {
    const p = wisps[i]
    p.r += 46 * dt
    p.alpha -= 1.1 * dt
    if (p.alpha <= 0) wisps.splice(i, 1)
  }
  for (let i = floats.length - 1; i >= 0; i--) {
    const f = floats[i]
    f.y += f.vy * dt
    f.alpha -= 0.9 * dt
    if (f.alpha <= 0) floats.splice(i, 1)
  }
  for (let i = splashes.length - 1; i >= 0; i--) {
    const s = splashes[i]
    s.vy += 320 * dt
    s.x += s.vx * dt
    s.y += s.vy * dt
    s.life -= dt
    if (s.life <= 0) splashes.splice(i, 1)
  }
  for (let i = toasts.length - 1; i >= 0; i--) {
    const tt = toasts[i]
    tt.life -= dt
    if (tt.life <= 0) toasts.splice(i, 1)
  }
}

/** 氛围更新（暂停时也继续，让画面有生命感） */
function updateAmbient(dt, t) {
  for (const m of mists) {
    m.x += m.spd * dt
    if (m.x - m.r > W) m.x = -m.r
  }
  for (const f of flies) {
    f.y -= f.spd * dt
    f.x += Math.sin(f.ph + t * 0.0004 + f.y * 0.012) * 14 * dt
    if (f.y < -12) {
      f.y = H + rand(8, 30)
      f.x = rand(0, W)
    }
  }
}

/* ---------------- 绘制 ---------------- */
function drawBackdrop(t) {
  const stormy = mode.value === 'storm'
  // 纸底
  const sky = ctx.createLinearGradient(0, 0, 0, H)
  if (stormy) {
    sky.addColorStop(0, '#dfe6e0')
    sky.addColorStop(1, '#ded6c2')
  } else {
    sky.addColorStop(0, '#eef4ee')
    sky.addColorStop(1, '#e9e2cd')
  }
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, W, H)

  // 淡月晕（右上，呼吸）
  const mx = W * 0.8
  const my = H * 0.15
  const mr = Math.min(W, H) * 0.12
  const breathe = 0.5 + 0.5 * Math.sin(t * 0.0006)
  const mg = ctx.createRadialGradient(mx, my, 0, mx, my, mr * (1 + breathe * 0.06))
  mg.addColorStop(0, 'rgba(255,250,235,0.5)')
  mg.addColorStop(0.5, 'rgba(245,240,225,0.16)')
  mg.addColorStop(1, 'rgba(245,240,225,0)')
  ctx.fillStyle = mg
  ctx.beginPath()
  ctx.arc(mx, my, mr, 0, Math.PI * 2)
  ctx.fill()

  // 远山（两层水墨）
  drawMountains(0.62, '#8fbdb5', 0.22, H * 0.42, 1)
  drawMountains(0.42, '#3d504b', 0.16, H * 0.55, 2)

  // 烟雾团
  for (const m of mists) {
    const g = ctx.createRadialGradient(m.x, m.y, 0, m.x, m.y, m.r)
    g.addColorStop(0, 'rgba(220, 235, 231, 0.5)')
    g.addColorStop(1, 'rgba(220, 235, 231, 0)')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.arc(m.x, m.y, m.r, 0, Math.PI * 2)
    ctx.fill()
  }

  // 水面
  const wy = waterY()
  ctx.strokeStyle = 'rgba(61, 80, 75, 0.4)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(0, wy)
  for (let x = 0; x <= W; x += 18) {
    ctx.lineTo(x, wy + Math.sin(x * 0.035 + t * 0.0011) * 2.2)
  }
  ctx.stroke()
  // 水面倒影淡染
  const wg = ctx.createLinearGradient(0, wy, 0, H)
  wg.addColorStop(0, 'rgba(143, 189, 181, 0.18)')
  wg.addColorStop(1, 'rgba(143, 189, 181, 0)')
  ctx.fillStyle = wg
  ctx.fillRect(0, wy, W, H - wy)
}

function drawMountains(ampRatio, color, alpha, baseY, seed) {
  ctx.save()
  ctx.globalAlpha = alpha
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.moveTo(0, H)
  const n = 5 + seed
  for (let i = 0; i <= n; i++) {
    const x = (W / n) * i
    const peak = baseY - Math.abs(Math.sin(i * (1.7 + seed * 0.37) + seed)) * H * ampRatio * 0.5
    ctx.quadraticCurveTo(x - W / n / 2, baseY, x, peak)
  }
  ctx.lineTo(W, H)
  ctx.closePath()
  ctx.fill()
  ctx.restore()
}

function drawDrop(d) {
  ctx.save()
  ctx.translate(d.x, d.y)
  if (d.type === 'rain') {
    ctx.lineCap = 'round'
    // 长拖尾
    ctx.strokeStyle = 'rgba(78, 141, 134, 0.22)'
    ctx.lineWidth = d.size * 0.7
    ctx.beginPath()
    ctx.moveTo(0, 0)
    ctx.lineTo(-d.vx * 0.06, -22)
    ctx.stroke()
    // 主雨丝
    ctx.strokeStyle = 'rgba(78, 141, 134, 0.82)'
    ctx.lineWidth = d.size
    ctx.beginPath()
    ctx.moveTo(0, 0)
    ctx.lineTo(-d.vx * 0.04, -12)
    ctx.stroke()
  } else if (d.type === 'petal') {
    ctx.rotate(d.rot)
    const g = ctx.createLinearGradient(0, -d.size, 0, d.size)
    g.addColorStop(0, 'rgba(216, 178, 106, 0.96)')
    g.addColorStop(1, 'rgba(179, 120, 70, 0.92)')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.moveTo(0, -d.size)
    ctx.bezierCurveTo(d.size * 1.1, -d.size * 0.4, d.size * 0.8, d.size * 0.7, 0, d.size)
    ctx.bezierCurveTo(-d.size * 0.8, d.size * 0.7, -d.size * 1.1, -d.size * 0.4, 0, -d.size)
    ctx.fill()
    ctx.strokeStyle = 'rgba(140, 95, 50, 0.5)'
    ctx.lineWidth = 0.9
    ctx.beginPath()
    ctx.moveTo(0, -d.size * 0.8)
    ctx.lineTo(0, d.size * 0.8)
    ctx.stroke()
  } else if (d.type === 'leaf') {
    ctx.rotate(d.rot)
    const g = ctx.createLinearGradient(-d.size, 0, d.size, 0)
    g.addColorStop(0, 'rgba(78, 141, 134, 0.95)')
    g.addColorStop(1, 'rgba(60, 110, 103, 0.9)')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.moveTo(-d.size, 0)
    ctx.quadraticCurveTo(0, -d.size * 0.5, d.size, 0)
    ctx.quadraticCurveTo(0, d.size * 0.5, -d.size, 0)
    ctx.fill()
    ctx.strokeStyle = 'rgba(40, 82, 75, 0.5)'
    ctx.lineWidth = 0.9
    ctx.beginPath()
    ctx.moveTo(-d.size * 0.8, 0)
    ctx.lineTo(d.size * 0.8, 0)
    ctx.stroke()
  } else {
    // 朱砂小印
    ctx.rotate(Math.sin(d.rot) * 0.3)
    ctx.shadowColor = 'rgba(179, 54, 43, 0.55)'
    ctx.shadowBlur = 10
    ctx.fillStyle = '#b3362b'
    const s = d.size
    roundRect(-s / 2, -s / 2, s, s, 3)
    ctx.fill()
    ctx.shadowBlur = 0
    ctx.strokeStyle = 'rgba(248, 234, 216, 0.8)'
    ctx.lineWidth = 1.4
    roundRect(-s / 2 + 2.5, -s / 2 + 2.5, s - 5, s - 5, 2)
    ctx.stroke()
    ctx.fillStyle = 'rgba(248, 234, 216, 0.92)'
    ctx.font = Math.round(s * 0.7) + 'px KaiTi, STKaiti, serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('听', 0, 1)
  }
  ctx.restore()
}

function roundRect(x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

function drawUmbrella(t) {
  const x = umb.x
  const y = umbY() + Math.sin(t * 0.0016) * 2.4 // 呼吸般轻微起伏
  const hw = umb.hw
  const ch = umb.ch

  ctx.save()
  ctx.translate(x, y)
  ctx.rotate(umb.tilt)

  // 伞柄（竹节墨笔）
  ctx.strokeStyle = '#2c3e39'
  ctx.lineWidth = 5
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.moveTo(0, 2)
  ctx.lineTo(0, 60)
  ctx.stroke()
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.arc(9, 60, 9, Math.PI, 0, false)
  ctx.stroke()

  // 伞面（油纸暖色渐变）
  const g = ctx.createLinearGradient(0, -ch, 0, 6)
  g.addColorStop(0, '#f6ead2')
  g.addColorStop(1, '#e2cf9f')
  ctx.fillStyle = g
  ctx.beginPath()
  ctx.moveTo(-hw, 0)
  ctx.quadraticCurveTo(-hw, -ch * 1.05, 0, -ch * 1.12)
  ctx.quadraticCurveTo(hw, -ch * 1.05, hw, 0)
  ctx.quadraticCurveTo(0, ch * 0.3, -hw, 0)
  ctx.closePath()
  ctx.fill()

  // 金线伞缘
  ctx.strokeStyle = 'rgba(163, 127, 61, 0.9)'
  ctx.lineWidth = 2.4
  ctx.beginPath()
  ctx.moveTo(-hw, 0)
  ctx.quadraticCurveTo(0, ch * 0.3, hw, 0)
  ctx.stroke()

  // 伞骨
  ctx.strokeStyle = 'rgba(78, 141, 134, 0.55)'
  ctx.lineWidth = 1.3
  for (let i = -3; i <= 3; i++) {
    const ex = (hw * i) / 3.6
    const ey = Math.cos((i / 3.6) * (Math.PI / 2)) * ch * 0.16
    ctx.beginPath()
    ctx.moveTo(0, -ch * 1.1)
    ctx.quadraticCurveTo(ex * 0.5, -ch * 0.62, ex, ey)
    ctx.stroke()
  }

  // 伞尖
  ctx.fillStyle = '#a37f3d'
  ctx.beginPath()
  ctx.arc(0, -ch * 1.12, 3.4, 0, Math.PI * 2)
  ctx.fill()

  // 朱砂流苏
  ctx.strokeStyle = 'rgba(179, 54, 43, 0.85)'
  ctx.lineWidth = 1.6
  const sw = Math.sin(t * 0.003 + umb.tilt * 6) * 4
  ctx.beginPath()
  ctx.moveTo(-hw * 0.92, ch * 0.14)
  ctx.quadraticCurveTo(-hw * 0.92 + sw, 14, -hw * 0.92 + sw, 22)
  ctx.stroke()
  ctx.fillStyle = '#b3362b'
  ctx.beginPath()
  ctx.arc(-hw * 0.92 + sw, 25, 2.6, 0, Math.PI * 2)
  ctx.fill()

  ctx.restore()

  // 接雨引导光（仅游戏中）
  if (started.value && !paused.value && !ended.value) {
    const gy = y
    const gg = ctx.createRadialGradient(x, gy + 12, 0, x, gy + 12, hw * 1.15)
    gg.addColorStop(0, 'rgba(201, 163, 92, 0.16)')
    gg.addColorStop(1, 'rgba(201, 163, 92, 0)')
    ctx.fillStyle = gg
    ctx.beginPath()
    ctx.ellipse(x, gy + 12, hw * 1.15, 16, 0, 0, Math.PI * 2)
    ctx.fill()
  }

  // 连韵环（伞顶）
  if (hud.combo > 0 && started.value) {
    const ratio = Math.min(hud.combo / 20, 1)
    const cx = x
    const cy = y - ch - 14
    ctx.strokeStyle = 'rgba(201, 163, 92, 0.35)'
    ctx.lineWidth = 2.4
    ctx.beginPath()
    ctx.arc(cx, cy, 13, 0, Math.PI * 2)
    ctx.stroke()
    ctx.strokeStyle = ratio >= 1 ? '#b3362b' : '#4e8d86'
    ctx.lineWidth = 2.6
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.arc(cx, cy, 13, -Math.PI / 2, -Math.PI / 2 + ratio * Math.PI * 2)
    ctx.stroke()
    ctx.fillStyle = 'rgba(61, 80, 75, 0.72)'
    ctx.font = '11px KaiTi, STKaiti, serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('韵', cx, cy)
  }
}

function drawEffects() {
  // 接住的大涟漪（双圈）
  for (const p of ripples) {
    ctx.strokeStyle = p.hue
    ctx.globalAlpha = Math.max(p.alpha, 0)
    ctx.lineWidth = 1.8
    ctx.beginPath()
    ctx.ellipse(p.x, p.y, p.r, p.r * 0.38, 0, 0, Math.PI * 2)
    ctx.stroke()
    ctx.beginPath()
    ctx.ellipse(p.x, p.y, p.r * 0.55, p.r * 0.21, 0, 0, Math.PI * 2)
    ctx.stroke()
    ctx.globalAlpha = 1
  }
  // 落水小涟漪
  for (const p of wisps) {
    ctx.strokeStyle = 'rgba(78, 141, 134, 1)'
    ctx.globalAlpha = Math.max(p.alpha, 0)
    ctx.lineWidth = 1.2
    ctx.beginPath()
    ctx.ellipse(p.x, p.y, p.r, p.r * 0.3, 0, 0, Math.PI * 2)
    ctx.stroke()
    ctx.globalAlpha = 1
  }
  // 墨点飞溅
  for (const s of splashes) {
    ctx.globalAlpha = Math.max(s.life * 1.6, 0)
    ctx.fillStyle = s.color
    ctx.beginPath()
    ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2)
    ctx.fill()
    ctx.globalAlpha = 1
  }
  // 漂浮文字
  for (const f of floats) {
    ctx.globalAlpha = Math.max(f.alpha, 0)
    ctx.fillStyle = f.grey ? 'rgba(61, 80, 75, 0.75)' : '#b3362b'
    ctx.font = '600 17px STKaiti, KaiTi, serif'
    ctx.textAlign = 'center'
    ctx.fillText(f.text, f.x, f.y)
    ctx.globalAlpha = 1
  }
}

function drawFireflies(t) {
  if (reducedMotion || flies.length === 0) return
  for (const f of flies) {
    const a = (0.35 + 0.35 * Math.sin(t * 0.002 + f.ph)) * f.alpha
    const g = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, f.r * 3)
    g.addColorStop(0, `rgba(246, 226, 150, ${a})`)
    g.addColorStop(1, 'rgba(246, 226, 150, 0)')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.arc(f.x, f.y, f.r * 3, 0, Math.PI * 2)
    ctx.fill()
  }
}

function drawToasts() {
  for (let i = 0; i < toasts.length; i++) {
    const tt = toasts[i]
    const age = 1.7 - tt.life
    const alpha = age < 0.2 ? age / 0.2 : Math.min(1, tt.life / 0.6)
    ctx.globalAlpha = Math.max(alpha, 0)
    ctx.fillStyle = '#b3362b'
    ctx.font = '600 22px STKaiti, KaiTi, serif'
    ctx.textAlign = 'center'
    ctx.fillText(tt.text, W / 2, 72 - (1 - alpha) * 8)
    ctx.globalAlpha = 1
  }
}

function drawVignette() {
  const g = ctx.createRadialGradient(W / 2, H * 0.45, H * 0.3, W / 2, H * 0.5, H * 0.78)
  g.addColorStop(0, 'rgba(30, 43, 40, 0)')
  g.addColorStop(1, 'rgba(30, 43, 40, 0.18)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, W, H)
}

/* ---------------- 主循环 ---------------- */
function loop(t) {
  raf = requestAnimationFrame(loop)
  const dt = clamp((t - lastT) / 1000, 0, 0.05)
  lastT = t
  if (!ctx) return

  updateAmbient(dt, t)

  // 得分滚动
  if (started.value && !ended.value) {
    displayScore.value += (hud.score - displayScore.value) * Math.min(1, dt * 8)
    if (Math.abs(hud.score - displayScore.value) < 0.5) displayScore.value = hud.score
  }

  if (!started.value) {
    // 未开局：飘一阵慢悠悠的观赏雨，只落水不记分
    idleAcc += dt * 1000
    if (idleAcc > 900) {
      idleAcc = 0
      spawnDrop(Math.random() < 0.25 ? (Math.random() < 0.5 ? 'petal' : 'leaf') : 'rain')
    }
    for (let i = drops.length - 1; i >= 0; i--) {
      const d = drops[i]
      d.y += d.vy * dt
      d.x += d.vx * dt
      if (d.y >= waterY()) {
        wisps.push({ x: d.x, y: waterY(), r: 2, alpha: 0.5 })
        drops.splice(i, 1)
      }
    }
    for (let i = wisps.length - 1; i >= 0; i--) {
      const p = wisps[i]
      p.r += 46 * dt
      p.alpha -= 1.1 * dt
      if (p.alpha <= 0) wisps.splice(i, 1)
    }
  } else if (!paused.value && !ended.value) {
    update(dt)
  }

  // 绘制
  drawBackdrop(t)
  for (const d of drops) drawDrop(d)
  drawEffects()
  drawFireflies(t)
  drawUmbrella(t)
  drawToasts()
  drawVignette()
}

/* ---------------- 交互 ---------------- */
const keys = new Set()

function onPointer(e) {
  const rect = cvs.value.getBoundingClientRect()
  const cx = (e.touches ? e.touches[0].clientX : e.clientX) - rect.left
  umb.targetX = clamp(cx, umb.hw, W - umb.hw)
}

function onKey(e, down) {
  const k = e.key
  if (k === 'ArrowLeft' || k === 'a' || k === 'A') keys[down ? 'add' : 'delete']('left')
  else if (k === 'ArrowRight' || k === 'd' || k === 'D') keys[down ? 'add' : 'delete']('right')
  else if (down && k === ' ' && started.value && !paused.value && !ended.value && (!e.target || e.target === document.body)) {
    // 仅当焦点不在按钮上时响应空格，避免与按钮点击叠加
    e.preventDefault()
    togglePause()
  }
}

function startGame() {
  started.value = true
  paused.value = false
  ended.value = false
  hud.score = 0
  hud.combo = 0
  hud.maxCombo = 0
  hud.caught = 0
  stats.rain = stats.petal = stats.leaf = stats.seal = stats.missed = 0
  displayScore.value = 0
  elapsed.value = 0
  prevBest.value = best[mode.value]
  drops.length = 0
  floats.length = 0
  splashes.length = 0
  toasts.length = 0
  spawnAcc = 0
  lastComboMilestone = 0
  lastScoreMilestone = 0
}

function togglePause() {
  paused.value = !paused.value
}

function endGame() {
  ended.value = true
  paused.value = false
  recordRain() // 记录一次「伞下听雨」人次（后端 fire-and-forget）
}

function toggleAudio() {
  setAudio(!audioOn.value)
}

function persistBest() {
  try {
    localStorage.setItem(BEST_KEY, JSON.stringify(best))
  } catch {
    /* 隐私模式下忽略 */
  }
}

function loadBest() {
  try {
    const raw = localStorage.getItem(BEST_KEY)
    if (raw) {
      const o = JSON.parse(raw)
      best.serene = o.serene || 0
      best.storm = o.storm || 0
    }
  } catch {
    /* 忽略损坏数据 */
  }
}

// 切换模式时连韵归零（已落下的雨保持原速飘完即可）
watch(mode, () => {
  if (started.value) hud.combo = 0
  lastComboMilestone = 0
})

/* ---------------- 可选雨声（自合成，无外部文件，默认关） ---------------- */
const audioOn = ref(false)
let actx = null
let ambientGain = null
let lastChime = 0

function ensureAudio() {
  if (actx) return
  try {
    const AC = window.AudioContext || window.webkitAudioContext
    if (!AC) return
    actx = new AC()
    const buf = actx.createBuffer(1, actx.sampleRate * 2, actx.sampleRate)
    const d = buf.getChannelData(0)
    for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * 0.5
    const noise = actx.createBufferSource()
    noise.buffer = buf
    noise.loop = true
    const bp = actx.createBiquadFilter()
    bp.type = 'bandpass'
    bp.frequency.value = 1100
    bp.Q.value = 0.6
    ambientGain = actx.createGain()
    ambientGain.gain.value = 0
    noise.connect(bp)
    bp.connect(ambientGain)
    ambientGain.connect(actx.destination)
    noise.start()
  } catch {
    actx = null
  }
}

function setAudio(on) {
  audioOn.value = on
  try {
    if (on) {
      ensureAudio()
      if (actx) {
        if (actx.state === 'suspended') actx.resume()
        ambientGain.gain.setTargetAtTime(0.05, actx.currentTime, 0.4)
      }
    } else if (actx && ambientGain) {
      ambientGain.gain.setTargetAtTime(0, actx.currentTime, 0.3)
    }
  } catch {
    /* 忽略 */
  }
}

function chime(type) {
  if (!audioOn.value || !actx) return
  const now = actx.currentTime
  if (now - lastChime < 0.05) return
  lastChime = now
  try {
    const o = actx.createOscillator()
    const g = actx.createGain()
    const base = type === 'seal' ? 880 : type === 'petal' ? 720 : type === 'leaf' ? 640 : 560
    o.type = 'sine'
    o.frequency.value = base
    g.gain.setValueAtTime(0.0001, now)
    g.gain.exponentialRampToValueAtTime(0.06, now + 0.01)
    g.gain.exponentialRampToValueAtTime(0.0001, now + 0.32)
    o.connect(g)
    g.connect(actx.destination)
    o.start(now)
    o.stop(now + 0.34)
  } catch {
    /* 忽略 */
  }
}

/* ---------------- 生命周期 ---------------- */
onMounted(() => {
  ctx = cvs.value.getContext('2d')
  resize()
  loadBest()
  try {
    reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  } catch {
    reducedMotion = false
  }

  // 初始化烟雾团
  for (let i = 0; i < 4; i++) {
    mists.push({
      x: rand(0, W),
      y: rand(H * 0.2, H * 0.7),
      r: rand(60, 140),
      spd: rand(6, 16),
    })
  }
  // 初始化流萤（reduced-motion 时不生成，保持安静）
  if (!reducedMotion) {
    for (let i = 0; i < 7; i++) {
      flies.push({
        x: rand(0, W),
        y: rand(0, H),
        r: rand(1.4, 2.8),
        spd: rand(8, 18),
        ph: rand(0, Math.PI * 2),
        alpha: rand(0.6, 1),
      })
    }
  }

  ro = new ResizeObserver(resize)
  ro.observe(wrap.value)

  const kd = (e) => onKey(e, true)
  const ku = (e) => onKey(e, false)
  const onVis = () => {
    if (document.hidden && started.value && !paused.value && !ended.value) paused.value = true
  }

  cvs.value.addEventListener('mousemove', onPointer)
  cvs.value.addEventListener('touchmove', onPointer, { passive: true })
  window.addEventListener('keydown', kd)
  window.addEventListener('keyup', ku)
  document.addEventListener('visibilitychange', onVis)
  cleanup = () => {
    window.removeEventListener('keydown', kd)
    window.removeEventListener('keyup', ku)
    document.removeEventListener('visibilitychange', onVis)
  }

  lastT = performance.now()
  raf = requestAnimationFrame(loop)
})

let cleanup = null

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  if (ro) ro.disconnect()
  if (cleanup) cleanup()
  if (actx) {
    try { actx.close() } catch { /* ignore */ }
    actx = null
  }
})
</script>

<template>
  <section class="view rain-page">
    <!-- ============ 标题区 ============ -->
    <div class="rain-head">
      <span class="seal seal-mark">听雨</span>
      <h1 class="rain-title">伞 下 听 雨</h1>
      <p class="rain-verse">一伞烟雨 · 满袖飞花 <b>接住流光</b></p>
    </div>

    <!-- ============ 游戏区 ============ -->
    <div class="rain-stage card">
      <span class="corner tl"></span>
      <span class="corner tr"></span>
      <span class="corner bl"></span>
      <span class="corner br"></span>

      <!-- HUD -->
      <div class="hud">
        <div class="hud-item"><i>听雨</i><b>{{ Math.round(displayScore) }}</b></div>
        <div class="hud-item" :class="{ on: hud.combo >= 3 }"><i>连韵</i><b>×{{ hud.combo }}</b></div>
        <div class="hud-item"><i>最佳</i><b>{{ best[mode] }}</b></div>
        <div class="hud-item mode"><i>此刻</i><b>{{ modeName }}</b></div>
        <div v-if="started && !ended" class="hud-item"><i>时长</i><b>{{ timeText }}</b></div>
      </div>

      <div ref="wrap" class="canvas-wrap">
        <canvas ref="cvs"></canvas>

        <!-- 开局遮罩 -->
        <div v-if="!started" class="overlay">
          <p class="ov-verse">烟雨落青阶，飞花过伞来。</p>
          <p class="ov-tip">移动鼠标 / 触屏 / ←→ 键撑伞，接住雨珠与飞花</p>
          <button type="button" class="btn primary ov-btn" @click="startGame">入 雨</button>
          <p class="ov-sub">悠然听雨 · 无输赢 · 只为静心</p>
        </div>

        <!-- 暂停遮罩 -->
        <div v-else-if="paused" class="overlay soft" @click="togglePause">
          <p class="ov-verse">雨暂歇，伞未收。</p>
          <p class="ov-tip">点击任意处继续</p>
        </div>

        <!-- 收伞结算卷宗 -->
        <div v-else-if="ended" class="overlay scroll-ov">
          <div class="scroll-card">
            <span class="scroll-seal">卷</span>
            <h2 class="scroll-rank">{{ rank.title }}</h2>
            <p class="scroll-verse">{{ rank.verse }}</p>

            <div class="scroll-stats">
              <div class="sstat"><i>听雨</i><b>{{ hud.score }}</b></div>
              <div class="sstat"><i>接住</i><b>{{ hud.caught }}</b></div>
              <div class="sstat"><i>最长连韵</i><b>×{{ hud.maxCombo }}</b></div>
              <div class="sstat"><i>听雨时长</i><b>{{ timeText }}</b></div>
            </div>

            <div class="scroll-flora">
              <span class="flora" v-for="lg in legend" :key="lg.key">
                <i class="dot" :style="{ background: lg.color }"></i>{{ lg.label }} {{ stats[lg.key] }}
              </span>
            </div>

            <div v-if="badges.length" class="scroll-badges">
              <span class="badge" v-for="b in badges" :key="b">{{ b }}</span>
            </div>

            <p class="scroll-best">
              历来最佳 <b>{{ best[mode] }}</b>
              <span v-if="newRecord" class="rec-tag">新纪录</span>
              <span v-else> · 本局 {{ hud.score }}</span>
            </p>

            <div class="scroll-actions">
              <button type="button" class="btn primary" @click="startGame">重 听</button>
              <button type="button" class="ctrl-btn" @click="router.push({ name: 'landing' })">回 阁</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ============ 控制区 ============ -->
      <div class="rain-ctrl">
        <div class="ctrl-group">
          <span class="ctrl-label">模式</span>
          <button
            type="button"
            class="ctrl-btn"
            :class="{ active: mode === 'serene' }"
            @click="mode = 'serene'"
          >悠然听雨</button>
          <button
            type="button"
            class="ctrl-btn"
            :class="{ active: mode === 'storm' }"
            @click="mode = 'storm'"
          >疾雨挑战</button>
        </div>

        <div class="ctrl-group">
          <span class="ctrl-label">雨势</span>
          <input
            v-model.number="rainLevel"
            class="rain-slider"
            type="range"
            min="1"
            max="5"
            step="1"
          />
          <span class="slider-hint">{{ ['细雨', '微雨', '疏雨', '密雨', '骤雨'][rainLevel - 1] }}</span>
        </div>

        <div class="ctrl-group">
          <button
            v-if="!started"
            type="button"
            class="btn primary ctrl-play"
            @click="startGame"
          >开 始</button>
          <template v-else>
            <button v-if="!ended" type="button" class="ctrl-btn" @click="togglePause">{{ paused ? '继 续' : '暂 停' }}</button>
            <button type="button" class="ctrl-btn" @click="startGame">重 来</button>
            <button v-if="!ended" type="button" class="ctrl-btn ghost-end" @click="endGame">收 伞</button>
          </template>
          <button
            type="button"
            class="ctrl-btn audio-btn"
            :class="{ active: audioOn }"
            :title="audioOn ? '关闭雨声' : '开启雨声'"
            @click="toggleAudio"
          >{{ audioOn ? '雨声·开' : '雨声·关' }}</button>
        </div>
      </div>

      <p class="rain-note">
        雨珠 +1 · 飞花 +3 · 竹叶 +2 · 朱砂小印 +8 · 连韵不断有加成 · 韵断了也无妨，江湖嘛，图个自在。
      </p>

      <!-- 物候图例 -->
      <div class="legend">
        <span class="chip" v-for="lg in legend" :key="lg.key">
          <i class="dot" :style="{ background: lg.color }"></i>{{ lg.label }} <b>+{{ lg.pts }}</b>
        </span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.rain-page {
  position: relative;
  max-width: 1000px;
  margin: 0 auto;
  padding: 46px 24px 64px;
}

/* ============ 标题区 ============ */
.rain-head {
  position: relative;
  text-align: center;
  margin-bottom: 30px;
}
.rain-head .seal {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%) rotate(-4deg);
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  writing-mode: vertical-rl;
  font-size: 14px;
  letter-spacing: 2px;
  opacity: 0.92;
}
.rain-title {
  font-size: 42px;
  font-weight: normal;
  letter-spacing: 14px;
  text-indent: 14px;
  margin-top: 24px;
  background: linear-gradient(180deg, #2c423d 20%, #4e8d86 90%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.rain-verse {
  font-size: 15px;
  letter-spacing: 6px;
  color: var(--ink-soft);
  opacity: 0.75;
  margin-top: 10px;
}
.rain-verse b {
  color: var(--cinnabar);
  font-weight: normal;
}

/* ============ 游戏舞台（复用 .card 质感） ============ */
.rain-stage {
  width: 100%;
  padding: 20px 20px 22px;
}

.hud {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px 18px;
  padding: 2px 6px 14px;
}
.hud-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.hud-item i {
  font-style: normal;
  font-size: 12.5px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.72;
}
.hud-item b {
  font-size: 21px;
  font-weight: normal;
  letter-spacing: 1px;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.hud-item.on b {
  color: var(--cinnabar);
  animation: combo-pulse 0.5s ease;
}
.hud-item.mode b {
  font-size: 15px;
  letter-spacing: 3px;
}
@keyframes combo-pulse {
  40% { transform: scale(1.28); }
}

.canvas-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  border: 1px solid rgba(201, 163, 92, 0.5);
  border-radius: 4px;
  overflow: hidden;
  background: #eef4ee;
  box-shadow: inset 0 0 40px rgba(201, 163, 92, 0.08);
  touch-action: none;
  cursor: none;
}
.canvas-wrap canvas {
  display: block;
  width: 100%;
  height: 100%;
}

/* 遮罩层 */
.overlay {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  background: rgba(244, 241, 232, 0.66);
  backdrop-filter: blur(3px);
  cursor: default;
  padding: 20px;
}
.overlay.soft {
  cursor: pointer;
}
.ov-verse {
  font-size: 21px;
  letter-spacing: 7px;
  color: var(--ink);
}
.ov-tip {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.8;
}
.ov-btn {
  margin-top: 8px;
  padding: 11px 40px;
  font-size: 16px;
}
.ov-sub {
  font-size: 12px;
  letter-spacing: 4px;
  color: var(--gold-deep);
  opacity: 0.85;
  margin-top: 4px;
}

/* 收伞结算卷宗 */
.scroll-ov {
  background: rgba(244, 241, 232, 0.78);
  overflow-y: auto;
}
.scroll-card {
  position: relative;
  width: min(420px, 92%);
  background: linear-gradient(160deg, rgba(250, 248, 240, 0.97), rgba(240, 238, 228, 0.95));
  border: 1px solid rgba(201, 163, 92, 0.55);
  border-radius: 6px;
  padding: 30px 28px 24px;
  box-shadow: 0 22px 60px rgba(30, 43, 40, 0.22), inset 0 0 50px rgba(201, 163, 92, 0.08);
  animation: scroll-in 0.5s cubic-bezier(0.22, 0.8, 0.3, 1) both;
}
.scroll-card::before {
  content: '';
  position: absolute;
  inset: 6px;
  border: 1px solid rgba(201, 163, 92, 0.3);
  border-radius: 3px;
  pointer-events: none;
}
@keyframes scroll-in {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to { opacity: 1; transform: none; }
}
.scroll-seal {
  position: absolute;
  top: -16px;
  right: -10px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--cinnabar);
  color: #f8ead8;
  border-radius: 5px;
  font-size: 18px;
  transform: rotate(6deg);
  box-shadow: 0 4px 14px rgba(179, 54, 43, 0.4), inset 0 0 8px rgba(120, 20, 10, 0.45);
}
.scroll-rank {
  font-size: 28px;
  font-weight: normal;
  letter-spacing: 8px;
  text-indent: 8px;
  text-align: center;
  color: var(--ink);
}
.scroll-verse {
  font-size: 13.5px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.82;
  text-align: center;
  margin-top: 10px;
  line-height: 1.8;
}
.scroll-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin: 20px 0 14px;
}
.scroll-stats .sstat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  background: rgba(244, 241, 232, 0.6);
  border: 1px solid rgba(201, 163, 92, 0.3);
  border-radius: 4px;
}
.scroll-stats .sstat i {
  font-style: normal;
  font-size: 11.5px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  opacity: 0.75;
}
.scroll-stats .sstat b {
  font-size: 19px;
  font-weight: normal;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.scroll-flora {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 14px;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--ink-soft);
}
.scroll-flora .flora {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.scroll-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin: 14px 0 6px;
}
.scroll-badges .badge {
  font-size: 12.5px;
  letter-spacing: 2px;
  color: var(--gold-deep);
  border: 1px solid rgba(163, 127, 61, 0.5);
  background: rgba(250, 248, 240, 0.7);
  padding: 4px 12px;
  border-radius: 999px;
}
.scroll-best {
  text-align: center;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  margin-top: 10px;
}
.scroll-best b {
  color: var(--ink);
  font-weight: normal;
}
.rec-tag {
  display: inline-block;
  margin-left: 8px;
  font-size: 12px;
  letter-spacing: 2px;
  color: #f8ead8;
  background: var(--cinnabar);
  padding: 2px 10px;
  border-radius: 999px;
}
.scroll-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 18px;
}
.scroll-actions .btn {
  padding: 10px 28px;
  font-size: 15px;
}

/* ============ 控制区 ============ */
.rain-ctrl {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 22px;
  margin-top: 18px;
  padding: 14px 16px;
  background: rgba(244, 241, 232, 0.6);
  border: 1px solid rgba(201, 163, 92, 0.3);
  border-radius: 4px;
}
.ctrl-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ctrl-label {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--ink-soft);
  opacity: 0.75;
}
.ctrl-btn {
  border: 1px solid rgba(163, 127, 61, 0.5);
  background: rgba(250, 248, 240, 0.7);
  color: var(--gold-deep);
  font-family: inherit;
  font-size: 13.5px;
  letter-spacing: 3px;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.25s;
}
.ctrl-btn:hover {
  background: rgba(201, 163, 92, 0.16);
  transform: translateY(-1px);
}
.ctrl-btn.active {
  color: #f8f2e4;
  background: linear-gradient(120deg, #28524b, #3d6f66);
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(40, 82, 75, 0.3);
}
.ctrl-btn.ghost-end {
  color: var(--cinnabar);
  border-color: rgba(179, 54, 43, 0.5);
}
.ctrl-btn.ghost-end:hover {
  background: rgba(179, 54, 43, 0.12);
}
.ctrl-btn.audio-btn {
  font-size: 12.5px;
  letter-spacing: 2px;
}
.ctrl-play {
  padding: 9px 30px;
  font-size: 15px;
  border-radius: 3px;
}

/* 雨势滑杆（水墨化） */
.rain-slider {
  -webkit-appearance: none;
  appearance: none;
  width: 130px;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--jade-light), var(--jade), var(--ink-soft));
  outline: none;
  cursor: pointer;
}
.rain-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--cinnabar);
  border: 2px solid #f8ead8;
  box-shadow: 0 2px 8px rgba(179, 54, 43, 0.4);
  cursor: grab;
}
.rain-slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--cinnabar);
  border: 2px solid #f8ead8;
  box-shadow: 0 2px 8px rgba(179, 54, 43, 0.4);
  cursor: grab;
}
.slider-hint {
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--gold-deep);
  min-width: 2.5em;
}

.rain-note {
  margin-top: 14px;
  text-align: center;
  font-size: 12.5px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  opacity: 0.66;
}

/* 物候图例 */
.legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 14px;
  margin-top: 12px;
}
.legend .chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  letter-spacing: 2px;
  color: var(--ink-soft);
  border: 1px solid rgba(201, 163, 92, 0.3);
  background: rgba(250, 248, 240, 0.55);
  padding: 4px 12px;
  border-radius: 999px;
}
.legend .chip b {
  color: var(--gold-deep);
  font-weight: normal;
}
.legend .dot,
.scroll-flora .dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}

/* ============ 响应式 ============ */
@media (max-width: 720px) {
  .rain-title { font-size: 32px; letter-spacing: 9px; }
  .canvas-wrap { aspect-ratio: 4 / 5; }
  .hud-item.mode { display: none; }
  .rain-ctrl { justify-content: center; }
  .scroll-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>
