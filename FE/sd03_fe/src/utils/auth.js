const TOKEN_KEY = 'polyshoes_token'
const REFRESH_KEY = 'polyshoes_refresh'
const USER_KEY = 'polyshoes_user'

export const getToken = () => localStorage.getItem(TOKEN_KEY)

export const getRefreshToken = () => localStorage.getItem(REFRESH_KEY)

export const getUser = () => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY))
  } catch {
    return null
  }
}

export const luuPhien = (resp) => {
  localStorage.setItem(TOKEN_KEY, resp.token)
  if (resp.refreshToken) {
    localStorage.setItem(REFRESH_KEY, resp.refreshToken)
  } else {
    localStorage.removeItem(REFRESH_KEY)
  }
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      idKhachHang: resp.idKhachHang,
      tenTaiKhoan: resp.tenTaiKhoan,
      email: resp.email,
      tenKhachHang: resp.tenKhachHang,
      vaiTro: resp.vaiTro,
    }),
  )
}

export const dangXuat = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

export const isTokenExpired = (token) => {
  if (!token) return true
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (!payload.exp) return false
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}
