<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchableSelect from '@/components/SearchableSelect.vue'
<<<<<<< HEAD
import { notifySuccess, notifyWarning, notifyError } from '@/utils/notify'
import { formatNumber } from '@/utils/format'
=======
import PaginationBar from '@/components/PaginationBar.vue'
import { notifySuccess, notifyWarning, notifyError, notifyDeleteConfirm } from '@/utils/notify'
import { formatNumber, formatVnd } from '@/utils/format'
import { exportExcel } from '@/utils/export'
import { API_ORIGIN } from '@/api/http'
>>>>>>> 0781738bb1a85548501dd480700da28576f66d6c

const router = useRouter()
const API_BASE = `${API_ORIGIN}/api/v1`

const navigateToAdd = () => {
  router.push('/san-pham/them')
}

const products = ref([])
const isLoading = ref(false)
const totalElements = ref(0)
const totalPages = ref(1)
const currentPage = ref(0)
const pageSize = ref(10)
const pageSizeOptions = [5, 10, 20, 50]

<<<<<<< HEAD
=======
const activeTab = ref('ALL')
const statusTabs = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'ACTIVE', label: 'Đang kinh doanh' },
  { value: 'INACTIVE', label: 'Ngừng kinh doanh' },
]
>>>>>>> 0781738bb1a85548501dd480700da28576f66d6c
const selectedIds = ref([])
const posPendingOrders = ref([])

const loadPosPendingOrders = () => {
  try {
    const raw = localStorage.getItem('polyshoes_pos_pending_orders_v1')
    posPendingOrders.value = raw ? JSON.parse(raw) : []
  } catch {
    posPendingOrders.value = []
  }
}

const getPosReservedForProduct = (item) => {
  if (!item || !Array.isArray(posPendingOrders.value)) return 0
  let reserved = 0
  for (const order of posPendingOrders.value) {
    if (!Array.isArray(order.cart)) continue
    for (const c of order.cart) {
      const matchId = c.idSanPham && item.id && Number(c.idSanPham) === Number(item.id)
      const matchCode = c.maSp && item.maSanPham && String(c.maSp).toUpperCase() === String(item.maSanPham).toUpperCase()
      const matchName = !c.idSanPham && c.name && item.tenSanPham && String(c.name).trim() === String(item.tenSanPham).trim()
      if (matchId || matchCode || matchName) {
        reserved += Number(c.quantity || 0)
      }
    }
  }
  return reserved
}

const getAvailableProductStock = (item) => {
  const base = Number(item?.soLuong ?? 0)
  return Math.max(0, base - getPosReservedForProduct(item))
}

const handlePosStorageSync = (e) => {
  if (!e || !e.key || e.key === 'polyshoes_pos_pending_orders_v1' || e.key === 'polyshoes_pos_sync_signal') {
    loadPosPendingOrders()
    if (e?.key === 'polyshoes_pos_sync_signal') {
      fetchProducts()
    }
  }
}

const filters = reactive({
  keyword: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idChatLieu: '',
  idKieuDang: '',
  idXuatXu: '',
  doiTuong: '',
  trangThai: ''
})

const statusOptions = [
  { value: true, label: 'Kinh doanh' },
  { value: false, label: 'Ngưng kinh doanh' }
]

const isFilterExpanded = ref(true)

const brands = ref([])
const categories = ref([])
const materials = ref([])
const styles = ref([])
const origins = ref([])

let debounceTimer = null
let currentAbortController = null

const isQuickAddModalOpen = ref(false)
const isSubmittingQuickAdd = ref(false)
const quickAddErrors = reactive({
  tenSanPham: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idChatLieu: '',
  idKieuDang: '',
  idXuatXu: ''
})

const quickAddForm = reactive({
  tenSanPham: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idChatLieu: '',
  idKieuDang: '',
  idXuatXu: '',
  doiTuong: 'Nam',
  tinhNang: '',
  moTa: '',
  trangThai: true
})

const showToastNotice = (title, message, type = 'success') => {
  const content = message || title
  const heading = message && title ? title : ''
  if (type === 'success') {
    notifySuccess(content, heading)
  } else if (type === 'warning') {
    notifyWarning(content, heading)
  } else {
    notifyError(content, heading)
  }
}



const normalizeAttributeList = (data) => {
  if (!data) return []
  if (Array.isArray(data)) return data
  if (Array.isArray(data.content)) return data.content
  if (Array.isArray(data.data)) return data.data
  return []
}

