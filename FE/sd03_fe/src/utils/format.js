export const formatVnd = (n) =>
  new Intl.NumberFormat('vi-VN').format(Number(n) || 0) + ' ₫'

export const formatCurrency = (n) =>
  new Intl.NumberFormat('vi-VN').format(Number(n) || 0) + ' ₫'

export const formatNumber = (n) =>
  new Intl.NumberFormat('vi-VN').format(Number(n) || 0)

export const parseNumber = (val) => {
  if (val === null || val === undefined || val === '') return 0
  if (typeof val === 'number') return val
  const cleaned = String(val).replace(/[^\d]/g, '')
  return cleaned ? parseInt(cleaned, 10) : 0
}

export const removeVietnameseTones = (str) => {
  if (!str) return ''
  let s = String(str)
  s = s.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, 'a')
  s = s.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, 'e')
  s = s.replace(/ì|í|ị|ỉ|ĩ/g, 'i')
  s = s.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, 'o')
  s = s.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, 'u')
  s = s.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, 'y')
  s = s.replace(/đ/g, 'd')
  s = s.replace(/À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ/g, 'A')
  s = s.replace(/È|É|Ẹ|Ẻ|Ẽ|Ê|Ề|Ế|Ệ|Ể|Ễ/g, 'E')
  s = s.replace(/Ì|Í|Ị|Ỉ|Ĩ/g, 'I')
  s = s.replace(/Ò|Ó|Ọ|Ỏ|Õ|Ô|Ồ|Ố|Ộ|Ổ|Ỗ|Ơ|Ờ|Ớ|Ợ|Ở|Ỡ/g, 'O')
  s = s.replace(/Ù|Ú|Ụ|Ủ|Ũ|Ư|Ừ|Ứ|Ự|Ử|Ữ/g, 'U')
  s = s.replace(/Ỳ|Ý|Ỵ|Ỷ|Ỹ/g, 'Y')
  s = s.replace(/Đ/g, 'D')
  return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()
}

export const formatDateTime = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())} - ${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()}`
}

export const formatDate = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()}`
}

export const initialsOf = (name) => {
  if (!name) return 'HD'
  const parts = name.trim().split(/\s+/)
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

const AVATAR_COLORS = ['#dc2626', '#f59e0b', '#8b5cf6', '#14b8a6', '#3b82f6', '#22c55e', '#ec4899', '#6366f1']

export const avatarColor = (id) => AVATAR_COLORS[Math.abs(Number(id) || 0) % AVATAR_COLORS.length]

export const payStatusOf = (target, optionalInvoice = null) => {
  const invoice = (typeof target === 'object' && target !== null) ? target : optionalInvoice
  const trangThai = (typeof target === 'object' && target !== null) ? target.trangThai : target

  if (trangThai === 'Đã hoàn tiền') return 'Đã hoàn tiền'
  if (trangThai === 'Đã hoàn thành' || trangThai === 'Đã giao hàng') return 'Đã thanh toán'
  if (trangThai === 'Đã hủy') return 'Chưa thanh toán'

  if (invoice) {
    const loaiDon = String(invoice.loaiDon || invoice.maLoaiDon || '').toUpperCase()
    const pttt = String(invoice.tenPhuongThucThanhToan || invoice.phuongThucThanhToan?.ten || invoice.tenPhuongThuc || '').toUpperCase()
    const maPttt = String(invoice.maPhuongThucThanhToan || invoice.phuongThucThanhToan?.ma || '').toUpperCase()
    const idPttt = Number(invoice.idPhuongThucThanhToan || invoice.phuongThucThanhToan?.id || 0)

    const isVnPay = pttt.includes('VNPAY') || maPttt.includes('VNPAY') || idPttt === 4
    const isChuyenKhoan = pttt.includes('CHUYỂN KHOẢN') || pttt.includes('CHUYEN_KHOAN') || maPttt.includes('CHUYEN_KHOAN') || idPttt === 2
    const isCashCod = pttt.includes('COD') || pttt.includes('KHI NHẬN HÀNG') || pttt.includes('TIỀN MẶT') || maPttt.includes('COD') || maPttt.includes('TM') || idPttt === 11 || idPttt === 1 || idPttt === 19

    // Đơn giao hàng tận nơi
    const hasDelivery = Boolean(
      (invoice.diaChiNhanHang && invoice.diaChiNhanHang.trim().length > 0 && invoice.diaChiNhanHang.trim() !== 'Nhận tại quầy')
      || Number(invoice.phiVanChuyen) > 0
      || loaiDon.includes('GIAO_HANG') || loaiDon.includes('GIAO HÀNG')
      || loaiDon === 'ONLINE'
    )

    // 1. Đơn chuyển khoản hoặc VNPay (trả trước)
    if ((isVnPay || isChuyenKhoan) && !isCashCod) {
      if (trangThai !== 'Chờ thanh toán' && trangThai !== 'CHO_THANH_TOAN') {
        return 'Đã thanh toán'
      }
      return 'Chưa thanh toán'
    }

    // 2. Đơn có giao hàng mà chọn Tiền mặt (COD): khách nhận hàng mới trả tiền
    // Chỉ khi đã giao hàng thành công (Đã giao hàng / Đã hoàn thành) mới là Đã thanh toán
    if (hasDelivery && isCashCod) {
      if (trangThai === 'Đã giao hàng' || trangThai === 'Đã hoàn thành') {
        return 'Đã thanh toán'
      }
      return 'Chưa thanh toán'
    }

    // 3. Đơn bán tại quầy nhận ngay (không giao hàng): khách trả tiền mặt lấy hàng ngay
    const isPos = loaiDon.includes('TẠI QUẦY') || loaiDon.includes('TAI_QUAY')
    if (isPos && !hasDelivery) {
      return 'Đã thanh toán'
    }
  }

  return 'Chưa thanh toán'
}

export const formatTimeAgo = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const now = new Date()
  const diffSec = Math.floor((now.getTime() - d.getTime()) / 1000)
  if (diffSec < 0 || diffSec < 60) return 'Vừa xong'
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin} phút trước`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} giờ trước`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay} ngày trước`
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())} - ${p(d.getDate())}/${p(d.getMonth() + 1)}`
}

