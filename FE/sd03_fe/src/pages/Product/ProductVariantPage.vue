<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import QRCode from 'qrcode'
import QuickAddModal from '@/components/QuickAddModal.vue'
import SearchableSelect from '@/components/SearchableSelect.vue'
import CurrencyInput from '@/components/CurrencyInput.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { notifySuccess, notifyWarning, notifyError } from '@/utils/notify'
import { formatNumber, formatVnd } from '@/utils/format'
import { exportExcel } from '@/utils/export'
import { API_ORIGIN } from '@/api/http'

const router = useRouter()
const route = useRoute()
const API_BASE = `${API_ORIGIN}/api/v1`

const showToast = ref(true)
const isLoading = ref(false)

const variants = ref([])
const totalElements = ref(0)
const totalPages = ref(1)
const currentPage = ref(0)
const pageSize = ref(10)
const pageSizeOptions = [5, 10, 20, 50]

const selectedIds = ref([])
const isAllSelected = computed({
  get: () => variants.value.length > 0 && selectedIds.value.length === variants.value.length,
  set: (val) => {
    if (val) {
      selectedIds.value = variants.value.map(v => v.id)
    } else {
      selectedIds.value = []
    }
  }
})

const filters = reactive({
  search: '',
  idSanPham: '',
  idMauSac: '',
  idKichCo: '',
  trangThai: ''
})

const colorsList = ref([])
const sizesList = ref([])

let debounceTimer = null
let currentAbortController = null

const productsList = ref([])

const formattedProductsOptions = computed(() => {
  return (productsList.value || []).map(p => ({
    id: p.id,
    ten: p.maSanPham ? `[${p.maSanPham}] ${p.tenSanPham}` : p.tenSanPham,
    tenSanPham: p.tenSanPham,
    maSanPham: p.maSanPham
  }))
})

watch(() => filters.idSanPham, (newVal) => {
  if (newVal) {
    const p = productsList.value.find(item => item.id === Number(newVal))
    if (p) {
      selectedProductName.value = p.tenSanPham || ''
      selectedProductCode.value = p.maSanPham || `SP0${p.id}`
    }
  } else if (!route.query.idSanPham) {
    selectedProductName.value = ''
    selectedProductCode.value = ''
  }
})

const fetchFilterOptions = async () => {
  try {
    const [resColor, resSize, resProd] = await Promise.all([
      fetch(`${API_BASE}/attributes/mau_sac`),
      fetch(`${API_BASE}/attributes/kich_co`),
      fetch(`${API_BASE}/san-pham?page=0&size=1000`)
    ])
    if (resColor.ok) colorsList.value = await resColor.json()
    if (resSize.ok) sizesList.value = await resSize.json()
    if (resProd.ok) {
      const pData = await resProd.json()
      productsList.value = pData.content || []
    }
  } catch (e) {
    console.error('Lỗi khi tải danh mục màu sắc / kích cỡ / sản phẩm:', e)
  }
}

