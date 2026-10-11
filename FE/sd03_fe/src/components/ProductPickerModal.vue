<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatVnd } from '../utils/format'

const props = defineProps({
  isOpen: Boolean,
  products: { type: Array, default: () => [] },
  existingIds: { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'confirm'])

const keyword = ref('')
const page = ref(1)
const pageSize = 8
const selected = ref([])

const lower = (v) => String(v ?? '').toLowerCase()

const daCoSet = computed(() => new Set(props.existingIds.map((n) => Number(n))))

const trangThaiDong = (s) => {
  if (daCoSet.value.has(Number(s.id))) return 'existing'
  if (s.trangThai === false || s.trangThaiSanPham === false) return 'ngungBan'
  if (s.soLuong !== null && s.soLuong !== undefined && Number(s.soLuong) <= 0) return 'hetHang'
  return 'ok'
}

const filtered = computed(() => {
  const kw = lower(keyword.value).trim()
  if (!kw) return props.products
  return props.products.filter((s) =>
    [s.ma, s.tenSanPham, s.tenKichCo, s.tenMau].some((f) => lower(f).includes(kw)),
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))

watch(filtered, () => {
  if (page.value > totalPages.value) page.value = totalPages.value
})

watch(keyword, () => {
  page.value = 1
})

const pageItems = computed(() =>
  filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize),
)

const chonDuocTrenTrang = computed(() =>
  pageItems.value.filter((s) => trangThaiDong(s) === 'ok'),
)

const tatCaTrenTrang = computed(
  () =>
    chonDuocTrenTrang.value.length > 0 &&
    chonDuocTrenTrang.value.every((s) => selected.value.includes(s.id)),
)

const toggleTatCa = () => {
  const ids = chonDuocTrenTrang.value.map((s) => s.id)
  if (tatCaTrenTrang.value) {
    selected.value = selected.value.filter((id) => !ids.includes(id))
  } else {
    selected.value = [...new Set([...selected.value, ...ids])]
  }
}

const toggleDong = (s) => {
  if (trangThaiDong(s) !== 'ok') return
  selected.value = selected.value.includes(s.id)
    ? selected.value.filter((id) => id !== s.id)
    : [...selected.value, s.id]
}

const soDaChon = computed(() => selected.value.length)

const dong = () => emit('close')

const xacNhan = () => {
  if (!selected.value.length) return
  emit('confirm', [...selected.value])
}

const reset = () => {
  keyword.value = ''
  page.value = 1
  selected.value = []
}

watch(
  () => props.isOpen,
  (open) => {
    if (open) reset()
  },
)

const onKeydown = (e) => {
  if (e.key === 'Escape' && props.isOpen) dong()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div v-if="isOpen" class="pp-overlay" @click.self="dong">
    <div class="pp-card" role="dialog" aria-modal="true" aria-label="Thêm dòng sản phẩm">
      <div class="pp-header">
        <h3 class="pp-title">Thêm dòng sản phẩm</h3>
        <button class="pp-close" type="button" aria-label="Đóng" @click="dong">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <div class="pp-toolbar">
        <input
          v-model="keyword"
          class="pp-search"
          type="text"
          placeholder="Tìm theo mã, tên, kích cỡ, màu sắc..."
        />
        <span class="pp-selected">Đã chọn: {{ soDaChon }}</span>
      </div>

      <div class="pp-table-wrap">
        <table class="pp-table">
          <thead>
            <tr>
              <th class="pp-col-check">
                <input
                  type="checkbox"
                  :checked="tatCaTrenTrang"
                  :disabled="!chonDuocTrenTrang.length"
                  aria-label="Chọn tất cả sản phẩm trên trang"
                  @change="toggleTatCa"
                />
              </th>
              <th>Mã</th>
              <th>Sản phẩm</th>
              <th>Biến thể</th>
              <th class="pp-num">Giá bán</th>
              <th class="pp-num">Tồn kho</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="s in pageItems"
              :key="s.id"
              :class="{
                'pp-row-off': trangThaiDong(s) !== 'ok',
                'pp-row-sel': selected.includes(s.id),
              }"
              @click="toggleDong(s)"
            >
              <td class="pp-col-check" @click.stop>
                <input
                  type="checkbox"
                  :checked="selected.includes(s.id)"
                  :disabled="trangThaiDong(s) !== 'ok'"
                  :aria-label="`Chọn ${s.ma}`"
                  @change="toggleDong(s)"
                />
              </td>
              <td class="pp-ma">{{ s.ma }}</td>
              <td>{{ s.tenSanPham }}</td>
              <td>{{ s.tenKichCo }} / {{ s.tenMau }}</td>
              <td class="pp-num">{{ formatVnd(s.giaBan) }}</td>
              <td class="pp-num">
                {{ s.soLuong === null || s.soLuong === undefined ? '—' : s.soLuong }}
              </td>
              <td>
                <span v-if="trangThaiDong(s) === 'existing'" class="pp-badge pp-badge-info">
                  Đã có trong hóa đơn
                </span>
                <span v-else-if="trangThaiDong(s) === 'ngungBan'" class="pp-badge pp-badge-danger">
                  Ngừng bán
                </span>
                <span v-else-if="trangThaiDong(s) === 'hetHang'" class="pp-badge pp-badge-warn">
                  Hết hàng
                </span>
                <span v-else class="pp-badge pp-badge-ok">Có thể bán</span>
              </td>
            </tr>
            <tr v-if="!pageItems.length">
              <td colspan="7" class="pp-empty">Không tìm thấy sản phẩm phù hợp</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pp-footer">
        <div class="pp-pager">
          <template v-if="filtered.length">
            <button
              class="pp-page-btn"
              type="button"
              :disabled="page <= 1"
              aria-label="Trang trước"
              @click="page--"
            >
              ‹
            </button>
            <span class="pp-page-info">Trang {{ page }} / {{ totalPages }}</span>
            <button
              class="pp-page-btn"
              type="button"
              :disabled="page >= totalPages"
              aria-label="Trang sau"
              @click="page++"
            >
              ›
            </button>
            <span class="pp-total">Tổng {{ filtered.length }} sản phẩm</span>
          </template>
        </div>
        <div class="pp-actions">
          <button class="pp-btn pp-btn-cancel" type="button" @click="dong">Hủy</button>
          <button class="pp-btn pp-btn-ok" type="button" :disabled="!soDaChon" @click="xacNhan">
            Thêm ({{ soDaChon }})
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pp-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 16px;
  animation: pp-fade-in 0.15s ease-out;
}

