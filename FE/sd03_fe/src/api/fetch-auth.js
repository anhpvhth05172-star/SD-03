import { getToken, getRefreshToken, isTokenExpired } from '../utils/auth.js'

const API_BASE =
  (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_API_BASE) ||
  'http://localhost:8080/api'

const API_ORIGIN = new URL(API_BASE).origin

/**
 * Một số trang (Sản phẩm, Biến thể, Thuộc tính, POS) gọi API bằng fetch() trần,
 * không gắn header Authorization. Sau khi siết bảo mật (endpoint sản phẩm yêu cầu
 * ROLE_NHAN_VIEN) các request này bị 401. Bọc window.fetch để tự gắn token
 * (tự refresh nếu token sắp hết hạn) cho mọi request trỏ tới origin API.
 */
export const caiDatFetchAuth = () => {
  if (typeof window === 'undefined' || typeof window.fetch !== 'function') return
  const goc = window.fetch.bind(window)

  const layUrl = (input) => {
    if (typeof input === 'string') return input
    if (typeof URL !== 'undefined' && input instanceof URL) return input.href
    if (typeof Request !== 'undefined' && input instanceof Request) return input.url
    return String(input)
  }

  window.fetch = async (input, init) => {
    const url = layUrl(input)
    if (!url.startsWith(API_ORIGIN)) return goc(input, init)

    const headers = new Headers(
      (init && init.headers) ||
        (typeof Request !== 'undefined' && input instanceof Request ? input.headers : undefined)
    )

    if (!headers.has('Authorization')) {
      let token = getToken()
      if ((!token || isTokenExpired(token)) && getRefreshToken()) {
        try {
          const { refreshSession } = await import('./http.js')
          await refreshSession()
          token = getToken()
        } catch {
          // Khong refresh duoc van gui bang token cu (server se tra 401 neu het han)
        }
      }
      if (token) headers.set('Authorization', `Bearer ${token}`)
    }

    if (typeof Request !== 'undefined' && input instanceof Request) {
      return goc(new Request(input, { ...(init || {}), headers }))
    }
    return goc(input, { ...(init || {}), headers })
  }
}
