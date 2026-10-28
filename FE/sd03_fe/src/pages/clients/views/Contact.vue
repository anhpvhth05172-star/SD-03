<script setup>
import { reactive, ref } from "vue";

const infoCards = [
  {
    icon: "bi-telephone-fill",
    title: "Hotline tư vấn",
    main: "1900 1234",
    sub: "08:30 – 22:00 hằng ngày",
    href: "tel:19001234",
  },
  {
    icon: "bi-envelope-fill",
    title: "Email hỗ trợ",
    main: "cskh@polyshoes.vn",
    sub: "Phản hồi trong 2 giờ",
    href: "mailto:cskh@polyshoes.vn",
  },
  {
    icon: "bi-geo-alt-fill",
    title: "Showroom chính",
    main: "123 Trịnh Văn Bô",
    sub: "Nam Từ Liêm, Hà Nội",
    href: "https://www.google.com/maps/search/?api=1&query=123%20Tr%E1%BB%8Bnh%20V%C4%83n%20B%C3%B4%2C%20Nam%20T%E1%BB%AB%20Li%C3%AAm%2C%20H%C3%A0%20N%E1%BB%99i",
    external: true,
  },
  {
    icon: "bi-clock-fill",
    title: "Giờ mở cửa",
    main: "08:30 – 22:00",
    sub: "Cả Thứ 7 và Chủ nhật",
  },
];

const topics = [
  { value: "size", label: "Tư vấn chọn size và mẫu giày" },
  { value: "exchange", label: "Yêu cầu đổi size / bảo hành" },
  { value: "order", label: "Hỏi về tình trạng đơn hàng" },
  { value: "coop", label: "Hợp tác kinh doanh / đại lý" },
  { value: "feedback", label: "Góp ý và khiếu nại dịch vụ" },
];

const socials = [
  { icon: "bi-facebook", color: "text-primary", label: "Fanpage Poly Shoes" },
  { icon: "bi-instagram", color: "text-danger", label: "@polyshoes_official" },
  { icon: "bi-tiktok", color: "text-dark", label: "TikTok Poly Shoes" },
];

const form = reactive({
  name: "",
  phone: "",
  email: "",
  topic: "size",
  message: "",
});

const errors = reactive({ name: "", phone: "", message: "" });
const sentSuccess = ref(false);
let timer = null;

const submitContact = () => {
  errors.name = form.name.trim() ? "" : "Vui lòng nhập họ và tên của bạn.";
  errors.phone = /^0\d{9}$/.test(form.phone.trim())
    ? ""
    : "Số điện thoại không hợp lệ (10 số, bắt đầu bằng số 0).";
  errors.message = form.message.trim()
    ? ""
    : "Vui lòng nhập nội dung cần hỗ trợ.";

  if (errors.name || errors.phone || errors.message) return;

  sentSuccess.value = true;
  form.name = "";
  form.phone = "";
  form.email = "";
  form.message = "";
  form.topic = "size";

  clearTimeout(timer);
  timer = setTimeout(() => {
    sentSuccess.value = false;
  }, 6000);
};
</script>

