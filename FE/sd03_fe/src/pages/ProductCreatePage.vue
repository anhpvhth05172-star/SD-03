<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import QuickAddModal from '../components/QuickAddModal.vue'
import { notifySuccess, notifyWarning, notifyError } from '@/utils/notify'

const router = useRouter()
const API_BASE = 'http://localhost:8080/api/v1'

const form = reactive({
  code: '423305',
  name: '',
  brand: '',
  category: '',
  gender: 'Nam',
  material: '',
  style: '',
  origin: '',
  feature: '',
  description: ''
})

const errors = reactive({
  name: '',
  brand: '',
  category: '',
  material: '',
  style: '',
  origin: ''
})

const clearErrors = () => {
  errors.name = ''
  errors.brand = ''
  errors.category = ''
  errors.material = ''
  errors.style = ''
  errors.origin = ''
}

const brandOptions = ref([])
const categoryOptions = ref([])
const materialOptions = ref([])
const styleOptions = ref([])
const originOptions = ref([])
const colorOptions = ref([])
const sizeOptions = ref([])

const genderOptions = [
  { value: 'Nam', label: 'Nam' },
  { value: 'Nữ', label: 'Nữ' },
  { value: 'Unisex', label: 'Tất cả (Unisex)' }
]

const selectedColors = ref([])
const selectedSizes = ref([])
const tempColorSelect = ref('')
const tempSizeSelect = ref('')

const isAllChecked = ref(true)
const defaultQty = ref(10)
const defaultPrice = ref(1500000)

const hasGeneratedVariants = ref(false)
const colorGroups = ref([])

const colorImages = reactive({})

const isModalOpen = ref(false)
const modalType = ref('color')

const showToast = (message, type = 'success') => {
  if (type === 'success') {
    notifySuccess(message)
  } else if (type === 'warning') {
    notifyWarning(message)
  } else {
    notifyError(message)
  }
}

const fetchOptions = async () => {
  try {
    const [bRes, cRes, mRes, sRes, oRes, colRes, szRes] = await Promise.all([
      fetch(`${API_BASE}/attributes/thuong_hieu`),
      fetch(`${API_BASE}/attributes/loai_giay`),
      fetch(`${API_BASE}/attributes/chat_lieu`),
      fetch(`${API_BASE}/attributes/kieu_dang`),
      fetch(`${API_BASE}/attributes/xuat_xu`),
      fetch(`${API_BASE}/attributes/mau_sac`),
      fetch(`${API_BASE}/attributes/kich_co`)
    ])

    if (bRes.ok) brandOptions.value = await bRes.json()
    if (cRes.ok) categoryOptions.value = await cRes.json()
    if (mRes.ok) materialOptions.value = await mRes.json()
    if (sRes.ok) styleOptions.value = await sRes.json()
    if (oRes.ok) originOptions.value = await oRes.json()
    if (colRes.ok) colorOptions.value = await colRes.json()
    if (szRes.ok) sizeOptions.value = await szRes.json()
  } catch (err) {
    console.error('Lỗi tải danh mục thuộc tính từ máy chủ:', err)
  }
}

const generateCode = () => {
  const num = Math.floor(100000 + Math.random() * 900000)
  form.code = `${num}`
}

const handleSelectColor = (e) => {
  const val = e.target.value
  if (!val) return
  const col = colorOptions.value.find(c => c.id === Number(val) || c.ten === val)
  if (col && !selectedColors.value.some(c => c.id === col.id)) {
    selectedColors.value.push(col)
  }
  tempColorSelect.value = ''
}

const removeColor = (id) => {
  selectedColors.value = selectedColors.value.filter(c => c.id !== id)
}

const handleSelectSize = (e) => {
  const val = e.target.value
  if (!val) return
  const sz = sizeOptions.value.find(s => s.id === Number(val) || s.ten === val)
  if (sz && !selectedSizes.value.some(s => s.id === sz.id)) {
    selectedSizes.value.push(sz)
  }
  tempSizeSelect.value = ''
}

const removeSize = (id) => {
  selectedSizes.value = selectedSizes.value.filter(s => s.id !== id)
}

