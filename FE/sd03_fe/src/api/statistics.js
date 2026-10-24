import http from './http'

/**
 * Lấy tổng quan thống kê cho khoảng ngày.
 * @param {string} tu - yyyy-MM-dd
 * @param {string} den - yyyy-MM-dd
 * @param {string|null} thuongHieu - tên thương hiệu (optional)
 * @returns {Promise<{doanhThu:number,tienMat:number,tienChuyenKhoan:number,soDon:number,aov:number,sanPhamBan:number,khachMoi:number}>}
 */
export const fetchTongQuan = (tu, den, thuongHieu = null) => {
  const params = { tu, den }
  if (thuongHieu) params.thuongHieu = thuongHieu
  return http.get('/thong-ke/tong-quan', { params }).then((r) => r.data)
}

/**
 * Lấy cơ cấu trạng thái đơn hàng cho khoảng ngày.
 * @param {string} tu - yyyy-MM-dd
 * @param {string} den - yyyy-MM-dd
 * @param {string|null} thuongHieu - tên thương hiệu (optional)
 * @returns {Promise<Array<{ten:string,soLuong:number}>>}
 */
export const fetchTrangThaiDon = (tu, den, thuongHieu = null) => {
  const params = { tu, den }
  if (thuongHieu) params.thuongHieu = thuongHieu
  return http.get('/thong-ke/trang-thai-don', { params }).then((r) => r.data)
}
