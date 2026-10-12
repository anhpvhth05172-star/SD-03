<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  createInvoice,
  getInvoiceFormData,
  listInvoices,
} from '@/api/invoice'
import { getDistricts, getProvinces, getWards, tinhPhi } from '@/api/shipping'
import ProductPickerModal from '@/components/ProductPickerModal.vue'
import { notifySuccess, notifyWarning, notifyError } from '@/utils/notify'
import { formatVnd } from '@/utils/format'

const router = useRouter()
const saving = ref(false)
const loading = ref(false)
const pickerOpen = ref(false)

const statusOptions = [
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
  'Đã hủy',
  'Đã hoàn tiền',
]

const options = ref({
  khachHangs: [],
  nhanViens: [],
  phuongThucThanhToans: [],
  phieuGiamGias: [],
  sanPhamChiTiets: [],
})

const newMaHoaDon = () => 'HD' + String(Date.now()).slice(-8)

const blankLine = () => ({ idSanPhamChiTiet: '', soLuong: 1, donGia: 0, ghiChu: '' })

const form = ref({
  maHoaDon: newMaHoaDon(),
  loaiDon: 'Tại quầy',
  phiVanChuyen: 0,
  tenKhachHang: '',
  soDienThoaiKhachHang: '',
  diaChiNhanHang: '',
  trangThai: 'Chờ xác nhận',
  ghiChu: '',
  idKhachHang: '',
  idNhanVien: '',
  idPhuongThucThanhToan: '',
  idPhieuGiamGia: '',
  chiTiet: [blankLine()],
})

const spctById = computed(() => {
  const map = {}
  for (const s of options.value.sanPhamChiTiets) map[s.id] = s
  return map
})

const existingLineIds = computed(() =>
  form.value.chiTiet.map((l) => Number(l.idSanPhamChiTiet)).filter((n) => n > 0),
)

const pggById = computed(() => {
  const map = {}
  for (const p of options.value.phieuGiamGias) map[p.id] = p
  return map
})

const tongTien = computed(() =>
  form.value.chiTiet.reduce(
    (sum, line) => sum + (Number(line.soLuong) || 0) * (Number(line.donGia) || 0),
    0,
  ) + (Number(form.value.phiVanChuyen) || 0),
)

const giamGia = computed(() => {
  const pgg = pggById.value[form.value.idPhieuGiamGia]
  if (!pgg) return 0
  let giam =
    pgg.loaiGiamGia === 'PERCENT'
      ? Math.floor((tongTien.value * Number(pgg.giaTriGiam)) / 100)
      : Number(pgg.giaTriGiam) || 0
  if (Number(pgg.giamToiDa) > 0) giam = Math.min(giam, Number(pgg.giamToiDa))
  return Math.max(0, Math.min(giam, tongTien.value))
})

const canhbaoPgg = computed(() => {
  const pgg = pggById.value[form.value.idPhieuGiamGia]
  if (!pgg) return ''
  if (pgg.trangThai === false) return 'Phiếu giảm giá này không hoạt động'
  if (!form.value.idKhachHang) return 'Vui lòng chọn khách hàng để sử dụng phiếu giảm giá'
  if (
    pgg.soLuong !== null &&
    pgg.soLuong !== undefined &&
    Number(pgg.soLuongDaSuDung || 0) >= Number(pgg.soLuong)
  ) {
    return 'Phiếu giảm giá đã hết lượt sử dụng'
  }
  if (Number(pgg.hoaDonToiThieu) > 0 && tongTien.value < Number(pgg.hoaDonToiThieu)) {
    return `Hóa đơn cần từ ${formatVnd(pgg.hoaDonToiThieu)} mới dùng phiếu này`
  }
  const now = Date.now()
  if (pgg.ngayBatDau && now < new Date(pgg.ngayBatDau).getTime()) return 'Phiếu chưa đến ngày sử dụng'
  if (pgg.ngayKetThuc && now > new Date(pgg.ngayKetThuc).getTime()) return 'Phiếu đã hết hạn'
  return ''
})

const tienSauGiam = computed(() => Math.max(0, tongTien.value - giamGia.value))

const lineThanhTien = (line) => (Number(line.soLuong) || 0) * (Number(line.donGia) || 0)

const dongLoi = computed(() =>
  Object.keys(errors.value)
    .filter((k) => k.startsWith('dong'))
    .map((k) => ({ dong: Number(k.slice(4)) + 1, msg: errors.value[k] }))
    .sort((a, b) => a.dong - b.dong),
)

const addLine = () => {
  pickerOpen.value = true
}

const removeLine = (index) => {
  if (form.value.chiTiet.length === 1) return
  form.value.chiTiet.splice(index, 1)
}

const onKhachHangChange = () => {
  const kh = options.value.khachHangs.find((k) => k.id === form.value.idKhachHang)
  if (kh) {
    form.value.tenKhachHang = kh.ten
    form.value.soDienThoaiKhachHang = kh.soDienThoai || ''
  }
}

const laOnline = computed(() => form.value.loaiDon === 'Online')

