export const TOI_DA = 25

export const boDau = (s) =>
  String(s || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')

/**
 * Phần gốc (chưa cộng số) của mã sinh từ họ tên:
 * tên gọi cuối viết hoa chữ đầu + chữ cái đầu của họ và tên đệm viết thường.
 * Ví dụ: "Nguyễn Thị Bình" -> "Binhnt".
 */
export const sinhMaTuTen = (ten) => {
  const goc = String(ten || '').trim().replace(/\s+/g, ' ')
  if (!goc) return 'NV'
  const parts = goc.split(' ')
  const cuoiRaw = boDau(parts[parts.length - 1]).toLowerCase()
  const cuoi = cuoiRaw ? cuoiRaw.charAt(0).toUpperCase() + cuoiRaw.slice(1) : ''
  if (parts.length === 1) return cat(cuoi)
  const dau = parts
    .slice(0, -1)
    .map((p) => boDau(p).toLowerCase())
    .filter(Boolean)
    .map((p) => p.charAt(0))
    .join('')
  return cat(cuoi + dau)
}

/** Tương thích với tên gọi cũ. */
export const sinhMaNhanVien = sinhMaTuTen

/**
 * Cộng số thứ tự: 01..99 (đúng 2 chữ số), từ 100 trở lên giữ nguyên số chữ số.
 * Kết quả luôn nằm trong TOI_DA ký tự.
 */
export const congSo = (goc, so) => {
  const thuTu = Math.max(Number(so) || 1, 1)
  const hauTo = thuTu < 100 ? String(thuTu).padStart(2, '0') : String(thuTu)
  const toiDaGoc = Math.max(1, TOI_DA - hauTo.length)
  const gocCat = String(goc || '').trim() ? cat(String(goc).trim(), toiDaGoc) : 'NV'
  return gocCat + hauTo
}

/** Mã đề xuất theo họ tên (số 01). Backend là nơi quyết định mã cuối cùng. */
export const maDeXuat = (ten) => congSo(sinhMaTuTen(ten), 1)

export const hopLeMaNhanVien = (ma) => /^[A-Za-z][A-Za-z0-9]{0,24}$/.test(String(ma || '').trim())

export const hopLeMa = hopLeMaNhanVien

const cat = (s) => (s.length <= TOI_DA ? s : s.slice(0, TOI_DA))
