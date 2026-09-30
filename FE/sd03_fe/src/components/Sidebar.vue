<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const isProductMenuOpen = ref(true)

const toggleProductMenu = () => {
  isProductMenuOpen.value = !isProductMenuOpen.value
}

const navigateTo = (path) => {
  router.push(path)
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo-wrapper" @click="navigateTo('/san-pham')">
        <div class="logo-circle-badge">
          <img src="/logo.png" alt="POLY SHOES Logo" class="logo-img" />
        </div>
      </div>
    </div>

    <nav class="sidebar-nav">
      <ul class="nav-list">
        <li class="nav-item">
          <a class="nav-link">
            <span class="nav-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="20" x2="18" y2="10"></line>
                <line x1="12" y1="20" x2="12" y2="4"></line>
                <line x1="6" y1="20" x2="6" y2="14"></line>
              </svg>
            </span>
            <span class="nav-text">Thống kê</span>
          </a>
        </li>

        <li class="nav-item">
          <a class="nav-link">
            <span class="nav-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                <polyline points="9 22 9 12 15 12 15 22"></polyline>
              </svg>
            </span>
            <span class="nav-text">Bán hàng tại quầy</span>
          </a>
        </li>

        <li class="nav-item">
          <a class="nav-link">
            <span class="nav-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
            </span>
            <span class="nav-text">Quản lý hóa đơn</span>
          </a>
        </li>

        <li class="nav-item has-subnav" :class="{ 'open': isProductMenuOpen }">
          <div class="nav-link parent-active" @click="toggleProductMenu">
            <span class="nav-icon group-active-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z" />
              </svg>
            </span>
            <span class="nav-text font-bold">Quản lý sản phẩm</span>
          </div>

          <ul class="subnav-list" v-show="isProductMenuOpen">
            <li class="subnav-item">
              <a class="subnav-link"
                :class="{ 'active': route.path === '/san-pham' || route.path === '/' || route.path === '/san-pham/them' }"
                @click="navigateTo('/san-pham')">
                <span class="subnav-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                    stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 14c2-4 5-6 10-6h6a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H4a1 1 0 0 1-1-1v-2z"></path>
                    <path d="M12 8v3"></path>
                    <path d="M15 8v3"></path>
                  </svg>
                </span>
                <span class="subnav-text">Sản phẩm</span>
              </a>
            </li>

            <li class="subnav-item">
              <a
                class="subnav-link"
                :class="{ 'active': route.path === '/san-pham/bien-the' }"
                @click="navigateTo('/san-pham/bien-the')"
              >
                <span class="subnav-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                    stroke-linecap="round" stroke-linejoin="round">
                    <path
                      d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z">
                    </path>
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                    <line x1="12" y1="22.08" x2="12" y2="12"></line>
                  </svg>
                </span>
                <span class="subnav-text">Biến thể sản phẩm</span>
              </a>
            </li>
          </ul>
        </li>

        <li class="nav-item">
          <a class="nav-link">
            <span class="nav-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </span>
            <span class="nav-text">Quản lý khách hàng</span>
          </a>
        </li>

        <li class="nav-item">
          <a class="nav-link">
            <span class="nav-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </span>
            <span class="nav-text">Quản lý nhân viên</span>
          </a>
        </li>

        <li class="nav-item">
          <a class="nav-link">
            <span class="nav-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="5" x2="5" y2="19"></line>
                <circle cx="6.5" cy="6.5" r="2.5"></circle>
                <circle cx="17.5" cy="17.5" r="2.5"></circle>
              </svg>
            </span>
            <span class="nav-text">Quản lý giảm giá</span>
          </a>
        </li>
      </ul>
    </nav>

    <div class="sidebar-footer">
      <span class="version-tag">Poly Shoes · v2.4</span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 250px;
  min-width: 250px;
  background-color: #ffffff;
  border-right: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  z-index: 30;
  user-select: none;
}

.sidebar-header {
  padding: 24px 20px 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.logo-wrapper {
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: transform 0.2s ease;
}

.logo-wrapper:hover {
  transform: scale(1.03);
}

.logo-circle-badge {
  width: 95px;
  height: 95px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  background: #ffffff;
  overflow: hidden;
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
}

.nav-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

a, a:hover, a:focus {
  text-decoration: none !important;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 16px;
  border-radius: 10px;
  color: #1e293b;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none !important;
  cursor: pointer;
  transition: all 0.15s ease;
}

.nav-link:hover {
  background-color: #f8fafc;
  color: #0f172a;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1e293b;
}

.parent-active {
  color: var(--primary);
  background-color: #fff1f2;
}

.group-active-icon {
  color: var(--primary);
}

.subnav-list {
  list-style: none;
  padding-left: 20px;
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.subnav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 10px;
  color: #475569;
  font-size: 13.5px;
  font-weight: 500;
  text-decoration: none !important;
  cursor: pointer;
  transition: all 0.15s ease;
}

.subnav-link:hover {
  background-color: #f8fafc;
  color: #0f172a;
}

.subnav-link.active {
  background-color: #ffe4e6;
  color: var(--primary);
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(217, 45, 32, 0.08);
}

.subnav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid #f8fafc;
}

.version-tag {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}
</style>
