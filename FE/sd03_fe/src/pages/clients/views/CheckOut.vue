<script setup>
import { reactive, ref, computed, watch, onMounted, unref } from "vue";
import * as store from "../store";

const { cart, cartTotal, money } = store;

// Phí vận chuyển đơn online (back-end yêu cầu > 1.000đ). Đổi theo chính sách của shop.
const SHIP_FEE = 30000;

/* ================= CẤU HÌNH API ================= */
const API = "http://localhost:8080/api/khach-hang";

const getToken = () => {
  const a = unref(store.auth);
  if (a?.token) return a.token;
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
  if (!res.ok) throw new Error(res.status);
  return res.json();
}

// Giải mã JWT -> lấy username / id
function decodeJwt(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(decodeURIComponent(escape(atob(payload))));
  } catch (e) {
    return null;
  }
}

function getLoggedInUser() {
  // 1) store.auth (nếu có)
  const a = unref(store.auth);
  if (a) {
    const u = unref(a.user) || a;
    if (u && (u.id || u.tenTaiKhoan || u.username || u.email)) return u;
  }

  // 2) quét toàn bộ localStorage + sessionStorage
  for (const area of [localStorage, sessionStorage]) {
    for (let i = 0; i < area.length; i++) {
      const key = area.key(i);
      const raw = area.getItem(key);
      if (!raw) continue;

      // Chuỗi là JWT
      if (raw.split(".").length === 3) {
        const p = decodeJwt(raw);
        if (p)
          return { tenTaiKhoan: p.sub || p.username, id: p.id || p.userId };
      }
      // Chuỗi là JSON
      try {
        const obj = JSON.parse(raw);
        const u = obj.user || obj.khachHang || obj.data || obj;
        if (
          u &&
          typeof u === "object" &&
          (u.id || u.tenTaiKhoan || u.username || u.email)
        ) {
          return u;
        }
        if (obj.token) {
          const p = decodeJwt(obj.token);
          if (p)
            return { tenTaiKhoan: p.sub || p.username, id: p.id || p.userId };
        }
      } catch (e) {
        /* không phải JSON */
      }
    }
  }
  return null;
}

async function fetchCustomer() {
  const u = getLoggedInUser();
  console.log("[Checkout] user đăng nhập:", u);
  if (!u) {
    console.warn(
      "[Checkout] Không tìm thấy user đăng nhập trong store/localStorage",
    );
    return null;
  }

  const username = u.tenTaiKhoan || u.username;
  const matches = (k) =>
    (u.id && k.id === u.id) ||
    (username && k.tenTaiKhoan === username) ||
    (u.email && k.email === u.email);

  // 1) API chi tiết theo id
  if (u.id) {
    try {
      const c = await getJson(`${API}/${u.id}`);
      console.log("[Checkout] khách hàng (chi tiết):", c);
      return c;
    } catch (e) {
      console.warn("[Checkout] API chi tiết lỗi:", e.message);
    }
  }

  // 2) Tìm trong danh sách
  try {
    const page = await getJson(`${API}?size=1000`);
    const list = page.content || page;
    const c = list.find(matches) || null;
    console.log("[Checkout] khách hàng (từ danh sách):", c);
    return c;
  } catch (e) {
    console.warn("[Checkout] API danh sách lỗi (401/CORS?):", e.message);
    return u.tenKhachHang ? u : null;
  }
}

const customer = ref(null);
onMounted(async () => {
  try {
    customer.value = await fetchCustomer();
  } catch (e) {
    console.error("Không tải được khách hàng:", e);
  }
});

/* ============== ADAPTER theo JSON thực tế ============== */
const currentUser = computed(() => customer.value);
const isLoggedIn = computed(() => !!customer.value);

const userInfo = (u) => ({
  name: u.tenKhachHang || "",
  phone: u.soDienThoai || "",
});

const fullAddress = (a) =>
  [a.diaChiCuThe, a.phuong, a.thanhPho].filter(Boolean).join(", ");

const savedAddresses = computed(() => {
  const c = customer.value;
  if (!c) return [];
  const list = c.danhSachDiaChi?.length
    ? c.danhSachDiaChi
    : c.diaChiMacDinh
      ? [c.diaChiMacDinh]
      : [];
  return list
    .filter((a) => a.trangThai !== false)
    .map((a) => ({
      id: a.id,
      name: a.tenChiChi || c.tenKhachHang || "",
      phone: c.soDienThoai || "",
      address: fullAddress(a),
      isDefault: !!a.macDinh,
    }))
    .sort((x, y) => Number(y.isDefault) - Number(x.isDefault)); // mặc định lên đầu
});

const itemImage = (i) =>
  i.image || i.hinhAnh || i.img || "/placeholder-shoe.png";

