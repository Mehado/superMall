// scripts/ui-smoke-onboarding.mjs
// 浏览器（Playwright）实测：外卖「商家入驻 → 平台审核 → 商家登录」+「骑手注册」+ 各入口可达。
//
// 与 verify-platform-onboarding.mjs 的区别：那个打 HTTP 接口，这个**真的开浏览器点页面**，
// 覆盖表单校验、跳转、弹窗、toast 等前端交互。
//
// 用法（需 前端 :5178 + 后端 :8080 + Redis 在跑）：
//   node scripts/ui-smoke-onboarding.mjs
//   node scripts/ui-smoke-onboarding.mjs --base http://localhost:5178 --headed
import { execFileSync } from 'node:child_process'
import { createRequire } from 'node:module'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(process.cwd())
const require = createRequire(path.join(ROOT, 'frontend', 'package.json'))
// 工程里装的是 playwright-core（浏览器已在本机 ms-playwright 缓存里），不是完整版 playwright
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

/* ---------------- Redis：读验证码答案 ---------------- */
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
      // redis-cli 在某些参数形式下会把错误打到 stdout 且退出码 0，这里显式拦掉
      if (out.startsWith('ERR ') || out.startsWith('WRONGTYPE')) continue
      return out
    } catch {
      /* retry */
    }
  }
  throw new Error('redis-cli 执行失败')
}

/* ---------------- 浏览器 ---------------- */

const ts = Date.now().toString(36)
const SHOP = `浏览器小馆${ts}`
const MERCHANT_EMAIL = `uishop${ts}@163.com`
const RIDER_EMAIL = `uirider${ts}@gmail.com`
const PWD = 'Mall1234'

const captchaStore = []  // [{ key, image }]
let page

/**
 * 填验证码：**按页面实际显示的那张图**找到它的 key，再去 Redis 读答案。
 *
 * 不能简单用「最后一个 key」——页面挂载 + 手动刷新可能并发两张，
 * 谁后到不确定。这里把 DOM 里 img 的 base64 和我们捕获到的响应对齐，
 * 显示的是哪张就用哪张的答案，绝对不会错位。
 */
async function fillCaptcha(timeout = 12000) {
  const img = page.locator('img[alt="验证码"]')
  await img.waitFor({ state: 'visible', timeout })
  const deadline = Date.now() + timeout
  let key = null
  while (Date.now() < deadline) {
    const src = await img.getAttribute('src').catch(() => null)
    if (src && src.startsWith('data:image/png;base64,')) {
      const b64 = src.slice('data:image/png;base64,'.length)
      const hit = captchaStore.find((x) => x.image === b64)
      if (hit) {
        key = hit.key
        break
      }
    }
    await page.waitForTimeout(120)
  }
  if (!key) throw new Error('无法把页面上的验证码图与接口响应对应起来')
  const code = redis(['GET', `captcha:${key}`])
  if (!code) throw new Error(`Redis 取不到 captcha:${key}`)
  await page.locator(CAPTCHA_INPUT).fill(code)
  if (process.env.UI_DEBUG) console.log(`  [captcha] key=${key} code=${code} storeSize=${captchaStore.length}`)
  return code
}

async function waitUntil(fn, timeout = 8000, step = 150) {
  const deadline = Date.now() + timeout
  for (;;) {
    if (await fn()) return true
    if (Date.now() > deadline) return false
    await page.waitForTimeout(step)
  }
}

const CAPTCHA_INPUT = 'input[placeholder="输入右侧 4 位字符"]'

