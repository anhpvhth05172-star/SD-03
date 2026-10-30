<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import { useRoute, useRouter } from 'vue-router'
import { checkCustomerEmail, createCustomer, getCustomer, suggestCustomerCode, updateCustomer } from '@/api/customer'
import { layDiaGioi, layPhuongXa, phanTichDiaChiCccd, timTinh } from '@/api/diaGioi'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { setFlash } from '@/utils/flash'
import { hopLeMa, maDeXuat } from '@/utils/maNhanVien'
import { layNguoiDung } from '@/utils/session'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import {
  duTuoiToiThieu,
  emailHopLe,
  isBlank,
  kiemTra,
  khongDuocTuongLai,
  soDienThoaiHopLe,
  toiDa,
} from '@/utils/validate'

const TUOI_TOI_THIEU = 16
const THONG_BAO_TUOI = 'Khách hàng phải đủ 16 tuổi.'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id
const saving = ref(false)
const loading = ref(false)
const errorMsg = ref('')

/** Chỉ Nam/Nữ theo yêu cầu; value giữ nguyên NAM/NU để khớp payload API. */
const gioiTinhOptions = [
  { value: 'NAM', label: 'Nam' },
  { value: 'NU', label: 'Nữ' },
]
const trangThais = [
  { value: 'true', label: 'Đang hoạt động' },
  { value: 'false', label: 'Đã khóa' },
]

const tinhThanhs = ref([])
const phuongXas = ref([])
const diaGioiLoading = ref(false)
const diaGioiError = ref('')
const maTuDong = ref(true)
const dangGoiMa = ref(false)
const tenDaGoiMa = ref('')
const daNopForm = ref(false)
const ngaySinhDaCham = ref(false)
const emailTrungLoi = ref('')
let yeuCauHienTai = 0
let yeuCauMa = 0

const form = ref({
  maKhachHang: '',
  tenKhachHang: '',
  tenTaiKhoan: '',
  matKhau: '',
  email: '',
  soDienThoai: '',
  ngaySinh: '',
  gioiTinh: '',
  trangThai: 'true',
  tinhThanhPho: '',
  phuong: '',
  diaChiCuThe: '',
  anhDaiDien: '',
})

const kiemTraEmailTrung = async () => {
  const em = (form.value.email || '').trim()
  if (!em || !emailHopLe(em)) {
    emailTrungLoi.value = ''
    return
  }
  try {
    const res = await checkCustomerEmail(em, isEdit ? route.params.id : null)
    if (res && res.exists) {
      emailTrungLoi.value = 'Email này đã được sử dụng bởi khách hàng khác'
    } else {
      emailTrungLoi.value = ''
    }
  } catch {
    emailTrungLoi.value = ''
  }
}

const maPreview = computed(() => (form.value.tenKhachHang.trim() ? maDeXuat(form.value.tenKhachHang) : ''))

const loiNgaySinh = computed(() =>
  kiemTra([
    khongDuocTuongLai(form.value.ngaySinh, 'Ngày sinh'),
    duTuoiToiThieu(form.value.ngaySinh, TUOI_TOI_THIEU, THONG_BAO_TUOI),
  ]),
)

/** Hiện lỗi ngay khi đã chọn ngày sinh; với ô trống chỉ hiện sau submit/blur. */
const hienThiLoiNgaySinh = computed(() =>
  !isBlank(form.value.ngaySinh) || daNopForm.value || ngaySinhDaCham.value
    ? loiNgaySinh.value
    : '',
)

const loiGioiTinh = computed(() => (isBlank(form.value.gioiTinh) ? 'Vui lòng chọn giới tính' : ''))

const loiSoDienThoai = computed(() =>
  kiemTra([
    toiDa(form.value.soDienThoai, 10, 'Số điện thoại'),
    soDienThoaiHopLe(form.value.soDienThoai),
  ]),
)

/** Đánh dấu người dùng đã thao tác với trường (blur/change) để hiện lỗi ngay tại chỗ. */
const chamTruong = ref({})
const cham = (ten) => {
  chamTruong.value[ten] = true
}
/** Chỉ hiện lỗi khi trường đã được thao tác hoặc sau lần gửi form đầu tiên. */
const hienLoi = (ten, loi) => (daNopForm.value || chamTruong.value[ten] ? loi : '')

