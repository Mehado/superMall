// scripts/verify-platform-onboarding.mjs
// 外卖「平台运营 + 商家入驻 + 骑手注册」验收。
//
// 验证的完整链路：
//   商家：入驻申请 → 待审核（登录被拦）→ 平台驳回（带原因）→ 重新提交 → 平台通过
//         → 商家登录 → 建分类 / 建商品 → 用户端能浏览到该商品
//   骑手：注册即启用 → 能查资料 → 平台列表可见 → 平台停用 → 不能再登录
//   隔离：买家 / 商家 / 骑手 token 打平台接口一律 401；平台 token 打商家接口 401
//
// 用法（需 8080 外卖 + Redis 在跑）：
//   node scripts/verify-platform-onboarding.mjs
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'

const BASE = String(process.env.WAIMAI_BASE || 'http://localhost:8080').replace(/\/$/, '')

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

/* ---------------- HTTP ---------------- */

async function api(method, path, { headers = {}, body } = {}) {
  const h = { ...headers }
  if (body !== undefined) h['Content-Type'] = 'application/json'
  const res = await fetch(BASE + path, {
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

/* ---------------- Redis（读验证码答案） ---------------- */

const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1'
const REDIS_PORT = process.env.REDIS_PORT || '6379'
const REDIS_PASSWORD = process.env.REDIS_PASSWORD || 'mehado'
const CANDIDATES = [
  process.env.REDIS_CLI,
  'redis-cli',
  'D:/redis/Redis-x64-5.0.14.1/redis-cli',
  'C:/Program Files/Redis/redis-cli.exe'
].filter(Boolean)
let cliPath = null
function redis(args) {
  if (!cliPath) {
    for (const p of CANDIDATES) {
      if (p !== 'redis-cli' && !fs.existsSync(p)) continue
      try {
        execFileSync(p, ['--version'], { stdio: 'ignore' })
        cliPath = p
        break
      } catch {
        /* 下一个 */
      }
    }
    if (!cliPath) throw new Error('找不到 redis-cli，可用 REDIS_CLI 指定路径')
  }
  const base = ['-h', REDIS_HOST, '-p', REDIS_PORT, '-a', REDIS_PASSWORD]
  for (const a of [[...base, '--no-auth-warning', ...args], [...base, ...args]]) {
    try {
      return execFileSync(cliPath, a, { encoding: 'utf8' }).trim()
    } catch {
      /* 重试 */
    }
  }
  throw new Error('redis-cli 执行失败')
}

/** 取一组图形验证码字段（外卖 key 前缀 captcha:） */
async function captcha() {
  const r = await api('GET', '/api/auth/captcha')
  const key = r.json?.data?.key
  if (!key) return {}
  const code = redis(['GET', `captcha:${key}`])
  if (!code) throw new Error(`Redis 取不到 captcha:${key}，请确认 Redis 在 ${REDIS_HOST}:${REDIS_PORT} 且在跑`)
  return { captchaKey: key, captchaCode: code }
}

/* ================= 主流程 ================= */

const ts = Date.now().toString(36)
const MERCHANT_EMAIL = `newshop${ts}@163.com`
const RIDER_EMAIL = `newrider${ts}@gmail.com`
const MERCHANT_PWD = 'Mall1234'
const CATEGORY_NAME = `测试分类${ts}`
const PRODUCT_NAME = `测试商品${ts}`

try {
  section('1. 平台运营登录（platform@163.com / Admin1234）')
  const pf = await captcha()
  const plogin = await api('POST', '/api/platform/login', {
    body: { username: 'platform@163.com', password: 'Admin1234', ...pf }
  })
  const platformToken = plogin.json?.data?.token
  check('平台登录成功且返回 platform-token', ok(plogin) && typeof platformToken === 'string',
    plogin.json?.msg || '')
  const PH = { headers: { 'platform-token': platformToken } }

  const overview = await api('GET', '/api/platform/overview', PH)
  check('平台看板可用（含待审商家 / 骑手数）',
    ok(overview) && typeof overview.json?.data?.totalRiders === 'number',
    JSON.stringify(overview.json?.data))

  section('2. 准备好其它角色的 token（用于隔离测试）')
  const mf = await captcha()
  const mlogin = await api('POST', '/api/merchant/login', {
    body: { username: 'merchant1@163.com', password: MERCHANT_PWD, ...mf }
  })
  const merchantToken = mlogin.json?.data?.token
  check('既有商家（老王牛肉面）能登录', ok(mlogin) && !!merchantToken, mlogin.json?.msg || '')

  const uf = await captcha()
  const ulogin = await api('POST', '/api/user/login', {
    body: { username: 'user1@qq.com', password: MERCHANT_PWD, ...uf }
  })
  const userToken = ulogin.json?.data?.token
  check('买家能登录', ok(ulogin) && !!userToken, ulogin.json?.msg || '')

  section('3. 角色隔离：别人打不了平台接口')
  check('无 token 打平台接口 → 401',
    (await api('GET', '/api/platform/overview')).status === 401)
  check('商家 token 打平台接口 → 401',
    (await api('GET', '/api/platform/overview', { headers: { 'merchant-token': merchantToken } })).status === 401)
  check('买家 token 打平台接口 → 401',
    (await api('GET', '/api/platform/overview', { headers: { token: userToken } })).status === 401)
  check('平台 token 打商家接口 → 401',
    (await api('GET', '/api/merchant/shop', PH)).status === 401)

  section('4. 商家入驻申请')
  const af = await captcha()
  const apply = await api('POST', '/api/merchant/apply', {
    body: {
      username: MERCHANT_EMAIL,
      password: MERCHANT_PWD,
      shopName: `川味小馆${ts}`,
      contactName: '张三',
      phone: '13900000099',
      address: '北京市朝阳区望京街 9 号',
      description: '测试入驻',
      licenseImage: '',
      ...af
    }
  })
  const merchantId = apply.json?.data?.id
  check('提交入驻申请成功（返回待审核）',
    ok(apply) && apply.json?.data?.auditStatus === 0, apply.json?.msg || '')
  check('申请后不能直接登录（审核中）',
    !ok(await api('POST', '/api/merchant/login', {
      body: { username: MERCHANT_EMAIL, password: MERCHANT_PWD, ...(await captcha()) }
    })))
  const status1 = await api('GET', `/api/merchant/apply/status?username=${encodeURIComponent(MERCHANT_EMAIL)}`)
  check('申请状态查询 = 待审核', ok(status1) && status1.json?.data?.auditStatus === 0,
    status1.json?.msg || '')

  const pendingList = await api('GET', `/api/platform/merchants/page?auditStatus=0&keyword=${ts}`, PH)
  check('平台待审列表能看到该申请',
    ok(pendingList) && (pendingList.json?.data?.records || []).some((m) => m.username === MERCHANT_EMAIL),
    `total=${pendingList.json?.data?.total}`)

  section('5. 平台审核：驳回 → 重提 → 通过')
  const badAudit = await api('PUT', `/api/platform/merchants/${merchantId}/audit`, {
    ...PH,
    body: { approved: false }
  })
  check('驳回不填原因 → 被拒', !ok(badAudit), badAudit.json?.msg || '')

  const reject = await api('PUT', `/api/platform/merchants/${merchantId}/audit`, {
    ...PH,
    body: { approved: false, reason: '营业执照照片不清晰' }
  })
  check('驳回成功', ok(reject), reject.json?.msg || '')

  const relogin = await api('POST', '/api/merchant/login', {
    body: { username: MERCHANT_EMAIL, password: MERCHANT_PWD, ...(await captcha()) }
  })
  check('驳回后登录被拦且带原因',
    !ok(relogin) && (relogin.json?.msg || '').includes('营业执照照片不清晰'), relogin.json?.msg || '')

  const reapply = await api('POST', '/api/merchant/apply', {
    body: {
      username: MERCHANT_EMAIL, password: MERCHANT_PWD,
      shopName: `川味小馆${ts}`, contactName: '张三', phone: '13900000099',
      address: '北京市朝阳区望京街 9 号', licenseImage: '',
      ...(await captcha())
    }
  })
  check('驳回后可用同一邮箱重新提交', ok(reapply) && reapply.json?.data?.auditStatus === 0,
    reapply.json?.msg || '')

  const dupApply = await api('POST', '/api/merchant/apply', {
    body: {
      username: MERCHANT_EMAIL, password: MERCHANT_PWD,
      shopName: '重复提交', contactName: '张三', phone: '13900000099',
      address: '北京市朝阳区望京街 9 号',
      ...(await captcha())
    }
  })
  check('审核中不能重复提交', !ok(dupApply), dupApply.json?.msg || '')

  const approve = await api('PUT', `/api/platform/merchants/${merchantId}/audit`, {
    ...PH,
    body: { approved: true }
  })
  check('审核通过', ok(approve), approve.json?.msg || '')
  check('同一申请不能重复审核',
    !ok(await api('PUT', `/api/platform/merchants/${merchantId}/audit`, { ...PH, body: { approved: true } })))

  section('6. 新商家登录 → 上架商品 → 用户可见')
  const nf = await captcha()
  const nlogin = await api('POST', '/api/merchant/login', {
    body: { username: MERCHANT_EMAIL, password: MERCHANT_PWD, ...nf }
  })
  const newMerchantToken = nlogin.json?.data?.token
  check('审核通过后新商家能登录', ok(nlogin) && !!newMerchantToken, nlogin.json?.msg || '')
  const NH = { headers: { 'merchant-token': newMerchantToken } }

  const catRes = await api('POST', '/api/merchant/categories', {
    ...NH,
    body: { businessType: 1, name: CATEGORY_NAME, sort: 1 }
  })
  check('新商家能建自己的分类', ok(catRes), catRes.json?.msg || '')

  const catPage = await api('GET', `/api/merchant/categories?businessType=1&pageSize=50`, NH)
  const category = (catPage.json?.data?.records || []).find((c) => c.name === CATEGORY_NAME)
  check('新商家只能看到自己的分类（数据隔离）', !!category && ok(catPage))

  const prodRes = await api('POST', '/api/merchant/products', {
    ...NH,
    body: { categoryId: category?.id, name: PRODUCT_NAME, price: 19.9, stock: 50, status: 1 }
  })
  check('新商家能建自己的商品', ok(prodRes), prodRes.json?.msg || '')

  const pub = await api('GET', `/api/public/merchants/${merchantId}/products?page=1&pageSize=10`)
  check('用户端能浏览到新商家的商品',
    ok(pub) && (pub.json?.data?.records || []).some((p) => p.name === PRODUCT_NAME),
    `total=${pub.json?.data?.total}`)

  section('7. 骑手注册 → 平台管理')
  const rf = await captcha()
  const riderReg = await api('POST', '/api/delivery/register', {
    body: { username: RIDER_EMAIL, password: MERCHANT_PWD, name: `测试骑手${ts}`, phone: '13700000099', ...rf }
  })
  const riderToken = riderReg.json?.data?.token
  check('骑手注册成功且直接返回 token', ok(riderReg) && !!riderToken, riderReg.json?.msg || '')

  const riderProfile = await api('GET', '/api/delivery/profile', { headers: { 'delivery-token': riderToken } })
  check('新骑手能查自己的资料', ok(riderProfile) && riderProfile.json?.data?.name === `测试骑手${ts}`,
    riderProfile.json?.msg || '')

  const riderList = await api('GET', `/api/platform/deliverymen/page?keyword=${ts}`, PH)
  const rider = (riderList.json?.data?.records || []).find((d) => d.username === RIDER_EMAIL)
  check('平台骑手列表能看到新骑手', ok(riderList) && !!rider, `total=${riderList.json?.data?.total}`)

  const disable = await api('PUT', `/api/platform/deliverymen/${rider?.id}/status?status=0`, PH)
  check('平台可停用骑手', ok(disable), disable.json?.msg || '')
  check('停用后骑手不能再登录',
    !ok(await api('POST', '/api/delivery/login', {
      body: { username: RIDER_EMAIL, password: MERCHANT_PWD, ...(await captcha()) }
    })))
  check('平台可重新启用骑手',
    ok(await api('PUT', `/api/platform/deliverymen/${rider?.id}/status?status=1`, PH)))

  console.log(`\nℹ️  本次造了测试数据，清理 SQL：`)
  console.log(`   DELETE FROM product  WHERE merchant_id = ${merchantId};`)
  console.log(`   DELETE FROM sku      WHERE merchant_id = ${merchantId};`)
  console.log(`   DELETE FROM category WHERE merchant_id = ${merchantId};`)
  console.log(`   DELETE FROM merchant WHERE username = '${MERCHANT_EMAIL}';`)
  console.log(`   DELETE FROM deliveryman WHERE username = '${RIDER_EMAIL}';`)
  console.log(`   （或直接按前缀：DELETE FROM merchant WHERE username LIKE 'newshop%@163.com';）`)
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
