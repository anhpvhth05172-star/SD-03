<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getSanPhamById, getBienTheBySanPham } from "@/api/product";
import { money, addToCart } from "../store";

const route = useRoute();
const router = useRouter();

// ===== State =====
const product = ref(null);       // SanPhamResponse
const variants = ref([]);        // SanPhamChiTietResponse[]
const loading = ref(false);
const error = ref("");

// Lựa chọn người dùng
const selectedColor = ref(null);  // idMauSac
const selectedVariant = ref(null); // SanPhamChiTietResponse được chọn
const qty = ref(1);
const addError = ref("");
const added = ref(false);
const adding = ref(false);
const addSuccessMessage = ref("");

// ===== Fetch =====
async function fetchData() {
  const id = route.params.id;
  if (!id) return;

  loading.value = true;
  error.value = "";
  product.value = null;
  variants.value = [];
  selectedColor.value = null;
  selectedVariant.value = null;

  try {
    const [spRes, varRes] = await Promise.all([
      getSanPhamById(id),
      getBienTheBySanPham(id),
    ]);
    product.value = spRes;

    // varRes có thể là PageResponse { content: [] } hoặc mảng trực tiếp
    variants.value = varRes?.content ?? varRes ?? [];

    // Không tự động chọn màu sẵn để khách hàng tự chọn
    selectedColor.value = null;
    selectedVariant.value = null;
  } catch (e) {
    error.value = e.message || "Không thể tải thông tin sản phẩm.";
  } finally {
    loading.value = false;
  }
}

onMounted(fetchData);
watch(() => route.params.id, fetchData);

// ===== Computed =====

// Danh sách màu sắc duy nhất (từ biến thể còn hàng)
const colors = computed(() => {
  const seen = new Set();
  return variants.value.filter((v) => {
    if (seen.has(v.idMauSac)) return false;
    seen.add(v.idMauSac);
    return true;
  });
});

// Size của màu đang chọn
const sizesOfColor = computed(() =>
  variants.value.filter((v) => v.idMauSac === selectedColor.value)
);

// Danh sách ảnh của sản phẩm hoặc biến thể
const allImages = computed(() => {
  const list = [];
  if (product.value?.img) list.push(product.value.img);
  variants.value.forEach((v) => {
    if (v.img && !list.includes(v.img)) list.push(v.img);
  });
  return list;
});

const currentImageIndex = ref(0);

// Ảnh của màu đang chọn hoặc carousel
const displayImg = computed(() => {
  if (allImages.value.length > 0 && currentImageIndex.value < allImages.value.length) {
    return allImages.value[currentImageIndex.value];
  }
  const first = variants.value.find((v) => v.idMauSac === selectedColor.value);
  return first?.img || product.value?.img || null;
});

function prevImage() {
  if (allImages.value.length <= 1) return;
  currentImageIndex.value = (currentImageIndex.value - 1 + allImages.value.length) % allImages.value.length;
}

function nextImage() {
  if (allImages.value.length <= 1) return;
  currentImageIndex.value = (currentImageIndex.value + 1) % allImages.value.length;
}

// Giá hiển thị (lấy giaSauGiam nếu có, fallback về price gốc)
const displayPrice = computed(() => {
  if (selectedVariant.value) return Number(selectedVariant.value.giaSauGiam || selectedVariant.value.price || 0);
  return Number(product.value?.giaBanMin || product.value?.giaBan || 0);
});

// Giá gốc (hiển thị gạch ngang nếu có giảm)
const originalPrice = computed(() => {
  if (selectedVariant.value) return Number(selectedVariant.value.price || 0);
  return Number(product.value?.giaBanMax || product.value?.giaBanMin || product.value?.giaBan || 0);
});

// % giảm của biến thể đang chọn hoặc toàn bộ sản phẩm
const phanTramGiam = computed(() => {
  if (selectedVariant.value) {
    const pt = selectedVariant.value.phanTramGiam || selectedVariant.value.phanTramGiamKhac;
    if (pt != null && Number(pt) > 0) return Math.round(Number(pt));
    const orig = Number(selectedVariant.value.price || 0);
    const curr = Number(selectedVariant.value.giaSauGiam || orig);
    if (orig > curr && orig > 0) {
      return Math.round(((orig - curr) / orig) * 100);
    }
  }
  // Nếu chưa chọn biến thể cụ thể, tính dựa trên giá max và min
  const orig = Number(product.value?.giaBanMax || product.value?.giaGoc || 0);
  const curr = Number(product.value?.giaBanMin || product.value?.giaBan || 0);
  if (orig > curr && orig > 0) {
    return Math.round(((orig - curr) / orig) * 100);
  }
  return 0;
});

