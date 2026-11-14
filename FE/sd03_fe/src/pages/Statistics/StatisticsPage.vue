<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import http, { API_BASE } from '@/api/http'
import { formatVnd } from '@/utils/format'
import { exportExcel } from '@/utils/export'
import { notifySuccess, notifyError } from '@/utils/notify'
import {
  fetchTongQuan,
  fetchTrangThaiDon,
  fetchSanPhamTheoNgay,
  fetchThuongHieuThongKe,
  fetchSanPhamBang,
  fetchDonHangChiTiet,
  fetchSanPhamDaBan,
  fetchEmailMacDinh,
  guiMailTongKet,
  fetchSoSanh
} from '@/api/statistics'
import BarLineChart from '@/components/charts/BarLineChart.vue'

const router = useRouter()

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
const formatDateTime = (isoStr) => {
  if (!isoStr) return '—'
  const d = new Date(isoStr)
  if (isNaN(d.getTime())) return isoStr
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

const dauThang = () => {
  const n = new Date()
  return new Date(n.getFullYear(), n.getMonth(), 1)
}

/* ---------------------------------- Trạng thái bộ lọc chính ---------------------------------- */
const tuNgay = ref(toInputDate(new Date()))
const denNgay = ref(toInputDate(new Date()))
const quickActive = ref('homNay')
const kieuThongKe = ref('ngay')
const thuongHieuChon = ref('')
const timSanPham = ref('')
const thuongHieus = ref([])

const dangTai = ref(false)
const tongQuanData = ref({
  doanhThu: 0,
  tienMat: 0,
  tienChuyenKhoan: 0,
  soDon: 0,
  aov: 0,
  sanPhamBan: 0,
  khachMoi: 0,
  doanhThuDuKien: 0
})

/* ---------------------------------- Thẻ KPI & Khung chi tiết động ---------------------------------- */
const activeKpi = ref('TONG_DON_HANG') // Mặc định mở Tổng đơn hàng giống ảnh 1
const subTabOrder = ref('ALL') // 'ALL' | 'TAI_QUAY' | 'ONLINE' hoặc 'TIEN_MAT' | 'CHUYEN_KHOAN'
const kwDonHang = ref('')
const filterTrangThaiDon = ref('')
const filterThanhToanDon = ref('')
const sortDonHang = ref('moiNhat')
const pageDon = ref(0)
const sizeDon = ref(5)

const donHangList = ref([])
const sanPhamDaBanList = ref([])
const dangTaiChiTiet = ref(false)

const toggleKpi = (key) => {
  if (activeKpi.value === key) {
    activeKpi.value = null
  } else {
    activeKpi.value = key
    subTabOrder.value = 'ALL'
    pageDon.value = 0
    kwDonHang.value = ''
    filterTrangThaiDon.value = ''
    filterThanhToanDon.value = ''
    taiChiTietKpi()
  }
}

const taiChiTietKpi = async () => {
  if (!activeKpi.value) return
  dangTaiChiTiet.value = true
  try {
    if (activeKpi.value === 'SAN_PHAM_DA_BAN') {
      const res = await fetchSanPhamDaBan(tuNgay.value, denNgay.value, thuongHieuChon.value || null, timSanPham.value || null)
      sanPhamDaBanList.value = res || []
    } else {
      let kpiType = 'ALL_VALID'
      if (activeKpi.value === 'DOANH_THU_THUC_TE') kpiType = 'DOANH_THU_THUC_TE'
      else if (activeKpi.value === 'DOANH_THU_DU_KIEN') kpiType = 'DOANH_THU_DU_KIEN'
      else if (activeKpi.value === 'TIEN_MAT_CHUYEN_KHOAN') kpiType = 'ALL_VALID'
      else if (activeKpi.value === 'TONG_DON_HANG') kpiType = 'ALL_VALID'

      const params = {
        tu: tuNgay.value,
        den: denNgay.value,
        kpiType,
        thuongHieu: thuongHieuChon.value || null
      }
      const res = await fetchDonHangChiTiet(params)
      donHangList.value = res || []
    }
  } catch (e) {
    console.error('Lỗi tải chi tiết KPI:', e)
  } finally {
    dangTaiChiTiet.value = false
  }
}

/* Bộ lọc client cho danh sách đơn hàng chi tiết */
const donHangLoc = computed(() => {
  let list = [...donHangList.value]

  // Lọc theo sub-tab
  if (activeKpi.value === 'TIEN_MAT_CHUYEN_KHOAN') {
    if (subTabOrder.value === 'TIEN_MAT') {
      list = list.filter((d) => d.hinhThucThanhToan === 'Tiền mặt')
    } else if (subTabOrder.value === 'CHUYEN_KHOAN') {
      list = list.filter((d) => d.hinhThucThanhToan === 'Chuyển khoản')
    }
  } else {
    if (subTabOrder.value === 'TAI_QUAY') {
      list = list.filter((d) => String(d.loaiDon || '').toUpperCase().includes('TAI_QUAY') || String(d.loaiDon || '').includes('Tại quầy'))
    } else if (subTabOrder.value === 'ONLINE') {
      list = list.filter((d) => String(d.loaiDon || '').toUpperCase().includes('ONLINE'))
    }
  }

  // Lọc từ khóa
  const kw = kwDonHang.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      (d) =>
        String(d.maHoaDon || '').toLowerCase().includes(kw) ||
        String(d.tenKhachHang || '').toLowerCase().includes(kw) ||
        String(d.soDienThoaiKhachHang || '').toLowerCase().includes(kw) ||
        String(d.tenNhanVien || '').toLowerCase().includes(kw)
    )
  }

  // Lọc trạng thái
  if (filterTrangThaiDon.value) {
    list = list.filter((d) => d.trangThai === filterTrangThaiDon.value)
  }

  // Lọc thanh toán
  if (filterThanhToanDon.value) {
    list = list.filter((d) => d.trangThaiThanhToan === filterThanhToanDon.value)
  }

  // Sắp xếp
  if (sortDonHang.value === 'cuNhat') {
    list.sort((a, b) => new Date(a.ngayTao) - new Date(b.ngayTao))
  } else if (sortDonHang.value === 'tienCao') {
    list.sort((a, b) => (Number(b.thanhTien) || 0) - (Number(a.thanhTien) || 0))
  } else if (sortDonHang.value === 'tienThap') {
    list.sort((a, b) => (Number(a.thanhTien) || 0) - (Number(b.thanhTien) || 0))
  } else {
    list.sort((a, b) => new Date(b.ngayTao) - new Date(a.ngayTao))
  }

  return list
})

const countDonTaiQuay = computed(() =>
  donHangList.value.filter((d) => String(d.loaiDon || '').toUpperCase().includes('TAI_QUAY') || String(d.loaiDon || '').includes('Tại quầy')).length
)
const countDonOnline = computed(() =>
  donHangList.value.filter((d) => String(d.loaiDon || '').toUpperCase().includes('ONLINE')).length
)
const countDonTienMat = computed(() =>
  donHangList.value.filter((d) => d.hinhThucThanhToan === 'Tiền mặt').length
)
const countDonChuyenKhoan = computed(() =>
  donHangList.value.filter((d) => d.hinhThucThanhToan === 'Chuyển khoản').length
)

const tongTrangDon = computed(() => Math.max(1, Math.ceil(donHangLoc.value.length / sizeDon.value)))
const donHangTrang = computed(() => {
  const start = pageDon.value * sizeDon.value
  return donHangLoc.value.slice(start, start + sizeDon.value)
})

