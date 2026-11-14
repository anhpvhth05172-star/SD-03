<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { getUser, dangXuat, getRefreshToken, isKhachHangDangNhap } from '@/utils/auth'
import { huyPhien } from '@/api/auth'
import { money } from '@/components/store'

const router = useRouter()
const localUser = ref(getUser())
const daDangNhap = isKhachHangDangNhap()

// Data states
const thongTin = ref({})
const vouchers = ref([])
const donHangs = ref([])
const activeTab = ref('ho-so') // 'ho-so' | 'don-hang' | 'voucher' | 'dia-chi'

// Loading & error states
const loading = ref(daDangNhap)
const loadingOrders = ref(false)
const loadingVouchers = ref(false)
const thongBao = ref({ type: '', text: '' })

// Modal edit profile state
const showEditModal = ref(false)
const editForm = ref({
  tenKhachHang: '',
  soDienThoai: '',
  ngaySinh: '',
  gioiTinh: 'Nam',
})
const savingProfile = ref(false)
const editError = ref('')

// Copied voucher state
const copiedCode = ref('')

const initials = computed(() => {
  const name = thongTin.value?.tenKhachHang || thongTin.value?.tenTaiKhoan || localUser.value?.tenKhachHang || ''
  if (!name.trim()) return 'PS'
  const words = name.trim().split(/\s+/)
  if (words.length === 1) return words[0].substring(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
})



const formatDate = (val) => {
  if (!val) return '—'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return String(val)
    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const year = d.getFullYear()
    return `${day}/${month}/${year}`
  } catch {
    return String(val)
  }
}

const formatDateTime = (val) => {
  if (!val) return '—'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return String(val)
    const hours = String(d.getHours()).padStart(2, '0')
    const mins = String(d.getMinutes()).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const year = d.getFullYear()
    return `${hours}:${mins} ${day}/${month}/${year}`
  } catch {
    return String(val)
  }
}

const formatAddress = (addr) => {
  if (!addr) return ''
  if (typeof addr === 'string') return addr
  const parts = [
    addr.diaChiChiTiet,
    addr.phuongXa,
    addr.quanHuyen,
    addr.tinhThanhPho,
  ].filter(Boolean)
  return parts.join(', ')
}

const getOrderStatusBadge = (st) => {
  const s = String(st || '').toUpperCase()
  if (s === 'CHO_THANH_TOAN' || s === 'CHO_XAC_NHAN' || s.includes('CHỜ')) {
    return { label: 'Chờ xác nhận', cls: 'badge-status-warning' }
  }
  if (s === 'DA_XAC_NHAN') {
    return { label: 'Đã xác nhận', cls: 'badge-status-info' }
  }
  if (s === 'DANG_GIAO' || s.includes('GIAO')) {
    return { label: 'Đang giao hàng', cls: 'badge-status-primary' }
  }
  if (s === 'DA_GIAO' || s === 'DA_HOAN_THANH' || s.includes('HOÀN THÀNH')) {
    return { label: 'Hoàn thành', cls: 'badge-status-success' }
  }
  if (s === 'DA_HUY' || s.includes('HỦY')) {
    return { label: 'Đã hủy', cls: 'badge-status-danger' }
  }
  return { label: st || '—', cls: 'badge-status-secondary' }
}

const loadProfile = async () => {
  if (!daDangNhap) return
  loading.value = true
  try {
    const res = await http.get('/auth/thong-tin')
    thongTin.value = res.data || {}
    // Cập nhật lại localStorage user nếu cần
    if (res.data) {
      const u = getUser() || {}
      localStorage.setItem('polyshoes_user', JSON.stringify({
        ...u,
        tenKhachHang: res.data.tenKhachHang || u.tenKhachHang,
        tenTaiKhoan: res.data.tenTaiKhoan || u.tenTaiKhoan,
        email: res.data.email || u.email,
        soDienThoai: res.data.soDienThoai || u.soDienThoai,
      }))
    }
  } catch (e) {
    console.error('Không tải được thông tin tài khoản:', e)
  } finally {
    loading.value = false
  }
}

