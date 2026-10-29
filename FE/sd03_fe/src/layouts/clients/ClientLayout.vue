<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute } from "vue-router";
import { cartCount, fetchCart } from "@/pages/clients/store";
import { getUser, dangXuat } from "@/utils/auth";
import "@/pages/clients/css/style.css";

const route = useRoute();

const user = ref(getUser());
const userName = computed(() => {
  if (!user.value) return "TÀI KHOẢN";
  return (
    user.value?.tenKhachHang ||
    user.value?.tenTaiKhoan ||
    "KHÁCH HÀNG"
  ).toUpperCase();
});

// Dropdown states
const showUserMenu = ref(false);
const showNotiMenu = ref(false);

const toggleUserMenu = (e) => {
  e.stopPropagation();
  showUserMenu.value = !showUserMenu.value;
  showNotiMenu.value = false;
};

const toggleNotiMenu = (e) => {
  e.stopPropagation();
  showNotiMenu.value = !showNotiMenu.value;
  showUserMenu.value = false;
};

const closeDropdowns = () => {
  showUserMenu.value = false;
  showNotiMenu.value = false;
};

onMounted(() => {
  window.addEventListener("click", closeDropdowns);
  fetchCart();
});

onUnmounted(() => {
  window.removeEventListener("click", closeDropdowns);
});

const handleLogout = () => {
  dangXuat();
  user.value = null;
  fetchCart();
  closeDropdowns();
};

// Newsletter
const newsletterEmail = ref("");
const newsletterSubscribed = ref(false);
const handleSubscribe = () => {
  if (newsletterEmail.value.trim()) {
    newsletterSubscribed.value = true;
    newsletterEmail.value = "";
    setTimeout(() => {
      newsletterSubscribed.value = false;
    }, 4000);
  }
};
</script>

