<script setup>
import { computed, onMounted, ref } from 'vue'
import SearchableSelect from '@/components/SearchableSelect.vue'
import { createAddress, deleteAddress, listAddresses, setDefaultAddress, updateAddress } from '../api/customer'
import { layDiaGioi, layPhuongXa, timTinh } from '../api/diaGioi'
import { batBuoc, isBlank, kiemTra, toiDa } from '../utils/validate'
import { notifySuccess, notifyWarning, notifyError, notifyDeleteConfirm } from '@/utils/notify'

const props = defineProps({
  customerId: { type: [Number, String], required: true },
})
const emit = defineEmits(['close', 'changed'])

const danhSach = ref([])
const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const showForm = ref(false)
const editingId = ref(null)

const tinhThanhs = ref([])
const phuongXas = ref([])
const diaGioiLoading = ref(false)
const diaGioiError = ref('')
let yeuCauHienTai = 0

const form = ref(trong())

function trong() {
  return {
    tenDiaChi: '',
    tinhThanhPho: '',
    phuong: '',
    diaChiCuThe: '',
    macDinh: false,
  }
}

const lineOf = (d) => [d.diaChiCuThe, d.phuong, d.thanhPho].filter(Boolean).join(', ') || '—'

const napDanhSach = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    danhSach.value = await listAddresses(props.customerId)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}

const napDiaGioi = async (tenTinh, tenPhuong) => {
  const myToken = ++yeuCauHienTai
  diaGioiLoading.value = true
  diaGioiError.value = ''
  try {
    const ds = await layDiaGioi()
    if (myToken !== yeuCauHienTai) return
    tinhThanhs.value = ds
    const tinh = timTinh(ds, tenTinh)
    phuongXas.value = await layPhuongXa(ds, tinh)
    if (tenPhuong && !phuongXas.value.some((p) => p.ten === tenPhuong)) {
      phuongXas.value = [{ code: 'khac', ten: tenPhuong }, ...phuongXas.value]
    }
  } catch (e) {
    if (myToken === yeuCauHienTai) diaGioiError.value = e.message || 'Không tải được danh sách địa lý'
  } finally {
    if (myToken === yeuCauHienTai) diaGioiLoading.value = false
  }
}

// Danh sách lựa chọn cho Select có tìm kiếm (giữ nguyên placeholder động)
const optsTinh = computed(() => [
  {
    value: '',
    label: diaGioiLoading.value ? 'Đang tải danh sách tỉnh/thành...' : 'Chọn tỉnh/thành phố',
  },
  ...tinhThanhs.value.map((t) => ({ value: t.ten, label: t.ten })),
])

const optsPhuong = computed(() => [
  {
    value: '',
    label: diaGioiLoading.value
      ? 'Đang tải...'
      : form.value.tinhThanhPho
        ? 'Chọn phường/xã'
        : 'Chọn tỉnh trước',
  },
  ...phuongXas.value.map((p) => ({ value: p.ten, label: p.ten })),
])

const chonTinh = async (tenTinh) => {
  form.value.tinhThanhPho = tenTinh || ''
  form.value.phuong = ''
  const myToken = ++yeuCauHienTai
  const ds = tinhThanhs.value.length ? tinhThanhs.value : await layDiaGioi().catch(() => null)
  if (myToken !== yeuCauHienTai || !ds) return
  diaGioiLoading.value = true
  try {
    phuongXas.value = await layPhuongXa(ds, timTinh(ds, tenTinh))
  } finally {
    diaGioiLoading.value = false
  }
}

const moForm = (d = null) => {
  editingId.value = d ? d.id : null
  form.value = d
    ? {
        tenDiaChi: d.tenChiChi || '',
        tinhThanhPho: d.thanhPho || '',
        phuong: d.phuong || '',
        diaChiCuThe: d.diaChiCuThe || '',
        macDinh: !!d.macDinh,
      }
    : trong()
  errorMsg.value = ''
  showForm.value = true
  napDiaGioi(form.value.tinhThanhPho, form.value.phuong)
}

const huyForm = () => {
  showForm.value = false
  editingId.value = null
  errorMsg.value = ''
}

