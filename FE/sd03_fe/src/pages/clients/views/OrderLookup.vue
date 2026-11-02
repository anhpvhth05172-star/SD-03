<script setup>
import { ref, computed, unref, onMounted } from "vue";
import { useRoute } from "vue-router";
import * as store from "../store";
import { getUser } from "@/utils/auth";

const { money } = store;
const route = useRoute();

/* ================= CẤU HÌNH API ================= */
const HOA_DON_API = "http://localhost:8080/api/hoa-don";

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
  if (!res.ok) {
    const e = new Error(String(res.status));
    e.status = res.status;
    throw e;
  }
  return res.json();
}

/* >>> ADAPTER: đổi tên field cho khớp HoaDonDTO của back-end <<< */
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
  size: (i) => i.kichCo ?? i.tenKichCo ?? i.size ?? "",
  qty: (i) => Number(i.soLuong ?? i.qty ?? 0),
  price: (i) => Number(i.donGia ?? i.price ?? 0),
  image: (i) => i.hinhAnh ?? i.img ?? null,
};

// id cố định theo bảng phuong_thuc_thanh_toan (khớp trang thanh toán)
const PAY_LABEL = {
  11: "Thanh toán khi nhận hàng (COD)",
  2: "Chuyển khoản ngân hàng",
  4: "Thanh toán qua VNPAY",
};

// Trạng thái đơn (khớp TRANG_THAI_MAP của BE) -> nhãn, màu, bước tiến độ
// step: 0-3 là bước hiện tại, 4 = hoàn tất, -1 = kết thúc không giao (hủy / hoàn tiền)
const STATUS = {
  CHO_THANH_TOAN: { label: "Chờ thanh toán", tone: "warn", step: 1 },
  CHO_XAC_NHAN: { label: "Chờ xác nhận", tone: "warn", step: 1 },
  DA_XAC_NHAN: { label: "Đã xác nhận", tone: "info", step: 2 },
  CHO_GIAO_HANG: { label: "Chờ giao hàng", tone: "info", step: 2 },
  DANG_GIAO_HANG: { label: "Đang giao hàng", tone: "info", step: 2 },
  DA_GIAO_HANG: { label: "Đã giao hàng", tone: "ok", step: 4 },
  DA_HOAN_THANH: { label: "Đã hoàn thành", tone: "ok", step: 4 },
  DA_HUY: { label: "Đã hủy", tone: "danger", step: -1 },
  DA_HOAN_TIEN: { label: "Đã hoàn tiền", tone: "danger", step: -1 },
};
const statusInfo = (code) =>
  STATUS[code] || { label: code || "Đang xử lý", tone: "info", step: 1 };

function fmtDate(raw) {
  if (!raw) return "";
  const d = new Date(raw);
  return isNaN(d) ? String(raw) : d.toLocaleString("vi-VN");
}

// Chuẩn hóa 1 hóa đơn từ BE thành object dùng cho giao diện
function normalize(h) {
  const st = HD.status(h);
  const payId = HD.payId(h);
  return {
    id: HD.id(h),
    customerId: HD.customerId(h),
    code: String(HD.code(h)),
    date: fmtDate(HD.date(h)),
    ts: new Date(HD.date(h) || 0).getTime() || 0,
    status: st,
    statusInfo: statusInfo(st),
    name: HD.name(h),
    phone: HD.phone(h),
    address: HD.address(h),
    note: HD.note(h),
    pay: PAY_LABEL[payId] || HD.payName(h) || "Thanh toán khi nhận hàng (COD)",
    ship: HD.ship(h),
    discount: HD.discount(h),
    total: HD.total(h),
    items: HD.items(h).map((i) => ({
      name: HDI.name(i),
      size: HDI.size(i),
      qty: HDI.qty(i),
      price: HDI.price(i),
      image: HDI.image(i),
    })),
    hasItems: HD.items(h).length > 0,
  };
}

/* ================= CHỈ XEM ĐƠN CỦA TÀI KHOẢN ĐANG ĐĂNG NHẬP ================= */
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
  const a = unref(store.auth);
  const au = unref(a?.user) || a;
  const p = decodeJwt(getToken());
  return {
    loggedIn: !!(u || getToken()),
    id: u?.id ?? au?.id ?? p?.id ?? p?.userId ?? null,
    username:
      u?.tenTaiKhoan ??
      u?.username ??
      au?.tenTaiKhoan ??
      au?.username ??
      p?.sub ??
      p?.username ??
      null,
    email: u?.email ?? au?.email ?? null,
  };
}

// Xác định id khách hàng: ưu tiên id có sẵn, không có thì tra qua API khach-hang
async function resolveCustomerId() {
  const info = getLoginInfo();
  if (info.id != null) return info.id;
  if (!info.username && !info.email) return null;
  try {
    const page = await getJson(`${KHACH_HANG_API}?size=1000`);
    const list = page.content || page || [];
    const c = list.find(
      (k) =>
        (info.username && k.tenTaiKhoan === info.username) ||
        (info.email && k.email === info.email),
    );
    return c?.id ?? null;
  } catch (e) {
    console.warn("[Auth] Không tra được id khách hàng:", e.message);
    return null;
  }
}

const myId = ref(null); // id khách hàng (được xác định khi trang tải)
const isLoggedIn = ref(getLoginInfo().loggedIn);
const isMine = (o) =>
  o && o.customerId != null && String(o.customerId) === String(myId.value);

// Lấy chi tiết hóa đơn theo id; trả về null nếu không phải đơn của mình
async function fetchById(id) {
  const o = normalize(await getJson(`${HOA_DON_API}/${id}`));
  return isMine(o) ? o : null;
}

/* ================= ĐƠN GẦN ĐÂY (đồng bộ trạng thái từ BE) ================= */
// localStorage chỉ giữ tham chiếu (mã + id) để biết khách đã đặt đơn nào,
// toàn bộ dữ liệu hiển thị được tải lại từ back-end.
const storedRefs = () => {
  try {
    const list = JSON.parse(localStorage.getItem("ps_orders")) || [];
    return list.filter((o) => o && o.orderId);
  } catch {
    return [];
  }
};

const recentOrders = ref([]);
const loadingRecent = ref(false);

