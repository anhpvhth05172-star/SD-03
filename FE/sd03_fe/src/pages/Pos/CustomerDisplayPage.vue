<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const currentOrderData = ref({
  orderCode: '',
  orderTitle: 'Hóa đơn 1',
  customer: null,
  cart: [],
  subTotal: 0,
  discountAmount: 0,
  shippingFee: 0,
  finalTotal: 0,
  cashGiven: 0,
  transferGiven: 0,
  changeAmount: 0,
  paymentMethod: 'TIEN_MAT',
  voucherCode: null,
  isDelivery: false,
  updatedAt: Date.now()
})

const currentTime = ref('')
let clockTimer = null
let pollTimer = null
let broadcastChannel = null
const isFullscreen = ref(false)

const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(val) || 0)
}

const updateClock = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) +
    ' - ' + now.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })
}

const loadOrderFromStorage = () => {
  try {
    const rawPayload = localStorage.getItem('polyshoes_customer_display_payload')
    if (rawPayload) {
      const parsed = JSON.parse(rawPayload)
      if (parsed && typeof parsed === 'object') {
        currentOrderData.value = parsed
        return
      }
    }

    // Fallback: Đọc từ POS pending orders nếu chưa có payload riêng
    const rawOrders = localStorage.getItem('polyshoes_pos_pending_orders_v1')
    const activeIdx = Number(localStorage.getItem('polyshoes_pos_active_idx_v1') || 0)
    if (rawOrders) {
      const orders = JSON.parse(rawOrders)
      if (Array.isArray(orders) && orders.length > 0) {
        const ord = orders[activeIdx] || orders[0]
        const cart = Array.isArray(ord.cart) ? ord.cart : []
        const sub = cart.reduce((sum, i) => sum + (Number(i.price || 0) * Number(i.quantity || 0)), 0)
        const ship = ord.isDelivery ? Number(ord.deliveryInfo?.shippingFee || 0) : 0
        const disc = ord.selectedVoucher ? Number(ord.selectedVoucher.giaTriGiam || 0) : 0
        const total = Math.max(0, sub + ship - disc)

        currentOrderData.value = {
          orderCode: ord.code || '',
          orderTitle: ord.title || 'Hóa đơn bán hàng',
          customer: ord.customer ? {
            tenKhachHang: ord.customer.tenKhachHang || ord.customer.ten,
            soDienThoai: ord.customer.soDienThoai || ord.customer.sdt
          } : null,
          cart: cart.map(item => ({
            id: item.id || item.idCtsp,
            name: item.name || item.tenSanPham,
            maCtsp: item.maCtsp || item.maChiTietSanPham || item.maSp,
            color: item.color || item.tenMau,
            size: item.size || item.tenKichCo,
            price: Number(item.price || 0),
            quantity: Number(item.quantity || 0),
            total: Number(item.price || 0) * Number(item.quantity || 0),
            image: item.image || item.hinhAnh || null
          })),
          subTotal: sub,
          discountAmount: disc,
          shippingFee: ship,
          finalTotal: total,
          cashGiven: Number(ord.cashGiven || 0),
          transferGiven: Number(ord.transferGiven || 0),
          changeAmount: Math.max(0, Number(ord.cashGiven || 0) - total),
          paymentMethod: ord.paymentMethod || 'TIEN_MAT',
          voucherCode: ord.selectedVoucher?.maPhieuGiamGia || null,
          isDelivery: Boolean(ord.isDelivery),
          updatedAt: Date.now()
        }
      }
    }
  } catch (e) {
    console.error('Error loading customer display state:', e)
  }
}

const activePosPopup = ref(null)

const loadPopupFromStorage = () => {
  try {
    const raw = localStorage.getItem('polyshoes_pos_popup_state')
    if (raw) {
      activePosPopup.value = JSON.parse(raw)
    } else {
      activePosPopup.value = null
    }
  } catch {
    activePosPopup.value = null
  }
}

const deliveryInfo = computed(() => {
  const d = currentOrderData.value?.deliveryInfo
  const cust = currentOrderData.value?.customer
  if (!currentOrderData.value?.isDelivery) return null
  return {
    recipientName: d?.recipientName || cust?.tenKhachHang || 'Khách nhận tại nhà',
    recipientPhone: d?.recipientPhone || cust?.soDienThoai || '',
    address: d?.fullAddress || d?.recipientAddress || '',
    shippingFee: Number(d?.shippingFee || currentOrderData.value?.shippingFee || 0)
  }
})

