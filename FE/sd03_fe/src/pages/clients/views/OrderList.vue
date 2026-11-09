<script setup>
import { ref, computed, watch } from "vue";
import { useRoute } from "vue-router";
import * as store from "../store";
import { getToken as getSessionToken, getUser } from "@/utils/auth";

const { money } = store;
const route = useRoute();

// Đường dẫn trang tra cứu đơn hàng (đổi cho khớp router của bạn)
const LOOKUP_ROUTE = "/tra-cuu";

/* ================= CẤU HÌNH API ================= */
const HOA_DON_API = "/api/hoa-don";

const getToken = () => {
  const sessionToken = getSessionToken();
  if (sessionToken) return sessionToken;
  for (const area of [localStorage, sessionStorage]) {
    const t = area.getItem("token") || area.getItem("accessToken");
    if (t) return t;
  }
  return "";
};
const authHeaders = () => {
  const t = getToken();
  return t ? { Authorization: `Bearer ${t}` } : {};
};
async function getJson(url) {
  const res = await fetch(url, { headers: authHeaders() });
  if (!res.ok) {
    const e = new Error(String(res.status));
    e.status = res.status;
    throw e;
  }
  return res.json();
}

/* >>> ADAPTER: đổi tên field cho khớp HoaDonDTO (giống trang tra cứu) <<< */
const HD = {
  id: (h) => h.id,
  customerId: (h) => h.idKhachHang ?? h.khachHang?.id ?? h.khachHangId,
  code: (h) => h.maHoaDon ?? h.ma ?? h.id,
  date: (h) => h.ngayTao ?? h.ngayDat ?? h.createdAt,
  status: (h) => String(h.trangThai ?? "").toUpperCase(),
  name: (h) => h.tenKhachHang ?? h.khachHang?.tenKhachHang ?? "",
  phone: (h) =>
    h.soDienThoaiKhachHang ?? h.soDienThoai ?? h.khachHang?.soDienThoai ?? "",
  address: (h) => h.diaChiNhanHang ?? h.diaChi ?? "",
  note: (h) => h.ghiChu ?? "",
  payId: (h) => h.idPhuongThucThanhToan ?? h.phuongThucThanhToan?.id,
  payName: (h) => h.tenPhuongThucThanhToan ?? h.phuongThucThanhToan?.ten ?? "",
  ship: (h) => Number(h.phiVanChuyen ?? 0),
  discount: (h) => Number(h.tienGiam ?? h.giamGia ?? h.soTienGiam ?? 0),
  total: (h) => Number(h.tongTien ?? h.tongTienSauGiam ?? h.thanhTien ?? 0),
  items: (h) => h.chiTiet ?? h.danhSachChiTiet ?? h.hoaDonChiTiets ?? [],
};
const HDI = {
  name: (i) => i.tenSanPham ?? i.sanPham?.tenSanPham ?? i.name ?? "",
  brand: (i) => i.thuongHieu ?? i.tenThuongHieu ?? i.brand ?? "",
  size: (i) => i.kichCo ?? i.tenKichCo ?? i.size ?? "",
  qty: (i) => Number(i.soLuong ?? i.qty ?? 0),
  price: (i) => Number(i.donGia ?? i.price ?? 0),
  image: (i) => i.hinhAnh ?? i.img ?? null,
};

const PAY_LABEL = {
  11: "Thanh toán khi nhận hàng (COD)",
  2: "Chuyển khoản ngân hàng",
  4: "Thanh toán qua VNPAY",
};

const STATUS = {
  CHO_THANH_TOAN: {
    label: "Chờ thanh toán",
    cls: "bg-warning-subtle text-warning-emphasis",
  },
  CHO_XAC_NHAN: {
    label: "Chờ xác nhận",
    cls: "bg-warning-subtle text-warning-emphasis",
  },
  DA_XAC_NHAN: { label: "Đã xác nhận", cls: "bg-primary-subtle text-primary" },
  CHO_GIAO_HANG: { label: "Chờ giao hàng", cls: "bg-info-subtle text-info" },
  DANG_GIAO: {
    label: "Đang vận chuyển",
    cls: "bg-primary-subtle text-primary",
  },
  DANG_GIAO_HANG: {
    label: "Đang vận chuyển",
    cls: "bg-primary-subtle text-primary",
  },
  GIAO_HANG_THAT_BAI: {
    label: "Giao thất bại",
    cls: "bg-warning-subtle text-warning-emphasis",
  },
  HOAN_THANH: {
    label: "Giao thành công",
    cls: "bg-success-subtle text-success",
  },
  DA_GIAO: { label: "Giao thành công", cls: "bg-success-subtle text-success" },
  DA_HOAN_THANH: {
    label: "Đã hoàn thành",
    cls: "bg-success-subtle text-success",
  },
  DA_HUY: { label: "Đã hủy", cls: "bg-danger-subtle text-danger" },
  DA_HOAN_TIEN: { label: "Đã hoàn tiền", cls: "bg-danger-subtle text-danger" },
};
const statusInfo = (code) =>
  STATUS[code] || {
    label: code || "Đang xử lý",
    cls: "bg-secondary-subtle text-secondary",
  };

