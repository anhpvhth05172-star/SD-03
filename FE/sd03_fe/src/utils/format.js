export const formatVnd = (n) =>
  new Intl.NumberFormat('vi-VN').format(Number(n) || 0) + '₫'

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
