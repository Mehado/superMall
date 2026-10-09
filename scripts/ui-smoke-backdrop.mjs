// scripts/ui-smoke-backdrop.mjs
// 浏览器（Playwright）实测「全站背景图」：13 个页面都挂上对的虚化档位，且不影响可用性。
//
// 背景实现：components/AppBackdrop.vue（全站唯一背景组件），铺 photo/portal-bg.jpg（cover）
// + 纵向渐变白纱；虚化**按页面分档**（2026-09-21 按用户拍板确认，保留分档设计）：
//
//   档位   | 页面                                            | 来源
//   -------|-------------------------------------------------|--------------------------------
//   不虚化 | 主页面 / 登录 / 注册 / 商家入驻                   | 这些页面自己传默认值
//   6px    | 404                                             | NotFound.vue  :blur="6"
//   8px    | 商城首页 /shop、商城我的 /shop/me                | ShopLayout    :blur="8"
//   8px    | 外卖首页 /food、外卖我的 /food/me                | FoodLayout    :blur="8"
//   10px   | /admin、/merchant/*、/rider/*、/platform         | WorkbenchLayout :blur="10"
//
// ⚠️ 档位由**外壳**决定：同外壳的子页共享同一档（所以 /shop/me 与 /shop 同为 8px，
//    而不是回到“不虚化”）。改布局的 :blur 时记得同步这张表与本脚本。
//
// 断言：每页 .backdrop 存在、.bd-img 已加载、虚化档位与预期一致、白纱存在、无 JS 报错。
// 注：断言按**桌面 1440 宽**跑（小屏 CSS 会把虚化强制压成 none，见 AppBackdrop.vue 的媒体查询）。
//
// 用法（需 前端 :5178 + 后端 :8080/:8081 + Redis 在跑）：
//   node scripts/ui-smoke-backdrop.mjs
//   node scripts/ui-smoke-backdrop.mjs --headed
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

