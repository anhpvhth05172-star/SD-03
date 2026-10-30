import { createRouter, createWebHistory } from "vue-router";
import {
  getToken,
  getRefreshToken,
  isTokenExpired,
  dangXuat,
} from "../utils/auth";
import { refreshSession } from "../api/http";
import { laAdmin } from "../utils/session";
import AdminLayout from "../layouts/AdminLayout.vue";
import ClientLayout from "../layouts/clients/ClientLayout.vue";
import ProductListPage from "../pages/ProductListPage.vue";
import ProductCreatePage from "../pages/ProductCreatePage.vue";
import ProductVariantPage from "../pages/ProductVariantPage.vue";
import InvoiceListPage from "../pages/InvoiceListPage.vue";
import InvoiceFormPage from "../pages/InvoiceFormPage.vue";
import InvoiceDetailPage from "../pages/InvoiceDetailPage.vue";
import VoucherListPage from "../pages/VoucherListPage.vue";
import DiscountEventPage from "../pages/DiscountEventPage.vue";
import EmployeeListPage from "../pages/EmployeeListPage.vue";
import EmployeeCreatePage from "../pages/EmployeeCreatePage.vue";
import CustomerListPage from "../pages/CustomerListPage.vue";
import CustomerCreatePage from "../pages/CustomerCreatePage.vue";
import PosSalesPage from "../pages/PosSalesPage.vue";
import AttributeCategoryPage from "../pages/AttributeCategoryPage.vue";
import LoginPage from "../pages/LoginPage.vue";
import RegisterPage from "../pages/RegisterPage.vue";
import PlaceholderPage from "../pages/PlaceholderPage.vue";
import DiscountForm from "@/pages/Discount/DiscountForm.vue";
import VoucherForm from "@/pages/Discount/VoucherForm.vue";
import DiscountFormDetail from "@/pages/Discount/DiscountFormDetail.vue";
import VoucherFormDetail from "@/pages/Discount/VoucherFormDetail.vue";
import DiscountFormUpdate from "@/pages/Discount/DiscountFormUpdate.vue";
import VoucherFormUpdate from "@/pages/Discount/VoucherFormUpdate.vue";

// ===== Client (cửa hàng) =====
import Home from "@/pages/clients/views/Home.vue";
import ProductShop from "@/pages/clients/views/ProductShop.vue";
import ProductDetail from "@/pages/clients/views/ProductDetail.vue";
import ProductCart from "@/pages/clients/views/ProductCart.vue";
import CheckOut from "@/pages/clients/views/CheckOut.vue";
import ProductCartDetail from "@/pages/clients/views/ProductCartDetail.vue";
import About from "@/pages/clients/views/About.vue";
import OrderList from "@/pages/clients/views/OrderList.vue";
import OrderLookup from "@/pages/clients/views/OrderLookup.vue";
import Contact from "@/pages/clients/views/Contact.vue";
import VNPayReturn from "@/pages/clients/views/VNPayReturn.vue";

