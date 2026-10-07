<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createEmployee, getEmployee, getEmployeeFormData, updateEmployee } from '../api/employee'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id
const saving = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const provinces = ['', 'Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Cần Thơ']
const wards = ['', 'Phường 1', 'Phường 2', 'Phường 3', 'Phường 4', 'Phường 5']
const gioiTinhs = [
  { value: '', label: 'Chọn giới tính' },
  { value: 'NAM', label: 'Nam' },
  { value: 'NU', label: 'Nữ' },
  { value: 'KHAC', label: 'Khác' },
]
const trangThais = [
  { value: 'true', label: 'Đang hoạt động' },
  { value: 'false', label: 'Đã khóa' },
]

const vaiTros = ref([])

const form = ref({
  maNhanVien: '',
  tenTaiKhoan: '',
  matKhau: '',
  email: '',
  soDienThoai: '',
  ngaySinh: '',
  gioiTinh: '',
  idVaiTro: '',
  queQuan: '',
  phuong: '',
  diaChiCuThe: '',
  trangThai: 'true',
})

const validate = () => {
  if (!form.value.tenTaiKhoan.trim()) return 'Họ và tên không được để trống'
  if (form.value.tenTaiKhoan.trim().length > 100) return 'Họ và tên không được vượt quá 100 ký tự'
  if (form.value.maNhanVien.trim() && !/^NV\d{3,}$/.test(form.value.maNhanVien.trim())) {
    return 'Mã nhân viên không hợp lệ (định dạng NV001, NV002, ...)'
  }
  if (!form.value.idVaiTro) return 'Vui lòng chọn vai trò'
  if (form.value.email && !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(form.value.email.trim())) {
    return 'Email không hợp lệ'
  }
  if (form.value.email.trim().length > 150) return 'Email không được vượt quá 150 ký tự'
  if (form.value.soDienThoai && !/^(0|\+84)\d{8,10}$/.test(form.value.soDienThoai.trim())) {
    return 'Số điện thoại không hợp lệ'
  }
  if (form.value.soDienThoai.trim().length > 20) return 'Số điện thoại không được vượt quá 20 ký tự'
  if (form.value.matKhau && form.value.matKhau.length < 6) {
    return 'Mật khẩu phải có ít nhất 6 ký tự'
  }
  if (form.value.matKhau.length > 255) return 'Mật khẩu không được vượt quá 255 ký tự'
  if (form.value.diaChiCuThe.trim().length > 510) return 'Địa chỉ cụ thể không được vượt quá 510 ký tự'
  return ''
}

const buildPayload = () => ({
  maNhanVien: form.value.maNhanVien.trim() || null,
  tenTaiKhoan: form.value.tenTaiKhoan.trim(),
  matKhau: form.value.matKhau ? form.value.matKhau : null,
  email: form.value.email.trim() || null,
  soDienThoai: form.value.soDienThoai.trim() || null,
  ngaySinh: form.value.ngaySinh || null,
  gioiTinh: form.value.gioiTinh || null,
  idVaiTro: Number(form.value.idVaiTro),
  queQuan: form.value.queQuan || null,
  phuong: form.value.phuong || null,
  diaChiCuThe: form.value.diaChiCuThe.trim() || null,
  trangThai: form.value.trangThai === 'true',
})

const submit = async () => {
  const error = validate()
  if (error) {
    errorMsg.value = error
    return
  }
  errorMsg.value = ''
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit) {
      await updateEmployee(route.params.id, payload)
    } else {
      await createEmployee(payload)
    }
    router.push('/nhan-vien')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await getEmployeeFormData()
    vaiTros.value = data.vaiTros || []
    if (isEdit) {
      const nv = await getEmployee(route.params.id)
      form.value = {
        maNhanVien: nv.maNhanVien || '',
        tenTaiKhoan: nv.tenTaiKhoan || '',
        matKhau: '',
        email: nv.email || '',
        soDienThoai: nv.soDienThoai || '',
        ngaySinh: nv.ngaySinh || '',
        gioiTinh: nv.gioiTinh || '',
        idVaiTro: nv.idVaiTro || '',
        queQuan: nv.queQuan || '',
        phuong: nv.phuong || '',
        diaChiCuThe: nv.diaChiCuThe || '',
        trangThai: nv.trangThai === false ? 'false' : 'true',
      }
    }
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="create-wrap">
    <section class="create-card">
      <h2 class="create-title">{{ isEdit ? 'Cập nhật Tài Khoản Nhân Viên' : 'Tạo Tài Khoản Nhân Viên' }}</h2>

      <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>
      <p v-if="loading" class="form-error">Đang tải dữ liệu...</p>

      <div class="form-fields">
        <input v-model="form.maNhanVien" type="text" maxlength="50" placeholder="Mã nhân viên (để trống sẽ tự sinh)" />

        <input v-model="form.tenTaiKhoan" type="text" maxlength="100" placeholder="Họ và tên" />

        <input v-model="form.matKhau" type="password" maxlength="255" :placeholder="isEdit ? 'Mật khẩu mới (bỏ trống để giữ nguyên)' : 'Mật khẩu (tối thiểu 6 ký tự, mặc định 123456)'" />

        <input v-model="form.email" type="email" maxlength="150" placeholder="Email" />
        <input v-model="form.ngaySinh" type="date" placeholder="Ngày sinh" />
        <input v-model="form.soDienThoai" type="tel" maxlength="20" placeholder="Số điện thoại" />

        <select v-model="form.gioiTinh">
          <option v-for="item in gioiTinhs" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>

        <select v-model="form.idVaiTro">
          <option value="" disabled>Chọn vai trò</option>
          <option v-for="v in vaiTros" :key="v.id" :value="v.id">{{ v.ten }}</option>
        </select>

        <select v-if="isEdit" v-model="form.trangThai">
          <option v-for="item in trangThais" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>

        <select v-model="form.queQuan">
          <option v-for="item in provinces" :key="item" :value="item">{{ item || 'Chọn tỉnh' }}</option>
        </select>

        <select v-model="form.phuong">
          <option v-for="item in wards" :key="item" :value="item">{{ item || 'Chọn phường' }}</option>
        </select>

        <textarea v-model="form.diaChiCuThe" maxlength="510" placeholder="Địa chỉ cụ thể" rows="3"></textarea>
      </div>

      <div class="form-actions">
        <button class="btn-primary" type="button" :disabled="saving" @click="submit">
          {{ saving ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Thêm' }}
        </button>
        <button class="btn-cancel" type="button" @click="router.push('/nhan-vien')">Hủy</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.create-wrap {
  min-height: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 26px 12px 40px;
}

.create-card {
  width: min(620px, 100%);
  background: var(--white);
  border: 1px solid #ececef;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(20, 20, 22, 0.05);
  padding: 30px 44px 30px;
}

.create-title {
  text-align: center;
  font-size: 15.5px;
  font-weight: 700;
  color: #1d1d23;
  margin-bottom: 22px;
}

.form-error {
  background: #fdecec;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 7px;
  padding: 9px 12px;
  margin-bottom: 13px;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.form-fields input,
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

.form-fields input::placeholder,
.form-fields textarea::placeholder {
  color: #9a9aa3;
}

.form-fields input:focus,
.form-fields select:focus,
.form-fields textarea:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
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
</style>
