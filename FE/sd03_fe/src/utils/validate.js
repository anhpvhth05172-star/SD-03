export const isBlank = (v) => v === null || v === undefined || String(v).trim() === ''

export const batBuoc = (v, label) => (isBlank(v) ? `${label} không được để trống` : '')

export const toiDa = (v, max, label) =>
  !isBlank(v) && String(v).trim().length > max ? `${label} không được vượt quá ${max} ký tự` : ''

export const emailHopLe = (v) => {
  if (isBlank(v)) return ''
  return /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(String(v).trim()) ? '' : 'Email không hợp lệ'
}

export const soDienThoaiHopLe = (v) => {
  if (isBlank(v)) return ''
  return /^0\d{8,9}$/.test(String(v).trim())
    ? ''
    : 'Số điện thoại không hợp lệ (bắt đầu bằng 0, gồm 9-10 chữ số, không chứa chữ cái)'
}

/** Phân tích ngày theo định dạng YYYY-MM-DD (hoặc Date hợp lệ); trả về null nếu không hợp lệ. */
export const parseNgay = (v) => {
  if (isBlank(v)) return null
  const s = String(v).trim()
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(s)
  if (!m) {
    const d = new Date(s)
    return Number.isNaN(d.getTime()) ? null : d
  }
  const nam = Number(m[1])
  const thang = Number(m[2])
  const ngay = Number(m[3])
  const d = new Date(nam, thang - 1, ngay)
  if (d.getFullYear() !== nam || d.getMonth() !== thang - 1 || d.getDate() !== ngay) return null
  return d
}

/**
 * Tuổi thực tế tại ngày hiện tại: lấy hiệu năm rồi trừ 1 nếu chưa tới sinh nhật.
 * Trả về null khi ngày sinh không hợp lệ.
 */
export const tinhTuoi = (ngaySinh, homNay = new Date()) => {
  const d = parseNgay(ngaySinh)
  if (!d) return null
  let tuoi = homNay.getFullYear() - d.getFullYear()
  const chuaDenSinhNham =
    homNay.getMonth() < d.getMonth() ||
    (homNay.getMonth() === d.getMonth() && homNay.getDate() < d.getDate())
  if (chuaDenSinhNham) tuoi -= 1
  return tuoi
}

/** Giá trị max cho input ngày sinh = ngày hiện tại trừ đúng số tuổi tối thiểu. */
export const ngaySinhToiDa = (tuoiToiThieu, homNay = new Date()) => {
  const d = new Date(homNay.getFullYear() - tuoiToiThieu, homNay.getMonth(), homNay.getDate())
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/**
 * Validate ngày sinh theo tuổi tối thiểu. Trả về '' nếu hợp lệ.
 * Không kiểm tra khi ngày sinh để trống (trường bắt buộc do batBuoc đảm nhận).
 */
export const duTuoiToiThieu = (ngaySinh, tuoiToiThieu, thongBao, homNay = new Date()) => {
  if (isBlank(ngaySinh)) return ''
  const d = parseNgay(ngaySinh)
  if (!d) return 'Ngày sinh không hợp lệ'
  const dauNgay = new Date(homNay.getFullYear(), homNay.getMonth(), homNay.getDate())
  if (d > dauNgay) return 'Ngày sinh không được ở tương lai'
  const tuoi = tinhTuoi(ngaySinh, dauNgay)
  return tuoi !== null && tuoi >= tuoiToiThieu ? '' : thongBao
}

export const khongDuocTuongLai = (v, label) => {
  if (isBlank(v)) return ''
  const d = parseNgay(v)
  if (!d) return `${label} không hợp lệ`
  const homNay = new Date()
  homNay.setHours(0, 0, 0, 0)
  return d > homNay ? `${label} không được ở tương lai` : ''
}

export const matKhauHopLe = (v, required = false) => {
  if (isBlank(v)) return required ? 'Mật khẩu không được để trống' : ''
  const s = String(v)
  if (s.length < 6) return 'Mật khẩu phải có ít nhất 6 ký tự'
  if (s.length > 127) return 'Mật khẩu không được vượt quá 127 ký tự'
  return ''
}

export const chonBatBuoc = (v, label) => (isBlank(v) ? `Vui lòng chọn ${label.toLowerCase()}` : '')

export const kiemTra = (rules) => {
  for (const r of rules) {
    if (r) return r
  }
  return ''
}
