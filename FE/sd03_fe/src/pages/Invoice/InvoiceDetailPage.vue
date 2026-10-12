<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getInvoice } from '@/api/invoice'
import { getCustomer } from '@/api/customer'
import { formatDateTime, formatVnd, payStatusOf } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const data = ref(null)
const khInfo = ref(null)

const STEPS = [
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
]

const TERMINAL = ['Đã hủy', 'Đã hoàn tiền']

const isTerminal = computed(
  () => !!data.value && TERMINAL.includes(data.value.trangThai),
)

const stepIndex = computed(() =>
  data.value ? STEPS.indexOf(data.value.trangThai) : -1,
)

const stepClass = (i) => {
  if (i < stepIndex.value) return 'is-done'
  if (i === stepIndex.value) return 'is-active'
  return 'is-pending'
}

const pillClass = (value) => {
  if (['Đã hoàn thành', 'Đã giao hàng', 'Đã xác nhận'].includes(value))
    return 'inv-pill-green'
  if (['Đã hủy', 'Đã hoàn tiền'].includes(value)) return 'inv-pill-red'
  if (['Chờ xác nhận', 'Chờ giao hàng'].includes(value)) return 'inv-pill-amber'
  if (['Đang giao hàng'].includes(value)) return 'inv-pill-teal'
  return 'inv-pill-gray'
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
  loading.value = false
}

const inHoaDon = () => window.print()

