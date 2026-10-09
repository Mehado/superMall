// scripts/migrate-seed-images-to-oss.mjs
// 把演示图搬到 OSS，并把「数据库 + 种子 SQL」里的图片地址改成 OSS 绝对 URL。
//
// 为什么需要：`frontend/public/seed/` 那些演示图（商品 / 分类 / banner / 金刚区）
// 是第三方素材，**没有进 git 仓库**。于是「换机器 / 从仓库 clone / 上云」时就全是灰占位。
// 项目要迁移、要部署，图必须跟着数据走 —— 最省事的形态就是：**图放 OSS，DB 存绝对 URL**。
// 这样仓库、镜像、任何环境都不需要图片文件，本地 / Docker / 云端看到的是同一批图。
//
// 做四件事：
//   ① 把 frontend/public/seed/ 下的图上传到 OSS（默认 supermall/seed/，已存在且大小一致就跳过）
//   ② 把 4 张表里的 '/seed/xxx' 批量换成 'https://<bucket>.<endpoint>/supermall/seed/xxx'
//   ③ 同步重写 sql/seed-mall-demo.sql（让新环境导入的**直接是 OSS URL**）
//   ④ 抽样 HEAD 校验公网可访问（200 + image/*）
//
// 用法：
//   node scripts/migrate-seed-images-to-oss.mjs                 # dry-run：只列要做什么
//   node scripts/migrate-seed-images-to-oss.mjs --apply         # 真上传 + 真改库 + 改 SQL
//   node scripts/migrate-seed-images-to-oss.mjs --apply --prefix supermall/seed/   # 换 OSS 目录
//   ... --skip-upload（只改地址）/ --skip-db / --skip-sql
//
// 幂等：重复跑 = 图片跳过、地址 replace 找不到 '/seed/' 就不动。
// 依赖：mvn（解 OSS 密钥）+ mysql + 网络。
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { ossConfig, listObjects, putObject, headObject, contentTypeOf } from './lib/oss.mjs'

const ROOT = process.cwd()
const APPLY = process.argv.includes('--apply')
const argOf = (name, def) => {
  const i = process.argv.indexOf(`--${name}`)
  return i > -1 && process.argv[i + 1] && !process.argv[i + 1].startsWith('--') ? process.argv[i + 1] : def
}
const PREFIX = argOf('prefix', 'supermall/seed/').replace(/^\/+/, '')
const SKIP_UPLOAD = process.argv.includes('--skip-upload')
const SKIP_DB = process.argv.includes('--skip-db')
const SKIP_SQL = process.argv.includes('--skip-sql')

const LOCAL_DIR = path.join(ROOT, 'frontend/public/seed')
const LOCAL_URL_PREFIX = '/seed/'          // DB / SQL 里现在用的相对前缀
const SQL_FILE = path.join(ROOT, 'sql/seed-mall-demo.sql')

const mysql = (statement) => execFileSync('D:/mysql/mysql-9.5.0-winx64/bin/mysql.exe', [
  '--ssl-mode=DISABLED', '--default-character-set=utf8mb4',
  '-h', '127.0.0.1', '-uroot', '-pMing1234', '-D', 'supermall', '-N', '-B', '-e', statement
], { encoding: 'utf8' }).trim()

/* ---------------- 0. 配置 & 现状 ---------------- */
const cfg = ossConfig()
const OSS_URL_PREFIX = `${cfg.base}/${PREFIX}`
console.log(`OSS       : ${cfg.bucket}.${cfg.endpoint}  →  前缀 ${PREFIX}`)
console.log(`目标 URL  : ${OSS_URL_PREFIX}xxx.jpg`)
console.log(`本地图目录: ${path.relative(ROOT, LOCAL_DIR)}`)
console.log(`模式      : ${APPLY ? '【真执行】' : '【dry-run，加 --apply 才动手】'}\n`)

const files = fs.existsSync(LOCAL_DIR)
  ? fs.readdirSync(LOCAL_DIR).filter((f) => /\.(jpe?g|png|gif|webp|svg|bmp)$/i.test(f)).sort()
  : []
if (!files.length) {
  console.error(`❌ ${path.relative(ROOT, LOCAL_DIR)} 里没有图片 —— 先跑：node scripts/seed-mall-demo.mjs --apply`)
  process.exit(1)
}

const TABLES = [
  ['mall_product', 'main_image'],
  ['mall_product_image', 'url'],
  ['mall_category', 'icon'],
  ['mall_banner', 'image'],
  ['mall_sku', 'image']
]
const counts = TABLES.map(([t, c]) => {
  const n = Number(mysql(`select count(*) from ${t} where ${c} like '${LOCAL_URL_PREFIX}%'`) || 0)
  return { table: t, column: c, rows: n }
})
const totalRows = counts.reduce((s, x) => s + x.rows, 0)
console.log('库里的相对地址（待改写）：')
for (const c of counts) console.log(`  ${String(c.rows).padStart(4)} 行  ${c.table}.${c.column}`)
console.log(`  合计 ${totalRows} 行`)

