<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { getInvoice, getInvoiceHistory } from '@/api/invoice'
import { getCustomer } from '@/api/customer'
import { formatDateTime, formatVnd, payStatusOf } from '@/utils/format'
import {
  TRANG_THAI_ONLINE,
  TRANG_THAI_POS,
  isTerminal as isTerminalOf,
  laPos,
  pillClass,
  trangThaiTiepTheoOf,
} from '@/constants/invoiceStatus'
import { updateInvoiceStatus } from '@/api/invoice'
import { notifyError, notifyConfirm, notifySuccess } from '@/utils/notify'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const data = ref(null)
const khInfo = ref(null)
const qrDataUrl = ref('')
const qrReady = ref(false)
const updatingStatus = ref(false)
const showStatusMenu = ref(false)
const statusMenuRef = ref(null)
const showHistory = ref(false)
const historyLoading = ref(false)
const historyError = ref('')
const historyList = ref([])

const TRANG_THAI_CODE = {
  'Chờ xác nhận': 'CHO_XAC_NHAN',
  'Đã xác nhận': 'DA_XAC_NHAN',
  'Chờ giao hàng': 'CHO_GIAO_HANG',
  'Đang giao hàng': 'DANG_GIAO_HANG',
  'Đã giao hàng': 'DA_GIAO_HANG',
  'Đã hoàn thành': 'DA_HOAN_THANH',
  'Đã hủy': 'DA_HUY',
  'Đã hoàn tiền': 'DA_HOAN_TIEN',
}

const qrPayload = computed(() => {
  const ma = data.value?.maHoaDon
  return ma ? `POLYSHOES|${ma}` : ''
})

const genQr = async () => {
  qrReady.value = false
  if (!qrPayload.value) {
    qrDataUrl.value = ''
    return false
  }
  const payload = qrPayload.value
  try {
    qrDataUrl.value = await QRCode.toDataURL(payload, {
      width: 110,
      margin: 1,
      errorCorrectionLevel: 'M',
      color: { dark: '#202124', light: '#ffffff' },
    })
    if (payload !== qrPayload.value) return false
    await nextTick()
    qrReady.value = true
    return true
  } catch {
    qrDataUrl.value = ''
    qrReady.value = false
    notifyError('Không tạo được mã QR', 'Mã QR sẽ không xuất hiện trên bản in.')
    return false
  }
}

const stepsList = computed(() => {
  if (!data.value) return TRANG_THAI_ONLINE
  return laPos(data.value) ? TRANG_THAI_POS : TRANG_THAI_ONLINE
})

const isTerminal = computed(() => !!data.value && isTerminalOf(data.value))

const tiepTheoList = computed(() => trangThaiTiepTheoOf(data.value))

const laTrangThaiHienTai = (ten) => data.value?.trangThai === ten

const laTrangThaiTiepTheo = (ten) => tiepTheoList.value.includes(ten)

const laDaQua = (ten) => {
  const list = stepsList.value
  const cur = list.indexOf(data.value?.trangThai)
  const i = list.indexOf(ten)
  return cur > -1 && i > -1 && i < cur
}

const chuyenTrangThai = async (ten) => {
  if (!data.value || updatingStatus.value) return
  if (!laTrangThaiTiepTheo(ten)) return
  const code = TRANG_THAI_CODE[ten]
  if (!code) return
  showStatusMenu.value = false
  const hienTai = data.value.trangThai || '—'
  const ma = data.value.maHoaDon || `#${data.value.id}`
  const ok = await notifyConfirm(
    'Cập nhật trạng thái',
    `Hóa đơn ${ma}\nTrạng thái hiện tại: ${hienTai}\nTrạng thái đích: ${ten}`,
    'Xác nhận chuyển',
    'Hủy',
  )
  if (!ok) return
  updatingStatus.value = true
  try {
    await updateInvoiceStatus(data.value.id, code)
    notifySuccess('Đã cập nhật trạng thái', `${hienTai} → ${ten}`)
    await load()
  } catch (e) {
    notifyError('Cập nhật thất bại', e.message || 'Không thể chuyển trạng thái')
  } finally {
    updatingStatus.value = false
  }
}

const toggleStatusMenu = () => {
  if (updatingStatus.value) return
  showStatusMenu.value = !showStatusMenu.value
}

const chonMenu = (ten) => {
  showStatusMenu.value = false
  chuyenTrangThai(ten)
}

const onDocClick = (e) => {
  if (!showStatusMenu.value) return
  const el = statusMenuRef.value
  if (el && !el.contains(e.target)) showStatusMenu.value = false
}

const onEscape = (e) => {
  if (e.key !== 'Escape') return
  if (showStatusMenu.value) {
    showStatusMenu.value = false
    return
  }
  if (showHistory.value) showHistory.value = false
}

const moLichSu = async () => {
  if (!data.value) return
  showHistory.value = true
  historyLoading.value = true
  historyError.value = ''
  historyList.value = []
  try {
    historyList.value = await getInvoiceHistory(data.value.id)
  } catch (e) {
    historyError.value = e.message || 'Không tải được lịch sử thao tác'
  } finally {
    historyLoading.value = false
  }
}

const dongLichSu = () => {
  showHistory.value = false
}

const tienHang = computed(() =>
  (data.value?.chiTiet || []).reduce(
    (sum, l) => sum + (Number(l.thanhTien) || 0),
    0,
  ),
)

