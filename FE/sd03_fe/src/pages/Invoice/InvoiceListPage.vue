<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SearchableSelect from '@/components/SearchableSelect.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { listInvoices, exportInvoices } from '@/api/invoice'
import {
  avatarColor,
  formatDateTime,
  formatVnd,
  initialsOf,
  payStatusOf,
} from '@/utils/format'
import { notifySuccess, notifyWarning, notifyError } from '@/utils/notify'

const router = useRouter()

const tabs = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'CHO_XAC_NHAN', label: 'Chờ xác nhận' },
  { key: 'DA_XAC_NHAN', label: 'Đã xác nhận' },
  { key: 'CHO_GIAO_HANG', label: 'Chờ giao hàng' },
  { key: 'DANG_GIAO_HANG', label: 'Đang giao hàng' },
  { key: 'DA_GIAO_HANG', label: 'Đã giao hàng' },
  { key: 'DA_HOAN_THANH', label: 'Đã hoàn thành' },
  { key: 'DA_HUY', label: 'Đã hủy' },
  { key: 'DA_HOAN_TIEN', label: 'Đã hoàn tiền' },
  { key: 'GIAO_HANG_THAT_BAI', label: 'Giao hàng thất bại' },
]

const activeTab = ref('ALL')

const invoices = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const pageSize = ref(10)
const pageSizeOptions = [5, 10]
const updatedAt = ref('')

const fMa = ref('')
const fTuNgay = ref('')
const fDenNgay = ref('')
const fLoaiDon = ref('')

const loaiDonOptions = [
  { id: '', ten: 'Tất cả' },
  { id: 'Tại quầy', ten: 'Tại quầy' },
  { id: 'Online', ten: 'Online' },
]

const trangThaiOptions = computed(() => tabs.map((t) => ({ id: t.key, ten: t.label })))

const todayLocal = () => {
  const d = new Date()
  const p = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

fTuNgay.value = todayLocal()
fDenNgay.value = todayLocal()

const parseDate = (value) => {
  const v = (value || '').trim()
  if (!v) return null
  if (/^\d{4}-\d{2}-\d{2}$/.test(v)) return v
  const m = v.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (m) return `${m[3]}-${m[2]}-${m[1]}`
  return undefined
}

const buildParams = () => {
  const params = { page: page.value, size: pageSize.value }
  if (fMa.value) params.ma = fMa.value
  const tu = parseDate(fTuNgay.value)
  const den = parseDate(fDenNgay.value)
  if (tu === undefined || den === undefined) return null
  if (tu) params.tuNgay = tu
  if (den) params.denNgay = den
  if (fLoaiDon.value) params.loaiDon = fLoaiDon.value
  if (activeTab.value !== 'ALL') {
    const tab = tabs.find((t) => t.key === activeTab.value)
    params.trangThai = tab ? tab.label : ''
  }
  return params
}

const showNotFound = () => {
  invoices.value = []
  totalElements.value = 0
  totalPages.value = 0
}

const load = async () => {
  const params = buildParams()
  if (!params) {
    showNotFound()
    notifyWarning('Khoảng ngày không hợp lệ', 'Vui lòng kiểm tra lại ngày bắt đầu và ngày kết thúc')
    return
  }
  loading.value = true
  try {
    const data = await listInvoices(params)
    invoices.value = data.content || []
    totalElements.value = data.totalElements || 0
    totalPages.value = data.totalPages || 0
    const now = new Date()
    const p = (x) => String(x).padStart(2, '0')
    updatedAt.value = `${p(now.getHours())}:${p(now.getMinutes())} ${p(now.getDate())}/${p(now.getMonth() + 1)}/${now.getFullYear()}`
  } catch {
    showNotFound()
  } finally {
    loading.value = false
  }
}

const applyFilter = () => {
  page.value = 0
  load()
}

const resetFilter = () => {
  fMa.value = ''
  fTuNgay.value = todayLocal()
  fDenNgay.value = todayLocal()
  fLoaiDon.value = ''
  activeTab.value = 'ALL'
  page.value = 0
  load()
}

const selectTab = (key) => {
  activeTab.value = key || 'ALL'
  page.value = 0
  load()
}

const goPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  load()
}

const onPageSizeChange = (s) => {
  pageSize.value = s
  page.value = 0
  load()
}

const exporting = ref(false)

