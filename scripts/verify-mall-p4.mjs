// scripts/verify-mall-p4.mjs
// 电商板块 P4（订单履约）验收：下单 / 防超卖 / 支付 / 超时取消 / 发货 / 收货 / 退款审核。
//
// 重点验证：
//   ① 下单：金额服务端重算、库存 Redis+DB 双扣、购物车已结算行清空、token 幂等
//   ② 防超卖：库存不足 / 商品下架 时下单被拒
//   ③ 取消 & 超时：状态 CANCELLED 且库存回补；超时由定时任务自动关单
//   ④ 支付：Mock 支付一次性令牌、状态流转、销量累加、重复支付被拒
//   ⑤ 履约：待发货→待收货（含物流单号）→已完成；重复发货被拒
//   ⑥ 退款：申请→审核驳回（订单回退）→再次申请→审核通过（回补库存、扣回销量）
//   ⑦ 权限：买家 token 打后台 401、越权看别人订单被拒
//
// 用法（需 8081 电商 + Redis + MySQL 在跑）：
//   node scripts/verify-mall-p4.mjs
//   SKIP_TIMEOUT=1 node scripts/verify-mall-p4.mjs   # 跳过需要等待 1 分钟的定时任务用例
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
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/* ---------------- HTTP ---------------- */

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

/* ---------------- CLI ---------------- */

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
const num = (s) => Number(sql(s))

/* ---------------- 登录 / 注册 ---------------- */

let adminToken = null
let tempToken = null
let tempUserId = null
let user2Token = null
const newEmail = `p4verify${Date.now().toString(36)}@qq.com`

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
async function buyerLogin(username, password) {
  const cap = await captcha()
  const r = await req('POST', '/api/mall/user/login', { body: { username, password, ...cap } })
  if (!ok(r)) throw new Error(`买家登录失败（${username}）：` + r.text)
  return r.json.data.token
}

const A = (m, p, body) => req(m, p, { headers: { 'mall-admin-token': adminToken }, body })
const U = (m, p, body) => req(m, p, { headers: { token: tempToken }, body })

/* ---------------- 便捷方法 ---------------- */

async function newAddress() {
  const r = await U('POST', '/api/mall/user/address', {
    consignee: 'P4买家', phone: '13800138000', areaCode: '110101', detail: '东华门大街 1 号', label: '家'
  })
  if (!ok(r)) throw new Error('建地址失败：' + r.text)
  return r.json.data
}

async function addCart(skuId, number) {
  const r = await U('POST', '/api/mall/user/cart', { skuId, number })
  if (!ok(r)) throw new Error('加购失败：' + r.text)
  return r.json.data
}

async function submitOrder(addressId, remark = 'P4验收') {
  const t = await U('GET', '/api/mall/user/orders/token')
  const r = await U('POST', '/api/mall/user/orders/submit', {
    addressId, remark, submitToken: t.json.data.submitToken
  })
  return r
}

async function payOrder(orderNumber) {
  const t = await U('GET', `/api/mall/user/payment/token?orderNumber=${orderNumber}`)
  if (!ok(t)) return t
  return U('POST', '/api/mall/user/payment/pay', {
    orderNumber, payToken: t.json.data.payToken
  })
}

const stockOf = (skuId) => num(`select stock from mall_sku where id=${skuId}`)
const skuSalesOf = (skuId) => num(`select sales from mall_sku where id=${skuId}`)
const productSalesOf = (pid) => num(`select sales from mall_product where id=${pid}`)

/* ---------------- 用例 ---------------- */

let init = {}
const createdOrderNumbers = []

