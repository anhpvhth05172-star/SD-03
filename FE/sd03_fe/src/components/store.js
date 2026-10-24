import { reactive, computed, watch } from "vue";
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
    return JSON.parse(localStorage.getItem("ps_cart")) || [];
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

const mapServerItem = (item) => ({
  id: item.idSanPham,
  variantId: item.idSanPhamChiTiet,
  idGioHangChiTiet: item.id,
  name: item.tenSanPham,
  brand: item.maCtsp || "",
  color: item.mauSac || "",
  size: item.kichCo || "",
  price: Number(item.giaHienTai != null ? item.giaHienTai : item.donGia || 0),
  donGia: Number(item.donGia || 0),
  img: item.hinhAnh || null,
  qty: item.soLuong || 1,
  stock: item.tonKho != null ? item.tonKho : 999,
  conHang: item.conHang !== false,
  canhBao: item.canhBao || null,
});

// Lấy giỏ hàng từ BE khi đã đăng nhập
export async function fetchCart() {
  const user = getUser();
  if (!user || !user.idKhachHang) {
    cart.serverCart = null;
    return cart.items;
  }

  cart.loading = true;
  cart.error = null;
  try {
    const res = await layGioHangApi(user.idKhachHang);
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
  }
}

// Thêm sản phẩm vào giỏ hàng (kết nối BE)
export async function addToCart(p, variantId, size, qty = 1, color = "") {
  const user = getUser();
  if (!user || !user.idKhachHang) {
    return {
      success: false,
      needLogin: true,
      message: "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng",
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
    const msg = err.message || "Không thể thêm vào giỏ hàng";
    cart.error = msg;
    throw new Error(msg);
  } finally {
    cart.loading = false;
  }
}

// Cập nhật số lượng item trong giỏ
export async function updateCartItemQty(idGioHangChiTiet, qty) {
  const user = getUser();
  if (!user || !user.idKhachHang) {
    const item = cart.items.find((i) => i.idGioHangChiTiet === idGioHangChiTiet);
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
    throw new Error(msg);
  } finally {
    cart.loading = false;
  }
}

// Xóa 1 item khỏi giỏ
export async function removeItem(target) {
  const user = getUser();
  let item = null;
  let idToDelete = null;

  if (typeof target === "object" && target !== null) {
    item = target;
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
      cart.items = [];
      cart.serverCart = null;
    } catch (err) {
      cart.error = err.message || "Không thể làm trống giỏ hàng";
      throw err;
    } finally {
      cart.loading = false;
    }
    return;
  }
  cart.items = [];
  cart.serverCart = null;
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
