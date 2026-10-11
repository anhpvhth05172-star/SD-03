<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="text-center text-muted py-5">
      <span class="spinner-border spinner-border-sm me-2"></span>
      Đang tải chi tiết phiếu...
    </div>

    <!-- Lỗi -->
    <div v-else-if="!voucher" class="card border-0 shadow-sm voucher-card">
      <div class="card-body text-center py-5">
        <i class="bi bi-ticket-perforated fs-1 text-muted"></i>
        <p class="mt-3 mb-4 text-muted">{{ loadError }}</p>
        <button class="btn btn-light border px-4" @click="router.back()">
          Quay lại
        </button>
      </div>
    </div>

    <template v-else>
      <!-- Thông tin chính -->
      <div class="card voucher-card border-0 shadow-sm mb-4">
        <div class="card-body p-4">
          <div
            class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4"
          >
            <div class="d-flex align-items-center gap-3">
              <div class="voucher-icon">
                <i class="bi bi-ticket-perforated"></i>
              </div>
              <div>
                <h5 class="mb-1 fw-semibold">{{ voucher.tenPhieuGiamGia }}</h5>
                <span class="voucher-code">{{ voucher.maPhieuGiamGia }}</span>
              </div>
            </div>
            <div class="d-flex align-items-center gap-2">
              <span class="badge rounded-pill" :class="status.cls">
                {{ status.text }}
              </span>
              <span class="badge rounded-pill bg-voucher-soft">
                {{ isPrivate ? "Cá nhân" : "Công khai" }}
              </span>
            </div>
          </div>

          <div class="row g-4">
            <div class="col-md-4">
              <div class="info-label">Loại giảm</div>
              <div class="info-value">
                {{
                  voucher.loaiGiamGia === "PERCENT" ? "Phần trăm" : "Tiền mặt"
                }}
              </div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Giá trị giảm</div>
              <div class="info-value text-voucher fw-bold">
                {{ discountText }}
              </div>
            </div>
            <div class="col-md-4" v-if="voucher.loaiGiamGia === 'PERCENT'">
              <div class="info-label">Giảm tối đa</div>
              <div class="info-value">
                {{
                  voucher.giamToiDa
                    ? money(voucher.giamToiDa)
                    : "Không giới hạn"
                }}
              </div>
            </div>

            <div class="col-md-4">
              <div class="info-label">Đơn tối thiểu</div>
              <div class="info-value">
                {{ money(voucher.hoaDonToiThieu || 0) }}
              </div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Giới hạn mỗi tài khoản</div>
              <div class="info-value">{{ voucher.gioiHanMoiTaiKhoan }} lần</div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Số lượt đã dùng</div>
              <div class="info-value">
                {{ voucher.soLuongDaSuDung || 0 }} /
                {{ unlimited ? "Vô hạn" : voucher.soLuong }}
              </div>
              <div v-if="!unlimited" class="progress mt-2" style="height: 6px">
                <div
                  class="progress-bar bar-voucher"
                  :style="{ width: usedPercent + '%' }"
                ></div>
              </div>
            </div>

            <div class="col-md-4">
              <div class="info-label">Ngày bắt đầu</div>
              <div class="info-value">{{ dateTime(voucher.ngayBatDau) }}</div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Ngày kết thúc</div>
              <div class="info-value">{{ dateTime(voucher.ngayKetThuc) }}</div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Ngày tạo</div>
              <div class="info-value">{{ dateTime(voucher.ngayTao) }}</div>
            </div>

            <div class="col-12">
              <div class="info-label">Mô tả</div>
              <div class="info-value">{{ voucher.moTa || "—" }}</div>
            </div>
          </div>

          <hr class="my-4" />
          <button class="btn btn-light border px-4" @click="router.back()">
            <i class="bi bi-arrow-left me-2"></i>Quay lại
          </button>
        </div>
      </div>

      <!-- Danh sách khách hàng (chỉ phiếu cá nhân) -->
      <div v-if="isPrivate" class="card voucher-card border-0 shadow-sm">
        <div class="card-body p-4">
          <div
            class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3"
          >
            <h6 class="mb-0 fw-semibold">
              Khách hàng được áp dụng
              <span class="badge bg-voucher-soft ms-2">
                {{ customers.length }}
              </span>
              <span class="small text-muted ms-2">
                (đã dùng {{ usedCount }})
              </span>
            </h6>
            <input
              v-model.trim="keyword"
              type="text"
              class="form-control search-input"
              placeholder="Tìm theo tên, email, SĐT..."
            />
          </div>

          <div class="table-responsive">
            <table class="table align-middle mb-0">
              <thead>
                <tr>
                  <th style="width: 60px">#</th>
                  <th>Khách hàng</th>
                  <th>Email</th>
                  <th>Số điện thoại</th>
                  <th>Trạng thái</th>
                  <th>Ngày sử dụng</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!filteredCustomers.length">
                  <td colspan="6" class="text-center text-muted py-4">
                    Không có khách hàng phù hợp
                  </td>
                </tr>
                <tr v-for="(c, i) in filteredCustomers" :key="c.idKhachHang">
                  <td>{{ i + 1 }}</td>
                  <td>
                    <div class="fw-semibold">{{ c.tenKhachHang }}</div>
                    <div class="small text-muted">{{ c.maKhachHang }}</div>
                  </td>
                  <td>{{ c.email || "—" }}</td>
                  <td>{{ c.soDienThoai || "—" }}</td>
                  <td>
                    <span
                      class="badge rounded-pill"
                      :class="c.ngaySuDung ? 'bg-secondary' : 'bg-success'"
                    >
                      {{ c.ngaySuDung ? "Đã dùng" : "Chưa dùng" }}
                    </span>
                  </td>
                  <td>{{ c.ngaySuDung ? dateTime(c.ngaySuDung) : "—" }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted } from "vue";
