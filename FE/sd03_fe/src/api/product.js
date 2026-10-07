import http from './http'

export const getSanPhams = (keyword) =>
  http
    .get('/san-pham', { params: keyword ? { keyword } : {} })
    .then((r) => r.data)

export const getSanPham = (id) => http.get(`/san-pham/${id}`).then((r) => r.data)

export const createSanPham = (data) => http.post('/san-pham', data).then((r) => r.data)

export const getSanPhamChiTiets = (id) =>
  http.get(`/san-pham/${id}/chi-tiet`).then((r) => r.data)

export const createSanPhamChiTiet = (id, data) =>
  http.post(`/san-pham/${id}/chi-tiet`, data).then((r) => r.data)

export const getThuocTinh = () => http.get('/thuoc-tinh').then((r) => r.data)
