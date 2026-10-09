<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MUploader from '@/components/mall/MUploader.vue'
import { mallAdminApi } from '@/api/mall'
import { ok } from '@/lib/toast'

const props = defineProps({ id: { type: [String, Number], default: '' } })
const route = useRoute()
const router = useRouter()

const productId = computed(() => props.id || route.params.id || 'new')
const isNew = computed(() => String(productId.value) === 'new')

const categories = ref([])
const brands = ref([])
const saving = ref(false)
const loading = ref(true)
const specText = ref('')

const form = reactive({
  id: null,
  categoryId: '',
  brandId: '',
  name: '',
  subtitle: '',
  detail: '',
  images: [],
  specs: [],
  skus: []
})

function flatCategories(tree) {
  const out = []
  const walk = (arr, depth = 0) => {
    ;(arr || []).forEach((c) => {
      out.push({ ...c, depth })
      if (c.children?.length) walk(c.children, depth + 1)
    })
  }
  walk(tree)
  return out
}

const expectedSkuCount = computed(() =>
  form.specs.length ? form.specs.reduce((n, s) => n * Math.max(1, s.values.length), 1) : 1
)

function combos(specs) {
  return specs.reduce(
    (acc, spec) => acc.flatMap((row) => spec.values.map((v) => [...row, v])),
    [[]]
  )
}

/** SKU 数必须等于各规格值数量的乘积，否则后端会报错 */
function generateSkus() {
  const specs = form.specs.filter((s) => s.name && s.values.length)
  if (!specs.length) {
    if (!form.skus.length) {
      form.skus = [{ skuCode: '', price: 0, stock: 0, specValues: [] }]
    } else {
      form.skus = [form.skus[0]]
    }
    ok('已按无规格商品处理')
    return
  }
  const list = combos(specs)
  const next = list.map((values) => {
    const existed = form.skus.find(
      (s) => s.specValues?.length === values.length && s.specValues.every((v, i) => v === values[i])
    )
    return {
      skuCode: existed?.skuCode || '',
      price: existed?.price ?? form.skus[0]?.price ?? 0,
      stock: existed?.stock ?? 0,
      specValues: values
    }
  })
  form.skus = next
  ok(`已生成 ${next.length} 个 SKU`)
}

function addSpec() {
  form.specs.push({ name: '', values: [] })
  syncSpecText()
}

function removeSpec(i) {
  form.specs.splice(i, 1)
  syncSpecText()
  generateSkus()
}

function syncSpecText() {
  specText.value = form.specs.map((s) => s.values.join(',')).join('\n')
}

function applySpecValues(i, text) {
  form.specs[i].values = String(text)
    .split(/[,，\s]+/)
    .map((v) => v.trim())
    .filter(Boolean)
}

async function load() {
  loading.value = true
  try {
    const [c, b] = await Promise.all([
      mallAdminApi.categoriesTree().catch(() => []),
      mallAdminApi.brands().catch(() => [])
    ])
    categories.value = flatCategories(c)
    brands.value = Array.isArray(b) ? b : b?.records || []

    if (!isNew.value) {
      const p = await mallAdminApi.product(productId.value)
      Object.assign(form, {
        id: p.id,
        categoryId: p.categoryId ?? '',
        brandId: p.brandId ?? '',
        name: p.name || '',
        subtitle: p.subtitle || '',
        detail: p.detail || '',
        images: p.images || (p.image ? [p.image] : []),
        specs: (p.specs || []).map((s) => ({ name: s.name, values: s.values || [] })),
        skus: (p.skus || []).map((s) => ({
          skuCode: s.skuCode || '',
          price: s.price ?? 0,
          stock: s.stock ?? 0,
          specValues: s.specValues || []
        }))
      })
      syncSpecText()
    } else {
      form.skus = [{ skuCode: '', price: 0, stock: 0, specValues: [] }]
    }
  } finally {
    loading.value = false
  }
}

async function upload(file) {
  const res = await mallAdminApi.upload(file)
  return typeof res === 'string' ? res : res?.url || res?.data || ''
}

async function save() {
  if (!form.categoryId || !form.name) {
    ok('请填写商品名称并选择分类')
    return
  }
  if (form.skus.length !== expectedSkuCount.value) {
    ok(`请先生成规格组合：当前需要 ${expectedSkuCount.value} 条规格库存，已有 ${form.skus.length} 条`)
    return
  }
  saving.value = true
  try {
    // 保存是整体覆盖：想清空字段要传空字符串（传 null 会被当成不改）
    const body = {
      categoryId: Number(form.categoryId),
      brandId: form.brandId === '' ? '' : Number(form.brandId),
      name: form.name,
      subtitle: form.subtitle || '',
      detail: form.detail || '',
      images: form.images,
      specs: form.specs
        .filter((s) => s.name && s.values.length)
        .map((s) => ({ name: s.name, values: s.values })),
      skus: form.skus.map((s) => ({
        skuCode: s.skuCode || '',
        price: Number(s.price) || 0,
        stock: Number(s.stock) || 0,
        specValues: s.specValues
      }))
    }
    if (isNew.value) await mallAdminApi.createProduct(body)
    else await mallAdminApi.updateProduct({ ...body, id: form.id })
    ok(isNew.value ? '商品已创建' : '商品已保存')
    router.push('/admin/products')
  } finally {
    saving.value = false
  }
}