<template>
  <div class="ct-page">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      /
      <span class="text-dark fw-semibold">Liên hệ</span>
    </nav>

    <!-- ================= HERO ================= -->
    <section class="ct-hero mb-4">
      <div class="ct-hero-shape" aria-hidden="true">
        <i class="bi bi-chat-dots"></i>
      </div>

      <div class="ct-hero-body">
        <div class="ct-hero-eyebrow">Hỗ trợ khách hàng</div>
        <h1 class="ct-hero-title">
          Liên hệ với<br />
          <span>Poly Shoes</span>
        </h1>
        <p class="ct-hero-sub">
          Chúng tôi luôn lắng nghe và sẵn sàng giải đáp mọi thắc mắc của bạn về
          sản phẩm, dịch vụ và đơn hàng.
        </p>
        <div class="d-flex flex-wrap gap-2">
          <a href="tel:19001234" class="btn btn-primary ct-pill-btn px-4">
            <i class="bi bi-telephone-fill me-2"></i>Gọi 1900 1234
          </a>
          <RouterLink to="/tra-cuu" class="btn ct-ghost-btn px-4">
            Tra cứu đơn hàng
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- ================= THÔNG TIN NHANH ================= -->
    <section class="row g-3 mb-5">
      <div v-for="c in infoCards" :key="c.title" class="col-6 col-lg-3">
        <component
          :is="c.href ? 'a' : 'div'"
          :href="c.href"
          :target="c.external ? '_blank' : undefined"
          :rel="c.external ? 'noopener' : undefined"
          class="ps-card ct-info h-100"
          :class="{ 'ct-info-link': c.href }"
        >
          <div class="ct-ico"><i :class="'bi ' + c.icon"></i></div>
          <div class="min-w-0">
            <div class="ct-info-title">{{ c.title }}</div>
            <div class="ct-info-main">{{ c.main }}</div>
            <div class="ct-info-sub">{{ c.sub }}</div>
          </div>
        </component>
      </div>
    </section>

    <!-- ================= FORM + HỖ TRỢ GẤP ================= -->
    <section class="row g-4 mb-5">
      <!-- Form -->
      <div class="col-lg-7">
        <div class="ps-card ct-form h-100">
          <div class="ct-eyebrow">/ Gửi tin nhắn</div>
          <h3 class="ct-h">Để lại lời nhắn cho chúng tôi</h3>
          <p class="text-muted-ps small mb-4">
            Điền thông tin vào mẫu dưới đây, nhân viên chăm sóc khách hàng sẽ
            liên hệ hỗ trợ bạn sớm nhất.
          </p>

          <div v-if="sentSuccess" class="ct-success" role="status">
            <i class="bi bi-check-circle-fill"></i>
            <div>
              <strong>Gửi thành công!</strong> Cảm ơn bạn, Poly Shoes sẽ liên hệ
              qua số điện thoại trong ít phút.
            </div>
          </div>

          <form novalidate @submit.prevent="submitContact">
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label for="ct-name" class="form-label ct-label"
                  >Họ và tên *</label
                >
                <input
                  id="ct-name"
                  v-model="form.name"
                  class="form-control ct-input"
                  :class="{ 'is-invalid': errors.name }"
                  placeholder="Ví dụ: Nguyễn Văn A"
                  autocomplete="name"
                  @input="errors.name = ''"
                />
                <div class="invalid-feedback">{{ errors.name }}</div>
              </div>
              <div class="col-md-6">
                <label for="ct-phone" class="form-label ct-label"
                  >Số điện thoại *</label
                >
                <input
                  id="ct-phone"
                  v-model="form.phone"
                  class="form-control ct-input"
                  :class="{ 'is-invalid': errors.phone }"
                  placeholder="Ví dụ: 0987654321"
                  inputmode="tel"
                  autocomplete="tel"
                  @input="errors.phone = ''"
                />
                <div class="invalid-feedback">{{ errors.phone }}</div>
              </div>
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label for="ct-email" class="form-label ct-label"
                  >Email (tùy chọn)</label
                >
                <input
                  id="ct-email"
                  v-model="form.email"
                  type="email"
                  class="form-control ct-input"
                  placeholder="name@example.com"
                  autocomplete="email"
                />
              </div>
              <div class="col-md-6">
                <label for="ct-topic" class="form-label ct-label"
                  >Vấn đề cần hỗ trợ</label
                >
                <select
                  id="ct-topic"
                  v-model="form.topic"
                  class="form-select ct-input"
                >
                  <option v-for="t in topics" :key="t.value" :value="t.value">
                    {{ t.label }}
                  </option>
                </select>
              </div>
            </div>

            <div class="mb-4">
              <label for="ct-message" class="form-label ct-label"
                >Nội dung tin nhắn *</label
              >
              <textarea
                id="ct-message"
                v-model="form.message"
                rows="5"
                class="form-control ct-input"
                :class="{ 'is-invalid': errors.message }"
                placeholder="Nhập chi tiết yêu cầu của bạn để chúng tôi phục vụ tốt nhất..."
                @input="errors.message = ''"
              ></textarea>
              <div class="invalid-feedback">{{ errors.message }}</div>
            </div>

            <button type="submit" class="btn btn-primary ct-pill-btn px-4 py-2">
              <i class="bi bi-send me-2"></i>Gửi thông tin liên hệ
            </button>
          </form>
        </div>
      </div>

      <!-- Hỗ trợ gấp -->
      <div class="col-lg-5">
        <div class="d-flex flex-column gap-4 h-100">
          <!-- Thẻ hotline tối -->
          <div class="ct-hotline">
            <div class="ct-hotline-shape" aria-hidden="true"></div>
            <div class="position-relative">
              <div class="ct-hero-eyebrow mb-2">Cần hỗ trợ gấp?</div>
              <p class="ct-hotline-text">
                Có câu hỏi về đơn hàng đang giao hoặc muốn giữ size giày hot?
                Hãy gọi trực tiếp cho chúng tôi.
              </p>
              <div class="d-flex align-items-center gap-3">
                <div class="ct-hotline-ico"><i class="bi bi-headset"></i></div>
                <div>
                  <div class="ct-hotline-label">Tổng đài miễn cước</div>
                  <a href="tel:19001234" class="ct-hotline-num">1900 1234</a>
                </div>
              </div>
            </div>
          </div>

          <!-- Mạng xã hội -->
          <div class="ps-card ct-social flex-grow-1">
            <h6 class="fw-bold mb-1">Theo dõi Poly Shoes</h6>
            <p class="text-muted-ps small mb-3">
              Cập nhật mẫu giày mới và săn voucher giảm giá hằng tuần.
            </p>
            <div class="d-flex flex-wrap gap-2">
              <a
                v-for="s in socials"
                :key="s.label"
                href="#"
                class="ct-chip"
                @click.prevent
              >
                <i :class="['bi', s.icon, s.color]"></i>{{ s.label }}
              </a>
            </div>

            <div class="ct-note">
              <i class="bi bi-shield-lock"></i>
              Poly Shoes cam kết bảo mật 100% thông tin cá nhân của khách hàng.
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Màu chủ đạo bám theo theme của dự án (đỏ - trắng) */
.ct-page {
  --ct-red: var(--bs-primary, #d62828);
  --ct-red-soft: rgba(var(--bs-primary-rgb, 214, 40, 40), 0.08);
}

/* ---------- Tiêu đề section ---------- */
.ct-eyebrow {
  color: var(--ct-red);
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.ct-h {
  font-weight: 600;
  margin: 0.15rem 0 0.4rem;
}

/* ---------- Hero ---------- */
.ct-hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 3.25rem 2.75rem;
  color: #fff;
  background: linear-gradient(115deg, #2a0508 0%, #12090a 55%, #0b0b0b 100%);
}
.ct-hero-shape {
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
.ct-hero-shape i {
  font-size: 11rem;
  color: rgba(255, 255, 255, 0.16);
  transform: rotate(-14deg);
}
.ct-hero-body {
  position: relative;
  z-index: 1;
  max-width: 560px;
}
.ct-hero-eyebrow {
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
.ct-hero-eyebrow::after {
  content: "";
  width: 36px;
  height: 2px;
  background: var(--ct-red);
}
.ct-hero-title {
  margin: 0 0 0.75rem;
  font-size: clamp(1.9rem, 4.2vw, 3rem);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.02em;
  text-transform: uppercase;
}
.ct-hero-title span {
  color: #ff3b47;
}
.ct-hero-sub {
  margin-bottom: 1.5rem;
  max-width: 500px;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.72);
}
.ct-pill-btn {
  border-radius: 999px;
  font-weight: 600;
}
.ct-ghost-btn {
  border-radius: 999px;
  font-weight: 600;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.06);
}
.ct-ghost-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.6);
}

