<script setup>
import { ref } from 'vue'
import Icon from '@/components/Icon.vue'
import { err, ok } from '@/lib/toast'

/**
 * 头像上传（买家「我的」页用；商城端与外卖端共用）。
 *
 * 两步是后端的约定：先上传拿 OSS URL（uploader），再保存到资料（saver）。
 * 前端也做一次类型/大小预校验，避免白传一次大文件。
 */
const props = defineProps({
  /** 当前头像 URL；为空则显示首字母占位 */
  src: { type: String, default: '' },
  /** 首字母占位（昵称首字） */
  initial: { type: String, default: '我' },
  /** 上传函数：file -> Promise<url> */
  uploader: { type: Function, required: true },
  /** 保存函数：url -> Promise<any> */
  saver: { type: Function, required: true },
  size: { type: Number, default: 76 },
  maxMb: { type: Number, default: 2 }
})
const emit = defineEmits(['updated'])

const busy = ref(false)
const input = ref(null)

function pick() {
  if (!busy.value) input.value?.click()
}

async function onPick(event) {
  const file = (event.target.files || [])[0]
  // 立刻清空，否则连续选同一张图不会再触发 change
  if (input.value) input.value.value = ''
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    err('请选择图片文件')
    return
  }
  if (file.size > props.maxMb * 1024 * 1024) {
    err(`头像不能超过 ${props.maxMb}MB`)
    return
  }
  busy.value = true
  try {
    const url = await props.uploader(file)
    const finalUrl = typeof url === 'string' ? url : url?.url || url?.data || ''
    if (!finalUrl) throw new Error('上传失败，请稍后重试')
    await props.saver(finalUrl)
    ok('头像已更新')
    emit('updated', finalUrl)
  } catch {
    /* 错误提示由 request 拦截器 / 上面统一给出 */
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div
    class="avatar-up"
    :style="{ width: `${size}px`, height: `${size}px` }"
    role="button"
    :aria-label="src ? '更换头像' : '上传头像'"
    :aria-busy="busy"
    @click="pick"
    @keydown.enter.prevent="pick"
    @keydown.space.prevent="pick"
  >
    <img v-if="src" :src="src" alt="头像" />
    <span v-else class="avatar-initial">{{ initial || '我' }}</span>
    <span class="avatar-mask">
      <Icon name="upload" :size="16" />
      <em>{{ busy ? '上传中…' : '换头像' }}</em>
    </span>
    <input ref="input" type="file" accept="image/*" hidden @change="onPick" />
  </div>
</template>

<style scoped>
.avatar-up {
  position: relative;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  background: var(--m-accent);
  color: #fff;
  display: grid;
  place-items: center;
  user-select: none;
}
.avatar-up img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar-initial {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  opacity: 0;
  transition: opacity 0.18s var(--m-ease, ease);
}
.avatar-up:hover .avatar-mask,
.avatar-up:focus-visible .avatar-mask {
  opacity: 1;
}
.avatar-mask em {
  font-style: normal;
  font-size: 11px;
  line-height: 1;
}
</style>
