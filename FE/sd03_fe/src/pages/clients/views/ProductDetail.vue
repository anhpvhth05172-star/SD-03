<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getSanPhamById, getBienTheBySanPham } from "@/api/product";
import { money, addToCart } from "../store";
import Swal from "sweetalert2";

const route = useRoute();
const router = useRouter();

// ===== State =====
const product = ref(null); // SanPhamResponse
const variants = ref([]); // SanPhamChiTietResponse[]
const loading = ref(false);
const error = ref("");

// Lựa chọn người dùng
const selectedColor = ref(null); // idMauSac
const selectedSize = ref(null); // size
const qty = ref(1);
const adding = ref(false);

// ===== Thông báo (SweetAlert2) =====
// Toast nhỏ góc phải: dùng cho thông báo nhanh (thành công, vượt tồn kho khi gõ)
const Toast = Swal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 2000,
  timerProgressBar: true,
  backdrop: false, // toast không dùng nền mờ
  width: "auto", // co theo nội dung
  customClass: {
    container: "ps-toast-container",
    popup: "ps-toast-popup",
    title: "ps-toast-title",
  },
  didOpen: (toast) => {
    // Dừng timer khi rê chuột vào
    toast.addEventListener("mouseenter", Swal.stopTimer);
    toast.addEventListener("mouseleave", Swal.resumeTimer);
  },
});

function notify(icon, title) {
  Toast.fire({ icon, title });
}

// Hộp thoại giữa màn hình: dùng cho lỗi cần người dùng đọc và xử lý
function showAlert(icon, title, text) {
  return Swal.fire({
    icon,
    title,
    text,
    confirmButtonText: "Đã hiểu",
    confirmButtonColor: "#2563eb",
  });
}

// Gallery
const currentImageIndex = ref(0);
const imgFailed = ref(false);

// Tab dưới
const activeTab = ref("detail"); // detail | desc

// ===== Fetch =====
async function fetchData() {
  const id = route.params.id;
  if (!id) return;

  loading.value = true;
  error.value = "";
  product.value = null;
  variants.value = [];
  selectedColor.value = null;
  selectedSize.value = null;
  currentImageIndex.value = 0;
  qty.value = 1;

  try {
    const [spRes, varRes] = await Promise.all([
      getSanPhamById(id),
      getBienTheBySanPham(id),
    ]);
    product.value = spRes;

    // varRes có thể là PageResponse { content: [] } hoặc mảng trực tiếp
    variants.value = varRes?.content ?? varRes ?? [];
  } catch (e) {
    error.value = e.message || "Không thể tải thông tin sản phẩm.";
  } finally {
    loading.value = false;
  }
}

onMounted(fetchData);
watch(() => route.params.id, fetchData);

// ===== Computed =====

// Danh sách màu sắc duy nhất
const colors = computed(() => {
  const seen = new Set();
  return variants.value.filter((v) => {
    if (seen.has(v.idMauSac)) return false;
    seen.add(v.idMauSac);
    return true;
  });
});

// Danh sách size duy nhất (sắp xếp tăng dần)
const allSizes = computed(() => {
  const list = [...new Set(variants.value.map((v) => v.size))];
  return list.sort((a, b) =>
    String(a).localeCompare(String(b), undefined, { numeric: true }),
  );
});

// Biến thể theo màu + size
function variantOf(idMauSac, size) {
  return variants.value.find((v) => v.idMauSac === idMauSac && v.size === size);
}

// Biến thể đang được chọn (có đủ màu + size)
const selectedVariant = computed(() => {
  if (selectedColor.value == null || selectedSize.value == null) return null;
  return variantOf(selectedColor.value, selectedSize.value) || null;
});

// Tổng tồn kho của một màu
function stockOfColor(idMauSac) {
  return variants.value
    .filter((v) => v.idMauSac === idMauSac)
    .reduce((s, v) => s + Number(v.stock || 0), 0);
}

// Tổng tồn kho của một size
function stockOfSize(size) {
  return variants.value
    .filter((v) => v.size === size)
    .reduce((s, v) => s + Number(v.stock || 0), 0);
}

