<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthShell from '@/components/AuthShell.vue'
import CaptchaField from '@/components/CaptchaField.vue'
import PasswordMeter from '@/components/PasswordMeter.vue'
import { mallAuthApi } from '@/api/mall'
import { foodAuthApi } from '@/api/delivery'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'
import { ok } from '@/lib/toast'

/**
 * 注册页 —— 与登录页共用 components/AuthShell.vue（左字标 / 右玻璃卡片）
 * 这里只注册买家（商城买家 / 外卖用户）；
 * 商家走「入驻申请」(/merchant/apply)，骑手走「骑手注册」(/rider/register)。
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const scope = ref('food') // food | mall
const scopeLocked = ref(true) // 从某一端进入时只展示该端的注册方式
const form = reactive({ username: '', password: '', confirm: '', captchaCode: '', captchaKey: '' })
const captchaRef = ref(null)
const submitting = ref(false)

const isMall = computed(() => scope.value === 'mall')
const loadCaptcha = () => (isMall.value ? mallAuthApi.captcha() : foodAuthApi.captcha())

const ready = computed(
  () => form.username.includes('@') && form.password.length >= 8 && form.password === form.confirm
)

async function submit() {
  if (submitting.value || !ready.value) return
  submitting.value = true
  try {
    const body = {
      username: form.username.trim(),
      password: form.password,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    }
    const data = isMall.value
      ? await mallAuthApi.register(body)
      : await foodAuthApi.userRegister(body)
    const token =
      (typeof data === 'string' ? data : null) || data?.token || data?.accessToken || ''
    const role = isMall.value ? ROLE.mallUser : ROLE.deliveryUser
    if (token) {
      // 注册接口直接返回 token —— 不要再调登录
      auth.saveSession(role, token, data?.userInfo || data?.user || null)
      ok('注册成功，已自动登录')
      router.replace(role === ROLE.mallUser ? '/shop' : '/food')
    } else {
      ok('注册成功，请登录')
      router.replace({ path: '/login', query: { role } })
    }
  } catch {
    captchaRef.value?.refresh()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  scope.value = route.query.scope === 'mall' ? 'mall' : 'food'
  scopeLocked.value = true
})
</script>

<template>
  <AuthShell>
    <template #hint>注册成功后会自动登录，直接开始下单、评价与查看配送进度。</template>

    <div class="scope-chips">
      <button
        v-if="!scopeLocked || scope === 'food'"
        type="button"
        class="m-chip scope-chip"
        :class="{ 'm-chip-active': scope === 'food' }"
        @click="scope = 'food'"
      >
        外卖用户注册
      </button>
      <button
        v-if="!scopeLocked || scope === 'mall'"
        type="button"
        class="m-chip scope-chip"
        :class="{ 'm-chip-active': scope === 'mall' }"
        @click="scope = 'mall'"
      >
        商城买家注册
      </button>
      <button v-if="scopeLocked" type="button" class="m-chip scope-chip" @click="scopeLocked = false">
        注册其它身份
      </button>
    </div>

    <span class="m-eyebrow">Create account</span>
    <h2 class="m-h2 card-title">注册买家账号</h2>
    <p class="m-muted card-desc">注册成功后会自动登录，直接开始使用。</p>

    <form class="m-col card-form" @submit.prevent="submit" @keydown.enter.prevent="submit">
      <div class="m-field">
        <label class="m-eyebrow">邮箱账号</label>
        <input v-model="form.username" class="m-input" placeholder="user3@qq.com" autocomplete="username" />
        <p class="m-dim">请填写常用邮箱，用于登录</p>
      </div>

      <div class="m-field">
        <label class="m-eyebrow">密码</label>
        <input
          v-model="form.password"
          class="m-input"
          type="password"
          placeholder="设置登录密码"
          autocomplete="new-password"
        />
        <PasswordMeter :value="form.password" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">确认密码</label>
        <input
          v-model="form.confirm"
          class="m-input"
          type="password"
          placeholder="再输入一次"
          autocomplete="new-password"
        />
        <p v-if="form.confirm && form.confirm !== form.password" class="m-dim" style="color: var(--m-danger)">
          两次输入不一致
        </p>
      </div>

      <CaptchaField
        ref="captchaRef"
        v-model="form.captchaCode"
        v-model:captcha-key="form.captchaKey"
        :loader="loadCaptcha"
      />

      <button class="m-btn m-btn-primary m-btn-lg m-btn-block" type="submit" :disabled="!ready || submitting">
        {{ submitting ? '提交中…' : '注册并登录' }}
      </button>
    </form>

    <div class="card-foot">
      <RouterLink
        :to="{ path: '/login', query: { role: isMall ? ROLE.mallUser : ROLE.deliveryUser } }"
        class="m-btn-link"
      >
        已有账号？去登录
      </RouterLink>
      <RouterLink v-if="scope === 'food'" to="/merchant/apply" class="m-btn-link">我要开店（商家入驻）</RouterLink>
      <RouterLink to="/" class="m-btn-link">返回主页面</RouterLink>
    </div>
  </AuthShell>
</template>

<style scoped>
.scope-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 22px;
}
.scope-chip {
  padding: 8px 15px;
  font-size: 13px;
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
