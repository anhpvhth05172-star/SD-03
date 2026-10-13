<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteEmployee, getEmployee, getEmployeeFormData, listEmployees, updateEmployeeStatus } from '@/api/employee'
import { exportCsv, fetchAllPages } from '@/utils/export'
import { takeFlashWithMeta } from '@/utils/flash'
import { avatarColor, formatDate, formatDateTime, initialsOf } from '@/utils/format'
import { getAvatarSrc, onAvatarError } from '@/utils/avatar'
import { notifySuccess, notifyWarning, notifyError, notifyConfirm, notifyDeleteConfirm } from '@/utils/notify'

const router = useRouter()
const flashMsg = ref('')
const exporting = ref(false)

const vaiTros = ref([])
const rows = ref([])
const loading = ref(false)
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(5) // Khớp 5 dòng như trong ảnh mẫu

const fKeyword = ref('')
const fVaiTro = ref('')
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
  if (fVaiTro.value) params.idVaiTro = fVaiTro.value
  if (fTrangThai.value) params.trangThai = fTrangThai.value
  return params
}

const load = async () => {
  const params = buildParams()
  if (!params) return
  loading.value = true
  try {
    const data = await listEmployees(params)
    const maxPage = data.totalPages > 0 ? data.totalPages - 1 : 0
    if (page.value > maxPage) {
      page.value = maxPage
      loading.value = false
      return await load()
    }
    rows.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
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
  fVaiTro.value = ''
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

const formatEmployeeAddress = (row) => {
  const parts = [row.diaChiCuThe, row.phuong, row.queQuan].filter(Boolean)
  return parts.length ? parts.join(', ') : '—'
}

const gioiTinhLabel = (value) => {
  if (value === 'NAM' || value === 'Nam') return 'Nam'
  if (value === 'NU' || value === 'Nu' || value === 'Nữ') return 'Nữ'
  return value || '—'
}

const exportFile = async () => {
  const params = buildParams()
  if (!params) return
  exporting.value = true
  try {
    const all = await fetchAllPages(listEmployees, params)
    exportCsv(
      'danh-sach-nhan-vien.csv',
      ['STT', 'Mã NV', 'Họ tên', 'Email', 'Giới tính', 'Số điện thoại', 'Địa chỉ', 'Vai trò', 'Trạng thái'],
      all.map((r, i) => [
        i + 1,
        r.maNhanVien,
        r.tenTaiKhoan,
        r.email || '',
        gioiTinhLabel(r.gioiTinh),
        r.soDienThoai || '',
        formatEmployeeAddress(r),
        r.tenVaiTro || '',
        r.trangThai ? 'Hoạt động' : 'Đã khóa',
      ]),
    )
    notifySuccess('Đã xuất danh sách nhân viên thành công!')
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
    detailData.value = await getEmployee(row.id)
  } catch (e) {
    notifyError('Lỗi lấy chi tiết', e.message)
  }
}

const isAccountProtected = (row) => {
  if (!row) return false
  const maVt = String(row.maVaiTro || '').toUpperCase()
  const tenVt = String(row.tenVaiTro || '').toLowerCase()
  const maNv = String(row.maNhanVien || '').toUpperCase()
  const tenTk = String(row.tenTaiKhoan || '').toLowerCase()
  return (
    maVt === 'ADMIN' ||
    tenVt.includes('quản trị') ||
    tenVt.includes('admin') ||
    maNv.startsWith('ADMIN') ||
    tenTk.startsWith('admin') ||
    row.idVaiTro === 1
  )
}

const toggleStatus = async (row) => {
  if (isAccountProtected(row)) {
    notifyWarning('Tài khoản Quản trị viên không thể thay đổi trạng thái')
    return
  }
  const actionText = row.trangThai ? 'khóa' : 'kích hoạt'
  const ok = await notifyConfirm(
    `Xác nhận ${actionText} tài khoản`,
    `Bạn có chắc chắn muốn ${actionText} tài khoản nhân viên "${row.tenTaiKhoan}"?`,
    'Xác nhận',
    'Hủy bỏ'
  )
  if (!ok) return

  const moi = !row.trangThai
  try {
    await updateEmployeeStatus(row.id, moi ? 'true' : 'false')
    row.trangThai = moi
    row.trangThaiLabel = moi ? 'Đang hoạt động' : 'Đã khóa'
    notifySuccess(`Đã ${actionText} tài khoản "${row.tenTaiKhoan}" thành công!`)
    await load()
  } catch (e) {
    notifyError('Thao tác thất bại', e.message)
  }
}

const removeEmployee = async (row) => {
  const ok = await notifyDeleteConfirm(row.tenTaiKhoan || row.maNhanVien)
  if (!ok) return
  try {
    await deleteEmployee(row.id)
    notifySuccess(`Đã xóa nhân viên "${row.tenTaiKhoan}" thành công!`)
    await load()
  } catch (e) {
    notifyError('Không thể xóa nhân viên', e.message)
  }
}

onMounted(async () => {
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
  try {
    const formData = await getEmployeeFormData()
    vaiTros.value = formData.vaiTros || []
  } catch {
    vaiTros.value = []
  }
  load()
})
</script>

<template>
  <div class="employee-page-container">
    <!-- Card 1: BỘ LỌC -->
    <div class="filter-card">
      <div class="card-title-row">
        <div class="icon-circle icon-filter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
          </svg>
        </div>
        <span class="card-title-text">Bộ lọc</span>
      </div>

      <!-- Hàng 1: Các ô lọc tìm kiếm -->
      <div class="filter-controls-row">
        <!-- Input tìm kiếm -->
        <div class="search-input-wrap">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            v-model="fKeyword"
            type="text"
            class="input-search"
            placeholder="Tìm theo mã, họ tên, tài khoản, SĐT..."
            @input="onKeywordInput"
            @keyup.enter="applyFilter"
          />
        </div>

        <!-- Dropdown Vai trò -->
        <div class="select-wrap">
          <select v-model="fVaiTro" class="filter-select" @change="applyFilter">
            <option value="">Tất cả vai trò</option>
            <option v-for="vt in vaiTros" :key="vt.id" :value="vt.id">{{ vt.ten }}</option>
          </select>
        </div>

        <!-- Dropdown Trạng thái -->
        <div class="select-wrap">
          <select v-model="fTrangThai" class="filter-select" @change="applyFilter">
            <option value="">Tất cả</option>
            <option value="active">Hoạt động</option>
            <option value="locked">Đã khóa</option>
          </select>
        </div>
      </div>

      <!-- Hàng 2: Các nút hành động bên phải -->
      <div class="filter-actions-row">
        <!-- Nút Đặt lại bộ lọc -->
        <button class="btn-action-outline" type="button" @click="resetFilter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
          </svg>
          Đặt lại bộ lọc
        </button>

        <!-- Nút Xuất Excel -->
        <button class="btn-action-outline" type="button" :disabled="exporting" @click="exportFile">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
          {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
        </button>

        <!-- Nút Thêm nhân viên -->
        <button class="btn-action-primary" type="button" @click="router.push('/nhan-vien/them')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Thêm nhân viên
        </button>
      </div>
    </div>

    <!-- Card 2: DANH SÁCH NHÂN VIÊN -->
    <div class="table-card">
      <div class="card-title-row">
        <div class="icon-circle icon-users">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <span class="card-title-text">Danh sách nhân viên</span>
      </div>

      <div class="table-container">
        <table class="employee-table">
          <thead>
            <tr>
              <th style="width: 45px;">STT</th>
              <th style="width: 60px; text-align: center;">Ảnh</th>
              <th style="width: 105px;">Mã NV</th>
              <th style="width: 160px;">Họ tên</th>
              <th style="width: 175px;">Email</th>
              <th style="width: 80px;">Giới tính</th>
              <th style="width: 115px;">SĐT</th>
              <th>Địa chỉ</th>
              <th style="width: 120px;">Vai trò</th>
              <th style="width: 110px; text-align: center;">Trạng thái</th>
              <th style="width: 150px; text-align: center;">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="11" class="text-center py-5 text-muted">Đang tải dữ liệu...</td>
            </tr>
            <tr v-else-if="rows.length === 0">
              <td colspan="11" class="text-center py-5 text-muted">Không tìm thấy nhân viên nào</td>
            </tr>
            <template v-else>
              <tr v-for="(row, index) in rows" :key="row.id">
                <td class="cell-stt">{{ (page * size) + index + 1 }}</td>
                <td class="text-center">
                  <div class="employee-avatar has-img">
                    <img
                      :src="getAvatarSrc(row)"
                      :alt="row.tenTaiKhoan"
                      @error="onAvatarError($event, row)"
                    />
                  </div>
                </td>
                <td class="cell-code">{{ row.maNhanVien }}</td>
                <td class="cell-name">{{ row.tenTaiKhoan }}</td>
                <td class="cell-email" :title="row.email || '—'">{{ row.email || '—' }}</td>
                <td class="cell-gender">{{ gioiTinhLabel(row.gioiTinh) }}</td>
                <td class="cell-phone">{{ row.soDienThoai || '—' }}</td>
                <td class="cell-address">
                  <span class="address-clamp" :title="formatEmployeeAddress(row)">
                    {{ formatEmployeeAddress(row) }}
                  </span>
                </td>
                <td class="cell-role">{{ row.tenVaiTro || '—' }}</td>
                <td class="text-center">
                  <span class="status-pill" :class="row.trangThai ? 'is-active' : 'is-locked'">
                    {{ row.trangThai ? 'Hoạt động' : 'Đã khóa' }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="action-buttons-group">
                    <!-- Nút 1: On/Off (Power) -->
                    <button
                      class="btn-action-circle"
                      :class="isAccountProtected(row) ? 'is-disabled' : 'is-power-active'"
                      type="button"
                      :disabled="isAccountProtected(row)"
                      :title="isAccountProtected(row) ? 'Tài khoản Quản trị viên (không thể đổi trạng thái)' : (row.trangThai ? 'Khóa tài khoản' : 'Kích hoạt tài khoản')"
                      @click="!isAccountProtected(row) ? toggleStatus(row) : null"
                    >
                      <i class="bi bi-power"></i>
                    </button>

                    <!-- Nút 2: Mắt (Xem chi tiết) -->
                    <button
                      class="btn-action-circle is-view"
                      type="button"
                      title="Xem chi tiết"
                      @click="openDetail(row)"
                    >
                      <i class="bi bi-eye"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Phân trang Footer -->
      <div class="table-pagination-row">
        <!-- Bên giữa: Tổng số dòng theo bộ lọc -->
        <div class="pagination-summary">
          Hiển thị <strong>{{ rows.length }}</strong> nhân viên trong tổng số
          <strong>{{ totalElements }}</strong> nhân viên
        </div>
        <!-- Bên trái: Số dòng mỗi trang -->
        <div class="pagination-size-wrap">
          <select v-model="size" class="pagination-select" @change="onSizeChange">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
          </select>
        </div>

        <!-- Bên phải: Các nút phân trang -->
        <div class="pagination-nav-wrap">
          <!-- Đầu trang -->
          <button
            class="pagination-nav-btn"
            type="button"
            title="Đầu trang"
            :disabled="page === 0"
            @click="goPage(0)"
          >
            K
          </button>

          <!-- Trang trước -->
          <button
            class="pagination-nav-btn"
            type="button"
            title="Trang trước"
            :disabled="page === 0"
            @click="goPage(page - 1)"
          >
            &lt;
          </button>

          <!-- Các nút số trang -->
          <button
            v-for="p in pageList"
            :key="p"
            class="pagination-num-btn"
            :class="{ 'is-active': p === page + 1 }"
            type="button"
            @click="goPage(p - 1)"
          >
            {{ p }}
          </button>

          <!-- Trang sau -->
          <button
            class="pagination-nav-btn"
            type="button"
            title="Trang sau"
            :disabled="page + 1 >= totalPages"
            @click="goPage(page + 1)"
          >
            &gt;
          </button>

          <!-- Cuối trang -->
          <button
            class="pagination-nav-btn"
            type="button"
            title="Cuối trang"
            :disabled="page + 1 >= totalPages"
            @click="goPage(totalPages - 1)"
          >
            &gt;|
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Chi tiết Nhân viên (Profile Card Hiện đại, Đẹp mắt) -->
    <div v-if="detailData" class="modal-backdrop" @click.self="detailData = null">
      <div class="profile-modal-card">
        <!-- Banner đầu trang -->
        <div class="profile-modal-banner employee-theme">
          <button class="btn-banner-close" type="button" title="Đóng" @click="detailData = null">
            <i class="bi bi-x-lg"></i>
          </button>

          <div class="profile-banner-content">
            <div class="profile-avatar-wrapper">
              <img
                :src="getAvatarSrc(detailData)"
                :alt="detailData.tenTaiKhoan"
                class="profile-avatar-img"
                @error="onAvatarError($event, detailData)"
              />
            </div>

            <div class="profile-meta-info">
              <div class="profile-header-badges">
                <span class="badge-code">{{ detailData.maNhanVien }}</span>
                <span class="badge-role">{{ detailData.tenVaiTro || 'Nhân viên' }}</span>
                <span class="status-pill" :class="detailData.trangThai ? 'is-active' : 'is-locked'">
                  {{ detailData.trangThai ? 'Hoạt động' : 'Đã khóa' }}
                </span>
              </div>
              <h3 class="profile-name">{{ detailData.tenTaiKhoan }}</h3>
              <p class="profile-email-sub"><i class="bi bi-envelope"></i> {{ detailData.email || 'Chưa cập nhật email' }}</p>
            </div>
          </div>
        </div>

        <!-- Thân Modal: Các nhóm thông tin -->
        <div class="profile-modal-body">
          <!-- Nhóm 1: Thông tin cá nhân & liên hệ -->
          <div class="info-section-group">
            <h4 class="section-group-title">
              <i class="bi bi-person-lines-fill"></i> Thông tin liên hệ & Cá nhân
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
                <span class="info-cell-label"><i class="bi bi-calendar3"></i> Ngày sinh</span>
                <span class="info-cell-value">{{ formatDate(detailData.ngaySinh) || '—' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label"><i class="bi bi-calendar-check"></i> Ngày vào làm</span>
                <span class="info-cell-value">{{ formatDate(detailData.ngayTao) || '—' }}</span>
              </div>
            </div>
          </div>

          <!-- Nhóm 2: Địa chỉ nơi ở -->
          <div class="info-section-group">
            <h4 class="section-group-title">
              <i class="bi bi-geo-alt-fill text-danger"></i> Địa chỉ nơi ở
            </h4>
            <div class="address-box-card">
              <p class="address-text">{{ formatEmployeeAddress(detailData) || 'Chưa cập nhật địa chỉ' }}</p>
            </div>
          </div>

          <!-- Metadata thời gian -->
          <div class="modal-time-footer">
            <span><i class="bi bi-clock-history"></i> Cập nhật gần nhất: {{ formatDateTime(detailData.ngayCapNhat) || '—' }}</span>
            <span v-if="detailData.nguoiCapNhat">Bởi: <b>{{ detailData.nguoiCapNhat }}</b></span>
          </div>
        </div>

        <!-- Cụm nút hành động -->
        <div class="profile-modal-actions">
          <button class="btn-profile-edit" type="button" @click="router.push(`/nhan-vien/${detailData.id}/sua`)">
            <i class="bi bi-pencil-square"></i>
            <span>Chỉnh sửa thông tin</span>
          </button>
          <button
            v-if="!isAccountProtected(detailData)"
            class="btn-profile-delete"
            type="button"
            @click="removeEmployee(detailData); detailData = null"
          >
            <i class="bi bi-trash3"></i>
            <span>Xóa tài khoản</span>
          </button>
          <button class="btn-profile-close" type="button" @click="detailData = null">
            Đóng
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.employee-page-container {
  padding: 16px 20px 40px;
  background-color: #f8fafc;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: inherit;
}

/* Card chung */
.filter-card,
.table-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
}

/* Tiêu đề Card */
.card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.icon-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-circle svg {
  width: 16px;
  height: 16px;
}

.icon-filter {
  background: #f1f5f9;
  color: #475569;
}

.icon-users {
  background: #ffe4e6;
  color: #f43f5e;
}

.card-title-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

/* Hàng Điều Khiển Bộ Lọc */
.filter-controls-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

/* Search input */
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
  border-color: #f43f5e;
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.input-search::placeholder {
  color: #94a3b8;
}

/* Dropdown bộ lọc */
.select-wrap {
  flex-shrink: 0;
}

.filter-select {
  height: 42px;
  min-width: 140px;
  padding: 0 32px 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  font-size: 13.5px;
  color: #334155;
  background: #ffffff url("data:image/svg2+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E") no-repeat right 10px center;
  background-size: 14px;
  appearance: none;
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-select:focus {
  border-color: #f43f5e;
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

/* Hàng nút hành động bộ lọc bên phải */
.filter-actions-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

/* Nút outline (Đặt lại, Xuất Excel) */
.btn-action-outline {
  height: 38px;
  padding: 0 16px;
  border: 1px solid #ffe4e6;
  border-radius: 8px;
  background: #ffffff;
  color: #f43f5e;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-outline svg {
  width: 14px;
  height: 14px;
}

.btn-action-outline:hover {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #e11d48;
}

/* Nút chính (Thêm nhân viên) */
.btn-action-primary {
  height: 38px;
  padding: 0 18px;
  background: linear-gradient(135deg, #ff3366, #e11d48);
  border: none;
  border-radius: 8px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.25);
  transition: all 0.2s ease;
}

.btn-action-primary svg {
  width: 15px;
  height: 15px;
}

.btn-action-primary:hover {
  background: linear-gradient(135deg, #f43f5e, #be123c);
  box-shadow: 0 6px 16px rgba(225, 29, 72, 0.35);
}

/* Bảng dữ liệu */
.table-container {
  overflow-x: auto;
  width: 100%;
}

.employee-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.employee-table th {
  padding: 13px 12px;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  white-space: nowrap;
}

.employee-table td {
  padding: 13px 12px;
  font-size: 13.5px;
  color: #334155;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.employee-table tbody tr:hover {
  background-color: #fafbfc;
}

.cell-stt {
  color: #475569;
  font-weight: 500;
}

.cell-code {
  font-weight: 600;
  color: #0f172a;
}

.cell-name {
  font-weight: 600;
  color: #0f172a;
}

.cell-email {
  color: #475569;
  max-width: 175px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-gender {
  color: #334155;
}

.cell-phone {
  color: #334155;
  font-weight: 500;
}

.cell-role {
  color: #334155;
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
  max-width: 280px;
}

/* Avatar tròn */
.employee-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  text-transform: uppercase;
  margin: 0 auto;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  background: #f1f5f9;
}

.employee-avatar.has-img img {
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
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.status-pill.is-active {
  background-color: #ecfdf5;
  color: #10b981;
  border: 1px solid #d1fae5;
}

.status-pill.is-locked {
  background-color: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

/* Nhóm nút hành động */
.action-buttons-group {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

/* Nút hành động tròn */
.action-buttons-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-action-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #f1f5f9;
  background: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  font-size: 15px;
  line-height: 1;
}

.btn-action-circle i {
  font-size: 15px;
  line-height: 1;
  display: inline-block;
}

/* Nút On/Off khi là Quản trị viên: DISABLE mờ xám */
.btn-action-circle.is-disabled {
  background: #f8fafc !important;
  color: #cbd5e1 !important;
  border-color: #f1f5f9 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
  opacity: 0.45 !important;
  box-shadow: none !important;
}

/* Nút On/Off khi là Nhân viên: MÀU ĐỎ */
.btn-action-circle.is-power-active {
  background: #f8fafc;
  color: #dc2626;
  border-color: #fee2e2;
}

.btn-action-circle.is-power-active:hover {
  background: #fee2e2;
  color: #b91c1c;
  transform: scale(1.08);
}

/* Nút Mắt (Xem chi tiết): Xám đậm */
.btn-action-circle.is-view {
  background: #f8fafc;
  color: #475569;
  border-color: #e2e8f0;
}

.btn-action-circle.is-view:hover {
  background: #e2e8f0;
  color: #0f172a;
  transform: scale(1.08);
}

/* Phân trang Footer */
.table-pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f8fafc;
}

.pagination-summary {
  flex: 1;
  text-align: center;
  font-size: 13px;
  color: #64748b;
}

.pagination-summary strong {
  color: #1f2937;
  font-weight: 700;
}

.pagination-size-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-select {
  height: 32px;
  padding: 0 24px 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff url("data:image/svg2+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E") no-repeat right 6px center;
  background-size: 12px;
  appearance: none;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  outline: none;
}

.pagination-nav-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-nav-btn,
.pagination-num-btn {
  height: 30px;
  min-width: 30px;
  padding: 0 6px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pagination-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-num-btn.is-active {
  background: #e11d48;
  color: #ffffff;
  border-color: #e11d48;
  font-weight: 600;
}

.pagination-num-btn:hover:not(.is-active),
.pagination-nav-btn:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

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
  max-width: 560px;
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

.profile-modal-banner.employee-theme {
  background: linear-gradient(135deg, #fff1f2 0%, #ffe4e6 50%, #fecdd3 100%);
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

.profile-avatar-initials {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f43f5e, #e11d48);
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

.badge-code {
  font-size: 11.5px;
  font-weight: 700;
  color: #e11d48;
  background: #ffffff;
  padding: 2px 8px;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.badge-role {
  font-size: 11.5px;
  font-weight: 600;
  color: #475569;
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
  color: #e11d48;
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

.btn-profile-edit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 9999px;
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  color: #ffffff;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.2);
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
