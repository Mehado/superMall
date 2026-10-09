/**
 * 滚动图池 —— photo/marquee/ 下的商品图（首页字标背景用）
 *
 * 约定与 lib/mall-images.js 一致：**文件名随意**，按文件名自然排序取用（01.jpg、02.jpg…）。
 * 池子为空时返回 []，调用方不渲染滚动层（页面照旧成立）。
 *
 * ⚠️ 放图注意：浏览器按**扩展名**取 MIME，扩展名写错（比如把 WebP/AVIF 存成 .png）
 * 虽然部分浏览器会「猜内容」渲染出来，但不稳，请转成真格式再放（本项目已统一转 jpg）。
 */
const MALL_MODULES = import.meta.glob('/photo/marquee/*.{jpg,jpeg,png,webp,avif,svg}', {
  eager: true,
  import: 'default'
})
/** 外卖首页用另一个池，两边各放各的图 */
const FOOD_MODULES = import.meta.glob('/photo/marquee-food/*.{jpg,jpeg,png,webp,avif,svg}', {
  eager: true,
  import: 'default'
})

/** 自然排序：2.jpg 排在 10.jpg 前面，方便用 01、02… 编号控制顺序 */
function naturalCompare(a, b) {
  return a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' })
}

function toUrlList(modules) {
  return Object.keys(modules)
    .sort(naturalCompare)
    .map((path) => modules[path])
    .filter((url) => typeof url === 'string' && url.length > 0)
}

/** 商城首页（photo/marquee/） */
export const marqueeImages = toUrlList(MALL_MODULES)
export const hasMarqueeImages = marqueeImages.length > 0

/** 外卖首页（photo/marquee-food/） */
export const foodMarqueeImages = toUrlList(FOOD_MODULES)
export const hasFoodMarqueeImages = foodMarqueeImages.length > 0

/**
 * 把图池切成「每组 n 张」的滚动分组。
 *
 * 每列取池子里的一段**滑动窗口**（起点错开），这样：
 *   1. 每列都是「n 张一组」；
 *   2. 5 列不会显示同一组，看起来不像复制粘贴；
 *   3. 池子里的图全部用得上。
 */
export function buildGroups(images, { columns = 5, perGroup = 5 } = {}) {
  const list = images || []
  if (!list.length || columns < 1) return []
  const n = list.length
  const step = Math.max(1, Math.round(n / columns))
  return Array.from({ length: columns }, (_, i) => {
    const group = []
    for (let k = 0; k < perGroup; k++) group.push(list[(i * step + k) % n])
    return group
  })
}