function fmtDate(raw) {
  if (!raw) return "";
  const d = new Date(raw);
  return isNaN(d) ? String(raw) : d.toLocaleString("vi-VN");
}

function normalize(h) {
  const payId = HD.payId(h);
  const items = HD.items(h).map((i) => ({
    name: HDI.name(i),
    brand: HDI.brand(i),
    size: HDI.size(i),
    qty: HDI.qty(i),
    price: HDI.price(i),
    image: HDI.image(i),
  }));
  const subtotal = items.reduce((s, i) => s + i.price * i.qty, 0);
  const ship = HD.ship(h);
  const discount = HD.discount(h);
  return {
    id: HD.id(h),
    customerId: HD.customerId(h),
    code: String(HD.code(h)),
    date: fmtDate(HD.date(h)),
    status: HD.status(h),
    info: {
      name: HD.name(h),
      phone: HD.phone(h),
      address: HD.address(h),
      note: HD.note(h),
      pay:
        PAY_LABEL[payId] || HD.payName(h) || "Thanh toán khi nhận hàng (COD)",
    },
    items,
    subtotal,
    ship,
    discount,
    // Ưu tiên tổng tiền do BE tính; không có thì tự cộng
    total: HD.total(h) || subtotal + ship - discount,
  };
}

/* ================= TẢI ĐƠN HÀNG ================= */
const order = ref(null);
const loading = ref(false);
const error = ref("");

function decodeJwt(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(decodeURIComponent(escape(atob(payload))));
  } catch (e) {
    return null;
  }
}

const KHACH_HANG_API = "http://localhost:8080/api/khach-hang";

// Thông tin đăng nhập hiện có (id có thể chưa có, chỉ có tên đăng nhập)
function getLoginInfo() {
  const u = getUser();
  const p = decodeJwt(getToken());
  return {
    loggedIn: !!(u || getToken()),
    id: u?.idKhachHang ?? u?.id ?? p?.idKhachHang ?? p?.id ?? p?.userId ?? null,
    username:
      u?.tenTaiKhoan ??
      u?.username ??
      p?.sub ??
      p?.username ??
      null,
    email: u?.email ?? null,
  };
}

// Xác định id khách hàng: ưu tiên id có sẵn, không có thì tra qua API thong-tin
async function resolveCustomerId() {
  const info = getLoginInfo();
  if (info.id != null) return info.id;
  try {
    const profile = await getJson("/api/auth/thong-tin");
    return profile?.id ?? null;
  } catch (e) {
    console.warn("[Auth] Không tra được id khách hàng:", e.message);
    return null;
  }
}

async function loadOrder(id) {
  order.value = null;
  error.value = "";
  if (!id) return;

  loading.value = true;
  const myId = await resolveCustomerId();
  if (!myId) {
    error.value = getLoginInfo().loggedIn
      ? "Không xác định được tài khoản của bạn, vui lòng đăng nhập lại."
      : "Vui lòng đăng nhập để xem chi tiết đơn hàng.";
    loading.value = false;
    return;
  }

  try {
    const o = normalize(await getJson(`${HOA_DON_API}/cua-toi/${id}`));
    // Chỉ cho xem đơn của chính mình; đơn người khác hiển thị như "không tìm thấy"
    if (String(o.customerId) === String(myId)) order.value = o;
  } catch (e) {
    console.error("[ChiTietDon] Lỗi tải đơn:", e);
    if (e.status === 404) {
      error.value = "";
    } else if (e.status === 401 || e.status === 403) {
      error.value = "Bạn chưa có quyền xem đơn hàng này, vui lòng đăng nhập.";
    } else if (e.message === "Failed to fetch") {
      error.value = "Không kết nối được máy chủ, vui lòng thử lại.";
    } else {
      error.value = "Không tải được đơn hàng, vui lòng thử lại.";
    }
  } finally {
    loading.value = false;
  }
}

watch(() => route.params.id, loadOrder, { immediate: true });

const badge = computed(() => statusInfo(order.value?.status));
const lookupLink = computed(() =>
  order.value
    ? { path: LOOKUP_ROUTE, query: { code: order.value.code } }
    : LOOKUP_ROUTE,
);
</script>

