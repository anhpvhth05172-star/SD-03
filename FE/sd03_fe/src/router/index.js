import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getRefreshToken, isTokenExpired, dangXuat } from '../utils/auth'
import { refreshSession } from '../api/http'
import { laAdmin } from '../utils/session'
import AdminLayout from '../layouts/AdminLayout.vue'
import ProductListPage from '@/pages/Product/ProductListPage.vue'
import ProductCreatePage from '@/pages/Product/ProductCreatePage.vue'
import ProductVariantPage from '@/pages/Product/ProductVariantPage.vue'
import InvoiceListPage from '@/pages/Invoice/InvoiceListPage.vue'
import InvoiceDetailPage from '@/pages/Invoice/InvoiceDetailPage.vue'
import VoucherListPage from '@/pages/Discount/VoucherListPage.vue'
import DiscountEventPage from '@/pages/Discount/DiscountEventPage.vue'
import EmployeeListPage from '@/pages/Employee/EmployeeListPage.vue'
import EmployeeCreatePage from '@/pages/Employee/EmployeeCreatePage.vue'
import CustomerListPage from '@/pages/Customer/CustomerListPage.vue'
import CustomerCreatePage from '@/pages/Customer/CustomerCreatePage.vue'
import PosSalesPage from '@/pages/Pos/PosSalesPage.vue'
import AttributeCategoryPage from '@/pages/Product/AttributeCategoryPage.vue'
import PlaceholderPage from '@/pages/Common/PlaceholderPage.vue'
import DiscountForm from '@/pages/Discount/DiscountForm.vue'
import VoucherForm from '@/pages/Discount/VoucherForm.vue'
import DiscountFormDetail from '@/pages/Discount/DiscountFormDetail.vue'
import VoucherFormDetail from '@/pages/Discount/VoucherFormDetail.vue'
import DiscountFormUpdate from '@/pages/Discount/DiscountFormUpdate.vue'
import VoucherFormUpdate from '@/pages/Discount/VoucherFormUpdate.vue'
import LoginPage from '@/pages/Auth/LoginPage.vue'
import RegisterPage from '@/pages/Auth/RegisterPage.vue'
import StaffChatPage from '@/pages/Chat/StaffChatPage.vue'

const routes = [
  { path: '/dang-nhap', component: LoginPage, meta: { title: 'Đăng nhập' } },
  { path: '/dang-ky', component: RegisterPage, meta: { title: 'Đăng ký' } },
  {
    path: '/',
    component: AdminLayout,
    children: [
      { path: '', redirect: '/san-pham' },
      { path: '/san-pham', name: 'ProductList', component: ProductListPage, meta: { title: 'Quản lý sản phẩm' } },
      { path: '/san-pham/them', name: 'ProductCreate', component: ProductCreatePage, meta: { title: 'Thêm mới sản phẩm' } },
      { path: '/san-pham/bien-the', name: 'ProductVariant', component: ProductVariantPage, meta: { title: 'Biến thể sản phẩm' } },
      { path: '/bien-the-san-pham', component: ProductVariantPage, meta: { title: 'Biến thể sản phẩm' } },
      { path: '/thuoc-tinh', component: AttributeCategoryPage, meta: { title: 'Danh mục thuộc tính' } },
      { path: '/thuoc-tinh/:category', component: AttributeCategoryPage, meta: { title: 'Danh mục thuộc tính' } },
      { path: '/hoa-don', component: InvoiceListPage, meta: { title: 'Quản lý hóa đơn', requiresAuth: true } },
      { path: '/hoa-don/them', redirect: '/hoa-don' },
      { path: '/hoa-don/:id', component: InvoiceDetailPage, meta: { title: 'Chi tiết hóa đơn', requiresAuth: true } },
      { path: '/hoa-don/:id/sua', redirect: (to) => `/hoa-don/${to.params.id}` },
      { path: '/phieu-giam-gia', component: VoucherListPage, meta: { title: 'Quản lý phiếu giảm giá' } },
      { path: '/phieu-giam-gia/add', component: DiscountForm, meta: { title: 'Thêm phiếu giảm giá' } },
      { path: '/phieu-giam-gia/detail/:id', component: DiscountFormDetail, meta: { title: 'Chi tiết phiếu giảm giá' } },
      { path: '/phieu-giam-gia/update/:id', component: VoucherFormUpdate, meta: { title: 'Cập nhật phiếu giảm giá' } },
      { path: '/phieu-giam-gia/:id/edit', redirect: to => `/phieu-giam-gia/update/${to.params.id}` },
      { path: '/dot-giam-gia', component: DiscountEventPage, meta: { title: 'Quản lý đợt giảm giá', requiresAuth: true } },
      { path: '/dot-giam-gia/add', component: VoucherForm, meta: { title: 'Thêm đợt giảm giá' } },
      { path: '/dot-giam-gia/detail/:id', component: VoucherFormDetail, meta: { title: 'Chi tiết đợt giảm giá' } },
      { path: '/dot-giam-gia/update/:id', component: DiscountFormUpdate, meta: { title: 'Cập nhật đợt giảm giá' } },
      { path: '/dot-giam-gia/edit/:id', redirect: to => `/dot-giam-gia/update/${to.params.id}` },
      { path: '/giam-gia', redirect: '/phieu-giam-gia' },
      {
        path: '/thong-ke',
        component: PlaceholderPage,
        meta: { title: 'Thống kê', requiresAuth: true },
      },
      {
        path: '/ban-hang',
        name: 'PosSales',
        component: PosSalesPage,
        meta: { title: 'Bán hàng tại quầy', requiresAuth: true },
      },
      {
        path: '/khach-hang',
        component: CustomerListPage,
        meta: { title: 'Quản lý khách hàng', requiresAuth: true },
      },
      {
        path: '/khach-hang/them',
        component: CustomerCreatePage,
        meta: { title: 'Quản lý khách hàng', requiresAuth: true },
      },
      {
        path: '/khach-hang/:id/sua',
        component: CustomerCreatePage,
        meta: { title: 'Quản lý khách hàng', requiresAuth: true },
      },
      {
        path: '/nhan-vien',
        component: EmployeeListPage,
        meta: { title: 'Quản lý nhân viên', requiresAuth: true },
      },
      {
        path: '/nhan-vien/them',
        component: EmployeeCreatePage,
        meta: { title: 'Quản lý nhân viên', requiresAuth: true },
      },
      {
        path: '/nhan-vien/:id/sua',
        component: EmployeeCreatePage,
        meta: { title: 'Quản lý nhân viên', requiresAuth: true },
      },
      {
        path: '/chat',
        component: StaffChatPage,
        meta: { title: 'Chat khách hàng', requiresAuth: true },
      },
    ],
  },
  {
    path: '/admin/:pathMatch(.*)*',
    redirect: (to) => {
      const sub = to.params.pathMatch
      const path = Array.isArray(sub) ? sub.join('/') : sub
      return path ? `/${path}` : '/san-pham'
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/san-pham',
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
    // Token nhan vien khong co refresh: neu het han thi ve trang dang nhap
    if (token) dangXuat()
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


router.beforeEach((to) => {
  if (to.path.startsWith('/nhan-vien') && !laAdmin()) {
    return '/khach-hang'
  }
  return true
})


export default router
