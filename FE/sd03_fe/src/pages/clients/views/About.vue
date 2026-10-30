<script setup>
const stats = [
  { val: "50,000+", label: "Khách hàng hài lòng" },
  { val: "100%", label: "Sneaker chính hãng" },
  { val: "5 Showroom", label: "Toàn quốc" },
  { val: "7 Ngày", label: "Đổi size miễn phí" },
];

const values = [
  {
    icon: "bi-shield-check",
    title: "100% Chính Hãng",
    desc: "Tất cả sản phẩm đều được kiểm định nghiêm ngặt, cam kết hoàn tiền gấp đôi nếu phát hiện hàng giả.",
  },
  {
    icon: "bi-arrow-repeat",
    title: "Đổi Size Trong 7 Ngày",
    desc: "Chưa vừa chân? Poly Shoes hỗ trợ đổi size miễn phí lần đầu tận nhà nhanh chóng và tiện lợi.",
  },
  {
    icon: "bi-truck",
    title: "Giao Hàng Thần Tốc",
    desc: "Đóng gói 2 lớp cẩn thận, nhận hàng trong 1–3 ngày và cho phép kiểm tra trước khi trả tiền.",
  },
  {
    icon: "bi-patch-check",
    title: "Bảo Hành Keo Chỉ 12 Tháng",
    desc: "Chính sách bảo hành keo chỉ chu đáo, hỗ trợ tư vấn chăm sóc và vệ sinh giày trọn đời.",
  },
];

const stores = [
  {
    name: "Showroom Trịnh Văn Bô - Hà Nội",
    address: "123 Trịnh Văn Bô, Nam Từ Liêm, Hà Nội",
    phone: "1900 1234 (Nhánh 1)",
    time: "08:30 - 22:00 (Hàng ngày)",
  },
  {
    name: "Showroom Nguyễn Trãi - Hà Nội",
    address: "456 Nguyễn Trãi, Thanh Xuân, Hà Nội",
    phone: "1900 1234 (Nhánh 2)",
    time: "08:30 - 22:00 (Hàng ngày)",
  },
  {
    name: "Showroom CMT8 - TP. Hồ Chí Minh",
    address: "789 Cách Mạng Tháng 8, Quận 3, TP. HCM",
    phone: "1900 1234 (Nhánh 3)",
    time: "08:30 - 22:00 (Hàng ngày)",
  },
];
</script>