const validate = () =>
  kiemTra([
    batBuoc(form.value.diaChiCuThe, 'Địa chỉ cụ thể'),
    toiDa(form.value.tenDiaChi, 100, 'Tên địa chỉ'),
    toiDa(form.value.tinhThanhPho, 100, 'Tỉnh/Thành phố'),
    toiDa(form.value.phuong, 100, 'Phường/Xã'),
    toiDa(form.value.diaChiCuThe, 255, 'Địa chỉ cụ thể'),
  ])

const luu = async () => {
  const loi = validate()
  if (loi) {
    errorMsg.value = loi
    notifyWarning('Thông tin chưa hợp lệ', loi)
    return
  }
  errorMsg.value = ''
  saving.value = true
  const isEditing = !!editingId.value
  const payload = {
    tenDiaChi: form.value.tenDiaChi.trim() || null,
    tinhThanhPho: form.value.tinhThanhPho || null,
    phuong: form.value.phuong || null,
    diaChiCuThe: form.value.diaChiCuThe.trim(),
    macDinh: form.value.macDinh,
    trangThai: true,
  }
  try {
    if (editingId.value) {
      await updateAddress(props.customerId, editingId.value, payload)
    } else {
      await createAddress(props.customerId, payload)
    }
    showForm.value = false
    editingId.value = null
    await napDanhSach()
    emit('changed')
    notifySuccess(isEditing ? 'Đã cập nhật địa chỉ thành công!' : 'Đã thêm mới địa chỉ thành công!')
  } catch (e) {
    errorMsg.value = e.message
    notifyError('Lỗi lưu địa chỉ', e.message)
  } finally {
    saving.value = false
  }
}

const xoa = async (d) => {
  const ok = await notifyDeleteConfirm(`địa chỉ ${d.maDiaChi || ''}`)
  if (!ok) return
  try {
    await deleteAddress(props.customerId, d.id)
    if (editingId.value === d.id) huyForm()
    await napDanhSach()
    emit('changed')
    notifySuccess('Đã xóa địa chỉ thành công!')
  } catch (e) {
    errorMsg.value = e.message
    notifyError('Không thể xóa địa chỉ', e.message)
  }
}

const datMacDinh = async (d) => {
  try {
    await setDefaultAddress(props.customerId, d.id)
    await napDanhSach()
    emit('changed')
    notifySuccess('Đã đổi địa chỉ mặc định!')
  } catch (e) {
    errorMsg.value = e.message
    notifyError('Lỗi thiết lập địa chỉ', e.message)
  }
}

onMounted(() => {
  napDanhSach()
  napDiaGioi('', '')
})
</script>

<template>
  <div class="modal-mask" @click.self="emit('close')">
    <div class="modal-card">
      <div class="modal-head">
        <h3>Địa chỉ khách hàng</h3>
        <button class="modal-close" type="button" aria-label="Đóng" @click="emit('close')">×</button>
      </div>

      <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

      <div class="addr-toolbar">
        <span class="addr-count">{{ danhSach.length }} địa chỉ</span>
        <button v-if="!showForm" class="btn-add" type="button" @click="moForm()">+ Thêm địa chỉ</button>
      </div>

      <div v-if="showForm" class="addr-form">
        <div class="field">
          <label class="field-label">Tên địa chỉ</label>
          <input v-model="form.tenDiaChi" type="text" maxlength="100" placeholder="Nhà riêng, Văn phòng..." />
        </div>
        <div class="field">
          <label class="field-label">Tỉnh/Thành phố</label>
          <SearchableSelect
            v-model="form.tinhThanhPho"
            :options="optsTinh"
            :disabled="diaGioiLoading"
            placeholder="Chọn tỉnh/thành phố"
            @change="chonTinh"
          />
          <p v-if="diaGioiError" class="field-hint is-error">
            {{ diaGioiError }}
            <button type="button" class="link-retry" @click="napDiaGioi(form.tinhThanhPho, form.phuong)">Thử lại</button>
          </p>
        </div>
        <div class="field">
          <label class="field-label">Phường/Xã</label>
          <SearchableSelect
            v-model="form.phuong"
            :options="optsPhuong"
            :disabled="diaGioiLoading || !form.tinhThanhPho"
            placeholder="Chọn phường/xã"
          />
        </div>
        <div class="field">
          <label class="field-label">Địa chỉ cụ thể <b class="req">*</b></label>
          <textarea v-model="form.diaChiCuThe" maxlength="255" rows="2" placeholder="Số nhà, đường..."></textarea>
        </div>
        <label class="check-line">
          <input v-model="form.macDinh" type="checkbox" />
          Đặt làm địa chỉ mặc định
        </label>
        <div class="addr-actions">
          <button class="btn-primary" type="button" :disabled="saving" @click="luu">
            {{ saving ? 'Đang lưu...' : editingId ? 'Cập nhật' : 'Thêm' }}
          </button>
          <button class="btn-cancel" type="button" @click="huyForm">Hủy</button>
        </div>
      </div>

      <p v-else-if="loading" class="empty">Đang tải...</p>
      <p v-else-if="danhSach.length === 0" class="empty">Chưa có địa chỉ nào</p>

      <ul v-else class="addr-list">
        <li v-for="d in danhSach" :key="d.id" class="addr-item">
          <div class="addr-main">
            <div class="addr-title">
              <span>{{ d.tenChiChi || 'Địa chỉ' }}</span>
              <span v-if="d.macDinh" class="pill pill-green">Mặc định</span>
              <span v-if="d.trangThai === false" class="pill pill-red">Ngừng dùng</span>
            </div>
            <div class="addr-sub">{{ lineOf(d) }}</div>
          </div>
          <div class="addr-acts">
            <button v-if="!d.macDinh" type="button" class="mini-btn" @click="datMacDinh(d)">Mặc định</button>
            <button type="button" class="mini-btn" @click="moForm(d)">Sửa</button>
            <button type="button" class="mini-btn is-red" @click="xoa(d)">Xóa</button>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(17, 17, 20, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 60;
  padding: 20px;
}