const diaChi = ref({
  tinhId: '',
  tinhTen: '',
  quanId: '',
  quanTen: '',
  phuongCode: '',
  phuongTen: '',
  pho: '',
  soNha: '',
})
const tinhThanhs = ref([])
const quanHuyens = ref([])
const phuongXas = ref([])
const diaChiLoading = ref({ tinh: false, quan: false, phuong: false })
const diaChiLoi = ref('')

const phiState = ref('none')
const phiError = ref('')
let phiYeuCau = 0
let phiTimer = null

const phiHienThi = computed(() => {
  if (phiState.value === 'loading') return 'Đang tính phí...'
  if (phiState.value === 'ok') return formatVnd(form.value.phiVanChuyen)
  return ''
})

const chiTietSignature = computed(() =>
  form.value.chiTiet.map((l) => `${l.idSanPhamChiTiet}x${l.soLuong}`).join('|'),
)

const taiTinhThanh = async () => {
  if (diaChiLoading.value.tinh) return
  diaChiLoading.value.tinh = true
  diaChiLoi.value = ''
  try {
    tinhThanhs.value = await getProvinces()
  } catch (e) {
    tinhThanhs.value = []
    diaChiLoi.value = e.message || 'Không tải được danh sách tỉnh/thành'
  } finally {
    diaChiLoading.value.tinh = false
  }
}

const taiQuanHuyen = async (provinceId) => {
  diaChiLoading.value.quan = true
  diaChiLoi.value = ''
  try {
    quanHuyens.value = await getDistricts(provinceId)
  } catch (e) {
    quanHuyens.value = []
    diaChiLoi.value = e.message || 'Không tải được danh sách quận/huyện'
  } finally {
    diaChiLoading.value.quan = false
  }
}

const taiPhuongXa = async (districtId) => {
  diaChiLoading.value.phuong = true
  diaChiLoi.value = ''
  try {
    phuongXas.value = await getWards(districtId)
  } catch (e) {
    phuongXas.value = []
    diaChiLoi.value = e.message || 'Không tải được danh sách phường/xã'
  } finally {
    diaChiLoading.value.phuong = false
  }
}

const capNhatDiaChiText = () => {
  if (!laOnline.value) return
  const duongDay = [diaChi.value.soNha, diaChi.value.pho]
    .map((s) => String(s || '').trim())
    .filter(Boolean)
    .join(' ')
  const parts = [
    duongDay,
    diaChi.value.phuongTen,
    diaChi.value.quanTen,
    diaChi.value.tinhTen,
  ]
    .map((s) => String(s || '').trim())
    .filter(Boolean)
  form.value.diaChiNhanHang = parts.join(', ')
}

const xoaPhi = () => {
  phiYeuCau++
  clearTimeout(phiTimer)
  form.value.phiVanChuyen = 0
  phiState.value = 'none'
  phiError.value = ''
}

const coTheTinhPhi = () =>
  laOnline.value &&
  diaChi.value.quanId &&
  diaChi.value.phuongCode &&
  form.value.chiTiet.length > 0 &&
  form.value.chiTiet.every((l) => l.idSanPhamChiTiet && Number(l.soLuong) > 0)

const yeuCauTinhPhi = (delay = 400) => {
  clearTimeout(phiTimer)
  if (!laOnline.value) {
    phiYeuCau++
    phiState.value = 'none'
    phiError.value = ''
    return
  }
  if (!coTheTinhPhi()) {
    xoaPhi()
    return
  }
  phiState.value = 'loading'
  phiError.value = ''
  form.value.phiVanChuyen = 0
  phiTimer = setTimeout(() => tinhPhiNgay(), delay)
}

const tinhPhiNgay = async () => {
  clearTimeout(phiTimer)
  if (!coTheTinhPhi()) {
    xoaPhi()
    return
  }
  const yeuCauHienTai = ++phiYeuCau
  phiState.value = 'loading'
  phiError.value = ''
  form.value.phiVanChuyen = 0
  try {
    const kq = await tinhPhi({
      toDistrictId: Number(diaChi.value.quanId),
      toWardCode: String(diaChi.value.phuongCode),
      items: form.value.chiTiet.map((l) => ({
        idSanPhamChiTiet: Number(l.idSanPhamChiTiet),
        soLuong: Number(l.soLuong),
      })),
    })
    if (yeuCauHienTai !== phiYeuCau) return
    const phi = Number(kq.shippingFee) || 0
    if (phi > 1000) {
      form.value.phiVanChuyen = phi
      phiState.value = 'ok'
      phiError.value = ''
    } else {
      form.value.phiVanChuyen = 0
      phiState.value = 'error'
      phiError.value = kq.message || 'GHN trả về phí vận chuyển không hợp lệ'
    }
  } catch (e) {
    if (yeuCauHienTai !== phiYeuCau) return
    form.value.phiVanChuyen = 0
    phiState.value = 'error'
    phiError.value = e.message || 'Không tính được phí vận chuyển'
  }
}

