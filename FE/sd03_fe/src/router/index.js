import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getRefreshToken, isTokenExpired, dangXuat } from '../utils/auth'
import { refreshSession } from '../api/http'
import AdminLayout from '../layouts/AdminLayout.vue'
import ProductListPage from '../pages/ProductListPage.vue'
import ProductCreatePage from '../pages/ProductCreatePage.vue'
import ProductVariantPage from '../pages/ProductVariantPage.vue'
import InvoiceListPage from '../pages/InvoiceListPage.vue'
import InvoiceFormPage from '../pages/InvoiceFormPage.vue'
import InvoiceDetailPage from '../pages/InvoiceDetailPage.vue'
import VoucherListPage from '../pages/VoucherListPage.vue'
import DiscountEventPage from '../pages/DiscountEventPage.vue'
import EmployeeListPage from '../pages/EmployeeListPage.vue'
import EmployeeCreatePage from '../pages/EmployeeCreatePage.vue'
import CustomerListPage from '../pages/CustomerListPage.vue'
import CustomerCreatePage from '../pages/CustomerCreatePage.vue'
import PosSalesPage from '../pages/PosSalesPage.vue'
import LoginPage from '../pages/LoginPage.vue'
import RegisterPage from '../pages/RegisterPage.vue'
import PlaceholderPage from '../pages/PlaceholderPage.vue'
import DiscountForm from '@/pages/Discount/DiscountForm.vue'
import VoucherForm from '@/pages/Discount/VoucherForm.vue'
import DiscountFormDetail from '@/pages/Discount/DiscountFormDetail.vue'
import VoucherFormDetail from '@/pages/Discount/VoucherFormDetail.vue'
import DiscountFormUpdate from '@/pages/Discount/DiscountFormUpdate.vue'
import VoucherFormUpdate from '@/pages/Discount/VoucherFormUpdate.vue'

const adminChildren = [
  { path: '', redirect: '/san-pham' },
  { path: 'san-pham', name: 'ProductList', component: ProductListPage, meta: { title: 'Quản lý sản phẩm' } },
  { path: 'san-pham/them', name: 'ProductCreate', component: ProductCreatePage, meta: { title: 'Thêm mới sản phẩm' } },
  { path: 'san-pham/bien-the', name: 'ProductVariant', component: ProductVariantPage, meta: { title: 'Biến thể sản phẩm' } },
  { path: 'bien-the-san-pham', component: ProductVariantPage, meta: { title: 'Biến thể sản phẩm' } },
  { path: 'thuoc-tinh', component: () => import('../pages/AttributeCategoryPage.vue'), meta: { title: 'Danh mục thuộc tính' } },
  { path: 'thuoc-tinh/:category', component: () => import('../pages/AttributeCategoryPage.vue'), meta: { title: 'Danh mục thuộc tính' } },
  { path: 'hoa-don', component: InvoiceListPage, meta: { title: 'Quản lý hóa đơn' } },
  { path: 'hoa-don/them', component: InvoiceFormPage, meta: { title: 'Tạo hóa đơn' } },
  { path: 'hoa-don/:id', component: InvoiceDetailPage, meta: { title: 'Chi tiết hóa đơn' } },
  { path: 'hoa-don/:id/sua', component: InvoiceFormPage, meta: { title: 'Sửa hóa đơn' } },
  { path: 'phieu-giam-gia', component: VoucherListPage, meta: { title: 'Quản lý phiếu giảm giá' } },
  { path: 'phieu-giam-gia/add', component: DiscountForm, meta: { title: 'Thêm phiếu giảm giá' } },
  { path: 'phieu-giam-gia/detail/:id', component: DiscountFormDetail, meta: { title: 'Chi tiết phiếu giảm giá' } },
  { path: 'phieu-giam-gia/update/:id', component: VoucherFormUpdate, meta: { title: 'Cập nhật phiếu giảm giá' } },
  { path: 'dot-giam-gia', component: DiscountEventPage, meta: { title: 'Quản lý đợt giảm giá' } },
  { path: 'dot-giam-gia/add', component: VoucherForm, meta: { title: 'Thêm đợt giảm giá' } },
  { path: 'dot-giam-gia/detail/:id', component: VoucherFormDetail, meta: { title: 'Chi tiết đợt giảm giá' } },
  { path: 'dot-giam-gia/update/:id', component: DiscountFormUpdate, meta: { title: 'Cập nhật đợt giảm giá' } },
  { path: 'giam-gia', redirect: '/phieu-giam-gia' },
  { path: 'thong-ke', component: PlaceholderPage, meta: { title: 'Thống kê' } },
  { path: 'ban-hang', name: 'PosSales', component: PosSalesPage, meta: { title: 'Bán hàng tại quầy' } },
  { path: 'khach-hang', component: CustomerListPage, meta: { title: 'Quản lý khách hàng' } },
  { path: 'khach-hang/them', component: CustomerCreatePage, meta: { title: 'Quản lý khách hàng' } },
  { path: 'khach-hang/:id/sua', component: CustomerCreatePage, meta: { title: 'Quản lý khách hàng' } },
  { path: 'nhan-vien', component: EmployeeListPage, meta: { title: 'Quản lý nhân viên' } },
  { path: 'nhan-vien/them', component: EmployeeCreatePage, meta: { title: 'Quản lý nhân viên' } },
  { path: 'nhan-vien/:id/sua', component: EmployeeCreatePage, meta: { title: 'Quản lý nhân viên' } }
]

const routes = [
  { path: '/dang-nhap', component: LoginPage, meta: { title: 'Đăng nhập' } },
  { path: '/dang-ky', component: RegisterPage, meta: { title: 'Đăng ký' } },
  {
    path: '/',
    component: AdminLayout,
    children: adminChildren
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: adminChildren
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/san-pham'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
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
