import axios from 'axios'
import { getToken, getRefreshToken, isTokenExpired, luuPhien, dangXuat } from '../utils/auth'

export const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'

const http = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise = null

export const refreshSession = () => {
  if (!refreshPromise) {
    const refreshToken = getRefreshToken()
    if (!refreshToken) return Promise.resolve(null)
    refreshPromise = axios
      .post(`${API_BASE}/auth/refresh`, { refreshToken })
      .then((resp) => {
        luuPhien(resp.data)
        return resp.data
      })
      .catch(() => null)
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

http.interceptors.request.use(async (config) => {
  const token = getToken()
  if (token && !isTokenExpired(token)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const msg =
      err.response?.data?.message ||
      err.response?.data?.detail ||
      'Không kết nối được máy chủ'
    return Promise.reject(new Error(msg))
  },
)

export default http
