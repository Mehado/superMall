<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { foodUserApi } from '@/api/delivery'
import { useCartStore } from '@/stores/cart'
import { money, toBodyTime } from '@/utils/format'
import { ok } from '@/lib/toast'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()

const merchantId = computed(() => route.query.merchantId)
const group = ref(null)
const addresses = ref([])
const order = ref(null)
const step = ref(1)
const submitting = ref(false)
const paying = ref(false)
const addressOpen = ref(false)

const form = reactive({
  addressId: '',
  remark: '',
  estimatedDeliveryTime: '',
  payMethod: 'MOCK'
})

const addressForm = reactive({
  id: null,
  consignee: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 1
})

const amount = computed(() =>
  (group.value?.items || []).reduce((s, i) => s + Number(i.price || 0) * Number(i.number || 0), 0)
)

async function load() {
  const [cartData, addr] = await Promise.all([foodUserApi.cart(), foodUserApi.addresses()])
  const groups = Array.isArray(cartData) ? cartData : []
  group.value = groups.find((g) => String(g.merchantId) === String(merchantId.value)) || groups[0] || null
  addresses.value = Array.isArray(addr) ? addr : addr?.records || []
  const def = addresses.value.find((a) => Number(a.isDefault) === 1) || addresses.value[0]
  if (def) form.addressId = def.id
}

function openAddress() {
  Object.assign(addressForm, {
    id: null,
    consignee: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: addresses.value.length ? 0 : 1
  })
  addressOpen.value = true
}

async function saveAddress() {
  if (!addressForm.consignee || !addressForm.phone || !addressForm.detail) {
    ok('请填写收货人、电话与详细地址')
    return
  }
  const body = {
    consignee: addressForm.consignee,
    phone: addressForm.phone,
    province: addressForm.province,
    city: addressForm.city,
    district: addressForm.district,
    detail: addressForm.detail,
    isDefault: Number(addressForm.isDefault)
  }
  await foodUserApi.createAddress(body)
  addressOpen.value = false
  ok('地址已新增')
  load()
}