/* ================= PHIẾU GIẢM GIÁ (gọi API thật) ================= */
const VOUCHER_API = "http://localhost:8080/admin/phieu-giam-gia";

/* >>> ADAPTER: đổi tên field cho khớp entity/DTO PhieuGiamGia của bạn <<< */
const VF = {
  id: (v) => v.id,
  code: (v) => v.maPhieuGiamGia,
  loai: (v) => String(v.loaiGiamGia ?? "").toUpperCase(), // "PERCENT" => theo %, còn lại => số tiền
  value: (v) => Number(v.giaTriGiam ?? 0),
  max: (v) => Number(v.giamToiDa ?? 0),
  min: (v) => Number(v.hoaDonToiThieu ?? 0),
  start: (v) => v.ngayBatDau,
  end: (v) => v.ngayKetThuc,
  // số lượt còn lại (undefined nếu phiếu không giới hạn)
  qty: (v) =>
    v.soLuong != null
      ? Number(v.soLuong) - Number(v.soLuongDaSuDung ?? 0)
      : undefined,
  active: (v) => v.trangThai,
  scope: (v) => String(v.phamVi ?? "").toUpperCase(), // chứa CA_NHAN/KHACH_HANG => riêng từng khách
  // danh sách id khách được áp dụng (lấy từ API detail)
  customerIds: (d) =>
    (d.danhSachKhachHang || d.khachHangIds || d.khachHang || []).map((k) =>
      typeof k === "object" ? (k.id ?? k.idKhachHang) : k,
    ),
};

const isPercent = (loai) => /PHAN_TRAM|PERCENT|%/.test(loai);
const isPersonal = (scope) => /CA_NHAN|KHACH_HANG|RIENG|PRIVATE/.test(scope);

// Kiểm tra điều kiện của 1 phiếu. Trả về chuỗi lỗi, hoặc "" nếu hợp lệ.
// ignoreMin = true: bỏ qua điều kiện "đơn tối thiểu" (dùng khi liệt kê phiếu gợi ý)
function checkVoucher(detail, ignoreMin = false) {
  const act = VF.active(detail);
  if (act === false || /NGUNG|TAT|INACTIVE|HET_HAN/i.test(String(act)))
    return "Mã giảm giá đang bị tắt hoặc đã hết hạn.";

  const now = new Date();
  const st = VF.start(detail);
  const en = VF.end(detail);
  if (st && now < new Date(st))
    return "Mã giảm giá chưa đến thời gian áp dụng.";
  if (en && now > new Date(en)) return "Mã giảm giá đã hết hạn.";

  const qty = VF.qty(detail);
  if (qty !== undefined && qty !== null && Number(qty) <= 0)
    return "Mã giảm giá đã hết lượt sử dụng.";

  if (isPersonal(VF.scope(detail))) {
    const cid = customer.value?.id;
    if (!cid) return "Vui lòng đăng nhập để dùng mã này.";
    if (!VF.customerIds(detail).includes(cid))
      return "Mã này không áp dụng cho tài khoản của bạn.";
  }

  const min = VF.min(detail);
  if (!ignoreMin && orderTotal.value < min)
    return `Đơn tối thiểu ${money(min)} để dùng mã này.`;

  return "";
}

// Chuẩn hóa dữ liệu phiếu thành { id, code, type, value, max, min }
function toVoucher(detail) {
  return {
    id: VF.id(detail),
    code: String(VF.code(detail)).toUpperCase(),
    type: isPercent(VF.loai(detail)) ? "percent" : "fixed",
    value: VF.value(detail),
    max: VF.max(detail),
    min: VF.min(detail),
  };
}

// Số tiền giảm của 1 phiếu với tổng đơn `total`
function calcDiscount(v, total) {
  if (!v) return 0;
  if (total < (v.min || 0)) return 0;
  let d = v.type === "percent" ? (total * v.value) / 100 : v.value;
  if (v.max > 0) d = Math.min(d, v.max);
  return Math.max(0, Math.min(Math.floor(d), total));
}

// Lấy chi tiết 1 phiếu (có danh sách khách hàng được gắn)
async function getVoucherDetail(v) {
  try {
    return { ...v, ...(await getJson(`${VOUCHER_API}/detail/${VF.id(v)}`)) };
  } catch (e) {
    return v; // dùng dữ liệu từ danh sách
  }
}

