<script setup>
import { computed, ref } from 'vue'
import { notifySuccess, notifyWarning, notifyDeleteConfirm } from '@/utils/notify'
import SearchableSelect from '@/components/SearchableSelect.vue'
import {
  danhSachCa,
  themCa,
  capNhatCa,
  doiTrangThaiCa,
  caTrungTen,
  xoaCa,
  soLichCuaCa,
  slugMaCa,
} from './scheduleData'

/* ---------------------------------- Bộ lọc + bảng ---------------------------------- */
const fKeyword = ref('')
const fTrangThai = ref('')

const caLoc = computed(() => {
  const kw = fKeyword.value.trim().toLowerCase()
  let rows = [...danhSachCa]
  if (kw) {
    rows = rows.filter(
      (c) =>
        String(c.ten).toLowerCase().includes(kw) || String(c.ma).toLowerCase().includes(kw)
    )
  }
  if (fTrangThai.value === 'bat') rows = rows.filter((c) => c.dangBat)
  if (fTrangThai.value === 'tat') rows = rows.filter((c) => !c.dangBat)
  return rows
})

const tongCa = computed(() => danhSachCa.length)
const soDangBat = computed(() => danhSachCa.filter((c) => c.dangBat).length)

const datLaiBoLoc = () => {
  fKeyword.value = ''
  fTrangThai.value = ''
}

/* ---------------------------------- Thao tác ---------------------------------- */
const doiTrangThaiBtn = (ca) => {
  doiTrangThaiCa(ca.id)
  notifySuccess(
    ca.dangBat ? 'Đã bật ca làm' : 'Đã tắt ca làm',
    `${ca.ten} (${ca.batDau} - ${ca.ketThuc})`
  )
}

const xoaCaBtn = async (ca) => {
  const n = soLichCuaCa(ca.id)
  if (n > 0) {
    notifyWarning(
      'Không thể xóa ca làm',
      `Còn ${n} lịch làm việc đang gắn với ca "${ca.ten}" — hãy xóa hoặc đổi các lịch đó trước`
    )
    return
  }
  const ok = await notifyDeleteConfirm(`ca làm "${ca.ten}"`)
  if (!ok) return
  xoaCa(ca.id)
  notifySuccess('Đã xóa ca làm', ca.ten)
}

/* ---------------------------------- Form thêm / sửa ---------------------------------- */
const showForm = ref(false)
const formMode = ref('them')
const form = ref({ id: null, ten: '', batDau: '08:00', ketThuc: '12:00' })
const formError = ref('')

const moFormThem = () => {
  formMode.value = 'them'
  form.value = { id: null, ten: '', batDau: '08:00', ketThuc: '12:00' }
  formError.value = ''
  showForm.value = true
}

const moFormSua = (ca) => {
  formMode.value = 'sua'
  form.value = { id: ca.id, ten: ca.ten, batDau: ca.batDau, ketThuc: ca.ketThuc }
  formError.value = ''
  showForm.value = true
}

const luuForm = () => {
  formError.value = ''
  const f = form.value
  const ten = f.ten.trim()
  if (!ten) {
    formError.value = 'Vui lòng nhập tên ca làm'
    return
  }
  if (caTrungTen(ten, formMode.value === 'sua' ? f.id : null)) {
    formError.value = 'Tên ca làm đã tồn tại — vui lòng chọn tên khác'
    return
  }
  if (!f.batDau || !f.ketThuc) {
    formError.value = 'Vui lòng chọn giờ bắt đầu và giờ kết thúc'
    return
  }
  if (f.batDau >= f.ketThuc) {
    formError.value = 'Giờ kết thúc phải sau giờ bắt đầu'
    return
  }
  const payload = { ten, batDau: f.batDau, ketThuc: f.ketThuc, ma: slugMaCa(ten) }
  if (formMode.value === 'sua') {
    capNhatCa(f.id, payload)
    notifySuccess('Đã cập nhật ca làm', `${ten} (${f.batDau} - ${f.ketThuc})`)
  } else {
    themCa(payload)
    notifySuccess('Đã thêm ca làm', `${ten} (${f.batDau} - ${f.ketThuc})`)
  }
  showForm.value = false
}
</script>

