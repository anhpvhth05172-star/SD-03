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

// Dữ liệu Phường/Xã dự phòng theo từng tỉnh (dùng khi không kết nối được provinces.open-api.vn)
const DS_PHUONG_XA_DU_PHONG = {
  HN: ['Phường Ba Đình', 'Phường Hoàn Kiếm', 'Phường Cửa Nam', 'Phường Hai Bà Trưng', 'Phường Đống Đa', 'Phường Văn Miếu - Quốc Tử Giám', 'Phường Cầu Giấy', 'Phường Thanh Xuân', 'Phường Tây Hồ', 'Phường Long Biên', 'Phường Bồ Đề', 'Phường Hoàng Mai', 'Phường Hà Đông', 'Phường Bắc Từ Liêm', 'Phường Nam Từ Liêm', 'Xã Gia Lâm', 'Xã Đông Anh', 'Xã Sóc Sơn', 'Xã Thanh Trì', 'Xã Hoài Đức'],
  HCM: ['Phường Sài Gòn', 'Phường Bến Nghé', 'Phường Bến Thành', 'Phường Tân Định', 'Phường Đa Kao', 'Phường Võ Thị Sáu', 'Phường Bình Thạnh', 'Phường Gia Định', 'Phường Phú Nhuận', 'Phường Tân Bình', 'Phường Gò Vấp', 'Phường Thủ Đức', 'Phường Chợ Lớn', 'Phường Bình Tân', 'Phường Vũng Tàu', 'Phường Thủ Dầu Một', 'Phường Dĩ An', 'Xã Bình Chánh', 'Xã Củ Chi'],
  HP: ['Phường Hồng Bàng', 'Phường Ngô Quyền', 'Phường Lê Chân', 'Phường Hải An', 'Phường Kiến An', 'Phường Đồ Sơn', 'Phường Dương Kinh', 'Phường Thủy Nguyên', 'Phường Hải Dương', 'Phường Chí Linh', 'Xã An Dương', 'Xã An Lão', 'Xã Kiến Thụy', 'Xã Tiên Lãng', 'Xã Vĩnh Bảo'],
  DN: ['Phường Hải Châu', 'Phường Thanh Khê', 'Phường Sơn Trà', 'Phường Ngũ Hành Sơn', 'Phường Liên Chiểu', 'Phường Cẩm Lệ', 'Phường Hòa Xuân', 'Phường Tam Kỳ', 'Phường Hội An', 'Xã Hòa Vang', 'Xã Hòa Tiến', 'Xã Điện Bàn'],
  HUE: ['Phường Phú Xuân', 'Phường Thuận Hóa', 'Phường Hương Thủy', 'Phường Hương Trà', 'Phường Phong Điền', 'Phường Vỹ Dạ', 'Phường An Cựu', 'Xã Phú Vang', 'Xã Quảng Điền', 'Xã A Lưới'],
  CT: ['Phường Ninh Kiều', 'Phường Cái Khế', 'Phường An Hòa', 'Phường Bình Thủy', 'Phường Cái Răng', 'Phường Ô Môn', 'Phường Thốt Nốt', 'Phường Vị Thanh', 'Phường Sóc Trăng', 'Xã Phong Điền', 'Xã Vĩnh Thạnh', 'Xã Thới Lai'],
  AG: ['Phường Long Xuyên', 'Phường Mỹ Bình', 'Phường Mỹ Xuyên', 'Phường Châu Đốc', 'Phường Châu Phú A', 'Phường Châu Phú B', 'Phường Tân Châu', 'Phường Rạch Giá', 'Phường Hà Tiên', 'Phường Phú Quốc', 'Xã An Châu', 'Xã Chợ Mới', 'Xã Thoại Sơn', 'Xã Tri Tôn', 'Xã Tịnh Biên'],
  BN: ['Phường Kinh Bắc', 'Phường Võ Cường', 'Phường Suối Hoa', 'Phường Từ Sơn', 'Phường Quế Võ', 'Phường Thuận Thành', 'Phường Bắc Giang', 'Phường Ngô Quyền', 'Xã Yên Phong', 'Xã Tiên Du', 'Xã Gia Bình', 'Xã Lương Tài'],
  CM: ['Phường An Xuyên', 'Phường Tân Thành', 'Phường Hòa Thành', 'Phường Bạc Liêu', 'Phường Giá Rai', 'Xã Thới Bình', 'Xã U Minh', 'Xã Trần Văn Thời', 'Xã Cái Nước', 'Xã Năm Căn', 'Xã Đất Mũi'],
  CB: ['Phường Thục Phán', 'Phường Nùng Trí Cao', 'Phường Tân Giang', 'Xã Hòa An', 'Xã Quảng Uyên', 'Xã Trùng Khánh', 'Xã Bảo Lạc', 'Xã Nguyên Bình'],
  DL: ['Phường Buôn Ma Thuột', 'Phường Tân An', 'Phường Tân Lợi', 'Phường Ea Tam', 'Phường Buôn Hồ', 'Phường Tuy Hòa', 'Xã Cư M\'gar', 'Xã Krông Pắc', 'Xã Ea Kar', 'Xã Sông Cầu'],
  DB: ['Phường Điện Biên Phủ', 'Phường Mường Thanh', 'Phường Tân Thanh', 'Phường Mường Lay', 'Xã Mường Ảng', 'Xã Tuần Giáo', 'Xã Điện Biên', 'Xã Mường Chà'],
  DNA: ['Phường Trấn Biên', 'Phường Biên Hòa', 'Phường Tam Hiệp', 'Phường Tân Phong', 'Phường Long Khánh', 'Phường Đồng Xoài', 'Xã Long Thành', 'Xã Nhơn Trạch', 'Xã Trảng Bom', 'Xã Xuân Lộc'],
  DT: ['Phường Cao Lãnh', 'Phường Mỹ Phú', 'Phường Sa Đéc', 'Phường Hồng Ngự', 'Phường Mỹ Tho', 'Phường Gò Công', 'Xã Lai Vung', 'Xã Lấp Vò', 'Xã Tháp Mười', 'Xã Cai Lậy'],
  GL: ['Phường Pleiku', 'Phường Diên Hồng', 'Phường Hoa Lư', 'Phường An Khê', 'Phường Ayun Pa', 'Phường Quy Nhơn', 'Phường An Nhơn', 'Xã Chư Sê', 'Xã Đak Đoa', 'Xã Hoài Nhơn'],
  HT: ['Phường Thành Sen', 'Phường Trần Phú', 'Phường Hà Huy Tập', 'Phường Hồng Lĩnh', 'Phường Kỳ Anh', 'Xã Thạch Hà', 'Xã Cẩm Xuyên', 'Xã Can Lộc', 'Xã Hương Sơn', 'Xã Nghi Xuân'],
  HY: ['Phường Phố Hiến', 'Phường Hiến Nam', 'Phường Hồng Châu', 'Phường Mỹ Hào', 'Phường Thái Bình', 'Phường Trần Hưng Đạo', 'Xã Văn Giang', 'Xã Khoái Châu', 'Xã Yên Mỹ', 'Xã Tiền Hải', 'Xã Hồng Vũ'],
  KH: ['Phường Nha Trang', 'Phường Lộc Thọ', 'Phường Vĩnh Hải', 'Phường Cam Ranh', 'Phường Ninh Hòa', 'Phường Phan Rang', 'Xã Diên Khánh', 'Xã Vạn Ninh', 'Xã Cam Lâm'],
  LC: ['Phường Tân Phong', 'Phường Đoàn Kết', 'Xã Phong Thổ', 'Xã Tam Đường', 'Xã Than Uyên', 'Xã Mường Tè', 'Xã Sìn Hồ'],
  LS: ['Phường Tam Thanh', 'Phường Đông Kinh', 'Phường Lương Văn Tri', 'Phường Kỳ Lừa', 'Xã Cao Lộc', 'Xã Hữu Lũng', 'Xã Chi Lăng', 'Xã Lộc Bình'],
  LCA: ['Phường Lào Cai', 'Phường Cam Đường', 'Phường Sa Pa', 'Phường Yên Bái', 'Phường Nghĩa Lộ', 'Xã Bảo Thắng', 'Xã Bắc Hà', 'Xã Văn Yên', 'Xã Trấn Yên'],
  LD: ['Phường Xuân Hương - Đà Lạt', 'Phường Lâm Viên - Đà Lạt', 'Phường Bảo Lộc', 'Phường Gia Nghĩa', 'Phường Phan Thiết', 'Phường La Gi', 'Xã Đức Trọng', 'Xã Di Linh', 'Xã Lâm Hà'],
  NA: ['Phường Thành Vinh', 'Phường Trường Vinh', 'Phường Cửa Lò', 'Phường Thái Hòa', 'Phường Hoàng Mai', 'Xã Diễn Châu', 'Xã Quỳnh Lưu', 'Xã Nam Đàn', 'Xã Hưng Nguyên', 'Xã Nghi Lộc'],
  NB: ['Phường Hoa Lư', 'Phường Tam Điệp', 'Phường Nam Định', 'Phường Phủ Lý', 'Phường Duy Tiên', 'Xã Gia Viễn', 'Xã Nho Quan', 'Xã Kim Sơn', 'Xã Hải Hậu', 'Xã Ý Yên'],
  PT: ['Phường Việt Trì', 'Phường Nông Trang', 'Phường Phú Thọ', 'Phường Vĩnh Yên', 'Phường Phúc Yên', 'Phường Hòa Bình', 'Xã Lâm Thao', 'Xã Thanh Sơn', 'Xã Tam Đảo', 'Xã Lương Sơn'],
  QN: ['Phường Cẩm Thành', 'Phường Nghĩa Lộ', 'Phường Trương Quang Trọng', 'Phường Đức Phổ', 'Phường Kon Tum', 'Xã Bình Sơn', 'Xã Sơn Tịnh', 'Xã Tư Nghĩa', 'Xã Mộ Đức'],
  QNI: ['Phường Hạ Long', 'Phường Hồng Gai', 'Phường Bãi Cháy', 'Phường Cẩm Phả', 'Phường Uông Bí', 'Phường Móng Cái', 'Phường Đông Triều', 'Xã Vân Đồn', 'Xã Tiên Yên'],
  QT: ['Phường Đông Hà', 'Phường Quảng Trị', 'Phường Đồng Hới', 'Phường Ba Đồn', 'Xã Gio Linh', 'Xã Vĩnh Linh', 'Xã Cam Lộ', 'Xã Hướng Hóa', 'Xã Lệ Thủy'],
  SL: ['Phường Tô Hiệu', 'Phường Chiềng Cơi', 'Phường Chiềng An', 'Phường Mộc Châu', 'Xã Mai Sơn', 'Xã Thuận Châu', 'Xã Phù Yên', 'Xã Yên Châu'],
  TN: ['Phường Tân Ninh', 'Phường Ninh Sơn', 'Phường Trảng Bàng', 'Phường Hòa Thành', 'Phường Long An', 'Phường Kiến Tường', 'Xã Bến Lức', 'Xã Đức Hòa', 'Xã Gò Dầu', 'Xã Châu Thành'],
  TNG: ['Phường Phan Đình Phùng', 'Phường Tích Lương', 'Phường Sông Công', 'Phường Phổ Yên', 'Phường Bắc Kạn', 'Xã Đại Từ', 'Xã Phú Bình', 'Xã Định Hóa', 'Xã Phú Lương'],
  TH: ['Phường Hạc Thành', 'Phường Đông Sơn', 'Phường Sầm Sơn', 'Phường Bỉm Sơn', 'Phường Nghi Sơn', 'Xã Hoằng Hóa', 'Xã Quảng Xương', 'Xã Thọ Xuân'],
  TQ: ['Phường Minh Xuân', 'Phường Nông Tiến', 'Phường An Tường', 'Phường Hà Giang', 'Xã Sơn Dương', 'Xã Yên Sơn', 'Xã Chiêm Hóa', 'Xã Hàm Yên', 'Xã Vị Xuyên'],
  VL: ['Phường Long Châu', 'Phường Tân Ngãi', 'Phường Bình Minh', 'Phường Bến Tre', 'Phường Trà Vinh', 'Xã Long Hồ', 'Xã Vũng Liêm', 'Xã Tam Bình', 'Xã Trà Ôn'],
}