const onTinhChange = async () => {
  const t = tinhThanhs.value.find((x) => x.id === Number(diaChi.value.tinhId))
  diaChi.value.tinhTen = t ? t.name : ''
  diaChi.value.quanId = ''
  diaChi.value.quanTen = ''
  diaChi.value.phuongCode = ''
  diaChi.value.phuongTen = ''
  quanHuyens.value = []
  phuongXas.value = []
  capNhatDiaChiText()
  xoaPhi()
  if (diaChi.value.tinhId) await taiQuanHuyen(Number(diaChi.value.tinhId))
}

const onQuanChange = async () => {
  const q = quanHuyens.value.find((x) => x.id === Number(diaChi.value.quanId))
  diaChi.value.quanTen = q ? q.name : ''
  diaChi.value.phuongCode = ''
  diaChi.value.phuongTen = ''
  phuongXas.value = []
  capNhatDiaChiText()
  xoaPhi()
  if (diaChi.value.quanId) await taiPhuongXa(Number(diaChi.value.quanId))
}

const onPhuongChange = () => {
  const w = phuongXas.value.find((x) => String(x.code) === String(diaChi.value.phuongCode))
  diaChi.value.phuongTen = w ? w.name : ''
  capNhatDiaChiText()
  yeuCauTinhPhi(0)
}

const onPhoSoNhaChange = () => capNhatDiaChiText()

watch(
  () => form.value.loaiDon,
  (moi) => {
    if (moi === 'Online') {
      if (!tinhThanhs.value.length) taiTinhThanh()
      yeuCauTinhPhi(0)
    } else {
      phiYeuCau++
      clearTimeout(phiTimer)
      phiState.value = 'none'
      phiError.value = ''
    }
  },
)

watch(chiTietSignature, () => {
  if (laOnline.value) yeuCauTinhPhi(500)
})

const toNum = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

const buildPayload = () => ({
  maHoaDon: form.value.maHoaDon.trim(),
  loaiDon: form.value.loaiDon,
  phiVanChuyen: Number(form.value.phiVanChuyen) || 0,
  tenKhachHang: form.value.tenKhachHang.trim() || null,
  soDienThoaiKhachHang: form.value.soDienThoaiKhachHang.trim() || null,
  diaChiNhanHang: form.value.diaChiNhanHang.trim() || null,
  toProvinceId: laOnline.value ? toNum(diaChi.value.tinhId) : null,
  toDistrictId: laOnline.value ? toNum(diaChi.value.quanId) : null,
  toWardCode: laOnline.value ? (diaChi.value.phuongCode || null) : null,
  trangThai: form.value.trangThai,
  ghiChu: form.value.ghiChu.trim() || null,
  idKhachHang: toNum(form.value.idKhachHang),
  idNhanVien: toNum(form.value.idNhanVien),
  idPhuongThucThanhToan: toNum(form.value.idPhuongThucThanhToan),
  idPhieuGiamGia: toNum(form.value.idPhieuGiamGia),
  chiTiet: form.value.chiTiet.map((line) => ({
    idSanPhamChiTiet: toNum(line.idSanPhamChiTiet),
    soLuong: Number(line.soLuong),
    donGia: Number(line.donGia),
    ghiChu: line.ghiChu ? line.ghiChu.trim() : null,
    trangThai: true,
  })),
})

const errors = ref({})

const coKyTuDieuKhien = (s) =>
  [...s].some((c) => {
    const n = c.charCodeAt(0)
    return n <= 8 || n === 11 || n === 12 || (n >= 14 && n <= 31) || n === 127
  })

