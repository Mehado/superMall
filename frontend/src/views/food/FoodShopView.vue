<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MProductCard from '@/components/mall/MProductCard.vue'
import MPager from '@/components/mall/MPager.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import MModal from '@/components/mall/MModal.vue'
import MStepper from '@/components/mall/MStepper.vue'
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

const shop = ref(null)
const categories = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(true)
const query = reactive({ categoryId: '', name: '', page: 1, pageSize: 12 })

const skuOpen = ref(false)
const skuTarget = ref(null)
const skuList = ref([])
const pickedSku = ref(null)
const quantity = ref(1)

const merchantId = computed(() => route.params.id)

async function loadShop() {
  const [s, c] = await Promise.all([
    foodPublicApi.merchantDetail(merchantId.value).catch(() => null),
    foodPublicApi.merchantCategories(merchantId.value).catch(() => [])
  ])
  shop.value = s
  categories.value = Array.isArray(c) ? c : c?.records || []
}

async function loadProducts() {
  loading.value = true
  try {
    const data = await foodPublicApi.merchantProducts(merchantId.value, {
      page: query.page,
      pageSize: query.pageSize,
      categoryId: query.categoryId || undefined,
      name: query.name || undefined
    })
    products.value = data?.records || data?.list || (Array.isArray(data) ? data : [])
    total.value = data?.total ?? products.value.length
  } finally {
    loading.value = false
  }
}

async function openSku(product) {
  if (Number(shop.value?.status) === 0) {
    ok('商家休息中，暂不能下单')
    return
  }
  if (!auth.isLoggedIn(ROLE.deliveryUser)) {
    router.push({ path: '/login', query: { role: ROLE.deliveryUser, redirect: route.fullPath } })
    return
  }
  const detail = await foodPublicApi.product(product.id).catch(() => product)
  skuTarget.value = detail
  skuList.value = detail?.skuList || detail?.skus || []
  pickedSku.value = skuList.value.length === 1 ? skuList.value[0].id : null
  quantity.value = 1
  skuOpen.value = true
}

async function confirmAdd() {
  const hasSpec = skuList.value.length > 1
  if (hasSpec && !pickedSku.value) {
    ok('请选择规格')
    return
  }
  // 无规格商品 skuId 传 0；有规格必须传真实 skuId
  await foodUserApi.addCart({
    merchantId: Number(merchantId.value),
    productId: skuTarget.value.id,
    skuId: hasSpec ? Number(pickedSku.value) : 0,
    number: quantity.value
  })
  ok('已加入购物车')
  skuOpen.value = false
  cart.refreshFood()
}

const skuPrice = computed(
  () => skuList.value.find((s) => String(s.id) === String(pickedSku.value))?.price ?? skuTarget.value?.price
)

watch(
  () => route.params.id,
  async () => {
    await loadShop()
    query.page = 1
    loadProducts()
  }
)

onMounted(async () => {
  await loadShop()
  loadProducts()
})
</script>

