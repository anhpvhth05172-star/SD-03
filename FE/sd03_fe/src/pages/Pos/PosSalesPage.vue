<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { Html5Qrcode } from 'html5-qrcode'
import http from '@/api/http'
import { getProvinces, getWards, getServices, tinhPhi } from '@/api/shipping'
import { DS_63_TINH_THANH } from '@/api/diaGioi'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm } from '@/utils/notify'
import { formatVnd, formatDateTime } from '@/utils/format'
import { getDangMo, moCa, getTongKet, ketCa } from '@/api/giaoCa'
import { inBienLaiDoiSoatK80 } from '@/utils/printShiftReceipt'

import { getUser, dangXuat } from '@/utils/auth'
import { datVaiTro, vaiTroHienTai } from '@/utils/session'

const API_BASE = '/api/v1'


const router = useRouter()

// --- QUẢN LÝ CA LÀM VIỆC (MỞ CA / ĐÓNG CA TRÊN POS) ---
const activeShift = ref(null)
const isLoadingShift = ref(false)

const isMoCaModalOpen = ref(false)
const moCaForm = reactive({
  tienDauCa: 300000,
  ghiChu: ''
})
const moCaSubmitting = ref(false)

const isKetCaModalOpen = ref(false)
const ketCaSummary = ref(null)
const ketCaLoading = ref(false)
const ketCaSubmitting = ref(false)
const ketCaForm = reactive({
  tienThucTe: 0,
  lyDoLech: '',
  ghiChu: ''
})

const chenhLechKetCa = computed(() => {
  const tienDuKien = Number(ketCaSummary.value?.tienDuKien || 0)
  const tienThucTe = Number(ketCaForm.tienThucTe || 0)
  return tienThucTe - tienDuKien
})

const fetchActiveShift = async () => {
  isLoadingShift.value = true
  try {
    activeShift.value = await getDangMo()
  } catch (err) {
    console.warn('Lỗi lấy thông tin ca đang mở:', err)
  } finally {
    isLoadingShift.value = false
  }
}

const openMoCaModal = () => {
  moCaForm.tienDauCa = 300000
  moCaForm.ghiChu = ''
  isMoCaModalOpen.value = true
}

const handleMoCaSubmit = async () => {
  const amount = Number(moCaForm.tienDauCa || 0)
  if (amount < 0) {
    notifyWarning('Số tiền không hợp lệ', 'Tiền mặt đầu ca không được âm')
    return
  }
  moCaSubmitting.value = true
  try {
    const res = await moCa({
      tienDauCa: amount,
      ghiChu: moCaForm.ghiChu?.trim() || null
    })
    activeShift.value = res
    isMoCaModalOpen.value = false
    notifySuccess('Mở ca thành công', `Đã mở ca ${res.maCa}. Bắt đầu phiên bán hàng!`)
  } catch (err) {
    notifyError('Mở ca thất bại', err.message || 'Không thể mở ca làm việc')
  } finally {
    moCaSubmitting.value = false
  }
}

const openKetCaModal = async () => {
  if (!activeShift.value?.id) {
    notifyWarning('Chưa có ca mở', 'Không có ca làm việc nào đang mở để kết ca')
    return
  }
  isKetCaModalOpen.value = true
  ketCaLoading.value = true
  try {
    ketCaSummary.value = await getTongKet(activeShift.value.id)
    ketCaForm.tienThucTe = Number(ketCaSummary.value?.tienDuKien || 0)
    ketCaForm.lyDoLech = ''
    ketCaForm.ghiChu = ''
  } catch (err) {
    notifyError('Lỗi tải tổng kết ca', err.message || 'Không thể lấy dữ liệu tổng kết ca')
    isKetCaModalOpen.value = false
  } finally {
    ketCaLoading.value = false
  }
}

const handleKetCaSubmit = async () => {
  if (!activeShift.value?.id) return
  const diff = chenhLechKetCa.value
  if (diff !== 0 && !ketCaForm.lyDoLech?.trim()) {
    notifyWarning('Thiếu lý do giải trình', `Phát hiện chênh lệch tiền mặt (${formatVnd(diff)}). Vui lòng nhập lý do giải trình trước khi đóng ca!`)
    return
  }
  ketCaSubmitting.value = true
  try {
    const closedCa = { ...activeShift.value }
    const closedSummary = { ...ketCaSummary.value }
    const closedForm = { ...ketCaForm }
    const closedCaId = activeShift.value.id

    await ketCa(closedCaId, {
      tienThucTe: Number(ketCaForm.tienThucTe || 0),
      lyDoLech: ketCaForm.lyDoLech?.trim() || null,
      ghiChu: ketCaForm.ghiChu?.trim() || null
    })
    isKetCaModalOpen.value = false
    activeShift.value = null
    notifySuccess('Đóng ca thành công', 'Ca làm việc đã được kết thúc và khóa sổ đối soát.')

    const wantPrint = await notifyConfirm(
      'In biên lai đối soát ca?',
      'Đã đóng ca thành công! Bạn có muốn in ngay Biên lai đối soát (khổ nhiệt K80) để kẹp két tiền không?',
      'In biên lai ngay',
      'Xem chi tiết ca'
    )
    if (wantPrint) {
      inBienLaiDoiSoatK80({
        ...closedCa,
        tienThucTe: Number(closedForm.tienThucTe || 0),
        lyDoLech: closedForm.lyDoLech,
        chenhLech: diff,
        thoiGianKet: new Date()
      }, closedSummary)
    }
    router.push(`/ca/${closedCaId}/doi-soat`)
  } catch (err) {
    notifyError('Kết ca thất bại', err.message || 'Không thể đóng ca làm việc')
  } finally {
    ketCaSubmitting.value = false
  }
}

const staffUserRef = ref(getUser())
const selectedStaffId = ref(staffUserRef.value?.idNhanVien || null)
const allEmployeesFull = ref([])
const staffSnapshotMap = new Map()
const pendingSyncAlerts = ref([])
let isSyncingHardCases = false
let realtimePollTimer = null

const staffUser = computed(() => staffUserRef.value)
const activePosStaff = computed(() => {
  if (selectedStaffId.value) {
    const found = allEmployeesFull.value.find(e => Number(e.id) === Number(selectedStaffId.value))
      || allEmployees.value.find(e => Number(e.id) === Number(selectedStaffId.value))
    if (found) return found
  }
  if (staffUserRef.value?.idNhanVien) {
    const found = allEmployeesFull.value.find(e => Number(e.id) === Number(staffUserRef.value.idNhanVien))
      || allEmployees.value.find(e => Number(e.id) === Number(staffUserRef.value.idNhanVien))
    if (found) return found
  }
  if (staffUserRef.value?.tenTaiKhoan) {
    const found = allEmployeesFull.value.find(e => e.tenTaiKhoan === staffUserRef.value.tenTaiKhoan)
      || allEmployees.value.find(e => (e.ten || e.tenTaiKhoan) === staffUserRef.value.tenTaiKhoan)
    if (found) return found
  }
  return allEmployeesFull.value[0] || allEmployees.value[0] || null
})

const staffName = computed(() => {
  const active = activePosStaff.value
  if (active) return active.tenTaiKhoan || active.ten || 'Nhân viên'
  return staffUserRef.value?.tenKhachHang || staffUserRef.value?.tenTaiKhoan || 'Nhân viên'
})

const staffRoleLabel = computed(() => {
  const active = activePosStaff.value
  if (active?.maVaiTro) {
    return String(active.maVaiTro).toUpperCase() === 'ADMIN' ? 'Admin' : 'Nhân viên'
  }
  if (active?.tenVaiTro) return active.tenVaiTro
  return vaiTroHienTai.value === 'ADMIN' ? 'Admin' : 'Nhân viên'
})

const MAX_ORDERS = 5
const POS_PENDING_STORAGE_KEY = 'polyshoes_pos_pending_orders_v1'
const POS_ACTIVE_IDX_STORAGE_KEY = 'polyshoes_pos_active_idx_v1'
const POS_RECENT_INVOICES_KEY = 'polyshoes_pos_recent_invoices_v1'
const POS_STAFF_SNAPSHOT_KEY = 'polyshoes_pos_staff_snapshot_v1'

const loadStaffSnapshotFromStorage = () => {
  try {
    const raw = sessionStorage.getItem(POS_STAFF_SNAPSHOT_KEY)
    if (!raw) return false
    const entries = JSON.parse(raw)
    if (Array.isArray(entries) && entries.length > 0) {
      staffSnapshotMap.clear()
      for (const [k, v] of entries) {
        staffSnapshotMap.set(Number(k), v)
      }
      return true
    }
  } catch {}
  return false
}

const saveStaffSnapshotToStorage = () => {
  try {
    sessionStorage.setItem(POS_STAFF_SNAPSHOT_KEY, JSON.stringify(Array.from(staffSnapshotMap.entries())))
  } catch {}
}

const pendingOrders = ref([])
const activeOrderIndex = ref(0)
let isRestoringOrders = false

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

// --- MỤC 6: ĐỒNG BỘ & CHẶN VƯỢT TỒN KHO CHÉO GIỮA CÁC HÓA ĐƠN CHỜ ---
const getReservedQtyAcrossOrders = (idCtsp, excludeOrderId = null) => {
  if (!idCtsp) return 0
  let total = 0
  for (const order of pendingOrders.value) {
    if (excludeOrderId !== null && order.id === excludeOrderId) continue
    if (!Array.isArray(order.cart)) continue
    for (const item of order.cart) {
      if (Number(item.idCtsp) === Number(idCtsp)) {
        total += Number(item.quantity || 0)
      }
    }
  }
  return total
}

const getReservedInOtherOrders = (idCtsp) => {
  return getReservedQtyAcrossOrders(idCtsp, currentOrder.value?.id ?? null)
}

const getServerBaseStock = (prod) => {
  if (!prod) return 0
  if (prod._serverStock !== undefined && prod._serverStock !== null) {
    return Number(prod._serverStock)
  }
  const raw = Number(prod.stock !== undefined ? prod.stock : (prod.soLuong || 0))
  prod._serverStock = raw
  return raw
}

const recalculateAvailableStocks = () => {
  if (!Array.isArray(allProducts.value)) return
  allProducts.value.forEach(p => {
    const baseStock = getServerBaseStock(p)
    const reserved = getReservedQtyAcrossOrders(p.id)
    const available = Math.max(0, baseStock - reserved)
    p.stock = available
    p.soLuong = available
  })
}

const setServerProductsList = (rawList) => {
  if (!Array.isArray(rawList)) return
  rawList.forEach(p => {
    p._serverStock = Number(p.stock !== undefined ? p.stock : (p.soLuong || 0))
  })
  allProducts.value = rawList
  reconcilePendingOrdersWithServerStock()
  recalculateAvailableStocks()
}

const ensureProductInCatalog = (variant) => {
  if (!variant || !variant.id) return null
  let existing = allProducts.value.find(p => Number(p.id) === Number(variant.id))
  if (!existing) {
    const rawStock = Number(variant.stock !== undefined ? variant.stock : (variant.soLuong || 0))
    const cloned = {
      ...variant,
      _serverStock: rawStock + getReservedQtyAcrossOrders(variant.id)
    }
    allProducts.value.push(cloned)
    recalculateAvailableStocks()
    existing = allProducts.value.find(p => Number(p.id) === Number(variant.id))
  }
  return existing
}

const reconcilePendingOrdersWithServerStock = () => {
  if (!Array.isArray(pendingOrders.value) || pendingOrders.value.length === 0) return
  if (!Array.isArray(allProducts.value) || allProducts.value.length === 0) return

  const usedMap = new Map()
  const adjustedMessages = []

  for (const order of pendingOrders.value) {
    if (!Array.isArray(order.cart)) {
      order.cart = []
      continue
    }
    for (let i = order.cart.length - 1; i >= 0; i--) {
      const item = order.cart[i]
      const prod = allProducts.value.find(p => Number(p.id) === Number(item.idCtsp))
      if (!prod) continue

      const serverStock = getServerBaseStock(prod)
      const alreadyUsed = usedMap.get(Number(item.idCtsp)) || 0
      const maxRemaining = Math.max(0, serverStock - alreadyUsed)

      if (maxRemaining <= 0) {
        adjustedMessages.push(`"${item.name}" (${order.title}) đã hết hàng thực tế và được gỡ khỏi giỏ`)
        order.cart.splice(i, 1)
      } else if (item.quantity > maxRemaining) {
        adjustedMessages.push(`"${item.name}" (${order.title}) giảm từ ${item.quantity} xuống ${maxRemaining} theo tồn kho thực tế`)
        item.quantity = maxRemaining
        usedMap.set(Number(item.idCtsp), alreadyUsed + maxRemaining)
      } else {
        usedMap.set(Number(item.idCtsp), alreadyUsed + Number(item.quantity || 0))
      }
    }
  }

  if (adjustedMessages.length > 0) {
    notifyWarning('Cập nhật tồn kho thực tế', adjustedMessages.join('; '))
  }
}

// --- MỤC 1: LƯU GIỮ HÓA ĐƠN CHỜ VÀO LOCALSTORAGE KHI F5 HOẶC CHUYỂN TRANG ---
const savePendingOrdersToStorage = () => {
  if (isRestoringOrders) return
  try {
    const serialized = pendingOrders.value.map(o => ({
      id: o.id,
      code: o.code,
      title: o.title,
      cart: Array.isArray(o.cart) ? o.cart : [],
      customer: o.customer || null,
      isDelivery: Boolean(o.isDelivery),
      deliveryInfo: {
        recipientName: o.deliveryInfo?.recipientName || '',
        recipientPhone: o.deliveryInfo?.recipientPhone || '',
        recipientAddress: o.deliveryInfo?.recipientAddress || '',
        province: o.deliveryInfo?.province || null,
        ward: o.deliveryInfo?.ward || null,
        wardsLoading: false,
        services: Array.isArray(o.deliveryInfo?.services) ? o.deliveryInfo.services : [],
        servicesDistrictId: o.deliveryInfo?.servicesDistrictId || null,
        serviceTypeId: o.deliveryInfo?.serviceTypeId || null,
        shippingFee: Number(o.deliveryInfo?.shippingFee || 0),
        feeLoading: false,
        feeError: '',
        legacy: o.deliveryInfo?.legacy || null,
        saveToCustomerProfile: Boolean(o.deliveryInfo?.saveToCustomerProfile)
      },
      paymentMethod: o.paymentMethod || 'TIEN_MAT',
      selectedVoucher: o.selectedVoucher || null,
      cashGiven: Number(o.cashGiven || 0),
      transferGiven: Number(o.transferGiven || 0),
      note: o.note || ''
    }))
    localStorage.setItem(POS_PENDING_STORAGE_KEY, JSON.stringify(serialized))
    localStorage.setItem(POS_ACTIVE_IDX_STORAGE_KEY, String(activeOrderIndex.value))
  } catch (e) {
    console.warn('Không thể lưu hóa đơn chờ vào localStorage:', e)
  }
}

const restorePendingOrdersFromStorage = () => {
  try {
    const raw = localStorage.getItem(POS_PENDING_STORAGE_KEY)
    if (!raw) return false
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed) || parsed.length === 0) return false

    isRestoringOrders = true
    pendingOrders.value = parsed.slice(0, MAX_ORDERS).map((o, idx) => ({
      id: o.id || (Date.now() + idx),
      code: o.code || generateOrderCode(),
      title: o.title || `Hóa đơn ${idx + 1}`,
      cart: Array.isArray(o.cart) ? o.cart : [],
      customer: o.customer || null,
      isDelivery: Boolean(o.isDelivery),
      deliveryInfo: {
        recipientName: o.deliveryInfo?.recipientName || '',
        recipientPhone: o.deliveryInfo?.recipientPhone || '',
        recipientAddress: o.deliveryInfo?.recipientAddress || '',
        province: o.deliveryInfo?.province || null,
        ward: o.deliveryInfo?.ward || null,
        wardsLoading: false,
        services: Array.isArray(o.deliveryInfo?.services) ? o.deliveryInfo.services : [],
        servicesDistrictId: o.deliveryInfo?.servicesDistrictId || null,
        serviceTypeId: o.deliveryInfo?.serviceTypeId || null,
        shippingFee: Number(o.deliveryInfo?.shippingFee || 0),
        feeLoading: false,
        feeError: '',
        legacy: o.deliveryInfo?.legacy || null,
        saveToCustomerProfile: Boolean(o.deliveryInfo?.saveToCustomerProfile)
      },
      paymentMethod: o.paymentMethod || 'TIEN_MAT',
      selectedVoucher: o.selectedVoucher || null,
      cashGiven: Number(o.cashGiven || 0),
      transferGiven: Number(o.transferGiven || 0),
      note: o.note || ''
    }))

    const savedIdx = Number(localStorage.getItem(POS_ACTIVE_IDX_STORAGE_KEY) || 0)
    activeOrderIndex.value = (savedIdx >= 0 && savedIdx < pendingOrders.value.length) ? savedIdx : 0

    reconcilePendingOrdersWithServerStock()
    recalculateAvailableStocks()
    isRestoringOrders = false
    savePendingOrdersToStorage()
    return true
  } catch (e) {
    isRestoringOrders = false
    console.warn('Không thể khôi phục hóa đơn chờ từ localStorage:', e)
    return false
  }
}

watch([pendingOrders, activeOrderIndex], () => {
  savePendingOrdersToStorage()
}, { deep: true })