// Màu bị vô hiệu hóa?
function isColorDisabled(c) {
  if (selectedSize.value != null) {
    const v = variantOf(c.idMauSac, selectedSize.value);
    return !v || Number(v.stock || 0) <= 0;
  }
  return stockOfColor(c.idMauSac) <= 0;
}

// Size bị vô hiệu hóa?
function isSizeDisabled(size) {
  if (selectedColor.value != null) {
    const v = variantOf(selectedColor.value, size);
    return !v || Number(v.stock || 0) <= 0;
  }
  return stockOfSize(size) <= 0;
}

// Tổng tồn kho
const totalStock = computed(() =>
  variants.value.reduce((s, v) => s + Number(v.stock || 0), 0),
);

// Tồn kho đang áp dụng (theo biến thể nếu đã chọn)
const currentStock = computed(() =>
  selectedVariant.value
    ? Number(selectedVariant.value.stock || 0)
    : totalStock.value,
);

const inStock = computed(() => totalStock.value > 0);

// Danh sách ảnh
const allImages = computed(() => {
  const list = [];
  if (product.value?.img) list.push(product.value.img);
  variants.value.forEach((v) => {
    if (v.img && !list.includes(v.img)) list.push(v.img);
  });
  return list;
});

const displayImg = computed(
  () => allImages.value[currentImageIndex.value] || null,
);

watch(displayImg, () => {
  imgFailed.value = false;
});

function prevImage() {
  if (allImages.value.length <= 1) return;
  currentImageIndex.value =
    (currentImageIndex.value - 1 + allImages.value.length) %
    allImages.value.length;
}

function nextImage() {
  if (allImages.value.length <= 1) return;
  currentImageIndex.value =
    (currentImageIndex.value + 1) % allImages.value.length;
}

function goToImage(i) {
  currentImageIndex.value = i;
}

// Giá hiển thị
const priceMin = computed(() =>
  Number(product.value?.giaBanMin || product.value?.giaBan || 0),
);
const priceMax = computed(() =>
  Number(
    product.value?.giaBanMax ||
      product.value?.giaBanMin ||
      product.value?.giaBan ||
      0,
  ),
);
const hasPriceRange = computed(
  () => !selectedVariant.value && priceMax.value > priceMin.value,
);

const displayPrice = computed(() => {
  if (selectedVariant.value) {
    return Number(
      selectedVariant.value.giaSauGiam || selectedVariant.value.price || 0,
    );
  }
  return priceMin.value;
});

// Giá gốc (chỉ có ý nghĩa khi đã chọn biến thể)
const originalPrice = computed(() => {
  if (selectedVariant.value) return Number(selectedVariant.value.price || 0);
  return 0;
});

// % giảm của biến thể đang chọn
const phanTramGiam = computed(() => {
  const v = selectedVariant.value;
  if (!v) return 0;
  const pt = v.phanTramGiam || v.phanTramGiamKhac;
  if (pt != null && Number(pt) > 0) return Math.round(Number(pt));
  const orig = Number(v.price || 0);
  const curr = Number(v.giaSauGiam || orig);
  if (orig > curr && orig > 0) return Math.round(((orig - curr) / orig) * 100);
  return 0;
});

const showOriginal = computed(
  () => !!selectedVariant.value && originalPrice.value > displayPrice.value,
);

// Mã sản phẩm hiển thị
const displayCode = computed(
  () =>
    selectedVariant.value?.maSanPhamChiTiet ||
    selectedVariant.value?.barcode ||
    product.value?.maSanPham ||
    "—",
);

const selectedColorName = computed(
  () =>
    colors.value.find((c) => c.idMauSac === selectedColor.value)?.color || "",
);

// Bảng thông tin chi tiết: trường nào không có sẽ tự ẩn
const specs = computed(() => {
  const p = product.value;
  if (!p) return [];
  const rows = [
    { label: "Mã sản phẩm", value: p.maSanPham },
    { label: "Thương hiệu", value: p.tenThuongHieu },
    { label: "Loại giày", value: p.tenLoaiGiay },
    { label: "Chất liệu", value: p.tenChatLieu },
    { label: "Đế giày", value: p.tenDeGiay },
    { label: "Xuất xứ", value: p.tenXuatXu },
    { label: "Giới tính", value: p.tenGioiTinh || p.gioiTinh },
    { label: "Kiểu dáng", value: p.tenKieuDang },
    { label: "Số màu sắc", value: colors.value.length || null },
    { label: "Số kích cỡ", value: allSizes.value.length || null },
  ];
  return rows.filter(
    (r) => r.value !== null && r.value !== undefined && r.value !== "",
  );
});

