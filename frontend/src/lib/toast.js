import { reactive, readonly } from 'vue'

const list = reactive([])
let seed = 0

export const toasts = readonly(list)

/** 全局轻提示：HTTP 层的业务失败、401 都会走这里 */
export function notify(message, type = 'info', duration = 2400) {
  if (!message) return
  const text =
    typeof message === 'string' ? message : String(message?.msg || message?.message || message)
  if (!text) return
  const id = ++seed
  list.push({ id, text, type })
  setTimeout(() => {
    const i = list.findIndex((t) => t.id === id)
    if (i > -1) list.splice(i, 1)
  }, duration)
}

export const ok = (msg) => notify(msg, 'ok')
export const err = (msg) => notify(msg, 'err')
