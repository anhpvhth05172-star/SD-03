<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dangXuat, getRefreshToken, getUser } from '../utils/auth'
import { huyPhien } from '../api/auth'
import { laAdmin, tenVaiTro } from '../utils/session'
import { listInvoices } from '../api/invoice'
import { formatTimeAgo, formatVnd } from '../utils/format'

defineProps({
  title: { type: String, default: '' },
})

const route = useRoute()
const router = useRouter()
const nguoiDung = ref(getUser())
const isAdmin = computed(() => laAdmin())
const notificationOpen = ref(false)
const notificationWrapRef = ref(null)
const pendingOrders = ref([])
const pendingOrderCount = ref(0)
const isRefreshing = ref(false)
let notificationTimer

const loadPendingOrders = async () => {
  if (!isAdmin.value) return
  try {
    const result = await listInvoices({ trangThai: 'CHO_XAC_NHAN', page: 0, size: 8 })
    pendingOrders.value = result?.content || []
    pendingOrderCount.value = Number(result?.totalElements || 0)
  } catch {
  }
}

const manualRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  await loadPendingOrders()
  setTimeout(() => {
    isRefreshing.value = false
  }, 450)
}

const handleClickOutside = (e) => {
  if (notificationOpen.value && notificationWrapRef.value && !notificationWrapRef.value.contains(e.target)) {
    notificationOpen.value = false
  }
}

const getOrderTypeClass = (loaiDon) => {
  const type = String(loaiDon || '').toUpperCase()
  if (type.includes('GIAO') || type.includes('DELIVERY')) return 'type-delivery'
  if (type.includes('QUẦY') || type.includes('QUAY') || type.includes('POS')) return 'type-store'
  if (type.includes('ONLINE') || type.includes('TRỰC TUYẾN')) return 'type-online'
  return 'type-default'
}

if (typeof window !== 'undefined') {
  window.addEventListener('storage', () => {
    nguoiDung.value = getUser()
  })
  window.addEventListener('polyshoes-user-updated', () => {
    nguoiDung.value = getUser()
  })
}

watch(route, () => {
  nguoiDung.value = getUser()
})

onMounted(() => {
  loadPendingOrders()
  notificationTimer = window.setInterval(loadPendingOrders, 30000)
  document.addEventListener('mousedown', handleClickOutside)
})

onUnmounted(() => {
  window.clearInterval(notificationTimer)
  document.removeEventListener('mousedown', handleClickOutside)
})

const tenHienThi = () => nguoiDung.value?.tenTaiKhoan || 'Admin'

const xulyDangXuat = async () => {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) await huyPhien(refreshToken)
  } catch {
  }
  dangXuat()
  router.push('/nhan-vien')
}
</script>