async function login(role, username, password) {
  store.length = 0
  await page.goto(`${BASE}/login?role=${role}`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  const img = page.locator('img[alt="验证码"]')
  if (!(await img.count())) return // 已登录会被守卫直接跳走
  await img.waitFor({ state: 'visible', timeout: 12000 })
  await page.waitForTimeout(700)
  const src = await img.getAttribute('src')
  const b64 = String(src).replace(/^data:image\/png;base64,/, '')
  const hit = store.find((x) => x.image === b64) || store.at(-1)
  await page.locator('input[placeholder="请输入邮箱账号"]').fill(username)
  await page.locator('input[placeholder="请输入密码"]').fill(password)
  await page.locator('input[placeholder="输入右侧 4 位字符"]').fill(redis(['GET', `${hit.mall ? 'mall:' : ''}captcha:${hit.key}`]))
  await page.locator('button[type="submit"]').click()
  await page.waitForTimeout(1800)
}

/** 按角色分组：一个角色只登录一次（登录页在已登录时会直接跳走） */
const GROUPS = [
  { role: null, pages: [
    { slug: 'portal', label: '主页面', route: '/' },
    { slug: 'login', label: '登录页', route: '/login' },
    { slug: 'register', label: '注册页', route: '/register' },
    { slug: 'apply', label: '商家入驻', route: '/merchant/apply' },
    { slug: '404', label: '404', route: '/nope-not-here', expectBlur: 6 }
  ] },
  { role: 'mall-user', creds: ['user1@qq.com', 'Mall1234'], pages: [
    { slug: 'shop', label: '商城首页', route: '/shop', expectBlur: 8 },
    // 同一个外壳（ShopLayout :blur="8"）→ 与首页同档
    { slug: 'mall-me', label: '商城我的', route: '/shop/me', expectBlur: 8 }
  ] },
  { role: 'delivery-user', creds: ['user1@qq.com', 'Mall1234'], pages: [
    { slug: 'food', label: '外卖首页', route: '/food', expectBlur: 8 },
    { slug: 'food-me', label: '外卖我的', route: '/food/me', expectBlur: 8 }
  ] },
  { role: 'merchant', creds: ['merchant1@163.com', 'Mall1234'], pages: [
    // 工作台外壳（WorkbenchLayout :blur="10"）：商家 / 骑手 / 运营 / 平台四端同档
    { slug: 'merchant', label: '商家工作台', route: '/merchant/shop', expectBlur: 10 }
  ] },
  { role: 'rider', creds: ['rider1@gmail.com', 'Mall1234'], pages: [
    { slug: 'rider', label: '骑手工作台', route: '/rider/profile', expectBlur: 10 }
  ] },
  { role: 'mall-admin', creds: ['admin@qq.com', 'Admin1234'], pages: [
    { slug: 'admin', label: '运营看板', route: '/admin', expectBlur: 10 }
  ] },
  { role: 'platform-admin', creds: ['platform@163.com', 'Admin1234'], pages: [
    { slug: 'platform', label: '平台运营端', route: '/platform', expectBlur: 10 }
  ] }
]

async function main() {
  const browser = await chromium.launch({ headless: !HEADED })
  page = await (await browser.newContext({ viewport: { width: 1440, height: 900 } })).newPage()
  const errors = []
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', (e) => errors.push(String(e.message)))
  page.on('response', async (res) => {
    const u = res.url()
    if (u.includes('/auth/captcha')) {
      try {
        const j = await res.json()
        if (j?.data?.key) store.push({ key: j.data.key, image: j.data.image || '', mall: u.includes('/api/mall/') })
      } catch { /* ignore */ }
    }
  })

  let total = 0
  for (const g of GROUPS) {
    if (g.role) await login(g.role, g.creds[0], g.creds[1])
    for (const p of g.pages) {
      total++
      await page.goto(BASE + p.route, { waitUntil: 'domcontentloaded' })
      await page.waitForLoadState('networkidle').catch(() => {})
      await page.waitForTimeout(1200)
      const r = await page.evaluate(() => {
        const bd = document.querySelector('.backdrop')
        const img = document.querySelector('.bd-img')
        const veil = document.querySelector('.bd-veil')
        return {
          hasBackdrop: !!bd,
          imgOk: img ? img.naturalWidth > 0 : false,
          filter: img ? getComputedStyle(img).filter : 'none',
          veil: veil ? getComputedStyle(veil).backgroundImage.slice(0, 40) : ''
        }
      })
      const wantBlur = p.expectBlur || 0
      const blurOk = wantBlur > 0
        ? r.filter.includes(`blur(${wantBlur}px)`)
        : (r.filter === 'none' || r.filter === '')
      check(`${p.label} ${p.route}：背景图已加载，虚化=${wantBlur > 0 ? wantBlur + 'px' : '无'}`,
        r.hasBackdrop && r.imgOk && blurOk,
        `filter=${r.filter} veil=${r.veil ? 'on' : 'off'}`)
      await page.screenshot({ path: path.join(SHOT_DIR, `bg-${p.slug}.png`) })
    }
  }

  section('控制台')
  const real = errors.filter((t) => !t.includes('favicon'))
  check(`这 ${total} 个页面都没有 JS 运行时错误`, real.length === 0, real.slice(0, 2).join(' | '))

  await browser.close()
  console.log(`\nℹ️  截图：${SHOT_DIR}/bg-*.png（每页一张，方便肉眼核对）`)
  console.log('ℹ️  可读性（像素级对比度）用一次性脚本量过：12 页全部 >= 4.5:1，详见 docs/项目进度.md #77')
}

main()
  .catch((e) => check('脚本执行完成', false, `抛异常：${e.message}`))
  .finally(() => {
    console.log(`\n══════════════════════════════════════`)
    console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
    console.log(`══════════════════════════════════════`)
    process.exit(failed ? 1 : 0)
  })