// Trả về { ok: true, voucher } hoặc { ok: false, message }  (nhập mã tay)
async function fetchVoucher(code) {
  // Back-end chỉ cho dùng phiếu giảm giá khi có khách hàng
  if (!customer.value?.id)
    return { ok: false, message: "Vui lòng đăng nhập để dùng phiếu giảm giá." };

  // 1) Tìm phiếu theo mã
  let found = null;
  try {
    const page = await getJson(
      `${VOUCHER_API}?keyword=${encodeURIComponent(code)}&size=50`,
    );
    const list = page.content || page;
    found = list.find((v) => String(VF.code(v)).toUpperCase() === code);
  } catch (e) {
    return { ok: false, message: "Không kiểm tra được mã, vui lòng thử lại." };
  }
  if (!found) return { ok: false, message: "Mã giảm giá không tồn tại." };

  // 2) Lấy chi tiết + 3) Kiểm tra điều kiện
  const detail = await getVoucherDetail(found);
  const msg = checkVoucher(detail);
  if (msg) return { ok: false, message: msg };

  return { ok: true, voucher: toVoucher(detail) };
}
/* ======================================================= */

const f = reactive({ name: "", phone: "", address: "", note: "", pay: "cod" });
const errors = reactive({});
const done = ref(false);
const orderId = ref("");

/* ---------- Tự điền khi đã đăng nhập ---------- */
const selectedAddr = ref("new");

function chooseAddress(id) {
  selectedAddr.value = id;
  const u = currentUser.value;
  if (!u) return;
  const base = userInfo(u);
  if (id === "new") {
    f.name = base.name;
    f.phone = base.phone;
    f.address = "";
    return;
  }
  const a = savedAddresses.value.find((x) => x.id === id);
  if (!a) return;
  f.name = a.name || base.name;
  f.phone = a.phone || base.phone;
  f.address = a.address;
}

/* ---------- Khóa ô nhập khi đã tự điền ---------- */
// Họ tên, SĐT: khóa nếu thông tin lấy được từ tài khoản (SĐT trống thì vẫn cho nhập)
const lockName = computed(
  () => isLoggedIn.value && !!userInfo(currentUser.value).name,
);
const lockPhone = computed(
  () => isLoggedIn.value && !!userInfo(currentUser.value).phone,
);
// Địa chỉ: khóa khi đang dùng địa chỉ đã lưu, chỉ nhập được khi chọn "Nhập địa chỉ mới"
const lockAddress = computed(
  () => isLoggedIn.value && selectedAddr.value !== "new",
);

// Chạy khi dữ liệu khách hàng tải xong
watch(
  currentUser,
  (u) => {
    if (!u) return;
    const def = savedAddresses.value[0]; // đã sort: mặc định đứng đầu
    chooseAddress(def ? def.id : "new");
  },
  { immediate: true },
);

/* ---------- Voucher ---------- */
const voucherCode = ref("");
const voucher = ref(null); // { code, ...rule }
const voucherMsg = reactive({ text: "", ok: false });

// Tổng trước giảm = tiền hàng + phí vận chuyển (khớp cách tính của back-end)
const orderTotal = computed(() => cartTotal.value + SHIP_FEE);

const discount = computed(() => calcDiscount(voucher.value, orderTotal.value));
const finalTotal = computed(() => orderTotal.value - discount.value);

const applying = ref(false);
async function applyVoucher() {
  const code = voucherCode.value.trim().toUpperCase();
  if (!code) {
    voucherMsg.text = "Vui lòng nhập mã giảm giá.";
    voucherMsg.ok = false;
    return;
  }
  applying.value = true;
  const r = await fetchVoucher(code);
  applying.value = false;
  if (!r.ok) {
    voucher.value = null;
    voucherMsg.text = r.message;
    voucherMsg.ok = false;
    return;
  }
  voucher.value = r.voucher;
  voucherMsg.text = "Áp dụng mã thành công!";
  voucherMsg.ok = true;
}
function removeVoucher() {
  voucher.value = null;
  voucherCode.value = "";
  voucherMsg.text = "";
}
// Giỏ thay đổi làm không còn đủ điều kiện -> gỡ voucher
watch(orderTotal, () => {
  if (voucher.value && orderTotal.value < (voucher.value.min || 0)) {
    removeVoucher();
    voucherMsg.text = "Mã giảm giá đã bị gỡ vì đơn hàng không đủ điều kiện.";
    voucherMsg.ok = false;
  }
});

/* ---------- Danh sách phiếu gợi ý theo giá trị đơn hàng ---------- */
const allVouchers = ref([]); // chi tiết các phiếu tải từ API
const loadingVouchers = ref(false);

async function loadVouchers() {
  if (!customer.value?.id) {
    allVouchers.value = [];
    return;
  }
  loadingVouchers.value = true;
  try {
    const page = await getJson(`${VOUCHER_API}?size=100`);
    const list = page.content || page;
    allVouchers.value = await Promise.all(list.map(getVoucherDetail));
  } catch (e) {
    console.warn("[Checkout] Không tải được danh sách phiếu:", e.message);
    allVouchers.value = [];
  } finally {
    loadingVouchers.value = false;
  }
}
// Tải lại khi đã có thông tin khách hàng
watch(() => customer.value?.id, loadVouchers, { immediate: true });

