<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import AvatarUploader from '@/components/AvatarUploader.vue'
import ProfileEditModal from '@/components/ProfileEditModal.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { mallAuthApi, mallCenterApi, mallOrderApi } from '@/api/mall'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { ROLE } from '@/utils/request'
import { MALL_ORDER_STATUS, money, shortTime, statusOf } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const profile = ref(null)
const favoriteCount = ref(0)
const recentOrders = ref([])
const orderTotal = ref(0)
const loading = ref(true)
const profileOpen = ref(false)

const shortcuts = [
  { to: '/shop/orders', label: '我的订单', icon: 'list' },
  { to: '/shop/favorites', label: '我的收藏', icon: 'heart' },
  { to: '/shop/footprints', label: '我的足迹', icon: 'eye' },
  { to: '/shop/reviews', label: '我的评价', icon: 'star' },
  { to: '/shop/addresses', label: '收货地址', icon: 'pin' },
  { to: '/shop/cart', label: '购物车', icon: 'cart' }
]

const nickname = computed(
  () => profile.value?.nickname || profile.value?.username || auth.profileOf(ROLE.mallUser)?.username || '商城买家'
)
const initial = computed(() => String(nickname.value).slice(0, 1).toUpperCase())

async function load() {
  loading.value = true
  try {
    const [p, c, o] = await Promise.all([
      mallAuthApi.profile().catch(() => null),
      mallCenterApi.favoriteCount().catch(() => 0),
      mallOrderApi.page({ page: 1, pageSize: 3 }).catch(() => null)
    ])
    profile.value = p
    favoriteCount.value = typeof c === 'number' ? c : c?.count ?? c?.total ?? 0
    recentOrders.value = o?.records || o?.list || []
    orderTotal.value = o?.total ?? recentOrders.value.length
    if (p) auth.setProfile(ROLE.mallUser, p)
  } finally {
    loading.value = false
  }
}

/** 头像上传完成后的保存（两步：先传 OSS 拿 URL，再写进资料） */
async function saveAvatar(url) {
  await mallAuthApi.updateProfile({ avatar: url })
  await load()
}

function openProfile() {
  profileOpen.value = true
}

/** 保存资料（弹窗组件负责提示与关闭，这里只落库 + 刷新） */
async function saveProfile(payload) {
  await mallAuthApi.updateProfile(payload)
  await load()
}

function logout() {
  auth.logout(ROLE.mallUser)
  cart.refreshMall()
  router.push({ path: '/login', query: { role: ROLE.mallUser } })
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <!-- 资料卡 -->
      <div class="profile-head">
        <AvatarUploader
          :src="profile?.avatar"
          :initial="initial"
          :uploader="mallAuthApi.uploadAvatar"
          :saver="saveAvatar"
          :size="76"
        />
        <div class="m-grow">
          <span class="m-eyebrow">My account</span>
          <h1 class="m-h2" style="margin-top: 6px">{{ nickname }}</h1>
          <p class="m-muted" style="font-size: 13px; margin-top: 4px">
            {{ profile?.email || profile?.username || '—' }}
            <span v-if="profile?.createTime"> · 注册于 {{ shortTime(profile.createTime) }}</span>
          </p>
        </div>
        <div class="m-row m-wrapflex" style="gap: 10px">
          <button class="m-btn m-btn-outline" type="button" @click="openProfile">
            编辑资料
          </button>
          <RouterLink to="/shop/orders" class="m-btn m-btn-outline">
            我的订单
          </RouterLink>
          <button class="m-btn m-btn-outline" type="button" @click="logout">
            <Icon name="logout" :size="16" />
            退出登录
          </button>
        </div>
      </div>

      <!-- 统计 -->
      <div class="m-stats m-mb-4">
        <div class="m-stat">
          <p class="m-eyebrow">购物车商品</p>
          <p class="m-stat-val m-mt-2">{{ cart.mallTotal }}</p>
          <p class="m-dim m-mt-1">失效 {{ cart.mallInvalid }} 件</p>
        </div>
        <div class="m-stat">
          <p class="m-eyebrow">收藏商品</p>
          <p class="m-stat-val m-mt-2">{{ favoriteCount }}</p>
          <p class="m-dim m-mt-1">含已下架商品</p>
        </div>
        <div class="m-stat">
          <p class="m-eyebrow">我的订单</p>
          <p class="m-stat-val m-mt-2">{{ orderTotal }}</p>
          <p class="m-dim m-mt-1">包含全部状态</p>
        </div>
      </div>

      <!-- 常用入口 -->
      <div class="m-panel m-mb-4">
        <h2 class="m-h3 m-mb-4">常用入口</h2>
        <div class="shortcut-grid">
          <RouterLink v-for="s in shortcuts" :key="s.to" :to="s.to" class="shortcut">
            <span class="shortcut-ico"><Icon :name="s.icon" :size="18" /></span>
            <span class="m-h4">{{ s.label }}</span>
            <Icon name="right" :size="15" class="shortcut-arrow" />
          </RouterLink>
        </div>
      </div>

      <!-- 最近订单 -->
      <div class="m-panel">
        <div class="m-between m-mb-4">
          <h2 class="m-h3">最近订单</h2>
          <RouterLink to="/shop/orders" class="m-btn-link">全部订单</RouterLink>
        </div>

        <div v-if="loading" class="m-skeleton" style="height: 120px" />
        <div v-else-if="recentOrders.length" class="m-col" style="gap: 10px">
          <RouterLink
            v-for="o in recentOrders"
            :key="o.orderNumber"
            :to="`/shop/orders/${o.orderNumber}`"
            class="m-listrow m-between"
          >
            <div>
              <p class="m-h4 m-num">{{ o.orderNumber }}</p>
              <p class="m-dim">{{ shortTime(o.createTime) }}</p>
            </div>
            <div class="m-row" style="gap: 12px">
              <span class="m-price">{{ money(o.payAmount) }}</span>
              <MStatus
                :text="statusOf(MALL_ORDER_STATUS, o.status).text"
                :tone="statusOf(MALL_ORDER_STATUS, o.status).tone"
              />
            </div>
          </RouterLink>
        </div>
        <p v-else class="m-dim">还没有订单</p>
      </div>
    </div>

    <!-- 编辑资料（与外卖买家共用组件） -->
    <ProfileEditModal
      :open="profileOpen"
      :profile="profile || {}"
      :saver="saveProfile"
      @close="profileOpen = false"
      @saved="load"
    />
  </ShopLayout>
</template>

<style scoped>
.profile-head {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  padding: 32px;
  margin-bottom: 16px;
  background: var(--m-paper);
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-lg);
}
.avatar {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: var(--m-r);
  background: var(--m-accent);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
}
.shortcut-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
.shortcut {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-sm);
  transition: border-color 0.2s var(--m-ease), transform 0.2s var(--m-ease);
}
.shortcut:hover {
  border-color: var(--m-ink);
  transform: translateY(-2px);
}
.shortcut-ico {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: var(--m-r-xs);
  background: var(--m-bone-deep);
  color: var(--m-ink-2);
}
.shortcut-arrow {
  margin-left: auto;
  color: var(--m-faint);
}
</style>
