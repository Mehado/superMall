<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MStepper from '@/components/mall/MStepper.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallCartApi } from '@/api/mall'
import { useCartStore } from '@/stores/cart'
import { ok } from '@/lib/toast'
import { money } from '@/utils/format'

const router = useRouter()
const cart = useCartStore()

const raw = ref(null)
const loading = ref(true)
const busy = ref(false)

const items = computed(() => raw.value?.items || raw.value?.records || raw.value?.list || [])
const validItems = computed(() => items.value.filter((i) => i.valid !== false))
const invalidItems = computed(() => items.value.filter((i) => i.valid === false))
const allSelected = computed(
  () => validItems.value.length > 0 && validItems.value.every((i) => Number(i.selected) === 1)
)

/** 结算金额以服务端 selectedAmount 为准，前端只在缺失时兜底算一次 */
const fallbackAmount = computed(() =>
  validItems.value
    .filter((i) => Number(i.selected) === 1)
    .reduce((sum, i) => sum + Number(i.price || 0) * Number(i.number || 0), 0)
)
const selectedAmount = computed(() => raw.value?.selectedAmount ?? fallbackAmount.value)
const selectedCount = computed(
  () =>
    validItems.value
      .filter((i) => Number(i.selected) === 1)
      .reduce((s, i) => s + Number(i.number || 0), 0)
)

async function load() {
  loading.value = true
  try {
    raw.value = await mallCartApi.list()
    cart.refreshMall()
  } finally {
    loading.value = false
  }
}

async function changeNumber(item, number) {
  busy.value = true
  try {
    await mallCartApi.updateNumber(item.id, number)
    await load()
  } finally {
    busy.value = false
  }
}

async function toggleRow(item) {
  if (item.valid === false) {
    ok(`该商品已失效，不能选中（${item.invalidReason || '不可购买'}）`)
    return
  }
  await mallCartApi.selectRow(item.id, Number(item.selected) === 1 ? 0 : 1)
  await load()
}

async function toggleAll() {
  await mallCartApi.selectAll(allSelected.value ? 0 : 1)
  await load()
}

async function removeRow(item) {
  await mallCartApi.removeRow(item.id)
  ok('已删除该商品')
  await load()
}

async function clearScope(scope) {
  await mallCartApi.clear(scope)
  ok(scope === 'invalid' ? '已清理失效商品' : '购物车已清空')
  await load()
}

