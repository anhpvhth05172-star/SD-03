<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { removeVietnameseTones } from '@/utils/format'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, null, undefined],
    default: null
  },
  options: {
    type: Array,
    default: () => []
  },
  valueKey: {
    type: String,
    default: 'id'
  },
  labelKey: {
    type: String,
    default: 'ten'
  },
  placeholder: {
    type: String,
    default: 'Chọn giá trị...'
  },
  searchPlaceholder: {
    type: String,
    default: 'Gõ để tìm kiếm...'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  allowClear: {
    type: Boolean,
    default: true
  },
  hasError: {
    type: Boolean,
    default: false
  },
  colorKey: {
    type: String,
    default: 'ma'
  },
  prefix: {
    type: String,
    default: ''
  },
  searchable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const searchQuery = ref('')
const activeIndex = ref(-1)
const selectContainer = ref(null)
const searchInput = ref(null)
const optionsList = ref(null)

const getOptionValue = (opt) => {
  if (opt === null || opt === undefined) return null
  if (typeof opt !== 'object') return opt
  if (props.valueKey in opt) return opt[props.valueKey]
  if ('value' in opt) return opt.value
  if ('id' in opt) return opt.id
  return opt
}

const getOptionLabel = (opt) => {
  if (opt === null || opt === undefined) return ''
  if (typeof opt !== 'object') return String(opt)
  if (props.labelKey in opt) return opt[props.labelKey]
  if ('label' in opt) return opt.label
  if ('ten' in opt) return opt.ten
  if ('tenMau' in opt) return opt.tenMau
  if ('tenKichCo' in opt) return opt.tenKichCo
  if ('name' in opt) return opt.name
  return String(opt)
}

const getOptionColor = (opt) => {
  if (!opt || typeof opt !== 'object') return null
  if (props.colorKey && opt[props.colorKey]) return opt[props.colorKey]
  if (opt.ma && String(opt.ma).startsWith('#')) return opt.ma
  if (opt.colorCode) return opt.colorCode
  return null
}

const selectedOption = computed(() => {
  if (props.modelValue === null || props.modelValue === undefined || props.modelValue === '') {
    return null
  }
  return props.options.find(opt => {
    const val = getOptionValue(opt)
    return String(val) === String(props.modelValue)
  }) || null
})

const displayLabel = computed(() => {
  if (!selectedOption.value) return ''
  const lbl = getOptionLabel(selectedOption.value)
  return props.prefix ? `${props.prefix}${lbl}` : lbl
})

const filteredOptions = computed(() => {
  if (!searchQuery.value || !searchQuery.value.trim()) {
    return props.options
  }
  const query = removeVietnameseTones(searchQuery.value)
  return props.options.filter(opt => {
    const lbl = removeVietnameseTones(getOptionLabel(opt))
    return lbl.includes(query)
  })
})

const toggleDropdown = () => {
  if (props.disabled) return
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

const openDropdown = () => {
  if (props.disabled) return
  isOpen.value = true
  searchQuery.value = ''
  activeIndex.value = -1

  nextTick(() => {
    if (props.searchable && searchInput.value) {
      searchInput.value.focus()
    }
  })
}

const closeDropdown = () => {
  isOpen.value = false
  searchQuery.value = ''
  activeIndex.value = -1
}

const selectOption = (opt) => {
  const val = getOptionValue(opt)
  emit('update:modelValue', val)
  emit('change', val, opt)
  closeDropdown()
}

const clearSelection = (e) => {
  e.stopPropagation()
  emit('update:modelValue', null)
  emit('change', null, null)
}

const handleKeyDown = (e) => {
  if (!isOpen.value) {
    if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
      e.preventDefault()
      openDropdown()
    }
    return
  }

  if (e.key === 'Escape') {
    e.preventDefault()
    closeDropdown()
    return
  }

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    if (activeIndex.value < filteredOptions.value.length - 1) {
      activeIndex.value++
      scrollToActiveOption()
    }
    return
  }

  if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (activeIndex.value > 0) {
      activeIndex.value--
      scrollToActiveOption()
    }
    return
  }

  if (e.key === 'Enter') {
    e.preventDefault()
    if (activeIndex.value >= 0 && activeIndex.value < filteredOptions.value.length) {
      selectOption(filteredOptions.value[activeIndex.value])
    }
    return
  }
}

const scrollToActiveOption = () => {
  nextTick(() => {
    if (!optionsList.value) return
    const activeEl = optionsList.value.querySelector('.searchable-option.is-active')
    if (activeEl) {
      activeEl.scrollIntoView({ block: 'nearest' })
    }
  })
}

const handleClickOutside = (e) => {
  if (selectContainer.value && !selectContainer.value.contains(e.target)) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})
</script>

