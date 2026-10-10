<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createCustomer, getCustomer, suggestCustomerCode, updateCustomer } from '../api/customer'
import { layDiaGioi, layPhuongXa, timTinh } from '../api/diaGioi'
import AvatarCard from '../components/AvatarCard.vue'
import { setFlash } from '../utils/flash'
import { hopLeMa, maDeXuat } from '../utils/maNhanVien'
import { datVaiTro, layNguoiDung, vaiTroHienTai } from '../utils/session'
import {
  batBuoc,
  duTuoiToiThieu,
  emailHopLe,
  isBlank,
  kiemTra,
  khongDuocTuongLai,
  matKhauHopLe,
  soDienThoaiHopLe,
  toiDa,
} from '../utils/validate'

const TUOI_TOI_THIEU = 16
const THONG_BAO_TUOI = 'Khách hàng phải đủ 16 tuổi.'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id
const saving = ref(false)
const loading = ref(false)
const errorMsg = ref('')

/** Chỉ Nam/Nữ theo yêu cầu; value giữ nguyên NAM/NU để khớp payload API. */
const gioiTinhOptions = [
  { value: 'NAM', label: 'Nam' },
  { value: 'NU', label: 'Nữ' },
]
const trangThais = [
  { value: 'true', label: 'Đang hoạt động' },
  { value: 'false', label: 'Đã khóa' },
]

const tinhThanhs = ref([])
const phuongXas = ref([])
const diaGioiLoading = ref(false)
const diaGioiError = ref('')
const maTuDong = ref(true)
const dangGoiMa = ref(false)
const tenDaGoiMa = ref('')
const daNopForm = ref(false)
const ngaySinhDaCham = ref(false)
let yeuCauHienTai = 0
let yeuCauMa = 0

const form = ref({
  maKhachHang: '',
  tenKhachHang: '',
  tenTaiKhoan: '',
  matKhau: '',
  email: '',
  soDienThoai: '',
  ngaySinh: '',
  gioiTinh: '',
  trangThai: 'true',
  tinhThanhPho: '',
  phuong: '',
  diaChiCuThe: '',
})

const maPreview = computed(() => (form.value.tenKhachHang.trim() ? maDeXuat(form.value.tenKhachHang) : ''))

const loiNgaySinh = computed(() =>
  kiemTra([
    khongDuocTuongLai(form.value.ngaySinh, 'Ngày sinh'),
    duTuoiToiThieu(form.value.ngaySinh, TUOI_TOI_THIEU, THONG_BAO_TUOI),
  ]),
)

/** Hiện lỗi ngay khi đã chọn ngày sinh; với ô trống chỉ hiện sau submit/blur. */
const hienThiLoiNgaySinh = computed(() =>
  !isBlank(form.value.ngaySinh) || daNopForm.value || ngaySinhDaCham.value
    ? loiNgaySinh.value
    : '',
)

const loiGioiTinh = computed(() => (isBlank(form.value.gioiTinh) ? 'Vui lòng chọn giới tính' : ''))

const chonVaiTro = (e) => {
  const id = e.target.value
  const v = id
  if (v) datVaiTro(v)
}

const validate = () =>
  kiemTra([
    batBuoc(form.value.tenKhachHang, 'Họ và tên'),
    toiDa(form.value.tenKhachHang, 200, 'Họ và tên'),
    toiDa(form.value.maKhachHang, 25, 'Mã khách hàng'),
    form.value.maKhachHang.trim() && !hopLeMa(form.value.maKhachHang)
      ? 'Mã khách hàng không hợp lệ (bắt đầu bằng chữ cái, chỉ chữ cái và số, tối đa 25 ký tự)'
      : '',
    batBuoc(form.value.email, 'Email'),
    toiDa(form.value.email, 150, 'Email'),
    emailHopLe(form.value.email),
    toiDa(form.value.soDienThoai, 10, 'Số điện thoại'),
    soDienThoaiHopLe(form.value.soDienThoai),
    khongDuocTuongLai(form.value.ngaySinh, 'Ngày sinh'),
    duTuoiToiThieu(form.value.ngaySinh, TUOI_TOI_THIEU, THONG_BAO_TUOI),
    loiGioiTinh.value,
    matKhauHopLe(form.value.matKhau, false),
    toiDa(form.value.matKhau, 127, 'Mật khẩu'),
    toiDa(form.value.tinhThanhPho, 100, 'Tỉnh/Thành phố'),
    toiDa(form.value.phuong, 100, 'Phường/Xã'),
    toiDa(form.value.diaChiCuThe, 255, 'Địa chỉ cụ thể'),
  ])

