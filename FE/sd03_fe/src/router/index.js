import { createRouter, createWebHistory } from 'vue-router'
import ProductListView from '../views/ProductListView.vue'
import ProductCreateView from '../views/ProductCreateView.vue'
import ProductVariantView from '../views/ProductVariantView.vue'

import AdminLayout from '../layouts/AdminLayout.vue'
import InvoiceListPage from '../pages/InvoiceListPage.vue'
import InvoiceFormPage from '../pages/InvoiceFormPage.vue'
import VoucherListPage from '../pages/VoucherListPage.vue'
import DiscountEventPage from '../pages/DiscountEventPage.vue'
import EmployeeListPage from '../pages/EmployeeListPage.vue'
import EmployeeCreatePage from '../pages/EmployeeCreatePage.vue'
import CustomerListPage from '../pages/CustomerListPage.vue'
import CustomerCreatePage from '../pages/CustomerCreatePage.vue'
import ProductListPage from '../pages/ProductListPage.vue'
import ProductVariantPage from '../pages/ProductVariantPage.vue'
import PlaceholderPage from '../pages/PlaceholderPage.vue'

const routes = [
  { path: '/', redirect: '/san-pham' },
  { path: '/san-pham', name: 'ProductList', component: ProductListView },
  { path: '/san-pham/them', name: 'ProductCreate', component: ProductCreateView },
  { path: '/san-pham/bien-the', name: 'ProductVariant', component: ProductVariantView },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      { path: 'hoa-don', component: InvoiceListPage, meta: { title: 'Quản lý hóa đơn' } },
      { path: 'hoa-don/them', component: InvoiceFormPage, meta: { title: 'Tạo hóa đơn' } },
      { path: 'hoa-don/:id/sua', component: InvoiceFormPage, meta: { title: 'Sửa hóa đơn' } },
      {
        path: 'phieu-giam-gia',
        component: VoucherListPage,
        meta: { title: 'Quản lý phiếu giảm giá' },
      },
      {
        path: 'dot-giam-gia',
        component: DiscountEventPage,
        meta: { title: 'Quản lý đợt giảm giá' },
      },
      { path: 'giam-gia', redirect: '/phieu-giam-gia' },
      { path: 'thong-ke', component: PlaceholderPage, meta: { title: 'Thống kê' } },
      { path: 'ban-hang', component: PlaceholderPage, meta: { title: 'Bán hàng tại quầy' } },
      { path: 'khach-hang', component: CustomerListPage, meta: { title: 'Quản lý khách hàng' } },
      { path: 'khach-hang/them', component: CustomerCreatePage, meta: { title: 'Quản lý khách hàng' } },
      { path: 'nhan-vien', component: EmployeeListPage, meta: { title: 'Quản lý nhân viên' } },
      { path: 'nhan-vien/them', component: EmployeeCreatePage, meta: { title: 'Quản lý nhân viên' } },
      { path: 'san-pham', component: ProductListView, meta: { title: 'Quản lý sản phẩm' } },
      { path: 'bien-the-san-pham', component: ProductVariantView, meta: { title: 'Biến thể sản phẩm' } },
      { path: 'thuoc-tinh', component: () => import('../views/AttributeCategoryView.vue'), meta: { title: 'Danh mục thuộc tính' } },
      { path: 'thuoc-tinh/:tab', component: () => import('../views/AttributeCategoryView.vue'), meta: { title: 'Danh mục thuộc tính' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
