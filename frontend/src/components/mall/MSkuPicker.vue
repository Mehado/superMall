<script setup>
import { computed } from 'vue'
import { money } from '@/utils/format'

const props = defineProps({
  specs: { type: Array, default: () => [] },
  skus: { type: Array, default: () => [] },
  modelValue: { type: [Number, String, null], default: null }
})
const emit = defineEmits(['update:modelValue'])

/** skus[].specValues 与 specs[] 顺序一一对应 */
const picked = computed(() => {
  const sku = props.skus.find((s) => String(s.id) === String(props.modelValue))
  return sku?.specValues || []
})

const currentSku = computed(
  () => props.skus.find((s) => String(s.id) === String(props.modelValue)) || null
)

function match(next) {
  return props.skus.find(
    (s) =>
      Array.isArray(s.specValues) &&
      s.specValues.length === next.length &&
      s.specValues.every((v, i) => v === next[i])
  )
}

/** 选不中的规格值置灰 */
function disabled(index, value) {
  const next = [...picked.value]
  next[index] = value
  for (let i = 0; i < next.length; i++) if (next[i] === undefined) next[i] = ''
  return !match(next)
}

function choose(index, value) {
  const next = [...picked.value]
  while (next.length < props.specs.length) next.push('')
  next[index] = next[index] === value ? '' : value
  const hit = match(next)
  if (hit) {
    emit('update:modelValue', hit.id)
    return
  }
  const partial = props.skus.filter((s) =>
    next.every((v, i) => !v || s.specValues?.[i] === v)
  )
  emit('update:modelValue', partial.length === 1 ? partial[0].id : null)
}
</script>

<template>
  <div class="m-col" style="gap: 18px">
    <div v-for="(spec, index) in specs" :key="spec.name" class="m-col" style="gap: 10px">
      <span class="m-eyebrow">{{ spec.name }}</span>
      <div class="m-row m-wrapflex" style="gap: 8px">
        <button
          v-for="value in spec.values"
          :key="value"
          type="button"
          class="m-chip"
          :class="{ 'm-chip-active': picked[index] === value }"
          :disabled="disabled(index, value)"
          @click="choose(index, value)"
        >
          {{ value }}
        </button>
      </div>
    </div>

    <div v-if="specs.length" class="m-soft" style="padding: 14px 16px">
      <template v-if="currentSku">
        <div class="m-between">
          <span class="m-muted" style="font-size: 13px">已选</span>
          <span style="font-size: 13px; font-weight: 600">{{ currentSku.specValues.join(' / ') }}</span>
        </div>
        <div class="m-between m-mt-2">
          <span class="m-muted" style="font-size: 13px">库存</span>
          <span class="m-num" style="font-size: 13px">{{ currentSku.stock ?? '—' }}</span>
        </div>
      </template>
      <p v-else class="m-dim">请选择完整规格</p>
    </div>
  </div>
</template>
