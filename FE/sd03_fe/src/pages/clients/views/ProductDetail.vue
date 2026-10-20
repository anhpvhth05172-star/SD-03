<script setup>
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { products, money, addToCart } from "../store";

const route = useRoute();
const router = useRouter();
const p = computed(() =>
  products.find((x) => x.id === Number(route.params.id)),
);
const size = ref(null);
const qty = ref(1);
const error = ref("");
const added = ref(false);

function add(goCart = false) {
  if (!size.value) {
    error.value = "Vui lòng chọn size trước khi thêm vào giỏ.";
    return;
  }
  error.value = "";
  addToCart(p.value, size.value, qty.value);
  if (goCart) router.push("/gio-hang");
  else {
    added.value = true;
    setTimeout(() => (added.value = false), 2000);
  }
}
</script>

<template>
  <div v-if="p">
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      /
      <RouterLink to="/cua-hang" class="text-muted-ps text-decoration-none"
        >Sản phẩm</RouterLink
      >
      /
      <span class="text-dark fw-semibold">{{ p.name }}</span>
    </nav>

    <div class="ps-card p-3 p-md-4">
      <div class="row g-4">
        <div class="col-md-5">
          <div class="ps-thumb" style="font-size: 9rem">{{ p.emoji }}</div>
        </div>
        <div class="col-md-7">
          <span class="ps-pill">{{ p.brand }} · {{ p.type }}</span>
          <h2 class="mt-3 mb-1">{{ p.name }}</h2>
          <div class="text-muted-ps small mb-3">Mã sản phẩm: {{ p.code }}</div>
          <div class="d-flex align-items-baseline gap-3 mb-4">
            <span class="price fs-2">{{ money(p.price) }}</span>
            <span
              v-if="p.old"
              class="text-muted-ps text-decoration-line-through"
              >{{ money(p.old) }}</span
            >
          </div>

          <div class="fw-semibold mb-2">Chọn size</div>
          <div class="d-flex flex-wrap gap-2 mb-2">
            <template v-for="s in p.sizes" :key="s">
              <input
                type="radio"
                class="btn-check"
                :id="'s' + s"
                :value="s"
                v-model="size"
              />
              <label
                class="btn btn-outline-secondary size-btn"
                :for="'s' + s"
                >{{ s }}</label
              >
            </template>
          </div>
          <div v-if="error" class="text-danger small mb-2">{{ error }}</div>

          <div class="fw-semibold mt-3 mb-2">Số lượng</div>
          <div class="input-group mb-4" style="width: 140px">
            <button
              class="btn btn-light-border"
              @click="qty = Math.max(1, qty - 1)"
            >
              −
            </button>
            <input class="form-control text-center" :value="qty" readonly />
            <button class="btn btn-light-border" @click="qty++">+</button>
          </div>

          <div class="d-flex flex-wrap gap-2">
            <button class="btn btn-outline-primary px-4" @click="add(false)">
              {{ added ? "Đã thêm vào giỏ" : "Thêm vào giỏ" }}
            </button>
            <button class="btn btn-primary px-4" @click="add(true)">
              Mua ngay
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="ps-card text-center p-5">
    <h5>Không tìm thấy sản phẩm</h5>
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2"
      >Quay lại danh sách</RouterLink
    >
  </div>
</template>