// ===== Watch =====
// Khi biến thể thay đổi: reset lỗi và giới hạn số lượng theo tồn kho
watch(selectedVariant, (v) => {
  if (v) {
    const stock = Number(v.stock || 0);
    if (qty.value > stock) qty.value = Math.max(1, stock);
  } else {
    qty.value = 1;
  }
});

// ===== Actions =====
function selectColor(c) {
  if (isColorDisabled(c)) return;

  // Bấm lại màu đang chọn => bỏ chọn
  if (selectedColor.value === c.idMauSac) {
    selectedColor.value = null;
    return;
  }

  selectedColor.value = c.idMauSac;

  // Chuyển ảnh chính sang ảnh của màu vừa chọn
  if (c.img) {
    const idx = allImages.value.indexOf(c.img);
    if (idx >= 0) currentImageIndex.value = idx;
  }
}

function selectSize(size) {
  if (isSizeDisabled(size)) return;

  // Bấm lại size đang chọn => bỏ chọn
  if (selectedSize.value === size) {
    selectedSize.value = null;
    return;
  }

  selectedSize.value = size;
}

// ===== Số lượng =====

// Tồn kho tối đa cho phép nhập (Infinity nếu chưa chọn biến thể)
function maxQty() {
  return selectedVariant.value
    ? Number(selectedVariant.value.stock || 0)
    : Infinity;
}

function incQty() {
  const max = maxQty();
  if (qty.value < max) qty.value++;
  else notify("warning", `Chỉ còn ${max} sản phẩm trong kho.`);
}

function decQty() {
  qty.value = Math.max(1, qty.value - 1);
}

// Khi người dùng đang gõ: chỉ giữ lại chữ số, không ép về 1 ngay
// để người dùng có thể xóa hết rồi gõ số mới
function onQtyInput(e) {
  const input = e.target;
  const digits = input.value.replace(/\D/g, ""); // bỏ ký tự không phải số

  // Ô trống: cho phép tạm thời (sẽ chuẩn hóa ở blur)
  if (digits === "") {
    input.value = "";
    return;
  }

  let n = parseInt(digits, 10);
  const max = maxQty();

  if (n > max) {
    n = max;
    notify("warning", `Chỉ còn ${max} sản phẩm trong kho.`);
  }

  if (n < 1) n = 1;

  qty.value = n;
  input.value = String(n); // đồng bộ lại giá trị hiển thị
}

// Khi rời khỏi ô / nhấn Enter: nếu để trống hoặc không hợp lệ thì về 1
function onQtyBlur(e) {
  if (!qty.value || qty.value < 1) qty.value = 1;
  e.target.value = String(qty.value);
}

