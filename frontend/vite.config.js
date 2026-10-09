import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * 六端合一的单页应用（电商商城端 / 电商运营后台 / 外卖用户端 / 外卖商家端 / 外卖骑手端 / 外卖平台运营端）。
 *
 * - baseURL 全部留空，页面只用相对路径 /api/...，dev 与 Nginx 都不用改代码
 * - 电商后端在 8081（/api/mall/**），外卖后端在 8080（其余 /api/**）
 * - 路由懒加载，各端互不加载对方的 chunk
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const mallTarget = env.VITE_MALL_PROXY_TARGET || 'http://localhost:8081'
  const deliveryTarget = env.VITE_DELIVERY_PROXY_TARGET || 'http://localhost:8080'

  const proxy = {
    // 注意顺序：更长的前缀要写在前面
    '/api/mall': { target: mallTarget, changeOrigin: true },
    '/api': { target: deliveryTarget, changeOrigin: true },
    '/img': { target: deliveryTarget, changeOrigin: true }
  }

  return {
    plugins: [vue()],
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
    },
    server: {
      port: 5178,
      proxy,
      watch: {
        // Windows 下写文件不是原子的：Vite 可能在文件被截断的瞬間读到空内容，
        // 把模块缓存成空（表现为 mall.css 变成 "" → 全站样式丢失）。
        // awaitWriteFinish 会等文件稳定后再读，从根上避免这个竞态。
        awaitWriteFinish: { stabilityThreshold: 300, pollInterval: 60 }
      }
    },
    preview: { port: 4178, proxy },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules')) return 'vendor'
            return undefined
          }
        }
      }
    }
  }
})