const fetchOptions = async () => {
  try {
    const bulkRes = await fetch(`${API_BASE}/attributes/all-filter-options`)
    if (bulkRes.ok) {
      const data = await bulkRes.json()
      if (data && typeof data === 'object') {
        brands.value = normalizeAttributeList(data.thuongHieu)
        categories.value = normalizeAttributeList(data.loaiGiay)
        materials.value = normalizeAttributeList(data.chatLieu)
        styles.value = normalizeAttributeList(data.kieuDang)
        origins.value = normalizeAttributeList(data.xuatXu)
        if (brands.value.length > 0 || categories.value.length > 0) {
          return
        }
      }
    }
  } catch (e) {
    console.warn('Lỗi gọi all-filter-options, chuyển sang gọi từng danh mục:', e)
  }

  try {
    const [resTH, resLG, resCL, resKD, resXX] = await Promise.allSettled([
      fetch(`${API_BASE}/attributes/thuong_hieu`),
      fetch(`${API_BASE}/attributes/loai_giay`),
      fetch(`${API_BASE}/attributes/chat_lieu`),
      fetch(`${API_BASE}/attributes/kieu_dang`),
      fetch(`${API_BASE}/attributes/xuat_xu`)
    ])

    const parseRes = async (settledRes) => {
      if (settledRes.status === 'fulfilled' && settledRes.value.ok) {
        try {
          const json = await settledRes.value.json()
          return normalizeAttributeList(json)
        } catch {
          return []
        }
      }
      return []
    }

    const [b, c, m, s, o] = await Promise.all([
      parseRes(resTH),
      parseRes(resLG),
      parseRes(resCL),
      parseRes(resKD),
      parseRes(resXX)
    ])

    brands.value = b
    categories.value = c
    materials.value = m
    styles.value = s
    origins.value = o
  } catch (err) {
    console.error('Lỗi tải danh mục bộ lọc:', err)
  }
}

const buildQueryParams = (pageIndex) => {
  const params = new URLSearchParams()
  params.append('page', pageIndex)
  params.append('size', pageSize.value)

  if (filters.keyword && filters.keyword.trim()) params.append('keyword', filters.keyword.trim())
  if (filters.idThuongHieu) params.append('idThuongHieu', filters.idThuongHieu)
  if (filters.idLoaiGiay) params.append('idLoaiGiay', filters.idLoaiGiay)
  if (filters.idChatLieu) params.append('idChatLieu', filters.idChatLieu)
  if (filters.idKieuDang) params.append('idKieuDang', filters.idKieuDang)
  if (filters.idXuatXu) params.append('idXuatXu', filters.idXuatXu)
  if (filters.doiTuong) params.append('doiTuong', filters.doiTuong)

  if (filters.trangThai === true || filters.trangThai === 'true') {
    params.append('trangThai', 'true')
  } else if (filters.trangThai === false || filters.trangThai === 'false') {
    params.append('trangThai', 'false')
  }

  return params
}

const fetchProducts = async () => {
  isLoading.value = true
  if (currentAbortController) {
    currentAbortController.abort()
  }
  currentAbortController = new AbortController()

  try {
    const params = buildQueryParams(currentPage.value)
    const res = await fetch(`${API_BASE}/san-pham?${params.toString()}`, {
      signal: currentAbortController.signal
    })

    if (res.ok) {
      const data = await res.json()
      products.value = data.content || []
      totalElements.value = data.totalElements !== undefined ? data.totalElements : products.value.length
      totalPages.value = data.totalPages || 1
      selectedIds.value = []
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      console.error('Lỗi khi gọi API sản phẩm:', err)
    }
  } finally {
    isLoading.value = false
  }
}

const reloadAllData = () => {
  fetchOptions()
  fetchProducts()
}

<<<<<<< HEAD
=======
const selectTab = (tab) => {
  activeTab.value = tab || 'ALL'
  currentPage.value = 0
  fetchProducts()
}

>>>>>>> 0781738bb1a85548501dd480700da28576f66d6c
const onFilterChange = () => {
  currentPage.value = 0
  fetchProducts()
}

const onSearchInput = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 0
    fetchProducts()
  }, 200)
}

const resetFilters = () => {
  filters.keyword = ''
  filters.idThuongHieu = ''
  filters.idLoaiGiay = ''
  filters.idChatLieu = ''
  filters.idKieuDang = ''
  filters.idXuatXu = ''
  filters.doiTuong = ''
  filters.trangThai = ''
  currentPage.value = 0
  fetchProducts()
  showToastNotice('Thành công', 'Đã đặt lại bộ lọc về mặc định', 'success')
}

const changePage = (p) => {
  if (p >= 0 && p < totalPages.value && p !== currentPage.value) {
    currentPage.value = p
    fetchProducts()
  }
}

const onSizeChange = (s) => {
  pageSize.value = s
  currentPage.value = 0
  fetchProducts()
}

