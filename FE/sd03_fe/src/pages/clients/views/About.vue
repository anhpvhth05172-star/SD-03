<script setup>
const stats = [
  { val: "50.000+", label: "Khách hàng hài lòng", icon: "bi-emoji-smile" },
  { val: "100%", label: "Sneaker chính hãng", icon: "bi-patch-check" },
  { val: "5", label: "Showroom toàn quốc", icon: "bi-shop" },
  { val: "7 ngày", label: "Đổi size miễn phí", icon: "bi-arrow-repeat" },
];

const values = [
  {
    icon: "bi-shield-check",
    title: "100% chính hãng",
    desc: "Mọi sản phẩm đều được kiểm định nghiêm ngặt. Phát hiện hàng giả, Poly Shoes hoàn tiền gấp đôi.",
  },
  {
    icon: "bi-arrow-repeat",
    title: "Đổi size trong 7 ngày",
    desc: "Chưa vừa chân? Đổi size miễn phí lần đầu, nhân viên đến tận nhà nhận và giao lại.",
  },
  {
    icon: "bi-truck",
    title: "Giao hàng nhanh",
    desc: "Đóng gói 2 lớp cẩn thận, nhận hàng trong 1–3 ngày và được kiểm tra trước khi thanh toán.",
  },
  {
    icon: "bi-patch-check",
    title: "Bảo hành keo chỉ 12 tháng",
    desc: "Bảo hành keo chỉ chu đáo, tư vấn chăm sóc và vệ sinh giày trọn đời.",
  },
];

const stores = [
  {
    name: "Showroom Trịnh Văn Bô",
    city: "Hà Nội",
    address: "123 Trịnh Văn Bô, Nam Từ Liêm, Hà Nội",
    phone: "1900 1234",
    ext: "Nhánh 1",
    time: "08:30 – 22:00, hằng ngày",
  },
  {
    name: "Showroom Nguyễn Trãi",
    city: "Hà Nội",
    address: "456 Nguyễn Trãi, Thanh Xuân, Hà Nội",
    phone: "1900 1234",
    ext: "Nhánh 2",
    time: "08:30 – 22:00, hằng ngày",
  },
  {
    name: "Showroom CMT8",
    city: "TP. Hồ Chí Minh",
    address: "789 Cách Mạng Tháng 8, Quận 3, TP. HCM",
    phone: "1900 1234",
    ext: "Nhánh 3",
    time: "08:30 – 22:00, hằng ngày",
  },
];

const mapLink = (address) =>
  "https://www.google.com/maps/search/?api=1&query=" +
  encodeURIComponent(address);
</script>

