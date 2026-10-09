import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/tokens.css'
import './styles/base.css'
import './styles/rail.css'
import './styles/mall.css'

/**
 * HTTP 层（utils/request.js）已经把业务失败 / 401 统一 toast 出来了，
 * 这里只兜住页面 onMounted 里未显式 catch 的 promise，避免控制台噪音。
 */
window.addEventListener('unhandledrejection', (event) => {
  event.preventDefault()
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
