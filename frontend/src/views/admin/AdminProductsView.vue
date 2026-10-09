<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { mallAdminApi } from '@/api/mall'
import { money } from '@/utils/format'
import { priceRange } from '@/utils/mall-format'
import { ok } from '@/lib/toast'

const router = useRouter()

const filters = reactive({ name: '', categoryId: '', brandId: '', status: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)
const categories = ref([])
const brands = ref([])

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

function priceText(row) {
  const p = priceRange(row)
  if (p.low === null) return '—'
  return p.range ? `${money(p.low)} – ${money(p.high)}` : money(p.low)
}

async function load() {
  loading.value = true
  try {
    const data = await mallAdminApi.products({
      name: filters.name || undefined,
      categoryId: filters.categoryId || undefined,
      brandId: filters.brandId || undefined,
      status: filters.status === '' ? undefined : filters.status,
      page: page.value,
      pageSize
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function loadFacets() {
  const [c, b] = await Promise.all([
    mallAdminApi.categoriesTree().catch(() => []),
    mallAdminApi.brands().catch(() => [])
  ])
  categories.value = flatCategories(c)
  brands.value = Array.isArray(b) ? b : b?.records || []
}

async function toggleStatus(row) {
  const next = Number(row.status) === 1 ? 0 : 1
  await mallAdminApi.productStatus(row.id, next)
  ok(next === 1 ? '已上架' : '已下架')
  load()
}

async function remove(row) {
  await mallAdminApi.removeProduct(row.id)
  ok('商品已删除')
  load()
}

function search() {
  page.value = 1
  load()
}

function reset() {
  Object.assign(filters, { name: '', categoryId: '', brandId: '', status: '' })
  search()
}

onMounted(async () => {
  await loadFacets()
  load()
})
</script>

<template>
  <WorkbenchLayout>
    <template #actions>
      <RouterLink to="/admin/products/new" class="m-btn m-btn-primary">
        + 新增商品
      </RouterLink>
    </template>

    <div class="m-panel m-mb-4">
      <div class="filter-row">
        <input v-model="filters.name" class="m-input" placeholder="商品名称" @keyup.enter="search" />
        <select v-model="filters.categoryId" class="m-select">
          <option value="">全部分类</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">
            {{ '　'.repeat(c.depth) }}{{ c.name }}
          </option>
        </select>
        <select v-model="filters.brandId" class="m-select">
          <option value="">全部品牌</option>
          <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option>
        </select>
        <select v-model="filters.status" class="m-select">
          <option value="">全部状态</option>
          <option :value="1">已上架</option>
          <option :value="0">已下架</option>
        </select>
        <button class="m-btn m-btn-primary" type="button" @click="search">查询</button>
        <button class="m-btn m-btn-soft" type="button" @click="reset">重置</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 80px">ID</th>
            <th>商品</th>
            <th>分类 / 品牌</th>
            <th>价格</th>
            <th>库存</th>
            <th>销量</th>
            <th>状态</th>
            <th style="width: 220px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td class="m-num">{{ row.id }}</td>
            <td>
              <div class="m-cell">
                <img v-if="row.image || row.images?.[0]" class="m-thumb-sm" :src="row.image || row.images[0]" :alt="row.name" />
                <span v-else class="m-thumb-sm" />
                <div>
                  <p class="m-h4">{{ row.name }}</p>
                  <p class="m-dim">{{ row.subtitle || '' }}</p>
                </div>
              </div>
            </td>
            <td>
              <p style="font-size: 13px">{{ row.categoryName || '—' }}</p>
              <p class="m-dim">{{ row.brandName || '无品牌' }}</p>
            </td>
            <td class="m-price">{{ priceText(row) }}</td>
            <td class="m-num">{{ row.stock ?? 0 }}</td>
            <td class="m-num">{{ row.sales ?? 0 }}</td>
            <td>
              <MStatus
                :text="Number(row.status) === 1 ? '已上架' : '已下架'"
                :tone="Number(row.status) === 1 ? 'emerald' : 'gray'"
              />
            </td>
            <td>
              <div class="m-cell-actions">
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="router.push(`/admin/products/${row.id}/edit`)">
                  编辑
                </button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="toggleStatus(row)">
                  {{ Number(row.status) === 1 ? '下架' : '上架' }}
                </button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="remove(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="8" class="m-table-empty">暂无商品，点右上角「新增商品」开始添加。</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
  </WorkbenchLayout>
</template>

<style scoped>
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.filter-row .m-input,
.filter-row .m-select {
  width: auto;
  min-width: 160px;
}
</style>