import axios from "axios";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const baseAPI = inject("baseAPI");

const UNLIMITED_QTY = 2147483647;

const loading = ref(true);
const loadError = ref("");
const voucher = ref(null);
const customers = ref([]);
const keyword = ref("");

const isPrivate = computed(() => voucher.value?.phamVi === "CA_NHAN");
const unlimited = computed(() => voucher.value?.soLuong >= UNLIMITED_QTY);

const usedPercent = computed(() => {
  const v = voucher.value;
  if (!v || !v.soLuong) return 0;
  return Math.min(
    100,
    Math.round(((v.soLuongDaSuDung || 0) / v.soLuong) * 100),
  );
});

const usedCount = computed(
  () => customers.value.filter((c) => c.ngaySuDung).length,
);

const filteredCustomers = computed(() => {
  const kw = keyword.value.toLowerCase();
  if (!kw) return customers.value;
  return customers.value.filter((c) =>
    [c.tenKhachHang, c.maKhachHang, c.email, c.soDienThoai].some((v) =>
      (v || "").toLowerCase().includes(kw),
    ),
  );
});

const discountText = computed(() => {
  const v = voucher.value;
  if (!v) return "";
  return v.loaiGiamGia === "PERCENT" ? `${v.giaTriGiam}%` : money(v.giaTriGiam);
});

const status = computed(() => {
  const v = voucher.value;
  if (!v) return {};
  const now = new Date();
  if (!v.trangThai) return { text: "Tạm tắt", cls: "bg-secondary" };
  if (new Date(v.ngayKetThuc) < now)
    return { text: "Đã hết hạn", cls: "bg-dark" };
  if (!unlimited.value && (v.soLuongDaSuDung || 0) >= v.soLuong)
    return { text: "Hết lượt", cls: "bg-warning text-dark" };
  if (new Date(v.ngayBatDau) > now)
    return { text: "Sắp diễn ra", cls: "bg-info text-dark" };
  return { text: "Đang diễn ra", cls: "bg-success" };
});

const money = (n) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    n ?? 0,
  );

const dateTime = (v) =>
  v
    ? new Date(v).toLocaleString("vi-VN", {
        hour12: false,
        dateStyle: "short",
        timeStyle: "short",
      })
    : "—";

onMounted(async () => {
  try {
    const res = await axios.get(
      baseAPI + "admin/phieu-giam-gia/detail/" + route.params.id,
    );
    const { khachHangs, ...info } = res.data;
    voucher.value = info;
    customers.value = khachHangs || [];
  } catch (e) {
    loadError.value =
      e.response?.status === 404
        ? "Không tìm thấy phiếu giảm giá"
        : e.response?.data?.message || "Không kết nối được máy chủ";
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.voucher-card {
  border-radius: 1.25rem;
}
.voucher-icon {
  width: 44px;
  height: 44px;
  border-radius: 0.75rem;
  background: #fde8ec;
  color: #e8284a;
  display: grid;
  place-items: center;
  font-size: 1.15rem;
}
.voucher-code {
  display: inline-block;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.85rem;
  color: #e8284a;
  background: #fde8ec;
  border: 1px dashed #e8284a;
  border-radius: 0.4rem;
  padding: 0.1rem 0.6rem;
}
.info-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 0.25rem;
}
.info-value {
  font-size: 0.95rem;
  color: #111827;
}
.text-voucher {
  color: #e8284a;
}
.bg-voucher-soft {
  background: #fde8ec;
  color: #e8284a;
}
.bar-voucher {
  background-color: #e8284a;
}
.search-input {
  max-width: 280px;
  background-color: #f8f9fb;
}
.search-input:focus {
  background-color: #fff;
  border-color: #e8284a;
  box-shadow: 0 0 0 0.2rem rgba(232, 40, 74, 0.15);
}
.table thead th {
  font-size: 0.8rem;
  font-weight: 600;
  color: #6b7280;
  background: #f8f9fb;
}
</style>
