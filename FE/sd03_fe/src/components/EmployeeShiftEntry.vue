<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDangMo, moCa } from '@/api/giaoCa'
import { formatDateTime, formatVnd } from '@/utils/format'
import { laAdmin } from '@/utils/session'
import { notifySuccess } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const caHienTai = ref(null)
const dangTai = ref(false)
const moModal = ref(false)
const dangMo = ref(false)
const loi = ref('')
const form = reactive({ tienDauCa: 500000, ghiChu: '' })

const hienThi = computed(() => !laAdmin() && route.path !== '/ban-hang' && route.path !== '/admin/ban-hang')

const taiCa = async () => {
  if (!hienThi.value) return
  dangTai.value = true
  try {
    caHienTai.value = await getDangMo()
  } catch (error) {
    console.error('Lỗi tải ca làm việc:', error)
  } finally {
    dangTai.value = false
  }
}

const batDauMoCa = () => {
  loi.value = ''
  form.tienDauCa = 500000
  form.ghiChu = ''
  moModal.value = true
}

const batDauKetCa = async () => {
  if (route.name !== 'PosSales') await router.push({ name: 'PosSales' })
  await nextTick()
  window.dispatchEvent(new Event('polyshoes:open-ket-ca'))
}

const xacNhanMoCa = async () => {
  const tien = Number(form.tienDauCa)
  if (!Number.isFinite(tien) || tien < 0 || tien % 1000 !== 0) {
    loi.value = 'Tiền đầu ca phải là số không âm và là bội số của 1.000đ'
    return
  }

  dangMo.value = true
  loi.value = ''
  try {
    caHienTai.value = await moCa({ tienDauCa: tien, ghiChu: form.ghiChu.trim() || null })
    moModal.value = false
    notifySuccess('Mở ca thành công', `Ca ${caHienTai.value.maCa} đã được mở`)
    window.dispatchEvent(new Event('polyshoes:shift-changed'))
  } catch (error) {
    const message = error.message || ''
    if (message.includes('CA_DA_MO')) {
      await taiCa()
      moModal.value = false
    } else if (message.includes('CA_CHUA_DUOC_XEP_LICH')) {
      loi.value = 'Bạn chưa được phân công ca đang diễn ra hôm nay. Nhờ Admin xếp lịch trước khi vào ca.'
    } else if (message.includes('CA_XEP_LICH_TRUNG')) {
      loi.value = 'Lịch của bạn có ca bị trùng giờ. Nhờ Admin điều chỉnh lịch trước khi vào ca.'
    } else {
      loi.value = message || 'Không thể mở ca làm việc'
    }
  } finally {
    dangMo.value = false
  }
}

onMounted(() => {
  taiCa()
  window.addEventListener('polyshoes:shift-changed', taiCa)
})
onUnmounted(() => window.removeEventListener('polyshoes:shift-changed', taiCa))
watch(() => route.fullPath, taiCa)
</script>

<template>
  <section v-if="hienThi" class="shift-entry" :class="{ 'shift-entry-open': caHienTai }" aria-live="polite">
    <div class="shift-entry-status">
      <span class="shift-entry-dot"></span>
      <span v-if="dangTai">Đang tải trạng thái ca...</span>
      <span v-else-if="caHienTai">
        Đang trong ca {{ caHienTai.tenCaLich || caHienTai.maCa }}<template v-if="caHienTai.tenCaLich"> · Mã {{ caHienTai.maCa }}</template> · Mở lúc {{ formatDateTime(caHienTai.thoiGianMo) }}
      </span>
      <span v-else>Chế độ xem · Chưa mở ca làm việc</span>
    </div>
    <button v-if="!dangTai && !caHienTai" type="button" class="shift-entry-button" @click="batDauMoCa">
      Mở ca
    </button>
    <button v-else-if="!dangTai && caHienTai" type="button" class="shift-entry-button" @click="batDauKetCa">
      Kết ca
    </button>

    <div v-if="moModal" class="shift-modal-backdrop" @click.self="moModal = false">
      <form class="shift-modal" @submit.prevent="xacNhanMoCa">
        <h2>Mở ca làm việc</h2>
        <p>Nhập số tiền kiểm đếm ban đầu trước khi bắt đầu ca.</p>
        <label for="shift-opening-cash">Tiền đầu ca</label>
        <div class="shift-cash-input">
          <input id="shift-opening-cash" v-model.number="form.tienDauCa" type="number" min="0" step="1000" required />
          <span>₫</span>
        </div>
        <small v-if="Number.isFinite(Number(form.tienDauCa))">{{ formatVnd(form.tienDauCa) }}</small>
        <label for="shift-opening-note">Ghi chú</label>
        <textarea id="shift-opening-note" v-model="form.ghiChu" maxlength="255" rows="2" placeholder="Không bắt buộc"></textarea>
        <div v-if="loi" class="shift-error">{{ loi }}</div>
        <div class="shift-modal-actions">
          <button type="button" class="shift-cancel" :disabled="dangMo" @click="moModal = false">Hủy</button>
          <button type="submit" class="shift-confirm" :disabled="dangMo">{{ dangMo ? 'Đang mở ca...' : 'Xác nhận mở ca' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.shift-entry { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 24px; border-bottom: 1px solid #fecaca; background: #fff7f7; color: #991b1b; font-size: 13px; }
.shift-entry-open { border-color: #bbf7d0; background: #f0fdf4; color: #166534; }
.shift-entry-status { display: flex; align-items: center; gap: 9px; }
.shift-entry-dot { width: 9px; height: 9px; flex: 0 0 9px; border-radius: 50%; background: #dc2626; }
.shift-entry-open .shift-entry-dot { background: #16a34a; }
.shift-entry-button, .shift-confirm { border: 0; border-radius: 7px; padding: 8px 14px; background: #b91c1c; color: #fff; font-weight: 600; cursor: pointer; }
.shift-entry-button:hover, .shift-confirm:hover { background: #991b1b; }
.shift-modal-backdrop { position: fixed; inset: 0; z-index: 2000; display: grid; place-items: center; padding: 16px; background: rgb(15 23 42 / 55%); }
.shift-modal { display: grid; gap: 10px; width: min(100%, 440px); padding: 24px; border-radius: 14px; background: #fff; box-shadow: 0 20px 50px rgb(15 23 42 / 25%); color: #0f172a; }
.shift-modal h2 { margin: 0; font-size: 20px; }
.shift-modal p { margin: 0 0 4px; color: #64748b; font-size: 13px; }
.shift-modal label { font-size: 13px; font-weight: 600; }
.shift-cash-input { display: flex; align-items: center; gap: 8px; }
.shift-cash-input input, .shift-modal textarea { box-sizing: border-box; width: 100%; padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 7px; font: inherit; }
.shift-cash-input span, .shift-modal small { color: #64748b; }
.shift-modal textarea { resize: vertical; }
.shift-error { color: #b91c1c; font-size: 13px; }
.shift-modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 6px; }
.shift-cancel { padding: 8px 14px; border: 1px solid #cbd5e1; border-radius: 7px; background: #fff; color: #334155; cursor: pointer; }
.shift-confirm:disabled, .shift-cancel:disabled { cursor: not-allowed; opacity: .6; }
@media (max-width: 600px) { .shift-entry { align-items: flex-start; flex-direction: column; padding: 10px 16px; } }
</style>
