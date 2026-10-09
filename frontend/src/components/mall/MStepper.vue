<script setup>
import Icon from '@/components/Icon.vue'

const props = defineProps({
  modelValue: { type: Number, default: 1 },
  min: { type: Number, default: 1 },
  max: { type: Number, default: 999 },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'change'])

function set(next) {
  if (props.disabled) return
  const v = Math.min(props.max, Math.max(props.min, next))
  emit('update:modelValue', v)
  emit('change', v)
}
</script>

<template>
  <span class="m-stepper">
    <button type="button" :disabled="disabled || modelValue <= min" @click="set(modelValue - 1)">
      <Icon name="minus" :size="15" />
    </button>
    <span class="m-num">{{ modelValue }}</span>
    <button type="button" :disabled="disabled || modelValue >= max" @click="set(modelValue + 1)">
      <Icon name="plus" :size="15" />
    </button>
  </span>
</template>
