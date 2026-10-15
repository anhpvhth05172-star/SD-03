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

const AVATAR_COLORS = ['#cc0000', '#f59e0b', '#8b5cf6', '#14b8a6', '#3b82f6', '#22c55e', '#ec4899', '#6366f1']

export const avatarColor = (id) => AVATAR_COLORS[Math.abs(Number(id) || 0) % AVATAR_COLORS.length]

export const payStatusOf = (trangThai) => {
  if (trangThai === 'Đã hoàn thành' || trangThai === 'Đã giao hàng') return 'Đã thanh toán'
  if (trangThai === 'Đã hoàn tiền') return 'Đã hoàn tiền'
  return 'Chưa thanh toán'
}

