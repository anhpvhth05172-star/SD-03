/**
 * Tiện ích tạo Avatar hình ảnh mặc định đẹp mắt, hiện đại cho Nhân viên & Khách hàng.
 * Đảm bảo 100% hoạt động offline, không phụ thuộc mạng ngoài.
 */

// SVG Avatar Nam hiện đại
const SVG_MALE = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><defs><linearGradient id="bgM" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%233b82f6"/><stop offset="100%" stop-color="%231d4ed8"/></linearGradient></defs><circle cx="40" cy="40" r="40" fill="url(%23bgM)"/><circle cx="40" cy="32" r="15" fill="%23ffffff" fill-opacity="0.95"/><path d="M16 68c0-13.25 10.75-24 24-24s24 10.75 24 24v4H16v-4z" fill="%23ffffff" fill-opacity="0.95"/></svg>`

// SVG Avatar Nữ hiện đại
const SVG_FEMALE = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><defs><linearGradient id="bgF" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%23ec4899"/><stop offset="100%" stop-color="%23be185d"/></linearGradient></defs><circle cx="40" cy="40" r="40" fill="url(%23bgF)"/><circle cx="40" cy="32" r="15" fill="%23ffffff" fill-opacity="0.95"/><path d="M16 68c0-13.25 10.75-24 24-24s24 10.75 24 24v4H16v-4z" fill="%23ffffff" fill-opacity="0.95"/></svg>`

// SVG Avatar Trung tính (Mặc định)
const SVG_DEFAULT = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><defs><linearGradient id="bgD" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%236366f1"/><stop offset="100%" stop-color="%234338ca"/></linearGradient></defs><circle cx="40" cy="40" r="40" fill="url(%23bgD)"/><circle cx="40" cy="32" r="15" fill="%23ffffff" fill-opacity="0.95"/><path d="M16 68c0-13.25 10.75-24 24-24s24 10.75 24 24v4H16v-4z" fill="%23ffffff" fill-opacity="0.95"/></svg>`

/**
 * Trả về đường dẫn ảnh Avatar hiển thị:
 * 1. Nếu có ảnh upload (base64 hoặc url) -> dùng ảnh đó.
 * 2. Nếu không có -> trả về SVG Avatar hình người chuẩn theo giới tính.
 */
export const getAvatarSrc = (item) => {
  if (!item) return SVG_DEFAULT

  const rawImg = item.anhNhanVien || item.anhKhachHang || item.avatar || item.hinhAnh
  if (rawImg && typeof rawImg === 'string' && rawImg.trim() !== '' && rawImg.trim() !== 'null') {
    return rawImg
  }

  const gt = String(item.gioiTinh || '').toUpperCase()
  if (gt === 'NU' || gt === 'NỮ' || gt === 'FEMALE') {
    return SVG_FEMALE
  }
  if (gt === 'NAM' || gt === 'MALE') {
    return SVG_MALE
  }

  return SVG_DEFAULT
}

/**
 * Xử lý khi ảnh thật bị lỗi load (404 hoặc corrupt)
 */
export const onAvatarError = (event, item) => {
  if (event && event.target) {
    const gt = String(item?.gioiTinh || '').toUpperCase()
    if (gt === 'NU' || gt === 'NỮ' || gt === 'FEMALE') {
      event.target.src = SVG_FEMALE
    } else if (gt === 'NAM' || gt === 'MALE') {
      event.target.src = SVG_MALE
    } else {
      event.target.src = SVG_DEFAULT
    }
  }
}