<template>
  <div class="ab-page">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      /
      <span class="text-dark fw-semibold">Giới thiệu</span>
    </nav>

    <!-- ================= HERO ================= -->
    <section class="ab-hero mb-4">
      <div class="ab-hero-shape" aria-hidden="true">
        <span>👟</span>
      </div>

      <div class="ab-hero-body">
        <div class="ab-hero-eyebrow">Về chúng tôi</div>
        <h1 class="ab-hero-title">
          Giày đẹp,<br />
          <span>bước cùng đam mê</span>
        </h1>
        <p class="ab-hero-sub">
          Poly Shoes mang đến cho giới trẻ Việt Nam những mẫu sneaker
          <strong>chính hãng</strong> chất lượng cao, đúng phong cách và có giá
          hợp lý nhất.
        </p>
        <div class="d-flex flex-wrap gap-2">
          <RouterLink to="/cua-hang" class="btn btn-primary ab-pill-btn px-4">
            Khám phá sản phẩm
          </RouterLink>
          <RouterLink to="/lien-he" class="btn ab-ghost-btn px-4">
            Liên hệ chúng tôi
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- ================= SỐ LIỆU ================= -->
    <section class="ps-card ab-stats mb-5">
      <div v-for="s in stats" :key="s.label" class="ab-stat">
        <div class="ab-stat-ico"><i :class="'bi ' + s.icon"></i></div>
        <div>
          <div class="ab-stat-val">{{ s.val }}</div>
          <div class="ab-stat-label">{{ s.label }}</div>
        </div>
      </div>
    </section>

    <!-- ================= GIÁ TRỊ CỐT LÕI ================= -->
    <section class="mb-5">
      <div class="ab-head">
        <div class="ab-eyebrow">/ Giá trị cốt lõi</div>
        <h3 class="ab-h">Vì sao chọn Poly Shoes?</h3>
        <p class="text-muted-ps small mb-0">
          Bốn cam kết để bạn mua giày an tâm từ lúc chọn đến lúc nhận hàng.
        </p>
      </div>

      <div class="row g-3">
        <div v-for="v in values" :key="v.title" class="col-sm-6 col-lg-3">
          <div class="ps-card ab-value h-100">
            <div class="ab-ico"><i :class="'bi ' + v.icon"></i></div>
            <h6 class="fw-bold mb-2">{{ v.title }}</h6>
            <p class="text-muted-ps small mb-0">{{ v.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= CÂU CHUYỆN ================= -->
    <section class="ps-card ab-story mb-5">
      <div class="row align-items-center g-4">
        <div class="col-lg-7">
          <div class="ab-eyebrow">/ Câu chuyện thương hiệu</div>
          <h3 class="ab-h mb-3">Tự tin trên từng bước chân</h3>
          <p class="text-muted-ps small">
            Poly Shoes bắt đầu từ tình yêu với văn hoá sneaker của những người
            trẻ. Chúng tôi hiểu một đôi giày tốt không chỉ là phụ kiện thời
            trang mà còn là người bạn đồng hành, tiếp thêm sự tự tin trong công
            việc, học tập và cuộc sống mỗi ngày.
          </p>
          <p class="text-muted-ps small mb-0">
            Đến nay, Poly Shoes là địa chỉ tin cậy của hơn 50.000 tín đồ mê giày
            trên cả nước, luôn cập nhật xu hướng và bộ sưu tập mới nhất từ các
            thương hiệu hàng đầu thế giới.
          </p>
        </div>

        <div class="col-lg-5">
          <div class="ab-quality">
            <div class="ab-quality-ico">🏆</div>
            <h5 class="fw-bold mb-2">Chất lượng tạo niềm tin</h5>
            <p class="text-muted-ps small mb-3">
              Mỗi đôi giày đến tay khách hàng đều được kiểm tra kỹ từ form dáng,
              đường may đến tem mác chính hãng.
            </p>
            <RouterLink to="/cua-hang" class="btn btn-primary ab-pill-btn px-4">
              Mua sắm ngay hôm nay
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= SHOWROOM ================= -->
    <section class="mb-5">
      <div class="ab-head">
        <div class="ab-eyebrow">/ Hệ thống cửa hàng</div>
        <h3 class="ab-h">Ghé thăm showroom Poly Shoes</h3>
        <p class="text-muted-ps small mb-0">
          Thử giày trực tiếp trong không gian mua sắm hiện đại.
        </p>
      </div>

      <div class="row g-3">
        <div v-for="st in stores" :key="st.name" class="col-md-6 col-lg-4">
          <div class="ps-card ab-store h-100">
            <span class="ab-city">
              <i class="bi bi-geo-alt-fill"></i>{{ st.city }}
            </span>
            <h6 class="fw-bold mt-3 mb-3">{{ st.name }}</h6>

            <div class="ab-store-list small text-muted-ps">
              <div>
                <i class="bi bi-pin-map"></i><span>{{ st.address }}</span>
              </div>
              <div>
                <i class="bi bi-telephone"></i>
                <span>
                  <a
                    :href="'tel:' + st.phone.replace(/\s/g, '')"
                    class="fw-semibold text-dark text-decoration-none"
                    >{{ st.phone }}</a
                  >
                  ({{ st.ext }})
                </span>
              </div>
              <div>
                <i class="bi bi-clock"></i><span>{{ st.time }}</span>
              </div>
            </div>

            <a
              :href="mapLink(st.address)"
              target="_blank"
              rel="noopener"
              class="btn btn-outline-primary btn-sm ab-pill-btn mt-3"
            >
              <i class="bi bi-signpost-split me-1"></i>Xem đường đi
            </a>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Màu chủ đạo bám theo theme của dự án (đỏ - trắng) */
.ab-page {
  --ab-red: var(--bs-primary, #d62828);
  --ab-red-soft: rgba(var(--bs-primary-rgb, 214, 40, 40), 0.08);
}

/* ---------- Tiêu đề section (giống trang tra cứu) ---------- */
.ab-head {
  margin-bottom: 1.25rem;
}
.ab-eyebrow {
  color: var(--ab-red);
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.ab-h {
  font-weight: 600;
  margin: 0.15rem 0 0.3rem;
}

/* ---------- Hero tối + khối đỏ nghiêng ---------- */
.ab-hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 3.25rem 2.75rem;
  color: #fff;
  background: linear-gradient(115deg, #2a0508 0%, #12090a 55%, #0b0b0b 100%);
}
.ab-hero-shape {
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
.ab-hero-shape span {
  font-size: 9rem;
  line-height: 1;
  transform: rotate(-26deg);
  filter: drop-shadow(0 18px 18px rgba(0, 0, 0, 0.35));
}
.ab-hero-body {
  position: relative;
  z-index: 1;
  max-width: 560px;
}
.ab-hero-eyebrow {
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
.ab-hero-eyebrow::after {
  content: "";
  width: 36px;
  height: 2px;
  background: var(--ab-red);
}
.ab-hero-title {
  margin: 0 0 0.75rem;
  font-size: clamp(1.9rem, 4.2vw, 3rem);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.02em;
  text-transform: uppercase;
}
.ab-hero-title span {
  color: #ff3b47;
}
.ab-hero-sub {
  margin-bottom: 1.5rem;
  max-width: 500px;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.72);
}
.ab-hero-sub strong {
  color: #fff;
}
.ab-pill-btn {
  border-radius: 999px;
  font-weight: 600;
}
.ab-ghost-btn {
  border-radius: 999px;
  font-weight: 600;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.06);
}
.ab-ghost-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.6);
}

/* ---------- Số liệu ---------- */
.ab-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  padding: 0.5rem 0;
  border-radius: 16px;
}
.ab-stat {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1.1rem 1.5rem;
}
.ab-stat + .ab-stat {
  border-left: 1px solid #eee;
}
.ab-stat-ico {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  color: var(--ab-red);
  background: var(--ab-red-soft);
  border-radius: 12px;
}
.ab-stat-val {
  font-size: 1.35rem;
  font-weight: 800;
  line-height: 1.15;
  color: #111;
}
.ab-stat-label {
  font-size: 0.8rem;
  color: #6b7280;
}

/* ---------- Giá trị cốt lõi ---------- */
.ab-value {
  padding: 1.4rem;
  border-radius: 16px;
}
.ab-ico {
  width: 46px;
  height: 46px;
  margin-bottom: 0.9rem;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: var(--ab-red);
  background: var(--ab-red-soft);
  border-radius: 12px;
}

/* ---------- Câu chuyện ---------- */
.ab-story {
  padding: 2rem;
  border-radius: 18px;
}
.ab-quality {
  padding: 1.75rem 1.5rem;
  text-align: center;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 14px;
}
.ab-quality-ico {
  width: 64px;
  height: 64px;
  margin: 0 auto 0.9rem;
  display: grid;
  place-items: center;
  font-size: 1.9rem;
  background: var(--ab-red-soft);
  border-radius: 50%;
}

/* ---------- Showroom ---------- */
.ab-store {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 1.4rem;
  border-radius: 16px;
}
.ab-city {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.15rem 0.75rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ab-red);
  background: var(--ab-red-soft);
  border-radius: 999px;
}
.ab-store-list {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  flex-grow: 1;
}
.ab-store-list > div {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
}
.ab-store-list i {
  margin-top: 0.15rem;
  color: var(--ab-red);
}

/* ---------- Responsive ---------- */
@media (max-width: 991.98px) {
  .ab-hero {
    padding: 2.25rem 1.5rem;
  }
  .ab-hero-shape {
    width: 70%;
    right: -30%;
    opacity: 0.5;
  }
  .ab-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .ab-stat:nth-child(3) {
    border-left: 0;
  }
  .ab-stat:nth-child(n + 3) {
    border-top: 1px solid #eee;
  }
}
@media (max-width: 575.98px) {
  .ab-hero-shape span {
    font-size: 6rem;
  }
  .ab-story {
    padding: 1.25rem;
  }
  .ab-stat {
    padding: 0.9rem 1rem;
    gap: 0.6rem;
  }
  .ab-stat-ico {
    width: 38px;
    height: 38px;
    font-size: 1.05rem;
  }
  .ab-stat-val {
    font-size: 1.1rem;
  }
}
</style>
