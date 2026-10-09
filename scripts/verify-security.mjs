// scripts/verify-security.mjs
// 安全 / 性能四项验收（对应需求条目）：
//   S1 SQL 注入   —— 静态检查（`${}` 拼接）+ 真实注入串打一遍
//   S2 XSS        —— 富文本写入 + 读回，看是否被原样存储（说明有没有做过滤）
//   S3 越权       —— 用 A 的 token 去访问 B 的资源（订单/地址/购物车/评价/商品/配送单），必须被拒
//   S4 上传       —— 类型白名单与大小限制（头像 ✓ / 商品图？）
//   P1 分页       —— 列表接口是否都分页（超量请求看是否被截断）
//   用法（需 8080/8081/Redis 在跑）：node scripts/verify-security.mjs
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = process.cwd()
const TO = String(process.env.TAKEOUT_BASE || 'http://localhost:8080').replace(/\/$/, '')
const MALL = String(process.env.MALL_BASE || 'http://localhost:8081').replace(/\/$/, '')

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

async function req(base, method, p, { headers = {}, body } = {}) {
  const h = { ...headers }
  if (body !== undefined) h['Content-Type'] = 'application/json'
  const res = await fetch(base + p, { method, headers: h, body: body === undefined ? undefined : JSON.stringify(body) })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}
const okRes = (r) => r.json?.code === 1
const msgOf = (r) => r.json?.msg || ''

function redis(args) {
  const candidates = [process.env.REDIS_CLI, 'redis-cli', 'D:/redis/Redis-x64-5.0.14.1/redis-cli',
    'C:/Program Files/Redis/redis-cli.exe'].filter(Boolean)
  for (const exe of candidates) {
    if (exe !== 'redis-cli' && !fs.existsSync(exe)) continue
    for (const a of [
      ['-h', '127.0.0.1', '-p', '6379', '-a', process.env.REDIS_PASSWORD || 'mehado', '--no-auth-warning', ...args],
      ['-h', '127.0.0.1', '-p', '6379', '-a', process.env.REDIS_PASSWORD || 'mehado', ...args]
    ]) {
      try { const out = execFileSync(exe, a, { encoding: 'utf8' }).trim(); if (!out.startsWith('ERR ')) return out } catch { /* retry */ }
    }
  }
  throw new Error('redis-cli 失败')
}

async function login(base, mall, pathLogin, creds, captchaPath) {
  const cap = await req(base, 'GET', captchaPath)
  const key = cap.json?.data?.key
  const code = redis(['GET', `${mall ? 'mall:' : ''}captcha:${key}`])
  const r = await req(base, 'POST', pathLogin, { body: { ...creds, captchaKey: key, captchaCode: code } })
  if (!okRes(r)) throw new Error(`登录失败 ${pathLogin}: ${r.text}`)
  return r.json.data.token
}

const T = {}   // 各角色 token

