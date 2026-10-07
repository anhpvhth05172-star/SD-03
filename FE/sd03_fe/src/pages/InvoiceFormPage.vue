<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createInvoice, getInvoiceFormData, listInvoices } from '../api/invoice'
import { formatVnd } from '../utils/format'

const router = useRouter()
const saving = ref(false)
const loading = ref(false)

const statusOptions = [
  'Chờ xác nhận',
  'Đã xác nhận',
  'Chờ giao hàng',
  'Đang giao hàng',
  'Đã giao hàng',
  'Đã hoàn thành',
  'Đã hủy',
  'Đã hoàn tiền',
]

const options = ref({
  khachHangs: [],
  nhanViens: [],
  phuongThucThanhToans: [],
  phieuGiamGias: [],
  sanPhamChiTiets: [],
})

const newMaHoaDon = () => 'HD' + String(Date.now()).slice(-8)

const blankLine = () => ({ idSanPhamChiTiet: '', soLuong: 1, donGia: 0, ghiChu: '' })

const form = ref({
  maHoaDon: newMaHoaDon(),
  loaiDon: 'Tại quầy',
  phiVanChuyen: 0,
  tenKhachHang: '',
  soDienThoaiKhachHang: '',
  diaChiNhanHang: '',
  trangThai: 'Chờ xác nhận',
  ghiChu: '',
  idKhachHang: '',
  idNhanVien: '',
  idPhuongThucThanhToan: '',
  idPhieuGiamGia: '',
  chiTiet: [blankLine()],
})

const spctById = computed(() => {
  const map = {}
  for (const s of options.value.sanPhamChiTiets) map[s.id] = s
  return map
})

const pggById = computed(() => {
  const map = {}
  for (const p of options.value.phieuGiamGias) map[p.id] = p
  return map
})

const tongTien = computed(() =>
  form.value.chiTiet.reduce(
    (sum, line) => sum + (Number(line.soLuong) || 0) * (Number(line.donGia) || 0),
    0,
  ) + (Number(form.value.phiVanChuyen) || 0),
)

const giamGia = computed(() => {
  const pgg = pggById.value[form.value.idPhieuGiamGia]
  if (!pgg) return 0
  let giam =
    pgg.loaiGiamGia === 'PERCENT'
      ? Math.floor((tongTien.value * Number(pgg.giaTriGiam)) / 100)
      : Number(pgg.giaTriGiam) || 0
  if (Number(pgg.giamToiDa) > 0) giam = Math.min(giam, Number(pgg.giamToiDa))
  return Math.max(0, Math.min(giam, tongTien.value))
})

const canhbaoPgg = computed(() => {
  const pgg = pggById.value[form.value.idPhieuGiamGia]
  if (!pgg) return ''
  if (pgg.trangThai === false) return 'Phiếu giảm giá này không hoạt động'
  if (!form.value.idKhachHang) return 'Vui lòng chọn khách hàng để sử dụng phiếu giảm giá'
  if (
    pgg.soLuong !== null &&
    pgg.soLuong !== undefined &&
    Number(pgg.soLuongDaSuDung || 0) >= Number(pgg.soLuong)
  ) {
    return 'Phiếu giảm giá đã hết lượt sử dụng'
  }
  if (Number(pgg.hoaDonToiThieu) > 0 && tongTien.value < Number(pgg.hoaDonToiThieu)) {
    return `Hóa đơn cần từ ${formatVnd(pgg.hoaDonToiThieu)} mới dùng phiếu này`
  }
  const now = Date.now()
  if (pgg.ngayBatDau && now < new Date(pgg.ngayBatDau).getTime()) return 'Phiếu chưa đến ngày sử dụng'
  if (pgg.ngayKetThuc && now > new Date(pgg.ngayKetThuc).getTime()) return 'Phiếu đã hết hạn'
  return ''
})

const tienSauGiam = computed(() => Math.max(0, tongTien.value - giamGia.value))

const lineThanhTien = (line) => (Number(line.soLuong) || 0) * (Number(line.donGia) || 0)

