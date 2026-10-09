<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Icon from '@/components/Icon.vue'
import ShopLayout from '@/layouts/ShopLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallAddressApi, mallCatalogApi } from '@/api/mall'
import { ok } from '@/lib/toast'

const list = ref([])
const loading = ref(true)
const open = ref(false)
const submitting = ref(false)
const provinces = ref([])
const cities = ref([])
const districts = ref([])

const form = reactive({
  id: null,
  consignee: '',
  phone: '',
  provinceCode: '',
  cityCode: '',
  areaCode: '',
  detail: '',
  label: '家',
  isDefault: 1,
  province: '',
  city: '',
  district: ''
})

async function load() {
  loading.value = true
  try {
    const data = await mallAddressApi.list()
    list.value = Array.isArray(data) ? data : data?.records || []
  } finally {
    loading.value = false
  }
}

async function loadProvinces() {
  const data = await mallCatalogApi.areas()
  provinces.value = Array.isArray(data) ? data : []
}

async function onProvince() {
  const p = provinces.value.find((x) => String(x.code) === String(form.provinceCode))
  form.province = p?.name || ''
  form.cityCode = ''
  form.areaCode = ''
  cities.value = form.provinceCode ? await mallCatalogApi.areas(form.provinceCode) : []
  districts.value = []
}

async function onCity() {
  const c = cities.value.find((x) => String(x.code) === String(form.cityCode))
  form.city = c?.name || ''
  form.areaCode = ''
  districts.value = form.cityCode ? await mallCatalogApi.areas(form.cityCode) : []
}

function onDistrict() {
  const d = districts.value.find((x) => String(x.code) === String(form.areaCode))
  form.district = d?.name || ''
}

function reset() {
  Object.assign(form, {
    id: null,
    consignee: '',
    phone: '',
    provinceCode: '',
    cityCode: '',
    areaCode: '',
    detail: '',
    label: '家',
    isDefault: list.value.length ? 0 : 1,
    province: '',
    city: '',
    district: ''
  })
  cities.value = []
  districts.value = []
}

async function openCreate() {
  reset()
  if (!provinces.value.length) await loadProvinces()
  open.value = true
}

async function openEdit(a) {
  if (!provinces.value.length) await loadProvinces()
  Object.assign(form, {
    id: a.id,
    consignee: a.consignee,
    phone: a.phone,
    areaCode: a.areaCode || '',
    detail: a.detail,
    label: a.label || '家',
    isDefault: Number(a.isDefault) || 0,
    province: a.province || '',
    city: a.city || '',
    district: a.district || '',
    provinceCode: a.provinceCode || '',
    cityCode: a.cityCode || ''
  })
  if (form.provinceCode) cities.value = await mallCatalogApi.areas(form.provinceCode)
  if (form.cityCode) districts.value = await mallCatalogApi.areas(form.cityCode)
  open.value = true
}

async function save() {
  if (!form.consignee || !form.phone || !form.areaCode || !form.detail) {
    ok('请补全收货人、电话、所在地区与详细地址')
    return
  }
  submitting.value = true
  try {
    // 省市区只传末级 areaCode，服务端回填省市区名称
    const body = {
      consignee: form.consignee,
      phone: form.phone,
      areaCode: form.areaCode,
      detail: form.detail,
      label: form.label,
      isDefault: Number(form.isDefault)
    }
    if (form.id) await mallAddressApi.update({ ...body, id: form.id })
    else await mallAddressApi.create(body)
    open.value = false
    ok('地址已保存')
    load()
  } finally {
    submitting.value = false
  }
}

async function setDefault(a) {
  await mallAddressApi.setDefault(a.id)
  ok('已设为默认地址')
  load()
}

async function remove(a) {
  await mallAddressApi.remove(a.id)
  ok('地址已删除')
  load()
}

onMounted(load)
</script>

