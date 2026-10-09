/**
 * E2E 测试公共工具。
 *
 * ── 关于验证码怎么过 ─────────────────────────────────────────
 * 图形验证码是后端生成、答案只存 Redis 的，Playwright 读不了图片。
 * 这里【不】给后端开任何后门（比如"测试模式返回答案"）——那等于把防机器人关掉。
 * 而是：前端 DOM 上带 data-captcha-key（key 本来就在接口响应里给过浏览器，
 * 不是秘密），测试拿这个 key 直接去 Redis 取答案。
 * 这样跑的仍然是「图片下发 → 提交 → 服务端校验 → 作废」的真实链路。
 * ─────────────────────────────────────────────────────────
 */
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'

export const DEMO_PASSWORD = 'Mall1234'

export const DEMO = {
  user: { label: '用户', email: 'user1@qq.com' },
  merchant: { label: '商家', email: 'merchant1@163.com' },
  rider: { label: '骑手', email: 'rider1@gmail.com' }
}

/* ---------------- redis-cli ---------------- */

const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1'
const REDIS_PORT = process.env.REDIS_PORT || '6379'
// 本机开发 Redis 密码（项目文档里已写明）；换环境请设 REDIS_PASSWORD
const REDIS_PASSWORD = process.env.REDIS_PASSWORD || 'mehado'

const CANDIDATES = [
  process.env.REDIS_CLI,
  'redis-cli',
  'D:/redis/Redis-x64-5.0.14.1/redis-cli',
  'C:/Program Files/Redis/redis-cli.exe'
].filter(Boolean)

let cliPath = null
function resolveCli() {
  if (cliPath) return cliPath
  for (const p of CANDIDATES) {
    if (p !== 'redis-cli' && !fs.existsSync(p)) continue
    try {
      execFileSync(p, ['--version'], { stdio: 'ignore' })
      cliPath = p
      return cliPath
    } catch {
      /* 换下一个候选 */
    }
  }
  throw new Error(
    `找不到可用的 redis-cli。请安装 Redis 客户端，或用 REDIS_CLI 环境变量指定路径。\n已尝试：${CANDIDATES.join(', ')}`
  )
}

function redisCmd(args) {
  const cli = resolveCli()
  const base = ['-h', REDIS_HOST, '-p', REDIS_PORT, '-a', REDIS_PASSWORD]
  const variants = [[...base, '--no-auth-warning', ...args], [...base, ...args]]
  let lastErr
  for (const a of variants) {
    try {
      return execFileSync(cli, a, { encoding: 'utf8' }).trim()
    } catch (e) {
      lastErr = e
    }
  }
  throw new Error(`redis-cli 执行失败：${lastErr?.message || '未知错误'}`)
}

/** 读取图形验证码的答案。key 来自前端 DOM 的 data-captcha-key */
export function readCaptchaAnswer(key) {
  const answer = redisCmd(['GET', `captcha:${key}`])
  if (!answer) {
    throw new Error(
      `Redis 里取不到 captcha:${key}（可能已过期或已被消费）。\n` +
        `请确认 Redis 在 ${REDIS_HOST}:${REDIS_PORT} 且在跑，密码是否正确。`
    )
  }
  return answer
}

/* ---------------- 页面操作 ---------------- */

/** 是否渲染了验证码控件（后端 super-mall.captcha.enabled=false 时不渲染） */
export async function captchaVisible(page) {
  return (await page.locator('.captcha').count()) > 0
}

/**
 * 填写图形验证码。返回是否填了。
 * 会等 data-captcha-key 出现（图片是异步拉的）。
 */
export async function fillCaptcha(page) {
  if (!(await captchaVisible(page))) return false

  await page.waitForFunction(
    () => {
      const el = document.querySelector('.captcha')
      return !!el && !!el.getAttribute('data-captcha-key')
    },
    { timeout: 15000 }
  )

  const key = await page.locator('.captcha').getAttribute('data-captcha-key')
  const answer = readCaptchaAnswer(key)
  await page.fill('.captcha input.code', answer)
  return true
}

/**
 * 点图片换一张，并等 key 真的变了。
 *
 * 必须等：刷新是异步请求，点完立即读 data-captcha-key 拿到的还是旧 key，
 * 而刷新完成时组件会清空已填的验证码 —— 不等就会填到一张已经作废的图上。
 */
export async function refreshCaptcha(page) {
  if (!(await captchaVisible(page))) return false
  const old = await page.locator('.captcha').getAttribute('data-captcha-key')
  await page.click('.captcha .pic')
  await page.waitForFunction(
    (prev) => {
      const el = document.querySelector('.captcha')
      const k = el && el.getAttribute('data-captcha-key')
      return !!k && k !== prev
    },
    old,
    { timeout: 15000 }
  )
  return true
}

/** 打开登录页（清掉旧登录态，保证从干净状态开始） */
export async function gotoLogin(page, base) {
  await page.goto(base + '/login', { waitUntil: 'networkidle' })
  await page.evaluate(() => localStorage.clear())
  await page.goto(base + '/login', { waitUntil: 'networkidle' })
}

/** 只把身份/邮箱/密码填好，不提交（给「校验拦截」类用例复用） */
export async function fillCredentials(page, roleKey, { email, password }) {
  await page.click(`.roles button:has-text("${DEMO[roleKey].label}")`)
  await page.fill('#login-email', email)
  await page.fill('#login-password', password)
}

/** 走完整 UI 登录：身份 → 邮箱 → 密码 → 验证码 → 提交 */
export async function loginViaUI(page, roleKey, opts = {}) {
  const email = opts.email || DEMO[roleKey].email
  const password = opts.password || DEMO_PASSWORD

  await fillCredentials(page, roleKey, { email, password })
  await fillCaptcha(page)
  await page.click('button[type="submit"]')
  await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 20000 })
}

/** 读界面上的提示条文案（Toast，取最新一条） */
export async function toastText(page) {
  const t = page.locator('.toast .msg').last()
  try {
    await t.waitFor({ state: 'visible', timeout: 4000 })
    return ((await t.textContent()) || '').trim()
  } catch {
    return ''
  }
}

/**
 * 点提交并读取【本次操作新产生的】提示。
 *
 * 不能直接取 .last()：上一个用例的 toast 可能还没消失（3.2s 后自动移除），
 * 而新 toast 是异步（要等接口回来）才出现的，直接读会拿到上一条，造成假失败。
 * 所以这里先记下现有文案，点完等出现一条新的，再返回它。
 *
 * @param {import('playwright').Page} page
 * @param {string} [selector] 提交按钮选择器，默认登录页的提交按钮
 */
export async function submitAndReadToast(page, selector = 'button[type="submit"]') {
  const before = await page.locator('.toast .msg').allTextContents()
  await page.click(selector)

  await page
    .waitForFunction(
      (seen) => {
        const cur = [...document.querySelectorAll('.toast .msg')].map((e) => (e.textContent || '').trim())
        return cur.some((t) => !seen.includes(t))
      },
      before,
      { timeout: 8000 }
    )
    .catch(() => {
      /* 超时也往下走，至少把现场文案带回去，方便看失败原因 */
    })

  const after = (await page.locator('.toast .msg').allTextContents()).map((t) => t.trim())
  const fresh = after.filter((t) => !before.includes(t))
  return fresh.at(-1) || after.at(-1) || ''
}