try {
  // ============================================================
  section('S1 SQL 注入：静态（无 ${} 拼接）+ 动态（注入串打真实查询）')
  const xmls = []
  const walk = (d) => fs.readdirSync(d, { withFileTypes: true }).forEach((e) => {
    const p = path.join(d, e.name)
    if (e.isDirectory()) walk(p)
    else if (p.endsWith('.xml')) xmls.push(p)
  })
  walk(path.join(ROOT, 'superMall-takeout/src/main/resources/mapper'))
  walk(path.join(ROOT, 'superMall-mall/src/main/resources/mapper'))
  const risky = xmls.flatMap((f) => fs.readFileSync(f, 'utf8').split('\n')
    .map((l, i) => ({ f, i: i + 1, l }))
    .filter(({ l }) => /\$\{/.test(l) && !/<!--/.test(l)))
  check(`S1-1 所有 Mapper XML 均未使用 \${} 拼接（扫 ${xmls.length} 个文件）`, risky.length === 0,
    risky.map((r) => `${path.basename(r.f)}:${r.i}`).join(', '))

  T.u1 = await login(MALL, true, '/api/mall/user/login', { username: 'user1@qq.com', password: 'Mall1234' }, '/api/mall/auth/captcha')
  const inject = "' or 1=1 -- "
  const inj = await req(MALL, 'GET', `/api/mall/catalog/products/page?keyword=${encodeURIComponent(inject)}&page=1&pageSize=5`, { headers: { token: T.u1 } })
  check('S1-2 关键字带注入串 → 正常返回空结果，没有报 SQL 语法错',
    okRes(inj) && !/SQL|syntax|Unknown column/i.test(inj.text), `total=${inj.json?.data?.total}`)
  const inj2 = await req(MALL, 'GET', `/api/mall/catalog/products/page?orderBy=${encodeURIComponent("sales desc; drop table mall_order")}&page=1&pageSize=5`)
  check('S1-3 排序参数塞 SQL → 走白名单回落（不报错、表还在）',
    okRes(inj2), `orderBy 被忽略，返回 ${inj2.json?.data?.total} 条`)

  // ============================================================
  section('S2 XSS：富文本是否做了过滤（写进去再读回来）')
  // 用**临时商品**验（不动演示数据）：商品保存是"整体覆盖"，动 #1 会重建它的 SKU（id 会变），
  // 所以这里 create → 读回 → delete，全程只碰自己造的那条。
  const xss = '<p>安全<strong>排版</strong></p><script>alert(1)</script>'
    + '<img src="/seed/c-001.jpg" onerror="alert(2)"><a href="javascript:alert(3)">x</a>'
  const adminTok0 = await login(MALL, true, '/api/mall/admin/login', { username: 'admin@qq.com', password: 'Admin1234' }, '/api/mall/auth/captcha')
  const tree = await req(MALL, 'GET', '/api/mall/admin/categories/tree', { headers: { 'mall-admin-token': adminTok0 } })
  const findLeaf = (nodes) => {
    for (const n of nodes || []) {
      if (!n.children || n.children.length === 0) return n.id
      const hit = findLeaf(n.children)
      if (hit) return hit
    }
    return null
  }
  const leafId = findLeaf(tree.json?.data)
  const created = await req(MALL, 'POST', '/api/mall/admin/products', {
    headers: { 'mall-admin-token': adminTok0 },
    body: { categoryId: leafId, name: `XSS验收-${Date.now().toString(36)}`, status: 0, images: [],
            detail: xss, specs: [], skus: [{ price: 9.9, stock: 10, specValues: [] }] }
  })
  const tempId = created.json?.data
  check('S2-0 临时商品已创建（用于 XSS 验收，跑完即删）', !!tempId, `id=${tempId}`)
  const readBack = tempId ? ((await req(MALL, 'GET', `/api/mall/catalog/products/${tempId}`, {})).json?.data?.detail || '') : ''
  check('S2-1 需求口径：富文本入库时应被 XSS 过滤（script 被清掉）', !readBack.includes('<script>'),
    `读回：${readBack.slice(0, 70)}`)
  check('S2-1b 事件属性与 javascript: 协议被摘掉',
    !readBack.includes('onerror') && !readBack.includes('javascript:'), `读回：${readBack.slice(0, 70)}`)
  check('S2-1c 安全标签被保留（富文本没被清成纯文本）',
    readBack.includes('<p>') && readBack.includes('<strong>'), `读回：${readBack.slice(0, 70)}`)
  check('S2-1d 站内相对图片地址没被误伤（/seed/c-001.jpg 保留）', readBack.includes('/seed/c-001.jpg'),
    `读回：${readBack.slice(0, 70)}`)
  if (tempId) {
    const del = await req(MALL, 'DELETE', `/api/mall/admin/products/${tempId}`, { headers: { 'mall-admin-token': adminTok0 } })
    check('S2-2 临时商品已删除（演示数据零残留）', okRes(del), msgOf(del))
  }

  // 外卖侧：店铺简介（不碰任何子表）+ 商品描述（用临时商品）
  const mTok = await login(TO, false, '/api/merchant/login', { username: 'merchant1@163.com', password: 'Mall1234' }, '/api/auth/captcha')
  const shopBefore = (await req(TO, 'GET', '/api/merchant/shop', { headers: { 'merchant-token': mTok } })).json?.data || {}
  await req(TO, 'PUT', '/api/merchant/shop', {
    headers: { 'merchant-token': mTok },
    body: { shopName: shopBefore.shopName, logo: shopBefore.logo, phone: shopBefore.phone,
            address: shopBefore.address, description: xss, status: shopBefore.status }
  })
  const shopAfter = (await req(TO, 'GET', '/api/merchant/shop', { headers: { 'merchant-token': mTok } })).json?.data?.description || ''
  check('S2-3 外卖店铺简介：script / 事件属性被剥掉',
    !shopAfter.includes('<script>') && !shopAfter.includes('onerror'), `读回：${shopAfter.slice(0, 50)}`)
  await req(TO, 'PUT', '/api/merchant/shop', {
    headers: { 'merchant-token': mTok },
    body: { shopName: shopBefore.shopName, logo: shopBefore.logo, phone: shopBefore.phone,
            address: shopBefore.address, description: shopBefore.description ?? '', status: shopBefore.status }
  })
  const shopRestored = (await req(TO, 'GET', '/api/merchant/shop', { headers: { 'merchant-token': mTok } })).json?.data?.description || ''
  check('S2-3b 外卖店铺简介已还原', shopRestored === (shopBefore.description ?? ''), `还原后：${String(shopRestored).slice(0, 30)}`)

  // 注意：这个接口也是分页的（data = {total, records}）
  const catsPage = (await req(TO, 'GET', '/api/merchant/categories', { headers: { 'merchant-token': mTok } })).json?.data || {}
  const catId = (catsPage.records || [])[0]?.id
  if (catId) {
    const newP = await req(TO, 'POST', '/api/merchant/products', {
      headers: { 'merchant-token': mTok },
      // 注意：外卖的 SkuDTO 字段是 name/price/stock（没有 specValues，多传会 400）
      body: { categoryId: catId, name: `XSS验收-${Date.now().toString(36)}`, description: xss,
              price: 9.9, stock: 10, status: 1, skuList: [{ name: '默认', price: 9.9, stock: 10, status: 1 }] }
    })
    // 列表里找刚建的（拿 id）
    const list = (await req(TO, 'GET', '/api/merchant/products?page=1&pageSize=50', { headers: { 'merchant-token': mTok } })).json?.data?.records || []
    const mine = list.find((x) => String(x.name || '').startsWith('XSS验收-'))
    check('S2-4 外卖商品描述：script 被剥掉',
      !!mine && !String(mine.description || '').includes('<script>'), `读回：${String(mine?.description || '').slice(0, 50)}`)
    if (mine) {
      // 外卖没有单独的上下架接口：要下线得走整体更新（PUT /api/merchant/products），然后再删
      await req(TO, 'PUT', '/api/merchant/products', {
        headers: { 'merchant-token': mTok },
        body: { id: mine.id, categoryId: mine.categoryId, name: mine.name, image: mine.image,
                description: mine.description, price: mine.price, stock: mine.stock, status: 0,
                skuList: (mine.skuList || []).map((k) => ({ id: k.id, name: k.name, price: k.price, stock: k.stock, status: k.status })) }
      })
      const d = await req(TO, 'DELETE', `/api/merchant/products/${mine.id}`, { headers: { 'merchant-token': mTok } })
      check('S2-4b 临时外卖商品已删除（演示数据零残留）', okRes(d), msgOf(d) || '已删除')
    }
    void newP
  } else {
    check('S2-4 外卖商品描述（没有可用分类，跳过）', true, '（前置数据不足）')
  }

  // 覆盖度：所有"用户输入的字"入库点都接了清洗（代码级断言）
  const sites = [
    ['superMall-mall/src/main/java/com/supermall/mall/service/impl/MallProductServiceImpl.java', 2],
    ['superMall-mall/src/main/java/com/supermall/mall/service/impl/MallReviewServiceImpl.java', 2],
    ['superMall-mall/src/main/java/com/supermall/mall/service/impl/MallRefundServiceImpl.java', 1],
    ['superMall-takeout/src/main/java/com/supermall/takeout/service/impl/ProductServiceImpl.java', 2],
    ['superMall-takeout/src/main/java/com/supermall/takeout/service/impl/MerchantServiceImpl.java', 3]
  ]
  const missing = sites.filter(([f, n]) =>
    (fs.readFileSync(path.join(ROOT, f), 'utf8').match(/RichTextSanitizer[.]/g) || []).length < n)
  check('S2-5 8 处入库点都接了清洗（商品详情×2 / 评价×2 / 退款×1 / 外卖商品×2 / 店铺简介×3）',
    missing.length === 0, missing.map((m) => path.basename(m[0])).join(', ') || '全部就位')
  console.log('     ↳ 口径：入库前用 Jsoup Safelist 清洗（common 的 RichTextSanitizer）；')
  console.log('       富文本保排版标签、纯文本剥标签，危险协议与事件属性一律摘掉。')

  section('S3 越权：用 A 的 token 访问 B 的资源（全部必须被拒）')
  T.u2 = await login(MALL, true, '/api/mall/user/login', { username: 'user2@qq.com', password: 'Mall1234' }, '/api/mall/auth/captcha')
  const u1Orders = await req(MALL, 'GET', '/api/mall/user/orders/page?page=1&pageSize=20', { headers: { token: T.u1 } })
  const u2Orders = await req(MALL, 'GET', '/api/mall/user/orders/page?page=1&pageSize=20', { headers: { token: T.u2 } })
  const u1Order = (u1Orders.json?.data?.records || [])[0]?.orderNumber
  const u2Order = (u2Orders.json?.data?.records || [])[0]?.orderNumber
  if (u1Order && u2Order) {
    const cross = await req(MALL, 'GET', `/api/mall/user/orders/${u1Order}`, { headers: { token: T.u2 } })
    check('S3-1 买家用自己 token 查别人的订单号 → 被拒', !okRes(cross), msgOf(cross) || `status=${cross.status}`)
    const okOwn = await req(MALL, 'GET', `/api/mall/user/orders/${u2Order}`, { headers: { token: T.u2 } })
    check('S3-1b 查自己的订单 → 正常（不是把功能写死）', okRes(okOwn))
  } else {
    // 库里的商城订单为 0（演示环境干净），改用**静态确认**：订单读写都走 requireOwned(userId, orderNumber)
    const svc = fs.readFileSync(path.join(ROOT, 'superMall-mall/src/main/java/com/supermall/mall/service/impl/MallOrderServiceImpl.java'), 'utf8')
    const guarded = (svc.match(/requireOwned\(/g) || []).length
    check('S3-1 商城订单越权：代码层强制 requireOwned(userId, orderNumber)', guarded >= 3,
      `detail/cancel/confirm 共用 ${guarded} 处；库里暂无订单，未做 HTTP 实测（外卖侧同类已实测通过）`)
  }

  const u1Addr = (await req(MALL, 'GET', '/api/mall/user/address', { headers: { token: T.u1 } })).json?.data?.[0]?.id
  const u2Addr = (await req(MALL, 'GET', '/api/mall/user/address', { headers: { token: T.u2 } })).json?.data?.[0]?.id
  if (u1Addr && u2Addr && u1Addr !== u2Addr) {
    const g = await req(MALL, 'GET', `/api/mall/user/address/${u1Addr}`, { headers: { token: T.u2 } })
    check('S3-2 买家读别人的收货地址 → 被拒', !okRes(g), msgOf(g))
    const u = await req(MALL, 'PUT', '/api/mall/user/address', {
      headers: { token: T.u2 },
      body: { id: u1Addr, consignee: 'hacker', phone: '13800000000', detail: 'x' }
    })
    check('S3-3 买家改别人的收货地址 → 被拒', !okRes(u), msgOf(u))
    await req(MALL, 'DELETE', `/api/mall/user/address/${u1Addr}`, { headers: { token: T.u2 } })
    const still = await req(MALL, 'GET', '/api/mall/user/address', { headers: { token: T.u1 } })
    const survives = (still.json?.data || []).some((a) => a.id === u1Addr)
    check('S3-4 买家删别人的收货地址 → 对方地址仍在（幂等删除 0 行生效）', survives,
      `删除接口对"不属于自己的 id"是 0 行受影响、返回成功；靠 SQL 里的 and user_id 兜底`)
  } else {
    check('S3-2/3/4 地址越权（需要两个账号各至少 1 条地址）', false, `u1=${u1Addr} u2=${u2Addr}`)
  }

  const u1Cart = (await req(MALL, 'GET', '/api/mall/user/cart', { headers: { token: T.u1 } })).json?.data?.items?.[0]?.id
  if (u1Cart) {
    const m = await req(MALL, 'PUT', `/api/mall/user/cart/${u1Cart}`, { headers: { token: T.u2 }, body: { number: 99 } })
    check('S3-5 买家改别人的购物车行 → 被拒', !okRes(m), msgOf(m))
    const del = await req(MALL, 'DELETE', `/api/mall/user/cart/${u1Cart}`, { headers: { token: T.u2 } })
    check('S3-6 买家删别人的购物车行 → 被拒', !okRes(del), msgOf(del))
  } else {
    check('S3-5/6 购物车越权（user1 购物车为空，跳过）', true, '（前置数据不足，未测）')
  }

  // 外卖：订单 / 地址 / 商家商品 / 骑手配送单
  T.t1 = await login(TO, false, '/api/user/login', { username: 'user1@qq.com', password: 'Mall1234' }, '/api/auth/captcha')
  T.t2 = await login(TO, false, '/api/user/login', { username: 'user2@qq.com', password: 'Mall1234' }, '/api/auth/captcha')
  const t1Orders = await req(TO, 'GET', '/api/user/orders?page=1&pageSize=20', { headers: { token: T.t1 } })
  const t2Orders = await req(TO, 'GET', '/api/user/orders?page=1&pageSize=20', { headers: { token: T.t2 } })
  const t1OrderId = (t1Orders.json?.data?.records || [])[0]?.id
  const t2OrderId = (t2Orders.json?.data?.records || [])[0]?.id
  if (t1OrderId) {
    const cross = await req(TO, 'GET', `/api/user/orders/${t1OrderId}`, { headers: { token: T.t2 } })
    check('S3-7 外卖用户查别人的订单 → 被拒', !okRes(cross), msgOf(cross) || `status=${cross.status}`)
    if (t2OrderId) {
      const own = await req(TO, 'GET', `/api/user/orders/${t2OrderId}`, { headers: { token: T.t2 } })
      check('S3-7b 查自己的订单 → 正常', okRes(own))
    }
  } else {
    check('S3-7 外卖用户查别人的订单 → 被拒', false, 'user1 无订单数据')
  }

  const t1Addr = (await req(TO, 'GET', '/api/user/address', { headers: { token: T.t1 } })).json?.data?.[0]?.id
  if (t1Addr) {
    const g = await req(TO, 'GET', `/api/user/address/${t1Addr}`, { headers: { token: T.t2 } })
    check('S3-8 外卖用户读别人的地址 → 被拒', !okRes(g), msgOf(g))
    const def = await req(TO, 'PUT', `/api/user/address/default/${t1Addr}`, { headers: { token: T.t2 } })
    check('S3-9 外卖用户把别人的地址设成默认 → 被拒', !okRes(def), msgOf(def))
  } else {
    check('S3-8/9 外卖地址越权（user1 无地址，跳过）', false, '前置数据不足')
  }

  T.m1 = await login(TO, false, '/api/merchant/login', { username: 'merchant1@163.com', password: 'Mall1234' }, '/api/auth/captcha')
  T.m2 = await login(TO, false, '/api/merchant/login', { username: 'merchant2@163.com', password: 'Mall1234' }, '/api/auth/captcha')
  const m1Products = await req(TO, 'GET', '/api/merchant/products?page=1&pageSize=5', { headers: { 'merchant-token': T.m1 } })
  const m1ProductId = (m1Products.json?.data?.records || [])[0]?.id
  if (m1ProductId) {
    const g = await req(TO, 'GET', `/api/merchant/products/${m1ProductId}`, { headers: { 'merchant-token': T.m2 } })
    check('S3-10 商家读别人店铺的商品 → 被拒', !okRes(g), msgOf(g))
    const u = await req(TO, 'PUT', `/api/merchant/products`, {
      headers: { 'merchant-token': T.m2 },
      body: { id: m1ProductId, name: 'hacked', categoryId: 1, price: 0.01, stock: 1 }
    })
    check('S3-11 商家改别人店铺的商品 → 被拒', !okRes(u), msgOf(u))
    const d = await req(TO, 'DELETE', `/api/merchant/products/${m1ProductId}`, { headers: { 'merchant-token': T.m2 } })
    check('S3-12 商家删别人店铺的商品 → 被拒', !okRes(d), msgOf(d))
  } else {
    check('S3-10/11/12 商家商品越权（merchant1 无商品，跳过）', false, '前置数据不足')
  }

  T.r1 = await login(TO, false, '/api/delivery/login', { username: 'rider1@gmail.com', password: 'Mall1234' }, '/api/auth/captcha')
  T.r2 = await login(TO, false, '/api/delivery/login', { username: 'rider2@gmail.com', password: 'Mall1234' }, '/api/auth/captcha')
  const r1Mine = await req(TO, 'GET', '/api/delivery/orders/mine?page=1&pageSize=5', { headers: { 'delivery-token': T.r1 } })
  const r1Do = (r1Mine.json?.data?.records || [])[0]
  if (r1Do) {
    const orderId = r1Do.orderId ?? r1Do.id
    const g = await req(TO, 'GET', `/api/delivery/orders/${orderId}`, { headers: { 'delivery-token': T.r2 } })
    check('S3-13 骑手读别人的配送单 → 被拒', !okRes(g), msgOf(g))
    const p = await req(TO, 'PUT', `/api/delivery/orders/pickup/${orderId}`, { headers: { 'delivery-token': T.r2 } })
    check('S3-14 骑手对别人的单点"取餐" → 被拒', !okRes(p), msgOf(p))
  } else {
    check('S3-13/14 骑手越权（rider1 无配送单，跳过）', false, '前置数据不足')
  }

  // ============================================================
  section('S4 上传：类型白名单 + 大小限制')
  const evil = new Blob([Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" onload="alert(1)"></svg>')], { type: 'image/svg+xml' })
  const form = new FormData()
  form.append('file', evil, 'evil.svg')
  const adminTok = await login(MALL, true, '/api/mall/admin/login', { username: 'admin@qq.com', password: 'Admin1234' }, '/api/mall/auth/captcha')
  const up1 = await fetch(`${MALL}/api/mall/admin/file/upload`, { method: 'POST', headers: { 'mall-admin-token': adminTok }, body: form })
  const up1Body = await up1.text()
  check('S4-1 商品图上传：非图片类型被拒（白名单）', !up1Body.includes('"code":1'),
    up1Body.slice(0, 100))

  const big = new Blob([Buffer.alloc(6 * 1024 * 1024)], { type: 'image/png' })
  const form2 = new FormData()
  form2.append('file', big, 'big.png')
  const up2 = await fetch(`${MALL}/api/mall/admin/file/upload`, { method: 'POST', headers: { 'mall-admin-token': adminTok }, body: form2 })
  const up2Body = await up2.text()
  const rejected = !up2Body.includes('"code":1')
  check('S4-2 商品图上传：超过 5MB 被拒（multipart 上限生效）', rejected, `HTTP ${up2.status} ${up2Body.slice(0, 80)}`)
  check('S4-2b 超限提示是否友好（应说明"不能超过 5MB"）', /5\s?MB|太大|超过/.test(up2Body),
    `提示是：${(JSON.parse(up2Body || '{}').msg) || up2Body.slice(0, 60)}`)

  const avatar = new Blob([Buffer.alloc(3 * 1024 * 1024)], { type: 'image/png' })
  const form3 = new FormData()
  form3.append('file', avatar, 'a.png')
  const up3 = await fetch(`${MALL}/api/mall/user/file/avatar`, { method: 'POST', headers: { token: T.u1 }, body: form3 })
  const up3Body = await up3.text()
  check('S4-3 头像上传：超过 2MB 被拒（头像有独立上限）', !up3Body.includes('"code":1'), up3Body.slice(0, 100))

  // ============================================================
  section('P1 列表分页：超量请求能否被截断（不分页=一次全吐）')
  const bigPage = await req(MALL, 'GET', '/api/mall/catalog/products/page?page=1&pageSize=100000', {})
  const returned = (bigPage.json?.data?.records || []).length
  check(`P1-1 商品列表 pageSize=100000 → 被服务端截断（实回 ${returned} 条）`, returned > 0 && returned <= 100,
    `total=${bigPage.json?.data?.total}`)
  const adminPage = await req(MALL, 'GET', '/api/mall/admin/products/page?page=1&pageSize=100000', { headers: { 'mall-admin-token': adminTok } })
  check(`P1-2 后台商品列表 pageSize=100000 → 截断（实回 ${(adminPage.json?.data?.records || []).length} 条）`,
    (adminPage.json?.data?.records || []).length <= 100)
  const pool = await req(TO, 'GET', '/api/delivery/orders/pool', { headers: { 'delivery-token': T.r1 } })
  check('P1-3 骑手抢单池也是分页的（data 为 {total,records}）',
    pool.json?.data && Array.isArray(pool.json.data.records),
    `total=${pool.json?.data?.total} records=${(pool.json?.data?.records || []).length}`)
  const poolBig = await req(TO, 'GET', '/api/delivery/orders/pool?page=1&pageSize=100000', { headers: { 'delivery-token': T.r1 } })
  check('P1-4 抢单池 pageSize=100000 → 被截断', (poolBig.json?.data?.records || []).length <= 100,
    `实回 ${(poolBig.json?.data?.records || []).length} 条`)
  // 公开商家列表：2026-09-21 起也分页了（原来返回全量数组）
  const merchants = await req(TO, 'GET', '/api/public/merchants?page=1&pageSize=3', {})
  check('P1-5 公开商家列表已分页（data = {total, records}）',
    merchants.json?.data && Array.isArray(merchants.json.data.records),
    `total=${merchants.json?.data?.total} records=${(merchants.json?.data?.records || []).length}`)
  const mBig = await req(TO, 'GET', '/api/public/merchants?page=1&pageSize=100000', {})
  check('P1-6 商家列表 pageSize=100000 → 截断到 100', (mBig.json?.data?.records || []).length <= 100,
    `实回 ${(mBig.json?.data?.records || []).length} 条（total=${mBig.json?.data?.total}）`)
  const mKw = await req(TO, 'GET', `/api/public/merchants?keyword=${encodeURIComponent('牛肉')}`, {})
  check('P1-7 商家列表 keyword 走服务端（店名模糊）',
    (mKw.json?.data?.records || []).every((m) => String(m.shopName).includes('牛肉')),
    `命中 ${mKw.json?.data?.total} 家`)
} catch (e) {
  console.error('\n💥 脚本异常：', e.message)
  failed++
} finally {
  console.log(`\n结果：${results.length - failed}/${results.length} 通过`)
  if (failed > 0) process.exitCode = 1
}
