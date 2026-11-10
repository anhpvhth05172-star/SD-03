const COOKIE_NAME = "ps_guest_cart_exp";
const LS_KEY = "ps_guest_cart_exp"; // dấu vết để biết cookie đã từng tồn tại (nay đã hết hạn)
export const GUEST_CART_TTL_MS = 30 * 60 * 1000;

function readCookie() {
  const row = document.cookie
    .split("; ")
    .find((r) => r.startsWith(COOKIE_NAME + "="));
  if (!row) return null;
  const n = Number(decodeURIComponent(row.split("=")[1]));
  return Number.isFinite(n) ? n : null;
}

// Gọi mỗi khi khách vãng lai thêm sản phẩm vào giỏ -> gia hạn thêm 30 phút
export function startGuestCartTimer() {
  const exp = Date.now() + GUEST_CART_TTL_MS;
  document.cookie = `${COOKIE_NAME}=${exp}; max-age=${GUEST_CART_TTL_MS / 1000}; path=/; SameSite=Lax`;
  try {
    localStorage.setItem(LS_KEY, String(exp));
  } catch {
    /* bỏ qua */
  }
  return exp;
}

// Gọi khi giỏ trống, khi đặt hàng thành công hoặc khi khách đăng nhập
export function clearGuestCartTimer() {
  document.cookie = `${COOKIE_NAME}=; max-age=0; path=/; SameSite=Lax`;
  try {
    localStorage.removeItem(LS_KEY);
  } catch {
    /* bỏ qua */
  }
}

// null  : chưa có bộ đếm
// <= 0  : đã hết hạn (cookie đã bị trình duyệt xóa hoặc quá giờ)
// > 0   : số mili giây còn lại
export function getGuestCartRemaining() {
  const c = readCookie();
  if (c != null) return c - Date.now();
  let marker = null;
  try {
    marker = localStorage.getItem(LS_KEY);
  } catch {
    /* bỏ qua */
  }
  return marker != null ? 0 : null;
}
