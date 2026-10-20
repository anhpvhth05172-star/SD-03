<script setup>
import { cart, cartTotal, money, removeItem } from "../store";
</script>

<template>
  <h2 class="mb-4">Giỏ hàng</h2>

  <div v-if="cart.items.length" class="row g-4">
    <div class="col-lg-8">
      <div class="ps-card p-3">
        <div
          v-for="(i, idx) in cart.items"
          :key="i.id + '-' + i.size"
          class="d-flex align-items-center gap-3 py-3"
          :class="{ 'border-top': idx }"
        >
          <div
            class="ps-thumb flex-shrink-0"
            style="width: 80px; font-size: 2.2rem"
          >
            {{ i.emoji }}
          </div>
          <div class="flex-grow-1">
            <div class="fw-bold">{{ i.name }}</div>
            <div class="small text-muted-ps">
              {{ i.brand }} · Size {{ i.size }}
            </div>
            <div class="price mt-1">{{ money(i.price) }}</div>
          </div>
          <div class="input-group input-group-sm" style="width: 110px">
            <button
              class="btn btn-light-border"
              @click="i.qty = Math.max(1, i.qty - 1)"
            >
              −
            </button>
            <input class="form-control text-center" :value="i.qty" readonly />
            <button class="btn btn-light-border" @click="i.qty++">+</button>
          </div>
          <button
            class="btn btn-sm btn-outline-primary"
            @click="removeItem(idx)"
          >
            Xóa
          </button>
        </div>
      </div>
    </div>

    <div class="col-lg-4">
      <div class="ps-card p-4">
        <h5 class="mb-3">Tóm tắt đơn hàng</h5>
        <div class="d-flex justify-content-between mb-2">
          <span class="text-muted-ps">Tạm tính</span
          ><span>{{ money(cartTotal) }}</span>
        </div>
        <div class="d-flex justify-content-between mb-3">
          <span class="text-muted-ps">Phí vận chuyển</span
          ><span class="text-success">Miễn phí</span>
        </div>
        <div class="d-flex justify-content-between border-top pt-3 mb-4">
          <span class="fw-bold">Tổng cộng</span
          ><span class="price fs-5">{{ money(cartTotal) }}</span>
        </div>
        <RouterLink to="/thanh-toan" class="btn btn-primary w-100 py-2"
          >Tiến hành thanh toán</RouterLink
        >
      </div>
    </div>
  </div>

  <div v-else class="ps-card text-center p-5">
    <div class="fs-1">🛒</div>
    <h5 class="mt-2">Giỏ hàng đang trống</h5>
    <p class="text-muted-ps">Chọn một đôi giày bạn thích để bắt đầu.</p>
    <RouterLink to="/cua-hang" class="btn btn-primary px-4"
      >Xem sản phẩm</RouterLink
    >
  </div>
</template>
