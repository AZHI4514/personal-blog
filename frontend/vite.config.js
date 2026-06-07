import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(async ({ command }) => {
  const plugins = [
    vue(),
    legacy({
      targets: ['defaults', 'Android >= 5', 'iOS >= 10'],
      renderLegacyChunks: true,
      modernPolyfills: true
    })
  ]

  if (command === 'serve') {
    try {
      const { default: vueDevTools } = await import('vite-plugin-vue-devtools')
      plugins.push(vueDevTools())
    } catch (error) {
      console.warn('vite-plugin-vue-devtools disabled:', error)
    }
  }

  return {
    plugins,
    build: {
      // Keep CSS output conservative for older mobile WebViews.
      cssTarget: ['chrome61', 'safari11'],
      sourcemap: false,        // 关键：关闭 sourcemap 省内存
      minify: 'terser',
      chunkSizeWarningLimit: 500
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '@framework': fileURLToPath(new URL('../Live2d/CubismSdkForWeb-5-r.5/Framework/src', import.meta.url)),
        '@live2d-demo': fileURLToPath(new URL('../Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src', import.meta.url))
      }
    },
    server: {
      fs: {
        allow: ['..']
      },
      proxy: {
        '/clap': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/images': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/uploads': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/musics': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/posts': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/ai': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/users': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/visitor-stats': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})