async function add(goCart = false) {
  // Chuẩn hóa số lượng trước khi validate
  if (!qty.value || qty.value < 1) qty.value = 1;

  if (colors.value.length > 0 && selectedColor.value == null) {
    showAlert("warning", "Chưa chọn màu sắc", "Vui lòng chọn màu sắc trước.");
    return;
  }
  if (!selectedVariant.value) {
    showAlert(
      "warning",
      "Chưa chọn kích cỡ",
      "Vui lòng chọn size trước khi thêm vào giỏ.",
    );
    return;
  }
  if (Number(selectedVariant.value.stock || 0) <= 0) {
    showAlert("error", "Hết hàng", "Sản phẩm này đã hết hàng.");
    return;
  }
  if (qty.value > Number(selectedVariant.value.stock || 0)) {
    showAlert(
      "warning",
      "Vượt quá tồn kho",
      `Chỉ còn ${selectedVariant.value.stock} sản phẩm trong kho.`,
    );
    return;
  }
  adding.value = true;

  try {
    const res = await addToCart(
      {
        id: product.value.id,
        tenSanPham: product.value.tenSanPham,
        tenThuongHieu: product.value.tenThuongHieu,
        giaBan: displayPrice.value,
        giaBanMin: displayPrice.value,
        img:
          selectedVariant.value.img ||
          displayImg.value ||
          product.value?.img ||
          null,
      },
      selectedVariant.value.id, // variantId (idSanPhamChiTiet)
      selectedVariant.value.size, // tên size
      qty.value,
      selectedVariant.value.color, // tên màu
    );

    if (res?.needLogin) {
      const result = await Swal.fire({
        icon: "info",
        title: "Cần đăng nhập",
        text: res.message || "Vui lòng đăng nhập để thêm vào giỏ hàng.",
        showCancelButton: true,
        confirmButtonText: "Đăng nhập",
        cancelButtonText: "Để sau",
        confirmButtonColor: "#2563eb",
      });
      if (result.isConfirmed) {
        router.push({
          path: "/dang-nhap",
          query: { redirect: route.fullPath },
        });
      }
      return;
    }

    if (goCart) {
      router.push("/gio-hang");
    } else {
      notify("success", res?.message || "Đã thêm sản phẩm vào giỏ hàng");
    }
  } catch (err) {
    showAlert(
      "error",
      "Không thể thêm vào giỏ",
      err.message || "Có lỗi xảy ra khi thêm vào giỏ hàng.",
    );
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
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2"
      >Quay lại danh sách</RouterLink
    >
  </div>

  <!-- Nội dung sản phẩm -->
  <div v-else-if="product">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      <span class="mx-1">/</span>
      <RouterLink to="/cua-hang" class="text-muted-ps text-decoration-none"
        >Sản phẩm</RouterLink
      >
      <span class="mx-1">/</span>
      <span class="text-dark fw-semibold">{{ product.tenSanPham }}</span>
    </nav>

    <div class="detail-card p-3 p-md-4">
      <div class="row g-4">
        <!-- ===== Cột ảnh ===== -->
        <div class="col-md-6 col-lg-5">
          <div class="gallery-sticky">
            <div class="gallery-box position-relative">
              <img
                v-if="displayImg && !imgFailed"
                :key="displayImg"
                :src="displayImg"
                :alt="product.tenSanPham"
                class="gallery-main-img"
                @error="imgFailed = true"
              />
              <div v-else class="gallery-fallback">👟</div>

              <span v-if="phanTramGiam > 0" class="gallery-badge-sale"
                >-{{ phanTramGiam }}%</span
              >
              <span v-if="!inStock" class="gallery-badge-out">Hết hàng</span>

              <button
                v-if="allImages.length > 1"
                type="button"
                class="carousel-nav-btn btn-nav-left"
                title="Ảnh trước"
                @click.stop="prevImage"
              >
                <i class="bi bi-chevron-left"></i>
              </button>
              <button
                v-if="allImages.length > 1"
                type="button"
                class="carousel-nav-btn btn-nav-right"
                title="Ảnh tiếp"
                @click.stop="nextImage"
              >
                <i class="bi bi-chevron-right"></i>
              </button>

              <span v-if="allImages.length > 1" class="gallery-counter">
                {{ currentImageIndex + 1 }} / {{ allImages.length }}
              </span>
            </div>

            <!-- Thumbnail ảnh -->
            <div v-if="allImages.length > 1" class="thumb-strip mt-3">
              <button
                v-for="(img, i) in allImages"
                :key="img + i"
                type="button"
                class="thumb-item"
                :class="{ active: i === currentImageIndex }"
                @click="goToImage(i)"
              >
                <img :src="img" :alt="`Ảnh ${i + 1}`" />
              </button>
            </div>
          </div>
        </div>

        <!-- ===== Cột thông tin ===== -->
        <div class="col-md-6 col-lg-7 ps-lg-4">
          <!-- Thương hiệu / loại -->
          <div class="d-flex flex-wrap align-items-center gap-2 mb-2">
            <span class="badge-brand-category">
              {{ product.tenThuongHieu || "Chính hãng"
              }}<template v-if="product.tenLoaiGiay">
                · {{ product.tenLoaiGiay }}</template
              >
            </span>
            <span class="stock-badge" :class="inStock ? 'in' : 'out'">
              <i
                :class="
                  inStock ? 'bi bi-check-circle-fill' : 'bi bi-x-circle-fill'
                "
              ></i>
              {{ inStock ? "Còn hàng" : "Hết hàng" }}
            </span>
          </div>

          <h1 class="product-detail-title mb-1">{{ product.tenSanPham }}</h1>

          <div class="meta-line mb-3">
            <span
              >Mã SP: <b>{{ displayCode }}</b></span
            >
            <span v-if="product.tenThuongHieu"
              >Thương hiệu: <b>{{ product.tenThuongHieu }}</b></span
            >
            <span v-if="variants.length"
              >Tồn kho: <b>{{ currentStock }}</b></span
            >
          </div>

          <!-- Giá -->
          <div class="price-box mb-4">
            <div class="d-flex flex-wrap align-items-baseline gap-3">
              <template v-if="hasPriceRange">
                <span class="price-highlight-red">{{ money(priceMin) }}</span>
                <span class="price-range-dash">–</span>
                <span class="price-highlight-red">{{ money(priceMax) }}</span>
              </template>
              <template v-else>
                <span class="price-highlight-red">{{
                  money(displayPrice)
                }}</span>
                <span v-if="showOriginal" class="price-old">{{
                  money(originalPrice)
                }}</span>
                <span v-if="phanTramGiam > 0" class="badge-discount-pink"
                  >-{{ phanTramGiam }}%</span
                >
              </template>
            </div>
            <div v-if="hasPriceRange" class="small text-muted mt-1">
              Giá thay đổi theo màu sắc và kích cỡ bạn chọn.
            </div>
          </div>

          <!-- Chọn màu: hiển thị tất cả, hết hàng thì làm mờ + không cho chọn -->
          <div v-if="colors.length > 0" class="mb-3">
            <label class="option-label">
              Màu sắc
              <span v-if="selectedColor != null" class="text-dark fw-bold ms-1"
                >: {{ selectedColorName }}</span
              >
              <span v-else class="text-danger fw-normal ms-1">(Chưa chọn)</span>
            </label>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="c in colors"
                :key="c.idMauSac"
                type="button"
                class="btn size-box-btn px-3"
                :class="[
                  selectedColor === c.idMauSac
                    ? 'size-btn-active'
                    : 'size-btn-normal',
                  { 'option-disabled': isColorDisabled(c) },
                ]"
                :disabled="isColorDisabled(c)"
                :title="isColorDisabled(c) ? 'Hết hàng' : c.color"
                @click="selectColor(c)"
              >
                <span
                  v-if="c.colorHex"
                  class="color-dot me-2"
                  :style="{ backgroundColor: c.colorHex }"
                ></span>
                {{ c.color }}
              </button>
            </div>
          </div>

          <!-- Chọn size: hiển thị tất cả, hết hàng thì làm mờ + không cho chọn -->
          <div v-if="allSizes.length > 0" class="mb-3">
            <label class="option-label">
              Kích cỡ
              <span v-if="selectedSize != null" class="text-dark fw-bold ms-1"
                >: {{ selectedSize }}</span
              >
              <span v-else class="text-danger fw-normal ms-1">(Chưa chọn)</span>
            </label>
            <div class="d-flex flex-wrap gap-2">
              <button
                v-for="size in allSizes"
                :key="size"
                type="button"
                class="btn size-box-btn"
                :class="[
                  selectedSize === size ? 'size-btn-active' : 'size-btn-normal',
                  { 'option-disabled': isSizeDisabled(size) },
                ]"
                :disabled="isSizeDisabled(size)"
                :title="isSizeDisabled(size) ? 'Hết hàng' : `Size ${size}`"
                @click="selectSize(size)"
              >
                {{ size }}
              </button>
            </div>
          </div>

          <!-- Số lượng -->
          <div class="mb-4">
            <label class="option-label">Số lượng</label>
            <div class="d-inline-flex align-items-center gap-3">
              <div
                class="qty-pill-box d-inline-flex align-items-center bg-white"
              >
                <button
                  type="button"
                  class="qty-btn"
                  :disabled="adding"
                  @click="decQty"
                >
                  −
                </button>
                <input
                  class="qty-input text-center border-0 fw-semibold"
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  maxlength="4"
                  :value="qty"
                  :disabled="adding"
                  @input="onQtyInput"
                  @blur="onQtyBlur"
                  @keydown.enter.prevent="$event.target.blur()"
                  @focus="$event.target.select()"
                />
                <button
                  type="button"
                  class="qty-btn"
                  :disabled="adding"
                  @click="incQty"
                >
                  +
                </button>
              </div>
              <span v-if="selectedVariant" class="text-muted small">
                (Còn {{ selectedVariant.stock }} sản phẩm)
              </span>
            </div>
          </div>

          <!-- Nút hành động -->
          <div class="d-flex flex-wrap align-items-center gap-2 mb-4">
            <button
              class="btn btn-action-compact btn-cart-compact"
              :disabled="adding || !inStock"
              @click="add(false)"
            >
              <span
                v-if="adding"
                class="spinner-border spinner-border-sm me-1"
                role="status"
              ></span>
              <i v-else class="bi bi-cart-plus me-1"></i>
              <span>Thêm vào giỏ</span>
            </button>

            <button
              class="btn btn-action-compact btn-buy-compact"
              :disabled="adding || !inStock"
              @click="add(true)"
            >
              <i class="bi bi-lightning-charge-fill me-1"></i>
              <span>Mua ngay</span>
            </button>
          </div>

          <!-- Perks -->
          <!-- <div class="product-perks-box p-3 rounded-4">
            <div class="row g-2 text-center text-md-start">
              <div class="col-12 col-md-4 d-flex align-items-center gap-2">
                <i class="bi bi-shield-check text-danger fs-4"></i>
                <div class="small lh-sm">
                  <div class="fw-bold">100% Chính hãng</div>
                  <span class="text-muted perk-sub">Kiểm tra hàng</span>
                </div>
              </div>
              <div
                class="col-12 col-md-4 d-flex align-items-center gap-2 perk-mid"
              >
                <i class="bi bi-arrow-repeat text-danger fs-4"></i>
                <div class="small lh-sm">
                  <div class="fw-bold">Đổi size 7 ngày</div>
                  <span class="text-muted perk-sub">Miễn phí đổi</span>
                </div>
              </div>
              <div class="col-12 col-md-4 d-flex align-items-center gap-2">
                <i class="bi bi-truck text-danger fs-4"></i>
                <div class="small lh-sm">
                  <div class="fw-bold">Giao hàng nhanh</div>
                  <span class="text-muted perk-sub">1-3 ngày nhận</span>
                </div>
              </div>
            </div>
          </div> -->
        </div>
      </div>
    </div>

    <!-- ===== Thông tin chi tiết / Mô tả ===== -->
    <div class="detail-card p-3 p-md-4 mt-4">
      <div class="info-tabs mb-3">
        <button
          type="button"
          class="info-tab"
          :class="{ active: activeTab === 'detail' }"
          @click="activeTab = 'detail'"
        >
          Thông tin chi tiết
        </button>
        <button
          type="button"
          class="info-tab"
          :class="{ active: activeTab === 'desc' }"
          @click="activeTab = 'desc'"
        >
          Mô tả sản phẩm
        </button>
      </div>

      <!-- Bảng thông số -->
      <div v-show="activeTab === 'detail'">
        <div v-if="specs.length" class="spec-grid">
          <div v-for="s in specs" :key="s.label" class="spec-row">
            <span class="spec-label">{{ s.label }}</span>
            <span class="spec-value">{{ s.value }}</span>
          </div>
        </div>
        <p v-else class="text-muted mb-0">
          Chưa có thông tin chi tiết cho sản phẩm này.
        </p>
      </div>

      <!-- Mô tả -->
      <div v-show="activeTab === 'desc'">
        <p
          v-if="product.moTa"
          class="text-muted-ps lh-lg mb-0"
          style="white-space: pre-line"
        >
          {{ product.moTa }}
        </p>
        <p v-else class="text-muted mb-0">Chưa có mô tả cho sản phẩm này.</p>
      </div>
    </div>
  </div>

  <!-- Không tìm thấy -->
  <div v-else class="ps-card text-center p-5">
    <h5>Không tìm thấy sản phẩm</h5>
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2"
      >Quay lại danh sách</RouterLink
    >
  </div>
