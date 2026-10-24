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
  if (!u) {
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
      return c;
    } catch (e) {
      // Fallback to the public customer lookup below when account details are unavailable.
    }
  }

  // 2) Tìm trong danh sách
  try {
    const page = await getJson(`${API}?size=1000`);
    const list = page.content || page;
    const c = list.find(matches) || null;
    return c;
  } catch (e) {
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
const VOUCHER_API = API.replace(/\/khach-hang$/, "/phieu-giam-gia");

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

// Trả về { ok: true, voucher } hoặc { ok: false, message }
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

  // 2) Lấy chi tiết (có danh sách khách hàng được gắn)
  let detail = found;
  try {
    detail = {
      ...found,
      ...(await getJson(`${VOUCHER_API}/detail/${VF.id(found)}`)),
    };
  } catch (e) {
    /* dùng dữ liệu từ danh sách */
  }

  // 3) Kiểm tra điều kiện
  const act = VF.active(detail);
  if (act === false || /NGUNG|TAT|INACTIVE|HET_HAN/i.test(String(act)))
    return { ok: false, message: "Mã giảm giá đang bị tắt hoặc đã hết hạn." };

  const now = new Date();
  const st = VF.start(detail);
  const en = VF.end(detail);
  if (st && now < new Date(st))
    return { ok: false, message: "Mã giảm giá chưa đến thời gian áp dụng." };
  if (en && now > new Date(en))
    return { ok: false, message: "Mã giảm giá đã hết hạn." };

  const qty = VF.qty(detail);
  if (qty !== undefined && qty !== null && Number(qty) <= 0)
    return { ok: false, message: "Mã giảm giá đã hết lượt sử dụng." };

  if (isPersonal(VF.scope(detail))) {
    const cid = customer.value?.id;
    if (!cid)
      return { ok: false, message: "Vui lòng đăng nhập để dùng mã này." };
    if (!VF.customerIds(detail).includes(cid))
      return {
        ok: false,
        message: "Mã này không áp dụng cho tài khoản của bạn.",
      };
  }

  const min = VF.min(detail);
  if (orderTotal.value < min)
    return {
      ok: false,
      message: `Đơn tối thiểu ${money(min)} để dùng mã này.`,
    };

  return {
    ok: true,
    voucher: {
      id: VF.id(detail),
      code,
      type: isPercent(VF.loai(detail)) ? "percent" : "fixed",
      value: VF.value(detail),
      max: VF.max(detail),
      min,
    },
  };
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

const discount = computed(() => {
  const v = voucher.value;
  if (!v) return 0;
  if (orderTotal.value < (v.min || 0)) return 0;
  let d = v.type === "percent" ? (orderTotal.value * v.value) / 100 : v.value;
  if (v.max > 0) d = Math.min(d, v.max);
  return Math.max(0, Math.min(Math.floor(d), orderTotal.value));
});
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

  <div v-else-if="!cart.items.length" class="ps-card text-center p-5">
    <h5>Giỏ hàng đang trống</h5>
    <RouterLink to="/cua-hang" class="btn btn-primary mt-2"
      >Xem sản phẩm</RouterLink
    >
  </div>

  <div v-else>
    <h2 class="mb-4">Thanh toán</h2>
    <div class="row g-4">
      <div class="col-lg-7">
        <div class="ps-card p-4">
          <h5 class="mb-3">Thông tin nhận hàng</h5>

          <!-- Địa chỉ đã lưu (chỉ hiện khi đã đăng nhập) -->
          <div v-if="isLoggedIn && savedAddresses.length" class="mb-4">
            <label class="form-label fw-semibold small"
              >Chọn địa chỉ đã lưu</label
            >
            <div
              v-for="a in savedAddresses"
              :key="a.id"
              class="form-check border rounded p-2 ps-5 mb-2"
              :class="{ 'border-primary bg-light': selectedAddr === a.id }"
            >
              <input
                :id="'addr-' + a.id"
                class="form-check-input"
                type="radio"
                :checked="selectedAddr === a.id"
                @change="chooseAddress(a.id)"
              />
              <label :for="'addr-' + a.id" class="form-check-label w-100">
                <div class="fw-semibold small">
                  {{ a.name || userInfo(currentUser).name }} ·
                  {{ a.phone || userInfo(currentUser).phone }}
                  <span v-if="a.isDefault" class="badge bg-primary ms-1"
                    >Mặc định</span
                  >
                </div>
                <div class="small text-muted">{{ a.address }}</div>
              </label>
            </div>
            <div
              class="form-check border rounded p-2 ps-5"
              :class="{ 'border-primary bg-light': selectedAddr === 'new' }"
            >
              <input
                id="addr-new"
                class="form-check-input"
                type="radio"
                :checked="selectedAddr === 'new'"
                @change="chooseAddress('new')"
              />
              <label
                for="addr-new"
                class="form-check-label w-100 small fw-semibold"
              >
                Nhập địa chỉ mới
              </label>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold small">Họ và tên</label>
            <input
              v-model="f.name"
              class="form-control"
              :readonly="lockName"
              :class="{ 'is-invalid': errors.name, 'bg-light': lockName }"
              placeholder="Nguyễn Văn A"
            />
            <div class="invalid-feedback">{{ errors.name }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label fw-semibold small">Số điện thoại</label>
            <input
              v-model="f.phone"
              class="form-control"
              :readonly="lockPhone"
              :class="{ 'is-invalid': errors.phone, 'bg-light': lockPhone }"
              placeholder="0912345678"
            />
            <div class="invalid-feedback">{{ errors.phone }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label fw-semibold small">Địa chỉ</label>
            <input
              v-model="f.address"
              class="form-control"
              :readonly="lockAddress"
              :class="{ 'is-invalid': errors.address, 'bg-light': lockAddress }"
              placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
            />
            <div class="invalid-feedback">{{ errors.address }}</div>
          </div>
          <div class="mb-4">
            <label class="form-label fw-semibold small"
              >Ghi chú (không bắt buộc)</label
            >
            <textarea
              v-model="f.note"
              rows="2"
              class="form-control"
              placeholder="Ghi chú về đơn hàng, thời gian nhận hàng..."
            ></textarea>
          </div>
          <h5 class="mb-3">Phương thức thanh toán</h5>
          <div class="form-check mb-2">
            <input
              id="cod"
              v-model="f.pay"
              value="cod"
              type="radio"
              class="form-check-input"
            />
            <label for="cod" class="form-check-label"
              >Thanh toán khi nhận hàng (COD)</label
            >
          </div>
          <div class="form-check">
            <input
              id="bank"
              v-model="f.pay"
              value="bank"
              type="radio"
              class="form-check-input"
            />
            <label for="bank" class="form-check-label"
              >Chuyển khoản ngân hàng</label
            >
          </div>
        </div>
      </div>

      <div class="col-lg-5">
        <div class="ps-card p-4">
          <h5 class="mb-3">Đơn hàng của bạn</h5>

          <!-- Sản phẩm có ảnh -->
          <div
            v-for="i in cart.items"
            :key="i.id + '-' + i.size"
            class="d-flex align-items-center gap-3 mb-3"
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
              <div class="text-muted">Size {{ i.size }} × {{ i.qty }}</div>
            </div>
            <div class="small fw-semibold">{{ money(i.price * i.qty) }}</div>
          </div>

          <!-- Voucher -->
          <div class="border-top pt-3 mb-3">
            <label class="form-label fw-semibold small">Mã giảm giá</label>
            <div v-if="!voucher" class="input-group">
              <input
                v-model="voucherCode"
                class="form-control text-uppercase"
                placeholder="Nhập mã voucher"
                @keyup.enter="applyVoucher"
              />
              <button
                class="btn btn-outline-primary"
                type="button"
                :disabled="applying"
                @click="applyVoucher"
              >
                {{ applying ? "Đang kiểm tra..." : "Áp dụng" }}
              </button>
            </div>
            <div
              v-else
              class="d-flex justify-content-between align-items-center border rounded p-2 bg-light"
            >
              <span class="small">
                🏷️ <strong>{{ voucher.code }}</strong>
              </span>
              <button
                class="btn btn-sm btn-link text-danger p-0"
                @click="removeVoucher"
              >
                Gỡ
              </button>
            </div>
            <div
              v-if="voucherMsg.text"
              class="small mt-1"
              :class="voucherMsg.ok ? 'text-success' : 'text-danger'"
            >
              {{ voucherMsg.text }}
            </div>
          </div>

          <!-- Tổng tiền -->
          <div class="d-flex justify-content-between small mb-2">
            <span>Tạm tính</span><span>{{ money(cartTotal) }}</span>
          </div>
          <div class="d-flex justify-content-between small mb-2">
            <span>Phí vận chuyển</span><span>{{ money(SHIP_FEE) }}</span>
          </div>
          <div
            v-if="discount"
            class="d-flex justify-content-between small mb-2 text-success"
          >
            <span>Giảm giá</span><span>-{{ money(discount) }}</span>
          </div>
          <div class="d-flex justify-content-between border-top pt-3 my-3">
            <span class="fw-bold">Tổng cộng</span>
            <span class="price fs-5">{{ money(finalTotal) }}</span>
          </div>
          <div v-if="submitError" class="alert alert-danger small py-2">
            {{ submitError }}
          </div>
          <button
            class="btn btn-primary w-100 py-2"
            :disabled="submitting"
            @click="submit"
          >
            {{ submitting ? "Đang xử lý..." : "Đặt hàng" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
