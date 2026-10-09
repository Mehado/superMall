<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MTabs from '@/components/mall/MTabs.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { riderApi } from '@/api/delivery'
import { DELIVERY_STATUS, money, shortTime, statusOf } from '@/utils/format'
import { ok } from '@/lib/toast'

const TABS = [
  { key: '', label: '全部' },
  { key: 'ACCEPTED', label: statusOf(DELIVERY_STATUS, 'ACCEPTED').text },
  { key: 'DELIVERING', label: statusOf(DELIVERY_STATUS, 'DELIVERING').text },
  { key: 'DELIVERED', label: statusOf(DELIVERY_STATUS, 'DELIVERED').text }
]

const status = ref('')
const page = ref(1)
const pageSize = 8
const total = ref(0)
const list = ref([])
const loading = ref(true)
const acting = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await riderApi.mine({
      page: page.value,
      pageSize,
      status: status.value || undefined
    })
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function changeTab(key) {
  status.value = key
  page.value = 1
  load()
}

async function pickup(d) {
  acting.value = d.orderId
  try {
    await riderApi.pickup(d.orderId)
    ok('已取餐，开始配送')
    load()
  } finally {
    acting.value = null
  }
}

async function delivered(d) {
  acting.value = d.orderId
  try {
    await riderApi.delivered(d.orderId)
    ok('已送达，订单完成')
    load()
  } finally {
    acting.value = null
  }
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout section="rider">
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="load">刷新</button>
    </template>

    <MTabs :model-value="status" :tabs="TABS" @update:model-value="changeTab" />

    <div v-if="loading" class="m-skeleton m-mt-4" style="height: 240px" />

    <div v-else-if="list.length" class="m-col m-mt-5" style="gap: 16px">
      <div v-for="d in list" :key="d.id" class="m-panel">
        <div class="m-between m-wrapflex">
          <div>
            <p class="m-h3">{{ d.shopName || '商家' }}</p>
            <p class="m-dim">配送单 #{{ d.id }} · 订单 id {{ d.orderId }}</p>
          </div>
          <MStatus
            :text="statusOf(DELIVERY_STATUS, d.status).text"
            :tone="statusOf(DELIVERY_STATUS, d.status).tone"
          />
        </div>

        <div class="m-divider" />

        <dl class="m-kv">
          <dt>取餐地址</dt>
          <dd>{{ d.pickupAddress || '—' }}</dd>
          <dt>送达地址</dt>
          <dd>{{ d.receiverAddress || d.address || '—' }}</dd>
          <dt>收货人</dt>
          <dd>{{ d.consignee || '—' }} <span class="m-num">{{ d.phone || '' }}</span></dd>
          <dt>配送费</dt>
          <dd class="m-price">{{ money(d.deliveryFee ?? 0) }}</dd>
          <dt>接单时间</dt>
          <dd>{{ shortTime(d.acceptTime || d.createTime) }}</dd>
        </dl>

        <div class="m-row m-wrapflex m-mt-4" style="gap: 10px">
          <button
            v-if="d.status === 'ACCEPTED'"
            class="m-btn m-btn-accent"
            type="button"
            :disabled="acting === d.orderId"
            @click="pickup(d)"
          >
            {{ acting === d.orderId ? '处理中…' : '取餐' }}
          </button>
          <button
            v-if="d.status === 'DELIVERING'"
            class="m-btn m-btn-primary"
            type="button"
            :disabled="acting === d.orderId"
            @click="delivered(d)"
          >
            {{ acting === d.orderId ? '处理中…' : '确认送达' }}
          </button>
          <span v-if="d.status === 'DELIVERED'" class="m-dim">已完成，等待系统结算</span>
        </div>
      </div>

      <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
    </div>

    <MEmpty v-else image="empty-order" icon="truck" title="还没有配送单" desc="去抢单池抢一单试试">
      <RouterLink to="/rider" class="m-btn m-btn-accent m-mt-2">去抢单池</RouterLink>
    </MEmpty>
  </WorkbenchLayout>
</template>