const formatPrice = (val) => {
  if (val === null || val === undefined || isNaN(val)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

const formatProductPrice = (item) => {
  if (!item) return '0 ₫'
  if (item.giaBanMin && item.giaBanMax && item.giaBanMin !== item.giaBanMax) {
    return `${formatPrice(item.giaBanMin)} - ${formatPrice(item.giaBanMax)}`
  }
  return formatPrice(item.giaBan || 0)
}

const hasProductDiscount = (item) => {
  return item && item.discount && item.discount !== '-' && item.discount !== '0%'
}

const formatProductDiscountedPrice = (item) => {
  if (!item) return '0 ₫'
  if (item.giaSauGiamMin && item.giaSauGiamMax && item.giaSauGiamMin !== item.giaSauGiamMax) {
    return `${formatPrice(item.giaSauGiamMin)} - ${formatPrice(item.giaSauGiamMax)}`
  }
  return formatPrice(item.giaSauGiamMin || item.giaBan || 0)
}

const toggleStatus = async (item) => {
  const oldStatus = item.trangThai
  const newStatus = !oldStatus
  item.trangThai = newStatus
  const statusLabel = newStatus ? 'Đang kinh doanh' : 'Ngừng kinh doanh'
  const code = item.maSanPham || `SP0${item.id}`

  try {
    const res = await fetch(`${API_BASE}/san-pham/${item.id}/toggle-status`, { method: 'PATCH' })
    if (res.ok) {
      showToastNotice(
        'Cập nhật trạng thái thành công',
        `Đã chuyển trạng thái sản phẩm (${code}) sang "${statusLabel}"`,
        newStatus ? 'success' : 'warning'
      )
    } else {
      item.trangThai = oldStatus
      showToastNotice('Lỗi cập nhật', 'Không thể chuyển đổi trạng thái sản phẩm', 'danger')
    }
  } catch (err) {
    item.trangThai = oldStatus
    showToastNotice('Lỗi kết nối', 'Không thể kết nối với máy chủ', 'danger')
  }
}

const viewProductDetails = (item) => {
  router.push('/san-pham/bien-the')
}

const isExporting = ref(false)

const exportToExcel = async () => {
  if (isExporting.value) return
  isExporting.value = true

  try {
    let itemsToExport = []

    if (selectedIds.value.length > 0) {
      itemsToExport = products.value.filter(p => selectedIds.value.includes(p.id))
    } else {
      const params = buildQueryParams(0)
      params.set('size', '10000')
      const res = await fetch(`${API_BASE}/san-pham?${params.toString()}`)
      if (res.ok) {
        const data = await res.json()
        itemsToExport = data.content || products.value
      } else {
        itemsToExport = products.value
      }
    }

    if (!itemsToExport || itemsToExport.length === 0) {
      showToastNotice('Không có dữ liệu', 'Không có sản phẩm nào để xuất Excel', 'warning')
      return
    }

    const now = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    const dateStr = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`

    await exportExcel(
      `Danh_sach_san_pham_${dateStr}.xlsx`,
      ['STT', 'Mã sản phẩm', 'Tên sản phẩm', 'Thương hiệu', 'Loại giày', 'Số lượng', 'Giá bán', 'Đối tượng', 'Tính năng', 'Trạng thái'],
      itemsToExport.map((item, index) => [
        index + 1,
        item.maSanPham || `SP0${item.id}`,
        item.tenSanPham || '',
        item.tenThuongHieu || '',
        item.tenLoaiGiay || '',
        item.soLuong !== undefined ? item.soLuong : 0,
        item.giaBan || 0,
        item.doiTuong || 'Unisex',
        item.tinhNang || '',
        item.trangThai !== false ? 'Đang kinh doanh' : 'Ngừng kinh doanh',
      ]),
      { sheetName: 'Danh sách sản phẩm' }
    )
    showToastNotice('Thành công', `Đã xuất ${itemsToExport.length} sản phẩm ra file Excel!`, 'success')
  } catch (err) {
    console.error('Lỗi xuất Excel:', err)
    showToastNotice('Lỗi', 'Không thể xuất file Excel', 'danger')
  } finally {
    isExporting.value = false
  }
}

const openQuickAdd = () => {
  quickAddForm.tenSanPham = ''
  quickAddForm.idThuongHieu = ''
  quickAddForm.idLoaiGiay = ''
  quickAddForm.idChatLieu = ''
  quickAddForm.idKieuDang = ''
  quickAddForm.idXuatXu = ''
  quickAddForm.doiTuong = 'Nam'
  quickAddForm.tinhNang = ''
  quickAddForm.moTa = ''
  quickAddForm.trangThai = true

  Object.keys(quickAddErrors).forEach(k => (quickAddErrors[k] = ''))
  isQuickAddModalOpen.value = true
}

const validateQuickAddForm = () => {
  let valid = true
  Object.keys(quickAddErrors).forEach(k => (quickAddErrors[k] = ''))

  if (!quickAddForm.tenSanPham || !quickAddForm.tenSanPham.trim()) {
    quickAddErrors.tenSanPham = 'Tên sản phẩm không được để trống'
    valid = false
  }
  if (!quickAddForm.idThuongHieu) {
    quickAddErrors.idThuongHieu = 'Vui lòng chọn Thương hiệu'
    valid = false
  }
  if (!quickAddForm.idLoaiGiay) {
    quickAddErrors.idLoaiGiay = 'Vui lòng chọn Loại giày'
    valid = false
  }
  if (!quickAddForm.idChatLieu) {
    quickAddErrors.idChatLieu = 'Vui lòng chọn Chất liệu'
    valid = false
  }
  if (!quickAddForm.idKieuDang) {
    quickAddErrors.idKieuDang = 'Vui lòng chọn Kiểu dáng'
    valid = false
  }
  if (!quickAddForm.idXuatXu) {
    quickAddErrors.idXuatXu = 'Vui lòng chọn Xuất xứ'
    valid = false
  }
  return valid
}

const submitQuickAdd = async () => {
  if (!validateQuickAddForm()) return

  isSubmittingQuickAdd.value = true
  const payload = {
    tenSanPham: quickAddForm.tenSanPham.trim(),
    idThuongHieu: Number(quickAddForm.idThuongHieu),
    idLoaiGiay: Number(quickAddForm.idLoaiGiay),
    idChatLieu: Number(quickAddForm.idChatLieu),
    idKieuDang: Number(quickAddForm.idKieuDang),
    idXuatXu: Number(quickAddForm.idXuatXu),
    doiTuong: quickAddForm.doiTuong,
    tinhNang: quickAddForm.tinhNang ? quickAddForm.tinhNang.trim() : '',
    moTa: quickAddForm.moTa ? quickAddForm.moTa.trim() : '',
    trangThai: quickAddForm.trangThai
  }

  try {
    const res = await fetch(`${API_BASE}/san-pham`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      showToastNotice('Thành công', `Đã thêm sản phẩm "${quickAddForm.tenSanPham}" thành công!`, 'success')
      isQuickAddModalOpen.value = false
      fetchProducts()
    } else {
      const errData = await res.json().catch(() => ({}))
      showToastNotice('Lỗi thêm sản phẩm', errData.message || 'Không thể tạo sản phẩm mới', 'danger')
    }
  } catch (err) {
    showToastNotice('Lỗi kết nối', 'Không thể kết nối máy chủ', 'danger')
  } finally {
    isSubmittingQuickAdd.value = false
  }
}

const isGeneratingAi = ref(false)

const generateAiContent = async (targetForm) => {
  if (!targetForm.tenSanPham || !targetForm.tenSanPham.trim()) {
    showToastNotice('Thiếu thông tin', 'Vui lòng nhập Tên sản phẩm trước khi sinh nội dung bằng AI', 'warning')
    return
  }

  isGeneratingAi.value = true
  try {
    const brandObj = brands.value.find(b => b.id === Number(targetForm.idThuongHieu))
    const catObj = categories.value.find(c => c.id === Number(targetForm.idLoaiGiay))
    const matObj = materials.value.find(m => m.id === Number(targetForm.idChatLieu))
    const styleObj = styles.value.find(s => s.id === Number(targetForm.idKieuDang))

    const payload = {
      tenSanPham: targetForm.tenSanPham.trim(),
      thuongHieu: brandObj ? brandObj.ten : '',
      loaiGiay: catObj ? catObj.ten : '',
      chatLieu: matObj ? matObj.ten : '',
      kieuDang: styleObj ? styleObj.ten : '',
      doiTuong: targetForm.doiTuong || 'Nam'
    }

    const res = await fetch(`${API_BASE}/san-pham/ai-generate-content`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      const data = await res.json()
      if (data.tinhNang) targetForm.tinhNang = data.tinhNang
      if (data.moTa) targetForm.moTa = data.moTa
      showToastNotice('Thành công', '✨ Gemini AI đã tự động tạo tính năng và mô tả sản phẩm!', 'success')
    } else {
      showToastNotice('Lỗi', 'Không thể kết nối dịch vụ AI', 'danger')
    }
  } catch (err) {
    console.error('AI generate error:', err)
    showToastNotice('Lỗi kết nối', 'Không thể kết nối máy chủ AI', 'danger')
  } finally {
    isGeneratingAi.value = false
  }
}

onMounted(() => {
  loadPosPendingOrders()
  window.addEventListener('storage', handlePosStorageSync)
  window.addEventListener('focus', loadPosPendingOrders)
  Promise.allSettled([fetchOptions(), fetchProducts()])
})

onUnmounted(() => {
  window.removeEventListener('storage', handlePosStorageSync)
  window.removeEventListener('focus', loadPosPendingOrders)
  if (debounceTimer) clearTimeout(debounceTimer)
  if (currentAbortController) currentAbortController.abort()
})
</script>

<template>
  <div class="product-management-page">
    <div class="page-top-row">
      <div class="page-intro">
        <h1 class="page-heading">
          Quản lý Sản phẩm
        </h1>
      </div>
    </div>

    <div class="filter-box">
      <div class="status-tabs-row">
        <div class="tabs-group">
          <SearchableSelect
            v-model="activeTab"
            :options="statusTabs"
            label-key="label"
            value-key="value"
            placeholder="Trạng thái"
            @change="selectTab"
          />
        </div>

        <div class="page-actions">
          <button class="btn btn-reset-filter" @click="resetFilters" title="Đặt lại bộ lọc về mặc định">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6"></path>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
            </svg>
            Đặt lại bộ lọc
          </button>

          <button class="btn btn-outline-refresh" @click="reloadAllData" :class="{ 'is-loading': isLoading }"
            title="Tải lại dữ liệu">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10"></polyline>
              <polyline points="1 20 1 14 7 14"></polyline>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
            </svg>
            Làm mới
          </button>
          <button class="btn btn-export-excel" @click="exportToExcel" :disabled="isExporting"
            title="Xuất danh sách sản phẩm ra file Excel">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="8" y1="13" x2="16" y2="13"></line>
              <line x1="8" y1="17" x2="16" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            {{ isExporting ? 'Đang xuất...' : 'Xuất Excel' }}
          </button>
          <button class="btn btn-quick-add" @click="openQuickAdd" title="Thêm nhanh sản phẩm">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            Thêm nhanh
          </button>
          <button class="btn btn-add-new" @click="navigateToAdd">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            Thêm sản phẩm mới
          </button>
        </div>
      </div>

      <transition name="collapse-fade">
        <div v-show="isFilterExpanded" class="filters-grid">
          <div class="filter-col span-2">
            <label class="filter-lbl">Tìm kiếm sản phẩm</label>
            <div class="search-wrap">
              <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input v-model="filters.keyword" type="text" class="form-control"
                placeholder="Nhập tên hoặc mã sản phẩm (vd: SP001, Nike...)" @input="onSearchInput" />
              <button v-if="filters.keyword" class="clear-search-btn"
                @click="filters.keyword = ''; onFilterChange()">×</button>
            </div>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Thương hiệu</label>
            <SearchableSelect v-model="filters.idThuongHieu" :options="brands" label-key="ten" value-key="id"
              placeholder="Tất cả thương hiệu" search-placeholder="Tìm thương hiệu..." @change="onFilterChange" />
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Loại giày</label>
            <SearchableSelect v-model="filters.idLoaiGiay" :options="categories" label-key="ten" value-key="id"
              placeholder="Tất cả loại giày" search-placeholder="Tìm loại giày..." @change="onFilterChange" />
          </div>
        </div>
      </transition>
    </div>

    <div class="table-container-card">
      <div class="table-card-head">
        <div class="head-left">
          <h2 class="card-title">Danh sách mặt hàng</h2>
          <span class="items-count-pill">{{ products.length }}/{{ totalElements }} sản phẩm</span>
          <span v-if="selectedIds.length > 0" class="selected-badge">Đã chọn: {{ selectedIds.length }}</span>
        </div>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th width="60" class="text-center">STT</th>
              <th width="130" class="text-center">MÃ SẢN PHẨM</th>
              <th width="280">SẢN PHẨM</th>
              <th width="140" class="text-center">THƯƠNG HIỆU</th>
              <th width="140" class="text-center">LOẠI GIÀY</th>
              <th width="110" class="text-center">SỐ LƯỢNG</th>
              <th width="170" class="text-center">GIÁ BÁN</th>
              <th width="140" class="text-center">TRẠNG THÁI</th>
              <th width="120" class="text-center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in products" :key="item.id">
              <td class="text-center text-muted">{{ currentPage * pageSize + idx + 1 }}</td>
              <td class="text-center code-plain-text">{{ item.maSanPham || `SP0${item.id}` }}</td>
              <td>
                <div class="product-details-col">
                  <span class="name-plain-text">{{ item.tenSanPham }}</span>
                  <span v-if="item.doiTuong" class="target-badge">{{ item.doiTuong }}</span>
                </div>
              </td>
              <td class="text-center attr-plain-text">{{ item.tenThuongHieu || '---' }}</td>
              <td class="text-center attr-plain-text">{{ item.tenLoaiGiay || '---' }}</td>
              <td class="text-center qty-plain-text">
                <div>{{ formatNumber(getAvailableProductStock(item)) }}</div>
                <div v-if="getPosReservedForProduct(item) > 0" style="font-size: 11px; color: #d97706; font-weight: 600; margin-top: 2px;">
                  Giữ tại quầy: {{ getPosReservedForProduct(item) }} (Kho: {{ formatNumber(item.soLuong || 0) }})
                </div>
              </td>
              <td class="text-center price-plain-text">
                <div v-if="hasProductDiscount(item)" class="product-price-discount-box">
                  <div class="product-sale-price-row">
                    <span class="product-sale-price">{{ formatProductDiscountedPrice(item) }}</span>
                    <span class="badge-discount-pill">{{ item.discount }}</span>
                  </div>
                  <span class="product-original-price">{{ formatProductPrice(item) }}</span>
                </div>
                <span v-else>{{ formatProductPrice(item) }}</span>
              </td>
              <td class="text-center">
                <span class="simple-status-badge" :class="item.trangThai ? 'status-active' : 'status-inactive'">
                  {{ item.trangThai ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-btns">
                  <label class="switch-toggle" :title="item.trangThai ? 'Click để ngừng kinh doanh' : 'Click để bật kinh doanh'">
                    <input type="checkbox" :checked="item.trangThai" @change="toggleStatus(item)" />
                    <span class="slider round"></span>
                  </label>
                  <button class="act-btn view-btn" title="Xem chi tiết sản phẩm" @click="viewProductDetails(item)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>

            <template v-if="isLoading && products.length === 0">
              <tr v-for="n in 5" :key="`skel-${n}`" class="skel-row">
                <td class="text-center">
                  <div class="skel-box skel-code"></div>
                </td>
                <td>
                  <div class="skel-box skel-code"></div>
                </td>
                <td>
                  <div class="skel-box skel-title"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-pill"></div>
                </td>
                <td class="text-end">
                  <div class="skel-box skel-text"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-pill"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-btn"></div>
                </td>
              </tr>
            </template>

            <tr v-else-if="products.length === 0">
              <td colspan="9" class="empty-state-cell">
                <div class="empty-state-box">
                  <div class="empty-icon">👟</div>
                  <div class="empty-title">Không tìm thấy sản phẩm nào</div>
                  <p class="empty-desc">Thử thay đổi từ khóa tìm kiếm hoặc đặt lại các bộ lọc thuộc tính.</p>
                  <button class="btn btn-outline-reset" @click="resetFilters">Xóa bộ lọc</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="table-card-foot">
        <PaginationBar
          :page="currentPage"
          :total-pages="totalPages"
          :total-elements="totalElements"
          :page-size="pageSize"
          :sizes="pageSizeOptions"
          :from="products.length ? currentPage * pageSize + 1 : 0"
          :to="currentPage * pageSize + products.length"
          label="sản phẩm"
          @change="changePage"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <transition name="modal-fade">
      <div v-if="isEditModalOpen" class="modal-overlay" @click.self="isEditModalOpen = false">
        <div class="modal-dialog">
          <div class="modal-header">
            <div class="modal-title-group">
              <h3 class="modal-title">Chỉnh sửa thông tin sản phẩm</h3>
              <span class="modal-code-tag">{{ editForm.maSanPham }}</span>
            </div>
            <button class="modal-close" @click="isEditModalOpen = false">×</button>
          </div>

          <div class="modal-body">
            <div class="edit-grid">
              <div class="form-group span-2">
                <label class="form-label">Tên sản phẩm <span class="req">*</span></label>
                <input v-model="editForm.tenSanPham" type="text" class="modal-input"
                  :class="{ 'has-error': editErrors.tenSanPham }" placeholder="Nhập tên sản phẩm..." />
                <span v-if="editErrors.tenSanPham" class="err-msg">{{ editErrors.tenSanPham }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Thương hiệu <span class="req">*</span></label>
                <SearchableSelect v-model="editForm.idThuongHieu" :options="brands" label-key="ten" value-key="id"
                  placeholder="-- Chọn thương hiệu --" search-placeholder="Tìm thương hiệu..."
                  :has-error="!!editErrors.idThuongHieu" />
                <span v-if="editErrors.idThuongHieu" class="err-msg">{{ editErrors.idThuongHieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Loại giày <span class="req">*</span></label>
                <SearchableSelect v-model="editForm.idLoaiGiay" :options="categories" label-key="ten" value-key="id"
                  placeholder="-- Chọn loại giày --" search-placeholder="Tìm loại giày..."
                  :has-error="!!editErrors.idLoaiGiay" />
                <span v-if="editErrors.idLoaiGiay" class="err-msg">{{ editErrors.idLoaiGiay }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Chất liệu <span class="req">*</span></label>
                <SearchableSelect v-model="editForm.idChatLieu" :options="materials" label-key="ten" value-key="id"
                  placeholder="-- Chọn chất liệu --" search-placeholder="Tìm chất liệu..."
                  :has-error="!!editErrors.idChatLieu" />
                <span v-if="editErrors.idChatLieu" class="err-msg">{{ editErrors.idChatLieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Kiểu dáng <span class="req">*</span></label>
                <SearchableSelect v-model="editForm.idKieuDang" :options="styles" label-key="ten" value-key="id"
                  placeholder="-- Chọn kiểu dáng --" search-placeholder="Tìm kiểu dáng..."
                  :has-error="!!editErrors.idKieuDang" />
                <span v-if="editErrors.idKieuDang" class="err-msg">{{ editErrors.idKieuDang }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Xuất xứ <span class="req">*</span></label>
                <SearchableSelect v-model="editForm.idXuatXu" :options="origins" label-key="ten" value-key="id"
                  placeholder="-- Chọn xuất xứ --" search-placeholder="Tìm xuất xứ..."
                  :has-error="!!editErrors.idXuatXu" />
                <span v-if="editErrors.idXuatXu" class="err-msg">{{ editErrors.idXuatXu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Đối tượng sử dụng</label>
                <SearchableSelect v-model="editForm.doiTuong" :options="[
                  { value: 'Nam', label: 'Nam' },
                  { value: 'Nữ', label: 'Nữ' },
                  { value: 'Unisex', label: 'Tất cả (Unisex)' }
                ]" label-key="label" value-key="value" placeholder="Chọn đối tượng..." :allow-clear="false" />
              </div>

              <div class="form-group span-2 ai-magic-card">
                <div class="ai-magic-content">
                  <div class="ai-magic-info">
                    <span class="ai-badge">Gemini AI Copilot</span>
                    <span class="ai-magic-desc">Tự động sinh tính năng nổi bật và bài viết mô tả sản phẩm chuẩn
                      SEO</span>
                  </div>
                  <button type="button" class="btn-ai-generate" :disabled="isGeneratingAi"
                    @click="generateAiContent(editForm)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                      stroke-width="2.2">
                      <path
                        d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83">
                      </path>
                    </svg>
                    {{ isGeneratingAi ? 'Đang tạo...' : '✨ Sinh mô tả bằng AI' }}
                  </button>
                </div>
              </div>

              <div class="form-group span-2">
                <label class="form-label">Tính năng nổi bật</label>
                <input v-model="editForm.tinhNang" type="text" class="modal-input"
                  placeholder="Ví dụ: Êm ái, Chống trượt, Thoáng khí..." />
              </div>

              <div class="form-group span-2">
                <label class="form-label">Mô tả sản phẩm</label>
                <textarea v-model="editForm.moTa" class="modal-textarea" rows="3"
                  placeholder="Nhập mô tả chi tiết sản phẩm..."></textarea>
              </div>

              <div class="form-group span-2 switch-row">
                <label class="form-label mb-0">Trạng thái (Bật: Đang kinh doanh / Tắt: Ngừng kinh doanh)</label>
                <label class="switch-toggle">
                  <input type="checkbox" v-model="editForm.trangThai" />
                  <span class="slider round"></span>
                </label>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="isEditModalOpen = false">Hủy bỏ</button>
            <button class="btn btn-primary" :disabled="isUpdating" @click="submitQuickEdit">
              {{ isUpdating ? 'Đang lưu...' : 'Lưu thay đổi' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div v-if="isQuickAddModalOpen" class="modal-overlay" @click.self="isQuickAddModalOpen = false">
        <div class="modal-dialog">
          <div class="modal-header">
            <div class="modal-title-group">
              <h3 class="modal-title">Thêm nhanh sản phẩm</h3>
            </div>
            <button class="modal-close" @click="isQuickAddModalOpen = false">×</button>
          </div>

          <div class="modal-body">
            <div class="edit-grid">
              <div class="form-group span-2">
                <label class="form-label">Tên sản phẩm <span class="req">*</span></label>
                <input v-model="quickAddForm.tenSanPham" type="text" class="modal-input"
                  :class="{ 'has-error': quickAddErrors.tenSanPham }" placeholder="Nhập tên sản phẩm..." />
                <span v-if="quickAddErrors.tenSanPham" class="err-msg">{{ quickAddErrors.tenSanPham }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Thương hiệu <span class="req">*</span></label>
                <SearchableSelect v-model="quickAddForm.idThuongHieu" :options="brands" label-key="ten" value-key="id"
                  placeholder="-- Chọn thương hiệu --" search-placeholder="Tìm thương hiệu..."
                  :has-error="!!quickAddErrors.idThuongHieu" />
                <span v-if="quickAddErrors.idThuongHieu" class="err-msg">{{ quickAddErrors.idThuongHieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Loại giày <span class="req">*</span></label>
                <SearchableSelect v-model="quickAddForm.idLoaiGiay" :options="categories" label-key="ten" value-key="id"
                  placeholder="-- Chọn loại giày --" search-placeholder="Tìm loại giày..."
                  :has-error="!!quickAddErrors.idLoaiGiay" />
                <span v-if="quickAddErrors.idLoaiGiay" class="err-msg">{{ quickAddErrors.idLoaiGiay }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Chất liệu <span class="req">*</span></label>
                <SearchableSelect v-model="quickAddForm.idChatLieu" :options="materials" label-key="ten" value-key="id"
                  placeholder="-- Chọn chất liệu --" search-placeholder="Tìm chất liệu..."
                  :has-error="!!quickAddErrors.idChatLieu" />
                <span v-if="quickAddErrors.idChatLieu" class="err-msg">{{ quickAddErrors.idChatLieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Kiểu dáng <span class="req">*</span></label>
                <SearchableSelect v-model="quickAddForm.idKieuDang" :options="styles" label-key="ten" value-key="id"
                  placeholder="-- Chọn kiểu dáng --" search-placeholder="Tìm kiểu dáng..."
                  :has-error="!!quickAddErrors.idKieuDang" />
                <span v-if="quickAddErrors.idKieuDang" class="err-msg">{{ quickAddErrors.idKieuDang }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Xuất xứ <span class="req">*</span></label>
                <SearchableSelect v-model="quickAddForm.idXuatXu" :options="origins" label-key="ten" value-key="id"
                  placeholder="-- Chọn xuất xứ --" search-placeholder="Tìm xuất xứ..."
                  :has-error="!!quickAddErrors.idXuatXu" />
                <span v-if="quickAddErrors.idXuatXu" class="err-msg">{{ quickAddErrors.idXuatXu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Đối tượng sử dụng</label>
                <SearchableSelect v-model="quickAddForm.doiTuong" :options="[
                  { value: 'Nam', label: 'Nam' },
                  { value: 'Nữ', label: 'Nữ' },
                  { value: 'Unisex', label: 'Tất cả (Unisex)' }
                ]" label-key="label" value-key="value" placeholder="Chọn đối tượng..." :allow-clear="false" />
              </div>

              <div class="form-group span-2 ai-magic-card">
                <div class="ai-magic-content">
                  <div class="ai-magic-info">
                    <span class="ai-badge">Gemini AI Copilot</span>
                    <span class="ai-magic-desc">Tự động sinh tính năng nổi bật và bài viết mô tả sản phẩm chuẩn
                      SEO</span>
                  </div>
                  <button type="button" class="btn-ai-generate" :disabled="isGeneratingAi"
                    @click="generateAiContent(quickAddForm)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                      stroke-width="2.2">
                      <path
                        d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83">
                      </path>
                    </svg>
                    {{ isGeneratingAi ? 'Đang tạo...' : '✨ Sinh mô tả bằng AI' }}
                  </button>
                </div>
              </div>

              <div class="form-group span-2">
                <label class="form-label">Tính năng nổi bật</label>
                <input v-model="quickAddForm.tinhNang" type="text" class="modal-input"
                  placeholder="Ví dụ: Êm ái, Chống trượt, Thoáng khí..." />
              </div>

              <div class="form-group span-2">
                <label class="form-label">Mô tả sản phẩm</label>
                <textarea v-model="quickAddForm.moTa" class="modal-textarea" rows="3"
                  placeholder="Nhập mô tả chi tiết sản phẩm..."></textarea>
              </div>

              <div class="form-group span-2 switch-row">
                <label class="form-label mb-0">Trạng thái (Bật: Kinh doanh / Tắt: Ngưng kinh doanh)</label>
                <label class="switch-toggle">
                  <input type="checkbox" v-model="quickAddForm.trangThai" />
                  <span class="slider round"></span>
                </label>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="isQuickAddModalOpen = false">Hủy bỏ</button>
            <button class="btn btn-primary" :disabled="isSubmittingQuickAdd" @click="submitQuickAdd">
              {{ isSubmittingQuickAdd ? 'Đang lưu...' : 'Thêm sản phẩm' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<style scoped src="@/assets/styles/ProductListPage.css"></style>
