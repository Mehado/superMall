/* 外卖端接口 —— 用户 / 商家 / 骑手三端，对应《外卖前端开发手册》第 10 章索引（67 个端点） */
import {
  deliveryUserHttp as user,
  merchantHttp as merchant,
  riderHttp as rider,
  platformHttp as platform,
  uploaderOf
} from '@/utils/request'

export const foodAuthApi = {
  captcha: () => user.get('/api/auth/captcha'),
  policy: () => user.get('/api/auth/policy'),
  userLogin: (body) => user.post('/api/user/login', body),
  /** 注册直接返回 token */
  userRegister: (body) => user.post('/api/user/register', body),
  merchantLogin: (body) => merchant.post('/api/merchant/login', body),
  riderLogin: (body) => rider.post('/api/delivery/login', body),

  /** 商家自助入驻（公开）：提交后待平台审核，通过才能登录商家端 */
  merchantApply: (body) => merchant.post('/api/merchant/apply', body),
  /** 按邮箱查入驻申请状态（公开） */
  merchantApplyStatus: (username) =>
    user.get('/api/merchant/apply/status', { params: { username } }),
  /** 骑手注册（公开）：注册即启用，直接返回 token */
  riderRegister: (body) => rider.post('/api/delivery/register', body)
}

/** 平台运营端（外卖）：审核商家入驻、管理骑手。请求头 platform-token */
export const platformApi = {
  login: (body) => platform.post('/api/platform/login', body),
  /** 我的资料（password 恒为 null） */
  profile: () => platform.get('/api/platform/profile'),
  /** 改资料（目前只有 name） */
  updateProfile: (body) => platform.put('/api/platform/profile', body),
  overview: () => platform.get('/api/platform/overview'),
  /** auditStatus：0 待审核 1 已通过 2 已驳回；不传=全部（待审优先） */
  merchants: (params) => platform.get('/api/platform/merchants/page', { params }),
  auditMerchant: (id, body) => platform.put(`/api/platform/merchants/${id}/audit`, body),
  deliverymen: (params) => platform.get('/api/platform/deliverymen/page', { params }),
  updateDeliverymanStatus: (id, status) =>
    platform.put(`/api/platform/deliverymen/${id}/status`, null, { params: { status } })
}

export const foodPublicApi = {
  /** 首页随机商品流，返回列表（不是分页），可按 businessType 过滤 */
  products: (params) => user.get('/api/public/products', { params }),
  product: (id) => user.get(`/api/public/products/${id}`),
  /** 商家列表：现在分页（page/pageSize）+ 店名模糊（keyword） */
  merchants: (params) => user.get('/api/public/merchants', { params }),
  merchantDetail: (id) => user.get(`/api/public/merchants/${id}`),
  merchantCategories: (id) => user.get(`/api/public/merchants/${id}/categories`),
  merchantProducts: (id, params) => user.get(`/api/public/merchants/${id}/products`, { params })
}

export const foodUserApi = {
  profile: () => user.get('/api/user/profile'),
  /** body 可只传想改的字段：nickname / phone / avatar（avatar 传空串=清除） */
  updateProfile: (body) => user.put('/api/user/profile', body),
  updatePassword: (body) => user.put('/api/user/password', body),
  /** 先上传拿 OSS URL，再把 URL 交给 updateProfile({ avatar }) 保存 */
  uploadAvatar: (file) => uploaderOf(user).single('/api/user/avatar', file),

  addresses: () => user.get('/api/user/address'),
  address: (id) => user.get(`/api/user/address/${id}`),
  createAddress: (body) => user.post('/api/user/address', body),
  updateAddress: (body) => user.put('/api/user/address', body),
  removeAddress: (id) => user.delete(`/api/user/address/${id}`),
  setDefaultAddress: (id) => user.put(`/api/user/address/default/${id}`),

  /** 购物车按「用户 × 商家」分组；items[].id 是购物车明细 id */
  cart: () => user.get('/api/user/cart'),
  addCart: (body) => user.post('/api/user/cart', body),
  /** number = 0 即删除该行 */
  updateCart: (body) => user.put('/api/user/cart', body),
  removeCartItem: (id) => user.delete(`/api/user/cart/${id}`),
  clearMerchantCart: (merchantId) => user.delete(`/api/user/cart/merchant/${merchantId}`),
  clearCart: () => user.delete('/api/user/cart'),

  submitToken: () => user.get('/api/user/orders/token'),
  submitOrder: (body) => user.post('/api/user/orders/submit', body),
  /** page / pageSize 必传，后端没有默认值 */
  orders: (params) => user.get('/api/user/orders', { params }),
  order: (id) => user.get(`/api/user/orders/${id}`),
  cancelOrder: (id) => user.put(`/api/user/orders/cancel/${id}`),

  payToken: (orderNumber) => user.get('/api/user/payment/token', { params: { orderNumber } }),
  pay: (body) => user.post('/api/user/payment/pay', body)
}

