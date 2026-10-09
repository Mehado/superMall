// scripts/ui-smoke-login-redirect.mjs
// 浏览器（Playwright）实测：六端登录的「redirect 归属身份」问题。
//
// 背景（2026-09-21 修的真实 bug）：
//   六端共用一个工程，被拦下来时会跳 `/login?role=A&redirect=A的页面`。
//   用户在登录页把身份筹码切到 B 再登录 —— 旧代码会照 redirect 跳到 A 的页面，
//   而路由守卫发现没有 A 的 token，又把它拦回 `/login?role=A&redirect=...`。
//   表现：**提示登录成功，然后卡在登录页**；偶尔「进到界面了，点个人中心又要再登录」。
//
// 覆盖：
//   ① 跨身份 redirect：切到另一个身份登录 → 必须落到该身份主页（不再弹回登录页）
//   ② 同身份 redirect：登录后要落到原来想去的页面（含 /shop/me 个人中心）
//   ③ 公开页：游客直接访问 /shop 不跳登录；NavRail 未登录时是「登录」入口
//   ④ 守卫加固：已登录身份访问 `/login?role=自己&redirect=别人的页面` → 回自己主页
//   ⑤ 全程无 JS 运行时错误
//
// 用法（需 前端 :5178 + 后端 :8080/:8081 + Redis 在跑）：
//   node scripts/ui-smoke-login-redirect.mjs
//   node scripts/ui-smoke-login-redirect.mjs --headed
import { execFileSync } from 'node:child_process'
import { createRequire } from 'node:module'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(process.cwd())
const require = createRequire(path.join(ROOT, 'frontend', 'package.json'))
const { chromium } = require('playwright-core')

const arg = (name, def) => {
  const i = process.argv.indexOf(`--${name}`)
  if (i === -1) return def
  const v = process.argv[i + 1]
  return v === undefined || v.startsWith('--') ? true : v
}
const BASE = String(arg('base', process.env.UI_BASE || 'http://localhost:5178')).replace(/\/$/, '')
const HEADED = Boolean(arg('headed', false))
const SHOT_DIR = path.join(ROOT, 'logs', 'ui-smoke')
fs.mkdirSync(SHOT_DIR, { recursive: true })

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

/* ---------------- Redis（读图形验证码答案） ---------------- */
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

/* ---------------- 账号 ---------------- */
const ACCOUNTS = {
  '商城买家': { chip: '商城买家', username: 'user1@qq.com', password: 'Mall1234', home: '/shop' },
  '商城运营': { chip: '商城运营', username: 'admin@qq.com', password: 'Admin1234', home: '/admin' }
}

/* ---------------- 浏览器 ---------------- */
const captchas = []
const errors = []
let page
let ctx
let browser

const pathOf = (url) => new URL(url).pathname
const queryOf = (url) => new URL(url).searchParams

async function fresh() {
  if (ctx) await ctx.close()
  ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  page = await ctx.newPage()
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', (e) => errors.push(String(e.message)))
  page.on('response', async (res) => {
    const u = res.url()
    if (u.includes('/auth/captcha')) {
      try {
        const j = await res.json()
        if (j?.data?.key) captchas.push({ key: j.data.key, image: j.data.image || '' })
      } catch { /* ignore */ }
    }
  })
}

/** 等页面上的验证码图与接口响应对上（切换身份会重新拉一张，所以要循环重试） */
async function currentCaptcha() {
  const img = page.locator('img[alt="验证码"]')
  await img.waitFor({ state: 'visible', timeout: 12000 })
  const deadline = Date.now() + 12000
  while (Date.now() < deadline) {
    const src = await img.getAttribute('src').catch(() => null)
    if (src && src.startsWith('data:image/png;base64,')) {
      const b64 = src.slice('data:image/png;base64,'.length)
      // 取最后一张匹配的（切换身份后旧图还在数组里，新图在末尾）
      const hit = [...captchas].reverse().find((x) => x.image === b64)
      if (hit) return { key: hit.key, mall: true }
    }
    await page.waitForTimeout(120)
  }
  throw new Error('无法把验证码图与接口响应对应起来')
}

/**
 * 轮询收集提示条文本 —— toast 只活 2.4~3.2 秒，
 * 等页面跳完再去读就什么都没了（这里踩过一次）。
 */
async function collectToasts(ms = 3200) {
  const seen = new Set()
  const deadline = Date.now() + ms
  while (Date.now() < deadline) {
    for (const t of await page.locator('.toast').allTextContents()) seen.add(t)
    await page.waitForTimeout(120)
  }
  return [...seen]
}

/** 在当前登录页填表提交（chip 传了就先把身份筹码切过去）；返回本次出现的提示条 */
async function submitLogin(who) {
  const acc = ACCOUNTS[who]
  await page.locator('.role-chip', { hasText: acc.chip }).click()
  await page.waitForTimeout(900) // 等切换身份后重新拉验证码
  const { key, mall } = await currentCaptcha()
  const code = redis(['GET', `${mall ? 'mall:' : ''}captcha:${key}`])
  await page.locator('input[placeholder="请输入邮箱账号"]').fill(acc.username)
  await page.locator('input[placeholder="请输入密码"]').fill(acc.password)
  await page.locator('input[placeholder="输入右侧 4 位字符"]').fill(code)
  await page.locator('button[type="submit"]').click()
  const toasts = await collectToasts(3200)
  await page.waitForTimeout(600)
  return toasts
}

