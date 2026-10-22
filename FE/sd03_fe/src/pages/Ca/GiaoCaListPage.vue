<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUser } from '@/utils/auth'
import { laAdmin } from '@/utils/session'
import { formatVnd, formatDateTime, formatDate } from '@/utils/format'
import { getLichSu } from '@/api/giaoCa'
import PaginationBar from '@/components/PaginationBar.vue'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { notifyError, notifySuccess } from '@/utils/notify'
import http from '@/api/http'

const route = useRoute()
const router = useRouter()

const list = ref([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)

const employees = ref([])
const isLoadingEmployees = ref(false)

const isAdmin = computed(() => laAdmin())
const currentUser = computed(() => getUser())
const currentStaffId = computed(() => currentUser.value?.idNhanVien)

const emptyFilter = () => ({
  tuNgay: '',
  denNgay: '',
  trangThai: '',
  idNhanVien: ''
})

const filter = reactive(emptyFilter())
const applied = reactive(emptyFilter())

const hasFilter = computed(() => Object.values(applied).some(v => v))

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

const exportExcel = () => {
  console.log('Xuất Excel với params:', buildParams())
  notifySuccess('Placeholder', 'Chức năng xuất Excel đang phát triển')
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

const formatDateOnly = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()}`
}

const summary = computed(() => {
  return list.value.reduce((acc, row) => {
    acc.doanhThu += Number(row.tongDoanhThu || 0)
    acc.tienMat += Number(row.tienMatThu || 0)
    acc.tienHoan += Number(row.tienHoanMat || 0)
    acc.duKien += Number(row.tienDuKien || 0)
    acc.thucTe += Number(row.tienThucTe || 0)
    acc.chenhLech += Number(row.chenhLech || 0)
    return acc
  }, { doanhThu: 0, tienMat: 0, tienHoan: 0, duKien: 0, thucTe: 0, chenhLech: 0 })
})

onMounted(async () => {
  if (isAdmin.value) {
    await loadEmployees()
  } else if (currentStaffId.value) {
    filter.idNhanVien = currentStaffId.value
    applied.idNhanVien = currentStaffId.value
  }
  await loadData(0)
})

watch(() => route.fullPath, () => {
  loadData(page.value)
})
</script>

<template>
<div class="giao-ca-page">
  <div class="screen-head">
    <div>
      <h1 class="screen-title">Lịch sử giao ca</h1>
    </div>
    <button class="btn-export" type="button" @click="exportExcel" disabled title="Chưa hỗ trợ">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <polyline points="17 8 12 3 7 8" />
        <line x1="12" y1="3" x2="12" y2="15" />
      </svg>
      Xuất Excel
    </button>
  </div>

  <section class="panel filter-panel">
    <div class="filter-head">
      <div class="filter-title">
        <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
        </svg>
        <h3>Tìm kiếm & bộ lọc</h3>
      </div>
    </div>

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
  </section>

  <section class="panel">
    <div class="panel-head">
      <span class="panel-icon is-blue">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      </span>
      <h3 class="panel-title">Danh sách ca làm việc</h3>
      <span class="count-pill">{{ totalElements }} ca</span>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Mã ca</th>
            <th>Nhân viên</th>
            <th>Quầy</th>
            <th>Mở lúc</th>
            <th>Kết lúc</th>
            <th>Trạng thái</th>
            <th style="text-align: right">Tiền đầu ca</th>
            <th style="text-align: right">Doanh thu</th>
            <th style="text-align: right">Tiền mặt</th>
            <th style="text-align: right">Tiền K-M</th>
            <th style="text-align: right">Tiền hoàn</th>
            <th style="text-align: right">Dự kiến</th>
            <th style="text-align: right">Thực tế</th>
            <th style="text-align: right">Chênh lệch</th>
            <th>Lý do lệch</th>
            <th>Ghi chú mở</th>
            <th style="width: 120px; text-align: center;">Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="17" class="text-center py-4">Đang tải dữ liệu...</td>
          </tr>
          <tr v-else-if="list.length === 0">
            <td colspan="17" class="text-center py-4">Chưa có ca làm việc nào</td>
          </tr>
          <template v-else>
            <tr v-for="(row, idx) in list" :key="row.id || idx">
              <td class="cell-code">{{ row.maCa }}</td>
              <td>{{ row.tenNhanVien || row.maNhanVien }}</td>
              <td>{{ row.tenQuay || row.maQuay || '—' }}</td>
              <td>{{ formatDateTime(row.ngayMo) }}</td>
              <td>{{ row.ngayKet ? formatDateTime(row.ngayKet) : '—' }}</td>
              <td>
                <span class="status-badge" :class="trangThaiClass(row.trangThai)">
                  {{ getTrangThaiLabel(row.trangThai) }}
                </span>
              </td>
              <td style="text-align: right">{{ formatVnd(row.tienDauCa) }}</td>
              <td style="text-align: right">{{ formatVnd(row.tongDoanhThu) }}</td>
              <td style="text-align: right">{{ formatVnd(row.tienMatThu) }}</td>
              <td style="text-align: right">{{ formatVnd(row.tienKhongMat) }}</td>
              <td style="text-align: right">{{ formatVnd(row.tienHoanMat) }}</td>
              <td style="text-align: right">{{ formatVnd(row.tienDuKien) }}</td>
              <td style="text-align: right">{{ formatVnd(row.tienThucTe) }}</td>
              <td style="text-align: right" :class="{ 'text-red': Number(row.chenhLech || 0) !== 0 }">
                {{ formatVnd(row.chenhLech) }}
              </td>
              <td>{{ row.lyDoLech || '—' }}</td>
              <td>{{ row.ghiChuMo || '—' }}</td>
              <td style="text-align: center;">
                <button
                  class="btn-action-circle is-view"
                  type="button"
                  title="Xem đối soát"
                  @click="navigateToDoiSoat(row.id)"
                  :disabled="!row.id"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                </button>
              </td>
            </tr>
          </template>
          <tr v-if="list.length > 0" class="summary-row">
            <td colspan="7" style="text-align: right; font-weight: 600;">Tổng cộng (trang):</td>
            <td style="text-align: right">{{ formatVnd(summary.doanhThu) }}</td>
            <td style="text-align: right">{{ formatVnd(summary.tienMat) }}</td>
            <td style="text-align: right">{{ formatVnd(summary.tienHoan) }}</td>
            <td></td>
            <td style="text-align: right">{{ formatVnd(summary.duKien) }}</td>
            <td style="text-align: right">{{ formatVnd(summary.thucTe) }}</td>
            <td style="text-align: right" :class="{ 'text-red': summary.chenhLech !== 0 }">
              {{ formatVnd(summary.chenhLech) }}
            </td>
            <td colspan="3"></td>
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

.screen-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.screen-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-export {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-export svg {
  width: 16px;
  height: 16px;
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
  .filter-row > .f-item {
    grid-column: 1 / -1;
  }
  .screen-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>