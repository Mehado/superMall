<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import AppBackdrop from '@/components/AppBackdrop.vue'
import { brandLogo, brandWordmark } from '@/lib/brand'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'

/**
 * 工作台外壳 —— 运营后台 / 商家端 / 骑手端共用。
 *
 * 版式：左侧栏（导航按 section 取，只定义一次）+ 纸面内容区。
 * 强调色由主题决定：/admin 是商城紫，/merchant 与 /rider 带 theme-food 走品牌绿。
 * 页面标题默认取路由 meta.title，动态标题（如「编辑商品 #1」）用 title 属性覆盖。
 */
const SECTIONS = {
  admin: {
    brandSub: '运营后台',
    role: ROLE.mallAdmin,
    nav: [
      { to: '/admin', label: '销售看板', icon: 'chart' },
      { to: '/admin/products', label: '商品管理', icon: 'box' },
      { to: '/admin/categories', label: '分类管理', icon: 'grid' },
      { to: '/admin/brands', label: '品牌管理', icon: 'tag' },
      { to: '/admin/banners', label: '推荐位', icon: 'image' },
      { to: '/admin/orders', label: '订单管理', icon: 'list' },
      { to: '/admin/refunds', label: '退款审核', icon: 'refresh' },
      { to: '/admin/reviews', label: '评价管理', icon: 'star' },
      { to: '/admin/members', label: '会员管理', icon: 'user' }
    ],
    subtitles: {
      '/admin': '电商板块 · 商品 / 订单 / 会员 / 统计',
      '/admin/products': '管理商品信息、库存与上下架',
      '/admin/categories': '支持多级分类，父分类下可继续加子分类',
      '/admin/brands': '品牌会作为前台筛选条件',
      '/admin/banners': '维护首页轮播与金刚区入口',
      '/admin/orders': '发货后订单流转到「待收货」',
      '/admin/refunds': '整单退款：同意后退款完成，驳回则订单回到申请前状态',
      '/admin/reviews': '可按商品 / 用户 / 星级筛选',
      '/admin/members': '查看买家账号、消费情况与状态'
    },
    fallbackSubtitle: '商品信息、图片与规格库存',
    fallbackName: 'admin'
  },
  merchant: {
    brandSub: '商家后台',
    role: ROLE.merchant,
    nav: [
      { to: '/merchant', label: '经营看板', icon: 'chart' },
      { to: '/merchant/orders', label: '订单管理', icon: 'list' },
      { to: '/merchant/products', label: '商品管理', icon: 'box' },
      { to: '/merchant/categories', label: '分类管理', icon: 'grid' },
      { to: '/merchant/shop', label: '店铺设置', icon: 'store' }
    ],
    subtitles: {
      '/merchant': '接单、备餐与今日经营情况',
      '/merchant/orders': '接单 / 备餐完成 / 拒单',
      '/merchant/products': '维护菜单与商品状态',
      '/merchant/categories': '分类决定商品的业态',
      '/merchant/shop': '营业状态会直接影响用户能否加购与下单'
    },
    fallbackSubtitle: '商品信息、价格与规格库存',
    fallbackName: '商家'
  },
  platform: {
    brandSub: '平台运营',
    role: ROLE.platformAdmin,
    nav: [
      { to: '/platform', label: '入驻审核', icon: 'shield' },
      { to: '/platform/riders', label: '骑手管理', icon: 'truck' },
      { to: '/platform/profile', label: '账号资料', icon: 'user' }
    ],
    subtitles: {
      '/platform': '审核商家入驻申请，通过后商家即可开店经营',
      '/platform/riders': '查看骑手并停用异常账号',
      '/platform/profile': '修改自己的姓名，查看账号状态'
    },
    fallbackSubtitle: '商家入驻与骑手管理',
    fallbackName: '平台运营'
  },
  rider: {
    brandSub: '骑手端',
    role: ROLE.rider,
    nav: [
      { to: '/rider', label: '抢单池', icon: 'bell' },
      { to: '/rider/mine', label: '我的配送', icon: 'truck' },
      { to: '/rider/profile', label: '骑手资料', icon: 'user' }
    ],
    subtitles: {
      '/rider': '抢单后到「我的配送」取餐与送达',
      '/rider/mine': '接单后请及时取餐与送达',
      '/rider/profile': '资料与配送统计'
    },
    fallbackSubtitle: '',
    fallbackName: '骑手'
  }
}

const props = defineProps({
  /** 工作台归属：admin | merchant | rider */
  section: { type: String, default: 'admin' },
  /** 覆盖标题（动态标题页用，如「编辑商品 #1」） */
  title: { type: String, default: '' },
  /** 覆盖副标题 */
  subtitle: { type: String, default: '' }
})

/** 工作台全是密集表格：背景压淡一档，别跟内容抢注意力 */
const WORKBENCH_VEIL = [0.86, 0.72, 0.9]

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const cfg = computed(() => SECTIONS[props.section] || SECTIONS.admin)

const pageTitle = computed(() => props.title || route.meta?.title || cfg.value.brandSub)
const pageSubtitle = computed(() => {
  if (props.subtitle) return props.subtitle
  if (cfg.value.subtitles[route.path]) return cfg.value.subtitles[route.path]
  return cfg.value.fallbackSubtitle
})

const userName = computed(() => {
  const p = auth.profileOf(cfg.value.role)
  return p?.nickname || p?.username || p?.name || p?.shopName || cfg.value.fallbackName
})
const initial = computed(() => String(userName.value).slice(0, 1).toUpperCase())

