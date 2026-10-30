<script setup>
/**
 * 水墨背景层（2026-10-30 重写）
 * 宣纸肌理 + 五叠远山（皴笔/湿边）+ 留白云雾 + 水面小舟
 * + 竹枝 / 芦苇 / 飞鸟 / 墨点飞溅 / 朱印落款
 * 纯 SVG + CSS 动画，reduced-motion 由 base.css 全局兜底
 */
</script>

<template>
  <div class="ink-bg" aria-hidden="true">
    <!-- 宣纸底：暖冷渐层 + 纸纹噪点 + 帘纹 -->
    <div class="ink-paper"></div>

    <svg class="ink-scene" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice">
      <defs>
        <!-- ============ 渐层 ============ -->
        <linearGradient id="skyWash" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#f2f3e9" stop-opacity=".85" />
          <stop offset="38%" stop-color="#e7ece5" stop-opacity=".45" />
          <stop offset="100%" stop-color="#dbe6df" stop-opacity="0" />
        </linearGradient>

        <linearGradient id="m1" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#8fb0a9" stop-opacity=".40" />
          <stop offset="55%" stop-color="#8fb0a9" stop-opacity=".16" />
          <stop offset="100%" stop-color="#8fb0a9" stop-opacity="0" />
        </linearGradient>
        <linearGradient id="m2" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#689087" stop-opacity=".52" />
          <stop offset="55%" stop-color="#689087" stop-opacity=".22" />
          <stop offset="100%" stop-color="#689087" stop-opacity="0" />
        </linearGradient>
        <linearGradient id="m3" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#3f6a63" stop-opacity=".62" />
          <stop offset="55%" stop-color="#3f6a63" stop-opacity=".28" />
          <stop offset="100%" stop-color="#3f6a63" stop-opacity="0" />
        </linearGradient>
        <linearGradient id="m4" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#264a44" stop-opacity=".74" />
          <stop offset="50%" stop-color="#264a44" stop-opacity=".36" />
          <stop offset="100%" stop-color="#264a44" stop-opacity=".08" />
        </linearGradient>
        <linearGradient id="bank" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#17322c" stop-opacity=".82" />
          <stop offset="100%" stop-color="#17322c" stop-opacity=".55" />
        </linearGradient>

        <linearGradient id="water" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#f0f4ee" stop-opacity=".92" />
          <stop offset="45%" stop-color="#e3ece5" stop-opacity=".86" />
          <stop offset="100%" stop-color="#cfdcd4" stop-opacity=".9" />
        </linearGradient>

        <radialGradient id="moonG" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#fbf6e6" stop-opacity=".98" />
          <stop offset="42%" stop-color="#f1e8cf" stop-opacity=".55" />
          <stop offset="100%" stop-color="#efe6cf" stop-opacity="0" />
        </radialGradient>

        <radialGradient id="bloom" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#22423b" stop-opacity=".30" />
          <stop offset="60%" stop-color="#22423b" stop-opacity=".10" />
          <stop offset="100%" stop-color="#22423b" stop-opacity="0" />
        </radialGradient>

        <radialGradient id="warmBloom" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#e8d9ac" stop-opacity=".40" />
          <stop offset="100%" stop-color="#e8d9ac" stop-opacity="0" />
        </radialGradient>

        <!-- ============ 滤镜 ============ -->
        <!-- 湿墨边缘晕染（山形） -->
        <filter id="bleedA" x="-14%" y="-24%" width="128%" height="148%">
          <feTurbulence type="fractalNoise" baseFrequency="0.0055 0.019" numOctaves="4" seed="17" result="t" />
          <feDisplacementMap in="SourceGraphic" in2="t" scale="24" xChannelSelector="R" yChannelSelector="G" />
          <feGaussianBlur stdDeviation="1.5" />
        </filter>
        <filter id="bleedB" x="-14%" y="-24%" width="128%" height="148%">
          <feTurbulence type="fractalNoise" baseFrequency="0.0075 0.024" numOctaves="4" seed="53" result="t" />
          <feDisplacementMap in="SourceGraphic" in2="t" scale="18" xChannelSelector="R" yChannelSelector="G" />
          <feGaussianBlur stdDeviation="1.2" />
        </filter>
        <!-- 大面积水渍晕开 -->
        <filter id="wash" x="-30%" y="-30%" width="160%" height="160%">
          <feTurbulence type="fractalNoise" baseFrequency="0.011 0.017" numOctaves="5" seed="9" result="t" />
          <feDisplacementMap in="SourceGraphic" in2="t" scale="42" xChannelSelector="R" yChannelSelector="G" />
          <feGaussianBlur stdDeviation="6" />
        </filter>
        <!-- 柔雾 -->
        <filter id="fog" x="-50%" y="-60%" width="200%" height="220%">
          <feGaussianBlur stdDeviation="22" />
        </filter>
        <filter id="fogS" x="-50%" y="-80%" width="200%" height="260%">
          <feGaussianBlur stdDeviation="10" />
        </filter>
        <filter id="soft3" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="3" />
        </filter>

        <!-- 竹叶原型 -->
        <g id="inkLeaf">
          <path d="M0,0 C22,-16 52,-17 80,-5 C52,10 22,11 0,0 Z" />
        </g>
      </defs>

      <!-- 天际淡墨洗 -->
      <rect x="-120" y="-60" width="1680" height="720" fill="url(#skyWash)" />

      <!-- 大块墨晕（远近各一，压住画面四角） -->
      <g filter="url(#wash)">
        <ellipse cx="200" cy="700" rx="330" ry="120" fill="url(#bloom)" />
        <ellipse cx="1250" cy="760" rx="360" ry="110" fill="url(#bloom)" />
        <ellipse cx="1180" cy="140" rx="260" ry="120" fill="url(#warmBloom)" />
      </g>

      <!-- 淡月 + 月晕 -->
      <g class="moon">
        <circle cx="1092" cy="176" r="104" fill="url(#moonG)" />
        <circle cx="1092" cy="176" r="36" fill="#f7f1de" opacity=".82" />
        <circle cx="1080" cy="168" r="7" fill="#e6dcc0" opacity=".35" />
        <circle cx="1103" cy="188" r="4.5" fill="#e6dcc0" opacity=".28" />
      </g>

      <!-- ============ 五叠远山 ============ -->
      <!-- 一叠：最淡 -->
      <path
        d="M-120,624 Q90,540 210,588 Q300,514 400,560 Q470,498 560,556 Q660,504 770,566 Q860,518 960,570 Q1070,514 1180,572 Q1290,526 1400,580 Q1470,556 1560,572 L1560,1000 L-120,1000 Z"
        fill="url(#m1)" filter="url(#bleedA)"
      />
      <!-- 二叠 -->
      <path
        d="M-120,676 Q120,596 250,652 Q360,574 470,634 Q580,566 700,642 Q820,586 940,650 Q1060,594 1180,654 Q1300,600 1420,658 Q1470,632 1560,642 L1560,1000 L-120,1000 Z"
        fill="url(#m2)" filter="url(#bleedB)"
      />
      <!-- 三叠：带主峰 -->
      <path
        d="M-120,730 Q40,690 140,712 Q186,700 232,624 Q272,676 322,704 Q430,656 540,716 Q650,644 760,710 Q880,650 1000,716 Q1120,656 1240,718 Q1350,670 1560,714 L1560,1000 L-120,1000 Z"
        fill="url(#m3)" filter="url(#bleedA)"
      />
      <!-- 三叠主峰皴笔 -->
      <g class="cun" stroke="#22423b" fill="none" stroke-linecap="round">
        <path d="M232,640 Q226,676 214,706" stroke-width="2" opacity=".26" />
        <path d="M244,652 Q252,684 250,712" stroke-width="1.6" opacity=".2" />
        <path d="M600,676 Q592,706 578,730" stroke-width="1.8" opacity=".2" />
        <path d="M628,684 Q636,712 642,736" stroke-width="1.4" opacity=".16" />
        <path d="M1086,690 Q1078,718 1066,742" stroke-width="1.8" opacity=".2" />
      </g>
      <!-- 四叠：最近，墨色最浓 -->
      <path
        d="M-120,782 Q140,708 262,754 Q382,688 502,750 Q622,700 742,762 Q862,704 982,764 Q1102,712 1222,766 Q1342,722 1560,760 L1560,1000 L-120,1000 Z"
        fill="url(#m4)" filter="url(#bleedB)"
      />
      <!-- 四叠湿墨脊线 -->
      <path
        d="M-120,782 Q140,708 262,754 Q382,688 502,750 Q622,700 742,762 Q862,704 982,764 Q1102,712 1222,766 Q1342,722 1560,760"
        stroke="#16302a" stroke-opacity=".38" stroke-width="2.6" fill="none" filter="url(#soft3)"
      />

      <!-- ============ 云雾带（山腰留白） ============ -->
      <!-- 滤镜下放到静态图元上，动画只作用于外层 g，避免逐帧重算模糊 -->
      <g class="fog-a">
        <ellipse cx="380" cy="622" rx="430" ry="34" fill="#f4f6ef" opacity=".78" filter="url(#fog)" />
        <ellipse cx="1060" cy="648" rx="470" ry="30" fill="#f4f6ef" opacity=".72" filter="url(#fog)" />
      </g>
      <g class="fog-b">
        <ellipse cx="700" cy="706" rx="520" ry="30" fill="#f2f5ee" opacity=".7" filter="url(#fog)" />
        <ellipse cx="180" cy="732" rx="300" ry="26" fill="#f2f5ee" opacity=".6" filter="url(#fog)" />
      </g>
      <g class="fog-c">
        <ellipse cx="900" cy="778" rx="540" ry="24" fill="#eef2ec" opacity=".62" filter="url(#fog)" />
        <ellipse cx="360" cy="796" rx="360" ry="20" fill="#eef2ec" opacity=".5" filter="url(#fog)" />
      </g>

      <!-- ============ 水面 ============ -->
      <rect x="-120" y="784" width="1680" height="206" fill="url(#water)" opacity=".88" />
      <!-- 山影倒影 -->
      <g filter="url(#fogS)" opacity=".3">
        <path d="M120,790 Q262,742 380,796 Q502,780 620,806 L620,900 L120,900 Z" fill="#2a4c45" />
        <path d="M760,792 Q900,744 1030,798 Q1150,780 1270,806 L1270,900 L760,900 Z" fill="#2a4c45" />
      </g>
      <!-- 水纹留白 -->
      <g class="ripples" stroke="#fbfcf7" fill="none" stroke-linecap="round">
        <path d="M180,812 Q300,806 430,812" stroke-width="2" opacity=".7" />
        <path d="M520,830 Q660,824 800,830" stroke-width="2" opacity=".6" />
        <path d="M240,846 Q400,840 560,846" stroke-width="1.6" opacity=".5" />
        <path d="M880,822 Q1010,816 1160,822" stroke-width="1.8" opacity=".55" />
        <path d="M960,852 Q1120,846 1290,852" stroke-width="1.6" opacity=".45" />
      </g>
      <!-- 孤舟 -->
      <g class="boat" transform="translate(1006,824)">
        <path d="M0,0 C22,13 62,15 88,2 C74,15 58,19 44,19 C28,19 12,12 0,0 Z" fill="#1b332c" opacity=".62" />
        <path d="M22,-1 C28,-16 50,-20 62,-8 C52,-4 34,-1 22,-1 Z" fill="#1b332c" opacity=".56" />
        <path d="M54,-9 L58,-27" stroke="#1b332c" stroke-width="2" opacity=".48" stroke-linecap="round" />
        <path d="M6,5 C22,11 62,12 84,6" stroke="#f6f8f1" stroke-width="1.6" opacity=".55" fill="none" />
      </g>

      <!-- ============ 近岸坡石 + 芦苇 ============ -->
      <path
        d="M-120,880 Q180,846 420,874 Q660,842 900,872 Q1140,846 1380,876 Q1480,862 1560,872 L1560,1000 L-120,1000 Z"
        fill="url(#bank)" filter="url(#bleedA)"
      />
      <g class="reed" stroke="#12302a" fill="none" stroke-linecap="round" opacity=".62">
        <path d="M1330,884 C1338,850 1334,824 1318,800" stroke-width="2.2" />
        <path d="M1352,886 C1364,856 1370,832 1388,812" stroke-width="2" />
        <path d="M1304,886 C1294,860 1284,838 1266,820" stroke-width="1.7" />
        <path d="M1318,800 C1310,788 1312,776 1320,766" stroke-width="3.6" opacity=".85" />
        <path d="M1388,812 C1384,798 1388,786 1398,778" stroke-width="3.2" opacity=".8" />
        <path d="M1266,820 C1258,808 1258,796 1264,786" stroke-width="2.8" opacity=".7" />
      </g>

      <!-- ============ 竹枝（左侧前景） ============ -->
      <g class="bamboo">
        <path
          d="M-30,150 C46,238 32,398 84,528 C116,606 122,662 118,712"
          stroke="#1c332d" stroke-width="8" fill="none" stroke-linecap="round" opacity=".42"
        />
        <path
          d="M-30,214 C22,292 14,420 56,528"
          stroke="#1c332d" stroke-width="4.5" fill="none" stroke-linecap="round" opacity=".26"
        />
        <g stroke="#1c332d" stroke-width="3" opacity=".3" stroke-linecap="round">
          <path d="M8,268 l16,-4" />
          <path d="M20,352 l15,-3" />
          <path d="M44,436 l15,-3" />
          <path d="M74,520 l16,-3" />
        </g>
        <g fill="#22423b">
          <use href="#inkLeaf" transform="translate(48,300) rotate(-22) scale(1.15)" opacity=".34" />
          <use href="#inkLeaf" transform="translate(56,318) rotate(16) scale(0.95)" opacity=".28" />
          <use href="#inkLeaf" transform="translate(76,404) rotate(-30) scale(1.05)" opacity=".3" />
          <use href="#inkLeaf" transform="translate(88,424) rotate(8) scale(0.85)" opacity=".24" />
          <use href="#inkLeaf" transform="translate(104,498) rotate(-16) scale(1.2)" opacity=".3" />
          <use href="#inkLeaf" transform="translate(112,520) rotate(24) scale(0.9)" opacity=".22" />
          <use href="#inkLeaf" transform="translate(124,588) rotate(-26) scale(1)" opacity=".26" />
        </g>
      </g>

      <!-- ============ 飞鸟点墨 ============ -->
      <g class="birds" stroke="#1f3a34" fill="none" stroke-linecap="round">
        <path d="M892,232 q12,-10 24,0 q12,-10 24,0" stroke-width="2.6" opacity=".5" />
        <path d="M952,262 q10,-8 20,0 q10,-8 20,0" stroke-width="2.2" opacity=".38" />
        <path d="M1006,214 q8,-7 16,0 q8,-7 16,0" stroke-width="1.8" opacity=".3" />
        <path d="M846,286 q7,-6 14,0 q7,-6 14,0" stroke-width="1.6" opacity=".24" />
        <path d="M1040,270 q6,-5 12,0 q6,-5 12,0" stroke-width="1.4" opacity=".2" />
      </g>

      <!-- ============ 墨笔横划（飞白） ============ -->
      <path d="M40,116 Q400,80 770,104 T1400,88" stroke="#24403a" stroke-opacity=".16" stroke-width="8" fill="none" stroke-linecap="round" />
      <path d="M110,144 Q460,116 830,138" stroke="#24403a" stroke-opacity=".1" stroke-width="4" fill="none" stroke-linecap="round" />
      <path d="M900,368 Q1120,346 1360,362" stroke="#24403a" stroke-opacity=".08" stroke-width="3" fill="none" stroke-linecap="round" />

      <!-- ============ 墨点飞溅 ============ -->
      <g class="splatter" fill="#1f3a34">
        <circle cx="668" cy="742" r="3.4" opacity=".2" />
        <circle cx="690" cy="758" r="2" opacity=".16" />
        <circle cx="646" cy="766" r="1.5" opacity=".14" />
        <circle cx="352" cy="806" r="2.8" opacity=".18" />
        <circle cx="374" cy="822" r="1.6" opacity=".13" />
        <circle cx="1188" cy="700" r="2.4" opacity=".16" />
        <circle cx="1206" cy="716" r="1.4" opacity=".12" />
      </g>

      <!-- ============ 朱印落款 ============ -->
      <g class="seal-art" transform="translate(212,790)">
        <rect x="0" y="0" width="46" height="46" rx="4" fill="#b3362b" opacity=".3" />
        <rect x="4.5" y="4.5" width="37" height="37" rx="2" fill="none" stroke="#f8ead8" stroke-opacity=".28" stroke-width="1.4" />
        <text x="23" y="31" text-anchor="middle" font-size="24" fill="#f8ead8" fill-opacity=".42" font-family="STKaiti, KaiTi, serif">烟</text>
      </g>
    </svg>

    <!-- 四角压暗，增加纵深 -->
    <div class="ink-vignette"></div>

    <!-- 飘浮烟雾 -->
    <div class="mist m1"></div>
    <div class="mist m2"></div>
    <div class="mist m3"></div>
    <div class="mist m4"></div>

    <slot />
  </div>