watch(productId, load)
onMounted(load)
</script>

<template>
  <WorkbenchLayout :title="isNew ? '新增商品' : `编辑商品 #${productId}`">
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="router.push('/admin/products')">返回列表</button>
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
            <input v-model="form.name" class="m-input" placeholder="纯棉圆领T恤" />
          </div>
          <div class="m-field">
            <label class="m-eyebrow">副标题</label>
            <input v-model="form.subtitle" class="m-input" placeholder="260g 重磅纯棉" />
          </div>
          <div class="m-field">
            <label class="m-eyebrow">分类 *</label>
            <select v-model="form.categoryId" class="m-select">
              <option value="">请选择分类</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">
                {{ '　'.repeat(c.depth) }}{{ c.name }}
              </option>
            </select>
          </div>
          <div class="m-field">
            <label class="m-eyebrow">品牌</label>
            <select v-model="form.brandId" class="m-select">
              <option value="">无品牌</option>
              <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option>
            </select>
          </div>
        </div>

        <div class="m-field m-mt-4">
          <label class="m-eyebrow">商品详情</label>
          <textarea v-model="form.detail" class="m-textarea" placeholder="100% 精梳棉…" />
        </div>

        <div class="m-field m-mt-4">
          <span class="m-eyebrow">商品图片（第一张作为主图）</span>
          <MUploader v-model="form.images" :uploader="upload" :max="10" hint="最多 10 张，第一张作为商品主图" />
        </div>
      </div>

      <div class="m-panel">
        <div class="m-between m-wrapflex m-mb-4">
          <h2 class="m-h3">规格</h2>
          <div class="m-row m-wrapflex" style="gap: 8px">
            <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="addSpec">+ 添加规格项</button>
            <button class="m-btn m-btn-primary m-btn-sm" type="button" @click="generateSkus">生成 SKU 组合</button>
          </div>
        </div>

        <div v-if="form.specs.length" class="m-col" style="gap: 12px">
          <div v-for="(spec, i) in form.specs" :key="i" class="m-soft" style="padding: 14px 16px">
            <div class="m-row m-wrapflex" style="gap: 10px">
              <input v-model="spec.name" class="m-input" placeholder="规格名，如：颜色" style="max-width: 180px" />
              <input
                class="m-input m-grow"
                placeholder="规格值，用逗号分隔：白,黑"
                :value="spec.values.join(',')"
                @input="applySpecValues(i, $event.target.value)"
              />
              <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="removeSpec(i)">删除</button>
            </div>
            <p v-if="spec.values.length" class="m-dim m-mt-2">当前值：{{ spec.values.join(' / ') }}</p>
          </div>
        </div>
        <p v-else class="m-dim">无规格商品：不添加规格项即可，买家直接按商品下单。</p>
      </div>

      <div class="m-panel">
        <div class="m-between m-wrapflex m-mb-4">
          <h2 class="m-h3">规格库存（{{ form.skus.length }} / {{ expectedSkuCount }}）</h2>
          <span class="m-dim">每个规格组合单独设置价格与库存</span>
        </div>

        <div class="m-table-wrap">
          <table class="m-table" style="min-width: 640px">
            <thead>
              <tr>
                <th>规格组合</th>
                <th style="width: 200px">SKU 编码</th>
                <th style="width: 150px">价格</th>
                <th style="width: 150px">库存</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(sku, i) in form.skus" :key="i">
                <td>
                  <span v-if="sku.specValues.length" class="m-row m-wrapflex" style="gap: 6px">
                    <span v-for="v in sku.specValues" :key="v" class="m-badge m-badge-soft">{{ v }}</span>
                  </span>
                  <span v-else class="m-dim">默认规格（无规格商品）</span>
                </td>
                <td><input v-model="sku.skuCode" class="m-input is-compact" placeholder="TS-W-M" /></td>
                <td><input v-model.number="sku.price" type="number" min="0" step="0.01" class="m-input is-compact" /></td>
                <td><input v-model.number="sku.stock" type="number" min="0" class="m-input is-compact" /></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="m-row m-wrapflex m-mt-3" style="gap: 8px">
          <button
            class="m-btn m-btn-soft m-btn-sm"
            type="button"
            @click="form.skus.push({ skuCode: '', price: form.skus[0]?.price || 0, stock: 0, specValues: [] })"
          >
            + 手动加一行
          </button>
          <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="generateSkus">按规格重新生成</button>
        </div>
      </div>

      <div class="m-panel">
        <h2 class="m-h3 m-mb-3">填写提示</h2>
        <ul class="m-dim m-col" style="gap: 6px">
          <li>· 改完规格记得点「生成 SKU 组合」，确保每个组合都有价格与库存</li>
          <li>· 图片第一张会作为商品主图</li>
          <li>· 保存后可在商品列表里上架或下架</li>
        </ul>
        <button class="m-btn m-btn-primary m-mt-4" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : isNew ? '创建商品' : '保存修改' }}
        </button>
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
