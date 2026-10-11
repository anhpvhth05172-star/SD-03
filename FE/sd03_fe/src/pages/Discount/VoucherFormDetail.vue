<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="text-center text-muted py-5">
      <span class="spinner-border spinner-border-sm me-2"></span>
      Đang tải chi tiết đợt giảm giá...
    </div>

    <!-- Lỗi -->
    <div v-else-if="!dot" class="card border-0 shadow-sm dot-card">
      <div class="card-body text-center py-5">
        <i class="bi bi-percent fs-1 text-muted"></i>
        <p class="mt-3 mb-4 text-muted">{{ loadError }}</p>
        <button class="btn btn-light border px-4" @click="router.back()">
          Quay lại
        </button>
      </div>
    </div>

    <template v-else>
      <!-- Thông tin chính -->
      <div class="card dot-card border-0 shadow-sm mb-4">
        <div class="card-body p-4">
          <div
            class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4"
          >
            <div class="d-flex align-items-center gap-3">
              <div class="dot-icon">
                <i class="bi bi-percent"></i>
              </div>
              <div>
                <h5 class="mb-1 fw-semibold">{{ dot.tenDotGiamGia }}</h5>
                <span class="dot-code">{{ dot.maDotGiamGia }}</span>
              </div>
            </div>
            <span class="badge rounded-pill" :class="status.cls">
              {{ status.text }}
            </span>
          </div>

          <div class="row g-4">
            <div class="col-md-4">
              <div class="info-label">Phần trăm giảm</div>
              <div class="info-value text-dot fw-bold fs-5">
                {{ dot.phanTramGiam }}%
              </div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Ngày bắt đầu</div>
              <div class="info-value">{{ dateTime(dot.ngayBatDau) }}</div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Ngày kết thúc</div>
              <div class="info-value">{{ dateTime(dot.ngayKetThuc) }}</div>
            </div>

            <div class="col-md-4">
              <div class="info-label">Số sản phẩm</div>
              <div class="info-value">{{ productCount }}</div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Số biến thể áp dụng</div>
              <div class="info-value">{{ items.length }}</div>
            </div>
            <div class="col-md-4">
              <div class="info-label">Ngày tạo</div>
              <div class="info-value">{{ dateTime(dot.ngayTao) }}</div>
            </div>

            <div class="col-12">
              <div class="info-label">Mô tả</div>
              <div class="info-value">{{ dot.moTa || "—" }}</div>
            </div>
          </div>

          <hr class="my-4" />
          <button class="btn btn-light border px-4" @click="router.back()">
            <i class="bi bi-arrow-left me-2"></i>Quay lại
          </button>
        </div>
      </div>

      <!-- Sản phẩm áp dụng -->
      <div class="card dot-card border-0 shadow-sm">
        <div class="card-body p-4">
          <div
            class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3"
          >
            <h6 class="mb-0 fw-semibold">
              Sản phẩm áp dụng
              <span class="badge bg-dot-soft ms-2">{{ items.length }}</span>
            </h6>
            <input
              v-model.trim="keyword"
              type="text"
              class="form-control search-input"
              placeholder="Tìm theo tên, mã sản phẩm, màu, size..."
            />
          </div>

          <div class="table-responsive product-list">
            <table class="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Sản phẩm / Mã chi tiết</th>
                  <th>Size</th>
                  <th>Màu</th>
                  <th class="text-end">Giá gốc</th>
                  <th class="text-end">Giá sau giảm</th>
                  <th class="text-end">Tồn</th>
                  <th>Áp dụng</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!groups.length">
                  <td colspan="7" class="text-center text-muted py-4">
                    Không có sản phẩm phù hợp
                  </td>
                </tr>
                <template v-for="g in groups" :key="g.sanPhamId">
                  <tr class="group-row">
                    <td colspan="7">
                      <span class="fw-semibold">{{ g.tenSanPham }}</span>
                      <span class="text-muted small ms-2">
                        {{ g.maSanPham }} · {{ g.items.length }} biến thể
                      </span>
                    </td>
                  </tr>
                  <tr v-for="p in g.items" :key="p.id" class="variant-row">
                    <td class="ps-4 small">{{ p.maChiTiet }}</td>
                    <td>{{ p.kichCo }}</td>
                    <td>{{ p.mauSac }}</td>
                    <td class="text-end text-muted">
                      <s>{{ money(p.giaBan) }}</s>
                    </td>
                    <td class="text-end">
                      <span class="text-dot fw-semibold">
                        {{ money(p.giaSauGiam) }}
                      </span>
                    </td>
                    <td class="text-end">{{ p.soLuong }}</td>
                    <td>
                      <span
                        class="badge rounded-pill"
                        :class="p.trangThai ? 'bg-success' : 'bg-secondary'"
                      >
                        {{ p.trangThai ? "Đang áp dụng" : "Đã gỡ" }}
                      </span>
                    </td>
                  </tr>
                </template>
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

