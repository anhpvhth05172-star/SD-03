<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, onUnmounted, ref } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import { useRoute, useRouter } from 'vue-router'
import { DS_34_TINH_THANH, layDiaGioi, layPhuongXa, phanTichChuoiCccd, phanTichDiaChiCccd, timTinh } from '@/api/diaGioi'
import {
  checkEmployeeEmail,
  checkEmployeeSdt,
  createEmployee,
  getEmployee,
  getEmployeeFormData,
  suggestEmployeeCode,
  updateEmployee,
} from '@/api/employee'
import { setFlash } from '@/utils/flash'
import { layNguoiDung } from '@/utils/session'
import { notifyConfirm, notifyError, notifySuccess, notifyWarning } from '@/utils/notify'
import SearchableSelect from '@/components/SearchableSelect.vue'
import {
  batBuoc,
  chonBatBuoc,
  duTuoiToiThieu,
  emailHopLe,
  isBlank,
  kiemTra,
  khongDuocTuongLai,
  soDienThoaiHopLe,
  toiDa,
} from '@/utils/validate'

const TUOI_TOI_THIEU = 18
const THONG_BAO_TUOI = 'Nhân viên phải đủ 18 tuổi.'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id
const saving = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const anhDaChon = ref('')
const fileInputEl = ref(null)

const gioiTinhOptions = [
  { value: 'NAM', label: 'Nam' },
  { value: 'NU', label: 'Nữ' },
]

const vaiTros = ref([])
const daNopForm = ref(false)
const ngaySinhDaCham = ref(false)
const emailTrungLoi = ref('')
const sdtTrungLoi = ref('')
let yeuCauHienTai = 0

const tinhThanhs = ref([])
const phuongXas = ref([])
const diaGioiLoading = ref(false)
const diaChiCccdGoc = ref('')

const isScanModalOpen = ref(false)
const isProcessingScan = ref(false)
const scanSuccessAnim = ref(false)
const scannedName = ref('')
const maPreview = ref('')
let maTimeout = null

const capNhatMaGoiY = () => {
  if (isEdit) return
  const ten = form.value.tenTaiKhoan ? form.value.tenTaiKhoan.trim() : ''
  if (!ten) {
    maPreview.value = ''
    return
  }
  clearTimeout(maTimeout)
  maTimeout = setTimeout(async () => {
    try {
      const res = await suggestEmployeeCode(ten)
      maPreview.value = res?.ma || ''
    } catch {
      maPreview.value = ''
    }
  }, 250)
}

const form = ref({
  maNhanVien: '',
  tenTaiKhoan: '',
  matKhau: '',
  email: '',
  soDienThoai: '',
  ngaySinh: '',
  gioiTinh: 'NAM',
  idVaiTro: '',
  queQuan: '',
  phuong: '',
  diaChiCuThe: '',
  trangThai: 'true',
})


const moChonAnh = () => {
  if (fileInputEl.value) {
    fileInputEl.value.click()
  }
}

const nenAnh = (file, maxKichThuoc = 400, chatLuong = 0.85) => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let { width, height } = img
        if (width > maxKichThuoc || height > maxKichThuoc) {
          if (width > height) {
            height = Math.round((height * maxKichThuoc) / width)
            width = maxKichThuoc
          } else {
            width = Math.round((width * maxKichThuoc) / height)
            height = maxKichThuoc
          }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', chatLuong))
      }
      img.onerror = () => resolve(e.target.result)
      img.src = e.target.result
    }
    reader.onerror = () => resolve('')
    reader.readAsDataURL(file)
  })
}

const onFileChange = async (e) => {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    notifyWarning('Vui lòng chọn tệp định dạng hình ảnh')
    return
  }
  try {
    anhDaChon.value = await nenAnh(file)
  } catch {
    const reader = new FileReader()
    reader.onload = () => {
      anhDaChon.value = reader.result
    }
    reader.readAsDataURL(file)
  }
}


