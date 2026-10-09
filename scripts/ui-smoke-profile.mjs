// scripts/ui-smoke-profile.mjs
// 浏览器（Playwright）实测买家「我的」页：编辑资料（昵称/手机号）+ 头像上传，外卖（/food/me）与电商（/shop/me）两端都测。
//
// 用法（需 前端 :5178 + 后端 :8080/:8081 + Redis 在跑）：
//   node scripts/ui-smoke-profile.mjs
//   node scripts/ui-smoke-profile.mjs --headed
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

/* ---------------- Redis ---------------- */
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

/* ---------------- 素材 ---------------- */
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
  'base64'
)
const AVATAR_FILE = { name: 'avatar.png', mimeType: 'image/png', buffer: PNG }

const store = []
let page

async function waitUntil(fn, timeout = 10000, step = 150) {
  const deadline = Date.now() + timeout
  for (;;) {
    if (await fn()) return true
    if (Date.now() > deadline) return false
    await page.waitForTimeout(step)
  }
}

/** 打开登录页并登录（验证码按“页面显示的那张图”对齐） */
async function login(role, username, password) {
  store.length = 0
  await page.goto(`${BASE}/login?role=${role}`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  const img = page.locator('img[alt="验证码"]')
  await img.waitFor({ state: 'visible', timeout: 12000 })
  const deadline = Date.now() + 10000
  let key = null
  let mall = false
  while (Date.now() < deadline) {
    const src = await img.getAttribute('src').catch(() => null)
    if (src && src.startsWith('data:image/png;base64,')) {
      const b64 = src.slice('data:image/png;base64,'.length)
      const hit = store.find((x) => x.image === b64)
      if (hit) { key = hit.key; mall = hit.mall; break }
    }
    await page.waitForTimeout(120)
  }
  if (!key) throw new Error('无法把验证码图与接口响应对应起来')
  const code = redis(['GET', `${mall ? 'mall:' : ''}captcha:${key}`])
  await page.locator('input[placeholder="请输入邮箱账号"]').fill(username)
  await page.locator('input[placeholder="请输入密码"]').fill(password)
  await page.locator('input[placeholder="输入右侧 4 位字符"]').fill(code)
  await page.locator('button[type="submit"]').click()
}

/**
 * 直接设置当前角色的头像（用页面里的 token 打接口，不经过 UI）。
 *
 * ⚠️ 用例**必须**「进来先记下原头像 → 跑完还原」，
 * 不能无脑清空 —— 演示账号（user1@qq.com）上可能有真人在用的头像。
 */
async function setAvatar(roleStorage, url) {
  await page.evaluate(
    async ({ key, url }) => {
      const raw = localStorage.getItem(`supermall:${key}`)
      const token = raw ? JSON.parse(raw) : ''
      if (!token) return
      await fetch('/api/user/profile', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', token },
        body: JSON.stringify({ avatar: url })
      })
    },
    { key: roleStorage, url }
  )
}

/** 用页面里的 token 直接读写资料（用于准备/还原，不经过 UI） */
async function profileViaApi(apiPath, storageKey, method = 'GET', payload) {
  return page.evaluate(
    async ({ apiPath, key, method, payload }) => {
      const raw = localStorage.getItem(`supermall:${key}`)
      const token = raw ? JSON.parse(raw) : ''
      const res = await fetch(apiPath, {
        method,
        headers: { 'Content-Type': 'application/json', token },
        body: payload === undefined ? undefined : JSON.stringify(payload)
      })
      const text = await res.text()
      try {
        return JSON.parse(text)?.data ?? null
      } catch {
        return null
      }
    },
    { apiPath, key: storageKey, method, payload }
  )
}

