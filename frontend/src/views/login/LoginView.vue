<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import AuthShell from '@/components/AuthShell.vue'
import CaptchaField from '@/components/CaptchaField.vue'
import { mallAuthApi } from '@/api/mall'
import { foodAuthApi, foodUserApi, merchantApi, platformApi, riderApi } from '@/api/delivery'
import { useAuthStore } from '@/stores/auth'
import { ROLE, ROLE_META } from '@/utils/request'
import { landingFor } from '@/router'
import { notify, ok } from '@/lib/toast'

/**
 * 登录页 —— 外壳见 components/AuthShell.vue（左字标 / 右玻璃卡片）
 * 五个身份（买家 / 运营 / 外卖用户 / 商家 / 骑手）用角色筹码切换。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const TABS = [
  { role: ROLE.mallUser, label: '商城买家', scope: 'mall', icon: 'cart' },
  { role: ROLE.mallAdmin, label: '商城运营', scope: 'mall', icon: 'chart' },
  { role: ROLE.deliveryUser, label: '外卖用户', scope: 'food', icon: 'truck' },
  { role: ROLE.merchant, label: '外卖商家', scope: 'food', icon: 'store' },
  { role: ROLE.rider, label: '外卖骑手', scope: 'food', icon: 'wallet' },
  { role: ROLE.platformAdmin, label: '平台运营', scope: 'food', icon: 'shield' }
]

const DESC = {
  [ROLE.mallUser]: '逛商品、加购下单、查看评价',
  [ROLE.mallAdmin]: '商品、订单、退款与经营数据',
  [ROLE.deliveryUser]: '附近点餐、查看配送进度',
  [ROLE.merchant]: '接单备餐、维护菜单与营业状态',
  [ROLE.rider]: '抢单、取餐、送达',
  [ROLE.platformAdmin]: '审核商家入驻、管理骑手'
}

const role = ref(ROLE.deliveryUser)
const scope = ref('all') // all | mall | food
const form = reactive({ username: '', password: '', captchaCode: '', captchaKey: '' })
const captchaRef = ref(null)
/** null = 还没问到后端（先不渲染验证码，避免它自己先拉一张、loadPolicy 再拉一张） */
const captchaRequired = ref(null)
const submitting = ref(false)
const policyText = ref('')

const current = computed(() => TABS.find((t) => t.role === role.value))
const isMall = computed(() => role.value.startsWith('mall'))
const visibleTabs = computed(() =>
  scope.value === 'all' ? TABS : TABS.filter((t) => t.scope === scope.value)
)

const loadCaptcha = () => (isMall.value ? mallAuthApi.captcha() : foodAuthApi.captcha())

async function loadPolicy() {
  try {
    const data = isMall.value ? await mallAuthApi.policy() : await foodAuthApi.policy()
    captchaRequired.value = data?.captchaRequired !== false
    const pwd = data?.passwordRule || data?.passwordPolicy
    policyText.value = pwd ? String(pwd) : ''
    // 注意：这里**不**主动 refresh ——
    // 首次挂载时 CaptchaField 变为可见后会自己拉一张（见它的 onMounted）；
    // 这里再拉一张会造成两张并发、谁后到谁生效，用户照图输入就可能 submit 到另一张的 key。
  } catch {
    captchaRequired.value = true
  }
}

async function switchRole(next) {
  role.value = next
  router.replace({ path: '/login', query: { ...route.query, role: next } })
  form.captchaCode = ''
  form.captchaKey = ''
  await loadPolicy()
  // 切换身份后必须换一张：外卖走 /api/auth/captcha、电商走 /api/mall/auth/captcha，
  // key 前缀不同（captcha: vs mall:captcha:），旧的那张在另一端找不到。
  await nextTick()
  if (captchaRequired.value) captchaRef.value?.refresh()
}

function showAllRoles() {
  scope.value = 'all'
}

async function fetchProfile(r) {
  try {
    if (r === ROLE.mallUser) return await mallAuthApi.profile()
    if (r === ROLE.mallAdmin) return await mallAuthApi.adminProfile()
    if (r === ROLE.deliveryUser) return await foodUserApi.profile()
    if (r === ROLE.merchant) return await merchantApi.shop()
    if (r === ROLE.rider) return await riderApi.profile()
  } catch {
    return null
  }
  return null
}

