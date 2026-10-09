<script setup>
import { computed } from 'vue'
import Icon from '@/components/Icon.vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})
const emit = defineEmits(['change'])

const pages = computed(() => Math.max(1, Math.ceil((props.total || 0) / (props.pageSize || 10))))

const windowed = computed(() => {
  const last = pages.value
  const from = Math.max(1, Math.min(props.page - 2, last - 4))
  const to = Math.min(last, from + 4)
  const list = []
  for (let i = from; i <= to; i++) list.push(i)
  return list
})

function go(p) {
  if (p < 1 || p > pages.value || p === props.page) return
  emit('change', p)
}
</script>

<template>
  <div v-if="pages > 1" class="m-pager">
    <button :disabled="page <= 1" @click="go(page - 1)"><Icon name="left" :size="14" /></button>
    <button
      v-for="p in windowed"
      :key="p"
      :class="{ active: p === page }"
      @click="go(p)"
    >
      {{ p }}
    </button>
    <button :disabled="page >= pages" @click="go(page + 1)"><Icon name="right" :size="14" /></button>
    <span class="m-pager-total">共 {{ total }} 条</span>
  </div>
</template>