const generateOrderCode = () => {
  const d = new Date()
  const timeCode = String(d.getHours()).padStart(2, '0') +
    String(d.getMinutes()).padStart(2, '0') +
    String(d.getSeconds()).padStart(2, '0')
  return `HD${timeCode}`
}

const createNewOrder = (silent = false) => {
  if (pendingOrders.value.length >= MAX_ORDERS) {
    notifyWarning('Đã đạt giới hạn!', `Chỉ được tạo tối đa ${MAX_ORDERS} hóa đơn chờ cùng lúc.`)
    return
  }

  const newIdx = pendingOrders.value.length + 1
  const newOrder = {
    id: Date.now() + Math.floor(Math.random() * 1000),
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
      legacy: null,
      saveToCustomerProfile: false
    },
    paymentMethod: 'TIEN_MAT',
    selectedVoucher: null,
    cashGiven: 0,
    transferGiven: 0,
    note: ''
  }

  pendingOrders.value.push(newOrder)
  activeOrderIndex.value = pendingOrders.value.length - 1
  if (!silent) {
    notifySuccess('Đã thêm hóa đơn chờ mới', newOrder.title, 1200)
  }
}

const selectOrderTab = (index) => {
  activeOrderIndex.value = index
  const ord = pendingOrders.value[index]
  if (ord?.customer?.id && !customerAddressesMap[ord.customer.id]) {
    fetchAndSyncCustomerAddresses(ord.customer, false, false)
  }
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

  pendingOrders.value.splice(index, 1)
  if (pendingOrders.value.length === 0) {
    createNewOrder(true)
  } else if (activeOrderIndex.value >= pendingOrders.value.length) {
    activeOrderIndex.value = Math.max(0, pendingOrders.value.length - 1)
  }
  recalculateAvailableStocks()
  savePendingOrdersToStorage()
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
    createNewOrder(true)
  }

  const targetProd = ensureProductInCatalog(productVariant)
  const currentAvailableStock = targetProd
    ? Number(targetProd.stock !== undefined ? targetProd.stock : (targetProd.soLuong || 0))
    : Number(productVariant.stock !== undefined ? productVariant.stock : (productVariant.soLuong || 0))

  const reservedOther = getReservedInOtherOrders(productVariant.id)

  if (currentAvailableStock < qtyToAdd) {
    const extraMsg = reservedOther > 0
      ? ` (Đang giữ ${reservedOther} sản phẩm ở hóa đơn chờ khác)`
      : ''
    notifyWarning('Tồn kho không đủ!', `Sản phẩm này chỉ còn ${currentAvailableStock} sản phẩm khả dụng${extraMsg}.`)
    return
  }

  const cart = currentOrder.value.cart
  const existingItem = cart.find(i => Number(i.idCtsp) === Number(productVariant.id))
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
      phanTramGiam: productVariant.phanTramGiam || null,
      giaSauGiam: productVariant.giaSauGiam || null,
      quantity: qtyToAdd,
      maxStock: getServerBaseStock(targetProd || productVariant),
      img: productVariant.img || ''
    })
  }

  recalculateAvailableStocks()
  savePendingOrdersToStorage()

  if (!silent) {
    notifySuccess('Thêm thành công', `Đã thêm "${prodName}" (+${qtyToAdd}) vào giỏ hàng`, 1500)
  }
}

const getMaxAllowedForCartItem = (item) => {
  if (!item) return 1
  const targetProd = allProducts.value.find(p => Number(p.id) === Number(item.idCtsp))
  const serverStock = targetProd
    ? getServerBaseStock(targetProd)
    : Number(item.maxStock || (Number(item.quantity || 1) + getRemainingStock(item.idCtsp)))
  const reservedOther = getReservedInOtherOrders(item.idCtsp)
  return Math.max(1, serverStock - reservedOther)
}

const handleCartItemQtyInput = (item, event) => {
  if (!item || !event?.target) return
  const rawVal = event.target.value.trim()
  if (rawVal === '') return
  
  const parsed = parseInt(rawVal, 10)
  if (isNaN(parsed) || parsed < 1) return

  const maxAllowed = getMaxAllowedForCartItem(item)
  if (parsed <= maxAllowed) {
    item.quantity = parsed
    recalculateAvailableStocks()
    savePendingOrdersToStorage()
  }
}

const updateCartItemQty = async (item, rawNewQty, event = null) => {
  if (!item) return
  const raw = event?.target ? event.target.value.trim() : String(rawNewQty).trim()
  let newQty = parseInt(raw, 10)

  // Nếu người dùng nhập 0 hoặc giảm về 0: Xác nhận xóa sản phẩm khỏi giỏ hàng
  if (raw === '0' || newQty === 0) {
    const ok = await notifyConfirm(
      'Xóa sản phẩm khỏi giỏ?',
      `Số lượng sản phẩm bằng 0. Bạn có muốn xóa "${item.name || 'sản phẩm này'}" khỏi giỏ hàng không?`,
      'Xóa ngay',
      'Giữ lại (số lượng 1)'
    )
    if (ok) {
      await removeCartItem(item, true)
      return
    } else {
      newQty = 1
      if (event?.target) event.target.value = 1
      item.quantity = 1
      recalculateAvailableStocks()
      savePendingOrdersToStorage()
      return
    }
  }

  // Nếu ô trống (vừa xóa trắng để gõ số mới rồi click ra ngoài) hoặc không hợp lệ: khôi phục về 1
  if (isNaN(newQty) || newQty < 1) {
    newQty = 1
    if (event?.target) event.target.value = 1
    item.quantity = 1
    recalculateAvailableStocks()
    savePendingOrdersToStorage()
    return
  }

  const maxAllowedForThisOrder = getMaxAllowedForCartItem(item)
  const reservedOther = getReservedInOtherOrders(item.idCtsp)

  if (newQty > maxAllowedForThisOrder) {
    const extraMsg = reservedOther > 0
      ? ` (đang giữ ${reservedOther} sản phẩm ở hóa đơn chờ khác)`
      : ''
    notifyWarning(
      'Vượt quá tồn kho khả dụng!',
      `Tối đa có thể đặt cho đơn này là ${maxAllowedForThisOrder} sản phẩm${extraMsg}.`
    )
    newQty = Math.max(1, maxAllowedForThisOrder)
  }

  item.quantity = newQty
  if (event?.target) event.target.value = newQty
  recalculateAvailableStocks()
  savePendingOrdersToStorage()
}

const removeCartItem = async (item, skipConfirm = false) => {
  if (!currentOrder.value) return
  const prodName = item.name || 'sản phẩm này'
  if (!skipConfirm) {
    const ok = await notifyConfirm(
      'Xác nhận xóa sản phẩm?',
      `Bạn có chắc chắn muốn xóa "${prodName}" khỏi giỏ hàng không?`,
      'Xóa ngay',
      'Giữ lại'
    )
    if (!ok) return
  }

  const idx = currentOrder.value.cart.findIndex(i => Number(i.idCtsp) === Number(item.idCtsp))
  if (idx !== -1) {
    currentOrder.value.cart.splice(idx, 1)
    recalculateAvailableStocks()
    savePendingOrdersToStorage()
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
    currentOrder.value.cart = []
    recalculateAvailableStocks()
    savePendingOrdersToStorage()
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
          found = ensureProductInCatalog(data.content[0]) || data.content[0]
        }
      }
    } catch { }
  }

  if (found) {
    const stock = getRemainingStock(found.id)
    if (stock <= 0) {
      notifyWarning('Hết hàng', `Sản phẩm "${found.tenSp || found.tenSanPham}" đã hết tồn kho khả dụng!`)
      return
    }
    addToCart(found, 1)
    barcodeSearchText.value = ''
  } else {
    notifyWarning('Không tìm thấy', `Không tìm thấy sản phẩm nào khớp với mã "${code}"`)
  }
}

const getRemainingStock = (idCtsp) => {
  const prod = allProducts.value.find(p => Number(p.id) === Number(idCtsp))
  if (!prod) return 0
  const baseStock = getServerBaseStock(prod)
  const reserved = getReservedQtyAcrossOrders(idCtsp)
  return Math.max(0, baseStock - reserved)
}

const setQuickCash = (amount) => {
  if (!currentOrder.value) return
  if (amount === 'EXACT') {
    currentOrder.value.cashGiven = finalTotal.value
  } else {
    currentOrder.value.cashGiven = Number(amount)
  }
}
const smartQuickCashOptions = computed(() => {
  const total = finalTotal.value || 0
  if (total <= 0) return [50000, 100000, 200000, 500000]
  const options = []
  if (total >= 500000) {
    const round100k = Math.ceil(total / 100000) * 100000
    if (round100k > total) options.push(round100k)
    const round500k = Math.ceil(total / 500000) * 500000
    if (round500k > total && !options.includes(round500k)) options.push(round500k)
    const round1m = Math.ceil(total / 1000000) * 1000000
    if (round1m > total && !options.includes(round1m)) options.push(round1m)
    const nextStep = (round1m > 0 ? round1m : total) + 1000000
    if (!options.includes(nextStep) && options.length < 4) options.push(nextStep)
  } else {
    if (total < 100000) options.push(100000)
    if (total < 200000) options.push(200000)
    if (total < 500000) options.push(500000)
    if (total < 1000000) options.push(1000000)
  }
  return options.slice(0, 4)
})


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

const customerAddressesMap = reactive({})
const isSavingCustomerAddress = ref(false)

const normalizePlaceName = (str) => {
  if (!str) return ''
  return String(str)
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/\b(tinh|thanh pho|tp\.?|phuong|xa|thi tran|p\.?|x\.?|tt\.?)\b/g, ' ')
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const isPlaceholderCounterAddress = (addr) => {
  if (!addr) return true
  const tp = String(addr.thanhPho || addr.tinhThanhPho || '').trim().toLowerCase()
  const dc = String(addr.diaChiCuThe || '').trim().toLowerCase()
  return !tp || tp === 'tại quầy' || dc === 'mua hàng trực tiếp tại quầy' || dc === 'tại quầy'
}

const matchProvinceByName = (name) => {
  const target = normalizePlaceName(name)
  if (!target || !Array.isArray(provincesList.value)) return null
  return (
    provincesList.value.find(p => normalizePlaceName(p.name) === target) ||
    provincesList.value.find(p => {
      const pn = normalizePlaceName(p.name)
      return pn && (pn.includes(target) || target.includes(pn))
    }) ||
    null
  )
}

const matchWardByName = (name) => {
  const target = normalizePlaceName(name)
  if (!target || !Array.isArray(wardsList.value)) return null
  return (
    wardsList.value.find(w => normalizePlaceName(w.name) === target) ||
    wardsList.value.find(w => {
      const wn = normalizePlaceName(w.name)
      return wn && (wn.includes(target) || target.includes(wn))
    }) ||
    null
  )
}

const currentCustomerAddressEntry = computed(() => {
  const custId = currentOrder.value?.customer?.id
  if (!custId) return null
  return customerAddressesMap[custId] || null
})

const currentCustomerSavedAddresses = computed(() => {
  return currentCustomerAddressEntry.value?.realAddresses || []
})

const hasCustomerSavedAddress = computed(() => {
  return currentCustomerSavedAddresses.value.length > 0
})

const isCurrentDeliveryAddressComplete = computed(() => {
  const d = currentOrder.value?.deliveryInfo
  if (!d) return false
  return Boolean(d.province?.name && d.ward?.name && d.recipientAddress?.trim())
})

const isCurrentDeliveryAddressSaved = computed(() => {
  const d = currentOrder.value?.deliveryInfo
  if (!d || !isCurrentDeliveryAddressComplete.value) return false
  const curProv = normalizePlaceName(d.province?.name)
  const curWard = normalizePlaceName(d.ward?.name)
  const curStreet = String(d.recipientAddress || '').trim().toLowerCase()

  return currentCustomerSavedAddresses.value.some(a => {
    const savedProv = normalizePlaceName(a.thanhPho || a.tinhThanhPho)
    const savedWard = normalizePlaceName(a.phuong)
    const savedStreet = String(a.diaChiCuThe || '').trim().toLowerCase()
    return savedProv === curProv && savedWard === curWard && savedStreet === curStreet
  })
})

const applySavedAddressToDeliveryForm = async (addrObj, cust = null, showToast = false) => {
  if (!currentOrder.value?.deliveryInfo || !addrObj) return
  const d = currentOrder.value.deliveryInfo
  const activeCust = cust || currentOrder.value.customer

  if (activeCust) {
    d.recipientName = activeCust.ten || activeCust.tenKhachHang || d.recipientName || ''
    d.recipientPhone = activeCust.soDienThoai || d.recipientPhone || ''
  }

  d.recipientAddress = addrObj.diaChiCuThe || ''

  if (!provincesList.value || provincesList.value.length === 0) {
    await loadProvinces()
  }

  const matchedProv = matchProvinceByName(addrObj.thanhPho || addrObj.tinhThanhPho)
  if (matchedProv) {
    d.province = matchedProv
    await onProvinceChange()
    const matchedWard = matchWardByName(addrObj.phuong)
    if (matchedWard) {
      d.ward = matchedWard
      await tinhPhiVanChuyen()
    }
  }

  d.saveToCustomerProfile = false

  if (showToast) {
    const fullStr = [addrObj.diaChiCuThe, addrObj.phuong, addrObj.thanhPho || addrObj.tinhThanhPho].filter(Boolean).join(', ')
    notifySuccess('Đã tự động điền địa chỉ', fullStr || 'Đã điền thông tin người nhận từ hồ sơ khách hàng!', 1800)
  }
}

const fetchAndSyncCustomerAddresses = async (cust, autoFillIfDelivery = true, showToast = false) => {
  if (!cust || !cust.id) return null
  try {
    const res = await http.get(`/khach-hang/${cust.id}/dia-chi`)
    const list = Array.isArray(res.data) ? res.data : []
    const realAddresses = list.filter(a => a.trangThai !== false && !isPlaceholderCounterAddress(a))
    const placeholderAddress = list.find(a => isPlaceholderCounterAddress(a)) || null
    const defaultAddress = realAddresses.find(a => a.macDinh) || realAddresses[0] || null

    customerAddressesMap[cust.id] = {
      realAddresses,
      placeholderAddress,
      defaultAddress
    }

    if (defaultAddress) {
      cust.diaChi = defaultAddress.diaChiCuThe || ''
      cust.tinhThanhPho = defaultAddress.thanhPho || ''
      cust.phuong = defaultAddress.phuong || ''
      cust.defaultAddress = defaultAddress
    }

    if (currentOrder.value?.isDelivery && autoFillIfDelivery && currentOrder.value?.customer?.id === cust.id) {
      const d = currentOrder.value.deliveryInfo
      d.recipientName = cust.ten || cust.tenKhachHang || d.recipientName || ''
      d.recipientPhone = cust.soDienThoai || d.recipientPhone || ''

      if (defaultAddress) {
        await applySavedAddressToDeliveryForm(defaultAddress, cust, showToast)
      } else {
        d.saveToCustomerProfile = true
        if (showToast) {
          notifyWarning(
            'Khách chưa có địa chỉ lưu sẵn',
            'Đã điền Họ tên & SĐT. Vui lòng chọn Tỉnh/Phường/Số nhà — hệ thống sẽ lưu vào hồ sơ khách cho lần mua sau!'
          )
        }
      }
    }
    return customerAddressesMap[cust.id]
  } catch (err) {
    console.warn('Không thể tải sổ địa chỉ khách hàng:', err)
    return null
  }
}

const saveDeliveryAddressToCustomerProfile = async (silent = false) => {
  const cust = currentOrder.value?.customer
  const d = currentOrder.value?.deliveryInfo
  if (!cust || !cust.id || !d) return false

  if (!d.province?.name || !d.ward?.name || !d.recipientAddress?.trim()) {
    if (!silent) {
      notifyWarning('Thiếu địa chỉ', 'Vui lòng chọn đầy đủ Tỉnh/Thành phố, Phường/Xã và nhập Địa chỉ cụ thể trước khi lưu vào hồ sơ khách hàng!')
    }
    return false
  }

  isSavingCustomerAddress.value = true
  try {
    let entry = customerAddressesMap[cust.id]
    if (!entry) {
      entry = await fetchAndSyncCustomerAddresses(cust, false, false)
    }

    const curProv = normalizePlaceName(d.province.name)
    const curWard = normalizePlaceName(d.ward.name)
    const curStreet = d.recipientAddress.trim().toLowerCase()

    const existingSame = (entry?.realAddresses || []).find(a =>
      normalizePlaceName(a.thanhPho || a.tinhThanhPho) === curProv &&
      normalizePlaceName(a.phuong) === curWard &&
      String(a.diaChiCuThe || '').trim().toLowerCase() === curStreet
    )

    const payload = {
      tenDiaChi: 'Địa chỉ giao hàng',
      tinhThanhPho: d.province.name,
      phuong: d.ward.name,
      diaChiCuThe: d.recipientAddress.trim(),
      macDinh: true,
      trangThai: true
    }

    if (existingSame) {
      if (!existingSame.macDinh) {
        await http.put(`/khach-hang/${cust.id}/dia-chi/${existingSame.id}/mac-dinh`)
      }
    } else if (entry?.placeholderAddress && (!entry.realAddresses || entry.realAddresses.length === 0)) {
      await http.put(`/khach-hang/${cust.id}/dia-chi/${entry.placeholderAddress.id}`, payload)
    } else {
      await http.post(`/khach-hang/${cust.id}/dia-chi`, payload)
    }

    await fetchAndSyncCustomerAddresses(cust, false, false)
    d.saveToCustomerProfile = false

    if (!silent) {
      notifySuccess(
        'Đã lưu vào hồ sơ khách hàng!',
        `Địa chỉ "${d.recipientAddress.trim()}, ${d.ward.name}, ${d.province.name}" đã được đặt làm mặc định cho khách ${cust.ten || cust.tenKhachHang}.`,
        2200
      )
    }
    return true
  } catch (err) {
    console.error('Lỗi lưu địa chỉ vào hồ sơ khách hàng:', err)
    if (!silent) {
      notifyError('Không thể lưu địa chỉ', err.message || 'Đã xảy ra lỗi khi lưu địa chỉ vào hồ sơ khách hàng.')
    }
    return false
  } finally {
    isSavingCustomerAddress.value = false
  }
}

