<script setup>
import { onMounted, ref, computed, watch } from "vue";
import { useRouter } from "vue-router";
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

const router = useRouter();
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

// ignoreMin = true: bỏ qua điều kiện đơn tối thiểu (dùng cho gợi ý "mua thêm")
function checkVoucher(detail, total, ignoreMin = false) {
  const act = VF.active(detail);
  if (act === false || /NGUNG|TAT|INACTIVE|HET_HAN/i.test(String(act)))
    return false;

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

  if (!ignoreMin) {
    const min = VF.min(detail);
    if (total < min) return false;
  }

  return true;
}

function calcDiscount(v, total) {
  if (!v || total < (v.min || 0)) return 0;
  let d = v.type === "percent" ? (total * v.value) / 100 : v.value;
  if (v.max > 0) d = Math.min(d, v.max);
  return Math.max(0, Math.min(Math.floor(d), total));
}

const toVoucher = (d) => ({
  id: VF.id(d),
  code: String(VF.code(d)).toUpperCase(),
  name: d.tenPhieuGiamGia || "",
  type: isPercent(VF.loai(d)) ? "percent" : "fixed",
  value: VF.value(d),
  max: VF.max(d),
  min: VF.min(d),
});

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
      }),
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
      const v = toVoucher(d);
      return { v, discount: calcDiscount(v, total) };
    })
    .filter((o) => o.discount > 0)
    .sort((a, b) => b.discount - a.discount); // Ưu đãi tốt nhất lên đầu
});

// Tự động tìm voucher có ưu đãi tốt nhất
const bestVoucherOption = computed(() =>
  eligibleVouchers.value.length > 0 ? eligibleVouchers.value[0] : null,
);

// Watcher tự động áp dụng voucher tốt nhất
watch(
  [bestVoucherOption, cartTotal],
  ([best]) => {
    selectedVoucher.value = best ? best.v : null;
  },
  { immediate: true },
);

// Số tiền được giảm
const discountAmount = computed(() =>
  calcDiscount(selectedVoucher.value, cartTotal.value),
);

// Tổng tiền sau giảm
const finalCartTotal = computed(() =>
  Math.max(0, cartTotal.value - discountAmount.value),
);

// Gợi ý: mua thêm bao nhiêu để mở khóa voucher tốt hơn
const nextVoucherHint = computed(() => {
  const total = cartTotal.value;
  if (total <= 0) return null;

  let best = null;
  for (const d of allVouchers.value) {
    if (!checkVoucher(d, total, true)) continue;
    const v = toVoucher(d);
    if (v.min <= total) continue; // đã đủ điều kiện, không cần gợi ý
    const disc = calcDiscount(v, v.min);
    if (disc <= discountAmount.value) continue; // không tốt hơn voucher hiện tại
    const need = v.min - total;
    if (!best || need < best.need) best = { v, need, disc };
  }
  return best;
});

// ===== Thông tin tổng hợp =====
const lineTotal = (i) => Number(i.price || 0) * Number(i.qty || 0);
const totalQty = computed(() =>
  cart.items.reduce((s, i) => s + Number(i.qty || 0), 0),
);
const hasOutOfStock = computed(() =>
  cart.items.some((i) => i.conHang === false),
);
const rowId = (i) => i.idGioHangChiTiet || i.id;
const rowKey = (i) => `${rowId(i)}-${i.size}`;
const LOW_STOCK = 5;

// Ảnh lỗi -> hiện emoji thay thế
const imgFailed = ref({});

// ===== Bắt buộc đăng nhập để xem giỏ hàng =====
const LOGIN_URL = "/dang-nhap?redirect=/gio-hang";

