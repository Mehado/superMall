<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import MSection from '@/components/mall/MSection.vue'
import CategoryCard from './CategoryCard.vue'

/**
 * 「按分类逛」区块 —— 风格提示第五节
 *
 * 取哪些分类：先顶级，再用二级补齐。
 * 种子数据只有 3 个顶级分类（服饰鞋包 / 食品饮料 / 数码配件），
 * 只铺顶级会让 4 列网格空掉一格以上，看起来像加载失败；
 * 用二级分类补到 limit 个刚好铺满。
 */
const props = defineProps({
  /** GET /catalog/categories 返回的顶级列表（children 已嵌套） */
  categories: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  limit: { type: Number, default: 8 }
})

const items = computed(() => {
  const pool = [...props.categories, ...props.categories.flatMap((c) => c.children || [])]
  const seen = new Set()
  const out = []
  for (const c of pool) {
    if (!c || seen.has(c.id)) continue
    seen.add(c.id)
    out.push(c)
    if (out.length >= props.limit) break
  }
  return out
})

const showSection = computed(() => props.loading || items.value.length > 0)
</script>

<template>
  <section v-if="showSection" class="m-wrap m-wrap-mid m-section">
    <MSection eyebrow="Browse" title="按分类逛" desc="每一类都带在售商品数量">
      <template #right>
        <RouterLink to="/shop/search" class="m-btn-link">全部分类</RouterLink>
      </template>
    </MSection>

    <div v-if="items.length" class="m-cats">
      <CategoryCard v-for="(c, i) in items" :key="c.id" :category="c" :index="i" />
    </div>
    <div v-else class="m-cats">
      <div v-for="i in limit" :key="i" class="m-skeleton" style="aspect-ratio: 4 / 5" />
    </div>
  </section>
</template>
