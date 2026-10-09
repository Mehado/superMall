<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import Icon from '@/components/Icon.vue'
import { brandLogo } from '@/lib/brand'

/**
 * 左侧竖向导航栏 —— 商城端与外卖端共用（对齐 shop.app 的 icon rail）
 *
 * 结构：
 *   最上面 —— 小 Logo（只有图标/单字，没有文字）
 *   中间   —— 各端自己的菜单，**垂直居中**、**只放图标**
 *             文字做成 hover 提示（.navrail-tip）
 *   最下面 —— 登录（已登录则是圆形头像 + 昵称首字母）
 *
 * 两端差异只有「配色」和「菜单内容」，配色由外层 .navrail-root 上的
 * 修饰类决定（见 styles/rail.css），本组件不需要感知。
 */
const props = defineProps({
  /** 顶部 Logo 的跳转地址 */
  homeTo: { type: String, default: '/' },
  /** Logo 里的单字标记（如外卖的「外」）；留空则渲染默认的购物袋图标 */
  logoText: { type: String, default: '' },
  /** 菜单项：{ to, label, icon } */
  nav: { type: Array, default: () => [] },
  /** 哪一项挂购物车角标（通常是购物车那一项） */
  cartTo: { type: String, default: '' },
  cartCount: { type: Number, default: 0 },
  /** 已登录时的个人中心地址；为空则显示「登录」 */
  accountTo: { type: String, default: '' },
  /** 已登录用户的昵称，用来取头像首字母 */
  accountName: { type: String, default: '' },
  /** 已登录用户的头像 URL；有则直接显示，没有就回退首字母 */
  accountAvatar: { type: String, default: '' },
  /** 跨端切换：目标地址；为空则不渲染这个按钮（商城 → 外卖 / 外卖 → 商城） */
  switchTo: { type: String, default: '' },
  /** 跨端切换按钮的提示文字 */
  switchLabel: { type: String, default: '切换' },
  /** 跨端切换按钮的图标（用目标端的图标最直观） */
  switchIcon: { type: String, default: 'grid' },
  loginQuery: { type: Object, default: null }
})

const route = useRoute()

/** 头像里的首字母；取不到就退回一个「我」 */
const accountInitial = computed(() => {
  const name = String(props.accountName || '').trim()
  return name ? name.slice(0, 1).toUpperCase() : ''
})

function isActive(to) {
  if (to === props.homeTo) return route.path === to
  return route.path.startsWith(to)
}
</script>

<template>
  <aside class="navrail">
    <!-- 最上面：小 Logo，没有文字 -->
    <RouterLink :to="homeTo" class="navrail-logo" :class="{ 'has-img': brandLogo }" aria-label="返回首页">
      <img v-if="brandLogo" :src="brandLogo" alt="superMall" />
      <span v-else-if="logoText" class="navrail-logo-text">{{ logoText }}</span>
      <svg
        v-else
        width="22"
        height="22"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M5 8h14l-1.1 11.2a1.6 1.6 0 0 1-1.6 1.3H7.7a1.6 1.6 0 0 1-1.6-1.3L5 8Z" />
        <path d="M9 8V6.5a3 3 0 0 1 6 0V8" />
      </svg>
    </RouterLink>

    <!-- 中间：垂直居中、只有图标 -->
    <nav class="navrail-nav" aria-label="主导航">
      <RouterLink
        v-for="item in nav"
        :key="item.to"
        :to="item.to"
        class="navrail-item"
        :class="{ active: isActive(item.to) }"
        :aria-label="item.label"
      >
        <span class="navrail-ico">
          <Icon :name="item.icon" :size="24" />
          <span v-if="cartTo && item.to === cartTo && cartCount > 0" class="navrail-badge">
            {{ cartCount }}
          </span>
        </span>
        <span class="navrail-tip">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <!-- 最下面：跨端切换 + 登录 / 我的 -->
    <div class="navrail-foot">
      <RouterLink
        v-if="switchTo"
        :to="switchTo"
        class="navrail-item navrail-switch"
        :aria-label="switchLabel"
      >
        <span class="navrail-ico"><Icon :name="switchIcon" :size="22" /></span>
        <span class="navrail-tip">{{ switchLabel }}</span>
      </RouterLink>

      <RouterLink
        v-if="accountTo"
        :to="accountTo"
        class="navrail-item"
        aria-label="我的"
      >
        <span class="navrail-avatar" :class="{ 'has-img': accountAvatar }">
          <img v-if="accountAvatar" :src="accountAvatar" alt="" />
          <template v-else>{{ accountInitial || '我' }}</template>
        </span>
        <span class="navrail-tip">我的</span>
      </RouterLink>
      <RouterLink
        v-else
        :to="{ path: '/login', query: loginQuery || {} }"
        class="navrail-item"
        aria-label="登录"
      >
        <span class="navrail-avatar is-guest"><Icon name="user" :size="21" /></span>
        <span class="navrail-tip">登录</span>
      </RouterLink>
    </div>
  </aside>
</template>
