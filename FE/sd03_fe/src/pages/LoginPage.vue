<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { dangNhap } from '../api/auth'
import { luuPhien } from '../utils/auth'

const router = useRouter()

const form = ref({ taiKhoan: '', matKhau: '' })
const error = ref('')
const loading = ref(false)

const submit = async () => {
  error.value = ''
  if (!form.value.taiKhoan.trim() || !form.value.matKhau) {
    error.value = 'Vui lòng nhập tài khoản và mật khẩu'
    return
  }
  loading.value = true
  try {
    const resp = await dangNhap({
      taiKhoan: form.value.taiKhoan.trim(),
      matKhau: form.value.matKhau,
    })
    luuPhien(resp)
    router.push('/hoa-don')
  } catch (e) {
    error.value = e.message || 'Đăng nhập thất bại'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-screen">
    <div class="auth-card">
      <div class="auth-brand">
        <img src="/images/logo_polyshoes_final.png" alt="PolyShoes" />
      </div>
      <h1 class="auth-title">Đăng nhập</h1>
      <p class="auth-sub">Đăng nhập để sử dụng phiếu giảm giá theo tài khoản của bạn</p>

      <form class="auth-form" @submit.prevent="submit">
        <div class="field">
          <label>Email hoặc tên tài khoản</label>
          <input
            v-model="form.taiKhoan"
            type="text"
            placeholder="nguyenvana hoặc user@example.com"
            autocomplete="username"
          />
        </div>

        <div class="field">
          <label>Mật khẩu</label>
          <input
            v-model="form.matKhau"
            type="password"
            placeholder="Nhập mật khẩu"
            autocomplete="current-password"
          />
        </div>

        <p v-if="error" class="auth-error">{{ error }}</p>

        <button class="auth-btn" type="submit" :disabled="loading">
          {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
        </button>
      </form>

      <p class="auth-switch">
        Chưa có tài khoản?
        <router-link to="/dang-ky">Đăng ký</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-screen {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f7;
  padding: 24px;
}

.auth-card {
  width: min(400px, 100%);
  background: var(--white);
  border-radius: 14px;
  padding: 28px 30px 26px;
  box-shadow: 0 10px 34px rgba(20, 20, 30, 0.08);
}

.auth-brand {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
}

.auth-brand img {
  width: 110px;
  height: 76px;
  object-fit: contain;
}

.auth-title {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: #22222a;
  margin: 0;
}

.auth-sub {
  text-align: center;
  font-size: 12.5px;
  color: #9a9aa3;
  margin: 6px 0 18px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field label {
  display: block;
  font-size: 12.5px;
  color: #55555e;
  margin-bottom: 5px;
  font-weight: 600;
}

.field input {
  width: 100%;
  height: 40px;
  border: 1px solid #dcdce2;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13.5px;
  color: #2c2c33;
  background: #fbfbfc;
  outline: none;
  transition: border-color 0.15s ease;
}

.field input:focus {
  border-color: var(--red);
}

.auth-error {
  margin: 0;
  font-size: 13px;
  color: #dc2626;
  font-weight: 600;
}

.auth-btn {
  height: 42px;
  border: none;
  border-radius: 9px;
  background: var(--red);
  color: #fff;
  font-size: 14.5px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.auth-switch {
  text-align: center;
  font-size: 13px;
  color: #6a6a73;
  margin: 16px 0 0;
}

.auth-switch a {
  color: var(--red);
  font-weight: 700;
  text-decoration: none;
}

.auth-switch a:hover {
  text-decoration: underline;
}
</style>