</template>

<style scoped>
.ink-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

/* ---------- 宣纸底 ---------- */
.ink-paper {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1100px 700px at 16% 10%, rgba(255, 255, 255, 0.6), transparent 62%),
    radial-gradient(900px 620px at 86% 6%, rgba(240, 231, 205, 0.5), transparent 64%),
    radial-gradient(1000px 700px at 50% 108%, rgba(206, 222, 214, 0.55), transparent 66%),
    linear-gradient(158deg, #ecefe7 0%, #f5f2e8 32%, #e3ece5 66%, #d3dfd9 100%);
}

/* 纸纹噪点 */
.ink-paper::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='240' height='240'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='240' height='240' filter='url(%23n)' opacity='0.5'/%3E%3C/svg%3E");
  mix-blend-mode: multiply;
  opacity: 0.26;
}

/* 帘纹（横向纤维） */
.ink-paper::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    90deg,
    rgba(120, 140, 130, 0.05) 0px,
    rgba(120, 140, 130, 0.05) 1px,
    transparent 1px,
    transparent 4px
  );
  opacity: 0.5;
}

.ink-scene {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* ---------- 四角压暗 ---------- */
.ink-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 90% at 50% 45%, transparent 52%, rgba(28, 46, 40, 0.16) 100%);
}

