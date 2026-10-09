<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MUploader from '@/components/mall/MUploader.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallCenterApi, mallReviewApi } from '@/api/mall'
import { money } from '@/utils/format'
import { ok } from '@/lib/toast'

const route = useRoute()
const router = useRouter()
const orderNumber = computed(() => route.params.orderNumber)

const items = ref([])
const loading = ref(true)
const submitting = ref(false)
const drafts = reactive({})

const reviewable = computed(() => items.value.filter((it) => !it.review))

function ensureDraft(item) {
  const key = item.orderItemId || item.id
  if (!drafts[key]) {
    drafts[key] = { rating: 5, content: '', images: [], anonymous: false }
  }
  return drafts[key]
}

async function load() {
  loading.value = true
  try {
    const data = await mallReviewApi.orderReviewable(orderNumber.value)
    items.value = Array.isArray(data) ? data : data?.items || data?.records || []
    reviewable.value.forEach(ensureDraft)
  } finally {
    loading.value = false
  }
}

async function upload(file) {
  const res = await mallCenterApi.upload(file)
  return typeof res === 'string' ? res : res?.url || res?.data || ''
}

async function submit() {
  const payload = reviewable.value
    .map((it) => {
      const d = ensureDraft(it)
      return {
        orderItemId: it.orderItemId || it.id,
        rating: d.rating,
        content: d.content,
        images: d.images,
        anonymous: d.anonymous
      }
    })
    .filter((r) => r.rating > 0)

  if (!payload.length) {
    ok('没有可提交的评价')
    return
  }
  submitting.value = true
  try {
    await mallReviewApi.submit({ orderNumber: orderNumber.value, reviews: payload })
    ok('评价已提交')
    router.push(`/shop/orders/${orderNumber.value}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Review</span>
          <h1 class="m-h1 m-mt-2">发表评价</h1>
          <p class="m-muted m-mt-1" style="font-size: 13px">
            订单号 <span class="m-num">{{ orderNumber }}</span> · 已完成订单中每个明细只能评一次
          </p>
        </div>
        <RouterLink :to="`/shop/orders/${orderNumber}`" class="m-btn m-btn-soft m-btn-sm">返回订单</RouterLink>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 320px" />

      <template v-else-if="reviewable.length">
        <div v-for="item in reviewable" :key="item.orderItemId || item.id" class="m-panel m-mb-4">
          <div class="m-row" style="gap: 14px">
            <img v-if="item.image || item.productImage" class="m-thumb" :src="item.image || item.productImage" :alt="item.name" />
            <span v-else class="m-thumb" style="display: grid; place-items: center">
              <Icon name="image" :size="20" />
            </span>
            <div class="m-grow">
              <p class="m-h4">{{ item.name || item.productName }}</p>
              <p class="m-dim">{{ item.skuName || (item.specValues || []).join(' / ') }}</p>
              <p class="m-price m-mt-1">{{ money(item.price) }}</p>
            </div>
          </div>

          <div class="m-divider" />

          <div class="m-field">
            <span class="m-eyebrow">评分</span>
            <div class="m-row" style="gap: 6px">
              <button
                v-for="s in 5"
                :key="s"
                type="button"
                class="star-pick"
                :class="{ on: s <= ensureDraft(item).rating }"
                :aria-label="`${s} 分`"
                @click="ensureDraft(item).rating = s"
              >
                <Icon name="star" :size="24" filled :class="{ off: s > ensureDraft(item).rating }" />
              </button>
              <span class="m-dim">{{ ensureDraft(item).rating }} 分</span>
            </div>
          </div>

          <div class="m-field m-mt-4">
            <label class="m-eyebrow">评价内容</label>
            <textarea
              v-model="ensureDraft(item).content"
              class="m-textarea"
              placeholder="说说使用感受吧（可选）"
              maxlength="500"
            />
          </div>

          <div class="m-field m-mt-4">
            <span class="m-eyebrow">晒图（最多 9 张）</span>
            <MUploader
              v-model="ensureDraft(item).images"
              :uploader="upload"
              :max="9"
              hint="最多 9 张，支持 jpg / png"
            />
          </div>

          <label class="m-check m-mt-3">
            <input v-model="ensureDraft(item).anonymous" type="checkbox" />
            <span>匿名评价</span>
          </label>
        </div>

        <div class="m-panel" style="position: sticky; bottom: 16px">
          <div class="m-between">
            <span class="m-muted" style="font-size: 13px">共 {{ reviewable.length }} 个明细待评价</span>
            <button class="m-btn m-btn-primary m-btn-lg" type="button" :disabled="submitting" @click="submit">
              {{ submitting ? '提交中…' : '提交评价' }}
            </button>
          </div>
        </div>
      </template>

      <MEmpty image="empty-review"
        v-else
        icon="star"
        title="没有待评价的明细"
        desc="已完成订单中每个明细只能评价一次，评过的会显示在「我的评价」里"
      >
        <RouterLink to="/shop/reviews" class="m-btn m-btn-primary m-mt-2">查看我的评价</RouterLink>
      </MEmpty>
    </div>
  </ShopLayout>
</template>

<style scoped>
.star-pick {
  display: grid;
  place-items: center;
  padding: 2px;
  color: var(--m-accent);
}
.star-pick .off {
  color: var(--m-line-2);
}
</style>
