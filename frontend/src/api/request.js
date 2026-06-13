import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/'

const request = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: true
})

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
