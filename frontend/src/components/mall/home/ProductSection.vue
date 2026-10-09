<script setup>
import { RouterLink } from 'vue-router'
import MSection from '@/components/mall/MSection.vue'
import MProductCard from '@/components/mall/MProductCard.vue'
import MEmpty from '@/components/mall/MEmpty.vue'

/**
 * 商品推荐区 —— 风格提示第六节的节奏：
 *   标题 → 横向商品列表 / Grid → 商品卡片
 *
 * layout='grid'  → 桌面 4 列 / 平板 3 列 / 手机 2 列
 * layout='track' → 横向轨道，移动端露出下一张的一半做引导
 */
const props = defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  products: { type: Array, default: () => [] },
  /** 「更多」链接的目标；给了才渲染 */
  moreTo: { type: [String, Object], default: null },
  moreLabel: { type: String, default: '查看全部' },
  loading: { type: Boolean, default: false },
  layout: { type: String, default: 'grid' },
  emptyIcon: { type: String, default: 'box' },
  skeletonCount: { type: Number, default: 4 }
})
const emit = defineEmits(['open', 'add'])

const containerClass = () => (props.layout === 'track' ? 'm-track' : 'm-pgrid')
</script>

<template>
  <section class="m-wrap m-wrap-mid m-section">
    <MSection :eyebrow="eyebrow" :title="title" :desc="desc">
      <template #right>
        <RouterLink v-if="moreTo" :to="moreTo" class="m-btn-link">{{ moreLabel }}</RouterLink>
      </template>
    </MSection>

    <div v-if="products.length" :class="containerClass()">
      <MProductCard
        v-for="p in products"
        :key="p.id"
        :product="p"
        :off="p.status === 0"
        local-image
        show-rating
        @open="emit('open', $event)"
        @add="emit('add', $event)"
      />
    </div>

    <div v-else-if="loading" :class="containerClass()">
      <div v-for="i in skeletonCount" :key="i" class="m-skeleton" style="aspect-ratio: 4 / 5" />
    </div>

    <MEmpty image="empty-data" v-else :icon="emptyIcon" :title="`暂无${title}`" desc="稍后再来看看" />
  </section>
</template>