/** 打开一个前端路由并等接口静默 */
async function open(route) {
  await page.goto(BASE + route, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
}

async function shot(name) {
  await page.screenshot({ path: path.join(SHOT_DIR, `${name}.png`), fullPage: true })
}

async function main() {
  const browser = await chromium.launch({ headless: !HEADED })
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  page = await ctx.newPage()
  const consoleErrors = []
  page.on('console', (m) => {
    if (m.type() === 'error') consoleErrors.push(m.text())
  })
  page.on('pageerror', (e) => consoleErrors.push(String(e.message)))
  page.on('request', (r) => {
    if (process.env.UI_DEBUG && r.method() === 'POST' && /\/api\/(merchant\/apply|platform\/login|user\/login|delivery\/register)/.test(r.url())) {
      console.log('  [req]', r.url().replace(/^.*\/api/, '/api'), (r.postData() || '').slice(0, 220))
    }
  })
  page.on('response', async (res) => {
    const u = res.url()
    if (u.includes('/api/auth/captcha') || u.includes('/api/mall/auth/captcha')) {
      try {
        const j = await res.json()
        if (j?.data?.key) captchaStore.push({ key: j.data.key, image: j.data.image || '' })
      } catch {
        /* ignore */
      }
    }
  })

  /* ============ 1. 入口可达 ============ */
  section('1. 入口可达性')

  await open('/')
  const portalText = await page.locator('body').innerText()
  check('主页面 / 有「商家入驻」入口', portalText.includes('商家入驻'))
  check('主页面 / 有「骑手注册」入口', portalText.includes('骑手注册'))

  await open('/login')
  const loginText = await page.locator('body').innerText()
  check('登录页有「我要开店（商家入驻）」', loginText.includes('我要开店'))
  check('登录页有「骑手注册」', loginText.includes('骑手注册'))
  check('登录页有「平台运营」Tab', loginText.includes('平台运营'))

  await open('/register?scope=food')
  check('买家注册页有「我要开店」引导', (await page.locator('body').innerText()).includes('我要开店'))

  // 点登录页的「我要开店」应真的跳到入驻页
  await open('/login')
  await page.getByRole('link', { name: '我要开店（商家入驻）' }).click()
  await page.waitForURL('**/merchant/apply', { timeout: 8000 }).catch(() => {})
  check('登录页点「我要开店」跳到 /merchant/apply', page.url().includes('/merchant/apply'), page.url())

  /* ============ 2. 商家入驻申请（页面填写） ============ */
  section('2. 商家入驻申请')

  
  await open('/merchant/apply')
  await page.locator('input[placeholder="例如：川味小馆"]').fill(SHOP)
  await page.locator('input[placeholder="张三"]').fill('王五')
  await page.locator('input[placeholder="13900000000"]').fill('13900000077')
  await page.locator('input[placeholder="北京市朝阳区望京街 9 号"]').fill('北京市朝阳区望京街 77 号')
  await page.locator('input[placeholder="选填，一句话介绍你的店"]').fill('浏览器实测小店')
  await page.locator('input[placeholder="newshop@163.com"]').fill(MERCHANT_EMAIL)
  await page.locator('input[placeholder="8-32 位，含大写 + 小写 + 数字"]').fill(PWD)
  await page.locator('input[placeholder="再输入一次"]').fill(PWD)
  await fillCaptcha()
  await page.locator('button[type="submit"]').click()

  const gotPending = await waitUntil(async () =>
    (await page.locator('body').innerText()).includes('待审核')
  )
  check('提交后页面出现「待审核」结果卡片', gotPending,
    gotPending ? '' : (await page.locator('body').innerText()).replace(/\s+/g, ' ').slice(0, 200))
  await shot('01-merchant-apply-pending')

  /* ============ 3. 平台运营登录 + 通过审核 ============ */
  section('3. 平台运营审核')

  
  await open('/login?role=platform-admin')
  await page.locator('input[placeholder="请输入邮箱账号"]').fill('platform@163.com')
  await page.locator('input[placeholder="请输入密码"]').fill('Admin1234')
  await fillCaptcha()
  await page.locator('button[type="submit"]').click()
  const platformIn = await waitUntil(async () => page.url().includes('/platform'))
  check('平台运营登录后进入 /platform', platformIn, page.url())
  check('平台页显示「商家入驻审核」', (await page.locator('body').innerText()).includes('商家入驻审核'))
  await shot('02-platform-audit-list')

  const row = page.locator('table tbody tr', { hasText: SHOP })
  check('待审核列表出现刚提交的店铺', (await row.count()) > 0, `找到 ${await row.count()} 行`)

  await row.getByRole('button', { name: '通过' }).first().click()
  await page.getByRole('button', { name: '确认通过' }).click()
  const approved = await waitUntil(async () => (await row.count()) === 0)
  check('点「通过」后该申请从待审核列表消失', approved)
  await shot('03-platform-approved')

  /* ============ 4. 新商家登录 → 进商家端 ============ */
  section('4. 新商家登录')

  
  await open('/login?role=merchant')
  await page.locator('input[placeholder="请输入邮箱账号"]').fill(MERCHANT_EMAIL)
  await page.locator('input[placeholder="请输入密码"]').fill(PWD)
  await fillCaptcha()
  await page.locator('button[type="submit"]').click()
  const merchantIn = await waitUntil(async () => page.url().includes('/merchant'))
  check('审核通过后新商家能登录并进入商家端', merchantIn, page.url())
  check('商家端侧栏出现「商品管理」', (await page.locator('body').innerText()).includes('商品管理'))
  await shot('04-merchant-dashboard')

  /* ============ 5. 骑手注册 → 自动登录进骑手端 ============ */
  section('5. 骑手注册')

  
  await open('/rider/register')
  await page.locator('input[placeholder="例如：疾风"]').fill(`浏览器骑手${ts}`)
  await page.locator('input[placeholder="13700000000"]').fill('13700000077')
  await page.locator('input[placeholder="rider3@gmail.com"]').fill(RIDER_EMAIL)
  await page.locator('input[placeholder="8-32 位，含大写 + 小写 + 数字"]').fill(PWD)
  await page.locator('input[placeholder="再输入一次"]').fill(PWD)
  await fillCaptcha()
  await page.locator('button[type="submit"]').click()
  const riderIn = await waitUntil(async () => /\/rider$/.test(new URL(page.url()).pathname))
  check('骑手注册后自动登录并进入 /rider', riderIn, page.url())
  await shot('05-rider-registered')

  /* ============ 6. 骑手出现在平台「骑手管理」 ============ */
  section('6. 平台骑手管理')

  await open('/platform/riders')
  const hasRider = await waitUntil(async () =>
    (await page.locator('body').innerText()).includes(RIDER_EMAIL)
  )
  check('平台骑手列表能看到新注册骑手', hasRider)
  await shot('06-platform-riders')

  /* ============ 7. 买家「我的」页推广位 ============ */
  section('7. 外卖端「我的」页开店入口')

  
  await open('/login?role=delivery-user')
  await page.locator('input[placeholder="请输入邮箱账号"]').fill('user1@qq.com')
  await page.locator('input[placeholder="请输入密码"]').fill(PWD)
  await fillCaptcha()
  await page.locator('button[type="submit"]').click()
  const userIn = await waitUntil(async () => page.url().includes('/food'))
  check('买家登录成功', userIn, page.url())

  await open('/food/me')
  const profText = await page.locator('body').innerText()
  check('「我的」页有开店铺推广卡', profText.includes('也想在平台上做生意？'))
  check('「我的」页有「我要开店」按钮', profText.includes('我要开店'))
  await shot('07-food-profile-join')

  /* ============ 8. 无 JS 报错 ============ */
  section('8. 控制台')
  const realErrors = consoleErrors.filter((t) => !t.includes('favicon'))
  check('页面无 JS 运行时错误', realErrors.length === 0, realErrors.slice(0, 3).join(' | '))

  await browser.close()

  console.log(`\nℹ️  截图目录：${SHOT_DIR}`)
  console.log(`ℹ️  本次造了测试数据，清理 SQL：`)
  console.log(`   DELETE p FROM product p JOIN merchant m ON m.id=p.merchant_id WHERE m.username = '${MERCHANT_EMAIL}';`)
  console.log(`   DELETE s FROM sku s JOIN merchant m ON m.id=s.merchant_id WHERE m.username = '${MERCHANT_EMAIL}';`)
  console.log(`   DELETE c FROM category c JOIN merchant m ON m.id=c.merchant_id WHERE m.username = '${MERCHANT_EMAIL}';`)
  console.log(`   DELETE FROM merchant WHERE username = '${MERCHANT_EMAIL}';`)
  console.log(`   DELETE FROM deliveryman WHERE username = '${RIDER_EMAIL}';`)
}

main()
  .catch((e) => check('脚本执行完成', false, `抛异常：${e.message}`))
  .finally(() => {
    console.log(`\n══════════════════════════════════════`)
    console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
    console.log(`══════════════════════════════════════`)
    process.exit(failed ? 1 : 0)
  })
