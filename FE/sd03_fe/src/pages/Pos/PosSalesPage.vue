<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import QRCode from 'qrcode'
import { Html5Qrcode } from 'html5-qrcode'
import http, { API_ORIGIN } from '@/api/http'
import { getProvinces, getWards, getServices, tinhPhi } from '@/api/shipping'
import { DS_63_TINH_THANH } from '@/api/diaGioi'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import { formatVnd } from '@/utils/format'

import { getUser } from '@/utils/auth'
import { tenVaiTro } from '@/utils/session'

const API_BASE = `${API_ORIGIN}/api/v1`

const staffUser = computed(() => getUser())
const staffName = computed(() => staffUser.value?.tenKhachHang || staffUser.value?.tenTaiKhoan || 'Nhân viên')
const staffRole = computed(() => tenVaiTro())

const clockTimer = ref(null)

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen().catch(() => {})
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

  notifySuccess('Đã thêm vào giỏ hàng', `${productVariant.tenSp || productVariant.tenSanPham || 'Sản phẩm'} (+${qtyToAdd})`, 1000)
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
    } catch {}
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
    if (currentOrder.value.selectedVoucher) {
      currentOrder.value.selectedVoucher = null
      notifyWarning('Đã gỡ phiếu giảm giá', 'Phiếu giảm giá chỉ áp dụng cho khách hàng đã định danh.')
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
    addToCart(p, 1)
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
    notifySuccess('Thêm vào giỏ hàng', `Đã thêm ${added} dòng sản phẩm vào giỏ hàng.`)
  }
}

const getShoeImage = (p) => {
  if (p.img) return p.img
  return 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60'
}

// Camera QR / Barcode Scanner Modal (F3)
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
  } catch {}
}

const findProductByScannedCode = (rawCode) => {
  if (!rawCode) return null
  let code = String(rawCode).trim()

  // 1. Trường hợp quét mã QR biến thể dạng JSON { maCtsp, maSp, id, ... }
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
    } catch {}
  }

  // 2. Trường hợp mã có tiền tố: ví dụ "CTSP:SP035-MS09-42"
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

  // 3. Quét mã CTSP, Barcode hoặc ID thông thường
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

  // Nếu trong bộ nhớ chưa có, tra cứu trực tiếp từ backend
  if (!found) {
    try {
      const trimmed = String(decodedText).trim()
      let searchKey = trimmed
      if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
        try {
          const parsed = JSON.parse(trimmed)
          searchKey = parsed.maCtsp || parsed.ma || parsed.id || trimmed
        } catch {}
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

    // Đợi 0.6s để người dùng thấy visual quét thành công rồi đóng modal
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
      () => {}
    )
  } catch (err) {
    console.warn('Lỗi bật Camera POS:', err)
    isCameraRunning.value = false
    cameraError.value = 'Không thể mở Camera (chưa cấp quyền truy cập trình duyệt hoặc máy không có Camera).'
  }
}

