<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import QuickAddModal from '../components/QuickAddModal.vue'

const router = useRouter()

const form = reactive({
  code: 'G66748',
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
  { id: 2, name: 'Trắng', hex: '#f8fafc' },
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
const selectedSizes = ref([availableSizes.value[2], availableSizes.value[3], availableSizes.value[4]])

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
    <div class="page-action-row">
      <button class="btn-back" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
          stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
        <span>Quay lại danh sách</span>
      </button>
    </div>

    <section class="form-card">
      <div class="form-grid">
        <div class="form-group">
          <label class="form-label">Mã sản phẩm</label>
          <input v-model="form.code" type="text" class="form-input disabled" readonly />
        </div>

        <div class="form-group">
          <label class="form-label">
            Sản phẩm <span class="required-star">*</span>
          </label>
          <input v-model="form.name" type="text" class="form-input" placeholder="Nhập tên sản phẩm..." />
        </div>

        <div class="form-group">
          <label class="form-label">
            Thương hiệu <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.brand" class="form-select">
              <option value="" disabled selected>Chọn thương hiệu...</option>
              <option v-for="item in brandOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            Loại giày <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.category" class="form-select">
              <option value="" disabled selected>Chọn loại giày...</option>
              <option v-for="item in categoryOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            Giới tính <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.gender" class="form-select">
              <option v-for="item in genderOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            Chất liệu <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.material" class="form-select">
              <option value="" disabled selected>Chọn chất liệu giày...</option>
              <option v-for="item in materialOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            Đế giày <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.sole" class="form-select">
              <option value="" disabled selected>Chọn đế giày...</option>
              <option v-for="item in soleOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            Cổ giày <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.collar" class="form-select">
              <option value="" disabled selected>Chọn cổ giày...</option>
              <option v-for="item in collarOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            Công nghệ đệm <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.cushion" class="form-select">
              <option value="" disabled selected>Chọn công nghệ đệm...</option>
              <option v-for="item in cushionOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">
            Trọng lượng <span class="required-star">*</span>
          </label>
          <div class="select-wrapper">
            <select v-model="form.weight" class="form-select">
              <option value="" disabled selected>Chọn trọng lượng...</option>
              <option v-for="item in weightOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
        </div>
      </div>
    </section>
    <section class="form-card">
      <div class="attribute-row">
        <div class="attribute-label">
          Màu sắc <span class="required-star">*</span>
        </div>
        <div class="attribute-input-area">
          <div class="select-wrapper">
            <select v-model="tempColorSelect" class="form-select attribute-select" @change="handleSelectColor">
              <option value="" disabled selected>Chọn màu</option>
              <option v-for="color in availableColors" :key="color.id" :value="color.id">
                {{ color.name }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>

          <div v-if="selectedColors.length > 0" class="selected-tags-wrap">
            <span v-for="col in selectedColors" :key="col.id" class="attribute-tag">
              <span class="color-dot" :style="{ backgroundColor: col.hex }"></span>
              <span>{{ col.name }}</span>
              <button class="btn-tag-remove" @click="removeColor(col.id)" title="Xóa màu này">
                &times;
              </button>
            </span>
          </div>
        </div>

        <button class="btn-add-fast" @click="openQuickAdd('color')">
          <span class="plus-icon">+</span>
          <span>Thêm nhanh</span>
        </button>
      </div>

      <div class="attribute-row">
        <div class="attribute-label">
          Kích cỡ <span class="required-star">*</span>
        </div>
        <div class="attribute-input-area">
          <div class="select-wrapper">
            <select v-model="tempSizeSelect" class="form-select attribute-select" @change="handleSelectSize">
              <option value="" disabled selected>Chọn size</option>
              <option v-for="sz in availableSizes" :key="sz.id" :value="sz.id">
                {{ sz.name }}
              </option>
            </select>
            <div class="select-chevron">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>

          <div v-if="selectedSizes.length > 0" class="selected-tags-wrap">
            <span v-for="sz in selectedSizes" :key="sz.id" class="attribute-tag size-tag">
              <span>Size {{ sz.name }}</span>
              <button class="btn-tag-remove" @click="removeSize(sz.id)" title="Xóa size này">
                &times;
              </button>
            </span>
          </div>
        </div>

        <button class="btn-add-fast" @click="openQuickAdd('size')">
          <span class="plus-icon">+</span>
          <span>Thêm nhanh</span>
        </button>
      </div>
    </section>

    <section v-if="hasGeneratedVariants" id="variants-section" class="form-card variants-table-card">
      <div class="variants-header">
        <div class="variants-header-left">
          <h3 class="variants-title">Danh sách biến thể tạo tự động</h3>
          <span class="badge-count-pill">{{ variants.length }} biến thể</span>
        </div>

        <div class="bulk-controls">
          <div class="bulk-item">
            <span class="bulk-label">Giá chung:</span>
            <input v-model.number="bulkPrice" type="number" class="bulk-input" step="10000" />
            <button class="btn-apply-bulk" @click="applyBulkPrice">Áp dụng giá</button>
          </div>
          <div class="bulk-item">
            <span class="bulk-label">SL chung:</span>
            <input v-model.number="bulkStock" type="number" class="bulk-input" min="0" />
            <button class="btn-apply-bulk" @click="applyBulkStock">Áp dụng SL</button>
          </div>
        </div>
      </div>

      <div class="table-responsive">
        <table class="variants-table">
          <thead>
            <tr>
              <th width="40">STT</th>
              <th width="140">Màu sắc</th>
              <th width="100">Kích cỡ</th>
              <th width="200">Mã SKU</th>
              <th width="180">Giá bán (VNĐ)</th>
              <th width="140">Số lượng tồn</th>
              <th width="140">Trạng thái</th>
              <th width="80" class="text-center">Xóa</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(v, index) in variants" :key="v.id">
              <td>{{ index + 1 }}</td>
              <td>
                <div class="variant-color-badge">
                  <span class="color-dot" :style="{ backgroundColor: v.colorHex }"></span>
                  <span class="font-semibold">{{ v.color }}</span>
                </div>
              </td>
              <td>
                <span class="size-pill">{{ v.size }}</span>
              </td>
              <td>
                <input v-model="v.sku" type="text" class="table-cell-input" />
              </td>
              <td>
                <input v-model.number="v.price" type="number" class="table-cell-input price-cell-input" />
              </td>
              <td>
                <input v-model.number="v.stock" type="number" class="table-cell-input stock-cell-input" min="0" />
              </td>
              <td>
                <span class="badge badge-success">
                  <span class="status-dot"></span> Đang bán
                </span>
              </td>
              <td class="text-center">
                <button class="btn-delete-row" title="Xóa biến thể" @click="removeVariant(v.id)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="variants-footer-actions">
        <button class="btn btn-secondary" @click="goBack">Hủy bỏ</button>
        <button class="btn btn-primary btn-save-large" @click="saveProduct">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
            <polyline points="17 21 17 13 7 13 7 21"></polyline>
            <polyline points="7 3 7 8 15 8"></polyline>
          </svg>
          Lưu thông tin sản phẩm
        </button>
      </div>
    </section>

    <div class="floating-action-bar">
      <button class="btn-generate-variants" @click="generateVariants">
        Tạo biến thể tự động
      </button>

      <button class="floating-bot-btn" title="Trợ lý tạo sản phẩm">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="10" rx="2"></rect>
          <circle cx="12" cy="5" r="2"></circle>
          <path d="M12 7v4"></path>
          <line x1="8" y1="16" x2="8.01" y2="16"></line>
          <line x1="16" y1="16" x2="16.01" y2="16"></line>
        </svg>
      </button>
    </div>

    <QuickAddModal :is-open="isModalOpen" :type="modalType" @close="isModalOpen = false" @add="handleAttributeAdded" />

    <transition name="toast-slide">
      <div v-if="toast.show" class="toast-notification" :class="'toast-' + toast.type">
        <span class="toast-icon">
          <svg v-if="toast.type === 'success'" width="18" height="18" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.product-create-page {
  width: 100%;
  position: relative;
}

.page-action-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background-color: #ffffff;
  border: 1px solid #fee2e2;
  color: var(--primary);
  border-radius: var(--radius-md);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(217, 45, 32, 0.04);
  transition: all 0.15s ease;
  user-select: none;
}

.btn-back:hover {
  background-color: #fff1f2;
  border-color: #fca5a5;
  transform: translateX(-2px);
}

.form-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 24px;
  row-gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
}

.required-star {
  color: var(--primary);
  margin-left: 3px;
  font-weight: 700;
}

.form-input,
.form-select {
  height: 42px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  font-family: inherit;
  font-size: 13.5px;
  color: #1e293b;
  background-color: #ffffff;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  width: 100%;
}

.form-input:focus,
.form-select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(217, 45, 32, 0.12);
}