.modal-card {
  width: min(640px, 100%);
  max-height: 86vh;
  overflow: auto;
  background: #fff;
  border-radius: 14px;
  padding: 22px 24px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.modal-head h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1d1d23;
}

.modal-close {
  border: none;
  background: #f2f2f5;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 19px;
  line-height: 1;
  cursor: pointer;
  color: #4a4a52;
}

.form-error {
  background: #fdecec;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 7px;
  padding: 9px 12px;
  margin-bottom: 12px;
}

.addr-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.addr-count {
  font-size: 12.5px;
  color: #6b7280;
}

.btn-add {
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 7px;
  background: var(--red);
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.addr-form {
  border: 1px solid #ececef;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 11px;
  margin-bottom: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #3d3d45;
}

.field-label .req {
  color: var(--red);
}

.field input,
.field select,
.field .ss,
.field textarea {
  width: 100%;
  border: 1px solid #e3e3e8;
  border-radius: 7px;
  padding: 8px 11px;
  font-size: 13px;
  color: #4a4a52;
  outline: none;
  background: #fff;
}

.field textarea {
  resize: vertical;
  line-height: 1.5;
}

.field input:focus,
.field select:focus,
.field .ss:focus,
.field .ss.is-open,
.field textarea:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.field-hint {
  font-size: 12px;
  color: #6b7280;
}

.field-hint.is-error {
  color: #dc2626;
}

.link-retry {
  border: none;
  background: none;
  color: var(--red);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
  margin-left: 6px;
}

.check-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #4a4a52;
}

.addr-actions {
  display: flex;
  gap: 9px;
  justify-content: flex-end;
}

.btn-primary {
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 7px;
  background: var(--red);
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.6;
}

.btn-cancel {
  height: 34px;
  padding: 0 16px;
  border: none;
  border-radius: 7px;
  background: #6b7280;
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.empty {
  text-align: center;
  color: #6b7280;
  font-size: 13px;
  padding: 18px 0;
}

.addr-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.addr-item {
  border: 1px solid #ececef;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.addr-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 700;
  color: #1d1d23;
}

.addr-sub {
  font-size: 12.5px;
  color: #4a4a52;
  margin-top: 3px;
}

.addr-acts {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.mini-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #e3e3e8;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  font-weight: 600;
  color: #4a4a52;
  cursor: pointer;
}

.mini-btn.is-red {
  border-color: #f6c8c8;
  color: var(--red);
}

.mini-btn:hover {
  background: #f7f7f9;
}

.pill {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}

.pill-green {
  background: #e7f7ee;
  color: #137a45;
}

.pill-red {
  background: #fdeaea;
  color: #b91c1c;
}
</style>
