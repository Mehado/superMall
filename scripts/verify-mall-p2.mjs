// scripts/verify-mall-p2.mjs
// 电商板块 P2（浏览与发现）验收：推荐位 / 分类导航商品数 / 搜索 / 排序 / 筛选 / 收藏 / 足迹。
//
// 重点验证：
//   ① 推荐位：上下线 + 生效时间窗（过期的不返回）
//   ② 分类导航：父节点商品数 = 自己 + 所有后代
//   ③ 搜索：商品名 / 副标题 / 品牌名 / 分类名 四个维度都能命中
//   ④ 排序：price_asc / price_desc / sales_desc / newest 顺序正确；非法 orderBy 回落 default
//   ⑤ 筛选：父分类展开后代、品牌、价格区间（区间交集语义）
//   ⑥ 收藏：幂等、计数、列表含下架商品、未登录 401
//   ⑦ 足迹：去重累加、列表、删单条、清空、未登录 401
//   ⑧ 公开目录强制只看上架（传 status=0 也无效）
//
// 用法（需 8081 电商 + Redis + MySQL 在跑）：
//   node scripts/verify-mall-p2.mjs
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
        // 形如 'redis-cli' / 'mysql' 的裸命令交给 PATH 解析；含路径分隔符的先判断文件存在
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
        // stderr 丢弃：mysql 会往 stderr 打 "Using a password ... insecure" 告警
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

// 用来摆测试数据（例如把 sales 设成不同值以验证销量排序）。
// 这是验收脚本布置 fixture 的手段，业务代码不会这么干。
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

/* ---------------- 登录 ---------------- */

let adminToken = null
let buyerToken = null

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
async function buyerLogin() {
  const cap = await captcha()
  const r = await req('POST', '/api/mall/user/login', {
    body: { username: 'user1@qq.com', password: 'Mall1234', ...cap }
  })
  if (!ok(r)) throw new Error('买家登录失败：' + r.text)
  buyerToken = r.json.data.token
}

const A = (m, p, body) => req(m, p, { headers: { 'mall-admin-token': adminToken }, body })
const U = (m, p, body) => req(m, p, { headers: { token: buyerToken }, body })
const PUB = (m, p) => req(m, p)

/* ---------------- 用例 ---------------- */

const createdBanners = []
let originalSales = []
let originalStatus = []

