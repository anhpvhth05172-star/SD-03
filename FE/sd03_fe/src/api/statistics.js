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

