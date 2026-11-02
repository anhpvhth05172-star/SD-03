<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dangXuat, getRefreshToken, getUser } from '../utils/auth'
import { huyPhien } from '../api/auth'
import { laAdmin, tenVaiTro } from '../utils/session'
import { listInvoices } from '../api/invoice'

defineProps({
  title: { type: String, default: '' },
})

const route = useRoute()
const router = useRouter()
const nguoiDung = ref(getUser())
const isAdmin = computed(() => laAdmin())
const notificationOpen = ref(false)
const pendingOrders = ref([])
const pendingOrderCount = ref(0)
let notificationTimer

const loadPendingOrders = async () => {
  if (!isAdmin.value) return
  try {
    const result = await listInvoices({ trangThai: 'CHO_XAC_NHAN', page: 0, size: 5 })
    pendingOrders.value = result?.content || []
    pendingOrderCount.value = Number(result?.totalElements || 0)
  } catch {
    // Tải lại ở chu kỳ tiếp theo; không làm ảnh hưởng thanh điều hướng.
  }
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
})

onUnmounted(() => window.clearInterval(notificationTimer))

const tenHienThi = () => nguoiDung.value?.tenTaiKhoan || 'Admin'

const xulyDangXuat = async () => {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) await huyPhien(refreshToken)
  } catch {
    // Bỏ qua lỗi gọi API đăng xuất phía server.
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
      <div v-if="isAdmin" class="notification-wrap">
        <button
          type="button"
          class="notification-button"
          aria-label="Đơn hàng mới"
          :aria-expanded="notificationOpen"
          @click="notificationOpen = !notificationOpen"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
            <path d="M10 21h4" />
          </svg>
          <span v-if="pendingOrderCount" class="notification-count">{{ pendingOrderCount > 99 ? '99+' : pendingOrderCount }}</span>
        </button>
        <div v-if="notificationOpen" class="notification-menu">
          <div class="notification-title">Đơn hàng chờ xác nhận</div>
          <RouterLink
            v-for="order in pendingOrders"
            :key="order.id"
            :to="`/hoa-don/${order.id}`"
            class="notification-order"
            @click="notificationOpen = false"
          >
            <strong>{{ order.maHoaDon || `#${order.id}` }}</strong>
            <span>{{ order.tenKhachHang || 'Khách hàng' }}</span>
          </RouterLink>
          <div v-if="!pendingOrders.length" class="notification-empty">Chưa có đơn mới</div>
          <RouterLink to="/hoa-don" class="notification-all" @click="notificationOpen = false">Xem danh sách đơn hàng</RouterLink>
        </div>
      </div>
      <div class="user-profile-badge">
        <div class="avatar-circle">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
            stroke-linejoin="round">
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
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4a4a52;
  font-size: 13px;
}

.breadcrumb svg {
  width: 15px;
  height: 15px;
  color: #2b2b32;
}

.crumb {
  font-weight: 600;
  color: #2b2b32;
}

.crumb-sep {
  color: #b0b0b8;
}

.crumb-current {
  color: #23232a;
  font-weight: 600;
}

.auth-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

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
  color: #334155;
  background: #fff;
  cursor: pointer;
}

.notification-button svg {
  width: 19px;
  height: 19px;
}

.notification-count {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  padding: 1px 5px;
  border-radius: 10px;
  color: #fff;
  background: #dc2626;
  font-size: 10px;
  font-weight: 700;
  text-align: center;
}

.notification-menu {
  position: absolute;
  z-index: 20;
  top: calc(100% + 10px);
  right: 0;
  width: 280px;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 28px rgb(15 23 42 / 14%);
}

.notification-title,
.notification-all {
  display: block;
  padding: 9px 10px;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
}

.notification-order {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 9px 10px;
  border-radius: 8px;
  color: #334155;
  font-size: 12px;
  text-decoration: none;
}

.notification-order:hover,
.notification-all:hover {
  background: #f8fafc;
}

.notification-order span,
.notification-empty {
  color: #64748b;
  font-size: 11px;
}

.notification-empty {
  padding: 12px 10px;
}

.notification-all {
  border-top: 1px solid #e2e8f0;
  color: #b91c1c;
  text-decoration: none;
}

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