try {
  await adminLogin()
  await buyerLogin()
  console.log('（管理员 + 买家已登录）')

  // ============================================================
  section('0. 布置 fixture：把 3 个种子商品上架，并给不同的销量')
  originalStatus = sql("select concat(id,':',status) from mall_product order by id").split('\n').filter(Boolean)
  originalSales = sql("select concat(id,':',sales) from mall_product order by id").split('\n').filter(Boolean)
  sql('update mall_product set status = 1 where id in (1,2,3)')
  sql('update mall_product set sales = 500 where id = 1')
  sql('update mall_product set sales = 50  where id = 2')
  sql('update mall_product set sales = 900 where id = 3')
  check('fixture 就绪（3 个商品上架、销量 500/50/900）', true)

  // ============================================================
  section('1. 推荐位：CRUD + 上下线 + 生效时间窗')
  const b1 = await A('POST', '/api/mall/admin/banners', {
    title: 'P2验收-轮播A', image: 'https://example.com/a.png', position: 1, linkType: 1, linkValue: '1', sort: 1
  })
  check('新增推荐位（不传 status 默认上线）', ok(b1), msg(b1))
  if (ok(b1)) createdBanners.push(b1.json.data)

  const b2 = await A('POST', '/api/mall/admin/banners', {
    title: 'P2验收-已过期', image: 'https://example.com/expired.png', position: 1, linkType: 3,
    linkValue: 'https://example.com', sort: 2,
    startTime: '2020-01-01 00:00', endTime: '2020-01-02 00:00'
  })
  check('新增已过期的推荐位（时间窗在 2020 年）', ok(b2), msg(b2))
  if (ok(b2)) createdBanners.push(b2.json.data)

  const b3 = await A('POST', '/api/mall/admin/banners', {
    title: 'P2验收-下线', image: 'https://example.com/offline.png', position: 1, linkType: 1, linkValue: '2', sort: 3, status: 0
  })
  check('新增推荐位并直接置为下线', ok(b3), msg(b3))
  if (ok(b3)) createdBanners.push(b3.json.data)

  const offline = await A('PUT', `/api/mall/admin/banners/${createdBanners[0]}/status?status=0`)
  check('上下线接口可用', ok(offline), msg(offline))

  const badTime = await A('POST', '/api/mall/admin/banners', {
    title: '时间窗倒挂', image: 'https://example.com/x.png',
    startTime: '2026-09-20 00:00', endTime: '2026-09-19 00:00'
  })
  check('失效时间早于生效时间 → 被拒', !ok(badTime) && msg(badTime).includes('失效时间'), msg(badTime))

  const badStatus = await A('PUT', `/api/mall/admin/banners/${createdBanners[0]}/status?status=9`)
  check('非法上下线状态 → 被拒', !ok(badStatus) && msg(badStatus).includes('只能是'), msg(badStatus))

  // 前台：已下线的 A 和已过期的都看不到
  await A('PUT', `/api/mall/admin/banners/${createdBanners[0]}/status?status=1`)
  const pubBanners = await PUB('GET', '/api/mall/catalog/banners?position=1')
  const titles = (pubBanners.json?.data || []).map((b) => b.title)
  check('前台推荐位只返回上线且在生效期内的',
    ok(pubBanners) && titles.includes('P2验收-轮播A')
      && !titles.includes('P2验收-已过期') && !titles.includes('P2验收-下线'),
    `返回：${titles.join(' | ')}`)

  // ============================================================
  section('2. 分类导航：父节点商品数 = 自己 + 所有后代')
  const tree = await PUB('GET', '/api/mall/catalog/categories')
  const findNode = (nodes, name) => {
    for (const n of nodes || []) {
      if (n.name === name) return n
      const hit = findNode(n.children, name)
      if (hit) return hit
    }
    return null
  }
  const roots = tree.json?.data || []
  const tshirt = findNode(roots, 'T恤')
  const menswear = findNode(roots, '男装')
  const clothing = findNode(roots, '服饰鞋包')
  const coffee = findNode(roots, '咖啡')
  check('分类树带 productCount 字段',
    tshirt?.productCount !== undefined && clothing?.productCount !== undefined,
    `T恤=${tshirt?.productCount} 男装=${menswear?.productCount} 服饰鞋包=${clothing?.productCount}`)
  check('叶子分类商品数 = 直属商品数（T恤 1 个）', tshirt?.productCount === 1, `${tshirt?.productCount}`)
  check('父分类商品数向上累加（男装 = T恤 + 衬衫）',
    menswear?.productCount === (tshirt?.productCount || 0) + (findNode(roots, '衬衫')?.productCount || 0),
    `男装=${menswear?.productCount}`)
  check('顶级分类累加所有后代（服饰鞋包 = 男装 + 鞋靴）',
    clothing?.productCount === (menswear?.productCount || 0) + (findNode(roots, '鞋靴')?.productCount || 0),
    `服饰鞋包=${clothing?.productCount}`)
  check('另一棵树（食品饮料 → 咖啡）商品数也对', coffee?.productCount === 1, `咖啡=${coffee?.productCount}`)

  // ============================================================
  section('3. 关键字搜索：商品名 / 副标题 / 品牌名 / 分类名')
  const search = async (kw) => {
    const r = await PUB('GET', `/api/mall/catalog/products/page?keyword=${encodeURIComponent(kw)}&pageSize=100`)
    return (r.json?.data?.records || []).map((p) => p.name)
  }

  const byName = await search('圆领')
  check('按商品名命中（"圆领" → T恤）', byName.includes('纯棉圆领T恤'), byName.join(','))

  const bySubtitle = await search('回弹中底')
  check('按副标题命中（"回弹中底" → 运动鞋）', bySubtitle.includes('轻便运动鞋'), bySubtitle.join(','))

  const byBrand = await search('棉致')
  check('按品牌名命中（"棉致" → T恤）', byBrand.includes('纯棉圆领T恤'), byBrand.join(','))

  const byCategory = await search('运动鞋')
  check('按分类名命中（"运动鞋" → 运动鞋）', byCategory.includes('轻便运动鞋'), byCategory.join(','))

  const noHit = await search('这个词肯定搜不到xyz')
  check('无匹配关键字 → 空列表', noHit.length === 0, `${noHit.length} 条`)

  // ============================================================
  section('4. 排序：价格 / 销量 / 最新 + 非法值回落')
  const listWith = async (qs) => {
    const r = await PUB('GET', `/api/mall/catalog/products/page?pageSize=100&${qs}`)
    return (r.json?.data?.records || []).map((p) => Number(p.priceLow))
  }

  const priceAsc = await listWith('orderBy=price_asc')
  check('price_asc：价格升序（39.9 → 79 → 299）',
    JSON.stringify(priceAsc) === JSON.stringify([39.9, 79, 299]), priceAsc.join(','))

  const priceDesc = await listWith('orderBy=price_desc')
  check('price_desc：价格降序（299 → 79 → 39.9）',
    JSON.stringify(priceDesc) === JSON.stringify([299, 79, 39.9]), priceDesc.join(','))

  const salesDesc = await PUB('GET', '/api/mall/catalog/products/page?pageSize=100&orderBy=sales_desc')
  const salesNames = (salesDesc.json?.data?.records || []).map((p) => `${p.name}:${p.sales}`)
  check('sales_desc：销量降序（咖啡900 → T恤500 → 运动鞋50）',
    salesNames[0]?.endsWith(':900') && salesNames[1]?.endsWith(':500') && salesNames[2]?.endsWith(':50'),
    salesNames.join(' | '))

  const illegal = await PUB('GET', '/api/mall/catalog/products/page?pageSize=100&orderBy=' + encodeURIComponent('id desc; drop table mall_product'))
  const total = illegal.json?.data?.total
  check('非法 orderBy 不报错、回落默认排序（表还活着）',
    ok(illegal) && typeof total === 'number', `total=${total} ` + msg(illegal))
  const stillThere = await PUB('GET', '/api/mall/catalog/products/page?pageSize=1')
  check('非法 orderBy 后商品表未被破坏', ok(stillThere) && stillThere.json?.data?.total === 3,
    `total=${stillThere.json?.data?.total}`)

  // ============================================================
  section('5. 筛选：父分类展开后代 / 品牌 / 价格区间')
  const byTopCat = await PUB('GET', `/api/mall/catalog/products/page?categoryId=${clothing?.id}&pageSize=100`)
  check('按顶级分类筛选能带出后代商品（服饰鞋包 → 2 个）',
    byTopCat.json?.data?.total === 2, `total=${byTopCat.json?.data?.total}`)

  const byLeafCat = await PUB('GET', `/api/mall/catalog/products/page?categoryId=${tshirt?.id}&pageSize=100`)
  check('按叶子分类筛选（T恤 → 1 个）', byLeafCat.json?.data?.total === 1, `total=${byLeafCat.json?.data?.total}`)

  const byBrandFilter = await PUB('GET', '/api/mall/catalog/products/page?brandId=2&pageSize=100')
  const brandNames = (byBrandFilter.json?.data?.records || []).map((p) => p.name)
  check('按品牌筛选（峰行 → 运动鞋）', brandNames.includes('轻便运动鞋'), brandNames.join(','))

  // T恤 79~82、运动鞋 299~309：区间 70~90 应命中 T恤（有交集），不命中运动鞋
  const priceFilter = await PUB('GET', '/api/mall/catalog/products/page?minPrice=70&maxPrice=90&pageSize=100')
  const filtered = (priceFilter.json?.data?.records || []).map((p) => p.name)
  check('价格区间 70~90 只命中 T恤（区间交集语义）',
    filtered.includes('纯棉圆领T恤') && !filtered.includes('轻便运动鞋'), filtered.join(','))

  // 区间 300~310 落在运动鞋的区间内
  const priceFilter2 = await PUB('GET', '/api/mall/catalog/products/page?minPrice=300&maxPrice=310&pageSize=100')
  const filtered2 = (priceFilter2.json?.data?.records || []).map((p) => p.name)
  check('价格区间 300~310 命中运动鞋', filtered2.includes('轻便运动鞋'), filtered2.join(','))

  // 组合筛选：分类 + 品牌 + 价格
  const combo = await PUB('GET', `/api/mall/catalog/products/page?categoryId=${clothing?.id}&brandId=1&minPrice=50&maxPrice=100&pageSize=100`)
  const comboNames = (combo.json?.data?.records || []).map((p) => p.name)
  check('组合筛选（分类 + 品牌 + 价格区间）', comboNames.length === 1 && comboNames[0] === '纯棉圆领T恤',
    comboNames.join(','))

  // ============================================================
  section('6. 公开目录强制只看上架')
  await A('PUT', '/api/mall/admin/products/2/status?status=0')
  const forced = await PUB('GET', '/api/mall/catalog/products/page?pageSize=100')
  const forcedNames = (forced.json?.data?.records || []).map((p) => p.name)
  check('下架后公开列表不再包含它', !forcedNames.includes('轻便运动鞋'), forcedNames.join(','))

  const tryStatus = await PUB('GET', '/api/mall/catalog/products/page?status=0&pageSize=100')
  const tryNames = (tryStatus.json?.data?.records || []).map((p) => p.name)
  check('前台传 status=0 也无效（服务端强制覆盖成 1）',
    !tryNames.includes('轻便运动鞋'), tryNames.join(','))
  await A('PUT', '/api/mall/admin/products/2/status?status=1')

  section('7. 分页上限')
  const bigPage = await PUB('GET', '/api/mall/catalog/products/page?pageSize=9999')
  check('pageSize 上限 100（不报错）', ok(bigPage), `total=${bigPage.json?.data?.total}`)

  // ============================================================
  section('8. 收藏夹')
  const noLogin = await PUB('POST', '/api/mall/user/favorites/1')
  check('未登录收藏 → 401', noLogin.status === 401, `HTTP ${noLogin.status}`)

  const fav1 = await U('POST', '/api/mall/user/favorites/1')
  check('收藏商品成功', ok(fav1), msg(fav1))

  const favAgain = await U('POST', '/api/mall/user/favorites/1')
  check('重复收藏幂等（不报错）', ok(favAgain), msg(favAgain))

  const favExists = await U('GET', '/api/mall/user/favorites/1/exists')
  check('查询已收藏', favExists.json?.data?.favorited === true)
  const notFav = await U('GET', '/api/mall/user/favorites/2/exists')
  check('未收藏的商品返回 false', notFav.json?.data?.favorited === false)

  await U('POST', '/api/mall/user/favorites/2')
  const favCount = await U('GET', '/api/mall/user/favorites/count')
  check('收藏计数 = 2', favCount.json?.data?.total === 2, `${favCount.json?.data?.total}`)

  const favList = await U('GET', '/api/mall/user/favorites?page=1&pageSize=10')
  const favIds = (favList.json?.data?.records || []).map((p) => p.id)
  check('收藏列表返回商品卡片', ok(favList) && favIds.length === 2, favIds.join(','))

  const favUnknown = await U('POST', '/api/mall/user/favorites/999999')
  check('收藏不存在的商品 → 被拒', !ok(favUnknown) && msg(favUnknown).includes('商品不存在'), msg(favUnknown))

  // 下架商品仍要出现在收藏列表里（前端标灰）
  await A('PUT', '/api/mall/admin/products/1/status?status=0')
  const favList2 = await U('GET', '/api/mall/user/favorites?page=1&pageSize=10')
  const offSaleInFav = (favList2.json?.data?.records || []).find((p) => p.id === 1)
  check('收藏列表包含已下架商品（status=0，需前端标灰）',
    !!offSaleInFav && offSaleInFav.status === 0, `status=${offSaleInFav?.status}`)
  await A('PUT', '/api/mall/admin/products/1/status?status=1')

  const unfav = await U('DELETE', '/api/mall/user/favorites/1')
  check('取消收藏成功', ok(unfav), msg(unfav))
  const unfavAgain = await U('DELETE', '/api/mall/user/favorites/1')
  check('重复取消幂等（不报错）', ok(unfavAgain), msg(unfavAgain))
  const favCount2 = await U('GET', '/api/mall/user/favorites/count')
  check('取消后计数 = 1', favCount2.json?.data?.total === 1, `${favCount2.json?.data?.total}`)
  await U('DELETE', '/api/mall/user/favorites/2')

  // ============================================================
  section('9. 浏览足迹')
  const fpNoLogin = await PUB('POST', '/api/mall/user/footprints/1')
  check('未登录记足迹 → 401', fpNoLogin.status === 401, `HTTP ${fpNoLogin.status}`)

  await U('POST', '/api/mall/user/footprints/1')
  await U('POST', '/api/mall/user/footprints/1')
  await U('POST', '/api/mall/user/footprints/1')
  check('连续浏览同一商品 3 次', true)

  const fpList = await U('GET', '/api/mall/user/footprints?page=1&pageSize=10')
  const rows = fpList.json?.data?.records || []
  const row1 = rows.find((r) => r.productId === 1)
  check('足迹去重：只留 1 行', rows.filter((r) => r.productId === 1).length === 1, `${rows.length} 行`)
  check('浏览次数累加到 3', row1?.visitCount === 3, `visitCount=${row1?.visitCount}`)
  check('足迹带商品卡片', !!row1?.product?.name, row1?.product?.name)
  check('足迹带最近浏览时间', !!row1?.lastVisitTime, row1?.lastVisitTime)

  await U('POST', '/api/mall/user/footprints/2')
  const fpList2 = await U('GET', '/api/mall/user/footprints?page=1&pageSize=10')
  const rows2 = fpList2.json?.data?.records || []
  check('最新浏览排在最前（按最近浏览倒序）', rows2[0]?.productId === 2, `首条=${rows2[0]?.productId}`)

  const fpUnk = await U('POST', '/api/mall/user/footprints/999999')
  check('记录不存在商品的足迹 → 被拒', !ok(fpUnk) && msg(fpUnk).includes('商品不存在'), msg(fpUnk))

  const delOne = await U('DELETE', '/api/mall/user/footprints/2')
  check('删除单条足迹', ok(delOne), msg(delOne))
  const afterDel = await U('GET', '/api/mall/user/footprints?page=1&pageSize=10')
  check('删除后只剩 1 条', (afterDel.json?.data?.records || []).length === 1,
    `${(afterDel.json?.data?.records || []).length}`)

  const clear = await U('DELETE', '/api/mall/user/footprints')
  check('清空足迹', ok(clear), msg(clear))
  const afterClear = await U('GET', '/api/mall/user/footprints?page=1&pageSize=10')
  check('清空后列表为空', (afterClear.json?.data?.records || []).length === 0)

  // ============================================================
  section('10. 推荐商品')
  const hot = await PUB('GET', '/api/mall/catalog/recommend?type=hot&limit=3')
  const hotNames = (hot.json?.data || []).map((p) => `${p.name}:${p.sales}`)
  check('推荐 hot 按销量倒序', hotNames[0]?.endsWith(':900'), hotNames.join(' | '))
  const newOnes = await PUB('GET', '/api/mall/catalog/recommend?type=new&limit=2')
  check('推荐 new 返回 2 条', (newOnes.json?.data || []).length === 2, `${(newOnes.json?.data || []).length} 条`)
  const defaultOnes = await PUB('GET', '/api/mall/catalog/recommend?limit=999')
  check('推荐 limit 上限 50 / 默认 hot（不报错）', ok(defaultOnes), `返回 ${(defaultOnes.json?.data || []).length} 条`)

  // ============================================================
  section('11. 清理：删除本次验收造的推荐位、恢复商品数据')
  for (const id of createdBanners) {
    await A('DELETE', `/api/mall/admin/banners/${id}`)
  }
  const afterDelBanners = await A('GET', '/api/mall/admin/banners?position=1')
  const remainTitles = (afterDelBanners.json?.data || []).map((b) => b.title)
  check('验收造的推荐位已删除', !remainTitles.some((t) => t.startsWith('P2验收-')), remainTitles.join(' | '))
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  // 恢复商品状态与销量
  try {
    if (originalStatus?.length) {
      for (const pair of originalStatus) {
        const [id, status] = pair.split(':')
        sql(`update mall_product set status = ${status} where id = ${id}`)
      }
    }
    if (originalSales?.length) {
      for (const pair of originalSales) {
        const [id, sales] = pair.split(':')
        sql(`update mall_product set sales = ${sales} where id = ${id}`)
      }
    }
    console.log('\n（已恢复商品 status / sales 到验收前的值）')
  } catch (e) {
    console.log('（恢复数据失败，请手工检查 mall_product：', e.message, '）')
  }

  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
