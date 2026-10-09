<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MTabs from '@/components/mall/MTabs.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MModal from '@/components/mall/MModal.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallOrderApi, mallPaymentApi } from '@/api/mall'
import { MALL_ORDER_STATUS, money, shortTime, statusOf } from '@/utils/format'
import { ok } from '@/lib/toast'

const route = useRoute()
const router = useRouter()

const TABS = [
  { key: '', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待付款' },
  { key: 'PENDING_DELIVERY', label: '待发货' },
  { key: 'PENDING_RECEIPT', label: '待收货' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'REFUNDING', label: '退款中' }
]

const status = ref('')
const page = ref(1)
const pageSize = 8
const total = ref(0)
const orders = ref([])
const loading = ref(true)

const refundOpen = ref(false)
const refundTarget = ref(null)
const refundForm = reactive({ type: 1, reason: '', description: '' })

async function load() {
  loading.value = true
  try {
    const data = await mallOrderApi.page({
      status: status.value || undefined,
      page: page.value,
      pageSize
    })
    orders.value = data?.records || data?.list || []
    total.value = data?.total ?? orders.value.length
  } finally {
    loading.value = false
  }
}

function changeTab(key) {
  status.value = key
  page.value = 1
  load()
}

async function pay(order) {
  const tokenRes = await mallPaymentApi.payToken(order.orderNumber)
  const payToken = tokenRes?.payToken || tokenRes
  await mallPaymentApi.pay({ orderNumber: order.orderNumber, payToken })
  ok('支付成功')
  load()
}

async function cancel(order) {
  await mallOrderApi.cancel(order.orderNumber)
  ok('订单已取消')
  load()
}

async function receipt(order) {
  await mallOrderApi.receipt(order.orderNumber)
  ok('已确认收货')
  load()
}

function openRefund(order) {
  refundTarget.value = order
  refundForm.type = 1
  refundForm.reason = ''
  refundForm.description = ''
  refundOpen.value = true
}

async function submitRefund() {
  await mallOrderApi.applyRefund(refundTarget.value.orderNumber, {
    type: refundForm.type,
    reason: refundForm.reason,
    description: refundForm.description
  })
  refundOpen.value = false
  ok('退款申请已提交')
  load()
}

function itemsOf(order) {
  return order.items || order.orderItems || order.details || []
}

// 退款单据状态 -> 列表提示（含被驳回，驳回后订单会回到原状态，必须靠退款单才能看出来）
function refundHint(order) {
  const r = order.refund
  if (!r) return ''
  if (r.status === 'PENDING') return '退款审核中，点击查看进度'
  if (r.status === 'APPROVED') return '退款已通过'
  if (r.status === 'REJECTED') return r.adminRemark ? `退款已驳回：${r.adminRemark}` : '退款已驳回，点击查看原因'
  return `退款${r.statusDesc || r.status || ''}`
}

function refundTone(order) {
  const s = order.refund?.status
  if (s === 'APPROVED') return 'is-approved'
  if (s === 'REJECTED') return 'is-rejected'
  return 'is-pending'
}

const emptyHint = computed(() => (status.value ? '这个状态下暂时没有订单' : '还没有下过单'))

onMounted(() => {
  if (route.query.status) status.value = String(route.query.status)
  load()
})
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Orders</span>
          <h1 class="m-h1 m-mt-2">我的订单</h1>
        </div>
        <RouterLink to="/shop/me" class="m-btn m-btn-soft m-btn-sm">个人中心</RouterLink>
      </div>

      <MTabs :model-value="status" :tabs="TABS" @update:model-value="changeTab" />

      <div v-if="loading" class="m-skeleton m-mt-4" style="height: 280px" />

      <div v-else-if="orders.length" class="m-col m-mt-5" style="gap: 16px">
        <div v-for="order in orders" :key="order.orderNumber || order.id" class="m-panel" style="padding: 22px">
          <div class="m-between m-wrapflex">
            <div class="m-row m-wrapflex" style="gap: 12px">
              <span class="m-dim m-num">订单号 {{ order.orderNumber }}</span>
              <span class="m-dim">{{ shortTime(order.createTime) }}</span>
            </div>
            <MStatus
              :text="statusOf(MALL_ORDER_STATUS, order.status).text"
              :tone="statusOf(MALL_ORDER_STATUS, order.status).tone"
            />
          </div>

          <div class="m-divider" />

          <div class="m-col" style="gap: 14px">
            <RouterLink
              v-for="it in itemsOf(order)"
              :key="it.id || it.productId"
              :to="`/shop/products/${it.productId || it.id}`"
              class="m-row"
              style="gap: 14px"
            >
              <img v-if="it.image || it.productImage" class="m-thumb-sm" :src="it.image || it.productImage" :alt="it.name" />
              <span v-else class="m-thumb-sm" style="display: grid; place-items: center">
                <Icon name="image" :size="16" />
              </span>
              <div class="m-grow">
                <p class="m-h4 m-clamp-2">{{ it.name || it.productName }}</p>
                <p class="m-dim">{{ it.skuName || (it.specValues || []).join(' / ') }}</p>
              </div>
              <span class="m-muted m-num" style="font-size: 13px">×{{ it.number || it.quantity }}</span>
              <span class="m-price">{{ money(it.price) }}</span>
            </RouterLink>
          </div>

          <div class="m-divider" />

          <div class="m-between m-wrapflex" style="gap: 14px">
            <div class="m-row m-wrapflex" style="gap: 16px">
              <span class="m-dim">商品 {{ money(order.totalAmount) }}</span>
              <span class="m-dim">运费 {{ money(order.freightAmount ?? 0) }}</span>
              <span class="m-dim">优惠 {{ money(order.discountAmount ?? 0) }}</span>
              <span class="m-h4">
                实付 <span class="m-price" style="font-size: 19px">{{ money(order.payAmount) }}</span>
              </span>
            </div>

            <div class="m-row m-wrapflex" style="gap: 8px">
              <RouterLink :to="`/shop/orders/${order.orderNumber}`" class="m-btn m-btn-outline m-btn-sm">
                订单详情
              </RouterLink>

              <template v-if="order.status === 'PENDING_PAYMENT'">
                <button class="m-btn m-btn-primary m-btn-sm" type="button" @click="pay(order)">去支付</button>
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="cancel(order)">取消订单</button>
              </template>

              <button
                v-if="order.status === 'PENDING_RECEIPT'"
                class="m-btn m-btn-primary m-btn-sm"
                type="button"
                @click="receipt(order)"
              >
                确认收货
              </button>

              <button
                v-if="['PENDING_DELIVERY', 'PENDING_RECEIPT', 'COMPLETED'].includes(order.status)"
                class="m-btn m-btn-soft m-btn-sm"
                type="button"
                @click="openRefund(order)"
              >
                申请退款
              </button>

              <RouterLink
                v-if="order.status === 'COMPLETED'"
                :to="`/shop/orders/${order.orderNumber}/review`"
                class="m-btn m-btn-primary m-btn-sm"
              >
                去评价
              </RouterLink>

              <RouterLink
                v-if="order.refund"
                :to="`/shop/orders/${order.orderNumber}`"
                class="refund-hint"
                :class="refundTone(order)"
              >
                {{ refundHint(order) }}
              </RouterLink>
            </div>
          </div>
        </div>
      </div>

      <MEmpty v-else image="empty-order" icon="box" :title="emptyHint" desc="下单后可以在这里追踪支付、发货、收货与退款进度">
        <RouterLink to="/shop" class="m-btn m-btn-primary m-mt-2">去逛逛</RouterLink>
      </MEmpty>

      <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
    </div>

    <MModal :open="refundOpen" title="申请退款（整单）" @close="refundOpen = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-soft" style="padding: 14px 16px">
          <p class="m-dim">订单号</p>
          <p class="m-h4 m-num">{{ refundTarget?.orderNumber }}</p>
          <p class="m-dim m-mt-2">退款金额 = 订单实付 {{ money(refundTarget?.payAmount) }}</p>
        </div>
        <div class="m-field">
          <span class="m-eyebrow">退款类型</span>
          <div class="m-row" style="gap: 8px">
            <button
              v-for="t in [{ k: 1, l: '仅退款' }, { k: 2, l: '退货退款' }]"
              :key="t.k"
              type="button"
              class="m-chip"
              :class="{ 'm-chip-active': refundForm.type === t.k }"
              @click="refundForm.type = t.k"
            >
              {{ t.l }}
            </button>
          </div>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">退款原因</label>
          <input v-model="refundForm.reason" class="m-input" placeholder="如：不想要了 / 商品与描述不符" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">补充说明</label>
          <textarea v-model="refundForm.description" class="m-textarea" placeholder="可选" />
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="refundOpen = false">取消</button>
        <button class="m-btn m-btn-danger" type="button" @click="submitRefund">提交申请</button>
      </template>
    </MModal>
  </ShopLayout>
</template>

<style scoped>
.refund-hint {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.6;
  text-decoration: none;
  border: 1px solid transparent;
}
.refund-hint.is-pending {
  background: #fff8e6;
  color: #9a6b00;
  border-color: #f0d493;
}
.refund-hint.is-approved {
  background: #eaf7ef;
  color: #22764a;
  border-color: #a9dcc0;
}
.refund-hint.is-rejected {
  background: #fdecec;
  color: #b13232;
  border-color: #f0b4b4;
}
</style>