</template>

<!-- ===== SweetAlert2 Toast (KHÔNG scoped vì Swal render ra <body>) ===== -->
<style>
.swal2-container.ps-toast-container {
  background: transparent !important; /* bỏ nền xám */
  width: auto !important;
  max-width: 100vw;
  padding: 16px !important;
  pointer-events: none; /* không chặn click phía sau */
}

.swal2-popup.ps-toast-popup {
  pointer-events: auto; /* riêng toast vẫn tương tác được */
  width: auto !important;
  min-width: 260px;
  max-width: 380px;
  padding: 12px 16px !important;
  display: flex !important;
  align-items: center;
  gap: 12px;
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.15) !important;
  font-size: 0.9rem !important;
  letter-spacing: normal !important;
}

.swal2-popup.ps-toast-popup .swal2-icon {
  width: 28px !important;
  height: 28px !important;
  min-width: 28px;
  margin: 0 !important;
  border-width: 2px !important;
}

.swal2-popup.ps-toast-popup .swal2-icon .swal2-icon-content {
  font-size: 1.2rem !important;
}

.swal2-popup.ps-toast-popup .swal2-success-ring {
  width: 28px !important;
  height: 28px !important;
  top: -2px !important;
  left: -2px !important;
}

.swal2-popup.ps-toast-popup .swal2-title.ps-toast-title {
  margin: 0 !important;
  padding: 0 !important;
  font-size: 0.9rem !important;
  font-weight: 600 !important;
  line-height: 1.35 !important;
  letter-spacing: normal !important;
  text-align: left !important;
  color: #1e293b !important;
}

