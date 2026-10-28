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
      (o) => o.info?.phone === query || o.id === cleanCode,
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
  <div class="ol-page">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      /
      <span class="text-dark fw-semibold">Tra cứu đơn hàng</span>
    </nav>

    <!-- ================= HERO TÌM KIẾM ================= -->
    <section class="ol-hero mb-5">
      <div class="ol-hero-shape" aria-hidden="true">
        <i class="bi bi-box-seam"></i>
      </div>

      <div class="ol-hero-body">
        <div class="ol-hero-eyebrow">Kiểm tra tiến độ đơn hàng</div>
        <h1 class="ol-hero-title">
          Theo dõi đơn hàng<br />
          <span>cùng Poly Shoes</span>
        </h1>
        <p class="ol-hero-sub">
          Nhập mã đơn hàng (ví dụ: <strong>HD123456</strong>) hoặc số điện thoại
          đặt hàng để xem tình trạng vận chuyển và chi tiết đơn.
        </p>

        <!-- Ô tìm kiếm -->
        <div class="ol-search">
          <i class="bi bi-search ol-search-ico"></i>
          <input
            v-model="searchInput"
            class="ol-search-input"
            placeholder="Nhập mã đơn hàng hoặc số điện thoại..."
            @keyup.enter="doSearch()"
          />
          <button class="btn btn-primary ol-search-btn" @click="doSearch()">
            Tra cứu ngay
          </button>
        </div>

        <div v-if="errorMessage" class="ol-error">
          <i class="bi bi-exclamation-circle me-1"></i>{{ errorMessage }}
        </div>

        <!-- Đơn gần đây -->
        <div v-if="recentOrders.length" class="ol-recent">
          <span class="ol-recent-label">Đơn bạn đã đặt gần đây:</span>
          <div class="d-flex flex-wrap gap-2 mt-2">
            <button
              v-for="o in recentOrders.slice(-4)"
              :key="o.id"
              type="button"
              class="ol-chip"
              @click="doSearch(o.id)"
            >
              <i class="bi bi-receipt me-1"></i>#{{ o.id }} ({{
                o.info?.name || "Khách"
              }})
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= DANH SÁCH ĐƠN ĐÃ ĐẶT ================= -->
    <section v-if="!searched && recentOrders.length" class="mb-5">
      <div class="ol-head">
        <div class="ol-eyebrow">/ Lịch sử đặt hàng</div>
        <h3 class="ol-h">Đơn hàng bạn đã đặt ({{ recentOrders.length }})</h3>
        <p class="text-muted-ps small mb-0">
          Nhấn theo dõi để xem lộ trình hoặc mở chi tiết từng đơn hàng.
        </p>
      </div>

      <div class="d-flex flex-column gap-3">
        <div v-for="o in recentOrders" :key="o.id" class="ps-card ol-order">
          <!-- Đầu thẻ -->
          <div class="ol-order-top">
            <div>
              <div class="d-flex align-items-center flex-wrap gap-2">
                <span class="ol-order-code">Đơn hàng #{{ o.id }}</span>
                <span class="ol-status"
                  ><i class="bi bi-dot"></i>Đang xử lý</span
                >
              </div>
              <div class="text-muted-ps small mt-1">
                <i class="bi bi-clock me-1"></i>Ngày đặt: {{ o.date }}
              </div>
            </div>
            <div class="text-sm-end">
              <div class="text-muted-ps small">Tổng tiền</div>
              <span class="price fs-5">{{ money(o.total) }}</span>
            </div>
          </div>

          <!-- Sản phẩm -->
          <div class="d-flex flex-column gap-3 my-3">
            <div
              v-for="item in o.items"
              :key="item.id + '-' + item.size"
              class="d-flex align-items-center gap-3"
            >
              <div class="ps-thumb flex-shrink-0 ol-thumb">
                {{ item.emoji || "👟" }}
              </div>
              <div class="flex-grow-1">
                <div class="fw-semibold text-dark small">{{ item.name }}</div>
                <div class="small text-muted-ps">
                  {{ item.brand }} ·
                  <span class="ol-tag">Size {{ item.size }}</span> · SL:
                  {{ item.qty }}
                </div>
              </div>
              <span class="fw-semibold text-dark small">{{
                money(item.price * item.qty)
              }}</span>
            </div>
          </div>

          <!-- Chân thẻ -->
          <div class="ol-order-foot">
            <div class="small text-muted-ps">
              <i class="bi bi-geo-alt-fill text-primary me-1"></i>
              <span class="fw-semibold text-dark">{{ o.info?.name }}</span> ·
              {{ o.info?.phone }} ·
              <span class="text-truncate d-inline-block align-bottom ol-addr">
                {{ o.info?.address }}
              </span>
            </div>
            <div class="d-flex gap-2 flex-shrink-0">
              <button
                class="btn btn-outline-primary btn-sm ol-pill-btn"
                @click="doSearch(o.id)"
              >
                <i class="bi bi-search me-1"></i>Theo dõi tiến độ
              </button>
              <RouterLink
                :to="`/don-hang/${o.id}`"
                class="btn btn-primary btn-sm ol-pill-btn"
              >
                Chi tiết
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= KẾT QUẢ TRA CỨU ================= -->
    <section v-if="searched" class="mb-5">
      <button
        class="ol-back"
        @click="
          searched = false;
          searchInput = '';
        "
      >
        <i class="bi bi-arrow-left me-1"></i>Quay lại danh sách đơn hàng
      </button>

      <div v-if="currentResult" class="ps-card ol-result">
        <!-- Đầu kết quả -->
        <div class="ol-result-top">
          <div>
            <div class="d-flex align-items-center flex-wrap gap-2">
              <h3 class="mb-0 fw-bold">Đơn hàng #{{ currentResult.id }}</h3>
              <span class="ol-status"><i class="bi bi-dot"></i>Đang xử lý</span>
            </div>
            <div class="text-muted-ps small mt-1">
              Thời gian đặt hàng: {{ currentResult.date }}
            </div>
          </div>
          <RouterLink
            :to="`/don-hang/${currentResult.id}`"
            class="btn btn-outline-primary ol-pill-btn px-4"
          >
            Xem chi tiết đầy đủ
          </RouterLink>
        </div>

        <!-- Tiến độ giao hàng -->
        <div class="ol-steps">
          <div class="ol-step done">
            <div class="ol-step-dot"><i class="bi bi-check-lg"></i></div>
            <div class="ol-step-name">Đã đặt hàng</div>
            <div class="ol-step-note">{{ currentResult.date }}</div>
          </div>
          <div class="ol-step current">
            <div class="ol-step-dot"><i class="bi bi-box-seam"></i></div>
            <div class="ol-step-name">Đã xác nhận</div>
            <div class="ol-step-note">Đang đóng gói</div>
          </div>
          <div class="ol-step">
            <div class="ol-step-dot"><i class="bi bi-truck"></i></div>
            <div class="ol-step-name">Đang vận chuyển</div>
            <div class="ol-step-note">Dự kiến 1-2 ngày</div>
          </div>
          <div class="ol-step">
            <div class="ol-step-dot"><i class="bi bi-house-check"></i></div>
            <div class="ol-step-name">Giao thành công</div>
            <div class="ol-step-note">Nhận hàng &amp; kiểm tra</div>
          </div>
        </div>

        <!-- Thông tin & sản phẩm -->
        <div class="row g-4">
          <div class="col-md-6">
            <div class="ol-box h-100">
              <h6 class="ol-box-title">
                <i class="bi bi-person-lines-fill"></i>Thông tin người nhận
              </h6>
              <div class="ol-kv">
                <span>Họ và tên</span
                ><strong>{{ currentResult.info?.name }}</strong>
              </div>
              <div class="ol-kv">
                <span>Số điện thoại</span
                ><strong>{{ currentResult.info?.phone }}</strong>
              </div>
              <div class="ol-kv">
                <span>Địa chỉ giao</span
                ><strong>{{ currentResult.info?.address }}</strong>
              </div>
              <div class="ol-kv">
                <span>Thanh toán</span>
                <strong>
                  {{
                    currentResult.info?.pay === "bank"
                      ? "Chuyển khoản ngân hàng"
                      : "Thanh toán khi nhận hàng (COD)"
                  }}
                </strong>
              </div>
            </div>
          </div>
          <div class="col-md-6">
            <div class="ol-box h-100">
              <h6 class="ol-box-title">
                <i class="bi bi-bag-check"></i>Sản phẩm trong đơn
              </h6>
              <div
                v-for="item in currentResult.items"
                :key="item.id + '-' + item.size"
                class="ol-line"
              >
                <div class="small">
                  <span class="fw-semibold text-dark">{{ item.name }}</span>
                  <span class="text-muted-ps ms-2"
                    >(Size {{ item.size }} × {{ item.qty }})</span
                  >
                </div>
                <span class="price small">{{
                  money(item.price * item.qty)
                }}</span>
              </div>
              <div class="ol-sum">
                <span class="fw-bold">Tổng thanh toán</span>
                <span class="price fs-5">{{ money(currentResult.total) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Không tìm thấy -->
      <div v-else class="ps-card text-center p-5">
        <div class="ol-empty-ico"><i class="bi bi-search"></i></div>
        <h4 class="fw-bold mb-2">Không tìm thấy đơn hàng</h4>
        <p class="text-muted-ps mb-4 mx-auto" style="max-width: 450px">
          Không có đơn hàng nào khớp với <strong>"{{ searchInput }}"</strong>.
          Vui lòng kiểm tra lại mã đơn hàng hoặc số điện thoại bạn đã dùng khi
          đặt hàng.
        </p>
        <button
          class="btn btn-primary ol-pill-btn px-4"
          @click="
            searchInput = '';
            searched = false;
          "
        >
          Thử lại
        </button>
      </div>
    </section>

    <!-- ================= HỎI ĐÁP ================= -->
    <section class="mb-5">
      <div class="ol-head">
        <div class="ol-eyebrow">/ Hỏi đáp</div>
        <h3 class="ol-h">Câu hỏi thường gặp về giao hàng</h3>
      </div>
      <div class="row g-3">
        <div class="col-md-4">
          <div class="ps-card ol-faq h-100">
            <div class="ol-faq-ico"><i class="bi bi-truck"></i></div>
            <h6 class="fw-bold mb-2">Thời gian nhận hàng mất bao lâu?</h6>
            <p class="text-muted-ps small mb-0">
              Giao hàng tiêu chuẩn từ 1–3 ngày làm việc trên toàn quốc, nội
              thành Hà Nội &amp; TP.HCM từ 24h.
            </p>
          </div>
        </div>
        <div class="col-md-4">
          <div class="ps-card ol-faq h-100">
            <div class="ol-faq-ico"><i class="bi bi-box-seam"></i></div>
            <h6 class="fw-bold mb-2">Có được kiểm tra hàng trước không?</h6>
            <p class="text-muted-ps small mb-0">
              Có. Poly Shoes cho phép khách mở hộp kiểm tra đúng mẫu, đúng size
              trước khi thanh toán cho shipper.
            </p>
          </div>
        </div>
        <div class="col-md-4">
          <div class="ps-card ol-faq h-100">
            <div class="ol-faq-ico"><i class="bi bi-headset"></i></div>
            <h6 class="fw-bold mb-2">Đổi thông tin nhận hàng bằng cách nào?</h6>
            <p class="text-muted-ps small mb-0">
              Vui lòng gọi hotline <strong>1900 1234</strong> kèm mã đơn hàng để
              nhân viên cập nhật kịp thời.
            </p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Màu chủ đạo bám theo theme của dự án (đỏ - trắng) */
.ol-page {
  --ol-red: var(--bs-primary, #d62828);
  --ol-red-soft: rgba(var(--bs-primary-rgb, 214, 40, 40), 0.08);
}

/* ---------- Tiêu đề section (giống trang chủ) ---------- */
.ol-head {
  margin-bottom: 1.25rem;
}
.ol-eyebrow {
  color: var(--ol-red);
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.ol-h {
  font-weight: 600;
  margin: 0.15rem 0 0.3rem;
}

/* ---------- Hero tối như banner trang chủ ---------- */
.ol-hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 3.25rem 2.75rem;
  color: #fff;
  background: linear-gradient(115deg, #2a0508 0%, #12090a 55%, #0b0b0b 100%);
}
.ol-hero-shape {
  position: absolute;
  top: -30%;
  right: -6%;
  width: 46%;
  height: 160%;
  display: grid;
  place-items: center;
  background: linear-gradient(140deg, #ef2b3a 0%, #8f0f1b 100%);
  transform: rotate(14deg);
  opacity: 0.95;
}
.ol-hero-shape i {
  font-size: 11rem;
  color: rgba(255, 255, 255, 0.16);
  transform: rotate(-14deg);
}
.ol-hero-body {
  position: relative;
  z-index: 1;
  max-width: 620px;
}
.ol-hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.8);
}
.ol-hero-eyebrow::after {
  content: "";
  width: 36px;
  height: 2px;
  background: var(--ol-red);
}
.ol-hero-title {
  margin: 0 0 0.75rem;
  font-size: clamp(1.9rem, 4.2vw, 3rem);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.02em;
  text-transform: uppercase;
}
.ol-hero-title span {
  color: #ff3b47;
}
.ol-hero-sub {
  margin-bottom: 1.5rem;
  max-width: 520px;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.72);
}
.ol-hero-sub strong {
  color: #fff;
}

.ol-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.4rem 0.4rem 1.1rem;
  background: #fff;
  border-radius: 999px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
}
.ol-search-ico {
  color: #9ca3af;
}
.ol-search-input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  padding: 0.55rem 0.25rem;
  font-size: 0.95rem;
  color: #111;
}
.ol-search-btn {
  border-radius: 999px;
  padding: 0.65rem 1.5rem;
  font-weight: 700;
  white-space: nowrap;
}
.ol-error {
  margin-top: 0.75rem;
  font-size: 0.85rem;
  color: #ffb4b9;
}
.ol-recent {
  margin-top: 1.5rem;
  padding-top: 1.1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}
