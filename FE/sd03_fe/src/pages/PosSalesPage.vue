<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import QRCode from 'qrcode'
import http from '@/api/http'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import { formatVnd } from '@/utils/format'

const API_BASE = 'http://localhost:8080/api/v1'

// ==========================================
// 1. DATA STATE & PENDING INVOICES
// ==========================================
const MAX_ORDERS = 5
const pendingOrders = ref([])
const activeOrderIndex = ref(0)

const allProducts = ref([])
const allCustomers = ref([])
const allVouchers = ref([])
const allEmployees = ref([])
const allPaymentMethods = ref([])
const isLoadingData = ref(false)

// Attributes for modal filters
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

  // Hoàn trả lại số lượng vào tồn kho
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

// ==========================================
// 2. CART TOTAL CALCULATIONS
// ==========================================
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

// ==========================================
// 3. CART OPERATIONS
// ==========================================
const getEffectivePrice = (item) => {
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

  // Tìm biến thể trong allProducts để kiểm tra và trừ tồn kho
  const targetProd = allProducts.value.find(p => p.id === productVariant.id)
  const currentAvailableStock = targetProd
    ? (targetProd.stock !== undefined ? targetProd.stock : (targetProd.soLuong || 0))
    : (productVariant.stock !== undefined ? productVariant.stock : (productVariant.soLuong || 0))

  if (currentAvailableStock < qtyToAdd) {
    notifyWarning('Tồn kho không đủ!', `Sản phẩm này chỉ còn ${currentAvailableStock} sản phẩm trong kho.`)
    return
  }

  // Trừ tồn kho trong danh sách sản phẩm
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
    // Tăng số lượng -> kiểm tra tồn kho và trừ đi
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
    // Giảm số lượng -> hoàn trả lại vào tồn kho
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
    // Hoàn trả lại toàn bộ số lượng vào kho
    const targetProd = allProducts.value.find(p => p.id === item.idCtsp)
    if (targetProd) {
      if (targetProd.stock !== undefined) targetProd.stock += item.quantity
      if (targetProd.soLuong !== undefined) targetProd.soLuong += item.quantity
    }
    currentOrder.value.cart.splice(idx, 1)
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

// ==========================================
// 4. CUSTOMER SEARCH & MANAGEMENT
// ==========================================
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

const saveQuickCustomer = () => {
  if (!newCustomerForm.tenKhachHang.trim()) {
    notifyWarning('Thiếu thông tin', 'Vui lòng nhập tên khách hàng!')
    return
  }
  if (!newCustomerForm.soDienThoai.trim()) {
    notifyWarning('Thiếu thông tin', 'Vui lòng nhập số điện thoại khách hàng!')
    return
  }

  const createdCust = {
    id: Date.now(),
    ten: newCustomerForm.tenKhachHang.trim(),
    soDienThoai: newCustomerForm.soDienThoai.trim(),
    email: newCustomerForm.email.trim(),
    diaChi: newCustomerForm.diaChi.trim()
  }

  allCustomers.value.unshift(createdCust)
  selectCustomer(createdCust)
  isAddCustomerModalOpen.value = false

  newCustomerForm.tenKhachHang = ''
  newCustomerForm.soDienThoai = ''
  newCustomerForm.email = ''
  newCustomerForm.diaChi = ''

  notifySuccess('Thành công', 'Đã thêm và chọn khách hàng mới!')
}

// ==========================================
// 5. PRODUCT PICKER MODAL (LIKE SCREENSHOT)
// ==========================================
const isProductModalOpen = ref(false)
const modalFilters = reactive({
  search: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idMauSac: '',
  idKichCo: '',
  priceRange: ''
})

const modalPageSize = ref(5)
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

  // 1. Search text
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

  // 2. Brand
  if (modalFilters.idThuongHieu) {
    list = list.filter(p => String(p.idThuongHieu) === String(modalFilters.idThuongHieu) || (p.tenThuongHieu && p.tenThuongHieu === modalFilters.idThuongHieu))
  }

  // 3. Category / Shoe type
  if (modalFilters.idLoaiGiay) {
    list = list.filter(p => String(p.idLoaiGiay) === String(modalFilters.idLoaiGiay) || (p.tenLoaiGiay && p.tenLoaiGiay === modalFilters.idLoaiGiay))
  }

  // 4. Color
  if (modalFilters.idMauSac) {
    list = list.filter(p => String(p.idMauSac) === String(modalFilters.idMauSac) || (p.color && p.color === modalFilters.idMauSac) || (p.tenMau && p.tenMau === modalFilters.idMauSac))
  }

  // 5. Size
  if (modalFilters.idKichCo) {
    list = list.filter(p => String(p.idKichCo) === String(modalFilters.idKichCo) || (p.size && p.size === modalFilters.idKichCo) || (p.tenKichCo && p.tenKichCo === modalFilters.idKichCo))
  }

  // 6. Price Range
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

const handleCheckout = async () => {
  if (!currentOrder.value) return
  if (currentOrder.value.cart.length === 0) {
    notifyWarning('Giỏ hàng trống', 'Vui lòng chọn ít nhất 1 sản phẩm vào giỏ hàng trước khi thanh toán!')
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
    const payload = {
      maHoaDon: currentOrder.value.code,
      loaiDon: currentOrder.value.isDelivery ? 'Online' : 'Tại quầy',
      phiVanChuyen: currentOrder.value.isDelivery ? Number(currentOrder.value.deliveryInfo.shippingFee || 0) : 0,
      tenKhachHang: currentOrder.value.customer ? currentOrder.value.customer.ten : 'Khách lẻ',
      soDienThoaiKhachHang: currentOrder.value.customer ? currentOrder.value.customer.soDienThoai : '',
      diaChiNhanHang: currentOrder.value.isDelivery ? currentOrder.value.deliveryInfo.recipientAddress : '',
      trangThai: 'Đã hoàn thành',
      ghiChu: currentOrder.value.note || 'Bán hàng tại quầy POS',
      idKhachHang: currentOrder.value.customer ? currentOrder.value.customer.id : null,
      idNhanVien: allEmployees.value.length > 0 ? allEmployees.value[0].id : null,
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

    pendingOrders.value.splice(activeOrderIndex.value, 1)
    if (activeOrderIndex.value >= pendingOrders.value.length) {
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

// ==========================================
// 9. INITIAL LOAD
// ==========================================
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
      allVouchers.value = d.phieuGiamGias || []
      allEmployees.value = d.nhanViens || []
      allPaymentMethods.value = d.phuongThucs || []
    }

    if (brandRes.status === 'fulfilled' && Array.isArray(brandRes.value)) brandsList.value = brandRes.value
    if (catRes.status === 'fulfilled' && Array.isArray(catRes.value)) categoriesList.value = catRes.value
    if (colorRes.status === 'fulfilled' && Array.isArray(colorRes.value)) colorsList.value = colorRes.value
    if (sizeRes.status === 'fulfilled' && Array.isArray(sizeRes.value)) sizesList.value = sizeRes.value

    if (prodRes.status === 'fulfilled' && prodRes.value) {
      allProducts.value = prodRes.value.content || []
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
  loadInitialData()
})
</script>

<template>
  <div class="pos-page">
    <div class="pos-top-bar">
      <div class="pos-breadcrumbs">
        <span>Trang chủ</span> / <span class="active">Bán hàng tại quầy</span>
      </div>
      <div class="pos-title-badge">
        <span class="pos-badge-live">
          <span class="pulse-dot"></span> Quầy POS đang hoạt động
        </span>
      </div>
    </div>

    <div class="pos-main-grid">
      <div class="pos-col-left">

        <!-- CARD 1: HÓA ĐƠN CHỜ (PENDING ORDERS) -->
        <div class="pos-card pending-orders-card">
          <div class="pending-orders-head">
            <button class="btn-add-order" @click="createNewOrder" :disabled="pendingOrders.length >= MAX_ORDERS">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              Thêm hóa đơn chờ
            </button>
            <div class="pending-counter">
              {{ pendingOrders.length }}/{{ MAX_ORDERS }} hóa đơn
            </div>
          </div>

          <div v-if="pendingOrders.length === 0" class="empty-pending-box">
            Chưa có hóa đơn chờ nào.
          </div>
          <div v-else class="pending-tabs-list">
            <div v-for="(order, idx) in pendingOrders" :key="order.id" class="pending-tab-item"
              :class="{ active: activeOrderIndex === idx }" @click="selectOrderTab(idx)">
              <span>{{ order.title }}</span>
              <span class="tab-badge-count" v-if="order.cart.length > 0">{{ order.cart.length }}</span>
              <button class="tab-close-btn" @click="closeOrderTab(idx, $event)" title="Đóng hóa đơn này">×</button>
            </div>
          </div>
        </div>

        <!-- CARD 2: GIỎ HÀNG (CART) -->
        <div class="pos-card cart-card">
          <div class="cart-card-head">
            <div class="cart-title-wrap">
              <h3 class="cart-title">Giỏ hàng</h3>
              <span class="cart-qty-pill">{{ totalCartQty }} sản phẩm</span>
            </div>

            <div class="cart-actions-right">
              <button class="btn-qr-scan" @click="openQrScanner" title="Quét mã vạch / QR sản phẩm">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="7" height="7"></rect>
                  <rect x="14" y="3" width="7" height="7"></rect>
                  <rect x="14" y="14" width="7" height="7"></rect>
                  <rect x="3" y="14" width="7" height="7"></rect>
                  <line x1="7" y1="7" x2="7" y2="7"></line>
                  <line x1="17" y1="7" x2="17" y2="7"></line>
                  <line x1="7" y1="17" x2="7" y2="17"></line>
                  <line x1="17" y1="17" x2="17" y2="17"></line>
                </svg>
              </button>

              <button class="btn-pick-product" @click="openProductPicker">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                Chọn sản phẩm
              </button>
            </div>
          </div>

          <div class="cart-table-wrapper">
            <table class="cart-table">
              <thead>
                <tr>
                  <th width="50" class="text-center">STT</th>
                  <th width="120">Mã sản phẩm</th>
                  <th>Tên sản phẩm</th>
                  <th width="100">Màu sắc</th>
                  <th width="90">Kích cỡ</th>
                  <th width="120" class="text-center">Số lượng</th>
                  <th width="130" class="text-end">Đơn giá</th>
                  <th width="70" class="text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="currentOrder && currentOrder.cart.length > 0">
                  <tr v-for="(item, idx) in currentOrder.cart" :key="item.idCtsp">
                    <td class="cell-stt">{{ idx + 1 }}</td>
                    <td class="cell-code">{{ item.maCtsp || item.maSp }}</td>
                    <td class="cell-name" :title="item.name">{{ item.name }}</td>
                    <td>
                      <span class="badge-color-tag">{{ item.color }}</span>
                    </td>
                    <td>
                      <span class="badge-size-tag">{{ item.size }}</span>
                    </td>
                    <td class="text-center">
                      <div class="qty-stepper">
                        <button class="stepper-btn" @click="updateCartItemQty(item, item.quantity - 1)"
                          :disabled="item.quantity <= 1">−</button>
                        <input type="number" class="stepper-input" v-model.number="item.quantity"
                          @change="updateCartItemQty(item, item.quantity)" min="1" />
                        <button class="stepper-btn" @click="updateCartItemQty(item, item.quantity + 1)"
                          :disabled="getRemainingStock(item.idCtsp) <= 0">+</button>
                      </div>
                    </td>
                    <td class="text-end cell-price">{{ formatVnd(item.price * item.quantity) }}</td>
                    <td class="text-center">
                      <button class="btn-del-cart-item" @click="removeCartItem(item)" title="Xóa khỏi giỏ hàng">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                          stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2">
                          </path>
                        </svg>
                      </button>
                    </td>
                  </tr>
                </template>
                <tr v-else>
                  <td colspan="8" class="empty-cart-row">Giỏ hàng trống.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>

      <!-- RIGHT COLUMN: KHÁCH HÀNG & THÔNG TIN ĐƠN HÀNG -->
      <div class="pos-col-right">
        <div class="pos-card customer-card">
          <div class="customer-card-head">
            <h4 class="card-title-sm">Khách hàng</h4>
          </div>

          <div class="customer-search-box">
            <input type="text" class="input-customer-search" placeholder="Nhập tên hoặc số điện thoại khách hàng"
              v-model="customerSearchQuery" @focus="showCustomerDropdown = true" />
            <button class="btn-add-customer-quick" @click="isAddCustomerModalOpen = true"
              title="Thêm nhanh khách hàng">+</button>

            <ul v-if="showCustomerDropdown && filteredCustomers.length > 0" class="customer-autocomplete-list">
              <li v-for="c in filteredCustomers" :key="c.id" class="customer-auto-item" @click="selectCustomer(c)">
                <span class="cust-auto-name">{{ c.ten || c.tenKhachHang }}</span>
                <span class="cust-auto-phone">{{ c.soDienThoai }}</span>
              </li>
            </ul>
          </div>

          <div v-if="currentOrder && currentOrder.customer" class="selected-customer-info">
            <div class="cust-profile-left">
              <div class="cust-avatar-icon">
                {{ (currentOrder.customer.ten || 'K')[0].toUpperCase() }}
              </div>
              <div class="cust-details">
                <span class="cust-name">{{ currentOrder.customer.ten || currentOrder.customer.tenKhachHang }}</span>
                <span class="cust-phone">{{ currentOrder.customer.soDienThoai }}</span>
              </div>
            </div>
            <button class="btn-remove-cust" @click="removeSelectedCustomer"
              title="Bỏ chọn (chuyển về khách lẻ)">×</button>
          </div>
        </div>

        <div class="pos-card order-checkout-card">
          <div class="order-info-head">
            <h4 class="order-info-title">Thông tin đơn hàng</h4>
            <div class="delivery-toggle-wrap">
              <span>Giao hàng</span>
              <label class="switch-ui">
                <input type="checkbox" v-if="currentOrder" v-model="currentOrder.isDelivery" />
                <span class="slider-round"></span>
              </label>
            </div>
          </div>

          <div v-if="currentOrder && currentOrder.isDelivery" class="delivery-form-box">
            <input type="text" class="del-input" placeholder="Tên người nhận hàng..."
              v-model="currentOrder.deliveryInfo.recipientName" />
            <input type="text" class="del-input" placeholder="Số điện thoại người nhận..."
              v-model="currentOrder.deliveryInfo.recipientPhone" />
            <input type="text" class="del-input" placeholder="Địa chỉ giao hàng chi tiết..."
              v-model="currentOrder.deliveryInfo.recipientAddress" />
            <input type="number" class="del-input" placeholder="Phí vận chuyển (VNĐ)..."
              v-model.number="currentOrder.deliveryInfo.shippingFee" />
          </div>

          <div class="summary-row">
            <span class="summary-label">Tổng tiền hàng ({{ totalCartQty }} sản phẩm):</span>
            <span class="summary-val">{{ formatVnd(subTotal) }}</span>
          </div>

          <div v-if="discountAmount > 0" class="summary-row">
            <span class="summary-label">Giảm giá voucher:</span>
            <span class="summary-val" style="color: #16a34a;">- {{ formatVnd(discountAmount) }}</span>
          </div>

          <div v-if="currentOrder && currentOrder.isDelivery && currentOrder.deliveryInfo.shippingFee > 0"
            class="summary-row">
            <span class="summary-label">Phí vận chuyển:</span>
            <span class="summary-val">+ {{ formatVnd(currentOrder.deliveryInfo.shippingFee) }}</span>
          </div>

          <div class="payment-section" v-if="currentOrder">
            <div class="section-subtitle">Hình thức thanh toán</div>
            <div class="radio-group-pay">
              <label class="radio-pay-label">
                <input type="radio" value="TIEN_MAT" v-model="currentOrder.paymentMethod" />
                Tiền mặt
              </label>
              <label class="radio-pay-label">
                <input type="radio" value="CHUYEN_KHOAN" v-model="currentOrder.paymentMethod" />
                Chuyển khoản
              </label>
              <label class="radio-pay-label">
                <input type="radio" value="KET_HOP" v-model="currentOrder.paymentMethod" />
                Kết hợp
              </label>
            </div>
          </div>

          <div class="pay-amount-box" v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'">
            <div class="section-subtitle">Khách thanh toán</div>
            <input type="number" class="input-money-paid" placeholder="Nhập số tiền khách đưa"
              v-model.number="currentOrder.cashGiven" />
            <div class="quick-cash-chips">
              <button class="cash-chip" @click="setQuickCash('EXACT')">Đủ tiền</button>
              <button class="cash-chip" @click="setQuickCash(50000)">50k</button>
              <button class="cash-chip" @click="setQuickCash(100000)">100k</button>
              <button class="cash-chip" @click="setQuickCash(200000)">200k</button>
              <button class="cash-chip" @click="setQuickCash(500000)">500k</button>
            </div>
          </div>

          <div class="pay-amount-box" v-if="currentOrder && currentOrder.paymentMethod === 'KET_HOP'">
            <div class="section-subtitle">Chuyển khoản thêm</div>
            <input type="number" class="input-money-paid" placeholder="Nhập số tiền chuyển khoản"
              v-model.number="currentOrder.transferGiven" />
          </div>

          <div class="summary-row" v-if="currentOrder && currentOrder.paymentMethod !== 'CHUYEN_KHOAN'">
            <span class="summary-label">Tiền thừa trả khách</span>
            <span class="summary-val" :style="{ color: changeDue < 0 ? '#dc2626' : '#0f172a' }">
              {{ changeDue >= 0 ? formatVnd(changeDue) : `Thiếu ${formatVnd(Math.abs(changeDue))}` }}
            </span>
          </div>

          <div class="notes-section" v-if="currentOrder">
            <div class="section-subtitle">Ghi chú thanh toán</div>
            <textarea class="textarea-notes" placeholder="Ghi chú thêm nếu cần" v-model="currentOrder.note"></textarea>
          </div>

          <div class="order-action-btns">
            <button class="btn-pos-cancel" @click="handleCancelOrder"
              :disabled="!currentOrder || currentOrder.cart.length === 0">
              Hủy hóa đơn
            </button>
            <button class="btn-pos-checkout" :class="{ ready: currentOrder && currentOrder.cart.length > 0 }"
              @click="handleCheckout"
              :disabled="!currentOrder || currentOrder.cart.length === 0 || isProcessingCheckout">
              {{ isProcessingCheckout ? 'Đang xử lý...' : 'Thanh toán' }}
            </button>
          </div>
        </div>

      </div>

    </div>

    <!-- ========================================================
         MODAL 1: DANH SÁCH SẢN PHẨM (EXACT MATCH SCREENSHOT)
         ======================================================== -->
    <transition name="fade">
      <div v-if="isProductModalOpen" class="pos-modal-overlay" @click.self="isProductModalOpen = false">
        <div class="pos-modal-dialog modal-xl">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Danh sách sản phẩm</h3>
            <button class="pos-modal-close" @click="isProductModalOpen = false">✕</button>
          </div>

          <div class="pos-modal-body">
            <!-- Filter Bar -->
            <div class="pos-prod-filter-bar">
              <!-- Search Input -->
              <div class="filter-search-wrap">
                <svg class="search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input type="text" class="filter-search-input" placeholder="Tìm kiếm mã, tên sản phẩm..."
                  v-model="modalFilters.search" />
              </div>

              <!-- Brand Filter -->
              <select v-model="modalFilters.idThuongHieu" class="filter-select">
                <option value="">Tất cả thương hiệu</option>
                <option v-for="b in brandsList" :key="b.id" :value="b.id || b.ten">{{ b.ten || b.tenThuongHieu }}
                </option>
              </select>

              <!-- Category / Loai Giay Filter -->
              <select v-model="modalFilters.idLoaiGiay" class="filter-select">
                <option value="">Tất cả thể loại</option>
                <option v-for="c in categoriesList" :key="c.id" :value="c.id || c.ten">{{ c.ten || c.tenLoaiGiay }}
                </option>
              </select>

              <!-- Color Filter -->
              <select v-model="modalFilters.idMauSac" class="filter-select">
                <option value="">Tất cả màu</option>
                <option v-for="m in colorsList" :key="m.id" :value="m.id || m.ten || m.tenMau">{{ m.ten || m.tenMau }}
                </option>
              </select>

              <!-- Size Filter -->
              <select v-model="modalFilters.idKichCo" class="filter-select">
                <option value="">Tất cả size</option>
                <option v-for="s in sizesList" :key="s.id" :value="s.id || s.ten || s.tenKichCo">{{ s.ten || s.tenKichCo
                  }}
                </option>
              </select>

              <!-- Price Range Filter -->
              <select v-model="modalFilters.priceRange" class="filter-select">
                <option value="">Tất cả mức giá</option>
                <option value="<1m">Dưới 1.000.000 đ</option>
                <option value="1m-3m">1.000.000 đ - 3.000.000 đ</option>
                <option value="3m-5m">3.000.000 đ - 5.000.000 đ</option>
                <option value=">5m">Trên 5.000.000 đ</option>
              </select>

              <!-- Refresh Button -->
              <button class="btn-refresh-filter" @click="resetModalFilters" title="Đặt lại bộ lọc">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 4v6h-6"></path>
                  <path d="M1 20v-6h6"></path>
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
                </svg>
                Làm mới
              </button>
            </div>

            <!-- Product Table -->
            <div class="pos-prod-table-wrap">
              <table class="pos-prod-table">
                <thead>
                  <tr>
                    <th width="45" class="text-center">STT</th>
                    <th width="160">Mã Sản Phẩm</th>
                    <th>Tên Sản Phẩm</th>
                    <th width="100">Màu sắc</th>
                    <th width="70">Size</th>
                    <th width="70" class="text-center">Ảnh</th>
                    <th width="85">Số lượng</th>
                    <th width="140">Giá bán</th>
                    <th width="90">Giảm giá</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in paginatedModalVariants" :key="item.id"
                    @click="handleSelectProductFromModal(item)" title="Click để thêm sản phẩm vào giỏ hàng">
                    <td class="prod-stt">{{ modalCurrentPage * modalPageSize + idx + 1 }}</td>
                    <td class="prod-code">{{ item.maCtsp || item.maChiTietSanPham || item.maSp }}</td>
                    <td class="prod-name" :class="{ plain: !item.discount }">
                      {{ item.tenSp || item.tenSanPham }}
                    </td>
                    <td class="prod-color">{{ item.color || item.tenMau || '---' }}</td>
                    <td class="prod-size">{{ item.size || item.tenKichCo || '---' }}</td>
                    <td class="text-center">
                      <div class="prod-img-box">
                        <img :src="getShoeImage(item)" class="prod-img-thumb" alt="shoe" />
                        <span v-if="item.discount" class="badge-discount-overlay">{{ item.discount }}</span>
                      </div>
                    </td>
                    <td class="prod-stock">{{ item.stock !== undefined ? item.stock : (item.soLuong || 0) }}</td>
                    <td>
                      <div class="prod-price-block">
                        <span class="price-current">{{ formatVnd(getEffectivePrice(item)) }}</span>
                        <span v-if="item.discount" class="price-original">
                          {{ formatVnd(item.price || item.giaBan) }}
                        </span>
                      </div>
                    </td>
                    <td>
                      <span v-if="item.discount" class="prod-discount-tag">{{ item.discount }}</span>
                      <span v-else class="prod-discount-tag none">-</span>
                    </td>
                  </tr>
                  <tr v-if="paginatedModalVariants.length === 0">
                    <td colspan="9" class="empty-cart-row">Không tìm thấy sản phẩm phù hợp.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Modal Pagination Footer -->
          <div class="pos-prod-footer">
            <div class="page-size-selector">
              <select v-model.number="modalPageSize">
                <option v-for="sz in pageSizeOptions" :key="sz" :value="sz">{{ sz }}</option>
              </select>
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
              <span v-if="modalTotalPages > 10" style="padding: 0 4px; color: #94a3b8;">...</span>
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
          </div>
        </div>
      </div>
    </transition>

    <!-- ========================================================
         MODAL 2: QUÉT MÃ QR / BARCODE SCANNER
         ======================================================== -->
    <transition name="fade">
      <div v-if="isQrModalOpen" class="pos-modal-overlay" @click.self="isQrModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Quét mã vạch / Barcode</h3>
            <button class="pos-modal-close" @click="isQrModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="qr-modal-content">
              <div class="qr-scanner-box">
                <div class="qr-laser-line"></div>
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="1.5">
                  <rect x="3" y="3" width="7" height="7"></rect>
                  <rect x="14" y="3" width="7" height="7"></rect>
                  <rect x="14" y="14" width="7" height="7"></rect>
                  <rect x="3" y="14" width="7" height="7"></rect>
                </svg>
              </div>
              <p class="text-muted text-sm">Đặt mã vạch hoặc mã QR của sản phẩm trước máy quét hoặc nhập mã trực tiếp
                bên
                dưới:</p>

              <div style="width: 100%; display: flex; gap: 8px;">
                <input type="text" class="picker-search-input" placeholder="Nhập mã CTSP hoặc barcode..."
                  v-model="scannedCodeInput" @keyup.enter="handleBarcodeScan" autofocus />
                <button class="btn-pick-add" style="padding: 0 16px; font-size: 13px;" @click="handleBarcodeScan">
                  Tìm
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ========================================================
         MODAL 3: THÊM NHANH KHÁCH HÀNG
         ======================================================== -->
    <transition name="fade">
      <div v-if="isAddCustomerModalOpen" class="pos-modal-overlay" @click.self="isAddCustomerModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Thêm nhanh khách hàng</h3>
            <button class="pos-modal-close" @click="isAddCustomerModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="modal-form-grid">
              <div class="form-group-modal">
                <label>Họ và tên <span style="color: #dc2626;">*</span></label>
                <input type="text" placeholder="Nhập tên khách hàng..." v-model="newCustomerForm.tenKhachHang" />
              </div>
              <div class="form-group-modal">
                <label>Số điện thoại <span style="color: #dc2626;">*</span></label>
                <input type="text" placeholder="Nhập số điện thoại..." v-model="newCustomerForm.soDienThoai" />
              </div>
              <div class="form-group-modal">
                <label>Email</label>
                <input type="email" placeholder="Nhập địa chỉ email..." v-model="newCustomerForm.email" />
              </div>
              <div class="form-group-modal">
                <label>Địa chỉ</label>
                <textarea rows="2" placeholder="Nhập địa chỉ..." v-model="newCustomerForm.diaChi"></textarea>
              </div>
            </div>
          </div>

          <div class="pos-modal-foot">
            <button class="btn-pos-cancel" style="height: 36px; padding: 0 16px;"
              @click="isAddCustomerModalOpen = false">Hủy</button>
            <button class="btn-pos-checkout ready" style="height: 36px; padding: 0 16px;" @click="saveQuickCustomer">Lưu
              khách hàng</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ========================================================
         MODAL 4: QR CHUYỂN KHOẢN (VIETQR)
         ======================================================== -->
    <transition name="fade">
      <div v-if="isBankQrModalOpen" class="pos-modal-overlay" @click.self="isBankQrModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Quét mã thanh toán ngân hàng</h3>
            <button class="pos-modal-close" @click="isBankQrModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body text-center">
            <img v-if="bankQrDataUrl" :src="bankQrDataUrl"
              style="max-width: 220px; margin: 0 auto; border-radius: 8px; border: 1px solid #e2e8f0;" />
            <div style="margin-top: 12px; font-weight: 700; color: #dc2626; font-size: 16px;">
              Số tiền: {{ formatVnd(finalTotal) }}
            </div>
            <div style="font-size: 12.5px; color: #64748b; margin-top: 4px;">
              Nội dung CK: <strong>Thanh toan don hang {{ currentOrder?.code }}</strong>
            </div>
          </div>

          <div class="pos-modal-foot">
            <button class="btn-pos-checkout ready" style="height: 36px; padding: 0 16px;"
              @click="isBankQrModalOpen = false">Đã nhận tiền</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ========================================================
         MODAL 5: HÓA ĐƠN BÁN LẺ (IN RECEIPT)
         ======================================================== -->
    <transition name="fade">
      <div v-if="isReceiptModalOpen && completedInvoiceData" class="pos-modal-overlay"
        @click.self="isReceiptModalOpen = false">
        <div class="pos-modal-dialog modal-sm">
          <div class="pos-modal-head">
            <h3 class="pos-modal-title">Hóa đơn bán hàng</h3>
            <button class="pos-modal-close" @click="isReceiptModalOpen = false">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="receipt-printable-box">
              <div class="receipt-header">
                <h2 class="receipt-store-title">POLYSHOES STORE</h2>
                <p class="receipt-sub">Đ/C: Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</p>
                <p class="receipt-sub">Hotline: 0987.654.321</p>
              </div>

              <div class="receipt-meta">
                <div>Mã HĐ: <strong>{{ completedInvoiceData.code }}</strong></div>
                <div>Thời gian: {{ completedInvoiceData.date }}</div>
                <div>Khách hàng: <strong>{{ completedInvoiceData.customerName }}</strong></div>
                <div v-if="completedInvoiceData.customerPhone">SĐT: {{ completedInvoiceData.customerPhone }}</div>
              </div>

              <table class="receipt-table">
                <thead>
                  <tr>
                    <th>Tên hàng</th>
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

              <div class="receipt-total-block">
                <div class="summary-row">
                  <span>Tổng tiền:</span>
                  <strong>{{ formatVnd(completedInvoiceData.subTotal) }}</strong>
                </div>
                <div class="summary-row" v-if="completedInvoiceData.discount > 0">
                  <span>Giảm giá:</span>
                  <strong>- {{ formatVnd(completedInvoiceData.discount) }}</strong>
                </div>
                <div class="summary-row" v-if="completedInvoiceData.shippingFee > 0">
                  <span>Phí ship:</span>
                  <strong>+ {{ formatVnd(completedInvoiceData.shippingFee) }}</strong>
                </div>
                <div class="summary-row"
                  style="font-size: 15px; font-weight: 800; border-top: 1px solid #000; padding-top: 6px;">
                  <span>THANH TOÁN:</span>
                  <span>{{ formatVnd(completedInvoiceData.finalTotal) }}</span>
                </div>
                <div class="summary-row" v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT'">
                  <span>Tiền khách đưa:</span>
                  <span>{{ formatVnd(completedInvoiceData.cashGiven) }}</span>
                </div>
                <div class="summary-row" v-if="completedInvoiceData.paymentMethod === 'TIEN_MAT'">
                  <span>Tiền thừa:</span>
                  <span>{{ formatVnd(completedInvoiceData.changeDue) }}</span>
                </div>
              </div>

              <div class="receipt-foot-msg">
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