.pp-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 780px;
  max-height: calc(100vh - 32px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  animation: pp-scale-up 0.15s ease-out;
}

.pp-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pp-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.pp-close {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
}

.pp-close:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.pp-toolbar {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.pp-search {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  color: #0f172a;
  outline: none;
}

.pp-search:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.pp-selected {
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  white-space: nowrap;
}

.pp-table-wrap {
  overflow: auto;
  flex: 1;
}

.pp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.pp-table th,
.pp-table td {
  padding: 9px 12px;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
  color: #334155;
}

.pp-table th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  z-index: 1;
}

.pp-num {
  text-align: right !important;
  white-space: nowrap;
}

.pp-col-check {
  width: 36px;
  text-align: center !important;
}

.pp-col-check input {
  cursor: pointer;
}

.pp-table tbody tr {
  cursor: pointer;
}

.pp-table tbody tr:hover {
  background: #f8fafc;
}

.pp-row-sel {
  background: #eff6ff !important;
}

.pp-row-off {
  opacity: 0.55;
  cursor: not-allowed !important;
}

.pp-ma {
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
}

.pp-empty {
  text-align: center !important;
  color: #94a3b8;
  padding: 28px 12px !important;
  cursor: default;
}

.pp-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.pp-badge-ok {
  background: #dcfce7;
  color: #15803d;
}

.pp-badge-info {
  background: #e0f2fe;
  color: #0369a1;
}

.pp-badge-danger {
  background: #fee2e2;
  color: #b91c1c;
}

.pp-badge-warn {
  background: #fef3c7;
  color: #b45309;
}

.pp-footer {
  padding: 12px 20px;
  background-color: #f8fafc;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pp-pager {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
}

.pp-page-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
  color: #334155;
}

.pp-page-btn:hover:not(:disabled) {
  background: #f1f5f9;
}

.pp-page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pp-total {
  color: #64748b;
  margin-left: 4px;
}

.pp-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pp-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
}

.pp-btn-cancel {
  background: #fff;
  border-color: #cbd5e1;
  color: #334155;
}

.pp-btn-cancel:hover {
  background: #f1f5f9;
}

.pp-btn-ok {
  background: #2563eb;
  color: #fff;
}

.pp-btn-ok:hover:not(:disabled) {
  background: #1d4ed8;
}

.pp-btn-ok:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@keyframes pp-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes pp-scale-up {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