const printZoom = computed(() => {
  const lines = data.value?.chiTiet || []
  const estimatedHeight = 500 + lines.reduce((height, line) => {
    const descriptionLength = `${line.tenSanPham || ''} ${line.maSanPhamChiTiet || ''}`.length
    return height + 38 + (descriptionLength > 48 ? 15 : 0)
  }, 0)
  return Math.min(1, Math.max(0.5, 1050 / estimatedHeight))
})

const coGiamGia = computed(() => {
  const d = data.value
  if (!d) return false
  return (Number(d.tienGiam) || 0) > 0 || !!d.tenGiamGiaUngDung || !!d.tenPhieuGiamGia
})

const tenGiamGia = computed(() => {
  const d = data.value
  if (!d) return ''
  return d.tenGiamGiaUngDung || d.tenPhieuGiamGia || 'Không áp dụng'
})

const chot = (v) => v !== null && v !== undefined && v !== ''

const timKiemSp = ref('')
const locMau = ref('')
const locKichCo = ref('')

const mauOptions = computed(() => {
  const set = new Set()
  for (const l of data.value?.chiTiet || []) if (l.tenMau) set.add(l.tenMau)
  return [...set]
})

const kichCoOptions = computed(() => {
  const set = new Set()
  for (const l of data.value?.chiTiet || []) if (l.tenKichCo) set.add(l.tenKichCo)
  return [...set]
})

const chiTietLoc = computed(() => {
  let list = data.value?.chiTiet || []
  const q = timKiemSp.value.trim().toLowerCase()
  if (q) {
    list = list.filter(
      (l) =>
        (l.tenSanPham || '').toLowerCase().includes(q) ||
        (l.maSanPhamChiTiet || '').toLowerCase().includes(q),
    )
  }
  if (locMau.value) list = list.filter((l) => l.tenMau === locMau.value)
  if (locKichCo.value) list = list.filter((l) => l.tenKichCo === locKichCo.value)
  return list
})

const load = async () => {
  const id = route.params.id
  if (!/^\d+$/.test(String(id))) {
    error.value = `ID hóa đơn không hợp lệ: ${id}`
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  data.value = null
  khInfo.value = null
  qrDataUrl.value = ''
  qrReady.value = false
  try {
    data.value = await getInvoice(id)
  } catch (e) {
    error.value = e.message || 'Không tải được chi tiết hóa đơn'
    loading.value = false
    return
  }
  if (data.value?.idKhachHang) {
    try {
      khInfo.value = await getCustomer(data.value.idKhachHang)
    } catch {
      khInfo.value = null
    }
  }
  await genQr()
  loading.value = false
}

const inHoaDon = async () => {
  if (!qrReady.value) {
    const ok = await genQr()
    if (!ok) {
      const tietKiem = await notifyConfirm(
        'Không tạo được mã QR',
        'Bạn vẫn muốn in hóa đơn mà không có mã QR?',
        'In không có QR',
        'Hủy in',
      )
      if (!tietKiem) return
    }
  }
  await nextTick()
  window.print()
}

watch(() => route.params.id, load)
onMounted(() => {
  load()
  document.addEventListener('click', onDocClick, true)
  document.addEventListener('keydown', onEscape)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick, true)
  document.removeEventListener('keydown', onEscape)
})
</script>

