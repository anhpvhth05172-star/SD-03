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
  align: {
    type: String,
    default: 'left' // 'left' hoặc 'right'
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
let isComposing = false

const formatNumberString = (val) => {
  if (val === null || val === undefined || val === '') return ''
  const num = typeof val === 'number' ? val : parseInt(String(val).replace(/[^\d]/g, ''), 10)
  if (isNaN(num)) return ''
  if (num === 0 && props.placeholder && props.placeholder !== '0') {
    return ''
  }
  return new Intl.NumberFormat('vi-VN').format(num)
}

const displayValue = ref(formatNumberString(props.modelValue))

// Đồng bộ khi modelValue thay đổi từ bên ngoài (cha truyền vào)
watch(
  () => props.modelValue,
  (newVal) => {
    const currentNum = parseInt(String(displayValue.value).replace(/[^\d]/g, ''), 10) || 0
    const newNum = parseInt(String(newVal).replace(/[^\d]/g, ''), 10) || 0
    if (isFocused.value && currentNum === newNum) {
      return
    }
    const formatted = formatNumberString(newVal)
    displayValue.value = formatted
    if (inputRef.value && inputRef.value.value !== formatted) {
      inputRef.value.value = formatted
    }
  }
)

const handleFocus = (e) => {
  isFocused.value = true
  // Khi focus vào input, tự động chọn toàn bộ để người dùng có thể gõ đè số mới ngay nếu muốn
  if (inputRef.value && displayValue.value) {
    nextTick(() => {
      if (inputRef.value) {
        inputRef.value.select()
      }
    })
  }
  emit('focus', e)
}

const updateValue = (rawInput, targetCursorDigits) => {
  let digits = String(rawInput || '').replace(/[^\d]/g, '')

  if (!digits) {
    displayValue.value = ''
    if (inputRef.value && inputRef.value.value !== '') {
      inputRef.value.value = ''
    }
    emit('update:modelValue', '')
    emit('change', '')
    return
  }

  if (digits.length > 12) {
    digits = digits.slice(0, 12)
  }

  let num = parseInt(digits, 10)
  if (isNaN(num)) num = 0

  if (props.max !== undefined && props.max !== null && num > props.max) {
    num = props.max
  }

  const formatted = new Intl.NumberFormat('vi-VN').format(num)
  displayValue.value = formatted

  // QUAN TRỌNG: Chỉ gán lại input value khi chuỗi formatted thực sự khác giá trị hiện tại trong DOM!
  // Tuyệt đối không gán đè chuỗi giống nhau vì sẽ gây xung đột với Windows IME (Unikey/EVKey) làm lặp số!
  if (inputRef.value && inputRef.value.value !== formatted) {
    inputRef.value.value = formatted

    // Tính lại vị trí con trỏ theo số lượng chữ số thực tế trước con trỏ
    let newCursorPos = formatted.length
    let count = 0
    for (let i = 0; i < formatted.length; i++) {
      if (/\d/.test(formatted[i])) {
        count++
      }
      if (count >= targetCursorDigits) {
        newCursorPos = i + 1
        break
      }
    }
    inputRef.value.setSelectionRange(newCursorPos, newCursorPos)
  }

  emit('update:modelValue', num)
  emit('change', num)
}

const handleClear = () => {
  displayValue.value = ''
  if (inputRef.value) {
    inputRef.value.value = ''
    inputRef.value.focus()
  }
  emit('update:modelValue', '')
  emit('change', '')
}

const handleCompositionStart = () => {
  isComposing = true
}

const handleCompositionEnd = (e) => {
  isComposing = false
  handleInput(e)
}

const handleKeyDown = (e) => {
  const el = inputRef.value
  if (!el) return

  // Cho phép phím điều khiển, phím tắt sao chép, điều hướng
  if (e.ctrlKey || e.metaKey || ['Tab', 'Enter', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(e.key)) {
    return
  }

  const start = el.selectionStart
  const end = el.selectionEnd

  // Phím Backspace khi con trỏ đứng ngay sau dấu chấm (ví dụ "100.|000")
  if (e.key === 'Backspace' && start === end && start > 0) {
    if (el.value[start - 1] === '.') {
      e.preventDefault()
      // Xóa chữ số đứng trước dấu chấm
      const current = el.value
      const nextVal = current.slice(0, Math.max(0, start - 2)) + current.slice(start)
      const digitsBefore = nextVal.slice(0, Math.max(0, start - 2)).replace(/[^\d]/g, '').length
      updateValue(nextVal, digitsBefore)
      return
    }
  }

  // Phím Delete khi con trỏ đứng ngay trước dấu chấm (ví dụ "100|.000")
  if (e.key === 'Delete' && start === end && start < el.value.length) {
    if (el.value[start] === '.') {
      e.preventDefault()
      // Xóa chữ số đứng sau dấu chấm
      const current = el.value
      const nextVal = current.slice(0, start) + current.slice(start + 2)
      const digitsBefore = nextVal.slice(0, start).replace(/[^\d]/g, '').length
      updateValue(nextVal, digitsBefore)
      return
    }
  }

  // Chặn nhập các ký tự không phải số (cho phép backspace, delete và các phím số)
  if (e.key === '.' || e.key === ',') {
    e.preventDefault()
  }
}

const handleInput = (e) => {
  if (isComposing) return
  const inputEl = e.target
  const rawInput = inputEl.value || ''
  const cursorPos = inputEl.selectionStart || 0
  const digitsBeforeCursor = rawInput.slice(0, cursorPos).replace(/[^\d]/g, '').length
  updateValue(rawInput, digitsBeforeCursor)
}

const handleBlur = (e) => {
  isFocused.value = false
  const raw = String(displayValue.value).replace(/[^\d]/g, '')
  if (!raw) {
    if (props.min > 0) {
      displayValue.value = formatNumberString(props.min)
      if (inputRef.value && inputRef.value.value !== displayValue.value) {
        inputRef.value.value = displayValue.value
      }
      emit('update:modelValue', props.min)
    } else {
      displayValue.value = ''
      if (inputRef.value && inputRef.value.value !== '') {
        inputRef.value.value = ''
      }
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
    if (inputRef.value && inputRef.value.value !== formatted) {
      inputRef.value.value = formatted
    }
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
      :style="{ textAlign: align }"
      :value="displayValue"
      :placeholder="placeholder"
      :disabled="disabled"
      autocomplete="off"
      spellcheck="false"
      @focus="handleFocus"
      @keydown="handleKeyDown"
      @input="handleInput"
      @compositionstart="handleCompositionStart"
      @compositionend="handleCompositionEnd"
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
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  background-color: #ffffff;
  color: #0f172a;
  outline: none;
  transition: all 0.2s ease;
  font-family: inherit;
}

.currency-input:hover:not(:disabled) {
  border-color: #94a3b8;
}

.currency-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.currency-input-wrap.has-error .currency-input {
  border-color: #ef4444;
  background-color: #fef2f2;
}

.currency-input-wrap.has-error .currency-input:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
}

.currency-input-wrap.is-disabled .currency-input {
  background-color: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
  border-color: #e2e8f0;
}

.currency-suffix {
  position: absolute;
  right: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  pointer-events: none;
  user-select: none;
}

.btn-clear-currency {
  position: absolute;
  right: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: #cbd5e1;
  color: #475569;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-clear-currency:hover {
  background: #94a3b8;
  color: #ffffff;
}
</style>
