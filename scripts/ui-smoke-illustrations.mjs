// scripts/ui-smoke-illustrations.mjs
// 浏览器（Playwright）实测：空态 / 404 插图真的渲染出来了。
//
// 资产：frontend/photo/illustration/*.svg（unDraw 素材，由 scripts/import-illustration.mjs 换色内联）
// 用法：<MEmpty image="empty-cart" ... />（见 frontend/photo/README.md 第四节）
//
// 覆盖（除「地址」外都是**只读**用例；地址需要一个没有地址的账号，故临时注册并在最后删掉）：
//   ① 404 /nope-not-here                        → not-found
//   ② 商城：空购物车 / 空订单 / 空收藏 / 空足迹 / 空评价 / 搜索无结果
//   ③ 外卖：空购物车 / 空订单 / 商家搜索无匹配 / 骑手「抢单池无单」/ 骑手「无配送单」
//   ④ 后台：运营看板无数据（empty-data）
//   ⑤ 地址：临时买家（0 个地址）→ empty-address；商品不存在 → not-found
//   ⑥ 懒加载回归：有数据的首页**不请求** illustrations chunk
//
// 断言：插图是**内联 svg**（不是 <img>，所以 currentColor 生效 → 主色 = 品牌蓝 #003de2）、
//       有尺寸（>150px）、有内容（>10 个图形）、且不再渲染老的 .m-empty-ico 方块。
//
// 用法（需 前端 :5178 + 后端 :8080/:8081 + Redis + MySQL 在跑）：
//   node scripts/ui-smoke-illustrations.mjs
//   node scripts/ui-smoke-illustrations.mjs --headed
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

const BRAND_BLUE = 'rgb(0, 61, 226)'

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

/* ---------------- redis ---------------- */
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

/* ---------------- mysql（只用来清理临时账号） ---------------- */
function mysql(statement) {
  const candidates = [
    process.env.MYSQL_CLI, 'mysql',
    'D:/mysql/mysql-9.5.0-winx64/bin/mysql.exe',
    'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe'
  ].filter(Boolean)
  for (const exe of candidates) {
    if (exe !== 'mysql' && !fs.existsSync(exe)) continue
    try {
      return execFileSync(exe, [
        '--ssl-mode=DISABLED', '--default-character-set=utf8mb4',
        '-h', process.env.MYSQL_HOST || '127.0.0.1', '-P', process.env.MYSQL_PORT || '3306',
        '-u', process.env.MYSQL_USER || 'root', `-p${process.env.MYSQL_PASSWORD || 'Ming1234'}`,
        '-D', process.env.MYSQL_DATABASE || 'supermall', '-N', '-B', '-e', statement
      ], { encoding: 'utf8' }).trim()
    } catch { /* next */ }
  }
  throw new Error('mysql 执行失败：' + statement)
}

/* ---------------- 账号 ---------------- */
/** role 必须是登录页真实存在的身份（否则会落到默认 tab，登成另一个身份） */
const ACCOUNTS = {
  'mall-empty': { role: 'mall-user', username: 'user2@qq.com', password: 'Mall1234' },
  'food-empty': { role: 'delivery-user', username: 'user2@qq.com', password: 'Mall1234' },
  rider: { role: 'rider', username: 'rider2@gmail.com', password: 'Mall1234' },
  admin: { role: 'mall-admin', username: 'admin@qq.com', password: 'Admin1234' }
}

const captchas = []
const errors = []
let page
let ctx
let browser
let tempUserId = null

async function fresh() {
  if (ctx) await ctx.close()
  ctx = await browser.newContext({ viewport: { width: 1440, height: 950 } })
  page = await ctx.newPage()
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', (e) => errors.push(String(e.message)))
  page.on('response', async (res) => {
    const u = res.url()
    if (u.includes('/auth/captcha')) {
      try {
        const j = await res.json()
        // ⚠️ mall 标记必须带上：商城的验证码存在 mall:captcha:，外卖在 captcha:
        if (j?.data?.key) captchas.push({ key: j.data.key, image: j.data.image || '', mall: u.includes('/api/mall/') })
      } catch { /* ignore */ }
    }
  })
}

async function captchaCode() {
  const img = page.locator('img[alt="验证码"]')
  await img.waitFor({ state: 'visible', timeout: 15000 }).catch(() => {})
  if (!(await img.count())) return null
  const deadline = Date.now() + 12000
  while (Date.now() < deadline) {
    const src = await img.getAttribute('src').catch(() => null)
    if (src && src.startsWith('data:image/png;base64,')) {
      const b64 = src.slice('data:image/png;base64,'.length)
      const hit = [...captchas].reverse().find((x) => x.image === b64)
      if (hit) return { key: hit.key, code: redis(['GET', `${hit.mall ? 'mall:' : ''}captcha:${hit.key}`]) }
    }
    await page.waitForTimeout(140)
  }
  return null
}

