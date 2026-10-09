// scripts/verify-mall-p3.mjs
// 电商板块 P3（购物车与地址）验收：省市区三级联动 / 收货地址 / 购物车。
//
// 重点验证：
//   ① 省市区：34 个省级、逐级下钻、末级 hasChildren=false、未知 parent 空列表
//   ② 地址：第一条自动默认、设默认互斥、areaCode 回填省市区、非末级被拒、越权防护
//   ③ 购物车：同 SKU 累加、无规格商品可加、价格实时取 SKU、件数与金额汇总
//   ④ 失效商品：下架后变 valid=false、不计入结算金额、不能选中、可一键清理、恢复后正常
//   ⑤ 参数校验：数量 0、超库存、缺少 skuId、非法 scope、未登录 401
//   ⑥ 隔离：不同用户的购物车互不可见/互不可改；外卖建的地址在电商可见
//
// 用法（需 8081 电商 + Redis + MySQL 在跑）：
//   node scripts/verify-mall-p3.mjs
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'

const MALL = String(process.env.MALL_BASE || 'http://localhost:8081').replace(/\/$/, '')

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

/* ---------------- HTTP ---------------- */

async function req(method, path, { headers = {}, body } = {}) {
  const h = { ...headers }
  if (body !== undefined) h['Content-Type'] = 'application/json'
  const res = await fetch(MALL + path, {
    method,
    headers: h,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let json = null
  try {
    json = JSON.parse(text)
  } catch {
    /* 非 JSON */
  }
  return { status: res.status, json, text }
}
const ok = (r) => r.json?.code === 1
const msg = (r) => r.json?.msg || ''

/* ---------------- CLI 小工具（redis 取验证码 / mysql 摆测试数据） ---------------- */

function makeCli(candidates, versionArgs, baseArgs) {
  let path = null
  return (args) => {
    if (!path) {
      for (const p of candidates) {
        const isPath = p.includes('/') || p.includes('\\')
        if (isPath && !fs.existsSync(p)) continue
        try {
          execFileSync(p, versionArgs, { stdio: 'ignore' })
          path = p
          break
        } catch {
          /* 下一个 */
        }
      }
      if (!path) throw new Error(`找不到可执行文件，尝试过：${candidates.join(', ')}`)
    }
    for (const a of baseArgs.map((b) => [...b, ...args])) {
      try {
        return execFileSync(path, a, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim()
      } catch {
        /* 重试 */
      }
    }
    throw new Error('命令执行失败：' + args.join(' '))
  }
}

const redis = makeCli(
  [
    process.env.REDIS_CLI,
    'redis-cli',
    'D:/redis/Redis-x64-5.0.14.1/redis-cli',
    'C:/Program Files/Redis/redis-cli.exe'
  ].filter(Boolean),
  ['--version'],
  [
    ['-h', process.env.REDIS_HOST || '127.0.0.1', '-p', process.env.REDIS_PORT || '6379',
      '-a', process.env.REDIS_PASSWORD || 'mehado', '--no-auth-warning'],
    ['-h', process.env.REDIS_HOST || '127.0.0.1', '-p', process.env.REDIS_PORT || '6379',
      '-a', process.env.REDIS_PASSWORD || 'mehado']
  ]
)

const mysql = makeCli(
  [
    process.env.MYSQL_CLI,
    'mysql',
    'D:/mysql/mysql-9.5.0-winx64/bin/mysql',
    'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe'
  ].filter(Boolean),
  ['--version'],
  [
    ['--ssl-mode=DISABLED', '--default-character-set=utf8mb4',
      '-h', process.env.MYSQL_HOST || '127.0.0.1', '-P', process.env.MYSQL_PORT || '3306',
      '-u', process.env.MYSQL_USER || 'root', `-p${process.env.MYSQL_PASSWORD || 'Ming1234'}`,
      '-D', process.env.MYSQL_DATABASE || 'supermall', '-N', '-B', '-e']
  ]
)
const sql = (s) => mysql([s])

/* ---------------- 登录 / 注册 ---------------- */

let adminToken = null

async function captcha() {
  const r = await req('GET', '/api/mall/auth/captcha')
  const key = r.json?.data?.key
  return { captchaKey: key, captchaCode: redis(['GET', `mall:captcha:${key}`]) }
}

async function adminLogin() {
  const cap = await captcha()
  const r = await req('POST', '/api/mall/admin/login', {
    body: { username: 'admin@qq.com', password: 'Admin1234', ...cap }
  })
  if (!ok(r)) throw new Error('管理员登录失败：' + r.text)
  adminToken = r.json.data.token
}

async function buyerLogin(username, password) {
  const cap = await captcha()
  const r = await req('POST', '/api/mall/user/login', {
    body: { username, password, ...cap }
  })
  if (!ok(r)) throw new Error(`买家登录失败（${username}）：` + r.text)
  return r.json.data.token
}

const A = (m, p, opts) => req(m, p, { headers: { 'mall-admin-token': adminToken }, ...(opts || {}) })
const PUB = (m, p) => req(m, p)

let tempToken = null
const newEmail = `p3verify${Date.now().toString(36)}@qq.com`
// opts 形态与 req 一致：{ body }；不传就是无 body 的请求
const U = (m, p, opts) => req(m, p, { headers: { token: tempToken }, ...(opts || {}) })

/* ---------------- 用例 ---------------- */

let originalProductStatus = []
let tempUserId = null
let user2Token = null

try {
  await adminLogin()
  console.log('（管理员已登录）')

  // 注册一个干净的临时买家，避免污染演示账号的地址与购物车
  const cap = await captcha()
  const reg = await req('POST', '/api/mall/user/register', {
    body: { username: newEmail, password: 'Mall1234', nickname: 'P3验收买家', ...cap }
  })
  if (!ok(reg)) throw new Error('临时买家注册失败：' + reg.text)
  tempToken = reg.json.data.token
  tempUserId = reg.json.data.id
  console.log(`（临时买家 ${newEmail} 已注册，id=${tempUserId}）`)

  // 购物车测试需要商品在售；种子里 status 默认 0（避免演示出现碎图），这里临时上架并在 finally 还原
  originalProductStatus = sql("select concat(id,':',status) from mall_product where id in (1,3) order by id")
    .split('\n').filter(Boolean)
  sql('update mall_product set status = 1 where id in (1,3)')

  // ============================================================
  section('1. 省市区三级联动（公开接口）')
  const provinces = await PUB('GET', '/api/mall/catalog/areas')
  check('根节点返回 34 个省级行政区', (provinces.json?.data || []).length === 34,
    `${(provinces.json?.data || []).length} 个`)
  check('省级节点 level=1 且都有下级',
    (provinces.json?.data || []).every((a) => a.level === 1 && a.hasChildren === true))

  const cities = await PUB('GET', '/api/mall/catalog/areas?parentCode=110000')
  check('北京市 → 市（110100 北京市）',
    (cities.json?.data || []).map((a) => a.code).join(',') === '110100',
    JSON.stringify(cities.json?.data))

  const districts = await PUB('GET', '/api/mall/catalog/areas?parentCode=110100')
  const dNames = (districts.json?.data || []).map((a) => a.name)
  check('北京市 → 区县包含东城区/朝阳区', dNames.includes('东城区') && dNames.includes('朝阳区'),
    dNames.slice(0, 4).join(' | '))
  check('区县是末级（hasChildren=false）',
    (districts.json?.data || []).every((a) => a.hasChildren === false && a.level === 3))

  const unknownArea = await PUB('GET', '/api/mall/catalog/areas?parentCode=999999')
  check('未知 parentCode 返回空数组', Array.isArray(unknownArea.json?.data) && unknownArea.json.data.length === 0)

  // ============================================================
  section('2. 收货地址：默认规则 + 区划回填')
  const a1 = await U('POST', '/api/mall/user/address', {
    body: { consignee: 'P3甲', phone: '13800138001', areaCode: '110101', detail: '东华门大街 1 号', label: '家' }
  })
  check('新增第一条地址成功', ok(a1), msg(a1))
  const addr1 = a1.json?.data

  const list1 = await U('GET', '/api/mall/user/address')
  const first = (list1.json?.data || []).find((a) => a.id === addr1)
  check('第一条地址自动成为默认地址', first?.isDefault === 1)
  check('areaCode 回填省 / 市 / 区名称',
    first?.province === '北京市' && first?.city === '北京市' && first?.district === '东城区',
    `${first?.province}/${first?.city}/${first?.district}`)
  check('areaCode 原样保存', first?.areaCode === '110101')

  const a2 = await U('POST', '/api/mall/user/address', {
    body: { consignee: 'P3乙', phone: '13800138002', areaCode: '440305', detail: '科技园路 2 号', label: '公司' }
  })
  check('新增第二条地址成功（不默认）', ok(a2), msg(a2))
  const addr2 = a2.json?.data

  const list2 = await U('GET', '/api/mall/user/address')
  check('列表默认地址排最前', (list2.json?.data || [])[0]?.id === addr1)
  check('第二条 isDefault=0',
    (list2.json?.data || []).find((a) => a.id === addr2)?.isDefault === 0)

  const detail = await U('GET', `/api/mall/user/address/${addr2}`)
  check('地址详情可用', ok(detail) && detail.json?.data?.district === '南山区',
    `${detail.json?.data?.province}/${detail.json?.data?.city}/${detail.json?.data?.district}`)

  const setDef = await U('PUT', `/api/mall/user/address/${addr2}/default`)
  check('设为默认地址', ok(setDef), msg(setDef))
  const list3 = await U('GET', '/api/mall/user/address')
  check('设默认后只有它是默认（互斥）',
    (list3.json?.data || []).filter((a) => a.isDefault === 1).length === 1 &&
    (list3.json?.data || []).find((a) => a.id === addr2)?.isDefault === 1)

  const upd = await U('PUT', '/api/mall/user/address', {
    body: { id: addr2, consignee: 'P3乙改', phone: '13800138002', areaCode: '440305', detail: '科技园路 22 号' }
  })
  check('修改地址成功', ok(upd), msg(upd))
  check('修改时未传 isDefault 仍保持默认',
    (await U('GET', `/api/mall/user/address/${addr2}`)).json?.data?.isDefault === 1)

  // 区划校验
  const nonLeaf = await U('POST', '/api/mall/user/address', {
    body: { consignee: 'P3丙', phone: '13800138003', areaCode: '110000', detail: 'x' }
  })
  check('areaCode 传省份（非末级）→ 被拒', !ok(nonLeaf) && msg(nonLeaf).includes('请选择到区'),
    msg(nonLeaf))

  const badArea = await U('POST', '/api/mall/user/address', {
    body: { consignee: 'P3丙', phone: '13800138003', areaCode: '999999', detail: 'x' }
  })
  check('areaCode 不存在 → 被拒', !ok(badArea) && msg(badArea).includes('行政区划不存在'), msg(badArea))

  const noArea = await U('POST', '/api/mall/user/address', {
    body: { consignee: 'P3丙', phone: '13800138003', province: '北京市', detail: 'x' }
  })
  check('不传 areaCode 且省市区不全 → 被拒', !ok(noArea) && msg(noArea).includes('请选择完整'), msg(noArea))

  const manual = await U('POST', '/api/mall/user/address', {
    body: { consignee: 'P3手填', phone: '13800138004', province: '浙江省', city: '杭州市', district: '西湖区', detail: '文一西路 3 号' }
  })
  check('手工省市区齐全 → 允许（外卖老地址兼容）', ok(manual), msg(manual))
  const addr3 = manual.json?.data

  const badPhone = await U('POST', '/api/mall/user/address', {
    body: { consignee: 'P3丁', phone: '12345', areaCode: '110101', detail: 'x' }
  })
  check('手机号格式错误 → 被拒', !ok(badPhone) && msg(badPhone).includes('手机号'), msg(badPhone))

  // 越权 / 未登录
  const otherAddr = await U('GET', '/api/mall/user/address/1')
  check('访问别人的地址 → 拒绝', !ok(otherAddr) && msg(otherAddr).includes('收货地址不存在'), msg(otherAddr))
  const noAuth = await req('GET', '/api/mall/user/address')
  check('未登录访问地址 → HTTP 401', noAuth.status === 401, `HTTP ${noAuth.status}`)

  // ============================================================
  section('3. 购物车：加购 / 累加 / 价格实时计算')
  await U('DELETE', '/api/mall/user/cart')
  const c0 = await U('GET', '/api/mall/user/cart')
  check('清空后购物车为空', (c0.json?.data?.items || []).length === 0)

  const add1 = await U('POST', '/api/mall/user/cart', { body: { skuId: 1, number: 2 } })
  check('加购 SKU 1（T恤 白/M）x2', ok(add1) && add1.json?.data?.number === 2, JSON.stringify(add1.json?.data))
  const add2 = await U('POST', '/api/mall/user/cart', { body: { skuId: 1, number: 1 } })
  check('同 SKU 再加 1 → 累加为 3', ok(add2) && add2.json?.data?.number === 3, JSON.stringify(add2.json?.data))
  const add3 = await U('POST', '/api/mall/user/cart', { body: { skuId: 9, number: 1 } })
  check('加购无规格商品（SKU 9）x1', ok(add3) && add3.json?.data?.number === 1, JSON.stringify(add3.json?.data))
  const itemSku1 = add2.json?.data?.id
  const itemSku9 = add3.json?.data?.id
  check('两次加购同一 SKU 只有一行（唯一键）', itemSku1 === add1.json?.data?.id)

  const cart = await U('GET', '/api/mall/user/cart')
  const cdata = cart.json?.data || {}
  const row1 = (cdata.items || []).find((i) => i.skuId === 1)
  const row9 = (cdata.items || []).find((i) => i.skuId === 9)
  check('购物车 2 行', (cdata.items || []).length === 2, `${(cdata.items || []).length} 行`)
  check('价格实时取自 SKU（79.00 / 39.90）',
    Number(row1?.price) === 79 && Number(row9?.price) === 39.9,
    `${row1?.price} / ${row9?.price}`)
  check('规格文案正确（白 / M）', row1?.specText === '白 / M', String(row1?.specText))
  check('无规格商品 specText 为空', !row9?.specText, String(row9?.specText))
  check('件数汇总 totalNumber=4', cdata.totalNumber === 4, `${cdata.totalNumber}`)
  check('默认全部选中（selectedCount=2）', cdata.selectedCount === 2, `${cdata.selectedCount}`)
  check('结算金额 = 79×3 + 39.9 = 276.90', Number(cdata.selectedAmount) === 276.9, `${cdata.selectedAmount}`)
  check('allSelected=true', cdata.allSelected === true)

  const count = await U('GET', '/api/mall/user/cart/count')
  check('角标 total=2 / invalid=0',
    count.json?.data?.total === 2 && count.json?.data?.invalid === 0, JSON.stringify(count.json?.data))

  // ============================================================
  section('4. 购物车：改数量 / 勾选 / 校验')
  const updNum = await U('PUT', `/api/mall/user/cart/${itemSku1}`, { body: { number: 5 } })
  check('改数量为 5 成功', ok(updNum), msg(updNum))
  const overStock = await U('PUT', `/api/mall/user/cart/${itemSku1}`, { body: { number: 500 } })
  check('改数量超过库存 → 被拒', !ok(overStock) && msg(overStock).includes('库存不足'), msg(overStock))
  const overMax = await U('PUT', `/api/mall/user/cart/${itemSku1}`, { body: { number: 9999 } })
  check('数量超过 999 → 参数校验失败', !ok(overMax) && msg(overMax).includes('999'), msg(overMax))
  const zeroNum = await U('PUT', `/api/mall/user/cart/${itemSku1}`, { body: { number: 0 } })
  check('数量 0 → 参数校验失败', !ok(zeroNum) && msg(zeroNum).includes('至少为 1'), msg(zeroNum))
  const noSku = await U('POST', '/api/mall/user/cart', { body: { number: 1 } })
  check('缺 skuId → 参数校验失败', !ok(noSku) && msg(noSku).includes('skuId'), msg(noSku))

  const unselect = await U('PUT', `/api/mall/user/cart/${itemSku1}/selected?selected=0`)
  check('取消勾选单行', ok(unselect), msg(unselect))
  const afterUnselect = await U('GET', '/api/mall/user/cart')
  check('取消勾选后金额只剩 SKU 9（39.90）',
    Number(afterUnselect.json?.data?.selectedAmount) === 39.9,
    `${afterUnselect.json?.data?.selectedAmount}`)
  check('allSelected 变为 false', afterUnselect.json?.data?.allSelected === false)

  const selectAll = await U('PUT', '/api/mall/user/cart/selected?selected=1')
  check('一键全选', ok(selectAll), msg(selectAll))
  const afterSelectAll = await U('GET', '/api/mall/user/cart')
  check('全选后金额 = 79×5 + 39.9 = 434.90',
    Number(afterSelectAll.json?.data?.selectedAmount) === 434.9,
    `${afterSelectAll.json?.data?.selectedAmount}`)

  // 越权：user2 不能改临时用户的购物车行
  user2Token = await buyerLogin('user2@qq.com', 'Mall1234')
  const u2 = (m, p, opts) => req(m, p, { headers: { token: user2Token }, ...(opts || {}) })
  await u2('DELETE', '/api/mall/user/cart?scope=all')
  const u2edit = await u2('PUT', `/api/mall/user/cart/${itemSku1}`, { body: { number: 1 } })
  check('改别人的购物车行 → 拒绝', !ok(u2edit) && msg(u2edit).includes('购物车中不存在'), msg(u2edit))
  const u2list = await u2('GET', '/api/mall/user/cart')
  check('别人的购物车互不可见', (u2list.json?.data?.items || []).length === 0)

  // ============================================================
  section('5. 失效商品：下架后的表现')
  // product 1 下架，product 3 保持上架（脚本开头已临时上架）
  sql('update mall_product set status = 0 where id = 1')
  sql('update mall_product set status = 1 where id = 3')

  const cartInvalid = await U('GET', '/api/mall/user/cart')
  const invRow = (cartInvalid.json?.data?.items || []).find((i) => i.skuId === 1)
  check('下架商品在购物车仍可见（不消失）', !!invRow)
  check('下架商品 valid=false', invRow?.valid === false)
  check('失效原因 = 商品已下架', invRow?.invalidReason === '商品已下架', String(invRow?.invalidReason))
  check('失效商品不计入结算金额（只剩 39.90）',
    Number(cartInvalid.json?.data?.selectedAmount) === 39.9,
    `${cartInvalid.json?.data?.selectedAmount}`)
  check('invalidCount=1', cartInvalid.json?.data?.invalidCount === 1)

  const selectInvalid = await U('PUT', `/api/mall/user/cart/${itemSku1}/selected?selected=1`)
  check('失效商品不能被选中', !ok(selectInvalid) && msg(selectInvalid).includes('已失效'), msg(selectInvalid))

  const addOffSale = await U('POST', '/api/mall/user/cart', { body: { skuId: 1, number: 1 } })
  check('下架商品不能再加购', !ok(addOffSale) && msg(addOffSale).includes('已下架'), msg(addOffSale))

  const allSelValid = await U('PUT', '/api/mall/user/cart/selected?selected=1')
  check('一键全选只作用于有效商品', ok(allSelValid), msg(allSelValid))
  const afterAllSel = await U('GET', '/api/mall/user/cart')
  check('失效行仍未被选中',
    (afterAllSel.json?.data?.items || []).find((i) => i.skuId === 1)?.selected === 0)

  const clearInvalid = await U('DELETE', '/api/mall/user/cart?scope=invalid')
  check('清理失效商品', ok(clearInvalid), msg(clearInvalid))
  const afterClearInvalid = await U('GET', '/api/mall/user/cart')
  check('清理后只剩 1 行（无规格商品）',
    (afterClearInvalid.json?.data?.items || []).length === 1 &&
    afterClearInvalid.json?.data?.items?.[0]?.skuId === 9)

  // 恢复商品状态，验证失效行恢复后能继续用
  sql('update mall_product set status = 1 where id = 1')
  const addBack = await U('POST', '/api/mall/user/cart', { body: { skuId: 1, number: 1 } })
  check('商品恢复上架后可再次加购', ok(addBack) && addBack.json?.data?.number === 1, msg(addBack))

  const badScope = await U('DELETE', '/api/mall/user/cart?scope=weird')
  check('非法 scope → 被拒', !ok(badScope) && msg(badScope).includes('all 或 invalid'), msg(badScope))

  // ============================================================
  section('6. 删除 / 清空 / 未登录')
  const delOne = await U('DELETE', `/api/mall/user/cart/${addBack.json?.data?.id}`)
  check('删除单行', ok(delOne), msg(delOne))
  const clearAll = await U('DELETE', '/api/mall/user/cart?scope=all')
  check('清空购物车', ok(clearAll), msg(clearAll))
  const finalCount = await U('GET', '/api/mall/user/cart/count')
  check('清空后 total=0', finalCount.json?.data?.total === 0, JSON.stringify(finalCount.json?.data))

  const cartNoAuth = await req('GET', '/api/mall/user/cart')
  check('未登录访问购物车 → HTTP 401', cartNoAuth.status === 401, `HTTP ${cartNoAuth.status}`)

  // ============================================================
  section('7. 共用地址表：外卖建的地址在电商可见')
  // user1 在外卖种子里有一条地址（id=1，无 area_code），电商列表应能看到
  const user1Token = await buyerLogin('user1@qq.com', 'Mall1234')
  const u1 = (m, p) => req(m, p, { headers: { token: user1Token } })
  const u1list = await u1('GET', '/api/mall/user/address')
  check('user1 能看到外卖种子地址（共享 address 表）',
    (u1list.json?.data || []).some((a) => a.id === 1 && a.consignee === '小明'),
    JSON.stringify((u1list.json?.data || []).map((a) => a.consignee)))
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  // ---------- 清理：删掉临时买家的一切痕迹 + 恢复商品状态 ----------
  try {
    if (tempUserId) {
      sql(`delete from mall_cart_item where user_id = ${tempUserId}`)
      sql(`delete from address where user_id = ${tempUserId}`)
      sql(`delete from sys_user where id = ${tempUserId}`)
      console.log(`\n（已清理临时买家 ${newEmail} 及其购物车 / 地址）`)
    }
    for (const pair of originalProductStatus) {
      const [id, status] = pair.split(':')
      sql(`update mall_product set status = ${status} where id = ${id}`)
    }
    if (originalProductStatus.length) {
      console.log('（已恢复商品 status 到验收前的值）')
    }
  } catch (e) {
    console.log('（清理失败，请手工检查：', e.message, '）')
  }

  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