onMounted(() => {
  // Chưa đăng nhập -> chuyển sang trang đăng nhập, không tải dữ liệu giỏ hàng
  if (!user.value) {
    router.replace(LOGIN_URL);
    return;
  }
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
  actionLoadingId.value = rowId(item);
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
  actionLoadingId.value = rowId(item);
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
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none"
        >Trang chủ</RouterLink
      >
      <span class="mx-1">/</span>
      <span class="text-dark fw-semibold">Giỏ hàng của tôi</span>
    </nav>

    <!-- Chưa đăng nhập: bắt buộc đăng nhập mới xem được giỏ hàng -->
    <div v-if="!user" class="login-required">
      <div class="login-ico"><i class="bi bi-lock-fill"></i></div>
      <h4 class="fw-bold">Vui lòng đăng nhập để xem giỏ hàng</h4>
      <p class="text-muted-ps mb-4 mx-auto" style="max-width: 420px">
        Bạn cần đăng nhập để xem, chỉnh sửa giỏ hàng và nhận các mã giảm giá
        dành riêng cho thành viên.
      </p>
      <RouterLink
        to="/khach-hang?redirect=/gio-hang"
        class="btn btn-primary rounded-pill px-4 py-2 fw-bold"
      >
        <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập ngay
      </RouterLink>
    </div>


    <!-- Đã đăng nhập -->
    <template v-else>
      <!-- Header -->
      <div class="cart-header mb-4">
        <div class="d-flex align-items-center gap-3">
          <div class="cart-header-ico">
            <i class="bi bi-bag-heart-fill"></i>
          </div>
          <div>
            <h2 class="cart-title mb-0">Giỏ hàng của bạn</h2>
            <p v-if="cart.items.length" class="text-muted-ps mb-0 small">
              <strong>{{ cart.items.length }}</strong> sản phẩm ·
              <strong>{{ totalQty }}</strong> đôi/chiếc trong giỏ
            </p>
          </div>
        </div>
        <button
          v-if="cart.items.length > 0"
          class="btn btn-sm btn-outline-danger rounded-pill px-3"
          :disabled="cart.loading"
          @click="handleClear"
        >
          <i class="bi bi-trash me-1"></i> Xóa tất cả
        </button>
      </div>

      <!-- Lỗi thao tác -->
      <div v-if="actionError" class="notice notice-danger mb-3">
        <div class="small">
          <i class="bi bi-exclamation-triangle-fill me-1"></i>{{ actionError }}
        </div>
      </div>

      <!-- Có sản phẩm hết hàng -->
      <div v-if="hasOutOfStock" class="notice notice-danger mb-3">
        <div class="small">
          <i class="bi bi-exclamation-octagon-fill me-1"></i>
          Giỏ hàng có sản phẩm đã hết hàng, vui lòng xóa hoặc đổi sản phẩm khác
          trước khi đặt hàng.
        </div>
      </div>

      <!-- Loading skeleton -->
      <div v-if="cart.loading && !cart.items.length" class="row g-4">
        <div class="col-lg-8">
          <div v-for="n in 3" :key="n" class="skel-row">
            <div class="sk sk-img"></div>
            <div class="flex-grow-1">
              <div class="sk sk-line w-50"></div>
              <div class="sk sk-line w-75"></div>
              <div class="sk sk-line w-25"></div>
            </div>
          </div>
        </div>
        <div class="col-lg-4">
          <div class="summary-card">
            <div class="sk sk-line w-50"></div>
            <div class="sk sk-line"></div>
            <div class="sk sk-line"></div>
            <div class="sk sk-btn"></div>
          </div>
        </div>
      </div>

      <!-- Giỏ hàng có sản phẩm -->
      <div v-else-if="cart.items.length" class="row g-4">
        <!-- Danh sách sản phẩm -->
        <div class="col-lg-8">
          <div class="d-flex flex-column gap-3">
            <article
              v-for="(i, idx) in cart.items"
              :key="rowKey(i)"
              class="cart-item"
              :class="{ 'is-out': i.conHang === false }"
            >
              <!-- Ảnh -->
              <div class="item-thumb">
                <img
                  v-if="i.img && !imgFailed[rowKey(i)]"
                  :src="i.img"
                  :alt="i.name"
                  @error="imgFailed[rowKey(i)] = true"
                />
                <span v-else class="item-thumb-fallback">👟</span>
                <span v-if="i.conHang === false" class="item-out-badge"
                  >Hết hàng</span
                >
              </div>

              <!-- Thông tin -->
              <div class="item-info">
                <div v-if="i.brand" class="item-brand">{{ i.brand }}</div>
                <div class="item-name" :title="i.name">{{ i.name }}</div>

                <div class="item-tags">
                  <span v-if="i.color" class="item-tag">
                    <i class="bi bi-palette2"></i>{{ i.color }}
                  </span>
                  <span v-if="i.size" class="item-tag">
                    <i class="bi bi-rulers"></i>Size {{ i.size }}
                  </span>
                  <span
                    v-if="
                      i.stock != null &&
                      i.conHang !== false &&
                      i.stock <= LOW_STOCK
                    "
                    class="item-tag tag-low"
                  >
                    <i class="bi bi-fire"></i>Chỉ còn {{ i.stock }} sản phẩm
                  </span>
                </div>

                <div class="item-unit">
                  Đơn giá: <strong>{{ money(i.price) }}</strong>
                </div>

                <div v-if="i.canhBao" class="item-warn">
                  <i class="bi bi-exclamation-triangle-fill me-1"></i
                  >{{ i.canhBao }}
                </div>
              </div>

              <!-- Bên phải: số lượng, thành tiền, xóa -->
              <div class="item-side">
                <div class="item-line-total">{{ money(lineTotal(i)) }}</div>

                <div class="qty-pill">
                  <button
                    type="button"
                    class="qty-btn"
                    :disabled="actionLoadingId === rowId(i) || i.qty <= 1"
                    @click="handleQtyChange(i, -1)"
                  >
                    −
                  </button>
                  <span class="qty-val">
                    <span
                      v-if="actionLoadingId === rowId(i)"
                      class="spinner-border spinner-border-sm"
                    ></span>
                    <template v-else>{{ i.qty }}</template>
                  </span>
                  <button
                    type="button"
                    class="qty-btn"
                    :disabled="
                      actionLoadingId === rowId(i) ||
                      (i.stock != null && i.qty >= i.stock)
                    "
                    @click="handleQtyChange(i, 1)"
                  >
                    +
                  </button>
                </div>

                <button
                  type="button"
                  class="item-remove"
                  :disabled="actionLoadingId === rowId(i)"
                  title="Xóa sản phẩm"
                  @click="handleRemove(i, idx)"
                >
                  <i class="bi bi-trash3"></i><span>Xóa</span>
                </button>
              </div>
            </article>
          </div>

          <RouterLink to="/cua-hang" class="back-shop">
            <i class="bi bi-arrow-left me-1"></i>Tiếp tục mua hàng
          </RouterLink>
        </div>

        <!-- Tóm tắt đơn hàng -->
        <div class="col-lg-4">
          <aside class="summary-card">
            <h5 class="summary-title">
              <i class="bi bi-receipt-cutoff me-2"></i>Tóm tắt đơn hàng
            </h5>

            <!-- Voucher đang áp dụng -->
            <div v-if="selectedVoucher" class="voucher-box applied">
              <div class="voucher-ico">
                <i class="bi bi-ticket-perforated-fill"></i>
              </div>
              <div class="flex-grow-1 min-w-0">
                <div class="voucher-code">
                  {{ selectedVoucher.code }}
                  <span class="voucher-badge">Ưu đãi tốt nhất</span>
                </div>
                <div v-if="selectedVoucher.name" class="voucher-name">
                  {{ selectedVoucher.name }}
                </div>
                <div class="voucher-desc">
                  {{
                    selectedVoucher.type === "percent"
                      ? `Giảm ${selectedVoucher.value}%`
                      : `Giảm ${money(selectedVoucher.value)}`
                  }}
                  <template v-if="selectedVoucher.max > 0">
                    · tối đa {{ money(selectedVoucher.max) }}
                  </template>
                  <template v-if="selectedVoucher.min > 0">
                    · đơn từ {{ money(selectedVoucher.min) }}
                  </template>
                </div>
              </div>
              <div class="voucher-save">-{{ money(discountAmount) }}</div>
            </div>

            <!-- Gợi ý mua thêm -->
            <div v-if="nextVoucherHint" class="voucher-box hint">
              <div class="voucher-ico"><i class="bi bi-gift-fill"></i></div>
              <div class="small">
                Mua thêm <strong>{{ money(nextVoucherHint.need) }}</strong> để
                dùng mã
                <strong>{{ nextVoucherHint.v.code }}</strong>
                (giảm
                {{
                  nextVoucherHint.v.type === "percent"
                    ? `${nextVoucherHint.v.value}%`
                    : money(nextVoucherHint.v.value)
                }})
              </div>
            </div>

            <div v-if="loadingVouchers" class="small text-muted-ps mb-2">
              <span class="spinner-border spinner-border-sm me-1"></span>Đang
              tìm mã giảm giá...
            </div>

            <!-- Bảng giá -->
            <div class="sum-row">
              <span>Tạm tính ({{ totalQty }} sản phẩm)</span>
              <strong>{{ money(cartTotal) }}</strong>
            </div>
            <div
              v-if="selectedVoucher && discountAmount > 0"
              class="sum-row text-success"
            >
              <span>Giảm giá voucher</span>
              <strong>-{{ money(discountAmount) }}</strong>
            </div>
            <div class="sum-row">
              <span>Phí vận chuyển</span>
              <span class="text-muted-ps small">Tính ở bước thanh toán</span>
            </div>

            <div class="sum-total">
              <span>Tổng cộng</span>
              <span class="price">{{ money(finalCartTotal) }}</span>
            </div>
            <div v-if="discountAmount > 0" class="sum-saved">
              <i class="bi bi-piggy-bank-fill me-1"></i>Bạn đã tiết kiệm
              <strong>{{ money(discountAmount) }}</strong>
            </div>

            <component
              :is="hasOutOfStock ? 'button' : 'RouterLink'"
              v-bind="
                hasOutOfStock
                  ? { disabled: true, type: 'button' }
                  : { to: '/thanh-toan' }
              "
              class="btn btn-primary w-100 py-3 rounded-pill fw-bold checkout-btn"
            >
              Tiến hành đặt hàng <i class="bi bi-arrow-right ms-1"></i>
            </component>

            <!-- Cam kết -->
            <div class="perks">
              <div class="perk">
                <i class="bi bi-shield-lock-fill text-success"></i>Bảo mật thanh
                toán
              </div>
              <div class="perk">
                <i class="bi bi-box-seam-fill text-danger"></i>Kiểm tra khi nhận
              </div>
              <div class="perk">
                <i class="bi bi-arrow-repeat text-primary"></i>Đổi size 7 ngày
              </div>
            </div>
          </aside>
        </div>
      </div>

      <!-- Giỏ hàng trống -->
      <div v-else class="empty-cart">
        <div class="empty-ico">🛒</div>
        <h4 class="fw-bold">Giỏ hàng của bạn đang trống</h4>
        <p class="text-muted-ps mb-4 mx-auto" style="max-width: 420px">
          Hãy chọn một đôi giày bạn ưng ý nhất để tự tin sải bước cùng Poly
          Shoes nhé!
        </p>
        <RouterLink
          to="/cua-hang"
          class="btn btn-primary rounded-pill px-4 py-2 fw-bold"
        >
          <i class="bi bi-shop me-1"></i> Mua sắm ngay
        </RouterLink>
      </div>
    </template>
  </div>
</template>

<style scoped>
.min-w-0 {
  min-width: 0;
}

/* ===== Login required ===== */
.login-required {
  text-align: center;
  padding: 64px 24px;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 24px;
}
.login-ico {
  width: 110px;
  height: 110px;
  margin: 0 auto 18px;
  display: grid;
  place-items: center;
  font-size: 3rem;
  color: #dc2626;
  background: linear-gradient(145deg, #fdf2f2, #fbe9ee);
  border-radius: 50%;
}

/* ===== Header ===== */
.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.cart-header-ico {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 1.5rem;
  color: #fff;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 8px 18px rgba(220, 38, 38, 0.3);
}
.cart-title {
  font-weight: 800;
  letter-spacing: -0.01em;
  color: #111827;
}

/* ===== Notices ===== */
.notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 14px;
}
.notice-warn {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}
.notice-danger {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

/* ===== Cart item ===== */
.cart-item {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 18px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.05);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.cart-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.09);
}
.cart-item.is-out {
  background: #fafafa;
}
.cart-item.is-out .item-thumb img,
.cart-item.is-out .item-info {
  opacity: 0.55;
}

.item-thumb {
  position: relative;
  flex: 0 0 112px;
  width: 112px;
  height: 112px;
  border-radius: 16px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #fdf2f2, #fbe9ee);
}
.item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.item-thumb-fallback {
  font-size: 2.8rem;
}
.item-out-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #dc2626;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.item-brand {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #dc2626;
}
.item-name {
  font-size: 1.02rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 2px;
}
.item-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.78rem;
  font-weight: 600;
  color: #475569;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 999px;
}
.item-tag i {
  color: #94a3b8;
}
.item-tag.tag-low {
  color: #b45309;
  background: #fffbeb;
}
.item-tag.tag-low i {
  color: #f59e0b;
}
.item-unit {
  font-size: 0.85rem;
  color: #64748b;
  margin-top: 2px;
}
.item-unit strong {
  color: #1e293b;
}
.item-warn {
  margin-top: 4px;
  font-size: 0.8rem;
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fde68a;
  padding: 6px 10px;
  border-radius: 10px;
}

