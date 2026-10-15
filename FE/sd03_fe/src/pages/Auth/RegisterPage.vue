<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { dangKy } from '@/api/auth'

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
  if (!f.soDienThoai.trim()) {
    error.value = 'Số điện thoại không được để trống'
    return
  }
  if (!f.matKhau || f.matKhau.length < 6) {
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
      <h1 class="auth-title">Đăng ký</h1>
      <p class="auth-sub">Tạo tài khoản khách hàng để theo dõi đơn và phiếu giảm giá</p>

      <form class="auth-form" @submit.prevent="submit">
        <div class="field">
          <label>Tài khoản</label>
          <input v-model="form.tenTaiKhoan" type="text" placeholder="Tên tài khoản" />
        </div>
        <div class="field">
          <label>Email</label>
          <input v-model="form.email" type="email" placeholder="user@example.com" />
        </div>
        <div class="field">
          <label>Số điện thoại</label>
          <input v-model="form.soDienThoai" type="tel" placeholder="09xxxxxxxx" />
        </div>
        <div class="field">
          <label>Mật khẩu</label>
          <input v-model="form.matKhau" type="password" placeholder="Tối thiểu 6 ký tự" />
        </div>
        <div class="field">
          <label>Xác nhận mật khẩu</label>
          <input v-model="form.xacNhanMatKhau" type="password" placeholder="Nhập lại mật khẩu" />
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
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  padding: 24px;
}
.auth-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35);
}
.auth-brand {
  text-align: center;
  margin-bottom: 12px;
}
.auth-brand img {
  height: 48px;
}
.auth-title {
  text-align: center;
  margin: 0 0 4px;
  font-size: 24px;
  color: #0f172a;
}
.auth-sub {
  text-align: center;
  color: #64748b;
  font-size: 14px;
  margin: 0 0 20px;
}
.field {
  margin-bottom: 14px;
}
.field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}
.field input {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
.auth-error {
  color: #dc2626;
  font-size: 13px;
  margin: 0 0 12px;
}
.auth-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
}
.auth-btn:disabled {
  opacity: 0.6;
}
.auth-switch {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin: 16px 0 0;
}
.auth-switch a {
  color: #2563eb;
  font-weight: 600;
}
</style>
