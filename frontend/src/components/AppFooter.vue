<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { brandLogo, brandWordmark } from '@/lib/brand'

/**
 * 共用页脚（商城端 + 外卖端）—— 与 NavRail 同理，是同构外壳的一部分。
 * 深墨底 + 纸白字，两端只差文案与入口；跨端只留一个「返回主页面」由用户自己分流。
 */
const props = defineProps({
  variant: { type: String, default: 'shop' } // shop | food
})

const META = {
  shop: {
    sub: '商城',
    tagline: '好物齐全，一件也包邮。',
    links: [
      { to: '/shop', label: '商城首页' },
      { to: '/shop/search', label: '全部商品' },
      { to: '/shop/cart', label: '购物车' },
      { to: '/shop/orders', label: '我的订单' },
      { to: '/shop/favorites', label: '我的收藏' }
    ],
    services: ['正品保障', '7 天无理由退货', '48 小时发货'],
    bottom: 'superMall 商城'
  },
  food: {
    sub: '外卖',
    tagline: '附近好店，热乎送到。',
    links: [
      { to: '/food', label: '点餐首页' },
      { to: '/food/merchants', label: '全部商家' },
      { to: '/food/cart', label: '购物车' },
      { to: '/food/orders', label: '我的订单' }
    ],
    services: ['食材新鲜', '准时送达', '超时必赔'],
    bottom: 'superMall 外卖'
  }
}

const meta = computed(() => META[props.variant] || META.shop)
</script>

<template>
  <footer class="m-footer">
    <div class="m-wrap">
      <div class="m-footer-grid">
        <div>
          <RouterLink :to="meta.links[0].to" class="m-logo" style="margin-bottom: 16px">
            <span class="m-logo-mark" :class="{ 'has-img': brandLogo }">
              <img v-if="brandLogo" :src="brandLogo" alt="" />
              <template v-else-if="variant === 'food'">外</template>
              <svg
                v-else
                width="19"
                height="19"
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
            </span>
            <span class="m-logo-text">
              <img v-if="brandWordmark" class="m-logo-wordmark" :src="brandWordmark" alt="superMall" />
              <span v-else class="m-logo-name">superMall</span>
              <span class="m-logo-sub">{{ meta.sub }}</span>
            </span>
          </RouterLink>
          <p style="font-size: 13px; max-width: 260px">{{ meta.tagline }}</p>
          <RouterLink to="/" class="m-btn m-btn-outline m-btn-sm m-mt-4">
            返回主页面
          </RouterLink>
        </div>

        <div>
          <h4>导航</h4>
          <ul class="m-col" style="gap: 10px">
            <li v-for="l in meta.links" :key="l.to">
              <RouterLink :to="l.to">{{ l.label }}</RouterLink>
            </li>
          </ul>
        </div>

        <div>
          <h4>服务保障</h4>
          <ul class="m-col" style="gap: 10px">
            <li v-for="s in meta.services" :key="s"><span>{{ s }}</span></li>
          </ul>
        </div>

        <div>
          <h4>联系我们</h4>
          <ul class="m-col" style="gap: 10px">
            <li><span>客服热线 400-000-0000</span></li>
            <li><span>服务时间 09:00 - 22:00</span></li>
          </ul>
        </div>
      </div>

      <div class="m-footer-bottom">
        <span>© {{ new Date().getFullYear() }} {{ meta.bottom }}</span>
        <span>本页面为教学演示项目，支付为模拟流程</span>
      </div>
    </div>
  </footer>
</template>
