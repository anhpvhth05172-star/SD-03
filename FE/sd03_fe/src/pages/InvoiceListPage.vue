<script setup>
import { ref } from 'vue'

const tabs = [
  'Tất Cả',
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

const invoices = [
  {
    stt: 1,
    code: 'HD0123457',
    staff: 'Phạm Văn A',
    customer: 'Phạm Văn B',
    phone: '012345689',
    total: '1.500.000₫',
    type: 'Tại quầy',
    created: '12:23:42 - 10/03/2026',
    status: 'Đã hoàn thành',
    payStatus: 'Đã thanh toán',
  },
  {
    stt: 2,
    code: 'HD0123458',
    staff: 'Admin 2',
    customer: 'Nguyễn Thị C',
    phone: '0987654321',
    total: '2.350.000₫',
    type: 'Online',
    created: '09:15:02 - 11/03/2026',
    status: 'Chờ xác nhận',
    payStatus: 'Đã thanh toán',
  },
  {
    stt: 3,
    code: 'HD0123459',
    staff: 'Admin 1',
    customer: 'Trần Văn D',
    phone: '0912345678',
    total: '890.000₫',
    type: 'Online',
    created: '15:44:10 - 11/03/2026',
    status: 'Đang giao hàng',
    payStatus: 'Chưa thanh toán',
  },
  {
    stt: 4,
    code: 'HD0123460',
    staff: 'Admin 3',
    customer: 'Lê Văn E',
    phone: '0908765432',
    total: '3.720.000₫',
    type: 'Tại quầy',
    created: '08:02:55 - 12/03/2026',
    status: 'Đã hủy',
    payStatus: 'Đã hoàn tiền',
  },
  {
    stt: 5,
    code: 'HD0123461',
    staff: 'Phạm Văn A',
    customer: 'Đỗ Thị F',
    phone: '0977112233',
    total: '1.190.000₫',
    type: 'Online',
    created: '19:30:18 - 12/03/2026',
    status: 'Chờ giao hàng',
    payStatus: 'Đã thanh toán',
  },
  {
    stt: 6,
    code: 'HD0123462',
    staff: 'Admin 2',
    customer: 'Vũ Văn G',
    phone: '0866009911',
    total: '5.400.000₫',
    type: 'Tại quầy',
    created: '21:11:07 - 13/03/2026',
    status: 'Đã hoàn thành',
    payStatus: 'Đã thanh toán',
  },
]

const statusClass = (value) => {
  if (['Đã hoàn thành', 'Đã thanh toán', 'Đã xác nhận', 'Đã giao hàng'].includes(value)) return 'badge-green'
  if (['Đã hủy', 'Đã hoàn tiền'].includes(value)) return 'badge-red'
  if (['Chờ xác nhận', 'Chờ giao hàng'].includes(value)) return 'badge-amber'
  return 'badge-gray'
}
</script>

<template>
  <div class="page">
    <section class="card page-head">
      <h2 class="page-title">Quản Lý Hóa Đơn</h2>
    </section>

    <section class="card">
      <div class="card-head">
        <svg class="head-icon-gray" viewBox="0 0 24 24" fill="currentColor">
          <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
        </svg>
        <h3 class="card-head-title">Bộ Lọc</h3>
      </div>

      <div class="filter-grid">
        <div class="field">
          <label>Mã hóa đơn</label>
          <input type="text" placeholder="Nhập mã hóa đơn" />
        </div>

        <div class="field">
          <label>Ngày bắt đầu</label>
          <div class="date-box">
            <input type="text" value="28/05/2025" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="field">
          <label>Ngày kết thúc</label>
          <div class="date-box">
            <input type="text" placeholder="dd/mm/yyyy" />
            <svg class="date-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="field">
          <label>Loại đơn</label>
          <select>
            <option value="">Tất cả</option>
            <option value="counter">Tại quầy</option>
            <option value="online">Online</option>
          </select>
        </div>
      </div>

      <div class="filter-actions">
        <button class="btn btn-blue" type="button">Tìm Kiếm</button>
        <button class="btn btn-red" type="button">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 11a8 8 0 1 0-.9 4.5" />
            <path d="M20 4.5V11h-6.5" />
          </svg>
          Làm Mới
        </button>
      </div>
    </section>

    <section class="card">
      <div class="card-head between">
        <div class="head-left">
          <span class="head-icon-red">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 2.5h12v19l-3-1.8-3 1.8-3-1.8-3 1.8v-19zM9.5 7.5h5M9.5 11.5h5M9.5 15.5h3.5" />
            </svg>
          </span>
          <h3 class="card-head-title">Danh Sách Hóa Đơn</h3>
        </div>

        <button class="btn btn-green" type="button">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3.5v11M7.5 10.5l4.5 4.5 4.5-4.5M4.5 20.5h15" />
          </svg>
          Xuất File
        </button>
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
        <table>
          <thead>
            <tr>
              <th class="col-stt">STT</th>
              <th>Mã Hóa Đơn</th>
              <th>Tên Nhân Viên</th>
              <th>Tên Khách Hàng</th>
              <th>SĐT Khách Hàng</th>
              <th>Tổng Tiền</th>
              <th>Loại Đơn</th>
              <th>Ngày Tạo</th>
              <th>Trạng Thái Hóa Đơn</th>
              <th>Trạng Thái Thanh Toán</th>
              <th class="col-action">Hành Động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in invoices" :key="row.stt">
              <td class="col-stt">{{ row.stt }}</td>
              <td class="cell-code">{{ row.code }}</td>
              <td>{{ row.staff }}</td>
              <td>{{ row.customer }}</td>
              <td>{{ row.phone }}</td>
              <td class="cell-total">{{ row.total }}</td>
              <td>{{ row.type }}</td>
              <td class="cell-date">{{ row.created }}</td>
              <td>
                <span class="badge" :class="statusClass(row.status)">{{ row.status }}</span>
              </td>
              <td>
                <span class="badge" :class="statusClass(row.payStatus)">{{ row.payStatus }}</span>
              </td>
              <td class="col-action">
                <button class="btn-eye" type="button" aria-label="Xem chi tiết">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="page-btn" type="button" aria-label="Trang trước">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14.5 6l-6 6 6 6" />
          </svg>
        </button>
        <button class="page-btn is-active" type="button">1</button>
        <button class="page-btn" type="button" aria-label="Trang sau">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9.5 6l6 6-6 6" />
          </svg>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card {
  background: var(--white);
  border: 1px solid var(--card-line);
  border-radius: 9px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  padding: 16px 18px 18px;
}

.page-head {
  padding: 14px 18px;
}

.page-title {
  color: var(--red);
  font-size: 17px;
  font-weight: 700;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 16px;
}

.card-head.between {
  justify-content: space-between;
  margin-bottom: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 11px;
}

.head-icon-gray {
  width: 18px;
  height: 18px;
  color: #67676f;
}

.head-icon-red {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--red-soft);
  color: var(--red);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.head-icon-red svg {
  width: 19px;
  height: 19px;
}

.card-head-title {
  font-size: 15.5px;
  font-weight: 700;
  color: #23232a;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field label {
  font-size: 13px;
  font-weight: 700;
  color: #2b2b32;
}

.field input,
.field select {
  width: 100%;
  height: 36px;
  border: 1px solid #e2e2e8;
  border-radius: 6px;
  padding: 0 11px;
  font-size: 13px;
  color: var(--text);
  background: #fafafb;
  outline: none;
}

.field input::placeholder {
  color: #a2a2ab;
}

.field input:focus,
.field select:focus {
  border-color: var(--red);
  background: var(--white);
}

.date-box {
  position: relative;
}

.date-box input {
  padding-right: 34px;
}

.date-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 15px;
  height: 15px;
  color: #4a4a52;
  pointer-events: none;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.btn {
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: 6px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: filter 0.15s ease;
}

.btn:hover {
  filter: brightness(1.06);
}

.btn svg {
  width: 15px;
  height: 15px;
}

.btn-blue {
  background: var(--blue);
  color: var(--white);
}

.btn-red {
  background: var(--red-bright);
  color: var(--white);
}

.btn-green {
  background: var(--green);
  color: var(--white);
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.tab {
  background: none;
  border: 1px solid transparent;
  padding: 6px 13px;
  border-radius: 6px;
  font-size: 12.5px;
  color: #cf8f93;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.tab:hover {
  color: var(--red);
}

.tab.is-active {
  background: var(--red-soft);
  border-color: #f6ccd0;
  color: var(--red);
  font-weight: 600;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11.5px;
}

thead th {
  background: #f4f5f7;
  color: #2b2b32;
  font-weight: 700;
  text-align: left;
  padding: 9px 7px;
  line-height: 1.3;
  white-space: normal;
  border: 1px solid #ececef;
}

thead th:first-child {
  border-radius: 6px 0 0 6px;
}

thead th:last-child {
  border-radius: 0 6px 6px 0;
}

tbody td {
  padding: 11px 7px;
  color: #3a3a42;
  border: 1px solid #f0f0f3;
  white-space: nowrap;
}

tbody tr:hover td {
  background: #fdf4f5;
}

.col-stt {
  text-align: center;
  width: 40px;
}

.col-action {
  text-align: center;
  width: 58px;
}

.cell-code {
  font-weight: 600;
  color: #23232a;
}

.cell-total {
  font-weight: 600;
}

.cell-date {
  font-size: 12px;
  color: #5f5f68;
}

.badge {
  display: inline-block;
  padding: 3px 7px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.badge-green {
  background: #e7f8ee;
  color: #15803d;
}

.badge-red {
  background: #fdecec;
  color: #dc2626;
}

.badge-amber {
  background: #fdf3e3;
  color: #d97706;
}

.badge-gray {
  background: #f1f2f4;
  color: #6b7280;
}

.btn-eye {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #2b2b30;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.btn-eye:hover {
  background: var(--red-soft);
  color: var(--red);
}

.btn-eye svg {
  width: 17px;
  height: 17px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

.page-btn {
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid #e4e4e9;
  border-radius: 7px;
  background: var(--white);
  color: #55555e;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.page-btn:hover {
  border-color: var(--red);
  color: var(--red);
}

.page-btn.is-active {
  background: #f1f2f5;
  border-color: #dcdce2;
  color: #1f1f24;
  font-weight: 600;
}

.page-btn svg {
  width: 15px;
  height: 15px;
}
</style>
