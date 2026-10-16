<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { getUser, dangXuat, getRefreshToken } from '@/utils/auth'
import { huyPhien } from '@/api/auth'

const router = useRouter()
const nguoiDung = ref(getUser())
const vouchers = ref([])
const loading = ref(true)
const loi = ref('')

onMounted(async () => {
  try {
    const data = await http.get('/phieu-giam-gia/cua-toi').then((r) => r.data)
    vouchers.value = Array.isArray(data) ? data : []
  } catch (e) {
    loi.value = e.message || 'Không tải được phiếu giảm giá'
  } finally {
    loading.value = false
  }
})

const xulyDangXuat = async () => {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) await huyPhien(refreshToken)
  } catch {
    // Bỏ qua lỗi gọi API.
  }
  dangXuat()
  router.push('/dang-nhap')
}
</script>

<template>
  <div class="tk-page">
    <header class="tk-header">
      <img src="/images/logo_polyshoes_final.png" alt="PolyShoes" class="tk-logo" />
      <div class="tk-header-right">
        <span class="tk-user">{{ nguoiDung?.tenKhachHang || nguoiDung?.tenTaiKhoan }}</span>
        <button type="button" class="btn-logout" @click="xulyDangXuat">Đăng xuất</button>
      </div>
    </header>

    <main class="tk-body">
      <section class="tk-card">
        <h2>Thông tin tài khoản</h2>
        <dl class="tk-info">
          <div>
            <dt>Tên đăng nhập</dt>
            <dd>{{ nguoiDung?.tenTaiKhoan || '—' }}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{{ nguoiDung?.email || '—' }}</dd>
          </div>
        </dl>
      </section>

      <section class="tk-card">
        <h2>Phiếu giảm giá của tôi</h2>
        <p v-if="loading" class="tk-muted">Đang tải...</p>
        <p v-else-if="loi" class="tk-err">{{ loi }}</p>
        <p v-else-if="!vouchers.length" class="tk-muted">Bạn chưa có phiếu giảm giá nào.</p>
        <ul v-else class="tk-vouchers">
          <li v-for="p in vouchers" :key="p.idPhieuGiamGia" class="tk-voucher">
            <span class="tk-voucher-name">{{ p.ten || p.ma }}</span>
            <span class="tk-voucher-meta">
              Mã: {{ p.ma }} · Đã dùng {{ p.soLanDaDung || 0 }}/{{
                p.gioiHanMoiTaiKhoan ?? 1
              }}
            </span>
          </li>
        </ul>
      </section>
    </main>
  </div>
</template>

<style scoped>
.tk-page {
  min-height: 100vh;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
}

.tk-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.tk-logo {
  height: 34px;
  object-fit: contain;
}

.tk-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tk-user {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
}

.btn-logout {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 16px;
  padding: 6px 14px;
  cursor: pointer;
}

.btn-logout:hover {
  background: #fef2f2;
  border-color: #fecaca;
}

.tk-body {
  flex: 1;
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  padding: 28px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tk-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px 22px;
}

.tk-card h2 {
  margin: 0 0 14px;
  font-size: 16px;
  color: #1e293b;
}

.tk-info {
  margin: 0;
  display: grid;
  gap: 10px;
}

.tk-info dt {
  font-size: 12px;
  color: #64748b;
}

.tk-info dd {
  margin: 2px 0 0;
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.tk-muted {
  color: #64748b;
  font-size: 13.5px;
  margin: 0;
}

.tk-err {
  color: #dc2626;
  font-size: 13.5px;
  margin: 0;
}

.tk-vouchers {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tk-voucher {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tk-voucher-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.tk-voucher-meta {
  font-size: 12.5px;
  color: #64748b;
}
</style>
