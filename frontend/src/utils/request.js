import axios from 'axios'

const request = axios.create({
  // 使用环境变量配置 API 基础地址
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      console.error('API Error:', res.message)
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data
  },
  error => {
    console.error('Request Error:', error)
    const d = error.response?.data
    const serverMsg =
      (typeof d === 'string' && d) ||
      d?.message ||
      d?.error ||
      error.message
    return Promise.reject(new Error(serverMsg || '网络或服务器错误'))
  }
)

export default request