<template>
  <div class="ps-about-page">
    <!-- Breadcrumb -->
    <nav class="small mb-4 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <span class="text-dark fw-semibold">Giới thiệu</span>
    </nav>

    <!-- Hero Banner -->
    <section class="hero p-4 p-md-5 mb-5 text-center text-md-start">
      <div class="row align-items-center g-4">
        <div class="col-md-7">
          <span class="ps-pill">Về chúng tôi</span>
          <h1 class="display-5 mt-3 mb-3 fw-bold">
            Poly Shoes – Giày đẹp,<br />bước cùng đam mê.
          </h1>
          <p class="text-muted-ps fs-5 mb-4" style="max-width: 520px; line-height: 1.6">
            Được thành lập với sứ mệnh mang đến cho giới trẻ Việt Nam những mẫu sneaker chính hãng chất lượng cao, chuẩn phong cách và giá cả hợp lý nhất.
          </p>
          <div class="d-flex flex-wrap gap-3 justify-content-center justify-content-md-start">
            <RouterLink to="/cua-hang" class="btn btn-primary btn-lg px-4">
              Khám phá sản phẩm
            </RouterLink>
            <RouterLink to="/lien-he" class="btn btn-light-border btn-lg px-4">
              Liên hệ chúng tôi
            </RouterLink>
          </div>
        </div>
        <div class="col-md-5 text-center">
          <div
            class="ps-thumb mx-auto bg-white shadow-sm"
            style="max-width: 300px; font-size: 8rem; transform: rotate(-8deg)"
          >
            👟
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Bar -->
    <section class="ps-card p-4 mb-5">
      <div class="row text-center g-4">
        <div v-for="s in stats" :key="s.label" class="col-6 col-md-3">
          <div class="display-6 fw-bolder text-danger mb-1">{{ s.val }}</div>
          <div class="text-muted-ps small fw-semibold">{{ s.label }}</div>
        </div>
      </div>
    </section>

    <!-- Core Values -->
    <section class="mb-5">
      <div class="text-center mb-4">
        <span class="ps-pill">Giá trị cốt lõi</span>
        <h2 class="mt-2 mb-2">Vì sao chọn Poly Shoes?</h2>
        <p class="text-muted-ps">Trải nghiệm mua sắm an tâm với 4 tiêu chuẩn hàng đầu</p>
      </div>
      <div class="row g-4">
        <div v-for="v in values" :key="v.title" class="col-md-6 col-lg-3">
          <div class="ps-card p-4 h-100 d-flex flex-column gap-2">
            <div
              class="d-inline-flex align-items-center justify-content-center rounded-circle text-danger mb-2"
              style="width: 52px; height: 52px; background: var(--ps-soft); font-size: 1.6rem"
            >
              <i :class="'bi ' + v.icon"></i>
            </div>
            <h5 class="fw-bold mb-1">{{ v.title }}</h5>
            <p class="text-muted-ps small mb-0" style="line-height: 1.6">{{ v.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Story Section -->
    <section class="ps-card p-4 p-md-5 mb-5">
      <div class="row align-items-center g-4">
        <div class="col-md-6">
          <span class="ps-pill">Câu chuyện thương hiệu</span>
          <h3 class="mt-3 mb-3">Tự tin trên từng bước chân</h3>
          <p class="text-muted-ps" style="line-height: 1.7">
            Poly Shoes bắt đầu từ tình yêu và niềm đam mê mãnh liệt với văn hoá sneaker của những người trẻ. Chúng tôi hiểu rằng một đôi giày tốt không chỉ là món phụ kiện thời trang, mà còn là người bạn đồng hành tiếp thêm sự tự tin cho bạn trong công việc, học tập và cuộc sống hàng ngày.
          </p>
          <p class="text-muted-ps" style="line-height: 1.7">
            Đến nay, Poly Shoes tự hào là điểm đến tin cậy của hơn 50.000 tín đồ mê giày trên khắp cả nước, liên tục cập nhật những xu hướng và bộ sưu tập sneaker mới nhất từ các thương hiệu hàng đầu thế giới.
          </p>
        </div>
        <div class="col-md-6 text-center">
          <div class="p-4 bg-light rounded-4 border">
            <div class="display-3 mb-2">🏆</div>
            <h4 class="fw-bold mb-2">Chất lượng tạo niềm tin</h4>
            <p class="text-muted-ps small mb-3">
              Mỗi sản phẩm đến tay khách hàng đều được kiểm tra kỹ lưỡng từ form dáng, đường may đến tem mác chính hãng.
            </p>
            <RouterLink to="/cua-hang" class="btn btn-outline-primary px-4">
              Mua sắm ngay hôm nay
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- Showrooms -->
    <section class="mb-5">
      <div class="text-center mb-4">
        <span class="ps-pill">Hệ thống cửa hàng</span>
        <h2 class="mt-2 mb-2">Ghé thăm Showroom Poly Shoes</h2>
        <p class="text-muted-ps">Trải nghiệm không gian mua sắm hiện đại và thử giày trực tiếp</p>
      </div>
      <div class="row g-4">
        <div v-for="st in stores" :key="st.name" class="col-md-4">
          <div class="ps-card p-4 h-100">
            <div class="d-flex align-items-center gap-2 mb-3">
              <i class="bi bi-shop fs-4 text-danger"></i>
              <h6 class="fw-bold mb-0">{{ st.name }}</h6>
            </div>
            <div class="small text-muted-ps d-flex flex-column gap-2">
              <div class="d-flex align-items-start gap-2">
                <i class="bi bi-geo-alt-fill text-danger mt-1"></i>
                <span>{{ st.address }}</span>
              </div>
              <div class="d-flex align-items-center gap-2">
                <i class="bi bi-telephone-fill text-danger"></i>
                <span>{{ st.phone }}</span>
              </div>
              <div class="d-flex align-items-center gap-2">
                <i class="bi bi-clock-fill text-danger"></i>
                <span>{{ st.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
