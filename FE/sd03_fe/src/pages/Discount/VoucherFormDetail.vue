<template>
  <div class="card voucher-card border-0 shadow-sm">
    <div class="card-body p-4">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-danger"></div>
      </div>

      <div v-else-if="!d" class="text-center text-muted py-5">
        Không tìm thấy đợt giảm giá
      </div>

      <template v-else>
        <!-- Header -->
        <div
          class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4"
        >
          <div class="d-flex align-items-center gap-3">
            <div class="voucher-icon"><i class="bi bi-percent"></i></div>
            <div>
              <div class="d-flex align-items-center gap-2">
                <span class="fw-semibold fs-5 font-monospace">{{
                  d.maDotGiamGia
                }}</span>
                <button class="btn btn-sm btn-light border" @click="copyCode">
                  <i class="bi bi-clipboard"></i>
                  {{ copied ? "Đã chép" : "Sao chép" }}
                </button>
              </div>
              <div class="text-muted small">{{ d.tenDotGiamGia }}</div>
            </div>
          </div>
          <span class="badge px-3 py-2" :class="status.class">{{
            status.text
          }}</span>
        </div>

        <!-- Số liệu chính -->
        <div class="row g-3 mb-4">
          <div class="col-md-6">
            <div class="stat-box">
              <div class="stat-label">Mức giảm</div>
              <div class="stat-value">{{ Number(d.phanTramGiam) }}%</div>
            </div>
          </div>
          <div class="col-md-6">
            <div class="stat-box">
              <div class="stat-label">Thời lượng</div>
              <div class="stat-value">{{ totalDays }} ngày</div>
            </div>
          </div>
          <!-- <div class="col-md-4">
            <div class="stat-box">
              <div class="stat-label">Mô tả</div>
              <div class="stat-value">
                {{ d.moTa }}
              </div>
            </div>
          </div> -->
        </div>

        <!-- Tiến độ thời gian -->
        <div class="border rounded-3 p-3 mb-4">
          <div class="d-flex justify-content-between small mb-2">
            <span class="text-muted">Tiến độ thời gian</span>
            <span class="fw-semibold">{{ progressText }}</span>
          </div>
          <div class="progress" style="height: 8px">
            <div
              class="progress-bar bg-danger"
              :style="{ width: progress + '%' }"
            ></div>
          </div>
          <div class="d-flex justify-content-between small text-muted mt-1">
            <span>{{ date(d.ngayBatDau) }}</span>
            <span>{{ date(d.ngayKetThuc) }}</span>
          </div>
        </div>

        <!-- Thông tin khác -->
        <table class="table table-borderless align-middle mb-0">
          <tbody>
            <tr>
              <td class="text-muted w-25">Ngày bắt đầu</td>
              <td>{{ dateTime(d.ngayBatDau) }}</td>
            </tr>
            <tr>
              <td class="text-muted">Ngày kết thúc</td>
              <td>{{ dateTime(d.ngayKetThuc) }}</td>
            </tr>
            <tr>
              <td class="text-muted">Ngày tạo</td>
              <td>{{ dateTime(d.ngayTao) }}</td>
            </tr>
          </tbody>
        </table>

        <hr class="my-4" />

        <div class="d-flex gap-2">
          <button
            class="btn btn-voucher px-4"
            @click="router.push(`/admin/dot-giam-gia/edit/${d.id}`)"
          >
            <i class="bi bi-pencil me-2"></i>Sửa đợt
          </button>
          <button
            class="btn btn-light border px-4"
            :disabled="toggling"
            @click="toggleStatus"
          >
            <i
              class="bi me-2"
              :class="d.trangThai ? 'bi-pause-circle' : 'bi-play-circle'"
            ></i>
            {{ d.trangThai ? "Tắt đợt giảm giá" : "Bật đợt giảm giá" }}
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

const d = ref({});
const loading = ref(true);
const copied = ref(false);
const toggling = ref(false);

const DAY = 24 * 60 * 60 * 1000;

const status = computed(() => {
  const x = d.value;
  const now = new Date();
  if (!x.trangThai)
    return { text: "Tạm dừng", class: "bg-danger-subtle text-danger-emphasis" };
  if (new Date(x.ngayBatDau) > now)
    return {
      text: "Sắp diễn ra",
      class: "bg-primary-subtle text-primary-emphasis",
    };
  if (new Date(x.ngayKetThuc) < now)
    return {
      text: "Đã kết thúc",
      class: "bg-secondary-subtle text-secondary-emphasis",
    };
  return {
    text: "Đang diễn ra",
    class: "bg-success-subtle text-success-emphasis",
  };
});

const totalDays = computed(() =>
  Math.round(
    (new Date(d.value.ngayKetThuc) - new Date(d.value.ngayBatDau)) / DAY,
  ),
);

const progress = computed(() => {
  const start = new Date(d.value.ngayBatDau).getTime();
  const end = new Date(d.value.ngayKetThuc).getTime();
  const pct = ((Date.now() - start) / (end - start)) * 100;
  return Math.max(0, Math.min(100, Math.round(pct)));
});

const progressText = computed(() => {
  const now = Date.now();
  const start = new Date(d.value.ngayBatDau).getTime();
  const end = new Date(d.value.ngayKetThuc).getTime();
  if (now < start) return `Bắt đầu sau ${Math.ceil((start - now) / DAY)} ngày`;
  if (now > end)
    return `Đã kết thúc cách đây ${Math.floor((now - end) / DAY)} ngày`;
  return `Còn ${Math.ceil((end - now) / DAY)} ngày`;
});

const date = (s) => (s ? new Date(s).toLocaleDateString("vi-VN") : "—");
const dateTime = (s) => (s ? new Date(s).toLocaleString("vi-VN") : "—");

const copyCode = async () => {
  await navigator.clipboard.writeText(d.value.maDotGiamGia);
  copied.value = true;
  setTimeout(() => (copied.value = false), 1500);
};

const load = async () => {
  try {
    const { data } = await axios.get(
      baseAPI + "admin/dot-giam-gia/detail/" + route.params.id,
    );
    d.value = data;
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Không tải được dữ liệu",
      text: e.response?.data?.message,
    });
  } finally {
    loading.value = false;
  }
};

const toggleStatus = async () => {
  const turningOn = !d.value.trangThai;
  const ended = new Date(d.value.ngayKetThuc) < new Date();
  if (turningOn && ended) {
    const r = await Swal.fire({
      icon: "warning",
      title: "Đợt này đã kết thúc",
      text: "Bật lại cũng không có hiệu lực vì đã quá ngày kết thúc. Vẫn bật?",
      showCancelButton: true,
      confirmButtonText: "Vẫn bật",
      cancelButtonText: "Hủy",
    });
    if (!r.isConfirmed) return;
  }
  toggling.value = true;
  try {
    // Đổi URL cho đúng endpoint backend của bạn
    await axios.put(baseAPI + `admin/dot-giam-gia/${d.value.id}/trang-thai`, {
      trangThai: turningOn,
    });
    d.value.trangThai = turningOn;
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Cập nhật thất bại",
      text: e.response?.data?.message,
    });
  } finally {
    toggling.value = false;
  }
};

onMounted(load);
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