async function loadRecent() {
  // Chưa đăng nhập -> không hiện đơn nào
  if (!myId.value) {
    recentOrders.value = [];
    return;
  }
  loadingRecent.value = true;
  try {
    // 1) Lấy danh sách đơn của khách hàng đang đăng nhập từ BE
    const page = await getJson(
      `${HOA_DON_API}?idKhachHang=${encodeURIComponent(myId.value)}&size=50`,
    );
    const list = (page.content || page || [])
      .map(normalize)
      .filter(isMine) // chốt chặn phía FE: chỉ giữ đơn của mình
      .sort((a, b) => b.ts - a.ts)
      .slice(0, 10);

    // 2) Danh sách có thể không kèm chi tiết sản phẩm -> tải lại theo id
    const full = await Promise.all(
      list.map((o) => (o.hasItems ? o : fetchById(o.id).catch(() => null))),
    );
    recentOrders.value = full.filter(Boolean);
  } catch (e) {
    console.warn("[TraCuu] Không tải được danh sách đơn:", e.message);
    // 3) Dự phòng: dùng id đơn đã lưu ở máy, vẫn chỉ giữ đơn của mình
    const refs = storedRefs().slice(0, 10);
    const results = await Promise.all(
      refs.map((r) => fetchById(r.orderId).catch(() => null)),
    );
    recentOrders.value = results.filter(Boolean);
  } finally {
    loadingRecent.value = false;
  }
}

/* ================= TRA CỨU ================= */
const searchInput = ref(route.query.code || "");
const searched = ref(false);
const searching = ref(false);
const currentResult = ref(null);
const errorMessage = ref("");

const isPhone = (s) => /^0\d{9}$/.test(s);

async function findOrder(query) {
  // 1) Chuỗi toàn số (không phải SĐT) -> coi là id hóa đơn
  if (/^\d+$/.test(query) && !isPhone(query)) {
    try {
      const r = await fetchById(query);
      if (r) return r;
    } catch (e) {
      if (e.status !== 404) throw e;
    }
  }

  // 2) Tìm theo mã hóa đơn / số điện thoại
  const page = await getJson(
    `${HOA_DON_API}?keyword=${encodeURIComponent(query)}&idKhachHang=${encodeURIComponent(myId.value)}&size=50`,
  );
  const list = page.content || page || [];
  // Chỉ giữ đơn khớp mã/SĐT VÀ thuộc về tài khoản đang đăng nhập
  const matches = list.filter(
    (h) =>
      (String(HD.code(h)).toUpperCase() === query ||
        (isPhone(query) && HD.phone(h) === query)) &&
      String(HD.customerId(h)) === String(myId.value),
  );
  if (!matches.length) return null;

  // Nhiều đơn cùng SĐT -> lấy đơn mới nhất
  const newest = matches
    .map((h) => ({ h, ts: new Date(HD.date(h) || 0).getTime() || 0 }))
    .sort((a, b) => b.ts - a.ts)[0].h;

  // 3) Danh sách có thể không kèm chi tiết sản phẩm -> tải lại theo id
  const n = normalize(newest);
  return n.hasItems ? n : await fetchById(n.id);
}

const doSearch = async (codeToSearch) => {
  if (searching.value) return;
  const raw = String(codeToSearch ?? searchInput.value ?? "").trim();
  const query = (raw.startsWith("#") ? raw.slice(1) : raw).toUpperCase();
  if (!query) {
    errorMessage.value = "Vui lòng nhập mã đơn hàng hoặc số điện thoại.";
    return;
  }
  if (!myId.value) {
    errorMessage.value = isLoggedIn.value
      ? "Không xác định được tài khoản của bạn, vui lòng đăng nhập lại."
      : "Vui lòng đăng nhập để tra cứu đơn hàng của bạn.";
    return;
  }
  errorMessage.value = "";
  searchInput.value = query;
  searched.value = true;
  searching.value = true;
  currentResult.value = null;

  try {
    currentResult.value = await findOrder(query);
  } catch (e) {
    console.error("[TraCuu] Lỗi tra cứu:", e);
    searched.value = false;
    errorMessage.value =
      e.message === "Failed to fetch"
        ? "Không kết nối được máy chủ, vui lòng thử lại."
        : e.status === 401 || e.status === 403
          ? "Bạn chưa có quyền tra cứu, vui lòng đăng nhập và thử lại."
          : "Không tra cứu được đơn hàng, vui lòng thử lại.";
  } finally {
    searching.value = false;
  }
};

function backToList() {
  searched.value = false;
  searchInput.value = "";
  currentResult.value = null;
  loadRecent(); // làm mới trạng thái đơn
}

/* ---------- Tiến độ giao hàng theo trạng thái thật ---------- */
const stepIndex = computed(() => currentResult.value?.statusInfo.step ?? 1);

// Đơn đã kết thúc theo hướng không giao (hủy / hoàn tiền)
const isEnded = (o) => !!o && ["DA_HUY", "DA_HOAN_TIEN"].includes(o.status);
const endedText = (o) =>
  o?.status === "DA_HOAN_TIEN"
    ? "Đơn hàng đã được hoàn tiền"
    : "Đơn hàng này đã bị hủy";

const isCancelled = computed(() => isEnded(currentResult.value));

const stepClass = (i) => ({
  done: i < stepIndex.value,
  current: i === stepIndex.value,
});

const confirmNote = computed(() => {
  const s = currentResult.value?.status;
  if (s === "CHO_THANH_TOAN") return "Chờ thanh toán";
  if (s === "CHO_XAC_NHAN") return "Chờ cửa hàng xác nhận";
  return "Cửa hàng đã xác nhận";
});

const shipNote = computed(() => {
  const s = currentResult.value?.status;
  if (s === "DA_XAC_NHAN") return "Đang đóng gói";
  if (s === "CHO_GIAO_HANG") return "Chờ bàn giao vận chuyển";
  if (s === "DANG_GIAO_HANG") return "Dự kiến 1-2 ngày";
  return "Dự kiến 1-2 ngày";
});

