import axios from 'axios'
import { readText } from '@/utils/storage'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/'

const request = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: true
})

request.interceptors.request.use(
  config => {
    const token = readText('token', '')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res.data
  },
  error => Promise.reject(error)
)

export default request