const selectCustomer = async (cust) => {
  if (!currentOrder.value) createNewOrder()
  currentOrder.value.customer = cust
  customerSearchQuery.value = ''
  showCustomerDropdown.value = false
  if (cust) {
    if (currentOrder.value.isDelivery) {
      currentOrder.value.deliveryInfo.recipientName = cust.ten || cust.tenKhachHang || ''
      currentOrder.value.deliveryInfo.recipientPhone = cust.soDienThoai || ''
    }
    await fetchAndSyncCustomerAddresses(cust, true, false)
  }
}

const fillDeliveryFromCustomer = async () => {
  if (!currentOrder.value?.customer || !currentOrder.value?.deliveryInfo) return
  const cust = currentOrder.value.customer
  currentOrder.value.deliveryInfo.recipientName = cust.ten || cust.tenKhachHang || ''
  currentOrder.value.deliveryInfo.recipientPhone = cust.soDienThoai || ''
  await fetchAndSyncCustomerAddresses(cust, true, true)
}

const clearDeliveryForm = () => {
  const d = currentOrder.value?.deliveryInfo
  if (!d) return
  d.recipientName = ''
  d.recipientPhone = ''
  d.province = null
  d.ward = null
  d.recipientAddress = ''
  d.shippingFee = 0
  d.legacy = null
  d.feeError = ''
  d.services = []
  d.servicesDistrictId = null
  d.serviceTypeId = null
  d.saveToCustomerProfile = !hasCustomerSavedAddress.value
  wardsList.value = []
}

watch(() => currentOrder.value?.isDelivery, async (enabled) => {
  if (enabled && currentOrder.value?.customer && currentOrder.value?.deliveryInfo) {
    const cust = currentOrder.value.customer
    const d = currentOrder.value.deliveryInfo
    if (!d.recipientName) d.recipientName = cust.ten || cust.tenKhachHang || ''
    if (!d.recipientPhone) d.recipientPhone = cust.soDienThoai || ''
    const cached = customerAddressesMap[cust.id]
    if (cached?.defaultAddress && !d.province) {
      await applySavedAddressToDeliveryForm(cached.defaultAddress, cust, false)
    } else if (!cached) {
      await fetchAndSyncCustomerAddresses(cust, true, false)
    } else if (!cached.defaultAddress) {
      d.saveToCustomerProfile = true
    }
  }
})

watch(
  [
    () => currentOrder.value?.deliveryInfo?.province,
    () => currentOrder.value?.deliveryInfo?.ward,
    () => currentOrder.value?.deliveryInfo?.recipientAddress
  ],
  () => {
    const d = currentOrder.value?.deliveryInfo
    if (!d || !currentOrder.value?.customer?.id) return
    if (isCurrentDeliveryAddressComplete.value && !isCurrentDeliveryAddressSaved.value && !hasCustomerSavedAddress.value) {
      d.saveToCustomerProfile = true
    }
  }
)

