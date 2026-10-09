<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MUploader from '@/components/mall/MUploader.vue'
import { mallAdminApi } from '@/api/mall'
import { ok } from '@/lib/toast'

const filters = reactive({ name: '', status: '' })
const list = ref([])
const loading = ref(true)
const open = ref(false)
const saving = ref(false)
const form = reactive({ id: null, name: '', logo: '', description: '', sort: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await mallAdminApi.brands({
      name: filters.name || undefined,
      status: filters.status === '' ? undefined : filters.status
    })
    list.value = Array.isArray(data) ? data : data?.records || data?.list || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', logo: '', description: '', sort: 0, status: 1 })
  open.value = true
}

async function openEdit(row) {
  const detail = await mallAdminApi.brand(row.id).catch(() => row)
  Object.assign(form, {
    id: detail.id,
    name: detail.name || '',
    logo: detail.logo || '',
    description: detail.description || '',
    sort: detail.sort ?? 0,
    status: detail.status ?? 1
  })
  open.value = true
}

async function upload(file) {
  const res = await mallAdminApi.upload(file)
  return typeof res === 'string' ? res : res?.url || res?.data || ''
}

async function save() {
  if (!form.name) {
    ok('请填写品牌名称')
    return
  }
  saving.value = true
  try {
    const body = {
      name: form.name,
      logo: form.logo || '',
      description: form.description || '',
      sort: Number(form.sort) || 0,
      status: Number(form.status)
    }
    if (form.id) await mallAdminApi.updateBrand({ ...body, id: form.id })
    else await mallAdminApi.createBrand(body)
    open.value = false
    ok('品牌已保存')
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await mallAdminApi.removeBrand(row.id)
  ok('品牌已删除')
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout>
    <template #actions>
      <button class="m-btn m-btn-primary" type="button" @click="openCreate">+ 新增品牌</button>
    </template>

    <div class="m-panel m-mb-4">
      <div class="filter-row">
        <input v-model="filters.name" class="m-input" placeholder="品牌名称" @keyup.enter="load" />
        <select v-model="filters.status" class="m-select">
          <option value="">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </select>
        <button class="m-btn m-btn-primary" type="button" @click="load">查询</button>
      </div>
    </div>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 80px">ID</th>
            <th>品牌</th>
            <th>描述</th>
            <th>排序</th>
            <th>状态</th>
            <th style="width: 160px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td class="m-num">{{ row.id }}</td>
            <td>
              <div class="m-cell">
                <img v-if="row.logo" class="m-thumb-sm" :src="row.logo" :alt="row.name" />
                <span v-else class="m-thumb-sm" />
                <span class="m-h4">{{ row.name }}</span>
              </div>
            </td>
            <td class="m-dim">{{ row.description || '—' }}</td>
            <td class="m-num">{{ row.sort ?? 0 }}</td>
            <td>
              <MStatus
                :text="Number(row.status) === 1 ? '启用' : '停用'"
                :tone="Number(row.status) === 1 ? 'emerald' : 'gray'"
              />
            </td>
            <td>
              <div class="m-cell-actions">
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openEdit(row)">编辑</button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="remove(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="6" class="m-table-empty">暂无品牌</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MModal :open="open" :title="form.id ? '编辑品牌' : '新增品牌'" @close="open = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-field">
          <label class="m-eyebrow">品牌名称 *</label>
          <input v-model="form.name" class="m-input" placeholder="如：优衣库" />
        </div>
        <div class="m-field">
          <span class="m-eyebrow">品牌 Logo</span>
          <MUploader
            :model-value="form.logo ? [form.logo] : []"
            :uploader="upload"
            :max="1"
            @update:model-value="(v) => (form.logo = v[0] || '')"
          />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">品牌描述</label>
          <textarea v-model="form.description" class="m-textarea" />
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">排序</label>
            <input v-model.number="form.sort" type="number" class="m-input" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">状态</label>
            <select v-model.number="form.status" class="m-select">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="m-btn m-btn-soft" type="button" @click="open = false">取消</button>
        <button class="m-btn m-btn-primary" type="button" :disabled="saving" @click="save">保存</button>
      </template>
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
  min-width: 160px;
}
</style>