const buildParams = (pageIndex) => {
  const params = new URLSearchParams()
  params.append('page', pageIndex)
  params.append('size', pageSize.value)

  if (filters.search && filters.search.trim()) {
    params.append('search', filters.search.trim())
  }
  if (filters.idSanPham) {
    params.append('idSanPham', filters.idSanPham)
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

  return params
}

const fetchVariants = async (useCache = true) => {
  isLoading.value = true
  selectedIds.value = []

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
    } else {
      if (variants.value.length === 0) {
        useFallbackData()
      }
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      console.error('Lỗi khi tải biến thể sản phẩm:', err)
      if (variants.value.length === 0) {
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
  }, 200)
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

const onSizeChange = (s) => {
  pageSize.value = s
  currentPage.value = 0
  fetchVariants(false)
}

const formatPrice = (val) => {
  if (!val) return '0 đ'
  return new Intl.NumberFormat('vi-VN').format(val) + ' đ'
}

const hasDiscount = (item) => {
  return item && item.discount && item.discount !== '-' && item.discount !== '0%'
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
  if (item.trangThai === false) return 'Ngừng kinh doanh'
  return 'Đang kinh doanh'
}

const getVariantStatusClass = (item) => {
  if (item.trangThai === false) return 'is-stopped'
  return 'is-selling'
}

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

const toggleStatus = async (item) => {
  const oldStatus = item.trangThai
  const newStatus = !oldStatus
  item.trangThai = newStatus
  const statusLabel = newStatus ? 'Đang kinh doanh' : 'Ngừng kinh doanh'
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

const isExporting = ref(false)

const exportVariantsToExcel = async () => {
  if (isExporting.value) return
  isExporting.value = true

  try {
    let itemsToExport = []

    if (selectedIds.value.length > 0) {
      itemsToExport = variants.value.filter(v => selectedIds.value.includes(v.id))
    } else {
      const params = buildParams(0)
      params.set('size', '10000')
      const res = await fetch(`${API_BASE}/san-pham-chi-tiet?${params.toString()}`)
      if (res.ok) {
        const data = await res.json()
        itemsToExport = data.content || variants.value
      } else {
        itemsToExport = variants.value
      }
    }

    if (!itemsToExport || itemsToExport.length === 0) {
      showToastNotice('Không có dữ liệu', 'Không có biến thể nào để xuất Excel', 'warning')
      return
    }

    const now = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    const dateStr = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`

    await exportExcel(
      `Danh_sach_bien_the_${dateStr}.xlsx`,
      [
        'STT', 'Tên sản phẩm', 'Thương hiệu', 'Loại giày', 'Chất liệu', 'Kiểu dáng', 'Xuất xứ',
        'Đối tượng', 'Màu sắc', 'Kích cỡ', 'Số lượng', 'Giá bán', 'Tính năng', 'Mô tả',
        'Mã SP', 'Mã CTSP', 'Trạng thái',
      ],
      itemsToExport.map((item, index) => [
        index + 1,
        item.tenSp || '',
        item.tenThuongHieu || '',
        item.tenLoaiGiay || '',
        item.chatLieu || '',
        item.kieuDang || '',
        item.xuatXu || '',
        item.doiTuong || '',
        item.color || '',
        item.size || '',
        item.stock !== undefined ? item.stock : 0,
        item.price || 0,
        item.tinhNang || '',
        item.moTa || '',
        item.maSp || '',
        item.maCtsp || '',
        item.trangThai !== false ? 'Đang kinh doanh' : 'Ngừng kinh doanh',
      ]),
      { sheetName: 'Danh sách biến thể' }
    )
    showToastNotice('Thành công', `Đã xuất ${itemsToExport.length} biến thể ra file Excel!`, 'success')
  } catch (err) {
    console.error('Lỗi xuất Excel:', err)
    showToastNotice('Lỗi', 'Không thể xuất file Excel', 'danger')
  } finally {
    isExporting.value = false
  }
}

const isGeneratingQr = ref(false)
const qrModal = reactive({
  isOpen: false,
  title: '',
  subtitle: '',
  items: []
})

const generateVariantQrDataUrl = async (item) => {
  const payload = JSON.stringify({
    maCtsp: item.maCtsp || `SPCT0${item.id}`,
    maSp: item.maSp || '',
        tenSp: item.tenSp || '',
    color: item.color || '',
    size: item.size || '',
    price: item.price || 0,
    stock: item.stock || 0,
    id: item.id
  })
  return await QRCode.toDataURL(payload, {
    width: 260,
    margin: 2,
    color: {
      dark: '#0f172a',
      light: '#ffffff'
    }
  })
}

const openSingleQrModal = async (item) => {
  isGeneratingQr.value = true
  try {
    const dataUrl = await generateVariantQrDataUrl(item)
    qrModal.title = `Mã QR Biến thể: ${item.maCtsp || `SPCT0${item.id}`}`
      qrModal.subtitle = `${item.tenSp || ''} (${item.color || ''} - Size ${item.size || ''})`
    qrModal.items = [
      {
        ...item,
        qrDataUrl: dataUrl
      }
    ]
    qrModal.isOpen = true
  } catch (err) {
    console.error('Lỗi tạo QR:', err)
    showToastNotice('Lỗi', 'Không thể tạo mã QR', 'danger')
  } finally {
    isGeneratingQr.value = false
  }
}

const openBatchQrModal = async () => {
  let targetItems = []
  if (selectedIds.value.length > 0) {
    targetItems = variants.value.filter(v => selectedIds.value.includes(v.id))
  } else {
    targetItems = variants.value
  }

  if (targetItems.length === 0) {
    showToastNotice('Thông báo', 'Không có biến thể nào trong danh sách để tạo QR', 'warning')
    return
  }

  isGeneratingQr.value = true
  try {
    const itemsWithQr = await Promise.all(
      targetItems.map(async (item) => ({
        ...item,
        qrDataUrl: await generateVariantQrDataUrl(item)
      }))
    )

    qrModal.title = `Danh sách ${itemsWithQr.length} Mã QR Biến thể`
    qrModal.subtitle = selectedIds.value.length > 0
      ? `Đang hiển thị mã QR của các biến thể đã chọn`
      : `Đang hiển thị mã QR của tất cả biến thể trang hiện tại`
    qrModal.items = itemsWithQr
    qrModal.isOpen = true
  } catch (err) {
    console.error('Lỗi tạo batch QR:', err)
    showToastNotice('Lỗi', 'Không thể tạo mã QR hàng loạt', 'danger')
  } finally {
    isGeneratingQr.value = false
  }
}

const downloadQrImage = (item) => {
  if (!item.qrDataUrl) return
  const link = document.createElement('a')
  link.href = item.qrDataUrl
  link.download = `QR_${item.maCtsp || item.id}.png`
  link.click()
}

const downloadAllQrImages = () => {
  if (!qrModal.items || qrModal.items.length === 0) return
  qrModal.items.forEach((item, index) => {
    setTimeout(() => {
      downloadQrImage(item)
    }, index * 200)
  })
  showToastNotice('Thành công', `Đang tải xuống ${qrModal.items.length} ảnh mã QR...`, 'success')
}

const printQrLabels = () => {
  window.print()
}

const isVisualSearchOpen = ref(false)
const isVisualSearching = ref(false)
const visualImagePreview = ref(null)
const visualImageFile = ref(null)
const visualSearchResult = ref(null)

const openVisualSearch = () => {
  visualImagePreview.value = null
  visualImageFile.value = null
  visualSearchResult.value = null
  isVisualSearching.value = false
  isVisualSearchOpen.value = true
}

const onVisualFileSelect = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showToastNotice('Lỗi định dạng', 'Vui lòng chọn file hình ảnh (JPG, PNG, WEBP)', 'warning')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    showToastNotice('File quá lớn', 'Kích thước file ảnh tối đa 5MB', 'warning')
    return
  }
  visualImageFile.value = file
  const reader = new FileReader()
  reader.onload = (evt) => {
    visualImagePreview.value = evt.target.result
    executeVisualSearch(evt.target.result, file.type)
  }
  reader.readAsDataURL(file)
}

const onVisualDrop = (e) => {
  e.preventDefault()
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('image/')) {
    visualImageFile.value = file
    const reader = new FileReader()
    reader.onload = (evt) => {
      visualImagePreview.value = evt.target.result
      executeVisualSearch(evt.target.result, file.type)
    }
    reader.readAsDataURL(file)
  }
}

const executeVisualSearch = async (base64, mimeType) => {
  isVisualSearching.value = true
  visualSearchResult.value = null

  try {
    const res = await fetch(`${API_BASE}/san-pham-chi-tiet/ai-visual-search`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        imageBase64: base64,
        mimeType: mimeType || 'image/jpeg'
      })
    })

    if (res.ok) {
      visualSearchResult.value = await res.json()
      showToastNotice('Thành công', '✨ Gemini AI đã phân tích ảnh và tìm thấy biến thể tương thích!', 'success')
    } else {
      showToastNotice('Lỗi', 'Không thể phân tích ảnh bằng AI', 'danger')
    }
  } catch (err) {
    console.error('Visual search error:', err)
    showToastNotice('Lỗi kết nối', 'Không thể kết nối máy chủ AI Vision', 'danger')
  } finally {
    isVisualSearching.value = false
  }
}

const applyMatchedVariant = (item) => {
  isVisualSearchOpen.value = false
  if (item.idSanPham) {
    filters.idSanPham = item.idSanPham
  }
  filters.search = item.maCtsp || ''
  currentPage.value = 0
  fetchVariants(false)
  showToastNotice('Đã chọn', `Đang lọc danh sách theo biến thể "${item.maCtsp || item.color}"`, 'success')
}

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

const isAddModalOpen = ref(false)
const isSubmittingAdd = ref(false)
const addFormError = ref('')
const addForm = reactive({
  idSanPham: '',
  idMauSac: '',
  idKichCo: '',
  stock: 10,
  price: 1500000,
  trangThai: true,
  imagePreview: null,
  existingColorImage: null
})

const isQuickAddModalOpen = ref(false)
const quickAddModalType = ref('color')

const openQuickAdd = (type) => {
  quickAddModalType.value = type
  isQuickAddModalOpen.value = true
}

const handleQuickAttributeAdded = async (item) => {
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
        const newCol = { id: saved.id, ten: saved.ten, tenMau: saved.ten, ma: saved.moTa || item.code || '#111827' }
        colorsList.value.push(newCol)
        addForm.idMauSac = saved.id
        onAddColorChange()
        showToastNotice('Thành công', `Đã lưu màu sắc mới: ${saved.ten}`, 'success')
      } else {
        const newSz = { id: saved.id, ten: saved.ten, tenKichCo: saved.ten }
        sizesList.value.push(newSz)
        addForm.idKichCo = saved.id
        showToastNotice('Thành công', `Đã lưu kích cỡ mới: ${saved.ten}`, 'success')
      }
    } else {
      const errData = await res.json().catch(() => ({}))
      showToastNotice('Lỗi', errData.message || 'Không thể lưu thuộc tính', 'danger')
    }
  } catch (err) {
    console.error('Quick add attribute error:', err)
    showToastNotice('Lỗi kết nối', 'Không thể thêm nhanh thuộc tính', 'danger')
  }
}

const addFormProduct = computed(() =>
  productsList.value.find((p) => p.id === addForm.idSanPham)
)

const openAddVariantModal = () => {
  addFormError.value = ''
  addForm.idSanPham = productsList.value.length > 0 ? productsList.value[0].id : ''
  addForm.idMauSac = ''
  addForm.idKichCo = ''
  addForm.stock = 10
  addForm.price = (variants.value.length > 0 && variants.value[0].price) ? Number(variants.value[0].price) : 1500000
  addForm.trangThai = true
  addForm.imagePreview = null
  addForm.existingColorImage = null
  isAddModalOpen.value = true
}

const onAddColorChange = () => {
  addForm.existingColorImage = null
  addForm.imagePreview = null
  if (!addForm.idMauSac) return

  const match = variants.value.find(v => Number(v.idMauSac) === Number(addForm.idMauSac) && v.img)
  if (match) {
    addForm.existingColorImage = match.img
  }
}

const triggerAddImageUpload = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    const MAX_SIZE = 5 * 1024 * 1024 // 5MB
    if (file.size > MAX_SIZE) {
      notifyWarning(`Ảnh "${file.name}" vượt quá dung lượng tối đa 5MB!`)
      return
    }

    const isValid = await checkImageMagicBytes(file)
    if (!isValid) {
      notifyError(`Tệp "${file.name}" không phải là ảnh hợp lệ (chỉ hỗ trợ JPG, PNG, WEBP)!`)
      return
    }

    const reader = new FileReader()
    reader.onload = (ev) => {
      addForm.imagePreview = ev.target.result
    }
    reader.readAsDataURL(file)
  }
  input.click()
}

const saveAddVariant = async () => {
  addFormError.value = ''
  const targetIdSanPham = addForm.idSanPham
  if (!targetIdSanPham) {
    addFormError.value = 'Vui lòng chọn sản phẩm cần thêm biến thể!'
    return
  }
  if (!addForm.idMauSac) {
    addFormError.value = 'Vui lòng chọn màu sắc cho biến thể!'
    return
  }
  if (!addForm.idKichCo) {
    addFormError.value = 'Vui lòng chọn kích cỡ cho biến thể!'
    return
  }
  const stockNum = Number(addForm.stock)
  if (addForm.stock === null || addForm.stock === undefined || !Number.isInteger(stockNum) || stockNum < 0 || stockNum > 100000) {
    addFormError.value = 'Số lượng tồn phải là số nguyên từ 0 đến 100,000!'
    return
  }
  const priceNum = Number(addForm.price)
  if (addForm.price === null || addForm.price === undefined || !Number.isFinite(priceNum) || priceNum < 1000 || priceNum > 1000000000) {
    addFormError.value = 'Giá bán phải từ 1,000 VNĐ đến 1,000,000,000 VNĐ!'
    return
  }

  const isDuplicate = variants.value.some(v =>
    (v.idSanPham === Number(targetIdSanPham) || !v.idSanPham) &&
    Number(v.idMauSac) === Number(addForm.idMauSac) &&
    Number(v.idKichCo) === Number(addForm.idKichCo)
  )
  if (isDuplicate) {
    const colorObj = colorsList.value.find(c => c.id === Number(addForm.idMauSac))
    const sizeObj = sizesList.value.find(s => s.id === Number(addForm.idKichCo))
    addFormError.value = `Biến thể [Màu: ${colorObj?.ten || colorObj?.tenMau}, Size: ${sizeObj?.ten || sizeObj?.tenKichCo}] đã tồn tại trong sản phẩm này!`
    return
  }

  isSubmittingAdd.value = true
  try {
    const payload = [{
      idSanPham: Number(targetIdSanPham),
      idMauSac: Number(addForm.idMauSac),
      idKichCo: Number(addForm.idKichCo),
      soLuong: stockNum,
      giaBan: priceNum,
      trangThai: addForm.trangThai,
      hinhAnh: addForm.imagePreview || null
    }]

    const res = await fetch(`${API_BASE}/san-pham-chi-tiet/batch`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      isAddModalOpen.value = false
      notifySuccess('Thêm mới biến thể sản phẩm thành công!')
      fetchVariants(false)
    } else {
      const err = await res.json().catch(() => ({}))
      addFormError.value = err.message || 'Không thể thêm biến thể mới'
      notifyError(err.message || 'Thêm biến thể thất bại')
    }
  } catch (err) {
    console.error('Error adding variant:', err)
    addFormError.value = 'Lỗi kết nối máy chủ'
    notifyError('Không thể kết nối đến máy chủ')
  } finally {
    isSubmittingAdd.value = false
  }
}

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
    if (editForm.stock === null || editForm.stock === undefined || !Number.isInteger(stockNum) || stockNum < 0 || stockNum > 100000) {
      formError.value = 'Số lượng tồn phải là số nguyên từ 0 đến 100,000!'
      return
    }
    const priceNum = Number(editForm.price)
    if (editForm.price === null || editForm.price === undefined || !Number.isFinite(priceNum) || priceNum < 1000 || priceNum > 1000000000) {
      formError.value = 'Giá bán phải từ 1,000 VNĐ đến 1,000,000,000 VNĐ!'
      return
    }

    const currentId = editingVariantId.value
    const targetIdx = variants.value.findIndex(v => v.id === currentId)
    if (targetIdx === -1) return

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

const resetFilters = () => {
  filters.search = ''
  filters.idSanPham = ''
  filters.idMauSac = ''
  filters.idKichCo = ''
  filters.trangThai = ''
  currentPage.value = 0
  fetchVariants(false)
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
        <h1 class="heading-title">Biến thể sản phẩm</h1>
      </div>

      <div class="top-actions-group">
        <button class="btn btn-primary-add" @click="openAddVariantModal" title="Thêm biến thể mới cho sản phẩm này">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Thêm biến thể
        </button>

        <button class="btn btn-outline-refresh" @click="fetchVariants(false)" :class="{ 'is-loading': isLoading }"
          title="Tải lại danh sách">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"></polyline>
            <polyline points="1 20 1 14 7 14"></polyline>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
          Làm mới
        </button>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-row-top">
        <div class="search-col">
          <label class="filter-lbl">Tìm kiếm biến thể</label>
          <div class="search-row-box">
            <div class="search-input-wrap">
              <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input v-model="filters.search" type="text" class="form-control"
                placeholder="Nhập mã sản phẩm, mã CTSP, màu sắc..." @input="onSearchInput" />
              <button v-if="filters.search" class="clear-search-btn" @click="filters.search = ''; onFilterChange()"
                title="Xóa tìm kiếm">×</button>
            </div>

            <button type="button" class="btn-visual-search-standalone" @click="openVisualSearch"
              title="Nhận diện & tìm biến thể bằng ảnh (AI Vision)">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                <circle cx="12" cy="13" r="4"></circle>
              </svg>
              <span>Tìm bằng ảnh</span>
            </button>
          </div>
        </div>

        <div class="filter-actions-group">
          <button class="btn-card-action" @click="resetFilters" title="Đặt lại bộ lọc về mặc định">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6"></path>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
            </svg>
            Đặt lại bộ lọc
          </button>

          <transition name="fade">
            <button v-if="selectedIds.length > 0" class="btn-card-action btn-card-qr" @click="openBatchQrModal"
              :disabled="isGeneratingQr" title="Xem và tải mã QR cho các biến thể đã chọn">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
              </svg>
              {{ isGeneratingQr ? 'Đang tạo QR...' : `Tải QR (${selectedIds.length})` }}
            </button>
          </transition>

          <button class="btn-card-action btn-card-excel" @click="exportVariantsToExcel" :disabled="isExporting"
            title="Xuất danh sách biến thể ra file Excel">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="8" y1="13" x2="16" y2="13"></line>
              <line x1="8" y1="17" x2="16" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            {{ isExporting ? 'Đang xuất...' : (selectedIds.length > 0 ? `Xuất Excel (${selectedIds.length})` : 'Xuất Excel') }}
          </button>
        </div>
      </div>

      <div class="filter-row-bottom">
        <div class="filter-col">
          <label class="filter-lbl">Sản phẩm</label>
          <SearchableSelect
            v-model="filters.idSanPham"
            :options="productsList"
            label-key="tenSanPham"
            value-key="id"
            placeholder="Tất cả sản phẩm"
            search-placeholder="Tìm sản phẩm..."
            @change="onFilterChange"
          />
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Màu sắc</label>
          <SearchableSelect v-model="filters.idMauSac" :options="colorsList" label-key="ten" value-key="id"
            color-key="ma" placeholder="Tất cả màu sắc" search-placeholder="Tìm màu sắc..." @change="onFilterChange" />
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Kích cỡ</label>
          <SearchableSelect v-model="filters.idKichCo" :options="sizesList" label-key="ten" value-key="id"
            prefix="Size " placeholder="Tất cả kích cỡ" search-placeholder="Tìm size..." @change="onFilterChange" />
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Trạng thái</label>
          <SearchableSelect
            v-model="filters.trangThai"
            :options="[
              { value: true, label: 'Đang kinh doanh' },
              { value: false, label: 'Ngừng kinh doanh' }
            ]"
            label-key="label"
            value-key="value"
            placeholder="Tất cả trạng thái"
            @change="onFilterChange"
          />
        </div>
      </div>
    </div>

    <!-- Table Card -->
    <div class="table-card">
      <div class="table-card-head">
        <div class="head-left">
          <h2 class="card-title">Danh sách biến thể chi tiết</h2>
          <span class="items-count-pill">{{ variants.length }}/{{ totalElements }} biến thể</span>
          <span v-if="selectedIds.length > 0" class="selected-badge">Đã chọn: {{ selectedIds.length }}</span>
          <button v-if="selectedIds.length > 0" class="btn-table-head-qr" @click="openBatchQrModal"
            :disabled="isGeneratingQr" title="Tải mã QR cho các biến thể đã chọn">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            Tải QR ({{ selectedIds.length }})
          </button>
        </div>
      </div>

      <div class="table-responsive">
        <table class="variant-data-table">
          <thead>
            <tr>
              <th width="44" class="text-center">
                <input type="checkbox" v-model="isAllSelected" class="custom-chk" />
              </th>
              <th width="50" class="text-center">STT</th>
              <th width="80" class="text-center">Ảnh</th>
              <th width="110">Mã SP</th>
              <th width="130">Mã CTSP</th>
              <th width="150">Màu sắc</th>
              <th width="80" class="text-center">Kích cỡ</th>
              <th width="100" class="text-center">Số lượng</th>
              <th width="130" class="text-end">Giá bán</th>
              <th width="140" class="text-center">Trạng thái</th>
              <th width="160" class="text-center">Hành động</th>
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
                <td class="text-center">
                  <div class="skel-box skel-img"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-num"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-num"></div>
                </td>
                <td>
                  <div class="skel-box skel-text"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-pill"></div>
                </td>
                <td class="text-center">
                  <div class="skel-box skel-actions"></div>
                </td>
              </tr>
            </template>

            <template v-else-if="variants.length > 0">
              <tr v-for="(item, idx) in variants" :key="item.id"
                :class="{ 'row-selected': selectedIds.includes(item.id) }">
                <td class="text-center">
                  <input type="checkbox" :value="item.id" v-model="selectedIds" class="custom-chk" />
                </td>
                <td class="text-center index-col">{{ currentPage * pageSize + idx + 1 }}</td>
                <td class="text-center">
                  <div class="variant-thumb-wrapper">
                    <img v-if="item.img" :src="item.img" alt="Thumbnail" class="variant-thumb"
                      @error="item.img = null" />
                    <div v-else class="variant-thumb-placeholder"
                      :style="{ backgroundColor: getColorHex(item) + '22', color: getColorHex(item) }">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="1.8">
                        <path
                          d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z">
                        </path>
                      </svg>
                    </div>
                    <span v-if="hasDiscount(item)" class="image-discount-badge">{{ item.discount }}</span>
                  </div>
                </td>
                <td class="code-col">{{ item.maSp || '---' }}</td>
                <td class="code-ctsp-col">
                  <span class="sku-badge">{{ item.maCtsp || `SPCT0${item.id}` }}</span>
                </td>
                <td>
                  <div class="color-cell">
                    <span class="color-dot" :style="{ backgroundColor: getColorHex(item) }"></span>
                    <span class="color-name">{{ item.color || 'Mặc định' }}</span>
                  </div>
                </td>
                <td class="text-center size-col">
                  <span class="size-pill">{{ item.size || '-' }}</span>
                </td>
                <td class="text-center stock-col">
                  <span class="stock-badge">
                    {{ formatNumber(item.stock !== undefined ? item.stock : 0) }}
                  </span>
                </td>
                <td class="text-end price-val">
                  <div v-if="hasDiscount(item)" class="price-discount-box">
                    <span class="sale-price">{{ formatPrice(item.giaSauGiam || item.price) }}</span>
                    <span class="original-price">{{ formatPrice(item.price || 0) }}</span>
                  </div>
                  <span v-else>{{ formatPrice(item.price || 0) }}</span>
                </td>
                <td class="text-center">
                  <span class="simple-status-badge" :class="item.trangThai ? 'status-active' : 'status-inactive'">
                    {{ item.trangThai ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="action-icon-group">
                    <!-- Switch Toggle -->
                    <label class="switch-toggle" :title="item.trangThai ? 'Click để ngừng kinh doanh' : 'Click để bật kinh doanh'">
                      <input type="checkbox" :checked="item.trangThai" @change="toggleStatus(item)" />
                      <span class="slider round"></span>
                    </label>

                    <!-- QR Button -->
                    <button class="act-circle-btn qr-btn" title="Xem & tải mã QR" @click="openSingleQrModal(item)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="2">
                        <rect x="3" y="3" width="7" height="7"></rect>
                        <rect x="14" y="3" width="7" height="7"></rect>
                        <rect x="14" y="14" width="7" height="7"></rect>
                        <rect x="3" y="14" width="7" height="7"></rect>
                      </svg>
                    </button>

                    <!-- Edit Button -->
                    <button class="act-circle-btn edit-btn" title="Chỉnh sửa biến thể" @click="openEditModal(item)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="2.2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </template>

            <!-- Empty State -->
            <tr v-else>
              <td colspan="11" class="empty-state-cell">
                <div class="empty-state-box">
                  <div class="empty-icon">👟</div>
                  <div class="empty-title">Không tìm thấy biến thể nào</div>
                  <p class="empty-desc">Thử tìm kiếm với từ khóa khác hoặc bấm đặt lại bộ lọc.</p>
                  <button class="btn-action-outline" @click="resetFilters">Đặt lại bộ lọc</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="table-card-foot">
        <PaginationBar
          :page="currentPage"
          :total-pages="totalPages"
          :total-elements="totalElements"
          :page-size="pageSize"
          :sizes="pageSizeOptions"
          :from="variants.length ? currentPage * pageSize + 1 : 0"
          :to="currentPage * pageSize + variants.length"
          label="biến thể"
          @change="changePage"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <!-- QR CODE MODAL (Single or Batch) -->
    <transition name="modal-fade">
      <div v-if="qrModal.isOpen" class="modal-overlay" @click.self="qrModal.isOpen = false">
        <div class="modal-dialog qr-dialog">
          <div class="modal-header">
            <div class="modal-title-group">
              <h3 class="modal-title">{{ qrModal.title }}</h3>
              <p v-if="qrModal.subtitle" class="modal-subtitle">{{ qrModal.subtitle }}</p>
            </div>
            <button class="modal-close" @click="qrModal.isOpen = false">×</button>
          </div>

          <div class="modal-body qr-modal-body">
            <div class="qr-gallery-grid" :class="{ 'single-view': qrModal.items.length === 1 }">
              <div v-for="qrItem in qrModal.items" :key="qrItem.id" class="qr-card-item">
                <div class="qr-image-wrapper">
                  <img :src="qrItem.qrDataUrl" alt="QR Code" class="qr-img-tag" />
                </div>
                <div class="qr-info-box">
                  <div class="qr-sku-tag">{{ qrItem.maCtsp || `SPCT0${qrItem.id}` }}</div>
                  <div class="qr-prod-title">{{ qrItem.tenSp || 'Sản phẩm' }}</div>
                  <div class="qr-attr-pills">
                    <span class="qr-pill color">{{ qrItem.color || 'Màu mặc định' }}</span>
                    <span class="qr-pill size">Size {{ qrItem.size || '-' }}</span>
                  </div>
                  <div class="qr-price">{{ formatPrice(qrItem.price) }}</div>
                </div>
                <button class="btn-download-single-qr" @click="downloadQrImage(qrItem)" title="Tải ảnh PNG">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="7 10 12 15 17 10"></polyline>
                    <line x1="12" y1="15" x2="12" y2="3"></line>
                  </svg>
                  Tải ảnh QR
                </button>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="qrModal.isOpen = false">Đóng</button>
            <button class="btn btn-outline" @click="printQrLabels">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 6 2 18 2 18 9"></polyline>
                <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                <rect x="6" y="14" width="12" height="8"></rect>
              </svg>
              In tem QR
            </button>
            <button v-if="qrModal.items.length > 1" class="btn btn-primary" @click="downloadAllQrImages">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
              Tải tất cả ảnh QR ({{ qrModal.items.length }})
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Quick Edit Variant Modal -->
    <transition name="modal-fade">
      <div v-if="isEditModalOpen" class="modal-overlay">
        <div class="modal-dialog" @click.stop>
          <div class="modal-header">
            <div class="modal-title-group">
              <h3 class="modal-title">Chỉnh sửa biến thể sản phẩm</h3>
              <span class="modal-code-tag">{{ editForm.maCtsp }}</span>
            </div>
            <button class="modal-close" @click="closeEditModal">×</button>
          </div>

          <div class="modal-body">
            <div v-if="formError" class="modal-alert-error">
              {{ formError }}
            </div>

            <div class="edit-grid">
              <div class="form-group span-2">
                <label class="form-lbl">Mã CTSP (SKU)</label>
                <input v-model="editForm.maCtsp" type="text" class="modal-input input-readonly" readonly disabled />
              </div>

              <div class="form-group">
                <label class="form-lbl">Màu sắc</label>
                <SearchableSelect
                  v-model="editForm.idMauSac"
                  :options="colorsList"
                  label-key="ten"
                  value-key="id"
                  color-key="ma"
                  placeholder="-- Chọn màu sắc --"
                  search-placeholder="Tìm màu sắc..."
                  :allow-clear="false"
                  disabled
                />
              </div>

              <div class="form-group">
                <label class="form-lbl">Kích cỡ</label>
                <SearchableSelect
                  v-model="editForm.idKichCo"
                  :options="sizesList"
                  label-key="ten"
                  value-key="id"
                  prefix="Size "
                  placeholder="-- Chọn kích cỡ --"
                  search-placeholder="Tìm size..."
                  :allow-clear="false"
                  disabled
                />
              </div>

              <div class="form-group">
                <label class="form-lbl">Số lượng tồn kho <span class="required">*</span></label>
                <input v-model.number="editForm.stock" type="number" min="0" class="modal-input"
                  placeholder="Nhập số lượng tồn..." />
              </div>

              <div class="form-group">
                <label class="form-lbl">Giá bán (VNĐ) <span class="required">*</span></label>
                <CurrencyInput v-model="editForm.price" placeholder="Nhập giá bán..." />
              </div>

              <div class="form-group span-2">
                <label class="form-lbl">Trạng thái</label>
                <SearchableSelect
                  v-model="editForm.trangThai"
                  :options="[
                    { value: true, label: 'Đang kinh doanh' },
                    { value: false, label: 'Ngừng kinh doanh' }
                  ]"
                  label-key="label"
                  value-key="value"
                  :allow-clear="false"
                />
              </div>
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

    <!-- Add Variant Modal -->
    <transition name="modal-fade">
      <div v-if="isAddModalOpen" class="modal-overlay">
        <div class="modal-dialog" @click.stop>
          <div class="modal-header">
            <div class="modal-title-group">
              <h3 class="modal-title">Thêm biến thể mới</h3>
              <p class="modal-subtitle">
                Sản phẩm: <strong>{{ addFormProduct?.tenSanPham || 'Chọn sản phẩm' }}</strong>
                <span v-if="addFormProduct?.maSanPham" class="modal-code-tag">{{ addFormProduct.maSanPham }}</span>
              </p>
            </div>
            <button class="modal-close" @click="isAddModalOpen = false">×</button>
          </div>

          <div class="modal-body">
            <div v-if="addFormError" class="modal-alert-error">
              {{ addFormError }}
            </div>

            <div class="edit-grid">
              <!-- Nếu chưa lọc theo sản phẩm thì cho chọn sản phẩm -->
              <div class="form-group span-2">
                <label class="form-lbl">Chọn sản phẩm <span class="required">*</span></label>
                <SearchableSelect v-model="addForm.idSanPham" :options="productsList" label-key="tenSanPham"
                  value-key="id" placeholder="-- Chọn sản phẩm --" search-placeholder="Tìm sản phẩm..." />
              </div>

              <div class="form-group">
                <div class="field-label-row">
                  <label class="form-lbl">Màu sắc <span class="required">*</span></label>
                  <button type="button" class="btn-quick-add-link" @click="openQuickAdd('color')"
                    title="Thêm nhanh màu sắc">+ Thêm nhanh</button>
                </div>
                <SearchableSelect v-model="addForm.idMauSac" :options="colorsList" label-key="ten" value-key="id"
                  color-key="ma" placeholder="-- Chọn màu sắc --" search-placeholder="Tìm màu..."
                  @change="onAddColorChange" />
              </div>

              <div class="form-group">
                <div class="field-label-row">
                  <label class="form-lbl">Kích cỡ <span class="required">*</span></label>
                  <button type="button" class="btn-quick-add-link" @click="openQuickAdd('size')"
                    title="Thêm nhanh kích cỡ">+ Thêm nhanh</button>
                </div>
                <SearchableSelect v-model="addForm.idKichCo" :options="sizesList" label-key="ten" value-key="id"
                  prefix="Size " placeholder="-- Chọn kích cỡ --" search-placeholder="Tìm size..." />
              </div>

              <div class="form-group">
                <label class="form-lbl">Số lượng tồn kho <span class="required">*</span></label>
                <input v-model.number="addForm.stock" type="number" min="0" class="modal-input"
                  placeholder="Nhập số lượng..." />
              </div>

              <div class="form-group">
                <label class="form-lbl">Giá bán (VNĐ) <span class="required">*</span></label>
                <CurrencyInput v-model="addForm.price" placeholder="Nhập giá bán..." />
              </div>

              <div class="form-group span-2">
                <label class="form-lbl">Trạng thái</label>
                <SearchableSelect
                  v-model="addForm.trangThai"
                  :options="[
                    { value: true, label: 'Đang kinh doanh' },
                    { value: false, label: 'Ngừng kinh doanh' }
                  ]"
                  label-key="label"
                  value-key="value"
                  :allow-clear="false"
                />
              </div>

              <!-- PHẦN HÌNH ẢNH BIẾN THỂ -->
              <div class="form-group span-2 image-section-box">
                <label class="form-lbl">Hình ảnh đại diện cho màu sắc</label>

                <!-- Case 1: Đã có ảnh cho màu này từ các biến thể trước -->
                <div v-if="addForm.existingColorImage && !addForm.imagePreview" class="existing-image-box">
                  <img :src="addForm.existingColorImage" class="existing-img-thumb" />
                  <div class="existing-img-info">
                    <span class="badge-auto-img">✓ Tự động dùng lại ảnh của màu này</span>
                    <p class="img-hint">Màu này đã có ảnh đại diện trong sản phẩm. Bạn có thể tải ảnh mới nếu muốn thay
                      đổi.
                    </p>
                  </div>
                  <button type="button" class="btn-change-img" @click="triggerAddImageUpload">Tải ảnh khác</button>
                </div>

                <!-- Case 2: Đã chọn ảnh mới -->
                <div v-else-if="addForm.imagePreview" class="new-image-preview-box">
                  <img :src="addForm.imagePreview" class="existing-img-thumb" />
                  <div class="existing-img-info">
                    <span class="badge-new-img">📷 Ảnh mới đã chọn</span>
                    <p class="img-hint">Ảnh này sẽ được lưu cho phân loại màu này.</p>
                  </div>
                  <button type="button" class="btn-remove-img" @click="addForm.imagePreview = null">Xóa ảnh này</button>
                </div>

                <!-- Case 3: Màu chưa có ảnh và chưa upload -->
                <div v-else class="upload-image-box" @click="triggerAddImageUpload">
                  <div class="upload-icon">🖼</div>
                  <p class="upload-text">Nhấn để tải lên ảnh cho màu này (Tùy chọn)</p>
                  <p class="upload-sub">Hỗ trợ JPG, PNG, WEBP (Tối đa 5MB)</p>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="isAddModalOpen = false" :disabled="isSubmittingAdd">Hủy
              bỏ</button>
            <button class="btn btn-primary" @click="saveAddVariant" :disabled="isSubmittingAdd">
              <span v-if="isSubmittingAdd">Đang lưu...</span>
              <span v-else>+ Thêm biến thể</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Quick Add Modal for Color and Size -->
    <QuickAddModal :is-open="isQuickAddModalOpen" :type="quickAddModalType" @close="isQuickAddModalOpen = false"
      @add="handleQuickAttributeAdded" />
  </div>
</template>

<style scoped src="@/assets/styles/ProductVariantPage.css"></style>
