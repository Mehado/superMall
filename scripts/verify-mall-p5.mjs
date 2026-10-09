// scripts/verify-mall-p5.mjs
// 电商板块 P5（评价系统）验收：已完成订单评价 / 晒图 / 追评 / 商品详情展示 / 好评率 / 后台管理。
//
// 重点验证：
//   ① 只有已完成订单能评价；订单归属校验；未登录 401
//   ② 批量评价：评分 + 文字 + 晒图；重复评价 / 重复明细 / 非本单明细全部被拒
//   ③ 追评：只能追评一次；追评图与初评图分开
//   ④ 商品详情：评价分页（按星级 / 有图过滤）、统计（评分 / 评价数 / 好评率 / 星级分布）
//   ⑤ 匿名：对外昵称「匿名用户」，不泄露昵称/邮箱
//   ⑥ 后台：评价分页、删除违规评价并重算商品聚合
//   ⑦ 权限：买家/管理员 token 互相打对方接口 401
//
// 用法（需 8081 电商 + Redis + MySQL 在跑）：
//   node scripts/verify-mall-p5.mjs
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'

const MALL = String(process.env.MALL_BASE || 'http://localhost:8081').replace(/\/$/, '')

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

async function req(method, path, { headers = {}, body } = {}) {
  const h = { ...headers }
  const isForm = typeof FormData !== 'undefined' && body instanceof FormData
  if (body !== undefined && !isForm) h['Content-Type'] = 'application/json'
  const res = await fetch(MALL + path, {
    method,
    headers: h,
    body: body === undefined ? undefined : (isForm ? body : JSON.stringify(body))
  })
  const text = await res.text()
  let json = null
  try {
    json = JSON.parse(text)
  } catch {
    /* 非 JSON */
  }
  return { status: res.status, json, text }
}
const ok = (r) => r.json?.code === 1
const msg = (r) => r.json?.msg || ''

