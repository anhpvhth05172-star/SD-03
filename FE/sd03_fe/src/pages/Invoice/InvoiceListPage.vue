<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  listInvoices,
  exportInvoices,
  deleteInvoice,
  restoreInvoice,
} from '@/api/invoice'
import {
  avatarColor,
  formatDate,
  formatDateTime,
  formatVnd,
  initialsOf,
  payStatusOf,
} from '@/utils/format'
import { notifySuccess, notifyWarning, notifyError, notifyDeleteConfirm } from '@/utils/notify'

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
  { key: 'DA_XOA', label: 'Đã xóa' },
]

const tabDot = {
  ALL: '',
  CHO_XAC_NHAN: 'dot-amber',
  DA_XAC_NHAN: 'dot-sky',
  CHO_GIAO_HANG: 'dot-blue',
  DANG_GIAO_HANG: 'dot-teal',
  DA_GIAO_HANG: 'dot-green',
  DA_HOAN_THANH: 'dot-green',
  DA_HUY: 'dot-gray',
  DA_HOAN_TIEN: 'dot-rose',
  DA_XOA: 'dot-red',
}

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

const showFilter = ref(true)

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
  if (activeTab.value === 'DA_XOA') {
    params.daXoa = true
  } else if (activeTab.value !== 'ALL') {
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

const appliedRange = ref('')

const load = async () => {
  const params = buildParams()
  if (!params) {
    showNotFound()
    notifyWarning('Khoảng ngày không hợp lệ', 'Vui lòng kiểm tra lại ngày bắt đầu và ngày kết thúc')
    return
  }
  const parts = []
  if (params.tuNgay) parts.push(`từ ${formatDate(params.tuNgay)}`)
  if (params.denNgay) parts.push(`đến ${formatDate(params.denNgay)}`)
  appliedRange.value = parts.join(' ')
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
  fTuNgay.value = ''
  fDenNgay.value = ''
  fLoaiDon.value = ''
  activeTab.value = 'ALL'
  page.value = 0
  load()
}

const selectTab = (key) => {
  activeTab.value = key
  page.value = 0
  load()
}

const goPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  load()
}

const onPageSizeChange = () => {
  page.value = 0
  load()
}

const isDeletedTab = computed(() => activeTab.value === 'DA_XOA')

const canSoftDelete = (row) =>
  row.trangThai === 'Chờ xác nhận' || row.trangThai === 'Đã hủy'

const xoaHoaDon = async (row) => {
  const ok = await notifyDeleteConfirm(row.maHoaDon || `hóa đơn #${row.id}`)
  if (!ok) return
  try {
    await deleteInvoice(row.id)
    notifySuccess('Đã xóa hóa đơn', `Hóa đơn ${row.maHoaDon} đã được chuyển vào thùng rác`)
    load()
  } catch (e) {
    notifyError('Không thể xóa', e.message || 'Chỉ hóa đơn Chờ xác nhận hoặc Đã hủy mới được xóa')
  }
}

const khoiPhucHoaDon = async (row) => {
  try {
    await restoreInvoice(row.id)
    notifySuccess('Đã khôi phục', `Hóa đơn ${row.maHoaDon} đã được khôi phục`)
    load()
  } catch (e) {
    notifyError('Không thể khôi phục', e.message || 'Có lỗi xảy ra')
  }
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
  delete exportParams.daXoa
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
  if (['Đã hủy', 'Đã hoàn tiền'].includes(value)) return 'inv-pill-red'
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
        <button class="btn-add-new" type="button" @click="router.push('/hoa-don/them')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Tạo hóa đơn
        </button>
      </div>
    </div>

    <div class="filter-box">
      <div class="status-tabs-row">
        <div class="tabs-group">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="status-tab"
            :class="{ active: activeTab === tab.key, 'tab-danger': tab.key === 'DA_XOA' }"
            @click="selectTab(tab.key)"
          >
            <span v-if="tabDot[tab.key]" class="dot" :class="tabDot[tab.key]"></span>
            <span v-else class="tab-icon">📄</span>
            {{ tab.label }}
          </button>
        </div>
        <button class="btn-reset-filter" type="button" @click="showFilter = !showFilter">
          {{ showFilter ? 'Ẩn bộ lọc' : 'Hiện bộ lọc' }}
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path :d="showFilter ? 'M6 14.5l6-6 6 6' : 'M6 9.5l6 6 6-6'" />
          </svg>
        </button>
      </div>

      <div v-show="showFilter">
        <div class="filters-grid">
          <div class="filter-col span-2">
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
            <label class="filter-lbl">Loại đơn</label>
            <select v-model="fLoaiDon" class="form-select" @change="applyFilter">
              <option value="">Tất cả</option>
              <option value="Tại quầy">Tại quầy</option>
              <option value="Online">Online</option>
            </select>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Trạng thái thanh toán</label>
            <div class="form-control no-icon" style="cursor: default; color: #94a3b8">
              {{ isDeletedTab ? 'Không áp dụng' : 'Theo trạng thái đơn hàng' }}
            </div>
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Từ ngày</label>
            <input v-model="fTuNgay" type="date" class="form-control" @change="applyFilter" />
          </div>

          <div class="filter-col">
            <label class="filter-lbl">Đến ngày</label>
            <input v-model="fDenNgay" type="date" class="form-control" @change="applyFilter" />
          </div>
        </div>

        <div class="filter-actions">
          <button class="btn-apply" type="button" @click="applyFilter">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            Áp dụng
          </button>
          <button class="btn-reset-filter" type="button" @click="resetFilter">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
            Đặt lại
          </button>
          <span v-if="appliedRange" class="foot-info">Khoảng thời gian: {{ appliedRange }}</span>
        </div>
      </div>
    </div>

    <div class="table-container-card">
      <div class="table-card-head">
        <div class="head-left">
          <h2 class="card-title">Danh sách hóa đơn</h2>
          <span class="items-count-pill">{{ invoices.length }}/{{ totalElements }} hóa đơn</span>
        </div>
        <div class="head-right">
          <span class="page-size-label">Hiển thị:</span>
          <select v-model="pageSize" class="page-size-select" @change="onPageSizeChange">
            <option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }} bản ghi/trang</option>
          </select>
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
                      v-if="!isDeletedTab"
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
                    <button
                      v-if="isDeletedTab"
                      class="act-btn restore-btn"
                      type="button"
                      title="Khôi phục hóa đơn"
                      @click="khoiPhucHoaDon(row)"
                    >
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="1 4 1 10 7 10"></polyline>
                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                      </svg>
                    </button>
                    <button
                      v-else-if="canSoftDelete(row)"
                      class="act-btn del-btn"
                      type="button"
                      title="Xóa mềm hóa đơn"
                      @click="xoaHoaDon(row)"
                    >
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="table-card-foot">
        <span class="foot-info">
          Hiển thị <strong>{{ footFrom }} - {{ footTo }}</strong> trong tổng số
          <strong>{{ totalElements }}</strong> hóa đơn
        </span>
        <div class="foot-pager">
          <button class="pager-btn" type="button" :disabled="page === 0" @click="goPage(page - 1)">
            ‹ Trước
          </button>
          <button
            v-for="p in pageList"
            :key="p"
            type="button"
            class="pager-btn num-btn"
            :class="{ active: p === page + 1 }"
            @click="goPage(p - 1)"
          >
            {{ p }}
          </button>
          <button class="pager-btn" type="button" :disabled="page + 1 >= totalPages" @click="goPage(page + 1)">
            Sau ›
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/styles/InvoiceListPage.css"></style>