/* ================= PHẦN THÊM CHO GIAO DIỆN ================= */
// Tab lọc theo trạng thái
const FILTERS = [
  { key: "ALL", label: "Tất cả", icon: "bi-collection", match: () => true },
  {
    key: "pending",
    label: "Chờ xử lý",
    icon: "bi-hourglass-split",
    match: (s) =>
      [
        "CHO_THANH_TOAN",
        "CHO_XAC_NHAN",
        "DA_XAC_NHAN",
        "CHO_GIAO_HANG",
      ].includes(s),
  },
  {
    key: "shipping",
    label: "Đang giao",
    icon: "bi-truck",
    match: (s) => s === "DANG_GIAO_HANG",
  },
  {
    key: "done",
    label: "Hoàn thành",
    icon: "bi-bag-check",
    match: (s) => ["DA_GIAO_HANG", "DA_HOAN_THANH"].includes(s),
  },
  {
    key: "cancel",
    label: "Hủy / Hoàn tiền",
    icon: "bi-x-circle",
    match: (s) => ["DA_HUY", "DA_HOAN_TIEN"].includes(s),
  },
];
const activeFilter = ref("ALL");
const countOf = (key) =>
  recentOrders.value.filter((o) =>
    FILTERS.find((f) => f.key === key).match(o.status),
  ).length;
const filteredOrders = computed(() => {
  const f = FILTERS.find((x) => x.key === activeFilter.value) || FILTERS[0];
  return recentOrders.value.filter((o) => f.match(o.status));
});

// Thu gọn / mở rộng danh sách sản phẩm trong thẻ đơn
const MAX_ITEMS = 2;
const expandedIds = ref({});
const visibleItems = (o) =>
  expandedIds.value[o.id] ? o.items : o.items.slice(0, MAX_ITEMS);
const toggleExpand = (o) => {
  expandedIds.value = {
    ...expandedIds.value,
    [o.id]: !expandedIds.value[o.id],
  };
};

// Thanh tiến độ mini trong thẻ đơn (4 đoạn)
const miniActive = (o, i) => o.statusInfo.step > i;

// Tạm tính = tổng các dòng sản phẩm
const subtotal = (o) => o.items.reduce((s, i) => s + i.price * i.qty, 0);
const itemCount = (o) => o.items.reduce((s, i) => s + i.qty, 0);

// Sao chép mã đơn
const copiedCode = ref("");
async function copyCode(code) {
  try {
    await navigator.clipboard.writeText(code);
    copiedCode.value = code;
    setTimeout(() => {
      if (copiedCode.value === code) copiedCode.value = "";
    }, 1800);
  } catch (e) {
    console.warn("Không sao chép được:", e);
  }
}

onMounted(async () => {
  myId.value = await resolveCustomerId();
  loadRecent();
  if (route.query.code) doSearch(route.query.code);
});
</script>