const loadVouchers = async () => {
  if (!daDangNhap) return
  loadingVouchers.value = true
  try {
    const res = await http.get('/phieu-giam-gia/cua-toi')
    vouchers.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error('Không tải được phiếu giảm giá:', e)
  } finally {
    loadingVouchers.value = false
  }
}

const loadOrders = async () => {
  if (!daDangNhap) return
  loadingOrders.value = true
  try {
    const res = await http.get('/hoa-don/cua-toi', { params: { size: 20 } })
    if (res.data?.content && Array.isArray(res.data.content)) {
      donHangs.value = res.data.content
    } else if (Array.isArray(res.data)) {
      donHangs.value = res.data
    } else {
      donHangs.value = []
    }
  } catch (e) {
    console.error('Không tải được danh sách đơn hàng:', e)
  } finally {
    loadingOrders.value = false
  }
}

const copyVoucher = (code) => {
  if (!code) return
  navigator.clipboard?.writeText(code)
  copiedCode.value = code
  setTimeout(() => {
    if (copiedCode.value === code) copiedCode.value = ''
  }, 2500)
}

const moModalSua = () => {
  editError.value = ''
  editForm.value = {
    tenKhachHang: thongTin.value?.tenKhachHang || '',
    soDienThoai: thongTin.value?.soDienThoai || '',
    ngaySinh: thongTin.value?.ngaySinh ? String(thongTin.value.ngaySinh).split('T')[0] : '',
    gioiTinh: thongTin.value?.gioiTinh || 'Nam',
  }
  showEditModal.value = true
}

const dongModalSua = () => {
  showEditModal.value = false
  editError.value = ''
}

const luuThongTin = async () => {
  editError.value = ''
  if (!editForm.value.tenKhachHang || !editForm.value.tenKhachHang.trim()) {
    editError.value = 'Họ và tên không được để trống'
    return
  }
  savingProfile.value = true
  try {
    const res = await http.put('/auth/thong-tin', {
      tenKhachHang: editForm.value.tenKhachHang.trim(),
      soDienThoai: editForm.value.soDienThoai ? editForm.value.soDienThoai.trim() : null,
      ngaySinh: editForm.value.ngaySinh || null,
      gioiTinh: editForm.value.gioiTinh || null,
    })
    thongTin.value = res.data || {}
    thongBao.value = { type: 'success', text: 'Cập nhật thông tin thành công!' }
    setTimeout(() => { thongBao.value = { type: '', text: '' } }, 3500)
    dongModalSua()
  } catch (e) {
    editError.value = e.response?.data?.message || e.message || 'Cập nhật thông tin thất bại'
  } finally {
    savingProfile.value = false
  }
}

const xulyDangXuat = async () => {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) await huyPhien(refreshToken)
  } catch {
    // Bỏ qua lỗi gọi API hủy phiên
  }
  dangXuat()
  router.push('/khach-hang')
}

onMounted(() => {
  if (daDangNhap) {
    loadProfile()
    loadVouchers()
    loadOrders()
  }
})
</script>