async function submit() {
  // 兜底：Enter 与表单原生 submit 可能同时触发，只放行一次
  if (submitting.value) return
  if (!form.username || !form.password) {
    ok('请输入账号与密码')
    return
  }
  if (captchaRequired.value && (!form.captchaCode || !form.captchaKey)) {
    ok('请输入图形验证码')
    return
  }
  submitting.value = true
  try {
    const body = {
      username: form.username.trim(),
      password: form.password,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    }
    let data
    if (role.value === ROLE.mallUser) data = await mallAuthApi.login(body)
    else if (role.value === ROLE.mallAdmin) data = await mallAuthApi.adminLogin(body)
    else if (role.value === ROLE.deliveryUser) data = await foodAuthApi.userLogin(body)
    else if (role.value === ROLE.merchant) data = await foodAuthApi.merchantLogin(body)
    else if (role.value === ROLE.platformAdmin) data = await platformApi.login(body)
    else data = await foodAuthApi.riderLogin(body)

    const token =
      (typeof data === 'string' ? data : null) ||
      data?.token ||
      data?.accessToken ||
      data?.jwt ||
      ''
    if (!token) throw new Error('登录失败，请稍后重试')

    // 平台运营没有 /profile 接口，登录响应本身就是它的资料
    auth.saveSession(role.value, token, role.value === ROLE.platformAdmin ? data : null)
    const profile = await fetchProfile(role.value)
    if (profile) auth.setProfile(role.value, profile)

    ok(`${ROLE_META[role.value].label}登录成功`)
    // ⚠️ redirect 可能是**另一个身份**的页面（例：在 /shop/me 被拦下来 → 到登录页后又把
    // 身份切成「商城运营」）。这种 redirect 绝不能再跳，否则会被路由守卫按角色拦回登录页，
    // 表现就是「提示登录成功，然后卡住」。规则见 router/index.js 的 landingFor()。
    const { to: landing, dropped, need } = landingFor(role.value, route.query.redirect)
    if (dropped) {
      notify(
        `该页面需要「${ROLE_META[need].label}」身份，已进入「${ROLE_META[role.value].label}」主页`,
        'info',
        3200
      )
    }
    router.replace(landing)
  } catch (e) {
    // 验证码一次性：失败后重新获取一张
    if (captchaRequired.value) captchaRef.value?.refresh()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const asked = String(route.query.role || '')
  const hit = TABS.find((t) => t.role === asked)
  if (hit) {
    role.value = hit.role
    scope.value = hit.scope
  }
  await loadPolicy()
})
</script>

<template>
  <AuthShell>
    <template #hint>选择身份登录，继续未完成的订单与购物车。</template>

    <div v-if="visibleTabs.length > 1" class="role-chips">
      <button
        v-for="t in visibleTabs"
        :key="t.role"
        type="button"
        class="m-chip role-chip"
        :class="{ 'm-chip-active': t.role === role }"
        @click="switchRole(t.role)"
      >
        <Icon :name="t.icon" :size="14" />
        {{ t.label }}
      </button>
    </div>

    <span class="m-eyebrow">Sign in</span>
    <h2 class="m-h2 card-title">{{ current.label }}登录</h2>
    <p class="m-muted card-desc">{{ DESC[current.role] }}</p>

    <!-- Enter 一律当提交：在表单上拦 keydown，避免焦点落在验证码刷新按钮上时 Enter 变成“刷新” -->
    <form class="m-col card-form" @submit.prevent="submit" @keydown.enter.prevent="submit">
      <div class="m-field">
        <label class="m-eyebrow">账号（邮箱）</label>
        <input
          v-model="form.username"
          class="m-input"
          placeholder="请输入邮箱账号"
          autocomplete="username"
        />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">密码</label>
        <input
          v-model="form.password"
          class="m-input"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
        />
        <p v-if="policyText" class="m-dim">{{ policyText }}</p>
      </div>

      <CaptchaField
        v-if="captchaRequired"
        ref="captchaRef"
        v-model="form.captchaCode"
        v-model:captcha-key="form.captchaKey"
        :loader="loadCaptcha"
      />

      <button class="m-btn m-btn-primary m-btn-lg m-btn-block" type="submit" :disabled="submitting">
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>

    <div class="card-foot">
      <RouterLink
        :to="{ path: '/register', query: { scope: isMall ? 'mall' : 'food' } }"
        class="m-btn-link"
      >
        还没有账号？去注册
      </RouterLink>
      <RouterLink v-if="!isMall" to="/merchant/apply" class="m-btn-link">我要开店（商家入驻）</RouterLink>
      <RouterLink v-if="!isMall" to="/rider/register" class="m-btn-link">骑手注册</RouterLink>
      <button v-if="scope !== 'all'" class="m-btn-link" type="button" @click="showAllRoles">
        查看其它身份
      </button>
      <RouterLink to="/" class="m-btn-link">返回主页面</RouterLink>
    </div>
  </AuthShell>
</template>

<style scoped>
.role-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 22px;
}
.role-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 13px;
  font-size: 12px;
}
.card-title {
  margin-top: 6px;
}
.card-desc {
  margin-top: 4px;
  font-size: 13px;
}
.card-form {
  margin-top: 22px;
  gap: 16px;
}
.card-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  margin-top: 22px;
}
</style>
