import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './styles/app.css'

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
        <h1 style="margin:0 0 12px;font-size:20px;">\u9875\u9762\u542f\u52a8\u5931\u8d25</h1>
        <p style="margin:0 0 10px;line-height:1.6;">\u5f53\u524d\u6d4f\u89c8\u5668\u5728\u6267\u884c\u524d\u7aef\u811a\u672c\u65f6\u53d1\u751f\u9519\u8bef\u3002\u8bf7\u622a\u56fe\u8fd9\u4e00\u6bb5\u6587\u5b57\u53d1\u7ed9\u7ad9\u957f\u3002</p>
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
