<script setup>
/**
 * 落地页（首页）
 * 水墨英雄区：左侧文案 + 右侧「离恨烟」立绘
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { useReveal } from '@/composables/useReveal'

const router = useRouter()

const root = ref(null)
useReveal(root)

const copy = {
  badge: '🌂 离恨烟 · 官方社区',
  titleShine: '离恨烟',
  verse: '烟水茫茫 · 离恨绵绵 伞落惊鸿',
  desc: '一伞烟雨，一梦江湖。与阿离同游水墨山水之间，登录即可开启你的伞下江湖，记录每一次惊鸿照影。',
  primary: '进入江湖',
  ghost: '先逛逛',
  stats: [
    { value: '1280', label: '江湖侠客' },
    { value: '365', label: '烟雨日程' },
    { value: '99%', label: '风雅指数' },
  ],
  features: [
    { ic: '🌂', title: '油纸伞', desc: '领取属于你的油纸伞，伞面之上丹青自绘，行走烟雨之间。' },
    { ic: '🏅', title: '江湖徽章', desc: '完成每日行侠任务，解锁鎏金徽章与朱砂印鉴。' },
    { ic: '📖', title: '行侠日志', desc: '以笔墨记录每一次伞下奇遇与山水相逢。' },
    { ic: '👫', title: '侠客同游', desc: '结识四方侠客，共赏青绿山水，同赴惊鸿之约。' },
  ],
  sectionTitle: '伞 下 江 湖',
  sectionSub: '烟雨朦胧处，与君共撑一伞',
}

onMounted(() => {
  // 保证从其它页返回时揭示动画正常重放
  root.value?.querySelectorAll('.reveal').forEach((el) => el.classList.remove('in'))
})
</script>

<template>
  <section ref="root" class="view landing active">
    <div class="hero">
      <div class="hero-copy">
        <div class="badge reveal">{{ copy.badge }}</div>

        <h1 class="reveal">
          <span class="shine">{{ copy.titleShine }}</span>
        </h1>

        <p class="verse reveal">{{ copy.verse }}</p>

        <p class="desc reveal">{{ copy.desc }}</p>

        <div class="cta reveal">
          <button class="btn primary" @click="router.push({ name: 'login' })">
            {{ copy.primary }}
          </button>
          <button class="btn ghost" @click="router.push({ name: 'home' })">
            {{ copy.ghost }}
          </button>
        </div>

        <div class="stats reveal">
          <div v-for="s in copy.stats" :key="s.label" class="stat">
            <b>{{ s.value }}</b><span>{{ s.label }}</span>
          </div>
        </div>
      </div>

      <div class="hero-figure reveal">
        <img src="/lihengyan.jpg" alt="离恨烟" />
        <div class="fig-seal seal-mark">离恨烟</div>
      </div>
    </div>

    <div class="features">
      <h2 class="section-title reveal">{{ copy.sectionTitle }}</h2>
      <p class="section-sub reveal">{{ copy.sectionSub }}</p>
      <div class="fgrid">
        <div v-for="f in copy.features" :key="f.title" class="fcard reveal">
          <div class="ic">{{ f.ic }}</div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