const xuatExcel = async () => {
  const params = buildParams()
  if (!params) {
    notifyWarning('Khoảng ngày không hợp lệ')
    return
  }
  const exportParams = { ...params }
  delete exportParams.page
  delete exportParams.size
  exporting.value = true
  try {
    const blob = await exportInvoices(exportParams)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const p = (x) => String(x).padStart(2, '0')
    const now = new Date()
    const ngay = `${now.getFullYear()}-${p(now.getMonth() + 1)}-${p(now.getDate())}`
    a.href = url
    a.download = `Danh_sach_hoa_don_${ngay}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    notifySuccess('Xuất Excel thành công')
  } catch (e) {
    notifyError('Xuất file Excel thất bại', e.message || '')
  } finally {
    exporting.value = false
  }
}

const footFrom = computed(() =>
  totalElements.value === 0 || invoices.value.length === 0 ? 0 : page.value * pageSize.value + 1,
)
const footTo = computed(() =>
  totalElements.value === 0 || invoices.value.length === 0
    ? 0
    : page.value * pageSize.value + invoices.value.length,
)

const pageList = computed(() => {
  const total = totalPages.value
  const cur = page.value + 1
  let start = Math.max(1, cur - 2)
  let end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  const arr = []
  for (let i = start; i <= end; i++) arr.push(i)
  return arr
})

const statusPill = (value) => {
  if (['Đã hoàn thành', 'Đã giao hàng'].includes(value)) return 'inv-pill-green'
  if (['Đã xác nhận'].includes(value)) return 'inv-pill-sky'
  if (['Chờ giao hàng'].includes(value)) return 'inv-pill-blue'
  if (['Đang giao hàng'].includes(value)) return 'inv-pill-teal'
  if (['Đã hủy', 'Đã hoàn tiền', 'Giao hàng thất bại'].includes(value)) return 'inv-pill-red'
  if (['Chờ xác nhận'].includes(value)) return 'inv-pill-amber'
  return 'inv-pill-gray'
}

const payPill = (value) => {
  const s = payStatusOf(value)
  if (s === 'Đã thanh toán') return 'inv-pill-green'
  if (s === 'Đã hoàn tiền') return 'inv-pill-red'
  return 'inv-pill-gray'
}

const payLabel = (value) => payStatusOf(value)

const fmtNgayTao = (iso) => {
  if (!iso) return '—'
  return formatDateTime(iso)
}

const tongTienText = (v) => formatVnd(v)

const xemChiTiet = (row) => router.push(`/hoa-don/${row.id}`)

onMounted(load)
</script>

<template>
  <div class="invoice-management-page">
    <div class="page-top-row">
      <div class="page-intro">
        <div class="breadcrumbs-tag">
          <span>Trang chủ</span> / <span class="active">Quản lý hóa đơn</span>
        </div>
        <h1 class="page-heading">
          Quản lý Hóa đơn
          <span class="count-badge">{{ totalElements }}</span>
        </h1>
      </div>
      <div class="page-actions">
        <button
          class="btn-outline-refresh"
          :class="{ 'is-loading': loading }"
          type="button"
          title="Tải lại dữ liệu"
          @click="load"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"></polyline>
            <polyline points="1 20 1 14 7 14"></polyline>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
          Làm mới
        </button>
        <button
          class="btn-export-excel"
          type="button"
          :disabled="exporting"
          title="Xuất danh sách hóa đơn ra file Excel"
          @click="xuatExcel"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="8" y1="13" x2="16" y2="13"></line>
            <line x1="8" y1="17" x2="16" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
        </button>
      </div>
    </div>

    <div class="filter-box">
      <div>
        <div class="filters-grid">
          <div class="filter-col filter-col-search">
            <label class="filter-lbl">Tìm kiếm hóa đơn</label>
            <div class="search-wrap">
              <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                v-model="fMa"
                type="text"
                class="form-control"
                placeholder="Nhập mã hóa đơn (vd: HD000013)"
                @keyup.enter="applyFilter"
              />
              <button v-if="fMa" class="clear-search-btn" type="button" @click="fMa = ''; applyFilter()">×</button>
            </div>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Trạng thái</label>
            <SearchableSelect
              v-model="activeTab"
              :options="trangThaiOptions"
              value-key="id"
              label-key="ten"
              placeholder="Tất cả trạng thái"
              search-placeholder="Tìm trạng thái..."
              @change="selectTab"
            />
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Loại đơn</label>
            <SearchableSelect
              v-model="fLoaiDon"
              :options="loaiDonOptions"
              value-key="id"
              label-key="ten"
              placeholder="Tất cả loại đơn"
              search-placeholder="Tìm loại đơn..."
              @change="applyFilter"
            />
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Từ ngày</label>
            <input v-model="fTuNgay" type="date" class="form-control" @change="applyFilter" />
          </div>

          <div class="filter-actions">
            <button class="btn-reset-filter" type="button" @click="resetFilter">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 11a8 8 0 1 0-.9 4.5" />
                <path d="M20 4.5V11h-6.5" />
              </svg>
              Đặt lại
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-container-card">
      <div class="table-card-head">
        <div class="head-left">
          <h2 class="card-title">Danh sách hóa đơn</h2>
          <span class="items-count-pill">{{ invoices.length }}/{{ totalElements }} hóa đơn</span>
        </div>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th width="60" class="text-center">STT</th>
              <th width="150">MÃ HÓA ĐƠN</th>
              <th width="230">KHÁCH HÀNG</th>
              <th width="150">NHÂN VIÊN</th>
              <th width="110">LOẠI ĐƠN</th>
              <th width="140" class="text-end">TỔNG TIỀN</th>
              <th width="160">NGÀY TẠO</th>
              <th width="150" class="text-center">TRẠNG THÁI</th>
              <th width="140" class="text-center">THANH TOÁN</th>
              <th width="110" class="text-center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading">
              <tr v-for="n in 5" :key="`skel-${n}`">
                <td class="text-center"><div class="skel-box skel-code"></div></td>
                <td><div class="skel-box skel-code"></div></td>
                <td><div class="skel-box skel-title"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td class="text-end"><div class="skel-box skel-text"></div></td>
                <td><div class="skel-box skel-text"></div></td>
                <td class="text-center"><div class="skel-box skel-pill"></div></td>
                <td class="text-center"><div class="skel-box skel-pill"></div></td>
                <td class="text-center"><div class="skel-box skel-btn"></div></td>
              </tr>
            </template>

            <tr v-else-if="invoices.length === 0">
              <td colspan="10" class="empty-state-cell">
                <div class="empty-state-box">
                  <div class="empty-icon">🧾</div>
                  <div class="empty-title">Không tìm thấy hóa đơn nào</div>
                  <p class="empty-desc">Thử thay đổi từ khóa tìm kiếm hoặc đặt lại các bộ lọc.</p>
                  <button class="btn-outline-reset" type="button" @click="resetFilter">Xóa bộ lọc</button>
                </div>
              </td>
            </tr>

            <template v-else>
              <tr v-for="(row, idx) in invoices" :key="row.id">
                <td class="text-center text-muted">{{ page * pageSize + idx + 1 }}</td>
                <td>
                  <span class="code-wrapper">
                    <span class="code-badge">{{ row.maHoaDon || `HD${row.id}` }}</span>
                  </span>
                </td>
                <td>
                  <div class="entity-cell">
                    <span class="entity-avatar" :style="{ background: avatarColor(row.id) }">
                      {{ initialsOf(row.tenKhachHang) }}
                    </span>
                    <div class="entity-info">
                      <span class="entity-name">{{ row.tenKhachHang || 'Khách lẻ' }}</span>
                      <span class="entity-sub">{{ row.soDienThoaiKhachHang || 'Không có SĐT' }}</span>
                    </div>
                  </div>
                </td>
                <td class="attr-plain-text">{{ row.tenNhanVien || '—' }}</td>
                <td class="attr-plain-text">{{ row.loaiDon }}</td>
                <td class="text-end price-plain-text">{{ tongTienText(row.tongTien) }}</td>
                <td class="date-plain-text">{{ fmtNgayTao(row.ngayTao) }}</td>
                <td class="text-center">
                  <span class="inv-pill" :class="statusPill(row.trangThai)">{{ row.trangThai }}</span>
                </td>
                <td class="text-center">
                  <span class="inv-pill" :class="payPill(row.trangThai)">{{ payLabel(row.trangThai) }}</span>
                </td>
                <td class="text-center">
                  <div class="action-btns">
                    <button
                      class="act-btn view-btn"
                      type="button"
                      title="Xem chi tiết"
                      @click="xemChiTiet(row)"
                    >
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div class="table-card-foot">
        <PaginationBar
          :page="page"
          :total-pages="totalPages"
          :total-elements="totalElements"
          :page-size="pageSize"
          :from="footFrom"
          :to="footTo"
          :sizes="pageSizeOptions"
          label="hóa đơn"
          @change="goPage"
          @size-change="onPageSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/styles/InvoiceListPage.css"></style>
