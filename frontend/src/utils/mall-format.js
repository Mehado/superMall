/**
 * 电商专属的小工具（与外卖端隔离，不放进共享的 utils/format.js）。
 *
 * 后端商品列表 / 推荐 / 收藏 / 详情返回的是 priceLow / priceHigh（SPU 价格区间），
 * 没有单值 price —— 商品卡与详情页必须用它，否则会显示 ¥0.00。
 */
export function priceRange(product) {
  const rawLow = product?.priceLow ?? product?.price
  const rawHigh = product?.priceHigh ?? product?.price
  const low = Number(rawLow)
  const high = Number(rawHigh)
  const hasLow = Number.isFinite(low)
  const hasHigh = Number.isFinite(high)
  if (!hasLow && !hasHigh) return { low: null, high: null, range: false }
  const l = hasLow ? low : high
  const h = hasHigh ? high : low
  return { low: l, high: h, range: h > l }
}

/**
 * 把推荐位（mall_banner）的 linkType / linkValue 翻译成前端能执行的目标。
 *
 * 字段名以数据库注释为准（见 sql/mall.sql 的 mall_banner）：
 *   linkType  0 无跳转 / 1 商品 / 2 分类 / 3 外部链接
 *   linkValue 商品 id / 分类 id / 外链 URL
 *
 * 返回 null 表示这个位置不可点，调用方应当渲染成纯展示元素。
 */
export function bannerTarget(banner) {
  const value = banner?.linkValue
  if (value === null || value === undefined || value === '') return null

  const text = String(value)
  // 兼容运营直接填站内路径的情况（如 /shop/search）
  if (text.startsWith('/')) return { kind: 'route', to: text }
  if (text.startsWith('http://') || text.startsWith('https://')) {
    return { kind: 'external', to: text }
  }

  switch (Number(banner.linkType)) {
    case 1:
      return { kind: 'route', to: `/shop/products/${text}` }
    case 2:
      return { kind: 'route', to: { path: '/shop/search', query: { categoryId: text } } }
    case 3:
      return { kind: 'external', to: text }
    default:
      return null
  }
}
