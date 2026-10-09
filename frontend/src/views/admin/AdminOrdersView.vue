<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { mallAdminApi } from '@/api/mall'
import { MALL_ORDER_STATUS, money, shortTime, statusOf } from '@/utils/format'
import { ok } from '@/lib/toast'

const ORDER_STATUS = ['PENDING_PAYMENT', 'PENDING_DELIVERY', 'PENDING_RECEIPT', 'COMPLETED', 'CANCELLED', 'REFUNDING', 'REFUNDED']

const filters = reactive({ status: '', orderNumber: '', userId: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)

const shipOpen = ref(false)
const shipTarget = ref(null)
const shipForm = reactive({ company: '', trackingNo: '' })
const detailOpen = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await mallAdminApi.orders({
      status: filters.status || undefined,
      orderNumber: filters.orderNumber || undefined,
      userId: filters.userId || undefined,
      page: page.value,
      pageSize
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function openShip(row) {
  shipTarget.value = row
  shipForm.company = ''
  shipForm.trackingNo = ''
  shipOpen.value = true
}

async function ship() {
  if (!shipForm.company.trim() || !shipForm.trackingNo.trim()) {
    ok('请填写物流公司与运单号')
    return
  }
  await mallAdminApi.ship(shipTarget.value.orderNumber, {
    company: shipForm.company.trim(),
    trackingNo: shipForm.trackingNo.trim()
  })
  shipOpen.value = false
  ok('已发货，订单进入待收货')
  load()
}

async function openDetail(row) {
  detail.value = await mallAdminApi.order(row.orderNumber)
  detailOpen.value = true
}

function reset() {
  Object.assign(filters, { status: '', orderNumber: '', userId: '' })
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
        <input v-model="filters.orderNumber" class="m-input" placeholder="订单号" @keyup.enter="page = 1; load()" />
        <input v-model="filters.userId" class="m-input" placeholder="用户 ID" @keyup.enter="page = 1; load()" />
        <select v-model="filters.status" class="m-select">
          <option value="">全部状态</option>
          <option v-for="s in ORDER_STATUS" :key="s" :value="s">{{ statusOf(MALL_ORDER_STATUS, s).text }}</option>
        </select>
        <button class="m-btn m-btn-primary" type="button" @click="page = 1; load()">查询</button>
        <button class="m-btn m-btn-soft" type="button" @click="reset">重置</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>买家</th>
            <th>金额</th>
            <th>状态</th>
            <th>下单时间</th>
            <th>物流</th>
            <th style="width: 200px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.orderNumber">
            <td class="m-num">{{ row.orderNumber }}</td>
            <td>
              <p class="m-h4">{{ row.consignee || row.receiver || '—' }}</p>
              <p class="m-dim m-num">{{ row.phone || '' }}</p>
            </td>
            <td class="m-price">{{ money(row.payAmount) }}</td>
            <td>
              <MStatus
                :text="statusOf(MALL_ORDER_STATUS, row.status).text"
                :tone="statusOf(MALL_ORDER_STATUS, row.status).tone"
              />
            </td>
            <td class="m-dim">{{ shortTime(row.createTime) }}</td>
            <td class="m-dim">
              {{ row.shipment?.company || '—' }}
              {{ row.shipment?.trackingNo || '' }}
            </td>
            <td>
              <div class="m-cell-actions">
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openDetail(row)">详情</button>
                <button
                  v-if="row.status === 'PENDING_DELIVERY'"
                  class="m-btn m-btn-primary m-btn-sm"
                  type="button"
                  @click="openShip(row)"
                >
                  发货
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

    <MModal :open="shipOpen" title="订单发货" @close="shipOpen = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-soft" style="padding: 14px 16px">
          <p class="m-dim">订单号</p>
          <p class="m-h4 m-num">{{ shipTarget?.orderNumber }}</p>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">物流公司</label>
          <input v-model="shipForm.company" class="m-input" placeholder="如：顺丰速运" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">运单号</label>
          <input v-model="shipForm.trackingNo" class="m-input" placeholder="SF1234567890" />
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="shipOpen = false">取消</button>
        <button class="m-btn m-btn-primary" type="button" @click="ship">确认发货</button>
      </template>
    </MModal>

    <MModal :open="detailOpen" title="订单详情" wide @close="detailOpen = false">
      <div v-if="detail">
        <dl class="m-kv">
          <dt>订单号</dt>
          <dd class="m-num">{{ detail.orderNumber }}</dd>
          <dt>状态</dt>
          <dd>
            <MStatus
              :text="statusOf(MALL_ORDER_STATUS, detail.status).text"
              :tone="statusOf(MALL_ORDER_STATUS, detail.status).tone"
            />
          </dd>
          <dt>收货人</dt>
          <dd>{{ detail.consignee || '—' }} / {{ detail.phone || '—' }}</dd>
          <dt>地址</dt>
          <dd>{{ detail.address || `${detail.province || ''}${detail.city || ''}${detail.district || ''}${detail.detail || ''}` }}</dd>
          <dt>实付</dt>
          <dd class="m-price">{{ money(detail.payAmount) }}</dd>
          <dt>下单时间</dt>
          <dd>{{ shortTime(detail.createTime) }}</dd>
          <dt>备注</dt>
          <dd>{{ detail.remark || '无' }}</dd>
        </dl>

        <div class="m-divider" />
        <div class="m-col" style="gap: 14px">
          <div v-for="it in detail.items || detail.orderItems || []" :key="it.id" class="m-row" style="gap: 14px">
            <img v-if="it.image" class="m-thumb-sm" :src="it.image" :alt="it.name" />
            <span v-else class="m-thumb-sm" />
            <div class="m-grow">
              <p class="m-h4">{{ it.name || it.productName }}</p>
              <p class="m-dim">{{ it.skuName || (it.specValues || []).join(' / ') }}</p>
            </div>
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
