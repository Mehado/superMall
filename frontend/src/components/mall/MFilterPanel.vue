<script setup>
import { reactive, watch } from 'vue'
import Icon from '@/components/Icon.vue'

const props = defineProps({
  flat: { type: Array, default: () => [] },
  brands: { type: Array, default: () => [] },
  categoryId: { type: [String, Number], default: '' },
  brandId: { type: [String, Number], default: '' },
  minPrice: { type: [String, Number], default: '' },
  maxPrice: { type: [String, Number], default: '' }
})
const emit = defineEmits(['patch', 'reset'])

const price = reactive({ min: props.minPrice || '', max: props.maxPrice || '' })

watch(
  () => [props.minPrice, props.maxPrice],
  ([a, b]) => {
    price.min = a || ''
    price.max = b || ''
  }
)

function applyPrice() {
  emit('patch', { minPrice: price.min, maxPrice: price.max, page: 1 })
}
</script>

<template>
  <div class="m-col" style="gap: 28px">
    <div>
      <p class="m-eyebrow m-mb-3">分类</p>
      <div class="m-col" style="gap: 2px">
        <button
          type="button"
          class="filter-link"
          :class="{ active: !categoryId }"
          @click="emit('patch', { categoryId: '', page: 1 })"
        >
          全部分类
        </button>
        <button
          v-for="c in flat"
          :key="c.id"
          type="button"
          class="filter-link"
          :class="{ active: String(categoryId) === String(c.id) }"
          :style="{ paddingLeft: 12 + c.depth * 14 + 'px' }"
          @click="emit('patch', { categoryId: c.id, page: 1 })"
        >
          <span class="m-ellipsis">{{ c.name }}</span>
          <span v-if="c.productCount" class="m-dim m-num" style="margin-left: auto">{{ c.productCount }}</span>
        </button>
        <p v-if="!flat.length" class="m-dim">暂无分类</p>
      </div>
    </div>

    <div>
      <p class="m-eyebrow m-mb-3">品牌</p>
      <div class="m-row m-wrapflex" style="gap: 8px">
        <button
          v-for="b in brands"
          :key="b.id"
          type="button"
          class="m-chip"
          :class="{ 'm-chip-active': String(brandId) === String(b.id) }"
          @click="
            emit('patch', {
              brandId: String(brandId) === String(b.id) ? '' : b.id,
              page: 1
            })
          "
        >
          {{ b.name }}
        </button>
        <span v-if="!brands.length" class="m-dim">暂无品牌</span>
      </div>
    </div>

    <div>
      <p class="m-eyebrow m-mb-3">价格区间</p>
      <div class="m-row" style="gap: 8px">
        <input v-model="price.min" class="m-input" placeholder="最低" inputmode="numeric" />
        <span class="m-muted">—</span>
        <input v-model="price.max" class="m-input" placeholder="最高" inputmode="numeric" />
      </div>
      <button class="m-btn m-btn-primary m-btn-block m-mt-3" type="button" @click="applyPrice">
        应用价格
      </button>
    </div>

    <button class="m-btn m-btn-soft m-btn-block" type="button" @click="emit('reset')">
      <Icon name="refresh" :size="15" />
      重置全部筛选
    </button>
  </div>
</template>

<style scoped>
.filter-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  padding: 8px 12px;
  border-radius: var(--m-r-xs);
  font-size: 13px;
  color: var(--m-ink-2);
  text-align: left;
  transition: background-color 0.2s var(--m-ease), color 0.2s var(--m-ease);
}
.filter-link:hover {
  background: var(--m-bone-deep);
  color: var(--m-ink);
}
.filter-link.active {
  background: var(--m-ink);
  color: var(--m-paper);
  font-weight: 600;
}
.filter-link.active .m-dim {
  color: rgba(255, 255, 255, 0.55);
}
</style>