// Phiếu còn hiệu lực, kèm trạng thái đủ/chưa đủ điều kiện theo tổng đơn hiện tại
const voucherOptions = computed(() => {
  const total = orderTotal.value;
  const list = allVouchers.value
    .filter((d) => !checkVoucher(d, true))
    .map((d) => {
      const v = toVoucher(d);
      const eligible = total >= (v.min || 0);
      const end = VF.end(d);
      return {
        v,
        eligible,
        discount: eligible ? calcDiscount(v, total) : 0,
        missing: Math.max(0, (v.min || 0) - total),
        expire: end ? new Date(end).toLocaleDateString("vi-VN") : "",
        best: false,
      };
    });
  // Đủ điều kiện lên trước, giảm nhiều nhất lên đầu
  list.sort(
    (x, y) =>
      Number(y.eligible) - Number(x.eligible) || y.discount - x.discount,
  );
  const best = list.find((o) => o.eligible && o.discount > 0);
  if (best) best.best = true;
  return list;
});

const bestVoucher = computed(() => voucherOptions.value.find((o) => o.best));

const voucherLabel = (v) =>
  v.type === "percent"
    ? `Giảm ${v.value}%${v.max > 0 ? ` (tối đa ${money(v.max)})` : ""}`
    : `Giảm ${money(v.value)}`;

// Bấm vào phiếu: chọn / bỏ chọn
function toggleVoucher(o) {
  if (!o.eligible) return;
  if (voucher.value?.id === o.v.id) {
    removeVoucher();
    return;
  }
  voucher.value = o.v;
  voucherCode.value = "";
  voucherMsg.text = "Áp dụng mã thành công!";
  voucherMsg.ok = true;
}

/* ---------- Đặt hàng: lưu vào hóa đơn (POST /api/hoa-don) ---------- */
const HOA_DON_API = "http://localhost:8080/api/hoa-don";
const ID_NHAN_VIEN_ONLINE = null; // null = không gán nhân viên (đơn online)
const orderCode = ref("");
const submitting = ref(false);
const submitError = ref("");

// Sinh mã dạng HD + yyMMddHHmmss + 3 số ngẫu nhiên, ví dụ HD261024153045123
function genMaHoaDon() {
  const d = new Date();
  const p = (n, l = 2) => String(n).padStart(l, "0");
  const stamp =
    String(d.getFullYear()).slice(2) +
    p(d.getMonth() + 1) +
    p(d.getDate()) +
    p(d.getHours()) +
    p(d.getMinutes()) +
    p(d.getSeconds());
  return `HD${stamp}${p(Math.floor(Math.random() * 1000), 3)}`;
}

/* ---------- Phương thức thanh toán: id cố định theo bảng phuong_thuc_thanh_toan ---------- */
// 11 = COD (Thanh toán khi nhận hàng), 2 = Chuyển khoản ngân hàng
const PAY_METHOD_ID = { cod: 11, bank: 2 };

const phuongThucFields = (kind) => ({
  idPhuongThucThanhToan: PAY_METHOD_ID[kind],
});

// Các field khớp HoaDonRequest của back-end. Tiền hàng, giảm giá, tổng tiền
// do back-end tự tính lại từ chi tiết + phiếu giảm giá nên không gửi lên.
function buildHoaDonRequest() {
  return {
    maHoaDon: genMaHoaDon(),
    loaiDon: "ONLINE",
    trangThai: "CHO_XAC_NHAN",

    // Khách hàng / thông tin nhận hàng
    idKhachHang: customer.value?.id ?? null,
    tenKhachHang: f.name.trim(),
    soDienThoaiKhachHang: f.phone.trim(),
    diaChiNhanHang: f.address.trim(),
    ghiChu: f.note.trim(),

    // Đơn online không có nhân viên (cần back-end cho phép null)
    idNhanVien: ID_NHAN_VIEN_ONLINE,

    ...phuongThucFields(f.pay),
    idPhieuGiamGia: voucher.value?.id ?? null,
    phiVanChuyen: SHIP_FEE,

    chiTiet: cart.items.map((i) => ({
      idSanPhamChiTiet: i.idSanPhamChiTiet ?? i.spctId ?? i.id,
      soLuong: i.qty,
      donGia: i.price,
    })),
  };
}

function clearCart() {
  if (typeof store.clearCart === "function") return store.clearCart();
  if (Array.isArray(cart.items)) cart.items.splice(0);
}