const sanPhamDaBanLoc = computed(() => {
  const kw = kwDonHang.value.trim().toLowerCase()
  let list = [...sanPhamDaBanList.value]
  if (kw) {
    list = list.filter((item) =>
      String(item.maSanPham || '').toLowerCase().includes(kw) ||
      String(item.tenSanPham || '').toLowerCase().includes(kw) ||
      String(item.maHoaDon || '').toLowerCase().includes(kw)
    )
  }
  return list
})
const tongTrangSpBan = computed(() => Math.max(1, Math.ceil(sanPhamDaBanLoc.value.length / sizeDon.value)))
const sanPhamDaBanTrang = computed(() => {
  const start = pageDon.value * sizeDon.value
  return sanPhamDaBanLoc.value.slice(start, start + sizeDon.value)
})

const xemChiTietDon = (id) => {
  if (id) router.push(`/hoa-don/${id}`)
}

/* ---------------------------------- Phần giữa: Biểu đồ sản phẩm bán theo ngày (thực tế từ HĐ) ---------------------------------- */
const spTheoNgayList = ref([])
const chartField = ref('soLuongBan') // 'soLuongBan' | 'doanhThu'

const chartLabels = computed(() => spTheoNgayList.value.map((item) => item.nhanNgay || item.ngay))
const chartValues = computed(() => spTheoNgayList.value.map((item) => item[chartField.value] || 0))
const tongSanPhamBanNgay = computed(() =>
  spTheoNgayList.value.reduce((acc, cur) => acc + (cur.soLuongBan || 0), 0)
)
const tongDoanhThuNgay = computed(() =>
  spTheoNgayList.value.reduce((acc, cur) => acc + (cur.doanhThu || 0), 0)
)

/* ---------------------------------- Phần dưới: 2 Biểu đồ tròn Doughnut ---------------------------------- */
const thuongHieuData = ref({ tongDoanhThu: 0, tongSoLuong: 0, danhSach: [] })
const trangThaiDonData = ref([])

// Biểu đồ Thương hiệu
const brandSlices = computed(() => {
  const list = thuongHieuData.value.danhSach || []
  if (!list.length) return []
  return list.map((item) => ({
    label: item.ten,
    value: item.doanhThu,
    color: item.mau || '#926c45'
  }))
})

// Biểu đồ Trạng thái đơn gom 3 nhóm (Đã hủy, Hoàn thành, Đang xử lý)
const orderStatusSummary = computed(() => {
  let daHuy = 0
  let hoanThanh = 0
  let dangXuLy = 0

  for (const item of trangThaiDonData.value) {
    const ten = String(item.ten || '').toLowerCase()
    const sl = Number(item.soLuong) || 0
    if (ten.includes('hủy') || ten.includes('hoàn tiền')) {
      daHuy += sl
    } else if (ten.includes('hoàn thành') || ten.includes('đã giao hàng')) {
      hoanThanh += sl
    } else {
      dangXuLy += sl
    }
  }

  const tong = daHuy + hoanThanh + dangXuLy
  return {
    tong,
    daHuy,
    hoanThanh,
    dangXuLy,
    daHuyPct: tong > 0 ? Math.round((daHuy / tong) * 100) : 0,
    hoanThanhPct: tong > 0 ? Math.round((hoanThanh / tong) * 100) : 0,
    dangXuLyPct: tong > 0 ? Math.round((dangXuLy / tong) * 100) : 0
  }
})

const orderStatusSlices = computed(() => {
  const s = orderStatusSummary.value
  const arr = []
  if (s.dangXuLy > 0) arr.push({ label: 'Đang xử lý', value: s.dangXuLy, color: '#f59e0b' })
  if (s.hoanThanh > 0) arr.push({ label: 'Hoàn thành', value: s.hoanThanh, color: '#16a34a' })
  if (s.daHuy > 0) arr.push({ label: 'Đã hủy', value: s.daHuy, color: '#dc2626' })
  return arr
})

/* Helper tính cung Doughnut SVG */
const taoCungDonut = (slices, totalVal, R = 68, C = 85) => {
  if (!totalVal || !slices.length) return []
  let start = -Math.PI / 2
  return slices.map((s) => {
    const sweep = (s.value / totalVal) * Math.PI * 2
    const end = start + sweep
    const large = sweep > Math.PI ? 1 : 0
    const p1 = { x: C + R * Math.cos(start), y: C + R * Math.sin(start) }
    const p2 = { x: C + R * Math.cos(end), y: C + R * Math.sin(end) }
    const path =
      sweep >= Math.PI * 2 - 0.0001
        ? `M ${C} ${C - R} A ${R} ${R} 0 1 1 ${C - 0.01} ${C - R} Z`
        : `M ${C} ${C} L ${p1.x} ${p1.y} A ${R} ${R} 0 ${large} 1 ${p2.x} ${p2.y} Z`
    start = end
    return { path, color: s.color, label: s.label, value: s.value }
  })
}

/* ---------------------------------- Phần dưới: Bảng Thống kê sản phẩm ---------------------------------- */
const sanPhamBangList = ref([])
const kwBangSp = ref('')
const sortBangSp = ref('banChay')
const pageBangSp = ref(0)
const sizeBangSp = ref(10)

const sanPhamBangLoc = computed(() => {
  let list = [...sanPhamBangList.value]
  const kw = kwBangSp.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      (item) =>
        String(item.maSanPham || '').toLowerCase().includes(kw) ||
        String(item.tenSanPham || '').toLowerCase().includes(kw) ||
        String(item.tenThuongHieu || '').toLowerCase().includes(kw)
    )
  }

  if (sortBangSp.value === 'doanhThu') {
    list.sort((a, b) => (b.doanhThu || 0) - (a.doanhThu || 0))
  } else if (sortBangSp.value === 'tonKhoNhieu') {
    list.sort((a, b) => (b.tonKho || 0) - (a.tonKho || 0))
  } else if (sortBangSp.value === 'tonKhoIt') {
    list.sort((a, b) => (a.tonKho || 0) - (b.tonKho || 0))
  } else {
    // Mặc định: bán chạy nhất
    list.sort((a, b) => (b.daBan || 0) - (a.daBan || 0))
  }
  return list
})

const tongTrangBangSp = computed(() => Math.max(1, Math.ceil(sanPhamBangLoc.value.length / sizeBangSp.value)))
const sanPhamBangTrang = computed(() => {
  const start = pageBangSp.value * sizeBangSp.value
  return sanPhamBangLoc.value.slice(start, start + sizeBangSp.value)
})

/* ---------------------------------- Nạp dữ liệu đồng bộ ---------------------------------- */
const napDuLieu = async () => {
  if (!tuNgay.value || !denNgay.value) return
  dangTai.value = true
  try {
    const th = thuongHieuChon.value || null
    const [tq, tt, spNgay, thTk, spB] = await Promise.all([
      fetchTongQuan(tuNgay.value, denNgay.value, th),
      fetchTrangThaiDon(tuNgay.value, denNgay.value, th),
      fetchSanPhamTheoNgay(tuNgay.value, denNgay.value, th),
      fetchThuongHieuThongKe(tuNgay.value, denNgay.value),
      fetchSanPhamBang(tuNgay.value, denNgay.value, th, timSanPham.value || null)
    ])

    tongQuanData.value = tq || {
      doanhThu: 0,
      tienMat: 0,
      tienChuyenKhoan: 0,
      soDon: 0,
      aov: 0,
      sanPhamBan: 0,
      khachMoi: 0,
      doanhThuDuKien: 0
    }
    trangThaiDonData.value = tt || []
    spTheoNgayList.value = spNgay || []
    thuongHieuData.value = thTk || { tongDoanhThu: 0, tongSoLuong: 0, danhSach: [] }
    sanPhamBangList.value = spB || []

    // Đồng thời nạp chi tiết thẻ KPI đang mở
    if (activeKpi.value) {
      await taiChiTietKpi()
    }
  } catch (e) {
    console.error('Lỗi nạp thống kê:', e)
    notifyError('Không tải được dữ liệu thống kê')
  } finally {
    dangTai.value = false
  }
}

