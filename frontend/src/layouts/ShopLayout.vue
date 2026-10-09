<script setup>
import { computed, onMounted } from 'vue'
import NavRail from '@/components/NavRail.vue'
import AppFooter from '@/components/AppFooter.vue'
import AppBackdrop from '@/components/AppBackdrop.vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'

/**
 * 商城（买家端）外壳 —— 左侧竖向导航栏
 *
 * 版式对齐 shop.app：
 *   左边一列菜单（顶部小 Logo / 中间图标垂直居中 / 最底部登录）
 *   右边是内容区 + 页脚
 *
 * 商城端 13 个页面共用本外壳；与外卖端 FoodLayout、运营后台 AdminLayout 完全隔离。
 *
 * 历史变更：原来这里是「顶部 MallHeader + 内容」，已按用户要求整体换成左侧竖栏，
 * MallHeader.vue 随之删除。
 */
const cart = useCartStore()
const auth = useAuthStore()

/** 左栏中间那组菜单 */
const NAV = [
  { to: '/shop', label: '推荐', icon: 'sparkles' },
  { to: '/shop/search', label: '分类', icon: 'grid' },
  { to: '/shop/favorites', label: '喜欢', icon: 'heart' },
  { to: '/shop/cart', label: '购物车', icon: 'cart' },
  { to: '/shop/orders', label: '订单', icon: 'list' }
]

/**
 * 整个商城端用**同一档**背景（压淡 + 轻虚化）。
 * 原先是「只有首页压淡」，结果从首页点进任何子页背景会突然变清楚，像换了一张图 —— 已统一。
 */
const SOFT_VEIL = [0.82, 0.62, 0.86]

const accountTo = computed(() => (auth.isLoggedIn(ROLE.mallUser) ? '/shop/me' : ''))

/** 底部圆头像里的首字母 —— 直接读本地会话里的资料，不再多发一次请求 */
const accountName = computed(() => {
  if (!auth.isLoggedIn(ROLE.mallUser)) return ''
  const profile = auth.profileOf(ROLE.mallUser)
  return profile?.nickname || profile?.username || ''
})

/** 底部圆形头像：有头像用图，没有就回退首字母 */
const accountAvatar = computed(() =>
  auth.isLoggedIn(ROLE.mallUser) ? auth.profileOf(ROLE.mallUser)?.avatar || '' : ''
)

onMounted(() => cart.refreshMall())
</script>

<template>
  <div class="navrail-root">
    <AppBackdrop :blur="8" :veil="SOFT_VEIL" />
    <NavRail
      home-to="/shop"
      :nav="NAV"
      cart-to="/shop/cart"
      :cart-count="cart.mallTotal"
      switch-to="/food"
      switch-label="去点外卖"
      switch-icon="truck"
      :account-to="accountTo"
      :account-name="accountName"
      :account-avatar="accountAvatar"
      :login-query="{ role: ROLE.mallUser, redirect: '/shop' }"
    />
    <div class="navrail-body">
      <main class="navrail-main"><slot /></main>
      <AppFooter variant="shop" />
    </div>
  </div>
</template>