const showQrCenterModal = computed(() => {
  return Boolean(currentOrderData.value?.isBankQrModalOpen) && Number(currentOrderData.value?.finalTotal || 0) > 0
})

const qrCodeUrl = computed(() => {
  const total = Number(currentOrderData.value?.finalTotal || 0)
  if (total <= 0) return null
  const code = currentOrderData.value?.orderCode || 'POLYSHOES'
  // VietQR chuẩn Napas tự động sinh QR theo số tiền và mã hóa đơn
  return `https://img.vietqr.io/image/MB-0987654321-compact2.png?amount=${total}&addInfo=${encodeURIComponent('Thanh toan ' + code)}&accountName=POLYSHOES%20SNEAKER`
})

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  loadOrderFromStorage()
  loadPopupFromStorage()

  // Đồng bộ qua BroadcastChannel tức thì
  try {
    broadcastChannel = new BroadcastChannel('polyshoes_pos_display_channel')
    broadcastChannel.onmessage = (event) => {
      if (event.data && typeof event.data === 'object') {
        if (event.data.type === 'POS_POPUP_STATE') {
          activePosPopup.value = event.data.popup
        } else {
          currentOrderData.value = event.data
        }
      }
    }
  } catch (e) {}

  // Lắng nghe thay đổi storage từ tab POS
  window.addEventListener('storage', (e) => {
    if (e.key === 'polyshoes_pos_popup_state') {
      loadPopupFromStorage()
    }
    if (e.key === 'polyshoes_customer_display_payload' || e.key === 'polyshoes_pos_pending_orders_v1' || e.key === 'polyshoes_pos_active_idx_v1') {
      loadOrderFromStorage()
    }
  })

  // Polling nhẹ mỗi 1s để đảm bảo đồng bộ 100%
  pollTimer = setInterval(() => {
    loadOrderFromStorage()
    loadPopupFromStorage()
  }, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (pollTimer) clearInterval(pollTimer)
  if (broadcastChannel) broadcastChannel.close()
})
</script>