.item-side {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}
.item-line-total {
  font-size: 1.15rem;
  font-weight: 800;
  color: #dc2626;
  letter-spacing: -0.01em;
}

/* Qty pill */
.qty-pill {
  display: inline-flex;
  align-items: center;
  border: 1.5px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  overflow: hidden;
}
.qty-btn {
  width: 34px;
  height: 34px;
  border: 0;
  background: transparent;
  font-size: 1.1rem;
  color: #475569;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}
.qty-btn:hover:not(:disabled) {
  background: #fef2f2;
  color: #dc2626;
}
.qty-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.qty-val {
  min-width: 36px;
  text-align: center;
  font-weight: 700;
  color: #111827;
}

.item-remove {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #94a3b8;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.item-remove:hover:not(:disabled) {
  color: #dc2626;
  background: #fef2f2;
}
.item-remove:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.back-shop {
  display: inline-block;
  margin-top: 16px;
  font-weight: 600;
  font-size: 0.9rem;
  color: #475569;
  text-decoration: none;
  transition: color 0.15s;
}
.back-shop:hover {
  color: #dc2626;
}

/* ===== Summary ===== */
.summary-card {
  position: sticky;
  top: 90px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 20px;
  padding: 22px;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.07);
}
.summary-title {
  font-weight: 800;
  margin-bottom: 16px;
  color: #111827;
}