<template>
  <div class="inv-detail">
    <nav class="inv-crumb" aria-label="Breadcrumb">
      <router-link class="crumb-link" to="/hoa-don">Trang chủ</router-link>
      <span class="crumb-sep">/</span>
      <router-link class="crumb-link" to="/hoa-don">Quản lý hóa đơn</router-link>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">Chi tiết hóa đơn</span>
    </nav>

    <section v-if="loading" class="state-card">
      <div class="state-box">Đang tải dữ liệu...</div>
    </section>

    <section v-else-if="error" class="state-card">
      <div class="state-box is-error">
        <p class="state-msg">{{ error }}</p>
        <button class="btn btn-ghost" type="button" @click="router.push('/hoa-don')">
          ← Quay lại danh sách
        </button>
      </div>
    </section>

    <div v-else-if="data" class="inv-layout">
      <div class="inv-print-area">
        <article class="invoice-print-sheet print-only" :style="{ '--receipt-zoom': printZoom }">
          <header class="receipt-header">
            <div class="receipt-header-top">
              <div class="receipt-brand-block">
                <div class="receipt-brand">POLY<span>SHOES</span></div>
                <p class="receipt-tagline">Thời trang &amp; phong cách</p>
              </div>
              <div v-if="qrDataUrl" class="receipt-qr-block">
                <img class="qr-img" :src="qrDataUrl" alt="Mã QR hóa đơn" />
                <p class="qr-code">{{ data.maHoaDon }}</p>
              </div>
            </div>
            <h1>HÓA ĐƠN BÁN HÀNG</h1>
            <p class="receipt-code">Mã hóa đơn <strong>{{ data.maHoaDon }}</strong></p>
          </header>

          <section class="receipt-meta">
            <div><span>Ngày tạo</span><strong>{{ formatDateTime(data.ngayTao) }}</strong></div>
            <div><span>Loại đơn</span><strong>{{ data.loaiDon || '—' }}</strong></div>
            <div><span>Trạng thái</span><strong>{{ data.trangThai || '—' }}</strong></div>
            <div><span>Nhân viên</span><strong>{{ data.tenNhanVien || '—' }}</strong></div>
          </section>

          <section class="receipt-customer">
            <h2>Thông tin khách hàng</h2>
            <p><span>Khách hàng</span><strong>{{ data.tenKhachHang || 'Khách lẻ' }}</strong></p>
            <p><span>Điện thoại</span><strong>{{ data.soDienThoaiKhachHang || '—' }}</strong></p>
            <p><span>Địa chỉ</span><strong>{{ data.diaChiNhanHang || '—' }}</strong></p>
            <p v-if="data.ghiChu"><span>Ghi chú</span><strong>{{ data.ghiChu }}</strong></p>
          </section>

          <table class="receipt-table">
            <thead>
              <tr>
                <th class="receipt-index">#</th>
                <th>Sản phẩm</th>
                <th class="receipt-number">Đơn giá</th>
                <th class="receipt-number">SL</th>
                <th class="receipt-number">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(line, i) in data.chiTiet || []" :key="line.id || i">
                <td class="receipt-index">{{ i + 1 }}</td>
                <td>
                  <strong>{{ line.tenSanPham || 'Sản phẩm' }}</strong>
                  <small>{{ line.maSanPhamChiTiet || '—' }}<template v-if="line.tenMau || line.tenKichCo"> · {{ [line.tenMau, line.tenKichCo].filter(Boolean).join(' / ') }}</template></small>
                </td>
                <td class="receipt-number">{{ formatVnd(line.donGia) }}</td>
                <td class="receipt-number">{{ line.soLuong }}</td>
                <td class="receipt-number">{{ formatVnd(line.thanhTien) }}</td>
              </tr>
              <tr v-if="!(data.chiTiet || []).length"><td colspan="5" class="receipt-empty">Không có sản phẩm</td></tr>
            </tbody>
          </table>

          <section class="receipt-totals">
            <p><span>Tiền hàng</span><strong>{{ formatVnd(tienHang) }}</strong></p>
            <p v-if="coGiamGia"><span>Giảm giá <small>({{ tenGiamGia }})</small></span><strong>− {{ formatVnd(data.tienGiam) }}</strong></p>
            <p><span>Phí vận chuyển</span><strong>{{ formatVnd(data.phiVanChuyen) }}</strong></p>
            <p class="receipt-grand-total"><span>Tổng thanh toán</span><strong>{{ formatVnd(data.tienSauGiamGia) }}</strong></p>
            <p><span>Thanh toán</span><strong>{{ data.tenPhuongThucThanhToan || '—' }}</strong></p>
          </section>

          <footer class="receipt-footer">
            <div class="receipt-signatures"><span>Khách hàng<br><small>(Ký, ghi rõ họ tên)</small></span><span>Người bán hàng<br><small>(Ký, ghi rõ họ tên)</small></span></div>
            <strong>Cảm ơn quý khách và hẹn gặp lại!</strong>
            <small>Hóa đơn được in từ hệ thống quản lý PolyShoes.</small>
          </footer>
        </article>
        <div class="print-head print-only">
          <h2>PolyShoes — Hóa đơn {{ data.maHoaDon }}</h2>
          <p>Ngày tạo: {{ formatDateTime(data.ngayTao) }}</p>
        </div>

        <div class="main-col">
          <section class="card status-card">
            <div class="card-head">
              <span class="card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="4" y="3" width="16" height="18" rx="2" />
                  <path d="M8 8h8M8 12h8M8 16h5" />
                </svg>
              </span>
              <h3 class="card-title">Trạng thái đơn hàng</h3>
              <span class="inv-pill" :class="pillClass(data.trangThai)">{{ data.trangThai }}</span>
              <button class="status-history-link" type="button" @click="moLichSu">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v5l3 2" />
                </svg>
                Lịch sử thao tác
              </button>
            </div>

            <div v-if="isTerminal" class="terminal-box">
              <span class="terminal-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v6M12 16.5v.5" />
                </svg>
              </span>
              <div class="terminal-text">
                <strong>{{ data.trangThai }}</strong>
                <span>
                  {{ formatDateTime(data.ngayCapNhat) }}
                  <template v-if="chot(data.nguoiCapNhat)"> • {{ data.nguoiCapNhat }}</template>
                </span>
              </div>
            </div>

            <template v-else>
              <div class="stepper">
                <div
                  v-for="(s, i) in stepsList"
                  :key="s"
                  class="stepper-step"
                  :class="{
                    'is-done': laDaQua(s),
                    'is-active': laTrangThaiHienTai(s),
                    'is-next': laTrangThaiTiepTheo(s),
                  }"
                  :title="laTrangThaiHienTai(s) ? 'Trạng thái hiện tại' : (laDaQua(s) ? 'Đã hoàn thành' : undefined)"
                >
                  <span class="stepper-node">
                    <svg v-if="i === 0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="9" />
                      <path d="M12 7v5l3 2" />
                    </svg>
                    <svg v-else-if="i === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M9 11l2.5 2.5L16 8" />
                      <rect x="4" y="4" width="16" height="16" rx="3" />
                    </svg>
                    <svg v-else-if="i === 2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M3 8l9-4 9 4v8l-9 4-9-4V8z" />
                      <path d="M3 8l9 4 9-4M12 12v8" />
                    </svg>
                    <svg v-else-if="i === 3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M3 7h11v9H3zM14 10h4l3 3v3h-7" />
                      <circle cx="7" cy="18" r="2" />
                      <circle cx="17" cy="18" r="2" />
                    </svg>
                    <svg v-else-if="i === 4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 3l2.5 5 5.5.8-4 3.9.9 5.5L12 16.5 7.1 18.2l.9-5.5-4-3.9L9.5 8z" />
                    </svg>
                  </span>
                  <span class="stepper-label">{{ s }}</span>
                  <span v-if="i === 0" class="stepper-time">{{ formatDateTime(data.ngayTao) }}</span>
                  <span v-else-if="laTrangThaiHienTai(s) && i > 0" class="stepper-time">
                    {{ formatDateTime(data.ngayCapNhat) }}
                  </span>
                  <span v-else-if="laDaQua(s)" class="stepper-time stepper-done">✓ Hoàn thành</span>
                  <span v-else class="stepper-time">&nbsp;</span>
                </div>
              </div>

              <p v-if="data.ngayCapNhat" class="status-updated">
                Cập nhật lần cuối: {{ formatDateTime(data.ngayCapNhat) }}
                <template v-if="chot(data.nguoiCapNhat)"> • {{ data.nguoiCapNhat }}</template>
              </p>
            </template>
          </section>

          <div class="info-row">
            <section class="card info-card">
              <div class="card-head">
                <span class="card-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5" />
                  </svg>
                </span>
                <h3 class="card-title">Thông tin khách hàng</h3>
              </div>
              <div class="kv">
                <div class="kv-row">
                  <span class="kv-label">Tên khách hàng</span>
                  <span class="kv-value">{{ data.tenKhachHang || 'Khách lẻ' }}</span>
                </div>
                <div class="kv-row">
                  <span class="kv-label">Mã khách hàng</span>
                  <span class="kv-value">{{ khInfo?.maKhachHang || '—' }}</span>
                </div>
                <div class="kv-row">
                  <span class="kv-label">Số điện thoại</span>
                  <span class="kv-value">{{ data.soDienThoaiKhachHang || '—' }}</span>
                </div>
                <div class="kv-row">
                  <span class="kv-label">Email</span>
                  <span class="kv-value">{{ khInfo?.email || '—' }}</span>
                </div>
              </div>
            </section>

            <section class="card info-card">
              <div class="card-head">
                <span class="card-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11z" />
                    <circle cx="12" cy="10" r="2.5" />
                  </svg>
                </span>
                <h3 class="card-title">Thông tin giao hàng</h3>
              </div>
              <div class="kv">
                <div class="kv-row">
                  <span class="kv-label">Địa chỉ nhận hàng</span>
                  <span class="kv-value">{{ data.diaChiNhanHang || '—' }}</span>
                </div>
                <div class="kv-row">
                  <span class="kv-label">Loại đơn</span>
                  <span class="kv-value">{{ data.loaiDon || '—' }}</span>
                </div>
                <div class="kv-row">
                  <span class="kv-label">Ghi chú</span>
                  <span class="kv-value">{{ data.ghiChu || '—' }}</span>
                </div>
                <div class="kv-row">
                  <span class="kv-label">Nhân viên</span>
                  <span class="kv-value">{{ data.tenNhanVien || '—' }}</span>
                </div>
              </div>
            </section>
          </div>

          <section class="card products-card">
            <div class="card-head">
              <span class="card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 8l9-4 9 4v8l-9 4-9-4V8z" />
                  <path d="M3 8l9 4 9-4M12 12v8" />
                </svg>
              </span>
              <h3 class="card-title">Danh sách sản phẩm</h3>
              <span class="count-pill">{{ (data.chiTiet || []).length }} SP</span>
            </div>

            <div class="prod-filters">
              <div class="filter-search">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8" />
                  <path d="M21 21l-4.35-4.35" />
                </svg>
                <input v-model="timKiemSp" type="text" placeholder="Tìm theo tên hoặc SKU..." />
              </div>
              <select v-model="locMau" class="filter-select">
                <option value="">Tất cả màu sắc</option>
                <option v-for="m in mauOptions" :key="m" :value="m">{{ m }}</option>
              </select>
              <select v-model="locKichCo" class="filter-select">
                <option value="">Tất cả kích cỡ</option>
                <option v-for="k in kichCoOptions" :key="k" :value="k">{{ k }}</option>
              </select>
            </div>

            <div class="table-wrap">
              <table class="prod-table">
                <thead>
                  <tr>
                    <th class="th-stt">STT</th>
                    <th class="th-img">Ảnh</th>
                    <th>Tên sản phẩm</th>
                    <th>Màu sắc</th>
                    <th>Kích cỡ</th>
                    <th class="num">Số lượng</th>
                    <th class="num">Đơn giá</th>
                    <th class="num">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!chiTietLoc.length">
                    <td colspan="8" class="empty">Không có dòng sản phẩm nào</td>
                  </tr>
                  <tr v-for="(line, i) in chiTietLoc" :key="line.id || i">
                    <td class="td-stt">{{ i + 1 }}</td>
                    <td class="td-img">
                      <div class="ph-img">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                          <rect x="3" y="4" width="18" height="16" rx="2" />
                          <circle cx="9" cy="10" r="2" />
                          <path d="M3 17l5-4 4 3 3-2 6 5" />
                        </svg>
                      </div>
                    </td>
                    <td>
                      <div class="p-name">{{ line.tenSanPham || '—' }}</div>
                      <div class="p-sku">{{ line.maSanPhamChiTiet || '—' }}</div>
                    </td>
                    <td>{{ line.tenMau || '—' }}</td>
                    <td>{{ line.tenKichCo || '—' }}</td>
                    <td class="num">{{ line.soLuong }}</td>
                    <td class="num">{{ formatVnd(line.donGia) }}</td>
                    <td class="num strong">{{ formatVnd(line.thanhTien) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <div class="side-col">
          <section class="card totals-card">
            <div class="card-head">
              <span class="card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="4" y="3" width="16" height="18" rx="2" />
                  <path d="M8 8h8M8 12h8M8 16h4" />
                </svg>
              </span>
              <h3 class="card-title">Tổng kết thanh toán</h3>
            </div>
            <div class="sum-rows">
              <div class="sum-row">
                <span>Tổng tiền hàng</span>
                <span>{{ formatVnd(tienHang) }}</span>
              </div>
              <div class="sum-row">
                <span>Giảm giá</span>
                <span v-if="coGiamGia" class="sum-discount">
                  − {{ formatVnd(data.tienGiam) }}
                </span>
                <span v-else>− 0 ₫</span>
              </div>
              <div class="sum-row">
                <span>Phí vận chuyển</span>
                <span>+ {{ formatVnd(data.phiVanChuyen) }}</span>
              </div>
              <div class="sum-row sum-final">
                <span>Tổng tiền</span>
                <span class="sum-total">{{ formatVnd(data.tienSauGiamGia) }}</span>
              </div>
            </div>
          </section>

          <section class="card pay-card">
            <div class="card-head">
              <span class="card-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="6" width="18" height="12" rx="2" />
                  <path d="M3 10h18" />
                </svg>
              </span>
              <h3 class="card-title">Lịch sử thanh toán</h3>
            </div>
            <div class="pay-row">
              <div class="pay-main">
                <span class="pay-label">Phương thức thanh toán</span>
                <strong>{{ data.tenPhuongThucThanhToan || '—' }}</strong>
              </div>
              <span class="inv-pill" :class="pillClass(payStatusOf(data.trangThai))">
                {{ payStatusOf(data.trangThai) }}
              </span>
            </div>
            <div class="pay-amount-row">
              <span class="pay-label">Số tiền</span>
              <span class="pay-amount">{{ formatVnd(data.tienSauGiamGia) }}</span>
            </div>
            <p class="pay-note">Lập đơn: {{ formatDateTime(data.ngayTao) }}</p>
          </section>

          <div class="side-actions">
            <button class="btn btn-primary" type="button" @click="inHoaDon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 9V3h12v6M6 18H4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2" />
                <rect x="6" y="14" width="12" height="7" />
              </svg>
              In hóa đơn
            </button>
            <div v-if="!isTerminal && tiepTheoList.length" ref="statusMenuRef" class="status-menu-wrap">
              <button
                class="btn btn-outline"
                type="button"
                :disabled="updatingStatus"
                @click="toggleStatusMenu"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
                {{ updatingStatus ? 'Đang xử lý...' : 'Chuyển trạng thái' }}
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M6 9l6 6 6-6" />
                </svg>
              </button>
              <div v-if="showStatusMenu" class="status-menu" role="menu">
                <button
                  v-for="tt in tiepTheoList"
                  :key="tt"
                  class="status-menu-item"
                  type="button"
                  role="menuitem"
                  @click="chonMenu(tt)"
                >
                  {{ tt }}
                </button>
              </div>
            </div>
            <button class="btn btn-outline" type="button" @click="router.push('/hoa-don')">
              Quay lại danh sách
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showHistory" class="history-overlay" @click.self="dongLichSu">
      <div class="history-modal" role="dialog" aria-modal="true" aria-label="Lịch sử thao tác hóa đơn">
        <div class="history-head">
          <h3 class="history-title">
            Lịch sử thao tác
            <span v-if="data" class="history-code">{{ data.maHoaDon }}</span>
          </h3>
          <button class="history-close" type="button" aria-label="Đóng" @click="dongLichSu">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="history-body">
          <section v-if="historyLoading" class="history-state">
            Đang tải lịch sử thao tác...
          </section>
          <section v-else-if="historyError" class="history-state is-error">
            <p>{{ historyError }}</p>
            <button class="btn btn-outline" type="button" @click="moLichSu">Thử lại</button>
          </section>
          <section v-else-if="!historyList.length" class="history-state">
            Chưa có lịch sử thao tác cho hóa đơn này.
          </section>
          <ol v-else class="history-timeline">
            <li v-for="row in historyList" :key="row.id" class="history-item">
              <span class="history-dot" aria-hidden="true"></span>
              <div class="history-content">
                <p class="history-transition">
                  <span v-if="row.trangThaiTruoc">{{ row.trangThaiTruoc }}</span>
                  <span v-else class="history-muted">—</span>
                  <span class="history-arrow">→</span>
                  <strong>{{ row.trangThaiSau }}</strong>
                </p>
                <p class="history-meta">
                  <time>{{ formatDateTime(row.thoiGian) }}</time>
                </p>
                <p v-if="row.ghiChu" class="history-note">{{ row.ghiChu }}</p>
              </div>
            </li>
          </ol>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.inv-crumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: #94a3b8;
  margin-bottom: 16px;
}

.crumb-link {
  color: #64748b;
  text-decoration: none;
  font-weight: 500;
}

.crumb-link:hover {
  color: #dc2626;
  text-decoration: underline;
}

.crumb-sep {
  color: #cbd5e1;
  font-weight: 500;
}

.crumb-current {
  color: #475569;
  font-weight: 600;
}

.inv-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  align-items: start;
}

