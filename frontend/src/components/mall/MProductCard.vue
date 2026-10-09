<script setup>
import { computed, ref } from 'vue'
import Icon from '@/components/Icon.vue'
import MStars from './MStars.vue'
import { money } from '@/utils/format'
import { priceRange } from '@/utils/mall-format'
import { productImage } from '@/lib/mall-images'

const props = defineProps({
  product: { type: Object, required: true },
  showAdd: { type: Boolean, default: true },
  off: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
  /** 打开后：后端没给主图时回落到 photo/product/ 本地图池（首页在用，默认关，不影响其他页面） */
  localImage: { type: Boolean, default: false },
  /** 打开后：星级旁显示「4.8 · 128」——风格提示第六节要求卡片带评分与评价数量 */
  showRating: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'add'])
const added = ref(false)

const image = computed(() => {
  const remote = props.product.image || props.product.mainImage || props.product.images?.[0] || ''
  // 电商接口文档 3.3：mainImage「可能为 null（前端必须占位）」，所以这里必须有兜底
  return remote || (props.localImage ? productImage(props.product.id) : '')
})
const rating = computed(() => Number(props.product.rating ?? props.product.score ?? 0))
const reviewCount = computed(
  () => props.product.reviewCount ?? props.product.reviews ?? props.product.commentCount ?? 0
)
const discount = computed(() => {
  const origin = Number(props.product.originalPrice)
  const now = Number(props.product.price)
  if (!Number.isFinite(origin) || origin <= now || origin <= 0) return 0
  return Math.round(((origin - now) / origin) * 100)
})
const price = computed(() => priceRange(props.product))
const priceText = computed(() => {
  if (price.value.low === null) return '价格待定'
  return price.value.range
    ? `${money(price.value.low)} – ${money(price.value.high)}`
    : money(price.value.low)
})

function handleAdd() {
  if (props.off || props.busy) return
  emit('add', props.product)
  added.value = true
  setTimeout(() => (added.value = false), 1200)
}
</script>

<template>
  <article class="m-pcard" @click="emit('open', product)">
    <div class="m-pcard-media">
      <img v-if="image" :src="image" :alt="product.name" loading="lazy" />
      <div v-else class="m-pcard-placeholder"><Icon name="image" :size="30" /></div>

      <span v-if="product.tag" class="m-badge m-badge-ink m-pcard-badge">{{ product.tag }}</span>
      <span v-if="discount > 0" class="m-badge m-badge-accent m-pcard-save">省 {{ discount }}%</span>
      <div v-if="off" class="m-pcard-off">已下架</div>
    </div>

    <div class="m-pcard-body">
      <span v-if="product.brandName" class="m-pcard-brand">{{ product.brandName }}</span>
      <h3 class="m-pcard-name m-clamp-2">{{ product.name }}</h3>

      <MStars
        v-if="Number(reviewCount) > 0"
        :value="rating"
        :count="reviewCount"
        :show-value="showRating"
      />
      <span v-else class="m-dim">暂无评价</span>

      <div class="m-pcard-foot">
        <div>
          <span class="m-pcard-price">{{ priceText }}</span>
          <span v-if="discount > 0" class="m-strike" style="margin-left: 6px">
            {{ money(product.originalPrice) }}
          </span>
        </div>
        <button
          v-if="showAdd"
          class="m-add-btn"
          :class="{ done: added }"
          type="button"
          :disabled="off || busy"
          :aria-label="off ? '已下架' : '加入购物车'"
          @click.stop="handleAdd"
        >
          <Icon :name="added ? 'check' : 'plus'" :size="16" />
        </button>
      </div>
    </div>
  </article>
</template>
