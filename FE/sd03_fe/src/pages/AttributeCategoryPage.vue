<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const API_BASE = 'http://localhost:8080/api/v1/attributes'

const toastMessage = ref('')
const toastType = ref('success')
const showToast = (msg, type = 'success') => {
  toastMessage.value = msg
  toastType.value = type
  setTimeout(() => {
    toastMessage.value = ''
  }, 3500)
}

const slugToIdMap = {
  'thuong-hieu': 'thuong_hieu',
  'xuat-xu': 'xuat_xu',
  'chat-lieu': 'chat_lieu',
  'kieu-dang': 'kieu_dang',
  'loai-giay': 'loai_giay',
  'kich-co': 'kich_co',
  'mau-sac': 'mau_sac',
  'than-giay': 'than_giay',
  'de-giay': 'de_giay'
}

const getTabFromRoute = () => {
  const param = route.params.category
  if (param && slugToIdMap[param]) {
    return slugToIdMap[param]
  }
  if (param && attributesData[param]) {
    return param
  }
  return 'thuong_hieu'
}

const activeTab = ref(getTabFromRoute())

const tabs = [
  { id: 'thuong_hieu', label: 'Thương hiệu', icon: '🏷️', codePrefix: 'TH' },
  { id: 'xuat_xu', label: 'Xuất xứ', icon: '🌐', codePrefix: 'XX' },
  { id: 'chat_lieu', label: 'Chất liệu', icon: '🧵', codePrefix: 'CL' },
  { id: 'kieu_dang', label: 'Kiểu dáng', icon: '👟', codePrefix: 'KD' },
  { id: 'loai_giay', label: 'Loại giày', icon: '📌', codePrefix: 'LG' },
  { id: 'kich_co', label: 'Kích cỡ', icon: '📐', codePrefix: 'KC' },
  { id: 'mau_sac', label: 'Màu sắc', icon: '🎨', codePrefix: 'MS' },
  { id: 'than_giay', label: 'Thân giày', icon: '🥾', codePrefix: 'TG' },
  { id: 'de_giay', label: 'Đế giày', icon: '👣', codePrefix: 'DG' }
]

const attributesData = reactive({
  thuong_hieu: [],
  xuat_xu: [],
  chat_lieu: [],
  kieu_dang: [],
  loai_giay: [],
  kich_co: [],
  mau_sac: [],
  than_giay: [],
  de_giay: []
})

const isLoading = ref(false)

