<script setup>
import { onMounted, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MStatus from '@/components/mall/MStatus.vue'
import ProfileEditModal from '@/components/ProfileEditModal.vue'
import { platformApi } from '@/api/delivery'

/**
 * 平台运营 · 账号资料。
 *
 * 可改字段只有 **姓名**（`platform_admin` 表只有 账号/密码/状态/姓名）。
 * 与买家/骑手共用 `ProfileEditModal`，交互一致。
 */
const loading = ref(true)
const profile = ref(null)
const editOpen = ref(false)

const FIELDS = [{ key: 'name', label: '姓名', placeholder: '如：平台运营', maxlength: 50, required: true }]

async function load() {
  loading.value = true
  try {
    profile.value = await platformApi.profile().catch(() => null)
  } finally {
    loading.value = false
  }
}

async function saveProfile(payload) {
  await platformApi.updateProfile(payload)
  await load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout section="platform">
    <template #actions>
      <button class="m-btn m-btn-primary" type="button" @click="editOpen = true">编辑资料</button>
    </template>

    <div v-if="loading" class="m-skeleton" style="height: 260px" />

    <div v-else class="m-panel" style="max-width: 760px">
      <div class="m-between m-mb-4">
        <h2 class="m-h3">账号信息</h2>
        <MStatus
          :text="profile?.status === 0 ? '已禁用' : '正常'"
          :tone="profile?.status === 0 ? 'red' : 'emerald'"
        />
      </div>

      <dl class="m-kv">
        <dt>姓名</dt>
        <dd>{{ profile?.name || '—' }}</dd>
        <dt>登录账号</dt>
        <dd class="m-num">{{ profile?.username || '—' }}</dd>
        <dt>角色</dt>
        <dd>平台运营（商家入驻审核 / 骑手管理）</dd>
      </dl>

      <p class="m-dim m-mt-4">姓名可自助修改；登录账号与账号状态由系统管理。</p>
    </div>

    <ProfileEditModal
      :open="editOpen"
      :profile="profile || {}"
      :fields="FIELDS"
      :saver="saveProfile"
      @close="editOpen = false"
      @saved="load"
    />
  </WorkbenchLayout>
</template>
