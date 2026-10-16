import axios from 'axios'
import { getToken, getRefreshToken, isTokenExpired, luuPhien, dangXuat } from '../utils/auth'

export const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'

const API_ORIGIN = new URL(API_BASE).origin

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
  window.location.href = '/dang-nhap'
}

const ganToken = async (config) => {
  let token = getToken()
  if (token && !isTokenExpired(token)) {
    config.headers = { ...config.headers, Authorization: `Bearer ${token}` }
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
    config.headers = { ...config.headers, Authorization: `Bearer ${token}` }
  }
  return config
}

http.interceptors.request.use(async (config) => {
  if (String(config.url || '').includes('/auth/refresh')) {
    return config
  }
  return ganToken(config)
})

// Một số trang (module giảm giá) dùng axios mặc định thay vì instance http.
// Gắn token cho mọi request cùng origin API để không bị 401 sau khi siết bảo mật.
axios.interceptors.request.use(async (config) => {
  const url = String(config.url || '')
  if (!url.startsWith(API_ORIGIN)) return config
  if (url.includes('/auth/refresh')) return config
  return ganToken(config)
})

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const config = err.config
    const status = err.response?.status
    const la401 = status === 401
    const la403 = status === 403
    const laRefreshUrl = String(config?.url || '').includes('/auth/refresh')

    // 403: đã đăng nhập nhưng thiếu quyền — không đăng xuất, chỉ báo lỗi.
    if (la403) {
      let data403 = err.response?.data
      const msg403 = data403?.message || 'Bạn không có quyền thực hiện thao tác này'
      const e403 = new Error(msg403)
      e403.status = 403
      return Promise.reject(e403)
    }

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
    const e = new Error(msg)
    e.status = status
    return Promise.reject(e)
  },
)

export default http
