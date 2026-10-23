<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import QRCode from 'qrcode'
import { Html5Qrcode } from 'html5-qrcode'
import http, { API_ORIGIN } from '@/api/http'
import { getProvinces, getWards, getServices, tinhPhi } from '@/api/shipping'
import { DS_63_TINH_THANH } from '@/api/diaGioi'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import { formatVnd, formatDateTime } from '@/utils/format'
import { getDangMo, moCa, getTongKet, ketCa } from '@/api/giaoCa'
import SearchableSelect from '@/components/SearchableSelect.vue'

import { getUser } from '@/utils/auth'
import { tenVaiTro } from '@/utils/session'

const API_BASE = `${API_ORIGIN}/api/v1`

const staffUser = computed(() => getUser())
const staffName = computed(() => staffUser.value?.tenKhachHang || staffUser.value?.tenTaiKhoan || 'Nhân viên')
const staffRole = computed(() => tenVaiTro())

const clockTimer = ref(null)

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => { })
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen().catch(() => { })
    }
  }
}

const MAX_ORDERS = 5
const pendingOrders = ref([])
const activeOrderIndex = ref(0)

const allProducts = ref([])
const allCustomers = ref([])
const allVouchers = ref([])
const allEmployees = ref([])
const allPaymentMethods = ref([])
const isLoadingData = ref(false)

const brandsList = ref([])
const categoriesList = ref([])
const colorsList = ref([])
const sizesList = ref([])

const currentOrder = computed(() => {
  if (pendingOrders.value.length === 0) return null
  return pendingOrders.value[activeOrderIndex.value] || null
})

const generateOrderCode = () => {
  const d = new Date()
  const timeCode = String(d.getHours()).padStart(2, '0') +
    String(d.getMinutes()).padStart(2, '0') +
    String(d.getSeconds()).padStart(2, '0')
  return `HD${timeCode}`
}

const createNewOrder = () => {
  if (pendingOrders.value.length >= MAX_ORDERS) {
    notifyWarning('Đã đạt giới hạn!', `Chỉ được tạo tối đa ${MAX_ORDERS} hóa đơn chờ cùng lúc.`)
    return
  }

  const newIdx = pendingOrders.value.length + 1
  const newOrder = {
    id: Date.now(),
    code: generateOrderCode(),
    title: `Hóa đơn ${newIdx}`,
    cart: [],
    customer: null,
    isDelivery: false,
    deliveryInfo: {
      recipientName: '',
      recipientPhone: '',
      recipientAddress: '',
      province: null,
      ward: null,
      wardsLoading: false,
      services: [],
      servicesDistrictId: null,
      serviceTypeId: null,
      shippingFee: 0,
      feeLoading: false,
      feeError: '',
      legacy: null
    },
    paymentMethod: 'TIEN_MAT',
    selectedVoucher: null,
    cashGiven: 0,
    transferGiven: 0,
    note: ''
  }

  pendingOrders.value.push(newOrder)
  activeOrderIndex.value = pendingOrders.value.length - 1
  notifySuccess('Đã thêm hóa đơn chờ mới', newOrder.title, 1200)
}

const selectOrderTab = (index) => {
  activeOrderIndex.value = index
}

const closeOrderTab = async (index, event) => {
  if (event) event.stopPropagation()
  const target = pendingOrders.value[index]
  if (!target) return

  if (target.cart.length > 0) {
    const ok = await notifyConfirm(
      'Hủy hóa đơn chờ?',
      `Hóa đơn "${target.title}" đang có ${target.cart.length} sản phẩm. Toàn bộ sản phẩm sẽ được hoàn trả lại tồn kho. Bạn có chắc muốn xóa?`,
      'Xóa hóa đơn',
      'Giữ lại'
    )
    if (!ok) return
  }

  target.cart.forEach(item => {
    const targetProd = allProducts.value.find(p => p.id === item.idCtsp)
    if (targetProd) {
      if (targetProd.stock !== undefined) targetProd.stock += item.quantity
      if (targetProd.soLuong !== undefined) targetProd.soLuong += item.quantity
    }
  })

  pendingOrders.value.splice(index, 1)
  if (activeOrderIndex.value >= pendingOrders.value.length) {
    activeOrderIndex.value = Math.max(0, pendingOrders.value.length - 1)
  }
}

const subTotal = computed(() => {
  if (!currentOrder.value) return 0
  return currentOrder.value.cart.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

const totalCartQty = computed(() => {
  if (!currentOrder.value) return 0
  return currentOrder.value.cart.reduce((sum, item) => sum + item.quantity, 0)
})

const discountAmount = computed(() => {
  if (!currentOrder.value || !currentOrder.value.selectedVoucher) return 0
  const v = currentOrder.value.selectedVoucher
  if (subTotal.value < (v.hoaDonToiThieu || 0)) return 0

  let discount = 0
  if (v.loaiGiamGia === 'PERCENT' || v.loaiGiamGia === '%') {
    discount = (subTotal.value * (v.giaTriGiam || 0)) / 100
    if (v.giamToiDa && discount > v.giamToiDa) discount = v.giamToiDa
  } else {
    discount = v.giaTriGiam || 0
  }
  return Math.min(discount, subTotal.value)
})

const finalTotal = computed(() => {
  if (!currentOrder.value) return 0
  let total = subTotal.value - discountAmount.value
  if (currentOrder.value.isDelivery) {
    total += Number(currentOrder.value.deliveryInfo.shippingFee || 0)
  }
  return Math.max(0, total)
})

const changeDue = computed(() => {
  if (!currentOrder.value) return 0
  if (currentOrder.value.paymentMethod === 'TIEN_MAT') {
    return (currentOrder.value.cashGiven || 0) - finalTotal.value
  } else if (currentOrder.value.paymentMethod === 'CHUYEN_KHOAN') {
    return 0
  } else {
    const totalPaid = Number(currentOrder.value.cashGiven || 0) + Number(currentOrder.value.transferGiven || 0)
    return totalPaid - finalTotal.value
  }
})

const getEffectivePrice = (item) => {
  if (item.giaSauGiam !== null && item.giaSauGiam !== undefined) {
    return Number(item.giaSauGiam)
  }
  const basePrice = Number(item.price || item.giaBan || 0)
  if (item.discount) {
    const discNum = parseFloat(item.discount.toString().replace(/[^0-9.-]+/g, ''))
    if (!isNaN(discNum) && discNum > 0) {
      if (item.discount.toString().includes('%')) {
        return Math.round(basePrice * (1 - discNum / 100))
      } else {
        return Math.max(0, basePrice - discNum)
      }
    }
  }
  return basePrice
}

const addToCart = (productVariant, qtyToAdd = 1, silent = false) => {
  if (!currentOrder.value) {
    createNewOrder()
  }

  const targetProd = allProducts.value.find(p => p.id === productVariant.id)
  const currentAvailableStock = targetProd
    ? (targetProd.stock !== undefined ? targetProd.stock : (targetProd.soLuong || 0))
    : (productVariant.stock !== undefined ? productVariant.stock : (productVariant.soLuong || 0))

  if (currentAvailableStock < qtyToAdd) {
    notifyWarning('Tồn kho không đủ!', `Sản phẩm này chỉ còn ${currentAvailableStock} sản phẩm trong kho.`)
    return
  }

  if (targetProd) {
    if (targetProd.stock !== undefined) targetProd.stock -= qtyToAdd
    if (targetProd.soLuong !== undefined) targetProd.soLuong -= qtyToAdd
  }

  const cart = currentOrder.value.cart
  const existingItem = cart.find(i => i.idCtsp === productVariant.id)
  const effectivePrice = getEffectivePrice(productVariant)
  const prodName = productVariant.tenSp || productVariant.tenSanPham || 'Sản phẩm'

  if (existingItem) {
    existingItem.quantity += qtyToAdd
  } else {
    cart.push({
      idCtsp: productVariant.id,
      idSanPham: productVariant.idSanPham || productVariant.sanPham?.id || (targetProd ? targetProd.idSanPham : null),
      maSp: productVariant.maSp || (targetProd ? targetProd.maSp : null) || `SP0${productVariant.id}`,
      maCtsp: productVariant.maCtsp || productVariant.maChiTietSanPham || `CTSP${productVariant.id}`,
      name: prodName,
      color: productVariant.color || productVariant.tenMau || '---',
      colorHex: productVariant.colorHex || '',
      size: productVariant.size || productVariant.tenKichCo || '---',
      price: effectivePrice,
      originalPrice: Number(productVariant.price || productVariant.giaBan || 0),
      discount: productVariant.discount || null,
      quantity: qtyToAdd,
      img: productVariant.img || ''
    })
  }

  if (!silent) {
    notifySuccess('Thêm thành công', `Đã thêm "${prodName}" (+${qtyToAdd}) vào giỏ hàng`, 1500)
  }
}

const updateCartItemQty = async (item, newQty) => {
  if (newQty <= 0) {
    await removeCartItem(item)
    return
  }

  const delta = newQty - item.quantity
  const targetProd = allProducts.value.find(p => p.id === item.idCtsp)
  const currentAvailable = targetProd
    ? (targetProd.stock !== undefined ? targetProd.stock : (targetProd.soLuong || 0))
    : 0

  if (delta > 0) {
    if (delta > currentAvailable) {
      notifyWarning('Vượt quá tồn kho!', `Chỉ còn thêm ${currentAvailable} sản phẩm trong kho.`)
      const actualAdd = currentAvailable
      if (actualAdd > 0) {
        if (targetProd) {
          if (targetProd.stock !== undefined) targetProd.stock -= actualAdd
          if (targetProd.soLuong !== undefined) targetProd.soLuong -= actualAdd
        }
        item.quantity += actualAdd
      }
      return
    }

    if (targetProd) {
      if (targetProd.stock !== undefined) targetProd.stock -= delta
      if (targetProd.soLuong !== undefined) targetProd.soLuong -= delta
    }
    item.quantity = newQty
  } else if (delta < 0) {
    const returnQty = Math.abs(delta)
    if (targetProd) {
      if (targetProd.stock !== undefined) targetProd.stock += returnQty
      if (targetProd.soLuong !== undefined) targetProd.soLuong += returnQty
    }
    item.quantity = newQty
  }
}

const removeCartItem = async (item) => {
  if (!currentOrder.value) return
  const prodName = item.name || 'sản phẩm này'
  const ok = await notifyConfirm(
    'Xác nhận xóa sản phẩm?',
    `Bạn có chắc chắn muốn xóa "${prodName}" khỏi giỏ hàng không?`,
    'Xóa ngay',
    'Giữ lại'
  )
  if (!ok) return

  const idx = currentOrder.value.cart.findIndex(i => i.idCtsp === item.idCtsp)
  if (idx !== -1) {
    const targetProd = allProducts.value.find(p => p.id === item.idCtsp)
    if (targetProd) {
      if (targetProd.stock !== undefined) targetProd.stock += item.quantity
      if (targetProd.soLuong !== undefined) targetProd.soLuong += item.quantity
    }
    currentOrder.value.cart.splice(idx, 1)
    notifySuccess('Xóa thành công', `Đã xóa "${prodName}" khỏi giỏ hàng`, 1500)
  }
}

const clearCurrentCart = async () => {
  if (!currentOrder.value || currentOrder.value.cart.length === 0) return
  const ok = await notifyConfirm(
    'Làm trống giỏ hàng?',
    'Toàn bộ sản phẩm trong giỏ hàng sẽ được hoàn trả lại tồn kho!',
    'Làm trống',
    'Giữ lại'
  )
  if (ok) {
    currentOrder.value.cart.forEach(item => {
      const targetProd = allProducts.value.find(p => p.id === item.idCtsp)
      if (targetProd) {
        if (targetProd.stock !== undefined) targetProd.stock += item.quantity
        if (targetProd.soLuong !== undefined) targetProd.soLuong += item.quantity
      }
    })
    currentOrder.value.cart = []
    notifySuccess('Đã làm trống giỏ hàng')
  }
}

const barcodeSearchText = ref('')
const handleBarcodeSearchSubmit = async () => {
  const code = barcodeSearchText.value.trim()
  if (!code) return
  let found = findProductByScannedCode(code) || allProducts.value.find(p => {
    const name = (p.tenSp || p.tenSanPham || '').toLowerCase()
    return name.includes(code.toLowerCase())
  })

  if (!found) {
    try {
      const res = await fetch(`${API_BASE}/san-pham-chi-tiet?search=${encodeURIComponent(code)}&size=5`)
      if (res.ok) {
        const data = await res.json()
        if (data && data.content && data.content.length > 0) {
          found = data.content[0]
          if (!allProducts.value.some(p => p.id === found.id)) {
            allProducts.value.push(found)
          }
        }
      }
    } catch { }
  }

  if (found) {
    const stock = found.stock !== undefined ? found.stock : (found.soLuong || 0)
    if (stock <= 0) {
      notifyWarning('Hết hàng', `Sản phẩm "${found.tenSp || found.tenSanPham}" đã hết tồn kho!`)
      return
    }
    addToCart(found, 1)
    barcodeSearchText.value = ''
  } else {
    notifyWarning('Không tìm thấy', `Không tìm thấy sản phẩm nào khớp với mã "${code}"`)
  }
}

const getRemainingStock = (idCtsp) => {
  const prod = allProducts.value.find(p => p.id === idCtsp)
  return prod ? (prod.stock !== undefined ? prod.stock : (prod.soLuong || 0)) : 0
}

const setQuickCash = (amount) => {
  if (!currentOrder.value) return
  if (amount === 'EXACT') {
    currentOrder.value.cashGiven = finalTotal.value
  } else {
    currentOrder.value.cashGiven = Number(amount)
  }
}

const customerSearchQuery = ref('')
const showCustomerDropdown = ref(false)
const isAddCustomerModalOpen = ref(false)
const newCustomerForm = reactive({
  tenKhachHang: '',
  soDienThoai: '',
  email: '',
  gioiTinh: 'Nam',
  ngaySinh: '',
  diaChi: ''
})

const filteredCustomers = computed(() => {
  const q = customerSearchQuery.value.trim().toLowerCase()
  if (!q) return allCustomers.value.slice(0, 8)
  return allCustomers.value.filter(c =>
    (c.ten && c.ten.toLowerCase().includes(q)) ||
    (c.soDienThoai && c.soDienThoai.includes(q))
  ).slice(0, 8)
})

const selectCustomer = (cust) => {
  if (!currentOrder.value) createNewOrder()
  currentOrder.value.customer = cust
  customerSearchQuery.value = ''
  showCustomerDropdown.value = false
  if (currentOrder.value.isDelivery && cust) {
    currentOrder.value.deliveryInfo.recipientName = cust.ten
    currentOrder.value.deliveryInfo.recipientPhone = cust.soDienThoai
  }
}

const removeSelectedCustomer = () => {
  if (currentOrder.value) {
    currentOrder.value.customer = null
  }
}

const isSavingCustomer = ref(false)

const saveQuickCustomer = async () => {
  if (!newCustomerForm.tenKhachHang.trim()) {
    notifyWarning('Thiếu thông tin', 'Vui lòng nhập tên khách hàng!')
    return
  }
  const phone = newCustomerForm.soDienThoai.trim()
  if (!phone) {
    notifyWarning('Thiếu thông tin', 'Vui lòng nhập số điện thoại khách hàng!')
    return
  }
  if (!/^0\d{9,10}$/.test(phone)) {
    notifyWarning('Số điện thoại không hợp lệ', 'Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng số 0!')
    return
  }

  isSavingCustomer.value = true
  try {
    const payload = {
      tenKhachHang: newCustomerForm.tenKhachHang.trim(),
      soDienThoai: phone,
      email: newCustomerForm.email?.trim() || undefined,
      gioiTinh: newCustomerForm.gioiTinh || undefined,
      ngaySinh: newCustomerForm.ngaySinh || undefined,
      diaChiCuThe: newCustomerForm.diaChi?.trim() || undefined,
      trangThai: true
    }

    const res = await http.post('/khach-hang', payload)
    const newCust = res.data
    const createdCust = {
      id: newCust.id,
      ten: newCust.tenKhachHang,
      soDienThoai: newCust.soDienThoai,
      email: newCust.email,
      diaChi: newCust.diaChiMacDinh ? newCust.diaChiMacDinh.diaChiCuThe : (newCustomerForm.diaChi?.trim() || '')
    }

    allCustomers.value.unshift(createdCust)
    selectCustomer(createdCust)
    isAddCustomerModalOpen.value = false

    newCustomerForm.tenKhachHang = ''
    newCustomerForm.soDienThoai = ''
    newCustomerForm.email = ''
    newCustomerForm.gioiTinh = 'Nam'
    newCustomerForm.ngaySinh = ''
    newCustomerForm.diaChi = ''

    notifySuccess('Thành công', `Đã thêm khách hàng "${createdCust.ten}" thành công!`)
  } catch (err) {
    console.error('Error creating customer:', err)
    notifyError('Lỗi thêm khách hàng', err.message || 'Không thể tạo khách hàng mới trên máy chủ!')
  } finally {
    isSavingCustomer.value = false
  }
}

const isProductModalOpen = ref(false)
const modalFilters = reactive({
  search: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idMauSac: '',
  idKichCo: '',
  priceRange: ''
})

const modalPageSize = ref(10)
const modalCurrentPage = ref(0)
const pageSizeOptions = [5, 10, 20, 50]

const resetModalFilters = () => {
  modalFilters.search = ''
  modalFilters.idThuongHieu = ''
  modalFilters.idLoaiGiay = ''
  modalFilters.idMauSac = ''
  modalFilters.idKichCo = ''
  modalFilters.priceRange = ''
  modalCurrentPage.value = 0
}

const brandFilterOptions = computed(() => [
  { value: '', label: 'Tất cả thương hiệu' },
  ...(brandsList.value || []).map(b => ({ value: b.id || b.ten, label: b.ten || b.tenThuongHieu }))
])
const categoryFilterOptions = computed(() => [
  { value: '', label: 'Tất cả thể loại' },
  ...(categoriesList.value || []).map(c => ({ value: c.id || c.ten, label: c.ten || c.tenLoaiGiay }))
])
const colorFilterOptions = computed(() => [
  { value: '', label: 'Tất cả màu sắc' },
  ...(colorsList.value || []).map(m => ({ value: m.id || m.ten || m.tenMau, label: m.ten || m.tenMau }))
])
const sizeFilterOptions = computed(() => [
  { value: '', label: 'Tất cả kích cỡ' },
  ...(sizesList.value || []).map(s => ({ value: s.id || s.ten || s.tenKichCo, label: s.ten || s.tenKichCo }))
])
const priceFilterOptions = [
  { value: '', label: 'Tất cả mức giá' },
  { value: '<1m', label: 'Dưới 1.000.000 ₫' },
  { value: '1m-3m', label: '1.000.000 - 3.000.000 ₫' },
  { value: '3m-5m', label: '3.000.000 - 5.000.000 ₫' },
  { value: '>5m', label: 'Trên 5.000.000 ₫' }
]
const pageSizeSelectOptions = pageSizeOptions.map(sz => ({ value: sz, label: `${sz} / trang` }))

const filteredModalVariants = computed(() => {
  let list = allProducts.value || []
  const term = modalFilters.search.trim().toLowerCase()
  if (term) {
    list = list.filter(p =>
      (p.tenSp && p.tenSp.toLowerCase().includes(term)) ||
      (p.tenSanPham && p.tenSanPham.toLowerCase().includes(term)) ||
      (p.maCtsp && p.maCtsp.toLowerCase().includes(term)) ||
      (p.maSp && p.maSp.toLowerCase().includes(term)) ||
      (p.maChiTietSanPham && p.maChiTietSanPham.toLowerCase().includes(term)) ||
      (p.color && p.color.toLowerCase().includes(term)) ||
      (p.size && p.size.toLowerCase().includes(term))
    )
  }

  if (modalFilters.idThuongHieu) {
    list = list.filter(p => String(p.idThuongHieu) === String(modalFilters.idThuongHieu) || (p.tenThuongHieu && p.tenThuongHieu === modalFilters.idThuongHieu))
  }

  if (modalFilters.idLoaiGiay) {
    list = list.filter(p => String(p.idLoaiGiay) === String(modalFilters.idLoaiGiay) || (p.tenLoaiGiay && p.tenLoaiGiay === modalFilters.idLoaiGiay))
  }

  if (modalFilters.idMauSac) {
    list = list.filter(p => String(p.idMauSac) === String(modalFilters.idMauSac) || (p.color && p.color === modalFilters.idMauSac) || (p.tenMau && p.tenMau === modalFilters.idMauSac))
  }

  if (modalFilters.idKichCo) {
    list = list.filter(p => String(p.idKichCo) === String(modalFilters.idKichCo) || (p.size && p.size === modalFilters.idKichCo) || (p.tenKichCo && p.tenKichCo === modalFilters.idKichCo))
  }

  if (modalFilters.priceRange) {
    list = list.filter(p => {
      const pr = getEffectivePrice(p)
      if (modalFilters.priceRange === '<1m') return pr < 1000000
      if (modalFilters.priceRange === '1m-3m') return pr >= 1000000 && pr <= 3000000
      if (modalFilters.priceRange === '3m-5m') return pr > 3000000 && pr <= 5000000
      if (modalFilters.priceRange === '>5m') return pr > 5000000
      return true
    })
  }

  return list
})

const modalTotalElements = computed(() => filteredModalVariants.value.length)
const modalTotalPages = computed(() => Math.max(1, Math.ceil(modalTotalElements.value / modalPageSize.value)))

const paginatedModalVariants = computed(() => {
  const start = modalCurrentPage.value * modalPageSize.value
  return filteredModalVariants.value.slice(start, start + modalPageSize.value)
})

const changeModalPage = (p) => {
  if (p >= 0 && p < modalTotalPages.value) {
    modalCurrentPage.value = p
  }
}

watch([modalFilters, modalPageSize], () => {
  modalCurrentPage.value = 0
}, { deep: true })

const modalSelectedIds = ref([])

const openProductPicker = () => {
  if (!currentOrder.value) {
    createNewOrder()
  }
  resetModalFilters()
  modalSelectedIds.value = []
  isProductModalOpen.value = true
}

const isModalRowSelected = (id) => modalSelectedIds.value.includes(id)

const isAllPageSelected = computed(() => {
  const selectable = paginatedModalVariants.value.filter(p => (p.stock ?? p.soLuong ?? 0) > 0)
  if (selectable.length === 0) return false
  return selectable.every(p => modalSelectedIds.value.includes(p.id))
})

const isIndeterminatePage = computed(() => {
  const selectable = paginatedModalVariants.value.filter(p => (p.stock ?? p.soLuong ?? 0) > 0)
  const selected = selectable.filter(p => modalSelectedIds.value.includes(p.id))
  return selected.length > 0 && selected.length < selectable.length
})

const toggleModalRow = (product, event) => {
  if (event) event.stopPropagation()
  const stock = product.stock !== undefined ? product.stock : (product.soLuong || 0)
  if (stock <= 0) {
    notifyWarning('Hết hàng', 'Sản phẩm này hiện đã hết tồn kho!')
    return
  }
  toggleModalSelect(product.id)
}

const toggleModalSelect = (id) => {
  const i = modalSelectedIds.value.indexOf(id)
  if (i >= 0) modalSelectedIds.value.splice(i, 1)
  else modalSelectedIds.value.push(id)
}

const toggleSelectAllPage = () => {
  const selectable = paginatedModalVariants.value.filter(p => (p.stock ?? p.soLuong ?? 0) > 0)
  if (isAllPageSelected.value) {
    const pageIds = selectable.map(p => p.id)
    modalSelectedIds.value = modalSelectedIds.value.filter(id => !pageIds.includes(id))
  } else {
    const toAdd = selectable.filter(p => !modalSelectedIds.value.includes(p.id)).map(p => p.id)
    modalSelectedIds.value = [...modalSelectedIds.value, ...toAdd]
  }
}

const handleSelectProductFromModal = (product) => {
  const stock = product.stock !== undefined ? product.stock : (product.soLuong || 0)
  if (stock <= 0) {
    notifyWarning('Hết hàng', 'Sản phẩm này hiện đã hết tồn kho!')
    return
  }
  toggleModalSelect(product.id)
}

const closeProductPicker = () => {
  isProductModalOpen.value = false
  modalSelectedIds.value = []
}

const confirmModalSelection = () => {
  const products = modalSelectedIds.value
    .map(id => allProducts.value.find(p => p.id === id))
    .filter(Boolean)

  if (products.length === 0) {
    notifyWarning('Chưa chọn sản phẩm', 'Vui lòng chọn ít nhất 1 sản phẩm để thêm vào giỏ hàng.')
    return
  }

  let added = 0
  let outOfStock = 0

  products.forEach(p => {
    const stock = p.stock !== undefined ? p.stock : (p.soLuong || 0)
    if (stock <= 0) {
      outOfStock++
      return
    }
    addToCart(p, 1, true)
    added++
  })

  isProductModalOpen.value = false
  modalSelectedIds.value = []

  if (outOfStock > 0) {
    notifyWarning(
      'Thêm vào giỏ hàng',
      `Đã thêm ${added} dòng sản phẩm. Bỏ qua ${outOfStock} sản phẩm hết hàng.`
    )
  } else if (added > 0) {
    notifySuccess('Thêm thành công', `Đã thêm ${added} sản phẩm vào giỏ hàng thành công!`, 1500)
  }
}

const getShoeImage = (p) => {
  if (p.img) return p.img
  return 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60'
}

const isQrModalOpen = ref(false)
const isCameraRunning = ref(false)
const cameraError = ref('')
const isProcessingScan = ref(false)
const scanSuccessAnim = ref(false)
const scannedProductName = ref('')
const scannedCodeInput = ref('')
let html5QrScanner = null

const playScannerBeep = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.type = 'sine'
    osc.frequency.value = 1400
    gain.gain.setValueAtTime(0.18, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.16)
    osc.start()
    osc.stop(ctx.currentTime + 0.16)
  } catch { }
}

