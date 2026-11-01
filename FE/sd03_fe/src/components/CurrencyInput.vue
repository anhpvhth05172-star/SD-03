<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: [Number, String],
    default: ''
  },
  placeholder: {
    type: String,
    default: '0'
  },
  min: {
    type: Number,
    default: 0
  },
  max: {
    type: Number,
    default: 1000000000
  },
  disabled: {
    type: Boolean,
    default: false
  },
  hasError: {
    type: Boolean,
    default: false
  },
  customClass: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'blur', 'focus'])

const inputRef = ref(null)
const isFocused = ref(false)
let isSelectAllOnMouseUp = false

const formatNumberString = (val) => {
  if (val === null || val === undefined || val === '') return ''
  const num = typeof val === 'number' ? val : parseInt(String(val).replace(/[^\d]/g, ''), 10)
  if (isNaN(num)) return ''
  // Nếu giá trị là 0 và có placeholder tùy chỉnh (không phải '0'), trả về chuỗi rỗng để hiện placeholder rõ ràng
  if (num === 0 && props.placeholder && props.placeholder !== '0') {
    return ''
  }
  return new Intl.NumberFormat('vi-VN').format(num)
}

const displayValue = ref(formatNumberString(props.modelValue))

// Đồng bộ khi modelValue thay đổi từ bên ngoài (ví dụ bấm nút "Đủ tiền", chọn gợi ý tiền mặt, đổi tab hóa đơn)
watch(
  () => props.modelValue,
  (newVal) => {
    const currentNum = parseInt(String(displayValue.value).replace(/[^\d]/g, ''), 10) || 0
    const newNum = parseInt(String(newVal).replace(/[^\d]/g, ''), 10) || 0
    // Khi đang focus gõ và giá trị số học không đổi thì KHÔNG chạm vào DOM để tránh giật con trỏ hoặc nhảy số
    if (isFocused.value && currentNum === newNum) {
      return
    }
    const formatted = formatNumberString(newVal)
    displayValue.value = formatted
    if (inputRef.value) {
      inputRef.value.value = formatted
    }
  }
)

const handleMouseDown = () => {
  // Nếu input chưa được focus trước đó, đánh dấu để bôi đen toàn bộ sau khi mouseup
  if (!isFocused.value) {
    isSelectAllOnMouseUp = true
  }
}

const handleMouseUp = (e) => {
  if (isSelectAllOnMouseUp) {
    isSelectAllOnMouseUp = false
    e.preventDefault()
    if (inputRef.value) {
      inputRef.value.select()
    }
  }
}

const handleFocus = (e) => {
  isFocused.value = true
  emit('focus', e)
}

const handleClear = () => {
  displayValue.value = ''
  if (inputRef.value) {
    inputRef.value.value = ''
    inputRef.value.focus()
  }
  emit('update:modelValue', 0)
  emit('change', 0)
}

const handleKeyDown = (e) => {
  const el = inputRef.value
  if (!el) return

  // Khi người dùng bấm Backspace ngay sau dấu chấm phân tách (ví dụ "5.|000")
  // Dịch con trỏ sang trái 1 bước để xóa chữ số thực thay vì xóa dấu chấm
  if (e.key === 'Backspace') {
    const start = el.selectionStart
    const end = el.selectionEnd
    if (start === end && start > 0 && el.value[start - 1] === '.') {
      el.setSelectionRange(start - 1, start - 1)
    }
  }
}

