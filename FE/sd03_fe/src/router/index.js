import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'
import InvoiceListPage from '../pages/InvoiceListPage.vue'
import PlaceholderPage from '../pages/PlaceholderPage.vue'

const routes = [
  { path: '/', redirect: '/hoa-don' },
  {
    path: '/',
    component: AdminLayout,
    children: [
      { path: 'hoa-don', component: InvoiceListPage, meta: { title: 'Quản lý hóa đơn' } },
      { path: 'thong-ke', component: PlaceholderPage, meta: { title: 'Thống kê' } },
      { path: 'ban-hang', component: PlaceholderPage, meta: { title: 'Bán hàng tại quầy' } },
      { path: 'san-pham', component: PlaceholderPage, meta: { title: 'Quản lý sản phẩm' } },
      { path: 'khach-hang', component: PlaceholderPage, meta: { title: 'Quản lý khách hàng' } },
      { path: 'nhan-vien', component: PlaceholderPage, meta: { title: 'Quản lý nhân viên' } },
      { path: 'giam-gia', component: PlaceholderPage, meta: { title: 'Quản lý giảm giá' } },
      { path: 'thuoc-tinh', component: PlaceholderPage, meta: { title: 'Danh mục thuộc tính' } },
      { path: ':pathMatch(.*)*', redirect: '/hoa-don' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