const removeSelectedCustomer = () => {
  if (currentOrder.value) {
    currentOrder.value.customer = null
    if (currentOrder.value.deliveryInfo) {
      currentOrder.value.deliveryInfo.saveToCustomerProfile = false
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
    const rawAddress = newCustomerForm.diaChi?.trim() || ''
    const fallbackEmail = newCustomerForm.email?.trim() || `khachle.${phone}@polyshoes.vn`
    const payload = {
      tenKhachHang: newCustomerForm.tenKhachHang.trim(),
      soDienThoai: phone.slice(0, 10),
      email: fallbackEmail,
      gioiTinh: newCustomerForm.gioiTinh || 'Nam',
      ngaySinh: newCustomerForm.ngaySinh || undefined,
      tinhThanhPho: 'Tại quầy',
      phuong: 'Tại quầy',
      diaChiCuThe: rawAddress || 'Mua hàng trực tiếp tại quầy',
      trangThai: true
    }

    const res = await http.post('/khach-hang', payload)
    const newCust = res.data
    const createdCust = {
      id: newCust.id,
      ten: newCust.tenKhachHang,
      soDienThoai: newCust.soDienThoai,
      email: newCust.email,
      diaChi: rawAddress || (newCust.diaChiMacDinh ? newCust.diaChiMacDinh.diaChiCuThe : '')
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
  // Lắng nghe pendingOrders để kích hoạt lại computed ngay khi thêm/sửa/xóa sản phẩm trong giỏ
  const _cartTrigger = pendingOrders.value.map(o => (o.cart || []).map(i => `${i.idCtsp}:${i.quantity}`).join(',')).join(';')

  // Tự động ẩn những sản phẩm có số lượng tồn kho khả dụng = 0 (khi đã thêm hết vào giỏ hàng)
  let list = (allProducts.value || []).filter(p => {
    return getRemainingStock(p.id) > 0
  })
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
  validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: true })
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

const quickAddSingleProductFromModal = (product) => {
  const stock = product.stock !== undefined ? product.stock : (product.soLuong || 0)
  if (stock <= 0) {
    notifyWarning('Hết hàng', 'Sản phẩm này hiện đã hết tồn kho!')
    return
  }
  addToCart(product, 1, false)
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

const getDiscountPercentBadge = (item) => {
  if (!item) return ''
  // 1. Kiểm tra trường phanTramGiam dạng số > 0 (từ dot_giam_gia)
  if (item.phanTramGiam !== null && item.phanTramGiam !== undefined && item.phanTramGiam !== '') {
    const p = parseFloat(String(item.phanTramGiam).replace(/[^0-9.-]+/g, ''))
    if (!isNaN(p) && p > 0) {
      return `-${Math.round(p)}%`
    }
  }
  // 2. Kiểm tra trường discount dạng chuỗi vd: "-20%" hoặc "20%"
  if (item.discount && item.discount !== '-') {
    const s = String(item.discount).trim()
    const num = parseFloat(s.replace(/[^0-9.-]+/g, ''))
    if (!isNaN(num) && num > 0) {
      return `-${Math.round(Math.abs(num))}%`
    }
  }
  // 3. So sánh giá bán gốc (price / originalPrice) và giá sau giảm (giaSauGiam / price)
  const base = Number(item.originalPrice || item.price || item.giaBan || 0)
  const after = Number(item.giaSauGiam || (item.originalPrice && item.price ? item.price : 0))
  if (base > 0 && after > 0 && after < base) {
    const pct = Math.round(((base - after) / base) * 100)
    if (pct > 0) return `-${pct}%`
  }
  return ''
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
          found = ensureProductInCatalog(data.content[0]) || data.content[0]
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

    const stock = getRemainingStock(found.id)
    if (stock <= 0) {
      notifyWarning('Hết hàng', `Sản phẩm "${found.tenSp || found.tenSanPham}" đã hết tồn kho khả dụng!`)
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

  const targetNew = ensureProductInCatalog(newVariant)
  const availableStock = targetNew
    ? Number(targetNew.stock !== undefined ? targetNew.stock : (targetNew.soLuong || 0))
    : Number(newVariant.stock !== undefined ? newVariant.stock : (newVariant.soLuong || 0))

  const reservedOther = getReservedInOtherOrders(newVariant.id)

  if (availableStock < currentItem.quantity) {
    const extraMsg = reservedOther > 0 ? ` (đang giữ ${reservedOther} sản phẩm ở hóa đơn chờ khác)` : ''
    notifyWarning('Tồn kho không đủ!', `Biến thể này chỉ còn ${availableStock} sản phẩm khả dụng${extraMsg}, không đủ số lượng ${currentItem.quantity} trong giỏ.`)
    return
  }

  const newEffectivePrice = getEffectivePrice(newVariant)
  const newSize = newVariant.size || newVariant.tenKichCo || '---'
  const newColor = newVariant.color || newVariant.tenMau || '---'

  const existingInCart = currentOrder.value.cart.find(i => Number(i.idCtsp) === Number(newVariant.id) && i !== currentItem)
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

  recalculateAvailableStocks()
  savePendingOrdersToStorage()

  isSwapModalOpen.value = false
  swapCartItem.value = null
  selectedNewVariant.value = null
}

const defaultStoreBank = {
  id: 'MB',
  name: 'MB Bank',
  fullName: 'Ngân hàng TMCP Quân Đội (MB Bank)',
  accountNo: '0987654321',
  accountName: 'POLYSHOES STORE',
  color: '#004b8d'
}

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
  const bank = defaultStoreBank

  vietQrNetworkUrl.value = `https://img.vietqr.io/image/${bank.id}-${bank.accountNo}-compact2.png?amount=${amount}&addInfo=${encodeURIComponent(memo)}&accountName=${encodeURIComponent(bank.accountName)}`

  try {
    const qrString = `2|99|${bank.accountNo}|${bank.accountName}|${memo}|0|0|${amount}|${memo}`
    bankQrDataUrl.value = await QRCode.toDataURL(qrString, { width: 300, margin: 1 })
  } catch (err) {
    console.error('Lỗi sinh QR code offline:', err)
  }

  isBankQrModalOpen.value = true
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

const isAutoCheckoutEnabled = ref(false)
const autoCheckoutCountdown = ref(0)
let autoCheckoutTimer = null

const stopAutoCheckoutCountdown = () => {
  if (autoCheckoutTimer) {
    clearInterval(autoCheckoutTimer)
    autoCheckoutTimer = null
  }
  autoCheckoutCountdown.value = 0
}

const startAutoCheckoutCountdown = () => {
  stopAutoCheckoutCountdown()
  if (!isAutoCheckoutEnabled.value) return
  autoCheckoutCountdown.value = 5
  autoCheckoutTimer = setInterval(() => {
    if (autoCheckoutCountdown.value > 1) {
      autoCheckoutCountdown.value--
    } else {
      stopAutoCheckoutCountdown()
      confirmBankPaymentReceived()
    }
  }, 1000)
}

const toggleAutoCheckout = () => {
  isAutoCheckoutEnabled.value = !isAutoCheckoutEnabled.value
  if (isAutoCheckoutEnabled.value) {
    startAutoCheckoutCountdown()
  } else {
    stopAutoCheckoutCountdown()
  }
}

const confirmBankPaymentReceived = async () => {
  if (!currentOrder.value || isProcessingCheckout.value) return
  stopAutoCheckoutCountdown()

  if (currentOrder.value.paymentMethod === 'KET_HOP') {
    currentOrder.value.transferGiven = bankTransferAmount.value
  } else {
    currentOrder.value.paymentMethod = 'CHUYEN_KHOAN'
  }

  playPaymentSuccessSound()
  isPaymentSuccessAnim.value = true

  setTimeout(async () => {
    isPaymentSuccessAnim.value = false
    isBankQrModalOpen.value = false
    // Tự động hoàn tất hóa đơn ngay lập tức vào database, không cần ấn nút thanh toán nữa
    await handleCheckout({ skipConfirm: true })
  }, 600)
}

watch(isBankQrModalOpen, (isOpen) => {
  if (!isOpen) {
    stopAutoCheckoutCountdown()
  } else if (isAutoCheckoutEnabled.value) {
    startAutoCheckoutCountdown()
  }
})

watch(() => currentOrder.value?.paymentMethod, (newMethod) => {
  if (newMethod === 'CHUYEN_KHOAN' && finalTotal.value > 0) {
    generateBankTransferQr()
  }
})

const provincesList = ref([])
const wardsList = ref([])

const loadProvinces = async () => {
  // 1. Ưu tiên gọi API Địa giới hành chính mới nhất 2026 (v2 - 2 cấp Tỉnh/Thành phố -> Phường/Xã)
  try {
    const res = await fetch('https://provinces.open-api.vn/api/v2/p/')
    if (res.ok) {
      const data = await res.json()
      if (Array.isArray(data) && data.length > 0) {
        provincesList.value = data.map(p => ({ id: p.code, name: p.name }))
        return
      }
    }
  } catch (err) {
    console.warn('Open API v2 (2026) failed, checking backend /shipping/provinces...', err)
  }

  // 2. Fallback sang Backend /shipping/provinces (chỉ nhận nếu đã cập nhật danh sách tỉnh mới <= 35 tỉnh)
  try {
    const list = await getProvinces()
    if (Array.isArray(list) && list.length > 0 && list.length <= 35) {
      provincesList.value = list
      return
    }
  } catch (e) {
    console.warn('Backend /shipping/provinces failed, using local 2026 province list...', e)
  }

  // 3. Fallback danh sách tỉnh/thành phố mới 2026 lưu cục bộ
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
    // 1. Ưu tiên lấy danh sách Phường/Xã trực thuộc Tỉnh/Thành phố theo chuẩn 2 cấp mới 2026 (API v2)
    try {
      const res = await fetch(`https://provinces.open-api.vn/api/v2/p/${d.province.id}?depth=2`)
      if (res.ok) {
        const pData = await res.json()
        if (pData && Array.isArray(pData.wards) && pData.wards.length > 0) {
          wardsList.value = pData.wards.map(w => ({
            id: w.code,
            name: w.name
          }))
          d.wardsLoading = false
          return
        }
      }
    } catch {
    }

    // 2. Fallback sang Backend /shipping/wards
    try {
      const list = await getWards(d.province.id)
      const isDummy = Array.isArray(list) && list.length === 2 && list[0]?.id === 1 && list[1]?.id === 2
      if (Array.isArray(list) && list.length > 0 && !isDummy) {
        wardsList.value = list.map(w => ({
          id: w.id,
          name: String(w.name || '').replace(/\s*\([^)]*\)\s*$/, '').trim() || w.name
        }))
        d.wardsLoading = false
        return
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

watch(() => currentOrder.value?.cart?.length, () => {
  const d = currentOrder.value?.deliveryInfo
  if (d && d.province && d.ward && !d.feeLoading) {
    tinhPhiVanChuyen()
  }
})


const isReceiptModalOpen = ref(false)
const completedInvoiceData = ref(null)
const isProcessingCheckout = ref(false)

// --- MỤC 5: LỊCH SỬ ĐƠN VỪA BÁN TRONG CA & IN LẠI HÓA ĐƠN NHANH ---
const recentPosInvoices = ref([])
const isRecentInvoicesModalOpen = ref(false)
const recentInvoiceSearch = ref('')

const loadRecentInvoicesFromStorage = () => {
  try {
    const raw = localStorage.getItem(POS_RECENT_INVOICES_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      recentPosInvoices.value = parsed
    }
  } catch (e) {
    console.warn('Không thể đọc lịch sử đơn vừa bán:', e)
  }
}

const saveRecentInvoiceToStorage = (invoiceRecord) => {
  if (!invoiceRecord) return
  try {
    const list = [
      invoiceRecord,
      ...recentPosInvoices.value.filter(i => i.code !== invoiceRecord.code)
    ].slice(0, 30)
    recentPosInvoices.value = list
    localStorage.setItem(POS_RECENT_INVOICES_KEY, JSON.stringify(list))
  } catch (e) {
    console.warn('Không thể lưu lịch sử đơn vừa bán:', e)
  }
}

const filteredRecentInvoices = computed(() => {
  const q = recentInvoiceSearch.value.trim().toLowerCase()
  if (!q) return recentPosInvoices.value
  return recentPosInvoices.value.filter(inv =>
    (inv.code && inv.code.toLowerCase().includes(q)) ||
    (inv.customerName && inv.customerName.toLowerCase().includes(q)) ||
    (inv.customerPhone && inv.customerPhone.toLowerCase().includes(q))
  )
})

const openRecentInvoicesModal = () => {
  recentInvoiceSearch.value = ''
  isRecentInvoicesModalOpen.value = true
}

const viewRecentInvoiceReceipt = (inv) => {
  if (!inv) return
  completedInvoiceData.value = inv
  isRecentInvoicesModalOpen.value = false
  isReceiptModalOpen.value = true
}

const reprintRecentInvoice = async (inv) => {
  if (!inv) return
  completedInvoiceData.value = inv
  isRecentInvoicesModalOpen.value = false
  isReceiptModalOpen.value = true
  await nextTick()
  setTimeout(() => {
    printReceipt()
  }, 80)
}

const clearRecentInvoicesHistory = async () => {
  if (recentPosInvoices.value.length === 0) return
  const ok = await notifyConfirm(
    'Xóa lịch sử đơn vừa bán?',
    'Xóa danh sách lưu tạm các đơn vừa bán trong ca trên máy này (dữ liệu hóa đơn trên hệ thống vẫn được giữ nguyên)?',
    'Xóa danh sách',
    'Hủy'
  )
  if (!ok) return
  recentPosInvoices.value = []
  localStorage.removeItem(POS_RECENT_INVOICES_KEY)
  notifySuccess('Đã xóa danh sách đơn vừa bán trong ca')
}

const handleCancelOrder = async () => {
  if (!currentOrder.value) return
  const ok = await notifyConfirm(
    'Hủy đơn hàng này?',
    'Toàn bộ sản phẩm trong giỏ hàng hiện tại sẽ được hoàn trả lại tồn kho!',
    'Đồng ý hủy',
    'Không'
  )
  if (ok) {
    currentOrder.value.cart = []
    currentOrder.value.customer = null
    currentOrder.value.cashGiven = 0
    currentOrder.value.transferGiven = 0
    currentOrder.value.note = ''
    recalculateAvailableStocks()
    savePendingOrdersToStorage()
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
  if (methodType === 'KET_HOP') {
    const cash = Number(currentOrder.value?.cashGiven || 0)
    const transfer = Number(currentOrder.value?.transferGiven || 0)
    if (transfer > cash) {
      const found = allPaymentMethods.value.find(p => p.ten && p.ten.toLowerCase().includes('chuyển khoản'))
      return found ? found.id : (allPaymentMethods.value[1]?.id || 2)
    }
    const found = allPaymentMethods.value.find(p => p.ten && p.ten.toLowerCase().includes('tiền mặt'))
    return found ? found.id : (allPaymentMethods.value[0]?.id || 1)
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
    return `${day}/${month}/${year}`
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

const normalizeDiscountStr = (val) => {
  if (!val) return ''
  const s = String(val).trim()
  if (s === '-' || s === '0%' || s === '-0%') return ''
  return s
}

const syncEmployeeListAndCheckRole = (empList, isInitial = false) => {
  const changes = []
  let abortedByStaffLock = false

  if (!Array.isArray(empList) || empList.length === 0) {
    return { abortedByStaffLock, changes }
  }

  const loggedId = staffUserRef.value?.idNhanVien ? Number(staffUserRef.value.idNhanVien) : null
  const loggedAccount = staffUserRef.value?.tenTaiKhoan || ''

  for (const emp of empList) {
    const empId = Number(emp.id)
    const newRole = String(emp.maVaiTro || 'STAFF').toUpperCase()
    const newRoleLabel = newRole === 'ADMIN' ? 'Admin' : 'Nhân viên'
    const newActive = emp.trangThai !== false
    const empName = emp.tenTaiKhoan || emp.ten || `NV#${empId}`

    const prev = staffSnapshotMap.get(empId)
    const isLoggedEmp = (loggedId && empId === loggedId) || (loggedAccount && empName === loggedAccount)
    const isSelectedPosEmp = selectedStaffId.value && Number(selectedStaffId.value) === empId

    if (!isInitial && prev) {
      // Kiểm tra khóa/mở khóa tài khoản nhân viên
      if (prev.trangThai !== newActive) {
        if (!newActive) {
          if (isLoggedEmp) {
            abortedByStaffLock = true
            notifyError(
              'Tài khoản nhân viên đã bị khóa!',
              `Tài khoản "${empName}" vừa bị Quản trị viên khóa. Không thể tiếp tục bán hàng, hệ thống sẽ đăng xuất!`,
              5000
            )
            setTimeout(() => {
              dangXuat()
              window.location.href = '/dang-nhap'
            }, 1500)
          } else if (isSelectedPosEmp) {
            const fallbackActive = empList.find(e => e.trangThai !== false && Number(e.id) !== empId)
            selectedStaffId.value = fallbackActive ? fallbackActive.id : null
            changes.push(`Nhân viên phụ trách "${empName}" vừa bị khóa tài khoản (đã tự động chuyển sang ${fallbackActive?.tenTaiKhoan || 'nhân viên khác'})`)
          } else {
            changes.push(`Tài khoản nhân viên "${empName}" vừa bị khóa trên hệ thống`)
          }
        } else {
          changes.push(`Tài khoản nhân viên "${empName}" vừa được mở khóa hoạt động trở lại`)
        }
      }

      // Kiểm tra chuyển quyền nhân viên (Admin <-> Nhân viên)
      if (prev.maVaiTro && prev.maVaiTro !== newRole) {
        const oldRoleLabel = prev.maVaiTro === 'ADMIN' ? 'Admin' : 'Nhân viên'
        if (isLoggedEmp || !loggedId) {
          datVaiTro(newRole)
          try {
            const rawU = localStorage.getItem('polyshoes_user')
            if (rawU) {
              const parsedU = JSON.parse(rawU)
              parsedU.vaiTro = newRole
              localStorage.setItem('polyshoes_user', JSON.stringify(parsedU))
              staffUserRef.value = parsedU
            }
            window.dispatchEvent(new Event('polyshoes-user-updated'))
          } catch {}
        }
        changes.push(`Chuyển quyền nhân viên: Tài khoản "${empName}" vừa thay đổi vai trò từ [${oldRoleLabel}] sang [${newRoleLabel}]`)
      }
    }

    // Nếu lúc đầu đăng nhập quyền trong localStorage bị lệch với DB thì đồng bộ lại
    if (isLoggedEmp && staffUserRef.value?.vaiTro && String(staffUserRef.value.vaiTro).toUpperCase() !== newRole) {
      const oldRoleLabel = String(staffUserRef.value.vaiTro).toUpperCase() === 'ADMIN' ? 'Admin' : 'Nhân viên'
      datVaiTro(newRole)
      try {
        const rawU = localStorage.getItem('polyshoes_user')
        if (rawU) {
          const parsedU = JSON.parse(rawU)
          parsedU.vaiTro = newRole
          localStorage.setItem('polyshoes_user', JSON.stringify(parsedU))
          staffUserRef.value = parsedU
        }
        window.dispatchEvent(new Event('polyshoes-user-updated'))
      } catch {}
      if (!isInitial && !changes.some(c => c.includes(empName))) {
        changes.push(`Chuyển quyền nhân viên: Tài khoản "${empName}" vừa thay đổi vai trò từ [${oldRoleLabel}] sang [${newRoleLabel}]`)
      }
    }

    staffSnapshotMap.set(empId, {
      maVaiTro: newRole,
      tenVaiTro: newRoleLabel,
      trangThai: newActive,
      tenTaiKhoan: empName
    })
  }

  allEmployeesFull.value = empList
  const activeEmps = empList.filter(e => e.trangThai !== false)
  if (activeEmps.length > 0) {
    allEmployees.value = activeEmps.map(e => ({ id: e.id, ten: e.tenTaiKhoan || e.ten, maVaiTro: e.maVaiTro, trangThai: e.trangThai }))
  }

  if (!selectedStaffId.value || !activeEmps.some(e => Number(e.id) === Number(selectedStaffId.value))) {
    const matchLogged = activeEmps.find(e =>
      (loggedId && Number(e.id) === loggedId) ||
      (loggedAccount && (e.tenTaiKhoan === loggedAccount || e.ten === loggedAccount))
    )
    selectedStaffId.value = matchLogged ? matchLogged.id : (activeEmps[0]?.id || null)
  }

  saveStaffSnapshotToStorage()
  return { abortedByStaffLock, changes }
}

const withRequestTimeout = (promise, ms = 4000) => {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error('POS sync timeout')), ms))
  ])
}

// --- KIỂM TRA & ĐỒNG BỘ 4 CASE KHÓ BÁN HÀNG TẠI QUẦY ---
// 1. Đổi giá sản phẩm (giá bán gốc thay đổi hoặc ngừng kinh doanh)
// 2. Sửa voucher (hết hạn, hết hạn do edit, khóa voucher, hết lượt, thay đổi giá trị giảm / đơn tối thiểu)
// 3. Chuyển quyền nhân viên (đổi vai trò Admin <-> Nhân viên hoặc khóa tài khoản nhân viên)
// 4. Đợt giảm giá thay đổi (bắt đầu / kết thúc / tắt đợt giảm giá / thay đổi % giảm)
const validateAndSyncHardCases = async (options = { isBeforeCheckout: false, silentToast: false }) => {
  if (isSyncingHardCases) return { abortedByStaffLock: false, changes: [] }
  if (!options.isBeforeCheckout) {
    const hasOpenModalOrDialog =
      isProductModalOpen.value ||
      isVoucherModalOpen.value ||
      isQrModalOpen.value ||
      isAddCustomerModalOpen.value ||
      isSwapModalOpen.value ||
      isReceiptModalOpen.value ||
      isRecentInvoicesModalOpen.value ||
      isProcessingCheckout.value ||
      (typeof document !== 'undefined' && document.body.classList.contains('swal2-shown'))
    if (hasOpenModalOrDialog) {
      return { abortedByStaffLock: false, changes: [] }
    }
  }
  isSyncingHardCases = true
  const changes = []
  let abortedByStaffLock = false

  try {
    const [formRes, prodRes, empRes] = await Promise.allSettled([
      withRequestTimeout(http.get('/hoa-don/form-data'), 4000),
      withRequestTimeout(fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500`).then(r => r.json()), 4000),
      withRequestTimeout(http.get('/nhan-vien', { params: { page: 0, size: 100 } }), 4000)
    ])

    // --- CASE 3: CHUYỂN QUYỀN NHÂN VIÊN & TRẠNG THÁI TÀI KHOẢN ---
    if (empRes.status === 'fulfilled' && empRes.value?.data?.content) {
      const staffCheck = syncEmployeeListAndCheckRole(empRes.value.data.content, false)
      if (staffCheck.abortedByStaffLock) {
        abortedByStaffLock = true
        return { abortedByStaffLock, changes: staffCheck.changes }
      }
      if (staffCheck.changes.length > 0) {
        changes.push(...staffCheck.changes)
      }
    }

    // Lưu tạm tổng tiền hàng cũ của từng đơn trước khi cập nhật giá mới để đối chiếu voucher chính xác
    const oldSubTotalsMap = new Map()
    for (const order of pendingOrders.value) {
      const oldSub = (order.cart || []).reduce((sum, i) => sum + (Number(i.price || 0) * Number(i.quantity || 0)), 0)
      oldSubTotalsMap.set(order.id, oldSub)
    }

    // --- CASE 1 (ĐỔI GIÁ) & CASE 4 (ĐỢT GIẢM GIÁ THAY ĐỔI) ---
    if (prodRes.status === 'fulfilled' && prodRes.value && Array.isArray(prodRes.value.content)) {
      const rawProducts = prodRes.value.content
      const prodMap = new Map(rawProducts.map(p => [Number(p.id), p]))

      for (const order of pendingOrders.value) {
        if (!Array.isArray(order.cart) || order.cart.length === 0) continue
        const orderLabel = pendingOrders.value.length > 1 ? ` (${order.title})` : ''

        for (let i = order.cart.length - 1; i >= 0; i--) {
          const item = order.cart[i]
          const freshProd = prodMap.get(Number(item.idCtsp))

          // Kiểm tra sản phẩm bị ngừng bán hoặc xóa
          if (!freshProd || freshProd.trangThai === false) {
            changes.push(`Sản phẩm "${item.name}" (Size ${item.size} - ${item.color})${orderLabel} vừa ngừng kinh doanh và đã được gỡ khỏi giỏ hàng`)
            order.cart.splice(i, 1)
            continue
          }

          const oldOriginalPrice = Number(item.originalPrice || item.price || 0)
          const newOriginalPrice = Number(freshProd.price || freshProd.giaBan || 0)
          const oldEffectivePrice = Number(item.price || 0)
          const newEffectivePrice = getEffectivePrice(freshProd)
          const oldDisc = normalizeDiscountStr(item.discount)
          const newDisc = normalizeDiscountStr(freshProd.discount)

          // Case 1: Đổi giá bán gốc
          if (oldOriginalPrice !== newOriginalPrice) {
            item.originalPrice = newOriginalPrice
            item.price = newEffectivePrice
            item.discount = freshProd.discount || null
            changes.push(
              `Đổi giá: Sản phẩm "${item.name}" (Size ${item.size} - ${item.color})${orderLabel} thay đổi giá gốc từ ${formatVnd(oldOriginalPrice)} → ${formatVnd(newOriginalPrice)} (Đơn giá mới: ${formatVnd(newEffectivePrice)})`
            )
          }
          // Case 4: Đợt giảm giá thay đổi (giá gốc giữ nguyên nhưng % khuyến mãi hoặc giá sau giảm thay đổi)
          else if (oldEffectivePrice !== newEffectivePrice || oldDisc !== newDisc) {
            item.originalPrice = newOriginalPrice
            item.price = newEffectivePrice
            item.discount = freshProd.discount || null
            if (!newDisc) {
              changes.push(
                `Đợt giảm giá kết thúc: Sản phẩm "${item.name}" (Size ${item.size} - ${item.color})${orderLabel} đã hết khuyến mãi (${oldDisc || 'KM'}), đơn giá về giá gốc ${formatVnd(newEffectivePrice)}`
              )
            } else if (!oldDisc) {
              changes.push(
                `Đợt giảm giá mới: Sản phẩm "${item.name}" (Size ${item.size} - ${item.color})${orderLabel} được áp dụng khuyến mãi ${newDisc} (${formatVnd(oldEffectivePrice)} → ${formatVnd(newEffectivePrice)})`
              )
            } else {
              changes.push(
                `Đợt giảm giá thay đổi: Sản phẩm "${item.name}" (Size ${item.size} - ${item.color})${orderLabel} đổi mức khuyến mãi từ ${oldDisc} → ${newDisc} (Đơn giá: ${formatVnd(oldEffectivePrice)} → ${formatVnd(newEffectivePrice)})`
              )
            }
          }
        }
      }

      // Cập nhật danh sách sản phẩm đang kinh doanh và đồng bộ lại tồn kho khả dụng
      const activeProducts = rawProducts.filter(p => p.trangThai !== false)
      setServerProductsList(activeProducts)
    }

    // --- CASE 2: SỬA VOUCHER (HẾT HẠN / HẾT HẠN DO ĐỔI EDIT / KHÓA / HẾT LƯỢT / ĐỔI GIÁ TRỊ GIẢM) ---
    if (formRes.status === 'fulfilled' && formRes.value?.data) {
      const d = formRes.value.data
      allCustomers.value = d.khachHangs || []
      const rawVouchers = d.phieuGiamGias || []
      allVouchers.value = rawVouchers.filter(v => v.trangThai !== false)

      const now = new Date()
      for (const order of pendingOrders.value) {
        if (!order.selectedVoucher) continue
        const oldV = order.selectedVoucher
        const vCode = oldV.ma || oldV.maPhieuGiamGia || 'Voucher'
        const orderLabel = pendingOrders.value.length > 1 ? ` (${order.title})` : ''
        const freshV = rawVouchers.find(v =>
          Number(v.id) === Number(oldV.id) ||
          (v.ma && String(v.ma).toUpperCase() === String(vCode).toUpperCase())
        )

        const newOrderSubTotal = (order.cart || []).reduce((sum, i) => sum + (Number(i.price || 0) * Number(i.quantity || 0)), 0)
        const oldOrderSubTotal = oldSubTotalsMap.get(order.id) ?? newOrderSubTotal
        const oldDiscAmt = calculateVoucherDiscount(oldV, oldOrderSubTotal)

        if (!freshV || freshV.trangThai === false) {
          order.selectedVoucher = null
          changes.push(`Sửa voucher: Phiếu giảm giá "${vCode}"${orderLabel} đã bị khóa hoặc ngừng hoạt động và được tự động gỡ`)
        } else if (freshV.ngayKetThuc && new Date(freshV.ngayKetThuc) < now) {
          order.selectedVoucher = null
          changes.push(`Sửa voucher: Phiếu giảm giá "${vCode}"${orderLabel} đã hết hạn sử dụng (${formatDateVn(freshV.ngayKetThuc)}) và được tự động gỡ`)
        } else if (freshV.ngayBatDau && new Date(freshV.ngayBatDau) > now) {
          order.selectedVoucher = null
          changes.push(`Sửa voucher: Phiếu giảm giá "${vCode}"${orderLabel} chưa đến thời gian hiệu lực (${formatDateVn(freshV.ngayBatDau)}) và được tự động gỡ`)
        } else if (freshV.soLuong && freshV.soLuongDaSuDung !== undefined && Number(freshV.soLuongDaSuDung) >= Number(freshV.soLuong)) {
          order.selectedVoucher = null
          changes.push(`Sửa voucher: Phiếu giảm giá "${vCode}"${orderLabel} đã hết lượt sử dụng (${freshV.soLuongDaSuDung}/${freshV.soLuong}) và được tự động gỡ`)
        } else if (newOrderSubTotal < Number(freshV.hoaDonToiThieu || 0)) {
          order.selectedVoucher = null
          changes.push(`Sửa voucher: Phiếu giảm giá "${vCode}"${orderLabel} yêu cầu đơn tối thiểu ${formatVnd(freshV.hoaDonToiThieu)} (hiện tại ${formatVnd(newOrderSubTotal)}) nên đã được tự động gỡ`)
        } else {
          const newDiscAmt = calculateVoucherDiscount(freshV, newOrderSubTotal)
          const valueEdited =
            Number(oldV.giaTriGiam || 0) !== Number(freshV.giaTriGiam || 0) ||
            String(oldV.loaiGiamGia || '') !== String(freshV.loaiGiamGia || '') ||
            Number(oldV.giamToiDa || 0) !== Number(freshV.giamToiDa || 0) ||
            Number(oldV.hoaDonToiThieu || 0) !== Number(freshV.hoaDonToiThieu || 0)

          if (valueEdited) {
            order.selectedVoucher = freshV
            changes.push(
              `Sửa voucher: Phiếu giảm giá "${freshV.ma || vCode}"${orderLabel} vừa thay đổi giá trị giảm (Số tiền giảm: ${formatVnd(oldDiscAmt)} → ${formatVnd(newDiscAmt)})`
            )
          } else if (oldDiscAmt !== newDiscAmt && oldOrderSubTotal !== newOrderSubTotal) {
            order.selectedVoucher = freshV
            changes.push(
              `Cập nhật voucher "${freshV.ma || vCode}"${orderLabel}: Mức giảm điều chỉnh từ ${formatVnd(oldDiscAmt)} → ${formatVnd(newDiscAmt)} theo đơn giá mới`
            )
          }
        }
      }
    }

    if (changes.length > 0) {
      savePendingOrdersToStorage()
      for (const msg of changes) {
        if (!pendingSyncAlerts.value.includes(msg)) {
          pendingSyncAlerts.value.push(msg)
        }
      }
      if (isBankQrModalOpen.value) {
        stopAutoCheckoutCountdown()
        if (finalTotal.value > 0) {
          generateBankTransferQr()
        } else {
          isBankQrModalOpen.value = false
        }
      }
      if (!options.silentToast) {
        notifyWarning('Dữ liệu bán hàng vừa thay đổi!', changes.join(' • '), 6500)
      }
    }
  } catch (err) {
    console.warn('Đồng bộ dữ liệu thời gian thực POS thất bại:', err)
  } finally {
    isSyncingHardCases = false
  }

  return { abortedByStaffLock, changes }
}

const openVoucherModal = () => {
  if (!currentOrder.value) createNewOrder()
  voucherSearchQuery.value = ''
  isVoucherModalOpen.value = true
  validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: true })
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

const handleApplyManualVoucher = async () => {
  const code = voucherInputCode.value.trim().toUpperCase()
  if (!code) return
  if (!currentOrder.value) createNewOrder()

  let found = allVouchers.value.find(v =>
    (v.ma && v.ma.toUpperCase() === code) ||
    (v.maPhieuGiamGia && v.maPhieuGiamGia.toUpperCase() === code)
  )

  if (!found) {
    await validateAndSyncHardCases({ isBeforeCheckout: true, silentToast: true })
    found = allVouchers.value.find(v =>
      (v.ma && v.ma.toUpperCase() === code) ||
      (v.maPhieuGiamGia && v.maPhieuGiamGia.toUpperCase() === code)
    )
  }

  if (!found) {
    notifyError('Mã không hợp lệ', `Không tìm thấy mã giảm giá "${code}" hoặc mã đã bị khóa/hết hạn!`)
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

const handleCheckout = async (options = {}) => {
  const skipConfirm = options?.skipConfirm === true
  if (!currentOrder.value || isProcessingCheckout.value) return
  if (currentOrder.value.cart.length === 0) {
    notifyWarning('Giỏ hàng trống', 'Vui lòng chọn ít nhất 1 sản phẩm vào giỏ hàng trước khi thanh toán!')
    return
  }

  if (!activeShift.value) {
    notifyWarning('Chưa mở ca làm việc', 'Vui lòng mở ca làm việc trước khi thực hiện thanh toán tại quầy!')
    openMoCaModal()
    return
  }

  // Bắt buộc kiểm tra & đồng bộ 4 Case khó (Đổi giá, Sửa voucher, Chuyển quyền NV, Đợt giảm giá) ngay trước khi thanh toán
  const syncRes = await validateAndSyncHardCases({ isBeforeCheckout: true, silentToast: true })
  if (syncRes.abortedByStaffLock) return

  const combinedAlerts = [...new Set([...pendingSyncAlerts.value, ...syncRes.changes])]
  pendingSyncAlerts.value = []

  if (currentOrder.value.cart.length === 0) {
    notifyWarning('Giỏ hàng trống', combinedAlerts.join(' • ') || 'Các sản phẩm trong giỏ đã ngừng kinh doanh hoặc hết hàng!')
    return
  }

  if (combinedAlerts.length > 0) {
    notifyWarning('Dữ liệu đơn hàng vừa thay đổi!', combinedAlerts.join(' • '), 6500)
    const okAfterChange = await notifyConfirm(
      'Cảnh báo dữ liệu vừa thay đổi!',
      `${combinedAlerts.join(' | ')} — Bạn có xác nhận tiếp tục thanh toán không?`,
      'Tiếp tục thanh toán',
      'Kiểm tra lại đơn'
    )
    if (!okAfterChange) return
  }

  if (currentOrder.value.paymentMethod === 'TIEN_MAT') {
    if (!currentOrder.value.cashGiven || currentOrder.value.cashGiven === 0) {
      currentOrder.value.cashGiven = finalTotal.value
    } else if (currentOrder.value.cashGiven < finalTotal.value) {
      notifyWarning('Chưa đủ tiền', `Số tiền khách đưa chưa đủ! Cần thêm ${formatVnd(finalTotal.value - currentOrder.value.cashGiven)}`)
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

  if (currentOrder.value.isDelivery) {
    const d = currentOrder.value.deliveryInfo
    if (!d.recipientName?.trim()) {
      notifyWarning('Thiếu thông tin giao hàng', 'Vui lòng nhập họ và tên người nhận hàng!')
      return
    }
    if (!d.recipientPhone?.trim()) {
      notifyWarning('Thiếu thông tin giao hàng', 'Vui lòng nhập số điện thoại người nhận hàng!')
      return
    }
    if (!d.province || !d.ward || !d.recipientAddress?.trim()) {
      notifyWarning('Thiếu địa chỉ giao hàng', 'Vui lòng chọn Tỉnh/Thành phố, Phường/Xã và nhập địa chỉ cụ thể!')
      return
    }
    if (d.feeLoading) {
      notifyWarning('Phí vận chuyển', 'Đang tính phí vận chuyển, vui lòng đợi trong giây lát...')
      return
    }
    if (!d.legacy || !d.shippingFee || d.shippingFee <= 0) {
      notifyWarning('Phí vận chuyển', 'Chưa tính được phí vận chuyển. Vui lòng chọn địa chỉ giao hàng hợp lệ và thử lại.')
      return
    }
  }

  const isDeliveryOrder = Boolean(currentOrder.value.isDelivery)
  const targetStatus = isDeliveryOrder ? 'Đã xác nhận' : 'Đã hoàn thành'

  if (!skipConfirm && combinedAlerts.length === 0) {
    const ok = await notifyConfirm(
      'Xác nhận thanh toán',
      'Bạn có chắc chắn muốn xác nhận thanh toán đơn hàng này không?',
      'Xác nhận',
      'Hủy'
    )
    if (!ok) return
  }

  isProcessingCheckout.value = true

  try {
    // Kiểm tra tồn kho thực tế trước khi gửi thanh toán
    for (const item of currentOrder.value.cart) {
      const prod = allProducts.value.find(p => Number(p.id) === Number(item.idCtsp))
      if (prod) {
        const realServerStock = getServerBaseStock(prod)
        if (item.quantity > realServerStock) {
          notifyError(
            'Không đủ tồn kho!',
            `Sản phẩm "${item.name}" (Size ${item.size} - ${item.color}) chỉ còn ${realServerStock} sản phẩm thực tế trong kho.`
          )
          isProcessingCheckout.value = false
          return
        }
      }
    }

    const paymentMethodId = getPaymentMethodId(currentOrder.value.paymentMethod)
    const employeeId = selectedStaffId.value || staffUserRef.value?.idNhanVien || (allEmployees.value.length > 0 ? allEmployees.value[0].id : 2)

    const delivery = currentOrder.value.isDelivery ? currentOrder.value.deliveryInfo : null

    const customerPhone = (delivery && delivery.recipientPhone?.trim())
      ? delivery.recipientPhone.trim()
      : (currentOrder.value.customer && currentOrder.value.customer.soDienThoai
        ? currentOrder.value.customer.soDienThoai.trim()
        : '')

    const customerName = (delivery && delivery.recipientName?.trim())
      ? delivery.recipientName.trim()
      : (currentOrder.value.customer
        ? (currentOrder.value.customer.ten || currentOrder.value.customer.tenKhachHang)
        : 'Khách lẻ')
    const payload = {
      maHoaDon: currentOrder.value.code,
      loaiDon: currentOrder.value.isDelivery ? 'Tại quầy (Giao hàng)' : 'Tại quầy',
      phiVanChuyen: currentOrder.value.isDelivery ? Number(delivery.shippingFee || 0) : 0,
      tenKhachHang: customerName,
      soDienThoaiKhachHang: customerPhone,
      diaChiNhanHang: delivery
        ? [delivery.recipientAddress, delivery.ward?.name, delivery.province?.name].filter(Boolean).join(', ')
        : '',
      toProvinceId: delivery?.legacy?.toProvinceId ?? null,
      toDistrictId: delivery?.legacy?.toDistrictId ?? null,
      toWardCode: delivery?.legacy?.toWardCode ?? null,
      trangThai: targetStatus,
      ghiChu: currentOrder.value.note || (isDeliveryOrder ? 'Đơn giao hàng tạo tại quầy POS' : 'Bán hàng tại quầy POS'),
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

    const soldCartSnapshot = currentOrder.value.cart.map(item => ({ ...item }))
    const invoiceRes = await http.post('/hoa-don', payload)
    const savedDto = invoiceRes?.data || null

    if (
      currentOrder.value.isDelivery &&
      currentOrder.value.customer?.id &&
      (delivery?.saveToCustomerProfile || !hasCustomerSavedAddress.value) &&
      !isCurrentDeliveryAddressSaved.value
    ) {
      await saveDeliveryAddressToCustomerProfile(true)
    }

    const completedRecord = {
      id: savedDto?.id,
      code: currentOrder.value.code,
      title: currentOrder.value.title,
      isDelivery: isDeliveryOrder,
      trangThai: targetStatus,
      deliveryAddress: payload.diaChiNhanHang,
      customerName: payload.tenKhachHang,
      customerPhone: payload.soDienThoaiKhachHang,
      cart: soldCartSnapshot,
      subTotal: subTotal.value,
      discount: savedDto?.tienGiam !== undefined && savedDto?.tienGiam !== null ? Number(savedDto.tienGiam) : discountAmount.value,
      shippingFee: payload.phiVanChuyen,
      finalTotal: savedDto?.tienSauGiamGia !== undefined && savedDto?.tienSauGiamGia !== null ? Number(savedDto.tienSauGiamGia) : finalTotal.value,
      paymentMethod: currentOrder.value.paymentMethod,
      cashGiven: currentOrder.value.cashGiven,
      changeDue: Math.max(0, changeDue.value),
      date: new Date().toLocaleString('vi-VN'),
      staffName: staffName.value || 'Nhân viên quầy'
    }

    completedInvoiceData.value = completedRecord
    saveRecentInvoiceToStorage(completedRecord)

    // Trừ ngay tồn kho gốc (_serverStock) cho các sản phẩm vừa bán
    soldCartSnapshot.forEach(soldItem => {
      const prod = allProducts.value.find(p => Number(p.id) === Number(soldItem.idCtsp))
      if (prod) {
        prod._serverStock = Math.max(0, getServerBaseStock(prod) - Number(soldItem.quantity || 0))
      }
    })

    if (isDeliveryOrder) {
      notifySuccess('Tạo đơn giao hàng thành công!', `Đơn hàng ${currentOrder.value.code} đã được tạo với trạng thái Đã xác nhận`)
    } else {
      notifySuccess('Thanh toán thành công!', `Đã hoàn tất hóa đơn ${currentOrder.value.code}`)
    }

    pendingOrders.value.splice(activeOrderIndex.value, 1)
    if (pendingOrders.value.length === 0) {
      createNewOrder(true)
    } else if (activeOrderIndex.value >= pendingOrders.value.length) {
      activeOrderIndex.value = Math.max(0, pendingOrders.value.length - 1)
    }

    // Đồng bộ lại tồn kho khả dụng và số lượt dùng voucher mới nhất cho các hóa đơn chờ còn lại
    reconcilePendingOrdersWithServerStock()
    recalculateAvailableStocks()
    savePendingOrdersToStorage()

    validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: true })

    if (savedDto?.id) {
      // Khi click vào thanh toán lập tức chuyển sang trang hóa đơn chi tiết của hóa đơn vừa thanh toán
      router.push(`/hoa-don/${savedDto.id}`)
      return
    }

    isReceiptModalOpen.value = true

  } catch (err) {
    console.error('Checkout error:', err)
    notifyError('Lỗi thanh toán', err.message || 'Không thể lưu hóa đơn vào hệ thống!')
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

  // Lấy nội dung hóa đơn
  const receiptHtml = printEl.outerHTML

  // Tự động đóng modal xem trước hóa đơn ngay khi thực hiện in, thu ngân không cần bấm Đóng thủ công
  isReceiptModalOpen.value = false

  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = '0'
  iframe.style.opacity = '0'
  iframe.style.pointerEvents = 'none'
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow.document
  doc.open()
  doc.write(`<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Hoa_don_${completedInvoiceData.value?.code || 'POS'}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
      @page {
        size: 80mm auto;
        margin: 0;
      }
      * {
        box-sizing: border-box;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
        color-adjust: exact !important;
      }
      html, body {
        margin: 0;
        padding: 0;
        width: 100%;
        background: #ffffff;
        font-family: 'Plus Jakarta Sans', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        color: #0f172a;
        font-size: 12px;
        line-height: 1.35;
      }
      .thermal-print-wrap {
        width: 100%;
        display: flex;
        justify-content: center;
        background: #ffffff;
      }
      .thermal-receipt-box {
        width: 76mm !important;
        max-width: 76mm !important;
        min-width: 76mm !important;
        margin: 0 auto !important;
        padding: 10px 8px !important;
        background: #ffffff !important;
        box-sizing: border-box !important;
      }
      .receipt-header-center {
        text-align: center;
        padding-bottom: 10px;
        border-bottom: 1px dashed #cbd5e1 !important;
        margin-bottom: 10px;
      }
      .receipt-brand-logo {
        font-size: 18px;
        font-weight: 800;
        letter-spacing: 0.5px;
        margin-bottom: 3px;
        color: #0f172a;
      }
      .receipt-sub-line {
        font-size: 10.5px;
        color: #475569;
        margin: 2px 0;
        line-height: 1.35;
      }
      .receipt-bill-heading {
        text-align: center;
        margin-bottom: 10px;
        padding-bottom: 8px;
        border-bottom: 1px dashed #cbd5e1 !important;
      }
      .receipt-bill-title {
        font-size: 14px;
        font-weight: 800;
        color: #0f172a;
        letter-spacing: 0.5px;
      }
      .receipt-barcode-code {
        font-family: 'SFMono-Regular', Consolas, monospace;
        font-size: 11.5px;
        font-weight: 600;
        letter-spacing: 2px;
        color: #64748b;
        margin-top: 2px;
      }
      .receipt-meta-info {
        display: flex;
        flex-direction: column;
        gap: 5px;
        margin-bottom: 12px;
        padding: 8px 10px;
        background-color: #f8fafc !important;
        border: 1px solid #e2e8f0 !important;
        border-radius: 8px;
      }
      .meta-row {
        display: flex;
        justify-content: space-between;
        font-size: 11.5px;
      }
      .meta-k {
        color: #475569;
      }
      .meta-v {
        font-weight: 600;
        color: #0f172a;
        text-align: right;
      }
      .receipt-items-table {
        width: 100%;
        border-collapse: collapse;
        margin-bottom: 12px;
      }
      .receipt-items-table thead tr {
        border-top: 1px dashed #cbd5e1 !important;
        border-bottom: 1px dashed #cbd5e1 !important;
      }
      .receipt-items-table th {
        padding: 6px 0;
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        color: #475569;
        letter-spacing: 0.3px;
        text-align: left;
      }
      .receipt-items-table tbody tr {
        border-bottom: 1px solid #f1f5f9;
      }
      .receipt-items-table td {
        padding: 6px 0;
        font-size: 11.5px;
        vertical-align: top;
      }
      .text-center {
        text-align: center !important;
      }
      .text-end {
        text-align: right !important;
      }
      .rc-item-name {
        font-weight: 600;
        color: #0f172a;
        line-height: 1.3;
      }
      .rc-item-variant {
        font-size: 10px;
        color: #64748b;
        margin-top: 1px;
      }
      .rc-item-qty {
        text-align: center !important;
        font-weight: 600;
      }
      .rc-item-price {
        text-align: right !important;
        color: #475569;
      }
      .rc-item-total {
        text-align: right !important;
        font-weight: 700;
        color: #0f172a;
      }
      .receipt-totals-block {
        border-top: 1px dashed #cbd5e1 !important;
        padding-top: 10px;
        display: flex;
        flex-direction: column;
        gap: 5px;
      }
      .receipt-sum-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 11.5px;
      }
      .sum-k {
        color: #64748b;
      }
      .sum-v {
        font-weight: 600;
        color: #0f172a;
      }
      .discount-val {
        color: #dc2626 !important;
        font-weight: 700;
      }
      .grand-sum {
        border-top: 1.5px solid #0f172a !important;
        padding-top: 6px;
        margin-top: 3px;
      }
      .grand-sum .sum-k {
        font-size: 12.5px;
        font-weight: 800;
        color: #0f172a;
      }
      .grand-sum-val {
        font-size: 16px;
        font-weight: 900;
        color: #dc2626 !important;
      }
      .receipt-footer-notes {
        text-align: center;
        margin-top: 14px;
        padding-top: 10px;
        border-top: 1px dashed #cbd5e1 !important;
      }
      .rc-thank-txt {
        font-size: 12px;
        font-weight: 700;
        margin: 0 0 2px;
        color: #0f172a;
      }
      .rc-policy-txt {
        font-size: 10px;
        color: #64748b;
        margin: 0;
        line-height: 1.35;
      }
    
.btn-receipt-view-invoice {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 16px;
  background: #eff6ff;
  border: 1px solid #93c5fd;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1d4ed8;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-receipt-view-invoice:hover {
  background: #dbeafe;
  border-color: #60a5fa;
  color: #1e40af;
}

</style>
  </head>
  <body>
    <div class="thermal-print-wrap">
      ${receiptHtml}
    </div>
  </body>
</html>`)
  doc.close()

  const doPrint = () => {
    iframe.contentWindow.focus()
    iframe.contentWindow.onafterprint = () => {
      isReceiptModalOpen.value = false
    }
    try {
      iframe.contentWindow.print()
    } catch (e) {
      console.error('Print trigger error:', e)
    } finally {
      isReceiptModalOpen.value = false
    }
    setTimeout(() => {
      if (document.body.contains(iframe)) {
        document.body.removeChild(iframe)
      }
    }, 2000)
  }

  if (iframe.contentWindow.document.fonts && iframe.contentWindow.document.fonts.ready) {
    iframe.contentWindow.document.fonts.ready.then(() => {
      setTimeout(doPrint, 120)
    }).catch(() => {
      setTimeout(doPrint, 200)
    })
  } else {
    setTimeout(doPrint, 250)
  }
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
    isRecentInvoicesModalOpen.value = false
    isMoCaModalOpen.value = false
    isKetCaModalOpen.value = false
    stopCameraScanner()
  }
}

const loadInitialData = async () => {
  isLoadingData.value = true
  try {
    const hadStaffSnapshot = loadStaffSnapshotFromStorage()
    const restoredOrders = restorePendingOrdersFromStorage()

    const [formRes, prodRes, brandRes, catRes, colorRes, sizeRes, empRes] = await Promise.allSettled([
      http.get('/hoa-don/form-data'),
      fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/thuong_hieu`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/loai_giay`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/mau_sac`).then(r => r.json()),
      fetch(`${API_BASE}/attributes/kich_co`).then(r => r.json()),
      http.get('/nhan-vien', { params: { page: 0, size: 100 } })
    ])

    if (formRes.status === 'fulfilled' && formRes.value.data) {
      const d = formRes.value.data
      allCustomers.value = d.khachHangs || []
      allVouchers.value = (d.phieuGiamGias || []).filter(v => v.trangThai !== false)
      allEmployees.value = d.nhanViens || []
      allPaymentMethods.value = d.phuongThucThanhToans || d.phuongThucs || []
    }

    if (empRes.status === 'fulfilled' && empRes.value?.data?.content && !hadStaffSnapshot) {
      syncEmployeeListAndCheckRole(empRes.value.data.content, true)
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

    let loadedProducts = []
    if (prodRes.status === 'fulfilled' && prodRes.value && prodRes.value.content) {
      loadedProducts = prodRes.value.content.filter(p => p.trangThai !== false)
    }

    if (loadedProducts.length === 0) {
      loadedProducts = [
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

    setServerProductsList(loadedProducts)

    if (!restoredOrders || pendingOrders.value.length === 0) {
      createNewOrder(true)
    } else if (currentOrder.value?.customer?.id) {
      fetchAndSyncCustomerAddresses(currentOrder.value.customer, false, false)
    }

    // Nếu quay lại màn hình POS từ các màn quản lý khác (cùng 1 tab), kiểm tra ngay 4 case khó
    if (restoredOrders || hadStaffSnapshot) {
      await validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: false })
    }
  } catch (error) {
    console.error('Error loading POS data:', error)
    if (pendingOrders.value.length === 0) {
      createNewOrder(true)
    }
  } finally {
    isLoadingData.value = false
  }
}

const handleWindowClick = (e) => {
  if (!e.target.closest('.customer-search-area')) {
    showCustomerDropdown.value = false
  }
}

const handleWindowFocusOrVisible = () => {
  if (document.visibilityState === 'visible') {
    validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: false })
  }
}

const handleCrossTabStorageSync = (e) => {
  if (!e || !e.key) return
  if (
    e.key === 'polyshoes_pos_sync_signal' ||
    e.key === 'polyshoes_last_updated_staff' ||
    e.key === 'vaiTroHienTai' ||
    e.key === 'polyshoes_user'
  ) {
    staffUserRef.value = getUser()
    validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: false })
  }
}

