<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MPager from '@/components/mall/MPager.vue'
import MStars from '@/components/mall/MStars.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallAdminApi } from '@/api/mall'
import { shortTime } from '@/utils/format'
import { ok } from '@/lib/toast'

const filters = reactive({ productId: '', userId: '', rating: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const data = await mallAdminApi.reviews({
      productId: filters.productId || undefined,
      userId: filters.userId || undefined,
      rating: filters.rating || undefined,
      page: page.value,
      pageSize
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function remove(row) {
  await mallAdminApi.removeReview(row.id)
  ok('评价已删除')
  load()
}

function reset() {
  Object.assign(filters, { productId: '', userId: '', rating: '' })
  page.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout>
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="load">刷新</button>
    </template>

    <div class="m-panel m-mb-4">
      <div class="filter-row">
        <input v-model="filters.productId" class="m-input" placeholder="商品 ID" @keyup.enter="page = 1; load()" />
        <input v-model="filters.userId" class="m-input" placeholder="用户 ID" @keyup.enter="page = 1; load()" />
        <select v-model="filters.rating" class="m-select">
          <option value="">全部星级</option>
          <option v-for="r in [5, 4, 3, 2, 1]" :key="r" :value="r">{{ r }} 星</option>
        </select>
        <button class="m-btn m-btn-primary" type="button" @click="page = 1; load()">查询</button>
        <button class="m-btn m-btn-soft" type="button" @click="reset">重置</button>
      </div>
    </div>

    <div class="m-col" style="gap: 14px">
      <div v-if="loading" class="m-skeleton" style="height: 180px" />

      <div v-for="row in list" :key="row.id" class="m-panel">
        <div class="m-between m-wrapflex" style="gap: 14px">
          <div class="m-cell">
            <img v-if="row.productImage" class="m-thumb-sm" :src="row.productImage" :alt="row.productName" />
            <span v-else class="m-thumb-sm" />
            <div>
              <p class="m-h4">{{ row.productName || `商品 #${row.productId}` }}</p>
              <p class="m-dim">
                {{ row.anonymous ? '匿名用户' : row.nickname || row.username || `用户 #${row.userId}` }}
                · {{ shortTime(row.createTime) }}
              </p>
            </div>
          </div>
          <div class="m-row m-wrapflex" style="gap: 12px">
            <MStars :value="row.rating || 0" :count="null" />
            <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="remove(row)">删除</button>
          </div>
        </div>
        <p class="m-mt-3" style="font-size: 14px">{{ row.content || '（无文字）' }}</p>
        <div v-if="row.images?.length" class="m-row m-wrapflex m-mt-3" style="gap: 8px">
          <img
            v-for="(img, i) in row.images"
            :key="img + i"
            :src="img"
            :alt="`图${i}`"
            style="width: 72px; height: 72px; object-fit: cover; border-radius: 12px"
          />
        </div>
        <p v-if="row.followContent" class="m-dim m-mt-3">追评：{{ row.followContent }}</p>
      </div>

      <MEmpty image="empty-review" v-if="!loading && !list.length" icon="star" title="暂无评价" />
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