const routes = [
  { path: "/dang-nhap", component: LoginPage, meta: { title: "Đăng nhập" } },
  { path: "/dang-ky", component: RegisterPage, meta: { title: "Đăng ký" } },
  {
    path: "/admin",
    component: AdminLayout,
    children: [
      { path: "", redirect: "/admin/san-pham" },
      { path: "/admin", redirect: "/admin/san-pham" },
      {
        path: "san-pham",
        name: "ProductList",
        component: ProductListPage,
        meta: { title: "Quản lý sản phẩm" },
      },
      {
        path: "san-pham/them",
        name: "ProductCreate",
        component: ProductCreatePage,
        meta: { title: "Thêm mới sản phẩm" },
      },
      {
        path: "san-pham/bien-the",
        name: "ProductVariant",
        component: ProductVariantPage,
        meta: { title: "Biến thể sản phẩm" },
      },
      {
        path: "bien-the-san-pham",
        component: ProductVariantPage,
        meta: { title: "Biến thể sản phẩm" },
      },
      {
        path: "thuoc-tinh",
        component: AttributeCategoryPage,
        meta: { title: "Danh mục thuộc tính" },
      },
      {
        path: "thuoc-tinh/:category",
        component: AttributeCategoryPage,
        meta: { title: "Danh mục thuộc tính" },
      },
      {
        path: "hoa-don",
        component: InvoiceListPage,
        meta: { title: "Quản lý hóa đơn", requiresAuth: true },
      },
      {
        path: "hoa-don/them",
        component: InvoiceFormPage,
        meta: { title: "Tạo hóa đơn", requiresAuth: true },
      },
      {
        path: "hoa-don/:id",
        component: InvoiceDetailPage,
        meta: { title: "Chi tiết hóa đơn", requiresAuth: true },
      },
      {
        path: "hoa-don/:id/sua",
        component: InvoiceFormPage,
        meta: { title: "Sửa hóa đơn", requiresAuth: true },
      },
      {
        path: "phieu-giam-gia",
        component: VoucherListPage,
        meta: { title: "Quản lý phiếu giảm giá" },
      },
      {
        path: "phieu-giam-gia/add",
        component: DiscountForm,
        meta: { title: "Thêm phiếu giảm giá" },
      },
      {
        path: "phieu-giam-gia/detail/:id",
        component: DiscountFormDetail,
        meta: { title: "Chi tiết phiếu giảm giá" },
      },
      {
        path: "phieu-giam-gia/update/:id",
        component: DiscountFormUpdate,
        meta: { title: "Cập nhật phiếu giảm giá" },
      },
      {
        path: "phieu-giam-gia/:id/edit",
        redirect: (to) => `/admin/phieu-giam-gia/update/${to.params.id}`,
      },
      {
        path: "dot-giam-gia",
        component: DiscountEventPage,
        meta: { title: "Quản lý đợt giảm giá", requiresAuth: true },
      },
      {
        path: "dot-giam-gia/add",
        component: VoucherForm,
        meta: { title: "Thêm đợt giảm giá" },
      },
      {
        path: "dot-giam-gia/detail/:id",
        component: VoucherFormDetail,
        meta: { title: "Chi tiết đợt giảm giá" },
      },
      {
        path: "dot-giam-gia/update/:id",
        component: VoucherFormUpdate,
        meta: { title: "Cập nhật đợt giảm giá" },
      },
      {
        path: "dot-giam-gia/edit/:id",
        redirect: (to) => `/admin/dot-giam-gia/update/${to.params.id}`,
      },
      { path: "giam-gia", redirect: "/admin/phieu-giam-gia" },
      {
        path: "thong-ke",
        component: PlaceholderPage,
        meta: { title: "Thống kê", requiresAuth: true },
      },
      {
        path: "ban-hang",
        name: "PosSales",
        component: PosSalesPage,
        meta: { title: "Bán hàng tại quầy", requiresAuth: true },
      },
      {
        path: "khach-hang",
        component: CustomerListPage,
        meta: { title: "Quản lý khách hàng", requiresAuth: true },
      },
      {
        path: "khach-hang/them",
        component: CustomerCreatePage,
        meta: { title: "Quản lý khách hàng", requiresAuth: true },
      },
      {
        path: "khach-hang/:id/sua",
        component: CustomerCreatePage,
        meta: { title: "Quản lý khách hàng", requiresAuth: true },
      },
      {
        path: "nhan-vien",
        component: EmployeeListPage,
        meta: { title: "Quản lý nhân viên", requiresAuth: true },
      },
      {
        path: "nhan-vien/them",
        component: EmployeeCreatePage,
        meta: { title: "Quản lý nhân viên", requiresAuth: true },
      },
      {
        path: "nhan-vien/:id/sua",
        component: EmployeeCreatePage,
        meta: { title: "Quản lý nhân viên", requiresAuth: true },
      },
    ],
  },
  // ===== Client: giao diện khách hàng (layout riêng) =====
  {
    path: "/",
    component: ClientLayout,
    children: [
      {
        path: "/trang-chu",
        name: "ClientHome",
        component: Home,
        meta: { title: "Trang chủ" },
      },
      {
        path: "/client",
        redirect: "/trang-chu",
      },
      {
        path: "/gioi-thieu",
        name: "ClientAbout",
        component: About,
        meta: { title: "Giới thiệu - Poly Shoes" },
      },
      {
        path: "/cua-hang",
        name: "ClientShop",
        component: ProductShop,
        meta: { title: "Sản phẩm" },
      },
      {
        path: "/goi-y",
        redirect: "/cua-hang",
      },
      {
        path: "/cua-hang/:id",
        name: "ClientDetail",
        component: ProductDetail,
        meta: { title: "Chi tiết sản phẩm" },
      },
      {
        path: "/don-hang",
        redirect: "/tra-cuu",
      },
      {
        path: "/don-hang/:id",
        name: "ClientOrderDetail",
        component: ProductCartDetail,
        meta: { title: "Chi tiết đơn hàng" },
      },
      {
        path: "/tra-cuu",
        name: "ClientLookup",
        component: OrderLookup,
        meta: { title: "Tra cứu đơn hàng" },
      },
      {
        path: "/lien-he",
        name: "ClientContact",
        component: Contact,
        meta: { title: "Liên hệ Poly Shoes" },
      },
      {
        path: "/gio-hang",
        name: "ClientCart",
        component: ProductCart,
        meta: { title: "Giỏ hàng" },
      },
      {
        path: "/thanh-toan",
        name: "ClientCheckout",
        component: CheckOut,
        meta: { title: "Thanh toán" },
      },
      {
        path: "/thanh-toan-vnpay",
        name: "ClientVNPayReturn",
        component: VNPayReturn,
        meta: { title: "Ket qua thanh toan VNPAY" },
      },
    ],
  },
  // ===== Backward Compatibility Redirects (URL cũ không có tiền tố /admin) =====
  { path: "/san-pham", redirect: "/admin/san-pham" },
  { path: "/san-pham/them", redirect: "/admin/san-pham/them" },
  { path: "/san-pham/bien-the", redirect: "/admin/san-pham/bien-the" },
  { path: "/bien-the-san-pham", redirect: "/admin/bien-the-san-pham" },
  { path: "/thuoc-tinh", redirect: "/admin/thuoc-tinh" },
  { path: "/thuoc-tinh/:category", redirect: (to) => `/admin/thuoc-tinh/${to.params.category}` },
  { path: "/hoa-don", redirect: "/admin/hoa-don" },
  { path: "/hoa-don/them", redirect: "/admin/hoa-don/them" },
  { path: "/hoa-don/:id", redirect: (to) => `/admin/hoa-don/${to.params.id}` },
  { path: "/hoa-don/:id/sua", redirect: (to) => `/admin/hoa-don/${to.params.id}/sua` },
  { path: "/phieu-giam-gia", redirect: "/admin/phieu-giam-gia" },
  { path: "/phieu-giam-gia/add", redirect: "/admin/phieu-giam-gia/add" },
  { path: "/phieu-giam-gia/detail/:id", redirect: (to) => `/admin/phieu-giam-gia/detail/${to.params.id}` },
  { path: "/phieu-giam-gia/update/:id", redirect: (to) => `/admin/phieu-giam-gia/update/${to.params.id}` },
  { path: "/dot-giam-gia", redirect: "/admin/dot-giam-gia" },
  { path: "/dot-giam-gia/add", redirect: "/admin/dot-giam-gia/add" },
  { path: "/dot-giam-gia/detail/:id", redirect: (to) => `/admin/dot-giam-gia/detail/${to.params.id}` },
  { path: "/dot-giam-gia/update/:id", redirect: (to) => `/admin/dot-giam-gia/update/${to.params.id}` },
  { path: "/thong-ke", redirect: "/admin/thong-ke" },
  { path: "/ban-hang", redirect: "/admin/ban-hang" },
  { path: "/khach-hang", redirect: "/admin/khach-hang" },
  { path: "/khach-hang/them", redirect: "/admin/khach-hang/them" },
  { path: "/khach-hang/:id/sua", redirect: (to) => `/admin/khach-hang/${to.params.id}/sua` },
  { path: "/nhan-vien", redirect: "/admin/nhan-vien" },
  { path: "/nhan-vien/them", redirect: "/admin/nhan-vien/them" },
  { path: "/nhan-vien/:id/sua", redirect: (to) => `/admin/nhan-vien/${to.params.id}/sua` },

  { path: "/", redirect: "/trang-chu" },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/trang-chu",
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

// router.beforeEach(async (to) => {
//   if (!to.meta.requiresAuth) return true

//   const token = getToken()
//   const refreshToken = getRefreshToken()

//   if (!token && !refreshToken) return '/dang-nhap'

//   if (token && !isTokenExpired(token)) return true

//   if (!refreshToken) {
//     dangXuat()
//     return '/dang-nhap'
//   }

//   try {
//     await refreshSession()
//     return true
//   } catch {
//     dangXuat()
//     return '/dang-nhap'
//   }
// })

// router.beforeEach((to) => {
//   if (to.path.startsWith('/admin/nhan-vien') && !laAdmin()) {
//     return '/admin/khach-hang'
//   }
//   return true
// })

export default router;
