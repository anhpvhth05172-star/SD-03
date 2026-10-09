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
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3.5 10.5L12 3.5l8.5 7v9a1.5 1.5 0 0 1-1.5 1.5h-4.5v-6h-5v6H5a1.5 1.5 0 0 1-1.5-1.5v-9z" />
      </svg>
      <span class="crumb">Trang chủ</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">{{ title }}</span>
    </nav>

    <div class="auth-box">
      <template v-if="nguoiDung">
        <span class="auth-role" v-if="nguoiDung.vaiTro === 'ADMIN'">Admin</span>
        <span class="auth-user" :title="nguoiDung.email">{{ nguoiDung.tenTaiKhoan }}</span>
        <button class="auth-btn" type="button" @click="xulyDangXuat">Đăng xuất</button>
      </template>
      <router-link v-else class="auth-btn is-login" to="/dang-nhap">Đăng nhập</router-link>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: 56px;
  background: var(--white);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 22px;
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

.auth-role {
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--red);
  color: #fff;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.4px;
  text-transform: uppercase;
}

.auth-user {
  font-size: 12.5px;
  font-weight: 600;
  color: #61616a;
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.auth-btn {
  padding: 7px 16px;
  border: 1px solid #f2c4c9;
  border-radius: 9px;
  background: var(--red-soft);
  color: var(--red);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  cursor: pointer;
  transition: background 0.15s ease;
}

.auth-btn:hover {
  background: #fbe7e9;
}

.auth-btn.is-login {
  display: inline-block;
  text-decoration: none;
}
</style>
