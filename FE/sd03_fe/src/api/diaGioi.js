/**
 * BẢN ĐỒ ĐỊA GIỚI HÀNH CHÍNH VIỆT NAM MỚI NHẤT (34 TỈNH/THÀNH PHỐ)
 * Theo Nghị quyết số 202/2025/QH15 của Quốc hội (Hiệu lực từ 01/7/2025)
 * Cả nước sắp xếp từ 63 tỉnh/thành phố xuống còn 34 đơn vị hành chính cấp tỉnh
 * (gồm 6 Thành phố trực thuộc Trung ương và 28 Tỉnh).
 */

// Danh sách chuẩn 34 Tỉnh/Thành phố mới nhất
export const DS_34_TINH_THANH = [
  // 6 Thành phố trực thuộc Trung ương
  { code: 'HN', ten: 'Thành phố Hà Nội', subCodes: [1], tinhCu: ['Hà Nội'] },
  { code: 'HP', ten: 'Thành phố Hải Phòng', subCodes: [31, 30], tinhCu: ['Hải Phòng', 'Hải Dương'] },
  { code: 'HUE', ten: 'Thành phố Huế', subCodes: [46], tinhCu: ['Huế', 'Thừa Thiên Huế'] },
  { code: 'DN', ten: 'Thành phố Đà Nẵng', subCodes: [48, 49], tinhCu: ['Đà Nẵng', 'Quảng Nam'] },
  { code: 'HCM', ten: 'Thành phố Hồ Chí Minh', subCodes: [79, 74, 77], tinhCu: ['Hồ Chí Minh', 'TP.HCM', 'TP Hồ Chí Minh', 'Bình Dương', 'Bà Rịa - Vũng Tàu', 'Bà Rịa Vũng Tàu'] },
  { code: 'CT', ten: 'Thành phố Cần Thơ', subCodes: [92, 93, 94], tinhCu: ['Cần Thơ', 'Hậu Giang', 'Sóc Trăng'] },

  // 28 Tỉnh
  { code: 'AG', ten: 'Tỉnh An Giang', subCodes: [89, 91], tinhCu: ['An Giang', 'Kiên Giang'] },
  { code: 'BN', ten: 'Tỉnh Bắc Ninh', subCodes: [27, 24], tinhCu: ['Bắc Ninh', 'Bắc Giang'] },
  { code: 'CM', ten: 'Tỉnh Cà Mau', subCodes: [96, 95], tinhCu: ['Cà Mau', 'Bạc Liêu'] },
  { code: 'CB', ten: 'Tỉnh Cao Bằng', subCodes: [4], tinhCu: ['Cao Bằng'] },
  { code: 'DL', ten: 'Tỉnh Đắk Lắk', subCodes: [66, 54], tinhCu: ['Đắk Lắk', 'Dak Lak', 'Phú Yên'] },
  { code: 'DB', ten: 'Tỉnh Điện Biên', subCodes: [11], tinhCu: ['Điện Biên'] },
  { code: 'DNA', ten: 'Tỉnh Đồng Nai', subCodes: [75, 70], tinhCu: ['Đồng Nai', 'Bình Phước'] },
  { code: 'DT', ten: 'Tỉnh Đồng Tháp', subCodes: [87, 82], tinhCu: ['Đồng Tháp', 'Tiền Giang'] },
  { code: 'GL', ten: 'Tỉnh Gia Lai', subCodes: [64, 52], tinhCu: ['Gia Lai', 'Bình Định'] },
  { code: 'HT', ten: 'Tỉnh Hà Tĩnh', subCodes: [42], tinhCu: ['Hà Tĩnh'] },
  { code: 'HY', ten: 'Tỉnh Hưng Yên', subCodes: [33, 34], tinhCu: ['Hưng Yên', 'Thái Bình'] },
  { code: 'KH', ten: 'Tỉnh Khánh Hòa', subCodes: [56, 58], tinhCu: ['Khánh Hòa', 'Ninh Thuận'] },
  { code: 'LC', ten: 'Tỉnh Lai Châu', subCodes: [12], tinhCu: ['Lai Châu'] },
  { code: 'LS', ten: 'Tỉnh Lạng Sơn', subCodes: [20], tinhCu: ['Lạng Sơn'] },
  { code: 'LCA', ten: 'Tỉnh Lào Cai', subCodes: [10, 15], tinhCu: ['Lào Cai', 'Yên Bái'] },
  { code: 'LD', ten: 'Tỉnh Lâm Đồng', subCodes: [68, 67, 60], tinhCu: ['Lâm Đồng', 'Đắk Nông', 'Dak Nong', 'Bình Thuận'] },
  { code: 'NA', ten: 'Tỉnh Nghệ An', subCodes: [40], tinhCu: ['Nghệ An'] },
  { code: 'NB', ten: 'Tỉnh Ninh Bình', subCodes: [37, 35, 36], tinhCu: ['Ninh Bình', 'Hà Nam', 'Nam Định'] },
  { code: 'PT', ten: 'Tỉnh Phú Thọ', subCodes: [25, 26, 17], tinhCu: ['Phú Thọ', 'Vĩnh Phúc', 'Hòa Bình', 'Hoà Bình'] },
  { code: 'QN', ten: 'Tỉnh Quảng Ngãi', subCodes: [51, 62], tinhCu: ['Quảng Ngãi', 'Kon Tum'] },
  { code: 'QNI', ten: 'Tỉnh Quảng Ninh', subCodes: [22], tinhCu: ['Quảng Ninh'] },
  { code: 'QT', ten: 'Tỉnh Quảng Trị', subCodes: [45, 44], tinhCu: ['Quảng Trị', 'Quảng Bình'] },
  { code: 'SL', ten: 'Tỉnh Sơn La', subCodes: [14], tinhCu: ['Sơn La'] },
  { code: 'TN', ten: 'Tỉnh Tây Ninh', subCodes: [72, 80], tinhCu: ['Tây Ninh', 'Long An'] },
  { code: 'TNG', ten: 'Tỉnh Thái Nguyên', subCodes: [19, 6], tinhCu: ['Thái Nguyên', 'Bắc Kạn', 'Bắc Cạn'] },
  { code: 'TH', ten: 'Tỉnh Thanh Hóa', subCodes: [38], tinhCu: ['Thanh Hóa'] },
  { code: 'TQ', ten: 'Tỉnh Tuyên Quang', subCodes: [8, 2], tinhCu: ['Tuyên Quang', 'Hà Giang'] },
  { code: 'VL', ten: 'Tỉnh Vĩnh Long', subCodes: [86, 83, 84], tinhCu: ['Vĩnh Long', 'Bến Tre', 'Trà Vinh'] },
]

