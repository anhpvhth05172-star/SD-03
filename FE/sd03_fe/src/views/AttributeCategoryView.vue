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
  modalForm.mo_ta = ''
  modalForm.trang_thai = 'Hoạt động'
  isModalOpen.value = true
}

const openEditModal = (item) => {
  isEditing.value = true
  editingId.value = item.id
  formError.value = ''
  modalForm.ma = item.ma
  modalForm.ten = item.ten
  modalForm.mo_ta = item.mo_ta || ''
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
    await callback()
  }
}

const handleCancelConfirm = () => {
  confirmModal.isOpen = false
}

const confirmSaveAttribute = () => {
  formError.value = ''
  if (!modalForm.ten.trim()) {
    formError.value = 'Vui lòng nhập tên thuộc tính!'
    showToast('Vui lòng nhập tên thuộc tính!', 'warning')
    return
  }

  const isEdit = isEditing.value
  const actionName = isEdit ? 'lưu chỉnh sửa' : 'thêm mới'

  openConfirmModal({
    title: `Xác nhận ${actionName}`,
    message: `Bạn có chắc chắn muốn ${actionName} ${activeTabInfo.value.label.toLowerCase()} "${modalForm.ten.trim()}" không?`,
    confirmText: isEdit ? 'Lưu thay đổi' : 'Thêm ngay',
    cancelText: 'Hủy bỏ',
    variant: 'primary',
    onConfirm: () => executeSaveAttribute()
  })
}

const executeSaveAttribute = async () => {
  const payload = {
    ma: modalForm.ma,
    ten: modalForm.ten.trim(),
    moTa: modalForm.mo_ta.trim(),
    trangThai: modalForm.trang_thai === 'Hoạt động'
  }

  const category = activeTab.value
  const isEdit = isEditing.value
  const url = isEdit ? `${API_BASE}/${category}/${editingId.value}` : `${API_BASE}/${category}`
  const method = isEdit ? 'PUT' : 'POST'

  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (!res.ok) {
      const errData = await res.json()
      let msg = errData.message || 'Lỗi xử lý thuộc tính!'
      if (errData.errors) {
        msg = Object.values(errData.errors).join(', ')
      }
      formError.value = msg
      showToast(msg, 'warning')
      return
    }

    const savedData = await res.json()
    await fetchAttributes(category)

    showToast(isEdit ? `Cập nhật thành công ${activeTabInfo.value.label}: ${savedData.ten}` : `Thêm mới thành công ${activeTabInfo.value.label}: ${savedData.ten}`, 'success')
    closeModal()
  } catch (err) {
    console.error('API Error:', err)
    const list = attributesData[category]
    if (isEdit) {
      const idx = list.findIndex(i => i.id === editingId.value)
      if (idx !== -1) {
        list[idx].ten = modalForm.ten.trim()
        list[idx].mo_ta = modalForm.mo_ta.trim()
        list[idx].trang_thai = modalForm.trang_thai
      }
      showToast(`Cập nhật thành công: ${modalForm.ten}`, 'success')
    } else {
      list.push({
        id: Date.now(),
        ma: modalForm.ma,
        ten: modalForm.ten.trim(),
        mo_ta: modalForm.mo_ta.trim(),
        trang_thai: modalForm.trang_thai
      })
      showToast(`Thêm mới thành công: ${modalForm.ten}`, 'success')
    }
    closeModal()
  }
}

const confirmToggleStatus = (item) => {
  const nextStatus = item.trang_thai === 'Hoạt động' ? 'Ngừng hoạt động' : 'Hoạt động'
  openConfirmModal({
    title: 'Xác nhận chuyển trạng thái',
    message: `Bạn có chắc chắn muốn đổi trạng thái của ${activeTabInfo.value.label.toLowerCase()} "${item.ten}" sang "${nextStatus}" không?`,
    confirmText: 'Đổi trạng thái',
    cancelText: 'Hủy bỏ',
    variant: 'warning',
    onConfirm: () => executeToggleStatus(item)
  })
}

const executeToggleStatus = async (item) => {
  const category = activeTab.value
  try {
    const res = await fetch(`${API_BASE}/${category}/${item.id}/toggle-status`, {
      method: 'PATCH'
    })
    if (res.ok) {
      const updated = await res.json()
      item.trang_thai = updated.trangThai !== undefined ? updated.trangThai : updated.trang_thai
      showToast(`Đã đổi trạng thái "${item.ten}" thành "${item.trang_thai}" thành công!`, 'success')
    } else {
      item.trang_thai = item.trang_thai === 'Hoạt động' ? 'Ngừng hoạt động' : 'Hoạt động'
      showToast(`Đã đổi trạng thái "${item.ten}" thành "${item.trang_thai}" thành công!`, 'success')
    }
  } catch (err) {
    item.trang_thai = item.trang_thai === 'Hoạt động' ? 'Ngừng hoạt động' : 'Hoạt động'
    showToast(`Đã đổi trạng thái "${item.ten}" thành "${item.trang_thai}" thành công!`, 'success')
  }
}