<template>
  <div class="ol-page">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      <span class="mx-1">/</span>
      <span class="text-dark fw-semibold">Tra cứu đơn hàng</span>
    </nav>

    <!-- ================= HERO TÌM KIẾM ================= -->
    <section class="ol-hero mb-4">
      <div class="ol-hero-shape" aria-hidden="true">
        <i class="bi bi-box-seam"></i>
      </div>
      <div class="ol-hero-dots" aria-hidden="true"></div>

      <div class="ol-hero-body">
        <div class="ol-hero-eyebrow">Kiểm tra tiến độ đơn hàng</div>
        <h1 class="ol-hero-title">
          Theo dõi đơn hàng<br />
          <span>cùng Poly Shoes</span>
        </h1>
        <p class="ol-hero-sub">
          Nhập mã đơn hàng (ví dụ: <strong>HD123456</strong>) hoặc số điện thoại
          đặt hàng để xem tình trạng vận chuyển và chi tiết đơn.
        </p>

        <div class="ol-search">
          <i class="bi bi-search ol-search-ico"></i>
          <input
            v-model="searchInput"
            class="ol-search-input"
            placeholder="Nhập mã đơn hàng hoặc số điện thoại..."
            @keyup.enter="doSearch()"
          />
          <button
            v-if="searchInput"
            type="button"
            class="ol-search-clear"
            title="Xóa"
            @click="searchInput = ''"
          >
            <i class="bi bi-x-lg"></i>
          </button>
          <button
            class="ol-search-btn"
            :disabled="searching"
            @click="doSearch()"
          >
            <span
              v-if="searching"
              class="spinner-border spinner-border-sm me-1"
            ></span>
            {{ searching ? "Đang tra cứu" : "Tra cứu ngay" }}
          </button>
        </div>

        <div v-if="errorMessage" class="ol-error">
          <i class="bi bi-exclamation-circle me-1"></i>{{ errorMessage }}
        </div>

        <div v-if="recentOrders.length" class="ol-recent">
          <span class="ol-recent-label">Đơn bạn đã đặt gần đây:</span>
          <div class="d-flex flex-wrap gap-2 mt-2">
            <button
              v-for="o in recentOrders.slice(0, 4)"
              :key="o.id"
              type="button"
              class="ol-chip"
              @click="doSearch(o.code)"
            >
              <i class="bi bi-receipt me-1"></i>#{{ o.code }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= CHƯA ĐĂNG NHẬP ================= -->
    <section v-if="!isLoggedIn" class="mb-5">
      <div class="ol-state">
        <div class="ol-empty-ico"><i class="bi bi-person-lock"></i></div>
        <h4 class="fw-bold mb-2">Đăng nhập để xem đơn hàng của bạn</h4>
        <p class="text-muted-ps mb-4 mx-auto" style="max-width: 450px">
          Để bảo mật thông tin, bạn chỉ có thể tra cứu những đơn hàng thuộc tài
          khoản của mình.
        </p>
        <RouterLink to="/dang-nhap" class="btn btn-primary ol-pill-btn px-4"
          >Đăng nhập</RouterLink
        >
      </div>
    </section>

    <!-- ================= DANH SÁCH ĐƠN ================= -->
    <section v-if="!searched && loadingRecent" class="mb-5">
      <div class="d-flex flex-column gap-3">
        <div v-for="n in 3" :key="n" class="ol-skel">
          <div class="sk sk-h"></div>
          <div class="sk sk-row"></div>
          <div class="sk sk-row w-75"></div>
        </div>
      </div>
    </section>

    <section v-else-if="!searched && recentOrders.length" class="mb-5">
      <!-- Thống kê -->
      <div class="ol-stats mb-4">
        <div class="ol-stat">
          <div class="ol-stat-ico"><i class="bi bi-receipt"></i></div>
          <div>
            <div class="ol-stat-num">{{ recentOrders.length }}</div>
            <div class="ol-stat-lbl">Tổng đơn hàng</div>
          </div>
        </div>
        <div class="ol-stat warn">
          <div class="ol-stat-ico"><i class="bi bi-hourglass-split"></i></div>
          <div>
            <div class="ol-stat-num">{{ countOf("pending") }}</div>
            <div class="ol-stat-lbl">Chờ xử lý</div>
          </div>
        </div>
        <div class="ol-stat info">
          <div class="ol-stat-ico"><i class="bi bi-truck"></i></div>
          <div>
            <div class="ol-stat-num">{{ countOf("shipping") }}</div>
            <div class="ol-stat-lbl">Đang giao</div>
          </div>
        </div>
        <div class="ol-stat ok">
          <div class="ol-stat-ico"><i class="bi bi-bag-check"></i></div>
          <div>
            <div class="ol-stat-num">{{ countOf("done") }}</div>
            <div class="ol-stat-lbl">Hoàn thành</div>
          </div>
        </div>
      </div>

      <div class="ol-head">
        <div class="ol-eyebrow">/ Lịch sử đặt hàng</div>
        <h3 class="ol-h">Đơn hàng bạn đã đặt</h3>
      </div>

      <!-- Tab lọc -->
      <div class="ol-tabs mb-3">
        <button
          v-for="f in FILTERS"
          :key="f.key"
          type="button"
          class="ol-tab"
          :class="{ active: activeFilter === f.key }"
          @click="activeFilter = f.key"
        >
          <i class="bi" :class="f.icon"></i>
          {{ f.label }}
          <span class="ol-tab-count">{{ countOf(f.key) }}</span>
        </button>
      </div>

      <div v-if="filteredOrders.length" class="d-flex flex-column gap-3">
        <article
          v-for="o in filteredOrders"
          :key="o.id"
          class="ol-order"
          :class="o.statusInfo.tone"
        >
          <!-- Đầu thẻ -->
          <div class="ol-order-top">
            <div>
              <div class="d-flex align-items-center flex-wrap gap-2">
                <span class="ol-order-code">#{{ o.code }}</span>
                <button
                  type="button"
                  class="ol-copy"
                  :title="copiedCode === o.code ? 'Đã sao chép' : 'Sao chép mã'"
                  @click="copyCode(o.code)"
                >
                  <i
                    class="bi"
                    :class="copiedCode === o.code ? 'bi-check2' : 'bi-copy'"
                  ></i>
                </button>
                <span class="ol-status" :class="o.statusInfo.tone">
                  <i class="bi bi-dot"></i>{{ o.statusInfo.label }}
                </span>
              </div>
              <div class="text-muted-ps small mt-1">
                <i class="bi bi-clock me-1"></i>{{ o.date }} ·
                {{ itemCount(o) }} sản phẩm
              </div>
            </div>
            <div class="text-sm-end">
              <div class="text-muted-ps small">Tổng tiền</div>
              <span class="price fs-5">{{ money(o.total) }}</span>
            </div>
          </div>

          <!-- Tiến độ mini -->
          <div v-if="!isEnded(o)" class="ol-mini" :class="o.statusInfo.tone">
            <span
              v-for="i in 4"
              :key="i"
              class="ol-mini-seg"
              :class="{ on: miniActive(o, i - 1) }"
            ></span>
          </div>
          <div v-else class="ol-mini-cancel">
            <i class="bi bi-x-circle me-1"></i>{{ endedText(o) }}
          </div>

          <!-- Sản phẩm -->
          <div class="ol-items">
            <div
              v-for="(item, idx) in visibleItems(o)"
              :key="idx + '-' + item.size"
              class="ol-item"
            >
              <div class="ol-thumb">
                <img
                  v-if="item.image"
                  :src="item.image"
                  :alt="item.name"
                  class="ol-thumb-img"
                />
                <template v-else>👟</template>
              </div>
              <div class="flex-grow-1 min-w-0">
                <div class="fw-semibold text-dark small ol-item-name">
                  {{ item.name }}
                </div>
                <div class="small text-muted-ps">
                  <span class="ol-tag">Size {{ item.size }}</span>
                  <span class="ms-1">× {{ item.qty }}</span>
                </div>
              </div>
              <span class="fw-semibold text-dark small">{{
                money(item.price * item.qty)
              }}</span>
            </div>

            <button
              v-if="o.items.length > MAX_ITEMS"
              type="button"
              class="ol-more"
              @click="toggleExpand(o)"
            >
              <template v-if="expandedIds[o.id]">
                Thu gọn <i class="bi bi-chevron-up ms-1"></i>
              </template>
              <template v-else>
                Xem thêm {{ o.items.length - MAX_ITEMS }} sản phẩm
                <i class="bi bi-chevron-down ms-1"></i>
              </template>
            </button>
          </div>

          <!-- Chân thẻ -->
          <div class="ol-order-foot">
            <div class="small text-muted-ps ol-foot-info">
              <i class="bi bi-geo-alt-fill text-primary me-1"></i>
              <span class="fw-semibold text-dark">{{ o.name }}</span> ·
              {{ o.phone }}
              <span
                v-if="o.address"
                class="text-truncate d-inline-block align-bottom ol-addr"
              >
                · {{ o.address }}
              </span>
            </div>
            <button
              class="btn btn-primary btn-sm ol-pill-btn"
              @click="doSearch(o.code)"
            >
              <i class="bi bi-geo me-1"></i>Theo dõi tiến độ
            </button>
          </div>
        </article>
      </div>

      <div v-else class="ol-state ol-state-sm">
        <div class="ol-empty-ico"><i class="bi bi-inbox"></i></div>
        <h6 class="fw-bold mb-1">Không có đơn nào ở mục này</h6>
        <button
          class="btn btn-link btn-sm text-decoration-none"
          @click="activeFilter = 'ALL'"
        >
          Xem tất cả đơn hàng
        </button>
      </div>
    </section>

    <!-- ================= KẾT QUẢ TRA CỨU ================= -->
    <section v-if="searched" class="mb-5">
      <button class="ol-back" @click="backToList">
        <i class="bi bi-arrow-left me-1"></i>Quay lại danh sách đơn hàng
      </button>

      <!-- Loading -->
      <div v-if="searching" class="ol-skel">
        <div class="sk sk-h"></div>
        <div class="sk sk-row"></div>
        <div class="sk sk-row w-75"></div>
        <div class="sk sk-row w-50"></div>
      </div>

      <div v-else-if="currentResult" class="ol-result">
        <!-- Đầu kết quả -->
        <div class="ol-result-top" :class="currentResult.statusInfo.tone">
          <div>
            <div class="ol-result-label">Mã đơn hàng</div>
            <div class="d-flex align-items-center flex-wrap gap-2">
              <h3 class="mb-0 fw-bold">#{{ currentResult.code }}</h3>
              <button
                type="button"
                class="ol-copy light"
                :title="
                  copiedCode === currentResult.code
                    ? 'Đã sao chép'
                    : 'Sao chép mã'
                "
                @click="copyCode(currentResult.code)"
              >
                <i
                  class="bi"
                  :class="
                    copiedCode === currentResult.code ? 'bi-check2' : 'bi-copy'
                  "
                ></i>
              </button>
            </div>
            <div class="ol-result-date">
              <i class="bi bi-clock me-1"></i>Đặt lúc {{ currentResult.date }}
            </div>
          </div>
          <span class="ol-status big" :class="currentResult.statusInfo.tone">
            <i class="bi bi-dot"></i>{{ currentResult.statusInfo.label }}
          </span>
        </div>

        <div class="ol-result-body">
          <!-- Đơn đã hủy / hoàn tiền -->
          <div v-if="isCancelled" class="ol-cancel-box">
            <i class="bi bi-x-circle-fill"></i>
            <div>
              <div class="fw-bold">{{ endedText(currentResult) }}</div>
              <div class="small">
                Vui lòng liên hệ hotline <strong>1900 1234</strong> nếu cần hỗ
                trợ.
              </div>
            </div>
          </div>

          <!-- Tiến độ giao hàng -->
          <div v-else class="ol-steps">
            <div class="ol-step" :class="stepClass(0)">
              <div class="ol-step-dot"><i class="bi bi-check-lg"></i></div>
              <div class="ol-step-name">Đã đặt hàng</div>
              <div class="ol-step-note">{{ currentResult.date }}</div>
            </div>
            <div class="ol-step" :class="stepClass(1)">
              <div class="ol-step-dot"><i class="bi bi-box-seam"></i></div>
              <div class="ol-step-name">Đã xác nhận</div>
              <div class="ol-step-note">{{ confirmNote }}</div>
            </div>
            <div class="ol-step" :class="stepClass(2)">
              <div class="ol-step-dot"><i class="bi bi-truck"></i></div>
              <div class="ol-step-name">Đang vận chuyển</div>
              <div class="ol-step-note">{{ shipNote }}</div>
            </div>
            <div class="ol-step" :class="stepClass(3)">
              <div class="ol-step-dot"><i class="bi bi-house-check"></i></div>
              <div class="ol-step-name">Giao thành công</div>
              <div class="ol-step-note">Nhận hàng &amp; kiểm tra</div>
            </div>
          </div>

          <div class="row g-4">
            <!-- Người nhận -->
            <div class="col-lg-5">
              <div class="ol-box h-100">
                <h6 class="ol-box-title">
                  <i class="bi bi-person-lines-fill"></i>Thông tin người nhận
                </h6>
                <div class="ol-kv">
                  <span>Họ và tên</span
                  ><strong>{{ currentResult.name || "—" }}</strong>
                </div>
                <div class="ol-kv">
                  <span>Số điện thoại</span
                  ><strong>{{ currentResult.phone || "—" }}</strong>
                </div>
                <div class="ol-kv">
                  <span>Địa chỉ giao</span
                  ><strong>{{ currentResult.address || "—" }}</strong>
                </div>
                <div class="ol-kv">
                  <span>Thanh toán</span
                  ><strong>{{ currentResult.pay }}</strong>
                </div>
                <div v-if="currentResult.note" class="ol-kv">
                  <span>Ghi chú</span><strong>{{ currentResult.note }}</strong>
                </div>
              </div>
            </div>

            <!-- Sản phẩm + tổng -->
            <div class="col-lg-7">
              <div class="ol-box h-100">
                <h6 class="ol-box-title">
                  <i class="bi bi-bag-check"></i>Sản phẩm trong đơn
                  <span class="ol-box-count">{{
                    itemCount(currentResult)
                  }}</span>
                </h6>

                <div
                  v-for="(item, idx) in currentResult.items"
                  :key="idx + '-' + item.size"
                  class="ol-item in-box"
                >
                  <div class="ol-thumb">
                    <img
                      v-if="item.image"
                      :src="item.image"
                      :alt="item.name"
                      class="ol-thumb-img"
                    />
                    <template v-else>👟</template>
                  </div>
                  <div class="flex-grow-1 min-w-0">
                    <div class="fw-semibold text-dark small ol-item-name">
                      {{ item.name }}
                    </div>
                    <div class="small text-muted-ps">
                      <span class="ol-tag">Size {{ item.size }}</span>
                      <span class="ms-1">× {{ item.qty }}</span>
                      <span class="ms-1">· {{ money(item.price) }}</span>
                    </div>
                  </div>
                  <span class="price small">{{
                    money(item.price * item.qty)
                  }}</span>
                </div>

                <div class="ol-totals">
                  <div class="ol-line">
                    <span class="small text-muted-ps">Tạm tính</span>
                    <span class="small fw-semibold">{{
                      money(subtotal(currentResult))
                    }}</span>
                  </div>
                  <div v-if="currentResult.ship" class="ol-line">
                    <span class="small text-muted-ps">Phí vận chuyển</span>
                    <span class="small fw-semibold">{{
                      money(currentResult.ship)
                    }}</span>
                  </div>
                  <div
                    v-if="currentResult.discount"
                    class="ol-line text-success"
                  >
                    <span class="small">Giảm giá</span>
                    <span class="small fw-semibold"
                      >-{{ money(currentResult.discount) }}</span
                    >
                  </div>
                  <div class="ol-sum">
                    <span class="fw-bold">Tổng thanh toán</span>
                    <span class="price fs-4">{{
                      money(currentResult.total)
                    }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Hỗ trợ -->
          <div class="ol-help">
            <div class="d-flex align-items-center gap-3">
              <div class="ol-help-ico"><i class="bi bi-headset"></i></div>
              <div>
                <div class="fw-bold">Cần hỗ trợ về đơn hàng này?</div>
                <div class="small text-muted-ps">
                  Gọi hotline kèm mã đơn để được cập nhật nhanh.
                </div>
              </div>
            </div>
            <a href="tel:19001234" class="btn btn-primary ol-pill-btn px-4">
              <i class="bi bi-telephone-fill me-1"></i>1900 1234
            </a>
          </div>
        </div>
      </div>

      <!-- Không tìm thấy -->
      <div v-else class="ol-state">
        <div class="ol-empty-ico"><i class="bi bi-search"></i></div>
        <h4 class="fw-bold mb-2">Không tìm thấy đơn hàng</h4>
        <p class="text-muted-ps mb-4 mx-auto" style="max-width: 450px">
          Không có đơn hàng nào khớp với <strong>"{{ searchInput }}"</strong>.
          Vui lòng kiểm tra lại mã đơn hàng hoặc số điện thoại bạn đã dùng khi
          đặt hàng.
        </p>
        <button class="btn btn-primary ol-pill-btn px-4" @click="backToList">
          Thử lại
        </button>
      </div>
    </section>

    <!-- ================= HỎI ĐÁP ================= -->
    <section class="mb-5">
      <div class="ol-head">
        <div class="ol-eyebrow">/ Hỏi đáp</div>
        <h3 class="ol-h">Câu hỏi thường gặp về giao hàng</h3>
      </div>
      <div class="row g-3">
        <div class="col-md-4">
          <div class="ol-faq h-100">
            <div class="ol-faq-ico"><i class="bi bi-truck"></i></div>
            <h6 class="fw-bold mb-2">Thời gian nhận hàng mất bao lâu?</h6>
            <p class="text-muted-ps small mb-0">
              Giao hàng tiêu chuẩn từ 1–3 ngày làm việc trên toàn quốc, nội
              thành Hà Nội &amp; TP.HCM từ 24h.
            </p>
          </div>
        </div>
        <div class="col-md-4">
          <div class="ol-faq h-100">
            <div class="ol-faq-ico"><i class="bi bi-box-seam"></i></div>
            <h6 class="fw-bold mb-2">Có được kiểm tra hàng trước không?</h6>
            <p class="text-muted-ps small mb-0">
              Có. Poly Shoes cho phép khách mở hộp kiểm tra đúng mẫu, đúng size
              trước khi thanh toán cho shipper.
            </p>
          </div>
        </div>
        <div class="col-md-4">
          <div class="ol-faq h-100">
            <div class="ol-faq-ico"><i class="bi bi-headset"></i></div>
            <h6 class="fw-bold mb-2">Đổi thông tin nhận hàng bằng cách nào?</h6>
            <p class="text-muted-ps small mb-0">
              Vui lòng gọi hotline <strong>1900 1234</strong> kèm mã đơn hàng để
              nhân viên cập nhật kịp thời.
            </p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.ol-page {
  --ol-red: var(--bs-primary, #d62828);
  --ol-red-soft: rgba(var(--bs-primary-rgb, 214, 40, 40), 0.08);
  --ol-card: #ffffff;
  --ol-border: #eef0f3;
  --ol-shadow: 0 4px 20px rgba(15, 23, 42, 0.06);
}
.min-w-0 {
  min-width: 0;
}

/* ---------- Section head ---------- */
.ol-head {
  margin-bottom: 1rem;
}
.ol-eyebrow {
  color: var(--ol-red);
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.ol-h {
  font-weight: 700;
  margin: 0.15rem 0 0.3rem;
}

/* ---------- Hero ---------- */
.ol-hero {
  position: relative;
  overflow: hidden;
  border-radius: 28px;
  padding: 3.25rem 2.75rem;
  color: #fff;
  background: linear-gradient(115deg, #2a0508 0%, #12090a 55%, #0b0b0b 100%);
  box-shadow: 0 16px 40px rgba(18, 9, 10, 0.25);
}
.ol-hero-shape {
  position: absolute;
  top: -30%;
  right: -6%;
  width: 46%;
  height: 160%;
  display: grid;
  place-items: center;
  background: linear-gradient(140deg, #ef2b3a 0%, #8f0f1b 100%);
  transform: rotate(14deg);
  opacity: 0.95;
}
.ol-hero-shape i {
  font-size: 11rem;
  color: rgba(255, 255, 255, 0.16);
  transform: rotate(-14deg);
}
.ol-hero-dots {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 220px;
  height: 140px;
  background-image: radial-gradient(
    rgba(255, 255, 255, 0.14) 1.5px,
    transparent 1.5px
  );
  background-size: 16px 16px;
  pointer-events: none;
}
.ol-hero-body {
  position: relative;
  z-index: 1;
  max-width: 640px;
}
.ol-hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.8);
}
.ol-hero-eyebrow::after {
  content: "";
  width: 36px;
  height: 2px;
  background: var(--ol-red);
}
.ol-hero-title {
  margin: 0 0 0.75rem;
  font-size: clamp(1.9rem, 4.2vw, 3rem);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.02em;
  text-transform: uppercase;
}
.ol-hero-title span {
  color: #ff3b47;
}
.ol-hero-sub {
  margin-bottom: 1.5rem;
  max-width: 520px;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.72);
}
.ol-hero-sub strong {
  color: #fff;
}

.ol-search {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.4rem 0.4rem 1.1rem;
  background: #fff;
  border-radius: 999px;
  box-shadow: 0 12px 34px rgba(0, 0, 0, 0.38);
}
.ol-search-ico {
  color: #9ca3af;
}
.ol-search-input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  padding: 0.6rem 0.25rem;
  font-size: 0.95rem;
  color: #111;
}
.ol-search-clear {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 0.65rem;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.ol-search-clear:hover {
  background: #e2e8f0;
}
.ol-search-btn {
  border: 0;
  border-radius: 999px;
  padding: 0.7rem 1.5rem;
  font-weight: 700;
  white-space: nowrap;
  color: #fff;
  background: linear-gradient(135deg, #ef2b3a, #c1121f);
  box-shadow: 0 6px 16px rgba(193, 18, 31, 0.4);
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}
.ol-search-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(193, 18, 31, 0.5);
}
.ol-search-btn:disabled {
  opacity: 0.75;
  cursor: wait;
}
.ol-error {
  margin-top: 0.75rem;
  font-size: 0.85rem;
  color: #ffb4b9;
}
.ol-recent {
  margin-top: 1.5rem;
  padding-top: 1.1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}
.ol-recent-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
}
.ol-chip {
  padding: 0.4rem 0.9rem;
  font-size: 0.8rem;
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  transition:
    background-color 0.15s,
    border-color 0.15s;
  cursor: pointer;
}
.ol-chip:hover {
  background: var(--ol-red);
  border-color: var(--ol-red);
}

/* ---------- Stats ---------- */
.ol-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.ol-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--ol-card);
  border: 1px solid var(--ol-border);
  border-radius: 18px;
  padding: 16px 18px;
  box-shadow: var(--ol-shadow);
}
.ol-stat-ico {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: var(--ol-red);
  background: var(--ol-red-soft);
}
.ol-stat.warn .ol-stat-ico {
  color: #b45309;
  background: #fffbeb;
}
.ol-stat.info .ol-stat-ico {
  color: #1d4ed8;
  background: #eff6ff;
}
.ol-stat.ok .ol-stat-ico {
  color: #15803d;
  background: #ecfdf3;
}
.ol-stat-num {
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1;
  color: #111827;
}
.ol-stat-lbl {
  font-size: 0.8rem;
  color: #6b7280;
  margin-top: 4px;
}