// Giữ lại alias để các code cũ không bị lỗi tham chiếu
export const DS_63_TINH_THANH = DS_34_TINH_THANH

/**
 * Bảng ánh xạ từ 63 tỉnh/thành phố cũ sang 34 tỉnh/thành phố mới
 */
const BAN_DO_CHUYEN_DOI = {
  // Miền Bắc
  'hà nội': 'Thành phố Hà Nội',
  'hải phòng': 'Thành phố Hải Phòng',
  'hải dương': 'Thành phố Hải Phòng',
  'hưng yên': 'Tỉnh Hưng Yên',
  'thái bình': 'Tỉnh Hưng Yên',
  'bắc ninh': 'Tỉnh Bắc Ninh',
  'bắc giang': 'Tỉnh Bắc Ninh',
  'ninh bình': 'Tỉnh Ninh Bình',
  'hà nam': 'Tỉnh Ninh Bình',
  'nam định': 'Tỉnh Ninh Bình',
  'phú thọ': 'Tỉnh Phú Thọ',
  'vĩnh phúc': 'Tỉnh Phú Thọ',
  'hòa bình': 'Tỉnh Phú Thọ',
  'hoà bình': 'Tỉnh Phú Thọ',
  'quảng ninh': 'Tỉnh Quảng Ninh',
  'thái nguyên': 'Tỉnh Thái Nguyên',
  'bắc kạn': 'Tỉnh Thái Nguyên',
  'bắc cạn': 'Tỉnh Thái Nguyên',
  'lạng sơn': 'Tỉnh Lạng Sơn',
  'cao bằng': 'Tỉnh Cao Bằng',
  'tuyên quang': 'Tỉnh Tuyên Quang',
  'hà giang': 'Tỉnh Tuyên Quang',
  'lào cai': 'Tỉnh Lào Cai',
  'yên bái': 'Tỉnh Lào Cai',
  'lai châu': 'Tỉnh Lai Châu',
  'điện biên': 'Tỉnh Điện Biên',
  'sơn la': 'Tỉnh Sơn La',

  // Miền Trung
  'thanh hóa': 'Tỉnh Thanh Hóa',
  'nghệ an': 'Tỉnh Nghệ An',
  'hà tĩnh': 'Tỉnh Hà Tĩnh',
  'quảng bình': 'Tỉnh Quảng Trị',
  'quảng trị': 'Tỉnh Quảng Trị',
  'huế': 'Thành phố Huế',
  'thừa thiên huế': 'Thành phố Huế',
  'thừa thiên - huế': 'Thành phố Huế',
  'đà nẵng': 'Thành phố Đà Nẵng',
  'quảng nam': 'Thành phố Đà Nẵng',
  'quảng ngãi': 'Tỉnh Quảng Ngãi',
  'kon tum': 'Tỉnh Quảng Ngãi',
  'bình định': 'Tỉnh Gia Lai',
  'gia lai': 'Tỉnh Gia Lai',
  'phú yên': 'Tỉnh Đắk Lắk',
  'đắk lắk': 'Tỉnh Đắk Lắk',
  'dak lak': 'Tỉnh Đắk Lắk',
  'khánh hòa': 'Tỉnh Khánh Hòa',
  'ninh thuận': 'Tỉnh Khánh Hòa',
  'bình thuận': 'Tỉnh Lâm Đồng',
  'đắk nông': 'Tỉnh Lâm Đồng',
  'dak nong': 'Tỉnh Lâm Đồng',
  'lâm đồng': 'Tỉnh Lâm Đồng',

  // Miền Nam
  'hồ chí minh': 'Thành phố Hồ Chí Minh',
  'tp.hồ chí minh': 'Thành phố Hồ Chí Minh',
  'tp.hcm': 'Thành phố Hồ Chí Minh',
  'tp hcm': 'Thành phố Hồ Chí Minh',
  'sài gòn': 'Thành phố Hồ Chí Minh',
  'bình dương': 'Thành phố Hồ Chí Minh',
  'bà rịa - vũng tàu': 'Thành phố Hồ Chí Minh',
  'bà rịa vũng tàu': 'Thành phố Hồ Chí Minh',
  'vũng tàu': 'Thành phố Hồ Chí Minh',
  'đồng nai': 'Tỉnh Đồng Nai',
  'bình phước': 'Tỉnh Đồng Nai',
  'tây ninh': 'Tỉnh Tây Ninh',
  'long an': 'Tỉnh Tây Ninh',
  'tiền giang': 'Tỉnh Đồng Tháp',
  'đồng tháp': 'Tỉnh Đồng Tháp',
  'bến tre': 'Tỉnh Vĩnh Long',
  'trà vinh': 'Tỉnh Vĩnh Long',
  'vĩnh long': 'Tỉnh Vĩnh Long',
  'an giang': 'Tỉnh An Giang',
  'kiên giang': 'Tỉnh An Giang',
  'cần thơ': 'Thành phố Cần Thơ',
  'hậu giang': 'Thành phố Cần Thơ',
  'sóc trăng': 'Thành phố Cần Thơ',
  'bạc liêu': 'Tỉnh Cà Mau',
  'cà mau': 'Tỉnh Cà Mau',
}

