import { ROLE } from '@/utils/request'

/** 外卖骑手端 —— 对应《外卖前端开发手册》第 5 章 */
export default [
  {
    path: '/rider',
    name: 'rider-pool',
    component: () => import('@/views/rider/RiderPoolView.vue'),
    meta: { title: '抢单池', role: ROLE.rider }
  },
  {
    path: '/rider/mine',
    name: 'rider-mine',
    component: () => import('@/views/rider/RiderMineView.vue'),
    meta: { title: '我的配送', role: ROLE.rider }
  },
  {
    path: '/rider/profile',
    name: 'rider-profile',
    component: () => import('@/views/rider/RiderProfileView.vue'),
    meta: { title: '骑手资料', role: ROLE.rider }
  }
]
