import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 0.0.0.0：局域网内手机/其他电脑可用 http://192.168.68.49:5173 访问
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    proxy: {
      // 将 /api 请求代理到后端（端口需与 application.yml 的 server.port 一致）
      '/api': {
        target: 'http://192.168.68.49:8080',
        changeOrigin: true
      }
    }
  }
})
