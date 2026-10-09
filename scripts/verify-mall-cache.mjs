// scripts/verify-mall-cache.mjs
// 电商板块「商品详情缓存三防」验收（与外卖 ProductServiceImpl#getDetail 同款）。
//
// 重点验证：
//   ① 缓存写入：公开详情回源后写 mall:product:detail:{id}，TTL 落在 [300,420) 秒
//   ② 命中缓存：二次请求不再重置 TTL（没有回源）
//   ③ 防穿透：不存在的商品 → 写 NULL 标记 60s，且后续请求不再打 DB
//   ④ 防击穿：删缓存后并发 50 请求，MySQL Com_select 增量远小于请求数（只回源一次）
//   ⑤ 防雪崩：多次写缓存的 TTL 有随机抖动（不是同一个值）
//   ⑥ 一致性：后台改商品 / 上下架 / 删除 都会删缓存；公开详情读到最新值
//   ⑦ 后台 detail 不吃缓存（直查 DB），公开 detail 吃缓存（表现为"旧值"）
//   ⑧ 命名空间：只产生 mall: 前缀的 key，绝不写外卖的 product:detail: / cache:lock:
//
// 用法（需 8081 电商 + Redis + MySQL 在跑）：
//   node scripts/verify-mall-cache.mjs
// 可选：
//   MALL_BASE=http://localhost:8081
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'

