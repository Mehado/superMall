<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MModal from '@/components/mall/MModal.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallOrderApi, mallPaymentApi } from '@/api/mall'
import {
  MALL_ORDER_STATUS,
  MALL_REFUND_STATUS,
  MALL_REFUND_TYPE,
  money,
  shortTime,
  statusOf
} from '@/utils/format'
import { ok } from '@/lib/toast'

const route = useRoute()
const order = ref(null)
const refund = ref(null)
const loading = ref(true)
const refundOpen = ref(false)
const refundForm = reactive({ type: 1, reason: '', description: '' })

const orderNumber = computed(() => route.params.orderNumber)
const statusMeta = computed(() => statusOf(MALL_ORDER_STATUS, order.value?.status))
const items = computed(() => order.value?.items || order.value?.orderItems || [])
const refundMeta = computed(() => statusOf(MALL_REFUND_STATUS, refund.value?.status))
const canApplyRefund = computed(() =>
  ['PENDING_DELIVERY', 'PENDING_RECEIPT', 'COMPLETED'].includes(order.value?.status)
)

async function load() {
  loading.value = true
  try {
    order.value = await mallOrderApi.detail(orderNumber.value)
    // 订单详情接口已带最新退款单（含被驳回的），直接用它；
    // 老数据没有的话再回退到单独的退款接口
    refund.value = order.value?.refund || null
    if (!refund.value && ['REFUNDING', 'REFUNDED'].includes(order.value?.status)) {
      refund.value = await mallOrderApi.refund(orderNumber.value).catch(() => null)
    }
  } finally {
    loading.value = false
  }
}

function openRefund() {
  refundForm.type = refund.value?.type || 1
  refundForm.reason = ''
  refundForm.description = ''
  refundOpen.value = true
}

async function pay() {
  const tokenRes = await mallPaymentApi.payToken(orderNumber.value)
  await mallPaymentApi.pay({ orderNumber: orderNumber.value, payToken: tokenRes?.payToken || tokenRes })
  ok('支付成功')
  load()
}

async function cancel() {
  await mallOrderApi.cancel(orderNumber.value)
  ok('订单已取消')
  load()
}

async function receipt() {
  await mallOrderApi.receipt(orderNumber.value)
  ok('已确认收货')
  load()
}

