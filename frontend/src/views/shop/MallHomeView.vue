<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FlowingGallery from '@/components/FlowingGallery.vue'
import { brandWordmark } from '@/lib/brand'
import { marqueeImages, hasMarqueeImages } from '@/lib/marquee-images'
import ShopLayout from '@/layouts/ShopLayout.vue'
import CategorySection from '@/components/mall/home/CategorySection.vue'
import ProductSection from '@/components/mall/home/ProductSection.vue'
import { mallCatalogApi, mallCartApi } from '@/api/mall'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'
import { bannerTarget } from '@/utils/mall-format'
import { resolveImage } from '@/lib/mall-images'
import { ok } from '@/lib/toast'

/**
 * 商城首页
 *
 * 版式（对齐 shop.app 的左侧导航布局）：
 *   左边一列竖向菜单：小 Logo（无文字）→ 推荐 / 分类 / 喜欢 / 购物车 / 订单 → 最底部登录
 *   中间一列从上到下：大商城名 → 搜索框 → 金刚区 → 分类 → 热销 → 新品 → 服务承诺
 *
 * 接口对应《电商前端开发手册》3.1。
 * 两条必须扛住的现实（后端种子数据的既定事实）：
 *   1. banner 全是 status=0（下线），所以金刚区默认拿不到数据；
 *   2. 商品 / 分类都没有图片，主图可能为 null。
 * 页面在「零 banner 零图片」下也必须成立，这靠 MImageFrame 的占位与降级实现。
 */
const router = useRouter()
const cart = useCartStore()
const auth = useAuthStore()

const shortcuts = ref([])
const hot = ref([])
const fresh = ref([])
const categories = ref([])
const loading = ref(true)
const keyword = ref('')

/** 热门词用种子里真实存在的分类，点了能出结果 */
const HOT_WORDS = ['男装', '运动鞋', '咖啡', '充电宝']

async function load() {
  loading.value = true
  try {
    const [s, h, n, c] = await Promise.all([
      mallCatalogApi.banners(2).catch(() => []),
      mallCatalogApi.recommend('hot', 8).catch(() => []),
      mallCatalogApi.recommend('new', 6).catch(() => []),
      mallCatalogApi.categories().catch(() => [])
    ])
    shortcuts.value = Array.isArray(s) ? s : []
    hot.value = Array.isArray(h) ? h : []
    fresh.value = Array.isArray(n) ? n : []
    categories.value = Array.isArray(c) ? c : []
  } finally {
    loading.value = false
  }
}

function goSearch(text) {
  const q = String(text ?? '').trim()
  router.push({ path: '/shop/search', query: q ? { keyword: q } : {} })
}

function openProduct(product) {
  router.push(`/shop/products/${product.id}`)
}

function openBanner(banner) {
  const target = bannerTarget(banner)
  if (!target) return
  if (target.kind === 'external') {
    window.open(target.to, '_blank', 'noopener')
    return
  }
  router.push(target.to)
}

async function addToCart(product) {
  if (!auth.isLoggedIn(ROLE.mallUser)) {
    router.push({ path: '/login', query: { role: ROLE.mallUser, redirect: '/shop' } })
    return
  }
  // 手册第 6 节第 5 条：加购要 skuId 而不是商品 id；无规格商品也有默认 SKU
  const skuId = product.skuId || product.skus?.[0]?.id || product.defaultSkuId
  if (!skuId) {
    router.push(`/shop/products/${product.id}`)
    return
  }
  await mallCartApi.add({ skuId, number: 1 })
  ok('已加入购物车')
  cart.refreshMall()
}

const promises = [
  { icon: 'truck', t: '发货快', d: '下单后由商家发货并填写运单号，全程可查' },
  { icon: 'shield', t: '安心支付', d: '支持在线支付，退款申请与审核进度透明' },
  { icon: 'refresh', t: '退货无忧', d: '支持 7 天无理由退货，退款按整单处理' }
]

const katImage = (item) => resolveImage(item, 'shortcut', item.id)

onMounted(load)
</script>