<template>
  <!-- ===== Đang tải ===== -->
  <div v-if="loading" class="ps-card text-center p-5">
    <div class="text-muted-ps">Đang tải đơn hàng...</div>
  </div>

  <!-- ===== Chi tiết đơn hàng ===== -->
  <div v-else-if="order" class="ps-order-detail">
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      /
      <RouterLink :to="LOOKUP_ROUTE" class="text-muted-ps text-decoration-none"
        >Tra cứu đơn hàng</RouterLink
      >
      /
      <span class="text-dark fw-semibold"
        >Chi tiết đơn hàng #{{ order.code }}</span
      >
    </nav>

    <div class="row g-4">
      <div class="col-lg-8">
        <div class="ps-card p-4 mb-4">
          <div
            class="d-flex justify-content-between align-items-center border-bottom pb-3 mb-3"
          >
            <div>
              <h4 class="mb-1">Đơn hàng #{{ order.code }}</h4>
              <div class="text-muted-ps small">
                Thời gian đặt: {{ order.date }}
              </div>
            </div>
            <span class="badge fs-6 px-3 py-2 rounded-pill" :class="badge.cls">
              {{ badge.label }}
            </span>
          </div>

          <h5 class="mb-3">Danh sách sản phẩm</h5>
          <div
            v-for="(i, idx) in order.items"
            :key="idx + '-' + i.size"
            class="d-flex align-items-center gap-3 py-3"
            :class="{ 'border-top': idx }"
          >
            <div
              class="ps-thumb flex-shrink-0 overflow-hidden"
              style="width: 70px; height: 70px; font-size: 2rem"
            >
              <img
                v-if="i.image"
                :src="i.image"
                :alt="i.name"
                style="width: 100%; height: 100%; object-fit: cover"
              />
              <template v-else>👟</template>
            </div>
            <div class="flex-grow-1">
              <div class="fw-bold">{{ i.name }}</div>
              <div class="small text-muted-ps">
                <template v-if="i.brand">{{ i.brand }} · </template>
                Size: {{ i.size }} · SL: {{ i.qty }}
              </div>
            </div>
            <div class="text-end">
              <div class="price">{{ money(i.price * i.qty) }}</div>
              <div class="small text-muted-ps">{{ money(i.price) }} / đôi</div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-4">
        <div class="ps-card p-4 mb-4">
          <h5 class="mb-3">Thông tin nhận hàng</h5>
          <div class="mb-2">
            <span class="text-muted-ps small d-block">Người nhận</span>
            <strong>{{ order.info.name }}</strong>
          </div>
          <div class="mb-2">
            <span class="text-muted-ps small d-block">Số điện thoại</span>
            <span>{{ order.info.phone }}</span>
          </div>
          <div class="mb-2">
            <span class="text-muted-ps small d-block">Địa chỉ</span>
            <span>{{ order.info.address }}</span>
          </div>
          <div v-if="order.info.note" class="mb-2">
            <span class="text-muted-ps small d-block">Ghi chú</span>
            <span>{{ order.info.note }}</span>
          </div>
          <div class="mb-3">
            <span class="text-muted-ps small d-block"
              >Hình thức thanh toán</span
            >
            <span>{{ order.info.pay }}</span>
          </div>

          <hr />

          <div class="d-flex justify-content-between mb-2">
            <span class="text-muted-ps">Tạm tính</span>
            <span>{{ money(order.subtotal) }}</span>
          </div>
          <div class="d-flex justify-content-between mb-2">
            <span class="text-muted-ps">Phí giao hàng</span>
            <span v-if="order.ship">{{ money(order.ship) }}</span>
            <span v-else class="text-success">Miễn phí</span>
          </div>
          <div
            v-if="order.discount"
            class="d-flex justify-content-between mb-2 text-success"
          >
            <span>Giảm giá</span>
            <span>-{{ money(order.discount) }}</span>
          </div>
          <div class="d-flex justify-content-between border-top pt-3 mb-4">
            <span class="fw-bold">Tổng thanh toán</span>
            <span class="price fs-5">{{ money(order.total) }}</span>
          </div>

          <div class="d-flex flex-column gap-2">
            <RouterLink
              :to="lookupLink"
              class="btn btn-outline-primary w-100 py-2"
            >
              Theo dõi tiến độ đơn hàng
            </RouterLink>
            <RouterLink to="/cua-hang" class="btn btn-primary w-100 py-2">
              Tiếp tục mua hàng
            </RouterLink>
            <RouterLink
              to="/trang-chu"
              class="btn btn-light-border w-100 py-2 text-center text-decoration-none"
            >
              Về trang chủ
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- ===== Không tìm thấy / lỗi ===== -->
  <div v-else class="ps-card text-center p-5">
    <div class="fs-1">🔍</div>
    <h4 class="mt-2">
      {{ error ? "Không tải được đơn hàng" : "Không tìm thấy đơn hàng" }}
    </h4>
    <p class="text-muted-ps">
      {{ error || "Mã đơn hàng không tồn tại hoặc đã bị xóa." }}
    </p>
    <div class="d-flex justify-content-center gap-2 mt-2">
      <button
        v-if="error"
        class="btn btn-outline-primary px-4"
        @click="loadOrder(route.params.id)"
      >
        Thử lại
      </button>
      <RouterLink :to="LOOKUP_ROUTE" class="btn btn-outline-primary px-4">
        Tra cứu đơn hàng
      </RouterLink>
      <RouterLink to="/cua-hang" class="btn btn-primary px-4">
        Quay lại cửa hàng
      </RouterLink>
    </div>
  </div>
</template>