const kiemTraEmailTrung = async () => {
  const em = (form.value.email || '').trim()
  if (!em || !emailHopLe(em)) {
    emailTrungLoi.value = ''
    return
  }
  try {
    const res = await checkEmployeeEmail(em, isEdit ? route.params.id : null)
    if (res && res.exists) {
      emailTrungLoi.value = 'Email này đã được sử dụng bởi nhân viên khác'
    } else {
      emailTrungLoi.value = ''
    }
  } catch {
    emailTrungLoi.value = 'Không thể kiểm tra email trùng, vui lòng thử lại'
  }
}

const kiemTraSdtTrung = async () => {
  const sdt = (form.value.soDienThoai || '').trim()
  if (!/^0\d{9}$/.test(sdt)) {
    sdtTrungLoi.value = ''
    return
  }
  try {
    const res = await checkEmployeeSdt(sdt, isEdit ? route.params.id : null)
    if (res && res.exists) {
      sdtTrungLoi.value = 'Số điện thoại đã được sử dụng'
    } else {
      sdtTrungLoi.value = ''
    }
  } catch {
    sdtTrungLoi.value = 'Không thể kiểm tra số điện thoại trùng, vui lòng thử lại'
  }
}


const isCameraRunning = ref(false)
const cameraError = ref('')
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
  } catch { }
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
    form.value.queQuan = tinh.ten

    // 3. Đưa ngay Phường/Xã gốc vào danh sách để dropdown hiển thị tức thì (0ms)
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

    // 2. Điền thông tin vào form ngay lập tức (Họ tên, ngày sinh, giới tính, địa chỉ)
    if (info.hoTen) {
      form.value.tenTaiKhoan = info.hoTen
      capNhatMaGoiY()
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

    // 3. Cho người dùng nhìn thấy thông báo quét thành công trên camera trong 0.6s
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
  notifySuccess(`Quét CCCD thành công!`, `Đã nhận diện nhân viên: ${info.hoTen}`, 2000)

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
    html5QrCode = new Html5Qrcode('cccd-reader')
    isCameraRunning.value = true

    // Ưu tiên camera sau nếu có, hoặc dùng camera mặc định
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
      () => { }
    )
  } catch (err) {
    console.warn('Camera error:', err)
    isCameraRunning.value = false
    cameraError.value = 'Không thể mở Camera (chưa cấp quyền truy cập hoặc camera đang bận).'
  }
}

const stopCamera = async () => {
  // 1. Dừng trực tiếp tất cả MediaStream tracks trên mọi thẻ video để đèn camera phần cứng tắt ngay lập tức
  try {
    const videoEls = document.querySelectorAll('#cccd-reader video, video')
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
      html5QrCode = new Html5Qrcode('cccd-reader')
    }
    isProcessingScan.value = true
    const decodedText = await html5QrCode.scanFile(file, true)
    await xuLyChuoiCccd(decodedText)
  } catch (err) {
    isProcessingScan.value = false
    notifyWarning('Không tìm thấy mã QR hợp lệ trong hình ảnh tải lên')
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

const loiNgaySinh = computed(() =>
  kiemTra([
    batBuoc(form.value.ngaySinh, 'Ngày sinh'),
    khongDuocTuongLai(form.value.ngaySinh, 'Ngày sinh'),
    duTuoiToiThieu(form.value.ngaySinh, TUOI_TOI_THIEU, THONG_BAO_TUOI),
  ]),
)

const hienThiLoiNgaySinh = computed(() =>
  !isBlank(form.value.ngaySinh) || daNopForm.value || ngaySinhDaCham.value
    ? loiNgaySinh.value
    : '',
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
    isBlank(form.value.tenTaiKhoan) ? 'Vui lòng nhập họ và tên' : '',
    toiDa(form.value.tenTaiKhoan, 100, 'Họ và tên'),
  ]),
)

