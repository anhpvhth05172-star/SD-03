<script setup>
import { ref } from 'vue'
import StatCard from '../components/StatCard.vue'

const showFilter = ref(true)

const pages = [1, 2, 3, 4, 5]

const iconTag = [
  'M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z',
]
const iconCheck = [
  'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z',
]
const iconCard = [
  'M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z',
]
const iconClock = [
  'M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z',
]

const vouchers = [
  {
    name: 'GIAM20',
    code: 'PG001',
    sub: 'Giảm 20% cho đơn từ 500.000₫',
    initials: 'G2',
    avatar: '#e0202a',
    event: 'Summer Sale 2026',
    discount: '20%',
    used: '45/200',
    expiry: '01/06/2026',
    status: 'Đang hoạt động',
  },
  {
    name: 'FREESHIP',
    code: 'PG002',
    sub: 'Miễn phí vận chuyển đơn từ 300.000₫',
    initials: 'FS',
    avatar: '#3b82f6',
    event: 'Summer Sale 2026',
    discount: '100.000₫',
    used: '120/500',
    expiry: '30/06/2026',
    status: 'Đang hoạt động',
  },
  {
    name: 'GIAM50K',
    code: 'PG003',
    sub: 'Giảm 50.000₫ cho đơn từ 1.000.000₫',
    initials: 'G5',
    avatar: '#f59e0b',
    event: 'Flash Sale 05/2026',
    discount: '50.000₫',
    used: '300/300',
    expiry: '20/05/2026',
    status: 'Hết hạn',
  },
  {
    name: 'NEWUSER',
    code: 'PG004',
    sub: 'Giảm 15% cho khách hàng mới',
    initials: 'NU',
    avatar: '#8b5cf6',
    event: 'Ưu đãi thành viên mới',
    discount: '15%',
    used: '88/1000',
    expiry: '31/12/2026',
    status: 'Đang hoạt động',
  },
  {
    name: 'TANGRAM',
    code: 'PG005',
    sub: 'Tặng thêm 10% khi mua từ 2 sản phẩm',
    initials: 'TR',
    avatar: '#14b8a6',
    event: 'Summer Sale 2026',
    discount: '10%',
    used: '0/150',
    expiry: '15/07/2026',
    status: 'Sắp diễn ra',
  },
  {
    name: 'GIAM100',
    code: 'PG006',
    sub: 'Giảm 100.000₫ cho đơn từ 2.000.000₫',
    initials: 'G1',
    avatar: '#22c55e',
    event: 'Flash Sale 05/2026',
    discount: '100.000₫',
    used: '64/80',
    expiry: '10/05/2026',
    status: 'Hết hạn',
  },
]

const statusPill = (value) => {
  if (value === 'Đang hoạt động') return 'pill-green'
  if (value === 'Sắp diễn ra') return 'pill-amber'
  return 'pill-gray'
}
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý phiếu giảm giá</h1>
        <p class="screen-sub">Tạo, theo dõi và phân phối các mã giảm giá đến khách hàng</p>
      </div>
      <button class="btn-add" type="button">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M12 6v12M6 12h12" />
        </svg>
        Thêm phiếu giảm giá
      </button>
    </div>

    <div class="stat-row">
      <StatCard
        tone="red"
        :icon="iconTag"
        label="Tổng phiếu"
        value="128"
        note="+12 phiếu trong tháng"
        note-tone="red"
      />
      <StatCard
        tone="green"
        :icon="iconCheck"
        label="Đang hoạt động"
        value="45"
        note="35% tổng số phiếu"
        note-tone="green"
      />
      <StatCard
        tone="purple"
        :icon="iconCard"
        label="Đã sử dụng"
        value="60"
        note="47% số phiếu đã phát"
        note-tone="purple"
      />
      <StatCard
        tone="amber"
        :icon="iconClock"
        label="Hết hạn"
        value="23"
        note="Cần gia hạn hoặc thu hồi"
        note-tone="amber"
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
          <span class="result-count">45 kết quả</span>
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
          <label>Mã phiếu</label>
          <input type="text" placeholder="Nhập mã phiếu giảm giá" />
        </div>

        <div class="f-item">
          <label>Đợt giảm giá</label>
          <select>
            <option value="">Tất cả</option>
            <option value="summer">Summer Sale 2026</option>
            <option value="flash">Flash Sale 05/2026</option>
            <option value="member">Ưu đãi thành viên mới</option>
          </select>
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select>
            <option value="">Tất cả</option>
            <option value="active">Đang hoạt động</option>
            <option value="upcoming">Sắp diễn ra</option>
            <option value="expired">Hết hạn</option>
          </select>
        </div>

        <div class="f-item">
          <label>Khách hàng</label>
          <input type="text" placeholder="Nhập tên hoặc số điện thoại" />
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
        <h3 class="panel-title">Danh sách phiếu giảm giá</h3>
        <span class="count-pill">128 phiếu</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Phiếu giảm giá</th>
              <th>Đợt giảm giá</th>
              <th>Mức giảm</th>
              <th>Đã dùng</th>
              <th>Hạn dùng</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in vouchers" :key="row.code">
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
              <td>{{ row.event }}</td>
              <td class="cell-strong">{{ row.discount }}</td>
              <td>{{ row.used }}</td>
              <td>{{ row.expiry }}</td>
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
        <span class="foot-text">Hiển thị 1-6 trong 128 phiếu giảm giá</span>
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
