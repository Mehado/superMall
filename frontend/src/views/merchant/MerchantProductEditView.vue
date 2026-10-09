<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MUploader from '@/components/mall/MUploader.vue'
import { merchantApi } from '@/api/delivery'
import { BUSINESS_TYPE } from '@/utils/format'
import { ok } from '@/lib/toast'

const props = defineProps({ id: { type: [String, Number], default: '' } })
const route = useRoute()
const router = useRouter()

const productId = computed(() => props.id || route.params.id || 'new')
const isNew = computed(() => String(productId.value) === 'new')

const categories = ref([])
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  id: null,
  categoryId: '',
  name: '',
  description: '',
  price: 0,
  stock: 0,
  status: 1,
  image: '',
  skus: []
})

async function load() {
  loading.value = true
  try {
    const c = await merchantApi.categories().catch(() => [])
    categories.value = Array.isArray(c) ? c : c?.records || []

    if (!isNew.value) {
      const [p, skus] = await Promise.all([
        merchantApi.product(productId.value),
        merchantApi.skusOfProduct(productId.value).catch(() => [])
      ])
      Object.assign(form, {
        id: p.id,
        categoryId: p.categoryId ?? '',
        name: p.name || '',
        description: p.description || '',
        price: p.price ?? 0,
        stock: p.stock ?? 0,
        status: p.status ?? 1,
        image: p.image || '',
        skus: (Array.isArray(skus) ? skus : skus?.records || []).map((s) => ({
          id: s.id,
          name: s.name || '',
          price: s.price ?? 0,
          stock: s.stock ?? 0,
          specValues: s.specValues || []
        }))
      })
    }
  } finally {
    loading.value = false
  }
}

async function upload(file) {
  const res = await merchantApi.upload(file)
  return typeof res === 'string' ? res : res?.url || res?.data || ''
}

function addSku() {
  form.skus.push({ id: null, name: '', price: form.price, stock: 0, specValues: [] })
}

async function save() {
  if (!form.name || !form.categoryId) {
    ok('请填写商品名称并选择分类')
    return
  }
  saving.value = true
  try {
    const body = {
      categoryId: Number(form.categoryId),
      name: form.name,
      description: form.description,
      price: Number(form.price) || 0,
      stock: Number(form.stock) || 0,
      status: Number(form.status),
      image: form.image,
      skuList: form.skus.map((s) => ({
        id: s.id || undefined,
        name: s.name,
        price: Number(s.price) || 0,
        stock: Number(s.stock) || 0,
        specValues: s.specValues || []
      }))
    }
    if (isNew.value) await merchantApi.createProduct(body)
    else await merchantApi.updateProduct({ ...body, id: form.id })
    ok(isNew.value ? '商品已创建' : '商品已保存')
    router.push('/merchant/products')
  } finally {
    saving.value = false
  }
}

watch(productId, load)
onMounted(load)
</script>

<template>
  <WorkbenchLayout section="merchant" :title="isNew ? '新增商品' : `编辑商品 #${productId}`">
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="router.push('/merchant/products')">返回列表</button>
      <button class="m-btn m-btn-primary" type="button" :disabled="saving" @click="save">
        {{ saving ? '保存中…' : '保存商品' }}
      </button>
    </template>

    <div v-if="loading" class="m-skeleton" style="height: 440px" />

    <div v-else class="m-col" style="gap: 16px">
      <div class="m-panel">
        <h2 class="m-h3 m-mb-4">基础信息</h2>
        <div class="form-grid">
          <div class="m-field">
            <label class="m-eyebrow">商品名称 *</label>
            <input v-model="form.name" class="m-input" placeholder="红烧牛肉面" />
          </div>
          <div class="m-field">
            <label class="m-eyebrow">分类 *</label>
            <select v-model="form.categoryId" class="m-select">
              <option value="">请选择分类</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">
                {{ c.name }}
                <template v-if="c.businessType"> · {{ BUSINESS_TYPE[c.businessType] }}</template>
              </option>
            </select>
          </div>
          <div class="m-field">
            <label class="m-eyebrow">价格</label>
            <input v-model.number="form.price" type="number" step="0.01" class="m-input" />
          </div>
          <div class="m-field">
            <label class="m-eyebrow">库存</label>
            <input v-model.number="form.stock" type="number" class="m-input" />
          </div>
          <div class="m-field">
            <label class="m-eyebrow">状态</label>
            <select v-model.number="form.status" class="m-select">
              <option :value="1">上架</option>
              <option :value="0">下架</option>
            </select>
          </div>
        </div>

        <div class="m-field m-mt-4">
          <label class="m-eyebrow">商品描述</label>
          <textarea v-model="form.description" class="m-textarea" />
        </div>

        <div class="m-field m-mt-4">
          <span class="m-eyebrow">商品图片</span>
          <MUploader
            :model-value="form.image ? [form.image] : []"
            :uploader="upload"
            :max="1"
            @update:model-value="(v) => (form.image = v[0] || '')"
          />
          <p class="m-dim">建议上传方形图片，展示效果更好。</p>
        </div>
      </div>

      <div class="m-panel">
        <div class="m-between m-wrapflex m-mb-4">
          <div>
            <h2 class="m-h3">规格与库存</h2>
            <p class="m-dim m-mt-1">有规格的商品，买家下单时会选择对应规格</p>
          </div>
          <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="addSku">+ 添加规格</button>
        </div>

        <div class="m-table-wrap">
          <table class="m-table" style="min-width: 620px">
            <thead>
              <tr>
                <th>规格名</th>
                <th style="width: 170px">价格</th>
                <th style="width: 150px">库存</th>
                <th style="width: 110px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in form.skus" :key="i">
                <td><input v-model="s.name" class="m-input is-compact" placeholder="小份 / 大份" /></td>
                <td><input v-model.number="s.price" type="number" step="0.01" class="m-input is-compact" /></td>
                <td><input v-model.number="s.stock" type="number" class="m-input is-compact" /></td>
                <td>
                  <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="form.skus.splice(i, 1)">删除</button>
                </td>
              </tr>
              <tr v-if="!form.skus.length">
                <td colspan="4" class="m-table-empty">无规格商品：不添加规格即可</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </WorkbenchLayout>
</template>

<style scoped>
.form-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}
</style>