const loiTen = computed(() =>
  kiemTra([
    isBlank(form.value.tenKhachHang) ? 'Vui lòng nhập họ và tên' : '',
    toiDa(form.value.tenKhachHang, 150, 'Họ và tên'),
  ]),
)

const loiEmail = computed(() =>
  kiemTra([
    isBlank(form.value.email) ? 'Vui lòng nhập email' : '',
    toiDa(form.value.email, 150, 'Email'),
    emailHopLe(form.value.email),
  ]),
)

const loiMaKhachHang = computed(() =>
  kiemTra([
    toiDa(form.value.maKhachHang, 25, 'Mã khách hàng'),
    form.value.maKhachHang.trim() && !hopLeMa(form.value.maKhachHang)
      ? 'Mã khách hàng không hợp lệ (bắt đầu bằng chữ cái, chỉ chữ cái và số, tối đa 25 ký tự)'
      : '',
  ]),
)

const loiTinhThanhPho = computed(() => toiDa(form.value.tinhThanhPho, 100, 'Tỉnh/Thành phố'))
const loiPhuong = computed(() => toiDa(form.value.phuong, 100, 'Phường/Xã'))
const loiDiaChiCuThe = computed(() => toiDa(form.value.diaChiCuThe, 255, 'Địa chỉ cụ thể'))

const validate = () =>
  kiemTra([
    loiMaKhachHang.value,
    loiTen.value,
    loiEmail.value,
    emailTrungLoi.value,
    loiSoDienThoai.value,
    loiNgaySinh.value,
    loiGioiTinh.value,
    loiTinhThanhPho.value,
    loiPhuong.value,
    loiDiaChiCuThe.value,
  ])

const buildPayload = () => ({
  maKhachHang: form.value.maKhachHang.trim() || null,
  tenKhachHang: form.value.tenKhachHang.trim(),
  matKhau: form.value.matKhau ? form.value.matKhau : null,
  email: form.value.email.trim(),
  soDienThoai: form.value.soDienThoai.trim() || null,
  ngaySinh: form.value.ngaySinh || null,
  gioiTinh: form.value.gioiTinh || null,
  trangThai: form.value.trangThai === 'true',
  tinhThanhPho: form.value.tinhThanhPho || null,
  phuong: form.value.phuong || null,
  diaChiCuThe: form.value.diaChiCuThe.trim() || null,
  nguoiCapNhat: layNguoiDung(),
})

const chonMaTuDong = async (khiNop = false) => {
  if (isEdit || !maTuDong.value) return
  const ten = form.value.tenKhachHang.trim()
  if (!ten) return
  if (!khiNop && dangGoiMa.value) return
  const myToken = ++yeuCauMa
  dangGoiMa.value = true
  try {
    const data = await suggestCustomerCode(ten)
    if (myToken === yeuCauMa && maTuDong.value) {
      form.value.maKhachHang = data.ma || maDeXuat(ten)
      tenDaGoiMa.value = ten
    }
  } catch {
    if (myToken === yeuCauMa && maTuDong.value) {
      form.value.maKhachHang = maDeXuat(ten)
      tenDaGoiMa.value = ten
    }
  } finally {
    dangGoiMa.value = false
  }
}


// Modal & Camera quét mã CCCD
const isScanModalOpen = ref(false)
const scanTab = ref('camera') // 'camera' | 'usb'
const isCameraRunning = ref(false)
const cameraError = ref('')
const rawCccdInput = ref('')
const diaChiCccdGoc = ref('')
let html5QrCode = null

const playBeep = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.type = 'sine'
    osc.frequency.value = 1200
    gain.gain.setValueAtTime(0.15, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.18)
    osc.start()
    osc.stop(ctx.currentTime + 0.18)
  } catch {}
}

