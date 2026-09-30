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
      { path: 'hoa-don', component: InvoiceListPage, meta: { title: 'Quản Lý Hóa Đơn' } },
      { path: 'tong-quan', component: PlaceholderPage, meta: { title: 'Tổng Quan' } },
      { path: 'ban-hang', component: PlaceholderPage, meta: { title: 'Bán Hàng Tại Quầy' } },
      { path: 'san-pham', component: PlaceholderPage, meta: { title: 'Quản Lý Sản Phẩm' } },
      { path: 'tai-khoan', component: PlaceholderPage, meta: { title: 'Quản Lý Tài Khoản' } },
      { path: 'khuyen-mai', component: PlaceholderPage, meta: { title: 'Khuyến Mãi' } },
      { path: ':pathMatch(.*)*', redirect: '/hoa-don' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