const buildPayload = () => ({
  maKhachHang: form.value.maKhachHang.trim() || null,
  tenKhachHang: form.value.tenKhachHang.trim(),
  matKhau: form.value.matKhau ? form.value.matKhau : null,
  email: form.value.email.trim(),
  soDienThoai: form.value.soDienThoai.trim() || null,
  ngaySinh: form.value.ngaySinh || null,
  gioiTinh: form.value.gioiTinh || null,
  trangThai: form.value.trangThai === 'true',
  tinhThanhPho: form.value.tinhThanhPho || null,
  phuong: form.value.phuong || null,
  diaChiCuThe: form.value.diaChiCuThe.trim() || null,
  nguoiCapNhat: layNguoiDung(),
})

const chonMaTuDong = async (khiNop = false) => {
  if (isEdit || !maTuDong.value) return
  const ten = form.value.tenKhachHang.trim()
  if (!ten) return
  if (!khiNop && dangGoiMa.value) return
  const myToken = ++yeuCauMa
  dangGoiMa.value = true
  try {
    const data = await suggestCustomerCode(ten)
    if (myToken === yeuCauMa && maTuDong.value) {
      form.value.maKhachHang = data.ma || maDeXuat(ten)
      tenDaGoiMa.value = ten
    }
  } catch {
    if (myToken === yeuCauMa && maTuDong.value) {
      form.value.maKhachHang = maDeXuat(ten)
      tenDaGoiMa.value = ten
    }
  } finally {
    dangGoiMa.value = false
  }
}

const submit = async () => {
  daNopForm.value = true
  const error = validate()
  if (error) {
    errorMsg.value = error
    return
  }
  errorMsg.value = ''
  // Họ tên đổi sau lần gọi trước -> cập nhật lại mã đề xuất trước khi lưu.
  if (!isEdit && maTuDong.value && tenDaGoiMa.value !== form.value.tenKhachHang.trim()) {
    await chonMaTuDong(true)
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit) {
      await updateCustomer(route.params.id, payload)
      setFlash('Đã cập nhật thông tin khách hàng')
    } else {
      const taoMoi = await createCustomer(payload)
      setFlash(`Đã thêm khách hàng ${taoMoi.maKhachHang} — ${taoMoi.tenKhachHang}`)
    }
    router.push('/khach-hang')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    saving.value = false
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
    phuongXas.value = layPhuongXa(ds, tinh)
    if (tenPhuong && !phuongXas.value.some((p) => p.ten === tenPhuong)) {
      phuongXas.value = [{ code: 'khac', ten: tenPhuong }, ...phuongXas.value]
    }
  } catch (e) {
    if (myToken === yeuCauHienTai) diaGioiError.value = e.message || 'Không tải được danh sách địa lý'
  } finally {
    if (myToken === yeuCauHienTai) diaGioiLoading.value = false
  }
}

const chonTinh = async (tenTinh) => {
  form.value.tinhThanhPho = tenTinh || ''
  form.value.phuong = ''
  const myToken = ++yeuCauHienTai
  const ds = tinhThanhs.value.length ? tinhThanhs.value : await layDiaGioi().catch(() => null)
  if (myToken !== yeuCauHienTai || !ds) return
  phuongXas.value = layPhuongXa(ds, timTinh(ds, tenTinh))
}

