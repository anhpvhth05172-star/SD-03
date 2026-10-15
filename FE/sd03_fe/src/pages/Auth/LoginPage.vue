<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { dangNhap, dangNhapNhanVien } from '@/api/auth'
import { luuPhien } from '@/utils/auth'
import { datVaiTro, datNguoiDung } from '@/utils/session'

const router = useRouter()

const cheDo = ref('nhan-vien') // 'nhan-vien' | 'khach'
const form = ref({ taiKhoan: '', matKhau: '' })
const error = ref('')
const loading = ref(false)

const chonCheDo = (mode) => {
  cheDo.value = mode
  error.value = ''
}

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
    }
    const resp =
      cheDo.value === 'nhan-vien' ? await dangNhapNhanVien(payload) : await dangNhap(payload)
    luuPhien(resp)
    datVaiTro(resp.vaiTro)
    datNguoiDung(resp.tenTaiKhoan)
    if (cheDo.value === 'nhan-vien') {
      router.push('/hoa-don')
    } else {
      router.push('/khach-hang')
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
      <h1 class="auth-title">Đăng nhập</h1>
      <p class="auth-sub">
        {{
          cheDo === 'nhan-vien'
            ? 'Đăng nhập nhân viên để quản lý cửa hàng'
            : 'Đăng nhập để sử dụng phiếu giảm giá theo tài khoản của bạn'
        }}
      </p>

      <div class="auth-tabs">
        <button
          type="button"
          class="tab"
          :class="{ active: cheDo === 'nhan-vien' }"
          @click="chonCheDo('nhan-vien')"
        >
          Nhân viên
        </button>
        <button
          type="button"
          class="tab"
          :class="{ active: cheDo === 'khach' }"
          @click="chonCheDo('khach')"
        >
          Khách hàng
        </button>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <div class="field">
          <label>Tài khoản</label>
          <input
            v-model="form.taiKhoan"
            type="text"
            placeholder="Nhập tên tài khoản hoặc email"
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

      <p v-if="cheDo === 'khach'" class="auth-switch">
        Chưa có tài khoản?
        <router-link to="/dang-ky">Đăng ký</router-link>
      </p>
      <p class="auth-switch">
        <router-link to="/san-pham">Về trang quản lý</router-link>
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
.auth-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 4px;
}
.auth-tabs .tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px;
  border-radius: 8px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
}
.auth-tabs .tab.active {
  background: #fff;
  color: #0f172a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.field {
  margin-bottom: 16px;
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
.field input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
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
  cursor: not-allowed;
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
