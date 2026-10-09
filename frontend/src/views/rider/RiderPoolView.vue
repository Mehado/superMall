<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MPager from '@/components/mall/MPager.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { riderApi } from '@/api/delivery'
import { DELIVERY_STATUS, money, shortTime, statusOf } from '@/utils/format'
import { ok, err } from '@/lib/toast'

const list = ref([])
const stats = ref(null)
const page = ref(1)
const pageSize = 8
const total = ref(0)
const loading = ref(true)
const grabbingId = ref(null)
let timer = null

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const [data, s] = await Promise.all([
      riderApi.pool({ page: page.value, pageSize }),
      riderApi.statistics().catch(() => null)
    ])
    list.value = data?.records || data?.list || []
    total.value = data?.total ?? list.value.length
    stats.value = s
  } finally {
    loading.value = false
  }
}

/** 抢单是并发安全的：同一单只会有一个骑手抢到 */
async function grab(d) {
  const orderId = d.orderId || d.id
  grabbingId.value = orderId
  try {
    await riderApi.grab(orderId)
    ok('抢单成功，去「我的配送」取餐')
  } catch (e) {
    err('手慢了，配送单可能已被抢走，已刷新列表')
  } finally {
    grabbingId.value = null
    load(true)
  }
}

onMounted(async () => {
  await load()
  timer = setInterval(() => load(true), 15000)
})

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <WorkbenchLayout section="rider" title="骑手工作台">
    <template #actions>
      <button class="m-btn m-btn-primary" type="button" @click="load()">刷新</button>
    </template>

    <div class="m-stats">
      <div class="m-stat">
        <p class="m-eyebrow">可抢配送单</p>
        <p class="m-stat-val m-mt-2">{{ total }}</p>
        <p class="m-dim m-mt-1">WAITING 状态</p>
      </div>
      <div class="m-stat">
        <p class="m-eyebrow">进行中</p>
        <p class="m-stat-val m-mt-2">{{ stats?.delivering ?? stats?.deliveringCount ?? 0 }}</p>
        <p class="m-dim m-mt-1">已接单 / 配送中</p>
      </div>
      <div class="m-stat">
        <p class="m-eyebrow">今日完成</p>
        <p class="m-stat-val m-mt-2">{{ stats?.todayDelivered ?? stats?.completedCount ?? 0 }}</p>
        <p class="m-dim m-mt-1">已送达</p>
      </div>
      <div class="m-stat">
        <p class="m-eyebrow">累计配送费</p>
        <p class="m-stat-val m-mt-2">{{ money(stats?.totalDeliveryFee ?? stats?.todayAmount ?? 0) }}</p>
        <p class="m-dim m-mt-1">按配送单统计</p>
      </div>
    </div>

    <div class="m-panel m-mt-4">
      <div class="m-between m-mb-4">
        <h2 class="m-h3">抢单池</h2>
        <span class="m-dim">列表会定时自动刷新</span>
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 240px" />

      <div v-else-if="list.length" class="pool">
        <div v-for="d in list" :key="d.id" class="m-panel">
          <div class="m-between">
            <div>
              <p class="m-h4">{{ d.shopName || d.pickupAddress || '商家' }}</p>
              <p class="m-dim">配送单 #{{ d.id }}</p>
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
            <dd>{{ d.consignee || '—' }} {{ d.phone || '' }}</dd>
            <dt>配送费</dt>
            <dd class="m-price">{{ money(d.deliveryFee ?? 0) }}</dd>
            <dt>下单时间</dt>
            <dd>{{ shortTime(d.orderTime || d.createTime) }}</dd>
          </dl>

          <button
            class="m-btn m-btn-accent m-btn-block m-mt-4"
            type="button"
            :disabled="grabbingId === (d.orderId || d.id)"
            @click="grab(d)"
          >
            {{ grabbingId === (d.orderId || d.id) ? '抢单中…' : '抢单' }}
          </button>
        </div>
      </div>

      <MEmpty image="empty-data" v-else icon="bell" title="抢单池暂时没有单" desc="商家点「备餐完成」后，配送单才会进入抢单池">
        <button class="m-btn m-btn-soft m-mt-2" type="button" @click="load()">刷新看看</button>
      </MEmpty>
    </div>

    <MPager :page="page" :page-size="pageSize" :total="total" @change="(p) => { page = p; load() }" />
  </WorkbenchLayout>
</template>

<style scoped>
.pool {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}
</style>
