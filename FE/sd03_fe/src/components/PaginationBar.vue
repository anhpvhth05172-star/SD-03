<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  totalElements: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  from: { type: Number, required: true },
  to: { type: Number, required: true },
  sizes: { type: Array, default: () => [10, 20, 50] },
  label: { type: String, default: '' },
})

const emit = defineEmits(['change', 'size-change'])

const onPage = (p) => {
  if (p < 0 || p >= props.totalPages || p === props.page) return
  emit('change', p)
}

const pages = computed(() => {
  const tp = props.totalPages
  const cur = props.page
  if (tp <= 7) return Array.from({ length: tp }, (_, i) => i)
  const set = new Set([0, tp - 1])
  for (let i = cur - 2; i <= cur + 2; i++) {
    if (i >= 0 && i < tp) set.add(i)
  }
  const sorted = [...set].sort((a, b) => a - b)
  const out = []
  let prev = null
  for (const p of sorted) {
    if (prev !== null && p - prev > 1) out.push(null)
    out.push(p)
    prev = p
  }
  return out
})
</script>

<template>
  <div class="pagination-bar">
    <span class="pb-info">Hiển thị {{ from }} - {{ to }} / {{ totalElements }}{{ label ? ' ' + label : '' }}</span>
    <nav class="pb-nav" aria-label="Phân trang">
      <button class="pb-btn pb-arrow" type="button" :disabled="page <= 0" title="Trang trước" @click="onPage(page - 1)">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <template v-for="(p, i) in pages" :key="'pb-' + i">
        <span v-if="p === null" class="pb-ellipsis">…</span>
        <button
          v-else
          class="pb-btn pb-page"
          :class="{ 'is-active': p === page }"
          type="button"
          :aria-current="p === page ? 'page' : undefined"
          @click="onPage(p)"
        >
          {{ p + 1 }}
        </button>
      </template>
      <button
        class="pb-btn pb-arrow"
        type="button"
        :disabled="page >= totalPages - 1"
        title="Trang sau"
        @click="onPage(page + 1)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 18l6-6-6-6" />
        </svg>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid #eef2f6;
}

.pb-info {
  font-size: 13.5px;
  color: #334155;
}

.pb-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pb-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 34px;
  padding: 0 6px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 13.5px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pb-arrow svg {
  width: 16px;
  height: 16px;
}

.pb-btn:hover:not(:disabled):not(.is-active) {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #334155;
}

.pb-page.is-active {
  background: #1e293b;
  border-color: #1e293b;
  color: #fff;
  font-weight: 600;
  min-width: 38px;
  height: 38px;
}

.pb-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pb-btn:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.pb-ellipsis {
  padding: 0 2px;
  color: #94a3b8;
  font-size: 13px;
}
</style>