.form-input::placeholder {
  color: #94a3b8;
}

.form-input.disabled {
  background-color: #f8fafc;
  color: #0f172a;
  font-weight: 600;
  cursor: not-allowed;
  border-color: #e2e8f0;
}

.select-wrapper {
  position: relative;
  width: 100%;
}

.select-wrapper select {
  appearance: none;
  -webkit-appearance: none;
  padding-right: 36px;
  cursor: pointer;
}

.select-chevron {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  color: #94a3b8;
  display: flex;
  align-items: center;
}

.attribute-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.attribute-row:last-child {
  margin-bottom: 0;
}

.attribute-label {
  width: 95px;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  flex-shrink: 0;
  padding-top: 10px;
}

.attribute-input-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attribute-select {
  height: 42px;
}

.btn-add-fast {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  background: #ffffff;
  border: 1px solid #fee2e2;
  color: var(--primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;
  height: 42px;
}

.btn-add-fast:hover {
  background-color: #fff1f2;
  border-color: #fca5a5;
  transform: translateY(-1px);
}

.plus-icon {
  font-size: 16px;
  font-weight: 700;
}

.selected-tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.attribute-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px 5px 12px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.size-tag {
  background-color: #fff1f2;
  border-color: #fecdd3;
  color: var(--primary);
}

.color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid #cbd5e1;
  display: inline-block;
}

