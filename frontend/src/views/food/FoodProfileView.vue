<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import AvatarUploader from '@/components/AvatarUploader.vue'
import FoodLayout from '@/layouts/FoodLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import ProfileEditModal from '@/components/ProfileEditModal.vue'
import PasswordMeter from '@/components/PasswordMeter.vue'
import { foodUserApi } from '@/api/delivery'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { ROLE } from '@/utils/request'
import { shortTime } from '@/utils/format'
import { ok } from '@/lib/toast'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const profile = ref(null)
const addresses = ref([])
const loading = ref(true)

const profileOpen = ref(false)
const passwordOpen = ref(false)
const addressOpen = ref(false)

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
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

const nickname = computed(
  () => profile.value?.nickname || profile.value?.username || auth.profileOf(ROLE.deliveryUser)?.username || '外卖用户'
)
const initial = computed(() => String(nickname.value).slice(0, 1).toUpperCase())

async function load() {
  loading.value = true
  try {
    const [p, a] = await Promise.all([
      foodUserApi.profile().catch(() => null),
      foodUserApi.addresses().catch(() => [])
    ])
    profile.value = p
    addresses.value = Array.isArray(a) ? a : a?.records || []
    if (p) auth.setProfile(ROLE.deliveryUser, p)
  } finally {
    loading.value = false
  }
}

function openProfile() {
  profileOpen.value = true
}

/** 保存资料（弹窗组件负责提示与关闭，这里只落库 + 刷新） */
async function saveProfile(payload) {
  await foodUserApi.updateProfile(payload)
  await load()
}

/** 头像上传完成后的保存（两步：先传 OSS 拿 URL，再写进资料） */
async function saveAvatar(url) {
  await foodUserApi.updateProfile({ avatar: url })
  await load()
}

function openPassword() {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirm: '' })
  passwordOpen.value = true
}

async function savePassword() {
  if (passwordForm.newPassword !== passwordForm.confirm) {
    ok('两次新密码不一致')
    return
  }
  await foodUserApi.updatePassword({
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword
  })
  passwordOpen.value = false
  ok('密码已修改')
}