const generateVariants = () => {
  if (selectedColors.value.length === 0 || selectedSizes.value.length === 0) {
    showToast('Vui lòng chọn ít nhất 1 màu sắc và 1 kích cỡ!', 'warning')
    return
  }

  const groups = []
  selectedColors.value.forEach(color => {
    const items = []
    selectedSizes.value.forEach((size, idx) => {
      items.push({
        checked: true,
        stt: idx + 1,
        sizeId: size.id,
        sizeName: size.ten || size.name,
        qty: defaultQty.value || 0,
        price: defaultPrice.value || 0
      })
    })

    groups.push({
      colorId: color.id,
      colorName: color.ten || color.name,
      colorCode: color.ma || '#111827',
      sizeSummary: selectedSizes.value.map(s => `Size ${s.ten || s.name}`).join(' • '),
      items
    })

    if (!colorImages[color.id]) {
      colorImages[color.id] = []
    }
  })

  colorGroups.value = groups
  hasGeneratedVariants.value = true
  showToast(`Đã tạo thành công danh sách biến thể cho ${selectedColors.value.length} màu sắc!`)
}

const applyBulkValues = () => {
  // 1. Kiểm tra có biến thể nào được tick chọn hay không
  let selectedCount = 0
  colorGroups.value.forEach(group => {
    group.items.forEach(item => {
      if (item.checked) selectedCount++
    })
  })

  if (selectedCount === 0) {
    showToast('Vui lòng chọn ít nhất 1 biến thể để áp dụng!', 'warning')
    return
  }

  // 2. Validate Giá bán mặc định (Bắt buộc)
  if (defaultPrice.value === null || defaultPrice.value === undefined || defaultPrice.value === '') {
    showToast('Giá bán mặc định không được để trống!', 'warning')
    return
  }
  const priceNum = Number(defaultPrice.value)
  if (isNaN(priceNum) || priceNum < 1000 || priceNum > 1000000000) {
    showToast('Giá bán mặc định phải là số từ 1,000đ đến 1,000,000,000đ!', 'warning')
    return
  }

  // 3. Validate Số lượng mặc định
  let qtyNum = 0
  if (defaultQty.value !== null && defaultQty.value !== undefined && defaultQty.value !== '') {
    qtyNum = Number(defaultQty.value)
    if (isNaN(qtyNum) || qtyNum < 0 || qtyNum > 100000) {
      showToast('Số lượng mặc định phải là số nguyên từ 0 đến 100,000!', 'warning')
      return
    }
  }

  // 4. Áp dụng giá trị vào các biến thể đã chọn
  colorGroups.value.forEach(group => {
    group.items.forEach(item => {
      if (item.checked) {
        item.qty = qtyNum
        item.price = priceNum
      }
    })
  })

  showToast(`Đã áp dụng số lượng (${qtyNum}) và giá bán (${priceNum.toLocaleString('vi-VN')} đ) cho ${selectedCount} biến thể đã chọn!`, 'success')
}

const toggleCheckAll = (e) => {
  const checked = e.target.checked
  isAllChecked.value = checked
  colorGroups.value.forEach(group => {
    group.items.forEach(item => item.checked = checked)
  })
}

const removeItem = (groupIndex, itemIndex) => {
  colorGroups.value[groupIndex].items.splice(itemIndex, 1)
  if (colorGroups.value[groupIndex].items.length === 0) {
    colorGroups.value.splice(groupIndex, 1)
  }
}

// Kiểm tra Magic Bytes client-side (JPEG: FF D8 FF, PNG: 89 50 4E 47, WEBP: RIFF...WEBP)
const checkImageMagicBytes = async (file) => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onloadend = (e) => {
      const arr = new Uint8Array(e.target.result).subarray(0, 12)
      if (arr.length < 4) return resolve(false)
      
      // JPEG: FF D8 FF
      if (arr[0] === 0xFF && arr[1] === 0xD8 && arr[2] === 0xFF) return resolve(true)
      // PNG: 89 50 4E 47
      if (arr[0] === 0x89 && arr[1] === 0x50 && arr[2] === 0x4E && arr[3] === 0x47) return resolve(true)
      // WEBP: 52 49 46 46 (RIFF) ... 57 45 42 50 (WEBP)
      if (arr[0] === 0x52 && arr[1] === 0x49 && arr[2] === 0x46 && arr[3] === 0x46 &&
          arr[8] === 0x57 && arr[9] === 0x45 && arr[10] === 0x42 && arr[11] === 0x50) return resolve(true)
      // GIF: 47 49 46 38
      if (arr[0] === 0x47 && arr[1] === 0x49 && arr[2] === 0x46 && arr[3] === 0x38) return resolve(true)
      
      resolve(false)
    }
    reader.readAsArrayBuffer(file.slice(0, 12))
  })
}

