<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { cartCount, fetchCart } from "@/pages/clients/store";
import { getUser, dangXuat } from "@/utils/auth";
import "@/pages/clients/css/style.css";

const route = useRoute();
const router = useRouter();

// Đường dẫn trang thông tin khách hàng (đổi cho khớp router của bạn)
const PROFILE_ROUTE = "/tai-khoan";

// Search in navbar
const navSearch = ref("");
const handleNavSearch = () => {
  if (navSearch.value.trim()) {
    router.push({ path: "/cua-hang", query: { q: navSearch.value.trim() } });
    navSearch.value = "";
  }
};

const user = ref(getUser());
const isLoggedIn = computed(() => !!user.value);

const userName = computed(() => {
  if (!user.value) return "TÀI KHOẢN";
  return (
    user.value?.tenKhachHang ||
    user.value?.tenTaiKhoan ||
    "KHÁCH HÀNG"
  ).toUpperCase();
});

// Dòng phụ dưới tên trong menu: email hoặc số điện thoại nếu có
const userSub = computed(
  () => user.value?.email || user.value?.soDienThoai || "Khách hàng thân thiết",
);

// Chỉ hiện lối vào trang quản trị với tài khoản quản trị (chỉnh theo field vai trò thật của bạn)
const isAdmin = computed(() => {
  const r = String(
    user.value?.role ?? user.value?.vaiTro ?? user.value?.tenVaiTro ?? "",
  ).toUpperCase();
  return /ADMIN|QUAN_LY|QUẢN/.test(r);
});

// Dropdown state
const showUserMenu = ref(false);

const toggleUserMenu = (e) => {
  e.stopPropagation();
  showUserMenu.value = !showUserMenu.value;
};

const closeDropdowns = () => {
  showUserMenu.value = false;
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
  router.push("/trang-chu");
};

// Newsletter
</script>

