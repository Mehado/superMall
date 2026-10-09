/**
 * 三端合一验收测试（对【生产构建】跑，等价于 Nginx 单端口部署）
 *
 * 覆盖需求第八节全部 8 条验收标准，外加：
 *   - 登录校验（邮箱格式 / 常用后缀 / 密码强度 / 图形验证码）
 *   - 主页面 = 用户端店铺首页（游客可逛 + 左上角登录按钮）
 *
 * 运行前需：后端 8080 在跑、`npm run preview`（4173）在跑、Redis 在跑（用于取验证码答案）。
 */
import { chromium } from 'playwright'
import {
  DEMO,
  DEMO_PASSWORD,
  fillCaptcha,
  fillCredentials,
  gotoLogin,
  loginViaUI,
  refreshCaptcha,
  submitAndReadToast
} from './e2e-lib.mjs'

const BASE = process.env.BASE || 'http://localhost:4173'
const results = []
let failed = 0

function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}

const browser = await chromium.launch()
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
const page = await ctx.newPage()

/** 记录所有 JS 请求，用于验证分包隔离 */
const requested = []
page.on('request', (r) => {
  const u = r.url()
  if (u.endsWith('.js') || u.endsWith('.vue')) requested.push(u)
})
const errors = []
page.on('pageerror', (e) => errors.push(String(e)))

/**
 * 把请求 URL 规范化成模块名，让【生产构建】和【dev server】两种形态都能断言：
 *   生产：/assets/HomeView-mCYnoAZo.js   → HomeView
 *   dev ：/src/views/user/HomeView.vue   → HomeView
 */
function modName(u) {
  const prod = u.match(/\/assets\/([A-Za-z0-9_]+)-[A-Za-z0-9_-]+\.js$/)
  if (prod) return prod[1]
  const dev = u.match(/\/src\/.+\/([A-Za-z0-9_]+)\.vue/)
  if (dev) return dev[1]
  return null
}
const reqMods = () => requested.map(modName).filter(Boolean)

/** 用户端【专属】模块（商家/骑手端不应加载） */
const USER_ONLY = ['HomeView', 'MerchantsView', 'ProductDetailView', 'CartView', 'OrderDetailView', 'PriceTag', 'EmptyState']

async function goto(path) {
  await page.goto(BASE + path, { waitUntil: 'networkidle' })
}
const path = () => new URL(page.url()).pathname
const token = () => page.evaluate(() => localStorage.getItem('mall_token'))
const role = () => page.evaluate(() => localStorage.getItem('mall_role'))

async function logout() {
  const btn = page.locator('button:has-text("退出")').first()
  await btn.click()
  await page.waitForURL((u) => u.pathname === '/login', { timeout: 10000 })
}