let cacheTinh = null
const cacheCon = new Map()

/**
 * Loại bỏ tiền tố hành chính (Tỉnh, TP, Thành phố...)
 */
export const loaiBoTienTo = (str) => {
  if (!str) return ''
  return String(str)
    .trim()
    .toLowerCase()
    .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|xã|phường|thị trấn)\s+/i, '')
    .trim()
}

/**
 * Tải danh mục 34 Tỉnh/Thành phố mới nhất
 */
export const layDiaGioi = async () => {
  if (cacheTinh) return cacheTinh
  cacheTinh = [...DS_34_TINH_THANH]
  return cacheTinh
}

/**
 * Tìm tỉnh/thành theo tên hoặc mã.
 * Tự động chuyển đổi tên tỉnh cũ (vd Thái Bình, Bình Dương, Hải Dương) sang tỉnh mới nhất (Hưng Yên, TP.HCM, Hải Phòng).
 */
export const timTinh = (ds, tenHayCode) => {
  if (!tenHayCode) return null
  const danhSach = ds && ds.length ? ds : DS_34_TINH_THANH
  const raw = String(tenHayCode).trim().toLowerCase()
  const clean = loaiBoTienTo(raw)

  // 1. So khớp mã tỉnh mới (HN, HP, HY, HCM...)
  let found = danhSach.find((t) => String(t.code).toLowerCase() === raw)
  if (found) return found

  // 2. So khớp trực tiếp tên 34 tỉnh mới
  found = danhSach.find((t) => String(t.ten).trim().toLowerCase() === raw)
  if (found) return found

  found = danhSach.find((t) => loaiBoTienTo(t.ten) === clean)
  if (found) return found

  // 3. Ánh xạ từ danh mục tỉnh cũ sang tỉnh mới
  const tenMoi = BAN_DO_CHUYEN_DOI[clean] || BAN_DO_CHUYEN_DOI[raw]
  if (tenMoi) {
    found = danhSach.find((t) => t.ten === tenMoi)
    if (found) return found
  }

  // 4. So khớp theo danh sách tinhCu khai báo trong mỗi tỉnh
  found = danhSach.find((t) => {
    if (!t.tinhCu || !t.tinhCu.length) return false
    return t.tinhCu.some((c) => {
      const cClean = loaiBoTienTo(c)
      return cClean === clean || clean.includes(cClean) || cClean.includes(clean)
    })
  })
  if (found) return found

  // 5. So khớp tương đối
  found = danhSach.find((t) => {
    const tClean = loaiBoTienTo(t.ten)
    return tClean.includes(clean) || clean.includes(tClean)
  })

  return found || null
}

