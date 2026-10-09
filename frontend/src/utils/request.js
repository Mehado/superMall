import axios from 'axios'
import { notify } from '@/lib/toast'
import { read, remove } from './storage'

/**
 * 多端并存的关键：每个角色一把 token、一个请求头，互不覆盖也互不通用。
 * 电商买家 / 外卖用户虽然头名都叫 token，但存储与实例彼此独立。
 */
export const ROLE = {
  mallUser: 'mall-user',
  mallAdmin: 'mall-admin',
  deliveryUser: 'delivery-user',
  merchant: 'merchant',
  rider: 'rider',
  platformAdmin: 'platform-admin'
}

export const ROLE_META = {
  [ROLE.mallUser]: { header: 'token', label: '商城买家', home: '/shop', accent: 'amber' },
  [ROLE.mallAdmin]: { header: 'mall-admin-token', label: '商城运营', home: '/admin', accent: 'ink' },
  [ROLE.deliveryUser]: { header: 'token', label: '外卖用户', home: '/food', accent: 'emerald' },
  [ROLE.merchant]: { header: 'merchant-token', label: '外卖商家', home: '/merchant', accent: 'ink' },
  [ROLE.rider]: { header: 'delivery-token', label: '外卖骑手', home: '/rider', accent: 'ink' },
  [ROLE.platformAdmin]: { header: 'platform-token', label: '平台运营', home: '/platform', accent: 'ink' }
}

export const tokenKey = (role) => `token:${role}`
export const profileKey = (role) => `profile:${role}`
export const getToken = (role) => read(tokenKey(role), '')

export function clearSession(role) {
  remove(tokenKey(role))
  remove(profileKey(role))
}

function redirectToLogin(role) {
  if (window.location.pathname === '/login') return
  const here = window.location.pathname + window.location.search
  window.location.assign(
    `/login?role=${encodeURIComponent(role)}&redirect=${encodeURIComponent(here)}`
  )
}

/**
 * 统一 axios 实例工厂。
 * - 成功和业务失败 HTTP 都是 200，必须判 code === 1
 * - 401 = 该角色 token 失效（角色二次校验）
 */
function createHttp(role) {
  const { header } = ROLE_META[role]
  const http = axios.create({ baseURL: '', timeout: 15000 })

  http.interceptors.request.use((config) => {
    const token = getToken(role)
    if (token) config.headers[header] = token
    return config
  })

  http.interceptors.response.use(
    (res) => {
      const body = res.data
      if (body && typeof body === 'object' && typeof body.code !== 'undefined') {
        if (body.code === 1) return body.data
        const message = body.msg || '操作失败'
        notify(message, 'err')
        return Promise.reject(new Error(message))
      }
      return body
    },
    (error) => {
      const status = error?.response?.status
      if (status === 401) {
        clearSession(role)
        notify(`${ROLE_META[role].label}登录状态已失效，请重新登录`, 'err')
        redirectToLogin(role)
      } else {
        notify(error?.response?.data?.msg || error?.message || '网络异常，请稍后重试', 'err')
      }
      return Promise.reject(error)
    }
  )

  return http
}

export const mallUserHttp = createHttp(ROLE.mallUser)
export const mallAdminHttp = createHttp(ROLE.mallAdmin)
export const deliveryUserHttp = createHttp(ROLE.deliveryUser)
export const merchantHttp = createHttp(ROLE.merchant)
export const riderHttp = createHttp(ROLE.rider)
export const platformHttp = createHttp(ROLE.platformAdmin)

/** 图片上传：multipart，超时放宽 */
export function uploaderOf(http) {
  return {
    single: (url, file, field = 'file') => {
      const form = new FormData()
      form.append(field, file)
      return http.post(url, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30000
      })
    },
    batch: (url, files, field = 'files') => {
      const form = new FormData()
      Array.from(files).forEach((f) => form.append(field, f))
      return http.post(url, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000
      })
    }
  }
}
