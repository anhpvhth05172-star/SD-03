<script setup>
import { onMounted, ref, computed, watch } from "vue";
import {
  cart,
  cartTotal,
  cartCount,
  money,
  removeItem,
  updateCartItemQty,
  clearCart,
  fetchCart,
} from "../store";
import { getUser } from "@/utils/auth";

const user = ref(getUser());
const actionLoadingId = ref(null);
const actionError = ref("");

// ===== Voucher Logic =====
const VOUCHER_API = "http://localhost:8080/admin/phieu-giam-gia";
const allVouchers = ref([]);
const loadingVouchers = ref(false);
const selectedVoucher = ref(null);

const isPercent = (loai) => /PHAN_TRAM|PERCENT|%/.test(loai);
const isPersonal = (scope) => /CA_NHAN|KHACH_HANG|RIENG|PRIVATE/.test(scope);

const VF = {
  id: (v) => v.id,
  code: (v) => v.maPhieuGiamGia,
  loai: (v) => String(v.loaiGiamGia ?? "").toUpperCase(),
  value: (v) => Number(v.giaTriGiam ?? 0),
  max: (v) => Number(v.giamToiDa ?? 0),
  min: (v) => Number(v.hoaDonToiThieu ?? 0),
  start: (v) => v.ngayBatDau,
  end: (v) => v.ngayKetThuc,
  qty: (v) =>
    v.soLuong != null
      ? Number(v.soLuong) - Number(v.soLuongDaSuDung ?? 0)
      : undefined,
  active: (v) => v.trangThai,
  scope: (v) => String(v.phamVi ?? "").toUpperCase(),
  customerIds: (d) =>
    (d.danhSachKhachHang || d.khachHangIds || d.khachHang || []).map((k) =>
      typeof k === "object" ? (k.id ?? k.idKhachHang) : k,
    ),
};

function checkVoucher(detail, total) {
  const act = VF.active(detail);
  if (act === false || /NGUNG|TAT|INACTIVE|HET_HAN/i.test(String(act))) return false;

  const now = new Date();
  const st = VF.start(detail);
  const en = VF.end(detail);
  if (st && now < new Date(st)) return false;
  if (en && now > new Date(en)) return false;

  const qty = VF.qty(detail);
  if (qty !== undefined && qty !== null && Number(qty) <= 0) return false;

  if (isPersonal(VF.scope(detail))) {
    const cid = user.value?.idKhachHang || user.value?.id;
    if (!cid || !VF.customerIds(detail).includes(cid)) return false;
  }

  const min = VF.min(detail);
  if (total < min) return false;

  return true;
}

function calcDiscount(v, total) {
  if (!v || total < (v.min || 0)) return 0;
  let d = v.type === "percent" ? (total * v.value) / 100 : v.value;
  if (v.max > 0) d = Math.min(d, v.max);
  return Math.max(0, Math.min(Math.floor(d), total));
}

async function loadVouchers() {
  loadingVouchers.value = true;
  try {
    const res = await fetch(`${VOUCHER_API}?size=100`);
    if (!res.ok) throw new Error("Không thể tải voucher");
    const data = await res.json();
    const list = data.content || data || [];

    const detailedList = await Promise.all(
      list.map(async (v) => {
        try {
          const dRes = await fetch(`${VOUCHER_API}/detail/${VF.id(v)}`);
          if (dRes.ok) {
            const dData = await dRes.json();
            return { ...v, ...dData };
          }
        } catch {
          // fallback
        }
        return v;
      })
    );
    allVouchers.value = detailedList;
  } catch (err) {
    allVouchers.value = [];
  } finally {
    loadingVouchers.value = false;
  }
}

