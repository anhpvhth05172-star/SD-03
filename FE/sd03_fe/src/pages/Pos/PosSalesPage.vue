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
import { getDangMo, moCa, getTongKet, ketCa, getDanhSachCaLam } from '@/api/giaoCa'
import { inBienLaiDoiSoatK80 } from '@/utils/printShiftReceipt'
import PaginationBar from '@/components/PaginationBar.vue'

import { getUser, dangXuat } from '@/utils/auth'
import { datVaiTro, vaiTroHienTai, laAdmin } from '@/utils/session'

const API_BASE = '/api/v1'


const router = useRouter()

// --- QUẢN LÝ CA LÀM VIỆC (MỞ CA / ĐÓNG CA TRÊN POS) ---
const activeShift = ref(null)
const isLoadingShift = ref(false)
const isCheDoXem = computed(() => !laAdmin() && !activeShift.value?.id)

const kiemTraQuyenBanHang = (actionName = 'thêm sản phẩm vào giỏ hàng') => {
  if (laAdmin()) return true
  if (!activeShift.value?.id) {
    notifyWarning(
      'Chưa mở ca làm việc',
      `Bạn đang ở Chế độ xem. Vui lòng mở ca làm việc để ${actionName} và bán hàng tại quầy!`
    )
    openMoCaModal()
    return false
  }
  return true
}

const isMoCaModalOpen = ref(false)
const dsCaChiaSan = ref([])
const isLoadingCaChiaSan = ref(false)
const moCaForm = reactive({
  idCaLich: null,
  tienDauCa: '',
  ghiChu: ''
})
const moCaSubmitting = ref(false)

const selectedCa = computed(() => {
  return dsCaChiaSan.value.find((c) => c.id === moCaForm.idCaLich) || null
})

const isKetCaModalOpen = ref(false)
const ketCaSummary = ref(null)
const ketCaLoading = ref(false)
const ketCaSubmitting = ref(false)
const ketCaForm = reactive({
  tienThucTe: 0,
  tienChi: 0,
  lyDoLech: '',
  ghiChu: ''
})

const tienMatDuKienSauChi = computed(() =>
  Math.max(0, Number(ketCaSummary.value?.tienDuKien || 0) - Number(ketCaForm.tienChi || 0))
)