function isActive(to) {
  return to === cfg.value.nav[0].to ? route.path === to : route.path.startsWith(to)
}

function logout() {
  auth.logout(cfg.value.role)
  router.push({ path: '/login', query: { role: cfg.value.role } })
}
</script>

<template>
  <div class="admin-root">
    <AppBackdrop :blur="10" :veil="WORKBENCH_VEIL" />
    <aside class="admin-side">
      <RouterLink :to="cfg.nav[0].to" class="admin-logo">
        <span class="admin-logo-mark" :class="{ 'has-img': brandLogo }">
          <img v-if="brandLogo" :src="brandLogo" alt="" />
          <svg
            v-else
            width="18"
            height="18"
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
        <span class="admin-logo-text">
          <img v-if="brandWordmark" class="admin-logo-wordmark" :src="brandWordmark" alt="superMall" />
          <span v-else class="admin-logo-name">superMall</span>
          <span class="admin-logo-sub">{{ cfg.brandSub }}</span>
        </span>
      </RouterLink>

      <nav class="admin-nav">
        <RouterLink
          v-for="l in cfg.nav"
          :key="l.to"
          :to="l.to"
          class="admin-link"
          :class="{ active: isActive(l.to) }"
        >
          <Icon :name="l.icon" :size="17" />
          <span>{{ l.label }}</span>
        </RouterLink>
      </nav>

      <div class="admin-side-foot">
        <span class="admin-user">
          <span class="admin-avatar">{{ initial }}</span>
          <span class="admin-uname">{{ userName }}</span>
        </span>
        <button class="admin-logout" type="button" title="退出登录" @click="logout">
          <Icon name="logout" :size="16" />
          <span>退出</span>
        </button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-head">
        <div>
          <span class="m-eyebrow">{{ cfg.brandSub }}</span>
          <h1 class="m-h2" style="margin-top: 6px">{{ pageTitle }}</h1>
          <p v-if="pageSubtitle" class="m-muted" style="font-size: 13px; margin-top: 4px">
            {{ pageSubtitle }}
          </p>
        </div>
        <div class="m-row m-wrapflex" style="gap: 8px">
          <slot name="actions" />
        </div>
      </header>

      <slot />
    </main>
  </div>
</template>

<style scoped>
.admin-root {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: 100vh;
  /* 底色交给 AppBackdrop 的环境光 */
  background: transparent;
}

/* ── 侧栏（与主题同色） ─────────────────────────────────────────── */
.admin-side {
  position: sticky;
  top: 0;
  z-index: 2;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 22px 14px 18px;
  /* 玻璃质感：让背景柔光透过来 */
  background: rgba(255, 255, 255, 0.62);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  backdrop-filter: blur(16px) saturate(1.6);
  border-right: 1px solid var(--m-line);
  color: var(--m-taupe);
  overflow-y: auto;
}
.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 18px;
}
.admin-logo-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  background: var(--m-accent);
  color: #fff;
  flex-shrink: 0;
}
/* 换成 photo/logo 里的图后：去底色，圆角方形 + 完整显示 */
.admin-logo-mark.has-img {
  background: transparent;
  overflow: hidden;
}
.admin-logo-mark.has-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.admin-logo-wordmark {
  height: 18px;
  width: auto;
  display: block;
}
.admin-logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}
.admin-logo-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--m-ink);
}
.admin-logo-sub {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--m-taupe);
}
.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.admin-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--m-r-xs);
  font-size: 13px;
  font-weight: 500;
  color: var(--m-ink-2);
  transition: background-color 0.2s var(--m-ease), color 0.2s var(--m-ease);
}
.admin-link:hover {
  background: var(--m-bone-deep);
  color: var(--m-ink);
}
.admin-link.active {
  background: var(--m-accent-soft);
  color: var(--m-accent-deep);
  font-weight: 700;
}
.admin-side-foot {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--m-line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.admin-avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--m-accent);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.admin-uname {
  font-size: 12px;
  color: var(--m-ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.admin-logout {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border-radius: var(--m-pill);
  font-size: 12px;
  color: var(--m-taupe);
  transition: background-color 0.2s var(--m-ease), color 0.2s var(--m-ease);
}
.admin-logout:hover {
  background: var(--m-bone-deep);
  color: var(--m-ink);
}

/* ── 内容区 ───────────────────────────────────────────── */
.admin-main {
  position: relative;
  z-index: 1;
  min-width: 0;
  padding: 30px 34px 72px;
}
.admin-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
  padding-bottom: 20px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--m-line);
}

/* ── 移动端：侧栏翻成顶部横滑导航 ─────────────────────── */
@media (max-width: 900px) {
  .admin-root {
    grid-template-columns: minmax(0, 1fr);
  }
  .admin-side {
    position: sticky;
    top: 0;
    z-index: 30;
    height: auto;
    flex-direction: row;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: saturate(1.4) blur(14px);
    border-right: 0;
    border-bottom: 1px solid var(--m-line);
    overflow-x: auto;
  }
  .admin-logo {
    padding: 0;
    flex-shrink: 0;
  }
  .admin-logo-text {
    display: none;
  }
  .admin-nav {
    flex-direction: row;
    gap: 4px;
    flex: 1;
  }
  .admin-link {
    white-space: nowrap;
    padding: 8px 12px;
  }
  .admin-side-foot {
    margin-top: 0;
    padding-top: 0;
    border-top: 0;
    flex-shrink: 0;
  }
  .admin-uname,
  .admin-logout span {
    display: none;
  }
  .admin-main {
    padding: 20px 16px 64px;
  }
}
</style>
