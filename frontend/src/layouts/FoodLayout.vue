<script setup>
import { computed, onMounted } from 'vue'
import NavRail from '@/components/NavRail.vue'
import AppFooter from '@/components/AppFooter.vue'
import AppBackdrop from '@/components/AppBackdrop.vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'

/**
 * 外卖（用户端）外壳 —— 左侧竖向导航栏
 *
 * 与商城端 ShopLayout 同构，共用一个 NavRail 组件；差异只有两处：
 *   1. 配色：加 .navrail-root--food，把强调色从商城紫换成品牌绿
 *   2. 菜单内容：点餐 / 商家 / 购物车 / 订单
 *
 * 历史变更：原来这里是「顶部 AppHeader + 内容 + AppFooter」，
 * 已按用户要求换成与商城一致的左侧竖栏。
 */
const cart = useCartStore()
const auth = useAuthStore()

/** 左栏中间那组菜单 */
const NAV = [
  { to: '/food', label: '点餐', icon: 'fire' },
  { to: '/food/merchants', label: '商家', icon: 'store' },
  { to: '/food/cart', label: '购物车', icon: 'cart' },
  { to: '/food/orders', label: '订单', icon: 'list' }
]

/**
 * 整个外卖端用**同一档**背景（压淡 + 轻虚化）。
 * 原先是「只有首页压淡」，结果从首页点进任何子页背景会突然变清楚，像换了一张图 —— 已统一。
 */
const SOFT_VEIL = [0.82, 0.62, 0.86]

const accountTo = computed(() => (auth.isLoggedIn(ROLE.deliveryUser) ? '/food/me' : ''))

/** 底部圆头像里的首字母 —— 直接读本地会话里的资料，不再多发一次请求 */
const accountName = computed(() => {
  if (!auth.isLoggedIn(ROLE.deliveryUser)) return ''
  const profile = auth.profileOf(ROLE.deliveryUser)
  return profile?.nickname || profile?.username || ''
})

/** 底部圆形头像：有头像用图，没有就回退首字母 */
const accountAvatar = computed(() =>
  auth.isLoggedIn(ROLE.deliveryUser) ? auth.profileOf(ROLE.deliveryUser)?.avatar || '' : ''
)

onMounted(() => cart.refreshFood())
</script>

<template>
  <div class="navrail-root navrail-root--food">
    <AppBackdrop :blur="8" :veil="SOFT_VEIL" />
    <NavRail
      home-to="/food"
      logo-text="外"
      :nav="NAV"
      cart-to="/food/cart"
      :cart-count="cart.foodTotal"
      switch-to="/shop"
      switch-label="去逛商城"
      switch-icon="cart"
      :account-to="accountTo"
      :account-name="accountName"
      :account-avatar="accountAvatar"
      :login-query="{ role: ROLE.deliveryUser, redirect: '/food' }"
    />
    <div class="navrail-body">
      <main class="navrail-main"><slot /></main>
      <AppFooter variant="food" />
    </div>
  </div>
</template>
