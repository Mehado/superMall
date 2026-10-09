// scripts/loadtest/feed-load.mjs
// 首页商品流压测：GET /api/public/products（公共接口，无限流）——测吞吐与延迟分布。
//
// 用法：
//   node scripts/loadtest/feed-load.mjs --concurrency 50 --requests 2000
//   node scripts/loadtest/feed-load.mjs --base http://localhost:8080 --type 2
import { api, isOk, num, report } from './lib.mjs'

const concurrency = num('concurrency', 50)
const total = num('requests', 2000)
const type = num('type', 1)
const limit = num('limit', 6)
const url = `/api/public/products?businessType=${type}&limit=${limit}`

const latencies = []
const reasons = new Map()
let ok = 0
let fail = 0
let issued = 0

async function worker() {
  while (true) {
    if (issued++ >= total) return
    const r = await api('GET', url)
    latencies.push(r.ms)
    if (isOk(r)) {
      ok++
    } else {
      fail++
      const key = r.json?.msg || (r.text || `HTTP ${r.status}`).slice(0, 60)
      reasons.set(key, (reasons.get(key) || 0) + 1)
    }
  }
}

console.log(`压测目标 : GET ${url}`)
console.log(`并发 ${concurrency} · 总请求 ${total}`)

const t0 = performance.now()
await Promise.all(Array.from({ length: concurrency }, worker))
report({
  title: '首页商品流压测',
  latencies,
  ok,
  fail,
  seconds: (performance.now() - t0) / 1000,
  reasons,
})
