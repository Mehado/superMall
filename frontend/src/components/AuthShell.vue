<script setup>
import { RouterLink } from 'vue-router'
import AppBackdrop from '@/components/AppBackdrop.vue'
import { brandWordmark } from '@/lib/brand'

/**
 * 登录 / 注册共用外壳
 *
 * 版式：左右平分整页
 *   左：SuperMall 字标（大）+ 标语
 *   右：玻璃卡片，内容由默认插槽给
 * 背景是品牌蓝柔光，玻璃卡片叠在上面才有折射感。
 */
</script>

<template>
  <div class="auth">
    <AppBackdrop />

    <div class="auth-inner">
      <section class="auth-brand">
        <RouterLink to="/" class="auth-logo" aria-label="返回主页面">
          <img v-if="brandWordmark" class="auth-wordmark" :src="brandWordmark" alt="SuperMall" />
          <span v-else class="auth-wordmark-text">SuperMall</span>
        </RouterLink>
        <p class="auth-tagline">想买的、想吃的，一个地方全搞定。</p>
        <p v-if="$slots.hint" class="auth-hint"><slot name="hint" /></p>
      </section>

      <section class="auth-panel">
        <div class="auth-glass"><slot /></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #fff;
  color: var(--m-ink, #16130f);
}

/* ── 品牌蓝柔光背景 ───────────────────────────────────── */
/* 背景由 components/AppBackdrop.vue 提供 */

/* ── 左右平分整页 ─────────────────────────────────────── */
.auth-inner {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  align-items: center;
  gap: 48px;
  padding: 44px clamp(24px, 4vw, 72px);
}

/* 左：字标 */
.auth-brand {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.auth-logo {
  display: block;
}
.auth-wordmark {
  width: min(100%, 640px);
  height: auto;
  display: block;
  animation: auth-rise 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.auth-wordmark-text {
  font-size: clamp(60px, 9vw, 150px);
  font-weight: 700;
  letter-spacing: -0.045em;
  line-height: 1;
  color: var(--m-accent, #003de2);
}
.auth-tagline {
  font-size: clamp(17px, 1.7vw, 26px);
  font-weight: 600;
  letter-spacing: -0.01em;
}
.auth-hint {
  color: var(--m-taupe, #8a7c6b);
  font-size: 14px;
  max-width: 40ch;
}

/* 右：玻璃卡片 */
.auth-panel {
  display: flex;
  justify-content: center;
}
.auth-glass {
  width: 100%;
  max-width: 460px;
  padding: 34px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.55);
  -webkit-backdrop-filter: blur(22px) saturate(1.7);
  backdrop-filter: blur(22px) saturate(1.7);
  border: 1px solid rgba(255, 255, 255, 0.78);
  box-shadow: 0 30px 70px -34px rgba(0, 61, 226, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

@keyframes auth-rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 960px) {
  .auth-inner {
    grid-template-columns: minmax(0, 1fr);
    align-content: center;
    gap: 30px;
    padding: 40px 22px;
  }
  .auth-brand {
    align-items: center;
    text-align: center;
  }
  .auth-wordmark {
    margin: 0 auto;
  }
  .auth-hint {
    max-width: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .auth-wordmark {
    animation: none !important;
  }
}
</style>