const dongLoi = computed(() =>
  Object.keys(errors.value)
    .filter((k) => k.startsWith('dong'))
    .map((k) => ({ dong: Number(k.slice(4)) + 1, msg: errors.value[k] }))
    .sort((a, b) => a.dong - b.dong),
)

const addLine = () => form.value.chiTiet.push(blankLine())

const removeLine = (index) => {
  if (form.value.chiTiet.length === 1) return
  form.value.chiTiet.splice(index, 1)
}

const onProductChange = (line) => {
  const spct = spctById.value[line.idSanPhamChiTiet]
  if (spct) line.donGia = Number(spct.giaBan)
}

const onKhachHangChange = () => {
  const kh = options.value.khachHangs.find((k) => k.id === form.value.idKhachHang)
  if (kh) {
    form.value.tenKhachHang = kh.ten
    form.value.soDienThoaiKhachHang = kh.soDienThoai || ''
  }
}

const toNum = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

const buildPayload = () => ({
  maHoaDon: form.value.maHoaDon.trim(),
  loaiDon: form.value.loaiDon,
  phiVanChuyen: Number(form.value.phiVanChuyen) || 0,
  tenKhachHang: form.value.tenKhachHang.trim() || null,
  soDienThoaiKhachHang: form.value.soDienThoaiKhachHang.trim() || null,
  diaChiNhanHang: form.value.diaChiNhanHang.trim() || null,
  trangThai: form.value.trangThai,
  ghiChu: form.value.ghiChu.trim() || null,
  idKhachHang: toNum(form.value.idKhachHang),
  idNhanVien: toNum(form.value.idNhanVien),
  idPhuongThucThanhToan: toNum(form.value.idPhuongThucThanhToan),
  idPhieuGiamGia: toNum(form.value.idPhieuGiamGia),
  chiTiet: form.value.chiTiet.map((line) => ({
    idSanPhamChiTiet: toNum(line.idSanPhamChiTiet),
    soLuong: Number(line.soLuong),
    donGia: Number(line.donGia),
    ghiChu: line.ghiChu ? line.ghiChu.trim() : null,
    trangThai: true,
  })),
})

const errors = ref({})

const coKyTuDieuKhien = (s) =>
  [...s].some((c) => {
    const n = c.charCodeAt(0)
    return n <= 8 || n === 11 || n === 12 || (n >= 14 && n <= 31) || n === 127
  })

