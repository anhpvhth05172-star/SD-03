<script setup>
import { ref } from 'vue'
import StatCard from '../components/StatCard.vue'

const showFilter = ref(true)

const pages = [1, 2]

const iconEvent = [
  'M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z',
]
const iconCheck = [
  'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z',
]
const iconClock = [
  'M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z',
]
const iconArchive = [
  'M20.54 5.23l-1.39-1.68C18.88 3.21 18.47 3 18 3H6c-.47 0-.88.21-1.16.55L3.46 5.23C3.17 5.57 3 6.02 3 6.5V19c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6.5c0-.48-.17-.93-.46-1.27zM12 17.5L6.5 12H10v-2h4v2h3.5L12 17.5zM5.12 5l.81-1h12l.94 1H5.12z',
]

const events = [
  {
    name: 'Summer Sale 2026',
    code: 'DG001',
    sub: 'Giảm giá toàn hệ thống - tất cả khách hàng',
    initials: 'SS',
    avatar: '#e0202a',
    time: '01/05/2026 - 30/06/2026',
    discount: '10% - 30%',
    scope: 'Tất cả khách hàng',
    vouchers: '120 phiếu',
    status: 'Đang diễn ra',
  },
  {
    name: 'Flash Sale 05/2026',
    code: 'DG002',
    sub: 'Sự kiện giảm giá trong 3 ngày',
    initials: 'FS',
    avatar: '#f59e0b',
    time: '20/05/2026 - 22/05/2026',
    discount: 'Tối đa 50%',
    scope: 'Tất cả khách hàng',
    vouchers: '80 phiếu',
    status: 'Sắp diễn ra',
  },
  {
    name: 'Ưu đãi thành viên mới',
    code: 'DG003',
    sub: 'Dành cho khách hàng đăng ký mới',
    initials: 'UM',
    avatar: '#8b5cf6',
    time: '01/01/2026 - 31/12/2026',
    discount: '15%',
    scope: 'Khách hàng mới',
    vouchers: '1000 phiếu',
    status: 'Đang diễn ra',
  },
  {
    name: 'Black Friday 2026',
    code: 'DG004',
    sub: 'Sự kiện giảm giá lớn nhất năm',
    initials: 'BF',
    avatar: '#14b8a6',
    time: '27/11/2026 - 30/11/2026',
    discount: 'Tối đa 60%',
    scope: 'Tất cả khách hàng',
    vouchers: '500 phiếu',
    status: 'Sắp diễn ra',
  },
  {
    name: 'Tết Nguyên Đán 2026',
    code: 'DG005',
    sub: 'Ưu đãi dịp Tết nguyên đán',
    initials: 'TND',
    avatar: '#3b82f6',
    time: '15/02/2026 - 28/02/2026',
    discount: '5% - 25%',
    scope: 'Tất cả khách hàng',
    vouchers: '300 phiếu',
    status: 'Đã kết thúc',
  },
  {
    name: 'Back To School',
    code: 'DG006',
    sub: 'Giảm giá mùa tựu trường',
    initials: 'BS',
    avatar: '#22c55e',
    time: '10/08/2026 - 30/09/2026',
    discount: '10% - 20%',
    scope: 'Tất cả khách hàng',
    vouchers: '150 phiếu',
    status: 'Đã kết thúc',
  },
]

const statusPill = (value) => {
  if (value === 'Đang diễn ra') return 'pill-green'
  if (value === 'Sắp diễn ra') return 'pill-amber'
  return 'pill-gray'
}
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý đợt giảm giá</h1>
        <p class="screen-sub">
          Quản lý các sự kiện giảm giá áp dụng cho tất cả khách hàng trên toàn hệ thống
        </p>
      </div>
      <button class="btn-add" type="button">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M12 6v12M6 12h12" />
        </svg>
        Thêm đợt giảm giá
      </button>
    </div>

    <div class="stat-row">
      <StatCard
        tone="red"
        :icon="iconEvent"
        label="Tổng đợt"
        value="12"
        note="+2 đợt trong tháng"
        note-tone="red"
      />
      <StatCard
        tone="green"
        :icon="iconCheck"
        label="Đang diễn ra"
        value="4"
        note="Áp dụng toàn hệ thống"
        note-tone="green"
      />
      <StatCard
        tone="amber"
        :icon="iconClock"
        label="Sắp diễn ra"
        value="3"
        note="Chuẩn bị khởi chạy"
        note-tone="amber"
      />
      <StatCard
        tone="blue"
        :icon="iconArchive"
        label="Đã kết thúc"
        value="5"
        note="Đã lưu trữ"
        note-tone="blue"
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
          <span class="result-count">12 kết quả</span>
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
          <label>Tên đợt giảm giá</label>
          <input type="text" placeholder="Nhập tên đợt giảm giá" />
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select>
            <option value="">Tất cả</option>
            <option value="running">Đang diễn ra</option>
            <option value="upcoming">Sắp diễn ra</option>
            <option value="finished">Đã kết thúc</option>
          </select>
        </div>

        <div class="f-item">
          <label>Từ ngày</label>
          <div class="date-box">
            <input type="text" value="01/05/2026" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="f-item">
          <label>Đến ngày</label>
          <div class="date-box">
            <input type="text" placeholder="dd/mm/yyyy" />
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
              d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"
            />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách đợt giảm giá</h3>
        <span class="count-pill">12 đợt</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Đợt giảm giá</th>
              <th>Thời gian</th>
              <th>Mức giảm</th>
              <th>Áp dụng</th>
              <th>Số phiếu</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in events" :key="row.code">
              <td>
                <div class="entity">
                  <span class="entity-avatar" :style="{ background: row.avatar }">{{ row.initials }}</span>
                  <div class="entity-info">
                    <div class="entity-line">
                      <span class="entity-name">{{ row.name }}</span>
                      <span class="entity-code">{{ row.code }}</span>
                    </div>
                    <div class="entity-sub">{{ row.sub }}</div>
                  </div>
                </div>
              </td>
              <td>{{ row.time }}</td>
              <td class="cell-strong">{{ row.discount }}</td>
              <td>{{ row.scope }}</td>
              <td>{{ row.vouchers }}</td>
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
                  <button class="act-btn" type="button" aria-label="Thêm">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                      <path d="M12 6v12M6 12h12" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">Hiển thị 1-6 trong 12 đợt giảm giá</span>
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