watch(() => route.params.id, load)
onMounted(load)
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
            <div class="receipt-brand">POLY<span>SHOES</span></div>
            <p class="receipt-tagline">Thời trang &amp; phong cách</p>
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

          <div v-else class="steps">
            <div v-for="(s, i) in STEPS" :key="s" class="step" :class="stepClass(i)">
              <span class="step-node">
                <svg v-if="i === 0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v5l3 2" />
                </svg>
                <svg v-else-if="i === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 11l2.5 2.5L16 8" />
                  <rect x="4" y="4" width="16" height="16" rx="3" />
                </svg>
                <svg v-else-if="i === 2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 8l9-4 9 4v8l-9 4-9-4V8z" />
                  <path d="M3 8l9 4 9-4M12 12v8" />
                </svg>
                <svg v-else-if="i === 3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 7h11v9H3zM14 10h4l3 3v3h-7" />
                  <circle cx="7" cy="18" r="2" />
                  <circle cx="17" cy="18" r="2" />
                </svg>
                <svg v-else-if="i === 4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 6L9 17l-5-5" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 3l2.5 5 5.5.8-4 3.9.9 5.5L12 16.5 7.1 18.2l.9-5.5-4-3.9L9.5 8z" />
                </svg>
              </span>
              <span class="step-label">{{ s }}</span>
              <span v-if="i === 0" class="step-meta">{{ formatDateTime(data.ngayTao) }}</span>
              <span
                v-else-if="i === stepIndex && stepIndex > 0"
                class="step-meta"
              >
                {{ formatDateTime(data.ngayCapNhat) }}
                <template v-if="chot(data.nguoiCapNhat)"> • {{ data.nguoiCapNhat }}</template>
              </span>
            </div>
          </div>
        </section>

        <section class="card">
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
              <span class="kv-label">Số điện thoại</span>
              <span class="kv-value">{{ data.soDienThoaiKhachHang || '—' }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-label">Email</span>
              <span class="kv-value">{{ khInfo?.email || '—' }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-label">Mã khách hàng</span>
              <span class="kv-value">{{ khInfo?.maKhachHang || '—' }}</span>
            </div>
          </div>
        </section>

        <section class="card">
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
              <span class="kv-label">Nhân viên phụ trách</span>
              <span class="kv-value">{{ data.tenNhanVien || '—' }}</span>
            </div>
          </div>
        </section>

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
              <span>Phiếu giảm giá</span>
              <span v-if="coGiamGia" class="sum-discount">
                {{ tenGiamGia }}
                <template v-if="(Number(data.tienGiam) || 0) > 0">
                  (-{{ formatVnd(data.tienGiam) }})
                </template>
              </span>
              <span v-else>—</span>
            </div>
            <div class="sum-row">
              <span>Phí vận chuyển</span>
              <span>+ {{ formatVnd(data.phiVanChuyen) }}</span>
            </div>
            <div class="sum-row sum-final">
              <span>Tổng tiền</span>
              <span>{{ formatVnd(data.tienSauGiamGia) }}</span>
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
              <strong>{{ data.tenPhuongThucThanhToan || '—' }}</strong>
              <span class="pay-sub">Lập đơn: {{ formatDateTime(data.ngayTao) }}</span>
            </div>
            <span class="inv-pill" :class="pillClass(payStatusOf(data.trangThai))">
              {{ payStatusOf(data.trangThai) }}
            </span>
          </div>
          <div class="pay-amount">{{ formatVnd(data.tienSauGiamGia) }}</div>
          <p class="pay-note">Thông tin lấy từ hóa đơn, chưa bao gồm lịch sử giao dịch chi tiết.</p>
        </section>

        <section class="card products-card">
          <div class="card-head">
            <span class="card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 8l9-4 9 4v8l-9 4-9-4V8z" />
                <path d="M3 8l9 4 9-4M12 12v8" />
              </svg>
            </span>
            <h3 class="card-title">Danh sách sản phẩm</h3>
            <span class="count-pill">({{ (data.chiTiet || []).length }})</span>
          </div>
          <div class="table-wrap">
            <table class="prod-table">
              <thead>
                <tr>
                  <th class="th-img"></th>
                  <th>Sản phẩm</th>
                  <th>Phân loại</th>
                  <th class="num">Đơn giá</th>
                  <th class="num">Số lượng</th>
                  <th class="num">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!(data.chiTiet || []).length">
                  <td colspan="6" class="empty">Không có dòng sản phẩm nào</td>
                </tr>
                <tr v-for="(line, i) in data.chiTiet" :key="line.id || i">
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
                    <div class="p-sku">SKU: {{ line.maSanPhamChiTiet || '—' }}</div>
                  </td>
                  <td>
                    <div class="p-variant">
                      {{ [line.tenMau, line.tenKichCo].filter(Boolean).join(' • ') || '—' }}
                    </div>
                  </td>
                  <td class="num">{{ formatVnd(line.donGia) }}</td>
                  <td class="num">{{ line.soLuong }}</td>
                  <td class="num strong">{{ formatVnd(line.thanhTien) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <div class="action-bar">
          <button class="btn btn-print" type="button" @click="inHoaDon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 9V3h12v6M6 18H4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2" />
              <rect x="6" y="14" width="12" height="7" />
            </svg>
            In hóa đơn
          </button>
          <button class="btn btn-ghost" type="button" @click="router.push('/hoa-don')">
            Quay lại danh sách
          </button>
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
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
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

.status-card,
.products-card,
.action-bar {
  grid-column: span 2;
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

.steps {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 6px;
  padding: 10px 4px 4px;
}

.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  position: relative;
  text-align: center;
  min-width: 0;
}

.step::before {
  content: '';
  position: absolute;
  top: 19px;
  right: 50%;
  width: 100%;
  height: 2px;
  background: #e2e8f0;
  z-index: 0;
}

.step:first-child::before {
  display: none;
}

.step.is-done::before,
.step.is-active::before {
  background: #dc2626;
}

.step-node {
  position: relative;
  z-index: 1;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  border: 2px solid #e2e8f0;
}

.step-node svg {
  width: 18px;
  height: 18px;
}

.step.is-done .step-node {
  background: #dc2626;
  border-color: #dc2626;
  color: #fff;
}

.step.is-active .step-node {
  background: #fff;
  border-color: #dc2626;
  color: #dc2626;
}

.step-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #475569;
}

.step.is-pending .step-label {
  color: #94a3b8;
  font-weight: 500;
}

.step-meta {
  font-size: 10.5px;
  color: #94a3b8;
  line-height: 1.4;
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

.pay-sub {
  font-size: 11.5px;
  color: #94a3b8;
}

.pay-amount {
  margin-top: 10px;
  text-align: right;
  font-size: 15px;
  font-weight: 700;
  color: #dc2626;
}

.pay-note {
  margin: 12px 0 0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
}

.count-pill {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
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

.action-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
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

.btn-print {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.btn-print:hover {
  background: linear-gradient(135deg, #b91c1c 0%, #991b1b 100%);
  transform: translateY(-1px);
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
    grid-template-columns: 1fr;
  }

  .status-card,
  .products-card,
  .action-bar {
    grid-column: span 1;
  }

  .steps {
    flex-wrap: wrap;
    gap: 14px;
  }

  .step {
    flex: 1 1 40%;
  }

  .step::before {
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
    font-size: 10pt;
    line-height: 1.45;
    zoom: var(--receipt-zoom, 1);
    break-inside: avoid-page;
  }

  .receipt-header {
    text-align: center;
    padding-bottom: 12px;
    border-bottom: 2px solid #222;
  }

  .receipt-brand {
    color: #c90016;
    font-size: 21pt;
    font-weight: 800;
    letter-spacing: 2px;
  }

  .receipt-brand span {
    color: #222;
  }

  .receipt-tagline {
    margin: 0 0 10px;
    color: #555;
    font-size: 9pt;
  }

  .receipt-header h1 {
    margin: 10px 0 3px;
    font-size: 16pt;
    letter-spacing: 1px;
  }

  .receipt-code {
    margin: 0;
    color: #444;
  }

  .receipt-code strong {
    font-size: 12pt;
    color: #c90016;
    letter-spacing: 0.5px;
  }

  .receipt-meta {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 5px 20px;
    padding: 12px 0;
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
    padding: 11px 0;
    border-bottom: 1px solid #bbb;
  }

  .receipt-customer h2 {
    margin: 0 0 7px;
    font-size: 10pt;
    text-transform: uppercase;
  }

  .receipt-customer p + p {
    margin-top: 4px;
  }

  .receipt-customer p strong {
    text-align: right;
    overflow-wrap: anywhere;
  }

  .receipt-table {
    width: 100%;
    margin-top: 12px;
    border-collapse: collapse;
    table-layout: auto;
  }

  .receipt-table th {
    padding: 7px 5px;
    border-top: 1px solid #222;
    border-bottom: 1px solid #222;
    text-align: left;
    font-size: 9pt;
  }

  .receipt-table td {
    padding: 7px 5px;
    border-bottom: 1px solid #ddd;
    vertical-align: top;
  }

  .receipt-table td strong,
  .receipt-table td small {
    display: block;
  }

  .receipt-table td small {
    margin-top: 2px;
    color: #555;
    font-size: 8pt;
  }

  .receipt-number {
    text-align: right !important;
    white-space: nowrap;
  }

  .receipt-index {
    width: 24px;
    text-align: center !important;
  }

  .receipt-empty {
    padding: 16px !important;
    text-align: center;
  }

  .receipt-totals {
    width: 72%;
    margin: 12px 0 0 auto;
  }

  .receipt-totals p {
    padding: 4px 0;
  }

  .receipt-totals p small {
    color: #666;
  }

  .receipt-grand-total {
    margin: 4px 0 !important;
    padding: 9px 0 !important;
    border-top: 1px solid #222;
    border-bottom: 1px solid #222;
    font-size: 12pt;
  }

  .receipt-grand-total strong {
    color: #b00014;
    font-size: 14pt;
  }

  .receipt-footer {
    margin-top: 18px;
    text-align: center;
  }

  .receipt-signatures {
    display: flex;
    justify-content: space-between;
    margin-bottom: 30px;
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
    margin-bottom: 4px;
    font-size: 11pt;
  }

  .print-head,
  .inv-crumb,
  .action-bar {
    display: none !important;
  }

  tr,
  .receipt-customer,
  .receipt-totals {
    break-inside: avoid;
  }
}
</style>
