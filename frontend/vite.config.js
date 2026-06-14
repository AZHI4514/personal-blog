import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 构建配置文件。
 * 配置入口：https://vite.dev/config/
 *
 * @param {Object} context - Vite 提供的构建上下文
 * @param {string} context.command - 当前命令：'serve'（开发）或 'build'（生产）
 */
export default defineConfig(async ({ command }) => {
  // ==================== 插件列表 ====================

  /**
   * 插件数组，包含 Vue SFC 编译和旧版浏览器兼容。
   */
  const plugins = [
    // Vue 3 单文件组件编译插件
    vue(),
    // 旧版浏览器兼容：打包时生成 ES5 回退代码，支持 Android 5+ 和 iOS 10+
    legacy({
      targets: ['defaults', 'Android >= 5', 'iOS >= 10'],
      renderLegacyChunks: true,   // 生成独立的老式 chunk
      modernPolyfills: true       // 为现代浏览器也注入必要的 polyfill
    })
  ]

  // 开发模式下动态加载 Vue DevTools 插件（可选，不存在时静默跳过）
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

    // ==================== 构建配置 ====================
    build: {
      // CSS 编译目标：保持对旧移动端 WebView 的兼容
      cssTarget: ['chrome61', 'safari11'],
      // 生产构建关闭 sourcemap，减小打包体积
      sourcemap: false,
      // 使用 terser 压缩（对 ES5 回退更友好）
      minify: 'terser',
      // chunk 大小警告阈值设为 500KB
      chunkSizeWarningLimit: 500
    },

    // ==================== 路径别名 ====================
    resolve: {
      alias: {
        // @ → 项目 src 目录
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        // @framework → Live2D Cubism SDK 框架源码
        '@framework': fileURLToPath(new URL('../Live2d/CubismSdkForWeb-5-r.5/Framework/src', import.meta.url)),
        // @live2d-demo → Live2D 示例项目中的演示代码
        '@live2d-demo': fileURLToPath(new URL('../Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src', import.meta.url))
      }
    },

    // ==================== 开发服务器配置 ====================
    server: {
      // 文件系统访问：允许访问项目根目录以外的文件（如 Live2D SDK）
      fs: {
        allow: ['..']
      },
      // 代理配置：将前端 API 请求转发到后端 Spring Boot 服务（localhost:8080）
      proxy: {
        '/clap': {
          target: 'http://localhost:8080',
          changeOrigin: true   // 修改请求头中的 origin 为目标地址
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
        },
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})