/* ---------- 动效 ---------- */
.moon {
  transform-box: fill-box;
  transform-origin: center;
  animation: moon-breathe 9s ease-in-out infinite alternate;
}
@keyframes moon-breathe {
  from { opacity: 0.78; transform: scale(1); }
  to { opacity: 1; transform: scale(1.045); }
}

.birds {
  animation: birds-drift 26s ease-in-out infinite alternate;
}
@keyframes birds-drift {
  from { transform: translate(0, 0); }
  to { transform: translate(-78px, 16px); }
}

/* 云雾横向流动 */
.fog-a { animation: fog-a 34s ease-in-out infinite alternate; }
.fog-b { animation: fog-b 42s ease-in-out infinite alternate; }
.fog-c { animation: fog-c 50s ease-in-out infinite alternate; }
@keyframes fog-a {
  from { transform: translate(-46px, 0); opacity: 0.85; }
  to { transform: translate(52px, -6px); opacity: 1; }
}
@keyframes fog-b {
  from { transform: translate(38px, 4px); opacity: 1; }
  to { transform: translate(-42px, -4px); opacity: 0.8; }
}
@keyframes fog-c {
  from { transform: translate(-30px, 2px); opacity: 0.75; }
  to { transform: translate(44px, -3px); opacity: 0.95; }
}

