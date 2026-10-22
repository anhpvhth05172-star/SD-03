import http from './http'
import { getMaAnDanh } from '@/utils/auth'

// header ẩn danh cho khách chưa đăng nhập — dùng chung getMaAnDanh (sessionStorage)
const headersAnDanh = () => ({ 'X-Ma-An-Danh': getMaAnDanh() })

// ---- Khách ----

export const layHoiThoaiKhach = () =>
  http.post('/chat/khach/hoi-thoai', null, { headers: headersAnDanh() }).then((r) => r.data)

export const guiTinKhach = (idHoiThoai, noiDung) =>
  http
    .post(`/chat/khach/hoi-thoai/${idHoiThoai}/tin-nhan`, { noiDung }, { headers: headersAnDanh() })
    .then((r) => r.data)

export const layTinKhach = (idHoiThoai, tuId) =>
  http
    .get(`/chat/khach/hoi-thoai/${idHoiThoai}/tin-nhan`, {
      params: { tuId },
      headers: headersAnDanh(),
    })
    .then((r) => r.data)

export const danhDaDocKhach = (idHoiThoai) =>
  http
    .put(`/chat/khach/hoi-thoai/${idHoiThoai}/da-doc`, null, { headers: headersAnDanh() })
    .then((r) => r.data)

export const lienKetSauDangNhap = (maAnDanh) =>
  http.post('/chat/khach/lien-ket', { maAnDanh }).then((r) => r.data)

// ---- AI ----

export const layHoiThoaiAI = () =>
  http.post('/chat/ai/hoi-thoai', null, { headers: headersAnDanh() }).then((r) => r.data)

export const hoiAI = (noiDung) =>
  http
    .post('/chat/ai/tin-nhan', { noiDung }, { headers: headersAnDanh() })
    .then((r) => r.data)

export const layTinAI = (idHoiThoai, tuId) =>
  http
    .get(`/chat/ai/hoi-thoai/${idHoiThoai}/tin-nhan`, {
      params: { tuId },
      headers: headersAnDanh(),
    })
    .then((r) => r.data)

// ---- Nhân viên ----

export const dsHoiThoaiNhanVien = () => http.get('/chat/nhan-vien/hoi-thoai').then((r) => r.data)

export const guiTinNhanVien = (idHoiThoai, noiDung) =>
  http.post(`/chat/nhan-vien/hoi-thoai/${idHoiThoai}/tin-nhan`, { noiDung }).then((r) => r.data)

export const layTinNhanVien = (idHoiThoai, tuId) =>
  http.get(`/chat/nhan-vien/hoi-thoai/${idHoiThoai}/tin-nhan`, { params: { tuId } }).then((r) => r.data)

export const danhDaDocNhanVien = (idHoiThoai) =>
  http.put(`/chat/nhan-vien/hoi-thoai/${idHoiThoai}/da-doc`).then((r) => r.data)

export const dongHoiThoai = (idHoiThoai) =>
  http.post(`/chat/nhan-vien/hoi-thoai/${idHoiThoai}/dong`).then((r) => r.data)
