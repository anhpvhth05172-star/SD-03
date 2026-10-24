import http from './http'

// ===== Admin APIs =====
// NOTE: BE controller dùng /api/v1/san-pham, http.js base là /api → cần prefix /v1
export const getSanPhams = (keyword) =>
  http
    .get('/v1/san-pham', { params: keyword ? { keyword } : {} })
    .then((r) => r.data)

export const getSanPham = (id) => http.get(`/v1/san-pham/${id}`).then((r) => r.data)

export const createSanPham = (data) => http.post('/v1/san-pham', data).then((r) => r.data)

export const getSanPhamChiTiets = (id) =>
  http.get(`/v1/san-pham/${id}/chi-tiet`).then((r) => r.data)

export const createSanPhamChiTiet = (id, data) =>
  http.post(`/v1/san-pham/${id}/chi-tiet`, data).then((r) => r.data)

export const getThuocTinh = () => http.get('/v1/thuoc-tinh').then((r) => r.data)

// ===== Client APIs =====

/**
 * Lấy danh sách sản phẩm cho trang shop (có phân trang, lọc)
 * @param {Object} params - { page, size, keyword, idThuongHieu, idLoaiGiay, trangThai }
 */
export const getSanPhamClient = (params = {}) =>
  http
    .get('/v1/san-pham', {
      params: {
        page: 0,
        size: 100,
        trangThai: true,
        ...params,
      },
    })
    .then((r) => r.data)

/**
 * Lấy chi tiết 1 sản phẩm theo id
 */
export const getSanPhamById = (id) =>
  http.get(`/v1/san-pham/${id}`).then((r) => r.data)

/**
 * Lấy tất cả biến thể (chi tiết) của 1 sản phẩm
 */
export const getBienTheBySanPham = (idSanPham) =>
  http
    .get('/v1/san-pham-chi-tiet', {
      params: { idSanPham, trangThai: true, size: 200 },
    })
    .then((r) => r.data)