try {
  await adminLogin()
  console.log('（管理员已登录）')

  // 临时买家，避免污染演示账号数据
  const cap = await captcha()
  const reg = await req('POST', '/api/mall/user/register', {
    body: { username: newEmail, password: 'Mall1234', nickname: 'P4验收买家', ...cap }
  })
  if (!ok(reg)) throw new Error('临时买家注册失败：' + reg.text)
  tempToken = reg.json.data.token
  tempUserId = reg.json.data.id
  console.log(`（临时买家 ${newEmail} 已注册，id=${tempUserId}）`)

  // 商品 1/3 上架（种子默认下架），并记录库存 / 销量
  init = {
    p1Status: num('select status from mall_product where id=1'),
    p3Status: num('select status from mall_product where id=3'),
    sku1Stock: stockOf(1),
    sku9Stock: stockOf(9),
    sku1Sales: skuSalesOf(1),
    sku9Sales: skuSalesOf(9),
    p1Sales: productSalesOf(1),
    p3Sales: productSalesOf(3)
  }
  sql('update mall_product set status=1 where id in (1,3)')

  const addressId = await newAddress()

  // ============================================================
  section('1. 下单前置校验')
  const noAuth = await req('GET', '/api/mall/user/orders/page')
  check('未登录访问订单 → HTTP 401', noAuth.status === 401, `HTTP ${noAuth.status}`)

  const emptySubmit = await submitOrder(addressId)
  check('购物车空 → 请先选择要结算的商品',
    !ok(emptySubmit) && msg(emptySubmit).includes('请先选择要结算的商品'), msg(emptySubmit))

  const noToken = await U('POST', '/api/mall/user/orders/submit', { addressId })
  check('缺 submitToken → 参数校验失败', !ok(noToken) && msg(noToken).includes('下单令牌'), msg(noToken))

  // ============================================================
  section('2. 下单：金额 / 库存 / 幂等 / 快照')
  const stock1Before = stockOf(1)
  const stock9Before = stockOf(9)
  await addCart(1, 2)
  await addCart(9, 1)

  const sub = await submitOrder(addressId, '尽快发货')
  check('提交订单成功', ok(sub), msg(sub))
  const orderNo = sub.json?.data?.orderNumber
  createdOrderNumbers.push(orderNo)
  check('金额 = 79×2 + 39.9 = 197.90', Number(sub.json?.data?.payAmount) === 197.9,
    `${sub.json?.data?.payAmount}`)
  check('支付截止时间 ≈ 15 分钟后',
    new Date(sub.json?.data?.expireTime.replace(' ', 'T')) > new Date(Date.now() + 13 * 60 * 1000))

  check('SKU 1 库存 -2', stockOf(1) === stock1Before - 2, `${stock1Before} → ${stockOf(1)}`)
  check('SKU 9 库存 -1', stockOf(9) === stock9Before - 1, `${stock9Before} → ${stockOf(9)}`)
  check('Redis 库存与 DB 一致',
    Number(redis(['GET', 'mall:stock:sku:1'])) === stockOf(1),
    `redis=${redis(['GET', 'mall:stock:sku:1'])} db=${stockOf(1)}`)

  const cartAfter = await U('GET', '/api/mall/user/cart')
  check('下单后购物车已结算行被清空', (cartAfter.json?.data?.items || []).length === 0)

  const detail = await U('GET', `/api/mall/user/orders/${orderNo}`)
  const od = detail.json?.data || {}
  check('订单状态 = 待付款 / 未支付',
    od.status === 'PENDING_PAYMENT' && od.payStatus === 'UN_PAID', `${od.status}/${od.payStatus}`)
  check('收货信息是快照（收货人 P4买家）', od.receiverName === 'P4买家' && od.receiverDistrict === '东城区')
  check('明细 2 行且带规格文案', (od.items || []).length === 2 &&
    (od.items || []).some((i) => i.specText === '白 / M'), JSON.stringify((od.items || []).map((i) => i.specText)))
  check('statusDesc = 待付款', od.statusDesc === '待付款', String(od.statusDesc))

  const myPage = await U('GET', '/api/mall/user/orders/page?status=PENDING_PAYMENT&page=1&pageSize=10')
  check('我的订单分页能查到该单',
    (myPage.json?.data?.records || []).some((o) => o.orderNumber === orderNo))

  // 重复令牌
  const t2 = await U('GET', '/api/mall/user/orders/token')
  await U('POST', '/api/mall/user/orders/submit', { addressId, submitToken: t2.json.data.submitToken })
  const dup = await U('POST', '/api/mall/user/orders/submit', { addressId, submitToken: t2.json.data.submitToken })
  check('同一令牌重复提交 → 请勿重复提交订单',
    !ok(dup) && msg(dup).includes('请勿重复提交'), msg(dup))

  // 防超卖：Redis 预扣库存不足（DB 仍有货，但 Redis 预扣挡下）
  const stockKey1 = 'mall:stock:sku:1'
  const savedRedisStock = redis(['GET', stockKey1])
  redis(['SET', stockKey1, '1'])
  await addCart(1, 2)
  const oversell = await submitOrder(addressId)
  check('库存不足 → 下单被拒（Redis 预扣拦截）',
    !ok(oversell) && msg(oversell).includes('库存不足'), msg(oversell))
  redis(['SET', stockKey1, savedRedisStock])
  await U('DELETE', '/api/mall/user/cart?scope=all')

  // 商品下架 → 下单被拒（先加购，再下架）
  await addCart(1, 1)
  sql('update mall_product set status=0 where id=1')
  const offSale = await submitOrder(addressId)
  check('商品下架 → 下单被拒', !ok(offSale) && msg(offSale).includes('已下架'), msg(offSale))
  sql('update mall_product set status=1 where id=1')
  await U('DELETE', '/api/mall/user/cart?scope=all')

  // ============================================================
  section('3. 取消订单（库存回补）')
  await addCart(1, 1)
  const beforeCancelStock = stockOf(1)
  const cancelSub = await submitOrder(addressId)
  const cancelNo = cancelSub.json?.data?.orderNumber
  createdOrderNumbers.push(cancelNo)
  check('取消前库存已扣', stockOf(1) === beforeCancelStock - 1)

  const cancelRes = await U('PUT', `/api/mall/user/orders/${cancelNo}/cancel`)
  check('取消订单成功', ok(cancelRes), msg(cancelRes))
  check('取消后库存回补', stockOf(1) === beforeCancelStock, `${stockOf(1)}`)
  const cancelDetail = await U('GET', `/api/mall/user/orders/${cancelNo}`)
  check('订单状态 = 已取消', cancelDetail.json?.data?.status === 'CANCELLED')
  const cancelAgain = await U('PUT', `/api/mall/user/orders/${cancelNo}/cancel`)
  check('已取消订单再取消 → 状态不允许', !ok(cancelAgain), msg(cancelAgain))

  // ============================================================
  section('4. 支付（Mock）')
  await addCart(1, 2)
  const paySub = await submitOrder(addressId)
  const payNo = paySub.json?.data?.orderNumber
  createdOrderNumbers.push(payNo)
  const sales1Before = skuSalesOf(1)
  const p1SalesBefore = productSalesOf(1)

  const payRes = await payOrder(payNo)
  check('支付成功', ok(payRes), msg(payRes))
  const paidAmount = Number(paySub.json?.data?.payAmount)
  const payDetail = await U('GET', `/api/mall/user/orders/${payNo}`)
  check('订单状态 = 待发货 / 已支付',
    payDetail.json?.data?.status === 'PENDING_DELIVERY' && payDetail.json?.data?.payStatus === 'PAID',
    `${payDetail.json?.data?.status}/${payDetail.json?.data?.payStatus}`)
  check('支付时间已写入', !!payDetail.json?.data?.payTime)
  check('销量累加：SKU +2', skuSalesOf(1) === sales1Before + 2, `${sales1Before} → ${skuSalesOf(1)}`)
  check('销量累加：SPU +2', productSalesOf(1) === p1SalesBefore + 2)

  const payAgain = await U('GET', `/api/mall/user/payment/token?orderNumber=${payNo}`)
  check('已支付订单再取支付令牌 → 状态不允许', !ok(payAgain) && msg(payAgain).includes('不允许支付'), msg(payAgain))

  const cancelPaid = await U('PUT', `/api/mall/user/orders/${payNo}/cancel`)
  check('已支付订单不能直接取消（要走退款）', !ok(cancelPaid) && msg(cancelPaid).includes('状态不允许'), msg(cancelPaid))

  // ============================================================
  section('5. 发货 / 确认收货')
  check('发货前无物流信息', payDetail.json?.data?.shipment == null)
  const ship = await A('PUT', `/api/mall/admin/orders/${payNo}/ship`, {
    company: '顺丰速运', trackingNo: 'SF' + Date.now()
  })
  check('管理员发货成功', ok(ship), msg(ship))
  const shipped = await U('GET', `/api/mall/user/orders/${payNo}`)
  check('订单状态 = 待收货', shipped.json?.data?.status === 'PENDING_RECEIPT')
  check('物流公司与单号已写入',
    shipped.json?.data?.shipment?.company === '顺丰速运' && !!shipped.json?.data?.shipment?.trackingNo,
    JSON.stringify(shipped.json?.data?.shipment))
  const shipAgain = await A('PUT', `/api/mall/admin/orders/${payNo}/ship`, {
    company: '中通', trackingNo: 'ZT1'
  })
  check('重复发货 → 该订单已发货', !ok(shipAgain) && msg(shipAgain).includes('已发货'), msg(shipAgain))

  const receipt = await U('PUT', `/api/mall/user/orders/${payNo}/receipt`)
  check('确认收货成功', ok(receipt), msg(receipt))
  const completed = await U('GET', `/api/mall/user/orders/${payNo}`)
  check('订单状态 = 已完成', completed.json?.data?.status === 'COMPLETED')
  check('收货时间已写入', !!completed.json?.data?.receiveTime)
  const receiptAgain = await U('PUT', `/api/mall/user/orders/${payNo}/receipt`)
  check('已完成订单再确认收货 → 状态不允许', !ok(receiptAgain), msg(receiptAgain))

  // ============================================================
  section('6. 退款申请 + 审核')
  const stockBeforeRefund = stockOf(1)
  const salesBeforeRefund = skuSalesOf(1)
  const refundApply = await U('POST', `/api/mall/user/orders/${payNo}/refund`, {
    type: 2, reason: '不想要了', description: '已拆封'
  })
  check('申请退款成功', ok(refundApply), msg(refundApply))
  const refundId = refundApply.json?.data?.id
  check('退款金额 = 订单实付', Number(refundApply.json?.data?.amount) === paidAmount,
    `${refundApply.json?.data?.amount} vs ${paidAmount}`)
  check('记录申请前订单状态 = COMPLETED', refundApply.json?.data?.previousStatus === 'COMPLETED')
  const afterApply = await U('GET', `/api/mall/user/orders/${payNo}`)
  check('申请后订单状态 = 退款中', afterApply.json?.data?.status === 'REFUNDING')

  const dupApply = await U('POST', `/api/mall/user/orders/${payNo}/refund`, { type: 1, reason: 'x' })
  check('重复申请退款 → 状态不支持', !ok(dupApply) && msg(dupApply).includes('不支持'), msg(dupApply))

  const refundProgress = await U('GET', `/api/mall/user/orders/${payNo}/refund`)
  check('买家可查退款进度', ok(refundProgress) && refundProgress.json?.data?.status === 'PENDING')

  const refundPage = await A('GET', '/api/mall/admin/refunds/page?status=PENDING&page=1&pageSize=10')
  check('后台退款列表含该申请',
    (refundPage.json?.data?.records || []).some((r) => r.id === refundId))
  const refundDetail = await A('GET', `/api/mall/admin/refunds/${refundId}`)
  check('后台退款详情带关联订单',
    refundDetail.json?.data?.order?.orderNumber === payNo &&
    (refundDetail.json?.data?.order?.items || []).length === 1,
    JSON.stringify(refundDetail.json?.data?.order?.orderNumber))

  // 驳回 → 订单回退
  const reject = await A('PUT', `/api/mall/admin/refunds/${refundId}/audit`, {
    approved: false, adminRemark: '不符合退货条件'
  })
  check('审核驳回成功', ok(reject), msg(reject))
  const afterReject = await U('GET', `/api/mall/user/orders/${payNo}`)
  check('驳回后订单回退到已完成', afterReject.json?.data?.status === 'COMPLETED')
  check('驳回后退款单状态 = 已拒绝', afterReject.json?.data?.refund?.status === 'REJECTED')
  const auditAgain = await A('PUT', `/api/mall/admin/refunds/${refundId}/audit`, { approved: true })
  check('已审核的退款单不能再审', !ok(auditAgain) && msg(auditAgain).includes('重复审核'), msg(auditAgain))

  // 再次申请 → 通过
  const refundApply2 = await U('POST', `/api/mall/user/orders/${payNo}/refund`, {
    type: 2, reason: '坚持退款'
  })
  check('驳回后可重新申请', ok(refundApply2), msg(refundApply2))
  const refundId2 = refundApply2.json?.data?.id
  const approve = await A('PUT', `/api/mall/admin/refunds/${refundId2}/audit`, {
    approved: true, adminRemark: '同意退款'
  })
  check('审核通过成功', ok(approve), msg(approve))
  const afterApprove = await U('GET', `/api/mall/user/orders/${payNo}`)
  check('订单状态 = 已退款', afterApprove.json?.data?.status === 'REFUNDED')
  check('支付状态 = 已退款', afterApprove.json?.data?.payStatus === 'REFUNDED')
  check('退款后库存回补', stockOf(1) === stockBeforeRefund + 2, `${stockBeforeRefund} → ${stockOf(1)}`)
  check('退款后销量扣回', skuSalesOf(1) === salesBeforeRefund - 2, `${salesBeforeRefund} → ${skuSalesOf(1)}`)
  check('Redis 库存也回补', Number(redis(['GET', 'mall:stock:sku:1'])) === stockOf(1))

  // ============================================================
  section('7. 运营后台订单查询')
  const adminPage = await A('GET', `/api/mall/admin/orders/page?orderNumber=${payNo}`)
  check('后台按订单号查询成功', (adminPage.json?.data?.records || []).length === 1)
  const adminByUser = await A('GET', `/api/mall/admin/orders/page?userId=${tempUserId}&page=1&pageSize=50`)
  check('后台按买家 id 查询成功', (adminByUser.json?.data?.records || []).length >= 2)
  const adminDetail = await A('GET', `/api/mall/admin/orders/${payNo}`)
  check('后台订单详情含明细与物流', (adminDetail.json?.data?.items || []).length === 1 &&
    !!adminDetail.json?.data?.shipment)

  // ============================================================
  section('8. 权限隔离')
  const adminAsBuyer = await req('GET', `/api/mall/user/orders/${payNo}`, {
    headers: { 'mall-admin-token': adminToken }
  })
  check('管理员 token 打买家订单接口 → 401', adminAsBuyer.status === 401, `HTTP ${adminAsBuyer.status}`)
  const buyerAsAdmin = await req('GET', '/api/mall/admin/orders/page', { headers: { token: tempToken } })
  check('买家 token 打后台订单接口 → 401', buyerAsAdmin.status === 401, `HTTP ${buyerAsAdmin.status}`)

  user2Token = await buyerLogin('user2@qq.com', 'Mall1234')
  const other = await req('GET', `/api/mall/user/orders/${payNo}`, { headers: { token: user2Token } })
  check('看别人的订单 → 订单不存在', !ok(other) && msg(other).includes('订单不存在'), msg(other))

  // ============================================================
  section('9. 超时未支付自动取消（定时任务）')
  if (process.env.SKIP_TIMEOUT) {
    check('SKIP_TIMEOUT 已设置，跳过定时任务用例', true)
  } else {
    await addCart(1, 1)
    const beforeTimeoutStock = stockOf(1)
    const timeoutSub = await submitOrder(addressId)
    const timeoutNo = timeoutSub.json?.data?.orderNumber
    createdOrderNumbers.push(timeoutNo)
    check('建待付款订单（库存已扣）', stockOf(1) === beforeTimeoutStock - 1)

    // 把支付截止时间改到过去，等定时任务（每分钟第 0 秒）扫到
    sql(`update mall_order set expire_time = date_sub(now(), interval 1 minute) where order_number='${timeoutNo}'`)
    let cancelled = false
    const deadline = Date.now() + 75000
    while (Date.now() < deadline) {
      await sleep(5000)
      const st = sql(`select status from mall_order where order_number='${timeoutNo}'`)
      if (st === 'CANCELLED') { cancelled = true; break }
    }
    check('定时任务自动取消超时订单', cancelled,
      cancelled ? '75 秒内被取消' : '等待 75 秒仍未取消')
    if (cancelled) {
      check('超时取消后库存回补', stockOf(1) === beforeTimeoutStock, `${stockOf(1)}`)
      const toDetail = await U('GET', `/api/mall/user/orders/${timeoutNo}`)
      check('取消原因为超时未支付', (toDetail.json?.data?.cancelReason || '').includes('超时'), toDetail.json?.data?.cancelReason)
    }
  }
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  // ---------- 清理：删掉临时买家的一切痕迹，恢复商品/SKU 数据与 Redis ----------
  try {
    if (tempUserId) {
      // 退款先删（可能还有 PENDING 的），再删物流/明细/订单
      sql(`delete from mall_refund where user_id=${tempUserId}`)
      sql(`delete from mall_shipment where order_id in (select id from mall_order where user_id=${tempUserId})`)
      sql(`delete from mall_order_item where order_id in (select id from mall_order where user_id=${tempUserId})`)
      sql(`delete from mall_order where user_id=${tempUserId}`)
      sql(`delete from mall_cart_item where user_id=${tempUserId}`)
      sql(`delete from address where user_id=${tempUserId}`)
      sql(`delete from sys_user where id=${tempUserId}`)
      console.log(`\n（已清理临时买家 ${newEmail} 及其订单 / 地址 / 购物车）`)
    }
    if (init.sku1Stock !== undefined) {
      sql(`update mall_sku set stock=${init.sku1Stock}, sales=${init.sku1Sales} where id=1`)
      sql(`update mall_sku set stock=${init.sku9Stock}, sales=${init.sku9Sales} where id=9`)
      sql(`update mall_product set sales=${init.p1Sales} where id=1`)
      sql(`update mall_product set sales=${init.p3Sales} where id=3`)
      sql(`update mall_product set status=${init.p1Status} where id=1`)
      sql(`update mall_product set status=${init.p3Status} where id=3`)
      // DB 还原后同步 Redis 库存，避免与 DB 不一致
      redis(['SET', 'mall:stock:sku:1', String(init.sku1Stock)])
      redis(['SET', 'mall:stock:sku:9', String(init.sku9Stock)])
      console.log('（已恢复 SKU 库存/销量、商品销量/状态，并同步 Redis）')
    }
  } catch (e) {
    console.log('（清理失败，请手工检查：', e.message, '）')
  }

  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
