<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { laAdmin } from '../utils/session'

defineProps({
  collapsed: { type: Boolean, default: false },
})

const route = useRoute()

const menu = [
  {
    label: 'Thống kê',
    path: '/thong-ke',
    iconType: 'chart',
  },
  {
    label: 'Bán hàng tại quầy',
    path: '/ban-hang',
    iconType: 'pos',
  },
  {
    label: 'Quản lý hóa đơn',
    path: '/hoa-don',
    iconType: 'invoice',
  },
  {
    label: 'Quản lý sản phẩm',
    path: '/san-pham',
    iconType: 'product',
    children: [
      { label: 'Sản phẩm', path: '/san-pham' },
      { label: 'Biến thể sản phẩm', path: '/bien-the-san-pham' },
    ],
  },
  {
    label: 'Danh mục thuộc tính',
    path: '/thuoc-tinh',
    iconType: 'attributes',
    children: [
      { label: 'Thương hiệu', path: '/thuoc-tinh/thuong-hieu' },
      { label: 'Xuất xứ', path: '/thuoc-tinh/xuat-xu' },
      { label: 'Chất liệu', path: '/thuoc-tinh/chat-lieu' },
      { label: 'Kiểu dáng', path: '/thuoc-tinh/kieu-dang' },
      { label: 'Loại giày', path: '/thuoc-tinh/loai-giay' },
      { label: 'Kích cỡ', path: '/thuoc-tinh/kich-co' },
      { label: 'Màu sắc', path: '/thuoc-tinh/mau-sac' },
      { label: 'Thân giày', path: '/thuoc-tinh/than-giay' },
      { label: 'Đế giày', path: '/thuoc-tinh/de-giay' },
    ],
  },
  {
    label: 'Quản lý khách hàng',
    path: '/khach-hang',
    iconType: 'customers',
  },
  {
    label: 'Quản lý nhân viên',
    path: '/nhan-vien',
    iconType: 'employees',
    adminOnly: true,
  },
  {
    label: 'Quản lý giảm giá',
    path: '/phieu-giam-gia',
    iconType: 'discounts',
    children: [
      { label: 'Phiếu giảm giá', path: '/phieu-giam-gia' },
      { label: 'Đợt giảm giá', path: '/dot-giam-gia' },
    ],
  },
]

const openState = ref({})

const isOpen = (item) => {
  if (item.children) {
    const auto = item.children.some((child) => isChildActive(child))
    return openState.value[item.path] ?? auto
  }
  return false
}

const isChildActive = (child) => {
  if (child.path === '/san-pham') {
    return route.path === '/san-pham' || route.path === '/san-pham/them'
  }
  if (child.path === '/bien-the-san-pham') {
    return route.path === '/bien-the-san-pham' || route.path.startsWith('/san-pham/bien-the')
  }
  return route.path === child.path || route.path.startsWith(child.path + '/')
}

const isParentActive = (item) => {
  if (item.children) {
    return item.children.some((child) => isChildActive(child))
  }
  return route.path === item.path || route.path.startsWith(item.path + '/')
}

const toggle = (item) => {
  if (!item.children) return
  const auto = item.children.some((child) => isChildActive(child))
  openState.value = { ...openState.value, [item.path]: !(openState.value[item.path] ?? auto) }
}

const visibleMenu = computed(() => menu.filter((item) => !item.adminOnly || laAdmin()))
</script>

