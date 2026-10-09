/**
 * 业务闭环回归测试（对生产构建跑）
 *
 * 目的：证明「三端合一 + 登录校验/验证码」改造后**原有业务功能不变**。
 * 完整跑通：用户下单 → 支付 → 商家接单 → 商家备餐 → 骑手抢单 → 骑手取餐 → 骑手送达 → 用户看到已完成。
 *
 * 运行前需：后端 8080 在跑、`npm run preview`（4173）在跑、Redis 在跑（用于取验证码答案）。
 */
import { chromium } from 'playwright'
import { gotoLogin, loginViaUI } from './e2e-lib.mjs'

const BASE = process.env.BASE || 'http://localhost:4173'
const SHOP_ID = process.env.SHOP_ID || '1'
const results = []
let failed = 0

function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const step = (s) => console.log(`\n───────── ${s} ─────────`)

const browser = await chromium.launch()
const ctx = await browser.newContext({ viewport: { width: 1440, height: 950 } })
const page = await ctx.newPage()
const errors = []
page.on('pageerror', (e) => errors.push(String(e)))

const path = () => new URL(page.url()).pathname

/** 清掉旧登录态 → 走完整 UI 登录（含图形验证码） */
async function loginAs(roleKey) {
  await gotoLogin(page, BASE)
  await loginViaUI(page, roleKey)
}

/** 在列表里按订单号定位那一张卡片 */
const ticketByNo = (no) => page.locator('article').filter({ hasText: no })

try {
  let orderNo = ''

  step('用户端：浏览 → 加购')
  await loginAs('user')
  await page.goto(`${BASE}/user/shop/${SHOP_ID}`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.pcard', { timeout: 15000 })
  const cards = await page.locator('.pcard').count()
  check('店内商品列表渲染', cards > 0, `${cards} 个商品卡片`)

  // 带规格的商品 add() 会拒绝（要求先选规格），所以优先选【无规格】的商品卡
  const plainCards = page.locator('.pcard').filter({ hasNot: page.locator('button.sku') })
  if ((await plainCards.count()) > 0) {
    await plainCards.first().locator('.add').first().click()
  } else {
    const card = page.locator('.pcard').first()
    await card.locator('button.sku').first().click()
    await card.locator('.add').first().click()
  }
  await page.waitForTimeout(1200)

  const toastMsg = (await page.locator('.toast .msg').first().textContent().catch(() => '')) || ''
  check('加购成功（出现「已加入」提示）', toastMsg.includes('已加入'), `提示：${toastMsg || '(无)'}`)
  const barText = (await page.locator('.cartbar .lbl').first().textContent().catch(() => '')) || ''
  check('购物车栏显示已选件数', /已选\s*\d+\s*件/.test(barText), `实际「${barText.trim()}」`)

  step('用户端：购物车 → 下单')
  await page.goto(BASE + '/user/cart', { waitUntil: 'networkidle' })
  await page.waitForSelector('input[type="radio"]', { timeout: 15000 })
  const radios = await page.locator('input[type="radio"]').count()
  check('购物车有商品且渲染出收货地址', radios > 0, `${radios} 个地址可选`)
  await page.locator('input[type="radio"]').first().check()

  await page.locator('button.btn-seal.wide').first().click()
  await page.waitForURL(/\/user\/orders\/\d+$/, { timeout: 15000 })
  check('下单成功并跳到订单详情', /\/user\/orders\/\d+$/.test(path()), path())

  orderNo = (await page.locator('p.no').first().textContent())?.trim() || ''
  check('拿到订单号', !!orderNo, orderNo)

  step('用户端：支付（Mock）')
  const payBtn = page.locator('button.btn-seal').filter({ hasText: /支付/ }).first()
  check('订单详情出现支付按钮', (await payBtn.count()) > 0)
  await payBtn.click()
  await page.waitForTimeout(1500)
  await page.reload({ waitUntil: 'networkidle' })
  const afterPay = (await page.locator('.tag').first().textContent())?.trim()
  check('支付后状态变为「待接单」', afterPay === '待接单', `实际「${afterPay}」`)

  step('商家端：接单 → 备餐')
  await loginAs('merchant')
  check('商家登录落点 /merchant/dashboard', path() === '/merchant/dashboard', path())

  await page.waitForSelector('article.ticket', { timeout: 15000 })
  const paidTicket = ticketByNo(orderNo)
  check('商家「待接单」列表里能找到该订单', (await paidTicket.count()) > 0)
  await paidTicket.locator('button:has-text("接单")').first().click()
  await page.waitForTimeout(1500)

  await page.locator('button:has-text("备餐中")').first().click()
  await page.waitForTimeout(1200)
  const prepTicket = ticketByNo(orderNo)
  check('切到「备餐中」能看到该订单', (await prepTicket.count()) > 0)
  await prepTicket.locator('button:has-text("备餐完成")').first().click()
  await page.waitForTimeout(1500)

  step('骑手端：抢单 → 取餐 → 送达')
  await loginAs('rider')
  check('骑手登录落点 /rider/orders', path() === '/rider/orders', path())
  await page.waitForSelector('article', { timeout: 15000 })
  const poolTicket = ticketByNo(orderNo)
  check('抢单池里能找到该订单', (await poolTicket.count()) > 0)
  await poolTicket.locator('button:has-text("抢这一单")').first().click()
  await page.waitForTimeout(1500)

  await page.goto(BASE + '/rider/mine', { waitUntil: 'networkidle' })
  await page.waitForSelector('article', { timeout: 15000 })
  const mineTicket = ticketByNo(orderNo)
  check('「我的配送」里能找到该订单', (await mineTicket.count()) > 0)
  await mineTicket.locator('button:has-text("已取到餐")').first().click()
  await page.waitForTimeout(1500)

  await ticketByNo(orderNo).locator('button:has-text("确认送达")').first().click()
  await page.waitForTimeout(1800)
  const riderDone = await ticketByNo(orderNo).locator('text=已送达').count()
  check('骑手端显示已送达', riderDone > 0)

  step('回到用户端：确认订单已完成')
  await loginAs('user')
  await page.goto(BASE + '/user/orders', { waitUntil: 'networkidle' })
  await page.waitForSelector('li.order', { timeout: 15000 })
  const userTicket = page.locator('li.order').filter({ hasText: orderNo })
  check('用户订单列表里能找到该订单', (await userTicket.count()) > 0, orderNo)
  const statusText = (await userTicket.locator('.tag').first().textContent())?.trim()
  check('用户端看到「已完成」', statusText === '已完成', `实际「${statusText}」`)

  step('运行时错误检查')
  check('全程无未捕获 JS 异常', errors.length === 0, errors.slice(0, 3).join(' | ') || '无')
} catch (e) {
  check('业务链路测试执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  await browser.close()
  process.exit(failed ? 1 : 0)
}