const chenhLechKetCa = computed(() => {
  const tienDuKien = tienMatDuKienSauChi.value
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

const openMoCaModal = async () => {
  moCaForm.tienDauCa = ''
  moCaForm.ghiChu = ''
  isLoadingCaChiaSan.value = true
  isMoCaModalOpen.value = true
  try {
    const list = await getDanhSachCaLam()
    dsCaChiaSan.value = list || []
    if (list && list.length > 0) {
      if (!moCaForm.idCaLich || !list.some((c) => c.id === moCaForm.idCaLich)) {
        moCaForm.idCaLich = list[0].id
      }
    }
  } catch (err) {
    console.warn('Lỗi tải danh sách ca làm:', err)
  } finally {
    isLoadingCaChiaSan.value = false
  }
}

const handleMoCaSubmit = async () => {
  const amount = Number(moCaForm.tienDauCa || 0)
  if (amount < 0) {
    notifyWarning('Số tiền không hợp lệ', 'Tiền mặt đầu ca không được âm')
    return
  }
  if (!moCaForm.idCaLich && dsCaChiaSan.value.length > 0) {
    notifyWarning('Chưa chọn ca làm việc', 'Vui lòng chọn một ca được chia sẵn để bắt đầu ca làm!')
    return
  }
  moCaSubmitting.value = true
  try {
    const res = await moCa({
      idCaLich: moCaForm.idCaLich,
      tienDauCa: amount,
      ghiChu: moCaForm.ghiChu?.trim() || null
    })
    activeShift.value = res
    isMoCaModalOpen.value = false
    const tenCa = res.tenCaLich || selectedCa.value?.ten || 'ca làm việc'
    notifySuccess('Mở ca thành công', `Đã mở ${tenCa} (${res.maCa}). Bắt đầu phiên bán hàng!`)
    window.dispatchEvent(new Event('polyshoes:shift-changed'))
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
    ketCaForm.tienChi = 0
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

    const closedResult = await ketCa(closedCaId, {
      tienThucTe: Number(ketCaForm.tienThucTe || 0),
      tienChi: Number(ketCaForm.tienChi || 0),
      lyDoLech: ketCaForm.lyDoLech?.trim() || null,
      ghiChu: ketCaForm.ghiChu?.trim() || null
    })
    isKetCaModalOpen.value = false
    activeShift.value = null
    notifySuccess('Đóng ca thành công', 'Ca làm việc đã được kết thúc và khóa sổ đối soát.')
    window.dispatchEvent(new Event('polyshoes:shift-changed'))

    const wantPrint = await notifyConfirm(
      'In biên lai đối soát ca?',
      'Đã đóng ca thành công! Bạn có muốn in ngay Biên lai đối soát (khổ nhiệt K80) để kẹp két tiền không?',
      'In biên lai ngay',
      'Xem chi tiết ca'
    )
    if (wantPrint) {
      inBienLaiDoiSoatK80({
        ...closedCa,
        tenNguoiNhan: closedResult?.tenNguoiNhan || 'Cuối ngày',
        tienChi: Number(closedForm.tienChi || 0),
        tienDuKien: tienMatDuKienSauChi.value,
        tienThucTe: Number(closedForm.tienThucTe || 0),
        lyDoLech: closedForm.lyDoLech,
        chenhLech: diff,
        thoiGianKet: new Date()
      }, closedSummary)
    }
    router.push(`/ca/${closedCaId}/doi-soat`)
  } catch (err) {
    const msg = err.message || ''
    if (msg.includes('CON_HD_DANG_XU_LY')) {
      notifyWarning(
        'Còn đơn hàng chưa hoàn tất',
        'Ca làm việc vẫn còn đơn hàng tại quầy chưa được thanh toán hoặc hủy. Vui lòng kiểm tra lại trước khi đóng ca!'
      )
    } else {
      notifyError('Kết ca thất bại', msg || 'Không thể đóng ca làm việc')
    }
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
  } catch {
    // Cache session có thể bị chặn; trạng thái ca sẽ được nạp lại từ API.
  }
  return false
}

const saveStaffSnapshotToStorage = () => {
  try {
    sessionStorage.setItem(POS_STAFF_SNAPSHOT_KEY, JSON.stringify(Array.from(staffSnapshotMap.entries())))
  } catch {
    // Cache session là tùy chọn, không ảnh hưởng dữ liệu bán hàng trên máy chủ.
  }
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

const syncCartPricesWithCatalog = (sourceList = allProducts.value) => {
  if (!Array.isArray(pendingOrders.value) || !Array.isArray(sourceList) || sourceList.length === 0) return
  const prodMap = new Map(sourceList.map(p => [Number(p.id), p]))
  let hasPriceChange = false

  for (const order of pendingOrders.value) {
    if (!Array.isArray(order.cart)) continue
    for (const item of order.cart) {
      const freshProd = prodMap.get(Number(item.idCtsp))
      if (!freshProd) continue

      const freshOriginalPrice = Number(freshProd.price || freshProd.giaBan || 0)
      const freshEffectivePrice = getEffectivePrice(freshProd)
      const freshDisc = freshProd.discount || (freshProd.phanTramGiam ? `-${Math.round(freshProd.phanTramGiam)}%` : null)
      const freshGiaSauGiam = freshProd.giaSauGiam !== undefined && freshProd.giaSauGiam !== null ? Number(freshProd.giaSauGiam) : null
      const freshPhanTramGiam = freshProd.phanTramGiam !== undefined && freshProd.phanTramGiam !== null ? Number(freshProd.phanTramGiam) : null

      if (
        item.price !== freshEffectivePrice ||
        item.originalPrice !== freshOriginalPrice ||
        item.discount !== freshDisc ||
        item.giaSauGiam !== freshGiaSauGiam ||
        item.phanTramGiam !== freshPhanTramGiam
      ) {
        item.originalPrice = freshOriginalPrice
        item.price = freshEffectivePrice
        item.discount = freshDisc
        item.giaSauGiam = freshGiaSauGiam
        item.phanTramGiam = freshPhanTramGiam
        hasPriceChange = true
      }
    }
  }

  if (hasPriceChange) {
    savePendingOrdersToStorage()
  }
}

const setServerProductsList = (rawList) => {
  if (!Array.isArray(rawList)) return
  rawList.forEach(p => {
    p._serverStock = Number(p.stock !== undefined ? p.stock : (p.soLuong || 0))
  })
  allProducts.value = rawList
  syncCartPricesWithCatalog(rawList)
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
      customCashGiven: Boolean(o.customCashGiven),
      userRemovedVoucher: Boolean(o.userRemovedVoucher),
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
    pendingOrders.value = parsed.map((o, idx) => ({
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
  const isSilentBool = typeof silent === 'boolean' && silent
  if (!isSilentBool && !kiemTraQuyenBanHang('tạo hóa đơn mới')) return


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
    userRemovedVoucher: false,
    customCashGiven: false,
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

  let discount
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
  if (!kiemTraQuyenBanHang('thêm sản phẩm vào giỏ hàng')) return
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
    existingItem.price = effectivePrice
    existingItem.originalPrice = Number(productVariant.price || productVariant.giaBan || 0)
    existingItem.discount = productVariant.discount || (productVariant.phanTramGiam ? `-${Math.round(productVariant.phanTramGiam)}%` : null)
    existingItem.phanTramGiam = productVariant.phanTramGiam !== undefined ? productVariant.phanTramGiam : null
    existingItem.giaSauGiam = productVariant.giaSauGiam !== undefined ? productVariant.giaSauGiam : null
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
      discount: productVariant.discount || (productVariant.phanTramGiam ? `-${Math.round(productVariant.phanTramGiam)}%` : null),
      phanTramGiam: productVariant.phanTramGiam !== undefined ? productVariant.phanTramGiam : null,
      giaSauGiam: productVariant.giaSauGiam !== undefined ? productVariant.giaSauGiam : null,
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
  if (!kiemTraQuyenBanHang('chỉnh sửa giỏ hàng')) return
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
      if (event?.target) event.target.value = 1
      item.quantity = 1
      recalculateAvailableStocks()
      savePendingOrdersToStorage()
      return
    }
  }

  // Nếu ô trống (vừa xóa trắng để gõ số mới rồi click ra ngoài) hoặc không hợp lệ: khôi phục về 1
  if (isNaN(newQty) || newQty < 1) {
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
  if (!kiemTraQuyenBanHang('xóa sản phẩm')) return
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
    } catch {
      // Tiếp tục tra cứu mã bằng các nguồn dữ liệu cục bộ.
    }
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

const handleCashGivenInput = (e) => {
  if (!currentOrder.value) return
  currentOrder.value.customCashGiven = true
  currentOrder.value.cashGiven = e.target.value === '' ? 0 : Math.max(0, Number(e.target.value))
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
  if (!kiemTraQuyenBanHang('chọn khách hàng')) return
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

// Nhóm biến thể theo Sản phẩm cha (mỗi sản phẩm chỉ hiển thị 1 dòng đại diện)
const activePickerProduct = ref(null)
const isVariantPickerModalOpen = ref(false)
const selectedPickerVariant = ref(null)
const pickerColorFilter = ref('ALL')

const openVariantPickerForProduct = (prodGroup) => {
  if (!prodGroup) return
  activePickerProduct.value = prodGroup
  selectedPickerVariant.value = null
  pickerColorFilter.value = 'ALL'
  isVariantPickerModalOpen.value = true
}

const closeVariantPickerModal = () => {
  isVariantPickerModalOpen.value = false
  activePickerProduct.value = null
  selectedPickerVariant.value = null
}

const availablePickerColors = computed(() => {
  if (!activePickerProduct.value) return []
  const set = new Set()
  activePickerProduct.value.variants.forEach(v => {
    const c = v.color || v.tenMau
    if (c) set.add(c)
  })
  return Array.from(set)
})

const filteredPickerVariants = computed(() => {
  if (!activePickerProduct.value) return []
  let list = activePickerProduct.value.variants
  if (pickerColorFilter.value !== 'ALL') {
    list = list.filter(v => (v.color || v.tenMau) === pickerColorFilter.value)
  }
  return list
})

const addVariantFromPicker = (variant) => {
  if (!kiemTraQuyenBanHang('thêm sản phẩm vào giỏ')) return
  if (!variant) return
  const stock = getRemainingStock(variant.id)
  if (stock <= 0) {
    notifyWarning('Hết hàng', 'Biến thể này hiện đã hết tồn kho khả dụng!')
    return
  }
  addToCart(variant, 1, false)
  closeVariantPickerModal()
}

const filteredModalProducts = computed(() => {
  const _cartTrigger = pendingOrders.value.map(o => (o.cart || []).map(i => `${i.idCtsp}:${i.quantity}`).join(',')).join(';')

  // Lấy toàn bộ biến thể còn tồn kho khả dụng > 0
  let variantList = (allProducts.value || []).filter(p => {
    return getRemainingStock(p.id) > 0
  })

  const term = modalFilters.search.trim().toLowerCase()
  if (term) {
    variantList = variantList.filter(p =>
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
    variantList = variantList.filter(p => String(p.idThuongHieu) === String(modalFilters.idThuongHieu) || (p.tenThuongHieu && p.tenThuongHieu === modalFilters.idThuongHieu))
  }

  if (modalFilters.idLoaiGiay) {
    variantList = variantList.filter(p => String(p.idLoaiGiay) === String(modalFilters.idLoaiGiay) || (p.tenLoaiGiay && p.tenLoaiGiay === modalFilters.idLoaiGiay))
  }

  if (modalFilters.idMauSac) {
    variantList = variantList.filter(p => String(p.idMauSac) === String(modalFilters.idMauSac) || (p.color && p.color === modalFilters.idMauSac) || (p.tenMau && p.tenMau === modalFilters.idMauSac))
  }

  if (modalFilters.idKichCo) {
    variantList = variantList.filter(p => String(p.idKichCo) === String(modalFilters.idKichCo) || (p.size && p.size === modalFilters.idKichCo) || (p.tenKichCo && p.tenKichCo === modalFilters.idKichCo))
  }

  if (modalFilters.priceRange) {
    variantList = variantList.filter(p => {
      const pr = getEffectivePrice(p)
      if (modalFilters.priceRange === '<1m') return pr < 1000000
      if (modalFilters.priceRange === '1m-3m') return pr >= 1000000 && pr <= 3000000
      if (modalFilters.priceRange === '3m-5m') return pr > 3000000 && pr <= 5000000
      if (modalFilters.priceRange === '>5m') return pr > 5000000
      return true
    })
  }

  // Nhóm các biến thể theo sản phẩm cha: mỗi sản phẩm chỉ xuất hiện 1 dòng duy nhất
  const groupsMap = new Map()
  variantList.forEach(v => {
    const spKey = v.idSanPham ? `id_${v.idSanPham}` : (v.maSp ? `masp_${v.maSp}` : (v.tenSp || v.tenSanPham || `item_${v.id}`))
    if (!groupsMap.has(spKey)) {
      groupsMap.set(spKey, {
        key: spKey,
        idSanPham: v.idSanPham || null,
        maSp: v.maSp || v.maSanPham || (v.maCtsp ? v.maCtsp.split('-')[0] : 'SP---'),
        tenSp: v.tenSp || v.tenSanPham || 'Sản phẩm',
        img: getShoeImage(v),
        tenThuongHieu: v.tenThuongHieu || '',
        tenLoaiGiay: v.tenLoaiGiay || '',
        variants: [],
        totalStock: 0,
        minPrice: Infinity,
        maxPrice: -Infinity,
        colors: new Set(),
        sizes: new Set()
      })
    }
    const grp = groupsMap.get(spKey)
    grp.variants.push(v)
    const stock = getRemainingStock(v.id)
    grp.totalStock += stock
    const effPrice = getEffectivePrice(v)
    if (effPrice < grp.minPrice) grp.minPrice = effPrice
    if (effPrice > grp.maxPrice) grp.maxPrice = effPrice
    if (v.color || v.tenMau) grp.colors.add(v.color || v.tenMau)
    if (v.size || v.tenKichCo) grp.sizes.add(v.size || v.tenKichCo)
  })

  const productList = []
  groupsMap.forEach(grp => {
    if (grp.minPrice === Infinity) grp.minPrice = 0
    if (grp.maxPrice === -Infinity) grp.maxPrice = grp.minPrice
    grp.colorsList = Array.from(grp.colors)
    grp.sizesList = Array.from(grp.sizes).sort((a, b) => Number(a) - Number(b))
    productList.push(grp)
  })

  return productList
})

const modalTotalElements = computed(() => filteredModalProducts.value.length)
const modalTotalPages = computed(() => Math.max(1, Math.ceil(modalTotalElements.value / modalPageSize.value)))

const paginatedModalProducts = computed(() => {
  const start = modalCurrentPage.value * modalPageSize.value
  return filteredModalProducts.value.slice(start, start + modalPageSize.value)
})

const changeModalPage = (p) => {
  if (p >= 0 && p < modalTotalPages.value) {
    modalCurrentPage.value = p
  }
}

watch([modalFilters, modalPageSize], () => {
  modalCurrentPage.value = 0
}, { deep: true })

const openProductPicker = async () => {
  if (!currentOrder.value) {
    createNewOrder()
  }
  resetModalFilters()
  isProductModalOpen.value = true
  await handleDiscountChanged()
}

const closeProductPicker = () => {
  isProductModalOpen.value = false
  syncCartPricesWithCatalog()
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

// Chế độ quét liên tục nhiều mã
const lastScannedTimeMap = new Map()
const scannedSessionList = ref([])
const scanNotificationToast = ref('')
let scanToastTimer = null

const totalScannedSessionItems = computed(() => {
  return scannedSessionList.value.reduce((sum, item) => sum + (Number(item.count) || 0), 0)
})

const parseScannedCodes = (rawText) => {
  if (!rawText) return []
  const text = String(rawText).trim()

  // 1. JSON Array: e.g. ["SP01", "SP02"]
  if (text.startsWith('[') && text.endsWith(']')) {
    try {
      const arr = JSON.parse(text)
      if (Array.isArray(arr)) {
        return arr.map(item => {
          if (typeof item === 'string') return item.trim()
          if (item && typeof item === 'object') return item.maCtsp || item.ma || item.id || ''
          return String(item).trim()
        }).filter(Boolean)
      }
    } catch {
      // Chuỗi không phải JSON; tiếp tục thử các định dạng mã khác.
    }
  }

  // 2. JSON Object with array
  if (text.startsWith('{') && text.endsWith('}')) {
    try {
      const parsed = JSON.parse(text)
      if (Array.isArray(parsed.items || parsed.codes || parsed.list)) {
        const list = parsed.items || parsed.codes || parsed.list
        return list.map(x => typeof x === 'string' ? x.trim() : (x?.maCtsp || x?.ma || String(x?.id || ''))).filter(Boolean)
      }
      return [text]
    } catch {
      // Chuỗi không phải JSON; giữ nguyên văn bản để xử lý như mã quét.
    }
  }

  // 3. Phân tách bằng dấu phẩy, chấm phẩy, xuống dòng
  if (text.includes('\n') || text.includes(',') || text.includes(';')) {
    const rawTokens = text.split(/[\n,;]+/).map(t => t.trim()).filter(Boolean)
    if (rawTokens.length > 1) {
      return rawTokens
    }
  }

  return [text]
}

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
  } catch {
    // Âm báo mã QR là tùy chọn trên trình duyệt không hỗ trợ Web Audio.
  }
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
    } catch {
      // Chuỗi quét không phải JSON; tiếp tục so khớp trực tiếp mã thô.
    }
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
  if (!decodedText) return

  const codes = parseScannedCodes(decodedText)
  if (codes.length === 0) return

  // Chống lặp quét cùng 1 mã trong thời gian ngắn (cooldown 1.2s)
  const now = Date.now()
  const validCodes = []
  for (const c of codes) {
    const key = String(c).trim().toLowerCase()
    const lastTime = lastScannedTimeMap.get(key) || 0
    if (now - lastTime >= 1200) {
      lastScannedTimeMap.set(key, now)
      validCodes.push(c)
    }
  }

  if (validCodes.length === 0) return

  let newlyAddedCount = 0
  let lastProduct = null

  for (const code of validCodes) {
    let found = findProductByScannedCode(code)

    if (!found) {
      try {
        const trimmed = String(code).trim()
        let searchKey = trimmed
        if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
          try {
            const parsed = JSON.parse(trimmed)
            searchKey = parsed.maCtsp || parsed.ma || parsed.id || trimmed
          } catch {
            // Không phải JSON; dùng nguyên chuỗi làm mã tìm kiếm.
          }
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
      const stock = getRemainingStock(found.id)
      if (stock <= 0) {
        notifyWarning('Hết hàng', `Sản phẩm "${found.tenSp || found.tenSanPham}" đã hết tồn kho khả dụng!`)
        continue
      }

      addToCart(found, 1)
      newlyAddedCount++
      lastProduct = found

      // Cập nhật danh sách các sản phẩm quét trong phiên
      const existing = scannedSessionList.value.find(item => item.id === found.id)
      if (existing) {
        existing.count += 1
      } else {
        scannedSessionList.value.unshift({
          id: found.id,
          maCtsp: found.maCtsp || found.maChiTietSanPham || found.ma,
          tenSp: found.tenSp || found.tenSanPham,
          size: found.size || found.tenKichCo,
          color: found.color || found.tenMau,
          price: found.giaBan || 0,
          count: 1
        })
      }
    } else {
      notifyWarning('Không tìm thấy', `Không tìm thấy sản phẩm có mã: "${code}"`)
    }
  }

  if (newlyAddedCount > 0 && lastProduct) {
    playScannerBeep()
    scanSuccessAnim.value = true
    setTimeout(() => { scanSuccessAnim.value = false }, 350)

    const prodName = `${lastProduct.tenSp || lastProduct.tenSanPham} (${lastProduct.color || lastProduct.tenMau || ''} - Size ${lastProduct.size || lastProduct.tenKichCo || ''})`
    scanNotificationToast.value = newlyAddedCount === 1
      ? `+1 ${prodName}`
      : `Đã thêm ${newlyAddedCount} sản phẩm vào giỏ`

    if (scanToastTimer) clearTimeout(scanToastTimer)
    scanToastTimer = setTimeout(() => {
      scanNotificationToast.value = ''
    }, 2200)
  }
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
          } catch {
            // Bỏ qua track đã được trình duyệt giải phóng trong lúc đóng camera.
          }
        })
        v.srcObject = null
      }
      try {
        v.pause()
        v.removeAttribute('src')
        v.load()
      } catch {
        // Bỏ qua lỗi dọn video nếu phần tử đã bị tháo khỏi DOM.
      }
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
    } catch {
      // Scanner có thể đã tự dọn tài nguyên khi camera đóng.
    }
    html5QrScanner = null
  }

  isCameraRunning.value = false
}

