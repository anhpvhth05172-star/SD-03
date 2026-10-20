<script setup>
import { ref, computed, watch } from "vue";
import { useRoute } from "vue-router";
import { products, brands, types } from "../store";
import ProductCard from "./ProductCard.vue";

const route = useRoute();
const q = ref("");
const brand = ref(route.query.brand || "");
const type = ref("");
const sort = ref("");

watch(
  () => route.query.brand,
  (val) => {
    brand.value = val || "";
  }
);

const list = computed(() => {
  let r = products.filter(
    (p) =>
      (!q.value ||
        (p.name + p.code).toLowerCase().includes(q.value.toLowerCase())) &&
      (!brand.value || p.brand === brand.value) &&
      (!type.value || p.type === type.value),
  );
  if (sort.value === "asc") r = [...r].sort((a, b) => a.price - b.price);
  if (sort.value === "desc") r = [...r].sort((a, b) => b.price - a.price);
  return r;
});

const reset = () => {
  q.value = "";
  brand.value = "";
  type.value = "";
  sort.value = "";
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

  <p class="text-muted-ps">
    {{ list.length }} / {{ products.length }} sản phẩm
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
