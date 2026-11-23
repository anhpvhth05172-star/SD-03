
import { exportExcel } from '@/utils/export'

const pad2 = (x) => String(x).padStart(2, '0')

export const khoaNgay = (d) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`

export const docNgay = (s) => {
  if (!s) return null
  let m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s.trim())
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(s.trim())
  if (m) return new Date(Number(m[3]), Number(m[2]) - 1, Number(m[1]))
  return null
}

export const hienThiNgay = (khoa) => {
  const d = docNgay(khoa)
  if (!d) return '—'
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`
}

export const slugMaCa = (ten) =>
  String(ten || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/^ca\s*/, '')
    .replace(/[^a-z0-9]/g, '')

export const xuatMauLichLamViec = async () => {
  const d = new Date()
  const ngay = `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}${d.getFullYear()}`
  await exportExcel(
    'mau-lich-lam-viec.xlsx',
    ['Mã nhân viên', 'Mã ca (hoặc tên ca)', 'Ngày làm (dd/MM/yyyy)'],
    [
      ['NV0001', 'sang', ngay],
      ['NV0002', 'chieu', ngay],
      ['NV0003', 'Ca tối', ngay],
    ],
    { sheetName: 'Lịch làm việc' }
  )
}

const docFileDong = (text) =>
  text
    .replace(/\r/g, '')
    .split('\n')
    .map((d) => d.trim())
    .filter((d) => d.length > 0)

const tachCot = (dong) => {
  if (dong.includes(';')) return dong.split(';').map((c) => c.trim().replace(/^"|"$/g, ''))
  return dong.split(',').map((c) => c.trim().replace(/^"|"$/g, ''))
}

export const phanTichDongLich = (dongGoc, danhSachNhanVien, danhSachCa) => {
  const cacDong = (dongGoc || []).filter((d) =>
    Array.isArray(d)
      ? d.some((c) => String(c ?? '').trim() !== '')
      : String(d ?? '').trim() !== ''
  )
  const ketQua = { tongDong: 0, hopLe: 0, loi: [], rows: [] }
  if (!cacDong.length) {
    ketQua.loi.push({ dong: 0, lyDo: 'Tệp không có dữ liệu' })
    return ketQua
  }

  const chuDong = (d) =>
    Array.isArray(d) ? d.map((c) => String(c ?? '').trim()) : tachCot(d)
  const dauTach = chuDong(cacDong[0]).map((x) =>
    x
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
  )
  const coTieuDe = dauTach.some((x) => x.includes('ma nhan vien') || x === 'manhanvien')
  const batDau = coTieuDe ? 1 : 0

  const setMaNV = new Set((danhSachNhanVien || []).map((nv) => String(nv.maNhanVien).trim()))
  const caList = danhSachCa || []
  const timCaTheoMa = (ma) => caList.find((c) => c.ma === String(ma).trim().toLowerCase())
  const timCaTheoTen = (ten) =>
    caList.find((c) => slugMaCa(c.ten) === slugMaCa(ten))

  for (let i = batDau; i < cacDong.length; i++) {
    const soDong = i + 1
    const cot = chuDong(cacDong[i])
    ketQua.tongDong++
    const maNV = (cot[0] || '').trim()
    const caNhap = (cot[1] || '').trim()
    const ngayNhap = (cot[2] || '').trim()

    const loiDong = []
    if (!maNV) loiDong.push('thiếu mã nhân viên')
    else if (setMaNV.size && !setMaNV.has(maNV)) loiDong.push(`mã nhân viên "${maNV}" không tồn tại`)

    let ca = null
    if (!caNhap) loiDong.push('thiếu mã ca')
    else {
      ca = timCaTheoMa(caNhap) || timCaTheoTen(caNhap)
      if (!ca) loiDong.push(`mã ca "${caNhap}" không hợp lệ`)
    }

    const ngay = docNgay(ngayNhap)
    if (!ngay) loiDong.push(`ngày "${ngayNhap}" không hợp lệ (dd/MM/yyyy)`)
    const khoa = ngay ? khoaNgay(ngay) : ''

    if (loiDong.length) {
      ketQua.loi.push({ dong: soDong, lyDo: loiDong.join('; ') })
    } else {
      ketQua.hopLe++
      ketQua.rows.push({
        maNhanVien: maNV,
        tenNhanVien:
          (danhSachNhanVien || []).find((nv) => String(nv.maNhanVien) === maNV)?.tenTaiKhoan || '',
        idCa: ca.id,
        ngay: khoa,
      })
    }
  }
  return ketQua
}

export const phanTichCsvLich = (text, danhSachNhanVien, danhSachCa) =>
  phanTichDongLich(docFileDong(text).map(tachCot), danhSachNhanVien, danhSachCa)