const loiEmail = computed(() =>
  kiemTra([
    isBlank(form.value.email) ? 'Vui lòng nhập email' : '',
    toiDa(form.value.email, 150, 'Email'),
    emailHopLe(form.value.email),
  ]),
)

const loiSdt = computed(() =>
  kiemTra([
    isBlank(form.value.soDienThoai) ? 'Vui lòng nhập số điện thoại' : '',
    toiDa(form.value.soDienThoai, 10, 'Số điện thoại'),
    soDienThoaiHopLe(form.value.soDienThoai),
  ]),
)

const loiVaiTro = computed(() => chonBatBuoc(form.value.idVaiTro, 'vai trò'))
const loiGioiTinh = computed(() => batBuoc(form.value.gioiTinh, 'Giới tính'))
const loiQueQuan = computed(() =>
  kiemTra([
    isBlank(form.value.queQuan) ? 'Vui lòng chọn tỉnh/thành phố' : '',
    toiDa(form.value.queQuan, 150, 'Tỉnh/Thành phố'),
  ]),
)
const loiPhuong = computed(() =>
  kiemTra([
    isBlank(form.value.phuong) ? 'Vui lòng chọn xã/phường/thị trấn' : '',
    toiDa(form.value.phuong, 100, 'Phường/Xã'),
  ]),
)
const loiDiaChiCuThe = computed(() => toiDa(form.value.diaChiCuThe, 255, 'Địa chỉ cụ thể'))

const validate = () =>
  kiemTra([
    loiTen.value,
    loiEmail.value,
    emailTrungLoi.value,
    loiSdt.value,
    sdtTrungLoi.value,
    loiVaiTro.value,
    loiGioiTinh.value,
    loiNgaySinh.value,
    loiQueQuan.value,
    loiPhuong.value,
    loiDiaChiCuThe.value,
  ])

const buildPayload = () => ({
  maNhanVien: form.value.maNhanVien ? form.value.maNhanVien.trim() : null,
  tenTaiKhoan: form.value.tenTaiKhoan.trim(),
  matKhau: form.value.matKhau ? form.value.matKhau : null,
  email: form.value.email.trim(),
  soDienThoai: form.value.soDienThoai.trim(),
  ngaySinh: form.value.ngaySinh || null,
  gioiTinh: form.value.gioiTinh || null,
  idVaiTro: Number(form.value.idVaiTro),
  queQuan: form.value.queQuan || null,
  phuong: form.value.phuong || null,
  diaChiCuThe: form.value.diaChiCuThe ? form.value.diaChiCuThe.trim() : null,
  anhNhanVien: anhDaChon.value || null,
  trangThai: form.value.trangThai === 'true',
  nguoiCapNhat: layNguoiDung(),
})