/** 去一个受保护页 → 应被拦到登录页；返回登录页的 role / redirect 参数 */
async function gotoGuarded(route) {
  await fresh()
  await page.goto(BASE + route, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  const q = queryOf(page.url())
  return { role: q.get('role'), redirect: q.get('redirect'), path: pathOf(page.url()) }
}

try {
  browser = await chromium.launch({ headless: !HEADED })

  // ============================================================
  section('1. 未登录访问受保护页：应带 role + redirect 到登录页')
  const cart = await gotoGuarded('/shop/cart')
  check('访问 /shop/cart → 到登录页', cart.path === '/login', cart.path)
  check('带上 role=mall-user', cart.role === 'mall-user', String(cart.role))
  check('带上 redirect=/shop/cart', cart.redirect === '/shop/cart', String(cart.redirect))

  const adminPage = await gotoGuarded('/admin/products')
  check('访问 /admin/products → 到登录页且 role=mall-admin',
    adminPage.path === '/login' && adminPage.role === 'mall-admin', `${adminPage.path} role=${adminPage.role}`)
  check('redirect=/admin/products', adminPage.redirect === '/admin/products', String(adminPage.redirect))

  // ============================================================
  section('2. 跨身份 redirect：切到另一个身份登录 → 落该身份主页（本次修复点）')
  // 从 /shop/cart 被拦下（redirect 是买家页面），却在登录页切到「商城运营」
  const crossA = await gotoGuarded('/shop/cart')
  check('前置：redirect 属于商城买家', crossA.redirect === '/shop/cart')
  const toastsA = await submitLogin('商城运营')
  check('商城运营登录后落到 /admin（不再弹回登录页）', pathOf(page.url()) === '/admin', page.url())
  const tokensA = await page.evaluate(() =>
    Object.keys(localStorage).filter((k) => k.startsWith('supermall:token:')))
  check('只写了 mall-admin 的 token', tokensA.length === 1 && tokensA[0].endsWith('mall-admin'), tokensA.join(','))
  check('提示「登录成功」', toastsA.some((t) => t.includes('登录成功')), toastsA.join(' | '))
  check('额外提示该页面属于别的身份',
    toastsA.some((t) => t.includes('需要') && t.includes('身份')), toastsA.join(' | '))

  // 反向：从 /admin/products 被拦下（redirect 是运营页面），切到「商城买家」
  const crossB = await gotoGuarded('/admin/products')
  check('前置：redirect 属于商城运营', crossB.redirect === '/admin/products')
  await submitLogin('商城买家')
  check('商城买家登录后落到 /shop（不再弹回登录页）', pathOf(page.url()) === '/shop', page.url())

  // ============================================================
  section('3. 同身份 redirect：登录后要落到原目标（含个人中心）')
  const me = await gotoGuarded('/shop/me')
  check('前置：redirect=/shop/me', me.redirect === '/shop/me', String(me.redirect))
  await submitLogin('商城买家')
  check('登录后直达「个人中心」/shop/me', pathOf(page.url()) === '/shop/me', page.url())
  const navrailName = await page.locator('.navrail-foot a[aria-label="我的"]').count()
  check('个人中心页里 NavRail 显示「我的」（说明确实登录了）', navrailName === 1, `count=${navrailName}`)
  await page.screenshot({ path: path.join(SHOT_DIR, 'login-redirect-me.png') })

  const prod = await gotoGuarded('/admin/products')
  await submitLogin('商城运营')
  check('运营登录后直达 /admin/products（redirect 被保留）',
    pathOf(page.url()) === '/admin/products', page.url())

  // ============================================================
  section('4. 公开页与未登录入口')
  await fresh()
  await page.goto(BASE + '/shop', { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  check('游客可停留在公开页 /shop（不跳登录）', pathOf(page.url()) === '/shop', page.url())
  const loginLink = await page.locator('.navrail-foot a[aria-label="登录"]').count()
  check('未登录时 NavRail 底部是「登录」入口', loginLink === 1, `count=${loginLink}`)
  const href = await page.locator('.navrail-foot a[aria-label="登录"]').getAttribute('href')
  check('该入口带上 role=mall-user 与 redirect', /role=mall-user/.test(href), String(href))

  // ============================================================
  section('5. 守卫加固：已登录身份 + 别人的 redirect → 回自己主页')
  const s5 = await gotoGuarded('/shop/cart')
  await submitLogin('商城运营') // 现在登录态是 mall-admin
  check('第 1 次登录落到 /admin', pathOf(page.url()) === '/admin', page.url())
  // 直接敲一个「自己是 mall-admin，但 redirect 是买家页面」的登录页地址
  await page.goto(`${BASE}/login?role=mall-admin&redirect=/shop/me`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.waitForTimeout(1200)
  check('守卫不跟着别人的 redirect 跳，回到 /admin', pathOf(page.url()) === '/admin', page.url())

  // ============================================================
  section('6. 没有 JS 运行时错误')
  const realErrors = errors.filter((e) => !/favicon|ResizeObserver|Download the Vue/i.test(e))
  check('无 JS 运行时错误', realErrors.length === 0, realErrors.slice(0, 2).join(' | '))

  await browser.close()
} catch (e) {
  console.error('\n💥 脚本异常：', e.message)
  failed++
  if (browser) await browser.close().catch(() => {})
} finally {
  console.log(`\n结果：${results.length - failed}/${results.length} 通过`)
  if (failed > 0) process.exitCode = 1
}

// 说明：本脚本只做登录/跳转，**不写任何业务数据**（不建账号、不下单、不改资料），
// 所以不需要「快照 → 还原」。用到的账号都是演示账号：user1@qq.com / admin@qq.com。
void section
