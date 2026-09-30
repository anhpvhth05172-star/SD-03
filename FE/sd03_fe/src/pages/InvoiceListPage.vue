<script setup>
const invoices = [
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'COD', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'VNPAY', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'Card', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'COD', total: '1.500.000 đ', status: 'Đã hủy' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'COD', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'VNPAY', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'COD', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'COD', total: '1.500.000 đ', status: 'Đã hủy' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'VNPAY', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'VNPAY', total: '1.500.000 đ', status: 'Đã hủy' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'Card', total: '1.500.000 đ', status: 'Hoàn thành' },
  { id: 'HD0123789', customer: 'Phạm Văn A', date: '16/08/2026', payment: 'Card', total: '1.500.000 đ', status: 'Hoàn thành' },
]
</script>

<template>
  <section class="card">
    <h2 class="card-title">Quản lý hóa đơn</h2>

    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="11" cy="11" r="7" />
          <path d="M20 20l-3.5-3.5" />
        </svg>
        <input type="text" placeholder="Nhập mã hóa đơn" />
      </div>
      <button class="btn-search" type="button">Tìm kiếm</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID hóa đơn</th>
            <th>Tên khách hàng</th>
            <th>Ngày</th>
            <th>PTTT</th>
            <th>Tổng tiền</th>
            <th>Trạng thái</th>
            <th>Chi tiết</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in invoices" :key="index">
            <td class="cell-id">{{ row.id }}</td>
            <td>{{ row.customer }}</td>
            <td>{{ row.date }}</td>
            <td>{{ row.payment }}</td>
            <td class="cell-total">{{ row.total }}</td>
            <td>
              <span class="badge" :class="row.status === 'Hoàn thành' ? 'badge-ok' : 'badge-cancel'">
                {{ row.status }}
              </span>
            </td>
            <td>
              <button class="btn-detail" type="button">Xem chi tiết</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.card {
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 28px 32px 34px;
}

.card-title {
  color: var(--red);
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.search-box {
  position: relative;
  width: 260px;
}

.search-box svg {
  position: absolute;
  left: 11px;
  top: 50%;
  transform: translateY(-50%);
  width: 15px;
  height: 15px;
  color: #9b9b9b;
  pointer-events: none;
}

.search-box input {
  width: 100%;
  height: 36px;
  border: 1px solid #e3e3e3;
  border-radius: 6px;
  padding: 0 12px 0 33px;
  font-size: 13px;
  color: var(--text);
  outline: none;
  background: #fff;
}

.search-box input::placeholder {
  color: #a5a5a5;
}

.search-box input:focus {
  border-color: var(--red);
}

.btn-search {
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: 6px;
  background: var(--red);
  color: #fff;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-search:hover {
  background: var(--red-dark);
}

.table-wrap {
  max-width: 800px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

thead th {
  background: #f0f1f3;
  color: #2f2f2f;
  font-weight: 700;
  text-align: left;
  padding: 12px 14px;
  white-space: nowrap;
}

thead th:first-child {
  border-top-left-radius: 8px;
}

thead th:last-child {
  border-top-right-radius: 8px;
}

tbody td {
  padding: 11px 14px;
  color: #333;
  white-space: nowrap;
  border-bottom: 1px solid #ffffff;
}

tbody tr:nth-child(odd) {
  background: #fdeeee;
}

tbody tr:nth-child(even) {
  background: #ffffff;
}

tbody tr:hover {
  background: #fbdede;
}

.cell-id {
  font-weight: 600;
}

.cell-total {
  font-weight: 600;
}

.badge {
  display: inline-block;
  min-width: 92px;
  text-align: center;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.badge-ok {
  background: #2eb82e;
}

.badge-cancel {
  background: #e01b1b;
}

.btn-detail {
  background: #ffffff;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  color: #c1272d;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s ease;
}

.btn-detail:hover {
  background: #fafafa;
}
</style>
