<script setup>
import { computed, onMounted, ref } from 'vue'
import { listVouchers, suDungCuaToi } from '../api/voucher'
import { getToken } from '../utils/auth'

const showFilter = ref(true)
const loading = ref(true)
const error = ref('')
const vouchers = ref([])
const suDungMap = ref({})
const capNhatLuc = ref('')
const coDangNhap = ref(!!getToken())

const loc = ref({ ma: '', idDot: '', trangThai: '' })
const locDaApDung = ref({ ma: '', idDot: '', trangThai: '' })

const TRANG_THAI = ['Đang hoạt động', 'Sắp diễn ra', 'Hết hạn', 'Ngừng hoạt động']

const statusCua = (v) => {
  if (!v.trangThai) return 'Ngừng hoạt động'
  const now = Date.now()
  const batDau = v.ngayBatDau ? new Date(v.ngayBatDau).getTime() : 0
  const ketThuc = v.ngayKetThuc ? new Date(v.ngayKetThuc).getTime() : Infinity
  if (now < batDau) return 'Sắp diễn ra'
  if (now > ketThuc) return 'Hết hạn'
  return 'Đang hoạt động'
}

const statusPill = (value) => {
  if (value === 'Đang hoạt động') return 'pill-green'
  if (value === 'Sắp diễn ra') return 'pill-amber'
  return 'pill-gray'
}

