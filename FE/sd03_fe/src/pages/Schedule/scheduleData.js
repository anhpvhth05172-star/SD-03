/**
 * Store dữ liệu cho màn Quản lý lịch làm việc & Lịch ca làm.
 *
 * PHẦN CHƯA ĐƯỢC KẾT NỐI: backend hiện chưa có bảng/API cho lịch làm việc
 * và ca làm, nên toàn bộ dữ liệu dưới đây nằm trong bộ nhớ phía client
 * (reload trang sẽ trở về dữ liệu mẫu). Khi có API, chỉ cần thay các hàm
 * CRUD ở đây bằng lời gọi HTTP.
 */
import { computed, reactive } from 'vue'
import { exportExcel } from '@/utils/export'

export const nhanVienMau = [
  { id: 1, maNhanVien: 'NV0001', tenTaiKhoan: 'Nguyễn Quốc Huy', tenVaiTro: 'Quản trị viên', maVaiTro: 'ADMIN' },
  { id: 2, maNhanVien: 'NV0002', tenTaiKhoan: 'Lê Minh Châu', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 3, maNhanVien: 'NV0003', tenTaiKhoan: 'Lê Hoàng Nam', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 4, maNhanVien: 'NV0004', tenTaiKhoan: 'Phạm Thu Trang', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 5, maNhanVien: 'NV0005', tenTaiKhoan: 'Võ Đức Long', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 6, maNhanVien: 'NV0006', tenTaiKhoan: 'Đặng Ngọc Mai', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 7, maNhanVien: 'NV0007', tenTaiKhoan: 'Bùi Gia Hân', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 8, maNhanVien: 'NV0008', tenTaiKhoan: 'Đỗ Thành Công', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 9, maNhanVien: 'NV0009', tenTaiKhoan: 'Hồ Quang Minh', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 10, maNhanVien: 'NV0010', tenTaiKhoan: 'Trần Thị Bích Ngọc', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 11, maNhanVien: 'NV0011', tenTaiKhoan: 'Dương Tuấn Kiệt', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
  { id: 12, maNhanVien: 'NV0012', tenTaiKhoan: 'Vũ Khánh Linh', tenVaiTro: 'Nhân viên bán hàng', maVaiTro: 'STAFF' },
]

export const danhSachCa = reactive([
  { id: 1, ma: 'sang', ten: 'Ca sáng', batDau: '08:00', ketThuc: '12:00', dangBat: true },
  { id: 2, ma: 'chieu', ten: 'Ca chiều', batDau: '13:00', ketThuc: '17:00', dangBat: true },
  { id: 3, ma: 'toi', ten: 'Ca tối', batDau: '17:30', ketThuc: '21:30', dangBat: true },
])

export const caDangBat = computed(() => danhSachCa.filter((c) => c.dangBat))

export const danhSachLich = reactive([])

let nextId = 1
export const taoId = () => nextId++

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

/* ---------------------------------- Dữ liệu mẫu ---------------------------------- */
const taoLichMau = () => {
  const hom = new Date()
  hom.setHours(0, 0, 0, 0)
  for (let i = -7; i <= 7; i++) {
    const d = new Date(hom)
    d.setDate(d.getDate() + i)
    const thu = d.getDay()
    if (thu === 0) continue
    const khoa = khoaNgay(d)
    const buoi = i + 7
    const nomsang = [(0 + buoi) % 12, (1 + buoi) % 12, (2 + buoi) % 12]
    const nomchieu = [(3 + buoi) % 12, (4 + buoi) % 12, (5 + buoi) % 12]
    const nomtoi = [(6 + buoi) % 12, (7 + buoi) % 12, (8 + buoi) % 12]
    const them = (idCa, idxs) =>
      idxs.forEach((idx) => {
        const nv = nhanVienMau[idx]
        danhSachLich.push({
          id: taoId(),
          maNhanVien: nv.maNhanVien,
          tenNhanVien: nv.tenTaiKhoan,
          idCa,
          ngay: khoa,
        })
      })
    them(1, nomsang)
    them(2, nomchieu)
    if (buoi % 5 !== 4) them(3, nomtoi)
  }
}
taoLichMau()

/* ---------------------------------- CRUD lịch ---------------------------------- */
export const timCa = (id) => danhSachCa.find((c) => c.id === Number(id))

export const timCaTheoMa = (ma) =>
  danhSachCa.find((c) => c.ma === String(ma).trim().toLowerCase())

export const timCaTheoTen = (ten) =>
  danhSachCa.find((c) => slugMaCa(c.ten) === slugMaCa(ten))

export const kiemTraTrung = (maNhanVien, ngay, idCa, boQuaId = null) =>
  danhSachLich.some(
    (l) =>
      l.id !== boQuaId &&
      l.maNhanVien === maNhanVien &&
      l.ngay === ngay &&
      l.idCa === Number(idCa)
  )

export const themLich = (item) => {
  const row = { id: taoId(), ...item }
  danhSachLich.push(row)
  return row
}

export const capNhatLich = (id, item) => {
  const idx = danhSachLich.findIndex((l) => l.id === id)
  if (idx === -1) return null
  danhSachLich[idx] = { ...danhSachLich[idx], ...item }
  return danhSachLich[idx]
}

export const xoaLich = (id) => {
  const idx = danhSachLich.findIndex((l) => l.id === id)
  if (idx === -1) return false
  danhSachLich.splice(idx, 1)
  return true
}

export const xoaLichTheoNgay = (ngay) => {
  for (let i = danhSachLich.length - 1; i >= 0; i--) {
    if (danhSachLich[i].ngay === ngay) danhSachLich.splice(i, 1)
  }
}

export const lichCuaNgay = (ngay) => danhSachLich.filter((l) => l.ngay === ngay)

export const lichCuaCaNgay = (idCa, ngay) =>
  danhSachLich.filter((l) => l.idCa === Number(idCa) && l.ngay === ngay)

/**
 * Xếp ca tự động cho một ngày: lấy nhân viên chưa có lịch ngày đó,
 * chia tròn đều vào các ca đang bật.
 */
export const xepCaTuDong = (ngay, danhSachNhanVien) => {
  const caList = caDangBat.value
  if (!ngay || !caList.length) return { themMoi: 0, lyDo: 'khongCa' }
  const daCo = new Set(lichCuaNgay(ngay).map((l) => l.maNhanVien))
  const ungVien = (danhSachNhanVien || []).filter((nv) => !daCo.has(nv.maNhanVien))
  if (!ungVien.length) return { themMoi: 0, lyDo: 'khongUngVien' }
  let them = 0
  ungVien.forEach((nv, i) => {
    const ca = caList[i % caList.length]
    themLich({
      maNhanVien: nv.maNhanVien,
      tenNhanVien: nv.tenTaiKhoan || nv.ten || '',
      idCa: ca.id,
      ngay,
    })
    them++
  })
  return { themMoi: them, lyDo: 'ok' }
}

/* ---------------------------------- Ca làm ---------------------------------- */
let nextCaId = 4
export const taoIdCa = () => nextCaId++

export const timCaById = (id) => danhSachCa.find((c) => c.id === Number(id))

export const themCa = (item) => {
  const row = { id: taoIdCa(), dangBat: true, ...item }
  danhSachCa.push(row)
  return row
}

export const capNhatCa = (id, item) => {
  const idx = danhSachCa.findIndex((c) => c.id === Number(id))
  if (idx === -1) return null
  danhSachCa[idx] = { ...danhSachCa[idx], ...item }
  return danhSachCa[idx]
}

export const doiTrangThaiCa = (id) => {
  const ca = timCaById(id)
  if (ca) ca.dangBat = !ca.dangBat
  return ca
}

export const caTrungTen = (ten, boQuaId = null) =>
  danhSachCa.some(
    (c) => c.id !== Number(boQuaId) && slugMaCa(c.ten) === slugMaCa(ten)
  )

export const soLichCuaCa = (id) =>
  danhSachLich.filter((l) => l.idCa === Number(id)).length

export const xoaCa = (id) => {
  const idx = danhSachCa.findIndex((c) => c.id === Number(id))
  if (idx === -1) return false
  danhSachCa.splice(idx, 1)
  return true
}

/* ---------------------------------- Nhập / xuất Excel ---------------------------------- */
export const xuatMauLichLamViec = async () => {
  const d = new Date()
  const ngay = `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`
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

/**
 * Phân tích + kiểm tra dữ liệu nhập lịch làm việc từ các DÒNG ĐÃ TÁCH CỘT
 * (mỗi phần tử là một mảng ô riêng — đọc theo từng ô, không gộp cả dòng thành chuỗi).
 * Trả về { tongDong, hopLe, loi: [{ dong, lyDo }], rows: [...] }
 */
export const phanTichDongLich = (dongGoc, danhSachNhanVien) => {
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

    if (!loiDong.length && kiemTraTrung(maNV, khoa, ca.id)) {
      loiDong.push('trùng lịch làm việc')
    }

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

/** Nhận tệp CSV (chuỗi) → tách dòng/cột rồi phân tích. */
export const phanTichCsvLich = (text, danhSachNhanVien) =>
  phanTichDongLich(docFileDong(text).map(tachCot), danhSachNhanVien)

export const nhapRowsVaoStore = (rows) => {
  rows.forEach((r) => themLich({ ...r }))
  return rows.length
}
