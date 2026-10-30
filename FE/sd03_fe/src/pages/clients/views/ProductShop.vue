<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getSanPhamClient } from "@/api/product";
import ProductCard from "./ProductCard.vue";

const route = useRoute();
const q = ref("");
const brand = ref(route.query.brand || "");
const type = ref("");
const sort = ref("");
const allProducts = ref([]);
const loading = ref(false);
const error = ref("");
const brands = computed(() => [...new Set(allProducts.value.map((p) => p.tenThuongHieu).filter(Boolean))]);
const types = computed(() => [...new Set(allProducts.value.map((p) => p.tenLoaiGiay).filter(Boolean))]);

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
let searchTimer = null;
watch(q, () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(fetchProducts, 400);
});
watch(() => route.query.brand, (val) => { brand.value = val || ""; });
const list = computed(() => {
  let r = allProducts.value.filter((p) => (!brand.value || p.tenThuongHieu === brand.value) && (!type.value || p.tenLoaiGiay === type.value));
  if (sort.value === "asc") r = [...r].sort((a, b) => Number(a.giaBanMin || a.giaBan || 0) - Number(b.giaBanMin || b.giaBan || 0));
  if (sort.value === "desc") r = [...r].sort((a, b) => Number(b.giaBanMin || b.giaBan || 0) - Number(a.giaBanMin || a.giaBan || 0));
  return r;
});
const reset = () => { q.value = ""; brand.value = ""; type.value = ""; sort.value = ""; fetchProducts(); };
</script>

<template>
  <section class="shop-hero mb-4">
    <div><div class="shop-eyebrow">POLY SHOES / CỬA HÀNG</div><h1>Khám phá bộ sưu tập</h1><p>Tìm đôi giày hợp phong cách và nhịp sống của bạn.</p></div>
    <i class="bi bi-bag-heart shop-hero-icon" aria-hidden="true"></i>
  </section>

  <div class="shop-filter ps-card p-3 p-md-4 mb-4">
    <div class="filter-heading"><i class="bi bi-sliders me-2"></i>Bộ lọc sản phẩm</div>
    <div class="row g-3">
      <div class="col-md-4"><label class="form-label fw-semibold small">Tìm kiếm</label><input v-model="q" class="form-control" placeholder="Tên hoặc mã sản phẩm (vd: SP001, Nike...)" /></div>
      <div class="col-6 col-md-3"><label class="form-label fw-semibold small">Thương hiệu</label><select v-model="brand" class="form-select"><option value="">Tất cả thương hiệu</option><option v-for="b in brands" :key="b">{{ b }}</option></select></div>
      <div class="col-6 col-md-3"><label class="form-label fw-semibold small">Loại giày</label><select v-model="type" class="form-select"><option value="">Tất cả loại giày</option><option v-for="t in types" :key="t">{{ t }}</option></select></div>
      <div class="col-md-2"><label class="form-label fw-semibold small">Sắp xếp</label><select v-model="sort" class="form-select"><option value="">Mặc định</option><option value="asc">Giá tăng dần</option><option value="desc">Giá giảm dần</option></select></div>
    </div>
  </div>

  <div v-if="loading" class="text-center py-5"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Đang tải...</span></div><p class="mt-2 text-muted-ps">Đang tải sản phẩm...</p></div>
  <div v-else-if="error" class="ps-card text-center p-5"><div class="fs-1">⚠️</div><h5 class="mt-2">Lỗi tải dữ liệu</h5><p class="text-muted-ps">{{ error }}</p><button class="btn btn-primary px-4" @click="fetchProducts">Thử lại</button></div>
  <template v-else>
    <div class="shop-results"><span><strong>{{ list.length }}</strong> sản phẩm phù hợp</span><span v-if="brand" class="active-filter">{{ brand }} <button type="button" aria-label="Xóa thương hiệu" @click="brand = ''">×</button></span><span v-if="type" class="active-filter">{{ type }} <button type="button" aria-label="Xóa loại giày" @click="type = ''">×</button></span></div>
    <div v-if="list.length" class="row g-3 g-lg-4"><div v-for="p in list" :key="p.id" class="col-6 col-md-4 col-lg-3"><ProductCard :product="p" /></div></div>
    <div v-else class="ps-card text-center p-5"><div class="fs-1">👟</div><h5 class="mt-2">Không tìm thấy sản phẩm nào</h5><p class="text-muted-ps">Thử thay đổi từ khóa tìm kiếm hoặc đặt lại các bộ lọc.</p><button class="btn btn-light-border px-4" @click="reset">Xóa bộ lọc</button></div>
  </template>
</template>

<style scoped>
.shop-hero{display:flex;align-items:center;justify-content:space-between;min-height:175px;padding:2rem 2.5rem;border-radius:18px;background:linear-gradient(110deg,#fff0ef 0%,#fff9f8 65%,#f9dddd 100%);overflow:hidden}.shop-eyebrow{font-size:.7rem;font-weight:800;letter-spacing:.14em;color:#a61f2b;margin-bottom:.6rem}.shop-hero h1{font-size:clamp(1.75rem,4vw,2.5rem);letter-spacing:-.05em;margin:0 0 .45rem}.shop-hero p{color:#68717e;margin:0}.shop-hero-icon{font-size:5rem;color:rgba(198,40,40,.18);margin-right:2rem}.shop-filter{border-radius:14px}.filter-heading{font-weight:800;margin:0 0 1rem;color:#28303d}.shop-results{display:flex;align-items:center;gap:.65rem;color:#727987;font-size:.9rem;margin:1.4rem 0 .9rem}.shop-results strong{color:#202735}.active-filter{background:#fff0ef;color:#a61f1f;border-radius:999px;padding:.3rem .65rem;font-size:.78rem}.active-filter button{border:0;background:none;color:inherit;font-size:1rem;margin-left:.25rem;line-height:1}@media(max-width:575.98px){.shop-hero{padding:1.4rem;min-height:150px}.shop-hero-icon{font-size:3rem;margin-right:0}}
</style>
