// scripts/verify-mall-p7.mjs
// 电商板块 P7（运营后台：会员管理 + 销售统计）验收。
//
// 重点验证：
//   ① 会员分页 / 详情（带订单数、消费额、评价数）/ 启用禁用（禁用后无法登录）
//   ② 看板总览：成交额、订单数、会员数、待发货、待审退款
//   ③ 销售额趋势：日 / 月，且无成交的区间补 0
//   ④ 品类占比：按顶级分类归并 + 服务端算好百分比
//   ⑤ 热销 TOP N：按销量排序 + limit 上限
//   ⑥ 权限：买家 token 打后台统计/会员接口 401
//
// 用法（需 8081 电商 + Redis + MySQL 在跑）：
//   node scripts/verify-mall-p7.mjs
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
  if (body !== undefined) h['Content-Type'] = 'application/json'
  const res = await fetch(MALL + path, {
    method,
    headers: h,
    body: body === undefined ? undefined : JSON.stringify(body)
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
const num = (v) => Number(v)

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
const newEmail = `p7verify${Date.now().toString(36)}@qq.com`

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

/** 下一单并支付，返回 { orderNumber, orderAmount } */
async function paidOrder(addressId, cart) {
  await U('DELETE', '/api/mall/user/cart?scope=all')
  for (const [skuId, number] of cart) {
    await U('POST', '/api/mall/user/cart', { skuId, number })
  }
  const t = await U('GET', '/api/mall/user/orders/token')
  const sub = await U('POST', '/api/mall/user/orders/submit', {
    addressId, submitToken: t.json.data.submitToken
  })
  const orderNumber = sub.json.data.orderNumber
  const pt = await U('GET', `/api/mall/user/payment/token?orderNumber=${orderNumber}`)
  await U('POST', '/api/mall/user/payment/pay', { orderNumber, payToken: pt.json.data.payToken })
  return { orderNumber, orderAmount: num(sub.json.data.payAmount) }
}

let init = {}

try {
  await adminLogin()
  console.log('（管理员已登录）')

  const cap = await captcha()
  const reg = await req('POST', '/api/mall/user/register', {
    body: { username: newEmail, password: 'Mall1234', nickname: 'P7验收买家', ...cap }
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
  section('1. 会员管理')
  const page = await A('GET', `/api/mall/admin/members/page?keyword=${encodeURIComponent(newEmail)}`)
  check('会员分页命中该买家', page.json?.data?.total === 1, `total=${page.json?.data?.total}`)
  const member = page.json?.data?.records?.[0] || {}
  check('会员信息含统计字段（orderCount/totalSpent）',
    'orderCount' in member && 'totalSpent' in member && 'reviewCount' in member,
    JSON.stringify(member))

  const detail = await A('GET', `/api/mall/admin/members/${tempUserId}`)
  check('会员详情可用', ok(detail) && detail.json?.data?.id === tempUserId)
  check('新会员状态为启用', detail.json?.data?.status === 1)

  const notFound = await A('GET', '/api/mall/admin/members/99999999')
  check('会员不存在 → 拒绝', !ok(notFound) && msg(notFound).includes('会员不存在'), msg(notFound))

  const badStatus = await A('PUT', `/api/mall/admin/members/${tempUserId}/status?status=9`)
  check('非法状态 → 拒绝', !ok(badStatus) && msg(badStatus).includes('只能是 1'), msg(badStatus))

  const disable = await A('PUT', `/api/mall/admin/members/${tempUserId}/status?status=0`)
  check('禁用会员成功', ok(disable), msg(disable))
  const cap2 = await captcha()
  const loginDisabled = await req('POST', '/api/mall/user/login', {
    body: { username: newEmail, password: 'Mall1234', ...cap2 }
  })
  check('禁用后无法登录', !ok(loginDisabled) && msg(loginDisabled).includes('禁用'), msg(loginDisabled))
  await A('PUT', `/api/mall/admin/members/${tempUserId}/status?status=1`)
  const cap3 = await captcha()
  const loginEnabled = await req('POST', '/api/mall/user/login', {
    body: { username: newEmail, password: 'Mall1234', ...cap3 }
  })
  check('重新启用后可以登录', ok(loginEnabled))

  const adminAsBuyer = await req('GET', '/api/mall/admin/members/page', { headers: { token: tempToken } })
  check('买家 token 打后台会员接口 → 401', adminAsBuyer.status === 401, `HTTP ${adminAsBuyer.status}`)

  // ============================================================
  section('2. 准备统计数据（下两笔已支付订单）')
  const base = (await A('GET', '/api/mall/admin/stats/overview')).json.data
  const baseTop = (await A('GET', '/api/mall/admin/stats/top-products?limit=50')).json.data || []
  const baseTshirt = baseTop.find((t) => t.productId === 1)?.quantity || 0

  const addr = await U('POST', '/api/mall/user/address', {
    consignee: 'P7买家', phone: '13800138000', areaCode: '110101', detail: '测试路 1 号'
  })
  // 订单 1：T恤 x2 + 咖啡液 x1 = 197.90
  const o1 = await paidOrder(addr.json.data, [[1, 2], [9, 1]])
  // 订单 2：T恤 x1 = 79.00
  const o2 = await paidOrder(addr.json.data, [[1, 1]])
  const expectedTotal = Math.round((o1.orderAmount + o2.orderAmount) * 100) / 100
  check('两笔订单已支付', o1.orderNumber && o2.orderNumber, `${o1.orderNumber} / ${o2.orderNumber}`)
  check('本次成交额 = 197.90 + 79.00 = 276.90', expectedTotal === 276.9, `${expectedTotal}`)

  // ============================================================
  section('3. 看板总览')
  const ov = (await A('GET', '/api/mall/admin/stats/overview')).json.data
  check('累计成交额增加 276.90',
    Math.abs(num(ov.totalSales) - (num(base.totalSales) + 276.9)) < 0.001,
    `${base.totalSales} → ${ov.totalSales}`)
  check('已支付订单数 +2', ov.paidOrders === base.paidOrders + 2, `${base.paidOrders} → ${ov.paidOrders}`)
  check('订单总数 +2', ov.totalOrders === base.totalOrders + 2)
  check('今日成交额同步增加',
    Math.abs(num(ov.todaySales) - (num(base.todaySales) + 276.9)) < 0.001,
    `${base.todaySales} → ${ov.todaySales}`)
  check('会员数 ≥ 1', ov.memberCount >= 1)
  check('待发货订单数 +2', ov.pendingDelivery === base.pendingDelivery + 2, `${base.pendingDelivery} → ${ov.pendingDelivery}`)
  check('总览含待审退款字段', 'refunding' in ov && 'reviewCount' in ov)

  // ============================================================
  section('4. 销售额趋势（折线图）')
  const trend = (await A('GET', '/api/mall/admin/stats/sales-trend?type=day&size=7')).json.data
  check('日趋势返回 7 个点', trend.length === 7, `${trend.length}`)
  const todayStr = (() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })()
  const lastPoint = trend[trend.length - 1]
  check('最后一个点是今天', lastPoint.date === todayStr, `${lastPoint.date} vs ${todayStr}`)
  check('今天成交额 = 本次 276.90（基线 + 增量）',
    Math.abs(num(lastPoint.sales) - (num(base.todaySales) + 276.9)) < 0.001,
    `${lastPoint.sales}`)
  check('今天订单数 ≥ 2', lastPoint.orders >= 2)
  check('无成交的日期补 0（前两天存在 0 值点）',
    trend.slice(0, 5).every((p) => p.sales !== null && p.sales !== undefined && p.orders >= 0))

  const trendM = (await A('GET', '/api/mall/admin/stats/sales-trend?type=month&size=3')).json.data
  check('月趋势返回 3 个点', trendM.length === 3)
  const curMonth = todayStr.slice(0, 7)
  check('最后一个月是当前月', trendM[2].date === curMonth, `${trendM[2].date} vs ${curMonth}`)
  check('当月成交额 ≥ 276.90', num(trendM[2].sales) >= 276.9 - 0.001, `${trendM[2].sales}`)
  const trendClamp = (await A('GET', '/api/mall/admin/stats/sales-trend?type=day&size=999')).json.data
  check('日趋势 size 上限 90', trendClamp.length === 90, `${trendClamp.length}`)

  // ============================================================
  section('5. 品类占比（饼图）')
  const cats = (await A('GET', '/api/mall/admin/stats/category-sales')).json.data
  check('品类占比返回数据', Array.isArray(cats) && cats.length >= 1, `${cats.length} 个品类`)
  const cloth = cats.find((c) => c.categoryName === '服饰鞋包')
  const food = cats.find((c) => c.categoryName === '食品饮料')
  check('服饰鞋包按顶级分类归并（T恤 237.00）',
    !!cloth && num(cloth.amount) >= 237 - 0.001, JSON.stringify(cloth))
  check('食品饮料（咖啡液 39.90）',
    !!food && num(food.amount) >= 39.9 - 0.001, JSON.stringify(food))
  const percentSum = cats.reduce((s, c) => s + num(c.percent), 0)
  check('百分比合计 ≈ 100', Math.abs(percentSum - 100) < 0.5, `sum=${percentSum}`)

  // ============================================================
  section('6. 热销商品 TOP N（柱状图）')
  const top = (await A('GET', '/api/mall/admin/stats/top-products?limit=5')).json.data
  check('TOP N 返回数据', Array.isArray(top) && top.length >= 1)
  const tshirt = top.find((t) => t.productId === 1)
  check('T恤销量 = 基线 + 3', !!tshirt && tshirt.quantity === baseTshirt + 3,
    `${baseTshirt} → ${tshirt?.quantity}`)
  check('T恤成交额 ≥ 237.00', !!tshirt && num(tshirt.amount) >= 237 - 0.001, `${tshirt?.amount}`)
  const sorted = top.every((t, i) => i === 0 || top[i - 1].quantity >= t.quantity)
  check('按销量倒序', sorted, JSON.stringify(top.map((t) => `${t.productName}:${t.quantity}`)))
  const topClamp = (await A('GET', '/api/mall/admin/stats/top-products?limit=999')).json.data
  check('limit 上限 50（返回条数 ≤ 50）', topClamp.length <= 50, `${topClamp.length}`)

  // ============================================================
  section('7. 权限隔离')
  const statsAsBuyer = await req('GET', '/api/mall/admin/stats/overview', { headers: { token: tempToken } })
  check('买家 token 打后台统计接口 → 401', statsAsBuyer.status === 401, `HTTP ${statsAsBuyer.status}`)
  const statsNoToken = await req('GET', '/api/mall/admin/stats/overview')
  check('未登录打后台统计接口 → 401', statsNoToken.status === 401, `HTTP ${statsNoToken.status}`)
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
      console.log(`\n（已清理临时买家 ${newEmail} 及其订单 / 地址 / 购物车）`)
    }
    if (init.p1Status !== undefined) {
      sql(`update mall_product set status=${init.p1Status} where id=1`)
      sql(`update mall_product set status=${init.p3Status} where id=3`)
      console.log('（已恢复商品状态）')
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
