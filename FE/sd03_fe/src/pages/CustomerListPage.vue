<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showFilter = ref(true)

const pages = [1, 2, 3, 4, 5]

const customers = [
  {
    name: 'Nguyễn Văn An',
    initials: 'NA',
    color: '#cc0000',
    code: 'KH001',
    contact: '0987 654 321 · an.nv@gmail.com',
    rank: 'Kim cương',
    orders: '24 đơn',
    spent: '18.500.000₫',
    joined: '12/01/2024',
    status: 'Đang hoạt động',
  },
  {
    name: 'Trần Thị Bích',
    initials: 'TB',
    color: '#f59e0b',
    code: 'KH002',
    contact: '0912 345 678 · thb@gmail.com',
    rank: 'Bạc',
    orders: '12 đơn',
    spent: '6.200.000₫',
    joined: '03/05/2024',
    status: 'Đang hoạt động',
  },
  {
    name: 'Lê Minh Cường',
    initials: 'LC',
    color: '#8b5cf6',
    code: 'KH003',
    contact: '0983 112 233 · lmc@gmail.com',
    rank: 'Vàng',
    orders: '8 đơn',
    spent: '4.750.000₫',
    joined: '22/11/2024',
    status: 'Đang hoạt động',
  },
  {
    name: 'Phạm Thu Hà',
    initials: 'PH',
    color: '#14b8a6',
    code: 'KH004',
    contact: '0977 445 566 · pth@gmail.com',
    rank: 'Thường',
    orders: '3 đơn',
    spent: '1.200.000₫',
    joined: '15/03/2025',
    status: 'Chưa xác thực',
  },
  {
    name: 'Đặng Quốc Huy',
    initials: 'ĐH',
    color: '#3b82f6',
    code: 'KH005',
    contact: '0909 887 766 · dqh@gmail.com',
    rank: 'Vàng',
    orders: '15 đơn',
    spent: '7.900.000₫',
    joined: '08/07/2024',
    status: 'Đang hoạt động',
  },
  {
    name: 'Vũ Thị Kim Ngân',
    initials: 'KN',
    color: '#22c55e',
    code: 'KH006',
    contact: '0968 223 344 · vtkn@gmail.com',
    rank: 'Bạc',
    orders: '6 đơn',
    spent: '3.100.000₫',
    joined: '19/09/2025',
    status: 'Đang hoạt động',
  },
  {
    name: 'Bùi Anh Tú',
    initials: 'BT',
    color: '#f97316',
    code: 'KH007',
    contact: '0955 667 788 · bat@gmail.com',
    rank: 'Thường',
    orders: '2 đơn',
    spent: '690.000₫',
    joined: '27/12/2025',
    status: 'Đã khóa',
  },
  {
    name: 'Hồ Ngọc Hà',
    initials: 'HH',
    color: '#ec4899',
    code: 'KH008',
    contact: '0944 556 677 · hnh@gmail.com',
    rank: 'Kim cương',
    orders: '31 đơn',
    spent: '24.300.000₫',
    joined: '05/02/2023',
    status: 'Đang hoạt động',
  },
]

const statusPill = (value) => {
  if (value === 'Đang hoạt động') return 'pill-green'
  if (value === 'Chưa xác thực') return 'pill-amber'
  return 'pill-gray'
}

const rankPill = (value) => {
  if (value === 'Kim cương') return 'pill-blue'
  if (value === 'Vàng') return 'pill-amber'
  if (value === 'Bạc') return 'pill-gray'
  return 'pill-green'
}
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
          <label>Tìm khách hàng</label>
          <input type="text" placeholder="Nhập tên, SĐT hoặc email" />
        </div>

        <div class="f-item">
          <label>Hạng thành viên</label>
          <select>
            <option value="">Tất cả</option>
            <option value="diamond">Kim cương</option>
            <option value="gold">Vàng</option>
            <option value="silver">Bạc</option>
            <option value="normal">Thường</option>
          </select>
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select>
            <option value="">Tất cả</option>
            <option value="active">Đang hoạt động</option>
            <option value="unverified">Chưa xác thực</option>
            <option value="locked">Đã khóa</option>
          </select>
        </div>

        <div class="f-item">
          <label>Ngày đăng ký</label>
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
        <h3 class="panel-title">Danh sách khách hàng</h3>
        <span class="count-pill">48 khách hàng</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
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
            <tr v-for="row in customers" :key="row.code">
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
              <td><span class="pill" :class="rankPill(row.rank)">{{ row.rank }}</span></td>
              <td>{{ row.orders }}</td>
              <td>{{ row.spent }}</td>
              <td>{{ row.joined }}</td>
              <td><span class="pill" :class="statusPill(row.status)">{{ row.status }}</span></td>
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
        <span class="foot-text">Hiển thị 1-8 trong 48 khách hàng</span>
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
