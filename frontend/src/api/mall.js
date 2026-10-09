/* 电商（商城）端接口 —— 逐条对应《电商前端开发手册》第 7 章索引（93 个端点） */
import { mallUserHttp as user, mallAdminHttp as admin, uploaderOf } from '@/utils/request'

const M = '/api/mall'

export const mallAuthApi = {
  /** 图形验证码：一次性，登录失败后必须重新拉一张 */
  captcha: () => user.get(`${M}/auth/captcha`),
  policy: () => user.get(`${M}/auth/policy`),
  login: (body) => user.post(`${M}/user/login`, body),
  /** 注册接口直接返回 token，不要再调登录 */
  register: (body) => user.post(`${M}/user/register`, body),
  profile: () => user.get(`${M}/user/profile`),
  /** body 可只传想改的字段：nickname / phone / avatar（avatar 传空串=清除） */
  updateProfile: (body) => user.put(`${M}/user/profile`, body),
  /** 先上传拿 OSS URL，再把 URL 交给 updateProfile({ avatar }) 保存 */
  uploadAvatar: (file) => uploaderOf(user).single(`${M}/user/file/avatar`, file),
  adminLogin: (body) => admin.post(`${M}/admin/login`, body),
  adminProfile: () => admin.get(`${M}/admin/profile`)
}

export const mallCatalogApi = {
  /** position: 1 轮播 / 2 金刚区 / 3 其它 */
  banners: (position = 1) => user.get(`${M}/catalog/banners`, { params: { position } }),
  /** type: hot | new | rating */
  recommend: (type = 'hot', limit = 10) =>
    user.get(`${M}/catalog/recommend`, { params: { type, limit } }),
  categories: () => user.get(`${M}/catalog/categories`),
  brands: (params) => user.get(`${M}/catalog/brands`, { params }),
  /** 省市区三级联动：省份 → ?parentCode=省code → ?parentCode=市code */
  areas: (parentCode) => user.get(`${M}/catalog/areas`, { params: parentCode ? { parentCode } : {} }),
  products: (params) => user.get(`${M}/catalog/products/page`, { params }),
  product: (id) => user.get(`${M}/catalog/products/${id}`),
  reviews: (productId, params) => user.get(`${M}/catalog/products/${productId}/reviews`, { params }),
  reviewStats: (productId) => user.get(`${M}/catalog/products/${productId}/review-stats`)
}

export const mallCartApi = {
  /** item.price 是 SKU 当前价（实时），结算用 selectedAmount */
  list: () => user.get(`${M}/user/cart`),
  count: () => user.get(`${M}/user/cart/count`),
  /** 加购传 skuId，不是商品 id */
  add: (body) => user.post(`${M}/user/cart`, body),
  updateNumber: (id, number) => user.put(`${M}/user/cart/${id}`, { number }),
  selectRow: (id, selected) =>
    user.put(`${M}/user/cart/${id}/selected`, null, { params: { selected } }),
  selectAll: (selected) => user.put(`${M}/user/cart/selected`, null, { params: { selected } }),
  /** {id} 是购物车行 id */
  removeRow: (id) => user.delete(`${M}/user/cart/${id}`),
  clear: (scope = 'all') => user.delete(`${M}/user/cart`, { params: { scope } })
}

export const mallAddressApi = {
  list: () => user.get(`${M}/user/address`),
  detail: (id) => user.get(`${M}/user/address/${id}`),
  create: (body) => user.post(`${M}/user/address`, body),
  update: (body) => user.put(`${M}/user/address`, body),
  remove: (id) => user.delete(`${M}/user/address/${id}`),
  setDefault: (id) => user.put(`${M}/user/address/${id}/default`)
}

export const mallOrderApi = {
  /** 下单令牌：10 分钟一次性，失败（如购物车空）也会被消耗 */
  submitToken: () => user.get(`${M}/user/orders/token`),
  submit: (body) => user.post(`${M}/user/orders/submit`, body),
  page: (params) => user.get(`${M}/user/orders/page`, { params }),
  /** 路径里是订单号 orderNumber，不是数据库 id */
  detail: (orderNumber) => user.get(`${M}/user/orders/${orderNumber}`),
  cancel: (orderNumber) => user.put(`${M}/user/orders/${orderNumber}/cancel`),
  receipt: (orderNumber) => user.put(`${M}/user/orders/${orderNumber}/receipt`),
  applyRefund: (orderNumber, body) => user.post(`${M}/user/orders/${orderNumber}/refund`, body),
  refund: (orderNumber) => user.get(`${M}/user/orders/${orderNumber}/refund`)
}

export const mallPaymentApi = {
  payToken: (orderNumber) => user.get(`${M}/user/payment/token`, { params: { orderNumber } }),
  pay: (body) => user.post(`${M}/user/payment/pay`, body)
}