<template>
  <header class="app-header">
    <nav class="breadcrumb">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
        stroke-linejoin="round">
        <path d="M3.5 10.5L12 3.5l8.5 7v9a1.5 1.5 0 0 1-1.5 1.5h-4.5v-6h-5v6H5a1.5 1.5 0 0 1-1.5-1.5v-9z" />
      </svg>
      <span class="crumb">Trang chủ</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">{{ title }}</span>
    </nav>

    <div class="auth-box">
      <!-- Notification Dropdown -->
      <div v-if="isAdmin" ref="notificationWrapRef" class="notification-wrap">
        <button type="button" class="notification-button"
          :class="{ 'is-active': notificationOpen, 'has-badge': pendingOrderCount > 0 }" aria-label="Thông báo đơn hàng"
          :aria-expanded="notificationOpen" @click="notificationOpen = !notificationOpen">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
            stroke-linejoin="round" aria-hidden="true">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
            <path d="M10 21h4" />
          </svg>
          <span v-if="pendingOrderCount > 0" class="notification-count">
            {{ pendingOrderCount > 99 ? '99+' : pendingOrderCount }}
          </span>
        </button>

        <Transition name="dropdown-pop">
          <div v-if="notificationOpen" class="notification-dropdown">
            <!-- Header -->
            <div class="notif-header">
              <div class="notif-header-title">
                <span class="title-text">Đơn hàng chờ xác nhận</span>
                <span v-if="pendingOrderCount > 0" class="badge-count">{{ pendingOrderCount }} mới</span>
              </div>
              <button type="button" class="btn-refresh" :class="{ 'is-spinning': isRefreshing }" title="Làm mới"
                @click.stop="manualRefresh">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                  stroke-linejoin="round">
                  <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
                </svg>
              </button>
            </div>

            <div v-if="pendingOrders.length > 0" class="notif-list custom-scrollbar">
              <RouterLink v-for="order in pendingOrders" :key="order.id" :to="`/hoa-don/${order.id}`" class="notif-item"
                @click="notificationOpen = false">
                <div class="item-avatar">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
                    stroke-linejoin="round">
                    <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
                    <line x1="3" y1="6" x2="21" y2="6" />
                    <path d="M16 10a4 4 0 0 1-8 0" />
                  </svg>
                  <span class="item-avatar-ping"></span>
                </div>

                <div class="item-content">
                  <div class="item-row-top">
                    <span class="order-code">#{{ order.maHoaDon || order.id }}</span>
                    <span class="order-time">{{ formatTimeAgo(order.ngayTao) }}</span>
                  </div>

                  <div class="item-row-mid">
                    <span class="customer-info" :title="order.tenKhachHang || 'Khách hàng'">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                        stroke-linejoin="round">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                      </svg>
                      {{ order.tenKhachHang || 'Khách mua tại quầy / trực tuyến' }}
                    </span>
                    <span v-if="order.loaiDon" class="order-type-tag" :class="getOrderTypeClass(order.loaiDon)">
                      {{ order.loaiDon }}
                    </span>
                  </div>

                  <div class="item-row-bot">
                    <span class="order-price">{{ formatVnd(order.tienSauGiamGia ?? order.tongTien) }}</span>
                    <span class="view-link">
                      Xử lý ngay
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
                        stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="9 18 15 12 9 6" />
                      </svg>
                    </span>
                  </div>
                </div>
              </RouterLink>
            </div>

            <!-- Empty State -->
            <div v-else class="notif-empty">
              <div class="empty-icon-wrap">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
                  stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
              </div>
              <p class="empty-title">Không có đơn chờ duyệt</p>
              <p class="empty-desc">Tất cả đơn hàng hiện tại đã được xử lý</p>
            </div>

            <!-- Footer -->
            <div class="notif-footer">
              <RouterLink to="/hoa-don" class="notif-view-all" @click="notificationOpen = false">
                <span>Xem tất cả đơn hàng</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                  stroke-linejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </RouterLink>
            </div>
          </div>
        </Transition>
      </div>

      <!-- User Profile Badge -->
      <div class="user-profile-badge">
        <div class="avatar-circle">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <div class="user-meta">
          <span class="user-name">{{ tenHienThi() }}</span>
          <span class="user-role-text">{{ tenVaiTro() }}</span>
        </div>
        <button type="button" class="btn-logout" @click="xulyDangXuat">Đăng xuất</button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 24px;
  position: relative;
  z-index: 40;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.breadcrumb svg {
  width: 15px;
  height: 15px;
  color: #475569;
}

.crumb {
  font-weight: 600;
  color: #475569;
}

.crumb-sep {
  color: #94a3b8;
}

.crumb-current {
  color: #0f172a;
  font-weight: 600;
}

.auth-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* =========================================================
   Notification Trigger Button
   ========================================================= */
.notification-wrap {
  position: relative;
}

.notification-button {
  position: relative;
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 50%;
  color: #475569;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.notification-button:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.notification-button.is-active {
  border-color: #dc2626;
  color: #dc2626;
  background: #fef2f2;
}

.notification-button svg {
  width: 20px;
  height: 20px;
  transition: transform 0.2s ease;
}

.notification-button:hover svg {
  transform: rotate(8deg);
}

.notification-count {
  position: absolute;
  top: -3px;
  right: -3px;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 10px;
  color: #ffffff;
  background: #dc2626;
  font-size: 10.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 5px rgba(220, 38, 38, 0.4);
  animation: pulse-badge 2s infinite ease-in-out;
}

@keyframes pulse-badge {

  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.08);
  }
}

/* =========================================================
   Notification Dropdown Menu
   ========================================================= */