/* 水纹呼吸 */
.ripples {
  animation: ripple-breathe 6.5s ease-in-out infinite alternate;
}
@keyframes ripple-breathe {
  from { opacity: 0.55; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(10px); }
}

/* 孤舟轻晃 */
.boat {
  transform-box: fill-box;
  transform-origin: 50% 100%;
  animation: boat-bob 7.5s ease-in-out infinite alternate;
}
@keyframes boat-bob {
  from { transform: translate(1006px, 824px) rotate(-1.1deg); }
  to { transform: translate(1022px, 830px) rotate(1.1deg); }
}

/* 竹枝摇曳 */
.bamboo {
  transform-box: fill-box;
  transform-origin: 8% 0%;
  animation: bamboo-sway 12s ease-in-out infinite alternate;
}
@keyframes bamboo-sway {
  from { transform: rotate(-0.7deg); }
  to { transform: rotate(0.9deg); }
}

/* 芦苇拂动 */
.reed {
  transform-box: fill-box;
  transform-origin: 50% 100%;
  animation: reed-sway 6s ease-in-out infinite alternate;
}
@keyframes reed-sway {
  from { transform: rotate(-1.6deg) skewX(-1deg); }
  to { transform: rotate(1.6deg) skewX(1deg); }
}

/* 墨点轻微呼吸 */
.splatter {
  animation: splatter-pulse 8s ease-in-out infinite alternate;
}
@keyframes splatter-pulse {
  from { opacity: 0.75; }
  to { opacity: 1; }
}

