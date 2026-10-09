<script setup>
import { computed } from 'vue'
import { portalBackground } from '@/lib/brand'

/**
 * 环境背景 —— 全站共用（主页面 / 登录注册 / 商城 / 外卖 / 各工作台 / 404）。
 *
 * 两种形态：
 *   ① 有 `photo/portal-bg.jpg`  →  整页铺这张图（cover），上面叠一层纵向渐变白纱保证文字可读；
 *   ② 没有这张图              →  退回原来的品牌蓝柔光（三团 blob）。
 *
 * 默认不虚化（同一层白纱）；**虚化按外壳分档**（2026-09-21 拍板保留分档设计）：
 *   不虚化 —— 主页面 / 登录 / 注册 / 商家入驻
 *   6px   —— 404
 *   8px   —— 商城端（ShopLayout）/ 外卖端（FoodLayout）——商品图本来就多，背景再清楚会抢目光
 *   10px  —— 工作台四端（WorkbenchLayout）——密集表格页，背景压淡一档最好读
 * 改档位时同步：`scripts/ui-smoke-backdrop.mjs` 的 expectBlur + `docs/项目进度.md` #77 的档位表。
 *
 * 用 `position: fixed`：长页面滚动时图停在视口里，不会滚出去。
 * 内容层只要 `position: relative + z-index: 1` 就能压在它上面。
 */
const props = defineProps({
  /** 背景图虚化像素；0 = 不虚化（默认）。>0 时自动放大一点，虚化的边缘不会露出来 */
  blur: { type: Number, default: 0 },
  /** 覆盖白纱强度 [上, 中, 下] 三个透明度；不传用默认档 */
  veil: { type: Array, default: null }
})

const DEFAULT_VEIL = [0.62, 0.3, 0.72]

const hasImage = Boolean(portalBackground)

const imgStyle = computed(() =>
  props.blur > 0 ? { filter: `blur(${props.blur}px)`, transform: 'scale(1.06)' } : null
)
const veilStyle = computed(() => {
  const v = Array.isArray(props.veil) && props.veil.length === 3 ? props.veil : DEFAULT_VEIL
  return {
    background: `linear-gradient(180deg, rgba(255,255,255,${v[0]}) 0%, rgba(255,255,255,${v[1]}) 42%, rgba(255,255,255,${v[2]}) 100%)`
  }
})
</script>

<template>
  <div class="backdrop" :class="{ 'has-img': hasImage }" aria-hidden="true">
    <img
      v-if="hasImage"
      class="bd-img"
      :src="portalBackground"
      alt=""
      fetchpriority="high"
      decoding="async"
      :style="imgStyle"
    />
    <span class="bd-veil" :style="veilStyle" />

    <!-- 没有背景图时，退回品牌蓝柔光 -->
    <template v-if="!hasImage">
      <span class="blob b1" />
      <span class="blob b2" />
      <span class="blob b3" />
    </template>
  </div>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

/* ---------- 背景图 + 白纱（虚化档位由外壳传 blur 决定） ---------- */
.bd-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  /* 个别页面会临时加虚化（如商城/外卖首页），加个过渡免得路由切换时跳一下 */
  transition: filter 0.35s ease, transform 0.35s ease;
}
.bd-veil {
  position: absolute;
  inset: 0;
  /* 实际值由 :style 注入（默认档 0.62 / 0.3 / 0.72）：上/下压重给字标与页脚让路，中间留透 */
}

/* 小屏不做虚化：大尺寸 fixed + blur 在手机上栅格化很贵（!important 压掉行内样式） */
@media (max-width: 900px) {
  .bd-img {
    filter: none !important;
    transform: none !important;
  }
}

/* ---------- 兜底：品牌蓝柔光（没有背景图时） ---------- */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
}
.b1 {
  width: 50vw;
  height: 50vw;
  left: -14vw;
  top: -18vw;
  background: #bcd0ff;
}
.b2 {
  width: 42vw;
  height: 42vw;
  right: -12vw;
  top: 2vh;
  background: #d6e3ff;
}
.b3 {
  width: 62vw;
  height: 42vw;
  left: 42%;
  bottom: -20vw;
  transform: translateX(-50%);
  background: #a9c4ff;
}
</style>