const validate = () => {
  const e = {}
  const ma = String(form.value.maHoaDon || '').trim()
  if (!ma) e.maHoaDon = 'Mã hóa đơn không được để trống'
  else if (ma.length > 50) e.maHoaDon = 'Mã hóa đơn quá độ dài tối đa (50 ký tự)'

  if (!form.value.loaiDon) e.loaiDon = 'Vui lòng chọn loại đơn'
  if (!form.value.trangThai) e.trangThai = 'Vui lòng chọn trạng thái'
  if (!form.value.idPhuongThucThanhToan) {
    e.idPhuongThucThanhToan = 'Vui lòng chọn phương thức thanh toán'
  }
  if (!form.value.idNhanVien) e.idNhanVien = 'Vui lòng chọn nhân viên'

  const ten = String(form.value.tenKhachHang || '').trim()
  if (!ten) e.tenKhachHang = 'Tên khách hàng không được để trống'
  else if (ten.length > 150) e.tenKhachHang = 'Tên khách hàng quá độ dài tối đa (150 ký tự)'
  else if (!/^[\p{L}\s.'-]+$/u.test(ten)) {
    e.tenKhachHang = 'Tên khách hàng chỉ được chứa chữ, khoảng trắng và dấu . \' -'
  }

  const sdt = String(form.value.soDienThoaiKhachHang || '').trim()
  if (!sdt) e.soDienThoaiKhachHang = 'Số điện thoại không được để trống'
  else if (!/^0\d{9,10}$/.test(sdt)) {
    e.soDienThoaiKhachHang = 'Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng 0'
  }

  const phiRaw = form.value.phiVanChuyen
  if (phiRaw === '' || phiRaw === null || phiRaw === undefined) {
    e.phiVanChuyen = 'Phí vận chuyển không được để trống'
  } else {
    const phi = Number(phiRaw)
    if (!Number.isFinite(phi)) e.phiVanChuyen = 'Phí vận chuyển chỉ được nhập số'
    else if (phi < 0) e.phiVanChuyen = 'Phí vận chuyển không được âm'
    else if (phi <= 1000) e.phiVanChuyen = 'Phí vận chuyển phải lớn hơn 1000'
  }

  const diaChi = String(form.value.diaChiNhanHang || '').trim()
  if (form.value.loaiDon === 'Online' && !diaChi) {
    e.diaChiNhanHang = 'Đơn online phải có địa chỉ nhận hàng'
  } else if (diaChi) {
    if (diaChi.length > 500) {
      e.diaChiNhanHang = 'Địa chỉ nhận hàng quá độ dài tối đa (500 ký tự)'
    } else if (!/^[\p{L}\p{N}\s.,\/-]+$/u.test(diaChi)) {
      e.diaChiNhanHang = 'Địa chỉ nhận hàng chứa ký tự không hợp lệ'
    }
  }

  const ghiChu = String(form.value.ghiChu || '').trim()
  if (ghiChu.length > 1000) e.ghiChu = 'Ghi chú quá độ dài tối đa (1000 ký tự)'
  else if (coKyTuDieuKhien(ghiChu)) e.ghiChu = 'Ghi chú chứa ký tự không hợp lệ'

  if (!form.value.chiTiet.length) {
    e.chiTiet = 'Vui lòng thêm ít nhất một sản phẩm'
  } else {
    const daChon = new Set()
    form.value.chiTiet.forEach((line, i) => {
      if (e['dong' + i]) return
      if (!line.idSanPhamChiTiet) {
        e['dong' + i] = 'Vui lòng chọn sản phẩm'
        return
      }
      if (daChon.has(line.idSanPhamChiTiet)) {
        const sp = spctById.value[line.idSanPhamChiTiet]
        e['dong' + i] = `Sản phẩm "${sp ? sp.ma : line.idSanPhamChiTiet}" đã tồn tại ở dòng trước`
        return
      }
      daChon.add(line.idSanPhamChiTiet)

      const soLuongTrong =
        line.soLuong === '' || line.soLuong === null || line.soLuong === undefined
      const soLuong = Number(line.soLuong)
      if (soLuongTrong || !Number.isInteger(soLuong) || soLuong < 1) {
        e['dong' + i] = 'Số lượng phải là số nguyên lớn hơn 0'
        return
      }

      const donGiaTrong =
        line.donGia === '' || line.donGia === null || line.donGia === undefined
      const donGia = Number(line.donGia)
      if (donGiaTrong || !Number.isFinite(donGia) || donGia <= 0) {
        e['dong' + i] = 'Đơn giá phải lớn hơn 0'
        return
      }

      const spct = spctById.value[line.idSanPhamChiTiet]
      if (spct && (spct.trangThai === false || spct.trangThaiSanPham === false)) {
        e['dong' + i] = `Sản phẩm "${spct.ma}" đã ngừng bán`
        return
      }
      if (spct && spct.soLuong !== null && spct.soLuong !== undefined && soLuong > Number(spct.soLuong)) {
        e['dong' + i] = `Sản phẩm "${spct.ma}" chỉ còn ${spct.soLuong} trong kho`
      }
    })
  }

  if (form.value.idKhachHang) {
    const kh = options.value.khachHangs.find((k) => k.id === form.value.idKhachHang)
    if (kh && kh.trangThai === false) e.khachHang = 'Khách hàng đã bị vô hiệu hóa'
  }

  if (canhbaoPgg.value) e.idPhieuGiamGia = canhbaoPgg.value

  errors.value = e
  return Object.keys(e).length === 0
}

const submit = async () => {
  if (!validate()) return
  saving.value = true
  try {
    const ma = form.value.maHoaDon.trim()
    const kiemTra = await listInvoices({ ma, size: 100 })
    if ((kiemTra.content || []).some((h) => h.maHoaDon === ma)) {
      errors.value = { ...errors.value, maHoaDon: `Mã hóa đơn "${ma}" đã tồn tại` }
      return
    }
    const payload = buildPayload()
    await createInvoice(payload)
    router.push('/hoa-don')
  } catch (e) {
    alert(e.message)
  } finally {
    saving.value = false
  }
}

const optionLabel = (s) =>
  `${s.ma} - ${s.tenSanPham} (${s.tenKichCo} / ${s.tenMau}) - ${formatVnd(s.giaBan)} - còn ${s.soLuong}` +
  (s.trangThai === false || s.trangThaiSanPham === false ? ' - ngừng bán' : '')

onMounted(async () => {
  loading.value = true
  try {
    options.value = await getInvoiceFormData()
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="create-wrap">
    <section class="create-card" v-if="!loading">
      <h2 class="create-title">Tạo hóa đơn</h2>

      <div class="section-label">Thông tin hóa đơn</div>
      <div class="form-grid">
        <div class="field">
          <label>Mã hóa đơn *</label>
          <input v-model="form.maHoaDon" type="text" placeholder="Mã hóa đơn" readonly />
          <div v-if="errors.maHoaDon" class="field-error">{{ errors.maHoaDon }}</div>
        </div>
        <div class="field">
          <label>Loại đơn *</label>
          <select v-model="form.loaiDon">
            <option value="Tại quầy">Tại quầy</option>
            <option value="Online">Online</option>
          </select>
          <div v-if="errors.loaiDon" class="field-error">{{ errors.loaiDon }}</div>
        </div>
        <div class="field">
          <label>Trạng thái *</label>
          <select v-model="form.trangThai">
            <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
          </select>
          <div v-if="errors.trangThai" class="field-error">{{ errors.trangThai }}</div>
        </div>
        <div class="field">
          <label>Phương thức thanh toán *</label>
          <select v-model="form.idPhuongThucThanhToan">
            <option value="">— Không chọn —</option>
            <option v-for="p in options.phuongThucThanhToans" :key="p.id" :value="p.id">{{ p.ten }}</option>
          </select>
          <div v-if="errors.idPhuongThucThanhToan" class="field-error">{{ errors.idPhuongThucThanhToan }}</div>
        </div>
        <div class="field">
          <label>Nhân viên *</label>
          <select v-model="form.idNhanVien">
            <option value="">— Không chọn —</option>
            <option v-for="n in options.nhanViens" :key="n.id" :value="n.id">{{ n.ten }}</option>
          </select>
          <div v-if="errors.idNhanVien" class="field-error">{{ errors.idNhanVien }}</div>
        </div>
        <div class="field">
          <label>Khách hàng</label>
          <select v-model="form.idKhachHang" @change="onKhachHangChange">
            <option value="">— Khách lẻ —</option>
            <option
              v-for="k in options.khachHangs"
              :key="k.id"
              :value="k.id"
              :disabled="k.trangThai === false"
            >
              {{ k.ten }} ({{ k.soDienThoai }}){{ k.trangThai === false ? ' - đã vô hiệu hóa' : '' }}
            </option>
          </select>
          <div v-if="errors.khachHang" class="field-error">{{ errors.khachHang }}</div>
        </div>
        <div class="field">
          <label>Tên khách hàng *</label>
          <input v-model="form.tenKhachHang" type="text" placeholder="Tên khách hàng" />
          <div v-if="errors.tenKhachHang" class="field-error">{{ errors.tenKhachHang }}</div>
        </div>
        <div class="field">
          <label>Số điện thoại *</label>
          <input v-model="form.soDienThoaiKhachHang" type="tel" placeholder="Số điện thoại" />
          <div v-if="errors.soDienThoaiKhachHang" class="field-error">{{ errors.soDienThoaiKhachHang }}</div>
        </div>
        <div class="field">
          <label>Phí vận chuyển (₫) *</label>
          <input v-model.number="form.phiVanChuyen" type="number" min="1001" placeholder="0" />
          <div v-if="errors.phiVanChuyen" class="field-error">{{ errors.phiVanChuyen }}</div>
        </div>
        <div class="field">
          <label>Phiếu giảm giá</label>
          <select v-model="form.idPhieuGiamGia">
            <option value="">— Không dùng —</option>
            <option
              v-for="p in options.phieuGiamGias"
              :key="p.id"
              :value="p.id"
              :disabled="p.trangThai === false"
            >
              {{ p.ten }}{{ p.trangThai === false ? ' (ngừng hoạt động)' : '' }}
            </option>
          </select>
          <div v-if="errors.idPhieuGiamGia" class="field-error">{{ errors.idPhieuGiamGia }}</div>
        </div>
        <div class="field field-wide">
          <label>Địa chỉ nhận hàng</label>
          <input v-model="form.diaChiNhanHang" type="text" placeholder="Địa chỉ nhận hàng" />
          <div v-if="errors.diaChiNhanHang" class="field-error">{{ errors.diaChiNhanHang }}</div>
        </div>
        <div class="field field-wide">
          <label>Ghi chú</label>
          <textarea v-model="form.ghiChu" rows="2" placeholder="Ghi chú"></textarea>
          <div v-if="errors.ghiChu" class="field-error">{{ errors.ghiChu }}</div>
        </div>
      </div>

      <div class="section-label">Chi tiết hóa đơn</div>
      <table class="line-table">
        <thead>
          <tr>
            <th class="col-sp">Sản phẩm *</th>
            <th class="col-sl">SL</th>
            <th class="col-gia">Đơn giá (₫)</th>
            <th class="col-tt">Thành tiền</th>
            <th class="col-x"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(line, i) in form.chiTiet" :key="i">
            <td class="col-sp">
              <select v-model="line.idSanPhamChiTiet" @change="onProductChange(line)">
                <option value="">— Chọn sản phẩm —</option>
                <option
                  v-for="s in options.sanPhamChiTiets"
                  :key="s.id"
                  :value="s.id"
                  :disabled="s.trangThai === false || s.trangThaiSanPham === false"
                >
                  {{ optionLabel(s) }}
                </option>
              </select>
            </td>
            <td class="col-sl"><input v-model.number="line.soLuong" type="number" min="1" /></td>
            <td class="col-gia"><input v-model.number="line.donGia" type="number" min="0" step="1000" /></td>
            <td class="col-tt amount">{{ formatVnd(lineThanhTien(line)) }}</td>
            <td class="col-x">
              <button
                class="line-remove"
                type="button"
                aria-label="Xóa dòng"
                :disabled="form.chiTiet.length === 1"
                @click="removeLine(i)"
              >
                ×
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <button class="line-add" type="button" @click="addLine">+ Thêm dòng sản phẩm</button>
      <div v-if="errors.chiTiet" class="field-error">{{ errors.chiTiet }}</div>
      <ul v-if="dongLoi.length" class="line-error-list">
        <li v-for="lo in dongLoi" :key="lo.dong">Dòng {{ lo.dong }}: {{ lo.msg }}</li>
      </ul>

      <div class="totals-box">
        <div class="t-row"><span>Tổng tiền</span><span>{{ formatVnd(tongTien) }}</span></div>
        <div class="t-row"><span>Giảm giá</span><span class="t-discount">- {{ formatVnd(giamGia) }}</span></div>
        <div v-if="canhbaoPgg" class="t-warning">{{ canhbaoPgg }}</div>
        <div class="t-row t-final"><span>Tiền sau giảm giá</span><span>{{ formatVnd(tienSauGiam) }}</span></div>
      </div>

      <div class="form-actions">
        <button class="btn-primary" type="button" :disabled="saving" @click="submit">
          {{ saving ? 'Đang lưu...' : 'Thêm' }}
        </button>
        <button class="btn-cancel" type="button" @click="router.push('/hoa-don')">Hủy</button>
      </div>
    </section>
    <section class="create-card" v-else>
      <p class="loading-text">Đang tải...</p>
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
  width: min(880px, 100%);
  background: var(--white);
  border: 1px solid #ececef;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(20, 20, 22, 0.05);
  padding: 28px 36px 30px;
}

.create-title {
  text-align: center;
  font-size: 15.5px;
  font-weight: 700;
  color: #1d1d23;
  margin-bottom: 20px;
}

.loading-text {
  text-align: center;
  color: #9a9aa3;
  padding: 30px 0;
}

.section-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--red);
  margin: 4px 0 12px;
  padding-left: 9px;
  border-left: 3px solid var(--red);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 13px 16px;
  margin-bottom: 22px;
}

.field label {
  display: block;
  font-size: 11.5px;
  color: #77777f;
  margin-bottom: 4px;
  font-weight: 500;
}

.field input,
.field select,
.field textarea {
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

.field textarea {
  height: auto;
  min-height: 62px;
  padding: 10px 13px;
  resize: vertical;
  line-height: 1.5;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.field-wide {
  grid-column: span 3;
}

.field-error {
  font-size: 11.5px;
  color: var(--red);
  margin-top: 4px;
  line-height: 1.35;
}

.field input[readonly] {
  background: #f7f7f9;
  color: #6b6b73;
  cursor: default;
}

.line-error-list {
  list-style: none;
  margin: 0 0 14px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.line-error-list li {
  font-size: 12px;
  color: var(--red);
  background: var(--red-soft);
  border-radius: 6px;
  padding: 6px 9px;
}

.line-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 10px;
}

.line-table th {
  font-size: 11.5px;
  color: #77777f;
  text-align: left;
  padding: 8px 8px;
  border-bottom: 1px solid #ececef;
}

.line-table td {
  padding: 8px;
  border-bottom: 1px solid #f3f3f5;
  vertical-align: middle;
}

.line-table input,
.line-table select {
  width: 100%;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #e3e3e8;
  border-radius: 7px;
  font-size: 12.5px;
  color: #4a4a52;
  background: #fff;
  outline: none;
}

.line-table input:focus,
.line-table select:focus {
  border-color: var(--red);
}

.col-sl {
  width: 78px;
}

.col-gia {
  width: 130px;
}

.col-tt {
  width: 130px;
}

.col-x {
  width: 44px;
}

.amount {
  font-weight: 600;
  color: #1d1d23;
  font-size: 13px;
}

.line-remove {
  border: none;
  background: #f3f3f5;
  color: #6b7280;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
}

.line-remove:hover:not(:disabled) {
  background: var(--red-soft);
  color: var(--red);
}

.line-remove:disabled {
  opacity: 0.35;
  cursor: default;
}

.line-add {
  border: 1px dashed #d5d5dc;
  background: #fafafd;
  color: var(--red);
  font-size: 12.5px;
  font-weight: 600;
  width: 100%;
  height: 36px;
  border-radius: 7px;
  cursor: pointer;
  margin-bottom: 18px;
  transition: background 0.15s ease;
}

.line-add:hover {
  background: var(--red-soft);
}

.totals-box {
  width: min(340px, 100%);
  margin-left: auto;
  background: #fafafd;
  border: 1px solid #ececef;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.t-row {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  color: #4a4a52;
}

.t-discount {
  color: var(--red);
  font-weight: 600;
}

.t-warning {
  font-size: 12px;
  color: #b45309;
  background: #fef3c7;
  border-radius: 6px;
  padding: 6px 9px;
}

.t-final {
  font-weight: 700;
  color: #1d1d23;
  font-size: 14.5px;
  border-top: 1px dashed #d5d5dc;
  padding-top: 8px;
}

.t-final span:last-child {
  color: var(--red);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
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

.btn-primary:hover:not(:disabled) {
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
