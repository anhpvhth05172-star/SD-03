<script setup>
import {
  ref,
  computed,
  watch,
  onMounted,
  onBeforeUnmount,
  nextTick,
} from "vue";
import { useRoute } from "vue-router";
import { getSanPhamClient } from "@/api/product";
import ProductCard from "./ProductCard.vue";

const route = useRoute();

// ===== Bộ lọc =====
const q = ref("");
const brand = ref(route.query.brand || "");
const type = ref("");
const sort = ref("");

// ===== Phân trang =====
const pageSizeOptions = [12, 24, 48];
const pageSize = ref(12);
const currentPage = ref(1);
const gridTop = ref(null);

// ===== Dữ liệu từ API =====
const allProducts = ref([]);
const loading = ref(false);
const error = ref("");

// Danh sách thương hiệu (kèm số lượng) / loại giày từ dữ liệu thực
const brands = computed(() => {
  const map = new Map();
  allProducts.value.forEach((p) => {
    if (!p.tenThuongHieu) return;
    map.set(p.tenThuongHieu, (map.get(p.tenThuongHieu) || 0) + 1);
  });
  return [...map.entries()].map(([name, count]) => ({ name, count }));
});

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
onBeforeUnmount(() => clearTimeout(searchTimer));

// Đồng bộ brand từ query string
watch(
  () => route.query.brand,
  (val) => {
    brand.value = val || "";
  },
);

// Đổi bộ lọc / số lượng mỗi trang thì quay về trang 1
watch([q, brand, type, sort, pageSize], () => {
  currentPage.value = 1;
});

// ===== Lọc/sắp xếp phía FE =====
const priceOf = (p) => Number(p.giaBanMin || p.giaBan || 0);

const list = computed(() => {
  let r = allProducts.value.filter(
    (p) =>
      (!brand.value || p.tenThuongHieu === brand.value) &&
      (!type.value || p.tenLoaiGiay === type.value),
  );
  if (sort.value === "asc") r = [...r].sort((a, b) => priceOf(a) - priceOf(b));
  if (sort.value === "desc") r = [...r].sort((a, b) => priceOf(b) - priceOf(a));
  if (sort.value === "name")
    r = [...r].sort((a, b) =>
      String(a.tenSanPham || "").localeCompare(
        String(b.tenSanPham || ""),
        "vi",
      ),
    );
  return r;
});

// ===== Tính toán phân trang =====
const totalPages = computed(() =>
  Math.max(1, Math.ceil(list.value.length / pageSize.value)),
);

// Nếu danh sách ngắn lại thì kéo trang hiện tại về trang cuối hợp lệ
watch(totalPages, (n) => {
  if (currentPage.value > n) currentPage.value = n;
});

const visibleList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return list.value.slice(start, start + pageSize.value);
});

const rangeFrom = computed(() =>
  list.value.length ? (currentPage.value - 1) * pageSize.value + 1 : 0,
);
const rangeTo = computed(() =>
  Math.min(currentPage.value * pageSize.value, list.value.length),
);

// Dãy số trang có dấu "…"  (vd: 1 … 4 5 6 … 12)
const pageItems = computed(() => {
  const total = totalPages.value;
  const cur = currentPage.value;
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);

  const items = [1];
  const start = Math.max(2, cur - 1);
  const end = Math.min(total - 1, cur + 1);

  if (start > 2) items.push("...");
  for (let i = start; i <= end; i++) items.push(i);
  if (end < total - 1) items.push("...");
  items.push(total);
  return items;
});

const hasFilter = computed(() => !!(q.value || brand.value || type.value));

// ===== Actions =====
const selectBrand = (b) => {
  brand.value = brand.value === b ? "" : b;
};