const loading = ref(true);
const loadError = ref("");
const dot = ref(null);
const items = ref([]);
const keyword = ref("");

const productCount = computed(
  () => new Set(items.value.map((p) => p.sanPhamId)).size,
);

const filteredItems = computed(() => {
  const kw = keyword.value.toLowerCase();
  if (!kw) return items.value;
  return items.value.filter((p) =>
    [p.tenSanPham, p.maSanPham, p.maChiTiet, p.kichCo, p.mauSac].some((v) =>
      String(v ?? "")
        .toLowerCase()
        .includes(kw),
    ),
  );
});

// Gom biến thể theo sản phẩm (giữ thứ tự từ server)
const groups = computed(() => {
  const map = new Map();
  for (const p of filteredItems.value) {
    if (!map.has(p.sanPhamId)) {
      map.set(p.sanPhamId, {
        sanPhamId: p.sanPhamId,
        maSanPham: p.maSanPham,
        tenSanPham: p.tenSanPham,
        items: [],
      });
    }
    map.get(p.sanPhamId).items.push(p);
  }
  return [...map.values()];
});

const status = computed(() => {
  const d = dot.value;
  if (!d) return {};
  const now = new Date();
  if (!d.trangThai) return { text: "Tạm tắt", cls: "bg-secondary" };
  if (new Date(d.ngayKetThuc) < now)
    return { text: "Đã kết thúc", cls: "bg-dark" };
  if (new Date(d.ngayBatDau) > now)
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
      baseAPI + "admin/dot-giam-gia/detail/" + route.params.id,
    );
    const { sanPhams, ...info } = res.data;
    dot.value = info;
    items.value = sanPhams || [];
  } catch (e) {
    loadError.value =
      e.response?.status === 404
        ? "Không tìm thấy đợt giảm giá"
        : e.response?.data?.message || "Không kết nối được máy chủ";
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.dot-card {
  border-radius: 1.25rem;
}
.dot-icon {
  width: 44px;
  height: 44px;
  border-radius: 0.75rem;
  background: #fde8ec;
  color: #e8284a;
  display: grid;
  place-items: center;
  font-size: 1.15rem;
}
.dot-code {
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
.text-dot {
  color: #e8284a;
}
.bg-dot-soft {
  background: #fde8ec;
  color: #e8284a;
}
.search-input {
  max-width: 320px;
  background-color: #f8f9fb;
}
.search-input:focus {
  background-color: #fff;
  border-color: #e8284a;
  box-shadow: 0 0 0 0.2rem rgba(232, 40, 74, 0.15);
}
.product-list {
  max-height: 420px;
  overflow-y: auto;
}
.product-list thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8f9fb;
  font-size: 0.78rem;
  font-weight: 600;
  color: #6b7280;
}
.group-row td {
  background: #fff5f7;
  border-top: 1px solid #f3d3da;
}
.variant-row:hover td {
  background: #fafafa;
}
</style>