<template>
  <div 
    ref="selectContainer" 
    class="searchable-select" 
    :class="{ 
      'is-open': isOpen, 
      'is-disabled': disabled,
      'has-error': hasError 
    }"
    @keydown="handleKeyDown"
  >
    <!-- Main Display Trigger -->
    <div 
      class="select-trigger" 
      @click="toggleDropdown"
      tabindex="0"
    >
      <div class="selected-content">
        <template v-if="selectedOption">
          <span 
            v-if="getOptionColor(selectedOption)" 
            class="color-dot-indicator" 
            :style="{ backgroundColor: getOptionColor(selectedOption) }"
          ></span>
          <span class="selected-text">{{ displayLabel }}</span>
        </template>
        <span v-else class="placeholder-text">{{ placeholder }}</span>
      </div>

      <div class="actions-group">
        <button 
          v-if="allowClear && selectedOption && !disabled" 
          type="button" 
          class="clear-btn" 
          @click="clearSelection" 
          title="Xóa lựa chọn"
        >
          ✕
        </button>
        <span class="chevron-icon">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- Dropdown Menu -->
    <div v-show="isOpen" class="dropdown-popover">
      <!-- Search Input -->
      <div v-if="searchable" class="search-box">
        <svg class="search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
        <input 
          ref="searchInput"
          v-model="searchQuery" 
          type="text" 
          class="search-input" 
          :placeholder="searchPlaceholder"
          @click.stop
        />
        <button 
          v-if="searchQuery" 
          type="button" 
          class="clear-search-btn" 
          @click.stop="searchQuery = ''"
        >
          ✕
        </button>
      </div>

      <!-- Options List -->
      <ul ref="optionsList" class="options-list">
        <li 
          v-for="(opt, idx) in filteredOptions" 
          :key="getOptionValue(opt) ?? idx"
          class="searchable-option"
          :class="{
            'is-selected': String(getOptionValue(opt)) === String(modelValue),
            'is-active': idx === activeIndex
          }"
          @click="selectOption(opt)"
          @mouseenter="activeIndex = idx"
        >
          <span 
            v-if="getOptionColor(opt)" 
            class="color-dot-indicator" 
            :style="{ backgroundColor: getOptionColor(opt) }"
          ></span>
          <span class="option-label">
            {{ prefix ? `${prefix}${getOptionLabel(opt)}` : getOptionLabel(opt) }}
          </span>
          <span v-if="String(getOptionValue(opt)) === String(modelValue)" class="check-icon">
            ✓
          </span>
        </li>

        <li v-if="filteredOptions.length === 0" class="no-options">
          Không tìm thấy kết quả phù hợp
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.searchable-select {
  position: relative;
  width: 100%;
  font-family: inherit;
  user-select: none;
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 40px;
  padding: 8px 12px;
  background-color: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  outline: none;
}

.select-trigger:hover:not(.is-disabled) {
  border-color: #9ca3af;
}

.searchable-select.is-open .select-trigger {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15);
}

.searchable-select.has-error .select-trigger {
  border-color: #dc2626;
  background-color: #fef2f2;
}

.searchable-select.is-disabled .select-trigger {
  background-color: #f3f4f6;
  cursor: not-allowed;
  opacity: 0.7;
}

.selected-content {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: #1f2937;
  flex: 1;
}

.color-dot-indicator {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
  display: inline-block;
}

.selected-text {
  font-weight: 500;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
}

.placeholder-text {
  color: #9ca3af;
}

.actions-group {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
  flex-shrink: 0;
}

.clear-btn {
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  line-height: 1;
}

.clear-btn:hover {
  color: #ef4444;
  background-color: #fee2e2;
}

.chevron-icon {
  color: #6b7280;
  display: flex;
  align-items: center;
  transition: transform 0.2s ease;
}

.searchable-select.is-open .chevron-icon {
  transform: rotate(180deg);
}

/* Dropdown Popover */
.dropdown-popover {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  z-index: 1050;
  overflow: hidden;
  animation: fadeInDown 0.15s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-bottom: 1px solid #f3f4f6;
  background-color: #f9fafb;
}

.search-icon {
  position: absolute;
  left: 18px;
  color: #9ca3af;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 6px 28px 6px 28px;
  font-size: 13px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: #ffffff;
  color: #111827;
  outline: none;
  transition: border-color 0.15s ease;
}

.search-input:focus {
  border-color: #dc2626;
}

.clear-search-btn {
  position: absolute;
  right: 18px;
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 12px;
  cursor: pointer;
  padding: 2px;
}

.clear-search-btn:hover {
  color: #4b5563;
}

.options-list {
  list-style: none;
  margin: 0;
  padding: 4px;
  max-height: 220px;
  overflow-y: auto;
}

.options-list::-webkit-scrollbar {
  width: 6px;
}

.options-list::-webkit-scrollbar-thumb {
  background-color: #e5e7eb;
  border-radius: 3px;
}

.options-list::-webkit-scrollbar-thumb:hover {
  background-color: #d1d5db;
}

.searchable-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13.5px;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s ease;
}

.searchable-option:hover,
.searchable-option.is-active {
  background-color: #f3f4f6;
  color: #111827;
}

.searchable-option.is-selected {
  background-color: #fee2e2;
  color: #b91c1c;
  font-weight: 600;
}

.option-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.check-icon {
  color: #dc2626;
  font-weight: bold;
  font-size: 13px;
}

.no-options {
  padding: 16px 12px;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
}
</style>
