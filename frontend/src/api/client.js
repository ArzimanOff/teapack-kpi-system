import axios from 'axios'
import {
  getToken, getRefreshToken, setAccessToken, clearAuth,
} from '../utils/auth'

const client = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

// Отдельный axios без перехватчиков — чтобы refresh-запрос не зацикливался
const rawAxios = axios.create({ baseURL: 'http://localhost:8080', timeout: 10000 })

let refreshInFlight = null

function refreshAccessToken() {
  if (refreshInFlight) return refreshInFlight
  const refreshToken = getRefreshToken()
  if (!refreshToken) return Promise.reject(new Error('no refresh token'))

  refreshInFlight = rawAxios
    .post('/api/auth/refresh', { refreshToken })
    .then(res => {
      const { token } = res.data || {}
      if (!token) throw new Error('no access token in refresh response')
      setAccessToken(token)
      return token
    })
    .finally(() => { refreshInFlight = null })

  return refreshInFlight
}

client.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  response => response,
  async error => {
    const original = error.config
    const status = error.response?.status

    // Не пытаемся обновлять токен на самом /auth/refresh и /auth/login
    const url = original?.url || ''
    const isAuthCall = url.includes('/api/auth/refresh') || url.includes('/api/auth/login')

    if (status === 401 && !isAuthCall && !original._retry) {
      original._retry = true
      try {
        const newToken = await refreshAccessToken()
        original.headers.Authorization = `Bearer ${newToken}`
        return client(original)
      } catch (e) {
        clearAuth()
        window.location.href = '/login'
        return Promise.reject(e)
      }
    }

    if (status === 401) {
      clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client
