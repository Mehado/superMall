/**
 * 品牌资源 —— photo/ 下的 logo 与字标
 *
 * 约定：
 *   photo/logo/*           品牌 logo（文件夹里有图就用，文件名随意）
 *   photo/SuperMall.png    字标（替换原来的「superMall」文字）
 *
 * 取不到就返回 ''，调用方回退到内置的购物袋 SVG / 文字，页面不会塌。
 * 与 lib/mall-images.js 一样用 import.meta.glob —— 每个 pattern 必须是完整字符串字面量。
 */
const LOGO = import.meta.glob('/photo/logo/*.{png,jpg,jpeg,svg,webp,avif}', {
  eager: true,
  import: 'default'
})
/** 抠好白底的版本（由 SuperMall.png 生成），优先用 */
const WORDMARK_ALPHA = import.meta.glob('/photo/SuperMall-transparent.{png,webp,svg}', {
  eager: true,
  import: 'default'
})
const WORDMARK = import.meta.glob('/photo/SuperMall.{png,jpg,jpeg,svg,webp,avif}', {
  eager: true,
  import: 'default'
})
/** 主页面整页背景图（放 photo/portal-bg.{jpg,png,…} 即可生效；没有就退回原来的柔光背景） */
const PORTAL_BG = import.meta.glob('/photo/portal-bg.{jpg,jpeg,png,webp,avif}', {
  eager: true,
  import: 'default'
})

function firstUrl(mods) {
  return (
    Object.keys(mods)
      .sort()
      .map((k) => mods[k])
      .find((url) => typeof url === 'string' && url.length > 0) || ''
  )
}

export const brandLogo = firstUrl(LOGO)
export const brandWordmark = firstUrl(WORDMARK_ALPHA) || firstUrl(WORDMARK)
export const portalBackground = firstUrl(PORTAL_BG)