const validate = () => {
  const e = {}
  const ma = String(form.value.maHoaDon || '').trim()
  if (!ma) e.maHoaDon = 'Mã hóa đơn không được để trống'
  else if (ma.length > 50) e.maHoaDon = 'Mã hóa đơn quá độ dài tối đa (50 ký tự)'

  if (!form.value.loaiDon) e.loaiDon = 'Vui lòng chọn loại đơn'
  if (!form.value.trangThai) e.trangThai = 'Vui lòng chọn trạng thái'
  if (!form.value.idPhuongThucThanhToan) {
    e.idPhuongThucThanhToan = 'Vui lòng chọn phương thức thanh toán'
  }
  if (!form.value.idNhanVien) e.idNhanVien = 'Vui lòng chọn nhân viên'

  const ten = String(form.value.tenKhachHang || '').trim()
  if (!ten) e.tenKhachHang = 'Tên khách hàng không được để trống'
  else if (ten.length > 150) e.tenKhachHang = 'Tên khách hàng quá độ dài tối đa (150 ký tự)'
  else if (!/^[\p{L}\s.'-]+$/u.test(ten)) {
    e.tenKhachHang = 'Tên khách hàng chỉ được chứa chữ, khoảng trắng và dấu . \' -'
  }

  const sdt = String(form.value.soDienThoaiKhachHang || '').trim()
  if (!sdt) e.soDienThoaiKhachHang = 'Số điện thoại không được để trống'
  else if (!/^0\d{9,10}$/.test(sdt)) {
    e.soDienThoaiKhachHang = 'Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng 0'
  }

  if (laOnline.value) {
    if (phiState.value === 'loading') {
      e.phiVanChuyen = 'Đang tính phí vận chuyển, vui lòng chờ'
    } else if (phiState.value === 'error') {
      e.phiVanChuyen = phiError.value || 'Chưa tính được phí vận chuyển từ GHN'
    } else if (phiState.value !== 'ok' || !(Number(form.value.phiVanChuyen) > 1000)) {
      e.phiVanChuyen = 'Phí vận chuyển chưa được xác nhận'
    }
  } else {
    const phiRaw = form.value.phiVanChuyen
    if (phiRaw === '' || phiRaw === null || phiRaw === undefined) {
      e.phiVanChuyen = 'Phí vận chuyển không được để trống'
    } else {
      const phi = Number(phiRaw)
      if (!Number.isFinite(phi)) e.phiVanChuyen = 'Phí vận chuyển chỉ được nhập số'
      else if (phi < 0) e.phiVanChuyen = 'Phí vận chuyển không được âm'
      else if (phi <= 1000) e.phiVanChuyen = 'Phí vận chuyển phải lớn hơn 1000'
    }
  }

  if (laOnline.value) {
    if (!diaChi.value.tinhId) e.diaChiNhanHang = 'Vui lòng chọn tỉnh/thành phố'
    else if (!diaChi.value.quanId) e.diaChiNhanHang = 'Vui lòng chọn quận/huyện'
    else if (!diaChi.value.phuongCode) e.diaChiNhanHang = 'Vui lòng chọn phường/xã'
    else if (!String(diaChi.value.pho || '').trim()) {
      e.diaChiNhanHang = 'Vui lòng nhập tên phố'
    } else if (!String(diaChi.value.soNha || '').trim()) {
      e.diaChiNhanHang = 'Vui lòng nhập số nhà'
    }
  }
  if (!e.diaChiNhanHang) {
    const diaChiText = String(form.value.diaChiNhanHang || '').trim()
    if (laOnline.value && !diaChiText) {
      e.diaChiNhanHang = 'Đơn online phải có địa chỉ nhận hàng'
    } else if (diaChiText) {
      if (diaChiText.length > 500) {
        e.diaChiNhanHang = 'Địa chỉ nhận hàng quá độ dài tối đa (500 ký tự)'
      } else if (!/^[\p{L}\p{N}\s.,\/-]+$/u.test(diaChiText)) {
        e.diaChiNhanHang = 'Địa chỉ nhận hàng chứa ký tự không hợp lệ'
      }
    }
  }

  const ghiChu = String(form.value.ghiChu || '').trim()
  if (ghiChu.length > 1000) e.ghiChu = 'Ghi chú quá độ dài tối đa (1000 ký tự)'
  else if (coKyTuDieuKhien(ghiChu)) e.ghiChu = 'Ghi chú chứa ký tự không hợp lệ'

  if (!form.value.chiTiet.length) {
    e.chiTiet = 'Vui lòng thêm ít nhất một sản phẩm'
  } else {
    const daChon = new Set()
    form.value.chiTiet.forEach((line, i) => {
      if (e['dong' + i]) return
      if (!line.idSanPhamChiTiet) {
        e['dong' + i] = 'Vui lòng chọn sản phẩm'
        return
      }
      if (daChon.has(line.idSanPhamChiTiet)) {
        const sp = spctById.value[line.idSanPhamChiTiet]
        e['dong' + i] = `Sản phẩm "${sp ? sp.ma : line.idSanPhamChiTiet}" đã tồn tại ở dòng trước`
        return
      }
      daChon.add(line.idSanPhamChiTiet)

      const soLuongTrong =
        line.soLuong === '' || line.soLuong === null || line.soLuong === undefined
      const soLuong = Number(line.soLuong)
      if (soLuongTrong || !Number.isInteger(soLuong) || soLuong < 1) {
        e['dong' + i] = 'Số lượng phải là số nguyên lớn hơn 0'
        return
      }

      const donGiaTrong =
        line.donGia === '' || line.donGia === null || line.donGia === undefined
      const donGia = Number(line.donGia)
      if (donGiaTrong || !Number.isFinite(donGia) || donGia <= 0) {
        e['dong' + i] = 'Đơn giá phải lớn hơn 0'
        return
      }

      const spct = spctById.value[line.idSanPhamChiTiet]
      if (spct && (spct.trangThai === false || spct.trangThaiSanPham === false)) {
        e['dong' + i] = `Sản phẩm "${spct.ma}" đã ngừng bán`
        return
      }
      if (spct && spct.soLuong !== null && spct.soLuong !== undefined && soLuong > Number(spct.soLuong)) {
        e['dong' + i] = `Sản phẩm "${spct.ma}" chỉ còn ${spct.soLuong} trong kho`
      }
    })
  }

  if (form.value.idKhachHang) {
    const kh = options.value.khachHangs.find((k) => k.id === form.value.idKhachHang)
    if (kh && kh.trangThai === false) e.khachHang = 'Khách hàng đã bị vô hiệu hóa'
  }

  if (canhbaoPgg.value) e.idPhieuGiamGia = canhbaoPgg.value

  errors.value = e
  return Object.keys(e).length === 0
}

const submit = async () => {
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload()
    const ma = form.value.maHoaDon.trim()
    const kiemTra = await listInvoices({ ma, size: 100 })
    if ((kiemTra.content || []).some((h) => h.maHoaDon === ma)) {
      errors.value = { ...errors.value, maHoaDon: `Mã hóa đơn "${ma}" đã tồn tại` }
      saving.value = false
      return
    }
    await createInvoice(payload)
    router.push('/hoa-don')
  } catch (e) {
    notifyError('Không thể tạo hóa đơn', e.message || 'Có lỗi xảy ra')
  } finally {
    saving.value = false
  }
}