// Khi đổi màu → reset variant
watch(selectedColor, () => {
  selectedVariant.value = null;
  addError.value = "";
});

// ===== Actions =====
function selectVariant(v) {
  selectedVariant.value = v;
  addError.value = "";
}

async function add(goCart = false) {
  if (colors.value.length > 0 && !selectedColor.value) {
    addError.value = "Vui lòng chọn màu sắc trước.";
    return;
  }
  if (!selectedVariant.value) {
    addError.value = "Vui lòng chọn size trước khi thêm vào giỏ.";
    return;
  }
  if (selectedVariant.value.stock <= 0) {
    addError.value = "Sản phẩm này đã hết hàng.";
    return;
  }
  addError.value = "";
  adding.value = true;

  try {
    const res = await addToCart(
      {
        id: product.value.id,
        tenSanPham: product.value.tenSanPham,
        tenThuongHieu: product.value.tenThuongHieu,
        giaBan: displayPrice.value,
        giaBanMin: displayPrice.value,
        img: selectedVariant.value.img || displayImg.value || product.value?.img || null,
      },
      selectedVariant.value.id,   // variantId (idSanPhamChiTiet)
      selectedVariant.value.size, // tên size
      qty.value,
      selectedVariant.value.color // tên màu
    );

    if (res?.needLogin) {
      addError.value = res.message || "Vui lòng đăng nhập để thêm vào giỏ hàng.";
      setTimeout(() => {
        router.push({ path: "/dang-nhap", query: { redirect: route.fullPath } });
      }, 1500);
      return;
    }

    if (goCart) {
      router.push("/gio-hang");
    } else {
      added.value = true;
      addSuccessMessage.value = res?.message || "Đã thêm sản phẩm vào giỏ hàng";
      setTimeout(() => {
        added.value = false;
        addSuccessMessage.value = "";
      }, 2500);
    }
  } catch (err) {
    addError.value = err.message || "Có lỗi xảy ra khi thêm vào giỏ hàng.";
  } finally {
    adding.value = false;
  }
}
</script>

