<script setup>
import { reactive, ref, watch } from 'vue'
import MModal from '@/components/mall/MModal.vue'
import { ok } from '@/lib/toast'

/**
 * 编辑资料弹窗 —— 买家（外卖/电商）/ 骑手 / 平台运营 共用。
 *
 * 字段由 `fields` 描述，不同角色传不同 schema：
 *   买家   [{ key:'nickname', label:'昵称' }, { key:'phone', label:'手机号', phone:true }]
 *   骑手   [{ key:'name', label:'姓名' },     { key:'phone', label:'手机号', phone:true }]
 *   平台   [{ key:'name', label:'姓名' }]
 *
 * 邮箱/账号是登录凭据，**不可修改**，只做只读展示。
 * 保存由父组件注入 `saver`（各端接口不同）。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  /** 当前资料对象 */
  profile: { type: Object, default: () => ({}) },
  /** 保存函数：(payload) -> Promise */
  saver: { type: Function, required: true },
  /** 可编辑字段定义 */
  fields: {
    type: Array,
    default: () => [
      { key: 'nickname', label: '昵称', placeholder: '如：小明', maxlength: 20 },
      { key: 'phone', label: '手机号', placeholder: '11 位手机号', maxlength: 11, phone: true }
    ]
  },
  /** 只读展示的账号字段（取 profile 里的 key） */
  accountKey: { type: String, default: 'username' },
  title: { type: String, default: '编辑资料' }
})
const emit = defineEmits(['close', 'saved'])

const form = reactive({})
const busy = ref(false)

watch(
  () => props.open,
  (v) => {
    if (!v) return
    // 每次打开都用最新资料回填（父组件刚 load 过）
    for (const f of props.fields) {
      form[f.key] = props.profile?.[f.key] ?? ''
    }
  },
  { immediate: true }
)

async function submit() {
  if (busy.value) return
  for (const f of props.fields) {
    if (f.required && !String(form[f.key] ?? '').trim()) {
      ok(`${f.label}不能为空`)
      return
    }
    if (f.phone && form[f.key] && !/^1[3-9][0-9]{9}$/.test(form[f.key])) {
      ok('手机号格式不正确')
      return
    }
  }
  busy.value = true
  try {
    const payload = {}
    for (const f of props.fields) payload[f.key] = form[f.key]
    await props.saver(payload)
    ok('资料已更新')
    emit('saved')
    emit('close')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <MModal :open="open" :title="title" @close="$emit('close')">
    <div class="m-col" style="gap: 16px">
      <div v-for="f in fields" :key="f.key" class="m-field">
        <label class="m-eyebrow">{{ f.label }}</label>
        <input
          v-model="form[f.key]"
          class="m-input"
          :placeholder="f.placeholder || ''"
          :maxlength="f.maxlength || undefined"
          @keydown.enter.prevent="submit"
        />
      </div>
      <div v-if="profile?.[accountKey]" class="m-field">
        <label class="m-eyebrow">登录账号（不可修改）</label>
        <input class="m-input" :value="profile[accountKey]" disabled />
      </div>
    </div>
    <template #footer>
      <button class="m-btn m-btn-soft" type="button" @click="$emit('close')">取消</button>
      <button class="m-btn m-btn-accent" type="button" :disabled="busy" @click="submit">
        {{ busy ? '保存中…' : '保存' }}
      </button>
    </template>
  </MModal>
</template>