<template>
  <div class="client-root">
    <!-- Top Announcement Bar (Matching Screenshot) -->
    <div
      class="client-top-bar py-1 px-3 text-white small"
      style="background-color: #d32f2f"
    >
      <div class="container d-flex justify-content-between align-items-center">
        <span class="d-none d-md-inline text-white">
          <i class="bi bi-truck me-1"></i> Miễn phí vận chuyển cho đơn từ
          500.000đ
        </span>
        <span class="mx-auto mx-md-0 fw-semibold text-white">
          ⚡ Ưu đãi mùa hè: Nhập
          <strong class="text-warning">POLY2026</strong> giảm ngay 10%
        </span>
        <div class="d-none d-md-flex align-items-center gap-3 text-white">
          <a href="tel:19001234" class="text-white text-decoration-none">
            <i class="bi bi-telephone me-1"></i> Hotline: 1900 1234
          </a>
        </div>
      </div>
    </div>

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
          <div
            class="client-actions ms-lg-auto my-2 my-lg-0 d-flex align-items-center gap-2"
          >
            <!-- Search bar input -->
            <div class="client-nav-search d-none d-xl-block position-relative">
              <input
                v-model="navSearch"
                type="text"
                class="form-control nav-search-input"
                placeholder="Tìm kiếm sản phẩm..."
                @keyup.enter="handleNavSearch"
              />
              <button
                type="button"
                class="btn nav-search-btn"
                @click="handleNavSearch"
              >
                <i class="bi bi-search"></i>
              </button>
            </div>

            <!-- Cart Icon with Badge -->
            <RouterLink
              to="/gio-hang"
              class="client-action-btn"
              title="Giỏ hàng"
            >
              <i class="bi bi-bag fs-5"></i>
              <span class="client-action-badge">
                {{ cartCount }}
              </span>
            </RouterLink>

            <!-- Tài khoản: bấm vào tên để mở menu -->
            <div class="position-relative">
              <!-- Icon người dùng (chỉ hiện trên màn hình nhỏ) -->
              <button
                type="button"
                class="client-action-btn d-sm-none"
                title="Tài khoản"
                @click="toggleUserMenu"
              >
                <i class="bi bi-person fs-5"></i>
              </button>

              <!-- Nút tên người dùng -->
              <button
                v-if="isLoggedIn"
                type="button"
                class="btn client-admin-pill d-none d-sm-inline-flex align-items-center gap-1"
                :aria-expanded="showUserMenu"
                title="Tài khoản của bạn"
                @click="toggleUserMenu"
              >
                <i class="bi bi-person-circle"></i>
                <span>{{ userName }}</span>
                <i
                  class="bi small"
                  :class="showUserMenu ? 'bi-chevron-up' : 'bi-chevron-down'"
                ></i>
              </button>

              <!-- Chưa đăng nhập -->
              <RouterLink
                v-else
                to="/khach-hang"
                class="btn client-admin-pill d-none d-sm-inline-flex align-items-center gap-1"
                title="Đăng nhập"
              >
                <i class="bi bi-box-arrow-in-right"></i>
                <span>ĐĂNG NHẬP</span>
              </RouterLink>

              <!-- User Dropdown Menu -->
              <div
                v-if="showUserMenu"
                class="client-dropdown-menu shadow-lg"
                @click.stop
              >
                <template v-if="isLoggedIn">
                  <div class="px-3 py-2 border-bottom">
                    <div class="fw-bold text-dark small">{{ userName }}</div>
                    <div class="text-muted small" style="font-size: 0.75rem">
                      {{ userSub }}
                    </div>
                  </div>
                  <RouterLink
                    :to="PROFILE_ROUTE"
                    class="client-dropdown-item"
                    @click="closeDropdowns"
                  >
                    <i class="bi bi-person-vcard text-muted"></i>
                    <span>Thông tin khách hàng</span>
                  </RouterLink>
                  <RouterLink
                    to="/tra-cuu"
                    class="client-dropdown-item"
                    @click="closeDropdowns"
                  >
                    <i class="bi bi-receipt text-muted"></i>
                    <span>Đơn hàng của tôi</span>
                  </RouterLink>
                  <RouterLink
                    to="/gio-hang"
                    class="client-dropdown-item"
                    @click="closeDropdowns"
                  >
                    <i class="bi bi-bag-check text-muted"></i>
                    <span>Giỏ hàng của tôi</span>
                  </RouterLink>
                  <template v-if="isAdmin">
                    <div class="client-dropdown-divider"></div>
                    <RouterLink
                      to="/admin/san-pham"
                      class="client-dropdown-item text-muted"
                      @click="closeDropdowns"
                    >
                      <i class="bi bi-speedometer2"></i>
                      <span>Trang quản trị (Admin)</span>
                    </RouterLink>
                  </template>
                  <div class="client-dropdown-divider"></div>
                  <button
                    type="button"
                    class="client-dropdown-item text-danger"
                    @click="handleLogout"
                  >
                    <i class="bi bi-box-arrow-right"></i>
                    <span>Đăng xuất</span>
                  </button>
                </template>

                <template v-else>
                  <RouterLink
                    to="/khach-hang"
                    class="client-dropdown-item text-primary"
                    @click="closeDropdowns"
                  >
                    <i class="bi bi-box-arrow-in-right"></i>
                    <span>Đăng nhập</span>
                  </RouterLink>
                  <RouterLink
                    to="/tra-cuu"
                    class="client-dropdown-item"
                    @click="closeDropdowns"
                  >
                    <i class="bi bi-search text-muted"></i>
                    <span>Tra cứu đơn hàng</span>
                  </RouterLink>
                </template>
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
      <!-- Main Footer Columns (Exact Matching User Screenshot) -->
      <div class="client-footer-main">
        <div class="container">
          <div class="row g-4 g-lg-5">
            <!-- Col 1: Brand & Contact -->
            <div class="col-lg-4 col-md-6">
              <div class="d-flex align-items-center gap-2 mb-3">
                <img
                  src="/logo.png"
                  alt="Poly Shoes"
                  class="footer-brand-logo"
                />
                <div>
                  <div class="fw-black fs-4 text-white footer-brand-text">
                    POLY SHOES
                  </div>
                  <div
                    class="text-danger small fw-semibold tracking-wider"
                    style="font-size: 0.72rem; letter-spacing: 0.15em"
                  >
                    AUTHENTIC SNEAKERS
                  </div>
                </div>
              </div>
              <p class="small mb-4 footer-about-text text-white-50">
                Poly Shoes – Hệ thống phân phối giày sneaker chính hãng hàng
                đầu. Đa dạng mẫu mã từ Nike, Adidas, Jordan, Puma,... Tự tin
                bước cùng bạn trên mọi hành trình.
              </p>

              <div
                class="d-flex flex-column gap-2 small footer-contact-list mb-4 text-white-50"
              >
                <div class="d-flex align-items-start gap-2">
                  <i class="bi bi-geo-alt-fill text-danger mt-1"></i>
                  <span>123 Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <i class="bi bi-telephone-fill text-danger"></i>
                  <span
                    >Hotline:
                    <strong class="text-white">1900 1234</strong> (08:30 –
                    22:00)</span
                  >
                </div>
                <div class="d-flex align-items-center gap-2">
                  <i class="bi bi-envelope-fill text-danger"></i>
                  <span
                    >Email:
                    <strong class="text-white">cskh@polyshoes.vn</strong></span
                  >
                </div>
              </div>

              <!-- Social Links -->
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
              <div class="client-footer-title text-white">KHÁM PHÁ</div>
              <ul class="client-footer-links">
                <li>
                  <RouterLink to="/trang-chu" class="client-footer-link">
                    <span>Trang chủ</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/cua-hang" class="client-footer-link">
                    <span>Tất cả sản phẩm</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink
                    to="/cua-hang?brand=Nike"
                    class="client-footer-link"
                  >
                    <span>Giày Nike</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink
                    to="/cua-hang?brand=Adidas"
                    class="client-footer-link"
                  >
                    <span>Giày Adidas</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/gioi-thieu" class="client-footer-link">
                    <span>Về chúng tôi</span>
                  </RouterLink>
                </li>
              </ul>
            </div>

            <!-- Col 3: Customer Service & Policies -->
            <div class="col-lg-3 col-md-6 col-6">
              <div class="client-footer-title text-white">
                HỖ TRỢ KHÁCH HÀNG
              </div>
              <ul class="client-footer-links">
                <li>
                  <RouterLink to="/tra-cuu" class="client-footer-link">
                    <span>Tra cứu đơn hàng</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/don-hang" class="client-footer-link">
                    <span>Liên hệ đơn hàng</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/gio-hang" class="client-footer-link">
                    <span>Giỏ hàng của tôi</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/lien-he" class="client-footer-link">
                    <span>Liên hệ & khiếu nại</span>
                  </RouterLink>
                </li>
                <li>
                  <RouterLink to="/gioi-thieu" class="client-footer-link">
                    <span>Chính sách đổi trả 7 ngày</span>
                  </RouterLink>
                </li>
              </ul>
            </div>

            <!-- Col 4: Payment methods -->
            <div class="col-lg-3 col-md-6">
              <!-- Payment Partners -->
              <div>
                <span class="small fw-semibold text-white-50 d-block mb-2"
                  >Phương thức thanh toán:</span
                >
                <div class="d-flex flex-wrap gap-2 align-items-center">
                  <div class="payment-badge-dark">
                    <i class="bi bi-cash-stack text-success"></i> COD
                  </div>
                  <div class="payment-badge-dark">
                    <i class="bi bi-bank text-info"></i> Chuyển khoản
                  </div>
                  <div class="payment-badge-dark">
                    <i class="bi bi-credit-card text-warning"></i> VNPay /
                    Visa/Mastercard
                  </div>
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
          <span class="text-white-50"
            >© 2026 Poly Shoes. Tất cả các quyền được bảo lưu.</span
          >
          <div class="d-flex align-items-center gap-3">
            <a
              href="javascript:void(0)"
              class="text-decoration-none footer-bottom-link-dark"
              >Điều khoản dịch vụ</a
            >
            <span class="text-white-50">|</span>
            <a
              href="javascript:void(0)"
              class="text-decoration-none footer-bottom-link-dark"
              >Chính sách bảo mật</a
            >
            <span class="text-white-50">|</span>
            <RouterLink
              to="/admin/san-pham"
              class="text-decoration-none text-danger fw-semibold"
            >
              Trang quản trị (Admin)
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
