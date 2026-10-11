<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CustomerAddressModal from '../components/CustomerAddressModal.vue'
import { getCustomer, listCustomers, updateCustomer, updateCustomerStatus } from '../api/customer'
import { exportCsv, fetchAllPages } from '../utils/export'
import { takeFlash } from '../utils/flash'
import { avatarColor, formatDate, formatDateTime, formatVnd, initialsOf } from '../utils/format'
import { layNguoiDung } from '../utils/session'
import {
  batBuoc,
  duTuoiToiThieu,
  emailHopLe,
  isBlank,
  kiemTra,
  khongDuocTuongLai,
  matKhauHopLe,
  soDienThoaiHopLe,
  toiDa,
} from '../utils/validate'

const router = useRouter()
const showFilter = ref(true)
const flashMsg = ref('')
const exporting = ref(false)

const rows = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = 10
const updatedAt = ref('')

const fKeyword = ref('')
const fHang = ref('')
const fTrangThai = ref('')
const fTuNgay = ref('')

const detailData = ref(null)

const TUOI_TOI_THIEU = 16
const THONG_BAO_TUOI = 'Khách hàng phải đủ 16 tuổi.'

/** Chỉ Nam/Nữ; value giữ nguyên NAM/NU để khớp payload API. */
const gioiTinhOptions = [
  { value: 'NAM', label: 'Nam' },
  { value: 'NU', label: 'Nữ' },
]
const trangThais = [
  { value: 'true', label: 'Đang hoạt động' },
  { value: 'false', label: 'Đã khóa' },
]

/** Chế độ xem/sửa trong form chi tiết. */
const dangSua = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const daNopSua = ref(false)
const ngaySinhDaCham = ref(false)
const formSua = ref(khongSua())

function khongSua() {
  return {
    tenKhachHang: '',
    email: '',
    soDienThoai: '',
    ngaySinh: '',
    gioiTinh: '',
    trangThai: 'true',
    matKhau: '',
  }
}

const rankOptions = [
  { value: '', label: 'Tất cả' },
  { value: 'diamond', label: 'Kim cương' },
  { value: 'gold', label: 'Vàng' },
  { value: 'silver', label: 'Bạc' },
  { value: 'normal', label: 'Thường' },
]

const statusOptions = [
  { value: '', label: 'Tất cả' },
  { value: 'active', label: 'Đang hoạt động' },
  { value: 'locked', label: 'Đã khóa' },
]

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
  if (fKeyword.value.trim()) params.keyword = fKeyword.value.trim()
  if (fHang.value) params.hangThanhVien = fHang.value
  if (fTrangThai.value) params.trangThai = fTrangThai.value
  const tu = parseDate(fTuNgay.value)
  if (tu === undefined) {
    alert('Sai định dạng ngày (dd/mm/yyyy)')
    return null
  }
  if (tu) params.tuNgay = tu
  return params
}

const load = async () => {
  const params = buildParams()
  if (!params) return
  loading.value = true
  try {
    const data = await listCustomers(params)
    const maxPage = data.totalPages > 0 ? data.totalPages - 1 : 0
    if (page.value > maxPage) {
      page.value = maxPage
      loading.value = false
      return await load()
    }
    rows.value = data.content
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
  fKeyword.value = ''
  fHang.value = ''
  fTrangThai.value = ''
  fTuNgay.value = ''
  page.value = 0
  load()
}

const goPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  load()
}

const exportFile = async () => {
  const params = buildParams()
  if (!params) return
  exporting.value = true
  try {
    const all = await fetchAllPages(listCustomers, params)
    exportCsv(
      'khach-hang.csv',
      ['Mã KH', 'Họ và tên', 'Tên đăng nhập', 'Email', 'Số điện thoại', 'Hạng thành viên', 'Tổng đơn', 'Tổng chi tiêu', 'Ngày đăng ký', 'Trạng thái'],
      all.map((r) => [
        r.maKhachHang,
        r.tenKhachHang,
        r.tenTaiKhoan || '',
        r.email || '',
        r.soDienThoai || '',
        r.hangThanhVien || '',
        r.soDon ?? 0,
        r.tongChiTieu ?? 0,
        formatDate(r.ngayTao),
        r.trangThaiLabel || '',
      ]),
    )
  } catch (e) {
    alert(e.message)
  } finally {
    exporting.value = false
  }
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
  huySua()
  try {
    detailData.value = await getCustomer(row.id)
  } catch (e) {
    alert(e.message)
  }
}

