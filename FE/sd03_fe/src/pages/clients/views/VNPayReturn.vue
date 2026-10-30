<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { clearCart, money } from "../store";

const router = useRouter();

const loading = ref(true);
const result = ref(null);
const retrying = ref(false);
const retryError = ref("");

const formatPayDate = (dStr) => {
  if (!dStr || dStr.length !== 14) return dStr || "—";
  const y = dStr.slice(0, 4), m = dStr.slice(4, 6), d = dStr.slice(6, 8);
  const h = dStr.slice(8, 10), mi = dStr.slice(10, 12), s = dStr.slice(12, 14);
  return `${d}/${m}/${y} ${h}:${mi}:${s}`;
};

onMounted(async () => {
  try {
    const search = window.location.search;
    if (!search) {
      result.value = {
        success: false,
        message: "Không tìm thấy thông tin giao dịch từ VNPAY. Có thể bạn truy cập trang này trực tiếp.",
      };
      loading.value = false;
      return;
    }

    const res = await fetch(`http://localhost:8080/api/vnpay/payment-callback${search}`);
    if (!res.ok) throw new Error(`Máy chủ phản hồi lỗi ${res.status}`);

    const data = await res.json();
    result.value = data;

    if (data.success) {
      // ✅ Thanh toán thành công → xóa giỏ hàng (đơn đã được xác nhận phía BE)
      try { await clearCart(); } catch (e) {}

      // Lưu đơn vào danh sách tra cứu (nếu có mã đơn)
      if (data.maHoaDon && data.orderId) {
        try {
          const orders = JSON.parse(localStorage.getItem("ps_orders") || "[]");
          const exists = orders.some((o) => o.id === data.maHoaDon);
          if (!exists) {
            orders.unshift({
              id: data.maHoaDon,
              orderId: data.orderId,
              info: { pay: "vnpay" },
              total: data.amount,
              date: new Date().toLocaleString("vi-VN"),
            });
            localStorage.setItem("ps_orders", JSON.stringify(orders.slice(0, 20)));
          }
        } catch (e) {}
      }
    }
    // ❌ Nếu thất bại: BE đã xóa đơn CHO_THANH_TOAN → giỏ hàng vẫn còn nguyên
  } catch (err) {
    console.error("[VNPayReturn] Lỗi xác thực:", err);
    result.value = {
      success: false,
      message: err.message || "Không thể kết nối đến máy chủ để xác thực kết quả thanh toán.",
    };
  } finally {
    loading.value = false;
  }
});

// Thử lại thanh toán → quay về giỏ hàng để đặt lại
function goToCart() {
  router.push("/gio-hang");
}

async function retryPayment() {
  retrying.value = true;
  retryError.value = "";
  // Giỏ hàng vẫn còn → quay lại trang thanh toán
  try {
    router.push("/thanh-toan");
  } catch (e) {
    retryError.value = "Không thể quay lại trang thanh toán.";
    retrying.value = false;
  }
}
</script>