/* ---------- Tabs ---------- */
.ol-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.ol-tab {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 0.87rem;
  font-weight: 600;
  color: #475569;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.ol-tab:hover {
  border-color: #fca5a5;
  color: var(--ol-red);
}
.ol-tab.active {
  color: #fff;
  background: var(--ol-red);
  border-color: var(--ol-red);
  box-shadow: 0 4px 12px rgba(214, 40, 40, 0.28);
}
.ol-tab-count {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
}
.ol-tab.active .ol-tab-count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* ---------- Order card ---------- */
.ol-order {
  position: relative;
  background: var(--ol-card);
  border: 1px solid var(--ol-border);
  border-left: 5px solid #94a3b8;
  border-radius: 18px;
  padding: 1.3rem 1.4rem;
  box-shadow: var(--ol-shadow);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.ol-order:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.1);
}
.ol-order.ok {
  border-left-color: #22c55e;
}
.ol-order.info {
  border-left-color: #3b82f6;
}
.ol-order.warn {
  border-left-color: #f59e0b;
}
.ol-order.danger {
  border-left-color: #ef4444;
}

.ol-order-top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}
.ol-order-code {
  font-size: 1.1rem;
  font-weight: 800;
  color: #111827;
}

.ol-copy {
  width: 28px;
  height: 28px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 0.8rem;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.15s;
}
.ol-copy:hover {
  color: var(--ol-red);
  border-color: var(--ol-red);
}
.ol-copy.light {
  background: rgba(255, 255, 255, 0.7);
  border-color: rgba(0, 0, 0, 0.08);
}