<template>
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
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2">Quay lại danh sách</RouterLink>
  </div>

  <!-- Nội dung sản phẩm -->
  <div v-else-if="product">
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <RouterLink to="/cua-hang" class="text-muted-ps text-decoration-none">Sản phẩm</RouterLink>
      /
      <span class="text-dark fw-semibold">{{ product.tenSanPham }}</span>
    </nav>

    <div class="ps-card p-3 p-md-4 rounded-4 shadow-sm border-0 bg-white">
      <div class="row g-4 align-items-center">
        <!-- Ảnh sản phẩm: Tràn viền khung nền hồng pastel mềm như ảnh mẫu -->
        <div class="col-md-6 col-lg-5">
          <div class="product-gallery-bleed-box position-relative overflow-hidden rounded-4 d-flex align-items-center justify-content-center">
            <img
              v-if="displayImg"
              :src="displayImg"
              :alt="product.tenSanPham"
              class="product-main-img img-fluid"
              @error="(e) => { e.target.style.display='none'; e.target.nextSibling.style.display='flex'; }"
            />
            <div
              :style="displayImg ? 'display:none; align-items:center; justify-content:center; width:100%; height:100%; font-size: 7rem;' : 'display:flex; align-items:center; justify-content:center; width:100%; height:100%; font-size: 7rem;'"
            >👟</div>

            <!-- Nút mũi tên lướt ảnh tròn tinh tế -->
            <button 
              v-if="allImages.length > 1"
              type="button" 
              class="carousel-nav-btn btn-nav-left shadow-sm"
              @click.stop="prevImage"
              title="Ảnh trước"
            >
              <i class="bi bi-chevron-left"></i>
            </button>
            <button 
              v-if="allImages.length > 1"
              type="button" 
              class="carousel-nav-btn btn-nav-right shadow-sm"
              @click.stop="nextImage"
              title="Ảnh tiếp"
            >
              <i class="bi bi-chevron-right"></i>
            </button>
          </div>

          <!-- Thumbnail màu sắc -->
          <div v-if="colors.length > 0" class="d-flex flex-wrap gap-2 mt-3 justify-content-center">
            <div
              v-for="c in colors"
              :key="c.idMauSac"
              class="color-swatch"
              :class="{ active: selectedColor === c.idMauSac }"
              :style="c.colorHex ? `background-color: ${c.colorHex}` : ''"
              :title="c.color"
              @click="selectedColor = c.idMauSac"
            >
              <img
                v-if="c.img"
                :src="c.img"
                :alt="c.color"
                class="color-thumb"
              />
              <span v-else class="color-name">{{ c.color }}</span>
            </div>
          </div>
        </div>

        <!-- Thông tin sản phẩm -->
        <div class="col-md-6 col-lg-7 ps-lg-4">
          <!-- Tag danh mục / thương hiệu nhỏ trên cùng như ảnh: Adidas · Chạy bộ -->
          <div class="mb-2">
            <span class="badge-brand-category">
              {{ product.tenThuongHieu || 'Chính hãng' }}<template v-if="product.tenLoaiGiay"> · {{ product.tenLoaiGiay }}</template>
            </span>
          </div>

          <!-- Tên sản phẩm to tròn sạch sẽ -->
          <h1 class="fw-bold text-dark mb-1 product-detail-title">{{ product.tenSanPham }}</h1>
          
          <!-- Mã sản phẩm: SP002 -->
          <div class="text-secondary small mb-3">
            Mã sản phẩm: <span class="fw-medium text-dark">{{ selectedVariant?.maSanPhamChiTiet || selectedVariant?.barcode || product.maSanPham || 'SP001' }}</span>
          </div>

          <!-- Giá sản phẩm: 4.100.000đ  4.500.000đ (đúng ảnh mẫu) -->
          <div class="d-flex align-items-baseline gap-3 mb-4">
            <span class="price-highlight-red">{{ money(displayPrice) }}</span>
            <span
              v-if="phanTramGiam > 0 || (originalPrice > displayPrice)"
              class="text-muted text-decoration-line-through fs-6"
            >
              {{ money(originalPrice) }}
            </span>
            <span v-if="phanTramGiam > 0" class="badge-discount-pink">
              -{{ phanTramGiam }}%
            </span>
          </div>

          <!-- Chọn màu sắc -->
          <div v-if="colors.length > 0" class="mb-3">
            <label class="d-block text-secondary small fw-semibold mb-2">
              Màu sắc: 
              <span v-if="selectedColor" class="text-dark fw-bold">
                {{ colors.find(c => c.idMauSac === selectedColor)?.color }}
              </span>
              <span v-else class="text-danger fw-normal">
                (Chưa chọn)
              </span>
            </label>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="c in colors"
                :key="c.idMauSac"
                type="button"
                class="btn size-box-btn px-3"
                :class="selectedColor === c.idMauSac ? 'size-btn-active' : 'size-btn-normal'"
                @click="selectedColor = c.idMauSac"
              >
                <span
                  v-if="c.colorHex"
                  class="d-inline-block rounded-circle me-1 border"
                  :style="{ backgroundColor: c.colorHex, width: '12px', height: '12px' }"
                ></span>
                {{ c.color }}
              </button>
            </div>
          </div>

          <!-- Chọn size: các ô vuông 38, 39, 40, 41, 42 -->
          <div class="mb-3">
            <label class="d-block text-secondary small fw-semibold mb-2">
              Chọn size
              <span v-if="selectedVariant" class="text-dark fw-bold ms-1">: {{ selectedVariant.size }}</span>
            </label>
            <div class="d-flex flex-wrap gap-2">
              <template v-if="selectedColor">
                <template v-for="v in sizesOfColor" :key="v.id">
                  <button
                    type="button"
                    class="btn size-box-btn"
                    :class="[
                      selectedVariant?.id === v.id ? 'size-btn-active' : 'size-btn-normal',
                      { 'disabled opacity-50': v.stock <= 0 }
                    ]"
                    :disabled="v.stock <= 0"
                    :title="v.stock <= 0 ? 'Hết hàng' : `Còn ${v.stock} sản phẩm`"
                    @click="selectVariant(v)"
                  >
                    {{ v.size }}
                  </button>
                </template>
                <div v-if="!sizesOfColor.length" class="text-muted small">
                  Màu này hiện tại đã hết size.
                </div>
              </template>
              <div v-else class="text-muted small fst-italic">
                👉 Vui lòng chọn màu sắc ở trên để xem các kích cỡ có sẵn.
              </div>
            </div>
          </div>

          <!-- Số lượng: - 1 + -->
          <div class="mb-4">
            <label class="d-block text-secondary small fw-semibold mb-2">Số lượng</label>
            <div class="d-inline-flex align-items-center gap-3">
              <div class="qty-pill-box d-inline-flex align-items-center rounded border bg-white">
                <button
                  type="button"
                  class="qty-btn"
                  @click="qty = Math.max(1, qty - 1)"
                  :disabled="adding"
                >−</button>
                <input class="qty-input text-center border-0 fw-semibold" :value="qty" readonly />
                <button
                  type="button"
                  class="qty-btn"
                  @click="qty++"
                  :disabled="adding"
                >+</button>
              </div>
              <span v-if="selectedVariant" class="text-muted small">
                (Còn {{ selectedVariant.stock }} sp)
              </span>
            </div>
          </div>

          <!-- Thông báo lỗi hoặc thành công -->
          <div v-if="addError" class="alert alert-danger py-2 px-3 small mb-3 rounded-3">{{ addError }}</div>
          <div v-if="added" class="alert alert-success py-2 px-3 small mb-3 rounded-3">
            ✓ {{ addSuccessMessage || "Đã thêm sản phẩm vào giỏ hàng thành công!" }}
          </div>

          <!-- 2 BUTTON NHỎ GỌN NHƯ ẢNH: "Thêm vào giỏ" và "Mua ngay" -->
          <div class="d-flex align-items-center gap-2 mb-4">
            <button 
              class="btn btn-outline-primary btn-action-compact btn-cart-compact"
              @click="add(false)" 
              :disabled="adding"
            >
              <span v-if="adding" class="spinner-border spinner-border-sm me-1" role="status"></span>
              <span>Thêm vào giỏ</span>
            </button>

            <button 
              class="btn btn-primary btn-action-compact btn-buy-compact"
              @click="add(true)" 
              :disabled="adding"
            >
              <span>Mua ngay</span>
            </button>
          </div>

          <!-- Sneaker Perks Block -->
          <div class="product-perks-box p-3 rounded-4 mb-4 bg-light border">
            <div class="row g-2 text-center text-md-start">
              <div class="col-4 d-flex align-items-center gap-2">
                <i class="bi bi-shield-check text-danger fs-4"></i>
                <div class="small lh-sm">
                  <div class="fw-bold">100% Chính hãng</div>
                  <span class="text-muted" style="font-size: 0.75rem;">Kiểm tra hàng</span>
                </div>
              </div>
              <div class="col-4 d-flex align-items-center gap-2 border-start border-end px-2">
                <i class="bi bi-arrow-repeat text-danger fs-4"></i>
                <div class="small lh-sm">
                  <div class="fw-bold">Đổi size 7 ngày</div>
                  <span class="text-muted" style="font-size: 0.75rem;">Miễn phí đổi</span>
                </div>
              </div>
              <div class="col-4 d-flex align-items-center gap-2 ps-2">
                <i class="bi bi-truck text-danger fs-4"></i>
                <div class="small lh-sm">
                  <div class="fw-bold">Giao hàng nhanh</div>
                  <span class="text-muted" style="font-size: 0.75rem;">1-3 ngày nhận</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Mô tả -->
          <div v-if="product.moTa" class="mt-4 pt-3 border-top">
            <div class="fw-bold fs-5 mb-2">Đặc điểm nổi bật</div>
            <p class="text-muted-ps lh-lg mb-0" style="white-space: pre-line;">{{ product.moTa }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Không tìm thấy -->
  <div v-else class="ps-card text-center p-5">
    <h5>Không tìm thấy sản phẩm</h5>
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2">Quay lại danh sách</RouterLink>
  </div>
</template>

<style scoped>
/* Khung ảnh tràn viền nền pastel hồng nhạt mềm mại như ảnh mẫu */
.product-gallery-bleed-box {
  background: #fbf0f0;
  border-radius: 20px;
  min-height: 380px;
  height: 420px;
  padding: 24px;
}

.product-main-img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  filter: drop-shadow(0 15px 25px rgba(0, 0, 0, 0.08));
  transition: transform 0.3s ease;
}