<template>
  <ShopLayout>
    <!-- 报头：大商城名 → 搜索框 -->
    <header class="m-wrap m-wrap-mid m-shophead">
      <!-- 字标舞台：图片列在字标**上面**滚动，字从列间缝隙里「半侵入」地露出来 -->
      <div class="shopname-stage">
        <FlowingGallery
          v-if="hasMarqueeImages"
          class="shopname-flow"
          :images="marqueeImages"
          :lanes="2"
          :per-group="5"
          :speed="34"
        />
        <img v-if="brandWordmark" class="m-shopname-img" :src="brandWordmark" alt="SuperMall" />
        <h1 v-else class="m-shopname">SuperMall</h1>
      </div>

      <form class="m-shopsearch" @submit.prevent="goSearch(keyword)">
        <Icon name="search" :size="18" class="m-search-ico" />
        <input v-model="keyword" placeholder="搜索商品、品牌、分类" aria-label="搜索商品" />
        <button class="m-btn m-btn-primary" type="submit">搜索</button>
      </form>

      <div class="m-hotkeys">
        <span class="m-dim">热门</span>
        <button
          v-for="k in HOT_WORDS"
          :key="k"
          class="m-btn-link"
          type="button"
          @click="goSearch(k)"
        >
          {{ k }}
        </button>
      </div>
    </header>

    <!-- 金刚区：banner position=2。后端默认返回空（种子 status=0），此时整块不渲染 -->
    <section v-if="shortcuts.length" class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-kats">
        <button
          v-for="item in shortcuts.slice(0, 8)"
          :key="item.id"
          class="m-kat"
          type="button"
          @click="openBanner(item)"
        >
          <span class="m-kat-img">
            <img v-if="katImage(item)" :src="katImage(item)" :alt="item.title" />
            <Icon v-else name="tag" :size="20" />
          </span>
          <span class="m-kat-name">{{ item.title }}</span>
        </button>
      </div>
    </section>

    <CategorySection :categories="categories" :loading="loading" />

    <ProductSection
      eyebrow="Best sellers"
      title="热销推荐"
      desc="最近买得最多的好物"
      :products="hot"
      :loading="loading"
      :more-to="{ path: '/shop/search', query: { orderBy: 'sales_desc' } }"
      more-label="更多热销"
      @open="openProduct"
      @add="addToCart"
    />

    <ProductSection
      eyebrow="Just in"
      title="新品上架"
      desc="刚刚到店的新鲜货"
      :products="fresh"
      :loading="loading"
      layout="track"
      :more-to="{ path: '/shop/search', query: { orderBy: 'newest' } }"
      more-label="全部新品"
      empty-icon="sparkles"
      @open="openProduct"
      @add="addToCart"
    />

    <section class="m-wrap m-wrap-mid m-section">
      <div class="m-promises">
        <div v-for="f in promises" :key="f.t" class="m-promise">
          <Icon :name="f.icon" :size="20" style="color: var(--m-taupe)" />
          <div>
            <p class="m-promise-t">{{ f.t }}</p>
            <p class="m-promise-d">{{ f.d }}</p>
          </div>
        </div>
      </div>
    </section>
  </ShopLayout>
</template>

<style scoped>
/* ── 字标舞台（横向流动图片墙 + SuperMall 字标） ─────────────
   层级（从下到上）：图片墙 1 → 白色柔光 2 → 字标 3 → 搜索框 4
   → 图片在字标**后面**左右滚动，字永远不被盖住；装饰层也绝不挡可交互控件。 */
.shopname-stage {
  position: relative;
  display: grid;
  place-items: center;
  padding: 8px 0 4px;
  isolation: isolate;
}

/* 字标 + 搜索框整体往下挪一点（在 mall.css 的 --m-s8=64px 基础上再加；
   觉得还不够/太多，就调这个 clamp 的数值） */
.m-shophead {
  padding-top: calc(var(--m-s8) + clamp(14px, 2.4vw, 36px));
}

/* 字标后面一层很淡的白色柔光：图片深浅不一，靠它保证字始终清楚（觉得太糊就把 .6 调小/删掉） */
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
  height: clamp(62px, 10vw, 116px); /* 比 mall.css 的 clamp(80,13vw,150) 小一点 */
}

.shopname-flow {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 1;
  width: min(100%, 980px);
  height: clamp(196px, 22vw, 286px); /* 2 行 ×(卡片 112 + 浮动留白 10) + 行距 14 ≈ 258，留点余量 */
}

/* 搜索框 / 热门词抬到最上 */
.m-shopsearch,
.m-hotkeys {
  position: relative;
  z-index: 4;
}

/* 窄屏不播（省流量、也不挤字标） */
@media (max-width: 720px) {
  .shopname-flow {
    display: none;
  }
}
</style>