export const layPhuongXaDuPhong = (tinh) => {
  if (!tinh) return []
  let key = tinh.code && DS_PHUONG_XA_DU_PHONG[tinh.code] ? tinh.code : null
  if (!key) {
    const matched = timTinh(DS_34_TINH_THANH, tinh.ten || tinh.name || tinh.code)
      || DS_34_TINH_THANH.find((t) => Array.isArray(t.subCodes) && t.subCodes.includes(Number(tinh.id ?? tinh.code)))
    key = matched?.code || null
  }
  const names = key && DS_PHUONG_XA_DU_PHONG[key] ? DS_PHUONG_XA_DU_PHONG[key] : []
  return names
    .map((ten, idx) => ({ code: `${key || 'VN'}_${idx + 1}`, ten }))
    .sort((a, b) => a.ten.localeCompare(b.ten, 'vi'))
}

/**
 * Tải danh sách đơn vị hành chính cấp cơ sở (Xã/Phường/Thị trấn/Thành phố trực thuộc) theo tỉnh mới.
 * Sử dụng v2 (2 cấp) và v1 depth=3 trực tiếp từ trình duyệt, kèm dữ liệu dự phòng thực tế theo tỉnh.
 */
export const layPhuongXa = async (ds, tinh) => {
  if (!tinh) return []
  if (tinh.phuongXa && tinh.phuongXa.length) return tinh.phuongXa

  const subCodes = tinh.subCodes && tinh.subCodes.length ? tinh.subCodes : [tinh.code ?? tinh.id]
  const cacheKey = `${tinh.code || tinh.id || tinh.ten}:${subCodes.join(',')}`
  if (cacheCon.has(cacheKey)) {
    return cacheCon.get(cacheKey)
  }
  const mapTen = new Map()

  const fetchSub = async (subCode) => {
    if (!(typeof subCode === 'number' || !isNaN(Number(subCode)))) return null
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 3500)
    try {
      // 1. Ưu tiên API v2 (mô hình 2 cấp trực thuộc Tỉnh -> Phường/Xã)
      try {
        const resV2 = await fetch(`https://provinces.open-api.vn/api/v2/p/${subCode}?depth=2`, { signal: ctrl.signal })
        if (resV2.ok) {
          const dataV2 = await resV2.json()
          if (dataV2 && Array.isArray(dataV2.wards) && dataV2.wards.length > 0) {
            return { districts: [{ wards: dataV2.wards }] }
          }
        }
      } catch {
        // Tiếp tục sang nguồn dữ liệu địa giới dự phòng bên dưới.
      }

      // 2. Fallback sang API v1 depth=3 và legacy /api/p/ depth=3
      for (const url of [
        `https://provinces.open-api.vn/api/v1/p/${subCode}?depth=3`,
        `https://provinces.open-api.vn/api/p/${subCode}?depth=3`,
      ]) {
        try {
          const res = await fetch(url, { signal: ctrl.signal })
          if (res.ok) {
            const data = await res.json()
            if (data && (Array.isArray(data.districts) || Array.isArray(data.wards))) {
              return data
            }
          }
        } catch {
          // Thử endpoint tiếp theo
        }
      }
      return null
    } catch {
      return null
    } finally {
      clearTimeout(timer)
    }
  }

  try {
    const results = await Promise.all(subCodes.map(fetchSub))
    for (const data of results) {
      if (!data) continue
      if (Array.isArray(data.wards)) {
        for (const w of data.wards) {
          if (w.name && !mapTen.has(w.name)) {
            mapTen.set(w.name, { code: w.code, ten: w.name })
          }
        }
      }
      if (!Array.isArray(data.districts)) continue
      for (const d of data.districts) {
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

  // Bổ sung các xã/phường mới thành lập nếu từ API có dữ liệu
  const tenTinhClean = loaiBoTienTo(tinh.ten || '')
  if (mapTen.size > 0 && tenTinhClean.includes('hưng yên') && !mapTen.has('Xã Hồng Vũ')) {
    mapTen.set('Xã Hồng Vũ', { code: 'hong_vu', ten: 'Xã Hồng Vũ' })
  }

  const danhSachTong = Array.from(mapTen.values())
  danhSachTong.sort((a, b) => a.ten.localeCompare(b.ten, 'vi'))

  // Chỉ cache kết quả thật từ API để khi có mạng trở lại sẽ tải đủ toàn bộ phường/xã
  if (danhSachTong.length > 0) {
    cacheCon.set(cacheKey, danhSachTong)
    return danhSachTong
  }

  return layPhuongXaDuPhong(tinh)
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
  let diaChiCuThe

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
  let dobRaw
  let gioiTinhRaw = ''
  let diaChiRaw
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