const huy = () => {
  router.push('/hoa-don')
}

const xoaLoiDong = () => {
  const e = { ...errors.value }
  for (const k of Object.keys(e)) {
    if (k.startsWith('dong') || k === 'chiTiet') delete e[k]
  }
  errors.value = e
}

const onPickProducts = (ids) => {
  const daChon = new Set(
    form.value.chiTiet
      .filter((l) => l.idSanPhamChiTiet)
      .map((l) => Number(l.idSanPhamChiTiet)),
  )
  const chiTietMoi = form.value.chiTiet.filter((l) => l.idSanPhamChiTiet)
  let them = 0
  let boQua = 0
  for (const id of ids) {
    const key = Number(id)
    if (daChon.has(key)) {
      boQua++
      continue
    }
    const spct = spctById.value[id]
    chiTietMoi.push({
      idSanPhamChiTiet: id,
      soLuong: 1,
      donGia: spct ? Number(spct.giaBan) || 0 : 0,
      ghiChu: '',
    })
    daChon.add(key)
    them++
  }
  form.value.chiTiet = chiTietMoi.length ? chiTietMoi : [blankLine()]
  pickerOpen.value = false
  xoaLoiDong()
  if (them && !boQua) {
    notifySuccess('Đã thêm sản phẩm', `Đã thêm ${them} dòng sản phẩm vào hóa đơn`)
  } else if (them && boQua) {
    notifyWarning(
      'Đã thêm sản phẩm',
      `Thêm ${them} dòng, bỏ qua ${boQua} sản phẩm đã có trong hóa đơn`,
    )
  } else if (boQua) {
    notifyWarning('Không có sản phẩm mới', `${boQua} sản phẩm đã có trong hóa đơn`)
  }
}

onMounted(async () => {
  loading.value = true
  try {
    options.value = await getInvoiceFormData()
  } catch (e) {
    notifyError('Không tải được dữ liệu', e.message)
    loading.value = false
    return
  }
  loading.value = false
})
</script>

