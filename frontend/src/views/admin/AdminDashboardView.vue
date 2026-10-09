<script setup>
import { computed, onMounted, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallAdminApi } from '@/api/mall'
import { money } from '@/utils/format'

const overview = ref(null)
const trend = ref([])
const categorySales = ref([])
const topProducts = ref([])
const days = ref(7)
const loading = ref(true)

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '销售额', value: money(o.totalSales ?? o.salesAmount ?? 0), hint: `今日 ${money(o.todaySales ?? 0)}` },
    { label: '订单数', value: o.totalOrders ?? o.orderCount ?? 0, hint: `待发货 ${o.pendingDelivery ?? 0}` },
    { label: '会员数', value: o.totalMembers ?? o.memberCount ?? 0, hint: `新增 ${o.newMembers ?? 0}` },
    { label: '退款中', value: o.refundingCount ?? 0, hint: `待审核 ${o.pendingRefund ?? 0}` }
  ]
})

const trendMax = computed(() =>
  Math.max(1, ...trend.value.map((t) => Number(t.sales ?? t.amount ?? t.value ?? 0)))
)
const trendTotal = computed(() =>
  trend.value.reduce((s, t) => s + Number(t.sales ?? t.amount ?? t.value ?? 0), 0)
)

async function load() {
  loading.value = true
  try {
    const [o, t, c, p] = await Promise.all([
      mallAdminApi.statsOverview().catch(() => null),
      mallAdminApi.statsSalesTrend({ days: days.value }).catch(() => []),
      mallAdminApi.statsCategorySales().catch(() => []),
      mallAdminApi.statsTopProducts({ limit: 8 }).catch(() => [])
    ])
    overview.value = o
    trend.value = Array.isArray(t) ? t : t?.records || t?.list || []
    categorySales.value = Array.isArray(c) ? c : c?.records || []
    topProducts.value = Array.isArray(p) ? p : p?.records || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout>
    <template #actions>
      <select v-model.number="days" class="m-select" style="width: 130px" @change="load">
        <option :value="7">近 7 天</option>
        <option :value="15">近 15 天</option>
        <option :value="30">近 30 天</option>
      </select>
      <button class="m-btn m-btn-primary" type="button" @click="load">刷新</button>
    </template>

    <div class="m-stats">
      <div v-for="c in cards" :key="c.label" class="m-stat">
        <p class="m-eyebrow">{{ c.label }}</p>
        <p class="m-stat-val m-mt-2">{{ c.value }}</p>
        <p class="m-dim m-mt-1">{{ c.hint }}</p>
      </div>
    </div>

    <div class="dash-grid m-mt-4">
      <div class="m-panel">
        <div class="m-between m-mb-4">
          <h2 class="m-h3">销售趋势</h2>
          <span v-if="trend.length" class="m-dim">合计 {{ money(trendTotal) }}</span>
        </div>

        <div v-if="loading" class="m-skeleton" style="height: 210px" />
        <div v-else-if="trend.length" class="trend">
          <div v-for="(t, i) in trend" :key="i" class="trend-col">
            <span class="trend-val m-num">{{ money(t.sales ?? t.amount ?? t.value ?? 0, '') }}</span>
            <span class="trend-bar" :style="{ height: `${Math.max(6, (Number(t.sales ?? t.amount ?? t.value ?? 0) / trendMax) * 100)}%` }" />
            <span class="trend-day">{{ t.date || t.day || t.label }}</span>
          </div>
        </div>
        <MEmpty image="empty-data" v-else icon="chart" title="暂无趋势数据" desc="需要先有已支付订单" />
      </div>

      <div class="m-panel">
        <h2 class="m-h3 m-mb-4">分类销售占比</h2>
        <div v-if="categorySales.length" class="m-col" style="gap: 14px">
          <div v-for="c in categorySales" :key="c.categoryId || c.name">
            <div class="m-between">
              <span class="m-h4">{{ c.categoryName || c.name }}</span>
              <span class="m-dim m-num">{{ money(c.sales ?? c.amount ?? 0) }}</span>
            </div>
            <span class="bar-track">
              <span
                class="bar-fill"
                :style="{ width: `${Math.min(100, Number(c.percent ?? c.ratio ?? 40) * (Number(c.percent) > 1 ? 1 : 100))}%` }"
              />
            </span>
          </div>
        </div>
        <MEmpty image="empty-data" v-else icon="chart" title="暂无分类销售数据" />
      </div>
    </div>

    <div class="m-panel m-mt-4">
      <h2 class="m-h3 m-mb-4">热销商品 TOP</h2>
      <div class="m-table-wrap">
        <table class="m-table">
          <thead>
            <tr>
              <th>商品</th>
              <th>销量</th>
              <th>销售额</th>
              <th>库存</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in topProducts" :key="p.id || p.productId">
              <td>
                <div class="m-cell">
                  <img v-if="p.image" class="m-thumb-sm" :src="p.image" :alt="p.name" />
                  <span v-else class="m-thumb-sm" />
                  <span class="m-h4">{{ p.name || p.productName }}</span>
                </div>
              </td>
              <td class="m-num">{{ p.sales ?? p.salesCount ?? 0 }}</td>
              <td class="m-num">{{ money(p.salesAmount ?? p.amount ?? 0) }}</td>
              <td>
                <MStatus
                  :text="`${p.stock ?? 0}`"
                  :tone="Number(p.stock ?? 0) > 0 ? 'emerald' : 'red'"
                />
              </td>
            </tr>
            <tr v-if="!topProducts.length">
              <td colspan="4" class="m-table-empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </WorkbenchLayout>
</template>

<style scoped>
.dash-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
}
@media (max-width: 1000px) {
  .dash-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
.trend {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 210px;
}
.trend-col {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}
.trend-val {
  font-size: 11px;
  color: var(--m-taupe);
}
.trend-bar {
  width: 100%;
  border-radius: 6px 6px 3px 3px;
  background: var(--m-accent);
  opacity: 0.85;
  transition: opacity 0.2s var(--m-ease);
}
.trend-col:hover .trend-bar {
  opacity: 1;
}
.trend-day {
  font-size: 11px;
  color: var(--m-faint);
  white-space: nowrap;
}
.bar-track {
  display: block;
  height: 8px;
  margin-top: 8px;
  border-radius: 99px;
  background: var(--m-bone-deep);
  overflow: hidden;
}
.bar-fill {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--m-ink);
}
</style>