.notification-dropdown {
  position: absolute;
  z-index: 50;
  top: calc(100% + 12px);
  right: -10px;
  width: 380px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 20px 35px -5px rgba(15, 23, 42, 0.15), 0 10px 18px -6px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Header */
.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

.notif-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.badge-count {
  background: #fee2e2;
  color: #dc2626;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 12px;
  border: 1px solid #fecaca;
}

.btn-refresh {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover {
  background: #f8fafc;
  color: #0f172a;
  border-color: #cbd5e1;
}

.btn-refresh svg {
  width: 14px;
  height: 14px;
}

.btn-refresh.is-spinning svg {
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

/* List of Orders */
.notif-list {
  max-height: 390px;
  overflow-y: auto;
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 12px;
  text-decoration: none;
  background: #ffffff;
  border: 1px solid transparent;
  transition: all 0.18s ease;
}

.notif-item:hover {
  background: #f8fafc;
  border-color: #e2e8f0;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.04);
}

.item-avatar {
  position: relative;
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #fef2f2;
  border: 1px solid #fee2e2;
  color: #dc2626;
  display: grid;
  place-items: center;
}

.item-avatar svg {
  width: 19px;
  height: 19px;
}

.item-avatar-ping {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background: #dc2626;
  border-radius: 50%;
  border: 1.5px solid #ffffff;
}

.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.order-code {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.notif-item:hover .order-code {
  color: #dc2626;
}

.order-time {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.item-row-mid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.customer-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #475569;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 190px;
}

.customer-info svg {
  width: 13px;
  height: 13px;
  color: #94a3b8;
  flex-shrink: 0;
}

.order-type-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
  border: 1px solid transparent;
}

.order-type-tag.type-delivery {
  background: #eff6ff;
  color: #2563eb;
  border-color: #dbeafe;
}

.order-type-tag.type-store {
  background: #ecfdf5;
  color: #059669;
  border-color: #d1fae5;
}

.order-type-tag.type-online {
  background: #f5f3ff;
  color: #7c3aed;
  border-color: #ede9fe;
}

.order-type-tag.type-default {
  background: #f1f5f9;
  color: #475569;
  border-color: #e2e8f0;
}

.item-row-bot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
}

.order-price {
  font-size: 13px;
  font-weight: 700;
  color: #dc2626;
}

.view-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.18s ease;
}

.view-link svg {
  width: 13px;
  height: 13px;
}

.notif-item:hover .view-link {
  opacity: 1;
  transform: translateX(0);
  color: #dc2626;
}

/* Empty State */
.notif-empty {
  padding: 36px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.empty-icon-wrap {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #dcfce7;
  display: grid;
  place-items: center;
  margin-bottom: 4px;
}

.empty-icon-wrap svg {
  width: 24px;
  height: 24px;
}

.empty-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.empty-desc {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

/* Footer */
.notif-footer {
  padding: 10px 14px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.notif-view-all {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  font-size: 12.5px;
  font-weight: 600;
  color: #dc2626;
  text-decoration: none;
  background: #ffffff;
  border: 1px solid #fee2e2;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.notif-view-all svg {
  width: 14px;
  height: 14px;
  transition: transform 0.2s ease;
}

.notif-view-all:hover {
  background: #dc2626;
  color: #ffffff;
  border-color: #dc2626;
  box-shadow: 0 4px 10px rgba(220, 38, 38, 0.25);
}

.notif-view-all:hover svg {
  transform: translateX(3px);
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: #f8fafc;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* Dropdown Animation */
.dropdown-pop-enter-active,
.dropdown-pop-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.dropdown-pop-enter-from,
.dropdown-pop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}

/* =========================================================
   User Profile Badge
   ========================================================= */
.user-profile-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 12px 5px 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 30px;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fee2e2;
  color: #dc2626;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.user-role-text {
  font-size: 10.5px;
  color: #64748b;
  line-height: 1.2;
}

.btn-logout {
  margin-left: 4px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
  border-radius: 16px;
  padding: 6px 12px;
  cursor: pointer;
}

.btn-logout:hover {
  background: #fef2f2;
  border-color: #fecaca;
}
</style>
