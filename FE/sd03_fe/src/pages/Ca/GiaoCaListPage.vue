<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUser } from '@/utils/auth'
import { laAdmin } from '@/utils/session'
import { formatVnd, formatDateTime } from '@/utils/format'
import { getDangMo, getLichSu, moCa } from '@/api/giaoCa'
import PaginationBar from '@/components/PaginationBar.vue'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { notifyError, notifySuccess } from '@/utils/notify'
import http from '@/api/http'

const route = useRoute()
const router = useRouter()

const list = ref([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(5)
const totalPages = ref(0)
const totalElements = ref(0)

const employees = ref([])
const isLoadingEmployees = ref(false)

const isAdmin = computed(() => laAdmin())
const currentUser = computed(() => getUser())
const currentStaffId = computed(() => currentUser.value?.idNhanVien)
const caTruoc = computed(() => list.value.find((row) => row.trangThai === 'DA_KET_CA'))
const caHienTai = ref(null)
const moCaForm = reactive({ tienDauCa: 300000, ghiChu: '' })
const moCaLoading = ref(false)
const moCaError = ref('')

const emptyFilter = () => ({
  tuNgay: '',
  denNgay: '',
  trangThai: '',
  idNhanVien: ''
})

const filter = reactive(emptyFilter())
const applied = reactive(emptyFilter())

const trangThaiOptions = [
  { value: '', label: 'Tất cả' },
  { value: 'DANG_MO', label: 'Đang mở' },
  { value: 'DA_KET_CA', label: 'Đã kết' }
]

const employeeOptions = computed(() => [
  { value: '', label: 'Tất cả nhân viên' },
  ...employees.value.map(e => ({ value: e.id, label: `${e.maNhanVien} - ${e.tenTaiKhoan}` }))
])

const buildParams = () => {
  const params = { page: page.value, size: pageSize.value }
  if (applied.tuNgay) params.tuNgay = applied.tuNgay
  if (applied.denNgay) params.denNgay = applied.denNgay
  if (applied.trangThai) params.trangThai = applied.trangThai
  if (applied.idNhanVien) params.idNhanVien = applied.idNhanVien
  return params
}

const loadEmployees = async () => {
  if (!isAdmin.value) return
  isLoadingEmployees.value = true
  try {
    const res = await http.get('/nhan-vien', { params: { size: 1000 } })
    if (res.data?.content) {
      employees.value = res.data.content
    } else if (Array.isArray(res.data)) {
      employees.value = res.data
    }
  } catch (err) {
    console.error('Lỗi tải danh sách nhân viên:', err)
  } finally {
    isLoadingEmployees.value = false
  }
}

const loadData = async (p = 0) => {
  loading.value = true
  try {
    const params = buildParams()
    const res = await getLichSu(params)
    list.value = res.content || []
    page.value = res.number || 0
    totalPages.value = res.totalPages || 0
    totalElements.value = res.totalElements || 0
  } catch (err) {
    notifyError('Lỗi tải dữ liệu', err.message)
  } finally {
    loading.value = false
  }
}

const loadCaHienTai = async () => {
  try {
    caHienTai.value = await getDangMo()
  } catch (err) {
    notifyError('Lỗi tải ca làm việc', err.message)
  }
}

const batDauCa = async () => {
  moCaError.value = ''
  const tienDauCa = Number(moCaForm.tienDauCa)
  if (!Number.isFinite(tienDauCa) || tienDauCa < 0) {
    moCaError.value = 'Tiền đầu ca phải là số không âm'
    return
  }
  moCaLoading.value = true
  try {
    caHienTai.value = await moCa({ tienDauCa, ghiChu: moCaForm.ghiChu.trim() || null })
    notifySuccess('Mở ca thành công', `Ca ${caHienTai.value.maCa} đã được mở`)
    await loadData(0)
  } catch (err) {
    moCaError.value = err.message || 'Không thể mở ca làm việc'
  } finally {
    moCaLoading.value = false
  }
}

const applyFilter = () => {
  Object.assign(applied, filter)
  page.value = 0
  loadData(0)
}

const resetFilter = () => {
  Object.assign(filter, emptyFilter())
  Object.assign(applied, emptyFilter())
  page.value = 0
  loadData(0)
}

const onSizeChange = (s) => {
  pageSize.value = s
  page.value = 0
  loadData(0)
}

const goPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  loadData(p)
}

const navigateToDoiSoat = (id) => {
  router.push(`/ca/${id}/doi-soat`)
}

const getTrangThaiLabel = (trangThai) => {
  if (trangThai === 'DANG_MO') return 'Đang mở'
  if (trangThai === 'DA_KET_CA') return 'Đã kết'
  return trangThai
}

const trangThaiClass = (trangThai) => {
  if (trangThai === 'DANG_MO') return 'status-yellow'
  if (trangThai === 'DA_KET_CA') return 'status-green'
  return 'status-gray'
}

onMounted(async () => {
  if (isAdmin.value) {
    await loadEmployees()
  } else if (currentStaffId.value) {
    filter.idNhanVien = currentStaffId.value
    applied.idNhanVien = currentStaffId.value
  }
  await Promise.all([loadData(0), loadCaHienTai()])
})

watch(() => route.fullPath, () => {
  loadData(page.value)
})
</script>

<template>
<div class="giao-ca-page">
  <section class="panel shift-panel">
    <div class="shift-heading">
      <span class="status-badge" :class="caHienTai ? 'status-yellow' : 'status-gray'">
        {{ caHienTai ? 'ĐANG TRONG CA' : 'CHƯA VÀO CA LÀM VIỆC' }}
      </span>
      <h1 class="screen-title">Mở ca làm việc & Kiểm đếm tiền ban đầu</h1>
    </div>

    <div v-if="caTruoc" class="previous-shift">
      <i class="bi bi-info-circle" aria-hidden="true"></i>
      <div>
        <strong>Ca trước: {{ caTruoc.maCa }}</strong>
        <span>{{ caTruoc.tenNhanVien || caTruoc.maNhanVien }} · {{ formatDateTime(caTruoc.thoiGianKet || caTruoc.thoiGianMo) }}</span>
        <span>Tiền cuối ca: {{ formatVnd(caTruoc.tienThucTe ?? caTruoc.tienDauCa) }} · Chênh lệch: {{ formatVnd(caTruoc.chenhLech) }}</span>
      </div>
    </div>

    <div v-if="caHienTai" class="open-shift-info">
      <span>Ca {{ caHienTai.maCa }} đang mở từ {{ formatDateTime(caHienTai.thoiGianMo) }}.</span>
      <button type="button" class="btn-shift-action" @click="router.push('/ban-hang')">Đi tới bán hàng</button>
    </div>
    <form v-else class="open-shift-form" @submit.prevent="batDauCa">
      <div class="open-shift-fields">
        <div class="f-item">
          <label for="open-cash">Tiền đầu ca <b>*</b></label>
          <input id="open-cash" v-model.number="moCaForm.tienDauCa" class="f-control" type="number" min="0" step="1000" required placeholder="Nhập tiền đầu ca" />
        </div>
        <div class="f-item">
          <label for="open-note">Ghi chú khi mở ca</label>
          <input id="open-note" v-model="moCaForm.ghiChu" class="f-control" type="text" maxlength="500" placeholder="Ghi chú mở ca (nếu có)..." />
        </div>
        <div class="f-item staff-field">
          <label>Nhân viên trực quầy <b>*</b></label>
          <div class="staff-value"><i class="bi bi-person-circle" aria-hidden="true"></i>{{ currentUser?.tenKhachHang || currentUser?.tenTaiKhoan || 'Tài khoản đang đăng nhập' }}</div>
        </div>
      </div>
      <p v-if="moCaError" class="shift-error">{{ moCaError }}</p>
      <div class="shift-form-actions">
        <button type="submit" class="btn-shift-action" :disabled="moCaLoading">
          {{ moCaLoading ? 'Đang mở ca...' : 'Bắt đầu ca làm việc ngay' }}
        </button>
      </div>
    </form>
  </section>

  <details class="panel filter-details">
    <summary class="filter-summary">Tìm kiếm & bộ lọc</summary>
    <form class="filter-row" @submit.prevent="applyFilter">
      <div class="f-item">
        <label for="f-from">Từ ngày</label>
        <input
          id="f-from"
          v-model="filter.tuNgay"
          type="date"
          class="f-control"
          :max="filter.denNgay || undefined"
        />
      </div>

      <div class="f-item">
        <label for="f-to">Đến ngày</label>
        <input
          id="f-to"
          v-model="filter.denNgay"
          type="date"
          class="f-control"
          :min="filter.tuNgay || undefined"
        />
      </div>

      <div class="f-item">
        <label>Trạng thái</label>
        <SearchableSelect
          v-model="filter.trangThai"
          :options="trangThaiOptions"
          value-key="value"
          label-key="label"
          placeholder="Tất cả"
          search-placeholder="Tìm trạng thái..."
          @change="(v) => { filter.trangThai = v === null || v === undefined ? '' : v }"
        />
      </div>

      <div class="f-item" v-if="isAdmin">
        <label>Nhân viên</label>
        <SearchableSelect
          v-model="filter.idNhanVien"
          :options="employeeOptions"
          value-key="value"
          label-key="label"
          placeholder="Tất cả nhân viên"
          search-placeholder="Tìm nhân viên..."
          :disabled="isLoadingEmployees"
          @change="(v) => { filter.idNhanVien = v === null || v === undefined ? '' : v }"
        />
      </div>

      <div class="f-actions">
        <button
          type="button"
          class="btn-icon-reset"
          aria-label="Đặt lại bộ lọc"
          title="Đặt lại bộ lọc"
          @click="resetFilter"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 12a8 8 0 1 1-2.6-5.9" />
            <path d="M20 4v5h-5" />
          </svg>
        </button>
        <button type="submit" class="btn-apply">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 7h16M7 12h10M10 17h4" />
          </svg>
          Áp dụng
        </button>
      </div>
    </form>
  </details>

  <section class="panel">
    <div class="panel-head">
      <span class="panel-icon is-blue">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      </span>
      <h3 class="panel-title">Các lượt giao ca gần nhất</h3>
      <span class="count-pill">{{ totalElements }} ca</span>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead><tr><th>Mã ca</th><th>Người giao</th><th>Quầy</th><th>Thời gian mở</th><th>Thời gian đóng</th><th style="text-align:right">Tiền cuối ca</th><th style="text-align:right">Chênh lệch</th><th>Trạng thái</th><th style="text-align:center">Biên bản</th></tr></thead>
        <tbody>
          <tr v-if="loading"><td colspan="9" class="text-center py-4">Đang tải dữ liệu...</td></tr>
          <tr v-else-if="list.length === 0"><td colspan="9" class="text-center py-4">Chưa có ca làm việc nào</td></tr>
          <tr v-for="(row, idx) in list" v-else :key="row.id || idx">
            <td class="cell-code">{{ row.maCa }}</td>
            <td>{{ row.tenNhanVien || row.maNhanVien || '—' }}</td>
            <td>{{ row.tenQuay || row.maQuay || row.tenQuy || '—' }}</td>
            <td>{{ formatDateTime(row.thoiGianMo) }}</td>
            <td>{{ row.thoiGianKet ? formatDateTime(row.thoiGianKet) : '—' }}</td>
            <td style="text-align:right">{{ formatVnd(row.tienThucTe ?? row.tienDauCa) }}</td>
            <td style="text-align:right" :class="{ 'text-red': Number(row.chenhLech || 0) !== 0 }">{{ formatVnd(row.chenhLech) }}</td>
            <td><span class="status-badge" :class="trangThaiClass(row.trangThai)">{{ getTrangThaiLabel(row.trangThai) }}</span></td>
            <td style="text-align:center"><button class="btn-action-circle is-view" type="button" title="Xem biên bản giao ca" @click="navigateToDoiSoat(row.id)" :disabled="!row.id"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg></button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <PaginationBar
      v-if="totalElements > 0"
      :page="page"
      :total-pages="totalPages"
      :total-elements="totalElements"
      :page-size="pageSize"
      :from="list.length ? page * pageSize + 1 : 0"
      :to="page * pageSize + list.length"
      label="ca"
      @change="goPage"
      @size-change="onSizeChange"
    />
  </section>
</div>
</template>

<style scoped>
.giao-ca-page {
  padding: 16px 20px 40px;
  background-color: #f8fafc;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shift-panel {
  padding: 18px 20px;
}

.shift-heading {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

.shift-heading .screen-title {
  font-size: 18px;
}

.previous-shift,
.open-shift-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
}

.previous-shift > i { color: #475569; font-size: 16px; }
.previous-shift div { display: flex; flex-direction: column; gap: 3px; }
.previous-shift strong { color: #1e293b; }
.previous-shift span { line-height: 1.4; }
.open-shift-info { justify-content: space-between; margin: 0; }

.open-shift-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
  align-items: end;
}

.open-shift-fields .f-item label { text-transform: none; }
.open-shift-fields .f-item label b { color: #dc2626; }
.staff-field { grid-column: 1 / -1; }

.staff-value {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
}

.shift-error { margin: 10px 0 0; color: #dc2626; font-size: 12px; }
.shift-form-actions { display: flex; justify-content: flex-end; margin-top: 14px; }

.btn-shift-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 16px;
  border: 0;
  border-radius: 8px;
  background: #dc2626;
  color: #fff;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-shift-action:hover:not(:disabled) { background: #b91c1c; }
.btn-shift-action:disabled { opacity: .6; cursor: not-allowed; }
.filter-details { padding: 0 18px 14px; }
.filter-summary { padding: 13px 0 0; color: #475569; font-size: 13px; font-weight: 600; cursor: pointer; }
.filter-details .filter-row { margin-top: 14px; }
.filter-row > .f-actions { grid-column: 1 / -1; }

.screen-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

/* Panel */
.panel {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef2f6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  overflow: hidden;
}

.filter-panel {
  padding: 20px 24px;
}

.filter-head {
  margin-bottom: 16px;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1e293b;
}

.filter-title svg {
  width: 20px;
  height: 20px;
  color: #475569;
}

.filter-title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 16px 20px;
  align-items: end;
}

.filter-row > .f-item {
  grid-column: span 3;
  min-width: 0;
}

.filter-row > .f-item:last-child {
  grid-column: span 12;
}

.f-item label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.f-control {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  font: inherit;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.f-control:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.f-control::placeholder {
  color: #94a3b8;
}

.f-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.btn-icon-reset {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  transition: background-color 0.15s, border-color 0.15s;
}

.btn-icon-reset svg {
  width: 18px;
  height: 18px;
}

.btn-icon-reset:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-apply {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 20px;
  border: 0;
  border-radius: 8px;
  background: #dc2626;
  color: #fff;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-apply svg {
  width: 16px;
  height: 16px;
}

.btn-apply:hover {
  background: #b91c1c;
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.panel-icon.is-blue {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.panel-icon svg {
  width: 18px;
  height: 18px;
}

.panel-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.count-pill {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: 600;
}

.table-wrap {
  overflow-x: auto;
  width: 100%;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  padding: 12px 10px;
  font-size: 11.5px;
  font-weight: 700;
  text-transform: uppercase;
  color: #475569;
  border-bottom: 1px solid #eef2f6;
  background: #fafbfc;
  white-space: nowrap;
  text-align: left;
}

.data-table td {
  padding: 12px 10px;
  color: #334155;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background: #fafbfc;
}

.cell-code {
  font-weight: 600;
  color: #dc2626;
  font-family: monospace;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.status-badge.status-yellow {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
}

.status-badge.status-green {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.status-badge.status-gray {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.text-red {
  color: #dc2626;
  font-weight: 600;
}

.summary-row {
  background: #f8fafc !important;
  font-weight: 600;
}

.summary-row td {
  border-top: 2px solid #e2e8f0;
}

/* Action buttons */
.btn-action-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #475569;
}

.btn-action-circle svg {
  width: 16px;
  height: 16px;
}

.btn-action-circle.is-view:hover:not(:disabled) {
  background: #e2e8f0;
  color: #1e293b;
  transform: scale(1.08);
}

.btn-action-circle:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 1100px) {
  .filter-row > .f-item {
    grid-column: span 6;
  }
  .filter-row > .f-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .open-shift-fields { grid-template-columns: 1fr; }
  .staff-field { grid-column: auto; }
  .shift-panel { padding: 16px; }
  .open-shift-info { align-items: flex-start; flex-direction: column; }
  .filter-row > .f-item {
    grid-column: 1 / -1;
  }
}
</style>