.swal2-popup.ps-toast-popup .swal2-timer-progress-bar {
  background: rgba(37, 99, 235, 0.4);
}
</style>

<style scoped>
.detail-card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
  border: 1px solid #f1f5f9;
}

/* ===== Gallery ===== */
.gallery-sticky {
  position: sticky;
  top: 90px;
}

.gallery-box {
  background: linear-gradient(145deg, #fdf2f2 0%, #fbe9ee 100%);
  border-radius: 20px;
  height: 420px;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.gallery-main-img {
  max-width: 92%;
  max-height: 92%;
  object-fit: contain;
  filter: drop-shadow(0 18px 24px rgba(0, 0, 0, 0.12));
  transition: transform 0.35s ease;
  animation: fadeIn 0.3s ease;
}

.gallery-box:hover .gallery-main-img {
  transform: scale(1.05) rotate(-2deg);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.97);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.gallery-fallback {
  font-size: 7rem;
}

.gallery-badge-sale {
  position: absolute;
  top: 14px;
  left: 14px;
  background: #dc2626;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(220, 38, 38, 0.3);
}

.gallery-badge-out {
  position: absolute;
  top: 14px;
  right: 14px;
  background: #334155;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
}

.gallery-counter {
  position: absolute;
  bottom: 12px;
  right: 14px;
  background: rgba(15, 23, 42, 0.55);
  color: #fff;
  font-size: 0.72rem;
  padding: 2px 10px;
  border-radius: 999px;
}

.carousel-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  cursor: pointer;
  z-index: 5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
}

.carousel-nav-btn:hover {
  background: #fff;
  color: #0f172a;
  transform: translateY(-50%) scale(1.1);
}

.btn-nav-left {
  left: 14px;
}
.btn-nav-right {
  right: 14px;
}

.thumb-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.thumb-item {
  flex: 0 0 auto;
  width: 66px;
  height: 66px;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  background: #fbf0f0;
  padding: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.thumb-item:hover {
  border-color: #cbd5e1;
  transform: translateY(-2px);
}
.thumb-item.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.18);
}