.ol-status {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.8rem 0.15rem 0.3rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: #15803d;
  background: #ecfdf3;
  border-radius: 999px;
}
.ol-status.warn {
  color: #b45309;
  background: #fffbeb;
}
.ol-status.info {
  color: #1d4ed8;
  background: #eff6ff;
}
.ol-status.danger {
  color: #b91c1c;
  background: #fef2f2;
}
.ol-status .bi {
  font-size: 1.3rem;
  line-height: 1;
}
.ol-status.big {
  font-size: 0.92rem;
  padding: 0.3rem 1rem 0.3rem 0.4rem;
}

/* Mini progress */
.ol-mini {
  display: flex;
  gap: 6px;
  margin: 1rem 0 0.4rem;
}
.ol-mini-seg {
  flex: 1;
  height: 6px;
  border-radius: 6px;
  background: #e5e7eb;
  transition: background 0.3s;
}
.ol-mini.ok .ol-mini-seg.on {
  background: #22c55e;
}
.ol-mini.info .ol-mini-seg.on {
  background: #3b82f6;
}
.ol-mini.warn .ol-mini-seg.on {
  background: #f59e0b;
}
.ol-mini-cancel {
  margin: 1rem 0 0.4rem;
  padding: 8px 12px;
  font-size: 0.82rem;
  font-weight: 600;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 10px;
}

