import { reactive, computed, watch } from "vue";
import http from "../api/http";
import { getUser } from "../utils/auth";
import {
  themVaoGioHangApi,
  layGioHangApi,
  capNhatSoLuongApi,
  xoaItemGioHangApi,
  xoaTatCaGioHangApi,
} from "../api/cart";

export const money = (n) => {
  if (n == null || isNaN(n)) return "—";
  return Number(n).toLocaleString("vi-VN") + "₫";
};

// ===== Cart State =====
const saved = (() => {
  try {
    const user = getUser();
    const raw = JSON.parse(localStorage.getItem("ps_cart")) || [];
    // Nếu chưa đăng nhập: bỏ qua các item thuộc về tài khoản trước đó (có idGioHangChiTiet)
    if (!user || !user.idKhachHang) {
      const guestOnly = raw.filter((i) => !i.idGioHangChiTiet);
      if (guestOnly.length !== raw.length) {
        localStorage.setItem("ps_cart", JSON.stringify(guestOnly));
      }
      return guestOnly;
    }
    return raw;
  } catch {
    return [];
  }
})();

export const cart = reactive({
  items: saved,
  serverCart: null,
  loading: false,
  error: null,
});

watch(
  () => cart.items,
  (v) => localStorage.setItem("ps_cart", JSON.stringify(v)),
  { deep: true },
);

export const cartCount = computed(() => {
  if (cart.serverCart && cart.serverCart.tongSoLuong != null) {
    return Number(cart.serverCart.tongSoLuong);
  }
  return cart.items.reduce((s, i) => s + (Number(i.qty) || 0), 0);
});

export const cartTotal = computed(() => {
  if (cart.serverCart && cart.serverCart.tongTien != null) {
    return Number(cart.serverCart.tongTien);
  }
  return cart.items.reduce(
    (s, i) => s + (Number(i.qty) || 0) * (Number(i.price) || 0),
    0,
  );
});

// [SỬA] Xóa giỏ hàng ở phía client (dùng khi đăng xuất).
// KHÔNG gọi API nên giỏ hàng trên server vẫn còn cho lần đăng nhập sau.
export function resetCartLocal() {
  cart.items.splice(0, cart.items.length);
  cart.serverCart = null;
  cart.loading = false;
  cart.error = null;
  localStorage.removeItem("ps_cart");
  localStorage.setItem("ps_cart", "[]");
}

if (typeof window !== "undefined") {
  window.addEventListener("auth:logout", () => {
    resetCartLocal();
  });
}

const mapServerItem = (item) => ({
  id: item.idSanPham,
  variantId: item.idSanPhamChiTiet,
  idSanPhamChiTiet: item.idSanPhamChiTiet,
  idGioHangChiTiet: item.id,
  name: item.tenSanPham,
  brand: item.maCtsp || "",
  color: item.mauSac || "",
  size: item.kichCo || "",
  price: Number(item.giaHienTai != null && item.giaHienTai > 0 ? item.giaHienTai : (item.donGia || 0)),
  giaHienTai: Number(item.giaHienTai || 0),
  donGia: Number(item.donGia || 0),
  img: item.hinhAnh || null,
  qty: item.soLuong || 1,
  stock: item.tonKho != null ? item.tonKho : 999,
  conHang: item.conHang !== false,
  canhBao: item.canhBao || null,
});

let activeFetchPromise = null;

// Làm mới giá sản phẩm hiện tại cho giỏ hàng khách vãng lai (chỉ gọi ở màn Giỏ hàng)
export async function refreshGuestCartPrices() {
  const user = getUser();
  if (user && user.idKhachHang) return;
  const variantIds = [...new Set(
    cart.items
      .map((i) => Number(i.idSanPhamChiTiet || i.variantId))
      .filter((id) => Number.isFinite(id) && id > 0)
  )];
  if (variantIds.length === 0) return;
  await Promise.allSettled(
    variantIds.map(async (variantId) => {
      const res = await http.get(`/v1/san-pham-chi-tiet/${variantId}`);
      const data = res?.data;
      const livePrice = Number(data?.giaSauGiam ?? data?.price ?? 0);
      if (!(livePrice > 0)) return;
      cart.items.forEach((item) => {
        const itemVid = Number(item.idSanPhamChiTiet || item.variantId);
        if (itemVid === variantId) {
          item.price = livePrice;
          item.giaHienTai = livePrice;
        }
      });
    })
  );
  localStorage.setItem("ps_cart", JSON.stringify(cart.items));
}

