/**
 * Dữ liệu minh họa cho màn Thống kê.
 *
 * PHẦN CHƯA ĐƯỢC KẾT NỐI: hệ thống chưa có API thống kê phía backend,
 * nên toàn bộ số liệu dưới đây được sinh ngẫu nhiên có trộn (seed cố định)
 * ngay trên client để trình bày giao diện. Khi có API thật, chỉ cần thay
 * các hàm tính toán ở đây bằng lời gọi HTTP tương ứng.
 */

const SEED_GOC = 20260816
const SO_NGAY_LUU = 420

export const TRANG_THAI_DON = [
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
  'Đã hủy',
  'Đã hoàn tiền',
]

export const MAU_TRANG_THAI = [0.07, 0.1, 0.06, 0.04, 0.05, 0.58, 0.07, 0.03]

export const MAU_THUONG_HIEU = [
  { ten: 'New Balance' },
  { ten: 'Nike' },
  { ten: 'Fila' },
  { ten: 'Adidas' },
  { ten: 'Asics' },
]

export const SAN_PHAM_MAU = [
  { maSanPham: 'G57664', tenSanPham: 'New Balance 530', tenThuongHieu: 'New Balance', giaBan: 2516000, soLuong: 588 },
  { maSanPham: 'G86428', tenSanPham: 'Air Jordan 1 Low G', tenThuongHieu: 'Nike', giaBan: 1700000, soLuong: 394 },
  { maSanPham: 'SP10', tenSanPham: 'Fila Disruptor 2', tenThuongHieu: 'Fila', giaBan: 2350000, soLuong: 299 },
  { maSanPham: 'SP02', tenSanPham: 'Adidas Ultraboost 22', tenThuongHieu: 'Adidas', giaBan: 977000, soLuong: 295 },
  { maSanPham: 'SP06', tenSanPham: 'Asics Gel-Kayano 29', tenThuongHieu: 'Asics', giaBan: 1650000, soLuong: 300 },
  { maSanPham: 'SP14', tenSanPham: 'Nike Court Vision Low', tenThuongHieu: 'Nike', giaBan: 1290000, soLuong: 260 },
  { maSanPham: 'SP21', tenSanPham: 'New Balance 9060', tenThuongHieu: 'New Balance', giaBan: 3290000, soLuong: 180 },
  { maSanPham: 'SP33', tenSanPham: 'Adidas Stan Smith', tenThuongHieu: 'Adidas', giaBan: 1490000, soLuong: 240 },
]

const mulberry32 = (a) => () => {
  a |= 0
  a = (a + 0x6d2b79f5) | 0
  let t = Math.imul(a ^ (a >>> 15), 1 | a)
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296
}

