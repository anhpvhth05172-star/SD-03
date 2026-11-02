import http from './http'

export const dangKy = (data) => http.post('/auth/dang-ky', data).then((r) => r.data)

export const dangNhap = (data) => http.post('/auth/dang-nhap', data).then((r) => r.data)

export const dangNhapNhanVien = (data) =>
  http.post('/auth/nhan-vien/dang-nhap', data).then((r) => r.data)

export const guiMaQuenMatKhau = (email) =>
  http.post('/auth/quen-mat-khau/gui-ma', { email }).then((r) => r.data)

export const datLaiMatKhau = (data) =>
  http.post('/auth/quen-mat-khau/dat-lai', data).then((r) => r.data)

export const guiMaQuenMatKhauNhanVien = (email) =>
  http.post('/auth/nhan-vien/gui-ma-quen-mat-khau', { email }).then((r) => r.data)

export const datLaiMatKhauNhanVien = (data) =>
  http.post('/auth/nhan-vien/dat-lai-mat-khau', data).then((r) => r.data)

export const thongTin = () => http.get('/auth/thong-tin').then((r) => r.data)

export const huyPhien = (refreshToken) =>
  http.post('/auth/dang-xuat', { refreshToken }).then((r) => r.data)
