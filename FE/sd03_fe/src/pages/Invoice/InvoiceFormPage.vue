<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createInvoice,
  getInvoice,
  getInvoiceFormData,
  listInvoices,
  updateInvoice,
} from '@/api/invoice'
import { formatVnd, payStatusOf } from '@/utils/format'
import { notifyError, notifySuccess, notifyWarning } from '@/utils/notify'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const invoiceId = computed(() => route.params.id)

const saving = ref(false)
const loading = ref(false)

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

const initialData = ref(null)
const initialTotal = ref(0)
const initialShippingFee = ref(0)
const initialPaidAmount = ref(0)
const isPaidInAdvance = ref(false)
const initialPaymentMethodName = ref('')

const newMaHoaDon = () => 'HD' + String(Date.now()).slice(-8)

const blankLine = () => ({ idSanPhamChiTiet: '', soLuong: 1, donGia: 0, ghiChu: '' })

const form = ref({
  maHoaDon: newMaHoaDon(),
  loaiDon: 'Online',
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
  toProvinceId: null,
  toDistrictId: null,
  toWardCode: '',
  chiTiet: [blankLine()],
})

const spctById = computed(() => {
  const map = {}
  for (const s of options.value.sanPhamChiTiets) map[s.id] = s
  return map
})

const pggById = computed(() => {
  const map = {}
  for (const p of options.value.phieuGiamGias) map[p.id] = p
  return map
})

const tienHang = computed(() =>
  form.value.chiTiet.reduce(
    (sum, line) => sum + (Number(line.soLuong) || 0) * (Number(line.donGia) || 0),
    0,
  ),
)

const tongTien = computed(() => tienHang.value + (Number(form.value.phiVanChuyen) || 0))

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

// Chênh lệch phí ship
const chenhLechPhiShip = computed(() => {
  if (!isEdit.value) return 0
  return (Number(form.value.phiVanChuyen) || 0) - initialShippingFee.value
})

// Chênh lệch tài chính tổng thể
const chenhLechTongTien = computed(() => {
  if (!isEdit.value) return 0
  return tienSauGiam.value - initialTotal.value
})

// Chênh lệch đối soát với số tiền đã trả trước
const chenhLechTraTruoc = computed(() => {
  if (!isEdit.value) return 0
  return tienSauGiam.value - initialPaidAmount.value
})

const dongLoi = computed(() =>
  Object.keys(errors.value)
    .filter((k) => k.startsWith('dong'))
    .map((k) => ({ dong: Number(k.slice(4)) + 1, msg: errors.value[k] }))
    .sort((a, b) => a.dong - b.dong),
)

const addLine = () => form.value.chiTiet.push(blankLine())

const removeLine = (index) => {
  if (form.value.chiTiet.length === 1) return
  form.value.chiTiet.splice(index, 1)
}

const onProductChange = (line) => {
  const spct = spctById.value[line.idSanPhamChiTiet]
  if (spct) {
    line.donGia = Number(spct.giaBan)
  }
}

const onKhachHangChange = () => {
  const kh = options.value.khachHangs.find((k) => k.id === form.value.idKhachHang)
  if (kh) {
    form.value.tenKhachHang = kh.ten
    form.value.soDienThoaiKhachHang = kh.soDienThoai || ''
  }
}

const toNum = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