const triggerImageUpload = (colorId) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp'
  input.multiple = true
  input.onchange = async (e) => {
    const files = Array.from(e.target.files)
    if (!colorImages[colorId]) colorImages[colorId] = []
    const currentList = colorImages[colorId]

    const MAX_SIZE = 5 * 1024 * 1024 // 5MB
    const MAX_IMAGES_PER_COLOR = 4

    let addedCount = 0
    for (const file of files) {
      if (currentList.length >= MAX_IMAGES_PER_COLOR) {
        showToast(`Nhóm màu này đã đạt giới hạn tối đa ${MAX_IMAGES_PER_COLOR} ảnh chi tiết!`, 'warning')
        break
      }

      // 1. Kiểm tra dung lượng 5MB
      if (file.size > MAX_SIZE) {
        showToast(`Ảnh "${file.name}" (${(file.size / (1024 * 1024)).toFixed(1)}MB) vượt quá dung lượng tối đa 5MB!`, 'warning')
        continue
      }

      // 2. Kiểm tra định dạng & Magic Bytes
      const isValidRealImage = await checkImageMagicBytes(file)
      if (!isValidRealImage) {
        showToast(`Tệp "${file.name}" không phải là ảnh hợp lệ (chỉ hỗ trợ JPG, PNG, WEBP)!`, 'error')
        continue
      }

      const reader = new FileReader()
      reader.onload = (uploadEvent) => {
        const base64Url = uploadEvent.target.result
        currentList.push(base64Url)
      }
      reader.readAsDataURL(file)
      addedCount++
    }

    if (addedCount > 0) {
      showToast(`Đã tải lên ${addedCount} ảnh hợp lệ!`)
    }
  }
  input.click()
}

const removeImage = (colorId, index, event) => {
  if (event) event.stopPropagation()
  if (colorImages[colorId]) {
    colorImages[colorId].splice(index, 1)
  }
}

const openQuickAdd = (type) => {
  modalType.value = type
  isModalOpen.value = true
}

