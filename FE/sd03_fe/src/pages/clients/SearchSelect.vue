<script setup>
import {
  ref,
  computed,
  watch,
  nextTick,
  onMounted,
  onBeforeUnmount,
} from "vue";

const props = defineProps({
  modelValue: { type: [String, Number], default: "" },
  options: { type: Array, default: () => [] }, // mảng string
  icon: { type: String, default: "bi-grid" },
  allLabel: { type: String, default: "Tất cả" },
  placeholder: { type: String, default: "Tìm kiếm..." },
});
const emit = defineEmits(["update:modelValue"]);

const root = ref(null);
const searchEl = ref(null);
const listEl = ref(null);
const open = ref(false);
const keyword = ref("");
const active = ref(0);

// Bỏ dấu tiếng Việt để gõ không dấu vẫn tìm ra
const normalize = (s) =>
  String(s ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .trim();

const items = computed(() => {
  const k = normalize(keyword.value);
  const list = (
    k ? props.options.filter((o) => normalize(o).includes(k)) : props.options
  ).map((o) => ({ value: o, label: o }));
  // Chỉ hiện "Tất cả" khi chưa gõ tìm kiếm
  return k ? list : [{ value: "", label: props.allLabel }, ...list];
});

const displayLabel = computed(() => props.modelValue || props.allLabel);

const scrollToActive = () => {
  nextTick(() => {
    const el = listEl.value?.children[active.value];
    el?.scrollIntoView({ block: "nearest" });
  });
};

const openDropdown = async () => {
  open.value = true;
  keyword.value = "";
  const idx = items.value.findIndex((i) => i.value === props.modelValue);
  active.value = idx >= 0 ? idx : 0;
  await nextTick();
  searchEl.value?.focus();
  scrollToActive();
};

const closeDropdown = () => {
  open.value = false;
};

const toggle = () => (open.value ? closeDropdown() : openDropdown());

const select = (item) => {
  emit("update:modelValue", item.value);
  closeDropdown();
};

const onKeydown = (e) => {
  if (e.key === "ArrowDown") {
    e.preventDefault();
    active.value = Math.min(active.value + 1, items.value.length - 1);
    scrollToActive();
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    active.value = Math.max(active.value - 1, 0);
    scrollToActive();
  } else if (e.key === "Enter") {
    e.preventDefault();
    if (items.value[active.value]) select(items.value[active.value]);
  } else if (e.key === "Escape") {
    closeDropdown();
  }
};

// Gõ tìm kiếm thì đưa con trỏ về dòng đầu tiên
watch(keyword, () => (active.value = 0));

// Bấm ra ngoài thì đóng
const onClickOutside = (e) => {
  if (root.value && !root.value.contains(e.target)) closeDropdown();
};
onMounted(() => document.addEventListener("mousedown", onClickOutside));
onBeforeUnmount(() =>
  document.removeEventListener("mousedown", onClickOutside),
);
</script>

<template>
  <div ref="root" class="ss" :class="{ 'is-open': open }">
    <button type="button" class="ss-control" @click="toggle">
      <i class="bi ss-icon" :class="icon"></i>
      <span class="ss-label">{{ displayLabel }}</span>
      <i class="bi bi-chevron-down ss-caret"></i>
    </button>

    <div v-if="open" class="ss-dropdown">
      <div class="ss-search">
        <i class="bi bi-search"></i>
        <input
          ref="searchEl"
          v-model="keyword"
          type="text"
          :placeholder="placeholder"
          @keydown="onKeydown"
        />
      </div>

      <ul ref="listEl" class="ss-list">
        <li
          v-for="(it, idx) in items"
          :key="it.value || '__all__'"
          class="ss-option"
          :class="{
            'is-active': idx === active,
            'is-selected': it.value === modelValue,
          }"
          @mouseenter="active = idx"
          @click="select(it)"
        >
          <span>{{ it.label }}</span>
          <i v-if="it.value === modelValue" class="bi bi-check2"></i>
        </li>
        <li v-if="!items.length" class="ss-empty">Không tìm thấy kết quả</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.ss {
  position: relative;
  width: 100%;
}

/* Khớp với .select-box select của trang cửa hàng */
.ss-control {
  width: 100%;
  height: 44px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  font-size: 0.9rem;
  color: #1e293b;
  text-align: left;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;
}
.ss-control:focus-visible,
.ss.is-open .ss-control {
  background: #ffffff;
  border-color: #f87171;
  box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.2);
}
.ss-icon {
  color: #94a3b8;
}
.ss-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ss-caret {
  font-size: 0.75rem;
  color: #94a3b8;
  transition: transform 0.15s;
}
.ss.is-open .ss-caret {
  transform: rotate(180deg);
}

.ss-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  min-width: 220px;
  z-index: 1050;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.14);
  overflow: hidden;
}
.ss-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #94a3b8;
}
.ss-search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  font-size: 0.9rem;
  color: #111827;
  background: transparent;
}
.ss-list {
  list-style: none;
  margin: 0;
  padding: 6px;
  max-height: 240px;
  overflow-y: auto;
}
.ss-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 0.9rem;
  color: #334155;
  cursor: pointer;
}
.ss-option.is-active {
  background: #fef2f2;
  color: #dc2626;
}
.ss-option.is-selected {
  font-weight: 700;
}
.ss-empty {
  padding: 14px 10px;
  text-align: center;
  font-size: 0.85rem;
  color: #94a3b8;
}
</style>
