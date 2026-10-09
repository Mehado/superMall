import { ROLE } from '@/utils/request'

/** 电商运营后台 —— 对应《电商前端开发手册》第 4 章 */
export default [
  {
    path: '/admin',
    name: 'admin-dashboard',
    component: () => import('@/views/admin/AdminDashboardView.vue'),
    meta: { title: '销售看板', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/products',
    name: 'admin-products',
    component: () => import('@/views/admin/AdminProductsView.vue'),
    meta: { title: '商品管理', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/products/new',
    name: 'admin-product-new',
    component: () => import('@/views/admin/AdminProductEditView.vue'),
    props: { id: 'new' },
    meta: { title: '新增商品', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/products/:id/edit',
    name: 'admin-product-edit',
    component: () => import('@/views/admin/AdminProductEditView.vue'),
    meta: { title: '编辑商品', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/categories',
    name: 'admin-categories',
    component: () => import('@/views/admin/AdminCategoriesView.vue'),
    meta: { title: '分类管理', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/brands',
    name: 'admin-brands',
    component: () => import('@/views/admin/AdminBrandsView.vue'),
    meta: { title: '品牌管理', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/banners',
    name: 'admin-banners',
    component: () => import('@/views/admin/AdminBannersView.vue'),
    meta: { title: '推荐位管理', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/orders',
    name: 'admin-orders',
    component: () => import('@/views/admin/AdminOrdersView.vue'),
    meta: { title: '订单管理', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/refunds',
    name: 'admin-refunds',
    component: () => import('@/views/admin/AdminRefundsView.vue'),
    meta: { title: '退款审核', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/reviews',
    name: 'admin-reviews',
    component: () => import('@/views/admin/AdminReviewsView.vue'),
    meta: { title: '评价管理', role: ROLE.mallAdmin }
  },
  {
    path: '/admin/members',
    name: 'admin-members',
    component: () => import('@/views/admin/AdminMembersView.vue'),
    meta: { title: '会员管理', role: ROLE.mallAdmin }
  }
]
