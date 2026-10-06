<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listCustomers } from '../api/customer'
import { avatarColor, formatDate, formatVnd, initialsOf } from '../utils/format'

const router = useRouter()
const showFilter = ref(true)
const loading = ref(true)
const error = ref('')
const customers = ref([])
const capNhatLuc = ref('')

const loc = ref({ tim: '', trangThai: '' })
const locDaApDung = ref({ tim: '', trangThai: '' })

const TRANG_THAI = ['Đang hoạt động', 'Đã khóa']

const statusPill = (value) => {
  if (value === 'Đang hoạt động') return 'pill-green'
  return 'pill-gray'
}

const rows = computed(() => {
  const ap = locDaApDung.value
  return customers.value
    .map((c) => ({
      key: c.id,
      name: c.tenKhachHang,
      code: c.ma,
      initials: initialsOf(c.tenKhachHang),
      color: avatarColor(c.id),
      contact: [c.soDienThoai || '—', c.email || '—'].join(' · '),
      orders: `${c.soDon ?? 0} đơn`,
      spent: formatVnd(c.tongChiTieu || 0),
      joined: c.ngayTao ? formatDate(c.ngayTao) : '—',
      status: c.trangThai ? 'Đang hoạt động' : 'Đã khóa',
      kho: c.trangThai,
    }))
    .filter((r) => {
      if (!ap.tim) return true
      const tu = ap.tim.toLowerCase()
      return (
        r.name.toLowerCase().includes(tu) ||
        r.contact.toLowerCase().includes(tu) ||
        r.code.toLowerCase().includes(tu)
      )
    })
    .filter((r) => !ap.trangThai || r.status === ap.trangThai)
})

const apDungBoLoc = () => {
  locDaApDung.value = { ...loc.value }
}

const datLai = () => {
  loc.value = { tim: '', trangThai: '' }
  locDaApDung.value = { ...loc.value }
}

const tai = async () => {
  loading.value = true
  error.value = ''
  try {
    customers.value = await listCustomers()
    capNhatLuc.value = new Date().toLocaleString('vi-VN')
  } catch (e) {
    error.value = e.message || 'Không tải được danh sách khách hàng'
  } finally {
    loading.value = false
  }
}

onMounted(tai)
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý khách hàng</h1>
        <p class="screen-sub">Quản lý thông tin khách hàng, tích điểm thành viên và lịch sử mua hàng</p>
      </div>
      <button class="btn-add" type="button" @click="router.push('/khach-hang/them')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M12 6v12M6 12h12" />
        </svg>
        Thêm khách hàng
      </button>
    </div>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Tìm kiếm &amp; bộ lọc</h3>
        <div class="panel-right">
          <span class="result-count">{{ rows.length }} kết quả</span>
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
          <input v-model="loc.tim" type="text" placeholder="Nhập tên, SĐT hoặc email" />
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select v-model="loc.trangThai">
            <option value="">Tất cả</option>
            <option v-for="tt in TRANG_THAI" :key="tt" :value="tt">{{ tt }}</option>
          </select>
        </div>

        <div class="filter-actions">
          <button class="btn-reset" type="button" aria-label="Đặt lại" @click="datLai">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
          </button>
          <button class="btn-apply" type="button" @click="apDungBoLoc">
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
        <span class="count-pill">{{ customers.length }} khách hàng</span>
        <div class="panel-right">
          <span v-if="capNhatLuc" class="panel-meta">Cập nhật lúc {{ capNhatLuc }}</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Khách hàng</th>
              <th>Tổng đơn</th>
              <th>Tổng chi tiêu</th>
              <th>Ngày đăng ký</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="5" class="state-cell">Đang tải dữ liệu...</td>
            </tr>
            <tr v-else-if="error">
              <td colspan="5" class="state-cell is-error">
                {{ error }}
                <button class="link-red" type="button" @click="tai">Thử lại</button>
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td colspan="5" class="state-cell">Chưa có khách hàng nào</td>
            </tr>
            <tr v-for="row in rows" :key="row.key">
              <td>
                <div class="entity">
                  <span class="entity-avatar" :style="{ background: row.color }">{{ row.initials }}</span>
                  <div class="entity-info">
                    <div class="entity-line">
                      <span class="entity-name">{{ row.name }}</span>
                      <span class="entity-code">{{ row.code }}</span>
                    </div>
                    <div class="entity-sub">{{ row.contact }}</div>
                  </div>
                </div>
              </td>
              <td>{{ row.orders }}</td>
              <td class="cell-strong">{{ row.spent }}</td>
              <td>{{ row.joined }}</td>
              <td><span class="pill" :class="statusPill(row.status)">{{ row.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">Hiển thị {{ rows.length }} trong {{ customers.length }} khách hàng</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.state-cell {
  text-align: center;
  color: #9a9aa3;
  padding: 24px 0;
  font-size: 13.5px;
}

.state-cell.is-error {
  color: #dc2626;
  font-weight: 600;
}
</style>
