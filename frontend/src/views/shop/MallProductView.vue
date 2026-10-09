<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MSkuPicker from '@/components/mall/MSkuPicker.vue'
import MStars from '@/components/mall/MStars.vue'
import MStepper from '@/components/mall/MStepper.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MPager from '@/components/mall/MPager.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallCartApi, mallCatalogApi, mallCenterApi } from '@/api/mall'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'
import { money, shortTime } from '@/utils/format'
import { priceRange } from '@/utils/mall-format'
import { ok } from '@/lib/toast'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const auth = useAuthStore()

const product = ref(null)
const stats = ref(null)
const reviews = ref([])
const reviewTotal = ref(0)
const reviewFilter = ref({ rating: '', hasImage: '', page: 1 })
const skuId = ref(null)
const quantity = ref(1)
const activeImage = ref(0)
const loading = ref(true)
const favorite = ref(false)

const images = computed(() => {
  const list = product.value?.images || []
  if (list.length) return list
  return product.value?.image ? [product.value.image] : []
})
const specs = computed(() => product.value?.specs || [])
const skus = computed(() => product.value?.skus || [])
const currentSku = computed(() => skus.value.find((s) => String(s.id) === String(skuId.value)) || null)
const priceIsRange = computed(
  () => !currentSku.value && priceRange(product.value || {}).range
)
const priceText = computed(() => {
  const r = priceRange(product.value || {})
  if (r.low === null) return '价格待定'
  if (currentSku.value) return money(currentSku.value.price)
  return r.range ? `${money(r.low)} – ${money(r.high)}` : money(r.low)
})
const stock = computed(() => currentSku.value?.stock ?? product.value?.stock ?? 0)
const off = computed(() => product.value?.status === 0)
const canBuy = computed(
  () => !off.value && (specs.value.length === 0 || Boolean(currentSku.value)) && stock.value > 0
)

function ratingDist() {
  return stats.value?.ratingDistribution || stats.value?.starCount || stats.value?.stars || {}
}
function countOf(star) {
  const dist = ratingDist()
  return Number(dist[star] ?? dist[String(star)] ?? 0)
}
function barWidth(star) {
  const total = Number(stats.value?.reviewCount || 0)
  if (!total) return 0
  return Math.round((countOf(star) / total) * 100)
}

async function loadReviews() {
  const data = await mallCatalogApi.reviews(route.params.id, {
    rating: reviewFilter.value.rating || undefined,
    hasImage: reviewFilter.value.hasImage || undefined,
    page: reviewFilter.value.page,
    pageSize: 5
  })
  reviews.value = data?.records || data?.list || []
  reviewTotal.value = data?.total ?? reviews.value.length
}

async function load() {
  loading.value = true
  try {
    const id = route.params.id
    const [p, s] = await Promise.all([
      mallCatalogApi.product(id),
      mallCatalogApi.reviewStats(id).catch(() => null)
    ])
    product.value = p || null
    stats.value = s || null
    if (skus.value.length === 1) skuId.value = skus.value[0].id
    activeImage.value = 0
    await loadReviews().catch(() => {})

    // 进详情页记录一次足迹（未登录跳过）
    if (auth.isLoggedIn(ROLE.mallUser)) {
      mallCenterApi.addFootprint(id).catch(() => {})
      mallCenterApi
        .favoriteExists(id)
        .then((v) => (favorite.value = v === true || v?.exists === true))
        .catch(() => {})
    }
  } finally {
    loading.value = false
  }
}

async function addToCart(goCheckout = false) {
  if (!auth.isLoggedIn(ROLE.mallUser)) {
    router.push({ path: '/login', query: { role: ROLE.mallUser, redirect: route.fullPath } })
    return
  }
  if (!canBuy.value) {
    ok(specs.value.length ? '请选择完整的规格' : '当前商品不可购买')
    return
  }
  const finalSku = currentSku.value?.id || skus.value[0]?.id
  await mallCartApi.add({ skuId: finalSku, number: quantity.value })
  ok(goCheckout ? '已加入购物车，去结算' : '已加入购物车')
  cart.refreshMall()
  if (goCheckout) router.push('/shop/cart')
}

