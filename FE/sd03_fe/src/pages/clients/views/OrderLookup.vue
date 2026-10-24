<script setup>
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import { getOrder, money } from "../store";

const route = useRoute();
const searchInput = ref(route.query.code || "");
const searched = ref(false);
const currentResult = ref(null);
const errorMessage = ref("");

// Quick order history from localStorage
const recentOrders = computed(() => {
  try {
    return JSON.parse(localStorage.getItem("ps_orders")) || [];
  } catch {
    return [];
  }
});

const doSearch = (codeToSearch) => {
  const query = (codeToSearch || searchInput.value).trim().toUpperCase();
  if (!query) {
    errorMessage.value = "Vui lòng nhập mã đơn hàng hoặc số điện thoại.";
    return;
  }
  errorMessage.value = "";
  searched.value = true;
  searchInput.value = query;

  const cleanCode = query.startsWith("#") ? query.slice(1) : query;
  
  // Try finding by ID
  let found = getOrder(cleanCode);
  
  // If not found by ID, try finding by phone in localStorage
  if (!found && recentOrders.value.length) {
    found = recentOrders.value.find(
      (o) => o.info?.phone === query || o.id === cleanCode
    );
  }

  currentResult.value = found || null;
};

// If code query param passed, execute search immediately
if (route.query.code) {
  doSearch(route.query.code);
}
</script>

