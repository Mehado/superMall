<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AuthShell from '@/components/AuthShell.vue'
import CaptchaField from '@/components/CaptchaField.vue'
import PasswordMeter from '@/components/PasswordMeter.vue'
import { foodAuthApi } from '@/api/delivery'
import { ok } from '@/lib/toast'

/**
 * 商家入驻申请（公开页）。
 * 提交后进入「待审核」，平台运营审核通过才能登录商家端；
 * 被驳回可以在这里重新提交，并查看驳回原因。
 */
const form = reactive({
  username: '',
  password: '',
  confirm: '',
  shopName: '',
  contactName: '',
  phone: '',
  address: '',
  description: '',
  licenseImage: '',
  captchaCode: '',
  captchaKey: ''
})
const captchaRef = ref(null)
const submitting = ref(false)
/** 提交后的结果（含审核状态）；非 null 时展示结果卡片 */
const result = ref(null)
/** 「我的申请」查询 */
const queryEmail = ref('')
const querying = ref(false)

const ready = computed(
  () =>
    form.username.includes('@') &&
    form.password.length >= 8 &&
    form.password === form.confirm &&
    form.shopName.trim() &&
    form.contactName.trim() &&
    /^1[3-9][0-9]{9}$/.test(form.phone) &&
    form.address.trim()
)

function resetCaptcha() {
  form.captchaCode = ''
  form.captchaKey = ''
  captchaRef.value?.refresh()
}

async function submit() {
  if (submitting.value || !ready.value) return
  if (!form.captchaCode || !form.captchaKey) {
    ok('请输入图形验证码')
    return
  }
  submitting.value = true
  try {
    const data = await foodAuthApi.merchantApply({
      username: form.username.trim(),
      password: form.password,
      shopName: form.shopName.trim(),
      contactName: form.contactName.trim(),
      phone: form.phone,
      address: form.address.trim(),
      description: form.description || undefined,
      licenseImage: form.licenseImage || undefined,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    })
    result.value = data
    ok(data?.message || '入驻申请已提交')
  } catch {
    resetCaptcha()
  } finally {
    submitting.value = false
  }
}

async function queryStatus() {
  if (!queryEmail.value.includes('@')) {
    ok('请输入提交时使用的邮箱')
    return
  }
  querying.value = true
  try {
    result.value = await foodAuthApi.merchantApplyStatus(queryEmail.value.trim())
  } catch {
    /* 错误提示由拦截器统一 toast */
  } finally {
    querying.value = false
  }
}

function applyAgain() {
  result.value = null
  queryEmail.value = form.username
  resetCaptcha()
}

onMounted(() => {
  queryEmail.value = form.username
})
</script>

<template>
  <AuthShell>
    <template #hint>提交店铺资料，平台审核通过后即可登录商家端接单经营。</template>

    <span class="m-eyebrow">Merchant onboarding</span>
    <h2 class="m-h2 card-title">商家入驻申请</h2>
    <p class="m-muted card-desc">平台运营会审核你的资料，通过后即可用该邮箱登录商家端。</p>

    <!-- 结果卡片：提交后 / 查询后展示 -->
    <div v-if="result" class="result-card">
      <div class="result-head">
        <span class="m-eyebrow">申请状态</span>
        <span
          class="result-badge"
          :class="{
            pending: result.auditStatus === 0,
            approved: result.auditStatus === 1,
            rejected: result.auditStatus === 2
          }"
        >
          {{ result.auditStatusDesc || '—' }}
        </span>
      </div>
      <p class="result-msg">{{ result.message }}</p>
      <p v-if="result.rejectReason" class="result-reason">驳回原因：{{ result.rejectReason }}</p>
      <dl class="m-kv">
        <dt>店铺</dt>
        <dd>{{ result.shopName || '—' }}</dd>
        <dt>账号</dt>
        <dd class="m-num">{{ result.username }}</dd>
      </dl>
      <div class="result-foot">
        <button v-if="result.auditStatus === 2" class="m-btn m-btn-primary m-btn-sm" type="button" @click="applyAgain">
          重新提交申请
        </button>
        <RouterLink v-if="result.auditStatus === 1" to="/login?role=merchant" class="m-btn m-btn-primary m-btn-sm">
          去商家端登录
        </RouterLink>
        <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="result = null">继续填写</button>
      </div>
    </div>

    <form v-else class="m-col card-form" @submit.prevent="submit" @keydown.enter.prevent="submit">
      <div class="m-field">
        <label class="m-eyebrow">店铺名称</label>
        <input v-model="form.shopName" class="m-input" placeholder="例如：川味小馆" />
      </div>

      <div class="m-row form-2">
        <div class="m-field">
          <label class="m-eyebrow">联系人</label>
          <input v-model="form.contactName" class="m-input" placeholder="张三" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">联系电话</label>
          <input v-model="form.phone" class="m-input" placeholder="13900000000" maxlength="11" />
        </div>
      </div>

      <div class="m-field">
        <label class="m-eyebrow">店铺地址（取餐点）</label>
        <input v-model="form.address" class="m-input" placeholder="北京市朝阳区望京街 9 号" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">店铺简介</label>
        <input v-model="form.description" class="m-input" placeholder="选填，一句话介绍你的店" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">营业执照 / 门店照 URL</label>
        <input v-model="form.licenseImage" class="m-input" placeholder="选填，可粘贴图片链接" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">登录邮箱（账号）</label>
        <input v-model="form.username" class="m-input" placeholder="newshop@163.com" autocomplete="username" />
      </div>

      <div class="m-field">
        <label class="m-eyebrow">登录密码</label>
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
        {{ submitting ? '提交中…' : '提交入驻申请' }}
      </button>
    </form>

    <div class="query-box">
      <span class="m-eyebrow">已有申请？查询进度</span>
      <div class="m-row" style="gap: 8px; margin-top: 8px">
        <input v-model="queryEmail" class="m-input" placeholder="提交时的邮箱" />
        <button class="m-btn m-btn-soft" type="button" :disabled="querying" @click="queryStatus">
          {{ querying ? '查询中…' : '查询' }}
        </button>
      </div>
    </div>

    <div class="card-foot">
      <RouterLink to="/login?role=merchant" class="m-btn-link">已有店铺？去登录</RouterLink>
      <RouterLink to="/login?role=platform-admin" class="m-btn-link">平台运营登录</RouterLink>
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
.form-2 {
  gap: 12px;
}
.form-2 > * {
  flex: 1 1 0;
  min-width: 0;
}
.result-card {
  margin-top: 22px;
  padding: 18px;
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-sm);
  background: var(--m-bone);
}
.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.result-badge {
  padding: 4px 12px;
  border-radius: var(--m-pill);
  font-size: 12px;
  font-weight: 700;
}
.result-badge.pending {
  background: rgba(240, 173, 78, 0.16);
  color: #a5670a;
}
.result-badge.approved {
  background: rgba(24, 160, 88, 0.14);
  color: #12703d;
}
.result-badge.rejected {
  background: rgba(214, 69, 69, 0.14);
  color: #a52a2a;
}
.result-msg {
  margin-top: 12px;
  font-size: 13px;
}
.result-reason {
  margin-top: 6px;
  font-size: 13px;
  color: var(--m-danger);
}
.result-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}
.query-box {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--m-line);
}
.card-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  margin-top: 22px;
}
</style>
