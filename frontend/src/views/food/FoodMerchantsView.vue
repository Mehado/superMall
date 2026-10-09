<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import MPager from '@/components/mall/MPager.vue'
import { foodPublicApi } from '@/api/delivery'
import { BUSINESS_TYPE, money } from '@/utils/format'

/**
 * 全部商家 —— 列表已**服务端分页**（`GET /api/public/merchants?page&pageSize&keyword`）。
 *
 * 历史变更：以前接口返回全量数组，搜店名是**前端过滤**；现在关键字也交给服务端（`shop_name like`），
 * 否则“只搜到当前页”会造成“明明有这家店却搜不到”的错觉。
 */
const merchants = ref([])
const loading = ref(true)
const keyword = ref('')
const page = ref(1)
const pageSize = 12
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const data = await foodPublicApi.merchants({
      page: page.value,
      pageSize,
      keyword: keyword.value || undefined
    })
    merchants.value = data?.records || data?.list || (Array.isArray(data) ? data : [])
    total.value = Number(data?.total ?? merchants.value.length)
  } catch {
    merchants.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

let timer = null
watch(keyword, () => {
  // 打字防抖：停止输入 300ms 再请求，且回到第 1 页
  clearTimeout(timer)
  timer = setTimeout(() => {
    page.value = 1
    load()
  }, 300)
})

watch(page, load)

onMounted(load)
</script>

<template>
  <FoodLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Merchants</span>
          <h1 class="m-h1 m-mt-2">全部商家</h1>
        </div>
        <span class="m-dim m-num">共 {{ total }} 家店</span>
      </div>

      <form class="m-shopsearch is-inline" @submit.prevent>
        <Icon name="search" :size="18" class="m-search-ico" />
        <input v-model="keyword" placeholder="搜店名" aria-label="搜索商家" />
      </form>

      <div v-if="loading" class="merchant-grid">
        <div v-for="i in 6" :key="i" class="m-skeleton" style="height: 190px" />
      </div>

      <div v-else-if="merchants.length" class="merchant-grid">
        <RouterLink
          v-for="m in merchants"
          :key="m.id"
          :to="`/food/shops/${m.id}`"
          class="m-panel merchant-card"
        >
          <div class="m-row m-row-top" style="gap: 14px">
            <img
              v-if="m.image || m.logo"
              class="m-thumb"
              :src="m.image || m.logo"
              :alt="m.name"
            />
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
                <span v-if="m.minAmount !== undefined"> · 起送 {{ money(m.minAmount) }}</span>
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
              {{ Number(m.status) === 0 ? '休息中（不能加购下单）' : '营业中' }}
            </span>
            <span class="m-btn-link">进店 →</span>
          </div>
        </RouterLink>
      </div>

      <MEmpty image="empty-search" v-else icon="store" title="没有匹配的商家" desc="换个店名再试试" />

      <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => (page = p)" />
    </div>
  </FoodLayout>
</template>

<style scoped>
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
