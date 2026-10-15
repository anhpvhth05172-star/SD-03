<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: [Number, String],
    default: 0
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

const emit = defineEmits(['update:modelValue', 'change', 'blur'])

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
    const formatted = formatNumberString(newVal)
    if (displayValue.value !== formatted) {
      displayValue.value = formatted
    }
  }
)

const handleInput = (e) => {
  const rawInput = e.target.value
  const digits = rawInput.replace(/[^\d]/g, '')
  
  if (!digits) {
    displayValue.value = ''
    emit('update:modelValue', 0)
    emit('change', 0)
    return
  }

  let num = parseInt(digits, 10)
  if (props.max && num > props.max) {
    num = props.max
  }

  displayValue.value = new Intl.NumberFormat('vi-VN').format(num)
  emit('update:modelValue', num)
  emit('change', num)
}

const handleBlur = (e) => {
  let num = typeof props.modelValue === 'number' ? props.modelValue : parseInt(String(displayValue.value).replace(/[^\d]/g, ''), 10)
  if (isNaN(num) || num < props.min) {
    num = props.min
  }
  displayValue.value = formatNumberString(num)
  emit('update:modelValue', num)
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
      type="text" 
      class="currency-input"
      :value="displayValue"
      :placeholder="placeholder"
      :disabled="disabled"
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