.ol-recent-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
}
.ol-chip {
  padding: 0.4rem 0.9rem;
  font-size: 0.8rem;
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  transition:
    background-color 0.15s,
    border-color 0.15s;
}
.ol-chip:hover {
  background: var(--ol-red);
  border-color: var(--ol-red);
}

/* ---------- Thẻ đơn hàng ---------- */
.ol-order {
  padding: 1.4rem 1.5rem;
  border-radius: 16px;
}
.ol-order-top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}
.ol-order-code {
  font-size: 1.1rem;
  font-weight: 700;
}
.ol-status {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.8rem 0.15rem 0.3rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: #15803d;
  background: #ecfdf3;
  border-radius: 999px;
}
.ol-status .bi {
  font-size: 1.3rem;
  line-height: 1;
}
.ol-thumb {
  width: 54px;
  height: 54px;
  font-size: 1.6rem;
  border-radius: 12px;
}
.ol-tag {
  padding: 0.05rem 0.5rem;
  font-size: 0.75rem;
  background: #f3f4f6;
  border-radius: 6px;
}
.ol-order-foot {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.9rem 1rem;
  background: var(--ol-red-soft);
  border-radius: 12px;
}
.ol-addr {
  max-width: 320px;
}
.ol-pill-btn {
  border-radius: 999px;
  font-weight: 600;
  padding-inline: 1rem;
}