.voucher-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  margin-bottom: 12px;
}
.voucher-box.applied {
  background: linear-gradient(120deg, #f0fdf4, #dcfce7);
  border: 1px dashed #86efac;
}
.voucher-box.hint {
  background: #fff7ed;
  border: 1px dashed #fdba74;
  color: #9a3412;
}
.voucher-ico {
  flex: 0 0 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 1.1rem;
  color: #fff;
  background: #16a34a;
}
.voucher-box.hint .voucher-ico {
  background: #f97316;
}
.voucher-code {
  font-weight: 800;
  color: #15803d;
  font-size: 0.92rem;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.voucher-badge {
  font-size: 0.65rem;
  font-weight: 700;
  color: #fff;
  background: #16a34a;
  padding: 2px 8px;
  border-radius: 999px;
}
.voucher-name {
  font-size: 0.78rem;
  color: #166534;
  font-weight: 600;
}
.voucher-desc {
  font-size: 0.75rem;
  color: #4b5563;
}
.voucher-save {
  font-weight: 800;
  color: #15803d;
  white-space: nowrap;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 0.92rem;
  color: #475569;
}
.sum-row strong {
  color: #111827;
}
.sum-row.text-success strong {
  color: #16a34a;
}

.sum-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 2px dashed #e5e7eb;
  font-size: 1.1rem;
  font-weight: 800;
  color: #111827;
}
.sum-total .price {
  font-size: 1.55rem;
  color: #dc2626;
}
.sum-saved {
  margin-top: 8px;
  text-align: right;
  font-size: 0.82rem;
  color: #15803d;
}

.checkout-btn {
  margin-top: 18px;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.28);
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}
.checkout-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.36);
}
.checkout-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.perks {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.perk {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
  color: #64748b;
}
.perk i {
  font-size: 1rem;
}

/* ===== Empty ===== */
.empty-cart {
  text-align: center;
  padding: 64px 24px;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 24px;
}
.empty-ico {
  width: 110px;
  height: 110px;
  margin: 0 auto 18px;
  display: grid;
  place-items: center;
  font-size: 3.4rem;
  background: linear-gradient(145deg, #fdf2f2, #fbe9ee);
  border-radius: 50%;
}

/* ===== Skeleton ===== */
.skel-row {
  display: flex;
  gap: 16px;
  padding: 16px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 18px;
}
.sk {
  border-radius: 8px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 37%, #f1f5f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}
.sk-img {
  flex: 0 0 112px;
  height: 112px;
  border-radius: 16px;
}
.sk-line {
  height: 14px;
  margin-bottom: 12px;
}
.sk-btn {
  height: 48px;
  border-radius: 999px;
  margin-top: 18px;
}
.w-25 {
  width: 25%;
}
.w-50 {
  width: 50%;
}
.w-75 {
  width: 75%;
}
@keyframes shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ===== Responsive ===== */
@media (max-width: 767px) {
  .cart-item {
    flex-wrap: wrap;
    padding: 14px;
  }
  .item-thumb {
    flex-basis: 88px;
    width: 88px;
    height: 88px;
  }
  .item-info {
    flex-basis: calc(100% - 110px);
  }
  .item-side {
    flex-basis: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding-top: 12px;
    border-top: 1px dashed #e5e7eb;
  }
  .item-line-total {
    order: 3;
  }
  .summary-card {
    position: static;
  }
}
</style>
