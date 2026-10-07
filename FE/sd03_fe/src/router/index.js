import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getRefreshToken, isTokenExpired, dangXuat } from '../utils/auth'
import { refreshSession } from '../api/http'
import AdminLayout from '../layouts/AdminLayout.vue'
import InvoiceListPage from '../pages/InvoiceListPage.vue'
import InvoiceFormPage from '../pages/InvoiceFormPage.vue'
import InvoiceDetailPage from '../pages/InvoiceDetailPage.vue'
import VoucherListPage from '../pages/VoucherListPage.vue'
import DiscountEventPage from '../pages/DiscountEventPage.vue'
import EmployeeListPage from '../pages/EmployeeListPage.vue'
import EmployeeCreatePage from '../pages/EmployeeCreatePage.vue'
import CustomerListPage from '../pages/CustomerListPage.vue'
import CustomerCreatePage from '../pages/CustomerCreatePage.vue'
import LoginPage from '../pages/LoginPage.vue'
import RegisterPage from '../pages/RegisterPage.vue'
import ProductListView from '../views/ProductListView.vue'
import ProductCreateView from '../views/ProductCreateView.vue'
import ProductVariantView from '../views/ProductVariantView.vue'
import ProductVariantPage from '../pages/ProductVariantPage.vue'
import PlaceholderPage from '../pages/PlaceholderPage.vue'

const routes = [
  { path: '/', redirect: '/hoa-don' },
  { path: '/dang-nhap', component: LoginPage, meta: { title: 'Đăng nhập' } },
  { path: '/dang-ky', component: RegisterPage, meta: { title: 'Đăng ký' } },
  {
    path: '/',
    component: AdminLayout,
    children: [
      {
        path: 'hoa-don',
        component: InvoiceListPage,
        meta: { title: 'Quản lý hóa đơn', requiresAuth: true },
      },
      {
        path: 'hoa-don/them',
        component: InvoiceFormPage,
        meta: { title: 'Tạo hóa đơn', requiresAuth: true },
      },
      {
        path: 'hoa-don/:id',
        component: InvoiceDetailPage,
        meta: { title: 'Chi tiết hóa đơn', requiresAuth: true },
      },
      {
        path: 'phieu-giam-gia',
        component: VoucherListPage,
        meta: { title: 'Quản lý phiếu giảm giá' },
      },
      {
        path: 'dot-giam-gia',
        component: DiscountEventPage,
        meta: { title: 'Quản lý đợt giảm giá', requiresAuth: true },
      },
      { path: 'giam-gia', redirect: '/phieu-giam-gia' },
      {
        path: 'thong-ke',
        component: PlaceholderPage,
        meta: { title: 'Thống kê', requiresAuth: true },
      },
      {
        path: 'ban-hang',
        component: PlaceholderPage,
        meta: { title: 'Bán hàng tại quầy', requiresAuth: true },
      },
      { path: 'san-pham', component: ProductListView, meta: { title: 'Quản lý sản phẩm' } },
      { path: 'san-pham/them', component: ProductCreateView, meta: { title: 'Thêm sản phẩm' } },
      {
        path: 'san-pham/:id/bien-the',
        component: ProductVariantView,
        meta: { title: 'Biến thể sản phẩm' },
      },
      {
        path: 'bien-the-san-pham',
        component: ProductVariantPage,
        meta: { title: 'Biến thể sản phẩm' },
      },
      {
        path: 'khach-hang',
        component: CustomerListPage,
        meta: { title: 'Quản lý khách hàng', requiresAuth: true },
      },
      {
        path: 'khach-hang/them',
        component: CustomerCreatePage,
        meta: { title: 'Quản lý khách hàng', requiresAuth: true },
      },
      {
        path: 'nhan-vien',
        component: EmployeeListPage,
        meta: { title: 'Quản lý nhân viên', requiresAuth: true },
      },
      {
        path: 'nhan-vien/them',
        component: EmployeeCreatePage,
        meta: { title: 'Quản lý nhân viên', requiresAuth: true },
      },
      { path: ':pathMatch(.*)*', redirect: '/hoa-don' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true

  const token = getToken()
  const refreshToken = getRefreshToken()

  if (!token && !refreshToken) return '/dang-nhap'

  if (token && !isTokenExpired(token)) return true

  if (!refreshToken) {
    dangXuat()
    return '/dang-nhap'
  }

  try {
    await refreshSession()
    return true
  } catch {
    dangXuat()
    return '/dang-nhap'
  }
})

export default router