const openQrScanner = async () => {
  if (!kiemTraQuyenBanHang('quét mã')) return
  isQrModalOpen.value = true
  cameraError.value = ''
  scannedCodeInput.value = ''
  scannedSessionList.value = []
  scanNotificationToast.value = ''
  lastScannedTimeMap.clear()
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
    // Tự động hoàn tất hóa đơn ngay lập tức vào database và tự động in hóa đơn
    await handleCheckout({ skipConfirm: true, autoPrint: true })
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
      // API địa giới trực tiếp không khả dụng thì thử nguồn vận chuyển phía dưới.
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
      // Tiếp tục với lựa chọn khu vực dự phòng nếu API trả lỗi.
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
      toWardCode: '001'
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

const hasCustomerUsedVoucher = (v, custId) => {
  if (!v || !custId) return false
  const targetId = Number(custId)
  if (Array.isArray(v.idKhachHangsDaDung)) {
    return v.idKhachHangsDaDung.some(id => Number(id) === targetId)
  }
  return false
}

const isVoucherAssignedToCustomer = (v, custId) => {
  if (!v || !custId) return false
  const targetId = Number(custId)
  if (Array.isArray(v.idKhachHangsChuaDung)) {
    return v.idKhachHangsChuaDung.some(id => Number(id) === targetId)
  }
  return false
}

const filteredVouchers = computed(() => {
  const q = voucherSearchQuery.value.trim().toLowerCase()
  const custId = currentOrder.value?.customer?.id ? Number(currentOrder.value.customer.id) : null

  return allVouchers.value.filter(v => {
    // Với phiếu giảm giá cá nhân: chỉ hiển thị nếu khách hàng được cấp (kể cả đã dùng để hiện lý do Không dùng được)
    const isCaNhan = v.phamVi === 'CA_NHAN' || v.scope === 'CA_NHAN'
    if (isCaNhan) {
      if (!custId) return false
      if (!isVoucherAssignedToCustomer(v, custId) && !hasCustomerUsedVoucher(v, custId)) {
        return false
      }
    }
    if (!q) return true
    return (
      (v.ma && v.ma.toLowerCase().includes(q)) ||
      (v.ten && v.ten.toLowerCase().includes(q)) ||
      (v.maPhieuGiamGia && v.maPhieuGiamGia.toLowerCase().includes(q))
    )
  })
})

const calculateVoucherDiscount = (voucher, currentSubTotal) => {
  if (!voucher) return 0
  const sub = currentSubTotal !== undefined ? currentSubTotal : subTotal.value
  const minRequired = Number(voucher.hoaDonToiThieu || 0)
  if (sub < minRequired) return 0

  let discount
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

const isVoucherEligible = (v, order = currentOrder.value) => {
  if (!v || !order) return false
  if (v.trangThai === false) return false

  const custId = order.customer?.id ? Number(order.customer.id) : null
  const isCaNhan = v.phamVi === 'CA_NHAN' || v.scope === 'CA_NHAN'
  if (isCaNhan) {
    if (!custId) return false
    if (!isVoucherAssignedToCustomer(v, custId)) {
      return false
    }
  }

  // Khách hàng đã dùng hết lượt phiếu này (dù là công khai hay cá nhân)
  if (custId && hasCustomerUsedVoucher(v, custId)) {
    return false
  }

  const now = new Date()
  if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false
  if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false
  if (v.soLuong && v.soLuongDaSuDung !== undefined && v.soLuongDaSuDung >= v.soLuong) return false

  const minRequired = Number(v.hoaDonToiThieu || 0)
  const currentSub = order.cart ? order.cart.reduce((sum, item) => sum + (Number(item.price || 0) * Number(item.quantity || 0)), 0) : subTotal.value
  return currentSub >= minRequired
}

const availableEligibleVouchersCount = computed(() => {
  return allVouchers.value.filter(v => isVoucherEligible(v)).length
})

const getBestVoucherForOrder = (order = currentOrder.value) => {
  if (!order || !order.cart || order.cart.length === 0) return null
  const currentSub = order.cart.reduce((sum, item) => sum + (Number(item.price || 0) * Number(item.quantity || 0)), 0)
  if (currentSub <= 0 || allVouchers.value.length === 0) return null

  const validVouchers = allVouchers.value.filter(v => isVoucherEligible(v, order))
  if (validVouchers.length === 0) return null

  const sorted = [...validVouchers].sort((a, b) => {
    const discA = calculateVoucherDiscount(a, currentSub)
    const discB = calculateVoucherDiscount(b, currentSub)
    if (discB !== discA) return discB - discA
    return (Number(a.hoaDonToiThieu || 0)) - (Number(b.hoaDonToiThieu || 0))
  })

  const best = sorted[0]
  const discountVal = calculateVoucherDiscount(best, currentSub)
  if (discountVal <= 0) return null

  return {
    voucher: best,
    discount: discountVal
  }
}

const bestVoucherSuggestion = computed(() => {
  return getBestVoucherForOrder(currentOrder.value)
})

const nextVoucherSuggestion = computed(() => {
  if (!currentOrder.value || subTotal.value <= 0 || allVouchers.value.length === 0) return null
  const now = new Date()
  const custId = currentOrder.value?.customer?.id ? Number(currentOrder.value.customer.id) : null
  const eligibleHigher = allVouchers.value.filter(v => {
    if (v.trangThai === false) return false
    const isCaNhan = v.phamVi === 'CA_NHAN' || v.scope === 'CA_NHAN'
    if (isCaNhan) {
      if (!custId) return false
      if (!isVoucherAssignedToCustomer(v, custId)) return false
    }
    if (custId && hasCustomerUsedVoucher(v, custId)) return false
    if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false
    if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false
    if (v.soLuong && v.soLuongDaSuDung !== undefined && v.soLuongDaSuDung >= v.soLuong) return false
    const minRequired = Number(v.hoaDonToiThieu || 0)
    return minRequired > subTotal.value
  })

  if (eligibleHigher.length === 0) return null

  eligibleHigher.sort((a, b) => {
    const diffA = Number(a.hoaDonToiThieu || 0) - subTotal.value
    const diffB = Number(b.hoaDonToiThieu || 0) - subTotal.value
    if (diffA !== diffB) return diffA - diffB
    const potA = calculateVoucherDiscount(a, Number(a.hoaDonToiThieu || 0))
    const potB = calculateVoucherDiscount(b, Number(b.hoaDonToiThieu || 0))
    return potB - potA
  })

  const target = eligibleHigher[0]
  const neededMore = Number(target.hoaDonToiThieu || 0) - subTotal.value
  const potentialDiscount = calculateVoucherDiscount(target, Number(target.hoaDonToiThieu || 0))
  const currentDiscount = discountAmount.value
  const extraSaving = potentialDiscount > currentDiscount ? potentialDiscount - currentDiscount : 0

  return {
    voucher: target,
    neededMore,
    minRequired: Number(target.hoaDonToiThieu || 0),
    potentialDiscount,
    extraSaving
  }
})

// Tự động áp dụng phiếu giảm giá tốt nhất đủ điều kiện (hoàn toàn tự động)
watch([() => subTotal.value, () => bestVoucherSuggestion.value, () => currentOrder.value?.customer?.id], ([newSubTotal, newBest], [, , oldCustId]) => {
  if (!currentOrder.value) return
  if (newSubTotal <= 0) {
    currentOrder.value.selectedVoucher = null
    currentOrder.value.userSelectedVoucherManually = false
    currentOrder.value.userRemovedVoucher = false
    return
  }

  // Nếu thay đổi khách hàng, cho phép tự động tính lại voucher tốt nhất cho khách mới
  const currentCustId = currentOrder.value?.customer?.id
  if (currentCustId !== oldCustId) {
    currentOrder.value.userSelectedVoucherManually = false
    currentOrder.value.userRemovedVoucher = false
  }

  // 1. Nếu voucher đang chọn không còn hợp lệ: tự động chuyển về voucher tốt nhất hiện tại
  if (currentOrder.value.selectedVoucher && !isVoucherEligible(currentOrder.value.selectedVoucher)) {
    currentOrder.value.selectedVoucher = newBest?.voucher || null
    currentOrder.value.userSelectedVoucherManually = false
    currentOrder.value.userRemovedVoucher = false
    return
  }

  // 2. Nếu người dùng đã chủ động chọn voucher này và nó vẫn hợp lệ: giữ nguyên
  if (currentOrder.value.userSelectedVoucherManually && currentOrder.value.selectedVoucher) {
    return
  }

  // 3. Tự động áp dụng mã tốt nhất và nâng cấp mốc cao hơn khi đạt điều kiện
  if (!currentOrder.value.selectedVoucher) {
    if (newBest?.voucher) {
      currentOrder.value.selectedVoucher = newBest.voucher
      currentOrder.value.userSelectedVoucherManually = false
    }
  } else if (newBest?.voucher) {
    if (currentOrder.value.selectedVoucher.id !== newBest.voucher.id) {
      currentOrder.value.selectedVoucher = newBest.voucher
    }
  }
}, { immediate: true })

// Mặc định ô tiền khách đưa bằng tổng tiền cần thanh toán
watch(finalTotal, (newTotal) => {
  if (currentOrder.value && !currentOrder.value.customCashGiven) {
    currentOrder.value.cashGiven = newTotal
  }
}, { immediate: true })

watch(activeOrderIndex, () => {
  if (currentOrder.value && !currentOrder.value.customCashGiven) {
    currentOrder.value.cashGiven = finalTotal.value
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
  if (!v) return 'Mã giảm giá không tồn tại.'
  if (v.trangThai === false) {
    return 'Mã giảm giá đang bị tắt hoặc đã hết hạn.'
  }
  const now = new Date()
  if (v.ngayBatDau && new Date(v.ngayBatDau) > now) {
    return `Chưa đến thời gian áp dụng (bắt đầu từ ${formatDateVn(v.ngayBatDau)}).`
  }
  if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) {
    return `Mã giảm giá đã hết hạn vào ngày ${formatDateVn(v.ngayKetThuc)}.`
  }
  if (v.soLuong && v.soLuongDaSuDung !== undefined && v.soLuongDaSuDung >= v.soLuong) {
    return 'Đã hết lượt sử dụng.'
  }
  const custId = currentOrder.value?.customer?.id ? Number(currentOrder.value.customer.id) : null
  const isCaNhan = v.phamVi === 'CA_NHAN' || v.scope === 'CA_NHAN'
  if (isCaNhan && !custId) {
    return 'Cần chọn khách hàng để dùng mã cá nhân.'
  }
  if (custId && hasCustomerUsedVoucher(v, custId)) {
    return 'Khách hàng đã dùng hết lượt mã này.'
  }
  if (isCaNhan && custId && !isVoucherAssignedToCustomer(v, custId)) {
    return 'Mã cá nhân không thuộc khách hàng này.'
  }
  const minRequired = Number(v.hoaDonToiThieu || 0)
  if (subTotal.value < minRequired) {
    const remaining = minRequired - subTotal.value
    return `Mua thêm ${formatVnd(remaining)} để dùng mã này (Đơn từ ${formatVnd(minRequired)}).`
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
          } catch {
            // Đồng bộ quyền cục bộ là tùy chọn; dữ liệu quyền từ API vẫn được áp dụng.
          }
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
      } catch {
        // Đồng bộ quyền cục bộ là tùy chọn; dữ liệu quyền từ API vẫn được áp dụng.
      }
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
      isVoucherModalOpen.value ||
      isQrModalOpen.value ||
      isAddCustomerModalOpen.value ||
      isReceiptModalOpen.value ||
      isRecentInvoicesModalOpen.value ||
      isProcessingCheckout.value
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
      withRequestTimeout(fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500&_t=${Date.now()}`, { cache: 'no-store' }).then(r => r.json()), 4000),
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
            item.discount = freshProd.discount || (freshProd.phanTramGiam ? `-${Math.round(freshProd.phanTramGiam)}%` : null)
            item.phanTramGiam = freshProd.phanTramGiam !== undefined ? freshProd.phanTramGiam : null
            item.giaSauGiam = freshProd.giaSauGiam !== undefined ? freshProd.giaSauGiam : null
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
          changes.push(`Sửa voucher: Phiếu giảm giá "${vCode}"${orderLabel} đã bị khóa, ngừng hoạt động hoặc bị xóa và được tự động gỡ`)
          const fallback = getBestVoucherForOrder(order)
          if (fallback?.voucher) {
            order.selectedVoucher = fallback.voucher
            changes.push(`Tự động đổi voucher: Chuyển sang phiếu giảm giá tốt nhất "${fallback.voucher.ma}" cho đơn hàng${orderLabel}`)
          }
          continue
        }

        const isCaNhan = freshV.phamVi === 'CA_NHAN' || freshV.scope === 'CA_NHAN'
        const custId = order.customer?.id ? Number(order.customer.id) : null
        const alreadyUsed = custId && hasCustomerUsedVoucher(freshV, custId)
        const notAssigned = isCaNhan && (!custId || !isVoucherAssignedToCustomer(freshV, custId))

        let removeReason = null
        if (alreadyUsed) {
          removeReason = `khách hàng đã dùng hết lượt`
        } else if (notAssigned) {
          removeReason = `phiếu giảm giá cá nhân không được cấp cho khách hàng này`
        } else if (freshV.ngayKetThuc && new Date(freshV.ngayKetThuc) < now) {
          removeReason = `đã hết hạn sử dụng (${formatDateVn(freshV.ngayKetThuc)})`
        } else if (freshV.ngayBatDau && new Date(freshV.ngayBatDau) > now) {
          removeReason = `chưa đến thời gian hiệu lực (${formatDateVn(freshV.ngayBatDau)})`
        } else if (freshV.soLuong && freshV.soLuongDaSuDung !== undefined && Number(freshV.soLuongDaSuDung) >= Number(freshV.soLuong)) {
          removeReason = `đã hết lượt sử dụng (${freshV.soLuongDaSuDung}/${freshV.soLuong})`
        } else if (newOrderSubTotal < Number(freshV.hoaDonToiThieu || 0)) {
          removeReason = `yêu cầu đơn tối thiểu ${formatVnd(freshV.hoaDonToiThieu)} (hiện tại ${formatVnd(newOrderSubTotal)})`
        }

        if (removeReason) {
          order.selectedVoucher = null
          changes.push(`Sửa voucher: Phiếu giảm giá "${vCode}"${orderLabel} ${removeReason} và được tự động gỡ`)
          const fallback = getBestVoucherForOrder(order)
          if (fallback?.voucher) {
            order.selectedVoucher = fallback.voucher
            changes.push(`Tự động đổi voucher: Chuyển sang phiếu giảm giá tốt nhất "${fallback.voucher.ma}" cho đơn hàng${orderLabel}`)
          }
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
  if (!kiemTraQuyenBanHang('áp dụng voucher')) return
  if (!currentOrder.value) return
  if (!isVoucherEligible(voucher)) {
    notifyWarning('Chưa đủ điều kiện', getVoucherIneligibilityReason(voucher))
    return
  }
  currentOrder.value.selectedVoucher = voucher
  currentOrder.value.userSelectedVoucherManually = true
  currentOrder.value.userRemovedVoucher = false
  voucherInputCode.value = ''
  isVoucherModalOpen.value = false
  notifySuccess('Đã áp dụng voucher', `Đã áp dụng mã "${voucher.ma || voucher.maPhieuGiamGia}" (${voucher.ten})!`)
}

const removeVoucher = () => {
  if (!kiemTraQuyenBanHang('hủy voucher')) return
  if (currentOrder.value) {
    currentOrder.value.selectedVoucher = null
    currentOrder.value.userSelectedVoucherManually = false
    currentOrder.value.userRemovedVoucher = true
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

watch([bestVoucherSuggestion, () => currentOrder.value?.selectedVoucher?.id], ([best]) => {
  if (!currentOrder.value) return
  const voucher = best?.voucher || null
  if (currentOrder.value.selectedVoucher?.id !== voucher?.id) {
    currentOrder.value.selectedVoucher = voucher
  }
})

watch(tienMatDuKienSauChi, (tienMoi, tienCu) => {
  if (isKetCaModalOpen.value && Number(ketCaForm.tienThucTe) === Number(tienCu)) {
    ketCaForm.tienThucTe = tienMoi
  }
})

watch([finalTotal, () => currentOrder.value?.paymentMethod, currentOrder], ([total, method], [oldTotal] = []) => {
  if (!currentOrder.value || method === 'CHUYEN_KHOAN') return
  const cash = Number(currentOrder.value.cashGiven || 0)
  if (!cash || cash === Number(oldTotal || 0)) currentOrder.value.cashGiven = total
})

const handleCheckout = async (options = {}) => {
  const skipConfirm = options?.skipConfirm === true
  if (!currentOrder.value || isProcessingCheckout.value) return
  if (currentOrder.value.cart.length === 0) {
    notifyWarning('Giỏ hàng trống', 'Vui lòng chọn ít nhất 1 sản phẩm vào giỏ hàng trước khi thanh toán!')
    return
  }

  if (!laAdmin() && !activeShift.value) {
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
      idCaLamViec: activeShift.value?.id || null,
      chiTiet: currentOrder.value.cart.map(item => ({
        idSanPhamChiTiet: item.idCtsp,
        soLuong: item.quantity,
        donGia: item.price,
        ghiChu: '',
        trangThai: true
      }))
    }

    // Kiểm tra tính hợp lệ của voucher trước khi checkout
    if (currentOrder.value.selectedVoucher && !isVoucherEligible(currentOrder.value.selectedVoucher)) {
      const fallbackBest = getBestVoucherForOrder(currentOrder.value)
      currentOrder.value.selectedVoucher = fallbackBest?.voucher || null
      currentOrder.value.userSelectedVoucherManually = false
      payload.idPhieuGiamGia = fallbackBest?.voucher ? fallbackBest.voucher.id : null
      if (fallbackBest?.voucher) {
        notifyWarning('Đổi voucher tốt nhất', `Mã giảm giá đã chọn không còn khả dụng, hệ thống tự động chuyển sang mã tốt nhất tiếp theo "${fallbackBest.voucher.ma}"!`)
      } else {
        notifyWarning('Bỏ mã giảm giá', 'Mã giảm giá đã chọn không còn khả dụng cho khách hàng này.')
      }
    } else {
      payload.idPhieuGiamGia = currentOrder.value.selectedVoucher ? currentOrder.value.selectedVoucher.id : null
    }

    const soldCartSnapshot = currentOrder.value.cart.map(item => ({ ...item }))
    let invoiceRes
    try {
      invoiceRes = await http.post('/hoa-don', payload)
    } catch (checkoutErr) {
      const errMsg = checkoutErr?.response?.data?.message ||
        checkoutErr?.response?.data?.error ||
        (typeof checkoutErr?.response?.data === 'string' ? checkoutErr?.response?.data : '') ||
        checkoutErr?.message || ''
      console.warn('Checkout error caught:', errMsg)
      if (payload.idPhieuGiamGia && (
        errMsg.includes('đã dùng phiếu') ||
        errMsg.includes('không được cấp') ||
        errMsg.includes('giới hạn') ||
        errMsg.includes('không hoạt động') ||
        errMsg.includes('hóa đơn tối thiểu') ||
        errMsg.includes('Phiếu giảm giá') ||
        errMsg.includes('phiếu giảm giá')
      )) {
        console.warn('Voucher rejected by backend, falling back to next best voucher:', errMsg)
        const rejectedVoucherId = Number(payload.idPhieuGiamGia)
        const custId = payload.idKhachHang ? Number(payload.idKhachHang) : null
        if (custId) {
          const vObj = allVouchers.value.find(v => Number(v.id) === rejectedVoucherId)
          if (vObj) {
            const daDung = Array.isArray(vObj.idKhachHangsDaDung) ? vObj.idKhachHangsDaDung : []
            if (!daDung.some(id => Number(id) === custId)) {
              vObj.idKhachHangsDaDung = [...daDung, custId]
            }
            if (Array.isArray(vObj.idKhachHangsChuaDung)) {
              vObj.idKhachHangsChuaDung = vObj.idKhachHangsChuaDung.filter(id => Number(id) !== custId)
            }
          }
        }
        const nextBest = getBestVoucherForOrder(currentOrder.value)
        currentOrder.value.selectedVoucher = nextBest?.voucher || null
        currentOrder.value.userSelectedVoucherManually = false
        payload.idPhieuGiamGia = nextBest?.voucher ? nextBest.voucher.id : null

        notifyWarning('Tự động đổi voucher', `Mã vừa chọn không hợp lệ với khách hàng. Hệ thống tự động chuyển sang ${nextBest?.voucher ? `mã tốt nhất tiếp theo "${nextBest.voucher.ma}"` : 'không dùng voucher'} và hoàn tất thanh toán!`)

        invoiceRes = await http.post('/hoa-don', payload)
      } else {
        throw checkoutErr
      }
    }
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

    if (currentOrder.value) {
      currentOrder.value.cart = []
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

    // Tự động hỏi in hóa đơn sau khi thanh toán
    let wantPrint = false
    if (options?.autoPrint) {
      wantPrint = true
    } else {
      wantPrint = await notifyConfirm(
        isDeliveryOrder ? 'Tạo đơn giao hàng thành công!' : 'Thanh toán thành công!',
        `Đã hoàn tất hóa đơn "${completedRecord.code}". Bạn có muốn in hóa đơn không?`,
        'In hóa đơn',
        'Không in',
        'success'
      )
    }

    if (wantPrint) {
      isReceiptModalOpen.value = true
      await nextTick()
      printReceipt()
    } else {
      notifySuccess('Thanh toán thành công!', `Đã hoàn tất hóa đơn ${completedRecord.code}`)
    }

    // Tự động chuyển sang trang hóa đơn chi tiết của hóa đơn đó
    if (completedRecord?.id) {
      setTimeout(() => {
        router.push(`/hoa-don/${completedRecord.id}`)
      }, wantPrint ? 600 : 250)
    }

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
    if (isVariantPickerModalOpen.value) {
      isVariantPickerModalOpen.value = false
      return
    }
    isProductModalOpen.value = false
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
      fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500&_t=${Date.now()}`, { cache: 'no-store' }).then(r => r.json()),
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

const handleDiscountChanged = async () => {
  try {
    const res = await fetch(`${API_BASE}/san-pham-chi-tiet?page=0&size=500&_t=${Date.now()}`, { cache: 'no-store' }).then(r => r.json())
    if (res && Array.isArray(res.content)) {
      setServerProductsList(res.content.filter(p => p.trangThai !== false))
    }
  } catch (e) {
    console.warn('Lỗi tải lại sản phẩm khi đợt giảm giá thay đổi:', e)
  }
  validateAndSyncHardCases({ isBeforeCheckout: false, silentToast: false })
}

const handleWindowFocusOrVisible = () => {
  if (document.visibilityState === 'visible') {
    handleDiscountChanged()
  }
}

const handleCrossTabStorageSync = (e) => {
  if (!e || !e.key) return
  if (
    e.key === 'polyshoes_pos_sync_signal' ||
    e.key === 'polyshoes_discount_updated' ||
    e.key === 'polyshoes_last_updated_staff' ||
    e.key === 'vaiTroHienTai' ||
    e.key === 'polyshoes_user'
  ) {
    staffUserRef.value = getUser()
    handleDiscountChanged()
  }
}

onMounted(async () => {
  await fetchActiveShift()
  loadRecentInvoicesFromStorage()
  loadInitialData()
  loadProvinces()
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('click', handleWindowClick)
  window.addEventListener('focus', handleWindowFocusOrVisible)
  document.addEventListener('visibilitychange', handleWindowFocusOrVisible)
  window.addEventListener('storage', handleCrossTabStorageSync)
  window.addEventListener('polyshoes:shift-changed', fetchActiveShift)
  window.addEventListener('polyshoes:open-ket-ca', openKetCaModal)
  window.addEventListener('polyshoes:open-mo-ca', openMoCaModal)
  window.addEventListener('polyshoes:discount-changed', handleDiscountChanged)
  realtimePollTimer = setInterval(() => {
    if (document.visibilityState === 'visible') {
      handleDiscountChanged()
    }
  }, 3000)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('click', handleWindowClick)
  window.removeEventListener('focus', handleWindowFocusOrVisible)
  document.removeEventListener('visibilitychange', handleWindowFocusOrVisible)
  window.removeEventListener('storage', handleCrossTabStorageSync)
  window.removeEventListener('polyshoes:shift-changed', fetchActiveShift)
  window.removeEventListener('polyshoes:open-ket-ca', openKetCaModal)
  window.removeEventListener('polyshoes:open-mo-ca', openMoCaModal)
  window.removeEventListener('polyshoes:discount-changed', handleDiscountChanged)
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
        <template v-if="!laAdmin()">
          <div class="pos-status-indicator" v-if="activeShift">
            <span class="pulse-dot"></span>
            <span>Ca đang mở</span>
          </div>
          <div class="pos-status-indicator view-mode-indicator" v-else>
            <span class="pulse-dot view-mode"></span>
            <span>Chế độ xem (Chưa mở ca)</span>
          </div>
        </template>
      </div>

      <div class="pos-header-actions">
        <!-- Nút Mở / Đóng ca làm việc trên quầy POS (Chỉ hiển thị cho Nhân viên, Admin không cần mở/đóng ca) -->
        <template v-if="!laAdmin()">
          <div v-if="activeShift" class="pos-shift-group">
            <span class="pos-shift-badge" :title="`Ca làm việc ${activeShift.tenCaLich || activeShift.maCa} đang hoạt động`">
              <span class="pulse-dot"></span>
              Ca: <strong>{{ activeShift.tenCaLich || activeShift.maCa }}</strong>
              <small v-if="activeShift.tenCaLich" class="pos-shift-code-tag">({{ activeShift.maCa }})</small>
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
        </template>

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

    <!-- Cảnh báo khi quầy chưa mở ca làm việc (Chỉ hiển thị cho Nhân viên ở Chế độ xem) -->
    <div v-if="isCheDoXem && !isLoadingShift" class="pos-no-shift-alert-banner is-view-mode">
      <div class="alert-banner-left">
        <span class="badge-view-mode-tag">CHẾ ĐỘ XEM</span>
        <span class="view-mode-text">Bạn chưa vào ca làm việc nên chỉ có quyền tra cứu thông tin. Các thao tác bán hàng, tạo đơn hàng và chỉnh sửa dữ liệu đang bị khóa.</span>
      </div>
      <button type="button" class="btn-banner-open-shift btn-view-mode-red" @click="openMoCaModal">
        <span>Vào ca làm việc ngay</span>
      </button>
    </div>

    <div class="pos-main-grid">
      <div class="pos-col-left">
        <div class="pos-workspace-panel">
          <div class="pos-orders-tabs-bar">
            <div class="pos-tabs-scroll-area">
              <button class="btn-create-order-tab" @click="createNewOrder" title="Tạo hóa đơn chờ mới">
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
              <span class="counter-num">{{ pendingOrders.length }}</span>
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
                      <th width="120" class="text-center">MÀU / SIZE</th>
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
                          <span class="variant-color-txt">{{ item.color }}</span>
                          <span class="variant-sep">•</span>
                          <span class="variant-size-txt">{{ item.size }}</span>
                        </div>
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
                        <div class="price-stack" style="white-space: nowrap !important;">
                          <span class="price-active" style="white-space: nowrap !important;">{{ formatVnd(item.price) }}</span>
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

          <!-- PHIẾU GIẢM GIÁ (VOUCHER) ĐƠN GIẢN THEO YÊU CẦU -->
          <div class="pos-voucher-simple-wrap" v-if="currentOrder">
            <div class="pos-voucher-simple-title">Phiếu giảm giá (Voucher)</div>

            <div v-if="currentOrder.selectedVoucher" class="pos-voucher-simple-card">
              <div class="pv-card-left">
                <span class="pv-card-icon">🏷️</span>
                <div class="pv-card-info">
                  <div class="pv-card-headline">
                    <span class="pv-badge" :class="currentOrder.selectedVoucher.phamVi === 'CA_NHAN' ? 'pv-badge-personal' : 'pv-badge-public'">
                      {{ currentOrder.selectedVoucher.phamVi === 'CA_NHAN' ? 'CÁ NHÂN' : 'CÔNG KHAI' }}
                    </span>
                    <strong class="pv-code">{{ currentOrder.selectedVoucher.ma || currentOrder.selectedVoucher.maPhieuGiamGia }}</strong>
                    <span class="pv-name" v-if="currentOrder.selectedVoucher.ten">- {{ currentOrder.selectedVoucher.ten }}</span>
                  </div>
                  <div class="pv-card-subline">
                    <span>Đơn từ {{ formatVnd(currentOrder.selectedVoucher.hoaDonToiThieu || 0) }}</span>
                    <template v-if="currentOrder.selectedVoucher.giamToiDa">
                      • Tối đa {{ formatVnd(currentOrder.selectedVoucher.giamToiDa) }}
                    </template>
                  </div>
                </div>
              </div>

              <div class="pv-card-right">
                <div class="pv-discount-amt">-{{ formatVnd(discountAmount) }}</div>
                <div class="pv-discount-pct" v-if="currentOrder.selectedVoucher.loaiGiamGia === 'PHAN_TRAM' || currentOrder.selectedVoucher.loaiGiamGia === 'PERCENT' || currentOrder.selectedVoucher.type === 'percent'">
                  -{{ Math.round(currentOrder.selectedVoucher.giaTriGiam || 0) }}%
                </div>
              </div>
            </div>

            <!-- Gợi ý mua thêm (như bán hàng online) -->
            <div v-if="!currentOrder.selectedVoucher && nextVoucherSuggestion" class="next-voucher-upsell-box mt-2">
              <div class="upsell-icon-col">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
              </div>
              <div class="upsell-body-col">
                <div class="upsell-title">
                  Mua thêm <span class="upsell-highlight-amt">{{ formatVnd(nextVoucherSuggestion.neededMore) }}</span> để dùng mã
                  <span class="upsell-code-badge">{{ nextVoucherSuggestion.voucher.ma }}</span>
                </div>
                <div class="upsell-desc">
                  Ưu đãi giảm thêm lên tới <strong class="upsell-benefit">{{ formatVnd(nextVoucherSuggestion.potentialDiscount) }}</strong>
                </div>
              </div>
            </div>
                </div>
              </div>
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
                  :value="currentOrder.cashGiven === 0 && !currentOrder.customCashGiven ? '' : currentOrder.cashGiven"
                  @input="handleCashGivenInput($event)"
                  placeholder=""
                  min="0"
                  step="1000"
                  @focus="$event.target.select()"
                />
                <span class="cash-currency-badge">₫</span>
              </div>
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
                <span class="modal-badge-count">{{ modalTotalElements }} sản phẩm</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isProductModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="modal-prod-filter-bar">
              <div class="modal-filter-top-row">
                <div class="modal-search-field">
                  <input v-model="modalFilters.search" type="text" placeholder="Tìm theo tên sản phẩm, mã sản phẩm, mã CTSP..."
                    class="modal-search-inp" />
                  <button v-if="modalFilters.search" class="modal-clear-search-btn"
                    @click="modalFilters.search = ''">×</button>
                </div>
                <button class="btn-modal-reset-filter" @click="resetModalFilters" title="Đặt lại toàn bộ bộ lọc">
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

            <div class="pos-prod-table-wrap">
              <table class="pos-prod-table">
                <thead>
                  <tr>
                    <th style="width: 42px;" class="text-center">STT</th>
                    <th style="width: 56px;" class="text-center">ẢNH</th>
                    <th style="width: 100px;" class="text-center">MÃ SP</th>
                    <th>TÊN SẢN PHẨM</th>
                    <th style="width: 120px;" class="text-center">MÀU SẮC</th>
                    <th style="width: 105px;" class="text-center">KÍCH CỠ</th>
                    <th style="width: 90px;" class="text-center">TỔNG TỒN</th>
                    <th style="width: 140px;" class="text-center">KHOẢNG GIÁ</th>
                    <th style="width: 125px;" class="text-center">THAO TÁC</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(prod, idx) in paginatedModalProducts" :key="prod.key"
                    @click="openVariantPickerForProduct(prod)"
                    style="cursor: pointer;"
                    title="Bấm để xem và chọn biến thể">
                    <td class="text-center prod-stt">{{ modalCurrentPage * modalPageSize + idx + 1 }}</td>
                    <td class="text-center">
                      <div class="modal-thumb-box">
                        <img :src="prod.img" class="modal-thumb-img" alt="shoe" />
                        <span class="prod-img-discount-badge" v-if="prod.variants.length > 1">
                          {{ prod.variants.length }} mẫu
                        </span>
                      </div>
                    </td>
                    <td class="text-center">
                      <span class="code-pill-modal">{{ prod.maSp }}</span>
                    </td>
                    <td class="modal-prod-name">
                      <div class="prod-text-title fw-bold">{{ prod.tenSp }}</div>
                      <div class="small text-muted" v-if="prod.tenThuongHieu">{{ prod.tenThuongHieu }}</div>
                    </td>
                    <td class="text-center">
                      <div class="d-flex flex-wrap gap-1 justify-content-center align-items-center">
                        <span v-for="c in prod.colorsList.slice(0, 3)" :key="c" class="badge bg-light text-dark border" style="font-size: 11px;">
                          {{ c }}
                        </span>
                        <span v-if="prod.colorsList.length > 3" class="badge bg-secondary-subtle text-secondary" style="font-size: 10px;">
                          +{{ prod.colorsList.length - 3 }}
                        </span>
                      </div>
                    </td>
                    <td class="text-center">
                      <div class="d-flex flex-wrap gap-1 justify-content-center align-items-center">
                        <span v-for="s in prod.sizesList.slice(0, 4)" :key="s" class="badge-size-tag" style="font-size: 11px; padding: 2px 5px;">
                          {{ s }}
                        </span>
                        <span v-if="prod.sizesList.length > 4" class="text-muted small">
                          ...
                        </span>
                      </div>
                    </td>
                    <td class="text-center">
                      <span class="stock-badge in-stock">
                        {{ prod.totalStock }} đôi
                      </span>
                    </td>
                    <td class="text-center">
                      <div class="prod-modal-price-box">
                        <span class="price-now" v-if="prod.minPrice === prod.maxPrice">
                          {{ formatVnd(prod.minPrice) }}
                        </span>
                        <span class="price-now" v-else style="font-size: 12.5px;">
                          {{ formatVnd(prod.minPrice) }} - {{ formatVnd(prod.maxPrice) }}
                        </span>
                      </div>
                    </td>
                    <td class="text-center" @click.stop>
                      <button class="btn btn-sm btn-danger fw-bold rounded-pill px-3 py-1"
                        @click="openVariantPickerForProduct(prod)"
                        style="font-size: 12px; display: inline-flex; align-items: center; gap: 4px;">
                        <span>Chọn biến thể</span>
                        <span class="badge bg-white text-danger rounded-circle px-1" style="font-size: 10px;">{{ prod.variants.length }}</span>
                      </button>
                    </td>
                  </tr>
                  <tr v-if="paginatedModalProducts.length === 0">
                    <td colspan="9" class="empty-state-modal">
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

          <!-- Phân trang -->
          <div v-if="modalTotalPages > 1" class="table-card-foot" style="border-top: 1px solid #e2e8f0; background: #ffffff;">
            <PaginationBar
              :page="modalCurrentPage"
              :total-pages="modalTotalPages"
              :total-elements="modalTotalElements"
              :page-size="modalPageSize"
              :sizes="pageSizeOptions"
              :from="paginatedModalProducts.length ? modalCurrentPage * modalPageSize + 1 : 0"
              :to="modalCurrentPage * modalPageSize + paginatedModalProducts.length"
              label="sản phẩm"
              @change="changeModalPage"
              @size-change="(sz) => { modalPageSize = sz; modalCurrentPage = 0 }"
            />
          </div>

          <div class="pos-prod-footer">
            <div class="pos-modal-page-size">
              <span class="page-size-label">Hiển thị:</span>
              <select v-model.number="modalPageSize" class="page-size-select" @change="modalCurrentPage = 0">
                <option v-for="sz in pageSizeOptions" :key="sz" :value="sz">{{ sz }} / trang</option>
              </select>
              <span class="page-size-total">Tổng <strong>{{ modalTotalElements }}</strong> sản phẩm</span>
            </div>

            <div class="pos-prod-action-group">
              <button class="btn-pos-secondary px-4 py-2" @click="closeProductPicker">Đóng</button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- SUB-MODAL: CHỌN BIẾN THỂ CỦA SẢN PHẨM -->
    <transition name="fade">
      <div v-if="isVariantPickerModalOpen" class="pos-modal-overlay" style="z-index: 1065;" @click.self="closeVariantPickerModal">
        <div class="pos-modal-dialog modal-swap-variant" style="max-width: 860px;">
          <div class="pos-modal-head">
            <div class="modal-head-title">
              <div class="modal-title-icon" style="background: #fee2e2; color: #dc2626;">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
              </div>
              <div class="modal-title-text-group">
                <h3>Chọn biến thể sản phẩm</h3>
                <span class="modal-badge-count" v-if="activePickerProduct">{{ activePickerProduct.variants.length }} biến thể có sẵn</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="closeVariantPickerModal" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="current-swap-banner" v-if="activePickerProduct">
              <div class="csb-left">
                <img v-if="activePickerProduct.img" :src="activePickerProduct.img" alt="Ảnh SP" class="csb-img" />
                <div v-else class="csb-no-img">👟</div>
                <div class="csb-details">
                  <span class="csb-title" style="font-size: 16px;">{{ activePickerProduct.tenSp }}</span>
                  <div class="csb-pills">
                    <span class="csb-pill pill-code">Mã SP: {{ activePickerProduct.maSp }}</span>
                    <span class="csb-pill pill-current">Tổng tồn kho: <strong>{{ activePickerProduct.totalStock }} đôi</strong></span>
                    <span class="csb-pill pill-qty" v-if="activePickerProduct.tenThuongHieu">{{ activePickerProduct.tenThuongHieu }}</span>
                  </div>
                </div>
              </div>
              <div class="csb-price-box">
                <span class="csb-price-lbl">Khoảng giá:</span>
                <span class="csb-price-val" v-if="activePickerProduct.minPrice === activePickerProduct.maxPrice">
                  {{ formatVnd(activePickerProduct.minPrice) }}
                </span>
                <span class="csb-price-val" v-else style="font-size: 14px;">
                  {{ formatVnd(activePickerProduct.minPrice) }} - {{ formatVnd(activePickerProduct.maxPrice) }}
                </span>
              </div>
            </div>

            <div class="swap-color-filter-bar" v-if="availablePickerColors.length > 1">
              <span class="filter-bar-title">Lọc theo màu sắc:</span>
              <div class="filter-chips-list">
                <button type="button" class="swap-chip-btn" :class="{ active: pickerColorFilter === 'ALL' }"
                  @click="pickerColorFilter = 'ALL'">
                  Tất cả màu ({{ activePickerProduct?.variants.length || 0 }})
                </button>
                <button v-for="color in availablePickerColors" :key="color" type="button" class="swap-chip-btn"
                  :class="{ active: pickerColorFilter === color }" @click="pickerColorFilter = color">
                  {{ color }}
                </button>
              </div>
            </div>

            <div class="swap-variants-matrix" v-if="filteredPickerVariants.length > 0">
              <div v-for="variant in filteredPickerVariants" :key="variant.id" class="variant-swap-card" :class="{
                'is-card-selected': selectedPickerVariant?.id === variant.id,
                'is-stock-depleted': getRemainingStock(variant.id) <= 0
              }"
                @click="getRemainingStock(variant.id) > 0 && (selectedPickerVariant = variant)">
                <div class="vsc-top-bar">
                  <span v-if="getRemainingStock(variant.id) <= 0" class="badge-vsc out-stock">Hết hàng</span>
                  <span v-else class="badge-vsc in-stock">Còn {{ getRemainingStock(variant.id) }} đôi</span>

                  <span class="vsc-color-pill">
                    <span class="vsc-dot" :style="{ backgroundColor: variant.colorHex || '#64748b' }"></span>
                    {{ variant.color || variant.tenMau }}
                  </span>
                </div>

                <div class="vsc-body">
                  <div class="vsc-size-display">
                    <span class="size-large">{{ variant.size || variant.tenKichCo }}</span>
                    <span class="size-sub">Size / Mã: {{ variant.maCtsp || variant.maChiTietSanPham || variant.maSp }}</span>
                  </div>

                  <div class="vsc-price-col">
                    <div class="vsc-price-val">{{ formatVnd(getEffectivePrice(variant)) }}</div>
                    <div class="vsc-price-diff" v-if="variant.discount && variant.discount !== '-'">
                      <span class="diff-tag down">{{ variant.discount }}</span>
                    </div>
                  </div>
                </div>

                <div class="vsc-foot">
                  <button type="button" v-if="getRemainingStock(variant.id) <= 0" class="btn-vsc-btn disabled-stock" disabled>
                    Hết hàng
                  </button>
                  <button type="button" v-else class="btn btn-sm btn-danger w-100 py-2 fw-bold rounded-3"
                    @click.stop="addVariantFromPicker(variant)">
                    + Thêm vào giỏ
                  </button>
                </div>
              </div>
            </div>

            <div v-else class="swap-empty-box">
              <p>Không có biến thể nào còn hàng phù hợp với màu này.</p>
            </div>
          </div>

          <div class="pos-modal-footer cust-modal-footer">
            <button type="button" class="btn-cust-cancel" @click="closeVariantPickerModal">
              Đóng
            </button>
          </div>
        </div>
      </div>
    </transition>


      <!-- MODAL PHIẾU GIẢM GIÁ (ĐỒNG BỘ GIAO DIỆN ONLINE) -->
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
                <h3>Phiếu giảm giá</h3>
                <span class="modal-badge-count">{{ availableEligibleVouchersCount }}/{{ allVouchers.length }} dùng được</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="isVoucherModalOpen = false" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div v-if="bestVoucherSuggestion" class="modal-best-voucher-banner">
              <div class="best-banner-info-left">
                <div class="best-badge-pill">
                  <span class="pill-fire">🔥</span>
                  <span>Đề xuất tốt nhất dùng được</span>
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
              <button v-if="currentOrder?.selectedVoucher?.id !== bestVoucherSuggestion.voucher.id" type="button" class="btn-apply-best-modal" @click="applyVoucher(bestVoucherSuggestion.voucher)">
                Chọn mã tối ưu này
              </button>
              <span v-else class="badge-voucher-applied">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                Đang áp dụng
              </span>
            </div>

            <div class="voucher-modal-search-bar">
              <input v-model="voucherSearchQuery" type="text" placeholder="Tìm theo mã hoặc tên phiếu giảm giá..."
                class="voucher-search-inp" @keyup.enter="handleModalSearchApply" />
            </div>

            <div class="voucher-cards-list">
              <div v-for="v in sortedFilteredVouchers" :key="v.id" class="voucher-card-item" :class="{
                eligible: isVoucherEligible(v),
                selected: currentOrder?.selectedVoucher?.id === v.id,
                ineligible: !isVoucherEligible(v),
                isBestItem: bestVoucherSuggestion?.voucher?.id === v.id
              }" @click="isVoucherEligible(v) && currentOrder?.selectedVoucher?.id !== v.id ? applyVoucher(v) : null">
                <div class="voucher-card-left">
                  <div class="voucher-discount-highlight">
                    {{ formatVoucherValue(v) }}
                  </div>
                  <div class="voucher-type-sub">
                    {{ v.loaiGiamGia === 'PERCENT' || v.loaiGiamGia === '%' ? 'GIẢM %' : 'GIẢM TIỀN' }}
                  </div>
                  <span v-if="bestVoucherSuggestion?.voucher?.id === v.id && isVoucherEligible(v)" class="best-ticket-star">
                    ⭐ Tốt nhất
                  </span>
                </div>

                <div class="voucher-card-body">
                  <div class="voucher-card-header-row">
                    <span class="voucher-card-code">{{ v.ma }}</span>
                    <span v-if="isVoucherEligible(v)" class="badge bg-success-subtle text-success border border-success-subtle" style="font-size: 11px;">
                      Dùng được
                    </span>
                    <span v-else class="badge bg-secondary-subtle text-secondary border border-secondary-subtle" style="font-size: 11px;">
                      Không dùng được
                    </span>
                    <span v-if="bestVoucherSuggestion?.voucher?.id === v.id && isVoucherEligible(v)" class="badge bg-success text-white" style="font-size: 11px;">
                      Tốt nhất
                    </span>
                    <span v-if="v.soLuong" class="voucher-card-remain">
                      Còn {{ Math.max(0, (v.soLuong || 0) - (v.soLuongDaSuDung || 0)) }} lượt
                    </span>
                  </div>
                  <div class="voucher-card-name">{{ v.ten }}</div>
                  <div class="voucher-card-conditions">
                    <span>Đơn từ: <strong>{{ formatVnd(v.hoaDonToiThieu || 0) }}</strong></span>
                    <span v-if="v.giamToiDa"> • Tối đa: <strong>{{ formatVnd(v.giamToiDa) }}</strong></span>
                  </div>

                  <div v-if="isVoucherEligible(v) && subTotal > 0" class="voucher-calc-saving text-success fw-semibold">
                    Giảm được {{ formatVnd(calculateVoucherDiscount(v, subTotal)) }}
                  </div>

                  <div class="voucher-card-date" v-if="v.ngayKetThuc">
                    HSD: {{ formatDateVn(v.ngayKetThuc) }}
                  </div>
                  <div v-if="!isVoucherEligible(v)" class="voucher-ineligible-msg text-danger small">
                    {{ getVoucherIneligibilityReason(v) }}
                  </div>
                </div>

                <div class="voucher-card-action">
                  <span v-if="currentOrder?.selectedVoucher?.id === v.id" class="badge-voucher-applied" title="Mã đang được áp dụng cho đơn hàng">
                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                    Đang dùng
                  </span>
                  <button v-else-if="isVoucherEligible(v)" type="button" class="btn-select-voucher"
                    @click.stop="applyVoucher(v)">
                    Chọn mã
                  </button>
                  <button v-else type="button" class="btn-select-voucher disabled" disabled>
                    Không khả dụng
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
                <span class="modal-badge-count">Chế độ quét liên tục nhiều mã QR & Barcode</span>
              </div>
            </div>
            <button class="pos-modal-close" @click="closeQrScanner" title="Đóng (Esc)">×</button>
          </div>

          <div class="pos-modal-body">
            <div class="pos-camera-scan-wrap">
              <div class="pos-camera-viewport">
                <div id="pos-camera-scanner-view" class="pos-camera-stream-el"></div>
                <div v-if="isCameraRunning && !scanSuccessAnim" class="pos-scanner-laser-line"></div>
                <div class="pos-scanner-frame-overlay" :class="{ 'flash-success': scanSuccessAnim }">
                  <span class="corner top-left"></span>
                  <span class="corner top-right"></span>
                  <span class="corner bottom-left"></span>
                  <span class="corner bottom-right"></span>
                </div>

                <!-- Toast nổi góc trên camera khi thêm thành công sản phẩm -->
                <transition name="fade">
                  <div v-if="scanNotificationToast" class="pos-scan-notification-pill">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    <span>{{ scanNotificationToast }}</span>
                  </div>
                </transition>
              </div>

              <!-- Gợi ý hướng dẫn -->
              <p class="camera-guide-hint">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="16" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
                <span>Hướng camera vào lần lượt các <b>mã QR / Barcode</b> để quét liên tục nhiều sản phẩm. Bấm <b>Hoàn tất</b> khi quét xong.</span>
              </p>

              <!-- Danh sách các sản phẩm đã quét trong phiên -->
              <div v-if="scannedSessionList.length > 0" class="pos-scanned-session-box">
                <div class="scanned-session-header">
                  <span class="session-header-title">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2">
                      <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                      <line x1="3" y1="6" x2="21" y2="6"></line>
                      <path d="M16 10a4 4 0 0 1-8 0"></path>
                    </svg>
                    Đã quét được: <strong>{{ totalScannedSessionItems }} sản phẩm</strong>
                  </span>
                  <button type="button" class="btn-clear-scanned-session" @click="scannedSessionList = []">Làm mới danh sách</button>
                </div>
                <div class="scanned-session-items-list">
                  <div v-for="item in scannedSessionList" :key="item.id" class="scanned-session-item">
                    <div class="scanned-item-info">
                      <span class="scanned-item-name">{{ item.tenSp }}</span>
                      <span class="scanned-item-meta">Size {{ item.size }} • {{ item.color }} • {{ formatVnd(item.price) }}</span>
                    </div>
                    <span class="scanned-item-qty">x{{ item.count }}</span>
                  </div>
                </div>
              </div>

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
                  placeholder="Nhập mã CTSP hoặc nhiều mã phân cách bằng dấu phẩy (VD: SP01, SP02)..."
                  @keyup.enter="handleManualCodeSubmit" />
                <button class="btn-pos-manual-add" @click="handleManualCodeSubmit">
                  Thêm
                </button>
              </div>

              <div class="pos-camera-footer-actions">
                <button type="button" class="btn-complete-scan" @click="closeQrScanner">
                  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  <span>Hoàn tất quét{{ totalScannedSessionItems > 0 ? ` (${totalScannedSessionItems} sản phẩm)` : '' }}</span>
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
                <span class="shift-info-label">Ca được chia sẵn:</span>
                <strong class="shift-info-val text-brand">{{ selectedCa?.ten || 'Chưa chọn ca' }}</strong>
              </div>
              <div class="shift-info-item">
                <span class="shift-info-label">Khung giờ ca:</span>
                <span class="shift-info-val font-weight-bold">{{ selectedCa ? (selectedCa.batDau?.substring(0, 5) + ' — ' + selectedCa.ketThuc?.substring(0, 5)) : '--:--' }}</span>
              </div>
              <div class="shift-info-item">
                <span class="shift-info-label">Điểm bán hàng:</span>
                <span class="shift-info-val text-brand">Quầy POS #01</span>
              </div>
            </div>

            <!-- Ca trong lịch của nhân viên được chọn tự động; không yêu cầu chọn bằng combobox. -->
            <div class="pos-shift-field-group">
              <label class="pos-shift-label">
                <span>Ca đang trực theo lịch hôm nay</span>
              </label>

              <div v-if="isLoadingCaChiaSan" class="shift-cards-loading">
                <span class="loading-spinner"></span> Đang tải danh sách ca làm...
              </div>

              <div v-else-if="selectedCa" class="shift-cards-empty">
                {{ selectedCa.ten }} · {{ selectedCa.batDau?.substring(0, 5) }} — {{ selectedCa.ketThuc?.substring(0, 5) }}
              </div>
              <div v-else class="shift-cards-empty">Hôm nay bạn chưa được xếp lịch ca.</div>
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
              <span>{{ moCaSubmitting ? 'Đang mở ca...' : ('Xác nhận mở ' + (selectedCa?.ten || 'ca làm việc')) }}</span>
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
                        <!-- CẢNH BÁO NẾU CÒN ĐƠN CHƯA HOÀN TẤT -->
            <div class="pos-pending-orders-alert" v-if="ketCaSummary.hoaDonDangXuLy && ketCaSummary.hoaDonDangXuLy.length > 0">
              <div class="pending-alert-icon">⚠️</div>
              <div class="pending-alert-content">
                <strong>Ca làm việc còn {{ ketCaSummary.hoaDonDangXuLy.length }} đơn hàng chưa hoàn tất:</strong>
                <span class="pending-order-codes">{{ ketCaSummary.hoaDonDangXuLy.join(', ') }}</span>
                <p>Vui lòng xử lý thanh toán hoặc hủy đơn chờ trên quầy trước khi đóng ca để đối soát két tiền chính xác!</p>
              </div>
            </div>

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
                <span class="sum-card-lbl">TIỀN MẶT DỰ KIẾN TRONG KÉT</span>
                <strong class="sum-card-val text-brand">{{ formatVnd(tienMatDuKienSauChi) }}</strong>
                <span class="sum-card-sub">Sau khi trừ tiền chi trong ca</span>
              </div>
            </div>

            <!-- KHỐI 2: KIỂM ĐẾM TIỀN MẶT THỰC TẾ & CHÊNH LỆCH -->
            <div class="pos-shift-count-box">
              <div class="pos-shift-field-group">
                <label class="pos-shift-label">Nhân viên nhận bàn giao</label>
                <div class="shift-cards-empty">{{ ketCaSummary.tenNguoiNhan || 'Cuối ngày' }}</div>
              </div>
              <div class="pos-shift-field-group">
                <label class="pos-shift-label" for="shift-cash-expense">Tiền chi trong ca</label>
                <div class="pos-shift-input-wrap">
                  <input id="shift-cash-expense" v-model.number="ketCaForm.tienChi" type="number" min="0" step="1000" class="pos-shift-input" />
                  <span class="pos-shift-currency-unit">VNĐ</span>
                </div>
              </div>
              <div class="count-box-head">
                <label class="pos-shift-label mb-0">
                  <span>Kiểm đếm tiền mặt thực tế trong két</span>
                  <span class="pos-shift-required">*</span>
                  <strong class="pos-shift-preview-val">{{ formatVnd(ketCaForm.tienThucTe || 0) }}</strong>
                </label>
                <button type="button" class="btn-match-du-kien" @click="ketCaForm.tienThucTe = tienMatDuKienSauChi">
                  <span>Khớp dự kiến ({{ formatVnd(tienMatDuKienSauChi) }})</span>
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
            <button type="button" class="btn-shift-confirm-close" @click="handleKetCaSubmit" :disabled="ketCaSubmitting || (chenhLechKetCa !== 0 && !ketCaForm.lyDoLech?.trim()) || (ketCaSummary.hoaDonDangXuLy && ketCaSummary.hoaDonDangXuLy.length > 0)"
              :title="(ketCaSummary.hoaDonDangXuLy && ketCaSummary.hoaDonDangXuLy.length > 0) ? 'Còn hóa đơn chưa hoàn tất. Vui lòng thanh toán hoặc hủy trước khi đóng ca!' : 'Xác nhận đóng ca'">
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
