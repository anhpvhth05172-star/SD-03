<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import QuickAddModal from '../components/QuickAddModal.vue'

const router = useRouter()

const form = reactive({
  code: 'SP-NK-2026',
  name: '',
  brand: '',
  category: '',
  gender: 'all',
  material: '',
  sole: '',
  collar: '',
  cushion: '',
  weight: ''
})

const brandOptions = [
  { value: 'nike', label: 'Nike' },
  { value: 'adidas', label: 'Adidas' },
  { value: 'puma', label: 'Puma' },
  { value: 'newbalance', label: 'New Balance' },
  { value: 'converse', label: 'Converse' },
  { value: 'vans', label: 'Vans' },
  { value: 'asics', label: 'Asics' }
]

const categoryOptions = [
  { value: 'sneaker', label: 'Sneaker' },
  { value: 'running', label: 'Giày chạy bộ' },
  { value: 'sport', label: 'Giày thể thao' },
  { value: 'basketball', label: 'Giày bóng rổ' },
  { value: 'high_top', label: 'Giày cổ cao' },
  { value: 'skate', label: 'Giày trượt ván' }
]

const genderOptions = [
  { value: 'all', label: 'Tất cả' },
  { value: 'male', label: 'Nam' },
  { value: 'female', label: 'Nữ' },
  { value: 'unisex', label: 'Unisex' }
]

const materialOptions = [
  { value: 'mesh', label: 'Vải dệt (Mesh)' },
  { value: 'leather', label: 'Da thật (Leather)' },
  { value: 'canvas', label: 'Vải Canvas' },
  { value: 'suede', label: 'Da lộn (Suede)' },
  { value: 'synthetic', label: 'Da nhân tạo (Synthetic)' },
  { value: 'knit', label: 'Primeknit / Flyknit' }
]

const soleOptions = [
  { value: 'rubber', label: 'Đế cao su (Rubber)' },
  { value: 'boost', label: 'Đế Boost' },
  { value: 'eva', label: 'Đế EVA' },
  { value: 'phylon', label: 'Đế Phylon' },
  { value: 'air', label: 'Đế đệm khí (Air)' }
]

const collarOptions = [
  { value: 'low', label: 'Cổ thấp (Low-top)' },
  { value: 'mid', label: 'Cổ lửng (Mid-top)' },
  { value: 'high', label: 'Cổ cao (High-top)' }
]

const cushionOptions = [
  { value: 'air', label: 'Nike Air / Air Max' },
  { value: 'react', label: 'React Foam' },
  { value: 'boost', label: 'Ultraboost' },
  { value: 'cloudfoam', label: 'Cloudfoam' },
  { value: 'zoom', label: 'Zoom Air' },
  { value: 'ortholite', label: 'Lót đệm OrthoLite' }
]

const weightOptions = [
  { value: 'light', label: '< 250g (Siêu nhẹ)' },
  { value: 'normal', label: '250g - 350g (Tiêu chuẩn)' },
  { value: 'heavy', label: '> 350g (Đầm chân)' }
]

const availableColors = ref([
  { id: 1, name: 'Đen', hex: '#111827' },
  { id: 2, name: 'Trắng', hex: '#ffffff' },
  { id: 3, name: 'Đỏ', hex: '#ef4444' },
  { id: 4, name: 'Xanh dương', hex: '#3b82f6' },
  { id: 5, name: 'Xám', hex: '#64748b' },
  { id: 6, name: 'Xanh lá', hex: '#10b981' }
])

const availableSizes = ref([
  { id: 1, name: '38' },
  { id: 2, name: '39' },
  { id: 3, name: '40' },
  { id: 4, name: '41' },
  { id: 5, name: '42' },
  { id: 6, name: '43' },
  { id: 7, name: '44' }
])

const selectedColors = ref([availableColors.value[0], availableColors.value[1]])
const selectedSizes = ref([availableSizes.value[1], availableSizes.value[2], availableSizes.value[3]])

const tempColorSelect = ref('')
const tempSizeSelect = ref('')

const handleSelectColor = (e) => {
  const col = availableColors.value.find(c => c.id === Number(e.target.value))
  if (col && !selectedColors.value.some(c => c.id === col.id)) {
    selectedColors.value.push(col)
  }
  tempColorSelect.value = ''
}

const removeColor = (id) => {
  selectedColors.value = selectedColors.value.filter(c => c.id !== id)
}

const handleSelectSize = (e) => {
  const sz = availableSizes.value.find(s => s.id === Number(e.target.value))
  if (sz && !selectedSizes.value.some(s => s.id === sz.id)) {
    selectedSizes.value.push(sz)
  }
  tempSizeSelect.value = ''
}

