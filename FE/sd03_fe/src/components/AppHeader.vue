<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dangXuat, getRefreshToken, getUser } from '../utils/auth'
import { huyPhien } from '../api/auth'

defineProps({
  title: { type: String, default: '' },
})

const route = useRoute()
const router = useRouter()
const nguoiDung = ref(getUser())

watch(route, () => {
  nguoiDung.value = getUser()
})

const xulyDangXuat = async () => {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) await huyPhien(refreshToken)
  } catch {
    dangXuat()
  }
  dangXuat()
  router.push('/dang-nhap')
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
      <div class="user-profile-badge">
        <div class="avatar-circle">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <div class="user-meta">
          <span class="user-name">Admin</span>
          <span class="user-role-text">Duc</span>
        </div>
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
</style>
