import { createRouter, createWebHistory } from 'vue-router'
import {
  getToken,
  getRefreshToken,
  isTokenExpired,
  dangXuat,
  isNhanVienDangNhap,
  isKhachHangDangNhap,
  getUser,
} from '../utils/auth'
import { refreshSession } from '../api/http'
import AdminLayout from '../layouts/AdminLayout.vue'
import ClientLayout from '@/layouts/clients/ClientLayout.vue'
import ShippingFeeSettingsPage from '@/pages/Shipping/ShippingFeeSettingsPage.vue'
import ProductListPage from '@/pages/Product/ProductListPage.vue'
import ProductCreatePage from '@/pages/Product/ProductCreatePage.vue'
import ProductVariantPage from '@/pages/Product/ProductVariantPage.vue'
import InvoiceListPage from '@/pages/Invoice/InvoiceListPage.vue'
import InvoiceDetailPage from '@/pages/Invoice/InvoiceDetailPage.vue'
import InvoiceFormPage from '@/pages/Invoice/InvoiceFormPage.vue'
import VoucherListPage from '@/pages/Discount/VoucherListPage.vue'
import DiscountEventPage from '@/pages/Discount/DiscountEventPage.vue'
import EmployeeListPage from '@/pages/Employee/EmployeeListPage.vue'
import EmployeeCreatePage from '@/pages/Employee/EmployeeCreatePage.vue'
import CustomerListPage from '@/pages/Customer/CustomerListPage.vue'
import CustomerCreatePage from '@/pages/Customer/CustomerCreatePage.vue'
import PosSalesPage from '@/pages/Pos/PosSalesPage.vue'
import AttributeCategoryPage from '@/pages/Product/AttributeCategoryPage.vue'
import DiscountForm from '@/pages/Discount/DiscountForm.vue'
import VoucherForm from '@/pages/Discount/VoucherForm.vue'
import DiscountFormDetail from '@/pages/Discount/DiscountFormDetail.vue'
import VoucherFormDetail from '@/pages/Discount/VoucherFormDetail.vue'
import DiscountFormUpdate from '@/pages/Discount/DiscountFormUpdate.vue'
import VoucherFormUpdate from '@/pages/Discount/VoucherFormUpdate.vue'
import LoginPage from '@/pages/Auth/LoginPage.vue'
import RegisterPage from '@/pages/Auth/RegisterPage.vue'
import QuenMatKhauPage from '@/pages/Auth/QuenMatKhauPage.vue'
import StaffChatPage from '@/pages/Chat/StaffChatPage.vue'
import CustomerAccountPage from '@/pages/Auth/CustomerAccountPage.vue'
import StatisticsPage from '@/pages/Statistics/StatisticsPage.vue'
import WorkSchedulePage from '@/pages/Schedule/WorkSchedulePage.vue'
import ShiftSchedulePage from '@/pages/Schedule/ShiftSchedulePage.vue'
import GiaoCaListPage from '@/pages/Ca/GiaoCaListPage.vue'
import Home from '@/pages/clients/views/Home.vue'
import ProductShop from '@/pages/clients/views/ProductShop.vue'
import ProductDetail from '@/pages/clients/views/ProductDetail.vue'
import ProductCart from '@/pages/clients/views/ProductCart.vue'
import CheckOut from '@/pages/clients/views/CheckOut.vue'
import OrderLookup from '@/pages/clients/views/OrderLookup.vue'
import OrderList from '@/pages/clients/views/OrderList.vue'
import ProductCartDetail from '@/pages/clients/views/ProductCartDetail.vue'
import About from '@/pages/clients/views/About.vue'
import Contact from '@/pages/clients/views/Contact.vue'
import VNPayReturn from '@/pages/clients/views/VNPayReturn.vue'
import CustomerDisplayPage from '@/pages/Pos/CustomerDisplayPage.vue'

const metaBo = { requiresAuth: true, canBo: true }

// Xác định chế độ chạy: 'client' (cổng 5173), 'admin' (cổng 5174), hoặc 'all'
export const appMode =
  import.meta.env.VITE_APP_MODE ||
  (import.meta.env.MODE === 'admin'
    ? 'admin'
    : import.meta.env.MODE === 'client'
      ? 'client'
      : 'all')

