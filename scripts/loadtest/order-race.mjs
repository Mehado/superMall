// scripts/loadtest/order-race.mjs
// 端到端「抢购」压测：造 N 个买家，同时下单抢 M 件库存，验证不超卖。
//
// ⚠️ 重要：/api/user/orders/submit 有 @RateLimit(15/60s) 且按 IP 限流、不认 X-Forwarded-For。
//    单机并发 > 15 时，超出部分会被限流拒绝（这是系统防刷设计，不是 bug）。
//    因此默认 users=15 / stock=10；想验证「100 并发抢 10 件」请用 JUnit 并发测试
//    （StockDeductConcurrencyTest / DbStockDeductConcurrencyTest），它绕过 HTTP 直打 Service/Redis/DB。
//
// 用法：
//   node scripts/loadtest/order-race.mjs                       # 15 买家抢 10 件
//   node scripts/loadtest/order-race.mjs --users 15 --stock 5
//   REDIS_PASSWORD=xxx node scripts/loadtest/order-race.mjs    # 指定 Redis 密码
//
// 注：登录/注册都需要图形验证码，脚本会自行拉取并从 Redis 读取答案（无需后端开测试后门）。
import { api, isOk, arg, num, captchaFields, redisClearRateLimits } from './lib.mjs'

const users = num('users', 15)
const stock = num('stock', 10)
const productId = num('product', 2) // 默认「番茄鸡蛋面」，无 SKU，脚本更简单
const merchantId = num('merchant', 1)
// 账号一律是邮箱，密码需 8 位且含大小写字母与数字
const merchantUser = arg('merchantUser', 'merchant1@163.com')
const password = arg('password', 'Mall1234')
const runId = Date.now().toString(36)

const log = (...a) => console.log(...a)
const die = (msg) => {
  console.error('❌ ' + msg)
  process.exit(1)
}

// 0) 重置限流窗口（可选）
if (redisClearRateLimits()) {
  log('限流窗口已重置')
}

// 1) 商家登录（登录需要图形验证码，答案在 Redis）
const merchantLogin = await api('POST', '/api/merchant/login', {
  body: { username: merchantUser, password, ...(await captchaFields()) },
})
const merchantToken = merchantLogin.json?.data?.token
if (!merchantToken) die('商家登录失败：' + merchantLogin.text)

// 2) 确保商家营业中（否则加购会被拒）
const biz = await api('GET', '/api/merchant/shop/business', { merchantToken })
const originalBiz = biz.json?.data ?? 1
if (originalBiz !== 1) {
  await api('PUT', '/api/merchant/shop/business', {
    merchantToken,
    body: { businessStatus: 1 },
  })
  log(`商家原为休息中，已临时切为营业中（结束后恢复）`)
}

// 3) 读取商品并设置库存
const detail = await api('GET', `/api/merchant/products/${productId}`, { merchantToken })
const product = detail.json?.data
if (!product) die('读取商品失败：' + detail.text)
const originalStock = product.stock
const setStock = (s) =>
  api('PUT', '/api/merchant/products', {
    merchantToken,
    body: {
      id: product.id,
      categoryId: product.categoryId,
      name: product.name,
      image: product.image,
      description: product.description,
      price: product.price,
      stock: s,
      status: product.status,
    },
  })
const setStockRes = await setStock(stock)
if (!isOk(setStockRes)) die('设置库存失败：' + setStockRes.text)
log(`商品 #${productId}「${product.name}」库存 ${originalStock} → ${stock}（已同步 Redis）`)

// 4) 造买家：注册 → 登录 → 地址 → 加购 → 下单令牌
log(`\n准备 ${users} 个买家...`)
const buyers = []
for (let i = 1; i <= users; i++) {
  // 账号必须是邮箱：注册与登录都要求邮箱格式 + 常用后缀
  const username = `lt${runId}${i}@qq.com`
  await api('POST', '/api/user/register', {
    body: {
      username,
      password,
      nickname: `压测${i}`,
      ...(await captchaFields()),
    },
  })
  const loginRes = await api('POST', '/api/user/login', {
    body: { username, password, ...(await captchaFields()) },
  })
  const token = loginRes.json?.data?.token
  const userId = loginRes.json?.data?.id
  if (!token) {
    log(`  · 买家 ${i} 登录失败：${loginRes.json?.msg || loginRes.status}`)
    continue
  }
  await api('POST', '/api/user/address', {
    token,
    body: {
      consignee: `买家${i}`,
      phone: `138${String(10_000_000 + i).slice(0, 8)}`,
      province: '北京市',
      city: '北京市',
      district: '海淀区',
      detail: `压测地址 ${i} 号`,
      label: '公司',
      isDefault: 1,
    },
  })
  const addrList = await api('GET', '/api/user/address', { token })
  const addressId = addrList.json?.data?.[0]?.id
  await api('POST', '/api/user/cart', {
    token,
    body: { merchantId, productId, skuId: 0, number: 1 },
  })
  const tokenRes = await api('GET', '/api/user/orders/token', { token })
  buyers.push({
    username,
    token,
    userId,
    addressId,
    submitToken: tokenRes.json?.data,
    tokenMsg: tokenRes.json?.msg || '',
  })
  if (i % 5 === 0) log(`  · 已准备 ${i}/${users}`)
}

