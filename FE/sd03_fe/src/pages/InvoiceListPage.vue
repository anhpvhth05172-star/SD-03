<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteInvoice, getInvoice, listInvoices } from '../api/invoice'
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

const detailData = ref(null)

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
  if (activeTab.value > 0) params.trangThai = tabs[activeTab.value]
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

const openDetail = async (row) => {
  try {
    detailData.value = await getInvoice(row.id)
  } catch (e) {
    alert(e.message)
  }
}

const removeRow = async (row) => {
  if (!confirm(`Xóa hóa đơn ${row.maHoaDon}? Hành động này không thể hoàn tác.`)) return
  try {
    await deleteInvoice(row.id)
    await load()
  } catch (e) {
    alert(e.message)
  }
}

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

      <div v-show="showFilter" class="filter-row">
        <div class="f-item">
          <label>Mã hóa đơn</label>
          <input v-model="fMa" type="text" placeholder="Nhập mã hóa đơn" @keyup.enter="applyFilter" />
        </div>

        <div class="f-item">
          <label>Ngày bắt đầu</label>
          <div class="date-box">
            <input v-model="fTuNgay" type="text" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="f-item">
          <label>Ngày kết thúc</label>
          <div class="date-box">
            <input v-model="fDenNgay" type="text" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="f-item">
          <label>Loại đơn</label>
          <select v-model="fLoaiDon">
            <option value="">Tất cả</option>
            <option value="Tại quầy">Tại quầy</option>
            <option value="Online">Online</option>
          </select>
        </div>

        <div class="filter-actions">
          <button class="btn-reset" type="button" aria-label="Đặt lại" @click="resetFilter">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
          </button>
          <button class="btn-apply" type="button" @click="applyFilter">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            Áp dụng
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
          <button class="btn-export" type="button">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3.5v11M7.5 10.5l4.5 4.5 4.5-4.5M4.5 20.5h15" />
            </svg>
            Xuất File
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
                  <button class="act-btn" type="button" aria-label="Xem" @click="openDetail(row)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                  <button class="act-btn is-red" type="button" aria-label="Sửa" @click="router.push(`/hoa-don/${row.id}/sua`)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3zM14.5 6.5l3 3" />
                    </svg>
                  </button>
                  <button class="act-btn" type="button" aria-label="Xóa" @click="removeRow(row)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" />
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

    <div v-if="detailData" class="modal-mask" @click.self="detailData = null">
      <div class="modal-card">
        <div class="modal-head">
          <h3>Chi tiết hóa đơn <span class="modal-code">{{ detailData.maHoaDon }}</span></h3>
          <button class="modal-close" type="button" aria-label="Đóng" @click="detailData = null">×</button>
        </div>

        <div class="modal-grid">
          <div class="m-item"><label>Loại đơn</label><span>{{ detailData.loaiDon }}</span></div>
          <div class="m-item"><label>Trạng thái</label><span class="pill" :class="statusPill(detailData.trangThai)">{{ detailData.trangThai }}</span></div>
          <div class="m-item"><label>Khách hàng</label><span>{{ detailData.tenKhachHang || 'Khách lẻ' }}</span></div>
          <div class="m-item"><label>Số điện thoại</label><span>{{ detailData.soDienThoaiKhachHang || '—' }}</span></div>
          <div class="m-item"><label>Nhân viên</label><span>{{ detailData.tenNhanVien || '—' }}</span></div>
          <div class="m-item"><label>Phương thức TT</label><span>{{ detailData.tenPhuongThucThanhToan || '—' }}</span></div>
          <div class="m-item"><label>Phiếu giảm giá</label><span>{{ detailData.tenPhieuGiamGia || 'Không dùng' }}</span></div>
          <div class="m-item"><label>Ngày tạo</label><span>{{ formatDateTime(detailData.ngayTao) }}</span></div>
          <div class="m-item m-wide"><label>Địa chỉ nhận hàng</label><span>{{ detailData.diaChiNhanHang || '—' }}</span></div>
          <div class="m-item m-wide"><label>Ghi chú</label><span>{{ detailData.ghiChu || '—' }}</span></div>
        </div>

        <table class="data-table modal-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Sản phẩm</th>
              <th>SL</th>
              <th>Đơn giá</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(line, i) in detailData.chiTiet" :key="line.id">
              <td>{{ i + 1 }}</td>
              <td>
                <div class="entity-name">{{ line.tenSanPham }}</div>
                <div class="entity-sub">{{ line.maSanPhamChiTiet }} • {{ line.tenKichCo }} • {{ line.tenMau }}</div>
              </td>
              <td>{{ line.soLuong }}</td>
              <td>{{ formatVnd(line.donGia) }}</td>
              <td class="cell-strong">{{ formatVnd(line.thanhTien) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="modal-totals">
          <div class="t-row"><span>Phí vận chuyển</span><span>{{ formatVnd(detailData.phiVanChuyen) }}</span></div>
          <div class="t-row"><span>Tổng tiền</span><span>{{ formatVnd(detailData.tongTien) }}</span></div>
          <div class="t-row t-final"><span>Tiền sau giảm giá</span><span>{{ formatVnd(detailData.tienSauGiamGia) }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/styles/InvoiceListPage.css"></style>
