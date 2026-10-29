<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDangMo, getLichSu } from '@/api/giaoCa'
import { listCaLam } from '@/api/schedule'
import { listEmployees } from '@/api/employee'
import { formatVnd } from '@/utils/format'
import { notifyError } from '@/utils/notify'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()
const pad2 = (x) => String(x).padStart(2, '0')

/* ---------------------------------- Kiểm tra trạng thái vào ca ---------------------------------- */
const caHienTai = ref(null)

const kiemTraCaHienTai = async () => {
  try {
    caHienTai.value = await getDangMo()
  } catch {
    caHienTai.value = null
  }
}

const diToiGiaoCa = () => {
  router.push('/ca')
}

/* ---------------------------------- Danh sách ca làm việc & nhân viên ---------------------------------- */
const cas = ref([])
const nhanViens = ref([])

const taiDanhSachPhu = async () => {
  try {
    const [caRes, nvRes] = await Promise.all([
      listCaLam().catch(() => []),
      listEmployees({ page: 0, size: 1000 }).catch(() => ({ content: [] })),
    ])
    cas.value = caRes || []
    nhanViens.value = nvRes.content || []
  } catch {
    cas.value = []
    nhanViens.value = []
  }
}

const timTenNhanVien = (row) => {
  if (row.tenNhanVien && row.tenNhanVien !== row.maNhanVien) {
    return row.tenNhanVien
  }
  const nv = nhanViens.value.find((e) => e.id === row.idNhanVien || e.maNhanVien === row.maNhanVien)
  return nv ? nv.tenTaiKhoan || nv.ten || row.tenNhanVien || '—' : row.tenNhanVien || '—'
}

const timMaNhanVien = (row) => {
  if (row.maNhanVien) return row.maNhanVien
  const nv = nhanViens.value.find((e) => e.id === row.idNhanVien)
  return nv ? nv.maNhanVien : `NV${row.idNhanVien || ''}`
}

const getAvatarInitial = (name) => {
  if (!name) return '?'
  const clean = String(name).trim()
  if (!clean) return '?'
  const parts = clean.split(/\s+/)
  const last = parts[parts.length - 1]
  return (last[0] || '?').toUpperCase()
}

const timTenCa = (row) => {
  if (row.maCa && !row.maCa.startsWith('GC-')) {
    return row.maCa
  }
  if (!row.thoiGianMo) return row.maCa || 'Ca làm'
  const timeStr = String(row.thoiGianMo).slice(11, 16)
  if (timeStr && cas.value.length > 0) {
    const found = cas.value.find((c) => {
      if (!c.batDau || !c.ketThuc) return false
      return timeStr >= c.batDau && timeStr <= c.ketThuc
    })
    if (found) return found.ten
  }
  return row.maCa || 'Ca làm'
}