async function goToPage(p) {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return;
  currentPage.value = p;
  await nextTick();
  gridTop.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

const reset = () => {
  q.value = "";
  brand.value = "";
  type.value = "";
  sort.value = "";
};
</script>

<template>
  <div class="shop-container">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      <span class="mx-1">/</span>
      <span class="text-dark fw-semibold">Cửa hàng sneaker</span>
    </nav>

    <!-- ===== Hero banner ===== -->
    <section class="shop-hero mb-4">
      <div class="hero-blob hero-blob-1"></div>
      <div class="hero-blob hero-blob-2"></div>

      <div class="row align-items-center g-4 position-relative">
        <div class="col-lg-7">
          <span class="hero-tag">🔥 BỘ SƯU TẬP 2026</span>
          <h1 class="hero-title">
            Sneaker chính hãng,<br />phong cách của riêng bạn
          </h1>
          <p class="hero-sub">
            Khám phá những mẫu giày hot trend từ Nike, Adidas, Jordan, Puma… Đảm
            bảo 100% chính hãng, đổi size 7 ngày miễn phí.
          </p>
        </div>

        <div class="col-lg-5 d-none d-lg-block">
          <div class="hero-perks">
            <div class="hero-perk">
              <i class="bi bi-shield-fill-check"></i>
              <div>
                <b>Chính hãng 100%</b>
                <span>Bảo hành keo chỉ 12 tháng</span>
              </div>
            </div>
            <div class="hero-perk">
              <i class="bi bi-arrow-repeat"></i>
              <div>
                <b>Đổi size 7 ngày</b>
                <span>Miễn phí đổi tận nhà</span>
              </div>
            </div>
            <div class="hero-perk">
              <i class="bi bi-truck"></i>
              <div>
                <b>Giao hàng nhanh</b>
                <span>Nhận hàng trong 1-3 ngày</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== Brand pills ===== -->
    <div v-if="brands.length" class="brand-bar mb-3">
      <button
        type="button"
        class="brand-pill"
        :class="{ active: !brand }"
        @click="brand = ''"
      >
        Tất cả
        <span class="brand-count">{{ allProducts.length }}</span>
      </button>
      <button
        v-for="b in brands"
        :key="b.name"
        type="button"
        class="brand-pill"
        :class="{ active: brand === b.name }"
        @click="selectBrand(b.name)"
      >
        {{ b.name }}
        <span class="brand-count">{{ b.count }}</span>
      </button>
    </div>

    <!-- ===== Filter bar (sticky) ===== -->
    <div class="filter-bar mb-3">
      <div class="row g-2 align-items-center">
        <div class="col-12 col-md-5">
          <div class="search-box">
            <i class="bi bi-search"></i>
            <input
              v-model="q"
              type="text"
              placeholder="Tìm theo tên giày, mã sản phẩm, thương hiệu..."
            />
            <button
              v-if="q"
              type="button"
              class="search-clear"
              title="Xóa từ khóa"
              @click="q = ''"
            >
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
        </div>

        <div class="col-6 col-md-3">
          <div class="select-box">
            <i class="bi bi-grid"></i>
            <select v-model="type">
              <option value="">Tất cả loại giày</option>
              <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>
        </div>

        <div class="col-6 col-md-3">
          <div class="select-box">
            <i class="bi bi-arrow-down-up"></i>
            <select v-model="sort">
              <option value="">Mặc định (Mới nhất)</option>
              <option value="asc">Giá thấp → cao</option>
              <option value="desc">Giá cao → thấp</option>
              <option value="name">Tên A → Z</option>
            </select>
          </div>
        </div>

        <div class="col-12 col-md-1">
          <button
            type="button"
            class="reset-btn"
            title="Đặt lại bộ lọc"
            :disabled="!hasFilter && !sort"
            @click="reset"
          >
            <i class="bi bi-arrow-counterclockwise"></i>
            <span class="d-md-none ms-1">Đặt lại</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ===== Active filter chips ===== -->
    <div v-if="hasFilter" class="chip-row mb-3">
      <span class="chip-label">Đang lọc:</span>
      <span v-if="q" class="filter-chip">
        Từ khóa: "{{ q }}"
        <button type="button" @click="q = ''"><i class="bi bi-x"></i></button>
      </span>
      <span v-if="brand" class="filter-chip">
        Hãng: {{ brand }}
        <button type="button" @click="brand = ''">
          <i class="bi bi-x"></i>
        </button>
      </span>
      <span v-if="type" class="filter-chip">
        Loại: {{ type }}
        <button type="button" @click="type = ''">
          <i class="bi bi-x"></i>
        </button>
      </span>
      <button type="button" class="chip-clear" @click="reset">
        Xóa tất cả
      </button>
    </div>

    <!-- Điểm neo để cuộn khi đổi trang -->
    <div ref="gridTop" class="grid-anchor"></div>

    <!-- ===== Loading skeleton ===== -->
    <div v-if="loading" class="row g-3 g-md-4">
      <div v-for="n in 8" :key="n" class="col-6 col-md-4 col-lg-3">
        <div class="skeleton-card">
          <div class="sk sk-img"></div>
          <div class="sk sk-line w-50"></div>
          <div class="sk sk-line w-75"></div>
          <div class="sk sk-line w-40"></div>
        </div>
      </div>
    </div>

    <!-- ===== Error ===== -->
    <div v-else-if="error" class="state-card">
      <div class="state-icon">⚠️</div>
      <h5 class="fw-bold">Lỗi kết nối máy chủ</h5>
      <p class="text-muted-ps mb-3">{{ error }}</p>
      <button class="btn btn-primary rounded-pill px-4" @click="fetchProducts">
        <i class="bi bi-arrow-clockwise me-1"></i> Thử lại
      </button>
    </div>

    <!-- ===== Product grid ===== -->
    <template v-else>
      <div class="result-bar mb-3">
        <p class="mb-0 small text-muted-ps">
          <template v-if="list.length">
            Hiển thị <strong>{{ rangeFrom }}–{{ rangeTo }}</strong> /
            <strong>{{ list.length }}</strong> sản phẩm
            <template v-if="list.length !== allProducts.length">
              (trong tổng số {{ allProducts.length }})
            </template>
          </template>
          <template v-else>Không có sản phẩm nào</template>
        </p>

        <div v-if="list.length > pageSizeOptions[0]" class="page-size">
          <label for="pageSize">Hiển thị</label>
          <select id="pageSize" v-model.number="pageSize">
            <option v-for="n in pageSizeOptions" :key="n" :value="n">
              {{ n }} / trang
            </option>
          </select>
        </div>
      </div>

      <div v-if="list.length" class="row g-3 g-md-4">
        <div
          v-for="p in visibleList"
          :key="`${currentPage}-${p.id}`"
          class="col-6 col-md-4 col-lg-3 grid-item"
        >
          <ProductCard :product="p" />
        </div>
      </div>

      <!-- ===== Pagination ===== -->
      <nav
        v-if="totalPages > 1"
        class="pagination-wrap"
        aria-label="Phân trang sản phẩm"
      >
        <button
          type="button"
          class="page-btn page-nav"
          :disabled="currentPage === 1"
          title="Trang trước"
          @click="goToPage(currentPage - 1)"
        >
          <i class="bi bi-chevron-left"></i>
          <span class="d-none d-sm-inline">Trước</span>
        </button>

        <template v-for="(it, i) in pageItems" :key="`${it}-${i}`">
          <span v-if="it === '...'" class="page-dots">…</span>
          <button
            v-else
            type="button"
            class="page-btn"
            :class="{ active: it === currentPage }"
            :aria-current="it === currentPage ? 'page' : undefined"
            @click="goToPage(it)"
          >
            {{ it }}
          </button>
        </template>

        <button
          type="button"
          class="page-btn page-nav"
          :disabled="currentPage === totalPages"
          title="Trang sau"
          @click="goToPage(currentPage + 1)"
        >
          <span class="d-none d-sm-inline">Sau</span>
          <i class="bi bi-chevron-right"></i>
        </button>
      </nav>

      <p
        v-if="totalPages > 1"
        class="text-center small text-muted-ps mt-2 mb-0"
      >
        Trang {{ currentPage }} / {{ totalPages }}
      </p>

      <!-- Empty -->
      <div v-if="!list.length" class="state-card">
        <div class="state-icon">👟</div>
        <h4 class="fw-bold">Không tìm thấy sản phẩm phù hợp</h4>
        <p class="text-muted-ps mb-4 mx-auto" style="max-width: 420px">
          Không có đôi sneaker nào khớp với bộ lọc hiện tại. Thử từ khóa khác
          hoặc đặt lại bộ lọc nhé!
        </p>
        <button class="btn btn-primary rounded-pill px-4" @click="reset">
          <i class="bi bi-arrow-counterclockwise me-1"></i> Xóa tất cả bộ lọc
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* ===== Hero ===== */
.shop-hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 40px 36px;
  background: linear-gradient(135deg, #fff1f2 0%, #ffe4e6 45%, #fdf2f8 100%);
  border: 1px solid #fecdd3;
  box-shadow: 0 8px 32px rgba(220, 38, 38, 0.08);
}

.hero-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  opacity: 0.55;
  pointer-events: none;
}
.hero-blob-1 {
  width: 240px;
  height: 240px;
  background: #fda4af;
  top: -80px;
  right: -40px;
}
.hero-blob-2 {
  width: 200px;
  height: 200px;
  background: #fbcfe8;
  bottom: -90px;
  left: 30%;
}

