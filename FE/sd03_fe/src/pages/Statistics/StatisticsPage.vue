<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import PaginationBar from '@/components/PaginationBar.vue'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { listEmployees } from '@/api/employee'
import { API_BASE } from '@/api/http'
import { formatVnd } from '@/utils/format'
import { exportExcel } from '@/utils/export'
import { notifySuccess, notifyError } from '@/utils/notify'
import { fetchTongQuan, fetchTrangThaiDon } from '@/api/statistics'
import BarLineChart from '@/components/charts/BarLineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import {
  MAU_THUONG_HIEU,
  SAN_PHAM_MAU,
  layTangTruongLabel,
  khoaNgay,
  khoangNhanh,
  taoBangNgay,
  tinhBangThuongHieu,
  tinhChuKy,
  tinhChuoiBanHang,
  tinhKhoang,
  tinhNhanVien,
  tinhSanPham,
  tinhTrangThaiDon,
} from './statisticsData'

const pad2 = (x) => String(x).padStart(2, '0')
const toInputDate = (d) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
const docInputDate = (s) => {
  if (!s) return null
  const [y, m, d] = s.split('-').map(Number)
  if (!y || !m || !d) return null
  return new Date(y, m - 1, d)
}
const hienThiNgay = (s) => {
  const d = docInputDate(s)
  if (!d) return '—'
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`
}
const gioHienTai = (d = new Date()) =>
  `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`

const dauThang = () => {
  const n = new Date()
  return new Date(n.getFullYear(), n.getMonth(), 1)
}

/* ---------------------------------- Trạng thái bộ lọc ---------------------------------- */
const tuNgay = ref(toInputDate(dauThang()))
const denNgay = ref(toInputDate(new Date()))
const quickActive = ref('')
const kieuTongQuan = ref('ngay')
const thuongHieuChon = ref('')
const timThuongHieu = ref('')

const thuongHieus = ref([])
const nhanViens = ref([])
const sanPhams = ref([])
const bang = ref(new Map())
const capNhatLuc = ref(gioHienTai())

const tongQuanData = ref(null)
const trangThaiData = ref([])
const dangTai = ref(false)

const chartMode = ref('cot')
const thMode = ref('tron')
const thTheo = ref('doanhThu')

const kwNv = ref('')
const sortNv = ref('doanhThuCao')
const pageNv = ref(0)
const sizeNv = ref(5)

const kwSp = ref('')
const tonKhoSp = ref('')
const sortSp = ref('banChay')
const pageSp = ref(0)
const sizeSp = ref(5)

const QUICK = [
  { key: 'homNay', label: 'Hôm nay' },
  { key: '7ngay', label: '7 ngày qua' },
  { key: 'thangNay', label: 'Tháng này' },
  { key: 'thangTruoc', label: 'Tháng trước' },
  { key: 'namNay', label: 'Năm nay' },
]

const CHON_NHANH = (key) => {
  const k = khoangNhanh(key)
  tuNgay.value = toInputDate(k.tu)
  denNgay.value = toInputDate(k.den)
  quickActive.value = key
}

const onDateChange = () => {
  quickActive.value = ''
  const tu = docInputDate(tuNgay.value)
  const den = docInputDate(denNgay.value)
  if (tu && den && tu > den) {
    denNgay.value = tuNgay.value
  }
}

watch([tuNgay, denNgay], () => {
  if (!docInputDate(tuNgay.value) || !docInputDate(denNgay.value)) return
  const tu = docInputDate(tuNgay.value)
  const den = docInputDate(denNgay.value)
  if (tu && den && tu > den) denNgay.value = tuNgay.value
})

const datLaiBoLoc = () => {
  tuNgay.value = toInputDate(dauThang())
  denNgay.value = toInputDate(new Date())
  quickActive.value = ''
  kieuTongQuan.value = 'ngay'
  thuongHieuChon.value = ''
  timThuongHieu.value = ''
  kwNv.value = ''
  sortNv.value = 'doanhThuCao'
  pageNv.value = 0
  kwSp.value = ''
  tonKhoSp.value = ''
  sortSp.value = 'banChay'
  pageSp.value = 0
  notifySuccess('Đã đặt lại bộ lọc', 'Trở về mặc định đầu tháng')
}

/* ---------------------------------- Dữ liệu ---------------------------------- */
const tuDate = computed(() => docInputDate(tuNgay.value) || new Date())
const denDate = computed(() => docInputDate(denNgay.value) || new Date())

const sanPhamHienTai = computed(() =>
  sanPhams.value.length ? sanPhams.value : SAN_PHAM_MAU
)
const nhanVienHienTai = computed(() => nhanViens.value)

const dongSanPhamAll = computed(() =>
  tinhSanPham(sanPhamHienTai.value, tuDate.value, denDate.value)
)
const bangTH = computed(() => tinhBangThuongHieu(dongSanPhamAll.value))

const tyLeChon = computed(() => {
  if (!thuongHieuChon.value) return 1
  const tl = bangTH.value.tyLe[thuongHieuChon.value]
  return tl === undefined ? 0 : tl
})

const khoang = computed(() => {
  if (tongQuanData.value) {
    const t = tongQuanData.value
    return {
      doanhThu: t.doanhThu || 0,
      tienMat: t.tienMat || 0,
      tienChuyenKhoan: t.tienChuyenKhoan || 0,
      soDon: t.soDon || 0,
      aov: t.aov || 0,
      sanPhamBan: t.sanPhamBan || 0,
      khachMoi: t.khachMoi || 0,
      soTra: 0,
      soNgay: 0,
    }
  }
  return tinhKhoang(bang.value, tuDate.value, denDate.value, tyLeChon.value)
})

const chuoiBanHang = computed(() =>
  tinhChuoiBanHang(bang.value, tuDate.value, denDate.value, kieuTongQuan.value, tyLeChon.value)
)
const chartLabels = computed(() => chuoiBanHang.value.map((x) => x.nhan))
const chartValues = computed(() => chuoiBanHang.value.map((x) => x.giaTri))

const the = computed(() => (thTheo.value === 'soDon' ? 'soDon' : 'doanhThu'))

const slicesThuongHieu = computed(() => {
  const rows = [...bangTH.value.top]
  if (bangTH.value.khac.doanhThu > 0 || bangTH.value.khac.soDon > 0) rows.push(bangTH.value.khac)
  return rows
    .map((r) => ({
      label: r.ten,
      value: r[the.value],
      color: r.mau,
    }))
    .filter((s) => s.value > 0)
})

const giaTriTH = (r) => (the.value === 'soDon' ? `${new Intl.NumberFormat('vi-VN').format(r.soDon)} đơn` : formatVnd(r.doanhThu))

const danhSachTH = computed(() => {
  const rows = [...bangTH.value.top]
  rows.push(bangTH.value.khac)
  return rows
})

const chaoMauNgauNhien = (seed) => {
  const x = Math.sin(seed * 12.9898) * 43758.5453
  return x - Math.floor(x)
}

const seriesTH = computed(() =>
  bangTH.value.top.map((r, bi) => {
    const tl = bangTH.value.tong > 0 ? r.doanhThu / bangTH.value.tong : 0
    return {
      label: r.ten,
      color: r.mau,
      values: chuoiBanHang.value.map((c, i) =>
        Math.round(c.giaTri * tl * (0.6 + chaoMauNgauNhien((i + 3) * (bi + 2)) * 0.8))
      ),
    }
  })
)

const chuKy = computed(() => tinhChuKy(bang.value))
const trangThai = computed(() => {
  if (trangThaiData.value.length) {
    return trangThaiData.value.map((x) => ({ ...x, mau: mauTrangThai(x.ten) }))
  }
  return tinhTrangThaiDon(bang.value, tuDate.value, denDate.value, tyLeChon.value)
})
const slicesTrangThai = computed(() =>
  trangThai.value.map((x) => ({ label: x.ten, value: x.soLuong, color: x.mau }))
)

const mauTrangThai = (ten) => {
  const map = {
    'Chờ xác nhận': '#f43f5e',
    'Đã xác nhận': '#f59e0b',
    'Chờ giao hàng': '#eab308',
    'Đang giao hàng': '#14b8a6',
    'Đã giao hàng': '#3b82f6',
    'Đã hoàn thành': '#22c55e',
    'Đã hủy': '#ef4444',
    'Đã hoàn tiền': '#8b5cf6',
  }
  return map[ten] || '#94a3b8'
}

/* ------------------------------- Bảng doanh thu nhân viên ------------------------------- */
const nhanVienDayDu = computed(() => tinhNhanVien(nhanVienHienTai.value, khoang.value))

const nhanVienLoc = computed(() => {
  const kw = kwNv.value.trim().toLowerCase()
  let rows = nhanVienDayDu.value
  if (kw) {
    rows = rows.filter(
      (r) =>
        String(r.maNhanVien).toLowerCase().includes(kw) ||
        String(r.ten).toLowerCase().includes(kw)
    )
  }
  const sorted = [...rows]
  if (sortNv.value === 'doanhThuThap') sorted.sort((a, b) => a.doanhThu - b.doanhThu)
  else if (sortNv.value === 'donNhieu') sorted.sort((a, b) => b.don - a.don)
  else sorted.sort((a, b) => b.doanhThu - a.doanhThu)
  return sorted
})

const tongTrangNv = computed(() => Math.max(1, Math.ceil(nhanVienLoc.value.length / sizeNv.value)))
const dongNv = computed(() => {
  const start = pageNv.value * sizeNv.value
  return nhanVienLoc.value.slice(start, start + sizeNv.value)
})
watch(nhanVienLoc, () => {
  if (pageNv.value >= tongTrangNv.value) pageNv.value = 0
})

/* ---------------------------------- Bảng tổng quan sản phẩm ---------------------------------- */
const sanPhamLoc = computed(() => {
  const kw = kwSp.value.trim().toLowerCase()
  let rows = dongSanPhamAll.value
  if (thuongHieuChon.value) {
    rows = rows.filter((r) => r.tenThuongHieu === thuongHieuChon.value)
  }
  if (kw) {
    rows = rows.filter(
      (r) =>
        String(r.maSanPham).toLowerCase().includes(kw) ||
        String(r.tenSanPham).toLowerCase().includes(kw) ||
        String(r.tenThuongHieu).toLowerCase().includes(kw)
    )
  }
  if (tonKhoSp.value === 'het') rows = rows.filter((r) => r.tonKho === 0)
  else if (tonKhoSp.value === 'sapHet') rows = rows.filter((r) => r.tonKho > 0 && r.tonKho <= 50)
  else if (tonKhoSp.value === 'con') rows = rows.filter((r) => r.tonKho > 50)

  const sorted = [...rows]
  if (sortSp.value === 'doanhThu') sorted.sort((a, b) => b.doanhThu - a.doanhThu)
  else if (sortSp.value === 'tonKho') sorted.sort((a, b) => b.tonKho - a.tonKho)
  else sorted.sort((a, b) => b.daBan - a.daBan)
  return sorted
})

const tongTrangSp = computed(() => Math.max(1, Math.ceil(sanPhamLoc.value.length / sizeSp.value)))
const dongSp = computed(() => {
  const start = pageSp.value * sizeSp.value
  return sanPhamLoc.value.slice(start, start + sizeSp.value)
})
watch(sanPhamLoc, () => {
  if (pageSp.value >= tongTrangSp.value) pageSp.value = 0
})

const khoMauTon = (ton) => (ton === 0 ? 'is-empty' : ton <= 50 ? 'is-low' : 'is-ok')
const initialsOf = (name) => {
  if (!name) return 'NV'
  const parts = String(name).trim().split(/\s+/)
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}
const mauAvarta = (id) => {
  const colors = ['#cc0000', '#f59e0b', '#8b5cf6', '#14b8a6', '#3b82f6', '#22c55e', '#ec4899', '#6366f1']
  return colors[Math.abs(Number(id) || 0) % colors.length]
}

/* ---------------------------------- Phân trang ---------------------------------- */
const datTrangNv = (p) => {
  if (p < 0 || p >= tongTrangNv.value || p === pageNv.value) return
  pageNv.value = p
}
const datTrangSp = (p) => {
  if (p < 0 || p >= tongTrangSp.value || p === pageSp.value) return
  pageSp.value = p
}

/* ---------------------------------- Xuất Excel ---------------------------------- */
const xuatChuKy = async () => {
  const rows = chuKy.value.map((r, i) => [
    i + 1,
    r.nhan,
    r.doanhThu,
    r.soDon,
    r.aov,
    layTangTruongLabel(r.tangTruong),
  ])
  await exportExcel(
    'tong-quan-chu-ky-thoi-gian.xlsx',
    ['STT', 'Thời gian', 'Doanh thu', 'Số đơn', 'AOV (Trung bình/đơn)', 'Tăng trưởng'],
    rows,
    { sheetName: 'Chu kỳ thời gian' }
  )
  notifySuccess('Đã xuất Excel', 'Tệp Excel (.xlsx) đã được tải xuống')
}

const xuatNhanVien = async () => {
  const rows = nhanVienLoc.value.map((r, i) => [
    i + 1,
    r.maNhanVien,
    r.ten,
    r.vaiTro,
    r.don,
    r.spBan,
    r.doanhThu,
  ])
  await exportExcel(
    'tong-quan-doanh-thu-nhan-vien.xlsx',
    ['STT', 'Mã nhân viên', 'Nhân viên', 'Vai trò', 'Đơn hàng', 'Sản phẩm bán', 'Doanh thu'],
    rows,
    { sheetName: 'Doanh thu nhân viên' }
  )
  notifySuccess('Đã xuất Excel', 'Tệp Excel (.xlsx) đã được tải xuống')
}

const xuatSanPham = async () => {
  const rows = sanPhamLoc.value.map((r, i) => [
    i + 1,
    r.maSanPham,
    r.tenSanPham,
    r.tenThuongHieu,
    r.daBan,
    r.soTra,
    r.doanhThu,
    r.tonKho,
  ])
  await exportExcel(
    'tong-quan-san-pham.xlsx',
    ['STT', 'Mã sản phẩm', 'Tên sản phẩm', 'Thương hiệu', 'Đã bán', 'Số lượng trả', 'Doanh thu', 'Tồn kho'],
    rows,
    { sheetName: 'Sản phẩm' }
  )
  notifySuccess('Đã xuất Excel', 'Tệp Excel (.xlsx) đã được tải xuống')
}

const capNhatDoanhThu = async () => {
  await napThongKe()
  capNhatLuc.value = gioHienTai()
  notifySuccess('Đã cập nhật doanh thu', 'Dữ liệu thống kê đã được tải lại')
}

/* ---------------------------------- Nạp dữ liệu ---------------------------------- */
const napThongKe = async () => {
  if (!tuNgay.value || !denNgay.value) return
  dangTai.value = true
  try {
    const thuongHieu = thuongHieuChon.value || null
    const [tong, trangThai] = await Promise.all([
      fetchTongQuan(tuNgay.value, denNgay.value, thuongHieu),
      fetchTrangThaiDon(tuNgay.value, denNgay.value, thuongHieu),
    ])
    tongQuanData.value = tong
    trangThaiData.value = trangThai
    capNhatLuc.value = gioHienTai()
  } catch (e) {
    console.error('Lỗi tải thống kê:', e)
    tongQuanData.value = null
    trangThaiData.value = []
    notifyError('Không tải được dữ liệu thống kê')
  } finally {
    dangTai.value = false
  }
}

const locThuongHieu = computed(() => {
  const kw = timThuongHieu.value.trim().toLowerCase()
  if (!kw) return thuongHieus.value
  return thuongHieus.value.filter((t) => String(t).toLowerCase().includes(kw))
})

watch(locThuongHieu, (list) => {
  if (thuongHieuChon.value && !list.includes(thuongHieuChon.value)) {
    thuongHieuChon.value = ''
  }
})

const thuongHieuOptions = computed(() => [
  { value: '', label: 'Tất cả thương hiệu' },
  ...locThuongHieu.value.map((t) => ({ value: t, label: t }))
])

const onThuongHieuChange = (val) => {
  thuongHieuChon.value = val ?? ''
}

const onTonKhoSpChange = (val) => {
  tonKhoSp.value = val ?? ''
  pageSp.value = 0
}

watch([tuNgay, denNgay, thuongHieuChon], () => {
  if (!docInputDate(tuNgay.value) || !docInputDate(denNgay.value)) return
  napThongKe()
})

onMounted(async () => {
  bang.value = taoBangNgay()
  capNhatLuc.value = gioHienTai()
  napThongKe()

  try {
    const d = await listEmployees({ page: 0, size: 500 })
    nhanViens.value = d.content || []
  } catch {
    nhanViens.value = []
  }

  try {
    const res = await fetch(`${API_BASE}/attributes/all-filter-options`)
    if (res.ok) {
      const data = await res.json()
      thuongHieus.value = (data.thuongHieu || []).map((t) => t.ten || t).filter(Boolean)
    }
  } catch {
    /* fallback phía dưới */
  }
  if (!thuongHieus.value.length) {
    thuongHieus.value = MAU_THUONG_HIEU.map((t) => t.ten)
  }

  try {
    const res = await fetch(`${API_BASE}/san-pham?page=0&size=500`)
    if (res.ok) {
      const data = await res.json()
      sanPhams.value = data.content || []
    }
  } catch {
    sanPhams.value = []
  }
})
</script>

<template>
  <div class="statistics-page-container">
    <!-- CARD 1: BỘ LỌC TỔNG QUAN -->
    <div class="filter-card">
      <div class="card-title-row space-between">
        <div class="title-with-icon">
          <div class="icon-circle icon-filter">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
            </svg>
          </div>
          <span class="card-title-text">Bộ lọc tổng quan</span>
        </div>

        <div class="quick-range-group">
          <button
            v-for="q in QUICK"
            :key="q.key"
            type="button"
            class="quick-range-btn"
            :class="{ 'is-active': quickActive === q.key }"
            @click="CHON_NHANH(q.key)"
          >
            {{ q.label }}
          </button>
        </div>
      </div>

      <div class="filter-grid">
        <div class="field-block">
          <label class="field-label">Từ ngày</label>
          <input v-model="tuNgay" type="date" class="input-date" @change="onDateChange" />
        </div>
        <div class="field-block">
          <label class="field-label">Đến ngày</label>
          <input v-model="denNgay" type="date" class="input-date" @change="onDateChange" />
        </div>
        <div class="field-block">
          <label class="field-label">Tổng quan</label>
          <SearchableSelect
            v-model="kieuTongQuan"
            :options="[
              { value: 'ngay', label: 'Theo ngày' },
              { value: 'tuan', label: 'Theo tuần' },
              { value: 'thang', label: 'Theo tháng' }
            ]"
            label-key="label"
            value-key="value"
            :allow-clear="false"
            placeholder="Theo ngày"
          />
        </div>
        <div class="field-block">
          <label class="field-label">Thương hiệu</label>
          <SearchableSelect
            v-model="thuongHieuChon"
            :options="thuongHieuOptions"
            label-key="label"
            value-key="value"
            placeholder="Tất cả thương hiệu"
            search-placeholder="Tìm thương hiệu..."
            @change="onThuongHieuChange"
          />
        </div>
        <div class="field-block">
          <label class="field-label">Tìm theo thương hiệu</label>
          <div class="search-input-wrap">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input v-model="timThuongHieu" type="text" class="input-search" placeholder="Nhập tên thương hiệu..." />
          </div>
        </div>
        <div class="field-block field-reset">
          <label class="field-label">&nbsp;</label>
          <button class="btn-action-outline" type="button" @click="datLaiBoLoc">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
            </svg>
            Reset
          </button>
        </div>
      </div>
    </div>

    <!-- THANH THÔNG TIN DỮ LIỆU -->
    <div class="info-strip">
      <div class="info-left">
        <span class="info-dot"></span>
        <span class="info-text">
          Đang hiển thị dữ liệu tổng quan từ ngày <strong>{{ hienThiNgay(tuNgay) }}</strong> đến ngày
          <strong>{{ hienThiNgay(denNgay) }}</strong>
        </span>
      </div>
      <div class="info-right">
        <span class="info-meta">Cập nhật lúc {{ capNhatLuc }} · Tự động lúc 22:00</span>
        <button class="btn-action-outline btn-refresh" type="button" @click="capNhatDoanhThu">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
          </svg>
          Cập nhật doanh thu
        </button>
      </div>
    </div>

    <!-- 3 THẺ SỐ LIỆU LỚN -->
    <div class="stat-grid stat-grid-3">
      <div class="stat-card">
        <div class="stat-icon is-emerald">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
        </div>
        <div class="stat-value">{{ formatVnd(khoang.doanhThu) }}</div>
        <div class="stat-label">Tổng doanh thu</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-green">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="6" width="20" height="12" rx="2"></rect>
            <circle cx="12" cy="12" r="2"></circle>
            <path d="M6 12h.01M18 12h.01"></path>
          </svg>
        </div>
        <div class="stat-value">{{ formatVnd(khoang.tienMat) }}</div>
        <div class="stat-label">Tổng tiền mặt</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-blue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="5" width="20" height="14" rx="2"></rect>
            <line x1="2" y1="10" x2="22" y2="10"></line>
          </svg>
        </div>
        <div class="stat-value">{{ formatVnd(khoang.tienChuyenKhoan) }}</div>
        <div class="stat-label">Tổng tiền chuyển khoản</div>
      </div>
    </div>

    <!-- 4 THẺ SỐ LIỆU NHỎ -->
    <div class="stat-grid stat-grid-4">
      <div class="stat-card">
        <div class="stat-icon is-cyan">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1"></circle>
            <circle cx="20" cy="21" r="1"></circle>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
          </svg>
        </div>
        <div class="stat-value">{{ new Intl.NumberFormat('vi-VN').format(khoang.soDon) }}</div>
        <div class="stat-label">Tổng đơn hàng</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-indigo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>
        </div>
        <div class="stat-value">{{ formatVnd(khoang.aov) }}</div>
        <div class="stat-label">Giá trị trung bình đơn (AOV)</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-amber">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
            <line x1="12" y1="22.08" x2="12" y2="12"></line>
          </svg>
        </div>
        <div class="stat-value">{{ new Intl.NumberFormat('vi-VN').format(khoang.sanPhamBan) }}</div>
        <div class="stat-label">Sản phẩm đã bán</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon is-rose">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="8.5" cy="7" r="4"></circle>
            <line x1="20" y1="8" x2="20" y2="14"></line>
            <line x1="23" y1="11" x2="17" y2="11"></line>
          </svg>
        </div>
        <div class="stat-value">{{ new Intl.NumberFormat('vi-VN').format(khoang.khachMoi) }}</div>
        <div class="stat-label">Khách mới</div>
      </div>
    </div>

    <!-- HÀNG BIỂU ĐỒ -->
    <div class="charts-grid">
      <div class="filter-card chart-card">
        <div class="card-title-row space-between">
          <div class="title-with-icon">
            <div class="icon-circle icon-chart">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="20" x2="18" y2="10"></line>
                <line x1="12" y1="20" x2="12" y2="4"></line>
                <line x1="6" y1="20" x2="6" y2="14"></line>
              </svg>
            </div>
            <span class="card-title-text">Sản phẩm bán được theo ngày</span>
          </div>
          <div class="seg-toggle">
            <button type="button" class="seg-btn" :class="{ 'is-active': chartMode === 'cot' }" @click="chartMode = 'cot'">Cột</button>
            <button type="button" class="seg-btn" :class="{ 'is-active': chartMode === 'duong' }" @click="chartMode = 'duong'">Đường</button>
          </div>
        </div>
        <BarLineChart :labels="chartLabels" :values="chartValues" :mode="chartMode" color="#d92d20" :height="270" />
      </div>

      <div class="filter-card chart-card">
        <div class="card-title-row space-between">
          <div class="title-with-icon">
            <div class="icon-circle icon-brand">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M12 2a10 10 0 0 1 10 10h-10z"></path>
              </svg>
            </div>
            <span class="card-title-text">Thương hiệu</span>
          </div>
          <div class="brand-controls">
            <div class="seg-toggle">
              <button type="button" class="seg-btn" :class="{ 'is-active': thMode === 'tron' }" @click="thMode = 'tron'">Tròn</button>
              <button type="button" class="seg-btn" :class="{ 'is-active': thMode === 'duong' }" @click="thMode = 'duong'">Đường</button>
            </div>
            <SearchableSelect
              v-model="thTheo"
              :options="[
                { value: 'doanhThu', label: 'Theo doanh thu' },
                { value: 'soDon', label: 'Theo số đơn' }
              ]"
              label-key="label"
              value-key="value"
              :allow-clear="false"
              placeholder="Theo doanh thu"
            />
          </div>
        </div>

        <template v-if="thMode === 'tron'">
          <PieChart :slices="slicesThuongHieu" :size="200" />
          <div class="brand-list">
            <div v-for="r in danhSachTH" :key="r.ten" class="brand-row">
              <span class="brand-dot" :style="{ background: r.mau }"></span>
              <span class="brand-name">{{ r.ten }}</span>
              <span class="brand-value">{{ giaTriTH(r) }}</span>
            </div>
          </div>
        </template>
        <template v-else>
          <BarLineChart :labels="chartLabels" :series="seriesTH" mode="duong" :height="300" />
        </template>
      </div>
    </div>

    <!-- HÀNG: CHU KỲ + TRẠNG THÁI ĐƠN -->
    <div class="charts-grid">
      <div class="filter-card chart-card">
        <div class="card-title-row space-between">
          <div class="title-with-icon">
            <div class="icon-circle icon-cycle">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2"></rect>
                <line x1="16" y1="2" x2="16" y2="6"></line>
                <line x1="8" y1="2" x2="8" y2="6"></line>
                <line x1="3" y1="10" x2="21" y2="10"></line>
              </svg>
            </div>
            <span class="card-title-text">Tổng quan theo chu kỳ thời gian</span>
          </div>
          <button class="btn-action-outline" type="button" @click="xuatChuKy">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
            Xuất Excel
          </button>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th class="text-right">Doanh thu</th>
                <th class="text-center">Số đơn</th>
                <th class="text-right">AOV (Trung bình/đơn)</th>
                <th class="text-center">Tăng trưởng</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in chuKy" :key="r.nhan" class="row-alt">
                <td class="cell-strong">{{ r.nhan }}</td>
                <td class="text-right">{{ formatVnd(r.doanhThu) }}</td>
                <td class="text-center">{{ new Intl.NumberFormat('vi-VN').format(r.soDon) }}</td>
                <td class="text-right">{{ formatVnd(r.aov) }}</td>
                <td class="text-center">
                  <span class="growth-pill" :class="r.tangTruong >= 0 ? 'is-up' : 'is-down'">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                      <template v-if="r.tangTruong >= 0">
                        <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                        <polyline points="17 6 23 6 23 12"></polyline>
                      </template>
                      <template v-else>
                        <polyline points="23 18 13.5 8.5 8.5 13.5 1 6"></polyline>
                        <polyline points="17 18 23 18 23 12"></polyline>
                      </template>
                    </svg>
                    {{ layTangTruongLabel(r.tangTruong) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="filter-card chart-card">
        <div class="card-title-row">
          <div class="icon-circle icon-status">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
            </svg>
          </div>
          <span class="card-title-text">Cơ cấu trạng thái đơn hàng</span>
        </div>

        <PieChart :slices="slicesTrangThai" :size="195" />
        <div class="status-legend">
          <div v-for="s in slicesTrangThai" :key="s.label" class="status-row">
            <span class="brand-dot" :style="{ background: mauTrangThai(s.label) }"></span>
            <span class="status-name">{{ s.label }}</span>
            <span class="status-count">{{ new Intl.NumberFormat('vi-VN').format(s.value) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- TỔNG QUAN DOANH THU NHÂN VIÊN -->
    <div class="table-card">
      <div class="card-title-row space-between">
        <div class="title-with-icon">
          <div class="icon-circle icon-users">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
          </div>
          <span class="card-title-text">Tổng quan doanh thu nhân viên</span>
          <span class="count-pill">{{ nhanVienLoc.length }} nhân viên</span>
        </div>
        <button class="btn-action-outline" type="button" @click="xuatNhanVien">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
          Xuất Excel
        </button>
      </div>

      <div class="filter-controls-row">
        <div class="field-block grow">
          <label class="field-label">Tìm nhân viên</label>
          <div class="search-input-wrap">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input v-model="kwNv" type="text" class="input-search" placeholder="Mã hoặc tên nhân viên" @input="pageNv = 0" />
          </div>
        </div>
        <div class="field-block">
          <label class="field-label">Sắp xếp</label>
          <SearchableSelect
            v-model="sortNv"
            :options="[
              { value: 'doanhThuCao', label: 'Doanh thu cao nhất' },
              { value: 'doanhThuThap', label: 'Doanh thu thấp nhất' },
              { value: 'donNhieu', label: 'Đơn hàng nhiều nhất' }
            ]"
            label-key="label"
            value-key="value"
            :allow-clear="false"
            placeholder="Doanh thu cao nhất"
          />
        </div>
        <div class="field-block field-reset">
          <label class="field-label">&nbsp;</label>
          <button class="btn-action-outline" type="button" @click="kwNv = ''; sortNv = 'doanhThuCao'; pageNv = 0">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
            </svg>
            Reset
          </button>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px">STT</th>
              <th style="width: 120px">Mã nhân viên</th>
              <th>Nhân viên</th>
              <th class="text-center" style="width: 110px">Đơn hàng</th>
              <th class="text-center" style="width: 130px">Sản phẩm bán</th>
              <th class="text-right" style="width: 150px">Doanh thu</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="nhanVienLoc.length === 0">
              <td colspan="6" class="text-center empty-row">Không tìm thấy nhân viên nào</td>
            </tr>
            <tr v-for="(r, i) in dongNv" :key="r.id ?? r.maNhanVien" class="row-alt">
              <td class="cell-stt">{{ pageNv * sizeNv + i + 1 }}</td>
              <td class="cell-code"><span class="code-chip">{{ r.maNhanVien }}</span></td>
              <td>
                <div class="entity-cell">
                  <span class="avatar-dot" :style="{ background: mauAvarta(r.id) }">{{ initialsOf(r.ten) }}</span>
                  <div class="entity-meta">
                    <span class="entity-name">{{ r.ten }}</span>
                    <span class="entity-sub">{{ r.vaiTro || 'Nhân viên bán hàng' }}</span>
                  </div>
                </div>
              </td>
              <td class="text-center">{{ new Intl.NumberFormat('vi-VN').format(r.don) }}</td>
              <td class="text-center">{{ new Intl.NumberFormat('vi-VN').format(r.spBan) }}</td>
              <td class="text-right cell-money">{{ formatVnd(r.doanhThu) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar
        :page="pageNv"
        :total-pages="tongTrangNv"
        :total-elements="nhanVienLoc.length"
        :page-size="sizeNv"
        :from="dongNv.length ? pageNv * sizeNv + 1 : 0"
        :to="pageNv * sizeNv + dongNv.length"
        :sizes="[5, 10, 20]"
        label="nhân viên"
        @change="datTrangNv"
        @size-change="(s) => { sizeNv = s; pageNv = 0 }"
      />
    </div>

    <!-- TỔNG QUAN SẢN PHẨM -->
    <div class="table-card">
      <div class="card-title-row space-between">
        <div class="title-with-icon">
          <div class="icon-circle icon-product">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
              <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
              <line x1="12" y1="22.08" x2="12" y2="12"></line>
            </svg>
          </div>
          <span class="card-title-text">Tổng quan sản phẩm</span>
          <span class="count-pill">{{ sanPhamLoc.length }} sản phẩm</span>
        </div>
        <button class="btn-action-outline" type="button" @click="xuatSanPham">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
          Xuất Excel
        </button>
      </div>

      <div class="filter-controls-row">
        <div class="field-block grow">
          <label class="field-label">Lọc trong bảng</label>
          <div class="search-input-wrap">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input v-model="kwSp" type="text" class="input-search" placeholder="Mã, tên hoặc thương hiệu" @input="pageSp = 0" />
          </div>
        </div>
        <div class="field-block">
          <label class="field-label">Tồn kho</label>
          <SearchableSelect
            v-model="tonKhoSp"
            :options="[
              { value: '', label: 'Tất cả tồn kho' },
              { value: 'con', label: 'Còn hàng' },
              { value: 'sapHet', label: 'Sắp hết hàng' },
              { value: 'het', label: 'Hết hàng' }
            ]"
            label-key="label"
            value-key="value"
            placeholder="Tất cả tồn kho"
            search-placeholder="Tìm trạng thái tồn..."
            @change="onTonKhoSpChange"
          />
        </div>
        <div class="field-block">
          <label class="field-label">Sắp xếp</label>
          <SearchableSelect
            v-model="sortSp"
            :options="[
              { value: 'banChay', label: 'Bán chạy nhất' },
              { value: 'doanhThu', label: 'Doanh thu cao nhất' },
              { value: 'tonKho', label: 'Tồn kho cao nhất' }
            ]"
            label-key="label"
            value-key="value"
            :allow-clear="false"
            placeholder="Bán chạy nhất"
            @change="pageSp = 0"
          />
        </div>
        <div class="field-block field-reset">
          <label class="field-label">&nbsp;</label>
          <button class="btn-action-outline" type="button" @click="kwSp = ''; tonKhoSp = ''; sortSp = 'banChay'; pageSp = 0">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
            </svg>
            Reset
          </button>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px">STT</th>
              <th style="width: 110px">Mã sản phẩm</th>
              <th>Tên sản phẩm</th>
              <th style="width: 140px">Thương hiệu</th>
              <th class="text-center" style="width: 90px">Đã bán</th>
              <th class="text-center" style="width: 110px">Số lượng trả</th>
              <th class="text-right" style="width: 140px">Doanh thu</th>
              <th class="text-center" style="width: 95px">Tồn kho</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="sanPhamLoc.length === 0">
              <td colspan="8" class="text-center empty-row">Không tìm thấy sản phẩm nào</td>
            </tr>
            <tr v-for="(r, i) in dongSp" :key="r.maSanPham" class="row-alt">
              <td class="cell-stt">{{ pageSp * sizeSp + i + 1 }}</td>
              <td class="cell-code">{{ r.maSanPham }}</td>
              <td>
                <div class="entity-cell">
                  <span class="avatar-square">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                    </svg>
                  </span>
                  <div class="entity-meta">
                    <span class="entity-name">{{ r.tenSanPham }}</span>
                    <span class="entity-sub">Tổng dữ liệu</span>
                  </div>
                </div>
              </td>
              <td><span class="brand-chip">{{ r.tenThuongHieu }}</span></td>
              <td class="text-center">{{ new Intl.NumberFormat('vi-VN').format(r.daBan) }}</td>
              <td class="text-center" :class="{ 'is-return': r.soTra > 0 }">{{ new Intl.NumberFormat('vi-VN').format(r.soTra) }}</td>
              <td class="text-right cell-money">{{ formatVnd(r.doanhThu) }}</td>
              <td class="text-center">
                <span class="stock-badge" :class="khoMauTon(r.tonKho)">{{ new Intl.NumberFormat('vi-VN').format(r.tonKho) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar
        :page="pageSp"
        :total-pages="tongTrangSp"
        :total-elements="sanPhamLoc.length"
        :page-size="sizeSp"
        :from="dongSp.length ? pageSp * sizeSp + 1 : 0"
        :to="pageSp * sizeSp + dongSp.length"
        :sizes="[5, 10, 20]"
        label="sản phẩm"
        @change="datTrangSp"
        @size-change="(s) => { sizeSp = s; pageSp = 0 }"
      />
    </div>
  </div>
</template>

<style scoped>
.statistics-page-container {
  padding: 16px 20px 40px;
  background-color: #f8fafc;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: inherit;
}

.filter-card,
.table-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.card-title-row.space-between {
  justify-content: space-between;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.icon-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-circle svg {
  width: 16px;
  height: 16px;
}

.icon-filter {
  background: #f1f5f9;
  color: #475569;
}

.icon-chart,
.icon-cycle {
  background: #ffe4e6;
  color: var(--primary);
}

.icon-brand,
.icon-status {
  background: #fff1f2;
  color: var(--primary-hover);
}

.icon-users {
  background: #ecfdf5;
  color: #10b981;
}

.icon-product {
  background: #fffbeb;
  color: #f59e0b;
}

.card-title-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.count-pill {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  margin-left: 4px;
}

/* Nút khoảng thời gian nhanh */
.quick-range-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-range-btn {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.quick-range-btn:hover {
  border-color: #fecdd3;
  color: var(--primary-hover);
}

.quick-range-btn.is-active {
  background: #fff1f2;
  border-color: var(--primary);
  color: var(--primary-hover);
}

/* Ô lọc */
.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
  align-items: end;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.field-block.grow {
  flex: 1;
  min-width: 260px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.input-date {
  height: 42px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
  font-family: inherit;
  width: 100%;
}

.input-date:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.search-icon {
  position: absolute;
  left: 14px;
  width: 16px;
  height: 16px;
  color: #94a3b8;
  pointer-events: none;
}

.input-search {
  width: 100%;
  height: 42px;
  padding: 0 16px 0 42px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
}

.input-search:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.input-search::placeholder {
  color: #94a3b8;
}

.field-reset {
  align-self: end;
}

.btn-action-outline {
  height: 42px;
  padding: 0 16px;
  border: 1px solid #ffe4e6;
  border-radius: 8px;
  background: #ffffff;
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-action-outline svg {
  width: 14px;
  height: 14px;
}

.btn-action-outline:hover {
  background: #fff1f2;
  border-color: #fecdd3;
  color: var(--primary-hover);
}

/* Thanh thông tin */
.info-strip {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.info-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.info-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.14);
  flex-shrink: 0;
}

.info-text {
  font-size: 13.5px;
  color: #334155;
}

.info-text strong {
  color: #0f172a;
}

.info-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.info-meta {
  font-size: 12.5px;
  color: #94a3b8;
}

.btn-refresh {
  height: 36px;
}

/* Thẻ số liệu */
.stat-grid {
  display: grid;
  gap: 18px;
}

.stat-grid-3 {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.stat-grid-4 {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.stat-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 22px 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.stat-icon svg {
  width: 19px;
  height: 19px;
}

.stat-icon.is-emerald {
  background: #ecfdf5;
  color: #10b981;
}

.stat-icon.is-green {
  background: #f0fdf4;
  color: #22c55e;
}

.stat-icon.is-blue {
  background: #eff6ff;
  color: #3b82f6;
}

.stat-icon.is-cyan {
  background: #ecfeff;
  color: #06b6d4;
}

.stat-icon.is-indigo {
  background: #eef2ff;
  color: #6366f1;
}

.stat-icon.is-amber {
  background: #fffbeb;
  color: #f59e0b;
}

.stat-icon.is-rose {
  background: #fff1f2;
  color: var(--primary);
}

.stat-value {
  font-size: 27px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
  line-height: 1.15;
}

.stat-label {
  font-size: 13.5px;
  color: #64748b;
  font-weight: 500;
}

/* Biểu đồ */
.charts-grid {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 20px;
  align-items: start;
}

.chart-card {
  min-width: 0;
}

.seg-toggle {
  display: inline-flex;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}

.seg-btn {
  border: none;
  background: transparent;
  padding: 6px 14px;
  font-size: 12.5px;
  font-weight: 600;
  color: #64748b;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.seg-btn.is-active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.brand-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.brand-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 4px;
  border-bottom: 1px solid #f8fafc;
  font-size: 13.5px;
}

.brand-row:last-child {
  border-bottom: none;
}

.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.brand-name {
  flex: 1;
  color: #334155;
  font-weight: 500;
}

.brand-value {
  color: #0f172a;
  font-weight: 700;
}

.status-legend {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 14px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 2px;
  font-size: 13px;
}

.status-name {
  flex: 1;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-count {
  font-weight: 700;
  color: #0f172a;
}

/* Bảng */
.filter-controls-row {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.table-container {
  overflow-x: auto;
  width: 100%;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th {
  padding: 13px 12px;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  white-space: nowrap;
}

.data-table td {
  padding: 13px 12px;
  font-size: 13.5px;
  color: #334155;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background-color: #fafbfc;
}

.row-alt:nth-child(even) {
  background: #fafbfc;
}

.row-alt:nth-child(even):hover {
  background: #f5f7fa;
}

.text-right {
  text-align: right;
}

.text-center {
  text-align: center;
}

.cell-stt {
  color: #475569;
  font-weight: 500;
}

.cell-strong {
  font-weight: 700;
  color: #0f172a;
}

.cell-code {
  font-weight: 600;
  color: #0f172a;
}

.cell-money {
  font-weight: 700;
  color: #0f172a;
}

.empty-row {
  color: #94a3b8;
  padding: 28px 12px !important;
}

.code-chip {
  display: inline-flex;
  padding: 3px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.entity-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.avatar-dot {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #ffffff;
  font-size: 11.5px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  text-transform: uppercase;
}

.avatar-square {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: #f1f5f9;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-square svg {
  width: 17px;
  height: 17px;
}

.entity-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.entity-name {
  font-weight: 600;
  color: #0f172a;
  font-size: 13.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.entity-sub {
  font-size: 12px;
  color: #94a3b8;
}

.brand-chip {
  display: inline-flex;
  padding: 3px 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 20px;
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.stock-badge {
  display: inline-flex;
  min-width: 44px;
  justify-content: center;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12.5px;
  font-weight: 700;
}

.stock-badge.is-ok {
  background: #ecfdf5;
  color: #10b981;
  border: 1px solid #d1fae5;
}

.stock-badge.is-low {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.stock-badge.is-empty {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.is-return {
  color: #dc2626;
  font-weight: 600;
}

/* Tăng trưởng */
.growth-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 11px;
  border-radius: 20px;
  font-size: 12.5px;
  font-weight: 700;
}

.growth-pill svg {
  width: 13px;
  height: 13px;
}

.growth-pill.is-up {
  background: #ecfdf5;
  color: #10b981;
}

.growth-pill.is-down {
  background: #fef2f2;
  color: #ef4444;
}

@media (max-width: 1199px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .stat-value {
    font-size: 23px;
  }
}

@media (max-width: 767px) {
  .statistics-page-container {
    padding: 14px 12px 32px;
  }

  .filter-card,
  .table-card {
    padding: 16px 14px;
  }

  .card-title-row {
    flex-wrap: wrap;
  }

  .quick-range-group {
    width: 100%;
  }

  .filter-controls-row {
    flex-direction: column;
    align-items: stretch;
  }

  .field-block.grow {
    min-width: 0;
  }
}
</style>