/**
 * Tải danh sách đơn vị hành chính cấp cơ sở (Xã/Phường/Thị trấn/Thành phố trực thuộc) theo tỉnh mới.
 * Sử dụng depth=3 để lấy chi tiết các Xã, Phường, Thị trấn, Thành phố; LOẠI BỎ cấp Huyện theo yêu cầu.
 */
export const layPhuongXa = async (ds, tinh) => {
  if (!tinh) return []
  if (tinh.phuongXa && tinh.phuongXa.length) return tinh.phuongXa

  const cacheKey = tinh.code || tinh.ten
  if (cacheCon.has(cacheKey)) {
    return cacheCon.get(cacheKey)
  }

  const subCodes = tinh.subCodes && tinh.subCodes.length ? tinh.subCodes : [tinh.code]
  const mapTen = new Map()

  const fetchSub = async (subCode) => {
    if (!(typeof subCode === 'number' || !isNaN(Number(subCode)))) return null
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 10000)
    try {
      // Ưu tiên api v2 nếu có
      try {
        const resV2 = await fetch(`https://provinces.open-api.vn/api/v2/p/${subCode}?depth=2`, { signal: ctrl.signal })
        if (resV2.ok) {
          const dataV2 = await resV2.json()
          if (dataV2 && Array.isArray(dataV2.wards) && dataV2.wards.length > 0) {
            return { districts: [{ wards: dataV2.wards }] }
          }
        }
      } catch {}

      const res = await fetch(`https://provinces.open-api.vn/api/v1/p/${subCode}?depth=3`, { signal: ctrl.signal })
      if (!res.ok) return null
      return await res.json()
    } catch {
      // Bo qua loi subCode rieng le (timeout/quet mang...)
      return null
    } finally {
      clearTimeout(timer)
    }
  }

  try {
    const results = await Promise.all(subCodes.map(fetchSub))
    for (const data of results) {
      if (!data || !Array.isArray(data.districts)) continue
      for (const d of data.districts) {
        // 1. Them Thanh pho truc thuoc tinh hoac Thi xa (neu co)
        if (d.name && (d.name.startsWith('Thành phố') || d.name.startsWith('Thị xã'))) {
          if (!mapTen.has(d.name)) {
            mapTen.set(d.name, { code: d.code, ten: d.name })
          }
        }

        // 2. Them tat ca cac Xa, Phuong, Thi tran tu wards
        if (d.wards && Array.isArray(d.wards)) {
          for (const w of d.wards) {
            if (w.name && !mapTen.has(w.name)) {
              mapTen.set(w.name, { code: w.code, ten: w.name })
            }
          }
        }
      }
    }
  } catch (e) {
    console.warn('Lỗi khi tải đơn vị cấp xã/phường cho:', tinh.ten, e)
  }

  // 3. Bổ sung các xã/phường mới thành lập sau sáp nhập theo các nghị quyết gần đây
  const tenTinhClean = loaiBoTienTo(tinh.ten || '')
  if (tenTinhClean.includes('hưng yên')) {
    if (!mapTen.has('Xã Hồng Vũ')) {
      mapTen.set('Xã Hồng Vũ', { code: 'hong_vu', ten: 'Xã Hồng Vũ' })
    }
  }

  const danhSachTong = Array.from(mapTen.values())

  // Sắp xếp theo bảng chữ cái tiếng Việt
  danhSachTong.sort((a, b) => a.ten.localeCompare(b.ten, 'vi'))

  // Chỉ cache kết quả có dữ liệu - tránh cache rỗng khi mạng lỗi khiến dropdown trống vĩnh viễn
  if (danhSachTong.length > 0) {
    cacheCon.set(cacheKey, danhSachTong)
  }
  return danhSachTong
}

