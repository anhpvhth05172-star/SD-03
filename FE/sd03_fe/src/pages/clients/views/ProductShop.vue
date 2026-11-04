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
import SearchSelect from "../SearchSelect.vue";

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

// ===== Banner: giày nổi bật (lấy từ dữ liệu thật) =====
const featured = computed(() =>
  allProducts.value.filter((p) => p.img).slice(0, 4),
);
const featuredIndex = ref(0);
const currentFeatured = computed(
  () => featured.value[featuredIndex.value] || null,
);

const formatVnd = (n) => Number(n || 0).toLocaleString("vi-VN") + "₫";

let bannerTimer = null;
function startBannerTimer() {
  clearInterval(bannerTimer);
  bannerTimer = setInterval(() => {
    if (featured.value.length > 1) {
      featuredIndex.value = (featuredIndex.value + 1) % featured.value.length;
    }
  }, 3500);
}

function pickFeatured(i) {
  featuredIndex.value = i;
  startBannerTimer();
}

// Nếu danh sách nổi bật thay đổi (vd: sau khi tìm kiếm) thì về ảnh đầu
watch(featured, () => {
  featuredIndex.value = 0;
});

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

onMounted(() => {
  fetchProducts();
  startBannerTimer();
});

// Debounce khi gõ tìm kiếm
let searchTimer = null;
watch(q, () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(fetchProducts, 400);
});
onBeforeUnmount(() => {
  clearTimeout(searchTimer);
  clearInterval(bannerTimer);
});

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

// Cuộn xuống lưới sản phẩm (nút trên banner)
function scrollToGrid() {
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

    <!-- ===== Banner giày mới về ===== -->
    <section class="shop-banner mb-4">
      <div class="banner-glow banner-glow-1"></div>
      <div class="banner-glow banner-glow-2"></div>

      <div class="row align-items-center g-4 position-relative">
        <div class="col-lg-6">
          <span class="banner-tag">
            <span class="banner-tag-dot"></span> NEW ARRIVALS
          </span>
          <h1 class="banner-title">
            Sneaker mới về,<br />
            <span class="banner-title-accent">chạm đất là mê</span>
          </h1>
          <p class="banner-sub">
            Cập nhật liên tục các mẫu giày hot trend từ Nike, Adidas, Jordan,
            Puma… Nhập mã <b class="banner-code">POLY2026</b> giảm ngay 10%.
          </p>
          <div class="d-flex flex-wrap align-items-center gap-3">
            <button type="button" class="banner-btn" @click="scrollToGrid">
              Mua ngay <i class="bi bi-arrow-right ms-1"></i>
            </button>
            <span class="banner-note">
              <i class="bi bi-truck me-1"></i> Miễn phí ship từ 500.000đ
            </span>
          </div>
        </div>

        <!-- Sân khấu giày -->
        <div class="col-lg-6 d-none d-lg-block">
          <div class="shoe-stage">
            <div class="shoe-ring"></div>
            <div class="shoe-circle"></div>

            <template v-if="currentFeatured">
              <img
                :key="currentFeatured.id"
                :src="currentFeatured.img"
                :alt="currentFeatured.tenSanPham"
                class="shoe-img"
              />

              <div class="shoe-info">
                <div class="shoe-info-brand">
                  {{ currentFeatured.tenThuongHieu || "Sneaker" }}
                </div>
                <div class="shoe-info-name">
                  {{ currentFeatured.tenSanPham }}
                </div>
                <div class="shoe-info-price">
                  Từ {{ formatVnd(priceOf(currentFeatured)) }}
                </div>
              </div>

              <div v-if="featured.length > 1" class="shoe-dots">
                <button
                  v-for="(f, i) in featured"
                  :key="f.id"
                  type="button"
                  class="shoe-dot"
                  :class="{ active: i === featuredIndex }"
                  :title="f.tenSanPham"
                  @click="pickFeatured(i)"
                ></button>
              </div>
            </template>
            <div v-else class="shoe-placeholder">👟</div>
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

        <!-- Loại giày: select có ô tìm kiếm -->
        <div class="col-6 col-md-3">
          <SearchSelect
            v-model="type"
            :options="types"
            icon="bi-grid"
            all-label="Tất cả loại giày"
            placeholder="Tìm loại giày..."
          />
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

      <!-- ===== Pagination (góc phải, nhỏ gọn) ===== -->
      <div v-if="totalPages > 1" class="pagination-row">
        <span class="page-info"
          >Trang {{ currentPage }} / {{ totalPages }}</span
        >

        <nav class="pagination-wrap" aria-label="Phân trang sản phẩm">
          <button
            type="button"
            class="page-btn"
            :disabled="currentPage === 1"
            title="Trang trước"
            @click="goToPage(currentPage - 1)"
          >
            <i class="bi bi-chevron-left"></i>
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
            class="page-btn"
            :disabled="currentPage === totalPages"
            title="Trang sau"
            @click="goToPage(currentPage + 1)"
          >
            <i class="bi bi-chevron-right"></i>
          </button>
        </nav>
      </div>

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
/* ===== Banner giày mới về ===== */
.shop-banner {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 40px 44px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 60%, #111827 100%);
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.3);
  color: #ffffff;
}