<template>
  <div class="vnpay-return-container py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-7 col-md-9">

          <!-- Đang tải / xác thực -->
          <div v-if="loading" class="card shadow-sm border-0 rounded-4 text-center p-5">
            <div class="spinner-border text-primary mx-auto mb-3" style="width: 3rem; height: 3rem;" role="status">
              <span class="visually-hidden">Đang xử lý...</span>
            </div>
            <h5 class="fw-bold mb-2">Đang xác thực kết quả thanh toán VNPAY</h5>
            <p class="text-muted small mb-0">Hệ thống đang kiểm tra chữ ký và cập nhật trạng thái đơn hàng. Vui lòng đợi...</p>
          </div>

          <!-- ✅ Thanh toán thành công -->
          <div v-else-if="result?.success" class="card shadow-sm border-0 rounded-4 overflow-hidden">
            <div class="card-header bg-success text-white text-center py-4 border-0">
              <div class="status-icon bg-white text-success rounded-circle mx-auto mb-3 d-flex align-items-center justify-content-center shadow-sm">
                <i class="bi bi-check-lg fs-1"></i>
              </div>
              <h4 class="fw-bold mb-1">Đặt hàng &amp; Thanh toán thành công!</h4>
              <p class="mb-0 opacity-75 small">Đơn hàng đã được xác nhận và đang chờ xử lý</p>
            </div>

            <div class="card-body p-4 p-md-5">
              <div class="alert alert-success d-flex align-items-center gap-2 mb-4">
                <i class="bi bi-info-circle-fill fs-5"></i>
                <span class="small">Poly Shoes đã nhận đơn hàng của bạn và sẽ liên hệ xác nhận sớm nhất!</span>
              </div>

              <h6 class="fw-bold text-uppercase text-muted small mb-3">Chi tiết giao dịch</h6>
              <div class="bg-light rounded-3 p-3 mb-4">
                <div class="d-flex justify-content-between py-2 border-bottom">
                  <span class="text-muted small">Mã đơn hàng</span>
                  <strong class="text-dark">#{{ result.maHoaDon || result.orderId }}</strong>
                </div>
                <div class="d-flex justify-content-between py-2 border-bottom">
                  <span class="text-muted small">Số tiền thanh toán</span>
                  <strong class="text-danger fs-6">{{ money(result.amount) }}</strong>
                </div>
                <div v-if="result.transactionNo" class="d-flex justify-content-between py-2 border-bottom">
                  <span class="text-muted small">Mã giao dịch VNPAY</span>
                  <span class="font-monospace small fw-semibold">{{ result.transactionNo }}</span>
                </div>
                <div v-if="result.bankCode" class="d-flex justify-content-between py-2 border-bottom">
                  <span class="text-muted small">Ngân hàng</span>
                  <span class="badge bg-primary-subtle text-primary fw-semibold">{{ result.bankCode }}</span>
                </div>
                <div v-if="result.payDate" class="d-flex justify-content-between py-2">
                  <span class="text-muted small">Thời gian giao dịch</span>
                  <span class="text-muted small">{{ formatPayDate(result.payDate) }}</span>
                </div>
              </div>

              <div class="d-grid d-sm-flex gap-2 justify-content-center">
                <RouterLink
                  v-if="result.orderId"
                  :to="`/don-hang/${result.orderId}`"
                  class="btn btn-outline-primary px-4 py-2"
                >
                  <i class="bi bi-receipt me-1"></i> Xem chi tiết đơn hàng
                </RouterLink>
                <RouterLink to="/cua-hang" class="btn btn-primary px-4 py-2">
                  <i class="bi bi-bag me-1"></i> Tiếp tục mua hàng
                </RouterLink>
              </div>
            </div>
          </div>

          <!-- ❌ Thanh toán thất bại / hủy -->
          <div v-else class="card shadow-sm border-0 rounded-4 overflow-hidden">
            <div class="card-header bg-danger text-white text-center py-4 border-0">
              <div class="status-icon bg-white text-danger rounded-circle mx-auto mb-3 d-flex align-items-center justify-content-center shadow-sm">
                <i class="bi bi-x-lg fs-1"></i>
              </div>
              <h4 class="fw-bold mb-1">Thanh toán không thành công</h4>
              <p class="mb-0 opacity-75 small">Đơn hàng tạm thời đã bị hủy — giỏ hàng của bạn vẫn được giữ nguyên</p>
            </div>

            <div class="card-body p-4 p-md-5">
              <div class="alert alert-danger mb-4">
                <div class="d-flex align-items-start gap-2">
                  <i class="bi bi-exclamation-triangle-fill fs-5 flex-shrink-0 mt-1"></i>
                  <div>
                    <strong>Lý do: </strong>{{ result?.message || "Giao dịch không thành công." }}
                  </div>
                </div>
              </div>

              <div v-if="result?.responseCode" class="bg-light rounded-3 p-3 mb-4">
                <div class="d-flex justify-content-between py-1">
                  <span class="text-muted small">Mã phản hồi VNPAY</span>
                  <span class="font-monospace text-muted small">{{ result.responseCode }}</span>
                </div>
              </div>

              <div v-if="retryError" class="alert alert-warning small mb-3">{{ retryError }}</div>

              <div class="alert alert-info small mb-4">
                <i class="bi bi-cart-check me-1"></i>
                Giỏ hàng của bạn vẫn được giữ nguyên. Bạn có thể thử lại thanh toán hoặc chọn hình thức khác.
              </div>

              <div class="d-grid d-sm-flex gap-2 justify-content-center">
                <button
                  type="button"
                  class="btn btn-danger px-4 py-2"
                  :disabled="retrying"
                  @click="retryPayment"
                >
                  <span v-if="retrying" class="spinner-border spinner-border-sm me-1"></span>
                  <i v-else class="bi bi-arrow-repeat me-1"></i>
                  Thử thanh toán lại
                </button>
                <button type="button" class="btn btn-outline-secondary px-4 py-2" @click="goToCart">
                  <i class="bi bi-cart me-1"></i> Về giỏ hàng
                </button>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vnpay-return-container {
  min-height: 70vh;
  display: flex;
  align-items: center;
}
.status-icon {
  width: 70px;
  height: 70px;
}
</style>