async function submitOrder() {
  if (!form.addressId) {
    ok('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    // 下单令牌：10 分钟一次性
    const tokenRes = await foodUserApi.submitToken()
    const token = typeof tokenRes === 'string' ? tokenRes : tokenRes?.token || ''
    const created = await foodUserApi.submitOrder({
      addressId: Number(form.addressId),
      merchantId: Number(merchantId.value),
      remark: form.remark || undefined,
      estimatedDeliveryTime: form.estimatedDeliveryTime ? toBodyTime(form.estimatedDeliveryTime) : undefined,
      payMethod: form.payMethod,
      token
    })
    order.value = created
    step.value = 2
    ok('下单成功，15 分钟内未支付会自动关单')
    cart.refreshFood()
  } finally {
    submitting.value = false
  }
}

async function pay() {
  paying.value = true
  try {
    const tokenRes = await foodUserApi.payToken(order.value.orderNumber)
    const payToken = typeof tokenRes === 'string' ? tokenRes : tokenRes?.payToken || ''
    await foodUserApi.pay({ orderNumber: order.value.orderNumber, payToken })
    step.value = 3
    ok('支付成功')
  } finally {
    paying.value = false
  }
}

onMounted(load)
</script>

<template>
  <FoodLayout>
    <div class="m-wrap m-wrap-narrow m-section-sm">
      <span class="m-eyebrow">Checkout</span>
      <h1 class="m-h1 m-mt-2 m-mb-5">确认订单</h1>

      <div class="m-panel m-mb-4" style="padding: 20px 24px">
        <div class="m-steps">
          <span class="m-step" :class="step > 1 ? 'done' : 'now'"><i>1</i> 确认地址与备注</span>
          <Icon name="right" :size="14" class="m-muted" />
          <span class="m-step" :class="step === 2 ? 'now' : step > 2 ? 'done' : ''"><i>2</i> 在线支付</span>
          <Icon name="right" :size="14" class="m-muted" />
          <span class="m-step" :class="step === 3 ? 'done' : ''"><i>3</i> 等待商家接单</span>
        </div>
      </div>

      <template v-if="order">
        <div class="m-panel">
          <div class="m-between">
            <div>
              <p class="m-eyebrow">订单号</p>
              <p class="m-h3 m-num m-mt-1">{{ order.orderNumber }}</p>
            </div>
            <MStatus text="待支付" tone="amber" />
          </div>
          <div class="m-divider" />
          <dl class="m-kv">
            <dt>订单金额</dt>
            <dd class="m-price m-num" style="font-size: 22px">{{ money(order.orderAmount ?? order.amount) }}</dd>
            <dt>下单时间</dt>
            <dd>{{ order.orderTime || '—' }}</dd>
          </dl>
          <div class="m-row m-wrapflex m-mt-5" style="gap: 10px">
            <button v-if="step < 3" class="m-btn m-btn-accent m-btn-lg" type="button" :disabled="paying" @click="pay">
              {{ paying ? '支付中…' : '立即支付' }}
            </button>
            <RouterLink v-else to="/food/orders" class="m-btn m-btn-primary m-btn-lg">查看我的订单</RouterLink>
            <RouterLink v-if="step < 3" to="/food/orders" class="m-btn m-btn-outline m-btn-lg">稍后支付</RouterLink>
          </div>
        </div>
      </template>

      <template v-else-if="group">
        <div class="m-panel m-mb-4">
          <div class="m-between m-mb-4">
            <h2 class="m-h3">收货地址</h2>
            <button class="m-btn-link" type="button" @click="openAddress">+ 新增地址</button>
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
                </div>
                <p class="m-muted m-mt-1" style="font-size: 13px">
                  {{ a.province || '' }}{{ a.city || '' }}{{ a.district || '' }}{{ a.detail }}
                </p>
              </div>
            </label>
          </div>
          <MEmpty image="empty-address" v-else icon="pin" title="还没有收货地址" desc="先添加一个收货地址，才能提交订单">
            <button class="m-btn m-btn-accent m-mt-2" type="button" @click="openAddress">新增地址</button>
          </MEmpty>
        </div>

        <div class="m-panel m-mb-4">
          <h2 class="m-h3 m-mb-4">{{ group.shopName || group.merchantName || '本店商品' }}</h2>
          <div class="m-col" style="gap: 16px">
            <div v-for="i in group.items" :key="i.id" class="m-row" style="gap: 14px">
              <img v-if="i.image" class="m-thumb-sm" :src="i.image" :alt="i.name" />
              <span v-else class="m-thumb-sm" style="display: grid; place-items: center">
                <Icon name="image" :size="16" />
              </span>
              <div class="m-grow">
                <p class="m-h4 m-clamp-2">{{ i.name || i.productName }}</p>
                <p class="m-dim">{{ i.skuName || '默认规格' }}</p>
              </div>
              <span class="m-muted m-num" style="font-size: 13px">×{{ i.number }}</span>
              <span class="m-price">{{ money(Number(i.price) * Number(i.number)) }}</span>
            </div>
          </div>
        </div>

        <div class="m-panel m-mb-4">
          <h2 class="m-h3 m-mb-4">配送与备注</h2>
          <div class="deliver-grid">
            <div class="m-field">
              <label class="m-eyebrow">期望送达时间</label>
              <input v-model="form.estimatedDeliveryTime" type="datetime-local" class="m-input" />
              <p class="m-dim">我们会尽量按这个时间送达</p>
            </div>
            <div class="m-field">
              <label class="m-eyebrow">支付方式</label>
              <select v-model="form.payMethod" class="m-select">
                <option value="MOCK">在线支付</option>
              </select>
            </div>
          </div>
          <div class="m-field m-mt-4">
            <label class="m-eyebrow">备注</label>
            <textarea v-model="form.remark" class="m-textarea" placeholder="如：不要香菜、放前台（可留空）" />
          </div>
        </div>

        <div class="m-panel" style="position: sticky; bottom: 16px">
          <div class="m-between">
            <div>
              <p class="m-dim">已含配送费，最终金额以下单结果为准</p>
              <p class="m-price" style="font-size: 28px">{{ money(amount) }}</p>
            </div>
            <button class="m-btn m-btn-accent m-btn-lg" type="button" :disabled="submitting" @click="submitOrder">
              {{ submitting ? '提交中…' : '提交订单' }}
            </button>
          </div>
        </div>
      </template>

      <MEmpty v-else image="empty-cart" icon="cart" title="这家店的购物车是空的">
        <RouterLink to="/food/cart" class="m-btn m-btn-accent m-mt-2">回到购物车</RouterLink>
      </MEmpty>
    </div>

    <MModal :open="addressOpen" title="新增收货地址" @close="addressOpen = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">收货人</label>
            <input v-model="addressForm.consignee" class="m-input" placeholder="小明" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">手机号</label>
            <input v-model="addressForm.phone" class="m-input" placeholder="13800138000" />
          </div>
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">省</label>
            <input v-model="addressForm.province" class="m-input" placeholder="北京市" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">市</label>
            <input v-model="addressForm.city" class="m-input" placeholder="北京市" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">区</label>
            <input v-model="addressForm.district" class="m-input" placeholder="东城区" />
          </div>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">详细地址</label>
          <input v-model="addressForm.detail" class="m-input" placeholder="东华门大街 1 号" />
        </div>
        <label class="m-check">
          <input v-model="addressForm.isDefault" type="checkbox" :true-value="1" :false-value="0" />
          <span>设为默认地址</span>
        </label>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="addressOpen = false">取消</button>
        <button class="m-btn m-btn-accent" type="button" @click="saveAddress">保存</button>
      </template>
    </MModal>
  </FoodLayout>
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
.deliver-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}
</style>
