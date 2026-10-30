const TOKEN_KEY = 'polyshoes_token'
const REFRESH_KEY = 'polyshoes_refresh'
const USER_KEY = 'polyshoes_user'
const LOGIN_KIND_KEY = 'polyshoes_last_login_kind'
const MA_AN_DANH_KEY = 'polyshoes_ma_an_danh'

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
      idKhachHang: resp.idKhachHang ?? null,
      idNhanVien: resp.idNhanVien ?? null,
      loaiTaiKhoan: resp.loaiTaiKhoan || 'KHACH_HANG',
      tenTaiKhoan: resp.tenTaiKhoan,
      email: resp.email,
      tenKhachHang: resp.tenKhachHang,
      vaiTro: resp.vaiTro,
    }),
  )
  if (resp.loaiTaiKhoan) {
    localStorage.setItem(LOGIN_KIND_KEY, resp.loaiTaiKhoan === 'NHAN_VIEN' ? 'nhan-vien' : 'khach')
  }
}

export const getLastLoginPath = () =>
  localStorage.getItem(LOGIN_KIND_KEY) === 'nhan-vien' ? '/login-nhan-vien' : '/login'

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

/** UUID ẩn danh cho khách chưa đăng nhập (chat) — chỉ sống trong phiên tab (sessionStorage). */
export const getMaAnDanh = () => {
  let ma = sessionStorage.getItem(MA_AN_DANH_KEY)
  if (!ma) {
    ma =
      typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}`
    sessionStorage.setItem(MA_AN_DANH_KEY, ma)
  }
  // Dọn key cũ trên localStorage (phiên bản trước lưu bền vững)
  localStorage.removeItem(MA_AN_DANH_KEY)
  return ma
}

export const isNhanVienDangNhap = () => {
  const u = getUser()
  return u?.loaiTaiKhoan === 'NHAN_VIEN'
}

export const isKhachHangDangNhap = () => {
  const u = getUser()
  return !!getToken() && u?.loaiTaiKhoan !== 'NHAN_VIEN'
}
