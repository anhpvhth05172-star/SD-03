<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getSanPhamClient } from "@/api/product";
import { money } from "../store";
import ProductCard from "./ProductCard.vue";

const route = useRoute();

// ===== Bộ lọc =====
const q = ref("");
const brand = ref(route.query.brand || "");
const type = ref("");
const sort = ref("");

// ===== Dữ liệu từ API =====
const allProducts = ref([]);
const loading = ref(false);
const error = ref("");

// Danh sách thương hiệu / loại giày từ dữ liệu thực
const brands = computed(() => [
  ...new Set(allProducts.value.map((p) => p.tenThuongHieu).filter(Boolean)),
]);
const types = computed(() => [
  ...new Set(allProducts.value.map((p) => p.tenLoaiGiay).filter(Boolean)),
]);

// ===== Fetch sản phẩm =====
async function fetchProducts() {
  loading.value = true;
  error.value = "";
  try {
    const res = await getSanPhamClient({ keyword: q.value || undefined });
    allProducts.value = res?.content ?? res ?? [];
  } catch (e) {
    error.value = e.message || "Không thể tải danh sách sản phẩm.";
  } finally {
    loading.value = false;
  }
}

onMounted(fetchProducts);

// Debounce khi gõ tìm kiếm
let searchTimer = null;
watch(q, () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(fetchProducts, 400);
});

// Đồng bộ brand từ query string
watch(
  () => route.query.brand,
  (val) => {
    brand.value = val || "";
  }
);

// ===== Lọc/sắp xếp phía FE =====
const list = computed(() => {
  let r = allProducts.value.filter(
    (p) =>
      (!brand.value || p.tenThuongHieu === brand.value) &&
      (!type.value || p.tenLoaiGiay === type.value)
  );
  if (sort.value === "asc")
    r = [...r].sort(
      (a, b) => Number(a.giaBanMin || a.giaBan || 0) - Number(b.giaBanMin || b.giaBan || 0)
    );
  if (sort.value === "desc")
    r = [...r].sort(
      (a, b) => Number(b.giaBanMin || b.giaBan || 0) - Number(a.giaBanMin || a.giaBan || 0)
    );
  return r;
});

const selectBrand = (b) => {
  brand.value = brand.value === b ? "" : b;
};

const reset = () => {
  q.value = "";
  brand.value = "";
  type.value = "";
  sort.value = "";
  fetchProducts();
};
</script>