.hero-tag {
  display: inline-block;
  background: #ffffff;
  color: #dc2626;
  font-weight: 700;
  font-size: 0.78rem;
  padding: 6px 14px;
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.12);
  margin-bottom: 14px;
}

.hero-title {
  font-size: 2.2rem;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: #111827;
  margin-bottom: 12px;
}

.hero-sub {
  color: #64748b;
  max-width: 540px;
  margin-bottom: 0;
}

.hero-perks {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-perk {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(6px);
  border: 1px solid #ffffff;
  border-radius: 16px;
  padding: 12px 16px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
}
.hero-perk i {
  font-size: 1.6rem;
  color: #dc2626;
}
.hero-perk b {
  display: block;
  font-size: 0.95rem;
  color: #111827;
}
.hero-perk span {
  font-size: 0.78rem;
  color: #64748b;
}

/* ===== Brand pills ===== */
.brand-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.brand-pill {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  color: #475569;
  font-weight: 600;
  font-size: 0.88rem;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.brand-pill:hover {
  border-color: #fca5a5;
  color: #dc2626;
  transform: translateY(-1px);
}
.brand-pill.active {
  background: #dc2626;
  border-color: #dc2626;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}

.brand-count {
  font-size: 0.72rem;
  font-weight: 700;
  background: #f1f5f9;
  color: #64748b;
  padding: 1px 8px;
  border-radius: 999px;
}
.brand-pill.active .brand-count {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

/* ===== Filter bar ===== */
.filter-bar {
  position: sticky;
  top: 70px;
  z-index: 20;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
}

.search-box,
.select-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-box > i,
.select-box > i {
  position: absolute;
  left: 14px;
  color: #94a3b8;
  pointer-events: none;
}

.search-box input,
.select-box select {
  width: 100%;
  height: 44px;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 12px;
  padding: 0 14px 0 40px;
  font-size: 0.9rem;
  color: #1e293b;
  outline: none;
  transition: all 0.2s ease;
}

.search-box input {
  padding-right: 38px;
}

.search-box input:focus,
.select-box select:focus {
  background: #ffffff;
  border-color: #f87171;
  box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.2);
}

.search-clear {
  position: absolute;
  right: 8px;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 0.65rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.search-clear:hover {
  background: #cbd5e1;
}

.reset-btn {
  width: 100%;
  height: 44px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  border-radius: 12px;
  color: #475569;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.2s ease;
}
.reset-btn:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
}
.reset-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ===== Active chips ===== */
.chip-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
}
.chip-label {
  color: #64748b;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fee2e2;
  color: #b91c1c;
  font-weight: 600;
  padding: 5px 6px 5px 12px;
  border-radius: 999px;
}
.filter-chip button {
  border: none;
  background: rgba(185, 28, 28, 0.12);
  color: #b91c1c;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
}
.filter-chip button:hover {
  background: rgba(185, 28, 28, 0.25);
}

.chip-clear {
  border: none;
  background: none;
  color: #64748b;
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
}
.chip-clear:hover {
  color: #dc2626;
}

/* ===== Anchor + result bar ===== */
/* chừa chỗ cho navbar + filter-bar sticky khi cuộn tới */
.grid-anchor {
  scroll-margin-top: 150px;
  height: 0;
}

.result-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.page-size {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: #64748b;
}
.page-size select {
  height: 34px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  border-radius: 10px;
  padding: 0 10px;
  font-size: 0.85rem;
  color: #1e293b;
  outline: none;
  cursor: pointer;
}
.page-size select:focus {
  border-color: #f87171;
  box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.2);
}

/* ===== Grid ===== */
.grid-item {
  animation: itemIn 0.35s ease both;
}
@keyframes itemIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 32px;
}

