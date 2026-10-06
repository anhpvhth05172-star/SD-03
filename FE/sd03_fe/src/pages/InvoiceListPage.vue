<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listInvoices } from '../api/invoice'
import { avatarColor, formatDateTime, formatVnd, initialsOf, payStatusOf } from '../utils/format'

const router = useRouter()
const showFilter = ref(true)

const tabs = [
  'Tất cả',
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
  'Đã hủy',
  'Đã hoàn tiền',
]
const activeTab = ref(0)

const invoices = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = 10
const updatedAt = ref('')

const fMa = ref('')
const fTuNgay = ref('')
const fDenNgay = ref('')
const fLoaiDon = ref('')

const parseDate = (value) => {
  const v = (value || '').trim()
  if (!v) return null
  if (/^\d{4}-\d{2}-\d{2}$/.test(v)) return v
  const m = v.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (m) return `${m[3]}-${m[2]}-${m[1]}`
  return undefined
}

const buildParams = () => {
  const params = { page: page.value, size }
  if (fMa.value.trim()) params.ma = fMa.value.trim()
  const tu = parseDate(fTuNgay.value)
  const den = parseDate(fDenNgay.value)
  if (tu === undefined || den === undefined) {
    alert('Sai định dạng ngày (dd/mm/yyyy)')
    return null
  }
  if (tu) params.tuNgay = tu
  if (den) params.denNgay = den
  if (fLoaiDon.value) params.loaiDon = fLoaiDon.value
  if (activeTab.value > 0) {
    params.trangThai = tabs[activeTab.value]
  }
  return params
}

const load = async () => {
  const params = buildParams()
  if (!params) return
  loading.value = true
  try {
    const data = await listInvoices(params)
    invoices.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
    const now = new Date()
    const p = (x) => String(x).padStart(2, '0')
    updatedAt.value = `${p(now.getHours())}:${p(now.getMinutes())} ${p(now.getDate())}/${p(now.getMonth() + 1)}/${now.getFullYear()}`
  } catch (e) {
    alert(e.message)
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
  activeTab.value = 0
  page.value = 0
  load()
}

const selectTab = (index) => {
  activeTab.value = index
  page.value = 0
  load()
}

const goPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  load()
}

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

const footFrom = computed(() => (totalElements.value === 0 ? 0 : page.value * size + 1))
const footTo = computed(() => Math.min((page.value + 1) * size, totalElements.value))

const statusPill = (value) => {
  if (['Đã hoàn thành', 'Đã thanh toán', 'Đã xác nhận', 'Đã giao hàng'].includes(value)) return 'pill-green'
  if (['Đã hủy', 'Đã hoàn tiền'].includes(value)) return 'pill-red'
  if (['Chờ xác nhận', 'Chờ giao hàng'].includes(value)) return 'pill-amber'
  return 'pill-gray'
}

onMounted(load)
</script>

