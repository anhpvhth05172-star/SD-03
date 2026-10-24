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

const metaBo = { requiresAuth: true, canBo: true }

const routes = [
  { path: '/login', component: LoginPage, meta: { title: 'Đăng nhập khách hàng', loginPage: true, loginMode: 'khach' } },
  { path: '/login-nhan-vien', component: LoginPage, meta: { title: 'Đăng nhập nhân viên', loginPage: true, loginMode: 'nhan-vien' } },
  {
    path: '/dang-nhap',
    redirect: (to) => ({ path: to.query.cheDo === 'khach' ? '/login' : '/login-nhan-vien', query: to.query.redirect ? { redirect: to.query.redirect } : {} }),
  },
  { path: '/dang-ky', component: RegisterPage, meta: { title: 'Đăng ký' } },
  { path: '/quen-mat-khau', component: QuenMatKhauPage, meta: { title: 'Quên mật khẩu' } },
  {
    path: '/tai-khoan',
    component: CustomerAccountPage,
    meta: { title: 'Tài khoản của tôi', canKhach: true },
  },
  {
    path: '/',
    component: AdminLayout,
    children: [
      {
        path: '',
        // Mở khu vực khách hàng trước; khách có thể đăng nhập sau từ giao diện.
        redirect: '/trang-chu',
      },
      { path: '/san-pham', name: 'ProductList', component: ProductListPage, meta: { ...metaBo, title: 'Quản lý sản phẩm' } },
      { path: '/san-pham/them', name: 'ProductCreate', component: ProductCreatePage, meta: { ...metaBo, title: 'Thêm mới sản phẩm' } },
      { path: '/san-pham/bien-the', name: 'ProductVariant', component: ProductVariantPage, meta: { ...metaBo, title: 'Biến thể sản phẩm' } },
      { path: '/bien-the-san-pham', component: ProductVariantPage, meta: { ...metaBo, title: 'Biến thể sản phẩm' } },
      { path: '/thuoc-tinh', component: AttributeCategoryPage, meta: { ...metaBo, title: 'Danh mục thuộc tính' } },
      { path: '/thuoc-tinh/:category', component: AttributeCategoryPage, meta: { ...metaBo, title: 'Danh mục thuộc tính' } },
      { path: '/hoa-don', component: InvoiceListPage, meta: { ...metaBo, title: 'Quản lý hóa đơn' } },
      { path: '/hoa-don/them', component: InvoiceFormPage, meta: { ...metaBo, title: 'Tạo hóa đơn' } },
      { path: '/hoa-don/:id', component: InvoiceDetailPage, meta: { ...metaBo, title: 'Chi tiết hóa đơn' } },
      { path: '/hoa-don/:id/sua', component: InvoiceFormPage, meta: { ...metaBo, title: 'Sửa hóa đơn' } },
      { path: '/phieu-giam-gia', component: VoucherListPage, meta: { ...metaBo, title: 'Quản lý phiếu giảm giá' } },
      { path: '/phieu-giam-gia/add', component: DiscountForm, meta: { ...metaBo, title: 'Thêm phiếu giảm giá' } },
      { path: '/phieu-giam-gia/detail/:id', component: DiscountFormDetail, meta: { ...metaBo, title: 'Chi tiết phiếu giảm giá' } },
      { path: '/phieu-giam-gia/update/:id', component: DiscountFormUpdate, meta: { ...metaBo, title: 'Cập nhật phiếu giảm giá' } },
      { path: '/phieu-giam-gia/:id/edit', redirect: to => `/phieu-giam-gia/update/${to.params.id}` },
      { path: '/dot-giam-gia', component: DiscountEventPage, meta: { ...metaBo, title: 'Quản lý đợt giảm giá' } },
      { path: '/dot-giam-gia/add', component: VoucherForm, meta: { ...metaBo, title: 'Thêm đợt giảm giá' } },
      { path: '/dot-giam-gia/detail/:id', component: VoucherFormDetail, meta: { ...metaBo, title: 'Chi tiết đợt giảm giá' } },
      { path: '/dot-giam-gia/update/:id', component: VoucherFormUpdate, meta: { ...metaBo, title: 'Cập nhật đợt giảm giá' } },
      { path: '/dot-giam-gia/edit/:id', redirect: to => `/dot-giam-gia/update/${to.params.id}` },
      { path: '/giam-gia', redirect: '/phieu-giam-gia' },
      {
        path: '/thong-ke',
        component: StatisticsPage,
        meta: { ...metaBo, title: 'Thống kê', roles: ['ADMIN', 'STAFF', 'NHAN_VIEN'] },
      },
      {
        path: '/lich-lam-viec',
        component: WorkSchedulePage,
        meta: { ...metaBo, title: 'Quản lý lịch làm việc', roles: ['ADMIN'] },
      },
      {
        path: '/lich-ca-lam',
        component: ShiftSchedulePage,
        meta: { ...metaBo, title: 'Lịch ca làm', roles: ['ADMIN'] },
      },
      {
        path: '/ban-hang',
        name: 'PosSales',
        component: PosSalesPage,
        // Màn hình mua hàng mặc định — công khai (khách ẩn danh cũng thấy)
        meta: { title: 'Bán hàng tại quầy' },
      },
      {
        path: '/khach-hang',
        component: CustomerListPage,
        meta: { ...metaBo, title: 'Quản lý khách hàng' },
      },
      {
        path: '/khach-hang/them',
        component: CustomerCreatePage,
        meta: { ...metaBo, title: 'Quản lý khách hàng' },
      },
      {
        path: '/khach-hang/:id/sua',
        component: CustomerCreatePage,
        meta: { ...metaBo, title: 'Quản lý khách hàng' },
      },
      {
        path: '/nhan-vien',
        component: EmployeeListPage,
        meta: { ...metaBo, title: 'Quản lý nhân viên', roles: ['ADMIN'] },
      },
      {
        path: '/nhan-vien/them',
        component: EmployeeCreatePage,
        meta: { ...metaBo, title: 'Quản lý nhân viên', roles: ['ADMIN'] },
      },
      {
        path: '/nhan-vien/:id/sua',
        component: EmployeeCreatePage,
        meta: { ...metaBo, title: 'Quản lý nhân viên', roles: ['ADMIN'] },
      },
      {
        path: '/chat',
        component: StaffChatPage,
        meta: { ...metaBo, title: 'Chat khách hàng' },
      },
      {
        path: '/ca',
        name: 'GiaoCaList',
        component: GiaoCaListPage,
        meta: { ...metaBo, title: 'Lịch sử giao ca', roles: ['NHAN_VIEN', 'ADMIN'] },
      },
      {
        path: '/ca/:id/doi-soat',
        name: 'GiaoCaDoiSoat',
        component: () => import('@/pages/Ca/GiaoCaDoiSoatPage.vue'),
        meta: { ...metaBo, title: 'Đối soát ca', roles: ['NHAN_VIEN', 'ADMIN'] },
      },
    ],
  },
  {
    path: '/admin/:pathMatch(.*)*',
    redirect: (to) => {
      const sub = to.params.pathMatch
      const path = Array.isArray(sub) ? sub.join('/') : sub
      return path ? `/${path}` : '/login-nhan-vien'
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  // Trang đăng nhập/đăng ký: đã đăng nhập thì về trang tương ứng vai trò.
  if (to.meta.loginPage || to.path === '/dang-ky') {
    if (isNhanVienDangNhap()) return '/hoa-don'
    if (isKhachHangDangNhap()) return '/tai-khoan'
    return true
  }

  if (!to.meta.requiresAuth) return true

  const token = getToken()
  const refreshToken = getRefreshToken()

  if (!token && !refreshToken) return '/login-nhan-vien'

  if (!(token && !isTokenExpired(token))) {
    if (!refreshToken) {
      if (token) dangXuat()
      return '/login-nhan-vien'
    }
    try {
      await refreshSession()
    } catch {
      dangXuat()
      return '/login-nhan-vien'
    }
  }

  // Vùng quản trị: chỉ nhân viên. Khách hàng về trang tài khoản, chưa đăng nhập về đăng nhập.
  if (to.meta.canBo && !isNhanVienDangNhap()) {
    return isKhachHangDangNhap() ? '/tai-khoan' : '/login-nhan-vien'
  }

  // Trang khách hàng: nhân viên về trang quản trị.
  if (to.meta.canKhach && isNhanVienDangNhap()) {
    return '/hoa-don'
  }

  const allowedRoles = to.meta.roles
  if (allowedRoles?.length) {
    const role = String(getUser()?.vaiTro || '').trim().toUpperCase()
    if (!allowedRoles.includes(role)) return '/hoa-don'
  }

  return true
})

export default router