/* Items */
.ol-items {
  margin: 0.8rem 0;
}
.ol-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px dashed #eceff3;
}
.ol-item:last-of-type {
  border-bottom: 0;
}
.ol-item.in-box {
  border-bottom: 1px dashed #e5e7eb;
}
.ol-item-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ol-thumb {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  font-size: 1.6rem;
  border-radius: 14px;
  background: linear-gradient(145deg, #fdf2f2, #fbe9ee);
  overflow: hidden;
}
.ol-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.ol-tag {
  padding: 0.05rem 0.55rem;
  font-size: 0.75rem;
  font-weight: 600;
  background: #f3f4f6;
  border-radius: 6px;
}
.ol-more {
  display: block;
  width: 100%;
  margin-top: 6px;
  padding: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--ol-red);
  background: transparent;
  border: 1px dashed #fecaca;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.ol-more:hover {
  background: var(--ol-red-soft);
}

.ol-order-foot {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  background: var(--ol-red-soft);
  border-radius: 14px;
}
.ol-foot-info {
  min-width: 0;
}
.ol-addr {
  max-width: 300px;
}
.ol-pill-btn {
  border-radius: 999px;
  font-weight: 600;
  padding-inline: 1rem;
}

/* ---------- Result ---------- */
.ol-back {
  margin-bottom: 1rem;
  padding: 0.45rem 1.1rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  cursor: pointer;
  transition:
    border-color 0.15s,
    color 0.15s;
}
.ol-back:hover {
  color: var(--ol-red);
  border-color: var(--ol-red);
}

