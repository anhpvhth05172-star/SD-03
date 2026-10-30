<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDoiSoat, getTongKet } from '@/api/giaoCa'
import { formatVnd, formatDateTime } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'
import { inBienLaiDoiSoatK80, inBienBanDoiSoatA4 } from '@/utils/printShiftReceipt'
import { taoVaTaiPdf } from '@/utils/pdf'

const route = useRoute()
const router = useRouter()

const id = route.params.id
const loading = ref(true)
const caData = ref(null)
const tongKetData = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const [doiSoatRes, tongKetRes] = await Promise.allSettled([
      getDoiSoat(id),
      getTongKet(id)
    ])
    if (doiSoatRes.status === 'fulfilled') {
      caData.value = doiSoatRes.value
    }
    if (tongKetRes.status === 'fulfilled') {
      tongKetData.value = tongKetRes.value
    }
    if (!caData.value && !tongKetData.value) {
      notifyError('Không tìm thấy ca làm việc', 'Mã ca không tồn tại hoặc bạn không có quyền truy cập.')
    }
  } catch (err) {
    notifyError('Lỗi tải thông tin ca', err.message || 'Không thể lấy dữ liệu đối soát ca.')
  } finally {
    loading.value = false
  }
}

const statusText = computed(() => {
  const st = caData.value?.trangThai || tongKetData.value?.trangThai
  if (st === 'DANG_MO') return 'Đang mở'
  if (st === 'DA_KET_CA') return 'Đã kết ca'
  return st || '—'
})

const statusClass = computed(() => {
  const st = caData.value?.trangThai || tongKetData.value?.trangThai
  return st === 'DANG_MO' ? 'badge-open' : 'badge-closed'
})

const chenhLechNumber = computed(() => {
  return Number(caData.value?.chenhLech || 0)
})

// 1. In phiếu nhiệt K80 (80mm) cho máy in bill quầy
const inPhieuK80 = () => {
  inBienLaiDoiSoatK80(caData.value, tongKetData.value)
}

// 2. In biên bản khổ A4 chuẩn có Quốc hiệu và chữ ký 2 bên
const inBienBanA4 = () => {
  inBienBanDoiSoatA4(caData.value, tongKetData.value)
}

// 3. Tải file PDF biên bản đối soát
const xuatPdf = async () => {
  const el = document.querySelector('.content-body')
  if (!el) return
  const ma = caData.value?.maCa || tongKetData.value?.maCa || `Ca_${id}`
  try {
    await taoVaTaiPdf(el, `Bien_ban_doi_soat_${ma}.pdf`)
    notifySuccess('Xuất PDF thành công', `Đã tải về tệp Bien_ban_doi_soat_${ma}.pdf`)
  } catch (err) {
    notifyError('Xuất PDF thất bại', err.message || 'Không thể tạo file PDF')
  }
}

const xemHoaDon = (maHoaDon) => {
  if (maHoaDon) {
    router.push({ path: '/hoa-don', query: { ma: maHoaDon } })
  }
}

onMounted(loadData)
</script>

