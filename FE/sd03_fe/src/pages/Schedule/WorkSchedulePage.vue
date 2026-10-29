<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PaginationBar from '@/components/PaginationBar.vue'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { listEmployees } from '@/api/employee'
import { getDangMo } from '@/api/giaoCa'
import { notifySuccess, notifyWarning, notifyError, notifyDeleteConfirm } from '@/utils/notify'
import {
  listLich,
  createLich,
  updateLich,
  deleteLich,
  deleteLichTheoNgay,
  xepTuDong,
  importLich,
  listCaLam,
} from '@/api/schedule'
import {
  hienThiNgay,
  khoaNgay,
  docNgay,
  xuatMauLichLamViec,
  phanTichCsvLich,
  phanTichDongLich,
} from './scheduleData'

const router = useRouter()
const pad2 = (x) => String(x).padStart(2, '0')

/* ---------------------------------- Dữ liệu ca hiện tại (kiểm tra vào ca) ---------------------------------- */
const caHienTai = ref(null)

const kiemTraCaHienTai = async () => {
  try {
    caHienTai.value = await getDangMo()
  } catch {
    caHienTai.value = null
  }
}

const diToiGiaoCa = () => {
  router.push('/ca')
}

/* ---------------------------------- Dữ liệu nhân viên + ca + lịch ---------------------------------- */
const nhanViens = ref([])
const cas = ref([])
const lichs = ref([])
const dsNhanVien = computed(() => nhanViens.value)
const caDangBat = computed(() => cas.value.filter((c) => c.dangBat))
const caHienThi = computed(() => {
  const active = cas.value.filter((c) => c.dangBat)
  const list = active.length > 0 ? active : cas.value
  return [...list].sort((a, b) => String(a.batDau || '').localeCompare(String(b.batDau || '')))
})

const tenCua = (ma) => {
  const nv = dsNhanVien.value.find((x) => String(x.maNhanVien) === String(ma))
  return nv ? nv.tenTaiKhoan || nv.ten || ma : ma
}

const formatGioPhut = (t) => {
  if (!t) return '--:--'
  return String(t).slice(0, 5)
}

const getAvatarInitial = (name) => {
  if (!name) return '?'
  const clean = String(name).trim()
  if (!clean) return '?'
  const parts = clean.split(/\s+/)
  const last = parts[parts.length - 1]
  return (last[0] || '?').toUpperCase()
}

const timCa = (id) => cas.value.find((c) => c.id === Number(id))

const taiDuLieu = async () => {
  try {
    const [nvRes, caRes, lichRes] = await Promise.allSettled([
      listEmployees({ page: 0, size: 1000 }),
      listCaLam(),
      listLich({}),
    ])
    if (nvRes.status === 'fulfilled') nhanViens.value = nvRes.value?.content || []
    if (caRes.status === 'fulfilled') cas.value = caRes.value || []
    if (lichRes.status === 'fulfilled') lichs.value = lichRes.value || []
    await kiemTraCaHienTai()
  } catch (err) {
    console.error('Lỗi khi tải dữ liệu lịch:', err)
  }
}

/* ---------------------------------- Bộ lọc + bảng ---------------------------------- */
const fKeyword = ref('')
const fCa = ref('')
const fNgay = ref('')

const caFilterOptions = computed(() => [
  { value: '', label: 'Tất cả ca' },
  ...caDangBat.value.map((ca) => ({ value: ca.id, label: ca.ten }))
])

const onCaFilterChange = (val) => {
  fCa.value = val ?? ''
  page.value = 0
}
const view = ref('lich')
const calMode = ref('tuan')
const anchor = ref(new Date())
anchor.value.setHours(0, 0, 0, 0)

const datePickerInput = ref(null)

const moDatePicker = () => {
  if (datePickerInput.value) {
    if (typeof datePickerInput.value.showPicker === 'function') {
      datePickerInput.value.showPicker()
    } else {
      datePickerInput.value.focus()
    }
  }
}

const onDatePicked = (e) => {
  const val = e.target.value
  if (!val) return
  const d = docNgay(val)
  if (d) {
    d.setHours(0, 0, 0, 0)
    anchor.value = d
  }
}

const chuyenTuan = (huong) => {
  const d = new Date(anchor.value)
  d.setDate(d.getDate() + huong * 7)
  d.setHours(0, 0, 0, 0)
  anchor.value = d
}

const veHomNay = () => {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  anchor.value = now
}

