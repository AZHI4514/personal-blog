import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
import './styles/app.css'

/**
 * 将各种错误对象统一转换成可显示的字符串。
 * 支持 Error 对象（提取 stack）、字符串、null/undefined 等类型。
 * @param {*} error - 捕获到的错误对象
 * @returns {string} 格式化后的错误消息文本
 */
function normalizeErrorMessage(error) {
  if (!error) return 'Unknown startup error'
  if (typeof error === 'string') return error
  return error.stack || error.message || String(error)
}

/**
 * 在页面上渲染应用启动失败的错误提示界面。
 * 仅在 Vue 尚未挂载时使用——避免白屏，让用户看到有意义的错误信息。
 * @param {string} message - 要展示的错误消息
 */
function renderBootError(message) {
  const target = document.getElementById('app')
  if (!target) return

  target.innerHTML = `
    <div style="min-height:100vh;padding:24px;box-sizing:border-box;background:#87D4DA;color:#12343b;font-family:monospace;">
      <div style="max-width:960px;margin:0 auto;background:#fffef7;border:2px solid #79ACC5;padding:16px 18px;box-shadow:2px 2px 0 rgba(0,0,0,0.12);">
        <h1 style="margin:0 0 12px;font-size:20px;">页面启动失败</h1>
        <p style="margin:0 0 10px;line-height:1.6;">当前浏览器在执行前端脚本时发生错误。请截图这一段文字发给站长。</p>
        <pre style="margin:0;white-space:pre-wrap;word-break:break-word;">${message}</pre>
      </div>
    </div>
  `
}

// 全局错误边界：捕获同步运行时错误。
// 仅在 Vue 尚未挂载（#app 下无子元素）时渲染错误界面，避免覆盖 Vue 自己的错误处理。
window.addEventListener('error', (event) => {
  const message = normalizeErrorMessage(event.error || event.message)
  console.error('Global boot error:', message)
  if (!document.querySelector('#app > *')) {
    renderBootError(message)
  }
})

// 全局错误边界：捕获未处理的 Promise rejection。
// 同上，仅在 Vue 尚未挂载时接管 UI。
window.addEventListener('unhandledrejection', (event) => {
  const message = normalizeErrorMessage(event.reason)
  console.error('Unhandled promise rejection:', message)
  if (!document.querySelector('#app > *')) {
    renderBootError(message)
  }
})

// 创建 Vue 3 应用实例
const app = createApp(App)

// 安装路由插件
app.use(router)

// 挂载应用到 #app 容器，外层 try/catch 兜底捕获挂载阶段的同步异常
try {
  app.mount('#app')
} catch (error) {
  const message = normalizeErrorMessage(error)
  console.error('Vue mount failed:', message)
  renderBootError(message)
}
