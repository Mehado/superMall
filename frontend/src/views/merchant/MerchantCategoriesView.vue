<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MStatus from '@/components/mall/MStatus.vue'
import { merchantApi } from '@/api/delivery'
import { BUSINESS_TYPE } from '@/utils/format'
import { ok } from '@/lib/toast'

const list = ref([])
const loading = ref(true)
const open = ref(false)
const saving = ref(false)
const businessFilter = ref('')
const form = reactive({ id: null, name: '', sort: 0, businessType: 1, status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await merchantApi.categories({ businessType: businessFilter.value || undefined })
    list.value = Array.isArray(data) ? data : data?.records || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', sort: list.value.length, businessType: 1, status: 1 })
  open.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    sort: row.sort ?? 0,
    businessType: row.businessType ?? 1,
    status: row.status ?? 1
  })
  open.value = true
}

async function save() {
  if (!form.name) {
    ok('请填写分类名称')
    return
  }
  saving.value = true
  try {
    const body = {
      name: form.name,
      sort: Number(form.sort) || 0,
      businessType: Number(form.businessType),
      status: Number(form.status)
    }
    if (form.id) await merchantApi.updateCategory({ ...body, id: form.id })
    else await merchantApi.createCategory(body)
    open.value = false
    ok('分类已保存')
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await merchantApi.removeCategory(row.id)
  ok('分类已删除')
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout section="merchant">
    <template #actions>
      <select v-model="businessFilter" class="m-select" style="width: 170px" @change="load">
        <option value="">全部业态</option>
        <option value="1">{{ BUSINESS_TYPE[1] }}</option>
        <option value="2">{{ BUSINESS_TYPE[2] }}</option>
      </select>
      <button class="m-btn m-btn-primary" type="button" @click="openCreate">+ 新增分类</button>
    </template>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 80px">ID</th>
            <th>分类名称</th>
            <th>业态</th>
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
            <td class="m-h4">{{ row.name }}</td>
            <td><span class="m-badge m-badge-ok">{{ BUSINESS_TYPE[row.businessType] || '—' }}</span></td>
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
            <td colspan="6" class="m-table-empty">暂无分类</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MModal :open="open" :title="form.id ? '编辑分类' : '新增分类'" @close="open = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-field">
          <label class="m-eyebrow">分类名称 *</label>
          <input v-model="form.name" class="m-input" placeholder="如：主食" />
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">业态</label>
            <select v-model.number="form.businessType" class="m-select">
              <option :value="1">{{ BUSINESS_TYPE[1] }}</option>
              <option :value="2">{{ BUSINESS_TYPE[2] }}</option>
            </select>
          </div>
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
