<script setup>
import { computed } from 'vue'

/** 把业务状态机的 tone 映射到商城色板 */
const TONES = {
  amber: 'm-badge-warn',
  blue: 'm-badge-info',
  emerald: 'm-badge-ok',
  red: 'm-badge-danger',
  gray: 'm-badge-soft',
  ink: 'm-badge-ink',
  accent: 'm-badge-accent'
}

const props = defineProps({
  text: { type: String, default: '' },
  tone: { type: String, default: 'gray' },
  solid: { type: Boolean, default: false }
})

const cls = computed(() => (props.solid ? 'm-badge-ink' : TONES[props.tone] || 'm-badge-soft'))
</script>

<template>
  <span class="m-badge" :class="cls">{{ text }}</span>
</template>
