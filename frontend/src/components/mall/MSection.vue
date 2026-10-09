<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  desc: { type: String, default: '' }
})
</script>

<template>
  <div class="m-section-head">
    <div class="m-section-title">
      <span v-if="eyebrow" class="m-eyebrow">{{ eyebrow }}</span>
      <h2 class="m-h2">{{ title }}</h2>
      <p v-if="desc" class="m-muted" style="font-size: 13px">{{ desc }}</p>
    </div>
    <slot name="right" />
  </div>
</template>
