import { ROLE } from '@/utils/request'

/** 外卖商家端 —— 对应《外卖前端开发手册》第 4 章 */
export default [
  {
    path: '/merchant',
    name: 'merchant-dashboard',
    component: () => import('@/views/merchant/MerchantDashboardView.vue'),
    meta: { title: '经营看板', role: ROLE.merchant }
  },
  {
    path: '/merchant/orders',
    name: 'merchant-orders',
    component: () => import('@/views/merchant/MerchantOrdersView.vue'),
    meta: { title: '订单管理', role: ROLE.merchant }
  },
  {
    path: '/merchant/products',
    name: 'merchant-products',
    component: () => import('@/views/merchant/MerchantProductsView.vue'),
    meta: { title: '商品管理', role: ROLE.merchant }
  },
  {
    path: '/merchant/products/new',
    name: 'merchant-product-new',
    component: () => import('@/views/merchant/MerchantProductEditView.vue'),
    props: { id: 'new' },
    meta: { title: '新增商品', role: ROLE.merchant }
  },
  {
    path: '/merchant/products/:id/edit',
    name: 'merchant-product-edit',
    component: () => import('@/views/merchant/MerchantProductEditView.vue'),
    meta: { title: '编辑商品', role: ROLE.merchant }
  },
  {
    path: '/merchant/categories',
    name: 'merchant-categories',
    component: () => import('@/views/merchant/MerchantCategoriesView.vue'),
    meta: { title: '分类管理', role: ROLE.merchant }
  },
  {
    path: '/merchant/shop',
    name: 'merchant-shop',
    component: () => import('@/views/merchant/MerchantShopView.vue'),
    meta: { title: '店铺设置', role: ROLE.merchant }
  }
]
