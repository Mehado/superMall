<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MProductCard from '@/components/mall/MProductCard.vue'
import MPager from '@/components/mall/MPager.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallCartApi, mallCenterApi } from '@/api/mall'
import { useCartStore } from '@/stores/cart'
import { ok } from '@/lib/toast'

const router = useRouter()
const cart = useCartStore()
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const data = await mallCenterApi.favorites({ page: page.value, pageSize })
    const records = data?.records || data?.list || (Array.isArray(data) ? data : [])
    // 后端返回 { productId, createTime, product }，把商品卡片展平
    list.value = records.map((it) => ({
      ...(it.product || {}),
      productId: it.productId ?? it.product?.id,
      favoriteTime: it.createTime
    }))
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function remove(product) {
  await mallCenterApi.removeFavorite(product.productId || product.id)
  ok('已取消收藏')
  load()
}

async function addToCart(product) {
  const skuId = product.skuId || product.skus?.[0]?.id
  if (!skuId) {
    router.push(`/shop/products/${product.productId || product.id}`)
    return
  }
  await mallCartApi.add({ skuId, number: 1 })
  ok('已加入购物车')
  cart.refreshMall()
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Wishlist</span>
          <h1 class="m-h1 m-mt-2">我的收藏</h1>
          <p class="m-muted m-mt-1" style="font-size: 13px">已下架的商品会标记出来，方便你整理</p>
        </div>
        <RouterLink to="/shop/footprints" class="m-btn m-btn-soft m-btn-sm">我的足迹</RouterLink>
      </div>

      <div v-if="loading" class="m-grid">
        <div v-for="i in 8" :key="i" class="m-skeleton" style="aspect-ratio: 4 / 5" />
      </div>

      <div v-else-if="list.length">
        <div class="m-grid">
          <div v-for="p in list" :key="p.id || p.productId" class="fav-item">
            <MProductCard
              :product="{ ...p, id: p.productId || p.id }"
              :off="p.status === 0"
              @open="router.push(`/shop/products/${p.productId || p.id}`)"
              @add="addToCart"
            />
            <button class="fav-remove" type="button" @click.stop="remove(p)">取消收藏</button>
          </div>
        </div>
        <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
      </div>

      <MEmpty image="empty-favorite" v-else icon="heart" title="还没有收藏商品" desc="在商品详情页点「收藏」就会出现在这里">
        <RouterLink to="/shop" class="m-btn m-btn-primary m-mt-2">去逛逛</RouterLink>
      </MEmpty>
    </div>
  </ShopLayout>
</template>

<style scoped>
.fav-item {
  position: relative;
}
.fav-remove {
  position: absolute;
  right: 12px;
  top: 12px;
  z-index: 2;
  padding: 6px 12px;
  border-radius: var(--m-pill);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--m-line);
  font-size: 12px;
  font-weight: 600;
  color: var(--m-ink-2);
  opacity: 0;
  transition: opacity 0.2s var(--m-ease), background-color 0.2s var(--m-ease);
}
.fav-item:hover .fav-remove {
  opacity: 1;
}
.fav-remove:hover {
  background: var(--m-ink);
  border-color: var(--m-ink);
  color: #fff;
}
@media (max-width: 767px) {
  .fav-remove {
    opacity: 1;
  }
}
</style>
