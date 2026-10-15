<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import QRCode from 'qrcode'
import http from '@/api/http'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import { formatVnd, formatCurrency, formatNumber, removeVietnameseTones } from '@/utils/format'
import { layNguoiDung, tenVaiTro } from '@/utils/session'
import SearchableSelect from '@/components/SearchableSelect.vue'
import CurrencyInput from '@/components/CurrencyInput.vue'

const API_BASE = 'http://localhost:8080/api/v1'

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

// Current staff info
const staffName = computed(() => layNguoiDung() || 'Nhân viên 01')
const staffRole = computed(() => tenVaiTro() || 'Thu ngân')

// Live Clock
const currentTimeString = ref('')
const currentDateString = ref('')
let clockTimer = null

const updateClock = () => {
  const now = new Date()
  currentTimeString.value = now.toLocaleTimeString('vi-VN', { hour12: false })
  currentDateString.value = now.toLocaleDateString('vi-VN', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

// Fullscreen mode
const isFullscreen = ref(false)
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
    isFullscreen.value = true
  } else {
    document.exitFullscreen().catch(() => {})
    isFullscreen.value = false
  }
}

// Shortcuts modal
const isShortcutsModalOpen = ref(false)

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
      shippingFee: 0
    },
    paymentMethod: 'TIEN_MAT',
    selectedVoucher: null,
    cashGiven: 0,
    transferGiven: 0,
    note: ''
  }

  pendingOrders.value.push(newOrder)
  activeOrderIndex.value = pendingOrders.value.length - 1
  notifySuccess('Hóa đơn mới', `Đã tạo "${newOrder.title}" (${newOrder.code})`, 1000)
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
      `Hóa đơn "${target.title}" đang có ${target.cart.length} sản phẩm. Toàn bộ sản phẩm sẽ được hoàn trả lại tồn kho. Bạn có chắc muốn đóng?`,
      'Đóng hóa đơn',
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

// Financial calculations
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

const addToCart = (productVariant, qtyToAdd = 1) => {
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

  if (existingItem) {
    existingItem.quantity += qtyToAdd
  } else {
    cart.push({
      idCtsp: productVariant.id,
      maSp: productVariant.maSp || `SP0${productVariant.id}`,
      maCtsp: productVariant.maCtsp || productVariant.maChiTietSanPham || `CTSP${productVariant.id}`,
      name: productVariant.tenSp || productVariant.tenSanPham || 'Sản phẩm',
      color: productVariant.color || productVariant.tenMau || '---',
      size: productVariant.size || productVariant.tenKichCo || '---',
      price: effectivePrice,
      originalPrice: Number(productVariant.price || productVariant.giaBan || 0),
      discount: productVariant.discount || null,
      quantity: qtyToAdd,
      img: productVariant.img || ''
    })
  }

  notifySuccess('Đã thêm sản phẩm', `${productVariant.tenSp || productVariant.tenSanPham || 'Sản phẩm'} (+${qtyToAdd})`, 900)
}

