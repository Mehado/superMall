<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MPager from '@/components/mall/MPager.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallCenterApi } from '@/api/mall'
import { money, shortTime } from '@/utils/format'
import { ok } from '@/lib/toast'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const data = await mallCenterApi.footprints({ page: page.value, pageSize })
    const records = data?.records || data?.list || (Array.isArray(data) ? data : [])
    // 后端返回 { productId, lastVisitTime, visitCount, product }
    // 这里把商品卡片展平，方便模板直接取值
    list.value = records.map((it) => ({
      ...(it.product || {}),
      productId: it.productId ?? it.product?.id,
      visitCount: it.visitCount,
      lastVisitTime: it.lastVisitTime
    }))
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function removeOne(item) {
  await mallCenterApi.removeFootprint(item.productId)
  ok('已删除该足迹')
  load()
}

async function clearAll() {
  await mallCenterApi.clearFootprints()
  ok('足迹已清空')
  load()
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">History</span>
          <h1 class="m-h1 m-mt-2">我的足迹</h1>
          <p class="m-muted m-mt-1" style="font-size: 13px">最近浏览过的商品</p>
        </div>
        <div class="m-row" style="gap: 8px">
          <RouterLink to="/shop/favorites" class="m-btn m-btn-soft m-btn-sm">我的收藏</RouterLink>
          <button class="m-btn m-btn-outline m-btn-sm" type="button" :disabled="!list.length" @click="clearAll">
            清空足迹
          </button>
        </div>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 340px" />

      <div v-else-if="list.length" class="m-col" style="gap: 12px">
        <div v-for="item in list" :key="item.id || item.productId" class="m-listrow" style="align-items: center">
          <img
            v-if="item.mainImage"
            class="m-thumb-sm"
            :src="item.mainImage"
            :alt="item.name"
            style="cursor: pointer"
            @click="router.push(`/shop/products/${item.productId}`)"
          />
          <span v-else class="m-thumb-sm" style="display: grid; place-items: center">
            <Icon name="image" :size="16" />
          </span>
          <div class="m-grow">
            <p class="m-h4">{{ item.name || '商品已下架' }}</p>
            <p class="m-dim">
              {{ item.subtitle || '' }}
              <span v-if="item.status === 0" class="m-badge m-badge-soft" style="margin-left: 6px">已下架</span>
            </p>
          </div>
          <span class="m-price">{{ money(item.priceLow ?? item.priceHigh) }}</span>
          <span class="m-dim m-hide-sm">{{ shortTime(item.lastVisitTime) }}</span>
          <button class="m-btn-link" type="button" @click="removeOne(item)">
            <Icon name="trash" :size="15" />
          </button>
        </div>
        <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
      </div>

      <MEmpty image="empty-footprint" v-else icon="eye" title="还没有足迹" desc="浏览商品详情后会自动记录">
        <RouterLink to="/shop" class="m-btn m-btn-primary m-mt-2">去逛逛</RouterLink>
      </MEmpty>
    </div>
  </ShopLayout>
</template>