<template>
  <div class="customer-display-app">
    <!-- Top Bar Thương Hiệu -->
    <header class="display-header">
      <div class="header-brand">
        <div class="brand-logo-icon">👟</div>
        <div class="brand-text">
          <h1 class="brand-title">POLYSHOES</h1>
          <span class="brand-sub">HỆ THỐNG GIÀY SNEAKER CHÍNH HÃNG</span>
        </div>
      </div>

      <div class="header-center">
        <div class="welcome-badge">
          <i class="bi bi-stars"></i>
          <span>KÍNH CHÀO QUÝ KHÁCH</span>
        </div>
      </div>

      <div class="header-right">
        <div class="clock-display">
          <i class="bi bi-clock me-1"></i>
          <span>{{ currentTime }}</span>
        </div>
      </div>
    </header>

    <!-- Info Bar Hóa Đơn & Khách Hàng -->
    <div class="order-info-banner">
      <div class="info-tag order-tag">
        <span class="tag-label">MÃ ĐƠN HÀNG:</span>
        <strong class="tag-val">{{ currentOrderData.orderCode || 'Đang tạo đơn' }}</strong>
        <span class="tag-badge" v-if="currentOrderData.orderTitle">({{ currentOrderData.orderTitle }})</span>
      </div>

      <div class="info-tag customer-tag">
        <span class="tag-label">KHÁCH HÀNG:</span>
        <strong class="tag-val" v-if="currentOrderData.customer">
          {{ currentOrderData.customer.tenKhachHang }}
          <span class="cust-phone" v-if="currentOrderData.customer.soDienThoai">({{ currentOrderData.customer.soDienThoai }})</span>
        </strong>
        <span class="tag-val text-muted" v-else>Khách mua lẻ tại quầy</span>
      </div>

      <div class="info-tag mode-tag" v-if="currentOrderData.isDelivery">
        <i class="bi bi-truck me-1"></i>
        <span>ĐƠN GIAO HÀNG TẬN NƠI</span>
        <span v-if="deliveryInfo?.address" class="delivery-address-pill ms-2">
          ({{ deliveryInfo.recipientName }} - {{ deliveryInfo.address }})
        </span>
      </div>
    </div>

    <!-- Main Content Grid -->
    <main class="display-main">
      <!-- Cột Trái: Bảng Sản Phẩm Trong Đơn Hàng -->
      <section class="display-col-cart">
        <div class="panel-card cart-panel">
          <div class="panel-header">
            <div class="panel-title">
              <i class="bi bi-bag-check-fill text-danger me-2"></i>
              <span>DANH SÁCH SẢN PHẨM</span>
            </div>
            <span class="cart-items-count">
              {{ currentOrderData.cart?.length || 0 }} sản phẩm
            </span>
          </div>

          <div class="panel-body cart-body">
            <!-- Nếu giỏ trống -->
            <div v-if="!currentOrderData.cart || currentOrderData.cart.length === 0" class="cart-empty-state">
              <div class="empty-icon-wrap">
                <i class="bi bi-cart3"></i>
              </div>
              <h3 class="empty-title">Chào mừng Quý khách đến với PolyShoes!</h3>
              <p class="empty-desc">
                Quý khách vui lòng chọn giày sneaker tại quầy để nhân viên phục vụ và tính tiền ngay trên màn hình này.
              </p>
              <div class="empty-features">
                <div class="feat-pill"><i class="bi bi-patch-check"></i> 100% Chính hãng</div>
                <div class="feat-pill"><i class="bi bi-arrow-repeat"></i> Đổi trả dễ dàng</div>
                <div class="feat-pill"><i class="bi bi-shield-check"></i> Bảo hành chu đáo</div>
              </div>
            </div>

            <!-- Bảng danh sách sản phẩm (Không có nút bấm) -->
            <div v-else class="cart-table-wrapper">
              <table class="customer-cart-table">
                <thead>
                  <tr>
                    <th width="48" class="text-center">STT</th>
                    <th>SẢN PHẨM</th>
                    <th width="140" class="text-center">MÀU / SIZE</th>
                    <th width="130" class="text-end">ĐƠN GIÁ</th>
                    <th width="100" class="text-center">SỐ LƯỢNG</th>
                    <th width="150" class="text-end">THÀNH TIỀN</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in currentOrderData.cart" :key="idx" class="cart-row">
                    <td class="text-center row-stt">{{ idx + 1 }}</td>
                    <td class="row-product">
                      <div class="prod-thumb">
                        <img v-if="item.image" :src="item.image" :alt="item.name" />
                        <span v-else class="thumb-fallback">👟</span>
                      </div>
                      <div class="prod-details">
                        <span class="prod-name">{{ item.name }}</span>
                        <span class="prod-sku" v-if="item.maCtsp">{{ item.maCtsp }}</span>
                      </div>
                    </td>
                    <td class="text-center row-variant">
                      <span class="variant-badge">
                        {{ item.color || '-' }} / Size {{ item.size || '-' }}
                      </span>
                    </td>
                    <td class="text-end row-price">{{ formatCurrency(item.price) }}</td>
                    <td class="text-center row-qty">
                      <span class="qty-pill">x {{ item.quantity }}</span>
                    </td>
                    <td class="text-end row-total">{{ formatCurrency(item.total) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <!-- Cột Phải: Tóm Tắt Thanh Toán & Mã QR -->
      <section class="display-col-summary">
        <div class="panel-card summary-panel">
          <div class="panel-header">
            <div class="panel-title">
              <i class="bi bi-receipt-cutoff text-success me-2"></i>
              <span>CHI TIẾT THANH TOÁN</span>
            </div>
          </div>

          <div class="panel-body summary-body">
            <!-- Thông tin giao hàng khi thu ngân chọn Giao Hàng -->
            <div class="delivery-info-box" v-if="currentOrderData.isDelivery && deliveryInfo">
              <div class="del-box-header">
                <i class="bi bi-geo-alt-fill text-danger me-1"></i>
                <span class="del-box-title">ĐỊA CHỈ NHẬN HÀNG</span>
              </div>
              <div class="del-box-body">
                <div class="del-line">
                  <span class="del-lbl">Người nhận:</span>
                  <strong class="del-val">{{ deliveryInfo.recipientName }}</strong>
                </div>
                <div class="del-line" v-if="deliveryInfo.recipientPhone">
                  <span class="del-lbl">Số điện thoại:</span>
                  <span class="del-val font-monospace">{{ deliveryInfo.recipientPhone }}</span>
                </div>
                <div class="del-line" v-if="deliveryInfo.address">
                  <span class="del-lbl">Địa chỉ:</span>
                  <span class="del-val text-break">{{ deliveryInfo.address }}</span>
                </div>
              </div>
            </div>

            <div class="summary-line">
              <span class="lbl">Tạm tính tiền hàng:</span>
              <span class="val">{{ formatCurrency(currentOrderData.subTotal) }}</span>
            </div>

            <div class="summary-line text-danger" v-if="currentOrderData.discountAmount > 0">
              <span class="lbl">
                <i class="bi bi-tag-fill me-1"></i>
                Giảm giá khuyến mãi:
                <small v-if="currentOrderData.voucherCode">({{ currentOrderData.voucherCode }})</small>
              </span>
              <span class="val">-{{ formatCurrency(currentOrderData.discountAmount) }}</span>
            </div>

            <div class="summary-line" v-if="currentOrderData.isDelivery && currentOrderData.shippingFee > 0">
              <span class="lbl">Phí giao hàng:</span>
              <span class="val">{{ formatCurrency(currentOrderData.shippingFee) }}</span>
            </div>

            <!-- Tổng Tiền Nổi Bật -->
            <div class="summary-total-box">
              <span class="total-lbl">TỔNG TIỀN PHẢI TRẢ</span>
              <span class="total-amount">{{ formatCurrency(currentOrderData.finalTotal) }}</span>
            </div>

            <!-- Lời Cảm Ơn Cuối Màn Hình -->
            <div class="thank-you-box">
              <div class="thank-heart">❤️</div>
              <p class="thank-title">Cảm ơn Quý khách đã mua sắm tại PolyShoes!</p>
              <p class="thank-hotline">Hotline hỗ trợ & Đổi trả: <strong>1900 1234</strong></p>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- Popup Mã QR Thanh Toán Ở Giữa Màn Hình Khi Chuyển Sang Chuyển Khoản -->
    <transition name="qr-popup-fade">
      <div v-if="showQrCenterModal" class="qr-center-popup-overlay">
        <div class="qr-center-popup-card">
          <div class="qr-popup-header">
            <div class="qr-popup-badge">
              <i class="bi bi-qr-code-scan"></i>
              <span>QUÉT MÃ THANH TOÁN VIETQR</span>
            </div>
            <p class="qr-popup-sub">
              Quý khách vui lòng mở ứng dụng Ngân hàng hoặc Ví điện tử để quét mã
            </p>
          </div>

          <div class="qr-popup-body">
            <div class="qr-popup-img-wrapper" v-if="qrCodeUrl">
              <img :src="qrCodeUrl" alt="VietQR PolyShoes" class="qr-popup-img" />
            </div>

            <div class="qr-popup-total-box">
              <span class="qr-total-label">SỐ TIỀN CẦN THANH TOÁN</span>
              <strong class="qr-total-val">{{ formatCurrency(currentOrderData.finalTotal) }}</strong>
            </div>

            <div class="qr-popup-bank-info">
              <div class="bank-row">
                <span class="bank-lbl">Ngân hàng:</span>
                <span class="bank-val">MB Bank (Quân Đội)</span>
              </div>
              <div class="bank-row">
                <span class="bank-lbl">Chủ tài khoản:</span>
                <span class="bank-val">POLYSHOES SNEAKER</span>
              </div>
              <div class="bank-row">
                <span class="bank-lbl">Nội dung CK:</span>
                <span class="bank-val highlight">{{ 'Thanh toan ' + (currentOrderData.orderCode || 'POLYSHOES') }}</span>
              </div>
            </div>
          </div>

          <div class="qr-popup-footer">
            <i class="bi bi-shield-check text-success me-1"></i>
            <span>Hệ thống tự động xác nhận ngay khi nhận được thanh toán</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- Popup Đồng Bộ Từ Bán Hàng Tại Quầy (Hoàn Toàn Không Có Nút Bấm) -->
    <transition name="qr-popup-fade">
      <div v-if="activePosPopup" class="pos-sync-popup-overlay">
        <div class="pos-sync-popup-card">
          <div class="pos-popup-icon-wrap" :class="'icon-' + (activePosPopup.iconType || 'info')">
            <i v-if="activePosPopup.iconType === 'success'" class="bi bi-check-circle-fill"></i>
            <i v-else-if="activePosPopup.iconType === 'error'" class="bi bi-x-circle-fill"></i>
            <i v-else-if="activePosPopup.iconType === 'warning'" class="bi bi-exclamation-triangle-fill"></i>
            <i v-else class="bi bi-info-circle-fill"></i>
          </div>
          <h3 class="pos-popup-title">{{ activePosPopup.title }}</h3>
          <div class="pos-popup-desc" v-html="activePosPopup.text"></div>
          <div class="pos-popup-sub-hint">
            <i class="bi bi-display me-1"></i>
            Màn hình thông tin khách hàng • Đang đồng bộ cùng quầy thu ngân
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.customer-display-app {
  min-height: 100vh;
  background: #f1f5f9;
  color: #0f172a;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  display: flex;
  flex-direction: column;
}

/* Header */
.display-header {
  height: 64px;
  background: #0f172a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 2px solid #dc2626;
  flex-shrink: 0;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo-icon {
  font-size: 26px;
  width: 42px;
  height: 42px;
  background: #1e293b;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #334155;
}

.brand-title {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1.5px;
  margin: 0;
  line-height: 1.2;
  color: #ef4444;
}

.brand-sub {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.8px;
  color: #94a3b8;
}

.welcome-badge {
  background: rgba(220, 38, 38, 0.2);
  border: 1px solid #ef4444;
  color: #fca5a5;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.clock-display {
  font-size: 13px;
  color: #cbd5e1;
  font-weight: 500;
  background: #1e293b;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #334155;
}

.btn-fullscreen {
  background: #1e293b;
  border: 1px solid #475569;
  color: #cbd5e1;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-fullscreen:hover {
  background: #dc2626;
  color: #ffffff;
  border-color: #dc2626;
}

/* Order Info Banner */
.order-info-banner {
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 24px;
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.info-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.tag-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.tag-val {
  color: #0f172a;
}

.tag-badge {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.cust-phone {
  color: #dc2626;
  font-weight: 600;
}

.mode-tag {
  margin-left: auto;
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
  padding: 4px 12px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 12px;
}

/* Main Grid */
.display-main {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 16px;
  padding: 16px 24px;
  overflow: hidden;
}

.panel-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.panel-header {
  padding: 14px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
}

.cart-items-count {
  font-size: 13px;
  font-weight: 600;
  background: #e0e7ff;
  color: #3730a3;
  padding: 2px 10px;
  border-radius: 12px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

/* Empty State */
.cart-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100%;
  padding: 40px 20px;
}

.empty-icon-wrap {
  width: 80px;
  height: 80px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 22px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 10px;
}

.empty-desc {
  font-size: 15px;
  color: #64748b;
  max-width: 480px;
  line-height: 1.6;
  margin-bottom: 24px;
}

.empty-features {
  display: flex;
  gap: 12px;
}

.feat-pill {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Cart Table */
.cart-table-wrapper {
  overflow-x: auto;
}

.customer-cart-table {
  width: 100%;
  border-collapse: collapse;
}

.customer-cart-table th {
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  padding: 10px 12px;
  border-bottom: 2px solid #e2e8f0;
}

.customer-cart-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
  vertical-align: middle;
}

.row-stt {
  color: #94a3b8;
  font-weight: 600;
}

.row-product {
  display: flex;
  align-items: center;
  gap: 12px;
}

.prod-thumb {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.prod-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-fallback {
  font-size: 22px;
}

.prod-details {
  display: flex;
  flex-direction: column;
}

.prod-name {
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.prod-sku {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.variant-badge {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  display: inline-block;
}

.row-price {
  font-weight: 600;
  color: #475569;
}

.qty-pill {
  background: #fee2e2;
  color: #991b1b;
  font-size: 14px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-block;
}

.row-total {
  font-weight: 800;
  color: #0f172a;
  font-size: 15px;
}

/* Summary Panel */
.summary-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.summary-line .lbl {
  color: #64748b;
  font-weight: 500;
}

.summary-line .val {
  font-weight: 700;
  color: #1e293b;
}

.summary-total-box {
  background: #fef2f2;
  border: 2px solid #dc2626;
  border-radius: 12px;
  padding: 16px;
  margin: 8px 0;
  text-align: center;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.1);
}

.total-lbl {
  display: block;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #991b1b;
  margin-bottom: 6px;
}

.total-amount {
  display: block;
  font-size: 32px;
  font-weight: 900;
  color: #dc2626;
  line-height: 1.1;
}

.cash-details {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px dashed #cbd5e1;
}

/* QR Popup Modal Ở Giữa Màn Hình */
.qr-center-popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(6px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.qr-center-popup-card {
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 25px 60px -10px rgba(0, 0, 0, 0.35);
  max-width: 580px;
  width: 95%;
  padding: 30px 28px 24px;
  text-align: center;
  border: 2px solid #dc2626;
  animation: qrCardPop 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes qrCardPop {
  0% {
    transform: scale(0.92);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.qr-popup-header {
  margin-bottom: 14px;
}

.qr-popup-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fef2f2;
  color: #dc2626;
  padding: 10px 22px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 15px;
  letter-spacing: 0.5px;
  border: 1px solid #fecaca;
}

.qr-popup-sub {
  color: #64748b;
  font-size: 14px;
  margin: 8px 0 0;
}

.qr-popup-img-wrapper {
  margin: 16px 0;
  display: flex;
  justify-content: center;
}

.qr-popup-img {
  width: 390px;
  height: 390px;
  border-radius: 18px;
  border: 3px solid #fecaca;
  box-shadow: 0 12px 30px rgba(220, 38, 38, 0.15);
  background: #ffffff;
  padding: 10px;
}

.qr-popup-total-box {
  background: #fff8f8;
  border: 1.5px dashed #dc2626;
  border-radius: 14px;
  padding: 12px 20px;
  margin-bottom: 16px;
}

.qr-total-label {
  display: block;
  font-size: 12px;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.8px;
  margin-bottom: 2px;
}

.qr-total-val {
  display: block;
  font-size: 30px;
  font-weight: 900;
  color: #dc2626;
}

.qr-popup-bank-info {
  background: #f1f5f9;
  border-radius: 12px;
  padding: 14px 20px;
  text-align: left;
  margin-bottom: 14px;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bank-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bank-lbl {
  color: #64748b;
  font-weight: 500;
}

.bank-val {
  color: #0f172a;
  font-weight: 700;
}

.bank-val.highlight {
  color: #b45309;
  background: #fef3c7;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  font-weight: 800;
}

.qr-popup-footer {
  font-size: 12px;
  color: #16a34a;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding-top: 10px;
  border-top: 1px solid #e2e8f0;
}

/* Transitions */
.qr-popup-fade-enter-active,
.qr-popup-fade-leave-active {
  transition: opacity 0.25s ease;
}

.qr-popup-fade-enter-from,
.qr-popup-fade-leave-to {
  opacity: 0;
}

/* Thank you Box */
.thank-you-box {
  margin-top: auto;
  text-align: center;
  padding: 16px 10px 6px;
}

.thank-heart {
  font-size: 22px;
  margin-bottom: 4px;
}

.thank-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin: 0 0 4px;
}

.thank-hotline {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.delivery-address-pill {
  font-weight: 500;
  font-size: 13px;
  opacity: 0.95;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 6px;
}

/* Delivery Info Box */
.delivery-info-box {
  background: #f8fafc;
  border: 1.5px dashed #cbd5e1;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 14px;
}

.del-box-header {
  display: flex;
  align-items: center;
  font-size: 12px;
  font-weight: 800;
  color: #dc2626;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  border-bottom: 1px dashed #e2e8f0;
  padding-bottom: 6px;
}

.del-box-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.del-line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}

.del-lbl {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.del-val {
  color: #1e293b;
  text-align: right;
  font-weight: 600;
}

/* POS Sync Popup (View-only, Zero Buttons) */
.pos-sync-popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(8px);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.pos-sync-popup-card {
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 25px 60px -10px rgba(0, 0, 0, 0.4);
  max-width: 520px;
  width: 90%;
  padding: 32px 28px 24px;
  text-align: center;
  border: 2px solid #e2e8f0;
  animation: qrCardPop 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.pos-popup-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.pos-popup-icon-wrap.icon-warning {
  background: #fffbeb;
  color: #f59e0b;
}

.pos-popup-icon-wrap.icon-error {
  background: #fef2f2;
  color: #dc2626;
}

.pos-popup-icon-wrap.icon-success {
  background: #f0fdf4;
  color: #16a34a;
}

.pos-popup-icon-wrap.icon-info {
  background: #eff6ff;
  color: #2563eb;
}

.pos-popup-title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 10px;
}

.pos-popup-desc {
  font-size: 15px;
  color: #475569;
  line-height: 1.5;
  margin-bottom: 20px;
}

.pos-popup-sub-hint {
  font-size: 12px;
  color: #94a3b8;
  border-top: 1px solid #f1f5f9;
  padding-top: 12px;
}

@media (max-width: 900px) {
  .display-main {
    grid-template-columns: 1fr;
  }
}
</style>