<template>
  <div class="ps-order-lookup-page">
    <!-- Breadcrumb -->
    <nav class="small mb-4 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <span class="text-dark fw-semibold">Tra cứu đơn hàng</span>
    </nav>

    <!-- Search Hero -->
    <section class="ps-card p-4 p-md-5 mb-5 text-center">
      <div class="mx-auto" style="max-width: 650px">
        <span class="ps-pill">Kiểm tra tiến độ đơn hàng</span>
        <h2 class="display-6 mt-3 mb-2 fw-bold">Tra cứu đơn hàng của bạn</h2>
        <p class="text-muted-ps mb-4">
          Nhập mã đơn hàng (ví dụ: <strong class="text-dark">HD123456</strong>) hoặc số điện thoại đặt hàng để kiểm tra thông tin vận chuyển và chi tiết đơn.
        </p>

        <!-- Search Input -->
        <div class="input-group input-group-lg mb-3 shadow-sm">
          <span class="input-group-text bg-white border-end-0">
            <i class="bi bi-search text-muted"></i>
          </span>
          <input
            v-model="searchInput"
            class="form-control border-start-0 border-end-0"
            placeholder="Nhập mã đơn hàng hoặc số điện thoại..."
            @keyup.enter="doSearch()"
          />
          <button class="btn btn-primary px-4" @click="doSearch()">
            Tra cứu ngay
          </button>
        </div>

        <div v-if="errorMessage" class="text-danger small mb-3">
          {{ errorMessage }}
        </div>

        <!-- Recent Order Quick Chips -->
        <div v-if="recentOrders.length" class="mt-4 pt-3 border-top text-start">
          <span class="small fw-semibold text-muted-ps d-block mb-2">
            Đơn hàng bạn đã đặt gần đây:
          </span>
          <div class="d-flex flex-wrap gap-2">
            <button
              v-for="o in recentOrders.slice(-4)"
              :key="o.id"
              type="button"
              class="btn btn-sm btn-light-border px-3"
              @click="doSearch(o.id)"
            >
              <i class="bi bi-receipt me-1"></i> #{{ o.id }} ({{ o.info?.name || 'Khách' }})
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Recent Orders List Section (Khi chưa tìm kiếm cụ thể và đã có đơn hàng) -->
    <section v-if="!searched && recentOrders.length" class="mb-5">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h4 class="mb-1 fw-bold">
            <i class="bi bi-clock-history me-2 text-danger"></i>Đơn hàng bạn đã đặt ({{ recentOrders.length }})
          </h4>
          <p class="text-muted-ps small mb-0">Nhấn tra cứu để theo dõi lộ trình hoặc xem chi tiết từng đơn hàng</p>
        </div>
      </div>

      <div class="d-flex flex-column gap-3">
        <div
          v-for="o in recentOrders"
          :key="o.id"
          class="ps-card p-4"
        >
          <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center border-bottom pb-3 mb-3 gap-2">
            <div>
              <div class="d-flex align-items-center gap-2">
                <span class="fw-bold fs-5 text-dark">Đơn hàng #{{ o.id }}</span>
                <span class="badge bg-success-subtle text-success rounded-pill px-3 py-1">
                  Đang xử lý
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
          <div class="d-flex flex-column gap-2 mb-3">
            <div
              v-for="item in o.items"
              :key="item.id + '-' + item.size"
              class="d-flex align-items-center gap-3"
            >
              <div
                class="ps-thumb flex-shrink-0"
                style="width: 50px; height: 50px; font-size: 1.5rem"
              >
                {{ item.emoji || '👟' }}
              </div>
              <div class="flex-grow-1">
                <div class="fw-bold text-dark small">{{ item.name }}</div>
                <div class="small text-muted-ps">
                  {{ item.brand }} · Size: {{ item.size }} · SL: {{ item.qty }}
                </div>
              </div>
              <div class="text-end">
                <span class="fw-semibold text-dark small">{{ money(item.price * item.qty) }}</span>
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
              <button class="btn btn-outline-danger btn-sm px-3" @click="doSearch(o.id)">
                <i class="bi bi-search me-1"></i>Theo dõi tiến độ
              </button>
              <RouterLink :to="`/don-hang/${o.id}`" class="btn btn-primary btn-sm px-3">
                Chi tiết
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Search Result Section -->
    <section v-if="searched" class="mb-5">
      <div class="mb-3">
        <button class="btn btn-sm btn-light-border px-3" @click="searched = false; searchInput = ''">
          <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách đơn hàng
        </button>
      </div>
      <div v-if="currentResult" class="ps-card p-4 p-md-5">
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center border-bottom pb-4 mb-4 gap-3">
          <div>
            <div class="d-flex align-items-center gap-2">
              <h3 class="mb-0 fw-bold">Đơn hàng #{{ currentResult.id }}</h3>
              <span class="badge bg-success-subtle text-success fs-6 px-3 py-1 rounded-pill">
                Đang xử lý
              </span>
            </div>
            <div class="text-muted-ps small mt-1">
              Thời gian đặt hàng: {{ currentResult.date }}
            </div>
          </div>
          <div>
            <RouterLink :to="`/don-hang/${currentResult.id}`" class="btn btn-outline-primary px-4">
              Xem trang chi tiết đầy đủ
            </RouterLink>
          </div>
        </div>

        <!-- Delivery Stepper Progress -->
        <div class="mb-5 py-2">
          <div class="row text-center g-3">
            <div class="col-3">
              <div class="d-inline-flex align-items-center justify-content-center rounded-circle bg-danger text-white mb-2" style="width: 44px; height: 44px">
                <i class="bi bi-check-lg fs-5"></i>
              </div>
              <div class="fw-bold small">Đã đặt hàng</div>
              <div class="text-muted-ps" style="font-size: 0.75rem">{{ currentResult.date }}</div>
            </div>
            <div class="col-3">
              <div class="d-inline-flex align-items-center justify-content-center rounded-circle text-danger mb-2" style="width: 44px; height: 44px; background: var(--ps-soft)">
                <i class="bi bi-box-seam fs-5"></i>
              </div>
              <div class="fw-bold small">Đã xác nhận</div>
              <div class="text-muted-ps" style="font-size: 0.75rem">Đang đóng gói</div>
            </div>
            <div class="col-3">
              <div class="d-inline-flex align-items-center justify-content-center rounded-circle text-muted mb-2 bg-light border" style="width: 44px; height: 44px">
                <i class="bi bi-truck fs-5"></i>
              </div>
              <div class="text-muted small">Đang vận chuyển</div>
              <div class="text-muted-ps" style="font-size: 0.75rem">Dự kiến 1-2 ngày</div>
            </div>
            <div class="col-3">
              <div class="d-inline-flex align-items-center justify-content-center rounded-circle text-muted mb-2 bg-light border" style="width: 44px; height: 44px">
                <i class="bi bi-house-check fs-5"></i>
              </div>
              <div class="text-muted small">Giao thành công</div>
              <div class="text-muted-ps" style="font-size: 0.75rem">Nhận hàng & kiểm tra</div>
            </div>
          </div>
        </div>

        <!-- Information & Items Summary -->
        <div class="row g-4">
          <div class="col-md-6">
            <div class="p-3 bg-light rounded-3 h-100">
              <h6 class="fw-bold mb-3">Thông tin người nhận</h6>
              <div class="small d-flex flex-column gap-2 text-muted-ps">
                <div><strong class="text-dark">Họ và tên:</strong> {{ currentResult.info?.name }}</div>
                <div><strong class="text-dark">Số điện thoại:</strong> {{ currentResult.info?.phone }}</div>
                <div><strong class="text-dark">Địa chỉ giao:</strong> {{ currentResult.info?.address }}</div>
                <div>
                  <strong class="text-dark">Hình thức thanh toán:</strong> 
                  {{ currentResult.info?.pay === 'bank' ? 'Chuyển khoản ngân hàng' : 'Thanh toán khi nhận hàng (COD)' }}
                </div>
              </div>
            </div>
          </div>
          <div class="col-md-6">
            <div class="p-3 bg-light rounded-3 h-100">
              <h6 class="fw-bold mb-3">Sản phẩm trong đơn</h6>
              <div class="d-flex flex-column gap-2 small">
                <div
                  v-for="item in currentResult.items"
                  :key="item.id + '-' + item.size"
                  class="d-flex justify-content-between align-items-center border-bottom pb-2"
                >
                  <div>
                    <span class="fw-bold text-dark">{{ item.name }}</span>
                    <span class="text-muted-ps ms-2">(Size {{ item.size }} × {{ item.qty }})</span>
                  </div>
                  <span class="price">{{ money(item.price * item.qty) }}</span>
                </div>
                <div class="d-flex justify-content-between pt-2 fw-bold fs-6">
                  <span>Tổng thanh toán:</span>
                  <span class="price fs-5">{{ money(currentResult.total) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Not Found -->
      <div v-else class="ps-card text-center p-5">
        <div class="display-3 mb-2">🔍</div>
        <h4 class="fw-bold mb-2">Không tìm thấy đơn hàng</h4>
        <p class="text-muted-ps mb-4" style="max-width: 450px; margin: 0 auto">
          Không tìm thấy đơn hàng với thông tin <strong>"{{ searchInput }}"</strong>. Vui lòng kiểm tra lại mã đơn hàng hoặc số điện thoại bạn đã dùng khi đặt hàng.
        </p>
        <button class="btn btn-outline-primary px-4" @click="searchInput = ''; searched = false">
          Thử lại
        </button>
      </div>
    </section>

    <!-- Tracking FAQs -->
    <section class="mb-5">
      <div class="text-center mb-4">
        <span class="ps-pill">Hỏi đáp</span>
        <h3 class="mt-2 mb-2">Câu hỏi thường gặp về giao hàng</h3>
      </div>
      <div class="row g-4">
        <div class="col-md-4">
          <div class="ps-card p-4 h-100">
            <h6 class="fw-bold mb-2">🚚 Thời gian nhận hàng mất bao lâu?</h6>
            <p class="text-muted-ps small mb-0" style="line-height: 1.6">
              Thời gian giao hàng tiêu chuẩn từ 1–3 ngày làm việc đối với các tỉnh thành trên toàn quốc, nội thành Hà Nội & TP.HCM từ 24h.
            </p>
          </div>
        </div>
        <div class="col-md-4">
          <div class="ps-card p-4 h-100">
            <h6 class="fw-bold mb-2">📦 Có được kiểm tra hàng trước không?</h6>
            <p class="text-muted-ps small mb-0" style="line-height: 1.6">
              Có. Poly Shoes 100% cho phép khách hàng mở hộp kiểm tra đúng mẫu, đúng size trước khi thanh toán cho shipper.
            </p>
          </div>
        </div>
        <div class="col-md-4">
          <div class="ps-card p-4 h-100">
            <h6 class="fw-bold mb-2">📞 Đổi thông tin nhận hàng bằng cách nào?</h6>
            <p class="text-muted-ps small mb-0" style="line-height: 1.6">
              Quý khách vui lòng gọi ngay hotline <strong>1900 1234</strong> kèm theo mã đơn hàng để nhân viên hỗ trợ cập nhật kịp thời.
            </p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
