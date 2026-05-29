import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      '/clap': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {          // 新增代理，用于图片访问
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/musics': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/posts': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/users': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/visitor-stats': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    }
  }
})