const buildPayload = () => ({
  maHoaDon: form.value.maHoaDon.trim(),
  loaiDon: form.value.loaiDon,
  phiVanChuyen: Number(form.value.phiVanChuyen) || 0,
  tenKhachHang: form.value.tenKhachHang.trim() || null,
  soDienThoaiKhachHang: form.value.soDienThoaiKhachHang.trim() || null,
  diaChiNhanHang: form.value.diaChiNhanHang.trim() || null,
  trangThai: form.value.trangThai,
  ghiChu: form.value.ghiChu.trim() || null,
  idKhachHang: toNum(form.value.idKhachHang),
  idNhanVien: toNum(form.value.idNhanVien),
  idPhuongThucThanhToan: toNum(form.value.idPhuongThucThanhToan),
  idPhieuGiamGia: toNum(form.value.idPhieuGiamGia),
  toProvinceId: form.value.toProvinceId || 201,
  toDistrictId: form.value.toDistrictId || 1,
  toWardCode: form.value.toWardCode || '001',
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
  if (form.value.loaiDon !== 'Online' && !form.value.idNhanVien) {
    e.idNhanVien = 'Vui lòng chọn nhân viên'
  }

  const ten = String(form.value.tenKhachHang || '').trim()
  if (!ten) e.tenKhachHang = 'Tên khách hàng không được để trống'
  else if (ten.length > 150) e.tenKhachHang = 'Tên khách hàng quá độ dài tối đa (150 ký tự)'
  else if (!/^[\p{L}\s.'-]+$/u.test(ten)) {
    e.tenKhachHang = "Tên khách hàng chỉ được chứa chữ, khoảng trắng và dấu . ' -"
  }

  const sdt = String(form.value.soDienThoaiKhachHang || '').trim()
  if (!sdt) e.soDienThoaiKhachHang = 'Số điện thoại không được để trống'
  else if (!/^0\d{9,10}$/.test(sdt)) {
    e.soDienThoaiKhachHang = 'Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng 0'
  }

  const phiRaw = form.value.phiVanChuyen
  if (phiRaw === '' || phiRaw === null || phiRaw === undefined) {
    e.phiVanChuyen = 'Phí vận chuyển không được để trống'
  } else {
    const phi = Number(phiRaw)
    if (!Number.isFinite(phi)) e.phiVanChuyen = 'Phí vận chuyển chỉ được nhập số'
    else if (phi < 0) e.phiVanChuyen = 'Phí vận chuyển không được âm'
  }

  const diaChi = String(form.value.diaChiNhanHang || '').trim()
  if (form.value.loaiDon === 'Online' && !diaChi) {
    e.diaChiNhanHang = 'Đơn online phải có địa chỉ nhận hàng'
  } else if (diaChi) {
    if (diaChi.length > 500) {
      e.diaChiNhanHang = 'Địa chỉ nhận hàng quá độ dài tối đa (500 ký tự)'
    } else if (!/^[\p{L}\p{N}\s.,\x2f-]+$/u.test(diaChi)) {
      e.diaChiNhanHang = 'Địa chỉ nhận hàng chứa ký tự không hợp lệ'
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
      if (
        spct &&
        spct.soLuong !== null &&
        spct.soLuong !== undefined &&
        soLuong > Number(spct.soLuong)
      ) {
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
    if (isEdit.value) {
      await updateInvoice(invoiceId.value, payload)
      notifySuccess('Cập nhật hóa đơn thành công!', 'Dữ liệu và lịch sử đơn hàng đã được cập nhật.')
      router.push(`/hoa-don/${invoiceId.value}`)
    } else {
      const ma = form.value.maHoaDon.trim()
      const kiemTra = await listInvoices({ ma, size: 100 })
      if ((kiemTra.content || []).some((h) => h.maHoaDon === ma)) {
        errors.value = { ...errors.value, maHoaDon: `Mã hóa đơn "${ma}" đã tồn tại` }
        return
      }
      const res = await createInvoice(payload)
      notifySuccess('Tạo hóa đơn thành công!')
      router.push(`/hoa-don/${res.id || ''}`)
    }
  } catch (e) {
    notifyError(isEdit.value ? 'Cập nhật thất bại' : 'Tạo hóa đơn thất bại', e.message || '')
  } finally {
    saving.value = false
  }
}

const optionLabel = (s) =>
  `${s.ma} - ${s.tenSanPham} (${s.tenKichCo} / ${s.tenMau}) - ${formatVnd(s.giaBan)} - Còn: ${s.soLuong}` +
  (s.trangThai === false || s.trangThaiSanPham === false ? ' - ngừng bán' : '')

const getStockOf = (idSpct) => {
  const s = spctById.value[idSpct]
  return s?.soLuong ?? null
}

const quayLai = () => {
  if (isEdit.value) {
    router.push(`/hoa-don/${invoiceId.value}`)
  } else {
    router.push('/hoa-don')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    options.value = await getInvoiceFormData()

    if (isEdit.value) {
      const res = await getInvoice(invoiceId.value)
      if (res.trangThai !== 'Chờ xác nhận' && res.maTrangThai !== 'CHO_XAC_NHAN') {
        notifyWarning('Chỉ hóa đơn Chờ xác nhận mới được phép chỉnh sửa!')
        router.push(`/hoa-don/${invoiceId.value}`)
        return
      }
      initialData.value = res
      initialTotal.value = Number(res.tienSauGiamGia) || 0
      initialShippingFee.value = Number(res.phiVanChuyen) || 0
      initialPaymentMethodName.value = res.tenPhuongThucThanhToan || ''

      const payStatus = payStatusOf(res)
      const ptttName = String(res.tenPhuongThucThanhToan || '').toUpperCase()
      const isPaidStatus =
        payStatus === 'Đã thanh toán' ||
        res.trangThaiThanhToan === 'DA_THANH_TOAN' ||
        ptttName.includes('VNPAY') ||
        ptttName.includes('CHUYỂN KHOẢN') ||
        ptttName.includes('TRANSFER')
      isPaidInAdvance.value = isPaidStatus
      initialPaidAmount.value = isPaidStatus ? initialTotal.value : 0

      form.value = {
        maHoaDon: res.maHoaDon,
        loaiDon: res.loaiDon || 'Online',
        phiVanChuyen: Number(res.phiVanChuyen) || 0,
        tenKhachHang: res.tenKhachHang || '',
        soDienThoaiKhachHang: res.soDienThoaiKhachHang || '',
        diaChiNhanHang: res.diaChiNhanHang || '',
        trangThai: res.trangThai || 'Chờ xác nhận',
        ghiChu: res.ghiChu || '',
        idKhachHang: res.idKhachHang || '',
        idNhanVien: res.idNhanVien || '',
        idPhuongThucThanhToan: res.idPhuongThucThanhToan || '',
        idPhieuGiamGia: res.idPhieuGiamGia || '',
        toProvinceId: res.toProvinceId || 201,
        toDistrictId: res.toDistrictId || 1,
        toWardCode: res.toWardCode || '001',
        chiTiet: (res.chiTiet || []).map((c) => ({
          idSanPhamChiTiet: c.idSanPhamChiTiet,
          soLuong: c.soLuong,
          donGia: c.donGia,
          ghiChu: c.ghiChu || '',
        })),
      }
      if (form.value.chiTiet.length === 0) {
        form.value.chiTiet.push(blankLine())
      }
    }
  } catch (e) {
    notifyError('Lỗi tải dữ liệu', e.message || '')
    if (isEdit.value) router.push('/hoa-don')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="create-wrap">
    <div class="breadcrumb-bar">
      <router-link to="/hoa-don" class="bc-link">Quản lý hóa đơn</router-link>
      <span class="bc-sep">/</span>
      <span class="bc-cur">{{ isEdit ? `Sửa hóa đơn ${form.maHoaDon}` : 'Tạo hóa đơn' }}</span>
    </div>

    <section v-if="!loading" class="create-card">
      <div class="card-top-title">
        <h2 class="create-title">
          {{ isEdit ? `Chỉnh sửa hóa đơn: ${form.maHoaDon}` : 'Tạo mới hóa đơn' }}
        </h2>
        <span v-if="isEdit" class="mode-badge">Chờ xác nhận - Bán hàng Online</span>
      </div>

      <!-- Banner tổng quan khi sửa đơn hàng -->
      <div v-if="isEdit && initialData" class="overview-banner">
        <div class="ob-col">
          <span class="ob-label">Mã hóa đơn:</span>
          <strong class="ob-val text-primary">{{ form.maHoaDon }}</strong>
        </div>
        <div class="ob-col">
          <span class="ob-label">Phương thức thanh toán:</span>
          <strong class="ob-val">{{ initialPaymentMethodName || 'Chưa xác định' }}</strong>
        </div>
        <div class="ob-col">
          <span class="ob-label">Trạng thái thanh toán:</span>
          <span
            class="ob-pill"
            :class="isPaidInAdvance ? 'ob-pill-paid' : 'ob-pill-unpaid'"
          >
            {{ isPaidInAdvance ? 'Đã thanh toán trước' : 'Chưa thanh toán (COD)' }}
          </span>
        </div>
        <div class="ob-col">
          <span class="ob-label">Tổng thanh toán ban đầu:</span>
          <strong class="ob-val text-dark">{{ formatVnd(initialTotal) }}</strong>
        </div>
      </div>

      <!-- 1. THÔNG TIN CÁ NHÂN & ĐỊA CHỈ NHẬN HÀNG -->
      <div class="section-label">
        <span class="sec-num">1</span>
        THÔNG TIN CÁ NHÂN &amp; ĐỊA CHỈ NHẬN HÀNG
      </div>

      <div class="form-grid">
        <div class="field">
          <label>Mã hóa đơn *</label>
          <input v-model="form.maHoaDon" type="text" placeholder="Mã hóa đơn" readonly />
          <div v-if="errors.maHoaDon" class="field-error">{{ errors.maHoaDon }}</div>
        </div>

        <div class="field">
          <label>Loại đơn *</label>
          <select v-model="form.loaiDon" :disabled="isEdit">
            <option value="Online">Online</option>
            <option value="Tại quầy">Tại quầy</option>
          </select>
          <div v-if="errors.loaiDon" class="field-error">{{ errors.loaiDon }}</div>
        </div>

        <div class="field">
          <label>Trạng thái</label>
          <input :value="form.trangThai" type="text" readonly />
          <div v-if="errors.trangThai" class="field-error">{{ errors.trangThai }}</div>
        </div>

        <div class="field">
          <label>Phương thức thanh toán *</label>
          <select v-model="form.idPhuongThucThanhToan">
            <option value="">— Không chọn —</option>
            <option
              v-for="p in options.phuongThucThanhToans"
              :key="p.id"
              :value="p.id"
            >
              {{ p.ten }}
            </option>
          </select>
          <div v-if="errors.idPhuongThucThanhToan" class="field-error">
            {{ errors.idPhuongThucThanhToan }}
          </div>
        </div>

        <div v-if="form.loaiDon !== 'Online'" class="field">
          <label>Nhân viên phụ trách *</label>
          <select v-model="form.idNhanVien">
            <option value="">— Không chọn —</option>
            <option v-for="n in options.nhanViens" :key="n.id" :value="n.id">{{ n.ten }}</option>
          </select>
          <div v-if="errors.idNhanVien" class="field-error">{{ errors.idNhanVien }}</div>
        </div>

        <div class="field">
          <label>Tài khoản khách hàng</label>
          <select v-model="form.idKhachHang" @change="onKhachHangChange">
            <option value="">— Khách lẻ / Đặt online —</option>
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
          <label>Tên người nhận hàng *</label>
          <input
            v-model="form.tenKhachHang"
            type="text"
            placeholder="Họ tên người nhận hàng..."
          />
          <div v-if="errors.tenKhachHang" class="field-error">{{ errors.tenKhachHang }}</div>
        </div>

        <div class="field">
          <label>Số điện thoại nhận hàng *</label>
          <input
            v-model="form.soDienThoaiKhachHang"
            type="tel"
            placeholder="Số điện thoại nhận hàng (10-11 số)..."
          />
          <div v-if="errors.soDienThoaiKhachHang" class="field-error">
            {{ errors.soDienThoaiKhachHang }}
          </div>
        </div>

        <div class="field">
          <label class="d-flex justify-content-between align-items-center">
            <span>Phí vận chuyển (₫) *</span>
            <span v-if="isEdit && chenhLechPhiShip !== 0" class="fee-diff-tag" :class="chenhLechPhiShip > 0 ? 'up' : 'down'">
              {{ chenhLechPhiShip > 0 ? `+${formatVnd(chenhLechPhiShip)}` : `-${formatVnd(Math.abs(chenhLechPhiShip))}` }}
            </span>
          </label>
          <input
            v-model.number="form.phiVanChuyen"
            type="number"
            min="0"
            step="1000"
            placeholder="Phí vận chuyển..."
          />
          <small v-if="isEdit" class="field-hint">
            Phí gốc: <strong>{{ formatVnd(initialShippingFee) }}</strong> → Phí mới: <strong>{{ formatVnd(form.phiVanChuyen) }}</strong>
          </small>
          <div v-if="errors.phiVanChuyen" class="field-error">{{ errors.phiVanChuyen }}</div>
        </div>

        <div class="field">
          <label>Phiếu giảm giá (Voucher)</label>
          <select v-model="form.idPhieuGiamGia">
            <option value="">— Không áp dụng —</option>
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

        <div class="field field-wide">
          <label>Địa chỉ nhận hàng chi tiết *</label>
          <input
            v-model="form.diaChiNhanHang"
            type="text"
            placeholder="Số nhà, tên đường, thôn/xóm, phường/xã, quận/huyện, tỉnh/thành phố..."
          />
          <div v-if="errors.diaChiNhanHang" class="field-error">{{ errors.diaChiNhanHang }}</div>
        </div>

        <div class="field field-wide">
          <label>Ghi chú đơn hàng</label>
          <textarea
            v-model="form.ghiChu"
            rows="2"
            placeholder="Ghi chú về giao hàng, yêu cầu sửa đổi của khách..."
          ></textarea>
          <div v-if="errors.ghiChu" class="field-error">{{ errors.ghiChu }}</div>
        </div>
      </div>

      <!-- 2. THÔNG TIN SẢN PHẨM TRONG ĐƠN HÀNG -->
      <div class="section-label">
        <span class="sec-num">2</span>
        THÔNG TIN SẢN PHẨM TRONG ĐƠN HÀNG
      </div>

      <div class="table-responsive">
        <table class="line-table">
          <thead>
            <tr>
              <th class="col-stt">STT</th>
              <th class="col-sp">Sản phẩm chi tiết (Biến thể) *</th>
              <th class="col-ton">Tồn kho</th>
              <th class="col-sl">Số lượng</th>
              <th class="col-gia">Đơn giá (₫)</th>
              <th class="col-tt">Thành tiền</th>
              <th class="col-x">Xóa</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(line, i) in form.chiTiet" :key="i">
              <td class="col-stt text-center text-muted">{{ i + 1 }}</td>
              <td class="col-sp">
                <select v-model="line.idSanPhamChiTiet" @change="onProductChange(line)">
                  <option value="">— Chọn sản phẩm / biến thể —</option>
                  <option
                    v-for="s in options.sanPhamChiTiets"
                    :key="s.id"
                    :value="s.id"
                    :disabled="s.trangThai === false || s.trangThaiSanPham === false"
                  >
                    {{ optionLabel(s) }}
                  </option>
                </select>
              </td>
              <td class="col-ton text-center">
                <span
                  v-if="line.idSanPhamChiTiet"
                  class="stock-tag"
                  :class="Number(getStockOf(line.idSanPhamChiTiet)) > 0 ? 'stock-ok' : 'stock-out'"
                >
                  Còn {{ getStockOf(line.idSanPhamChiTiet) ?? '—' }}
                </span>
                <span v-else class="text-muted">—</span>
              </td>
              <td class="col-sl">
                <input
                  v-model.number="line.soLuong"
                  type="number"
                  min="1"
                  class="input-qty"
                />
              </td>
              <td class="col-gia">
                <input
                  v-model.number="line.donGia"
                  type="number"
                  min="0"
                  step="1000"
                  class="input-price"
                />
              </td>
              <td class="col-tt amount text-end">
                {{ formatVnd(lineThanhTien(line)) }}
              </td>
              <td class="col-x text-center">
                <button
                  class="line-remove"
                  type="button"
                  title="Xóa dòng sản phẩm"
                  :disabled="form.chiTiet.length === 1"
                  @click="removeLine(i)"
                >
                  ×
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <button class="line-add" type="button" @click="addLine">
        + Thêm sản phẩm vào đơn hàng
      </button>

      <div v-if="errors.chiTiet" class="field-error">{{ errors.chiTiet }}</div>
      <ul v-if="dongLoi.length" class="line-error-list">
        <li v-for="lo in dongLoi" :key="lo.dong">Dòng {{ lo.dong }}: {{ lo.msg }}</li>
      </ul>

      <!-- 3. ĐỐI SOÁT & XỬ LÝ CHÊNH LỆCH THANH TOÁN (PHỤ PHÍ & HOÀN PHÍ) -->
      <div class="section-label">
        <span class="sec-num">3</span>
        ĐỐI SOÁT &amp; XỬ LÝ CHÊNH LỆCH THANH TOÁN (PHỤ PHÍ &amp; HOÀN PHÍ)
      </div>

      <div class="finance-panel">
        <!-- Cột tóm tắt tài chính -->
        <div class="totals-box">
          <div class="t-row">
            <span>Tiền hàng:</span>
            <strong>{{ formatVnd(tienHang) }}</strong>
          </div>
          <div class="t-row">
            <span>Phí vận chuyển:</span>
            <div class="d-flex align-items-center gap-1">
              <strong>{{ formatVnd(form.phiVanChuyen) }}</strong>
              <small v-if="isEdit && chenhLechPhiShip !== 0" class="fee-diff-mini" :class="chenhLechPhiShip > 0 ? 'up' : 'down'">
                ({{ chenhLechPhiShip > 0 ? '+' : '' }}{{ formatVnd(chenhLechPhiShip) }})
              </small>
            </div>
          </div>
          <div class="t-row">
            <span>Giảm giá (Voucher):</span>
            <span class="t-discount">− {{ formatVnd(giamGia) }}</span>
          </div>
          <div v-if="canhbaoPgg" class="t-warning">{{ canhbaoPgg }}</div>

          <div class="t-row t-final">
            <span>Tổng thanh toán mới:</span>
            <span class="t-final-val">{{ formatVnd(tienSauGiam) }}</span>
          </div>

          <div v-if="isEdit" class="t-row t-initial">
            <span>Tổng thanh toán cũ:</span>
            <span class="text-muted">{{ formatVnd(initialTotal) }}</span>
          </div>
        </div>

        <!-- Cột xử lý đối soát Phụ phí / Hoàn phí -->
        <div v-if="isEdit" class="reconcile-card">
          <!-- Trường hợp 1: Đơn COD (Chưa thanh toán) -->
          <div v-if="!isPaidInAdvance" class="reconcile-box cod-box">
            <div class="rc-head">
              <span class="rc-badge cod-badge">ĐƠN HÀNG COD (CHƯA THANH TOÁN)</span>
            </div>
            <div class="rc-body">
              <p class="rc-text">
                Khách hàng chọn thanh toán khi nhận hàng. Khi thay đổi địa chỉ, phí ship hoặc danh sách sản phẩm:
              </p>
              <div class="rc-highlight">
                <span>Số tiền thu hộ (COD) shipper cần thu mới là:</span>
                <strong class="highlight-val">{{ formatVnd(tienSauGiam) }}</strong>
              </div>
              <p class="rc-sub">
                Hệ thống sẽ cập nhật số tiền COD mới cho đơn hàng khi shipper tiến hành giao.
              </p>
            </div>
          </div>

          <!-- Trường hợp 2: Đơn đã thanh toán trước (VNPAY / Chuyển khoản) -->
          <div v-else class="reconcile-box prepaid-box">
            <div class="rc-head">
              <span class="rc-badge prepaid-badge">ĐƠN ĐÃ THANH TOÁN TRƯỚC ({{ initialPaymentMethodName || 'VNPAY' }})</span>
            </div>
            <div class="rc-body">
              <div class="rc-paid-line">
                <span>Số tiền khách đã thanh toán trước:</span>
                <strong>{{ formatVnd(initialPaidAmount) }}</strong>
              </div>

              <!-- TH2a: Phụ phí thu thêm -->
              <div v-if="chenhLechTraTruoc > 0" class="diff-alert diff-alert-surcharge">
                <div class="diff-alert-title">
                  <span class="diff-icon">⚠️</span>
                  PHÁT SINH PHỤ PHÍ THU THÊM: +{{ formatVnd(chenhLechTraTruoc) }}
                </div>
                <p class="diff-desc">
                  Tổng tiền mới lớn hơn số tiền khách đã thanh toán trước.
                  <br />
                  👉 Cần thu thêm phụ phí <strong>{{ formatVnd(chenhLechTraTruoc) }}</strong> bằng hình thức COD khi shipper giao hàng hoặc hướng dẫn khách chuyển khoản bổ sung.
                </p>
              </div>

              <!-- TH2b: Hoàn phí cho khách -->
              <div v-else-if="chenhLechTraTruoc < 0" class="diff-alert diff-alert-refund">
                <div class="diff-alert-title">
                  <span class="diff-icon">💳</span>
                  CỬA HÀNG CẦN HOÀN PHÍ CHO KHÁCH: −{{ formatVnd(Math.abs(chenhLechTraTruoc)) }}
                </div>
                <p class="diff-desc">
                  Tổng tiền mới nhỏ hơn số tiền khách đã thanh toán trước.
                  <br />
                  👉 Cửa hàng cần thực hiện hoàn trả lại <strong>{{ formatVnd(Math.abs(chenhLechTraTruoc)) }}</strong> cho khách hàng qua tài khoản ngân hàng.
                </p>
              </div>

              <!-- TH2c: Không đổi -->
              <div v-else class="diff-alert diff-alert-neutral">
                <div class="diff-alert-title">
                  <span class="diff-icon">✅</span>
                  TỔNG TIỀN KHÔNG THAY ĐỔI
                </div>
                <p class="diff-desc">
                  Tổng tiền mới bằng đúng số tiền khách đã thanh toán trước ({{ formatVnd(initialPaidAmount) }}). Không phát sinh phụ phí hay hoàn phí.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="form-actions">
        <button class="btn-cancel" type="button" @click="quayLai">
          Quay lại
        </button>
        <button
          class="btn-primary"
          type="button"
          :disabled="saving"
          @click="submit"
        >
          <svg
            v-if="isEdit"
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            class="me-1"
          >
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
            <polyline points="17 21 17 13 7 13 7 21"></polyline>
            <polyline points="7 3 7 8 15 8"></polyline>
          </svg>
          {{ saving ? 'Đang lưu...' : (isEdit ? 'Lưu thay đổi đơn hàng' : 'Tạo hóa đơn') }}
        </button>
      </div>
    </section>

    <section v-else class="create-card">
      <p class="loading-text">Đang tải dữ liệu hóa đơn...</p>
    </section>
  </div>
</template>

<style scoped>
.create-wrap {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 16px 40px;
}

.breadcrumb-bar {
  width: min(960px, 100%);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 12px;
}

.bc-link {
  color: #2563eb;
  text-decoration: none;
}
.bc-link:hover {
  text-decoration: underline;
}
.bc-sep {
  color: #94a3b8;
}
.bc-cur {
  color: #64748b;
  font-weight: 500;
}

.create-card {
  width: min(960px, 100%);
  background: var(--white, #ffffff);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
  padding: 24px 30px 30px;
}

.card-top-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 10px;
}

.create-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.mode-badge {
  background: #fef3c7;
  color: #b45309;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid #fde68a;
}

.overview-banner {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 22px;
}

.ob-col {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ob-label {
  font-size: 11.5px;
  color: #64748b;
}

.ob-val {
  font-size: 13.5px;
  color: #1e293b;
}

.ob-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11.5px;
  font-weight: 600;
  width: fit-content;
}

.ob-pill-paid {
  background: #dcfce7;
  color: #15803d;
}

.ob-pill-unpaid {
  background: #eff6ff;
  color: #1d4ed8;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin: 22px 0 14px;
  padding-left: 8px;
  border-left: 3px solid #2563eb;
  letter-spacing: 0.2px;
}

.sec-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: #2563eb;
  color: #fff;
  border-radius: 50%;
  font-size: 11px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px 16px;
  margin-bottom: 16px;
}

.field label {
  display: block;
  font-size: 12px;
  color: #475569;
  margin-bottom: 5px;
  font-weight: 600;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.field textarea {
  height: auto;
  min-height: 60px;
  padding: 8px 12px;
  resize: vertical;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.field-wide {
  grid-column: span 3;
}

.field-error {
  font-size: 11.5px;
  color: #dc2626;
  margin-top: 4px;
}

.field-hint {
  font-size: 11px;
  color: #64748b;
  margin-top: 3px;
  display: block;
}

.fee-diff-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
}

.fee-diff-tag.up {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.fee-diff-tag.down {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.field input[readonly] {
  background: #f1f5f9;
  color: #64748b;
  cursor: not-allowed;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
  margin-bottom: 12px;
}

.line-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.line-table th {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  background: #f8fafc;
  padding: 10px 10px;
  border-bottom: 1px solid #e2e8f0;
}

.line-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.line-table select {
  width: 100%;
  height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12.5px;
}

.input-qty {
  width: 70px;
  height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
}

.input-price {
  width: 110px;
  height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  text-align: right;
  font-size: 13px;
  padding: 0 8px;
}

.col-stt {
  width: 44px;
}
.col-ton {
  width: 100px;
}
.col-sl {
  width: 90px;
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

.stock-tag {
  display: inline-block;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}
.stock-ok {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}
.stock-out {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.amount {
  font-weight: 700;
  color: #0f172a;
  font-size: 13px;
}

.line-remove {
  border: none;
  background: #fee2e2;
  color: #dc2626;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.line-remove:hover:not(:disabled) {
  background: #fecaca;
  color: #991b1b;
}

.line-remove:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.line-add {
  border: 1px dashed #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  width: 100%;
  height: 38px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 20px;
  transition: all 0.15s ease;
}

.line-add:hover {
  background: #dbeafe;
}

.finance-panel {
  display: grid;
  grid-template-columns: 1fr 1.35fr;
  gap: 20px;
  align-items: start;
  margin-bottom: 24px;
}

.totals-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.t-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13.5px;
  color: #475569;
}

.t-discount {
  color: #dc2626;
  font-weight: 600;
}

.t-warning {
  font-size: 12px;
  color: #b45309;
  background: #fef3c7;
  border-radius: 6px;
  padding: 6px 10px;
}

.t-final {
  font-weight: 700;
  color: #0f172a;
  font-size: 14.5px;
  border-top: 1px dashed #cbd5e1;
  padding-top: 10px;
  margin-top: 4px;
}

.t-final-val {
  color: #2563eb;
  font-size: 16px;
}

.t-initial {
  font-size: 12px;
}

.fee-diff-mini.up {
  color: #dc2626;
  font-weight: 600;
}
.fee-diff-mini.down {
  color: #16a34a;
  font-weight: 600;
}

.reconcile-card {
  height: 100%;
}

.reconcile-box {
  border-radius: 10px;
  padding: 16px 18px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.cod-box {
  border-left: 4px solid #2563eb;
  background: #f8fafc;
}

.prepaid-box {
  border-left: 4px solid #7c3aed;
  background: #faf5ff;
}

.rc-head {
  margin-bottom: 10px;
}

.rc-badge {
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
  padding: 3px 8px;
  border-radius: 4px;
}

.cod-badge {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.prepaid-badge {
  background: #f3e8ff;
  color: #6b21a8;
  border: 1px solid #ddd6fe;
}

.rc-text {
  font-size: 13px;
  color: #475569;
  margin-bottom: 12px;
  line-height: 1.5;
}

.rc-highlight {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 8px;
  font-size: 13px;
}

.highlight-val {
  font-size: 17px;
  color: #1d4ed8;
}

.rc-sub {
  font-size: 11.5px;
  color: #64748b;
  margin: 0;
}

.rc-paid-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #475569;
  padding-bottom: 10px;
  border-bottom: 1px solid #e9d5ff;
  margin-bottom: 12px;
}

.diff-alert {
  border-radius: 8px;
  padding: 12px 14px;
}

.diff-alert-surcharge {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.diff-alert-refund {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1e40af;
}

.diff-alert-neutral {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.diff-alert-title {
  font-size: 13.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.diff-desc {
  font-size: 12px;
  margin: 0;
  line-height: 1.5;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid #f1f5f9;
  padding-top: 18px;
}

.btn-primary {
  height: 40px;
  min-width: 140px;
  padding: 0 24px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel {
  height: 40px;
  min-width: 90px;
  padding: 0 20px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #475569;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-cancel:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  color: #0f172a;
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
  color: #dc2626;
  background: #fef2f2;
  border-radius: 6px;
  padding: 6px 9px;
}
</style>
