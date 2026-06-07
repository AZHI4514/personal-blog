import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

function normalizeErrorMessage(error) {
  if (!error) return 'Unknown startup error'
  if (typeof error === 'string') return error
  return error.stack || error.message || String(error)
}

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

window.addEventListener('error', (event) => {
  const message = normalizeErrorMessage(event.error || event.message)
  console.error('Global boot error:', message)
  if (!document.querySelector('#app > *')) {
    renderBootError(message)
  }
})

window.addEventListener('unhandledrejection', (event) => {
  const message = normalizeErrorMessage(event.reason)
  console.error('Unhandled promise rejection:', message)
  if (!document.querySelector('#app > *')) {
    renderBootError(message)
  }
})

const app = createApp(App)

app.use(createPinia())
app.use(router)

try {
  app.mount('#app')
} catch (error) {
  const message = normalizeErrorMessage(error)
  console.error('Vue mount failed:', message)
  renderBootError(message)
}
