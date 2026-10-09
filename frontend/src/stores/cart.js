import { defineStore } from 'pinia'
import { mallCartApi } from '@/api/mall'
import { foodUserApi } from '@/api/delivery'
import { getToken, ROLE } from '@/utils/request'

/** 两端的购物车角标都收在这里，页面只读 store */
export const useCartStore = defineStore('cart', {
  state: () => ({
    mallTotal: 0,
    mallInvalid: 0,
    foodTotal: 0,
    foodGroups: []
  }),
  actions: {
    async refreshMall() {
      if (!getToken(ROLE.mallUser)) {
        this.mallTotal = 0
        this.mallInvalid = 0
        return
      }
      try {
        const data = await mallCartApi.count()
        this.mallTotal = data?.total ?? 0
        this.mallInvalid = data?.invalid ?? 0
      } catch {
        /* 401 由拦截器统一处理 */
      }
    },
    async refreshFood() {
      if (!getToken(ROLE.deliveryUser)) {
        this.foodTotal = 0
        this.foodGroups = []
        return
      }
      try {
        const data = await foodUserApi.cart()
        this.foodGroups = Array.isArray(data) ? data : []
        this.foodTotal = this.foodGroups.reduce(
          (sum, g) => sum + (g.items || []).reduce((s, i) => s + (i.number || 0), 0),
          0
        )
      } catch {
        /* ignore */
      }
    }
  }
})
