<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Icon from '@/components/Icon.vue'
import AppBackdrop from '@/components/AppBackdrop.vue'
import { loadIllustration } from '@/lib/illustrations'

/** 404 也铺环境背景，但压淡一点，别抢「404」三个字 */
const VEIL = [0.8, 0.6, 0.88]

/** 插图：photo/illustration/not-found.svg（按需加载；主色已换成 currentColor，跟着 --m-accent 走） */
const art = ref('')
onMounted(async () => {
  art.value = await loadIllustration('not-found')
})
</script>

<template>
  <div class="nf">
    <AppBackdrop :blur="6" :veil="VEIL" />

    <div class="nf-inner">
      <span v-if="art" class="nf-art" v-html="art" />
      <span class="nf-code">404</span>
      <h1 class="m-h2">页面不存在</h1>
      <p class="m-muted nf-desc">地址可能写错了，或者这个端还没有对应页面。</p>

      <div class="nf-actions">
        <RouterLink to="/" class="m-btn m-btn-primary m-btn-lg">
          <Icon name="left" :size="16" />
          返回主页面
        </RouterLink>
        <RouterLink to="/shop" class="m-btn m-btn-outline m-btn-lg">去商城</RouterLink>
        <RouterLink to="/food" class="m-btn m-btn-outline m-btn-lg">去外卖</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.nf {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 40px 22px;
  background: #fff;
  color: var(--m-ink, #16130f);
}
.nf-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  max-width: 520px;
  text-align: center;
}
.nf-code {
  font-size: clamp(64px, 9vw, 128px);
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.06em;
  color: var(--m-accent, #003de2);
  margin-bottom: 6px;
  animation: nf-rise 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.nf-desc {
  font-size: 14px;
}
.nf-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}

.nf-art {
  display: block;
  width: min(360px, 78vw);
  margin-bottom: 2px;
  color: var(--m-accent, #003de2);
  animation: nf-rise 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.nf-art svg {
  display: block;
  width: 100%;
  height: auto;
}

@keyframes nf-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@media (prefers-reduced-motion: reduce) {
  .nf-code {
    animation: none !important;
  }
}
</style>
