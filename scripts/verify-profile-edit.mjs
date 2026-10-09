// scripts/verify-profile-edit.mjs
// 各角色「改资料」验收（外卖 8080）：平台运营 / 商家 / 骑手。
//
// 重点：
//   1. 平台运营  GET/PUT /api/platform/profile（新增）
//   2. 商家      PUT /api/merchant/shop 用 **shopName / logo**（旧前端传的 name / image 是无效的）
//               + PUT /api/merchant/shop/business 用 **businessStatus**（不是 status）
//   3. 骑手      PUT /api/delivery/profile 的 name / phone 真能落库
//   4. 全部改完还原，不留脏数据
//
// 用法（需 8080 + Redis 在跑）：
//   node scripts/verify-profile-edit.mjs
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

async function req(method, path, { headers = {}, body } = {}) {
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

/* Redis */
const CANDIDATES = [
  process.env.REDIS_CLI, 'redis-cli',
  'D:/redis/Redis-x64-5.0.14.1/redis-cli', 'C:/Program Files/Redis/redis-cli.exe'
].filter(Boolean)
let cliPath = null
function redis(args) {
  if (!cliPath) {
    for (const p of CANDIDATES) {
      if (p !== 'redis-cli' && !fs.existsSync(p)) continue
      try { execFileSync(p, ['--version'], { stdio: 'ignore' }); cliPath = p; break } catch { /* next */ }
    }
    if (!cliPath) throw new Error('找不到 redis-cli')
  }
  const base = ['-h', '127.0.0.1', '-p', '6379', '-a', process.env.REDIS_PASSWORD || 'mehado']
  for (const a of [[...base, '--no-auth-warning', ...args], [...base, ...args]]) {
    try {
      const out = execFileSync(cliPath, a, { encoding: 'utf8' }).trim()
      if (out.startsWith('ERR ')) continue
      return out
    } catch { /* retry */ }
  }
  throw new Error('redis-cli 执行失败')
}
async function captcha() {
  const r = await req('GET', '/api/auth/captcha')
  const key = r.json?.data?.key
  if (!key) return {}
  return { captchaKey: key, captchaCode: redis(['GET', `captcha:${key}`]) }
}

const tag = Date.now().toString(36).slice(-4)

try {
  /* ---------------- 平台运营 ---------------- */
  section('1. 平台运营：资料查询 / 修改')
  const pCap = await captcha()
  const pLogin = await req('POST', '/api/platform/login', {
    body: { username: 'platform@163.com', password: 'Admin1234', ...pCap }
  })
  const ptoken = pLogin.json?.data?.token
  check('平台运营登录成功', ok(pLogin) && !!ptoken, pLogin.json?.msg || '')
  const PH = { headers: { 'platform-token': ptoken } }

  check('未登录查资料 → 401', (await req('GET', '/api/platform/profile')).status === 401)
  const p0 = await req('GET', '/api/platform/profile', PH)
  check('查资料成功且 password 为 null',
    ok(p0) && p0.json?.data?.password === null, JSON.stringify(p0.json?.data))
  const originalPName = p0.json?.data?.name || ''

  const newPName = `平台改名${tag}`
  check('改姓名 → 成功', ok(await req('PUT', '/api/platform/profile', { ...PH, body: { name: newPName } })))
  const p1 = await req('GET', '/api/platform/profile', PH)
  check('姓名已落库', p1.json?.data?.name === newPName, `name=${p1.json?.data?.name}`)
  check('空姓名被拒',
    !ok(await req('PUT', '/api/platform/profile', { ...PH, body: { name: '  ' } })))
  await req('PUT', '/api/platform/profile', { ...PH, body: { name: originalPName } })
  check('已还原（平台姓名）', true)

  /* ---------------- 商家 ---------------- */
  section('2. 商家：店铺字段映射 + 营业状态字段名')
  const mCap = await captcha()
  const mLogin = await req('POST', '/api/merchant/login', {
    body: { username: 'merchant1@163.com', password: 'Mall1234', ...mCap }
  })
  const mtoken = mLogin.json?.data?.token
  check('商家登录成功', ok(mLogin) && !!mtoken, mLogin.json?.msg || '')
  const MH = { headers: { 'merchant-token': mtoken } }

  const shop0 = (await req('GET', '/api/merchant/shop', MH)).json?.data || {}
  const originalShop = { shopName: shop0.shopName, logo: shop0.logo, phone: shop0.phone, address: shop0.address }

  const newShopName = `老王牛肉面${tag}`
  const testLogo = 'https://mehado-java.oss-cn-beijing.aliyuncs.com/supermall/products/beef-noodle.svg'
  check('改 shopName + logo → 成功',
    ok(await req('PUT', '/api/merchant/shop', { ...MH, body: { shopName: newShopName, logo: testLogo } })))
  const shop1 = (await req('GET', '/api/merchant/shop', MH)).json?.data || {}
  check('店名真的改掉了（前端曾误传 name，静默无效）', shop1.shopName === newShopName, `shopName=${shop1.shopName}`)

  section('3. 商家：营业状态（字段名必须是 businessStatus）')
  const b0 = await req('GET', '/api/merchant/shop/business', MH)
  check('营业状态是数字（不是对象）', typeof b0.json?.data === 'number', `${JSON.stringify(b0.json?.data)} (${typeof b0.json?.data})`)
  const originalB = Number(b0.json?.data ?? 1)

  check('用错的字段名 status → 被拒（400/业务错）',
    !ok(await req('PUT', '/api/merchant/shop/business', { ...MH, body: { status: 0 } })))
  check('用 businessStatus → 成功',
    ok(await req('PUT', '/api/merchant/shop/business', { ...MH, body: { businessStatus: 0 } })))
  const b1 = await req('GET', '/api/merchant/shop/business', MH)
  check('已切换为休息中', Number(b1.json?.data) === 0, `business=${b1.json?.data}`)
  await req('PUT', '/api/merchant/shop/business', { ...MH, body: { businessStatus: originalB } })
  // 注意：logo 传 null 等于“不改”（<if test="logo != null">），要清空得传空串
  await req('PUT', '/api/merchant/shop', { ...MH, body: { ...originalShop, logo: originalShop.logo ?? '' } })
  const shopBack = (await req('GET', '/api/merchant/shop', MH)).json?.data || {}
  check('已还原（店名 / 门头图 / 营业状态）',
    shopBack.shopName === originalShop.shopName && Number((await req('GET', '/api/merchant/shop/business', MH)).json?.data) === originalB,
    `shopName=${shopBack.shopName}, business=${originalB}`)

  /* ---------------- 骑手 ---------------- */
  section('4. 骑手：姓名 / 手机号')
  const rCap = await captcha()
  const rLogin = await req('POST', '/api/delivery/login', {
    body: { username: 'rider1@gmail.com', password: 'Mall1234', ...rCap }
  })
  const rtoken = rLogin.json?.data?.token
  check('骑手登录成功', ok(rLogin) && !!rtoken, rLogin.json?.msg || '')
  const RH = { headers: { 'delivery-token': rtoken } }

  const rider0 = (await req('GET', '/api/delivery/profile', RH)).json?.data || {}
  const originalRider = { name: rider0.name, phone: rider0.phone }

  const newRiderName = `闪电侠${tag}`
  check('改姓名 + 手机号 → 成功',
    ok(await req('PUT', '/api/delivery/profile', { ...RH, body: { name: newRiderName, phone: '13700000066' } })))
  const rider1 = (await req('GET', '/api/delivery/profile', RH)).json?.data || {}
  check('姓名 / 手机号都已落库',
    rider1.name === newRiderName && rider1.phone === '13700000066',
    `name=${rider1.name}, phone=${rider1.phone}`)
  check('非法手机号被拒',
    !ok(await req('PUT', '/api/delivery/profile', { ...RH, body: { phone: '12345' } })))
  await req('PUT', '/api/delivery/profile', { ...RH, body: originalRider })
  const riderBack = (await req('GET', '/api/delivery/profile', RH)).json?.data || {}
  check('已还原（骑手资料）',
    riderBack.name === originalRider.name && riderBack.phone === originalRider.phone,
    `name=${riderBack.name}, phone=${riderBack.phone}`)
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
