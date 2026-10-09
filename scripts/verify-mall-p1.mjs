// scripts/verify-mall-p1.mjs
// 电商板块 P1（商品体系）验收：多级分类 / 品牌 / SPU-SKU 规格矩阵 / 多图 / 上下架。
//
// 重点验证：
//   ① 多级分类（3 级）+ 分类树 + 父分类筛选能带出后代商品 + 删除保护
//   ② 品牌 CRUD + 删除保护
//   ③ SPU-SKU 规格矩阵（2×2=4）完整落库、价格区间与总库存派生正确
//   ④ 矩阵不完整 / 规格值非法 / 重复组合 → 被拒
//   ⑤ 无规格商品 = 1 个裸 SKU，不建规格关联
//   ⑥ 多图上传（真传 OSS）+ 上下架 + 上架中不可删
//
// 用法（需 8081 电商 + 8080 外卖 + Redis 在跑）：
//   node scripts/verify-mall-p1.mjs
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

/* ---------------- Redis（取验证码答案） ---------------- */

const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1'
const REDIS_PORT = process.env.REDIS_PORT || '6379'
const REDIS_PASSWORD = process.env.REDIS_PASSWORD || 'mehado'
const CANDIDATES = [
  process.env.REDIS_CLI,
  'redis-cli',
  'D:/redis/Redis-x64-5.0.14.1/redis-cli',
  'C:/Program Files/Redis/redis-cli.exe'
].filter(Boolean)

let cliPath = null
function redis(args) {
  if (!cliPath) {
    for (const p of CANDIDATES) {
      if (p !== 'redis-cli' && !fs.existsSync(p)) continue
      try {
        execFileSync(p, ['--version'], { stdio: 'ignore' })
        cliPath = p
        break
      } catch {
        /* 下一个 */
      }
    }
    if (!cliPath) throw new Error('找不到 redis-cli，可用 REDIS_CLI 指定路径')
  }
  const base = ['-h', REDIS_HOST, '-p', REDIS_PORT, '-a', REDIS_PASSWORD]
  for (const a of [[...base, '--no-auth-warning', ...args], [...base, ...args]]) {
    try {
      return execFileSync(cliPath, a, { encoding: 'utf8' }).trim()
    } catch {
      /* 重试 */
    }
  }
  throw new Error('redis-cli 执行失败')
}

/* ---------------- 登录拿管理员 token ---------------- */

let adminToken = null

async function adminLogin() {
  const cap = await req('GET', '/api/mall/auth/captcha')
  const key = cap.json?.data?.key
  const code = redis(['GET', `mall:captcha:${key}`])
  const login = await req('POST', '/api/mall/admin/login', {
    body: { username: 'admin@qq.com', password: 'Admin1234', captchaKey: key, captchaCode: code }
  })
  if (!ok(login)) throw new Error('管理员登录失败：' + login.text)
  adminToken = login.json.data.token
}
const A = (method, path, body) =>
  req(method, path, { headers: { 'mall-admin-token': adminToken }, body })

/* ---------------- 用例 ---------------- */

