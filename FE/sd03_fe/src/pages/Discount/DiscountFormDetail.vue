<template>
  <div class="card voucher-card border-0 shadow-sm">
    <div class="card-body p-4">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-danger"></div>
      </div>

      <div v-else-if="!v" class="text-center text-muted py-5">
        Không tìm thấy phiếu giảm giá
      </div>

      <template v-else>
        <!-- Header -->
        <div
          class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4"
        >
          <div class="d-flex align-items-center gap-3">
            <div class="voucher-icon">
              <i class="bi bi-ticket-perforated"></i>
            </div>
            <div>
              <div class="d-flex align-items-center gap-2">
                <span class="fw-semibold fs-5 font-monospace">{{
                  v.maPhieuGiamGia
                }}</span>
                <button class="btn btn-sm btn-light border" @click="copyCode">
                  <i class="bi bi-clipboard"></i>
                  {{ copied ? "Đã chép" : "Sao chép" }}
                </button>
              </div>
              <div class="text-muted small">{{ v.tenPhieuGiamGia }}</div>
            </div>
          </div>
          <span class="badge px-3 py-2" :class="status.class">{{
            status.text
          }}</span>
        </div>

        <!-- Số liệu chính -->
        <div class="row g-3 mb-4">
          <div class="col-md-4">
            <div class="stat-box">
              <div class="stat-label">Giá trị giảm</div>
              <div class="stat-value">
                {{ isPercent ? v.giaTriGiam + "%" : money(v.giaTriGiam) }}
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="stat-box">
              <div class="stat-label">Giảm tối đa</div>
              <div class="stat-value">
                {{ isPercent && v.giamToiDa ? money(v.giamToiDa) : "—" }}
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="stat-box">
              <div class="stat-label">Đơn tối thiểu</div>
              <div class="stat-value">
                {{
                  v.hoaDonToiThieu ? money(v.hoaDonToiThieu) : "Không yêu cầu"
                }}
              </div>
            </div>
          </div>
        </div>

        <!-- Lượt sử dụng -->
        <div class="border rounded-3 p-3 mb-4">
          <div class="d-flex justify-content-between small mb-2">
            <span class="text-muted">Lượt sử dụng</span>
            <span class="fw-semibold" v-if="unlimited"
              >{{ v.soLuongDaSuDung }} lượt (vô hạn)</span
            >
            <span class="fw-semibold" v-else>
              {{ v.soLuongDaSuDung }} / {{ v.soLuong }} (còn
              {{ v.soLuong - v.soLuongDaSuDung }})
            </span>
          </div>
          <div class="progress" style="height: 8px" v-if="!unlimited">
            <div
              class="progress-bar bg-danger"
              :style="{ width: percentUsed + '%' }"
            ></div>
          </div>
        </div>

        <!-- Thông tin khác -->
        <table class="table table-borderless align-middle mb-0">
          <tbody>
            <tr>
              <td class="text-muted w-25">Loại giảm</td>
              <td>{{ isPercent ? "Phần trăm (%)" : "Tiền mặt (VNĐ)" }}</td>
            </tr>
            <tr>
              <td class="text-muted">Ngày bắt đầu</td>
              <td>{{ dateTime(v.ngayBatDau) }}</td>
            </tr>
            <tr>
              <td class="text-muted">Ngày kết thúc</td>
              <td>{{ dateTime(v.ngayKetThuc) }}</td>
            </tr>
            <tr>
              <td class="text-muted">Ngày tạo</td>
              <td>{{ dateTime(v.ngayTao) }}</td>
            </tr>
            <tr>
              <td class="text-muted">Cập nhật gần nhất</td>
              <td>{{ dateTime(v.ngayCapNhat) }}</td>
            </tr>
            <tr>
              <td class="text-muted">Mô tả</td>
              <td>{{ v.moTa || "—" }}</td>
            </tr>
          </tbody>
        </table>

        <hr class="my-4" />

        <div class="d-flex gap-2">
          <button
            class="btn btn-voucher px-4"
            @click="router.push(`/admin/phieu-giam-gia/${v.id}/edit`)"
          >
            <i class="bi bi-pencil me-2"></i>Sửa phiếu
          </button>
          <button class="btn btn-light border px-4" @click="router.back()">
            <i class="bi bi-arrow-left me-2"></i>Quay lại
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted } from "vue";
import axios from "axios";
import Swal from "sweetalert2";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const baseAPI = inject("baseAPI");

const v = ref({});

const UNLIMITED_QTY = 2147483647;

// Giá trị phải khớp với CHECK constraint trong DB
const isPercent = computed(() => v.value?.loaiGiamGia === "PRECENT");
const unlimited = computed(() => v.value?.soLuong >= UNLIMITED_QTY);
const percentUsed = computed(() =>
  Math.min(100, Math.round((v.value.soLuongDaSuDung / v.value.soLuong) * 100)),
);

const status = computed(() => {
  const d = v.value;
  const now = new Date();
  if (!d.trangThai)
    return {
      text: "Ngừng áp dụng",
      class: "bg-danger-subtle text-danger-emphasis",
    };
  if (new Date(d.ngayBatDau) > now)
    return {
      text: "Sắp diễn ra",
      class: "bg-primary-subtle text-primary-emphasis",
    };
  if (new Date(d.ngayKetThuc) < now)
    return {
      text: "Hết hạn",
      class: "bg-secondary-subtle text-secondary-emphasis",
    };
  if (!unlimited.value && d.soLuongDaSuDung >= d.soLuong)
    return {
      text: "Hết lượt",
      class: "bg-warning-subtle text-warning-emphasis",
    };
  return {
    text: "Đang hoạt động",
    class: "bg-success-subtle text-success-emphasis",
  };
});

const money = (n) => new Intl.NumberFormat("vi-VN").format(n) + " ₫";
const dateTime = (s) => (s ? new Date(s).toLocaleString("vi-VN") : "—");

const copyCode = async () => {
  await navigator.clipboard.writeText(v.value.maPhieuGiamGia);
  copied.value = true;
  setTimeout(() => (copied.value = false), 1500);
};

onMounted(async () => {
  try {
    const { data } = await axios.get(
      baseAPI + "admin/phieu-giam-gia/detail" + "/" + route.params.id,
    );
    v.value = data;
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Không tải được dữ liệu",
      text: e.response?.data?.message,
    });
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
.stat-box {
  background: #f8f9fb;
  border-radius: 0.75rem;
  padding: 1rem;
}
.stat-label {
  font-size: 0.8rem;
  color: #6b7280;
}
.stat-value {
  font-size: 1.4rem;
  font-weight: 600;
}
.btn-voucher {
  background-color: #e8284a;
  border-color: #e8284a;
  color: #fff;
  font-weight: 600;
}
.btn-voucher:hover {
  background-color: #cf1f3f;
  border-color: #cf1f3f;
  color: #fff;
}
</style>
