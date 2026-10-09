/**
 * photo/ 图片管线 —— 本地演示图池（后端没图时的兜底）
 *
 * 为什么需要它：后端种子数据的图片基本是空的
 *   · mall_product  —— 种子数据里 main_image 为空
 *   · mall_category —— icon 为空
 *   · mall_banner   —— 默认 status=0，image 指向不存在的 OSS 地址
 * 所以「后端零图片」是常态，页面必须在没有图的情况下依然成立（显示占位块）。
 *
 * 只有下面这三个池子**真的会被用到**（并且都只在商城首页 /shop）：
 *   photo/product/   商品卡兜底图   建议 800×1000 (4:5)   首页「热销推荐 / 新品上架」
 *   photo/category/  分类卡图       建议 600×750  (4:5)   首页「按分类逛」
 *   photo/shortcut/  金刚区小图     建议 240×240  (1:1)   首页金刚区
 *
 * 约定：图片丢进目录即可，**文件名随意**（按文件名自然排序稳定取用，多退少补）。
 *      想控制顺序就用 01.jpg、02.jpg… 编号。
 *
 * 取图策略：**后端给了图就用后端的，没给才回落到本地图池**。
 * 所以将来后端补上真实图片时，这里一行都不用改，也不会出现「本地图盖住真图」。
 * 完整的「哪张图出现在哪个位置」清单见 frontend/photo/README.md。
 */

/**
 * 目录名 → 图片模块表（路径 → 编译后的 URL）
 *
 * ⚠️ 每个 glob 都必须写成**完整的字符串字面量**。
 * Vite 是在编译期静态解析 import.meta.glob 的，模板字符串拼接
 *（如 `/photo/product/*.${EXT}`）会直接抛
 * "Expected glob to be a string, but got dynamic template literal"。
 */
const MODULES = {
  category: import.meta.glob('/photo/category/*.{jpg,jpeg,png,webp,avif,svg}', {
    eager: true,
    import: 'default'
  }),
  product: import.meta.glob('/photo/product/*.{jpg,jpeg,png,webp,avif,svg}', {
    eager: true,
    import: 'default'
  }),
  shortcut: import.meta.glob('/photo/shortcut/*.{jpg,jpeg,png,webp,avif,svg}', {
    eager: true,
    import: 'default'
  })
}

/**
 * 自然排序：让 2.jpg 排在 10.jpg 前面（默认字典序会反过来）。
 * 这样放图的人可以按 1、2、3… 编号来控制顺序。
 */
function naturalCompare(a, b) {
  return a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' })
}

/** 把模块表摊平成按文件名排好序的 URL 数组 */
function toUrlList(modules) {
  return Object.keys(modules)
    .sort(naturalCompare)
    .map((path) => modules[path])
    .filter((url) => typeof url === 'string' && url.length > 0)
}

/** 三个图池，导出后可在任意组件里直接读（数量、是否为空） */
export const imagePools = Object.freeze({
  category: toUrlList(MODULES.category),
  product: toUrlList(MODULES.product),
  shortcut: toUrlList(MODULES.shortcut)
})

/**
 * 从图池里按序号稳定取一张。池子为空返回 ''（由调用方决定怎么兜底）。
 * 用取模而不是随机：同一个商品每次刷新都拿到同一张图，不会闪。
 */
export function pickFromPool(pool, index = 0) {
  const list = imagePools[pool]
  if (!list || list.length === 0) return ''
  const i = Math.abs(Math.trunc(Number(index) || 0)) % list.length
  return list[i]
}

/** 商品用：按商品 id 取图，保证「同一个商品永远是同一张」 */
export function productImage(id) {
  const n = Number(id)
  return pickFromPool('product', Number.isFinite(n) ? n : 0)
}

export const categoryImages = imagePools.category
export const productImages = imagePools.product
export const shortcutImages = imagePools.shortcut

/** 图池是否已有图 —— 让页面能区分「没放图」和「放了但取不到」 */
export const hasImages = Object.freeze({
  category: imagePools.category.length > 0,
  product: imagePools.product.length > 0,
  shortcut: imagePools.shortcut.length > 0
})

/**
 * 统一的取图入口：**优先后端给的图，没有才回落本地池**。
 *
 * @param {object} source  后端返回的对象（商品 / banner / 分类）
 * @param {string} pool    本地图池名：category | product | shortcut
 * @param {number} index   取池子里的第几张（通常传商品 id）
 * @returns {string}       可直接塞进 <img src> 的地址，取不到则 ''
 */
export function resolveImage(source, pool, index = 0) {
  if (source) {
    const remote =
      source.image ||
      source.mainImage ||
      source.imageUrl ||
      source.pic ||
      source.cover ||
      source.icon ||
      source.logo ||
      source.images?.[0]
    if (typeof remote === 'string' && remote.trim()) return remote
  }
  return pickFromPool(pool, index)
}
