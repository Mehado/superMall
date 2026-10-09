<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MUploader from '@/components/mall/MUploader.vue'
import { mallAdminApi } from '@/api/mall'
import { ok } from '@/lib/toast'

const POSITIONS = [
  { value: 1, label: '1 · 首页轮播' },
  { value: 2, label: '2 · 金刚区' },
  { value: 3, label: '3 · 其它位' }
]

const list = ref([])
const loading = ref(true)
const open = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  position: 1,
  title: '',
  subtitle: '',
  image: '',
  link: '',
  targetType: '',
  targetId: '',
  sort: 0,
  status: 1
})

function positionLabel(v) {
  return POSITIONS.find((p) => p.value === Number(v))?.label || v
}

async function load() {
  loading.value = true
  try {
    const data = await mallAdminApi.banners()
    list.value = Array.isArray(data) ? data : data?.records || data?.list || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    position: 1,
    title: '',
    subtitle: '',
    image: '',
    link: '',
    targetType: '',
    targetId: '',
    sort: 0,
    status: 1
  })
  open.value = true
}

async function openEdit(row) {
  const detail = await mallAdminApi.banner(row.id).catch(() => row)
  Object.assign(form, {
    id: detail.id,
    position: detail.position ?? 1,
    title: detail.title || '',
    subtitle: detail.subtitle || '',
    image: detail.image || '',
    link: detail.link || '',
    targetType: detail.targetType || '',
    targetId: detail.targetId ?? '',
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
  if (!form.title || !form.image) {
    ok('请填写标题并上传图片')
    return
  }
  saving.value = true
  try {
    const body = {
      position: Number(form.position),
      title: form.title,
      subtitle: form.subtitle || '',
      image: form.image,
      link: form.link || '',
      targetType: form.targetType || '',
      targetId: form.targetId === '' ? '' : Number(form.targetId),
      sort: Number(form.sort) || 0,
      status: Number(form.status)
    }
    if (form.id) await mallAdminApi.updateBanner({ ...body, id: form.id })
    else await mallAdminApi.createBanner(body)
    open.value = false
    ok('推荐位已保存')
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  await mallAdminApi.bannerStatus(row.id, Number(row.status) === 1 ? 0 : 1)
  ok('状态已切换')
  load()
}

async function remove(row) {
  await mallAdminApi.removeBanner(row.id)
  ok('推荐位已删除')
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout>
    <template #actions>
      <button class="m-btn m-btn-primary" type="button" @click="openCreate">+ 新增推荐位</button>
    </template>

    <div class="m-table-wrap">
      <table class="m-table">
        <thead>
          <tr>
            <th style="width: 70px">ID</th>
            <th style="width: 130px">位置</th>
            <th>图片 / 文案</th>
            <th>跳转</th>
            <th>排序</th>
            <th>状态</th>
            <th style="width: 200px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="m-table-empty">加载中…</td>
          </tr>
          <tr v-for="row in list" :key="row.id">
            <td class="m-num">{{ row.id }}</td>
            <td>
              <span class="m-badge m-badge-soft">{{ positionLabel(row.position) }}</span>
            </td>
            <td>
              <div class="m-cell">
                <img v-if="row.image" class="banner-thumb" :src="row.image" :alt="row.title" />
                <span v-else class="banner-thumb" />
                <div>
                  <p class="m-h4">{{ row.title }}</p>
                  <p class="m-dim">{{ row.subtitle || '' }}</p>
                </div>
              </div>
            </td>
            <td class="m-dim">{{ row.link || row.targetType || '—' }}</td>
            <td class="m-num">{{ row.sort ?? 0 }}</td>
            <td>
              <MStatus
                :text="Number(row.status) === 1 ? '展示中' : '已隐藏'"
                :tone="Number(row.status) === 1 ? 'emerald' : 'gray'"
              />
            </td>
            <td>
              <div class="m-cell-actions">
                <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openEdit(row)">编辑</button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="toggle(row)">
                  {{ Number(row.status) === 1 ? '隐藏' : '展示' }}
                </button>
                <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="remove(row)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && !list.length">
            <td colspan="7" class="m-table-empty">暂无推荐位，首页轮播与金刚区会是空状态</td>
          </tr>
        </tbody>
      </table>
    </div>

    <MModal :open="open" :title="form.id ? '编辑推荐位' : '新增推荐位'" @close="open = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-field">
          <label class="m-eyebrow">展示位置</label>
          <select v-model.number="form.position" class="m-select">
            <option v-for="p in POSITIONS" :key="p.value" :value="p.value">{{ p.label }}</option>
          </select>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">标题 *</label>
          <input v-model="form.title" class="m-input" placeholder="夏季新品 5 折起" />
        </div>
        <div class="m-field">
          <label class="m-eyebrow">副标题</label>
          <input v-model="form.subtitle" class="m-input" placeholder="每日上新" />
        </div>
        <div class="m-field">
          <span class="m-eyebrow">图片 *</span>
          <MUploader
            :model-value="form.image ? [form.image] : []"
            :uploader="upload"
            :max="1"
            @update:model-value="(v) => (form.image = v[0] || '')"
          />
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">跳转类型</label>
            <select v-model="form.targetType" class="m-select">
              <option value="">不跳转</option>
              <option value="category">分类</option>
              <option value="product">商品</option>
              <option value="url">外链</option>
            </select>
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">跳转参数</label>
            <input v-if="form.targetType === 'url'" v-model="form.link" class="m-input" placeholder="https://…" />
            <input v-else v-model="form.targetId" class="m-input" placeholder="分类 / 商品 ID" />
          </div>
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">排序</label>
            <input v-model.number="form.sort" type="number" class="m-input" />
          </div>
          <div class="m-field m-grow">
            <label class="m-eyebrow">状态</label>
            <select v-model.number="form.status" class="m-select">
              <option :value="1">展示中</option>
              <option :value="0">隐藏</option>
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
.banner-thumb {
  width: 96px;
  height: 52px;
  border-radius: var(--m-r-xs);
  object-fit: cover;
  background: var(--m-bone-deep);
  flex-shrink: 0;
  display: block;
}
</style>
