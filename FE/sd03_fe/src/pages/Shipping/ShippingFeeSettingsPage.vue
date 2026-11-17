<script setup>
import { onMounted, ref } from 'vue'
import { getFreeshipConfig, updateFreeshipConfig } from '@/api/shipping'
import { formatVnd } from '@/utils/format'
import { notifyError, notifySuccess } from '@/utils/notify'

const loading = ref(false)
const saving = ref(false)
const freeShipThreshold = ref(10000000)

const loadConfig = async () => {
  loading.value = true
  try {
    const res = await getFreeshipConfig()
    if (res?.freeShipThreshold != null) {
      freeShipThreshold.value = Number(res.freeShipThreshold)
    }
  } catch (e) {
    notifyError('Không tải được cấu hình', e?.message || 'Vui lòng thử lại')
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  const val = Number(freeShipThreshold.value)
  if (!Number.isFinite(val) || val < 0) {
    notifyError('Giá trị không hợp lệ', 'Mốc miễn phí vận chuyển phải lớn hơn hoặc bằng 0')
    return
  }
  if (val > 1000000000) {
    notifyError('Giá trị quá lớn', 'Mốc miễn phí vận chuyển không được vượt quá 1.000.000.000 ₫')
    return
  }
  saving.value = true
  try {
    const res = await updateFreeshipConfig(val)
    if (res?.freeShipThreshold != null) {
      freeShipThreshold.value = Number(res.freeShipThreshold)
    }
    notifySuccess('Đã lưu cài đặt phí ship', val > 0 ? `Đơn từ ${formatVnd(val)} sẽ được miễn phí vận chuyển` : 'Đã tắt chính sách miễn phí vận chuyển')
  } catch (e) {
    notifyError('Lưu thất bại', e?.message || 'Không có quyền hoặc dữ liệu không hợp lệ')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<template>
  <div class="container-fluid py-3">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div>
        <h4 class="fw-bold mb-1">Cài đặt phí vận chuyển (Freeship)</h4>
        <p class="text-muted small mb-0">
          Cấu hình giá trị đơn hàng tối thiểu để tự động miễn phí vận chuyển cho đơn Online và đơn Giao hàng tại quầy.
        </p>
      </div>
    </div>

    <div class="card border-0 shadow-sm" style="max-width: 640px;">
      <div class="card-body p-4">
        <div v-if="loading" class="text-muted py-3">Đang tải cấu hình...</div>
        <form v-else @submit.prevent="saveConfig">
          <div class="mb-3">
            <label for="freeship-threshold" class="form-label fw-semibold">
              Mốc đơn hàng miễn phí vận chuyển (VNĐ)
            </label>
            <input
              id="freeship-threshold"
              v-model.number="freeShipThreshold"
              type="number"
              min="0"
              max="1000000000"
              step="10000"
              class="form-control"
              placeholder="Ví dụ: 10000000"
            />
            <div class="form-text mt-2">
              <span v-if="freeShipThreshold > 0">
                Hiện tại: Đơn hàng có tổng tiền hàng từ <strong>{{ formatVnd(freeShipThreshold) }}</strong> trở lên sẽ được miễn phí ship.
              </span>
              <span v-else class="text-warning">
                Nhập <strong>0</strong> để tắt tự động miễn phí vận chuyển.
              </span>
            </div>
          </div>

          <div class="d-flex gap-2 justify-content-end">
            <button type="button" class="btn btn-outline-secondary" :disabled="saving" @click="loadConfig">
              Tải lại
            </button>
            <button type="submit" class="btn btn-primary px-4" :disabled="saving">
              {{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
