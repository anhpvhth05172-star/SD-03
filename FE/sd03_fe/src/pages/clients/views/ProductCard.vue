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
const originalPrice = computed(() =>
  Number(props.product.price || props.product.giaBanMin || props.product.giaBan || 0)
);
// Giá thực hiển thị (giaSauGiam từ biến thể, hoặc giaBanMin từ sản phẩm)
const displayPrice = computed(() =>
  Number(props.product.giaSauGiam || props.product.giaBanMin || props.product.giaBan || props.product.price || 0)
);
// % giảm giá
const discountPercent = computed(() => {
  const pt = props.product.phanTramGiam;
  if (pt != null && Number(pt) > 0) return Number(pt);
  // fallback tính từ old (format cũ)
  const oldP = Number(props.product.old || 0);
  if (oldP > 0 && displayPrice.value > 0) {
    return Math.round((1 - displayPrice.value / oldP) * 100);
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
    <div class="ps-card p-3 h-100 d-flex flex-column gap-2">
      <div class="ps-thumb position-relative">
        <!-- Hiển thị ảnh thật nếu có, fallback về emoji -->
        <img
          v-if="displayImg"
          :src="displayImg"
          :alt="displayName"
          class="product-img"
          @error="(e) => { e.target.style.display='none'; e.target.nextSibling.style.display='flex'; }"
        />
        <div class="emoji-fallback" :style="displayImg ? 'display:none' : ''">👟</div>
        <span
          v-if="discountPercent > 0"
          class="ps-pill position-absolute top-0 start-0 m-2 bg-white"
        >
          -{{ discountPercent }}%
        </span>
      </div>
      <div class="small text-muted-ps">
        {{ displayBrand }}<span v-if="displayBrand && displayType"> · </span>{{ displayType }}
      </div>
      <h6 class="mb-0 fw-bold">{{ displayName }}</h6>
      <div class="mt-auto d-flex align-items-baseline gap-2">
        <span class="price">{{ money(displayPrice) }}</span>
        <!-- Giá gốc gạch ngang khi có đợt giảm giá -->
        <small
          v-if="discountPercent > 0 && originalPrice > displayPrice"
          class="text-muted-ps text-decoration-line-through"
          >{{ money(originalPrice) }}</small
        >
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
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