const stopCameraScanner = async () => {
  // Giải phóng MediaStream tracks
  try {
    const videoEls = document.querySelectorAll('#pos-camera-scanner-view video, video')
    videoEls.forEach((v) => {
      if (v.srcObject && typeof v.srcObject.getTracks === 'function') {
        v.srcObject.getTracks().forEach((track) => {
          try {
            track.stop()
            track.enabled = false
          } catch {}
        })
        v.srcObject = null
      }
      try {
        v.pause()
        v.removeAttribute('src')
        v.load()
      } catch {}
    })
  } catch (err) {
    console.warn('Lỗi giải phóng media tracks camera:', err)
  }

  // Tắt scanner html5QrScanner an toàn
  if (html5QrScanner) {
    try {
      if (html5QrScanner.isScanning) {
        await Promise.race([
          html5QrScanner.stop().catch(() => {}),
          new Promise((resolve) => setTimeout(resolve, 400)),
        ])
      }
    } catch (e) {
      console.warn('html5QrScanner.stop error:', e)
    }

    try {
      html5QrScanner.clear()
    } catch {}
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

  // Fallback 1: Tra cứu trực tiếp từ Open API v1 mới nhất (chuẩn hành chính 2026)
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

  // Fallback 2: Danh sách 63 tỉnh thành Việt Nam trong bộ nhớ
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
      // Tiếp tục fallback bên dưới
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
      // fallback
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

const isVoucherEligible = (v) => {
  if (!currentOrder.value) return false
  if (!currentOrder.value.customer) return false
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

const getVoucherIneligibilityReason = (v) => {
  if (!currentOrder.value?.customer) {
    return 'Cần chọn hoặc thêm khách hàng để dùng voucher'
  }
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
  if (!currentOrder.value.customer) {
    notifyWarning('Yêu cầu khách hàng', 'Vui lòng chọn hoặc thêm khách hàng để áp dụng phiếu giảm giá!')
    return
  }
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
      date: new Date().toLocaleString('vi-VN')
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
    notifyError('Lỗi thanh toán', err.message || 'Không thể lưu hóa đơn vào hệ thống!')
  } finally {
    isProcessingCheckout.value = false
  }
}

const printReceipt = () => {
  window.print()
}

// Keyboard listener (chỉ giữ Escape để đóng modal popup khi cần)
const handleKeyDown = (e) => {
  if (e.key === 'Escape') {
    isProductModalOpen.value = false
    isQrModalOpen.value = false
    isAddCustomerModalOpen.value = false
    isBankQrModalOpen.value = false
    isVoucherModalOpen.value = false
    isReceiptModalOpen.value = false
    stopCameraScanner()
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

const handleWindowClick = (e) => {
  if (!e.target.closest('.customer-search-area')) {
    showCustomerDropdown.value = false
  }
}

onMounted(() => {
  loadInitialData()
  loadProvinces()
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
    <!-- Main Workspace Grid -->
    <div class="pos-main-grid">
      <!-- Left Column: Unified POS Workspace (Tabs + Toolbar + Cart Table + Summary) -->
      <div class="pos-col-left">
        <div class="pos-workspace-panel">
          <!-- Horizontal Orders Tabs Bar -->
          <div class="pos-orders-tabs-bar">
            <div class="pos-tabs-scroll-area">
              <button
                class="btn-create-order-tab"
                @click="createNewOrder"
                :disabled="pendingOrders.length >= MAX_ORDERS"
                title="Tạo hóa đơn chờ mới (Tối đa 5 hóa đơn)"
              >
                <span>+ Thêm hóa đơn</span>
              </button>

              <div
                v-for="(order, idx) in pendingOrders"
                :key="order.id"
                class="pos-order-tab"
                :class="{ active: activeOrderIndex === idx }"
                @click="selectOrderTab(idx)"
              >
                <span class="tab-order-indicator"></span>
                <span class="tab-order-name">{{ order.title }}</span>
                <span class="tab-cart-qty" v-if="order.cart.length > 0">{{ order.cart.length }}</span>
                <button
                  class="tab-close-icon"
                  @click="closeOrderTab(idx, $event)"
                  title="Đóng hóa đơn này"
                >
                  ×
                </button>
              </div>
            </div>

            <div class="pos-orders-counter-pill">
              <span class="counter-num">{{ pendingOrders.length }}/{{ MAX_ORDERS }}</span>
              <span class="counter-lbl">hóa đơn</span>
            </div>
          </div>

          <!-- Main Cart Workspace Container -->
          <div class="pos-cart-main-container">
            <!-- Action & Fast Search Toolbar -->
            <div class="cart-unified-toolbar">
              <div class="fast-search-container">
                <input
                  type="text"
                  class="fast-search-input"
                  placeholder="Quét mã barcode hoặc nhập mã CTSP / tên sản phẩm..."
                  v-model="barcodeSearchText"
                  @keydown.enter="handleBarcodeSearchSubmit"
                />
                <button
                  v-if="barcodeSearchText"
                  class="btn-clear-fast-search"
                  @click="barcodeSearchText = ''"
                  title="Xóa tìm kiếm"
                >
                  ×
                </button>
                <button class="btn-fast-search-add" @click="handleBarcodeSearchSubmit" title="Thêm vào giỏ">
                  <span>Thêm</span>
                </button>
              </div>

              <div class="cart-right-tools">
                <button class="btn-tool-action btn-tool-camera" @click="openQrScanner" title="Quét mã sản phẩm qua Camera">
                  <span>Quét Camera</span>
                </button>

                <button class="btn-tool-action btn-tool-product" @click="openProductPicker" title="Mở danh mục chọn sản phẩm">
                  <span>Chọn sản phẩm</span>
                </button>

                <button
                  v-if="currentOrder && currentOrder.cart.length > 0"
                  class="btn-tool-action btn-tool-clear"
                  @click="clearCurrentCart"
                  title="Làm trống toàn bộ giỏ hàng"
                >
                  <span>Làm trống</span>
                </button>
              </div>
            </div>

            <!-- Cart Table (when items exist) -->
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
                      <th width="56" class="text-center">XÓA</th>
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
                            <span class="prod-code-pill">{{ item.maCtsp || item.maSp }}</span>
                          </div>
                        </div>
                      </td>
                      <td class="text-center">
                        <div class="variant-chips-wrap">
                          <span class="badge-size-pill">Size {{ item.size }}</span>
                          <span class="badge-color-pill">{{ item.color }}</span>
                        </div>
                      </td>
                      <td class="text-center">
                        <div class="qty-stepper-modern">
                          <button class="step-btn" @click="updateCartItemQty(item, item.quantity - 1)" :disabled="item.quantity <= 1">−</button>
                          <input type="number" class="step-input" v-model.number="item.quantity" @change="updateCartItemQty(item, item.quantity)" min="1" />
                          <button class="step-btn" @click="updateCartItemQty(item, item.quantity + 1)" :disabled="getRemainingStock(item.idCtsp) <= 0">+</button>
                        </div>
                        <div class="stock-hint-txt" v-if="getRemainingStock(item.idCtsp) < 10">
                          Còn lại: {{ getRemainingStock(item.idCtsp) }}
                        </div>
                      </td>
                      <td class="text-end">
                        <div class="price-stack">
                          <span class="price-active">{{ formatVnd(item.price) }}</span>
                          <span v-if="item.originalPrice && item.originalPrice > item.price" class="price-strikethrough">
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

              <!-- Cart Footer Status Bar -->
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

            <!-- Empty State (when cart has no items) -->
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

      <!-- Right Column: Customer & Checkout Panel -->
      <div class="pos-col-right">
        <!-- Customer Card -->
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
              <input
                type="text"
                class="input-cust-search"
                placeholder="Tìm theo tên hoặc SĐT khách hàng..."
                v-model="customerSearchQuery"
                @focus="showCustomerDropdown = true"
              />
              <button
                v-if="customerSearchQuery"
                class="btn-clear-cust"
                @click="customerSearchQuery = ''"
              >
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

          <!-- Selected Customer Box or Retail Guest -->
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

        <!-- Order Checkout Info Card -->
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

          <!-- Delivery Drawer Box -->
          <div v-if="currentOrder && currentOrder.isDelivery" class="delivery-drawer-box">
            <div class="drawer-title">Thông tin nhận hàng</div>
            <input type="text" class="del-input" placeholder="Tên người nhận hàng..." v-model="currentOrder.deliveryInfo.recipientName" />
            <input type="text" class="del-input" placeholder="Số điện thoại người nhận..." v-model="currentOrder.deliveryInfo.recipientPhone" />
            <select class="del-input" v-model="currentOrder.deliveryInfo.province" @change="onProvinceChange">
              <option :value="null" disabled>Chọn Tỉnh / Thành phố...</option>
              <option v-for="p in provincesList" :key="p.id" :value="p">{{ p.name }}</option>
            </select>
            <select class="del-input" v-model="currentOrder.deliveryInfo.ward" @change="onWardChange" :disabled="!currentOrder.deliveryInfo.province || currentOrder.deliveryInfo.wardsLoading">
              <option :value="null" disabled>{{ currentOrder.deliveryInfo.wardsLoading ? 'Đang tải phường/xã...' : 'Chọn Phường / Xã...' }}</option>
              <option v-for="w in wardsList" :key="w.id" :value="w">{{ w.name }}</option>
            </select>
            <input type="text" class="del-input" placeholder="Số nhà, tên đường chi tiết..." v-model="currentOrder.deliveryInfo.recipientAddress" />
            <select v-if="currentOrder.deliveryInfo.services && currentOrder.deliveryInfo.services.length" class="del-input" v-model="currentOrder.deliveryInfo.serviceTypeId" @change="onServiceChange">
              <option v-for="s in currentOrder.deliveryInfo.services" :key="s.serviceTypeId" :value="s.serviceTypeId">{{ s.shortName || ('Dịch vụ ' + s.serviceTypeId) }}</option>
            </select>
            <div v-if="currentOrder.deliveryInfo.feeLoading" class="fee-info">
              Đang tính phí vận chuyển với GHN...
            </div>
            <div v-else-if="currentOrder.deliveryInfo.feeError" class="fee-error">
              <span>{{ currentOrder.deliveryInfo.feeError }}</span>
              <button type="button" class="fee-retry" @click="tinhPhiVanChuyen">Thử lại</button>
            </div>
            <div v-else-if="currentOrder.deliveryInfo.shippingFee > 0" class="fee-info">
              Phí vận chuyển (GHN): <strong style="color: #d92d20;">{{ formatVnd(currentOrder.deliveryInfo.shippingFee) }}</strong>
            </div>
          </div>

          <!-- Voucher Section (Nhập mã trực tiếp & Mở modal danh sách) -->
          <div class="checkout-voucher-section" v-if="currentOrder">
            <div class="voucher-section-header">
              <span class="voucher-section-title">Phiếu giảm giá / Voucher</span>
              <button
                v-if="!currentOrder.selectedVoucher"
                type="button"
                class="btn-browse-vouchers"
                @click="openVoucherModal"
              >
                Danh sách
                <span class="voucher-badge-num" v-if="allVouchers.length > 0">({{ allVouchers.length }})</span>
              </button>
            </div>

            <!-- Khi đã áp dụng voucher -->
            <div v-if="currentOrder.selectedVoucher" class="selected-voucher-card">
              <div class="selected-voucher-content">
                <div class="selected-voucher-top">
                  <span class="selected-voucher-code">{{ currentOrder.selectedVoucher.ma || currentOrder.selectedVoucher.maPhieuGiamGia }}</span>
                  <span class="selected-voucher-val">- {{ formatVnd(discountAmount) }}</span>
                </div>
                <div class="selected-voucher-desc">
                  {{ currentOrder.selectedVoucher.ten }}
                  <span v-if="currentOrder.selectedVoucher.hoaDonToiThieu"> • Đơn từ {{ formatVnd(currentOrder.selectedVoucher.hoaDonToiThieu) }}</span>
                </div>
              </div>
              <button class="btn-remove-voucher-pill" @click="removeVoucher" title="Gỡ bỏ mã giảm giá">
                Gỡ
              </button>
            </div>

            <!-- Khi chưa áp dụng voucher: Ô nhập mã trực tiếp -->
            <div v-else class="voucher-input-group">
              <input
                type="text"
                v-model="voucherInputCode"
                class="voucher-quick-input"
                placeholder="Nhập mã voucher (VD: GG005)..."
                @keyup.enter="handleApplyManualVoucher"
              />
              <button
                type="button"
                class="btn-apply-manual-voucher"
                :disabled="!voucherInputCode.trim()"
                @click="handleApplyManualVoucher"
              >
                Áp dụng
              </button>
            </div>
          </div>

          <!-- Billing Summary Block -->
          <div class="billing-summary-block">
            <div class="bill-row">
              <span class="bill-label">Tổng tiền hàng ({{ totalCartQty }} sản phẩm):</span>
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
          </div>

          <!-- Grand Total Hero Card -->
          <div class="grand-total-hero-card">
            <div class="hero-left">
              <span class="hero-lbl">KHÁCH PHẢI TRẢ</span>
              <span class="hero-sub">{{ totalCartQty }} sản phẩm</span>
            </div>
            <div class="hero-amount">
              {{ formatVnd(finalTotal) }}
            </div>
          </div>

          <!-- Payment Methods -->
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

          <!-- Cash Input & Quick Chips -->
          <div class="cash-settlement-box" v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'">
            <div class="cash-input-field">
              <span class="cash-label">Khách thanh toán tiền mặt</span>
              <div class="cash-input-wrap">
                <input type="number" class="del-input cash-amount-inp" placeholder="Nhập số tiền khách đưa..." v-model.number="currentOrder.cashGiven" />
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

          <!-- Combined Mode: Additional Transfer Amount -->
          <div class="cash-settlement-box" v-if="currentOrder && currentOrder.paymentMethod === 'KET_HOP'">
            <div class="cash-input-field">
              <span class="cash-label">Số tiền chuyển khoản</span>
              <div class="cash-input-wrap">
                <input type="number" class="del-input cash-amount-inp" placeholder="Nhập số tiền chuyển khoản..." v-model.number="currentOrder.transferGiven" />
                <span class="cash-currency-badge">₫</span>
              </div>
            </div>
          </div>

          <!-- Change Due Box -->
          <div class="change-due-box" :class="changeDue >= 0 ? 'positive' : 'negative'" v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'">
            <span class="change-label">{{ changeDue >= 0 ? 'Tiền thừa trả khách:' : 'Khách còn thiếu:' }}</span>
            <span class="change-value">{{ changeDue >= 0 ? formatVnd(changeDue) : formatVnd(Math.abs(changeDue)) }}</span>
          </div>

          <!-- Bank QR Button -->
          <div class="bank-qr-trigger-box" v-if="currentOrder && (currentOrder.paymentMethod === 'CHUYEN_KHOAN' || currentOrder.paymentMethod === 'KET_HOP')">
            <button class="btn-show-bank-qr" @click="generateBankTransferQr">
              <span>Tạo mã QR chuyển khoản</span>
            </button>
          </div>

          <!-- Order Note -->
          <div class="order-note-wrap" v-if="currentOrder">
            <span class="note-label">Ghi chú đơn hàng</span>
            <input type="text" class="input-order-note" placeholder="Ghi chú thêm nếu cần..." v-model="currentOrder.note" />
          </div>

          <!-- Action Buttons -->
          <div class="pos-final-actions">
            <button class="btn-cancel-bill" @click="handleCancelOrder" :disabled="!currentOrder || currentOrder.cart.length === 0">
              Hủy đơn
            </button>
            <button
              class="btn-checkout-primary"
              :class="{ ready: currentOrder && currentOrder.cart.length > 0 && !isProcessingCheckout }"
              @click="handleCheckout"
              :disabled="!currentOrder || currentOrder.cart.length === 0 || isProcessingCheckout"
            >
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
            <!-- Modal Filters Toolbar (Hàng tìm kiếm + Lưới 5 ô Select cân đối, không cụt chữ) -->
            <div class="modal-prod-filter-bar">
              <div class="modal-filter-top-row">
                <div class="modal-search-field">
                  <input
                    v-model="modalFilters.search"
                    type="text"
                    placeholder="Tìm theo tên, mã CTSP, mã sản phẩm..."
                    class="modal-search-inp"
                  />
                  <button v-if="modalFilters.search" class="modal-clear-search-btn" @click="modalFilters.search = ''">×</button>
                </div>
                <button class="btn-modal-reset-filter" @click="resetModalFilters" title="Đặt lại toàn bộ 5 bộ lọc">
                  Đặt lại bộ lọc
                </button>
              </div>

              <!-- Lưới 5 ô Select bộ lọc -->
              <div class="modal-5-selects-grid">
                <div class="filter-select-wrap">
                  <select v-model="modalFilters.idThuongHieu" class="filter-select" :class="{ active: !!modalFilters.idThuongHieu }">
                    <option value="">Tất cả thương hiệu</option>
                    <option v-for="b in brandsList" :key="b.id" :value="b.id || b.ten">{{ b.ten || b.tenThuongHieu }}</option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.idLoaiGiay" class="filter-select" :class="{ active: !!modalFilters.idLoaiGiay }">
                    <option value="">Tất cả thể loại</option>
                    <option v-for="c in categoriesList" :key="c.id" :value="c.id || c.ten">{{ c.ten || c.tenLoaiGiay }}</option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.idMauSac" class="filter-select" :class="{ active: !!modalFilters.idMauSac }">
                    <option value="">Tất cả màu sắc</option>
                    <option v-for="m in colorsList" :key="m.id" :value="m.id || m.ten || m.tenMau">{{ m.ten || m.tenMau }}</option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.idKichCo" class="filter-select" :class="{ active: !!modalFilters.idKichCo }">
                    <option value="">Tất cả kích cỡ</option>
                    <option v-for="s in sizesList" :key="s.id" :value="s.id || s.ten || s.tenKichCo">{{ s.ten || s.tenKichCo }}</option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.priceRange" class="filter-select" :class="{ active: !!modalFilters.priceRange }">
                    <option value="">Tất cả mức giá</option>
                    <option value="<1m">Dưới 1.000.000 ₫</option>
                    <option value="1m-3m">1.000.000 - 3.000.000 ₫</option>
                    <option value="3m-5m">3.000.000 - 5.000.000 ₫</option>
                    <option value=">5m">Trên 5.000.000 ₫</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- Selection summary -->
            <div class="pos-prod-select-summary">
              <span class="select-summary-text">
                Đã chọn <strong>{{ modalSelectedIds.length }}</strong> sản phẩm
                <template v-if="filteredModalVariants.length === 0">(không có kết quả khớp bộ lọc)</template>
              </span>
              <button v-if="modalSelectedIds.length > 0" class="btn-clear-selection"
                @click="modalSelectedIds = []">Bỏ chọn tất cả</button>
            </div>

            <!-- Product Table -->
            <div class="pos-prod-table-wrap">
              <table class="pos-prod-table">
                <thead>
                  <tr>
                    <th width="42" class="text-center">
                      <input
                        type="checkbox"
                        class="custom-chk pos-chk"
                        :checked="isAllPageSelected"
                        @change="toggleSelectAllPage"
                        title="Chọn tất cả trên trang này"
                      />
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
                    :class="{ 'row-selected': isModalRowSelected(item.id) }"
                    @click="handleSelectProductFromModal(item)" title="Click để thêm nhanh hoặc tick chọn">
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
                        <span v-if="item.discount && item.discount !== '-'" class="price-was">{{ formatVnd(item.price || item.giaBan) }}</span>
                      </div>
                    </td>
                    <td class="text-center" @click.stop>
                      <button
                        class="btn-select-add-cart"
                        :disabled="(item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0"
                        @click="handleSelectProductFromModal(item)"
                      >
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
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

          <!-- Modal Footer: selection + confirm + pagination -->
          <div class="pos-prod-footer pos-prod-footer-with-action">
            <div class="pos-modal-page-size">
              <span class="page-size-label">Hiển thị:</span>
              <select v-model.number="modalPageSize" class="page-size-select">
                <option v-for="sz in pageSizeOptions" :key="sz" :value="sz">{{ sz }} / trang</option>
              </select>
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

    <!-- Voucher Selector Modal -->
    <transition name="fade">
      <div v-if="isVoucherModalOpen" class="pos-modal-overlay" @click.self="isVoucherModalOpen = false">
        <div class="pos-modal-dialog modal-md">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <h3>Phiếu giảm giá khả dụng</h3>
              <span class="modal-badge-count">{{ availableEligibleVouchersCount }}/{{ allVouchers.length }} khả dụng</span>
            </div>
            <button class="pos-modal-close" @click="isVoucherModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <!-- Thanh tìm kiếm & áp dụng nhanh trong modal -->
            <div class="voucher-modal-search-bar">
              <input
                v-model="voucherSearchQuery"
                type="text"
                placeholder="Nhập mã hoặc tên phiếu giảm giá..."
                class="voucher-search-inp"
                @keyup.enter="handleModalSearchApply"
              />
              <button
                v-if="voucherSearchQuery.trim()"
                type="button"
                class="btn-modal-apply-code"
                @click="handleModalSearchApply"
              >
                Áp dụng mã
              </button>
            </div>

            <!-- Cảnh báo nếu đang là khách lẻ kèm nút chọn/thêm nhanh -->
            <div v-if="!currentOrder?.customer" class="voucher-customer-alert">
              <div class="alert-content-wrap">
                <span class="alert-title">Đang là khách lẻ</span>
                <span class="alert-desc">Phiếu giảm giá chỉ áp dụng cho khách hàng đã định danh để tích điểm và ghi nhận lịch sử.</span>
              </div>
              <button type="button" class="btn-alert-pick-customer" @click="openQuickCustomerFromVoucher">
                Thêm / Chọn khách ngay
              </button>
            </div>

            <!-- Danh sách phiếu giảm giá -->
            <div class="voucher-cards-list">
              <div
                v-for="v in filteredVouchers"
                :key="v.id"
                class="voucher-card-item"
                :class="{
                  eligible: isVoucherEligible(v),
                  selected: currentOrder?.selectedVoucher?.id === v.id,
                  ineligible: !isVoucherEligible(v)
                }"
              >
                <div class="voucher-card-left">
                  <div class="voucher-discount-highlight">
                    {{ formatVoucherValue(v) }}
                  </div>
                  <div class="voucher-type-sub">
                    {{ v.loaiGiamGia === 'PERCENT' || v.loaiGiamGia === '%' ? 'GIẢM %' : 'GIẢM TIỀN' }}
                  </div>
                </div>

                <div class="voucher-card-body">
                  <div class="voucher-card-header-row">
                    <span class="voucher-card-code">{{ v.ma }}</span>
                    <span v-if="v.soLuong" class="voucher-card-remain">
                      Còn {{ Math.max(0, (v.soLuong || 0) - (v.soLuongDaSuDung || 0)) }} lượt
                    </span>
                  </div>
                  <div class="voucher-card-name">{{ v.ten }}</div>
                  <div class="voucher-card-conditions">
                    <span>Đơn tối thiểu: <strong>{{ formatVnd(v.hoaDonToiThieu || 0) }}</strong></span>
                    <span v-if="v.giamToiDa"> • Tối đa: <strong>{{ formatVnd(v.giamToiDa) }}</strong></span>
                  </div>
                  <div class="voucher-card-date" v-if="v.ngayKetThuc">
                    Hạn dùng: {{ formatDateVn(v.ngayKetThuc) }}
                  </div>
                  <div v-if="!isVoucherEligible(v)" class="voucher-ineligible-msg">
                    {{ getVoucherIneligibilityReason(v) }}
                  </div>
                </div>

                <div class="voucher-card-action">
                  <button
                    v-if="currentOrder?.selectedVoucher?.id === v.id"
                    type="button"
                    class="btn-voucher-applied"
                    @click="removeVoucher"
                    title="Bấm để gỡ bỏ voucher"
                  >
                    Đang dùng (Gỡ)
                  </button>
                  <button
                    v-else
                    type="button"
                    class="btn-apply-voucher"
                    :disabled="!isVoucherEligible(v)"
                    @click="applyVoucher(v)"
                  >
                    Áp dụng
                  </button>
                </div>
              </div>

              <div v-if="filteredVouchers.length === 0" class="empty-vouchers-msg">
                Không tìm thấy phiếu giảm giá nào phù hợp với "{{ voucherSearchQuery }}".
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Camera QR Scanner Modal (F3) -->
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
              <!-- Khung Camera Viewport -->
              <div class="pos-camera-viewport">
                <div id="pos-camera-scanner-view" class="pos-camera-stream-el"></div>

                <!-- Tia laser chuyển động khi camera đang chạy -->
                <div v-if="isCameraRunning && !scanSuccessAnim" class="pos-scanner-laser-line"></div>

                <!-- 4 góc khung ngắm -->
                <div class="pos-scanner-frame-overlay">
                  <span class="corner top-left"></span>
                  <span class="corner top-right"></span>
                  <span class="corner bottom-left"></span>
                  <span class="corner bottom-right"></span>
                </div>

                <!-- Thông báo quét thành công trực tiếp trên camera -->
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

              <!-- Hướng dẫn & Trạng thái lỗi nếu có -->
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

              <!-- Nhập thủ công dự phòng nếu không quét được -->
              <div class="pos-manual-code-row">
                <input
                  v-model="scannedCodeInput"
                  type="text"
                  class="pos-manual-code-inp"
                  placeholder="Hoặc nhập mã CTSP / Barcode thủ công rồi bấm Thêm..."
                  @keyup.enter="handleManualCodeSubmit"
                />
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
              <!-- Họ và tên -->
              <div class="cust-form-group">
                <label class="cust-label">Họ và tên <span class="required-star">*</span></label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                      <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                  </span>
                  <input
                    type="text"
                    class="cust-input"
                    placeholder="VD: Nguyễn Văn Anh"
                    v-model="newCustomerForm.tenKhachHang"
                    @keyup.enter="saveQuickCustomer"
                  />
                </div>
              </div>

              <!-- Số điện thoại -->
              <div class="cust-form-group">
                <label class="cust-label">Số điện thoại <span class="required-star">*</span></label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                    </svg>
                  </span>
                  <input
                    type="tel"
                    class="cust-input"
                    placeholder="VD: 0912345678"
                    v-model="newCustomerForm.soDienThoai"
                    @keyup.enter="saveQuickCustomer"
                  />
                </div>
              </div>

              <!-- Email -->
              <div class="cust-form-group">
                <label class="cust-label">Email</label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                      <polyline points="22,6 12,13 2,6"></polyline>
                    </svg>
                  </span>
                  <input
                    type="email"
                    class="cust-input"
                    placeholder="VD: khachhang@gmail.com"
                    v-model="newCustomerForm.email"
                  />
                </div>
              </div>

              <!-- Giới tính -->
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

              <!-- Ngày sinh -->
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
                  <input
                    type="date"
                    class="cust-input"
                    v-model="newCustomerForm.ngaySinh"
                  />
                </div>
              </div>

              <!-- Địa chỉ -->
              <div class="cust-form-group cust-full-span">
                <label class="cust-label">Địa chỉ</label>
                <div class="cust-input-wrap">
                  <span class="cust-input-icon top-aligned">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                      <circle cx="12" cy="10" r="3"></circle>
                    </svg>
                  </span>
                  <textarea
                    rows="2"
                    class="cust-textarea"
                    placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
                    v-model="newCustomerForm.diaChi"
                  ></textarea>
                </div>
              </div>
            </div>

            <!-- Hint box -->
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
      <div v-if="isBankQrModalOpen" class="pos-modal-overlay" @click.self="isBankQrModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Quét mã thanh toán ngân hàng</h3>
            <button class="pos-modal-close" @click="isBankQrModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="vietqr-wrap">
              <img v-if="bankQrDataUrl" :src="bankQrDataUrl" class="vietqr-img" alt="VietQR" />
              <div class="vietqr-amount">{{ formatVnd(finalTotal) }}</div>
              <div class="vietqr-memo">
                Nội dung CK: <strong>Thanh toan don hang {{ currentOrder?.code }}</strong>
              </div>
            </div>
          </div>

          <div class="pos-modal-foot">
            <button class="btn-pos-checkout ready" style="height: 36px; padding: 0 16px;"
              @click="isBankQrModalOpen = false">Đã nhận tiền</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="isReceiptModalOpen && completedInvoiceData" class="pos-modal-overlay"
        @click.self="isReceiptModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Hóa đơn bán hàng</h3>
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
                    <th>Tên hàng</th>
                    <th class="text-center" width="45">SL</th>
                    <th class="text-end" width="95">T.Tiền</th>
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
                <div class="receipt-sum-row" v-if="completedInvoiceData.discount > 0">
                  <span>Giảm giá:</span>
                  <strong>- {{ formatVnd(completedInvoiceData.discount) }}</strong>
                </div>
                <div class="receipt-sum-row" v-if="completedInvoiceData.shippingFee > 0">
                  <span>Phí vận chuyển:</span>
                  <strong>+ {{ formatVnd(completedInvoiceData.shippingFee) }}</strong>
                </div>
                <div class="receipt-sum-row grand-sum">
                  <span>THANH TOÁN:</span>
                  <span>{{ formatVnd(completedInvoiceData.finalTotal) }}</span>
                </div>
                <div class="receipt-sum-row" v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT'">
                  <span>Tiền khách đưa:</span>
                  <span>{{ formatVnd(completedInvoiceData.cashGiven) }}</span>
                </div>
                <div class="receipt-sum-row" v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT'">
                  <span>Tiền thừa trả khách:</span>
                  <span>{{ formatVnd(completedInvoiceData.changeDue) }}</span>
                </div>
              </div>

              <div class="receipt-thank-msg">
                Cảm ơn quý khách và hẹn gặp lại!
              </div>
            </div>
          </div>

          <div class="pos-modal-foot">
            <button class="btn-pos-cancel" style="height: 36px; padding: 0 16px;"
              @click="isReceiptModalOpen = false">Đóng</button>
            <button class="btn-pos-checkout ready" style="height: 36px; padding: 0 16px;" @click="printReceipt">
              In hóa đơn
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<style scoped src="@/assets/styles/PosSalesPage.css"></style>
