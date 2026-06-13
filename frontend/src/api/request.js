import axios from 'axios'

/**
 * 后端 API 基础地址，优先使用环境变量 VITE_API_BASE_URL，
 * 未配置时默认回退到根路径（同源部署）。
 * @constant {string}
 */
const baseURL = import.meta.env.VITE_API_BASE_URL || '/'

/**
 * 封装后的 axios 实例，供所有 API 模块共用。
 * - baseURL：统一请求前缀
 * - timeout：10 秒超时，避免请求无限挂起
 * - withCredentials：携带跨域 cookie，支持 session 认证
 */
const request = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: true
})

/**
 * 响应拦截器：统一处理后端返回的 JSON 结构 { code, message, data }。
 * - code === 200 时直接解包返回 data 字段
 * - 其他情况视为业务错误，以 Error 形式 reject
 */
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      // 业务层错误 → 转换为 Promise.reject，上层可统一 catch
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    // 正常情况 → 只返回 data 负载
    return res.data
  },
  error => Promise.reject(error)
)

export default request