// Danh sách voucher đủ điều kiện và tính mức giảm giá
const eligibleVouchers = computed(() => {
  const total = cartTotal.value;
  if (total <= 0) return [];

  return allVouchers.value
    .filter((d) => checkVoucher(d, total))
    .map((d) => {
      const v = {
        id: VF.id(d),
        code: String(VF.code(d)).toUpperCase(),
        name: d.tenPhieuGiamGia || "",
        type: isPercent(VF.loai(d)) ? "percent" : "fixed",
        value: VF.value(d),
        max: VF.max(d),
        min: VF.min(d),
      };
      const disc = calcDiscount(v, total);
      return { v, discount: disc };
    })
    .filter((o) => o.discount > 0)
    .sort((a, b) => b.discount - a.discount); // Ưu đãi tốt nhất lên đầu
});

// Tự động tìm voucher có ưu đãi tốt nhất
const bestVoucherOption = computed(() => {
  return eligibleVouchers.value.length > 0 ? eligibleVouchers.value[0] : null;
});

// Watcher tự động áp dụng voucher tốt nhất
watch(
  [bestVoucherOption, cartTotal],
  ([best]) => {
    if (best) {
      selectedVoucher.value = best.v;
    } else {
      selectedVoucher.value = null;
    }
  },
  { immediate: true }
);

// Số tiền được giảm
const discountAmount = computed(() => {
  return calcDiscount(selectedVoucher.value, cartTotal.value);
});

// Tổng tiền sau giảm
const finalCartTotal = computed(() => {
  return Math.max(0, cartTotal.value - discountAmount.value);
});

onMounted(() => {
  fetchCart();
  loadVouchers();
});

const handleQtyChange = async (item, delta) => {
  const newQty = item.qty + delta;
  if (newQty < 1) return;
  if (item.stock != null && newQty > item.stock) {
    actionError.value = `Kho chỉ còn ${item.stock} sản phẩm`;
    setTimeout(() => (actionError.value = ""), 3000);
    return;
  }
  actionLoadingId.value = item.idGioHangChiTiet || item.id;
  actionError.value = "";
  try {
    if (item.idGioHangChiTiet) {
      await updateCartItemQty(item.idGioHangChiTiet, newQty);
    } else {
      item.qty = newQty;
    }
  } catch (err) {
    actionError.value = err.message || "Không thể cập nhật số lượng";
    setTimeout(() => (actionError.value = ""), 3000);
  } finally {
    actionLoadingId.value = null;
  }
};

const handleRemove = async (item, idx) => {
  if (!confirm(`Bạn có chắc muốn xóa "${item.name}" khỏi giỏ hàng?`)) return;
  actionLoadingId.value = item.idGioHangChiTiet || item.id;
  actionError.value = "";
  try {
    await removeItem(item.idGioHangChiTiet ? item : idx);
  } catch (err) {
    actionError.value = err.message || "Không thể xóa sản phẩm";
    setTimeout(() => (actionError.value = ""), 3000);
  } finally {
    actionLoadingId.value = null;
  }
};

const handleClear = async () => {
  if (!confirm("Bạn có chắc chắn muốn xóa toàn bộ giỏ hàng?")) return;
  actionError.value = "";
  try {
    await clearCart();
  } catch (err) {
    actionError.value = err.message || "Không thể xóa giỏ hàng";
    setTimeout(() => (actionError.value = ""), 3000);
  }
};
</script>