<template>
  <div class="create-wrap">
    <section class="create-card" v-if="!loading">
      <h2 class="create-title">Tạo hóa đơn</h2>

      <div class="section-label">Thông tin hóa đơn</div>
      <div class="form-grid">
        <div class="field">
          <label>Mã hóa đơn *</label>
          <input v-model="form.maHoaDon" type="text" placeholder="Mã hóa đơn" readonly />
          <div v-if="errors.maHoaDon" class="field-error">{{ errors.maHoaDon }}</div>
        </div>
        <div class="field">
          <label>Loại đơn *</label>
          <select v-model="form.loaiDon">
            <option value="Tại quầy">Tại quầy</option>
            <option value="Online">Online</option>
          </select>
          <div v-if="errors.loaiDon" class="field-error">{{ errors.loaiDon }}</div>
        </div>
        <div class="field">
          <label>Trạng thái *</label>
          <select v-model="form.trangThai">
            <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
          </select>
          <div v-if="errors.trangThai" class="field-error">{{ errors.trangThai }}</div>
        </div>
        <div class="field">
          <label>Phương thức thanh toán *</label>
          <select v-model="form.idPhuongThucThanhToan">
            <option value="">— Không chọn —</option>
            <option v-for="p in options.phuongThucThanhToans" :key="p.id" :value="p.id">{{ p.ten }}</option>
          </select>
          <div v-if="errors.idPhuongThucThanhToan" class="field-error">{{ errors.idPhuongThucThanhToan }}</div>
        </div>
        <div class="field">
          <label>Nhân viên *</label>
          <select v-model="form.idNhanVien">
            <option value="">— Không chọn —</option>
            <option v-for="n in options.nhanViens" :key="n.id" :value="n.id">{{ n.ten }}</option>
          </select>
          <div v-if="errors.idNhanVien" class="field-error">{{ errors.idNhanVien }}</div>
        </div>
        <div class="field">
          <label>Khách hàng</label>
          <select v-model="form.idKhachHang" @change="onKhachHangChange">
            <option value="">— Khách lẻ —</option>
            <option
              v-for="k in options.khachHangs"
              :key="k.id"
              :value="k.id"
              :disabled="k.trangThai === false"
            >
              {{ k.ten }} ({{ k.soDienThoai }}){{ k.trangThai === false ? ' - đã vô hiệu hóa' : '' }}
            </option>
          </select>
          <div v-if="errors.khachHang" class="field-error">{{ errors.khachHang }}</div>
        </div>
        <div class="field">
          <label>Tên khách hàng *</label>
          <input v-model="form.tenKhachHang" type="text" placeholder="Tên khách hàng" />
          <div v-if="errors.tenKhachHang" class="field-error">{{ errors.tenKhachHang }}</div>
        </div>
        <div class="field">
          <label>Số điện thoại *</label>
          <input v-model="form.soDienThoaiKhachHang" type="tel" placeholder="Số điện thoại" />
          <div v-if="errors.soDienThoaiKhachHang" class="field-error">{{ errors.soDienThoaiKhachHang }}</div>
        </div>
        <div class="field">
          <label>Phí vận chuyển (₫) *</label>
          <template v-if="laOnline">
            <div class="phi-wrap">
              <input
                class="phi-input"
                :value="phiHienThi"
                type="text"
                readonly
                placeholder="Chưa xác định"
              />
              <button
                v-if="phiState === 'error'"
                class="phi-retry"
                type="button"
                @click="tinhPhiNgay"
              >
                Thử lại
              </button>
            </div>
            <div v-if="phiState === 'loading'" class="phi-hint">Đang tính phí với GHN...</div>
            <div v-else-if="phiState === 'error'" class="phi-hint is-error">{{ phiError }}</div>
            <div v-else-if="phiState === 'ok'" class="phi-hint is-ok">
              Phí GHN đã xác nhận
            </div>
            <div v-else class="phi-hint">Chọn đủ địa chỉ và sản phẩm để tính phí</div>
          </template>
          <input
            v-else
            v-model.number="form.phiVanChuyen"
            type="number"
            min="1001"
            placeholder="0"
          />
          <div v-if="errors.phiVanChuyen" class="field-error">{{ errors.phiVanChuyen }}</div>
        </div>
        <div class="field">
          <label>Phiếu giảm giá</label>
          <select v-model="form.idPhieuGiamGia">
            <option value="">— Không dùng —</option>
            <option
              v-for="p in options.phieuGiamGias"
              :key="p.id"
              :value="p.id"
              :disabled="p.trangThai === false"
            >
              {{ p.ten }}{{ p.trangThai === false ? ' (ngừng hoạt động)' : '' }}
            </option>
          </select>
          <div v-if="errors.idPhieuGiamGia" class="field-error">{{ errors.idPhieuGiamGia }}</div>
        </div>
        <div v-if="laOnline" class="field field-wide">
          <label>Địa chỉ nhận hàng *</label>
          <div class="addr-grid">
            <select
              v-model="diaChi.tinhId"
              :disabled="diaChiLoading.tinh"
              @change="onTinhChange"
            >
              <option value="">
                {{ diaChiLoading.tinh ? 'Đang tải...' : '— Tỉnh/Thành —' }}
              </option>
              <option v-for="t in tinhThanhs" :key="t.id" :value="t.id">{{ t.name }}</option>
            </select>
            <select
              v-model="diaChi.quanId"
              :disabled="!diaChi.tinhId || diaChiLoading.quan"
              @change="onQuanChange"
            >
              <option value="">
                {{ diaChiLoading.quan ? 'Đang tải...' : '— Quận/Huyện —' }}
              </option>
              <option v-for="q in quanHuyens" :key="q.id" :value="q.id">{{ q.name }}</option>
            </select>
            <select
              v-model="diaChi.phuongCode"
              :disabled="!diaChi.quanId || diaChiLoading.phuong"
              @change="onPhuongChange"
            >
              <option value="">
                {{ diaChiLoading.phuong ? 'Đang tải...' : '— Chọn Phường/Xã —' }}
              </option>
              <option v-for="w in phuongXas" :key="w.code" :value="w.code">{{ w.name }}</option>
            </select>
            <input
              v-model="diaChi.pho"
              class="addr-pho"
              type="text"
              placeholder="Tên phố (VD: Duong So 1)"
              @input="onPhoSoNhaChange"
            />
            <input
              v-model="diaChi.soNha"
              class="addr-so-nha"
              type="text"
              placeholder="Số nhà (VD: 123)"
              @input="onPhoSoNhaChange"
            />
          </div>
          <div v-if="diaChiLoi" class="field-error">{{ diaChiLoi }}</div>
          <input
            class="diaChi-preview"
            :value="form.diaChiNhanHang"
            type="text"
            readonly
            placeholder="Địa chỉ đầy đủ hiển thị tại đây"
          />
          <div v-if="errors.diaChiNhanHang" class="field-error">{{ errors.diaChiNhanHang }}</div>
        </div>
        <div v-else class="field field-wide">
          <label>Địa chỉ nhận hàng</label>
          <input v-model="form.diaChiNhanHang" type="text" placeholder="Địa chỉ nhận hàng" />
          <div v-if="errors.diaChiNhanHang" class="field-error">{{ errors.diaChiNhanHang }}</div>
        </div>
        <div class="field field-wide">
          <label>Ghi chú</label>
          <textarea v-model="form.ghiChu" rows="2" placeholder="Ghi chú"></textarea>
          <div v-if="errors.ghiChu" class="field-error">{{ errors.ghiChu }}</div>
        </div>
      </div>

      <div class="section-label">Chi tiết hóa đơn</div>
      <div class="picker-toolbar">
        <button class="line-add line-add--strong" type="button" @click="pickerOpen = true">
          + Chọn sản phẩm (chọn nhiều)
        </button>
        <span class="picker-hint">Dùng checkbox để chọn nhiều biến thể cùng lúc, chống trùng SKU tự động.</span>
      </div>
      <table class="line-table">
        <thead>
          <tr>
            <th class="col-sp">Sản phẩm *</th>
            <th class="col-sl">SL</th>
            <th class="col-gia">Đơn giá (₫)</th>
            <th class="col-tt">Thành tiền</th>
            <th class="col-x"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(line, i) in form.chiTiet" :key="i">
            <td class="col-sp">
              <div class="line-product" :class="{ 'is-empty': !line.idSanPhamChiTiet }">
                <template v-if="line.idSanPhamChiTiet && spctById[line.idSanPhamChiTiet]">
                  <span class="line-sku">{{ spctById[line.idSanPhamChiTiet].ma }}</span>
                  <span class="line-name">{{ spctById[line.idSanPhamChiTiet].tenSanPham }}</span>
                  <span class="line-variant">
                    {{ [spctById[line.idSanPhamChiTiet].tenKichCo, spctById[line.idSanPhamChiTiet].tenMau].filter(Boolean).join(' / ') }}
                  </span>
                  <span class="line-stock">còn {{ spctById[line.idSanPhamChiTiet].soLuong }}</span>
                </template>
                <span v-else class="line-placeholder">Chưa chọn sản phẩm — bấm nút ở trên để thêm</span>
              </div>
            </td>
            <td class="col-sl"><input v-model.number="line.soLuong" type="number" min="1" /></td>
            <td class="col-gia"><input v-model.number="line.donGia" type="number" min="0" step="1000" /></td>
            <td class="col-tt amount">{{ formatVnd(lineThanhTien(line)) }}</td>
            <td class="col-x">
              <button
                class="line-remove"
                type="button"
                aria-label="Xóa dòng"
                :disabled="form.chiTiet.length === 1"
                @click="removeLine(i)"
              >
                ×
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="errors.chiTiet" class="field-error">{{ errors.chiTiet }}</div>
      <ul v-if="dongLoi.length" class="line-error-list">
        <li v-for="lo in dongLoi" :key="lo.dong">Dòng {{ lo.dong }}: {{ lo.msg }}</li>
      </ul>

      <ProductPickerModal
        :is-open="pickerOpen"
        :products="options.sanPhamChiTiets"
        :existing-ids="existingLineIds"
        @close="pickerOpen = false"
        @confirm="onPickProducts"
      />

      <div class="totals-box">
        <div class="t-row"><span>Tổng tiền</span><span>{{ formatVnd(tongTien) }}</span></div>
        <div class="t-row"><span>Giảm giá</span><span class="t-discount">- {{ formatVnd(giamGia) }}</span></div>
        <div v-if="canhbaoPgg" class="t-warning">{{ canhbaoPgg }}</div>
        <div class="t-row t-final"><span>Tiền sau giảm giá</span><span>{{ formatVnd(tienSauGiam) }}</span></div>
      </div>

      <div class="form-actions">
        <button class="btn-primary" type="button" :disabled="saving" @click="submit">
          {{ saving ? 'Đang lưu...' : 'Tạo hóa đơn' }}
        </button>
        <button class="btn-cancel" type="button" @click="huy">Hủy</button>
      </div>
    </section>
    <section class="create-card" v-else>
      <p class="loading-text">Đang tải...</p>
    </section>
  </div>
