<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MProductCard from '@/components/mall/MProductCard.vue'
import MFilterPanel from '@/components/mall/MFilterPanel.vue'
import MPager from '@/components/mall/MPager.vue'
import MModal from '@/components/mall/MModal.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallCatalogApi, mallCartApi } from '@/api/mall'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'
import { ok } from '@/lib/toast'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const auth = useAuthStore()

const SORTS = [
  { key: 'default', label: '综合' },
  { key: 'sales_desc', label: '销量' },
  { key: 'price_asc', label: '价格从低到高' },
  { key: 'price_desc', label: '价格从高到低' },
  { key: 'rating_desc', label: '评分' },
  { key: 'newest', label: '最新' }
]

const categories = ref([])
const brands = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)
const filterOpen = ref(false)

const query = reactive({
  keyword: '',
  categoryId: '',
  brandId: '',
  minPrice: '',
  maxPrice: '',
  orderBy: 'default',
  page: 1,
  pageSize: 12
})

const flatCategories = computed(() => {
  const out = []
  const walk = (list, depth = 0) => {
    ;(list || []).forEach((c) => {
      out.push({ ...c, depth })
      if (c.children?.length) walk(c.children, depth + 1)
    })
  }
  walk(categories.value)
  return out
})

const activeFilters = computed(
  () =>
    [query.categoryId, query.brandId, query.minPrice, query.maxPrice].filter((v) => v !== '').length
)

function syncFromRoute() {
  const q = route.query
  query.keyword = String(q.keyword || '')
  query.categoryId = String(q.categoryId || '')
  query.brandId = String(q.brandId || '')
  query.minPrice = String(q.minPrice || '')
  query.maxPrice = String(q.maxPrice || '')
  query.orderBy = String(q.orderBy || 'default')
  query.page = Number(q.page || 1)
}

function pushQuery(patch = {}) {
  const next = { ...query, ...patch }
  const params = {}
  Object.entries(next).forEach(([k, v]) => {
    if (
      v !== '' &&
      v !== null &&
      v !== undefined &&
      !(k === 'page' && v === 1) &&
      !(k === 'orderBy' && v === 'default')
    ) {
      params[k] = v
    }
  })
  router.push({ path: '/shop/search', query: params })
}

/**
 * 页面内的搜索框。
 * 原来搜索入口在顶部 Header 上，改成左侧竖栏导航后 Header 没了，
 * 所以这个页面必须自带一个 —— 否则用户换关键词只能靠改地址栏。
 */
const keywordInput = ref('')
watch(() => query.keyword, (v) => (keywordInput.value = v || ''), { immediate: true })

function submitSearch() {
  pushQuery({ keyword: keywordInput.value.trim(), page: 1 })
}

function resetAll() {
  pushQuery({
    keyword: '',
    categoryId: '',
    brandId: '',
    minPrice: '',
    maxPrice: '',
    orderBy: 'default',
    page: 1
  })
}

