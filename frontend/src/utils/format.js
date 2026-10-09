/* 金额 / 时间 / 状态映射：两端的时间格式坑都在这里收敛 */

export function money(v, prefix = '¥') {
  const n = Number(v)
  if (!Number.isFinite(n)) return `${prefix}0.00`
  return `${prefix}${n.toFixed(2)}`
}

export function num(v, fallback = 0) {
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

function pad(n) {
  return String(n).padStart(2, '0')
}

/** 请求体用：yyyy-MM-dd HH:mm（电商接口带秒会报「请求体格式错误」） */
export function toBodyTime(value) {
  if (!value) return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}`
}

/** 查询参数用：yyyy-MM-dd HH:mm:ss（外卖后端要求必须带秒） */
export function toQueryTime(value) {
  if (!value) return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return `${toBodyTime(d)}:${pad(d.getSeconds())}`
}

export function shortTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function countdownText(expireTime) {
  if (!expireTime) return ''
  const end = new Date(String(expireTime).replace(/-/g, '/')).getTime()
  if (!Number.isFinite(end)) return ''
  const diff = end - Date.now()
  if (diff <= 0) return '已超时'
  return `${pad(Math.floor(diff / 60000))}:${pad(Math.floor((diff % 60000) / 1000))}`
}

/* ── 电商订单 / 支付 / 退款 ─────────────────────────────── */
export const MALL_ORDER_STATUS = {
  PENDING_PAYMENT: { text: '待付款', tone: 'amber', hint: '请在 15 分钟内完成支付，超时自动取消' },
  PENDING_DELIVERY: { text: '待发货', tone: 'blue', hint: '商家正在备货' },
  PENDING_RECEIPT: { text: '待收货', tone: 'blue', hint: '商品已发出，请注意查收' },
  COMPLETED: { text: '已完成', tone: 'emerald', hint: '交易完成，欢迎评价' },
  CANCELLED: { text: '已取消', tone: 'gray', hint: '订单已关闭' },
  REFUNDING: { text: '退款中', tone: 'red', hint: '退款申请审核中' },
  REFUNDED: { text: '已退款', tone: 'gray', hint: '退款已完成' }
}

export const MALL_REFUND_STATUS = {
  PENDING: { text: '待审核', tone: 'amber' },
  APPROVED: { text: '已同意', tone: 'emerald' },
  REJECTED: { text: '已驳回', tone: 'red' }
}

export const MALL_REFUND_TYPE = {
  1: '仅退款',
  2: '退货退款'
}

/* ── 外卖订单 / 配送单 ─────────────────────────────────── */
export const FOOD_ORDER_STATUS = {
  PENDING_PAYMENT: { text: '待支付', tone: 'amber' },
  PAID: { text: '待接单', tone: 'amber' },
  MERCHANT_ACCEPTED: { text: '备餐中', tone: 'blue' },
  WAITING_DELIVERY: { text: '待配送', tone: 'blue' },
  DELIVERING: { text: '配送中', tone: 'blue' },
  COMPLETED: { text: '已完成', tone: 'emerald' },
  CANCELLED: { text: '已取消', tone: 'gray' }
}

export const DELIVERY_STATUS = {
  WAITING: { text: '待抢单', tone: 'amber' },
  ACCEPTED: { text: '待取餐', tone: 'blue' },
  DELIVERING: { text: '配送中', tone: 'blue' },
  DELIVERED: { text: '已送达', tone: 'emerald' }
}

export const BUSINESS_TYPE = { 1: '外卖食品', 2: '百货商品' }

export function statusOf(map, key, fallback) {
  return map[key] || fallback || { text: '未知', tone: 'gray' }
}

/** 商品图可能为 null（百货 4 个种子商品），统一占位 */
export function imageOf(src) {
  return src || ''
}