/**
 * Bóc tách chuỗi địa chỉ từ CCCD và tự động chuẩn hóa theo 34 Tỉnh/TP mới nhất.
 * Ví dụ: 'Thôn 2, Vũ Thắng, Kiến Xương, Thái Bình'
 * -> Tỉnh mới: 'Tỉnh Hưng Yên' (Thái Bình đã sáp nhập vào Hưng Yên)
 * -> Phường/Xã: 'Vũ Thắng'
 * -> Địa chỉ cụ thể: 'Thôn 2, Kiến Xương (Thái Bình cũ)'
 */
/**
 * Bóc tách chuỗi địa chỉ từ CCCD thông minh và chính xác tuyệt đối.
 * Hỗ trợ cả 2 định dạng CCCD phổ biến tại Việt Nam:
 * - Định dạng 3 cấp (mẫu mới): [Thôn/Số nhà/Tổ], [Xã/Phường], [Tỉnh/TP]
 *   Ví dụ: 'Thôn Vũ Thắng 2, Xã Hồng Vũ, Tỉnh Hưng Yên'
 *   -> Tỉnh: 'Tỉnh Hưng Yên' | Phường/Xã: 'Xã Hồng Vũ' | Địa chỉ cụ thể: 'Thôn Vũ Thắng 2'
 * - Định dạng 4 cấp (mẫu cũ): [Thôn/Số nhà], [Xã/Phường], [Huyện/Quận], [Tỉnh/TP]
 *   Ví dụ: 'Thôn 2, Xã Vũ Thắng, Huyện Kiến Xương, Tỉnh Thái Bình'
 *   -> Tỉnh: 'Tỉnh Hưng Yên' (ánh xạ từ Thái Bình) | Phường/Xã: 'Xã Vũ Thắng' | Địa chỉ cụ thể: 'Thôn 2, Huyện Kiến Xương'
 */