const MALL = String(process.env.MALL_BASE || 'http://localhost:8081').replace(/\/$/, '')
const PRODUCT_CACHE_PREFIX = 'mall:product:detail:'
const CACHE_LOCK_PREFIX = 'mall:cache:lock:'

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

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
          /* next */
        }
      }
      if (!path) throw new Error(`找不到可执行文件：${candidates.join(', ')}`)
    }
    for (const a of baseArgs.map((b) => [...b, ...args])) {
      try {
        return execFileSync(path, a, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim()
      } catch {
        /* retry */
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

const cacheKey = (id) => PRODUCT_CACHE_PREFIX + id
const ttlOf = (key) => Number(redis(['TTL', key]))
const exists = (key) => redis(['EXISTS', key]) === '1'
const delCache = (id) => redis(['DEL', cacheKey(id)])
const comSelect = () =>
  Number(sql("show global status like 'Com_select'").split(/\s+/).pop())

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
const A = (m, p, body) => req(m, p, { headers: { 'mall-admin-token': adminToken }, body })
const P = (id) => req('GET', `/api/mall/catalog/products/${id}`)

const stamp = Date.now().toString(36)
let pid = null
let pid2 = null

try {
  await adminLogin()
  console.log('（管理员已登录）')

  // 找一个叶子分类（T恤）
  const tree = await A('GET', '/api/mall/admin/categories/tree')
  const findNode = (nodes, name) => {
    for (const n of nodes || []) {
      if (n.name === name) return n
      const hit = findNode(n.children, name)
      if (hit) return hit
    }
    return null
  }
  const tshirt = findNode(tree.json?.data || [], 'T恤')
  check('找到叶子分类「T恤」可用于建临时商品', !!tshirt?.id, `categoryId=${tshirt?.id}`)

  const create = await A('POST', '/api/mall/admin/products', {
    categoryId: tshirt?.id,
    name: `缓存验收-${stamp}`,
    subtitle: '缓存三防验收用临时商品',
    status: 1,
    images: [],
    specs: [],
    skus: [{ price: 199, stock: 100, specValues: [] }]
  })
  check('新增验收商品成功（上架）', ok(create) && !!create.json?.data, msg(create))
  pid = create.json?.data

  // ============================================================
  section('1. 缓存写入与命中')
  delCache(pid)
  check('请求前缓存不存在', !exists(cacheKey(pid)))

  const first = await P(pid)
  check('公开详情可用', ok(first) && first.json?.data?.id === pid, msg(first))
  check('缓存已写入 mall:product:detail:{id}', exists(cacheKey(pid)))
  const ttl1 = ttlOf(cacheKey(pid))
  check('缓存 TTL 落在 [300,420) 秒', ttl1 >= 300 && ttl1 < 420, `ttl=${ttl1}`)
  check('缓存内容含商品名', redis(['GET', cacheKey(pid)]).includes(`缓存验收-${stamp}`))
  check('缓存内容含多图/SKU 字段（详情完整结构）',
    redis(['GET', cacheKey(pid)]).includes('"skus"') && redis(['GET', cacheKey(pid)]).includes('"images"'))

  const second = await P(pid)
  const ttl2 = ttlOf(cacheKey(pid))
  check('二次请求命中缓存（内容一致）', second.json?.data?.name === first.json?.data?.name)
  check('命中缓存时 TTL 不被重置（没有回源重写）', ttl2 <= ttl1, `${ttl1} → ${ttl2}`)

  // ============================================================
  section('2. 防穿透（空值缓存）')
  const ghostId = 999999999
  const ghost1 = await P(ghostId)
  check('不存在的商品 → 报「商品不存在」', !ok(ghost1) && msg(ghost1).includes('商品不存在'), msg(ghost1))
  check('空值标记已写入', exists(cacheKey(ghostId)))
  check('空值标记内容为 NULL', redis(['GET', cacheKey(ghostId)]) === 'NULL')
  const ghostTtl1 = ttlOf(cacheKey(ghostId))
  check('空值标记 TTL ≤ 60 秒', ghostTtl1 > 0 && ghostTtl1 <= 60, `ttl=${ghostTtl1}`)

  const ghost2 = await P(ghostId)
  const ghostTtl2 = ttlOf(cacheKey(ghostId))
  check('再次请求仍报不存在（走空值缓存）', !ok(ghost2) && msg(ghost2).includes('商品不存在'))
  check('空值标记未被重写（TTL 递减 → 没回源 DB）', ghostTtl2 <= ghostTtl1, `${ghostTtl1} → ${ghostTtl2}`)

  // ============================================================
  section('3. 防击穿（互斥锁只放一个请求回源）')
  delCache(pid)
  const before = comSelect()
  const burst = await Promise.all(Array.from({ length: 50 }, () => P(pid)))
  const after = comSelect()
  const delta = after - before
  check('50 并发全部成功', burst.every((r) => ok(r) && r.json?.data?.id === pid))
  check('50 并发返回内容一致', new Set(burst.map((r) => r.json?.data?.name)).size === 1)
  check('只回源一次（Com_select 增量远小于 50 次请求）', delta < 25,
    `Com_select 增量=${delta}（每请求都回源的话约 300）`)

  const lockLeft = redis(['KEYS', `${CACHE_LOCK_PREFIX}${cacheKey(pid)}`])
  check('并发结束后互斥锁已释放（无残留锁）', lockLeft === '', `残留=[${lockLeft}]`)
  check('并发后缓存已写入', exists(cacheKey(pid)))

  // ============================================================
  section('4. 防雪崩（随机 TTL）')
  const ttls = []
  for (let i = 0; i < 6; i++) {
    delCache(pid)
    await P(pid)
    ttls.push(ttlOf(cacheKey(pid)))
  }
  check('6 次写缓存的 TTL 不全相同（有随机抖动）', new Set(ttls).size > 1, `ttls=${ttls.join(',')}`)
  check('所有 TTL 都在 [300,420) 秒内', ttls.every((t) => t >= 300 && t < 420), `ttls=${ttls.join(',')}`)

  // ============================================================
  section('5. 写操作失效缓存（Cache-Aside）')
  await P(pid) // 预热
  check('预热成功', exists(cacheKey(pid)))

  const newName = `缓存验收改名-${stamp}`
  const rename = await A('PUT', '/api/mall/admin/products', {
    id: pid,
    categoryId: tshirt?.id,
    name: newName,
    subtitle: '缓存三防验收用临时商品',
    status: 1,
    images: [],
    specs: [],
    skus: [{ price: 199, stock: 100, specValues: [] }]
  })
  check('后台改商品成功', ok(rename), msg(rename))
  check('改商品后缓存被删除', !exists(cacheKey(pid)))
  const afterRename = await P(pid)
  check('公开详情读到新名字', afterRename.json?.data?.name === newName, afterRename.json?.data?.name)

  await P(pid) // 预热
  const offSale = await A('PUT', `/api/mall/admin/products/${pid}/status?status=0`)
  check('下架成功', ok(offSale), msg(offSale))
  check('上下架后缓存被删除', !exists(cacheKey(pid)))
  const afterOff = await P(pid)
  check('公开详情读到最新 status=0（缓存没残留旧状态）', afterOff.json?.data?.status === 0,
    `status=${afterOff.json?.data?.status}`)

  await A('PUT', `/api/mall/admin/products/${pid}/status?status=1`)
  await P(pid) // 预热
  check('重新上架并预热成功', exists(cacheKey(pid)))

  // ============================================================
  section('6. 后台直查 DB / 公开吃缓存（缓存的"旧值"可被证明）')
  const create2 = await A('POST', '/api/mall/admin/products', {
    categoryId: tshirt?.id,
    name: `缓存验收2-${stamp}`,
    status: 1,
    images: [],
    specs: [],
    skus: [{ price: 299, stock: 50, specValues: [] }]
  })
  check('第二个临时商品创建成功', ok(create2) && !!create2.json?.data, msg(create2))
  pid2 = create2.json?.data

  const publicWarm = await P(pid2)
  check('公开详情预热成功（旧值已进缓存）', ok(publicWarm) && exists(cacheKey(pid2)))

  // 绕过服务层直接改库：缓存不会被删（模拟"缓存里的旧值"）
  const rawName = `直改库-${stamp}`
  sql(`update mall_product set name='${rawName}' where id=${pid2}`)
  const adminView = await A('GET', `/api/mall/admin/products/${pid2}`)
  check('后台详情直查 DB（立即看到直改后的名字）', adminView.json?.data?.name === rawName,
    adminView.json?.data?.name)
  const publicView = await P(pid2)
  check('公开详情仍返回缓存里的旧名字（证明缓存真的生效）',
    publicView.json?.data?.name === `缓存验收2-${stamp}`, publicView.json?.data?.name)

  await A('PUT', '/api/mall/admin/products', {
    id: pid2,
    categoryId: tshirt?.id,
    name: `${rawName}-规范保存`,
    status: 1,
    images: [],
    specs: [],
    skus: [{ price: 299, stock: 50, specValues: [] }]
  })
  const publicFresh = await P(pid2)
  check('后台规范保存后缓存被删、公开详情读到最新值',
    publicFresh.json?.data?.name === `${rawName}-规范保存`, publicFresh.json?.data?.name)

  // ============================================================
  section('7. 命名空间隔离（不能碰外卖的 key）')
  check('没有写入外卖的商品缓存前缀 product:detail:{id}', !exists(`product:detail:${pid2}`))
  check('没有写入外卖的锁前缀 cache:lock:...', !exists(`cache:lock:${cacheKey(pid2)}`))
  const lockPrefix = redis(['KEYS', `cache:lock:${PRODUCT_CACHE_PREFIX}*`])
  check('外卖锁前缀下没有任何电商写的锁', lockPrefix === '', `[${lockPrefix}]`)

  // ============================================================
  section('8. 删除商品')
  await A('PUT', `/api/mall/admin/products/${pid}/status?status=0`)
  await P(pid) // 预热，验证删除会删缓存
  const del = await A('DELETE', `/api/mall/admin/products/${pid}`)
  check('删除商品成功', ok(del), msg(del))
  check('删除后缓存被删除', !exists(cacheKey(pid)))
  const afterDel = await P(pid)
  check('删除后公开详情报「商品不存在」', !ok(afterDel) && msg(afterDel).includes('商品不存在'), msg(afterDel))
  check('删除后写入空值缓存（防穿透）', redis(['GET', cacheKey(pid)]) === 'NULL')
  pid = null
} catch (e) {
  console.error('\n💥 脚本异常：', e.message)
  failed++
} finally {
  // ============================================================
  console.log('\n───────── 清理 ─────────')
  for (const id of [pid, pid2].filter(Boolean)) {
    try {
      sql(`update mall_product set status=0 where id=${id}`)
      sql(`delete from mall_sku_spec where sku_id in (select id from mall_sku where product_id=${id})`)
      sql(`delete from mall_sku where product_id=${id}`)
      sql(`delete from mall_spec_value where spec_id in (select id from mall_spec where product_id=${id})`)
      sql(`delete from mall_spec where product_id=${id}`)
      sql(`delete from mall_product_image where product_id=${id}`)
      sql(`delete from mall_product where id=${id}`)
      redis(['DEL', cacheKey(id)])
      console.log(`  已清理临时商品 #${id} 及其缓存`)
    } catch (e) {
      console.log(`  ⚠️ 清理 #${id} 失败：${e.message}`)
    }
  }
  redis(['DEL', cacheKey(999999999)])
  console.log(`\n结果：${results.length - failed}/${results.length} 通过`)
  if (failed > 0) process.exitCode = 1
}
