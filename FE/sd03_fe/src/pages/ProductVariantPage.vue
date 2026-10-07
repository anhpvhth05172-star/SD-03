<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const API_BASE = 'http://localhost:8080/api/v1'

const showToast = ref(true)
const isLoading = ref(false)

const variants = ref([])
const totalElements = ref(0)
const totalPages = ref(1)
const currentPage = ref(0)
const pageSize = ref(5)
const pageSizeOptions = [5, 10, 20, 50]

const filters = reactive({
  search: '',
  idMauSac: '',
  idKichCo: '',
  trangThai: ''
})

const colorsList = ref([])
const sizesList = ref([])

const selectedProductCode = ref('SD03')
const selectedProductName = ref('Tất cả biến thể sản phẩm')

const initProductHeaderInfo = () => {
  if (route.query.tenSanPham) {
    selectedProductName.value = route.query.tenSanPham
  } else {
    selectedProductName.value = 'Tất cả biến thể sản phẩm'
  }

  if (route.query.maSanPham) {
    selectedProductCode.value = route.query.maSanPham
  } else if (route.query.idSanPham) {
    selectedProductCode.value = `SP0${route.query.idSanPham}`
  } else {
    selectedProductCode.value = 'SD03'
  }
}

let debounceTimer = null
let currentAbortController = null

try {
  const cached = localStorage.getItem('variant_list_cache')
  if (cached && !route.query.idSanPham) {
    const parsed = JSON.parse(cached)
    if (parsed && Array.isArray(parsed.content)) {
      variants.value = parsed.content
      totalElements.value = parsed.totalElements || parsed.content.length
      totalPages.value = parsed.totalPages || 1
    }
  }
} catch (e) { }

const fetchFilterOptions = async () => {
  try {
    const [resColor, resSize] = await Promise.all([
      fetch(`${API_BASE}/attributes/mau_sac`),
      fetch(`${API_BASE}/attributes/kich_co`)
    ])
    if (resColor.ok) colorsList.value = await resColor.json()
    if (resSize.ok) sizesList.value = await resSize.json()
  } catch (e) {
    console.error('Lỗi khi tải danh mục màu sắc / kích cỡ:', e)
  }
}

const buildParams = (pageIndex) => {
  const params = new URLSearchParams()
  params.append('page', pageIndex)
  params.append('size', pageSize.value)

  if (filters.search && filters.search.trim()) {
    params.append('search', filters.search.trim())
  }
  if (filters.idMauSac) {
    params.append('idMauSac', filters.idMauSac)
  }
  if (filters.idKichCo) {
    params.append('idKichCo', filters.idKichCo)
  }
  if (filters.trangThai !== '') {
    params.append('trangThai', filters.trangThai)
  }
  if (route.query.idSanPham) {
    params.append('idSanPham', route.query.idSanPham)
  }

  return params
}

const fetchVariants = async (useCache = true) => {
  isLoading.value = true
  variants.value = []

  if (currentAbortController) {
    currentAbortController.abort()
  }
  currentAbortController = new AbortController()

  try {
    const params = buildParams(currentPage.value)
    const res = await fetch(`${API_BASE}/san-pham-chi-tiet?${params.toString()}`, {
      signal: currentAbortController.signal
    })

    if (res.ok) {
      const data = await res.json()
      const content = data.content || []
      const total = data.totalElements !== undefined ? data.totalElements : content.length
      const pages = data.totalPages || 1

      variants.value = content
      totalElements.value = total
      totalPages.value = pages

      if (currentPage.value === 0 && !filters.search && !filters.idMauSac && !filters.idKichCo && filters.trangThai === '') {
        localStorage.setItem('variant_list_cache', JSON.stringify({ content, totalElements: total, totalPages: pages }))
      }
    } else {
      if (variants.value.length === 0 && !route.query.idSanPham) {
        useFallbackData()
      }
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      console.error('Lỗi khi tải biến thể sản phẩm:', err)
      if (variants.value.length === 0 && !route.query.idSanPham) {
        useFallbackData()
      }
    }
  } finally {
    isLoading.value = false
  }
}

watch(
  () => route.query,
  () => {
    initProductHeaderInfo()
    currentPage.value = 0
    fetchVariants(false)
  },
  { immediate: true }
)

const useFallbackData = () => {
  variants.value = []
  totalElements.value = 0
  totalPages.value = 1
}

const onSearchInput = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 0
    fetchVariants(false)
  }, 150)
}

const onFilterChange = () => {
  currentPage.value = 0
  fetchVariants(false)
}