.product-gallery-bleed-box:hover .product-main-img {
  transform: scale(1.05) rotate(-2deg);
}

.carousel-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  cursor: pointer;
  z-index: 5;
  transition: all 0.2s ease;
}

.carousel-nav-btn:hover {
  background: #ffffff;
  color: #0f172a;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.btn-nav-left {
  left: 14px;
}

.btn-nav-right {
  right: 14px;
}

/* Tag danh mục nhỏ: Adidas · Chạy bộ */
.badge-brand-category {
  display: inline-block;
  background-color: #fdf2f2;
  color: #ef4444;
  font-size: 0.78rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
}

/* Tiêu đề sản phẩm */
.product-detail-title {
  font-size: 1.85rem;
  letter-spacing: -0.01em;
  color: #111827;
}

/* Giá sản phẩm to đậm màu đỏ nổi bật */
.price-highlight-red {
  font-size: 1.85rem;
  color: #dc2626;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.badge-discount-pink {
  background-color: #fee2e2;
  color: #dc2626;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

/* Các ô chọn size vuông bo góc nhỏ 38, 39, 40, 41, 42 */
.size-box-btn {
  min-width: 44px;
  height: 40px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

/* Size bình thường: viền xám mỏng, nền trắng */
.size-btn-normal {
  background-color: #ffffff;
  color: #64748b;
  border: 1.5px solid #cbd5e1;
}

.size-btn-normal:hover:not(:disabled) {
  background-color: #f8fafc;
  border-color: #94a3b8;
  color: #1e293b;
}

/* Size được chọn: màu xám than / slate giống ảnh mẫu */
.size-btn-active {
  background-color: #64748b !important;
  color: #ffffff !important;
  border-color: #64748b !important;
  box-shadow: 0 2px 6px rgba(100, 116, 139, 0.3);
}

/* Bộ đếm số lượng: - 1 + */
.qty-pill-box {
  height: 36px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #cbd5e1;
}

.qty-btn {
  width: 32px;
  height: 100%;
  border: none;
  background: transparent;
  font-size: 1.1rem;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
}

.qty-btn:hover:not(:disabled) {
  background: #f1f5f9;
  color: #0f172a;
}

.qty-input {
  width: 40px;
  height: 100%;
  font-size: 0.95rem;
  background: transparent;
  color: #1e293b;
}

/* 2 BUTTON NHỎ GỌN THEO ẢNH: Thêm vào giỏ & Mua ngay */
.btn-action-compact {
  padding: 0.5rem 1.4rem;
  font-size: 0.9rem;
  font-weight: 600;
  border-radius: 8px;
  transition: all 0.2s ease;
}

/* Nút "Thêm vào giỏ": viền xanh dương nhạt / xanh navy, nền trắng chữ xanh */
.btn-cart-compact {
  background-color: #ffffff !important;
  border: 1.5px solid #3b82f6 !important;
  color: #2563eb !important;
}

.btn-cart-compact:hover:not(:disabled) {
  background-color: #eff6ff !important;
  border-color: #2563eb !important;
  color: #1d4ed8 !important;
}

/* Nút "Mua ngay": nền xanh dương đậm đà chữ trắng đúng theo ảnh */
.btn-buy-compact {
  background-color: #2563eb !important;
  border: 1.5px solid #2563eb !important;
  color: #ffffff !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

.btn-buy-compact:hover:not(:disabled) {
  background-color: #1d4ed8 !important;
  border-color: #1d4ed8 !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}

/* Thumbnail swatches */
.color-swatch {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  background: #f8fafc;
}

.color-swatch:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
}

.color-swatch.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25);
}

.color-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.color-name {
  font-size: 0.75rem;
  font-weight: 600;
  text-align: center;
  padding: 2px;
  line-height: 1.2;
}

.product-perks-box {
  background: #f8fafc !important;
  border-color: #f1f5f9 !important;
}
</style>