const removeSize = (id) => {
  selectedSizes.value = selectedSizes.value.filter(s => s.id !== id)
}

const isModalOpen = ref(false)
const modalType = ref('color')

const openQuickAdd = (type) => {
  modalType.value = type
  isModalOpen.value = true
}

const handleAttributeAdded = (item) => {
  if (item.type === 'color') {
    const newColor = {
      id: Date.now(),
      name: item.name,
      hex: item.code || '#ef4444'
    }
    availableColors.value.push(newColor)
    selectedColors.value.push(newColor)
    showToast(`Đã thêm màu sắc mới: ${item.name}`)
  } else {
    const newSize = {
      id: Date.now(),
      name: item.name
    }
    availableSizes.value.push(newSize)
    selectedSizes.value.push(newSize)
    showToast(`Đã thêm kích cỡ mới: ${item.name}`)
  }
}

const variants = ref([])
const hasGeneratedVariants = ref(false)
const bulkPrice = ref(2500000)
const bulkStock = ref(50)

const generateVariants = () => {
  if (selectedColors.value.length === 0 || selectedSizes.value.length === 0) {
    showToast('Vui lòng chọn ít nhất 1 màu sắc và 1 kích cỡ để tạo biến thể!', 'warning')
    return
  }

  const generated = []
  selectedColors.value.forEach(color => {
    selectedSizes.value.forEach(size => {
      generated.push({
        id: `${color.id}-${size.id}`,
        color: color.name,
        colorHex: color.hex,
        size: size.name,
        sku: `${form.code || 'SP'}-${color.name.toUpperCase().substring(0, 3)}-${size.name}`,
        price: bulkPrice.value || 2500000,
        stock: bulkStock.value || 50,
        status: 'Đang bán'
      })
    })
  })

  variants.value = generated
  hasGeneratedVariants.value = true
  showToast(`Đã tạo thành công ${generated.length} biến thể tự động!`)

  setTimeout(() => {
    const el = document.getElementById('variants-section')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }, 100)
}

const applyBulkPrice = () => {
  if (!bulkPrice.value) return
  variants.value.forEach(v => {
    v.price = bulkPrice.value
  })
  showToast(`Đã cập nhật giá bán cho ${variants.value.length} biến thể!`)
}

const applyBulkStock = () => {
  if (bulkStock.value === undefined || bulkStock.value === null) return
  variants.value.forEach(v => {
    v.stock = bulkStock.value
  })
  showToast(`Đã cập nhật số lượng tồn kho cho ${variants.value.length} biến thể!`)
}

const removeVariant = (id) => {
  variants.value = variants.value.filter(v => v.id !== id)
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN').format(val) + ' đ'
}