async function testBoard({ label, role, homePath, mePath, storageKey, profileApi }) {
  section(`${label}头像`)
  await login(role, 'user1@qq.com', 'Mall1234')
  const inHome = await waitUntil(async () => page.url().includes(homePath))
  check(`${label}登录成功`, inHome, page.url())

  // 顺带守一下样式：成功提示应为品牌蓝（曾经是绿色）
  if (label === '外卖买家') {
    const bg = await page.evaluate(() => {
      const el = document.querySelector('.toast.ok')
      return el ? getComputedStyle(el).backgroundColor : null
    })
    check('成功提示是品牌蓝（不是绿色）',
      ['rgb(0, 61, 226)', 'rgb(37, 99, 235)'].includes(bg || ''), String(bg))
  }

  await page.goto(BASE + mePath, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})

  // 记下原头像（跑完要还原，别把演示账号上真人设置的头像弄丢），
  // 再临时清空，保证“初始是字母占位”这个断言可判定
  const avatarBefore = (await profileViaApi(profileApi, storageKey))?.avatar || ''
  await setAvatar(storageKey, '')
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})

  const up = page.locator('.avatar-up')
  await up.waitFor({ state: 'visible', timeout: 10000 })
  const hadImg = await up.locator('img').count()
  check('初始为字母占位（无头像图）', hadImg === 0)

  await up.locator('input[type=file]').setInputFiles(AVATAR_FILE)
  const uploaded = await waitUntil(async () => (await up.locator('img').count()) > 0, 15000)
  check('选择图片后头像立刻显示为图片', uploaded)
  const src = uploaded ? await up.locator('img').getAttribute('src') : ''
  check('头像 URL 来自 OSS 头像目录', String(src).includes('/supermall/avatars/'), String(src))
  await page.screenshot({ path: path.join(SHOT_DIR, `avatar-${role}.png`), fullPage: true })

  const railImg = page.locator('.navrail-avatar.has-img img')
  const railOk = await waitUntil(async () => (await railImg.count()) > 0, 8000)
  check('左侧导航栏头像同步为图片', railOk)

  // 清除：回退首字母
  await setAvatar(storageKey, '')
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle').catch(() => {})
  const backToLetter = await waitUntil(async () => (await page.locator('.avatar-up img').count()) === 0, 10000)
  check('清除后回退首字母占位', backToLetter)

  /* ---------- 编辑资料（昵称 / 手机号） ---------- */
  const original = await profileViaApi(profileApi, storageKey)
  const newNick = `${label}改名${Date.now().toString(36).slice(-4)}`

  const hasEditBtn = await page.getByRole('button', { name: '编辑资料' }).count()
  check('「我的」页有「编辑资料」按钮', hasEditBtn > 0)
  await page.getByRole('button', { name: '编辑资料' }).first().click()
  // 弹窗打开后可见的输入框：0 昵称、1 手机号、2 登录账号（disabled）
  await page.locator('input[placeholder="11 位手机号"]').waitFor({ state: 'visible', timeout: 8000 })
  const modalInputs = page.locator('input.m-input:visible')
  await modalInputs.nth(0).fill(newNick)
  await modalInputs.nth(1).fill('13900000088')
  await page.getByRole('button', { name: '保存' }).first().click()

  const renamed = await waitUntil(async () =>
    (await page.locator('body').innerText()).includes(newNick), 10000)
  check('保存后页面昵称已更新', renamed)

  const persisted = await profileViaApi(profileApi, storageKey)
  check('接口里昵称 / 手机号都已落库',
    persisted?.nickname === newNick && persisted?.phone === '13900000088',
    `nickname=${persisted?.nickname}, phone=${persisted?.phone}`)

  // 还原（昵称/手机号 + 原头像）
  await profileViaApi(profileApi, storageKey, 'PUT', {
    nickname: original?.nickname ?? '',
    phone: original?.phone ?? ''
  })
  await setAvatar(storageKey, avatarBefore)
  check('已还原原资料与原头像', true)
}

async function main() {
  const browser = await chromium.launch({ headless: !HEADED })
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  page = await ctx.newPage()
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

  await testBoard({
    label: '外卖买家',
    role: 'delivery-user',
    homePath: '/food',
    mePath: '/food/me',
    storageKey: 'token:delivery-user',
    profileApi: '/api/user/profile'
  })

  await testBoard({
    label: '电商买家',
    role: 'mall-user',
    homePath: '/shop',
    mePath: '/shop/me',
    storageKey: 'token:mall-user',
    profileApi: '/api/mall/user/profile'
  })

  section('控制台')
  const real = errors.filter((t) => !t.includes('favicon'))
  check('页面无 JS 运行时错误', real.length === 0, real.slice(0, 2).join(' | '))

  await browser.close()
  console.log(`\nℹ️  截图目录：${SHOT_DIR}`)
  console.log('ℹ️  两个板块的昵称/手机号/头像都已还原成进来时的样子；上传到 OSS 的测试图可留作演示。')
}

main()
  .catch((e) => check('脚本执行完成', false, `抛异常：${e.message}`))
  .finally(() => {
    console.log(`\n══════════════════════════════════════`)
    console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
    console.log(`══════════════════════════════════════`)
    process.exit(failed ? 1 : 0)
  })