/* ===== Thông tin ===== */
.badge-brand-category {
  display: inline-block;
  background-color: #fdf2f2;
  color: #ef4444;
  font-size: 0.78rem;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}

.stock-badge {
  font-size: 0.78rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
}
.stock-badge.in {
  background: #ecfdf5;
  color: #059669;
}
.stock-badge.out {
  background: #f1f5f9;
  color: #64748b;
}

.product-detail-title {
  font-size: 1.9rem;
  font-weight: 800;
  letter-spacing: -0.015em;
  color: #111827;
  line-height: 1.25;
}

.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  font-size: 0.85rem;
  color: #64748b;
}
.meta-line b {
  color: #1e293b;
  font-weight: 600;
}

.price-box {
  background: #fff7f7;
  border: 1px solid #fee2e2;
  border-radius: 14px;
  padding: 14px 18px;
}

.price-highlight-red {
  font-size: 1.9rem;
  color: #dc2626;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.price-range-dash {
  font-size: 1.5rem;
  color: #dc2626;
  font-weight: 700;
}

.price-old {
  color: #94a3b8;
  text-decoration: line-through;
  font-size: 1.05rem;
}

.badge-discount-pink {
  background-color: #fee2e2;
  color: #dc2626;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 6px;
}

.option-label {
  display: block;
  color: #475569;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 8px;
}