async function submit() {
  submitError.value = "";
  errors.name = f.name.trim() ? "" : "Vui lòng nhập họ và tên.";
  errors.phone = /^0\d{9}$/.test(f.phone)
    ? ""
    : "Số điện thoại gồm 10 số, bắt đầu bằng 0.";
  errors.address = f.address.trim() ? "" : "Vui lòng nhập địa chỉ nhận hàng.";
  if (errors.name || errors.phone || errors.address) return;

  submitting.value = true;
  const payload = buildHoaDonRequest();
  console.log("[Checkout] payload gửi hóa đơn:", payload);
  try {
    const res = await fetch(HOA_DON_API, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      let msg = "";
      try {
        const err = await res.json();
        msg = err.message || err.error || "";
      } catch (e) {
        /* không có body JSON */
      }
      throw new Error(msg || `Đặt hàng thất bại (mã lỗi ${res.status}).`);
    }

    const hd = await res.json(); // HoaDonDTO
    orderId.value = hd.id;
    orderCode.value = hd.maHoaDon ?? hd.ma ?? hd.id;
    done.value = true;
    clearCart();
  } catch (e) {
    console.error("[Checkout] Lỗi tạo hóa đơn:", e);
    submitError.value =
      e.message === "Failed to fetch"
        ? "Không kết nối được máy chủ, vui lòng thử lại."
        : e.message;
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <!-- ===== Đặt hàng thành công ===== -->
  <div v-if="done" class="ps-card text-center p-5">
    <div class="fs-1">🎉</div>
    <h3 class="mt-2">Đặt hàng thành công</h3>
    <p class="mb-2">
      Mã đơn hàng của bạn: <strong class="text-danger">#{{ orderCode }}</strong>
    </p>
    <p class="text-muted-ps mb-4">
      Poly Shoes sẽ gọi xác nhận đơn qua số {{ f.phone }} trong ít phút.
    </p>
    <div class="d-flex justify-content-center gap-2">
      <RouterLink
        :to="`/don-hang/${orderId}`"
        class="btn btn-outline-primary px-4"
      >
        Chi tiết đơn hàng
      </RouterLink>
      <RouterLink to="/cua-hang" class="btn btn-primary px-4">
        Tiếp tục mua sắm
      </RouterLink>
    </div>
  </div>

  <!-- ===== Giỏ trống ===== -->
  <div v-else-if="!cart.items.length" class="ps-card text-center p-5">
    <h5>Giỏ hàng đang trống</h5>
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2"
      >Xem sản phẩm</RouterLink
    >
  </div>

  <!-- ===== Trang thanh toán ===== -->
  <div v-else class="co-page">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      /
      <RouterLink to="/gio-hang" class="text-muted-ps text-decoration-none"
        >Giỏ hàng</RouterLink
      >
      /
      <span class="text-dark fw-semibold">Thanh toán</span>
    </nav>

    <div class="row g-4">
      <!-- ================= CỘT TRÁI ================= -->
      <div class="col-lg-7">
        <!-- Gợi ý đăng nhập (chỉ hiện khi chưa đăng nhập) -->
        <div
          v-if="!isLoggedIn"
          class="ps-card co-card co-login d-flex align-items-center gap-2 mb-3"
        >
          <span class="small">Đã có tài khoản?</span>
          <RouterLink
            to="/dang-nhap"
            class="small fw-bold text-primary text-decoration-underline"
            >Đăng nhập</RouterLink
          >
        </div>

        <!-- Thông tin giao hàng -->
        <div class="ps-card co-card mb-3">
          <h5 class="co-title">Thông tin giao hàng</h5>

          <!-- Địa chỉ đã lưu (chỉ hiện khi đã đăng nhập) -->
          <div v-if="isLoggedIn && savedAddresses.length" class="mb-4">
            <label class="co-label">Chọn địa chỉ đã lưu</label>
            <label
              v-for="a in savedAddresses"
              :key="a.id"
              :for="'addr-' + a.id"
              class="co-option"
              :class="{ active: selectedAddr === a.id }"
            >
              <input
                :id="'addr-' + a.id"
                class="form-check-input co-radio"
                type="radio"
                :checked="selectedAddr === a.id"
                @change="chooseAddress(a.id)"
              />
              <span class="flex-grow-1">
                <span class="d-block fw-semibold small">
                  {{ a.name || userInfo(currentUser).name }} ·
                  {{ a.phone || userInfo(currentUser).phone }}
                  <span v-if="a.isDefault" class="badge bg-primary ms-1"
                    >Mặc định</span
                  >
                </span>
                <span class="d-block small text-muted">{{ a.address }}</span>
              </span>
            </label>
            <label
              for="addr-new"
              class="co-option mb-0"
              :class="{ active: selectedAddr === 'new' }"
            >
              <input
                id="addr-new"
                class="form-check-input co-radio"
                type="radio"
                :checked="selectedAddr === 'new'"
                @change="chooseAddress('new')"
              />
              <span class="small fw-semibold">Nhập địa chỉ mới</span>
            </label>
          </div>

          <div class="row g-3">
            <div class="col-md-6">
              <label class="co-label"
                >Họ và tên <span class="text-danger">*</span></label
              >
              <input
                v-model="f.name"
                class="form-control co-input"
                :readonly="lockName"
                :class="{ 'is-invalid': errors.name, 'bg-light': lockName }"
                placeholder="Nhập họ tên của bạn"
              />
              <div class="invalid-feedback">{{ errors.name }}</div>
            </div>
            <div class="col-md-6">
              <label class="co-label"
                >Số điện thoại <span class="text-danger">*</span></label
              >
              <input
                v-model="f.phone"
                class="form-control co-input"
                :readonly="lockPhone"
                :class="{ 'is-invalid': errors.phone, 'bg-light': lockPhone }"
                placeholder="Nhập số điện thoại"
              />
              <div class="invalid-feedback">{{ errors.phone }}</div>
            </div>
            <div class="col-12">
              <label class="co-label"
                >Địa chỉ nhận hàng <span class="text-danger">*</span></label
              >
              <input
                v-model="f.address"
                class="form-control co-input"
                :readonly="lockAddress"
                :class="{
                  'is-invalid': errors.address,
                  'bg-light': lockAddress,
                }"
                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
              />
              <div class="invalid-feedback">{{ errors.address }}</div>
            </div>
            <div class="col-12">
              <label class="co-label">Ghi chú (không bắt buộc)</label>
              <textarea
                v-model="f.note"
                rows="2"
                class="form-control co-input"
                placeholder="Ghi chú về đơn hàng, thời gian nhận hàng..."
              ></textarea>
            </div>
          </div>

          <div class="co-hint">
            <i class="bi bi-clock"></i>
            <span
              >Đơn hàng được giao trong giờ hành chính (thứ 2 đến thứ 7).</span
            >
          </div>
        </div>

        <!-- Hình thức vận chuyển -->
        <div class="ps-card co-card mb-3">
          <h5 class="co-title">Hình thức vận chuyển</h5>
          <label class="co-option active mb-0">
            <input
              class="form-check-input co-radio"
              type="radio"
              checked
              disabled
            />
            <i class="bi bi-truck co-ico"></i>
            <span class="flex-grow-1 small fw-semibold"
              >Giao hàng tiêu chuẩn</span
            >
            <span class="small fw-semibold">{{ money(SHIP_FEE) }}</span>
          </label>
        </div>

        <!-- Phương thức thanh toán -->
        <div class="ps-card co-card">
          <h5 class="co-title">Phương thức thanh toán</h5>

          <label
            for="cod"
            class="co-option"
            :class="{ active: f.pay === 'cod' }"
          >
            <input
              id="cod"
              v-model="f.pay"
              value="cod"
              type="radio"
              class="form-check-input co-radio"
            />
            <span class="co-ico-box"><i class="bi bi-cash-coin"></i></span>
            <span class="flex-grow-1">
              <span class="d-block fw-semibold"
                >Thanh toán khi nhận hàng (COD)</span
              >
              <span class="d-block small text-muted"
                >Quý khách thanh toán tiền mặt trực tiếp cho nhân viên giao hàng
                khi nhận hàng.</span
              >
            </span>
          </label>

          <label
            for="bank"
            class="co-option mb-0"
            :class="{ active: f.pay === 'bank' }"
          >
            <input
              id="bank"
              v-model="f.pay"
              value="bank"
              type="radio"
              class="form-check-input co-radio"
            />
            <span class="co-ico-box"><i class="bi bi-bank"></i></span>
            <span class="flex-grow-1">
              <span class="d-block fw-semibold">Chuyển khoản ngân hàng</span>
              <span class="d-block small text-muted"
                >Chuyển khoản qua ứng dụng Ngân hàng hoặc Internet
                Banking.</span
              >
            </span>
          </label>
        </div>
      </div>

      <!-- ================= CỘT PHẢI ================= -->
      <div class="col-lg-5">
        <div class="co-sticky">
          <!-- Sản phẩm -->
          <div class="ps-card co-card mb-3">
            <div class="co-cart-head">
              <i class="bi bi-bag-check text-primary"></i>
              Sản phẩm trong đơn ({{ cart.items.length }})
            </div>

            <div
              v-for="i in cart.items"
              :key="i.id + '-' + i.size"
              class="co-item"
            >
              <div class="position-relative flex-shrink-0">
                <img
                  :src="itemImage(i)"
                  :alt="i.name"
                  class="rounded border"
                  style="width: 64px; height: 64px; object-fit: cover"
                />
                <span
                  class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-secondary"
                >
                  {{ i.qty }}
                </span>
              </div>
              <div class="flex-grow-1 small">
                <div class="fw-semibold">{{ i.name }}</div>
                <span class="co-chip">Size {{ i.size }}</span>
              </div>
              <div class="small fw-bold">{{ money(i.price * i.qty) }}</div>
            </div>
          </div>

          <!-- Mã giảm giá -->
          <div class="ps-card co-card mb-3">
            <h6 class="co-title mb-3">Mã giảm giá</h6>

            <div v-if="!voucher" class="d-flex gap-2">
              <input
                v-model="voucherCode"
                class="form-control co-input text-uppercase"
                placeholder="Nhập mã giảm giá"
                @keyup.enter="applyVoucher"
              />
              <button
                class="btn btn-primary co-apply"
                type="button"
                :disabled="applying"
                @click="applyVoucher"
              >
                {{ applying ? "Đang kiểm tra..." : "Áp dụng" }}
              </button>
            </div>

            <div v-else class="co-voucher-ok">
              <i class="bi bi-ticket-perforated-fill"></i>
              <div class="flex-grow-1 small">
                <div>
                  Mã: <strong>{{ voucher.code }}</strong>
                  <span
                    v-if="bestVoucher && bestVoucher.v.id === voucher.id"
                    class="badge bg-success ms-1"
                    >Tốt nhất</span
                  >
                </div>
                <div class="text-success fw-semibold">
                  Đã giảm -{{ money(discount) }}
                </div>
              </div>
              <button
                class="btn btn-sm btn-link text-danger p-0"
                type="button"
                title="Gỡ mã"
                @click="removeVoucher"
              >
                <i class="bi bi-x-lg"></i>
              </button>
            </div>

            <div
              v-if="voucherMsg.text"
              class="small mt-2"
              :class="voucherMsg.ok ? 'text-success' : 'text-danger'"
            >
              {{ voucherMsg.text }}
            </div>

            <!-- Gợi ý nhanh -->
            <div v-if="bestVoucher && !voucher" class="small mt-3">
              <span class="text-muted">Gợi ý mã:</span>
              <button
                type="button"
                class="badge bg-primary border-0 ms-2 co-suggest"
                @click="toggleVoucher(bestVoucher)"
              >
                {{ bestVoucher.v.code }}
              </button>
            </div>

            <!-- Danh sách phiếu theo giá trị đơn hàng -->
            <div v-if="!isLoggedIn" class="small text-muted mt-3">
              Đăng nhập để xem các phiếu giảm giá dành cho bạn.
            </div>
            <div v-else-if="loadingVouchers" class="small text-muted mt-3">
              Đang tải phiếu giảm giá...
            </div>
            <div v-else-if="voucherOptions.length" class="mt-3">
              <div class="co-label">Phiếu phù hợp với đơn hàng</div>
              <div class="co-voucher-list">
                <div
                  v-for="o in voucherOptions"
                  :key="o.v.id"
                  class="co-vcard"
                  :class="{
                    active: voucher && voucher.id === o.v.id,
                    disabled: !o.eligible,
                  }"
                >
                  <div class="flex-grow-1">
                    <div class="fw-bold small">
                      {{ o.v.code }}
                      <span v-if="o.best" class="badge bg-success ms-1"
                        >Tốt nhất</span
                      >
                    </div>
                    <div class="small">
                      {{ voucherLabel(o.v) }} • Đơn từ {{ money(o.v.min) }}
                    </div>
                    <div v-if="o.expire" class="co-vexp">
                      HSD: {{ o.expire }}
                    </div>
                    <div v-if="!o.eligible" class="co-vexp text-danger">
                      Mua thêm {{ money(o.missing) }} để dùng mã này
                    </div>
                    <div v-else class="co-vexp text-success">
                      Giảm được {{ money(o.discount) }}
                    </div>
                  </div>
                  <button
                    type="button"
                    class="btn btn-sm co-vbtn"
                    :class="
                      voucher && voucher.id === o.v.id
                        ? 'btn-success'
                        : 'btn-outline-primary'
                    "
                    :disabled="!o.eligible"
                    @click="toggleVoucher(o)"
                  >
                    {{
                      voucher && voucher.id === o.v.id
                        ? "✓ Đang dùng"
                        : "Áp dụng"
                    }}
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="small text-muted mt-3">
              Hiện chưa có phiếu giảm giá nào khả dụng.
            </div>
          </div>

          <!-- Tóm tắt đơn hàng -->
          <div class="ps-card co-card">
            <h6 class="co-title mb-3">Tóm tắt đơn hàng</h6>

            <div class="co-row">
              <span>Tạm tính ({{ cart.items.length }} sản phẩm)</span>
              <span class="fw-semibold">{{ money(cartTotal) }}</span>
            </div>
            <div class="co-row">
              <span>Phí vận chuyển</span>
              <span class="fw-semibold">{{ money(SHIP_FEE) }}</span>
            </div>
            <div v-if="discount" class="co-row text-success">
              <span
                >Giảm giá<span v-if="voucher"> [{{ voucher.code }}]</span
                >:</span
              >
              <span class="fw-semibold">-{{ money(discount) }}</span>
            </div>

            <div class="co-total">
              <span class="fw-bold">TỔNG CỘNG:</span>
              <span class="price fs-4">{{ money(finalTotal) }}</span>
            </div>

            <div v-if="submitError" class="alert alert-danger small py-2">
              {{ submitError }}
            </div>

            <button
              class="btn btn-primary w-100 co-submit"
              :disabled="submitting"
              @click="submit"
            >
              {{ submitting ? "Đang xử lý..." : "Đặt hàng" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Màu bám theo theme của dự án (trắng - đỏ) thông qua biến Bootstrap */
.co-card {
  padding: 1.25rem 1.5rem;
  border-radius: 14px;
}
.co-login {
  padding-top: 0.9rem;
  padding-bottom: 0.9rem;
}
.co-title {
  font-weight: 700;
  margin-bottom: 1.1rem;
}
.co-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
}
.co-input {
  border-radius: 10px;
  padding: 0.65rem 0.85rem;
  font-size: 0.92rem;
}
.co-input:focus {
  border-color: var(--bs-primary);
  box-shadow: 0 0 0 0.2rem rgba(var(--bs-primary-rgb), 0.15);
}

/* Ô chọn (địa chỉ / vận chuyển / thanh toán) */
.co-option {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  margin-bottom: 0.7rem;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background-color 0.15s;
}
.co-option:hover {
  border-color: var(--bs-primary);
}
.co-option.active {
  border-color: var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.05);
}
.co-radio {
  margin: 0;
  flex-shrink: 0;
  cursor: pointer;
}
.co-ico {
  font-size: 1.4rem;
  color: var(--bs-primary);
}
.co-ico-box {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: var(--bs-primary);
  background: #f8f9fa;
  border-radius: 10px;
}

