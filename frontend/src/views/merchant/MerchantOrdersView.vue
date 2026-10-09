<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { merchantApi } from '@/api/delivery'
import { FOOD_ORDER_STATUS, money, shortTime, statusOf, toQueryTime } from '@/utils/format'
import { ok } from '@/lib/toast'

const STATUSES = ['PENDING_PAYMENT', 'PAID', 'MERCHANT_ACCEPTED', 'WAITING_DELIVERY', 'DELIVERING', 'COMPLETED', 'CANCELLED']

const filters = reactive({ status: '', number: '', phone: '', beginTime: '', endTime: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)

const rejectOpen = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const detailOpen = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await merchantApi.orders({
      page: page.value,
      pageSize,
      status: filters.status || undefined,
      number: filters.number || undefined,
      phone: filters.phone || undefined,
      // 查询参数时间必须带秒
      beginTime: filters.beginTime ? toQueryTime(filters.beginTime) : undefined,
      endTime: filters.endTime ? toQueryTime(filters.endTime) : undefined
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function accept(order) {
  await merchantApi.accept(order.id)
  ok('已接单')
  load()
}

async function ready(order) {
  await merchantApi.ready(order.id)
  ok('备餐完成，配送单已进入抢单池')
  load()
}

function openReject(order) {
  rejectTarget.value = order
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  await merchantApi.reject({ id: rejectTarget.value.id, cancelReason: rejectReason.value })
  rejectOpen.value = false
  ok('已拒单（自动退款并回补库存）')
  load()
}

async function openDetail(order) {
  detail.value = await merchantApi.order(order.id)
  detailOpen.value = true
}

function reset() {
  Object.assign(filters, { status: '', number: '', phone: '', beginTime: '', endTime: '' })
  page.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout section="merchant">
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="load">刷新</button>
    </template>

    <div class="m-panel m-mb-4">
      <div class="filter-row">
        <input v-model="filters.number" class="m-input" placeholder="订单号" />
        <input v-model="filters.phone" class="m-input" placeholder="收货人手机" />
        <select v-model="filters.status" class="m-select">
          <option value="">全部状态</option>
          <option v-for="s in STATUSES" :key="s" :value="s">{{ statusOf(FOOD_ORDER_STATUS, s).text }}</option>
        </select>
        <input v-model="filters.beginTime" type="datetime-local" class="m-input" />
        <input v-model="filters.endTime" type="datetime-local" class="m-input" />
        <button class="m-btn m-btn-primary" type="button" @click="page = 1; load()">查询</button>
        <button class="m-btn m-btn-soft" type="button" @click="reset">重置</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>收货人</th>
            <th>商品</th>
            <th>金额</th>
            <th>状态</th>
            <th>下单时间</th>
            <th style="width: 240px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="order in list" :key="order.id">
            <td class="m-num">{{ order.number || order.orderNumber }}</td>
            <td>
              <p class="m-h4">{{ order.consignee || '—' }}</p>
              <p class="m-dim m-num">{{ order.phone || '' }}</p>
            </td>
            <td class="m-dim">
              {{ (order.orderItems || order.items || []).map((i) => `${i.name || i.productName} ×${i.number || i.quantity}`).join('，') || '—' }}
            </td>
            <td class="m-price">{{ money(order.amount ?? order.orderAmount) }}</td>
            <td>
              <MStatus
                :text="statusOf(FOOD_ORDER_STATUS, order.status).text"
                :tone="statusOf(FOOD_ORDER_STATUS, order.status).tone"
              />
            </td>
            <td class="m-dim">{{ shortTime(order.orderTime) }}</td>
            <td>
              <div class="m-cell-actions">
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openDetail(order)">详情</button>
                <button v-if="order.status === 'PAID'" class="m-btn m-btn-accent m-btn-sm" type="button" @click="accept(order)">接单</button>
                <button v-if="order.status === 'PAID'" class="m-btn m-btn-outline m-btn-sm" type="button" @click="openReject(order)">拒单</button>
                <button
                  v-if="order.status === 'MERCHANT_ACCEPTED'"
                  class="m-btn m-btn-primary m-btn-sm"
                  type="button"
                  @click="ready(order)"
                >
                  备餐完成
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="7" class="m-table-empty">暂无订单</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />

    <MModal :open="rejectOpen" title="拒单" @close="rejectOpen = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-soft" style="padding: 14px 16px">
          <p class="m-dim">订单号</p>
          <p class="m-h4 m-num">{{ rejectTarget?.number || rejectTarget?.orderNumber }}</p>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">拒单原因</label>
          <textarea v-model="rejectReason" class="m-textarea" placeholder="如：食材售罄" />
        </div>
        <p class="m-dim">拒单后订单变为已取消，会自动退款并回补库存。</p>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="rejectOpen = false">取消</button>
        <button class="m-btn m-btn-danger" type="button" @click="submitReject">确认拒单</button>
      </template>
    </MModal>

    <MModal :open="detailOpen" title="订单详情" wide @close="detailOpen = false">
      <div v-if="detail">
        <dl class="m-kv">
          <dt>订单号</dt>
          <dd class="m-num">{{ detail.number || detail.orderNumber }}</dd>
          <dt>状态</dt>
          <dd>
            <MStatus
              :text="statusOf(FOOD_ORDER_STATUS, detail.status).text"
              :tone="statusOf(FOOD_ORDER_STATUS, detail.status).tone"
            />
          </dd>
          <dt>收货人</dt>
          <dd>{{ detail.consignee || '—' }} / {{ detail.phone || '—' }}</dd>
          <dt>地址</dt>
          <dd>{{ detail.address || `${detail.province || ''}${detail.city || ''}${detail.district || ''}${detail.detail || ''}` }}</dd>
          <dt>备注</dt>
          <dd>{{ detail.remark || '无' }}</dd>
          <dt>金额</dt>
          <dd class="m-price">{{ money(detail.amount ?? detail.orderAmount) }}</dd>
        </dl>
        <div class="m-divider" />
        <div class="m-col" style="gap: 12px">
          <div v-for="it in detail.orderItems || detail.items || []" :key="it.id" class="m-row" style="gap: 10px">
            <span class="m-grow m-h4">{{ it.name || it.productName }}</span>
            <span class="m-muted m-num" style="font-size: 13px">×{{ it.number || it.quantity }}</span>
            <span class="m-price">{{ money(it.price) }}</span>
          </div>
        </div>
      </div>
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
  min-width: 150px;
}
</style>
