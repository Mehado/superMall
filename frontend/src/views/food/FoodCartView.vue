<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MStepper from '@/components/mall/MStepper.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { foodUserApi } from '@/api/delivery'
import { useCartStore } from '@/stores/cart'
import { money } from '@/utils/format'
import { ok } from '@/lib/toast'

const router = useRouter()
const cart = useCartStore()
const groups = ref([])
const loading = ref(true)

const grandTotal = computed(() =>
  groups.value.reduce(
    (sum, g) => sum + (g.items || []).reduce((s, i) => s + Number(i.price || 0) * Number(i.number || 0), 0),
    0
  )
)

function subtotal(group) {
  return (group.items || []).reduce((s, i) => s + Number(i.price || 0) * Number(i.number || 0), 0)
}

function countOf(group) {
  return (group.items || []).reduce((s, i) => s + Number(i.number || 0), 0)
}

async function load() {
  loading.value = true
  try {
    const data = await foodUserApi.cart()
    groups.value = Array.isArray(data) ? data : []
    cart.refreshFood()
  } finally {
    loading.value = false
  }
}

/** number = 0 即删除该行 */
async function changeNumber(item, number) {
  await foodUserApi.updateCart({ id: item.id, number })
  load()
}

async function removeItem(item) {
  await foodUserApi.removeCartItem(item.id)
  ok('已移除')
  load()
}

async function clearMerchant(group) {
  await foodUserApi.clearMerchantCart(group.merchantId)
  ok(`已清空「${group.shopName || group.merchantName || '该商家'}」的购物车`)
  load()
}

async function clearAll() {
  await foodUserApi.clearCart()
  ok('购物车已清空')
  load()
}

function checkout(group) {
  router.push({ path: '/food/checkout', query: { merchantId: group.merchantId } })
}

onMounted(load)
</script>

<template>
  <FoodLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Cart</span>
          <h1 class="m-h1 m-mt-2">购物车</h1>
          <p class="m-muted m-mt-1" style="font-size: 13px">不同商家的商品需要分开结算</p>
        </div>
        <button class="m-btn m-btn-soft m-btn-sm" type="button" :disabled="!groups.length" @click="clearAll">
          清空全部
        </button>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 300px" />

      <div v-else-if="groups.length" class="m-col" style="gap: 18px">
        <div v-for="group in groups" :key="group.merchantId" class="m-panel">
          <div class="m-between m-wrapflex" style="gap: 14px">
            <div class="m-row" style="gap: 12px">
              <span class="cart-shop-mark"><Icon name="store" :size="18" /></span>
              <div>
                <p class="m-h3">{{ group.shopName || group.merchantName || `商家 #${group.merchantId}` }}</p>
                <p class="m-dim m-mt-1">
                  共 {{ countOf(group) }} 件
                  <span v-if="group.deliveryFee !== undefined"> · 配送费 {{ money(group.deliveryFee) }}</span>
                </p>
              </div>
            </div>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="clearMerchant(group)">
                清空本店
              </button>
              <button class="m-btn m-btn-accent m-btn-sm" type="button" @click="checkout(group)">
                去结算本店
              </button>
            </div>
          </div>

          <div class="m-divider" />

          <div class="m-col" style="gap: 16px">
            <div v-for="item in group.items" :key="item.id" class="m-row m-row-top" style="gap: 14px">
              <img v-if="item.image" class="m-thumb-sm" :src="item.image" :alt="item.name" />
              <span v-else class="m-thumb-sm" style="display: grid; place-items: center">
                <Icon name="image" :size="16" />
              </span>
              <div class="m-grow">
                <div class="m-between" style="align-items: flex-start">
                  <RouterLink :to="`/food/products/${item.productId}`" class="m-h4">
                    {{ item.name || item.productName }}
                  </RouterLink>
                  <button class="m-btn-link" type="button" title="移除" @click="removeItem(item)">
                    <Icon name="trash" :size="15" />
                  </button>
                </div>
                <p class="m-dim">{{ item.skuName || item.sku || '默认规格' }}</p>
                <div class="m-between m-mt-3">
                  <span class="m-price" style="font-size: 17px">{{ money(item.price) }}</span>
                  <MStepper
                    :model-value="Number(item.number) || 1"
                    :min="0"
                    :max="99"
                    @change="(n) => changeNumber(item, n)"
                  />
                </div>
              </div>
            </div>
          </div>

          <div class="m-divider" />
          <div class="m-between">
            <span class="m-dim">本店小计</span>
            <span class="m-price" style="font-size: 20px">{{ money(subtotal(group)) }}</span>
          </div>
        </div>

        <div class="m-panel m-between">
          <div>
            <p class="m-dim">全部商家合计</p>
            <p class="m-price" style="font-size: 26px">{{ money(grandTotal) }}</p>
          </div>
          <p class="m-dim" style="max-width: 420px">每次结算一家店铺，配送更快。</p>
        </div>
      </div>

      <MEmpty v-else image="empty-cart" icon="cart" title="购物车是空的" desc="去挑一家店点单吧">
        <RouterLink to="/food" class="m-btn m-btn-accent m-mt-2">去点单</RouterLink>
      </MEmpty>
    </div>
  </FoodLayout>
</template>

<style scoped>
.cart-shop-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: var(--m-r-sm);
  background: var(--m-accent-soft);
  color: var(--m-accent-deep);
  flex-shrink: 0;
}
</style>