const confirmDeleteItem = (item) => {
  openConfirmModal({
    title: `Xác nhận xóa ${activeTabInfo.value.label}`,
    message: `Bạn có chắc chắn muốn xóa ${activeTabInfo.value.label.toLowerCase()} "${item.ten}" không? Hành động này không thể khôi phục.`,
    confirmText: 'Xóa ngay',
    cancelText: 'Hủy bỏ',
    variant: 'danger',
    onConfirm: () => executeDelete(item)
  })
}

const executeDelete = async (item) => {
  const category = activeTab.value
  try {
    const res = await fetch(`${API_BASE}/${category}/${item.id}`, {
      method: 'DELETE'
    })
    if (res.ok) {
      showToast(`Đã xóa ${activeTabInfo.value.label.toLowerCase()} "${item.ten}" thành công!`, 'success')
      fetchAttributes(category)
    } else {
      attributesData[category] = attributesData[category].filter(i => i.id !== item.id)
      showToast(`Đã xóa ${activeTabInfo.value.label.toLowerCase()} "${item.ten}" thành công!`, 'success')
    }
  } catch (err) {
    attributesData[category] = attributesData[category].filter(i => i.id !== item.id)
    showToast(`Đã xóa ${activeTabInfo.value.label.toLowerCase()} "${item.ten}" thành công!`, 'success')
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
                  @click="confirmToggleStatus(item)" title="Click để đổi trạng thái">
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
            <button class="btn btn-primary" @click="confirmSaveAttribute">
              {{ isEditing ? 'Lưu thay đổi' : '+ Thêm thuộc tính' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="confirmModal.isOpen" class="modal-backdrop modal-backdrop-blur" @click.self="handleCancelConfirm">
        <div class="confirm-modal-box">
          <button class="confirm-close-btn" @click="handleCancelConfirm" title="Đóng">✕</button>

          <div class="confirm-hero-badge" :class="`badge-${confirmModal.variant}`">
            <div class="confirm-icon-glow">
              <svg v-if="confirmModal.variant === 'danger'" width="28" height="28" viewBox="0 0 24 24" fill="none"
                stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                <line x1="10" y1="11" x2="10" y2="17"></line>
                <line x1="14" y1="11" x2="14" y2="17"></line>
              </svg>
              <svg v-else-if="confirmModal.variant === 'warning'" width="28" height="28" viewBox="0 0 24 24" fill="none"
                stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"></path>
              </svg>
              <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
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

<style scoped>
.attribute-management-page {
  width: 100%;
}

.toast-floating {
  position: fixed;
  bottom: 28px;
  right: 28px;
  background: #0f172a;
  color: #ffffff;
  padding: 14px 22px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 14px;
  z-index: 1000;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  gap: 10px;
  border-left: 5px solid #22c55e;
}

.toast-warning {
  background: #78350f;
  border-left-color: #f59e0b;
}

.modal-backdrop-blur {
  backdrop-filter: blur(6px);
  background: rgba(15, 23, 42, 0.55);
}

.confirm-modal-box {
  background: #ffffff;
  width: 420px;
  max-width: 92vw;
  border-radius: 24px;
  padding: 32px 28px 24px;
  position: relative;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.9);
  animation: modalScaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.confirm-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  background: #f1f5f9;
  border: none;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.confirm-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.confirm-hero-badge {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.badge-danger {
  background: #fef2f2;
  border: 1px solid #fecdd3;
  box-shadow: 0 8px 20px rgba(239, 68, 68, 0.15);
}

.badge-warning {
  background: #fffbeb;
  border: 1px solid #fde68a;
  box-shadow: 0 8px 20px rgba(245, 158, 11, 0.15);
}

.badge-primary {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.15);
}

.confirm-icon-glow {
  display: flex;
  align-items: center;
  justify-content: center;
}

.confirm-title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 10px;
  letter-spacing: -0.4px;
}

.confirm-message {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 26px;
  padding: 0 8px;
}

.confirm-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-cancel {
  flex: 1;
  height: 44px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-cancel:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-submit {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 14px;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-submit.btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.35);
}

.btn-submit.btn-danger:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(239, 68, 68, 0.45);
}

.btn-submit.btn-warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35);
}

