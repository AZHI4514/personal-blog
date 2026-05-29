import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  baseURL: 'http://你的后端地址:8080',  // 后端基础地址
  timeout: 10000,                         // 请求超时时间（毫秒）
  withCredentials: true
})

// 请求拦截器：自动在请求头中添加 token（如果存在）
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')  // 假设 token 存在 localStorage
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一处理错误，并直接返回 data 部分
request.interceptors.response.use(
  response => {
    // 如果后端统一返回 { code, message, data }
    const res = response.data
    if (res.code !== 200) {
      // 这里可以统一弹出错误提示，例如 message.error(res.message)
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data  // 直接返回 data 字段的内容
  },
  error => Promise.reject(error)
)

export default request