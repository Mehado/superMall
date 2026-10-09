// scripts/ui-smoke-marquee.mjs
// 浏览器（Playwright）实测「首页字标后面的横向滚动图片墙」——商城 /shop 与外卖 /food 两页都测。
//
// 断言的是**可验证的事实**（不靠肉眼）：
//   结构：2 行横向轨道、每行「5 张一组」重复铺满、只有 10 张唯一图、无破图、卡片 4:5
//   图源：图片必须来自该页自己的图池（商城 photo/marquee/、外卖 photo/marquee-food/）
//   动画：横向 tx 在变、纵向 ty 恒为 0；每行时长不同、方向交错
//   三层动效：轨道循环 + 卡片 4s 错峰待机浮动 + 鼠标 3D 倾斜（--mx/--my）
//   层级：图片 1 < 白柔光 2 < 字标 3 < 搜索框 4 —— 字不被盖、控件不被挡（elementFromPoint）
//   降级：prefers-reduced-motion 下轨道动画/待机浮动关掉、卡片不倾斜；图片行不被容器裁掉
//
// 用法（需 前端 :5178 + 后端 :8080/:8081 + Redis 在跑）：
//   node scripts/ui-smoke-marquee.mjs
//   node scripts/ui-smoke-marquee.mjs --headed
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

/* ---------------- Redis（读验证码答案） ---------------- */
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
    if (!cliPath) throw new Error('找不到 redis-cli，可用 REDIS_CLI 指定路径')
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

async function waitUntil(fn, timeout = 10000, step = 150) {
  const deadline = Date.now() + timeout
  for (;;) {
    if (await fn()) return true
    if (Date.now() > deadline) return false
    await page.waitForTimeout(step)
  }
}

/** 登录（验证码按「页面实际显示的那张图」对齐） */
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
  await page.locator('input[placeholder="请输入邮箱账号"]').fill(username)
  await page.locator('input[placeholder="请输入密码"]').fill(password)
  await page.locator('input[placeholder="输入右侧 4 位字符"]').fill(redis(['GET', `${mall ? 'mall:' : ''}captcha:${key}`]))
  await page.locator('button[type="submit"]').click()
}

/** 抓一次页面上的全部可核对事实 */
const probe = (poolFragment) =>
  page.evaluate((pool) => {
    const flow = document.querySelector('.shopname-flow')
    const word = document.querySelector('.m-shopname-img')
    const search = document.querySelector('.m-shopsearch')
    if (!flow || !word) return null
    const tracks = [...document.querySelectorAll('.lane-track')]
    const imgs = [...document.querySelectorAll('.flow-card img')]
    const cards = [...document.querySelectorAll('.flow-card')]
    const z = (el) => (el ? getComputedStyle(el).zIndex : null)
    const fr = flow.getBoundingClientRect()
    const wr = word.getBoundingClientRect()
    const atWord = document.elementFromPoint(wr.left + wr.width / 2, wr.top + wr.height / 2)
    const sr = search ? search.getBoundingClientRect() : null
    const atSearch = sr ? document.elementFromPoint(sr.left + sr.width / 2, sr.top + sr.height / 2) : null
    const lanes = [...document.querySelectorAll('.flow-lane')]
    const fb = flow.getBoundingClientRect()
    const clipped = lanes.reduce((acc, l) => {
      const r = l.getBoundingClientRect()
      return acc + Math.max(0, fb.top - r.top) + Math.max(0, r.bottom - fb.bottom)
    }, 0)
    return {
      lanes: tracks.length,
      imgs: imgs.length,
      unique: new Set(imgs.map((i) => i.currentSrc || i.src)).size,
      broken: imgs.filter((i) => i.complete && i.naturalWidth === 0).length,
      wrongPool: imgs.filter((i) => !(i.currentSrc || i.src).includes(pool)).length,
      durs: tracks.map((t) => getComputedStyle(t).animationDuration),
      dirs: tracks.map((t) => getComputedStyle(t).animationDirection),
      zFlow: Number(z(flow)),
      zWord: Number(z(word)),
      zSearch: Number(z(search)),
      wordH: Math.round(wr.height),
      overlapWord: Math.max(0, Math.min(fr.bottom, wr.bottom) - Math.max(fr.top, wr.top)),
      atWord: atWord ? atWord.tagName + (atWord.className ? '.' + String(atWord.className).split(' ')[0] : '') : null,
      atSearch: atSearch ? atSearch.tagName : null,
      clipped: Math.round(clipped),
      cardAspect: cards.length
        ? Number((cards[0].getBoundingClientRect().height / cards[0].getBoundingClientRect().width).toFixed(2))
        : 0
    }
  }, poolFragment)

