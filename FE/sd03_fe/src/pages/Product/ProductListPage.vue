<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { notifySuccess, notifyWarning, notifyError, notifyDeleteConfirm } from '@/utils/notify'
import { formatNumber, formatVnd } from '@/utils/format'
import { exportExcel } from '@/utils/export'
import { API_ORIGIN } from '@/api/http'

const router = useRouter()
const API_BASE = `${API_ORIGIN}/api/v1`

const navigateToAdd = () => {
  router.push('/san-pham/them')
}

// State
const products = ref([])
const isLoading = ref(false)
const totalElements = ref(0)
const totalPages = ref(1)
const currentPage = ref(0)
const pageSize = ref(10)
const pageSizeOptions = [5, 10, 20, 50]

const activeTab = ref('ALL')
const statusTabs = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'ACTIVE', label: 'Đang kinh doanh' },
  { value: 'INACTIVE', label: 'Ngừng kinh doanh' },
]
const selectedIds = ref([])
const isAllSelected = computed({
  get: () => products.value.length > 0 && selectedIds.value.length === products.value.length,
  set: (val) => {
    if (val) {
      selectedIds.value = products.value.map(p => p.id)
    } else {
      selectedIds.value = []
    }
  }
})

const filters = reactive({
  keyword: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idChatLieu: '',
  idKieuDang: '',
  idXuatXu: '',
  doiTuong: ''
})

const isFilterExpanded = ref(true)

const brands = ref([])
const categories = ref([])
const materials = ref([])
const styles = ref([])
const origins = ref([])

let debounceTimer = null
let currentAbortController = null

// Quick edit modal state
const isEditModalOpen = ref(false)
const isUpdating = ref(false)
const editErrors = reactive({
  tenSanPham: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idChatLieu: '',
  idKieuDang: '',
  idXuatXu: ''
})

