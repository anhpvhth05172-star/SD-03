<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";

const props = defineProps({
  modelValue: {
    type: String,
    default: "",
  },
  options: {
    type: Array,
    default: () => [], // Mảng string hoặc { value, label }
  },
  placeholder: {
    type: String,
    default: "Chọn một mục...",
  },
  searchPlaceholder: {
    type: String,
    default: "Tìm kiếm...",
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  emptyText: {
    type: String,
    default: "Không tìm thấy kết quả",
  },
  invalid: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["update:modelValue", "change"]);

const isOpen = ref(false);
const searchQuery = ref("");
const searchInputRef = ref(null);
const containerRef = ref(null);

// Chuẩn hóa options thành { value, label }
// Hỗ trợ cả { code, ten }, { id, name }, { value, label }, chuỗi string
const normalizedOptions = computed(() => {
  return props.options.map((item) => {
    if (typeof item === "object" && item !== null) {
      const val = item.value ?? item.ten ?? item.name ?? item.code ?? "";
      const lbl = item.label ?? item.ten ?? item.name ?? String(val);
      return {
        value: String(val),
        label: String(lbl),
      };
    }
    return { value: String(item), label: String(item) };
  });
});

// Lọc options theo từ khóa tìm kiếm (bỏ dấu tiếng Việt)
const removeVietnameseTones = (str) => {
  if (!str) return "";
  return str
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase();
};

const filteredOptions = computed(() => {
  const q = removeVietnameseTones(searchQuery.value.trim());
  if (!q) return normalizedOptions.value;
  return normalizedOptions.value.filter((opt) =>
    removeVietnameseTones(opt.label).includes(q),
  );
});

const selectedLabel = computed(() => {
  const found = normalizedOptions.value.find(
    (opt) => opt.value === props.modelValue,
  );
  return found ? found.label : "";
});

function toggleDropdown() {
  if (props.disabled) return;
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    searchQuery.value = "";
    setTimeout(() => {
      searchInputRef.value?.focus();
    }, 50);
  }
}

function selectOption(opt) {
  emit("update:modelValue", opt.value);
  emit("change", opt.value);
  isOpen.value = false;
  searchQuery.value = "";
}

function handleClickOutside(e) {
  if (containerRef.value && !containerRef.value.contains(e.target)) {
    isOpen.value = false;
  }
}

onMounted(() => {
  window.addEventListener("click", handleClickOutside);
});

onUnmounted(() => {
  window.removeEventListener("click", handleClickOutside);
});

watch(
  () => props.disabled,
  (val) => {
    if (val) isOpen.value = false;
  },
);
</script>

<template>
  <div
    ref="containerRef"
    class="select2-container"
    :class="{ 'is-open': isOpen, 'is-disabled': disabled, 'is-invalid': invalid }"
  >
    <!-- Hộp hiển thị lựa chọn -->
    <div
      class="select2-selection"
      role="combobox"
      tabindex="0"
      @click="toggleDropdown"
      @keydown.enter.prevent="toggleDropdown"
      @keydown.space.prevent="toggleDropdown"
    >
      <span v-if="selectedLabel" class="select2-rendered">
        {{ selectedLabel }}
      </span>
      <span v-else class="select2-placeholder">
        {{ placeholder }}
      </span>
      <span class="select2-arrow">
        <i class="bi" :class="isOpen ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
      </span>
    </div>

    <!-- Dropdown menu -->
    <div v-show="isOpen" class="select2-dropdown">
      <!-- Ô tìm kiếm -->
      <div class="select2-search">
        <i class="bi bi-search select2-search-ico"></i>
        <input
          ref="searchInputRef"
          v-model="searchQuery"
          type="text"
          class="select2-search-input"
          :placeholder="searchPlaceholder"
          @click.stop
        />
      </div>

      <!-- Danh sách options -->
      <ul class="select2-results">
        <li
          v-for="opt in filteredOptions"
          :key="opt.value"
          class="select2-item"
          :class="{ 'is-selected': opt.value === modelValue }"
          @click="selectOption(opt)"
        >
          <span>{{ opt.label }}</span>
          <i
            v-if="opt.value === modelValue"
            class="bi bi-check-lg select2-check"
          ></i>
        </li>
        <li v-if="!filteredOptions.length" class="select2-empty">
          {{ emptyText }}
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.select2-container {
  position: relative;
  width: 100%;
  font-size: 0.95rem;
}

.select2-selection {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 0.55rem 0.9rem;
  background-color: #fff;
  border: 1px solid #ced4da;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.select2-container:not(.is-disabled) .select2-selection:hover {
  border-color: #a0aec0;
}

.select2-container.is-open .select2-selection {
  border-color: var(--bs-primary, #d62828);
  box-shadow: 0 0 0 0.2rem rgba(214, 40, 40, 0.15);
}

.select2-container.is-invalid .select2-selection {
  border-color: #dc3545;
}

.select2-container.is-disabled .select2-selection {
  background-color: #e9ecef;
  cursor: not-allowed;
  opacity: 0.75;
}

.select2-rendered {
  color: #212529;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 8px;
}

.select2-placeholder {
  color: #6c757d;
  font-weight: 400;
}

.select2-arrow {
  color: #6c757d;
  font-size: 0.8rem;
  display: flex;
  align-items: center;
}

/* Dropdown */
.select2-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 1050;
  background-color: #fff;
  border: 1px solid #ced4da;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  animation: select2FadeIn 0.15s ease-out;
}

@keyframes select2FadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.select2-search {
  position: relative;
  padding: 8px 10px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.select2-search-ico {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 0.85rem;
}

.select2-search-input {
  width: 100%;
  padding: 6px 12px 6px 32px;
  font-size: 0.9rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  outline: none;
  background-color: #fff;
}

.select2-search-input:focus {
  border-color: var(--bs-primary, #d62828);
}

.select2-results {
  list-style: none;
  margin: 0;
  padding: 4px 0;
  max-height: 220px;
  overflow-y: auto;
}

.select2-results::-webkit-scrollbar {
  width: 6px;
}
.select2-results::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 3px;
}

.select2-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  font-size: 0.9rem;
  color: #334155;
  cursor: pointer;
  transition: background-color 0.1s ease;
}

.select2-item:hover {
  background-color: rgba(214, 40, 40, 0.08);
  color: var(--bs-primary, #d62828);
}

.select2-item.is-selected {
  background-color: rgba(214, 40, 40, 0.12);
  color: var(--bs-primary, #d62828);
  font-weight: 600;
}

.select2-check {
  font-size: 1rem;
}

.select2-empty {
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 0.88rem;
}
</style>