<template>
  <div class="client-root">
    <!-- Header -->
    <header class="client-header">
      <nav class="navbar navbar-expand-lg container">
        <!-- Logo -->
        <RouterLink
          to="/trang-chu"
          class="navbar-brand d-flex align-items-center p-0 m-0"
          title="Poly Shoes - Trang chủ"
        >
          <img src="/logo.png" alt="Logo" class="client-header-logo" />
        </RouterLink>

        <!-- Mobile Toggler -->
        <button
          class="navbar-toggler border-0 shadow-none px-2"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#clientNavbar"
          aria-label="Toggle navigation"
        >
          <i class="bi bi-list fs-2 text-dark"></i>
        </button>

        <!-- Center Nav Links -->
        <div class="collapse navbar-collapse" id="clientNavbar">
          <ul class="client-nav my-3 my-lg-0">
            <li class="nav-item">
              <RouterLink
                to="/trang-chu"
                class="client-nav-link"
                :class="{ active: route.path === '/trang-chu' }"
              >
                TRANG CHỦ
              </RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink
                to="/cua-hang"
                class="client-nav-link"
                :class="{ active: route.path.startsWith('/cua-hang') }"
              >
                SẢN PHẨM
              </RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink
                to="/tra-cuu"
                class="client-nav-link"
                :class="{
                  active:
                    route.path === '/tra-cuu' ||
                    route.path.startsWith('/don-hang'),
                }"
              >
                TRA CỨU ĐƠN HÀNG
              </RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink
                to="/gioi-thieu"
                class="client-nav-link"
                :class="{ active: route.path === '/gioi-thieu' }"
              >
                GIỚI THIỆU
              </RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink
                to="/lien-he"
                class="client-nav-link"
                :class="{ active: route.path === '/lien-he' }"
              >
                LIÊN HỆ
              </RouterLink>
            </li>
          </ul>

          <!-- Right Action Items -->
          <div class="client-actions ms-lg-auto my-2 my-lg-0">
            <!-- Notification Bell -->
            <div class="position-relative">
              <button
                type="button"
                class="client-action-btn"
                title="Thông báo"
                @click="toggleNotiMenu"
              >
                <i class="bi bi-bell"></i>
                <span
                  class="position-absolute bg-danger rounded-circle border border-white"
                  style="top: 6px; right: 6px; width: 7px; height: 7px"
                ></span>
              </button>

              <!-- Notification Popover -->
              <div
                v-if="showNotiMenu"
                class="client-dropdown-menu p-3 shadow-lg"
                style="min-width: 290px"
                @click.stop
              >
                <div
                  class="d-flex justify-content-between align-items-center mb-2 pb-2 border-bottom"
                >
                  <span class="fw-bold small text-dark">Thông báo</span>
                  <span class="badge bg-danger-subtle text-danger small"
                    >Mới</span
                  >
                </div>
                <div class="d-flex flex-column gap-2 small">
                  <div class="p-2 rounded bg-light">
                    <strong class="text-danger d-block"
                      >👟 Bộ sưu tập mới 2026</strong
                    >
                    <span class="text-muted"
                      >Đã có mặt tại Poly Shoes. Đổi size trong 7 ngày miễn
                      phí!</span
                    >
                  </div>
                  <div class="p-2 rounded bg-light">
                    <strong class="text-dark d-block"
                      >🚚 Miễn phí vận chuyển</strong
                    >
                    <span class="text-muted"
                      >Giao hàng toàn quốc cho tất cả đơn hàng online.</span
                    >
                  </div>
                </div>
              </div>
            </div>

            <!-- Cart Icon -->
            <RouterLink
              to="/gio-hang"
              class="client-action-btn"
              title="Giỏ hàng"
            >
              <i class="bi bi-bag"></i>
              <span v-if="cartCount > 0" class="client-action-badge">
                {{ cartCount }}
              </span>
            </RouterLink>

            <!-- User Profile & Name -->
            <div class="position-relative">
              <button
                type="button"
                class="client-user-btn"
                @click="toggleUserMenu"
              >
                <i class="bi bi-person-circle user-icon"></i>
                <span class="d-none d-sm-inline">{{ userName }}</span>
              </button>

              <!-- User Dropdown Menu -->
              <div
                v-if="showUserMenu"
                class="client-dropdown-menu shadow-lg"
                @click.stop
              >
                <div class="px-3 py-2 border-bottom">
                  <div class="fw-bold text-dark small">{{ userName }}</div>
                  <div class="text-muted small" style="font-size: 0.75rem">
                    Khách hàng thân thiết
                  </div>
                </div>
                <RouterLink
                  to="/gio-hang"
                  class="client-dropdown-item"
                  @click="closeDropdowns"
                >
                  <i class="bi bi-bag-check text-muted"></i>
                  <span>Giỏ hàng của tôi</span>
                </RouterLink>
                <RouterLink
                  to="/tra-cuu"
                  class="client-dropdown-item"
                  @click="closeDropdowns"
                >
                  <i class="bi bi-search text-muted"></i>
                  <span>Tra cứu & đơn hàng của tôi</span>
                </RouterLink>
                <div class="client-dropdown-divider"></div>
                <RouterLink
                  to="/admin/san-pham"
                  class="client-dropdown-item text-muted"
                  @click="closeDropdowns"
                >
                  <i class="bi bi-speedometer2"></i>
                  <span>Trang quản trị (Admin)</span>
                </RouterLink>
                <div class="client-dropdown-divider"></div>
                <button
                  v-if="user"
                  type="button"
                  class="client-dropdown-item text-danger"
                  @click="handleLogout"
                >
                  <i class="bi bi-box-arrow-right"></i>
                  <span>Đăng xuất</span>
                </button>
                <RouterLink
                  v-else
                  to="/login"
                  class="client-dropdown-item text-primary"
                  @click="closeDropdowns"
                >
                  <i class="bi bi-box-arrow-in-right"></i>
                  <span>Đăng nhập</span>
                </RouterLink>
              </div>
            </div>
          </div>
        </div>
      </nav>
    </header>

    <!-- Main Content -->
    <main class="container py-4 py-md-5" style="min-height: 70vh">
      <RouterView />
    </main>

    <!-- Beautiful Modern Footer -->
    <footer class="client-footer">
      <!-- Perks Strip -->
      <div class="client-footer-perks">
        <div class="container">
          <div class="row g-4">
            <div class="col-6 col-lg-3">
              <div class="client-footer-perk-item">
                <div class="client-footer-perk-icon">
                  <i class="bi bi-truck"></i>
                </div>
                <div>
                  <div class="client-footer-perk-title">
                    Giao hàng toàn quốc
                  </div>
                  <p class="client-footer-perk-desc">
                    Nhận hàng 1–3 ngày, kiểm tra trước khi trả tiền
                  </p>
                </div>
              </div>
            </div>
            <div class="col-6 col-lg-3">
              <div class="client-footer-perk-item">
                <div class="client-footer-perk-icon">
                  <i class="bi bi-arrow-repeat"></i>
                </div>
                <div>
                  <div class="client-footer-perk-title">
                    Đổi size trong 7 ngày
                  </div>
                  <p class="client-footer-perk-desc">
                    Hỗ trợ đổi miễn phí tận nơi lần đầu tiên
                  </p>
                </div>
              </div>
            </div>
            <div class="col-6 col-lg-3">
              <div class="client-footer-perk-item">
                <div class="client-footer-perk-icon">
                  <i class="bi bi-shield-check"></i>
                </div>
                <div>
                  <div class="client-footer-perk-title">Chính hãng 100%</div>
                  <p class="client-footer-perk-desc">
                    Cam kết sản phẩm chuẩn, hoàn tiền gấp đôi nếu giả
                  </p>
                </div>
              </div>
            </div>
            <div class="col-6 col-lg-3">
              <div class="client-footer-perk-item">
                <div class="client-footer-perk-icon">
                  <i class="bi bi-headset"></i>
                </div>
                <div>
                  <div class="client-footer-perk-title">Hotline: 1900 1234</div>
                  <p class="client-footer-perk-desc">
                    Hỗ trợ tư vấn nhiệt tình từ 08:30 – 22:00
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Main Footer Columns -->
      <div class="client-footer-main">
        <div class="container">
          <div class="row g-4 g-lg-5">
            <!-- Col 1: About & Info -->
            <div class="col-lg-4 col-md-6">
              <div class="d-flex align-items-center gap-2 mb-3">
                <img
                  src="/logo.png"
                  alt="Poly Shoes"
                  style="height: 38px; width: auto; object-fit: contain"
                />
                <span class="fw-bold fs-5 text-white tracking-tight"
                  >POLY SHOES</span
                >
              </div>
              <p
                class="small text-muted mb-3"
                style="line-height: 1.6; color: #94a3b8 !important"
              >
                Poly Shoes tự hào mang đến cho khách hàng những mẫu sneaker
                chính hãng trẻ trung, năng động và thời thượng nhất. Đồng hành
                cùng bạn trên mọi hành trình.
              </p>
              <div
                class="d-flex flex-column gap-2 small text-muted mb-4"
                style="color: #94a3b8 !important"
              >
                <div class="d-flex align-items-start gap-2">
                  <i class="bi bi-geo-alt-fill text-danger mt-1"></i>
                  <span>123 Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <i class="bi bi-telephone-fill text-danger"></i>
                  <span>Hotline: 1900 1234 (08:30 – 22:00)</span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <i class="bi bi-envelope-fill text-danger"></i>
                  <span>Email: cskh@polyshoes.vn</span>
                </div>
              </div>
              <!-- Social Icons -->
              <div class="d-flex align-items-center gap-2">
                <a
                  href="javascript:void(0)"
                  class="client-footer-social-btn"
                  title="Facebook"
                >
                  <i class="bi bi-facebook"></i>
                </a>
                <a
                  href="javascript:void(0)"
                  class="client-footer-social-btn"
                  title="Instagram"
                >
                  <i class="bi bi-instagram"></i>
                </a>
                <a
                  href="javascript:void(0)"
                  class="client-footer-social-btn"
                  title="TikTok"
                >
                  <i class="bi bi-tiktok"></i>
                </a>
                <a
                  href="javascript:void(0)"
                  class="client-footer-social-btn"
                  title="YouTube"
                >
                  <i class="bi bi-youtube"></i>
                </a>
              </div>
            </div>

            <!-- Col 2: Navigation Links -->
            <div class="col-lg-2 col-md-6 col-6">
              <div class="client-footer-title">Khám phá</div>
              <ul class="client-footer-links">
                <li>
                  <RouterLink to="/trang-chu" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Trang chủ</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/cua-hang" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Tất cả sản phẩm</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink
                    to="/cua-hang?brand=Nike"
                    class="client-footer-link"
                  >
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Giày Nike</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink
                    to="/cua-hang?brand=Adidas"
                    class="client-footer-link"
                  >
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Giày Adidas</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/goi-y" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Sản phẩm gợi ý</span>
                  </RouterLink>
                </li>
              </ul>
            </div>

            <!-- Col 3: Customer Service & Policies -->
            <div class="col-lg-3 col-md-6 col-6">
              <div class="client-footer-title">Hỗ trợ khách hàng</div>
              <ul class="client-footer-links">
                <li>
                  <RouterLink to="/gioi-thieu" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Giới thiệu Poly Shoes</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/don-hang" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Đơn hàng của bạn</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/tra-cuu" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Tra cứu đơn hàng</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/lien-he" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Liên hệ & Góp ý</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/gio-hang" class="client-footer-link">
                    <i class="bi bi-chevron-right small text-danger"></i>
                    <span>Giỏ hàng của bạn</span>
                  </RouterLink>
                </li>
              </ul>
            </div>

            <!-- Col 4: Newsletter & Payments -->
            <div class="col-lg-3 col-md-6">
              <div class="client-footer-title">Đăng ký nhận ưu đãi</div>
              <p
                class="small text-muted mb-3"
                style="color: #94a3b8 !important"
              >
                Nhận ngay voucher
                <strong class="text-white">50.000₫</strong> cho đơn hàng đầu
                tiên cùng thông tin mẫu giày mới nhất.
              </p>
              <div class="mb-3">
                <div class="d-flex gap-2">
                  <input
                    v-model="newsletterEmail"
                    type="email"
                    class="form-control client-footer-newsletter-input shadow-none"
                    placeholder="Nhập email của bạn..."
                    @keyup.enter="handleSubscribe"
                  />
                  <button
                    type="button"
                    class="btn btn-primary px-3 text-nowrap"
                    @click="handleSubscribe"
                  >
                    Đăng ký
                  </button>
                </div>
                <div
                  v-if="newsletterSubscribed"
                  class="text-success small mt-2"
                >
                  <i class="bi bi-check-circle-fill me-1"></i> Cảm ơn bạn! Mã
                  voucher đã gửi đến email.
                </div>
              </div>

              <!-- Payment Partners -->
              <div class="mt-4">
                <span class="small fw-semibold text-white d-block mb-2"
                  >Phương thức thanh toán:</span
                >
                <div class="d-flex flex-wrap gap-2 align-items-center">
                  <span
                    class="badge bg-secondary-subtle text-white px-2 py-1 rounded"
                    style="
                      background: #1f2937 !important;
                      border: 1px solid #374151;
                    "
                  >
                    💵 Tiền mặt (COD)
                  </span>
                  <span
                    class="badge bg-secondary-subtle text-white px-2 py-1 rounded"
                    style="
                      background: #1f2937 !important;
                      border: 1px solid #374151;
                    "
                  >
                    🏦 Chuyển khoản
                  </span>
                  <span
                    class="badge bg-secondary-subtle text-white px-2 py-1 rounded"
                    style="
                      background: #1f2937 !important;
                      border: 1px solid #374151;
                    "
                  >
                    💳 VNPAY
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom Bar -->
      <div class="client-footer-bottom">
        <div
          class="container d-flex flex-column flex-md-row justify-content-between align-items-center gap-2"
        >
          <span> © 2026 Poly Shoes. Tất cả các quyền được bảo lưu. </span>
          <div class="d-flex align-items-center gap-3">
            <a
              href="javascript:void(0)"
              class="text-decoration-none text-muted-ps hover-white"
              >Điều khoản sử dụng</a
            >
            <span>·</span>
            <a
              href="javascript:void(0)"
              class="text-decoration-none text-muted-ps hover-white"
              >Chính sách bảo mật</a
            >
            <span>·</span>
            <RouterLink
              to="/admin/san-pham"
              class="text-decoration-none text-danger fw-semibold"
            >
              <i class="bi bi-shield-lock me-1"></i>Trang quản trị (Admin)
            </RouterLink>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.hover-white:hover {
  color: #ffffff !important;
}
</style>