const trackTx = () =>
  page.evaluate(() => new DOMMatrixReadOnly(getComputedStyle(document.querySelector('.lane-track')).transform))

async function runBoard({ label, role, home, poolFragment, slug }) {
  section(`${label} · 登录 → ${home}`)
  await login(role, 'user1@qq.com', 'Mall1234')
  check(`${label} 登录后进入 ${home}`, await waitUntil(async () => page.url().includes(home)), page.url())
  await page.locator('.shopname-flow').waitFor({ state: 'visible', timeout: 10000 })

  const m = await probe(poolFragment)
  if (!m) throw new Error(`${label}：页面上找不到 .shopname-flow / .m-shopname-img`)

  section(`${label} · 结构`)
  check('2 行横向轨道', m.lanes === 2, `实际 ${m.lanes} 行`)
  check('每行「5 张一组」重复铺满（图片节点 > 20）', m.imgs > 20, `${m.imgs} 个 img 节点`)
  check('只有 10 张唯一图（2 行 × 一组 5 张）', m.unique === 10, `unique=${m.unique}`)
  check('没有破图', m.broken === 0, `broken=${m.broken}`)
  check(`图片都来自本页图池 ${poolFragment}`, m.wrongPool === 0, `异常 ${m.wrongPool} 张`)
  check('卡片比例 4:5', Math.abs(m.cardAspect - 1.25) < 0.02, `aspect=${m.cardAspect}`)
  check('每行时长不同', new Set(m.durs).size === 2, m.durs.join(' / '))
  check('方向交错（一行反向）', m.dirs.includes('reverse') && m.dirs.includes('normal'), m.dirs.join(' / '))

  section(`${label} · 是「左右」滚动`)
  const t1 = await trackTx()
  await page.waitForTimeout(900)
  const t2 = await trackTx()
  check('横向位移 tx 在变化', Math.round(t1.m41) !== Math.round(t2.m41), `tx ${Math.round(t1.m41)} → ${Math.round(t2.m41)}`)
  check('纵向位移 ty 恒为 0', Math.round(t1.m42) === 0 && Math.round(t2.m42) === 0, `ty=${Math.round(t1.m42)}/${Math.round(t2.m42)}`)

  section(`${label} · 层级与命中`)
  check('图片墙在字标之下（z 1 < 3）', m.zFlow < m.zWord, `flow=${m.zFlow} word=${m.zWord}`)
  check('搜索框在图片墙上（z 4 > 1）', m.zSearch > m.zFlow, `search=${m.zSearch} flow=${m.zFlow}`)
  check('图片墙铺在字标后面（有重叠）', m.overlapWord > 40, `重叠 ${m.overlapWord}px`)
  check('字标正中一点最上层是字标本身（字没被盖住）', String(m.atWord).startsWith('IMG'), `实际 ${m.atWord}`)
  check('搜索框中心点最上层是 INPUT（点击没被吃掉）', m.atSearch === 'INPUT', `实际 ${m.atSearch}`)
  check('字标已缩小（< 130px）', m.wordH < 130, `高度 ${m.wordH}px`)
  check('图片行没有被容器裁掉', m.clipped <= 2, `被裁 ${m.clipped}px`)

  section(`${label} · 三层动效`)
  const f1 = await page.evaluate(() => getComputedStyle(document.querySelector('.card-inner')).transform)
  await page.waitForTimeout(700)
  const f2 = await page.evaluate(() => getComputedStyle(document.querySelector('.card-inner')).transform)
  check('卡片有「错峰待机浮动」', f1 !== f2, `${f1.slice(0, 28)} → ${f2.slice(0, 28)}`)

  const sb = await page.locator('.shopname-stage').boundingBox()
  await page.mouse.move(sb.x + 12, sb.y + sb.height / 2)
  await page.waitForTimeout(450)
  const leftPose = await page.evaluate(() => ({
    mx: Number(document.querySelector('.shopname-flow').style.getPropertyValue('--mx')),
    t: getComputedStyle(document.querySelector('.flow-card')).transform
  }))
  await page.mouse.move(sb.x + sb.width - 12, sb.y + sb.height / 2)
  await page.waitForTimeout(450)
  const rightPose = await page.evaluate(() => ({
    mx: Number(document.querySelector('.shopname-flow').style.getPropertyValue('--mx')),
    t: getComputedStyle(document.querySelector('.flow-card')).transform
  }))
  check('鼠标左右移动 → --mx 从负到正', leftPose.mx < 0 && rightPose.mx > 0, `左 ${leftPose.mx} → 右 ${rightPose.mx}`)
  check('卡片 3D 倾斜跟着鼠标变', leftPose.t !== rightPose.t, `${leftPose.t.slice(0, 30)} vs ${rightPose.t.slice(0, 30)}`)

  section(`${label} · 降级：prefers-reduced-motion`)
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.waitForTimeout(250)
  const rm = await page.evaluate(() => ({
    track: getComputedStyle(document.querySelector('.lane-track')).animationName,
    inner: getComputedStyle(document.querySelector('.card-inner')).animationName,
    card: getComputedStyle(document.querySelector('.flow-card')).transform
  }))
  check('轨道动画关掉', rm.track === 'none' || rm.track === '', `animationName=${rm.track}`)
  check('待机浮动关掉', rm.inner === 'none' || rm.inner === '', `animationName=${rm.inner}`)
  check('卡片不倾斜', rm.card === 'none' || rm.card === 'matrix(1, 0, 0, 1, 0, 0)', `transform=${rm.card}`)
  await page.emulateMedia({ reducedMotion: 'no-preference' })
  await page.waitForTimeout(250)
  const back = await page.evaluate(() => getComputedStyle(document.querySelector('.lane-track')).animationName)
  check('恢复后动画又回来了', back && back !== 'none', `animationName=${back}`)

  // 截图：先把鼠标摆到右上角，让 3D 倾斜出现在图里
  await page.mouse.move(sb.x + sb.width * 0.82, sb.y + sb.height * 0.24)
  await page.waitForTimeout(500)
  await page.screenshot({ path: path.join(SHOT_DIR, `marquee-${slug}.png`) })
  await page.screenshot({
    path: path.join(SHOT_DIR, `marquee-${slug}-stage.png`),
    clip: { x: Math.max(0, sb.x - 80), y: 0, width: Math.min(1400, sb.width + 160), height: sb.height + 260 }
  })
  await page.mouse.move(0, 0)
}

async function main() {
  const browser = await chromium.launch({ headless: !HEADED })
  page = await (await browser.newContext({ viewport: { width: 1440, height: 1000 } })).newPage()
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

  await runBoard({ label: '商城首页', role: 'mall-user', home: '/shop', poolFragment: '/photo/marquee/', slug: 'mall' })
  await runBoard({ label: '外卖首页', role: 'delivery-user', home: '/food', poolFragment: '/photo/marquee-food/', slug: 'food' })

  section('控制台')
  const real = errors.filter((t) => !t.includes('favicon'))
  check('两页都没有 JS 运行时错误', real.length === 0, real.slice(0, 2).join(' | '))

  await browser.close()
  console.log(`\nℹ️  截图目录：${SHOT_DIR}（marquee-mall-*.png / marquee-food-*.png）`)
}

main()
  .catch((e) => check('脚本执行完成', false, `抛异常：${e.message}`))
  .finally(() => {
    console.log(`\n══════════════════════════════════════`)
    console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
    console.log(`══════════════════════════════════════`)
    process.exit(failed ? 1 : 0)
  })