/* ==========================================================================
   1. CẤU HÌNH ROUTE CHO CỔNG KHÁCH HÀNG (CLIENT - 5173)
   ========================================================================== */
const clientRoutes = [
  { path: '/man-hinh-khach', component: CustomerDisplayPage, meta: { title: 'Màn hình khách hàng | PolyShoes POS', public: true } },
  { path: '/dang-nhap', component: LoginPage, meta: { title: 'Đăng nhập', loginPage: true, loginMode: 'khach' } },
  { path: '/khach-hang', redirect: '/dang-nhap' },
  { path: '/login', redirect: '/dang-nhap' },
  { path: '/dang-ky', component: RegisterPage, meta: { title: 'Đăng ký' } },
  { path: '/quen-mat-khau', component: QuenMatKhauPage, meta: { title: 'Quên mật khẩu' } },
  {
    path: '/',
    component: ClientLayout,
    children: [
      { path: '', redirect: '/trang-chu' },
      { path: 'trang-chu', component: Home, meta: { title: 'Trang chủ Poly Shoes' } },
      { path: 'cua-hang', component: ProductShop, meta: { title: 'Sản phẩm' } },
      { path: 'cua-hang/:id', component: ProductDetail, meta: { title: 'Chi tiết sản phẩm' } },
      { path: 'gio-hang', component: ProductCart, meta: { title: 'Giỏ hàng' } },
      { path: 'thanh-toan', component: CheckOut, meta: { title: 'Thanh toán' } },
      { path: 'tra-cuu', component: OrderLookup, meta: { title: 'Tra cứu đơn hàng' } },
      { path: 'don-hang', component: OrderList, meta: { title: 'Đơn hàng của bạn', canKhach: true } },
      { path: 'don-hang/:id', component: ProductCartDetail, meta: { title: 'Chi tiết đơn hàng', canKhach: true } },
      { path: 'tai-khoan', component: CustomerAccountPage, meta: { title: 'Tài khoản của tôi', canKhach: true } },
      { path: 'gioi-thieu', component: About, meta: { title: 'Giới thiệu' } },
      { path: 'lien-he', component: Contact, meta: { title: 'Liên hệ' } },
      { path: 'goi-y', redirect: '/cua-hang' },
      { path: 'thanh-toan-vnpay', name: 'ClientVNPayReturn', component: VNPayReturn, meta: { title: 'Kết quả thanh toán VNPay', canKhach: true } },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/trang-chu',
  },
]

/* ==========================================================================
   2. CẤU HÌNH ROUTE CHO CỔNG QUẢN TRỊ / NHÂN VIÊN (ADMIN - 5174)
   ========================================================================== */
const adminChildrenRoutes = [
  {
    path: '',
    redirect: () => {
      if (!isNhanVienDangNhap()) return '/dang-nhap'
      const role = String(getUser()?.vaiTro || '').trim().toUpperCase()
      return role === 'ADMIN' ? '/thong-ke' : '/ban-hang'
    },
  },
  { path: 'ban-hang', name: 'PosSales', component: PosSalesPage, meta: { ...metaBo, title: 'Bán hàng tại quầy', roles: ['ADMIN', 'STAFF', 'NHAN_VIEN'] } },
  { path: 'hoa-don', component: InvoiceListPage, meta: { ...metaBo, title: 'Quản lý hóa đơn' } },
  { path: 'hoa-don/them', component: InvoiceFormPage, meta: { ...metaBo, title: 'Tạo hóa đơn' } },
  { path: 'hoa-don/:id', component: InvoiceDetailPage, meta: { ...metaBo, title: 'Chi tiết hóa đơn' } },
  { path: 'hoa-don/:id/sua', component: InvoiceFormPage, meta: { ...metaBo, title: 'Sửa hóa đơn' } },
  { path: 'san-pham', name: 'ProductList', component: ProductListPage, meta: { ...metaBo, title: 'Quản lý sản phẩm' } },
  { path: 'san-pham/them', name: 'ProductCreate', component: ProductCreatePage, meta: { ...metaBo, title: 'Thêm mới sản phẩm', roles: ['ADMIN'] } },
  { path: 'san-pham/bien-the', name: 'ProductVariant', component: ProductVariantPage, meta: { ...metaBo, title: 'Biến thể sản phẩm' } },
  { path: 'bien-the-san-pham', component: ProductVariantPage, meta: { ...metaBo, title: 'Biến thể sản phẩm' } },
  { path: 'thuoc-tinh', component: AttributeCategoryPage, meta: { ...metaBo, title: 'Danh mục thuộc tính', roles: ['ADMIN'] } },
  { path: 'thuoc-tinh/:category', component: AttributeCategoryPage, meta: { ...metaBo, title: 'Danh mục thuộc tính', roles: ['ADMIN'] } },
  { path: 'phieu-giam-gia', component: VoucherListPage, meta: { ...metaBo, title: 'Quản lý phiếu giảm giá', roles: ['ADMIN'] } },
  { path: 'phieu-giam-gia/add', component: DiscountForm, meta: { ...metaBo, title: 'Thêm phiếu giảm giá', roles: ['ADMIN'] } },
  { path: 'phieu-giam-gia/detail/:id', component: DiscountFormDetail, meta: { ...metaBo, title: 'Chi tiết phiếu giảm giá', roles: ['ADMIN'] } },
  { path: 'phieu-giam-gia/update/:id', component: DiscountFormUpdate, meta: { ...metaBo, title: 'Cập nhật phiếu giảm giá', roles: ['ADMIN'] } },
  { path: 'phieu-giam-gia/:id/edit', redirect: to => `/phieu-giam-gia/update/${to.params.id}` },
  { path: 'dot-giam-gia', component: DiscountEventPage, meta: { ...metaBo, title: 'Quản lý đợt giảm giá', roles: ['ADMIN'] } },
  { path: 'dot-giam-gia/add', component: VoucherForm, meta: { ...metaBo, title: 'Thêm đợt giảm giá', roles: ['ADMIN'] } },
  { path: 'dot-giam-gia/detail/:id', component: VoucherFormDetail, meta: { ...metaBo, title: 'Chi tiết đợt giảm giá', roles: ['ADMIN'] } },
  { path: 'dot-giam-gia/update/:id', component: VoucherFormUpdate, meta: { ...metaBo, title: 'Cập nhật đợt giảm giá', roles: ['ADMIN'] } },
  { path: 'dot-giam-gia/edit/:id', redirect: to => `/dot-giam-gia/update/${to.params.id}` },
  { path: 'giam-gia', redirect: '/phieu-giam-gia' },
  { path: 'thong-ke', component: StatisticsPage, meta: { ...metaBo, title: 'Thống kê', roles: ['ADMIN'] } },
  { path: 'cai-dat-phi-ship', component: ShippingFeeSettingsPage, meta: { ...metaBo, title: 'Cài đặt phí ship', roles: ['ADMIN'] } },
  { path: 'lich-lam-viec', component: WorkSchedulePage, meta: { ...metaBo, title: 'Quản lý lịch làm việc', roles: ['ADMIN', 'STAFF', 'NHAN_VIEN'] } },
  { path: 'lich-ca-lam', component: ShiftSchedulePage, meta: { ...metaBo, title: 'Lịch ca làm', roles: ['ADMIN', 'STAFF', 'NHAN_VIEN'] } },
  { path: 'lich-su-hoat-dong', redirect: '/ca' },
  { path: 'quan-ly-khach-hang', component: CustomerListPage, meta: { ...metaBo, title: 'Quản lý khách hàng' } },
  { path: 'quan-ly-khach-hang/them', component: CustomerCreatePage, meta: { ...metaBo, title: 'Quản lý khách hàng' } },
  { path: 'quan-ly-khach-hang/:id/sua', component: CustomerCreatePage, meta: { ...metaBo, title: 'Quản lý khách hàng' } },
  { path: 'quan-ly-nhan-vien', component: EmployeeListPage, meta: { ...metaBo, title: 'Quản lý nhân viên', roles: ['ADMIN'] } },
  { path: 'quan-ly-nhan-vien/them', component: EmployeeCreatePage, meta: { ...metaBo, title: 'Quản lý nhân viên', roles: ['ADMIN'] } },
  { path: 'quan-ly-nhan-vien/:id/sua', component: EmployeeCreatePage, meta: { ...metaBo, title: 'Quản lý nhân viên', roles: ['ADMIN'] } },
  { path: 'chat', component: StaffChatPage, meta: { ...metaBo, title: 'Chat khách hàng' } },
  { path: 'ca', name: 'GiaoCaList', component: GiaoCaListPage, meta: { ...metaBo, title: 'Lịch sử giao ca' } },
  { path: 'ca/:id/doi-soat', name: 'GiaoCaDoiSoat', component: () => import('@/pages/Ca/GiaoCaDoiSoatPage.vue'), meta: { ...metaBo, title: 'Đối soát ca' } },
]

const adminRoutes = [
  { path: '/man-hinh-khach', component: CustomerDisplayPage, meta: { title: 'Màn hình khách hàng | PolyShoes POS', public: true } },
  { path: '/dang-nhap', component: LoginPage, meta: { title: 'Đăng nhập nhân viên', loginPage: true, loginMode: 'nhan-vien' } },
  { path: '/nhan-vien', redirect: '/dang-nhap' },
  { path: '/login', redirect: '/dang-nhap' },
  { path: '/login-nhan-vien', redirect: '/dang-nhap' },
  { path: '/quen-mat-khau', component: QuenMatKhauPage, meta: { title: 'Quên mật khẩu' } },
  {
    path: '/',
    component: AdminLayout,
    children: adminChildrenRoutes,
  },
  {
    path: '/admin/:pathMatch(.*)*',
    redirect: (to) => {
      const sub = to.params.pathMatch
      const path = Array.isArray(sub) ? sub.join('/') : sub
      return path ? `/${path}` : '/dang-nhap'
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dang-nhap',
  },
]

/* ==========================================================================
   3. CẤU HÌNH ROUTE CHẾ ĐỘ ALL (TƯƠNG THÍCH NGƯỢC)
   ========================================================================== */
const allRoutes = [
  { path: '/man-hinh-khach', component: CustomerDisplayPage, meta: { title: 'Màn hình khách hàng | PolyShoes POS', public: true } },
  { path: '/khach-hang', component: LoginPage, meta: { title: 'Đăng nhập khách hàng', loginPage: true, loginMode: 'khach' } },
  { path: '/nhan-vien', component: LoginPage, meta: { title: 'Đăng nhập nhân viên', loginPage: true, loginMode: 'nhan-vien' } },
  { path: '/login', redirect: (to) => ({ path: '/khach-hang', query: to.query }) },
  { path: '/login-nhan-vien', redirect: (to) => ({ path: '/nhan-vien', query: to.query }) },
  { path: '/khach-hang/them', redirect: '/quan-ly-khach-hang/them' },
  { path: '/khach-hang/:id/sua', redirect: (to) => `/quan-ly-khach-hang/${to.params.id}/sua` },
  { path: '/nhan-vien/them', redirect: '/quan-ly-nhan-vien/them' },
  { path: '/nhan-vien/:id/sua', redirect: (to) => `/quan-ly-nhan-vien/${to.params.id}/sua` },
  {
    path: '/dang-nhap',
    redirect: (to) => ({ path: to.query.cheDo === 'khach' ? '/khach-hang' : '/nhan-vien', query: to.query.redirect ? { redirect: to.query.redirect } : {} }),
  },
  { path: '/dang-ky', component: RegisterPage, meta: { title: 'Đăng ký' } },
  { path: '/quen-mat-khau', component: QuenMatKhauPage, meta: { title: 'Quên mật khẩu' } },
  {
    path: '/',
    component: ClientLayout,
    children: clientRoutes[4].children,
  },
  {
    path: '/',
    component: AdminLayout,
    children: adminChildrenRoutes,
  },
  {
    path: '/admin/:pathMatch(.*)*',
    redirect: (to) => {
      const sub = to.params.pathMatch
      const path = Array.isArray(sub) ? sub.join('/') : sub
      return path ? `/${path}` : '/nhan-vien'
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/khach-hang',
  },
]

// Chọn danh sách route phù hợp với appMode
const routes =
  appMode === 'client'
    ? clientRoutes
    : appMode === 'admin'
      ? adminRoutes
      : allRoutes

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/* ==========================================================================
   NAVIGATION GUARDS (KIỂM SOÁT PHÂN QUYỀN VÀ PHIÊN ĐĂNG NHẬP)
   ========================================================================== */
router.beforeEach(async (to) => {
  // 1. Chế độ CLIENT (Cổng 5173): Chỉ cho phép khách hàng
  if (appMode === 'client') {
    if (to.meta.loginPage || to.path === '/dang-ky') {
      if (isKhachHangDangNhap()) return '/trang-chu'
      return true
    }
    if (to.meta.canKhach && !isKhachHangDangNhap()) {
      return { path: '/dang-nhap', query: { redirect: to.fullPath } }
    }
    return true
  }

  // 2. Chế độ ADMIN (Cổng 5174): Chỉ cho phép nhân viên và quản trị viên
  if (appMode === 'admin') {
    if (to.meta.public) return true
    if (to.meta.loginPage) {
      if (isNhanVienDangNhap()) {
        const role = String(getUser()?.vaiTro || '').trim().toUpperCase()
        return role === 'ADMIN' ? '/thong-ke' : '/ban-hang'
      }
      return true
    }

    const token = getToken()
    const refreshToken = getRefreshToken()

    if (!token && !refreshToken) {
      return { path: '/dang-nhap', query: { redirect: to.fullPath } }
    }

    if (!(token && !isTokenExpired(token))) {
      if (!refreshToken) {
        if (token) dangXuat()
        return { path: '/dang-nhap', query: { redirect: to.fullPath } }
      }
      try {
        await refreshSession()
      } catch {
        dangXuat()
        return { path: '/dang-nhap', query: { redirect: to.fullPath } }
      }
    }

    if (!isNhanVienDangNhap()) {
      return '/dang-nhap'
    }

    const allowedRoles = to.meta.roles
    if (allowedRoles?.length) {
      const role = String(getUser()?.vaiTro || '').trim().toUpperCase()
      if (!allowedRoles.includes(role)) return '/ban-hang'
    }
    return true
  }

  // 3. Chế độ ALL (Tương thích ngược đầy đủ)
  if (to.meta.loginPage || to.path === '/dang-ky') {
    if (isNhanVienDangNhap()) {
      const role = String(getUser()?.vaiTro || '').trim().toUpperCase()
      return role === 'ADMIN' ? '/thong-ke' : '/ban-hang'
    }
    if (isKhachHangDangNhap()) return '/trang-chu'
    return true
  }

  if (!to.meta.requiresAuth) return true

  const token = getToken()
  const refreshToken = getRefreshToken()

  if (!token && !refreshToken) return '/nhan-vien'

  if (!(token && !isTokenExpired(token))) {
    if (!refreshToken) {
      if (token) dangXuat()
      return '/nhan-vien'
    }
    try {
      await refreshSession()
    } catch {
      dangXuat()
      return '/nhan-vien'
    }
  }

  // Vùng quản trị: chỉ nhân viên.
  if (to.meta.canBo && !isNhanVienDangNhap()) {
    return isKhachHangDangNhap() ? '/tai-khoan' : '/nhan-vien'
  }

  // Trang khách hàng: nhân viên về trang quản trị.
  if (to.meta.canKhach && isNhanVienDangNhap()) {
    return '/hoa-don'
  }

  const allowedRoles = to.meta.roles
  if (allowedRoles?.length) {
    const role = String(getUser()?.vaiTro || '').trim().toUpperCase()
    if (!allowedRoles.includes(role)) return '/ban-hang'
  }

  return true
})

// Cập nhật title trình duyệt theo mode và route
router.afterEach((to) => {
  const baseTitle =
    appMode === 'admin'
      ? 'PolyShoes - Quản trị Hệ thống'
      : appMode === 'client'
        ? 'PolyShoes - Cửa hàng Giày Sneaker'
        : 'POLY SHOES - Hệ Thống Quản Lý Bán Giày'
  document.title = to.meta.title ? `${to.meta.title} | ${baseTitle}` : baseTitle
})

export default router
