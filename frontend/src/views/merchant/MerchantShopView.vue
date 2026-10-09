<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MUploader from '@/components/mall/MUploader.vue'
import { merchantApi } from '@/api/delivery'
import { ok } from '@/lib/toast'

/**
 * 店铺设置。
 *
 * 可改字段严格对齐后端 `MerchantUpdateDTO`：**店铺名称 / 门头图 / 简介 / 电话 / 地址**。
 * （旧版页面还摆了「业态 / 配送费 / 起送价 / 营业时间」，但 `merchant` 表并没有这些列，
 *   保存是静默无效的，已去掉，避免误导。）
 *
 * 营业状态是另一个开关：`GET/PUT /api/merchant/shop/business`，
 * 请求体字段是 **`businessStatus`**（不是 `status`），返回的是数字 1/0。
 */
const loading = ref(true)
const saving = ref(false)
/** 商家自助营业状态：接口返回数字 1 营业中 / 0 休息中 */
const businessStatus = ref(1)

const form = reactive({
  shopName: '',
  description: '',
  address: '',
  phone: '',
  logo: ''
})

const isOpen = computed(() => Number(businessStatus.value ?? 1) !== 0)

async function load() {
  loading.value = true
  try {
    const [shop, b] = await Promise.all([merchantApi.shop(), merchantApi.business().catch(() => 1)])
    const status = typeof b === 'object' && b !== null ? b.status ?? b.businessStatus : b
    businessStatus.value = Number(status ?? 1)
    Object.assign(form, {
      shopName: shop?.shopName || '',
      description: shop?.description || '',
      address: shop?.address || '',
      phone: shop?.phone || '',
      logo: shop?.logo || ''
    })
  } finally {
    loading.value = false
  }
}

async function upload(file) {
  const res = await merchantApi.upload(file)
  return typeof res === 'string' ? res : res?.url || res?.data || ''
}

async function save() {
  if (!form.shopName.trim()) {
    ok('店铺名称不能为空')
    return
  }
  saving.value = true
  try {
    // 字段名与后端 DTO 一致：shopName / logo / description / phone / address
    await merchantApi.updateShop({
      shopName: form.shopName.trim(),
      logo: form.logo,
      description: form.description,
      phone: form.phone,
      address: form.address
    })
    ok('店铺信息已保存')
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleBusiness() {
  const next = isOpen.value ? 0 : 1
  // ⚠️ 字段名是 businessStatus
  await merchantApi.updateBusiness({ businessStatus: next })
  ok(next === 1 ? '已开始营业' : '已打烊')
  await load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout section="merchant">
    <template #actions>
      <button
        class="m-btn"
        :class="isOpen ? 'm-btn-outline' : 'm-btn-accent'"
        type="button"
        @click="toggleBusiness"
      >
        {{ isOpen ? '打烊' : '开始营业' }}
      </button>
      <button class="m-btn m-btn-primary" type="button" :disabled="saving" @click="save">保存</button>
    </template>

    <div v-if="loading" class="m-skeleton" style="height: 360px" />

    <div v-else class="m-panel" style="max-width: 760px">
      <div class="m-between m-mb-4">
        <h2 class="m-h3">营业状态</h2>
        <span class="m-row" style="gap: 8px">
          <span class="m-dot" :class="{ 'is-off': !isOpen }" />
          <span class="m-dim">{{ isOpen ? '营业中' : '休息中（用户无法加购/下单）' }}</span>
        </span>
      </div>

      <div class="m-col" style="gap: 16px">
        <div class="m-field">
          <label class="m-eyebrow">店铺名称</label>
          <input v-model="form.shopName" class="m-input" maxlength="100" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">店铺简介</label>
          <textarea v-model="form.description" class="m-textarea" maxlength="500" />
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">联系电话</label>
            <input v-model="form.phone" class="m-input" maxlength="20" />
          </div>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">店铺地址（取餐点）</label>
          <input v-model="form.address" class="m-input" maxlength="255" />
        </div>
        <div class="m-field">
          <span class="m-eyebrow">店铺门头图</span>
          <MUploader
            :model-value="form.logo ? [form.logo] : []"
            :uploader="upload"
            :max="1"
            @update:model-value="(v) => (form.logo = v[0] || '')"
          />
        </div>
      </div>

      <button class="m-btn m-btn-primary m-mt-5" type="button" :disabled="saving" @click="save">保存修改</button>
    </div>
  </WorkbenchLayout>
</template>
