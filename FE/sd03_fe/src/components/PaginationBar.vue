<script setup>
import { computed } from 'vue'
import SearchableSelect from '@/components/SearchableSelect.vue'

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

const sizeOptions = computed(() => props.sizes.map((s) => ({ value: s, label: String(s) })))

const onPage = (p) => {
  if (p < 0 || p >= props.totalPages || p === props.page) return
  emit('change', p)
}

const onSize = (val) => {
  const n = Number(val)
  if (!n || n === props.pageSize) return
  emit('size-change', n)
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
    <span class="pb-info">
      Hiển thị <strong>{{ from }}-{{ to }}</strong> trong tổng số
      <strong>{{ totalElements }}</strong
      ><span v-if="label">{{ " " + label }}</span>
    </span>
    <div class="pb-controls">
      <nav class="pb-nav" aria-label="Phân trang">
        <button class="pb-btn" type="button" :disabled="page <= 0" title="Trang đầu" @click="onPage(0)">«</button>
        <button class="pb-btn" type="button" :disabled="page <= 0" title="Trang trước" @click="onPage(page - 1)">‹</button>
        <template v-for="(p, i) in pages" :key="'pb-' + i">
          <span v-if="p === null" class="pb-ellipsis">…</span>
          <button
            v-else
            class="pb-btn"
            :class="{ 'is-active': p === page }"
            type="button"
            :aria-current="p === page ? 'page' : undefined"
            @click="onPage(p)"
          >
            {{ p + 1 }}
          </button>
        </template>
        <button class="pb-btn" type="button" :disabled="page >= totalPages - 1" title="Trang sau" @click="onPage(page + 1)">›</button>
        <button class="pb-btn" type="button" :disabled="page >= totalPages - 1" title="Trang cuối" @click="onPage(totalPages - 1)">»</button>
      </nav>
      <div class="pb-size">
        <SearchableSelect
          :model-value="pageSize"
          :options="sizeOptions"
          label-key="label"
          value-key="value"
          placeholder="Số dòng"
          search-placeholder="Tìm số dòng..."
          @change="onSize"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
}

.pb-info {
  font-size: 13px;
  color: #475569;
}

.pb-info strong {
  color: #0f172a;
}

.pb-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pb-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pb-btn {
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pb-btn:hover:not(:disabled):not(.is-active) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.pb-btn.is-active {
  background: #cc0000;
  border-color: #cc0000;
  color: #fff;
  font-weight: 600;
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

.pb-size {
  width: 130px;
}
</style>
