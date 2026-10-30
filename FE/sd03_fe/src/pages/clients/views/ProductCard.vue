<script setup>
import { computed } from "vue";
import { money } from "../store";

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
});

const displayName = computed(() => props.product.tenSanPham || props.product.name || "");
const displayBrand = computed(() => props.product.tenThuongHieu || props.product.brand || "");
const displayType = computed(() => props.product.tenLoaiGiay || props.product.type || "");
const originalPrice = computed(() => Number(props.product.price || props.product.giaBanMin || props.product.giaBan || 0));
const displayPrice = computed(() => Number(props.product.giaSauGiam || props.product.giaBanMin || props.product.giaBan || props.product.price || 0));
const discountPercent = computed(() => {
  const pt = props.product.phanTramGiam;
  if (pt != null && Number(pt) > 0) return Number(pt);
  const oldP = Number(props.product.old || 0);
  if (oldP > 0 && displayPrice.value > 0) return Math.round((1 - displayPrice.value / oldP) * 100);
  return 0;
});
const displayImg = computed(() => props.product.img || null);
const detailPath = computed(() => `/cua-hang/${props.product.id}`);
</script>

<template>
  <article class="product-tile ps-card p-3 h-100 d-flex flex-column gap-2">
    <RouterLink :to="detailPath" class="product-tile-main text-decoration-none text-reset">
      <div class="ps-thumb product-tile-image position-relative">
        <img v-if="displayImg" :src="displayImg" :alt="displayName" class="product-img" @error="(e) => { e.target.style.display='none'; e.target.nextSibling.style.display='flex'; }" />
        <div class="emoji-fallback" :style="displayImg ? 'display:none' : ''">👟</div>
        <span v-if="discountPercent > 0" class="ps-pill position-absolute top-0 start-0 m-2 bg-white">-{{ discountPercent }}%</span>
      </div>
      <div class="small text-muted-ps mt-2">
        {{ displayBrand }}<span v-if="displayBrand && displayType"> · </span>{{ displayType }}
      </div>
      <h6 class="mb-0 fw-bold">{{ displayName }}</h6>
      <div class="d-flex align-items-baseline flex-wrap gap-2">
        <span class="price">{{ money(displayPrice) }}</span>
        <small v-if="discountPercent > 0 && originalPrice > displayPrice" class="text-muted-ps text-decoration-line-through">{{ money(originalPrice) }}</small>
      </div>
    </RouterLink>
    <RouterLink :to="detailPath" class="btn btn-outline-primary product-buy-btn mt-auto">
      <i class="bi bi-bag-plus me-1"></i> Chọn size &amp; mua
    </RouterLink>
  </article>
</template>

<style scoped>
.product-tile {
  border-radius: 14px;
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
}
.product-tile:hover {
  transform: translateY(-4px);
  border-color: #f0c4c3;
  box-shadow: 0 12px 28px rgba(48, 20, 20, .09);
}
.product-tile-main { display: flex; flex-direction: column; gap: .5rem; }
.product-tile-image {
  border-radius: 10px;
  overflow: hidden;
  background: linear-gradient(145deg, #fff5f4, #f8e9e8);
}
.product-tile h6 { line-height: 1.4; min-height: 2.5em; }
.product-buy-btn { font-size: .82rem; padding: .55rem .65rem; white-space: nowrap; }
.product-img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.emoji-fallback { display: flex; align-items: center; justify-content: center; font-size: 5rem; width: 100%; height: 100%; }
</style>