const apDungDiaChiCccd = async (diaChiRaw) => {
  if (!diaChiRaw) return
  diaChiCccdGoc.value = diaChiRaw
  const parsed = phanTichDiaChiCccd(diaChiRaw)

  // 1. Tỉnh/Thành phố
  if (parsed.tinh) {
    const ds = tinhThanhs.value.length ? tinhThanhs.value : await layDiaGioi()
    tinhThanhs.value = ds
    let tinh = timTinh(ds, parsed.tinh)
    if (tinh) {
      form.value.tinhThanhPho = tinh.ten
    } else {
      tinh = { code: 'cccd_tinh', ten: parsed.tinh }
      tinhThanhs.value.unshift(tinh)
      form.value.tinhThanhPho = parsed.tinh
    }

    // 2. Nạp danh mục con (Huyện/Xã)
    const listCon = await layPhuongXa(tinhThanhs.value, tinh)
    phuongXas.value = listCon || []

    // 3. Phường/Xã
    if (parsed.phuong) {
      const pxMatch = phuongXas.value.find((p) => {
        const rawP = p.ten.toLowerCase()
        const targetP = parsed.phuong.toLowerCase()
        return rawP.includes(targetP) || targetP.includes(rawP)
      })
      if (pxMatch) {
        form.value.phuong = pxMatch.ten
      } else {
        phuongXas.value.unshift({ code: 'cccd_px', ten: parsed.phuong })
        form.value.phuong = parsed.phuong
      }
    }
  }

  // 4. Địa chỉ cụ thể
  if (parsed.diaChiCuThe) {
    form.value.diaChiCuThe = parsed.diaChiCuThe
  }
}

const xuLyChuoiCccd = async (raw) => {
  if (!raw || !raw.trim()) return false
  const parts = raw.trim().split('|')
  if (parts.length >= 5) {
    const hoTen = parts[2]?.trim()
    const dobRaw = parts[3]?.trim()
    const gioiTinhRaw = parts[4]?.trim()
    const diaChiRaw = parts[5]?.trim()

    // Chỉ điền các mục có trong CCCD: Họ tên, Ngày sinh, Giới tính, Địa chỉ
    if (hoTen) {
      form.value.tenKhachHang = hoTen
      if (!isEdit && maTuDong.value) {
        chonMaTuDong()
      }
    }
    if (dobRaw && dobRaw.length === 8) {
      const d = dobRaw.substring(0, 2)
      const m = dobRaw.substring(2, 4)
      const y = dobRaw.substring(4, 8)
      form.value.ngaySinh = `${y}-${m}-${d}`
    }
    if (gioiTinhRaw) {
      form.value.gioiTinh = gioiTinhRaw.toLowerCase().includes('nữ') ? 'NU' : 'NAM'
    }
    if (diaChiRaw) {
      await apDungDiaChiCccd(diaChiRaw)
    }
    playBeep()
    dongModalQuet()
    const parsed = phanTichDiaChiCccd(diaChiRaw)
    if (parsed.daDoiTinh) {
      notifySuccess(`Đã trích xuất CCCD thành công! Tỉnh ${parsed.tinhGoc} được chuẩn hóa thành "${parsed.tinh}" (theo danh mục 34 Tỉnh/TP mới nhất).`)
    } else {
      notifySuccess('Đã trích xuất thông tin khách hàng từ CCCD thành công!')
    }
    return true
  } else {
    notifyWarning('Mã QR không đúng định dạng CCCD chuẩn (cần phân tách bằng dấu |)')
    return false
  }
}

const startCamera = async () => {
  cameraError.value = ''
  try {
    const devices = await Html5Qrcode.getCameras()
    if (!devices || devices.length === 0) {
      cameraError.value = 'Không tìm thấy thiết bị Camera trên máy này'
      scanTab.value = 'usb'
      return
    }
    if (html5QrCode) {
      await stopCamera()
    }
    html5QrCode = new Html5Qrcode('customer-cccd-reader')
    isCameraRunning.value = true
    await html5QrCode.start(
      { facingMode: 'environment' },
      { fps: 15, qrbox: { width: 260, height: 260 }, aspectRatio: 1.0 },
      (decodedText) => {
        xuLyChuoiCccd(decodedText)
      },
      () => {}
    )
  } catch (err) {
    isCameraRunning.value = false
    cameraError.value = 'Không thể mở Camera. Vui lòng cấp quyền truy cập hoặc dùng súng quét USB.'
    scanTab.value = 'usb'
  }
}

