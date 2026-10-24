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

    // Chọn màu đầu tiên có sẵn
    if (colors.value.length) {
      selectedColor.value = colors.value[0].idMauSac;
    }
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

// Ảnh của màu đang chọn (lấy ảnh biến thể đầu tiên của màu đó)
const displayImg = computed(() => {
  const first = variants.value.find((v) => v.idMauSac === selectedColor.value);
  return first?.img || product.value?.img || null;
});

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

// % giảm của biến thể đang chọn
const phanTramGiam = computed(() => {
  if (selectedVariant.value) return Number(selectedVariant.value.phanTramGiam || 0);
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
        img: selectedVariant.value.img,
      },
      selectedVariant.value.id,   // variantId (idSanPhamChiTiet)
      selectedVariant.value.size, // tên size
      qty.value,
      selectedVariant.value.color // tên màu
    );

    if (res?.needLogin) {
      addError.value = res.message || "Vui lòng đăng nhập để thêm vào giỏ hàng.";
      setTimeout(() => {
        router.push({ path: "/login", query: { redirect: route.fullPath } });
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

    <div class="ps-card p-3 p-md-4">
      <div class="row g-4">
        <!-- Ảnh sản phẩm -->
        <div class="col-md-5">
          <div class="ps-thumb" style="font-size: 9rem; position: relative; overflow: hidden;">
            <img
              v-if="displayImg"
              :src="displayImg"
              :alt="product.tenSanPham"
              style="width:100%; height:100%; object-fit:cover; border-radius:12px;"
              @error="(e) => { e.target.style.display='none'; e.target.nextSibling.style.display='flex'; }"
            />
            <div
              :style="displayImg ? 'display:none; align-items:center; justify-content:center; width:100%; height:100%;' : 'display:flex; align-items:center; justify-content:center; width:100%; height:100%;'"
            >👟</div>
          </div>

          <!-- Thumbnail màu sắc -->
          <div v-if="colors.length > 1" class="d-flex flex-wrap gap-2 mt-3">
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
        <div class="col-md-7">
          <span class="ps-pill">
            {{ product.tenThuongHieu }}
            <span v-if="product.tenLoaiGiay"> · {{ product.tenLoaiGiay }}</span>
          </span>
          <h2 class="mt-3 mb-1">{{ product.tenSanPham }}</h2>
          <div class="text-muted-ps small mb-1">Mã sản phẩm: {{ product.maSanPham }}</div>
          <div v-if="product.doiTuong" class="text-muted-ps small mb-3">
            Đối tượng: {{ product.doiTuong }}
          </div>

          <div class="d-flex align-items-baseline gap-3 mb-4">
            <span class="price fs-2">{{ money(displayPrice) }}</span>
            <span
              v-if="product.giaBanMax && product.giaBanMin && product.giaBanMax !== product.giaBanMin"
              class="text-muted-ps small"
            >
              – {{ money(product.giaBanMax) }}
            </span>
          </div>

          <!-- Chọn màu sắc -->
          <div v-if="colors.length" class="mb-3">
            <div class="fw-semibold mb-2">Màu sắc</div>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="c in colors"
                :key="c.idMauSac"
                class="btn btn-sm"
                :class="selectedColor === c.idMauSac ? 'btn-primary' : 'btn-outline-secondary'"
                @click="selectedColor = c.idMauSac"
              >
                {{ c.color }}
              </button>
            </div>
          </div>

          <!-- Chọn Size -->
          <div class="fw-semibold mb-2">Chọn size</div>
          <div class="d-flex flex-wrap gap-2 mb-2">
            <template v-for="v in sizesOfColor" :key="v.id">
              <input
                type="radio"
                class="btn-check"
                :id="'v' + v.id"
                :value="v"
                v-model="selectedVariant"
                :disabled="v.stock <= 0"
              />
              <label
                class="btn btn-outline-secondary size-btn"
                :class="{ 'opacity-50': v.stock <= 0 }"
                :for="'v' + v.id"
                :title="v.stock <= 0 ? 'Hết hàng' : `Còn ${v.stock} đôi`"
              >
                {{ v.size }}
                <span v-if="v.stock <= 0" class="d-block" style="font-size:0.6rem;">Hết</span>
              </label>
            </template>
            <div v-if="!sizesOfColor.length" class="text-muted-ps small">
              Vui lòng chọn màu sắc.
            </div>
          </div>
          <div v-if="addError" class="alert alert-danger py-2 px-3 small mb-2">{{ addError }}</div>
          <div v-if="added" class="alert alert-success py-2 px-3 small mb-2">
            ✓ {{ addSuccessMessage || "Đã thêm vào giỏ hàng thành công!" }}
          </div>

          <!-- Số lượng -->
          <div class="fw-semibold mt-3 mb-2">Số lượng</div>
          <div class="input-group mb-4" style="width: 140px">
            <button
              class="btn btn-light-border"
              @click="qty = Math.max(1, qty - 1)"
              :disabled="adding"
            >−</button>
            <input class="form-control text-center" :value="qty" readonly />
            <button
              class="btn btn-light-border"
              @click="qty++"
              :disabled="adding"
            >+</button>
          </div>

          <!-- Tồn kho -->
          <div v-if="selectedVariant" class="text-muted-ps small mb-3">
            Tồn kho: <strong>{{ selectedVariant.stock }}</strong> đôi
          </div>

          <div class="d-flex flex-wrap gap-2">
            <button class="btn btn-outline-primary px-4" @click="add(false)" :disabled="adding">
              <span v-if="adding" class="spinner-border spinner-border-sm me-1" role="status"></span>
              {{ added ? "✓ Đã thêm vào giỏ" : "Thêm vào giỏ" }}
            </button>
            <button class="btn btn-primary px-4" @click="add(true)" :disabled="adding">
              Mua ngay
            </button>
          </div>

          <!-- Mô tả -->
          <div v-if="product.moTa" class="mt-4">
            <div class="fw-semibold mb-1">Mô tả</div>
            <p class="text-muted-ps" style="white-space: pre-line;">{{ product.moTa }}</p>
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
.color-swatch {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 2px solid #dee2e6;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
  background: #f8f9fa;
}
.color-swatch.active {
  border-color: var(--ps-primary, #0d6efd);
  box-shadow: 0 0 0 2px var(--ps-primary, #0d6efd);
}
.color-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.color-name {
  font-size: 0.7rem;
  text-align: center;
  padding: 2px;
  line-height: 1.2;
}
</style>
