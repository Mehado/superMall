<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { foodUserApi } from '@/api/delivery'
import { FOOD_ORDER_STATUS, money, shortTime, statusOf } from '@/utils/format'
import { ok } from '@/lib/toast'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const paying = ref(false)

const meta = computed(() => statusOf(FOOD_ORDER_STATUS, order.value?.status))
const items = computed(() => order.value?.orderItems || order.value?.items || [])

const FLOW = [
  { key: 'PENDING_PAYMENT', label: '待支付' },
  { key: 'PAID', label: '待接单' },
  { key: 'MERCHANT_ACCEPTED', label: '备餐中' },
  { key: 'WAITING_DELIVERY', label: '待配送' },
  { key: 'DELIVERING', label: '配送中' },
  { key: 'COMPLETED', label: '已完成' }
]
const currentIndex = computed(() => {
  const i = FLOW.findIndex((f) => f.key === order.value?.status)
  return i === -1 ? 0 : i
})

async function load() {
  loading.value = true
  try {
    order.value = await foodUserApi.order(route.params.id)
  } finally {
    loading.value = false
  }
}

async function cancel() {
  await foodUserApi.cancelOrder(order.value.id)
  ok('订单已取消')
  load()
}

async function pay() {
  paying.value = true
  try {
    const tokenRes = await foodUserApi.payToken(order.value.number || order.value.orderNumber)
    const payToken = typeof tokenRes === 'string' ? tokenRes : tokenRes?.payToken || ''
    await foodUserApi.pay({ orderNumber: order.value.number || order.value.orderNumber, payToken })
    ok('支付成功')
    load()
  } finally {
    paying.value = false
  }
}

onMounted(load)
</script>

<template>
  <FoodLayout>
    <div class="m-wrap m-wrap-narrow m-section-sm">
      <div class="m-row m-dim m-mb-4" style="gap: 6px">
        <RouterLink to="/food/orders" class="m-link">我的订单</RouterLink>
        <Icon name="right" :size="12" />
        <span class="m-num">{{ order?.number || order?.orderNumber || route.params.id }}</span>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 360px" />

      <template v-else-if="order">
        <div class="m-panel m-mb-4">
          <div class="m-between m-wrapflex" style="gap: 16px">
            <div>
              <div class="m-row" style="gap: 12px">
                <h1 class="m-h2">{{ order.shopName || order.merchantName || '订单详情' }}</h1>
                <MStatus :text="meta.text" :tone="meta.tone" />
              </div>
              <p class="m-muted m-num m-mt-1" style="font-size: 13px">
                {{ order.number || order.orderNumber }}
              </p>
            </div>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <button
                v-if="order.status === 'PENDING_PAYMENT'"
                class="m-btn m-btn-accent"
                type="button"
                :disabled="paying"
                @click="pay"
              >
                {{ paying ? '支付中…' : '去支付' }}
              </button>
              <button
                v-if="['PENDING_PAYMENT', 'PAID'].includes(order.status)"
                class="m-btn m-btn-soft"
                type="button"
                @click="cancel"
              >
                取消订单
              </button>
            </div>
          </div>
        </div>

        <div class="m-panel m-mb-4" style="padding: 20px 24px">
          <h2 class="m-h3 m-mb-4">订单进度</h2>
          <div class="m-steps">
            <template v-for="(f, i) in FLOW" :key="f.key">
              <span
                class="m-step"
                :class="order.status === 'CANCELLED' ? '' : i < currentIndex ? 'done' : i === currentIndex ? 'now' : ''"
              >
                <i>{{ i < currentIndex ? '✓' : i + 1 }}</i> {{ f.label }}
              </span>
              <Icon v-if="i < FLOW.length - 1" name="right" :size="13" class="m-muted" />
            </template>
          </div>
          <p v-if="order.status === 'CANCELLED'" class="m-dim m-mt-3">订单已取消</p>
          <p v-else class="m-dim m-mt-3">刷新页面即可看到最新进度。</p>
        </div>

        <div class="info-grid m-mb-4">
          <div class="m-panel">
            <h2 class="m-h3 m-mb-4">收货信息</h2>
            <dl class="m-kv">
              <dt>收货人</dt>
              <dd>{{ order.consignee || '—' }}</dd>
              <dt>电话</dt>
              <dd class="m-num">{{ order.phone || '—' }}</dd>
              <dt>地址</dt>
              <dd>{{ order.address || `${order.province || ''}${order.city || ''}${order.district || ''}${order.detail || ''}` }}</dd>
              <dt>备注</dt>
              <dd>{{ order.remark || '无' }}</dd>
            </dl>
          </div>
          <div class="m-panel">
            <h2 class="m-h3 m-mb-4">订单信息</h2>
            <dl class="m-kv">
              <dt>下单时间</dt>
              <dd>{{ shortTime(order.orderTime || order.createTime) }}</dd>
              <dt>支付状态</dt>
              <dd>{{ order.payStatus || '—' }}</dd>
              <dt>配送费</dt>
              <dd class="m-num">{{ money(order.deliveryFee ?? 0) }}</dd>
              <dt>期望送达</dt>
              <dd>{{ shortTime(order.estimatedDeliveryTime) }}</dd>
            </dl>
          </div>
        </div>

        <div class="m-panel">
          <h2 class="m-h3 m-mb-4">商品明细</h2>
          <div class="m-col" style="gap: 16px">
            <div v-for="it in items" :key="it.id" class="m-row" style="gap: 14px">
              <img v-if="it.image" class="m-thumb-sm" :src="it.image" :alt="it.name" />
              <span v-else class="m-thumb-sm" style="display: grid; place-items: center">
                <Icon name="image" :size="16" />
              </span>
              <div class="m-grow">
                <p class="m-h4 m-clamp-2">{{ it.name || it.productName }}</p>
                <p class="m-dim">{{ it.skuName || '默认规格' }}</p>
              </div>
              <span class="m-muted m-num" style="font-size: 13px">×{{ it.number || it.quantity }}</span>
              <span class="m-price">{{ money(it.price) }}</span>
            </div>
            <p v-if="!items.length" class="m-dim">暂无商品明细</p>
          </div>
          <div class="m-divider" />
          <div class="m-between" style="align-items: baseline">
            <span class="m-h4">订单金额</span>
            <span class="m-price" style="font-size: 24px">{{ money(order.amount ?? order.orderAmount) }}</span>
          </div>
        </div>
      </template>

      <MEmpty v-else image="empty-order" icon="box" title="订单不存在">
        <RouterLink to="/food/orders" class="m-btn m-btn-accent m-mt-2">返回订单列表</RouterLink>
      </MEmpty>
    </div>
  </FoodLayout>
</template>

<style scoped>
.info-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}
</style>