onMounted(async () => {
  if (!isEdit) {
    napDiaGioi('', '')
    return
  }
  loading.value = true
  try {
    const kh = await getCustomer(route.params.id)
    const diaChi = kh.diaChiMacDinh || {}
    form.value = {
      maKhachHang: kh.maKhachHang || '',
      tenKhachHang: kh.tenKhachHang || '',
      tenTaiKhoan: kh.tenTaiKhoan || '',
      matKhau: '',
      email: kh.email || '',
      soDienThoai: kh.soDienThoai || '',
      ngaySinh: kh.ngaySinh || '',
      gioiTinh: kh.gioiTinh || '',
      trangThai: kh.trangThai === false ? 'false' : 'true',
      tinhThanhPho: diaChi.thanhPho || '',
      phuong: diaChi.phuong || '',
      diaChiCuThe: diaChi.diaChiCuThe || '',
    }
    await napDiaGioi(form.value.tinhThanhPho, form.value.phuong)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="create-wrap">
    <h2 class="create-title">{{ isEdit ? 'Cập nhật Tài Khoản Khách Hàng' : 'Tạo Tài Khoản Khách Hàng' }}</h2>

    <div class="role-bar">
      <span>Đang thao tác với vai trò: <b>{{ vaiTroHienTai === 'ADMIN' ? 'Admin' : 'Nhân viên' }}</b></span>
      <select :value="vaiTroHienTai" @change="chonVaiTro($event)">
        <option value="ADMIN">Admin</option>
        <option value="STAFF">Nhân viên</option>
      </select>
    </div>

    <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>
    <p v-if="loading" class="form-error">Đang tải dữ liệu...</p>

    <div class="create-grid">
      <AvatarCard :ten="form.tenKhachHang" :phu="form.email || form.maKhachHang" />

      <div class="form-col">
        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </span>
            <h3 class="panel-title">Thông tin cơ bản</h3>
          </div>

          <div class="form-fields">
            <div class="field">
              <label class="field-label">Mã khách hàng</label>
              <input
                v-model="form.maKhachHang"
                type="text"
                maxlength="25"
                placeholder="Để trống sẽ tự sinh theo họ tên"
                @input="maTuDong = false"
              />
              <p v-if="!isEdit && maPreview" class="field-hint">Mã gợi ý theo họ tên: <b>{{ maPreview }}</b></p>
            </div>

            <div class="field">
              <label class="field-label">Họ và tên <b class="req">*</b></label>
              <input v-model="form.tenKhachHang" type="text" maxlength="200" placeholder="Nguyễn Thị Bình" @blur="chonMaTuDong()" />
            </div>

            <div class="field">
              <label class="field-label">Email <b class="req">*</b></label>
              <input v-model="form.email" type="email" maxlength="150" placeholder="khachhang@gmail.com" />
            </div>

            <div class="field">
              <label class="field-label">Mật khẩu</label>
              <input
                v-model="form.matKhau"
                type="password"
                maxlength="127"
                :placeholder="isEdit ? 'Mật khẩu mới (bỏ trống để giữ nguyên)' : 'Tối thiểu 6 ký tự, mặc định 123456'"
              />
            </div>

            <div class="field">
              <label class="field-label">Số điện thoại</label>
              <input v-model="form.soDienThoai" type="tel" maxlength="10" placeholder="0901234567" />
            </div>

            <div class="field">
              <label class="field-label">Ngày sinh</label>
              <input
                v-model="form.ngaySinh"
                type="date"
                @blur="ngaySinhDaCham = true"
              />
              <p v-if="hienThiLoiNgaySinh" class="field-hint is-error">{{ hienThiLoiNgaySinh }}</p>
            </div>

            <div class="field">
              <label class="field-label">Giới tính <b class="req">*</b></label>
              <div class="radio-row" role="radiogroup" aria-label="Giới tính">
                <label v-for="item in gioiTinhOptions" :key="item.value" class="radio-item">
                  <input v-model="form.gioiTinh" type="radio" name="gioiTinh" :value="item.value" />
                  <span class="radio-dot"></span>
                  <span>{{ item.label }}</span>
                </label>
              </div>
              <p v-if="daNopForm && loiGioiTinh" class="field-hint is-error">{{ loiGioiTinh }}</p>
            </div>

            <div v-if="isEdit" class="field">
              <label class="field-label">Trạng thái</label>
              <select v-model="form.trangThai">
                <option v-for="item in trangThais" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon is-red">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z" />
                <circle cx="12" cy="10" r="2.6" />
              </svg>
            </span>
            <h3 class="panel-title">Địa chỉ</h3>
          </div>

          <div class="form-fields">
            <div class="field">
              <label class="field-label">Tỉnh/Thành phố</label>
              <select v-model="form.tinhThanhPho" :disabled="diaGioiLoading" @change="chonTinh($event.target.value)">
                <option value="">
                  {{ diaGioiLoading ? 'Đang tải danh sách tỉnh/thành...' : 'Chọn tỉnh/thành phố' }}
                </option>
                <option v-for="t in tinhThanhs" :key="t.code" :value="t.ten">{{ t.ten }}</option>
              </select>
              <p v-if="diaGioiError" class="field-hint is-error">
                {{ diaGioiError }}
                <button type="button" class="link-retry" @click="napDiaGioi(form.tinhThanhPho, form.phuong)">Thử lại</button>
              </p>
            </div>

            <div class="field">
              <label class="field-label">Phường/Xã</label>
              <select v-model="form.phuong" :disabled="diaGioiLoading || !form.tinhThanhPho">
                <option value="">
                  {{ diaGioiLoading ? 'Đang tải...' : form.tinhThanhPho ? 'Chọn phường/xã' : 'Chọn tỉnh trước' }}
                </option>
                <option v-for="p in phuongXas" :key="p.code" :value="p.ten">{{ p.ten }}</option>
              </select>
            </div>

            <div class="field field-wide">
              <label class="field-label">Địa chỉ cụ thể</label>
              <textarea v-model="form.diaChiCuThe" maxlength="255" placeholder="Số nhà, đường..." rows="3"></textarea>
            </div>
          </div>
        </section>

        <div class="form-actions">
          <button class="btn-primary" type="button" :disabled="saving" @click="submit">
            {{ saving ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Thêm' }}
          </button>
          <button class="btn-cancel" type="button" @click="router.push('/khach-hang')">Hủy</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-wrap {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 6px 4px 40px;
}

.create-title {
  font-size: 17px;
  font-weight: 700;
  color: #1d1d23;
}

.create-grid {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.form-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.form-col .panel {
  padding: 18px 22px 22px;
}

.form-col .panel-head {
  padding-bottom: 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f3;
}

.role-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #f7f7f9;
  border: 1px solid #ececef;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 12.5px;
  color: #4a4a52;
}

.role-bar select {
  height: 30px;
  border: 1px solid #e3e3e8;
  border-radius: 6px;
  background: #fff;
  font-size: 12.5px;
  padding: 0 8px;
  color: #4a4a52;
  outline: none;
}

.form-error {
  background: #fdecec;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 7px;
  padding: 9px 12px;
}

.form-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.field-wide {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #3d3d45;
}

.field-label .req {
  color: var(--red);
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

.form-fields input:not([type='radio']),
.form-fields select,
.form-fields textarea {
  width: 100%;
  height: 38px;
  padding: 0 13px;
  border: 1px solid #e3e3e8;
  border-radius: 7px;
  background: #fff;
  font-size: 13px;
  color: #4a4a52;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.form-fields textarea {
  height: auto;
  min-height: 78px;
  padding: 10px 13px;
  resize: vertical;
  line-height: 1.5;
}

.form-fields input:not([type='radio'])::placeholder,
.form-fields textarea::placeholder {
  color: #9a9aa3;
}

.form-fields input:not([type='radio']):focus,
.form-fields select:focus,
.form-fields textarea:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.form-actions {
  display: flex;
  justify-content: flex-start;
  gap: 10px;
}

.btn-primary {
  height: 36px;
  min-width: 92px;
  padding: 0 22px;
  border: none;
  border-radius: 7px;
  background: var(--red);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-primary:hover {
  background: var(--red-dark);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: default;
}

.btn-cancel {
  height: 36px;
  min-width: 76px;
  padding: 0 18px;
  border: none;
  border-radius: 7px;
  background: #6b7280;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-cancel:hover {
  background: #59606b;
}

@media (max-width: 1024px) {
  .create-grid {
    grid-template-columns: 200px minmax(0, 1fr);
    gap: 12px;
  }

  .form-col .panel {
    padding: 16px 16px 18px;
  }

  .form-fields {
    gap: 12px 14px;
  }
}

@media (max-width: 760px) {
  .create-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-fields {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-actions {
    flex-wrap: wrap;
  }
}
</style>
