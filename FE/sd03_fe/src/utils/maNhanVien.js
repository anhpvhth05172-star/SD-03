export const TOI_DA = 25

export const boDau = (s) =>
  String(s || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')

/**
 * Mã phải bắt đầu bằng chữ cái (đúng hopLeMaNhanVien). Tên gọi cuối là chuỗi số
 * sẽ sinh mã bắt đầu bằng số -> thêm tiền tố "NV" (khớp backend MaNhanVien.sinh).
 */
const dauLaChuCai = (ma) => (/^[A-Za-z]/.test(ma) ? ma : `NV${ma}`)

/**
 * Phần gốc (chưa cộng số) của mã sinh từ họ tên:
 * tên gọi cuối viết hoa chữ đầu + chữ cái đầu của họ và tên đệm viết hoa.
 * Ví dụ: "Phạm Vũ Hoàng Anh" -> "AnhPVH".
 */
export const sinhMaTuTen = (ten) => {
  const goc = String(ten || '').trim().replace(/\s+/g, ' ')
  if (!goc) return 'NV'
  const parts = goc.split(' ')
  const cuoiRaw = boDau(parts[parts.length - 1]).toLowerCase()
  const cuoi = cuoiRaw ? cuoiRaw.charAt(0).toUpperCase() + cuoiRaw.slice(1) : ''
  if (parts.length === 1) return cat(dauLaChuCai(cuoi))
  const dau = parts
    .slice(0, -1)
    .map((p) => boDau(p))
    .filter(Boolean)
    .map((p) => p.charAt(0).toUpperCase())
    .join('')
  return cat(dauLaChuCai(cuoi + dau))
}

/** Tương thích với tên gọi cũ. */
export const sinhMaNhanVien = sinhMaTuTen

/**
 * Cộng số thứ tự: từ 2 trở lên cộng trực tiếp số (2, 3, ...).
 * Nếu số <= 1, giữ nguyên phần gốc. Kết quả luôn nằm trong TOI_DA ký tự.
 */
export const congSo = (goc, so) => {
  const thuTu = Number(so) || 1
  if (thuTu <= 1) return String(goc || '').trim() ? cat(String(goc).trim()) : 'NV'
  const hauTo = String(thuTu)
  const toiDaGoc = Math.max(1, TOI_DA - hauTo.length)
  const gocCat = String(goc || '').trim() ? cat(String(goc).trim(), toiDaGoc) : 'NV'
  return gocCat + hauTo
}

/**
 * Mã đề xuất theo họ tên: dùng phần gốc khi chưa có mã trùng (Phạm Vũ Hoàng Anh -> AnhPVH).
 * Khi mã gốc đã tồn tại, backend tự trả về mã kế tiếp (AnhPVH2, AnhPVH3, ...).
 */
export const maDeXuat = (ten) => sinhMaTuTen(ten)

export const hopLeMaNhanVien = (ma) => /^[A-Za-z][A-Za-z0-9]{0,24}$/.test(String(ma || '').trim())

export const hopLeMa = hopLeMaNhanVien

const cat = (s) => (s.length <= TOI_DA ? s : s.slice(0, TOI_DA))
