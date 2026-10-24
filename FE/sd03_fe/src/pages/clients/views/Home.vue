<script setup>
import { ref, onMounted } from "vue";
import { getSanPhamClient } from "@/api/product";
import ProductCard from "./ProductCard.vue";

const featured = ref([]);
const brands = ref([]);

onMounted(async () => {
  try {
    const res = await getSanPhamClient({ page: 0, size: 8 });
    const list = res?.content ?? res ?? [];
    featured.value = list.slice(0, 4);
    brands.value = [
      ...new Set(list.map((p) => p.tenThuongHieu).filter(Boolean)),
    ];
  } catch {
    // Lỗi load: hiển thị trang bình thường, không có sản phẩm featured
  }
});

const perks = [
  { t: "Chính hãng 100%", d: "Hoàn tiền gấp đôi nếu phát hiện hàng giả." },
  { t: "Đổi size trong 7 ngày", d: "Không vừa chân, đổi miễn phí lần đầu." },
  {
    t: "Giao nhanh toàn quốc",
    d: "Nhận hàng trong 1–3 ngày, kiểm tra trước khi thanh toán.",
  },
];
</script>

<template>
  <section class="hero p-4 p-md-5 mb-5">
    <div class="row align-items-center g-4">
      <div class="col-md-7">
        <span class="ps-pill">Bộ sưu tập mới 2026</span>
        <h1 class="display-4 mt-3 mb-3">Giày đẹp,<br />bước cùng bạn.</h1>
        <p class="text-muted-ps fs-5 mb-4" style="max-width: 460px">
          Hàng trăm mẫu sneaker chính hãng từ Nike, Adidas, Vans và nhiều thương
          hiệu khác.
        </p>
        <RouterLink to="/cua-hang" class="btn btn-primary btn-lg px-4"
          >Mua sắm ngay</RouterLink
        >
      </div>
      <div class="col-md-5 text-center">
        <div
          class="ps-thumb mx-auto bg-white"
          style="max-width: 320px; font-size: 9rem; transform: rotate(-12deg)"
        >
          👟
        </div>
      </div>
    </div>
  </section>

  <section v-if="brands.length" class="mb-5">
    <div class="d-flex flex-wrap gap-2">
      <RouterLink
        v-for="b in brands"
        :key="b"
        :to="{ path: '/cua-hang', query: { brand: b } }"
        class="btn btn-light-border px-4"
        >{{ b }}</RouterLink
      >
    </div>
  </section>

  <section v-if="featured.length" class="mb-5">
    <div class="d-flex justify-content-between align-items-end mb-3">
      <h3 class="mb-0">Sản phẩm nổi bật</h3>
      <RouterLink to="/cua-hang" class="fw-semibold text-decoration-none"
        >Xem tất cả</RouterLink
      >
    </div>
    <div class="row g-3">
      <div v-for="p in featured" :key="p.id" class="col-6 col-lg-3">
        <ProductCard :product="p" />
      </div>
    </div>
  </section>

  <section class="row g-3">
    <div v-for="x in perks" :key="x.t" class="col-md-4">
      <div class="ps-card p-4 h-100">
        <h6 class="fw-bold">{{ x.t }}</h6>
        <p class="mb-0 text-muted-ps">{{ x.d }}</p>
      </div>
    </div>
  </section>
</template>
