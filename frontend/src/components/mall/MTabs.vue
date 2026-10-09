<script setup>
defineProps({
  modelValue: { type: String, default: '' },
  tabs: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <div class="m-tabs">
    <button
      v-for="t in tabs"
      :key="t.key"
      type="button"
      class="m-tab"
      :class="{ active: t.key === modelValue }"
      @click="emit('update:modelValue', t.key)"
    >
      {{ t.label }}
    </button>
  </div>
</template>
