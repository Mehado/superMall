<script setup>
import { computed } from 'vue'
import Icon from '@/components/Icon.vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  count: { type: [Number, String], default: null },
  size: { type: Number, default: 14 },
  showValue: { type: Boolean, default: true }
})

const filled = computed(() => Math.round(Number(props.value) || 0))
</script>

<template>
  <span class="m-stars">
    <Icon
      v-for="i in 5"
      :key="i"
      name="star"
      :size="size"
      filled
      :class="{ off: i > filled }"
    />
    <span v-if="showValue" class="m-stars-num">
      {{ Number(value || 0).toFixed(1) }}<template v-if="count !== null"> · {{ count }}</template>
    </span>
  </span>
</template>
