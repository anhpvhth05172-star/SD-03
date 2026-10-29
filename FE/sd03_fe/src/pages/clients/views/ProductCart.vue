<script setup>
import { onMounted, ref } from "vue";
import {
  cart,
  cartTotal,
  cartCount,
  money,
  removeItem,
  updateCartItemQty,
  clearCart,
  fetchCart,
} from "../store";
import { getUser } from "@/utils/auth";

const user = ref(getUser());
const actionLoadingId = ref(null);
const actionError = ref("");

onMounted(() => {
  fetchCart();
});

const handleQtyChange = async (item, delta) => {
  const newQty = item.qty + delta;
  if (newQty < 1) return;
  if (item.stock != null && newQty > item.stock) {
    actionError.value = `Kho chỉ còn ${item.stock} sản phẩm`;
    setTimeout(() => (actionError.value = ""), 3000);
    return;
  }
  actionLoadingId.value = item.idGioHangChiTiet || item.id;
  actionError.value = "";
  try {
    if (item.idGioHangChiTiet) {
      await updateCartItemQty(item.idGioHangChiTiet, newQty);
    } else {
      item.qty = newQty;
    }
  } catch (err) {
    actionError.value = err.message || "Không thể cập nhật số lượng";
    setTimeout(() => (actionError.value = ""), 3000);
  } finally {
    actionLoadingId.value = null;
  }
};

const handleRemove = async (item, idx) => {
  if (!confirm(`Bạn có chắc muốn xóa "${item.name}" khỏi giỏ hàng?`)) return;
  actionLoadingId.value = item.idGioHangChiTiet || item.id;
  actionError.value = "";
  try {
    await removeItem(item.idGioHangChiTiet ? item : idx);
  } catch (err) {
    actionError.value = err.message || "Không thể xóa sản phẩm";
    setTimeout(() => (actionError.value = ""), 3000);
  } finally {
    actionLoadingId.value = null;
  }
};

const handleClear = async () => {
  if (!confirm("Bạn có chắc chắn muốn xóa toàn bộ giỏ hàng?")) return;
  actionError.value = "";
  try {
    await clearCart();
  } catch (err) {
    actionError.value = err.message || "Không thể xóa giỏ hàng";
    setTimeout(() => (actionError.value = ""), 3000);
  }
};
</script>