<template>
  <div class="account-page-wrapper">
    <div class="container py-4">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-4">
        <ol class="breadcrumb">
          <li class="breadcrumb-item"><router-link to="/trang-chu">Trang chủ</router-link></li>
          <li class="breadcrumb-item active" aria-current="page">Tài khoản của tôi</li>
        </ol>
      </nav>

      <!-- Alert Message -->
      <div v-if="thongBao.text" class="alert alert-success alert-dismissible fade show d-flex align-items-center mb-4" role="alert">
        <i class="bi bi-check-circle-fill fs-5 me-2"></i>
        <div>{{ thongBao.text }}</div>
        <button type="button" class="btn-close" @click="thongBao.text = ''" aria-label="Close"></button>
      </div>

      <!-- Guest Card -->
      <div v-if="!daDangNhap" class="guest-card shadow-sm text-center p-5">
        <div class="guest-icon mb-3">
          <i class="bi bi-person-lock"></i>
        </div>
        <h2 class="fw-bold mb-2">Bạn chưa đăng nhập</h2>
        <p class="text-muted mb-4 max-w-500 mx-auto">
          Đăng nhập hoặc đăng ký tài khoản PolyShoes để theo dõi đơn hàng, quản lý thông tin giao hàng và nhận nhiều mã giảm giá hấp dẫn.
        </p>
        <div class="d-flex justify-content-center gap-3">
          <router-link to="/khach-hang" class="btn btn-danger btn-lg px-4">
            <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập ngay
          </router-link>
          <router-link to="/dang-ky" class="btn btn-outline-secondary btn-lg px-4">
            Đăng ký tài khoản
          </router-link>
        </div>
      </div>

      <!-- Authenticated View -->
      <template v-else>
        <!-- Hero Profile Banner -->
        <div class="profile-hero-card shadow-sm mb-4">
          <div class="hero-inner d-flex flex-wrap align-items-center justify-content-between gap-4 p-4">
            <div class="d-flex align-items-center gap-4">
              <!-- Avatar Circle -->
              <div class="avatar-circle-wrapper position-relative">
                <div class="avatar-circle">
                  <span>{{ initials }}</span>
                </div>
              </div>

              <!-- Main Info -->
              <div>
                <div class="d-flex align-items-center flex-wrap gap-2 mb-1">
                  <h3 class="customer-name mb-0">{{ thongTin?.tenKhachHang || thongTin?.tenTaiKhoan || 'Khách hàng' }}</h3>
                  <span v-if="thongTin?.maKhachHang" class="badge-cust-code" title="Mã khách hàng">
                    {{ thongTin.maKhachHang }}
                  </span>
                </div>

                <div class="meta-row d-flex align-items-center flex-wrap gap-3 mt-2 text-muted small">
                  <span v-if="thongTin?.email">
                    <i class="bi bi-envelope me-1 text-danger"></i>{{ thongTin.email }}
                  </span>
                  <span v-if="thongTin?.soDienThoai">
                    <i class="bi bi-telephone me-1 text-danger"></i>{{ thongTin.soDienThoai }}
                  </span>
                  <span v-if="thongTin?.ngayTao">
                    <i class="bi bi-calendar-check me-1 text-danger"></i>Gia nhập: {{ formatDate(thongTin.ngayTao) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Profile Actions -->
            <div class="d-flex align-items-center gap-2">
              <button type="button" class="btn btn-outline-danger btn-sm px-3" @click="moModalSua">
                <i class="bi bi-pencil-square me-1"></i> Sửa thông tin
              </button>
              <button type="button" class="btn btn-light btn-sm text-danger px-3 border" @click="xulyDangXuat">
                <i class="bi bi-box-arrow-right me-1"></i> Đăng xuất
              </button>
            </div>
          </div>
        </div>

        <!-- 3 KPI Stat Cards -->
        <div class="row g-3 mb-4">
          <div class="col-12 col-md-4">
            <div class="stat-card shadow-sm cursor-pointer" @click="activeTab = 'don-hang'">
              <div class="d-flex align-items-center justify-content-between">
                <div>
                  <div class="stat-title">Đơn hàng của tôi</div>
                  <div class="stat-value text-danger">{{ thongTin?.soDon ?? donHangs.length }}</div>
                </div>
                <div class="stat-icon-box bg-danger-subtle text-danger">
                  <i class="bi bi-bag-check-fill"></i>
                </div>
              </div>
              <div class="stat-link mt-2">
                <span>Xem danh sách</span> <i class="bi bi-chevron-right small"></i>
              </div>
            </div>
          </div>

          <div class="col-12 col-md-4">
            <div class="stat-card shadow-sm">
              <div class="d-flex align-items-center justify-content-between">
                <div>
                  <div class="stat-title">Tổng chi tiêu</div>
                  <div class="stat-value text-success">{{ money(thongTin?.tongChiTieu || 0) }}</div>
                </div>
                <div class="stat-icon-box bg-success-subtle text-success">
                  <i class="bi bi-wallet2"></i>
                </div>
              </div>
              <div class="stat-desc mt-2 text-muted small">
                Tích lũy từ đơn mua
              </div>
            </div>
          </div>

          <div class="col-12 col-md-4">
            <div class="stat-card shadow-sm cursor-pointer" @click="activeTab = 'voucher'">
              <div class="d-flex align-items-center justify-content-between">
                <div>
                  <div class="stat-title">Phiếu giảm giá</div>
                  <div class="stat-value text-primary">{{ vouchers.length }}</div>
                </div>
                <div class="stat-icon-box bg-primary-subtle text-primary">
                  <i class="bi bi-ticket-perforated-fill"></i>
                </div>
              </div>
              <div class="stat-link mt-2 text-primary">
                <span>Kho voucher</span> <i class="bi bi-chevron-right small"></i>
              </div>
            </div>
          </div>
        </div>

        <!-- Tabbed Container -->
        <div class="tab-card shadow-sm">
          <!-- Tab Navigation -->
          <ul class="nav nav-pills custom-tabs p-3 border-bottom gap-2">
            <li class="nav-item">
              <button
                type="button"
                :class="['nav-link', activeTab === 'ho-so' ? 'active' : '']"
                @click="activeTab = 'ho-so'"
              >
                <i class="bi bi-person-lines-fill me-1"></i> Hồ sơ cá nhân
              </button>
            </li>
            <li class="nav-item">
              <button
                type="button"
                :class="['nav-link', activeTab === 'don-hang' ? 'active' : '']"
                @click="activeTab = 'don-hang'"
              >
                <i class="bi bi-receipt me-1"></i> Đơn hàng của tôi
                <span v-if="donHangs.length" class="badge bg-danger ms-1">{{ donHangs.length }}</span>
              </button>
            </li>
            <li class="nav-item">
              <button
                type="button"
                :class="['nav-link', activeTab === 'voucher' ? 'active' : '']"
                @click="activeTab = 'voucher'"
              >
                <i class="bi bi-tag-fill me-1"></i> Mã giảm giá
                <span v-if="vouchers.length" class="badge bg-danger ms-1">{{ vouchers.length }}</span>
              </button>
            </li>
            <li class="nav-item">
              <button
                type="button"
                :class="['nav-link', activeTab === 'dia-chi' ? 'active' : '']"
                @click="activeTab = 'dia-chi'"
              >
                <i class="bi bi-geo-alt-fill me-1"></i> Sổ địa chỉ
              </button>
            </li>
          </ul>

          <!-- Tab Content -->
          <div class="tab-body p-4">
            <!-- TAB 1: HỒ SƠ CÁ NHÂN -->
            <div v-if="activeTab === 'ho-so'">
              <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                <h5 class="fw-bold mb-0 text-dark">
                  <i class="bi bi-shield-lock-fill text-danger me-2"></i>Chi tiết thông tin cá nhân
                </h5>
                <button type="button" class="btn btn-sm btn-outline-danger" @click="moModalSua">
                  <i class="bi bi-pencil-fill me-1"></i> Cập nhật
                </button>
              </div>

              <div class="row g-3">
                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Họ và tên</span>
                    <span class="info-value">{{ thongTin?.tenKhachHang || '—' }}</span>
                  </div>
                </div>

                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Tên tài khoản</span>
                    <span class="info-value">{{ thongTin?.tenTaiKhoan || '—' }}</span>
                  </div>
                </div>

                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Email</span>
                    <span class="info-value">{{ thongTin?.email || '—' }}</span>
                  </div>
                </div>

                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Số điện thoại</span>
                    <span class="info-value">{{ thongTin?.soDienThoai || 'Chưa cập nhật' }}</span>
                  </div>
                </div>

                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Ngày sinh</span>
                    <span class="info-value">{{ formatDate(thongTin?.ngaySinh) }}</span>
                  </div>
                </div>

                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Giới tính</span>
                    <span class="info-value">{{ thongTin?.gioiTinh || 'Chưa cập nhật' }}</span>
                  </div>
                </div>

                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Số CCCD</span>
                    <span class="info-value">{{ thongTin?.soCccd || 'Chưa cập nhật' }}</span>
                  </div>
                </div>


                <div class="col-md-6 col-lg-4">
                  <div class="info-cell">
                    <span class="info-label">Ngày tham gia</span>
                    <span class="info-value">{{ formatDateTime(thongTin?.ngayTao) }}</span>
                  </div>
                </div>

                <div class="col-12">
                  <div class="info-cell">
                    <span class="info-label">Địa chỉ mặc định</span>
                    <span class="info-value">
                      <template v-if="thongTin?.diaChiMacDinh">
                        <i class="bi bi-geo-alt text-danger me-1"></i>
                        {{ formatAddress(thongTin.diaChiMacDinh) }}
                      </template>
                      <template v-else>
                        <span class="text-muted">Chưa thiết lập địa chỉ mặc định. Bạn có thể cập nhật trong phần sổ địa chỉ hoặc khi đặt hàng.</span>
                      </template>
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- TAB 2: ĐƠN HÀNG CỦA TÔI -->
            <div v-else-if="activeTab === 'don-hang'">
              <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                <h5 class="fw-bold mb-0 text-dark">
                  <i class="bi bi-box-seam-fill text-danger me-2"></i>Lịch sử mua hàng
                </h5>
                <router-link to="/tra-cuu" class="btn btn-sm btn-outline-secondary">
                  <i class="bi bi-search me-1"></i> Tra cứu đơn hàng khác
                </router-link>
              </div>

              <div v-if="loadingOrders" class="text-center py-5 text-muted">
                <div class="spinner-border spinner-border-sm text-danger me-2" role="status"></div>
                Đang tải danh sách đơn hàng...
              </div>

              <div v-else-if="!donHangs.length" class="empty-state text-center py-5">
                <div class="empty-icon text-muted mb-3">
                  <i class="bi bi-inbox fs-1"></i>
                </div>
                <h6 class="fw-bold mb-2">Bạn chưa có đơn hàng nào</h6>
                <p class="text-muted small mb-3">Hãy dạo quanh cửa hàng và chọn cho mình những đôi giày ưng ý nhất nhé!</p>
                <router-link to="/cua-hang" class="btn btn-danger btn-sm px-4">
                  Khám phá sản phẩm
                </router-link>
              </div>

              <div v-else class="table-responsive">
                <table class="table table-hover align-middle customer-order-table">
                  <thead class="table-light">
                    <tr>
                      <th scope="col">Mã đơn hàng</th>
                      <th scope="col">Ngày đặt</th>
                      <th scope="col">Người nhận / SĐT</th>
                      <th scope="col">Tổng tiền</th>
                      <th scope="col">Trạng thái</th>
                      <th scope="col" class="text-end">Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="dh in donHangs" :key="dh.id">
                      <td>
                        <span class="fw-bold text-dark">{{ dh.maHoaDon || dh.ma || ('HD-' + dh.id) }}</span>
                      </td>
                      <td class="small text-muted">{{ formatDateTime(dh.ngayTao) }}</td>
                      <td>
                        <div class="fw-medium small">{{ dh.tenKhachHang || thongTin?.tenKhachHang || '—' }}</div>
                        <div class="text-muted small">{{ dh.soDienThoaiKhachHang || dh.soDienThoai || '—' }}</div>
                      </td>
                      <td>
                        <span class="fw-bold text-danger">{{ money(dh.tongTien ?? dh.tongTienSauGiam ?? 0) }}</span>
                      </td>
                      <td>
                        <span :class="['badge-order-status', getOrderStatusBadge(dh.trangThai).cls]">
                          {{ getOrderStatusBadge(dh.trangThai).label }}
                        </span>
                      </td>
                      <td class="text-end">
                        <router-link :to="'/don-hang/' + dh.id" class="btn btn-sm btn-outline-danger">
                          Chi tiết <i class="bi bi-arrow-right ms-1"></i>
                        </router-link>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- TAB 3: MÃ GIẢM GIÁ -->
            <div v-else-if="activeTab === 'voucher'">
              <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                <h5 class="fw-bold mb-0 text-dark">
                  <i class="bi bi-ticket-perforated-fill text-danger me-2"></i>Kho phiếu giảm giá của bạn
                </h5>
                <span class="badge bg-danger-subtle text-danger px-3 py-2">
                  {{ vouchers.length }} mã khả dụng
                </span>
              </div>

              <div v-if="loadingVouchers" class="text-center py-5 text-muted">
                <div class="spinner-border spinner-border-sm text-danger me-2" role="status"></div>
                Đang tải phiếu giảm giá...
              </div>

              <div v-else-if="!vouchers.length" class="empty-state text-center py-5">
                <div class="empty-icon text-muted mb-3">
                  <i class="bi bi-ticket-detailed fs-1"></i>
                </div>
                <h6 class="fw-bold mb-2">Hiện chưa có phiếu giảm giá nào</h6>
                <p class="text-muted small">Các ưu đãi và voucher độc quyền sẽ tự động xuất hiện tại đây khi bạn thăng hạng hoặc có chiến dịch khuyến mại!</p>
              </div>

              <div v-else class="row g-3">
                <div v-for="p in vouchers" :key="p.idPhieuGiamGia || p.id" class="col-md-6 col-xl-4">
                  <div class="voucher-ticket-card">
                    <div class="voucher-ticket-left">
                      <div class="voucher-discount-val">
                        <template v-if="p.hinhThucGiam === 'PHAN_TRAM' || p.loaiGiamGia === 'PHAN_TRAM'">
                          {{ p.giaTriGiam || p.mucGiam }}%
                        </template>
                        <template v-else>
                          {{ money(p.giaTriGiam || p.mucGiam || 0) }}
                        </template>
                      </div>
                      <div class="voucher-discount-sub">GIẢM GIÁ</div>
                    </div>

                    <div class="voucher-ticket-right p-3">
                      <div class="d-flex align-items-start justify-content-between gap-2">
                        <div class="voucher-code-title fw-bold text-dark mb-1">
                          {{ p.ma }}
                        </div>
                        <button
                          type="button"
                          :class="['btn btn-sm', copiedCode === p.ma ? 'btn-success' : 'btn-outline-danger']"
                          style="font-size: 11px; padding: 2px 8px;"
                          @click="copyVoucher(p.ma)"
                        >
                          <i :class="['bi me-1', copiedCode === p.ma ? 'bi-check2' : 'bi-clipboard']"></i>
                          {{ copiedCode === p.ma ? 'Đã chép' : 'Sao chép' }}
                        </button>
                      </div>

                      <div class="voucher-desc small text-muted mb-2">
                        {{ p.ten || 'Phiếu ưu đãi khách hàng PolyShoes' }}
                      </div>

                      <div class="voucher-meta small text-muted border-top pt-2">
                        <div v-if="p.giaTriDonHangToiThieu != null">
                          Đơn tối thiểu: <span class="fw-medium text-dark">{{ money(p.giaTriDonHangToiThieu) }}</span>
                        </div>
                        <div v-if="p.ngayKetThuc">
                          Hạn dùng: <span class="fw-medium text-dark">{{ formatDate(p.ngayKetThuc) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- TAB 4: SỔ ĐỊA CHỈ -->
            <div v-else-if="activeTab === 'dia-chi'">
              <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                <h5 class="fw-bold mb-0 text-dark">
                  <i class="bi bi-geo-alt-fill text-danger me-2"></i>Sổ địa chỉ nhận hàng
                </h5>
              </div>

              <div v-if="!thongTin?.diaChiMacDinh && (!thongTin?.danhSachDiaChi || !thongTin.danhSachDiaChi.length)" class="empty-state text-center py-5">
                <div class="empty-icon text-muted mb-3">
                  <i class="bi bi-geo-alt fs-1"></i>
                </div>
                <h6 class="fw-bold mb-2">Chưa lưu địa chỉ nào</h6>
                <p class="text-muted small">Địa chỉ của bạn sẽ được tự động lưu lại khi bạn thực hiện đặt hàng tại PolyShoes.</p>
                <router-link to="/cua-hang" class="btn btn-danger btn-sm px-4">
                  Mua sắm ngay
                </router-link>
              </div>

              <div v-else class="row g-3">
                <!-- Địa chỉ mặc định -->
                <div v-if="thongTin?.diaChiMacDinh" class="col-md-6">
                  <div class="address-box-card address-box-default p-3 border rounded shadow-sm">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                      <span class="badge bg-danger">Mặc định</span>
                      <i class="bi bi-check-circle-fill text-danger"></i>
                    </div>
                    <div class="fw-bold text-dark mb-1">
                      {{ thongTin.diaChiMacDinh.tenNguoiNhan || thongTin.tenKhachHang || 'Người nhận' }}
                    </div>
                    <div class="text-muted small mb-2">
                      <i class="bi bi-telephone me-1"></i>
                      {{ thongTin.diaChiMacDinh.soDienThoai || thongTin.soDienThoai || 'Chưa có SĐT' }}
                    </div>
                    <div class="address-text text-secondary small">
                      <i class="bi bi-geo-alt text-danger me-1"></i>
                      {{ formatAddress(thongTin.diaChiMacDinh) }}
                    </div>
                  </div>
                </div>

                <!-- Các địa chỉ khác trong danh sách -->
                <template v-if="thongTin?.danhSachDiaChi && thongTin.danhSachDiaChi.length">
                  <div
                    v-for="(dc, idx) in thongTin.danhSachDiaChi"
                    :key="dc.id || idx"
                    class="col-md-6"
                  >
                    <div v-if="!thongTin.diaChiMacDinh || dc.id !== thongTin.diaChiMacDinh.id" class="address-box-card p-3 border rounded shadow-sm">
                      <div class="d-flex align-items-center justify-content-between mb-2">
                        <span v-if="dc.macDinh" class="badge bg-danger">Mặc định</span>
                        <span v-else class="badge bg-light text-secondary border">Địa chỉ phụ</span>
                      </div>
                      <div class="fw-bold text-dark mb-1">
                        {{ dc.tenNguoiNhan || thongTin.tenKhachHang || 'Người nhận' }}
                      </div>
                      <div class="text-muted small mb-2">
                        <i class="bi bi-telephone me-1"></i>
                        {{ dc.soDienThoai || thongTin.soDienThoai || '—' }}
                      </div>
                      <div class="address-text text-secondary small">
                        <i class="bi bi-geo-alt text-danger me-1"></i>
                        {{ formatAddress(dc) }}
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- MODAL CHỈNH SỬA THÔNG TIN CÁ NHÂN -->
    <div v-if="showEditModal" class="modal-backdrop-custom d-flex align-items-center justify-content-center">
      <div class="modal-card-custom shadow-lg">
        <div class="modal-header-custom d-flex align-items-center justify-content-between p-3 border-bottom">
          <h5 class="fw-bold mb-0">
            <i class="bi bi-person-gear text-danger me-2"></i>Chỉnh sửa thông tin cá nhân
          </h5>
          <button type="button" class="btn-close" @click="dongModalSua" aria-label="Đóng"></button>
        </div>

        <form @submit.prevent="luuThongTin" class="p-4">
          <div v-if="editError" class="alert alert-danger py-2 small mb-3">
            <i class="bi bi-exclamation-triangle-fill me-1"></i>{{ editError }}
          </div>

          <div class="mb-3">
            <label class="form-label fw-medium small">Họ và tên <span class="text-danger">*</span></label>
            <input
              v-model="editForm.tenKhachHang"
              type="text"
              class="form-control"
              placeholder="Nhập họ và tên đầy đủ"
              required
            />
          </div>

          <div class="mb-3">
            <label class="form-label fw-medium small">Số điện thoại</label>
            <input
              v-model="editForm.soDienThoai"
              type="tel"
              class="form-control"
              placeholder="Ví dụ: 0912345678"
            />
          </div>

          <div class="row g-3 mb-3">
            <div class="col-sm-6">
              <label class="form-label fw-medium small">Ngày sinh</label>
              <input
                v-model="editForm.ngaySinh"
                type="date"
                class="form-control"
              />
            </div>

            <div class="col-sm-6">
              <label class="form-label fw-medium small">Giới tính</label>
              <select v-model="editForm.gioiTinh" class="form-select">
                <option value="Nam">Nam</option>
                <option value="Nữ">Nữ</option>
                <option value="Khác">Khác</option>
              </select>
            </div>
          </div>

          <div class="d-flex justify-content-end gap-2 pt-2 border-top mt-4">
            <button type="button" class="btn btn-light border px-3" @click="dongModalSua">
              Hủy bỏ
            </button>
            <button type="submit" class="btn btn-danger px-4" :disabled="savingProfile">
              <span v-if="savingProfile" class="spinner-border spinner-border-sm me-1" role="status"></span>
              {{ savingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.account-page-wrapper {
  background-color: #f8fafc;
  min-height: calc(100vh - 200px);
}

/* Guest Card */
.guest-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.guest-icon {
  font-size: 54px;
  color: #dc2626;
}
.max-w-500 {
  max-width: 500px;
}

/* Profile Hero Card */
.profile-hero-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.35);
}

.customer-name {
  font-size: 22px;
  font-weight: 750;
  color: #0f172a;
}
.badge-cust-code {
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}


/* Stat Cards */
.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08) !important;
}
.cursor-pointer {
  cursor: pointer;
}
.stat-title {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 20px;
  font-weight: 800;
}
.stat-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.stat-link {
  font-size: 12px;
  font-weight: 600;
  color: #dc2626;
}

/* Tab container */
.tab-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.custom-tabs .nav-link {
  border-radius: 10px;
  color: #64748b;
  font-weight: 600;
  font-size: 14px;
  padding: 8px 16px;
  transition: all 0.2s ease;
}
.custom-tabs .nav-link:hover {
  color: #dc2626;
  background-color: #fef2f2;
}
.custom-tabs .nav-link.active {
  background-color: #dc2626;
  color: #ffffff;
}

/* Info Cells */
.info-cell {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}
.info-value {
  font-size: 14.5px;
  color: #0f172a;
  font-weight: 600;
}

/* Table Orders */
.customer-order-table th {
  font-size: 12.5px;
  font-weight: 650;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  padding: 12px 16px;
}
.customer-order-table td {
  padding: 14px 16px;
}

/* Order Status Badges */
.badge-order-status {
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
  display: inline-block;
}
.badge-status-warning {
  background-color: #fef3c7;
  color: #92400e;
}
.badge-status-info {
  background-color: #e0f2fe;
  color: #0369a1;
}
.badge-status-primary {
  background-color: #dbeafe;
  color: #1e40af;
}
.badge-status-success {
  background-color: #dcfce7;
  color: #166534;
}
.badge-status-danger {
  background-color: #fee2e2;
  color: #991b1b;
}
.badge-status-secondary {
  background-color: #f1f5f9;
  color: #475569;
}

/* Voucher Cards */
.voucher-ticket-card {
  display: flex;
  background: #ffffff;
  border: 1px dashed #fca5a5;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
.voucher-ticket-left {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #ffffff;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 95px;
  text-align: center;
}
.voucher-discount-val {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
}
.voucher-discount-sub {
  font-size: 10px;
  letter-spacing: 1px;
  opacity: 0.85;
}
.voucher-ticket-right {
  flex: 1;
}

/* Address Cards */
.address-box-card {
  background: #ffffff;
  border-color: #e2e8f0 !important;
  transition: all 0.2s ease;
}
.address-box-default {
  border-color: #fca5a5 !important;
  background: #fffafa;
}

/* Modal */
.modal-backdrop-custom {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(2px);
  z-index: 1050;
  padding: 16px;
}
.modal-card-custom {
  background: #ffffff;
  width: 100%;
  max-width: 520px;
  border-radius: 16px;
  overflow: hidden;
  animation: modalPopIn 0.2s ease-out;
}
@keyframes modalPopIn {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