/* ---------------------------------- Định dạng ngày giờ ---------------------------------- */
const formatThoiGian = (dt) => {
  if (!dt) return '—'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return String(dt)
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())} ${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}`
}

/* ---------------------------------- Bộ lọc & Danh sách ---------------------------------- */
const fKeyword = ref('')
const fTuNgay = ref('')
const fDenNgay = ref('')
const fTrangThai = ref('')

const list = ref([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)

let searchTimeout = null
const onKeywordInput = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    page.value = 0
    taiLichSu()
  }, 350)
}

const taiLichSu = async (p = page.value) => {
  loading.value = true
  try {
    const params = {
      page: p,
      size: pageSize.value,
    }
    if (fKeyword.value.trim()) params.keyword = fKeyword.value.trim()
    if (fTuNgay.value) params.tuNgay = fTuNgay.value
    if (fDenNgay.value) params.denNgay = fDenNgay.value
    if (fTrangThai.value) params.trangThai = fTrangThai.value

    const res = await getLichSu(params)
    list.value = res.content || []
    page.value = res.number || 0
    totalPages.value = res.totalPages || 0
    totalElements.value = res.totalElements || 0
  } catch (err) {
    list.value = []
    notifyError('Lỗi tải dữ liệu lịch sử', err.message || '')
  } finally {
    loading.value = false
  }
}

const taiLai = () => {
  fKeyword.value = ''
  fTuNgay.value = ''
  fDenNgay.value = ''
  fTrangThai.value = ''
  page.value = 0
  taiLichSu(0)
}

const datTrang = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  taiLichSu(p)
}

const doiSize = (s) => {
  pageSize.value = s
  page.value = 0
  taiLichSu(0)
}

const xemDoiSoat = (row) => {
  router.push(`/ca/${row.id}/doi-soat`)
}

watch([fTuNgay, fDenNgay, fTrangThai], () => {
  page.value = 0
  taiLichSu(0)
})

onMounted(async () => {
  await Promise.all([
    kiemTraCaHienTai(),
    taiDanhSachPhu(),
    taiLichSu(0),
  ])
})
</script>

<template>
  <div class="activity-history-page">
    <!-- 1. THÔNG BÁO CHẾ ĐỘ XEM / VÀO CA LÀM VIỆC -->
    <div v-if="!caHienTai" class="shift-alert-banner">
      <div class="shift-alert-left">
        <span class="badge-mode">CHẾ ĐỘ XEM</span>
        <span class="shift-alert-text">
          Bạn chưa vào ca làm việc nên chỉ có quyền tra cứu thông tin. Các thao tác bán hàng, tạo đơn hàng và chỉnh sửa dữ liệu đang bị khóa.
        </span>
      </div>
      <button type="button" class="btn-enter-shift" @click="diToiGiaoCa">
        Vào ca làm việc ngay
      </button>
    </div>

    <!-- 2. BREADCRUMBS -->
    <div class="schedule-breadcrumbs">
      <span class="crumb-parent">Lịch làm việc</span>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Lịch sử hoạt động</span>
    </div>

    <!-- 3. HỘP TÌM KIẾM VÀ BỘ LỌC -->
    <div class="filter-card">
      <div class="filter-grid">
        <div class="filter-col col-search">
          <label class="filter-label">TÌM KIẾM</label>
          <div class="search-input-box">
            <input
              v-model="fKeyword"
              type="text"
              class="form-input"
              placeholder="Tìm theo tài khoản / mã ca / mã hóa đơn..."
              @input="onKeywordInput"
            />
          </div>
        </div>

        <div class="filter-col col-date">
          <label class="filter-label">TỪ NGÀY:</label>
          <input
            v-model="fTuNgay"
            type="date"
            class="form-input"
          />
        </div>

        <div class="filter-col col-date">
          <label class="filter-label">ĐẾN NGÀY:</label>
          <input
            v-model="fDenNgay"
            type="date"
            class="form-input"
          />
        </div>

        <div class="filter-col col-status">
          <label class="filter-label">TRẠNG THÁI</label>
          <select v-model="fTrangThai" class="form-select">
            <option value="">Tất cả</option>
            <option value="DANG_MO">Đang mở</option>
            <option value="DA_KET_CA">Đã đóng</option>
          </select>
        </div>

        <div class="filter-col col-action">
          <label class="filter-label">&nbsp;</label>
          <button type="button" class="btn-reload" title="Tải lại bộ lọc" @click="taiLai">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
            </svg>
            Tải lại
          </button>
        </div>
      </div>
    </div>

    <!-- 4. BẢNG DANH SÁCH LỊCH SỬ HOẠT ĐỘNG GIAO CA -->
    <div class="table-card">
      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-num">#</th>
              <th>NGƯỜI GIAO</th>
              <th>NGƯỜI NHẬN</th>
              <th class="text-center">CA</th>
              <th class="text-center">MỞ</th>
              <th class="text-center">ĐÓNG</th>
              <th class="text-right">TIỀN MẶT</th>
              <th class="text-center">CHÊNH LỆCH</th>
              <th class="text-center">TRẠNG THÁI</th>
              <th class="text-center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="10" class="empty-cell">Đang tải lịch sử hoạt động...</td>
            </tr>
            <tr v-else-if="list.length === 0">
              <td colspan="10" class="empty-cell">Không có lượt giao ca nào phù hợp</td>
            </tr>
            <tr v-for="(row, idx) in list" :key="row.id" class="table-row">
              <!-- # STT -->
              <td class="col-num text-center">
                {{ page * pageSize + idx + 1 }}
              </td>

              <!-- Người giao -->
              <td>
                <div class="user-cell">
                  <div class="user-avatar">
                    {{ getAvatarInitial(timTenNhanVien(row)) }}
                  </div>
                  <div class="user-info">
                    <span class="user-code">{{ timMaNhanVien(row) }}</span>
                    <span class="user-name">{{ timTenNhanVien(row) }}</span>
                  </div>
                </div>
              </td>

              <!-- Người nhận -->
              <td>
                <span v-if="row.tenNguoiNhan" class="receiver-name">{{ row.tenNguoiNhan }}</span>
                <span v-else class="receiver-empty">Chưa có / Cuối ngày</span>
              </td>

              <!-- Ca -->
              <td class="text-center">
                <span class="shift-chip">{{ timTenCa(row) }}</span>
              </td>

              <!-- Mở -->
              <td class="text-center text-muted">
                {{ formatThoiGian(row.thoiGianMo) }}
              </td>

              <!-- Đóng -->
              <td class="text-center text-muted">
                {{ formatThoiGian(row.thoiGianKet) }}
              </td>

              <!-- Tiền mặt -->
              <td class="text-right">
                <div class="cash-info">
                  <span class="cash-start">Đầu: {{ formatVnd(row.tienDauCa || 0) }}</span>
                  <span class="cash-end">Cuối: <strong>{{ formatVnd(row.tienThucTe ?? row.tienDauCa ?? 0) }}</strong></span>
                </div>
              </td>

              <!-- Chênh lệch -->
              <td class="text-center">
                <span
                  class="diff-badge"
                  :class="{
                    'diff-zero': !row.chenhLech || Number(row.chenhLech) === 0,
                    'diff-neg': Number(row.chenhLech) < 0,
                    'diff-pos': Number(row.chenhLech) > 0,
                  }"
                >
                  {{ (Number(row.chenhLech) > 0 ? '+' : '') + formatVnd(row.chenhLech || 0) }}
                </span>
              </td>

              <!-- Trạng thái -->
              <td class="text-center">
                <span
                  class="status-pill"
                  :class="row.trangThai === 'DA_KET_CA' ? 'status-closed' : 'status-open'"
                >
                  {{ row.trangThai === 'DA_KET_CA' ? 'Đã đóng' : 'Đang mở' }}
                </span>
              </td>

              <!-- Hành động -->
              <td class="text-center">
                <button
                  type="button"
                  class="btn-action-doc"
                  title="Xem biên bản đối soát"
                  @click="xemDoiSoat(row)"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- PHÂN TRANG -->
      <div v-if="totalPages > 1" class="table-card-foot">
        <PaginationBar
          :page="page"
          :total-pages="totalPages"
          :total-elements="totalElements"
          :page-size="pageSize"
          :from="list.length ? page * pageSize + 1 : 0"
          :to="page * pageSize + list.length"
          :sizes="[10, 20, 50]"
          label="lượt giao ca"
          @change="datTrang"
          @size-change="doiSize"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.activity-history-page {
  padding: 16px 24px 40px;
  background-color: #fafbfd;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  font-family: inherit;
}

/* 1. Shift Notice Banner */
.shift-alert-banner {
  background: #fefce8;
  border: 1px solid #fef08a;
  border-radius: 8px;
  padding: 10px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.shift-alert-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.badge-mode {
  background: #fef08a;
  color: #a16207;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #fde047;
  white-space: nowrap;
}

.shift-alert-text {
  font-size: 13px;
  color: #713f12;
  font-weight: 400;
  line-height: 1.4;
}

.btn-enter-shift {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-enter-shift:hover {
  background: linear-gradient(135deg, #b91c1c 0%, #991b1b 100%);
  box-shadow: 0 6px 16px rgba(220, 38, 38, 0.35);
  transform: translateY(-1px);
}

/* 2. Breadcrumbs */
.schedule-breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  padding: 2px 0;
}

.crumb-parent {
  color: #64748b;
}

.crumb-separator {
  color: #cbd5e1;
}

.crumb-current {
  color: #0f172a;
  font-weight: 700;
}

/* 3. Filter Card */
.filter-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  padding: 18px 22px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
}

.filter-grid {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}

.filter-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.col-search {
  flex: 1;
  min-width: 250px;
}

.col-date {
  width: 170px;
}

.col-status {
  width: 160px;
}

.col-action {
  margin-left: auto;
}

.filter-label {
  font-size: 11.5px;
  font-weight: 700;
  color: #475569;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.form-input,
.form-select {
  height: 38px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.15s ease;
  width: 100%;
  font-family: inherit;
}

.form-input:focus,
.form-select:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.form-input::placeholder {
  color: #94a3b8;
}

.btn-reload {
  height: 38px;
  padding: 0 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-reload:hover {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
  transform: translateY(-1px);
}

/* 4. Table Card */
.table-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead th {
  background: #f8fafc;
  color: #475569;
  font-size: 11.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 13px 16px;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
  text-align: left;
}

.data-table thead th.col-num {
  width: 48px;
  text-align: center;
}

.data-table thead th.text-center {
  text-align: center;
}

.data-table thead th.text-right {
  text-align: right;
}

.data-table tbody td {
  padding: 13px 16px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  color: #334155;
}

.table-row:hover td {
  background: #f8fafc;
}

.col-num {
  color: #64748b;
  font-weight: 600;
}

/* User cell */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1e293b;
  color: #ffffff;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.user-code {
  font-weight: 700;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.25;
}

.user-name {
  font-size: 11.5px;
  color: #64748b;
  line-height: 1.2;
  margin-top: 2px;
}

.receiver-name {
  color: #334155;
  font-weight: 500;
}

.receiver-empty {
  color: #94a3b8;
  font-style: italic;
  font-size: 12.5px;
}

/* Shift chip */
.shift-chip {
  display: inline-block;
  background: #f1f5f9;
  color: #1e293b;
  padding: 3px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.text-muted {
  color: #475569;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
}

/* Cash info */
.cash-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.cash-start {
  font-size: 11.5px;
  color: #64748b;
}

.cash-end {
  font-size: 12.5px;
  color: #0f172a;
}

.diff-badge {
  font-weight: 600;
  font-size: 12.5px;
}

.diff-zero {
  color: #16a34a;
}

.diff-neg {
  color: #dc2626;
}

.diff-pos {
  color: #2563eb;
}

/* Status pill */
.status-pill {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-closed {
  background: #f0fdf4;
  color: #059669;
  border: 1px solid #bbf7d0;
}

.status-open {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecdd3;
}

/* Action button */
.btn-action-doc {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.btn-action-doc:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #2563eb;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.15);
  transform: translateY(-1px);
}

.empty-cell {
  text-align: center;
  padding: 36px;
  color: #94a3b8;
  font-size: 13.5px;
}
</style>