const editForm = reactive({
  id: null,
  maSanPham: '',
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

const activeFilterCount = computed(() => {
  let count = 0
  if (filters.keyword && filters.keyword.trim()) count++
  if (filters.idThuongHieu) count++
  if (filters.idLoaiGiay) count++
  return count
})

const normalizeAttributeList = (data) => {
  if (!data) return []
  if (Array.isArray(data)) return data
  if (Array.isArray(data.content)) return data.content
  if (Array.isArray(data.data)) return data.data
  return []
}

const fetchOptions = async () => {
  // Thử endpoint gom tất cả tùy chọn lọc
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

  // Fallback: Gọi song song từng danh mục thuộc tính
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

  if (activeTab.value === 'ACTIVE') params.append('trangThai', 'true')
  else if (activeTab.value === 'INACTIVE') params.append('trangThai', 'false')

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

const selectTab = (tab) => {
  activeTab.value = tab || 'ALL'
  currentPage.value = 0
  fetchProducts()
}

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
  activeTab.value = 'ALL'
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

const deleteProduct = async (id) => {
  const target = products.value.find(p => p.id === id)
  const name = target ? (target.tenSanPham || target.maSanPham) : ''
  const confirmed = await notifyDeleteConfirm(name)
  if (!confirmed) return
  const originalList = [...products.value]
  products.value = products.value.filter(p => p.id !== id)
  if (totalElements.value > 0) totalElements.value--

  try {
    const res = await fetch(`${API_BASE}/san-pham/${id}`, { method: 'DELETE' })
    if (!res.ok) {
      const errData = await res.json().catch(() => ({}))
      products.value = originalList
      if (totalElements.value >= 0) totalElements.value++
      showToastNotice('Không thể xóa', errData.message || 'Sản phẩm đã có dữ liệu giao dịch liên quan', 'danger')
    } else {
      showToastNotice('Thành công', 'Đã xóa sản phẩm thành công', 'success')
      fetchProducts()
    }
  } catch (err) {
    products.value = originalList
    if (totalElements.value >= 0) totalElements.value++
    showToastNotice('Lỗi kết nối', 'Không thể kết nối tới máy chủ', 'danger')
  }
}

const copyToClipboard = (text) => {
  if (!text) return
  navigator.clipboard.writeText(text)
  showToastNotice('Đã sao chép', `Đã sao chép mã "${text}" vào clipboard`, 'success')
}

const viewProductDetails = (item) => {
  router.push({
    path: '/san-pham/bien-the',
    query: {
      idSanPham: item.id,
      maSanPham: item.maSanPham || `SP0${item.id}`,
      tenSanPham: item.tenSanPham
    }
  })
}
const viewProductVariants = viewProductDetails

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



// Quick edit functions
const openQuickEdit = (item) => {
  editForm.id = item.id
  editForm.maSanPham = item.maSanPham || `SP0${item.id}`
  editForm.tenSanPham = item.tenSanPham || ''
  editForm.idThuongHieu = item.idThuongHieu || (brands.value.find(b => b.ten === item.tenThuongHieu)?.id || '')
  editForm.idLoaiGiay = item.idLoaiGiay || (categories.value.find(c => c.ten === item.tenLoaiGiay)?.id || '')
  editForm.idChatLieu = item.idChatLieu || (materials.value.find(m => m.ten === item.tenChatLieu)?.id || '')
  editForm.idKieuDang = item.idKieuDang || (styles.value.find(s => s.ten === item.tenKieuDang)?.id || '')
  editForm.idXuatXu = item.idXuatXu || (origins.value.find(o => o.ten === item.tenXuatXu)?.id || '')
  editForm.doiTuong = item.doiTuong || 'Nam'
  editForm.tinhNang = item.tinhNang || ''
  editForm.moTa = item.moTa || ''
  editForm.trangThai = item.trangThai !== false

  // Reset errors
  Object.keys(editErrors).forEach(k => editErrors[k] = '')
  isEditModalOpen.value = true
}

const validateEditForm = () => {
  let valid = true
  Object.keys(editErrors).forEach(k => editErrors[k] = '')

  if (!editForm.tenSanPham || !editForm.tenSanPham.trim()) {
    editErrors.tenSanPham = 'Tên sản phẩm không được để trống'
    valid = false
  }
  if (!editForm.idThuongHieu) {
    editErrors.idThuongHieu = 'Vui lòng chọn Thương hiệu'
    valid = false
  }
  if (!editForm.idLoaiGiay) {
    editErrors.idLoaiGiay = 'Vui lòng chọn Loại giày'
    valid = false
  }
  if (!editForm.idChatLieu) {
    editErrors.idChatLieu = 'Vui lòng chọn Chất liệu'
    valid = false
  }
  if (!editForm.idKieuDang) {
    editErrors.idKieuDang = 'Vui lòng chọn Kiểu dáng'
    valid = false
  }
  if (!editForm.idXuatXu) {
    editErrors.idXuatXu = 'Vui lòng chọn Xuất xứ'
    valid = false
  }
  return valid
}

const submitQuickEdit = async () => {
  if (!validateEditForm()) return

  isUpdating.value = true
  const payload = {
    tenSanPham: editForm.tenSanPham.trim(),
    idThuongHieu: Number(editForm.idThuongHieu),
    idLoaiGiay: Number(editForm.idLoaiGiay),
    idChatLieu: Number(editForm.idChatLieu),
    idKieuDang: Number(editForm.idKieuDang),
    idXuatXu: Number(editForm.idXuatXu),
    doiTuong: editForm.doiTuong,
    tinhNang: editForm.tinhNang ? editForm.tinhNang.trim() : '',
    moTa: editForm.moTa ? editForm.moTa.trim() : '',
    trangThai: editForm.trangThai
  }

  try {
    const res = await fetch(`${API_BASE}/san-pham/${editForm.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      showToastNotice('Thành công', `Đã cập nhật sản phẩm "${editForm.tenSanPham}" thành công!`, 'success')
      isEditModalOpen.value = false
      fetchProducts()
    } else {
      const errData = await res.json().catch(() => ({}))
      showToastNotice('Lỗi cập nhật', errData.message || 'Không thể lưu thay đổi', 'danger')
    }
  } catch (err) {
    showToastNotice('Lỗi kết nối', 'Không thể kết nối máy chủ', 'danger')
  } finally {
    isUpdating.value = false
  }
}

onMounted(() => {
  Promise.allSettled([fetchOptions(), fetchProducts()])
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (currentAbortController) currentAbortController.abort()
})
</script>

<template>
  <div class="product-management-page">
    <!-- Header Section -->
    <div class="page-top-row">
      <div class="page-intro">
        <div class="breadcrumbs-tag">
          <span>Trang chủ</span> / <span class="active">Quản lý sản phẩm</span>
        </div>
        <h1 class="page-heading">
          Quản lý Sản phẩm
        </h1>
      </div>
    </div>

    <!-- Filter Card -->
    <div class="filter-box">
      <!-- Tabs and Action Header -->
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

          <button class="btn btn-outline-refresh" @click="reloadAllData" :class="{ 'is-loading': isLoading }" title="Tải lại dữ liệu">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10"></polyline>
              <polyline points="1 20 1 14 7 14"></polyline>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
            </svg>
            Làm mới
          </button>
          <button class="btn btn-export-excel" @click="exportToExcel" :disabled="isExporting" title="Xuất danh sách sản phẩm ra file Excel">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="8" y1="13" x2="16" y2="13"></line>
              <line x1="8" y1="17" x2="16" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            {{ isExporting ? 'Đang xuất...' : 'Xuất Excel' }}
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

      <!-- Filter Controls Grid -->
      <transition name="collapse-fade">
        <div v-show="isFilterExpanded" class="filters-grid">
          <div class="filter-col span-2">
            <label class="filter-lbl">Tìm kiếm sản phẩm</label>
            <div class="search-wrap">
              <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                v-model="filters.keyword"
                type="text"
                class="form-control"
                placeholder="Nhập tên hoặc mã sản phẩm (vd: SP001, Nike...)"
                @input="onSearchInput"
              />
              <button v-if="filters.keyword" class="clear-search-btn" @click="filters.keyword = ''; onFilterChange()">×</button>
            </div>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Thương hiệu</label>
            <SearchableSelect
              v-model="filters.idThuongHieu"
              :options="brands"
              label-key="ten"
              value-key="id"
              placeholder="Tất cả thương hiệu"
              search-placeholder="Tìm thương hiệu..."
              @change="onFilterChange"
            />
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Loại giày</label>
            <SearchableSelect
              v-model="filters.idLoaiGiay"
              :options="categories"
              label-key="ten"
              value-key="id"
              placeholder="Tất cả loại giày"
              search-placeholder="Tìm loại giày..."
              @change="onFilterChange"
            />
          </div>
        </div>
      </transition>
    </div>

    <!-- Table Container -->
    <div class="table-container-card">
      <div class="table-card-head">
        <div class="head-left">
          <h2 class="card-title">Danh sách mặt hàng</h2>
          <span class="items-count-pill">{{ products.length }}/{{ totalElements }} sản phẩm</span>
          <span v-if="selectedIds.length > 0" class="selected-badge">Đã chọn: {{ selectedIds.length }}</span>
        </div>
        <div class="head-right">
          <span class="page-size-label">Hiển thị:</span>
          <select v-model="pageSize" class="page-size-select" @change="fetchProducts">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }} bản ghi/trang</option>
          </select>
        </div>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th width="60" class="text-center">STT</th>
              <th width="120">MÃ SẢN PHẨM</th>
              <th width="320">SẢN PHẨM</th>
              <th width="140">THƯƠNG HIỆU</th>
              <th width="140">LOẠI GIÀY</th>
              <th width="100" class="text-center">SỐ LƯỢNG</th>
              <th width="160" class="text-end">GIÁ BÁN</th>
              <th width="140" class="text-center">TRẠNG THÁI</th>
              <th width="120" class="text-center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in products" :key="item.id">
              <td class="text-center text-muted">{{ currentPage * pageSize + idx + 1 }}</td>
              <td class="code-plain-text">{{ item.maSanPham || `SP0${item.id}` }}</td>
              <td>
                <div class="product-cell-wrapper">
                  <div class="product-thumbnail-box">
                    <img
                      v-if="item.hinhAnhDaiDien"
                      :src="item.hinhAnhDaiDien"
                      :alt="item.tenSanPham"
                      class="product-thumb-img"
                      @error="item.hinhAnhDaiDien = null"
                    />
                    <div v-else class="product-thumb-placeholder">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                        <path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                      </svg>
                    </div>
                    <span v-if="hasProductDiscount(item)" class="image-discount-badge">{{ item.discount }}</span>
                  </div>
                  <div class="product-details-col">
                    <span class="name-plain-text">{{ item.tenSanPham }}</span>
                    <span v-if="item.doiTuong" class="target-badge">{{ item.doiTuong }}</span>
                  </div>
                </div>
              </td>
              <td class="attr-plain-text">{{ item.tenThuongHieu || '---' }}</td>
              <td class="attr-plain-text">{{ item.tenLoaiGiay || '---' }}</td>
              <td class="text-center qty-plain-text">{{ formatNumber(item.soLuong !== undefined && item.soLuong !== null ? item.soLuong : 0) }}</td>
              <td class="text-end price-plain-text">
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

            <!-- Skeleton loader -->
            <template v-if="isLoading && products.length === 0">
              <tr v-for="n in 5" :key="`skel-${n}`" class="skel-row">
                <td class="text-center"><div class="skel-box skel-code"></div></td>
                <td><div class="skel-box skel-code"></div></td>
                <td><div class="skel-box skel-title"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td class="text-center"><div class="skel-box skel-pill"></div></td>
                <td class="text-end"><div class="skel-box skel-text"></div></td>
                <td class="text-center"><div class="skel-box skel-pill"></div></td>
                <td class="text-center"><div class="skel-box skel-btn"></div></td>
              </tr>
            </template>

            <!-- Empty State -->
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

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="table-card-foot">
        <span class="foot-info">
          Hiển thị <strong>{{ products.length ? currentPage * pageSize + 1 : 0 }} - {{ currentPage * pageSize + products.length }}</strong> trong tổng số <strong>{{ totalElements }}</strong> mặt hàng
        </span>
        <div class="foot-pager">
          <button class="pager-btn" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">
            ‹ Trước
          </button>
          <button
            v-for="p in totalPages"
            :key="p"
            class="pager-btn num-btn"
            :class="{ active: p - 1 === currentPage }"
            @click="changePage(p - 1)"
          >
            {{ p }}
          </button>
          <button class="pager-btn" :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)">
            Sau ›
          </button>
        </div>
      </div>
    </div>

    <!-- Quick Edit Modal -->
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
                <input
                  v-model="editForm.tenSanPham"
                  type="text"
                  class="modal-input"
                  :class="{ 'has-error': editErrors.tenSanPham }"
                  placeholder="Nhập tên sản phẩm..."
                />
                <span v-if="editErrors.tenSanPham" class="err-msg">{{ editErrors.tenSanPham }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Thương hiệu <span class="req">*</span></label>
                <SearchableSelect
                  v-model="editForm.idThuongHieu"
                  :options="brands"
                  label-key="ten"
                  value-key="id"
                  placeholder="-- Chọn thương hiệu --"
                  search-placeholder="Tìm thương hiệu..."
                  :has-error="!!editErrors.idThuongHieu"
                />
                <span v-if="editErrors.idThuongHieu" class="err-msg">{{ editErrors.idThuongHieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Loại giày <span class="req">*</span></label>
                <SearchableSelect
                  v-model="editForm.idLoaiGiay"
                  :options="categories"
                  label-key="ten"
                  value-key="id"
                  placeholder="-- Chọn loại giày --"
                  search-placeholder="Tìm loại giày..."
                  :has-error="!!editErrors.idLoaiGiay"
                />
                <span v-if="editErrors.idLoaiGiay" class="err-msg">{{ editErrors.idLoaiGiay }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Chất liệu <span class="req">*</span></label>
                <SearchableSelect
                  v-model="editForm.idChatLieu"
                  :options="materials"
                  label-key="ten"
                  value-key="id"
                  placeholder="-- Chọn chất liệu --"
                  search-placeholder="Tìm chất liệu..."
                  :has-error="!!editErrors.idChatLieu"
                />
                <span v-if="editErrors.idChatLieu" class="err-msg">{{ editErrors.idChatLieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Kiểu dáng <span class="req">*</span></label>
                <SearchableSelect
                  v-model="editForm.idKieuDang"
                  :options="styles"
                  label-key="ten"
                  value-key="id"
                  placeholder="-- Chọn kiểu dáng --"
                  search-placeholder="Tìm kiểu dáng..."
                  :has-error="!!editErrors.idKieuDang"
                />
                <span v-if="editErrors.idKieuDang" class="err-msg">{{ editErrors.idKieuDang }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Xuất xứ <span class="req">*</span></label>
                <SearchableSelect
                  v-model="editForm.idXuatXu"
                  :options="origins"
                  label-key="ten"
                  value-key="id"
                  placeholder="-- Chọn xuất xứ --"
                  search-placeholder="Tìm xuất xứ..."
                  :has-error="!!editErrors.idXuatXu"
                />
                <span v-if="editErrors.idXuatXu" class="err-msg">{{ editErrors.idXuatXu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Đối tượng sử dụng</label>
                <SearchableSelect
                  v-model="editForm.doiTuong"
                  :options="[
                    { value: 'Nam', label: 'Nam' },
                    { value: 'Nữ', label: 'Nữ' },
                    { value: 'Unisex', label: 'Tất cả (Unisex)' }
                  ]"
                  label-key="label"
                  value-key="value"
                  placeholder="Chọn đối tượng..."
                  :allow-clear="false"
                />
              </div>

              <div class="form-group span-2">
                <label class="form-label">Tính năng nổi bật</label>
                <input
                  v-model="editForm.tinhNang"
                  type="text"
                  class="modal-input"
                  placeholder="Ví dụ: Êm ái, Chống trượt, Thoáng khí..."
                />
              </div>

              <div class="form-group span-2">
                <label class="form-label">Mô tả sản phẩm</label>
                <textarea
                  v-model="editForm.moTa"
                  class="modal-textarea"
                  rows="3"
                  placeholder="Nhập mô tả chi tiết sản phẩm..."
                ></textarea>
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

  </div>
</template>

<style scoped src="@/assets/styles/ProductListPage.css"></style>
