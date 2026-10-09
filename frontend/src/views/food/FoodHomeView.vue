<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FlowingGallery from '@/components/FlowingGallery.vue'
import { brandWordmark } from '@/lib/brand'
import { foodMarqueeImages, hasFoodMarqueeImages } from '@/lib/marquee-images'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MProductCard from '@/components/mall/MProductCard.vue'
import MSection from '@/components/mall/MSection.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { foodPublicApi } from '@/api/delivery'
import { BUSINESS_TYPE, money } from '@/utils/format'

/**
 * 外卖首页
 *
 * 「今天吃什么」刻意**不做成商城那种按分类的静态网格**，而是一条**随机商品流**：
 *   后端 GET /api/public/products 本身就是 `ORDER BY RAND()` 随机抽取，
 *   每次调用结果都不同 —— 所以「换一批」只要重新请求即可，前端不用自己洗牌。
 *   见《外卖接口文档》5.1「首页商品流（随机抽取）」。
 *
 * 版式与商城首页同构（左栏 + 大标题 + 搜索框 + 内容流），只是内容组织方式不同。
 */
const route = useRoute()
const router = useRouter()

const keyword = ref('')
const products = ref([])
const merchants = ref([])
const loading = ref(true)
/** 换一批时的独立状态，避免整页骨架屏闪烁 */
const shuffling = ref(false)

/** 一次多取一些，才撑得起「往下滚」的内容流；接口上限 60 */
const FEED_SIZE = 12

const HOT_WORDS = ['牛肉面', '奶茶', '盖饭', '水果']

async function load({ shuffle = false } = {}) {
  if (shuffle) shuffling.value = true
  else loading.value = true
  try {
    const [p, m] = await Promise.all([
      foodPublicApi.products({
        keyword: keyword.value || undefined,
        limit: FEED_SIZE
      }),
      // 首页只需要前几家（商家列表已分页）
      foodPublicApi.merchants({ page: 1, pageSize: 6 }).catch(() => [])
    ])
    products.value = Array.isArray(p) ? p : p?.records || p?.list || []
    merchants.value = Array.isArray(m) ? m : m?.records || m?.list || []
  } catch {
    products.value = []
    merchants.value = []
  } finally {
    loading.value = false
    shuffling.value = false
  }
}

/** 换一批：后端 ORDER BY RAND()，重新请求就是新的一批 */
function reshuffle() {
  load({ shuffle: true })
}

function submitSearch(text) {
  keyword.value = String(text ?? keyword.value ?? '').trim()
  // 只同步 URL 便于分享，不触发二次加载（本页没有 route watch）
  router.replace({ path: '/food', query: keyword.value ? { keyword: keyword.value } : {} })
  load()
}

function addToCart(product) {
  router.push(`/food/products/${product.id}`)
}

onMounted(() => {
  keyword.value = String(route.query.keyword || '')
  load()
})
</script>

