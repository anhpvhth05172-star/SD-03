<script setup>
import { ref } from 'vue'
import { changeCustomerPassword } from '@/api/customer'
import { changeEmployeePassword } from '@/api/employee'
import { kiemTra, matKhauHopLe } from '@/utils/validate'
import { notifyError, notifySuccess } from '@/utils/notify'

const props = defineProps({
  loai: { type: String, required: true },
  id: { type: [Number, String], required: true },
  tenTaiKhoan: { type: String, default: '' },
})
const emit = defineEmits(['close', 'success'])

const matKhauMoi = ref('')
const xacNhan = ref('')
const errorMsg = ref('')
const saving = ref(false)

const dong = () => {
  if (saving.value) return
  emit('close')
}

const luu = async () => {
  const loi = kiemTra([
    matKhauHopLe(matKhauMoi.value, true),
    matKhauHopLe(xacNhan.value, true),
    matKhauMoi.value !== xacNhan.value ? 'Mật khẩu xác nhận không khớp' : '',
  ])
  if (loi) {
    errorMsg.value = loi
    return
  }
  saving.value = true
  errorMsg.value = ''
  try {
    if (props.loai === 'NV') {
      await changeEmployeePassword(props.id, matKhauMoi.value, xacNhan.value)
    } else {
      await changeCustomerPassword(props.id, matKhauMoi.value, xacNhan.value)
    }
    notifySuccess('Đổi mật khẩu thành công', props.tenTaiKhoan ? `Tài khoản ${props.tenTaiKhoan}` : '')
    emit('success')
    emit('close')
  } catch (e) {
    errorMsg.value = e.message || 'Không thể đổi mật khẩu'
    notifyError('Đổi mật khẩu thất bại', errorMsg.value)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="dong">
    <div class="doi-mk-card">
      <div class="doi-mk-header">
        <h3><i class="bi bi-key"></i> Đổi mật khẩu</h3>
        <button type="button" class="doi-mk-x" :disabled="saving" @click="dong">&times;</button>
      </div>
      <p v-if="tenTaiKhoan" class="doi-mk-sub">Tài khoản: <b>{{ tenTaiKhoan }}</b></p>
      <form @submit.prevent="luu">
        <label class="doi-mk-label">Mật khẩu mới</label>
        <input
          v-model="matKhauMoi"
          type="password"
          class="doi-mk-input"
          placeholder="Tối thiểu 6 ký tự"
          autocomplete="new-password"
          :disabled="saving"
        />
        <label class="doi-mk-label">Xác nhận mật khẩu mới</label>
        <input
          v-model="xacNhan"
          type="password"
          class="doi-mk-input"
          placeholder="Nhập lại mật khẩu mới"
          autocomplete="new-password"
          :disabled="saving"
        />
        <p v-if="errorMsg" class="doi-mk-error"><i class="bi bi-exclamation-circle"></i> {{ errorMsg }}</p>
        <div class="doi-mk-actions">
          <button type="button" class="doi-mk-btn cancel" :disabled="saving" @click="dong">Hủy</button>
          <button type="submit" class="doi-mk-btn primary" :disabled="saving">
            {{ saving ? 'Đang lưu...' : 'Lưu mật khẩu mới' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.doi-mk-card {
  width: 400px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 14px;
  padding: 22px 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}
.doi-mk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.doi-mk-header h3 {
  margin: 0;
  font-size: 17px;
  color: #0f172a;
}
.doi-mk-x {
  border: none;
  background: none;
  font-size: 22px;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
}
.doi-mk-sub {
  margin: 4px 0 14px;
  font-size: 13px;
  color: #64748b;
}
.doi-mk-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin: 10px 0 4px;
}
.doi-mk-input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
.doi-mk-input:focus {
  outline: 2px solid #2563eb;
  border-color: transparent;
}
.doi-mk-error {
  margin: 10px 0 0;
  font-size: 13px;
  color: #dc2626;
}
.doi-mk-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
.doi-mk-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.doi-mk-btn.cancel {
  background: #f1f5f9;
  color: #475569;
}
.doi-mk-btn.primary {
  background: #2563eb;
  color: #fff;
}
.doi-mk-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
