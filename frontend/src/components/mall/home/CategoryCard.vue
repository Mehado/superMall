<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import MImageFrame from '@/components/mall/MImageFrame.vue'
import { pickFromPool, resolveImage } from '@/lib/mall-images'

/**
 * 视觉化分类卡 —— 风格提示第五节的版式：
 *
 *   ┌────────────┐
 *   │    图片     │   ← 统一 4:5
 *   ├────────────┤
 *   │  Beauty    │   ← 分类名
 *   └────────────┘
 *
 * 取图优先用后端返回的 category.icon，没有才回落到 photo/category/。
 * 按分类 id 取图（而不是列表下标），保证同一个分类永远是同一张。
 */
const props = defineProps({
  category: { type: Object, required: true },
  index: { type: Number, default: 0 }
})

const slot = computed(() => props.category.id ?? props.index)
const image = computed(() => resolveImage(props.category, 'category', slot.value))
</script>

<template>
  <RouterLink
    class="m-catcard"
    :to="{ path: '/shop/search', query: { categoryId: category.id } }"
  >
    <MImageFrame
      :src="image"
      :fallback="pickFromPool('category', slot)"
      :alt="category.name"
      ratio="4 / 5"
    />
    <span class="m-catcard-name">{{ category.name }}</span>
    <span class="m-catcard-meta">{{ category.productCount ?? 0 }} 件商品</span>
  </RouterLink>
</template>
