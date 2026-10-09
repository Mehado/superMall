<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { platformApi } from '@/api/delivery'
import { shortTime } from '@/utils/format'
import { ok } from '@/lib/toast'

/**
 * 平台运营 · 骑手管理。
 * 骑手注册即启用，这里主要用于查看与停用异常账号（停用后无法登录、无法接单）。
 */
const filters = reactive({ keyword: '', status: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const data = await platformApi.deliverymen({
      keyword: filters.keyword || undefined,
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

async function toggle(row) {
  const next = Number(row.status) === 1 ? 0 : 1
  await platformApi.updateDeliverymanStatus(row.id, next)
  ok(next === 1 ? '骑手已启用，可接单' : '骑手已停用')
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout section="platform">
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="load">刷新</button>
    </template>

    <div class="m-panel m-mb-4">
      <div class="filter-row">
        <input
          v-model="filters.keyword"
          class="m-input"
          placeholder="姓名 / 邮箱 / 手机号"
          @keyup.enter="page = 1; load()"
        />
        <select v-model="filters.status" class="m-select" @change="page = 1; load()">
          <option value="">全部状态</option>
          <option :value="1">可接单</option>
          <option :value="0">已停用</option>
        </select>
        <button class="m-btn m-btn-primary" type="button" @click="page = 1; load()">查询</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 70px">ID</th>
            <th>姓名</th>
            <th>账号（邮箱）</th>
            <th>手机号</th>
            <th>注册时间</th>
            <th>状态</th>
            <th style="width: 130px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td class="m-num">{{ row.id }}</td>
            <td>{{ row.name || '—' }}</td>
            <td class="m-num">{{ row.username }}</td>
            <td class="m-num">{{ row.phone || '—' }}</td>
            <td class="m-dim">{{ shortTime(row.createTime) }}</td>
            <td>
              <MStatus
                :text="Number(row.status) === 1 ? '可接单' : '已停用'"
                :tone="Number(row.status) === 1 ? 'emerald' : 'red'"
              />
            </td>
            <td>
              <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="toggle(row)">
                {{ Number(row.status) === 1 ? '停用' : '启用' }}
              </button>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="7" class="m-table-empty">暂无骑手</td>
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
  min-width: 180px;
}
</style>