const dongChiTiet = () => {
  huySua()
  detailData.value = null
}

const batDauSua = () => {
  const d = detailData.value
  if (!d) return
  formSua.value = {
    tenKhachHang: d.tenKhachHang || '',
    email: d.email || '',
    soDienThoai: d.soDienThoai || '',
    ngaySinh: d.ngaySinh || '',
    gioiTinh: d.gioiTinh || '',
    trangThai: d.trangThai ? 'true' : 'false',
    matKhau: '',
  }
  daNopSua.value = false
  ngaySinhDaCham.value = false
  errorMsg.value = ''
  dangSua.value = true
}

/** Thoát chế độ sửa mà không lưu dữ liệu. */
const huySua = () => {
  dangSua.value = false
  daNopSua.value = false
  ngaySinhDaCham.value = false
  errorMsg.value = ''
  formSua.value = khongSua()
}

const loiNgaySinhSua = computed(() =>
  kiemTra([
    khongDuocTuongLai(formSua.value.ngaySinh, 'Ngày sinh'),
    duTuoiToiThieu(formSua.value.ngaySinh, TUOI_TOI_THIEU, THONG_BAO_TUOI),
  ]),
)

const hienThiLoiNgaySinhSua = computed(() =>
  !isBlank(formSua.value.ngaySinh) || daNopSua.value || ngaySinhDaCham.value
    ? loiNgaySinhSua.value
    : '',
)

const loiGioiTinhSua = computed(() =>
  isBlank(formSua.value.gioiTinh) ? 'Vui lòng chọn giới tính' : '',
)

const validateSua = () =>
  kiemTra([
    batBuoc(formSua.value.tenKhachHang, 'Họ và tên'),
    toiDa(formSua.value.tenKhachHang, 150, 'Họ và tên'),
    batBuoc(formSua.value.email, 'Email'),
    toiDa(formSua.value.email, 150, 'Email'),
    emailHopLe(formSua.value.email),
    batBuoc(formSua.value.soDienThoai, 'Số điện thoại'),
    toiDa(formSua.value.soDienThoai, 10, 'Số điện thoại'),
    soDienThoaiHopLe(formSua.value.soDienThoai),
    loiNgaySinhSua.value,
    loiGioiTinhSua.value,
    matKhauHopLe(formSua.value.matKhau, false),
    toiDa(formSua.value.matKhau, 127, 'Mật khẩu'),
  ])

