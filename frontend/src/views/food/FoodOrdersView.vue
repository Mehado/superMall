<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MTabs from '@/components/mall/MTabs.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { foodUserApi } from '@/api/delivery'
import { FOOD_ORDER_STATUS, money, shortTime, statusOf } from '@/utils/format'
import { ok } from '@/lib/toast'

const TABS = [
  { key: '', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待支付' },
  { key: 'PAID', label: '待接单' },
  { key: 'MERCHANT_ACCEPTED', label: '备餐中' },
  { key: 'DELIVERING', label: '配送中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

const status = ref('')
const page = ref(1)
const pageSize = 8
const total = ref(0)
const list = ref([])
const loading = ref(true)
const payingId = ref(null)

async function load() {
  loading.value = true
  try {
    // page / pageSize 必传，后端没有默认值
    const data = await foodUserApi.orders({
      page: page.value,
      pageSize,
      status: status.value || undefined
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function changeTab(key) {
  status.value = key
  page.value = 1
  load()
}

async function cancel(order) {
  await foodUserApi.cancelOrder(order.id)
  ok('订单已取消（已支付的会退款并回补库存）')
  load()
}

async function pay(order) {
  payingId.value = order.id
  try {
    const tokenRes = await foodUserApi.payToken(order.number || order.orderNumber)
    const payToken = typeof tokenRes === 'string' ? tokenRes : tokenRes?.payToken || ''
    await foodUserApi.pay({ orderNumber: order.number || order.orderNumber, payToken })
    ok('支付成功')
    load()
  } finally {
    payingId.value = null
  }
}

function itemsOf(order) {
  return order.orderItems || order.items || []
}

onMounted(load)
</script>

<template>
  <FoodLayout>
    <div class="m-wrap m-wrap-narrow m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Orders</span>
          <h1 class="m-h1 m-mt-2">我的订单</h1>
        </div>
        <RouterLink to="/food/me" class="m-btn m-btn-soft m-btn-sm">个人中心</RouterLink>
      </div>

      <MTabs :model-value="status" :tabs="TABS" @update:model-value="changeTab" />

      <div v-if="loading" class="m-skeleton m-mt-4" style="height: 280px" />

      <div v-else-if="list.length" class="m-col m-mt-5" style="gap: 16px">
        <div v-for="order in list" :key="order.id" class="m-panel" style="padding: 22px">
          <div class="m-between m-wrapflex">
            <div>
              <p class="m-h4">{{ order.shopName || order.merchantName || '商家' }}</p>
              <p class="m-dim m-num m-mt-1">
                {{ order.number || order.orderNumber }} · {{ shortTime(order.orderTime) }}
              </p>
            </div>
            <MStatus
              :text="statusOf(FOOD_ORDER_STATUS, order.status).text"
              :tone="statusOf(FOOD_ORDER_STATUS, order.status).tone"
            />
          </div>

          <div class="m-divider" />

          <div class="m-col" style="gap: 10px">
            <div v-for="it in itemsOf(order)" :key="it.id" class="m-row" style="gap: 10px">
              <span class="m-grow m-h4 m-clamp-2">{{ it.name || it.productName }}</span>
              <span class="m-muted m-num" style="font-size: 13px">×{{ it.number || it.quantity }}</span>
              <span class="m-price">{{ money(it.price) }}</span>
            </div>
            <p v-if="!itemsOf(order).length" class="m-dim">明细可在订单详情中查看</p>
          </div>

          <div class="m-divider" />

          <div class="m-between m-wrapflex" style="gap: 14px">
            <div class="m-row m-wrapflex" style="gap: 16px">
              <span class="m-dim">配送费 {{ money(order.deliveryFee ?? 0) }}</span>
              <span class="m-h4">
                合计
                <span class="m-price" style="font-size: 19px">
                  {{ money(order.amount ?? order.orderAmount) }}
                </span>
              </span>
            </div>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <RouterLink :to="`/food/orders/${order.id}`" class="m-btn m-btn-outline m-btn-sm">
                详情
              </RouterLink>
              <button
                v-if="order.status === 'PENDING_PAYMENT'"
                class="m-btn m-btn-accent m-btn-sm"
                type="button"
                :disabled="payingId === order.id"
                @click="pay(order)"
              >
                去支付
              </button>
              <button
                v-if="['PENDING_PAYMENT', 'PAID'].includes(order.status)"
                class="m-btn m-btn-soft m-btn-sm"
                type="button"
                @click="cancel(order)"
              >
                取消订单
              </button>
              <span v-if="['WAITING_DELIVERY', 'DELIVERING'].includes(order.status)" class="m-dim">
                骑手配送中，可在详情查看进度
              </span>
            </div>
          </div>
        </div>

        <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
      </div>

      <MEmpty
        v-else
        image="empty-order"
        icon="box"
        title="还没有外卖订单"
        desc="下单 → 支付 → 商家接单 → 骑手送达，全流程可在详情追踪"
      >
        <RouterLink to="/food" class="m-btn m-btn-accent m-mt-2">去点单</RouterLink>
      </MEmpty>
    </div>
  </FoodLayout>
</template>