<template>
  <FoodLayout>
    <!-- 店铺头 -->
    <div class="m-wrap m-wrap-mid" style="padding-top: 32px">
      <div class="shop-head">
        <span class="shop-mark"><Icon name="store" :size="26" /></span>
        <div class="m-grow">
          <div class="m-row m-wrapflex" style="gap: 10px">
            <h1 class="m-h1">{{ shop?.name || shop?.shopName || '店铺详情' }}</h1>
            <span
              class="m-badge"
              :class="Number(shop?.status) === 0 ? 'm-badge-soft' : 'm-badge-ok'"
            >
              {{ Number(shop?.status) === 0 ? '休息中' : '营业中' }}
            </span>
          </div>
          <p class="shop-meta">
            {{ BUSINESS_TYPE[shop?.businessType] || '商家' }}
            <span v-if="shop?.deliveryFee !== undefined"> · 配送费 {{ money(shop.deliveryFee) }}</span>
            <span v-if="shop?.minAmount !== undefined"> · 起送 {{ money(shop.minAmount) }}</span>
          </p>
          <p v-if="shop?.address" class="shop-meta">
            <Icon name="pin" :size="13" /> {{ shop.address }}
          </p>
        </div>
        <RouterLink to="/food/cart" class="m-btn m-btn-outline shop-cart">
          <Icon name="cart" :size="16" />
          去购物车（{{ cart.foodTotal }}）
        </RouterLink>
      </div>
    </div>

    <div class="m-wrap m-wrap-mid m-section-sm shop-layout">
      <!-- 店内分类 -->
      <aside class="m-hide-md">
        <div class="shop-side">
          <p class="m-eyebrow m-mb-3">店内分类</p>
          <div class="m-col" style="gap: 2px">
            <button
              type="button"
              class="shop-cat"
              :class="{ active: !query.categoryId }"
              @click="query.categoryId = ''; query.page = 1; loadProducts()"
            >
              全部商品
            </button>
            <button
              v-for="c in categories"
              :key="c.id"
              type="button"
              class="shop-cat"
              :class="{ active: String(query.categoryId) === String(c.id) }"
              @click="query.categoryId = c.id; query.page = 1; loadProducts()"
            >
              {{ c.name }}
            </button>
            <p v-if="!categories.length" class="m-dim">该店暂无分类</p>
          </div>
        </div>
      </aside>

      <section>
        <form class="m-shopsearch is-inline" @submit.prevent="query.page = 1; loadProducts()">
          <Icon name="search" :size="18" class="m-search-ico" />
          <input v-model="query.name" placeholder="搜店内商品" aria-label="搜索店内商品" />
        </form>

        <div class="m-between m-mb-3">
          <span class="m-dim m-num">{{ total }} 件商品</span>
        </div>

        <div v-if="loading" class="m-grid">
          <div v-for="i in 6" :key="i" class="m-skeleton" style="aspect-ratio: 4 / 5" />
        </div>
        <div v-else-if="products.length" class="m-grid">
          <MProductCard
            v-for="p in products"
            :key="p.id"
            :product="p"
            @open="router.push(`/food/products/${p.id}`)"
            @add="openSku"
          />
        </div>
        <MEmpty image="empty-data" v-else icon="box" title="店内暂无商品" desc="换个分类看看" />

        <MPager
          :page="query.page"
          :page-size="query.pageSize"
          :total="total"
          @change="(p) => { query.page = p; loadProducts() }"
        />
      </section>
    </div>

    <MModal :open="skuOpen" title="选择规格" @close="skuOpen = false">
      <div v-if="skuTarget" class="m-col" style="gap: 18px">
        <div class="m-row m-row-top" style="gap: 14px">
          <img v-if="skuTarget.image" class="m-thumb" :src="skuTarget.image" :alt="skuTarget.name" />
          <span v-else class="m-thumb" style="display: grid; place-items: center">
            <Icon name="image" :size="20" />
          </span>
          <div class="m-grow">
            <p class="m-h3">{{ skuTarget.name }}</p>
            <p class="m-dim m-mt-1">{{ skuTarget.description || '' }}</p>
            <p class="m-price m-mt-2" style="font-size: 20px">{{ money(skuPrice) }}</p>
          </div>
        </div>

        <div v-if="skuList.length > 1" class="m-col" style="gap: 10px">
          <span class="m-eyebrow">规格</span>
          <div class="m-row m-wrapflex" style="gap: 8px">
            <button
              v-for="s in skuList"
              :key="s.id"
              type="button"
              class="m-chip"
              :class="{ 'm-chip-accent': String(pickedSku) === String(s.id) }"
              :disabled="Number(s.stock) <= 0"
              @click="pickedSku = s.id"
            >
              {{ s.name || (s.specValues || []).join('/') || `SKU ${s.id}` }}
              <span class="m-dim" style="margin-left: 6px">库存 {{ s.stock }}</span>
            </button>
          </div>
        </div>
        <p v-else class="m-dim">该商品无需选择规格。</p>

        <div class="m-row" style="gap: 14px">
          <span class="m-eyebrow">数量</span>
          <MStepper v-model="quantity" :max="99" />
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="skuOpen = false">取消</button>
        <button class="m-btn m-btn-accent" type="button" @click="confirmAdd">加入购物车</button>
      </template>
    </MModal>
  </FoodLayout>
</template>

<style scoped>
.shop-head {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  padding: 28px 32px;
  background: var(--m-paper);
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-lg);
}
.shop-mark {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  border-radius: var(--m-r);
  background: var(--m-accent-soft);
  color: var(--m-accent-deep);
  flex-shrink: 0;
}
.shop-meta {
  color: var(--m-taupe);
  font-size: 13px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
/* 深色底取消后，去购物车按钮用默认 outline 样式 */
.shop-layout {
  display: grid;
  gap: 40px;
  grid-template-columns: 200px minmax(0, 1fr);
}
@media (max-width: 899px) {
  .shop-layout {
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;
  }
}
.shop-side {
  position: sticky;
  top: 24px;
  align-self: start;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}
.shop-cat {
  display: block;
  width: 100%;
  padding: 9px 12px;
  border-radius: var(--m-r-xs);
  font-size: 13px;
  text-align: left;
  color: var(--m-ink-2);
  transition: background-color 0.2s var(--m-ease), color 0.2s var(--m-ease);
}
.shop-cat:hover {
  background: var(--m-bone-deep);
  color: var(--m-ink);
}
.shop-cat.active {
  background: var(--m-ink);
  color: var(--m-paper);
  font-weight: 600;
}
</style>