const submit = async () => {
  daNopForm.value = true
  if (form.value.email.trim()) {
    await kiemTraEmailTrung()
  }
  if (form.value.soDienThoai.trim()) {
    await kiemTraSdtTrung()
  }
  const error = validate()
  if (error) {
    errorMsg.value = ''
    return
  }
  errorMsg.value = ''

  const actionTitle = isEdit ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới'
  const actionMessage = isEdit
    ? `Bạn có chắc chắn muốn cập nhật thông tin nhân viên "${form.value.tenTaiKhoan}"?`
    : `Bạn có chắc chắn muốn thêm mới nhân viên "${form.value.tenTaiKhoan}"?`
  const ok = await notifyConfirm(actionTitle, actionMessage, isEdit ? 'Cập nhật' : 'Thêm mới', 'Hủy bỏ')
  if (!ok) return

  saving.value = true
  let emailThatBai = false
  try {
    const payload = buildPayload()
    let msg = ''
    if (isEdit) {
      await updateEmployee(route.params.id, payload)
      msg = `Đã cập nhật thông tin nhân viên "${form.value.tenTaiKhoan}" thành công!`
      setFlash(msg)
    } else {
      const taoMoi = await createEmployee(payload)
      const emailNhan = taoMoi.email || form.value.email || '(chưa có địa chỉ email)'
      msg = `Đã tạo nhân viên "${taoMoi.tenTaiKhoan}" (${taoMoi.maNhanVien}) thành công!`
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
      notifyWarning('Tạo mới thành công, gửi email thất bại', msg)
    } else {
      notifySuccess(msg)
    }
    setTimeout(() => {
      router.push('/nhan-vien')
    }, 600)
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
  try {
    const ds = await layDiaGioi()
    if (myToken !== yeuCauHienTai) return
    tinhThanhs.value = ds
    const tinh = timTinh(ds, tenTinh)
    phuongXas.value = await layPhuongXa(ds, tinh)
    if (tenPhuong && !phuongXas.value.some((p) => p.ten === tenPhuong)) {
      phuongXas.value = [{ code: 'khac', ten: tenPhuong }, ...phuongXas.value]
    }
  } catch {
  } finally {
    if (myToken === yeuCauHienTai) diaGioiLoading.value = false
  }
}

const chonTinh = async (tenTinh) => {
  form.value.queQuan = tenTinh || ''
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
  loading.value = true
  try {
    const data = await getEmployeeFormData()
    vaiTros.value = data.vaiTros || []

    if (!isEdit && vaiTros.value.length > 0) {
      const vtStaff = vaiTros.value.find((v) => v.ma === 'STAFF' || v.ten.toLowerCase().includes('nhân viên'))
      form.value.idVaiTro = vtStaff ? vtStaff.id : vaiTros.value[0].id
    }

    if (isEdit) {
      const nv = await getEmployee(route.params.id)
      form.value = {
        maNhanVien: nv.maNhanVien || '',
        tenTaiKhoan: nv.tenTaiKhoan || '',
        matKhau: '',
        email: nv.email || '',
        soDienThoai: nv.soDienThoai || '',
        ngaySinh: nv.ngaySinh || '',
        gioiTinh: nv.gioiTinh || 'NAM',
        idVaiTro: nv.idVaiTro || '',
        queQuan: nv.queQuan || '',
        phuong: nv.phuong || '',
        diaChiCuThe: nv.diaChiCuThe || '',
        trangThai: nv.trangThai === false ? 'false' : 'true',
      }
      anhDaChon.value = nv.anhNhanVien || ''
    }
    await napDiaGioi(form.value.queQuan, form.value.phuong)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="employee-create-page">
    <div class="top-nav-bar">
      <button class="btn-back" type="button" title="Quay lại danh sách" @click="router.push('/nhan-vien')">
        <i class="bi bi-arrow-left"></i>
      </button>
    </div>

    <div class="create-layout">
      <div class="avatar-card-col">
        <div class="avatar-box" @click="moChonAnh">
          <input ref="fileInputEl" type="file" accept="image/*" style="display: none;" @change="onFileChange" />

          <div class="avatar-circle">
            <img v-if="anhDaChon" :src="anhDaChon" alt="Avatar" class="avatar-img" />
            <span v-else class="avatar-letter">
              {{ form.tenTaiKhoan.trim() ? form.tenTaiKhoan.trim().charAt(0).toUpperCase() : 'N' }}
            </span>
          </div>

          <h3 class="avatar-title">
            {{ form.tenTaiKhoan.trim() ? form.tenTaiKhoan.trim() : 'Nhân viên mới' }}
          </h3>

          <p class="avatar-sub">
            {{ form.email.trim() ? form.email.trim() : 'Chưa cập nhật email' }}
          </p>

          <span class="avatar-hint">(Bấm vào ảnh để chọn avatar)</span>
        </div>
      </div>

      <div class="form-content-col">
        <section class="info-card">
          <div class="card-header-row">
            <div class="card-title-group">
              <div class="header-icon-box purple">
                <i class="bi bi-person"></i>
              </div>
              <div>
                <h2 class="card-title">Thông tin cơ bản</h2>
                <p class="card-subtitle">Họ tên, email, liên hệ và tài khoản.</p>
              </div>
            </div>
            <button class="btn-scan-cccd" type="button" @click="moModalQuetCccd">
              <i class="bi bi-qr-code-scan"></i>
              <span>Quét mã CCCD</span>
            </button>
          </div>

          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Mã nhân viên</label>
              <input class="form-input" type="text"
                :value="isEdit ? form.maNhanVien : maPreview"
                disabled
                placeholder="Tự động tạo khi nhập họ và tên" />
              <p class="field-hint" style="font-size: 12px; color: #64748b; margin-top: 4px;">
                Mã tự động tạo, không thể chỉnh sửa.
              </p>
            </div>

            <div class="form-group">
              <label class="form-label">Họ và tên <span class="req">*</span></label>
              <input v-model="form.tenTaiKhoan" type="text" class="form-input" placeholder="Nhập họ và tên"
                maxlength="100" @input="capNhatMaGoiY" @blur="cham('tenTaiKhoan')" />
              <p v-if="hienLoi('tenTaiKhoan', loiTen)" class="input-error">{{ loiTen }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">Email <span class="req">*</span></label>
              <input v-model="form.email" type="email" class="form-input"
                :class="{ 'has-error': emailTrungLoi || hienLoi('email', loiEmail) }"
                placeholder="Nhập email" maxlength="150" @blur="cham('email'); kiemTraEmailTrung()" />
              <p v-if="hienLoi('email', loiEmail)" class="input-error">{{ loiEmail }}</p>
              <p v-else-if="emailTrungLoi" class="input-error">{{ emailTrungLoi }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">Số điện thoại <span class="req">*</span></label>
              <input v-model="form.soDienThoai" type="tel" class="form-input" placeholder="Nhập số điện thoại"
                :class="{ 'has-error': sdtTrungLoi || hienLoi('soDienThoai', loiSdt) }"
                maxlength="10" @blur="cham('soDienThoai'); kiemTraSdtTrung()" />
              <p v-if="hienLoi('soDienThoai', loiSdt)" class="input-error">{{ loiSdt }}</p>
              <p v-else-if="sdtTrungLoi" class="input-error">{{ sdtTrungLoi }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">Vai trò <span class="req">*</span></label>
              <SearchableSelect
                v-model="form.idVaiTro"
                :options="vaiTros.map((v) => ({ value: v.id, label: v.ten }))"
                value-key="value"
                label-key="label"
                placeholder="Chọn vai trò"
                search-placeholder="Tìm vai trò..."
                @change="(v) => { form.idVaiTro = v === null ? '' : v; cham('idVaiTro') }" />
              <p v-if="hienLoi('idVaiTro', loiVaiTro)" class="input-error">{{ loiVaiTro }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">Giới tính <span class="req">*</span></label>
              <div class="radio-row" role="radiogroup" aria-label="Giới tính" @change="cham('gioiTinh')">
                <label v-for="g in gioiTinhOptions" :key="g.value" class="radio-item">
                  <input v-model="form.gioiTinh" type="radio" name="gioiTinh" :value="g.value" />
                  <span class="radio-dot"></span>
                  <span>{{ g.label }}</span>
                </label>
              </div>
              <p v-if="hienLoi('gioiTinh', loiGioiTinh)" class="input-error">{{ loiGioiTinh }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">Ngày sinh <span class="req">*</span></label>
              <div class="date-input-wrapper">
                <input v-model="form.ngaySinh" type="date" class="form-input" placeholder="mm/dd/yyyy"
                  @blur="ngaySinhDaCham = true" />
              </div>
              <p v-if="hienThiLoiNgaySinh" class="input-error">{{ hienThiLoiNgaySinh }}</p>
            </div>
          </div>
        </section>

        <section class="info-card">
          <div class="card-header-row" style="margin-bottom: 20px;">
            <div class="card-title-group">
              <div class="header-icon-box blue">
                <i class="bi bi-geo-alt"></i>
              </div>
              <div>
                <h2 class="card-title">Địa chỉ</h2>
              </div>
            </div>
          </div>

          <div v-if="diaChiCccdGoc" class="cccd-address-display">
            <div class="cccd-address-header">
              <i class="bi bi-qr-code-scan"></i>
              <span>Địa chỉ trích xuất từ CCCD:</span>
            </div>
            <div class="cccd-address-val">
              {{ diaChiCccdGoc }}
            </div>
          </div>

          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Tỉnh/Thành phố <span class="req">*</span></label>
              <SearchableSelect
                v-model="form.queQuan"
                :options="tinhThanhs.map((t) => ({ value: t.ten, label: t.ten }))"
                :placeholder="diaGioiLoading ? 'Đang tải tỉnh thành...' : 'Chọn tỉnh thành'"
                :disabled="diaGioiLoading"
                @update:model-value="(v) => { cham('queQuan'); chonTinh(v) }"
              />
              <p v-if="hienLoi('queQuan', loiQueQuan)" class="input-error">{{ loiQueQuan }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">Xã/Phường/Thị trấn <span class="req">*</span></label>
              <SearchableSelect
                v-model="form.phuong"
                :options="phuongXas.map((p) => ({ value: p.ten, label: p.ten }))"
                :placeholder="diaGioiLoading ? 'Đang tải...' : 'Chọn xã phường'"
                :disabled="diaGioiLoading || !form.queQuan"
                @update:model-value="cham('phuong')"
              />
              <p v-if="hienLoi('phuong', loiPhuong)" class="input-error">{{ loiPhuong }}</p>
            </div>
          </div>

          <div class="form-group" style="margin-top: 16px;">
            <label class="form-label">Địa chỉ cụ thể</label>
            <input v-model="form.diaChiCuThe" type="text" class="form-input" placeholder="Nhập địa chỉ cụ thể"
              maxlength="255" @blur="cham('diaChiCuThe')" />
            <p v-if="hienLoi('diaChiCuThe', loiDiaChiCuThe)" class="input-error">{{ loiDiaChiCuThe }}</p>
          </div>

          <div class="action-buttons-group">
            <button class="btn-submit-action" type="button" :disabled="saving" @click="submit">
              <i class="bi bi-person-check-fill"></i>
              <span>{{ saving ? 'Đang lưu...' : isEdit ? 'Cập nhật nhân viên' : 'Tạo nhân viên' }}</span>
            </button>

            <button class="btn-cancel-action" type="button" @click="router.push('/nhan-vien')">
              Hủy
            </button>
          </div>
        </section>
      </div>
    </div>

    <div v-if="isScanModalOpen" class="modal-backdrop" @click.self="dongModalQuet">
      <div class="scan-modal-card">
        <div class="scan-modal-header">
          <div class="scan-header-left">
            <div class="scan-header-icon">
              <i class="bi bi-qr-code-scan"></i>
            </div>
            <div>
              <h3 class="scan-header-title">Quét thẻ Căn cước công dân (CCCD)</h3>
              <p class="scan-header-subtitle">Tự động nhận diện và trích xuất thông tin công dân</p>
            </div>
          </div>
          <button type="button" class="btn-close-modal" title="Đóng" @click="dongModalQuet">×</button>
        </div>

        <div class="scan-modal-body">
          <div class="camera-scan-wrap">
            <div class="camera-viewport-box">
              <div id="cccd-reader" class="camera-stream-el"></div>

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
.employee-create-page {
  padding: 8px 12px 60px;
  background-color: transparent;
  min-height: 100%;
}

.top-nav-bar {
  margin-bottom: 16px;
}

.btn-back {
  background: transparent;
  border: none;
  font-size: 20px;
  color: #1e293b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #e2e8f0;
}

/* Layout 2 cột */
.create-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

/* CỘT TRÁI: CARD AVATAR */
.avatar-card-col {
  min-width: 0;
}

.avatar-box {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 16px;
  padding: 36px 20px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
}

.avatar-box:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
}

.avatar-circle {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  background: #edf2f7;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-bottom: 20px;
  transition: all 0.2s ease;
}

.avatar-box:hover .avatar-circle {
  transform: scale(1.02);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-letter {
  font-size: 52px;
  font-weight: 500;
  color: #64748b;
  user-select: none;
}

.avatar-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  word-break: break-word;
}

.avatar-sub {
  margin: 0 0 12px;
  font-size: 13px;
  color: #94a3b8;
  word-break: break-word;
}

.avatar-hint {
  font-size: 12px;
  color: #94a3b8;
  user-select: none;
}

/* CỘT PHẢI: FORM CHÍNH */
.form-content-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.info-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 16px;
  padding: 24px 28px 28px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

/* Header của Card */
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.card-title-group {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
}

.header-icon-box.purple {
  background: #f5f3ff;
  color: #8b5cf6;
}

.header-icon-box.blue {
  background: #f0f9ff;
  color: #0284c7;
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.card-subtitle {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}

/* Nút quét mã CCCD */
.btn-scan-cccd {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1px solid #fed7aa;
  border-radius: 9999px;
  padding: 7px 18px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-scan-cccd i {
  font-size: 14px;
  color: #1e293b;
}

.btn-scan-cccd:hover {
  background: #fff7ed;
  border-color: #f97316;
}

/* Grid form 2 cột */
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 20px;
  row-gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.form-label .req {
  color: #ef4444;
  margin-left: 2px;
}

/* Inputs */
.form-input {
  height: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  padding: 0 14px;
  font-size: 13.5px;
  color: #1e293b;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.2s ease;
  outline: none;
}

.form-input::placeholder {
  color: #94a3b8;
}

.form-input:focus {
  border-color: #f43f5e;
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.form-input.has-error {
  border-color: #ef4444;
}

.input-error {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: #ef4444;
  font-weight: 500;
}

/* Cụm nút hành động */
.action-buttons-group {
  margin-top: 28px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.btn-submit-action {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #ef4444 0%, #e11d48 100%);
  color: #ffffff;
  border: none;
  border-radius: 9999px;
  padding: 10px 24px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(225, 29, 72, 0.25);
  transition: all 0.2s ease;
}

.btn-submit-action:hover:not(:disabled) {
  opacity: 0.94;
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(225, 29, 72, 0.32);
}

.btn-submit-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel-action {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  border-radius: 9999px;
  padding: 10px 24px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel-action:hover {
  background: #f8fafc;
  color: #1e293b;
  border-color: #cbd5e1;
}

/* Modal Quét CCCD */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
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
  from {
    transform: scale(0.96);
    opacity: 0;
  }

  to {
    transform: scale(1);
    opacity: 1;
  }
}

.scan-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 24px;
  color: #64748b;
  cursor: pointer;
  line-height: 1;
}

.scan-modal-body {
  padding: 20px;
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
  border-color: #e11d48;
  box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.1);
}

@media (max-width: 900px) {
  .create-layout {
    grid-template-columns: 1fr;
  }

  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}

/* ==========================================================================
   CAMERA SCANNER & CCCD MODAL STYLES
   ========================================================================== */
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
  background: #fff1f2;
  color: #e11d48;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
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

/* Tia laser quét chạy lên xuống */
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

/* 4 góc khung quét */
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

.form-input:disabled {
  background: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
  -webkit-text-fill-color: #94a3b8;
  opacity: 1;
}
</style>
