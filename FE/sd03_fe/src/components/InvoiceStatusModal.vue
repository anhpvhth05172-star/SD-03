<script setup>
import { computed, ref } from 'vue'
import { updateInvoiceStatus } from '@/api/invoice'
import {
  TRANG_THAI_ONLINE,
  TRANG_THAI_POS,
  trangThaiTiepTheoOf,
  laPos,
} from '@/constants/invoiceStatus'
import { notifySuccess, notifyError } from '@/utils/notify'

const props = defineProps({
  invoice: { type: Object, required: true },
})
const emit = defineEmits(['close', 'changed'])

const saving = ref(false)
const errorMsg = ref('')
const chon = ref(null)
const ghiChu = ref('')

const laDonPos = computed(() => laPos(props.invoice))

const progressSteps = computed(() => {
  const list = laDonPos.value ? TRANG_THAI_POS : TRANG_THAI_ONLINE
  return list.map((ten, i) => {
    const tt = props.invoice?.trangThai
    return {
      ten,
      laHienTai: ten === tt,
      laDaQua: list.indexOf(tt) > -1 && i < list.indexOf(tt),
      laTiepTheo: trangThaiTiepTheoOf(props.invoice).includes(ten),
    }
  })
})

const tiepTheoHopLe = computed(() => {
  const all = trangThaiTiepTheoOf(props.invoice)
  const steps = progressSteps.value.filter((s) => s.laTiepTheo).map((s) => s.ten)
  return all.filter((t) => steps.includes(t))
})

const coTheChinh = computed(() => tiepTheoHopLe.value.length > 0)

const chonTrangThai = (ten) => {
  chon.value = ten
  errorMsg.value = ''
}

const huy = () => {
  if (!saving.value) emit('close')
}

const xacNhan = async () => {
  if (!chon.value) {
    errorMsg.value = 'Vui lòng chọn trạng thái kế tiếp'
    return
  }
  saving.value = true
  errorMsg.value = ''
  try {
    await updateInvoiceStatus(props.invoice.id, chon.value, ghiChu.value.trim())
    notifySuccess('Đã cập nhật trạng thái đơn hàng!')
    emit('changed')
    emit('close')
  } catch (e) {
    errorMsg.value = e.message || 'Không thể cập nhật trạng thái'
    notifyError('Cập nhật thất bại', errorMsg.value)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="modal-mask" @click.self="huy">
    <div class="modal-card">
      <div class="modal-head">
        <h3>Chỉnh đơn hàng</h3>
        <button class="modal-close" type="button" aria-label="Đóng" @click="huy">×</button>
      </div>

      <div class="status-current">
        <div class="status-meta">
          <span class="status-ma">{{ invoice.maHoaDon || '—' }}</span>
          <span class="status-label">Trạng thái hiện tại</span>
          <span class="status-value">{{ invoice.trangThai }}</span>
        </div>
        <span v-if="laDonPos" class="status-badge">Tại quầy</span>
      </div>

      <div class="progress" aria-label="Tiến trình đơn hàng">
        <div
          v-for="(s, i) in progressSteps"
          :key="s.ten"
          class="progress-step"
          :class="{
            'is-done': s.laDaQua,
            'is-current': s.laHienTai,
            'is-next': s.laTiepTheo,
          }"
        >
          <span class="progress-dot"></span>
          <span class="progress-label">{{ s.ten }}</span>
          <span v-if="i < progressSteps.length - 1" class="progress-line"></span>
        </div>
      </div>

      <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

      <div v-if="coTheChinh" class="status-options">
        <p class="options-hint">Chỉ được chuyển sang bước kế tiếp:</p>
        <button
          v-for="t in tiepTheoHopLe"
          :key="t"
          type="button"
          class="status-option"
          :class="{ 'is-active': chon === t }"
          :disabled="saving"
          @click="chonTrangThai(t)"
        >
          {{ t }}
        </button>
      </div>
      <p v-else class="empty">Đơn hàng đã ở trạng thái cuối, không thể chỉnh sửa thêm.</p>

      <div v-if="coTheChinh" class="field">
        <label class="field-label">Ghi chú (tùy chọn)</label>
        <textarea
          v-model="ghiChu"
          maxlength="500"
          rows="2"
          placeholder="Lý do đổi trạng thái..."
          :disabled="saving"
        ></textarea>
        <span class="field-count">{{ ghiChu.length }}/500</span>
      </div>

      <div class="modal-actions">
        <button
          class="btn-primary"
          type="button"
          :disabled="saving || !chon"
          @click="xacNhan"
        >
          {{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}
        </button>
        <button class="btn-cancel" type="button" :disabled="saving" @click="huy">Hủy</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(17, 17, 20, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 60;
  padding: 20px;
}

.modal-card {
  width: min(460px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: #fff;
  border-radius: 14px;
  padding: 22px 24px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.modal-head h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1d1d23;
}

.modal-close {
  border: none;
  background: #f2f2f5;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 19px;
  line-height: 1;
  cursor: pointer;
  color: #4a4a52;
}

.status-current {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  background: #f8f8fa;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 14px;
}

.status-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-ma {
  font-size: 12px;
  font-weight: 700;
  color: #dc2626;
}

.status-label {
  font-size: 11.5px;
  color: #6b7280;
}

.status-value {
  font-size: 13px;
  font-weight: 700;
  color: #1d1d23;
}

.status-badge {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #dc2626;
  background: #fdecec;
  border-radius: 6px;
  padding: 3px 8px;
  margin-top: 2px;
}

.progress {
  display: flex;
  align-items: flex-start;
  gap: 0;
  margin-bottom: 16px;
  padding: 12px 4px 4px;
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  min-width: 0;
  position: relative;
}

.progress-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e5e7eb;
  border: 2px solid #e5e7eb;
  margin-bottom: 6px;
  z-index: 1;
}

.progress-step.is-done .progress-dot {
  background: #dc2626;
  border-color: #dc2626;
}

.progress-step.is-current .progress-dot {
  background: #fff;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15);
}

.progress-step.is-next .progress-dot {
  background: #fef2f2;
  border-color: #fca5a5;
}

.progress-label {
  font-size: 9.5px;
  font-weight: 600;
  color: #9ca3af;
  text-align: center;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-step.is-done .progress-label,
.progress-step.is-current .progress-label {
  color: #1d1d23;
}

.progress-step.is-next .progress-label {
  color: #dc2626;
}

.progress-line {
  position: absolute;
  top: 5px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: #e5e7eb;
  z-index: 0;
}

.progress-step.is-done .progress-line {
  background: #dc2626;
}

.progress-step:last-child .progress-line {
  display: none;
}

.form-error {
  background: #fdecec;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 7px;
  padding: 9px 12px;
  margin-bottom: 12px;
}

.options-hint {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.status-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.status-option {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #3d3d45;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.status-option:hover:not(:disabled) {
  border-color: #dc2626;
}

.status-option.is-active {
  border-color: #dc2626;
  background: #fef2f2;
  color: #dc2626;
}

.status-option:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.empty {
  font-size: 12.5px;
  color: #6b7280;
  margin-bottom: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 14px;
  position: relative;
}

.field-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #3d3d45;
}

.field textarea {
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  padding: 9px 11px;
  font-size: 13px;
  font-family: inherit;
  resize: vertical;
}

.field textarea:focus {
  outline: none;
  border-color: #dc2626;
}

.field textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field-count {
  align-self: flex-end;
  font-size: 11px;
  color: #9ca3af;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn-primary {
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 7px;
  background: #dc2626;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-cancel {
  height: 36px;
  padding: 0 18px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #4a4a52;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
