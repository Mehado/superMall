// scripts/verify-avatar.mjs
// 买家头像：上传 + 保存 + 跨板块共享 验收（外卖 8080 / 电商 8081）。
//
// 覆盖：
//   外卖  POST /api/user/avatar            → 上传拿 OSS URL（目录 supermall/avatars/{userId}/）
//         PUT  /api/user/profile {avatar}  → 保存；只改头像不动昵称/手机号
//         PUT  /api/user/profile {avatar:""} → 清除
//   电商  POST /api/mall/user/file/avatar
//         PUT  /api/mall/user/profile {avatar}
//   校验  非图片 / 超 2MB / 未登录 被拒
//   共享  在电商改的头像，外卖 profile 立刻能看到（同一张 sys_user）
//
// 用法（需 8080 + 8081 + Redis 在跑，且 OSS 凭据有效）：
//   node scripts/verify-avatar.mjs
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'

const WAIMAI = String(process.env.WAIMAI_BASE || 'http://localhost:8080').replace(/\/$/, '')
const MALL = String(process.env.MALL_BASE || 'http://localhost:8081').replace(/\/$/, '')

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
  if (body !== undefined && !(body instanceof FormData)) h['Content-Type'] = 'application/json'
  const res = await fetch(base + path, {
    method,
    headers: h,
    body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body)
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
        /* next */
      }
    }
    if (!cliPath) throw new Error('找不到 redis-cli，可用 REDIS_CLI 指定路径')
  }
  const base = ['-h', process.env.REDIS_HOST || '127.0.0.1', '-p', process.env.REDIS_PORT || '6379',
    '-a', process.env.REDIS_PASSWORD || 'mehado']
  for (const a of [[...base, '--no-auth-warning', ...args], [...base, ...args]]) {
    try {
      const out = execFileSync(cliPath, a, { encoding: 'utf8' }).trim()
      if (out.startsWith('ERR ')) continue
      return out
    } catch {
      /* retry */
    }
  }
  throw new Error('redis-cli 执行失败')
}

async function takeoutCaptcha() {
  const r = await req(WAIMAI, 'GET', '/api/auth/captcha')
  const key = r.json?.data?.key
  if (!key) return {}
  return { captchaKey: key, captchaCode: redis(['GET', `captcha:${key}`]) }
}
async function mallCaptcha() {
  const r = await req(MALL, 'GET', '/api/mall/auth/captcha')
  const key = r.json?.data?.key
  if (!key) return {}
  return { captchaKey: key, captchaCode: redis(['GET', `mall:captcha:${key}`]) }
}

/* ---------------- 图片素材 ---------------- */
// 1x1 透明 PNG
const PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=='
const pngBytes = Buffer.from(PNG_BASE64, 'base64')

function pngForm() {
  const form = new FormData()
  form.append('file', new Blob([pngBytes], { type: 'image/png' }), 'avatar.png')
  return form
}
function bigForm() {
  const form = new FormData()
  const big = Buffer.alloc(2 * 1024 * 1024 + 1, 1)
  form.append('file', new Blob([big], { type: 'image/png' }), 'big.png')
  return form
}
function txtForm() {
  const form = new FormData()
  form.append('file', new Blob([Buffer.from('hello')], { type: 'text/plain' }), 'a.txt')
  return form
}