/* ---------------------------------- Quick time filter ---------------------------------- */
const QUICK = [
  { key: 'tatCa', label: 'Tất cả' },
  { key: 'homNay', label: 'Hôm nay' },
  { key: '7ngay', label: '7 ngày qua' },
  { key: 'thangNay', label: 'Tháng này' },
  { key: 'thangTruoc', label: 'Tháng trước' },
  { key: 'namNay', label: 'Năm nay' }
]

const chonNhanh = (key) => {
  quickActive.value = key
  const nay = new Date()
  if (key === 'tatCa') {
    tuNgay.value = '2024-01-01'
    denNgay.value = toInputDate(nay)
  } else if (key === 'homNay') {
    tuNgay.value = toInputDate(nay)
    denNgay.value = toInputDate(nay)
  } else if (key === '7ngay') {
    const d7 = new Date()
    d7.setDate(d7.getDate() - 6)
    tuNgay.value = toInputDate(d7)
    denNgay.value = toInputDate(nay)
  } else if (key === 'thangNay') {
    tuNgay.value = toInputDate(new Date(nay.getFullYear(), nay.getMonth(), 1))
    denNgay.value = toInputDate(nay)
  } else if (key === 'thangTruoc') {
    tuNgay.value = toInputDate(new Date(nay.getFullYear(), nay.getMonth() - 1, 1))
    denNgay.value = toInputDate(new Date(nay.getFullYear(), nay.getMonth(), 0))
  } else if (key === 'namNay') {
    tuNgay.value = toInputDate(new Date(nay.getFullYear(), 0, 1))
    denNgay.value = toInputDate(nay)
  }
}

const datLaiBoLoc = () => {
  quickActive.value = 'homNay'
  const nay = new Date()
  tuNgay.value = toInputDate(nay)
  denNgay.value = toInputDate(nay)
  thuongHieuChon.value = ''
  timSanPham.value = ''
  kieuThongKe.value = 'ngay'
  notifySuccess('Đã đặt lại bộ lọc', 'Dữ liệu đã được nạp lại cho ngày hôm nay')
}

watch([tuNgay, denNgay, thuongHieuChon], () => {
  napDuLieu()
})

/* ---------------------------------- Gửi mail tổng kết & So sánh ---------------------------------- */
const showEmailModal = ref(false)
const emailLoai = ref('THANG')
const emailThang = ref(new Date().getMonth() + 1)
const emailNam = ref(new Date().getFullYear())
const emailNhan = ref('')
const emailDangGui = ref(false)
const emailErr = ref('')
const soSanhMode = ref(false)

const moGuiMail = () => {
  emailErr.value = ''
  showEmailModal.value = true
  if (!emailNhan.value) {
    fetchEmailMacDinh()
      .then((r) => {
        if (!emailNhan.value) emailNhan.value = r?.email || ''
      })
      .catch(() => {})
  }
}

const guiBaoCao = async () => {
  emailErr.value = ''
  const email = (emailNhan.value || '').trim()
  if (!email.includes('@')) {
    emailErr.value = 'Email người nhận không hợp lệ'
    return
  }
  emailDangGui.value = true
  try {
    const payload = { loai: emailLoai.value, nam: emailNam.value, emailNhan: email }
    if (emailLoai.value === 'THANG') payload.thang = emailThang.value
    const res = await guiMailTongKet(payload)
    if (res?.thanhCong) {
      notifySuccess('Đã gửi báo cáo', `Báo cáo đã được gửi tới ${res.emailNhan}`)
      showEmailModal.value = false
    } else {
      notifyError('Gửi báo cáo thất bại', res?.lyDo || 'Không thể gửi email')
    }
  } catch (e) {
    notifyError('Gửi báo cáo thất bại', e?.message || 'Lỗi kết nối')
  } finally {
    emailDangGui.value = false
  }
}

onMounted(async () => {
  napDuLieu()
  try {
    const cachedBrands = sessionStorage.getItem('stats_brands_cache')
    if (cachedBrands) {
      thuongHieus.value = JSON.parse(cachedBrands)
    } else {
      const res = await http.get('/v1/attributes/all-filter-options')
      if (res && res.data) {
        const list = (res.data.thuongHieu || []).map((t) => t.ten || t).filter(Boolean)
        thuongHieus.value = list
        try {
          sessionStorage.setItem('stats_brands_cache', JSON.stringify(list))
        } catch {}
      }
    }
  } catch {
    if (!thuongHieus.value.length) {
      thuongHieus.value = ['Local Brand', 'FF Shirt', 'Nike', 'Adidas']
    }
  }
})
</script>

