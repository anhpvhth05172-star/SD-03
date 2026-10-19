<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CustomerAddressModal from '@/components/CustomerAddressModal.vue'
import DoiMatKhauModal from '@/components/DoiMatKhauModal.vue'
import { getCustomer, listCustomers, updateCustomerStatus, deleteCustomer } from '@/api/customer'
import { exportExcel, fetchAllPages } from '@/utils/export'
import { takeFlashWithMeta } from '@/utils/flash'
import { avatarColor, formatDate, formatDateTime, formatVnd, initialsOf } from '@/utils/format'
import { getAvatarSrc, onAvatarError } from '@/utils/avatar'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm, notifyDeleteConfirm } from '@/utils/notify'

const router = useRouter()
const flashMsg = ref('')
const exporting = ref(false)

const rows = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(5) // Khớp 5 dòng như trong ảnh mẫu

const fKeyword = ref('')
const fTrangThai = ref('')
const detailData = ref(null)

let searchTimeout = null
const onKeywordInput = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    applyFilter()
  }, 350)
}

const buildParams = () => {
  const params = { page: page.value, size: size.value }
  if (fKeyword.value.trim()) params.keyword = fKeyword.value.trim()
  if (fTrangThai.value) params.trangThai = fTrangThai.value
  return params
}

const load = async () => {
  const params = buildParams()
  loading.value = true
  try {
    const data = await listCustomers(params)
    const maxPage = data.totalPages > 0 ? data.totalPages - 1 : 0
    if (page.value > maxPage) {
      page.value = maxPage
      loading.value = false
      return await load()
    }
    rows.value = data.content || []
    totalElements.value = data.totalElements || 0
    totalPages.value = data.totalPages || 0
  } catch (e) {
    notifyError('Lỗi tải dữ liệu', e.message)
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
  fTrangThai.value = ''
  page.value = 0
  load()
}

const onSizeChange = () => {
  page.value = 0
  load()
}

const goPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return
  page.value = p
  load()
}

const getDefaultAddress = (row) => {
  if (!row) return null
  if (row.diaChiMacDinh) return row.diaChiMacDinh
  if (row.danhSachDiaChi && row.danhSachDiaChi.length) {
    const md = row.danhSachDiaChi.find((d) => d.macDinh)
    return md || row.danhSachDiaChi[0]
  }
  return null
}

const formatCustomerAddress = (row) => {
  const d = getDefaultAddress(row)
  if (!d) return '—'
  const parts = [d.diaChiCuThe, d.phuong, d.thanhPho].filter(Boolean)
  return parts.length ? parts.join(', ') : '—'
}

const formatCustomerAddressTooltip = (row) => {
  const ds =
    row.danhSachDiaChi && row.danhSachDiaChi.length
      ? row.danhSachDiaChi
      : row.diaChiMacDinh
        ? [row.diaChiMacDinh]
        : []
  if (!ds.length) return 'Chưa cập nhật địa chỉ'
  if (ds.length === 1) {
    const parts = [ds[0].diaChiCuThe, ds[0].phuong, ds[0].thanhPho].filter(Boolean)
    return parts.length ? parts.join(', ') : '—'
  }
  return ds
    .map((d, i) => {
      const parts = [d.diaChiCuThe, d.phuong, d.thanhPho].filter(Boolean)
      const addr = parts.length ? parts.join(', ') : '—'
      const ten = d.tenChiChi && d.tenChiChi !== row.tenKhachHang ? ` (${d.tenChiChi})` : ''
      const def = d.macDinh ? ' [Mặc định]' : ''
      return `${i + 1}. ${addr}${ten}${def}`
    })
    .join('\n')
}

const diaChiText = computed(() => {
  if (!detailData.value) return ''
  return formatCustomerAddress(detailData.value)
})