try {
  console.log('\n───────── 验收 1：只有一个登录入口，能切换三种身份 ─────────')
  await gotoLogin(page, BASE)
  const tabCount = await page.locator('.roles button').count()
  check('登录页有且仅有 3 个身份 Tab', tabCount === 3, `实际 ${tabCount}`)
  for (const r of ['user', 'merchant', 'rider']) {
    await page.click(`.roles button:has-text("${DEMO[r].label}")`)
    const on = await page.locator(`.roles button:has-text("${DEMO[r].label}")`).getAttribute('class')
    check(`可切换到「${DEMO[r].label}」身份`, String(on).includes('on'))
  }

  console.log('\n───────── 新增：登录校验（邮箱 / 密码 / 图形验证码）─────────')

  // 验证码控件确实渲染了，并且能拿到 key、能点击刷新换一张
  check('登录页渲染了图形验证码', (await page.locator('.captcha').count()) > 0)
  const key1 = await page.locator('.captcha').getAttribute('data-captcha-key')
  check('验证码下发了 key', !!key1)
  await refreshCaptcha(page)
  const key1b = await page.locator('.captcha').getAttribute('data-captcha-key')
  check('点图片能换一张（key 变化）', !!key1b && key1b !== key1)

  // 邮箱格式不对
  await fillCredentials(page, 'user', { email: 'not-an-email', password: DEMO_PASSWORD })
  await fillCaptcha(page)
  let msg = await submitAndReadToast(page)
  check('邮箱格式不对 → 被拦并提示', /邮箱/.test(msg), `提示：${msg}`)

  // 后缀不在常用清单（qq.con 这类错别字）
  await page.fill('#login-email', 'user1@qq.con')
  await page.fill('#login-password', DEMO_PASSWORD)
  msg = await submitAndReadToast(page)
  check('不支持的邮箱后缀 → 被拦并提示', /后缀|常用邮箱/.test(msg), `提示：${msg}`)

  // 弱密码（旧的 123456）
  await page.fill('#login-email', DEMO.user.email)
  await page.fill('#login-password', '123456')
  msg = await submitAndReadToast(page)
  check('弱密码 123456 → 被拦并提示', /密码不达标/.test(msg), `提示：${msg}`)

  // 缺大写 / 缺数字
  await page.fill('#login-password', 'mall1234')
  msg = await submitAndReadToast(page)
  check('缺大写字母 → 被拦', /大写/.test(msg), `提示：${msg}`)

  await page.fill('#login-password', 'MallAbcd')
  msg = await submitAndReadToast(page)
  check('缺数字 → 被拦', /数字/.test(msg), `提示：${msg}`)

  // 密码强度指示器逐条点亮
  await page.fill('#login-password', 'Mall1234')
  await page.waitForTimeout(150)
  const okCount = await page.locator('.meter .rules li.ok').count()
  check('密码强度指示器 4 条全亮', okCount === 4, `实际亮 ${okCount} 条`)

  // 验证码为空
  await page.fill('.captcha input.code', '')
  msg = await submitAndReadToast(page)
  check('验证码为空 → 被拦', /验证码/.test(msg), `提示：${msg}`)

  // 验证码错误
  await page.fill('.captcha input.code', 'ZZZZ')
  msg = await submitAndReadToast(page)
  check('验证码错误 → 被拦', /验证码不正确/.test(msg), `提示：${msg}`)

  // 失败后验证码应自动换一张（一次性，不复用）
  const keyAfterFail = await page.locator('.captcha').getAttribute('data-captcha-key')
  check('登录失败后验证码自动刷新', !!keyAfterFail && keyAfterFail !== key1)

  // 邮箱后缀补全建议
  await page.fill('#login-email', 'user1@q')
  await page.waitForTimeout(200)
  const sugText = await page.locator('.sugs .sug').first().textContent().catch(() => '')
  check('输入 @q 时给出后缀补全建议', (sugText || '').includes('user1@qq.com'), `建议：${sugText}`)
  await page.locator('.sugs .sug').first().click()
  check('点建议可补全邮箱', (await page.inputValue('#login-email')) === 'user1@qq.com')

  // 只填用户名时给常用后缀快捷按钮
  await page.fill('#login-email', 'someone')
  await page.waitForTimeout(200)
  const popText = await page.locator('.sugs .sug').first().textContent().catch(() => '')
  check('未填 @ 时给出常用后缀按钮', (popText || '').includes('@'), `按钮：${popText}`)
  await page.locator('.sugs .sug').first().click()
  const filled = await page.inputValue('#login-email')
  check('点常用后缀可拼出完整邮箱', /^someone@/.test(filled), `结果：${filled}`)

  console.log('\n───────── 新增：注册流程（用户身份，注册即登录）─────────')
  await gotoLogin(page, BASE)
  await page.click('.roles button:has-text("用户")')
  await page.click('button:has-text("注册新用户")')
  await page.waitForTimeout(200)
  check(
    '切到注册模式后出现昵称/手机号字段',
    (await page.locator('#login-nickname').count()) > 0 && (await page.locator('#login-phone').count()) > 0
  )

  // 注册时的密码强度同样被拦（不消费验证码：前端就拦住了，没发请求）
  const newEmail = `e2e${Date.now().toString(36)}@qq.com`
  await page.fill('#login-email', newEmail)
  await page.fill('#login-password', 'weakpass')
  await fillCaptcha(page)
  let rmsg = await submitAndReadToast(page)
  check('注册时弱密码被拦', /密码不达标/.test(rmsg), `提示：${rmsg}`)

  // 合法数据 → 注册成功并直接拿到登录态（不再二次请求登录）
  await page.fill('#login-password', 'E2eTest1234')
  await refreshCaptcha(page)
  await fillCaptcha(page)
  const loginCalls = []
  const onReq = (r) => {
    if (/\/api\/user\/(register|login)/.test(r.url())) loginCalls.push(r.url().split('/api')[1])
  }
  page.on('request', onReq)
  await page.click('button[type="submit"]')
  await page.waitForURL((u) => u.pathname === '/user/home', { timeout: 20000 })
  page.off('request', onReq)

  check('合法邮箱+强密码+验证码 → 注册成功并自动登录', path() === '/user/home', `落到 ${path()}`)
  check('注册过程只发了一次请求（未重复调登录）', loginCalls.length === 1 && loginCalls[0].startsWith('/user/register'), `实际：${loginCalls.join(', ')}`)
  const nick = ((await page.locator('.account .who .name').textContent()) || '').trim()
  check('昵称默认取邮箱 @ 前面的部分', nick.startsWith('e2e'), `昵称：${nick}`)
  console.log(`  ℹ️  本次创建账号 ${newEmail}，如需清理：DELETE FROM sys_user WHERE username LIKE 'e2e%@qq.com';`)
  await page.evaluate(() => localStorage.clear())

  console.log('\n───────── 新增：主页面 = 用户端店铺首页（游客可逛） ─────────')
  await page.evaluate(() => localStorage.clear())
  await page.goto(BASE + '/', { waitUntil: 'networkidle' })
  check('游客访问 / 落到 /user/home', path() === '/user/home', `落到 ${path()}`)
  await page.waitForSelector('.pcard', { timeout: 15000 }).catch(() => {})
  const feed = await page.locator('.pcard').count()
  check('未登录也能看到商品列表（可滚动浏览）', feed > 0, `${feed} 个商品卡片`)

  const loginBtn = page.locator('.account .loginbtn')
  check('左上角有登录按钮', (await loginBtn.count()) > 0)
  const btnBox = await loginBtn.boundingBox()
  const brandBox = await page.locator('.bar .brand').boundingBox()
  check(
    '登录按钮位于左上角（在 Logo 左侧）',
    !!btnBox && !!brandBox && btnBox.x < brandBox.x,
    btnBox && brandBox ? `登录 x=${Math.round(btnBox.x)} / Logo x=${Math.round(brandBox.x)}` : ''
  )

  await loginBtn.click()
  await page.waitForURL((u) => u.pathname === '/login', { timeout: 10000 })
  check('点左上角登录按钮进入 /login', path() === '/login', `落到 ${path()}`)

  // 从店铺内页点登录 → 登录后回到该页（redirect 生效）
  await page.evaluate(() => localStorage.clear())
  await page.goto(BASE + '/user/merchants', { waitUntil: 'networkidle' })
  await page.locator('.account .loginbtn').click()
  await page.waitForURL((u) => u.pathname === '/login', { timeout: 10000 })
  await page.click('.roles button:has-text("用户")')
  await page.fill('#login-email', DEMO.user.email)
  await page.fill('#login-password', DEMO_PASSWORD)
  await fillCaptcha(page)
  await page.click('button[type="submit"]')
  await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 20000 })
  check('登录后回到点登录前的页面', path() === '/user/merchants', `落到 ${path()}`)
  await page.evaluate(() => localStorage.clear())

  console.log('\n───────── 验收 3：未登录访问内页 → 跳登录 ─────────')
  for (const p of ['/user/cart', '/user/orders', '/user/profile', '/merchant/dashboard', '/rider/orders']) {
    await goto(p)
    check(`游客访问 ${p} → /login`, path() === '/login', `落到 ${path()}`)
  }

  console.log('\n───────── 补充：用户端游客可浏览（保留原有业务） ─────────')
  for (const p of ['/user/home', '/user/merchants']) {
    await goto(p)
    check(`游客可浏览 ${p}`, path() === p, `落到 ${path()}`)
  }

  console.log('\n───────── 验收 2a：用户登录 → /user/home ─────────')
  await gotoLogin(page, BASE)
  await loginViaUI(page, 'user')
  check('用户登录后进入 /user/home', path() === '/user/home', `落到 ${path()}`)
  check('role 持久化为 user', (await role()) === 'user')

  console.log('\n───────── 验收 5：刷新页面登录态不丢 ─────────')
  const t1 = await token()
  await page.reload({ waitUntil: 'networkidle' })
  check('刷新后仍在 /user/home', path() === '/user/home', `落到 ${path()}`)
  check('刷新后 token 仍存在', (await token()) === t1 && !!t1)
  check('刷新后 role 仍为 user', (await role()) === 'user')

  console.log('\n───────── 验收 4：用户访问商家路由 → 被拦截 ─────────')
  await goto('/merchant/dashboard')
  check('用户访问 /merchant/dashboard → /login', path() === '/login', `落到 ${path()}`)
  check('被拦截后仍保留登录态（未被登出）', (await role()) === 'user')
  await goto('/rider/orders')
  check('用户访问 /rider/orders → /login', path() === '/login', `落到 ${path()}`)
  const notice = await page.locator('.current').count()
  check('已登录时 /login 放行并提示当前身份', notice > 0)
  await page.click('button:has-text("回我的主页")')
  await page.waitForURL((u) => u.pathname === '/user/home', { timeout: 8000 })
  check('可从登录页一键回到自己的主页', path() === '/user/home', `落到 ${path()}`)

  console.log('\n───────── 验收 6：退出登录 → 回登录页 ─────────')
  await logout()
  check('退出后回到 /login', path() === '/login', `落到 ${path()}`)
  check('退出后 token 已清空', !(await token()))
  check('退出后 role 已清空', !(await role()))
  await goto('/user/cart')
  check('退出后访问内页又被拦回 /login', path() === '/login', `落到 ${path()}`)

  console.log('\n───────── 验收 2b/2c：商家 & 骑手登录落点 ─────────')
  await gotoLogin(page, BASE)
  await loginViaUI(page, 'merchant')
  check('商家登录后进入 /merchant/dashboard', path() === '/merchant/dashboard', `落到 ${path()}`)
  check('商家角色 = merchant', (await role()) === 'merchant')
  await page.goto(BASE + '/', { waitUntil: 'networkidle' })
  check('已登录商家访问 / → /merchant/dashboard', path() === '/merchant/dashboard', `落到 ${path()}`)
  const shopName = await page.locator('.shopname').first().textContent()
  check('商家端渲染出店铺名', !!shopName && shopName.trim().length > 0, `"${shopName?.trim()}"`)
  await goto('/merchant/products/goods')
  check('商家端百货商品页可访问', path() === '/merchant/products/goods', `落到 ${path()}`)
  await logout()

  await gotoLogin(page, BASE)
  await loginViaUI(page, 'rider')
  check('骑手登录后进入 /rider/orders', path() === '/rider/orders', `落到 ${path()}`)
  check('骑手 role = rider', (await role()) === 'rider')
  await goto('/rider/mine')
  check('骑手端我的配送可访问', path() === '/rider/mine', `落到 ${path()}`)
  await logout()

  console.log('\n───────── 验收 7：history 路由刷新直达（SPA 回退） ─────────')
  await gotoLogin(page, BASE)
  await loginViaUI(page, 'merchant')
  await page.goto(BASE + '/merchant/categories', { waitUntil: 'networkidle' })
  check('深链 /merchant/categories 可直接打开', path() === '/merchant/categories', `落到 ${path()}`)
  check('深链后仍是登录态', (await role()) === 'merchant')
  await logout()

  console.log('\n───────── 验收 8：三个端代码分包、互不加载 ─────────')
  requested.length = 0
  await gotoLogin(page, BASE)
  await loginViaUI(page, 'merchant')
  await page.waitForTimeout(600)
  let mods = reqMods()
  let leak = mods.filter((m) => USER_ONLY.includes(m))
  check('商家端未加载任何用户端页面', leak.length === 0, [...new Set(leak)].join(', ') || '零泄漏')
  check('商家端加载了 MerchantLayout', mods.includes('MerchantLayout'))
  check('商家端加载了自己的 OrdersView', mods.includes('OrdersView'))

  requested.length = 0
  await logout()
  await loginViaUI(page, 'rider')
  await page.waitForTimeout(600)
  mods = reqMods()
  leak = mods.filter((m) => USER_ONLY.includes(m))
  check('骑手端未加载任何用户端页面', leak.length === 0, [...new Set(leak)].join(', ') || '零泄漏')
  check('骑手端加载了 RiderLayout', mods.includes('RiderLayout'))

  requested.length = 0
  await logout()
  await page.goto(BASE + '/user/home', { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  mods = reqMods()
  check('用户端加载了 HomeView', mods.includes('HomeView'))
  check('用户端未加载商家端 Layout', !mods.includes('MerchantLayout'))
  check('用户端未加载骑手端 Layout', !mods.includes('RiderLayout'))

  console.log('\n───────── 补充：404 ─────────')
  await goto('/no/such/page')
  const nf = await page.locator('text=这个页面不存在').count()
  check('未知路由渲染 404 页', nf > 0)

  console.log('\n───────── 运行时错误检查 ─────────')
  check('全程无未捕获 JS 异常', errors.length === 0, errors.slice(0, 3).join(' | ') || '无')
} catch (e) {
  check('测试脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  await browser.close()
  process.exit(failed ? 1 : 0)
}