<template>
  <div class="cart-container">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      /
      <span class="text-dark fw-semibold">Giỏ hàng của tôi</span>
    </nav>

    <div class="d-flex align-items-center justify-content-between mb-4">
      <div>
        <h2 class="mb-1 fw-bold">Giỏ hàng của bạn</h2>
        <p class="text-muted-ps mb-0 small" v-if="cartCount > 0">
          Bạn đang có <strong>{{ cartCount }}</strong> sản phẩm trong giỏ hàng
        </p>
      </div>
      <button
        v-if="cart.items.length > 0"
        class="btn btn-sm btn-outline-danger rounded-pill px-3"
        @click="handleClear"
        :disabled="cart.loading"
      >
        <i class="bi bi-trash me-1"></i> Xóa tất cả
      </button>
    </div>

    <!-- Thông báo đăng nhập nếu là khách -->
    <div v-if="!user" class="alert alert-warning border-0 rounded-4 d-flex align-items-center justify-content-between p-3 mb-4 shadow-sm">
      <div class="d-flex align-items-center gap-2 small">
        <i class="bi bi-info-circle-fill text-warning fs-5"></i>
        <span>Đăng nhập tài khoản để đồng bộ giỏ hàng trên nhiều thiết bị và nhận mã giảm giá thành viên!</span>
      </div>
      <RouterLink to="/dang-nhap?redirect=/gio-hang" class="btn btn-sm btn-dark rounded-pill px-3 fw-bold">
        Đăng nhập ngay
      </RouterLink>
    </div>

    <!-- Cảnh báo lỗi -->
    <div v-if="actionError" class="alert alert-danger rounded-4 py-2 px-3 mb-3">
      <i class="bi bi-exclamation-triangle-fill me-1"></i> {{ actionError }}
    </div>

    <!-- Loading State -->
    <div v-if="cart.loading && !cart.items.length" class="text-center py-5">
      <div class="spinner-border text-danger" role="status">
        <span class="visually-hidden">Đang tải giỏ hàng...</span>
      </div>
      <p class="mt-2 text-muted-ps">Đang đồng bộ giỏ hàng...</p>
    </div>

    <div v-else-if="cart.items.length" class="row g-4">
      <div class="col-lg-8">
        <div class="ps-card p-4">
          <div
            v-for="(i, idx) in cart.items"
            :key="(i.idGioHangChiTiet || i.id) + '-' + i.size"
            class="d-flex align-items-center gap-3 py-3"
            :class="{ 'border-top': idx, 'opacity-75': !i.conHang }"
          >
            <!-- Thumbnail ảnh -->
            <div
              class="ps-thumb flex-shrink-0 position-relative rounded-3"
              style="width: 88px; height: 88px; overflow: hidden;"
            >
              <img
                v-if="i.img"
                :src="i.img"
                :alt="i.name"
                style="width: 100%; height: 100%; object-fit: cover;"
                @error="(e) => { e.target.style.display='none'; e.target.nextSibling.style.display='flex'; }"
              />
              <div
                :style="i.img ? 'display:none; align-items:center; justify-content:center; width:100%; height:100%; font-size:2.4rem;' : 'display:flex; align-items:center; justify-content:center; width:100%; height:100%; font-size:2.4rem;'"
              >
                👟
              </div>
              <span
                v-if="!i.conHang"
                class="badge bg-danger position-absolute top-50 start-50 translate-middle"
                style="font-size: 0.65rem;"
              >
                Hết hàng
              </span>
            </div>

            <!-- Thông tin sản phẩm -->
            <div class="flex-grow-1 min-w-0">
              <div class="fw-bold text-dark text-truncate fs-6">{{ i.name }}</div>
              <div class="small text-muted-ps mt-1">
                <span v-if="i.color">Màu: <strong>{{ i.color }}</strong></span>
                <span v-if="i.color && i.size"> · </span>
                <span v-if="i.size">Size: <strong class="badge bg-light text-dark border">{{ i.size }}</strong></span>
                <span v-if="i.brand" class="d-none d-md-inline ms-1 text-secondary"> ({{ i.brand }})</span>
              </div>
              <div class="price mt-1 fs-5">{{ money(i.price) }}</div>
              <div v-if="i.canhBao" class="text-danger small mt-1">
                ⚠️ {{ i.canhBao }}
              </div>
            </div>

            <!-- Bộ chỉnh số lượng -->
            <div class="input-group input-group-sm rounded-pill overflow-hidden border" style="width: 110px">
              <button
                class="btn btn-light"
                :disabled="actionLoadingId === (i.idGioHangChiTiet || i.id) || i.qty <= 1"
                @click="handleQtyChange(i, -1)"
              >
                −
              </button>
              <input
                class="form-control text-center bg-white border-0 fw-bold"
                :value="i.qty"
                readonly
              />
              <button
                class="btn btn-light"
                :disabled="actionLoadingId === (i.idGioHangChiTiet || i.id) || (i.stock != null && i.qty >= i.stock)"
                @click="handleQtyChange(i, 1)"
              >
                +
              </button>
            </div>

            <!-- Nút xóa -->
            <button
              class="btn btn-sm btn-outline-danger border-0 rounded-circle"
              style="width: 36px; height: 36px;"
              :disabled="actionLoadingId === (i.idGioHangChiTiet || i.id)"
              @click="handleRemove(i, idx)"
              title="Xóa sản phẩm"
            >
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Tóm tắt đơn hàng -->
      <div class="col-lg-4">
        <div class="ps-card p-4">
          <h5 class="fw-bold mb-3">Tóm tắt đơn hàng</h5>
          <!-- Voucher tự động áp dụng -->
          <div v-if="selectedVoucher" class="alert alert-success border-0 py-2 px-3 rounded-3 d-flex align-items-center justify-content-between mb-3 shadow-sm">
            <div class="small">
              <div class="d-flex align-items-center gap-1 fw-bold text-success">
                <i class="bi bi-tag-fill"></i>
                <span>{{ selectedVoucher.code }}</span>
                <span class="badge bg-success text-white ms-1" style="font-size: 0.65rem">Ưu đãi tốt nhất</span>
              </div>
              <div class="text-secondary" style="font-size: 0.75rem">
                {{ selectedVoucher.type === 'percent' ? `Giảm ${selectedVoucher.value}%` : `Giảm ${money(selectedVoucher.value)}` }}
                {{ selectedVoucher.max > 0 ? `(tối đa ${money(selectedVoucher.max)})` : '' }}
              </div>
            </div>
            <div class="fw-bold text-success small">-{{ money(discountAmount) }}</div>
          </div>

          <div class="d-flex justify-content-between mb-2">
            <span class="text-muted-ps">Tạm tính:</span>
            <span class="fw-semibold">{{ money(cartTotal) }}</span>
          </div>

          <div v-if="selectedVoucher && discountAmount > 0" class="d-flex justify-content-between mb-3 text-success">
            <span>Giảm giá voucher:</span>
            <span class="fw-bold">-{{ money(discountAmount) }}</span>
          </div>

          <div class="d-flex justify-content-between border-top pt-3 mb-4">
            <span class="fw-bold fs-5">Tổng cộng:</span>
            <span class="price fs-4">{{ money(finalCartTotal) }}</span>
          </div>
          <RouterLink to="/thanh-toan" class="btn btn-primary w-100 py-3 rounded-pill fw-bold shadow-sm mb-2">
            Tiến hành đặt hàng <i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
          <RouterLink to="/cua-hang" class="btn btn-light-border w-100 py-2 rounded-pill fw-semibold text-center d-block">
            Tiếp tục mua hàng
          </RouterLink>

          <!-- Safe checkout badges -->
          <div class="mt-4 pt-3 border-top text-center">
            <div class="small text-muted-ps mb-2">ĐẢM BẢO AN TÂM KHI MUA SẮM</div>
            <div class="d-flex justify-content-center gap-3 text-secondary small">
              <span><i class="bi bi-shield-lock-fill text-success"></i> Bảo mật 100%</span>
              <span><i class="bi bi-box-seam-fill text-danger"></i> Kiểm tra khi nhận</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Trống giỏ hàng -->
    <div v-else class="ps-card text-center p-5">
      <div class="display-3 mb-3">🛒</div>
      <h4 class="fw-bold">Giỏ hàng của bạn đang trống</h4>
      <p class="text-muted-ps mb-4" style="max-width: 420px; margin: 0 auto">
        Hãy chọn một đôi giày bạn ưng ý nhất để tự tin sải bước cùng Poly Shoes nhé!
      </p>
      <RouterLink to="/cua-hang" class="btn btn-primary rounded-pill px-4 py-2 fw-bold">
        <i class="bi bi-shop me-1"></i> Mua sắm ngay
      </RouterLink>
    </div>
  </div>
</template>