/* ---------- Kết quả tra cứu ---------- */
.ol-back {
  margin-bottom: 1rem;
  padding: 0.4rem 1rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  transition:
    border-color 0.15s,
    color 0.15s;
}
.ol-back:hover {
  color: var(--ol-red);
  border-color: var(--ol-red);
}
.ol-result {
  padding: 2rem;
  border-radius: 18px;
}
.ol-result-top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding-bottom: 1.5rem;
  margin-bottom: 1.75rem;
  border-bottom: 1px solid #eee;
}

/* Thanh tiến độ có đường nối */
.ol-steps {
  display: flex;
  margin-bottom: 2.25rem;
}
.ol-step {
  position: relative;
  flex: 1;
  text-align: center;
  padding: 0 0.25rem;
}
.ol-step:not(:last-child)::after {
  content: "";
  position: absolute;
  top: 22px;
  left: calc(50% + 30px);
  right: calc(-50% + 30px);
  height: 3px;
  border-radius: 3px;
  background: #e5e7eb;
}
.ol-step.done:not(:last-child)::after {
  background: var(--ol-red);
}
.ol-step-dot {
  position: relative;
  z-index: 1;
  width: 46px;
  height: 46px;
  margin: 0 auto 0.6rem;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  color: #9ca3af;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
}
.ol-step.done .ol-step-dot {
  color: #fff;
  background: var(--ol-red);
  border-color: var(--ol-red);
}
.ol-step.current .ol-step-dot {
  color: var(--ol-red);
  background: #fff;
  border: 2px solid var(--ol-red);
  box-shadow: 0 0 0 5px var(--ol-red-soft);
}
.ol-step-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #9ca3af;
}
.ol-step.done .ol-step-name,
.ol-step.current .ol-step-name {
  color: #111;
}
.ol-step-note {
  font-size: 0.75rem;
  color: #6b7280;
}

