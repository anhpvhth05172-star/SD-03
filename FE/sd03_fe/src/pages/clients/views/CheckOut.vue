<script setup>
import { reactive, ref } from "vue";
import { cart, cartTotal, money, placeOrder } from "../store";

const f = reactive({ name: "", phone: "", address: "", note: "", pay: "cod" });
const errors = reactive({});
const done = ref(false);
const orderId = ref("");

function submit() {
  errors.name = f.name.trim() ? "" : "Vui lòng nhập họ và tên.";
  errors.phone = /^0\d{9}$/.test(f.phone)
    ? ""
    : "Số điện thoại gồm 10 số, bắt đầu bằng 0.";
  errors.address = f.address.trim() ? "" : "Vui lòng nhập địa chỉ nhận hàng.";
  if (errors.name || errors.phone || errors.address) return;

  const id = placeOrder({
    name: f.name.trim(),
    phone: f.phone.trim(),
    address: f.address.trim(),
    note: f.note.trim(),
    pay: f.pay,
  });

  orderId.value = id;
  done.value = true;
}
</script>

<template>
  <div v-if="done" class="ps-card text-center p-5">
    <div class="fs-1">🎉</div>
    <h3 class="mt-2">Đặt hàng thành công</h3>
    <p class="mb-2">
      Mã đơn hàng của bạn: <strong class="text-danger">#{{ orderId }}</strong>
    </p>
    <p class="text-muted-ps mb-4">
      Poly Shoes sẽ gọi xác nhận đơn qua số {{ f.phone }} trong ít phút.
    </p>
    <div class="d-flex justify-content-center gap-2">
      <RouterLink :to="`/don-hang/${orderId}`" class="btn btn-outline-primary px-4">
        Chi tiết đơn hàng
      </RouterLink>
      <RouterLink to="/cua-hang" class="btn btn-primary px-4">
        Tiếp tục mua sắm
      </RouterLink>
    </div>
  </div>

  <div v-else-if="!cart.items.length" class="ps-card text-center p-5">
    <h5>Giỏ hàng đang trống</h5>
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2"
      >Xem sản phẩm</RouterLink
    >
  </div>

  <div v-else>
    <h2 class="mb-4">Thanh toán</h2>
    <div class="row g-4">
      <div class="col-lg-7">
        <div class="ps-card p-4">
          <h5 class="mb-3">Thông tin nhận hàng</h5>
          <div class="mb-3">
            <label class="form-label fw-semibold small">Họ và tên</label>
            <input
              v-model="f.name"
              class="form-control"
              :class="{ 'is-invalid': errors.name }"
              placeholder="Nguyễn Văn A"
            />
            <div class="invalid-feedback">{{ errors.name }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label fw-semibold small">Số điện thoại</label>
            <input
              v-model="f.phone"
              class="form-control"
              :class="{ 'is-invalid': errors.phone }"
              placeholder="0912345678"
            />
            <div class="invalid-feedback">{{ errors.phone }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label fw-semibold small">Địa chỉ</label>
            <input
              v-model="f.address"
              class="form-control"
              :class="{ 'is-invalid': errors.address }"
              placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
            />
            <div class="invalid-feedback">{{ errors.address }}</div>
          </div>
          <div class="mb-4">
            <label class="form-label fw-semibold small"
              >Ghi chú (không bắt buộc)</label
            >
            <textarea
              v-model="f.note"
              rows="2"
              class="form-control"
              placeholder="Ghi chú về đơn hàng, thời gian nhận hàng..."
            ></textarea>
          </div>
          <h5 class="mb-3">Phương thức thanh toán</h5>
          <div class="form-check mb-2">
            <input
              id="cod"
              v-model="f.pay"
              value="cod"
              type="radio"
              class="form-check-input"
            />
            <label for="cod" class="form-check-label"
              >Thanh toán khi nhận hàng (COD)</label
            >
          </div>
          <div class="form-check">
            <input
              id="bank"
              v-model="f.pay"
              value="bank"
              type="radio"
              class="form-check-input"
            />
            <label for="bank" class="form-check-label"
              >Chuyển khoản ngân hàng</label
            >
          </div>
        </div>
      </div>

      <div class="col-lg-5">
        <div class="ps-card p-4">
          <h5 class="mb-3">Đơn hàng của bạn</h5>
          <div
            v-for="i in cart.items"
            :key="i.id + '-' + i.size"
            class="d-flex justify-content-between small mb-2"
          >
            <span>{{ i.name }} (size {{ i.size }}) × {{ i.qty }}</span>
            <span>{{ money(i.price * i.qty) }}</span>
          </div>
          <div class="d-flex justify-content-between border-top pt-3 my-3">
            <span class="fw-bold">Tổng cộng</span
            ><span class="price fs-5">{{ money(cartTotal) }}</span>
          </div>
          <button class="btn btn-primary w-100 py-2" @click="submit">
            Đặt hàng
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