async function submitRefund() {
  await mallOrderApi.applyRefund(orderNumber.value, {
    type: refundForm.type,
    reason: refundForm.reason,
    description: refundForm.description
  })
  refundOpen.value = false
  ok('退款申请已提交')
  load()
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-narrow m-section-sm">
      <div class="m-row m-dim m-mb-4" style="gap: 6px">
        <RouterLink to="/shop/orders" class="m-link">我的订单</RouterLink>
        <Icon name="right" :size="12" />
        <span class="m-num">{{ orderNumber }}</span>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 380px" />

      <template v-else-if="order">
        <div class="m-panel m-mb-4">
          <div class="m-between m-wrapflex" style="gap: 16px">
            <div>
              <div class="m-row" style="gap: 12px">
                <h1 class="m-h2">订单详情</h1>
                <MStatus :text="statusMeta.text" :tone="statusMeta.tone" />
              </div>
              <p class="m-muted m-mt-1" style="font-size: 13px">{{ statusMeta.hint }}</p>
            </div>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <button v-if="order.status === 'PENDING_PAYMENT'" class="m-btn m-btn-primary" type="button" @click="pay">
                去支付
              </button>
              <button v-if="order.status === 'PENDING_PAYMENT'" class="m-btn m-btn-soft" type="button" @click="cancel">
                取消订单
              </button>
              <button v-if="order.status === 'PENDING_RECEIPT'" class="m-btn m-btn-primary" type="button" @click="receipt">
                确认收货
              </button>
              <button
                v-if="canApplyRefund"
                class="m-btn m-btn-soft"
                type="button"
                @click="openRefund"
              >
                申请退款
              </button>
              <RouterLink
                v-if="order.status === 'COMPLETED'"
                :to="`/shop/orders/${orderNumber}/review`"
                class="m-btn m-btn-primary"
              >
                去评价
              </RouterLink>
            </div>
          </div>
        </div>

        <div class="m-panel m-mb-4" style="padding: 20px 24px">
          <div class="m-steps">
            <span class="m-step done"><i>✓</i> 提交订单</span>
            <Icon name="right" :size="14" class="m-muted" />
            <span
              class="m-step"
              :class="['PENDING_DELIVERY', 'PENDING_RECEIPT', 'COMPLETED'].includes(order.status) ? 'done' : 'now'"
            >
              <i>2</i> 支付完成
            </span>
            <Icon name="right" :size="14" class="m-muted" />
            <span class="m-step" :class="['PENDING_RECEIPT', 'COMPLETED'].includes(order.status) ? 'done' : ''">
              <i>3</i> 商家发货
            </span>
            <Icon name="right" :size="14" class="m-muted" />
            <span class="m-step" :class="order.status === 'COMPLETED' ? 'done' : ''"><i>4</i> 确认收货</span>
          </div>
        </div>

        <div class="info-grid m-mb-4">
          <div class="m-panel">
            <h2 class="m-h3 m-mb-4">收货信息</h2>
            <dl class="m-kv">
              <dt>收货人</dt>
              <dd>{{ order.consignee || order.receiver || '—' }}</dd>
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
              <dt>订单号</dt>
              <dd class="m-num">{{ order.orderNumber }}</dd>
              <dt>下单时间</dt>
              <dd>{{ shortTime(order.createTime) }}</dd>
              <dt>支付时间</dt>
              <dd>{{ shortTime(order.payTime) }}</dd>
              <dt>发货时间</dt>
              <dd>{{ shortTime(order.shipment?.shipTime || order.deliveryTime || order.shipTime) }}</dd>
              <dt>物流</dt>
              <dd>
                {{ order.shipment?.company || '—' }}
                {{ order.shipment?.trackingNo || '' }}
              </dd>
            </dl>
          </div>
        </div>

        <div class="m-panel m-mb-4">
          <h2 class="m-h3 m-mb-4">商品明细</h2>
          <div class="m-col" style="gap: 16px">
            <RouterLink
              v-for="it in items"
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
          <div class="m-col" style="gap: 8px">
            <div class="m-between">
              <span class="m-muted" style="font-size: 13px">商品金额</span>
              <span class="m-num">{{ money(order.totalAmount) }}</span>
            </div>
            <div class="m-between">
              <span class="m-muted" style="font-size: 13px">优惠</span>
              <span class="m-num">-{{ money(order.discountAmount ?? 0) }}</span>
            </div>
            <div class="m-between">
              <span class="m-muted" style="font-size: 13px">运费</span>
              <span class="m-num">{{ money(order.freightAmount ?? 0) }}</span>
            </div>
            <div class="m-between" style="align-items: baseline">
              <span class="m-h4">实付</span>
              <span class="m-price" style="font-size: 24px">{{ money(order.payAmount) }}</span>
            </div>
          </div>
        </div>

        <div v-if="refund" class="m-panel m-mb-4">
          <div class="m-between m-wrapflex m-mb-4" style="gap: 12px">
            <h2 class="m-h3">退款详情</h2>
            <MStatus :text="refundMeta.text" :tone="refundMeta.tone" />
          </div>

          <div class="refund-banner" :class="`is-${(refund.status || '').toLowerCase()}`">
            <p v-if="refund.status === 'PENDING'">退款申请已提交，正在等待商家审核。</p>
            <p v-else-if="refund.status === 'APPROVED'">退款已通过，款项将按原支付方式退回。</p>
            <p v-else-if="refund.status === 'REJECTED'">
              退款申请已被驳回<span v-if="refund.adminRemark">，商家意见：{{ refund.adminRemark }}</span>
            </p>
            <p v-else>退款单状态：{{ refundMeta.text }}</p>
          </div>

          <dl class="m-kv m-mt-4">
            <dt>退款单号</dt>
            <dd class="m-num">{{ refund.refundNumber || '—' }}</dd>
            <dt>退款类型</dt>
            <dd>{{ MALL_REFUND_TYPE[refund.type] || refund.typeDesc || refund.type || '—' }}</dd>
            <dt>退款金额</dt>
            <dd class="m-price m-num">{{ money(refund.amount ?? order.payAmount) }}</dd>
            <dt>申请原因</dt>
            <dd>{{ refund.reason || '—' }}</dd>
            <dt>补充说明</dt>
            <dd>{{ refund.description || '—' }}</dd>
            <dt>商家审核意见</dt>
            <dd :class="{ 'm-dim': !refund.adminRemark }">{{ refund.adminRemark || '暂无（审核通过后可见）' }}</dd>
            <dt>申请时间</dt>
            <dd>{{ shortTime(refund.createTime) }}</dd>
            <dt>审核时间</dt>
            <dd>{{ refund.auditTime ? shortTime(refund.auditTime) : '—' }}</dd>
          </dl>

          <div v-if="refund.status === 'REJECTED'" class="m-row m-mt-4" style="gap: 10px; align-items: center">
            <p class="m-dim" style="font-size: 13px">对审核结果有疑问，可修改说明后重新申请。</p>
            <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openRefund">重新申请退款</button>
          </div>
        </div>
      </template>

      <MEmpty v-else image="empty-order" icon="box" title="订单不存在">
        <RouterLink to="/shop/orders" class="m-btn m-btn-primary m-mt-2">返回订单列表</RouterLink>
      </MEmpty>
    </div>

    <MModal :open="refundOpen" title="申请退款（整单）" @close="refundOpen = false">
      <div class="m-col" style="gap: 16px">
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
          <input v-model="refundForm.reason" class="m-input" placeholder="如：不想要了" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">补充说明</label>
          <textarea v-model="refundForm.description" class="m-textarea" />
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
.info-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}
.refund-banner {
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
  background: var(--m-soft, #f4f5f7);
  border-left: 3px solid var(--m-muted, #b8bcc4);
}
.refund-banner.is-pending {
  background: #fff8e6;
  border-left-color: #e6a817;
}
.refund-banner.is-approved {
  background: #eaf7ef;
  border-left-color: #2f9e5f;
}
.refund-banner.is-rejected {
  background: #fdecec;
  border-left-color: #d64545;
}
</style>