const dateDisplayFormatted = computed(() => {
  const d = anchor.value
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`
})
const dateDisplayMmDdYyyy = dateDisplayFormatted

const page = ref(0)
const size = ref(10)

const lichLoc = computed(() => {
  const kw = fKeyword.value.trim().toLowerCase()
  let rows = [...lichs.value]
  if (kw) {
    rows = rows.filter(
      (l) =>
        String(l.maNhanVien).toLowerCase().includes(kw) ||
        String(l.tenNhanVien || '').toLowerCase().includes(kw) ||
        tenCua(l.maNhanVien).toLowerCase().includes(kw)
    )
  }
  if (fCa.value) rows = rows.filter((l) => l.idCa === Number(fCa.value))
  rows.sort((a, b) =>
    a.ngay === b.ngay
      ? a.idCa - b.idCa || String(a.maNhanVien).localeCompare(String(b.maNhanVien))
      : String(a.ngay).localeCompare(String(b.ngay))
  )
  return rows
})

const tongTrang = computed(() => Math.max(1, Math.ceil(lichLoc.value.length / size.value)))
const dongHienTai = computed(() => {
  const start = page.value * size.value
  return lichLoc.value.slice(start, start + size.value)
})
watch(lichLoc, () => {
  if (page.value >= tongTrang.value) page.value = 0
})

const datTrang = (p) => {
  if (p < 0 || p >= tongTrang.value || p === page.value) return
  page.value = p
}

const doiSize = (s) => {
  size.value = s
  page.value = 0
}

const datLaiBoLoc = () => {
  fKeyword.value = ''
  fCa.value = ''
  fNgay.value = ''
  page.value = 0
}

const tenCaCua = (id) => timCa(id)?.ten || '—'
const gioCua = (id) => {
  const ca = timCa(id)
  return ca ? `${formatGioPhut(ca.batDau)} - ${formatGioPhut(ca.ketThuc)}` : '—'
}

watch(fNgay, (v) => {
  const d = docNgay(v)
  if (d) {
    d.setHours(0, 0, 0, 0)
    anchor.value = d
  }
})

/* ---------------------------------- Thao tác hàng loạt ---------------------------------- */
const xepCaTuDongBtn = async () => {
  const khoa = fNgay.value ? khoaNgay(docNgay(fNgay.value)) : ''
  if (!khoa) return
  try {
    const kq = await xepTuDong(khoa)
    if (kq.lyDo === 'khongCa') {
      notifyWarning('Chưa có ca đang bật', 'Hãy bật ca làm ở trang Lịch ca làm trước')
      return
    }
    if (kq.lyDo === 'khongUngVien') {
      notifyWarning('Không còn nhân viên trống', 'Mọi nhân viên đã có lịch trong ngày này')
      return
    }
    await taiDuLieu()
    notifySuccess('Đã xếp ca tự động', `Thêm ${kq.themMoi} lịch làm việc ngày ${hienThiNgay(khoa)}`)
  } catch (e) {
    notifyWarning('Lỗi xếp ca tự động', e.response?.data?.message || e.message || '')
  }
}

const datLaiLich = async () => {
  const khoa = fNgay.value ? khoaNgay(docNgay(fNgay.value)) : ''
  if (!khoa) {
    notifyWarning('Chưa chọn Ngày làm', 'Hãy chọn ngày cần đặt lại lịch trước')
    return
  }
  const ok = await notifyDeleteConfirm(`tất cả lịch làm việc ngày ${hienThiNgay(khoa)}`)
  if (!ok) return
  try {
    await deleteLichTheoNgay(khoa)
    await taiDuLieu()
    notifySuccess('Đã đặt lại lịch', `Đã xóa lịch ngày ${hienThiNgay(khoa)}`)
  } catch (e) {
    notifyWarning('Lỗi xóa lịch', e.response?.data?.message || e.message || '')
  }
}

const taiTemplate = async () => {
  try {
    await xuatMauLichLamViec()
    notifySuccess('Đã tải tệp mẫu', 'Tệp Excel (.xlsx) nhập được trực tiếp vào hệ thống')
  } catch (e) {
    notifyError('Không tải được tệp mẫu', e.message || '')
  }
}

/* ---------------------------------- Nhập Excel / CSV ---------------------------------- */
const showImport = ref(false)
const importFileName = ref('')
const importPreview = ref(null)
const importLoading = ref(false)

const moImport = () => {
  importFileName.value = ''
  importPreview.value = null
  showImport.value = true
}

const dongImport = () => {
  showImport.value = false
}

const chuyenOChu = (v) => {
  if (v === null || v === undefined) return ''
  if (v instanceof Date) return `${pad2(v.getDate())}/${pad2(v.getMonth() + 1)}/${v.getFullYear()}`
  if (typeof v === 'object') {
    if (Array.isArray(v.richText)) return v.richText.map((r) => r.text || '').join('')
    if (v.text !== undefined) return String(v.text)
    if (v.result !== undefined) return chuyenOChu(v.result)
    return ''
  }
  return String(v).trim()
}

/** Đọc tệp Excel (.xlsx/.xls) thành các dòng gồm MẢNG Ô riêng theo từng cột. */
const docExcelTrenFile = async (file) => {
  const ExcelJS = (await import('exceljs')).default
  const wb = new ExcelJS.Workbook()
  await wb.xlsx.load(await file.arrayBuffer())
  const ws = wb.worksheets[0]
  if (!ws) return []
  const cacDong = []
  ws.eachRow((row) => {
    const o = row.values
    cacDong.push((Array.isArray(o) ? o.slice(1) : []).map(chuyenOChu))
  })
  return cacDong
}

const docTrenFile = async (e) => {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  importFileName.value = file.name
  importPreview.value = null
  importLoading.value = true
  try {
    if (/\.csv$/i.test(file.name)) {
      importPreview.value = phanTichCsvLich(await file.text(), dsNhanVien.value, cas.value)
    } else if (/\.xlsx?$/i.test(file.name)) {
      importPreview.value = phanTichDongLich(await docExcelTrenFile(file), dsNhanVien.value, cas.value)
    } else {
      notifyWarning('Sai định dạng tệp', 'Chỉ hỗ trợ tệp Excel (.xlsx, .xls) hoặc CSV (.csv)')
      importFileName.value = ''
      return
    }
    if (importPreview.value && importPreview.value.loi.length && !importPreview.value.hopLe) {
      notifyWarning('Tệp chưa có dòng hợp lệ', 'Xem lại thông báo lỗi ở dưới')
    }
  } catch (err) {
    importPreview.value = null
    importFileName.value = ''
    notifyError('Không đọc được tệp', err.message || 'Tệp không hợp lệ')
  } finally {
    importLoading.value = false
  }
}

const xacNhanNhap = async () => {
  const p = importPreview.value
  if (!p || !p.rows.length) return
  try {
    const rows = p.rows.map((r) => ({
      maNhanVien: r.maNhanVien,
      maCa: cas.value.find((c) => c.id === r.idCa)?.ma || String(r.idCa),
      ngay: r.ngay,
    }))
    const kq = await importLich(rows)
    await taiDuLieu()
    showImport.value = false
    view.value = 'bang'
    notifySuccess(
      'Hoàn tất nhập lịch làm việc',
      `Đã thêm ${kq.themMoi} lịch; bỏ qua ${kq.loi?.length || 0} dòng lỗi`
    )
  } catch (e) {
    notifyWarning('Lỗi nhập lịch', e.response?.data?.message || e.message || '')
  }
}

/* ---------------------------------- Form thêm / sửa ---------------------------------- */
const showForm = ref(false)
const formMode = ref('them')
const form = ref({ id: null, maNhanVien: '', idCa: '', ngay: '' })
const formError = ref('')

const nvFormOptions = computed(() =>
  dsNhanVien.value.map((nv) => ({
    value: nv.maNhanVien,
    label: `${nv.maNhanVien} — ${nv.tenTaiKhoan || nv.ten || nv.maNhanVien}`
  }))
)

const caFormOptions = computed(() =>
  caDangBat.value.map((ca) => ({
    value: ca.id,
    label: `${ca.ten} (${formatGioPhut(ca.batDau)} - ${formatGioPhut(ca.ketThuc)})`
  }))
)

const onFormNvChange = (val) => {
  form.value.maNhanVien = val ?? ''
}

const onFormCaChange = (val) => {
  form.value.idCa = val ?? ''
}

const moFormThem = () => {
  formMode.value = 'them'
  form.value = {
    id: null,
    maNhanVien: '',
    idCa: caDangBat.value[0]?.id || '',
    ngay: fNgay.value || khoaNgay(new Date()),
  }
  formError.value = ''
  showForm.value = true
}

const moFormSua = (row) => {
  formMode.value = 'sua'
  form.value = {
    id: row.id,
    maNhanVien: row.maNhanVien,
    idCa: row.idCa,
    ngay: row.ngay,
  }
  formError.value = ''
  showForm.value = true
  showDetail.value = false
}

const luuForm = async () => {
  formError.value = ''
  const f = form.value
  if (!f.maNhanVien) {
    formError.value = 'Vui lòng chọn nhân viên'
    return
  }
  if (!f.idCa) {
    formError.value = 'Vui lòng chọn ca làm'
    return
  }
  const d = docNgay(f.ngay)
  if (!d) {
    formError.value = 'Ngày làm không hợp lệ'
    return
  }
  const khoa = khoaNgay(d)
  const payload = {
    maNhanVien: f.maNhanVien,
    idCa: Number(f.idCa),
    ngay: khoa,
  }
  try {
    if (formMode.value === 'sua') await updateLich(f.id, payload)
    else await createLich(payload)
    await taiDuLieu()
    showForm.value = false
    notifySuccess(
      formMode.value === 'sua' ? 'Đã cập nhật lịch làm việc' : 'Đã thêm lịch làm việc',
      `${payload.maNhanVien} · ${tenCaCua(payload.idCa)} · ${hienThiNgay(khoa)}`
    )
  } catch (e) {
    formError.value = e.response?.data?.message || e.message || 'Lỗi khi lưu lịch'
  }
}

/* ---------------------------------- Chi tiết / xem thêm ---------------------------------- */
const showDetail = ref(false)
const detailRow = ref(null)

const moChiTiet = (row) => {
  detailRow.value = row
  showDetail.value = true
}

const xoaChiTiet = async () => {
  const row = detailRow.value
  if (!row) return
  const ok = await notifyDeleteConfirm(
    `lịch làm việc của ${tenCua(row.maNhanVien)} ngày ${hienThiNgay(row.ngay)}`
  )
  if (!ok) return
  try {
    await deleteLich(row.id)
    await taiDuLieu()
    showDetail.value = false
    notifySuccess('Đã xóa lịch làm việc')
  } catch (e) {
    notifyWarning('Lỗi xóa lịch', e.response?.data?.message || e.message || '')
  }
}

const showXemThem = ref(false)
const xemThemData = ref({ ca: null, ngay: '', list: [] })

const lichCuaNgay = (ngay) => lichs.value.filter((l) => l.ngay === ngay)
const lichCuaCaNgay = (idCa, ngay) =>
  lichs.value.filter((l) => l.idCa === Number(idCa) && l.ngay === ngay)

const moXemThem = (ca, khoa) => {
  const list = lichCuaCaNgay(ca.id, khoa)
  xemThemData.value = { ca, ngay: khoa, list }
  showXemThem.value = true
}

/* ---------------------------------- Lịch: tính toán ---------------------------------- */
const homNayKhoa = khoaNgay(new Date())

const chuDeThang = computed(
  () => `Tháng ${anchor.value.getMonth() + 1} Năm ${anchor.value.getFullYear()}`
)

const lichThang = computed(() => {
  const d = anchor.value
  const dauThang = new Date(d.getFullYear(), d.getMonth(), 1)
  const thuDau = (dauThang.getDay() + 6) % 7
  const batDau = new Date(dauThang)
  batDau.setDate(dauThang.getDate() - thuDau)
  const cells = []
  for (let i = 0; i < 42; i++) {
    const ng = new Date(batDau)
    ng.setDate(batDau.getDate() + i)
    const khoa = khoaNgay(ng)
    const caList = caDangBat.value
      .map((ca) => ({ ca, n: lichCuaCaNgay(ca.id, khoa).length }))
      .filter((x) => x.n > 0)
    cells.push({
      khoa,
      so: ng.getDate(),
      ngoaiThang: ng.getMonth() !== d.getMonth(),
      homNay: khoa === homNayKhoa,
      tong: lichCuaNgay(khoa).length,
      chips: caList.slice(0, 3),
      thua: caList.reduce((a, x) => a + x.n, 0) - caList.slice(0, 3).reduce((a, x) => a + x.n, 0),
    })
    if (i >= 34 && ng.getMonth() !== d.getMonth() && ng.getDay() === 0) break
  }
  return cells
})

const dauTuan = computed(() => {
  const d = new Date(anchor.value)
  const day = d.getDay()
  const diff = day === 0 ? 1 : -(day - 1)
  d.setDate(d.getDate() + diff)
  d.setHours(0, 0, 0, 0)
  return d
})

const ngayTrongTuan = computed(() => {
  const tenThuList = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật']
  const out = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(dauTuan.value)
    d.setDate(dauTuan.value.getDate() + i)
    const khoa = khoaNgay(d)
    const ngayThang = `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}`
    out.push({
      d,
      khoa,
      tenThu: tenThuList[i],
      ngayThang,
    })
  }
  return out
})

const rangeTuanText = computed(() => {
  if (!ngayTrongTuan.value.length) return { tu: '', den: '' }
  return {
    tu: hienThiNgay(ngayTrongTuan.value[0].khoa),
    den: hienThiNgay(ngayTrongTuan.value[ngayTrongTuan.value.length - 1].khoa),
  }
})

const onCellClick = (ca, khoa) => {
  formMode.value = 'them'
  form.value = {
    id: null,
    maNhanVien: '',
    idCa: ca.id,
    ngay: khoa,
  }
  formError.value = ''
  showForm.value = true
}

const thuVN = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']

const chuDeTuan = computed(() => {
  const first = ngayTrongTuan.value[0].d
  const last = ngayTrongTuan.value[6].d
  const fmt = (x) => `${x.getDate()} tháng ${x.getMonth() + 1}, ${x.getFullYear()}`
  return `${fmt(first)} - ${fmt(last)}`
})

const chuDeNgay = computed(
  () => `${anchor.value.getDate()} tháng ${anchor.value.getMonth() + 1}, ${anchor.value.getFullYear()}`
)

const thuCuaNgay = computed(() => thuVN[(anchor.value.getDay() + 6) % 7])
const nhanNgayDangChon = computed(
  () =>
    `${pad2(anchor.value.getDate())}/${pad2(anchor.value.getMonth() + 1)}/${anchor.value.getFullYear()}`
)

const chuyenHuong = (huong) => {
  const d = new Date(anchor.value)
  if (calMode.value === 'thang') d.setMonth(d.getMonth() + huong, 1)
  else if (calMode.value === 'tuan') d.setDate(d.getDate() + huong * 7)
  else d.setDate(d.getDate() + huong)
  d.setHours(0, 0, 0, 0)
  anchor.value = d
}

const datKieuXem = (kieu) => {
  calMode.value = kieu
}

onMounted(() => {
  taiDuLieu()
})
</script>

<template>
  <div class="schedule-page-container">
    <!-- 1. THÔNG BÁO CHẾ ĐỘ XEM / VÀO CA LÀM VIỆC -->
    <div v-if="!caHienTai" class="shift-alert-banner">
      <div class="shift-alert-left">
        <span class="badge-mode">CHẾ ĐỘ XEM</span>
        <span class="shift-alert-text">
          Bạn chưa vào ca làm việc nên chỉ có quyền tra cứu thông tin. Các thao tác bán hàng, tạo đơn hàng và chỉnh sửa dữ liệu đang bị khóa.
        </span>
      </div>
      <button type="button" class="btn-enter-shift" @click="diToiGiaoCa">
        Vào ca làm việc ngay
      </button>
    </div>

    <!-- 2. BREADCRUMBS -->
    <div class="schedule-breadcrumbs">
      <span class="crumb-parent">Lịch làm việc</span>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Lịch làm việc</span>
    </div>

    <!-- 3. THANH CÔNG CỤ ĐIỀU HƯỚNG & CHẾ ĐỘ XEM -->
    <div class="schedule-toolbar">
      <div class="toolbar-left">
        <!-- Hộp chọn ngày tuần: < MM/DD/YYYY 📅 > -->
        <div class="date-navigator-box">
          <button class="nav-arrow-btn" type="button" title="Tuần trước" @click="chuyenTuan(-1)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>

          <div class="date-display-wrap" title="Chọn ngày để chuyển tuần" @click="moDatePicker">
            <span class="date-text">{{ dateDisplayFormatted }}</span>
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
              <line x1="16" y1="2" x2="16" y2="6"></line>
              <line x1="8" y1="2" x2="8" y2="6"></line>
              <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
            <input
              ref="datePickerInput"
              type="date"
              class="hidden-date-input"
              :value="khoaNgay(anchor)"
              @change="onDatePicked"
            />
          </div>

          <button class="nav-arrow-btn" type="button" title="Tuần sau" @click="chuyenTuan(1)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </button>
        </div>

        <button class="btn-today" type="button" @click="veHomNay">
          Hôm nay
        </button>

        <div class="week-range-label">
          Tuần từ: <strong>{{ rangeTuanText.tu }}</strong> đến <strong>{{ rangeTuanText.den }}</strong>
        </div>
      </div>

      <div class="toolbar-right">
        <div class="view-switch-group">
          <button
            type="button"
            class="btn-view-toggle"
            :class="{ active: view === 'lich' }"
            @click="view = 'lich'"
          >
            Dạng lịch
          </button>
          <button
            type="button"
            class="btn-view-toggle"
            :class="{ active: view === 'bang' }"
            @click="view = 'bang'"
          >
            Dạng bảng
          </button>
        </div>
      </div>
    </div>

    <!-- 4. GIAO DIỆN DẠNG LỊCH (MA TRẬN LỊCH TUẦN ĐÚNG THEO HÌNH ẢNH) -->
    <div v-if="view === 'lich'" class="schedule-matrix-wrapper">
      <table class="schedule-matrix-table">
        <thead>
          <tr>
            <th class="col-shift-header">Ca / Ngày</th>
            <th
              v-for="day in ngayTrongTuan"
              :key="day.khoa"
              class="col-day-header"
              :class="{ 'is-today': day.khoa === homNayKhoa }"
            >
              <div class="header-day-name">{{ day.tenThu }}</div>
              <div class="header-day-date">{{ day.ngayThang }}</div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="caHienThi.length === 0">
            <td colspan="8" class="empty-matrix-cell">Chưa có ca làm việc nào</td>
          </tr>
          <tr v-for="ca in caHienThi" :key="ca.id" class="matrix-row">
            <td class="col-shift-info">
              <div class="shift-info-name">{{ ca.ten }}</div>
              <div class="shift-info-time">{{ formatGioPhut(ca.batDau) }} - {{ formatGioPhut(ca.ketThuc) }}</div>
            </td>
            <td
              v-for="day in ngayTrongTuan"
              :key="day.khoa"
              class="matrix-cell"
              :class="{ 'is-today': day.khoa === homNayKhoa }"
              @click="onCellClick(ca, day.khoa)"
            >
              <div class="cell-content">
                <div
                  v-for="item in lichCuaCaNgay(ca.id, day.khoa)"
                  :key="item.id"
                  class="emp-card"
                  :title="`Nhấp để xem/sửa lịch của ${item.tenNhanVien || tenCua(item.maNhanVien)}`"
                  @click.stop="moChiTiet(item)"
                >
                  <div class="emp-avatar">
                    {{ getAvatarInitial(item.tenNhanVien || tenCua(item.maNhanVien)) }}
                  </div>
                  <div class="emp-meta">
                    <span class="emp-name">{{ item.tenNhanVien || tenCua(item.maNhanVien) }}</span>
                    <span class="emp-sub">{{ item.maNhanVien }}</span>
                  </div>
                </div>

                <div
                  v-if="lichCuaCaNgay(ca.id, day.khoa).length === 0"
                  class="cell-empty-hint"
                  title="Nhấp để thêm nhân viên vào ca này"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                    <line x1="12" y1="5" x2="12" y2="19"></line>
                    <line x1="5" y1="12" x2="19" y2="12"></line>
                  </svg>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 5. GIAO DIỆN DẠNG BẢNG (KÈM BỘ LỌC + THAO TÁC HÀNG LOẠT) -->
    <template v-else>
      <!-- CARD BỘ LỌC -->
      <div class="filter-card">
        <div class="card-title-row">
          <div class="icon-circle icon-filter">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
            </svg>
          </div>
          <span class="card-title-text">Bộ lọc & Thao tác</span>
        </div>

        <div class="filter-controls-row">
          <div class="field-block grow">
            <div class="search-input-wrap">
              <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input v-model="fKeyword" type="text" class="input-search" placeholder="Tìm kiếm nhân viên..." @input="page = 0" />
            </div>
          </div>

          <div class="inline-field">
            <label class="inline-label">Ca làm:</label>
            <SearchableSelect
              v-model="fCa"
              :options="caFilterOptions"
              label-key="label"
              value-key="value"
              placeholder="Tất cả ca"
              search-placeholder="Tìm ca làm..."
              @change="onCaFilterChange"
            />
          </div>

          <div class="inline-field">
            <label class="inline-label">Ngày làm:</label>
            <input v-model="fNgay" type="date" class="input-date" />
          </div>
        </div>

        <div class="filter-actions-row is-left">
          <button
            class="btn-action-outline is-muted"
            type="button"
            :disabled="!fNgay"
            :title="fNgay ? 'Tự động xếp nhân viên còn trống vào các ca ngày này' : 'Hãy chọn Ngày làm trước'"
            @click="xepCaTuDongBtn"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="16 3 21 3 21 8"></polyline>
              <line x1="4" y1="20" x2="21" y2="3"></line>
              <polyline points="21 16 21 21 16 21"></polyline>
              <line x1="15" y1="15" x2="21" y2="21"></line>
              <line x1="4" y1="4" x2="9" y2="9"></line>
            </svg>
            Xếp ca tự động
          </button>

          <button class="btn-action-outline" type="button" @click="datLaiLich">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
            </svg>
            Đặt lại lịch
          </button>

          <div class="actions-spacer"></div>

          <button class="btn-action-primary" type="button" @click="taiTemplate">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            Tải template
          </button>

          <button class="btn-action-dark" type="button" @click="moImport">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            Nhập Excel
          </button>

          <button class="btn-action-primary" type="button" @click="moFormThem">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            Thêm mới lịch làm việc
          </button>
        </div>
      </div>

      <!-- CARD DANH SÁCH BẢNG -->
      <div class="table-card">
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 55px">STT</th>
                <th style="width: 130px">Mã nhân viên</th>
                <th>Nhân viên</th>
                <th style="width: 110px">Ca làm</th>
                <th style="width: 140px">Thời gian</th>
                <th style="width: 120px">Ngày làm</th>
                <th class="text-center" style="width: 130px">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="lichLoc.length === 0">
                <td colspan="7" class="text-center empty-row">Không có lịch làm việc nào phù hợp</td>
              </tr>
              <tr v-for="(row, i) in dongHienTai" :key="row.id" class="row-alt">
                <td class="cell-stt">{{ page * size + i + 1 }}</td>
                <td class="cell-code"><span class="code-chip">{{ row.maNhanVien }}</span></td>
                <td class="cell-name">{{ tenCua(row.maNhanVien) }}</td>
                <td><span class="shift-chip">{{ tenCaCua(row.idCa) }}</span></td>
                <td>{{ gioCua(row.idCa) }}</td>
                <td>{{ hienThiNgay(row.ngay) }}</td>
                <td class="text-center">
                  <div class="action-buttons-group">
                    <button class="btn-action-circle is-view" type="button" title="Xem chi tiết" @click="moChiTiet(row)">
                      <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn-action-circle is-delete" type="button" title="Xóa lịch" @click="moChiTiet(row)">
                      <i class="bi bi-trash3"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <PaginationBar
          :page="page"
          :total-pages="tongTrang"
          :total-elements="lichLoc.length"
          :page-size="size"
          :from="dongHienTai.length ? page * size + 1 : 0"
          :to="page * size + dongHienTai.length"
          :sizes="[5, 10, 20]"
          label="ca làm việc"
          @change="datTrang"
          @size-change="doiSize"
        />
      </div>
    </template>

    <!-- MODAL THÊM / SỬA -->
    <div v-if="showForm" class="modal-backdrop" @click.self="showForm = false">
      <div class="modal-card modal-card-schedule">
        <div class="modal-banner">
          <div class="modal-banner-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2"></rect>
              <line x1="16" y1="2" x2="16" y2="6"></line>
              <line x1="8" y1="2" x2="8" y2="6"></line>
              <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
          </div>
          <div class="modal-banner-text">
            <h3>{{ formMode === 'them' ? 'Thêm mới lịch làm việc' : 'Cập nhật lịch làm việc' }}</h3>
            <p>Nhân viên · Ca làm · Ngày làm</p>
          </div>
          <button class="btn-banner-close" type="button" @click="showForm = false">×</button>
        </div>

        <div class="modal-body">
          <div class="form-field">
            <label class="form-label">Nhân viên <span class="req">*</span></label>
            <SearchableSelect
              v-model="form.maNhanVien"
              :options="nvFormOptions"
              :searchable="false"
              label-key="label"
              value-key="value"
              placeholder="-- Chọn nhân viên --"
              @change="onFormNvChange"
            />
          </div>

          <div class="form-field">
            <label class="form-label">Ca làm <span class="req">*</span></label>
            <SearchableSelect
              v-model="form.idCa"
              :options="caFormOptions"
              :searchable="false"
              label-key="label"
              value-key="value"
              placeholder="-- Chọn ca làm --"
              @change="onFormCaChange"
            />
          </div>

          <div class="form-field">
            <label class="form-label">Ngày làm <span class="req">*</span></label>
            <input v-model="form.ngay" type="date" class="form-control" />
          </div>

          <p v-if="formError" class="form-error">
            <i class="bi bi-exclamation-circle"></i> {{ formError }}
          </p>
        </div>

        <div class="modal-footer">
          <button class="btn-ghost" type="button" @click="showForm = false">Hủy</button>
          <button class="btn-primary" type="button" @click="luuForm">
            {{ formMode === 'them' ? 'Thêm mới' : 'Lưu thay đổi' }}
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL CHI TIẾT -->
    <div v-if="showDetail && detailRow" class="modal-backdrop" @click.self="showDetail = false">
      <div class="modal-card">
        <div class="modal-banner">
          <div class="modal-banner-icon">
            <i class="bi bi-calendar2-week"></i>
          </div>
          <div class="modal-banner-text">
            <h3>Chi tiết lịch làm việc</h3>
            <p>{{ detailRow.maNhanVien }} · {{ hienThiNgay(detailRow.ngay) }}</p>
          </div>
          <button class="btn-banner-close" type="button" @click="showDetail = false">×</button>
        </div>

        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">Mã nhân viên</span><strong>{{ detailRow.maNhanVien }}</strong></div>
            <div class="detail-item"><span class="detail-label">Nhân viên</span><strong>{{ tenCua(detailRow.maNhanVien) }}</strong></div>
            <div class="detail-item"><span class="detail-label">Ca làm</span><strong>{{ tenCaCua(detailRow.idCa) }}</strong></div>
            <div class="detail-item"><span class="detail-label">Thời gian</span><strong>{{ gioCua(detailRow.idCa) }}</strong></div>
            <div class="detail-item"><span class="detail-label">Ngày làm</span><strong>{{ hienThiNgay(detailRow.ngay) }}</strong></div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-danger-ghost" type="button" @click="xoaChiTiet">
            <i class="bi bi-trash3"></i> Xóa
          </button>
          <div class="footer-spacer"></div>
          <button class="btn-ghost" type="button" @click="showDetail = false">Đóng</button>
          <button class="btn-primary" type="button" @click="moFormSua(detailRow)">
            <i class="bi bi-pencil"></i> Chỉnh sửa
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL XEM THÊM (ca theo ngày) -->
    <div v-if="showXemThem" class="modal-backdrop" @click.self="showXemThem = false">
      <div class="modal-card">
        <div class="modal-banner">
          <div class="modal-banner-icon"><i class="bi bi-people"></i></div>
          <div class="modal-banner-text">
            <h3>{{ xemThemData.ca?.ten }} · {{ hienThiNgay(xemThemData.ngay) }}</h3>
            <p>{{ xemThemData.ca?.batDau }} - {{ xemThemData.ca?.ketThuc }} · {{ xemThemData.list.length }} nhân viên</p>
          </div>
          <button class="btn-banner-close" type="button" @click="showXemThem = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="xemThemData.list.length === 0" class="empty-inline">
            Chưa có nhân viên nào xếp ca này
          </div>
          <div v-for="l in xemThemData.list" :key="l.id" class="people-row">
            <span class="code-chip">{{ l.maNhanVien }}</span>
            <span class="people-name">{{ tenCua(l.maNhanVien) }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <div class="footer-spacer"></div>
          <button class="btn-ghost" type="button" @click="showXemThem = false">Đóng</button>
        </div>
      </div>
    </div>

    <!-- NHẬP EXCEL / CSV -->
    <div v-if="showImport" class="modal-backdrop" @click.self="dongImport">
      <div class="modal-card">
        <div class="modal-banner">
          <div class="modal-banner-icon"><i class="bi bi-file-earmark-spreadsheet"></i></div>
          <div class="modal-banner-text">
            <h3>Nhập lịch làm việc từ tệp</h3>
            <p>Hỗ trợ tệp Excel (.xlsx, .xls) hoặc CSV · tối đa theo cấu trúc mẫu</p>
          </div>
          <button class="btn-banner-close" type="button" @click="dongImport">×</button>
        </div>

        <div class="modal-body">
          <label class="import-drop">
            <input type="file" @change="docTrenFile" />
            <i class="bi bi-upload"></i>
            <span v-if="!importFileName">Chọn tệp Excel (.xlsx/.xls) hoặc CSV từ máy</span>
            <span v-else>{{ importFileName }}</span>
          </label>

          <p class="import-note">
            Cột yêu cầu: <strong>Mã nhân viên</strong>, <strong>Mã ca</strong>, <strong>Ngày làm (dd/MM/yyyy)</strong>.
            Có thể tải tệp mẫu (.xlsx), chỉnh sửa trực tiếp trong Excel rồi tải lên lại.
          </p>

          <div v-if="importLoading" class="empty-inline">Đang đọc tệp...</div>

          <div v-if="importPreview" class="import-preview">
            <div class="preview-stats">
              <span class="preview-pill is-total">Tổng dòng: <b>{{ importPreview.tongDong }}</b></span>
              <span class="preview-pill is-ok">Hợp lệ: <b>{{ importPreview.hopLe }}</b></span>
              <span class="preview-pill is-bad">Lỗi: <b>{{ importPreview.loi.length }}</b></span>
            </div>
            <ul v-if="importPreview.loi.length" class="preview-errors">
              <li v-for="(l, i) in importPreview.loi.slice(0, 8)" :key="i">Dòng {{ l.dong }}: {{ l.lyDo }}</li>
            </ul>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-ghost" type="button" @click="dongImport">Hủy</button>
          <div class="footer-spacer"></div>
          <button
            class="btn-primary"
            type="button"
            :disabled="!importPreview || importPreview.rows.length === 0"
            @click="xacNhanNhap"
          >
            Xác nhận nhập ({{ importPreview ? importPreview.rows.length : 0 }})
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.schedule-page-container {
  padding: 16px 24px 40px;
  background-color: #fafbfd;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  font-family: inherit;
}

/* 1. Shift Notice Banner */
.shift-alert-banner {
  background: #fefce8;
  border: 1px solid #fef08a;
  border-radius: 8px;
  padding: 10px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.shift-alert-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.badge-mode {
  background: #fef08a;
  color: #a16207;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #fde047;
  white-space: nowrap;
}

.shift-alert-text {
  font-size: 13px;
  color: #713f12;
  font-weight: 400;
  line-height: 1.4;
}

.btn-enter-shift {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-enter-shift:hover {
  background: linear-gradient(135deg, #b91c1c 0%, #991b1b 100%);
  box-shadow: 0 6px 16px rgba(220, 38, 38, 0.35);
  transform: translateY(-1px);
}

/* 2. Breadcrumbs */
.schedule-breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  padding: 2px 0;
}

.crumb-parent {
  color: #64748b;
}

.crumb-separator {
  color: #cbd5e1;
}

.crumb-current {
  color: #0f172a;
  font-weight: 700;
}

/* 3. Toolbar */
.schedule-toolbar {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.date-navigator-box {
  display: inline-flex;
  align-items: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  height: 38px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.date-navigator-box:hover {
  border-color: #cbd5e1;
}

.nav-arrow-btn {
  background: transparent;
  border: none;
  color: #475569;
  width: 36px;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  padding: 0;
}

.nav-arrow-btn:hover {
  background: #fef2f2;
  color: #dc2626;
}

.nav-arrow-btn:first-child {
  border-top-left-radius: 7px;
  border-bottom-left-radius: 7px;
}

.nav-arrow-btn:last-child {
  border-top-right-radius: 7px;
  border-bottom-right-radius: 7px;
}

.date-display-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 18px;
  min-width: 155px;
  height: 100%;
  border-left: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.15s ease;
  background: #ffffff;
}

.date-display-wrap:hover {
  background: #fffafa;
}

.date-display-wrap:hover .date-text,
.date-display-wrap:hover .date-icon {
  color: #dc2626;
}

.date-text {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.3px;
  white-space: nowrap;
  transition: color 0.15s ease;
}

.date-icon {
  width: 16px;
  height: 16px;
  color: #64748b;
  flex-shrink: 0;
  transition: color 0.15s ease;
}

/* Triệt tiêu hoàn toàn icon mặc định của Chrome datepicker đè lên chữ */
.hidden-date-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0 !important;
  cursor: pointer;
  z-index: 2;
  margin: 0;
  padding: 0;
  border: none;
}

.hidden-date-input::-webkit-calendar-picker-indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0 !important;
  cursor: pointer;
  background: transparent !important;
  color: transparent !important;
  -webkit-appearance: none;
}

.hidden-date-input::-webkit-inner-spin-button,
.hidden-date-input::-webkit-clear-button {
  display: none !important;
  -webkit-appearance: none;
}

.btn-today {
  height: 38px;
  padding: 0 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-today:hover {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
  transform: translateY(-1px);
}

.week-range-label {
  font-size: 13px;
  color: #475569;
  margin-left: 6px;
}

.week-range-label strong {
  color: #0f172a;
  font-weight: 700;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-switch-group {
  display: inline-flex;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}

.btn-view-toggle {
  background: transparent;
  border: none;
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-view-toggle:hover:not(.active) {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.6);
}

.btn-view-toggle.active {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(220, 38, 38, 0.25);
}

/* 4. Weekly Schedule Matrix Table */
.schedule-matrix-wrapper {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
}

.schedule-matrix-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.col-shift-header {
  width: 130px;
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  text-align: center;
  padding: 15px 10px;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

.col-day-header {
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  text-align: center;
  padding: 12px 8px;
  transition: background-color 0.15s ease;
}

.col-day-header:last-child {
  border-right: none;
}

.col-day-header.is-today {
  background: #fef2f2;
  border-bottom: 2px solid #dc2626;
}

.col-day-header.is-today .header-day-name {
  color: #dc2626;
}

.col-day-header.is-today .header-day-date {
  color: #b91c1c;
  font-weight: 700;
}

.header-day-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.header-day-date {
  font-size: 12px;
  color: #64748b;
  margin-top: 3px;
}

.matrix-row:last-child td {
  border-bottom: none;
}

.col-shift-info {
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  text-align: center;
  padding: 16px 10px;
  vertical-align: middle;
}

.shift-info-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
}

.shift-info-time {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.matrix-cell {
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 10px;
  vertical-align: top;
  min-height: 80px;
  height: 80px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  position: relative;
}

.matrix-cell:last-child {
  border-right: none;
}

.matrix-cell:hover {
  background-color: #fafbfc;
}

.matrix-cell.is-today {
  background-color: #fffcfc;
}

.matrix-cell.is-today:hover {
  background-color: #fef2f2;
}

.cell-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 64px;
}

.emp-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px 9px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.emp-card:hover {
  background: #fffafa;
  border-color: #fca5a5;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(220, 38, 38, 0.1);
}

.emp-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #1e293b;
  color: #ffffff;
  font-size: 11.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s ease;
}

.emp-card:hover .emp-avatar {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
}

.emp-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.emp-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.25;
  transition: color 0.15s ease;
}

.emp-card:hover .emp-name {
  color: #dc2626;
}

.emp-sub {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
}

.cell-empty-hint {
  display: none;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 60px;
  color: #cbd5e1;
  transition: color 0.15s ease;
}

.matrix-cell:hover .cell-empty-hint {
  display: flex;
  color: #dc2626;
}

.empty-matrix-cell {
  padding: 30px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

/* 5. Filter Card & Table Card */
.filter-card,
.table-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.card-title-row.space-between {
  justify-content: space-between;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-circle svg {
  width: 16px;
  height: 16px;
}

.icon-filter {
  background: #f1f5f9;
  color: #475569;
}

.icon-list {
  background: #fee2e2;
  color: #dc2626;
}

.card-title-text {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

/* Bộ lọc */
.filter-controls-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.field-block.grow {
  flex: 1;
  min-width: 240px;
  display: flex;
  flex-direction: column;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.search-icon {
  position: absolute;
  left: 14px;
  width: 16px;
  height: 16px;
  color: #94a3b8;
  pointer-events: none;
}

.input-search {
  width: 100%;
  height: 40px;
  padding: 0 16px 0 42px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
}

.input-search:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.input-search::placeholder {
  color: #94a3b8;
}

.input-date {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  font-family: inherit;
  width: 175px;
  transition: all 0.2s ease;
}

.input-date:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.filter-actions-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.filter-actions-row.is-left {
  justify-content: flex-start;
}

.actions-spacer {
  flex: 1;
}

.btn-action-outline {
  height: 38px;
  padding: 0 16px;
  border: 1px solid #fca5a5;
  border-radius: 8px;
  background: #ffffff;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

.btn-action-outline svg {
  width: 14px;
  height: 14px;
}

.btn-action-outline:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #dc2626;
  color: #b91c1c;
  transform: translateY(-1px);
}

.btn-action-outline.is-muted {
  border-color: #e2e8f0;
  color: #475569;
  background: #ffffff;
}

.btn-action-outline.is-muted:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.btn-action-outline:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  pointer-events: none;
}

.btn-action-primary {
  height: 38px;
  padding: 0 18px;
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  border: none;
  border-radius: 8px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

.btn-action-primary svg {
  width: 15px;
  height: 15px;
}

.btn-action-primary:hover {
  background: linear-gradient(135deg, #b91c1c 0%, #991b1b 100%);
  box-shadow: 0 6px 16px rgba(220, 38, 38, 0.35);
  transform: translateY(-1px);
}

.btn-action-dark {
  height: 38px;
  padding: 0 18px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

.btn-action-dark svg {
  width: 15px;
  height: 15px;
}

.btn-action-dark:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  color: #0f172a;
  transform: translateY(-1px);
}

/* Bảng */
.table-container {
  overflow-x: auto;
  width: 100%;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th {
  padding: 13px 16px;
  font-size: 11.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  white-space: nowrap;
}

.data-table td {
  padding: 13px 16px;
  font-size: 13px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background-color: #f8fafc;
}

.row-alt:nth-child(even) {
  background: #fafbfc;
}

.text-center {
  text-align: center;
}

.cell-stt {
  color: #64748b;
  font-weight: 600;
}

.cell-code {
  font-weight: 600;
  color: #0f172a;
}

.cell-name {
  font-weight: 600;
  color: #0f172a;
}

.empty-row {
  color: #94a3b8;
  padding: 28px 12px !important;
}

.code-chip {
  display: inline-flex;
  padding: 3px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.shift-chip {
  display: inline-flex;
  padding: 3px 11px;
  border-radius: 6px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
}

.action-buttons-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-action-circle {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 0;
  font-size: 14px;
  line-height: 1;
  color: #475569;
}

.btn-action-circle.is-view:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #2563eb;
  transform: translateY(-1px);
}

.btn-action-circle.is-delete {
  color: #64748b;
}

.btn-action-circle.is-delete:hover {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
  transform: translateY(-1px);
}

/* Modal */
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

.modal-card {
  background: #ffffff;
  width: 100%;
  max-width: 520px;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  animation: modalIn 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-card.modal-card-schedule {
  overflow: visible;
}

.modal-card.modal-card-schedule .modal-banner {
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
}

.modal-card.modal-card-schedule .modal-footer {
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 16px;
}

.modal-card.modal-card-schedule .modal-body {
  overflow: visible;
  max-height: none;
}

.form-field:has(.searchable-select.is-open) {
  position: relative;
  z-index: 60;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-banner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 22px;
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 60%, #fecdd3 100%);
  border-bottom: 1px solid #f1f5f9;
}

.modal-banner-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #ffffff;
  color: #dc2626;
  border: 1px solid #fca5a5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(220, 38, 38, 0.1);
}

.modal-banner-icon svg {
  width: 20px;
  height: 20px;
}

.modal-banner-icon i {
  font-size: 19px;
}

.modal-banner-text h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.modal-banner-text p {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.btn-banner-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-banner-close:hover {
  background: #ffffff;
  color: #0f172a;
}

.modal-body {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 60vh;
  overflow-y: auto;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.req {
  color: #dc2626;
}

.form-control {
  height: 40px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  font-family: inherit;
  transition: all 0.15s ease;
  width: 100%;
}

.form-control:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.form-error {
  margin: 0;
  font-size: 13px;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 9px 12px;
  display: flex;
  align-items: center;
  gap: 7px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.detail-item {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 11px 13px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-item strong {
  font-size: 13.5px;
  color: #0f172a;
}

.people-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 4px;
  border-bottom: 1px solid #f8fafc;
}

.people-row:last-child {
  border-bottom: none;
}

.people-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
}

.empty-inline {
  text-align: center;
  color: #94a3b8;
  font-size: 13.5px;
  padding: 12px;
}

.import-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  border: 1.5px dashed #fca5a5;
  background: #fffafa;
  border-radius: 12px;
  padding: 22px 14px;
  cursor: pointer;
  color: #dc2626;
  font-size: 13.5px;
  font-weight: 600;
  transition: all 0.15s ease;
}

.import-drop:hover {
  border-color: #dc2626;
  background: #fef2f2;
}

.import-drop input {
  display: none;
}

.import-drop i {
  font-size: 22px;
}

.import-note {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.55;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 10px 12px;
}

.import-preview {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-pill {
  font-size: 12.5px;
  padding: 4px 11px;
  border-radius: 20px;
  font-weight: 600;
  border: 1px solid transparent;
}

.preview-pill.is-total {
  background: #f1f5f9;
  color: #475569;
  border-color: #e2e8f0;
}

.preview-pill.is-ok {
  background: #ecfdf5;
  color: #059669;
  border-color: #d1fae5;
}

.preview-pill.is-bad {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fecaca;
}

.preview-errors {
  margin: 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: #b91c1c;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.modal-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 22px;
  border-top: 1px solid #f1f5f9;
  background: #fafbfc;
}

.footer-spacer {
  flex: 1;
}

.btn-ghost {
  height: 38px;
  padding: 0 18px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.btn-ghost:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.btn-primary {
  height: 38px;
  padding: 0 18px;
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  border: none;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #b91c1c 0%, #991b1b 100%);
  box-shadow: 0 6px 16px rgba(220, 38, 38, 0.35);
  transform: translateY(-1px);
}

.btn-danger-ghost {
  height: 38px;
  padding: 0 16px;
  border: 1px solid #fca5a5;
  background: #ffffff;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

.btn-danger-ghost:hover {
  background: #fef2f2;
  border-color: #dc2626;
  color: #b91c1c;
  transform: translateY(-1px);
}

@media (max-width: 1199px) {
  .schedule-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-left {
    justify-content: space-between;
  }
}

@media (max-width: 767px) {
  .schedule-page-container {
    padding: 14px 12px 32px;
  }

  .filter-card,
  .table-card {
    padding: 16px 14px;
  }

  .filter-controls-row {
    flex-direction: column;
    align-items: stretch;
  }

  .inline-field {
    justify-content: space-between;
  }

  .input-date {
    width: auto;
    flex: 1;
  }

  .actions-spacer {
    display: none;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>