/**
 * Bảng ánh xạ chuyển đổi các xã, huyện và thôn cũ sau sáp nhập sang đơn vị hành chính mới nhất
 * (Theo Nghị quyết 1201/NQ-UBTVQH15 và các đề án sắp xếp đơn vị hành chính)
 */
const BAN_DO_SAP_NHAP_XA = [
  {
    // Kiến Xương, Thái Bình -> Hồng Vũ, Hưng Yên
    tuKhoaHuyen: ['kiến xương'],
    tuKhoaXa: ['vũ thắng', 'vũ bình', 'vũ hòa', 'vũ hoà', 'vũ công'],
    xaMoi: 'Xã Hồng Vũ',
    chuanHoaThon: (thon, xaGoc) => {
      const cleanXa = (xaGoc || '').toLowerCase()
      // Nhận diện số thôn: Thôn 1, Thôn 2, Thôn 3...
      const match = (thon || '').match(/(?:thôn|xóm|tổ)\s*(\d+)/i)
      const soThon = match ? match[1] : ''

      if (cleanXa.includes('vũ thắng')) {
        return soThon ? `Thôn Vũ Thắng ${soThon}` : (thon.includes('Vũ Thắng') ? thon : `Thôn Vũ Thắng ${thon}`.trim())
      }
      if (cleanXa.includes('vũ bình')) {
        return soThon ? `Thôn Vũ Bình ${soThon}` : (thon.includes('Vũ Bình') ? thon : `Thôn Vũ Bình ${thon}`.trim())
      }
      if (cleanXa.includes('vũ hòa') || cleanXa.includes('vũ hoà')) {
        return soThon ? `Thôn Vũ Hòa ${soThon}` : (thon.includes('Vũ Hòa') ? thon : `Thôn Vũ Hòa ${thon}`.trim())
      }
      if (cleanXa.includes('vũ công')) {
        return soThon ? `Thôn Vũ Công ${soThon}` : (thon.includes('Vũ Công') ? thon : `Thôn Vũ Công ${thon}`.trim())
      }
      return thon
    },
  },
]

/**
 * Bóc tách chuỗi địa chỉ từ CCCD thông minh và chính xác tuyệt đối.
 * Tự động chuyển đổi toàn diện: Tỉnh cũ -> Tỉnh mới (34 Tỉnh/TP), Xã cũ -> Xã mới, Thôn cũ -> Thôn mới!
 */