<template>
  <div class="doi-soat-container print-area">
    <!-- Header row -->
    <div class="top-nav-row no-print">
      <div class="breadcrumbs">
        <router-link to="/">Trang chủ</router-link> /
        <router-link to="/ca">Lịch sử giao ca</router-link> /
        <span class="active">Đối soát ca #{{ caData?.maCa || id }}</span>
      </div>
      <div class="action-buttons">
        <!-- Nút in phiếu nhiệt K80 (80mm) -->
        <button class="btn-print btn-print-k80" type="button" @click="inPhieuK80" title="In phiếu nhiệt 80mm cho máy in bill quầy">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="16" rx="2"></rect>
            <line x1="7" y1="8" x2="17" y2="8"></line>
            <line x1="7" y1="12" x2="17" y2="12"></line>
            <line x1="7" y1="16" x2="13" y2="16"></line>
          </svg>
          In phiếu nhiệt (K80)
        </button>

        <!-- Nút in biên bản A4 -->
        <button class="btn-print btn-print-a4" type="button" @click="inBienBanA4" title="In biên bản đối soát A4 đầy đủ có chữ ký">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="6 9 6 2 18 2 18 9"></polyline>
            <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
            <rect x="6" y="14" width="12" height="8"></rect>
          </svg>
          In biên bản A4
        </button>

        <!-- Nút tải PDF -->
        <button class="btn-print btn-pdf" type="button" @click="xuatPdf" title="Tải file PDF biên bản đối soát ca">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          Tải PDF
        </button>

        <router-link to="/ca" class="btn-back">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          Quay lại
        </router-link>
      </div>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="loading" class="loading-box">
      <div class="spinner"></div>
      <p>Đang tải dữ liệu đối soát ca...</p>
    </div>

    <div v-else-if="!caData && !tongKetData" class="not-found-box">
      <h3>Không tìm thấy dữ liệu ca</h3>
      <p>Ca làm việc không tồn tại hoặc đã bị xóa.</p>
      <router-link to="/ca" class="btn-back">Về danh sách ca</router-link>
    </div>

    <div v-else class="content-body">
      <!-- Title & Basic Info Header -->
      <div class="ca-header-card">
        <div class="header-main">
          <div class="ca-meta">
            <div class="ca-code-badge">MÃ CA: {{ caData?.maCa || tongKetData?.maCa || id }}</div>
            <span class="shift-status-pill" :class="statusClass">{{ statusText }}</span>
          </div>
          <h1 class="page-title">BIÊN BẢN ĐỐI SOÁT & TỔNG KẾT CA LÀM VIỆC</h1>
          <p class="store-name">Hệ thống giày PolyShoes • Quầy thu ngân</p>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <span class="info-k">Nhân viên trực ca:</span>
            <span class="info-v highlight-name">{{ caData?.tenNhanVien || tongKetData?.tenNhanVien || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-k">Thời gian mở ca:</span>
            <span class="info-v">{{ formatDateTime(caData?.thoiGianMo || tongKetData?.thoiGianMo) }}</span>
          </div>
          <div class="info-item">
            <span class="info-k">Thời gian chốt ca:</span>
            <span class="info-v">{{ caData?.thoiGianKet ? formatDateTime(caData.thoiGianKet) : 'Đang hoạt động' }}</span>
          </div>
          <div class="info-item">
            <span class="info-k">Quỹ tiền mặt:</span>
            <span class="info-v">{{ caData?.tenQuy || tongKetData?.tenQuy || 'Quỹ tiền mặt quầy chính' }}</span>
          </div>
        </div>
      </div>

      <!-- 4 KPI Metrics Cards -->
      <div class="kpi-grid">
        <div class="kpi-card card-opening">
          <div class="kpi-icon">💰</div>
          <div class="kpi-content">
            <span class="kpi-label">Tiền mặt ban đầu</span>
            <span class="kpi-value">{{ formatVnd(caData?.tienDauCa ?? tongKetData?.tienDauCa ?? 0) }}</span>
            <span class="kpi-sub">Số tiền lẻ bàn giao đầu ca</span>
          </div>
        </div>

        <div class="kpi-card card-cash">
          <div class="kpi-icon">💵</div>
          <div class="kpi-content">
            <span class="kpi-label">Tiền mặt thu tại quầy</span>
            <span class="kpi-value">{{ formatVnd(caData?.tienMatThu ?? tongKetData?.tienMatThu ?? 0) }}</span>
            <span class="kpi-sub">Doanh số tiền mặt trực tiếp</span>
          </div>
        </div>

        <div class="kpi-card card-transfer">
          <div class="kpi-icon">📲</div>
          <div class="kpi-content">
            <span class="kpi-label">Chuyển khoản / VietQR</span>
            <span class="kpi-value">{{ formatVnd(caData?.tienKhongMat ?? tongKetData?.tienKhongMat ?? 0) }}</span>
            <span class="kpi-sub">Tự động cộng tài khoản ngân hàng</span>
          </div>
        </div>

        <div class="kpi-card card-total">
          <div class="kpi-icon">📈</div>
          <div class="kpi-content">
            <span class="kpi-label">Tổng doanh thu ca</span>
            <span class="kpi-value text-primary">{{ formatVnd(caData?.tongDoanhThu ?? tongKetData?.tongDoanhThu ?? 0) }}</span>
            <span class="kpi-sub">{{ caData?.tongHoaDon ?? tongKetData?.tongHoaDon ?? 0 }} hóa đơn hoàn tất</span>
          </div>
        </div>
      </div>

      <!-- Đối soát Tiền mặt Trong két -->
      <div class="reconcile-card">
        <div class="card-head">
          <h2 class="card-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
            </svg>
            Đối soát Quỹ Tiền mặt trong Két
          </h2>
          <span class="head-sub">So sánh tiền hệ thống tính toán với tiền mặt thực tế kiểm kê</span>
        </div>

        <div class="reconcile-details-grid">
          <div class="calc-row">
            <span class="calc-label">Tiền đầu ca (+)</span>
            <span class="calc-val">{{ formatVnd(caData?.tienDauCa ?? tongKetData?.tienDauCa ?? 0) }}</span>
          </div>
          <div class="calc-row">
            <span class="calc-label">Doanh số tiền mặt POS (+)</span>
            <span class="calc-val">{{ formatVnd(caData?.tienMatThu ?? tongKetData?.tienMatThu ?? 0) }}</span>
          </div>
          <div class="calc-row">
            <span class="calc-label">Tiền hoàn trả khách (-)</span>
            <span class="calc-val">{{ formatVnd(caData?.tienHoanMat ?? tongKetData?.tienHoanMat ?? 0) }}</span>
          </div>
          <div class="calc-row calc-result-row">
            <span class="calc-label bold">Tiền dự kiến trong két:</span>
            <span class="calc-val bold text-indigo">{{ formatVnd(caData?.tienDuKien ?? tongKetData?.tienDuKien ?? 0) }}</span>
          </div>

          <div class="calc-row" v-if="caData?.thoiGianKet">
            <span class="calc-label bold">Tiền mặt thực tế kiểm kê:</span>
            <span class="calc-val bold">{{ formatVnd(caData?.tienThucTe ?? 0) }}</span>
          </div>

          <!-- Discrepancy Alert -->
          <div v-if="caData?.thoiGianKet" class="discrepancy-banner" :class="chenhLechNumber === 0 ? 'is-exact' : (chenhLechNumber > 0 ? 'is-surplus' : 'is-deficit')">
            <div class="banner-top">
              <span class="banner-tag">KẾT QUẢ ĐỐI SOÁT:</span>
              <strong v-if="chenhLechNumber === 0">Khớp 100% (Không phát sinh chênh lệch)</strong>
              <strong v-else-if="chenhLechNumber > 0">Thừa tiền mặt trong két (+{{ formatVnd(chenhLechNumber) }})</strong>
              <strong v-else>Thiếu tiền mặt trong két ({{ formatVnd(chenhLechNumber) }})</strong>
            </div>
            <div v-if="caData?.lyDoLech" class="banner-reason">
              <strong>Lý do giải trình:</strong> {{ caData.lyDoLech }}
            </div>
          </div>
          <div v-else class="discrepancy-banner is-open-info">
            <span class="banner-tag">CA ĐANG MỞ</span>
            <span>Số tiền thực tế và chênh lệch sẽ được xác nhận khi nhân viên thực hiện chốt ca.</span>
          </div>
        </div>
      </div>

      <!-- Danh sách hóa đơn gần đây trong ca -->
      <div class="invoices-card" v-if="tongKetData?.hoaDonGanDay && tongKetData.hoaDonGanDay.length > 0">
        <div class="card-head">
          <h2 class="card-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
            Danh sách Hóa đơn phát sinh trong Ca
          </h2>
          <span class="head-sub">Các giao dịch bán hàng đã hoàn thành trong ca này</span>
        </div>

        <div class="table-responsive">
          <table class="report-table">
            <thead>
              <tr>
                <th width="60">STT</th>
                <th>MÃ HÓA ĐƠN</th>
                <th>THỜI GIAN</th>
                <th>PHƯƠNG THỨC</th>
                <th class="text-end">TỔNG TIỀN</th>
                <th class="text-center">TRẠNG THÁI</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(hd, idx) in tongKetData.hoaDonGanDay" :key="hd.maHoaDon || idx">
                <td class="text-center">{{ idx + 1 }}</td>
                <td>
                  <span class="hd-code-link" @click="xemHoaDon(hd.maHoaDon)">{{ hd.maHoaDon }}</span>
                </td>
                <td>{{ formatDateTime(hd.ngayTao) }}</td>
                <td>
                  <span class="method-tag">{{ hd.tenPhuongThuc || 'Tiền mặt' }}</span>
                </td>
                <td class="text-end font-bold">{{ formatVnd(hd.tienSauGiamGia) }}</td>
                <td class="text-center">
                  <span class="hd-status-pill">{{ hd.trangThai }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Signature block for Printing -->
      <div class="signature-section only-print">
        <div class="sign-box">
          <p class="sign-title">Nhân viên giao ca</p>
          <p class="sign-hint">(Ký và ghi rõ họ tên)</p>
          <div class="sign-space"></div>
          <p class="sign-name">{{ caData?.tenNhanVien || tongKetData?.tenNhanVien || '................................' }}</p>
        </div>
        <div class="sign-box">
          <p class="sign-title">Quản lý / Người nhận bàn giao</p>
          <p class="sign-hint">(Ký và ghi rõ họ tên)</p>
          <div class="sign-space"></div>
          <p class="sign-name">....................................................</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.doi-soat-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 48px;
  font-family: inherit;
  color: #0f172a;
}

.top-nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.breadcrumbs {
  font-size: 13px;
  color: #64748b;
}

.breadcrumbs a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.breadcrumbs a:hover {
  text-decoration: underline;
}

.breadcrumbs .active {
  color: #0f172a;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.btn-print, .btn-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-print {
  background: #2563eb;
  color: #fff;
  border: 1px solid #1d4ed8;
}

.btn-print:hover {
  background: #1d4ed8;
}

.btn-print-k80 {
  background: #059669;
  border-color: #047857;
  color: #ffffff;
}

.btn-print-k80:hover {
  background: #047857;
}

.btn-print-a4 {
  background: #2563eb;
  border-color: #1d4ed8;
  color: #ffffff;
}

.btn-print-a4:hover {
  background: #1d4ed8;
}

.btn-pdf {
  background: #dc2626;
  border-color: #b91c1c;
  color: #ffffff;
}

.btn-pdf:hover {
  background: #b91c1c;
}

.btn-back {
  background: #ffffff;
  color: #475569;
  border: 1.5px solid #cbd5e1;
}

.btn-back:hover {
  background: #f8fafc;
  color: #0f172a;
}

/* Header Card */
.ca-header-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.header-main {
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 16px;
  margin-bottom: 16px;
}

.ca-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.ca-code-badge {
  background: #f1f5f9;
  color: #334155;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.shift-status-pill {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 700;
}

.badge-open {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.badge-closed {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 4px;
}

.store-name {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-k {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.info-v {
  font-size: 14px;
  color: #0f172a;
  font-weight: 600;
}

.highlight-name {
  color: #1e40af;
  font-weight: 700;
}

/* KPI Cards */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.kpi-icon {
  font-size: 26px;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

.kpi-value {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin: 2px 0;
}

.kpi-sub {
  font-size: 11px;
  color: #94a3b8;
}

.text-primary {
  color: #2563eb;
}

/* Reconcile Box */
.reconcile-card, .invoices-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.card-head {
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  display: block;
}

.reconcile-details-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.calc-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #f8fafc;
}

.calc-result-row {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  font-size: 15px;
}

.bold {
  font-weight: 700;
}

.text-indigo {
  color: #1e40af;
}

.discrepancy-banner {
  padding: 16px;
  border-radius: 8px;
  margin-top: 8px;
}

.is-exact {
  background: #ecfdf5;
  border: 1.5px solid #10b981;
  color: #065f46;
}

.is-surplus {
  background: #eff6ff;
  border: 1.5px solid #3b82f6;
  color: #1e40af;
}

.is-deficit {
  background: #fef2f2;
  border: 1.5px solid #ef4444;
  color: #991b1b;
}

.is-open-info {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.banner-top {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.banner-tag {
  font-size: 11px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0,0,0,0.06);
}

.banner-reason {
  margin-top: 8px;
  font-size: 13px;
  padding-top: 8px;
  border-top: 1px dashed rgba(0,0,0,0.12);
}

/* Table */
.report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.report-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  padding: 10px 12px;
  border-bottom: 1.5px solid #e2e8f0;
  text-align: left;
}

.report-table td {
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #1e293b;
}

.hd-code-link {
  font-weight: 700;
  color: #2563eb;
  cursor: pointer;
}

.hd-code-link:hover {
  text-decoration: underline;
}

.method-tag {
  background: #f1f5f9;
  color: #475569;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.hd-status-pill {
  background: #ecfdf5;
  color: #047857;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
}

.only-print {
  display: none;
}

/* Print Styles */
@media print {
  .no-print {
    display: none !important;
  }
  .only-print {
    display: flex !important;
    justify-content: space-around;
    margin-top: 48px;
    padding-top: 24px;
  }
  .sign-box {
    text-align: center;
  }
  .sign-space {
    height: 70px;
  }
  .sign-title {
    font-weight: 700;
    margin-bottom: 4px;
  }
  .sign-hint {
    font-size: 11px;
    font-style: italic;
    color: #64748b;
  }
  .sign-name {
    font-weight: 700;
  }
  body {
    background: #fff;
  }
}
</style>
