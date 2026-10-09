<script setup>
import { ref, watch } from 'vue'
import Icon from '@/components/Icon.vue'

/**
 * 空态。
 *
 * 两种视觉：
 *   · 传了 `image` 且该插图存在 → 内联渲染 `photo/illustration/<image>.svg`（主色跟随 --m-accent）
 *   · 否则 → 原来的 64px 线性图标方块（绝大多数页面走这条，且**不会**下载插图资源）
 *
 * 插图**按张懒加载**：一个页面通常只展示一张，就只下载那一张（几 KB），
 * 不传 `image` 的页面一张都不会下。
 */
const props = defineProps({
  icon: { type: String, default: 'box' },
  title: { type: String, default: '暂无数据' },
  desc: { type: String, default: '' },
  /** 插图名（photo/illustration 下的文件名，不含 .svg），如 empty-cart / empty-order */
  image: { type: String, default: '' }
})

const art = ref('')

watch(
  () => props.image,
  async (name) => {
    if (!name) {
      art.value = ''
      return
    }
    const { loadIllustration } = await import('@/lib/illustrations')
    const svg = await loadIllustration(name)
    // 拉图期间可能已经换图/卸载，回写前再确认一次
    if (props.image === name) art.value = svg
  },
  { immediate: true }
)
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html — 本地静态 SVG，非用户输入 -->
  <div class="m-empty">
    <span v-if="art" class="m-empty-art" v-html="art" />
    <span v-else class="m-empty-ico"><Icon :name="icon" :size="26" /></span>
    <p class="m-h3">{{ title }}</p>
    <p v-if="desc" class="m-muted" style="max-width: 420px; font-size: 13px">{{ desc }}</p>
    <slot />
  </div>
</template>
