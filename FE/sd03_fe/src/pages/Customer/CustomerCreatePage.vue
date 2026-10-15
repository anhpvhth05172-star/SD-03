<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, onUnmounted, ref } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import { useRoute, useRouter } from 'vue-router'
import { checkCustomerEmail, createCustomer, getCustomer, suggestCustomerCode, updateCustomer } from '@/api/customer'
import { DS_34_TINH_THANH, layDiaGioi, layPhuongXa, phanTichChuoiCccd, phanTichDiaChiCccd, timTinh } from '@/api/diaGioi'
import AvatarCard from '@/components/AvatarCard.vue'
import { setFlash } from '@/utils/flash'
import { hopLeMa, maDeXuat } from '@/utils/maNhanVien'
import { datVaiTro, layNguoiDung, vaiTroHienTai } from '@/utils/session'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import {
  batBuoc,
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
  soCccd: '',
  ngaySinh: '',
  gioiTinh: '',
  trangThai: 'true',
  tinhThanhPho: '',
  phuong: '',
  diaChiCuThe: '',
  anhDaiDien: '',
})

const onAvatarChange = (val) => {
  form.value.anhDaiDien = val?.dataUrl || ''
}

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

const loiSoCccd = computed(() =>
  kiemTra([
    toiDa(form.value.soCccd, 20, 'Số CCCD'),
    !isBlank(form.value.soCccd) && !/^\d{9,12}$/.test(form.value.soCccd.trim())
      ? 'Số CCCD không hợp lệ (gồm 9-12 chữ số, không chứa chữ cái)'
      : '',
  ]),
)

const chonVaiTro = (e) => {
  const id = e.target.value
  const v = id
  if (v) datVaiTro(v)
}

const validate = () =>
  kiemTra([
    batBuoc(form.value.tenKhachHang, 'Họ và tên'),
    toiDa(form.value.tenKhachHang, 150, 'Họ và tên'),
    toiDa(form.value.maKhachHang, 25, 'Mã khách hàng'),
    form.value.maKhachHang.trim() && !hopLeMa(form.value.maKhachHang)
      ? 'Mã khách hàng không hợp lệ (bắt đầu bằng chữ cái, chỉ chữ cái và số, tối đa 25 ký tự)'
      : '',
    batBuoc(form.value.email, 'Email'),
    toiDa(form.value.email, 150, 'Email'),
    emailHopLe(form.value.email),
    emailTrungLoi.value,
    toiDa(form.value.soDienThoai, 10, 'Số điện thoại'),
    soDienThoaiHopLe(form.value.soDienThoai),
    loiSoCccd.value,
    khongDuocTuongLai(form.value.ngaySinh, 'Ngày sinh'),
    duTuoiToiThieu(form.value.ngaySinh, TUOI_TOI_THIEU, THONG_BAO_TUOI),
    loiGioiTinh.value,
    toiDa(form.value.tinhThanhPho, 100, 'Tỉnh/Thành phố'),
    toiDa(form.value.phuong, 100, 'Phường/Xã'),
    toiDa(form.value.diaChiCuThe, 255, 'Địa chỉ cụ thể'),
  ])

