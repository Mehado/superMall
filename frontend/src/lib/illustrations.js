/**
 * 插图 —— photo/illustration/*.svg（unDraw 风格的空态 / 404 插图）
 *
 * 两个关键设计：
 *  ① **内联**（`?raw`）而不是 `<img src>`：插图主色已换成 `currentColor`
 *    （见 scripts/import-illustration.mjs），只有内联进 DOM，`currentColor` 才生效 ——
 *    一张图跟着主题着色。`<img>` 里的 SVG 是隔离文档，`currentColor` 只会变黑。
 *  ② **按张懒加载**：`import.meta.glob` 不带 `eager`，每张 SVG 各自成一个 chunk。
 *    一页通常只用一张（如空购物车），所以只会下载那一张（几 KB），
 *    而不是把整套 9 张（110KB / gzip 40KB）都拉下来。
 *
 * 用法：`<MEmpty image="empty-cart" title="购物车是空的" />`；取不到就回退线性图标，页面不会塌。
 * 新增插图：`node scripts/import-illustration.mjs <源.svg> --name xxx`
 */
const FILES = import.meta.glob('/photo/illustration/*.svg', {
  query: '?raw',
  import: 'default'
})

const name2path = (file) => file.split('/').pop().replace(/\.svg$/i, '')

/** name -> glob key（如 empty-cart -> /photo/illustration/empty-cart.svg） */
const BY_NAME = Object.fromEntries(Object.entries(FILES).map(([file]) => [name2path(file), file]))

/** 有哪些插图可用（调试 / 自检用） */
export const illustrationNames = Object.keys(BY_NAME).sort()

/** 有没有这张插图 */
export function hasIllustration(name) {
  return Boolean(name && BY_NAME[name])
}

/**
 * 取得插图的内联 SVG 源码（按需加载）；没有这张图返回 ''。
 */
export async function loadIllustration(name) {
  const key = name ? BY_NAME[name] : ''
  if (!key) return ''
  const mod = await FILES[key]()
  return typeof mod === 'string' ? mod : mod?.default || ''
}
