<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const API_BASE = 'http://localhost:8080/api/v1'

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

const toastNotice = reactive({
  show: false,
  type: 'success',
  title: '',
  message: ''
})
let toastTimer = null

const showToastNotice = (title, message, type = 'success') => {
  toastNotice.title = title
  toastNotice.message = message
  toastNotice.type = type
  toastNotice.show = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastNotice.show = false
  }, 3500)
}

const activeFilterCount = computed(() => {
  let count = 0
  if (filters.keyword.trim()) count++
  if (filters.idThuongHieu) count++
  if (filters.idLoaiGiay) count++
  if (filters.idChatLieu) count++
  if (filters.idKieuDang) count++
  if (filters.idXuatXu) count++
  if (filters.doiTuong) count++
  return count
})

const fetchOptions = async () => {
  try {
    const bulkRes = await fetch(`${API_BASE}/attributes/all-filter-options`)
    if (bulkRes.ok) {
      const data = await bulkRes.json()
      brands.value = data.thuongHieu || []
      categories.value = data.loaiGiay || []
      materials.value = data.chatLieu || []
      styles.value = data.kieuDang || []
      origins.value = data.xuatXu || []
      return
    }
  } catch (e) {}

  try {
    const [resTH, resLG, resCL, resKD, resXX] = await Promise.all([
      fetch(`${API_BASE}/attributes/thuong_hieu`),
      fetch(`${API_BASE}/attributes/loai_giay`),
      fetch(`${API_BASE}/attributes/chat_lieu`),
      fetch(`${API_BASE}/attributes/kieu_dang`),
      fetch(`${API_BASE}/attributes/xuat_xu`)
    ])

    const [b, c, m, s, o] = await Promise.all([
      resTH.ok ? resTH.json() : [],
      resLG.ok ? resLG.json() : [],
      resCL.ok ? resCL.json() : [],
      resKD.ok ? resKD.json() : [],
      resXX.ok ? resXX.json() : []
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
  activeTab.value = tab
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
}

const changePage = (p) => {
  if (p >= 0 && p < totalPages.value && p !== currentPage.value) {
    currentPage.value = p
    fetchProducts()
  }
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
  if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này? Thao tác này không thể hoàn tác.')) return
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

const viewProductVariants = (item) => {
  router.push({
    path: '/san-pham/bien-the',
    query: {
      idSanPham: item.id,
      maSanPham: item.maSanPham || `SP0${item.id}`,
      tenSanPham: item.tenSanPham
    }
  })
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
    <!-- Floating Toast Notification -->
    <transition name="toast-fade">
      <div v-if="toastNotice.show" class="floating-toast" :class="toastNotice.type">
        <div class="toast-icon-box" :class="toastNotice.type">
          <svg v-if="toastNotice.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <svg v-else-if="toastNotice.type === 'warning'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
          </svg>
        </div>
        <div class="toast-text-body">
          <div class="toast-title">{{ toastNotice.title }}</div>
          <div class="toast-msg">{{ toastNotice.message }}</div>
        </div>
        <button class="toast-close-btn" @click="toastNotice.show = false">×</button>
      </div>
    </transition>

    <!-- Header Section -->
    <div class="page-top-row">
      <div class="page-intro">
        <div class="breadcrumbs-tag">
          <span>Trang chủ</span> / <span>Kho hàng</span> / <span class="active">Quản lý sản phẩm</span>
        </div>
        <h1 class="page-heading">
          Quản lý Sản phẩm
          <span class="count-badge">{{ totalElements }} sản phẩm</span>
        </h1>
        <p class="page-subheading">
          Hệ thống quản trị danh mục giày, đa thuộc tính và đồng bộ biến thể thời gian thực.
        </p>
      </div>

      <div class="page-actions">
        <button class="btn btn-outline-refresh" @click="reloadAllData" :class="{ 'is-loading': isLoading }">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"></polyline>
            <polyline points="1 20 1 14 7 14"></polyline>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
          Làm mới
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

    <!-- Filter Card -->
    <div class="filter-box">
      <!-- Tabs and Action Header -->
      <div class="status-tabs-row">
        <div class="tabs-group">
          <button class="status-tab" :class="{ active: activeTab === 'ALL' }" @click="selectTab('ALL')">
            <span class="tab-icon">📦</span>
            Tất cả
            <span class="tab-pill">{{ totalElements }}</span>
          </button>
          <button class="status-tab" :class="{ active: activeTab === 'ACTIVE' }" @click="selectTab('ACTIVE')">
            <span class="dot green-dot"></span>
            Đang kinh doanh
          </button>
          <button class="status-tab" :class="{ active: activeTab === 'INACTIVE' }" @click="selectTab('INACTIVE')">
            <span class="dot gray-dot"></span>
            Ngừng kinh doanh
          </button>
        </div>

        <div class="tab-actions-group">
          <button class="btn-toggle-expand" @click="isFilterExpanded = !isFilterExpanded">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
            </svg>
            {{ isFilterExpanded ? 'Thu gọn bộ lọc' : 'Mở rộng bộ lọc' }}
            <span v-if="activeFilterCount > 0" class="filter-count-badge">{{ activeFilterCount }}</span>
          </button>

          <button
            v-if="activeFilterCount > 0 || activeTab !== 'ALL'"
            class="btn-reset-filter"
            @click="resetFilters"
            title="Đặt lại toàn bộ bộ lọc"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
              <path d="M3 3v5h5"></path>
            </svg>
            Xóa bộ lọc
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
            <select v-model="filters.idThuongHieu" class="form-select" @change="onFilterChange">
              <option value="">Tất cả thương hiệu</option>
              <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.ten }}</option>
            </select>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Loại giày</label>
            <select v-model="filters.idLoaiGiay" class="form-select" @change="onFilterChange">
              <option value="">Tất cả loại giày</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.ten }}</option>
            </select>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Chất liệu</label>
            <select v-model="filters.idChatLieu" class="form-select" @change="onFilterChange">
              <option value="">Tất cả chất liệu</option>
              <option v-for="m in materials" :key="m.id" :value="m.id">{{ m.ten }}</option>
            </select>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Kiểu dáng</label>
            <select v-model="filters.idKieuDang" class="form-select" @change="onFilterChange">
              <option value="">Tất cả kiểu dáng</option>
              <option v-for="s in styles" :key="s.id" :value="s.id">{{ s.ten }}</option>
            </select>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Xuất xứ</label>
            <select v-model="filters.idXuatXu" class="form-select" @change="onFilterChange">
              <option value="">Tất cả xuất xứ</option>
              <option v-for="o in origins" :key="o.id" :value="o.id">{{ o.ten }}</option>
            </select>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Đối tượng</label>
            <select v-model="filters.doiTuong" class="form-select" @change="onFilterChange">
              <option value="">Tất cả đối tượng</option>
              <option value="Nam">Nam</option>
              <option value="Nữ">Nữ</option>
              <option value="Unisex">Unisex</option>
            </select>
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
              <th width="44" class="text-center">
                <input type="checkbox" v-model="isAllSelected" class="custom-chk" />
              </th>
              <th width="140">Mã sản phẩm</th>
              <th width="280">Tên sản phẩm</th>
              <th>Thương hiệu</th>
              <th>Loại giày</th>
              <th>Chất liệu</th>
              <th>Kiểu dáng</th>
              <th>Xuất xứ</th>
              <th width="150" class="text-center">Trạng thái</th>
              <th width="130" class="text-center">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in products" :key="item.id" :class="{ 'row-selected': selectedIds.includes(item.id) }">
              <td class="text-center">
                <input type="checkbox" :value="item.id" v-model="selectedIds" class="custom-chk" />
              </td>
              <td>
                <div class="code-wrapper" @click="copyToClipboard(item.maSanPham || `SP0${item.id}`)" title="Click để sao chép mã">
                  <span class="code-badge">{{ item.maSanPham || `SP0${item.id}` }}</span>
                  <svg class="copy-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                  </svg>
                </div>
              </td>
              <td>
                <div class="product-info-cell">
                  <div class="prod-thumb">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                    </svg>
                  </div>
                  <div class="prod-text">
                    <span class="prod-title" :title="item.tenSanPham">{{ item.tenSanPham }}</span>
                    <span class="prod-target-tag" :class="item.doiTuong ? item.doiTuong.toLowerCase() : 'unisex'">
                      {{ item.doiTuong || 'Unisex' }}
                    </span>
                  </div>
                </div>
              </td>
              <td>
                <span class="brand-tag">{{ item.tenThuongHieu || '---' }}</span>
              </td>
              <td>
                <span class="type-pill">{{ item.tenLoaiGiay || 'Giày Sneaker' }}</span>
              </td>
              <td><span class="attr-text">{{ item.tenChatLieu || '---' }}</span></td>
              <td><span class="attr-text">{{ item.tenKieuDang || '---' }}</span></td>
              <td><span class="attr-text">{{ item.tenXuatXu || '---' }}</span></td>
              <td class="text-center">
                <label class="switch-toggle" :title="item.trangThai ? 'Click để ngừng kinh doanh' : 'Click để bật kinh doanh'">
                  <input type="checkbox" :checked="item.trangThai" @change="toggleStatus(item)" />
                  <span class="slider round"></span>
                </label>
                <div class="status-label" :class="{ active: item.trangThai }">
                  {{ item.trangThai ? 'Đang bán' : 'Ngừng bán' }}
                </div>
              </td>
              <td class="text-center">
                <div class="action-btns">
                  <button class="act-btn variant-btn" title="Quản lý biến thể sản phẩm" @click="viewProductVariants(item)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="3" width="7" height="7"></rect>
                      <rect x="14" y="3" width="7" height="7"></rect>
                      <rect x="14" y="14" width="7" height="7"></rect>
                      <rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                  </button>
                  <button class="act-btn edit-btn" title="Chỉnh sửa nhanh" @click="openQuickEdit(item)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>
                  <button class="act-btn del-btn" title="Xóa sản phẩm" @click="deleteProduct(item.id)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>

            <!-- Skeleton loader -->
            <template v-if="isLoading && products.length === 0">
              <tr v-for="n in 5" :key="`skel-${n}`" class="skel-row">
                <td class="text-center"><div class="skel-box skel-check"></div></td>
                <td><div class="skel-box skel-code"></div></td>
                <td><div class="skel-box skel-title"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td class="text-center"><div class="skel-box skel-pill"></div></td>
                <td class="text-center"><div class="skel-box skel-btn"></div></td>
              </tr>
            </template>

            <!-- Empty State -->
            <tr v-else-if="products.length === 0">
              <td colspan="10" class="empty-state-cell">
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
                <select v-model="editForm.idThuongHieu" class="modal-select" :class="{ 'has-error': editErrors.idThuongHieu }">
                  <option value="" disabled>-- Chọn thương hiệu --</option>
                  <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.ten }}</option>
                </select>
                <span v-if="editErrors.idThuongHieu" class="err-msg">{{ editErrors.idThuongHieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Loại giày <span class="req">*</span></label>
                <select v-model="editForm.idLoaiGiay" class="modal-select" :class="{ 'has-error': editErrors.idLoaiGiay }">
                  <option value="" disabled>-- Chọn loại giày --</option>
                  <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.ten }}</option>
                </select>
                <span v-if="editErrors.idLoaiGiay" class="err-msg">{{ editErrors.idLoaiGiay }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Chất liệu <span class="req">*</span></label>
                <select v-model="editForm.idChatLieu" class="modal-select" :class="{ 'has-error': editErrors.idChatLieu }">
                  <option value="" disabled>-- Chọn chất liệu --</option>
                  <option v-for="m in materials" :key="m.id" :value="m.id">{{ m.ten }}</option>
                </select>
                <span v-if="editErrors.idChatLieu" class="err-msg">{{ editErrors.idChatLieu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Kiểu dáng <span class="req">*</span></label>
                <select v-model="editForm.idKieuDang" class="modal-select" :class="{ 'has-error': editErrors.idKieuDang }">
                  <option value="" disabled>-- Chọn kiểu dáng --</option>
                  <option v-for="s in styles" :key="s.id" :value="s.id">{{ s.ten }}</option>
                </select>
                <span v-if="editErrors.idKieuDang" class="err-msg">{{ editErrors.idKieuDang }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Xuất xứ <span class="req">*</span></label>
                <select v-model="editForm.idXuatXu" class="modal-select" :class="{ 'has-error': editErrors.idXuatXu }">
                  <option value="" disabled>-- Chọn xuất xứ --</option>
                  <option v-for="o in origins" :key="o.id" :value="o.id">{{ o.ten }}</option>
                </select>
                <span v-if="editErrors.idXuatXu" class="err-msg">{{ editErrors.idXuatXu }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">Đối tượng sử dụng</label>
                <select v-model="editForm.doiTuong" class="modal-select">
                  <option value="Nam">Nam</option>
                  <option value="Nữ">Nữ</option>
                  <option value="Unisex">Tất cả (Unisex)</option>
                </select>
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
                <label class="form-label mb-0">Trạng thái kinh doanh</label>
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
