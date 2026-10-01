<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import StatCard from '../components/StatCard.vue'

const router = useRouter()
const showFilter = ref(true)

const pages = [1, 2, 3, 4, 5]

const iconPeople = [
  'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z',
]
const iconPerson = [
  'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
]
const iconCalendar = [
  'M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z',
]
const iconAssignment = [
  'M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2.42 8.91L10 17.34 7.58 14.92 6.16 16.34 10 20.17l6.84-6.83-1.42-1.43z',
]

const staffs = [
  {
    name: 'Nguyễn Minh Quân',
    initials: 'MQ',
    color: '#e0202a',
    code: 'NV001',
    email: 'quan.nm@polyshoe.vn',
    role: 'Giám đốc điều hành',
    status: 'Đang hoạt động',
    start: '12/03/2023',
  },
  {
    name: 'Trần Thị Hồng Nhung',
    initials: 'NH',
    color: '#f59e0b',
    code: 'NV003',
    email: 'nhung.th@polyshoe.vn',
    role: 'Nhân viên vận hành bán hàng',
    status: 'Đang hoạt động',
    start: '20/08/2024',
  },
  {
    name: 'Lê Hoàng Ngọc Phượng',
    initials: 'NP',
    color: '#8b5cf6',
    code: 'NV014',
    email: 'phuong.lhn@polyshoe.vn',
    role: 'Nhân viên bán shop',
    status: 'Nghỉ phép',
    start: '05/09/2025',
  },
  {
    name: 'Phạm Ngọc Linh',
    initials: 'NL',
    color: '#14b8a6',
    code: 'NV017',
    email: 'linh.pn@polyshoe.vn',
    role: 'Thủ kho',
    status: 'Đang hoạt động',
    start: '18/02/2025',
  },
  {
    name: 'Đỗ Quốc Việt',
    initials: 'ĐV',
    color: '#3b82f6',
    code: 'NV021',
    email: 'viet.dq@polyshoe.vn',
    role: 'Nhân viên bán hàng',
    status: 'Tạm ngưng',
    start: '07/05/2026',
  },
  {
    name: 'Vũ Khánh Vy',
    initials: 'KV',
    color: '#22c55e',
    code: 'NV026',
    email: 'vy.vk@polyshoe.vn',
    role: 'Chăm sóc khách hàng',
    status: 'Đang hoạt động',
    start: '22/07/2025',
  },
  {
    name: 'Bùi Đỗ Long',
    initials: 'DL',
    color: '#f97316',
    code: 'NV030',
    email: 'long.bd@polyshoe.vn',
    role: 'Nhân viên nhập đơn',
    status: 'Đang hoạt động',
    start: '04/11/2025',
  },
  {
    name: 'Hoàng Mai Phương',
    initials: 'MP',
    color: '#ec4899',
    code: 'NV036',
    email: 'phuong.hm@polyshoe.vn',
    role: 'Nhân viên bán hàng',
    status: 'Đang hoạt động',
    start: '16/09/2026',
  },
]

const statusPill = (value) => {
  if (value === 'Đang hoạt động') return 'pill-green'
  if (value === 'Nghỉ phép') return 'pill-amber'
  return 'pill-gray'
}
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý nhân viên</h1>
        <p class="screen-sub">Quản lý hồ sơ nhân sự, phân quyền và hợp đồng lao động tại Poly Shoe</p>
      </div>
      <button class="btn-add" type="button" @click="router.push('/nhan-vien/them')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M12 6v12M6 12h12" />
        </svg>
        Thêm nhân viên
      </button>
    </div>

    <div class="stat-row">
      <StatCard
        tone="red"
        :icon="iconPeople"
        label="Tổng số nhân viên"
        value="48"
        note="+6 nhân viên trong tháng"
        note-tone="red"
      />
      <StatCard
        tone="green"
        :icon="iconPerson"
        label="Đang làm việc"
        value="42"
        note="97% đang hoạt động"
        note-tone="green"
      />
      <StatCard
        tone="amber"
        :icon="iconCalendar"
        label="Chấm công hôm nay"
        value="06"
        note="Có mặt đúng giờ"
        note-tone="amber"
      />
      <StatCard
        tone="purple"
        :icon="iconAssignment"
        label="Cần phê duyệt đơn"
        value="05"
        note="Cần xem xét"
        note-tone="purple"
      />
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
          <span class="result-count">48 kết quả</span>
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
          <label>Mã nhân viên</label>
          <input type="text" placeholder="Nhập mã nhân viên" />
        </div>

        <div class="f-item">
          <label>Vai trò</label>
          <select>
            <option value="">Tất cả</option>
            <option value="manager">Quản lý</option>
            <option value="sale">Nhân viên bán hàng</option>
            <option value="warehouse">Thủ kho</option>
            <option value="support">Chăm sóc khách hàng</option>
          </select>
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select>
            <option value="">Tất cả</option>
            <option value="active">Đang hoạt động</option>
            <option value="leave">Nghỉ phép</option>
            <option value="suspend">Tạm ngưng</option>
          </select>
        </div>

        <div class="f-item">
          <label>Ngày bắt đầu</label>
          <div class="date-box">
            <input type="text" value="01/01/2023" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="filter-actions">
          <button class="btn-reset" type="button" aria-label="Đặt lại">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
          </button>
          <button class="btn-apply" type="button">
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
        <h3 class="panel-title">Danh sách nhân viên</h3>
        <span class="count-pill">48 nhân viên</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Nhân viên</th>
              <th>Vai trò</th>
              <th>Trạng thái</th>
              <th>Ngày bắt đầu</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in staffs" :key="row.code">
              <td>
                <div class="entity">
                  <span class="entity-avatar" :style="{ background: row.color }">{{ row.initials }}</span>
                  <div class="entity-info">
                    <div class="entity-line">
                      <span class="entity-name">{{ row.name }}</span>
                      <span class="entity-code">{{ row.code }}</span>
                    </div>
                    <div class="entity-sub">{{ row.email }}</div>
                  </div>
                </div>
              </td>
              <td>{{ row.role }}</td>
              <td><span class="pill" :class="statusPill(row.status)">{{ row.status }}</span></td>
              <td>{{ row.start }}</td>
              <td>
                <div class="act-group">
                  <button class="act-btn" type="button" aria-label="Xem">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                  <button class="act-btn is-red" type="button" aria-label="Sửa">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3zM14.5 6.5l3 3" />
                    </svg>
                  </button>
                  <button class="act-btn" type="button" aria-label="Khóa">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="5" y="11" width="14" height="9" rx="2" />
                      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">Hiển thị 1-8 trong 48 nhân viên</span>
        <div class="pager">
          <button class="page-btn" type="button" aria-label="Trang trước">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.5 6l-6 6 6 6" />
            </svg>
          </button>
          <button
            v-for="page in pages"
            :key="page"
            class="page-btn"
            :class="{ 'is-active': page === 1 }"
            type="button"
          >
            {{ page }}
          </button>
          <button class="page-btn" type="button" aria-label="Trang sau">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9.5 6l6 6-6 6" />
            </svg>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped></style>