const ready = buyers.filter((b) => b.addressId && b.submitToken)
log(`准备完成：可用买家 ${ready.length}/${users}`)
if (!ready.length) {
  const rateLimited = buyers.filter((b) => (b.tokenMsg || '').includes('过于频繁')).length
  if (rateLimited) {
    die(`所有买家的下单令牌都被限流（${rateLimited}/${users}）。请设置 REDIS_PASSWORD 重置窗口，或等待 60 秒后重试。`)
  }
  die('没有可用买家，检查商家是否营业 / 用户是否可注册')
}

// 5) 并发下单（同一起跑线）
log(`\n⚡ ${ready.length} 个请求同时下单，抢 ${stock} 件库存...`)
const t0 = performance.now()
const results = await Promise.all(
  ready.map((b) =>
    api('POST', '/api/user/orders/submit', {
      token: b.token,
      body: {
        addressId: b.addressId,
        merchantId,
        payMethod: 'MOCK',
        token: b.submitToken,
      },
    })
  )
)
const seconds = (performance.now() - t0) / 1000

const success = []
const outOfStock = []
const rateLimited = []
const other = []
results.forEach((res, i) => {
  const msg = res.json?.msg || ''
  if (isOk(res)) success.push({ buyer: ready[i], orderId: res.json.data?.id })
  else if (msg.includes('库存不足')) outOfStock.push(msg)
  else if (msg.includes('过于频繁')) rateLimited.push(msg)
  else other.push(msg || `HTTP ${res.status}`)
})

log(`  成功下单 : ${success.length}`)
log(`  库存不足 : ${outOfStock.length}`)
log(`  被限流   : ${rateLimited.length}`)
log(`  其他失败 : ${other.length}${other.length ? ' → ' + [...new Set(other)].join(' | ') : ''}`)
log(`  耗时     : ${seconds.toFixed(2)} s`)

// 6) 校验是否超卖
const after = await api('GET', `/api/merchant/products/${productId}`, { merchantToken })
const finalStock = after.json?.data?.stock
const oversold = success.length > stock || finalStock < 0 || finalStock !== stock - success.length
log(`\n库存核对：初始 ${stock} · 成功 ${success.length} · 剩余 ${finalStock}`)
log(
  oversold
    ? '❌ 结果异常：可能超卖或库存不一致'
    : '✅ 未超卖：成功数 ≤ 库存，且 剩余 = 初始 - 成功数'
)

// 7) 清理：取消测试订单（回补库存）+ 恢复原库存 / 营业状态
log('\n清理中...')
for (const s of success) {
  await api('PUT', `/api/user/orders/cancel/${s.orderId}`, { token: s.buyer.token })
}
await setStock(originalStock)
if (originalBiz !== 1) {
  await api('PUT', '/api/merchant/shop/business', {
    merchantToken,
    body: { businessStatus: originalBiz },
  })
}
log(`已取消 ${success.length} 个测试订单，商品库存恢复为 ${originalStock}`)

// 8) 打印测试用户清理 SQL（无删除用户接口，需手动执行）
const ids = buyers.map((b) => b.userId).filter(Boolean)
if (ids.length) {
  const inList = ids.join(',')
  log('\n以下压测用户需手动清理（脚本无法删除账号），SQL：')
  log(`DELETE oi FROM order_item oi JOIN orders o ON oi.order_id = o.id WHERE o.user_id IN (${inList});`)
  log(`DELETE FROM orders      WHERE user_id IN (${inList});`)
  log(`DELETE FROM cart_item   WHERE user_id IN (${inList});`)
  log(`DELETE FROM cart        WHERE user_id IN (${inList});`)
  log(`DELETE FROM address     WHERE user_id IN (${inList});`)
  log(`DELETE FROM sys_user    WHERE id IN (${inList});`)
}

process.exit(oversold ? 1 : 0)
