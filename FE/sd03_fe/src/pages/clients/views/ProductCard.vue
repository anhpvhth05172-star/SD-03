<script setup>
import { computed } from "vue";
import { money } from "../store";

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
});

// Hỗ trợ cả format API mới (tenSanPham, giaSauGiam) và format cũ (name, price)
const displayName = computed(() => props.product.tenSanPham || props.product.name || "");
const displayBrand = computed(() => props.product.tenThuongHieu || props.product.brand || "");
const displayType = computed(() => props.product.tenLoaiGiay || props.product.type || "");

// Giá gốc (trước giảm)
const originalPrice = computed(() => {
  const p = props.product;
  const rawOriginal = p.giaGoc || p.old || p.giaBanMax || p.giaBan || p.price || 0;
  return Number(rawOriginal);
});

// Giá thực hiển thị (sau giảm hoặc giá bán min)
const displayPrice = computed(() => {
  const p = props.product;
  return Number(p.giaSauGiam || p.giaBanMin || p.giaBan || p.price || 0);
});

// % giảm giá
const discountPercent = computed(() => {
  const p = props.product;
  const pt = p.phanTramGiam || p.discountPercent || p.phanTramGiamKhac;
  if (pt != null && Number(pt) > 0) return Math.round(Number(pt));
  
  // Tính theo tỉ lệ giá gốc và giá bán
  if (originalPrice.value > displayPrice.value && originalPrice.value > 0) {
    return Math.round(((originalPrice.value - displayPrice.value) / originalPrice.value) * 100);
  }
  return 0;
});
const displayImg = computed(() => props.product.img || null);
</script>

<template>
  <RouterLink
    :to="`/cua-hang/${product.id}`"
    class="text-decoration-none text-reset"
  >
    <div class="ps-card product-card p-3 h-100 d-flex flex-column gap-2">
      <div class="ps-thumb position-relative overflow-hidden">
        <!-- Hiển thị ảnh thật nếu có, fallback về emoji -->
        <img
          v-if="displayImg"
          :src="displayImg"
          :alt="displayName"
          class="product-img"
          @error="(e) => { e.target.style.display='none'; e.target.nextSibling.style.display='flex'; }"
        />
        <div class="emoji-fallback" :style="displayImg ? 'display:none' : ''">👟</div>
        
        <!-- Badge % giảm giá góc trái trên -->
        <span
          v-if="discountPercent > 0"
          class="badge bg-danger position-absolute top-0 start-0 m-2 rounded-pill px-2 py-1 shadow-sm"
          style="font-size: 0.72rem; font-weight: 700;"
        >
          -{{ discountPercent }}%
        </span>

        <!-- Wishlist Heart Button góc phải trên -->
        <button
          type="button"
          class="wishlist-btn position-absolute top-0 end-0 m-2"
          title="Yêu thích"
          @click.prevent
        >
          <i class="bi bi-heart"></i>
        </button>

        <div class="card-quick-hover position-absolute bottom-0 start-0 w-100 p-2 text-center">
          <span class="btn btn-sm btn-light w-100 rounded-pill fw-bold shadow-sm">
            <i class="bi bi-eye me-1"></i> Xem chi tiết
          </span>
        </div>
      </div>
      <div class="small text-muted-ps d-flex align-items-center gap-1">
        <span class="fw-semibold text-secondary">{{ displayBrand || 'Chính hãng' }}</span>
        <span v-if="displayType">· {{ displayType }}</span>
      </div>
      <h6 class="mb-0 fw-bold product-title" :title="displayName">{{ displayName }}</h6>
      
      <!-- Khối hiển thị Giá: Giá giảm (hiện tại), Giá gốc và % giảm -->
      <div class="mt-auto pt-2">
        <div class="d-flex align-items-baseline gap-2 flex-wrap">
          <span class="price fs-5 fw-extrabold">{{ money(displayPrice) }}</span>
          <!-- Giá gốc gạch ngang khi có giảm giá -->
          <span
            v-if="discountPercent > 0 && originalPrice > displayPrice"
            class="text-muted-ps text-decoration-line-through small"
          >
            {{ money(originalPrice) }}
          </span>
        </div>
        <div v-if="discountPercent > 0" class="small mt-1">
          <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-0">
            Tiết kiệm {{ discountPercent }}%
          </span>
        </div>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.product-card {
  position: relative;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
  transition: transform 0.4s ease;
}

.product-card:hover .product-img {
  transform: scale(1.08);
}

.product-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.35;
  font-size: 0.95rem;
  transition: color 0.2s ease;
}

.product-card:hover .product-title {
  color: var(--ps-red);
}

.card-quick-hover {
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.25s ease;
  pointer-events: none;
}

.product-card:hover .card-quick-hover {
  opacity: 1;
  transform: translateY(0);
}

.wishlist-btn {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);
  border: 1px solid #e2e8f0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 0.85rem;
  transition: all 0.2s ease;
  cursor: pointer;
  z-index: 5;
}

.wishlist-btn:hover {
  background: #ffffff;
  color: #d32f2f;
  transform: scale(1.15);
}

.emoji-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 5rem;
  width: 100%;
  height: 100%;
}
</style>
