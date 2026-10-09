import { createRouter, createWebHistory } from 'vue-router'
import { ROLE, ROLE_META, getToken } from '@/utils/request'
import shopRoutes from './modules/shop'
import adminRoutes from './modules/admin'
import foodRoutes from './modules/food'
import merchantRoutes from './modules/merchant'
import riderRoutes from './modules/rider'
import platformRoutes from './modules/platform'

const routes = [
  {
    path: '/',
    name: 'portal',
    component: () => import('@/views/PortalView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/login/RegisterView.vue'),
    meta: { title: '注册' }
  },
  ...shopRoutes,
  ...adminRoutes,
  ...foodRoutes,
  ...merchantRoutes,
  ...riderRoutes,
  ...platformRoutes,
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    if (saved) return saved
    return { top: 0 }
  }
})

/**
 * 某个路径需要哪个身份（`''` = 公开页面，任何身份都能去）。
 * 以路由的 `meta.role` 为唯一依据，避免在登录页里再维护一份「路径 → 角色」的映射。
 */
export function roleRequiredBy(path) {
  try {
    return router.resolve(String(path)).meta?.role || ''
  } catch {
    return ''
  }
}

/**
 * 登录成功后该落到哪里。
 *
 * ⚠️ 这里必须校验 `redirect` 的**归属身份**：
 * 六端共用一个工程，用户在 A 端被拦下（`redirect=/shop/me`）后会先到登录页，
 * 在登录页把身份筹码切到 B 端（商城运营）再登录 —— 此时若还按 `redirect` 跳到 `/shop/me`，
 * 路由守卫会因「没有 mall-user 的 token」把它拦回 `/login?role=mall-user&redirect=/shop/me`，
 * 用户看到的就是「提示登录成功，然后卡在登录页」（2026-09-21 修）。
 *
 * 规则：redirect 是公开页、或正好属于刚登录的身份 → 用它；否则回该身份主页。
 */
export function landingFor(role, redirect) {
  const home = ROLE_META[role].home
  const target = redirect ? String(redirect) : ''
  if (!target) return { to: home, dropped: false }
  const pathOnly = target.split('?')[0].split('#')[0]
  // 指回登录/注册页没有意义，直接回主页
  if (pathOnly === '/login' || pathOnly === '/register') return { to: home, dropped: false }
  const need = roleRequiredBy(target)
  if (!need || need === role) return { to: target, dropped: false }
  return { to: home, dropped: true, need }
}

/**
 * 路由守卫：多端共用一份路由表，靠 meta.role 做角色隔离。
 * 没登录 → 回登录页并带上 redirect；已登录还去登录页 → 直接回该角色工作台。
 */
router.beforeEach((to) => {
  const role = to.meta.role

  if (to.path === '/login') {
    const asked = to.query.role
    if (asked && getToken(asked)) {
      // 同样要校验 redirect 的归属身份，否则会在 /login ↔ 别的角色的页面之间来回弹
      return landingFor(asked, to.query.redirect).to
    }
    return true
  }

  if (role && !getToken(role)) {
    return { path: '/login', query: { role, redirect: to.fullPath } }
  }

  return true
})

router.afterEach((to) => {
  const label = to.meta.title
  document.title = label ? `${label} · superMall` : 'superMall · 商城 & 外卖'
  // 商城 / 外卖 / 后台 / 主页面 / 登录页共用 mall.css 设计系统，让品牌色与字体统一。
  // 注册页不带这些 class，样式不受影响。
  const isShop = to.path.startsWith('/shop')
  const isFood = to.path.startsWith('/food')
  const isAdmin = to.path.startsWith('/admin')
  const isPortal = to.path === '/'
  const isLogin = to.path === '/login'
  const isRegister = to.path === '/register'
  // 404（catch-all）也要在主题里，否则它会退回 base.css 的旧样式
  const isNotFound = to.name === 'not-found'
  // 商家端 / 骑手端 / 平台运营端同属外卖体系
  const isFoodWorkbench =
    to.path.startsWith('/merchant') ||
    to.path.startsWith('/rider') ||
    to.path.startsWith('/platform')
  document.documentElement.classList.toggle(
    'theme-mall',
    isShop || isFood || isAdmin || isFoodWorkbench || isPortal || isLogin || isRegister || isNotFound
  )
  document.documentElement.classList.toggle('theme-food', isFood || isFoodWorkbench)
})

export { ROLE }
export default router