</template>

<style scoped>
.create-wrap {
  min-height: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 26px 12px 40px;
}

.create-card {
  width: min(880px, 100%);
  background: var(--white);
  border: 1px solid #ececef;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(20, 20, 22, 0.05);
  padding: 28px 36px 30px;
}

.create-title {
  text-align: center;
  font-size: 15.5px;
  font-weight: 700;
  color: #1d1d23;
  margin-bottom: 20px;
}

.loading-text {
  text-align: center;
  color: #9a9aa3;
  padding: 30px 0;
}

.section-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--red);
  margin: 4px 0 12px;
  padding-left: 9px;
  border-left: 3px solid var(--red);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 13px 16px;
  margin-bottom: 22px;
}

.field label {
  display: block;
  font-size: 11.5px;
  color: #77777f;
  margin-bottom: 4px;
  font-weight: 500;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  height: 38px;
  padding: 0 13px;
  border: 1px solid #e3e3e8;
  border-radius: 7px;
  background: #fff;
  font-size: 13px;
  color: #4a4a52;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.field textarea {
  height: auto;
  min-height: 62px;
  padding: 10px 13px;
  resize: vertical;
  line-height: 1.5;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.field-wide {
  grid-column: span 3;
}

.addr-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 8px;
}

.diaChi-preview {
  background: #f7f7f9;
  color: #4a4a52;
  font-weight: 500;
}