const ngayHienThi = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`
}

const chuSoDau = (v) => {
  if (v.loaiGiamGia === 'PERCENT') return String(Number(v.giaTriGiam))
  return String(Math.floor(Number(v.giaTriGiam) / 1000)).slice(0, 2)
}

const cacDot = computed(() => {
  const map = new Map()
  for (const v of vouchers.value) {
    if (v.dotGiamGia) map.set(v.dotGiamGia.id, v.dotGiamGia.ten)
  }
  return [...map.entries()].map(([id, ten]) => ({ id, ten }))
})

const rows = computed(() => {
  const ap = locDaApDung.value
  return vouchers.value
    .map((v) => {
      const su = suDungMap.value[v.id]
      return {
        key: v.id,
        dotId: v.dotGiamGia ? v.dotGiamGia.id : null,
        name: v.ten,
        code: v.ma,
        sub: `Tối đa ${v.gioiHanMoiTaiKhoan || 1} lần/khách · Còn ${Math.max(
          0,
          (v.soLuong ?? 0) - (v.soLuongDaSuDung ?? 0),
        )} phiếu`,
        initials: chuSoDau(v),
        avatar: v.loaiGiamGia === 'PERCENT' ? '#cc0000' : '#3b82f6',
        event: v.dotGiamGia ? `Đợt: ${v.dotGiamGia.ten}` : 'Không thuộc đợt',
        discount: v.ten,
        used: coDangNhap.value && su
          ? `${su.soLanDaDung}/${su.gioiHanMoiTaiKhoan} lần`
          : `${v.soLuongDaSuDung ?? 0}/${v.soLuong ?? 0}`,
        expiry: ngayHienThi(v.ngayKetThuc),
        status: statusCua(v),
      }
    })
    .filter((r) => !ap.ma || r.code.toLowerCase().includes(ap.ma.toLowerCase()))
    .filter((r) => !ap.idDot || String(r.dotId) === ap.idDot)
    .filter((r) => !ap.trangThai || r.status === ap.trangThai)
})

const apDungBoLoc = () => {
  locDaApDung.value = { ...loc.value }
}

const datLai = () => {
  loc.value = { ma: '', idDot: '', trangThai: '' }
  locDaApDung.value = { ...loc.value }
}

const tai = async () => {
  loading.value = true
  error.value = ''
  try {
    vouchers.value = await listVouchers()
    capNhatLuc.value = new Date().toLocaleString('vi-VN')
    if (coDangNhap.value) {
      try {
        const danhSach = await suDungCuaToi()
        suDungMap.value = Object.fromEntries(danhSach.map((s) => [s.idPhieuGiamGia, s]))
      } catch {
        suDungMap.value = {}
      }
    } else {
      suDungMap.value = {}
    }
  } catch (e) {
    error.value = e.message || 'Không tải được danh sách phiếu giảm giá'
  } finally {
    loading.value = false
  }
}

onMounted(tai)
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý phiếu giảm giá</h1>
        <p class="screen-sub">Danh sách phiếu giảm giá và giới hạn sử dụng theo từng tài khoản</p>
      </div>
    </div>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Tìm kiếm &amp; bộ lọc</h3>
        <div class="panel-right">
          <span class="result-count">{{ rows.length }} kết quả</span>
          <button class="link-red" type="button" @click="showFilter = !showFilter">
            {{ showFilter ? 'Ẩn bớt' : 'Hiện thêm' }}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path :d="showFilter ? 'M6 14.5l6-6 6 6' : 'M6 9.5l6 6 6-6'" />
            </svg>
          </button>
        </div>
      </div>

      <div v-show="showFilter" class="filter-row">
        <div class="f-item">
          <label>Mã phiếu</label>
          <input v-model="loc.ma" type="text" placeholder="Nhập mã phiếu giảm giá" />
        </div>

        <div class="f-item">
          <label>Đợt giảm giá</label>
          <select v-model="loc.idDot">
            <option value="">Tất cả</option>
            <option v-for="dot in cacDot" :key="dot.id" :value="String(dot.id)">{{ dot.ten }}</option>
          </select>
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select v-model="loc.trangThai">
            <option value="">Tất cả</option>
            <option v-for="tt in TRANG_THAI" :key="tt" :value="tt">{{ tt }}</option>
          </select>
        </div>

        <div class="filter-actions">
          <button class="btn-reset" type="button" aria-label="Đặt lại" @click="datLai">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
          </button>
          <button class="btn-apply" type="button" @click="apDungBoLoc">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            Áp dụng
          </button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon is-red">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"
            />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách phiếu giảm giá</h3>
        <span class="count-pill">{{ vouchers.length }} phiếu</span>
        <div class="panel-right">
          <span v-if="capNhatLuc" class="panel-meta">Cập nhật lúc {{ capNhatLuc }}</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Phiếu giảm giá</th>
              <th>Đợt giảm giá</th>
              <th>Mức giảm</th>
              <th>Đã dùng</th>
              <th>Hạn dùng</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="state-cell">Đang tải dữ liệu...</td>
            </tr>
            <tr v-else-if="error">
              <td colspan="6" class="state-cell is-error">
                {{ error }}
                <button class="link-red" type="button" @click="tai">Thử lại</button>
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td colspan="6" class="state-cell">Không có phiếu giảm giá nào</td>
            </tr>
            <tr v-for="row in rows" :key="row.key">
              <td>
                <div class="entity">
                  <span class="entity-avatar" :style="{ background: row.avatar }">{{ row.initials }}</span>
                  <div class="entity-info">
                    <div class="entity-line">
                      <span class="entity-name">{{ row.name }}</span>
                      <span class="entity-code">{{ row.code }}</span>
                    </div>
                    <div class="entity-sub">{{ row.sub }}</div>
                  </div>
                </div>
              </td>
              <td>{{ row.event }}</td>
              <td class="cell-strong">{{ row.discount }}</td>
              <td>{{ row.used }}</td>
              <td>{{ row.expiry }}</td>
              <td><span class="pill" :class="statusPill(row.status)">{{ row.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">Hiển thị {{ rows.length }} trong {{ vouchers.length }} phiếu giảm giá</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.state-cell {
  text-align: center;
  color: #9a9aa3;
  padding: 24px 0;
  font-size: 13.5px;
}

.state-cell.is-error {
  color: #dc2626;
  font-weight: 600;
}
</style>