.page-btn {
  min-width: 42px;
  height: 42px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  color: #475569;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled):not(.active) {
  border-color: #fca5a5;
  color: #dc2626;
  background: #fff5f5;
  transform: translateY(-1px);
}

.page-btn.active {
  background: #dc2626;
  border-color: #dc2626;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
  cursor: default;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-nav {
  padding: 0 16px;
}

.page-dots {
  min-width: 24px;
  text-align: center;
  color: #94a3b8;
  font-weight: 700;
  user-select: none;
}

/* ===== Skeleton ===== */
.skeleton-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 18px;
  padding: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}
.sk {
  border-radius: 8px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 37%, #f1f5f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}
.sk-img {
  height: 170px;
  border-radius: 14px;
  margin-bottom: 14px;
}
.sk-line {
  height: 12px;
  margin-bottom: 10px;
}
.w-40 {
  width: 40%;
}
@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ===== State card ===== */
.state-card {
  text-align: center;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 20px;
  padding: 56px 24px;
}
.state-icon {
  font-size: 3.2rem;
  margin-bottom: 10px;
}

@media (max-width: 767px) {
  .shop-hero {
    padding: 26px 20px;
    border-radius: 20px;
  }
  .hero-title {
    font-size: 1.6rem;
  }
  .filter-bar {
    top: 56px;
  }
  .grid-anchor {
    scroll-margin-top: 130px;
  }
  .page-btn {
    min-width: 38px;
    height: 38px;
    padding: 0 10px;
  }
}
</style>