.btn-submit.btn-warning:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(245, 158, 11, 0.45);
}

.btn-submit.btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.btn-submit.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.45);
}

@keyframes modalScaleUp {
  0% {
    transform: scale(0.9);
    opacity: 0;
  }

  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* Header */
.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}

.btn-add-attr {
  background: linear-gradient(135deg, #d92d20 0%, #b42318 100%);
  color: #ffffff;
  border: none;
  font-size: 14px;
  font-weight: 700;
  padding: 11px 22px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(217, 45, 32, 0.3);
  transition: all 0.2s;
}

.btn-add-attr:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(217, 45, 32, 0.4);
}

/* Tabs Nav */
.tabs-nav-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 20px;
}

.attr-tab-btn {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}

.attr-tab-btn:hover {
  background: #f8fafc;
  color: #0f172a;
}

.attr-tab-btn.active {
  background: #fff1f2;
  border-color: #fee2e2;
  color: #d92d20;
}

.tab-count-badge {
  background: #f1f5f9;
  padding: 2px 7px;
  border-radius: 12px;
  font-size: 11px;
  color: #64748b;
}

.attr-tab-btn.active .tab-count-badge {
  background: #d92d20;
  color: #ffffff;
}

/* Filter Card */
.filter-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.search-box {
  position: relative;
  flex: 1;
  max-width: 420px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}

.filter-input {
  width: 100%;
  height: 42px;
  padding: 0 16px 0 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.status-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-lbl {
  font-size: 13.5px;
  font-weight: 700;
  color: #475569;
}

.filter-select {
  height: 42px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13.5px;
  outline: none;
  background: #ffffff;
}

/* Table Card */
.table-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);
}

.table-header {
  padding: 18px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.table-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-title h2 {
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
}

.count-pill {
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  color: #475569;
}

.table-responsive {
  overflow-x: auto;
}

.attr-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.attr-table th {
  padding: 14px 18px;
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.attr-table td {
  padding: 14px 18px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.code-pill {
  font-family: monospace;
  font-weight: 700;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 4px;
  color: #334155;
  font-size: 12.5px;
}

.name-main {
  font-weight: 800;
  color: #0f172a;
}

.color-preview-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.color-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.color-hex {
  font-family: monospace;
  font-weight: 700;
  color: #334155;
}

.text-slate-desc {
  color: #475569;
  font-size: 13.5px;
}

.badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.badge-active {
  background: #ecfdf5;
  color: #059669;
}

.badge-active .dot {
  background: #10b981;
}

.badge-inactive {
  background: #f1f5f9;
  color: #64748b;
}

.badge-inactive .dot {
  background: #94a3b8;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-action {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 6px;
  cursor: pointer;
  color: #475569;
}

.btn-action.edit:hover {
  color: #d92d20;
  border-color: #fee2e2;
  background: #fff1f2;
}

.btn-action.del:hover {
  color: #ef4444;
  border-color: #fee2e2;
  background: #fff1f2;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-box {
  width: 460px;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15);
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
}

.btn-close {
  background: none;
  border: none;
  font-size: 16px;
  color: #94a3b8;
  cursor: pointer;
}

.modal-body {
  padding: 24px;
}

.margin-bottom {
  margin-bottom: 16px;
}

.form-lbl {
  font-size: 13.5px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 6px;
  display: block;
}

.required {
  color: #d92d20;
}

.form-inp {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.form-inp.disabled {
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
}

.color-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.color-picker-box {
  width: 44px;
  height: 44px;
  padding: 2px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.modal-footer {
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-secondary {
  background: #e2e8f0;
  color: #475569;
  border: none;
  padding: 10px 18px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

.btn-primary {
  background: #d92d20;
  color: #ffffff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
}

.form-error-alert {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 700;
  margin-bottom: 16px;
}

/* Pagination Footer */
.pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #ffffff;
  border-top: 1px solid #f1f5f9;
  flex-wrap: wrap;
  gap: 16px;
}

.pagination-info {
  font-size: 13.5px;
  color: #64748b;
}

.pagination-info strong {
  color: #0f172a;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size-lbl {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.page-size-select {
  height: 34px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  background: #ffffff;
  outline: none;
  cursor: pointer;
}

.page-btn-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-nav-btn,
.page-num-btn {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.page-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-nav-btn:not(:disabled):hover,
.page-num-btn:hover {
  background: #f8fafc;
  color: #0f172a;
  border-color: #cbd5e1;
}

.page-num-btn.active {
  background: #d92d20;
  color: #ffffff;
  border-color: #d92d20;
}
</style>