export const merchantApi = {
  shop: () => merchant.get('/api/merchant/shop'),
  updateShop: (body) => merchant.put('/api/merchant/shop', body),
  business: () => merchant.get('/api/merchant/shop/business'),
  /** 休息中服务端会强制禁止用户加购与下单 */
  updateBusiness: (body) => merchant.put('/api/merchant/shop/business', body),

  categories: (params) => merchant.get('/api/merchant/categories', { params }),
  category: (id) => merchant.get(`/api/merchant/categories/${id}`),
  createCategory: (body) => merchant.post('/api/merchant/categories', body),
  updateCategory: (body) => merchant.put('/api/merchant/categories', body),
  removeCategory: (id) => merchant.delete(`/api/merchant/categories/${id}`),

  products: (params) => merchant.get('/api/merchant/products', { params }),
  product: (id) => merchant.get(`/api/merchant/products/${id}`),
  createProduct: (body) => merchant.post('/api/merchant/products', body),
  updateProduct: (body) => merchant.put('/api/merchant/products', body),
  removeProduct: (id) => merchant.delete(`/api/merchant/products/${id}`),

  skusOfProduct: (productId) => merchant.get(`/api/merchant/skus/product/${productId}`),
  sku: (id) => merchant.get(`/api/merchant/skus/${id}`),

  /** beginTime / endTime 格式必须 yyyy-MM-dd HH:mm:ss（带秒） */
  orders: (params) => merchant.get('/api/merchant/orders', { params }),
  orderStatistics: () => merchant.get('/api/merchant/orders/statistics'),
  order: (id) => merchant.get(`/api/merchant/orders/${id}`),
  accept: (id) => merchant.put(`/api/merchant/orders/accept/${id}`),
  /** 备餐完成必须调，否则不会生成配送单、骑手抢单池看不到 */
  ready: (id) => merchant.put(`/api/merchant/orders/ready/${id}`),
  reject: (body) => merchant.put('/api/merchant/orders/reject', body),
  simulatePay: (orderNumber) => merchant.put(`/api/merchant/orders/simulate-pay/${orderNumber}`),

  upload: (file) => uploaderOf(merchant).single('/api/merchant/file/upload', file)
}

export const riderApi = {
  profile: () => rider.get('/api/delivery/profile'),
  updateProfile: (body) => rider.put('/api/delivery/profile', body),
  pool: (params) => rider.get('/api/delivery/orders/pool', { params }),
  mine: (params) => rider.get('/api/delivery/orders/mine', { params }),
  /** 路径里的 {orderId} 是订单 id，不是配送单 id */
  order: (orderId) => rider.get(`/api/delivery/orders/${orderId}`),
  grab: (orderId) => rider.put(`/api/delivery/orders/grab/${orderId}`),
  pickup: (orderId) => rider.put(`/api/delivery/orders/pickup/${orderId}`),
  delivered: (orderId) => rider.put(`/api/delivery/orders/delivered/${orderId}`),
  statistics: () => rider.get('/api/delivery/orders/statistics')
}
