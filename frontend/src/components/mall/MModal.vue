<script setup>
import { watch, onBeforeUnmount } from 'vue'
import Icon from '@/components/Icon.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  wide: { type: Boolean, default: false },
  hideFooter: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])

watch(
  () => props.open,
  (v) => {
    document.body.style.overflow = v ? 'hidden' : ''
  }
)
onBeforeUnmount(() => {
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="m-overlay">
      <div class="m-overlay-mask" @click="emit('close')" />
      <div class="m-modal" :class="{ 'm-modal-wide': wide }">
        <div class="m-modal-head">
          <h3 class="m-h3">{{ title }}</h3>
          <button class="m-close" type="button" @click="emit('close')">
            <Icon name="close" :size="17" />
          </button>
        </div>
        <div class="m-modal-body"><slot /></div>
        <div v-if="!hideFooter" class="m-modal-foot">
          <slot name="footer">
            <button class="m-btn m-btn-soft" type="button" @click="emit('close')">关闭</button>
          </slot>
        </div>
      </div>
    </div>
  </Teleport>
</template>