const buildPayload = () => ({
  maKhachHang: form.value.maKhachHang.trim() || null,
  tenKhachHang: form.value.tenKhachHang.trim(),
  matKhau: form.value.matKhau ? form.value.matKhau : null,
  email: form.value.email.trim(),
  soDienThoai: form.value.soDienThoai.trim() || null,
  soCccd: form.value.soCccd.trim(),
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
const isCameraRunning = ref(false)
const cameraError = ref('')
const isProcessingScan = ref(false)
const diaChiCccdGoc = ref('')
const scanSuccessAnim = ref(false)
const scannedName = ref('')
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

const apDungDiaChiCccd = (diaChiRaw) => {
  if (!diaChiRaw) return
  diaChiCccdGoc.value = diaChiRaw
  const parsed = phanTichDiaChiCccd(diaChiRaw)

  // 1. Áp dụng ngay địa chỉ cụ thể
  form.value.diaChiCuThe = parsed.diaChiCuThe || diaChiRaw

  // 2. Áp dụng ngay Tỉnh/Thành phố gốc từ CCCD vào dropdown và form
  if (parsed.tinh) {
    const tenTinh = parsed.tinh
    let tinh = tinhThanhs.value.find((t) => t.ten.toLowerCase() === tenTinh.toLowerCase())
    if (!tinh) {
      tinh = timTinh(tinhThanhs.value, tenTinh)
    }
    if (!tinh) {
      tinh = { code: 'cccd_tinh_' + Date.now(), ten: tenTinh }
      tinhThanhs.value.unshift(tinh)
    }
    // Gán trực tiếp tên tỉnh khớp với option trong combobox
    form.value.tinhThanhPho = tinh.ten

    // 3. Đưa ngay Phường/Xã gốc vào danh sách để dropdown hiển thị lập tức (0ms)
    if (parsed.phuong) {
      const tenPx = parsed.phuong
      if (!phuongXas.value.some((p) => p.ten.toLowerCase() === tenPx.toLowerCase())) {
        phuongXas.value = [{ code: 'cccd_px_' + Date.now(), ten: tenPx }, ...phuongXas.value]
      }
      form.value.phuong = tenPx
    }

    // Đảm bảo combobox không bị khóa loading
    diaGioiLoading.value = false

    // 4. Tải ngầm danh sách phường/xã của tỉnh trong nền để người dùng có thể đổi nếu muốn
    if (tinh) {
      layPhuongXa(tinhThanhs.value, tinh).then((listCon) => {
        if (listCon && listCon.length) {
          const curPx = form.value.phuong
          const merged = [...listCon]
          if (curPx && !merged.some((p) => p.ten.toLowerCase() === curPx.toLowerCase())) {
            merged.unshift({ code: 'cccd_px_' + Date.now(), ten: curPx })
          }
          phuongXas.value = merged
        }
      }).catch((e) => console.warn('Tải ngầm phường/xã:', e))
    }
  }
}

const xuLyChuoiCccd = async (raw) => {
  if (!raw || !raw.trim()) {
    isProcessingScan.value = false
    return false
  }

  const info = phanTichChuoiCccd(raw)
  if (!info || !info.hoTen) {
    notifyWarning('Mã QR không đúng định dạng CCCD chuẩn (cần phân tách bằng dấu |)')
    isProcessingScan.value = false
    return false
  }

  try {
    // 1. Phát âm thanh beep & hiển thị hiệu ứng thành công trực tiếp trên camera
    playBeep()
    scanSuccessAnim.value = true
    scannedName.value = info.hoTen

    // 2. Điền thông tin vào form khách hàng ngay lập tức (Họ tên, ngày sinh, giới tính, địa chỉ)
    if (info.hoTen) {
      form.value.tenKhachHang = info.hoTen
      if (!isEdit && maTuDong.value) {
        chonMaTuDong()
      }
    }
    if (info.soCccd) {
      form.value.soCccd = info.soCccd
    }
    if (info.ngaySinh) {
      form.value.ngaySinh = info.ngaySinh
    }
    if (info.gioiTinh) {
      form.value.gioiTinh = info.gioiTinh
    }
    if (info.diaChiRaw) {
      apDungDiaChiCccd(info.diaChiRaw)
    }

    // 3. Giữ visual "Quét thành công" trên camera 0.6s trước khi đóng
    await new Promise((resolve) => setTimeout(resolve, 600))
  } catch (err) {
    console.warn('Lỗi xử lý quét CCCD:', err)
  } finally {
    // Luôn luôn tắt camera triệt để và đóng modal
    await stopCamera()
    isScanModalOpen.value = false
    scanSuccessAnim.value = false
  }

  // 4. Hiển thị thông báo SweetAlert2
  notifySuccess(`Quét CCCD thành công!`, `Đã nhận diện khách hàng: ${info.hoTen}`, 2000)

  isProcessingScan.value = false
  return true
}


const startCamera = async () => {
  cameraError.value = ''
  scanSuccessAnim.value = false
  try {
    const devices = await Html5Qrcode.getCameras()
    if (!devices || devices.length === 0) {
      cameraError.value = 'Không tìm thấy thiết bị Camera trên máy này.'
      return
    }
    if (html5QrCode) {
      await stopCamera()
    }
    html5QrCode = new Html5Qrcode('customer-cccd-reader')
    isCameraRunning.value = true

    // Ưu tiên camera sau nếu có, hoặc dùng camera đầu tiên
    const cameraId = devices.length > 1
      ? (devices.find(d => d.label.toLowerCase().includes('back') || d.label.toLowerCase().includes('rear'))?.id || devices[0].id)
      : devices[0].id

    await html5QrCode.start(
      cameraId,
      {
        fps: 20,
        qrbox: (viewfinderWidth, viewfinderHeight) => {
          const minEdge = Math.min(viewfinderWidth, viewfinderHeight)
          const edge = Math.max(180, Math.floor(minEdge * 0.8))
          return { width: edge, height: edge }
        },
      },
      (decodedText) => {
        if (isProcessingScan.value) return
        isProcessingScan.value = true
        xuLyChuoiCccd(decodedText)
      },
      () => {}
    )
  } catch (err) {
    console.warn('Camera error:', err)
    isCameraRunning.value = false
    cameraError.value = 'Không thể mở Camera (chưa cấp quyền truy cập hoặc camera đang bận).'
  }
}

const stopCamera = async () => {
  // 1. Dừng trực tiếp tất cả MediaStream tracks trên mọi thẻ video để tắt phần cứng camera ngay
  try {
    const videoEls = document.querySelectorAll('#customer-cccd-reader video, video')
    videoEls.forEach((v) => {
      if (v.srcObject && typeof v.srcObject.getTracks === 'function') {
        v.srcObject.getTracks().forEach((track) => {
          try {
            track.stop()
            track.enabled = false
          } catch {}
        })
        v.srcObject = null
      }
      try {
        v.pause()
        v.removeAttribute('src')
        v.load()
      } catch {}
    })
  } catch (err) {
    console.warn('Lỗi giải phóng media tracks:', err)
  }

  // 2. Dừng scanner của html5QrCode an toàn với Promise.race (không bao giờ bị treo promise)
  if (html5QrCode) {
    try {
      if (html5QrCode.isScanning) {
        await Promise.race([
          html5QrCode.stop().catch(() => {}),
          new Promise((resolve) => setTimeout(resolve, 400)),
        ])
      }
    } catch (e) {
      console.warn('html5QrCode.stop error:', e)
    }

    try {
      html5QrCode.clear()
    } catch {}
    html5QrCode = null
  }

  isCameraRunning.value = false
}

const moModalQuetCccd = () => {
  isProcessingScan.value = false
  scanSuccessAnim.value = false
  scannedName.value = ''
  isScanModalOpen.value = true
  nextTick(() => {
    startCamera()
  })
}

const dongModalQuet = async () => {
  await stopCamera()
  isProcessingScan.value = false
  scanSuccessAnim.value = false
  isScanModalOpen.value = false
}

const onScanFileChange = async (e) => {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  try {
    if (!html5QrCode) {
      html5QrCode = new Html5Qrcode('customer-cccd-reader')
    }
    isProcessingScan.value = true
    const decodedText = await html5QrCode.scanFile(file, true)
    await xuLyChuoiCccd(decodedText)
  } catch {
    isProcessingScan.value = false
    notifyWarning('Không tìm thấy mã QR hợp lệ trong hình ảnh')
  } finally {
    e.target.value = ''
  }
}

onUnmounted(async () => {
  await stopCamera()
})

onBeforeUnmount(async () => {
  await stopCamera()
})

const submit = async () => {
  daNopForm.value = true
  if ((form.value.email || '').trim()) {
    await kiemTraEmailTrung()
  }
  const error = validate()
  if (error) {
    errorMsg.value = error
    notifyWarning('Vui lòng kiểm tra lại thông tin', error)
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
  let emailThatBai = false
  try {
    const payload = buildPayload()
    let msg = ''
    if (isEdit) {
      await updateCustomer(route.params.id, payload)
      msg = `Đã cập nhật thông tin khách hàng "${form.value.tenKhachHang}" thành công!`
      setFlash(msg)
    } else {
      const taoMoi = await createCustomer(payload)
      const emailNhan = taoMoi.email || form.value.email || '(chưa có địa chỉ email)'
      msg = `Đã thêm khách hàng "${taoMoi.tenKhachHang}" (${taoMoi.maKhachHang}) thành công!`
      if (taoMoi.emailGuiThanhCong === true) {
        msg += ` Email thông tin tài khoản đã được nhà cung cấp chấp nhận gửi tới ${emailNhan} — nếu chưa thấy, hãy kiểm tra thư mục Spam.`
      } else if (taoMoi.emailGuiThanhCong === false) {
        emailThatBai = true
        msg += ` Hệ thống KHÔNG gửi được email thông tin tài khoản tới ${emailNhan}. Vui lòng kiểm tra lại địa chỉ email hoặc cấu hình gửi mail.`
      } else {
        msg += ` Hệ thống chưa xác nhận trạng thái gửi email thông tin tài khoản.`
      }
      setFlash(msg, emailThatBai ? 'warning' : 'success')
    }
    if (emailThatBai) {
      notifyWarning('Thêm mới thành công, gửi email thất bại', msg)
    } else {
      notifySuccess(msg)
    }
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
      soCccd: kh.soCccd || '',
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

    <div class="role-bar">
      <span>Đang thao tác với vai trò: <b>{{ vaiTroHienTai === 'ADMIN' ? 'Admin' : 'Nhân viên' }}</b></span>
      <select :value="vaiTroHienTai" @change="chonVaiTro($event)">
        <option value="ADMIN">Admin</option>
        <option value="STAFF">Nhân viên</option>
      </select>
    </div>

    <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>
    <p v-if="loading" class="form-error">Đang tải dữ liệu...</p>

    <div class="create-grid">
      <AvatarCard
        :ten="form.tenKhachHang"
        :phu="form.email || form.maKhachHang"
        :anh="form.anhDaiDien"
        @change="onAvatarChange"
      />

      <div class="form-col">
        <section class="panel">
          <div class="panel-head" style="justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span class="panel-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </span>
              <h3 class="panel-title">Thông tin cơ bản</h3>
            </div>
            <button class="btn-scan-cccd-pill" type="button" @click="moModalQuetCccd">
              <i class="bi bi-qr-code-scan"></i>
              <span>Quét mã CCCD</span>
            </button>
          </div>

          <div class="form-fields">
            <div class="field">
              <label class="field-label">Mã khách hàng</label>
              <input
                v-model="form.maKhachHang"
                type="text"
                maxlength="25"
                placeholder="Để trống sẽ tự sinh theo họ tên"
                @input="maTuDong = false"
              />
              <p v-if="!isEdit && maPreview" class="field-hint">Mã gợi ý theo họ tên: <b>{{ maPreview }}</b></p>
            </div>

            <div class="field">
              <label class="field-label">Họ và tên <b class="req">*</b></label>
              <input v-model="form.tenKhachHang" type="text" maxlength="200" placeholder="Nguyễn Thị Bình" @blur="chonMaTuDong()" />
            </div>

            <div class="field">
              <label class="field-label">Email <b class="req">*</b></label>
              <input
                v-model="form.email"
                type="email"
                maxlength="150"
                placeholder="khachhang@gmail.com"
                @blur="kiemTraEmailTrung"
              />
              <p v-if="emailTrungLoi" class="field-hint is-error">{{ emailTrungLoi }}</p>
            </div>

            <div class="field">
              <label class="field-label">Số điện thoại</label>
              <input v-model="form.soDienThoai" type="tel" maxlength="10" placeholder="0901234567" />
            </div>

            <div class="field">
              <label class="field-label">Số CCCD/CMND</label>
              <input
                v-model="form.soCccd"
                type="text"
                inputmode="numeric"
                maxlength="20"
                placeholder="012345678901"
              />
              <p v-if="daNopForm && loiSoCccd" class="field-hint is-error">{{ loiSoCccd }}</p>
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
              <div class="radio-row" role="radiogroup" aria-label="Giới tính">
                <label v-for="item in gioiTinhOptions" :key="item.value" class="radio-item">
                  <input v-model="form.gioiTinh" type="radio" name="gioiTinh" :value="item.value" />
                  <span class="radio-dot"></span>
                  <span>{{ item.label }}</span>
                </label>
              </div>
              <p v-if="daNopForm && loiGioiTinh" class="field-hint is-error">{{ loiGioiTinh }}</p>
            </div>

            <div v-if="isEdit" class="field">
              <label class="field-label">Trạng thái</label>
              <select v-model="form.trangThai">
                <option v-for="item in trangThais" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
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
              <select v-model="form.tinhThanhPho" :disabled="diaGioiLoading" @change="chonTinh($event.target.value)">
                <option value="">
                  {{ diaGioiLoading ? 'Đang tải danh sách tỉnh/thành...' : 'Chọn tỉnh/thành phố' }}
                </option>
                <option v-for="t in tinhThanhs" :key="t.code" :value="t.ten">{{ t.ten }}</option>
              </select>
              <p v-if="diaGioiError" class="field-hint is-error">
                {{ diaGioiError }}
                <button type="button" class="link-retry" @click="napDiaGioi(form.tinhThanhPho, form.phuong)">Thử lại</button>
              </p>
            </div>

            <div class="field">
              <label class="field-label">Phường/Xã</label>
              <select v-model="form.phuong" :disabled="diaGioiLoading || !form.tinhThanhPho">
                <option value="">
                  {{ diaGioiLoading ? 'Đang tải...' : form.tinhThanhPho ? 'Chọn phường/xã' : 'Chọn tỉnh trước' }}
                </option>
                <option v-for="p in phuongXas" :key="p.code" :value="p.ten">{{ p.ten }}</option>
              </select>
            </div>

            <div class="field field-wide">
              <label class="field-label">Địa chỉ cụ thể</label>
              <textarea v-model="form.diaChiCuThe" maxlength="255" placeholder="Số nhà, đường..." rows="3"></textarea>
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

    <!-- Modal Quét CCCD -->
    <div v-if="isScanModalOpen" class="modal-backdrop" @click.self="dongModalQuet">
      <div class="scan-modal-card">
        <div class="scan-modal-header">
          <div class="scan-header-left">
            <div class="scan-header-icon">
              <i class="bi bi-qr-code-scan"></i>
            </div>
            <div>
              <h3 class="scan-header-title">Quét thẻ Căn cước công dân (CCCD)</h3>
              <p class="scan-header-subtitle">Tự động nhận diện và trích xuất thông tin khách hàng</p>
            </div>
          </div>
          <button type="button" class="btn-close-modal" title="Đóng" @click="dongModalQuet">×</button>
        </div>

        <div class="scan-modal-body">
          <div class="camera-scan-wrap">
            <div class="camera-viewport-box">
              <div id="customer-cccd-reader" class="camera-stream-el"></div>

              <!-- Tia laser quét -->
              <div v-if="isCameraRunning && !scanSuccessAnim" class="scanner-laser-line"></div>

              <!-- 4 góc khung quét -->
              <div class="scanner-frame-overlay">
                <span class="corner top-left"></span>
                <span class="corner top-right"></span>
                <span class="corner bottom-left"></span>
                <span class="corner bottom-right"></span>
              </div>

              <!-- Lớp phủ báo Quét Thành Công trực tiếp trên camera -->
              <div v-if="scanSuccessAnim" class="scan-success-overlay">
                <div class="success-icon-circle">
                  <i class="bi bi-check-lg"></i>
                </div>
                <span class="success-text-bold">Quét thành công!</span>
                <span class="success-sub-text">{{ scannedName }}</span>
              </div>
            </div>

            <p class="scan-instruction-text">
              <i class="bi bi-info-circle-fill text-primary"></i>
              <span>Đưa <b>mã QR góc phải</b> trên thẻ CCCD vào khung hình để camera tự nhận diện.</span>
            </p>

            <div v-if="cameraError" class="camera-alert-error">
              <i class="bi bi-exclamation-triangle-fill"></i>
              <span>{{ cameraError }}</span>
            </div>

            <div class="camera-extra-actions">
              <label class="btn-scan-file-label">
                <i class="bi bi-image"></i> Tải ảnh chụp CCCD từ máy
                <input type="file" accept="image/*" style="display: none;" @change="onScanFileChange" />
              </label>
            </div>
          </div>
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
  grid-template-columns: 250px minmax(0, 1fr);
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

.role-bar select {
  height: 30px;
  border: 1px solid #e3e3e8;
  border-radius: 6px;
  background: #fff;
  font-size: 12.5px;
  padding: 0 8px;
  color: #4a4a52;
  outline: none;
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
.form-fields select,
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
.form-fields select:focus,
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
    grid-template-columns: 200px minmax(0, 1fr);
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
  padding: 18px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.scan-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.scan-header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.scan-header-subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #64748b;
}

.scan-header-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 24px;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  background: #f1f5f9;
  color: #334155;
}

.scan-modal-body {
  padding: 20px 24px 24px;
}

.camera-scan-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.camera-viewport-box {
  width: 100%;
  max-width: 400px;
  height: 290px;
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

/* Lớp phủ báo Quét Thành Công trực tiếp trên camera */
.scan-success-overlay {
  position: absolute;
  inset: 0;
  background: rgba(16, 185, 129, 0.94);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #ffffff;
  z-index: 25;
  animation: modalScale 0.2s ease-out;
}

.success-icon-circle {
  width: 60px;
  height: 60px;
  background: #ffffff;
  color: #10b981;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
}

.success-text-bold {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.2px;
}

.success-sub-text {
  font-size: 13.5px;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.15);
  padding: 4px 14px;
  border-radius: 9999px;
  max-width: 85%;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.scanner-laser-line {
  position: absolute;
  top: 15%;
  left: 10%;
  right: 10%;
  height: 2px;
  background: #ef4444;
  box-shadow: 0 0 12px 2px #ef4444;
  animation: laserScan 2s infinite ease-in-out alternate;
  z-index: 10;
}

@keyframes laserScan {
  0% {
    top: 15%;
  }

  100% {
    top: 85%;
  }
}

.scanner-frame-overlay {
  position: absolute;
  width: 220px;
  height: 220px;
  pointer-events: none;
  z-index: 5;
}

.scanner-frame-overlay .corner {
  position: absolute;
  width: 26px;
  height: 26px;
  border-color: #ffffff;
  border-style: solid;
}

.corner.top-left {
  top: 0;
  left: 0;
  border-width: 3.5px 0 0 3.5px;
  border-top-left-radius: 6px;
}

.corner.top-right {
  top: 0;
  right: 0;
  border-width: 3.5px 3.5px 0 0;
  border-top-right-radius: 6px;
}

.corner.bottom-left {
  bottom: 0;
  left: 0;
  border-width: 0 0 3.5px 3.5px;
  border-bottom-left-radius: 6px;
}

.corner.bottom-right {
  bottom: 0;
  right: 0;
  border-width: 0 3.5px 3.5px 0;
  border-bottom-right-radius: 6px;
}

.scan-instruction-text {
  margin: 0;
  font-size: 13px;
  color: #475569;
  text-align: center;
  line-height: 1.5;
  padding: 0 10px;
}

.scan-instruction-text i {
  margin-right: 5px;
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
  padding: 7px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-scan-file-label:hover {
  background: #dbeafe;
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