/* ---------- Thẻ thông tin nhanh ---------- */
.ct-info {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  padding: 1.2rem 1.25rem;
  border-radius: 16px;
  color: inherit;
  text-decoration: none;
  transition:
    border-color 0.15s,
    transform 0.15s;
}
.ct-info-link:hover {
  border-color: var(--ct-red);
  transform: translateY(-2px);
  color: inherit;
}
.min-w-0 {
  min-width: 0;
}
.ct-ico {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  color: var(--ct-red);
  background: var(--ct-red-soft);
  border-radius: 12px;
}
.ct-info-title {
  font-size: 0.78rem;
  font-weight: 600;
  color: #6b7280;
}
.ct-info-main {
  font-size: 0.95rem;
  font-weight: 700;
  line-height: 1.3;
  color: #111;
  overflow-wrap: anywhere;
}
.ct-info-sub {
  font-size: 0.78rem;
  color: #6b7280;
}

/* ---------- Form ---------- */
.ct-form {
  padding: 2rem;
  border-radius: 18px;
}
.ct-label {
  margin-bottom: 0.35rem;
  font-size: 0.82rem;
  font-weight: 600;
}
.ct-input {
  padding: 0.65rem 0.9rem;
  font-size: 0.92rem;
  background-color: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition:
    border-color 0.15s,
    box-shadow 0.15s,
    background-color 0.15s;
}
.ct-input:focus {
  background-color: #fff;
  border-color: var(--ct-red);
  box-shadow: 0 0 0 4px var(--ct-red-soft);
}
.ct-success {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  padding: 0.9rem 1rem;
  font-size: 0.88rem;
  color: #15803d;
  background: #ecfdf3;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
}
.ct-success i {
  margin-top: 0.1rem;
  font-size: 1.2rem;
}

