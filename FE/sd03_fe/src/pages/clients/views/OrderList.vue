<script setup>
import { ref, computed } from "vue";
import { money } from "../store";

const q = ref("");
const statusFilter = ref("all");

const orders = computed(() => {
  try {
    const list = JSON.parse(localStorage.getItem("ps_orders")) || [];
    return list.reverse(); // newest first
  } catch {
    return [];
  }
});

const filteredOrders = computed(() => {
  return orders.value.filter((o) => {
    const matchQ =
      !q.value ||
      o.id.toLowerCase().includes(q.value.toLowerCase()) ||
      o.info?.name?.toLowerCase().includes(q.value.toLowerCase()) ||
      o.info?.phone?.includes(q.value);
    return matchQ;
  });
});
</script>

<template>
  <div class="ps-order-list-page">
    <!-- Breadcrumb -->
    <nav class="small mb-4 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <span class="text-dark fw-semibold">Đơn hàng của bạn</span>
    </nav>

    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
      <div>
        <h2 class="mb-1">Đơn hàng của bạn</h2>
        <p class="text-muted-ps mb-0">Quản lý và theo dõi trạng thái các đơn hàng đã đặt</p>
      </div>
      <RouterLink to="/tra-cuu" class="btn btn-outline-primary px-4">
        <i class="bi bi-search me-1"></i> Tra cứu đơn hàng khác
      </RouterLink>
    </div>

    <!-- Filter & Search Bar -->
    <div class="ps-card p-3 mb-4">
      <div class="row g-3 align-items-center">
        <div class="col-md-6">
          <div class="input-group">
            <span class="input-group-text bg-white border-end-0">
              <i class="bi bi-search text-muted"></i>
            </span>
            <input
              v-model="q"
              class="form-control border-start-0"
              placeholder="Tìm theo mã đơn (vd: HD123456), tên hoặc số điện thoại..."
            />
          </div>
        </div>
        <div class="col-md-6 text-md-end">
          <span class="text-muted-ps small">
            Tổng cộng: <strong>{{ filteredOrders.length }}</strong> đơn hàng
          </span>
        </div>
      </div>
    </div>

    <!-- Orders List -->
    <div v-if="filteredOrders.length" class="d-flex flex-column gap-4">
      <div
        v-for="o in filteredOrders"
        :key="o.id"
        class="ps-card p-4 transition-hover"
      >
        <!-- Order Header -->
        <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center border-bottom pb-3 mb-3 gap-2">
          <div>
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold fs-5 text-dark">Đơn hàng #{{ o.id }}</span>
              <span class="badge bg-success-subtle text-success rounded-pill px-3 py-1">
                Chờ xác nhận
              </span>
            </div>
            <div class="text-muted-ps small mt-1">
              <i class="bi bi-clock me-1"></i> Ngày đặt: {{ o.date }}
            </div>
          </div>
          <div class="text-sm-end">
            <div class="text-muted-ps small">Tổng tiền:</div>
            <span class="price fs-5">{{ money(o.total) }}</span>
          </div>
        </div>

        <!-- Order Items -->
        <div class="d-flex flex-column gap-3 mb-3">
          <div
            v-for="item in o.items"
            :key="item.id + '-' + item.size"
            class="d-flex align-items-center gap-3"
          >
            <div
              class="ps-thumb flex-shrink-0"
              style="width: 60px; height: 60px; font-size: 1.8rem"
            >
              {{ item.emoji }}
            </div>
            <div class="flex-grow-1">
              <div class="fw-bold text-dark">{{ item.name }}</div>
              <div class="small text-muted-ps">
                {{ item.brand }} · Size: {{ item.size }} · Số lượng: {{ item.qty }}
              </div>
            </div>
            <div class="text-end">
              <span class="fw-bold text-dark">{{ money(item.price * item.qty) }}</span>
            </div>
          </div>
        </div>

        <!-- Recipient & Action Footer -->
        <div class="bg-light p-3 rounded-3 d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
          <div class="small text-muted-ps">
            <span class="fw-semibold text-dark">{{ o.info?.name }}</span> · {{ o.info?.phone }} ·
            <span class="text-truncate d-inline-block align-bottom" style="max-width: 320px;">
              {{ o.info?.address }}
            </span>
          </div>
          <div class="d-flex gap-2">
            <RouterLink :to="`/don-hang/${o.id}`" class="btn btn-primary btn-sm px-3">
              Xem chi tiết
            </RouterLink>
            <RouterLink to="/cua-hang" class="btn btn-light-border btn-sm px-3">
              Mua lại
            </RouterLink>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="ps-card text-center p-5">
      <div class="display-3 mb-3">📦</div>
      <h4 class="fw-bold">Bạn chưa có đơn hàng nào</h4>
      <p class="text-muted-ps mb-4" style="max-width: 420px; margin: 0 auto">
        Hãy khám phá ngay hàng trăm mẫu sneaker chính hãng với nhiều ưu đãi hấp dẫn đang chờ đón bạn.
      </p>
      <RouterLink to="/cua-hang" class="btn btn-primary px-4 py-2">
        Mua sắm ngay
      </RouterLink>
    </div>
  </div>
</template>