const handleAttributeAdded = async (item) => {
  try {
    const endpoint = item.type === 'color' ? 'mau_sac' : 'kich_co'
    const payload = {
      ten: item.name.trim(),
      moTa: item.code || item.description || '',
      trangThai: true
    }

    const res = await fetch(`${API_BASE}/attributes/${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      const saved = await res.json()
      if (item.type === 'color') {
        const newCol = { id: saved.id, ten: saved.ten, ma: saved.moTa || item.code || '#111827' }
        colorOptions.value.push(newCol)
        selectedColors.value.push(newCol)
        showToast(`Đã lưu màu sắc mới: ${saved.ten}`)
      } else {
        const newSz = { id: saved.id, ten: saved.ten }
        sizeOptions.value.push(newSz)
        selectedSizes.value.push(newSz)
        showToast(`Đã lưu kích cỡ mới: ${saved.ten}`)
      }
    } else {
      const errData = await res.json().catch(() => ({}))
      showToast(errData.message || 'Không thể lưu thuộc tính mới vào CSDL', 'warning')
    }
  } catch (err) {
    console.error('Quick add attribute error:', err)
    showToast('Lỗi kết nối khi thêm nhanh thuộc tính', 'error')
  }
}

const validateForm = () => {
  clearErrors()
  let isValid = true

  if (!form.name || !form.name.trim()) {
    errors.name = 'Vui lòng nhập tên sản phẩm'
    isValid = false
  }
  if (!form.brand) {
    errors.brand = 'Vui lòng chọn Thương hiệu'
    isValid = false
  }
  if (!form.category) {
    errors.category = 'Vui lòng chọn Loại giày'
    isValid = false
  }
  if (!form.material) {
    errors.material = 'Vui lòng chọn Chất liệu'
    isValid = false
  }
  if (!form.style) {
    errors.style = 'Vui lòng chọn Kiểu dáng'
    isValid = false
  }
  if (!form.origin) {
    errors.origin = 'Vui lòng chọn Xuất xứ'
    isValid = false
  }

  return isValid
}

const isSubmitting = ref(false)
const showMergeModal = ref(false)
const mergePreviewData = ref(null)

const saveProduct = async (confirmMerge = false) => {
  if (!validateForm()) {
    showToast('Vui lòng điền đầy đủ các thuộc tính bắt buộc (*)!', 'warning')
    return
  }

  const bienThes = []
  if (colorGroups.value.length > 0) {
    let invalidVariantError = null

    for (const group of colorGroups.value) {
      for (const item of group.items) {
        if (item.checked) {
          const qty = Number(item.qty)
          const price = Number(item.price)

          if (isNaN(qty) || qty < 0 || qty > 100000) {
            invalidVariantError = `Biến thể (Màu: ${group.colorName}, Size: ${item.sizeName}) có số lượng không hợp lệ (phải từ 0 đến 100,000).`
            break
          }
          if (isNaN(price) || price < 1000 || price > 1000000000) {
            invalidVariantError = `Biến thể (Màu: ${group.colorName}, Size: ${item.sizeName}) có giá bán không hợp lệ (phải từ 1,000đ đến 1,000,000,000đ).`
            break
          }

          const imageList = colorImages[group.colorId] || []
          const firstImg = imageList.length > 0 ? imageList[0] : null

          bienThes.push({
            idMauSac: Number(group.colorId),
            idKichCo: Number(item.sizeId),
            soLuong: qty,
            giaBan: price,
            hinhAnh: firstImg,
            trangThai: true
          })
        }
      }
      if (invalidVariantError) break
    }

    if (invalidVariantError) {
      showToast(invalidVariantError, 'warning')
      return
    }
  }

  const payload = {
    tenSanPham: form.name.trim(),
    idThuongHieu: Number(form.brand),
    idLoaiGiay: Number(form.category),
    idChatLieu: Number(form.material),
    idKieuDang: Number(form.style),
    idXuatXu: Number(form.origin),
    doiTuong: form.gender,
    tinhNang: form.feature ? form.feature.trim() : '',
    moTa: form.description ? form.description.trim() : '',
    trangThai: true,
    confirmMerge: Boolean(confirmMerge),
    bienThes: bienThes
  }

  try {
    isSubmitting.value = true
    const res = await fetch(`${API_BASE}/san-pham/smart-save`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (!res.ok) {
      const errorText = await res.text()
      try {
        const errorObj = JSON.parse(errorText)
        if (errorObj.message) throw new Error(errorObj.message)
      } catch (e) {
        if (errorText.includes('tồn tại')) {
          errors.name = 'Tên sản phẩm đã tồn tại trong cơ sở dữ liệu'
          throw new Error('Tên sản phẩm đã tồn tại')
        }
      }
      throw new Error('Thao tác thất bại, vui lòng kiểm tra lại dữ liệu')
    }

    const data = await res.json()

    // Case trùng lặp -> Cần người dùng xác nhận
    if (data.status === 'NEED_CONFIRMATION') {
      mergePreviewData.value = data
      showMergeModal.value = true
      return
    }

    // Case thành công
    showMergeModal.value = false
    localStorage.removeItem('product_initial_list_cache')
    localStorage.removeItem('variant_list_cache')

    showToast(data.message || 'Lưu sản phẩm thành công!')

    setTimeout(() => {
      router.push('/san-pham')
    }, 1000)
  } catch (err) {
    showToast(err.message || 'Không thể kết nối đến cơ sở dữ liệu', 'error')
  } finally {
    isSubmitting.value = false
  }
}

const confirmMergeAction = () => {
  saveProduct(true)
}

const goBack = () => {
  router.push('/san-pham')
}

onMounted(() => {
  generateCode()
  fetchOptions()
})
</script>

<template>
  <div class="create-product-container">
    <div class="top-nav-bar">
      <div class="top-left"></div>
      <button class="btn btn-back-outline" @click="goBack">
        ← Quay lại danh sách
      </button>
    </div>

    <div class="form-card">
      <div class="form-grid-2col">
        <div class="form-field">
          <label class="form-label">Mã sản phẩm</label>
          <input v-model="form.code" type="text" class="form-input input-readonly" readonly />
        </div>

        <div class="form-field">
          <label class="form-label required">Sản phẩm</label>
          <input v-model="form.name" type="text" class="form-input" :class="{ 'input-err': errors.name }"
            placeholder="Nhập tên sản phẩm (vd: ASICS GEL-Kayano 31)..." />
          <span v-if="errors.name" class="err-txt">{{ errors.name }}</span>
        </div>

        <div class="form-field">
          <label class="form-label required">Thương hiệu</label>
          <select v-model="form.brand" class="form-select" :class="{ 'input-err': errors.brand }">
            <option value="" disabled selected>Chọn thương hiệu...</option>
            <option v-for="b in brandOptions" :key="b.id" :value="b.id">{{ b.ten }}</option>
          </select>
          <span v-if="errors.brand" class="err-txt">{{ errors.brand }}</span>
        </div>

        <div class="form-field">
          <label class="form-label required">Loại giày</label>
          <select v-model="form.category" class="form-select" :class="{ 'input-err': errors.category }">
            <option value="" disabled selected>Chọn loại giày...</option>
            <option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{ c.ten }}</option>
          </select>
          <span v-if="errors.category" class="err-txt">{{ errors.category }}</span>
        </div>

        <div class="form-field">
          <label class="form-label required">Giới tính / Đối tượng</label>
          <select v-model="form.gender" class="form-select">
            <option v-for="g in genderOptions" :key="g.value" :value="g.value">{{ g.label }}</option>
          </select>
        </div>

        <div class="form-field">
          <label class="form-label required">Chất liệu</label>
          <select v-model="form.material" class="form-select" :class="{ 'input-err': errors.material }">
            <option value="" disabled selected>Chọn chất liệu...</option>
            <option v-for="m in materialOptions" :key="m.id" :value="m.id">{{ m.ten }}</option>
          </select>
          <span v-if="errors.material" class="err-txt">{{ errors.material }}</span>
        </div>

        <div class="form-field">
          <label class="form-label required">Kiểu dáng</label>
          <select v-model="form.style" class="form-select" :class="{ 'input-err': errors.style }">
            <option value="" disabled selected>Chọn kiểu dáng...</option>
            <option v-for="s in styleOptions" :key="s.id" :value="s.id">{{ s.ten }}</option>
          </select>
          <span v-if="errors.style" class="err-txt">{{ errors.style }}</span>
        </div>

        <div class="form-field">
          <label class="form-label required">Xuất xứ</label>
          <select v-model="form.origin" class="form-select" :class="{ 'input-err': errors.origin }">
            <option value="" disabled selected>Chọn xuất xứ...</option>
            <option v-for="o in originOptions" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
          <span v-if="errors.origin" class="err-txt">{{ errors.origin }}</span>
        </div>

        <div class="form-field full-row">
          <label class="form-label">Tính năng sản phẩm</label>
          <input v-model="form.feature" type="text" class="form-input"
            placeholder="Mô tả các tính năng nổi bật (vd: Êm chân, thoáng khí...)" />
        </div>
      </div>
    </div>

    <div class="form-card margin-top">
      <div class="attr-row">
        <label class="attr-label required">Màu sắc</label>
        <div class="attr-input-wrap">
          <select v-model="tempColorSelect" class="form-select attr-select" @change="handleSelectColor">
            <option value="" disabled selected>Chọn màu</option>
            <option v-for="col in colorOptions" :key="col.id" :value="col.id">{{ col.ten }}</option>
          </select>
          <button class="btn btn-quick-add" @click="openQuickAdd('color')">+ Thêm nhanh</button>
        </div>
      </div>

      <div v-if="selectedColors.length > 0" class="badges-row">
        <span v-for="c in selectedColors" :key="c.id" class="badge-chip">
          <span class="color-dot" :style="{ backgroundColor: c.ma || '#111827' }"></span>
          {{ c.ten }}
          <button class="btn-remove-chip" @click="removeColor(c.id)">✕</button>
        </span>
      </div>

      <div class="attr-row margin-top">
        <label class="attr-label required">Kích cỡ</label>
        <div class="attr-input-wrap">
          <select v-model="tempSizeSelect" class="form-select attr-select" @change="handleSelectSize">
            <option value="" disabled selected>Chọn size</option>
            <option v-for="sz in sizeOptions" :key="sz.id" :value="sz.id">Size {{ sz.ten }}</option>
          </select>
          <button class="btn btn-quick-add" @click="openQuickAdd('size')">+ Thêm nhanh</button>
        </div>
      </div>

      <div v-if="selectedSizes.length > 0" class="badges-row">
        <span v-for="s in selectedSizes" :key="s.id" class="badge-chip size-chip">
          Size {{ s.ten }}
          <button class="btn-remove-chip" @click="removeSize(s.id)">✕</button>
        </span>
      </div>

      <div class="generate-btn-row">
        <button class="btn btn-red-primary" @click="generateVariants">
          Tạo biến thể tự động
        </button>
      </div>
    </div>

    <div class="form-card margin-top">
      <label class="card-section-label">Mô tả</label>
      <textarea v-model="form.description" rows="3" class="form-textarea" placeholder="Mô tả sản phẩm"></textarea>
    </div>

    <div v-if="hasGeneratedVariants" class="form-card margin-top">
      <div class="bulk-header-row">
        <label class="checkbox-label">
          <input type="checkbox" :checked="isAllChecked" @change="toggleCheckAll" class="custom-checkbox" />
          <span>Chọn tất cả biến thể</span>
        </label>

        <div class="bulk-inputs-right">
          <div class="bulk-field">
            <label class="bulk-lbl">Số lượng mặc định</label>
            <input v-model.number="defaultQty" type="number" class="bulk-num-input" placeholder="0" min="0" />
          </div>

          <div class="bulk-field">
            <label class="bulk-lbl required">Giá bán mặc định</label>
            <input v-model.number="defaultPrice" type="number" class="bulk-num-input" placeholder="0" min="0" />
          </div>

          <button class="btn btn-outline-red" @click="applyBulkValues">Áp dụng</button>
        </div>
      </div>

      <div v-for="(group, gIdx) in colorGroups" :key="group.colorId" class="color-group-box margin-top">
        <div class="group-header">
          <div class="group-title">
            <span class="color-dot" :style="{ backgroundColor: group.colorCode }"></span>
            <strong>{{ group.colorName }}</strong>
          </div>
          <span class="group-summary-badge">{{ group.sizeSummary }}</span>
        </div>

        <table class="variant-data-table">
          <thead>
            <tr>
              <th width="40"></th>
              <th width="60">STT</th>
              <th>Kích cỡ</th>
              <th width="220">Số lượng</th>
              <th width="240">Giá bán</th>
              <th width="80" class="text-center">Xóa</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, iIdx) in group.items" :key="item.sizeId">
              <td>
                <input type="checkbox" v-model="item.checked" class="custom-checkbox" />
              </td>
              <td class="text-muted">{{ item.stt }}</td>
              <td class="font-medium">Size {{ item.sizeName }}</td>
              <td>
                <input v-model.number="item.qty" type="number" class="table-num-input" min="0" />
              </td>
              <td>
                <input v-model.number="item.price" type="number" class="table-num-input" min="0" />
              </td>
              <td class="text-center">
                <button class="btn-icon-trash" @click="removeItem(gIdx, iIdx)" title="Xóa">
                  🗑
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="hasGeneratedVariants" class="pink-tint-card margin-top">
      <div class="tint-header">
        <h3 class="tint-title">Ảnh sản phẩm chi tiết</h3>
        <p class="tint-subtitle">Thêm ảnh cho từng màu sắc (biến thể đại diện) để tự động đồng bộ cho toàn bộ kích cỡ.
        </p>
      </div>

      <div class="images-grid margin-top">
        <div v-for="group in colorGroups" :key="group.colorId" class="color-image-card">
          <div class="img-card-head">
            <div class="img-head-text">
              <span class="img-card-title">Ảnh sản phẩm màu {{ group.colorName.toLowerCase() }}</span>
              <span class="img-card-sub">
                Đã tải: <strong>{{ colorImages[group.colorId]?.length || 0 }}/4</strong> ảnh chi tiết (tối đa 5MB/ảnh)
              </span>
            </div>
            <button 
              class="btn btn-red-solid" 
              @click="triggerImageUpload(group.colorId)"
              :disabled="(colorImages[group.colorId]?.length || 0) >= 4"
            >
              + Thêm ảnh
            </button>
          </div>

          <div class="img-dropzone" @click="triggerImageUpload(group.colorId)">
            <div v-if="colorImages[group.colorId] && colorImages[group.colorId].length > 0" class="preview-thumbs-grid">
              <div 
                v-for="(src, idx) in colorImages[group.colorId]" 
                :key="idx" 
                class="thumb-item-wrap"
                @click.stop
              >
                <img :src="src" class="thumb-preview" />
                <span v-if="idx === 0" class="badge-main-img">Ảnh chính</span>
                <button 
                  class="btn-remove-thumb" 
                  @click="removeImage(group.colorId, idx, $event)" 
                  title="Xóa ảnh này"
                >
                  ×
                </button>
              </div>
            </div>
            <div v-else class="empty-dropzone-content">
              <div class="icon-box-placeholder">🖼</div>
              <p class="placeholder-text-main">Chưa có ảnh cho nhóm màu này</p>
              <p class="placeholder-text-sub">Nhấn để tải lên từ 1 đến 4 ảnh (JPG, PNG, WEBP • Max 5MB/ảnh).</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Cảnh báo & Xác nhận Cập nhật / Cộng dồn kho khi trùng sản phẩm -->
    <div v-if="showMergeModal" class="modal-overlay">
      <div class="modal-merge-card">
        <div class="merge-modal-header">
          <div class="header-icon-wrap">
            <span class="warning-icon">⚠️</span>
            <div>
              <h3 class="merge-modal-title">Phát hiện sản phẩm đã tồn tại</h3>
              <p class="merge-modal-sub">Hệ thống phát hiện sản phẩm này đã có trong cơ sở dữ liệu</p>
            </div>
          </div>
          <button class="btn-close-modal" @click="showMergeModal = false">×</button>
        </div>

        <div class="merge-modal-body">
          <div class="merge-alert-box">
            <p>
              Sản phẩm <strong>"{{ mergePreviewData?.tenSanPham }}"</strong> 
              (Mã: <span class="badge-code">{{ mergePreviewData?.maSanPham }}</span>) 
              đã có sẵn trong hệ thống.
            </p>
            <p class="sub-alert-text">
              Bạn có muốn <strong>cập nhật thông tin</strong> và <strong>cộng dồn số lượng tồn kho</strong> cho các biến thể dưới đây không?
            </p>
          </div>

          <div class="merge-table-container">
            <table class="merge-table">
              <thead>
                <tr>
                  <th>Biến thể</th>
                  <th>Trạng thái</th>
                  <th class="text-right">Tồn hiện tại</th>
                  <th class="text-right">Nhập thêm</th>
                  <th class="text-right">Tổng tồn mới</th>
                  <th class="text-right">Giá bán mới</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in mergePreviewData?.previewItems || []" :key="idx">
                  <td class="font-semibold">
                    {{ item.tenMau }} - Size {{ item.tenKichCo }}
                  </td>
                  <td>
                    <span v-if="item.isExisting" class="tag-status tag-existing">🟡 Đã có trong kho</span>
                    <span v-else class="tag-status tag-new">🟢 Thêm mới</span>
                  </td>
                  <td class="text-right text-muted">
                    {{ item.currentStock?.toLocaleString('vi-VN') || 0 }}
                  </td>
                  <td class="text-right text-green-bold">
                    + {{ item.addedStock?.toLocaleString('vi-VN') || 0 }}
                  </td>
                  <td class="text-right text-primary-bold">
                    {{ item.totalStock?.toLocaleString('vi-VN') || 0 }}
                  </td>
                  <td class="text-right font-medium">
                    {{ item.newPrice ? Number(item.newPrice).toLocaleString('vi-VN') + ' đ' : '-' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="merge-modal-footer">
          <button class="btn btn-cancel-merge" @click="showMergeModal = false" :disabled="isSubmitting">
            Hủy / Đổi tên khác
          </button>
          <button class="btn btn-confirm-merge" @click="confirmMergeAction" :disabled="isSubmitting">
            <span v-if="isSubmitting" class="spinner-small"></span>
            <span v-else>✓ Đồng ý Cập nhật & Cộng dồn kho</span>
          </button>
        </div>
      </div>
    </div>

    <div class="fixed-bottom-bar">
      <button class="btn btn-save-fixed" @click="() => saveProduct(false)" :disabled="isSubmitting">
        <span v-if="isSubmitting" class="spinner-small"></span>
        <span v-else>Lưu sản phẩm</span>
      </button>
    </div>

    <QuickAddModal :is-open="isModalOpen" :type="modalType" @close="isModalOpen = false" @add="handleAttributeAdded" />
  </div>
</template>

<style scoped src="@/assets/styles/ProductCreatePage.css"></style>