try {
  await adminLogin()
  console.log('（管理员已登录，后续请求带 mall-admin-token）')

  // ============================================================
  section('1. 多级分类：3 级树 + 层级限制 + 同级重名')
  const tree = await A('GET', '/api/mall/admin/categories/tree')
  const roots = tree.json?.data || []
  check('分类树接口可用', ok(tree) && Array.isArray(roots), `根节点 ${roots.length} 个`)

  const findNode = (nodes, name) => {
    for (const n of nodes || []) {
      if (n.name === name) return n
      const hit = findNode(n.children, name)
      if (hit) return hit
    }
    return null
  }
  const clothing = findNode(roots, '服饰鞋包')
  const menswear = findNode(roots, '男装')
  const tshirt = findNode(roots, 'T恤')
  check('3 级结构正确（服饰鞋包 → 男装 → T恤）',
    clothing?.level === 1 && menswear?.level === 2 && tshirt?.level === 3,
    `level: ${clothing?.level}/${menswear?.level}/${tshirt?.level}`)
  check('叶子节点 children 为空数组', Array.isArray(tshirt?.children) && tshirt.children.length === 0)

  const dupCat = await A('POST', '/api/mall/admin/categories', { parentId: menswear?.id, name: '男装'.replace('男装', 'T恤') })
  check('同级重名分类 → 被拒', !ok(dupCat) && msg(dupCat).includes('已存在'), msg(dupCat))
  if (ok(dupCat)) await A('DELETE', `/api/mall/admin/categories/${dupCat.json.data}`)

  const tooDeep = await A('POST', '/api/mall/admin/categories', { parentId: tshirt?.id, name: '超四级' })
  check('第 4 级分类 → 被拒（最多 3 级）', !ok(tooDeep) && msg(tooDeep).includes('最多 3 级'), msg(tooDeep))

  const delWithChild = await A('DELETE', `/api/mall/admin/categories/${clothing?.id}`)
  check('删除还有子分类的分类 → 被拒', !ok(delWithChild) && msg(delWithChild).includes('子分类'), msg(delWithChild))

  const delWithProduct = await A('DELETE', `/api/mall/admin/categories/${tshirt?.id}`)
  check('删除还有商品的分类 → 被拒', !ok(delWithProduct) && msg(delWithProduct).includes('商品'), msg(delWithProduct))

  // ============================================================
  section('2. 品牌：CRUD + 同名 + 引用保护')
  const brands = await A('GET', '/api/mall/admin/brands')
  check('品牌列表可用', ok(brands) && (brands.json?.data || []).length >= 5, `${brands.json?.data?.length} 个`)

  const newBrand = await A('POST', '/api/mall/admin/brands', { name: `验证品牌${Date.now().toString(36)}`, description: 'P1 验收' })
  check('新增品牌成功', ok(newBrand) && !!newBrand.json?.data, msg(newBrand))
  const brandId = newBrand.json?.data

  const dupBrand = await A('POST', '/api/mall/admin/brands', { name: '棉致' })
  check('品牌同名 → 被拒', !ok(dupBrand) && msg(dupBrand).includes('已存在'), msg(dupBrand))

  const updBrand = await A('PUT', '/api/mall/admin/brands', { id: brandId, name: `验证品牌改${Date.now().toString(36)}` })
  check('修改品牌成功', ok(updBrand), msg(updBrand))

  // ============================================================
  section('3. 商品保存：规格矩阵 2×2=4 完整落库')
  const createProduct = await A('POST', '/api/mall/admin/products', {
    categoryId: tshirt?.id,
    brandId,
    name: 'P1验收-T恤',
    subtitle: '2 色 2 码',
    images: [],
    specs: [
      { name: '颜色', values: ['白', '黑'] },
      { name: '尺码', values: ['M', 'L'] }
    ],
    skus: [
      { skuCode: 'V-W-M', price: 79, stock: 10, specValues: ['白', 'M'] },
      { skuCode: 'V-W-L', price: 79, stock: 20, specValues: ['白', 'L'] },
      { skuCode: 'V-B-M', price: 89, stock: 30, specValues: ['黑', 'M'] },
      { skuCode: 'V-B-L', price: 89, stock: 40, specValues: ['黑', 'L'] }
    ]
  })
  check('新增商品成功', ok(createProduct) && !!createProduct.json?.data, msg(createProduct))
  const pid = createProduct.json?.data

  const detail = await A('GET', `/api/mall/admin/products/${pid}`)
  const p = detail.json?.data
  check('详情返回规格项 2 个', p?.specs?.length === 2, `specs=${JSON.stringify(p?.specs?.map((s) => s.name))}`)
  check('规格值按顺序返回', p?.specs?.[0]?.values?.join(',') === '白,黑' && p?.specs?.[1]?.values?.join(',') === 'M,L',
    `颜色=[${p?.specs?.[0]?.values}] 尺码=[${p?.specs?.[1]?.values}]`)
  check('详情返回 4 个 SKU', p?.skus?.length === 4, `${p?.skus?.length} 个`)
  check('SKU 的 specValues 与规格项顺序对应',
    p?.skus?.some((s) => s.specValues?.[0] === '黑' && s.specValues?.[1] === 'L'),
    JSON.stringify(p?.skus?.map((s) => s.specValues)))
  check('价格区间派生正确（79~89）',
    Number(p?.priceLow) === 79 && Number(p?.priceHigh) === 89, `${p?.priceLow}~${p?.priceHigh}`)
  check('总库存派生正确（10+20+30+40=100）', p?.stock === 100, `stock=${p?.stock}`)
  check('新建默认下架', p?.status === 0, `status=${p?.status}`)
  check('分类名/品牌名已回填', !!p?.categoryName && !!p?.brandName, `${p?.categoryName} / ${p?.brandName}`)

  // ============================================================
  section('4. 矩阵校验：不完整 / 非法值 / 重复组合')
  const base = {
    categoryId: tshirt?.id,
    name: 'P1验收-非法',
    specs: [
      { name: '颜色', values: ['白', '黑'] },
      { name: '尺码', values: ['M', 'L'] }
    ]
  }
  const missing = await A('POST', '/api/mall/admin/products', {
    ...base,
    skus: [
      { price: 79, stock: 1, specValues: ['白', 'M'] },
      { price: 79, stock: 1, specValues: ['白', 'L'] },
      { price: 89, stock: 1, specValues: ['黑', 'M'] }
    ]
  })
  check('SKU 少一个组合 → 被拒（应等于笛卡尔积）',
    !ok(missing) && msg(missing).includes('应为 4 个，实际 3 个'), msg(missing))

  const unknownValue = await A('POST', '/api/mall/admin/products', {
    categoryId: tshirt?.id,
    name: 'P1验收-非法值',
    specs: [{ name: '颜色', values: ['白', '黑'] }],
    skus: [
      { price: 79, stock: 1, specValues: ['白'] },
      { price: 89, stock: 1, specValues: ['红'] }
    ]
  })
  check('规格值不在定义里 → 被拒', !ok(unknownValue) && msg(unknownValue).includes('规格里'), msg(unknownValue))

  const dupCombo = await A('POST', '/api/mall/admin/products', {
    categoryId: tshirt?.id,
    name: 'P1验收-重复组合',
    specs: [{ name: '颜色', values: ['白', '黑'] }],
    skus: [
      { price: 79, stock: 1, specValues: ['白'] },
      { price: 80, stock: 1, specValues: ['白'] }
    ]
  })
  check('规格组合重复 → 被拒', !ok(dupCombo) && msg(dupCombo).includes('重复'), msg(dupCombo))

  const noSku = await A('POST', '/api/mall/admin/products', { categoryId: tshirt?.id, name: 'P1验收-无SKU', specs: [], skus: [] })
  check('一个 SKU 都没有 → 被拒', !ok(noSku) && msg(noSku).includes('SKU'), msg(noSku))

  const nonLeaf = await A('POST', '/api/mall/admin/products', {
    categoryId: menswear?.id,
    name: 'P1验收-挂父分类',
    specs: [],
    skus: [{ price: 1, stock: 1, specValues: [] }]
  })
  check('商品挂在非叶子分类 → 被拒', !ok(nonLeaf) && msg(nonLeaf).includes('最后一级'), msg(nonLeaf))

  // ============================================================
  section('5. 无规格商品 = 1 个裸 SKU')
  const noSpec = await A('POST', '/api/mall/admin/products', {
    categoryId: findNode(roots, '咖啡')?.id,
    name: 'P1验收-无规格咖啡',
    specs: [],
    skus: [{ price: 39.9, stock: 500, specValues: [] }]
  })
  check('无规格商品可保存', ok(noSpec), msg(noSpec))
  const noSpecId = noSpec.json?.data
  const noSpecDetail = await A('GET', `/api/mall/admin/products/${noSpecId}`)
  const np = noSpecDetail.json?.data
  check('无规格商品 specs 为空数组', Array.isArray(np?.specs) && np.specs.length === 0)
  check('无规格商品恰好 1 个 SKU 且 specValues 为空',
    np?.skus?.length === 1 && Array.isArray(np.skus[0].specValues) && np.skus[0].specValues.length === 0,
    JSON.stringify(np?.skus?.[0]?.specValues))
  check('无规格商品价格区间相等（39.9~39.9）',
    Number(np?.priceLow) === 39.9 && Number(np?.priceHigh) === 39.9, `${np?.priceLow}~${np?.priceHigh}`)

  // ============================================================
  section('6. 分类筛选：传父分类要能带出后代商品')
  const byLeaf = await A('GET', `/api/mall/admin/products/page?categoryId=${tshirt?.id}&pageSize=100`)
  check('按叶子分类筛选能查到刚建的商品',
    ok(byLeaf) && (byLeaf.json?.data?.records || []).some((r) => r.id === pid),
    `共 ${byLeaf.json?.data?.total} 条`)

  const byTop = await A('GET', `/api/mall/admin/products/page?categoryId=${clothing?.id}&pageSize=100`)
  check('按顶级分类「服饰鞋包」筛选也能查到（后代展开生效）',
    ok(byTop) && (byTop.json?.data?.records || []).some((r) => r.id === pid),
    `共 ${byTop.json?.data?.total} 条`)

  const byBrand = await A('GET', `/api/mall/admin/products/page?brandId=${brandId}&pageSize=100`)
  check('按品牌筛选可用', ok(byBrand) && (byBrand.json?.data?.records || []).length >= 1,
    `共 ${byBrand.json?.data?.total} 条`)

  // ============================================================
  section('7. 多图上传（真传 OSS）')
  const png1x1 = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8AARAAB/wD/AH8B9wAAAABJRU5ErkJggg==',
    'base64'
  )
  const form = new FormData()
  form.append('files', new Blob([png1x1], { type: 'image/png' }), 'p1-verify-a.png')
  form.append('files', new Blob([png1x1], { type: 'image/png' }), 'p1-verify-b.png')
  let uploadRes
  try {
    uploadRes = await fetch(MALL + '/api/mall/admin/file/upload-batch', {
      method: 'POST',
      headers: { 'mall-admin-token': adminToken },
      body: form
    })
    uploadRes = { status: uploadRes.status, json: await uploadRes.json() }
  } catch (e) {
    uploadRes = { status: 0, json: null, err: String(e.message || e) }
  }
  const uploaded = uploadRes.json?.data
  check('批量上传 2 张图片到 OSS', ok(uploadRes) && Array.isArray(uploaded) && uploaded.length === 2,
    uploadRes.err || msg(uploadRes) || JSON.stringify(uploaded))
  if (Array.isArray(uploaded) && uploaded.length === 2) {
    check('返回的是 OSS 公网 URL', uploaded.every((u) => /^https?:\/\//.test(u)), uploaded[0])
    const head = await fetch(uploaded[0], { method: 'HEAD' }).catch(() => null)
    check('上传后的 URL 可访问（HEAD 2xx）', head && head.ok, head ? `HTTP ${head.status}` : '请求失败')

    // 把图片挂到刚建的商品上，验证多图落库
    const withImages = await A('PUT', '/api/mall/admin/products', {
      id: pid,
      categoryId: tshirt?.id,
      brandId,
      name: 'P1验收-T恤',
      subtitle: '2 色 2 码（带图）',
      images: uploaded,
      specs: [
        { name: '颜色', values: ['白', '黑'] },
        { name: '尺码', values: ['M', 'L'] }
      ],
      skus: [
        { skuCode: 'V-W-M', price: 79, stock: 10, specValues: ['白', 'M'] },
        { skuCode: 'V-W-L', price: 79, stock: 20, specValues: ['白', 'L'] },
        { skuCode: 'V-B-M', price: 89, stock: 30, specValues: ['黑', 'M'] },
        { skuCode: 'V-B-L', price: 89, stock: 40, specValues: ['黑', 'L'] }
      ]
    })
    check('把图片挂到商品上（修改商品）成功', ok(withImages), msg(withImages))
    const withImgDetail = await A('GET', `/api/mall/admin/products/${pid}`)
    check('商品详情返回 2 张图', withImgDetail.json?.data?.images?.length === 2,
      `${withImgDetail.json?.data?.images?.length} 张`)
    check('未传 mainImage 时自动取第一张',
      withImgDetail.json?.data?.mainImage === uploaded[0], withImgDetail.json?.data?.mainImage)
    check('改图后规格矩阵仍然完整（4 个 SKU）', withImgDetail.json?.data?.skus?.length === 4)
  }

  // ============================================================
  section('8. 上下架 + 删除保护')
  const offSale = await A('PUT', `/api/mall/admin/products/${pid}/status?status=0`)
  check('下架成功', ok(offSale), msg(offSale))

  const onSale = await A('PUT', `/api/mall/admin/products/${pid}/status?status=1`)
  check('上架成功', ok(onSale), msg(onSale))
  check('上架后详情 status=1', (await A('GET', `/api/mall/admin/products/${pid}`)).json?.data?.status === 1)

  const badStatus = await A('PUT', `/api/mall/admin/products/${pid}/status?status=9`)
  check('非法状态值 → 被拒', !ok(badStatus) && msg(badStatus).includes('只能'), msg(badStatus))

  const delOnSale = await A('DELETE', `/api/mall/admin/products/${pid}`)
  check('上架中的商品不能删', !ok(delOnSale) && msg(delOnSale).includes('下架'), msg(delOnSale))

  const delBrandInUse = await A('DELETE', `/api/mall/admin/brands/${brandId}`)
  check('品牌下还有商品 → 不能删', !ok(delBrandInUse) && msg(delBrandInUse).includes('商品'), msg(delBrandInUse))

  // ============================================================
  section('9. 公开目录（无需登录）')
  const pubTree = await req('GET', '/api/mall/catalog/categories')
  check('公开分类树可用（无需登录）', ok(pubTree), `HTTP ${pubTree.status}`)
  const pubBrands = await req('GET', '/api/mall/catalog/brands')
  check('公开品牌列表可用', ok(pubBrands) && (pubBrands.json?.data || []).length >= 1)
  const pubPage = await req('GET', '/api/mall/catalog/products/page?pageSize=100')
  const pubRecords = pubPage.json?.data?.records || []
  check('公开商品分页只返回上架商品', ok(pubPage) && pubRecords.every((r) => r.status === 1),
    `${pubRecords.length} 条在售`)
  check('刚上架的商品出现在公开列表里', pubRecords.some((r) => r.id === pid))

  const pubDetail = await req('GET', `/api/mall/catalog/products/${pid}`)
  check('公开商品详情含规格与 SKU', ok(pubDetail) && pubDetail.json?.data?.skus?.length === 4)

  const wrongHeader = await req('GET', '/api/mall/admin/products/page', { headers: { token: adminToken } })
  check('管理员 token 用买家头 token 传 → 401（头名隔离）', wrongHeader.status === 401, `HTTP ${wrongHeader.status}`)

  // ============================================================
  section('10. 清理：删除本次验收造的数据')
  await A('PUT', `/api/mall/admin/products/${pid}/status?status=0`)
  const delP = await A('DELETE', `/api/mall/admin/products/${pid}`)
  check('下架后可删除商品', ok(delP), msg(delP))
  const delNp = await A('DELETE', `/api/mall/admin/products/${noSpecId}`)
  check('删除无规格商品', ok(delNp), msg(delNp))
  const delB = await A('DELETE', `/api/mall/admin/brands/${brandId}`)
  check('商品删完后品牌可删', ok(delB), msg(delB))
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