const findProductByScannedCode = (rawCode) => {
  if (!rawCode) return null
  let code = String(rawCode).trim()

  if (code.startsWith('{') && code.endsWith('}')) {
    try {
      const parsed = JSON.parse(code)
      if (parsed.maCtsp || parsed.id || parsed.ma) {
        const found = allProducts.value.find(p =>
          (parsed.maCtsp && p.maCtsp && p.maCtsp.toLowerCase() === parsed.maCtsp.toLowerCase()) ||
          (parsed.ma && p.ma && p.ma.toLowerCase() === parsed.ma.toLowerCase()) ||
          (parsed.id && p.id && String(p.id) === String(parsed.id)) ||
          (parsed.maChiTietSanPham && p.maChiTietSanPham && p.maChiTietSanPham.toLowerCase() === parsed.maChiTietSanPham.toLowerCase())
        )
        if (found) return found
      }
    } catch { }
  }

  if (code.includes(':')) {
    const parts = code.split(':')
    const lastPart = parts[parts.length - 1].trim()
    if (lastPart) {
      const foundSub = allProducts.value.find(p =>
        (p.maCtsp && p.maCtsp.toLowerCase() === lastPart.toLowerCase()) ||
        (p.ma && p.ma.toLowerCase() === lastPart.toLowerCase()) ||
        (p.maChiTietSanPham && p.maChiTietSanPham.toLowerCase() === lastPart.toLowerCase()) ||
        (p.id && String(p.id) === lastPart)
      )
      if (foundSub) return foundSub
    }
  }

  return allProducts.value.find(p =>
    (p.maCtsp && p.maCtsp.toLowerCase() === code.toLowerCase()) ||
    (p.maChiTietSanPham && p.maChiTietSanPham.toLowerCase() === code.toLowerCase()) ||
    (p.ma && p.ma.toLowerCase() === code.toLowerCase()) ||
    (p.maSp && p.maSp.toLowerCase() === code.toLowerCase()) ||
    (p.id && String(p.id) === code)
  )
}

const handleScannedResult = async (decodedText) => {
  if (!decodedText || isProcessingScan.value) return
  isProcessingScan.value = true

  let found = findProductByScannedCode(decodedText)

  if (!found) {
    try {
      const trimmed = String(decodedText).trim()
      let searchKey = trimmed
      if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
        try {
          const parsed = JSON.parse(trimmed)
          searchKey = parsed.maCtsp || parsed.ma || parsed.id || trimmed
        } catch { }
      }
      const res = await fetch(`${API_BASE}/san-pham-chi-tiet?search=${encodeURIComponent(searchKey)}&size=5`)
      if (res.ok) {
        const data = await res.json()
        if (data && data.content && data.content.length > 0) {
          found = data.content[0]
          if (!allProducts.value.some(p => p.id === found.id)) {
            allProducts.value.push(found)
          }
        }
      }
    } catch (err) {
      console.warn('Tra cứu backend theo mã quét thất bại:', err)
    }
  }

  if (found) {
    playScannerBeep()
    scanSuccessAnim.value = true
    scannedProductName.value = `${found.tenSp || found.tenSanPham || 'Sản phẩm'} (${found.color || found.tenMau || ''} - Size ${found.size || found.tenKichCo || ''})`

    const stock = found.stock !== undefined ? found.stock : (found.soLuong || 0)
    if (stock <= 0) {
      notifyWarning('Hết hàng', `Sản phẩm "${found.tenSp || found.tenSanPham}" đã hết tồn kho!`)
      isProcessingScan.value = false
      scanSuccessAnim.value = false
      return
    }

    addToCart(found, 1)
    await new Promise((resolve) => setTimeout(resolve, 600))
    await closeQrScanner()
    notifySuccess('Đã thêm sản phẩm', `Đã thêm "${found.tenSp || found.tenSanPham}" vào giỏ hàng`, 1500)
  } else {
    notifyError('Không tìm thấy', `Không tìm thấy sản phẩm có mã: "${decodedText}"`)
  }

  isProcessingScan.value = false
  scanSuccessAnim.value = false
}

const startCameraScanner = async () => {
  cameraError.value = ''
  scanSuccessAnim.value = false
  isProcessingScan.value = false

  try {
    let el = document.getElementById('pos-camera-scanner-view')
    if (!el) {
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 150))
      el = document.getElementById('pos-camera-scanner-view')
    }
    if (!el) {
      cameraError.value = 'Không tìm thấy khung hiển thị Camera. Vui lòng thử lại.'
      return
    }

    if (html5QrScanner) {
      await stopCameraScanner()
    }

    html5QrScanner = new Html5Qrcode('pos-camera-scanner-view')
    isCameraRunning.value = true

    let cameraIdOrConfig = { facingMode: 'environment' }
    try {
      const devices = await Html5Qrcode.getCameras()
      if (devices && devices.length > 0) {
        const backCam = devices.find(d => d.label.toLowerCase().includes('back') || d.label.toLowerCase().includes('rear'))
        cameraIdOrConfig = backCam ? backCam.id : devices[0].id
      }
    } catch {
      cameraIdOrConfig = { facingMode: 'environment' }
    }

    await html5QrScanner.start(
      cameraIdOrConfig,
      {
        fps: 25,
        qrbox: (viewfinderWidth, viewfinderHeight) => {
          const minEdge = Math.min(viewfinderWidth, viewfinderHeight)
          const edge = Math.max(180, Math.floor(minEdge * 0.75))
          return { width: edge, height: edge }
        },
      },
      (decodedText) => {
        handleScannedResult(decodedText)
      },
      () => { }
    )
  } catch (err) {
    console.warn('Lỗi bật Camera POS:', err)
    isCameraRunning.value = false
    cameraError.value = 'Không thể mở Camera (chưa cấp quyền truy cập trình duyệt hoặc máy không có Camera).'
  }
}

const stopCameraScanner = async () => {
  try {
    const videoEls = document.querySelectorAll('#pos-camera-scanner-view video, video')
    videoEls.forEach((v) => {
      if (v.srcObject && typeof v.srcObject.getTracks === 'function') {
        v.srcObject.getTracks().forEach((track) => {
          try {
            track.stop()
            track.enabled = false
          } catch { }
        })
        v.srcObject = null
      }
      try {
        v.pause()
        v.removeAttribute('src')
        v.load()
      } catch { }
    })
  } catch (err) {
    console.warn('Lỗi giải phóng media tracks camera:', err)
  }

  if (html5QrScanner) {
    try {
      if (html5QrScanner.isScanning) {
        await Promise.race([
          html5QrScanner.stop().catch(() => { }),
          new Promise((resolve) => setTimeout(resolve, 400)),
        ])
      }
    } catch (e) {
      console.warn('html5QrScanner.stop error:', e)
    }

    try {
      html5QrScanner.clear()
    } catch { }
    html5QrScanner = null
  }

  isCameraRunning.value = false
}

const openQrScanner = async () => {
  if (!currentOrder.value) createNewOrder()
  scannedCodeInput.value = ''
  cameraError.value = ''
  isQrModalOpen.value = true
  await nextTick()
  await startCameraScanner()
}

const closeQrScanner = async () => {
  await stopCameraScanner()
  isQrModalOpen.value = false
  scanSuccessAnim.value = false
  isProcessingScan.value = false
}

const handleManualCodeSubmit = () => {
  const code = scannedCodeInput.value.trim()
  if (!code) return
  handleScannedResult(code)
  scannedCodeInput.value = ''
}

const isSwapModalOpen = ref(false)
const swapCartItem = ref(null)
const swapVariants = ref([])
const isLoadingSwapVariants = ref(false)
const selectedNewVariant = ref(null)
const swapColorFilter = ref('ALL')

const openSwapModal = async (item) => {
  if (!item) return
  swapCartItem.value = item
  selectedNewVariant.value = null
  swapColorFilter.value = 'ALL'
  isSwapModalOpen.value = true
  isLoadingSwapVariants.value = true

  try {
    let idSp = item.idSanPham
    if (!idSp) {
      const matchInAll = allProducts.value.find(p => p.id === item.idCtsp)
      if (matchInAll && matchInAll.idSanPham) {
        idSp = matchInAll.idSanPham
        item.idSanPham = idSp
      }
    }

    let variants = []
    if (idSp) {
      try {
        const res = await fetch(`${API_BASE}/san-pham-chi-tiet/by-san-pham/${idSp}`)
        if (res.ok) {
          variants = await res.json()
        }
      } catch (err) {
        console.warn('Cannot fetch variants by-san-pham, falling back to allProducts:', err)
      }
    }

    if (!variants || variants.length === 0) {
      variants = allProducts.value.filter(p => {
        if (idSp && p.idSanPham === idSp) return true
        if (item.maSp && p.maSp === item.maSp) return true
        return false
      })
    }

    swapVariants.value = (variants || []).map(v => {
      const inAll = allProducts.value.find(p => p.id === v.id)
      const currentStock = inAll
        ? (inAll.stock !== undefined ? inAll.stock : (inAll.soLuong || 0))
        : (v.stock !== undefined ? v.stock : (v.soLuong || 0))
      return {
        ...v,
        currentStock: v.id === item.idCtsp ? currentStock + item.quantity : currentStock
      }
    })
  } catch (err) {
    console.error('Lỗi tải danh sách biến thể để đổi:', err)
  } finally {
    isLoadingSwapVariants.value = false
  }
}

const availableSwapColors = computed(() => {
  const set = new Set()
  swapVariants.value.forEach(v => {
    const c = v.color || v.tenMau
    if (c) set.add(c)
  })
  return Array.from(set)
})

const filteredSwapVariants = computed(() => {
  if (swapColorFilter.value === 'ALL') return swapVariants.value
  return swapVariants.value.filter(v => (v.color || v.tenMau) === swapColorFilter.value)
})

const executeSwap = (newVariant) => {
  if (!swapCartItem.value || !newVariant || !currentOrder.value) return
  const currentItem = swapCartItem.value
  if (newVariant.id === currentItem.idCtsp) {
    notifyWarning('Biến thể trùng khớp', 'Bạn đang chọn đúng biến thể hiện tại!')
    return
  }

  const targetNew = allProducts.value.find(p => p.id === newVariant.id)
  const availableStock = targetNew
    ? (targetNew.stock !== undefined ? targetNew.stock : (targetNew.soLuong || 0))
    : (newVariant.stock !== undefined ? newVariant.stock : (newVariant.soLuong || 0))

  if (availableStock < currentItem.quantity) {
    notifyWarning('Tồn kho không đủ!', `Biến thể này chỉ còn ${availableStock} sản phẩm, không đủ số lượng ${currentItem.quantity} trong giỏ.`)
    return
  }

  const targetOld = allProducts.value.find(p => p.id === currentItem.idCtsp)
  if (targetOld) {
    if (targetOld.stock !== undefined) targetOld.stock += currentItem.quantity
    if (targetOld.soLuong !== undefined) targetOld.soLuong += currentItem.quantity
  }

  if (targetNew) {
    if (targetNew.stock !== undefined) targetNew.stock -= currentItem.quantity
    if (targetNew.soLuong !== undefined) targetNew.soLuong -= currentItem.quantity
  }

  const newEffectivePrice = getEffectivePrice(newVariant)
  const newSize = newVariant.size || newVariant.tenKichCo || '---'
  const newColor = newVariant.color || newVariant.tenMau || '---'

  const existingInCart = currentOrder.value.cart.find(i => i.idCtsp === newVariant.id && i !== currentItem)
  if (existingInCart) {
    existingInCart.quantity += currentItem.quantity
    const oldIdx = currentOrder.value.cart.findIndex(i => i === currentItem)
    if (oldIdx !== -1) {
      currentOrder.value.cart.splice(oldIdx, 1)
    }
    notifySuccess('Gộp biến thể thành công!', `Đã chuyển và gộp ${currentItem.quantity} sản phẩm vào Size ${newSize} - ${newColor}`, 2000)
  } else {
    currentItem.idCtsp = newVariant.id
    currentItem.idSanPham = newVariant.idSanPham || currentItem.idSanPham
    currentItem.maCtsp = newVariant.maCtsp || newVariant.maChiTietSanPham || `CTSP${newVariant.id}`
    currentItem.name = newVariant.tenSp || newVariant.tenSanPham || currentItem.name
    currentItem.color = newColor
    currentItem.colorHex = newVariant.colorHex || ''
    currentItem.size = newSize
    currentItem.price = newEffectivePrice
    currentItem.originalPrice = Number(newVariant.price || newVariant.giaBan || 0)
    currentItem.discount = newVariant.discount || null
    if (newVariant.img) {
      currentItem.img = newVariant.img
    }
    notifySuccess('Đổi biến thể thành công!', `Đã đổi sang Size ${newSize} - Màu ${newColor}`, 2000)
  }

  isSwapModalOpen.value = false
  swapCartItem.value = null
  selectedNewVariant.value = null
}

