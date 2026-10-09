<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { merchantApi } from '@/api/delivery'
import { BUSINESS_TYPE, money } from '@/utils/format'
import { ok } from '@/lib/toast'

const router = useRouter()

const filters = reactive({ name: '', categoryId: '', businessType: '', status: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const categories = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const data = await merchantApi.products({
      page: page.value,
      pageSize,
      name: filters.name || undefined,
      categoryId: filters.categoryId || undefined,
      businessType: filters.businessType || undefined,
      status: filters.status === '' ? undefined : filters.status
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function remove(row) {
  await merchantApi.removeProduct(row.id)
  ok('商品已删除')
  load()
}

function reset() {
  Object.assign(filters, { name: '', categoryId: '', businessType: '', status: '' })
  page.value = 1
  load()
}

onMounted(async () => {
  const c = await merchantApi.categories().catch(() => [])
  categories.value = Array.isArray(c) ? c : c?.records || []
  load()
})
</script>

<template>
  <WorkbenchLayout section="merchant">
    <template #actions>
      <RouterLink to="/merchant/products/new" class="m-btn m-btn-primary">+ 新增商品</RouterLink>
    </template>

    <div class="m-panel m-mb-4">
      <div class="filter-row">
        <input v-model="filters.name" class="m-input" placeholder="商品名称" @keyup.enter="page = 1; load()" />
        <select v-model="filters.categoryId" class="m-select">
          <option value="">全部分类</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <select v-model="filters.businessType" class="m-select">
          <option value="">全部业态</option>
          <option value="1">{{ BUSINESS_TYPE[1] }}</option>
          <option value="2">{{ BUSINESS_TYPE[2] }}</option>
        </select>
        <select v-model="filters.status" class="m-select">
          <option value="">全部状态</option>
          <option :value="1">上架</option>
          <option :value="0">下架</option>
        </select>
        <button class="m-btn m-btn-primary" type="button" @click="page = 1; load()">查询</button>
        <button class="m-btn m-btn-soft" type="button" @click="reset">重置</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 80px">ID</th>
            <th>商品</th>
            <th>分类</th>
            <th>价格</th>
            <th>库存</th>
            <th>业态</th>
            <th>状态</th>
            <th style="width: 160px">操作</th>
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
                <img v-if="row.image" class="m-thumb-sm" :src="row.image" :alt="row.name" />
                <span v-else class="m-thumb-sm" />
                <div>
                  <p class="m-h4">{{ row.name }}</p>
                  <p class="m-dim">{{ row.description || '' }}</p>
                </div>
              </div>
            </td>
            <td>{{ row.categoryName || '—' }}</td>
            <td class="m-price">{{ money(row.price) }}</td>
            <td class="m-num">{{ row.stock ?? 0 }}</td>
            <td>
              <span class="m-badge m-badge-ok">{{ BUSINESS_TYPE[row.businessType] || '未设置' }}</span>
            </td>
            <td>
              <MStatus
                :text="Number(row.status) === 1 ? '上架' : '下架'"
                :tone="Number(row.status) === 1 ? 'emerald' : 'gray'"
              />
            </td>
            <td>
              <div class="m-cell-actions">
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="router.push(`/merchant/products/${row.id}/edit`)">
                  编辑
                </button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="remove(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="8" class="m-table-empty">暂无商品，点右上角「新增商品」</td>
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
  min-width: 150px;
}
</style>