const hashStr = (s) => {
  let h = 2166136261
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

const pad2 = (x) => String(x).padStart(2, '0')

export const khoaNgay = (d) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`

export const docKhoa = (k) => {
  const [y, m, d] = k.split('-').map(Number)
  return new Date(y, m - 1, d)
}

const congNgay = (d, n) => {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}

export const dauNgay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())

export const cuoiNgay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate(), 23, 59, 59, 999)

/** Sinh bảng số liệu theo từng ngày trong SO_NGAY_LUU ngày gần nhất. */
export const taoBangNgay = (hienTai = new Date()) => {
  const map = new Map()
  for (let i = SO_NGAY_LUU; i >= 0; i--) {
    const d = congNgay(hienTai, -i)
    d.setHours(0, 0, 0, 0)
    const key = khoaNgay(d)
    const rng = mulberry32(hashStr(key) ^ SEED_GOC)
    const thu = d.getDay()
    const heSoTuan = thu === 0 ? 1.2 : thu === 6 ? 1.35 : thu === 5 ? 1.12 : thu === 1 ? 0.9 : 1
    const viTri = (SO_NGAY_LUU - i) / SO_NGAY_LUU
    const heSoXuHuong = 0.78 + viTri * 0.44
    let soDon = Math.max(0, Math.round((rng() * 4 + 1) * heSoTuan * heSoXuHuong))
    if (rng() < 0.1) soDon += Math.round(rng() * 5)
    const aov = Math.round((350000 + rng() * 2400000) / 10000) * 10000
    const doanhThu = soDon * aov
    const tienChuyenKhoan = Math.round((doanhThu * (0.45 + rng() * 0.3)) / 10000) * 10000
    const tienMat = doanhThu - tienChuyenKhoan
    const sanPhamBan = soDon === 0 ? 0 : soDon + Math.round(rng() * soDon * 1.4)
    const khachMoi = Math.max(0, Math.round(soDon * (0.2 + rng() * 0.4)))
    const soTra = rng() < 0.16 ? Math.round(rng() * 2) : 0
    map.set(key, {
      ngay: key,
      soDon,
      doanhThu,
      tienMat,
      tienChuyenKhoan,
      sanPhamBan,
      khachMoi,
      soTra,
    })
  }
  return map
}

/** Khoảng ngày nhanh (Hôm nay, 7 ngày qua, Tháng này, Tháng trước, Năm nay). */
export const khoangNhanh = (loai, hienTai = new Date()) => {
  const nay = dauNgay(hienTai)
  switch (loai) {
    case 'homNay':
      return { tu: nay, den: nay }
    case '7ngay':
      return { tu: congNgay(nay, -6), den: nay }
    case 'thangNay':
      return { tu: new Date(nay.getFullYear(), nay.getMonth(), 1), den: nay }
    case 'thangTruoc': {
      const tu = new Date(nay.getFullYear(), nay.getMonth() - 1, 1)
      const den = new Date(nay.getFullYear(), nay.getMonth(), 0)
      return { tu, den }
    }
    case 'namNay':
      return { tu: new Date(nay.getFullYear(), 0, 1), den: nay }
    default:
      return { tu: congNgay(nay, -6), den: nay }
  }
}

const danhSachKhoa = (tu, den) => {
  const out = []
  let d = dauNgay(tu)
  const end = dauNgay(den)
  let dem = 0
  while (d <= end && dem < 800) {
    out.push(khoaNgay(d))
    d = congNgay(d, 1)
    dem++
  }
  return out
}

const lamTron = (v) => Math.round(v)

/** Tổng hợp một khoảng ngày. boiSo dùng để lọc theo thương hiệu (tỉ trọng). */
export const tinhKhoang = (bang, tu, den, boiSo = 1) => {
  const r = {
    soDon: 0,
    doanhThu: 0,
    tienMat: 0,
    tienChuyenKhoan: 0,
    sanPhamBan: 0,
    khachMoi: 0,
    soTra: 0,
    soNgay: 0,
  }
  danhSachKhoa(tu, den).forEach((k) => {
    const x = bang.get(k)
    if (!x) return
    r.soNgay++
    r.soDon += x.soDon
    r.doanhThu += x.doanhThu
    r.tienMat += x.tienMat
    r.tienChuyenKhoan += x.tienChuyenKhoan
    r.sanPhamBan += x.sanPhamBan
    r.khachMoi += x.khachMoi
    r.soTra += x.soTra
  })
  if (boiSo !== 1) {
    r.soDon = lamTron(r.soDon * boiSo)
    r.doanhThu = lamTron(r.doanhThu * boiSo)
    r.tienMat = lamTron(r.tienMat * boiSo)
    r.tienChuyenKhoan = lamTron(r.tienChuyenKhoan * boiSo)
    r.sanPhamBan = lamTron(r.sanPhamBan * boiSo)
    r.khachMoi = lamTron(r.khachMoi * boiSo)
    r.soTra = lamTron(r.soTra * boiSo)
  }
  r.aov = r.soDon > 0 ? Math.round(r.doanhThu / r.soDon) : 0
  return r
}

const nhanNgay = (d) => `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}`
const nhanThang = (d) => `${pad2(d.getMonth() + 1)}/${d.getFullYear()}`

/** Chuỗi dữ liệu biểu đồ "Sản phẩm bán được theo ngày" (kèm kiểu tổng hợp). */
export const tinhChuoiBanHang = (bang, tu, den, kieu = 'ngay', boiSo = 1) => {
  const cacNgay = danhSachKhoa(tu, den).map((k) => bang.get(k)).filter(Boolean)
  if (!cacNgay.length) return []

  if (kieu === 'thang') {
    const nhom = new Map()
    cacNgay.forEach((x) => {
      const d = docKhoa(x.ngay)
      const k = `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`
      if (!nhom.has(k)) nhom.set(k, { nhan: nhanThang(d), giaTri: 0 })
      nhom.get(k).giaTri += x.sanPhamBan
    })
    return [...nhom.values()].map((v) => ({ ...v, giaTri: lamTron(v.giaTri * boiSo) }))
  }

  if (kieu === 'tuan') {
    const nhom = new Map()
    cacNgay.forEach((x) => {
      const d = docKhoa(x.ngay)
      const thu = (d.getDay() + 6) % 7
      const dauTuan = congNgay(d, -thu)
      const k = khoaNgay(dauTuan)
      if (!nhom.has(k)) nhom.set(k, { nhan: nhanNgay(dauTuan), giaTri: 0 })
      nhom.get(k).giaTri += x.sanPhamBan
    })
    return [...nhom.values()].map((v) => ({ ...v, giaTri: lamTron(v.giaTri * boiSo) }))
  }

  return cacNgay.map((x) => ({
    nhan: nhanNgay(docKhoa(x.ngay)),
    giaTri: lamTron(x.sanPhamBan * boiSo),
  }))
}

/** Bảng "Tổng quan theo chu kỳ thời gian" (Hôm nay / Tuần này / Tháng này / Năm nay). */
export const tinhChuKy = (bang, hienTai = new Date()) => {
  const nay = dauNgay(hienTai)
  const tinhMoc = (nhan, tu, den, tuTruoc, denTruoc) => {
    const cur = tinhKhoang(bang, tu, den)
    const prev = tinhKhoang(bang, tuTruoc, denTruoc)
    let tang = 0
    if (prev.doanhThu <= 0) tang = cur.doanhThu > 0 ? 100 : 0
    else tang = Math.round(((cur.doanhThu - prev.doanhThu) / prev.doanhThu) * 100)
    return {
      nhan,
      doanhThu: cur.doanhThu,
      soDon: cur.soDon,
      aov: cur.aov,
      tangTruong: tang,
    }
  }

  const homNayTu = nay
  const tuanTu = congNgay(nay, -6)
  const thangTu = new Date(nay.getFullYear(), nay.getMonth(), 1)
  const namTu = new Date(nay.getFullYear(), 0, 1)

  const demKhoa = (tu, den) => danhSachKhoa(tu, den).length
  const cuaCach = (tu) => {
    const len = demKhoa(tu, nay)
    return { tu: congNgay(tu, -len), den: congNgay(tu, -1) }
  }

  return [
    tinhMoc('Hôm nay', homNayTu, nay, congNgay(nay, -1), congNgay(nay, -1)),
    tinhMoc('Tuần này', tuanTu, nay, ...Object.values(cuaCach(tuanTu))),
    tinhMoc('Tháng này', thangTu, nay, ...Object.values(cuaCach(thangTu))),
    tinhMoc('Năm nay', namTu, nay, ...Object.values(cuaCach(namTu))),
  ]
}

/** Cơ cấu trạng thái đơn hàng trong khoảng. */
export const tinhTrangThaiDon = (bang, tu, den, boiSo = 1) => {
  const k = tinhKhoang(bang, tu, den, boiSo)
  const tong = k.soDon
  let daPhan = 0
  return TRANG_THAI_DON.map((ten, i) => {
    let soLuong
    if (i === TRANG_THAI_DON.length - 1) soLuong = Math.max(0, tong - daPhan)
    else {
      soLuong = Math.round(tong * MAU_TRANG_THAI[i])
      daPhan += soLuong
    }
    return { ten, soLuong, mau: MAU_TRANG_THAI[i] }
  })
}

/** Số liệu bán theo sản phẩm trong khoảng (chưa lọc thương hiệu). */
export const tinhSanPham = (sanPhams, tu, den) => {
  const soNgay = Math.max(1, danhSachKhoa(tu, den).length)
  return (sanPhams || []).map((sp) => {
    const rng = mulberry32(hashStr(sp.maSanPham || sp.tenSanPham || '') ^ 977)
    const tyLe = 0.03 + rng() * 0.4
    const dao = 0.7 + rng() * 0.6
    const daBan = Math.round(tyLe * soNgay * dao)
    const soTra = Math.round(daBan * 0.06 * rng())
    const gia = Number(sp.giaBan || sp.giaBanMin || 0) || Math.round((500000 + rng() * 2600000) / 10000) * 10000
    const tonKho = sp.soLuong !== undefined && sp.soLuong !== null ? Number(sp.soLuong) : Math.round(50 + rng() * 550)
    return {
      maSanPham: sp.maSanPham || '—',
      tenSanPham: sp.tenSanPham || sp.ten || '—',
      tenThuongHieu: sp.tenThuongHieu || 'Thương hiệu khác',
      daBan,
      soTra,
      doanhThu: daBan * gia,
      tonKho,
    }
  })
}

/** Nhóm số liệu sản phẩm theo thương hiệu (top 5 + Thương hiệu khác). */
export const tinhBangThuongHieu = (dongSanPham) => {
  const nhom = new Map()
  let tong = 0
  ;(dongSanPham || []).forEach((sp) => {
    const ten = sp.tenThuongHieu || 'Thương hiệu khác'
    if (!nhom.has(ten)) nhom.set(ten, { ten, doanhThu: 0, soDon: 0, mau: '' })
    const g = nhom.get(ten)
    g.doanhThu += sp.doanhThu
    g.soDon += Math.round(sp.daBan / 1.3)
    tong += sp.doanhThu
  })
  const sapXep = [...nhom.values()].sort((a, b) => b.doanhThu - a.doanhThu)
  const mau = ['#d92d20', '#f59e0b', '#eab308', '#22c55e', '#3b82f6', '#94a3b8']
  const top = sapXep.slice(0, 5).map((x, i) => ({ ...x, mau: mau[i] }))
  const conLai = sapXep.slice(5)
  const khac = conLai.length
    ? {
        ten: 'Thương hiệu khác',
        doanhThu: conLai.reduce((a, x) => a + x.doanhThu, 0),
        soDon: conLai.reduce((a, x) => a + x.soDon, 0),
        mau: mau[5],
      }
    : { ten: 'Thương hiệu khác', doanhThu: 0, soDon: 0, mau: mau[5] }
  const tyLe = {}
  sapXep.forEach((x) => {
    tyLe[x.ten] = tong > 0 ? x.doanhThu / tong : 0
  })
  return { top, khac, tyLe, tong }
}

/** Phân bổ số liệu cho bảng doanh thu nhân viên. */
export const tinhNhanVien = (nhanViens, khoang) => {
  const ds = nhanViens || []
  if (!ds.length) return []
  const rngTong = mulberry32(SEED_GOC + khoang.soNgay)
  const trongSo = ds.map((nv) => {
    const r = mulberry32(hashStr(String(nv.id ?? nv.maNhanVien ?? nv.tenTaiKhoan ?? '')) ^ 555)
    const uuTienAdmin = String(nv.maVaiTro || '').toUpperCase() === 'ADMIN' ? 1.4 : 1
    return (0.4 + r() * 1.2) * uuTienAdmin
  })
  const tongTrong = trongSo.reduce((a, b) => a + b, 0) || 1
  const rows = ds.map((nv, i) => {
    const tyLe = trongSo[i] / tongTrong
    const don = Math.max(0, Math.round(khoang.soDon * tyLe))
    const spBan = Math.max(0, Math.round(khoang.sanPhamBan * tyLe))
    const doanhThu = Math.max(0, Math.round(khoang.doanhThu * tyLe))
    return {
      id: nv.id,
      maNhanVien: nv.maNhanVien || '—',
      ten: nv.tenTaiKhoan || nv.ten || '—',
      vaiTro: nv.tenVaiTro || nv.vaiTro || '',
      anh: nv.anhNhanVien || '',
      don,
      spBan,
      doanhThu,
      xepHang: rngTong(),
    }
  })
  return rows.sort((a, b) => b.doanhThu - a.doanhThu)
}

export const layTangTruongLabel = (tang) => `${tang >= 0 ? '+' : ''}${tang}%`
