<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDoiSoat } from '@/api/giaoCa'
import { formatVnd, formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const ca = ref(null)

onMounted(async () => {
  try {
    ca.value = await getDoiSoat(route.params.id)
  } catch (e) {
    error.value = e.response?.data?.message || e.message || 'Không tải được biên bản giao ca.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="reconcile-page">
    <div class="reconcile-heading">
      <div>
        <h1>Biên bản giao ca</h1>
        <p v-if="ca">{{ ca.maCa }} · {{ ca.tenCaLich || 'Không gắn lịch ca' }}</p>
      </div>
      <button type="button" class="btn-back" @click="router.push('/ca')">Quay lại danh sách</button>
    </div>

    <div v-if="loading" class="reconcile-card">Đang tải biên bản...</div>
    <div v-else-if="error" class="reconcile-card error">{{ error }}</div>
    <template v-else-if="ca">
      <section class="reconcile-grid">
        <article class="reconcile-card"><span>Nhân viên</span><strong>{{ ca.tenNhanVien || '—' }}</strong></article>
        <article class="reconcile-card"><span>Quầy</span><strong>{{ ca.tenQuy || '—' }}</strong></article>
        <article class="reconcile-card"><span>Thời gian mở</span><strong>{{ formatDateTime(ca.thoiGianMo) }}</strong></article>
        <article class="reconcile-card"><span>Thời gian kết</span><strong>{{ ca.thoiGianKet ? formatDateTime(ca.thoiGianKet) : 'Ca đang mở' }}</strong></article>
        <article class="reconcile-card"><span>Tiền đầu ca</span><strong>{{ formatVnd(ca.tienDauCa) }}</strong></article>
        <article class="reconcile-card"><span>Số hóa đơn</span><strong>{{ ca.tongHoaDon ?? 0 }}</strong></article>
        <article class="reconcile-card"><span>Doanh thu</span><strong>{{ formatVnd(ca.tongDoanhThu) }}</strong></article>
        <article class="reconcile-card"><span>Tiền mặt thu</span><strong>{{ formatVnd(ca.tienMatThu) }}</strong></article>
        <article class="reconcile-card"><span>Thanh toán không tiền mặt</span><strong>{{ formatVnd(ca.tienKhongMat) }}</strong></article>
        <article class="reconcile-card"><span>Tiền mặt hoàn</span><strong>{{ formatVnd(ca.tienHoanMat) }}</strong></article>
        <article class="reconcile-card"><span>Tiền chi trong ca</span><strong>{{ formatVnd(ca.tienChi || 0) }}</strong></article>
        <article class="reconcile-card"><span>Tiền dự kiến trong quỹ</span><strong>{{ formatVnd(ca.tienDuKien) }}</strong></article>
        <article class="reconcile-card"><span>Tiền thực tế</span><strong>{{ ca.tienThucTe == null ? '—' : formatVnd(ca.tienThucTe) }}</strong></article>
        <article class="reconcile-card"><span>Chênh lệch</span><strong>{{ ca.chenhLech == null ? '—' : formatVnd(ca.chenhLech) }}</strong><small v-if="ca.lyDoLech">{{ ca.lyDoLech }}</small></article>
        <article class="reconcile-card"><span>Ghi chú mở ca</span><strong>{{ ca.ghiChuMo || '—' }}</strong></article>
        <article class="reconcile-card"><span>Người nhận bàn giao</span><strong>{{ ca.tenNguoiNhan || 'Cuối ngày' }}</strong></article>
      </section>

      <section class="reconcile-card history-card">
        <h2>Lịch sử thao tác</h2>
        <div v-if="!ca.lichSu?.length" class="empty-history">Chưa có lịch sử thao tác.</div>
        <div v-else class="history-list">
          <article v-for="(item, index) in ca.lichSu" :key="`${item.hanhDong}-${item.thoiGian}-${index}`" class="history-item">
            <strong>{{ item.hanhDong === 'MO_CA' ? 'Mở ca' : item.hanhDong === 'KET_CA' ? 'Kết ca' : item.hanhDong }}</strong>
            <span>{{ item.tenNhanVien || 'Nhân viên' }} · {{ formatDateTime(item.thoiGian) }}</span>
            <small v-if="item.ghiChu">{{ item.ghiChu }}</small>
          </article>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.reconcile-page { padding: 20px; background: #f8fafc; min-height: 100vh; }
.reconcile-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.reconcile-heading h1 { margin: 0; color: #0f172a; font-size: 24px; }
.reconcile-heading p { margin: 5px 0 0; color: #64748b; }
.reconcile-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; }
.reconcile-card { padding: 16px; border: 1px solid #e2e8f0; border-radius: 12px; background: #fff; box-shadow: 0 3px 10px rgb(15 23 42 / 4%); }
.reconcile-card > span { display: block; margin-bottom: 7px; color: #64748b; font-size: 13px; }
.reconcile-card > strong { color: #0f172a; font-size: 16px; }
.reconcile-card small { display: block; margin-top: 5px; color: #64748b; }
.history-card { margin-top: 16px; }
.history-card h2 { margin: 0 0 12px; font-size: 18px; }
.history-list { display: grid; gap: 10px; }
.history-item { display: grid; gap: 4px; padding: 10px 12px; border-left: 3px solid #2563eb; background: #f8fafc; }
.history-item span, .history-item small, .empty-history { color: #64748b; font-size: 13px; }
.error { color: #b91c1c; }
.btn-back { padding: 9px 14px; border: 1px solid #cbd5e1; border-radius: 8px; background: #fff; color: #334155; cursor: pointer; }
@media (max-width: 600px) { .reconcile-heading { align-items: flex-start; flex-direction: column; } }
</style>