const updateCartItemQty = (item, newQty) => {
  if (newQty <= 0) {
    removeCartItem(item)
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

const removeCartItem = (item) => {
  if (!currentOrder.value) return
  const idx = currentOrder.value.cart.findIndex(i => i.idCtsp === item.idCtsp)
  if (idx !== -1) {
    const targetProd = allProducts.value.find(p => p.id === item.idCtsp)
    if (targetProd) {
      if (targetProd.stock !== undefined) targetProd.stock += item.quantity
      if (targetProd.soLuong !== undefined) targetProd.soLuong += item.quantity
    }
    currentOrder.value.cart.splice(idx, 1)
  }
}

const clearCurrentCart = async () => {
  if (!currentOrder.value || currentOrder.value.cart.length === 0) return
  const ok = await notifyConfirm(
    'Làm trống giỏ hàng?',
    'Toàn bộ sản phẩm sẽ được trả lại kho hàng.',
    'Làm trống',
    'Hủy'
  )
  if (!ok) return

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

const getRemainingStock = (idCtsp) => {
  const prod = allProducts.value.find(p => p.id === idCtsp)
  return prod ? (prod.stock !== undefined ? prod.stock : (prod.soLuong || 0)) : 0
}

// Fast inline barcode / CTSP scanner
const inlineScanCode = ref('')
const handleInlineScan = () => {
  const code = inlineScanCode.value.trim()
  if (!code) return

  const normCode = code.toLowerCase()
  const found = allProducts.value.find(p =>
    (p.maCtsp && p.maCtsp.toLowerCase() === normCode) ||
    (p.maChiTietSanPham && p.maChiTietSanPham.toLowerCase() === normCode) ||
    (p.maSp && p.maSp.toLowerCase() === normCode) ||
    (p.id && String(p.id) === normCode) ||
    ((p.tenSp || p.tenSanPham) && (p.tenSp || p.tenSanPham).toLowerCase() === normCode)
  )

  if (found) {
    addToCart(found, 1)
    inlineScanCode.value = ''
  } else {
    notifyWarning('Không tìm thấy', `Không tìm thấy mã sản phẩm / barcode "${code}"`)
  }
}

// Quick cash chips
const setQuickCash = (amount) => {
  if (!currentOrder.value) return
  if (amount === 'EXACT') {
    currentOrder.value.cashGiven = finalTotal.value
  } else {
    currentOrder.value.cashGiven = Number(amount)
  }
}

const addQuickCash = (amount) => {
  if (!currentOrder.value) return
  currentOrder.value.cashGiven = Number(currentOrder.value.cashGiven || 0) + Number(amount)
}

// Customer Management
const customerSearchQuery = ref('')
const showCustomerDropdown = ref(false)
const isAddCustomerModalOpen = ref(false)
const newCustomerForm = reactive({
  tenKhachHang: '',
  soDienThoai: '',
  email: '',
  diaChi: ''
})

const filteredCustomers = computed(() => {
  const q = customerSearchQuery.value.trim().toLowerCase()
  if (!q) return allCustomers.value.slice(0, 8)
  const normQ = removeVietnameseTones(q)
  return allCustomers.value.filter(c => {
    const name = removeVietnameseTones((c.ten || c.tenKhachHang || '').toLowerCase())
    const phone = (c.soDienThoai || '')
    return name.includes(normQ) || phone.includes(q)
  }).slice(0, 8)
})

const selectCustomer = (cust) => {
  if (!currentOrder.value) createNewOrder()
  currentOrder.value.customer = cust
  customerSearchQuery.value = ''
  showCustomerDropdown.value = false
  if (currentOrder.value.isDelivery && cust) {
    currentOrder.value.deliveryInfo.recipientName = cust.ten || cust.tenKhachHang || ''
    currentOrder.value.deliveryInfo.recipientPhone = cust.soDienThoai || ''
    currentOrder.value.deliveryInfo.recipientAddress = cust.diaChi || ''
  }
}

const removeSelectedCustomer = () => {
  if (currentOrder.value) {
    currentOrder.value.customer = null
    if (currentOrder.value.selectedVoucher) {
      currentOrder.value.selectedVoucher = null
      notifyWarning('Đã gỡ voucher', 'Phiếu giảm giá yêu cầu thông tin khách hàng nên đã được tự động gỡ bỏ.')
    }
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
      email: newCustomerForm.email.trim() || undefined,
      diaChiCuThe: newCustomerForm.diaChi.trim() || undefined,
      trangThai: true
    }

    const res = await http.post('/khach-hang', payload)
    const newCust = res.data
    const createdCust = {
      id: newCust.id,
      ten: newCust.tenKhachHang,
      soDienThoai: newCust.soDienThoai,
      email: newCust.email,
      diaChi: newCust.diaChiMacDinh ? newCust.diaChiMacDinh.diaChiCuThe : (newCustomerForm.diaChi.trim() || '')
    }

    allCustomers.value.unshift(createdCust)
    selectCustomer(createdCust)
    isAddCustomerModalOpen.value = false

    newCustomerForm.tenKhachHang = ''
    newCustomerForm.soDienThoai = ''
    newCustomerForm.email = ''
    newCustomerForm.diaChi = ''

    notifySuccess('Thành công', `Đã thêm khách hàng "${createdCust.ten}" thành công!`)
  } catch (err) {
    console.error('Error creating customer:', err)
    notifyError('Lỗi thêm khách hàng', err.message || 'Không thể tạo khách hàng mới trên máy chủ!')
  } finally {
    isSavingCustomer.value = false
  }
}

// Product Selector Modal
const isProductModalOpen = ref(false)
const modalFilters = reactive({
  search: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idMauSac: '',
  idKichCo: '',
  priceRange: ''
})

const modalPageSize = ref(6)
const modalCurrentPage = ref(0)
const pageSizeOptions = [6, 12, 24, 48]

const resetModalFilters = () => {
  modalFilters.search = ''
  modalFilters.idThuongHieu = ''
  modalFilters.idLoaiGiay = ''
  modalFilters.idMauSac = ''
  modalFilters.idKichCo = ''
  modalFilters.priceRange = ''
  modalCurrentPage.value = 0
}

const filteredModalVariants = computed(() => {
  let list = allProducts.value || []
  const term = modalFilters.search.trim().toLowerCase()
  if (term) {
    const norm = removeVietnameseTones(term)
    list = list.filter(p => {
      const name = removeVietnameseTones((p.tenSp || p.tenSanPham || '').toLowerCase())
      const maCtsp = (p.maCtsp || p.maChiTietSanPham || '').toLowerCase()
      const maSp = (p.maSp || '').toLowerCase()
      const color = removeVietnameseTones((p.color || p.tenMau || '').toLowerCase())
      const size = String(p.size || p.tenKichCo || '').toLowerCase()
      return name.includes(norm) || maCtsp.includes(term) || maSp.includes(term) || color.includes(norm) || size.includes(term)
    })
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

const openProductPicker = () => {
  if (!currentOrder.value) {
    createNewOrder()
  }
  resetModalFilters()
  isProductModalOpen.value = true
}

const handleSelectProductFromModal = (product) => {
  const stock = product.stock !== undefined ? product.stock : (product.soLuong || 0)
  if (stock <= 0) {
    notifyWarning('Hết hàng', 'Sản phẩm này hiện đã hết tồn kho!')
    return
  }
  addToCart(product, 1)
}

const getShoeImage = (p) => {
  if (p.img) return p.img
  return 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60'
}

// QR scanner modal
const isQrModalOpen = ref(false)
const scannedCodeInput = ref('')

const openQrScanner = () => {
  if (!currentOrder.value) createNewOrder()
  scannedCodeInput.value = ''
  isQrModalOpen.value = true
}

const handleBarcodeScan = () => {
  const code = scannedCodeInput.value.trim()
  if (!code) return

  const found = allProducts.value.find(p =>
    (p.maCtsp && p.maCtsp.toLowerCase() === code.toLowerCase()) ||
    (p.maChiTietSanPham && p.maChiTietSanPham.toLowerCase() === code.toLowerCase()) ||
    (p.id && String(p.id) === code)
  )

  if (found) {
    addToCart(found, 1)
    scannedCodeInput.value = ''
    isQrModalOpen.value = false
  } else {
    notifyError('Không tìm thấy', `Không tìm thấy sản phẩm có mã barcode/CTSP: "${code}"`)
  }
}

// Bank transfer VietQR modal
const isBankQrModalOpen = ref(false)
const bankQrDataUrl = ref('')

const generateBankTransferQr = async () => {
  if (finalTotal.value <= 0) return
  const qrString = `2|99|0987654321|POLYSHOES|${currentOrder.value.code}|0|0|${finalTotal.value}|Thanh toan don hang ${currentOrder.value.code}`
  try {
    bankQrDataUrl.value = await QRCode.toDataURL(qrString, { width: 240, margin: 1 })
    isBankQrModalOpen.value = true
  } catch (err) {
    console.error(err)
  }
}

watch(() => currentOrder.value?.paymentMethod, (newMethod) => {
  if (newMethod === 'CHUYEN_KHOAN' && finalTotal.value > 0) {
    generateBankTransferQr()
  }
})

// Voucher management
const isVoucherModalOpen = ref(false)
const voucherSearchQuery = ref('')

const openVoucherModal = () => {
  if (!currentOrder.value) createNewOrder()
  voucherSearchQuery.value = ''
  isVoucherModalOpen.value = true
}

const filteredVouchers = computed(() => {
  let list = allVouchers.value || []
  const q = voucherSearchQuery.value.trim().toLowerCase()
  if (q) {
    list = list.filter(v =>
      (v.ma && v.ma.toLowerCase().includes(q)) ||
      (v.ten && v.ten.toLowerCase().includes(q))
    )
  }
  return list
})

const isVoucherEligible = (voucher) => {
  if (!currentOrder.value) return false
  if (!currentOrder.value.customer) return false
  if (subTotal.value < (voucher.hoaDonToiThieu || 0)) return false
  if (voucher.soLuong !== undefined && voucher.soLuongDaSuDung !== undefined) {
    if (voucher.soLuongDaSuDung >= voucher.soLuong) return false
  }
  return true
}

const getVoucherIneligibilityReason = (voucher) => {
  if (!currentOrder.value || !currentOrder.value.customer) {
    return 'Cần chọn thông tin khách hàng'
  }
  if (subTotal.value < (voucher.hoaDonToiThieu || 0)) {
    const needMore = (voucher.hoaDonToiThieu || 0) - subTotal.value
    return `Mua thêm ${formatVnd(needMore)} để áp dụng`
  }
  if (voucher.soLuong !== undefined && voucher.soLuongDaSuDung !== undefined) {
    if (voucher.soLuongDaSuDung >= voucher.soLuong) return 'Hết số lượt áp dụng'
  }
  return ''
}

const applyVoucher = (voucher) => {
  if (!currentOrder.value) return
  if (!currentOrder.value.customer) {
    notifyWarning('Yêu cầu khách hàng', 'Vui lòng chọn hoặc thêm khách hàng để áp dụng phiếu giảm giá!')
    return
  }
  if (subTotal.value < (voucher.hoaDonToiThieu || 0)) {
    notifyWarning('Chưa đủ điều kiện', `Đơn hàng tối thiểu cần đạt ${formatVnd(voucher.hoaDonToiThieu)} để áp dụng phiếu này!`)
    return
  }
  currentOrder.value.selectedVoucher = voucher
  isVoucherModalOpen.value = false
  notifySuccess('Đã áp dụng voucher', `Đã áp dụng mã "${voucher.ma}" (${voucher.ten})!`)
}

const removeVoucher = () => {
  if (currentOrder.value) {
    currentOrder.value.selectedVoucher = null
    notifySuccess('Đã hủy voucher', 'Đã hủy áp dụng phiếu giảm giá cho đơn hàng!')
  }
}

// Checkout & Print Receipt
const isReceiptModalOpen = ref(false)
const completedInvoiceData = ref(null)
const isProcessingCheckout = ref(false)

const handleCancelOrder = async () => {
  if (!currentOrder.value) return
  const ok = await notifyConfirm(
    'Hủy đơn hàng này?',
    'Toàn bộ sản phẩm trong giỏ hàng hiện tại sẽ được hoàn trả lại kho hàng.',
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
    currentOrder.value.selectedVoucher = null
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

const handleCheckout = async () => {
  if (!currentOrder.value) return
  if (currentOrder.value.cart.length === 0) {
    notifyWarning('Giỏ hàng trống', 'Vui lòng chọn ít nhất 1 sản phẩm vào giỏ hàng trước khi thanh toán!')
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

  const ok = await notifyConfirm(
    'Xác nhận thanh toán',
    `Thanh toán hóa đơn "${currentOrder.value.code}" với tổng số tiền ${formatVnd(finalTotal.value)}?`,
    'Xác nhận thanh toán',
    'Kiểm tra lại'
  )
  if (!ok) return

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

    const payload = {
      maHoaDon: currentOrder.value.code,
      loaiDon: currentOrder.value.isDelivery ? 'Online' : 'Tại quầy',
      phiVanChuyen: currentOrder.value.isDelivery ? Number(currentOrder.value.deliveryInfo.shippingFee || 0) : 0,
      tenKhachHang: customerName,
      soDienThoaiKhachHang: customerPhone,
      diaChiNhanHang: currentOrder.value.isDelivery ? currentOrder.value.deliveryInfo.recipientAddress : '',
      trangThai: 'Đã hoàn thành',
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
      date: new Date().toLocaleString('vi-VN')
    }

    notifySuccess('Thanh toán thành công!', `Đã hoàn tất hóa đơn ${currentOrder.value.code}`)

    fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500&trangThai=true`)
      .then(r => r.json())
      .then(data => {
        if (data && data.content) allProducts.value = data.content
      })
      .catch(() => {})

    pendingOrders.value.splice(activeOrderIndex.value, 1)
    if (pendingOrders.value.length === 0) {
      createNewOrder()
    } else if (activeOrderIndex.value >= pendingOrders.value.length) {
      activeOrderIndex.value = Math.max(0, pendingOrders.value.length - 1)
    }

    isReceiptModalOpen.value = true

  } catch (err) {
    console.error('Checkout error:', err)
    notifyError('Lỗi thanh toán', err.message || 'Không thể lưu hóa đơn vào hệ thống!')
  } finally {
    isProcessingCheckout.value = false
  }
}

const printReceipt = () => {
  window.print()
}

// Global hotkeys listener
const handleKeyDown = (e) => {
  if (e.key === 'F1') {
    e.preventDefault()
    createNewOrder()
  } else if (e.key === 'F2') {
    e.preventDefault()
    openProductPicker()
  } else if (e.key === 'F3') {
    e.preventDefault()
    openQrScanner()
  } else if (e.key === 'F4') {
    e.preventDefault()
    const inp = document.getElementById('pos-customer-input')
    if (inp) inp.focus()
  } else if (e.key === 'F8') {
    e.preventDefault()
    handleCheckout()
  } else if (e.key === 'Escape') {
    isProductModalOpen.value = false
    isQrModalOpen.value = false
    isAddCustomerModalOpen.value = false
    isBankQrModalOpen.value = false
    isVoucherModalOpen.value = false
    isReceiptModalOpen.value = false
    isShortcutsModalOpen.value = false
  }
}

// Initial Data Loading
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

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  window.addEventListener('keydown', handleKeyDown)
  loadInitialData()
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<template>
  <div class="pos-page">
    <!-- Header Station Bar -->
    <header class="pos-top-bar">
      <div class="pos-station-info">
        <div class="pos-brand-pill">
          <span class="brand-store-name">POLYSHOES STORE</span>
          <span class="station-divider">/</span>
          <span class="station-num">Quầy POS #01</span>
        </div>
        <div class="pos-status-indicator">
          <span class="pulse-dot"></span>
          <span class="status-text">Hệ thống sẵn sàng</span>
        </div>
      </div>

      <div class="pos-header-center">
        <div class="pos-clock-box">
          <span class="clock-time">{{ currentTimeString }}</span>
          <span class="clock-date">{{ currentDateString }}</span>
        </div>
      </div>

      <div class="pos-header-actions">
        <button class="pos-hdr-btn" @click="isShortcutsModalOpen = true" title="Phím tắt hệ thống (F1-F8)">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="4" width="20" height="16" rx="2"></rect>
            <path d="M6 8h.01M10 8h.01M14 8h.01M18 8h.01M6 12h.01M10 12h.01M14 12h.01M18 12h.01M7 16h10"></path>
          </svg>
          <span class="hdr-btn-text">Phím tắt</span>
        </button>

        <button class="pos-hdr-btn" @click="toggleFullscreen" title="Bật/Tắt toàn màn hình">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
          </svg>
        </button>

        <div class="pos-staff-pill">
          <div class="staff-avatar">
            {{ (staffName || 'N')[0].toUpperCase() }}
          </div>
          <div class="staff-text">
            <span class="staff-role">{{ staffRole }}</span>
            <span class="staff-name">{{ staffName }}</span>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Workspace Grid -->
    <div class="pos-main-grid">
      <!-- Left Column: Pending Tabs & Cart -->
      <div class="pos-col-left">
        <!-- Pending Orders Card -->
        <div class="pos-card pending-orders-card">
          <div class="pending-orders-head">
            <div class="pending-left-actions">
              <button class="btn-add-order" @click="createNewOrder" :disabled="pendingOrders.length >= MAX_ORDERS">
                <span class="plus-icon">+</span>
                <span>Thêm hóa đơn</span>
                <span class="kbd-hint">F1</span>
              </button>
            </div>
            <div class="pending-counter">
              <span>Đang mở: <strong>{{ pendingOrders.length }}</strong>/{{ MAX_ORDERS }} đơn</span>
            </div>
          </div>

          <div v-if="pendingOrders.length === 0" class="empty-pending-box">
            Chưa có hóa đơn chờ nào. Bấm <strong>Thêm hóa đơn (F1)</strong> để bắt đầu bán hàng.
          </div>
          <div v-else class="pending-tabs-list">
            <div
              v-for="(order, idx) in pendingOrders"
              :key="order.id"
              class="pending-tab-item"
              :class="{ active: activeOrderIndex === idx }"
              @click="selectOrderTab(idx)"
            >
              <div class="tab-title-wrap">
                <span class="tab-title-text">{{ order.title }}</span>
                <span class="tab-code-sub">{{ order.code }}</span>
              </div>

              <div class="tab-metrics">
                <span class="tab-badge-count" v-if="order.cart.length > 0">
                  {{ order.cart.reduce((s, i) => s + i.quantity, 0) }} SP
                </span>
                <span class="tab-money-pill" v-if="order.cart.length > 0">
                  {{ formatCurrency(order.cart.reduce((s, i) => s + i.price * i.quantity, 0)) }}
                </span>
              </div>

              <button class="tab-close-btn" @click="closeOrderTab(idx, $event)" title="Đóng hóa đơn này">×</button>
            </div>
          </div>
        </div>

        <!-- Cart Card -->
        <div class="pos-card cart-card">
          <!-- Fast Scanner & Action Bar -->
          <div class="cart-action-toolbar">
            <div class="cart-fast-search-box">
              <svg class="search-lens-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                v-model="inlineScanCode"
                type="text"
                class="fast-scan-input"
                placeholder="Quét mã vạch hoặc nhập mã CTSP / tên sản phẩm rồi Enter..."
                @keyup.enter="handleInlineScan"
              />
              <button v-if="inlineScanCode" class="btn-clear-inline" @click="inlineScanCode = ''">×</button>
              <button class="btn-submit-scan" @click="handleInlineScan" title="Thêm ngay">Thêm</button>
            </div>

            <div class="cart-action-buttons">
              <button class="btn-qr-scan-action" @click="openQrScanner" title="Quét mã QR / Barcode qua máy quét (F3)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="7" height="7"></rect>
                  <rect x="14" y="3" width="7" height="7"></rect>
                  <rect x="14" y="14" width="7" height="7"></rect>
                  <rect x="3" y="14" width="7" height="7"></rect>
                  <line x1="7" y1="7" x2="7" y2="7"></line>
                  <line x1="17" y1="7" x2="17" y2="7"></line>
                  <line x1="7" y1="17" x2="7" y2="17"></line>
                  <line x1="17" y1="17" x2="17" y2="17"></line>
                </svg>
                <span>Quét mã</span>
                <span class="kbd-sub">F3</span>
              </button>

              <button class="btn-pick-product-action" @click="openProductPicker" title="Mở danh sách sản phẩm (F2)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="9" cy="21" r="1"></circle>
                  <circle cx="20" cy="21" r="1"></circle>
                  <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                </svg>
                <span>Chọn sản phẩm</span>
                <span class="kbd-sub">F2</span>
              </button>

              <button
                v-if="currentOrder && currentOrder.cart.length > 0"
                class="btn-clear-cart"
                @click="clearCurrentCart"
                title="Làm trống toàn bộ giỏ hàng"
              >
                Làm trống
              </button>
            </div>
          </div>

          <!-- Cart Table -->
          <div class="cart-table-wrapper">
            <table class="cart-table">
              <thead>
                <tr>
                  <th width="45" class="text-center">STT</th>
                  <th>Sản phẩm</th>
                  <th width="90">Phân loại</th>
                  <th width="125" class="text-center">Số lượng</th>
                  <th width="120" class="text-end">Đơn giá</th>
                  <th width="130" class="text-end">Thành tiền</th>
                  <th width="55" class="text-center">Xóa</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="currentOrder && currentOrder.cart.length > 0">
                  <tr v-for="(item, idx) in currentOrder.cart" :key="item.idCtsp" class="cart-item-row">
                    <td class="cell-stt">{{ idx + 1 }}</td>
                    <td class="cell-product-info">
                      <div class="product-item-wrap">
                        <img :src="item.img || 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=100&auto=format&fit=crop&q=60'" class="prod-thumb-mini" alt="product" />
                        <div class="prod-details">
                          <span class="prod-name-title" :title="item.name">{{ item.name }}</span>
                          <span class="prod-code-pill">{{ item.maCtsp || item.maSp }}</span>
                        </div>
                      </div>
                    </td>
                    <td>
                      <div class="variant-tags-stack">
                        <span class="badge-size-pill">Size {{ item.size }}</span>
                        <span class="badge-color-pill">{{ item.color }}</span>
                      </div>
                    </td>
                    <td class="text-center">
                      <div class="qty-stepper-modern">
                        <button class="step-btn" @click="updateCartItemQty(item, item.quantity - 1)" :disabled="item.quantity <= 1">−</button>
                        <input
                          v-model.number="item.quantity"
                          type="number"
                          class="step-input"
                          min="1"
                          @change="updateCartItemQty(item, item.quantity)"
                        />
                        <button class="step-btn" @click="updateCartItemQty(item, item.quantity + 1)" :disabled="getRemainingStock(item.idCtsp) <= 0">+</button>
                      </div>
                      <div class="stock-hint-txt">Còn lại: {{ getRemainingStock(item.idCtsp) }}</div>
                    </td>
                    <td class="text-end cell-unit-price">
                      <div class="price-stack">
                        <span class="price-active">{{ formatVnd(item.price) }}</span>
                        <span v-if="item.originalPrice > item.price" class="price-strikethrough">{{ formatVnd(item.originalPrice) }}</span>
                      </div>
                    </td>
                    <td class="text-end cell-total-price">
                      {{ formatVnd(item.price * item.quantity) }}
                    </td>
                    <td class="text-center">
                      <button class="btn-del-item" @click="removeCartItem(item)" title="Xóa khỏi giỏ hàng">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                      </button>
                    </td>
                  </tr>
                </template>
                <tr v-else>
                  <td colspan="7" class="empty-cart-state">
                    <div class="empty-cart-content">
                      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5">
                        <circle cx="9" cy="21" r="1"></circle>
                        <circle cx="20" cy="21" r="1"></circle>
                        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                      </svg>
                      <p class="empty-cart-title">Giỏ hàng hiện đang trống</p>
                      <p class="empty-cart-sub">Quét mã vạch hoặc bấm "Chọn sản phẩm (F2)" để thêm vào đơn hàng</p>
                      <button class="btn-empty-add-prod" @click="openProductPicker">
                        + Chọn sản phẩm ngay (F2)
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="currentOrder && currentOrder.cart.length > 0" class="cart-footer-summary">
            <span class="cart-total-badge">
              Tổng số lượng: <strong>{{ totalCartQty }}</strong> sản phẩm
            </span>
            <span class="cart-subtotal-badge">
              Tạm tính: <strong>{{ formatVnd(subTotal) }}</strong>
            </span>
          </div>
        </div>
      </div>

      <!-- Right Column: Customer & Checkout Panel -->
      <div class="pos-col-right">
        <!-- Customer Card -->
        <div class="pos-card customer-panel-card">
          <div class="panel-card-head">
            <div class="head-title-wrap">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <h4 class="panel-title">Khách hàng</h4>
            </div>
            <button class="btn-quick-add-cust" @click="isAddCustomerModalOpen = true" title="Thêm khách hàng mới">
              + Thêm mới
            </button>
          </div>

          <!-- Customer Search Autocomplete -->
          <div class="customer-search-area">
            <div class="customer-input-wrap">
              <input
                id="pos-customer-input"
                v-model="customerSearchQuery"
                type="text"
                class="input-cust-search"
                placeholder="Tìm khách hàng (tên, SĐT)... (F4)"
                @focus="showCustomerDropdown = true"
              />
              <button v-if="customerSearchQuery" class="btn-clear-cust" @click="customerSearchQuery = ''">×</button>
            </div>

            <!-- Dropdown suggestions -->
            <ul v-if="showCustomerDropdown && filteredCustomers.length > 0" class="customer-dropdown-menu">
              <li
                v-for="c in filteredCustomers"
                :key="c.id"
                class="cust-drop-item"
                @click="selectCustomer(c)"
              >
                <div class="cust-drop-avatar">{{ (c.ten || c.tenKhachHang || 'K')[0].toUpperCase() }}</div>
                <div class="cust-drop-info">
                  <div class="cust-drop-name">{{ c.ten || c.tenKhachHang }}</div>
                  <div class="cust-drop-sub">{{ c.soDienThoai || 'Chưa có SĐT' }}</div>
                </div>
              </li>
            </ul>
          </div>

          <!-- Selected Customer Profile Card -->
          <div v-if="currentOrder && currentOrder.customer" class="selected-customer-box">
            <div class="cust-box-left">
              <div class="cust-badge-avatar">
                {{ (currentOrder.customer.ten || currentOrder.customer.tenKhachHang || 'K')[0].toUpperCase() }}
              </div>
              <div class="cust-box-details">
                <div class="cust-box-name">{{ currentOrder.customer.ten || currentOrder.customer.tenKhachHang }}</div>
                <div class="cust-box-phone">{{ currentOrder.customer.soDienThoai }}</div>
                <div v-if="currentOrder.customer.diaChi" class="cust-box-address">{{ currentOrder.customer.diaChi }}</div>
              </div>
            </div>
            <button class="btn-unselect-cust" @click="removeSelectedCustomer" title="Chuyển về khách lẻ">
              Đổi
            </button>
          </div>
          <div v-else class="retail-guest-badge">
            <span class="retail-dot"></span>
            <span>Khách lẻ (Không lưu thông tin)</span>
          </div>
        </div>

        <!-- Checkout Card -->
        <div class="pos-card checkout-panel-card">
          <div class="panel-card-head">
            <div class="head-title-wrap">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="5" width="20" height="14" rx="2"></rect>
                <line x1="2" y1="10" x2="22" y2="10"></line>
              </svg>
              <h4 class="panel-title">Thanh toán</h4>
            </div>

            <div class="delivery-switch-wrap">
              <span class="delivery-switch-lbl">Giao hàng</span>
              <label class="toggle-switch">
                <input v-if="currentOrder" v-model="currentOrder.isDelivery" type="checkbox" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <!-- Delivery Address Form (If Delivery Active) -->
          <transition name="slide-fade">
            <div v-if="currentOrder && currentOrder.isDelivery" class="delivery-drawer-box">
              <div class="drawer-title">Thông tin giao hàng tận nơi</div>
              <input
                v-model="currentOrder.deliveryInfo.recipientName"
                type="text"
                class="del-input"
                placeholder="Tên người nhận hàng..."
              />
              <input
                v-model="currentOrder.deliveryInfo.recipientPhone"
                type="text"
                class="del-input"
                placeholder="Số điện thoại nhận hàng..."
              />
              <input
                v-model="currentOrder.deliveryInfo.recipientAddress"
                type="text"
                class="del-input"
                placeholder="Địa chỉ giao hàng chi tiết..."
              />
              <div class="shipping-fee-input-wrap">
                <label class="fee-lbl">Phí ship:</label>
                <CurrencyInput
                  v-model="currentOrder.deliveryInfo.shippingFee"
                  placeholder="0"
                />
              </div>
            </div>
          </transition>

          <!-- Voucher Row Section -->
          <div class="checkout-voucher-section">
            <div v-if="currentOrder && currentOrder.selectedVoucher" class="selected-voucher-pill">
              <div class="voucher-info-left">
                <span class="voucher-tag-code">{{ currentOrder.selectedVoucher.ma }}</span>
                <span class="voucher-discount-val">-{{ formatVnd(discountAmount) }}</span>
              </div>
              <button class="btn-remove-voucher" @click="removeVoucher" title="Hủy voucher">×</button>
            </div>

            <button
              v-else
              class="btn-open-voucher-modal"
              @click="openVoucherModal"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 10V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v5a2 2 0 0 1 0 4v5a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5a2 2 0 0 1 0-4z"></path>
              </svg>
              <span>Phiếu giảm giá / Voucher</span>
              <span class="voucher-count-avail" v-if="allVouchers.length > 0">{{ allVouchers.length }}</span>
            </button>
          </div>

          <!-- Financial Calculation Breakdown -->
          <div class="billing-summary-block">
            <div class="bill-row">
              <span class="bill-label">Tiền hàng ({{ totalCartQty }} SP):</span>
              <span class="bill-val">{{ formatVnd(subTotal) }}</span>
            </div>

            <div v-if="discountAmount > 0" class="bill-row row-discount">
              <span class="bill-label">Giảm giá voucher:</span>
              <span class="bill-val">- {{ formatVnd(discountAmount) }}</span>
            </div>

            <div v-if="currentOrder && currentOrder.isDelivery && currentOrder.deliveryInfo.shippingFee > 0" class="bill-row">
              <span class="bill-label">Phí vận chuyển:</span>
              <span class="bill-val">+ {{ formatVnd(currentOrder.deliveryInfo.shippingFee) }}</span>
            </div>

            <div class="bill-row row-grand-total">
              <span class="grand-label">KHÁCH PHẢI TRẢ:</span>
              <span class="grand-val">{{ formatVnd(finalTotal) }}</span>
            </div>
          </div>

          <!-- Payment Methods Selection -->
          <div class="payment-methods-block" v-if="currentOrder">
            <label class="pay-method-lbl">Phương thức thanh toán:</label>
            <div class="pay-method-grid">
              <label class="method-card" :class="{ active: currentOrder.paymentMethod === 'TIEN_MAT' }">
                <input v-model="currentOrder.paymentMethod" type="radio" value="TIEN_MAT" />
                <span class="method-title">Tiền mặt</span>
              </label>

              <label class="method-card" :class="{ active: currentOrder.paymentMethod === 'CHUYEN_KHOAN' }">
                <input v-model="currentOrder.paymentMethod" type="radio" value="CHUYEN_KHOAN" />
                <span class="method-title">Chuyển khoản</span>
              </label>

              <label class="method-card" :class="{ active: currentOrder.paymentMethod === 'KET_HOP' }">
                <input v-model="currentOrder.paymentMethod" type="radio" value="KET_HOP" />
                <span class="method-title">Kết hợp</span>
              </label>
            </div>
          </div>

          <!-- Cash Payment Inputs -->
          <div v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'" class="cash-settlement-box">
            <div class="cash-input-field">
              <label class="cash-label">Tiền khách đưa:</label>
              <CurrencyInput
                v-model="currentOrder.cashGiven"
                placeholder="Nhập số tiền..."
              />
            </div>

            <div class="quick-cash-row">
              <button class="quick-chip chip-exact" @click="setQuickCash('EXACT')">Đủ tiền</button>
              <button class="quick-chip" @click="setQuickCash(100000)">100k</button>
              <button class="quick-chip" @click="setQuickCash(200000)">200k</button>
              <button class="quick-chip" @click="setQuickCash(500000)">500k</button>
              <button class="quick-chip" @click="setQuickCash(1000000)">1tr</button>
              <button class="quick-chip" @click="setQuickCash(2000000)">2tr</button>
            </div>
          </div>

          <!-- Combined Transfer Input (If Combined Active) -->
          <div v-if="currentOrder && currentOrder.paymentMethod === 'KET_HOP'" class="cash-settlement-box combined-transfer">
            <div class="cash-input-field">
              <label class="cash-label">Số tiền chuyển khoản:</label>
              <CurrencyInput
                v-model="currentOrder.transferGiven"
                placeholder="Nhập tiền chuyển khoản..."
              />
            </div>
          </div>

          <!-- Change Due Box -->
          <div
            v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'"
            class="change-due-box"
            :class="{ negative: changeDue < 0, positive: changeDue >= 0 }"
          >
            <span class="change-label">{{ changeDue >= 0 ? 'Tiền thừa trả khách:' : 'Khách còn thiếu:' }}</span>
            <span class="change-value">{{ formatVnd(Math.abs(changeDue)) }}</span>
          </div>

          <!-- Bank QR Quick Trigger (If Transfer Active) -->
          <div v-if="currentOrder && currentOrder.paymentMethod === 'CHUYEN_KHOAN'" class="bank-qr-trigger-box">
            <button class="btn-show-bank-qr" @click="generateBankTransferQr">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
              </svg>
              <span>Xem mã VietQR chuyển khoản</span>
            </button>
          </div>

          <!-- Order Note -->
          <div class="order-note-wrap" v-if="currentOrder">
            <label class="note-label">Ghi chú đơn hàng:</label>
            <input
              v-model="currentOrder.note"
              type="text"
              class="input-order-note"
              placeholder="Ghi chú thêm nếu cần..."
            />
          </div>

          <!-- Final Checkout Action Buttons -->
          <div class="pos-final-actions">
            <button
              class="btn-cancel-bill"
              :disabled="!currentOrder || currentOrder.cart.length === 0"
              @click="handleCancelOrder"
              title="Hủy đơn này"
            >
              Hủy đơn
            </button>
            <button
              class="btn-checkout-primary"
              :class="{ ready: currentOrder && currentOrder.cart.length > 0 }"
              :disabled="!currentOrder || currentOrder.cart.length === 0 || isProcessingCheckout"
              @click="handleCheckout"
            >
              <span v-if="isProcessingCheckout">Đang xử lý...</span>
              <template v-else>
                <span>THANH TOÁN</span>
                <span class="btn-checkout-amount">{{ formatVnd(finalTotal) }}</span>
                <span class="kbd-btn-tag">F8</span>
              </template>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Product Picker Modal (F2) -->
    <transition name="fade">
      <div v-if="isProductModalOpen" class="pos-modal-overlay" @click.self="isProductModalOpen = false">
        <div class="pos-modal-dialog modal-xl">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="9" cy="21" r="1"></circle>
                <circle cx="20" cy="21" r="1"></circle>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
              </svg>
              <h3>Danh mục sản phẩm</h3>
              <span class="modal-badge-count">{{ modalTotalElements }} biến thể</span>
            </div>
            <button class="pos-modal-close" @click="isProductModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <!-- Modal Filters Toolbar -->
            <div class="modal-prod-filter-bar">
              <div class="modal-search-field">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input
                  v-model="modalFilters.search"
                  type="text"
                  placeholder="Tìm theo tên, mã CTSP, mã sản phẩm..."
                  class="modal-search-inp"
                />
              </div>

              <div class="modal-select-item">
                <SearchableSelect
                  v-model="modalFilters.idThuongHieu"
                  :options="brandsList"
                  label-key="ten"
                  value-key="id"
                  placeholder="Thương hiệu"
                />
              </div>

              <div class="modal-select-item">
                <SearchableSelect
                  v-model="modalFilters.idLoaiGiay"
                  :options="categoriesList"
                  label-key="ten"
                  value-key="id"
                  placeholder="Loại giày"
                />
              </div>

              <div class="modal-select-item">
                <SearchableSelect
                  v-model="modalFilters.idMauSac"
                  :options="colorsList"
                  label-key="ten"
                  value-key="id"
                  placeholder="Màu sắc"
                />
              </div>

              <div class="modal-select-item">
                <SearchableSelect
                  v-model="modalFilters.idKichCo"
                  :options="sizesList"
                  label-key="ten"
                  value-key="id"
                  placeholder="Kích cỡ"
                />
              </div>

              <div class="modal-select-item">
                <SearchableSelect
                  v-model="modalFilters.priceRange"
                  :options="[
                    { value: '<1m', label: 'Dưới 1.000.000 ₫' },
                    { value: '1m-3m', label: '1.000.000 - 3.000.000 ₫' },
                    { value: '3m-5m', label: '3.000.000 - 5.000.000 ₫' },
                    { value: '>5m', label: 'Trên 5.000.000 ₫' }
                  ]"
                  label-key="label"
                  value-key="value"
                  placeholder="Mức giá"
                />
              </div>

              <button class="btn-modal-reset-filter" @click="resetModalFilters" title="Đặt lại bộ lọc">
                Đặt lại
              </button>
            </div>

            <!-- Modal Products Table -->
            <div class="modal-table-wrap">
              <table class="modal-prod-table">
                <thead>
                  <tr>
                    <th width="45" class="text-center">STT</th>
                    <th width="70" class="text-center">Ảnh</th>
                    <th width="140">Mã CTSP</th>
                    <th>Tên sản phẩm</th>
                    <th width="100">Màu sắc</th>
                    <th width="70" class="text-center">Size</th>
                    <th width="85" class="text-center">Tồn kho</th>
                    <th width="130" class="text-end">Giá bán</th>
                    <th width="80" class="text-center">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(item, idx) in paginatedModalVariants"
                    :key="item.id"
                    class="modal-prod-row"
                    :class="{ 'out-of-stock': (item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0 }"
                    @click="handleSelectProductFromModal(item)"
                  >
                    <td class="text-center text-slate">{{ modalCurrentPage * modalPageSize + idx + 1 }}</td>
                    <td class="text-center">
                      <img :src="getShoeImage(item)" class="modal-thumb-img" alt="shoe" />
                    </td>
                    <td>
                      <span class="code-pill-modal">{{ item.maCtsp || item.maChiTietSanPham || item.maSp }}</span>
                    </td>
                    <td class="modal-prod-name">
                      <span class="prod-text-title">{{ item.tenSp || item.tenSanPham }}</span>
                    </td>
                    <td>
                      <span class="badge-color-tag">{{ item.color || item.tenMau || '---' }}</span>
                    </td>
                    <td class="text-center">
                      <span class="badge-size-tag">{{ item.size || item.tenKichCo || '---' }}</span>
                    </td>
                    <td class="text-center">
                      <span
                        class="stock-badge"
                        :class="{
                          'in-stock': (item.stock !== undefined ? item.stock : (item.soLuong || 0)) > 10,
                          'low-stock': (item.stock !== undefined ? item.stock : (item.soLuong || 0)) > 0 && (item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 10,
                          'zero-stock': (item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0
                        }"
                      >
                        {{ item.stock !== undefined ? item.stock : (item.soLuong || 0) }}
                      </span>
                    </td>
                    <td class="text-end">
                      <div class="prod-modal-price-box">
                        <span class="price-now">{{ formatVnd(getEffectivePrice(item)) }}</span>
                        <span v-if="item.discount" class="price-was">{{ formatVnd(item.price || item.giaBan) }}</span>
                      </div>
                    </td>
                    <td class="text-center" @click.stop>
                      <button
                        class="btn-select-add-cart"
                        :disabled="(item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0"
                        @click="handleSelectProductFromModal(item)"
                      >
                        Thêm
                      </button>
                    </td>
                  </tr>
                  <tr v-if="paginatedModalVariants.length === 0">
                    <td colspan="9" class="empty-state-modal">
                      Không tìm thấy sản phẩm nào phù hợp với bộ lọc hiện tại.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Modal Pagination -->
          <div class="pos-modal-footer">
            <div class="page-size-selector">
              <label>Hiển thị:</label>
              <select v-model.number="modalPageSize">
                <option v-for="sz in pageSizeOptions" :key="sz" :value="sz">{{ sz }} mục / trang</option>
              </select>
            </div>

            <div class="modal-pager-controls">
              <button class="pager-btn" :disabled="modalCurrentPage === 0" @click="changeModalPage(0)">«</button>
              <button class="pager-btn" :disabled="modalCurrentPage === 0" @click="changeModalPage(modalCurrentPage - 1)">‹</button>
              <button
                v-for="p in Math.min(modalTotalPages, 8)"
                :key="p"
                class="pager-btn page-num"
                :class="{ active: modalCurrentPage === p - 1 }"
                @click="changeModalPage(p - 1)"
              >
                {{ p }}
              </button>
              <button class="pager-btn" :disabled="modalCurrentPage >= modalTotalPages - 1" @click="changeModalPage(modalCurrentPage + 1)">›</button>
              <button class="pager-btn" :disabled="modalCurrentPage >= modalTotalPages - 1" @click="changeModalPage(modalTotalPages - 1)">»</button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Voucher Selector Modal -->
    <transition name="fade">
      <div v-if="isVoucherModalOpen" class="pos-modal-overlay" @click.self="isVoucherModalOpen = false">
        <div class="pos-modal-dialog modal-md">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 10V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v5a2 2 0 0 1 0 4v5a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5a2 2 0 0 1 0-4z"></path>
              </svg>
              <h3>Phiếu giảm giá khả dụng</h3>
            </div>
            <button class="pos-modal-close" @click="isVoucherModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="voucher-search-bar">
              <input
                v-model="voucherSearchQuery"
                type="text"
                placeholder="Nhập mã phiếu giảm giá để tìm kiếm..."
                class="voucher-search-inp"
              />
            </div>

            <div v-if="!currentOrder?.customer" class="voucher-customer-alert">
              <span>Đang là khách lẻ. Vui lòng chọn hoặc thêm khách hàng để áp dụng phiếu giảm giá.</span>
            </div>

            <div class="voucher-cards-list">
              <div
                v-for="v in filteredVouchers"
                :key="v.id"
                class="voucher-card-item"
                :class="{
                  eligible: isVoucherEligible(v),
                  selected: currentOrder?.selectedVoucher?.id === v.id
                }"
              >
                <div class="voucher-card-left">
                  <div class="voucher-discount-highlight">
                    {{ v.loaiGiamGia === 'PERCENT' || v.loaiGiamGia === '%' ? `${v.giaTriGiam}%` : formatCurrency(v.giaTriGiam) }}
                  </div>
                  <div class="voucher-type-sub">GIẢM GIÁ</div>
                </div>

                <div class="voucher-card-body">
                  <div class="voucher-card-code">{{ v.ma }}</div>
                  <div class="voucher-card-name">{{ v.ten }}</div>
                  <div class="voucher-card-conditions">
                    <span>Đơn tối thiểu: <strong>{{ formatVnd(v.hoaDonToiThieu || 0) }}</strong></span>
                    <span v-if="v.giamToiDa"> • Tối đa: <strong>{{ formatVnd(v.giamToiDa) }}</strong></span>
                  </div>
                  <div v-if="!isVoucherEligible(v)" class="voucher-ineligible-msg">
                    {{ getVoucherIneligibilityReason(v) }}
                  </div>
                </div>

                <div class="voucher-card-action">
                  <button
                    v-if="currentOrder?.selectedVoucher?.id === v.id"
                    class="btn-voucher-applied"
                    disabled
                  >
                    Đang dùng
                  </button>
                  <button
                    v-else
                    class="btn-apply-voucher"
                    :disabled="!isVoucherEligible(v)"
                    @click="applyVoucher(v)"
                  >
                    Áp dụng
                  </button>
                </div>
              </div>

              <div v-if="filteredVouchers.length === 0" class="empty-vouchers-msg">
                Không có phiếu giảm giá nào phù hợp.
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Barcode / QR Scanner Modal (F3) -->
    <transition name="fade">
      <div v-if="isQrModalOpen" class="pos-modal-overlay" @click.self="isQrModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Quét mã vạch / Barcode</h3>
            <button class="pos-modal-close" @click="isQrModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="qr-modal-content">
              <div class="qr-scanner-visual-box">
                <div class="qr-laser-line"></div>
                <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="#d92d20" stroke-width="1.5">
                  <rect x="3" y="3" width="7" height="7"></rect>
                  <rect x="14" y="3" width="7" height="7"></rect>
                  <rect x="14" y="14" width="7" height="7"></rect>
                  <rect x="3" y="14" width="7" height="7"></rect>
                </svg>
              </div>
              <p class="qr-guide-text">
                Hướng máy quét vào tem mã vạch hoặc nhập mã CTSP trực tiếp bên dưới:
              </p>

              <div class="qr-input-row">
                <input
                  v-model="scannedCodeInput"
                  type="text"
                  class="qr-code-inp"
                  placeholder="Nhập mã CTSP hoặc barcode..."
                  autofocus
                  @keyup.enter="handleBarcodeScan"
                />
                <button class="btn-qr-find" @click="handleBarcodeScan">Thêm</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Quick Add Customer Modal -->
    <transition name="fade">
      <div v-if="isAddCustomerModalOpen" class="pos-modal-overlay" @click.self="isAddCustomerModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Thêm nhanh khách hàng</h3>
            <button class="pos-modal-close" @click="isAddCustomerModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="modal-form-vertical">
              <div class="form-field-item">
                <label>Họ và tên <span class="required-star">*</span></label>
                <input v-model="newCustomerForm.tenKhachHang" type="text" placeholder="Nhập họ tên khách hàng..." />
              </div>
              <div class="form-field-item">
                <label>Số điện thoại <span class="required-star">*</span></label>
                <input v-model="newCustomerForm.soDienThoai" type="text" placeholder="Nhập số điện thoại (10 số)..." />
              </div>
              <div class="form-field-item">
                <label>Email</label>
                <input v-model="newCustomerForm.email" type="email" placeholder="Nhập địa chỉ email..." />
              </div>
              <div class="form-field-item">
                <label>Địa chỉ</label>
                <textarea v-model="newCustomerForm.diaChi" rows="2" placeholder="Nhập địa chỉ cụ thể..."></textarea>
              </div>
            </div>
          </div>

          <div class="pos-modal-footer">
            <button class="btn-modal-cancel" @click="isAddCustomerModalOpen = false">Hủy</button>
            <button class="btn-modal-submit" :disabled="isSavingCustomer" @click="saveQuickCustomer">
              {{ isSavingCustomer ? 'Đang lưu...' : 'Lưu khách hàng' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Bank VietQR Modal -->
    <transition name="fade">
      <div v-if="isBankQrModalOpen" class="pos-modal-overlay" @click.self="isBankQrModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Mã QR Chuyển khoản VietQR</h3>
            <button class="pos-modal-close" @click="isBankQrModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body text-center">
            <div class="vietqr-wrap">
              <img v-if="bankQrDataUrl" :src="bankQrDataUrl" class="vietqr-img" alt="VietQR" />
              <div class="vietqr-amount">{{ formatVnd(finalTotal) }}</div>
              <div class="vietqr-memo">
                Nội dung: <strong>Thanh toan don hang {{ currentOrder?.code }}</strong>
              </div>
            </div>
          </div>

          <div class="pos-modal-footer">
            <button class="btn-modal-submit" @click="isBankQrModalOpen = false">Đã nhận tiền thành công</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Printable Thermal Receipt Modal -->
    <transition name="fade">
      <div v-if="isReceiptModalOpen && completedInvoiceData" class="pos-modal-overlay" @click.self="isReceiptModalOpen = false">
        <div class="pos-modal-dialog modal-sm printable-dialog">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Hóa đơn thanh toán</h3>
            <button class="pos-modal-close" @click="isReceiptModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="thermal-receipt-box">
              <div class="receipt-header-center">
                <h2 class="receipt-brand-title">POLYSHOES STORE</h2>
                <p class="receipt-sub-line">Đ/C: Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</p>
                <p class="receipt-sub-line">Hotline: 0987.654.321</p>
              </div>

              <div class="receipt-meta-info">
                <div>Mã HĐ: <strong>{{ completedInvoiceData.code }}</strong></div>
                <div>Thời gian: {{ completedInvoiceData.date }}</div>
                <div>Khách hàng: <strong>{{ completedInvoiceData.customerName }}</strong></div>
                <div v-if="completedInvoiceData.customerPhone">SĐT: {{ completedInvoiceData.customerPhone }}</div>
              </div>

              <table class="receipt-items-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th class="text-center">SL</th>
                    <th class="text-end">T.Tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in completedInvoiceData.cart" :key="item.idCtsp">
                    <td>{{ item.name }} ({{ item.color }}, {{ item.size }})</td>
                    <td class="text-center">{{ item.quantity }}</td>
                    <td class="text-end">{{ formatVnd(item.price * item.quantity) }}</td>
                  </tr>
                </tbody>
              </table>

              <div class="receipt-totals-block">
                <div class="receipt-sum-row">
                  <span>Tổng tiền hàng:</span>
                  <strong>{{ formatVnd(completedInvoiceData.subTotal) }}</strong>
                </div>
                <div v-if="completedInvoiceData.discount > 0" class="receipt-sum-row">
                  <span>Giảm giá:</span>
                  <strong>- {{ formatVnd(completedInvoiceData.discount) }}</strong>
                </div>
                <div v-if="completedInvoiceData.shippingFee > 0" class="receipt-sum-row">
                  <span>Phí vận chuyển:</span>
                  <strong>+ {{ formatVnd(completedInvoiceData.shippingFee) }}</strong>
                </div>
                <div class="receipt-sum-row grand-sum">
                  <span>TỔNG THANH TOÁN:</span>
                  <span>{{ formatVnd(completedInvoiceData.finalTotal) }}</span>
                </div>
                <div v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT'" class="receipt-sum-row">
                  <span>Tiền khách đưa:</span>
                  <span>{{ formatVnd(completedInvoiceData.cashGiven) }}</span>
                </div>
                <div v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT'" class="receipt-sum-row">
                  <span>Tiền thừa:</span>
                  <span>{{ formatVnd(completedInvoiceData.changeDue) }}</span>
                </div>
              </div>

              <div class="receipt-thank-msg">
                Cảm ơn quý khách và hẹn gặp lại!
              </div>
            </div>
          </div>

          <div class="pos-modal-footer">
            <button class="btn-modal-cancel" @click="isReceiptModalOpen = false">Đóng</button>
            <button class="btn-modal-submit" @click="printReceipt">
              In hóa đơn
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Keyboard Shortcuts Help Modal -->
    <transition name="fade">
      <div v-if="isShortcutsModalOpen" class="pos-modal-overlay" @click.self="isShortcutsModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Phím tắt thao tác nhanh (POS)</h3>
            <button class="pos-modal-close" @click="isShortcutsModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="shortcuts-list">
              <div class="shortcut-item">
                <span class="kbd-key">F1</span>
                <span class="kbd-desc">Tạo hóa đơn chờ mới</span>
              </div>
              <div class="shortcut-item">
                <span class="kbd-key">F2</span>
                <span class="kbd-desc">Mở danh mục chọn sản phẩm</span>
              </div>
              <div class="shortcut-item">
                <span class="kbd-key">F3</span>
                <span class="kbd-desc">Mở quét mã vạch / Barcode</span>
              </div>
              <div class="shortcut-item">
                <span class="kbd-key">F4</span>
                <span class="kbd-desc">Focus ô tìm kiếm khách hàng</span>
              </div>
              <div class="shortcut-item">
                <span class="kbd-key">F8</span>
                <span class="kbd-desc">Xác nhận thanh toán đơn hàng</span>
              </div>
              <div class="shortcut-item">
                <span class="kbd-key">Enter</span>
                <span class="kbd-desc">Thêm nhanh sản phẩm trên ô quét mã</span>
              </div>
              <div class="shortcut-item">
                <span class="kbd-key">Esc</span>
                <span class="kbd-desc">Đóng các cửa sổ popup đang mở</span>
              </div>
            </div>
          </div>

          <div class="pos-modal-footer">
            <button class="btn-modal-submit" @click="isShortcutsModalOpen = false">Đã hiểu</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped src="@/assets/styles/PosSalesPage.css"></style>