.inv-print-area {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.main-col {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-col {
  flex: 0 0 340px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.side-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.print-only {
  display: none;
}

.card {
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
  padding: 18px 20px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 14px;
}

.card-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #fef2f2;
  color: #dc2626;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.card-icon svg {
  width: 15px;
  height: 15px;
}

.card-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  flex: 1;
}

.inv-pill {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.inv-pill-green { background: #f0fdf4; color: #15803d; }
.inv-pill-red { background: #fef2f2; color: #dc2626; }
.inv-pill-amber { background: #fffbeb; color: #b45309; }
.inv-pill-teal { background: #f0fdfa; color: #0f766e; }
.inv-pill-gray { background: #f1f5f9; color: #64748b; }

.status-history-link {
  appearance: none;
  border: 0;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #dc2626;
  cursor: pointer;
  white-space: nowrap;
  padding: 4px 6px;
  border-radius: 8px;
  transition: background 0.15s ease, color 0.15s ease;
}

.status-history-link:hover {
  background: #fef2f2;
  color: #b91c1c;
}

.status-history-link svg {
  width: 14px;
  height: 14px;
}

.stepper {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 4px;
  padding: 14px 6px 8px;
}

.stepper-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
  text-align: center;
  min-width: 0;
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  cursor: default;
}

button.stepper-step {
  cursor: default;
}

button.stepper-step.is-next {
  cursor: pointer;
}

button.stepper-step.is-next:hover:not(:disabled) .stepper-node {
  background: #fef2f2;
  border-color: #dc2626;
  box-shadow: 0 0 0 4px rgba(220, 38, 38, 0.1);
  transform: scale(1.08);
}

button.stepper-step.is-next:hover:not(:disabled) .stepper-label {
  color: #dc2626;
}

button.stepper-step.is-next:disabled {
  cursor: wait;
  opacity: 0.6;
}

.stepper-step::before {
  content: '';
  position: absolute;
  top: 17px;
  right: 50%;
  width: 100%;
  height: 2px;
  background: #e2e8f0;
  z-index: 0;
}

.stepper-step:first-child::before {
  display: none;
}

.stepper-step.is-done::before,
.stepper-step.is-active::before {
  background: #dc2626;
}

.stepper-node {
  position: relative;
  z-index: 1;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  border: 2px solid #e2e8f0;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.stepper-node svg {
  width: 16px;
  height: 16px;
}

.stepper-step.is-done .stepper-node {
  background: #dc2626;
  border-color: #dc2626;
  color: #fff;
}

.stepper-step.is-active .stepper-node {
  background: #fff;
  border-color: #dc2626;
  color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.stepper-step.is-next .stepper-node {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #dc2626;
}

.stepper-label {
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  line-height: 1.25;
}

.stepper-step.is-done .stepper-label,
.stepper-step.is-active .stepper-label {
  color: #1e293b;
}

.stepper-step:not(.is-done):not(.is-active) .stepper-label {
  color: #94a3b8;
  font-weight: 500;
}

.stepper-time {
  font-size: 10px;
  color: #94a3b8;
  line-height: 1.3;
  white-space: nowrap;
}

.stepper-hint {
  color: #dc2626;
  font-weight: 600;
  font-size: 10px;
}

.stepper-done {
  color: #15803d;
  font-weight: 600;
  font-size: 10px;
}

.count-pill {
  background: #f1f5f9;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 9999px;
  white-space: nowrap;
}

.prod-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.filter-search {
  flex: 1 1 200px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 11px;
  background: #f8fafc;
  transition: border-color 0.15s, background 0.15s;
}

.filter-search:focus-within {
  border-color: #dc2626;
  background: #fff;
}

.filter-search svg {
  width: 15px;
  height: 15px;
  color: #94a3b8;
  flex: 0 0 auto;
}

.filter-search input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #1e293b;
  padding: 8px 0;
  width: 100%;
}

.filter-search input::placeholder {
  color: #94a3b8;
}

.filter-select {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 11px;
  font-size: 12.5px;
  color: #334155;
  background: #fff;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
}

.filter-select:focus {
  border-color: #dc2626;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #fff;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: 0 2px 6px rgba(220, 38, 38, 0.25);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(220, 38, 38, 0.3);
}

.btn-primary svg {
  width: 16px;
  height: 16px;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 18px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.btn-outline:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #1e293b;
}

.status-updated {
  margin-top: 10px;
  font-size: 11.5px;
  color: #94a3b8;
}

.terminal-box {
  display: flex;
  align-items: center;
  gap: 13px;
  background: #fef2f2;
  border: 1px solid #fecdd3;
  border-radius: 12px;
  padding: 16px 18px;
}

.terminal-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #dc2626;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.terminal-icon svg {
  width: 22px;
  height: 22px;
}

.terminal-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.terminal-text strong {
  font-size: 15px;
  color: #dc2626;
}

.terminal-text span {
  font-size: 12px;
  color: #7f1d1d;
}

.kv {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.kv-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 14px;
}

.kv-label {
  font-size: 12.5px;
  color: #94a3b8;
  flex: 0 0 auto;
}

.kv-value {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
  text-align: right;
  word-break: break-word;
}

.sum-rows {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  font-size: 13.5px;
  color: #475569;
}

.sum-row span:last-child {
  font-weight: 600;
  color: #0f172a;
}

.sum-discount {
  color: #15803d !important;
}

.sum-final {
  border-top: 1px dashed #e2e8f0;
  padding-top: 11px;
  margin-top: 2px;
}

.sum-final span {
  font-size: 15px !important;
  font-weight: 700 !important;
}

.sum-final span:last-child {
  color: #dc2626 !important;
  font-size: 17px !important;
}

.pay-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.pay-label {
  font-size: 11.5px;
  color: #94a3b8;
  display: block;
  margin-bottom: 2px;
}

.pay-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.pay-main strong {
  font-size: 13.5px;
  color: #0f172a;
}

.pay-amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.pay-amount {
  font-size: 16px;
  font-weight: 700;
  color: #dc2626;
}

.pay-note {
  margin: 12px 0 0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
}

.status-menu-wrap {
  position: relative;
  width: 100%;
}

.status-menu-wrap > .btn {
  width: 100%;
  justify-content: space-between;
}

.status-menu {
  position: absolute;
  z-index: 40;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #fecdd3;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.status-menu-item {
  appearance: none;
  border: 0;
  background: transparent;
  text-align: left;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.status-menu-item:hover,
.status-menu-item:focus-visible {
  background: #fef2f2;
  color: #dc2626;
  outline: none;
}

.history-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.history-modal {
  width: min(560px, 100%);
  max-height: min(80vh, 640px);
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid #e2e8f0;
}

.history-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-code {
  font-size: 12px;
  font-weight: 700;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecdd3;
  border-radius: 999px;
  padding: 3px 10px;
}

.history-close {
  appearance: none;
  border: 0;
  background: transparent;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.history-close:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.history-close svg {
  width: 16px;
  height: 16px;
}

.history-body {
  padding: 16px 18px 20px;
  overflow-y: auto;
}

.history-state {
  margin: 0;
  padding: 22px 12px;
  text-align: center;
  font-size: 13px;
  color: #64748b;
}

.history-state.is-error {
  color: #dc2626;
}

.history-state .btn {
  margin-top: 10px;
}

.history-timeline {
  list-style: none;
  margin: 0;
  padding: 0 0 0 18px;
  position: relative;
}

.history-timeline::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: #fecdd3;
}

.history-item {
  position: relative;
  padding: 0 0 16px 10px;
}

.history-item:last-child {
  padding-bottom: 0;
}

.history-dot {
  position: absolute;
  left: -18px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #dc2626;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #fecdd3;
}

.history-transition {
  margin: 0;
  font-size: 13px;
  color: #0f172a;
  line-height: 1.5;
}

.history-transition strong {
  color: #dc2626;
}

.history-arrow {
  color: #94a3b8;
  margin: 0 6px;
}

.history-muted {
  color: #94a3b8;
}

.history-meta {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: #94a3b8;
}

.history-note {
  margin: 6px 0 0;
  font-size: 12px;
  color: #475569;
  background: #f8fafc;
  border-left: 3px solid #fecdd3;
  border-radius: 0 8px 8px 0;
  padding: 6px 10px;
  line-height: 1.5;
}

.table-wrap {
  overflow-x: auto;
}

.prod-table {
  width: 100%;
  border-collapse: collapse;
}

.prod-table th {
  font-size: 11.5px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.prod-table td {
  padding: 11px 12px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
  color: #334155;
  vertical-align: middle;
}

.prod-table .num {
  text-align: right;
  white-space: nowrap;
}

.prod-table .strong {
  font-weight: 700;
}

.th-img {
  width: 52px;
}

.th-stt {
  width: 40px;
  text-align: center !important;
}

.td-stt {
  text-align: center !important;
  color: #94a3b8;
  font-size: 12px;
}

.td-img {
  width: 52px;
}

.ph-img {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ph-img svg {
  width: 20px;
  height: 20px;
}

.p-name {
  font-weight: 700;
  color: #0f172a;
}

.p-sku {
  font-size: 11.5px;
  color: #94a3b8;
  margin-top: 2px;
}

.p-variant {
  font-size: 12.5px;
  color: #475569;
}

.empty {
  text-align: center;
  color: #94a3b8;
  padding: 22px 0 !important;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 20px;
  border: none;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn svg {
  width: 16px;
  height: 16px;
}

.btn-ghost {
  background: #ffffff;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.btn-ghost:hover {
  background: #f8fafc;
  color: #0f172a;
}

.state-card {
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
  padding: 8px 20px;
}

.state-box {
  padding: 26px 0;
  text-align: center;
  color: #94a3b8;
  font-size: 13.5px;
}

.state-box.is-error .state-msg {
  color: #dc2626;
  font-weight: 600;
  margin-bottom: 10px;
}

@media (max-width: 980px) {
  .inv-layout {
    grid-template-columns: 1fr;
  }

  .inv-print-area {
    flex-direction: column;
  }

  .side-col {
    flex: 1 1 auto;
    width: 100%;
  }

  .info-row {
    grid-template-columns: 1fr;
  }

  .stepper {
    flex-wrap: wrap;
    gap: 12px;
  }

  .stepper-step {
    flex: 1 1 30%;
  }

  .stepper-step::before {
    display: none;
  }
}
</style>

<style>
@media print {
  @page {
    size: A4 portrait;
    margin: 8mm;
  }

  body * {
    visibility: hidden !important;
  }

  .invoice-print-sheet,
  .invoice-print-sheet * {
    visibility: visible !important;
  }

  .inv-print-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100% !important;
    display: block !important;
  }

  .invoice-print-sheet {
    display: block !important;
    width: 100%;
    max-width: 180mm;
    margin: 0 auto;
    color: #202124;
    background: #fff;
    font-family: Arial, sans-serif;
    font-size: 9.5pt;
    line-height: 1.35;
    zoom: var(--receipt-zoom, 1);
  }

  .receipt-header {
    padding-bottom: 8px;
    border-bottom: 2px solid #222;
  }

  .receipt-header-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .receipt-brand-block {
    flex: 1;
  }

  .receipt-brand {
    color: #c90016;
    font-size: 19pt;
    font-weight: 800;
    letter-spacing: 2px;
    margin: 0;
  }

  .receipt-brand span {
    color: #222;
  }

  .receipt-tagline {
    margin: 2px 0 0;
    color: #555;
    font-size: 8.5pt;
  }

  .receipt-qr-block {
    flex: 0 0 auto;
    text-align: right;
    margin-left: 10px;
  }

  .receipt-qr-block .qr-img {
    display: block;
    width: 95px;
    height: 95px;
    image-rendering: pixelated;
  }

  .receipt-qr-block .qr-code {
    margin: 2px 0 0;
    font-size: 7.5pt;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: #444;
  }

  .receipt-header h1 {
    margin: 8px 0 2px;
    font-size: 14pt;
    letter-spacing: 1px;
    text-align: center;
  }

  .receipt-code {
    margin: 0;
    color: #444;
    text-align: center;
  }

  .receipt-code strong {
    font-size: 11pt;
    color: #c90016;
    letter-spacing: 0.5px;
  }

  .receipt-meta {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 3px 20px;
    padding: 8px 0;
    border-bottom: 1px solid #bbb;
  }

  .receipt-meta div,
  .receipt-customer p,
  .receipt-totals p {
    display: flex;
    justify-content: space-between;
    gap: 14px;
    margin: 0;
  }

  .receipt-meta span,
  .receipt-customer p span,
  .receipt-totals p span {
    color: #555;
  }

  .receipt-customer {
    padding: 8px 0;
    border-bottom: 1px solid #bbb;
  }

  .receipt-customer h2 {
    margin: 0 0 5px;
    font-size: 9pt;
    text-transform: uppercase;
  }

  .receipt-customer p + p {
    margin-top: 2px;
  }

  .receipt-customer p strong {
    text-align: right;
    overflow-wrap: anywhere;
  }

  .receipt-table {
    width: 100%;
    margin-top: 8px;
    border-collapse: collapse;
    table-layout: auto;
  }

  .receipt-table th {
    padding: 5px 4px;
    border-top: 1px solid #222;
    border-bottom: 1px solid #222;
    text-align: left;
    font-size: 8.5pt;
  }

  .receipt-table td {
    padding: 4px;
    border-bottom: 1px solid #ddd;
    vertical-align: top;
  }

  .receipt-table td strong,
  .receipt-table td small {
    display: block;
  }

  .receipt-table td small {
    margin-top: 1px;
    color: #555;
    font-size: 7.5pt;
  }

  .receipt-number {
    text-align: right !important;
    white-space: nowrap;
  }

  .receipt-index {
    width: 22px;
    text-align: center !important;
  }

  .receipt-empty {
    padding: 12px !important;
    text-align: center;
  }

  .receipt-totals {
    width: 72%;
    margin: 8px 0 0 auto;
  }

  .receipt-totals p {
    padding: 2px 0;
  }

  .receipt-totals p small {
    color: #666;
  }

  .receipt-grand-total {
    margin: 3px 0 !important;
    padding: 6px 0 !important;
    border-top: 1px solid #222;
    border-bottom: 1px solid #222;
    font-size: 11pt;
  }

  .receipt-grand-total strong {
    color: #b00014;
    font-size: 13pt;
  }

  .receipt-footer {
    margin-top: 12px;
    text-align: center;
  }

  .receipt-signatures {
    display: flex;
    justify-content: space-between;
    margin-bottom: 18px;
    text-align: center;
    font-weight: 700;
  }

  .receipt-signatures span {
    width: 40%;
  }

  .receipt-signatures small,
  .receipt-footer > small {
    color: #666;
    font-weight: 400;
  }

  .receipt-footer > strong,
  .receipt-footer > small {
    display: block;
  }

  .receipt-footer > strong {
    margin-bottom: 3px;
    font-size: 10pt;
  }

  .print-head,
  .inv-crumb,
  .main-col,
  .side-col,
  .status-card,
  .products-card,
  .state-card,
  .side-actions,
  .history-overlay {
    display: none !important;
  }
}
</style>
