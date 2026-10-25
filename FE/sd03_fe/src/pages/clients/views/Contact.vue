<script setup>
import { reactive, ref } from "vue";

const form = reactive({
  name: "",
  phone: "",
  email: "",
  topic: "size",
  message: "",
});

const errors = reactive({});
const sentSuccess = ref(false);

const submitContact = () => {
  errors.name = form.name.trim() ? "" : "Vui lòng nhập họ và tên của bạn.";
  errors.phone = /^0\d{9}$/.test(form.phone)
    ? ""
    : "Số điện thoại không hợp lệ (10 số, bắt đầu bằng số 0).";
  errors.message = form.message.trim() ? "" : "Vui lòng nhập nội dung cần hỗ trợ.";

  if (errors.name || errors.phone || errors.message) return;

  sentSuccess.value = true;
  form.name = "";
  form.phone = "";
  form.email = "";
  form.message = "";
  form.topic = "size";

  setTimeout(() => {
    sentSuccess.value = false;
  }, 6000);
};
</script>

<template>
  <div class="ps-contact-page">
    <!-- Breadcrumb -->
    <nav class="small mb-4 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <span class="text-dark fw-semibold">Liên hệ</span>
    </nav>

    <!-- Header Section -->
    <div class="text-center mb-5">
      <span class="ps-pill">Hỗ trợ khách hàng</span>
      <h2 class="display-6 mt-2 mb-2 fw-bold">Liên hệ với Poly Shoes</h2>
      <p class="text-muted-ps" style="max-width: 540px; margin: 0 auto">
        Chúng tôi luôn lắng nghe và sẵn sàng giải đáp mọi thắc mắc của bạn về sản phẩm, dịch vụ và đơn hàng.
      </p>
    </div>

    <!-- Quick Info Cards -->
    <div class="row g-4 mb-5">
      <div class="col-6 col-lg-3">
        <div class="ps-card p-4 h-100 text-center">
          <div
            class="d-inline-flex align-items-center justify-content-center rounded-circle text-danger mb-3"
            style="width: 52px; height: 52px; background: var(--ps-soft); font-size: 1.5rem"
          >
            <i class="bi bi-telephone-fill"></i>
          </div>
          <h6 class="fw-bold mb-1">Hotline tư vấn</h6>
          <div class="price fs-5 mb-1">1900 1234</div>
          <span class="small text-muted-ps">08:30 – 22:00 hàng ngày</span>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="ps-card p-4 h-100 text-center">
          <div
            class="d-inline-flex align-items-center justify-content-center rounded-circle text-danger mb-3"
            style="width: 52px; height: 52px; background: var(--ps-soft); font-size: 1.5rem"
          >
            <i class="bi bi-envelope-fill"></i>
          </div>
          <h6 class="fw-bold mb-1">Email hỗ trợ</h6>
          <div class="text-dark fw-semibold small mb-1">cskh@polyshoes.vn</div>
          <span class="small text-muted-ps">Phản hồi trong 2 giờ</span>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="ps-card p-4 h-100 text-center">
          <div
            class="d-inline-flex align-items-center justify-content-center rounded-circle text-danger mb-3"
            style="width: 52px; height: 52px; background: var(--ps-soft); font-size: 1.5rem"
          >
            <i class="bi bi-geo-alt-fill"></i>
          </div>
          <h6 class="fw-bold mb-1">Showroom chính</h6>
          <div class="text-dark fw-semibold small mb-1">123 Trịnh Văn Bô</div>
          <span class="small text-muted-ps">Nam Từ Liêm, Hà Nội</span>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="ps-card p-4 h-100 text-center">
          <div
            class="d-inline-flex align-items-center justify-content-center rounded-circle text-danger mb-3"
            style="width: 52px; height: 52px; background: var(--ps-soft); font-size: 1.5rem"
          >
            <i class="bi bi-clock-fill"></i>
          </div>
          <h6 class="fw-bold mb-1">Giờ mở cửa</h6>
          <div class="text-dark fw-semibold small mb-1">08:30 - 22:00</div>
          <span class="small text-muted-ps">Cả Thứ 7 & Chủ Nhật</span>
        </div>
      </div>
    </div>

    <!-- Main Contact Grid -->
    <div class="row g-4 mb-5">
      <!-- Contact Form -->
      <div class="col-lg-7">
        <div class="ps-card p-4 p-md-5 h-100">
          <h4 class="fw-bold mb-2">Gửi tin nhắn cho chúng tôi</h4>
          <p class="text-muted-ps small mb-4">
            Điền thông tin vào mẫu dưới đây, nhân viên chăm sóc khách hàng của Poly Shoes sẽ liên hệ hỗ trợ bạn sớm nhất.
          </p>

          <div v-if="sentSuccess" class="alert alert-success d-flex align-items-center gap-2 mb-4">
            <i class="bi bi-check-circle-fill fs-5"></i>
            <div>
              <strong>Gửi thành công!</strong> Cảm ơn bạn, Poly Shoes sẽ liên hệ hỗ trợ qua số điện thoại trong ít phút.
            </div>
          </div>

          <form @submit.prevent="submitContact">
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label fw-semibold small">Họ và tên *</label>
                <input
                  v-model="form.name"
                  class="form-control"
                  :class="{ 'is-invalid': errors.name }"
                  placeholder="Ví dụ: Nguyễn Văn A"
                />
                <div class="invalid-feedback">{{ errors.name }}</div>
              </div>
              <div class="col-md-6">
                <label class="form-label fw-semibold small">Số điện thoại *</label>
                <input
                  v-model="form.phone"
                  class="form-control"
                  :class="{ 'is-invalid': errors.phone }"
                  placeholder="Ví dụ: 0987654321"
                />
                <div class="invalid-feedback">{{ errors.phone }}</div>
              </div>
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label fw-semibold small">Email (tùy chọn)</label>
                <input
                  v-model="form.email"
                  type="email"
                  class="form-control"
                  placeholder="name@example.com"
                />
              </div>
              <div class="col-md-6">
                <label class="form-label fw-semibold small">Vấn đề cần hỗ trợ</label>
                <select v-model="form.topic" class="form-select">
                  <option value="size">Tư vấn chọn size & mẫu giày</option>
                  <option value="exchange">Yêu cầu đổi size / bảo hành</option>
                  <option value="order">Hỏi về tình trạng đơn hàng</option>
                  <option value="coop">Hợp tác kinh doanh / đại lý</option>
                  <option value="feedback">Góp ý & khiếu nại dịch vụ</option>
                </select>
              </div>
            </div>

            <div class="mb-4">
              <label class="form-label fw-semibold small">Nội dung tin nhắn *</label>
              <textarea
                v-model="form.message"
                rows="4"
                class="form-control"
                :class="{ 'is-invalid': errors.message }"
                placeholder="Nhập chi tiết yêu cầu của bạn để chúng tôi phục vụ tốt nhất..."
              ></textarea>
              <div class="invalid-feedback">{{ errors.message }}</div>
            </div>

            <button type="submit" class="btn btn-primary btn-lg px-5">
              Gửi thông tin liên hệ
            </button>
          </form>
        </div>
      </div>

      <!-- FAQ & Map Info Side -->
      <div class="col-lg-5">
        <div class="ps-card p-4 p-md-5 h-100 d-flex flex-column justify-content-between">
          <div>
            <h4 class="fw-bold mb-3">Bạn cần hỗ trợ gấp?</h4>
            <p class="text-muted-ps small mb-4">
              Nếu bạn có câu hỏi cấp bách về đơn hàng đang giao hoặc muốn giữ size giày hot, hãy gọi trực tiếp qua đường dây nóng:
            </p>

            <div class="p-3 rounded-3 bg-light border mb-4">
              <div class="d-flex align-items-center gap-3">
                <div
                  class="d-flex align-items-center justify-content-center rounded-circle bg-danger text-white flex-shrink-0"
                  style="width: 48px; height: 48px; font-size: 1.4rem"
                >
                  <i class="bi bi-headset"></i>
                </div>
                <div>
                  <span class="small text-muted-ps d-block">Tổng đài miễn cước cuộc gọi</span>
                  <span class="price fs-4">1900 1234</span>
                </div>
              </div>
            </div>

            <h6 class="fw-bold mb-2">Hệ thống mạng xã hội:</h6>
            <p class="text-muted-ps small mb-3">
              Theo dõi fanpage để cập nhật mẫu giày mới và săn voucher giảm giá hàng tuần:
            </p>
            <div class="d-flex flex-wrap gap-2 mb-4">
              <a href="javascript:void(0)" class="btn btn-sm btn-light-border px-3">
                <i class="bi bi-facebook text-primary me-1"></i> Fanpage Poly Shoes
              </a>
              <a href="javascript:void(0)" class="btn btn-sm btn-light-border px-3">
                <i class="bi bi-instagram text-danger me-1"></i> @polyshoes_official
              </a>
              <a href="javascript:void(0)" class="btn btn-sm btn-light-border px-3">
                <i class="bi bi-tiktok text-dark me-1"></i> TikTok Poly Shoes
              </a>
            </div>
          </div>

          <div class="border-top pt-3 text-muted-ps small">
            <i class="bi bi-info-circle me-1"></i> Poly Shoes cam kết bảo mật 100% thông tin cá nhân của khách hàng.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