const stopCamera = async () => {
  if (html5QrCode && isCameraRunning.value) {
    try {
      await html5QrCode.stop()
      html5QrCode.clear()
    } catch {}
    isCameraRunning.value = false
  }
}

const moModalQuetCccd = () => {
  rawCccdInput.value = ''
  isScanModalOpen.value = true
  scanTab.value = 'camera'
  nextTick(() => {
    startCamera()
  })
}

const dongModalQuet = async () => {
  await stopCamera()
  isScanModalOpen.value = false
}

const xuLyQuetCccdUsb = () => {
  xuLyChuoiCccd(rawCccdInput.value)
}

const onScanFileChange = async (e) => {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  try {
    if (!html5QrCode) {
      html5QrCode = new Html5Qrcode('customer-cccd-reader')
    }
    const decodedText = await html5QrCode.scanFile(file, true)
    xuLyChuoiCccd(decodedText)
  } catch {
    notifyWarning('Không tìm thấy mã QR hợp lệ trong hình ảnh')
  } finally {
    e.target.value = ''
  }
}

onUnmounted(() => {
  stopCamera()
})

const submit = async () => {
  daNopForm.value = true
  if ((form.value.email || '').trim()) {
    await kiemTraEmailTrung()
  }
  const error = validate()
  if (error) {
    errorMsg.value = ''
    return
  }
  errorMsg.value = ''

  const actionTitle = isEdit ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới'
  const actionMessage = isEdit
    ? `Bạn có chắc chắn muốn cập nhật thông tin khách hàng "${form.value.tenKhachHang}"?`
    : `Bạn có chắc chắn muốn thêm mới khách hàng "${form.value.tenKhachHang}"?`
  const ok = await notifyConfirm(actionTitle, actionMessage, isEdit ? 'Cập nhật' : 'Thêm mới', 'Hủy bỏ')
  if (!ok) return

  // Họ tên đổi sau lần gọi trước -> cập nhật lại mã đề xuất trước khi lưu.
  if (!isEdit && maTuDong.value && tenDaGoiMa.value !== (form.value.tenKhachHang || '').trim()) {
    await chonMaTuDong(true)
  }
  saving.value = true
  try {
    const payload = buildPayload()
    let msg = ''
    if (isEdit) {
      await updateCustomer(route.params.id, payload)
      msg = `Đã cập nhật thông tin khách hàng "${form.value.tenKhachHang}" thành công!`
      setFlash(msg)
    } else {
      const taoMoi = await createCustomer(payload)
      msg = `Đã thêm khách hàng "${taoMoi.tenKhachHang}" (${taoMoi.maKhachHang}) thành công!`
      setFlash(msg)
    }
    notifySuccess(msg)
    setTimeout(() => {
      router.push('/khach-hang')
    }, 500)
  } catch (e) {
    errorMsg.value = e.message
    notifyError(isEdit ? 'Cập nhật thất bại' : 'Thêm mới thất bại', e.message)
  } finally {
    saving.value = false
  }
}

const napDiaGioi = async (tenTinh, tenPhuong) => {
  const myToken = ++yeuCauHienTai
  diaGioiLoading.value = true
  diaGioiError.value = ''
  try {
    const ds = await layDiaGioi()
    if (myToken !== yeuCauHienTai) return
    tinhThanhs.value = ds
    const tinh = timTinh(ds, tenTinh)
    phuongXas.value = await layPhuongXa(ds, tinh)
    if (tenPhuong && !phuongXas.value.some((p) => p.ten === tenPhuong)) {
      phuongXas.value = [{ code: 'khac', ten: tenPhuong }, ...phuongXas.value]
    }
  } catch (e) {
    if (myToken === yeuCauHienTai) diaGioiError.value = e.message || 'Không tải được danh sách địa lý'
  } finally {
    if (myToken === yeuCauHienTai) diaGioiLoading.value = false
  }
}

const chonTinh = async (tenTinh) => {
  form.value.tinhThanhPho = tenTinh || ''
  form.value.phuong = ''
  const myToken = ++yeuCauHienTai
  const ds = tinhThanhs.value.length ? tinhThanhs.value : await layDiaGioi().catch(() => null)
  if (myToken !== yeuCauHienTai || !ds) return
  diaGioiLoading.value = true
  try {
    phuongXas.value = await layPhuongXa(ds, timTinh(ds, tenTinh))
  } finally {
    diaGioiLoading.value = false
  }
}