// Lấy giỏ hàng từ BE khi đã đăng nhập
export async function fetchCart({ silent = false } = {}) {
  const user = getUser();
  if (!user || !user.idKhachHang) {
    // Giỏ của khách vãng lai: đảm bảo không giữ lại các item thuộc tài khoản cũ (có idGioHangChiTiet)
    cart.serverCart = null;
    const hasServerItem = Array.isArray(cart.items) && cart.items.some((i) => i.idGioHangChiTiet);
    if (hasServerItem) {
      const guestOnly = cart.items.filter((i) => !i.idGioHangChiTiet);
      cart.items.splice(0, cart.items.length, ...guestOnly);
      localStorage.setItem("ps_cart", JSON.stringify(guestOnly));
    }
    return cart.items;
  }

  if (activeFetchPromise) {
    return activeFetchPromise;
  }

  activeFetchPromise = (async () => {
    if (!silent) {
      cart.loading = true;
    }
    cart.error = null;
    try {
      // Thu thập các món khách nhặt khi chưa đăng nhập và dọn ngay khỏi local trước khi gọi API để tránh gộp trùng
      const localSaved = (() => {
        try {
          return JSON.parse(localStorage.getItem("ps_cart")) || [];
        } catch {
          return [];
        }
      })();
      const sourceList = Array.isArray(cart.items) && cart.items.length > 0 ? cart.items : localSaved;
      const guestItems = sourceList.filter(
        (i) => !i.idGioHangChiTiet && (i.idSanPhamChiTiet || i.variantId)
      );

      if (guestItems.length > 0) {
        const remaining = (Array.isArray(cart.items) ? cart.items : []).filter((i) => i.idGioHangChiTiet);
        cart.items.splice(0, cart.items.length, ...remaining);
        localStorage.setItem("ps_cart", JSON.stringify(remaining));
      }

      let res;
      if (guestItems.length > 0) {
        try {
          await xoaTatCaGioHangApi(user.idKhachHang);
        } catch (clearErr) {
          console.warn("Không thể làm sạch giỏ server trước khi đồng bộ:", clearErr);
        }

        for (const gItem of guestItems) {
          const vId = Number(gItem.idSanPhamChiTiet || gItem.variantId);
          if (!vId) continue;
          try {
            await themVaoGioHangApi({
              idKhachHang: user.idKhachHang,
              idSanPhamChiTiet: vId,
              soLuong: Number(gItem.qty || 1),
            });
          } catch (mergeErr) {
            console.warn("Không thể đồng bộ item khách vãng lai vào giỏ server:", gItem, mergeErr);
          }
        }
      }
      res = await layGioHangApi(user.idKhachHang);

      cart.serverCart = res;
      if (res && Array.isArray(res.items)) {
        cart.items = res.items.map(mapServerItem);
      } else {
        cart.items = [];
      }
      return cart.items;
    } catch (err) {
      cart.error = err.message || "Không thể tải giỏ hàng";
      return cart.items;
    } finally {
      cart.loading = false;
      activeFetchPromise = null;
    }
  })();

  return activeFetchPromise;
}

// Đồng bộ giỏ hàng từ máy khách lên tài khoản sau khi đăng nhập để không mất sản phẩm
export async function dongBoGioHangSauDangNhap() {
  const user = getUser();
  if (!user || !user.idKhachHang) return;
  await fetchCart();
}

// Thêm sản phẩm vào giỏ hàng (kết nối BE)
export async function addToCart(p, variantId, size, qty = 1, color = "") {
  const user = getUser();
  const effectivePrice = Number(p.giaBan ?? p.giaBanMin ?? p.price ?? 0);
  if (!user || !user.idKhachHang) {
    const existing = cart.items.find(
      (item) => Number(item.idSanPhamChiTiet ?? item.variantId) === Number(variantId) && Number(item.price) === effectivePrice
    );
    const available = Number(p.stock ?? p.tonKho ?? 999);
    if (existing) {
      existing.qty = Math.min(Number(existing.qty || 0) + Number(qty || 1), available);
    } else {
      cart.items.push({
        id: p.id,
        variantId,
        idSanPhamChiTiet: variantId,
        name: p.tenSanPham || p.name || "Sản phẩm",
        brand: p.tenThuongHieu || p.brand || "",
        color,
        size,
        price: effectivePrice,
        donGia: effectivePrice,
        img: p.img || null,
        qty: Math.min(Number(qty || 1), available),
        stock: available,
        conHang: available > 0,
      });
    }
    return {
      success: true,
      local: true,
      message: "Đã thêm sản phẩm vào giỏ hàng",
    };
  }

  cart.loading = true;
  cart.error = null;
  try {
    const res = await themVaoGioHangApi({
      idKhachHang: user.idKhachHang,
      idSanPhamChiTiet: variantId,
      soLuong: qty,
    });
    // Đồng bộ lại giỏ hàng sau khi thêm
    await fetchCart();
    return {
      success: true,
      data: res,
      message: res?.message || "Đã thêm sản phẩm vào giỏ hàng",
    };
  } catch (err) {
    // Fallback lưu giỏ hàng local nếu phiên đăng nhập hết hạn hoặc lỗi kết nối
    const existing = cart.items.find(
      (item) => Number(item.idSanPhamChiTiet ?? item.variantId) === Number(variantId) && Number(item.price) === effectivePrice
    );
    const available = Number(p.stock ?? p.tonKho ?? 999);
    if (existing) {
      existing.qty = Math.min(Number(existing.qty || 0) + Number(qty || 1), available);
    } else {
      cart.items.push({
        id: p.id,
        variantId,
        idSanPhamChiTiet: variantId,
        name: p.tenSanPham || p.name || "Sản phẩm",
        brand: p.tenThuongHieu || p.brand || "",
        color,
        size,
        price: effectivePrice,
        donGia: effectivePrice,
        img: p.img || null,
        qty: Math.min(Number(qty || 1), available),
        stock: available,
        conHang: available > 0,
      });
    }
    return {
      success: true,
      local: true,
      message: "Đã thêm sản phẩm vào giỏ hàng",
    };
  } finally {
    cart.loading = false;
  }
}