.color-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid #cbd5e1;
  vertical-align: middle;
}

/* Ô chọn size / màu */
.size-box-btn {
  min-width: 46px;
  height: 42px;
  padding: 0 12px;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.size-btn-normal {
  background-color: #ffffff;
  color: #475569;
  border: 1.5px solid #cbd5e1;
}

.size-btn-normal:hover:not(:disabled) {
  background-color: #f8fafc;
  border-color: #94a3b8;
  color: #1e293b;
  transform: translateY(-1px);
}

.size-btn-active {
  background-color: #1e293b !important;
  color: #ffffff !important;
  border-color: #1e293b !important;
  box-shadow: 0 4px 10px rgba(30, 41, 59, 0.25);
}

/* Hết hàng / không khả dụng: làm mờ + không cho chọn */
.option-disabled,
.option-disabled:disabled {
  background: #f8fafc !important;
  color: #94a3b8 !important;
  border: 1.5px dashed #e2e8f0 !important;
  opacity: 0.45;
  text-decoration: line-through;
  cursor: not-allowed !important;
  pointer-events: none;
  box-shadow: none !important;
  transform: none !important;
}

/* Số lượng */
.qty-pill-box {
  height: 40px;
  border-radius: 10px;
  overflow: hidden;
  border: 1.5px solid #cbd5e1;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.qty-pill-box:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.qty-btn {
  width: 36px;
  height: 100%;
  border: none;
  background: transparent;
  font-size: 1.15rem;
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
  width: 52px;
  height: 100%;
  font-size: 0.95rem;
  background: transparent;
  color: #1e293b;
}

.qty-input:focus {
  outline: none;
  box-shadow: none;
}

/* Nút hành động */
.btn-action-compact {
  padding: 0.6rem 1.6rem;
  font-size: 0.92rem;
  font-weight: 600;
  border-radius: 10px;
  transition: all 0.2s ease;
}

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

.btn-buy-compact {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  border: 1.5px solid #2563eb !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.28);
}

.btn-buy-compact:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.38);
}

/* Perks */
.product-perks-box {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}
.perk-sub {
  font-size: 0.75rem;
}
@media (min-width: 768px) {
  .perk-mid {
    border-left: 1px solid #e2e8f0;
    border-right: 1px solid #e2e8f0;
    padding-left: 12px;
    padding-right: 12px;
  }
}

/* ===== Tabs ===== */
.info-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.info-tab {
  background: none;
  border: none;
  padding: 10px 18px;
  font-weight: 600;
  font-size: 0.95rem;
  color: #64748b;
  border-bottom: 3px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.info-tab:hover {
  color: #1e293b;
}
.info-tab.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
}

/* Bảng thông số */
.spec-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 32px;
}

@media (max-width: 767px) {
  .spec-grid {
    grid-template-columns: 1fr;
  }
}

.spec-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 4px;
  border-bottom: 1px dashed #e2e8f0;
  font-size: 0.92rem;
}

.spec-label {
  color: #64748b;
}
.spec-value {
  color: #0f172a;
  font-weight: 600;
  text-align: right;
}
</style>