export const phanTichDiaChiCccd = (raw) => {
  if (!raw || !raw.trim()) {
    return { tinh: '', tinhGoc: '', phuong: '', huyen: '', diaChiCuThe: '', diaChiDayDu: '', daDoiTinh: false }
  }

  let diaChiDayDu = raw.trim()
  // Chuẩn hóa dấu phân cách nếu dùng gạch ngang hoặc dấu gạch chéo
  if (!diaChiDayDu.includes(',') && (diaChiDayDu.includes(' - ') || diaChiDayDu.includes('/') || diaChiDayDu.includes(' -'))) {
    diaChiDayDu = diaChiDayDu.replace(/\s*-\s*|\s*\/\s*/g, ', ')
  }

  const parts = diaChiDayDu.split(',').map((s) => s.trim()).filter(Boolean)

  if (parts.length === 0) {
    return { tinh: '', tinhGoc: '', phuong: '', huyen: '', diaChiCuThe: '', diaChiDayDu, daDoiTinh: false }
  }

  // 1. Tỉnh/Thành phố luôn ở vị trí cuối cùng
  const tinhGoc = parts[parts.length - 1]
  const cacPhanConLai = parts.slice(0, parts.length - 1)

  let huyen = ''
  let xa = ''
  let diaChiCuThe = ''

  if (cacPhanConLai.length === 0) {
    diaChiCuThe = tinhGoc
  } else if (cacPhanConLai.length === 1) {
    const phan = cacPhanConLai[0]
    if (/^(xã|phường|thị trấn)\s+/i.test(phan)) {
      xa = phan
      diaChiCuThe = phan
    } else {
      diaChiCuThe = phan
    }
  } else if (cacPhanConLai.length === 2) {
    // Chuẩn 3 cấp: [Thôn], [Xã/Phường]
    const phan1 = cacPhanConLai[0] // Thôn
    const phan2 = cacPhanConLai[1] // Xã/Phường

    if (/^(xã|phường|thị trấn)\s+/i.test(phan2)) {
      xa = phan2
      diaChiCuThe = phan1
    } else if (/^(quận|huyện|thị xã|thành phố|tp)\s+/i.test(phan2)) {
      huyen = phan2
      xa = phan1
      diaChiCuThe = phan1
    } else {
      xa = phan2
      diaChiCuThe = phan1
    }
  } else {
    // Chuẩn 4 cấp: [Thôn], [Xã], [Huyện]
    const phanSatTinh = cacPhanConLai[cacPhanConLai.length - 1] // Huyện (vd: Kiến Xương)
    const phanKeSatTinh = cacPhanConLai[cacPhanConLai.length - 2] // Xã (vd: Vũ Thắng)
    const phanDau = cacPhanConLai.slice(0, cacPhanConLai.length - 2).join(', ') // Thôn (vd: Thôn 2)

    huyen = phanSatTinh
    xa = phanKeSatTinh
    diaChiCuThe = phanDau || phanKeSatTinh
  }

  // 2. Tự động kiểm tra ánh xạ xã sáp nhập và đổi tên thôn mới nhất (chỉ chạy khi cleanXa có giá trị)
  const cleanXa = (xa || '').toLowerCase().replace(/^(xã|phường|thị trấn)\s+/i, '').trim()
  const cleanHuyen = (huyen || '').toLowerCase().replace(/^(huyện|quận|thị xã|tp|thành phố)\s+/i, '').trim()

  if (cleanXa) {
    for (const rule of BAN_DO_SAP_NHAP_XA) {
      const hopHuyen = !rule.tuKhoaHuyen || (Boolean(cleanHuyen) && rule.tuKhoaHuyen.some((h) => cleanHuyen.includes(h) || h.includes(cleanHuyen)))
      const hopXa = rule.tuKhoaXa && rule.tuKhoaXa.some((x) => cleanXa.includes(x) || x.includes(cleanXa))

      if (hopXa && hopHuyen) {
        const xaGoc = xa
        xa = rule.xaMoi
        if (rule.chuanHoaThon && diaChiCuThe) {
          diaChiCuThe = rule.chuanHoaThon(diaChiCuThe, xaGoc)
        }
        break
      }
    }
  }

  // Giữ nguyên địa chỉ gốc theo đúng chữ ghi trên CCCD
  return {
    tinh: tinhGoc,
    tinhGoc,
    phuong: xa,
    huyen,
    diaChiCuThe: diaChiCuThe || diaChiDayDu,
    diaChiDayDu,
    daDoiTinh: false,
    tinhObj: null,
  }
}

/**
 * Phân tích chuỗi QR trên thẻ CCCD theo mọi định dạng (7 phần, 6 phần, 5 phần)
 * Format chuẩn: Số CCCD|Số CMND cũ|Họ và tên|Ngày sinh (ddMMyyyy)|Giới tính|Địa chỉ thường trú|Ngày cấp
 */