.ol-result {
  background: var(--ol-card);
  border: 1px solid var(--ol-border);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 10px 34px rgba(15, 23, 42, 0.08);
}
.ol-result-top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1.6rem 2rem;
  background: linear-gradient(120deg, #f8fafc, #eef2f7);
  border-bottom: 1px solid var(--ol-border);
}
.ol-result-top.ok {
  background: linear-gradient(120deg, #f0fdf4, #dcfce7);
}
.ol-result-top.info {
  background: linear-gradient(120deg, #eff6ff, #dbeafe);
}
.ol-result-top.warn {
  background: linear-gradient(120deg, #fffbeb, #fef3c7);
}
.ol-result-top.danger {
  background: linear-gradient(120deg, #fef2f2, #fee2e2);
}
.ol-result-label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #64748b;
}
.ol-result-date {
  margin-top: 4px;
  font-size: 0.85rem;
  color: #64748b;
}
.ol-result-body {
  padding: 2rem;
}

.ol-cancel-box {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 1.75rem;
  padding: 1rem 1.25rem;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 14px;
}
.ol-cancel-box > .bi {
  font-size: 1.8rem;
}

/* Stepper */
.ol-steps {
  display: flex;
  margin-bottom: 2.25rem;
  padding: 1.5rem 0.5rem 1.25rem;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 18px;
}
.ol-step {
  position: relative;
  flex: 1;
  text-align: center;
  padding: 0 0.25rem;
}
.ol-step:not(:last-child)::after {
  content: "";
  position: absolute;
  top: 22px;
  left: calc(50% + 30px);
  right: calc(-50% + 30px);
  height: 3px;
  border-radius: 3px;
  background: #e5e7eb;
}
.ol-step.done:not(:last-child)::after {
  background: var(--ol-red);
}
.ol-step-dot {
  position: relative;
  z-index: 1;
  width: 46px;
  height: 46px;
  margin: 0 auto 0.6rem;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  color: #9ca3af;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
}
.ol-step.done .ol-step-dot {
  color: #fff;
  background: var(--ol-red);
  border-color: var(--ol-red);
}
.ol-step.current .ol-step-dot {
  color: var(--ol-red);
  background: #fff;
  border: 2px solid var(--ol-red);
  box-shadow: 0 0 0 5px var(--ol-red-soft);
  animation: pulse 1.8s ease-in-out infinite;
}
@keyframes pulse {
  0%,
  100% {
    box-shadow: 0 0 0 4px var(--ol-red-soft);
  }
  50% {
    box-shadow: 0 0 0 9px rgba(214, 40, 40, 0.02);
  }
}
.ol-step-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #9ca3af;
}
.ol-step.done .ol-step-name,
.ol-step.current .ol-step-name {
  color: #111;
}
.ol-step-note {
  font-size: 0.75rem;
  color: #6b7280;
}

/* Boxes */
.ol-box {
  padding: 1.25rem 1.35rem;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
}
.ol-box-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.9rem;
  font-weight: 700;
}
.ol-box-title .bi {
  color: var(--ol-red);
}
.ol-box-count {
  margin-left: auto;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 10px;
  color: var(--ol-red);
  background: var(--ol-red-soft);
  border-radius: 999px;
}
.ol-kv {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.6rem 0;
  font-size: 0.88rem;
  border-bottom: 1px dashed #e5e7eb;
}
.ol-kv:last-child {
  border-bottom: 0;
}
.ol-kv span {
  flex-shrink: 0;
  color: #6b7280;
}
.ol-kv strong {
  text-align: right;
  color: #111;
}

.ol-totals {
  margin-top: 0.75rem;
  padding-top: 0.5rem;
}
.ol-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.4rem 0;
}
.ol-sum {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.6rem;
  padding-top: 0.85rem;
  border-top: 2px solid #e5e7eb;
}

.ol-help {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-top: 1.75rem;
  padding: 1rem 1.25rem;
  background: var(--ol-red-soft);
  border-radius: 16px;
}
.ol-help-ico {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: #fff;
  background: var(--ol-red);
  border-radius: 14px;
}

/* ---------- States ---------- */
.ol-state {
  text-align: center;
  padding: 3.5rem 1.5rem;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 22px;
}
.ol-state-sm {
  padding: 2rem 1rem;
}
.ol-empty-ico {
  width: 72px;
  height: 72px;
  margin: 0 auto 1rem;
  display: grid;
  place-items: center;
  font-size: 1.9rem;
  color: var(--ol-red);
  background: var(--ol-red-soft);
  border-radius: 50%;
}

/* Skeleton */
.ol-skel {
  padding: 1.4rem;
  background: #fff;
  border: 1px solid var(--ol-border);
  border-radius: 18px;
  box-shadow: var(--ol-shadow);
}
.sk {
  border-radius: 8px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 37%, #f1f5f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}
.sk-h {
  height: 22px;
  width: 40%;
  margin-bottom: 18px;
}
.sk-row {
  height: 14px;
  margin-bottom: 12px;
}
.w-75 {
  width: 75%;
}
.w-50 {
  width: 50%;
}
@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ---------- FAQ ---------- */
.ol-faq {
  padding: 1.4rem;
  background: #fff;
  border: 1px solid var(--ol-border);
  border-radius: 18px;
  box-shadow: var(--ol-shadow);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.ol-faq:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.1);
}
.ol-faq-ico {
  width: 46px;
  height: 46px;
  margin-bottom: 0.9rem;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: var(--ol-red);
  background: var(--ol-red-soft);
  border-radius: 12px;
}

/* ---------- Responsive ---------- */
@media (max-width: 991.98px) {
  .ol-hero {
    padding: 2.25rem 1.5rem;
  }
  .ol-hero-shape {
    width: 70%;
    right: -30%;
    opacity: 0.5;
  }
  .ol-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 575.98px) {
  .ol-search {
    flex-wrap: wrap;
    border-radius: 18px;
    padding: 0.6rem;
  }
  .ol-search-input {
    flex-basis: 70%;
  }
  .ol-search-btn {
    width: 100%;
  }
  .ol-result-top,
  .ol-result-body {
    padding: 1.25rem;
  }
  .ol-order {
    padding: 1.1rem;
  }
  .ol-step:not(:last-child)::after {
    left: calc(50% + 26px);
    right: calc(-50% + 26px);
  }
  .ol-step-name {
    font-size: 0.72rem;
  }
  .ol-step-note {
    display: none;
  }
  .ol-stat {
    padding: 12px;
    gap: 10px;
  }
  .ol-stat-num {
    font-size: 1.25rem;
  }
}
</style>
