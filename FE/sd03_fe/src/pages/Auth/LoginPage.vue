<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dangNhap, dangNhapNhanVien } from '@/api/auth'
import { lienKetSauDangNhap } from '@/api/chat'
import { luuPhien, getMaAnDanh } from '@/utils/auth'
import { datVaiTro, datNguoiDung } from '@/utils/session'

const router = useRouter()
const route = useRoute()

const cheDo = ref(route.query.cheDo === 'khach' ? 'khach' : 'nhan-vien') // 'nhan-vien' | 'khach'
const form = ref({ taiKhoan: '', matKhau: '', ghiNhoDangNhap: false })
const error = ref('')
const loading = ref(false)
const hienMatKhau = ref(false)

const submit = async () => {
  error.value = ''
  if (!form.value.taiKhoan.trim() || !form.value.matKhau) {
    error.value = 'Vui lòng nhập tài khoản và mật khẩu'
    return
  }
  loading.value = true
  try {
    const payload = {
      taiKhoan: form.value.taiKhoan.trim(),
      matKhau: form.value.matKhau,
      ghiNhoDangNhap: form.value.ghiNhoDangNhap,
    }
    const resp =
      cheDo.value === 'nhan-vien' ? await dangNhapNhanVien(payload) : await dangNhap(payload)
    luuPhien(resp)
    datVaiTro(resp.vaiTro)
    datNguoiDung(resp.tenTaiKhoan)
    const laTaiKhoanNhanVien = resp.loaiTaiKhoan === 'NHAN_VIEN' || resp.idNhanVien != null
    if (laTaiKhoanNhanVien) {
      router.push('/hoa-don')
    } else {
      // Gắn cuộc trò chuyện ẩn danh (nếu có) vào tài khoản để không mất hội thoại cũ
      lienKetSauDangNhap(getMaAnDanh()).catch(() => {})
      router.push('/tai-khoan')
    }
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
      <h1 class="auth-title">HỆ THỐNG QUẢN LÝ BÁN HÀNG</h1>

      <form class="auth-form" @submit.prevent="submit">
        <div class="field">
          <label for="login-account">Tài khoản / Email</label>
          <div class="input-wrap">
            <i class="bi bi-person" aria-hidden="true"></i>
            <input id="login-account" v-model="form.taiKhoan" type="text"
              placeholder="Nhập tài khoản hoặc email" autocomplete="username" />
          </div>
        </div>

        <div class="field">
          <div class="field-heading">
            <label for="login-password">Mật khẩu</label>
            <router-link to="/quen-mat-khau" class="auth-forgot">
              Quên mật khẩu?
            </router-link>
          </div>
          <div class="input-wrap">
            <i class="bi bi-lock" aria-hidden="true"></i>
            <input id="login-password" v-model="form.matKhau" :type="hienMatKhau ? 'text' : 'password'"
              placeholder="Nhập mật khẩu" autocomplete="current-password" />
            <button class="password-toggle" type="button" :aria-label="hienMatKhau ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
              @click="hienMatKhau = !hienMatKhau">
              <i :class="hienMatKhau ? 'bi bi-eye-slash' : 'bi bi-eye'" aria-hidden="true"></i>
            </button>
          </div>
        </div>

        <p v-if="error" class="auth-error">{{ error }}</p>

        <button class="auth-btn" type="submit" :disabled="loading">
          {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập vào hệ thống' }}
        </button>
      </form>

      <p class="auth-footer">© 2026 FF T-Shirt Fashion Store. Hệ thống quản lý bán hàng.</p>
      <p v-if="cheDo === 'khach'" class="auth-customer-links">
        Chưa có tài khoản? <router-link to="/dang-ky">Đăng ký</router-link>
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
  box-sizing: border-box;
  background: #faf9f5;
  padding: 20px;
}
.auth-card {
  width: 100%;
  max-width: 440px;
  box-sizing: border-box;
  background: #fffefa;
  border: 1px solid #eee9df;
  border-radius: 16px;
  padding: 34px 40px 30px;
  box-shadow: 0 8px 28px rgba(66, 54, 35, 0.1);
}
.auth-brand {
  text-align: center;
  margin-bottom: 12px;
}
.auth-brand img {
  width: 140px;
  height: 72px;
  object-fit: contain;
}
.auth-title {
  text-align: center;
  margin: 0 0 24px;
  font-size: 21px;
  line-height: 1.4;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #293c49;
}
.field {
  margin-bottom: 18px;
}
.field label {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #4c5b62;
  margin-bottom: 6px;
}
.field-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.field-heading label { margin-bottom: 6px; }
.input-wrap {
  display: flex;
  align-items: center;
  height: 46px;
  box-sizing: border-box;
  padding: 0 14px;
  border: 1px solid #e5e4df;
  border-radius: 8px;
  background: #fff;
  color: #87939a;
}
.input-wrap:focus-within {
  border-color: #66849a;
  box-shadow: 0 0 0 3px rgba(71, 104, 126, 0.12);
}
.input-wrap > i { font-size: 17px; }
.input-wrap input {
  width: 100%;
  min-width: 0;
  height: 100%;
  padding: 0 10px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #334155;
  font: inherit;
  font-size: 15px;
  box-sizing: border-box;
}
.input-wrap input::placeholder { color: #9aa3a8; }
.password-toggle {
  display: grid;
  place-items: center;
  border: 0;
  padding: 4px;
  background: transparent;
  color: #8a969c;
  cursor: pointer;
}
.password-toggle i { font-size: 17px; }
.auth-forgot {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #58768a;
  text-decoration: none;
}
.auth-forgot:hover { text-decoration: underline; }
.auth-error {
  color: #dc2626;
  font-size: 14px;
  margin: 0 0 12px;
}
.auth-btn {
  width: 100%;
  min-height: 48px;
  margin-top: 2px;
  padding: 12px 16px;
  border: none;
  border-radius: 8px;
  background: #45677f;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 3px 8px rgba(46, 71, 88, 0.2);
}
.auth-btn:hover:not(:disabled) { background: #38586f; }
.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-footer {
  text-align: center;
  font-size: 12px;
  color: #92989a;
  line-height: 1.5;
  margin: 20px 0 0;
}
.auth-customer-links {
  text-align: center;
  font-size: 15px;
  color: #64748b;
  margin: 16px 0 0;
}
.auth-customer-links a { color: #45677f; font-weight: 700; }
@media (max-width: 480px) { .auth-card { max-width: 100%; padding: 24px 20px 20px; } }
</style>
