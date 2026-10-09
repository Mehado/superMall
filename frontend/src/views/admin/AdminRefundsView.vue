<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { mallAdminApi } from '@/api/mall'
import { MALL_REFUND_STATUS, MALL_REFUND_TYPE, money, shortTime, statusOf } from '@/utils/format'
import { ok } from '@/lib/toast'

const filters = reactive({ status: 'PENDING' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)
const open = ref(false)
const auditTarget = ref(null)
const auditForm = reactive({ approved: true, remark: '' })

async function load() {
  loading.value = true
  try {
    const data = await mallAdminApi.refunds({
      status: filters.status || undefined,
      page: page.value,
      pageSize
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function openAudit(row, approved = true) {
  auditTarget.value = row
  auditForm.approved = approved
  auditForm.remark = ''
  open.value = true
}

async function submitAudit() {
  if (!auditForm.approved && !auditForm.remark.trim()) {
    ok('驳回时请填写驳回原因，买家会看到该说明')
    return
  }
  await mallAdminApi.auditRefund(auditTarget.value.id, {
    approved: auditForm.approved,
    adminRemark: auditForm.remark
  })
  open.value = false
  ok(auditForm.approved ? '已同意退款' : '已驳回，订单回到申请前状态')
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout>
    <template #actions>
      <select v-model="filters.status" class="m-select" style="width: 160px" @change="page = 1; load()">
        <option value="">全部</option>
        <option value="PENDING">待审核</option>
        <option value="APPROVED">已同意</option>
        <option value="REJECTED">已驳回</option>
      </select>
      <button class="m-btn m-btn-primary" type="button" @click="load">刷新</button>
    </template>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 80px">ID</th>
            <th>订单号</th>
            <th>类型</th>
            <th>金额</th>
            <th>原因</th>
            <th>状态</th>
            <th>申请时间</th>
            <th style="width: 180px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td class="m-num">{{ row.id }}</td>
            <td class="m-num">{{ row.orderNumber }}</td>
            <td>{{ MALL_REFUND_TYPE[row.type] || row.type || '—' }}</td>
            <td class="m-price">{{ money(row.amount ?? row.refundAmount ?? 0) }}</td>
            <td class="m-dim">{{ row.reason || '—' }}</td>
            <td>
              <MStatus
                :text="statusOf(MALL_REFUND_STATUS, row.status).text"
                :tone="statusOf(MALL_REFUND_STATUS, row.status).tone"
              />
            </td>
            <td class="m-dim">{{ shortTime(row.createTime) }}</td>
            <td>
              <div v-if="row.status === 'PENDING' || !row.status" class="m-cell-actions">
                <button class="m-btn m-btn-primary m-btn-sm" type="button" @click="openAudit(row, true)">同意</button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="openAudit(row, false)">驳回</button>
              </div>
              <span v-else class="m-dim">{{ row.adminRemark || '已处理' }}</span>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="8" class="m-table-empty">暂无退款申请</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />

    <MModal :open="open" :title="auditForm.approved ? '同意退款' : '驳回退款'" @close="open = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-soft" style="padding: 14px 16px">
          <p class="m-dim">订单号</p>
          <p class="m-h4 m-num">{{ auditTarget?.orderNumber }}</p>
          <p class="m-dim m-mt-2">
            退款金额 {{ money(auditTarget?.amount ?? auditTarget?.refundAmount ?? 0) }}
          </p>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">审核备注</label>
          <textarea
            v-model="auditForm.remark"
            class="m-textarea"
            :placeholder="auditForm.approved ? '同意退款，说明可选' : '请填写驳回原因'"
          />
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="open = false">取消</button>
        <button
          class="m-btn"
          :class="auditForm.approved ? 'm-btn-primary' : 'm-btn-danger'"
          type="button"
          @click="submitAudit"
        >
          {{ auditForm.approved ? '确认同意' : '确认驳回' }}
        </button>
      </template>
    </MModal>
  </WorkbenchLayout>
</template>