onMounted(() => {
  fetchActiveShift()
  loadRecentInvoicesFromStorage()
  loadInitialData()
  loadProvinces()
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('click', handleWindowClick)
  window.addEventListener('focus', handleWindowFocusOrVisible)
  document.addEventListener('visibilitychange', handleWindowFocusOrVisible)
  window.addEventListener('storage', handleCrossTabStorageSync)
  realtimePollTimer = setInterval(() => {
    if (document.visibilityState === 'visible') {
      validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: false })
    }
  }, 5000)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('click', handleWindowClick)
  window.removeEventListener('focus', handleWindowFocusOrVisible)
  document.removeEventListener('visibilitychange', handleWindowFocusOrVisible)
  window.removeEventListener('storage', handleCrossTabStorageSync)
  if (realtimePollTimer) {
    clearInterval(realtimePollTimer)
    realtimePollTimer = null
  }
  stopCameraScanner()
})
</script>

<template>
  <div class="pos-page">
    <!-- CỤC RIÊNG BIỆT: THANH ĐIỀU HÀNH CA LÀM VIỆC & NHÂN VIÊN POS -->
    <div class="pos-top-bar">
      <div class="pos-station-info">
        <div class="pos-brand-pill">
          <span class="station-brand">POLYSHOES</span>
          <span class="station-divider">•</span>
          <span class="station-num">BÁN HÀNG TẠI QUẦY</span>
        </div>
        <div class="pos-status-indicator" v-if="activeShift">
          <span class="pulse-dot"></span>
          <span>Ca đang mở</span>
        </div>
        <div class="pos-status-indicator shift-closed" v-else>
          <span class="pulse-dot closed"></span>
          <span>Chưa mở ca</span>
        </div>
      </div>

      <div class="pos-header-actions">
        <!-- Nút Mở / Đóng ca làm việc trên quầy POS -->
        <div v-if="activeShift" class="pos-shift-group">
          <span class="pos-shift-badge" :title="`Ca làm việc ${activeShift.maCa} đang hoạt động`">
            <span class="pulse-dot"></span>
            Ca: <strong>{{ activeShift.maCa }}</strong>
          </span>
          <button type="button" class="btn-pos-close-shift" @click="openKetCaModal" title="Đóng và kết ca làm việc này">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <span>Đóng ca</span>
          </button>
        </div>

        <button v-else type="button" class="btn-pos-open-shift" @click="openMoCaModal" title="Mở ca làm việc mới để bắt đầu bán hàng">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="16"></line>
            <line x1="8" y1="12" x2="16" y2="12"></line>
          </svg>
          <span>Mở ca</span>
        </button>

        <div class="pos-orders-counter-pill" :title="`Nhân viên phụ trách: ${staffName} (${staffRoleLabel})`">
          <span class="counter-lbl">NV:</span>
          <span class="counter-num">{{ staffName }}</span>
          <span class="counter-lbl">({{ staffRoleLabel }})</span>
        </div>

        <button type="button" class="btn-recent-pos-invoices" @click="openRecentInvoicesModal"
          title="Xem lịch sử đơn vừa bán trong ca và in lại hóa đơn">
          <span>Đơn vừa bán</span>
          <span class="recent-count-badge" v-if="recentPosInvoices.length > 0">{{ recentPosInvoices.length }}</span>
        </button>
      </div>
    </div>

    <!-- Cảnh báo khi quầy chưa mở ca làm việc -->
    <div v-if="!activeShift && !isLoadingShift" class="pos-no-shift-alert-banner">
      <div class="alert-banner-left">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <span><strong>Quầy hiện chưa mở ca làm việc.</strong> Vui lòng bấm <strong>Mở ca</strong> để khai báo tiền két và ghi nhận doanh thu phiên bán hàng!</span>
      </div>
      <button type="button" class="btn-banner-open-shift" @click="openMoCaModal">
        <span>+ Mở ca ngay</span>
      </button>
    </div>

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

            <div class="pos-orders-counter-pill" title="Số lượng hóa đơn chờ">
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
                      <th width="130" class="text-center">ĐƠN GIÁ</th>
                      <th width="140" class="text-center">THÀNH TIỀN</th>
                      <th width="70" class="text-center">THAO TÁC</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, idx) in currentOrder.cart" :key="item.idCtsp">
                      <td class="text-center cell-stt">{{ idx + 1 }}</td>
                      <td>
                        <div class="product-item-wrap">
                          <div class="prod-thumb-container">
                            <img v-if="item.img" :src="item.img" alt="Ảnh SP" class="prod-thumb-mini" />
                            <span v-if="getDiscountPercentBadge(item)" class="prod-img-discount-badge mini">
                              {{ getDiscountPercentBadge(item) }}
                            </span>
                          </div>
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
                          <button class="step-btn" @click="updateCartItemQty(item, item.quantity - 1)" title="Giảm số lượng (về 0 để xóa)">−</button>
                          <input type="number" class="step-input" :value="item.quantity"
                            min="1" :max="getMaxAllowedForCartItem(item)"
                            @input="handleCartItemQtyInput(item, $event)"
                            @change="updateCartItemQty(item, $event.target.value, $event)"
                            @blur="updateCartItemQty(item, $event.target.value, $event)"
                            @keydown.enter="$event.target.blur()" />
                          <button class="step-btn" @click="updateCartItemQty(item, item.quantity + 1)"
                            :disabled="getRemainingStock(item.idCtsp) <= 0 || item.quantity >= getMaxAllowedForCartItem(item)">+</button>
                        </div>
                        <div class="stock-hint-txt" v-if="getRemainingStock(item.idCtsp) < 10">
                          Còn lại: {{ getRemainingStock(item.idCtsp) }} / Tối đa: {{ getMaxAllowedForCartItem(item) }}
                        </div>
                        <div class="cross-order-stock-hint" v-if="getReservedInOtherOrders(item.idCtsp) > 0">
                          Đơn chờ khác giữ: {{ getReservedInOtherOrders(item.idCtsp) }}
                        </div>
                      </td>
                      <td class="text-center">
                        <div class="price-stack">
                          <span class="price-active">{{ formatVnd(item.price) }}</span>
                          <span v-if="item.originalPrice && item.originalPrice > item.price"
                            class="price-strikethrough">
                            {{ formatVnd(item.originalPrice) }}
                            <strong v-if="normalizeDiscountStr(item.discount)" style="color: #dc2626; font-size: 11px; margin-left: 4px; text-decoration: none; display: inline-block;">
                              ({{ normalizeDiscountStr(item.discount) }})
                            </strong>
                          </span>
                        </div>
                      </td>
                      <td class="text-center cell-total-price">
                        {{ formatVnd(item.price * item.quantity) }}
                      </td>
                      <td class="text-center">
                        <button class="btn-del-item" @click="removeCartItem(item)" title="Xóa khỏi giỏ hàng">
                          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                            <line x1="10" y1="11" x2="10" y2="17"></line>
                            <line x1="14" y1="11" x2="14" y2="17"></line>
                          </svg>
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

        <!-- FORM ĐIỀN THÔNG TIN GIAO HÀNG (Nằm ngay sau giỏ hàng khi bật Giao hàng) -->
        <div v-if="currentOrder && currentOrder.isDelivery" class="pos-delivery-form-card">
          <div class="pos-delivery-card-head">
            <div class="pos-delivery-title-group">
              <div class="pos-delivery-icon-badge">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="1" y="3" width="15" height="13" rx="2"></rect>
                  <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
                  <circle cx="5.5" cy="18.5" r="2.5"></circle>
                  <circle cx="18.5" cy="18.5" r="2.5"></circle>
                </svg>
              </div>
              <div class="pos-delivery-head-text">
                <h4 class="pos-delivery-title">Thông tin giao hàng</h4>
                <p class="pos-delivery-subtitle">Điền địa chỉ người nhận theo địa giới hành chính mới để tính phí vận chuyển GHN</p>
              </div>
            </div>

            <div class="pos-delivery-head-actions">
              <button v-if="currentOrder.customer" type="button" class="btn-del-action-fill" @click="fillDeliveryFromCustomer" title="Lấy họ tên, SĐT và địa chỉ từ khách hàng đã chọn">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span>Điền từ khách hàng</span>
              </button>
              <button type="button" class="btn-del-action-reset" @click="clearDeliveryForm" title="Làm mới các ô thông tin nhận hàng">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
                  <path d="M3 3v5h5"></path>
                </svg>
                <span>Làm mới</span>
              </button>
            </div>
          </div>

          <div class="pos-delivery-card-body">
            <!-- Sổ địa chỉ đã lưu của khách hàng (chọn nhanh 1 chạm) -->
            <div v-if="currentOrder.customer && currentCustomerSavedAddresses.length > 0" class="pos-del-saved-addresses-bar">
              <div class="saved-addr-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                </svg>
                <span>Địa chỉ đã lưu của <strong>{{ currentOrder.customer.ten || currentOrder.customer.tenKhachHang }}</strong>:</span>
              </div>
              <div class="saved-addr-pills">
                <button
                  v-for="addr in currentCustomerSavedAddresses"
                  :key="addr.id"
                  type="button"
                  class="saved-addr-pill"
                  :class="{
                    active:
                      normalizePlaceName(currentOrder.deliveryInfo.province?.name) === normalizePlaceName(addr.thanhPho) &&
                      normalizePlaceName(currentOrder.deliveryInfo.ward?.name) === normalizePlaceName(addr.phuong) &&
                      String(currentOrder.deliveryInfo.recipientAddress || '').trim().toLowerCase() === String(addr.diaChiCuThe || '').trim().toLowerCase()
                  }"
                  @click="applySavedAddressToDeliveryForm(addr, currentOrder.customer, true)"
                  :title="[addr.diaChiCuThe, addr.phuong, addr.thanhPho].filter(Boolean).join(', ')"
                >
                  <span v-if="addr.macDinh" class="pill-default-tag">Mặc định</span>
                  <span class="pill-addr-text">{{ [addr.diaChiCuThe, addr.phuong, addr.thanhPho].filter(Boolean).join(', ') }}</span>
                </button>
              </div>
            </div>

            <div class="pos-delivery-form-grid">
              <!-- Họ và tên người nhận -->
              <div class="pos-del-field">
                <label class="pos-del-label">
                  Họ và tên người nhận <span class="req-star">*</span>
                </label>
                <div class="pos-del-input-wrap">
                  <span class="pos-del-field-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                      <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                  </span>
                  <input type="text" class="pos-del-control with-icon" placeholder="Nhập họ và tên người nhận hàng..."
                    v-model="currentOrder.deliveryInfo.recipientName" />
                </div>
              </div>

              <!-- Số điện thoại người nhận -->
              <div class="pos-del-field">
                <label class="pos-del-label">
                  Số điện thoại nhận hàng <span class="req-star">*</span>
                </label>
                <div class="pos-del-input-wrap">
                  <span class="pos-del-field-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                    </svg>
                  </span>
                  <input type="tel" class="pos-del-control with-icon" placeholder="Nhập SĐT liên hệ giao hàng (VD: 0987...)"
                    v-model="currentOrder.deliveryInfo.recipientPhone" maxlength="11" />
                </div>
              </div>

              <!-- Tỉnh / Thành phố -->
              <div class="pos-del-field">
                <label class="pos-del-label">
                  Tỉnh / Thành phố <span class="req-star">*</span>
                </label>
                <select class="pos-del-control" v-model="currentOrder.deliveryInfo.province" @change="onProvinceChange">
                  <option :value="null" disabled>-- Chọn Tỉnh / Thành phố --</option>
                  <option v-for="p in provincesList" :key="p.id" :value="p">{{ p.name }}</option>
                </select>
              </div>

              <!-- Phường / Xã -->
              <div class="pos-del-field">
                <label class="pos-del-label">
                  Phường / Xã <span class="req-star">*</span>
                </label>
                <select class="pos-del-control" v-model="currentOrder.deliveryInfo.ward" @change="onWardChange"
                  :disabled="!currentOrder.deliveryInfo.province || currentOrder.deliveryInfo.wardsLoading">
                  <option :value="null" disabled>
                    {{ currentOrder.deliveryInfo.wardsLoading ? 'Đang tải danh sách Phường / Xã...' : '-- Chọn Phường / Xã --' }}
                  </option>
                  <option v-for="w in wardsList" :key="w.id" :value="w">{{ w.name }}</option>
                </select>
              </div>

              <!-- Địa chỉ chi tiết -->
              <div class="pos-del-field" :class="{ 'col-span-2': !(currentOrder.deliveryInfo.services && currentOrder.deliveryInfo.services.length) }">
                <label class="pos-del-label">
                  Địa chỉ cụ thể (Số nhà, ngõ, tên đường, tòa nhà...) <span class="req-star">*</span>
                </label>
                <div class="pos-del-input-wrap">
                  <span class="pos-del-field-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                      <circle cx="12" cy="10" r="3"></circle>
                    </svg>
                  </span>
                  <input type="text" class="pos-del-control with-icon" placeholder="VD: Số 120 đường Cầu Giấy, Tòa nhà Discovery..."
                    v-model="currentOrder.deliveryInfo.recipientAddress" />
                </div>
              </div>

              <!-- Gói dịch vụ vận chuyển (nếu có) -->
              <div class="pos-del-field" v-if="currentOrder.deliveryInfo.services && currentOrder.deliveryInfo.services.length">
                <label class="pos-del-label">Gói dịch vụ vận chuyển GHN</label>
                <select class="pos-del-control" v-model="currentOrder.deliveryInfo.serviceTypeId" @change="onServiceChange">
                  <option v-for="s in currentOrder.deliveryInfo.services" :key="s.serviceTypeId" :value="s.serviceTypeId">
                    {{ s.shortName || ('Dịch vụ GHN #' + s.serviceTypeId) }}
                  </option>
                </select>
              </div>
            </div>

            <!-- Tùy chọn & Nút lưu địa chỉ vào hồ sơ khách hàng cho các lần mua sau -->
            <div v-if="currentOrder.customer" class="pos-del-save-profile-bar" :class="{ 'is-saved': isCurrentDeliveryAddressSaved }">
              <div class="save-profile-left">
                <span v-if="!hasCustomerSavedAddress" class="save-profile-badge badge-warning">
                  Khách chưa có địa chỉ lưu sẵn
                </span>
                <span v-else-if="isCurrentDeliveryAddressSaved" class="save-profile-badge badge-saved">
                  ✓ Đã khớp địa chỉ trong hồ sơ KH
                </span>
                <span v-else class="save-profile-badge badge-new">
                  Địa chỉ giao hàng mới
                </span>

                <label v-if="!isCurrentDeliveryAddressSaved" class="save-profile-chk-label">
                  <input
                    type="checkbox"
                    class="custom-chk pos-chk"
                    v-model="currentOrder.deliveryInfo.saveToCustomerProfile"
                  />
                  <span>Tự động lưu làm địa chỉ mặc định của <strong>{{ currentOrder.customer.ten || currentOrder.customer.tenKhachHang }}</strong> khi Thanh toán</span>
                </label>
                <span v-else class="save-profile-saved-note">
                  Lần mua tiếp theo hệ thống sẽ tự động điền Tỉnh/Phường/Số nhà này cho khách.
                </span>
              </div>

              <button
                v-if="!isCurrentDeliveryAddressSaved"
                type="button"
                class="btn-save-addr-profile"
                :disabled="!isCurrentDeliveryAddressComplete || isSavingCustomerAddress"
                @click="saveDeliveryAddressToCustomerProfile(false)"
                title="Lưu ngay Tỉnh/Phường/Số nhà này vào hồ sơ khách hàng"
              >
                <span v-if="isSavingCustomerAddress" class="fee-spinner"></span>
                <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                  <polyline points="17 21 17 13 7 13 7 21"></polyline>
                  <polyline points="7 3 7 8 15 8"></polyline>
                </svg>
                <span>{{ isSavingCustomerAddress ? 'Đang lưu...' : 'Lưu vào hồ sơ KH' }}</span>
              </button>
            </div>

            <!-- Thanh trạng thái & Phí vận chuyển GHN -->
            <div class="pos-delivery-footer-bar">
              <div class="pos-del-carrier-info">
                <span class="carrier-badge">GHN Express</span>
                <span class="carrier-address-preview" v-if="currentOrder.deliveryInfo.province && currentOrder.deliveryInfo.ward">
                  Giao tới: <strong>{{ [currentOrder.deliveryInfo.recipientAddress, currentOrder.deliveryInfo.ward?.name, currentOrder.deliveryInfo.province?.name].filter(Boolean).join(', ') }}</strong>
                </span>
                <span class="carrier-address-hint" v-else>
                  Vui lòng chọn Tỉnh/Thành phố và Phường/Xã để tự động tính cước phí vận chuyển
                </span>
              </div>

              <div class="pos-del-fee-status">
                <div v-if="currentOrder.deliveryInfo.feeLoading" class="del-fee-loading">
                  <span class="fee-spinner"></span>
                  <span>Đang tính phí GHN...</span>
                </div>
                <div v-else-if="currentOrder.deliveryInfo.feeError" class="del-fee-error">
                  <span>{{ currentOrder.deliveryInfo.feeError }}</span>
                  <button type="button" class="btn-fee-retry" @click="tinhPhiVanChuyen">Thử lại</button>
                </div>
                <div v-else class="del-fee-value-box">
                  <span class="del-fee-lbl">Phí vận chuyển:</span>
                  <strong class="del-fee-amount">{{ formatVnd(currentOrder.deliveryInfo.shippingFee || 0) }}</strong>
                </div>
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

          <div v-if="currentOrder && currentOrder.isDelivery" class="delivery-active-summary-badge">
            <div class="delivery-summary-row">
              <span class="delivery-summary-dot"></span>
              <span class="delivery-summary-txt" v-if="currentOrder.deliveryInfo.recipientName || currentOrder.deliveryInfo.recipientPhone">
                Người nhận: <strong>{{ currentOrder.deliveryInfo.recipientName || 'Chưa nhập tên' }}</strong>
                <template v-if="currentOrder.deliveryInfo.recipientPhone"> ({{ currentOrder.deliveryInfo.recipientPhone }})</template>
              </span>
              <span class="delivery-summary-txt" v-else>
                Vui lòng điền <strong>Form thông tin giao hàng</strong> ngay bên dưới giỏ hàng
              </span>
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
              <div class="cash-label-row">
                <span class="cash-label">Khách thanh toán tiền mặt</span>
                <span v-if="currentOrder.cashGiven > 0" class="cash-hint-preview">
                  ({{ formatVnd(currentOrder.cashGiven) }})
                </span>
              </div>
              <div class="cash-input-wrap">
                <input
                  type="number"
                  class="cash-amount-inp"
                  :value="currentOrder.cashGiven === 0 ? '' : currentOrder.cashGiven"
                  @input="currentOrder.cashGiven = $event.target.value === '' ? 0 : Math.max(0, Number($event.target.value))"
                  placeholder="Nhập số tiền khách đưa..."
                  min="0"
                  step="1000"
                  @focus="$event.target.select()"
                />
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
              <div class="cash-label-row">
                <span class="cash-label">Số tiền chuyển khoản</span>
                <span v-if="currentOrder.transferGiven > 0" class="cash-hint-preview">
                  ({{ formatVnd(currentOrder.transferGiven) }})
                </span>
              </div>
              <div class="cash-input-wrap">
                <input
                  type="number"
                  class="cash-amount-inp"
                  :value="currentOrder.transferGiven === 0 ? '' : currentOrder.transferGiven"
                  @input="currentOrder.transferGiven = $event.target.value === '' ? 0 : Math.max(0, Number($event.target.value))"
                  placeholder="Nhập số tiền chuyển khoản..."
                  min="0"
                  step="1000"
                  @focus="$event.target.select()"
                />
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
              <span v-else>THANH TOÁN</span>
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
                  <select v-model="modalFilters.idThuongHieu" class="filter-select"
                    :class="{ active: !!modalFilters.idThuongHieu }">
                    <option value="">Tất cả thương hiệu</option>
                    <option v-for="b in brandsList" :key="b.id" :value="b.id || b.ten">{{ b.ten || b.tenThuongHieu }}
                    </option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.idLoaiGiay" class="filter-select"
                    :class="{ active: !!modalFilters.idLoaiGiay }">
                    <option value="">Tất cả thể loại</option>
                    <option v-for="c in categoriesList" :key="c.id" :value="c.id || c.ten">{{ c.ten || c.tenLoaiGiay }}
                    </option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.idMauSac" class="filter-select"
                    :class="{ active: !!modalFilters.idMauSac }">
                    <option value="">Tất cả màu sắc</option>
                    <option v-for="m in colorsList" :key="m.id" :value="m.id || m.ten || m.tenMau">{{ m.ten || m.tenMau
                    }}
                    </option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.idKichCo" class="filter-select"
                    :class="{ active: !!modalFilters.idKichCo }">
                    <option value="">Tất cả kích cỡ</option>
                    <option v-for="s in sizesList" :key="s.id" :value="s.id || s.ten || s.tenKichCo">{{ s.ten ||
                      s.tenKichCo
                    }}</option>
                  </select>
                </div>

                <div class="filter-select-wrap">
                  <select v-model="modalFilters.priceRange" class="filter-select"
                    :class="{ active: !!modalFilters.priceRange }">
                    <option value="">Tất cả mức giá</option>
                    <option value="<1m">Dưới 1.000.000 ₫</option>
                    <option value="1m-3m">1.000.000 - 3.000.000 ₫</option>
                    <option value="3m-5m">3.000.000 - 5.000.000 ₫</option>
                    <option value=">5m">Trên 5.000.000 ₫</option>
                  </select>
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
                    <th width="140" class="text-center">MÃ CTSP</th>
                    <th>SẢN PHẨM</th>
                    <th width="110" class="text-center">MÀU SẮC</th>
                    <th width="75" class="text-center">SIZE</th>
                    <th width="90" class="text-center">TỒN KHO</th>
                    <th width="130" class="text-center">ĐƠN GIÁ</th>
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
                        <span v-if="getDiscountPercentBadge(item)" class="prod-img-discount-badge">
                          {{ getDiscountPercentBadge(item) }}
                        </span>
                      </div>
                    </td>
                    <td class="text-center">
                      <span class="code-pill-modal">{{ item.maCtsp || item.maChiTietSanPham || item.maSp }}</span>
                    </td>
                    <td class="modal-prod-name">
                      <span class="prod-text-title">{{ item.tenSp || item.tenSanPham }}</span>
                    </td>
                    <td class="text-center">
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
                      <div class="modal-reserved-hint" v-if="getReservedQtyAcrossOrders(item.id) > 0">
                        Đang giữ: {{ getReservedQtyAcrossOrders(item.id) }}
                      </div>
                    </td>
                    <td class="text-center">
                      <div class="prod-modal-price-box">
                        <span class="price-now">{{ formatVnd(getEffectivePrice(item)) }}</span>
                        <span v-if="item.discount && item.discount !== '-'" class="price-was">{{ formatVnd(item.price ||
                          item.giaBan) }}</span>
                      </div>
                    </td>
                    <td class="text-center" @click.stop>
                      <button class="btn-select-add-cart"
                        :disabled="(item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0"
                        @click="quickAddSingleProductFromModal(item)">
                        <svg v-if="(item.stock !== undefined ? item.stock : (item.soLuong || 0)) > 0" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                          stroke-width="2.5">
                          <line x1="12" y1="5" x2="12" y2="19"></line>
                          <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                        {{ (item.stock !== undefined ? item.stock : (item.soLuong || 0)) <= 0 ? 'Hết hàng' : 'Thêm' }}
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

    <transition name="fade">
      <div v-if="isVoucherModalOpen" class="pos-modal-overlay" @click.self="isVoucherModalOpen = false">
        <div class="pos-modal-dialog modal-voucher-dialog">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
                  <line x1="7" y1="7" x2="7.01" y2="7"></line>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Phiếu giảm giá khả dụng</h3>
                <span class="modal-badge-count">{{ availableEligibleVouchersCount }}/{{ allVouchers.length }} khả dụng</span>
              </div>
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
            <div class="modal-head-title">
              <div class="modal-title-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <path d="M7 16V4m0 0L3 8m4-4l4 4m6 4v12m0 0l4-4m-4 4l-4-4" />
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Đổi Size / Màu sắc sản phẩm</h3>
                <span class="modal-badge-count">{{ swapVariants.length }} biến thể</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isSwapModalOpen = false" title="Đóng (Esc)">×</button>
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

          <div class="pos-modal-footer cust-modal-footer swap-modal-footer">
            <button type="button" class="btn-cust-cancel" @click="isSwapModalOpen = false">
              Hủy bỏ
            </button>
            <button type="button" class="btn-confirm-swap-action"
              :disabled="!selectedNewVariant || selectedNewVariant.id === swapCartItem?.idCtsp"
              @click="executeSwap(selectedNewVariant)">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
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
        <div class="pos-modal-dialog modal-bank-qr-clean">
          <div v-if="isPaymentSuccessAnim" class="bank-success-overlay">
            <div class="bso-card">
              <div class="bso-icon-ring">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </div>
              <h4 class="bso-title">THANH TOÁN THÀNH CÔNG!</h4>
              <p class="bso-amount">{{ formatVnd(bankTransferAmount) }}</p>
              <span class="bso-memo">Mã đơn: {{ currentOrder?.code }}</span>
            </div>
          </div>

          <div class="pos-modal-head bank-clean-head">
            <div class="bank-clean-title-group">
              <div class="bank-clean-brand">
                <span class="viet-txt">Viet</span><span class="qr-txt">QR</span>
                <span class="napas-badge">Napas 24/7</span>
              </div>
              <h3 class="bank-clean-title">Thanh toán chuyển khoản</h3>
            </div>
            <button class="pos-modal-close" @click="isBankQrModalOpen = false" title="Đóng">×</button>
          </div>

          <div class="pos-modal-body bank-clean-body">
            <div class="bank-clean-grid">
              <!-- Cột trái: Mã QR thanh toán -->
              <div class="bank-clean-qr-col">
                <div class="bank-clean-qr-box">
                  <img
                    v-if="!isQrImgFailed && vietQrNetworkUrl"
                    :src="vietQrNetworkUrl"
                    @error="onQrImgError"
                    class="bank-clean-qr-img"
                    alt="Mã QR VietQR"
                  />
                  <img
                    v-else-if="bankQrDataUrl"
                    :src="bankQrDataUrl"
                    class="bank-clean-qr-img"
                    alt="Mã QR Chuyển khoản"
                  />
                </div>
                <div class="bank-clean-qr-sub">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 14 14"></polyline>
                  </svg>
                  <span>Quét bằng app ngân hàng hoặc ví điện tử</span>
                </div>
              </div>

              <!-- Cột phải: Số tiền & Chi tiết tài khoản nhận -->
              <div class="bank-clean-info-col">
                <!-- Số tiền thanh toán -->
                <div class="bank-clean-amount-box">
                  <span class="bank-clean-amount-label">Số tiền cần thanh toán</span>
                  <div class="bank-clean-amount-row">
                    <span class="bank-clean-amount-value">{{ formatVnd(bankTransferAmount) }}</span>
                    <button
                      class="btn-clean-copy-amount"
                      @click="copyToClipboard(bankTransferAmount, 'số tiền')"
                      title="Sao chép số tiền"
                    >
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                      </svg>
                      <span>Sao chép</span>
                    </button>
                  </div>
                </div>

                <!-- Bảng chi tiết 1 tài khoản -->
                <div class="bank-clean-details-card">
                  <div class="bank-clean-row">
                    <span class="clean-row-lbl">Hóa đơn:</span>
                    <span class="clean-row-val">{{ currentOrder?.title }} (<strong>{{ currentOrder?.code }}</strong>)</span>
                  </div>

                  <div class="bank-clean-row">
                    <span class="clean-row-lbl">Ngân hàng:</span>
                    <span class="clean-row-val">
                      <span class="mb-brand-pill">MB Bank</span>
                      <span class="bank-sub-name">Quân Đội</span>
                    </span>
                  </div>

                  <div class="bank-clean-row">
                    <span class="clean-row-lbl">Chủ tài khoản:</span>
                    <span class="clean-row-val strong">{{ defaultStoreBank.accountName }}</span>
                  </div>

                  <div class="bank-clean-row">
                    <span class="clean-row-lbl">Số tài khoản:</span>
                    <div class="clean-row-val-copy">
                      <span class="stk-number">{{ defaultStoreBank.accountNo }}</span>
                      <button
                        class="btn-clean-copy-mini"
                        @click="copyToClipboard(defaultStoreBank.accountNo, 'số tài khoản')"
                        title="Sao chép số tài khoản"
                      >
                        Sao chép
                      </button>
                    </div>
                  </div>

                  <div class="bank-clean-row">
                    <span class="clean-row-lbl">Nội dung CK:</span>
                    <div class="clean-row-val-copy">
                      <span class="memo-badge">{{ currentOrder?.code }}</span>
                      <button
                        class="btn-clean-copy-mini"
                        @click="copyToClipboard(currentOrder?.code, 'nội dung chuyển khoản')"
                        title="Sao chép nội dung"
                      >
                        Sao chép
                      </button>
                    </div>
                  </div>
                </div>

                <div class="bank-clean-tip">
                  <span class="tip-dot" :class="{ pulsing: isAutoCheckoutEnabled }"></span>
                  <span v-if="isAutoCheckoutEnabled && autoCheckoutCountdown > 0">
                    Đang tự động xác nhận & thanh toán sau <strong>{{ autoCheckoutCountdown }}s</strong>...
                  </span>
                  <span v-else>
                    Mã QR tự động điền đúng STK, số tiền & nội dung
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="pos-modal-foot bank-clean-foot">
            <button
              type="button"
              class="btn-toggle-auto-pay"
              :class="{ active: isAutoCheckoutEnabled }"
              @click="toggleAutoCheckout"
              title="Bật/Tắt tự động thanh toán sau 5 giây"
            >
              <span class="auto-dot" :class="{ pulsing: isAutoCheckoutEnabled }"></span>
              <span>{{ isAutoCheckoutEnabled ? `Tự động (${autoCheckoutCountdown}s)` : 'Tự động thanh toán (5s)' }}</span>
            </button>

            <button class="btn-clean-cancel" @click="isBankQrModalOpen = false">
              Đóng
            </button>
            <button
              class="btn-clean-confirm"
              :disabled="isProcessingCheckout"
              @click="confirmBankPaymentReceived"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              <span>{{ isProcessingCheckout ? 'Đang thanh toán...' : 'Đã nhận tiền - Hoàn tất hóa đơn' }}</span>
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
                <div class="receipt-bill-title">{{ completedInvoiceData.isDelivery ? 'PHIẾU GIAO HÀNG (POS)' : 'HÓA ĐƠN BÁN LẺ (POS)' }}</div>
                <div class="receipt-barcode-code">*{{ completedInvoiceData.code }}*</div>
              </div>

              <div class="receipt-meta-info">
                <div class="meta-row">
                  <span class="meta-k">Mã hóa đơn:</span>
                  <span class="meta-v"><strong>{{ completedInvoiceData.code }}</strong></span>
                </div>
                <div class="meta-row">
                  <span class="meta-k">Trạng thái:</span>
                  <span class="meta-v"><strong style="color: #2563eb;">{{ completedInvoiceData.trangThai || (completedInvoiceData.isDelivery ? 'Chờ xác nhận' : 'Đã hoàn thành') }}</strong></span>
                </div>
                <div class="meta-row">
                  <span class="meta-k">Thời gian:</span>
                  <span class="meta-v">{{ completedInvoiceData.date }}</span>
                </div>
                <div class="meta-row" v-if="completedInvoiceData.isDelivery">
                  <span class="meta-k">Hình thức:</span>
                  <span class="meta-v">Giao hàng tận nơi</span>
                </div>
                <div class="meta-row" v-if="completedInvoiceData.deliveryAddress">
                  <span class="meta-k">Địa chỉ nhận:</span>
                  <span class="meta-v">{{ completedInvoiceData.deliveryAddress }}</span>
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
            <button
              v-if="completedInvoiceData?.id"
              class="btn-receipt-view-invoice"
              type="button"
              @click="router.push(`/hoa-don/${completedInvoiceData.id}`)"
              title="Xem chi tiết đơn này trong Quản lý hóa đơn"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <span>Xem chi tiết hóa đơn</span>
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

    <!-- MODAL LỊCH SỬ ĐƠN VỪA BÁN TRONG CA & IN LẠI HÓA ĐƠN -->
    <transition name="fade">
      <div v-if="isRecentInvoicesModalOpen" class="pos-modal-overlay" @click.self="isRecentInvoicesModalOpen = false">
        <div class="pos-modal-dialog recent-invoices-modal-box">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Lịch sử đơn vừa bán trong ca</h3>
                <span class="modal-badge-count">{{ recentPosInvoices.length }} hóa đơn</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isRecentInvoicesModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body recent-invoices-modal-body">
            <div class="recent-invoices-toolbar">
              <div class="recent-search-wrap">
                <input
                  type="text"
                  class="recent-search-input"
                  v-model="recentInvoiceSearch"
                  placeholder="Tìm theo mã hóa đơn, tên khách hàng hoặc số điện thoại..."
                />
                <button v-if="recentInvoiceSearch" class="btn-clear-recent-search" @click="recentInvoiceSearch = ''">×</button>
              </div>
              <button
                v-if="recentPosInvoices.length > 0"
                type="button"
                class="btn-clear-recent-history"
                @click="clearRecentInvoicesHistory"
              >
                Xóa lịch sử ca
              </button>
            </div>

            <div v-if="filteredRecentInvoices.length > 0" class="recent-invoices-table-wrap">
              <table class="recent-invoices-table">
                <thead>
                  <tr>
                    <th width="120" class="text-center">MÃ HÓA ĐƠN</th>
                    <th width="150" class="text-center">THỜI GIAN</th>
                    <th class="text-center">KHÁCH HÀNG</th>
                    <th width="90" class="text-center">SỐ SP</th>
                    <th width="145" class="text-center">THANH TOÁN</th>
                    <th width="135" class="text-center">TỔNG TIỀN</th>
                    <th width="195" class="text-center">THAO TÁC</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="inv in filteredRecentInvoices" :key="inv.code">
                    <td class="text-center">
                      <span class="recent-inv-code">{{ inv.code }}</span>
                    </td>
                    <td class="text-center recent-inv-date">{{ inv.date }}</td>
                    <td class="text-center">
                      <div class="recent-cust-info">
                        <span class="recent-cust-name">{{ inv.customerName || 'Khách lẻ' }}</span>
                        <span class="recent-cust-phone" v-if="inv.customerPhone">{{ inv.customerPhone }}</span>
                        <span class="recent-cust-phone text-muted" v-else>Không có SĐT</span>
                      </div>
                    </td>
                    <td class="text-center">
                      <span class="recent-qty-pill">
                        {{ Array.isArray(inv.cart) ? inv.cart.reduce((s, i) => s + Number(i.quantity || 0), 0) : 0 }}
                      </span>
                    </td>
                    <td class="text-center">
                      <span class="recent-method-badge">{{ formatPaymentMethodName(inv.paymentMethod) }}</span>
                    </td>
                    <td class="text-center">
                      <strong class="recent-total-val">{{ formatVnd(inv.finalTotal) }}</strong>
                    </td>
                    <td class="text-center">
                      <div class="recent-actions-group">
                        <button
                          type="button"
                          class="btn-recent-view"
                          @click="viewRecentInvoiceReceipt(inv)"
                          title="Xem chi tiết hóa đơn"
                        >
                          Xem bill
                        </button>
                        <button
                          type="button"
                          class="btn-recent-reprint"
                          @click="reprintRecentInvoice(inv)"
                          title="In lại hóa đơn nhiệt 80mm ngay"
                        >
                          In lại
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div v-else class="recent-empty-state">
              <p class="recent-empty-title">Chưa có hóa đơn nào vừa thanh toán trong ca</p>
              <p class="recent-empty-sub">Các hóa đơn sau khi hoàn tất thanh toán tại quầy sẽ tự động lưu tại đây để bạn tra cứu hoặc in lại nhanh.</p>
            </div>
          </div>

          <div class="pos-modal-foot">
            <button class="btn-receipt-close" @click="isRecentInvoicesModalOpen = false">
              Đóng
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- MODAL MỞ CA LÀM VIỆC -->
    <transition name="modal-fade">
      <div v-if="isMoCaModalOpen" class="pos-modal-overlay" @click.self="isMoCaModalOpen = false">
        <div class="pos-modal-dialog pos-shift-dialog">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon is-shift-open">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"></path>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Mở ca làm việc mới</h3>
                <span class="modal-badge-count">Khai báo quỹ tiền mặt đầu ca tại quầy POS</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isMoCaModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="pos-shift-info-card">
              <div class="shift-info-item">
                <span class="shift-info-label">Thu ngân phụ trách:</span>
                <strong class="shift-info-val">{{ staffName }} ({{ staffRoleLabel }})</strong>
              </div>
              <div class="shift-info-item">
                <span class="shift-info-label">Thời gian mở:</span>
                <strong class="shift-info-val">{{ formatDateTime(new Date()) }}</strong>
              </div>
              <div class="shift-info-item">
                <span class="shift-info-label">Điểm bán hàng:</span>
                <span class="shift-info-val text-brand">Quầy POS #01</span>
              </div>
            </div>

            <div class="pos-shift-field-group">
              <label class="pos-shift-label">
                <span>Tiền mặt ban đầu trong két (Tiền thối)</span>
                <span class="pos-shift-required">*</span>
                <strong class="pos-shift-preview-val">{{ formatVnd(moCaForm.tienDauCa || 0) }}</strong>
              </label>
              <div class="pos-shift-input-wrap">
                <input
                  v-model.number="moCaForm.tienDauCa"
                  type="number"
                  min="0"
                  step="10000"
                  class="pos-shift-input"
                  placeholder="Nhập số tiền mặt có sẵn trong két..."
                  @keydown.enter="handleMoCaSubmit"
                  autofocus
                />
                <span class="pos-shift-currency-unit">VNĐ</span>
              </div>

              <!-- Nút chọn nhanh mệnh giá phổ biến -->
              <div class="pos-shift-quick-chips">
                <button type="button" class="btn-quick-chip" @click="moCaForm.tienDauCa = 0">0 ₫</button>
                <button type="button" class="btn-quick-chip" @click="moCaForm.tienDauCa = 200000">200.000 ₫</button>
                <button type="button" class="btn-quick-chip" :class="{ active: moCaForm.tienDauCa === 300000 }" @click="moCaForm.tienDauCa = 300000">300.000 ₫ (Gợi ý)</button>
                <button type="button" class="btn-quick-chip" @click="moCaForm.tienDauCa = 500000">500.000 ₫</button>
                <button type="button" class="btn-quick-chip" @click="moCaForm.tienDauCa = 1000000">1.000.000 ₫</button>
              </div>
            </div>

            <div class="pos-shift-field-group">
              <label class="pos-shift-label">Ghi chú mở ca (Tùy chọn):</label>
              <textarea
                v-model="moCaForm.ghiChu"
                class="pos-shift-textarea"
                rows="2"
                placeholder="Ví dụ: Nhận bàn giao két tiền từ ca trước, tiền mệnh giá lẻ 10k, 20k, 50k đầy đủ..."
              ></textarea>
            </div>
          </div>

          <div class="pos-modal-footer">
            <button type="button" class="btn-shift-cancel" @click="isMoCaModalOpen = false" :disabled="moCaSubmitting">
              Hủy bỏ
            </button>
            <button type="button" class="btn-shift-confirm-open" @click="handleMoCaSubmit" :disabled="moCaSubmitting">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              <span>{{ moCaSubmitting ? 'Đang mở ca...' : 'Xác nhận mở ca' }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- MODAL ĐÓNG CA & ĐỐI SOÁT TIỀN MẶT -->
    <transition name="modal-fade">
      <div v-if="isKetCaModalOpen" class="pos-modal-overlay" @click.self="isKetCaModalOpen = false">
        <div class="pos-modal-dialog pos-shift-dialog pos-close-shift-dialog">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon is-shift-close">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Kết thúc ca làm việc & Đối soát</h3>
                <span class="modal-badge-count">Ca: {{ activeShift?.maCa }} • Mở lúc: {{ formatDateTime(activeShift?.thoiGianMo) }}</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isKetCaModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body" v-if="ketCaLoading">
            <div class="shift-loading-box">
              <span class="shift-spinner"></span>
              <p>Đang tổng hợp số liệu doanh thu và hóa đơn trong ca...</p>
            </div>
          </div>

          <div class="pos-modal-body" v-else-if="ketCaSummary">
            <!-- KHỐI 1: TỔNG KẾT DOANH THU & TIỀN DỰ KIẾN KÉT -->
            <div class="shift-summary-grid">
              <div class="shift-sum-card is-start">
                <span class="sum-card-lbl">Tiền đầu ca (A)</span>
                <strong class="sum-card-val">{{ formatVnd(ketCaSummary.tienDauCa || 0) }}</strong>
                <span class="sum-card-sub">Khai báo lúc mở ca</span>
              </div>

              <div class="shift-sum-card is-cash">
                <span class="sum-card-lbl">Tiền mặt bán (B)</span>
                <strong class="sum-card-val text-success">+{{ formatVnd(ketCaSummary.tienMatThu || 0) }}</strong>
                <span class="sum-card-sub">Thu tiền mặt tại quầy</span>
              </div>

              <div class="shift-sum-card is-transfer">
                <span class="sum-card-lbl">Chuyển khoản / Khác</span>
                <strong class="sum-card-val text-primary">{{ formatVnd(ketCaSummary.tienKhongMat || 0) }}</strong>
                <span class="sum-card-sub">Đã vào tài khoản NH</span>
              </div>

              <div class="shift-sum-card is-expected">
                <span class="sum-card-lbl">TIỀN MẶT DỰ KIẾN KÉT (A + B)</span>
                <strong class="sum-card-val text-brand">{{ formatVnd(ketCaSummary.tienDuKien || 0) }}</strong>
                <span class="sum-card-sub">{{ ketCaSummary.tongHoaDon || 0 }} hóa đơn hoàn tất</span>
              </div>
            </div>

            <!-- KHỐI 2: KIỂM ĐẾM TIỀN MẶT THỰC TẾ & CHÊNH LỆCH -->
            <div class="pos-shift-count-box">
              <div class="count-box-head">
                <label class="pos-shift-label mb-0">
                  <span>Kiểm đếm tiền mặt thực tế trong két</span>
                  <span class="pos-shift-required">*</span>
                  <strong class="pos-shift-preview-val">{{ formatVnd(ketCaForm.tienThucTe || 0) }}</strong>
                </label>
                <button type="button" class="btn-match-du-kien" @click="ketCaForm.tienThucTe = Number(ketCaSummary.tienDuKien || 0)">
                  <span>Khớp dự kiến ({{ formatVnd(ketCaSummary.tienDuKien || 0) }})</span>
                </button>
              </div>

              <div class="pos-shift-input-wrap">
                <input
                  v-model.number="ketCaForm.tienThucTe"
                  type="number"
                  min="0"
                  step="1000"
                  class="pos-shift-input is-lg"
                  placeholder="Nhập tổng số tiền mặt kiểm đếm được..."
                />
                <span class="pos-shift-currency-unit">VNĐ</span>
              </div>

              <!-- THANH HIỂN THỊ CHÊNH LỆCH TÀI CHÍNH -->
              <div class="shift-diff-banner" :class="{
                'is-match': chenhLechKetCa === 0,
                'is-over': chenhLechKetCa > 0,
                'is-short': chenhLechKetCa < 0
              }">
                <div class="diff-banner-icon">
                  <svg v-if="chenhLechKetCa === 0" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                  <svg v-else-if="chenhLechKetCa > 0" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="18 15 12 9 6 15"></polyline></svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                </div>
                <div class="diff-banner-text">
                  <span class="diff-label" v-if="chenhLechKetCa === 0">Két tiền khớp hoàn hảo:</span>
                  <span class="diff-label" v-else-if="chenhLechKetCa > 0">Phát hiện thừa tiền két:</span>
                  <span class="diff-label" v-else>Phát hiện thiếu hụt tiền két:</span>
                  <strong class="diff-amount">{{ formatVnd(Math.abs(chenhLechKetCa)) }}</strong>
                  <span class="diff-note" v-if="chenhLechKetCa === 0">Số tiền mặt thực tế kiểm đếm khớp 100% với số liệu phần mềm.</span>
                  <span class="diff-note text-warning" v-else>Bắt buộc phải nhập lý do giải trình bên dưới để hoàn tất đóng ca.</span>
                </div>
              </div>

              <!-- BẮT BUỘC GIẢI TRÌNH KHI CHÊNH LỆCH KHÁC 0 -->
              <div class="pos-shift-field-group mt-3" v-if="chenhLechKetCa !== 0">
                <label class="pos-shift-label">
                  <span class="text-danger">Lý do giải trình chênh lệch tiền mặt</span>
                  <span class="pos-shift-required">*</span>
                </label>
                <input
                  v-model="ketCaForm.lyDoLech"
                  type="text"
                  class="pos-shift-input is-warning-border"
                  placeholder="Nhập chi tiết lý do thừa / thiếu tiền (VD: Khách không lấy 10k tiền lẻ, nhầm lẫn tiền thối...)"
                  required
                />
              </div>

              <div class="pos-shift-field-group mt-3">
                <label class="pos-shift-label">Ghi chú bàn giao ca (Tùy chọn):</label>
                <textarea
                  v-model="ketCaForm.ghiChu"
                  class="pos-shift-textarea"
                  rows="2"
                  placeholder="Ghi chú thêm thông tin bàn giao cho thu ngân ca sau..."
                ></textarea>
              </div>
            </div>

            <!-- KHỐI 3: DANH SÁCH ĐƠN HÀNG TRONG CA -->
            <div class="shift-bills-accordion mt-3" v-if="ketCaSummary.hoaDonGanDay && ketCaSummary.hoaDonGanDay.length > 0">
              <div class="bills-accordion-title">
                <span>Hóa đơn vừa tạo trong ca ({{ ketCaSummary.hoaDonGanDay.length }})</span>
              </div>
              <div class="bills-mini-table-wrap">
                <table class="bills-mini-table">
                  <thead>
                    <tr>
                      <th>Mã HĐ</th>
                      <th>Thời gian</th>
                      <th>Phương thức</th>
                      <th class="text-right">Tổng tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="hd in ketCaSummary.hoaDonGanDay" :key="hd.maHoaDon">
                      <td><strong class="code-link">{{ hd.maHoaDon }}</strong></td>
                      <td>{{ formatDateTime(hd.ngayTao) }}</td>
                      <td>{{ hd.tenPhuongThuc || 'Tiền mặt' }}</td>
                      <td class="text-right font-weight-bold">{{ formatVnd(hd.tienSauGiamGia) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="pos-modal-footer">
            <button type="button" class="btn-shift-cancel" @click="isKetCaModalOpen = false" :disabled="ketCaSubmitting">
              Hủy / Quay lại
            </button>
            <button type="button" class="btn-shift-confirm-close" @click="handleKetCaSubmit" :disabled="ketCaSubmitting || (chenhLechKetCa !== 0 && !ketCaForm.lyDoLech?.trim())">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              <span>{{ ketCaSubmitting ? 'Đang đóng ca...' : 'Xác nhận đóng ca' }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<style scoped src="@/assets/styles/PosSalesPage.css"></style>
