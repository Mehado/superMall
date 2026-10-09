// scripts/import-illustration.mjs
// 把外部 SVG 插图（unDraw 之类）处理成项目能直接用的「内联插图」。
//
// 做的事：
//   ① 主色 → currentColor：外层容器设 color，插图自动跟随主题（--m-accent 品牌蓝）
//   ② 深色 → 项目墨色 #16130f（与 tokens.css 的 --m-ink 一致）
//   ③ 去掉 width/height（保留 viewBox），尺寸交给 CSS；加 aria-hidden（旁边的文案已经说明了）
//   ④ 原图留档到 frontend/photo/_source/illustration/（不打包、不上屏）
//
// 用法：
//   node scripts/import-illustration.mjs <源.svg> --name empty-cart
//   node scripts/import-illustration.mjs <源.svg> --name empty-cart --keep-colors   # 只搬运，不换色
//   node scripts/import-illustration.mjs "C:/Users/1/Desktop/分类配图/*.svg"        # 批量（用文件名做 name）
//
// 之后：在页面里用 <MEmpty image="empty-cart" ... />（MEmpty 会内联渲染，无需再引 CSS）
import fs from 'node:fs'
import path from 'node:path'

const ROOT = process.cwd()
const OUT_DIR = path.join(ROOT, 'frontend', 'photo', 'illustration')
const SRC_DIR = path.join(ROOT, 'frontend', 'photo', '_source', 'illustration')

/** 主色 → currentColor（unDraw 默认紫；部分素材还会用粉色副色 #fd6584，一并归到主色） */
const ACCENT = ['#6c63ff', '#fd6584']
/** 深色 → 项目墨色 */
const DARK = ['#3f3d56', '#090814', '#231f20', '#2f2e41']
const INK = '#16130f'

const args = process.argv.slice(2)
const keepColors = args.includes('--keep-colors')
const nameIdx = args.indexOf('--name')
const explicitName = nameIdx > -1 ? args[nameIdx + 1] : null
const inputs = args.filter((a) => !a.startsWith('--') && a !== explicitName)

if (!inputs.length) {
  console.error('用法：node scripts/import-illustration.mjs <源.svg|通配符> [--name xxx] [--keep-colors]')
  process.exit(1)
}

/** 展开通配符（Windows 下 shell 不展开） */
function expand(pattern) {
  if (!pattern.includes('*')) return fs.existsSync(pattern) ? [pattern] : []
  const dir = path.dirname(pattern)
  const re = new RegExp('^' + path.basename(pattern).replace(/[.+^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*') + '$')
  return fs.existsSync(dir) ? fs.readdirSync(dir).filter((f) => re.test(f)).map((f) => path.join(dir, f)) : []
}

function transform(svg) {
  let out = svg
  const hits = []
  if (!keepColors) {
    for (const c of ACCENT) {
      const re = new RegExp(c, 'gi')
      const n = (out.match(re) || []).length
      if (n) hits.push(`主色 ${c}→currentColor ×${n}`)
      out = out.replace(re, 'currentColor')
    }
    for (const c of DARK) {
      const re = new RegExp(c, 'gi')
      const n = (out.match(re) || []).length
      if (n) hits.push(`深色 ${c}→${INK} ×${n}`)
      out = out.replace(re, INK)
    }
  }
  // 尺寸交给 CSS
  out = out.replace(/\s(width|height)="[^"]*"/g, '')
  // 无障碍：旁边有文案，插图本身对读屏隐藏
  if (/aria-hidden=/.test(out)) out = out.replace(/aria-hidden="[^"]*"/, 'aria-hidden="true"')
  else out = out.replace(/<svg\b/, '<svg aria-hidden="true" focusable="false"')
  return { out, hits }
}

fs.mkdirSync(OUT_DIR, { recursive: true })
fs.mkdirSync(SRC_DIR, { recursive: true })

let ok = 0
for (const input of inputs) {
  for (const file of expand(input)) {
    const base = explicitName || path.basename(file).replace(/\.svg$/i, '')
    const name = base
      .replace(/^undraw_/, '')
      .replace(/_[a-z0-9]{4}$/i, '')
      .replace(/_/g, '-')
      .toLowerCase()
    const svg = fs.readFileSync(file, 'utf8')
    if (!/^<svg|<svg\b/.test(svg)) {
      console.log(`⏭  跳过（不是 SVG）：${file}`)
      continue
    }
    const { out, hits } = transform(svg)
    fs.writeFileSync(path.join(OUT_DIR, `${name}.svg`), out)
    fs.copyFileSync(file, path.join(SRC_DIR, `${name}.svg`)) // 原图留档
    console.log(`✅ ${name}.svg  ←  ${path.basename(file)}  ${hits.length ? '（' + hits.join('，') + '）' : '（未换色）'}`)
    ok++
  }
}

console.log(`\n输出目录：frontend/photo/illustration/   原图留档：frontend/photo/_source/illustration/`)
console.log(`共处理 ${ok} 个。图片是内联渲染的，改完**不用重启** dev server（HMR 会自己刷）。`)
if (ok && !keepColors) {
  console.log('提示：换色规则在脚本顶部 ACCENT / DARK 两个数组里；新素材若用别的色板，先在那里加一行。')
}
