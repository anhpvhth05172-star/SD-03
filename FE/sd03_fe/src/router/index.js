import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'
import InvoiceListPage from '../pages/InvoiceListPage.vue'
import VoucherListPage from '../pages/VoucherListPage.vue'
import DiscountEventPage from '../pages/DiscountEventPage.vue'
import PlaceholderPage from '../pages/PlaceholderPage.vue'

const routes = [
  { path: '/', redirect: '/hoa-don' },
  {
    path: '/',
    component: AdminLayout,
    children: [
      { path: 'hoa-don', component: InvoiceListPage, meta: { title: 'Quản lý hóa đơn' } },
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
      { path: 'san-pham', component: PlaceholderPage, meta: { title: 'Sản phẩm' } },
      {
        path: 'bien-the-san-pham',
        component: PlaceholderPage,
        meta: { title: 'Biến thể sản phẩm' },
      },
      { path: 'khach-hang', component: PlaceholderPage, meta: { title: 'Quản lý khách hàng' } },
      { path: 'nhan-vien', component: PlaceholderPage, meta: { title: 'Quản lý nhân viên' } },
      { path: ':pathMatch(.*)*', redirect: '/hoa-don' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