const storeBankAccounts = ref([
  {
    id: 'MB',
    name: 'MB Bank',
    shortName: 'MB',
    accountNo: '0987654321',
    accountName: 'POLYSHOES STORE',
    color: '#004b8d',
    tag: 'Khuyên dùng'
  },
  {
    id: 'VCB',
    name: 'Vietcombank',
    shortName: 'VCB',
    accountNo: '1012345678',
    accountName: 'POLYSHOES STORE',
    color: '#005f33',
    tag: 'Ngoại thương'
  },
  {
    id: 'TCB',
    name: 'Techcombank',
    shortName: 'TCB',
    accountNo: '19033445566011',
    accountName: 'POLYSHOES STORE',
    color: '#e01a22',
    tag: 'Kỹ thương'
  },
  {
    id: 'ACB',
    name: 'ACB',
    shortName: 'ACB',
    accountNo: '23456789',
    accountName: 'POLYSHOES STORE',
    color: '#0070ba',
    tag: 'Á Châu'
  }
])

const selectedBank = ref(storeBankAccounts.value[0])
const isCustomerDisplayMode = ref(false)
const isPaymentSuccessAnim = ref(false)

const isBankQrModalOpen = ref(false)
const bankQrDataUrl = ref('')
const vietQrNetworkUrl = ref('')
const isQrImgFailed = ref(false)

const bankTransferAmount = computed(() => {
  if (!currentOrder.value) return 0
  if (currentOrder.value.paymentMethod === 'KET_HOP') {
    if (currentOrder.value.transferGiven > 0) return currentOrder.value.transferGiven
    const remaining = finalTotal.value - (currentOrder.value.cashGiven || 0)
    return remaining > 0 ? remaining : finalTotal.value
  }
  return finalTotal.value
})

const bankTransferMemo = computed(() => {
  return currentOrder.value ? currentOrder.value.code : ''
})

const generateBankTransferQr = async () => {
  if (finalTotal.value <= 0 || !currentOrder.value) return
  isQrImgFailed.value = false
  const amount = Math.round(bankTransferAmount.value)
  const memo = bankTransferMemo.value
  const bank = selectedBank.value || storeBankAccounts.value[0]

  vietQrNetworkUrl.value = `https://img.vietqr.io/image/${bank.id}-${bank.accountNo}-compact2.png?amount=${amount}&addInfo=${encodeURIComponent(memo)}&accountName=${encodeURIComponent(bank.accountName)}`

  try {
    const qrString = `2|99|${bank.accountNo}|${bank.accountName}|${memo}|0|0|${amount}|${memo}`
    bankQrDataUrl.value = await QRCode.toDataURL(qrString, { width: 300, margin: 1 })
  } catch (err) {
    console.error('Lỗi sinh QR code offline:', err)
  }

  isBankQrModalOpen.value = true
}

const selectBank = (bank) => {
  selectedBank.value = bank
  generateBankTransferQr()
}

const onQrImgError = () => {
  isQrImgFailed.value = true
}

const playPaymentSuccessSound = () => {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext
    if (!AudioCtx) return
    const ctx = new AudioCtx()
    const playNote = (freq, startTime, duration) => {
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.type = 'triangle'
      osc.frequency.setValueAtTime(freq, startTime)
      gain.gain.setValueAtTime(0, startTime)
      gain.gain.linearRampToValueAtTime(0.4, startTime + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start(startTime)
      osc.stop(startTime + duration)
    }
    const now = ctx.currentTime
    playNote(659.25, now, 0.22)
    playNote(783.99, now + 0.12, 0.25)
    playNote(1046.50, now + 0.24, 0.45)
  } catch (e) {
    console.warn('Audio chime unsupported:', e)
  }
}

const copyToClipboard = async (text, label = 'nội dung') => {
  if (!text) return
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(String(text))
    } else {
      const el = document.createElement('textarea')
      el.value = String(text)
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
    }
    notifySuccess('Đã sao chép!', 'Đã chép ' + label + ' vào bộ nhớ tạm', 1500)
  } catch (err) {
    notifyWarning('Không thể sao chép', 'Vui lòng sao chép thủ công.')
  }
}

const confirmBankPaymentReceived = () => {
  if (currentOrder.value) {
    if (currentOrder.value.paymentMethod === 'KET_HOP') {
      currentOrder.value.transferGiven = bankTransferAmount.value
    }
  }
  playPaymentSuccessSound()
  isPaymentSuccessAnim.value = true
  setTimeout(() => {
    isPaymentSuccessAnim.value = false
    isBankQrModalOpen.value = false
    notifySuccess('Xác nhận thành công!', 'Đã ghi nhận thanh toán ' + formatVnd(bankTransferAmount.value) + ' cho hóa đơn ' + (currentOrder.value?.code || ''), 2500)
  }, 900)
}

watch(() => currentOrder.value?.paymentMethod, (newMethod) => {
  if (newMethod === 'CHUYEN_KHOAN' && finalTotal.value > 0) {
    generateBankTransferQr()
  }
})

const provincesList = ref([])
const wardsList = ref([])

const loadProvinces = async () => {
  try {
    const list = await getProvinces()
    if (Array.isArray(list) && list.length > 0) {
      provincesList.value = list
      return
    }
  } catch (e) {
    console.warn('Backend /shipping/provinces failed, falling back to open-api...', e)
  }

  try {
    const res = await fetch('https://provinces.open-api.vn/api/v1/p/')
    if (res.ok) {
      const data = await res.json()
      if (Array.isArray(data) && data.length > 0) {
        provincesList.value = data.map(p => ({ id: p.code, name: p.name }))
        return
      }
    }
  } catch (err) {
    console.warn('Open API fallback failed, using local Vietnam list...', err)
  }

  provincesList.value = DS_63_TINH_THANH.map((p, idx) => ({
    id: p.subCodes?.[0] || (idx + 1),
    name: p.ten
  }))
}

const onProvinceChange = async () => {
  const d = currentOrder.value?.deliveryInfo
  if (!d) return
  d.ward = null
  wardsList.value = []
  d.shippingFee = 0
  d.legacy = null
  d.feeError = ''
  d.services = []
  d.servicesDistrictId = null
  d.serviceTypeId = null
  if (d.province) {
    d.wardsLoading = true
    try {
      const list = await getWards(d.province.id)
      const isDummy = Array.isArray(list) && list.length === 2 && list[0]?.id === 1 && list[1]?.id === 2
      if (Array.isArray(list) && list.length > 0 && !isDummy) {
        wardsList.value = list
        d.wardsLoading = false
        return
      }
    } catch {
    }

    try {
      const res = await fetch(`https://provinces.open-api.vn/api/v1/p/${d.province.id}?depth=3`)
      if (res.ok) {
        const pData = await res.json()
        const allWards = []
        if (pData.districts && Array.isArray(pData.districts)) {
          for (const dist of pData.districts) {
            if (dist.wards && Array.isArray(dist.wards)) {
              for (const w of dist.wards) {
                allWards.push({ id: w.code, name: `${w.name} (${dist.name})` })
              }
            }
          }
        }
        if (allWards.length > 0) {
          wardsList.value = allWards
          d.wardsLoading = false
          return
        }
      }
    } catch {
    }

    wardsList.value = [
      { id: 1, name: 'Phường/Xã trung tâm' },
      { id: 2, name: 'Khu vực nội thành' }
    ]
    d.wardsLoading = false
  }
}

const tinhPhiVanChuyen = async () => {
  const d = currentOrder.value?.deliveryInfo
  if (!d || !d.province || !d.ward) return
  if (!currentOrder.value.cart || currentOrder.value.cart.length === 0) {
    d.feeError = 'Cần thêm sản phẩm vào giỏ để tính phí vận chuyển'
    d.shippingFee = 0
    d.legacy = null
    return
  }
  d.feeLoading = true
  d.feeError = ''
  try {
    const res = await tinhPhi({
      toProvinceName: d.province.name,
      toWardName: d.ward.name,
      serviceTypeId: d.serviceTypeId || undefined
    })
    d.shippingFee = res.shippingFee || 30000
    d.legacy = {
      toProvinceId: res.toProvinceId || d.province.id || 1,
      toDistrictId: res.toDistrictId || 1,
      toWardCode: String(res.toWardCode || d.ward.id || '001')
    }
    if (res.toDistrictId && d.servicesDistrictId !== res.toDistrictId) {
      try {
        d.services = await getServices(res.toDistrictId)
        d.servicesDistrictId = res.toDistrictId
      } catch {
        d.services = [{ serviceTypeId: 2, shortName: 'Giao hàng tiêu chuẩn' }]
      }
    }
  } catch (e) {
    d.shippingFee = 30000
    d.legacy = {
      toProvinceId: d.province.id || 1,
      toDistrictId: 1,
      toWardCode: String(d.ward.id || '001')
    }
    d.services = [{ serviceTypeId: 2, shortName: 'Giao hàng tiêu chuẩn' }]
    d.feeError = ''
  } finally {
    d.feeLoading = false
  }
}

const onWardChange = () => { tinhPhiVanChuyen() }
const onServiceChange = () => { tinhPhiVanChuyen() }

const provincesForSelect = computed(() =>
  (provincesList.value || []).map(p => ({ value: p.id, label: p.name, raw: p }))
)
const wardsForSelect = computed(() =>
  (wardsList.value || []).map(w => ({ value: w.id, label: w.name, raw: w }))
)
const serviceOptionsForOrder = computed(() => {
  const d = currentOrder.value?.deliveryInfo
  return (d?.services || []).map(s => ({
    value: s.serviceTypeId,
    label: s.shortName || ('Dịch vụ ' + s.serviceTypeId)
  }))
})
const onProvinceSelectChange = (val, opt) => {
  const d = currentOrder.value?.deliveryInfo
  if (!d) return
  d.province = opt ? opt.raw : null
  onProvinceChange()
}
const onWardSelectChange = (val, opt) => {
  const d = currentOrder.value?.deliveryInfo
  if (!d) return
  d.ward = opt ? opt.raw : null
  onWardChange()
}

watch(() => currentOrder.value?.cart?.length, () => {
  const d = currentOrder.value?.deliveryInfo
  if (d && d.province && d.ward && !d.feeLoading) {
    tinhPhiVanChuyen()
  }
})


const isReceiptModalOpen = ref(false)
const completedInvoiceData = ref(null)
const isProcessingCheckout = ref(false)

const handleCancelOrder = async () => {
  if (!currentOrder.value) return
  const ok = await notifyConfirm(
    'Hủy đơn hàng này?',
    'Toàn bộ sản phẩm trong giỏ hàng hiện tại sẽ được hoàn trả lại tồn kho!',
    'Đồng ý hủy',
    'Không'
  )
  if (ok) {
    currentOrder.value.cart.forEach(item => {
      const targetProd = allProducts.value.find(p => p.id === item.idCtsp)
      if (targetProd) {
        if (targetProd.stock !== undefined) targetProd.stock += item.quantity
        if (targetProd.soLuong !== undefined) targetProd.soLuong += item.quantity
      }
    })
    currentOrder.value.cart = []
    currentOrder.value.customer = null
    currentOrder.value.cashGiven = 0
    currentOrder.value.transferGiven = 0
    currentOrder.value.note = ''
    notifySuccess('Đã làm mới giỏ hàng và hoàn trả tồn kho')
  }
}

const getPaymentMethodId = (methodType) => {
  if (!allPaymentMethods.value || allPaymentMethods.value.length === 0) return 1
  if (methodType === 'TIEN_MAT') {
    const found = allPaymentMethods.value.find(p => p.ten && p.ten.toLowerCase().includes('tiền mặt'))
    return found ? found.id : (allPaymentMethods.value[0]?.id || 1)
  }
  if (methodType === 'CHUYEN_KHOAN') {
    const found = allPaymentMethods.value.find(p => p.ten && p.ten.toLowerCase().includes('chuyển khoản'))
    return found ? found.id : (allPaymentMethods.value[1]?.id || 2)
  }
  return allPaymentMethods.value[0]?.id || 1
}

const isVoucherModalOpen = ref(false)
const voucherSearchQuery = ref('')
const voucherInputCode = ref('')

const formatDateVn = (dateStr) => {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const year = d.getFullYear()
    const hour = String(d.getHours()).padStart(2, '0')
    const minute = String(d.getMinutes()).padStart(2, '0')
    return `${day}/${month}/${year} ${hour}:${minute}`
  } catch {
    return dateStr
  }
}

const formatVoucherValue = (v) => {
  if (!v) return ''
  if (v.loaiGiamGia === 'PERCENT' || v.loaiGiamGia === '%') {
    return `${Number(v.giaTriGiam || 0)}%`
  }
  return formatVnd(Number(v.giaTriGiam || 0))
}

const filteredVouchers = computed(() => {
  const q = voucherSearchQuery.value.trim().toLowerCase()
  if (!q) return allVouchers.value
  return allVouchers.value.filter(v =>
    (v.ma && v.ma.toLowerCase().includes(q)) ||
    (v.ten && v.ten.toLowerCase().includes(q)) ||
    (v.maPhieuGiamGia && v.maPhieuGiamGia.toLowerCase().includes(q))
  )
})

const calculateVoucherDiscount = (voucher, currentSubTotal) => {
  if (!voucher) return 0
  const sub = currentSubTotal !== undefined ? currentSubTotal : subTotal.value
  const minRequired = Number(voucher.hoaDonToiThieu || 0)
  if (sub < minRequired) return 0

  let discount = 0
  if (voucher.loaiGiamGia === 'PERCENT' || voucher.loaiGiamGia === '%') {
    discount = (sub * (Number(voucher.giaTriGiam) || 0)) / 100
    if (voucher.giamToiDa && discount > Number(voucher.giamToiDa)) {
      discount = Number(voucher.giamToiDa)
    }
  } else {
    discount = Number(voucher.giaTriGiam) || 0
  }
  return Math.min(discount, sub)
}

const isVoucherEligible = (v) => {
  if (!currentOrder.value) return false
  if (v.trangThai === false) return false

  const now = new Date()
  if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false
  if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false
  if (v.soLuong && v.soLuongDaSuDung !== undefined && v.soLuongDaSuDung >= v.soLuong) return false

  const minRequired = Number(v.hoaDonToiThieu || 0)
  return subTotal.value >= minRequired
}

const availableEligibleVouchersCount = computed(() => {
  return allVouchers.value.filter(v => isVoucherEligible(v)).length
})

const bestVoucherSuggestion = computed(() => {
  if (!currentOrder.value || subTotal.value <= 0 || allVouchers.value.length === 0) return null

  const now = new Date()
  const validVouchers = allVouchers.value.filter(v => {
    if (v.trangThai === false) return false
    if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false
    if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false
    if (v.soLuong && v.soLuongDaSuDung !== undefined && v.soLuongDaSuDung >= v.soLuong) return false
    const minRequired = Number(v.hoaDonToiThieu || 0)
    return subTotal.value >= minRequired
  })

  if (validVouchers.length === 0) return null

  const sorted = [...validVouchers].sort((a, b) => {
    const discA = calculateVoucherDiscount(a, subTotal.value)
    const discB = calculateVoucherDiscount(b, subTotal.value)
    if (discB !== discA) return discB - discA
    return (Number(a.hoaDonToiThieu || 0)) - (Number(b.hoaDonToiThieu || 0))
  })

  const best = sorted[0]
  const discountVal = calculateVoucherDiscount(best, subTotal.value)
  if (discountVal <= 0) return null

  return {
    voucher: best,
    discount: discountVal
  }
})

const sortedFilteredVouchers = computed(() => {
  const list = [...filteredVouchers.value]
  return list.sort((a, b) => {
    const elA = isVoucherEligible(a)
    const elB = isVoucherEligible(b)
    if (elA && !elB) return -1
    if (!elA && elB) return 1

    if (elA && elB) {
      const discA = calculateVoucherDiscount(a, subTotal.value)
      const discB = calculateVoucherDiscount(b, subTotal.value)
      if (discB !== discA) return discB - discA
    }

    return (Number(a.hoaDonToiThieu || 0)) - (Number(b.hoaDonToiThieu || 0))
  })
})

const applyBestVoucher = () => {
  if (!bestVoucherSuggestion.value) {
    notifyWarning('Chưa có mã phù hợp', 'Không có phiếu giảm giá nào đủ điều kiện cho đơn hàng này.')
    return
  }
  applyVoucher(bestVoucherSuggestion.value.voucher)
}

const getVoucherIneligibilityReason = (v) => {
  if (v.trangThai === false) {
    return 'Phiếu giảm giá tạm thời bị khóa'
  }
  const now = new Date()
  if (v.ngayBatDau && new Date(v.ngayBatDau) > now) {
    return `Chưa đến ngày áp dụng (bắt đầu từ ${formatDateVn(v.ngayBatDau)})`
  }
  if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) {
    return `Đã hết hạn vào ngày ${formatDateVn(v.ngayKetThuc)}`
  }
  if (v.soLuong && v.soLuongDaSuDung !== undefined && v.soLuongDaSuDung >= v.soLuong) {
    return 'Phiếu giảm giá đã hết lượt sử dụng'
  }
  const minRequired = Number(v.hoaDonToiThieu || 0)
  if (subTotal.value < minRequired) {
    const remaining = minRequired - subTotal.value
    return `Cần mua thêm ${formatVnd(remaining)} (tối thiểu ${formatVnd(minRequired)})`
  }
  return ''
}

const openVoucherModal = () => {
  if (!currentOrder.value) createNewOrder()
  voucherSearchQuery.value = ''
  isVoucherModalOpen.value = true
}

const openQuickCustomerFromVoucher = () => {
  isVoucherModalOpen.value = false
  isAddCustomerModalOpen.value = true
}

const applyVoucher = (voucher) => {
  if (!currentOrder.value) return
  if (!isVoucherEligible(voucher)) {
    notifyWarning('Chưa đủ điều kiện', getVoucherIneligibilityReason(voucher))
    return
  }
  currentOrder.value.selectedVoucher = voucher
  voucherInputCode.value = ''
  isVoucherModalOpen.value = false
  notifySuccess('Đã áp dụng voucher', `Đã áp dụng mã "${voucher.ma || voucher.maPhieuGiamGia}" (${voucher.ten})!`)
}

const removeVoucher = () => {
  if (currentOrder.value) {
    currentOrder.value.selectedVoucher = null
    voucherInputCode.value = ''
    notifySuccess('Đã hủy voucher', 'Đã hủy áp dụng phiếu giảm giá cho đơn hàng!')
  }
}

