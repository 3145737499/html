<script setup>
/**
 * 水墨背景层（2026-10-14 升级）
 * 四叠远山 + 淡月晕 + 飞鸟点墨 + 墨笔横划 + 三团飘浮烟雾
 * 纯 SVG + CSS 动画，reduced-motion 由 base.css 全局兜底
 */
</script>

<template>
  <div class="ink-bg" aria-hidden="true">
    <svg viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice">
      <defs>
        <radialGradient id="inkG" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#3d504b" stop-opacity=".28" />
          <stop offset="100%" stop-color="#3d504b" stop-opacity="0" />
        </radialGradient>
        <radialGradient id="moonG" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#f6efdd" stop-opacity=".95" />
          <stop offset="55%" stop-color="#efe6cf" stop-opacity=".55" />
          <stop offset="100%" stop-color="#efe6cf" stop-opacity="0" />
        </radialGradient>
        <linearGradient id="mtn-far" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#9dbfb8" stop-opacity=".26" />
          <stop offset="100%" stop-color="#9dbfb8" stop-opacity="0" />
        </linearGradient>
        <linearGradient id="mtn-mid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#7fa8a0" stop-opacity=".34" />
          <stop offset="100%" stop-color="#7fa8a0" stop-opacity="0" />
        </linearGradient>
        <linearGradient id="mtn-near" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#5d8880" stop-opacity=".4" />
          <stop offset="100%" stop-color="#5d8880" stop-opacity="0" />
        </linearGradient>
        <filter id="soft" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="14" />
        </filter>
      </defs>

      <ellipse cx="180" cy="820" rx="340" ry="130" fill="url(#inkG)" />
      <ellipse cx="1300" cy="120" rx="300" ry="110" fill="url(#inkG)" />

      <!-- 淡月 + 月晕 -->
      <g class="moon">
        <circle cx="1080" cy="180" r="88" fill="url(#moonG)" />
        <circle cx="1080" cy="180" r="34" fill="#f3ecd9" opacity=".8" />
      </g>

      <!-- 远山三叠（由淡到浓） -->
      <path d="M-40,640 Q140,520 320,600 T660,560 T1010,620 T1360,560 T1500,600 L1500,900 L-40,900 Z" fill="url(#mtn-far)" />
      <path d="M-40,700 Q180,540 380,660 T760,620 T1140,680 T1500,600 L1500,900 L-40,900 Z" fill="url(#mtn-mid)" />
      <path d="M-40,760 Q260,640 520,730 T1080,700 T1500,740 L1500,900 L-40,900 Z" fill="url(#mtn-near)" opacity=".8" />

      <!-- 山脚雾带（模糊晕染） -->
      <ellipse cx="420" cy="700" rx="380" ry="46" fill="#e8f0ec" opacity=".55" filter="url(#soft)" />
      <ellipse cx="1080" cy="740" rx="420" ry="52" fill="#e8f0ec" opacity=".5" filter="url(#soft)" />

      <!-- 飞鸟点墨（人字笔意） -->
      <g class="birds" stroke="#2c423d" stroke-width="2.4" fill="none" stroke-linecap="round">
        <path d="M905,215 q11,-9 22,0 q11,-9 22,0" opacity=".5" />
        <path d="M962,244 q9,-7 18,0 q9,-7 18,0" opacity=".38" />
        <path d="M1012,200 q7,-6 14,0 q7,-6 14,0" opacity=".3" />
      </g>

      <!-- 墨笔横划 -->
      <path d="M60,120 Q400,86 760,108 T1380,92" stroke="#2c423d" stroke-opacity=".14" stroke-width="7" fill="none" stroke-linecap="round" />
      <path d="M120,146 Q460,120 820,140" stroke="#2c423d" stroke-opacity=".09" stroke-width="4" fill="none" stroke-linecap="round" />
    </svg>

    <!-- 飘浮烟雾 -->
    <div class="mist m1"></div>
    <div class="mist m2"></div>
    <div class="mist m3"></div>
  </div>
</template>

<style scoped>
.ink-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.ink-bg svg {
  width: 100%;
  height: 100%;
}

/* 月晕呼吸 */
.moon {
  animation: moon-breathe 9s ease-in-out infinite alternate;
  transform-origin: 1080px 180px;
}
@keyframes moon-breathe {
  from { opacity: 0.75; transform: scale(1); }
  to { opacity: 1; transform: scale(1.04); }
}

/* 飞鸟缓渡 */
.birds {
  animation: birds-drift 26s ease-in-out infinite alternate;
}
@keyframes birds-drift {
  from { transform: translate(0, 0); }
  to { transform: translate(-70px, 14px); }
}

.mist {
  position: fixed;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.35;
  pointer-events: none;
  animation: drift 18s ease-in-out infinite alternate;
}

.mist.m1 {
  width: 520px;
  height: 520px;
  background: #a9cdc5;
  top: -120px;
  left: -120px;
}

.mist.m2 {
  width: 420px;
  height: 420px;
  background: #bcd8d1;
  bottom: -140px;
  right: 26%;
  animation-delay: -6s;
}

.mist.m3 {
  width: 300px;
  height: 300px;
  background: #e6d9bd;
  top: 30%;
  right: -100px;
  animation-delay: -12s;
  opacity: 0.25;
}

@keyframes drift {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(60px, 30px) scale(1.12);
  }
}
</style>