<template>
  <FoodLayout>
    <!-- 报头：大名字 → 搜索框 → 热门词（与商城首页同构） -->
    <header class="m-wrap m-wrap-mid m-shophead">
      <!-- 字标舞台：图片在字标**后面**左右横向滚动（与商城首页同构） -->
      <div class="shopname-stage">
        <FlowingGallery
          v-if="hasFoodMarqueeImages"
          class="shopname-flow"
          :images="foodMarqueeImages"
          :lanes="2"
          :per-group="5"
          :speed="34"
        />
        <img v-if="brandWordmark" class="m-shopname-img" :src="brandWordmark" alt="SuperMall" />
        <h1 v-else class="m-shopname">SuperMall</h1>
      </div>

      <form class="m-shopsearch" @submit.prevent="submitSearch()">
        <Icon name="search" :size="18" class="m-search-ico" />
        <input v-model="keyword" placeholder="搜商品、店家，例如「牛肉面」" aria-label="搜索商品或商家" />
        <button class="m-btn m-btn-accent" type="submit">搜索</button>
      </form>

      <div class="m-hotkeys">
        <span class="m-dim">热门</span>
        <button
          v-for="k in HOT_WORDS"
          :key="k"
          class="m-btn-link"
          type="button"
          @click="submitSearch(k)"
        >
          {{ k }}
        </button>
      </div>
    </header>

    <!-- 随机商品流：保留原来的「换一批」随机展示模式 -->
    <section class="m-wrap m-wrap-mid m-section">
      <MSection eyebrow="Random feed" title="今天吃什么" desc="每次刷新都不一样，随机挑给你的好味道">
        <template #right>
          <button class="m-btn m-btn-soft m-btn-sm" type="button" :disabled="shuffling" @click="reshuffle">
            <Icon name="refresh" :size="14" />
            {{ shuffling ? '换着…' : '换一批' }}
          </button>
        </template>
      </MSection>

      <div v-if="loading" class="m-grid">
        <div v-for="i in 8" :key="i" class="m-skeleton" style="aspect-ratio: 4 / 5" />
      </div>
      <div v-else-if="products.length" class="m-grid">
        <MProductCard
          v-for="p in products"
          :key="p.id"
          :product="p"
          @open="router.push(`/food/products/${p.id}`)"
          @add="addToCart"
        />
      </div>
      <MEmpty image="empty-data"
        v-else
        icon="box"
        :title="keyword ? '没搜到这个' : '暂时没有可点的商品'"
        desc="换个关键词，或者先去逛逛商家"
      />
    </section>

    <!-- 附近商家 -->
    <section class="m-wrap m-wrap-mid m-section">
      <MSection eyebrow="Nearby" title="附近商家" desc="挑一家喜欢的店进去点单">
        <template #right>
          <RouterLink to="/food/merchants" class="m-btn-link">全部商家</RouterLink>
        </template>
      </MSection>

      <div v-if="merchants.length" class="merchant-grid">
        <RouterLink
          v-for="m in merchants.slice(0, 6)"
          :key="m.id"
          :to="`/food/shops/${m.id}`"
          class="m-panel merchant-card"
        >
          <div class="m-row" style="gap: 14px">
            <img v-if="m.image || m.logo" class="m-thumb" :src="m.image || m.logo" :alt="m.name" />
            <span v-else class="m-thumb" style="display: grid; place-items: center">
              <Icon name="store" :size="24" />
            </span>
            <div class="m-grow">
              <div class="m-between">
                <p class="m-h3">{{ m.name || m.shopName }}</p>
                <span v-if="m.rating" class="m-badge m-badge-warn">
                  {{ Number(m.rating).toFixed(1) }}
                </span>
              </div>
              <p class="m-dim m-mt-1">
                {{ BUSINESS_TYPE[m.businessType] || '商家' }}
                <span v-if="m.deliveryFee !== undefined"> · 配送费 {{ money(m.deliveryFee) }}</span>
              </p>
            </div>
          </div>
          <p class="m-muted m-mt-3 m-clamp-2" style="min-height: 40px; font-size: 13px">
            {{ m.description || m.address || '欢迎光临本店' }}
          </p>
          <div class="m-divider" />
          <div class="m-between">
            <span class="m-dim">
              <span class="m-dot" :class="{ 'is-off': Number(m.status) === 0 }" style="display: inline-block; margin-right: 6px" />
              {{ Number(m.status) === 0 ? '休息中' : '营业中' }}
            </span>
            <span class="m-btn-link">进店点单 →</span>
          </div>
        </RouterLink>
      </div>

      <MEmpty image="empty-data" v-else-if="!loading" icon="store" title="暂无商家" />
    </section>
  </FoodLayout>
</template>

<style scoped>
/* ── 字标舞台（横向流动图片墙 + SuperMall 字标） ─────────────
   层级（从下到上）：图片墙 1 → 白色柔光 2 → 字标 3 → 搜索框 4
   → 图片在字标**后面**左右滚动，字永远不被盖住；装饰层也绝不挡可交互控件。
   与商城首页（MallHomeView）同构，图池用的是 photo/marquee-food/。 */
/* 字标 + 搜索框整体往下挪一点（与商城首页一致） */
.m-shophead {
  padding-top: calc(var(--m-s8) + clamp(14px, 2.4vw, 36px));
}

.shopname-stage {
  position: relative;
  display: grid;
  place-items: center;
  padding: 8px 0 4px;
  isolation: isolate;
}
.shopname-stage::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background: radial-gradient(56% 118% at 50% 50%, rgba(255, 255, 255, 0.45), rgba(255, 255, 255, 0) 74%);
}
.shopname-stage .m-shopname-img {
  position: relative;
  z-index: 3;
  height: clamp(62px, 10vw, 116px);
}
.shopname-flow {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 1;
  width: min(100%, 980px);
  height: clamp(196px, 22vw, 286px);
}
.m-shopsearch,
.m-hotkeys {
  position: relative;
  z-index: 4;
}
@media (max-width: 720px) {
  .shopname-flow {
    display: none;
  }
}

.merchant-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}
.merchant-card {
  display: block;
  transition: border-color 0.25s var(--m-ease), transform 0.25s var(--m-ease);
}
.merchant-card:hover {
  border-color: var(--m-ink);
  transform: translateY(-3px);
}
</style>