// Cập nhật số lượng item trong giỏ
export async function updateCartItemQty(idGioHangChiTiet, qty) {
  const user = getUser();
  if (!user || !user.idKhachHang) {
    const item = cart.items.find(
      (i) => i.idGioHangChiTiet === idGioHangChiTiet,
    );
    if (item) item.qty = qty;
    return;
  }

  cart.loading = true;
  cart.error = null;
  try {
    await capNhatSoLuongApi(idGioHangChiTiet, {
      idKhachHang: user.idKhachHang,
      soLuong: qty,
    });
    await fetchCart();
  } catch (err) {
    const msg = err.message || "Không thể cập nhật số lượng";
    cart.error = msg;
    throw new Error(msg, { cause: err });
  } finally {
    cart.loading = false;
  }
}

// Xóa 1 item khỏi giỏ
export async function removeItem(target) {
  const user = getUser();
  let item;
  let idToDelete = null;

  if (typeof target === "object" && target !== null) {
    idToDelete = target.idGioHangChiTiet;
  } else if (typeof target === "number") {
    // Có thể là idGioHangChiTiet hoặc index
    item = cart.items.find((i) => i.idGioHangChiTiet === target);
    if (item) {
      idToDelete = target;
    } else if (target >= 0 && target < cart.items.length) {
      item = cart.items[target];
      idToDelete = item?.idGioHangChiTiet;
    }
  }

  if (user && user.idKhachHang && idToDelete) {
    cart.loading = true;
    try {
      await xoaItemGioHangApi(idToDelete, user.idKhachHang);
      await fetchCart();
      return;
    } catch (err) {
      cart.error = err.message || "Không thể xóa sản phẩm khỏi giỏ";
      throw err;
    } finally {
      cart.loading = false;
    }
  }

  // Fallback offline / local
  if (typeof target === "number" && target >= 0 && target < cart.items.length) {
    cart.items.splice(target, 1);
  } else if (idToDelete) {
    const idx = cart.items.findIndex((i) => i.idGioHangChiTiet === idToDelete);
    if (idx !== -1) cart.items.splice(idx, 1);
  }
}

// Xóa tất cả trong giỏ
export async function clearCart() {
  const user = getUser();
  if (user && user.idKhachHang) {
    cart.loading = true;
    try {
      await xoaTatCaGioHangApi(user.idKhachHang);
    } catch (err) {
      console.warn("Không thể xóa giỏ hàng server:", err);
    } finally {
      cart.loading = false;
      cart.items = [];
      cart.serverCart = null;
      localStorage.removeItem("ps_cart");
      localStorage.setItem("ps_cart", "[]");
    }
    return;
  }
  cart.items = [];
  cart.serverCart = null;
  localStorage.removeItem("ps_cart");
  localStorage.setItem("ps_cart", "[]");
}

// ===== Orders =====
const readOrders = () => {
  try {
    return JSON.parse(localStorage.getItem("ps_orders")) || [];
  } catch {
    return [];
  }
};

export function placeOrder(info) {
  const id = "HD" + Date.now().toString().slice(-6);
  const order = {
    id,
    info,
    items: JSON.parse(JSON.stringify(cart.items)),
    total: cartTotal.value,
    date: new Date().toLocaleString("vi-VN"),
  };
  localStorage.setItem("ps_orders", JSON.stringify([...readOrders(), order]));
  clearCart();
  return id;
}

export const getOrder = (id) => readOrders().find((o) => o.id === id);

// ===== Legacy static data =====
export const products = [];
export const brands = [];
export const types = [];
