<script setup>
import { computed, ref, watch } from 'vue'
import Icon from '@/components/Icon.vue'

/**
 * MImageFrame —— 统一图片容器（风格提示第十二节）
 *
 * 只做三件事：
 *   1. 用 aspect-ratio 把**图片比例钉死**，无论有没有图，占位尺寸都一致
 *      → 这就是「将来换成真图不用改布局」的实现方式
 *   2. 没图时渲染中性色占位块（不用渐变、不用彩色，保持克制）
 *   3. 有图时统一 object-fit + hover 轻微缩放
 *
 * 三级降级（种子里 banner 的 OSS 地址是**不存在的占位地址**，必须有这一层）：
 *   主图 src → 加载失败 → 兜底图 fallback → 再失败 → 占位块
 *
 * 样式在 mall.css 的 .m-imgframe 区块，与本项目「设计系统集中管理」的约定一致。
 */
const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  /** 主图加载失败时的备用地址，通常传本地图池 */
  fallback: { type: String, default: '' },
  /** CSS aspect-ratio 值，如 '4 / 5'、'16 / 10'、'1 / 1' */
  ratio: { type: String, default: '4 / 5' },
  /** 圆角令牌或具体值 */
  radius: { type: String, default: 'var(--m-r)' },
  fit: { type: String, default: 'cover' },
  /** 是否在 hover 时轻微缩放（由宿主卡片决定，默认关，避免与卡片自身的 hover 冲突） */
  zoom: { type: Boolean, default: false },
  /** 占位块上的说明文字，留空则只显示图标 */
  hint: { type: String, default: '' }
})

/**
 * 候选地址列表，按顺序尝试，越界即渲染占位块。
 * 去重是必须的：主图与兜底图相同时若不去重，第一次失败后 src 不变、
 * 浏览器不会再触发 error，就会卡在碎图上。
 */
const candidates = computed(() => [...new Set([props.src, props.fallback].filter(Boolean))])
const idx = ref(0)
watch(
  () => props.src,
  () => (idx.value = 0)
)

const shown = computed(() => candidates.value[idx.value] || '')

function onError() {
  idx.value += 1
}
</script>

<template>
  <div
    class="m-imgframe"
    :class="{ 'is-zoom': zoom, 'is-empty': !shown }"
    :style="{ aspectRatio: ratio, borderRadius: radius }"
  >
    <img
      v-if="shown"
      :src="shown"
      :alt="alt"
      loading="lazy"
      :style="{ objectFit: fit }"
      @error="onError"
    />
    <div v-else class="m-imgframe-ph">
      <Icon name="image" :size="22" />
      <span v-if="hint" class="m-imgframe-hint">{{ hint }}</span>
    </div>
    <!-- 叠层内容：图上文字、角标、遮罩等（容器已是 position: relative） -->
    <slot />
  </div>
</template>