const exportFile = async () => {
  const params = buildParams()
  exporting.value = true
  try {
    const all = await fetchAllPages(listCustomers, params)
    await exportExcel(
      'danh-sach-khach-hang.xlsx',
      ['STT', 'Mã KH', 'Họ và tên', 'Email', 'Địa chỉ', 'Số điện thoại', 'Trạng thái'],
      all.map((r, i) => [
        i + 1,
        r.maKhachHang || '',
        r.tenKhachHang || '',
        r.email || '',
        formatCustomerAddress(r),
        String(r.soDienThoai || ''),
        r.trangThai ? 'Hoạt động' : 'Đã khóa',
      ]),
      { sheetName: 'Danh sách khách hàng' }
    )
    notifySuccess('Đã xuất danh sách khách hàng thành công!')
  } catch (e) {
    notifyError('Xuất file thất bại', e.message)
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

const openDetail = async (row) => {
  try {
    detailData.value = await getCustomer(row.id)
  } catch (e) {
    notifyError('Lỗi lấy chi tiết', e.message)
  }
}

const toggleStatus = async (row) => {
  const actionText = row.trangThai ? 'khóa' : 'kích hoạt'
  const ok = await notifyConfirm(
    `Xác nhận ${actionText} tài khoản`,
    `Bạn có chắc chắn muốn ${actionText} tài khoản khách hàng "${row.tenKhachHang}"?`,
    'Xác nhận',
    'Hủy bỏ'
  )
  if (!ok) return

  const moi = !row.trangThai
  try {
    await updateCustomerStatus(row.id, moi ? 'true' : 'false')
    row.trangThai = moi
    row.trangThaiLabel = moi ? 'Đang hoạt động' : 'Đã khóa'
    notifySuccess(`Đã ${actionText} tài khoản "${row.tenKhachHang}" thành công!`)
    await load()
  } catch (e) {
    notifyError('Thao tác thất bại', e.message)
  }
}

const removeCustomer = async (row) => {
  const ok = await notifyDeleteConfirm(row.tenKhachHang || row.maKhachHang)
  if (!ok) return
  try {
    await deleteCustomer(row.id)
    notifySuccess(`Đã xóa khách hàng "${row.tenKhachHang}" thành công!`)
    await load()
  } catch (e) {
    notifyError('Không thể xóa khách hàng', e.message)
  }
}

const diaChiCustomerId = ref(null)
const doiMkCustomerId = ref(null)
const openAddress = (row) => {
  diaChiCustomerId.value = row.id
}
const moSoDiaChi = (row) => {
  if (!row) return
  detailData.value = null
  openAddress(row)
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


onMounted(() => {
  const { message, type } = takeFlashWithMeta()
  flashMsg.value = message
  if (message) {
    if (type === 'warning') {
      notifyWarning(message)
    } else if (type === 'error') {
      notifyError(message)
    } else {
      notifySuccess(message)
    }
  }
  load()
})
</script>

<template>
  <div class="customer-page-container">
    <div class="filter-card">
      <div class="card-title-row">
        <div class="icon-circle icon-filter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
            stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
          </svg>
        </div>
        <span class="card-title-text">Bộ lọc</span>
      </div>

      <div class="filter-controls-row">
        <div class="search-input-wrap">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input v-model="fKeyword" type="text" class="input-search"
            placeholder="Tìm theo tên đăng nhập, họ tên, email, SĐT..." @input="onKeywordInput"
            @keyup.enter="applyFilter" />
        </div>

        <div class="select-status-wrap">
          <select v-model="fTrangThai" class="select-status" @change="applyFilter">
            <option value="">Tất cả</option>
            <option value="active">Hoạt động</option>
            <option value="locked">Đã khóa</option>
          </select>
        </div>
        <button class="btn-action-outline" type="button" @click="resetFilter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
            stroke-linejoin="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
          </svg>
          Đặt lại bộ lọc
        </button>
        <button class="btn-action-outline" type="button" :disabled="exporting" @click="exportFile">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
            stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
        </button>
        <button class="btn-action-primary" type="button" @click="router.push('/khach-hang/them')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"
            stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Thêm khách hàng
        </button>
      </div>
    </div>

    <div class="table-card">
      <div class="card-title-row">
        <div class="card-title-left">
          <div class="icon-circle icon-list">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
              stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
          </div>
          <span class="card-title-text">Danh sách khách hàng</span>
          <span class="items-count-pill">{{ rows.length }}/{{ totalElements }} khách hàng</span>
        </div>
        <div class="card-title-right">
          <span class="page-size-label">Hiển thị:</span>
          <select v-model="size" class="page-size-select" @change="onSizeChange">
            <option :value="5">5 bản ghi/trang</option>
            <option :value="10">10 bản ghi/trang</option>
            <option :value="20">20 bản ghi/trang</option>
            <option :value="50">50 bản ghi/trang</option>
          </select>
        </div>
      </div>

      <div class="table-container">
        <table class="customer-table">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th style="width: 65px; text-align: center;">Ảnh</th>
              <th style="width: 170px;">Họ tên</th>
              <th style="width: 200px;">Email</th>
              <th>Địa chỉ</th>
              <th style="width: 130px;">Số điện thoại</th>
              <th style="width: 120px; text-align: center;">Trạng thái</th>
              <th style="width: 205px; text-align: center;">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="text-center py-5 text-muted">Đang tải dữ liệu...</td>
            </tr>
            <tr v-else-if="rows.length === 0">
              <td colspan="8" class="text-center py-5 text-muted">Không tìm thấy khách hàng nào</td>
            </tr>
            <template v-else>
              <tr v-for="(row, index) in rows" :key="row.id">
                <td class="cell-stt">{{ (page * size) + index + 1 }}</td>
                <td class="text-center">
                  <div class="customer-avatar has-img">
                    <img :src="getAvatarSrc(row)" :alt="row.tenKhachHang" @error="onAvatarError($event, row)" />
                  </div>
                </td>
                <td class="cell-name">{{ row.tenKhachHang }}</td>
                <td class="cell-email">{{ row.email || '—' }}</td>
                <td class="cell-address">
                  <span class="address-clamp" :title="formatCustomerAddressTooltip(row)">
                    {{ formatCustomerAddress(row) }}
                  </span>
                  <span
                    v-if="row.danhSachDiaChi && row.danhSachDiaChi.length > 1"
                    class="extra-addr-tag"
                    :title="formatCustomerAddressTooltip(row)"
                  >
                    +{{ row.danhSachDiaChi.length - 1 }} địa chỉ
                  </span>
                </td>
                <td class="cell-phone">{{ row.soDienThoai || '—' }}</td>
                <td class="text-center">
                  <span class="status-pill" :class="row.trangThai ? 'is-active' : 'is-locked'">
                    {{ row.trangThai ? 'Hoạt động' : 'Đã khóa' }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="action-buttons-group">
                    <button class="btn-icon is-power" type="button"
                      :title="row.trangThai ? 'Khóa tài khoản' : 'Kích hoạt tài khoản'" @click="toggleStatus(row)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
                        stroke-linecap="round" stroke-linejoin="round">
                        <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                        <line x1="12" y1="2" x2="12" y2="12"></line>
                      </svg>
                    </button>
                    <button class="btn-icon is-location" type="button" title="Quản lý địa chỉ"
                      @click="openAddress(row)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
                        stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                        <circle cx="12" cy="10" r="3"></circle>
                      </svg>
                    </button>
                    <button class="btn-icon is-view" type="button" title="Xem chi tiết" aria-label="Xem chi tiết"
                      @click="openDetail(row)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
                        stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="4" width="18" height="16" rx="2"></rect>
                        <circle cx="9" cy="10" r="2"></circle>
                        <path d="M6 16c0-1.7 1.3-3 3-3s3 1.3 3 3"></path>
                        <line x1="15" y1="9" x2="18" y2="9"></line>
                        <line x1="15" y1="13" x2="18" y2="13"></line>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Phân trang Footer -->
      <div v-if="totalElements > 0" class="table-card-foot">
        <span class="foot-info">
          Hiển thị <strong>{{ rows.length ? page * size + 1 : 0 }} - {{ page * size + rows.length }}</strong> trong tổng số
          <strong>{{ totalElements }}</strong> khách hàng
        </span>
        <div v-if="totalPages > 1" class="foot-pager">
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
          <button
            class="pager-btn"
            type="button"
            :disabled="page + 1 >= totalPages"
            @click="goPage(page + 1)"
          >
            Sau ›
          </button>
        </div>
      </div>
    </div>

    <div v-if="detailData" class="modal-backdrop" @click.self="detailData = null">
      <div class="profile-modal-card">
        <div class="profile-modal-banner customer-theme">
          <button class="btn-banner-close" type="button" title="Đóng" @click="detailData = null">
            <i class="bi bi-x-lg"></i>
          </button>

          <div class="profile-banner-content">
            <div class="profile-avatar-wrapper customer">
              <img :src="getAvatarSrc(detailData)" :alt="detailData.tenKhachHang" class="profile-avatar-img"
                @error="onAvatarError($event, detailData)" />
            </div>

            <div class="profile-meta-info">
              <div class="profile-header-badges">
                <span class="badge-code customer">{{ detailData.maKhachHang }}</span>
                <span class="badge-role customer">{{ detailData.hangThanhVien || 'Thành viên' }}</span>
                <span class="status-pill" :class="detailData.trangThai ? 'is-active' : 'is-locked'">
                  {{ detailData.trangThai ? 'Hoạt động' : 'Đã khóa' }}
                </span>
              </div>
              <h3 class="profile-name">{{ detailData.tenKhachHang }}</h3>
              <p class="profile-email-sub"><i class="bi bi-envelope"></i> {{ detailData.email || 'Chưa cập nhật email'
              }}
              </p>
            </div>
          </div>
        </div>

        <!-- Thân Modal: Các nhóm thông tin -->
        <div class="profile-modal-body">
          <!-- 2 thẻ thống kê nhanh: Số đơn & Chi tiêu -->
          <div class="customer-stats-row">
            <div class="stat-card">
              <div class="stat-icon-circle blue">
                <i class="bi bi-bag-check-fill"></i>
              </div>
              <div class="stat-info">
                <span class="stat-label">Tổng đơn mua</span>
                <h4 class="stat-value">{{ detailData.soDon || 0 }} <span class="stat-unit">đơn</span></h4>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon-circle green">
                <i class="bi bi-wallet2"></i>
              </div>
              <div class="stat-info">
                <span class="stat-label">Tổng chi tiêu</span>
                <h4 class="stat-value text-success">{{ formatVnd(detailData.tongChiTieu) }}</h4>
              </div>
            </div>
          </div>
          <div class="info-section-group">
            <h4 class="section-group-title">
              <i class="bi bi-person-lines-fill"></i> Thông tin liên hệ & Tài khoản
            </h4>
            <div class="info-grid-2">
              <div class="info-cell">
                <span class="info-cell-label"><i class="bi bi-telephone"></i> Số điện thoại</span>
                <span class="info-cell-value">{{ detailData.soDienThoai || 'Chưa cập nhật' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label"><i class="bi bi-gender-ambiguous"></i> Giới tính</span>
                <span class="info-cell-value">{{ gioiTinhLabel(detailData.gioiTinh) }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label"><i class="bi bi-cake2"></i> Ngày sinh</span>
                <span class="info-cell-value">{{ formatDate(detailData.ngaySinh) || '—' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label"><i class="bi bi-person-badge"></i> Tên đăng nhập</span>
                <span class="info-cell-value">{{ detailData.tenTaiKhoan || detailData.email || '—' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label"><i class="bi bi-card-heading"></i> Số CCCD/CMND</span>
                <span class="info-cell-value">{{ detailData.soCccd || 'Chưa cập nhật' }}</span>
              </div>
            </div>
          </div>

          <div class="info-section-group">
            <h4 class="section-group-title">
              <i class="bi bi-geo-alt-fill text-danger"></i> Địa chỉ giao hàng
            </h4>
            <div class="address-box-card">
              <p class="address-text">{{ diaChiText || 'Chưa cập nhật địa chỉ giao hàng' }}</p>
            </div>
          </div>

          <div class="modal-time-footer">
            <span><i class="bi bi-calendar3"></i> Ngày đăng ký: {{ formatDate(detailData.ngayTao) }}</span>
            <span><i class="bi bi-clock-history"></i> Cập nhật: {{ formatDateTime(detailData.ngayCapNhat) || '—'
            }}</span>
          </div>
        </div>

        <div class="profile-modal-actions">
          <button class="btn-profile-address" type="button" @click="moSoDiaChi(detailData)">
            <i class="bi bi-geo-alt"></i>
            <span>Sổ địa chỉ</span>
          </button>
          <button class="btn-profile-edit customer" type="button"
            @click="router.push(`/khach-hang/${detailData.id}/sua`)"
            style="background: linear-gradient(135deg, #0284c7, #2563eb); box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);">
            <i class="bi bi-pencil-square"></i>
            <span>Chỉnh sửa thông tin</span>
          </button>
          <button class="btn-profile-edit customer" type="button"
            @click="doiMkCustomerId = detailData.id"
            style="background: linear-gradient(135deg, #d97706, #f59e0b); box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);">
            <i class="bi bi-key"></i>
            <span>Đổi mật khẩu</span>
          </button>
          <button class="btn-profile-delete" type="button" @click="removeCustomer(detailData); detailData = null">
            <i class="bi bi-trash3"></i>
            <span>Xóa tài khoản</span>
          </button>
          <button class="btn-profile-close" type="button" @click="detailData = null">
            Đóng
          </button>
        </div>
      </div>
    </div>
    <CustomerAddressModal v-if="diaChiCustomerId" :customer-id="diaChiCustomerId" @close="dongDiaChi"
      @changed="doiDiaChi" />
    <DoiMatKhauModal v-if="doiMkCustomerId" loai="KH" :id="doiMkCustomerId"
      :ten-tai-khoan="detailData?.tenTaiKhoan || detailData?.email || ''" @close="doiMkCustomerId = null"
      @success="detailData = null" />
  </div>
</template>

<style scoped>
.customer-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

/* Card Chung */
.filter-card,
.table-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  padding: 20px 24px;
}

/* Header của Card */
.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.card-title-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.items-count-pill {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
}

.page-size-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.page-size-select {
  padding: 6px 30px 6px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E") no-repeat right 8px center;
  background-size: 14px;
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  cursor: pointer;
  outline: none;
  appearance: none;
  transition: all 0.2s ease;
}

.page-size-select:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
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

.icon-list {
  background: #ffe4e6;
  color: #f43f5e;
}

.card-title-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.filter-controls-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input-wrap {
  flex: 1;
  min-width: 280px;
  position: relative;
  display: flex;
  align-items: center;
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
  height: 40px;
  padding: 0 16px 0 40px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
}

.input-search:focus {
  border-color: #f43f5e;
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.input-search::placeholder {
  color: #94a3b8;
}

.select-status-wrap {
  flex-shrink: 0;
}

.select-status {
  height: 40px;
  min-width: 110px;
  padding: 0 28px 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13.5px;
  color: #334155;
  background: #ffffff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") no-repeat right 10px center;
  background-size: 14px;
  appearance: none;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.select-status:focus {
  border-color: #f43f5e;
}

.btn-action-outline {
  height: 40px;
  padding: 0 16px;
  border: 1px solid #fca5a5;
  background: #ffffff;
  color: #ef4444;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.btn-action-outline svg {
  width: 15px;
  height: 15px;
}

.btn-action-outline:hover {
  background: #fef2f2;
  border-color: #ef4444;
  color: #dc2626;
}

/* Nút đỏ chính (Thêm khách hàng) */
.btn-action-primary {
  height: 40px;
  padding: 0 18px;
  border: none;
  background: #ff2d55;
  color: #ffffff;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(255, 45, 85, 0.32);
  transition: all 0.2s ease;
}

.btn-action-primary svg {
  width: 15px;
  height: 15px;
}

.btn-action-primary:hover {
  background: #e11d48;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 45, 85, 0.4);
}

/* Bảng dữ liệu */
.table-container {
  overflow-x: auto;
  width: 100%;
}

.customer-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.customer-table th {
  padding: 13px 14px;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  white-space: nowrap;
}

.customer-table td {
  padding: 14px 14px;
  font-size: 13.5px;
  color: #334155;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.customer-table tbody tr:hover {
  background-color: #fafbfc;
}

.cell-stt {
  color: #475569;
  font-weight: 500;
}

.cell-name {
  font-weight: 600;
  color: #0f172a;
}

.cell-email {
  color: #475569;
}

.cell-phone {
  color: #334155;
  font-weight: 500;
}

.cell-address {
  color: #475569;
  font-size: 13px;
  line-height: 1.45;
}

.address-clamp {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
  line-height: 1.45;
}

.extra-addr-tag {
  display: inline-block;
  margin-top: 4px;
  padding: 1px 7px;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: default;
}

/* Avatar tròn */
.customer-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12.5px;
  font-weight: 700;
  color: #ffffff;
  text-transform: uppercase;
  margin: 0 auto;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  background: #f1f5f9;
}

.customer-avatar.has-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}

/* Status Pill */
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-pill.is-active {
  background: #ecfdf5;
  color: #10b981;
  border: 1px solid #a7f3d0;
}

.status-pill.is-locked {
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fecaca;
}

/* Cụm nút hành động hình tròn nhỏ */
.action-buttons-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
}

.btn-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-icon svg {
  width: 14px;
  height: 14px;
}

/* Nút Power (đỏ/hồng) */
.btn-icon.is-power {
  background: #fff1f2;
  color: #f43f5e;
  border: 1px solid #ffe4e6;
}

.btn-icon.is-power:hover {
  background: #f43f5e;
  color: #ffffff;
}

/* Nút Location Pin (xanh dương) */
.btn-icon.is-location {
  background: #f0f9ff;
  color: #0284c7;
  border: 1px solid #e0f2fe;
}

.btn-icon.is-location:hover {
  background: #0284c7;
  color: #ffffff;
}

/* Nút Edit (tím) */
.btn-icon.is-edit {
  background: #f5f3ff;
  color: #8b5cf6;
  border: 1px solid #ede9fe;
}

.btn-icon.is-edit:hover {
  background: #8b5cf6;
  color: #ffffff;
}

/* Nút View (xám xanh) */
.btn-icon.is-view {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.btn-icon.is-view:hover {
  background: #64748b;
  color: #ffffff;
}

/* Nút Delete (đỏ tươi) */
.btn-icon.is-delete {
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fee2e2;
}

.btn-icon.is-delete:hover {
  background: #ef4444;
  color: #ffffff;
}

/* Phân trang Footer (chuẩn hóa giống trang Quản lý sản phẩm) */
.table-card-foot {
  padding: 14px 4px 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
  margin-top: 16px;
}

.foot-info {
  font-size: 12.5px;
  color: #64748b;
}

.foot-info strong {
  color: #1e293b;
  font-weight: 700;
}

.foot-pager {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pager-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1.4;
}

.pager-btn:hover:not(:disabled) {
  background: #f8fafc;
  color: #0f172a;
  border-color: #cbd5e1;
}

.pager-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pager-btn.num-btn {
  min-width: 32px;
  text-align: center;
  padding: 5px 0;
}

.pager-btn.active {
  background: #dc2626;
  color: #ffffff;
  border-color: #dc2626;
}

/* Modal Xem Chi Tiết */
/* ==========================================================================
   PROFILE MODAL THIẾT KẾ HIỆN ĐẠI & SANG TRỌNG
   ========================================================================== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  backdrop-filter: blur(4px);
  padding: 16px;
}

.profile-modal-card {
  background: #ffffff;
  width: 100%;
  max-width: 580px;
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  animation: modalCardIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalCardIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(14px);
  }

  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* Banner Header */
.profile-modal-banner {
  position: relative;
  padding: 24px 24px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.profile-modal-banner.customer-theme {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #bfdbfe 100%);
}

.btn-banner-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.btn-banner-close:hover {
  background: #ffffff;
  color: #0f172a;
  transform: rotate(90deg);
}

.profile-banner-content {
  display: flex;
  align-items: center;
  gap: 18px;
}

.profile-avatar-wrapper {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  border: 3px solid #ffffff;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-initials.customer {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #ffffff;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.profile-meta-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.profile-header-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.badge-code.customer {
  font-size: 11.5px;
  font-weight: 700;
  color: #2563eb;
  background: #ffffff;
  padding: 2px 8px;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.badge-role.customer {
  font-size: 11.5px;
  font-weight: 600;
  color: #0369a1;
  background: rgba(255, 255, 255, 0.7);
  padding: 2px 8px;
  border-radius: 6px;
}

.profile-name {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.profile-email-sub {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Modal Body */
.profile-modal-body {
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 2 Thẻ thống kê Khách hàng */
.customer-stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-card {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon-circle {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.stat-icon-circle.blue {
  background: #eff6ff;
  color: #2563eb;
}

.stat-icon-circle.green {
  background: #ecfdf5;
  color: #059669;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 11.5px;
  color: #64748b;
  font-weight: 500;
}

.stat-value {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.stat-unit {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.info-section-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-group-title {
  margin: 0;
  font-size: 12.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-group-title i {
  font-size: 14px;
  color: #2563eb;
}

.info-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.info-cell {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-cell-label {
  font-size: 11.5px;
  font-weight: 500;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 5px;
}

.info-cell-label i {
  font-size: 12px;
}

.info-cell-value {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
}

.address-box-card {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 12px 14px;
}

.address-text {
  margin: 0;
  font-size: 13.5px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.5;
  white-space: pre-line;
}

.modal-time-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: #94a3b8;
  padding-top: 6px;
  border-top: 1px dashed #f1f5f9;
}

.modal-time-footer i {
  margin-right: 4px;
}

/* Modal Actions */
.profile-modal-actions {
  padding: 14px 24px 20px;
  background: #ffffff;
  border-top: 1px solid #f8fafc;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.btn-profile-address {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: 9999px;
  background: #f0f9ff;
  color: #0284c7;
  border: 1px solid #bae6fd;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-profile-address:hover {
  background: #e0f2fe;
}

.btn-profile-edit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 9999px;
  color: #ffffff;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-profile-edit:hover {
  opacity: 0.94;
  transform: translateY(-1px);
}

.btn-profile-delete {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 9999px;
  background: #ffffff;
  color: #ef4444;
  border: 1px solid #fecaca;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-profile-delete:hover {
  background: #fef2f2;
  border-color: #f87171;
}

.btn-profile-close {
  padding: 9px 20px;
  border-radius: 9999px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-profile-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}
</style>
