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

/** Build daily aggregates from actual invoice records. */
export const taoBangNgay = (hoaDons = [], sanPhams = [], productIdByVariant = new Map()) => {
  const map = new Map()
  const validStatus = new Set(['DA_HOAN_THANH'])
  const productById = new Map((sanPhams || []).map((sp) => [Number(sp.id), sp]))
  const productByName = new Map((sanPhams || []).map((sp) => [sp.tenSanPham, sp]))
  const firstCustomerOrder = new Map()
  for (const hd of hoaDons) {
    if (hd.idKhachHang == null || !hd.ngayTao) continue
    const d = new Date(hd.ngayTao)
    const previous = firstCustomerOrder.get(hd.idKhachHang)
    if (!previous || d < previous) firstCustomerOrder.set(hd.idKhachHang, d)
  }
  for (const hd of hoaDons) {
    if (!hd.ngayTao) continue
    const d = new Date(hd.ngayTao)
    if (Number.isNaN(d.getTime())) continue
    const key = khoaNgay(d)
    if (!map.has(key)) map.set(key, { ngay: key, soDon: 0, doanhThu: 0, doanhThuDuKien: 0, tienMat: 0, tienChuyenKhoan: 0, sanPhamBan: 0, khachMoi: 0, soTra: 0, trangThais: {}, brands: {} })
    const row = map.get(key)
    const status = hd.maTrangThai || ''
    row.trangThais[status] = (row.trangThais[status] || 0) + 1
    if (status === 'DA_HOAN_TIEN') row.soTra++
    if (firstCustomerOrder.get(hd.idKhachHang)?.getTime() === d.getTime()) row.khachMoi++
    const amount = Number(hd.tienSauGiamGia || 0)
    if (status === 'DA_XAC_NHAN') {
      row.doanhThuDuKien += amount
      const lines = hd.chiTiet || []
      const totalLines = lines.reduce((sum, item) => sum + Number(item.thanhTien || 0), 0)
      for (const line of lines) {
        const product = productById.get(Number(productIdByVariant.get(Number(line.idSanPhamChiTiet))))
          || productByName.get(line.tenSanPham)
        const brand = product?.tenThuongHieu || 'Thương hiệu khác'
        if (!row.brands[brand]) row.brands[brand] = { doanhThu: 0, doanhThuDuKien: 0, tienMat: 0, tienChuyenKhoan: 0, sanPhamBan: 0, donIds: new Set() }
        row.brands[brand].doanhThuDuKien += totalLines > 0 ? amount * Number(line.thanhTien || 0) / totalLines : 0
      }
      continue
    }
    if (!validStatus.has(status)) continue
    row.soDon++
    row.doanhThu += amount
    const payment = String(hd.tenPhuongThucThanhToan || '').toLowerCase()
    if (payment.includes('mặt') || payment.includes('mat')) row.tienMat += amount
    else row.tienChuyenKhoan += amount
    for (const line of hd.chiTiet || []) {
      const product = productById.get(Number(productIdByVariant.get(Number(line.idSanPhamChiTiet))))
        || productByName.get(line.tenSanPham)
      const brand = product?.tenThuongHieu || 'Thương hiệu khác'
      const quantity = Number(line.soLuong || 0)
      row.sanPhamBan += quantity
      if (!row.brands[brand]) row.brands[brand] = { doanhThu: 0, doanhThuDuKien: 0, tienMat: 0, tienChuyenKhoan: 0, sanPhamBan: 0, donIds: new Set() }
      const lineTotal = Number(line.thanhTien || 0)
      const totalLines = (hd.chiTiet || []).reduce((sum, item) => sum + Number(item.thanhTien || 0), 0)
      const lineNet = totalLines > 0 ? amount * lineTotal / totalLines : 0
      row.brands[brand].doanhThu += lineNet
      if (payment.includes('mặt') || payment.includes('mat')) row.brands[brand].tienMat += lineNet
      else row.brands[brand].tienChuyenKhoan += lineNet
      row.brands[brand].sanPhamBan += quantity
      row.brands[brand].donIds.add(hd.id)
    }
    Object.values(row.brands).forEach((brand) => { brand.soDon = brand.donIds.size })
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
  while (d <= end) {
    out.push(khoaNgay(d))
    d = congNgay(d, 1)
    dem++
  }
  return out
}

const lamTron = (v) => Math.round(v)

/** Tổng hợp một khoảng ngày. Có thể lọc theo thương hiệu từ dữ liệu chi tiết hóa đơn. */
export const tinhKhoang = (bang, tu, den, thuongHieu = '') => {
  const r = { soDon: 0, doanhThu: 0, doanhThuDuKien: 0, tienMat: 0, tienChuyenKhoan: 0, sanPhamBan: 0, khachMoi: 0, soTra: 0, soNgay: 0 }
  danhSachKhoa(tu, den).forEach((key) => {
    const x = bang.get(key)
    if (!x) return
    r.soNgay++
    const chosen = thuongHieu ? x.brands?.[thuongHieu] : null
    r.soDon += chosen ? chosen.soDon : x.soDon
    r.doanhThu += chosen ? chosen.doanhThu : x.doanhThu
    r.doanhThuDuKien += chosen ? chosen.doanhThuDuKien : x.doanhThuDuKien
    r.tienMat += chosen ? chosen.tienMat : x.tienMat
    r.tienChuyenKhoan += chosen ? chosen.tienChuyenKhoan : x.tienChuyenKhoan
    r.sanPhamBan += chosen ? chosen.sanPhamBan : x.sanPhamBan
    r.khachMoi += chosen ? 0 : x.khachMoi
    r.soTra += chosen ? 0 : x.soTra
  })
  r.aov = r.soDon > 0 ? Math.round(r.doanhThu / r.soDon) : 0
  return r
}

const nhanNgay = (d) => `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}`
const nhanThang = (d) => `${pad2(d.getMonth() + 1)}/${d.getFullYear()}`

/** Chuỗi dữ liệu biểu đồ "Sản phẩm bán được theo ngày" (kèm kiểu tổng hợp). */
export const tinhChuoiBanHang = (bang, tu, den, kieu = 'ngay', thuongHieu = '') => {
  const cacNgay = danhSachKhoa(tu, den).map((k) => bang.get(k)).filter(Boolean)
  if (!cacNgay.length) return []
  const soLuong = (x) => thuongHieu ? (x.brands?.[thuongHieu]?.sanPhamBan || 0) : x.sanPhamBan
  if (kieu === 'thang' || kieu === 'tuan') {
    const nhom = new Map()
    cacNgay.forEach((x) => {
      const d = docKhoa(x.ngay)
      const dauTuan = congNgay(d, -((d.getDay() + 6) % 7))
      const key = kieu === 'thang' ? String(d.getFullYear()) + '-' + pad2(d.getMonth() + 1) : khoaNgay(dauTuan)
      const nhan = kieu === 'thang' ? nhanThang(d) : nhanNgay(dauTuan)
      if (!nhom.has(key)) nhom.set(key, { nhan, giaTri: 0 })
      nhom.get(key).giaTri += soLuong(x)
    })
    return [...nhom.values()].map((row) => ({ ...row, giaTri: lamTron(row.giaTri) }))
  }
  return cacNgay.map((x) => ({ nhan: nhanNgay(docKhoa(x.ngay)), giaTri: lamTron(soLuong(x)) }))
}

/** Bảng "Tổng quan theo chu kỳ thời gian" (Hôm nay / Tuần này / Tháng này / Năm nay). */
export const tinhChuKy = (bang, hienTai = new Date(), thuongHieu = '') => {
  const nay = dauNgay(hienTai)
  const tinhMoc = (nhan, tu, den, tuTruoc, denTruoc) => {
    const cur = tinhKhoang(bang, tu, den, thuongHieu)
    const prev = tinhKhoang(bang, tuTruoc, denTruoc, thuongHieu)
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

/** Actual order status breakdown for the selected period. */
export const tinhTrangThaiDon = (bang, tu, den) => {
  const counts = {}
  danhSachKhoa(tu, den).forEach((key) => Object.entries(bang.get(key)?.trangThais || {}).forEach(([status, count]) => { counts[status] = (counts[status] || 0) + count }))
  const labels = { CHO_XAC_NHAN: 'Chờ xác nhận', DA_XAC_NHAN: 'Đã xác nhận', CHO_GIAO_HANG: 'Chờ giao hàng', DANG_GIAO_HANG: 'Đang giao hàng', DA_GIAO_HANG: 'Đã giao hàng', DA_HOAN_THANH: 'Đã hoàn thành', DA_HUY: 'Đã hủy', DA_HOAN_TIEN: 'Đã hoàn tiền', GIAO_HANG_THAT_BAI: 'Giao hàng thất bại' }
  const colors = ['#d92d20', '#dc2626', '#ef4444', '#b91c1c', '#cc0000', '#a30000']
  return Object.entries(counts).map(([status, soLuong], i) => ({ ten: labels[status] || status, soLuong, mau: colors[i % colors.length] }))
}

/** Aggregate sold quantities/revenue from real invoice lines. */
export const tinhSanPham = (sanPhams, hoaDons, tu, den, productIdByVariant = new Map()) => {
  const rows = new Map((sanPhams || []).map((sp) => [Number(sp.id), { id: Number(sp.id), maSanPham: sp.maSanPham || '—', tenSanPham: sp.tenSanPham, tenThuongHieu: sp.tenThuongHieu || 'Thương hiệu khác', daBan: 0, soTra: 0, doanhThu: 0, tonKho: Number(sp.tongTonKho ?? sp.soLuong ?? 0), donIds: new Set() }]))
  const productIdByName = new Map((sanPhams || []).map((sp) => [sp.tenSanPham, Number(sp.id)]))
  const validStatus = new Set(['DA_HOAN_THANH'])
  for (const hd of hoaDons || []) {
    const d = hd.ngayTao ? new Date(hd.ngayTao) : null
    if (!d || d < dauNgay(tu) || d > cuoiNgay(den)) continue
    if (hd.maTrangThai === 'DA_HOAN_TIEN') {
      for (const line of hd.chiTiet || []) {
        const productId = Number(productIdByVariant.get(Number(line.idSanPhamChiTiet)) || productIdByName.get(line.tenSanPham))
        const row = rows.get(productId)
        if (row) row.soTra += Number(line.soLuong || 0)
      }
      continue
    }
    if (!validStatus.has(hd.maTrangThai)) continue
    const linesTotal = (hd.chiTiet || []).reduce((sum, line) => sum + Number(line.thanhTien || 0), 0)
    for (const line of hd.chiTiet || []) {
      const productId = Number(productIdByVariant.get(Number(line.idSanPhamChiTiet)) || productIdByName.get(line.tenSanPham))
      const row = rows.get(productId)
      if (!row) continue
      const lineTotal = Number(line.thanhTien || 0)
      row.daBan += Number(line.soLuong || 0)
      row.doanhThu += linesTotal > 0 ? Number(hd.tienSauGiamGia || 0) * lineTotal / linesTotal : 0
      row.donIds.add(hd.id)
    }
  }
  return [...rows.values()]
}

/** Group actual sales by product brand. */
export const tinhBangThuongHieu = (dongSanPham) => {
  const nhom = new Map()
  let tong = 0
  ;(dongSanPham || []).forEach((sp) => {
    const ten = sp.tenThuongHieu || 'Thương hiệu khác'
    if (!nhom.has(ten)) nhom.set(ten, { ten, doanhThu: 0, mau: '', donIds: new Set() })
    const g = nhom.get(ten)
    g.doanhThu += sp.doanhThu
    sp.donIds?.forEach((id) => g.donIds.add(id))
    tong += sp.doanhThu
  })
  const colors = ['#d92d20', '#dc2626', '#ef4444', '#b91c1c', '#cc0000', '#94a3b8']
  const all = [...nhom.values()].map((row) => ({ ten: row.ten, doanhThu: row.doanhThu, donIds: row.donIds }))
    .sort((a, b) => b.doanhThu - a.doanhThu)
  const top = all.slice(0, 5).map((row, i) => ({ ten: row.ten, doanhThu: row.doanhThu, soDon: row.donIds.size, mau: colors[i] }))
  const rest = all.slice(5)
  const restOrders = new Set(rest.flatMap((row) => [...row.donIds]))
  const khac = rest.length ? {
    ten: 'Thương hiệu khác', doanhThu: rest.reduce((sum, row) => sum + row.doanhThu, 0),
    soDon: restOrders.size, mau: colors[5],
  } : { ten: 'Thương hiệu khác', doanhThu: 0, soDon: 0, mau: colors[5] }
  const tyLe = {}
  all.forEach((row) => { tyLe[row.ten] = tong > 0 ? row.doanhThu / tong : 0 })
  return { top, khac, tyLe, tong }
}

/** Actual sales assigned to each employee on the source invoices. */
export const tinhNhanVien = (nhanViens, hoaDons, tu, den) => {
  const rows = new Map((nhanViens || []).map((nv) => [nv.id, { id: nv.id, maNhanVien: nv.maNhanVien || '—', ten: nv.tenTaiKhoan || nv.ten || '—', vaiTro: nv.tenVaiTro || nv.vaiTro || '', anh: nv.anhNhanVien || '', don: 0, spBan: 0, doanhThu: 0 }]))
  const validStatus = new Set(['DA_HOAN_THANH'])
  for (const hd of hoaDons || []) {
    const d = hd.ngayTao ? new Date(hd.ngayTao) : null
    if (!d || d < dauNgay(tu) || d > cuoiNgay(den) || !validStatus.has(hd.maTrangThai)) continue
    const row = rows.get(hd.idNhanVien)
    if (!row) continue
    row.don++
    row.doanhThu += Number(hd.tienSauGiamGia || 0)
    for (const line of hd.chiTiet || []) row.spBan += Number(line.soLuong || 0)
  }
  return [...rows.values()].sort((a, b) => b.doanhThu - a.doanhThu)
}

export const layTangTruongLabel = (tang) => `${tang >= 0 ? '+' : ''}${tang}%`
