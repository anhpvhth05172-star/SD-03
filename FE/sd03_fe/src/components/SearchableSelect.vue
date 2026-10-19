<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'Chọn...' },
  disabled: { type: Boolean, default: false },
  searchable: { type: Boolean, default: true },
  clearable: { type: Boolean, default: false },
  // v-model.number trên component: Vue truyền modifier vào đây, tự xử lý như native
  modelModifiers: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:modelValue', 'change'])

const rootRef = ref(null)
const searchRef = ref(null)
const open = ref(false)
const keyword = ref('')
const cursor = ref(0)

const rawModel = computed(() =>
  props.modelValue == null ? '' : props.modelValue
)

const selectedLabel = computed(() => {
  const hit = props.options.find((o) => String(o.value) === String(rawModel.value))
  return hit ? hit.label : ''
})

// Giống native select: giá trị khớp option nào thì hiển thị label của option đó
// (kể cả option rỗng "Tất cả"); không khớp thì hiển thị placeholder.
const displayLabel = computed(() => selectedLabel.value || '')

const normalize = (s) =>
  String(s == null ? '' : s)
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')

// v-model.number: chuyển giá trị số dạng chuỗi về number trước khi emit
const coerce = (v) => {
  if (props.modelModifiers && props.modelModifiers.number) {
    if (typeof v === 'string' && v.trim() !== '' && !Number.isNaN(Number(v))) {
      return Number(v)
    }
  }
  return v
}

const filtered = computed(() => {
  const q = normalize(keyword.value.trim())
  if (!q) return props.options
  return props.options.filter(
    (o) => normalize(o.label).includes(q) || normalize(String(o.value)).includes(q)
  )
})

const clampCursor = () => {
  const list = filtered.value
  if (!list.length) {
    cursor.value = 0
    return
  }
  if (cursor.value >= list.length || list[cursor.value]?.disabled) {
    cursor.value = firstEnabled()
  }
}

const firstEnabled = () => {
  const idx = filtered.value.findIndex((o) => !o.disabled)
  return idx === -1 ? 0 : idx
}

watch(filtered, () => {
  cursor.value = firstEnabled()
})

// di chuyển con trỏ, bỏ qua option disabled
const moveCursor = (dir) => {
  const list = filtered.value
  if (!list.length) return
  let i = cursor.value
  for (let n = 0; n < list.length; n++) {
    i = (i + dir + list.length) % list.length
    if (!list[i].disabled) {
      cursor.value = i
      return
    }
  }
}

const toggle = () => {
  if (props.disabled) return
  open.value = !open.value
  if (open.value) {
    keyword.value = ''
    cursor.value = 0
    nextTick(() => searchRef.value && searchRef.value.focus())
  }
}

const close = (refocus = false) => {
  open.value = false
  keyword.value = ''
  if (refocus) nextTick(() => rootRef.value && rootRef.value.focus())
}

const choose = (opt) => {
  if (opt.disabled) return
  close(true)
  if (String(opt.value) !== String(rawModel.value)) {
    emit('update:modelValue', coerce(opt.value))
    emit('change', coerce(opt.value))
  }
}

const chooseCursor = () => {
  const hit = filtered.value[cursor.value]
  if (hit) choose(hit)
}

const clear = () => {
  if (props.disabled) return
  close(true)
  if (rawModel.value !== '') {
    emit('update:modelValue', '')
    emit('change', '')
  }
}

const onRootClick = (e) => {
  if (props.disabled) return
  // click vào nút clear / dropdown đã stopPropagation riêng
  toggle()
}

const onRootKeydown = (e) => {
  if (props.disabled) return
  if (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar') {
    e.preventDefault()
    toggle()
    return
  }
  if (e.key === 'ArrowDown' && !open.value) {
    e.preventDefault()
    toggle()
    return
  }
  // gõ ký tự khi đang đóng -> mở và bắt đầu tìm kiếm ngay
  if (!open.value && e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) {
    open.value = true
    keyword.value = e.key
    cursor.value = 0
    e.preventDefault()
    nextTick(() => searchRef.value && searchRef.value.focus())
  }
}