/* Hộp thông tin */
.ol-box {
  padding: 1.25rem;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
}
.ol-box-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  font-weight: 700;
}
.ol-box-title .bi {
  color: var(--ol-red);
}
.ol-kv {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.55rem 0;
  font-size: 0.88rem;
  border-bottom: 1px dashed #e5e7eb;
}
.ol-kv:last-child {
  border-bottom: 0;
}
.ol-kv span {
  flex-shrink: 0;
  color: #6b7280;
}
.ol-kv strong {
  text-align: right;
  color: #111;
}
.ol-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.55rem 0;
  border-bottom: 1px dashed #e5e7eb;
}
.ol-sum {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.85rem;
  padding-top: 0.85rem;
  border-top: 1px solid #e5e7eb;
}

/* Không tìm thấy */
.ol-empty-ico {
  width: 72px;
  height: 72px;
  margin: 0 auto 1rem;
  display: grid;
  place-items: center;
  font-size: 1.9rem;
  color: var(--ol-red);
  background: var(--ol-red-soft);
  border-radius: 50%;
}

/* ---------- FAQ ---------- */
.ol-faq {
  padding: 1.4rem;
  border-radius: 16px;
}
.ol-faq-ico {
  width: 46px;
  height: 46px;
  margin-bottom: 0.9rem;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: var(--ol-red);
  background: var(--ol-red-soft);
  border-radius: 12px;
}

/* ---------- Responsive ---------- */
@media (max-width: 991.98px) {
  .ol-hero {
    padding: 2.25rem 1.5rem;
  }
  .ol-hero-shape {
    width: 70%;
    right: -30%;
    opacity: 0.5;
  }
}
@media (max-width: 575.98px) {
  .ol-search {
    flex-wrap: wrap;
    border-radius: 18px;
    padding: 0.6rem;
  }
  .ol-search-btn {
    width: 100%;
  }
  .ol-result {
    padding: 1.25rem;
  }
  .ol-step:not(:last-child)::after {
    left: calc(50% + 26px);
    right: calc(-50% + 26px);
  }
  .ol-step-name {
    font-size: 0.75rem;
  }
  .ol-step-note {
    display: none;
  }
}
</style>