.btn-tag-remove {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px 4px;
  border-radius: 50%;
  transition: color 0.15s;
}

.btn-tag-remove:hover {
  color: var(--primary);
}

.floating-action-bar {
  position: fixed;
  bottom: 24px;
  right: 32px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 50;
}

.btn-generate-variants {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: var(--primary);
  color: #ffffff;
  border: none;
  padding: 12px 24px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(217, 45, 32, 0.4);
  transition: all 0.15s ease;
  user-select: none;
}

.btn-generate-variants:hover {
  background-color: var(--primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(217, 45, 32, 0.5);
}

.btn-generate-variants:active {
  transform: translateY(0);
}

.floating-bot-btn {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background-color: var(--primary);
  color: #ffffff;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(217, 45, 32, 0.4);
  cursor: pointer;
  transition: all 0.15s ease;
}

.floating-bot-btn:hover {
  background-color: var(--primary-hover);
  transform: translateY(-2px);
}

.variants-table-card {
  margin-top: 24px;
  padding: 24px;
}

.variants-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.variants-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.variants-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.badge-count-pill {
  padding: 4px 12px;
  background-color: #fff1f2;
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  border-radius: 20px;
  border: 1px solid #fee2e2;
}

.bulk-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bulk-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bulk-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.bulk-input {
  width: 120px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.btn-apply-bulk {
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.btn-apply-bulk:hover {
  background-color: #f1f5f9;
}

.table-responsive {
  overflow-x: auto;
}

.variants-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13.5px;
}

.variants-table th {
  padding: 12px 14px;
  background-color: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
}

.variants-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.variant-color-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.size-pill {
  display: inline-block;
  padding: 4px 10px;
  background-color: #f1f5f9;
  border-radius: var(--radius-sm);
  font-weight: 700;
  font-size: 12.5px;
}

.table-cell-input {
  height: 36px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-sm);
  font-size: 13px;
  width: 100%;
}

.table-cell-input:focus {
  border-color: var(--primary);
  outline: none;
}

.price-cell-input {
  font-weight: 600;
  color: var(--primary);
}

.stock-cell-input {
  font-weight: 600;
}

.btn-delete-row {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  display: inline-flex;
}

.btn-delete-row:hover {
  color: var(--primary);
  background-color: #fee2e2;
}

.status-dot {
  width: 6px;
  height: 6px;
  background-color: #10b981;
  border-radius: 50%;
  display: inline-block;
}

.variants-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

.btn-save-large {
  padding: 10px 24px;
  font-size: 14px;
}

/* Toast Notifications */
.toast-notification {
  position: fixed;
  bottom: 84px;
  right: 32px;
  padding: 12px 20px;
  border-radius: var(--radius-md);
  color: #ffffff;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  font-weight: 600;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

.toast-success {
  background-color: #0f172a;
}

.toast-warning {
  background-color: #b45309;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.25s ease;
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(16px);
}
</style>