const toast = reactive({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message, type = 'success') => {
  toast.message = message
  toast.type = type
  toast.show = true
  setTimeout(() => {
    toast.show = false
  }, 3500)
}

const goBack = () => {
  router.push('/san-pham')
}

const saveProduct = () => {
  if (!form.name.trim()) {
    showToast('Vui lòng nhập tên sản phẩm!', 'warning')
    return
  }
  showToast('Đã lưu thông tin sản phẩm và các biến thể thành công!')
  setTimeout(() => {
    router.push('/san-pham')
  }, 1200)
}
</script>

<template>
  <div class="product-create-page">
    <!-- Header Row -->
    <div class="page-header-row">
      <div class="page-title-box">
        <h1 class="page-title">Thêm sản phẩm mới</h1>
        <p class="page-subtitle">Nhập thuộc tính sản phẩm và hệ thống sẽ tự động tạo danh sách biến thể SKU.</p>
      </div>

      <div class="page-actions-box">
        <button class="btn btn-outline-cancel" @click="goBack">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          Quay lại
        </button>
        <button class="btn btn-generate-hdr" @click="generateVariants">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
          Tạo biến thể tự động
        </button>
        <button class="btn btn-save-primary" @click="saveProduct">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
            <polyline points="17 21 17 13 7 13 7 21"></polyline>
            <polyline points="7 3 7 8 15 8"></polyline>
          </svg>
          Lưu sản phẩm
        </button>
      </div>
    </div>

    <section class="card-box">
      <div class="card-header-title">
        <span class="header-icon-circle">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z" />
          </svg>
        </span>
        <h2>1. Thông tin cơ bản sản phẩm</h2>
      </div>

      <div class="form-grid-2col margin-top-md">
        <div class="form-group">
          <label class="field-label">Mã sản phẩm</label>
          <input v-model="form.code" type="text" class="field-input disabled-input" readonly />
        </div>

        <div class="form-group">
          <label class="field-label">Tên sản phẩm <span class="required-star">*</span></label>
          <input v-model="form.name" type="text" class="field-input" placeholder="Ví dụ: Nike Air Max 270..." />
        </div>

        <div class="form-group">
          <label class="field-label">Thương hiệu <span class="required-star">*</span></label>
          <select v-model="form.brand" class="field-select">
            <option value="" disabled selected>Chọn thương hiệu...</option>
            <option v-for="item in brandOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="field-label">Loại giày <span class="required-star">*</span></label>
          <select v-model="form.category" class="field-select">
            <option value="" disabled selected>Chọn loại giày...</option>
            <option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="field-label">Giới tính <span class="required-star">*</span></label>
          <select v-model="form.gender" class="field-select">
            <option v-for="item in genderOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="field-label">Chất liệu <span class="required-star">*</span></label>
          <select v-model="form.material" class="field-select">
            <option value="" disabled selected>Chọn chất liệu...</option>
            <option v-for="item in materialOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="field-label">Đế giày <span class="required-star">*</span></label>
          <select v-model="form.sole" class="field-select">
            <option value="" disabled selected>Chọn đế giày...</option>
            <option v-for="item in soleOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="field-label">Cổ giày <span class="required-star">*</span></label>
          <select v-model="form.collar" class="field-select">
            <option value="" disabled selected>Chọn cổ giày...</option>
            <option v-for="item in collarOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="field-label">Công nghệ đệm <span class="required-star">*</span></label>
          <select v-model="form.cushion" class="field-select">
            <option value="" disabled selected>Chọn công nghệ đệm...</option>
            <option v-for="item in cushionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="field-label">Trọng lượng <span class="required-star">*</span></label>
          <select v-model="form.weight" class="field-select">
            <option value="" disabled selected>Chọn trọng lượng...</option>
            <option v-for="item in weightOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
      </div>
    </section>

    <section class="card-box margin-top-lg">
      <div class="card-header-title">
        <span class="header-icon-circle">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M12 3c-4.97 0-9 4.03-9 9 0 2.12.74 4.07 1.97 5.61L4.35 19.4c-.39.39-.39 1.02 0 1.41.39.39 1.02.39 1.41 0l1.9-1.9C9.22 19.53 10.57 20 12 20c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-4 9c-.83 0-1.5-.67-1.5-1.5S7.17 9 8 9s1.5.67 1.5 1.5S8.83 12 8 12zm4 0c-.83 0-1.5-.67-1.5-1.5S11.17 9 12 9s1.5.67 1.5 1.5S12.83 12 12 12zm4 0c-.83 0-1.5-.67-1.5-1.5S15.17 9 16 9s1.5.67 1.5 1.5S16.83 12 16 12z" />
          </svg>
        </span>
        <h2>2. Thuộc tính biến thể (Màu sắc & Kích cỡ)</h2>
      </div>

      <div class="attribute-selection-row margin-top-md">
        <div class="attr-label-col">
          <span class="field-label">Màu sắc <span class="required-star">*</span></span>
        </div>
        <div class="attr-content-col">
          <div class="flex-align-center gap-10">
            <select v-model="tempColorSelect" class="field-select attr-dropdown" @change="handleSelectColor">
              <option value="" disabled selected>Chọn màu sắc...</option>
              <option v-for="color in availableColors" :key="color.id" :value="color.id">{{ color.name }}</option>
            </select>
            <button class="btn btn-quick-add" @click="openQuickAdd('color')">
              <span class="plus-sign">+</span> Thêm màu mới
            </button>
          </div>

          <!-- Color Tags -->
          <div v-if="selectedColors.length > 0" class="tags-container margin-top-sm">
            <div v-for="col in selectedColors" :key="col.id" class="chip-tag color-chip">
              <span class="color-dot-swatch"
                :style="{ backgroundColor: col.hex, border: col.hex === '#ffffff' ? '1px solid #cbd5e1' : 'none' }"></span>
              <span class="chip-text">{{ col.name }}</span>
              <button class="chip-remove-btn" @click="removeColor(col.id)">&times;</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Row Kích Cỡ -->
      <div class="attribute-selection-row margin-top-md">
        <div class="attr-label-col">
          <span class="field-label">Kích cỡ (Size) <span class="required-star">*</span></span>
        </div>
        <div class="attr-content-col">
          <div class="flex-align-center gap-10">
            <select v-model="tempSizeSelect" class="field-select attr-dropdown" @change="handleSelectSize">
              <option value="" disabled selected>Chọn size...</option>
              <option v-for="sz in availableSizes" :key="sz.id" :value="sz.id">Size {{ sz.name }}</option>
            </select>
            <button class="btn btn-quick-add" @click="openQuickAdd('size')">
              <span class="plus-sign">+</span> Thêm size mới
            </button>
          </div>

          <!-- Size Tags -->
          <div v-if="selectedSizes.length > 0" class="tags-container margin-top-sm">
            <div v-for="sz in selectedSizes" :key="sz.id" class="chip-tag size-chip">
              <span class="chip-text">Size {{ sz.name }}</span>
              <button class="chip-remove-btn" @click="removeSize(sz.id)">&times;</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Button Generate -->
      <div class="generate-btn-banner margin-top-lg">
        <button class="btn btn-hero-generate" @click="generateVariants">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
          Tự động sinh danh sách {{ selectedColors.length * selectedSizes.length }} biến thể (SKU)
        </button>
      </div>
    </section>

    <!-- Section 3: Bảng danh sách biến thể tự động -->
    <section v-if="hasGeneratedVariants" id="variants-section" class="card-box margin-top-lg">
      <div class="card-header flex-align-center justify-between">
        <div class="card-header-title">
          <span class="header-icon-circle">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z" />
            </svg>
          </span>
          <h2>3. Danh sách biến thể tự động</h2>
          <span class="count-pill">{{ variants.length }} biến thể</span>
        </div>

        <!-- Bulk Controls Bar -->
        <div class="bulk-bar">
          <div class="bulk-group">
            <span class="bulk-label">Giá chung:</span>
            <input v-model.number="bulkPrice" type="number" class="bulk-input" step="10000" />
            <button class="btn btn-apply-bulk" @click="applyBulkPrice">Áp dụng giá</button>
          </div>
          <div class="bulk-group">
            <span class="bulk-label">SL chung:</span>
            <input v-model.number="bulkStock" type="number" class="bulk-input" min="0" />
            <button class="btn btn-apply-bulk" @click="applyBulkStock">Áp dụng SL</button>
          </div>
        </div>
      </div>

      <!-- Variants Table -->
      <div class="table-responsive margin-top-md">
        <table class="variants-table">
          <thead>
            <tr>
              <th width="50" class="text-center">STT</th>
              <th width="140">MÀU SẮC</th>
              <th width="100">KÍCH CỠ</th>
              <th width="200">MÃ SKU</th>
              <th width="180">GIÁ BÁN (VNĐ)</th>
              <th width="140" class="text-center">SỐ LƯỢNG KHO</th>
              <th width="130">TRẠNG THÁI</th>
              <th width="70" class="text-center">XÓA</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(v, index) in variants" :key="v.id">
              <td class="text-center font-bold text-muted">{{ index + 1 }}</td>
              <td>
                <div class="color-cell">
                  <span class="color-dot-swatch"
                    :style="{ backgroundColor: v.colorHex, border: v.colorHex === '#ffffff' ? '1px solid #cbd5e1' : 'none' }"></span>
                  <span class="font-semibold">{{ v.color }}</span>
                </div>
              </td>
              <td>
                <span class="size-badge">Size {{ v.size }}</span>
              </td>
              <td>
                <input v-model="v.sku" type="text" class="cell-input code-font" />
              </td>
              <td>
                <input v-model.number="v.price" type="number" class="cell-input price-font" />
              </td>
              <td>
                <input v-model.number="v.stock" type="number" class="cell-input stock-font text-center" min="0" />
              </td>
              <td>
                <span class="badge-status green">
                  <span class="status-dot"></span> Đang bán
                </span>
              </td>
              <td class="text-center">
                <button class="btn-icon-del" @click="removeVariant(v.id)" title="Xóa biến thể">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Save Actions Footer -->
      <div class="card-footer-actions margin-top-lg">
        <button class="btn btn-outline-cancel" @click="goBack">Hủy bỏ</button>
        <button class="btn btn-save-primary btn-lg" @click="saveProduct">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
            <polyline points="17 21 17 13 7 13 7 21"></polyline>
            <polyline points="7 3 7 8 15 8"></polyline>
          </svg>
          Lưu tất cả sản phẩm & biến thể
        </button>
      </div>
    </section>

    <!-- Modals & Toasts -->
    <QuickAddModal :is-open="isModalOpen" :type="modalType" @close="isModalOpen = false" @add="handleAttributeAdded" />

    <transition name="toast-fade">
      <div v-if="toast.show" class="toast-card" :class="'toast-' + toast.type">
        <span class="toast-icon">✓</span>
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.product-create-page {
  width: 100%;
}

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
  font-size: 13.5px;
  color: #64748b;
  margin-top: 4px;
}