const luuSua = async () => {
  daNopSua.value = true
  const loi = validateSua()
  if (loi) {
    errorMsg.value = loi
    return
  }
  errorMsg.value = ''
  saving.value = true
  try {
    const d = detailData.value
    // Chỉ cập nhật khách hàng đang xem; không gửi địa chỉ để BE giữ nguyên danh sách địa chỉ.
    await updateCustomer(d.id, {
      maKhachHang: d.maKhachHang,
      tenKhachHang: formSua.value.tenKhachHang.trim(),
      matKhau: formSua.value.matKhau ? formSua.value.matKhau : null,
      email: formSua.value.email.trim(),
      soDienThoai: formSua.value.soDienThoai.trim() || null,
      ngaySinh: formSua.value.ngaySinh || null,
      gioiTinh: formSua.value.gioiTinh || null,
      trangThai: formSua.value.trangThai === 'true',
      tinhThanhPho: null,
      phuong: null,
      diaChiCuThe: null,
      nguoiCapNhat: layNguoiDung(),
    })
    flashMsg.value = 'Đã cập nhật thông tin khách hàng'
    dangSua.value = false
    daNopSua.value = false
    formSua.value = khongSua()
    detailData.value = await getCustomer(d.id)
    await load()
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const moi = !row.trangThai
  try {
    await updateCustomerStatus(row.id, moi ? 'true' : 'false')
    row.trangThai = moi
    row.trangThaiLabel = moi ? 'Đang hoạt động' : 'Đã khóa'
    if (detailData.value && detailData.value.id === row.id) {
      detailData.value.trangThai = moi
      detailData.value.trangThaiLabel = row.trangThaiLabel
    }
    await load()
  } catch (e) {
    alert(e.message)
  }
}

const diaChiCustomerId = ref(null)
const openAddress = (row) => {
  diaChiCustomerId.value = row.id
}
const dongDiaChi = () => {
  diaChiCustomerId.value = null
}
const doiDiaChi = async () => {
  if (detailData.value) {
    try {
      detailData.value = await getCustomer(detailData.value.id)
    } catch {
      /* bỏ qua */
    }
  }
  load()
}

const gioiTinhLabel = (value) => {
  if (value === 'NAM') return 'Nam'
  if (value === 'NU') return 'Nữ'
  if (value === 'KHAC') return 'Khác'
  return '—'
}

const diaChiDay = (d) => [d.diaChiCuThe, d.phuong, d.thanhPho].filter(Boolean).join(', ') || '—'

const dsDiaChi = computed(() => (detailData.value && detailData.value.danhSachDiaChi) || [])

const statusPill = (value) => (value ? 'pill-green' : 'pill-gray')

const rankPill = (rank) => {
  if (rank === 'Kim cương') return 'pill-blue'
  if (rank === 'Vàng') return 'pill-amber'
  if (rank === 'Bạc') return 'pill-gray'
  return 'pill-green'
}

onMounted(() => {
  flashMsg.value = takeFlash()
  load()
})
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý khách hàng</h1>
        <p class="screen-sub">Quản lý thông tin khách hàng, tích điểm thành viên và lịch sử mua hàng</p>
      </div>
      <div class="head-actions">
        <button class="btn-export" type="button" :disabled="exporting" @click="exportFile">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3v12M7 10l5 5 5-5M4 21h16" />
          </svg>
          {{ exporting ? 'Đang xuất...' : 'Xuất File' }}
        </button>
        <button class="btn-add" type="button" @click="router.push('/khach-hang/them')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <path d="M12 6v12M6 12h12" />
          </svg>
          Thêm khách hàng
        </button>
      </div>
    </div>

    <p v-if="flashMsg" class="flash-ok">{{ flashMsg }}</p>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Tìm kiếm &amp; bộ lọc</h3>
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
          <label>Tìm khách hàng</label>
          <input v-model="fKeyword" type="text" placeholder="Nhập tên, SĐT hoặc email" @keyup.enter="applyFilter" />
        </div>

        <div class="f-item">
          <label>Hạng thành viên</label>
          <select v-model="fHang">
            <option v-for="r in rankOptions" :key="r.value" :value="r.value">{{ r.label }}</option>
          </select>
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select v-model="fTrangThai">
            <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>

        <div class="f-item">
          <label>Ngày đăng ký</label>
          <div class="date-box">
            <input v-model="fTuNgay" type="text" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
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
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"
            />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách khách hàng</h3>
        <span class="count-pill">{{ totalElements }} khách hàng</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc {{ updatedAt }}</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Khách hàng</th>
              <th>Hạng thành viên</th>
              <th>Tổng đơn</th>
              <th>Tổng chi tiêu</th>
              <th>Ngày đăng ký</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="empty-cell">Đang tải...</td>
            </tr>
            <tr v-else-if="rows.length === 0">
              <td colspan="7" class="empty-cell">Không có khách hàng nào</td>
            </tr>
            <template v-else>
              <tr v-for="row in rows" :key="row.id">
                <td>
                  <div class="entity">
                    <span class="entity-avatar" :style="{ background: avatarColor(row.id) }">{{ initialsOf(row.tenKhachHang) }}</span>
                    <div class="entity-info">
                      <div class="entity-line">
                        <span class="entity-name">{{ row.tenKhachHang }}</span>
                        <span class="entity-code">{{ row.maKhachHang }}</span>
                      </div>
                      <div class="entity-sub">
                        {{ row.soDienThoai || '—' }}<template v-if="row.email"> · {{ row.email }}</template>
                      </div>
                    </div>
                  </div>
                </td>
                <td><span class="pill" :class="rankPill(row.hangThanhVien)">{{ row.hangThanhVien }}</span></td>
                <td>{{ row.soDon }} đơn</td>
                <td class="cell-strong">{{ formatVnd(row.tongChiTieu) }}</td>
                <td>{{ formatDate(row.ngayTao) }}</td>
                <td><span class="pill" :class="statusPill(row.trangThai)">{{ row.trangThaiLabel }}</span></td>
                <td>
                  <div class="act-group">
                    <button class="act-btn" type="button" aria-label="Xem" title="Xem chi tiết" @click="openDetail(row)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    </button>
                    <button class="act-btn" type="button" aria-label="Địa chỉ" title="Quản lý địa chỉ" @click="openAddress(row)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z" />
                        <circle cx="12" cy="10" r="2.6" />
                      </svg>
                    </button>
                    <button
                      class="toggle"
                      :class="{ 'is-on': row.trangThai }"
                      type="button"
                      role="switch"
                      :aria-checked="row.trangThai ? 'true' : 'false'"
                      :aria-label="row.trangThai ? 'Tắt tài khoản' : 'Bật tài khoản'"
                      :title="row.trangThai ? 'Turn off' : 'Turn on'"
                      @click="toggleStatus(row)"
                    >
                      <span class="toggle-dot"></span>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">Hiển thị {{ footFrom }}-{{ footTo }} trong {{ totalElements }} khách hàng</span>
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

    <div v-if="detailData" class="modal-mask" @click.self="dongChiTiet">
      <div class="modal-card">
        <div class="modal-head">
          <h3>Chi tiết khách hàng <span class="modal-code">{{ detailData.maKhachHang }}</span></h3>
          <button class="modal-close" type="button" aria-label="Đóng" @click="dongChiTiet">×</button>
        </div>

        <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

        <div class="modal-grid">
          <div class="m-item">
            <label>Họ và tên <b v-if="dangSua" class="req">*</b></label>
            <input
              v-if="dangSua"
              v-model="formSua.tenKhachHang"
              class="m-input"
              type="text"
              maxlength="200"
            />
            <span v-else>{{ detailData.tenKhachHang }}</span>
          </div>

          <div class="m-item">
            <label>Tên đăng nhập</label>
            <span>{{ detailData.tenTaiKhoan || '—' }}</span>
          </div>

          <div class="m-item">
            <label>Trạng thái</label>
            <select v-if="dangSua" v-model="formSua.trangThai" class="m-select">
              <option v-for="t in trangThais" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
            <span v-else class="pill" :class="statusPill(detailData.trangThai)">{{ detailData.trangThaiLabel }}</span>
          </div>

          <div class="m-item">
            <label>Email <b v-if="dangSua" class="req">*</b></label>
            <input v-if="dangSua" v-model="formSua.email" class="m-input" type="email" maxlength="150" />
            <span v-else>{{ detailData.email || '—' }}</span>
          </div>

          <div class="m-item">
            <label>Số điện thoại <b v-if="dangSua" class="req">*</b></label>
            <input v-if="dangSua" v-model="formSua.soDienThoai" class="m-input" type="tel" maxlength="10" />
            <span v-else>{{ detailData.soDienThoai || '—' }}</span>
          </div>

          <div class="m-item">
            <label>Giới tính <b v-if="dangSua" class="req">*</b></label>
            <div v-if="dangSua" class="m-radio-row" role="radiogroup" aria-label="Giới tính">
              <label v-for="g in gioiTinhOptions" :key="g.value" class="m-radio">
                <input v-model="formSua.gioiTinh" type="radio" name="gtChiTiet" :value="g.value" />
                <span>{{ g.label }}</span>
              </label>
            </div>
            <span v-else>{{ gioiTinhLabel(detailData.gioiTinh) }}</span>
            <p v-if="dangSua && daNopSua && loiGioiTinhSua" class="m-hint">{{ loiGioiTinhSua }}</p>
          </div>

          <div class="m-item">
            <label>Ngày sinh</label>
            <input
              v-if="dangSua"
              v-model="formSua.ngaySinh"
              class="m-input"
              type="date"
              @blur="ngaySinhDaCham = true"
            />
            <span v-else>{{ formatDate(detailData.ngaySinh) }}</span>
            <p v-if="dangSua && hienThiLoiNgaySinhSua" class="m-hint">{{ hienThiLoiNgaySinhSua }}</p>
          </div>

          <div v-if="dangSua" class="m-item">
            <label>Mật khẩu mới</label>
            <input
              v-model="formSua.matKhau"
              class="m-input"
              type="password"
              maxlength="127"
              placeholder="Bỏ trống để giữ nguyên"
            />
          </div>

          <div class="m-item">
            <label>Hạng thành viên</label>
            <span class="pill" :class="rankPill(detailData.hangThanhVien)">{{ detailData.hangThanhVien }}</span>
          </div>

          <div class="m-item">
            <label>Ngày đăng ký</label>
            <span>{{ formatDate(detailData.ngayTao) }}</span>
          </div>

          <div class="m-item">
            <label>Tổng đơn</label>
            <span>{{ detailData.soDon }} đơn</span>
          </div>

          <div class="m-item">
            <label>Tổng chi tiêu</label>
            <span class="cell-strong">{{ formatVnd(detailData.tongChiTieu) }}</span>
          </div>

          <div class="m-item">
            <label>Cập nhật lúc</label>
            <span>{{ formatDateTime(detailData.ngayCapNhat) }}</span>
          </div>
        </div>

        <div class="m-section">
          <div class="m-section-head">
            <label>Địa chỉ ({{ dsDiaChi.length }})</label>
          </div>
          <p v-if="dsDiaChi.length === 0" class="m-empty">Chưa có địa chỉ nào</p>
          <ul v-else class="addr-view">
            <li v-for="d in dsDiaChi" :key="d.id" :title="diaChiDay(d)">
              <div class="addr-head">
                <span>{{ d.tenChiChi || d.maDiaChi }}</span>
                <span v-if="d.macDinh" class="pill pill-green">Mặc định</span>
              </div>
              <div class="addr-line"><b>Số nhà, đường:</b> {{ d.diaChiCuThe || '—' }}</div>
              <div class="addr-line"><b>Phường/Xã:</b> {{ d.phuong || '—' }}</div>
              <div class="addr-line"><b>Tỉnh/Thành phố:</b> {{ d.thanhPho || '—' }}</div>
              <div class="addr-line addr-meta">Mã địa chỉ: {{ d.maDiaChi }}</div>
            </li>
          </ul>
        </div>

        <div class="m-actions">
          <template v-if="dangSua">
            <button class="m-btn is-red" type="button" :disabled="saving" @click="luuSua">
              {{ saving ? 'Đang lưu...' : 'Lưu' }}
            </button>
            <button class="m-btn is-ghost" type="button" :disabled="saving" @click="huySua">Hủy</button>
          </template>
          <template v-else>
            <button class="m-btn is-red" type="button" @click="batDauSua">Sửa</button>
            <button class="m-btn is-ghost" type="button" @click="dongChiTiet">Đóng</button>
          </template>
        </div>
      </div>
    </div>
    <CustomerAddressModal
      v-if="diaChiCustomerId"
      :customer-id="diaChiCustomerId"
      @close="dongDiaChi"
      @changed="doiDiaChi"
    />
  </div>
</template>

<style scoped>
.flash-ok {
  background: #e7f7ee;
  border: 1px solid #bde7cf;
  color: #137a45;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 4px;
}

.form-error {
  background: #fdecec;
  border: 1px solid #f6c8c8;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 7px;
  padding: 9px 12px;
  margin-bottom: 12px;
}

.req {
  color: var(--red);
}

.m-input,
.m-select {
  width: 100%;
  border: 1px solid #e3e3e8;
  border-radius: 7px;
  padding: 7px 10px;
  font-size: 13px;
  color: #2c2c33;
  background: #fff;
  outline: none;
  font-family: inherit;
}

.m-input:focus,
.m-select:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.m-radio-row {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 26px;
}

.m-radio {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #2c2c33;
  cursor: pointer;
}

.m-radio input {
  width: 15px;
  height: 15px;
  accent-color: var(--red);
}

.m-hint {
  font-size: 12px;
  color: #dc2626;
  margin-top: 4px;
}

.m-section {
  border-top: 1px solid #ececef;
  margin-top: 14px;
  padding-top: 14px;
}

.m-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.m-section-head label {
  font-size: 12.5px;
  font-weight: 700;
  color: #3d3d45;
}

.m-empty {
  font-size: 13px;
  color: #6b7280;
}

.addr-view {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.addr-view li {
  border: 1px solid #ececef;
  border-radius: 9px;
  padding: 10px 12px;
  background: #fbfbfc;
}

.addr-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #1d1d23;
  margin-bottom: 4px;
}

.addr-line {
  font-size: 12.5px;
  color: #4a4a52;
  line-height: 1.6;
}

.addr-line b {
  color: #9a9aa3;
  font-weight: 600;
}

.addr-meta {
  font-size: 11.5px;
  color: #9a9aa3;
}

.m-actions {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  margin-top: 16px;
}

.m-btn {
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 7px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.m-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.m-btn.is-red {
  background: var(--red);
  color: #fff;
}

.m-btn.is-ghost {
  background: #6b7280;
  color: #fff;
}

</style>