<template>
  <div class="shift-page-container">
    <!-- CARD BỘ LỌC -->
    <div class="filter-card">
      <div class="card-title-row">
        <div class="icon-circle icon-filter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
          </svg>
        </div>
        <span class="card-title-text">Bộ lọc</span>
      </div>

      <div class="filter-controls-row">
        <div class="field-block grow">
          <div class="search-input-wrap">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input v-model="fKeyword" type="text" class="input-search" placeholder="Tìm kiếm tên ca, mã ca..." />
          </div>
        </div>

        <div class="inline-field">
          <label class="inline-label">Trạng thái:</label>
          <SearchableSelect
            v-model="fTrangThai"
            :options="[
              { value: '', label: 'Tất cả' },
              { value: 'bat', label: 'Đang bật' },
              { value: 'tat', label: 'Đang tắt' }
            ]"
            value-key="value"
            label-key="label"
            placeholder="Tất cả"
            search-placeholder="Tìm trạng thái..."
            style="width: auto; min-width: 150px"
            @change="(v) => { fTrangThai = v ?? '' }" />
        </div>
      </div>

      <div class="filter-actions-row is-left">
        <button class="btn-action-outline is-muted" type="button" @click="datLaiBoLoc">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
          </svg>
          Đặt lại bộ lọc
        </button>

        <div class="actions-spacer"></div>

        <button class="btn-action-primary" type="button" @click="moFormThem">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Thêm ca làm
        </button>
      </div>
    </div>

    <!-- CARD DANH SÁCH -->
    <div class="table-card">
      <div class="card-title-row space-between">
        <div class="title-with-icon">
          <div class="icon-circle icon-list">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          </div>
          <span class="card-title-text">Danh sách ca làm</span>
        </div>
        <div class="stat-pills">
          <span class="stat-pill">Tổng ca: <b>{{ tongCa }}</b></span>
          <span class="stat-pill is-on">Đang bật: <b>{{ soDangBat }}</b></span>
          <span class="stat-pill is-off">Đang tắt: <b>{{ tongCa - soDangBat }}</b></span>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 55px">STT</th>
              <th style="width: 120px">Mã ca</th>
              <th>Tên ca</th>
              <th style="width: 170px">Thời gian</th>
              <th style="width: 120px">Số lịch</th>
              <th style="width: 120px">Trạng thái</th>
              <th class="text-center" style="width: 150px">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="caLoc.length === 0">
              <td colspan="7" class="text-center empty-row">Không có ca làm nào phù hợp</td>
            </tr>
            <tr v-for="(ca, i) in caLoc" :key="ca.id" class="row-alt">
              <td class="cell-stt">{{ i + 1 }}</td>
              <td class="cell-code"><span class="code-chip">{{ ca.ma }}</span></td>
              <td class="cell-name">{{ ca.ten }}</td>
              <td>{{ ca.batDau }} - {{ ca.ketThuc }}</td>
              <td>{{ soLichCuaCa(ca.id) }} lịch</td>
              <td>
                <span class="status-chip" :class="ca.dangBat ? 'is-on' : 'is-off'">
                  {{ ca.dangBat ? 'Đang bật' : 'Đang tắt' }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-buttons-group">
                  <label class="switch" :title="ca.dangBat ? 'Tắt ca làm' : 'Bật ca làm'">
                    <input type="checkbox" :checked="ca.dangBat" @click.prevent="doiTrangThaiBtn(ca)" />
                    <span class="slider"></span>
                  </label>
                  <button class="btn-action-circle is-view" type="button" title="Chỉnh sửa" @click="moFormSua(ca)">
                    <i class="bi bi-pencil"></i>
                  </button>
                  <button class="btn-action-circle is-delete" type="button" title="Xóa ca làm" @click="xoaCaBtn(ca)">
                    <i class="bi bi-trash3"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL THÊM / SỬA -->
    <div v-if="showForm" class="modal-backdrop" @click.self="showForm = false">
      <div class="modal-card">
        <div class="modal-banner">
          <div class="modal-banner-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          </div>
          <div class="modal-banner-text">
            <h3>{{ formMode === 'them' ? 'Thêm mới ca làm' : 'Cập nhật ca làm' }}</h3>
            <p>Tên ca · Giờ bắt đầu · Giờ kết thúc</p>
          </div>
          <button class="btn-banner-close" type="button" @click="showForm = false">×</button>
        </div>

        <div class="modal-body">
          <div class="form-field">
            <label class="form-label">Tên ca làm <span class="req">*</span></label>
            <input v-model="form.ten" type="text" class="form-control" placeholder="VD: Ca sáng, Ca chiều..." maxlength="50" />
          </div>

          <div class="form-row-2">
            <div class="form-field">
              <label class="form-label">Giờ bắt đầu <span class="req">*</span></label>
              <input v-model="form.batDau" type="time" class="form-control" />
            </div>
            <div class="form-field">
              <label class="form-label">Giờ kết thúc <span class="req">*</span></label>
              <input v-model="form.ketThuc" type="time" class="form-control" />
            </div>
          </div>

          <p v-if="formError" class="form-error">
            <i class="bi bi-exclamation-circle"></i> {{ formError }}
          </p>
        </div>

        <div class="modal-footer">
          <button class="btn-ghost" type="button" @click="showForm = false">Hủy</button>
          <div class="footer-spacer"></div>
          <button class="btn-primary" type="button" @click="luuForm">
            {{ formMode === 'them' ? 'Thêm mới' : 'Lưu thay đổi' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shift-page-container {
  padding: 16px 20px 40px;
  background-color: #f8fafc;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: inherit;
}

.filter-card,
.table-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.card-title-row.space-between {
  justify-content: space-between;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.icon-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-circle svg {
  width: 16px;
  height: 16px;
}

.icon-filter {
  background: #f1f5f9;
  color: #475569;
}

.icon-list {
  background: #ffe4e6;
  color: #f43f5e;
}

.card-title-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

/* Bộ lọc */
.filter-controls-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.field-block.grow {
  flex: 1;
  min-width: 240px;
  display: flex;
  flex-direction: column;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.search-icon {
  position: absolute;
  left: 14px;
  width: 16px;
  height: 16px;
  color: #94a3b8;
  pointer-events: none;
}

.input-search {
  width: 100%;
  height: 42px;
  padding: 0 16px 0 42px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
  font-family: inherit;
}

.input-search:focus {
  border-color: #f43f5e;
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.input-search::placeholder {
  color: #94a3b8;
}

.filter-actions-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.filter-actions-row.is-left {
  justify-content: flex-start;
}

.actions-spacer {
  flex: 1;
}

.btn-action-outline {
  height: 38px;
  padding: 0 16px;
  border: 1px solid #ffe4e6;
  border-radius: 8px;
  background: #ffffff;
  color: #f43f5e;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-action-outline svg {
  width: 14px;
  height: 14px;
}

.btn-action-outline:hover {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #e11d48;
}

.btn-action-outline.is-muted {
  border-color: #e2e8f0;
  color: #94a3b8;
  background: #f8fafc;
}

.btn-action-primary {
  height: 38px;
  padding: 0 18px;
  background: linear-gradient(135deg, #ff3366, #e11d48);
  border: none;
  border-radius: 8px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.25);
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-action-primary svg {
  width: 15px;
  height: 15px;
}

.btn-action-primary:hover {
  background: linear-gradient(135deg, #f43f5e, #be123c);
  box-shadow: 0 6px 16px rgba(225, 29, 72, 0.35);
}

/* Pills thống kê */
.stat-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 20px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  font-size: 12.5px;
  color: #475569;
  font-weight: 500;
}

.stat-pill b {
  color: #0f172a;
  font-weight: 700;
}

.stat-pill.is-on {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #047857;
}

.stat-pill.is-on b {
  color: #047857;
}

.stat-pill.is-off {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #be123c;
}

.stat-pill.is-off b {
  color: #be123c;
}

/* Bảng */
.table-container {
  overflow-x: auto;
  width: 100%;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th {
  padding: 13px 12px;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  white-space: nowrap;
}

.data-table td {
  padding: 13px 12px;
  font-size: 13.5px;
  color: #334155;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background-color: #fafbfc;
}

.row-alt:nth-child(even) {
  background: #fafbfc;
}

.text-center {
  text-align: center;
}

.cell-stt {
  color: #475569;
  font-weight: 500;
}

.cell-code {
  font-weight: 600;
  color: #0f172a;
}

.cell-name {
  font-weight: 600;
  color: #0f172a;
}

.empty-row {
  color: #94a3b8;
  padding: 28px 12px !important;
}

.code-chip {
  display: inline-flex;
  padding: 3px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.status-chip {
  display: inline-flex;
  padding: 3px 11px;
  border-radius: 20px;
  font-size: 12.5px;
  font-weight: 600;
}

.status-chip.is-on {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #047857;
}

.status-chip.is-off {
  background: #fff1f2;
  border: 1px solid #fecdd3;
  color: #be123c;
}

.action-buttons-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-action-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #f1f5f9;
  background: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  font-size: 15px;
  line-height: 1;
  color: #475569;
}

.btn-action-circle.is-view:hover {
  background: #e2e8f0;
  color: #0f172a;
  transform: scale(1.08);
}

.btn-action-circle.is-delete {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fee2e2;
}

.btn-action-circle.is-delete:hover {
  background: #fee2e2;
  color: #b91c1c;
  transform: scale(1.08);
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  backdrop-filter: blur(4px);
  padding: 16px;
}

.modal-card {
  background: #ffffff;
  width: 100%;
  max-width: 480px;
  border-radius: 18px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  animation: modalIn 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-banner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 22px;
  background: linear-gradient(135deg, #fff1f2 0%, #ffe4e6 60%, #fecdd3 100%);
  border-bottom: 1px solid #f1f5f9;
}

.modal-banner-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #ffffff;
  color: #e11d48;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.modal-banner-icon svg {
  width: 20px;
  height: 20px;
}

.modal-banner-text h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.modal-banner-text p {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.btn-banner-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-banner-close:hover {
  background: #ffffff;
  color: #0f172a;
}

.modal-body {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 60vh;
  overflow-y: auto;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.req {
  color: #dc2626;
}

.form-control {
  height: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  padding: 0 14px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  font-family: inherit;
  transition: all 0.15s ease;
  width: 100%;
}

.form-control:focus {
  border-color: #f43f5e;
  box-shadow: 0 0 0 3px rgba(244, 63, 94, 0.1);
}

.form-error {
  margin: 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  font-size: 12.5px;
  font-weight: 600;
  color: #b91c1c;
  display: flex;
  align-items: center;
  gap: 6px;
}

.modal-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 22px;
  border-top: 1px solid #f1f5f9;
  background: #fafbfc;
}

.footer-spacer {
  flex: 1;
}

.btn-ghost {
  height: 38px;
  padding: 0 18px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.btn-ghost:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-primary {
  height: 38px;
  padding: 0 18px;
  background: linear-gradient(135deg, #ff3366, #e11d48);
  border: none;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s ease;
  font-family: inherit;
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.22);
}

.btn-primary:hover {
  background: linear-gradient(135deg, #f43f5e, #be123c);
}

@media (max-width: 767px) {
  .shift-page-container {
    padding: 14px 12px 32px;
  }

  .filter-card,
  .table-card {
    padding: 16px 14px;
  }

  .filter-controls-row {
    flex-direction: column;
    align-items: stretch;
  }

  .inline-field {
    justify-content: space-between;
  }

  .actions-spacer {
    display: none;
  }

  .form-row-2 {
    grid-template-columns: 1fr;
  }

  .stat-pills {
    width: 100%;
  }
}

.switch {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 21px;
  flex-shrink: 0;
  margin: 0;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e1;
  border-radius: 999px;
  transition: 0.25s;
}

.slider::before {
  position: absolute;
  content: "";
  width: 15px;
  height: 15px;
  left: 3px;
  top: 3px;
  background-color: white;
  border-radius: 50%;
  transition: 0.25s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.switch input:checked + .slider {
  background-color: #16a34a;
}

.switch input:checked + .slider::before {
  transform: translateX(17px);
}

.switch input:disabled + .slider {
  opacity: 0.5;
  cursor: not-allowed;
}

.switch input:focus-visible + .slider {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}
</style>