<template>
  <div class="screen">
    <section class="panel">
      <h2 class="page-title">Quản lý hóa đơn</h2>
    </section>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Bộ lọc tìm kiếm</h3>
        <div class="panel-right">
          <span class="result-count">{{ totalElements }} kết quả</span>
          <button class="link-red" type="button" @click="showFilter = !showFilter">
            {{ showFilter ? 'Ẩn bớt' : 'Hiện thêm' }}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path :d="showFilter ? 'M6 14.5l6-6 6 6' : 'M6 9.5l6 6 6-6'" />
            </svg>
          </button>
        </div>
      </div>

      <div v-show="showFilter">
        <div class="filter-row">
          <div class="f-item">
            <label>Mã hóa đơn</label>
            <input v-model="fMa" type="text" placeholder="Nhập mã hóa đơn" @keyup.enter="applyFilter" />
          </div>

          <div class="f-item">
            <label>Loại đơn</label>
            <select v-model="fLoaiDon">
              <option value="">Tất cả</option>
              <option value="Tại quầy">Tại quầy</option>
              <option value="Online">Online</option>
            </select>
          </div>

          <div class="f-item">
            <label>Từ ngày</label>
            <div class="date-box">
              <input v-model="fTuNgay" type="text" placeholder="dd/mm/yyyy" />
              <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
                <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
              </svg>
            </div>
          </div>

          <div class="f-item">
            <label>Đến ngày</label>
            <div class="date-box">
              <input v-model="fDenNgay" type="text" placeholder="dd/mm/yyyy" />
              <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
                <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
              </svg>
            </div>
          </div>
        </div>

        <div class="filter-actions">
          <button class="btn-apply" type="button" @click="applyFilter">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            Áp dụng
          </button>
          <button class="btn-reset" type="button" @click="resetFilter">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
            Đặt lại
          </button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon is-red">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 2.5h12v19l-3-1.8-3 1.8-3-1.8-3 1.8v-19zM9.5 7.5h5M9.5 11.5h5M9.5 15.5h3.5" />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách hóa đơn</h3>
        <span class="count-pill">{{ totalElements }} hóa đơn</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc {{ updatedAt }}</span>
          <button class="btn-create" type="button" @click="router.push('/hoa-don/them')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <path d="M12 6v12M6 12h12" />
            </svg>
            Tạo hóa đơn
          </button>
        </div>
      </div>

      <div class="tabs">
        <button
          v-for="(tab, index) in tabs"
          :key="tab"
          type="button"
          class="tab"
          :class="{ 'is-active': activeTab === index }"
          @click="selectTab(index)"
        >
          {{ tab }}
        </button>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Hóa đơn</th>
              <th>Nhân viên</th>
              <th>Loại đơn</th>
              <th>Tổng tiền</th>
              <th>Ngày tạo</th>
              <th>Trạng thái hóa đơn</th>
              <th>Trạng thái thanh toán</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="empty-cell">Đang tải...</td>
            </tr>
            <tr v-else-if="invoices.length === 0">
              <td colspan="8" class="empty-cell">Không có hóa đơn nào</td>
            </tr>
            <template v-else>
            <tr v-for="row in invoices" :key="row.id">
              <td>
                <div class="entity">
                  <span class="entity-avatar" :style="{ background: avatarColor(row.id) }">{{ initialsOf(row.tenKhachHang) }}</span>
                  <div class="entity-info">
                    <div class="entity-line">
                      <span class="entity-name">{{ row.maHoaDon }}</span>
                    </div>
                    <div class="entity-sub">{{ row.tenKhachHang || 'Khách lẻ' }}<template v-if="row.soDienThoaiKhachHang"> • {{ row.soDienThoaiKhachHang }}</template></div>
                  </div>
                </div>
              </td>
              <td>{{ row.tenNhanVien || '—' }}</td>
              <td>{{ row.loaiDon }}</td>
              <td class="cell-strong">{{ formatVnd(row.tongTien) }}</td>
              <td>{{ formatDateTime(row.ngayTao) }}</td>
              <td><span class="pill" :class="statusPill(row.trangThai)">{{ row.trangThai }}</span></td>
              <td><span class="pill" :class="statusPill(payStatusOf(row.trangThai))">{{ payStatusOf(row.trangThai) }}</span></td>
              <td>
                <div class="act-group">
                  <button class="act-btn" type="button" aria-label="Xem" @click="router.push(`/hoa-don/${row.id}`)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">Hiển thị {{ footFrom }}-{{ footTo }} trong {{ totalElements }} hóa đơn</span>
        <div class="pager">
          <button class="page-btn" type="button" aria-label="Trang trước" :disabled="page === 0" @click="goPage(page - 1)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.5 6l-6 6 6 6" />
            </svg>
          </button>
          <button
            v-for="p in pageList"
            :key="p"
            class="page-btn"
            :class="{ 'is-active': p === page + 1 }"
            type="button"
            @click="goPage(p - 1)"
          >
            {{ p }}
          </button>
          <button class="page-btn" type="button" aria-label="Trang sau" :disabled="page + 1 >= totalPages" @click="goPage(page + 1)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9.5 6l6 6-6 6" />
            </svg>
          </button>
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.page-title {
  color: var(--red);
  font-size: 17px;
  font-weight: 700;
}

.filter-row {
  grid-template-columns: 1.6fr 1fr 1fr 1fr;
}

.filter-actions {
  margin-top: 14px;
}

.btn-reset {
  width: auto;
  padding: 0 14px;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 760px) {
  .filter-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 480px) {
  .filter-row {
    grid-template-columns: 1fr;
  }
}

.btn-create {
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 7px;
  background: #1d1d23;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-create:hover {
  background: #000;
}

.btn-create svg {
  width: 15px;
  height: 15px;
}

.empty-cell {
  text-align: center;
  color: #9a9aa3;
  padding: 26px 0;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

</style>
