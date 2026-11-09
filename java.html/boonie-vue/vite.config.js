import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 监听所有网卡(0.0.0.0)：本机 localhost/127.0.0.1 与局域网 192.168.68.30 均可访问
    host: '0.0.0.0',
    open: true,
    // 开发环境下把 /api 代理到 Spring Boot 后端（9090），改走本机局域网 IP
    proxy: {
      '/api': {
        target: 'http://192.168.68.30:9090',
        changeOrigin: true,
      },
    },
  },
})
