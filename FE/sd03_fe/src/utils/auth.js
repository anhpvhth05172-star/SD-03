const TOKEN_KEY = 'polyshoes_token'
const USER_KEY = 'polyshoes_user'

export const getToken = () => localStorage.getItem(TOKEN_KEY)

export const getUser = () => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY))
  } catch {
    return null
  }
}

export const luuPhien = (resp) => {
  localStorage.setItem(TOKEN_KEY, resp.token)
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
  localStorage.removeItem(USER_KEY)
}