.banner-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
}
.banner-glow-1 {
  width: 340px;
  height: 340px;
  background: rgba(239, 68, 68, 0.45);
  top: -120px;
  right: 8%;
}
.banner-glow-2 {
  width: 240px;
  height: 240px;
  background: rgba(59, 130, 246, 0.25);
  bottom: -110px;
  left: -40px;
}

.banner-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-weight: 700;
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 18px;
}
.banner-tag-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.25);
  animation: pulseDot 1.6s ease-in-out infinite;
}
@keyframes pulseDot {
  0%,
  100% {
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.25);
  }
  50% {
    box-shadow: 0 0 0 7px rgba(239, 68, 68, 0.08);
  }
}

.banner-title {
  font-size: 2.6rem;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: #ffffff;
  margin-bottom: 14px;
}
.banner-title-accent {
  background: linear-gradient(90deg, #f87171, #fbbf24);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.banner-sub {
  color: rgba(255, 255, 255, 0.75);
  max-width: 480px;
  margin-bottom: 24px;
}
.banner-code {
  background: #fbbf24;
  color: #111827;
  padding: 1px 8px;
  border-radius: 6px;
  letter-spacing: 0.03em;
}

.banner-btn {
  border: none;
  background: #ef4444;
  color: #ffffff;
  font-weight: 700;
  font-size: 0.95rem;
  padding: 12px 30px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(239, 68, 68, 0.4);
  transition: all 0.2s ease;
}
.banner-btn:hover {
  background: #dc2626;
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(239, 68, 68, 0.5);
}
.banner-note {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.7);
}

/* ----- Sân khấu giày ----- */
.shoe-stage {
  position: relative;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shoe-circle {
  position: absolute;
  width: 270px;
  height: 270px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #f87171, #b91c1c 70%);
  box-shadow: 0 20px 50px rgba(185, 28, 28, 0.45);
}
.shoe-ring {
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  border: 2px dashed rgba(255, 255, 255, 0.2);
  animation: spinRing 28s linear infinite;
}
@keyframes spinRing {
  to {
    transform: rotate(360deg);
  }
}

.shoe-img {
  position: relative;
  z-index: 2;
  max-width: 78%;
  max-height: 230px;
  object-fit: contain;
  transform: rotate(-14deg);
  filter: drop-shadow(0 24px 22px rgba(0, 0, 0, 0.45));
  animation: shoeIn 0.6s ease both;
}
@keyframes shoeIn {
  from {
    opacity: 0;
    transform: translateX(40px) rotate(-4deg) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateX(0) rotate(-14deg) scale(1);
  }
}

.shoe-placeholder {
  position: relative;
  z-index: 2;
  font-size: 7rem;
}

.shoe-info {
  position: absolute;
  z-index: 3;
  left: 0;
  bottom: 6px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  padding: 10px 14px;
  max-width: 220px;
}
.shoe-info-brand {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #fca5a5;
}
.shoe-info-name {
  font-size: 0.9rem;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.shoe-info-price {
  font-size: 0.82rem;
  color: #fde68a;
  font-weight: 600;
  margin-top: 2px;
}

.shoe-dots {
  position: absolute;
  z-index: 3;
  right: 4px;
  bottom: 14px;
  display: flex;
  gap: 8px;
}
.shoe-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  border: none;
  padding: 0;
  background: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: all 0.25s ease;
}
.shoe-dot.active {
  width: 26px;
  background: #ef4444;
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

/* ===== Pagination (góc phải, nhỏ gọn) ===== */
.pagination-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
}

.page-info {
  font-size: 0.78rem;
  color: #64748b;
}

.pagination-wrap {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  color: #475569;
  font-weight: 600;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled):not(.active) {
  border-color: #fca5a5;
  color: #dc2626;
  background: #fff5f5;
}

.page-btn.active {
  background: #dc2626;
  border-color: #dc2626;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.3);
  cursor: default;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-dots {
  min-width: 18px;
  text-align: center;
  color: #94a3b8;
  font-weight: 700;
  font-size: 0.8rem;
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
  .shop-banner {
    padding: 28px 20px;
    border-radius: 20px;
  }
  .banner-title {
    font-size: 1.8rem;
  }
  .filter-bar {
    top: 56px;
  }
  .grid-anchor {
    scroll-margin-top: 130px;
  }
}
</style>
