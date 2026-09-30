import { createRouter, createWebHistory } from 'vue-router'
import ProductListView from '../views/ProductListView.vue'
import ProductCreateView from '../views/ProductCreateView.vue'
import ProductVariantView from '../views/ProductVariantView.vue'

import AdminLayout from '../layouts/AdminLayout.vue'
import InvoiceListPage from '../pages/InvoiceListPage.vue'
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
      { path: 'thong-ke', component: PlaceholderPage, meta: { title: 'Thống kê' } },
      { path: 'ban-hang', component: PlaceholderPage, meta: { title: 'Bán hàng tại quầy' } },
      { path: 'khach-hang', component: PlaceholderPage, meta: { title: 'Quản lý khách hàng' } },
      { path: 'nhan-vien', component: PlaceholderPage, meta: { title: 'Quản lý nhân viên' } },
      { path: 'giam-gia', component: PlaceholderPage, meta: { title: 'Quản lý giảm giá' } },
      { path: 'thuoc-tinh', component: PlaceholderPage, meta: { title: 'Danh mục thuộc tính' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
