<script setup>
import { onMounted, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MStatus from '@/components/mall/MStatus.vue'
import ProfileEditModal from '@/components/ProfileEditModal.vue'
import { riderApi } from '@/api/delivery'
import { money } from '@/utils/format'

/**
 * 骑手资料。
 *
 * 可改字段只有 **姓名 / 手机号**（`deliveryman` 表就只有这两列 + 账号状态），
 * 与买家共用 `ProfileEditModal`，所以交互一致。
 * 早期页面上的「身份证 / 交通工具 / 车牌号」后端并没有字段（保存是静默无效的），已去掉。
 */
const loading = ref(true)
const profile = ref(null)
const stats = ref(null)
const editOpen = ref(false)

const RIDER_FIELDS = [
  { key: 'name', label: '姓名', placeholder: '如：疾风', maxlength: 50, required: true },
  { key: 'phone', label: '手机号', placeholder: '11 位手机号', maxlength: 11, phone: true }
]

async function load() {
  loading.value = true
  try {
    const [p, s] = await Promise.all([
      riderApi.profile().catch(() => null),
      riderApi.statistics().catch(() => null)
    ])
    profile.value = p
    stats.value = s
  } finally {
    loading.value = false
  }
}

/** 弹窗负责提示与关闭，这里只落库 + 刷新 */
async function saveProfile(payload) {
  await riderApi.updateProfile(payload)
  await load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout section="rider">
    <template #actions>
      <button class="m-btn m-btn-primary" type="button" @click="editOpen = true">编辑资料</button>
    </template>

    <div class="m-stats">
      <div class="m-stat">
        <p class="m-eyebrow">已完成配送</p>
        <p class="m-stat-val m-mt-2">{{ stats?.deliveredCount ?? stats?.completedCount ?? 0 }}</p>
      </div>
      <div class="m-stat">
        <p class="m-eyebrow">进行中</p>
        <p class="m-stat-val m-mt-2">{{ stats?.deliveringCount ?? 0 }}</p>
      </div>
      <div class="m-stat">
        <p class="m-eyebrow">累计配送费</p>
        <p class="m-stat-val m-mt-2">{{ money(stats?.totalDeliveryFee ?? 0) }}</p>
      </div>
      <div class="m-stat">
        <p class="m-eyebrow">账号状态</p>
        <p class="m-stat-val m-mt-2" style="font-size: 22px">
          {{ profile?.status === 0 ? '禁用' : '正常' }}
        </p>
        <p class="m-dim m-mt-1">可正常接单</p>
      </div>
    </div>

    <div class="m-panel m-mt-4" style="max-width: 760px">
      <div class="m-between m-mb-4">
        <h2 class="m-h3">基本信息</h2>
        <MStatus
          :text="profile?.status === 0 ? '禁用' : '在职'"
          :tone="profile?.status === 0 ? 'red' : 'emerald'"
        />
      </div>

      <div v-if="loading" class="m-skeleton" style="height: 220px" />

      <dl v-else class="m-kv">
        <dt>姓名</dt>
        <dd>{{ profile?.name || '—' }}</dd>
        <dt>手机号</dt>
        <dd class="m-num">{{ profile?.phone || '—' }}</dd>
        <dt>登录账号</dt>
        <dd class="m-num">{{ profile?.username || '—' }}</dd>
        <dt>注册时间</dt>
        <dd class="m-dim">{{ profile?.createTime || '—' }}</dd>
      </dl>

      <p class="m-dim m-mt-4">姓名与手机号可自助修改；账号状态由平台运营管理。</p>
    </div>

    <ProfileEditModal
      :open="editOpen"
      :profile="profile || {}"
      :fields="RIDER_FIELDS"
      :saver="saveProfile"
      @close="editOpen = false"
      @saved="load"
    />
  </WorkbenchLayout>
</template>
