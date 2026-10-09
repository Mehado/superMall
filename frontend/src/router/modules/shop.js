import { ROLE } from '@/utils/request'

/** 电商商城端（买家）—— 对应《电商前端开发手册》第 3 章 */
export default [
  {
    path: '/shop',
    name: 'shop-home',
    component: () => import('@/views/shop/MallHomeView.vue'),
    meta: { title: '商城首页' }
  },
  {
    path: '/shop/search',
    name: 'shop-search',
    component: () => import('@/views/shop/MallSearchView.vue'),
    meta: { title: '搜索 / 分类' }
  },
  {
    path: '/shop/products/:id',
    name: 'shop-product',
    component: () => import('@/views/shop/MallProductView.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/shop/cart',
    name: 'shop-cart',
    component: () => import('@/views/shop/MallCartView.vue'),
    meta: { title: '购物车', role: ROLE.mallUser }
  },
  {
    path: '/shop/checkout',
    name: 'shop-checkout',
    component: () => import('@/views/shop/MallCheckoutView.vue'),
    meta: { title: '确认订单', role: ROLE.mallUser }
  },
  {
    path: '/shop/orders',
    name: 'shop-orders',
    component: () => import('@/views/shop/MallOrdersView.vue'),
    meta: { title: '我的订单', role: ROLE.mallUser }
  },
  {
    path: '/shop/orders/:orderNumber',
    name: 'shop-order-detail',
    component: () => import('@/views/shop/MallOrderDetailView.vue'),
    meta: { title: '订单详情', role: ROLE.mallUser }
  },
  {
    path: '/shop/orders/:orderNumber/review',
    name: 'shop-order-review',
    component: () => import('@/views/shop/MallReviewView.vue'),
    meta: { title: '发表评价', role: ROLE.mallUser }
  },
  {
    path: '/shop/reviews',
    name: 'shop-my-reviews',
    component: () => import('@/views/shop/MallMyReviewsView.vue'),
    meta: { title: '我的评价', role: ROLE.mallUser }
  },
  {
    path: '/shop/favorites',
    name: 'shop-favorites',
    component: () => import('@/views/shop/MallFavoritesView.vue'),
    meta: { title: '我的收藏', role: ROLE.mallUser }
  },
  {
    path: '/shop/footprints',
    name: 'shop-footprints',
    component: () => import('@/views/shop/MallFootprintsView.vue'),
    meta: { title: '我的足迹', role: ROLE.mallUser }
  },
  {
    path: '/shop/addresses',
    name: 'shop-addresses',
    component: () => import('@/views/shop/MallAddressView.vue'),
    meta: { title: '收货地址', role: ROLE.mallUser }
  },
  {
    path: '/shop/me',
    name: 'shop-me',
    component: () => import('@/views/shop/MallProfileView.vue'),
    meta: { title: '个人中心', role: ROLE.mallUser }
  }
]
