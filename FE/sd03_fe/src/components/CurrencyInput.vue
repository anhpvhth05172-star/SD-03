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

const formatNumberString = (val) => {
  if (val === null || val === undefined || val === '') return ''
  const num = typeof val === 'number' ? val : parseInt(String(val).replace(/[^\d]/g, ''), 10)
  if (isNaN(num)) return ''
  return new Intl.NumberFormat('vi-VN').format(num)
}

const displayValue = ref(formatNumberString(props.modelValue))

watch(
  () => props.modelValue,
  (newVal) => {
    // Nếu đang focus thì chỉ cập nhật khi giá trị số từ ngoài khác hẳn giá trị người dùng đang gõ
    if (isFocused.value) {
      const currentDigits = String(displayValue.value).replace(/[^\d]/g, '')
      const newDigits = newVal !== null && newVal !== undefined && newVal !== '' ? String(newVal).replace(/[^\d]/g, '') : ''
      const currentNum = currentDigits ? parseInt(currentDigits, 10) : NaN
      const newNum = newDigits ? parseInt(newDigits, 10) : NaN
      if (isNaN(currentNum) && isNaN(newNum)) return
      if (currentNum === newNum) return
    }
    const formatted = formatNumberString(newVal)
    displayValue.value = formatted
    if (inputRef.value) {
      inputRef.value.value = formatted
    }
  }
)

const handleFocus = (e) => {
  isFocused.value = true
  emit('focus', e)
  // Tự động bôi đen toàn bộ số để người dùng gõ số mới sẽ ghi đè ngay, không bị dính số cũ
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.select()
    }
  })
}

const handleInput = (e) => {
  const inputEl = e.target
  const rawInput = inputEl.value
  const oldCursorPos = inputEl.selectionStart || 0

  // Đếm số lượng chữ số nằm TRƯỚC con trỏ trước khi format lại
  const digitsBeforeCursor = rawInput.slice(0, oldCursorPos).replace(/[^\d]/g, '').length

  // Lọc chỉ lấy các chữ số
  let digits = rawInput.replace(/[^\d]/g, '')

  if (!digits) {
    displayValue.value = ''
    inputEl.value = ''
    emit('update:modelValue', '')
    emit('change', '')
    return
  }

  // Giới hạn tối đa 12 chữ số để tránh overflow
  if (digits.length > 12) {
    digits = digits.slice(0, 12)
  }

  let num = parseInt(digits, 10)
  if (isNaN(num)) num = 0

  const formatted = new Intl.NumberFormat('vi-VN').format(num)
  displayValue.value = formatted
  // Luôn đồng bộ DOM trực tiếp để không bao giờ bị lệch ký tự
  inputEl.value = formatted

  emit('update:modelValue', num)
  emit('change', num)

  // Khôi phục con trỏ chuột chính xác sau khi cập nhật
  nextTick(() => {
    if (!inputEl || !isFocused.value) return
    let newCursorPos = 0
    let count = 0
    for (let i = 0; i < formatted.length; i++) {
      if (/\d/.test(formatted[i])) {
        count++
      }
      if (count === digitsBeforeCursor) {
        newCursorPos = i + 1
        break
      }
    }
    if (digitsBeforeCursor === 0) newCursorPos = 0
    if (count < digitsBeforeCursor) newCursorPos = formatted.length

    inputEl.setSelectionRange(newCursorPos, newCursorPos)
  })
}

const handleBlur = (e) => {
  isFocused.value = false
  const raw = String(displayValue.value).replace(/[^\d]/g, '')
  if (!raw) {
    if (props.min > 0) {
      displayValue.value = formatNumberString(props.min)
      if (inputRef.value) inputRef.value.value = displayValue.value
      emit('update:modelValue', props.min)
    } else {
      displayValue.value = ''
      if (inputRef.value) inputRef.value.value = ''
      emit('update:modelValue', '')
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
      @focus="handleFocus"
      @input="handleInput"
      @blur="handleBlur"
    />
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
  padding: 8px 32px 8px 12px;
  font-size: 14px;
  text-align: right;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background-color: #ffffff;
  color: #111827;
  outline: none;
  font-family: inherit;
  transition: all 0.2s ease;
}

.currency-input:hover:not(:disabled) {
  border-color: #9ca3af;
}

.currency-input:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15);
}

.currency-input-wrap.has-error .currency-input {
  border-color: #dc2626;
  background-color: #fef2f2;
}

.currency-input-wrap.is-disabled .currency-input {
  background-color: #f3f4f6;
  cursor: not-allowed;
  opacity: 0.7;
}

.currency-suffix {
  position: absolute;
  right: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  pointer-events: none;
}
</style>