const changePage = (p) => {
  if (p >= 0 && p < totalPages.value && p !== currentPage.value) {
    currentPage.value = p
    fetchVariants(false)
  }
}

const formatPrice = (val) => {
  if (!val) return '0 đ'
  return new Intl.NumberFormat('vi-VN').format(val) + ' đ'
}

const getColorHex = (item) => {
  if (item.colorHex && item.colorHex.startsWith('#')) return item.colorHex
  const name = (item.color || '').toLowerCase()
  if (name.includes('đen')) return '#000000'
  if (name.includes('trắng')) return '#ffffff'
  if (name.includes('đỏ')) return '#d92d20'
  if (name.includes('xanh rêu')) return '#15803d'
  if (name.includes('xanh')) return '#2563eb'
  if (name.includes('vàng')) return '#eab308'
  if (name.includes('bạc') || name.includes('xám')) return '#94a3b8'
  return '#475569'
}

const getVariantStatusLabel = (item) => {
  if (item.trangThai === false) return 'Ngừng bán'
  const stock = item.stock !== undefined ? Number(item.stock) : 0
  if (stock === 0) return 'Hết hàng'
  if (stock < 5) return 'Sắp hết hàng'
  return 'Còn hàng'
}

const getVariantStatusClass = (item) => {
  if (item.trangThai === false) return 'is-stopped'
  const stock = item.stock !== undefined ? Number(item.stock) : 0
  if (stock === 0) return 'is-out-of-stock'
  if (stock < 5) return 'is-low-stock'
  return 'is-selling'
}

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

const toggleStatus = async (item) => {
  const oldStatus = item.trangThai
  const newStatus = !oldStatus
  item.trangThai = newStatus
  const statusLabel = newStatus ? 'Đang bán' : 'Ngừng bán'
  const itemCode = item.maCtsp || item.maSp || `SKU-${item.id}`

  try {
    const res = await fetch(`${API_BASE}/san-pham-chi-tiet/${item.id}/toggle-status`, { method: 'PATCH' })
    if (res.ok) {
      showToastNotice(
        'Cập nhật trạng thái thành công',
        `Đã chuyển trạng thái biến thể (${itemCode}) sang "${statusLabel}"`,
        newStatus ? 'success' : 'warning'
      )
    } else {
      item.trangThai = oldStatus
      showToastNotice('Lỗi cập nhật', 'Không thể chuyển đổi trạng thái biến thể sản phẩm', 'danger')
    }
  } catch (err) {
    item.trangThai = oldStatus
    showToastNotice('Lỗi kết nối', 'Không thể kết nối tới máy chủ', 'danger')
  }
}

// EDIT MODAL STATE
const isEditModalOpen = ref(false)
const editingVariantId = ref(null)
const editForm = reactive({
  maCtsp: '',
  idMauSac: '',
  idKichCo: '',
  stock: 10,
  price: 1000000,
  trangThai: true
})
const formError = ref('')

const openEditModal = (item) => {
  editingVariantId.value = item.id
  formError.value = ''
  editForm.maCtsp = item.maCtsp || `SPCT0${item.id}`
  editForm.idMauSac = item.idMauSac || (colorsList.value.find(c => (c.ten || c.tenMau) === item.color)?.id || '')
  editForm.idKichCo = item.idKichCo || (sizesList.value.find(s => (s.ten || s.tenKichCo) === item.size)?.id || '')
  editForm.stock = item.stock !== undefined ? item.stock : 10
  editForm.price = item.price !== undefined ? item.price : 1000000
  editForm.trangThai = item.trangThai !== false
  isEditModalOpen.value = true
}

const closeEditModal = () => {
  isEditModalOpen.value = false
  formError.value = ''
}

