<script setup>
import { computed } from 'vue'

const props = defineProps({ value: { type: String, default: '' } })

const rules = computed(() => [
  { text: '8-32 位', ok: props.value.length >= 8 && props.value.length <= 32 },
  { text: '含大写字母', ok: /[A-Z]/.test(props.value) },
  { text: '含小写字母', ok: /[a-z]/.test(props.value) },
  { text: '含数字', ok: /\d/.test(props.value) }
])

const score = computed(() => rules.value.filter((r) => r.ok).length)
const tone = computed(() => (score.value <= 2 ? 'var(--red-500)' : score.value === 3 ? 'var(--amber-500)' : 'var(--emerald-600)'))
</script>

<template>
  <div class="mt-2">
    <div class="row" style="gap: 4px">
      <span
        v-for="i in 4"
        :key="i"
        style="height: 4px; flex: 1; border-radius: 99px; transition: background 0.2s"
        :style="{ background: i <= score ? tone : 'var(--gray-200)' }"
      />
    </div>
    <div class="row wrap-flex mt-2" style="gap: 10px">
      <span
        v-for="r in rules"
        :key="r.text"
        class="muted-xs row"
        style="gap: 4px"
        :style="{ color: r.ok ? 'var(--emerald-600)' : 'var(--gray-400)' }"
      >
        {{ r.ok ? '✓' : '·' }} {{ r.text }}
      </span>
    </div>
  </div>
</template>