.seal-art {
  animation: seal-pulse 11s ease-in-out infinite alternate;
}
@keyframes seal-pulse {
  from { opacity: 0.7; }
  to { opacity: 1; }
}

/* ---------- 飘浮烟雾 ---------- */
.mist {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.35;
  pointer-events: none;
  animation: drift 18s ease-in-out infinite alternate;
}

.mist.m1 {
  width: 540px;
  height: 540px;
  background: #9ec2b9;
  top: -140px;
  left: -140px;
  opacity: 0.32;
}

.mist.m2 {
  width: 440px;
  height: 440px;
  background: #b6d3cb;
  bottom: -150px;
  right: 26%;
  animation-delay: -6s;
  opacity: 0.3;
}

.mist.m3 {
  width: 320px;
  height: 320px;
  background: #e6d9bd;
  top: 28%;
  right: -110px;
  animation-delay: -12s;
  opacity: 0.26;
}

.mist.m4 {
  width: 380px;
  height: 380px;
  background: #cbdfd8;
  top: 52%;
  left: -130px;
  animation-delay: -3s;
  opacity: 0.24;
}

/* 只做位移不做缩放：blur 图层缩放会逐帧重算，位移可走合成层 */
@keyframes drift {
  from { transform: translate(0, 0); }
  to { transform: translate(64px, 34px); }
}

@media (max-width: 960px) {
  .mist { filter: blur(44px); }
}
</style>
