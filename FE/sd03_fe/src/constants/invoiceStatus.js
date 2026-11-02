export const TRANG_THAI_ONLINE = [
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
]

export const TRANG_THAI_POS_GIAO_HANG = [
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
]

export const TRANG_THAI_POS = ['Chờ xác nhận', 'Đã hoàn thành']

export const TRANG_THAI_GIAO_THAT_BAI = [
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Giao hàng thất bại',
]

export const TRANG_THAI_KET_THUC = ['Đã hủy', 'Đã hoàn tiền']

export const nhanBuoc = (ten) => (ten === 'Đã giao hàng' ? 'Giao hàng thành công' : ten)

/**
 * Kiểm tra xem đơn hàng có phải hình thức giao hàng tận nơi không
 * (Bao gồm đơn Online và đơn Bán tại quầy chọn giao hàng).
 */
export const coGiaoHang = (invoice) => {
  if (!invoice) return false
  const loai = String(invoice.loaiDon || invoice.maLoaiDon || '').trim()
  if (loai === 'Online' || loai === 'ONLINE') return true
  if (loai.includes('Giao hàng') || loai.includes('GIAO_HANG')) return true
  if (invoice.diaChiNhanHang && invoice.diaChiNhanHang.trim().length > 0 && invoice.diaChiNhanHang.trim() !== 'Nhận tại quầy') return true
  if (Number(invoice.phiVanChuyen) > 0) return true
  return false
}

/**
 * Kiểm tra đơn có phải là đơn bán tại quầy (dù nhận ngay hay giao hàng)
 */
export const laPos = (invoice) => {
  if (!invoice) return false
  const loai = String(invoice.loaiDon || invoice.maLoaiDon || '').trim()
  return loai.includes('Tại quầy') || loai.includes('TAI_QUAY')
}

/**
 * Kiểm tra đơn bán tại quầy nhận ngay (không giao hàng)
 */
export const laPosNhanNgay = (invoice) => {
  return laPos(invoice) && !coGiaoHang(invoice)
}

/**
 * Phân biệt chính xác 3 loại đơn:
 * 1. TAI_QUAY (Tại quầy nhận ngay)
 * 2. TAI_QUAY_GIAO_HANG (Tại quầy giao hàng)
 * 3. ONLINE (Đơn online)
 */
export const loaiDonMeta = (invoice) => {
  const loai = String(invoice?.loaiDon || invoice?.maLoaiDon || '').trim()
  if (loai === 'Online' || loai === 'ONLINE') {
    return {
      code: 'ONLINE',
      label: 'Online',
      badgeClass: 'inv-type-online',
      icon: 'globe',
      isDelivery: true,
    }
  }
  if (coGiaoHang(invoice)) {
    return {
      code: 'TAI_QUAY_GIAO_HANG',
      label: 'Tại quầy (Giao hàng)',
      badgeClass: 'inv-type-pos-delivery',
      icon: 'truck',
      isDelivery: true,
    }
  }
  return {
    code: 'TAI_QUAY',
    label: 'Tại quầy',
    badgeClass: 'inv-type-pos',
    icon: 'store',
    isDelivery: false,
  }
}

export const isTerminal = (invoice) =>
  TRANG_THAI_KET_THUC.includes(invoice?.trangThai)

export const trangThaiTiepTheoOf = (invoice) => {
  if (!invoice) return []
  if (Array.isArray(invoice.trangThaiTiepTheo) && invoice.trangThaiTiepTheo.length > 0) {
    return invoice.trangThaiTiepTheo
  }
  const meta = loaiDonMeta(invoice)
  let list = TRANG_THAI_POS
  if (meta.code === 'TAI_QUAY_GIAO_HANG') {
    list = invoice.trangThai === 'Chờ xác nhận' ? TRANG_THAI_ONLINE : TRANG_THAI_POS_GIAO_HANG
  } else if (coGiaoHang(invoice)) {
    list = TRANG_THAI_ONLINE
  }
  const idx = list.indexOf(invoice.trangThai)
  if (idx === -1) return []
  return list.slice(idx + 1, idx + 2)
}

export const pillClass = (value) => {
  if (['Đã hoàn thành', 'Đã giao hàng', 'Đã xác nhận'].includes(value))
    return 'inv-pill-green'
  if (['Đã hủy', 'Đã hoàn tiền', 'Giao hàng thất bại'].includes(value)) return 'inv-pill-red'
  if (['Chờ xác nhận', 'Chờ giao hàng'].includes(value)) return 'inv-pill-amber'
  if (['Đang giao hàng'].includes(value)) return 'inv-pill-teal'
  return 'inv-pill-gray'
}