async function load() {
  loading.value = true
  try {
    const params = {
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      brandId: query.brandId || undefined,
      minPrice: query.minPrice || undefined,
      maxPrice: query.maxPrice || undefined,
      orderBy: query.orderBy,
      page: query.page,
      pageSize: query.pageSize
    }
    const data = await mallCatalogApi.products(params)
    const records = data?.records || data?.list || (Array.isArray(data) ? data : [])
    products.value = records
    total.value = data?.total ?? records.length
  } catch {
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadFacets() {
  try {
    const [c, b] = await Promise.all([
      mallCatalogApi.categories().catch(() => []),
      mallCatalogApi.brands().catch(() => [])
    ])
    categories.value = Array.isArray(c) ? c : []
    brands.value = Array.isArray(b) ? b.records || b : []
  } catch {
    categories.value = []
    brands.value = []
  }
}

async function addToCart(product) {
  if (!auth.isLoggedIn(ROLE.mallUser)) {
    router.push({ path: '/login', query: { role: ROLE.mallUser, redirect: route.fullPath } })
    return
  }
  const skuId = product.skuId || product.skus?.[0]?.id || product.defaultSkuId
  if (!skuId) {
    router.push(`/shop/products/${product.id}`)
    return
  }
  await mallCartApi.add({ skuId, number: 1 })
  ok('已加入购物车')
  cart.refreshMall()
}

function handlePatch(patch) {
  filterOpen.value = false
  pushQuery(patch)
}

watch(
  () => route.query,
  () => {
    syncFromRoute()
    load()
  }
)

onMounted(async () => {
  syncFromRoute()
  await Promise.all([loadFacets(), load()])
})
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <!-- 搜索框：原来在顶部 Header，改成左栏导航后移到这里 -->
      <form class="m-shopsearch is-inline" @submit.prevent="submitSearch">
        <Icon name="search" :size="18" class="m-search-ico" />
        <input v-model="keywordInput" placeholder="搜索商品、品牌、分类" aria-label="搜索商品" />
        <button class="m-btn m-btn-primary" type="submit">搜索</button>
      </form>

      <!-- 标题 -->
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Catalog</span>
          <h1 class="m-h1 m-mt-2">
            {{ query.keyword ? `“${query.keyword}”` : '全部商品' }}
          </h1>
          <p class="m-muted m-mt-1" style="font-size: 13px">
            共 <span class="m-num">{{ total }}</span> 件商品
          </p>
        </div>
        <button class="m-btn m-btn-outline m-hide-md" type="button" @click="filterOpen = true">
          <Icon name="list" :size="15" />
          筛选<template v-if="activeFilters"> · {{ activeFilters }}</template>
        </button>
      </div>

      <div class="search-layout">
        <!-- 侧栏筛选 -->
        <aside class="search-side m-hide-md">
          <MFilterPanel
            :flat="flatCategories"
            :brands="brands"
            :category-id="query.categoryId"
            :brand-id="query.brandId"
            :min-price="query.minPrice"
            :max-price="query.maxPrice"
            @patch="pushQuery"
            @reset="resetAll"
          />
        </aside>

        <!-- 列表 -->
        <section>
          <div class="m-between m-wrapflex m-mb-4" style="gap: 12px">
            <div class="m-rail" style="flex: 1">
              <button
                v-for="s in SORTS"
                :key="s.key"
                type="button"
                class="m-chip"
                :class="{ 'm-chip-active': query.orderBy === s.key }"
                @click="pushQuery({ orderBy: s.key, page: 1 })"
              >
                {{ s.label }}
              </button>
            </div>
          </div>

          <div v-if="loading" class="m-grid">
            <div v-for="i in 8" :key="i" class="m-skeleton" style="aspect-ratio: 4 / 5" />
          </div>

          <div v-else-if="products.length" class="m-grid">
            <MProductCard
              v-for="p in products"
              :key="p.id"
              :product="p"
              :off="p.status === 0"
              @open="router.push(`/shop/products/${p.id}`)"
              @add="addToCart"
            />
          </div>

          <MEmpty image="empty-search" v-else icon="search" title="没有找到商品" desc="换个关键词，或清空筛选条件试试">
            <button class="m-btn m-btn-primary m-mt-2" type="button" @click="resetAll">
              清空筛选
            </button>
          </MEmpty>

          <MPager
            :page="query.page"
            :page-size="query.pageSize"
            :total="total"
            @change="(p) => pushQuery({ page: p })"
          />
        </section>
      </div>
    </div>

    <MModal :open="filterOpen" title="筛选" @close="filterOpen = false">
      <MFilterPanel
        :flat="flatCategories"
        :brands="brands"
        :category-id="query.categoryId"
        :brand-id="query.brandId"
        :min-price="query.minPrice"
        :max-price="query.maxPrice"
        @patch="handlePatch"
        @reset="
          () => {
            filterOpen = false
            resetAll()
          }
        "
      />
    </MModal>
  </ShopLayout>
</template>

<style scoped>
.search-layout {
  display: grid;
  gap: 40px;
  grid-template-columns: 240px minmax(0, 1fr);
}
@media (max-width: 899px) {
  .search-layout {
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;
  }
}
.search-side {
  position: sticky;
  top: 24px;
  align-self: start;
  max-height: calc(100vh - 72px);
  overflow-y: auto;
  padding-right: 8px;
}
</style>