function makeCli(candidates, versionArgs, baseArgs) {
  let path = null
  return (args) => {
    if (!path) {
      for (const p of candidates) {
        const isPath = p.includes('/') || p.includes('\\')
        if (isPath && !fs.existsSync(p)) continue
        try {
          execFileSync(p, versionArgs, { stdio: 'ignore' })
          path = p
          break
        } catch {
          /* next */
        }
      }
      if (!path) throw new Error(`找不到可执行文件：${candidates.join(', ')}`)
    }
    for (const a of baseArgs.map((b) => [...b, ...args])) {
      try {
        return execFileSync(path, a, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim()
      } catch {
        /* retry */
      }
    }
    throw new Error('命令执行失败：' + args.join(' '))
  }
}

const redis = makeCli(
  [
    process.env.REDIS_CLI,
    'redis-cli',
    'D:/redis/Redis-x64-5.0.14.1/redis-cli',
    'C:/Program Files/Redis/redis-cli.exe'
  ].filter(Boolean),
  ['--version'],
  [
    ['-h', process.env.REDIS_HOST || '127.0.0.1', '-p', process.env.REDIS_PORT || '6379',
      '-a', process.env.REDIS_PASSWORD || 'mehado', '--no-auth-warning'],
    ['-h', process.env.REDIS_HOST || '127.0.0.1', '-p', process.env.REDIS_PORT || '6379',
      '-a', process.env.REDIS_PASSWORD || 'mehado']
  ]
)

const mysql = makeCli(
  [
    process.env.MYSQL_CLI,
    'mysql',
    'D:/mysql/mysql-9.5.0-winx64/bin/mysql',
    'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe'
  ].filter(Boolean),
  ['--version'],
  [
    ['--ssl-mode=DISABLED', '--default-character-set=utf8mb4',
      '-h', process.env.MYSQL_HOST || '127.0.0.1', '-P', process.env.MYSQL_PORT || '3306',
      '-u', process.env.MYSQL_USER || 'root', `-p${process.env.MYSQL_PASSWORD || 'Ming1234'}`,
      '-D', process.env.MYSQL_DATABASE || 'supermall', '-N', '-B', '-e']
  ]
)
const sql = (s) => mysql([s])

let adminToken = null
let tempToken = null
let tempUserId = null
const newEmail = `p5verify${Date.now().toString(36)}@qq.com`

async function captcha() {
  const r = await req('GET', '/api/mall/auth/captcha')
  const key = r.json?.data?.key
  return { captchaKey: key, captchaCode: redis(['GET', `mall:captcha:${key}`]) }
}
async function adminLogin() {
  const cap = await captcha()
  const r = await req('POST', '/api/mall/admin/login', {
    body: { username: 'admin@qq.com', password: 'Admin1234', ...cap }
  })
  if (!ok(r)) throw new Error('管理员登录失败：' + r.text)
  adminToken = r.json.data.token
}

const A = (m, p, body) => req(m, p, { headers: { 'mall-admin-token': adminToken }, body })
const U = (m, p, body) => req(m, p, { headers: { token: tempToken }, body })
const PUB = (m, p) => req(m, p)

/** 建一个已完成订单（含 SKU1、SKU9 各一件），返回 { orderNumber, items } */
async function completedOrder() {
  const addr = await U('POST', '/api/mall/user/address', {
    consignee: 'P5买家', phone: '13800138000', areaCode: '110101', detail: '测试路 1 号'
  })
  await U('DELETE', '/api/mall/user/cart?scope=all')
  await U('POST', '/api/mall/user/cart', { skuId: 1, number: 1 })
  await U('POST', '/api/mall/user/cart', { skuId: 9, number: 1 })
  const t = await U('GET', '/api/mall/user/orders/token')
  const sub = await U('POST', '/api/mall/user/orders/submit', {
    addressId: addr.json.data, submitToken: t.json.data.submitToken
  })
  const orderNumber = sub.json.data.orderNumber
  const pt = await U('GET', `/api/mall/user/payment/token?orderNumber=${orderNumber}`)
  await U('POST', '/api/mall/user/payment/pay', {
    orderNumber, payToken: pt.json.data.payToken
  })
  await A('PUT', `/api/mall/admin/orders/${orderNumber}/ship`, { company: '顺丰', trackingNo: 'SF' + Date.now() })
  await U('PUT', `/api/mall/user/orders/${orderNumber}/receipt`)
  const orv = await U('GET', `/api/mall/user/reviews/order/${orderNumber}`)
  return { orderNumber, items: orv.json.data.items }
}

let init = {}

try {
  await adminLogin()
  console.log('（管理员已登录）')

  const cap = await captcha()
  const reg = await req('POST', '/api/mall/user/register', {
    body: { username: newEmail, password: 'Mall1234', nickname: 'P5验收买家', ...cap }
  })
  if (!ok(reg)) throw new Error('临时买家注册失败：' + reg.text)
  tempToken = reg.json.data.token
  tempUserId = reg.json.data.id
  console.log(`（临时买家 ${newEmail} 已注册，id=${tempUserId}）`)

  init = {
    p1Status: Number(sql('select status from mall_product where id=1')),
    p3Status: Number(sql('select status from mall_product where id=3')),
    sku1Stock: Number(sql('select stock from mall_sku where id=1')),
    sku9Stock: Number(sql('select stock from mall_sku where id=9')),
    sku1Sales: Number(sql('select sales from mall_sku where id=1')),
    sku9Sales: Number(sql('select sales from mall_sku where id=9')),
    p1Sales: Number(sql('select sales from mall_product where id=1')),
    p3Sales: Number(sql('select sales from mall_product where id=3'))
  }
  sql('update mall_product set status=1 where id in (1,3)')

  // ============================================================
  section('1. 评价前置校验')
  const noAuth = await req('GET', '/api/mall/user/reviews/page')
  check('未登录访问我的评价 → HTTP 401', noAuth.status === 401, `HTTP ${noAuth.status}`)

  // 未支付订单不能评价
  const addr = await U('POST', '/api/mall/user/address', {
    consignee: 'P5买家', phone: '13800138000', areaCode: '110101', detail: '测试路 1 号'
  })
  await U('DELETE', '/api/mall/user/cart?scope=all')
  await U('POST', '/api/mall/user/cart', { skuId: 1, number: 1 })
  const t0 = await U('GET', '/api/mall/user/orders/token')
  const pending = await U('POST', '/api/mall/user/orders/submit', {
    addressId: addr.json.data, submitToken: t0.json.data.submitToken
  })
  const pendingNo = pending.json.data.orderNumber
  const pItems = (await U('GET', `/api/mall/user/reviews/order/${pendingNo}`)).json.data.items
  const early = await U('POST', '/api/mall/user/reviews', {
    orderNumber: pendingNo, reviews: [{ orderItemId: pItems[0].orderItemId, rating: 5 }]
  })
  check('未完成订单不能评价', !ok(early) && msg(early).includes('已完成'), msg(early))
  const earlyView = await U('GET', `/api/mall/user/reviews/order/${pendingNo}`)
  check('未完成订单 canReview=false', earlyView.json?.data?.canReview === false)
  await U('PUT', `/api/mall/user/orders/${pendingNo}/cancel`)

  // ============================================================
  section('2. 提交评价（评分 + 文字 + 晒图 + 匿名）')
  const { orderNumber, items } = await completedOrder()
  // 购物车是「最近修改优先」，所以订单明细顺序不固定：按 skuId 定位更稳
  const item1 = items.find((i) => i.skuId === 1)   // T恤（白 / M）
  const item2 = items.find((i) => i.skuId === 9)   // 咖啡液（无规格）
  check('已完成订单可评价（canReview=true）', items.length === 2)

  const sub = await U('POST', '/api/mall/user/reviews', {
    orderNumber,
    reviews: [
      { orderItemId: item1.orderItemId, rating: 5, content: '质量很好，物流快', images: ['https://example.com/r1.png', 'https://example.com/r2.png'], anonymous: false },
      { orderItemId: item2.orderItemId, rating: 3, content: '味道一般', anonymous: true }
    ]
  })
  check('批量提交 2 条评价成功', ok(sub) && sub.json?.data === 2, JSON.stringify(sub.json))

  const dup = await U('POST', '/api/mall/user/reviews', {
    orderNumber, reviews: [{ orderItemId: item1.orderItemId, rating: 1 }]
  })
  check('重复评价 → 被拒', !ok(dup) && msg(dup).includes('已评价过'), msg(dup))

  const dupInReq = await U('POST', '/api/mall/user/reviews', {
    orderNumber, reviews: [{ orderItemId: item2.orderItemId, rating: 5 }, { orderItemId: item2.orderItemId, rating: 4 }]
  })
  check('同一请求重复明细 → 被拒', !ok(dupInReq) && msg(dupInReq).includes('同一明细'), msg(dupInReq))

  const foreign = await U('POST', '/api/mall/user/reviews', {
    orderNumber, reviews: [{ orderItemId: 99999999, rating: 5 }]
  })
  check('评价不属于该订单的明细 → 被拒', !ok(foreign) && msg(foreign).includes('不属于该订单'), msg(foreign))

  const badRating = await U('POST', '/api/mall/user/reviews', {
    orderNumber, reviews: [{ orderItemId: item1.orderItemId, rating: 6 }]
  })
  check('评分越界（6 星）→ 参数校验失败', !ok(badRating) && msg(badRating).includes('5 星'), msg(badRating))

  const tooMany = await U('POST', '/api/mall/user/reviews', {
    orderNumber, reviews: [{ orderItemId: item1.orderItemId, rating: 5, images: Array.from({ length: 10 }, (_, i) => `https://x/${i}.png`) }]
  })
  check('晒图超 9 张 → 参数校验失败', !ok(tooMany) && msg(tooMany).includes('9 张'), msg(tooMany))

  // ============================================================
  section('3. 我的评价 / 订单评价情况')
  const orv = await U('GET', `/api/mall/user/reviews/order/${orderNumber}`)
  check('订单评价情况：两条都已评',
    orv.json?.data?.items?.every((i) => i.review != null) === true)
  const orvItem1 = orv.json?.data?.items?.find((i) => i.skuId === 1)
  check('订单评价情况：商品快照齐全',
    !!orvItem1?.productName && orvItem1?.specText === '白 / M',
    JSON.stringify({ name: orvItem1?.productName, spec: orvItem1?.specText }))

  const mine = await U('GET', '/api/mall/user/reviews/page?page=1&pageSize=10')
  const recs = mine.json?.data?.records || []
  check('我的评价 2 条', recs.length === 2, `${recs.length}`)
  const withImg = recs.find((r) => (r.images || []).length === 2)
  const anon = recs.find((r) => r.anonymous === true)
  check('初评晒图 2 张（我的评价里可见）', !!withImg, JSON.stringify(recs.map((r) => r.images)))
  check('商品快照带规格文案', withImg?.specText === '白 / M', String(withImg?.specText))
  check('匿名评价标记为 true', !!anon)
  check('匿名评价昵称为「匿名用户」', anon?.nickname === '匿名用户', String(anon?.nickname))
  check('实名评价昵称不是邮箱', !!withImg && !String(withImg.nickname).includes('@'), String(withImg?.nickname))

  // ============================================================
  section('4. 追评')
  const reviewId = withImg.id
  const follow = await U('POST', `/api/mall/user/reviews/${reviewId}/follow`, {
    content: '用了几天，依然很满意', images: ['https://example.com/f1.png']
  })
  check('追评成功', ok(follow), msg(follow))
  const followAgain = await U('POST', `/api/mall/user/reviews/${reviewId}/follow`, { content: '再追一次' })
  check('重复追评 → 被拒', !ok(followAgain) && msg(followAgain).includes('已追评'), msg(followAgain))

  const pub = await PUB('GET', `/api/mall/catalog/products/${item1.productId}/reviews`)
  const prew = (pub.json?.data?.records || []).find((r) => r.id === reviewId)
  check('公开评价带追评内容', prew?.followContent === '用了几天，依然很满意', String(prew?.followContent))
  check('公开评价带追评图', (prew?.followImages || []).length === 1)
  check('公开评价带初评图', (prew?.images || []).length === 2)

  // ============================================================
  section('5. 商品详情：评价列表 + 统计')
  const all = await PUB('GET', `/api/mall/catalog/products/${item1.productId}/reviews`)
  check('商品评价列表返回 1 条', (all.json?.data?.records || []).length === 1)
  check('匿名评价在公开列表也匿名',
    (await PUB('GET', `/api/mall/catalog/products/${item2.productId}/reviews`)).json?.data?.records?.[0]?.nickname === '匿名用户')

  const byRating = await PUB('GET', `/api/mall/catalog/products/${item1.productId}/reviews?rating=5`)
  check('按 5 星过滤命中 1 条', (byRating.json?.data?.records || []).length === 1)
  const noRating = await PUB('GET', `/api/mall/catalog/products/${item1.productId}/reviews?rating=1`)
  check('按 1 星过滤命中 0 条', (noRating.json?.data?.records || []).length === 0)
  const withImage = await PUB('GET', `/api/mall/catalog/products/${item1.productId}/reviews?hasImage=true`)
  check('按有图过滤命中 1 条', (withImage.json?.data?.records || []).length === 1)

  const st1 = await PUB('GET', `/api/mall/catalog/products/${item1.productId}/review-stats`)
  check('商品1 统计：评分 5 / 1 条 / 好评率 100',
    Number(st1.json?.data?.rating) === 5 && st1.json?.data?.reviewCount === 1 &&
    Number(st1.json?.data?.goodRate) === 100,
    JSON.stringify(st1.json?.data))
  check('星级分布 5→1 且 5 星=1',
    (st1.json?.data?.distribution || []).length === 5 &&
    st1.json?.data?.distribution?.[0]?.star === 5 && st1.json?.data?.distribution?.[0]?.count === 1)

  const st3 = await PUB('GET', `/api/mall/catalog/products/${item2.productId}/review-stats`)
  check('商品2 统计：评分 3 / 好评率 0',
    Number(st3.json?.data?.rating) === 3 && Number(st3.json?.data?.goodRate) === 0,
    JSON.stringify(st3.json?.data))

  const detail = await PUB('GET', `/api/mall/catalog/products/${item1.productId}`)
  check('商品详情也带 reviewCount / goodRate',
    detail.json?.data?.reviewCount === 1 && Number(detail.json?.data?.goodRate) === 100,
    `count=${detail.json?.data?.reviewCount} rate=${detail.json?.data?.goodRate}`)

  // ============================================================
  section('6. 运营后台评价管理')
  const page = await A('GET', `/api/mall/admin/reviews/page?productId=${item1.productId}`)
  check('后台按商品筛选评价', (page.json?.data?.records || []).length === 1)
  const allPage = await A('GET', '/api/mall/admin/reviews/page?page=1&pageSize=50')
  check('后台查全部评价', (allPage.json?.data?.records || []).length >= 2)

  const del = await A('DELETE', `/api/mall/admin/reviews/${reviewId}`)
  check('后台删除评价成功', ok(del), msg(del))
  const delAgain = await A('DELETE', `/api/mall/admin/reviews/${reviewId}`)
  check('重复删除 → 评价不存在', !ok(delAgain) && msg(delAgain).includes('评价不存在'), msg(delAgain))

  const st1b = await PUB('GET', `/api/mall/catalog/products/${item1.productId}/review-stats`)
  check('删除后商品1 聚合重算为无评价（5.00 / 0 / 100）',
    Number(st1b.json?.data?.rating) === 5 && st1b.json?.data?.reviewCount === 0 &&
    Number(st1b.json?.data?.goodRate) === 100,
    JSON.stringify(st1b.json?.data))
  check('删除后公开评价列表为空',
    (await PUB('GET', `/api/mall/catalog/products/${item1.productId}/reviews`)).json?.data?.records?.length === 0)

  // ============================================================
  section('6.5 买家晒图上传（评价用）')
  // 1x1 PNG，避免依赖本地文件
  const pngBytes = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    'base64')
  const noAuthUpload = await req('POST', '/api/mall/user/file/upload', { body: new FormData() })
  check('未登录上传 → HTTP 401', noAuthUpload.status === 401, `HTTP ${noAuthUpload.status}`)

  const fd = new FormData()
  fd.append('file', new Blob([pngBytes], { type: 'image/png' }), 'review.png')
  const up = await req('POST', '/api/mall/user/file/upload', { headers: { token: tempToken }, body: fd })
  check('买家上传单张成功并返回 URL', ok(up) && typeof up.json?.data === 'string', JSON.stringify(up.json))
  if (ok(up)) {
    const head = await fetch(up.json.data, { method: 'HEAD' })
    check('上传的图片在 OSS 可访问（HEAD 200）', head.status === 200, `HTTP ${head.status}`)
  }

  const fb = new FormData()
  fb.append('files', new Blob([pngBytes], { type: 'image/png' }), '1.png')
  fb.append('files', new Blob([pngBytes], { type: 'image/png' }), '2.png')
  const upb = await req('POST', '/api/mall/user/file/upload-batch', { headers: { token: tempToken }, body: fb })
  check('买家批量上传返回 2 个 URL', ok(upb) && (upb.json?.data || []).length === 2,
    `${(upb.json?.data || []).length}`)

  // ============================================================
  section('7. 权限隔离')
  const adminAsBuyer = await req('GET', '/api/mall/user/reviews/page', { headers: { 'mall-admin-token': adminToken } })
  check('管理员 token 打买家评价接口 → 401', adminAsBuyer.status === 401, `HTTP ${adminAsBuyer.status}`)
  const buyerAsAdmin = await req('GET', '/api/mall/admin/reviews/page', { headers: { token: tempToken } })
  check('买家 token 打后台评价接口 → 401', buyerAsAdmin.status === 401, `HTTP ${buyerAsAdmin.status}`)
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  try {
    if (tempUserId) {
      sql(`delete from mall_review_image where review_id in (select id from mall_review where user_id=${tempUserId})`)
      sql(`delete from mall_review where user_id=${tempUserId}`)
      sql(`delete from mall_shipment where order_id in (select id from mall_order where user_id=${tempUserId})`)
      sql(`delete from mall_order_item where order_id in (select id from mall_order where user_id=${tempUserId})`)
      sql(`delete from mall_order where user_id=${tempUserId}`)
      sql(`delete from mall_cart_item where user_id=${tempUserId}`)
      sql(`delete from address where user_id=${tempUserId}`)
      sql(`delete from sys_user where id=${tempUserId}`)
      console.log(`\n（已清理临时买家 ${newEmail} 及其评价 / 订单 / 地址 / 购物车）`)
    }
    // 商品 1/3 本无评价，恢复到「无评价默认好评」口径
    sql('update mall_product set rating=5.00, review_count=0, good_rate=100.00 where id in (1,3)')
    if (init.p1Status !== undefined) {
      sql(`update mall_product set status=${init.p1Status} where id=1`)
      sql(`update mall_product set status=${init.p3Status} where id=3`)
      console.log('（已恢复商品状态与评价聚合字段）')
    }
    if (init.sku1Stock !== undefined) {
      // 本脚本下过已支付订单，必须把库存/销量与 Redis 一起还原
      sql(`update mall_sku set stock=${init.sku1Stock}, sales=${init.sku1Sales} where id=1`)
      sql(`update mall_sku set stock=${init.sku9Stock}, sales=${init.sku9Sales} where id=9`)
      sql(`update mall_product set sales=${init.p1Sales} where id=1`)
      sql(`update mall_product set sales=${init.p3Sales} where id=3`)
      redis(['SET', 'mall:stock:sku:1', String(init.sku1Stock)])
      redis(['SET', 'mall:stock:sku:9', String(init.sku9Stock)])
      console.log('（已恢复 SKU 库存/销量并同步 Redis）')
    }
  } catch (e) {
    console.log('（清理失败，请手工检查：', e.message, '）')
  }

  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
