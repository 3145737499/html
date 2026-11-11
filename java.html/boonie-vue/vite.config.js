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
    // 监听所有网卡(0.0.0.0)：本机 localhost/127.0.0.1 与局域网任意 IP 均可访问
    host: '0.0.0.0',
    open: true,
    // 开发环境下把 /api 代理到本机后端（9090）。
    // 用 127.0.0.1 环回地址而非局域网 IP：后端地址随本机 IP 变化也不影响，
    // 根治旧版「connect ECONNREFUSED 192.168.68.30:9090」导致注册/登录全部失败的问题。
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9090',
        changeOrigin: true,
      },
    },
  },
})
