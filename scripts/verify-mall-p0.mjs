// scripts/verify-mall-p0.mjs
// 电商板块 P0（服务骨架 + 认证）验收 + 隔离性验证。
//
// 重点验证两件事：
//   ① 隔离：电商是独立服务(8081)，Redis key 走 mall: 前缀，管理员 token 与买家 token 互不通用
//   ② 互通：同一个 sys_user 账号，外卖(8080)登录拿到的 token 在电商(8081)也能用（反之亦然）
//
// 用法（需 8080 外卖 + 8081 电商 + Redis 都在跑）：
//   node scripts/verify-mall-p0.mjs
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'

const MALL = String(process.env.MALL_BASE || 'http://localhost:8081').replace(/\/$/, '')
const WAIMAI = String(process.env.WAIMAI_BASE || 'http://localhost:8080').replace(/\/$/, '')

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

/* ---------------- HTTP ---------------- */

async function req(base, method, path, { headers = {}, body } = {}) {
  const h = { ...headers }
  if (body !== undefined) h['Content-Type'] = 'application/json'
  const res = await fetch(base + path, {
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
const mallApi = (m, p, o) => req(MALL, m, p, o)
const waimaiApi = (m, p, o) => req(WAIMAI, m, p, o)
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

/** 取电商的验证码字段（key 前缀是 mall:captcha:） */
async function mallCaptcha() {
  const r = await mallApi('GET', '/api/mall/auth/captcha')
  const key = r.json?.data?.key
  if (!key) throw new Error('电商取验证码失败：' + r.text)
  const code = redis(['GET', `mall:captcha:${key}`])
  if (!code) throw new Error(`Redis 里取不到 mall:captcha:${key}`)
  return { captchaKey: key, captchaCode: code, key, raw: r.json.data }
}

/* ---------------- 用例 ---------------- */

try {
  section('0. 两个服务并存（独立进程、独立端口）')
  const mallUp = await mallApi('GET', '/api/mall/auth/policy')
  const waimaiUp = await waimaiApi('GET', '/api/auth/policy')
  check('电商服务 8081 可达', ok(mallUp), `HTTP ${mallUp.status}`)
  check('外卖服务 8080 可达', ok(waimaiUp), `HTTP ${waimaiUp.status}`)

  section('1. 电商验证码：接口 + Redis 前缀隔离')
  const cap = await mallCaptcha()
  check('GET /api/mall/auth/captcha 返回 key/image/ttlSeconds',
    !!cap.raw.key && !!cap.raw.image && typeof cap.raw.ttlSeconds === 'number',
    Object.keys(cap.raw).join(', '))
  check('image 是合法 PNG base64',
    Buffer.from(cap.raw.image, 'base64').subarray(0, 4).toString('hex') === '89504e47')
  check('答案存在 Redis 的 mall: 前缀下', !!cap.captchaCode, `mall:captcha:${cap.key} = ${cap.captchaCode}`)
  check('外卖的 captcha: 前缀下【没有】这张验证码（前缀隔离生效）',
    redis(['GET', `captcha:${cap.key}`]) === '', 'captcha:{同一key} 为空')

  section('2. 账号密码规则（复用共享 pojo 的校验注解）')
  const pol = mallUp.json?.data
  check('captchaRequired = true', pol?.captchaRequired === true)
  check('邮箱后缀清单与外卖一致（40 个）', pol?.emailDomains?.length === 40, `${pol?.emailDomains?.length} 个`)
  check('密码规则 8-32 位 + 大小写 + 数字', pol?.passwordMin === 8 && pol?.passwordMax === 32, pol?.passwordDesc)

  section('3. 运营后台管理员登录（独立密钥 + 独立头名）')
  const af = await mallCaptcha()
  const adminLogin = await mallApi('POST', '/api/mall/admin/login', {
    body: { username: 'admin@qq.com', password: 'Admin1234', ...af }
  })
  const adminToken = adminLogin.json?.data?.token
  check('管理员登录成功并返回 token', ok(adminLogin) && !!adminToken, adminLogin.json?.msg || '')
  check('返回含 id/username/name',
    !!adminLogin.json?.data?.id && adminLogin.json?.data?.username === 'admin@qq.com' && !!adminLogin.json?.data?.name,
    JSON.stringify(adminLogin.json?.data ? { name: adminLogin.json.data.name } : {}))

  const adminMe = await mallApi('GET', '/api/mall/admin/profile', {
    headers: { 'mall-admin-token': adminToken }
  })
  check('用 mall-admin-token 能调管理员接口', ok(adminMe))
  check('管理员资料里 password 为 null', adminMe.json?.data?.password === null)

  section('4. 买家登录（共享 sys_user，与外卖同一账号）')
  const bf = await mallCaptcha()
  const buyerLogin = await mallApi('POST', '/api/mall/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', ...bf }
  })
  const mallBuyerToken = buyerLogin.json?.data?.token
  check('买家登录成功（账号来自共享 sys_user）', ok(buyerLogin) && !!mallBuyerToken, buyerLogin.json?.msg || '')

  const buyerMe = await mallApi('GET', '/api/mall/user/profile', {
    headers: { token: mallBuyerToken }
  })
  check('用 token 能调买家接口', ok(buyerMe))
  check('买家资料里 password 为 null', buyerMe.json?.data?.password === null)

  section('5. 【关键】账号互通：外卖的 token 在电商也能用')
  const wf = await waimaiApi('GET', '/api/auth/captcha')
  const wKey = wf.json?.data?.key
  const wCode = redis(['GET', `captcha:${wKey}`])
  const wLogin = await waimaiApi('POST', '/api/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', captchaKey: wKey, captchaCode: wCode }
  })
  const waimaiUserToken = wLogin.json?.data?.token
  check('在外卖 8080 登录成功', ok(wLogin), wLogin.json?.msg || '')

  const crossToMall = await mallApi('GET', '/api/mall/user/profile', {
    headers: { token: waimaiUserToken }
  })
  check('外卖拿到的 token → 电商买家接口可用（同一把 user 密钥）', ok(crossToMall),
    crossToMall.json?.msg || `HTTP ${crossToMall.status}`)

  const crossToWaimai = await waimaiApi('GET', '/api/user/profile', {
    headers: { token: mallBuyerToken }
  })
  check('电商拿到的 token → 外卖买家接口可用（反向互通）', ok(crossToWaimai),
    crossToWaimai.json?.msg || `HTTP ${crossToWaimai.status}`)

  section('6. 【关键】角色隔离：token 不可越权')
  const buyerHitsAdmin = await mallApi('GET', '/api/mall/admin/profile', {
    headers: { 'mall-admin-token': mallBuyerToken }
  })
  check('买家 token 打管理员接口 → 401', buyerHitsAdmin.status === 401, `HTTP ${buyerHitsAdmin.status}`)

  const adminHitsBuyer = await mallApi('GET', '/api/mall/user/profile', {
    headers: { token: adminToken }
  })
  check('管理员 token 打买家接口 → 401', adminHitsBuyer.status === 401, `HTTP ${adminHitsBuyer.status}`)

  const noToken = await mallApi('GET', '/api/mall/user/profile')
  check('不带 token 打买家接口 → 401', noToken.status === 401, `HTTP ${noToken.status}`)

  section('7. 验证码是必需的，且一次性')
  const noCap = await mallApi('POST', '/api/mall/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234' }
  })
  check('不带验证码 → 「请先获取图形验证码」',
    (noCap.json?.msg || '').includes('请先获取图形验证码'), `实际：${noCap.json?.msg}`)

  const once = await mallCaptcha()
  const first = await mallApi('POST', '/api/mall/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', ...once }
  })
  const replay = await mallApi('POST', '/api/mall/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', ...once }
  })
  check('第一次用对 → 成功', ok(first), first.json?.msg || '')
  check('同一张验证码重放 → 已过期', (replay.json?.msg || '').includes('验证码已过期'), `实际：${replay.json?.msg}`)

  const wrong = await mallCaptcha()
  const wrongRes = await mallApi('POST', '/api/mall/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', ...wrong, captchaCode: 'ZZZZ' }
  })
  check('验证码错误 → 「验证码不正确」', (wrongRes.json?.msg || '').includes('验证码不正确'), `实际：${wrongRes.json?.msg}`)

  section('8. 管理员登录同样要验证码 + 密码强度校验')
  const adminNoCap = await mallApi('POST', '/api/mall/admin/login', {
    body: { username: 'admin@qq.com', password: 'Admin1234' }
  })
  check('管理员不带验证码 → 被拦', (adminNoCap.json?.msg || '').includes('请先获取图形验证码'),
    `实际：${adminNoCap.json?.msg}`)

  const weakPwd = await mallCaptcha()
  const weakRes = await mallApi('POST', '/api/mall/admin/login', {
    body: { username: 'admin@qq.com', password: '123456', ...weakPwd }
  })
  check('管理员弱密码 → 「密码不达标」', (weakRes.json?.msg || '').includes('密码不达标'),
    `实际：${weakRes.json?.msg}`)

  section('9. 买家注册（写共享 sys_user，返回 token）')
  const newEmail = `mall${Date.now().toString(36)}@qq.com`
  const rf = await mallCaptcha()
  const reg = await mallApi('POST', '/api/mall/user/register', {
    body: { username: newEmail, password: 'Mall1234', nickname: '商城新人', ...rf }
  })
  check('注册成功且直接返回 token', ok(reg) && typeof reg.json?.data?.token === 'string',
    reg.json?.msg || (reg.json?.data?.token ? '有 token' : '无 token'))
  check('注册返回的 token 可用于电商买家接口',
    ok(await mallApi('GET', '/api/mall/user/profile', { headers: { token: reg.json?.data?.token } })))

  // 注意：验证码只能取一次 —— 取两次会拿到两个不同的 key，key 和 code 对不上
  const wCap = await waimaiApi('GET', '/api/auth/captcha')
  const wKey2 = wCap.json?.data?.key
  const wCode2 = redis(['GET', `captcha:${wKey2}`])
  const regLogin = await waimaiApi('POST', '/api/user/login', {
    body: { username: newEmail, password: 'Mall1234', captchaKey: wKey2, captchaCode: wCode2 }
  })
  check('在电商注册的账号 → 可以直接在外卖登录（账号真正互通）',
    ok(regLogin), regLogin.json?.msg || '')

  console.log(`\nℹ️  本次注册了 ${newEmail}，清理 SQL：`)
  console.log(`   DELETE FROM sys_user WHERE username LIKE 'mall%@qq.com';`)
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