function openAddress(a = null) {
  Object.assign(addressForm, {
    id: a?.id ?? null,
    consignee: a?.consignee || '',
    phone: a?.phone || '',
    province: a?.province || '',
    city: a?.city || '',
    district: a?.district || '',
    detail: a?.detail || '',
    isDefault: Number(a?.isDefault) || (addresses.value.length ? 0 : 1)
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
  if (addressForm.id) await foodUserApi.updateAddress({ ...body, id: addressForm.id })
  else await foodUserApi.createAddress(body)
  addressOpen.value = false
  ok('地址已保存')
  load()
}

async function setDefault(a) {
  await foodUserApi.setDefaultAddress(a.id)
  ok('已设为默认')
  load()
}

async function removeAddress(a) {
  await foodUserApi.removeAddress(a.id)
  ok('地址已删除')
  load()
}

function logout() {
  auth.logout(ROLE.deliveryUser)
  cart.refreshFood()
  router.push({ path: '/login', query: { role: ROLE.deliveryUser } })
}

onMounted(load)
</script>

<template>
  <FoodLayout>
    <div class="m-wrap m-wrap-narrow m-section-sm">
      <!-- 资料卡 -->
      <div class="profile-head">
        <AvatarUploader
          :src="profile?.avatar"
          :initial="initial"
          :uploader="foodUserApi.uploadAvatar"
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
          <p class="m-stat-val m-mt-2">{{ cart.foodTotal }}</p>
          <p class="m-dim m-mt-1">{{ cart.foodGroups.length }} 家店</p>
        </div>
        <div class="m-stat">
          <p class="m-eyebrow">收货地址</p>
          <p class="m-stat-val m-mt-2">{{ addresses.length }}</p>
          <p class="m-dim m-mt-1">第一条自动默认</p>
        </div>
        <div class="m-stat">
          <p class="m-eyebrow">账号状态</p>
          <p class="m-stat-val m-mt-2" style="font-size: 22px">
            {{ profile?.status === 0 ? '已停用' : '正常' }}
          </p>
          <p class="m-dim m-mt-1">可正常下单</p>
        </div>
      </div>

      <!-- 开店 / 骑手招募（推广位；商家账号与买家账号是两套，需单独注册并由平台审核） -->
      <div class="m-panel m-mb-4 join-panel">
        <div class="m-between m-wrapflex" style="gap: 14px">
          <div>
            <span class="m-eyebrow">Join us</span>
            <h2 class="m-h3" style="margin-top: 6px">也想在平台上做生意？</h2>
            <p class="m-muted" style="font-size: 13px; margin-top: 4px">
              开店需要单独注册商家账号（与当前买家账号不是一个身份），提交后由平台审核；骑手注册即可接单。
            </p>
          </div>
          <div class="m-row m-wrapflex" style="gap: 10px">
            <RouterLink to="/merchant/apply" class="m-btn m-btn-accent">我要开店</RouterLink>
            <RouterLink to="/rider/register" class="m-btn m-btn-outline">成为骑手</RouterLink>
          </div>
        </div>
      </div>

      <!-- 收货地址 -->
      <div class="m-panel m-mb-4">
        <div class="m-between m-mb-4">
          <h2 class="m-h3">收货地址</h2>
          <button class="m-btn m-btn-accent m-btn-sm" type="button" @click="openAddress()">+ 新增地址</button>
        </div>

        <div v-if="loading" class="m-skeleton" style="height: 120px" />
        <div v-else-if="addresses.length" class="m-col" style="gap: 12px">
          <div v-for="a in addresses" :key="a.id" class="m-listrow m-between">
            <div>
              <div class="m-row m-wrapflex" style="gap: 8px">
                <span class="m-h4">{{ a.consignee }}</span>
                <span class="m-muted m-num" style="font-size: 13px">{{ a.phone }}</span>
                <span v-if="Number(a.isDefault) === 1" class="m-badge m-badge-accent">默认</span>
              </div>
              <p class="m-dim m-mt-1">{{ a.province || '' }}{{ a.city || '' }}{{ a.district || '' }}{{ a.detail }}</p>
            </div>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openAddress(a)">编辑</button>
              <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="setDefault(a)">设默认</button>
              <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="removeAddress(a)">删除</button>
            </div>
          </div>
        </div>
        <MEmpty image="empty-address" v-else icon="pin" title="还没有地址" desc="下单前先添加一个收货地址" />
      </div>

      <!-- 账号安全 -->
      <div class="m-panel">
        <h2 class="m-h3 m-mb-4">账号安全</h2>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <button class="m-btn m-btn-outline" type="button" @click="openPassword">修改密码</button>
          <RouterLink to="/food/orders" class="m-btn m-btn-outline">我的订单</RouterLink>
          <RouterLink to="/food/cart" class="m-btn m-btn-outline">我的购物车</RouterLink>
        </div>
      </div>
    </div>

    <!-- 编辑资料（与电商买家共用组件） -->
    <ProfileEditModal
      :open="profileOpen"
      :profile="profile || {}"
      :saver="saveProfile"
      @close="profileOpen = false"
      @saved="load"
    />

    <!-- 修改密码 -->
    <MModal :open="passwordOpen" title="修改密码" @close="passwordOpen = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-field">
          <label class="m-eyebrow">当前密码</label>
          <input v-model="passwordForm.oldPassword" type="password" class="m-input" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">新密码</label>
          <input v-model="passwordForm.newPassword" type="password" class="m-input" />
          <PasswordMeter :value="passwordForm.newPassword" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">确认新密码</label>
          <input v-model="passwordForm.confirm" type="password" class="m-input" />
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="passwordOpen = false">取消</button>
        <button class="m-btn m-btn-accent" type="button" @click="savePassword">确认修改</button>
      </template>
    </MModal>

    <!-- 地址编辑 -->
    <MModal :open="addressOpen" :title="addressForm.id ? '编辑地址' : '新增地址'" @close="addressOpen = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">收货人</label>
            <input v-model="addressForm.consignee" class="m-input" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">手机号</label>
            <input v-model="addressForm.phone" class="m-input" />
          </div>
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">省</label>
            <input v-model="addressForm.province" class="m-input" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">市</label>
            <input v-model="addressForm.city" class="m-input" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">区</label>
            <input v-model="addressForm.district" class="m-input" />
          </div>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">详细地址</label>
          <input v-model="addressForm.detail" class="m-input" />
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
</style>