const saveEditVariant = async () => {
  if (editForm.idMauSac === '' || editForm.idMauSac === null) {
    formError.value = 'Vui lòng chọn màu sắc cho biến thể!'
    return
  }
  if (editForm.idKichCo === '' || editForm.idKichCo === null) {
    formError.value = 'Vui lòng chọn kích cỡ cho biến thể!'
    return
  }
  const stockNum = Number(editForm.stock)
  if (editForm.stock === null || editForm.stock === undefined || isNaN(stockNum) || stockNum < 0 || stockNum > 100000) {
    formError.value = 'Số lượng tồn phải là số nguyên từ 0 đến 100,000!'
    return
  }
  const priceNum = Number(editForm.price)
  if (!editForm.price || isNaN(priceNum) || priceNum < 1000 || priceNum > 1000000000) {
    formError.value = 'Giá bán phải từ 1,000 VNĐ đến 1,000,000,000 VNĐ!'
    return
  }

  const currentId = editingVariantId.value
  const targetIdx = variants.value.findIndex(v => v.id === currentId)
  if (targetIdx === -1) return

  // Kiểm tra trùng lặp biến thể khác trong cùng danh sách
  const currentVariant = variants.value[targetIdx]
  const isDuplicate = variants.value.some(v => 
    v.id !== currentId && 
    (v.idSanPham === currentVariant.idSanPham || !v.idSanPham) &&
    Number(v.idMauSac) === Number(editForm.idMauSac) && 
    Number(v.idKichCo) === Number(editForm.idKichCo)
  )
  if (isDuplicate) {
    formError.value = 'Biến thể với Màu sắc và Kích cỡ này đã tồn tại trong sản phẩm!'
    return
  }

  const backupItem = { ...variants.value[targetIdx] }
  const selectedColor = colorsList.value.find(c => c.id === Number(editForm.idMauSac))
  const selectedSize = sizesList.value.find(s => s.id === Number(editForm.idKichCo))

  // 1. Optimistic UI update
  variants.value[targetIdx].stock = Number(editForm.stock)
  variants.value[targetIdx].price = Number(editForm.price)
  variants.value[targetIdx].trangThai = editForm.trangThai
  variants.value[targetIdx].maCtsp = editForm.maCtsp
  if (selectedColor) {
    variants.value[targetIdx].idMauSac = selectedColor.id
    variants.value[targetIdx].color = selectedColor.ten || selectedColor.tenMau
    variants.value[targetIdx].colorHex = selectedColor.moTa || '#64748b'
  }
  if (selectedSize) {
    variants.value[targetIdx].idKichCo = selectedSize.id
    variants.value[targetIdx].size = selectedSize.ten || selectedSize.tenKichCo
  }

  closeEditModal()
  showToastNotice('Thành công', `Đã cập nhật biến thể "${variants.value[targetIdx].maCtsp}" thành công!`, 'success')

  // 2. Background API Sync
  try {
    const payload = {
      idMauSac: editForm.idMauSac ? Number(editForm.idMauSac) : null,
      idKichCo: editForm.idKichCo ? Number(editForm.idKichCo) : null,
      soLuong: Number(editForm.stock),
      giaBan: Number(editForm.price),
      trangThai: editForm.trangThai,
      maChiTietSanPham: editForm.maCtsp
    }

    const res = await fetch(`${API_BASE}/san-pham-chi-tiet/${currentId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      fetchVariants(false)
    } else {
      const err = await res.json().catch(() => ({}))
      variants.value[targetIdx] = backupItem
      showToastNotice('Lỗi cập nhật', err.message || 'Không thể lưu thay đổi vào hệ thống', 'danger')
    }
  } catch (e) {
    console.error('Update variant error:', e)
    variants.value[targetIdx] = backupItem
    showToastNotice('Lỗi kết nối', 'Không thể kết nối máy chủ', 'danger')
  }
}

// CONFIRM MODAL STATE
const confirmModal = reactive({
  isOpen: false,
  title: '',
  message: '',
  confirmText: 'Xác nhận xóa',
  cancelText: 'Hủy bỏ',
  variant: 'danger',
  onConfirm: null
})

const openConfirmModal = ({ title, message, confirmText = 'Xác nhận', cancelText = 'Hủy bỏ', variant = 'danger', onConfirm }) => {
  confirmModal.title = title
  confirmModal.message = message
  confirmModal.confirmText = confirmText
  confirmModal.cancelText = cancelText
  confirmModal.variant = variant
  confirmModal.onConfirm = onConfirm
  confirmModal.isOpen = true
}

const handleConfirmAction = async () => {
  const callback = confirmModal.onConfirm
  confirmModal.isOpen = false
  if (callback) {
    callback()
  }
}

const handleCancelConfirm = () => {
  confirmModal.isOpen = false
}

const confirmDeleteVariant = (item) => {
  const itemCode = item.maCtsp || `SKU-${item.id}`
  openConfirmModal({
    title: 'Xác nhận xóa biến thể sản phẩm',
    message: `Bạn có chắc chắn muốn xóa biến thể "${itemCode}" (${item.color || 'Màu mặc định'} - Size ${item.size || 'Mặc định'}) không? Thao tác này sẽ xóa vĩnh viễn biến thể khỏi hệ thống.`,
    confirmText: 'Xóa ngay',
    cancelText: 'Hủy bỏ',
    variant: 'danger',
    onConfirm: () => executeDeleteVariant(item)
  })
}

const executeDeleteVariant = async (item) => {
  const targetId = item.id
  const originalList = [...variants.value]
  const targetIdx = variants.value.findIndex(v => v.id === targetId)
  if (targetIdx !== -1) {
    variants.value.splice(targetIdx, 1)
  }
  if (totalElements.value > 0) totalElements.value--

  showToastNotice('Đã xóa', `Đã xóa biến thể "${item.maCtsp || `ID ${targetId}`}" thành công!`, 'success')

  try {
    const res = await fetch(`${API_BASE}/san-pham-chi-tiet/${targetId}`, { method: 'DELETE' })
    if (res.ok) {
      fetchVariants(false)
    } else {
      const err = await res.json().catch(() => ({}))
      variants.value = originalList
      if (totalElements.value >= 0) totalElements.value++
      showToastNotice('Không thể xóa', err.message || 'Không thể xóa biến thể do có hóa đơn hoặc dữ liệu liên quan!', 'danger')
    }
  } catch (e) {
    console.error('Delete variant error:', e)
    variants.value = originalList
    if (totalElements.value >= 0) totalElements.value++
    showToastNotice('Lỗi kết nối', 'Không thể kết nối máy chủ', 'danger')
  }
}

const resetFilters = () => {
  filters.search = ''
  filters.idMauSac = ''
  filters.idKichCo = ''
  filters.trangThai = ''
  currentPage.value = 0
  if (route.query.idSanPham || route.query.tenSanPham || route.query.maSanPham) {
    router.push({ path: '/san-pham/bien-the' })
  } else {
    fetchVariants(false)
  }
}

onMounted(() => {
  fetchFilterOptions()
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (currentAbortController) currentAbortController.abort()
})
</script>

<template>
  <div class="product-variant-page">

    <div class="page-top-bar">
      <div class="title-info">
        <h1 class="heading-title">
          Biến thể của: <span class="highlight-product">{{ selectedProductName }}</span>
        </h1>
        <p class="product-code-sub">Mã sản phẩm: {{ selectedProductCode }}</p>
      </div>


      <transition name="toast-fade">
        <div v-if="toastNotice.show" class="toast-card" :class="toastNotice.type">
          <div class="toast-icon" :class="toastNotice.type">
            <svg v-if="toastNotice.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            <svg v-else-if="toastNotice.type === 'warning'" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2.5">
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
          <div class="toast-content">
            <div class="toast-title">{{ toastNotice.title }}</div>
            <div class="toast-sub">{{ toastNotice.message }}</div>
          </div>
          <button class="toast-close" @click="toastNotice.show = false">×</button>
        </div>
        <div v-else-if="showToast" class="toast-card">
          <div class="toast-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>
          <div class="toast-content">
            <div class="toast-title">Xem CTSP thành công</div>
            <div class="toast-sub">Đang xem CTSP của {{ selectedProductName }} ({{ selectedProductCode }})</div>
          </div>
          <button class="toast-close" @click="showToast = false">×</button>
        </div>
      </transition>
    </div>


    <div class="filter-card">
      <div class="filter-row-top">

        <div class="filter-search-box">
          <label class="filter-lbl">Tìm kiếm theo tên hoặc mã phân loại</label>
          <div class="search-input-wrap">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input v-model="filters.search" type="text" class="form-control"
              placeholder="Nhập mã sản phẩm, phân loại, màu sắc..." @input="onSearchInput" />
          </div>
        </div>

        <div class="filter-action-btns">
          <button class="btn-action-outline" @click="resetFilters">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6"></path>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
            </svg>
            Đặt lại bộ lọc
          </button>
          <button class="btn-action-outline">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            Tải QR
          </button>
          <button class="btn-action-outline">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            Xuất Excel
          </button>
        </div>
      </div>


      <div class="filter-dropdowns-grid">
        <div class="filter-col">
          <label class="filter-lbl">Màu sắc</label>
          <select v-model="filters.idMauSac" class="form-select" @change="onFilterChange">
            <option value="">Tất cả màu sắc</option>
            <option v-for="c in colorsList" :key="c.id" :value="c.id">{{ c.ten || c.tenMau }}</option>
          </select>
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Kích cỡ</label>
          <select v-model="filters.idKichCo" class="form-select" @change="onFilterChange">
            <option value="">Tất cả kích cỡ</option>
            <option v-for="s in sizesList" :key="s.id" :value="s.id">{{ s.ten || s.tenKichCo }}</option>
          </select>
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Trạng thái</label>
          <select v-model="filters.trangThai" class="form-select" @change="onFilterChange">
            <option value="">Tất cả trạng thái</option>
            <option :value="true">Đang bán</option>
            <option :value="false">Ngừng bán</option>
          </select>
        </div>
      </div>
    </div>


    <div class="table-card">
      <div class="table-responsive">
        <table class="variant-data-table">
          <thead>
            <tr>
              <th width="40" class="text-center"><input type="checkbox" /></th>
              <th width="50" class="text-center">STT</th>
              <th width="110">Mã SP</th>
              <th width="120">Mã CTSP</th>
              <th width="80" class="text-center">Ảnh</th>
              <th width="140">Màu sắc</th>
              <th width="90">Kích cỡ</th>
              <th width="90">Số lượng</th>
              <th width="140">Giá bán</th>
              <th width="90">Giảm</th>
              <th width="130">Trạng thái</th>
              <th width="120" class="text-center">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="isLoading && variants.length === 0">
              <tr v-for="n in 5" :key="`skel-${n}`" class="skel-row">
                <td class="text-center">
                  <div class="skel-box skel-check"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-num"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-img"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td>
                  <div class="skel-box skel-num"></div>
                </td>
                <td>
                  <div class="skel-box skel-num"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td>
                  <div class="skel-box skel-num"></div>
                </td>
                <td>
                  <div class="skel-box skel-pill"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-actions"></div>
                </td>
              </tr>
            </template>

            <template v-else>
              <tr v-for="(item, idx) in variants" :key="item.id">
                <td class="text-center"><input type="checkbox" /></td>
                <td class="text-center index-col">{{ currentPage * pageSize + idx + 1 }}</td>
                <td class="code-col">{{ item.maSp || 'SP020' }}</td>
                <td class="code-ctsp-col">{{ item.maCtsp || `SPCT0${item.id}` }}</td>
                <td class="text-center">
                  <img v-if="item.img" :src="item.img" alt="Thumbnail" class="variant-thumb" @error="item.img = null" />
                  <div v-else class="variant-thumb-placeholder" :style="{ backgroundColor: getColorHex(item) + '22', color: getColorHex(item) }">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                    </svg>
                  </div>
                </td>
                <td>
                  <div class="color-cell">
                    <span class="color-dot" :style="{ backgroundColor: getColorHex(item) }"></span>
                    <span class="color-name">{{ item.color || 'Đen trắng' }}</span>
                  </div>
                </td>
                <td class="size-col">{{ item.size || '44.5' }}</td>
                <td class="stock-col">
                  <span class="stock-badge" :class="getVariantStatusClass(item)">
                    {{ item.stock !== undefined ? item.stock : 0 }}
                  </span>
                </td>
                <td class="price-val">{{ formatPrice(item.price || 0) }}</td>
                <td class="discount-val">{{ item.discount || '-' }}</td>
                <td>
                  <span class="status-pill-badge" :class="getVariantStatusClass(item)">
                    <span class="status-indicator-dot"></span>
                    {{ getVariantStatusLabel(item) }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="action-icon-group">

                    <button class="act-circle-btn power-btn" :class="{ active: item.trangThai !== false }"
                      :title="item.trangThai !== false ? 'Ngừng kinh doanh' : 'Kích hoạt kinh doanh'"
                      @click="toggleStatus(item)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="2.2">
                        <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                        <line x1="12" y1="2" x2="12" y2="12"></line>
                      </svg>
                    </button>

                    <button class="act-circle-btn edit-btn" title="Chỉnh sửa biến thể" @click="openEditModal(item)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="2.2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                      </svg>
                    </button>

                    <button class="act-circle-btn del-btn" title="Xóa biến thể" @click="confirmDeleteVariant(item)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="2.2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>

              <tr v-if="variants.length === 0">
                <td colspan="12" style="text-align: center; padding: 40px; color: #64748b;">
                  Không tìm thấy biến thể sản phẩm nào phù hợp.
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div class="table-footer-pagination">
        <div class="foot-info">
          Hiển thị {{ variants.length ? currentPage * pageSize + 1 : 0 }} - {{ currentPage * pageSize + variants.length
          }}
          trong {{ totalElements }} biến thể chi tiết
          <span class="page-badge-pill">Trang {{ currentPage + 1 }}/{{ totalPages }}</span>
        </div>

        <div class="foot-controls">
          <span class="page-size-lbl">Kích thước trang:</span>
          <select v-model="pageSize" class="page-size-select" @change="fetchVariants(false)">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}/trang</option>
          </select>

          <div class="pager-btns-group">
            <button class="pager-nav-btn" :disabled="currentPage === 0" @click="changePage(0)"
              title="Trang đầu">«</button>
            <button class="pager-nav-btn" :disabled="currentPage === 0" @click="changePage(currentPage - 1)"
              title="Trang trước">‹</button>

            <button v-for="p in totalPages" :key="p" class="pager-num-btn" :class="{ active: p - 1 === currentPage }"
              @click="changePage(p - 1)">
              {{ p }}
            </button>

            <button class="pager-nav-btn" :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)"
              title="Trang sau">›</button>
            <button class="pager-nav-btn" :disabled="currentPage >= totalPages - 1" @click="changePage(totalPages - 1)"
              title="Trang cuối">»</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Chỉnh Sửa Biến Thể -->
    <transition name="fade">
      <div v-if="isEditModalOpen" class="modal-backdrop" @click.self="closeEditModal">
        <div class="modal-box">
          <div class="modal-header">
            <h3>Chỉnh sửa biến thể sản phẩm</h3>
            <button class="btn-close" @click="closeEditModal">✕</button>
          </div>

          <div class="modal-body">
            <div v-if="formError" class="form-error-alert">
              <span>⚠️ {{ formError }}</span>
            </div>

            <div class="form-group margin-bottom">
              <label class="form-lbl">Mã chi tiết sản phẩm (Mã CTSP)</label>
              <input v-model="editForm.maCtsp" type="text" class="form-inp disabled" readonly />
            </div>

            <div class="form-row-2col margin-bottom">
              <div class="form-group">
                <label class="form-lbl">Màu sắc <span class="required">*</span></label>
                <select v-model="editForm.idMauSac" class="form-inp">
                  <option value="" disabled>-- Chọn màu sắc --</option>
                  <option v-for="c in colorsList" :key="c.id" :value="c.id">
                    {{ c.ten || c.tenMau }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-lbl">Kích cỡ <span class="required">*</span></label>
                <select v-model="editForm.idKichCo" class="form-inp">
                  <option value="" disabled>-- Chọn kích cỡ --</option>
                  <option v-for="s in sizesList" :key="s.id" :value="s.id">
                    {{ s.ten || s.tenKichCo }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-row-2col margin-bottom">
              <div class="form-group">
                <label class="form-lbl">Số lượng tồn kho <span class="required">*</span></label>
                <input v-model.number="editForm.stock" type="number" min="0" class="form-inp"
                  placeholder="Nhập số lượng tồn..." />
              </div>

              <div class="form-group">
                <label class="form-lbl">Giá bán (VNĐ) <span class="required">*</span></label>
                <input v-model.number="editForm.price" type="number" min="0" step="10000" class="form-inp"
                  placeholder="Nhập giá bán..." />
              </div>
            </div>

            <div class="form-group">
              <label class="form-lbl">Trạng thái kinh doanh</label>
              <select v-model="editForm.trangThai" class="form-inp">
                <option :value="true">Đang bán</option>
                <option :value="false">Ngừng bán</option>
              </select>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="closeEditModal">Hủy bỏ</button>
            <button class="btn btn-primary" @click="saveEditVariant">
              Lưu thay đổi
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Confirmation Modal Dialog -->
    <transition name="fade">
      <div v-if="confirmModal.isOpen" class="modal-backdrop modal-backdrop-blur" @click.self="handleCancelConfirm">
        <div class="confirm-modal-box">
          <button class="confirm-close-btn" @click="handleCancelConfirm" title="Đóng">✕</button>

          <div class="confirm-hero-badge badge-danger">
            <div class="confirm-icon-glow">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                <line x1="10" y1="11" x2="10" y2="17"></line>
                <line x1="14" y1="11" x2="14" y2="17"></line>
              </svg>
            </div>
          </div>

          <h3 class="confirm-title">{{ confirmModal.title }}</h3>
          <p class="confirm-message">{{ confirmModal.message }}</p>

          <div class="confirm-actions">
            <button class="btn-cancel" @click="handleCancelConfirm">
              {{ confirmModal.cancelText }}
            </button>
            <button class="btn-submit btn-danger" @click="handleConfirmAction">
              {{ confirmModal.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped src="@/assets/styles/ProductVariantPage.css"></style>