<template>
  <div class="statistics-page-container">
    <!-- PAGE HEADER -->
    <div class="page-header">
      <div class="page-header-text">
        <h1 class="page-title">Thống kê doanh thu</h1>
        <p class="page-subtitle">Tổng hợp kinh doanh, phân tích sản phẩm và theo dõi tăng trưởng bán hàng</p>
      </div>
      <div class="page-header-actions">
        <button type="button" class="btn-mail-outline" @click="moGuiMail">
          <i class="bi bi-envelope"></i>
          Gửi mail tổng kết
        </button>
      </div>
    </div>

    <!-- 1. BỘ LỌC THỐNG KÊ (ẢNH 1 - PHẦN TRÊN) -->
    <div class="filter-card">
      <div class="filter-header-row">
        <div class="filter-title">
          <i class="bi bi-funnel"></i>
          <span>Bộ lọc thống kê</span>
        </div>
        <div class="quick-pills-group">
          <button
            v-for="q in QUICK"
            :key="q.key"
            type="button"
            class="quick-pill-btn"
            :class="{ 'is-active': quickActive === q.key }"
            @click="chonNhanh(q.key)"
          >
            {{ q.label }}
          </button>
        </div>
      </div>

      <div class="filter-inputs-grid">
        <div class="filter-input-col">
          <label class="filter-label">TỪ NGÀY</label>
          <div class="input-with-icon">
            <input type="date" v-model="tuNgay" class="form-control" />
          </div>
        </div>

        <div class="filter-input-col">
          <label class="filter-label">ĐẾN NGÀY</label>
          <div class="input-with-icon">
            <input type="date" v-model="denNgay" class="form-control" />
          </div>
        </div>

        <div class="filter-input-col">
          <label class="filter-label">THỐNG KÊ</label>
          <select v-model="kieuThongKe" class="form-select">
            <option value="ngay">Theo ngày</option>
            <option value="tuan">Theo tuần</option>
            <option value="thang">Theo tháng</option>
            <option value="nam">Theo năm</option>
          </select>
        </div>

        <div class="filter-input-col">
          <label class="filter-label">THƯƠNG HIỆU</label>
          <select v-model="thuongHieuChon" class="form-select">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="t in thuongHieus" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>

        <div class="filter-input-col flex-grow">
          <label class="filter-label">TÌM SẢN PHẨM</label>
          <div class="search-input-wrap">
            <i class="bi bi-search search-icon"></i>
            <input
              type="text"
              v-model="timSanPham"
              placeholder="Mã hoặc tên sản phẩm..."
              class="form-control ps-4"
              @keyup.enter="napDuLieu"
            />
          </div>
        </div>

        <div class="filter-actions-col">
          <label class="filter-label">&nbsp;</label>
          <button type="button" class="btn-reset" @click="datLaiBoLoc">
            <i class="bi bi-arrow-clockwise"></i>
            Đặt lại
          </button>
        </div>
      </div>

      <div class="filter-status-banner">
        <span class="status-dot">●</span>
        <span>Đang hiển thị dữ liệu từ ngày <strong>{{ hienThiNgay(tuNgay) }}</strong> đến ngày <strong>{{ hienThiNgay(denNgay) }}</strong></span>
      </div>
    </div>

    <!-- 2. HÀNG 5 THẺ KPI CÓ NÚT CHI TIẾT (ẢNH 1) -->
    <div class="kpi-grid">
      <!-- Card 1: Doanh thu thực tế -->
      <div
        class="kpi-card kpi-green"
        :class="{ 'is-active': activeKpi === 'DOANH_THU_THUC_TE' }"
        @click="toggleKpi('DOANH_THU_THUC_TE')"
      >
        <div class="kpi-top">
          <div class="kpi-icon icon-green">
            <i class="bi bi-graph-up-arrow"></i>
          </div>
          <button type="button" class="kpi-toggle-btn">
            {{ activeKpi === 'DOANH_THU_THUC_TE' ? 'Đang mở ˄' : 'Chi tiết ˅' }}
          </button>
        </div>
        <div class="kpi-body">
          <div class="kpi-value text-green">{{ formatVnd(tongQuanData.doanhThu) }}</div>
          <div class="kpi-title">Doanh thu thực tế</div>
        </div>
      </div>

      <!-- Card 2: Doanh thu dự kiến -->
      <div
        class="kpi-card kpi-amber"
        :class="{ 'is-active': activeKpi === 'DOANH_THU_DU_KIEN' }"
        @click="toggleKpi('DOANH_THU_DU_KIEN')"
      >
        <div class="kpi-top">
          <div class="kpi-icon icon-amber">
            <i class="bi bi-clock-history"></i>
          </div>
          <button type="button" class="kpi-toggle-btn">
            {{ activeKpi === 'DOANH_THU_DU_KIEN' ? 'Đang mở ˄' : 'Chi tiết ˅' }}
          </button>
        </div>
        <div class="kpi-body">
          <div class="kpi-value text-amber">{{ formatVnd(tongQuanData.doanhThuDuKien) }}</div>
          <div class="kpi-title">Doanh thu dự kiến</div>
        </div>
      </div>

      <!-- Card 3: Tiền mặt & Chuyển khoản -->
      <div
        class="kpi-card kpi-blue"
        :class="{ 'is-active': activeKpi === 'TIEN_MAT_CHUYEN_KHOAN' }"
        @click="toggleKpi('TIEN_MAT_CHUYEN_KHOAN')"
      >
        <div class="kpi-top">
          <div class="kpi-icon icon-blue">
            <i class="bi bi-wallet2"></i>
          </div>
          <button type="button" class="kpi-toggle-btn">
            {{ activeKpi === 'TIEN_MAT_CHUYEN_KHOAN' ? 'Đang mở ˄' : 'Chi tiết ˅' }}
          </button>
        </div>
        <div class="kpi-body">
          <div class="kpi-dual-row">
            <span class="label">Tiền mặt:</span>
            <span class="val">{{ formatVnd(tongQuanData.tienMat) }}</span>
          </div>
          <div class="kpi-dual-row">
            <span class="label">Chuyển khoản:</span>
            <span class="val">{{ formatVnd(tongQuanData.tienChuyenKhoan) }}</span>
          </div>
          <div class="kpi-title mt-1">Tiền mặt &amp; Chuyển khoản</div>
        </div>
      </div>

      <!-- Card 4: Tổng đơn hàng -->
      <div
        class="kpi-card kpi-purple"
        :class="{ 'is-active': activeKpi === 'TONG_DON_HANG' }"
        @click="toggleKpi('TONG_DON_HANG')"
      >
        <div class="kpi-top">
          <div class="kpi-icon icon-purple">
            <i class="bi bi-cart3"></i>
          </div>
          <button type="button" class="kpi-toggle-btn">
            {{ activeKpi === 'TONG_DON_HANG' ? 'Đang mở ˄' : 'Chi tiết ˅' }}
          </button>
        </div>
        <div class="kpi-body">
          <div class="kpi-value text-purple">{{ tongQuanData.soDon }}</div>
          <div class="kpi-title">Tổng đơn hàng</div>
        </div>
      </div>

      <!-- Card 5: Sản phẩm đã bán -->
      <div
        class="kpi-card kpi-orange"
        :class="{ 'is-active': activeKpi === 'SAN_PHAM_DA_BAN' }"
        @click="toggleKpi('SAN_PHAM_DA_BAN')"
      >
        <div class="kpi-top">
          <div class="kpi-icon icon-orange">
            <i class="bi bi-box-seam"></i>
          </div>
          <button type="button" class="kpi-toggle-btn">
            {{ activeKpi === 'SAN_PHAM_DA_BAN' ? 'Đang mở ˄' : 'Chi tiết ˅' }}
          </button>
        </div>
        <div class="kpi-body">
          <div class="kpi-value text-orange">{{ tongQuanData.sanPhamBan }}</div>
          <div class="kpi-title">Sản phẩm đã bán</div>
        </div>
      </div>
    </div>

    <!-- 3. KHUNG CHI TIẾT ĐỘNG KHI CLICK VÀO TỪNG THẺ KPI (ẢNH 1 - DƯỚI 5 CARD) -->
    <div v-if="activeKpi" class="kpi-detail-card">
      <div class="detail-header-row">
        <div class="detail-title-group">
          <i v-if="activeKpi === 'SAN_PHAM_DA_BAN'" class="bi bi-box-seam text-orange fs-5"></i>
          <i v-else class="bi bi-cart-check text-purple fs-5"></i>
          <h3 class="detail-heading">
            <template v-if="activeKpi === 'TONG_DON_HANG'">Danh sách tất cả đơn hàng</template>
            <template v-else-if="activeKpi === 'DOANH_THU_THUC_TE'">Chi tiết doanh thu thực tế</template>
            <template v-else-if="activeKpi === 'DOANH_THU_DU_KIEN'">Chi tiết doanh thu dự kiến</template>
            <template v-else-if="activeKpi === 'TIEN_MAT_CHUYEN_KHOAN'">Chi tiết tiền mặt &amp; chuyển khoản</template>
            <template v-else-if="activeKpi === 'SAN_PHAM_DA_BAN'">Chi tiết sản phẩm đã bán</template>
          </h3>
          <span class="detail-count-badge">
            <template v-if="activeKpi === 'SAN_PHAM_DA_BAN'">
              Đã bán ({{ sanPhamDaBanLoc.length }}/{{ sanPhamDaBanList.length }} sản phẩm)
            </template>
            <template v-else>
              Toàn bộ đơn hàng ({{ donHangLoc.length }}/{{ donHangList.length }} đơn)
            </template>
          </span>
        </div>

        <div class="detail-tabs-group">
          <!-- Sub-tabs cho đơn hàng -->
          <template v-if="activeKpi === 'TIEN_MAT_CHUYEN_KHOAN'">
            <button
              type="button"
              class="sub-tab-btn"
              :class="{ 'is-active': subTabOrder === 'ALL' }"
              @click="subTabOrder = 'ALL'"
            >
              <i class="bi bi-grid"></i> Tất cả <span class="tab-badge">{{ donHangList.length }}</span>
            </button>
            <button
              type="button"
              class="sub-tab-btn"
              :class="{ 'is-active': subTabOrder === 'TIEN_MAT' }"
              @click="subTabOrder = 'TIEN_MAT'"
            >
              <i class="bi bi-cash"></i> Tiền mặt {{ countDonTienMat }}
            </button>
            <button
              type="button"
              class="sub-tab-btn"
              :class="{ 'is-active': subTabOrder === 'CHUYEN_KHOAN' }"
              @click="subTabOrder = 'CHUYEN_KHOAN'"
            >
              <i class="bi bi-credit-card"></i> Chuyển khoản {{ countDonChuyenKhoan }}
            </button>
          </template>

          <template v-else-if="activeKpi !== 'SAN_PHAM_DA_BAN'">
            <button
              type="button"
              class="sub-tab-btn"
              :class="{ 'is-active': subTabOrder === 'ALL' }"
              @click="subTabOrder = 'ALL'"
            >
              <i class="bi bi-grid"></i> Tất cả <span class="tab-badge">{{ donHangList.length }}</span>
            </button>
            <button
              type="button"
              class="sub-tab-btn"
              :class="{ 'is-active': subTabOrder === 'TAI_QUAY' }"
              @click="subTabOrder = 'TAI_QUAY'"
            >
              <i class="bi bi-shop"></i> Tại quầy {{ countDonTaiQuay }}
            </button>
            <button
              type="button"
              class="sub-tab-btn"
              :class="{ 'is-active': subTabOrder === 'ONLINE' }"
              @click="subTabOrder = 'ONLINE'"
            >
              <i class="bi bi-globe"></i> Online {{ countDonOnline }}
            </button>
          </template>

          <button type="button" class="btn-collapse" @click="activeKpi = null">
            ✕ Thu gọn
          </button>
        </div>
      </div>

      <!-- Toolbar tìm kiếm & lọc trong bảng chi tiết -->
      <div class="detail-filter-bar">
        <div class="search-wrap">
          <i class="bi bi-search search-icon"></i>
          <input
            type="text"
            v-model="kwDonHang"
            :placeholder="activeKpi === 'SAN_PHAM_DA_BAN' ? 'Tìm mã, tên sản phẩm, mã đơn...' : 'Tìm mã đơn, khách hàng, SĐT...'"
            class="form-control"
          />
        </div>

        <template v-if="activeKpi !== 'SAN_PHAM_DA_BAN'">
          <select v-model="filterTrangThaiDon" class="form-select select-filter">
            <option value="">Tất cả trạng thái</option>
            <option value="Chờ xác nhận">Chờ xác nhận</option>
            <option value="Đã xác nhận">Đã xác nhận</option>
            <option value="Chờ giao hàng">Chờ giao hàng</option>
            <option value="Đang giao hàng">Đang giao hàng</option>
            <option value="Đã giao hàng">Đã giao hàng</option>
            <option value="Đã hoàn thành">Đã hoàn thành</option>
            <option value="Đã hủy">Đã hủy</option>
          </select>

          <select v-model="filterThanhToanDon" class="form-select select-filter">
            <option value="">Tất cả thanh toán</option>
            <option value="Đã thanh toán">Đã thanh toán</option>
            <option value="Chưa thanh toán">Chưa thanh toán</option>
          </select>

          <select v-model="sortDonHang" class="form-select select-sort">
            <option value="moiNhat">Mới nhất</option>
            <option value="cuNhat">Cũ nhất</option>
            <option value="tienCao">Thành tiền cao nhất</option>
            <option value="tienThap">Thành tiền thấp nhất</option>
          </select>
        </template>

        <button
          type="button"
          class="btn-reset-detail"
          @click="kwDonHang = ''; filterTrangThaiDon = ''; filterThanhToanDon = ''; sortDonHang = 'moiNhat'"
        >
          <i class="bi bi-arrow-clockwise"></i> Đặt lại
        </button>
      </div>

      <!-- Bảng chi tiết hóa đơn (Nếu chọn Card 1, 2, 3, 4) -->
      <div v-if="activeKpi !== 'SAN_PHAM_DA_BAN'" class="table-responsive mt-3">
        <table class="table custom-detail-table align-middle">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th>MÃ ĐƠN HÀNG</th>
              <th>THỜI GIAN TẠO</th>
              <th>KHÁCH HÀNG</th>
              <th>NHÂN VIÊN</th>
              <th>LOẠI ĐƠN</th>
              <th>HÌNH THỨC TT</th>
              <th>TRẠNG THÁI</th>
              <th>THANH TOÁN</th>
              <th class="text-end">THÀNH TIỀN</th>
              <th class="text-center" style="width: 80px;">THAO TÁC</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="dangTaiChiTiet">
              <td colspan="11" class="text-center py-4 text-muted">
                <span class="spinner-border spinner-border-sm me-2"></span> Đang tải danh sách đơn hàng...
              </td>
            </tr>
            <tr v-else-if="!donHangTrang.length">
              <td colspan="11" class="text-center py-4 text-muted">Không tìm thấy đơn hàng nào phù hợp</td>
            </tr>
            <tr v-for="(row, idx) in donHangTrang" :key="row.id || idx">
              <td>{{ pageDon * sizeDon + idx + 1 }}</td>
              <td>
                <span class="order-code-link" @click="xemChiTietDon(row.id)">{{ row.maHoaDon }}</span>
              </td>
              <td>{{ formatDateTime(row.ngayTao) }}</td>
              <td>
                <div class="fw-medium">{{ row.tenKhachHang || 'Khách lẻ' }}</div>
                <div class="text-muted small">{{ row.soDienThoaiKhachHang || '' }}</div>
              </td>
              <td>
                <span class="staff-badge">{{ row.tenNhanVien || 'Khách hàng online' }}</span>
              </td>
              <td>
                <span
                  class="badge-loai-don"
                  :class="String(row.loaiDon || '').toUpperCase().includes('ONLINE') ? 'badge-online' : 'badge-pos'"
                >
                  <i :class="String(row.loaiDon || '').toUpperCase().includes('ONLINE') ? 'bi bi-globe' : 'bi bi-shop'"></i>
                  {{ String(row.loaiDon || '').toUpperCase().includes('ONLINE') ? 'Online' : 'Tại quầy' }}
                </span>
              </td>
              <td>{{ row.hinhThucThanhToan || 'Chuyển khoản' }}</td>
              <td>
                <span
                  class="badge-order-status"
                  :class="{
                    'status-pending': row.trangThai === 'Chờ xác nhận' || row.trangThai === 'Chờ thanh toán',
                    'status-confirmed': row.trangThai === 'Đã xác nhận' || row.trangThai === 'Chờ giao hàng' || row.trangThai === 'Đang giao hàng',
                    'status-completed': row.trangThai === 'Đã hoàn thành' || row.trangThai === 'Đã giao hàng',
                    'status-cancelled': row.trangThai === 'Đã hủy' || row.trangThai === 'Đã hoàn tiền'
                  }"
                >
                  {{ row.trangThai }}
                </span>
              </td>
              <td>
                <span
                  class="badge-payment-status"
                  :class="row.trangThaiThanhToan === 'Đã thanh toán' ? 'pay-paid' : 'pay-unpaid'"
                >
                  {{ row.trangThaiThanhToan }}
                </span>
              </td>
              <td class="text-end fw-bold text-dark">{{ formatVnd(row.thanhTien) }}</td>
              <td class="text-center">
                <button
                  type="button"
                  class="btn-view-action"
                  title="Xem chi tiết hóa đơn"
                  @click="xemChiTietDon(row.id)"
                >
                  <i class="bi bi-eye"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Bảng chi tiết sản phẩm đã bán (Nếu chọn Card 5: Sản phẩm đã bán) -->
      <div v-else class="table-responsive mt-3">
        <table class="table custom-detail-table align-middle">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th>MÃ SẢN PHẨM</th>
              <th>TÊN SẢN PHẨM &amp; BIẾN THỂ</th>
              <th>THƯƠNG HIỆU</th>
              <th>MÃ HÓA ĐƠN</th>
              <th>THỜI GIAN</th>
              <th class="text-center">SỐ LƯỢNG</th>
              <th class="text-end">ĐƠN GIÁ</th>
              <th class="text-end">THÀNH TIỀN</th>
              <th class="text-center" style="width: 80px;">THAO TÁC</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="dangTaiChiTiet">
              <td colspan="10" class="text-center py-4 text-muted">
                <span class="spinner-border spinner-border-sm me-2"></span> Đang tải danh sách sản phẩm đã bán...
              </td>
            </tr>
            <tr v-else-if="!sanPhamDaBanTrang.length">
              <td colspan="10" class="text-center py-4 text-muted">Chưa có sản phẩm nào được bán trong khoảng thời gian này</td>
            </tr>
            <tr v-for="(item, idx) in sanPhamDaBanTrang" :key="idx">
              <td>{{ pageDon * sizeDon + idx + 1 }}</td>
              <td><span class="badge bg-light text-dark fw-bold">{{ item.maSanPham }}</span></td>
              <td>
                <div class="fw-bold">{{ item.tenSanPham }}</div>
                <div class="text-muted small">{{ item.bienThe }}</div>
              </td>
              <td>{{ item.tenThuongHieu }}</td>
              <td>
                <span class="order-code-link" @click="xemChiTietDon(item.idHoaDon)">{{ item.maHoaDon }}</span>
              </td>
              <td>{{ formatDateTime(item.ngayTao) }}</td>
              <td class="text-center fw-bold text-success">{{ item.soLuong }}</td>
              <td class="text-end">{{ formatVnd(item.donGia) }}</td>
              <td class="text-end fw-bold text-dark">{{ formatVnd(item.thanhTien) }}</td>
              <td class="text-center">
                <button
                  type="button"
                  class="btn-view-action"
                  title="Xem hóa đơn"
                  @click="xemChiTietDon(item.idHoaDon)"
                >
                  <i class="bi bi-eye"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang danh sách chi tiết -->
      <div class="detail-pagination-row">
        <div class="pagination-info">
          Hiển thị
          <template v-if="activeKpi === 'SAN_PHAM_DA_BAN'">
            {{ sanPhamDaBanLoc.length > 0 ? pageDon * sizeDon + 1 : 0 }} -
            {{ Math.min((pageDon + 1) * sizeDon, sanPhamDaBanLoc.length) }} trong tổng số
            {{ sanPhamDaBanLoc.length }} bản ghi
          </template>
          <template v-else>
            {{ donHangLoc.length > 0 ? pageDon * sizeDon + 1 : 0 }} -
            {{ Math.min((pageDon + 1) * sizeDon, donHangLoc.length) }} trong tổng số
            {{ donHangLoc.length }} đơn hàng
          </template>
        </div>

        <div class="pagination-pages">
          <button
            type="button"
            class="page-nav-btn"
            :disabled="pageDon === 0"
            @click="pageDon--"
          >
            ‹
          </button>
          <button
            v-for="p in (activeKpi === 'SAN_PHAM_DA_BAN' ? tongTrangSpBan : tongTrangDon)"
            :key="p"
            type="button"
            class="page-num-btn"
            :class="{ 'is-active': pageDon === p - 1 }"
            @click="pageDon = p - 1"
          >
            {{ p }}
          </button>
          <button
            type="button"
            class="page-nav-btn"
            :disabled="pageDon >= (activeKpi === 'SAN_PHAM_DA_BAN' ? tongTrangSpBan - 1 : tongTrangDon - 1)"
            @click="pageDon++"
          >
            ›
          </button>
        </div>

        <div class="pagination-size-select">
          <select v-model="sizeDon" class="form-select form-select-sm" @change="pageDon = 0">
            <option :value="5">Hiển thị 5 bản ghi / trang</option>
            <option :value="10">Hiển thị 10 bản ghi / trang</option>
            <option :value="20">Hiển thị 20 bản ghi / trang</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 4. PHẦN GIỮA: BIỂU ĐỒ SẢN PHẨM BÁN ĐƯỢC THEO NGÀY (DỮ LIỆU THỰC TẾ HÓA ĐƠN DB) -->
    <div class="chart-section-card mt-4">
      <div class="chart-header-row">
        <div>
          <h3 class="chart-title">
            <i class="bi bi-bar-chart-line text-primary"></i>
            Sản phẩm bán được theo ngày
          </h3>
          <p class="chart-subtitle">
            Dữ liệu tổng hợp trực tiếp từ hóa đơn thực tế trong hệ thống PolyShoes
          </p>
        </div>
        <div class="chart-actions-group">
          <span class="badge bg-light text-primary border me-2">
            Tổng bán: <strong>{{ tongSanPhamBanNgay }}</strong> sản phẩm
          </span>
          <span class="badge bg-light text-success border me-3">
            Doanh thu: <strong>{{ formatVnd(tongDoanhThuNgay) }}</strong>
          </span>
          <div class="btn-group btn-group-sm">
            <button
              type="button"
              class="btn btn-outline-secondary"
              :class="{ 'active': chartField === 'soLuongBan' }"
              @click="chartField = 'soLuongBan'"
            >
              Số lượng bán
            </button>
            <button
              type="button"
              class="btn btn-outline-secondary"
              :class="{ 'active': chartField === 'doanhThu' }"
              @click="chartField = 'doanhThu'"
            >
              Doanh thu
            </button>
          </div>
        </div>
      </div>

      <div class="chart-body-wrap">
        <div v-if="!chartLabels.length" class="text-center py-5 text-muted">
          Không có dữ liệu trong khoảng thời gian đã chọn
        </div>
        <BarLineChart
          v-else
          :labels="chartLabels"
          :values="chartValues"
          :color="chartField === 'soLuongBan' ? '#8b5cf6' : '#16a34a'"
          :currency="chartField === 'doanhThu'"
          :height="260"
          mode="cot"
        />
      </div>
    </div>

    <!-- 5. PHẦN DƯỚI (ẢNH 2): 2 BIỂU ĐỒ DOUGHNUT (THƯƠNG HIỆU & TRẠNG THÁI ĐƠN HÀNG) -->
    <div class="charts-two-col-grid mt-4">
      <!-- Biểu đồ 1: Thương hiệu -->
      <div class="donut-card">
        <div class="donut-card-header">
          <div class="donut-title">
            <i class="bi bi-circle"></i>
            <span>Thương hiệu</span>
          </div>
          <span class="badge-tag">Theo doanh thu</span>
        </div>

        <div class="donut-content-wrap">
          <div class="donut-svg-container">
            <svg viewBox="0 0 170 170" class="donut-svg">
              <path
                v-for="(arc, i) in taoCungDonut(brandSlices, thuongHieuData.tongDoanhThu)"
                :key="i"
                :d="arc.path"
                :fill="arc.color"
              >
                <title>{{ arc.label }}: {{ formatVnd(arc.value) }}</title>
              </path>
              <circle cx="85" cy="85" r="50" fill="#ffffff" />
              <text x="85" y="78" text-anchor="middle" class="donut-center-label">Tổng</text>
              <text x="85" y="98" text-anchor="middle" class="donut-center-value">
                {{ formatVnd(thuongHieuData.tongDoanhThu) }}
              </text>
            </svg>
          </div>

          <div class="donut-legend-list">
            <div
              v-for="item in thuongHieuData.danhSach"
              :key="item.ten"
              class="legend-item-row"
            >
              <div class="legend-label-col">
                <span class="legend-color-dot" :style="{ backgroundColor: item.mau || '#926c45' }"></span>
                <span class="legend-text">{{ item.ten }}</span>
              </div>
              <div class="legend-pct-col">{{ item.tyLe }}%</div>
            </div>
            <div v-if="!thuongHieuData.danhSach.length" class="text-muted small text-center py-2">
              Không có dữ liệu thương hiệu
            </div>
          </div>
        </div>
      </div>

      <!-- Biểu đồ 2: Trạng thái đơn hàng -->
      <div class="donut-card">
        <div class="donut-card-header">
          <div class="donut-title">
            <i class="bi bi-file-earmark-text"></i>
            <span>Trạng thái đơn hàng</span>
          </div>
          <span class="badge-tag">Tổng {{ orderStatusSummary.tong }} đơn</span>
        </div>

        <div class="donut-content-wrap">
          <div class="donut-svg-container">
            <svg viewBox="0 0 170 170" class="donut-svg">
              <path
                v-for="(arc, i) in taoCungDonut(orderStatusSlices, orderStatusSummary.tong)"
                :key="i"
                :d="arc.path"
                :fill="arc.color"
              >
                <title>{{ arc.label }}: {{ arc.value }} đơn</title>
              </path>
              <circle cx="85" cy="85" r="50" fill="#ffffff" />
              <text x="85" y="78" text-anchor="middle" class="donut-center-label">Đơn hàng</text>
              <text x="85" y="100" text-anchor="middle" class="donut-center-num">
                {{ orderStatusSummary.tong }}
              </text>
            </svg>
          </div>

          <div class="donut-legend-horizontal">
            <div class="legend-badge red">
              <span class="dot bg-danger"></span>
              Đã hủy ({{ orderStatusSummary.daHuy }})
            </div>
            <div class="legend-badge green">
              <span class="dot bg-success"></span>
              Hoàn thành ({{ orderStatusSummary.hoanThanh }})
            </div>
            <div class="legend-badge amber">
              <span class="dot bg-warning"></span>
              Đang xử lý ({{ orderStatusSummary.dangXuLy }})
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. PHẦN DƯỚI (ẢNH 2): BẢNG THỐNG KÊ SẢN PHẨM -->
    <div class="product-stats-card mt-4">
      <div class="card-header-bar">
        <div class="title-with-icon">
          <i class="bi bi-box fs-5"></i>
          <span>Thống kê sản phẩm</span>
        </div>

        <div class="header-tools">
          <div class="search-wrap">
            <i class="bi bi-search search-icon"></i>
            <input
              type="text"
              v-model="kwBangSp"
              placeholder="Tìm trong bảng..."
              class="form-control form-control-sm"
            />
          </div>

          <select v-model="sortBangSp" class="form-select form-select-sm select-sort">
            <option value="banChay">Bán chạy nhất</option>
            <option value="doanhThu">Doanh thu cao nhất</option>
            <option value="tonKhoNhieu">Tồn kho nhiều nhất</option>
            <option value="tonKhoIt">Tồn kho ít nhất</option>
          </select>

          <button
            type="button"
            class="btn-reset-table"
            @click="kwBangSp = ''; sortBangSp = 'banChay'; pageBangSp = 0"
          >
            <i class="bi bi-arrow-clockwise"></i> Đặt lại
          </button>
        </div>
      </div>

      <div class="table-responsive">
        <table class="table custom-product-table align-middle">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th>MÃ SẢN PHẨM</th>
              <th>TÊN SẢN PHẨM</th>
              <th>THƯƠNG HIỆU</th>
              <th class="text-center">ĐÃ BÁN</th>
              <th class="text-center">SỐ LƯỢNG TRẢ</th>
              <th class="text-end">DOANH THU</th>
              <th class="text-center">TỒN KHO</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!sanPhamBangTrang.length">
              <td colspan="8" class="text-center py-4 text-muted">Không tìm thấy sản phẩm nào</td>
            </tr>
            <tr v-for="(sp, idx) in sanPhamBangTrang" :key="sp.idSanPham || idx">
              <td>{{ pageBangSp * sizeBangSp + idx + 1 }}</td>
              <td><span class="badge bg-light text-dark fw-bold">{{ sp.maSanPham }}</span></td>
              <td>
                <div class="product-cell-wrap">
                  <div class="product-thumb">
                    <img v-if="sp.hinhAnh" :src="sp.hinhAnh" :alt="sp.tenSanPham" />
                    <div v-else class="thumb-placeholder"><i class="bi bi-box"></i></div>
                  </div>
                  <div class="product-info-col">
                    <div class="fw-bold product-name-text">{{ sp.tenSanPham }}</div>
                    <div class="text-muted small">{{ sp.bienThe || 'Tiêu chuẩn' }}</div>
                  </div>
                </div>
              </td>
              <td>{{ sp.tenThuongHieu || '—' }}</td>
              <td class="text-center fw-bold">{{ sp.daBan || 0 }}</td>
              <td class="text-center text-danger">{{ sp.soLuongTra || 0 }}</td>
              <td class="text-end fw-bold text-dark">{{ formatVnd(sp.doanhThu) }}</td>
              <td class="text-center">
                <span :class="sp.tonKho > 0 ? 'text-success fw-bold' : 'text-danger fw-bold'">
                  {{ sp.tonKho }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-pagination-footer">
        <div class="pagination-info">
          Hiển thị
          {{ sanPhamBangLoc.length > 0 ? pageBangSp * sizeBangSp + 1 : 0 }} -
          {{ Math.min((pageBangSp + 1) * sizeBangSp, sanPhamBangLoc.length) }} /
          {{ sanPhamBangLoc.length }} bản ghi
        </div>

        <div class="pagination-pages">
          <button
            type="button"
            class="page-nav-btn"
            :disabled="pageBangSp === 0"
            @click="pageBangSp--"
          >
            ‹
          </button>
          <button
            v-for="p in tongTrangBangSp"
            :key="p"
            type="button"
            class="page-num-btn"
            :class="{ 'is-active': pageBangSp === p - 1 }"
            @click="pageBangSp = p - 1"
          >
            {{ p }}
          </button>
          <button
            type="button"
            class="page-nav-btn"
            :disabled="pageBangSp >= tongTrangBangSp - 1"
            @click="pageBangSp++"
          >
            ›
          </button>
        </div>

        <div class="pagination-size-select">
          <select v-model="sizeBangSp" class="form-select form-select-sm" @change="pageBangSp = 0">
            <option :value="10">Hiển thị 10 bản ghi / trang</option>
            <option :value="20">Hiển thị 20 bản ghi / trang</option>
            <option :value="50">Hiển thị 50 bản ghi / trang</option>
          </select>
        </div>
      </div>
    </div>

    <!-- MODAL GỬI MAIL TỔNG KẾT -->
    <div v-if="showEmailModal" class="modal-backdrop-custom">
      <div class="modal-dialog-custom">
        <div class="modal-header-custom">
          <h5><i class="bi bi-envelope me-2"></i>Gửi báo cáo tổng kết doanh thu</h5>
          <button type="button" class="btn-close" @click="showEmailModal = false"></button>
        </div>
        <div class="modal-body-custom">
          <div class="mb-3">
            <label class="form-label">Kỳ báo cáo</label>
            <div class="d-flex gap-3">
              <label class="form-check-label">
                <input type="radio" v-model="emailLoai" value="THANG" /> Theo Tháng
              </label>
              <label class="form-check-label">
                <input type="radio" v-model="emailLoai" value="NAM" /> Theo Năm
              </label>
            </div>
          </div>

          <div v-if="emailLoai === 'THANG'" class="mb-3">
            <label class="form-label">Chọn tháng</label>
            <select v-model="emailThang" class="form-select">
              <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
            </select>
          </div>

          <div class="mb-3">
            <label class="form-label">Chọn năm</label>
            <input type="number" v-model="emailNam" class="form-control" />
          </div>

          <div class="mb-3">
            <label class="form-label">Email nhận báo cáo</label>
            <input type="email" v-model="emailNhan" class="form-control" placeholder="admin@polyshoes.vn" />
            <div v-if="emailErr" class="text-danger small mt-1">{{ emailErr }}</div>
          </div>
        </div>
        <div class="modal-footer-custom">
          <button type="button" class="btn btn-secondary" @click="showEmailModal = false">Hủy</button>
          <button type="button" class="btn btn-primary" :disabled="emailDangGui" @click="guiBaoCao">
            <span v-if="emailDangGui" class="spinner-border spinner-border-sm me-2"></span>
            Gửi báo cáo
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.statistics-page-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: 100vh;
  font-family: inherit;
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}
.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 4px 0 0 0;
}
.btn-mail-outline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-mail-outline:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