const handleInput = (e) => {
  const inputEl = e.target
  const rawInput = inputEl.value || ''
  const cursorPos = inputEl.selectionStart || 0

  // 1. Đếm số lượng chữ số thực tế đứng trước con trỏ trước khi format lại
  const digitsBeforeCursor = rawInput.slice(0, cursorPos).replace(/[^\d]/g, '').length

  // 2. Lọc chỉ lấy các chữ số 0-9
  let digits = rawInput.replace(/[^\d]/g, '')

  if (!digits) {
    displayValue.value = ''
    inputEl.value = ''
    emit('update:modelValue', 0)
    emit('change', 0)
    return
  }

  // Giới hạn tối đa 12 chữ số để không bị tràn số
  if (digits.length > 12) {
    digits = digits.slice(0, 12)
  }

  let num = parseInt(digits, 10)
  if (isNaN(num)) num = 0

  if (props.max !== undefined && props.max !== null && num > props.max) {
    num = props.max
    digits = String(num)
  }

  // 3. Format chuẩn theo tiền tệ Việt Nam (vi-VN)
  const formatted = new Intl.NumberFormat('vi-VN').format(num)
  displayValue.value = formatted
  inputEl.value = formatted

  emit('update:modelValue', num)
  emit('change', num)

  // 4. Tính toán chính xác vị trí con trỏ mới:
  // Duyệt chuỗi formatted sao cho số chữ số đứng trước con trỏ bằng đúng digitsBeforeCursor
  let newCursorPos = formatted.length
  let count = 0
  for (let i = 0; i < formatted.length; i++) {
    if (/\d/.test(formatted[i])) {
      count++
    }
    if (count >= digitsBeforeCursor) {
      newCursorPos = i + 1
      break
    }
  }

  nextTick(() => {
    if (!inputEl || !isFocused.value) return
    inputEl.setSelectionRange(newCursorPos, newCursorPos)
  })
}

const handleBlur = (e) => {
  isFocused.value = false
  isSelectAllOnMouseUp = false
  const raw = String(displayValue.value).replace(/[^\d]/g, '')
  if (!raw) {
    if (props.min > 0) {
      displayValue.value = formatNumberString(props.min)
      if (inputRef.value) inputRef.value.value = displayValue.value
      emit('update:modelValue', props.min)
    } else {
      displayValue.value = ''
      if (inputRef.value) inputRef.value.value = ''
      emit('update:modelValue', 0)
    }
  } else {
    let num = parseInt(raw, 10)
    if (isNaN(num)) num = props.min || 0
    if (props.min !== undefined && props.min !== null && num < props.min) {
      num = props.min
    }
    if (props.max !== undefined && props.max !== null && num > props.max) {
      num = props.max
    }

    const formatted = formatNumberString(num)
    displayValue.value = formatted
    if (inputRef.value) inputRef.value.value = formatted
    emit('update:modelValue', num)
  }
  emit('blur', e)
}
</script>

<template>
  <div 
    class="currency-input-wrap"
    :class="[
      customClass,
      { 'is-disabled': disabled, 'has-error': hasError }
    ]"
  >
    <input 
      ref="inputRef"
      type="text" 
      class="currency-input"
      :value="displayValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @mousedown="handleMouseDown"
      @mouseup="handleMouseUp"
      @focus="handleFocus"
      @keydown="handleKeyDown"
      @input="handleInput"
      @blur="handleBlur"
    />
    <button 
      v-if="!disabled && displayValue" 
      type="button" 
      class="btn-clear-currency" 
      @click.stop="handleClear"
      title="Xóa số tiền"
      tabindex="-1"
    >
      ×
    </button>
    <span class="currency-suffix">₫</span>
  </div>
</template>

<style scoped>
.currency-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.currency-input {
  width: 100%;
  padding: 8px 48px 8px 12px;
  font-size: 14px;
  text-align: right;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  background-color: #ffffff;
  color: #0f172a;
  outline: none;
  font-family: inherit;
  transition: all 0.2s ease;
}

.currency-input:hover:not(:disabled) {
  border-color: #94a3b8;
}

/* Đổi viền focus sang màu Xanh chuẩn focus của form, không dùng màu đỏ tránh hiểu nhầm là lỗi */
.currency-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

/* Chỉ khi thực sự có lỗi validation (hasError = true) mới hiển thị viền đỏ */
.currency-input-wrap.has-error .currency-input,
.currency-input-wrap.has-error .currency-input:focus {
  border-color: #dc2626 !important;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15) !important;
  background-color: #fef2f2;
}

.currency-input-wrap.is-disabled .currency-input {
  background-color: #f1f5f9;
  cursor: not-allowed;
  opacity: 0.7;
}

.btn-clear-currency {
  position: absolute;
  right: 26px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #64748b;
  border: none;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  padding: 0;
  z-index: 2;
}

.btn-clear-currency:hover {
  background: #cbd5e1;
  color: #0f172a;
}

.currency-suffix {
  position: absolute;
  right: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  pointer-events: none;
  z-index: 1;
}
</style>
