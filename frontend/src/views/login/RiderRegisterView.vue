<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AuthShell from '@/components/AuthShell.vue'
import CaptchaField from '@/components/CaptchaField.vue'
import PasswordMeter from '@/components/PasswordMeter.vue'
import { foodAuthApi } from '@/api/delivery'
import { useAuthStore } from '@/stores/auth'
import { ROLE } from '@/utils/request'
import { ok } from '@/lib/toast'

/**
 * 骑手注册（公开页）。
 * 骑手是平台级角色，注册即启用、可直接接单；平台运营可在后台停用异常账号。
 */
const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  confirm: '',
  name: '',
  phone: '',
  captchaCode: '',
  captchaKey: ''
})
const captchaRef = ref(null)
const submitting = ref(false)

const ready = computed(
  () =>
    form.username.includes('@') &&
    form.password.length >= 8 &&
    form.password === form.confirm &&
    form.name.trim() &&
    /^1[3-9][0-9]{9}$/.test(form.phone)
)

async function submit() {
  if (submitting.value || !ready.value) return
  if (!form.captchaCode || !form.captchaKey) {
    ok('请输入图形验证码')
    return
  }
  submitting.value = true
  try {
    const data = await foodAuthApi.riderRegister({
      username: form.username.trim(),
      password: form.password,
      name: form.name.trim(),
      phone: form.phone,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    })
    const token = data?.token
    if (token) {
      // 注册即登录：注册接口已经返回 token，不要再调登录
      auth.saveSession(ROLE.rider, token, { id: data.id, username: data.username, name: data.name })
      ok('注册成功，已自动登录')
      router.replace('/rider')
    } else {
      ok('注册成功，请登录')
      router.replace({ path: '/login', query: { role: 'rider' } })
    }
  } catch {
    form.captchaCode = ''
    form.captchaKey = ''
    captchaRef.value?.refresh()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthShell>
    <template #hint>注册成为骑手，去抢单池接单、取餐、送达。</template>

    <span class="m-eyebrow">Rider sign up</span>
    <h2 class="m-h2 card-title">骑手注册</h2>
    <p class="m-muted card-desc">注册成功后自动登录，可直接去抢单池接单。</p>

    <form class="m-col card-form" @submit.prevent="submit" @keydown.enter.prevent="submit">
      <div class="m-field">
        <label class="m-eyebrow">姓名 / 昵称</label>
        <input v-model="form.name" class="m-input" placeholder="例如：疾风" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">手机号</label>
        <input v-model="form.phone" class="m-input" placeholder="13700000000" maxlength="11" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">邮箱账号</label>
        <input v-model="form.username" class="m-input" placeholder="rider3@gmail.com" autocomplete="username" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">密码</label>
        <input
          v-model="form.password"
          class="m-input"
          type="password"
          placeholder="8-32 位，含大写 + 小写 + 数字"
          autocomplete="new-password"
        />
        <PasswordMeter :value="form.password" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">确认密码</label>
        <input v-model="form.confirm" class="m-input" type="password" placeholder="再输入一次" autocomplete="new-password" />
        <p v-if="form.confirm && form.confirm !== form.password" class="m-dim" style="color: var(--m-danger)">
          两次输入不一致
        </p>
      </div>

      <CaptchaField
        ref="captchaRef"
        v-model="form.captchaCode"
        v-model:captcha-key="form.captchaKey"
        :loader="foodAuthApi.captcha"
      />

      <button class="m-btn m-btn-primary m-btn-lg m-btn-block" type="submit" :disabled="!ready || submitting">
        {{ submitting ? '提交中…' : '注册并登录' }}
      </button>
    </form>

    <div class="card-foot">
      <RouterLink to="/login?role=rider" class="m-btn-link">已有账号？去登录</RouterLink>
      <RouterLink to="/" class="m-btn-link">返回主页面</RouterLink>
    </div>
  </AuthShell>
</template>

<style scoped>
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