const onSearchKeydown = (e) => {
  if (e.key === 'Escape') {
    e.stopPropagation()
    close(true)
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    moveCursor(1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    moveCursor(-1)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    chooseCursor()
  } else if (e.key === 'Tab') {
    close(false)
  }
}

const onDocMousedown = (e) => {
  if (open.value && rootRef.value && !rootRef.value.contains(e.target)) close(false)
}

onMounted(() => {
  document.addEventListener('mousedown', onDocMousedown)
})
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocMousedown)
})

watch(
  () => props.disabled,
  (v) => {
    if (v) close(false)
  }
)

watch(
  () => props.options,
  () => clampCursor()
)
</script>

<template>
  <div
    ref="rootRef"
    class="ss"
    :class="{ 'is-open': open, 'is-disabled': disabled }"
    role="combobox"
    :tabindex="disabled ? -1 : 0"
    :aria-expanded="open"
    :aria-disabled="disabled"
    aria-haspopup="listbox"
    @click="onRootClick"
    @keydown="onRootKeydown"
  >
    <span class="ss-value" :class="{ 'is-placeholder': !displayLabel }">
      {{ displayLabel || placeholder }}
    </span>
    <span class="ss-actions" @click.stop>
      <span
        v-if="clearable && displayLabel && !disabled"
        class="ss-clear"
        role="button"
        tabindex="-1"
        title="Bỏ chọn"
        @click="clear"
        @keydown.enter.stop.prevent="clear"
      >×</span>
      <svg class="ss-caret" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="6 9 12 15 18 9"></polyline>
      </svg>
    </span>

    <div v-if="open" class="ss-dropdown" @click.stop>
      <div v-if="searchable" class="ss-search-wrap">
        <input
          ref="searchRef"
          v-model="keyword"
          type="text"
          class="ss-search"
          placeholder="Tìm kiếm..."
          autocomplete="off"
          @keydown="onSearchKeydown"
        />
      </div>
      <ul class="ss-list" role="listbox">
        <li v-if="filtered.length === 0" class="ss-empty">Không tìm thấy kết quả phù hợp</li>
        <li
          v-for="(opt, idx) in filtered"
          :key="String(opt.value)"
          class="ss-option"
          :class="{
            'is-active': String(opt.value) === String(rawModel),
            'is-cursor': idx === cursor,
            'is-disabled': !!opt.disabled,
          }"
          role="option"
          :aria-disabled="!!opt.disabled"
          :aria-selected="String(opt.value) === String(rawModel)"
          @mousemove="cursor = idx"
          @click="choose(opt)"
        >
          <span class="ss-option-label">{{ opt.label }}</span>
          <svg
            v-if="String(opt.value) === String(rawModel)"
            class="ss-check"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
/*
 * Style của ô chọn (root .ss) được đặt trong assets/main.css (global, không scoped)
 * để các class style theo trang (scoped) luôn ghi đè đúng như với <select> cũ.
 * Style dưới đây chỉ dành cho các phần bên trong.
 */
.ss-value {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: none;
}

.ss-value.is-placeholder {
  color: #94a3b8;
}

.ss-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  flex-shrink: 0;
}

.ss-clear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  color: #94a3b8;
  font-size: 16px;
  line-height: 1;
}

.ss-clear:hover {
  background: #fee2e2;
  color: #ef4444;
}

.ss-caret {
  width: 16px;
  height: 16px;
  transition: transform 0.15s;
  flex-shrink: 0;
}

.ss.is-open .ss-caret {
  transform: rotate(180deg);
}

.ss-dropdown {
  position: absolute;
  z-index: 300;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.14);
  overflow: hidden;
}

.ss-search-wrap {
  padding: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.ss-search {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13.5px;
  color: #0f172a;
  background: #fff;
  outline: none;
  box-sizing: border-box;
}

.ss-search:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.ss-list {
  list-style: none;
  margin: 0;
  padding: 6px;
  max-height: 260px;
  overflow-y: auto;
}

.ss-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 6px;
  font-size: 14px;
  color: #1e293b;
  cursor: pointer;
}

.ss-option.is-cursor {
  background: #f1f5f9;
}

.ss-option.is-disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.ss-option.is-disabled.is-cursor {
  background: transparent;
}

.ss-option.is-active {
  background: #eef2ff;
  color: #4338ca;
  font-weight: 600;
}

.ss-option.is-active.is-cursor {
  background: #e0e7ff;
}

.ss-option-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ss-check {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}

.ss-empty {
  padding: 14px 10px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
</style>
