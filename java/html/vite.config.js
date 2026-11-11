import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      // 将 /api 请求代理到本地 Spring Boot 后端（按实际端口修改 target）
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
