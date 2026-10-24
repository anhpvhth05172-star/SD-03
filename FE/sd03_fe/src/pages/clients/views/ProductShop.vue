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
    // res có thể là PageResponse { content: [...], totalElements, ... }
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

const reset = () => {
  q.value = "";
  brand.value = "";
  type.value = "";
  sort.value = "";
  fetchProducts();
};
</script>

<template>
  <h2 class="mb-4">Sản phẩm</h2>

  <div class="ps-card p-3 p-md-4 mb-4">
    <div class="row g-3">
      <div class="col-md-4">
        <label class="form-label fw-semibold small">Tìm kiếm</label>
        <input
          v-model="q"
          class="form-control"
          placeholder="Nhập tên hoặc mã sản phẩm (vd: SP001, Nike...)"
        />
      </div>
      <div class="col-6 col-md-3">
        <label class="form-label fw-semibold small">Thương hiệu</label>
        <select v-model="brand" class="form-select">
          <option value="">Tất cả thương hiệu</option>
          <option v-for="b in brands" :key="b">{{ b }}</option>
        </select>
      </div>
      <div class="col-6 col-md-3">
        <label class="form-label fw-semibold small">Loại giày</label>
        <select v-model="type" class="form-select">
          <option value="">Tất cả loại giày</option>
          <option v-for="t in types" :key="t">{{ t }}</option>
        </select>
      </div>
      <div class="col-md-2">
        <label class="form-label fw-semibold small">Sắp xếp</label>
        <select v-model="sort" class="form-select">
          <option value="">Mặc định</option>
          <option value="asc">Giá tăng dần</option>
          <option value="desc">Giá giảm dần</option>
        </select>
      </div>
    </div>
  </div>

  <!-- Loading -->
  <div v-if="loading" class="text-center py-5">
    <div class="spinner-border text-primary" role="status">
      <span class="visually-hidden">Đang tải...</span>
    </div>
    <p class="mt-2 text-muted-ps">Đang tải sản phẩm...</p>
  </div>

  <!-- Error -->
  <div v-else-if="error" class="ps-card text-center p-5">
    <div class="fs-1">⚠️</div>
    <h5 class="mt-2">Lỗi tải dữ liệu</h5>
    <p class="text-muted-ps">{{ error }}</p>
    <button class="btn btn-primary px-4" @click="fetchProducts">Thử lại</button>
  </div>

  <!-- Danh sách sản phẩm -->
  <template v-else>
    <p class="text-muted-ps">
      {{ list.length }} / {{ allProducts.length }} sản phẩm
    </p>

    <div v-if="list.length" class="row g-3">
      <div v-for="p in list" :key="p.id" class="col-6 col-md-4 col-lg-3">
        <ProductCard :product="p" />
      </div>
    </div>
    <div v-else class="ps-card text-center p-5">
      <div class="fs-1">👟</div>
      <h5 class="mt-2">Không tìm thấy sản phẩm nào</h5>
      <p class="text-muted-ps">
        Thử thay đổi từ khóa tìm kiếm hoặc đặt lại các bộ lọc.
      </p>
      <button class="btn btn-light-border px-4" @click="reset">Xóa bộ lọc</button>
    </div>
  </template>
</template>