async function login(who) {
  const { role, username, password } = ACCOUNTS[who]
  await page.goto(`${BASE}/login?role=${role}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(800)
  if (!new URL(page.url()).pathname.startsWith('/login')) return // 已登录，守卫会放走
  const cap = await captchaCode()
  if (!cap) throw new Error(`${who} 登录页拿不到验证码`)
  await page.locator('input[placeholder="请输入邮箱账号"]').fill(username)
  await page.locator('input[placeholder="请输入密码"]').fill(password)
  await page.locator('input[placeholder="输入右侧 4 位字符"]').fill(cap.code)
  await page.locator('button[type="submit"]').click()
  await page.waitForTimeout(2500)
  const logged = await page.evaluate((r) => Boolean(localStorage.getItem(`supermall:token:${r}`)), role)
  if (!logged) throw new Error(`登录 ${who} (role=${role}) 失败，URL=${page.url()}`)
}

/**
 * 注册一个临时买家（用来测「没有地址」的空态）；返回 { id, token }。
 *
 * 这里不靠页面上的验证码图：自己调 `/api/mall/auth/captcha` 领一张、去 Redis 读答案，
 * 所以只认自己这张 key（注册接口会直接返回 token，顺手写进 localStorage 免登录）。
 */
async function registerTempBuyer() {
  const email = `artcheck${Date.now().toString(36)}@qq.com`
  const res = await page.evaluate(async (email) => {
    const cap = await (await fetch('/api/mall/auth/captcha')).json()
    const out = { cap: cap?.data }
    return out
  }, email)
  const key = res.cap?.key
  if (!key) throw new Error('领验证码失败')
  const code = redis(['GET', `mall:captcha:${key}`])
  const reg = await page.evaluate(async ({ email, key, code }) => {
    const r = await fetch('/api/mall/user/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: email, password: 'Mall1234', nickname: '插图验收', captchaKey: key, captchaCode: code })
    })
    return await r.json()
  }, { email, key, code })
  if (reg?.code !== 1) throw new Error('临时买家注册失败：' + JSON.stringify(reg))
  // 直接把 token 写进会话（storage.write 用的是 JSON.stringify）
  await page.evaluate((t) => localStorage.setItem('supermall:token:mall-user', JSON.stringify(t)), reg.data.token)
  return reg.data.id
}

/** 读页面上的插图状态 */
async function readArt(selector) {
  return page.evaluate((sel) => {
    const wrap = document.querySelector(sel)
    if (!wrap) return { found: false }
    const svg = wrap.querySelector('svg')
    const box = svg ? svg.getBoundingClientRect() : null
    return {
      found: true,
      inline: !!svg,
      width: box ? Math.round(box.width) : 0,
      height: box ? Math.round(box.height) : 0,
      paths: svg ? svg.querySelectorAll('path,circle,ellipse,rect').length : 0,
      color: getComputedStyle(wrap).color,
      hasOldIcon: !!document.querySelector('.m-empty-ico')
    }
  }, selector)
}

/** 空态插图断言（3 条/处，避免刷屏） */
async function expectArt(label, route, slug, { wait = 1000 } = {}) {
  await page.goto(BASE + route, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.waitForTimeout(wait)
  const r = await readArt('.m-empty-art')
  check(`${label}：插图已内联渲染（宽 ${r.width}px / ${r.paths} 个图形）`,
    r.found && r.inline && r.width > 150 && r.height > 80 && r.paths >= 8, JSON.stringify(r))
  check(`${label}：主色跟随 currentColor = 品牌蓝`, r.color === BRAND_BLUE, r.color)
  check(`${label}：不再渲染老图标方块`, r.found && r.hasOldIcon === false)
  if (slug) await page.screenshot({ path: path.join(SHOT_DIR, `art-${slug}.png`) })
}

try {
  browser = await chromium.launch({ headless: !HEADED })

  // ============================================================
  section('1. 404 页（无需登录）')
  await fresh()
  await page.goto(`${BASE}/nope-not-here`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.waitForTimeout(800)
  const nf = await readArt('.nf-art')
  check(`404：插图已内联渲染（宽 ${nf.width}px / ${nf.paths} 个图形）`, nf.found && nf.inline && nf.width > 200 && nf.paths >= 8)
  check('404：主色 = 品牌蓝', nf.color === BRAND_BLUE, nf.color)
  check('404：仍保留大号 404 字样与按钮', (await page.locator('.nf-code').innerText()) === '404')
  await page.screenshot({ path: path.join(SHOT_DIR, 'art-404.png') })

  // ============================================================
  section('2. 商城：空购物车 / 空订单 / 空收藏 / 空足迹 / 空评价 / 搜索无结果')
  await fresh()
  await login('mall-empty')
  await expectArt('空购物车', '/shop/cart', 'mall-cart-empty')
  await expectArt('空订单', '/shop/orders', 'mall-orders-empty')
  await expectArt('空收藏', '/shop/favorites', 'mall-favorites-empty')
  await expectArt('空足迹', '/shop/footprints', 'mall-footprints-empty')
  await expectArt('空评价', '/shop/reviews', 'mall-reviews-empty')
  await expectArt('搜索无结果', '/shop/search?keyword=zzzznope', 'mall-search-empty')

  // ============================================================
  section('3. 外卖买家：空购物车 / 空订单 / 商家搜索无匹配')
  await fresh()
  await login('food-empty')
  await expectArt('外卖空购物车', '/food/cart', 'food-cart-empty')
  await page.goto(`${BASE}/food/orders`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.locator('.m-tab', { hasText: '已完成' }).click()
  await page.waitForTimeout(1200)
  const fo = await readArt('.m-empty-art')
  check(`外卖空订单（已完成 tab）：插图已内联渲染（宽 ${fo.width}px / ${fo.paths} 个图形）`, fo.found && fo.inline && fo.width > 150 && fo.paths >= 8)
  check('外卖空订单：主色 = 品牌蓝', fo.color === BRAND_BLUE, fo.color)
  await page.screenshot({ path: path.join(SHOT_DIR, 'art-food-orders-empty.png') })
  // 外卖「搜店名」是本地过滤（不认 URL query），所以要真打字
  await page.goto(`${BASE}/food/merchants`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.locator('input[placeholder="搜店名"]').fill('zzzznope')
  await page.waitForTimeout(900)
  const fm = await readArt('.m-empty-art')
  check(`商家搜索无匹配：插图已内联渲染（宽 ${fm.width}px / ${fm.paths} 个图形）`,
    fm.found && fm.inline && fm.width > 150 && fm.paths >= 8, JSON.stringify(fm))
  check('商家搜索无匹配：主色 = 品牌蓝', fm.color === BRAND_BLUE, fm.color)
  await page.screenshot({ path: path.join(SHOT_DIR, 'art-food-merchants-empty.png') })

  // ============================================================
  section('4. 骑手：抢单池无单（空数据）+ 无配送单（空订单）')
  await fresh()
  await login('rider')
  await expectArt('抢单池无单', '/rider', 'rider-pool-empty')
  await expectArt('无配送单', '/rider/mine', 'rider-mine-empty')

  // ============================================================
  section('5. 运营后台：看板无数据')
  await fresh()
  await login('admin')
  await expectArt('看板无趋势数据', '/admin', 'admin-dashboard-empty')

  // ============================================================
  section('6. 地址空态（临时买家：0 个地址）+ 商品不存在')
  await fresh()
  await page.goto(`${BASE}/shop`, { waitUntil: 'domcontentloaded' }) // 先落到同源页面，才能 fetch / 写 localStorage
  await page.waitForLoadState('networkidle').catch(() => {})
  tempUserId = await registerTempBuyer()
  check('临时买家已注册（用来测「还没有地址」）', !!tempUserId, `id=${tempUserId}`)
  await expectArt('商城还没有收货地址', '/shop/addresses', 'mall-address-empty')
  await expectArt('商品不存在', '/shop/products/99999999', 'mall-product-missing')

  // ============================================================
  section('7. 懒加载回归：有数据的首页不下载 illustrations chunk')
  await fresh()
  const illusReqs = []
  page.on('request', (r) => { if (r.url().includes('illustrations')) illusReqs.push(r.url().split('/').pop()) })
  await page.goto(`${BASE}/shop`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.waitForTimeout(1500)
  const cards = await page.locator('.m-pcard').count()
  check(`商城首页有商品（${cards} 张卡）→ 不出现空态`, cards > 0)
  check('没有请求 illustrations chunk（按需加载生效）', illusReqs.length === 0, illusReqs.join(','))

  // ============================================================
  section('8. 没有 JS 运行时错误')
  const realErrors = errors.filter((e) => !/favicon|ResizeObserver|Download the Vue/i.test(e))
  check('无 JS 运行时错误', realErrors.length === 0, realErrors.slice(0, 2).join(' | '))

  await browser.close()
} catch (e) {
  console.error('\n💥 脚本异常：', e.message)
  failed++
  if (browser) await browser.close().catch(() => {})
} finally {
  if (tempUserId) {
    try {
      mysql(`delete from address where user_id=${tempUserId}`)
      mysql(`delete from sys_user where id=${tempUserId}`)
      console.log(`\n🧹 已删除临时买家 #${tempUserId}（本脚本自己造的账号，不留垃圾）`)
    } catch (e) {
      console.log(`\n⚠️ 临时买家 #${tempUserId} 清理失败：${e.message}`)
    }
  }
  console.log(`\n结果：${results.length - failed}/${results.length} 通过`)
  console.log('ℹ️  截图在 logs/ui-smoke/art-*.png')
  if (failed > 0) process.exitCode = 1
}
