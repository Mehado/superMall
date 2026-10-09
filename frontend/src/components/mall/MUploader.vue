<script setup>
import { ref } from 'vue'
import Icon from '@/components/Icon.vue'
import { err } from '@/lib/toast'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  /** 单文件上传函数：file -> url 字符串 */
  uploader: { type: Function, required: true },
  max: { type: Number, default: 9 },
  hint: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const busy = ref(false)
const input = ref(null)

async function onPick(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  const room = props.max - props.modelValue.length
  if (room <= 0) {
    err(`最多上传 ${props.max} 张`)
    return
  }
  busy.value = true
  try {
    const urls = []
    for (const file of files.slice(0, room)) {
      const url = await props.uploader(file)
      urls.push(typeof url === 'string' ? url : url?.url || url?.data || '')
    }
    emit('update:modelValue', [...props.modelValue, ...urls.filter(Boolean)])
  } finally {
    busy.value = false
    if (input.value) input.value.value = ''
  }
}

function removeAt(i) {
  const next = [...props.modelValue]
  next.splice(i, 1)
  emit('update:modelValue', next)
}
</script>

<template>
  <div>
    <div class="m-uploader">
      <div v-for="(url, i) in modelValue" :key="url + i" class="up-item">
        <img :src="url" :alt="`图片${i + 1}`" />
        <button class="up-del" type="button" @click="removeAt(i)">×</button>
      </div>
      <button
        v-if="modelValue.length < max"
        class="up-add"
        type="button"
        :disabled="busy"
        @click="input?.click()"
      >
        <span>
          <Icon name="upload" :size="18" />
          <br />
          {{ busy ? '上传中…' : '上传图片' }}
        </span>
      </button>
    </div>
    <p v-if="hint" class="m-dim m-mt-2">{{ hint }}</p>
    <input ref="input" type="file" accept="image/*" multiple hidden @change="onPick" />
  </div>
</template>
