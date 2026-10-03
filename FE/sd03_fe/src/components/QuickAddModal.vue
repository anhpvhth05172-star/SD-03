<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  isOpen: Boolean,
  type: {
    type: String, // 'color' or 'size'
    default: 'color'
  }
})

const emit = defineEmits(['close', 'add'])

const itemName = ref('')
const itemCode = ref('#d92d20')
const itemDescription = ref('')

const title = computed(() => {
  return props.type === 'color' ? 'Thêm nhanh Màu sắc' : 'Thêm nhanh Kích cỡ'
})

const labelName = computed(() => {
  return props.type === 'color' ? 'Tên màu sắc' : 'Kích cỡ (Size)'
})

const placeholderName = computed(() => {
  return props.type === 'color' ? 'Ví dụ: Xanh rêu, Đỏ đô...' : 'Ví dụ: 39.5, 44, 45...'
})

const handleClose = () => {
  itemName.value = ''
  itemDescription.value = ''
  emit('close')
}

const handleSave = () => {
  if (!itemName.value.trim()) return
  emit('add', {
    type: props.type,
    name: itemName.value.trim(),
    code: props.type === 'color' ? itemCode.value : null,
    description: itemDescription.value.trim()
  })
  handleClose()
}
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="handleClose">
    <div class="modal-card">
      <div class="modal-header">
        <h3 class="modal-title">{{ title }}</h3>
        <button class="btn-close" @click="handleClose">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <div class="modal-body">
        <div class="form-group mb-3">
          <label class="form-label">{{ labelName }} <span class="required-star">*</span></label>
          <input
            v-model="itemName"
            type="text"
            class="form-input"
            :placeholder="placeholderName"
            autofocus
            @keyup.enter="handleSave"
          />
        </div>

        <div v-if="type === 'color'" class="form-group mb-3">
          <label class="form-label">Mã màu đại diện</label>
          <div class="color-picker-row">
            <input v-model="itemCode" type="color" class="color-box-picker" />
            <input v-model="itemCode" type="text" class="form-input" placeholder="#HEX" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Mô tả (tùy chọn)</label>
          <input
            v-model="itemDescription"
            type="text"
            class="form-input"
            placeholder="Ghi chú thêm..."
          />
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-secondary" @click="handleClose">Hủy bỏ</button>
        <button class="btn btn-primary" :disabled="!itemName.trim()" @click="handleSave">
          + Thêm thuộc tính
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 16px;
  animation: fadeIn 0.15s ease-out;
}

.modal-card {
  background: #ffffff;
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 440px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  animation: scaleUp 0.15s ease-out;
}

.modal-header {
  padding: 18px 20px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.btn-close {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
}
.btn-close:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.modal-body {
  padding: 20px;
}

.mb-3 {
  margin-bottom: 16px;
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.color-box-picker {
  width: 42px;
  height: 42px;
  border: 1px solid var(--border-input);
  border-radius: var(--radius-md);
  cursor: pointer;
  padding: 2px;
}

.modal-footer {
  padding: 14px 20px;
  background-color: #f8fafc;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes scaleUp {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
