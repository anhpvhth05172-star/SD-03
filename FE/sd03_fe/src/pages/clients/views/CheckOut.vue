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

const itemImage = (i) => i.hinhAnh || i.img || i.image || null;

// Ảnh lỗi -> hiện emoji thay thế
const imgFailed = ref({});
const itemKey = (i) => `${i.id}-${i.size}`;

// Tổng số lượng sản phẩm khách đã chọn (cộng dồn qty của từng dòng)
const totalQty = computed(() =>
  cart.items.reduce((sum, i) => sum + (Number(i.qty) || 0), 0),
);

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
  qty: (v) => {
    const conLai = v.soLuongConLai ?? v.soLuongCon;
    if (conLai != null) return Number(conLai);
    const total = v.soLuong ?? v.soLuongToiDa ?? v.soLuongPhieu;
    if (total == null) return undefined;
    const used =
      v.soLuongDaSuDung ??
      v.soLuongDaDung ??
      v.daSuDung ??
      v.soLuongSuDung ??
      0;
    return Number(total) - Number(used);
  },
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

// Phiếu đã hết lượt sử dụng (hết số lượng hoặc trạng thái báo hết lượt)
function isExhausted(detail) {
  const qty = VF.qty(detail);
  if (qty !== undefined && qty !== null && Number(qty) <= 0) return true;
  return /HET_LUOT|HET_SO_LUONG|DA_HET|HET_SL/i.test(String(VF.active(detail)));
}

