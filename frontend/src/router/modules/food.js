import { ROLE } from '@/utils/request'

/** 外卖用户端 —— 对应《外卖前端开发手册》第 3 章 */
export default [
  {
    path: '/food',
    name: 'food-home',
    component: () => import('@/views/food/FoodHomeView.vue'),
    meta: { title: '外卖首页' }
  },
  {
    path: '/food/merchants',
    name: 'food-merchants',
    component: () => import('@/views/food/FoodMerchantsView.vue'),
    meta: { title: '商家列表' }
  },
  {
    path: '/food/shops/:id',
    name: 'food-shop',
    component: () => import('@/views/food/FoodShopView.vue'),
    meta: { title: '店铺详情' }
  },
  {
    path: '/food/products/:id',
    name: 'food-product',
    component: () => import('@/views/food/FoodProductView.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/food/cart',
    name: 'food-cart',
    component: () => import('@/views/food/FoodCartView.vue'),
    meta: { title: '购物车', role: ROLE.deliveryUser }
  },
  {
    path: '/food/checkout',
    name: 'food-checkout',
    component: () => import('@/views/food/FoodCheckoutView.vue'),
    meta: { title: '确认订单', role: ROLE.deliveryUser }
  },
  {
    path: '/food/orders',
    name: 'food-orders',
    component: () => import('@/views/food/FoodOrdersView.vue'),
    meta: { title: '我的订单', role: ROLE.deliveryUser }
  },
  {
    path: '/food/orders/:id',
    name: 'food-order-detail',
    component: () => import('@/views/food/FoodOrderDetailView.vue'),
    meta: { title: '订单详情', role: ROLE.deliveryUser }
  },
  {
    path: '/food/me',
    name: 'food-me',
    component: () => import('@/views/food/FoodProfileView.vue'),
    meta: { title: '个人中心', role: ROLE.deliveryUser }
  }
]
