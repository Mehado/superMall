import { defineStore } from 'pinia'
import { ROLE, ROLE_META, clearSession, getToken } from '@/utils/request'
import { read, write } from '@/utils/storage'

/** 六端会话：每把 token 独立存放、互不覆盖 —— 多端共用一个工程的关键 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    activeRole: read('active-role', ROLE.deliveryUser),
    revision: 0
  }),
  actions: {
    isLoggedIn(role) {
      this.revision // 依赖 revision，登出/登录后自动重算
      return Boolean(getToken(role))
    },
    profileOf(role) {
      this.revision
      return read(`profile:${role}`, null)
    },
    metaOf(role) {
      return ROLE_META[role]
    },
    saveSession(role, token, profile = null) {
      write(`token:${role}`, token)
      if (profile) write(`profile:${role}`, profile)
      this.activeRole = role
      write('active-role', role)
      this.revision++
    },
    setProfile(role, profile) {
      write(`profile:${role}`, profile)
      this.revision++
    },
    logout(role) {
      clearSession(role)
      this.revision++
    }
  }
})
