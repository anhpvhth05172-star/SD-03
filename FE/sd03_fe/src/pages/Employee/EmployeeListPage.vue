<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getEmployee,
  getEmployeeFormData,
  importEmployeesConfirm,
  listEmployees,
  sendEmployeesBulkEmail,
  updateEmployeeStatus,
} from '@/api/employee'
import DoiMatKhauModal from '@/components/DoiMatKhauModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { exportExcel, fetchAllPages } from '@/utils/export'
import { takeFlashWithMeta } from '@/utils/flash'
import { avatarColor, formatDate, formatDateTime, initialsOf } from '@/utils/format'
import { getAvatarSrc, onAvatarError } from '@/utils/avatar'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import { API_BASE } from '@/api/http'

const router = useRouter()
const flashMsg = ref('')
const exporting = ref(false)

const isImportModalOpen = ref(false)
const isImporting = ref(false)
const isDownloadingTemplate = ref(false)
const selectedImportFile = ref(null)
const importResult = ref(null)
const fileInputExcelRef = ref(null)

const vaiTros = ref([])
const rows = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(5) // Khớp 5 dòng như trong ảnh mẫu

const fKeyword = ref('')
const fVaiTro = ref('')
const fTrangThai = ref('')

const vaiTroFilterOptions = computed(() => [
  { value: '', label: 'Tất cả vai trò' },
  ...vaiTros.value.map((v) => ({ value: v.id, label: v.ten })),
])

const trangThaiFilterOptions = [
  { value: '', label: 'Tất cả' },
  { value: 'active', label: 'Hoạt động' },
  { value: 'locked', label: 'Đã khóa' },
]

const detailData = ref(null)
const doiMkNhanVienId = ref(null)

let searchTimeout = null
const onKeywordInput = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    applyFilter()
  }, 350)
}

const buildParams = () => {
  const params = { page: page.value, size: size.value }
  if (fKeyword.value.trim()) params.keyword = fKeyword.value.trim()
  if (fVaiTro.value) params.idVaiTro = fVaiTro.value
  if (fTrangThai.value) params.trangThai = fTrangThai.value
  return params
}

const load = async () => {
  const params = buildParams()
  if (!params) return
  loading.value = true
  try {
    const data = await listEmployees(params)
    const maxPage = data.totalPages > 0 ? data.totalPages - 1 : 0
    if (page.value > maxPage) {
      page.value = maxPage
      loading.value = false
      return await load()
    }
    rows.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch (e) {
    notifyError('Lỗi tải dữ liệu', e.message)
  } finally {
    loading.value = false
  }
}

const applyFilter = () => {
  page.value = 0
  load()
}

const openImportModal = () => {
  selectedImportFile.value = null
  importResult.value = null
  isImportModalOpen.value = true
}

const triggerSelectFile = () => fileInputExcelRef.value?.click()

const chonFileExcel = (file) => {
  if (!file) return
  const name = file.name.toLowerCase()
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    notifyWarning('Vui lòng chọn tệp định dạng Excel (.xlsx hoặc .xls)')
    return
  }
  selectedImportFile.value = file
  importResult.value = null
}

const onFileExcelChange = (e) => {
  chonFileExcel(e.target.files && e.target.files[0])
  e.target.value = ''
}

const onDropExcelFile = (e) => {
  chonFileExcel(e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0])
}

const downloadTemplate = async () => {
  isDownloadingTemplate.value = true
  try {
    const res = await fetch(`${API_BASE}/nhan-vien/import/mau`)
    if (!res.ok) throw new Error('Không thể tải file mẫu')
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'Mau_Import_Nhan_Vien.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    notifySuccess('Đã tải xuống file Excel mẫu thành công!')
  } catch (err) {
    notifyError('Tải file mẫu thất bại', err.message)
  } finally {
    isDownloadingTemplate.value = false
  }
}

const submitImportExcel = async () => {
  if (!selectedImportFile.value) {
    notifyWarning('Vui lòng chọn tệp Excel trước khi import')
    return
  }
  isImporting.value = true
  importResult.value = null
  try {
    const ketQua = await importEmployeesConfirm(selectedImportFile.value)
    const thanhCong = (ketQua.themThanhCong || 0) + (ketQua.capNhatThanhCong || 0)
    const thatBai = ketQua.thatBai || 0
    importResult.value = {
      soDongThanhCong: thanhCong,
      soDongThatBai: thatBai,
      tongSoDong: thanhCong + thatBai,
      danhSachLoi: ketQua.chiTietLoi || [],
    }
    if (thanhCong > 0) {
      notifySuccess(`Import thành công ${thanhCong}/${thanhCong + thatBai} dòng dữ liệu!`)
      load()
    } else {
      notifyWarning('Không có dòng dữ liệu nào được import thành công')
    }
  } catch (err) {
    notifyError('Import thất bại', err.message)
  } finally {
    isImporting.value = false
  }
}

const resetFilter = () => {
  fKeyword.value = ''
  fVaiTro.value = ''
  fTrangThai.value = ''
  page.value = 0
  load()
}

const onSizeChange = (s) => {
  size.value = s
  page.value = 0
  load()
}

const goPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  load()
}

const formatEmployeeAddress = (row) => {
  const parts = [row.diaChiCuThe, row.phuong, row.queQuan].filter(Boolean)
  return parts.length ? parts.join(', ') : '—'
}

const gioiTinhLabel = (value) => {
  if (value === 'NAM' || value === 'Nam') return 'Nam'
  if (value === 'NU' || value === 'Nu' || value === 'Nữ') return 'Nữ'
  return value || '—'
}

const ngaySinhChu = (value) => {
  if (!value) return ''
  const s = String(value)
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (m) return `${m[3]}/${m[2]}/${m[1]}`
  return s
}

const gioiTinhNhap = (value) => {
  if (value === 'NAM' || value === 'Nam') return 'Nam'
  if (value === 'NU' || value === 'Nu' || value === 'Nữ') return 'Nữ'
  if (value === 'KHAC' || value === 'Khác') return 'Khác'
  return ''
}

