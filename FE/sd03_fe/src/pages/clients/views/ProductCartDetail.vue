<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { getOrder, money } from "../store";

const route = useRoute();
const order = computed(() => getOrder(route.params.id));
</script>

<template>
  <div v-if="order" class="ps-order-detail">
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <span class="text-dark fw-semibold">Chi tiết đơn hàng #{{ order.id }}</span>
    </nav>

    <div class="row g-4">
      <div class="col-lg-8">
        <div class="ps-card p-4 mb-4">
          <div class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-3">
            <div>
              <h4 class="mb-1">Đơn hàng #{{ order.id }}</h4>
              <div class="text-muted-ps small">Thời gian đặt: {{ order.date }}</div>
            </div>
            <span class="badge bg-success-subtle text-success fs-6 px-3 py-2 rounded-pill">
              Chờ xác nhận
            </span>
          </div>

          <h5 class="mb-3">Danh sách sản phẩm</h5>
          <div
            v-for="(i, idx) in order.items"
            :key="i.id + '-' + i.size"
            class="d-flex align-items-center gap-3 py-3"
            :class="{ 'border-top': idx }"
          >
            <div
              class="ps-thumb flex-shrink-0"
              style="width: 70px; height: 70px; font-size: 2rem"
            >
              {{ i.emoji }}
            </div>
            <div class="flex-grow-1">
              <div class="fw-bold">{{ i.name }}</div>
              <div class="small text-muted-ps">
                {{ i.brand }} · Size: {{ i.size }} · SL: {{ i.qty }}
              </div>
            </div>
            <div class="text-end">
              <div class="price">{{ money(i.price * i.qty) }}</div>
              <div class="small text-muted-ps">{{ money(i.price) }} / đôi</div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-4">
        <div class="ps-card p-4 mb-4">
          <h5 class="mb-3">Thông tin nhận hàng</h5>
          <div class="mb-2">
            <span class="text-muted-ps small d-block">Người nhận</span>
            <strong>{{ order.info?.name }}</strong>
          </div>
          <div class="mb-2">
            <span class="text-muted-ps small d-block">Số điện thoại</span>
            <span>{{ order.info?.phone }}</span>
          </div>
          <div class="mb-2">
            <span class="text-muted-ps small d-block">Địa chỉ</span>
            <span>{{ order.info?.address }}</span>
          </div>
          <div v-if="order.info?.note" class="mb-2">
            <span class="text-muted-ps small d-block">Ghi chú</span>
            <span>{{ order.info?.note }}</span>
          </div>
          <div class="mb-3">
            <span class="text-muted-ps small d-block">Hình thức thanh toán</span>
            <span>
              {{ order.info?.pay === 'bank' ? 'Chuyển khoản ngân hàng' : 'Thanh toán khi nhận hàng (COD)' }}
            </span>
          </div>

          <hr />

          <div class="d-flex justify-content-between mb-2">
            <span class="text-muted-ps">Tạm tính</span>
            <span>{{ money(order.total) }}</span>
          </div>
          <div class="d-flex justify-content-between mb-3">
            <span class="text-muted-ps">Phí giao hàng</span>
            <span class="text-success">Miễn phí</span>
          </div>
          <div class="d-flex justify-content-between border-top pt-3 mb-4">
            <span class="fw-bold">Tổng thanh toán</span>
            <span class="price fs-5">{{ money(order.total) }}</span>
          </div>

          <div class="d-flex flex-column gap-2">
            <RouterLink to="/cua-hang" class="btn btn-primary w-100 py-2">
              Tiếp tục mua hàng
            </RouterLink>
            <RouterLink to="/trang-chu" class="btn btn-light-border w-100 py-2 text-center text-decoration-none">
              Về trang chủ
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="ps-card text-center p-5">
    <div class="fs-1">🔍</div>
    <h4 class="mt-2">Không tìm thấy đơn hàng</h4>
    <p class="text-muted-ps">
      Mã đơn hàng không tồn tại hoặc đã bị xóa khỏi phiên làm việc.
    </p>
    <RouterLink to="/cua-hang" class="btn btn-primary px-4 mt-2">
      Quay lại cửa hàng
    </RouterLink>
  </div>
</template>
