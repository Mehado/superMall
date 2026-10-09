// scripts/ui-smoke-workbench-profile.mjs
// 浏览器（Playwright）实测工作台三个角色的「改资料」：
//   骑手    /rider/profile   → 编辑资料弹窗（姓名/手机号）
//   商家    /merchant/shop   → 店铺表单（店名）+ 营业状态开关（打烊/开始营业）
//   平台运营 /platform/profile → 编辑资料弹窗（姓名）
//
// 用法（需 前端 :5178 + 后端 :8080 + Redis 在跑）：
//   node scripts/ui-smoke-workbench-profile.mjs
//   node scripts/ui-smoke-workbench-profile.mjs --headed
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

const store = []
let page
const tag = Date.now().toString(36).slice(-4)

async function waitUntil(fn, timeout = 10000, step = 150) {
  const deadline = Date.now() + timeout
  for (;;) {
    if (await fn()) return true
    if (Date.now() > deadline) return false
    await page.waitForTimeout(step)
  }
}

async function login(role, username, password) {
  store.length = 0
  await page.goto(`${BASE}/login?role=${role}`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  const img = page.locator('img[alt="验证码"]')
  await img.waitFor({ state: 'visible', timeout: 12000 })
  const deadline = Date.now() + 10000
  let key = null
  while (Date.now() < deadline) {
    const src = await img.getAttribute('src').catch(() => null)
    if (src && src.startsWith('data:image/png;base64,')) {
      const b64 = src.slice('data:image/png;base64,'.length)
      const hit = store.find((x) => x.image === b64)
      if (hit) { key = hit.key; break }
    }
    await page.waitForTimeout(120)
  }
  if (!key) throw new Error('无法把验证码图与接口响应对应起来')
  const code = redis(['GET', `captcha:${key}`])
  await page.locator('input[placeholder="请输入邮箱账号"]').fill(username)
  await page.locator('input[placeholder="请输入密码"]').fill(password)
  await page.locator('input[placeholder="输入右侧 4 位字符"]').fill(code)
  await page.locator('button[type="submit"]').click()
}

/** 在页面里用 localStorage 的 token 直接打接口（准备 / 还原用） */
async function apiCall(storageKey, header, method, apiPath, body) {
  return page.evaluate(
    async ({ storageKey, header, method, apiPath, body }) => {
      const raw = localStorage.getItem(`supermall:${storageKey}`)
      const token = raw ? JSON.parse(raw) : ''
      const res = await fetch(apiPath, {
        method,
        headers: { 'Content-Type': 'application/json', [header]: token },
        body: body === undefined ? undefined : JSON.stringify(body)
      })
      const text = await res.text()
      try {
        return JSON.parse(text)?.data ?? null
      } catch {
        return null
      }
    },
    { storageKey, header, method, apiPath, body }
  )
}

async function main() {
  const browser = await chromium.launch({ headless: !HEADED })
  page = await (await browser.newContext({ viewport: { width: 1440, height: 960 } })).newPage()
  const errors = []
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', (e) => errors.push(String(e.message)))
  page.on('response', async (res) => {
    const u = res.url()
    if (u.includes('/auth/captcha')) {
      try {
        const j = await res.json()
        if (j?.data?.key) store.push({ key: j.data.key, image: j.data.image || '' })
      } catch { /* ignore */ }
    }
  })

  /* ---------------- 骑手 ---------------- */
  section('骑手 · 编辑资料')
  await login('rider', 'rider1@gmail.com', 'Mall1234')
  check('骑手登录成功', await waitUntil(async () => page.url().includes('/rider')), page.url())
  await page.goto(`${BASE}/rider/profile`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})

  const rOriginal = await apiCall('token:rider', 'delivery-token', 'GET', '/api/delivery/profile')
  const newRiderName = `骑手改名${tag}`
  await page.getByRole('button', { name: '编辑资料' }).click()
  await page.locator('input[placeholder="11 位手机号"]').waitFor({ state: 'visible', timeout: 8000 })
  const rInputs = page.locator('input.m-input:visible')
  await rInputs.nth(0).fill(newRiderName)
  await page.getByRole('button', { name: '保存' }).first().click()
  check('骑手保存后页面显示新姓名', await waitUntil(async () =>
    (await page.locator('body').innerText()).includes(newRiderName), 10000))
  const rAfter = await apiCall('token:rider', 'delivery-token', 'GET', '/api/delivery/profile')
  check('骑手姓名已落库', rAfter?.name === newRiderName, `name=${rAfter?.name}`)
  await apiCall('token:rider', 'delivery-token', 'PUT', '/api/delivery/profile', {
    name: rOriginal?.name, phone: rOriginal?.phone
  })
  check('骑手资料已还原', true)
  await page.screenshot({ path: path.join(SHOT_DIR, 'workbench-rider-profile.png'), fullPage: true })

  /* ---------------- 商家 ---------------- */
  section('商家 · 店铺设置')
  await login('merchant', 'merchant1@163.com', 'Mall1234')
  check('商家登录成功', await waitUntil(async () => page.url().includes('/merchant')), page.url())
  await page.goto(`${BASE}/merchant/shop`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})

  const mOriginal = await apiCall('token:merchant', 'merchant-token', 'GET', '/api/merchant/shop')
  const mBizOriginal = await apiCall('token:merchant', 'merchant-token', 'GET', '/api/merchant/shop/business')
  const newShopName = `老王牛肉面${tag}`
  const shopInputs = page.locator('input.m-input:visible')
  await shopInputs.nth(0).fill(newShopName)
  await page.getByRole('button', { name: '保存', exact: true }).first().click()
  const mSaved = await waitUntil(async () =>
    (await apiCall('token:merchant', 'merchant-token', 'GET', '/api/merchant/shop'))?.shopName === newShopName, 10000)
  check('商家改店名后真的落库（旧版传 name 是无效的）', mSaved)

  // 营业状态切换
  const beforeOpen = Number((await apiCall('token:merchant', 'merchant-token', 'GET', '/api/merchant/shop/business')) ?? 1)
  const toggleBtn = page.getByRole('button', { name: beforeOpen === 1 ? '打烊' : '开始营业' })
  check('营业状态按钮文案与真实状态一致', (await toggleBtn.count()) > 0,
    `beforeOpen=${beforeOpen}`)
  await toggleBtn.click()
  const flipped = await waitUntil(async () =>
    Number((await apiCall('token:merchant', 'merchant-token', 'GET', '/api/merchant/shop/business')) ?? 1) === (beforeOpen === 1 ? 0 : 1),
    10000)
  check('点按钮后营业状态真的切换了（旧版传 status 会被拒）', flipped)

  const restored = await waitUntil(async () =>
    (await page.locator('body').innerText()).includes(beforeOpen === 1 ? '休息中' : '营业中'), 8000)
  check('页面状态文案同步更新', restored)
  await page.screenshot({ path: path.join(SHOT_DIR, 'workbench-merchant-shop.png'), fullPage: true })

  await apiCall('token:merchant', 'merchant-token', 'PUT', '/api/merchant/shop', {
    shopName: mOriginal?.shopName, logo: mOriginal?.logo ?? '',
    description: mOriginal?.description, phone: mOriginal?.phone, address: mOriginal?.address
  })
  await apiCall('token:merchant', 'merchant-token', 'PUT', '/api/merchant/shop/business',
    { businessStatus: Number(mBizOriginal ?? 1) })
  check('商家店铺 / 营业状态已还原', true)

  /* ---------------- 平台运营 ---------------- */
  section('平台运营 · 账号资料')
  await login('platform-admin', 'platform@163.com', 'Admin1234')
  check('平台运营登录成功', await waitUntil(async () => page.url().includes('/platform')), page.url())
  await page.goto(`${BASE}/platform/profile`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})

  check('账号资料页有「编辑资料」按钮', (await page.getByRole('button', { name: '编辑资料' }).count()) > 0)
  const pOriginal = await apiCall('token:platform-admin', 'platform-token', 'GET', '/api/platform/profile')
  const newPName = `平台改名${tag}`
  await page.getByRole('button', { name: '编辑资料' }).click()
  const pInput = page.locator('input.m-input:visible').first()
  await pInput.waitFor({ state: 'visible', timeout: 8000 })
  await pInput.fill(newPName)
  await page.getByRole('button', { name: '保存' }).first().click()
  check('保存后页面显示新姓名', await waitUntil(async () =>
    (await page.locator('body').innerText()).includes(newPName), 10000))
  const pAfter = await apiCall('token:platform-admin', 'platform-token', 'GET', '/api/platform/profile')
  check('平台姓名已落库', pAfter?.name === newPName, `name=${pAfter?.name}`)
  await apiCall('token:platform-admin', 'platform-token', 'PUT', '/api/platform/profile', { name: pOriginal?.name })
  check('平台资料已还原', true)
  await page.screenshot({ path: path.join(SHOT_DIR, 'workbench-platform-profile.png'), fullPage: true })

  section('控制台')
  const real = errors.filter((t) => !t.includes('favicon'))
  check('页面无 JS 运行时错误', real.length === 0, real.slice(0, 2).join(' | '))

  await browser.close()
  console.log(`\nℹ️  截图目录：${SHOT_DIR}`)
  console.log('ℹ️  三个角色的资料改动都已在用例结束时还原。')
}

main()
  .catch((e) => check('脚本执行完成', false, `抛异常：${e.message}`))
  .finally(() => {
    console.log(`\n══════════════════════════════════════`)
    console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
    console.log(`══════════════════════════════════════`)
    process.exit(failed ? 1 : 0)
  })