// Tiêu đề cột phải KHỚP CHÍNH XÁC mẫu import của backend (NhanVienImportService.TIEU_DE)
// để file xuất → chỉnh sửa → import lại đọc đúng từng cột.
const TIEU_DE_IMPORT_NHAN_VIEN = [
  'Mã nhân viên (trống = tự sinh)',
  'Họ và tên *',
  'Email',
  'Số điện thoại',
  'Giới tính (Nam / Nữ / Khác)',
  'Ngày sinh (dd/MM/yyyy)',
  'Vai trò (ADMIN / STAFF)',
  'Tỉnh/Thành phố',
  'Xã/Phường/Thị trấn',
  'Địa chỉ cụ thể',
]

const exportFile = async () => {
  const params = buildParams()
  if (!params) return
  exporting.value = true
  try {
    const all = await fetchAllPages(listEmployees, params)
    await exportExcel(
      'danh-sach-nhan-vien.xlsx',
      TIEU_DE_IMPORT_NHAN_VIEN,
      all.map((r) => [
        r.maNhanVien || '',
        r.tenTaiKhoan || '',
        r.email || '',
        String(r.soDienThoai || ''),
        gioiTinhNhap(r.gioiTinh),
        ngaySinhChu(r.ngaySinh),
        r.maVaiTro || r.tenVaiTro || '',
        r.queQuan || '',
        r.phuong || '',
        r.diaChiCuThe || '',
      ]),
      { sheetName: 'Danh sách nhân viên' }
    )
    notifySuccess('Đã xuất danh sách nhân viên thành công!')
  } catch (e) {
    notifyError('Xuất file thất bại', e.message)
  } finally {
    exporting.value = false
  }
}

const pageList = computed(() => {
  const total = totalPages.value
  const cur = page.value + 1
  let start = Math.max(1, cur - 2)
  let end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  const arr = []
  for (let i = start; i <= end; i++) arr.push(i)
  return arr
})

// ===== Chọn nhân viên bằng checkbox =====
const selectedIds = ref([])

const isSelected = (id) => selectedIds.value.includes(id)

const toggleSelect = (id) => {
  selectedIds.value = isSelected(id)
    ? selectedIds.value.filter((x) => x !== id)
    : [...selectedIds.value, id]
}

const allPageSelected = computed(
  () => rows.value.length > 0 && rows.value.every((r) => selectedIds.value.includes(r.id)),
)

const toggleSelectAllPage = () => {
  const pageIds = rows.value.map((r) => r.id)
  if (allPageSelected.value) {
    selectedIds.value = selectedIds.value.filter((id) => !pageIds.includes(id))
  } else {
    selectedIds.value = [...new Set([...selectedIds.value, ...pageIds])]
  }
}

const selectedRowsKnown = computed(() => rows.value.filter((r) => selectedIds.value.includes(r.id)))

// ===== Gửi email hàng loạt =====
const showEmailModal = ref(false)
const emailForm = ref({ tieuDe: '', noiDung: '' })
const emailSending = ref(false)
const emailResult = ref(null)

const openEmailModal = () => {
  if (selectedIds.value.length === 0) {
    notifyWarning('Vui lòng chọn ít nhất một nhân viên')
    return
  }
  emailResult.value = null
  showEmailModal.value = true
}

const sendBulkEmail = async () => {
  const tieuDe = emailForm.value.tieuDe.trim()
  const noiDung = emailForm.value.noiDung.trim()
  if (!tieuDe) {
    notifyWarning('Vui lòng nhập tiêu đề email')
    return
  }
  if (!noiDung) {
    notifyWarning('Vui lòng nhập nội dung email')
    return
  }
  const soLuong = selectedIds.value.length
  const ok = await notifyConfirm(
    'Xác nhận gửi email',
    `Gửi email "${tieuDe}" tới ${soLuong} nhân viên đã chọn?`,
    'Gửi email',
    'Hủy bỏ',
  )
  if (!ok) return

  emailSending.value = true
  try {
    const res = await sendEmployeesBulkEmail({
      ids: [...selectedIds.value],
      tieuDe,
      noiDung,
    })
    emailResult.value = res
    if (res.guiThanhCong > 0) {
      notifySuccess(`Đã gửi thành công ${res.guiThanhCong}/${res.tongYeuCau} email`)
    } else {
      notifyWarning('Không có email nào được gửi thành công')
    }
  } catch (e) {
    notifyError('Gửi email thất bại', e.message)
  } finally {
    emailSending.value = false
  }
}

// ===== Import nhân viên từ Excel =====
// Chỉ chọn/kéo thả 1 tệp rồi Xác nhận nhập — không qua bước trung gian.
// Mỗi lần xác nhận gửi chính tệp đang chọn lên backend để đọc lại từ đầu,
// nên đổi tệp hoặc sửa tệp rồi chọn lại luôn lấy dữ liệu mới nhất.
const showImportModal = ref(false)
const importFile = ref(null)
const importLoading = ref(false)
const importDragging = ref(false)
const importFileInput = ref(null)

const IMPORT_ACCEPT = '.xlsx,.xls'