/* ---------- Thẻ hotline tối ---------- */
.ct-hotline {
  position: relative;
  overflow: hidden;
  padding: 1.75rem;
  color: #fff;
  background: linear-gradient(115deg, #2a0508 0%, #12090a 60%, #0b0b0b 100%);
  border-radius: 18px;
}
.ct-hotline-shape {
  position: absolute;
  top: -40%;
  right: -22%;
  width: 55%;
  height: 190%;
  background: linear-gradient(140deg, #ef2b3a 0%, #8f0f1b 100%);
  transform: rotate(14deg);
  opacity: 0.85;
}
.ct-hotline-text {
  max-width: 320px;
  margin-bottom: 1.1rem;
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.72);
}
.ct-hotline-ico {
  flex-shrink: 0;
  width: 50px;
  height: 50px;
  display: grid;
  place-items: center;
  font-size: 1.4rem;
  background: var(--ct-red);
  border-radius: 50%;
  box-shadow: 0 0 0 5px rgba(255, 255, 255, 0.12);
}
.ct-hotline-label {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.7);
}
.ct-hotline-num {
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: 0.01em;
  color: #fff;
  text-decoration: none;
}
.ct-hotline-num:hover {
  color: #fff;
  text-decoration: underline;
}

/* ---------- Mạng xã hội ---------- */
.ct-social {
  display: flex;
  flex-direction: column;
  padding: 1.5rem;
  border-radius: 18px;
}
.ct-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.4rem 0.95rem;
  font-size: 0.82rem;
  font-weight: 600;
  color: #374151;
  text-decoration: none;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  transition:
    border-color 0.15s,
    color 0.15s;
}
.ct-chip:hover {
  color: var(--ct-red);
  border-color: var(--ct-red);
}
.ct-note {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-top: auto;
  padding: 0.8rem 1rem;
  font-size: 0.8rem;
  color: #6b7280;
  background: var(--ct-red-soft);
  border-radius: 12px;
}
.ct-social .d-flex + .ct-note {
  margin-top: 1.25rem;
}
.ct-note i {
  margin-top: 0.1rem;
  color: var(--ct-red);
}

/* ---------- Responsive ---------- */
@media (max-width: 991.98px) {
  .ct-hero {
    padding: 2.25rem 1.5rem;
  }
  .ct-hero-shape {
    width: 70%;
    right: -30%;
    opacity: 0.5;
  }
}
@media (max-width: 575.98px) {
  .ct-hero-shape i {
    font-size: 7rem;
  }
  .ct-form {
    padding: 1.25rem;
  }
  .ct-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.6rem;
    padding: 1rem;
  }
  .ct-hotline {
    padding: 1.4rem;
  }
  .ct-hotline-num {
    font-size: 1.4rem;
  }
}
</style>