<template>
  <div class="shop-container">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <span class="text-dark fw-semibold">Cửa hàng sneaker</span>
    </nav>

    <!-- Header Section (Clean White Theme) -->
    <div class="shop-header-banner p-4 p-md-5 rounded-4 mb-4 position-relative overflow-hidden bg-white border">
      <div class="row align-items-center g-4 position-relative z-2">
        <div class="col-lg-8">
          <span class="badge bg-danger-subtle text-danger px-3 py-2 rounded-pill fw-bold mb-2">
            🔥 BỘ SƯU TẬP 2026
          </span>
          <h1 class="display-5 fw-extrabold text-dark mb-2">Bộ Sưu Tập Sneaker Chính Hãng</h1>
          <p class="text-muted-ps mb-0" style="max-width: 580px;">
            Khám phá những mẫu giày hot trend từ Nike, Adidas, Jordan, Puma,... Đảm bảo 100% chính hãng, đổi size 7 ngày miễn phí tận nhà.
          </p>
        </div>
        <div class="col-lg-4 text-lg-end d-none d-lg-block">
          <div class="shop-stats-badge p-3 rounded-4 bg-light border d-inline-block text-start">
            <div class="d-flex align-items-center gap-3">
              <i class="bi bi-shield-fill-check text-danger fs-1"></i>
              <div>
                <div class="fw-bold fs-5 text-dark">Chính Hãng 100%</div>
                <div class="small text-muted-ps">Bảo hành keo chỉ 12 tháng</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Brand Pills -->
    <div v-if="brands.length" class="d-flex flex-wrap gap-2 mb-4 align-items-center">
      <span class="small fw-bold text-muted-ps me-1"><i class="bi bi-funnel"></i> Thương hiệu:</span>
      <button
        class="btn btn-sm rounded-pill px-3"
        :class="!brand ? 'btn-danger text-white fw-bold shadow-sm' : 'btn-light-border'"
        @click="brand = ''"
      >
        Tất cả
      </button>
      <button
        v-for="b in brands"
        :key="b"
        class="btn btn-sm rounded-pill px-3"
        :class="brand === b ? 'btn-danger text-white fw-bold shadow-sm' : 'btn-light-border'"
        @click="selectBrand(b)"
      >
        {{ b }}
      </button>
    </div>

    <!-- Filter Control Box -->
    <div class="ps-card p-3 p-md-4 mb-4 shadow-sm border-0">
      <div class="row g-3 align-items-end">
        <div class="col-md-5">
          <label class="form-label fw-bold small text-dark mb-1">
            <i class="bi bi-search text-danger me-1"></i> Tìm kiếm giày
          </label>
          <div class="input-group">
            <span class="input-group-text bg-light border-end-0">
              <i class="bi bi-search text-muted"></i>
            </span>
            <input
              v-model="q"
              class="form-control border-start-0 bg-light"
              placeholder="Nhập tên giày, mã sản phẩm hoặc thương hiệu..."
            />
          </div>
        </div>

        <div class="col-6 col-md-3">
          <label class="form-label fw-bold small text-dark mb-1">
            <i class="bi bi-grid text-danger me-1"></i> Loại giày
          </label>
          <select v-model="type" class="form-select bg-light">
            <option value="">Tất cả phân loại</option>
            <option v-for="t in types" :key="t">{{ t }}</option>
          </select>
        </div>

        <div class="col-6 col-md-3">
          <label class="form-label fw-bold small text-dark mb-1">
            <i class="bi bi-arrow-down-up text-danger me-1"></i> Giá bán
          </label>
          <select v-model="sort" class="form-select bg-light">
            <option value="">Mặc định (Mới nhất)</option>
            <option value="asc">Giá thấp đến cao</option>
            <option value="desc">Giá cao đến thấp</option>
          </select>
        </div>

        <div class="col-md-1 text-md-end">
          <button
            class="btn btn-light-border w-100 p-2"
            title="Đặt lại bộ lọc"
            @click="reset"
          >
            <i class="bi bi-arrow-counterclockwise fs-5"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- Active Filter Tags Indicator -->
    <div v-if="brand || type || q" class="d-flex align-items-center gap-2 mb-3 small flex-wrap">
      <span class="text-muted-ps">Đang lọc theo:</span>
      <span v-if="q" class="badge bg-danger-subtle text-danger p-2 rounded-pill">
        Từ khóa: "{{ q }}"
      </span>
      <span v-if="brand" class="badge bg-danger-subtle text-danger p-2 rounded-pill">
        Hãng: {{ brand }}
      </span>
      <span v-if="type" class="badge bg-danger-subtle text-danger p-2 rounded-pill">
        Loại: {{ type }}
      </span>
      <button class="btn btn-link btn-sm text-muted-ps p-0 text-decoration-none" @click="reset">
        (Xóa tất cả)
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-danger" role="status">
        <span class="visually-hidden">Đang tải sản phẩm...</span>
      </div>
      <p class="mt-2 text-muted-ps">Đang tải danh sách giày chính hãng...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="ps-card text-center p-5">
      <div class="fs-1 mb-2">⚠️</div>
      <h5 class="fw-bold">Lỗi kết nối máy chủ</h5>
      <p class="text-muted-ps">{{ error }}</p>
      <button class="btn btn-primary px-4 rounded-pill" @click="fetchProducts">
        <i class="bi bi-arrow-clockwise me-1"></i> Thử lại
      </button>
    </div>

    <!-- Product Grid -->
    <template v-else>
      <div class="d-flex justify-content-between align-items-center mb-3">
        <p class="text-muted-ps mb-0 small">
          Hiển thị <strong>{{ list.length }}</strong> trên tổng số <strong>{{ allProducts.length }}</strong> sản phẩm
        </p>
      </div>

      <div v-if="list.length" class="row g-3 g-md-4">
        <div v-for="p in list" :key="p.id" class="col-6 col-md-4 col-lg-3">
          <ProductCard :product="p" />
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="ps-card text-center p-5">
        <div class="fs-1 mb-3">👟</div>
        <h4 class="fw-bold">Không tìm thấy sản phẩm phù hợp</h4>
        <p class="text-muted-ps mb-4" style="max-width: 420px; margin: 0 auto;">
          Không có đôi sneaker nào khớp với bộ lọc hiện tại của bạn. Thử tìm kiếm với từ khóa khác hoặc đặt lại bộ lọc nhé!
        </p>
        <button class="btn btn-primary rounded-pill px-4" @click="reset">
          <i class="bi bi-arrow-counterclockwise me-1"></i> Xóa tất cả bộ lọc
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.shop-header-banner {
  background: #ffffff;
  border-color: #e2e8f0 !important;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}

.shop-stats-badge {
  border-color: #fee2e2 !important;
  background: #fff5f5 !important;
}
</style>
