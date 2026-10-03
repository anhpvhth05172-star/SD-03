<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const isAddPage = computed(() => route.path.includes('/them'))
const isVariantPage = computed(() => route.path.includes('/bien-the'))

const navigateToProducts = () => {
  router.push('/san-pham')
}
</script>

<template>
  <header class="topbar">
    <div class="topbar-left">
      <div class="folder-icon-box">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="#334155">
          <path
            d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z" />
        </svg>
      </div>

      <nav class="breadcrumb-nav">
        <span class="breadcrumb-item breadcrumb-clickable" @click="navigateToProducts">Quản lý sản phẩm</span>

        <template v-if="isVariantPage">
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item breadcrumb-clickable" @click="navigateToProducts">Biến thể sản phẩm</span>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item breadcrumb-red">Quản lý biến thể</span>
        </template>

        <template v-else>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-item" :class="{ 'breadcrumb-red': !isAddPage, 'breadcrumb-clickable': isAddPage }"
            @click="navigateToProducts">
            Sản phẩm
          </span>
          <template v-if="isAddPage">
            <span class="breadcrumb-separator">/</span>
            <span class="breadcrumb-item breadcrumb-red">Thêm sản phẩm</span>
          </template>
        </template>
      </nav>
    </div>

    <div class="topbar-right">
      <button class="icon-circle-btn" title="Thông báo">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#1e293b" stroke-width="2"
          stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
          <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
        </svg>
        <span class="notification-dot"></span>
      </button>

      <div class="user-profile-badge">
        <div class="avatar-circle">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
          </svg>
        </div>
        <div class="user-info">
          <span class="user-fullname">Duc</span>
          <span class="user-role">Quản lý</span>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  height: 68px;
  background-color: #ffffff;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 20;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.folder-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.breadcrumb-separator {
  color: #94a3b8;
  font-weight: 500;
}

.breadcrumb-item {
  color: #1e293b;
  transition: color 0.15s ease;
}

.breadcrumb-clickable {
  cursor: pointer;
}

.breadcrumb-clickable:hover {
  color: var(--primary);
}

.breadcrumb-red {
  color: var(--primary);
  font-weight: 700;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-circle-btn {
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 6px;
  border-radius: 50%;
  transition: background 0.15s;
}

.icon-circle-btn:hover {
  background-color: #f1f5f9;
}

.notification-dot {
  position: absolute;
  top: 5px;
  right: 6px;
  width: 7px;
  height: 7px;
  background-color: #0f172a;
  border-radius: 50%;
  border: 1.5px solid #ffffff;
}

.user-profile-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background-color 0.15s;
}

.user-profile-badge:hover {
  background-color: #f8fafc;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #0f172a;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-fullname {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
}

.user-role {
  font-size: 11.5px;
  color: #64748b;
  font-weight: 500;
}
</style>
