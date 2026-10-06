<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getInvoice } from '../api/invoice'
import { formatDateTime, formatVnd } from '../utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const data = ref(null)

const statusPill = (value) => {
  if (['Đã hoàn thành', 'Đã thanh toán', 'Đã xác nhận', 'Đã giao hàng'].includes(value)) return 'pill-green'
  if (['Đã hủy', 'Đã hoàn tiền'].includes(value)) return 'pill-red'
  if (['Chờ xác nhận', 'Chờ giao hàng'].includes(value)) return 'pill-amber'
  return 'pill-gray'
}

const giamGia = computed(() => {
  if (!data.value) return 0
  const tong = Number(data.value.tongTien) || 0
  const sau = Number(data.value.tienSauGiamGia) || 0
  return Math.max(0, tong - sau)
})

const load = async () => {
  const id = route.params.id
  if (!/^\d+$/.test(String(id))) {
    error.value = `ID hóa đơn không hợp lệ: ${id}`
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  data.value = null
  try {
    data.value = await getInvoice(id)
  } catch (e) {
    error.value = e.message || 'Không tải được chi tiết hóa đơn'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="screen">
    <div class="detail-top">
      <button class="btn-back" type="button" @click="router.push('/hoa-don')">← Quay lại danh sách</button>
    </div>

    <section v-if="loading" class="panel">
      <div class="state-box">Đang tải dữ liệu...</div>
    </section>

    <section v-else-if="error" class="panel">
      <div class="state-box is-error">
        <p class="state-msg">{{ error }}</p>
        <button class="btn-back" type="button" @click="router.push('/hoa-don')">← Quay lại danh sách</button>
      </div>
    </section>

    <template v-else-if="data">
      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon is-red">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 2.5h12v19l-3-1.8-3 1.8-3-1.8-3 1.8v-19zM9.5 7.5h5M9.5 11.5h5M9.5 15.5h3.5" />
            </svg>
          </span>
          <h3 class="panel-title">Thông tin hóa đơn</h3>
        </div>
        <div class="detail-grid">
          <div class="d-item"><label>Mã hóa đơn</label><span class="detail-code">{{ data.maHoaDon }}</span></div>
          <div class="d-item"><label>Trạng thái</label><span class="pill" :class="statusPill(data.trangThai)">{{ data.trangThai }}</span></div>
          <div class="d-item"><label>Loại đơn</label><span>{{ data.loaiDon }}</span></div>
          <div class="d-item"><label>Ngày tạo</label><span>{{ formatDateTime(data.ngayTao) }}</span></div>
          <div class="d-item"><label>Nhân viên</label><span>{{ data.tenNhanVien || '—' }}</span></div>
          <div class="d-item"><label>Phương thức thanh toán</label><span>{{ data.tenPhuongThucThanhToan || '—' }}</span></div>
          <div class="d-item d-wide"><label>Phiếu giảm giá</label><span>{{ data.tenPhieuGiamGia || 'Không dùng' }}</span></div>
          <div class="d-item"><label>Đợt giảm giá</label><span>{{ data.tenDotGiamGia || '—' }}</span></div>
          <div class="d-item"><label>Tiền giảm (lúc lập)</label><span>{{ data.tienGiam != null ? formatVnd(data.tienGiam) : '—' }}</span></div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
            </svg>
          </span>
          <h3 class="panel-title">Thông tin khách hàng</h3>
        </div>
        <div class="detail-grid">
          <div class="d-item"><label>Khách hàng</label><span>{{ data.tenKhachHang || 'Khách lẻ' }}</span></div>
          <div class="d-item"><label>Số điện thoại</label><span>{{ data.soDienThoaiKhachHang || '—' }}</span></div>
          <div class="d-item d-wide"><label>Địa chỉ nhận hàng</label><span>{{ data.diaChiNhanHang || '—' }}</span></div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z" />
            </svg>
          </span>
          <h3 class="panel-title">Chi tiết sản phẩm</h3>
          <span class="count-pill">{{ data.chiTiet.length }} dòng</span>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Sản phẩm</th>
                <th>SL</th>
                <th>Đơn giá</th>
                <th>Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!data.chiTiet.length">
                <td colspan="5" class="detail-empty">Không có dòng sản phẩm nào</td>
              </tr>
              <tr v-for="(line, i) in data.chiTiet" :key="line.id">
                <td>{{ i + 1 }}</td>
                <td>
                  <div class="entity-name">{{ line.tenSanPham }}</div>
                  <div class="entity-sub">{{ line.maSanPhamChiTiet }} • {{ line.tenKichCo }} • {{ line.tenMau }}</div>
                </td>
                <td>{{ line.soLuong }}</td>
                <td>{{ formatVnd(line.donGia) }}</td>
                <td class="cell-strong">{{ formatVnd(line.thanhTien) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1H6.32c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z" />
            </svg>
          </span>
          <h3 class="panel-title">Thanh toán</h3>
        </div>
        <div class="detail-totals">
          <div class="t-row"><span>Tổng tiền</span><span>{{ formatVnd(data.tongTien) }}</span></div>
          <div class="t-row"><span>Phí vận chuyển</span><span>{{ formatVnd(data.phiVanChuyen) }}</span></div>
          <div class="t-row"><span>Giảm giá</span><span class="t-discount">- {{ formatVnd(data.tienGiam != null ? data.tienGiam : giamGia) }}</span></div>
          <div v-if="data.tenGiamGiaUngDung" class="t-row"><span>Áp dụng</span><span>{{ data.tenGiamGiaUngDung }}</span></div>
          <div class="t-row t-final"><span>Tiền sau giảm giá</span><span>{{ formatVnd(data.tienSauGiamGia) }}</span></div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3zM14.5 6.5l3 3" />
            </svg>
          </span>
          <h3 class="panel-title">Ghi chú</h3>
        </div>
        <p class="note-box">{{ data.ghiChu || '—' }}</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-top {
  margin-bottom: 2px;
}

.btn-back {
  border: none;
  background: transparent;
  color: var(--red);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 0;
}

.btn-back:hover {
  text-decoration: underline;
}

.state-box {
  padding: 26px 0;
  text-align: center;
  color: #9a9aa3;
  font-size: 13.5px;
}

.state-box.is-error .state-msg {
  color: #dc2626;
  font-weight: 600;
  margin-bottom: 10px;
}

.detail-code {
  color: var(--red);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 18px;
  padding: 4px 2px 8px;
}

.d-item label {
  display: block;
  font-size: 11.5px;
  color: #9a9aa3;
  margin-bottom: 3px;
}

.d-item span {
  font-size: 13px;
  color: #2c2c33;
}

.d-wide {
  grid-column: span 3;
}

.detail-empty {
  text-align: center;
  color: #9a9aa3;
  padding: 22px 0;
}

.detail-totals {
  margin-left: auto;
  width: min(360px, 100%);
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 4px 2px 8px;
}

.t-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #4a4a52;
}

.t-discount {
  color: #16a34a;
}

.t-final {
  font-weight: 700;
  color: var(--red);
  font-size: 14px;
  border-top: 1px dashed #e3e3e8;
  padding-top: 8px;
}

.note-box {
  font-size: 13px;
  color: #4a4a52;
  line-height: 1.6;
  padding: 4px 2px 8px;
  white-space: pre-wrap;
}

@media (max-width: 760px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .d-wide {
    grid-column: span 1;
  }
}
</style>