try {
  section('1. 登录（买家，外卖 / 电商同一账号）')
  const tCap = await takeoutCaptcha()
  const tLogin = await req(WAIMAI, 'POST', '/api/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', ...tCap }
  })
  const takeoutToken = tLogin.json?.data?.token
  check('外卖买家登录成功', ok(tLogin) && !!takeoutToken, tLogin.json?.msg || '')

  const mCap = await mallCaptcha()
  const mLogin = await req(MALL, 'POST', '/api/mall/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', ...mCap }
  })
  const mallToken = mLogin.json?.data?.token
  check('电商买家登录成功（同账号）', ok(mLogin) && !!mallToken, mLogin.json?.msg || '')

  const userId = tLogin.json?.data?.id
  const before = (await req(WAIMAI, 'GET', '/api/user/profile', { headers: { token: takeoutToken } })).json?.data || {}
  console.log(`  当前资料：nickname=${before.nickname}, phone=${before.phone}, avatar=${before.avatar}`)

  section('2. 外卖：上传头像')
  check('未登录上传 → 401', (await req(WAIMAI, 'POST', '/api/user/avatar', { body: pngForm() })).status === 401)
  check('非图片（.txt）→ 被拒',
    !ok(await req(WAIMAI, 'POST', '/api/user/avatar', { headers: { token: takeoutToken }, body: txtForm() })))
  check('超过 2MB → 被拒',
    !ok(await req(WAIMAI, 'POST', '/api/user/avatar', { headers: { token: takeoutToken }, body: bigForm() })))

  const up1 = await req(WAIMAI, 'POST', '/api/user/avatar', { headers: { token: takeoutToken }, body: pngForm() })
  const url1 = up1.json?.data
  check('合法 PNG → 返回 URL', ok(up1) && typeof url1 === 'string', url1 || up1.json?.msg || '')
  check(`URL 落在头像目录 supermall/avatars/${userId}/`,
    String(url1).includes(`/supermall/avatars/${userId}/`), String(url1))
  const head1 = url1 ? await fetch(url1, { method: 'HEAD' }).catch(() => null) : null
  check('OSS 上真实可访问（HEAD 200）', head1?.status === 200, `status=${head1?.status}`)

  section('3. 外卖：保存到资料（只改头像，不动昵称/手机号）')
  check('PUT /api/user/profile {avatar} → 成功',
    ok(await req(WAIMAI, 'PUT', '/api/user/profile', { headers: { token: takeoutToken }, body: { avatar: url1 } })))
  const after1 = (await req(WAIMAI, 'GET', '/api/user/profile', { headers: { token: takeoutToken } })).json?.data || {}
  check('头像已保存', after1.avatar === url1, `avatar=${after1.avatar}`)
  check('昵称未被冲掉', after1.nickname === before.nickname, `${before.nickname} → ${after1.nickname}`)
  check('手机号未被冲掉', (after1.phone || '') === (before.phone || ''), `${before.phone} → ${after1.phone}`)

  section('4. 电商：上传 + 保存（同一张 sys_user，两边互通）')
  const up2 = await req(MALL, 'POST', '/api/mall/user/file/avatar', { headers: { token: mallToken }, body: pngForm() })
  const url2 = up2.json?.data
  check('电商上传成功', ok(up2) && typeof url2 === 'string', url2 || up2.json?.msg || '')
  check(`电商 URL 也在头像目录 supermall/avatars/${userId}/`,
    String(url2).includes(`/supermall/avatars/${userId}/`), String(url2))
  check('PUT /api/mall/user/profile {avatar} → 成功',
    ok(await req(MALL, 'PUT', '/api/mall/user/profile', { headers: { token: mallToken }, body: { avatar: url2 } })))

  const cross = (await req(WAIMAI, 'GET', '/api/user/profile', { headers: { token: takeoutToken } })).json?.data || {}
  check('电商改的头像，外卖 profile 立刻可见（共享表）', cross.avatar === url2, `外卖看到 avatar=${cross.avatar}`)
  const cross2 = (await req(MALL, 'GET', '/api/mall/user/profile', { headers: { token: mallToken } })).json?.data || {}
  check('电商 profile 也返回头像', cross2.avatar === url2)

  section('5. 清除头像')
  check('PUT {avatar:""} → 成功',
    ok(await req(WAIMAI, 'PUT', '/api/user/profile', { headers: { token: takeoutToken }, body: { avatar: '' } })))
  const cleared = (await req(WAIMAI, 'GET', '/api/user/profile', { headers: { token: takeoutToken } })).json?.data || {}
  check('头像已清空（前端会回退首字母）', !cleared.avatar, `avatar="${cleared.avatar}"`)
  check('非法头像地址被拒',
    !ok(await req(WAIMAI, 'PUT', '/api/user/profile', { headers: { token: takeoutToken }, body: { avatar: 'not-a-url' } })))

  section('6. 还原（别弄丢演示账号上真人设置的头像）')
  check('头像已还原为用例开始时的值',
    ok(await req(WAIMAI, 'PUT', '/api/user/profile', {
      headers: { token: takeoutToken },
      // 原值是 null 时只能还原成空串（接口没法把字段写回 NULL），对前端等价于“无头像”
      body: { avatar: before.avatar || '' }
    })))
  const finalProfile = (await req(WAIMAI, 'GET', '/api/user/profile', { headers: { token: takeoutToken } })).json?.data || {}
  check('还原后与开始时一致', (finalProfile.avatar || '') === (before.avatar || ''),
    `before=${before.avatar} after=${finalProfile.avatar}`)

  console.log('\nℹ️  本次上传了 2 张测试图到 OSS（supermall/avatars/），如需清理可在 OSS 控制台按前缀删除。')
  console.log('ℹ️  头像 / 昵称 / 手机号都已还原成用例开始时的样子。')
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
