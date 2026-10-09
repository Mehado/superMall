// scripts/cleanup-oss-test-objects.mjs
// 一次性清理：删掉 OSS 上「验收脚本传上去、但数据库里没有任何引用」的测试对象。
//
// 为什么会有这些：审「文件上传白名单」时真的往 OSS 传过东西 ——
//   · 带 onload=alert(1) 的 evil.svg（用来证明当时没有类型白名单）
//   · 3MB / 6MB 的假 PNG（用来试 multipart 上限）
// 白名单修好后，这些对象留着没有任何用处。
//
// 安全策略（**默认 dry-run，必须显式 --apply 才删**）：
//   ① 只动 mall 的目录（默认 supermall/mall/；外卖商品图在 supermall/products/，不碰）；
//   ② 只删「数据库里没有任何引用」的 key（mall_product.main_image、mall_product_image.url、
//      mall_banner.image、mall_category.icon、mall_sku.image 都算引用）；
//   ③ 再加一层：只删 `.svg` 或「≥2MB」的对象（正常商品图是 jpg/png 且几百 KB）。
//
// 用法：
//   node scripts/cleanup-oss-test-objects.mjs                    # dry-run
//   node scripts/cleanup-oss-test-objects.mjs --apply            # 真删
//   node scripts/cleanup-oss-test-objects.mjs --prefix supermall/seed/   # 换目录
//
// OSS 读写与签名在 scripts/lib/oss.mjs（与 migrate-seed-images-to-oss.mjs 共用）。
import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { ossConfig, listObjects, deleteObject } from './lib/oss.mjs'

const ROOT = process.cwd()
const APPLY = process.argv.includes('--apply')
const argv = process.argv.slice(2)
const prefixIdx = argv.indexOf('--prefix')
const PREFIX = prefixIdx > -1 && argv[prefixIdx + 1] ? argv[prefixIdx + 1] : 'supermall/mall/'

function mysql(statement) {
  return execFileSync('D:/mysql/mysql-9.5.0-winx64/bin/mysql.exe', [
    '--ssl-mode=DISABLED', '--default-character-set=utf8mb4',
    '-h', '127.0.0.1', '-uroot', '-pMing1234', '-D', 'supermall', '-N', '-B', '-e', statement
  ], { encoding: 'utf8' })
}

/** 数据库里被引用的对象 key（URL 去掉 host 之后的部分） */
function referencedKeys() {
  const urlCols = [
    ['mall_product', 'main_image'], ['mall_product_image', 'url'], ['mall_banner', 'image'],
    ['mall_category', 'icon'], ['mall_sku', 'image']
  ]
  const urls = []
  for (const [t, c] of urlCols) {
    const rows = mysql(`select ${c} from ${t} where ${c} is not null and ${c} <> ''`).trim()
    if (rows) urls.push(...rows.split('\n'))
  }
  return new Set(urls.map((u) =>
    decodeURIComponent(String(u).trim().split('://').pop().split('/').slice(1).join('/'))))
}

const cfg = ossConfig()
console.log(`OSS：${cfg.bucket}.${cfg.endpoint}  目录：${PREFIX}  ${APPLY ? '【真删】' : '【dry-run，加 --apply 才删】'}`)

const referenced = referencedKeys()
console.log(`数据库引用的对象：${referenced.size} 个`)

const all = await listObjects(cfg, PREFIX)
console.log(`列出 ${PREFIX} 下 ${all.length} 个对象\n`)

const candidates = all.filter((o) =>
  !referenced.has(o.key) && (o.key.toLowerCase().endsWith('.svg') || o.size >= 2 * 1024 * 1024))

if (!candidates.length) {
  console.log('✅ 没有可清理的对象（数据库引用的都保留，其余不符合「测试残留」特征）')
} else {
  console.log('待清理：')
  for (const o of candidates) {
    console.log(`  ${String((o.size / 1024).toFixed(0)).padStart(7)}KB  ${o.lastModified}  ${o.key}`)
  }
  if (!APPLY) {
    console.log(`\n（dry-run）确认无误后执行：node scripts/cleanup-oss-test-objects.mjs --apply${prefixIdx > -1 ? ' --prefix ' + PREFIX : ''}`)
  } else {
    console.log('')
    for (const o of candidates) {
      const r = await deleteObject(cfg, o.key)
      console.log(`  ${r.ok ? '🗑  已删除' : '❌ 删除失败 ' + r.status + ' ' + r.body}  ${o.key}`)
    }
  }
}
void path
