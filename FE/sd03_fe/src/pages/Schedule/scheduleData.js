/** Cache giao diện cho dữ liệu ca và lịch được lưu trên backend. */
import { computed, reactive } from 'vue'
import { exportExcel } from '@/utils/export'
import {
  createScheduleShift,
  createWorkSchedule,
  deleteScheduleShift,
  deleteWorkSchedule,
  deleteWorkSchedulesForDate,
  listScheduleShifts,
  listWorkSchedules,
  toggleScheduleShift,
  updateScheduleShift,
  updateWorkSchedule,
} from '@/api/schedule'

export const danhSachCa = reactive([])

export const caDangBat = computed(() => danhSachCa.filter((c) => c.dangBat).sort((a, b) => a.batDau.localeCompare(b.batDau)))

export const danhSachLich = reactive([])

const chuanHoaCa = (ca) => ({
  ...ca,
  id: Number(ca.id),
  batDau: String(ca.batDau || '').slice(0, 5),
  ketThuc: String(ca.ketThuc || '').slice(0, 5),
})

const chuanHoaLich = (lich) => ({ ...lich, id: Number(lich.id), idCa: Number(lich.idCa) })

export const napDuLieuLich = async () => {
  const [ca, lich] = await Promise.all([listScheduleShifts(), listWorkSchedules()])
  danhSachCa.splice(0, danhSachCa.length, ...ca.map(chuanHoaCa))
  danhSachLich.splice(0, danhSachLich.length, ...lich.map(chuanHoaLich))
}

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

export const themLich = async (item) => {
  const row = chuanHoaLich(await createWorkSchedule({
    maNhanVien: item.maNhanVien,
    idCa: Number(item.idCa),
    ngay: item.ngay,
  }))
  danhSachLich.push(row)
  return row
}

export const capNhatLich = async (id, item) => {
  const idx = danhSachLich.findIndex((l) => l.id === id)
  if (idx === -1) return null
  danhSachLich[idx] = chuanHoaLich(await updateWorkSchedule(id, {
    maNhanVien: item.maNhanVien,
    idCa: Number(item.idCa),
    ngay: item.ngay,
  }))
  return danhSachLich[idx]
}

export const xoaLich = async (id) => {
  const idx = danhSachLich.findIndex((l) => l.id === id)
  if (idx === -1) return false
  await deleteWorkSchedule(id)
  danhSachLich.splice(idx, 1)
  return true
}

export const xoaLichTheoNgay = async (ngay) => {
  await deleteWorkSchedulesForDate(ngay)
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
export const xepCaTuDong = async (ngay, danhSachNhanVien) => {
  const caList = caDangBat.value
  if (!ngay || !caList.length) return { themMoi: 0, lyDo: 'khongCa' }
  const daCo = new Set(lichCuaNgay(ngay).map((l) => l.maNhanVien))
  const ungVien = (danhSachNhanVien || []).filter((nv) => nv.trangThai !== false && !daCo.has(nv.maNhanVien))
  if (!ungVien.length) return { themMoi: 0, lyDo: 'khongUngVien' }
  const rows = ungVien.map((nv, i) => {
    const ca = caList[i % caList.length]
    return {
      maNhanVien: nv.maNhanVien,
      tenNhanVien: nv.tenTaiKhoan || nv.ten || '',
      idCa: ca.id,
      ngay,
    }
  })
  for (const row of rows) await themLich(row)
  return { themMoi: rows.length, lyDo: 'ok' }
}

/* ---------------------------------- Ca làm ---------------------------------- */
export const timCaById = (id) => danhSachCa.find((c) => c.id === Number(id))

export const themCa = async (item) => {
  const row = chuanHoaCa(await createScheduleShift({
    ma: item.ma,
    ten: item.ten,
    batDau: item.batDau,
    ketThuc: item.ketThuc,
  }))
  danhSachCa.push(row)
  return row
}

export const capNhatCa = async (id, item) => {
  const idx = danhSachCa.findIndex((c) => c.id === Number(id))
  if (idx === -1) return null
  danhSachCa[idx] = chuanHoaCa(await updateScheduleShift(id, {
    ma: item.ma,
    ten: item.ten,
    batDau: item.batDau,
    ketThuc: item.ketThuc,
  }))
  return danhSachCa[idx]
}

export const doiTrangThaiCa = async (id) => {
  const ca = timCaById(id)
  if (!ca) return null
  const updated = chuanHoaCa(await toggleScheduleShift(id))
  const idx = danhSachCa.findIndex((row) => row.id === Number(id))
  danhSachCa[idx] = updated
  return updated
}

export const caTrungTen = (ten, boQuaId = null) =>
  danhSachCa.some(
    (c) => c.id !== Number(boQuaId) && slugMaCa(c.ten) === slugMaCa(ten)
  )

export const soLichCuaCa = (id) =>
  danhSachLich.filter((l) => l.idCa === Number(id)).length

export const xoaCa = async (id) => {
  const idx = danhSachCa.findIndex((c) => c.id === Number(id))
  if (idx === -1) return false
  await deleteScheduleShift(id)
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

export const nhapRowsVaoStore = async (rows) => {
  for (const row of rows) await themLich({ ...row })
  return rows.length
}
