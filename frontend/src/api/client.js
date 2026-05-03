import axios from 'axios'
import { getToken, clearAuth } from '../utils/auth'

const client = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

// Добавляем токен к каждому запросу
client.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Если 401 — разлогиниваем
client.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client