const formatFileSize = (bytes) => {
  if (!Number.isFinite(bytes)) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

const closeImportModal = () => {
  if (importLoading.value) return
  showImportModal.value = false
  importFile.value = null
  importResult.value = null
  if (importFileInput.value) importFileInput.value.value = ''
}

const chonFileImport = (f) => {
  if (!f) return
  if (!/\.(xlsx|xls)$/i.test(f.name)) {
    notifyWarning('Sai định dạng tệp', 'Vui lòng chọn tệp Excel (.xlsx hoặc .xls)')
    return
  }
  importFile.value = f
  importResult.value = null
}

const onImportFileChange = (e) => {
  const f = e.target.files && e.target.files.length ? e.target.files[0] : null
  // Xóa giá trị input ngay: sau khi người dùng sửa tệp rồi chọn lại CÙNG tên tệp,
  // trình duyệt vẫn phát sinh change và ta nhận được phiên bản mới nhất của tệp.
  e.target.value = ''
  chonFileImport(f)
}

const openImportPicker = () => {
  if (importLoading.value || !importFileInput.value) return
  importFileInput.value.click()
}

const onImportDragOver = (e) => {
  e.preventDefault()
  if (!importLoading.value) importDragging.value = true
}

const onImportDragLeave = () => {
  importDragging.value = false
}

const onImportDrop = (e) => {
  e.preventDefault()
  importDragging.value = false
  if (importLoading.value) return
  const f = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length
    ? e.dataTransfer.files[0]
    : null
  chonFileImport(f)
}

const clearImportFile = () => {
  if (importLoading.value) return
  importFile.value = null
  importResult.value = null
  if (importFileInput.value) importFileInput.value.value = ''
}

const confirmImport = async () => {
  if (importLoading.value) return
  if (!importFile.value) {
    notifyWarning('Chưa chọn tệp', 'Vui lòng chọn tệp Excel trước khi xác nhận nhập')
    return
  }

  importLoading.value = true
  try {
    // Gửi đúng tệp đang chọn — backend đọc lại toàn bộ nội dung hiện tại của tệp
    const ketQua = await importEmployeesConfirm(importFile.value)
    importResult.value = ketQua

    const them = ketQua.themThanhCong || 0
    const capNhat = ketQua.capNhatThanhCong || 0
    const loi = ketQua.thatBai || 0

    // Làm mới danh sách theo đúng bộ lọc / phân trang đang áp dụng
    await load()

    if (them + capNhat === 0 && loi > 0) {
      notifyError('Nhập thất bại', `Không có dòng nào được lưu. ${loi} dòng lỗi — xem chi tiết trong hộp thoại.`)
      return // giữ modal + tệp để người dùng sửa tệp và thử lại
    }
    if (loi > 0) {
      notifyWarning('Nhập hoàn tất (có dòng lỗi)', `${them} thêm mới, ${capNhat} cập nhật, ${loi} dòng lỗi không được lưu`)
      return // giữ modal hiển thị chi tiết lỗi, tệp vẫn còn để thử lại
    }
    notifySuccess('Nhập nhân viên thành công', `${them} thêm mới, ${capNhat} cập nhật`)
    showImportModal.value = false
    importFile.value = null
    importResult.value = null
    if (importFileInput.value) importFileInput.value.value = ''
  } catch (e) {
    // Lỗi API / đọc tệp: giữ nguyên modal và tệp đã chọn để thử lại
    notifyError('Nhập thất bại', e.message)
  } finally {
    importLoading.value = false
  }
}

const openDetail = async (row) => {
  try {
    detailData.value = await getEmployee(row.id)
  } catch (e) {
    notifyError('Lỗi lấy chi tiết', e.message)
  }
}

const isAccountProtected = (row) => {
  if (!row) return false
  const maVt = String(row.maVaiTro || '').toUpperCase()
  const tenVt = String(row.tenVaiTro || '').toLowerCase()
  const maNv = String(row.maNhanVien || '').toUpperCase()
  const tenTk = String(row.tenTaiKhoan || '').toLowerCase()
  return (
    maVt === 'ADMIN' ||
    tenVt.includes('quản trị') ||
    tenVt.includes('admin') ||
    maNv.startsWith('ADMIN') ||
    tenTk.startsWith('admin') ||
    row.idVaiTro === 1
  )
}

const toggleStatus = async (row) => {
  if (isAccountProtected(row)) {
    notifyWarning('Tài khoản Quản trị viên không thể thay đổi trạng thái')
    return
  }
  const actionText = row.trangThai ? 'khóa' : 'kích hoạt'
  const ok = await notifyConfirm(
    `Xác nhận ${actionText} tài khoản`,
    `Bạn có chắc chắn muốn ${actionText} tài khoản nhân viên "${row.tenTaiKhoan}"?`,
    'Xác nhận',
    'Hủy bỏ'
  )
  if (!ok) return

  const moi = !row.trangThai
  try {
    await updateEmployeeStatus(row.id, moi ? 'true' : 'false')
    row.trangThai = moi
    row.trangThaiLabel = moi ? 'Đang hoạt động' : 'Đã khóa'
    notifySuccess(`Đã ${actionText} tài khoản "${row.tenTaiKhoan}" thành công!`)
    await load()
  } catch (e) {
    notifyError('Thao tác thất bại', e.message)
  }
}

onMounted(async () => {
  const { message, type } = takeFlashWithMeta()
  flashMsg.value = message
  if (message) {
    if (type === 'warning') {
      notifyWarning(message)
    } else if (type === 'error') {
      notifyError(message)
    } else {
      notifySuccess(message)
    }
  }
  try {
    const formData = await getEmployeeFormData()
    vaiTros.value = formData.vaiTros || []
  } catch {
    vaiTros.value = []
  }
  load()
})
</script>

<template>
  <div class="employee-page-container">
    <!-- Card 1: BỘ LỌC -->
    <div class="filter-card">
      <div class="card-title-row">
        <div class="icon-circle icon-filter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
          </svg>
        </div>
        <span class="card-title-text">Bộ lọc</span>
      </div>

      <!-- Hàng 1: Các ô lọc tìm kiếm -->
      <div class="filter-controls-row">
        <!-- Input tìm kiếm -->
        <div class="search-input-wrap">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            v-model="fKeyword"
            type="text"
            class="input-search"
            placeholder="Tìm theo mã, họ tên, tài khoản, SĐT..."
            @input="onKeywordInput"
            @keyup.enter="applyFilter"
          />
        </div>

        <!-- Dropdown Vai trò -->
        <div class="select-wrap">
          <SearchableSelect
            v-model="fVaiTro"
            :options="vaiTroFilterOptions"
            value-key="value"
            label-key="label"
            placeholder="Tất cả vai trò"
            search-placeholder="Tìm vai trò..."
            @change="(v) => { fVaiTro = v ?? ''; applyFilter() }" />
        </div>

        <!-- Dropdown Trạng thái -->
        <div class="select-wrap">
          <SearchableSelect
            v-model="fTrangThai"
            :options="trangThaiFilterOptions"
            value-key="value"
            label-key="label"
            placeholder="Tất cả"
            search-placeholder="Tìm trạng thái..."
            @change="(v) => { fTrangThai = v ?? ''; applyFilter() }" />
        </div>
      </div>

      <!-- Hàng 2: Các nút hành động bên phải -->
      <div class="filter-actions-row">
        <!-- Nút Đặt lại bộ lọc -->
        <button class="btn-action-outline" type="button" @click="resetFilter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
          </svg>
          Đặt lại bộ lọc
        </button>

        <!-- Nút Xuất Excel -->
        <button class="btn-action-outline" type="button" :disabled="exporting" @click="exportFile">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
        </button>

        <!-- Nút Nhập Excel -->
        <button class="btn-action-outline" type="button" @click="openImportModal" title="Nhập danh sách nhân viên từ file Excel">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="17 8 12 3 7 8"></polyline>
            <line x1="12" y1="3" x2="12" y2="15"></line>
          </svg>
          Nhập Excel
        </button>

        <!-- Nút Gửi email hàng loạt -->
        <button
          class="btn-action-outline"
          type="button"
          :disabled="selectedIds.length === 0"
          @click="openEmailModal"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
            <polyline points="22,6 12,13 2,6"></polyline>
          </svg>
          Gửi email{{ selectedIds.length ? ` (${selectedIds.length})` : '' }}
        </button>

        <!-- Nút Thêm nhân viên -->
        <button class="btn-action-primary" type="button" @click="router.push('/nhan-vien/them')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Thêm nhân viên
        </button>
      </div>
    </div>

    <!-- Card 2: DANH SÁCH NHÂN VIÊN -->
    <div class="table-card">
      <div class="card-title-row">
        <div class="card-title-left">
          <div class="icon-circle icon-users">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
          </div>
          <span class="card-title-text">Danh sách nhân viên</span>
          <span class="items-count-pill">{{ rows.length }}/{{ totalElements }} nhân viên</span>
        </div>
      </div>

      <div class="table-container">
        <table class="employee-table">
          <thead>
            <tr>
              <th style="width: 40px; text-align: center;" title="Chọn tất cả nhân viên trong trang">
                <input
                  type="checkbox"
                  class="row-check"
                  :checked="allPageSelected"
                  :disabled="loading || rows.length === 0"
                  @change="toggleSelectAllPage"
                />
              </th>
              <th style="width: 45px;">STT</th>
              <th style="width: 60px; text-align: center;">Ảnh</th>
              <th style="width: 105px;">Mã NV</th>
              <th style="width: 160px;">Họ tên</th>
              <th style="width: 175px;">Email</th>
              <th style="width: 80px;">Giới tính</th>
              <th style="width: 115px;">SĐT</th>
              <th>Địa chỉ</th>
              <th style="width: 120px;">Vai trò</th>
              <th style="width: 110px; text-align: center;">Trạng thái</th>
              <th style="width: 150px; text-align: center;">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="12" class="text-center py-5 text-muted">Đang tải dữ liệu...</td>
            </tr>
            <tr v-else-if="rows.length === 0">
              <td colspan="12" class="text-center py-5 text-muted">Không tìm thấy nhân viên nào</td>
            </tr>
            <template v-else>
              <tr v-for="(row, index) in rows" :key="row.id">
                <td class="text-center">
                  <input
                    type="checkbox"
                    class="row-check"
                    :checked="isSelected(row.id)"
                    @change="toggleSelect(row.id)"
                  />
                </td>
                <td class="cell-stt">{{ (page * size) + index + 1 }}</td>
                <td class="text-center">
                  <div class="employee-avatar has-img">
                    <img
                      :src="getAvatarSrc(row)"
                      :alt="row.tenTaiKhoan"
                      @error="onAvatarError($event, row)"
                    />
                  </div>
                </td>
                <td class="cell-code">{{ row.maNhanVien }}</td>
                <td class="cell-name">{{ row.tenTaiKhoan }}</td>
                <td class="cell-email" :title="row.email || '—'">{{ row.email || '—' }}</td>
                <td class="cell-gender">{{ gioiTinhLabel(row.gioiTinh) }}</td>
                <td class="cell-phone">{{ row.soDienThoai || '—' }}</td>
                <td class="cell-address">
                  <span class="address-clamp" :title="formatEmployeeAddress(row)">
                    {{ formatEmployeeAddress(row) }}
                  </span>
                </td>
                <td class="cell-role">{{ row.tenVaiTro || '—' }}</td>
                <td class="text-center">
                  <span class="status-pill" :class="row.trangThai ? 'is-active' : 'is-locked'">
                    {{ row.trangThai ? 'Hoạt động' : 'Đã khóa' }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="action-buttons-group">
                    <!-- Nút 1: On/Off (Switch) -->
                    <label
                      class="switch"
                      :title="isAccountProtected(row) ? 'Tài khoản Quản trị viên (không thể đổi trạng thái)' : (row.trangThai ? 'Khóa tài khoản' : 'Kích hoạt tài khoản')"
                    >
                      <input
                        type="checkbox"
                        :checked="row.trangThai"
                        :disabled="isAccountProtected(row)"
                        @click.prevent="isAccountProtected(row) ? null : toggleStatus(row)"
                      />
                      <span class="slider"></span>
                    </label>

                    <!-- Nút 2: Mắt (Xem chi tiết) -->
                    <button
                      class="btn-action-circle is-view"
                      type="button"
                      title="Xem chi tiết"
                      @click="openDetail(row)"
                    >
                      <i class="bi bi-eye"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Phân trang Footer -->
      <div v-if="totalElements > 0" class="table-card-foot">
        <PaginationBar
          :page="page"
          :total-pages="totalPages"
          :total-elements="totalElements"
          :page-size="size"
          :from="rows.length ? page * size + 1 : 0"
          :to="page * size + rows.length"
          :sizes="[5, 10, 20, 50]"
          label="nhân viên"
          @change="goPage"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <!-- Modal Chi tiết Nhân viên (Profile Card Hiện đại, Đẹp mắt) -->
    <div v-if="detailData" class="modal-backdrop" @click.self="detailData = null">
      <div class="profile-modal-card">
        <!-- Banner đầu trang -->
        <div class="profile-modal-banner employee-theme">
          <button class="btn-banner-close" type="button" title="Đóng" @click="detailData = null">
            <i class="bi bi-x-lg"></i>
          </button>

          <div class="profile-banner-content">
            <div class="profile-avatar-wrapper">
              <img
                :src="getAvatarSrc(detailData)"
                :alt="detailData.tenTaiKhoan"
                class="profile-avatar-img"
                @error="onAvatarError($event, detailData)"
              />
            </div>

            <div class="profile-meta-info">
              <div class="profile-header-badges">
                <span class="badge-code">{{ detailData.maNhanVien }}</span>
                <span class="badge-role">{{ detailData.tenVaiTro || 'Nhân viên' }}</span>
                <span class="status-pill" :class="detailData.trangThai ? 'is-active' : 'is-locked'">
                  {{ detailData.trangThai ? 'Hoạt động' : 'Đã khóa' }}
                </span>
              </div>
              <h3 class="profile-name">{{ detailData.tenTaiKhoan }}</h3>
              <p class="profile-email-sub">{{ detailData.email || 'Chưa cập nhật email' }}</p>
            </div>
          </div>
        </div>

        <!-- Thân Modal: Các nhóm thông tin -->
        <div class="profile-modal-body">
          <!-- Nhóm 1: Thông tin cá nhân & liên hệ -->
          <div class="info-section-group">
            <h4 class="section-group-title">Thông tin liên hệ & Cá nhân</h4>
            <div class="info-grid-2">
              <div class="info-cell">
                <span class="info-cell-label">Số điện thoại</span>
                <span class="info-cell-value">{{ detailData.soDienThoai || 'Chưa cập nhật' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label">Giới tính</span>
                <span class="info-cell-value">{{ gioiTinhLabel(detailData.gioiTinh) }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label">Ngày sinh</span>
                <span class="info-cell-value">{{ formatDate(detailData.ngaySinh) || '—' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label">Ngày vào làm</span>
                <span class="info-cell-value">{{ formatDate(detailData.ngayTao) || '—' }}</span>
              </div>
            </div>
          </div>

          <!-- Nhóm 2: Địa chỉ nơi ở -->
          <div class="info-section-group">
            <h4 class="section-group-title">Địa chỉ nơi ở</h4>
            <div class="address-box-card">
              <p class="address-text">{{ formatEmployeeAddress(detailData) || 'Chưa cập nhật địa chỉ' }}</p>
            </div>
          </div>

          <!-- Metadata thời gian -->
          <div class="modal-time-footer">
            <span>Cập nhật gần nhất: {{ formatDateTime(detailData.ngayCapNhat) || '—' }}</span>
            <span v-if="detailData.nguoiCapNhat">Bởi: <b>{{ detailData.nguoiCapNhat }}</b></span>
          </div>
        </div>

        <!-- Cụm nút hành động -->
        <div class="profile-modal-actions">
          <button class="btn-profile-edit" type="button" @click="router.push(`/nhan-vien/${detailData.id}/sua`)">
            <i class="bi bi-pencil-square"></i>
            <span>Chỉnh sửa thông tin</span>
          </button>
          <button
            v-if="!isAccountProtected(detailData)"
            class="btn-profile-secondary"
            type="button"
            @click="doiMkNhanVienId = detailData.id"
          >
            <i class="bi bi-key"></i>
            <span>Đổi mật khẩu</span>
          </button>
          <button class="btn-profile-close" type="button" @click="detailData = null">
            Đóng
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Import Excel -->
    <div v-if="isImportModalOpen" class="emp-import-backdrop" @click.self="isImportModalOpen = false">
      <div class="emp-import-modal">
        <div class="emp-import-header">
          <div class="emp-import-title-group">
            <span class="emp-import-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="12" y1="18" x2="12" y2="12"></line>
                <polyline points="9 15 12 12 15 15"></polyline>
              </svg>
            </span>
            <div>
              <h3 class="emp-import-title">Nhập nhân viên từ File Excel</h3>
              <p class="emp-import-sub">Tải file mẫu, điền dữ liệu rồi nhập danh sách vào hệ thống</p>
            </div>
          </div>
          <button class="emp-import-close" @click="isImportModalOpen = false" title="Đóng">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>

        <div class="emp-import-body">
          <div class="emp-template-card">
            <div>
              <h4 class="emp-template-heading">Chưa có file đúng chuẩn?</h4>
              <p class="emp-template-desc">Tải file mẫu có sẵn cấu trúc cột, điền dữ liệu là nhập được ngay.</p>
            </div>
            <button class="emp-btn-download" @click="downloadTemplate" :disabled="isDownloadingTemplate">
              {{ isDownloadingTemplate ? 'Đang tải...' : 'Tải file mẫu (.xlsx)' }}
            </button>
          </div>

          <input
            ref="fileInputExcelRef"
            type="file"
            accept=".xlsx,.xls"
            style="display: none;"
            @change="onFileExcelChange"
          />

          <div
            class="emp-dropzone"
            :class="{ 'has-file': !!selectedImportFile }"
            @dragover.prevent
            @dragenter.prevent
            @drop.prevent="onDropExcelFile"
            @click="triggerSelectFile"
          >
            <template v-if="!selectedImportFile">
              <div class="emp-drop-icon">
                <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242"></path>
                  <path d="M12 12v9"></path>
                  <path d="m16 16-4-4-4 4"></path>
                </svg>
              </div>
              <p class="emp-drop-title">Kéo &amp; thả file Excel vào đây, hoặc <span class="emp-highlight">bấm để chọn</span></p>
              <p class="emp-drop-hint">Hỗ trợ tệp .xlsx hoặc .xls</p>
            </template>
            <div v-else class="emp-drop-file" @click.stop>
              <div class="emp-file-meta">
                <span class="emp-file-name">{{ selectedImportFile.name }}</span>
                <span class="emp-file-size">{{ (selectedImportFile.size / 1024).toFixed(1) }} KB</span>
              </div>
              <button class="emp-file-remove" type="button" @click="selectedImportFile = null; importResult = null" title="Hủy chọn file">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>
          </div>

          <div class="emp-import-notice">
            <div class="emp-notice-title">Lưu ý quan trọng:</div>
            <ul class="emp-notice-list">
              <li>Ngày sinh dạng <strong>dd/MM/yyyy</strong>, nhân viên phải đủ 18 tuổi.</li>
              <li>Vai trò chỉ nhận <strong>ADMIN</strong> hoặc <strong>STAFF</strong>.</li>
              <li>Email, số điện thoại trùng (trong file hoặc trong hệ thống) sẽ báo lỗi từng dòng; các dòng khác vẫn import.</li>
              <li>Mã nhân viên tự动生成, mật khẩu mặc định <strong>123456</strong>; import không gửi mail cấp tài khoản.</li>
            </ul>
          </div>

          <div v-if="importResult" class="emp-import-result" :class="{ 'has-errors': importResult.soDongThatBai > 0 }">
            <h4 class="emp-result-heading">
              <span v-if="importResult.soDongThanhCong > 0">Hoàn tất xử lý:</span>
              <span v-else>Chưa có dữ liệu hợp lệ:</span>
              Đã nhập thành công {{ importResult.soDongThanhCong }} / {{ importResult.tongSoDong }} dòng
            </h4>
            <div class="emp-result-pills">
              <span class="emp-pill success">Thành công: <strong>{{ importResult.soDongThanhCong }}</strong></span>
              <span class="emp-pill danger">Lỗi: <strong>{{ importResult.soDongThatBai }}</strong></span>
            </div>
            <div v-if="importResult.danhSachLoi && importResult.danhSachLoi.length" class="emp-result-errors">
              <div class="emp-errors-title">Chi tiết dòng cần kiểm tra:</div>
              <ul class="emp-errors-list">
                <li v-for="(err, idx) in importResult.danhSachLoi" :key="idx">{{ err }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="emp-import-footer">
          <button class="emp-btn-cancel" @click="isImportModalOpen = false" :disabled="isImporting">
            {{ importResult ? 'Đóng' : 'Hủy bỏ' }}
          </button>
          <button class="emp-btn-submit" :disabled="!selectedImportFile || isImporting" @click="submitImportExcel">
            {{ isImporting ? 'Đang xử lý dữ liệu...' : 'Bắt đầu nhập dữ liệu' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: GỬI EMAIL HÀNG LOẠT -->
    <div v-if="showEmailModal" class="action-modal-overlay" @click.self="showEmailModal = false">
      <div class="action-modal modal-email">
        <div class="action-modal-head">
          <h3><i class="bi bi-envelope-paper"></i> Gửi email hàng loạt</h3>
          <button class="action-modal-x" type="button" @click="showEmailModal = false">×</button>
        </div>

        <div class="action-modal-body">
          <p class="action-modal-note">
            Email sẽ được gửi tới <b>{{ selectedIds.length }}</b> nhân viên đã chọn.
            Nhân viên không có email hợp lệ sẽ bị bỏ qua và báo cáo chi tiết.
          </p>

          <div v-if="selectedRowsKnown.length" class="email-chips">
            <span v-for="r in selectedRowsKnown" :key="r.id" class="email-chip">
              {{ r.tenTaiKhoan }} <i>{{ r.email || 'không có email' }}</i>
            </span>
            <span v-if="selectedIds.length > selectedRowsKnown.length" class="email-chip is-more">
              +{{ selectedIds.length - selectedRowsKnown.length }} nhân viên khác
            </span>
          </div>

          <div class="action-field">
            <label>Tiêu đề <b class="req">*</b></label>
            <input
              v-model="emailForm.tieuDe"
              type="text"
              maxlength="200"
              placeholder="Thông báo từ PolyShoes"
            />
          </div>

          <div class="action-field">
            <label>Nội dung <b class="req">*</b></label>
            <textarea
              v-model="emailForm.noiDung"
              rows="6"
              maxlength="5000"
              placeholder="Nhập nội dung gửi tới nhân viên..."
            ></textarea>
          </div>

          <div v-if="emailResult" class="import-result">
            <p>
              Đã gửi thành công <b>{{ emailResult.guiThanhCong }}</b>/<b>{{ emailResult.tongYeuCau }}</b>
              email<template v-if="emailResult.thatBai > 0">, {{ emailResult.thatBai }} bị bỏ qua/thất bại</template>.
            </p>
            <ul v-if="emailResult.boQua && emailResult.boQua.length">
              <li v-for="(b, i) in emailResult.boQua" :key="i">
                {{ b.tenTaiKhoan || b.maNhanVien || ('#' + b.id) }}: {{ b.lyDo }}
              </li>
            </ul>
          </div>
        </div>

        <div class="action-modal-foot">
          <button class="btn-action-outline" type="button" @click="showEmailModal = false">
            {{ emailResult ? 'Đóng' : 'Hủy' }}
          </button>
          <button
            v-if="!emailResult"
            class="btn-action-primary"
            type="button"
            :disabled="emailSending"
            @click="sendBulkEmail"
          >
            {{ emailSending ? 'Đang gửi...' : `Gửi ${selectedIds.length} email` }}
          </button>
        </div>
      </div>
    </div>
    <DoiMatKhauModal
      v-if="doiMkNhanVienId"
      loai="NV"
      :id="doiMkNhanVienId"
      :ten-tai-khoan="detailData?.tenTaiKhoan || detailData?.maNhanVien || ''"
      @close="doiMkNhanVienId = null"
      @success="detailData = null"
    />
  </div>
</template>

<style scoped>
.employee-page-container {
  padding: 16px 20px 40px;
  background-color: #f8fafc;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: inherit;
}

/* Card chung */
.filter-card,
.table-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
}

/* Tiêu đề Card */
.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.card-title-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.items-count-pill {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
}

.icon-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-circle svg {
  width: 16px;
  height: 16px;
}

.icon-filter {
  background: #f1f5f9;
  color: #475569;
}

.icon-users {
  background: #ffe4e6;
  color: #f43f5e;
}

.card-title-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

/* Hàng Điều Khiển Bộ Lọc */
.filter-controls-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

/* Search input */
.search-input-wrap {
  flex: 1;
  min-width: 280px;
  position: relative;
  display: flex;
  align-items: center;
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
  height: 42px;
  padding: 0 16px 0 42px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
}

.input-search:focus {
  border-color: #f43f5e;
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.input-search::placeholder {
  color: #94a3b8;
}

/* Dropdown bộ lọc */
.select-wrap {
  flex-shrink: 0;
  min-width: 140px;
}

/* Hàng nút hành động bộ lọc bên phải */
.filter-actions-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

/* Nút outline (Đặt lại, Xuất Excel) */
.btn-action-outline {
  height: 38px;
  padding: 0 16px;
  border: 1px solid #ffe4e6;
  border-radius: 8px;
  background: #ffffff;
  color: #f43f5e;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-outline svg {
  width: 14px;
  height: 14px;
}

.btn-action-outline:hover {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #e11d48;
}

/* Nút chính (Thêm nhân viên) */
.btn-action-primary {
  height: 38px;
  padding: 0 18px;
  background: linear-gradient(135deg, #ff3366, #e11d48);
  border: none;
  border-radius: 8px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.25);
  transition: all 0.2s ease;
}

.btn-action-primary svg {
  width: 15px;
  height: 15px;
}

.btn-action-primary:hover {
  background: linear-gradient(135deg, #f43f5e, #be123c);
  box-shadow: 0 6px 16px rgba(225, 29, 72, 0.35);
}

/* Bảng dữ liệu */
.table-container {
  overflow-x: auto;
  width: 100%;
}

.employee-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.employee-table th {
  padding: 13px 12px;
  font-size: 11.5px;
  font-weight: 700;
  text-transform: uppercase;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  white-space: nowrap;
}

.employee-table td {
  padding: 13px 12px;
  font-size: 13px;
  color: #334155;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.employee-table tbody tr:hover {
  background-color: #fafbfc;
}

.cell-stt {
  color: #475569;
  font-weight: 500;
}

.cell-code {
  font-weight: 600;
  color: #0f172a;
}

.cell-name {
  font-weight: 600;
  color: #0f172a;
}

.cell-email {
  color: #475569;
  max-width: 175px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-gender {
  color: #334155;
}

.cell-phone {
  color: #334155;
  font-weight: 500;
}

.cell-role {
  color: #334155;
}

.cell-address {
  color: #475569;
  font-size: 13px;
  line-height: 1.45;
}

.address-clamp {
  display: block;
  max-width: 280px;
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
}

/* Avatar tròn */
.employee-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  text-transform: uppercase;
  margin: 0 auto;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  background: #f1f5f9;
}

.employee-avatar.has-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}

/* Status Pill */
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.status-pill.is-active {
  background-color: #ecfdf5;
  color: #10b981;
  border: 1px solid #d1fae5;
}

.status-pill.is-locked {
  background-color: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

/* Nhóm nút hành động */
.action-buttons-group {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

/* Nút hành động tròn */
.action-buttons-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-action-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #f1f5f9;
  background: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  font-size: 15px;
  line-height: 1;
}

.btn-action-circle i {
  font-size: 15px;
  line-height: 1;
  display: inline-block;
}

/* Nút On/Off khi là Quản trị viên: DISABLE mờ xám */
.btn-action-circle.is-disabled {
  background: #f8fafc !important;
  color: #cbd5e1 !important;
  border-color: #f1f5f9 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
  opacity: 0.45 !important;
  box-shadow: none !important;
}

/* Nút Mắt (Xem chi tiết): Xám đậm */
.btn-action-circle.is-view {
  background: #f8fafc;
  color: #475569;
  border-color: #e2e8f0;
}

.btn-action-circle.is-view:hover {
  background: #e2e8f0;
  color: #0f172a;
  transform: scale(1.08);
}

/* Phân trang Footer (chuẩn hóa giống trang Quản lý sản phẩm) */
.table-card-foot {
  padding: 14px 4px 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
  margin-top: 16px;
}

/* ==========================================================================
   PROFILE MODAL THIẾT KẾ HIỆN ĐẠI & SANG TRỌNG
   ========================================================================== */
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

.profile-modal-card {
  background: #ffffff;
  width: 100%;
  max-width: 560px;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  overflow: hidden;
  animation: modalCardIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalCardIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(14px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* Banner Header */
.profile-modal-banner {
  position: relative;
  padding: 24px 24px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.profile-modal-banner.employee-theme {
  background: #ffffff;
}

.btn-banner-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.btn-banner-close:hover {
  background: #ffffff;
  color: #0f172a;
  transform: rotate(90deg);
}

.profile-banner-content {
  display: flex;
  align-items: center;
  gap: 18px;
}

.profile-avatar-wrapper {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  border: 3px solid #ffffff;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-initials {
  width: 100%;
  height: 100%;
  background: #cc0000;
  color: #ffffff;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.profile-meta-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.profile-header-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.badge-code {
  font-size: 11.5px;
  font-weight: 700;
  color: #cc0000;
  background: #fef2f2;
  padding: 2px 8px;
  border-radius: 6px;
}

.badge-role {
  font-size: 11.5px;
  font-weight: 600;
  color: #475569;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
}

.profile-name {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.profile-email-sub {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Modal Body */
.profile-modal-body {
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.info-section-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-group-title {
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  letter-spacing: 0;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.info-cell {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-cell-label {
  font-size: 12.5px;
  font-weight: 500;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 5px;
}

.info-cell-value {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.address-box-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 14px;
}

.address-text {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.5;
}

.modal-time-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: #94a3b8;
  padding-top: 6px;
  border-top: 1px dashed #f1f5f9;
}

.modal-time-footer i {
  margin-right: 4px;
}

/* Modal Actions */
.profile-modal-actions {
  padding: 14px 24px 20px;
  background: #ffffff;
  border-top: 1px solid #f8fafc;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.btn-profile-edit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 8px;
  background: var(--red);
  color: #ffffff;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-profile-edit:hover {
  background: #a30000;
}

.btn-profile-secondary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 8px;
  background: #ffffff;
  color: #334155;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-profile-secondary:hover {
  background: #f8fafc;
}

.btn-profile-close {
  padding: 9px 20px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-profile-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* ===== Modal Import Excel ===== */
.emp-import-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

.emp-import-modal {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 640px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
}

.emp-import-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 22px;
  border-bottom: 1px solid #e2e8f0;
}

.emp-import-title-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.emp-import-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #eef2ff;
  color: #4f46e5;
  flex-shrink: 0;
}

.emp-import-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.emp-import-sub {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.emp-import-close {
  border: none;
  background: #f1f5f9;
  border-radius: 8px;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  cursor: pointer;
  transition: background 0.15s ease;
}

.emp-import-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.emp-import-body {
  padding: 20px 22px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.emp-template-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
}

.emp-template-heading {
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
}

.emp-template-desc {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.emp-btn-download {
  border: 1px solid #4f46e5;
  background: #fff;
  color: #4f46e5;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s ease;
}

.emp-btn-download:hover:not(:disabled) {
  background: #eef2ff;
}

.emp-btn-download:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.emp-dropzone {
  border: 1.5px dashed #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  padding: 26px 18px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.emp-dropzone:hover,
.emp-dropzone.has-file {
  border-color: #4f46e5;
  background: #eef2ff;
}

.emp-drop-icon {
  color: #64748b;
  margin-bottom: 8px;
}

.emp-drop-title {
  margin: 0;
  font-size: 13.5px;
  color: #334155;
}

.emp-highlight {
  color: #4f46e5;
  font-weight: 600;
}

.emp-drop-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.emp-drop-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border: 1px solid #c7d2fe;
  border-radius: 10px;
  padding: 10px 14px;
  text-align: left;
}

.emp-file-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.emp-file-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.emp-file-size {
  font-size: 11.5px;
  color: #94a3b8;
}

.emp-file-remove {
  border: none;
  background: #fee2e2;
  color: #b91c1c;
  border-radius: 8px;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.emp-import-notice {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
  padding: 12px 16px;
}

.emp-notice-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #92400e;
  margin-bottom: 6px;
}

.emp-notice-list {
  margin: 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: #78350f;
  line-height: 1.7;
}

.emp-import-result {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  padding: 14px 16px;
}

.emp-import-result.has-errors {
  background: #fef2f2;
  border-color: #fecaca;
}

.emp-result-heading {
  margin: 0 0 10px;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
}

.emp-result-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.emp-pill {
  font-size: 12px;
  border-radius: 999px;
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #334155;
}

.emp-pill.success {
  border-color: #86efac;
  color: #15803d;
}

.emp-pill.danger {
  border-color: #fca5a5;
  color: #b91c1c;
}

.emp-result-errors {
  margin-top: 12px;
}

.emp-errors-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #b91c1c;
  margin-bottom: 6px;
}

.emp-errors-list {
  margin: 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: #7f1d1d;
  line-height: 1.7;
  max-height: 140px;
  overflow-y: auto;
}

.emp-import-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 22px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.emp-btn-cancel {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  padding: 9px 16px;
  cursor: pointer;
}

.emp-btn-cancel:hover:not(:disabled) {
  background: #f1f5f9;
}

.emp-btn-submit {
  border: none;
  background: #4f46e5;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  padding: 9px 18px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.emp-btn-submit:hover:not(:disabled) {
  background: #4338ca;
}

.emp-btn-submit:disabled,
.emp-btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== Checkbox chọn dòng ===== */
.row-check {
  width: 16px;
  height: 16px;
  accent-color: #e11d48;
  cursor: pointer;
}

.row-check:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* ===== Modal hành động (import / email hàng loạt) ===== */
.action-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1050;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.action-modal {
  background: #fff;
  border-radius: 16px;
  width: min(940px, 100%);
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.3);
}

.modal-email {
  width: min(680px, 100%);
}

.action-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.action-modal-head h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.action-modal-head h3 i {
  color: #e11d48;
}

.action-modal-x {
  border: 0;
  background: #f1f5f9;
  color: #64748b;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.action-modal-x:hover {
  background: #fee2e2;
  color: #ef4444;
}

.action-modal-body {
  padding: 18px 20px;
  overflow-y: auto;
}

.action-modal-note {
  margin: 0 0 14px;
  font-size: 13.5px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
}

.action-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

/* Khu vực chọn / kéo thả tệp import */
.import-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 30px 18px;
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.import-dropzone:hover,
.import-dropzone.is-dragover {
  border-color: #f43f5e;
  background: #fff1f2;
}

.import-dropzone:focus-visible {
  outline: 2px solid #f43f5e;
  outline-offset: 2px;
}

.import-dropzone.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
  pointer-events: none;
}

.import-dropzone > i {
  font-size: 34px;
  color: #e11d48;
}

.import-dropzone-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.import-dropzone-hint {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
}

.import-file-card {
  display: flex;
  align-items: center;
  gap: 12px;
  width: min(560px, 100%);
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}

.import-file-card > i {
  font-size: 28px;
  color: #e11d48;
}

.import-file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.import-file-current {
  max-width: 100%;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.import-file-size {
  font-size: 12px;
  color: #94a3b8;
}

.import-file-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.import-file-actions .import-file-btn {
  height: 32px;
  padding: 0 10px;
  font-size: 12.5px;
}

.import-file-actions .import-file-btn.is-danger:hover {
  background: #fee2e2;
  border-color: #fecaca;
  color: #b91c1c;
}

.import-processing {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 13.5px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
}

.import-processing .spinner-border {
  color: #f43f5e;
}

.import-result {
  margin-top: 14px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 13.5px;
  color: #166534;
}

.import-result p {
  margin: 0;
}

.import-result .is-bad {
  color: #dc2626;
}

.import-result ul {
  margin: 8px 0 0;
  padding-left: 18px;
}

.import-result li {
  margin-bottom: 3px;
}

.import-result.is-failed {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}

.email-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.email-chip {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 9999px;
  padding: 4px 10px;
  font-size: 12.5px;
  color: #334155;
  max-width: 100%;
}

.email-chip i {
  font-style: normal;
  color: #64748b;
}

.email-chip.is-more {
  background: #eef2ff;
  border-color: #c7d2fe;
  color: #4338ca;
  font-weight: 600;
}

.action-field {
  margin-bottom: 14px;
}

.action-field label {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.action-field .req {
  color: #dc2626;
}

.action-field input,
.action-field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #0f172a;
  font-family: inherit;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.action-field input:focus,
.action-field textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.14);
}

.action-field textarea {
  resize: vertical;
}

.switch {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 21px;
  flex-shrink: 0;
  margin: 0;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e1;
  border-radius: 999px;
  transition: 0.25s;
}

.slider::before {
  position: absolute;
  content: "";
  width: 15px;
  height: 15px;
  left: 3px;
  top: 3px;
  background-color: white;
  border-radius: 50%;
  transition: 0.25s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.switch input:checked + .slider {
  background-color: #16a34a;
}

.switch input:checked + .slider::before {
  transform: translateX(17px);
}

.switch input:disabled + .slider {
  opacity: 0.5;
  cursor: not-allowed;
}

.switch input:focus-visible + .slider {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}
</style>