const handleApplyManualVoucher = () => {
  const code = voucherInputCode.value.trim().toUpperCase()
  if (!code) return
  if (!currentOrder.value) createNewOrder()

  const found = allVouchers.value.find(v =>
    (v.ma && v.ma.toUpperCase() === code) ||
    (v.maPhieuGiamGia && v.maPhieuGiamGia.toUpperCase() === code)
  )

  if (!found) {
    notifyError('Mã không hợp lệ', `Không tìm thấy mã giảm giá "${code}". Vui lòng kiểm tra lại!`)
    return
  }

  applyVoucher(found)
}

const handleModalSearchApply = () => {
  const code = voucherSearchQuery.value.trim().toUpperCase()
  if (!code) return
  const found = filteredVouchers.value.find(v =>
    (v.ma && v.ma.toUpperCase() === code) ||
    (v.maPhieuGiamGia && v.maPhieuGiamGia.toUpperCase() === code)
  ) || filteredVouchers.value[0]

  if (found) {
    applyVoucher(found)
  } else {
    notifyError('Không tìm thấy', `Không tìm thấy phiếu giảm giá nào khớp với "${code}"`)
  }
}

watch(subTotal, (newSub) => {
  if (currentOrder.value?.selectedVoucher) {
    const v = currentOrder.value.selectedVoucher
    const minReq = Number(v.hoaDonToiThieu || 0)
    if (newSub < minReq) {
      notifyWarning(
        'Voucher tự động gỡ',
        `Tổng tiền hàng (${formatVnd(newSub)}) không còn đủ mức tối thiểu (${formatVnd(minReq)}) của mã ${v.ma || v.maPhieuGiamGia}.`
      )
      currentOrder.value.selectedVoucher = null
    }
  }
})

// ===== Giao ca (Mo ca / Ket ca / Chan checkout khi chua mo ca - R13) =====
const caHienTai = ref(null)
const isLoadingCa = ref(true)
const isMoCaModalOpen = ref(false)
const isSavingMoCa = ref(false)
const moCaForm = reactive({ tienDauCa: 500000, ghiChu: '' })
const moCaError = ref('')

const isKetCaModalOpen = ref(false)
const isLoadingTongKet = ref(false)
const isSavingKetCa = ref(false)
const ketCaTongKet = ref(null)
const ketCaForm = reactive({ tienThucTe: null, lyDoLech: '', ghiChu: '' })
const ketCaError = ref('')
const ketCaLichError = ref([])

const chenhLechKetCa = computed(() =>
  Number(ketCaForm.tienThucTe || 0) - Number(ketCaTongKet.value?.tienDuKien || 0)
)

const loadCaHienTai = async () => {
  isLoadingCa.value = true
  try {
    caHienTai.value = await getDangMo()
  } catch (err) {
    // Giữ state cũ thay vì set null (network blip không được đổi banner sang "Chưa mở ca" oan)
    console.error('Lỗi tải ca làm việc:', err)
    notifyWarning('Chưa tải được trạng thái ca', 'Giữ nguyên trạng thái hiện tại — vui lòng thử lại sau')
  } finally {
    isLoadingCa.value = false
  }
}

const openMoCaModal = () => {
  moCaError.value = ''
  moCaForm.tienDauCa = 500000
  moCaForm.ghiChu = ''
  isMoCaModalOpen.value = true
}

const confirmMoCa = async () => {
  if (isSavingMoCa.value) return
  const raw = moCaForm.tienDauCa
  const tien = Number(raw)
  if (raw === '' || raw === null || raw === undefined || Number.isNaN(tien)) {
    moCaError.value = 'Vui lòng nhập tiền đầu ca'
    return
  }
  if (tien < 0) {
    moCaError.value = 'Tiền đầu ca không được âm'
    return
  }
  if (tien % 1000 !== 0) {
    moCaError.value = 'Tiền đầu ca phải là bội số của 1.000đ'
    return
  }
  moCaError.value = ''
  isSavingMoCa.value = true
  try {
    const ca = await moCa({ tienDauCa: tien, ghiChu: moCaForm.ghiChu || null })
    caHienTai.value = ca
    isMoCaModalOpen.value = false
    notifySuccess('Mở ca thành công', `Ca ${ca.maCa} đã được mở`)
  } catch (err) {
    const msg = err.message || ''
    if (msg.includes('CA_DA_MO')) {
      moCaError.value = 'Nhân viên đã có ca đang mở'
      await loadCaHienTai()
      if (caHienTai.value) isMoCaModalOpen.value = false
    } else {
      moCaError.value = msg || 'Không thể mở ca'
    }
  } finally {
    isSavingMoCa.value = false
  }
}

let tongKetReqId = 0

const openKetCaModal = async () => {
  if (!caHienTai.value) return
  const reqId = ++tongKetReqId
  ketCaError.value = ''
  ketCaLichError.value = []
  ketCaForm.lyDoLech = ''
  ketCaForm.ghiChu = ''
  ketCaForm.tienThucTe = Number(caHienTai.value.tienDuKien || 0)
  ketCaTongKet.value = null
  isKetCaModalOpen.value = true
  isLoadingTongKet.value = true
  try {
    const tongKet = await getTongKet(caHienTai.value.id)
    if (reqId !== tongKetReqId) return
    ketCaTongKet.value = tongKet
    ketCaForm.tienThucTe = Number(tongKet.tienDuKien || 0)
  } catch (err) {
    if (reqId !== tongKetReqId || !isKetCaModalOpen.value) return
    console.error('Lỗi tải đối soát ca:', err)
    notifyError('Không tải được đối soát', err.message || 'Vui lòng thử lại')
    isKetCaModalOpen.value = false
  } finally {
    if (reqId === tongKetReqId) isLoadingTongKet.value = false
  }
}

const confirmKetCa = async () => {
  if (isSavingKetCa.value) return
  if (!caHienTai.value) return
  const raw = ketCaForm.tienThucTe
  const tien = Number(raw)
  if (raw === '' || raw === null || raw === undefined || Number.isNaN(tien)) {
    ketCaError.value = 'Vui lòng nhập tiền thực tế'
    return
  }
  if (tien < 0) {
    ketCaError.value = 'Tiền thực tế không được âm'
    return
  }
  const lech = tien - Number(ketCaTongKet.value?.tienDuKien || 0)
  if (lech !== 0 && !String(ketCaForm.lyDoLech || '').trim()) {
    ketCaError.value = 'Chênh lệch khác 0, bắt buộc nhập lý do lệch'
    return
  }
  ketCaError.value = ''
  ketCaLichError.value = []
  isSavingKetCa.value = true
  try {
    await ketCa(caHienTai.value.id, {
      tienThucTe: tien,
      lyDoLech: lech === 0 ? '' : String(ketCaForm.lyDoLech).trim(),
      ghiChu: ketCaForm.ghiChu || null
    })
    caHienTai.value = null
    isKetCaModalOpen.value = false
    notifySuccess('Kết ca thành công', 'Ca đã được kết thúc và lưu vào lịch sử')
  } catch (err) {
    const msg = err.message || ''
    if (msg.includes('LY_DO_LECH_THIEU')) {
      ketCaError.value = 'Chênh lệch khác 0, bắt buộc nhập lý do lệch'
    } else if (msg.includes('CON_HD_DANG_XU_LY')) {
      const noiDung = msg.replace(/^CON_HD_DANG_XU_LY:\s*/, '')
      const phanMa = noiDung.includes(':') ? noiDung.slice(noiDung.lastIndexOf(':') + 1) : noiDung
      const dsMa = phanMa.split(/\s+/).map((s) => s.trim()).filter(Boolean)
      ketCaLichError.value = dsMa.length > 0 ? dsMa : [noiDung]
    } else if (err.status === 409 || msg.includes('CA_DANG_KET')) {
      isKetCaModalOpen.value = false
      notifyWarning('Ca vừa được kết bởi thao tác khác', 'Trạng thái ca đã được làm mới')
      await loadCaHienTai()
    } else {
      ketCaError.value = msg || 'Không thể kết ca'
    }
  } finally {
    isSavingKetCa.value = false
  }
}

const handleCheckout = async () => {
  if (!currentOrder.value) return
  if (currentOrder.value.cart.length === 0) {
    notifyWarning('Giỏ hàng trống', 'Vui lòng chọn ít nhất 1 sản phẩm vào giỏ hàng trước khi thanh toán!')
    return
  }

  // Đang tải trạng thái ca: chưa biết có ca hay không — không chặn oan, để BE quyết định (CA_CHUA_MO nếu thiếu)
  if (!isLoadingCa.value && !caHienTai.value && !currentOrder.value.isDelivery) {
    notifyWarning('Chưa mở ca', 'Chưa mở ca, vui lòng mở ca để bán tại quầy!')
    openMoCaModal()
    return
  }

  if (currentOrder.value.selectedVoucher && !currentOrder.value.customer) {
    notifyWarning('Cần khách hàng', 'Vui lòng chọn hoặc thêm khách hàng để áp dụng phiếu giảm giá!')
    return
  }

  if (currentOrder.value.paymentMethod === 'TIEN_MAT') {
    if ((currentOrder.value.cashGiven || 0) < finalTotal.value) {
      notifyWarning('Chưa đủ tiền', `Số tiền khách đưa chưa đủ! Cần thêm ${formatVnd(finalTotal.value - (currentOrder.value.cashGiven || 0))}`)
      return
    }
  }

  if (currentOrder.value.paymentMethod === 'KET_HOP') {
    const totalPaid = Number(currentOrder.value.cashGiven || 0) + Number(currentOrder.value.transferGiven || 0)
    if (totalPaid < finalTotal.value) {
      notifyWarning('Chưa đủ tiền', `Tổng tiền thanh toán kết hợp chưa đủ! Cần thêm ${formatVnd(finalTotal.value - totalPaid)}`)
      return
    }
  }

  const ok = await notifyConfirm(
    'Xác nhận thanh toán',
    `Thanh toán hóa đơn "${currentOrder.value.code}" với tổng số tiền ${formatVnd(finalTotal.value)}?`,
    'Xác nhận thanh toán',
    'Kiểm tra lại'
  )
  if (!ok) return

  if (currentOrder.value.isDelivery) {
    const d = currentOrder.value.deliveryInfo
    if (d.feeLoading) {
      notifyWarning('Phí vận chuyển', 'Đang tính phí vận chuyển, vui lòng đợi trong giây lát...')
      return
    }
    if (!d.legacy || !d.shippingFee || d.shippingFee <= 0) {
      notifyWarning('Phí vận chuyển', 'Chưa tính được phí vận chuyển. Vui lòng chọn địa chỉ giao hàng hợp lệ và thử lại.')
      return
    }
  }

  isProcessingCheckout.value = true

  try {
    const paymentMethodId = getPaymentMethodId(currentOrder.value.paymentMethod)
    const employeeId = allEmployees.value.length > 0 ? allEmployees.value[0].id : 2

    const customerPhone = currentOrder.value.customer && currentOrder.value.customer.soDienThoai
      ? currentOrder.value.customer.soDienThoai
      : '0988888888'

    const customerName = currentOrder.value.customer
      ? (currentOrder.value.customer.ten || currentOrder.value.customer.tenKhachHang)
      : 'Khách lẻ'

    const delivery = currentOrder.value.isDelivery ? currentOrder.value.deliveryInfo : null
    const payload = {
      maHoaDon: currentOrder.value.code,
      loaiDon: currentOrder.value.isDelivery ? 'Online' : 'Tại quầy',
      phiVanChuyen: currentOrder.value.isDelivery ? Number(delivery.shippingFee || 0) : 0,
      tenKhachHang: customerName,
      soDienThoaiKhachHang: customerPhone,
      diaChiNhanHang: delivery
        ? [delivery.recipientAddress, delivery.ward?.name, delivery.province?.name].filter(Boolean).join(', ')
        : '',
      toProvinceId: delivery?.legacy?.toProvinceId ?? null,
      toDistrictId: delivery?.legacy?.toDistrictId ?? null,
      toWardCode: delivery?.legacy?.toWardCode ?? null,
      trangThai: currentOrder.value.isDelivery ? 'Chờ xác nhận' : 'Đã hoàn thành',
      ghiChu: currentOrder.value.note || 'Bán hàng tại quầy POS',
      idKhachHang: currentOrder.value.customer ? currentOrder.value.customer.id : null,
      idNhanVien: employeeId,
      idPhuongThucThanhToan: paymentMethodId,
      idPhieuGiamGia: currentOrder.value.selectedVoucher ? currentOrder.value.selectedVoucher.id : null,
      chiTiet: currentOrder.value.cart.map(item => ({
        idSanPhamChiTiet: item.idCtsp,
        soLuong: item.quantity,
        donGia: item.price,
        ghiChu: '',
        trangThai: true
      }))
    }

    await http.post('/hoa-don', payload)

    completedInvoiceData.value = {
      code: currentOrder.value.code,
      title: currentOrder.value.title,
      customerName: payload.tenKhachHang,
      customerPhone: payload.soDienThoaiKhachHang,
      cart: [...currentOrder.value.cart],
      subTotal: subTotal.value,
      discount: discountAmount.value,
      shippingFee: payload.phiVanChuyen,
      finalTotal: finalTotal.value,
      paymentMethod: currentOrder.value.paymentMethod,
      cashGiven: currentOrder.value.cashGiven,
      changeDue: Math.max(0, changeDue.value),
      date: new Date().toLocaleString('vi-VN'),
      staffName: staffName.value || 'Nhân viên quầy'
    }

    notifySuccess('Thanh toán thành công!', `Đã hoàn tất hóa đơn ${currentOrder.value.code}`)
    fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500&trangThai=true`)
      .then(r => r.json())
      .then(data => {
        if (data && data.content) allProducts.value = data.content
      })
      .catch(() => { })

    pendingOrders.value.splice(activeOrderIndex.value, 1)
    if (pendingOrders.value.length === 0) {
      createNewOrder()
    } else if (activeOrderIndex.value >= pendingOrders.value.length) {
      activeOrderIndex.value = Math.max(0, pendingOrders.value.length - 1)
    }

    isReceiptModalOpen.value = true

  } catch (err) {
    console.error('Checkout error:', err)
    if ((err.message || '').includes('CA_CHUA_MO')) {
      notifyWarning('Chưa mở ca', 'Chưa mở ca, vui lòng mở ca để bán tại quầy!')
      openMoCaModal()
      loadCaHienTai()
    } else {
      notifyError('Lỗi thanh toán', err.message || 'Không thể lưu hóa đơn vào hệ thống!')
    }
  } finally {
    isProcessingCheckout.value = false
  }
}

const formatPaymentMethodName = (method) => {
  if (method === 'TIEN_MAT') return 'Tiền mặt'
  if (method === 'CHUYEN_KHOAN') return 'Chuyển khoản (VietQR)'
  if (method === 'KET_HOP') return 'Tiền mặt + Chuyển khoản'
  return method || 'Tiền mặt'
}

const printReceipt = () => {
  const printEl = document.getElementById('printable-receipt-content')
  if (!printEl) {
    notifyWarning('Không tìm thấy nội dung', 'Không thể lấy nội dung hóa đơn để in.')
    return
  }

  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = '0'
  iframe.style.opacity = '0'
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow.document
  doc.open()
  doc.write(`<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Hoa_don_${completedInvoiceData.value?.code || 'POS'}</title>
    <style>
      @page {
        size: 80mm auto;
        margin: 4mm;
      }
      * {
        box-sizing: border-box;
      }
      body {
        margin: 0;
        padding: 0;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        color: #0f172a;
        background: #ffffff;
        font-size: 12px;
        line-height: 1.35;
      }
      .thermal-receipt-box {
        width: 100%;
        padding: 4px;
        background: #ffffff;
      }
      .receipt-header-center {
        text-align: center;
        padding-bottom: 8px;
        border-bottom: 1px dashed #64748b;
        margin-bottom: 8px;
      }
      .receipt-brand-logo {
        font-size: 17px;
        font-weight: 900;
        letter-spacing: 0.5px;
        margin-bottom: 3px;
        color: #0f172a;
      }
      .receipt-sub-line {
        font-size: 10.5px;
        color: #475569;
        margin: 2px 0;
      }
      .receipt-bill-heading {
        text-align: center;
        margin-bottom: 10px;
        padding-bottom: 6px;
        border-bottom: 1px dashed #64748b;
      }
      .receipt-bill-title {
        font-size: 13.5px;
        font-weight: 800;
        color: #0f172a;
      }
      .receipt-barcode-code {
        font-family: monospace;
        font-size: 11px;
        letter-spacing: 2px;
        color: #475569;
        margin-top: 2px;
      }
      .receipt-meta-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        margin-bottom: 10px;
        padding: 6px 8px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 4px;
      }
      .meta-row {
        display: flex;
        justify-content: space-between;
        font-size: 11px;
      }
      .meta-k {
        color: #475569;
      }
      .meta-v {
        font-weight: 600;
        color: #0f172a;
      }
      .receipt-items-table {
        width: 100%;
        border-collapse: collapse;
        margin-bottom: 10px;
      }
      .receipt-items-table thead tr {
        border-top: 1px dashed #64748b;
        border-bottom: 1px dashed #64748b;
      }
      .receipt-items-table th {
        padding: 6px 0;
        font-size: 10.5px;
        font-weight: 700;
        text-align: left;
        text-transform: uppercase;
        color: #475569;
      }
      .receipt-items-table tbody tr {
        border-bottom: 1px solid #f1f5f9;
      }
      .receipt-items-table td {
        padding: 5px 0;
        font-size: 11.5px;
        vertical-align: top;
      }
      .rc-item-name {
        font-weight: 600;
        color: #0f172a;
      }
      .rc-item-variant {
        font-size: 10px;
        color: #64748b;
        margin-top: 1px;
      }
      .rc-item-qty {
        text-align: center;
        font-weight: 600;
      }
      .rc-item-price {
        text-align: right;
        color: #475569;
      }
      .rc-item-total {
        text-align: right;
        font-weight: 700;
        color: #0f172a;
      }
      .receipt-totals-block {
        border-top: 1px dashed #64748b;
        padding-top: 8px;
        display: flex;
        flex-direction: column;
        gap: 4px;
      }
      .receipt-sum-row {
        display: flex;
        justify-content: space-between;
        font-size: 11px;
      }
      .sum-k {
        color: #475569;
      }
      .sum-v {
        font-weight: 600;
        color: #0f172a;
      }
      .discount-val {
        color: #dc2626;
        font-weight: 700;
      }
      .grand-sum {
        border-top: 1.5px solid #0f172a;
        padding-top: 6px;
        margin-top: 4px;
      }
      .grand-sum .sum-k {
        font-size: 12.5px;
        font-weight: 800;
        color: #0f172a;
      }
      .grand-sum-val {
        font-size: 15px;
        font-weight: 900;
        color: #dc2626;
      }
      .receipt-footer-notes {
        text-align: center;
        margin-top: 14px;
        padding-top: 8px;
        border-top: 1px dashed #64748b;
      }
      .rc-thank-txt {
        font-size: 11.5px;
        font-weight: 700;
        margin: 0 0 2px;
        color: #0f172a;
      }
      .rc-policy-txt {
        font-size: 10px;
        color: #64748b;
        margin: 0;
      }
    </style>
  </head>
  <body>
    ${printEl.outerHTML}
  </body>
</html>`)
  doc.close()

  iframe.contentWindow.focus()
  setTimeout(() => {
    iframe.contentWindow.print()
    setTimeout(() => {
      if (document.body.contains(iframe)) {
        document.body.removeChild(iframe)
      }
    }, 2000)
  }, 250)
}

const handleKeyDown = (e) => {
  if (e.key === 'Escape') {
    isProductModalOpen.value = false
    isSwapModalOpen.value = false
    isQrModalOpen.value = false
    isAddCustomerModalOpen.value = false
    isBankQrModalOpen.value = false
    isVoucherModalOpen.value = false
    isReceiptModalOpen.value = false
    isKetCaModalOpen.value = false
    stopCameraScanner()
  }
}

const loadInitialData = async () => {
  isLoadingData.value = true
  try {
    const [formRes, prodRes, brandRes, catRes, colorRes, sizeRes] = await Promise.allSettled([
      http.get('/hoa-don/form-data'),
      fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500&trangThai=true`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/thuong_hieu`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/loai_giay`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/mau_sac`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/kich_co`).then(r => r.json())
    ])

    if (formRes.status === 'fulfilled' && formRes.value.data) {
      const d = formRes.value.data
      allCustomers.value = d.khachHangs || []
      allVouchers.value = (d.phieuGiamGias || []).filter(v => v.trangThai !== false)
      allEmployees.value = d.nhanViens || []
      allPaymentMethods.value = d.phuongThucThanhToans || d.phuongThucs || []
    }

    if (allPaymentMethods.value.length === 0) {
      allPaymentMethods.value = [
        { id: 1, ten: 'Tiền mặt tại quầy' },
        { id: 2, ten: 'Chuyển khoản ngân hàng' }
      ]
    }

    if (brandRes.status === 'fulfilled' && Array.isArray(brandRes.value)) brandsList.value = brandRes.value
    if (catRes.status === 'fulfilled' && Array.isArray(catRes.value)) categoriesList.value = catRes.value
    if (colorRes.status === 'fulfilled' && Array.isArray(colorRes.value)) colorsList.value = colorRes.value
    if (sizeRes.status === 'fulfilled' && Array.isArray(sizeRes.value)) sizesList.value = sizeRes.value

    if (prodRes.status === 'fulfilled' && prodRes.value && prodRes.value.content) {
      allProducts.value = prodRes.value.content
    }

    if (allProducts.value.length === 0) {
      allProducts.value = [
        {
          id: 101,
          maSp: 'G66748',
          maCtsp: 'G66748-MS05-37',
          tenSp: 'ASICS GEL-Kayano 31',
          color: 'Xám',
          size: '37',
          stock: 99,
          price: 3790000,
          discount: '-20%',
          img: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60'
        },
        {
          id: 102,
          maSp: 'G66748',
          maCtsp: 'G66748-MS05-36',
          tenSp: 'ASICS GEL-Kayano 31',
          color: 'Xám',
          size: '36',
          stock: 100,
          price: 3790000,
          discount: '-20%',
          img: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60'
        },
        {
          id: 103,
          maSp: 'G66748',
          maCtsp: 'G66748-MS01-37',
          tenSp: 'ASICS GEL-Kayano 31',
          color: 'Đen',
          size: '37',
          stock: 100,
          price: 3790000,
          discount: '-20%',
          img: 'https://images.unsplash.com/photo-1549298916-b41d501d3772?w=120&auto=format&fit=crop&q=60'
        },
        {
          id: 104,
          maSp: 'G66748',
          maCtsp: 'G66748-MS01-36',
          tenSp: 'ASICS GEL-Kayano 31',
          color: 'Đen',
          size: '36',
          stock: 100,
          price: 3790000,
          discount: '-20%',
          img: 'https://images.unsplash.com/photo-1549298916-b41d501d3772?w=120&auto=format&fit=crop&q=60'
        },
        {
          id: 105,
          maSp: 'G57664',
          maCtsp: 'G57664-MS03-37',
          tenSp: 'New Balance 530',
          color: 'Đỏ',
          size: '37',
          stock: 100,
          price: 2500000,
          discount: null,
          img: 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=120&auto=format&fit=crop&q=60'
        }
      ]
    }

    if (pendingOrders.value.length === 0) {
      createNewOrder()
    }
  } catch (error) {
    console.error('Error loading POS data:', error)
  } finally {
    isLoadingData.value = false
  }
}

