<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { datLaiMatKhau, guiMaQuenMatKhau } from '@/api/auth'
import { kiemTra, matKhauHopLe } from '@/utils/validate'
import { notifySuccess, notifyError } from '@/utils/notify'

const router = useRouter()

const buoc = ref(1) // 1: nhập email, 2: nhập mã + mật khẩu mới
const email = ref('')
const maOTP = ref('')
const matKhauMoi = ref('')
const xacNhan = ref('')
const error = ref('')
const loading = ref(false)
const thongBao = ref('')

const guiMa = async () => {
  error.value = ''
  if (!email.value.trim()) {
    error.value = 'Email không được để trống'
    return
  }
  loading.value = true
  try {
    const resp = await guiMaQuenMatKhau(email.value.trim())
    thongBao.value = resp?.message || 'Đã gửi mã xác nhận đến email của bạn'
    buoc.value = 2
  } catch (e) {
    error.value = e.message || 'Không gửi được mã xác nhận'
  } finally {
    loading.value = false
  }
}

const guiLaiMa = async () => {
  error.value = ''
  loading.value = true
  try {
    const resp = await guiMaQuenMatKhau(email.value.trim())
    thongBao.value = resp?.message || 'Đã gửi lại mã xác nhận'
    notifySuccess('Đã gửi lại mã', 'Kiểm tra hộp thư email của bạn')
  } catch (e) {
    error.value = e.message || 'Không gửi được mã xác nhận'
    notifyError('Gửi lại thất bại', error.value)
  } finally {
    loading.value = false
  }
}

const datLai = async () => {
  error.value = ''
  const loi = kiemTra([
    !maOTP.value.trim() ? 'Mã xác nhận không được để trống' : '',
    matKhauHopLe(matKhauMoi.value, true),
    matKhauHopLe(xacNhan.value, true),
    matKhauMoi.value !== xacNhan.value ? 'Mật khẩu xác nhận không khớp' : '',
  ])
  if (loi) {
    error.value = loi
    return
  }
  loading.value = true
  try {
    const resp = await datLaiMatKhau({
      email: email.value.trim(),
      maOTP: maOTP.value.trim(),
      matKhauMoi: matKhauMoi.value,
      xacNhan: xacNhan.value,
    })
    notifySuccess('Đặt lại mật khẩu thành công', resp?.message || '')
    router.push('/dang-nhap')
  } catch (e) {
    error.value = e.message || 'Đặt lại mật khẩu thất bại'
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
      <h1 class="auth-title">Quên mật khẩu</h1>
      <p class="auth-sub">
        {{
          buoc === 1
            ? 'Nhập email đã đăng ký của bạn (nhân viên hoặc khách hàng) để nhận mã xác nhận'
            : 'Nhập mã xác nhận từ email và mật khẩu mới'
        }}
      </p>

      <form v-if="buoc === 1" class="auth-form" @submit.prevent="guiMa">
        <div class="field">
          <label>Email đã đăng ký</label>
          <input v-model="email" type="email" placeholder="ban@email.com" autocomplete="email" />
        </div>
        <p v-if="error" class="auth-error">{{ error }}</p>
        <button class="auth-btn" type="submit" :disabled="loading">
          {{ loading ? 'Đang gửi...' : 'Gửi mã xác nhận' }}
        </button>
      </form>

      <form v-else class="auth-form" @submit.prevent="datLai">
        <p class="otp-sent">
          <i class="bi bi-envelope-check"></i> {{ thongBao }}
        </p>
        <div class="field">
          <label>Mã xác nhận (6 số)</label>
          <input
            v-model="maOTP"
            type="text"
            inputmode="numeric"
            maxlength="6"
            placeholder="Nhập mã 6 số"
            autocomplete="one-time-code"
            class="otp-input"
          />
        </div>
        <div class="field">
          <label>Mật khẩu mới</label>
          <input
            v-model="matKhauMoi"
            type="password"
            placeholder="Tối thiểu 6 ký tự"
            autocomplete="new-password"
          />
        </div>
        <div class="field">
          <label>Xác nhận mật khẩu mới</label>
          <input
            v-model="xacNhan"
            type="password"
            placeholder="Nhập lại mật khẩu mới"
            autocomplete="new-password"
          />
        </div>
        <p v-if="error" class="auth-error">{{ error }}</p>
        <button class="auth-btn" type="submit" :disabled="loading">
          {{ loading ? 'Đang xử lý...' : 'Đặt lại mật khẩu' }}
        </button>
        <button class="auth-btn secondary" type="button" :disabled="loading" @click="guiLaiMa">
          Gửi lại mã
        </button>
      </form>

      <p class="auth-switch">
        <router-link to="/dang-nhap">Quay lại đăng nhập</router-link>
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
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);
  padding: 24px;
}
.auth-card {
  width: 420px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 32px 28px;
  box-shadow: 0 16px 48px rgba(15, 23, 42, 0.12);
  text-align: center;
}
.auth-brand img {
  max-height: 52px;
  margin-bottom: 8px;
}
.auth-title {
  margin: 8px 0 4px;
  font-size: 22px;
  color: #0f172a;
}
.auth-sub {
  margin: 0 0 20px;
  font-size: 13.5px;
  color: #64748b;
}
.auth-form {
  text-align: left;
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
.field input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}
.otp-input {
  letter-spacing: 6px;
  font-size: 18px;
  text-align: center;
}
.otp-sent {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  margin: 0 0 14px;
}
.auth-error {
  color: #dc2626;
  font-size: 13px;
  margin: 0 0 12px;
}
.auth-btn {
  width: 100%;
  padding: 11px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}
.auth-btn.secondary {
  background: #f1f5f9;
  color: #475569;
  margin-top: 8px;
}
.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-switch {
  margin: 14px 0 0;
  font-size: 13px;
  color: #64748b;
}
.auth-switch a {
  color: #2563eb;
  text-decoration: none;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
}
.auth-switch a:hover {
  text-decoration: underline;
}
</style>