.phi-wrap {
  display: flex;
  gap: 8px;
  align-items: center;
}

.phi-input {
  flex: 1;
  font-weight: 600;
  color: #1d1d23;
}

.phi-retry {
  height: 38px;
  padding: 0 13px;
  border: 1px solid var(--red);
  background: #fff;
  color: var(--red);
  border-radius: 7px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.phi-retry:hover {
  background: var(--red-soft);
}

.phi-hint {
  font-size: 11.5px;
  color: #77777f;
  margin-top: 4px;
  line-height: 1.35;
}

.phi-hint.is-error {
  color: var(--red);
}

.phi-hint.is-ok {
  color: #15803d;
}

.field-error {
  font-size: 11.5px;
  color: var(--red);
  margin-top: 4px;
  line-height: 1.35;
}

.field input[readonly] {
  background: #f7f7f9;
  color: #6b6b73;
  cursor: default;
}

.field select:disabled {
  background: #f7f7f9;
  color: #6b6b73;
  cursor: default;
  opacity: 1;
}

.line-error-list {
  list-style: none;
  margin: 0 0 14px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.line-error-list li {
  font-size: 12px;
  color: var(--red);
  background: var(--red-soft);
  border-radius: 6px;
  padding: 6px 9px;
}

.line-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 10px;
}

.line-table th {
  font-size: 11.5px;
  color: #77777f;
  text-align: left;
  padding: 8px 8px;
  border-bottom: 1px solid #ececef;
}

.line-table td {
  padding: 8px;
  border-bottom: 1px solid #f3f3f5;
  vertical-align: middle;
}

.line-table input,
.line-table select {
  width: 100%;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #e3e3e8;
  border-radius: 7px;
  font-size: 12.5px;
  color: #4a4a52;
  background: #fff;
  outline: none;
}

.line-table input:focus,
.line-table select:focus {
  border-color: var(--red);
}

.col-sl {
  width: 78px;
}

.col-gia {
  width: 130px;
}

.col-tt {
  width: 130px;
}

.col-x {
  width: 44px;
}

.amount {
  font-weight: 600;
  color: #1d1d23;
  font-size: 13px;
}

.line-remove {
  border: none;
  background: #f3f3f5;
  color: #6b7280;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
}

.line-remove:hover:not(:disabled) {
  background: var(--red-soft);
  color: var(--red);
}

.line-remove:disabled {
  opacity: 0.35;
  cursor: default;
}

.line-add {
  border: 1px dashed #d5d5dc;
  background: #fafafd;
  color: var(--red);
  font-size: 12.5px;
  font-weight: 600;
  width: 100%;
  height: 36px;
  border-radius: 7px;
  cursor: pointer;
  margin-bottom: 18px;
  transition: background 0.15s ease;
}

.line-add:hover {
  background: var(--red-soft);
}

.picker-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.line-add--strong {
  width: auto;
  margin-bottom: 0;
  padding: 0 16px;
  border-style: solid;
  border-color: var(--red);
  background: var(--red-soft);
}

.line-add--strong:hover {
  background: #fdecec;
  color: var(--red-dark);
}

.picker-hint {
  font-size: 12px;
  color: #8a8a94;
}

.line-product {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.line-product.is-empty .line-placeholder {
  color: #9a9aa3;
  font-style: italic;
  font-size: 12.5px;
}

.line-sku {
  font-size: 11px;
  font-weight: 700;
  color: #6b6b73;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.line-name {
  font-size: 13px;
  font-weight: 600;
  color: #1d1d23;
}

.line-variant {
  font-size: 12px;
  color: #4a4a52;
}

.line-stock {
  font-size: 11px;
  color: #15803d;
  font-weight: 600;
}

.totals-box {
  width: min(340px, 100%);
  margin-left: auto;
  background: #fafafd;
  border: 1px solid #ececef;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.t-row {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  color: #4a4a52;
}

.t-discount {
  color: var(--red);
  font-weight: 600;
}

.t-warning {
  font-size: 12px;
  color: #b45309;
  background: #fef3c7;
  border-radius: 6px;
  padding: 6px 9px;
}

.t-final {
  font-weight: 700;
  color: #1d1d23;
  font-size: 14.5px;
  border-top: 1px dashed #d5d5dc;
  padding-top: 8px;
}

.t-final span:last-child {
  color: var(--red);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-primary {
  height: 36px;
  min-width: 92px;
  padding: 0 22px;
  border: none;
  border-radius: 7px;
  background: var(--red);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-primary:hover:not(:disabled) {
  background: var(--red-dark);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: default;
}

.btn-cancel {
  height: 36px;
  min-width: 76px;
  padding: 0 18px;
  border: none;
  border-radius: 7px;
  background: #6b7280;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-cancel:hover {
  background: #59606b;
}
</style>
