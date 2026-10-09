<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { mallAdminApi } from '@/api/mall'
import { money, shortTime } from '@/utils/format'
import { ok } from '@/lib/toast'

const filters = reactive({ keyword: '', status: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)
const open = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await mallAdminApi.members({
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
  await mallAdminApi.memberStatus(row.id, next)
  ok(next === 1 ? '账号已启用' : '账号已禁用')
  load()
}

async function openDetail(row) {
  detail.value = await mallAdminApi.member(row.id).catch(() => row)
  open.value = true
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
        <input v-model="filters.keyword" class="m-input" placeholder="邮箱 / 昵称" @keyup.enter="page = 1; load()" />
        <select v-model="filters.status" class="m-select">
          <option value="">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">禁用</option>
        </select>
        <button class="m-btn m-btn-primary" type="button" @click="page = 1; load()">查询</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 80px">ID</th>
            <th>账号</th>
            <th>昵称</th>
            <th>订单数</th>
            <th>消费金额</th>
            <th>注册时间</th>
            <th>状态</th>
            <th style="width: 170px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td class="m-num">{{ row.id }}</td>
            <td class="m-num">{{ row.username || row.email }}</td>
            <td>{{ row.nickname || '—' }}</td>
            <td class="m-num">{{ row.orderCount ?? 0 }}</td>
            <td class="m-num">{{ money(row.totalAmount ?? 0) }}</td>
            <td class="m-dim">{{ shortTime(row.createTime) }}</td>
            <td>
              <MStatus
                :text="Number(row.status) === 1 ? '正常' : '禁用'"
                :tone="Number(row.status) === 1 ? 'emerald' : 'red'"
              />
            </td>
            <td>
              <div class="m-cell-actions">
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openDetail(row)">详情</button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="toggle(row)">
                  {{ Number(row.status) === 1 ? '禁用' : '启用' }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="8" class="m-table-empty">暂无会员</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />

    <MModal :open="open" title="会员详情" @close="open = false">
      <dl v-if="detail" class="m-kv">
        <dt>会员 ID</dt>
        <dd class="m-num">{{ detail.id }}</dd>
        <dt>账号</dt>
        <dd class="m-num">{{ detail.username || detail.email }}</dd>
        <dt>昵称</dt>
        <dd>{{ detail.nickname || '—' }}</dd>
        <dt>手机</dt>
        <dd class="m-num">{{ detail.phone || '—' }}</dd>
        <dt>订单数</dt>
        <dd class="m-num">{{ detail.orderCount ?? 0 }}</dd>
        <dt>消费金额</dt>
        <dd class="m-price">{{ money(detail.totalAmount ?? 0) }}</dd>
        <dt>注册时间</dt>
        <dd>{{ shortTime(detail.createTime) }}</dd>
        <dt>状态</dt>
        <dd>
          <MStatus
            :text="Number(detail.status) === 1 ? '正常' : '禁用'"
            :tone="Number(detail.status) === 1 ? 'emerald' : 'red'"
          />
        </dd>
      </dl>
    </MModal>
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
  min-width: 170px;
}
</style>
