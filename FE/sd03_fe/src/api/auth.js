import http from './http'

export const dangKy = (data) => http.post('/auth/dang-ky', data).then((r) => r.data)

export const dangNhap = (data) => http.post('/auth/dang-nhap', data).then((r) => r.data)

export const thongTin = () => http.get('/auth/thong-tin').then((r) => r.data)

export const huyPhien = (refreshToken) =>
  http.post('/auth/dang-xuat', { refreshToken }).then((r) => r.data)
