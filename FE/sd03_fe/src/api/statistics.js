import http from './http'

/**
 * Lấy tổng quan thống kê cho khoảng ngày.
 * @param {string} tu - yyyy-MM-dd
 * @param {string} den - yyyy-MM-dd
 * @param {string|null} thuongHieu - tên thương hiệu (optional)
 * @returns {Promise<{doanhThu:number,tienMat:number,tienChuyenKhoan:number,soDon:number,aov:number,sanPhamBan:number,khachMoi:number,doanhThuDuKien:number}>}
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

/**
 * Lấy sản phẩm bán được theo ngày từ hóa đơn thật trong database.
 * @param {string} tu - yyyy-MM-dd
 * @param {string} den - yyyy-MM-dd
 * @param {string|null} thuongHieu - optional
 * @returns {Promise<Array<{ngay:string,nhanNgay:string,soLuongBan:number,doanhThu:number,soDon:number}>>}
 */
export const fetchSanPhamTheoNgay = (tu, den, thuongHieu = null) => {
  const params = { tu, den }
  if (thuongHieu) params.thuongHieu = thuongHieu
  return http.get('/thong-ke/san-pham-theo-ngay', { params }).then((r) => r.data)
}

/**
 * Lấy thống kê cơ cấu thương hiệu (doanh thu, số lượng, tỷ lệ).
 * @param {string} tu - yyyy-MM-dd
 * @param {string} den - yyyy-MM-dd
 * @returns {Promise<{tongDoanhThu:number,tongSoLuong:number,danhSach:Array<{ten:string,doanhThu:number,soLuong:number,tyLe:number,mau:string}>}>}
 */
export const fetchThuongHieuThongKe = (tu, den) => {
  return http.get('/thong-ke/thuong-hieu', { params: { tu, den } }).then((r) => r.data)
}

/**
 * Lấy bảng thống kê sản phẩm (mã, tên, ảnh, size/màu, thương hiệu, đã bán, trả, doanh thu, tồn kho).
 * @param {string} tu - yyyy-MM-dd
 * @param {string} den - yyyy-MM-dd
 * @param {string|null} thuongHieu
 * @param {string|null} keyword
 */
export const fetchSanPhamBang = (tu, den, thuongHieu = null, keyword = null) => {
  const params = { tu, den }
  if (thuongHieu) params.thuongHieu = thuongHieu
  if (keyword) params.keyword = keyword
  return http.get('/thong-ke/san-pham-bang', { params }).then((r) => r.data)
}

/**
 * Lấy danh sách đơn hàng chi tiết khi bấm vào các thẻ KPI.
 * @param {Object} params
 */
export const fetchDonHangChiTiet = (params) => {
  return http.get('/thong-ke/don-hang-chi-tiet', { params }).then((r) => r.data)
}

/**
 * Lấy danh sách sản phẩm đã bán chi tiết khi bấm vào thẻ KPI Sản phẩm đã bán.
 * @param {string} tu
 * @param {string} den
 * @param {string|null} thuongHieu
 * @param {string|null} keyword
 */
export const fetchSanPhamDaBan = (tu, den, thuongHieu = null, keyword = null) => {
  const params = { tu, den }
  if (thuongHieu) params.thuongHieu = thuongHieu
  if (keyword) params.keyword = keyword
  return http.get('/thong-ke/san-pham-da-ban', { params }).then((r) => r.data)
}

/**
 * Lấy email mặc định nhận báo cáo (Admin hoạt động, fallback mail username).
 * @returns {Promise<{email:string}>}
 */
export const fetchEmailMacDinh = () =>
  http.get('/thong-ke/email-mac-dinh').then((r) => r.data)

/**
 * Gửi báo cáo tổng kết doanh thu (kết quả thật từ server).
 * @param {{loai:'THANG'|'NAM', thang?:number, nam:number, emailNhan:string}} payload
 * @returns {Promise<{thanhCong:boolean,emailNhan:string,tieuDe:string,lyDo:string|null}>}
 */
export const guiMailTongKet = (payload) =>
  http.post('/thong-ke/gui-mail-tong-ket', payload).then((r) => r.data)

/**
 * So sánh doanh thu theo kỳ + top sản phẩm.
 * @param {{cheDo:string,nam?:number,tu1?:string,den1?:string,tu2?:string,den2?:string,thuongHieu?:string,keyword?:string}} params
 * @returns {Promise<{ky:Array<{maKy:string,...}>,topSanPham:Array}>}
 */
export const fetchSoSanh = (params) =>
  http.get('/thong-ke/so-sanh', { params }).then((r) => r.data)

export const getRevenueSummary = ({ tu, den }) =>
  http.get('/thong-ke/doanh-thu', { params: { tu, den } }).then((r) => r.data)

export const listStatisticEmployees = (params) =>
  http.get('/thong-ke/nhan-vien', { params }).then((r) => r.data)