const fetchAttributes = async (category) => {
  isLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/${category}`)
    if (res.ok) {
      const data = await res.json()
      attributesData[category] = data.map(item => ({
        ...item,
        mo_ta: item.moTa !== undefined ? item.moTa : (item.mo_ta || ''),
        trang_thai: item.trangThai !== undefined ? item.trangThai : (item.trang_thai || 'Hoạt động')
      }))
    } else {
      attributesData[category] = []
    }
  } catch (err) {
    console.error(`Lỗi kết nối API danh mục ${category}:`, err)
    attributesData[category] = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  activeTab.value = getTabFromRoute()
  fetchAttributes(activeTab.value)
})

watch(() => route.params.category, () => {
  const target = getTabFromRoute()
  if (activeTab.value !== target) {
    activeTab.value = target
  }
})

watch(activeTab, (newCategory) => {
  fetchAttributes(newCategory)
})

const searchQuery = ref('')
const statusFilter = ref('Tất cả')

const currentPage = ref(1)
const pageSize = ref(5)
const pageSizeOptions = [5, 10, 20, 50]

watch([activeTab, searchQuery, statusFilter, pageSize], () => {
  currentPage.value = 1
})

const currentTabItems = computed(() => {
  const items = attributesData[activeTab.value] || []
  return items.filter(item => {
    if (statusFilter.value !== 'Tất cả' && item.trang_thai !== statusFilter.value) {
      return false
    }
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      const matchTen = item.ten ? item.ten.toLowerCase().includes(q) : false
      const matchMa = item.ma ? item.ma.toLowerCase().includes(q) : false
      const matchMoTa = item.mo_ta ? item.mo_ta.toLowerCase().includes(q) : false
      if (!matchTen && !matchMa && !matchMoTa) return false
    }
    return true
  })
})

const totalPages = computed(() => {
  return Math.ceil(currentTabItems.value.length / pageSize.value) || 1
})

const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return currentTabItems.value.slice(start, start + pageSize.value)
})

const getIndex = (indexOnPage) => {
  return (currentPage.value - 1) * pageSize.value + indexOnPage + 1
}

const goToPage = (p) => {
  if (p >= 1 && p <= totalPages.value) {
    currentPage.value = p
  }
}

const activeTabInfo = computed(() => {
  return tabs.find(t => t.id === activeTab.value) || tabs[0]
})

const isModalOpen = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const formError = ref('')

const modalForm = reactive({
  ma: '',
  ten: '',
  mo_ta: '',
  trang_thai: 'Hoạt động'
})

const openAddModal = () => {
  isEditing.value = false
  editingId.value = null
  formError.value = ''
  const prefix = activeTabInfo.value.codePrefix
  const nextNum = (attributesData[activeTab.value].length + 1).toString().padStart(3, '0')
  modalForm.ma = `${prefix}${nextNum}`
  modalForm.ten = ''
  modalForm.mo_ta = activeTab.value === 'mau_sac' ? '#10b981' : ''
  modalForm.trang_thai = 'Hoạt động'
  isModalOpen.value = true
}

const openEditModal = (item) => {
  isEditing.value = true
  editingId.value = item.id
  formError.value = ''
  modalForm.ma = item.ma
  modalForm.ten = item.ten
  modalForm.mo_ta = item.mo_ta || (activeTab.value === 'mau_sac' ? '#10b981' : '')
  modalForm.trang_thai = item.trang_thai
  isModalOpen.value = true
}

const closeModal = () => {
  isModalOpen.value = false
  formError.value = ''
}

const confirmModal = reactive({
  isOpen: false,
  title: '',
  message: '',
  confirmText: 'Xác nhận',
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

const isSubmitting = ref(false)

// SAVE / UPDATE ATTRIBUTE
const saveAttributeDirectly = async () => {
  formError.value = ''
  const tenTrimmed = (modalForm.ten || '').trim()
  if (!tenTrimmed) {
    formError.value = 'Vui lòng nhập tên thuộc tính!'
    return
  }

  const category = activeTab.value
  const isEdit = isEditing.value
  const label = activeTabInfo.value.label
  const currentId = editingId.value

  if (category === 'kich_co') {
    const formatted = tenTrimmed.replace(',', '.')
    if (!/^\d+(\.\d+)?$/.test(formatted)) {
      formError.value = 'Kích cỡ giày phải là số (ví dụ: 38, 39, 40, 40.5, 41), không được nhập chữ!'
      return
    }
    const num = Number(formatted)
    if (isNaN(num) || num <= 0 || num > 100) {
      formError.value = 'Kích cỡ giày phải là số hợp lệ từ 10 đến 60!'
      return
    }
  }

  const payload = {
    ma: modalForm.ma ? modalForm.ma.trim() : null,
    ten: tenTrimmed,
    moTa: (modalForm.mo_ta || '').trim(),
    trangThai: modalForm.trang_thai === 'Hoạt động'
  }

  isSubmitting.value = true

  try {
    const url = isEdit ? `${API_BASE}/${category}/${currentId}` : `${API_BASE}/${category}`
    const method = isEdit ? 'PUT' : 'POST'

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      closeModal()
      showToast(
        isEdit ? `Cập nhật thành công ${label}: "${tenTrimmed}"` : `Thêm mới thành công ${label}: "${tenTrimmed}"`,
        'success'
      )
      await fetchAttributes(category)
    } else {
      const errData = await res.json().catch(() => ({}))
      const msg = errData.message || (isEdit ? 'Cập nhật thất bại!' : 'Thêm mới thất bại!')
      formError.value = msg
      showToast(msg, 'warning')
    }
  } catch (err) {
    console.error('Save attribute error:', err)
    formError.value = 'Không thể kết nối đến máy chủ!'
    showToast('Không thể kết nối đến máy chủ!', 'warning')
  } finally {
    isSubmitting.value = false
  }
}

// TOGGLE STATUS
const toggleStatusFast = async (item) => {
  const category = activeTab.value
  try {
    const res = await fetch(`${API_BASE}/${category}/${item.id}/toggle-status`, {
      method: 'PATCH'
    })
    if (res.ok) {
      const updated = await res.json()
      item.trang_thai = updated.trangThai !== undefined ? updated.trangThai : (updated.status ? 'Hoạt động' : 'Ngừng hoạt động')
      showToast(`Đã cập nhật trạng thái "${item.ten}" thành "${item.trang_thai}"!`, 'success')
    } else {
      const errData = await res.json().catch(() => ({}))
      showToast(errData.message || 'Lỗi cập nhật trạng thái trên máy chủ!', 'warning')
    }
  } catch (err) {
    console.error('Toggle status error:', err)
    showToast('Lỗi kết nối máy chủ!', 'warning')
  }
}

// CONFIRM & DELETE
const confirmDeleteItem = (item) => {
  openConfirmModal({
    title: `Xác nhận xóa ${activeTabInfo.value.label}`,
    message: `Bạn có chắc chắn muốn xóa ${activeTabInfo.value.label.toLowerCase()} "${item.ten}" không?`,
    confirmText: 'Xóa ngay',
    cancelText: 'Hủy bỏ',
    variant: 'danger',
    onConfirm: () => executeDeleteFast(item)
  })
}

const executeDeleteFast = async (item) => {
  const category = activeTab.value
  try {
    const res = await fetch(`${API_BASE}/${category}/${item.id}`, {
      method: 'DELETE'
    })
    if (res.ok) {
      showToast(`Đã xóa ${activeTabInfo.value.label.toLowerCase()} "${item.ten}" thành công!`, 'success')
      await fetchAttributes(category)
    } else {
      const errData = await res.json().catch(() => ({}))
      showToast(errData.message || `Không thể xóa "${item.ten}" do có dữ liệu liên quan!`, 'warning')
    }
  } catch (err) {
    console.error('Delete error:', err)
    showToast('Lỗi kết nối khi xóa thuộc tính!', 'warning')
  }
}

</script>

<template>
  <div class="attribute-management-page">
    <transition name="fade">
      <div v-if="toastMessage" class="toast-floating" :class="{ 'toast-warning': toastType === 'warning' }">
        <span>{{ toastMessage }}</span>
      </div>
    </transition>

    <div class="page-header-row">
      <div class="page-title-box">
        <h1 class="page-title">
          <span style="margin-right: 8px;">{{ activeTabInfo.icon }}</span>
          Quản lý {{ activeTabInfo.label }}
        </h1>
        <p class="page-subtitle">
          Danh sách và thông tin quản lý thuộc tính {{ activeTabInfo.label }} trong hệ thống.
        </p>
      </div>

      <div class="page-actions-box">
        <button class="btn btn-add-attr" @click="openAddModal">
          <span class="plus-icon">+</span>
          Thêm {{ activeTabInfo.label }} mới
        </button>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-row">
        <div class="search-box">
          <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input v-model="searchQuery" type="text" class="filter-input"
            :placeholder="`Tìm kiếm ${activeTabInfo.label.toLowerCase()} theo tên hoặc mã...`" />
        </div>

        <div class="status-filter">
          <label class="filter-lbl">Trạng thái:</label>
          <select v-model="statusFilter" class="filter-select">
            <option value="Tất cả">Tất cả trạng thái</option>
            <option value="Hoạt động">Hoạt động</option>
            <option value="Ngừng hoạt động">Ngừng hoạt động</option>
          </select>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-header">
        <div class="table-title">
          <h2>Danh sách {{ activeTabInfo.label }}</h2>
          <span class="count-pill">{{ currentTabItems.length }} mục</span>
        </div>
      </div>

      <div class="table-responsive">
        <table class="attr-table">
          <thead>
            <tr>
              <th width="60" class="text-center">STT</th>
              <th width="140">MÃ THUỘC TÍNH</th>
              <th width="240">TÊN {{ activeTabInfo.label.toUpperCase() }}</th>
              <th>MÔ TẢ / GIÁ TRỊ</th>
              <th width="160">TRẠNG THÁI</th>
              <th width="140" class="text-center">THAO TÁC</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in paginatedItems" :key="item.id">
              <td class="text-center font-bold text-slate">{{ getIndex(index) }}</td>
              <td>
                <span class="code-pill">{{ item.ma || (activeTabInfo.codePrefix + String(item.id).padStart(2, '0'))
                  }}</span>
              </td>
              <td>
                <div class="name-col">
                  <span class="name-main">{{ item.ten }}</span>
                </div>
              </td>
              <td>
                <div v-if="activeTab === 'mau_sac'" class="color-preview-cell">
                  <span class="color-circle"
                    :style="{ backgroundColor: (item.mo_ta && item.mo_ta.startsWith('#')) ? item.mo_ta : '#64748b', border: item.mo_ta === '#ffffff' ? '1px solid #cbd5e1' : 'none' }"></span>
                  <span class="color-hex">{{ item.mo_ta || '#64748b' }}</span>
                </div>
                <span v-else class="text-slate-desc">{{ item.mo_ta || '---' }}</span>
              </td>
              <td>
                <span class="badge-pill"
                  :class="{ 'badge-active': item.trang_thai === 'Hoạt động', 'badge-inactive': item.trang_thai === 'Ngừng hoạt động' }"
                  @click="toggleStatusFast(item)" title="Click để đổi trạng thái tức thì">
                  <span class="dot"></span>
                  {{ item.trang_thai }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-btns">
                  <button class="btn-action edit" title="Chỉnh sửa" @click="openEditModal(item)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9"></path>
                      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                    </svg>
                  </button>
                  <button class="btn-action del" title="Xóa thuộc tính" @click="confirmDeleteItem(item)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="currentTabItems.length === 0">
              <td colspan="6" class="empty-state">
                <p>Không có thuộc tính nào phù hợp với bộ lọc</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="currentTabItems.length > 0" class="pagination-footer">
        <div class="pagination-info">
          Hiển thị <strong>{{ (currentPage - 1) * pageSize + 1 }}</strong> - <strong>{{ Math.min(currentPage * pageSize,
            currentTabItems.length) }}</strong> trên tổng số <strong>{{ currentTabItems.length }}</strong> {{
              activeTabInfo.label.toLowerCase() }}
        </div>

        <div class="pagination-controls">
          <div class="page-size-selector">
            <label class="page-size-lbl">Hiển thị:</label>
            <select v-model="pageSize" class="page-size-select">
              <option v-for="opt in pageSizeOptions" :key="opt" :value="opt">{{ opt }} mục / trang</option>
            </select>
          </div>

          <div class="page-btn-group">
            <button class="page-nav-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
              ‹ Trước
            </button>
            <button v-for="p in totalPages" :key="p" class="page-num-btn" :class="{ active: currentPage === p }"
              @click="goToPage(p)">
              {{ p }}
            </button>
            <button class="page-nav-btn" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">
              Sau ›
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal thêm/sửa thuộc tính -->
    <transition name="fade">
      <div v-if="isModalOpen" class="modal-backdrop" @click.self="closeModal">
        <div class="modal-box">
          <div class="modal-header">
            <h3>{{ isEditing ? 'Chỉnh sửa' : 'Thêm mới' }} {{ activeTabInfo.label }}</h3>
            <button class="btn-close" @click="closeModal">✕</button>
          </div>

          <div class="modal-body">
            <div v-if="formError" class="form-error-alert">
              <span>⚠️ {{ formError }}</span>
            </div>

            <div class="form-group margin-bottom">
              <label class="form-lbl">Mã {{ activeTabInfo.label }} (Tự động)</label>
              <input v-model="modalForm.ma" type="text" class="form-inp disabled" readonly />
            </div>

            <div class="form-group margin-bottom">
              <label class="form-lbl">Tên {{ activeTabInfo.label }} <span class="required">*</span></label>
              <input v-model="modalForm.ten" type="text" class="form-inp"
                :placeholder="`Nhập tên ${activeTabInfo.label.toLowerCase()}...`" autofocus />
            </div>

            <div v-if="activeTab === 'mau_sac'" class="form-group margin-bottom">
              <label class="form-lbl">Mã màu HEX đại diện</label>
              <div class="color-input-row">
                <input v-model="modalForm.mo_ta" type="color" class="color-picker-box" />
                <input v-model="modalForm.mo_ta" type="text" class="form-inp" placeholder="#HEX color code" />
              </div>
            </div>

            <div v-else class="form-group margin-bottom">
              <label class="form-lbl">Mô tả / Ghi chú</label>
              <textarea v-model="modalForm.mo_ta" class="form-textarea" rows="3"
                placeholder="Nhập ghi chú hoặc mô tả chi tiết..."></textarea>
            </div>

            <div class="form-group">
              <label class="form-lbl">Trạng thái</label>
              <select v-model="modalForm.trang_thai" class="form-inp">
                <option value="Hoạt động">Hoạt động</option>
                <option value="Ngừng hoạt động">Ngừng hoạt động</option>
              </select>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="closeModal">Hủy bỏ</button>
            <button class="btn btn-primary" :disabled="isSubmitting" @click="saveAttributeDirectly">
              {{ isSubmitting ? 'Đang lưu...' : (isEditing ? 'Lưu thay đổi' : '+ Thêm thuộc tính') }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Confirmation Modal Dialog (Redesigned) -->
    <transition name="fade">
      <div v-if="confirmModal.isOpen" class="modal-backdrop modal-backdrop-blur" @click.self="handleCancelConfirm">
        <div class="confirm-modal-box">
          <button class="confirm-close-btn" @click="handleCancelConfirm" title="Đóng">✕</button>

          <div class="confirm-hero-badge" :class="`badge-${confirmModal.variant}`">
            <div class="confirm-icon-glow">
              <svg v-if="confirmModal.variant === 'danger'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                <line x1="10" y1="11" x2="10" y2="17"></line>
                <line x1="14" y1="11" x2="14" y2="17"></line>
              </svg>
              <svg v-else-if="confirmModal.variant === 'warning'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"></path>
              </svg>
              <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                <polyline points="17 21 17 13 7 13 7 21"></polyline>
                <polyline points="7 3 7 8 15 8"></polyline>
              </svg>
            </div>
          </div>

          <h3 class="confirm-title">{{ confirmModal.title }}</h3>
          <p class="confirm-message">{{ confirmModal.message }}</p>

          <div class="confirm-actions">
            <button class="btn-cancel" @click="handleCancelConfirm">
              {{ confirmModal.cancelText }}
            </button>
            <button class="btn-submit" :class="`btn-${confirmModal.variant}`" @click="handleConfirmAction">
              {{ confirmModal.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped src="@/assets/styles/AttributeCategoryPage.css"></style>
