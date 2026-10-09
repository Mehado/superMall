import { ROLE } from '@/utils/request'

/**
 * 外卖平台运营端 + 商家入驻 / 骑手注册（公开页）。
 *
 * - /platform**        需要 platform-token（ROLE.platformAdmin）
 *   /platform          入驻审核
 *   /platform/riders   骑手管理
 *   /platform/profile  账号资料
 * - /merchant/apply    公开：商家自助入驻申请
 * - /rider/register    公开：骑手注册
 */
export default [
  {
    path: '/platform',
    name: 'platform-merchants',
    component: () => import('@/views/platform/PlatformMerchantAuditView.vue'),
    meta: { title: '商家入驻审核', role: ROLE.platformAdmin }
  },
  {
    path: '/platform/riders',
    name: 'platform-riders',
    component: () => import('@/views/platform/PlatformRiderView.vue'),
    meta: { title: '骑手管理', role: ROLE.platformAdmin }
  },
  {
    path: '/platform/profile',
    name: 'platform-profile',
    component: () => import('@/views/platform/PlatformProfileView.vue'),
    meta: { title: '账号资料', role: ROLE.platformAdmin }
  },
  {
    path: '/merchant/apply',
    name: 'merchant-apply',
    component: () => import('@/views/login/MerchantApplyView.vue'),
    meta: { title: '商家入驻' }
  },
  {
    path: '/rider/register',
    name: 'rider-register',
    component: () => import('@/views/login/RiderRegisterView.vue'),
    meta: { title: '骑手注册' }
  }
]