.page-actions-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-outline-cancel {
  background: #ffffff;
  border: 1px solid #fee2e2;
  color: #d92d20;
  font-size: 13.5px;
  font-weight: 600;
  padding: 9px 18px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-outline-cancel:hover {
  background-color: #fff1f2;
  border-color: #fca5a5;
}

.btn-generate-hdr {
  background: #fff1f2;
  border: 1px solid #fee2e2;
  color: #d92d20;
  font-size: 13.5px;
  font-weight: 700;
  padding: 9px 18px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
}

.btn-save-primary {
  background-color: var(--primary);
  color: #ffffff;
  border: none;
  font-size: 13.5px;
  font-weight: 700;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(217, 45, 32, 0.25);
  transition: all 0.15s;
}

.btn-save-primary:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}

/* Card Box */
.card-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header-title h2 {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.header-icon-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #fee2e2;
  color: #d92d20;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.required-star {
  color: #d92d20;
}

.field-input,
.field-select {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  color: #0f172a;
  outline: none;
  background: #ffffff;
  width: 100%;
}

.disabled-input {
  background-color: #f8fafc;
  color: #64748b;
  font-weight: 600;
}

.field-input:focus,
.field-select:focus {
  border-color: #d92d20;
}

/* Attribute Selection Row */
.attribute-selection-row {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.attr-label-col {
  width: 140px;
  padding-top: 10px;
}

.attr-content-col {
  flex: 1;
}

.attr-dropdown {
  width: 240px;
}

.btn-quick-add {
  background: #ffffff;
  border: 1px dashed #d92d20;
  color: #d92d20;
  font-weight: 600;
  font-size: 13px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.btn-quick-add:hover {
  background: #fff1f2;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.color-dot-swatch {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.chip-remove-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
}

.chip-remove-btn:hover {
  color: #d92d20;
}

/* Hero Generate Banner */
.generate-btn-banner {
  background: #fafafa;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: center;
}

.btn-hero-generate {
  background: #d92d20;
  color: #ffffff;
  border: none;
  font-size: 14.5px;
  font-weight: 700;
  padding: 12px 28px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(217, 45, 32, 0.3);
  transition: all 0.15s;
}

.btn-hero-generate:hover {
  background-color: #b42318;
  transform: translateY(-1px);
}

/* Bulk Bar */
.bulk-bar {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bulk-group {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.bulk-input {
  width: 110px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
}

.btn-apply-bulk {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  padding: 7px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.btn-apply-bulk:hover {
  background: #e2e8f0;
}

/* Table */
.table-responsive {
  overflow-x: auto;
}

.variants-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}

.variants-table th {
  padding: 12px 14px;
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
  border-bottom: 1px solid #e2e8f0;
}

.variants-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.color-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.size-badge {
  padding: 4px 10px;
  background: #f1f5f9;
  border-radius: 6px;
  font-weight: 700;
  font-size: 12.5px;
  color: #334155;
}

.cell-input {
  height: 34px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  width: 100%;
  font-size: 13px;
}

.code-font {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
}

.price-font {
  font-weight: 700;
  color: #d92d20;
}

.badge-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.badge-status.green {
  background: #ecfdf5;
  color: #059669;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
}

.btn-icon-del {
  background: none;
  border: 1px solid #fee2e2;
  border-radius: 6px;
  padding: 5px;
  color: #d92d20;
  cursor: pointer;
}

.btn-icon-del:hover {
  background: #fff1f2;
}

.card-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-lg {
  padding: 12px 24px;
  font-size: 14px;
}

.margin-top-md {
  margin-top: 16px;
}

.margin-top-lg {
  margin-top: 24px;
}

.margin-top-sm {
  margin-top: 10px;
}

.flex-align-center {
  display: flex;
  align-items: center;
}

.gap-10 {
  gap: 10px;
}

.justify-between {
  justify-content: space-between;
}

.count-pill {
  padding: 3px 10px;
  background: #fff1f2;
  color: #d92d20;
  font-size: 12px;
  font-weight: 700;
  border-radius: 20px;
  border: 1px solid #fee2e2;
}

.toast-card {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: #0f172a;
  color: #ffffff;
  padding: 12px 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  font-size: 13.5px;
  font-weight: 600;
}

.toast-warning {
  background: #d97706;
}
</style>