function checkout() {
  if (!selectedCount.value) {
    ok('请先勾选要结算的商品')
    return
  }
  router.push('/shop/checkout')
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Cart</span>
          <h1 class="m-h1 m-mt-2">购物车</h1>
        </div>
        <div class="m-row" style="gap: 8px">
          <button class="m-btn m-btn-soft m-btn-sm" type="button" :disabled="!invalidItems.length" @click="clearScope('invalid')">
            清理失效
          </button>
          <button class="m-btn m-btn-soft m-btn-sm" type="button" :disabled="!items.length" @click="clearScope('all')">
            清空购物车
          </button>
        </div>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 340px" />

      <div v-else-if="items.length" class="cart-layout">
        <div class="m-col" style="gap: 12px">
          <div class="m-panel m-row" style="padding: 16px 20px">
            <label class="m-check">
              <input type="checkbox" :checked="allSelected" @change="toggleAll" />
              <span>全选</span>
            </label>
            <div class="m-spacer" />
            <span class="m-dim m-num">共 {{ items.length }} 种商品</span>
          </div>

          <div
            v-for="item in items"
            :key="item.id"
            class="cart-row"
            :class="{ invalid: item.valid === false }"
          >
            <label class="m-check" style="align-self: flex-start; padding-top: 20px">
              <input
                type="checkbox"
                :checked="Number(item.selected) === 1 && item.valid !== false"
                :disabled="item.valid === false"
                @change="toggleRow(item)"
              />
            </label>

            <RouterLink :to="`/shop/products/${item.productId || item.spuId}`" class="cart-thumb">
              <img v-if="item.image" class="m-thumb" :src="item.image" :alt="item.name" />
              <span v-else class="m-thumb" style="display: grid; place-items: center">
                <Icon name="image" :size="20" />
              </span>
            </RouterLink>

            <div class="m-grow m-col" style="gap: 6px">
              <div class="m-between" style="align-items: flex-start">
                <span class="m-h4 m-clamp-2">{{ item.name || item.productName }}</span>
                <button class="m-btn-link" type="button" title="删除" @click="removeRow(item)">
                  <Icon name="trash" :size="15" />
                </button>
              </div>
              <span v-if="item.skuName || item.specValues?.length" class="m-dim">
                {{ item.skuName || item.specValues.join(' / ') }}
              </span>
              <span v-if="item.valid === false" class="m-badge m-badge-danger" style="align-self: flex-start">
                已失效：{{ item.invalidReason || '不可购买' }}
              </span>
              <div class="m-between m-mt-2">
                <span class="m-price" style="font-size: 17px">{{ money(item.price) }}</span>
                <MStepper
                  :model-value="Number(item.number) || 1"
                  :max="999"
                  :disabled="busy"
                  @change="(n) => changeNumber(item, n)"
                />
              </div>
            </div>
          </div>

          <p v-if="invalidItems.length" class="m-dim" style="line-height: 1.8">
            失效商品 {{ invalidItems.length }} 件：不会计入金额，也不能被选中，可点「清理失效」移除。
          </p>
        </div>

        <aside class="cart-summary">
          <h2 class="m-h3">结算信息</h2>
          <div class="m-divider" />
          <div class="m-between">
            <span class="m-muted" style="font-size: 13px">已选商品</span>
            <span class="m-num">{{ selectedCount }} 件</span>
          </div>
          <div class="m-between m-mt-2">
            <span class="m-muted" style="font-size: 13px">商品金额</span>
            <span class="m-price">{{ money(selectedAmount) }}</span>
          </div>
          <div class="m-between m-mt-2">
            <span class="m-muted" style="font-size: 13px">运费</span>
            <span class="m-num">{{ money(raw?.freightAmount ?? 0) }}</span>
          </div>
          <div class="m-divider" />
          <div class="m-between" style="align-items: baseline">
            <span class="m-h4">应付</span>
            <span class="m-price" style="font-size: 26px">{{ money(raw?.payAmount ?? selectedAmount) }}</span>
          </div>
          <button class="m-btn m-btn-primary m-btn-lg m-btn-block m-mt-4" type="button" @click="checkout">
            去结算
          </button>
          <p class="m-dim m-mt-2">最终金额以下单页结算为准。</p>
        </aside>
      </div>

      <MEmpty v-else image="empty-cart" icon="cart" title="购物车是空的" desc="去首页挑几件商品吧">
        <RouterLink to="/shop" class="m-btn m-btn-primary m-mt-2">去逛逛</RouterLink>
      </MEmpty>
    </div>
  </ShopLayout>
</template>

<style scoped>
.cart-layout {
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(0, 1fr) 340px;
  align-items: start;
}
@media (max-width: 899px) {
  .cart-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
.cart-row {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: var(--m-paper);
  border: 1px solid var(--m-line);
  border-radius: var(--m-r);
  transition: border-color 0.2s var(--m-ease);
}
.cart-row:hover {
  border-color: var(--m-line-2);
}
.cart-row.invalid {
  opacity: 0.6;
}
.cart-thumb {
  flex-shrink: 0;
}
.cart-summary {
  position: sticky;
  top: 24px;
  padding: 24px;
  background: var(--m-paper);
  border: 1px solid var(--m-line);
  border-radius: var(--m-r);
}
@media (max-width: 899px) {
  .cart-summary {
    position: static;
  }
}
</style>