const sqlHits = fs.existsSync(SQL_FILE)
  ? (fs.readFileSync(SQL_FILE, 'utf8').match(new RegExp(`'${LOCAL_URL_PREFIX}`, 'g')) || []).length
  : 0
console.log(`种子 SQL 里的相对地址：${sqlHits} 处（${path.relative(ROOT, SQL_FILE)}）\n`)

/* ---------------- 1. 上传 ---------------- */
let uploaded = 0
let skipped = 0
if (SKIP_UPLOAD) {
  console.log('1) 上传：--skip-upload，跳过')
} else {
  console.log(`1) 上传 ${files.length} 张 → ${PREFIX}`)
  const existing = new Map((await listObjects(cfg, PREFIX)).map((o) => [o.key, o.size]))
  for (const f of files) {
    const key = PREFIX + f
    const local = fs.statSync(path.join(LOCAL_DIR, f)).size
    if (existing.get(key) === local) {
      skipped++
      continue
    }
    if (!APPLY) {
      console.log(`   · 待传 ${f}（${(local / 1024).toFixed(0)}KB）`)
      continue
    }
    const res = await putObject(cfg, key, fs.readFileSync(path.join(LOCAL_DIR, f)), contentTypeOf(f))
    if (!res.ok) {
      console.error(`   ❌ ${f} 上传失败 HTTP ${res.status} ${res.body}`)
      process.exit(1)
    }
    uploaded++
    if (uploaded % 25 === 0) console.log(`   … 已传 ${uploaded} 张`)
  }
  console.log(`   结果：新传 ${uploaded} 张，已存在跳过 ${skipped} 张\n`)
}

/* ---------------- 2. 改库 ---------------- */
if (SKIP_DB) {
  console.log('2) 改库：--skip-db，跳过')
} else if (!APPLY) {
  console.log(`2) 改库（dry-run）：将执行 ${TABLES.length} 条 UPDATE，把 '${LOCAL_URL_PREFIX}' 换成 OSS 前缀`)
} else {
  console.log('2) 改库')
  for (const { table, column, rows } of counts) {
    if (!rows) {
      console.log(`   – ${table}.${column}：0 行，跳过`)
      continue
    }
    mysql(`update ${table} set ${column} = replace(${column}, '${LOCAL_URL_PREFIX}', '${OSS_URL_PREFIX}') where ${column} like '${LOCAL_URL_PREFIX}%'`)
    console.log(`   ✓ ${table}.${column}：${rows} 行`)
  }
}

/* ---------------- 3. 改种子 SQL ---------------- */
if (SKIP_SQL) {
  console.log('3) 种子 SQL：--skip-sql，跳过')
} else if (!sqlHits) {
  console.log('3) 种子 SQL：已经没有相对地址（可能已改过）')
} else if (!APPLY) {
  console.log(`3) 种子 SQL（dry-run）：将把 ${sqlHits} 处 '${LOCAL_URL_PREFIX} 替换成 OSS 前缀`)
} else {
  const src = fs.readFileSync(SQL_FILE, 'utf8')
  const out = src.split(`'${LOCAL_URL_PREFIX}`).join(`'${OSS_URL_PREFIX}`)
  fs.writeFileSync(SQL_FILE, out)
  console.log(`3) 种子 SQL：已替换 ${sqlHits} 处 → ${path.relative(ROOT, SQL_FILE)}`)
}

/* ---------------- 4. 抽样校验 ---------------- */
console.log('\n4) 抽样 HEAD 校验（公网可访问性）')
const samples = ['c-001.jpg', 'b-101.jpg', 's-01.jpg'].filter((f) => files.includes(f))
if (!APPLY) {
  console.log(`   （dry-run）将校验 ${samples.length} 张`)
} else {
  for (const f of samples) {
    const h = await headObject(cfg, PREFIX + f)
    console.log(h
      ? `   ✅ ${f}  ${(h.size / 1024).toFixed(0)}KB  ${h.contentType}  ${OSS_URL_PREFIX}${f}`
      : `   ❌ ${f}  取不到（HEAD 404）`)
  }
  const left = TABLES.map(([t, c]) => Number(mysql(`select count(*) from ${t} where ${c} like '${LOCAL_URL_PREFIX}%'`) || 0))
    .reduce((s, n) => s + n, 0)
  console.log(`   库里剩余相对地址：${left} 行 ${left === 0 ? '✅' : '⚠️'}`)
}

if (!APPLY) console.log('\n（dry-run 结束）确认无误后执行：node scripts/migrate-seed-images-to-oss.mjs --apply')