async function toggleFavorite() {
  if (!auth.isLoggedIn(ROLE.mallUser)) {
    router.push({ path: '/login', query: { role: ROLE.mallUser, redirect: route.fullPath } })
    return
  }
  if (favorite.value) {
    await mallCenterApi.removeFavorite(product.value.id)
    favorite.value = false
    ok('已取消收藏')
  } else {
    await mallCenterApi.addFavorite(product.value.id)
    favorite.value = true
    ok('已收藏')
  }
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div v-if="loading" class="m-skeleton" style="height: 560px" />

      <template v-else-if="product">
        <!-- 面包屑 -->
        <div class="m-row m-dim m-mb-4 m-wrapflex" style="gap: 6px">
          <RouterLink to="/shop" class="m-link">首页</RouterLink>
          <Icon name="right" :size="12" />
          <RouterLink
            :to="{ path: '/shop/search', query: { categoryId: product.categoryId } }"
            class="m-link"
          >
            {{ product.categoryName || '分类' }}
          </RouterLink>
          <Icon name="right" :size="12" />
          <span class="m-ellipsis" style="max-width: 320px">{{ product.name }}</span>
        </div>

        <div class="product-top">
          <!-- 图集 -->
          <div class="m-gallery">
            <div v-if="images.length > 1" class="m-gallery-thumbs">
              <button
                v-for="(img, i) in images"
                :key="img + i"
                type="button"
                :class="{ active: i === activeImage }"
                @click="activeImage = i"
              >
                <img :src="img" :alt="`${product.name}-${i}`" />
              </button>
            </div>
            <div class="m-gallery-main">
              <img v-if="images[activeImage]" :src="images[activeImage]" :alt="product.name" />
              <div v-else class="m-pcard-placeholder"><Icon name="image" :size="36" /></div>
              <MStatus v-if="off" class="m-pcard-badge" text="已下架" tone="ink" solid />
            </div>
          </div>

          <!-- 主信息 -->
          <div class="product-info">
            <div class="m-row m-wrapflex" style="gap: 8px">
              <MStatus v-if="product.brandName" :text="product.brandName" tone="gray" />
              <MStatus v-if="product.categoryName" :text="product.categoryName" tone="blue" />
            </div>

            <h1 class="m-h1">{{ product.name }}</h1>
            <p v-if="product.subtitle" class="m-muted" style="font-size: 15px">
              {{ product.subtitle }}
            </p>

            <MStars
              v-if="Number(stats?.reviewCount || 0) > 0"
              :value="Number(stats?.avgRating || stats?.rating || 0)"
              :count="stats?.reviewCount || 0"
              :size="15"
            />
            <p v-else class="m-dim">暂无评价</p>

            <div class="price-block">
              <div class="m-row" style="align-items: baseline; gap: 10px">
                <span class="m-price" style="font-size: 34px">{{ priceText }}</span>
                <span v-if="priceIsRange" class="m-dim">多规格可选</span>
                <span v-if="product.originalPrice" class="m-strike">
                  {{ money(product.originalPrice) }}
                </span>
                <span
                  v-if="Number(stats?.goodRate) > 0"
                  class="m-badge m-badge-ok"
                >
                  好评率
                  {{ Math.round(Number(stats.goodRate) * (Number(stats.goodRate) <= 1 ? 100 : 1)) }}%
                </span>
              </div>
              <div class="m-row m-wrapflex m-mt-3" style="gap: 18px">
                <span class="m-dim">库存 {{ stock }}</span>
                <span class="m-dim">销量 {{ product.sales ?? 0 }}</span>
                <span class="m-dim">支持 7 天无理由退货</span>
              </div>
            </div>

            <MSkuPicker v-model="skuId" :specs="specs" :skus="skus" />

            <div class="m-row" style="gap: 14px">
              <span class="m-eyebrow">数量</span>
              <MStepper v-model="quantity" :max="Math.max(1, stock || 1)" />
            </div>

            <div class="buy-row">
              <button class="m-btn m-btn-primary m-btn-lg" type="button" :disabled="!canBuy" @click="addToCart(false)">
                <Icon name="cart" :size="17" />
                加入购物车
              </button>
              <button class="m-btn m-btn-primary m-btn-lg" type="button" :disabled="!canBuy" @click="addToCart(true)">
                立即购买
              </button>
              <button class="m-btn m-btn-outline m-btn-lg m-btn-icon" type="button" title="收藏" @click="toggleFavorite">
                <Icon name="heart" :size="18" :filled="favorite" />
              </button>
            </div>
          </div>
        </div>

        <!-- 详情 + 评分统计 -->
        <div class="detail-grid">
          <div class="m-panel">
            <h2 class="m-h3 m-mb-4">商品详情</h2>
            <p class="m-muted" style="white-space: pre-wrap; line-height: 2; font-size: 14px">
              {{ product.detail || product.description || '暂无详情' }}
            </p>
          </div>

          <div class="m-panel">
            <h2 class="m-h3 m-mb-4">评分分布</h2>
            <template v-if="Number(stats?.reviewCount || 0) > 0">
              <div class="m-row" style="align-items: baseline; gap: 8px">
                <span class="m-price" style="font-size: 32px">
                  {{ Number(stats.avgRating || stats.rating || 0).toFixed(1) }}
                </span>
                <span class="m-dim">/ 5.0 · {{ stats.reviewCount }} 条评价</span>
              </div>
              <div class="m-col m-mt-4" style="gap: 8px">
                <div v-for="star in [5, 4, 3, 2, 1]" :key="star" class="m-row" style="gap: 10px">
                  <span class="m-dim m-num" style="width: 24px">{{ star }}星</span>
                  <span class="bar-track">
                    <span class="bar-fill" :style="{ width: `${barWidth(star)}%` }" />
                  </span>
                  <span class="m-dim m-num" style="width: 28px; text-align: right">{{ countOf(star) }}</span>
                </div>
              </div>
            </template>
            <p v-else class="m-dim">暂无评价</p>
          </div>
        </div>

        <!-- 评价列表 -->
        <div class="m-panel m-mt-4">
          <div class="m-between m-wrapflex m-mb-4">
            <h2 class="m-h3">商品评价</h2>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <button
                v-for="r in [{ k: '', l: '全部' }, { k: '5', l: '5星' }, { k: '4', l: '4星' }, { k: '3', l: '3星' }]"
                :key="r.k"
                type="button"
                class="m-chip"
                :class="{ 'm-chip-active': reviewFilter.rating === r.k }"
                @click="reviewFilter.rating = r.k; reviewFilter.page = 1; loadReviews()"
              >
                {{ r.l }}
              </button>
              <button
                type="button"
                class="m-chip"
                :class="{ 'm-chip-active': reviewFilter.hasImage === '1' }"
                @click="reviewFilter.hasImage = reviewFilter.hasImage === '1' ? '' : '1'; reviewFilter.page = 1; loadReviews()"
              >
                有图
              </button>
            </div>
          </div>

          <div v-if="reviews.length" class="m-col" style="gap: 20px">
            <div v-for="r in reviews" :key="r.id" class="review-item">
              <div class="m-between">
                <div class="m-row" style="gap: 10px">
                  <span class="m-badge m-badge-soft">
                    {{ r.anonymous ? '匿名用户' : r.nickname || r.username || '用户' }}
                  </span>
                  <MStars :value="r.rating || 0" :show-value="false" :size="13" />
                </div>
                <span class="m-dim">{{ shortTime(r.createTime) }}</span>
              </div>
              <p class="m-mt-2" style="white-space: pre-wrap; font-size: 14px">
                {{ r.content || '该用户没有填写评价内容' }}
              </p>
              <div v-if="r.images?.length" class="m-row m-wrapflex m-mt-3" style="gap: 8px">
                <img
                  v-for="(img, i) in r.images"
                  :key="img + i"
                  :src="img"
                  :alt="`评价图${i}`"
                  style="width: 78px; height: 78px; border-radius: 12px; object-fit: cover"
                />
              </div>
              <div v-if="r.followContent" class="m-soft m-mt-3" style="padding: 12px 14px">
                <p class="m-dim">追评 · {{ shortTime(r.followTime) }}</p>
                <p class="m-mt-1">{{ r.followContent }}</p>
              </div>
            </div>
            <MPager
              :page="reviewFilter.page"
              :page-size="5"
              :total="reviewTotal"
              @change="(p) => { reviewFilter.page = p; loadReviews() }"
            />
          </div>
          <MEmpty image="empty-review" v-else icon="star" title="还没有评价" desc="完成订单后可以在这里看到买家评价" />
        </div>
      </template>

      <MEmpty image="not-found" v-else icon="box" title="商品不存在或已删除">
        <RouterLink to="/shop" class="m-btn m-btn-primary m-mt-2">回到首页</RouterLink>
      </MEmpty>
    </div>
  </ShopLayout>
</template>

<style scoped>
.product-top {
  display: grid;
  gap: 48px;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 520px);
  align-items: start;
}
@media (max-width: 899px) {
  .product-top {
    grid-template-columns: minmax(0, 1fr);
    gap: 28px;
  }
}
.product-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 24px;
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
.buy-row .m-btn-primary {
  flex: 1;
  min-width: 160px;
}
.detail-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1fr) 340px;
  margin-top: 48px;
}
@media (max-width: 899px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
.bar-track {
  flex: 1;
  height: 6px;
  border-radius: 99px;
  background: var(--m-bone-deep);
  overflow: hidden;
}
.bar-fill {
  display: block;
  height: 100%;
  background: var(--m-accent);
}
.review-item {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--m-line);
}
.review-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}
</style>
