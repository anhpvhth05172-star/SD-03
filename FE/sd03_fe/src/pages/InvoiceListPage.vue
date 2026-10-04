<script setup>
import { ref } from 'vue'

const showFilter = ref(true)

const tabs = [
  'Tất cả',
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
  'Đã hủy',
  'Đã hoàn tiền',
]
const activeTab = ref(0)

const pages = [1, 2, 3, 4, 5]

const invoices = [
  {
    code: 'HD0123457',
    staff: 'Phạm Văn A',
    customer: 'Phạm Văn B',
    initials: 'PB',
    avatar: '#cc0000',
    phone: '012345689',
    total: '1.500.000₫',
    type: 'Tại quầy',
    created: '12:23:42 - 10/03/2026',
    status: 'Đã hoàn thành',
    payStatus: 'Đã thanh toán',
  },
  {
    code: 'HD0123458',
    staff: 'Admin 2',
    customer: 'Nguyễn Thị C',
    initials: 'NC',
    avatar: '#f59e0b',
    phone: '0987654321',
    total: '2.350.000₫',
    type: 'Online',
    created: '09:15:02 - 11/03/2026',
    status: 'Chờ xác nhận',
    payStatus: 'Đã thanh toán',
  },
  {
    code: 'HD0123459',
    staff: 'Admin 1',
    customer: 'Trần Văn D',
    initials: 'TD',
    avatar: '#8b5cf6',
    phone: '0912345678',
    total: '890.000₫',
    type: 'Online',
    created: '15:44:10 - 11/03/2026',
    status: 'Đang giao hàng',
    payStatus: 'Chưa thanh toán',
  },
  {
    code: 'HD0123460',
    staff: 'Admin 3',
    customer: 'Lê Văn E',
    initials: 'LE',
    avatar: '#14b8a6',
    phone: '0908765432',
    total: '3.720.000₫',
    type: 'Tại quầy',
    created: '08:02:55 - 12/03/2026',
    status: 'Đã hủy',
    payStatus: 'Đã hoàn tiền',
  },
  {
    code: 'HD0123461',
    staff: 'Phạm Văn A',
    customer: 'Đỗ Thị F',
    initials: 'DF',
    avatar: '#3b82f6',
    phone: '0977112233',
    total: '1.190.000₫',
    type: 'Online',
    created: '19:30:18 - 12/03/2026',
    status: 'Chờ giao hàng',
    payStatus: 'Đã thanh toán',
  },
  {
    code: 'HD0123462',
    staff: 'Admin 2',
    customer: 'Vũ Văn G',
    initials: 'GV',
    avatar: '#22c55e',
    phone: '0866009911',
    total: '5.400.000₫',
    type: 'Tại quầy',
    created: '21:11:07 - 13/03/2026',
    status: 'Đã hoàn thành',
    payStatus: 'Đã thanh toán',
  },
]

const statusPill = (value) => {
  if (['Đã hoàn thành', 'Đã thanh toán', 'Đã xác nhận', 'Đã giao hàng'].includes(value)) return 'pill-green'
  if (['Đã hủy', 'Đã hoàn tiền'].includes(value)) return 'pill-red'
  if (['Chờ xác nhận', 'Chờ giao hàng'].includes(value)) return 'pill-amber'
  return 'pill-gray'
}
</script>

<template>
  <div class="screen">
    <section class="panel">
      <h2 class="page-title">Quản lý hóa đơn</h2>
    </section>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Bộ lọc tìm kiếm</h3>
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
          <label>Mã hóa đơn</label>
          <input type="text" placeholder="Nhập mã hóa đơn" />
        </div>

        <div class="f-item">
          <label>Ngày bắt đầu</label>
          <div class="date-box">
            <input type="text" value="28/05/2025" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="f-item">
          <label>Ngày kết thúc</label>
          <div class="date-box">
            <input type="text" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="f-item">
          <label>Loại đơn</label>
          <select>
            <option value="">Tất cả</option>
            <option value="counter">Tại quầy</option>
            <option value="online">Online</option>
          </select>
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
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 2.5h12v19l-3-1.8-3 1.8-3-1.8-3 1.8v-19zM9.5 7.5h5M9.5 11.5h5M9.5 15.5h3.5" />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách hóa đơn</h3>
        <span class="count-pill">48 hóa đơn</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
          <button class="btn-export" type="button">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3.5v11M7.5 10.5l4.5 4.5 4.5-4.5M4.5 20.5h15" />
            </svg>
            Xuất File
          </button>
        </div>
      </div>

      <div class="tabs">
        <button
          v-for="(tab, index) in tabs"
          :key="tab"
          type="button"
          class="tab"
          :class="{ 'is-active': activeTab === index }"
          @click="activeTab = index"
        >
          {{ tab }}
        </button>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Hóa đơn</th>
              <th>Nhân viên</th>
              <th>Loại đơn</th>
              <th>Tổng tiền</th>
              <th>Ngày tạo</th>
              <th>Trạng thái hóa đơn</th>
              <th>Trạng thái thanh toán</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in invoices" :key="row.code">
              <td>
                <div class="entity">
                  <span class="entity-avatar" :style="{ background: row.avatar }">{{ row.initials }}</span>
                  <div class="entity-info">
                    <div class="entity-line">
                      <span class="entity-name">{{ row.code }}</span>
                    </div>
                    <div class="entity-sub">{{ row.customer }} • {{ row.phone }}</div>
                  </div>
                </div>
              </td>
              <td>{{ row.staff }}</td>
              <td>{{ row.type }}</td>
              <td class="cell-strong">{{ row.total }}</td>
              <td>{{ row.created }}</td>
              <td><span class="pill" :class="statusPill(row.status)">{{ row.status }}</span></td>
              <td><span class="pill" :class="statusPill(row.payStatus)">{{ row.payStatus }}</span></td>
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
        <span class="foot-text">Hiển thị 1-6 trong 48 hóa đơn</span>
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

<style scoped>
.page-title {
  color: var(--red);
  font-size: 17px;
  font-weight: 700;
}

.btn-export {
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 7px;
  background: var(--red);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-export:hover {
  background: var(--red-dark);
}

.btn-export svg {
  width: 15px;
  height: 15px;
}
</style>