.co-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  font-size: 0.8rem;
  color: #6b7280;
}

/* Cột phải */
.co-sticky {
  position: sticky;
  top: 1rem;
}
.co-cart-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-bottom: 0.9rem;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid #eee;
  font-weight: 700;
}
.co-item {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  margin-bottom: 0.9rem;
}
.co-item:last-child {
  margin-bottom: 0;
}
.co-chip {
  display: inline-block;
  margin-top: 0.25rem;
  padding: 0.1rem 0.55rem;
  font-size: 0.75rem;
  color: #6b7280;
  background: #f3f4f6;
  border-radius: 6px;
}

.co-apply {
  white-space: nowrap;
  border-radius: 10px;
  padding-inline: 1.1rem;
  font-weight: 700;
}
.co-voucher-ok {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.7rem 0.9rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 10px;
}
.co-voucher-ok > .bi-ticket-perforated-fill {
  font-size: 1.3rem;
  color: #16a34a;
}

.co-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
  font-size: 0.9rem;
}
.co-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 1rem 0;
  padding-top: 1rem;
  border-top: 1px dashed #d1d5db;
}
.co-submit {
  padding: 0.8rem;
  border-radius: 12px;
  font-weight: 700;
}

/* Danh sách phiếu giảm giá */
.co-voucher-list {
  max-height: 280px;
  overflow-y: auto;
  padding-right: 2px;
}
.co-vcard {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.7rem 0.9rem;
  margin-bottom: 0.6rem;
  border: 1px dashed #e5c7ca;
  border-radius: 10px;
  background: #fffafa;
  transition: border-color 0.15s;
}
.co-vcard.active {
  border: 1px solid var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.06);
}
.co-vcard.disabled {
  opacity: 0.6;
  background: #f8f9fa;
  border-color: #dee2e6;
}
.co-vexp {
  font-size: 0.75rem;
  color: #6b7280;
}
.co-vbtn {
  white-space: nowrap;
  font-weight: 600;
  border-radius: 8px;
}
.co-suggest {
  cursor: pointer;
  padding: 0.4rem 0.7rem;
}

@media (max-width: 991.98px) {
  .co-sticky {
    position: static;
  }
}
</style>
