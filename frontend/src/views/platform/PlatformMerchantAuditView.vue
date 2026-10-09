<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { platformApi } from '@/api/delivery'
import { shortTime } from '@/utils/format'
import { ok } from '@/lib/toast'

/**
 * 平台运营 · 商家入驻审核。
 * 待审核的申请通过后商家即可登录经营；驳回必须填原因（商家可据此重新提交）。
 */
const filters = reactive({ auditStatus: 0, keyword: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const list = ref([])
const loading = ref(true)
const overview = ref(null)

const auditOpen = ref(false)
const auditRow = ref(null)
const auditForm = reactive({ approved: true, reason: '' })
const auditing = ref(false)

const tabs = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' },
  { value: '', label: '全部' }
]

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '待审核', value: o.pendingMerchants ?? 0, tone: 'amber' },
    { label: '已通过商家', value: o.approvedMerchants ?? 0, tone: 'emerald' },
    { label: '已驳回', value: o.rejectedMerchants ?? 0, tone: 'red' },
    { label: '可接单骑手', value: o.activeRiders ?? 0, tone: 'ink' }
  ]
})

function auditTone(status) {
  if (status === 0) return 'amber'
  if (status === 1) return 'emerald'
  return 'red'
}
const auditText = (status) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[status] || '未知')

async function loadOverview() {
  overview.value = await platformApi.overview().catch(() => null)
}

async function load() {
  loading.value = true
  try {
    const data = await platformApi.merchants({
      auditStatus: filters.auditStatus === '' ? undefined : filters.auditStatus,
      keyword: filters.keyword || undefined,
      page: page.value,
      pageSize
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function switchTab(value) {
  filters.auditStatus = value
  page.value = 1
  load()
}

function openAudit(row, approved) {
  auditRow.value = row
  auditForm.approved = approved
  auditForm.reason = ''
  auditOpen.value = true
}

async function submitAudit() {
  if (!auditForm.approved && !auditForm.reason.trim()) {
    ok('驳回时必须填写驳回原因')
    return
  }
  auditing.value = true
  try {
    await platformApi.auditMerchant(auditRow.value.id, {
      approved: auditForm.approved,
      reason: auditForm.approved ? undefined : auditForm.reason.trim()
    })
    ok(auditForm.approved ? '已通过，商家可登录经营' : '已驳回')
    auditOpen.value = false
    await Promise.all([load(), loadOverview()])
  } finally {
    auditing.value = false
  }
}

onMounted(() => {
  loadOverview()
  load()
})
</script>

<template>
  <WorkbenchLayout section="platform">
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="loadOverview(); load()">刷新</button>
    </template>

    <div class="stat-grid m-mb-4">
      <div v-for="c in cards" :key="c.label" class="stat-card">
        <span class="m-eyebrow">{{ c.label }}</span>
        <strong class="stat-value" :class="`tone-${c.tone}`">{{ c.value }}</strong>
      </div>
    </div>

    <div class="m-panel m-mb-4">
      <div class="filter-row">
        <div class="m-chipbar">
          <button
            v-for="t in tabs"
            :key="String(t.value)"
            type="button"
            class="m-chip"
            :class="{ 'm-chip-active': filters.auditStatus === t.value }"
            @click="switchTab(t.value)"
          >
            {{ t.label }}
          </button>
        </div>
        <input
          v-model="filters.keyword"
          class="m-input"
          placeholder="店铺名 / 邮箱 / 手机号"
          @keyup.enter="page = 1; load()"
        />
        <button class="m-btn m-btn-primary" type="button" @click="page = 1; load()">查询</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 70px">ID</th>
            <th>店铺名称</th>
            <th>账号（邮箱）</th>
            <th>联系人 / 电话</th>
            <th>店铺地址</th>
            <th>申请时间</th>
            <th>状态</th>
            <th style="width: 180px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td class="m-num">{{ row.id }}</td>
            <td>
              <strong>{{ row.shopName }}</strong>
              <p v-if="row.description" class="m-dim" style="margin-top: 2px">{{ row.description }}</p>
              <p v-if="row.rejectReason" class="reject-reason">驳回：{{ row.rejectReason }}</p>
            </td>
            <td class="m-num">{{ row.username }}</td>
            <td>
              <span>{{ row.contactName || '—' }}</span>
              <p class="m-dim m-num" style="margin-top: 2px">{{ row.phone || '—' }}</p>
            </td>
            <td class="m-dim">{{ row.address || '—' }}</td>
            <td class="m-dim">{{ shortTime(row.applyTime || row.createTime) }}</td>
            <td>
              <MStatus :text="auditText(row.auditStatus)" :tone="auditTone(row.auditStatus)" />
            </td>
            <td>
              <div class="m-cell-actions">
                <template v-if="row.auditStatus === 0">
                  <button class="m-btn m-btn-primary m-btn-sm" type="button" @click="openAudit(row, true)">通过</button>
                  <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="openAudit(row, false)">驳回</button>
                </template>
                <a v-if="row.licenseImage" :href="row.licenseImage" target="_blank" class="m-btn-link">查看执照</a>
                <span v-if="row.auditStatus !== 0" class="m-dim">已审核</span>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="8" class="m-table-empty">暂无入驻申请</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />

    <MModal :open="auditOpen" :title="auditForm.approved ? '通过入驻申请' : '驳回入驻申请'" @close="auditOpen = false">
      <div v-if="auditRow" class="audit-body">
        <dl class="m-kv">
          <dt>店铺</dt>
          <dd>{{ auditRow.shopName }}</dd>
          <dt>账号</dt>
          <dd class="m-num">{{ auditRow.username }}</dd>
          <dt>联系人</dt>
          <dd>{{ auditRow.contactName || '—' }} / {{ auditRow.phone || '—' }}</dd>
          <dt>地址</dt>
          <dd>{{ auditRow.address || '—' }}</dd>
        </dl>

        <div v-if="!auditForm.approved" class="m-field" style="margin-top: 16px">
          <label class="m-eyebrow">驳回原因（必填）</label>
          <textarea
            v-model="auditForm.reason"
            class="m-input"
            rows="3"
            placeholder="例如：营业执照照片不清晰，请重新上传"
          ></textarea>
        </div>
        <p v-else class="m-dim" style="margin-top: 14px">通过后店铺状态置为「营业」，该邮箱即可登录商家端。</p>
      </div>

      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="auditOpen = false">取消</button>
        <button
          class="m-btn"
          :class="auditForm.approved ? 'm-btn-primary' : 'm-btn-outline'"
          type="button"
          :disabled="auditing"
          @click="submitAudit"
        >
          {{ auditing ? '提交中…' : auditForm.approved ? '确认通过' : '确认驳回' }}
        </button>
      </template>
    </MModal>
  </WorkbenchLayout>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-sm);
  background: var(--m-bone);
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.stat-value.tone-amber {
  color: #a5670a;
}
.stat-value.tone-emerald {
  color: #12703d;
}
.stat-value.tone-red {
  color: #a52a2a;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.m-chipbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-right: auto;
}
.filter-row .m-input {
  width: auto;
  min-width: 220px;
}
.reject-reason {
  margin-top: 3px;
  font-size: 12px;
  color: var(--m-danger);
}
.audit-body {
  min-width: 0;
}
</style>
