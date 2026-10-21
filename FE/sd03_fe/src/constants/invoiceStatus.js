export const TRANG_THAI_ONLINE = [
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
]

export const TRANG_THAI_POS = ['Chờ xác nhận', 'Đã hoàn thành']

export const TRANG_THAI_KET_THUC = ['Đã hủy', 'Đã hoàn tiền']

export const laPos = (invoice) => {
  const loaiDon = invoice?.loaiDon
  return loaiDon === 'Tại quầy' || loaiDon === 'TAI_QUAY'
}

export const isTerminal = (invoice) =>
  TRANG_THAI_KET_THUC.includes(invoice?.trangThai)

export const trangThaiTiepTheoOf = (invoice) => {
  if (!invoice) return []
  if (Array.isArray(invoice.trangThaiTiepTheo)) return invoice.trangThaiTiepTheo
  const list = laPos(invoice) ? TRANG_THAI_POS : TRANG_THAI_ONLINE
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