<template>
  <aside class="sidebar-container" :class="{ 'is-collapsed': collapsed }">
    <div class="brand-wrapper">
      <router-link to="/san-pham" class="brand-link">
        <div class="brand-logo-card">
          <img src="/images/logo_polyshoes_final.png" alt="PolyShoes Logo" class="brand-img" />
        </div>
      </router-link>
    </div>

    <nav class="sidebar-nav">
      <div class="menu-list">
        <template v-for="item in visibleMenu" :key="item.path">
          <router-link v-if="!item.children" :to="item.path" class="nav-item-btn"
            :class="{ 'is-active': isParentActive(item) }">
            <div class="nav-icon-box">
              <svg v-if="item.iconType === 'chart'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>

              <svg v-else-if="item.iconType === 'pos'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>

              <svg v-else-if="item.iconType === 'invoice'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>

              <svg v-else-if="item.iconType === 'customers'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>

              <svg v-else-if="item.iconType === 'employees'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <span class="nav-text">{{ item.label }}</span>
          </router-link>

          <div v-else class="nav-group-wrapper" :class="{ 'is-expanded': isOpen(item) }">
            <button type="button" class="nav-item-btn nav-group-header"
              :class="{ 'is-active': isParentActive(item), 'is-open': isOpen(item) }" @click="toggle(item)">
              <div class="nav-icon-box">
                <svg v-if="item.iconType === 'product'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path
                    d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                  <line x1="12" y1="22.08" x2="12" y2="12" />
                </svg>

                <svg v-else-if="item.iconType === 'attributes'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="8" y1="6" x2="21" y2="6" />
                  <line x1="8" y1="12" x2="21" y2="12" />
                  <line x1="8" y1="18" x2="21" y2="18" />
                  <line x1="3" y1="6" x2="3.01" y2="6" />
                  <line x1="3" y1="12" x2="3.01" y2="12" />
                  <line x1="3" y1="18" x2="3.01" y2="18" />
                </svg>

                <svg v-else-if="item.iconType === 'discounts'" width="18" height="18" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
                  <line x1="7" y1="7" x2="7.01" y2="7" />
                </svg>
              </div>
              <span class="nav-text">{{ item.label }}</span>
              <svg class="chevron-arrow" :class="{ 'is-flipped': isOpen(item) }" width="14" height="14"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>

            <div v-if="isOpen(item)" class="submenu-container">
              <div class="submenu-tree-line"></div>
              <div class="submenu-list">
                <router-link v-for="child in item.children" :key="child.path" :to="child.path" class="submenu-item-btn"
                  :class="{ 'is-active': isChildActive(child) }">
                  <span class="submenu-bullet"></span>
                  <span class="submenu-text">{{ child.label }}</span>
                </router-link>
              </div>
            </div>
          </div>
        </template>
      </div>
    </nav>

    <div class="sidebar-footer-card">
      <div class="status-indicator">
        <span class="pulse-dot"></span>
        <span class="status-title">PolyShoes Online</span>
      </div>
      <span class="status-sub">Hệ thống đang hoạt động</span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-container {
  width: 260px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.03), 0 2px 6px -1px rgba(0, 0, 0, 0.02);
  padding: 16px 12px 18px;
  display: flex;
  flex-direction: column;
  user-select: none;
}

.sidebar-container.is-collapsed {
  display: none;
}

/* --------------------------------------------------------------------------
   Brand Logo
   -------------------------------------------------------------------------- */
.brand-wrapper {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.brand-link {
  display: block;
  text-decoration: none;
}

.brand-logo-card {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 6px 0;
  border-radius: 12px;
  transition: transform 0.2s ease;
}

.brand-logo-card:hover {
  transform: scale(1.02);
}

.brand-img {
  max-width: 140px;
  height: 90px;
  object-fit: contain;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.06));
}

/* --------------------------------------------------------------------------
   Navigation Menu
   -------------------------------------------------------------------------- */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
}

.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 4px;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* Nav Item Button */
.nav-item-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  color: #475569;
  font-size: 13.5px;
  font-weight: 500;
  text-decoration: none;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-item-btn:hover {
  background: #f8fafc;
  color: #0f172a;
  transform: translateX(3px);
}

.nav-icon-box {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #64748b;
  transition: color 0.18s ease;
}

.nav-item-btn:hover .nav-icon-box {
  color: #0f172a;
}

.nav-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

/* Active State for Parent / Single */
.nav-item-btn.is-active {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  color: #dc2626;
  border-color: #fecaca;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(220, 38, 38, 0.08);
}

.nav-item-btn.is-active .nav-icon-box {
  color: #dc2626;
}

/* Chevron */
.chevron-arrow {
  color: #94a3b8;
  flex-shrink: 0;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.chevron-arrow.is-flipped {
  transform: rotate(180deg);
  color: #dc2626;
}

/* --------------------------------------------------------------------------
   Submenu Accordion
   -------------------------------------------------------------------------- */
.nav-group-wrapper {
  display: flex;
  flex-direction: column;
}

.submenu-container {
  position: relative;
  margin: 4px 0 6px 14px;
  padding-left: 14px;
}

.submenu-tree-line {
  position: absolute;
  top: 4px;
  bottom: 8px;
  left: 0;
  width: 2px;
  background: #e2e8f0;
  border-radius: 1px;
}

.submenu-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.submenu-item-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.15s ease;
}

.submenu-bullet {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
  transition: all 0.18s ease;
}

.submenu-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.submenu-item-btn:hover {
  background: #f8fafc;
  color: #0f172a;
  transform: translateX(2px);
}

.submenu-item-btn:hover .submenu-bullet {
  background: #94a3b8;
  transform: scale(1.2);
}

/* Active Submenu Item */
.submenu-item-btn.is-active {
  background: #fee2e2;
  color: #dc2626;
  font-weight: 600;
  border-color: #fca5a5;
  box-shadow: 0 1px 3px rgba(220, 38, 38, 0.08);
}

.submenu-item-btn.is-active .submenu-bullet {
  background: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.2);
}

/* --------------------------------------------------------------------------
   Sidebar Footer Status Card
   -------------------------------------------------------------------------- */
.sidebar-footer-card {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 7px;
}

.pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.status-title {
  font-size: 11.5px;
  font-weight: 700;
  color: #334155;
}

.status-sub {
  font-size: 10.5px;
  color: #94a3b8;
  margin-left: 14px;
}
</style>
