<script setup>
import { onMounted, ref } from 'vue'
import { err } from '@/lib/toast'

const props = defineProps({
  /** 返回 { key, image } 的函数（电商用 /api/mall/auth/captcha，外卖用 /api/auth/captcha） */
  loader: { type: Function, required: true },
  modelValue: { type: String, default: '' },
  captchaKey: { type: String, default: '' },
  label: { type: String, default: '图形验证码' }
})
const emit = defineEmits(['update:modelValue', 'update:captchaKey'])

const image = ref('')
const loading = ref(false)

/** 验证码一次性：无论对错都作废，登录失败后必须再拉一张 */
async function refresh() {
  loading.value = true
  try {
    const data = await props.loader()
    image.value = data?.image || ''
    emit('update:captchaKey', data?.key || '')
    emit('update:modelValue', '')
  } catch (e) {
    err('验证码加载失败，点图重试')
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
defineExpose({ refresh })
</script>

<template>
  <div class="field">
    <label class="label">{{ label }}</label>
    <div class="row">
      <input
        class="input grow"
        placeholder="输入右侧 4 位字符"
        :value="modelValue"
        maxlength="8"
        @input="emit('update:modelValue', $event.target.value)"
      />
      <button
        type="button"
        class="soft"
        style="width: 116px; height: 42px; overflow: hidden; display: grid; place-items: center"
        title="点击刷新验证码"
        @click="refresh"
      >
        <img v-if="image" :src="`data:image/png;base64,${image}`" alt="验证码" style="height: 100%" />
        <span v-else class="muted-xs">{{ loading ? '加载中…' : '点击获取' }}</span>
      </button>
    </div>
    <p class="muted-xs">验证码一次性有效，登录失败请点图重新获取</p>
  </div>
</template>
