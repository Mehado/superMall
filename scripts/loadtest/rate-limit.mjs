// scripts/loadtest/rate-limit.mjs
// 接口限流验证：GET /api/user/orders/token 限制 30 次 / 60 秒（按 IP + 方法）。
// 一次性并发 N 个请求，验证「只有前 30 个放行，其余被限流」。
//
// 用法：
//   node scripts/loadtest/rate-limit.mjs --concurrency 100
//   REDIS_PASSWORD=xxx node scripts/loadtest/rate-limit.mjs   # 指定 Redis 密码（结果可复现）
import { api, isOk, num, arg, captchaFields, redisClearRateLimits } from './lib.mjs'

const concurrency = num('concurrency', 100)
// 账号已改为邮箱，密码需 8 位且含大小写字母与数字
const user = arg('user', 'user1@qq.com')
const password = arg('password', 'Mall1234')
const LIMIT = 30

const cleaned = redisClearRateLimits()

// 登录需要图形验证码：脚本自己拉一张并从 Redis 读答案
const login = await api('POST', '/api/user/login', {
  body: { username: user, password, ...(await captchaFields()) },
})
const token = login.json?.data?.token
if (!token) {
  console.error('❌ 登录失败：', login.text)
  process.exit(1)
}

console.log('目标     : GET /api/user/orders/token')
console.log(`限流规则 : ${LIMIT} 次 / 60 秒（按 IP + 方法）`)
console.log(`并发数   : ${concurrency}`)
console.log(
  cleaned
    ? '限流窗口 : 已重置（读取 REDIS_PASSWORD 成功）'
    : '限流窗口 : 未重置（未设置 REDIS_PASSWORD；60 秒内跑过会影响计数）'
)

const results = await Promise.all(
  Array.from({ length: concurrency }, () => api('GET', '/api/user/orders/token', { token }))
)

let allowed = 0
let limited = 0
let other = 0
for (const r of results) {
  if (isOk(r)) allowed++
  else if ((r.json?.msg || '').includes('过于频繁')) limited++
  else other++
}

console.log(`\n放行 ${allowed} · 限流 ${limited} · 其他失败 ${other}`)
if (allowed <= LIMIT) {
  console.log(`✅ 限流生效：放行数 ${allowed} ≤ 上限 ${LIMIT}`)
} else {
  console.log(`⚠️ 放行数 ${allowed} 超过上限 ${LIMIT}，请检查限流实现/窗口是否被重置`)
  process.exit(1)
}
if (other > 0) {
  console.log('⚠️ 存在非限流失败，请查看后端日志')
}
