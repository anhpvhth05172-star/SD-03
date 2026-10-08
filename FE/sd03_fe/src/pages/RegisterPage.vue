<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { dangKy } from '../api/auth'

const router = useRouter()

const form = ref({
  tenTaiKhoan: '',
  email: '',
  soDienThoai: '',
  matKhau: '',
  xacNhanMatKhau: '',
})
const error = ref('')
const loading = ref(false)

const submit = async () => {
  error.value = ''
  const f = form.value
  if (!f.tenTaiKhoan.trim()) {
    error.value = 'Tên tài khoản không được để trống'
    return
  }
  if (!f.email.trim()) {
    error.value = 'Email không được để trống'
    return
  }
  if (!/^[\w.%+-]+@[\w.-]+\.[A-Za-z]{2,}$/.test(f.email.trim())) {
    error.value = 'Email không hợp lệ'
    return
  }
  if (!f.soDienThoai.trim()) {
    error.value = 'Số điện thoại không được để trống'
    return
  }
  if (f.matKhau.length < 6) {
    error.value = 'Mật khẩu phải có ít nhất 6 ký tự'
    return
  }
  if (f.matKhau !== f.xacNhanMatKhau) {
    error.value = 'Mật khẩu xác nhận không khớp'
    return
  }

  loading.value = true
  try {
    await dangKy({
      tenTaiKhoan: f.tenTaiKhoan.trim(),
      email: f.email.trim(),
      soDienThoai: f.soDienThoai.trim(),
      matKhau: f.matKhau,
      xacNhanMatKhau: f.xacNhanMatKhau,
    })
    alert('Đăng ký thành công. Vui lòng đăng nhập.')
    router.push('/dang-nhap')
  } catch (e) {
    error.value = e.message || 'Đăng ký thất bại'
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
      <h1 class="auth-title">Đăng ký tài khoản</h1>
      <p class="auth-sub">Tạo tài khoản khách hàng để lưu trữ và theo dõi phiếu giảm giá</p>

      <form class="auth-form" @submit.prevent="submit">
        <div class="field">
          <label>Tên tài khoản</label>
          <input v-model="form.tenTaiKhoan" type="text" placeholder="nguyenvana" autocomplete="username" />
        </div>

        <div class="field">
          <label>Email</label>
          <input v-model="form.email" type="email" placeholder="user@example.com" autocomplete="email" />
        </div>

        <div class="field">
          <label>Số điện thoại</label>
          <input v-model="form.soDienThoai" type="tel" placeholder="0912345678" autocomplete="tel" />
        </div>

        <div class="field">
          <label>Mật khẩu</label>
          <input
            v-model="form.matKhau"
            type="password"
            placeholder="Tối thiểu 6 ký tự"
            autocomplete="new-password"
          />
        </div>

        <div class="field">
          <label>Xác nhận mật khẩu</label>
          <input
            v-model="form.xacNhanMatKhau"
            type="password"
            placeholder="Nhập lại mật khẩu"
            autocomplete="new-password"
          />
        </div>

        <p v-if="error" class="auth-error">{{ error }}</p>

        <button class="auth-btn" type="submit" :disabled="loading">
          {{ loading ? 'Đang đăng ký...' : 'Đăng ký' }}
        </button>
      </form>

      <p class="auth-switch">
        Đã có tài khoản?
        <router-link to="/dang-nhap">Đăng nhập</router-link>
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
  width: min(420px, 100%);
  background: var(--white);
  border-radius: 14px;
  padding: 26px 30px 24px;
  box-shadow: 0 10px 34px rgba(20, 20, 30, 0.08);
}

.auth-brand {
  display: flex;
  justify-content: center;
  margin-bottom: 6px;
}

.auth-brand img {
  width: 100px;
  height: 70px;
  object-fit: contain;
}

.auth-title {
  text-align: center;
  font-size: 19px;
  font-weight: 700;
  color: #22222a;
  margin: 0;
}

.auth-sub {
  text-align: center;
  font-size: 12.5px;
  color: #9a9aa3;
  margin: 6px 0 16px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  height: 39px;
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
  margin: 14px 0 0;
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