// Kiểm tra điều kiện của 1 phiếu. Trả về chuỗi lỗi, hoặc "" nếu hợp lệ.
// ignoreMin = true: bỏ qua điều kiện "đơn tối thiểu" (dùng khi liệt kê phiếu gợi ý)
function checkVoucher(detail, ignoreMin = false) {
  if (isExhausted(detail)) return "Mã giảm giá đã hết lượt sử dụng.";

  const act = VF.active(detail);
  if (act === false || /NGUNG|TAT|INACTIVE|HET_HAN/i.test(String(act)))
    return "Mã giảm giá đang bị tắt hoặc đã hết hạn.";

  const now = new Date();
  const st = VF.start(detail);
  const en = VF.end(detail);
  if (st && now < new Date(st))
    return "Mã giảm giá chưa đến thời gian áp dụng.";
  if (en && now > new Date(en)) return "Mã giảm giá đã hết hạn.";

  if (isPersonal(VF.scope(detail))) {
    const cid = customer.value?.id;
    if (!cid) return "Vui lòng đăng nhập để dùng mã này.";
    const khList = detail.khachHangs || detail.danhSachKhachHang || detail.khachHang || [];
    const custRecord = khList.find(k => (k.idKhachHang ?? k.id) === cid);
    if (!custRecord) {
      return "Mã này không áp dụng cho tài khoản của bạn.";
    }
    if (custRecord.ngaySuDung || custRecord.trangThai === false) {
      return "Bạn đã sử dụng mã giảm giá này rồi.";
    }
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
const manualRemoved = ref(false); // khách chủ động gỡ mã -> không tự áp lại

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
  manualRemoved.value = false;
  voucher.value = r.voucher;
  voucherMsg.text = "Áp dụng mã thành công!";
  voucherMsg.ok = true;
}
function removeVoucher() {
  userRemovedVoucher.value = true;
  voucher.value = null;
  voucherCode.value = "";
  voucherMsg.text = "";
}
// Khách bấm gỡ mã
function onRemoveClick() {
  manualRemoved.value = true;
  removeVoucher();
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

// Phiếu còn hiệu lực VÀ còn lượt sử dụng (phiếu hết lượt bị ẩn hoàn toàn)
const voucherOptions = computed(() => {
  const total = orderTotal.value;
  const list = allVouchers.value
    .filter((d) => !isExhausted(d))
    .filter((d) => !checkVoucher(d, true))
    .map((d) => {
      const v = toVoucher(d);
      const eligible = total >= (v.min || 0);
      const end = VF.end(d);
      const left = VF.qty(d);
      return {
        v,
        eligible,
        discount: eligible ? calcDiscount(v, total) : 0,
        missing: Math.max(0, (v.min || 0) - total),
        expire: end ? new Date(end).toLocaleDateString("vi-VN") : "",
        left: left !== undefined && left !== null ? Number(left) : null,
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
const userRemovedVoucher = ref(false);

// Tự động áp dụng mã giảm giá tốt nhất khi có sản phẩm trong giỏ
watch([bestVoucher, orderTotal], ([best, total]) => {
  if (total <= 0) {
    voucher.value = null;
    userRemovedVoucher.value = false;
    return;
  }
  if (!userRemovedVoucher.value && best) {
    if (!voucher.value || voucher.value.id !== best.v.id) {
      voucher.value = best.v;
      voucherMsg.text = `Đã tự động áp dụng mã ${best.v.code} (tiết kiệm ${money(best.discount)})`;
      voucherMsg.ok = true;
    }
  } else if (voucher.value) {
    const curDetail = allVouchers.value.find((x) => VF.id(x) === voucher.value.id);
    if (!curDetail || checkVoucher(curDetail)) {
      if (best) {
        voucher.value = best.v;
      } else {
        voucher.value = null;
      }
    }
  }
}, { immediate: true });


// Voucher đang áp dụng mà bị hết lượt (sau khi tải lại danh sách) -> gỡ
watch(allVouchers, () => {
  if (!voucher.value) return;
  const d = allVouchers.value.find((x) => VF.id(x) === voucher.value.id);
  if (d && isExhausted(d)) {
    removeVoucher();
    voucherMsg.text = "Mã giảm giá đã hết lượt sử dụng nên đã được gỡ.";
    voucherMsg.ok = false;
  }
});

// Tự động áp dụng mã giảm giá tốt nhất cho khách hàng
watch(
  [bestVoucher, () => cart.items.length],
  ([best]) => {
    if (manualRemoved.value) return;
    if (best && (!voucher.value || voucher.value.id !== best.v.id)) {
      voucher.value = best.v;
      voucherCode.value = "";
      voucherMsg.text = `Đã tự động áp dụng mã ưu đãi tốt nhất (${best.v.code})!`;
      voucherMsg.ok = true;
    }
  },
  { immediate: true },
);

const voucherLabel = (v) =>
  v.type === "percent"
    ? `Giảm ${v.value}%${v.max > 0 ? ` (tối đa ${money(v.max)})` : ""}`
    : `Giảm ${money(v.value)}`;

// Bấm vào phiếu: chọn / bỏ chọn
function toggleVoucher(o) {
  if (!o.eligible) return;
  if (voucher.value?.id === o.v.id) {
    onRemoveClick();
    return;
  }
  manualRemoved.value = false;
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
// 11 = COD (Thanh toán khi nhận hàng), 2 = Chuyển khoản ngân hàng, 4 = VNPay QR (id 4 trong hinh_thuc_thanh_toan)
const PAY_METHOD_ID = { cod: 11, bank: 2, vnpay: 4 };

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

  // ====== VNPAY: Tạo đơn CHO_THANH_TOAN + redirect, CHƯA xóa giỏ ======
  if (f.pay === "vnpay") {
    const payload = buildHoaDonRequest();
    // Ghi đè trangThai, BE sẽ set CHO_THANH_TOAN
    payload.trangThai = "CHO_THANH_TOAN";
    console.log("[Checkout] VNPAY payload:", payload);
    try {
      const res = await fetch(
        "http://localhost:8080/api/vnpay/create-payment",
        {
          method: "POST",
          headers: { "Content-Type": "application/json", ...authHeaders() },
          body: JSON.stringify(payload),
        },
      );

      if (!res.ok) {
        let errJson = {};
        try {
          errJson = await res.json();
        } catch (e) {}
        throw new Error(
          errJson.message || `Lỗi tạo thanh toán VNPAY (mã lỗi ${res.status}).`,
        );
      }

      const payData = await res.json();
      if (payData && payData.paymentUrl) {
        // Redirect sang VNPay — KHÔNG xóa giỏ (giỏ chỉ xóa sau khi VNPayReturn xác nhận thành công)
        window.location.href = payData.paymentUrl;
        return;
      } else {
        throw new Error("Không nhận được liên kết thanh toán từ VNPAY.");
      }
    } catch (e) {
      console.error("[Checkout] Lỗi VNPAY:", e);
      submitError.value =
        e.message === "Failed to fetch"
          ? "Không kết nối được máy chủ, vui lòng thử lại."
          : e.message;
      submitting.value = false;
      return;
    }
  }

  // ====== COD / Bank: flow bình thường ======
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

    // Lưu vào danh sách đơn gần đây để tra cứu
    try {
      const orders = JSON.parse(localStorage.getItem("ps_orders") || "[]");
      orders.unshift({
        id: orderCode.value,
        orderId: hd.id,
        info: {
          name: f.name,
          phone: f.phone,
          address: f.address,
          note: f.note,
          pay: f.pay,
        },
        items: JSON.parse(JSON.stringify(cart.items)),
        total: finalTotal.value,
        date: new Date().toLocaleString("vi-VN"),
      });
      localStorage.setItem("ps_orders", JSON.stringify(orders.slice(0, 20)));
    } catch (e) {}

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

// Sao chép mã đơn
const copied = ref(false);
async function copyOrderCode() {
  try {
    await navigator.clipboard.writeText(String(orderCode.value));
    copied.value = true;
    setTimeout(() => (copied.value = false), 1800);
  } catch (e) {
    /* bỏ qua */
  }
}
</script>

<template>
  <!-- ===== Đặt hàng thành công ===== -->
  <div v-if="done" class="co-success">
    <div class="co-success-ico"><i class="bi bi-check-lg"></i></div>
    <h3 class="fw-bold mb-2">Đặt hàng thành công!</h3>
    <p class="text-muted-ps mb-3">Cảm ơn bạn đã mua sắm tại Poly Shoes.</p>

    <div class="co-success-code">
      <span class="small text-muted-ps">Mã đơn hàng</span>
      <strong>#{{ orderCode }}</strong>
      <button type="button" class="co-copy" @click="copyOrderCode">
        <i class="bi" :class="copied ? 'bi-check2' : 'bi-copy'"></i>
      </button>
    </div>

    <p class="text-muted-ps mb-4">
      Poly Shoes sẽ gọi xác nhận đơn qua số <strong>{{ f.phone }}</strong> trong
      ít phút.
    </p>
    <div class="d-flex justify-content-center flex-wrap gap-2">
      <RouterLink
        :to="`/don-hang/${orderId}`"
        class="btn btn-outline-primary rounded-pill px-4"
      >
        Chi tiết đơn hàng
      </RouterLink>
      <RouterLink to="/cua-hang" class="btn btn-primary rounded-pill px-4">
        Tiếp tục mua sắm
      </RouterLink>
    </div>
  </div>

  <!-- ===== Giỏ trống ===== -->
  <div v-else-if="!cart.items.length" class="co-success">
    <div class="co-success-ico empty"><i class="bi bi-bag-x"></i></div>
    <h5 class="fw-bold">Giỏ hàng đang trống</h5>
    <p class="text-muted-ps">
      Hãy thêm vài đôi giày bạn thích trước khi thanh toán nhé.
    </p>
    <RouterLink to="/cua-hang" class="btn btn-primary rounded-pill px-4 mt-1"
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
      <span class="mx-1">/</span>
      <RouterLink to="/gio-hang" class="text-muted-ps text-decoration-none"
        >Giỏ hàng</RouterLink
      >
      <span class="mx-1">/</span>
      <span class="text-dark fw-semibold">Thanh toán</span>
    </nav>

    <!-- Tiến trình -->
    <div class="co-steps mb-4">
      <div class="co-step done">
        <span class="co-step-dot"><i class="bi bi-check-lg"></i></span>
        <span class="co-step-name">Giỏ hàng</span>
      </div>
      <span class="co-step-line done"></span>
      <div class="co-step current">
        <span class="co-step-dot">2</span>
        <span class="co-step-name">Thanh toán</span>
      </div>
      <span class="co-step-line"></span>
      <div class="co-step">
        <span class="co-step-dot">3</span>
        <span class="co-step-name">Hoàn tất</span>
      </div>
    </div>

    <div class="row g-4">
      <!-- ================= CỘT TRÁI ================= -->
      <div class="col-lg-7">
        <!-- Gợi ý đăng nhập -->
        <div v-if="!isLoggedIn" class="co-login mb-3">
          <div class="d-flex align-items-center gap-2 small">
            <i class="bi bi-person-circle fs-5"></i>
            <span
              >Đã có tài khoản? Đăng nhập để tự điền thông tin và nhận mã giảm
              giá.</span
            >
          </div>
          <RouterLink
            to="/dang-nhap"
            class="btn btn-sm btn-dark rounded-pill px-3 fw-bold"
          >
            Đăng nhập
          </RouterLink>
        </div>

        <!-- Thông tin giao hàng -->
        <section class="co-card mb-3">
          <h5 class="co-title">
            <span class="co-title-ico"><i class="bi bi-geo-alt-fill"></i></span
            >Thông tin giao hàng
          </h5>

          <!-- Địa chỉ đã lưu -->
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
                name="addr"
                class="form-check-input co-radio"
                type="radio"
                :value="a.id"
                :checked="selectedAddr === a.id"
                @change="chooseAddress(a.id)"
              />
              <span class="flex-grow-1 min-w-0">
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
                name="addr"
                class="form-check-input co-radio"
                type="radio"
                value="new"
                :checked="selectedAddr === 'new'"
                @change="chooseAddress('new')"
              />
              <span class="small fw-semibold"
                ><i class="bi bi-plus-circle me-1"></i>Nhập địa chỉ mới</span
              >
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
        </section>

        <!-- Vận chuyển -->
        <section class="co-card mb-3">
          <h5 class="co-title">
            <span class="co-title-ico"><i class="bi bi-truck"></i></span>Hình
            thức vận chuyển
          </h5>
          <label class="co-option active mb-0">
            <input
              name="ship"
              class="form-check-input co-radio"
              type="radio"
              checked
              disabled
            />
            <span class="co-ico-box"><i class="bi bi-truck"></i></span>
            <span class="flex-grow-1">
              <span class="d-block small fw-semibold"
                >Giao hàng tiêu chuẩn</span
              >
              <span class="d-block small text-muted"
                >Nhận hàng trong 1-3 ngày làm việc</span
              >
            </span>
            <span class="small fw-bold">{{ money(SHIP_FEE) }}</span>
          </label>
        </section>

        <!-- Thanh toán -->
        <section class="co-card">
          <h5 class="co-title">
            <span class="co-title-ico"
              ><i class="bi bi-credit-card-2-front-fill"></i></span
            >Phương thức thanh toán
          </h5>

          <label
            for="pay-cod"
            class="co-option"
            :class="{ active: f.pay === 'cod' }"
          >
            <input
              id="pay-cod"
              v-model="f.pay"
              name="pay"
              value="cod"
              type="radio"
              class="form-check-input co-radio"
            />
            <span class="co-ico-box"><i class="bi bi-cash-coin"></i></span>
            <span class="flex-grow-1">
              <span class="d-block fw-semibold"
                >Thanh toán khi nhận hàng (COD)</span
              >
              <!-- <span class="d-block small text-muted">
                Thanh toán tiền mặt trực tiếp cho nhân viên giao hàng khi nhận
                hàng.
              </span> -->
            </span>
          </label>

          <label
            for="pay-vnpay"
            class="co-option mb-0"
            :class="{ active: f.pay === 'vnpay' }"
          >
            <input
              id="pay-vnpay"
              v-model="f.pay"
              name="pay"
              value="vnpay"
              type="radio"
              class="form-check-input co-radio"
            />
            <span class="co-ico-box vnpay-ico-box">
              <span
                class="fw-bold"
                style="font-size: 0.85rem; letter-spacing: -0.5px"
              >
                <span style="color: #005baa">VN</span
                ><span style="color: #e11b22">PAY</span>
              </span>
            </span>
            <span class="flex-grow-1">
              <span class="d-flex align-items-center gap-2 flex-wrap">
                <span class="fw-semibold">Thanh toán qua VNPAY</span>
                <!-- <span
                  class="badge bg-danger-subtle text-danger"
                  style="font-size: 0.7rem"
                  >Khuyên dùng</span
                > -->
              </span>
              <!-- <span class="d-block small text-muted">
                Thanh toán bảo mật qua VNPAY QR, 40+ ngân hàng nội địa hoặc thẻ
                Visa/Mastercard.
              </span> -->
            </span>
          </label>
        </section>
      </div>

      <!-- ================= CỘT PHẢI ================= -->
      <div class="col-lg-5">
        <div class="co-sticky">
          <!-- Sản phẩm -->
          <section class="co-card mb-3">
            <div class="co-cart-head">
              <span
                ><i class="bi bi-bag-check-fill text-danger me-2"></i>Sản phẩm
                trong đơn</span
              >
              <span class="co-count">{{ totalQty }}</span>
            </div>

            <div class="co-items">
              <div v-for="i in cart.items" :key="itemKey(i)" class="co-item">
                <div class="co-thumb">
                  <img
                    v-if="itemImage(i) && !imgFailed[itemKey(i)]"
                    :src="itemImage(i)"
                    :alt="i.name"
                    @error="imgFailed[itemKey(i)] = true"
                  />
                  <span v-else class="co-thumb-fb">👟</span>
                  <span class="co-thumb-qty">{{ i.qty }}</span>
                </div>
                <div class="flex-grow-1 min-w-0">
                  <div class="co-item-name">{{ i.name }}</div>
                  <div class="d-flex flex-wrap gap-1 mt-1">
                    <span v-if="i.color" class="co-chip">{{ i.color }}</span>
                    <span v-if="i.size" class="co-chip">Size {{ i.size }}</span>
                    <span class="co-chip"
                      >{{ money(i.price) }} × {{ i.qty }}</span
                    >
                  </div>
                </div>
                <div class="small fw-bold text-nowrap">
                  {{ money(i.price * i.qty) }}
                </div>
              </div>
            </div>
          </section>

          <!-- Mã giảm giá -->
          <section class="co-card mb-3">
            <h6 class="co-title mb-3">
              <span class="co-title-ico"
                ><i class="bi bi-ticket-perforated-fill"></i></span
              >Mã giảm giá
            </h6>

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
                    class="badge bg-success ms-1"
                    >Tự động áp dụng</span
                  >
                </div>
                <div class="text-success fw-semibold">
                  Đã giảm -{{ money(discount) }}
                </div>
              </div>
            </div>

            <div
              v-if="voucherMsg.text"
              class="small mt-2"
              :class="voucherMsg.ok ? 'text-success' : 'text-danger'"
            >
              {{ voucherMsg.text }}
            </div>

            <!-- Danh sách phiếu -->
            <div v-if="!isLoggedIn" class="small text-muted mt-3">
              Đăng nhập để xem các phiếu giảm giá dành cho bạn.
            </div>
            <div v-else-if="loadingVouchers" class="small text-muted mt-3">
              <span class="spinner-border spinner-border-sm me-1"></span>Đang
              tải phiếu giảm giá...
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
                  <div class="co-vleft"><i class="bi bi-percent"></i></div>
                  <div class="flex-grow-1 min-w-0">
                    <div class="fw-bold small">
                      {{ o.v.code }}
                      <span v-if="o.best" class="badge bg-success ms-1"
                        >Tốt nhất</span
                      >
                    </div>
                    <div class="small">
                      {{ voucherLabel(o.v) }} • Đơn từ {{ money(o.v.min) }}
                    </div>
                    <div class="co-vexp">
                      <span v-if="o.expire">HSD: {{ o.expire }}</span>
                      <span v-if="o.left !== null">
                        · Còn {{ o.left }} lượt</span
                      >
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
                        : "Chọn mã"
                    }}
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="small text-muted mt-3">
              Hiện chưa có phiếu giảm giá nào khả dụng.
            </div>
          </section>

          <!-- Tóm tắt -->
          <section class="co-card">
            <h6 class="co-title mb-3">
              <span class="co-title-ico"
                ><i class="bi bi-receipt-cutoff"></i></span
              >Tóm tắt đơn hàng
            </h6>

            <div class="co-row">
              <span>Tạm tính ({{ totalQty }} sản phẩm)</span>
              <span class="fw-semibold">{{ money(cartTotal) }}</span>
            </div>
            <div class="co-row">
              <span>Phí vận chuyển</span>
              <span class="fw-semibold">{{ money(SHIP_FEE) }}</span>
            </div>
            <div v-if="discount" class="co-row text-success">
              <span
                >Giảm giá<span v-if="voucher"> [{{ voucher.code }}]</span></span
              >
              <span class="fw-semibold">-{{ money(discount) }}</span>
            </div>

            <div class="co-total">
              <span class="fw-bold">Tổng cộng</span>
              <span class="price fs-4">{{ money(finalTotal) }}</span>
            </div>
            <div v-if="discount" class="co-saved">
              <i class="bi bi-piggy-bank-fill me-1"></i>Bạn đã tiết kiệm
              <strong>{{ money(discount) }}</strong>
            </div>

            <div
              v-if="submitError"
              class="alert alert-danger small py-2 mt-3 mb-0"
            >
              {{ submitError }}
            </div>

            <button
              class="btn btn-primary w-100 co-submit"
              :disabled="submitting"
              @click="submit"
            >
              <span
                v-if="submitting"
                class="spinner-border spinner-border-sm me-2"
              ></span>
              {{
                submitting
                  ? "Đang xử lý..."
                  : f.pay === "vnpay"
                    ? "Thanh toán qua VNPAY"
                    : "Đặt hàng"
              }}
              <i v-if="!submitting" class="bi bi-arrow-right ms-1"></i>
            </button>

            <div class="co-perks">
              <span
                ><i class="bi bi-shield-lock-fill text-success"></i>Bảo mật
                thanh toán</span
              >
              <span
                ><i class="bi bi-box-seam-fill text-danger"></i>Kiểm tra khi
                nhận</span
              >
              <span
                ><i class="bi bi-arrow-repeat text-primary"></i>Đổi size 7
                ngày</span
              >
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.min-w-0 {
  min-width: 0;
}

/* ===== Steps ===== */
.co-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 16px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.05);
}
.co-step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-weight: 600;
  font-size: 0.88rem;
}
.co-step-dot {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.82rem;
  font-weight: 700;
  background: #f1f5f9;
  color: #94a3b8;
}
.co-step.done .co-step-dot {
  background: #16a34a;
  color: #fff;
}
.co-step.done {
  color: #16a34a;
}
.co-step.current .co-step-dot {
  background: var(--bs-primary, #d62828);
  color: #fff;
  box-shadow: 0 0 0 4px rgba(214, 40, 40, 0.15);
}
.co-step.current {
  color: #111827;
}
.co-step-line {
  flex: 0 1 70px;
  height: 3px;
  border-radius: 3px;
  background: #e5e7eb;
}
.co-step-line.done {
  background: #16a34a;
}

/* ===== Card ===== */
.co-card {
  padding: 1.35rem 1.5rem;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 18px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.05);
}
.co-login {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
  border-radius: 14px;
}
.co-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  margin-bottom: 1.1rem;
  color: #111827;
}
.co-title-ico {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 1rem;
  color: var(--bs-primary, #d62828);
  background: rgba(var(--bs-primary-rgb, 214, 40, 40), 0.1);
}
.co-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: #334155;
}
.co-input {
  border-radius: 12px;
  padding: 0.65rem 0.9rem;
  font-size: 0.92rem;
}
.co-input:focus {
  border-color: var(--bs-primary);
  box-shadow: 0 0 0 0.2rem rgba(var(--bs-primary-rgb), 0.15);
}

/* ===== Option ===== */
.co-option {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  margin-bottom: 0.7rem;
  border: 1.5px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background-color 0.15s,
    box-shadow 0.15s;
}
.co-option:hover {
  border-color: var(--bs-primary);
}
.co-option.active {
  border-color: var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.05);
  box-shadow: 0 4px 14px rgba(var(--bs-primary-rgb), 0.12);
}
.co-radio {
  margin: 0;
  flex-shrink: 0;
  cursor: pointer;
}
.co-radio:focus {
  box-shadow: none;
}
.co-ico-box {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: var(--bs-primary);
  background: #f8f9fa;
  border-radius: 12px;
}
.vnpay-ico-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

/* ===== Right column ===== */
.co-sticky {
  position: sticky;
  top: 1rem;
}
.co-cart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.9rem;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid #eef0f3;
  font-weight: 800;
}
.co-count {
  min-width: 28px;
  text-align: center;
  padding: 2px 10px;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.1);
  border-radius: 999px;
}
.co-items {
  max-height: 340px;
  overflow-y: auto;
  padding-right: 2px;
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
.co-thumb {
  position: relative;
  flex: 0 0 66px;
  width: 66px;
  height: 66px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(145deg, #fdf2f2, #fbe9ee);
}
.co-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 14px;
}
.co-thumb-fb {
  font-size: 1.8rem;
}
.co-thumb-qty {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  display: grid;
  place-items: center;
  font-size: 0.7rem;
  font-weight: 700;
  color: #fff;
  background: #475569;
  border: 2px solid #fff;
  border-radius: 999px;
}
.co-item-name {
  font-size: 0.88rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.co-chip {
  display: inline-block;
  padding: 0.1rem 0.55rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 999px;
}

/* ===== Voucher ===== */
.co-apply {
  white-space: nowrap;
  border-radius: 12px;
  padding-inline: 1.1rem;
  font-weight: 700;
}
.co-voucher-ok {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.9rem;
  background: linear-gradient(120deg, #f0fdf4, #dcfce7);
  border: 1px dashed #86efac;
  border-radius: 12px;
}
.co-voucher-ok > .bi-ticket-perforated-fill {
  font-size: 1.4rem;
  color: #16a34a;
}
.co-x {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: rgba(220, 38, 38, 0.1);
  color: #dc2626;
  font-size: 0.7rem;
  cursor: pointer;
}
.co-x:hover {
  background: rgba(220, 38, 38, 0.2);
}

.co-voucher-list {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 2px;
}
.co-vcard {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0;
  margin-bottom: 0.6rem;
  overflow: hidden;
  border: 1px dashed #e5c7ca;
  border-radius: 12px;
  background: #fffafa;
  padding-right: 0.8rem;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}
.co-vcard:hover:not(.disabled) {
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
}
.co-vcard > .flex-grow-1 {
  padding: 0.6rem 0;
}
.co-vleft {
  align-self: stretch;
  flex: 0 0 46px;
  display: grid;
  place-items: center;
  font-size: 1.2rem;
  color: #fff;
  background: linear-gradient(160deg, #ef4444, #b91c1c);
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
.co-vcard.disabled .co-vleft {
  background: #94a3b8;
}
.co-vexp {
  font-size: 0.74rem;
  color: #6b7280;
}
.co-vbtn {
  white-space: nowrap;
  font-weight: 600;
  border-radius: 8px;
}

/* ===== Summary ===== */
.co-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
  font-size: 0.92rem;
  color: #475569;
}
.co-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 1rem 0 0.5rem;
  padding-top: 1rem;
  border-top: 2px dashed #e5e7eb;
}
.co-saved {
  text-align: right;
  font-size: 0.82rem;
  color: #15803d;
}
.co-submit {
  margin-top: 1.1rem;
  padding: 0.85rem;
  border-radius: 999px;
  font-weight: 800;
  box-shadow: 0 8px 20px rgba(var(--bs-primary-rgb), 0.28);
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}
.co-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(var(--bs-primary-rgb), 0.36);
}
.co-perks {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 16px;
  margin-top: 1rem;
  padding-top: 0.9rem;
  border-top: 1px solid #f1f5f9;
  font-size: 0.78rem;
  color: #64748b;
}
.co-perks i {
  margin-right: 5px;
}

/* ===== Success / Empty ===== */
.co-success {
  max-width: 560px;
  margin: 2rem auto;
  text-align: center;
  padding: 3rem 1.5rem;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 24px;
  box-shadow: 0 10px 34px rgba(15, 23, 42, 0.08);
}
.co-success-ico {
  width: 84px;
  height: 84px;
  margin: 0 auto 1.1rem;
  display: grid;
  place-items: center;
  font-size: 2.4rem;
  color: #fff;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  border-radius: 50%;
  box-shadow: 0 10px 24px rgba(22, 163, 74, 0.35);
}
.co-success-ico.empty {
  background: #f1f5f9;
  color: #94a3b8;
  box-shadow: none;
}
.co-success-code {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1rem;
  padding: 10px 18px;
  background: #fef2f2;
  border: 1px dashed #fca5a5;
  border-radius: 14px;
}
.co-success-code strong {
  color: #dc2626;
  font-size: 1.15rem;
}
.co-copy {
  width: 30px;
  height: 30px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
}
.co-copy:hover {
  color: #dc2626;
  border-color: #dc2626;
}

@media (max-width: 991.98px) {
  .co-sticky {
    position: static;
  }
}
@media (max-width: 575.98px) {
  .co-step-name {
    display: none;
  }
  .co-step.current .co-step-name {
    display: inline;
  }
  .co-card {
    padding: 1.1rem;
  }
}
</style>
