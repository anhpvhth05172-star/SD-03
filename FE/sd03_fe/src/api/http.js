import axios from 'axios'
import { getToken, getRefreshToken, isTokenExpired, luuPhien, dangXuat } from '../utils/auth'
import { vaiTroHienTai } from '../utils/session'

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

const chuyenVeDangNhap = () => {
  import('../router')
    .then((m) => {
      const router = m.default
      if (router.currentRoute.value.path !== '/dang-nhap') {
        router.push('/dang-nhap').catch(() => {})
      }
    })
    .catch(() => {})
}

http.interceptors.request.use(async (config) => {
  config.headers['X-Vai-Tro'] = vaiTroHienTai.value || 'ADMIN'
  if (String(config.url || '').includes('/auth/refresh')) {
    return config
  }
  let token = getToken()
  if (token && !isTokenExpired(token)) {
    config.headers.Authorization = `Bearer ${token}`
    return config
  }
  if (getRefreshToken()) {
    try {
      await refreshSession()
      token = getToken()
    } catch {
      // Phien co the da het han: van gui request.
    }
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const config = err.config
    const la401 = err.response?.status === 401
    const laRefreshUrl = String(config?.url || '').includes('/auth/refresh')

    if (la401 && config && !laRefreshUrl) {
      if (config._retried) {
        dangXuat()
        chuyenVeDangNhap()
        return Promise.reject(new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại'))
      }
      config._retried = true

      if (getRefreshToken()) {
        try {
          await refreshSession()
          config.headers = { ...config.headers, Authorization: `Bearer ${getToken()}` }
          return http.request(config)
        } catch {
          dangXuat()
          chuyenVeDangNhap()
          return Promise.reject(new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại'))
        }
      }
      if (getToken()) {
        dangXuat()
        chuyenVeDangNhap()
      }
    }

    let data = err.response?.data
    if (typeof Blob !== 'undefined' && data instanceof Blob) {
      try {
        data = JSON.parse(await data.text())
      } catch {
        data = null
      }
    }
    const msg = data?.message || data?.detail || 'Không kết nối được máy chủ'
    return Promise.reject(new Error(msg))
  },
)

export default http