export const mallReviewApi = {
  /** 哪些明细已评：review === null 即未评 */
  orderReviewable: (orderNumber) => user.get(`${M}/user/reviews/order/${orderNumber}`),
  submit: (body) => user.post(`${M}/user/reviews`, body),
  follow: (id, body) => user.post(`${M}/user/reviews/${id}/follow`, body),
  mine: (params) => user.get(`${M}/user/reviews/page`, { params })
}

export const mallCenterApi = {
  favorites: (params) => user.get(`${M}/user/favorites`, { params }),
  favoriteExists: (productId) => user.get(`${M}/user/favorites/${productId}/exists`),
  addFavorite: (productId) => user.post(`${M}/user/favorites/${productId}`),
  removeFavorite: (productId) => user.delete(`${M}/user/favorites/${productId}`),
  favoriteCount: () => user.get(`${M}/user/favorites/count`),
  footprints: (params) => user.get(`${M}/user/footprints`, { params }),
  addFootprint: (productId) => user.post(`${M}/user/footprints/${productId}`),
  removeFootprint: (productId) => user.delete(`${M}/user/footprints/${productId}`),
  clearFootprints: () => user.delete(`${M}/user/footprints`),
  /** 晒图：先传到 OSS 拿 URL，再把 URL 数组放进 images */
  upload: (file) => uploaderOf(user).single(`${M}/user/file/upload`, file),
  uploadBatch: (files) => uploaderOf(user).batch(`${M}/user/file/upload-batch`, files)
}

export const mallAdminApi = {
  categoriesTree: () => admin.get(`${M}/admin/categories/tree`),
  createCategory: (body) => admin.post(`${M}/admin/categories`, body),
  updateCategory: (body) => admin.put(`${M}/admin/categories`, body),
  removeCategory: (id) => admin.delete(`${M}/admin/categories/${id}`),

  brands: (params) => admin.get(`${M}/admin/brands`, { params }),
  brand: (id) => admin.get(`${M}/admin/brands/${id}`),
  createBrand: (body) => admin.post(`${M}/admin/brands`, body),
  updateBrand: (body) => admin.put(`${M}/admin/brands`, body),
  removeBrand: (id) => admin.delete(`${M}/admin/brands/${id}`),

  products: (params) => admin.get(`${M}/admin/products/page`, { params }),
  product: (id) => admin.get(`${M}/admin/products/${id}`),
  createProduct: (body) => admin.post(`${M}/admin/products`, body),
  updateProduct: (body) => admin.put(`${M}/admin/products`, body),
  productStatus: (id, status) =>
    admin.put(`${M}/admin/products/${id}/status`, null, { params: { status } }),
  removeProduct: (id) => admin.delete(`${M}/admin/products/${id}`),

  banners: (params) => admin.get(`${M}/admin/banners`, { params }),
  banner: (id) => admin.get(`${M}/admin/banners/${id}`),
  createBanner: (body) => admin.post(`${M}/admin/banners`, body),
  updateBanner: (body) => admin.put(`${M}/admin/banners`, body),
  bannerStatus: (id, status) =>
    admin.put(`${M}/admin/banners/${id}/status`, null, { params: { status } }),
  removeBanner: (id) => admin.delete(`${M}/admin/banners/${id}`),

  orders: (params) => admin.get(`${M}/admin/orders/page`, { params }),
  order: (orderNumber) => admin.get(`${M}/admin/orders/${orderNumber}`),
  /** 发货：填物流公司 + 单号 */
  ship: (orderNumber, body) => admin.put(`${M}/admin/orders/${orderNumber}/ship`, body),

  refunds: (params) => admin.get(`${M}/admin/refunds/page`, { params }),
  refund: (id) => admin.get(`${M}/admin/refunds/${id}`),
  auditRefund: (id, body) => admin.put(`${M}/admin/refunds/${id}/audit`, body),

  reviews: (params) => admin.get(`${M}/admin/reviews/page`, { params }),
  removeReview: (id) => admin.delete(`${M}/admin/reviews/${id}`),

  members: (params) => admin.get(`${M}/admin/members/page`, { params }),
  member: (id) => admin.get(`${M}/admin/members/${id}`),
  memberStatus: (id, status) =>
    admin.put(`${M}/admin/members/${id}/status`, null, { params: { status } }),

  statsOverview: () => admin.get(`${M}/admin/stats/overview`),
  statsSalesTrend: (params) => admin.get(`${M}/admin/stats/sales-trend`, { params }),
  statsCategorySales: () => admin.get(`${M}/admin/stats/category-sales`),
  statsTopProducts: (params) => admin.get(`${M}/admin/stats/top-products`, { params }),

  upload: (file) => uploaderOf(admin).single(`${M}/admin/file/upload`, file),
  uploadBatch: (files) => uploaderOf(admin).batch(`${M}/admin/file/upload-batch`, files)
}
