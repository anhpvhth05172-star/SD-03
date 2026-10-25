import axios from 'axios'
import { getToken, getRefreshToken, isTokenExpired, luuPhien, dangXuat } from '../utils/auth'

export const API_BASE = import.meta.env.VITE_API_BASE || '/api'

// Không set Content-Type mặc định ở đây: axios tự gắn application/json cho body object,
// còn FormData (upload Excel) phải để nguyên cho browser gắn multipart/form-data + boundary.
// Set cứng 'application/json' sẽ làm axios serialize FormData thành JSON -> backend không nhận được file.
// API_BASE có thể là đường dẫn tương đối (/api) — khi đó origin = origin của trang hiện tại,
// nên FE chạy qua localhost hay IP LAN đều call BE được (qua Vite proxy).
export const API_ORIGIN = new URL(
  API_BASE,
  typeof window !== 'undefined' && window.location ? window.location.origin : 'http://localhost'
).origin

/** URL có trỏ tới API không (tương đối /api/... hoặc tuyệt đối cùng origin). */
export const laUrlAPI = (url) => url.startsWith(API_BASE) || url.startsWith(API_ORIGIN)

// Các endpoint này phải gọi được độc lập với phiên hiện tại. Nếu token cũ hết hạn,
// không refresh/điều hướng trước khi request đăng nhập được gửi lên máy chủ.
const laAuthCongKhai = (url = '') =>
  /^\/auth\/(dang-nhap|nhan-vien\/dang-nhap|dang-ky|quen-mat-khau\/gui-ma|quen-mat-khau\/dat-lai|nhan-vien\/gui-ma-quen-mat-khau|nhan-vien\/dat-lai-mat-khau)(?:\?|$)/.test(url)

const http = axios.create({
  baseURL: API_BASE,
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
      const refreshed = await refreshSession()
      if (!refreshed?.token) {
        dangXuat()
        chuyenVeDangNhap()
        return config
      }
      token = getToken()
    } catch {
      dangXuat()
      chuyenVeDangNhap()
      return config
    }
  }
  if (token) {
    config.headers = { ...config.headers, Authorization: `Bearer ${token}` }
  }
  return config
}

http.interceptors.request.use(async (config) => {
  if (String(config.url || '').includes('/auth/refresh') || laAuthCongKhai(config.url)) {
    return config
  }
  return ganToken(config)
})

// Một số trang (module giảm giá) dùng axios mặc định thay vì instance http.
// Gắn token cho mọi request cùng origin API để không bị 401 sau khi siết bảo mật.
axios.interceptors.request.use(async (config) => {
  const url = String(config.url || '')
  if (!laUrlAPI(url)) return config
  if (url.includes('/auth/refresh') || laAuthCongKhai(url)) return config
  return ganToken(config)
})

http.interceptors.response.use(
  (res) => {
    const method = String(res?.config?.method || '').toLowerCase()
    if (['post', 'put', 'patch', 'delete'].includes(method)) {
      try {
        localStorage.setItem('polyshoes_pos_sync_signal', String(Date.now()))
      } catch {}
    }
    return res
  },
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
          const refreshed = await refreshSession()
          if (!refreshed?.token) throw new Error('Refresh thất bại')
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

axios.interceptors.response.use((res) => {
  const method = String(res?.config?.method || '').toLowerCase()
  if (['post', 'put', 'patch', 'delete'].includes(method)) {
    try {
      localStorage.setItem('polyshoes_pos_sync_signal', String(Date.now()))
    } catch {}
  }
  return res
})

export default http
