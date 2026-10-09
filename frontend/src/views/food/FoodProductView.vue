<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MStepper from '@/components/mall/MStepper.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { foodPublicApi, foodUserApi } from '@/api/delivery'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'
import { BUSINESS_TYPE, money } from '@/utils/format'
import { ok } from '@/lib/toast'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const auth = useAuthStore()

const product = ref(null)
const loading = ref(true)
const skuId = ref(null)
const quantity = ref(1)

const skus = computed(() => product.value?.skuList || product.value?.skus || [])
const currentSku = computed(() => skus.value.find((s) => String(s.id) === String(skuId.value)) || null)
const hasSpec = computed(() => skus.value.length > 1)
const price = computed(() => currentSku.value?.price ?? product.value?.price ?? 0)
const stock = computed(() => currentSku.value?.stock ?? product.value?.stock ?? 0)
const image = computed(() => product.value?.image || '')

async function add() {
  if (!auth.isLoggedIn(ROLE.deliveryUser)) {
    router.push({ path: '/login', query: { role: ROLE.deliveryUser, redirect: route.fullPath } })
    return
  }
  if (hasSpec.value && !skuId.value) {
    ok('请选择规格（skuList[].id）')
    return
  }
  await foodUserApi.addCart({
    merchantId: Number(product.value.merchantId),
    productId: Number(product.value.id),
    skuId: hasSpec.value ? Number(skuId.value) : 0,
    number: quantity.value
  })
  ok('已加入购物车')
  cart.refreshFood()
}

onMounted(async () => {
  try {
    const p = await foodPublicApi.product(route.params.id)
    product.value = p
    const list = p?.skuList || p?.skus || []
    if (list.length === 1) skuId.value = list[0].id
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <FoodLayout>
    <div class="m-wrap m-wrap-narrow m-section-sm">
      <div class="m-row m-dim m-mb-4 m-wrapflex" style="gap: 6px">
        <RouterLink to="/food" class="m-link">首页</RouterLink>
        <Icon name="right" :size="12" />
        <RouterLink v-if="product?.merchantId" :to="`/food/shops/${product.merchantId}`" class="m-link">
          {{ product.merchantName || '店铺' }}
        </RouterLink>
        <Icon name="right" :size="12" />
        <span class="m-ellipsis" style="max-width: 260px">{{ product?.name || '商品详情' }}</span>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 460px" />

      <template v-else-if="product">
        <div class="product-top">
          <div class="m-gallery-main">
            <img v-if="image" :src="image" :alt="product.name" />
            <div v-else class="m-pcard-placeholder">
              <div style="text-align: center">
                <Icon name="image" :size="32" />
                <p class="m-dim m-mt-2">暂无商品图片</p>
              </div>
            </div>
          </div>

          <div class="product-info">
            <div class="m-row m-wrapflex" style="gap: 8px">
              <span v-if="product.businessType" class="m-badge m-badge-ok">
                {{ BUSINESS_TYPE[product.businessType] }}
              </span>
              <RouterLink
                v-if="product.merchantId"
                :to="`/food/shops/${product.merchantId}`"
                class="m-badge m-badge-soft"
              >
                {{ product.merchantName || '进入店铺' }}
              </RouterLink>
            </div>

            <h1 class="m-h1">{{ product.name }}</h1>
            <p v-if="product.description" class="m-muted" style="font-size: 15px">
              {{ product.description }}
            </p>

            <div class="price-block">
              <span class="m-price" style="font-size: 34px">{{ money(price) }}</span>
              <span class="m-dim" style="margin-left: 12px">库存 {{ stock }}</span>
            </div>

            <div v-if="hasSpec" class="m-col" style="gap: 10px">
              <span class="m-eyebrow">选择规格</span>
              <div class="m-row m-wrapflex" style="gap: 8px">
                <button
                  v-for="s in skus"
                  :key="s.id"
                  type="button"
                  class="m-chip"
                  :class="{ 'm-chip-accent': String(skuId) === String(s.id) }"
                  :disabled="Number(s.stock) <= 0"
                  @click="skuId = s.id"
                >
                  {{ s.name || (s.specValues || []).join('/') || `SKU ${s.id}` }}
                </button>
              </div>
            </div>
            <p v-else class="m-dim">该商品无需选择规格。</p>

            <div class="m-row" style="gap: 14px">
              <span class="m-eyebrow">数量</span>
              <MStepper v-model="quantity" :max="Math.max(1, stock || 99)" />
            </div>

            <div class="buy-row">
              <button class="m-btn m-btn-accent m-btn-lg" type="button" @click="add">
                <Icon name="cart" :size="17" />
                加入购物车
              </button>
              <RouterLink to="/food/cart" class="m-btn m-btn-outline m-btn-lg">去购物车</RouterLink>
            </div>
          </div>
        </div>

        <div class="m-panel m-mt-5">
          <h2 class="m-h3 m-mb-3">商品说明</h2>
          <p class="m-muted" style="white-space: pre-wrap; line-height: 2; font-size: 14px">
            {{ product.detail || product.description || '暂无说明' }}
          </p>
        </div>
      </template>

      <MEmpty image="not-found" v-else icon="box" title="商品不存在">
        <RouterLink to="/food" class="m-btn m-btn-primary m-mt-2">回到首页</RouterLink>
      </MEmpty>
    </div>
  </FoodLayout>
</template>

<style scoped>
.product-top {
  display: grid;
  gap: 40px;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 460px);
  align-items: start;
}
@media (max-width: 899px) {
  .product-top {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
  }
}
.product-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.price-block {
  padding: 20px;
  background: var(--m-bone-deep);
  border-radius: var(--m-r);
}
.buy-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}
.buy-row .m-btn-accent {
  flex: 1;
  min-width: 160px;
}
</style>