export const phanTichChuoiCccd = (raw) => {
  if (!raw || typeof raw !== 'string') return null
  const clean = raw.replace(/^["'\s]+|["'\s]+$/g, '').trim()
  if (!clean.includes('|')) return null

  const parts = clean.split('|').map((p) => p.trim())
  if (parts.length < 4) return null

  let soCccd = ''
  let hoTen = ''
  let dobRaw = ''
  let gioiTinhRaw = ''
  let diaChiRaw = ''
  let ngayCapRaw = ''

  // Tìm số CCCD (12 chữ số)
  if (/^\d{12}$/.test(parts[0])) {
    soCccd = parts[0]
  } else {
    const cccdIdx = parts.findIndex((p) => /^\d{12}$/.test(p))
    if (cccdIdx !== -1) soCccd = parts[cccdIdx]
  }

  // Tìm ngày sinh: 8 chữ số (ddMMyyyy)
  const dobIndex = parts.findIndex((p, idx) => idx > 0 && /^\d{8}$/.test(p))

  if (dobIndex !== -1) {
    dobRaw = parts[dobIndex]
    // Họ tên nằm ngay trước ngày sinh
    if (dobIndex > 0) {
      hoTen = parts[dobIndex - 1]
    }
    // Giới tính nằm ngay sau ngày sinh
    if (dobIndex + 1 < parts.length) {
      gioiTinhRaw = parts[dobIndex + 1]
    }
  } else {
    // Trường hợp không tìm thấy ngày sinh dạng 8 số
    if (parts.length >= 7) {
      soCccd = soCccd || parts[0]
      hoTen = parts[2]
      dobRaw = parts[3]
      gioiTinhRaw = parts[4]
    } else if (parts.length === 6) {
      soCccd = soCccd || parts[0]
      hoTen = parts[1]
      dobRaw = parts[2]
      gioiTinhRaw = parts[3]
    } else {
      soCccd = soCccd || parts[0]
      hoTen = parts[1]
      dobRaw = parts[2]
      gioiTinhRaw = parts[3]
    }
  }

  // Lọc tìm địa chỉ thường trú chuẩn xác nhất:
  // Loại trừ số CCCD, ngày tháng, tên công dân, giới tính và quốc tịch "Việt Nam"
  const candidates = parts.filter((p, idx) => {
    if (idx === 0) return false
    if (/^\d{8,12}$/.test(p)) return false
    if (['nam', 'nữ', 'nu', 'male', 'female', 'm', 'f', 'việt nam', 'viet nam'].includes(p.toLowerCase())) return false
    if (p === hoTen) return false
    return true
  })

  const withCommas = candidates.filter((c) => c.includes(','))
  if (withCommas.length > 0) {
    diaChiRaw = withCommas.sort((a, b) => b.length - a.length)[0]
  } else {
    const withKeyword = candidates.find((c) =>
      /(tỉnh|thành phố|tp|huyện|quận|thị xã|xã|phường|thị trấn|thôn|tổ|đường|phố|số)/i.test(c),
    )
    diaChiRaw = withKeyword || candidates.sort((a, b) => b.length - a.length)[0] || ''
  }

  // Chuẩn hóa ngày sinh sang yyyy-MM-dd
  let ngaySinh = ''
  if (dobRaw && dobRaw.length === 8 && /^\d{8}$/.test(dobRaw)) {
    const d = dobRaw.substring(0, 2)
    const m = dobRaw.substring(2, 4)
    const y = dobRaw.substring(4, 8)
    ngaySinh = `${y}-${m}-${d}`
  }

  // Chuẩn hóa giới tính
  let gioiTinh = 'NAM'
  if (gioiTinhRaw) {
    const gLower = gioiTinhRaw.toLowerCase()
    if (gLower.includes('nữ') || gLower.includes('nu') || gLower === 'female' || gLower === 'f') {
      gioiTinh = 'NU'
    } else {
      gioiTinh = 'NAM'
    }
  }

  return {
    soCccd,
    hoTen: (hoTen || '').trim(),
    dobRaw,
    ngaySinh,
    gioiTinhRaw,
    gioiTinh,
    diaChiRaw: (diaChiRaw || '').trim(),
    ngayCapRaw,
  }
}
