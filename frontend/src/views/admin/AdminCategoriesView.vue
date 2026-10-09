<script setup>
import { onMounted, reactive, ref } from 'vue'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
import MModal from '@/components/mall/MModal.vue'
import MStatus from '@/components/mall/MStatus.vue'
import MEmpty from '@/components/mall/MEmpty.vue'
import { mallAdminApi } from '@/api/mall'
import { ok } from '@/lib/toast'

const tree = ref([])
const loading = ref(true)
const open = ref(false)
const saving = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', sort: 0, status: 1, icon: '' })

function flatten(nodes, depth = 0, out = []) {
  ;(nodes || []).forEach((n) => {
    out.push({ ...n, depth })
    if (n.children?.length) flatten(n.children, depth + 1, out)
  })
  return out
}

async function load() {
  loading.value = true
  try {
    tree.value = await mallAdminApi.categoriesTree()
  } finally {
    loading.value = false
  }
}

function openCreate(parent = 0) {
  Object.assign(form, { id: null, parentId: parent || 0, name: '', sort: 0, status: 1, icon: '' })
  open.value = true
}

function openEdit(node) {
  Object.assign(form, {
    id: node.id,
    parentId: node.parentId ?? 0,
    name: node.name,
    sort: node.sort ?? 0,
    status: node.status ?? 1,
    icon: node.icon || ''
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
      parentId: Number(form.parentId) || 0,
      name: form.name,
      sort: Number(form.sort) || 0,
      status: Number(form.status),
      icon: form.icon || ''
    }
    if (form.id) await mallAdminApi.updateCategory({ ...body, id: form.id })
    else await mallAdminApi.createCategory(body)
    open.value = false
    ok('分类已保存')
    load()
  } finally {
    saving.value = false
  }
}

async function remove(node) {
  await mallAdminApi.removeCategory(node.id)
  ok('分类已删除（分类下有商品时会失败）')
  load()
}

onMounted(load)
</script>

<template>
  <WorkbenchLayout>
    <template #actions>
      <button class="m-btn m-btn-soft" type="button" @click="load">刷新</button>
      <button class="m-btn m-btn-primary" type="button" @click="openCreate(0)">+ 新增一级分类</button>
    </template>

    <div class="m-panel">
      <div v-if="loading" class="m-skeleton" style="height: 260px" />
      <div v-else-if="tree.length" class="m-col" style="gap: 8px">
        <div
          v-for="node in flatten(tree)"
          :key="node.id"
          class="cat-row"
          :style="{ marginLeft: node.depth * 22 + 'px' }"
        >
          <div class="m-row m-wrapflex" style="gap: 10px">
            <span class="m-badge m-badge-soft m-num">ID {{ node.id }}</span>
            <span class="m-h4">{{ node.name }}</span>
            <span class="m-dim m-num">排序 {{ node.sort ?? 0 }}</span>
            <MStatus
              :text="Number(node.status) === 1 ? '启用' : '停用'"
              :tone="Number(node.status) === 1 ? 'emerald' : 'gray'"
            />
            <span v-if="node.productCount" class="m-badge m-badge-info m-num">{{ node.productCount }} 件商品</span>
          </div>
          <div class="m-row m-wrapflex" style="gap: 8px">
            <button class="m-btn m-btn-soft m-btn-sm" type="button" @click="openCreate(node.id)">加子分类</button>
            <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="openEdit(node)">编辑</button>
            <button class="m-btn m-btn-outline m-btn-sm" type="button" @click="remove(node)">删除</button>
          </div>
        </div>
      </div>
      <MEmpty image="empty-data" v-else icon="grid" title="暂无分类" desc="点右上角新增一级分类" />
    </div>

    <MModal :open="open" :title="form.id ? '编辑分类' : '新增分类'" @close="open = false">
      <div class="m-col" style="gap: 16px">
        <div class="m-field">
          <label class="m-eyebrow">上级分类</label>
          <select v-model="form.parentId" class="m-select">
            <option :value="0">— 作为一级分类 —</option>
            <option v-for="n in flatten(tree)" :key="n.id" :value="n.id">
              {{ '　'.repeat(n.depth) }}{{ n.name }}
            </option>
          </select>
        </div>
        <div class="m-field">
          <label class="m-eyebrow">分类名称 *</label>
          <input v-model="form.name" class="m-input" placeholder="如：生鲜果蔬" />
        </div>
        <div class="m-row m-wrapflex" style="gap: 12px">
          <div class="m-field m-grow">
            <label class="m-eyebrow">排序值</label>
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
.cat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 12px 16px;
  border: 1px solid var(--m-line);
  border-radius: var(--m-r-sm);
  transition: border-color 0.2s var(--m-ease);
}
.cat-row:hover {
  border-color: var(--m-line-2);
}
</style>
