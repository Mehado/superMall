<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { mallAddressApi, mallCartApi, mallOrderApi, mallPaymentApi } from '@/api/mall'
import { useCartStore } from '@/stores/cart'
import { money } from '@/utils/format'
import { ok } from '@/lib/toast'

const router = useRouter()
const cart = useCartStore()

const form = reactive({ addressId: '', remark: '' })
const addresses = ref([])
const cartData = ref(null)
const order = ref(null)
const loading = ref(true)
const submitting = ref(false)
const paying = ref(false)
const step = ref(1) // 1 确认订单 → 2 支付 → 3 完成

const items = computed(() =>
  (cartData.value?.items || []).filter((i) => Number(i.selected) === 1 && i.valid !== false)
)
const amount = computed(() => cartData.value?.selectedAmount ?? 0)

async function load() {
  loading.value = true
  try {
    const [list, cartRes] = await Promise.all([mallAddressApi.list(), mallCartApi.list()])
    addresses.value = Array.isArray(list) ? list : list?.records || []
    cartData.value = cartRes
    const def = addresses.value.find((a) => Number(a.isDefault) === 1) || addresses.value[0]
    if (def) form.addressId = def.id
  } finally {
    loading.value = false
  }
}

/** 下单：先取一次性令牌，再提交（令牌失败也会被消耗，必须重新取） */
async function submitOrder() {
  if (!form.addressId) {
    ok('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const tokenRes = await mallOrderApi.submitToken()
    const submitToken = tokenRes?.submitToken || tokenRes
    const created = await mallOrderApi.submit({
      addressId: form.addressId,
      remark: form.remark,
      submitToken
    })
    order.value = created
    step.value = 2
    ok('下单成功，请完成支付')
    cart.refreshMall()
  } finally {
    submitting.value = false
  }
}

/** 支付：再取一次一次性令牌（5 分钟有效） */
async function pay() {
  paying.value = true
  try {
    const tokenRes = await mallPaymentApi.payToken(order.value.orderNumber)
    const payToken = tokenRes?.payToken || tokenRes
    await mallPaymentApi.pay({ orderNumber: order.value.orderNumber, payToken })
    step.value = 3
    ok('支付成功')
  } finally {
    paying.value = false
  }
}

function goOrders() {
  router.push('/shop/orders')
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-narrow m-section-sm">
      <span class="m-eyebrow">Checkout</span>
      <h1 class="m-h1 m-mt-2 m-mb-5">确认订单</h1>

      <div class="m-panel m-mb-4" style="padding: 20px 24px">
        <div class="m-steps">
          <span class="m-step" :class="step > 1 ? 'done' : 'now'"><i>1</i> 确认收货信息</span>
          <Icon name="right" :size="14" class="m-muted" />
          <span class="m-step" :class="step === 2 ? 'now' : step > 2 ? 'done' : ''"><i>2</i> 在线支付</span>
          <Icon name="right" :size="14" class="m-muted" />
          <span class="m-step" :class="step === 3 ? 'done' : ''"><i>3</i> 完成</span>
        </div>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 320px" />

      <!-- 已下单，进入支付 -->
      <template v-else-if="order">
        <div class="m-panel">
          <div class="m-between">
            <div>
              <p class="m-eyebrow">订单号</p>
              <p class="m-h3 m-num m-mt-1">{{ order.orderNumber }}</p>
            </div>
            <MStatus text="待付款" tone="amber" />
          </div>
          <div class="m-divider" />
          <dl class="m-kv">
            <dt>商品金额</dt>
            <dd class="m-num">{{ money(order.totalAmount) }}</dd>
            <dt>优惠金额</dt>
            <dd class="m-num">{{ money(order.discountAmount ?? 0) }}</dd>
            <dt>运费</dt>
            <dd class="m-num">{{ money(order.freightAmount ?? 0) }}</dd>
            <dt>应付</dt>
            <dd class="m-price m-num" style="font-size: 22px">{{ money(order.payAmount) }}</dd>
            <dt>创建时间</dt>
            <dd>{{ order.createTime }}</dd>
            <dt>支付截止</dt>
            <dd>{{ order.expireTime }}</dd>
          </dl>
          <div class="m-row m-wrapflex m-mt-5" style="gap: 10px">
            <button v-if="step < 3" class="m-btn m-btn-primary m-btn-lg" type="button" :disabled="paying" @click="pay">
              <Icon name="wallet" :size="17" />
              {{ paying ? '支付中…' : '立即支付' }}
            </button>
            <button v-else class="m-btn m-btn-primary m-btn-lg" type="button" @click="goOrders">
              查看我的订单
            </button>
            <button v-if="step < 3" class="m-btn m-btn-outline m-btn-lg" type="button" @click="goOrders">
              稍后支付
            </button>
          </div>
        </div>
      </template>

      <!-- 确认页 -->
      <template v-else-if="items.length">
        <div class="m-panel m-mb-4">
          <div class="m-between m-mb-4">
            <h2 class="m-h3">收货地址</h2>
            <RouterLink to="/shop/addresses" class="m-btn-link">管理地址</RouterLink>
          </div>
          <div v-if="addresses.length" class="m-col" style="gap: 10px">
            <label
              v-for="a in addresses"
              :key="a.id"
              class="addr-option"
              :class="{ active: String(form.addressId) === String(a.id) }"
            >
              <input v-model="form.addressId" type="radio" :value="a.id" />
              <div class="m-grow">
                <div class="m-row m-wrapflex" style="gap: 8px">
                  <span class="m-h4">{{ a.consignee }}</span>
                  <span class="m-muted m-num" style="font-size: 13px">{{ a.phone }}</span>
                  <span v-if="Number(a.isDefault) === 1" class="m-badge m-badge-accent">默认</span>
                  <span v-if="a.label" class="m-badge m-badge-soft">{{ a.label }}</span>
                </div>
                <p class="m-muted m-mt-1" style="font-size: 13px">
                  {{ a.province || '' }}{{ a.city || '' }}{{ a.district || '' }}{{ a.detail }}
                </p>
              </div>
            </label>
          </div>
          <MEmpty image="empty-address" v-else icon="pin" title="还没有收货地址" desc="先添加一个收货地址，才能提交订单">
            <RouterLink to="/shop/addresses" class="m-btn m-btn-primary m-mt-2">去新增地址</RouterLink>
          </MEmpty>
        </div>

        <div class="m-panel m-mb-4">
          <h2 class="m-h3 m-mb-4">商品清单</h2>
          <div class="m-col" style="gap: 16px">
            <div v-for="i in items" :key="i.id" class="m-row" style="gap: 14px">
              <img v-if="i.image" class="m-thumb-sm" :src="i.image" :alt="i.name" />
              <span v-else class="m-thumb-sm" style="display: grid; place-items: center">
                <Icon name="image" :size="16" />
              </span>
              <div class="m-grow">
                <p class="m-h4 m-clamp-2">{{ i.name || i.productName }}</p>
                <p class="m-dim">{{ i.skuName || (i.specValues || []).join(' / ') }}</p>
              </div>
              <span class="m-muted m-num" style="font-size: 13px">×{{ i.number }}</span>
              <span class="m-price">{{ money(Number(i.price) * Number(i.number)) }}</span>
            </div>
          </div>
        </div>

        <div class="m-panel m-mb-4">
          <h2 class="m-h3 m-mb-3">订单备注</h2>
          <textarea v-model="form.remark" class="m-textarea" placeholder="如：工作日送货、放门口（可留空）" />
        </div>

        <div class="m-panel checkout-bar">
          <div class="m-between">
            <div>
              <p class="m-dim">最终支付金额以提交后为准</p>
              <p class="m-price" style="font-size: 28px">{{ money(amount) }}</p>
            </div>
            <button class="m-btn m-btn-primary m-btn-lg" type="button" :disabled="submitting" @click="submitOrder">
              {{ submitting ? '提交中…' : '提交订单' }}
            </button>
          </div>
        </div>
      </template>

      <MEmpty image="empty-data" v-else icon="cart" title="没有已选中的商品">
        <RouterLink to="/shop/cart" class="m-btn m-btn-primary m-mt-2">回到购物车</RouterLink>
      </MEmpty>
    </div>
  </ShopLayout>
</template>

<style scoped>
.addr-option {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 16px 18px;
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-sm);
  background: var(--m-paper);
  cursor: pointer;
  transition: border-color 0.2s var(--m-ease);
}
.addr-option:hover {
  border-color: var(--m-line-2);
}
.addr-option.active {
  border-color: var(--m-accent);
  box-shadow: 0 0 0 3px var(--m-accent-soft);
}
.addr-option input {
  margin-top: 3px;
  width: 17px;
  height: 17px;
  accent-color: var(--m-accent);
}
.checkout-bar {
  position: sticky;
  bottom: 16px;
}
</style>
