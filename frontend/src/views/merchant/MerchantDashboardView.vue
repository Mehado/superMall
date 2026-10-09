<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { merchantApi } from '@/api/delivery'
import { FOOD_ORDER_STATUS, money, shortTime, statusOf } from '@/utils/format'
import { ok } from '@/lib/toast'

const router = useRouter()

const stats = ref(null)
const shop = ref(null)
const business = ref(null)
const board = ref([])
const loading = ref(true)
const refreshing = ref(false)
let timer = null

const isOpen = computed(() => Number(business.value?.status ?? business.value?.businessStatus ?? 1) !== 0)

const cards = computed(() => {
  const s = stats.value || {}
  return [
    { label: '今日订单', value: s.todayOrders ?? 0 },
    { label: '今日营业额', value: money(s.todayAmount ?? 0) },
    { label: '待接单', value: s.paid ?? 0 },
    { label: '已完成', value: s.completed ?? 0 }
  ]
})

async function load(silent = false) {
  if (!silent) loading.value = true
  refreshing.value = true
  try {
    const [s, sh, b, o] = await Promise.all([
      merchantApi.orderStatistics().catch(() => null),
      merchantApi.shop().catch(() => null),
      merchantApi.business().catch(() => null),
      merchantApi.orders({ page: 1, pageSize: 8, status: 'PAID' }).catch(() => null)
    ])
    stats.value = s
    shop.value = sh
    business.value = b
    board.value = o?.records || o?.list || []
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function accept(order) {
  await merchantApi.accept(order.id)
  ok('已接单')
  load(true)
}

async function ready(order) {
  await merchantApi.ready(order.id)
  ok('备餐完成，已生成配送单进入骑手抢单池')
  load(true)
}

async function toggleBusiness() {
  const next = isOpen.value ? 0 : 1
  await merchantApi.updateBusiness({ status: next })
  ok(next === 1 ? '已开始营业' : '已打烊，用户将无法加购与下单')
  load(true)
}

async function simulatePay(order) {
  await merchantApi.simulatePay(order.number || order.orderNumber)
  ok('已模拟支付（调试）')
  load(true)
}

onMounted(async () => {
  await load()
  timer = setInterval(() => load(true), 30000) // 商家看板 30s 轮询
})

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <WorkbenchLayout
    section="merchant"
    :title="shop?.name || shop?.shopName || '商家工作台'"
  >
    <template #actions>
      <span class="m-row" style="gap: 8px">
        <span class="m-dot" :class="{ 'is-off': !isOpen }" />
        <span class="m-dim">{{ isOpen ? '营业中' : '休息中' }}</span>
      </span>
      <button
        class="m-btn"
        :class="isOpen ? 'm-btn-outline' : 'm-btn-accent'"
        type="button"
        @click="toggleBusiness"
      >
        {{ isOpen ? '打烊' : '开始营业' }}
      </button>
      <button class="m-btn m-btn-primary" type="button" :disabled="refreshing" @click="load()">刷新</button>
    </template>

    <div class="m-stats">
      <div v-for="c in cards" :key="c.label" class="m-stat">
        <p class="m-eyebrow">{{ c.label }}</p>
        <p class="m-stat-val m-mt-2">{{ c.value }}</p>
      </div>
    </div>

    <div class="m-panel m-mt-4">
      <div class="m-between m-wrapflex m-mb-4">
        <div>
          <h2 class="m-h3">待接单看板</h2>
          <p class="m-dim m-mt-1">接单 → 备餐完成（自动生成配送单给骑手）</p>
        </div>
        <RouterLink to="/merchant/orders" class="m-btn m-btn-soft m-btn-sm">全部订单</RouterLink>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 220px" />

      <div v-else-if="board.length" class="board">
        <div v-for="order in board" :key="order.id" class="m-panel">
          <div class="m-between">
            <div>
              <p class="m-h4 m-num">#{{ order.number || order.orderNumber }}</p>
              <p class="m-dim">{{ shortTime(order.orderTime) }}</p>
            </div>
            <MStatus
              :text="statusOf(FOOD_ORDER_STATUS, order.status).text"
              :tone="statusOf(FOOD_ORDER_STATUS, order.status).tone"
            />
          </div>

          <div class="m-divider" />

          <div class="m-col" style="gap: 8px">
            <div v-for="it in order.orderItems || order.items || []" :key="it.id" class="m-between">
              <span class="m-dim">{{ it.name || it.productName }} ×{{ it.number || it.quantity }}</span>
              <span class="m-dim m-num">{{ money(it.price) }}</span>
            </div>
          </div>

          <div class="m-divider" />
          <div class="m-between m-wrapflex" style="gap: 10px">
            <span class="m-h4">{{ money(order.amount ?? order.orderAmount) }}</span>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <button class="m-btn m-btn-accent m-btn-sm" type="button" @click="accept(order)">接单</button>
              <button class="m-btn m-btn-primary m-btn-sm" type="button" @click="ready(order)">备餐完成</button>
              <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="simulatePay(order)">模拟支付</button>
            </div>
          </div>
        </div>
      </div>

      <MEmpty image="empty-data" v-else icon="bell" title="暂无待接单订单" desc="用户支付后订单会出现在这里">
        <button class="m-btn m-btn-soft m-mt-2" type="button" @click="load()">手动刷新</button>
      </MEmpty>
    </div>

    <div class="m-panel m-mt-4">
      <h2 class="m-h3 m-mb-4">快捷入口</h2>
      <div class="shortcuts">
        <button class="shortcut" type="button" @click="router.push('/merchant/products/new')">
          <span class="shortcut-ico"><Icon name="plus" :size="18" /></span>
          <span class="m-h4">新增商品</span>
        </button>
        <button class="shortcut" type="button" @click="router.push('/merchant/categories')">
          <span class="shortcut-ico"><Icon name="grid" :size="18" /></span>
          <span class="m-h4">管理分类</span>
        </button>
        <button class="shortcut" type="button" @click="router.push('/merchant/shop')">
          <span class="shortcut-ico"><Icon name="store" :size="18" /></span>
          <span class="m-h4">店铺设置</span>
        </button>
      </div>
    </div>
  </WorkbenchLayout>
</template>

<style scoped>
.board {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}
.board > .m-panel {
  background: var(--m-paper);
}
.shortcuts {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}
.shortcut {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-sm);
  text-align: left;
  transition: border-color 0.2s var(--m-ease), transform 0.2s var(--m-ease);
}
.shortcut:hover {
  border-color: var(--m-ink);
  transform: translateY(-2px);
}
.shortcut-ico {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: var(--m-r-xs);
  background: var(--m-bone-deep);
  color: var(--m-ink-2);
}
</style>