/* 1. Filter Card */
.filter-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 20px;
  border: 1px solid #f1f5f9;
}
.filter-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filter-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #1e293b;
  font-size: 15px;
}
.quick-pills-group {
  display: flex;
  gap: 6px;
}
.quick-pill-btn {
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}
.quick-pill-btn:hover {
  background: #f8fafc;
}
.quick-pill-btn.is-active {
  background: #8b5cf6;
  color: #ffffff;
  border-color: #8b5cf6;
  font-weight: 600;
}

.filter-inputs-grid {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}
.filter-input-col {
  min-width: 150px;
}
.flex-grow {
  flex-grow: 1;
}
.filter-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.search-input-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 13px;
}
.btn-reset {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-reset:hover {
  background: #f1f5f9;
}
.filter-status-banner {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
  font-size: 13px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-dot {
  color: #3b82f6;
  font-size: 14px;
}

/* 2. KPI Cards Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.kpi-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 18px;
  border: 1.5px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.kpi-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.kpi-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.icon-green { background: #dcfce7; color: #16a34a; }
.icon-amber { background: #fef3c7; color: #d97706; }
.icon-blue { background: #e0f2fe; color: #0284c7; }
.icon-purple { background: #f3e8ff; color: #7c3aed; }
.icon-orange { background: #ffedd5; color: #ea580c; }

.kpi-toggle-btn {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
}
.kpi-card.is-active .kpi-toggle-btn {
  background: currentColor;
  color: #ffffff;
}

/* Active styles matching Screenshot 1 */
.kpi-purple.is-active {
  border-color: #8b5cf6;
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.2);
}
.kpi-purple.is-active .kpi-toggle-btn {
  background: #7c3aed;
  color: #ffffff;
  border-color: #7c3aed;
}

.kpi-green.is-active {
  border-color: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}
.kpi-green.is-active .kpi-toggle-btn {
  background: #16a34a;
  color: #ffffff;
  border-color: #16a34a;
}

.kpi-amber.is-active {
  border-color: #d97706;
  box-shadow: 0 0 0 2px rgba(217, 119, 6, 0.2);
}
.kpi-amber.is-active .kpi-toggle-btn {
  background: #d97706;
  color: #ffffff;
  border-color: #d97706;
}

.kpi-blue.is-active {
  border-color: #0284c7;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.2);
}
.kpi-blue.is-active .kpi-toggle-btn {
  background: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
}

.kpi-orange.is-active {
  border-color: #ea580c;
  box-shadow: 0 0 0 2px rgba(234, 88, 12, 0.2);
}
.kpi-orange.is-active .kpi-toggle-btn {
  background: #ea580c;
  color: #ffffff;
  border-color: #ea580c;
}

.kpi-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}
.text-green { color: #16a34a; }
.text-amber { color: #d97706; }
.text-purple { color: #7c3aed; }
.text-orange { color: #ea580c; }
.kpi-title {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}
.kpi-dual-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  line-height: 1.4;
}
.kpi-dual-row .label { color: #64748b; }
.kpi-dual-row .val { font-weight: 600; color: #1e293b; }

/* 3. Khung chi tiết động */
.kpi-detail-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
  margin-bottom: 24px;
  animation: fadeIn 0.25s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}

.detail-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 14px;
  margin-bottom: 14px;
}
.detail-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-heading {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}
.detail-count-badge {
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 6px;
}

.detail-tabs-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sub-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}
.sub-tab-btn.is-active {
  background: #1e293b;
  color: #ffffff;
  border-color: #1e293b;
  font-weight: 600;
}
.tab-badge {
  background: #0f172a;
  color: #ffffff;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 11px;
}
.sub-tab-btn.is-active .tab-badge {
  background: #ffffff;
  color: #0f172a;
}
.btn-collapse {
  padding: 5px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
}
.btn-collapse:hover {
  background: #f1f5f9;
}

.detail-filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.detail-filter-bar .search-wrap {
  position: relative;
  min-width: 260px;
}
.select-filter {
  width: auto;
  min-width: 160px;
}
.select-sort {
  width: auto;
  min-width: 140px;
}
.btn-reset-detail {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
}

/* Custom Table */
.custom-detail-table th {
  background: #f1ebe5;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  padding: 10px 12px;
  border-bottom: none;
  text-transform: uppercase;
}
.custom-detail-table td {
  font-size: 13px;
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
}
.order-code-link {
  font-weight: 700;
  color: #2563eb;
  cursor: pointer;
  text-decoration: underline;
}
.staff-badge {
  color: #0284c7;
  font-weight: 500;
}

.badge-loai-don {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.badge-online { background: #e0f2fe; color: #0284c7; }
.badge-pos { background: #fef3c7; color: #b45309; }

.badge-order-status {
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.status-pending { background: #fef3c7; color: #d97706; }
.status-confirmed { background: #e0f2fe; color: #0284c7; }
.status-completed { background: #dcfce7; color: #16a34a; }
.status-cancelled { background: #fee2e2; color: #dc2626; }

.badge-payment-status {
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.pay-paid { background: #dcfce7; color: #16a34a; }
.pay-unpaid { background: #ffedd5; color: #c2410c; }

.btn-view-action {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-view-action:hover {
  background: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}

/* Detail Pagination */
.detail-pagination-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  font-size: 13px;
  color: #64748b;
}
.pagination-pages {
  display: flex;
  gap: 4px;
}
.page-nav-btn, .page-num-btn {
  min-width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.page-num-btn.is-active {
  background: #926c45;
  color: #ffffff;
  border-color: #926c45;
  font-weight: 600;
}

/* 4. Chart Section */
.chart-section-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.chart-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.chart-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.chart-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 3px 0 0 0;
}

/* 5. Two Donut Charts Grid */
.charts-two-col-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.donut-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.donut-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.donut-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}
.badge-tag {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 11px;
  color: #64748b;
}
.donut-content-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.donut-svg-container {
  width: 170px;
  height: 170px;
  margin-bottom: 16px;
}
.donut-svg {
  width: 100%;
  height: 100%;
}
.donut-center-label {
  font-size: 11px;
  fill: #64748b;
  font-weight: 500;
}
.donut-center-value {
  font-size: 12px;
  fill: #1e293b;
  font-weight: 700;
}
.donut-center-num {
  font-size: 18px;
  fill: #1e293b;
  font-weight: 700;
}

.donut-legend-list {
  width: 100%;
}
.legend-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed #f1f5f9;
}
.legend-label-col {
  display: flex;
  align-items: center;
  gap: 8px;
}
.legend-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.legend-pct-col {
  font-weight: 700;
  color: #1e293b;
}

.donut-legend-horizontal {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  font-size: 12px;
  margin-top: 8px;
}
.legend-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
}
.legend-badge .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

/* 6. Product Stats Table (Bottom) */
.product-stats-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.card-header-bar .title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}
.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn-reset-table {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
}
.custom-product-table th {
  background: #e8ded4;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  padding: 10px 12px;
  border-bottom: none;
  text-transform: uppercase;
}
.custom-product-table td {
  font-size: 13px;
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
}
.product-cell-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  overflow: hidden;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.thumb-placeholder {
  color: #94a3b8;
  font-size: 18px;
}
.table-pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  font-size: 13px;
  color: #64748b;
}

/* Modal */
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}
.modal-dialog-custom {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 480px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0,0,0,0.15);
}
.modal-header-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}
.modal-body-custom {
  padding: 20px;
}
.modal-footer-custom {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

/* Responsive */
@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .charts-two-col-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .filter-inputs-grid {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