onMounted(async () => {
  if (!isEdit) {
    napDiaGioi('', '')
    return
  }
  loading.value = true
  try {
    const kh = await getCustomer(route.params.id)
    const diaChi = kh.diaChiMacDinh || {}
    form.value = {
      maKhachHang: kh.maKhachHang || '',
      tenKhachHang: kh.tenKhachHang || '',
      tenTaiKhoan: kh.tenTaiKhoan || '',
      matKhau: '',
      email: kh.email || '',
      soDienThoai: kh.soDienThoai || '',
      ngaySinh: kh.ngaySinh || '',
      gioiTinh: kh.gioiTinh || '',
      trangThai: kh.trangThai === false ? 'false' : 'true',
      tinhThanhPho: diaChi.thanhPho || '',
      phuong: diaChi.phuong || '',
      diaChiCuThe: diaChi.diaChiCuThe || '',
    }
    await napDiaGioi(form.value.tinhThanhPho, form.value.phuong)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="create-wrap">
    <h2 class="create-title">{{ isEdit ? 'Cập nhật Tài Khoản Khách Hàng' : 'Tạo Tài Khoản Khách Hàng' }}</h2>

    <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>
    <p v-if="loading" class="form-error">Đang tải dữ liệu...</p>

    <div class="create-grid">
      <div class="form-col">
        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </span>
            <h3 class="panel-title">Thông tin cơ bản</h3>
          </div>

          <div class="form-fields">
            <div v-if="isEdit" class="field">
              <label class="field-label">Mã khách hàng</label>
              <input
                v-model="form.maKhachHang"
                type="text"
                maxlength="25"
                placeholder="Mã khách hàng"
                @input="maTuDong = false"
                @blur="cham('maKhachHang')"
              />
              <p v-if="hienLoi('maKhachHang', loiMaKhachHang)" class="field-hint is-error">{{ loiMaKhachHang }}</p>
            </div>

            <div class="field">
              <label class="field-label">Họ và tên <b class="req">*</b></label>
              <input v-model="form.tenKhachHang" type="text" maxlength="200" placeholder="Nguyễn Thị Bình"
                @blur="cham('tenKhachHang'); chonMaTuDong()" />
              <p v-if="hienLoi('tenKhachHang', loiTen)" class="field-hint is-error">{{ loiTen }}</p>
            </div>

            <div class="field">
              <label class="field-label">Email <b class="req">*</b></label>
              <input
                v-model="form.email"
                type="email"
                maxlength="150"
                placeholder="khachhang@gmail.com"
                @blur="cham('email'); kiemTraEmailTrung"
              />
              <p v-if="hienLoi('email', loiEmail)" class="field-hint is-error">{{ loiEmail }}</p>
              <p v-else-if="emailTrungLoi" class="field-hint is-error">{{ emailTrungLoi }}</p>
            </div>

            <div class="field">
              <label class="field-label">Số điện thoại</label>
              <input v-model="form.soDienThoai" type="tel" maxlength="10" placeholder="0901234567"
                @blur="cham('soDienThoai')" />
              <p v-if="hienLoi('soDienThoai', loiSoDienThoai)" class="field-hint is-error">{{ loiSoDienThoai }}</p>
            </div>

            <div class="field">
              <label class="field-label">Ngày sinh</label>
              <input
                v-model="form.ngaySinh"
                type="date"
                @blur="ngaySinhDaCham = true"
              />
              <p v-if="hienThiLoiNgaySinh" class="field-hint is-error">{{ hienThiLoiNgaySinh }}</p>
            </div>

            <div class="field">
              <label class="field-label">Giới tính <b class="req">*</b></label>
              <div class="radio-row" role="radiogroup" aria-label="Giới tính" @change="cham('gioiTinh')">
                <label v-for="item in gioiTinhOptions" :key="item.value" class="radio-item">
                  <input v-model="form.gioiTinh" type="radio" name="gioiTinh" :value="item.value" />
                  <span class="radio-dot"></span>
                  <span>{{ item.label }}</span>
                </label>
              </div>
              <p v-if="hienLoi('gioiTinh', loiGioiTinh)" class="field-hint is-error">{{ loiGioiTinh }}</p>
            </div>

            <div v-if="isEdit" class="field">
              <label class="field-label">Trạng thái</label>
              <SearchableSelect
                v-model="form.trangThai"
                :options="trangThais"
                label-key="label"
                value-key="value"
                placeholder="Chọn trạng thái"
                search-placeholder="Tìm..."
                :allow-clear="false"
              />
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon is-red">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z" />
                <circle cx="12" cy="10" r="2.6" />
              </svg>
            </span>
            <h3 class="panel-title">Địa chỉ</h3>
          </div>
          <!-- Thông tin trích xuất rõ ràng từ CCCD -->
          <div v-if="diaChiCccdGoc" class="cccd-address-display" style="margin: 0 0 16px 0;">
            <div class="cccd-address-header">
              <i class="bi bi-qr-code-scan"></i>
              <span>Địa chỉ trích xuất từ CCCD:</span>
            </div>
            <div class="cccd-address-val">
              {{ diaChiCccdGoc }}
            </div>
          </div>

          <div class="form-fields">
            <div class="field">
              <label class="field-label">Tỉnh/Thành phố</label>
              <SearchableSelect
                v-model="form.tinhThanhPho"
                :options="tinhThanhs.map((t) => ({ value: t.ten, label: t.ten }))"
                :placeholder="diaGioiLoading ? 'Đang tải danh sách tỉnh/thành...' : 'Chọn tỉnh/thành phố'"
                :disabled="diaGioiLoading"
                @update:model-value="(v) => { cham('tinhThanhPho'); chonTinh(v) }"
              />
              <p v-if="diaGioiError" class="field-hint is-error">
                {{ diaGioiError }}
                <button type="button" class="link-retry" @click="napDiaGioi(form.tinhThanhPho, form.phuong)">Thử lại</button>
              </p>
              <p v-if="hienLoi('tinhThanhPho', loiTinhThanhPho)" class="field-hint is-error">{{ loiTinhThanhPho }}</p>
            </div>

            <div class="field">
              <label class="field-label">Phường/Xã</label>
              <SearchableSelect
                v-model="form.phuong"
                :options="phuongXas.map((p) => ({ value: p.ten, label: p.ten }))"
                :placeholder="diaGioiLoading ? 'Đang tải...' : form.tinhThanhPho ? 'Chọn phường/xã' : 'Chọn tỉnh trước'"
                :disabled="diaGioiLoading || !form.tinhThanhPho"
                @update:model-value="cham('phuong')"
              />
              <p v-if="hienLoi('phuong', loiPhuong)" class="field-hint is-error">{{ loiPhuong }}</p>
            </div>

            <div class="field field-wide">
              <label class="field-label">Địa chỉ cụ thể</label>
              <textarea v-model="form.diaChiCuThe" maxlength="255" placeholder="Số nhà, đường..." rows="3"
                @blur="cham('diaChiCuThe')"></textarea>
              <p v-if="hienLoi('diaChiCuThe', loiDiaChiCuThe)" class="field-hint is-error">{{ loiDiaChiCuThe }}</p>
            </div>
          </div>
        </section>

        <div class="form-actions">
          <button class="btn-primary" type="button" :disabled="saving" @click="submit">
            {{ saving ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Thêm' }}
          </button>
          <button class="btn-cancel" type="button" @click="router.push('/khach-hang')">Hủy</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-wrap {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 6px 4px 40px;
}

.create-title {
  font-size: 17px;
  font-weight: 700;
  color: #1d1d23;
}

.create-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.form-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.form-col .panel {
  padding: 18px 22px 22px;
}

.form-col .panel-head {
  padding-bottom: 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f3;
}

.role-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #f7f7f9;
  border: 1px solid #ececef;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 12.5px;
  color: #4a4a52;
}

.form-error {
  background: #fdecec;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 7px;
  padding: 9px 12px;
}

.form-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.field-wide {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #3d3d45;
}

.field-label .req {
  color: var(--red);
}

.field-hint {
  font-size: 12px;
  color: #6b7280;
}

.field-hint.is-error {
  color: #dc2626;
}

.link-retry {
  border: none;
  background: none;
  color: var(--red);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
  margin-left: 6px;
}

.form-fields input:not([type='radio']),
.form-fields textarea {
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

.form-fields textarea {
  height: auto;
  min-height: 78px;
  padding: 10px 13px;
  resize: vertical;
  line-height: 1.5;
}

.form-fields input:not([type='radio'])::placeholder,
.form-fields textarea::placeholder {
  color: #9a9aa3;
}

.form-fields input:not([type='radio']):focus,
.form-fields textarea:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.form-actions {
  display: flex;
  justify-content: flex-start;
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

.btn-primary:hover {
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

@media (max-width: 1024px) {
  .create-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
  }

  .form-col .panel {
    padding: 16px 16px 18px;
  }

  .form-fields {
    gap: 12px 14px;
  }
}

@media (max-width: 760px) {
  .create-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-fields {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-actions {
    flex-wrap: wrap;
  }
}

/* Nút Quét CCCD */
.btn-scan-cccd-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  padding: 6px 14px;
  border-radius: 9999px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-scan-cccd-pill:hover {
  background: #eff6ff;
  border-color: #60a5fa;
}

/* Modal Quét CCCD */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  backdrop-filter: blur(4px);
  padding: 16px;
}

.scan-modal-card {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.16);
  overflow: hidden;
  animation: modalScale 0.2s ease-out;
}

@keyframes modalScale {
  from { transform: scale(0.96); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.scan-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.scan-header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 24px;
  color: #64748b;
  cursor: pointer;
  line-height: 1;
}

.scan-tab-bar {
  display: flex;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
  padding: 0 20px;
  gap: 8px;
}

.scan-tab-btn {
  padding: 10px 16px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.scan-tab-btn:hover { color: #1e293b; }
.scan-tab-btn.active { color: #2563eb; border-bottom-color: #2563eb; background: #ffffff; }

.scan-modal-body { padding: 20px; }

.camera-scan-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.camera-viewport-box {
  width: 100%;
  max-width: 380px;
  height: 280px;
  background: #0f172a;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.camera-stream-el {
  width: 100% !important;
  height: 100% !important;
}

.camera-stream-el video {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  border-radius: 14px;
}

.scanner-laser-line {
  position: absolute;
  top: 15%;
  left: 10%;
  right: 10%;
  height: 2px;
  background: #3b82f6;
  box-shadow: 0 0 12px 2px #3b82f6;
  animation: laserScan 2s infinite ease-in-out alternate;
  z-index: 10;
}

@keyframes laserScan {
  0% { top: 15%; }
  100% { top: 85%; }
}

.scanner-frame-overlay {
  position: absolute;
  width: 200px;
  height: 200px;
  pointer-events: none;
  z-index: 5;
}

.scanner-frame-overlay .corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: #ffffff;
  border-style: solid;
}

.corner.top-left { top: 0; left: 0; border-width: 3px 0 0 3px; border-top-left-radius: 6px; }
.corner.top-right { top: 0; right: 0; border-width: 3px 3px 0 0; border-top-right-radius: 6px; }
.corner.bottom-left { bottom: 0; left: 0; border-width: 0 0 3px 3px; border-bottom-left-radius: 6px; }
.corner.bottom-right { bottom: 0; right: 0; border-width: 0 3px 3px 0; border-bottom-right-radius: 6px; }

.scan-instruction-text {
  margin: 0;
  font-size: 12.5px;
  color: #475569;
  text-align: center;
}

.camera-alert-error {
  background: #fef2f2;
  border: 1px solid #fee2e2;
  color: #ef4444;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 12.5px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-scan-file-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scan-textarea {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  font-size: 13px;
  color: #1e293b;
  font-family: inherit;
  box-sizing: border-box;
  outline: none;
  resize: vertical;
}

.scan-textarea:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.btn-cancel-action {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  border-radius: 9999px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-submit-action {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
  border-radius: 9999px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.cccd-address-display {
  background: linear-gradient(135deg, #f0fdf4 0%, #e0f2fe 100%);
  border: 1px solid #bbf7d0;
  border-left: 4px solid #10b981;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cccd-address-header {
  font-size: 12px;
  font-weight: 600;
  color: #047857;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cccd-address-val {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
}
</style>