<template>
  <div class="d-flex align-items-center justify-content-between mb-4">
    <h2 class="mb-0">
      Giỏ hàng
      <span v-if="cartCount > 0" class="badge bg-secondary fs-6 align-middle ms-2">
        {{ cartCount }} sản phẩm
      </span>
    </h2>
    <button
      v-if="cart.items.length > 0"
      class="btn btn-sm btn-outline-danger"
      @click="handleClear"
      :disabled="cart.loading"
    >
      <i class="bi bi-trash me-1"></i> Xóa tất cả
    </button>
  </div>

  <!-- Thông báo đăng nhập nếu là khách -->
  <div v-if="!user" class="alert alert-info d-flex align-items-center justify-content-between py-2 px-3 mb-4">
    <div class="small">
      <i class="bi bi-info-circle me-2"></i>
      Đăng nhập tài khoản để lưu trữ giỏ hàng an toàn và nhận nhiều ưu đãi hơn.
    </div>
    <RouterLink to="/login?redirect=/gio-hang" class="btn btn-sm btn-primary">
      Đăng nhập ngay
    </RouterLink>
  </div>

  <!-- Cảnh báo lỗi -->
  <div v-if="actionError" class="alert alert-danger py-2 px-3 mb-3">
    {{ actionError }}
  </div>

  <!-- Loading State -->
  <div v-if="cart.loading && !cart.items.length" class="text-center py-5">
    <div class="spinner-border text-primary" role="status">
      <span class="visually-hidden">Đang tải giỏ hàng...</span>
    </div>
    <p class="mt-2 text-muted-ps">Đang tải giỏ hàng từ máy chủ...</p>
  </div>

  <div v-else-if="cart.items.length" class="row g-4">
    <div class="col-lg-8">
      <div class="ps-card p-3">
        <div
          v-for="(i, idx) in cart.items"
          :key="(i.idGioHangChiTiet || i.id) + '-' + i.size"
          class="d-flex align-items-center gap-3 py-3"
          :class="{ 'border-top': idx, 'opacity-75': !i.conHang }"
        >
          <!-- Thumbnail ảnh -->
          <div
            class="ps-thumb flex-shrink-0 position-relative"
            style="width: 80px; height: 80px; overflow: hidden; border-radius: 8px;"
          >
            <img
              v-if="i.img"
              :src="i.img"
              :alt="i.name"
              style="width: 100%; height: 100%; object-fit: cover;"
              @error="(e) => { e.target.style.display='none'; e.target.nextSibling.style.display='flex'; }"
            />
            <div
              :style="i.img ? 'display:none; align-items:center; justify-content:center; width:100%; height:100%; font-size:2.2rem;' : 'display:flex; align-items:center; justify-content:center; width:100%; height:100%; font-size:2.2rem;'"
            >
              👟
            </div>
            <span
              v-if="!i.conHang"
              class="badge bg-danger position-absolute top-50 start-50 translate-middle"
              style="font-size: 0.65rem;"
            >
              Hết hàng
            </span>
          </div>

          <!-- Thông tin sản phẩm -->
          <div class="flex-grow-1 min-w-0">
            <div class="fw-bold text-truncate">{{ i.name }}</div>
            <div class="small text-muted-ps">
              <span v-if="i.color">Màu: <strong>{{ i.color }}</strong></span>
              <span v-if="i.color && i.size"> · </span>
              <span v-if="i.size">Size: <strong>{{ i.size }}</strong></span>
              <span v-if="i.brand" class="d-none d-md-inline"> ({{ i.brand }})</span>
            </div>
            <div class="price mt-1">{{ money(i.price) }}</div>
            <div v-if="i.canhBao" class="text-danger small mt-1">
              ⚠️ {{ i.canhBao }}
            </div>
          </div>

          <!-- Bộ chỉnh số lượng -->
          <div class="input-group input-group-sm" style="width: 110px">
            <button
              class="btn btn-light-border"
              :disabled="actionLoadingId === (i.idGioHangChiTiet || i.id) || i.qty <= 1"
              @click="handleQtyChange(i, -1)"
            >
              −
            </button>
            <input
              class="form-control text-center bg-white"
              :value="i.qty"
              readonly
            />
            <button
              class="btn btn-light-border"
              :disabled="actionLoadingId === (i.idGioHangChiTiet || i.id) || (i.stock != null && i.qty >= i.stock)"
              @click="handleQtyChange(i, 1)"
            >
              +
            </button>
          </div>

          <!-- Nút xóa -->
          <button
            class="btn btn-sm btn-outline-danger"
            :disabled="actionLoadingId === (i.idGioHangChiTiet || i.id)"
            @click="handleRemove(i, idx)"
            title="Xóa sản phẩm"
          >
            <i class="bi bi-trash"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- Tóm tắt đơn hàng -->
    <div class="col-lg-4">
      <div class="ps-card p-4">
        <h5 class="mb-3">Tóm tắt đơn hàng</h5>
        <div class="d-flex justify-content-between mb-2">
          <span class="text-muted-ps">Tạm tính</span>
          <span>{{ money(cartTotal) }}</span>
        </div>
        <div class="d-flex justify-content-between mb-3">
          <span class="text-muted-ps">Phí vận chuyển</span>
          <span class="text-success">Miễn phí</span>
        </div>
        <div class="d-flex justify-content-between border-top pt-3 mb-4">
          <span class="fw-bold">Tổng cộng</span>
          <span class="price fs-5">{{ money(cartTotal) }}</span>
        </div>
        <RouterLink to="/thanh-toan" class="btn btn-primary w-100 py-2">
          Tiến hành thanh toán
        </RouterLink>
        <RouterLink to="/cua-hang" class="btn btn-outline-secondary w-100 mt-2 py-2">
          Tiếp tục mua hàng
        </RouterLink>
      </div>
    </div>
  </div>

  <!-- Trống giỏ hàng -->
  <div v-else class="ps-card text-center p-5">
    <div class="fs-1">🛒</div>
    <h5 class="mt-2">Giỏ hàng đang trống</h5>
    <p class="text-muted-ps">Chọn một đôi giày bạn thích để bắt đầu mua sắm.</p>
    <RouterLink to="/cua-hang" class="btn btn-primary px-4">
      Xem sản phẩm
    </RouterLink>
  </div>
</template>
