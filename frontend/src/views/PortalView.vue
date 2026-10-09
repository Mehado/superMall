<script setup>
import { RouterLink } from 'vue-router'
import Icon from '@/components/Icon.vue'
import AppBackdrop from '@/components/AppBackdrop.vue'
import { brandWordmark } from '@/lib/brand'

/**
 * 主页面（分流入口）
 *
 * 版式：整页分上下两块
 *   上半：SuperMall 字标（用 photo/SuperMall.png），居中放大
 *   下半：三个玻璃质感大按钮 —— 商城 / 外卖 / 登录，左右排列
 *
 * 玻璃要「透」得出来，所以背景铺了几团品牌蓝的柔光（.blob），按钮叠在上面才有折射感。
 *
 * 背景由 components/AppBackdrop.vue 统一提供（全站同一张图，主页面用 hero 档：图最清楚）。
 */
const ENTRIES = [
  { to: '/shop', label: '商城', desc: '逛商品、下单、查物流', icon: 'cart' },
  { to: '/food', label: '外卖', desc: '附近好店，热乎到家', icon: 'truck' },
  { to: '/login', label: '登录', desc: '买家 / 商家 / 骑手 / 运营', icon: 'user' }
]

/**
 * 商家端、骑手端与运营后台都从登录入口按身份进入；
 * 尚未开户的商家 / 骑手走公开的入驻与注册页（不需要先登录）。
 */
const roleLinks = [
  { to: { path: '/login', query: { role: 'mall-admin' } }, label: '商城运营后台' },
  { to: { path: '/login', query: { role: 'merchant' } }, label: '外卖商家端' },
  { to: { path: '/login', query: { role: 'rider' } }, label: '外卖骑手端' },
  { to: '/merchant/apply', label: '商家入驻' },
  { to: '/rider/register', label: '骑手注册' }
]
</script>

<template>
  <div class="portal">
    <AppBackdrop />

    <!-- 上：艺术字 -->
    <section class="portal-top">
      <img v-if="brandWordmark" class="portal-wordmark" :src="brandWordmark" alt="SuperMall" />
      <h1 v-else class="portal-wordmark-text">SuperMall</h1>
      <p class="portal-tagline">想买的、想吃的，一个地方全搞定。</p>
    </section>

    <!-- 下：三个玻璃大按钮 -->
    <section class="portal-bottom">
      <nav class="entry-row" aria-label="选择入口">
        <RouterLink v-for="e in ENTRIES" :key="e.to" :to="e.to" class="glass-btn">
          <span class="glass-ico"><Icon :name="e.icon" :size="26" /></span>
          <span class="glass-label">{{ e.label }}</span>
          <span class="glass-desc">{{ e.desc }}</span>
        </RouterLink>
      </nav>

      <div class="portal-foot">
        <span class="portal-copy">© {{ new Date().getFullYear() }} superMall</span>
        <span class="portal-roles">
          <RouterLink v-for="l in roleLinks" :key="l.label" :to="l.to">{{ l.label }}</RouterLink>
        </span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.portal {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-rows: 1.1fr 0.9fr;
  overflow: hidden;
  background: #fff;
  color: var(--m-ink, #16130f);
}

/* 背景是照片时，玻璃按钮的底色提亮一点，折射感更明显 */
.portal .glass-btn {
  background: rgba(255, 255, 255, 0.55);
}

/* ── 上：艺术字 ───────────────────────────────────────── */
.portal-top {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 6vh 24px 2vh;
  text-align: center;
}
.portal-wordmark {
  width: clamp(280px, 58vw, 800px);
  height: auto;
  max-width: 92vw;
  display: block;
  animation: portal-rise 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.portal-wordmark-text {
  font-size: clamp(56px, 13vw, 180px);
  font-weight: 700;
  letter-spacing: -0.045em;
  line-height: 1;
  color: var(--m-accent, #003de2);
}
.portal-tagline {
  color: var(--m-taupe, #8a7c6b);
  font-size: clamp(13px, 1.4vw, 16px);
  letter-spacing: 0.04em;
}

/* ── 下：玻璃按钮 ─────────────────────────────────────── */
.portal-bottom {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 2vh 24px 5vh;
}
.entry-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 24px;
  /* 给一个确定宽度，否则在 column 父级里会被算成 fit-content 导致换行 */
  width: 100%;
  max-width: 1000px;
}
.glass-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: min(280px, 100%);
  padding: 34px 26px;
  border-radius: 26px;
  text-align: center;
  /* 玻璃：半透白底 + 背景模糊 + 高光边 */
  background: rgba(255, 255, 255, 0.5);
  -webkit-backdrop-filter: blur(22px) saturate(1.7);
  backdrop-filter: blur(22px) saturate(1.7);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: 0 26px 60px -32px rgba(0, 61, 226, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1),
    background-color 0.35s ease, box-shadow 0.35s ease;
}
.glass-btn:hover {
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 40px 80px -34px rgba(0, 61, 226, 0.6), inset 0 1px 0 #fff;
}
.glass-btn:active {
  transform: translateY(-2px);
}
.glass-ico {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: rgba(0, 61, 226, 0.1);
  color: var(--m-accent, #003de2);
  transition: background-color 0.35s ease, color 0.35s ease;
}
.glass-btn:hover .glass-ico {
  background: var(--m-accent, #003de2);
  color: #fff;
}
.glass-label {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.glass-desc {
  font-size: 13px;
  color: var(--m-taupe, #8a7c6b);
}

/* ── 底部小字 ─────────────────────────────────────────── */
.portal-foot {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  justify-content: center;
  font-size: 12px;
  color: var(--m-faint, #b3a99c);
}
.portal-roles {
  display: flex;
  align-items: center;
  gap: 16px;
}
.portal-roles a {
  color: var(--m-taupe, #8a7c6b);
  transition: color 0.2s ease;
}
.portal-roles a:hover {
  color: var(--m-accent, #003de2);
}

@keyframes portal-rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 720px) {
  .portal {
    grid-template-rows: auto 1fr;
  }
  .portal-top {
    padding-top: 12vh;
  }
  .entry-row {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    max-width: 320px;
  }
  .glass-btn {
    width: 100%;
    padding: 24px 20px;
  }
  .glass-label {
    font-size: 20px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .portal-wordmark,
  .glass-btn,
  .glass-ico {
    animation: none !important;
    transition: none !important;
  }
}
</style>