const handleWindowClick = (e) => {
  if (!e.target.closest('.customer-search-area')) {
    showCustomerDropdown.value = false
  }
}

onMounted(() => {
  loadInitialData()
  loadProvinces()
  loadCaHienTai()
  window.addEventListener('click', handleWindowClick)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('click', handleWindowClick)
  stopCameraScanner()
})
</script>

<template>
  <div class="pos-page">
    <!-- Giao ca banner (Mo ca / Ket ca) -->
    <div
      class="pos-ca-banner"
      :class="isLoadingCa ? 'loading' : (caHienTai ? 'ca-open' : 'ca-closed')"
    >
      <div class="ca-banner-info">
        <span class="ca-banner-dot"></span>
        <span v-if="isLoadingCa" class="ca-banner-text">Đang tải trạng thái ca...</span>
        <span v-else-if="caHienTai" class="ca-banner-text">
          Ca {{ caHienTai.maCa }} · Mở lúc {{ formatDateTime(caHienTai.thoiGianMo) }}<template v-if="caHienTai.tenQuy"> · {{ caHienTai.tenQuy }}</template>
        </span>
        <span v-else class="ca-banner-text">Chưa mở ca — Bán tại quầy bị tạm khóa</span>
      </div>
      <button v-if="!isLoadingCa && caHienTai" class="btn-ca-action btn-ca-ket" @click="openKetCaModal">
        Kết ca
      </button>
      <button v-else-if="!isLoadingCa" class="btn-ca-action btn-ca-mo" @click="openMoCaModal">
        Mở ca
      </button>
    </div>

    <!-- Main Workspace Grid -->
    <div class="pos-main-grid">
      <div class="pos-col-left">
        <div class="pos-workspace-panel">
          <div class="pos-orders-tabs-bar">
            <div class="pos-tabs-scroll-area">
              <button class="btn-create-order-tab" @click="createNewOrder"
                :disabled="pendingOrders.length >= MAX_ORDERS" title="Tạo hóa đơn chờ mới (Tối đa 5 hóa đơn)">
                <span>+ Thêm hóa đơn</span>
              </button>

              <div v-for="(order, idx) in pendingOrders" :key="order.id" class="pos-order-tab"
                :class="{ active: activeOrderIndex === idx }" @click="selectOrderTab(idx)">
                <span class="tab-order-indicator"></span>
                <span class="tab-order-name">{{ order.title }}</span>
                <span class="tab-order-code" v-if="order.code">({{ order.code }})</span>
                <span class="tab-cart-qty" v-if="order.cart.length > 0">{{ order.cart.length }}</span>
                <button class="tab-close-icon" @click="closeOrderTab(idx, $event)" title="Đóng hóa đơn này">
                  ×
                </button>
              </div>
            </div>

            <div class="pos-orders-counter-pill">
              <span class="counter-num">{{ pendingOrders.length }}/{{ MAX_ORDERS }}</span>
              <span class="counter-lbl">hóa đơn</span>
            </div>
          </div>

          <div class="pos-cart-main-container">
            <div class="cart-unified-toolbar">
              <div class="fast-search-container">
                <input type="text" class="fast-search-input"
                  placeholder="Quét mã barcode hoặc nhập mã CTSP / tên sản phẩm..." v-model="barcodeSearchText"
                  @keydown.enter="handleBarcodeSearchSubmit" />
                <button v-if="barcodeSearchText" class="btn-clear-fast-search" @click="barcodeSearchText = ''"
                  title="Xóa tìm kiếm">
                  ×
                </button>
                <button class="btn-fast-search-add" @click="handleBarcodeSearchSubmit" title="Thêm vào giỏ">
                  <span>Thêm</span>
                </button>
              </div>

              <div class="cart-right-tools">
                <button class="btn-tool-action btn-tool-camera" @click="openQrScanner"
                  title="Quét mã sản phẩm qua Camera">
                  <span>Quét Camera</span>
                </button>

                <button class="btn-tool-action btn-tool-product" @click="openProductPicker"
                  title="Mở danh mục chọn sản phẩm">
                  <span>Chọn sản phẩm</span>
                </button>

                <button v-if="currentOrder && currentOrder.cart.length > 0" class="btn-tool-action btn-tool-clear"
                  @click="clearCurrentCart" title="Làm trống toàn bộ giỏ hàng">
                  <span>Làm trống</span>
                </button>
              </div>
            </div>

            <template v-if="currentOrder && currentOrder.cart.length > 0">
              <div class="cart-table-wrapper">
                <table class="cart-table">
                  <thead>
                    <tr>
                      <th width="44" class="text-center">STT</th>
                      <th>SẢN PHẨM</th>
                      <th width="140" class="text-center">PHÂN LOẠI</th>
                      <th width="130" class="text-center">SỐ LƯỢNG</th>
                      <th width="130" class="text-end">ĐƠN GIÁ</th>
                      <th width="140" class="text-end">THÀNH TIỀN</th>
                      <th width="70" class="text-center">THAO TÁC</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, idx) in currentOrder.cart" :key="item.idCtsp">
                      <td class="text-center cell-stt">{{ idx + 1 }}</td>
                      <td>
                        <div class="product-item-wrap">
                          <img v-if="item.img" :src="item.img" alt="Ảnh SP" class="prod-thumb-mini" />
                          <div class="prod-details">
                            <span class="prod-name-title" :title="item.name">{{ item.name }}</span>
                            <span class="prod-code-sub">Mã: {{ item.maCtsp || item.maSp }}</span>
                          </div>
                        </div>
                      </td>
                      <td class="text-center">
                        <div class="variant-simple-text">
                          <span class="variant-size-txt">Size {{ item.size }}</span>
                          <span class="variant-sep">•</span>
                          <span class="variant-color-txt">{{ item.color }}</span>
                        </div>
                        <button type="button" class="btn-quick-swap-variant" @click="openSwapModal(item)"
                          title="Đổi sang Size hoặc Màu khác của sản phẩm này">
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            stroke-width="2.2">
                            <path d="M7 16V4m0 0L3 8m4-4l4 4m6 4v12m0 0l4-4m-4 4l-4-4" />
                          </svg>
                          <span>Đổi size/màu</span>
                        </button>
                      </td>
                      <td class="text-center">
                        <div class="qty-stepper-modern">
                          <button class="step-btn" @click="updateCartItemQty(item, item.quantity - 1)"
                            :disabled="item.quantity <= 1">−</button>
                          <input type="number" class="step-input" v-model.number="item.quantity"
                            @change="updateCartItemQty(item, item.quantity)" min="1" />
                          <button class="step-btn" @click="updateCartItemQty(item, item.quantity + 1)"
                            :disabled="getRemainingStock(item.idCtsp) <= 0">+</button>
                        </div>
                        <div class="stock-hint-txt" v-if="getRemainingStock(item.idCtsp) < 10">
                          Còn lại: {{ getRemainingStock(item.idCtsp) }}
                        </div>
                      </td>
                      <td class="text-end">
                        <div class="price-stack">
                          <span class="price-active">{{ formatVnd(item.price) }}</span>
                          <span v-if="item.originalPrice && item.originalPrice > item.price"
                            class="price-strikethrough">
                            {{ formatVnd(item.originalPrice) }}
                          </span>
                        </div>
                      </td>
                      <td class="text-end cell-total-price">
                        {{ formatVnd(item.price * item.quantity) }}
                      </td>
                      <td class="text-center">
                        <button class="btn-del-item" @click="removeCartItem(item)" title="Xóa khỏi giỏ hàng">
                          <span>Xóa</span>
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="pos-cart-bottom-bar">
                <div class="cart-items-indicator">
                  <span>Giỏ hàng: <strong>{{ totalCartQty }}</strong> sản phẩm</span>
                </div>

                <div class="cart-subtotal-indicator">
                  <span class="subtotal-lbl">Tổng tiền hàng:</span>
                  <span class="subtotal-val">{{ formatVnd(subTotal) }}</span>
                </div>
              </div>
            </template>

            <div class="empty-cart-state" v-else>
              <div class="empty-cart-content">
                <h5 class="empty-cart-title">Giỏ hàng đang trống</h5>
                <p class="empty-cart-sub">Quét mã barcode/QR hoặc bấm nút bên dưới để chọn sản phẩm vào đơn</p>
                <button class="btn-empty-add-prod" @click="openProductPicker">
                  <span>Chọn sản phẩm ngay</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="pos-col-right">
        <div class="pos-card customer-card">
          <div class="panel-card-head">
            <div class="head-title-wrap">
              <h4 class="panel-title">Khách hàng</h4>
            </div>
            <button class="btn-quick-add-cust" @click="isAddCustomerModalOpen = true">
              <span>Thêm nhanh</span>
            </button>
          </div>

          <div class="customer-search-area">
            <div class="customer-input-wrap">
              <input type="text" class="input-cust-search" placeholder="Tìm theo tên hoặc SĐT khách hàng..."
                v-model="customerSearchQuery" @focus="showCustomerDropdown = true" />
              <button v-if="customerSearchQuery" class="btn-clear-cust" @click="customerSearchQuery = ''">
                ×
              </button>
            </div>

            <ul v-if="showCustomerDropdown && filteredCustomers.length > 0" class="customer-dropdown-menu">
              <li v-for="c in filteredCustomers" :key="c.id" class="cust-drop-item" @click="selectCustomer(c)">
                <div class="cust-drop-avatar">
                  {{ (c.ten || c.tenKhachHang || 'K')[0].toUpperCase() }}
                </div>
                <div>
                  <div class="cust-drop-name">{{ c.ten || c.tenKhachHang }}</div>
                  <div class="cust-drop-sub">{{ c.soDienThoai }}</div>
                </div>
              </li>
            </ul>
          </div>

          <div v-if="currentOrder && currentOrder.customer" class="selected-customer-box">
            <div class="cust-box-left">
              <div class="cust-badge-avatar">
                {{ (currentOrder.customer.ten || currentOrder.customer.tenKhachHang || 'K')[0].toUpperCase() }}
              </div>
              <div class="cust-box-details">
                <span class="cust-box-name">{{ currentOrder.customer.ten || currentOrder.customer.tenKhachHang }}</span>
                <span class="cust-box-phone">{{ currentOrder.customer.soDienThoai }}</span>
              </div>
            </div>
            <button class="btn-unselect-cust" @click="removeSelectedCustomer" title="Bỏ chọn (trở lại khách lẻ)">
              Bỏ chọn
            </button>
          </div>
          <div v-else class="retail-guest-badge">
            <div class="guest-dot"></div>
            <span class="guest-label">Khách mua lẻ tại quầy (mặc định)</span>
          </div>
        </div>

        <div class="pos-card order-checkout-card">
          <div class="panel-card-head">
            <div class="head-title-wrap">
              <h4 class="panel-title">Thông tin đơn hàng</h4>
            </div>
            <div class="delivery-switch-wrap">
              <span class="delivery-switch-lbl">Giao hàng</span>
              <label class="toggle-switch">
                <input type="checkbox" v-if="currentOrder" v-model="currentOrder.isDelivery" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div v-if="currentOrder && currentOrder.isDelivery" class="delivery-drawer-box">
            <div class="drawer-title">Thông tin nhận hàng</div>
            <input type="text" class="del-input" placeholder="Tên người nhận hàng..." v-model="currentOrder.deliveryInfo.recipientName" />
            <input type="text" class="del-input" placeholder="Số điện thoại người nhận..." v-model="currentOrder.deliveryInfo.recipientPhone" />
            <SearchableSelect
              :model-value="currentOrder.deliveryInfo.province ? currentOrder.deliveryInfo.province.id : null"
              :options="provincesForSelect"
              value-key="value"
              label-key="label"
              placeholder="Chọn Tỉnh / Thành phố..."
              search-placeholder="Tìm tỉnh thành..."
              :allow-clear="false"
              @change="onProvinceSelectChange"
            />
            <SearchableSelect
              :model-value="currentOrder.deliveryInfo.ward ? currentOrder.deliveryInfo.ward.id : null"
              :options="wardsForSelect"
              value-key="value"
              label-key="label"
              :placeholder="currentOrder.deliveryInfo.wardsLoading ? 'Đang tải phường/xã...' : 'Chọn Phường / Xã...'"
              search-placeholder="Tìm phường xã..."
              :disabled="!currentOrder.deliveryInfo.province || currentOrder.deliveryInfo.wardsLoading"
              :allow-clear="false"
              @change="onWardSelectChange"
            />
            <input type="text" class="del-input" placeholder="Số nhà, tên đường chi tiết..." v-model="currentOrder.deliveryInfo.recipientAddress" />
            <SearchableSelect
              v-if="currentOrder.deliveryInfo.services && currentOrder.deliveryInfo.services.length"
              v-model="currentOrder.deliveryInfo.serviceTypeId"
              :options="serviceOptionsForOrder"
              value-key="value"
              label-key="label"
              placeholder="Chọn dịch vụ vận chuyển..."
              search-placeholder="Tìm dịch vụ..."
              :allow-clear="false"
              @change="onServiceChange"
            />
            <div v-if="currentOrder.deliveryInfo.feeLoading" class="fee-info">
              Đang tính phí vận chuyển với GHN...
            </div>
            <div v-else-if="currentOrder.deliveryInfo.feeError" class="fee-error">
              <span>{{ currentOrder.deliveryInfo.feeError }}</span>
              <button type="button" class="fee-retry" @click="tinhPhiVanChuyen">Thử lại</button>
            </div>
            <div v-else-if="currentOrder.deliveryInfo.shippingFee > 0" class="fee-info">
              Phí vận chuyển (GHN): <strong style="color: #d92d20;">{{ formatVnd(currentOrder.deliveryInfo.shippingFee)
              }}</strong>
            </div>
          </div>

          <div class="checkout-voucher-section" v-if="currentOrder">
            <div class="voucher-section-header">
              <span class="voucher-section-title">Phiếu giảm giá / Voucher</span>
              <button v-if="!currentOrder.selectedVoucher" type="button" class="btn-browse-vouchers"
                @click="openVoucherModal">
                Danh sách
                <span class="voucher-badge-num" v-if="allVouchers.length > 0">({{ allVouchers.length }})</span>
              </button>
            </div>

            <div v-if="currentOrder.selectedVoucher" class="selected-voucher-card">
              <div class="selected-voucher-content">
                <div class="selected-voucher-top">
                  <span class="selected-voucher-code">{{ currentOrder.selectedVoucher.ma ||
                    currentOrder.selectedVoucher.maPhieuGiamGia }}</span>
                  <span class="selected-voucher-val">- {{ formatVnd(discountAmount) }}</span>
                </div>
                <div class="selected-voucher-desc">
                  {{ currentOrder.selectedVoucher.ten }}
                  <span v-if="currentOrder.selectedVoucher.hoaDonToiThieu"> • Đơn từ {{
                    formatVnd(currentOrder.selectedVoucher.hoaDonToiThieu) }}</span>
                </div>
              </div>
              <button class="btn-remove-voucher-pill" @click="removeVoucher" title="Gỡ bỏ mã giảm giá">
                Gỡ
              </button>
            </div>

            <div v-if="!currentOrder.selectedVoucher && bestVoucherSuggestion" class="checkout-best-voucher-box">
              <div class="best-voucher-info-col">
                <div class="best-line-top">
                  <span class="best-tag-label">🔥 Gợi ý tốt nhất</span>
                  <span class="best-discount-val">-{{ formatVnd(bestVoucherSuggestion.discount) }}</span>
                </div>
                <div class="best-line-sub">
                  <span class="best-code-chip">{{ bestVoucherSuggestion.voucher.ma }}</span>
                  <span class="best-sub-desc" :title="bestVoucherSuggestion.voucher.ten">{{
                    bestVoucherSuggestion.voucher.ten
                  }}</span>
                </div>
              </div>
              <button type="button" class="btn-apply-best-quick" @click="applyBestVoucher">
                Áp dụng
              </button>
            </div>
            <div v-if="!currentOrder.selectedVoucher" class="voucher-input-group">
              <input type="text" v-model="voucherInputCode" class="voucher-quick-input"
                placeholder="Nhập mã voucher (VD: GG005)..." @keyup.enter="handleApplyManualVoucher" />
              <button type="button" class="btn-apply-manual-voucher" :disabled="!voucherInputCode.trim()"
                @click="handleApplyManualVoucher">
                Áp dụng
              </button>
            </div>
          </div>

          <div class="billing-summary-block">
            <div class="bill-row">
              <span class="bill-label">Tổng tiền hàng ({{ totalCartQty }} sản phẩm):</span>
              <span class="bill-val">{{ formatVnd(subTotal) }}</span>
            </div>
            <div v-if="discountAmount > 0" class="bill-row row-discount">
              <span class="bill-label">Giảm giá voucher:</span>
              <span class="bill-val">- {{ formatVnd(discountAmount) }}</span>
            </div>
            <div v-if="currentOrder && currentOrder.isDelivery && currentOrder.deliveryInfo.shippingFee > 0"
              class="bill-row">
              <span class="bill-label">Phí vận chuyển:</span>
              <span class="bill-val">+ {{ formatVnd(currentOrder.deliveryInfo.shippingFee) }}</span>
            </div>
          </div>

          <div class="grand-total-hero-card">
            <div class="hero-left">
              <span class="hero-lbl">KHÁCH PHẢI TRẢ</span>
              <span class="hero-sub">{{ totalCartQty }} sản phẩm</span>
            </div>
            <div class="hero-amount">
              {{ formatVnd(finalTotal) }}
            </div>
          </div>

          <div class="payment-methods-block" v-if="currentOrder">
            <span class="pay-method-lbl">Hình thức thanh toán</span>
            <div class="pay-method-grid">
              <label class="method-card" :class="{ active: currentOrder.paymentMethod === 'TIEN_MAT' }">
                <input type="radio" value="TIEN_MAT" v-model="currentOrder.paymentMethod" />
                <span class="method-title">Tiền mặt</span>
              </label>
              <label class="method-card" :class="{ active: currentOrder.paymentMethod === 'CHUYEN_KHOAN' }">
                <input type="radio" value="CHUYEN_KHOAN" v-model="currentOrder.paymentMethod" />
                <span class="method-title">Chuyển khoản</span>
              </label>
              <label class="method-card" :class="{ active: currentOrder.paymentMethod === 'KET_HOP' }">
                <input type="radio" value="KET_HOP" v-model="currentOrder.paymentMethod" />
                <span class="method-title">Kết hợp</span>
              </label>
            </div>
          </div>

          <div class="cash-settlement-box" v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'">
            <div class="cash-input-field">
              <span class="cash-label">Khách thanh toán tiền mặt</span>
              <div class="cash-input-wrap">
                <input type="number" class="del-input cash-amount-inp" placeholder="Nhập số tiền khách đưa..."
                  v-model.number="currentOrder.cashGiven" />
                <span class="cash-currency-badge">₫</span>
              </div>
            </div>
            <div class="quick-cash-row">
              <button class="quick-chip chip-exact" @click="setQuickCash('EXACT')">Đủ tiền</button>
              <button class="quick-chip" @click="setQuickCash(50000)">50k</button>
              <button class="quick-chip" @click="setQuickCash(100000)">100k</button>
              <button class="quick-chip" @click="setQuickCash(200000)">200k</button>
              <button class="quick-chip" @click="setQuickCash(500000)">500k</button>
            </div>
          </div>

          <div class="cash-settlement-box" v-if="currentOrder && currentOrder.paymentMethod === 'KET_HOP'">
            <div class="cash-input-field">
              <span class="cash-label">Số tiền chuyển khoản</span>
              <div class="cash-input-wrap">
                <input type="number" class="del-input cash-amount-inp" placeholder="Nhập số tiền chuyển khoản..."
                  v-model.number="currentOrder.transferGiven" />
                <span class="cash-currency-badge">₫</span>
              </div>
            </div>
          </div>

          <div class="change-due-box" :class="changeDue >= 0 ? 'positive' : 'negative'"
            v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'">
            <span class="change-label">{{ changeDue >= 0 ? 'Tiền thừa trả khách:' : 'Khách còn thiếu:' }}</span>
            <span class="change-value">{{ changeDue >= 0 ? formatVnd(changeDue) : formatVnd(Math.abs(changeDue))
            }}</span>
          </div>

          <div class="bank-qr-trigger-box"
            v-if="currentOrder && (currentOrder.paymentMethod === 'CHUYEN_KHOAN' || currentOrder.paymentMethod === 'KET_HOP')">
            <button class="btn-show-bank-qr" @click="generateBankTransferQr">
              <span>Tạo mã QR chuyển khoản</span>
            </button>
          </div>

          <div class="order-note-wrap" v-if="currentOrder">
            <span class="note-label">Ghi chú đơn hàng</span>
            <input type="text" class="input-order-note" placeholder="Ghi chú thêm nếu cần..."
              v-model="currentOrder.note" />
          </div>

          <div class="pos-final-actions">
            <button class="btn-cancel-bill" @click="handleCancelOrder"
              :disabled="!currentOrder || currentOrder.cart.length === 0">
              Hủy đơn
            </button>
            <button class="btn-checkout-primary"
              :class="{ ready: currentOrder && currentOrder.cart.length > 0 && !isProcessingCheckout }"
              @click="handleCheckout"
              :disabled="!currentOrder || currentOrder.cart.length === 0 || isProcessingCheckout">
              <span v-if="isProcessingCheckout">Đang xử lý...</span>
              <template v-else>
                <span>THANH TOÁN</span>
                <span class="btn-checkout-amount">{{ formatVnd(finalTotal) }}</span>
              </template>
            </button>
          </div>
        </div>
      </div>

    </div>

    <transition name="fade">
      <div v-if="isProductModalOpen" class="pos-modal-overlay" @click.self="closeProductPicker">
        <div class="pos-modal-dialog modal-xl">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Chọn sản phẩm bán hàng</h3>
                <span class="modal-badge-count">{{ modalTotalElements }} biến thể khả dụng</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isProductModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="modal-prod-filter-bar">
              <div class="modal-filter-top-row">
                <div class="modal-search-field">
                  <input v-model="modalFilters.search" type="text" placeholder="Tìm theo tên, mã CTSP, mã sản phẩm..."
                    class="modal-search-inp" />
                  <button v-if="modalFilters.search" class="modal-clear-search-btn"
                    @click="modalFilters.search = ''">×</button>
                </div>
                <button class="btn-modal-reset-filter" @click="resetModalFilters" title="Đặt lại toàn bộ 5 bộ lọc">
                  Đặt lại bộ lọc
                </button>
              </div>
              <div class="modal-5-selects-grid">
                <div class="filter-select-wrap">
                  <SearchableSelect
                    v-model="modalFilters.idThuongHieu"
                    :options="brandFilterOptions"
                    value-key="value"
                    label-key="label"
                    placeholder="Tất cả thương hiệu"
                    search-placeholder="Tìm thương hiệu..."
                    @change="v => { modalFilters.idThuongHieu = v === null ? '' : v }"
                  />
                </div>

                <div class="filter-select-wrap">
                  <SearchableSelect
                    v-model="modalFilters.idLoaiGiay"
                    :options="categoryFilterOptions"
                    value-key="value"
                    label-key="label"
                    placeholder="Tất cả thể loại"
                    search-placeholder="Tìm thể loại..."
                    @change="v => { modalFilters.idLoaiGiay = v === null ? '' : v }"
                  />
                </div>

                <div class="filter-select-wrap">
                  <SearchableSelect
                    v-model="modalFilters.idMauSac"
                    :options="colorFilterOptions"
                    value-key="value"
                    label-key="label"
                    placeholder="Tất cả màu sắc"
                    search-placeholder="Tìm màu sắc..."
                    @change="v => { modalFilters.idMauSac = v === null ? '' : v }"
                  />
                </div>

                <div class="filter-select-wrap">
                  <SearchableSelect
                    v-model="modalFilters.idKichCo"
                    :options="sizeFilterOptions"
                    value-key="value"
                    label-key="label"
                    placeholder="Tất cả kích cỡ"
                    search-placeholder="Tìm kích cỡ..."
                    @change="v => { modalFilters.idKichCo = v === null ? '' : v }"
                  />
                </div>

                <div class="filter-select-wrap">
                  <SearchableSelect
                    v-model="modalFilters.priceRange"
                    :options="priceFilterOptions"
                    value-key="value"
                    label-key="label"
                    placeholder="Tất cả mức giá"
                    search-placeholder="Tìm mức giá..."
                    @change="v => { modalFilters.priceRange = v === null ? '' : v }"
                  />
                </div>
              </div>
            </div>

            <div class="pos-prod-select-summary">
              <span class="select-summary-text">
                Đã chọn <strong>{{ modalSelectedIds.length }}</strong> sản phẩm
                <template v-if="filteredModalVariants.length === 0">(không có kết quả khớp bộ lọc)</template>
              </span>
              <button v-if="modalSelectedIds.length > 0" class="btn-clear-selection" @click="modalSelectedIds = []">Bỏ
                chọn
                tất cả</button>
            </div>

            <div class="pos-prod-table-wrap">
              <table class="pos-prod-table">
                <thead>
                  <tr>
                    <th width="42" class="text-center">
                      <input type="checkbox" class="custom-chk pos-chk" :checked="isAllPageSelected"
                        @change="toggleSelectAllPage" title="Chọn tất cả trên trang này" />
                    </th>
                    <th width="45" class="text-center">STT</th>
                    <th width="64" class="text-center">ẢNH</th>
                    <th width="140">MÃ CTSP</th>
                    <th>SẢN PHẨM</th>
                    <th width="110">MÀU SẮC</th>
                    <th width="75" class="text-center">SIZE</th>
                    <th width="90" class="text-center">TỒN KHO</th>
                    <th width="130" class="text-end">ĐƠN GIÁ</th>
                    <th width="90" class="text-center">CHỌN</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in paginatedModalVariants" :key="item.id"
                    :class="{ 'row-selected': isModalRowSelected(item.id) }" @click="handleSelectProductFromModal(item)"
                    title="Click để thêm nhanh hoặc tick chọn">
                    <td class="text-center" @click.stop>
                      <input type="checkbox" class="custom-chk pos-chk" :checked="isModalRowSelected(item.id)"
                        :disabled="(item.stock ?? item.soLuong ?? 0) <= 0" @change="toggleModalRow(item, $event)" />
                    </td>
                    <td class="text-center prod-stt">{{ modalCurrentPage * modalPageSize + idx + 1 }}</td>
                    <td class="text-center">
                      <div class="modal-thumb-box">
                        <img :src="getShoeImage(item)" class="modal-thumb-img" alt="shoe" />
                      </div>
                    </td>
                    <td>
                      <span class="code-pill-modal">{{ item.maCtsp || item.maChiTietSanPham || item.maSp }}</span>
                    </td>
                    <td class="modal-prod-name">
                      <span class="prod-text-title">{{ item.tenSp || item.tenSanPham }}</span>
                    </td>
                    <td>
                      <div class="modal-color-cell">
                        <span class="modal-color-dot" :style="{ backgroundColor: (item.colorHex || '#64748b') }"></span>
                        <span class="modal-color-name">{{ item.color || item.tenMau || '---' }}</span>
                      </div>
                    </td>
                    <td class="text-center">
                      <span class="badge-size-tag">{{ item.size || item.tenKichCo || '---' }}</span>
                    </td>
                    <td class="text-center">
                      <span class="stock-badge" :class="{
                        'in-stock': (item.stock !== undefined ? item.stock : (item.soLuong || 0)) > 10,
                        'low-stock': (item.stock !== undefined ? item.stock : (item.soLuong || 0)) > 0 && (item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 10,
                        'zero-stock': (item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0
                      }">
                        {{ item.stock !== undefined ? item.stock : (item.soLuong || 0) }}
                      </span>
                    </td>
                    <td class="text-end">
                      <div class="prod-modal-price-box">
                        <span class="price-now">{{ formatVnd(getEffectivePrice(item)) }}</span>
                        <span v-if="item.discount && item.discount !== '-'" class="price-was">{{ formatVnd(item.price ||
                          item.giaBan) }}</span>
                      </div>
                    </td>
                    <td class="text-center" @click.stop>
                      <button class="btn-select-add-cart"
                        :disabled="(item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0"
                        @click="handleSelectProductFromModal(item)">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                          stroke-width="2.5">
                          <line x1="12" y1="5" x2="12" y2="19"></line>
                          <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                        Thêm
                      </button>
                    </td>
                  </tr>
                  <tr v-if="paginatedModalVariants.length === 0">
                    <td colspan="10" class="empty-state-modal">
                      <div class="empty-modal-inner">
                        <span class="empty-icon-shoe">👟</span>
                        <p class="empty-text">Không tìm thấy sản phẩm nào phù hợp với bộ lọc hiện tại.</p>
                        <button class="btn-clear-modal-filters" @click="resetModalFilters">Xóa bộ lọc tìm kiếm</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="pos-prod-footer pos-prod-footer-with-action">
            <div class="pos-modal-page-size">
              <span class="page-size-label">Hiển thị:</span>
              <SearchableSelect
                v-model="modalPageSize"
                :options="pageSizeSelectOptions"
                value-key="value"
                label-key="label"
                placeholder="Số dòng / trang"
                search-placeholder="Tìm số dòng..."
                :allow-clear="false"
              />
              <span class="page-size-total">Tổng <strong>{{ modalTotalElements }}</strong> sản phẩm</span>
            </div>

            <div class="pager-nav-group">
              <button class="pager-nav-btn" :disabled="modalCurrentPage === 0" @click="changeModalPage(0)"
                title="Trang đầu">
                «
              </button>
              <button class="pager-nav-btn" :disabled="modalCurrentPage === 0"
                @click="changeModalPage(modalCurrentPage - 1)" title="Trang trước">
                ‹
              </button>
              <button v-for="p in Math.min(modalTotalPages, 10)" :key="p" class="pager-nav-btn"
                :class="{ active: modalCurrentPage === p - 1 }" @click="changeModalPage(p - 1)">
                {{ p }}
              </button>
              <span v-if="modalTotalPages > 10" class="pager-dots">...</span>
              <button v-if="modalTotalPages > 10" class="pager-nav-btn"
                :class="{ active: modalCurrentPage === modalTotalPages - 1 }"
                @click="changeModalPage(modalTotalPages - 1)">
                {{ modalTotalPages }}
              </button>
              <button class="pager-nav-btn" :disabled="modalCurrentPage >= modalTotalPages - 1"
                @click="changeModalPage(modalCurrentPage + 1)" title="Trang sau">
                ›
              </button>
              <button class="pager-nav-btn" :disabled="modalCurrentPage >= modalTotalPages - 1"
                @click="changeModalPage(modalTotalPages - 1)" title="Trang cuối">
                »
              </button>
            </div>

            <div class="pos-prod-action-group">
              <span class="pos-prod-selected-count">Đã chọn: <strong>{{ modalSelectedIds.length }}</strong></span>
              <button class="btn-pos-secondary" @click="closeProductPicker">Hủy</button>
              <button class="btn-pos-primary" :disabled="modalSelectedIds.length === 0" @click="confirmModalSelection">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
                Thêm vào giỏ hàng ({{ modalSelectedIds.length }})
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="isVoucherModalOpen" class="pos-modal-overlay" @click.self="isVoucherModalOpen = false">
        <div class="pos-modal-dialog modal-voucher-dialog">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <h3>Phiếu giảm giá khả dụng</h3>
              <span class="modal-badge-count">{{ availableEligibleVouchersCount }}/{{ allVouchers.length }} khả
                dụng</span>
            </div>
            <button class="pos-modal-close" @click="isVoucherModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div v-if="bestVoucherSuggestion" class="modal-best-voucher-banner">
              <div class="best-banner-info-left">
                <div class="best-badge-pill">
                  <span class="pill-fire">🔥</span>
                  <span>Đề xuất tốt nhất cho đơn hàng</span>
                </div>
                <div class="best-banner-title">
                  <span class="best-code-tag">{{ bestVoucherSuggestion.voucher.ma }}</span>
                  <span class="best-name-tag">{{ bestVoucherSuggestion.voucher.ten }}</span>
                </div>
                <div class="best-discount-statement">
                  Tiết kiệm nhiều nhất: <strong>-{{ formatVnd(bestVoucherSuggestion.discount) }}</strong>
                  <span class="best-subtotal-tag">(Tổng đơn: {{ formatVnd(subTotal) }})</span>
                </div>
              </div>
              <button type="button" class="btn-apply-best-modal" @click="applyVoucher(bestVoucherSuggestion.voucher)">
                Áp dụng mã này
              </button>
            </div>

            <div class="voucher-modal-search-bar">
              <input v-model="voucherSearchQuery" type="text" placeholder="Nhập mã hoặc tên phiếu giảm giá..."
                class="voucher-search-inp" @keyup.enter="handleModalSearchApply" />
              <button v-if="voucherSearchQuery.trim()" type="button" class="btn-modal-apply-code"
                @click="handleModalSearchApply">
                Áp dụng mã
              </button>
            </div>



            <div class="voucher-cards-list">
              <div v-for="v in sortedFilteredVouchers" :key="v.id" class="voucher-card-item" :class="{
                eligible: isVoucherEligible(v),
                selected: currentOrder?.selectedVoucher?.id === v.id,
                ineligible: !isVoucherEligible(v),
                isBestItem: bestVoucherSuggestion?.voucher?.id === v.id
              }">
                <div class="voucher-card-left">
                  <div class="voucher-discount-highlight">
                    {{ formatVoucherValue(v) }}
                  </div>
                  <div class="voucher-type-sub">
                    {{ v.loaiGiamGia === 'PERCENT' || v.loaiGiamGia === '%' ? 'GIẢM %' : 'GIẢM TIỀN' }}
                  </div>
                  <span v-if="bestVoucherSuggestion?.voucher?.id === v.id" class="best-ticket-star">
                    ⭐ Tốt nhất
                  </span>
                </div>

                <div class="voucher-card-body">
                  <div class="voucher-card-header-row">
                    <span class="voucher-card-code">{{ v.ma }}</span>
                    <span v-if="bestVoucherSuggestion?.voucher?.id === v.id" class="badge-best-voucher-tag">
                      🔥 Giảm nhiều nhất
                    </span>
                    <span v-if="v.soLuong" class="voucher-card-remain">
                      Còn {{ Math.max(0, (v.soLuong || 0) - (v.soLuongDaSuDung || 0)) }} lượt
                    </span>
                  </div>
                  <div class="voucher-card-name">{{ v.ten }}</div>
                  <div class="voucher-card-conditions">
                    <span>Đơn tối thiểu: <strong>{{ formatVnd(v.hoaDonToiThieu || 0) }}</strong></span>
                    <span v-if="v.giamToiDa"> • Tối đa: <strong>{{ formatVnd(v.giamToiDa) }}</strong></span>
                  </div>

                  <div v-if="isVoucherEligible(v) && subTotal > 0" class="voucher-calc-saving">
                    Tiết kiệm cho đơn này: <strong>-{{ formatVnd(calculateVoucherDiscount(v, subTotal)) }}</strong>
                  </div>

                  <div class="voucher-card-date" v-if="v.ngayKetThuc">
                    Hạn dùng: {{ formatDateVn(v.ngayKetThuc) }}
                  </div>
                  <div v-if="!isVoucherEligible(v)" class="voucher-ineligible-msg">
                    {{ getVoucherIneligibilityReason(v) }}
                  </div>
                </div>

                <div class="voucher-card-action">
                  <button v-if="currentOrder?.selectedVoucher?.id === v.id" type="button" class="btn-voucher-applied"
                    @click="removeVoucher" title="Bấm để gỡ bỏ voucher">
                    Đang dùng (Gỡ)
                  </button>
                  <button v-else type="button" class="btn-apply-voucher" :disabled="!isVoucherEligible(v)"
                    @click="applyVoucher(v)">
                    Áp dụng
                  </button>
                </div>
              </div>

              <div v-if="sortedFilteredVouchers.length === 0" class="empty-vouchers-msg">
                Không tìm thấy phiếu giảm giá nào phù hợp với "{{ voucherSearchQuery }}".
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="isQrModalOpen" class="pos-modal-overlay" @click.self="closeQrScanner">
        <div class="pos-modal-dialog modal-camera-dialog">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                  <circle cx="12" cy="13" r="4"></circle>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Quét Camera thêm sản phẩm</h3>
                <span class="modal-badge-count">Nhận diện QR & Barcode</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="closeQrScanner" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="pos-camera-scan-wrap">
              <div class="pos-camera-viewport">
                <div id="pos-camera-scanner-view" class="pos-camera-stream-el"></div>
                <div v-if="isCameraRunning && !scanSuccessAnim" class="pos-scanner-laser-line"></div>
                <div class="pos-scanner-frame-overlay">
                  <span class="corner top-left"></span>
                  <span class="corner top-right"></span>
                  <span class="corner bottom-left"></span>
                  <span class="corner bottom-right"></span>
                </div>

                <div v-if="scanSuccessAnim" class="pos-scan-success-overlay">
                  <div class="success-icon-badge">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="3">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                  </div>
                  <span class="success-text-title">Quét thành công!</span>
                  <span class="success-product-sub">{{ scannedProductName }}</span>
                </div>
              </div>

              <p class="camera-guide-hint">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="16" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
                <span>Hướng camera vào <b>mã QR / Barcode</b> trên tem giày hoặc hộp để tự động nhận diện.</span>
              </p>

              <div v-if="cameraError" class="pos-camera-error-banner">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <span>{{ cameraError }}</span>
              </div>

              <div class="pos-manual-code-row">
                <input v-model="scannedCodeInput" type="text" class="pos-manual-code-inp"
                  placeholder="Hoặc nhập mã CTSP / Barcode thủ công rồi bấm Thêm..."
                  @keyup.enter="handleManualCodeSubmit" />
                <button class="btn-pos-manual-add" @click="handleManualCodeSubmit">
                  Thêm
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="isAddCustomerModalOpen" class="pos-modal-overlay" @click.self="isAddCustomerModalOpen = false">
        <div class="pos-modal-dialog modal-customer-dialog">
          <div class="pos-modal-head cust-modal-header">
            <div class="modal-head-title">
              <div class="modal-title-icon customer-icon-badge">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="8.5" cy="7" r="4"></circle>
                  <line x1="20" y1="8" x2="20" y2="14"></line>
                  <line x1="23" y1="11" x2="17" y2="11"></line>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3 class="cust-modal-title">Thêm nhanh khách hàng</h3>
                <span class="cust-modal-subtitle">Tạo thông tin khách hàng mới và tự động gán vào đơn hàng</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isAddCustomerModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body modal-customer-body">
            <div class="customer-form-grid">
              <div class="cust-form-group">
                <label class="cust-label">Họ và tên <span class="required-star">*</span></label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                      <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                  </span>
                  <input type="text" class="cust-input" placeholder="VD: Nguyễn Văn Anh"
                    v-model="newCustomerForm.tenKhachHang" @keyup.enter="saveQuickCustomer" />
                </div>
              </div>

              <div class="cust-form-group">
                <label class="cust-label">Số điện thoại <span class="required-star">*</span></label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path
                        d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z">
                      </path>
                    </svg>
                  </span>
                  <input type="tel" class="cust-input" placeholder="VD: 0912345678"
                    v-model="newCustomerForm.soDienThoai" @keyup.enter="saveQuickCustomer" />
                </div>
              </div>

              <div class="cust-form-group">
                <label class="cust-label">Email</label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                      <polyline points="22,6 12,13 2,6"></polyline>
                    </svg>
                  </span>
                  <input type="email" class="cust-input" placeholder="VD: khachhang@gmail.com"
                    v-model="newCustomerForm.email" />
                </div>
              </div>

              <div class="cust-form-group">
                <label class="cust-label">Giới tính</label>
                <div class="cust-gender-group">
                  <label class="cust-gender-pill" :class="{ active: newCustomerForm.gioiTinh === 'Nam' }">
                    <input type="radio" value="Nam" v-model="newCustomerForm.gioiTinh" />
                    <span>Nam</span>
                  </label>
                  <label class="cust-gender-pill" :class="{ active: newCustomerForm.gioiTinh === 'Nữ' }">
                    <input type="radio" value="Nữ" v-model="newCustomerForm.gioiTinh" />
                    <span>Nữ</span>
                  </label>
                  <label class="cust-gender-pill" :class="{ active: newCustomerForm.gioiTinh === 'Khác' }">
                    <input type="radio" value="Khác" v-model="newCustomerForm.gioiTinh" />
                    <span>Khác</span>
                  </label>
                </div>
              </div>

              <div class="cust-form-group">
                <label class="cust-label">Ngày sinh</label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                      <line x1="16" y1="2" x2="16" y2="6"></line>
                      <line x1="8" y1="2" x2="8" y2="6"></line>
                      <line x1="3" y1="10" x2="21" y2="10"></line>
                    </svg>
                  </span>
                  <input type="date" class="cust-input" v-model="newCustomerForm.ngaySinh" />
                </div>
              </div>

              <div class="cust-form-group cust-full-span">
                <label class="cust-label">Địa chỉ</label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon top-aligned">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                      <circle cx="12" cy="10" r="3"></circle>
                    </svg>
                  </span>
                  <textarea rows="2" class="cust-textarea"
                    placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
                    v-model="newCustomerForm.diaChi"></textarea>
                </div>
              </div>
            </div>

            <div class="cust-hint-card">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#0284c7" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
              <span>Khách hàng sau khi lưu sẽ tự động được chọn vào hóa đơn hiện tại.</span>
            </div>
          </div>

          <div class="pos-modal-footer cust-modal-footer">
            <button class="btn-cust-cancel" @click="isAddCustomerModalOpen = false" :disabled="isSavingCustomer">
              Hủy
            </button>
            <button class="btn-cust-save" :disabled="isSavingCustomer" @click="saveQuickCustomer">
              <span v-if="isSavingCustomer" class="spinner-inline"></span>
              <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                <polyline points="17 21 17 13 7 13 7 21"></polyline>
                <polyline points="7 3 7 8 15 8"></polyline>
              </svg>
              <span>{{ isSavingCustomer ? 'Đang lưu...' : 'Lưu khách hàng' }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="isSwapModalOpen" class="pos-modal-overlay" @click.self="isSwapModalOpen = false">
        <div class="pos-modal-dialog modal-swap-variant">
          <div class="pos-modal-head">
            <div class="swap-modal-head-titles">
              <div class="swap-head-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M7 16V4m0 0L3 8m4-4l4 4m6 4v12m0 0l4-4m-4 4l-4-4" />
                </svg>
              </div>
              <div>
                <h3 class="pos-modal-title">Đổi Size / Phối Màu (Smart Swap)</h3>
                <span class="pos-modal-subtitle">Hoán đổi biến thể tức thì trên giỏ hàng không cần xóa đi thêm
                  lại</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isSwapModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="current-swap-banner" v-if="swapCartItem">
              <div class="csb-left">
                <img v-if="swapCartItem.img" :src="swapCartItem.img" alt="Ảnh SP" class="csb-img" />
                <div v-else class="csb-no-img">👟</div>
                <div class="csb-details">
                  <span class="csb-title">{{ swapCartItem.name }}</span>
                  <div class="csb-pills">
                    <span class="csb-pill pill-code">Mã: {{ swapCartItem.maCtsp || swapCartItem.maSp }}</span>
                    <span class="csb-pill pill-current">Đang chọn: <strong>Size {{ swapCartItem.size }}</strong> •
                      <strong>{{ swapCartItem.color }}</strong></span>
                    <span class="csb-pill pill-qty">Số lượng trong giỏ: <strong>{{ swapCartItem.quantity
                    }}</strong></span>
                  </div>
                </div>
              </div>
              <div class="csb-price-box">
                <span class="csb-price-lbl">Đơn giá hiện tại:</span>
                <span class="csb-price-val">{{ formatVnd(swapCartItem.price) }}</span>
              </div>
            </div>

            <div class="swap-color-filter-bar" v-if="availableSwapColors.length > 1">
              <span class="filter-bar-title">Lọc theo màu sắc:</span>
              <div class="filter-chips-list">
                <button type="button" class="swap-chip-btn" :class="{ active: swapColorFilter === 'ALL' }"
                  @click="swapColorFilter = 'ALL'">
                  Tất cả màu ({{ swapVariants.length }})
                </button>
                <button v-for="color in availableSwapColors" :key="color" type="button" class="swap-chip-btn"
                  :class="{ active: swapColorFilter === color }" @click="swapColorFilter = color">
                  {{ color }}
                </button>
              </div>
            </div>

            <div v-if="isLoadingSwapVariants" class="swap-loading-box">
              <div class="spinner-sm"></div>
              <span>Đang kiểm tra tồn kho các biến thể cùng dòng...</span>
            </div>

            <div v-else-if="filteredSwapVariants.length > 0" class="swap-variants-matrix">
              <div v-for="variant in filteredSwapVariants" :key="variant.id" class="variant-swap-card" :class="{
                'is-current-active': variant.id === swapCartItem?.idCtsp,
                'is-card-selected': selectedNewVariant?.id === variant.id,
                'is-stock-depleted': variant.id !== swapCartItem?.idCtsp && (variant.currentStock < (swapCartItem?.quantity || 1))
              }"
                @click="variant.id !== swapCartItem?.idCtsp && variant.currentStock >= (swapCartItem?.quantity || 1) && (selectedNewVariant = variant)">
                <div class="vsc-top-bar">
                  <span v-if="variant.id === swapCartItem?.idCtsp" class="badge-vsc current">Đang ở trong giỏ</span>
                  <span v-else-if="variant.currentStock < (swapCartItem?.quantity || 1)"
                    class="badge-vsc out-stock">Không
                    đủ hàng</span>
                  <span v-else class="badge-vsc in-stock">Còn {{ variant.currentStock }} đôi</span>

                  <span class="vsc-color-pill">
                    <span class="vsc-dot" :style="{ backgroundColor: variant.colorHex || '#64748b' }"></span>
                    {{ variant.color || variant.tenMau }}
                  </span>
                </div>

                <div class="vsc-body">
                  <div class="vsc-size-display">
                    <span class="size-large">{{ variant.size || variant.tenKichCo }}</span>
                    <span class="size-sub">Kích cỡ</span>
                  </div>

                  <div class="vsc-price-col">
                    <div class="vsc-price-val">{{ formatVnd(getEffectivePrice(variant)) }}</div>
                    <div class="vsc-price-diff">
                      <span v-if="getEffectivePrice(variant) === swapCartItem?.price" class="diff-tag same">Đồng
                        giá</span>
                      <span v-else-if="getEffectivePrice(variant) > swapCartItem?.price" class="diff-tag up">
                        +{{ formatVnd(getEffectivePrice(variant) - swapCartItem?.price) }}
                      </span>
                      <span v-else class="diff-tag down">
                        -{{ formatVnd(swapCartItem?.price - getEffectivePrice(variant)) }}
                      </span>
                    </div>
                  </div>
                </div>

                <div class="vsc-foot">
                  <button type="button" v-if="variant.id === swapCartItem?.idCtsp" class="btn-vsc-btn disabled-current"
                    disabled>
                    Dòng hiện tại
                  </button>
                  <button type="button" v-else-if="variant.currentStock < (swapCartItem?.quantity || 1)"
                    class="btn-vsc-btn disabled-stock" disabled>
                    Hết kho (Còn {{ variant.currentStock }})
                  </button>
                  <button type="button" v-else class="btn-vsc-btn select-action"
                    :class="{ active: selectedNewVariant?.id === variant.id }"
                    @click.stop="selectedNewVariant = variant">
                    <span v-if="selectedNewVariant?.id === variant.id">✓ Đã chọn đổi</span>
                    <span v-else>Chọn đổi sang đôi này</span>
                  </button>
                </div>
              </div>
            </div>

            <div v-else class="swap-empty-box">
              <p>Không tìm thấy biến thể nào phù hợp theo bộ lọc này.</p>
            </div>
          </div>

          <div class="pos-modal-foot">
            <button type="button" class="btn-cancel-modal" @click="isSwapModalOpen = false">
              Hủy bỏ
            </button>
            <button type="button" class="btn-confirm-swap-action"
              :disabled="!selectedNewVariant || selectedNewVariant.id === swapCartItem?.idCtsp"
              @click="executeSwap(selectedNewVariant)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              <span v-if="selectedNewVariant">
                Xác nhận đổi sang Size {{ selectedNewVariant.size || selectedNewVariant.tenKichCo }} ({{
                  selectedNewVariant.color || selectedNewVariant.tenMau }})
              </span>
              <span v-else>Chọn 1 biến thể ở trên để đổi</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="isBankQrModalOpen" class="pos-modal-overlay" @click.self="isBankQrModalOpen = false">
        <div class="pos-modal-dialog modal-bank-qr" :class="{ 'customer-mode-active': isCustomerDisplayMode }">
          <div v-if="isPaymentSuccessAnim" class="bank-success-overlay">
            <div class="bso-card">
              <div class="bso-icon-ring">
                <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </div>
              <h4 class="bso-title">THANH TOÁN THÀNH CÔNG!</h4>
              <p class="bso-amount">{{ formatVnd(bankTransferAmount) }}</p>
              <span class="bso-memo">Hóa đơn: {{ currentOrder?.code }}</span>
            </div>
          </div>

          <div class="pos-modal-head">
            <div class="bank-modal-head-titles">
              <div class="title-with-badge">
                <h3 class="pos-modal-title">Thanh toán chuyển khoản VietQR</h3>
                <span class="napas-badge-head">Napas 24/7</span>
              </div>
              <span class="pos-modal-subtitle">Quét mã bằng app ngân hàng bất kỳ để tự động điền STK & Số tiền</span>
            </div>
            <div class="bank-modal-head-actions">
              <button type="button" class="btn-toggle-customer-mode" :class="{ active: isCustomerDisplayMode }"
                @click="isCustomerDisplayMode = !isCustomerDisplayMode"
                title="Bật/Tắt chế độ màn hình lớn cho khách quét">
                <span v-if="!isCustomerDisplayMode">🖥️ Chế độ cho khách</span>
                <span v-else>📋 Chế độ thu ngân</span>
              </button>
              <button class="pos-modal-close" @click="isBankQrModalOpen = false">×</button>
            </div>
          </div>

          <div class="pos-modal-body p-0">
            <div class="bank-selector-pills-bar" v-if="!isCustomerDisplayMode">
              <span class="bs-label">Chọn tài khoản nhận:</span>
              <div class="bs-options">
                <button v-for="b in storeBankAccounts" :key="b.id" type="button" class="bs-pill-btn"
                  :class="{ active: selectedBank.id === b.id }" @click="selectBank(b)">
                  <span class="bs-dot" :style="{ backgroundColor: b.color }"></span>
                  <span class="bs-name">{{ b.name }}</span>
                  <span class="bs-tag" v-if="b.tag">{{ b.tag }}</span>
                </button>
              </div>
            </div>

            <div v-if="isCustomerDisplayMode" class="customer-qr-display-container">
              <div class="cqr-header">
                <div class="cqr-store-brand">POLYSHOES STORE</div>
                <div class="cqr-prompt">Quý khách mở ứng dụng ngân hàng bất kỳ để quét mã</div>
              </div>

              <div class="cqr-card-big">
                <div class="cqr-img-box">
                  <img v-if="!isQrImgFailed && vietQrNetworkUrl" :src="vietQrNetworkUrl" @error="onQrImgError"
                    class="cqr-img" alt="Mã VietQR lớn" />
                  <img v-else-if="bankQrDataUrl" :src="bankQrDataUrl" class="cqr-img" alt="Mã QR Chuyển khoản" />
                </div>
                <div class="cqr-amount-box">
                  <span class="cqr-amount-lbl">SỐ TIỀN THANH TOÁN</span>
                  <span class="cqr-amount-val">{{ formatVnd(bankTransferAmount) }}</span>
                </div>
              </div>

              <div class="cqr-bank-recap">
                <span class="cqr-recap-item">Ngân hàng: <strong>{{ selectedBank.name }}</strong></span>
                <span class="cqr-recap-sep">•</span>
                <span class="cqr-recap-item">Chủ TK: <strong>{{ selectedBank.accountName }}</strong></span>
                <span class="cqr-recap-sep">•</span>
                <span class="cqr-recap-item">Nội dung CK: <strong>{{ currentOrder?.code }}</strong></span>
              </div>
            </div>

            <div v-else class="bank-qr-layout">
              <div class="bank-qr-visual-card">
                <div class="vietqr-badge-header">
                  <div class="vietqr-brand-logo">
                    <span class="vietqr-text-viet">Viet</span><span class="vietqr-text-qr">QR</span>
                  </div>
                  <span class="bank-napas-tag">{{ selectedBank.shortName }} • Napas 247</span>
                </div>

                <div class="bank-qr-image-wrapper">
                  <img v-if="!isQrImgFailed && vietQrNetworkUrl" :src="vietQrNetworkUrl" @error="onQrImgError"
                    class="vietqr-render-img" alt="Mã VietQR" />
                  <img v-else-if="bankQrDataUrl" :src="bankQrDataUrl" class="vietqr-render-img"
                    alt="Mã QR Chuyển khoản" />
                </div>

                <div class="bank-qr-scan-guide">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 14 14"></polyline>
                  </svg>
                  <span>Tự động nhận diện STK & Số tiền</span>
                </div>
              </div>

              <div class="bank-details-pane">
                <div class="qr-amount-callout">
                  <span class="amount-callout-label">Số tiền cần thanh toán</span>
                  <div class="amount-callout-val">
                    <span class="amount-number">{{ formatVnd(bankTransferAmount) }}</span>
                    <button class="btn-copy-chip" @click="copyToClipboard(bankTransferAmount, 'số tiền')"
                      title="Sao chép số tiền">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="2">
                        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                      </svg>
                    </button>
                  </div>
                </div>

                <div class="bank-info-table">
                  <div class="bank-info-row">
                    <span class="info-k">Hóa đơn:</span>
                    <span class="info-v highlight-order">{{ currentOrder?.title }} (<strong>{{ currentOrder?.code
                    }}</strong>)</span>
                  </div>

                  <div class="bank-info-row">
                    <span class="info-k">Ngân hàng:</span>
                    <span class="info-v">
                      <strong :style="{ color: selectedBank.color }">{{ selectedBank.name }}</strong>
                    </span>
                  </div>

                  <div class="bank-info-row">
                    <span class="info-k">Chủ tài khoản:</span>
                    <span class="info-v strong-text">{{ selectedBank.accountName }}</span>
                  </div>

                  <div class="bank-info-row">
                    <span class="info-k">Số tài khoản:</span>
                    <div class="info-v-copy">
                      <span class="copy-text-code">{{ selectedBank.accountNo }}</span>
                      <button class="btn-copy-mini" @click="copyToClipboard(selectedBank.accountNo, 'số tài khoản')"
                        title="Sao chép số tài khoản">
                        Sao chép
                      </button>
                    </div>
                  </div>

                  <div class="bank-info-row">
                    <span class="info-k">Nội dung CK:</span>
                    <div class="info-v-copy">
                      <span class="copy-text-code highlight-memo">{{ currentOrder?.code }}</span>
                      <button class="btn-copy-mini"
                        @click="copyToClipboard(currentOrder?.code, 'nội dung chuyển khoản')" title="Sao chép nội dung">
                        Sao chép
                      </button>
                    </div>
                  </div>
                </div>

                <div class="bank-notice-alert">
                  <span class="notice-icon">ℹ</span>
                  <span>Nhấn <strong>Đã nhận tiền</strong> khi ứng dụng ngân hàng báo tiền về để hoàn tất.</span>
                </div>
              </div>
            </div>
          </div>

          <div class="pos-modal-foot bank-modal-foot">
            <button class="btn-cancel-modal" @click="isBankQrModalOpen = false">
              Đóng
            </button>
            <button class="btn-confirm-received" @click="confirmBankPaymentReceived">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              <span>Đã nhận tiền (Xác nhận) 🔔</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="isReceiptModalOpen && completedInvoiceData" class="pos-modal-overlay"
        @click.self="isReceiptModalOpen = false">
        <div class="pos-modal-dialog modal-receipt-dialog">
          <div class="pos-modal-head">
            <div class="receipt-head-text">
              <h3 class="pos-modal-title">Hóa đơn bán hàng</h3>
              <span class="receipt-head-sub">Xem trước & in hóa đơn thanh toán</span>
            </div>
            <button class="pos-modal-close" @click="isReceiptModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body receipt-modal-body">
            <div class="thermal-receipt-box" id="printable-receipt-content">
              <!-- Header tiệm -->
              <div class="receipt-header-center">
                <div class="receipt-brand-logo">POLYSHOES STORE</div>
                <p class="receipt-sub-line">Giày sneaker & phụ kiện thời trang chính hãng</p>
                <p class="receipt-sub-line">Đ/C: Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</p>
                <p class="receipt-sub-line">Hotline: 0987.654.321 • Website: polyshoes.vn</p>
              </div>

              <!-- Tiêu đề & Thông tin đơn -->
              <div class="receipt-bill-heading">
                <div class="receipt-bill-title">HÓA ĐƠN BÁN LẺ (POS)</div>
                <div class="receipt-barcode-code">*{{ completedInvoiceData.code }}*</div>
              </div>

              <div class="receipt-meta-info">
                <div class="meta-row">
                  <span class="meta-k">Mã hóa đơn:</span>
                  <span class="meta-v"><strong>{{ completedInvoiceData.code }}</strong></span>
                </div>
                <div class="meta-row">
                  <span class="meta-k">Thời gian:</span>
                  <span class="meta-v">{{ completedInvoiceData.date }}</span>
                </div>
                <div class="meta-row">
                  <span class="meta-k">Khách hàng:</span>
                  <span class="meta-v"><strong>{{ completedInvoiceData.customerName }}</strong></span>
                </div>
                <div class="meta-row" v-if="completedInvoiceData.customerPhone">
                  <span class="meta-k">Số điện thoại:</span>
                  <span class="meta-v">{{ completedInvoiceData.customerPhone }}</span>
                </div>
                <div class="meta-row">
                  <span class="meta-k">Thu ngân:</span>
                  <span class="meta-v">{{ completedInvoiceData.staffName || 'Nhân viên quầy' }}</span>
                </div>
              </div>

              <table class="receipt-items-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th class="text-center" width="36">SL</th>
                    <th class="text-end" width="80">Đơn giá</th>
                    <th class="text-end" width="95">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in completedInvoiceData.cart" :key="item.idCtsp">
                    <td>
                      <div class="rc-item-name">{{ item.name }}</div>
                      <div class="rc-item-variant">Size {{ item.size }} • {{ item.color }}</div>
                    </td>
                    <td class="text-center rc-item-qty">{{ item.quantity }}</td>
                    <td class="text-end rc-item-price">{{ formatVnd(item.price) }}</td>
                    <td class="text-end rc-item-total">{{ formatVnd(item.price * item.quantity) }}</td>
                  </tr>
                </tbody>
              </table>

              <div class="receipt-totals-block">
                <div class="receipt-sum-row">
                  <span class="sum-k">Tổng tiền hàng:</span>
                  <strong class="sum-v">{{ formatVnd(completedInvoiceData.subTotal) }}</strong>
                </div>
                <div class="receipt-sum-row text-discount" v-if="completedInvoiceData.discount > 0">
                  <span class="sum-k">Giảm giá (Voucher):</span>
                  <strong class="sum-v discount-val">- {{ formatVnd(completedInvoiceData.discount) }}</strong>
                </div>
                <div class="receipt-sum-row" v-if="completedInvoiceData.shippingFee > 0">
                  <span class="sum-k">Phí vận chuyển:</span>
                  <strong class="sum-v">+ {{ formatVnd(completedInvoiceData.shippingFee) }}</strong>
                </div>
                <div class="receipt-sum-row grand-sum">
                  <span class="sum-k">TỔNG THANH TOÁN:</span>
                  <span class="grand-sum-val">{{ formatVnd(completedInvoiceData.finalTotal) }}</span>
                </div>
                <div class="receipt-sum-row">
                  <span class="sum-k">Phương thức:</span>
                  <span class="sum-v">{{ formatPaymentMethodName(completedInvoiceData.paymentMethod) }}</span>
                </div>
                <div class="receipt-sum-row"
                  v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT' || completedInvoiceData.paymentMethod === 'KET_HOP'">
                  <span class="sum-k">Tiền khách đưa:</span>
                  <span class="sum-v">{{ formatVnd(completedInvoiceData.cashGiven) }}</span>
                </div>
                <div class="receipt-sum-row"
                  v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT' || completedInvoiceData.paymentMethod === 'KET_HOP'">
                  <span class="sum-k">Tiền thừa trả khách:</span>
                  <span class="sum-v">{{ formatVnd(completedInvoiceData.changeDue) }}</span>
                </div>
              </div>

              <div class="receipt-footer-notes">
                <p class="rc-thank-txt">Cảm ơn Quý khách và hẹn gặp lại!</p>
                <p class="rc-policy-txt">Quý khách vui lòng giữ hóa đơn để được đổi size/mẫu trong vòng 7 ngày.</p>
              </div>
            </div>
          </div>

          <div class="pos-modal-foot receipt-modal-foot">
            <button class="btn-receipt-close" @click="isReceiptModalOpen = false">
              Đóng
            </button>
            <button class="btn-receipt-print" @click="printReceipt">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 6 2 18 2 18 9"></polyline>
                <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                <rect x="6" y="14" width="12" height="8"></rect>
              </svg>
              <span>In hóa đơn</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Modal Mo ca (khong dong duoc khi click overlay - chua mo ca thi khong ban duoc) -->
    <transition name="fade">
      <div v-if="isMoCaModalOpen" class="pos-modal-overlay">
        <div class="pos-modal-dialog modal-sm ca-modal-dialog">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="16" y1="2" x2="16" y2="6"></line>
                  <line x1="8" y1="2" x2="8" y2="6"></line>
                  <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Mở ca làm việc</h3>
                <span class="modal-badge-count">Bắt buộc trước khi bán</span>
              </div>
            </div>
          </div>

          <div class="pos-modal-body">
            <div class="cust-form-group ca-form-group">
              <label class="cust-label">Tiền đầu ca <span class="required-star">*</span></label>
              <input
                type="number"
                class="cust-input ca-input-plain"
                min="0"
                step="1000"
                placeholder="VD: 500000"
                v-model.number="moCaForm.tienDauCa"
                @keyup.enter="confirmMoCa"
              />
              <span class="ca-input-hint">Bội số của 1.000đ, tối thiểu 0đ</span>
            </div>
            <div class="ca-field-error" v-if="moCaError">{{ moCaError }}</div>

            <div class="cust-form-group ca-form-group">
              <label class="cust-label">Ghi chú</label>
              <textarea
                rows="2"
                class="cust-textarea ca-textarea-plain"
                placeholder="Ghi chú khi mở ca (tuỳ chọn)"
                v-model="moCaForm.ghiChu"
              ></textarea>
            </div>
          </div>

          <div class="pos-modal-footer">
            <span class="ca-modal-hint">Chưa mở ca, giao dịch tại quầy bị tạm khóa.</span>
            <button class="btn-modal-submit" :disabled="isSavingMoCa" @click="confirmMoCa">
              {{ isSavingMoCa ? 'Đang mở ca...' : 'Xác nhận mở ca' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Modal Ket ca / Doi soat -->
    <transition name="fade">
      <div v-if="isKetCaModalOpen" class="pos-modal-overlay" @click.self="isKetCaModalOpen = false">
        <div class="pos-modal-dialog modal-md ca-modal-dialog">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Kết ca & Đối soát</h3>
                <span class="modal-badge-count" v-if="ketCaTongKet">{{ ketCaTongKet.maCa }}</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isKetCaModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div v-if="isLoadingTongKet" class="ca-loading-box">Đang tải đối soát ca...</div>

            <template v-else-if="ketCaTongKet">
              <div class="ca-info-row">
                <span><strong>Ca:</strong> {{ ketCaTongKet.maCa }}</span>
                <span><strong>Nhân viên:</strong> {{ ketCaTongKet.tenNhanVien || staffName }}</span>
                <span><strong>Mở lúc:</strong> {{ formatDateTime(ketCaTongKet.thoiGianMo) }}</span>
              </div>

              <div class="ca-doi-soat-grid">
                <div class="ca-ds-item">
                  <span class="ca-ds-label">Tiền đầu ca</span>
                  <span class="ca-ds-value">{{ formatVnd(ketCaTongKet.tienDauCa) }}</span>
                </div>
                <div class="ca-ds-item">
                  <span class="ca-ds-label">Tổng hóa đơn</span>
                  <span class="ca-ds-value">{{ ketCaTongKet.tongHoaDon || 0 }}</span>
                </div>
                <div class="ca-ds-item">
                  <span class="ca-ds-label">Doanh thu</span>
                  <span class="ca-ds-value">{{ formatVnd(ketCaTongKet.tongDoanhThu) }}</span>
                </div>
                <div class="ca-ds-item">
                  <span class="ca-ds-label">Tiền mặt thu</span>
                  <span class="ca-ds-value">{{ formatVnd(ketCaTongKet.tienMatThu) }}</span>
                </div>
                <div class="ca-ds-item">
                  <span class="ca-ds-label">Không tiền mặt</span>
                  <span class="ca-ds-value">{{ formatVnd(ketCaTongKet.tienKhongMat) }}</span>
                </div>
                <div class="ca-ds-item">
                  <span class="ca-ds-label">Tiền hoàn (mặt)</span>
                  <span class="ca-ds-value">{{ formatVnd(ketCaTongKet.tienHoanMat) }}</span>
                </div>
                <div class="ca-ds-item ca-ds-highlight">
                  <span class="ca-ds-label">Dự kiến</span>
                  <span class="ca-ds-value">{{ formatVnd(ketCaTongKet.tienDuKien) }}</span>
                </div>
              </div>

              <div class="ca-hd-section" v-if="ketCaTongKet.hoaDonGanDay && ketCaTongKet.hoaDonGanDay.length > 0">
                <div class="ca-hd-title">Hóa đơn gần nhất</div>
                <table class="ca-hd-table">
                  <thead>
                    <tr>
                      <th>Mã HĐ</th>
                      <th>Ngày tạo</th>
                      <th>Phương thức</th>
                      <th class="text-end">Tiền sau giảm</th>
                      <th>Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="hd in ketCaTongKet.hoaDonGanDay" :key="hd.maHoaDon">
                      <td>{{ hd.maHoaDon }}</td>
                      <td>{{ formatDateTime(hd.ngayTao) }}</td>
                      <td>{{ hd.tenPhuongThuc || '—' }}</td>
                      <td class="text-end">{{ formatVnd(hd.tienSauGiamGia) }}</td>
                      <td>{{ hd.trangThai || '—' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="cust-form-group ca-form-group">
                <label class="cust-label">Tiền thực tế <span class="required-star">*</span></label>
                <input
                  type="number"
                  class="cust-input ca-input-plain"
                  min="0"
                  step="1000"
                  placeholder="Nhập số tiền thực tế trong két"
                  v-model.number="ketCaForm.tienThucTe"
                />
              </div>

              <div class="ca-chenh-lech-box" :class="chenhLechKetCa === 0 ? 'lech-khong' : 'lech-khac'">
                <span>Chênh lệch (thực tế − dự kiến):</span>
                <strong>{{ formatVnd(chenhLechKetCa) }}</strong>
              </div>

              <div class="cust-form-group ca-form-group" v-if="chenhLechKetCa !== 0">
                <label class="cust-label">Lý do lệch <span class="required-star">*</span></label>
                <input
                  type="text"
                  class="cust-input ca-input-plain"
                  placeholder="Nhập lý do chênh lệch (bắt buộc khi lệch khác 0)"
                  v-model="ketCaForm.lyDoLech"
                />
              </div>

              <div class="cust-form-group ca-form-group">
                <label class="cust-label">Ghi chú kết ca</label>
                <input
                  type="text"
                  class="cust-input ca-input-plain"
                  placeholder="Tuỳ chọn"
                  v-model="ketCaForm.ghiChu"
                />
              </div>

              <div class="ca-field-error" v-if="ketCaError">{{ ketCaError }}</div>
              <div class="ca-field-error" v-if="ketCaLichError.length > 0">
                <div>Còn hóa đơn đang xử lý, vui lòng xử lý trước khi kết ca:</div>
                <ul class="ca-hd-list">
                  <li v-for="(m, i) in ketCaLichError" :key="i">{{ m }}</li>
                </ul>
              </div>
            </template>
          </div>

          <div class="pos-modal-footer">
            <button class="btn-modal-cancel" @click="isKetCaModalOpen = false" :disabled="isSavingKetCa">
              Đóng
            </button>
            <button
              class="btn-modal-submit"
              :disabled="isSavingKetCa || isLoadingTongKet || !ketCaTongKet"
              @click="confirmKetCa"
            >
              {{ isSavingKetCa ? 'Đang kết ca...' : 'Xác nhận kết ca' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<style scoped src="@/assets/styles/PosSalesPage.css"></style>

<style scoped>
/* Giao ca banner */
.pos-ca-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 18px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pos-ca-banner.ca-closed {
  background: #fef2f2;
  border-color: #fecaca;
}

.pos-ca-banner.ca-open {
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.pos-ca-banner.loading {
  background: #f8fafc;
  border-color: #e2e8f0;
}

.ca-banner-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ca-banner-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.ca-closed .ca-banner-dot { background: #dc2626; box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15); }
.ca-open .ca-banner-dot { background: #10b981; box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2); }
.loading .ca-banner-dot { background: #94a3b8; }

.ca-banner-text {
  font-size: 13.5px;
  font-weight: 700;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ca-closed .ca-banner-text { color: #b91c1c; }
.ca-open .ca-banner-text { color: #047857; }

.btn-ca-action {
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.btn-ca-mo {
  background: #dc2626;
  color: #ffffff;
  box-shadow: 0 2px 4px rgba(220, 38, 38, 0.2);
}

.btn-ca-mo:hover { background: #b91c1c; }

.btn-ca-ket {
  background: #ffffff;
  border: 1px solid #fca5a5;
  color: #b91c1c;
}

.btn-ca-ket:hover { background: #fef2f2; }

/* Mo/Ket ca modal */
.ca-modal-dialog .ca-form-group {
  margin-top: 12px;
  gap: 6px;
}

.ca-input-plain {
  padding-left: 12px;
}

.ca-textarea-plain {
  padding-left: 12px;
}

.ca-input-hint {
  font-size: 11.5px;
  color: #94a3b8;
}

.ca-field-error {
  margin-top: 8px;
  padding: 8px 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #b91c1c;
  font-size: 12.5px;
  font-weight: 600;
}

.ca-modal-hint {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
}

.ca-loading-box {
  padding: 28px 12px;
  text-align: center;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.ca-info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 12.5px;
  color: #475569;
}

.ca-info-row strong { color: #0f172a; }

.ca-doi-soat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.ca-ds-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.ca-ds-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.ca-ds-value {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
}

.ca-ds-highlight {
  background: #fefce8;
  border-color: #fde047;
}

.ca-ds-highlight .ca-ds-value { color: #a16207; }

.ca-hd-section { margin-top: 14px; }

.ca-hd-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 6px;
}

.ca-hd-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.ca-hd-table thead th {
  text-align: left;
  padding: 7px 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  color: #64748b;
  font-weight: 700;
  font-size: 11.5px;
}

.ca-hd-table tbody td {
  padding: 7px 10px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.ca-chenh-lech-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
}

.ca-chenh-lech-box.lech-khong {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #047857;
}

.ca-chenh-lech-box.lech-khac {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}

.ca-chenh-lech-box strong { font-size: 14px; font-weight: 800; }

.ca-hd-list {
  margin: 6px 0 0;
  padding-left: 18px;
}

.ca-hd-list li { font-weight: 700; }
</style>