<template>
  <ShopLayout>
    <div class="m-wrap m-wrap-mid m-section-sm">
      <div class="m-between m-wrapflex m-mb-5">
        <div>
          <span class="m-eyebrow">Addresses</span>
          <h1 class="m-h1 m-mt-2">收货地址</h1>
          <p class="m-muted m-mt-1" style="font-size: 13px">最多可保存 20 条地址</p>
        </div>
        <button class="m-btn m-btn-primary" type="button" @click="openCreate">
          <Icon name="plus" :size="16" />
          新增地址
        </button>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 260px" />

      <div v-else-if="list.length" class="m-col" style="gap: 14px">
        <div v-for="a in list" :key="a.id" class="m-panel">
          <div class="m-between m-wrapflex" style="gap: 14px">
            <div class="m-row m-wrapflex" style="gap: 10px">
              <span class="m-h4">{{ a.consignee }}</span>
              <span class="m-muted m-num" style="font-size: 13px">{{ a.phone }}</span>
              <span v-if="Number(a.isDefault) === 1" class="m-badge m-badge-accent">默认</span>
              <span v-if="a.label" class="m-badge m-badge-soft">{{ a.label }}</span>
            </div>
            <div class="m-row m-wrapflex" style="gap: 8px">
              <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openEdit(a)">编辑</button>
              <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="setDefault(a)">设为默认</button>
              <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="remove(a)">删除</button>
            </div>
          </div>
          <p class="m-muted m-mt-3" style="font-size: 14px">
            {{ a.province || '' }}{{ a.city || '' }}{{ a.district || '' }}{{ a.detail }}
          </p>
        </div>
        <p class="m-dim">第一条地址会自动设为默认地址。</p>
      </div>

      <MEmpty image="empty-address" v-else icon="pin" title="还没有收货地址" desc="下单前需要先添加一个收货地址">
        <button class="m-btn m-btn-primary m-mt-2" type="button" @click="openCreate">新增地址</button>
      </MEmpty>

      <div class="m-mt-5">
        <RouterLink to="/shop/me" class="m-btn-link">返回个人中心</RouterLink>
      </div>
    </div>

    <MModal :open="open" :title="form.id ? '编辑地址' : '新增地址'" @close="open = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">收货人</label>
            <input v-model="form.consignee" class="m-input" placeholder="张三" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">手机号</label>
            <input v-model="form.phone" class="m-input" placeholder="13800138000" />
          </div>
        </div>

        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">省</label>
            <select v-model="form.provinceCode" class="m-select" @change="onProvince">
              <option value="">请选择</option>
              <option v-for="p in provinces" :key="p.code" :value="p.code">{{ p.name }}</option>
            </select>
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">市</label>
            <select v-model="form.cityCode" class="m-select" :disabled="!cities.length" @change="onCity">
              <option value="">请选择</option>
              <option v-for="c in cities" :key="c.code" :value="c.code">{{ c.name }}</option>
            </select>
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">区 / 县</label>
            <select v-model="form.areaCode" class="m-select" :disabled="!districts.length" @change="onDistrict">
              <option value="">请选择</option>
              <option v-for="d in districts" :key="d.code" :value="d.code">{{ d.name }}</option>
            </select>
          </div>
        </div>

        <div class="m-field">
          <label class="m-eyebrow">详细地址</label>
          <input v-model="form.detail" class="m-input" placeholder="东华门大街 1 号" />
        </div>

        <div class="m-row m-wrapflex" style="gap: 16px; align-items: flex-end">
          <div class="m-field m-grow">
            <label class="m-eyebrow">标签</label>
            <div class="m-row" style="gap: 8px">
              <button
                v-for="l in ['家', '公司', '学校']"
                :key="l"
                type="button"
                class="m-chip"
                :class="{ 'm-chip-active': form.label === l }"
                @click="form.label = l"
              >
                {{ l }}
              </button>
            </div>
          </div>
          <label class="m-check" style="padding-bottom: 10px">
            <input v-model="form.isDefault" type="checkbox" :true-value="1" :false-value="0" />
            <span>设为默认</span>
          </label>
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="open = false">取消</button>
        <button class="m-btn m-btn-primary" type="button" :disabled="submitting" @click="save">
          {{ submitting ? '保存中…' : '保存地址' }}
        </button>
      </template>
    </MModal>
  </ShopLayout>
</template>
